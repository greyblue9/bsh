package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorType;

public class BSHRangeExpression extends SimpleNode {
  private ExtendedMethod opMethod = null;

  public BSHRangeExpression(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int length = this.jjtGetNumChildren();
    Object start = ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
    if(length == 1) {
      return start;
    } else {
      Object end = ((SimpleNode)this.jjtGetChild(1)).eval(callstack, interpreter);
      Object inc = null;
      if(length == 3) {
        inc = end;
        end = ((SimpleNode)this.jjtGetChild(2)).eval(callstack, interpreter);
      }

      start = Primitive.unwrap(start);
      end = Primitive.unwrap(end);
      Object[] args = null;
      Class[] types = null;
      if(inc != null) {
        inc = Primitive.unwrap(inc);
        args = new Object[]{start, end, inc};
        types = new Class[]{start.getClass(), end.getClass(), inc.getClass()};
      } else {
        args = new Object[]{start, end};
        types = new Class[]{start.getClass(), end.getClass()};
      }

      this.opMethod = OperatorProvider.findMethod(interpreter.getNameSpace(), OperatorType.RANGE.getMethodName(), this.opMethod, OperatorType.RANGE.getAllowLeftCast(), types);
      if(this.opMethod != null) {
        Object result = this.opMethod.eval(args);
        return Primitive.isWrapperType(result.getClass())?new Primitive(result):result;
      } else {
        throw new EvalError("Range function not found for given data types", this, callstack);
      }
    }
  }
}
