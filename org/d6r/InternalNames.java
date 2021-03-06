package org.d6r;

import java.lang.reflect.Array;


public final class InternalNames {
  
  private InternalNames() {
  }
  
  public static Class<?> getClass(final ClassLoader classLoader,
    final String internalName)
  {
    if (internalName.startsWith("[")) {
      final Class<?> componentClass
        = getClass(classLoader, internalName.substring(1));
      return Array.newInstance(componentClass, 0).getClass();
    }
    if (internalName.equals("Z")) {
      return Boolean.TYPE;
    }
    if (internalName.equals("B")) {
      return Byte.TYPE;
    }
    if (internalName.equals("S")) {
      return Short.TYPE;
    }
    if (internalName.equals("I")) {
      return Integer.TYPE;
    }
    if (internalName.equals("J")) {
      return Long.TYPE;
    }
    if (internalName.equals("F")) {
      return Float.TYPE;
    }
    if (internalName.equals("D")) {
      return Double.TYPE;
    }
    if (internalName.equals("C")) {
      return Character.TYPE;
    }
    if (internalName.equals("V")) {
      return Void.TYPE;
    }
    final String name = internalName.substring(
      1, -1 + internalName.length()
    ).replace('/', '.');
    try {
      return classLoader.loadClass(name);
    } catch (ClassNotFoundException e) {
      final NoClassDefFoundError error = new NoClassDefFoundError(name);
      error.initCause(e);
      throw error;
    } 
  }
  
  public static String getInternalName(final Class<?> c) {
    if (c.isArray()) {
      return '[' + getInternalName(c.getComponentType());
    }
    if (c == Boolean.TYPE) {
      return "Z";
    }
    if (c == Byte.TYPE) {
      return "B";
    }
    if (c == Short.TYPE) {
      return "S";
    }
    if (c == Integer.TYPE) {
      return "I";
    }
    if (c == Long.TYPE) {
      return "J";
    }
    if (c == Float.TYPE) {
      return "F";
    }
    if (c == Double.TYPE) {
      return "D";
    }
    if (c == Character.TYPE) {
      return "C";
    }
    if (c == Void.TYPE) {
      return "V";
    }
    return 'L' + c.getName().replace('.', '/') + ';';
  }
}


