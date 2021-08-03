package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.KindNode;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;

class BSHReturnStatement extends SimpleNode implements KindNode, ParserConstants {
  BSHReturnStatement(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    Object value;
    if(this.jjtGetNumChildren() > 0) {
      value = ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
    } else {
      value = Primitive.VOID;
    }

    return new ReturnControl(this.kind, value, this);
  }
}
