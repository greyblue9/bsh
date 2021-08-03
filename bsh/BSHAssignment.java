package bsh;

import bsh.BSHPrimaryExpression;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import org.d6r.Reflector.Util;

class BSHAssignment extends SimpleNode<EvalError> implements ParserConstants {
  public int operator;

  BSHAssignment(int id) {
    super(id);
  }

  public String toString() {
    return this.getText();
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.evalled) {
      return this;
    } else {
      BSHPrimaryExpression lhsNode = (BSHPrimaryExpression)this.jjtGetChild(0);
      if(lhsNode == null) {
        throw new InterpreterError("Error, null LHSnode");
      } else {
        boolean strictJava = interpreter.getStrictJava();
        LHS lhs = lhsNode.toLHS(callstack, interpreter);
        if(lhs == null) {
          throw new InterpreterError("Error, null LHS");
        } else {
          Object lhsValue = null;
          if(this.operator != 82) {
            try {
              lhsValue = lhs.getValue();
            } catch (UtilEvalError var11) {
              throw var11.toEvalError(this, callstack);
            }
          }

          SimpleNode rhsNode = (SimpleNode)this.jjtGetChild(1);

          Object rhs;
          try {
            rhs = rhsNode.eval(callstack, interpreter);
          } catch (Throwable var10) {
            throw Util.sneakyThrow(var10);
          }

          if(rhs == Primitive.VOID) {
            throw new EvalError("Void assignment.", this, callstack);
          } else {
            switch(this.operator) {
            case 82:
              return this.evalResult = lhs.assign(rhs, strictJava);
            case 119:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 103), strictJava);
            case 120:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 104), strictJava);
            case 121:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 105), strictJava);
            case 122:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 106), strictJava);
            case 123:
            case 124:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 107), strictJava);
            case 125:
            case 126:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 109), strictJava);
            case 127:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 111), strictJava);
            case 128:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 112), strictJava);
            case 129:
            case 130:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 113), strictJava);
            case 131:
            case 132:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 115), strictJava);
            case 133:
            case 134:
              return this.evalResult = lhs.assign(this.operation(lhsValue, rhs, 117), strictJava);
            default:
              throw new InterpreterError("unimplemented operator in assignment BSH");
            }
          }
        }
      }
    }
  }

  private Object operation(Object lhs, Object rhs, int kind) throws UtilEvalError {
    if(lhs instanceof String && rhs != Primitive.VOID) {
      if(kind != 103) {
        throw new UtilEvalError("Use of non + operator with String LHS");
      } else {
        return (String)lhs + rhs;
      }
    } else if(rhs == Primitive.VOID) {
      if(lhs instanceof LHS) {
        try {
          if(Interpreter.TRACE) {
            System.err.printf("attempt to unset %s ....", new Object[]{((LHS)lhs).varName});
          }

          ((LHS)lhs).nameSpace.unsetVariable(((LHS)lhs).varName);
        } catch (Throwable var5) {
          if(Interpreter.TRACE) {
            System.err.println("Failed: " + var5.toString());
          }
        }
      }

      return Primitive.NULL;
    } else {
      if(lhs instanceof Primitive || rhs instanceof Primitive) {
        label86: {
          if(lhs != Primitive.VOID && rhs != Primitive.VOID) {
            if(lhs != Primitive.NULL && rhs != Primitive.NULL) {
              break label86;
            }

            throw new UtilEvalError("Illegal use of null object or \'null\' literal");
          }

          throw new UtilEvalError("Illegal use of undefined object or \'void\' literal");
        }
      }

      if(!(lhs instanceof Boolean) && !(lhs instanceof Character) && !(lhs instanceof Number) && !(lhs instanceof Primitive) || !(rhs instanceof Boolean) && !(rhs instanceof Character) && !(rhs instanceof Number) && !(rhs instanceof Primitive)) {
        throw new UtilEvalError("Non primitive value in operator: " + lhs.getClass() + " " + tokenImage[kind] + " " + rhs.getClass());
      } else {
        return Primitive.binaryOperation(lhs, rhs, kind);
      }
    }
  }
}
