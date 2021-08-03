package org.d6r;

import org.d6r.annotation.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import static org.d6r.Reflect.findMethod;
import static bsh.Reflect.findBestMatch;
import org.apache.commons.lang3.tuple.Pair;



public class ProxyCreator {
  
  
  static MethodFilter defaultFilter;
  public static MethodFilter getDefaultFilter() {
    if (defaultFilter == null) {
      defaultFilter = new ObjectMethodsFilter(
        findMethod(Object.class, "hashCode"),
        findMethod(Object.class, "equals")
      );
    }
    return defaultFilter;
  }
  
  
  public static class ObjectMethodsFilter
           implements MethodFilter
  {
    Method[] unhandledMethods;
    Class<?>[][] unhandledParamTypes;
    
    public ObjectMethodsFilter(Method... unhandledMethods) {
      this.unhandledMethods = unhandledMethods;
    }
    
    @Override
    public boolean isHandled(Method method) {
      if (unhandledParamTypes == null 
      ||  unhandledParamTypes.length != unhandledMethods.length) 
      {
        unhandledParamTypes = new Class<?>[unhandledMethods.length][];
        for (int i=0, len=unhandledMethods.length; i<len; ++i) {
           unhandledParamTypes[i] = unhandledMethods[i].getParameterTypes();
        }
      }
      
      String name = method.getName();
      Class<?>[] params = method.getParameterTypes();
      
      for (int i=0, len=unhandledMethods.length; i<len; ++i) {
        Method um = unhandledMethods[i];
        if (! um.getName().equals(name)) continue;
        if (! Arrays.equals(unhandledParamTypes[i], params)) continue;
        // Method should not be handled
        return false;
      }
      return true;
    }
  }
  
  
  public static class InvocationHandlerWrapper<T>
           implements MethodHandler, 
                      InvocationHandler
  {
    protected InvocationHandler ih;
    
    public InvocationHandlerWrapper(InvocationHandler ih) {
      this.ih = ih;
    }
    
    Object o = new Object();
    volatile Method overridden;
    volatile Method forwarder;
    
    public Method getOverridden() {
      synchronized (o) { return overridden; }
    }
    public Method getForwarder() {
      synchronized (o) { return forwarder; }
    }
    
    @Override
    public Object invoke(Object self, Method forwarder, Object[] args) 
      throws Throwable
    {      
      System.err.printf(
        "%s.invoke(Object self, Method forwarder, Object[] args)",
        getClass().getName()
      );
      return ih.invoke(self, overridden, args);
    }
    
    
    @Override
    public Object invoke(Object self, Method overridden, Method forwarder, 
    Object[] args)
      throws Throwable 
    {
      System.err.printf(
        "%s.invoke(Object self, Method overridden, Method forwarder, "
        + "Object[] args)\n", getClass().getName()
      );
      synchronized (o) { this.overridden = overridden; }
      synchronized (o) { this.forwarder = forwarder; }
      
      Object result = ih.invoke(self, forwarder, args);      
      return result;
    }
  }
  
  public static class LoggingHandler<T>
           implements MethodHandler
  {
    List<Pair<Method, Object[]>> calls
      = new ArrayList<Pair<Method, Object[]>>();
    
    Map<Method, List<Object[]>> callMap 
      = new HashMap<Method, List<Object[]>>();
    int callCount = 0;
    
    @NonDumpable("Intercepting Proxies")
    Map<Method, MethodHandler> intercepts
      = new HashMap<Method, MethodHandler>();
    
    @NonDumpable("Proxied Object")
    protected ProxyObject[] mSelf;    
    
    public boolean VERBOSE = true;
    
    public MethodHandler intercept(Method method, MethodHandler target) {
      return intercepts.put(method, target);
    }
    
    public T getTarget() {
      return (T) (Object) mSelf[0];
    }
    
    public void setInstance(ProxyObject obj) {
      mSelf = new ProxyObject[] { obj };
    }
    
    @Override
    public Object invoke(Object self, 
    Method overridden, Method forwarder, Object[] args)
      throws Throwable 
    {
      if (callCount++ == 0) {
        System.err.print("\n");
      }
      
      try {
        calls.add(Pair.of(overridden, args));
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace")))
          e.printStackTrace();
      }
      
      try {
        List<Object[]> methodCalls;
        if ((methodCalls = callMap.get(overridden)) == null) {
          methodCalls = new ArrayList<Object[]>();
          callMap.put(overridden, methodCalls);
        }
        methodCalls.add(args);
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace")))
          e.printStackTrace();
      }
      
      try {
        System.err.println(dumpMembers.colorize(overridden));
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace")))
          e.printStackTrace();
      }
      
      /** /
      try {
        System.err.println(dumpMembers.colorize(forwarder));
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      }
      /* */
      if (VERBOSE) {
        try {
          CollectionUtil.printR(args);
        } catch (Throwable e) {
          if ("true".equals(System.getProperty("printStackTrace")))
            e.printStackTrace();
        }
      }
      
      if (intercepts.containsKey(overridden)) {
        System.err.println("Redirecting intercepted method");
        return intercepts.get(overridden).invoke(
          self, overridden, forwarder, args
        );
      }
      
      return forwarder.invoke(self, args);
    }
  }
  
  
  public static <T> T create(Class<T> cls, Object... args)
    throws Exception 
  {
    try {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(cls);
      factory.setFilter(getDefaultFilter());
      Class<T> clazz = factory.createClass();
      System.err.println(clazz.getName());
      
      LoggingHandler<T> handler = new LoggingHandler<T>();
      
      Constructor<T> ctor = findBestMatch(
        (Constructor<T>[]) clazz.getDeclaredConstructors(),
        args
      );
      System.err.printf(
        "constructor: \n  %s\n", 
        ctor.toGenericString()
      );
      Object instance = ctor.newInstance(args);
      ((ProxyObject) instance).setHandler(handler);
      handler.setInstance((ProxyObject) instance);
      return (T) instance;
    } catch (Throwable e2) { 
      e2.printStackTrace();      
    } 
    return null;
  }
  
  
  public static <T> T wrap(Object realObject, Class<T> cls, Object... args)
    throws Exception 
  {
    try {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(cls);
      factory.setFilter(getDefaultFilter());
      Class<T> clazz = factory.createClass();
      System.err.println(clazz.getName());
      
      LoggingHandler<T> handler = new LoggingHandler<T>();
      
      ProxyObject instance 
        = (ProxyObject) UnsafeUtil.allocateInstance(clazz);
      instance.setHandler(handler);
      CollectionUtil.clone(
        realObject, instance, new HashSet<Object>(), 0, 10
      );
      
      handler.setInstance(instance);
      return (T) instance;
    } catch (Throwable e2) { 
      e2.printStackTrace();      
    } 
    return null;
  }
  
  
  public static <T> T adapt(Class<T> cls, InvocationHandler ih,
  Object... args)
    throws Exception 
  {
    try {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(cls);
      factory.setFilter(getDefaultFilter());
      Class<T> clazz = factory.createClass();
      System.err.println(clazz.getName());
      
      InvocationHandlerWrapper handler 
        = new InvocationHandlerWrapper(ih);
      
      Constructor<T> ctor = findBestMatch(
        (Constructor<T>[]) clazz.getDeclaredConstructors(),
        args
      );
      System.err.printf(
        "constructor: \n  %s\n", ctor.toGenericString()
      );
      Object instance = ctor.newInstance(args);
      ((ProxyObject) instance).setHandler(handler);
      return (T) instance;
    } catch (Throwable e2) { 
      e2.printStackTrace();      
    } 
    return null;
  }
  
  public static <T> T create(Class<T> cls, 
  MethodHandler handler, Object... args)
    throws Exception 
  {
    try {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(cls);
      factory.setFilter(getDefaultFilter());
      Class<? extends T> clazz = factory.createClass();
      System.err.println(clazz.getName());
      
      Constructor<T> ctor = findBestMatch(
        (Constructor<T>[]) clazz.getDeclaredConstructors(),
        args
      );
      Object instance = ctor.newInstance(args);
      ((ProxyObject) instance).setHandler(handler);
      if (handler instanceof LoggingHandler<?>) {
        ((LoggingHandler<T>) handler).setInstance((ProxyObject) instance);
      }
      return (T) instance;
    } catch (Throwable e2) { 
      e2.printStackTrace();      
    } 
    return null;
  }
  
  
}
