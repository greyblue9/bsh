package bsh;

import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.StringUtil;
import bsh.TargetError;
import bsh.Types;
import bsh.UtilEvalError;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.d6r.CollectionUtil;
import org.d6r.Debug;
import org.d6r.Dumper;
import java.util.*;
import com.google.common.base.Function;
import org.d6r.*;

public class This implements Serializable, Runnable {
  final BshBinding namespace;
  transient Interpreter declaringInterpreter;
  private Map<Integer, Object> interfaces;
  private final InvocationHandler invocationHandler = new This.Handler();

  public static int hashCode(This th) {
    return ((31 + th.namespace.hashCode()) * 31 + th.declaringInterpreter.hashCode()) * 31 + th.interfaces.hashCode() + 31 + System.identityHashCode(th.invocationHandler);
  }

  public static boolean equals(This th, Object other) {
    if(th == null) {
      return false;
    } else if(other == null) {
      return false;
    } else {
      if(other instanceof This.Handler) {
        other = org.d6r.Reflect.getfldval(other, "this$0");
      }

      if(th.getClass() != other.getClass()) {
        return false;
      } else {
        This that = (This)other;
        return th.namespace != that.namespace?false:(th.declaringInterpreter != that.declaringInterpreter?false:(th.namespace != that.namespace?false:(!th.interfaces.equals(that.interfaces)?false:th.invocationHandler == that.invocationHandler)));
      }
    }
  }

  static This getThis(NameSpace namespace, Interpreter declaringInterpreter) {
    return Factory.getThis(namespace, declaringInterpreter);
  }

  public <T> T getInterface(Class cls) {
    return this.getInterface(new Class[]{cls});
  }

  public <T> T getInterface(Class<?>[] classes) {
    if(this.interfaces == null) {
      this.interfaces = Collections.emptyMap();
    }

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Object interf = Proxy.newProxyInstance(classLoader, classes, this.invocationHandler);
  return (T) (Object) interf;
  }

  This(NameSpace namespace, Interpreter declaringInterpreter) {
    this.namespace = namespace;
    this.declaringInterpreter = declaringInterpreter;
    if(Interpreter.DEBUG) {
      (new Error("bsh.This allocation")).printStackTrace();
    }

  }

  public NameSpace getNameSpace() {
    return (NameSpace)this.namespace;
  }

  public String toString() {
    return "\'this\' reference to Bsh object: " + this.namespace;
  }

  public void run() {
    try {
      this.invokeMethod("run", new Object[0]);
    } catch (EvalError var2) {
      this.declaringInterpreter.error("Exception in runnable:" + var2);
    }

  }

  public Object invokeMethod(String name, Object[] args) throws EvalError {
    return this.invokeMethod(name, args, (Interpreter)null, (CallStack)null, (SimpleNode)null, false);
  }
  
  
  WeakHashMap<Integer, Object> resolveCache = new WeakHashMap();
  
  
  public Object invokeMethod(String methodName, Object[] args,
  Interpreter interpreter, CallStack callstack, SimpleNode callerInfo, 
  boolean declaredOnly) 
    throws EvalError 
  {   
    Class<?>[] argTypes = Types.getTypes(args);
    int resolveKey = ObjectUtil.coalesceHashCode(
      System.identityHashCode(interpreter), methodName, argTypes
    );
    Object resolvedMethod 
      = resolveCache.get(Integer.valueOf(resolveKey));
      
      if (callstack == null) callstack = new CallStack(this.namespace);
      NameSpace namespace = (NameSpace) callstack.top();
      if (interpreter == null) interpreter = this.declaringInterpreter;

      if (callerInfo == null) {
        try {
          callerInfo = ((NameSpace) CallStack.getActiveCallStack().top())
            .callerInfoNode;
        } catch (Throwable ignore) {}
        if (callerInfo == null) callerInfo = SimpleNode.JAVACODE;
      }
      
    try {

      if (resolvedMethod == null) {
        
        if (methodName.equals("hashCode") && args.length == 0) {
          resolvedMethod = new Function<Object[], Integer>() {
            @Override public Integer apply(Object[] _args) {
              return ObjectUtil.coalesceHashCode(This.this);
            }
          };
          return ((Function<Object[],Object>) resolvedMethod).apply(args);
        } else if (methodName.equals("equals") && args.length == 1) {
          resolvedMethod = new Function<Object[], Boolean>() {
            @Override public Boolean apply(Object[] _args) {
              return _args[0] != null 
                  && _args[0].getClass() == This.this.getClass()
                  && This.this.namespace.equals(((This)_args[0]).namespace)
                  && This.this.invocationHandler
                      .equals(((This)_args[0]).invocationHandler);
            }
          };
          return ((Function<Object[],Object>) resolvedMethod).apply(args);
        } else {
          Throwable a = null, b = null;
          
          try {
            resolvedMethod = namespace.getMethod(
              methodName, argTypes, false
            );
          } catch (UtilEvalError var14) { a = var14; }
          
          if (resolvedMethod == null) {            
            for (bsh.BshMethod md: ((NameSpace) this.namespace)
                      .methods.get(methodName)) 
            {
                if (md != null) {
                  resolvedMethod = md;
                  break;
                }
            }
          }
        }
      }
      
      
      
    
      Object[] args2 = (args != null)
        ? new Object[args.length] : new Object[0];
      for (int i=0,len=args2.length; i<len; ++i) {
        args2[i] = args[i] != null ? args[i] : Primitive.NULL;
      }
      
      
      if (resolvedMethod instanceof BshMethod) {
        return ((BshMethod) resolvedMethod).invoke(
          args2, interpreter, callstack, callerInfo
        );
      } else if (resolvedMethod instanceof Function) {
        return ((Function)resolvedMethod).apply(args);
      } else if(methodName.equals("toString") && args.length == 0) {
        return this.toString();
      } else if(methodName.equals("clone") && args.length == 0) {
        throw Reflector.Util.sneakyThrow(new CloneNotSupportedException());
      } else {
        try {
          resolvedMethod = namespace.getMethod("invoke", new Class[2]);
        } catch (UtilEvalError var13) {
          var13.printStackTrace();
        }
        if (resolvedMethod != null) {
          return ((BshMethod) resolvedMethod).invoke(
            new Object[] { methodName, args2 },
            interpreter, callstack, callerInfo
          );
        } else {
          throw new EvalError(
            String.format(
              "The method %s is undefined for the namespace '%s' (%s)",
              StringUtil.methodString(methodName, argTypes),
              this.namespace.getName(), Debug.ToString(this.namespace)
            ), 
            callerInfo, callstack
          );
        }
      }
      
      
      /*if (resolvedMethod instanceof BshMethod) {
        return ((BshMethod)resolvedMethod)
          .invoke(args2, interpreter, callstack, callerInfo);
      } else if (resolvedMethod instanceof Function) {
        return ((Function)resolvedMethod).apply(args);
      }
      throw new AssertionError("NULL!");
      */
    } finally {
      if (resolvedMethod != null) resolveCache.put(
        Integer.valueOf(resolveKey), resolvedMethod
      );
    }
  }

  public static void bind(This ths, NameSpace namespace, Interpreter declaringInterpreter) {
    System.err.printf("bind(%s, %s, %s)\n", new Object[]{ths, namespace, declaringInterpreter});
    ths.namespace.setParent(namespace);
    ths.declaringInterpreter = declaringInterpreter;
  }

  static boolean isExposedThisMethod(String name) {
    return name.equals("getClass") || name.equals("invokeMethod") || name.equals("getInterface") || name.equals("wait") || name.equals("notify") || name.equals("notifyAll");
  }

  public class Handler implements InvocationHandler, Serializable {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
        return this.invokeImpl(proxy, method, args);
      } catch (TargetError var10) {
        TargetError ee = var10;
        Throwable t = var10.getTarget();
        Class c = t.getClass();
        String msg = t.getMessage();

        try {
          Throwable e = msg == null?(Throwable)c.getConstructor(new Class[0]).newInstance(new Object[0]):(Throwable)c.getConstructor(new Class[]{String.class}).newInstance(new Object[]{msg});
          e = org.d6r.Reflect.causeOrElse(e, ee);
          org.d6r.Reflect.setfldval(e, "detailMessage", String.format(StringUtils.join(Arrays.asList(new String[]{"%s\n", "Method: %s\n", "Arguments: %s\n"}), "\n"), new Object[]{org.d6r.Reflect.getfldval(e, "detailMessage"), method.toGenericString(), Arrays.toString(args)}));
          e.getStackTrace();
          org.d6r.Reflect.setfldval(e, "stackState", args);
          throw e;
        } catch (NoSuchMethodException var9) {
          throw org.d6r.Reflect.causeOrElse(t, var9);
        }
      } catch (EvalError var11) {
        if(Interpreter.DEBUG) {
          Interpreter.debug(String.format("EvalError in scripted interface \'%s\':\n  %s\n", new Object[]{This.this.toString(), StringUtils.join(ExceptionUtils.getRootCauseStackTrace(var11), "\n  ")}));
        }

        throw var11;
      }
    }

    public Object invokeImpl(Object proxy, Method method, Object[] args) throws EvalError {
      String methodName = method.getName();
      new CallStack(This.this.namespace);
      BshMethod equalsMethod = null;
      if(methodName.equals("equals")) {
        try {
          equalsMethod = This.this.namespace.getMethod("equals", new Class[]{Object.class});
        } catch (UtilEvalError var12) {
          ;
        }

        if(equalsMethod == null) {
          Object toStringMethod1 = args[0];
          if(org.d6r.Reflect.getfldval(org.d6r.Reflect.getfldval(proxy, "h"), "this$0") == toStringMethod1) {
            return Boolean.valueOf(true);
          }

          return Boolean.valueOf(false);
        }
      }

      BshMethod toStringMethod = null;
      if(methodName.equals("toString")) {
        try {
          toStringMethod = This.this.namespace.getMethod("toString", new Class[0]);
        } catch (UtilEvalError var11) {
          ;
        }

        if(toStringMethod == null) {
          return org.d6r.Reflect.getfldval(org.d6r.Reflect.getfldval(proxy, "h"), "this$0").toString();
        }
      }

      BshMethod hashCodeMethod = null;
      if(methodName.equals("toString")) {
        try {
          hashCodeMethod = This.this.namespace.getMethod("hashCode", new Class[0]);
        } catch (UtilEvalError var10) {
          ;
        }

        if(hashCodeMethod == null) {
          return Integer.valueOf(System.identityHashCode(org.d6r.Reflect.getfldval(org.d6r.Reflect.getfldval(proxy, "h"), "this$0")));
        }
      }

      Class[] paramTypes = method.getParameterTypes();
      return Primitive.unwrap(This.this.invokeMethod(methodName, Primitive.wrap(args, paramTypes)));
    }
  }
}