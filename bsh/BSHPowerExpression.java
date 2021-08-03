package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorType;

public class BSHPowerExpression extends SimpleNode {
  ExtendedMethod opMethod;

  BSHPowerExpression(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int nchild = this.jjtGetNumChildren();
    if(nchild == 1) {
      return ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
    } else {
      Object lhs = ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
      Object rhs = ((SimpleNode)this.jjtGetChild(1)).eval(callstack, interpreter);
      Object lhs2 = Primitive.unwrap(lhs);
      Object rhs2 = Primitive.unwrap(rhs);
      Class type1 = lhs2 != null?lhs2.getClass():null;
      Class type2 = rhs2 != null?rhs2.getClass():null;
      OperatorType opType = OperatorType.POWER;
      this.opMethod = OperatorProvider.findMethod(interpreter.getNameSpace(), opType.getMethodName(), this.opMethod, opType.getAllowLeftCast(), new Class[]{type1, type2});
      if(this.opMethod != null) {
        Object result = this.opMethod.eval(new Object[]{lhs2, rhs2});
        return Primitive.isWrapperType(result.getClass())?new Primitive(result):result;
      } else {
        throw new EvalError("No exponentiation method found for given data types.", this, callstack);
      }
    }
  }
}
