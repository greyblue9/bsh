package bsh;

import bsh.BSHAmbiguousName;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Name;
import bsh.UtilEvalError;
import java.util.Arrays;

public class BshAmbigName extends BSHAmbiguousName {
  public Integer id;

  public BshAmbigName(int id) {
    super(id);
    this.id = null;
    System.err.println(String.format("BshAmbigName(%s)", new Object[]{Integer.valueOf(id)}));
  }

  public BshAmbigName() {
    this(-1);
  }

  public BshAmbigName(int id, String text) {
    this(id);
    this.text = text;
    System.err.println(String.format("BshAmbigName(%s,\'%s\')", new Object[]{Integer.valueOf(id), text}));
  }

  public BshAmbigName(String text) {
    this();
    this.text = text;
  }

  public BshAmbigName(String text, int id) {
    this(id, text);
  }

  public Name getName(BshBinding namespace) {
    try {
      return namespace.getNameResolver(this.text);
    } catch (Throwable var3) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var3.getMessage(), var3.getClass().getSimpleName(), " namespace.getNameResolver ", Arrays.toString(new Object[]{this.text}), " getName ", Arrays.toString(new Object[]{namespace})}));
      return null;
    }
  }

  public Object toObject(CallStack callstack, Interpreter interpreter) throws EvalError {
    try {
      return this.toObject(callstack, interpreter, false);
    } catch (Throwable var4) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var4.getMessage(), var4.getClass().getSimpleName(), " toObject ", Arrays.toString(new Object[]{callstack, interpreter, Boolean.valueOf(false)}), " toObject ", Arrays.toString(new Object[]{callstack, interpreter})}));
      return null;
    }
  }

  public Object toObject(CallStack callstack, Interpreter interpreter, boolean forceClass) throws EvalError {
    try {
      return this.getName(callstack.top()).toObject(callstack, interpreter, forceClass);
    } catch (UtilEvalError var5) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var5.getMessage(), var5.getClass().getSimpleName(), " getName ", Arrays.toString(new Object[]{callstack.top()}), " toObject ", Arrays.toString(new Object[]{callstack, interpreter, Boolean.valueOf(forceClass)})}));
      throw var5.toEvalError(this, callstack);
    }
  }

  public Class toClass(CallStack callstack, Interpreter interpreter) throws EvalError {
    try {
      return this.getName(callstack.top()).toClass();
    } catch (ClassNotFoundException var4) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var4.getMessage(), var4.getClass().getSimpleName(), " getName ", Arrays.toString(new Object[]{callstack.top()}), " toClass ", Arrays.toString(new Object[0])}));
      throw new EvalError(var4.getMessage(), this, callstack);
    } catch (UtilEvalError var5) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var5.getMessage(), var5.getClass().getSimpleName(), " getName ", Arrays.toString(new Object[]{callstack.top()}), " toClass ", Arrays.toString(new Object[0])}));
      throw var5.toEvalError(this, callstack);
    }
  }

  public LHS toLHS(CallStack callstack, Interpreter interpreter) throws EvalError {
    Name name = null;

    try {
      name = this.getName(callstack.top());
      return name.toLHS(callstack, interpreter);
    } catch (UtilEvalError var5) {
      System.err.println(String.format("[ERROR] %s:\n  got %s calling:\n   %s(%s)\n  from:    %s(%s)", new Object[]{var5.getMessage(), var5.getClass().getSimpleName(), " toLHS ", Arrays.toString(new Object[]{name}), " toLHS ", Arrays.toString(new Object[]{callstack, interpreter})}));
      throw var5.toEvalError(this, callstack);
    }
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    throw new InterpreterError("Don\'t know how to eval an ambiguous name!  Use toObject() if you want an object.");
  }

  public String toString() {
    return "AmbigousName: " + this.text;
  }
}
