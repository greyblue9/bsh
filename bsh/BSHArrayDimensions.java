package bsh;

import bsh.BSHArrayInitializer;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.Reflect;
import bsh.SimpleNode;
import java.lang.reflect.Array;

class BSHArrayDimensions extends SimpleNode {
  public Class baseType;
  public int numDefinedDims;
  public int numUndefinedDims;
  public int[] definedDimensions;

  BSHArrayDimensions(int id) {
    super(id);
  }

  public void addDefinedDimension() {
    ++this.numDefinedDims;
  }

  public void addUndefinedDimension() {
    ++this.numUndefinedDims;
  }

  public Object eval(Class type, CallStack callstack, Interpreter interpreter) throws EvalError {
    if(Interpreter.DEBUG) {
      Interpreter.debug("array base type = " + type);
    }

    this.baseType = type;
    return this.eval(callstack, interpreter);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    SimpleNode child = (SimpleNode)this.jjtGetChild(0);
    if(child instanceof BSHArrayInitializer) {
      if(this.baseType == null) {
        throw new EvalError("Internal Array Eval err:  unknown base type", this, callstack);
      } else {
        Object var10 = ((BSHArrayInitializer)child).eval(this.baseType, this.numUndefinedDims, callstack, interpreter);
        Class var11 = var10.getClass();
        int actualDimensions = Reflect.getArrayDimensions(var11);
        this.definedDimensions = new int[actualDimensions];
        if(this.definedDimensions.length != this.numUndefinedDims) {
          throw new EvalError("Incompatible initializer. Allocation calls for a " + this.numUndefinedDims + " dimensional array, but initializer is a " + actualDimensions + " dimensional array", this, callstack);
        } else {
          Object arraySlice = var10;

          for(int i1 = 0; i1 < this.definedDimensions.length; ++i1) {
            this.definedDimensions[i1] = Array.getLength(arraySlice);
            if(this.definedDimensions[i1] > 0) {
              arraySlice = Array.get(arraySlice, 0);
            }
          }

          return var10;
        }
      }
    } else {
      this.definedDimensions = new int[this.numDefinedDims];

      for(int i = 0; i < this.numDefinedDims; ++i) {
        try {
          Object e = ((SimpleNode)this.jjtGetChild(i)).eval(callstack, interpreter);
          this.definedDimensions[i] = ((Primitive)e).intValue();
        } catch (Exception var9) {
          throw new EvalError("Array index: " + i + " does not evaluate to an integer", this, callstack);
        }
      }

      return Primitive.VOID;
    }
  }
}
