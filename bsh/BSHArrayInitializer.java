package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.Reflect;
import bsh.SimpleNode;
import bsh.Types;
import bsh.UtilEvalError;
import java.lang.reflect.Array;

class BSHArrayInitializer extends SimpleNode {
  BSHArrayInitializer(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    throw new EvalError("Array initializer has no base type.", this, callstack);
  }

  public Object eval(Class baseType, int dimensions, CallStack callstack, Interpreter interpreter) throws EvalError {
    int numInitializers = this.jjtGetNumChildren();
    int[] dima = new int[dimensions];
    dima[0] = numInitializers;
    Object initializers = Array.newInstance(baseType, dima);

    for(int i = 0; i < numInitializers; ++i) {
      SimpleNode node = (SimpleNode)this.jjtGetChild(i);
      Object currentInitializer;
      if(node instanceof BSHArrayInitializer) {
        if(dimensions < 2) {
          throw new EvalError("Invalid Location for Intializer, position: " + i, this, callstack);
        }

        currentInitializer = ((BSHArrayInitializer)node).eval(baseType, dimensions - 1, callstack, interpreter);
      } else {
        currentInitializer = node.eval(callstack, interpreter);
      }

      if(currentInitializer == Primitive.VOID) {
        throw new EvalError("Void in array initializer, position" + i, this, callstack);
      }

      Object value = currentInitializer;
      if(dimensions == 1) {
        try {
          value = Types.castObject(currentInitializer, baseType, 0);
        } catch (UtilEvalError var13) {
          throw var13.toEvalError("Error in array initializer", this, callstack);
        }

        value = Primitive.unwrap(value);
      }

      try {
        Array.set(initializers, i, value);
      } catch (IllegalArgumentException var14) {
        Interpreter.debug("illegal arg" + var14);
        this.throwTypeError(baseType, currentInitializer, i, callstack);
      } catch (ArrayStoreException var15) {
        Interpreter.debug("arraystore" + var15);
        this.throwTypeError(baseType, currentInitializer, i, callstack);
      }
    }

    return initializers;
  }

  private void throwTypeError(Class baseType, Object initializer, int argNum, CallStack callstack) throws EvalError {
    String rhsType;
    if(initializer instanceof Primitive) {
      rhsType = ((Primitive)initializer).getType().getName();
    } else {
      rhsType = Reflect.normalizeClassName(initializer.getClass());
    }

    throw new EvalError("Incompatible type: " + rhsType + " in initializer of array type: " + baseType + " at position: " + argNum, this, callstack);
  }
}
