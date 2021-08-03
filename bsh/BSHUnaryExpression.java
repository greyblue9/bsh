package bsh;

import bsh.BSHPrimaryExpression;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.KindNode;
import bsh.LHS;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.Token;
import bsh.UtilEvalError;

class BSHUnaryExpression extends SimpleNode implements KindNode, ParserConstants {
  public boolean postfix = false;

  BSHUnaryExpression(int id) {
    super(id);
  }

  public String getKindString(int kind) {
    return Token.getKindString(kind);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    SimpleNode node = (SimpleNode)this.jjtGetChild(0);

    try {
      if(this.kind != 101 && this.kind != 102) {
        return this.unaryOperation(node.eval(callstack, interpreter), this.kind);
      } else {
        LHS e = ((BSHPrimaryExpression)node).toLHS(callstack, interpreter);
        return this.lhsUnaryOperation(e, interpreter.getStrictJava());
      }
    } catch (UtilEvalError var5) {
      throw var5.toEvalError(this, callstack);
    }
  }

  private Object lhsUnaryOperation(LHS lhs, boolean strictJava) throws UtilEvalError {
    if(Interpreter.DEBUG) {
      Interpreter.debug("lhsUnaryOperation");
    }

    Object prevalue = lhs.getValue();
    Object postvalue = this.unaryOperation(prevalue, this.kind);
    Object retVal;
    if(this.postfix) {
      retVal = prevalue;
    } else {
      retVal = postvalue;
    }

    lhs.assign(postvalue, strictJava);
    return retVal;
  }

  private Object unaryOperation(Object op, int kind) throws UtilEvalError {
    if(!(op instanceof Boolean) && !(op instanceof Character) && !(op instanceof Number)) {
      if(!(op instanceof Primitive)) {
        throw new UtilEvalError("Unary operation " + tokenImage[kind] + " inappropriate for object");
      } else {
        return Primitive.unaryOperation((Primitive)op, kind);
      }
    } else {
      return this.primitiveWrapperUnaryOperation(op, kind);
    }
  }

  private Object primitiveWrapperUnaryOperation(Object val, int kind) throws UtilEvalError {
    Class operandType = val.getClass();
    Object operand = Primitive.promoteToInteger(val);
    if(operand instanceof Boolean) {
      return new Boolean(Primitive.booleanUnaryOperation((Boolean)operand, kind));
    } else if(!(operand instanceof Integer)) {
      if(operand instanceof Long) {
        return new Long(Primitive.longUnaryOperation((Long)operand, kind));
      } else if(operand instanceof Float) {
        return new Float(Primitive.floatUnaryOperation((Float)operand, kind));
      } else if(operand instanceof Double) {
        return new Double(Primitive.doubleUnaryOperation((Double)operand, kind));
      } else {
        throw new InterpreterError("An error occurred. Please call technical support.");
      }
    } else {
      int result = Primitive.intUnaryOperation((Integer)operand, kind);
      if(kind == 101 || kind == 102) {
        if(operandType == Byte.TYPE) {
          return new Byte((byte)result);
        }

        if(operandType == Short.TYPE) {
          return new Short((short)result);
        }

        if(operandType == Character.TYPE) {
          return new Character((char)result);
        }
      }

      return new Integer(result);
    }
  }
}
