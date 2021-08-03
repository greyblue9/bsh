package bsh.operators;

import java.io.Serializable;
import java.lang.reflect.Method;

public abstract class ExtendedMethod implements Serializable {
  public Method[] castMethods;
  public Class[] castedTypes;
  public Class resultType;

  public abstract Object eval(Object... var1) throws RuntimeException;

  public boolean matchTypes(Class... types) {
    return this.castedTypes != null || types != null && types.length != 0?this.castedTypes.equals(types):true;
  }

  public abstract String getName();
}
