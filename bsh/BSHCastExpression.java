package bsh;

import bsh.BSHType;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.operators.OperatorProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class BSHCastExpression extends SimpleNode {
  Method castMethod;

  public BSHCastExpression(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    BshBinding namespace = callstack.top();
    Class toType = ((BSHType)this.jjtGetChild(0)).getType(callstack, interpreter);
    SimpleNode expression = (SimpleNode)this.jjtGetChild(1);
    Object fromValue = expression.eval(callstack, interpreter);
    Class fromType = fromValue.getClass();

    try {
      Object e = Primitive.unwrap(fromValue);
      Class fromType2 = e != null?e.getClass():null;
      this.castMethod = OperatorProvider.findCastMethod(interpreter.getNameSpace(), fromType2, toType, this.castMethod);
      if(this.castMethod != null) {
        try {
          Object toValue = this.castMethod.invoke((Object)null, new Object[]{e});
          return Primitive.wrap(toValue, toType);
        } catch (IllegalAccessException var12) {
          ;
        } catch (IllegalArgumentException var13) {
          ;
        } catch (InvocationTargetException var14) {
          ;
        }
      }

      return Types.castObject(fromValue, toType, 0);
    } catch (UtilEvalError var15) {
      throw var15.toEvalError(this, callstack);
    }
  }
}
