package bsh;

import bsh.BSHSwitchLabel;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Node;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;
import bsh.UtilEvalError;

class BSHSwitchStatement extends SimpleNode implements ParserConstants {
  public BSHSwitchStatement(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int numchild = this.jjtGetNumChildren();
    byte child = 0;
    int var12 = child + 1;
    SimpleNode switchExp = (SimpleNode)this.jjtGetChild(child);
    Object switchVal = switchExp.eval(callstack, interpreter);
    ReturnControl returnControl = null;
    if(var12 >= numchild) {
      throw new EvalError("Empty switch statement.", this, callstack);
    } else {
      BSHSwitchLabel label = (BSHSwitchLabel)this.jjtGetChild(var12++);

      while(var12 < numchild && returnControl == null) {
        Node node;
        if(!label.isDefault && !this.primitiveEquals(switchVal, label.eval(callstack, interpreter), callstack, switchExp)) {
          while(var12 < numchild) {
            node = this.jjtGetChild(var12++);
            if(node instanceof BSHSwitchLabel) {
              label = (BSHSwitchLabel)node;
              break;
            }
          }
        } else {
          while(var12 < numchild) {
            node = this.jjtGetChild(var12++);
            if(!(node instanceof BSHSwitchLabel)) {
              Object value = ((SimpleNode)node).eval(callstack, interpreter);
              if(value instanceof ReturnControl) {
                returnControl = (ReturnControl)value;
                break;
              }
            }
          }
        }
      }

      return returnControl != null && returnControl.kind == 46?returnControl:Primitive.VOID;
    }
  }

  private boolean primitiveEquals(Object switchVal, Object targetVal, CallStack callstack, SimpleNode switchExp) throws EvalError {
    if(!(switchVal instanceof Primitive) && !(targetVal instanceof Primitive)) {
      return switchVal.equals(targetVal);
    } else {
      try {
        Object e = Primitive.binaryOperation(switchVal, targetVal, 91);
        e = Primitive.unwrap(e);
        return e.equals(Boolean.TRUE);
      } catch (UtilEvalError var6) {
        throw var6.toEvalError("Switch value: " + switchExp.getText() + ": ", this, callstack);
      }
    }
  }
}
