package bsh;

import bsh.BSHType;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.SimpleNode;

class BSHFormalParameter extends SimpleNode {
  public static final Class UNTYPED = null;
  public String name;
  public Class type;
  public boolean isVarargs = false;

  BSHFormalParameter(int id) {
    super(id);
  }

  public String getTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    return this.jjtGetNumChildren() > 0?((BSHType)this.jjtGetChild(0)).getTypeDescriptor(callstack, interpreter, defaultPackage):"Ljava/lang/Object;";
  }

  public void setVarargs() {
    this.isVarargs = true;
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.jjtGetNumChildren() > 0) {
      this.type = ((BSHType)this.jjtGetChild(0)).getType(callstack, interpreter);
    } else {
      this.type = UNTYPED;
    }

    return this.type;
  }
}
