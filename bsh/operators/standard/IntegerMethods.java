package bsh.operators.standard;

import java.io.Serializable;

public class IntegerMethods implements Serializable {
  public static Integer plus(Integer left, Integer right) {
    return Integer.valueOf(left.intValue() + right.intValue());
  }

  public static Integer minus(Integer left, Integer right) {
    return Integer.valueOf(left.intValue() - right.intValue());
  }

  public static Integer times(Integer left, Integer right) {
    return Integer.valueOf(left.intValue() * right.intValue());
  }

  public static Integer divide(Integer left, Integer right) {
    return Integer.valueOf(left.intValue() / right.intValue());
  }

  public static Integer uminus(Integer left) {
    return Integer.valueOf(-left.intValue());
  }

  public static int[] plus(int[] left, int[] right) {
    Object result = null;
    int i;
    int[] var4;
    if(left.length == right.length) {
      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[i];
      }
    } else if(left.length == 1) {
      var4 = new int[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] + right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[0];
      }
    }

    return var4;
  }

  public static int[] plus(int[] left, Integer right) {
    return plus(left, new int[]{right.intValue()});
  }

  public static int[] plus(Integer left, int[] right) {
    return plus(new int[]{left.intValue()}, right);
  }

  public static int[] minus(int[] left, int[] right) {
    Object result = null;
    int i;
    int[] var4;
    if(left.length == right.length) {
      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[i];
      }
    } else if(left.length == 1) {
      var4 = new int[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] - right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[0];
      }
    }

    return var4;
  }

  public static int[] minus(int[] left, Integer right) {
    return minus(left, new int[]{right.intValue()});
  }

  public static int[] minus(Integer left, int[] right) {
    return minus(new int[]{left.intValue()}, right);
  }

  public static int[] times(int[] left, int[] right) {
    Object result = null;
    int i;
    int[] var4;
    if(left.length == right.length) {
      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[i];
      }
    } else if(left.length == 1) {
      var4 = new int[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] * right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[0];
      }
    }

    return var4;
  }

  public static int[] times(int[] left, Integer right) {
    return times(left, new int[]{right.intValue()});
  }

  public static int[] times(Integer left, int[] right) {
    return times(new int[]{left.intValue()}, right);
  }

  public static int[] divide(int[] left, int[] right) {
    Object result = null;
    int i;
    int[] var4;
    if(left.length == right.length) {
      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[i];
      }
    } else if(left.length == 1) {
      var4 = new int[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] / right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new int[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[0];
      }
    }

    return var4;
  }

  public static int[] divide(int[] left, Integer right) {
    return divide(left, new int[]{right.intValue()});
  }

  public static int[] divide(Integer left, int[] right) {
    return divide(new int[]{left.intValue()}, right);
  }

  public static int[] uminus(int[] left) {
    int[] result = new int[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = -left[i];
    }

    return result;
  }

  public static int[] range(Integer min, Integer max) {
    return range(min, max, Integer.valueOf(1));
  }

  public static int[] range(Integer min, Integer max, Integer inc) {
    int range = max.intValue() - min.intValue();
    int count = range / inc.intValue() + 1;
    int[] result = new int[count];

    for(int i = 0; i < count; ++i) {
      result[i] = min.intValue() + inc.intValue() * i;
    }

    return result;
  }

  public static Integer cast(int left) {
    return Integer.valueOf(left);
  }

  public static Object[] cast(int[] left) {
    Integer[] result = new Integer[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Integer.valueOf(left[i]);
    }

    return result;
  }

  public static Integer getAt(int[] array, Integer index) {
    return Integer.valueOf(array[index.intValue()]);
  }

  public static int[] getAt(int[] array, int[] indices) {
    int[] result = new int[indices.length];

    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      result[i] = array[j];
    }

    return result;
  }

  public static void putAt(int[] array, Integer index, Integer value) {
    array[index.intValue()] = value.intValue();
  }

  public static void putAt(int[] array, int[] indices, int[] values) {
    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      array[j] = values[i];
    }

  }
}
