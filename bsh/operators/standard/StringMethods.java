package bsh.operators.standard;

import java.io.Serializable;

public class StringMethods implements Serializable {
  public static String plus(String left, String right) {
    return left + right;
  }

  public static String[] plus(String[] left, String[] right) {
    String[] result = null;
    int i;
    if(left.length == right.length) {
      result = new String[left.length];

      for(i = 0; i < result.length; ++i) {
        result[i] = left[i] + right[i];
      }
    } else if(left.length == 1) {
      result = new String[right.length];

      for(i = 0; i < result.length; ++i) {
        result[i] = left[0] + right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      result = new String[left.length];

      for(i = 0; i < result.length; ++i) {
        result[i] = left[i] + right[0];
      }
    }

    return result;
  }

  public static String[] plus(String[] left, String right) {
    String[] result = new String[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = left[i] + right;
    }

    return result;
  }

  public static String[] plus(String left, String[] right) {
    String[] result = new String[right.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = left + right[i];
    }

    return result;
  }

  public static char[] range(Character min, Character max) {
    int count = max.charValue() - min.charValue();
    char[] result = new char[count];

    for(int i = 0; i < count; ++i) {
      result[i] = (char)(min.charValue() + i);
    }

    return result;
  }
}
