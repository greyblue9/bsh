package bsh;

import bsh.BshClassManager;
import bsh.CallStack;
import bsh.Capabilities;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Primitive;
import bsh.ReflectError;
import bsh.SimpleNode;
import bsh.StringUtil;
import bsh.TargetError;
import bsh.This;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.UtilTargetError;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.d6r.ClassInfo;
import org.d6r.CollectionUtil;
import org.d6r.Debug;
import org.d6r.PosixFileInputStream;
import org.d6r.Reflector;

public class Reflect {
  private static Interpreter m_interpreter = null;
  public static final Reflect.Appender debug = new Reflect.Appender(System.err);
  public static ArrayList<Throwable> suppressed = new ArrayList<>();
  public static transient 
    WeakHashMap<MultiKey<Constructor<?>>, Constructor<?>>
      cache = new WeakHashMap<>(64);
  
  public static Interpreter interpreter() {
    if (m_interpreter == null) {
      m_interpreter = CollectionUtil.getInterpreter();
    }
    return m_interpreter;
  }

  public static Object invoke(Member member, Object target, Object... args)
    throws ReflectError
  {
    try {
      if(member instanceof Constructor) {
        return ((Constructor)member).newInstance(args);
      } else if(member instanceof Method) {
        return ((Method)member).invoke(target, args);
      } else if(member instanceof Field) {
        if(args != null && args.length != 0) {
          ((Field)member).set(target, args[0]);
          return args[0];
        } else {
          return ((Field)member).get(target);
        }
      } else {
        return Void.TYPE;
      }
    } catch (Throwable var5) {
      ReflectError re = new ReflectError(Reflector.getRootCause(var5).toString(), var5, member);
      re.put("member", member);
      re.put("target", target);
      re.put("args", args);
      re.put("exception", var5);
      throw re;
    }
  }

  public static void setAccessible(Object obj, boolean newAcc) {
    try {
      ((AccessibleObject)obj).setAccessible(newAcc);
    } catch (Throwable var3) {
      ;
    }

  }

  public static void setAccessible(Object obj) {
    setAccessible(obj, true);
  }

  public static Class<?>[] getParameterTypes(Member member) {
    return member instanceof Constructor?((Constructor)member).getParameterTypes():(member instanceof Method?((Method)member).getParameterTypes():new Class<?>[0]);
  }

  public static boolean isVarArgs(Member member) {
    return member instanceof Constructor?((Constructor)member).isVarArgs():(member instanceof Method?((Method)member).isVarArgs():false);
  }

  public static Class<?> getReturnType(Member member) {
    return member instanceof Constructor?((Constructor)member).getDeclaringClass():(member instanceof Method?((Method)member).getReturnType():(member instanceof Field?((Field)member).getType():Void.TYPE));
  }

  public static void debug(String fmt, Object... args) {
    if(Interpreter.DEBUG) {
      Object[] strArgs = new Object[args.length];
      System.arraycopy(args, 0, strArgs, 0, args.length);

      try {
        for(int e = 0; e < args.length; ++e) {
          Object arg = args[e];
          if(arg == null) {
            arg = "<null>";
          } else if(arg.getClass().isArray()) {
            arg = String.format("(length=%d) %s[]{ %s }", new Object[]{Integer.valueOf(Array.getLength(arg)), arg.getClass().getComponentType().getSimpleName(), Arrays.toString((Object[])arg)});
          }

          strArgs[e] = arg;
        }

        System.err.printf("%c[0m\n", new Object[]{Integer.valueOf(27)});
        System.err.printf(fmt, strArgs);
        System.err.printf("\n", new Object[]{Integer.valueOf(27)});
      } catch (Throwable var5) {
        System.err.printf("%s in Reflect.debug: [%s]\n  %s\n", new Object[]{var5.getClass().getSimpleName(), var5.getMessage() != null?var5.getMessage():"<no message>", StringUtils.join(ExceptionUtils.getRootCauseStackTrace(var5), "\n")});
        System.err.printf("  - fmt: \"%s\"\n", new Object[]{fmt});
        System.err.printf("  - args: %s\n", new Object[]{Debug.ToString(strArgs)});
      }

    }
  }

  public static Object invokeObjectMethod(Object object, String methodName, Object[] args, Interpreter interpreter, CallStack callstack, SimpleNode callerInfo) throws ReflectError, EvalError, InvocationTargetException {
    if(object instanceof This && !This.isExposedThisMethod(methodName)) {
      return ((This)object).invokeMethod(methodName, args, interpreter, callstack, callerInfo, false);
    } else {
      Member method = null;
      try {
        BshClassManager e = interpreter == null?null:interpreter.getClassManager();
        Class var20 = object.getClass();
        if(Interpreter.DEBUG) {
          debug("Class clas = %s", new Object[]{var20});
          debug("calling (assigning): method = resolveExpectedJavaMethod(bcm=%s, clas=%s, object=%s, methodName=%s, args=%s, false); \n", new Object[]{e, var20, object, methodName, args});
        }

        method = tryResolveJavaMethod(e, var20, object, methodName, args, false);
        if (method != null) {
          setAccessible(method, true);
          return invokeMethod(method, object, args);
        }
        
        Class<?>[] operArgTypes = null;
        Object[] operArgs = null;
        ExtendedMethod opMethod = null;
        if (object != null) {
          operArgTypes = new Class<?>[args.length + 1];
          operArgs = new Object[operArgTypes.length];
          
          operArgs[0] = Primitive.unwrap(object);
          if (operArgs[0] == null && object != null && object != Primitive.NULL) {
            operArgs[0] = object;
          }
          
          operArgTypes[0] = (operArgs[0] != null)
            ? operArgs[0].getClass()
            : null;
          
          for (int i = 1; i < operArgs.length; ++i) {
            operArgs[i] = Primitive.unwrap(args[i - 1]);
            operArgTypes[i] = operArgs[i] != null?operArgs[i].getClass():null;
          }
          opMethod = OperatorProvider.findMethod(
            interpreter.getNameSpace(),
            methodName,
            (ExtendedMethod) null,
            true,
            operArgTypes
          );
          if (opMethod != null) {
            Object operEvalResult = opMethod.eval(operArgs);
            return operEvalResult;
          }
        }
     
        
        // no found ext. method
        method = resolveExpectedJavaMethod(
          e, var20, object, methodName, args, false
        );
        
        setAccessible(method, true);
        return invokeMethod(method, object, args);
        
        
      } catch (Throwable t) { 
        throw Reflector.Util.sneakyThrow(t);
      } 
     
    }
  }

  public static Object invokeStaticMethod(BshClassManager bcm, Class clas, String methodName, Object[] args) throws ReflectError, UtilEvalError, InvocationTargetException {
    if(Interpreter.DEBUG) {
      Interpreter.debug("invoke static Member");
    }

    Member method = resolveExpectedJavaMethod(bcm, clas, (Object)null, methodName, args, true);
    return invokeMethod(method, (Object)null, args);
  }

  static Object invokeMethod(Member method, Object object, Object[] args) throws ReflectError, InvocationTargetException {
    if(args == null) {
      args = new Object[0];
    }

    logInvokeMethod("Invoking method (entry): ", method, args);
    boolean isVarArgs = isVarArgs(method);
    Class<?>[] types = getParameterTypes(method);
    Object[] tmpArgs = new Object[types.length];
    int fixedArgLen = types.length;
    if(isVarArgs) {
      if(fixedArgLen == args.length && types[fixedArgLen - 1].isAssignableFrom(args[fixedArgLen - 1].getClass())) {
        isVarArgs = false;
      } else {
        --fixedArgLen;
      }
    }

    try {
      int returnValue = 0;

      while(true) {
        if(returnValue >= fixedArgLen) {
          if(isVarArgs) {
            Class var13 = types[fixedArgLen].getComponentType();
            Object returnType = Array.newInstance(var13, args.length - fixedArgLen);
            int i = fixedArgLen;

            for(int j = 0; i < args.length; ++j) {
              Array.set(returnType, j, Primitive.unwrap(Types.castObject(args[i], var13, 1)));
              ++i;
            }

            tmpArgs[fixedArgLen] = returnType;
          }
          break;
        }

        tmpArgs[returnValue] = Types.castObject(args[returnValue], types[returnValue], 1);
        ++returnValue;
      }
    } catch (UtilEvalError var11) {
      throw new InterpreterError("illegal argument type in method invocation: " + var11);
    }

    tmpArgs = Primitive.unwrap(tmpArgs);
    logInvokeMethod("Invoking method (after massaging values): ", method, tmpArgs);
    setAccessible(method, true);
    Object var12 = invoke(method, object, tmpArgs);
    if(var12 == null) {
      var12 = Primitive.NULL;
    }

    Class var14 = getReturnType(method);
    return Primitive.wrap(var12, var14);
  }

  public static Object getIndex(Object indexable, int index) throws ReflectError, UtilTargetError {
    if(Interpreter.DEBUG) {
      Interpreter.debug("getIndex: " + indexable + ", index=" + index);
    }

    try {
      if(indexable.getClass().isArray()) {
        Object e = Array.get(indexable, index);
        return Primitive.wrap(e, indexable.getClass().getComponentType());
      } else if(indexable instanceof List) {
        return ((List)indexable).get(index);
      } else if(indexable instanceof Collection) {
        return CollectionUtil.toArray((Collection)indexable)[index];
      } else if(indexable instanceof Iterable) {
        return CollectionUtil.toArray((Iterable)indexable)[index];
      } else if(indexable instanceof Iterator) {
        return CollectionUtil.toArray((Iterator)indexable)[index];
      } else if(indexable instanceof Enumeration) {
        return CollectionUtil.toArray((Enumeration)indexable)[index];
      } else if(indexable instanceof Map) {
        return ((Map)indexable).get(Integer.valueOf(index));
      } else if(indexable instanceof Hashtable) {
        return ((Hashtable)indexable).get(Integer.valueOf(index));
      } else if(indexable instanceof Properties) {
        return ((Properties)indexable).get(Integer.valueOf(index));
      } else if(indexable instanceof CharSequence) {
        return new Primitive(((CharSequence)indexable).charAt(index));
      } else {
        throw new ReflectError(String.format("%s is not indexable", new Object[]{indexable.getClass().getName()}));
      }
    } catch (ArrayIndexOutOfBoundsException var3) {
      throw new UtilTargetError(var3);
    } catch (Exception var4) {
      throw new ReflectError("Indexed access:" + var4);
    }
  }

  public static void setIndex(Object array, int index, Object val) throws ReflectError, UtilTargetError {
    try {
      val = Primitive.unwrap(val);
      Array.set(array, index, val);
    } catch (ArrayStoreException var4) {
      throw new UtilTargetError(var4);
    } catch (IllegalArgumentException var5) {
      throw new UtilTargetError(new ArrayStoreException(var5.toString()));
    } catch (Exception var6) {
      throw new ReflectError("Array access:" + var6);
    }
  }

  public static Object getStaticFieldValue(Class clas, String fieldName) throws UtilEvalError, ReflectError {
    return getFieldValue(clas, (Object)null, fieldName, true);
  }

  public static <T> T getObjectFieldValue(Object object, String fieldName) throws ReflectError {
    try {
      if(object instanceof This) {
  return (T) (Object) ((This)object).namespace.getVariable(fieldName);
      } else if(object == Primitive.NULL) {
        throw new ReflectError(String.format("Attempt to access field \'%s\' on null value", new Object[]{fieldName}));
      } else {
        try {
  return (T) (Object) getFieldValue(object.getClass(), object, fieldName, false);
        } catch (ReflectError var3) {
          if(hasObjectPropertyGetter(object.getClass(), fieldName)) {
  return (T) (Object) getObjectProperty(object, fieldName);
          } else {
            throw var3;
          }
        }
      }
    } catch (UtilEvalError var4) {
      var4.printStackTrace();
  return (T) (Object) Primitive.NULL;
    }
  }

  static LHS getLHSStaticField(Class clas, String fieldName) throws UtilEvalError, ReflectError {
    Field f = resolveExpectedJavaField(clas, fieldName, true);
    setAccessible(f, true);
    return new LHS(f);
  }

  static LHS getLHSObjectField(Object object, String fieldName) throws UtilEvalError, ReflectError {
    if(object instanceof This) {
      boolean e1 = false;
      return new LHS(((This)object).namespace, fieldName, e1);
    } else {
      try {
        Field e = resolveExpectedJavaField(object.getClass(), fieldName, false);
        setAccessible(e, true);
        return new LHS(object, e);
      } catch (ReflectError var3) {
        if(hasObjectPropertySetter(object.getClass(), fieldName)) {
          return new LHS(object, fieldName);
        } else {
          throw var3;
        }
      }
    }
  }

  public static Object getFieldValue(Class clas, Object object, String fieldName, boolean staticOnly) throws UtilEvalError, ReflectError {
    try {
      Field e = resolveExpectedJavaField(clas, fieldName, staticOnly);
      setAccessible(e, true);
      Object value = e.get(object);
      Class returnType = e.getType();
      return Primitive.wrap(value, returnType);
    } catch (NullPointerException var7) {
      throw new ReflectError("???" + fieldName + " is not a static field.");
    } catch (IllegalAccessException var8) {
      throw new ReflectError("Can\'t access field: " + fieldName);
    }
  }

  public static Field resolveJavaField(Class clas, String fieldName, boolean staticOnly) throws UtilEvalError {
    try {
      return resolveExpectedJavaField(clas, fieldName, staticOnly);
    } catch (ReflectError var4) {
      return null;
    }
  }

  public static Field resolveExpectedJavaField(Class clas, String fieldName, boolean staticOnly) throws UtilEvalError, ReflectError {
    Field field;
    try {
      if(Capabilities.haveAccessibility()) {
        field = findAccessibleField(clas, fieldName);
      } else {
        field = clas.getField(fieldName);
      }
    } catch (NoSuchFieldException var5) {
      throw new ReflectError("No such field: " + fieldName, var5);
    } catch (SecurityException var6) {
      throw new UtilTargetError("Security Exception while searching fields of: " + clas, var6);
    }

    if(staticOnly && !Modifier.isStatic(field.getModifiers())) {
      throw new UtilEvalError("Can\'t reach instance field: " + fieldName + " from static context: " + clas.getName());
    } else {
      return field;
    }
  }

  public static Field findAccessibleField(Class<?> clas, String fieldName) throws UtilEvalError, NoSuchFieldException {
    Field field;
    try {
      field = clas.getField(fieldName);
      setAccessible(field, true);
      return field;
    } catch (NoSuchFieldException var7) {
      NoSuchFieldException origNsfe = null;

      while(true) {
        if(clas != null) {
          try {
            field = clas.getDeclaredField(fieldName);
            setAccessible(field, true);
            return field;
          } catch (SecurityException var5) {
            ;
          } catch (NoSuchFieldException var6) {
            if(origNsfe == null) {
              origNsfe = var6;
            }

            clas = clas.getSuperclass();
            continue;
          }
        }

        throw new NoSuchFieldException(fieldName);
      }
    }
  }
  
  
  public static Member resolveExpectedJavaMethod(final BshClassManager bcm,
    final Class<?> clas, final Object object, final String name,
    final Object[] args, final boolean staticOnly)
    throws ReflectError, UtilEvalError
  {
    if (object == Primitive.NULL) {
      throw new UtilTargetError(new NullPointerException(String.format(
        "Attempt to invoke method %s.%s on null value",
        clas != null? ClassInfo.typeToName(clas.getName()): "null",
        name
      )));
    }
    
    Class<?>[] types = Types.getTypes(args);
    Member method = resolveJavaMethod(bcm, clas, name, types, staticOnly);
    if (method == null) {
      Object[] args2 = null;
      for (int i=0, len=args.length; i<len; ++i) {
        Object arg = args[i];
        if (! (arg instanceof Primitive)) continue;
        Object valueUnwrapped = ((Primitive) arg).getValue();
        if (args2 == null) args2 = Arrays.copyOf(args, len);
        args2[i] = valueUnwrapped;
      }
      if (args2 !=  null) {
        types = Types.getTypes(args2);
        method = resolveJavaMethod(bcm, clas, name, types, staticOnly);
      }
    }
     if (method == null) {
      throw new ReflectError(
        (staticOnly ? "Static method " : "Method ")
          + StringUtil.methodString(name, types)
          + " not found in class \'" + 
            ((clas != null) ? clas.getName() : "<clas == NULL>")
          + "\'"
      );
    }
    return method;
  }
  
  
  /*
  public static Member resolveExpectedJavaMethod(BshClassManager bcm,
  Class<?> clas, Object object, String name, Object[] args, boolean staticOnly) 
    throws ReflectError, UtilEvalError
  {
    if (object == Primitive.NULL) throw new UtilTargetError(
      new NullPointerException(String.format(
        "Attempt to invoke method %s.%s on null value",
        clas != null? ClassInfo.typeToName(clas.getName()): "null", name
      ))
    );
    
    
    final Member result;
    try {
      result = tryResolveJavaMethod(bcm, clas, object, name, args, staticOnly);
      if (result != null) return result;
    } catch (Throwable t) {
      throw Reflector.Util.sneakyThrow(t);
    }
    final StringBuilder paramsSb = new StringBuilder(64);
    for (final Object arg: args) {
      if (paramsSb.length() != 0) paramsSb.append(", ");
      paramsSb.append(
        (arg != null) 
          ? ClassInfo.getSimpleName(ClassInfo.typeToName(arg))
          : "null"
      );
    }
    throw new ReflectError(String.format(
      "The %smethod %s(%s) is undefined for the type %s",
      (staticOnly ? "static ": ""), name, paramsSb, ClassInfo.typeToName(clas)
    ));
  }*/
  
  
  
  
  public static Member tryResolveJavaMethod(BshClassManager bcm,
    Class<?> clas, Object object, String name, Object[] args, boolean staticOnly) 
  {
    if (object == Primitive.NULL) return null;
    
    // try with the arguments passed 'as-is'
    Class<?>[] types = Types.getTypes(args);
    Member method = resolveJavaMethod(bcm, clas, name, types, staticOnly);
    if (method != null) return method;
    
    // try with unwrapped primitive forms of (wrapper) (?) arguments
    Object[] args2 = null;
    List<String> notes = null;
    for (int i=0, len=args.length; i<len; ++i) {
      final Object arg = args[i];
      if (! (arg instanceof Primitive)) continue;
      Object valueUnwrapped = ((Primitive) arg).getValue();
      if (valueUnwrapped == null) continue;
      if (args2 == null) {
        args2 = Arrays.copyOf(args, len);
      }
      if (notes == null && Interpreter.DEBUG) notes = new ArrayList<>();
      args2[i] = valueUnwrapped;
      if (Interpreter.DEBUG) notes.add(String.format(
        "Note: wrote unwrapped value from bsh.Primitive.getValue() (type: %s)" +
        " to args Object[] array at index %d: `%s`",
        valueUnwrapped.getClass().getName(), i, valueUnwrapped
      ));
    }
    if (args2 != null) {
      Class<?>[] primitiveTypes = Types.getTypes(args2);
      method = resolveJavaMethod(bcm, clas, name, types, staticOnly);
      if (method != null) {
        if (Interpreter.DEBUG) for (String note: notes) Interpreter.debug(note);
        System.arraycopy(args2, 0, args, 0, args.length);
        return method;
      }
    }
    if (method != null) {
      throw new AssertionError("logic error in tryResolveJavaMethod");
    }
    return null;
  }

  public static Member resolveJavaMethod(BshClassManager bcm, Class<?> clas, String name, Class<?>[] types, boolean staticOnly) throws UtilEvalError {
    if(clas == null) {
      throw new InterpreterError("null class");
    } else {
      Object method = null;
      if(bcm == null) {
        Interpreter.debug("resolveJavaMethod UNOPTIMIZED lookup (bcm == null)");
      } else {
        method = bcm.getResolvedMethod(clas, name, types, staticOnly);
      }

      if(method == null) {
        boolean publicOnly = !Capabilities.haveAccessibility();

        try {
          method = findOverloadedMethod(clas, name, types, publicOnly);
        } catch (SecurityException var9) {
          throw new UtilTargetError("Security Exception while searching methods of: " + clas, var9);
        }

        checkFoundStaticMethod((Member)method, staticOnly, clas);

        try {
          setAccessible(method, true);
        } catch (Exception var8) {
          ;
        }

        if(method != null && bcm != null && method instanceof Method) {
          bcm.cacheResolvedMethod(clas, types, (Method)method);
        }
      }

      return (Member)method;
    }
  }

  public static Member getDeclaredConstructorOrMethod(Class<?> declaringClass, 
    String vmName, Class... paramTypes)
  {
    return org.d6r.Reflect.getDeclaredConstructorOrMethod(
      declaringClass, vmName, paramTypes
    );
  }

  public static Member findOverloadedMethod(Class baseClass, String methodName,
    Class<?>[] types, boolean publicOnly)
  {
    publicOnly = false;
    ArrayList publicMethods = new ArrayList();
    ArrayList nonPublicMethods = null;
    if(!publicOnly) {
      nonPublicMethods = new ArrayList();
    }

    if(Interpreter.DEBUG) {
      debug("Searching for method: %s.%s\n", new Object[]{baseClass, StringUtil.methodString(methodName, types)});
      debug("publicMethods: %s\n", new Object[]{publicMethods});
      debug("nonPublicMethods: %s\n", new Object[]{nonPublicMethods});
      debug("baseClass = %s\n", new Object[]{baseClass});
      debug.append("types = ").append(Arrays.asList(types).toString()).append('\n');
      debug("baseClass = %s\n", new Object[]{baseClass});
      debug("types.length = %s\n", new Object[]{Integer.valueOf(types.length)});
      debug("publicOnly = %s\n", new Object[]{Boolean.valueOf(publicOnly)});
    }

    gatherMethodsRecursive(baseClass, methodName, types.length, publicMethods, nonPublicMethods);
    if(Interpreter.DEBUG) {
      Interpreter.debug("Looking for most specific method: " + methodName);
      debug("Find most specific method: methodName = %s\n", new Object[]{methodName});
      debug("publicMethods: %s\n", new Object[]{publicMethods});
      debug("nonPublicMethods: %s\n", new Object[]{nonPublicMethods});
      debug("baseClass = %s\n", new Object[]{baseClass});
      debug.append("types = ".concat(Arrays.asList(types).toString()).concat("\n"));
      debug("baseClass = %s\n", new Object[]{baseClass});
      debug("publicOnly = %s\n", new Object[]{Boolean.valueOf(publicOnly)});
    }

    Member method = findMostSpecificMethod(types, publicMethods);
    if(method == null && nonPublicMethods != null) {
      method = findMostSpecificMethod(types, nonPublicMethods);
    }

    return method;
  }

  public static void gatherMethodsRecursive(Class baseClass, String methodName, int numArgs, List<Member> publicMethods, List<Member> nonPublicMethods) {
    Class superclass = baseClass.getSuperclass();
    if(superclass != null) {
      gatherMethodsRecursive(superclass, methodName, numArgs, publicMethods, nonPublicMethods);
    }

    boolean isPublicClass = isPublic(baseClass);
    Method[] methods = baseClass.getDeclaredMethods();
    Method[] var11 = methods;
    int var10 = methods.length;

    int var9;
    for(var9 = 0; var9 < var10; ++var9) {
      Method intf = var11[var9];
      if(intf.getName().equals(methodName)) {
        if(isVarArgs(intf)) {
          if(getParameterTypes(intf).length - 1 > numArgs) {
            continue;
          }
        } else if(getParameterTypes(intf).length != numArgs) {
          continue;
        }

        try {
          setAccessible(intf, true);
        } catch (Throwable var13) {
          ;
        }

        if(publicMethods == null) {
          nonPublicMethods.add(intf);
        } else {
          publicMethods.add(intf);
        }
      }
    }

    Class<?>[] var15;
    var10 = (var15 = baseClass.getInterfaces()).length;

    for(var9 = 0; var9 < var10; ++var9) {
      Class var14 = var15[var9];
      gatherMethodsRecursive(var14, methodName, numArgs, publicMethods, nonPublicMethods);
    }

  }

  public static <T> T constructObject(Class<T> cls, Object[] args) throws ReflectError, InvocationTargetException {
    if(cls.isInterface()) {
      throw new ReflectError("Can\'t create instance of an interface: " + cls);
    } else {
      Class<?>[] types = Types.getTypes(args);

      for(int keyTypes = 0; keyTypes < args.length; ++keyTypes) {
        if(args[keyTypes] instanceof Primitive) {
          args[keyTypes] = Primitive.unwrap(args[keyTypes]);
        }
      }

      Class<?>[] var15 = new Class<?>[types.length + 1];
      System.arraycopy(types, 0, var15, 1, types.length);
      var15[0] = cls;
      MultiKey key = new MultiKey(var15, false);
      Constructor con = (Constructor)cache.get(key);
      Constructor[] constructors = null;
      if(con == null) {
        constructors = cls.getDeclaredConstructors();
        if(Interpreter.DEBUG) {
          Interpreter.debug.printf("Looking for most specific constructor: %s\n  - types = %s\n  - declared ctors: %s\n\n", new Object[]{cls.getName(), Arrays.asList(types), Arrays.asList(constructors)});
        }

        con = findBestMatch(constructors, args);
        if(con == null) {
          throw cantFindConstructor(cls, types);
        }

        if(Interpreter.DEBUG) {
          Interpreter.debug.printf("  - con = %s\n", new Object[]{con.toGenericString()});
        }

        setAccessible(con, true);
        cache.put(key, con);
      }

      args = Primitive.unwrap(args);

      try {
  return (T) (Object) con.newInstance(args);
      } catch (InstantiationException var12) {
        throw new ReflectError("The class " + cls + " is abstract ", var12);
      } catch (IllegalAccessException var13) {
        throw new ReflectError("We don\'t have permission to create an instance.Use setAccessibility(true) to enable access.", var13);
      } catch (Exception var14) {
        String diag = String.format("  - cls = %s\n  - Selected constructor (con) = %s\n  - args = %s\n  - types = %s\n  - constructors = %s\n\n", new Object[]{cls.getName(), con.toGenericString(), Debug.ToString(Arrays.asList(args)), Arrays.asList(types), constructors != null?Arrays.asList(constructors):"null"});
        String contextInfo = String.format("%s: %s\nDiagnostic info:\n%s\n", new Object[]{var14.getClass().getSimpleName(), var14.getMessage() != null?var14.getMessage():"[no msg]", diag});
        if(var14 instanceof InvocationTargetException) {
          InvocationTargetException var17 = (InvocationTargetException)var14;
          Throwable var16 = var17.getCause();
          var17 = new InvocationTargetException(var17.getTargetException(), contextInfo);
          if(var16 != null) {
            org.d6r.Reflect.setfldval(var17, "cause", var16);
          }

          if(Interpreter.DEBUG) {
            var17.printStackTrace();
          }

          throw var17;
        } else {
          Throwable cause = var14.getCause();
          ReflectError rex = new ReflectError(String.format("Exception constructing %s:\n%s", new Object[]{cls.getName(), contextInfo}), (Throwable)(cause != null?cause:var14));
          if(Interpreter.DEBUG) {
            rex.printStackTrace();
          }

          throw rex;
        }
      }
    }
  }

  public static <T> Constructor<T> findBestMatch(Constructor<T>[] ctors, Object[] args) {
    return Factory.findBestMatch(ctors, args);
  }

  static Constructor findMostSpecificConstructor(Class<?>[] idealMatch, Constructor[] constructors) {
    int match = findMostSpecificConstructorIndex(idealMatch, constructors);
    return match == -1?null:constructors[match];
  }

  static int findMostSpecificConstructorIndex(Class<?>[] idealMatch, Constructor[] constructors) {
    Class<?>[][] candidates = new Class<?>[constructors.length][];

    for(int i = 0; i < candidates.length; ++i) {
      candidates[i] = getParameterTypes(constructors[i]);
    }

    return findMostSpecificSignature(idealMatch, candidates);
  }

  private static Member findMostSpecificMethod(Class<?>[] idealMatch, List<Member> methods) {
    ArrayList candidateSigs = new ArrayList();
    ArrayList methodList = new ArrayList();
    Iterator var5 = methods.iterator();

    while(true) {
      Member match;
      Class<?>[] parameterTypes;
      do {
        if(!var5.hasNext()) {
          int var10 = findMostSpecificSignature(idealMatch, (Class<?>[][])candidateSigs.toArray(new Class<?>[candidateSigs.size()][]));
          return var10 == -1?null:(Member)methodList.get(var10);
        }

        match = (Member)var5.next();
        parameterTypes = getParameterTypes(match);
        methodList.add(match);
        candidateSigs.add(parameterTypes);
      } while(!isVarArgs(match));

      Class<?>[] candidateSig = new Class<?>[idealMatch.length];

      int j;
      for(j = 0; j < parameterTypes.length - 1; ++j) {
        candidateSig[j] = parameterTypes[j];
      }

      for(Class varType = parameterTypes[j].getComponentType(); j < idealMatch.length; ++j) {
        candidateSig[j] = varType;
      }

      methodList.add(match);
      candidateSigs.add(candidateSig);
    }
  }

  static int findMostSpecificSignature(Class<?>[] idealMatch, Class<?>[][] candidates) {
    for(int round = 1; round <= 4; ++round) {
      Class<?>[] bestMatch = null;
      int bestMatchIndex = -1;

      for(int i = 0; i < candidates.length; ++i) {
        Class<?>[] targetMatch = candidates[i];
        if(Types.isSignatureAssignable(idealMatch, targetMatch, round) && (bestMatch == null || Types.isSignatureAssignable(targetMatch, bestMatch, 1))) {
          bestMatch = targetMatch;
          bestMatchIndex = i;
        }
      }

      if(bestMatch != null) {
        return bestMatchIndex;
      }
    }

    return -1;
  }

  public static String accessorName(String getorset, String propName) {
    return getorset + String.valueOf(Character.toUpperCase(propName.charAt(0))) + propName.substring(1);
  }

  public static boolean hasObjectPropertyGetter(Class clas, String propName) {
    if(clas == Primitive.class) {
      return false;
    } else {
      String getterName = accessorName("get", propName);

      try {
        clas.getMethod(getterName, new Class<?>[0]);
        return true;
      } catch (NoSuchMethodException var5) {
        getterName = accessorName("is", propName);

        try {
          Method e = clas.getMethod(getterName, new Class<?>[0]);
          return getReturnType(e) == Boolean.TYPE;
        } catch (NoSuchMethodException var4) {
          return false;
        }
      }
    }
  }

  public static boolean hasObjectPropertySetter(Class clas, String propName) {
    String setterName = accessorName("set", propName);
    Method[] methods = clas.getMethods();
    Method[] var7 = methods;
    int var6 = methods.length;

    for(int var5 = 0; var5 < var6; ++var5) {
      Method method = var7[var5];
      if(method.getName().equals(setterName)) {
        return true;
      }
    }

    return false;
  }

  public static Object getObjectProperty(Object obj, String propName) throws UtilEvalError, ReflectError {
    Object[] args = new Object[0];
    Interpreter.debug("property access: ");
    Member method = null;
    Exception e1 = null;
    Exception e2 = null;

    String e;
    try {
      e = accessorName("get", propName);
      method = resolveExpectedJavaMethod((BshClassManager)null, obj.getClass(), obj, e, args, false);
    } catch (Exception var9) {
      e1 = var9;
    }

    if(method == null) {
      try {
        e = accessorName("is", propName);
        method = resolveExpectedJavaMethod((BshClassManager)null, obj.getClass(), obj, e, args, false);
        if(getReturnType(method) != Boolean.TYPE) {
          method = null;
        }
      } catch (Exception var8) {
        e2 = var8;
      }
    }

    if(method == null) {
      throw new ReflectError("Error in property getter: " + e1 + (e2 != null?" : " + e2:""));
    } else {
      try {
        return invokeMethod(method, obj, args);
      } catch (InvocationTargetException var7) {
        throw new UtilEvalError("Property accessor threw exception: " + var7.getTargetException());
      }
    }
  }

  public static void setObjectProperty(Object obj, String propName, Object value) throws ReflectError, UtilEvalError {
    String accessorName = accessorName("set", propName);
    Object[] args = new Object[]{value};
    Interpreter.debug("property access: ");

    try {
      Member e = resolveExpectedJavaMethod((BshClassManager)null, obj.getClass(), obj, accessorName, args, false);
      invokeMethod(e, obj, args);
    } catch (InvocationTargetException var6) {
      throw new UtilEvalError("Property accessor threw exception: " + var6.getTargetException());
    }
  }

  public static String normalizeClassName(Class type) {
    if(!type.isArray()) {
      return type.getName();
    } else {
      StringBuilder className = new StringBuilder();

      try {
        className.append(getArrayBaseType(type).getName()).append(' ');

        for(int i = 0; i < getArrayDimensions(type); ++i) {
          className.append("[]");
        }
      } catch (ReflectError var3) {
        ;
      }

      return className.toString();
    }
  }

  public static int getArrayDimensions(Class arrayClass) {
    return !arrayClass.isArray()?0:arrayClass.getName().lastIndexOf(91) + 1;
  }

  public static Class getArrayBaseType(Class arrayClass) throws ReflectError {
    if(!arrayClass.isArray()) {
      throw new ReflectError("The class is not an array.");
    } else {
      return arrayClass.getComponentType();
    }
  }

  public static Object invokeCompiledCommand(Class commandClass, Object[] args, Interpreter interpreter, CallStack callstack) throws UtilEvalError {
    Object[] invokeArgs = new Object[args.length + 2];
    invokeArgs[0] = interpreter;
    invokeArgs[1] = callstack;
    System.arraycopy(args, 0, invokeArgs, 2, args.length);
    BshClassManager bcm = interpreter.getClassManager();

    try {
      return invokeStaticMethod(bcm, commandClass, "invoke", invokeArgs);
    } catch (InvocationTargetException var7) {
      throw new UtilEvalError("Error in compiled command: " + var7.getTargetException(), var7);
    } catch (ReflectError var8) {
      throw new UtilEvalError("Error invoking compiled command: " + var8, var8);
    }
  }

  public static void logInvokeMethod(String msg, Member method, Object[] args) {
    if(Interpreter.DEBUG) {
      Interpreter.debug(msg + method + " with args:");

      for(int i = 0; i < args.length; ++i) {
        Object arg = args[i];
        Interpreter.debug("args[" + i + "] = " + arg + " type = " + (arg == null?"<unkown>":arg.getClass()));
      }
    }

  }

  public static void checkFoundStaticMethod(Member method, boolean staticOnly, Class clas) throws UtilEvalError {
    if(method != null && staticOnly && !isStatic(method)) {
      throw new UtilEvalError("Cannot reach instance method: " + StringUtil.methodString(method.getName(), getParameterTypes(method)) + " from static context: " + clas.getName());
    }
  }

  public static ReflectError cantFindConstructor(Class<?> clas, Class<?>[] types) {
    return types.length == 0?new ReflectError("Can\'t find default constructor for: " + clas):new ReflectError("Can\'t find constructor: " + StringUtil.methodString(clas.getName(), types) + " in class: " + clas.getName());
  }

  public static boolean isPublic(Member member) {
    return Modifier.isPublic(member.getModifiers());
  }

  public static boolean isPublic(Class clazz) {
    return Modifier.isPublic(clazz.getModifiers());
  }

  public static boolean isStatic(Member m) {
    return Modifier.isStatic(m.getModifiers());
  }

  public static class Appender implements Appendable {
    Appendable stream;

    public Appender(Appendable stream) {
      this.stream = stream;
    }

    public Reflect.Appender append(char c) {
      if(this.stream != null) {
        try {
          this.stream.append(c);
        } catch (IOException var3) {
          ;
        }
      }

      return this;
    }

    public Reflect.Appender append(CharSequence charSequence) {
      if(this.stream != null) {
        try {
          if(charSequence == null) {
            this.stream.append("null");
          } else {
            this.stream.append(charSequence.toString());
          }
        } catch (IOException var3) {
          ;
        }
      }

      return this;
    }

    public Reflect.Appender append(CharSequence charSequence, int start, int end) {
      if(this.stream != null) {
        try {
          if(charSequence == null) {
            charSequence = "null";
          }

          this.stream.append(((CharSequence)charSequence).subSequence(start, end).toString());
        } catch (IOException var5) {
          ;
        }
      }

      return this;
    }

    public Reflect.Appender append(Throwable t) {
      try {
        String[] var5;
        int var4 = (var5 = ExceptionUtils.getRootCauseStackTrace(t)).length;

        for(int var3 = 0; var3 < var4; ++var3) {
          String line = var5[var3];
          this.stream.append(line);
          this.stream.append('\n');
        }
      } catch (IOException var6) {
        ;
      }

      return this;
    }
  }
}