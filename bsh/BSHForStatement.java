package bsh;

import bsh.BSHIfStatement;
import bsh.BlockNameSpace;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;

class BSHForStatement extends SimpleNode implements ParserConstants {
  public boolean hasForInit;
  public boolean hasExpression;
  public boolean hasForUpdate;
  private SimpleNode forInit;
  private SimpleNode expression;
  private SimpleNode forUpdate;
  private SimpleNode statement;
  private boolean parsed;
  public SimpleNode expr = null;

  BSHForStatement(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int i = 0;
    if(this.hasForInit) {
      this.forInit = (SimpleNode)this.jjtGetChild(i++);
    }

    if(this.hasExpression) {
      this.expression = (SimpleNode)this.jjtGetChild(i++);
    }

    if(this.hasForUpdate) {
      this.forUpdate = (SimpleNode)this.jjtGetChild(i++);
    }

    if(i < this.jjtGetNumChildren()) {
      this.statement = (SimpleNode)this.jjtGetChild(i);
    }

    BshBinding enclosingNameSpace = callstack.top();
    BlockNameSpace forNameSpace = (BlockNameSpace)Factory.get(BlockNameSpace.class).make(new Object[]{enclosingNameSpace});
    callstack.swap(forNameSpace);
    if(this.hasForInit) {
      this.forInit.eval(callstack, interpreter);
    }

    Object returnControl = Primitive.VOID;

    while(true) {
      boolean breakout;
      if(this.hasExpression) {
        breakout = BSHIfStatement.evaluateCondition(this.expression, callstack, interpreter);
        if(!breakout) {
          break;
        }
      }

      breakout = false;
      if(this.statement != null) {
        Object ret = this.statement.eval(callstack, interpreter);
        if(ret instanceof ReturnControl) {
          switch(((ReturnControl)ret).kind) {
          case 12:
            breakout = true;
          case 19:
          default:
            break;
          case 46:
            returnControl = ret;
            breakout = true;
          }
        }
      }

      if(breakout) {
        break;
      }

      if(this.hasForUpdate) {
        this.forUpdate.eval(callstack, interpreter);
      }
    }

    callstack.swap(enclosingNameSpace);
    return returnControl;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(760);
    sb.append(this.firstToken.image);
    sb.append(" (");
    int i = 0;
    if(this.hasForInit) {
      SimpleNode init = (SimpleNode)this.jjtGetChild(i++);
      sb.append(init.toString());
    }

    if(this.hasExpression) {
      SimpleNode expr = (SimpleNode)this.jjtGetChild(i++);
      if(i > 1) {
        sb.append("; ");
      }

      sb.append(expr.toString());
    }

    if(this.hasForUpdate) {
      SimpleNode upd = (SimpleNode)this.jjtGetChild(i++);
      if(i > 1) {
        sb.append("; ");
      }

      sb.append(upd.toString());
    }

    sb.append(") ");
    SimpleNode stmt = (SimpleNode)this.jjtGetChild(i++);
    sb.append(stmt.toString());
    return sb.toString();
  }
}
