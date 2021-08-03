package org.d6r;

import java.io.ObjectStreamClass;
import sun.misc.Unsafe;
import org.d6r.Reflector.Util;   
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import bsh.Factory;
import bsh.ClassIdentifier;
import bsh.operators.Extension;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import org.d6r.Reflector.Util;
import bsh.ClassIdentifier;
import bsh.Factory;
import bsh.Types;
import org.apache.commons.lang3.exception.ExceptionUtils;
import dalvik.system.BaseDexClassLoader;
import java.io.ObjectStreamClass;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.*;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Deque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.*;
import java.util.TreeSet;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.collections4.list.PredicatedList;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.ClassPathUtil2;
import org.d6r.Reflect;
import org.d6r.UnsafeUtil;
import org.d6r.dumpMembers;
import org.d6r.findMethod;
import sun.misc.Unsafe;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;
import java.math.BigDecimal;
import static org.apache.commons.lang3.Validate.notNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import static org.d6r.ExceptionPropagation.When.*;
import static org.d6r.ExceptionPropagation.ExceptionChange.*;

@Retention(RetentionPolicy.RUNTIME)
@interface ExceptionPropagation {

  public static enum When {
    UNSPECIFIED, ALWAYS, SOMETIMES, NEVER, IF_DEBUG_FLAGS;
  } 
  public static enum ExceptionChange {
    WRAP, UNWRAP, REPLACE, EXTRA_INFO_ADDED;
  } 
  
  public When value() default UNSPECIFIED;
  public ExceptionChange[] changes() default { };
  public Class<? extends Throwable>[] changed() default { Throwable.class };
  public When[] output() default { };
  public String notes() default "";
}



/*
public interface FieldAccessor extends sun.reflect.FieldAccessor {
  @Override
  Object get(final Object p0) throws IllegalArgumentException;
  boolean getBoolean(final Object p0) throws IllegalArgumentException;
  byte getByte(final Object p0) throws IllegalArgumentException;
  char getChar(final Object p0) throws IllegalArgumentException;
  short getShort(final Object p0) throws IllegalArgumentException;
  int getInt(final Object p0) throws IllegalArgumentException;
  long getLong(final Object p0) throws IllegalArgumentException;
  float getFloat(final Object p0) throws IllegalArgumentException;
  double getDouble(final Object p0) throws IllegalArgumentException;

  void set(final Object p0, final Object p1) throws IllegalArgumentException, IllegalAccessException;
  void setBoolean(final Object p0, final boolean p1) throws IllegalArgumentException, IllegalAccessException;
  void setByte(final Object p0, final byte p1) throws IllegalArgumentException, IllegalAccessException;
  void setChar(final Object p0, final char p1) throws IllegalArgumentException, IllegalAccessException;
  void setShort(final Object p0, final short p1) throws IllegalArgumentException, IllegalAccessException;
  void setInt(final Object p0, final int p1) throws IllegalArgumentException, IllegalAccessException;
  void setLong(final Object p0, final long p1) throws IllegalArgumentException, IllegalAccessException;
  void setFloat(final Object p0, final float p1) throws IllegalArgumentException, IllegalAccessException;
  void setDouble(final Object p0, final double p1) throws IllegalArgumentException, IllegalAccessException;
}
*/

public class Reflect {
  
  public static class JREAccess {
    
    
    public static final String TAG = JREAccess.class.getSimpleName();
    public static boolean LOGVV = Log.isLoggable(Log.SEV_VERBOSE)
      && Boolean.parseBoolean(System.getProperty("x-verbose", "false"));
    
    static Map<String, Class<?>>[] accessorClasses
         = (Map<String,Class<?>>[]) new Map<?, ?>[]{ null, null };
    
    static final int MAP_STATIC = 0;
    static final int MAP_INSTANCE = 1;
    
    public static Map<String, Class<?>> getAccessorClasses(final boolean isStatic) 
    {
      final int mapIndex = (isStatic) ? MAP_STATIC : MAP_INSTANCE;
      final String staticStr = isStatic? "Static": "";
      return
          (accessorClasses[mapIndex] != null)
        ?  accessorClasses[mapIndex]
        : (accessorClasses[mapIndex] = RealArrayMap.toMap(
          "Boolean", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sBooleanFieldAccessorImpl", staticStr)),
          "Byte", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sByteFieldAccessorImpl", staticStr)),
          "Character", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sCharacterFieldAccessorImpl", staticStr)),
          "Double", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sDoubleFieldAccessorImpl", staticStr)),
          "Float", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sFloatFieldAccessorImpl", staticStr)),
          "Integer", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sIntegerFieldAccessorImpl", staticStr)),
          "Long", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sLongFieldAccessorImpl", staticStr)),
          "Object", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sObjectFieldAccessorImpl", staticStr)),
          "Short", DexVisitor.classForName(String.format(
            "sun.reflect.UnsafeQualified%sShortFieldAccessorImpl", staticStr))
        ));
    }
    
    static String getAccessorTypeNameSuffix(final String simpleBoxedClassNameKey) {
      switch (simpleBoxedClassNameKey) {
        case "Integer":   return "Int";
        case "Character": return "Char";
        case "Object":    return "";
        default:          return simpleBoxedClassNameKey;
      }
    }
      
    public static Object getfldval(final Object instOrCls, final String name) {
      return accessfldval(
        notNull(instOrCls, "instOrCls == null"), // instanceOrClassWithField
        notNull(name, "name == null"), // fieldName
        null, // newValue (N/A for get-field)
        false // isSet
      );
    }
    
    public static Object setfldval(final Object instOrCls, final String name,
      final Object newValue)
    {
      return accessfldval(
        notNull(instOrCls, "instOrCls == null"), // instanceOrClassWithField
        notNull(name, "name == null"), // fieldName
        newValue, // newValue (N/A for get-field)
        true // isSet
      );
    }
    
    public static Object accessfldval(final Object instanceOrClassWithField,
      final String fieldName, final Object newValue, final boolean isSet)
    {
      final Object instOrCls =
        notNull(instanceOrClassWithField, "instanceOrClassWithField == null");
      final String name = notNull(fieldName, "name == null");
      
      final Class<?> clsForField = dumpMembers.getClass(instOrCls);
      if (LOGVV) Log.v(TAG, "clsForField: Class<%s>", clsForField);
      final Object inst = (clsForField.isInstance(instOrCls)) ? instOrCls : null;
      if (LOGVV) Log.v(TAG, "inst: (%s) %s", ClassInfo.getSimpleName(inst), inst);
      final Field fld = Reflect.getfld(clsForField, name);
      if (LOGVV) Log.v(TAG, "fld: %s", dumpMembers.colorize(fld));
      return accessfld(inst, fld, newValue, isSet);
    }
      
    public static Object accessfld(final Object inst, final Field field,
      final Object newValue, final boolean isSet)
    {
      final Field fld = notNull(field, "field == null");
      final boolean isStatic = (fld.getModifiers() & Modifier.STATIC) != 0;
      try {
        final Class<?> valCls = fld.getType().isPrimitive()
          ? ClassUtils.primitiveToWrapper(fld.getType())
          : fld.getType();
        if (LOGVV) Log.v(TAG, "valCls: %s", dumpMembers.typeToString(valCls));
        
        
        if (LOGVV) Log.v(TAG, "isStatic: %s", isStatic);
        
        final Map<String, Class<?>> classes = getAccessorClasses(isStatic);
        final String key = classes.containsKey(valCls.getSimpleName())
          ? valCls.getSimpleName()
          : "Object";
        
        final Class<?> accessorClass = classes.get(key);
        final Object accessor = newInstance(accessorClass, fld, false);
        if (LOGVV) Log.v(TAG, "accessor: %s", accessor);
        
        final String accessorMethodName = String.format(
          (isSet? "set%1$s": "get%1$s"), getAccessorTypeNameSuffix(key));
        if (LOGVV) Log.v(TAG, "accessorMethodName: %s", accessorMethodName);
        
        
        final Class<?>[] paramTypes;
        
        if (isSet) {
          final Class<?> valueParamType;
          switch (accessorMethodName) {
            case "set":
              valueParamType = Object.class;
              break;
            default:
              valueParamType = fld.getType().isPrimitive()
                 ? fld.getType()
                 : ClassUtils.wrapperToPrimitive(fld.getType());
              break;
          }
          if (LOGVV) Log.v(TAG, "valueParamType: %s", valueParamType);
          paramTypes = new Class<?>[]{ Object.class, valueParamType };
        } else {
          paramTypes = new Class<?>[]{ Object.class };
        } 
        
        if (LOGVV) Log.v(TAG, "paramTypes: %s", Arrays.toString(paramTypes));
        
        Method accessorMethod = null;
        Class<?> searchCls = accessorClass;
        do {
          try {
            accessorMethod = accessorClass.getDeclaredMethod(
              accessorMethodName, paramTypes
            );
          } catch (NoSuchMethodException nsme) {
            if (searchCls.getSuperclass() == Object.class) {
              throw Reflector.Util.sneakyThrow(nsme);
            }
          }
        } while (accessorMethod == null && 
          (searchCls = searchCls.getSuperclass()) != null);
        final Method method = accessorMethod;
        
        if (LOGVV) Log.v(TAG, "accessorMethod: %s", accessorMethod);
        if (LOGVV) Log.v(TAG, "accessorMethod: %s", Debug.ToString(accessorMethod));
        System.setSecurityManager(null);
        final Boolean isAccessible = AccessController.doPrivileged(
          new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
              try {
                method.setAccessible(true);
                return method.isAccessible();
              } finally {
                if (LOGVV) Log.v(
                  TAG, "Inside %s; thread: %s; cl: %s", 
                  this.getClass().getEnclosingMethod().toGenericString(),
                  Thread.currentThread(),
                  Thread.currentThread().getContextClassLoader()
                );
                System.setSecurityManager(null);
              }
            }
          }
        );
        if (LOGVV) Log.v(TAG, "isAccessible: %s", (Object) isAccessible);
        if (((Object) isAccessible) == null ||
           ((Boolean) isAccessible).booleanValue() == false)
        {
          accessorMethod.setAccessible(true);
        }
        final Object returnValue = accessorMethod.invoke(
          accessor,
          ((Object[]) 
            ((isSet)
              ? new Object[]{ inst, newValue }
              : new Object[]{ inst }))
        );
        return (isSet) ? accessor : returnValue;
      } catch (final Throwable e) {
        errors.add(e);
        throw new RuntimeException(String.format(
          "Failed to %6$s %s field (%s) of class: %s, " +
          "for instance of type: %s: %s",
          isStatic? "static": "instance",
          dumpMembers.colorize(fld),
          ClassInfo.getGenericTypeName(fld.getDeclaringClass()),
          (inst != null) ? ClassInfo.getGenericTypeName(inst.getClass()) : null,
          e,// #5
          (isSet) ? dumpMembers.colorize("SET", "1;31")
                  : dumpMembers.colorize("GET", "1;35")
        ), e);
      }
    }
  }
  
  
  public static List<Throwable> errors = new ArrayList<Throwable>();
  
  static void d(String fmt, Object... args) {
    System.err.printf(fmt, (Object[]) args);
  }
    
  public static final UnsafeUtil uu = new UnsafeUtil();
  public static final Unsafe u = ClassPathUtil2.getUnsafe();

  static final String DPL = "dalvik.system.DexPathList";
  public static final Class<?> dexPathListCls;
  static final String DPL_ELEM = DPL.concat("$Element");
  public static final Class<?> elementCls;  
  public static final boolean noAccessCheck = true;
  public static final Map<Integer, Field> fieldCache
            = new HashMap<Integer, Field>(384);
  public static final Field fieldMods;
  public static final Method classForName;
  public static final Method constructNative;
  public static final Field  ctorSlot;
  // NEEDED BY oscCtor ... oscNewInstance below
  public static Method getDecl;
  public static Constructor<ObjectStreamClass> oscCtor;
  public static Method getCtorId;
  public static Method setCtor;
  public static Method setClass;
  public static Method setName;
  public static Method oscNewInstance;
  
  public static boolean JRE = CollectionUtil.isJRE();
  public static Field FLD_MODIFIERS;
  public static int ACC_FINAL = Modifier.FINAL;
  static {
    if (JRE) {
      try {
        (FLD_MODIFIERS = Field.class.getDeclaredField("modifiers")
        ).setAccessible(true);
      } catch (final Throwable e) {
        throw new AssertionError(e);
      }
    }
  }
  
  public static boolean setWritable(Field fld) {
    return !JRE || (Modifier.FINAL & fld.getModifiers()) == 0;
  }
  
  public static String _debugProp_ = System.getProperty("debug");  
  public static boolean DEBUG = ("true".equals(_debugProp_));
  
  static {
    ClassLoader LOADER = Thread.currentThread().getContextClassLoader();
    Class<?>         _dexPathListCls = null,
                     _elementCls = null;
    AccessibleObject _constructNative = null,
                     _classForName = null,
                     _ctorSlot = null,
                     _fieldMods = null;
    try {
      if (!JRE) {
        try {
          _dexPathListCls = Class.forName(
            "dalvik.system.DexPathList", false, LOADER
          );
        } catch (Throwable e) { e.printStackTrace(); }
        try {
          _elementCls = Class.forName(
            "dalvik.system.DexPathList$Element", false, LOADER
          );
        } catch (Throwable e) { e.printStackTrace(); }
        try {
          (_ctorSlot = Constructor.class.getDeclaredField("slot")
          ).setAccessible(true);
        } catch (Throwable e) { e.printStackTrace(); }
      }
      if (JRE) {
        String fname = "modifiers";
        try {
          _fieldMods = Field.class.getDeclaredField(fname);
          _fieldMods.setAccessible(true);
        } catch (Error iae) {
          iae.printStackTrace();
        } catch (NoSuchFieldException nsfe) {
          fname = "accessFlags";
          try {
            _fieldMods = Field.class.getDeclaredField(fname);
            _fieldMods.setAccessible(true);
          } catch (Error iae2) {
            iae2.printStackTrace();
          } catch (NoSuchFieldException nsfe2) {
            nsfe2.printStackTrace();
          }
        }
      }
      try {
        if (!JRE) {
          (_classForName = Class.class.getDeclaredMethod("classForName",
            String.class, Boolean.TYPE, ClassLoader.class)
          ).setAccessible(true);
        } else {
          (_classForName = Class.class.getDeclaredMethod("forName",
            String.class, Boolean.TYPE, ClassLoader.class)
          ).setAccessible(true);
        }
      } catch (Throwable e) {
        errors.add(e);
        e.printStackTrace();
      }
      if (!JRE) {
        try {
          (_constructNative = Constructor.class.getDeclaredMethod(
            "constructNative", Object[].class, Class.class,
            Class[].class, Integer.TYPE, Boolean.TYPE)
          ).setAccessible(true);
        } catch (Throwable e) {
          errors.add(e);
          if (e instanceof ClassFormatError) {
            e.printStackTrace(System.err);
            e.printStackTrace(System.out);
            throw (ClassFormatError) e;
          }
        }
      }
      // INITIALIZE getDecl BEFORE using getMember!
      try {
        if (JRE) {
          (getDecl = Reflect.class.getDeclaredMethod(
            "getDeclaredConstructorOrMethod", Class.class, String.class, 
            Class[].class)).setAccessible(true);
        } else {
          (getDecl = Class.class.getDeclaredMethod(
            "getDeclaredConstructorOrMethod", Class.class, String.class, 
            Class[].class)).setAccessible(true);
        }
      } catch (Throwable e) { e.printStackTrace(); }
    } catch (Throwable ex) { 
      ex.printStackTrace();
      if (ex.getCause() != null) ex.getCause().printStackTrace();
    }
    
    dexPathListCls = (Class<?>) _dexPathListCls;
    elementCls = (Class<?>) _elementCls;
    ctorSlot = (Field) _ctorSlot;
    fieldMods = (Field) _fieldMods;
    classForName = (Method) _classForName;
    constructNative = (Method) _constructNative;
    
    // REQUIRE getDecl to be initialized:
    if (!JRE) {
      oscCtor = getMember(ObjectStreamClass.class, "<init>");
      getCtorId = getMember(
        ObjectStreamClass.class,"getConstructorId",Class.class);
      setCtor = getMember(
        ObjectStreamClass.class, "setConstructor", Long.TYPE);
      setClass = getMember(
        ObjectStreamClass.class, "setClass", Class.class);
      setName = getMember(
      ObjectStreamClass.class, "setName", String.class);
      oscNewInstance = getMember(ObjectStreamClass.class,
        "newInstance", Class.class, Long.TYPE);
    }
  }
  
  
  public static Member getDeclaredConstructorOrMethod(Class<?> cls, 
  String name, Class<?>[] parameterTypes)
  {
    if ("<init>".equals(name)) {
      Class<?> c = cls;
      Constructor<?> ctor = null;
        do {
          try {
            ctor = c.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            return ctor;
          } catch (ReflectiveOperationException ex) {
            System.err.println(ex);
          }
        } while ((c = c.getSuperclass()) != null);
        return null;
    } else {
      Class<?> c = cls;
      Method md = null;
        do {
          try {
            md = c.getDeclaredMethod(name, parameterTypes);
            md.setAccessible(true);
            return md;
          } catch (ReflectiveOperationException ex) {
            System.err.println(ex);
          }
        } while ((c = c.getSuperclass()) != null);
        return null;
    }
  }
  
  public static Member findMember(Class<?> startCls, String name,
  Class<?>... parameterTypes)
  {
    Class<?> cls = startCls;
    do {
      if ("<init>".equals(name)) {
        Class<?> c = cls;
        Constructor<?> ctor = null;
        try {
          Class<?> sc = null;
          do {
            try {
              ctor = c.getDeclaredConstructor(parameterTypes);
            } catch (ReflectiveOperationException ex) {
              ctor = findMostSpecificMember(
                parameterTypes, c.getDeclaredConstructors()
              );
            }
            if (ctor != null) {
              ctor.setAccessible(true);
              return ctor;
            }
            sc = c.getSuperclass();
            if (sc == Object.class &&
                startCls != Object.class &&
               !startCls.isInterface())
            {
              for (final Constructor<?> curCtor:
                   startCls.getDeclaredConstructors())
              {
                curCtor.setAccessible(true);
                return curCtor;
              }
            }
          } while (sc != null);
          return null;
        } catch (Throwable e) {
          errors.add(e);
          e.printStackTrace();
          return null;
        }
      } else {
        Class<?> c = cls;
        Method md = null;
        try {
          do {
            try {
              md = c.getDeclaredMethod(name, parameterTypes);
            } catch (ReflectiveOperationException ex) {
              md = findMethod(c, name, parameterTypes);
            }
            if (md != null) {
              md.setAccessible(true);
              return md;
            }
          } while ((c = c.getSuperclass()) != null);
          return null;
        } catch (Throwable e) {
            errors.add(e);
            e.printStackTrace();
          return null;
        }
      }
    } while ((cls = cls.getSuperclass()) != null);
    //return null;
  }
  
  
  
  //static Object[] cnArgs = new Object[5];
  @Extension
  public static <T> 
  T newInstance(final ClassIdentifier ci, final Object... args) 
  {
    return newInstance(
      (Class<T>) ci.getTargetClass(), (Object[]) args
    );
  }
  
  
  @Extension
  public static <T> T newInstance(Class<T> declaringClass, Object... args) {
    final Constructor<T>[] ctors = (Constructor<T>[]) (Constructor<?>[])
      declaringClass.getDeclaredConstructors();
    
    final Constructor<T> ctor = Factory.findBestMatch(ctors, args);
    
    ReflectiveOperationException roe = null;
    
    if (ctor != null) {
      try {
        ctor.setAccessible(true);
        return (T) ctor.newInstance(args);
      } catch (final ReflectiveOperationException e) {
        roe = e;
      }
    }
    
    try {
      final Class<?>[] argumentTypes = new Class<?>[args.length];
      
      for (int i=0, nArgs = args.length; i<nArgs; ++i) {
        argumentTypes[i]
          = (args[i] != null) ? args[i].getClass() : null;
      }
      final Constructor<?> ctor2 = Reflect.findMostSpecificMember(
        // idealMatch,
        (Class<?>[]) argumentTypes,
        // M[] members
        (Constructor<?>[]) declaringClass.getDeclaredConstructors() 
      );
      ctor2.setAccessible(true);
      return (T) ctor2.newInstance(args);
    } catch (InvocationTargetException ex) {
      throw Reflector.Util.sneakyThrow(ex.getTargetException());
    } catch (UndeclaredThrowableException ex) {
      Throwable t = (
        ((UndeclaredThrowableException) ex).getUndeclaredThrowable()
      );
      if (roe != null) t.addSuppressed(roe);
      t.printStackTrace();
      return null;
    } catch (ReflectiveOperationException t) {
      if (roe != null) t.addSuppressed(roe);
      t.printStackTrace();
      return null;
    }
  }
  
  
  
  
  public static <M extends Member> 
  M getMember(Class<?> cls, String name, Class<?>... pTypes) {
    Class<?> c = cls;
    do {
      try {
        if (1 == 2) throw new NoSuchMethodException();
        if (1 == 3) throw new NoSuchMethodError("");
        
        if (!JRE) {
          Member mb = (Member) getDecl.invoke(null, c, name, pTypes);
          ((AccessibleObject) mb).setAccessible(true);
          return (M) mb;
        } else {
          Member mb = Reflect.getDeclaredConstructorOrMethod(c, name, pTypes);
          ((AccessibleObject) mb).setAccessible(true);
          return (M) mb;
        }
      } catch (RuntimeException | NoSuchMethodException | NoSuchMethodError e) {
        if (e instanceof Error) e.printStackTrace();
        final Class<?> sc = c.getSuperclass();
        if (sc == Object.class && cls != Object.class && !cls.isInterface()
            && "<init>".equals(name))
        {
          for (final Constructor<?> ct: cls.getDeclaredConstructors()) {
            ct.setAccessible(true);
            return (M) (Object) ct;
          }
        }
      } catch (Throwable t) {
        errors.add(t);
        t.printStackTrace();
        break;
      }
    } while (c != null);
    return null;
  }
  
  public static <T> T get(Object target, String name) {
    if (target instanceof bsh.ClassIdentifier) {
      target = ((bsh.ClassIdentifier) target).getTargetClass();
    }
    return (T) getfldval(
      target instanceof Class<?>? null: target, 
      target instanceof Class<?>
        ? (Class<?>) target
        : target.getClass(), 
      name
    );
  }
  
  public static <T>
  Collection<? extends T> get(Object target, Class<T> fieldType)
  {
    Class<?> cls = (target instanceof Class<?>)
      ? (Class<?>) target
      : (Class<?>) target.getClass();
    if (target == cls) target = null;
    
    Class<?> c = cls;
    Collection<? super Object> ret = new ArrayList<Object>();
    try {
      do {
        Field[] flds = c.getDeclaredFields();
        for (Field fld: flds) {
          if (target == null
          && (fld.getModifiers() & Modifier.STATIC) == 0) {
            continue;
          }
          Object val = null;
          try {
            fld.setAccessible(true);
            setWritable(fld);
            val = fld.get(target);
          } catch (ReflectiveOperationException ex) { 
            System.err.println(fld + ": " + ex);
            continue;
          }
          if (val == null) continue;
          if (! fieldType.isAssignableFrom(val.getClass())) {
            continue; 
          }
          ret.add(val);
        }
        c = c.getSuperclass();
      } while (c != Object.class && c != null);
    } catch (Throwable e) {
            errors.add(e);
            e.printStackTrace();
    }
    return (Collection<? extends T>) (Object) ret;
  }
  
  
  public static <R>
  R invoke2(Object source, String name, Object... args) {
    Object recvr; // = source;
    Class<?> cls; // = source != null? source.getClass(): null;
    if (source instanceof ClassIdentifier) {
      cls = ((ClassIdentifier)source).getTargetClass();
    }
    if (source instanceof Class<?>) {
      recvr = (source != Class.class)? null: Class.class; 
      cls = (Class<?>) source;
    } else {
      recvr = source;
      cls = (recvr != null)? recvr.getClass(): Object.class;
    }
    
    if (! cls.isInstance(recvr)) recvr = null;
    return (R) (
      (recvr != null)
        ? (R) Reflector.invokeInstanceMethod(recvr, name)
        : (R) Reflector.invokeStaticMethod(cls, name, args)
    );
  }
  
  @ExceptionPropagation(
    value = ALWAYS, 
    changes = UNWRAP,
    changed = { InvocationTargetException.class },
    output = ALWAYS
  )
  public static <R> R invoke(Member mb, Object... args) {
  
    try {
      if (mb instanceof Constructor) {
        return (R) ((Constructor<?>) mb).newInstance(args);
      }
      Object[] newArgs;
      Object receiver;
      
      if (args.length == 0) {
        newArgs = new Object[0];
        receiver = null;
      } else {
        receiver = args[0];
        
        if (args.length == 2 && args[1] instanceof Object[]) {
          newArgs = (Object[]) args[1];
        } else {
          newArgs = new Object[args.length - 1];
          System.arraycopy(args, 1, newArgs, 0, args.length - 1);
        }
      }      
      return (R) ((Method) mb).invoke(receiver, (Object[]) newArgs);
    } catch (Throwable e) {
            errors.add(e);
            e.printStackTrace();
      if (e instanceof InvocationTargetException) {
        e = ((InvocationTargetException) e).getTargetException();
      }
      e.printStackTrace();
      Reflector.Util.sneakyThrow(e);
    }
    return null;
  }
    
  public static <T> T allocateInstance(Class<T> cls)  {
    if (JRE) return CollectionUtil.tryAllocInstance(cls);
    // ObjectStreamClass osc = ObjectStreamClass.lookupAny(cls);
    Long ctId;
    T obj;
    try {
      if (getCtorId != null && oscNewInstance != null) {
        if (! getCtorId.isAccessible()) {
          getCtorId.setAccessible(true);
        }
        ctId = (Long) getCtorId.invoke(null, Object.class);
        //setCtor.invoke(osc, ctId);
        if (! oscNewInstance.isAccessible()) {
          oscNewInstance.setAccessible(true);
        }
        obj = (T) oscNewInstance.invoke(null, cls, ctId);            
        return obj;
      }
    } catch (Throwable e0) { 
      e0.printStackTrace();
    }
    try {
      // System.err.printf("Reflect.allocateInstance( %s.class )\n", cls);
      obj = (T) UnsafeUtil.allocateInstance(cls);
      // System.err.printf("OK: %s\n", cls);
      return obj;
    } catch (Throwable e) { 
      e.printStackTrace();
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static <T> T newInstance(final ClassIdentifier ci,
  final Class<?>[] parameterTypes, final Object... args) 
  {
    return newInstance(
      (Class<T>) ci.getTargetClass(), parameterTypes,
      args
    );
  }
  
  public static <T> T newInstance(final Class<T> declaringClass, 
  final Class<?>[] parameterTypes, Object... args) 
  {
    boolean redirect = false;
    for (int i=0; i<parameterTypes.length; i++) {
      if (args[i] == null) {
        if (parameterTypes[i].isPrimitive()) {
          redirect = true; 
          break;
        } else continue; 
      }
      if (! parameterTypes[i].isAssignableFrom(
        args[i].getClass()))
      {
        redirect = true; 
        break;
      }
    }
    if (redirect) {
      System.err.printf(
        "[INFO] Redirecting to <T> T Reflect.newInstance("
        + "Class<T> declaringClass, Object... args) ...\n"
      );
      return newInstance(
        declaringClass, parameterTypes, (Object[]) args
      );
    }
    Constructor<T> ctor = null;
    do {
      try { 
        ctor = (Constructor<T>) (Object)
         declaringClass.getDeclaredConstructor(parameterTypes);
      } catch (NoSuchMethodException ex) {
        Reflector.Util.sneakyThrow(ex);
        break;
      } catch (NoSuchMethodError ex) {
        ex.printStackTrace();
        break;
      }
      int slot = -1;
      try {
        ctor.setAccessible(true); 
        slot = ((Integer) ctorSlot.get(ctor)).intValue();
        //System.err.println(dumpMembers.colorize(ctor));
        if (args.length == 1 && args[0] instanceof Object[]) {
          args = (Object[]) args[0];
        }
        T obj = (T) constructNative.invoke(ctor,
          args, 
          declaringClass, 
          parameterTypes,
          Integer.valueOf(slot), 
          Boolean.valueOf(noAccessCheck)
        );
        return obj;
      } catch (InvocationTargetException ex) {      
        Reflector.Util.sneakyThrow(ex.getTargetException());
        break;
      } catch (ReflectiveOperationException ex) {
        ex.printStackTrace();
        break;
      }
    } while (false);
    try {
      T obj = (T) u.allocateInstance(declaringClass);
      return obj;
    } catch (Throwable e) { 
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  @Extension public static
  Field getfld(Object o, Class<?> cls, final String pName) {
    if (cls == null && o == null) return null;
    else if (cls == null && o != null) {
      cls = o.getClass();
    } else if (cls != null && o == null) {
      // static
    }
    Class<?> sup = cls;
    int hash = cls.hashCode() * 37 + (pName.hashCode() >>> 5);
    /*** /
    Integer hashv = Integer.valueOf(hash);
    /* */
    Field field = null;
    if (fieldCache.containsKey(hash)) {
      field = fieldCache.get(hash);
    } else {
      outer:
      do {
        Field[] fields = sup.getDeclaredFields();
        for (Field fld: fields) {
          String name = fld.getName();
          if (pName.equals(name)) {
            (field = fld).setAccessible(true);
            break outer;
          }
        }
        sup = sup.getSuperclass();
      } while (sup != null);
      fieldCache.put(hash, field);
    }
    return field;
  }
  
  @Extension
  public static Field getfld(final Class<?> cls, String pName) {
    return getfld((Object) null, cls, pName);
  }
  
  
  
  @Extension
  public static <T>
  T getfldval(Object o, Class<?> cls, String pName) {
    if (cls == null && o == null) return null;
    
    if (cls == null && o != null) {
      if (o instanceof Class && o != Class.class) {
        cls = (Class<?>) o;
        o = null;
      }
    }
    Field field = getfld(o, cls, pName);
    if (field == null) {
      /*System.err.printf(
        "[WARN] No such field: %s#%s\n", cls.getName(), pName
      );*/
      return null;
    }
    boolean isStatic = (field.getModifiers() & Modifier.STATIC) != 0;
    try {
      T val = (T) field.get(isStatic? null: o);
      return val;
    } catch (Throwable e) { 
      e.printStackTrace();
      return null;
    }
  }
  
  @Extension
  public static <T> T getfldval(Object target, String fieldName) {
    return getfldval(target, fieldName, false);
  }
  
  @Extension
  public static <T> T getfldval(Object o, String fieldName,
  boolean allowNull)
  {
    return getfldval(
      o, fieldName, allowNull, (boolean) false // strict
    ); 
  }
  
  @Extension
  public static <T> T getfldval(Object o, String fieldName,
  boolean allowNull, boolean strict)
  {
    Class<?> cls = dumpMembers.getClass(o);
    if (cls == null && o == null) {
      if (allowNull || !strict) return null;
      throw new IllegalArgumentException("target == null");
    }
    
    if (cls == null && o != null) {
      if (o instanceof Class && o != Class.class) {
        cls = (Class<?>) o;
        o = null;
      }
    }
    
    Field field = getfld(o, cls, fieldName);
    if (field == null) {
      if (allowNull || !strict) return null;
      throw new IllegalArgumentException(
        "field == null", new NoSuchFieldException(String.format(
          "%s on class %s (o = %s)", fieldName, cls, o
        ))
      );
    }
    
    boolean isStatic = (field.getModifiers() & Modifier.STATIC) != 0;
    try {
      T val = (T) field.get(isStatic? null: o);
      return val;
    } catch (Throwable e) { 
      e.printStackTrace();
      return null;
    }
  }
  
  /*
  public static Object setfldval(Object o, String pName, Object value) {
    return setfldval(o, null, pName, value);
  }*/
  @Extension
  public static <T>
  T setfldval(Object o, Class cls, String pName, T value) {
    System.err.printf(    
      "setfldval(%s, %s, %s, %s)\n",
      Dumper.tryToString(o),
      Dumper.tryToString(cls),
      pName,
      Dumper.tryToString(value)
    );
    Field field = getfld(o, cls, pName);
    if (field == null) {
      System.err.printf(
        "[WARN] No such field: %s#%s\n"
        + "  from Reflect.setfldval(%s)\n", 
        cls.getName(), pName, StringUtils.join(
          new Object[]{ o, cls, pName, value }, ", "
        )
      );
      return value;
    }
    boolean isStatic 
      = (field.getModifiers() & Modifier.STATIC) != 0;
    Throwable[] errs = new Throwable[2];
    try {
      field.setAccessible(true);
      if (!setWritable(field)) {
        JREAccess.setfldval(
          (isStatic? field.getDeclaringClass(): o),
          field.getName(),
          value
        );
      } else {
        field.set(isStatic? null: o, value);
      }
      return value;
    } catch (Throwable e) {
      errs[0] = e;
    }
    
    if (o instanceof Class) {
      cls = ((Class<?>) o).getClass();
      o = (Class<?>) o;
      field = getfld(o, cls, pName);
      try {
        field.setAccessible(true);
        setWritable(field);
        field.set(isStatic? null: o, value);
        return value;
      } catch (Throwable e) {
        errs[1] = e;
      }
    }
    
    errs[0].printStackTrace();
    errs[1].printStackTrace();
    return value;
  }
  
  @Extension
  public static 
  Object setfldval(Object subject, String fname, 
  Object newv) 
  { try {
      Class cls_ = null; 
      if (subject instanceof ClassIdentifier) {
        subject = ((ClassIdentifier) subject)
          .getTargetClass();
      }
      if (subject instanceof Class 
      && ! subject.equals(Class.class)) { 
        cls_ = (Class) subject; 
        subject = null;
      } else if (subject != null) { 
        cls_ = subject.getClass(); 
      } else { 
        System.err.println("No idea."); 
        return subject;
      } 
      
      //Field f = null; 
      while (cls_ != null) { 
       for (Field f: cls_.getDeclaredFields()) {
         if (!f.getName().equals(fname)) continue; 
        f.setAccessible(true);
        final boolean isStatic = (Modifier.STATIC & f.getModifiers()) != 0;
        if (!setWritable(f)) {
          JREAccess.setfldval(
            (isStatic? f.getDeclaringClass(): subject),
            f.getName(),
            newv
          );
        } else {
          f.set(isStatic? null: subject, newv);
        }
        return newv;
      }
      cls_ = cls_.getSuperclass(); 
      if (cls_ == null || cls_ == Object.class) {
        return false;
      } 
    }
  } catch (Throwable e) { 
    System.err.printf(
      "%s [setfldval on %s.%s]n", e, subject, fname
    );
  }
  return false;
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
    } else if (o instanceof ClassIdentifier) {
      cls = ((ClassIdentifier) o).getTargetClass();
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
  
  @Extension
  public static Method findMethod
  (Class<?> cls, String name, Class<?>... paramClzs) 
  {
    return 
      findMethod(new ClassTarget(cls, null), name, paramClzs);
  }
  
  /*
  @Extension
  public static Method findMethod
  (Class<Class> cls, String name, Class<?>... paramClzs) 
  {
    return 
      findMethod(
        new ClassTarget(Class.class, cls), name, paramClzs);
  }
  */
  @Extension
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
  
  @Extension
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
        errors.add(e2);
        return null; // throw new RuntimeException(e2);
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
        errors.add(e2);
        return null; // throw new RuntimeException(e2);
      }
      sc = sc.getSuperclass();
    } // while 2
  
    return null; // throw new RuntimeException(firstEx);
  }
  
  /*
  public static <T> Object invoke
  (Method m, T thisObj, Object... params) 
  {
    try {
      if (! m.isAccessible()) m.setAccessible(true);
      return m.invoke(thisObj, params);
    } catch (Exception ex) { throw new RuntimeException(ex); }
  }
  */
  
  static Void DEFAULT_VOID = Reflect.allocateInstance(Void.class);
  static String DEFAULT_STRING = "".intern();
  static BigInteger DEFAULT_BIG_INTEGER = BigInteger.valueOf(0);
  static BigDecimal DEFAULT_BIG_DECIMAL = BigDecimal.valueOf(0L);
  static AtomicInteger DEFAULT_ATOMIC_INTEGER = new AtomicInteger(0);
  static AtomicLong DEFAULT_ATOMIC_LONG = new AtomicLong(0L);
  
  @ExceptionPropagation(NEVER)
  public static <T> T defaultValue(Class<T> cls) {
    if (String.class.equals(cls)) return (T) DEFAULT_STRING;
    if (cls.isArray()) return (T) Array.newInstance(cls, 0);
    
    // if (! ClassUtils.isPrimitiveOrWrapper(cls)) return null;
    if (cls.isPrimitive()) cls=(Class<T>)ClassUtils.primitiveToWrapper(cls);
    if (Boolean.class.equals(cls)) return (T) Boolean.valueOf(false);
    if (Byte.class.equals(cls)) return (T) Byte.valueOf((byte) 0);
    if (Character.class.equals(cls)) return (T) Character.valueOf((char) 0);
    if (Double.class.equals(cls)) return (T) Double.valueOf(0D);
    if (Float.class.equals(cls)) return (T) Float.valueOf(0f);
    if (Integer.class.equals(cls)) return (T) Integer.valueOf(0);
    if (Long.class.equals(cls)) return (T) Long.valueOf(0L);
    if (Short.class.equals(cls)) return (T) Short.valueOf((short) 0);
    if (Void.class.equals(cls)) return (T) DEFAULT_VOID;
    
    if (BigInteger.class.equals(cls)) return (T) DEFAULT_BIG_INTEGER;
    if (BigDecimal.class.equals(cls)) return (T) DEFAULT_BIG_DECIMAL;
    if (AtomicInteger.class.equals(cls)) return (T) DEFAULT_ATOMIC_INTEGER;
    if (AtomicLong.class.equals(cls)) return (T) DEFAULT_ATOMIC_LONG;
    
    try {
      Method valueOf = Reflect.findMethod(cls, "valueOf");
      if (valueOf != null) {
        valueOf.setAccessible(true);
        Class<?>[] pTypes = valueOf.getParameterTypes();
        Object arg = defaultValue(pTypes[0]);
        if (arg == null 
        
        && ! pTypes[0].getName().startsWith("java.lang.Class")
        && ! pTypes[0].getName().startsWith("java.lang.reflect")) {
          try {
            System.err.println(cls);
            arg = UnsafeUtil.allocateInstance(cls);
          } catch (Throwable e) { }
        }
        arg = valueOf.invoke(null, arg);
      } 
    } catch (Throwable e) {
      e.printStackTrace();
      errors.add(e);
      if (e instanceof ClassFormatError) {
        e.printStackTrace(System.err);
        e.printStackTrace(System.out);
        throw (ClassFormatError) e;
      }
    }
    return null;
    /*throw new IllegalArgumentException(String.format(
      "Missing default value for java.lang.Class<%s>", cls.getName()
    ));*/
  }
  
  @ExceptionPropagation(
    value = ALWAYS,
    changes = { EXTRA_INFO_ADDED },
    output = { IF_DEBUG_FLAGS }
  )
  public static <R> R invokeMethod(Object instanceOrClass, String name, 
  Object... args) 
  {
    if (instanceOrClass == null) return null;
    
    Class<?> tcls
       = (instanceOrClass instanceof Class<?>
      &&  instanceOrClass != Class.class)
      ? (Class<?>) instanceOrClass
      : (Class<?>) instanceOrClass.getClass();
    
    Object receiver = (tcls == instanceOrClass) ? null: instanceOrClass;
    
    return Reflect.<R>invoke(tcls, receiver, name, true, (Object[]) args);
  }
  
  @ExceptionPropagation(
    value = ALWAYS,
    changes = { EXTRA_INFO_ADDED },
    output = { IF_DEBUG_FLAGS }
  )
  public static <R> R invoke(Object obj, String name, Object... args) {
    return Reflect.<R>invokeMethod(obj, name, args);
  }
  
  
  public static
  List<URL> getResources(BaseDexClassLoader ldr, String name) 
  {
    ArrayList<URL> result = new ArrayList<URL>();
    Object pathList = getfldval(ldr, "pathList");
    Object[] dexElements = getfldval(pathList, "dexElements");
    Method findResources = findMethod(
      elementCls, "findResource", String.class);
    for (Object element: dexElements) {
      URL url = (URL) invoke(findResources, element, name);
      if (url == null) continue; 
      result.add(url);
    }
    return result;
  }
  
  public static List<URL> getResources(String name) {
    ClassLoader ldr 
      = Thread.currentThread().getContextClassLoader();
    if (ldr == null) ldr = ClassLoader.getSystemClassLoader();
    if (ldr == null) ldr = Reflect.class.getClassLoader();
    if (ldr == null) return Collections.emptyList();
    return getResources((BaseDexClassLoader) ldr, name);
  }
  
  static Field[] NO_FIELDS_ASSIGNED = new Field[0];
  
  public static 
  Field[] initByType(Object target, Object... values) {
    if (target == null) return NO_FIELDS_ASSIGNED;
    if (values == null) return NO_FIELDS_ASSIGNED;
    
    Class<?> tgtCls = target.getClass();
    Object[] altValues = null;
    if (values.length == 1  && values[0] instanceof Object[]
    && (altValues = (Object[]) values[0]).length > 0) {
      System.err.printf(
        "[WARN] initByType(target = %s@%08x, Object... values): "
        + "values is:  Object[] { Object[%d] { ... } }; "
        + "unwrapping item 0. Possible misinterpretation of "
        + "varargs by compiler.",
        tgtCls.getName(),
        System.identityHashCode(target),
        altValues.length
      );
      values = altValues;
    }
    
    List<Field> assigned = new ArrayList<Field>();
    outer:
    for (Object value: values) {
      Class<?> cls = tgtCls;
      do {
        for (Field fld: cls.getDeclaredFields()) {          
          if ((fld.getModifiers() & Modifier.STATIC) != 0) {
            // static field
            continue; 
          }
          if (! fld.getType().isAssignableFrom(value.getClass()))
          {
            // incompatible type
            continue; 
          }
          if (fld.getType() == Object.class) continue; 
          
          Object fldval = null;
          try {
            fld.setAccessible(true);
            setWritable(fld);
            fldval = fld.get(target);
            if (fldval != null) continue;
            System.err.printf(
              " |::  %s %s := %s\n",
              cls.getSimpleName(),
              System.identityHashCode(target),
              fld.getType().getSimpleName(),
              fld.getName(),
              value
            );
            setWritable(fld);
            fld.set(target, value);
            assigned.add(fld);
          } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
          }
          continue outer;
        }
        cls = cls.getSuperclass();
      } while (cls != Object.class && cls != null);
      
      /*System.err.printf(
        "initByType: %s.@%08x: "
        + "Could not find compatible field in class heirarchy "
        + "for value of type '%s'\n",
        tgtCls.getSimpleName(),
        System.identityHashCode(target),
        value.getClass()
      );*/
    }
    return assigned.toArray(new Field[0]);
  }
  
  public static Object[] findField(Object o, Class<?> type) {
    return findField(o, type, 12);
  }
  
  public static Object[] findField(Object o, Class<?> type,
  int maxdepth)
  {
    return findField(
      o, type, new HashSet<Integer>(), maxdepth, 0, false
    );
  }  
  
  

  public static Object[] findField(Object o, 
  Class<?> type, Set<Integer> visited, int maxdepth, int depth, 
  boolean multiResult) 
  { 
    if (visited == null) visited = new HashSet<Integer>();
    if (DEBUG) System.err.printf(
      "findField(Object o = %s, Class<?> type = %s, "
      + "Setn<Integer> visited (size = %s), int maxdepth = %s, "
      + "int depth = %s)\n",
       o, type, visited.size(), maxdepth, depth
    );
   
    
    if (depth > maxdepth) return null;
    Deque<Object[]> que = new ArrayDeque<Object[]>();
    que.offerFirst(new Object[]{ o, Integer.valueOf(depth) });
    ArrayList<Object> results = new ArrayList<Object>(2);
    outer:
    while (! que.isEmpty()) {
      Object[] cur = que.pollFirst();
      Object obj = cur[0];
      depth = ((Integer) cur[1]).intValue();
      if (DEBUG) System.err.printf(
        "  consider: %s @ depth = %d\n", 
        obj, depth
      );
      
      if (depth > maxdepth) continue; 
      int id = System.identityHashCode(obj);
      if (visited.contains(id)) continue; 
      visited.add(id);
      Class<?> cls = obj.getClass();
      Class<?> fldType;
      Object value = null;
      do {
        for (Field fld: cls.getDeclaredFields()) {
          if ((fld.getModifiers() & Modifier.STATIC)!=0) {
            continue;
          }
          fldType = fld.getType();
          if (ClassUtils.isPrimitiveOrWrapper(fldType)) continue;
          if (depth < maxdepth || type.isAssignableFrom(fldType))
          {
            try {
              fld.setAccessible(true);
              setWritable(fld);
              value = fld.get(obj);
              if (value == null) continue;
            } catch (ReflectiveOperationException ex) {
              ex.printStackTrace();
              continue; 
            }
          }
          int vid = System.identityHashCode(value);
          if (type == null) continue; 
          if (value == null) continue; 
          if (type.isAssignableFrom(value.getClass())) {
            results.add(fld);
            results.add(value);
            if (! multiResult) break outer;
          }
          if (depth < maxdepth && ! visited.contains(vid)) {
            que.offerFirst(
              new Object[]{ value, Integer.valueOf(depth + 1) }
            );
          }
        }
        cls = cls.getSuperclass();
      } while (cls != Object.class && cls != null);
    }
    return results.size() >= 2? results.toArray(): null;
  }
  
  
  public static <T> List<T> searchObject(Object obj, Class<T> searchFor) { 
    return searchObject(obj, searchFor, false);
  }
  
  public static <T> List<T> searchObject(Object obj, Class<T> searchFor,
  boolean staticAlso) 
  { 
    return searchObject(obj, searchFor, staticAlso, 0, 5);
  }
  
  public static class PropertyPath {
    public static final PropertyPath ROOT
      = new PropertyPath(null, "", null);
    private PropertyPath parent;
    private String name;
    private Object data;
    private String opString;
    private Field field;
    private int index;
    
    public PropertyPath(final PropertyPath parent, final String name,
      final Object data, final Field field, final int index)
    {
      this.parent = parent != null? parent: ROOT;
      this.name = name;
      this.data = data;
      this.field = field;
      this.index = index;
    }
    
    public PropertyPath(PropertyPath parent, String name, Object data) {
      this(parent, name, data, (Field) null, (int) -1);
    }
    
    public static PropertyPath root() {
      return ROOT;
    }
    
    public PropertyPath object(Object obj, Object data) {
      if (obj == null) return new PropertyPath(this, "{null}", null);
      String name;
      if (obj instanceof CharSequence) {
        name = obj.toString();
      } else {
        name = String.format(
          "{%s}", ClassInfo.typeToName(obj.getClass().getName())
        );
      }
      return new PropertyPath(this, name, data);
    }
    
    public PropertyPath field(Field fld, Object data) {
      return new PropertyPath(
        this, ".".concat(fld.getName()), data, fld, -1
      );
    }
    
    public PropertyPath index(int index, Object data) {
      return new PropertyPath(this, String.format("[%d]", index), data,
        null, index);
    }
    
    public String toString() {
      if (parent == null || parent == this || parent == ROOT) return name;
      return parent.toString().concat(this.name);
    }
    
    public String getName() {
      return this.name;
    }
    public Field getField() {
      return this.field;
    }
  }
  
  public static <T> List<Pair<PropertyPath, T>> searchObjectPath(
  final Object obj, Class<T> searchFor, boolean staticAlso,
  int depth, int maxdepth) 
  { 
    int idx = 0; 
    Deque<Object> q = new ArrayDeque<Object>(32); 
    Deque<Integer> d = new ArrayDeque<Integer>(32); 
    Set<Integer> visited = new TreeSet<Integer>(); 
    List<Object> al = new ArrayList<Object>(); 
    final PropertyPath obj_propertyPath
      = PropertyPath.root().object(obj, obj);
    Map<Integer, PropertyPath> pathMap = new HashMap<>();
    final int objHc = System.identityHashCode(obj);
    q.offerLast(obj); 
    pathMap.put(objHc, obj_propertyPath);
    d.offerLast(depth);
    visited.add(objHc);
    while (! q.isEmpty()) { 
      /*if (dots++ % 10 == 0) System.err.printf("%c[0m.", 0x1b); */
      Object o = q.pollFirst();      
      PropertyPath o_propertyPath = pathMap.get(System.identityHashCode(o));
      depth = d.pollFirst().intValue();
      int nextDepth = (o == obj)? depth: depth+1;
      if (o == null) continue; 
      Class<?> cls = o.getClass();
      boolean wantPrimitive = searchFor.isPrimitive();
      if (searchFor.isInstance(o)) {
        al.add(Pair.of(o_propertyPath, o));
      }
      if (!wantPrimitive && cls.isPrimitive()) continue;
      if (o instanceof Object[]) {
          int i = -1;
          for (final Object v: (Object[]) o) {
            final int ihcv = System.identityHashCode(v);
            PropertyPath v_propertyPath = o_propertyPath.index(++i, v);
            pathMap.put(ihcv, v_propertyPath);
            if (v == null || visited.contains(ihcv)) continue;
            if (depth <= maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(nextDepth));
            }
            visited.add(ihcv);
          }
          continue;
      }
      do {         
        for (Field f: cls.getDeclaredFields()) { 
          
          if (!staticAlso 
          && (f.getModifiers() & Modifier.STATIC) != 0) continue; 
          try {
            f.setAccessible(true); 
            Object v = f.get(o);
            final int ihcv = System.identityHashCode(v);
            PropertyPath v_propertyPath = o_propertyPath.field(f, v);
            pathMap.put(ihcv, v_propertyPath);
            if (v == null || visited.contains(ihcv)) continue;
            if (depth + 1 < maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(nextDepth));
            }
            visited.add(ihcv);
          } catch (Throwable ex) {
            errors.add(ex);
            ex.printStackTrace();
          }
        }
        cls = cls.getSuperclass(); 
      } while (cls != Object.class && cls != null);
    } 
    return (List<Pair<PropertyPath, T>>) (List<?>) al;   
  }
  
  public static <T> List<T> searchObject(Object obj, Class<T> searchFor,
  boolean staticAlso, int depth, int maxdepth)
  {
    int idx = 0;
    Deque<Object> q = new ArrayDeque<Object>(64);
    Deque<Integer> d = new ArrayDeque<Integer>(64);
    Set<Integer> visited = new TreeSet<Integer>();
    List<Object> al = new ArrayList<Object>();
    q.offerLast(obj);
    d.offerLast(depth);
    visited.add(System.identityHashCode(obj));
    while (! q.isEmpty()) {
      /*if (dots++ % 10 == 0) System.err.printf("%c[0m.", 0x1b); */
      Object o = q.pollFirst();
      depth = d.pollFirst().intValue();
      if (o == null) continue;
      Class<?> cls = o.getClass();
      if (searchFor.isAssignableFrom(cls)) {
        al.add(o);
      }
      if (cls.isPrimitive()) continue;
      if (cls.isArray()) {
        if (ClassUtils.isPrimitiveOrWrapper(cls.getComponentType())) {
          continue;
        }
        if (! cls.getComponentType().isPrimitive()) {
          for (Object v: (Object[]) o) {
            if (v == null || visited.contains(
              System.identityHashCode(v)
            )) continue;
            if (depth + 1 < maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(depth + 1));
            }
            visited.add(System.identityHashCode(v));
          }
          continue;
        }
        /*int len = Array.getLength(o);
        for (int i=0; i<len; i++) {
          Object v = Array.get(o, i);
          if (v == null || visited.contains(
            System.identityHashCode(v)
          )) continue;
          q.offerFirst(v);
          visited.add(System.identityHashCode(v));
        };*/
        //continue;
      }
      do {
        for (Field f: cls.getDeclaredFields()) {
          if (!staticAlso
          && (f.getModifiers() & Modifier.STATIC) != 0) continue;
          try {
            f.setAccessible(true);
            Object v = f.get(o);
            if (v == null || visited.contains(
              System.identityHashCode(v)
            )) continue;
            if (depth + 1 < maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(depth + 1));
            }
            visited.add(System.identityHashCode(v));
          } catch (Throwable ex) {
            errors.add(ex);
            ex.printStackTrace();
          }
        }
      } while (cls != Object.class && cls != null);
    } 
    /*System.err.printf(
      "\n\n%c[1;%02dm%d found%c[0m\n", 
      0x1b, al.size() > 0 ? 32: 30, al.size(), 0x1b
    ); */
    return (List<T>) (List<?>) al;   
  }
  
  
  public static 
  Set<Class<?>> getEntireClassHeirarchy(Class<?>... clazzes) 
  {
    Set<Class<?>> primaryClasses = new TreeSet<Class<?>>(
      new ToStringComparator(false, true)
    );
    ClassLoader ldr 
      = Thread.currentThread().getContextClassLoader();
    
    for (Class<?> cls: clazzes) {
      if (primaryClasses.contains(cls)) continue; // done already
      Class<?> clsOrSuper = cls;
      do {
        Class<?>[] nestedClasses 
            = clsOrSuper.getDeclaredClasses();
        Collections.addAll(primaryClasses, nestedClasses);
        int i = 0;
        Class<?> inner = null;
        do {
          i += 1;
          try {
            inner = (Class<?>) classForName.invoke(null,
              String.format("%s$%d", clsOrSuper, i), false, ldr
            );
            System.err.printf(
              "Found inner class: %s", inner.getName()
            );
            primaryClasses.add(inner);
          } catch (Throwable e) { 
            break;
          }
        } while (inner != null);       
        Class<?>[] interfaces = clsOrSuper.getInterfaces();
        Collections.addAll(primaryClasses, interfaces);        
        clsOrSuper = clsOrSuper.getSuperclass();
      } while (clsOrSuper != null);
    } // for cls in clazzes    
    return primaryClasses;
  }
  
  public static 
  Member findMostSpecificMethod(Class<?>[] idealMatch,
  List<Member> methods)
  {
    List<Class[]> candidateSigs = new ArrayList<Class[]>();
    List<Member> methodList = new ArrayList<Member>();
    for (Member method : methods) {
      Class<?>[] parameterTypes = getParameterTypes(method);
      methodList.add(method);
      candidateSigs.add(parameterTypes);
      if (isVarArgs(method)) {
        Class<?>[] candidateSig
          = new Class<?>[idealMatch.length];
        int j = 0;
        for (; j < parameterTypes.length - 1; j++) {
          candidateSig[j] = parameterTypes[j];
        }
        Class<?> varType = parameterTypes[j].getComponentType();
        for (; j < idealMatch.length; j++) {
          candidateSig[j] = varType;
        }
        methodList.add(method);
        candidateSigs.add(candidateSig);
      }
    }
    int match = findMostSpecificSignature(
      idealMatch, candidateSigs.toArray(new Class<?>[0][])
    );
    return (match == -1) ? null: methodList.get(match);
  }
  
  
  static final int FIRST_ROUND_ASSIGNABLE = 1;
  static final int JAVA_BASE_ASSIGNABLE = 1;
  static final int JAVA_BOX_TYPES_ASSIGABLE = 2;
  static final int JAVA_VARARGS_ASSIGNABLE = 3;
  static final int LAST_ROUND_ASSIGNABLE = 4;
  static final int BSH_ASSIGNABLE = 4;
  
  public static 
  int findMostSpecificSignature(Class<?>[] idealMatch, 
  Class<?>[][] candidates) 
  {
    for (int round = FIRST_ROUND_ASSIGNABLE; 
             round <= LAST_ROUND_ASSIGNABLE; round++) 
    {
      Class<?>[] bestMatch = null;
      int bestMatchIndex = -1;
      for (int i = 0; i < candidates.length; i++) {
        Class<?>[] targetMatch = candidates[i];
        if (Types.isSignatureAssignable(
            idealMatch, targetMatch, round)
        && ( bestMatch == null
           || Types.isSignatureAssignable(
            targetMatch, bestMatch, JAVA_BASE_ASSIGNABLE)))
        {
          bestMatch = targetMatch;
          bestMatchIndex = i;
        }
      }
      if (bestMatch != null) return bestMatchIndex;
    }
    return -1;
  }
  
  public static boolean isVarArgs(Member member) {
    if (member instanceof Constructor) {
      return ((Constructor<?>) member).isVarArgs();    
    }
    return (member instanceof Method)
      ? ((Method) member).isVarArgs()
      : false;
  }
  
  public static Class<?>[] getParameterTypes(Member member) {
    if (member instanceof Constructor) {
      return ((Constructor<?>) member).getParameterTypes();
    }
    return (member instanceof Method)
      ? ((Method) member).getParameterTypes()
      : new Class[0];
  }
  
  public static List<Member> getMembers(final Class<?> cls) {
    Class<?> sc = cls;
    List<Member> all = new ArrayList<Member>();
    
    while (sc != null) {
      Collections.addAll(all, sc.getDeclaredConstructors());
      Collections.addAll(all, sc.getDeclaredMethods());
      sc = sc.getSuperclass();
    }
    return all;
  }
  
  public static <T>
  Constructor<T> findBestMatch(Constructor<T>[] ctors, Object[] args) {
    return Factory.findBestMatch(ctors, args);
  }

  public static <M extends Member> 
  M findMostSpecificMember(Class<?>[] idealMatch, M[] members)
  {
    int match = findMostSpecificMemberIndex(idealMatch, members);
    return (match == -1) ? null : (M) members[match];
  }

  public static <M extends Member> 
  int findMostSpecificMemberIndex(Class<?>[] idealMatch, M[] members)
  {
    Class<?>[][] candidates = new Class<?>[members.length][];
    for (int i = 0; i < candidates.length; i++) {
      candidates[i] = getParameterTypes((Member) members[i]);
    }
    return findMostSpecificSignature(idealMatch, candidates);
  }
  
  public static boolean DEBUG_INVOKE = false;
  
  
  public static <R> R invoke(@Nonnull Class<?> cls, @Nullable Object inst,
  @Nonnull final String name, final Object... args) 
  {
    return Reflect.<R>invoke(cls, inst, name, true, args);
  }
  
  
  public static <R> R invoke(@Nonnull Class<?> cls, @Nullable Object inst,
  @Nonnull final String name, boolean doThrow, final Object... args)
  {
    Class<?>[] argTypes = Types.getTypes(args);
    BooleanPredicate<Member> namePred;
    WhereFilter<Member>filter = new WhereFilter.Impl<Member>(
      namePred = new 
      BooleanPredicate<Member>() {
        @Override public boolean test(Member mb) {
          return name.equals(mb.getName());
        }
      }
    );
    Member mtd = findMostSpecificMethod(
      argTypes, 
     filter.get(getMembers(cls))
    ); // mtd = findMostSpecificMethod(..)
    if (DEBUG_INVOKE) d(
      "- findMostSpecificMethod returned: %s\n",
      (mtd instanceof Method)
        ? ((Method)         mtd).toGenericString()
        : ((Constructor<?>) mtd).toGenericString()
    );
    Class<?>[] paramTypes = getParameterTypes(mtd);
    Deque<Object> mainArgs = new ArrayDeque<Object>(Arrays.asList(args));
    
    if (DEBUG_INVOKE) d("isVarArgs: %s\n", isVarArgs(mtd));
    Deque<Object> nested = null;
    if (isVarArgs(mtd)) {
      nested = new ArrayDeque<Object>(args.length + 1 - paramTypes.length);
      top:
      do {
        if (DEBUG_INVOKE) d(
            "mainArgs:  %s\n"
          + "nested:    %s\n"
          + " ---------------"
          + "  - loop check:"
          + "  (mainArgs.size() %s >= paramTypes.length %s)  -> "
          + " %s\n", 
          mainArgs, nested, 
          String.format("\u001b[36m%d\u001b[0m",mainArgs.size()),
          String.format("\u001b[36m%d\u001b[0m",nested.size()),
          Boolean.valueOf(mainArgs.size() >= paramTypes.length)
        );
        while (mainArgs.size() >= paramTypes.length) {       
          nested.offerFirst(mainArgs.pollLast());
          continue top;        
        }
      } while (false);
      
      // NOTE: mainArgs.size() == paramTypes.length - 1
      if (DEBUG_INVOKE) d(
          "*** END of loop\n\n"
        + "mainArgs:  %s\n"
        + "nested:    %s\n"
        + " ============================\n"
        + "- adding nested to end of mainArgs ...\n", 
        mainArgs, nested, 
        String.format("\u001b[36m%d\u001b[0m", mainArgs.size()),
        String.format("\u001b[36m%d\u001b[0m", nested.size())
      );
      mainArgs.offerLast(
        nested.toArray((Object[]) Array.newInstance(
          paramTypes[paramTypes.length - 1].getComponentType(), 0
        ))
      ); 
      if (DEBUG_INVOKE) d(
          "mainArgs:  %s\n"
        + "- adding nested to end of mainArgs ...\n", 
          mainArgs, nested, 
          String.format("\u001b[36m%d\u001b[0m",mainArgs.size()),
          String.format("\u001b[36m%d\u001b[0m",nested.size())
      );
    } // end of varargs-specific logic
    
    Object[] argsFinal
      = mainArgs.toArray(new Object[paramTypes.length]);
    if (DEBUG_INVOKE) d("argsFinal:\n  %s\n",
      Debug.ToString(argsFinal)
    );
    if (argsFinal.length == 1 && argsFinal[0] instanceof Object[]) {
      argsFinal = (Object[]) argsFinal[0];
    }
    
    Object result = null;
    Throwable e = null;
    try {      
      ((AccessibleObject) mtd).setAccessible(true);
      return (R) (
        result = 
          (mtd instanceof Method)
            ? (Object) ((Method) mtd).invoke(inst, argsFinal)
            : (Object) ((Constructor<?>) mtd).newInstance(argsFinal)
            
      );
    } catch (Throwable ex) {
      e = ex;
    }
    // error @ this point
    Throwable root = Reflector.getRootCause(e);
    Map<String,Object> localVaridbleState = RealArrayMap.toMap(
      Arrays.asList(
        Pair.of("argTypes", argTypes),
        Pair.of("namePred", namePred),
        Pair.of("mtd", mtd),
        Pair.of("paramTypes", paramTypes),
        Pair.of("mainArgs", mainArgs),
        Pair.of("nested", nested),
        Pair.of("argsFinal", argsFinal),
        Pair.of("result", result) ,
        Pair.of("e", e),
        Pair.of("root", root)
      )
    );
    e.addSuppressed(new RuntimeException(String.format(
        "An %s occurred during an attempt to invoke:\n\n"
      + "    %s\n\n:\n\n"
      + "    \u001b[1;41;37m%s\u001b[0m\n"
      + "Local state:\n  %s\n\n",
      root.getClass().getSimpleName(),
      dumpMembers.colorize(mtd),
      (root.getMessage() != null)
        ? root.getMessage()
        : e.toString(),
      StringUtils.join(localVaridbleState.entrySet(), "\n  ")
    )));
    e.getStackTrace();
    Reflect.setfldval(e, "stackState", localVaridbleState);
    if (doThrow || DEBUG_INVOKE || DEBUG) e.printStackTrace();
    if (doThrow) throw Reflector.Util.sneakyThrow(e);
    Class<?> retType = mtd != null
      ? (mtd instanceof Method
          ? ((Method)mtd).getReturnType()
          : ((Constructor<?>)mtd).getDeclaringClass())
      : Object.class;
    Object defaultRet = defaultValue(retType);
    return (R) defaultRet;
  }
  
  
  public static <E extends Throwable> E causeOrElse(E e,
  Throwable causeIfMissing) 
  {
    if (e.getCause() == null || e.getCause().equals(e)) {
      Reflect.setfldval(e, "cause", causeIfMissing);
    }
    return e;
  }
  
  public static Matcher NUMERIC 
   = Pattern.compile("^[0-9]+$").matcher("");
  
  public static <R> R get(Object first, String... names) {
    Object crnt = first;
    ArrayIterator<String> it = new ArrayIterator<String>(names);
    while (crnt != null && it.hasNext()) { 
      String fieldName = it.next();
      if (! NUMERIC.reset(fieldName).find()) {
        Object _cur = Reflect.getfldval(crnt, fieldName);
        if (_cur != null) { crnt = _cur; continue; }
        if (crnt instanceof Map<?, ?>) {
          crnt = ((Map<?, Object>) crnt).get(fieldName);
        }
        continue;
      }
      
      int subscript = Integer.valueOf(fieldName, 10);
      if (crnt.getClass().isArray()) {
        crnt = Array.get(crnt, subscript);
      } else if (crnt instanceof Iterable<?>) {
        crnt = CollectionUtil.toArray((Iterable<?>)crnt)[subscript];
      } else if (crnt instanceof Enumeration<?>) {
        crnt = CollectionUtil.toArray((Enumeration)crnt)[subscript];
      } else if (crnt instanceof Iterator<?>) {
        crnt = CollectionUtil.toArray((Iterator<?>)crnt)[subscript];
      } else {
        new UnsupportedOperationException(String.format(
          "  - crnt = %s\n  fieldName = %\n",
          Debug.ToString(crnt), fieldName
        )).printStackTrace();
      }
    }
    return (R) crnt;    
  }
  
  static final Type[] NO_TYPES = new Type[0];
  
  public static Type getGenericReturnType(Member m) {
    if (m instanceof Method) {
      try {
        return ((Method) m).getGenericReturnType();
      } catch (GenericSignatureFormatError e) {
        signatureError(m, e);
        return ((Method) m).getReturnType();
      }
    }
    if (m instanceof Constructor) {
      try {
        return ((Constructor) m).getDeclaringClass();
      } catch (GenericSignatureFormatError e) {
        signatureError(m, e);
        return ((Constructor) m).getDeclaringClass();
      }
    }
    if (m instanceof Field) {
      try {
        return ((Field) m).getGenericType();
      } catch (GenericSignatureFormatError e) {
        signatureError(m, e);
        return ((Field) m).getType();
      }
    }
    throw new IllegalArgumentException("Member m: unsupported type");
  }
  
  public static Type[] getGenericParameterTypes(Member m) {
    if (m instanceof Method) {
      try {
        return ((Method) m).getGenericParameterTypes();
      } catch (GenericSignatureFormatError e) {
        signatureError(m, e);
        return ((Method) m).getParameterTypes();
      }
    }
    if (m instanceof Constructor) {
      try {
        return ((Constructor) m).getGenericParameterTypes();
      } catch (GenericSignatureFormatError e) {
        signatureError(m, e);
        return ((Constructor) m).getParameterTypes();
      }
    }
    if (m instanceof Field) {
      return NO_TYPES;
    }
    return NO_TYPES;
  }  

  static Type[] getTypeParameters(Object type) {
    try {
      if (type instanceof Class<?>) {
        Class<?> cls = (Class<?>) type;
        return cls.getTypeParameters();
      } else if (type instanceof ParameterizedType) {
        return ((ParameterizedType) type).getActualTypeArguments();
      } else if (type instanceof TypeVariable) {
        return ((TypeVariable) type).getBounds();
      } else if (type instanceof GenericArrayType) {
        return getTypeParameters(
          ((GenericArrayType) type).getGenericComponentType()
        );
      }
    } catch (GenericSignatureFormatError e) {
      signatureError(type, e);
    }
    return NO_TYPES;
  }
  
   static Type[] getTypeParameters(Object type, Map<Class<?>,Object> 
   typeMap)
   {
    try {
      if (type instanceof Class<?>) {
        Class<?> cls = (Class<?>) type;
        Type generic = (Type) typeMap.get(cls);
        if (generic == null) {
          return cls.getTypeParameters();
        } else {
          if (generic instanceof ParameterizedType) {
            return ((ParameterizedType) generic).getActualTypeArguments();
          } else if (generic instanceof TypeVariable) {
            return ((TypeVariable) generic).getBounds();
          } else if (generic instanceof Class<?>) {
            return ((Class<?>) generic).getTypeParameters();
          } else if (generic instanceof GenericArrayType) {
            return getTypeParameters(
              ((GenericArrayType) generic).getGenericComponentType()
            );
          }
        }
      } else if (type instanceof ParameterizedType) {
        return ((ParameterizedType) type).getActualTypeArguments();
      } else if (type instanceof TypeVariable) {
        return ((TypeVariable) type).getBounds();
      } else if (type instanceof Method) {
        return ((Method) type).getTypeParameters();
      } else if (type instanceof Constructor) {
        return ((Constructor) type).getTypeParameters();
      } else if (type instanceof GenericArrayType) {
        return getTypeParameters(
          ((GenericArrayType) type).getGenericComponentType()
        );
      }
    } catch (GenericSignatureFormatError e) {
      signatureError(type, e);
    }
    return NO_TYPES;
  }
  
  public static void signatureError(Object m, Throwable e) {
     errors.add(e);
     System.err.printf(
       "GenericSignatureFormatError: %s signature for '%s'\n",
       ClassInfo.simplifyName(
         ClassInfo.typeToName(m.getClass().getName())
       ),
       Debug.ToString(m)
    );
  }
}