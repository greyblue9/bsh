package bsh.operators.standard;

import java.io.Serializable;

public class DoubleMethods implements Serializable {
  public static Double plus(Double left, Double right) {
    return Double.valueOf(left.doubleValue() + right.doubleValue());
  }

  public static Double minus(Double left, Double right) {
    return Double.valueOf(left.doubleValue() - right.doubleValue());
  }

  public static Double times(Double left, Double right) {
    return Double.valueOf(left.doubleValue() * right.doubleValue());
  }

  public static Double divide(Double left, Double right) {
    return Double.valueOf(left.doubleValue() / right.doubleValue());
  }

  public static Double uminus(Double left) {
    return Double.valueOf(-left.doubleValue());
  }

  public static Double power(Double x, Double y) {
    return Double.valueOf(Math.pow(x.doubleValue(), y.doubleValue()));
  }

  public static double[] plus(double[] left, double[] right) {
    Object result = null;
    int i;
    double[] var4;
    if(left.length == right.length) {
      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[i];
      }
    } else if(left.length == 1) {
      var4 = new double[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] + right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] + right[0];
      }
    }

    return var4;
  }

  public static double[] plus(double[] left, Double right) {
    return plus(left, new double[]{right.doubleValue()});
  }

  public static double[] plus(Double left, double[] right) {
    return plus(new double[]{left.doubleValue()}, right);
  }

  public static double[] minus(double[] left, double[] right) {
    Object result = null;
    int i;
    double[] var4;
    if(left.length == right.length) {
      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[i];
      }
    } else if(left.length == 1) {
      var4 = new double[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] - right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] - right[0];
      }
    }

    return var4;
  }

  public static double[] minus(double[] left, Double right) {
    return minus(left, new double[]{right.doubleValue()});
  }

  public static double[] minus(Double left, double[] right) {
    return minus(new double[]{left.doubleValue()}, right);
  }

  public static double[] times(double[] left, double[] right) {
    Object result = null;
    int i;
    double[] var4;
    if(left.length == right.length) {
      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[i];
      }
    } else if(left.length == 1) {
      var4 = new double[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] * right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] * right[0];
      }
    }

    return var4;
  }

  public static double[] times(double[] left, Double right) {
    return times(left, new double[]{right.doubleValue()});
  }

  public static double[] times(Double left, double[] right) {
    return times(new double[]{left.doubleValue()}, right);
  }

  public static double[] divide(double[] left, double[] right) {
    Object result = null;
    int i;
    double[] var4;
    if(left.length == right.length) {
      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[i];
      }
    } else if(left.length == 1) {
      var4 = new double[right.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[0] / right[i];
      }
    } else {
      if(right.length != 1) {
        throw new IllegalArgumentException("Array length mismatch");
      }

      var4 = new double[left.length];

      for(i = 0; i < var4.length; ++i) {
        var4[i] = left[i] / right[0];
      }
    }

    return var4;
  }

  public static double[] divide(double[] left, Double right) {
    return divide(left, new double[]{right.doubleValue()});
  }

  public static double[] divide(Double left, double[] right) {
    return divide(new double[]{left.doubleValue()}, right);
  }

  public static double[] uminus(double[] left) {
    double[] result = new double[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = -left[i];
    }

    return result;
  }

  public static double[] power(double[] left, Double exponent) {
    double[] result = new double[left.length];
    double e = exponent.doubleValue();

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.pow(left[i], e);
    }

    return result;
  }

  public static double[] range(Double min, Double max) {
    return range(min, max, Double.valueOf(1.0D));
  }

  public static double[] range(Double min, Double max, Double inc) {
    double range = max.doubleValue() - min.doubleValue();
    int count = (int)(range / inc.doubleValue()) + 1;
    double[] result = new double[count];

    for(int i = 0; i < count; ++i) {
      result[i] = min.doubleValue() + inc.doubleValue() * (double)i;
    }

    return result;
  }

  public static Double cast(Float left) {
    return Double.valueOf(left.doubleValue());
  }

  public static Double cast(Integer left) {
    return Double.valueOf(left.doubleValue());
  }

  public static Double cast(double left) {
    return Double.valueOf(left);
  }

  public static Double cast(int left) {
    return Double.valueOf((double)left);
  }

  public static Double cast(float left) {
    return Double.valueOf((double)left);
  }

  public static double[] cast(float[] left) {
    double[] result = new double[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = (double)left[i];
    }

    return result;
  }

  public static double[] cast(int[] left) {
    double[] result = new double[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = (double)left[i];
    }

    return result;
  }

  public static Object[] cast(double[] left) {
    Double[] result = new Double[left.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Double.valueOf(left[i]);
    }

    return result;
  }

  public static Double getAt(double[] array, Integer index) {
    return Double.valueOf(array[index.intValue()]);
  }

  public static double[] getAt(double[] array, int[] indices) {
    double[] result = new double[indices.length];

    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      result[i] = array[j];
    }

    return result;
  }

  public static void putAt(double[] array, Integer index, Double value) {
    array[index.intValue()] = value.doubleValue();
  }

  public static void putAt(double[] array, int[] indices, double[] values) {
    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      array[j] = values[i];
    }

  }
}
