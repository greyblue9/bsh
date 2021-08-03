package org.d6r;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.d6r.Debug;

public class GroundZero {
  
  String className;
  Constructor<?> ctor;
  Constructor<?>[] ctors;
  Object[] args;
  Class<?>[] types;
  Throwable ex;
  Iterable<? extends Throwable> chain;
  Iterable<? extends CharSequence> info;
  List<Object> misc = new ArrayList<Object>();
  StackTraceElement[] trace;
  
  public GroundZero(Object... state) {
    trace = new Error().getStackTrace();
    for (Object o: state) this.initState(o);
  }
  
  public Object initState(Object o) {
    if (o instanceof Class<?>) {
      return className = ((Class<?>) o).getName();
    } if (o instanceof String) {
      return className = (String) o;
    } else if (o instanceof Constructor) {
      return ctor = (Constructor<?>) o;
    } else if (o instanceof Object[]) {
      Class<?> cmpType = o.getClass().getComponentType();
      if (cmpType == Constructor.class) {
        return ctors = (Constructor<?>[]) o;
      } else if (cmpType == Object.class) {
        return args = (Object[]) o;
      } else if (Class.class.isAssignableFrom(cmpType)) {
        return types = (Class<?>[]) o;
      }
      System.err.printf(
        "GroundZero: No idea what to do with array of type "
        + "%s[] (length = %d)\n",
        cmpType.getName(), ((Object[]) o).length
      );
    } else if (o instanceof Throwable) {
      return ex = (Throwable) o;
    } else if (o instanceof Iterable<?>) {
      Iterator<?> it = ((Iterable<?>) o).iterator();
      Object elem = null;
      while (elem == null && it.hasNext()) {
        if ((elem = it.next()) == null && !it.hasNext()) {
          return Boolean.FALSE;
        }
      }
      Class<?> elemCls = elem.getClass();
      if (Throwable.class.isAssignableFrom(elemCls)) {
        return chain = (Iterable<Throwable>) elem;
      } if (CharSequence.class.isAssignableFrom(elemCls)) {
        return info = (Iterable<CharSequence>) elem;
      }
    }
    misc.add(o);
    return o;
  }
  
  @Override
  public String toString() {
    return String.format(
        "  - cls = %s\n"
      + "  - Selected constructor (con) = %s\n"
      + "  - args = %s\n"
      + "  - types = %s\n"
      + "  - constructors = %s\n\n",
      className, ctor != null? ctor.toGenericString(): "null",
      Debug.ToString(Arrays.asList(args)),
      Arrays.asList(types),
      ctors != null
        ? Arrays.asList(ctors): "null"
    );
  }
}