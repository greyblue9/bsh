package bsh.operators.standard;

import java.io.Serializable;

public class FloatMethods implements Serializable {
  public static Float plus(Float left, Float right) {
    return Float.valueOf(left.floatValue() + right.floatValue());
  }

  public static Float minus(Float left, Float right) {
    return Float.valueOf(left.floatValue() - right.floatValue());
  }

  public static Float times(Float left, Float right) {
    return Float.valueOf(left.floatValue() * right.floatValue());
  }

  public static Float divide(Float left, Float right) {
    return Float.valueOf(left.floatValue() / right.floatValue());
  }

  public static Float uminus(Float left) {
    return Float.valueOf(-left.floatValue());
  }

  public static Float power(Float x, Float y) {
    return Float.valueOf((float)Math.pow(x.doubleValue(), y.doubleValue()));
  }

  public static float[] plus(float[] left, float[] right) {
    Object result = null;
    int i;
    float[] var4;
    if(left.length == right.length) {
      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[i];
      }
    } else if(left.length == 1) {
      var4 = new float[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] + right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[0];
      }
    }

    return var4;
  }

  public static float[] plus(float[] left, Float right) {
    return plus(left, new float[]{right.floatValue()});
  }

  public static float[] plus(Float left, float[] right) {
    return plus(new float[]{left.floatValue()}, right);
  }

  public static float[] minus(float[] left, float[] right) {
    Object result = null;
    int i;
    float[] var4;
    if(left.length == right.length) {
      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[i];
      }
    } else if(left.length == 1) {
      var4 = new float[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] - right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[0];
      }
    }

    return var4;
  }

  public static float[] minus(float[] left, Float right) {
    return minus(left, new float[]{right.floatValue()});
  }

  public static float[] minus(Float left, float[] right) {
    return minus(new float[]{left.floatValue()}, right);
  }

  public static float[] times(float[] left, float[] right) {
    Object result = null;
    int i;
    float[] var4;
    if(left.length == right.length) {
      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[i];
      }
    } else if(left.length == 1) {
      var4 = new float[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] * right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[0];
      }
    }

    return var4;
  }

  public static float[] times(float[] left, Float right) {
    return times(left, new float[]{right.floatValue()});
  }

  public static float[] times(Float left, float[] right) {
    return times(new float[]{left.floatValue()}, right);
  }

  public static float[] divide(float[] left, float[] right) {
    Object result = null;
    int i;
    float[] var4;
    if(left.length == right.length) {
      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[i];
      }
    } else if(left.length == 1) {
      var4 = new float[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] / right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new float[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[0];
      }
    }

    return var4;
  }

  public static float[] divide(float[] left, Float right) {
    return divide(left, new float[]{right.floatValue()});
  }

  public static float[] divide(Float left, float[] right) {
    return divide(new float[]{left.floatValue()}, right);
  }

  public static float[] uminus(float[] left) {
    float[] result = new float[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = -left[i];
    }

    return result;
  }

  public static float[] power(float[] left, Float exponent) {
    float[] result = new float[left.length];
    float e = exponent.floatValue();

    for(int i = 0; i < result.length; ++i) {
      result[i] = (float)Math.pow((double)left[i], (double)e);
    }

    return result;
  }

  public static Float cast(Integer left) {
    return Float.valueOf(left.floatValue());
  }

  public static Float cast(float left) {
    return Float.valueOf(left);
  }

  public static Float cast(int left) {
    return Float.valueOf((float)left);
  }

  public static float[] cast(int[] left) {
    float[] result = new float[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = (float)left[i];
    }

    return result;
  }

  public static Object[] cast(float[] left) {
    Float[] result = new Float[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Float.valueOf(left[i]);
    }

    return result;
  }

  public static Float getAt(float[] array, Integer index) {
    return Float.valueOf(array[index.intValue()]);
  }

  public static float[] getAt(float[] array, int[] indices) {
    float[] result = new float[indices.length];

    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      result[i] = array[j];
    }

    return result;
  }

  public static void putAt(float[] array, Integer index, Float value) {
    array[index.intValue()] = value.floatValue();
  }

  public static void putAt(float[] array, int[] indices, float[] values) {
    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      array[j] = values[i];
    }

  }
}
