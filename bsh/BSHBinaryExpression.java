package bsh;

import bsh.BSHType;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.KindNode;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.Token;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorType;

class BSHBinaryExpression extends SimpleNode implements KindNode, ParserConstants {
  ExtendedMethod opMethod = null;

  BSHBinaryExpression(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    Object lhs = ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
    if(this.kind == 35) {
      if(lhs == Primitive.NULL) {
        return new Primitive(false);
      } else {
        Class isLhsWrapper2 = ((BSHType)this.jjtGetChild(1)).getType(callstack, interpreter);
        if(lhs instanceof Primitive) {
          return isLhsWrapper2 == Primitive.class?new Primitive(true):new Primitive(false);
        } else {
          boolean rhs1 = Types.isJavaBaseAssignable(isLhsWrapper2, lhs.getClass());
          return new Primitive(rhs1);
        }
      }
    } else {
      Object isLhsWrapper;
      if(this.kind == 99 || this.kind == 100) {
        isLhsWrapper = lhs;
        if(this.isPrimitiveValue(lhs)) {
          isLhsWrapper = ((Primitive)lhs).getValue();
        }

        if(isLhsWrapper instanceof Boolean && !((Boolean)isLhsWrapper).booleanValue()) {
          return new Primitive(false);
        }
      }

      if(this.kind == 97 || this.kind == 98) {
        isLhsWrapper = lhs;
        if(this.isPrimitiveValue(lhs)) {
          isLhsWrapper = ((Primitive)lhs).getValue();
        }

        if(isLhsWrapper instanceof Boolean && ((Boolean)isLhsWrapper).booleanValue()) {
          return new Primitive(true);
        }
      }

      boolean isLhsWrapper1 = this.isWrapper(lhs);
      Object rhs = ((SimpleNode)this.jjtGetChild(1)).eval(callstack, interpreter);
      boolean isRhsWrapper = this.isWrapper(rhs);
      OperatorType opType = OperatorType.getType(this.kind);
      if(opType != null) {
        Object e = Primitive.unwrap(lhs);
        Object rhs2 = Primitive.unwrap(rhs);
        Class lhc = e != null?e.getClass():null;
        Class rhc = rhs2 != null?rhs2.getClass():null;
        if(lhc != null && rhc != null) {
          this.opMethod = OperatorProvider.findMethod(interpreter.getNameSpace(), opType.getMethodName(), this.opMethod, opType.getAllowLeftCast(), new Class[]{lhc, rhc});
          if(this.opMethod != null) {
            Object result = this.opMethod.eval(new Object[]{e, rhs2});
            if(Primitive.isWrapperType(result.getClass())) {
              return new Primitive(result);
            }

            return result;
          }
        }
      }

      if(!isLhsWrapper1 && !this.isPrimitiveValue(lhs) || !isRhsWrapper && !this.isPrimitiveValue(rhs) || isLhsWrapper1 && isRhsWrapper && this.kind == 91) {
        switch(this.kind) {
        case 91:
          return new Primitive(lhs == rhs);
        case 96:
          return new Primitive(lhs != rhs);
        case 103:
          if(lhs instanceof String || rhs instanceof String) {
            return lhs.toString() + rhs.toString();
          }
        }

        if(lhs instanceof Primitive || rhs instanceof Primitive) {
          if(lhs == Primitive.VOID || rhs == Primitive.VOID) {
            throw new EvalError("illegal use of undefined variable, class, or \'void\' literal", this, callstack);
          }

          if(lhs == Primitive.NULL || rhs == Primitive.NULL) {
            throw new EvalError("illegal use of null value or \'null\' literal", this, callstack);
          }
        }

        throw new EvalError("Operator: \'" + tokenImage[this.kind] + "\' inappropriate for objects", this, callstack);
      } else {
        try {
          return Primitive.binaryOperation(lhs, rhs, this.kind);
        } catch (UtilEvalError var13) {
          throw var13.toEvalError(this, callstack);
        }
      }
    }
  }

  private boolean isPrimitiveValue(Object obj) {
    return obj instanceof Primitive && obj != Primitive.VOID && obj != Primitive.NULL;
  }

  private boolean isWrapper(Object obj) {
    return obj instanceof Boolean || obj instanceof Character || obj instanceof Number;
  }

  public String toString() {
    String left = this.children[0].getText();
    String right = this.children[1].getText();
    String op = Token.getKindString(this.kind);
    String tostr = String.format("%s %s %s", new Object[]{left, op, right});
    return tostr;
  }
}
