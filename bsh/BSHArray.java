package bsh;

import bsh.BSHRangeExpression;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorUtil;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

class BSHArray extends SimpleNode {
  BSHArray(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    int length = this.jjtGetNumChildren();
    ArrayList result = new ArrayList(length);
    Class elementType = null;
    boolean mixedType = false;
    if(length == 0) {
      return new Object[0];
    } else {
      int expandedLength = 0;

      int ii;
      for(ii = 0; ii < length; ++ii) {
        SimpleNode rarray = (SimpleNode)this.jjtGetChild(ii);
        Object i = rarray.eval(callstack, interpreter);
        i = Primitive.unwrap(i);
        Class range = i.getClass();
        result.add(i);
        if(rarray instanceof BSHRangeExpression) {
          if(!range.isArray()) {
            throw new EvalError("Range expression result is not an array", this, callstack);
          }

          range = range.getComponentType();
          if(range.isPrimitive()) {
            range = Primitive.boxType(range);
          }

          expandedLength += Array.getLength(i);
        } else {
          ++expandedLength;
        }

        if(ii == 0) {
          elementType = range;
        } else if(!elementType.isAssignableFrom(range)) {
          elementType = OperatorUtil.commonSuperclass(new Class[]{elementType, range});
        }
      }

      ii = 0;
      int j;
      int var20;
      if(Integer.class.isAssignableFrom(elementType)) {
        int[] var17 = new int[expandedLength];

        for(var20 = 0; var20 < result.size(); ++var20) {
          if(this.jjtGetChild(var20) instanceof BSHRangeExpression) {
            int[] var22 = this.castToint(result.get(var20), callstack, interpreter.getNameSpace());

            for(j = 0; j < var22.length; ++j) {
              var17[ii++] = var22[j];
            }
          } else {
            var17[ii++] = ((Integer)result.get(var20)).intValue();
          }
        }

        return var17;
      } else if(Float.class.isAssignableFrom(elementType)) {
        float[] var16 = new float[expandedLength];

        for(var20 = 0; var20 < result.size(); ++var20) {
          if(this.jjtGetChild(var20) instanceof BSHRangeExpression) {
            float[] var24 = this.castTofloat(result.get(var20), callstack, interpreter.getNameSpace());

            for(j = 0; j < var24.length; ++j) {
              var16[ii++] = var24[j];
            }
          } else {
            var16[ii++] = ((Float)result.get(var20)).floatValue();
          }
        }

        return var16;
      } else if(Number.class.isAssignableFrom(elementType)) {
        double[] var19 = new double[expandedLength];

        for(var20 = 0; var20 < result.size(); ++var20) {
          if(this.jjtGetChild(var20) instanceof BSHRangeExpression) {
            double[] var23 = this.castTodouble(result.get(var20), callstack, interpreter.getNameSpace());

            for(j = 0; j < var23.length; ++j) {
              var19[ii++] = var23[j];
            }
          } else {
            var19[ii++] = ((Number)result.get(var20)).doubleValue();
          }
        }

        return var19;
      } else if(Boolean.class.isAssignableFrom(elementType)) {
        boolean[] var18 = new boolean[result.size()];

        for(var20 = 0; var20 < result.size(); ++var20) {
          var18[var20] = ((Boolean)result.get(var20)).booleanValue();
        }

        return var18;
      } else if(String.class.isAssignableFrom(elementType)) {
        String[] var14 = new String[result.size()];

        for(var20 = 0; var20 < result.size(); ++var20) {
          var14[var20] = (String)result.get(var20);
        }

        return var14;
      } else if(!Character.class.isAssignableFrom(elementType)) {
        Object[] var15 = (Object[])Array.newInstance(elementType, result.size());
        return result.toArray(var15);
      } else {
        char[] var13 = new char[expandedLength];

        for(var20 = 0; var20 < result.size(); ++var20) {
          if(this.jjtGetChild(var20) instanceof BSHRangeExpression) {
            char[] var21 = (char[])result.get(var20);

            for(j = 0; j < var21.length; ++j) {
              var13[ii++] = var21[j];
            }
          } else {
            var13[ii++] = ((Character)result.get(var20)).charValue();
          }
        }

        return var13;
      }
    }
  }

  private int[] castToint(Object array, CallStack callstack, BshBinding ns) throws EvalError {
    Class targetc = (new int[0]).getClass();
    if(targetc.isInstance(array)) {
      return (int[])array;
    } else {
      try {
        Method ex = OperatorProvider.findCastMethod(ns, array.getClass(), targetc, (Method)null);
        if(ex == null) {
          throw new EvalError("Cannot convert range type to array type", this, callstack);
        } else {
          return (int[])ex.invoke((Object)null, new Object[]{array});
        }
      } catch (IllegalAccessException var6) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (IllegalArgumentException var7) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (InvocationTargetException var8) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      }
    }
  }

  private float[] castTofloat(Object array, CallStack callstack, BshBinding ns) throws EvalError {
    Class targetc = (new float[0]).getClass();
    if(targetc.isInstance(array)) {
      return (float[])array;
    } else {
      try {
        Method ex = OperatorProvider.findCastMethod(ns, array.getClass(), targetc, (Method)null);
        if(ex == null) {
          throw new EvalError("Cannot convert range type to array type", this, callstack);
        } else {
          return (float[])ex.invoke((Object)null, new Object[]{array});
        }
      } catch (IllegalAccessException var6) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (IllegalArgumentException var7) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (InvocationTargetException var8) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      }
    }
  }

  private double[] castTodouble(Object array, CallStack callstack, BshBinding ns) throws EvalError {
    Class targetc = (new double[0]).getClass();
    if(targetc.isInstance(array)) {
      return (double[])array;
    } else {
      try {
        Method ex = OperatorProvider.findCastMethod(ns, array.getClass(), targetc, (Method)null);
        if(ex == null) {
          throw new EvalError("Cannot convert range type to array type", this, callstack);
        } else {
          return (double[])ex.invoke((Object)null, new Object[]{array});
        }
      } catch (IllegalAccessException var6) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (IllegalArgumentException var7) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      } catch (InvocationTargetException var8) {
        throw new EvalError("Cannot convert range type to array type", this, callstack);
      }
    }
  }
}
