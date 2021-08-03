package bsh;

import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Name;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import java.util.Arrays;
import org.d6r.Debug;

class BSHAmbiguousName extends SimpleNode {
  public String text;

  BSHAmbiguousName(int id) {
    super(id);
  }

  public Name getName(BshBinding namespace) {
    if(namespace == null && Interpreter.TRACE) {
      System.err.printf("[ERROR] BSHAmbiguousName #  Object getName(BshBinding namespace) :\n  namespace == null\n  this = %s\n", new Object[]{Debug.ToString(this)});
    }

    return namespace.getNameResolver(this.text);
  }

  public Object toObject(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(callstack.top() == null && Interpreter.TRACE) {
      System.err.printf("[ERROR] BSHAmbiguousName #  Object toObject(CallStack callstack, Interpreter interpreter) :\n  callstack.top() returned null\n  callstack = %s\n", new Object[]{Debug.ToString(callstack)});
    }

    return this.toObject(callstack, interpreter, false);
  }

  Object toObject(CallStack callstack, Interpreter interpreter, boolean forceClass) throws EvalError {
    if(callstack.top() == null && Interpreter.TRACE) {
      System.err.printf("[ERROR] BSHAmbiguousName #  Object toObject(CallStack callstack = %s, Interpreter interpreter = %s, boolean forceClass = %s) :\n  callstack.top() returned null\n  stack = %s\n", new Object[]{Debug.ToString(callstack), Debug.ToString(interpreter), Debug.ToString(Boolean.valueOf(forceClass)), Arrays.toString(Thread.currentThread().getStackTrace())});
    }

    try {
      return this.getName(callstack.top())
        .toObject(callstack, interpreter, forceClass);
    } catch (UtilEvalError var5) {
      if (forceClass) {
        int lastDot = text.lastIndexOf('.');
        if (lastDot != -1) {
          String className = text.substring(0, lastDot);
          String memberName = text.substring(lastDot + 1);
          if (bsh.Capabilities.classExists(className)) {
            try {
              Class<?> cls
                = callstack.top().getNameResolver(className).toClass();
              java.lang.reflect.Field fld = cls.getDeclaredField(memberName);
              if (fld != null) {
                return fld;
              }
            } catch (Error | ReflectiveOperationException e) {
              if (Interpreter.TRACE) e.printStackTrace();
            }
          }          
        }
      }
      throw var5.toEvalError(this, callstack);
    }
  }
  
  public Class toClass(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(callstack.top() == null && Interpreter.TRACE) {
      System.err.printf("[ERROR] BSHAmbiguousName #  Class toClass(CallStack callstack = %s, Interpreter interpreter = %s) :\n  callstack.top() returned null\n  stack = %s\n", new Object[]{Debug.ToString(callstack), Debug.ToString(interpreter), Arrays.toString(Thread.currentThread().getStackTrace())});
    }

    try {
      return this.getName(callstack.top()).toClass();
    } catch (ClassNotFoundException var4) {
      throw new EvalError(var4.getMessage(), this, callstack, var4);
    } catch (UtilEvalError var5) {
      throw var5.toEvalError(this, callstack);
    }
  }

  public LHS toLHS(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(callstack.top() == null && Interpreter.TRACE) {
      System.err.printf("[ERROR] BSHAmbiguousName #  LHS toLHS(CallStack callstack, Interpreter interpreter) :\n  callstack.top() returned null\n  callstack = %s\n", new Object[]{Debug.ToString(callstack)});
    }

    try {
      return this.getName(callstack.top()).toLHS(callstack, interpreter);
    } catch (UtilEvalError var4) {
      throw var4.toEvalError(this, callstack);
    }
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    throw new InterpreterError("Don\'t know how to eval an ambiguous name!  Use toObject() if you want an object.");
  }

  public String toString() {
    return this.text.trim();
  }
}