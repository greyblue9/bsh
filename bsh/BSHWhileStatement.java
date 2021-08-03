package bsh;

import bsh.BSHIfStatement;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;

class BSHWhileStatement extends SimpleNode implements ParserConstants {
  boolean isDoStatement;

  BSHWhileStatement(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int numChild = this.jjtGetNumChildren();
    SimpleNode condExp;
    SimpleNode body;
    if(this.isDoStatement) {
      condExp = (SimpleNode)this.jjtGetChild(1);
      body = (SimpleNode)this.jjtGetChild(0);
    } else {
      condExp = (SimpleNode)this.jjtGetChild(0);
      if(numChild > 1) {
        body = (SimpleNode)this.jjtGetChild(1);
      } else {
        body = null;
      }
    }

    boolean doOnceFlag = this.isDoStatement;

    while(doOnceFlag || BSHIfStatement.evaluateCondition(condExp, callstack, interpreter)) {
      doOnceFlag = false;
      if(body != null) {
        Object ret = body.eval(callstack, interpreter);
        if(ret instanceof ReturnControl) {
          switch(((ReturnControl)ret).kind) {
          case 12:
            return Primitive.VOID;
          case 19:
          default:
            break;
          case 46:
            return ret;
          }
        }
      }
    }

    return Primitive.VOID;
  }
}
