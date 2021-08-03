package bsh;

import bsh.BSHBlock;
import bsh.BSHMethodDeclaration;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.Modifiers;
import bsh.NameSpace;
import bsh.NameSpaceFactory;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.ReturnControl;
import bsh.SimpleNode;
import bsh.StringUtil;
import bsh.TargetError;
import bsh.Types;
import bsh.UtilEvalError;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.Debug;

import org.apache.commons.lang3.StringEscapeUtils;
import org.d6r.CharSequenceUtil;
import org.d6r.Dumper;

public class BshMethod implements Serializable, Cloneable {
  BshBinding declaringNameSpace;
  Modifiers modifiers;
  private String name;
  private Class<?> returnType;
  private String[] paramNames;
  private int numArgs;
  private Class<?>[] paramTypes;
  public BSHBlock methodBody;
  public BSHMethodDeclaration decl;
  private Method javaMethod;
  private Object javaObject;
  static WeakHashMap<Method, BshMethod> cache = new WeakHashMap(48);
  static HashMap<Class<?>, Pair<String, Integer>> pNameMap = new HashMap();
  static Matcher SHORT_NAME_REGEX = Pattern.compile("^(?:.*[.$])*([A-Za-z_][^.$]+)$").matcher("");

  public BshMethod(BSHMethodDeclaration method, BshBinding namespace, Modifiers modifiers) {
    this(method.name, method.returnType, method.paramsNode.getParamNames(), method.paramsNode.paramTypes, method, namespace, modifiers);
  }

  public static BshMethod valueOf(Method mthd, Object receiver) {
    BshMethod bm = (BshMethod)cache.get(mthd);
    if(bm == null) {
      bm = new BshMethod(mthd, receiver);
      cache.put(mthd, bm);
      return bm;
    } else if(receiver == null) {
      return bm;
    } else if(bm.javaObject == null) {
      bm.javaObject = receiver;
      return bm;
    } else if(receiver.equals(bm.javaObject)) {
      return bm;
    } else {
      bm = bm.clone();
      bm.javaObject = receiver;
      cache.put(mthd, bm);
      return bm;
    }
  }

  public BshMethod clone() {
    BshMethod clone = null;

    try {
      clone = (BshMethod)super.clone();
    } catch (Throwable var3) {
      var3.printStackTrace();
      return null;
    }

    clone.declaringNameSpace = this.declaringNameSpace;
    clone.name = this.name;
    clone.returnType = this.returnType;
    clone.paramNames = this.paramNames;
    clone.numArgs = this.numArgs;
    clone.paramTypes = this.paramTypes;
    clone.methodBody = this.methodBody;
    clone.decl = this.decl;
    clone.javaMethod = this.javaMethod;
    clone.javaObject = this.javaObject;
    return clone;
  }

  public BshMethod(String name, Class<?> returnType, String[] paramNames, Class<?>[] paramTypes, BSHMethodDeclaration method, BshBinding namespace, Modifiers modifiers) {
    this.name = name;
    this.returnType = returnType;
    this.paramNames = paramNames;
    if(paramNames != null) {
      this.numArgs = paramNames.length;
    }

    this.paramTypes = paramTypes;
    this.decl = method;
    this.methodBody = method.blockNode;
    this.declaringNameSpace = namespace;
    this.modifiers = modifiers;
  }

  public BshMethod(String name, Class<?> returnType, String[] paramNames, Class<?>[] paramTypes, BSHBlock methodBlockNode, BshBinding namespace, Modifiers modifiers) {
    this.name = name;
    this.returnType = returnType;
    this.paramNames = paramNames;
    if(paramNames != null) {
      this.numArgs = paramNames.length;
    }

    this.paramTypes = paramTypes;
    this.decl = methodBlockNode.parent instanceof BSHMethodDeclaration?(BSHMethodDeclaration)methodBlockNode.parent:null;
    this.methodBody = methodBlockNode;
    this.declaringNameSpace = namespace;
    this.modifiers = modifiers;
  }

  public BshMethod(Method method, Object object) {
    this.name = method.getName();
    this.returnType = method.getReturnType();
    this.paramNames = getParamNames(method);
    this.paramTypes = method.getParameterTypes();
    this.javaMethod = method;
    this.javaObject = object;
  }

  public static String[] getParamNames(Method method) {
    Class[] pTypes = method.getParameterTypes();
    String[] paramNames = new String[pTypes.length];
    int idx = -1;

    while(true) {
      ++idx;
      if(idx >= pTypes.length) {
        return paramNames;
      }

      Class flat = pTypes[idx].isArray()?pTypes[idx].getComponentType():pTypes[idx];
      Pair pair = (Pair)pNameMap.get(flat);
      String name;
      if(pair == null) {
        name = flat.getCanonicalName();
        if(name == null) {
          name = flat.getName();
        }

        name = SHORT_NAME_REGEX.reset(name).replaceAll("$1").toLowerCase();
        if(name == null || name.length() == 0) {
          name = "p";
        }

        pair = Pair.of(name, Integer.valueOf(0));
      }

      name = (String)pair.getKey();
      Integer count = Integer.valueOf(((Integer)pair.getValue()).intValue() + 1);
      pNameMap.put(flat, Pair.of(name, count));
      paramNames[idx] = count.equals(Integer.valueOf(1))?name:name.concat(count.toString());
    }
  }

  public Class<?>[] getParameterTypes() {
    return this.paramTypes;
  }

  public String[] getParameterNames() {
    return this.paramNames;
  }

  public Class<?> getReturnType() {
    return this.returnType;
  }

  public Modifiers getModifiers() {
    return this.modifiers;
  }

  public String getName() {
    return this.name;
  }

  public Object invoke(Object[] argValues, Interpreter interpreter) throws EvalError {
    return this.invoke(argValues, interpreter, (CallStack)null, (SimpleNode)null, false);
  }

  public Object invoke(Object[] argValues, Interpreter interpreter, CallStack callstack, SimpleNode callerInfo) throws EvalError {
    return this.invoke(argValues, interpreter, callstack, callerInfo, false);
  }

  public Object invoke(Object[] argValues, Interpreter interpreter, CallStack callstack, SimpleNode callerInfo, boolean overrideNameSpace) throws EvalError {
    if(this.javaMethod != null && this.javaMethod.getName().equals("run")) {
      try {
        if(!Arrays.toString(Thread.currentThread().getStackTrace()).matches(".*bsh.Reflect.invokeMethod.*bsh.BshMethod.invoke.*")) {
          return Reflect.invokeMethod(this.javaMethod, this.javaObject, argValues);
        } else {
          Iterator e = NameSpaceFactory.INSTANCE.nsMap.values().iterator();

          while(true) {
            label77:
            while(true) {
              Collection ms;
              do {
                if(!e.hasNext()) {
                  System.err.println("Aborting method invocation");
                  return Primitive.NULL;
                }

                BshBinding var16 = (BshBinding)e.next();
                ms = (Collection)var16.getMethodsByName().get(this.name);
              } while(ms == null);

              Iterator var10 = ms.iterator();

              while(var10.hasNext()) {
                BshMethod m = (BshMethod)var10.next();
                if(m.getParameterTypes().length == argValues.length) {
                  for(int p = 0; p < m.getParameterTypes().length; ++p) {
                    if(!m.getParameterTypes()[p].isAssignableFrom(Factory.typeof(argValues[p]))) {
                      continue label77;
                    }
                  }

                  System.err.println(m);
                  return m.invokeImpl(argValues, interpreter, callstack, callerInfo, overrideNameSpace);
                }
              }
            }
          }
        }
      } catch (ReflectError var14) {
        throw new EvalError("Error invoking Java method: " + var14, callerInfo, callstack);
      } catch (InvocationTargetException var15) {
        throw new TargetError("Exception invoking imported object method.", var15, callerInfo, callstack, true);
      }
    } else if(this.modifiers != null && this.modifiers.hasModifier("synchronized")) {
      Object lock;
      if(this.declaringNameSpace.isClass()) {
        try {
          lock = this.declaringNameSpace.getClassInstance();
        } catch (UtilEvalError var13) {
          throw new InterpreterError("Can\'t get class instance for synchronized method.");
        }
      } else {
        lock = this.declaringNameSpace.getThis(interpreter);
      }

      synchronized(lock) {
        return this.invokeImpl(argValues, interpreter, callstack, callerInfo, overrideNameSpace);
      }
    } else {
      if(Interpreter.DEBUG) {
        System.err.printf(">>> return invokeImpl(argValues = %s, interpreter = %s, callstack = %s, callerInfo = %s, overrideNameSpace = %s)\n", new Object[]{Arrays.asList(argValues), interpreter, callstack, Debug.ToString(callerInfo), Boolean.valueOf(overrideNameSpace)});
      }

      return this.invokeImpl(argValues, interpreter, callstack, callerInfo, overrideNameSpace);
    }
  }

  private Object invokeImpl(Object[] argValues, Interpreter interpreter, CallStack callstack, SimpleNode callerInfo, boolean overrideNameSpace) throws EvalError {
    
    
    Class returnType = this.getReturnType();
    Class[] paramTypes = this.getParameterTypes();
    if(callstack == null) {
      callstack = new CallStack(this.declaringNameSpace);
    }

    if(argValues == null) {
      argValues = new Object[0];
    }

    if(argValues.length != this.numArgs) {
      throw new EvalError("Wrong number of arguments for local method: " + this.name, callerInfo, callstack);
    } else {
      Object localNameSpace;
      if(overrideNameSpace) {
        localNameSpace = callstack.top();
      } else {
        localNameSpace = new NameSpace(this.declaringNameSpace, this.name);
        ((BshBinding)localNameSpace).setIsMethod(true);
      }

      ((BshBinding)localNameSpace).setNode(callerInfo);

      for(int ret = 0; ret < this.numArgs; ++ret) {
        if(paramTypes[ret] != null) {
          try {
            argValues[ret] = Types.castObject(argValues[ret], paramTypes[ret], 1);
          } catch (UtilEvalError var23) {
            throw new EvalError("Invalid argument: `" + this.paramNames[ret] + "\'" + " for method: " + this.name + " : " + var23.getMessage(), callerInfo, callstack);
          }

          try {
            ((BshBinding)localNameSpace).setTypedVariable(this.paramNames[ret], paramTypes[ret], argValues[ret], (Modifiers)null);
          } catch (UtilEvalError var22) {
            throw var22.toEvalError("Typed method parameter assignment", callerInfo, callstack);
          }
        } else {
          if(argValues[ret] == Primitive.VOID) {
            throw new EvalError("Undefined variable or class name, parameter: " + this.paramNames[ret] + " to method: " + this.name, callerInfo, callstack);
          }

          try {
            ((BshBinding)localNameSpace).setLocalVariable(this.paramNames[ret], argValues[ret], interpreter.getStrictJava());
          } catch (UtilEvalError var21) {
            throw var21.toEvalError(callerInfo, callstack);
          }
        }
      }

      if(!overrideNameSpace) {
        callstack.push((BshBinding)localNameSpace);
      }
        
        
      if (Interpreter.LOG_METHODS) {
        final StringBuilder argSb = new StringBuilder(80);
        for (int i=0, len=argValues.length; i<len; ++i) {
          if (i != 0) argSb.append(", ");
          final Object value = argValues[i];
          if (value instanceof Primitive) {
            final Object realValue = ((Primitive) value).getValue();
            argSb.append(realValue);
          } else if (value instanceof CharSequence) {
            argSb.append('"')
                 .append(StringEscapeUtils.escapeJava(
                    CharSequenceUtil.toString((CharSequence) value)
                  ))
                 .append("'");
          } else if (value == null) {
            argSb.append((String) null);
          } else {
            argSb.append(Dumper.tryToString(value));
          }
        }
        System.err.printf("  %s(%s)\n", name, argSb);
      }
      
      try {
        Object var26 = this.methodBody.eval(callstack, interpreter, true);
        CallStack returnStack = callstack.copy();
        ReturnControl retControl = null;
        if(var26 instanceof ReturnControl) {
          retControl = (ReturnControl)var26;
          if(retControl.kind != 46) {
            throw new EvalError("\'continue\' or \'break\' in method body", retControl.returnPoint, returnStack);
          }

          var26 = ((ReturnControl)var26).value;
          if(returnType == Void.TYPE && var26 != Primitive.VOID) {
            throw new EvalError("Cannot return value from void method", retControl.returnPoint, returnStack);
          }
        }

        if(returnType != null) {
          if(returnType == Void.TYPE) {
            Primitive var27 = Primitive.VOID;
            return var27;
          }

          try {
            var26 = Types.castObject(var26, returnType, 1);
          } catch (UtilEvalError var24) {
            SimpleNode node = callerInfo;
            if(retControl != null) {
              node = retControl.returnPoint;
            }

            throw var24.toEvalError("Incorrect type returned from method: " + this.name + var24.getMessage(), node, callstack);
          }
        }

        Object var15 = var26;
        return var15;
      } finally {
        if(!overrideNameSpace) {
          callstack.pop();
        }

      }
    }
  }

  public boolean hasModifier(String name) {
    return this.modifiers != null && this.modifiers.hasModifier(name);
  }

  public String toString() {
    return StringUtil.methodString(this.name, this.getParameterTypes(), this.getParameterNames(), this.getReturnType());
  }

  public boolean equals(Object other) {
    return !(other instanceof BshMethod)?false:this.hashCode() == ((BshMethod)other).hashCode();
  }

  private static boolean equal(Object obj1, Object obj2) {
    try {
      return obj1 == null?obj2 == null:obj1.equals(obj2);
    } catch (Throwable var3) {
      var3.printStackTrace();
      return false;
    }
  }

  public int hashCode() {
    int h = this.name.hashCode();
    int pidx = -1;
    int len = this.paramTypes != null?this.paramTypes.length:0;

    while(true) {
      ++pidx;
      if(pidx >= len) {
        h = h * 31 + (this.returnType != null?this.returnType.hashCode():999);
        h = h * 31 + (this.decl != null?this.decl.hashCode():999);
        h = h * 31 + (this.declaringNameSpace != null?this.declaringNameSpace.hashCode():999);
        return h;
      }

      h = h * 31 + (this.paramTypes[pidx] != null?this.paramTypes[pidx].hashCode():999);
    }
  }
}