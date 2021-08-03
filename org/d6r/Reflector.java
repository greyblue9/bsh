package org.d6r;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.util.Collection;
/**
  Exploits a weakness in the runtime to throw an arbitrary
  throwable without the traditional declaration.
  
  This is a dangerous API that should be used with great
  caution.
  
  Typically this is useful when rethrowing throwables that
  are of a known range of types.
  
  The following code must enumerate several types to rethrow:
  
  public void close() throws IOException {
      Throwable thrown = null;
      ...
      if (thrown != null) {
          if (thrown instanceof IOException) {
              throw (IOException) thrown;
          } else if (thrown instanceof RuntimeException) {
              throw (RuntimeException) thrown;
          } else if (thrown instanceof Error) {
              throw (Error) thrown;
          } else {
              throw new AssertionError();
          }
      }
  }
  
  With SneakyThrow, rethrowing is easier:
  
  public void close() throws IOException {
      Throwable thrown = null;
      ...
      if (thrown != null) {
          SneakyThrow.sneakyThrow(thrown);
      }
  }
*/
public class Reflector {
  
  public static boolean DEBUG = false;
  
  
  public static class Util {
    public static 
    <X extends RuntimeException> X sneakyThrow(Throwable t) {
      return (X) Util.<Error>sneakyThrow2(t);
    }
    
    /**
      Exploits unsafety to throw an exception that the
      compiler wouldn't permit but that the runtime doesn't
      check. 
      
      See Java Puzzlers #43.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> 
    Throwable sneakyThrow2(Throwable t) throws T {
      if (t == null) {
        Thread.currentThread().dumpStack();
        t = new RuntimeException(
          "Util.sneakyThrow2: t == null!"
        );
        if ("true".equals(System.getProperty("printStackTrace"))) t.printStackTrace();
      }
      throw (T) t;
    }
  }
  
  public static <T extends V, U extends V, V> V 
  firstNonNull(T o1, U o2) 
  {
    if (o1 != null) return o1;
    return o2;
  }
  
  public static <X extends Throwable> X unwrap(Throwable ex) {
    Throwable e = ex;
    do {
      if (e instanceof InvocationTargetException) {        
        e = firstNonNull(((InvocationTargetException) e)
          .getTargetException(), e);
        continue;
      }
      if (e instanceof UndeclaredThrowableException) {        
        e = firstNonNull(((UndeclaredThrowableException) e)
          .getUndeclaredThrowable(), e);
        continue;
      }
      /*if (e instanceof ExceptionInInitializerError) {
        Reflector.addSuppressed(e, e);
        e = firstNonNull(((ExceptionInInitializerError) e)
          .getException(), e);
        continue;
      }*/
      /*if (e instanceof ClassNotFoundException) {
        Reflector.addSuppressed(e, e);
        e = firstNonNull(((ClassNotFoundException) e)
          .getException(), e);
        continue;
      }*/
      break;
    } while (false);
    /*if (ex != e) {
      Reflect.setfldval(ex, "cause", null);
      Reflector.addSuppressed(e, ex);
    }*/
    return (X) e;
  }
  
  public static void addSuppressed(Throwable ex, Throwable supp) {
    try {
      ex.addSuppressed(supp);
    } catch (Throwable ignore) {
      List<Throwable> suppressed = Reflect.getfldval(
        ex, "suppressedExceptions"
      );
      if (suppressed == null) {
        suppressed = new ArrayList<Throwable>();
      } else {
        suppressed = new ArrayList<Throwable>(suppressed);
      }
      suppressed.add(supp);
      Reflect.setfldval(ex, "suppressedExceptions", suppressed);
    }
  }
  
  public static class Compiler {
    public static boolean subsumes
    (Object[] params, Class<?>[] parameterTypes) 
    {
       return false;
    }
  }
  
  public static class RT {
    public static final Object[] EMPTY_ARRAY = new Object[0];
    
    public static Class<?> classForName(String clsName) {
      try {
        return Class.forName(clsName);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  public static Throwable getRootCause(Throwable ex) {   
    Object[] exo = { null, ex }, last = null; 
    while (exo != null
    &&  ((last != null && last[1] != exo[1]) || last == null))
    { 
      last = exo; 
      exo = Reflect.findField(exo[1], Throwable.class, 2);
    }
    return exo != null
             ? (Throwable) exo[1]
             : (last != null
                 ? (Throwable) last[1]
                 : (ex));
  }
  
  public static <R> R invokeOrDefault(Object target, String name,
  Object[] args, Object defaultRet)
  {
    if (DEBUG) System.err.printf(
      "invokeOrDefault(\n"
      +"  Object target (%s): %s,\n"
      +"  String name: %s,\n"
      +"  Object[] args: %s,\n"
      +"  Object defaultRet: %s\n"
      +")\n",
      target != null? target.getClass().getName(): "null",
      target,
      name,
      args!=null? 
        org.apache.commons.lang3.StringUtils.join(args, ", "): "null",
      defaultRet
    );
    if (DEBUG) System.err.printf("  - caller: %s\n", Debug.getCallingMethod(2));
    if (DEBUG) System.err.printf("  - caller: %s\n", Debug.getCallingMethod(3));
    if (DEBUG) System.err.printf("  - caller: %s\n", Debug.getCallingMethod(4));
    
    if (target == null) {
      if (DEBUG) System.err.println("returning default");
      return (R) defaultRet;
    }
    
    Class tcls = (target instanceof Class<?>)
      ? (Class<?>) target
      : (Class<?>) target.getClass();
    if (DEBUG) System.err.printf("  - Assuming tcls: %s\n", tcls);
    
    while (tcls != null) {
      for (Method tm : tcls.getDeclaredMethods()) {
        
        if (tm.getName().matches(name) 
        && (tm.isVarArgs() 
         || tm.getParameterTypes().length == args.length)) 
        {
          if (DEBUG) System.err.printf("  - Considering: %s\n", tm);
          tm.setAccessible(true);
          if (tm.getReturnType() != Void.TYPE) {
            try {
              R ret = (R) tm.invoke(target, args);
              if (DEBUG) System.err.printf("  - Returning: %s\n", ret);
              return ret;
            } catch (Throwable e) {
              e.printStackTrace();
              continue;
            }
          }
          try {
            R ret = (R) tm.invoke(target, args);
            if (DEBUG) System.err.printf(
              "  - Returning default (void): %s\n", defaultRet);
            return (R) defaultRet;
          } catch (Throwable e) { 
            e.printStackTrace();
            continue;
          }
        }
      }
      tcls = tcls.getSuperclass();
      if (DEBUG) System.err.printf("  - tcls -> %s\n", tcls);
    }
    if (DEBUG) System.err.printf("  - giving up; return default: %s\n", 
      defaultRet);
    return (R) defaultRet;
  }
  
  public static <R> R invokeOrDefault(Object target, String name, 
  Object... args)
  {
    return (R) invokeOrDefault(target, name, args, null);
  }
  
  public static <R> R invoke(Object target, String name,
  Object... args)
  {
    return (R) invokeOrDefault(target, name, args, null);
  }
  
  public static <R> R invokeInstanceMethod(Object target, 
  String methodName, Object... args)
  {
    Class<?> c = target.getClass();
    List<Method> methods 
      = getMethods(c, args.length, methodName, false);
    return (R)invokeMatchingMethod(methodName, methods, target, args);
  }
  
  public static Throwable getCauseOrElse(Exception e) {
    return getRootCause(e);
  }
  
  
  public static 
  Throwable throwCauseOrElseException(Exception e)
  {
    Throwable cause = getRootCause(e);
    return Util.sneakyThrow(cause);
  }
  
  public static String noMethodReport(String methodName, Object target){
     return "No matching method found: " + methodName
        + (target==null?"":" for " + target.getClass());
  }
  public static Object invokeMatchingMethod(String methodName, List methods, Object target, Object[] args)
      {
    Method m = null;
    Object[] boxedArgs = null;
    if(methods.isEmpty())
      {
      throw new IllegalArgumentException(noMethodReport(methodName,target));
      }
    else if(methods.size() == 1)
      {
      m = (Method) methods.get(0);
      boxedArgs = boxArgs(m.getParameterTypes(), args);
      }
    else //overloaded w/same arity
      {
      Method foundm = null;
      for(Iterator i = methods.iterator(); i.hasNext();)
        {
        m = (Method) i.next();
  
        Class[] params = m.getParameterTypes();
        if(isCongruent(params, args))
          {
          if(foundm == null || Compiler.subsumes(params, foundm.getParameterTypes()))
            {
            foundm = m;
            boxedArgs = boxArgs(params, args);
            }
          }
        }
      m = foundm;
      }
    if(m == null)
      throw new IllegalArgumentException(noMethodReport(methodName,target));
  
    if(!Modifier.isPublic(m.getDeclaringClass().getModifiers()))
      {
      //public method of non-public class, try to find it in hierarchy
      Method oldm = m;
      m = getAsMethodOfPublicBase(target.getClass(), m);
      if(m == null)
        throw new IllegalArgumentException("Can't call public method of non-public class: " +
                                            oldm.toString());
      }
    try
      {
      return prepRet(m.getReturnType(), m.invoke(target, boxedArgs));
      }
    catch(Exception e)
      {
      return Util.sneakyThrow(getCauseOrElse(e));
      }
  
  }
  
  public static Method getAsMethodOfPublicBase(Class c, Method m){
    for(Class iface : c.getInterfaces())
      {
      for(Method im : iface.getMethods())
        {
        if(isMatch(im, m))
          {
          return im;
          }
        }
      }
    Class sc = c.getSuperclass();
    if(sc == null)
      return null;
    for(Method scm : sc.getMethods())
      {
      if(isMatch(scm, m))
        {
        return scm;
        }
      }
    return getAsMethodOfPublicBase(sc, m);
  }
  
  public static boolean isMatch(Method lhs, Method rhs) {
    if(!lhs.getName().equals(rhs.getName())
        || !Modifier.isPublic(lhs.getDeclaringClass().getModifiers()))
      {
      return false;
      }
  
      Class[] types1 = lhs.getParameterTypes();
      Class[] types2 = rhs.getParameterTypes();
      if(types1.length != types2.length)
        return false;
  
      boolean match = true;
      for (int i=0; i<types1.length;++i)
        {
        if(!types1[i].isAssignableFrom(types2[i]))
          {
          match = false;
          break;
          }
        }
      return match;
  }
  
  public static <T> T invokeConstructor(Class<T> c, 
  Object[] args) 
  {
    T obj = null;
    try {
      Constructor<T>[] allctors 
        = (Constructor<T>[]) c.getConstructors();
      ArrayList<Constructor<T>> ctors 
        = new ArrayList<Constructor<T>>();
      for (int i = 0; i < allctors.length; i++) {
        Constructor<T> ctor = allctors[i];
        if (ctor.getParameterTypes().length == args.length) {
          ctors.add(ctor);
        }
      }
      if (ctors.size() == 1) {
        Constructor<T> ctor = ctors.get(0);
        return (T) ctor.newInstance(        
          boxArgs(ctor.getParameterTypes(), args)
        );
      }
      // overloaded w/same arity
      for (Iterator<Constructor<T>> iterator 
         = ctors.iterator(); iterator.hasNext(); )
      {
          Constructor<T> ctor = iterator.next();
          Class<?>[] params = ctor.getParameterTypes();
          if (isCongruent(params, args)) {
            Object[] boxedArgs = boxArgs(params, args);
            return (T) ctor.newInstance(boxedArgs);
          }
      }
      Class<?>[] argTypes = new Class<?>[args.length];
      String[] clsNames = new String[args.length];
      for (int i=0; i<args.length; i++) {
        argTypes[i] = (args[i] != null)
          ? args[i].getClass(): Object.class;
        clsNames[i] = String.format(
          "%s.class", argTypes[i].getName()
        );
      }
      throw new IllegalArgumentException(String.format(
        "No matching ctor found: %s(%s)",
        c.getName(), StringUtils.join(clsNames, ", ")
      ));
    } catch(Exception e) {
      if (e instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) e;
      }
      return (T) Util.sneakyThrow(getRootCause(e));
    }
  }
  
  public static 
  Object invokeStaticMethodVariadic(String className, 
  String methodName, Object... args) {
    return invokeStaticMethod(className, methodName, args);
  }
  
  public static 
  Object invokeStaticMethod(String className,
  String methodName, Object[] args) {
    Class c = RT.classForName(className);
    return invokeStaticMethod(c, methodName, args);
  }
  
  public static 
  Object invokeStaticMethod(Class c,
  String methodName, Object[] args) {
    if(methodName.equals("new"))
      return invokeConstructor(c, args);
    List methods = getMethods(c, args.length, methodName, true);
    return invokeMatchingMethod(methodName, methods, null, args);
  }
  
  public static Object getStaticField(String className, String fieldName) {
    Class c = RT.classForName(className);
    return getStaticField(c, fieldName);
  }
  
  public static Object getStaticField(Class c, String fieldName) {
  //  if(fieldName.equals("class"))
  //    return c;
    Field f = getField(c, fieldName, true);
    if(f != null)
      {
      try
        {
        return prepRet(f.getType(), f.get(null));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      }
    throw new IllegalArgumentException("No matching field found: " + fieldName
      + " for " + c);
  }
  
  public static Object setStaticField(String className, String fieldName, Object val) {
    Class c = RT.classForName(className);
    return setStaticField(c, fieldName, val);
  }
  
  public static Object setStaticField(Class c, String fieldName, Object val) {
    Field f = getField(c, fieldName, true);
    if(f != null)
      {
      try
        {
        f.set(null, boxArg(f.getType(), val));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      return val;
      }
    throw new IllegalArgumentException("No matching field found: " + fieldName
      + " for " + c);
  }
  
  public static Object getInstanceField(Object target, String fieldName) {
    Class c = target.getClass();
    Field f = getField(c, fieldName, false);
    if(f != null)
      {
      try
        {
        return prepRet(f.getType(), f.get(target));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      }
    throw new IllegalArgumentException("No matching field found: " + fieldName
      + " for " + target.getClass());
  }
  
  public static Object setInstanceField(Object target, String fieldName, Object val) {
    Class c = target.getClass();
    Field f = getField(c, fieldName, false);
    if(f != null)
      {
      try
        {
        f.set(target, boxArg(f.getType(), val));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      return val;
      }
    throw new IllegalArgumentException("No matching field found: " + fieldName
      + " for " + target.getClass());
  }
  
  // not used as of Clojure 1.6, but left for runtime compatibility with
  // compiled bytecode from older versions
  public static Object invokeNoArgInstanceMember(Object target, String name) {
    return invokeNoArgInstanceMember(target, name, false);
  }
  
  public static Object invokeNoArgInstanceMember(Object target, String name, boolean requireField) {
    Class c = target.getClass();
  
    if(requireField) {
      Field f = getField(c, name, false);
      if(f != null)
        return getInstanceField(target, name);
      else
        throw new IllegalArgumentException("No matching field found: " + name
            + " for " + target.getClass());
    } else {
      List meths = getMethods(c, 0, name, false);
      if(meths.size() > 0)
        return invokeMatchingMethod(name, meths, target, RT.EMPTY_ARRAY);
      else
        return getInstanceField(target, name);
    }
  }
  
  public static Object invokeInstanceMember(Object target, String name) {
    //check for field first
    Class c = target.getClass();
    Field f = getField(c, name, false);
    if(f != null)  //field get
      {
      try
        {
        return prepRet(f.getType(), f.get(target));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      }
    return invokeInstanceMethod(target, name, RT.EMPTY_ARRAY);
  }
  
  public static Object invokeInstanceMember(String name, Object target, Object arg1) {
    //check for field first
    Class c = target.getClass();
    Field f = getField(c, name, false);
    if(f != null)  //field set
      {
      try
        {
        f.set(target, boxArg(f.getType(), arg1));
        }
      catch(IllegalAccessException e)
        {
        Util.sneakyThrow(e);
        }
      return arg1;
      }
    return invokeInstanceMethod(target, name, new Object[]{arg1});
  }
  
  public static Object invokeInstanceMember(String name, Object target, Object... args) {
    return invokeInstanceMethod(target, name, args);
  }
  
  
  public static Field getField(Class c, String name, boolean getStatics){
    Field[] allfields = c.getFields();
    for(int i = 0; i < allfields.length; i++)
      {
      if(name.equals(allfields[i].getName())
         && Modifier.isStatic(allfields[i].getModifiers()) == getStatics)
        return allfields[i];
      }
    return null;
  }
  
  public static List getMethods(Class c, int arity, String name, boolean getStatics){
    Method[] allmethods = c.getMethods();
    ArrayList methods = new ArrayList();
    ArrayList bridgeMethods = new ArrayList();
    for(int i = 0; i < allmethods.length; i++)
      {
      Method method = allmethods[i];
      if(name.equals(method.getName())
         && Modifier.isStatic(method.getModifiers()) == getStatics
         && method.getParameterTypes().length == arity)
        {
        try
          {
          if(method.isBridge()
             && c.getMethod(method.getName(), method.getParameterTypes())
              .equals(method))
            bridgeMethods.add(method);
          else
            methods.add(method);
          }
        catch(NoSuchMethodException e)
          {
          }
        }
  //         && (!method.isBridge()
  //             || (c == StringBuilder.class &&
  //                c.getMethod(method.getName(), method.getParameterTypes())
  //          .equals(method))))
  //        {
  //        methods.add(allmethods[i]);
  //        }
      }
  
    if(methods.isEmpty())
      methods.addAll(bridgeMethods);
    
    if(!getStatics && c.isInterface())
      {
      allmethods = Object.class.getMethods();
      for(int i = 0; i < allmethods.length; i++)
        {
        if(name.equals(allmethods[i].getName())
           && Modifier.isStatic(allmethods[i].getModifiers()) == getStatics
           && allmethods[i].getParameterTypes().length == arity)
          {
          methods.add(allmethods[i]);
          }
        }
      }
    return methods;
  }
  
  
  public static Object boxArg(Class paramType, Object arg){
    if(!paramType.isPrimitive())
      return paramType.cast(arg);
    else if(paramType == boolean.class)
      return Boolean.class.cast(arg);
    else if(paramType == char.class)
      return Character.class.cast(arg);
    else if(arg instanceof Number)
      {
      Number n = (Number) arg;
      if(paramType == int.class)
        return n.intValue();
      else if(paramType == float.class)
        return n.floatValue();
      else if(paramType == double.class)
        return n.doubleValue();
      else if(paramType == long.class)
        return n.longValue();
      else if(paramType == short.class)
        return n.shortValue();
      else if(paramType == byte.class)
        return n.byteValue();
      }
    throw new IllegalArgumentException("Unexpected param type, expected: " + paramType +
                                       ", given: " + arg.getClass().getName());
  }
  
  public static Object[] boxArgs(Class[] params, Object[] args){
    if(params.length == 0)
      return null;
    Object[] ret = new Object[params.length];
    for(int i = 0; i < params.length; i++)
      {
      Object arg = args[i];
      Class paramType = params[i];
      ret[i ] = boxArg(paramType, arg);
      }
    return ret;
  }
  
  public static boolean paramArgTypeMatch(Class paramType, Class argType){
    if(argType == null)
      return !paramType.isPrimitive();
    if(paramType == argType || paramType.isAssignableFrom(argType))
      return true;
    if(paramType == int.class)
      return argType == Integer.class
             || argType == long.class
          || argType == Long.class
          || argType == short.class
          || argType == byte.class;// || argType == FixNum.class;
    else if(paramType == float.class)
      return argType == Float.class
          || argType == double.class;
    else if(paramType == double.class)
      return argType == Double.class
          || argType == float.class;// || argType == DoubleNum.class;
    else if(paramType == long.class)
      return argType == Long.class
          || argType == int.class
          || argType == short.class
          || argType == byte.class;// || argType == BigNum.class;
    else if(paramType == char.class)
      return argType == Character.class;
    else if(paramType == short.class)
      return argType == Short.class;
    else if(paramType == byte.class)
      return argType == Byte.class;
    else if(paramType == boolean.class)
      return argType == Boolean.class;
    return false;
  }
  
  public static boolean isCongruent(Class[] params, Object[] args){
    boolean ret = false;
    if(args == null)
      return params.length == 0;
    if(params.length == args.length)
      {
      ret = true;
      for(int i = 0; ret && i < params.length; i++)
        {
        Object arg = args[i];
        Class argType = (arg == null) ? null : arg.getClass();
        Class paramType = params[i];
        ret = paramArgTypeMatch(paramType, argType);
        }
      }
    return ret;
  }
  
  public static Object prepRet(Class c, Object x){
    if (!(c.isPrimitive() || c == Boolean.class))
      return x;
    if(x instanceof Boolean)
      return ((Boolean) x)?Boolean.TRUE:Boolean.FALSE;
  //  else if(x instanceof Integer)
  //    {
  //    return ((Integer)x).longValue();
  //    }
  //  else if(x instanceof Float)
  //      return Double.valueOf(((Float) x).doubleValue());
    return x;
  }
}