package bsh;

import bsh.BSHType;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.SimpleNode;

class BSHReturnType extends SimpleNode {
  public boolean isVoid;

  BSHReturnType(int id) {
    super(id);
  }

  BSHType getTypeNode() {
    return (BSHType)this.jjtGetChild(0);
  }

  public String getTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    return this.isVoid?"V":this.getTypeNode().getTypeDescriptor(callstack, interpreter, defaultPackage);
  }

  public Class evalReturnType(CallStack callstack, Interpreter interpreter) throws EvalError {
    return this.isVoid?Void.TYPE:this.getTypeNode().getType(callstack, interpreter);
  }
}
