package bsh.operators;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashSet;

public class OperatorUtil implements Serializable {
  public static final Integer NO_METHOD = new Integer(0);

  public static Object getAt(Object target, Object key) {
    return NO_METHOD;
  }

  public static Class commonSuperclass(Class... classes) {
    Class rc = null;

    for(int i = 0; i < classes.length; ++i) {
      Class tc = classes[i];
      if(tc != null && !tc.isInterface()) {
        if(rc == null) {
          rc = tc;
        } else {
          while(!rc.isAssignableFrom(tc)) {
            rc = rc.getSuperclass();
          }
        }

        if(rc == Object.class) {
          break;
        }
      }
    }

    return rc;
  }

  public static Class getArrayType(Class elementType, int ndim) {
    int[] dim = new int[ndim];
    Class type = Array.newInstance(elementType, dim).getClass();
    return type;
  }

  public static Class[] getAllSuperTypes(Class type) {
    if(type.isArray()) {
      return getAllArraySuperTypes(type);
    } else {
      HashSet types = new HashSet();

      while(type != null) {
        types.add(type);
        Class[] interfaces = type.getInterfaces();

        for(int i = 0; i < interfaces.length; ++i) {
          Class class1 = interfaces[i];
          types.add(class1);
        }

        if(type.isPrimitive()) {
          type = null;
        } else if(!type.equals(Object.class)) {
          type = type.getSuperclass();
          if(type != null && type.equals(Object.class)) {
            type = null;
          }
        } else {
          type = null;
        }
      }

      return (Class[])types.toArray(new Class[types.size()]);
    }
  }

  public static Class[] getAllArraySuperTypes(Class atype) {
    if(!atype.isArray()) {
      return getAllSuperTypes(atype);
    } else {
      int ndim = 0;

      Class type;
      for(type = atype; type.isArray(); type = type.getComponentType()) {
        ++ndim;
      }

      Class[] allElementTypes = getAllSuperTypes(type);
      Class[] allArrayTypes = new Class[allElementTypes.length];

      for(int i = 0; i < allElementTypes.length; ++i) {
        allArrayTypes[i] = getArrayType(allElementTypes[i], ndim);
      }

      return allArrayTypes;
    }
  }
}
