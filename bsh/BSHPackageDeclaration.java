package bsh;

import bsh.BSHAmbiguousName;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;

public class BSHPackageDeclaration extends SimpleNode {
  public BSHPackageDeclaration(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    BSHAmbiguousName name = (BSHAmbiguousName)this.jjtGetChild(0);
    BshBinding namespace = callstack.top();
    namespace.setPackage(name.text);
    namespace.importPackage(name.text);
    return Primitive.VOID;
  }
}
