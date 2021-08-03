package bsh;

import bsh.BSHArrayInitializer;
import bsh.BSHType;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;

class BSHVariableDeclarator extends SimpleNode {
  public String name;

  BSHVariableDeclarator(int id) {
    super(id);
  }

  public Object eval(BSHType typeNode, CallStack callstack, Interpreter interpreter) throws EvalError {
    Object value = null;
    if(this.jjtGetNumChildren() > 0) {
      SimpleNode initializer = (SimpleNode)this.jjtGetChild(0);
      if(typeNode != null && initializer instanceof BSHArrayInitializer) {
        value = ((BSHArrayInitializer)initializer).eval(typeNode.getBaseType(), typeNode.getArrayDims(), callstack, interpreter);
      } else {
        value = initializer.eval(callstack, interpreter);
      }
    }

    if(value == Primitive.VOID) {
      throw new EvalError("Void initializer.", this, callstack);
    } else {
      return value;
    }
  }

  public String toString() {
    return this.name;
  }
}
