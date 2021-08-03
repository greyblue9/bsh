package bsh;

import bsh.BSHAmbiguousName;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import org.d6r.Debug;

public class BSHImportDeclaration extends SimpleNode {
  public boolean importPackage;
  public boolean staticImport;
  public boolean superImport;
  final Object VOID;

  BSHImportDeclaration(int id) {
    super(id);
    this.VOID = Primitive.VOID;
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    BshBinding namespace = callstack.top();
    if(this.superImport) {
      try {
        namespace.doSuperImport();
      } catch (UtilEvalError var5) {
        throw var5.toEvalError(this, callstack);
      }

      return this.VOID;
    } else if(this.staticImport) {
      if(this.importPackage) {
        Class name1 = ((BSHAmbiguousName)this.jjtGetChild(0)).toClass(callstack, interpreter);
        namespace.importStatic(name1);
        return this.VOID;
      } else {
        throw new EvalError("static field imports not supported yet", this, callstack);
      }
    } else {
      String name = ((BSHAmbiguousName)this.jjtGetChild(0)).text;
      if(this.importPackage) {
        namespace.importPackage(name);
        return this.VOID;
      } else {
        return this.tryImportClassInto(name, namespace, callstack);
      }
    }
  }

  Object tryImportClassInto(String name, BshBinding dest, CallStack callstack) throws EvalError {
    try {
      dest.importClass(name);
    } catch (UtilEvalError var5) {
      Interpreter.debug(String.format("Import class {name=\'%s\'} into ns{%s} failed:\n  Message = %s\n  callstack = %s\n  dest = %s\n", new Object[]{name != null?name:"null", dest != null?dest.toString():"null", var5.getMessage(), Debug.ToString(callstack), Debug.ToString(dest)}));
      throw var5.toEvalError(this, callstack);
    }

    return this.VOID;
  }
}
