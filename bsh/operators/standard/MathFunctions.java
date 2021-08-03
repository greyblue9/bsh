package bsh.operators.standard;

import bsh.operators.Extension;
import java.io.Serializable;

public class MathFunctions implements Serializable {
  @Extension
  public static Double sin(Double value) {
    return Double.valueOf(Math.sin(value.doubleValue()));
  }

  @Extension
  public static Double sind(Double value) {
    return Double.valueOf(Math.sin(Math.toRadians(value.doubleValue())));
  }

  @Extension
  public static Double cos(Double value) {
    return Double.valueOf(Math.cos(value.doubleValue()));
  }

  @Extension
  public static Double cosd(Double value) {
    return Double.valueOf(Math.cos(Math.toRadians(value.doubleValue())));
  }

  @Extension
  public static Double tan(Double value) {
    return Double.valueOf(Math.tan(value.doubleValue()));
  }

  @Extension
  public static Double tand(Double value) {
    return Double.valueOf(Math.tan(Math.toRadians(value.doubleValue())));
  }

  @Extension
  public static Double toDegrees(Double value) {
    return Double.valueOf(Math.toDegrees(value.doubleValue()));
  }

  @Extension
  public static Double toRadians(Double value) {
    return Double.valueOf(Math.toRadians(value.doubleValue()));
  }

  @Extension
  public static double[] sin(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.sin(value[i]);
    }

    return result;
  }

  @Extension
  public static double[] sind(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.sin(Math.toRadians(value[i]));
    }

    return result;
  }

  @Extension
  public static double[] cos(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.cos(value[i]);
    }

    return result;
  }

  @Extension
  public static double[] cosd(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.cos(Math.toRadians(value[i]));
    }

    return result;
  }

  @Extension
  public static double[] tan(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.tan(value[i]);
    }

    return result;
  }

  @Extension
  public static double[] tand(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.tan(Math.toRadians(value[i]));
    }

    return result;
  }

  @Extension
  public static double[] toDegrees(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.toDegrees(value[i]);
    }

    return result;
  }

  @Extension
  public static double[] toRadians(double[] value) {
    double[] result = new double[value.length];

    for(int i = 0; i < result.length; ++i) {
      result[i] = Math.toRadians(value[i]);
    }

    return result;
  }
}
