package bsh.operators;

import bsh.operators.ExtendedMethod;
import java.io.Serializable;
import java.lang.reflect.Method;
import org.d6r.Reflector;
import org.d6r.Reflector.Util;

public class BasicMethod extends ExtendedMethod implements Serializable {
  public Method opMethod;

  public BasicMethod(Method opMethod) {
    this.opMethod = opMethod;
    this.resultType = opMethod.getReturnType();
  }

  public BasicMethod(Method opMethod, Method leftCastMethod, Class leftCastType, Method rightCastMethod, Class rightCastType) {
    this.opMethod = opMethod;
    this.castMethods = new Method[]{leftCastMethod, rightCastMethod};
    this.castedTypes = new Class[]{leftCastType, rightCastType};
    this.resultType = opMethod.getReturnType();
  }

  public BasicMethod(Method opMethod, Method[] castMethods, Class[] castTypes) {
    this.opMethod = opMethod;
    this.castMethods = castMethods;
    this.castedTypes = castTypes;
    this.resultType = opMethod.getReturnType();
  }

  public Object eval(Object... args) throws RuntimeException {
    try {
      if(this.castMethods != null) {
        if(this.castMethods.length != args.length) {
          throw new IllegalArgumentException("Number of arguments does not match.");
        }

        for(int ex = 0; ex < this.castMethods.length; ++ex) {
          Method var5 = this.castMethods[ex];
          if(var5 != null) {
            args[ex] = var5.invoke((Object)null, new Object[]{args[ex]});
          }
        }
      }

      return this.opMethod.invoke((Object)null, args);
    } catch (ReflectiveOperationException var4) {
      Throwable cause = Reflector.getRootCause(var4);
      cause.printStackTrace();
      return Util.sneakyThrow(cause);
    }
  }

  public String getName() {
    return this.opMethod.getName();
  }
}
