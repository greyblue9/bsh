package org.d6r;

import java.net.*;
import java.util.*;
import dalvik.system.*;
import java.lang.reflect.*;


public class Reflect2 {
  
  public static final Class<?> dexPathListCls;
  public static final Class<?> elementCls;
  static {
    try {
      dexPathListCls 
 = Class.forName("dalvik.system.DexPathList");
      elementCls 
 = Class.forName("dalvik.system.DexPathList$Element");
    } catch (Exception ex) { throw new RuntimeException(ex); }
  }

  public static Object getfldval(Object o, String pName) {
    return getfldval(o, null, pName);
  }

  public static Object getfldval
  (Object o, Class cls, String pName) 
  {
    if (o == null) return "null";
    if (cls == null) cls = o.getClass();
    if (cls == null) return "null";
    Field[] fields = cls.getDeclaredFields();
    Field field;
    //StringBuilder sb = new StringBuilder(75);
    //sb.append(" ==  = As " + cls.getName() + " ==  = " + lf);
    for (int n = 0; n < fields.length; n++) {
      field = fields[n];
      String name = field.getName();
      if (name.equals(pName)) {
        try {
          ((AccessibleObject) field).setAccessible(true);
          boolean isStatic 
 = Modifier.isStatic(field.getModifiers());
          Object val = field.get(isStatic ? null : o);
          return val;
        } catch (IllegalAccessException e) {
          System.err.print(e.toString());
          return null;
        }
      }
    }
    Class superCls = cls.getSuperclass();
    if (superCls == null || superCls == Object.class) {
      //System.err.println(sb.toString());
      return null;
    }
    return getfldval(o, superCls, pName);
  }
  
  



  public static Object setfldval(Object o, String pName, Object value) {
    return setfldval(o, null, pName, value);
  }

  public static Object setfldval
  (Object o, Class cls, String pName, Object value) 
  {
    if (o == null) return "null";
    if (cls == null) cls = o.getClass();
    if (cls == null) return "null";
    Field[] fields = cls.getDeclaredFields();
    Field field;
    for (int n = 0; n < fields.length; n++) {
      field = fields[n];
      String name = field.getName();
      if (name.equals(pName)) {
        try {
          ((AccessibleObject) field).setAccessible(true);
          boolean isStatic 
 = Modifier.isStatic(field.getModifiers());
          field.set(isStatic ? null : o, value);
          Object val = field.get(isStatic ? null : o);
          return val;
        } catch (IllegalAccessException e) {
          System.err.print(e.toString());
          return null;
        } catch (Throwable e) {
          System.err.print(e.toString());
          return null;
        }
      }
    }
    Class superCls = cls.getSuperclass();
    if (superCls == null || superCls == Object.class) {
      System.err.println("[WARN] Can not find field `"+pName+"` in superclass heirarchy; returning NULL");
      return null;
    }
    return setfldval(o, superCls, pName, value);
  }
  
  public static class MethodTarget {
    public Method m;
    public Object target;
    public MethodTarget(Method m, Object target) {
      this.m = m;
      this.target = target;
    }
  }
  
  public static class ClassTarget {
    public Class<?> cls;
    public Object target;
    public ClassTarget(Class<?> cls, Object target) {
      this.cls = cls;
      this.target = target;
    }
  }
  
  public static ClassTarget resolveClassTarget(Object o) {
    Class<?> cls = null;
    Object target = null;
    
    if (o instanceof Class<?>) {
      if (Class.class.equals(o)) {
        cls = Class.class;
      } else {  
        cls = (Class<?>) o;
      }
    } else if (o instanceof String) { 
      try {
        cls = Class.forName((String) o, 
          false, Thread.currentThread().getContextClassLoader());
      } catch (ClassNotFoundException cfne) {
        cls = String.class;
        target = o;
      }
    } else if (o instanceof Object) {
      cls = o.getClass();
      target = o;
    } else {
      return null;
    }
    return new ClassTarget(cls, target);
  }
  
  // @Extension
  public static Method findMethod
  (Class<?> cls, String name, Class<?>... paramClzs) 
  {
    return 
      findMethod(new ClassTarget(cls, null), name, paramClzs);
  }
  
  /*
  // @Extension
  public static Method findMethod
  (Class<Class> cls, String name, Class<?>... paramClzs) 
  {
    return 
      findMethod(
        new ClassTarget(Class.class, cls), name, paramClzs);
  }
  */
  // @Extension
  public static Method findMethod
  (Object o, String name, Class<?>... paramClzs) 
  {
    ClassTarget ct = resolveClassTarget(o);
    if (ct == null) {
      throw new IllegalArgumentException(
        String.format("findMethod: o cannot be null: findMethod(o = %s, name = %s, paramClzs = %s)", o == null? "<null>": o.toString(), name == null? "<null>": name, paramClzs == null? "<null>": Arrays.toString(paramClzs))
      );
    }
    return findMethod(ct, name, paramClzs);
  }
  
  // @Extension
  public static Method findMethod
  (ClassTarget ct, String name, Class<?>... paramClzs) 
  {
    
    Method m = null;
    Class<?> cls = ct.cls;
    Object target = ct.target;
    NoSuchMethodException firstEx = null;
    
    Class<?> sc = cls;
    while (sc != null) {
      try {
        m = sc.getDeclaredMethod(name, paramClzs);
        m.setAccessible(true);
        return m;
      } catch (NoSuchMethodException e) {
        if (firstEx == null) firstEx = e;
      } catch (Throwable e2) {
        throw new RuntimeException(e2);
      }
      sc = sc.getSuperclass();
    }
    
    sc = cls;
    while (sc != null) {
      try {
        for (Method dm: sc.getDeclaredMethods()) {
          m = dm;
          if (name.equals(m.getName())) {
            m.setAccessible(true);
            return m;
          }
        }
      //} catch (NoSuchMethodException e) {
      //  if (firstEx == null) firstEx = e;
      } catch (Throwable e2) {
        throw new RuntimeException(e2);
      }
      sc = sc.getSuperclass();
    } // while 2
  
    throw new RuntimeException(firstEx);
  }
  
  public static <T> Object invoke
  (Method m, T thisObj, Object... params) 
  {
    try {
      if (! m.isAccessible()) m.setAccessible(true);
      return m.invoke(thisObj, params);
    } catch (Exception ex) { throw new RuntimeException(ex); }
  }
  
  
  public static Object invokeMethod
  (Object o, String name, Object... args) 
  {
    ClassTarget ct = resolveClassTarget(o);
    Class<?> cls = ct.cls;
    Object target = ct.target;
    
    ArrayList<Class<?>> pTypes = new ArrayList<Class<?>>();
    for (int n = 0; n < args.length; n++) {
      Object arg = args[n];
      Class<?> pType = (arg != null)
        ? arg.getClass()
        : Object.class;
      pTypes.add(pType);
    }
    
    Class<?>[] types = pTypes.toArray(new Class[0]);
    Method m = null;
    for (int cut = 0; cut < args.length; cut++) {
      int addOne = cut > 0? 1: 0;
      
      Class<?>[] useTypes = Arrays.copyOfRange(
        types, 0,
        types.length - cut + addOne
      );
      if (addOne > 0) {
        useTypes[useTypes.length - 1]
 = (Array.newInstance(types[useTypes.length - 1], 0))
              .getClass(); 
      }
      
      try { 
        m = findMethod(o, name, useTypes);
        return m.invoke(target, args);
      } catch (InvocationTargetException e) {
        System.err.println(e.toString());
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    } // cut loop
    return null;    
  }
  
  
  
  
  public static ArrayList<URL> getResources
  (BaseDexClassLoader ldr, String name) 
  {
    ArrayList<URL> result = new ArrayList<URL>();
    Object pathList = getfldval(ldr, "pathList");
    Object[] dexElements 
 = (Object[]) getfldval(pathList, "dexElements");
    Method findResources = findMethod(
      elementCls, "findResource", String.class);
    
    for (Object element: dexElements) {
      URL url = (URL) invoke(findResources, element, name);
      if (url == null) continue; 
      result.add(url);
    }
    return result;
  }
  
}