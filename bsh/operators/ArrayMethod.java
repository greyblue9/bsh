package bsh.operators;

import bsh.operators.ExtendedMethod;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ArrayMethod extends ExtendedMethod implements Serializable {
  public ExtendedMethod elementMethod;

  public ArrayMethod(ExtendedMethod elementMethod, Class[] castTypes, boolean[] isArrayElement) {
    this.elementMethod = elementMethod;
    this.castedTypes = castTypes;
    if(elementMethod.resultType != null) {
      this.resultType = Array.newInstance(elementMethod.resultType, 0).getClass();
    }

    this.castMethods = new Method[castTypes.length];

    for(int i = 0; i < isArrayElement.length; ++i) {
      if(!isArrayElement[i]) {
        this.castMethods[i] = elementMethod.castMethods[i];
        elementMethod.castMethods[i] = null;
      }
    }

  }

  public Object eval(Object... args) throws RuntimeException {
    try {
      if(this.castMethods != null) {
        if(this.castMethods.length != args.length) {
          throw new IllegalArgumentException("Number of arguments does not match.");
        }

        for(int ex = 0; ex < this.castMethods.length; ++ex) {
          Method len = this.castMethods[ex];
          if(len != null) {
            args[ex] = len.invoke((Object)null, new Object[]{args[ex]});
          }
        }
      }

      boolean[] var11 = new boolean[args.length];
      int var12 = -1;

      int i;
      for(int result = 0; result < args.length; ++result) {
        Object argsi = args[result];
        if(argsi.getClass().isArray()) {
          var11[result] = true;
          i = Array.getLength(argsi);
          if(var12 == -1) {
            var12 = i;
          }

          if(var12 != i) {
            throw new IllegalArgumentException("Arrays have different lengths");
          }
        } else {
          var11[result] = false;
        }
      }

      Object var13 = Array.newInstance(this.elementMethod.resultType, var12);
      Object[] var14 = new Object[args.length];

      for(i = 0; i < var12; ++i) {
        for(int resulti = 0; resulti < args.length; ++resulti) {
          var14[resulti] = args[resulti];
          if(var11[resulti]) {
            var14[resulti] = Array.get(var14[resulti], i);
          }
        }

        Object var15 = this.elementMethod.eval(var14);
        Array.set(var13, i, var15);
      }

      return var13;
    } catch (IllegalAccessException var8) {
      throw new RuntimeException("Error evaluating method: " + this.elementMethod.getName(), var8);
    } catch (IllegalArgumentException var9) {
      throw new RuntimeException("Error evaluating method: " + this.elementMethod.getName(), var9);
    } catch (InvocationTargetException var10) {
      throw new RuntimeException("Error evaluating method: " + this.elementMethod.getName(), var10);
    }
  }

  public String getName() {
    return this.elementMethod.getName();
  }
}
