package java.awt;



class ColorModel {
}

class ColorSpace {
}

class Rectangle {
}

class Rectangle2D {
}

class AffineTransform {
}

class RenderingHints {
}

class PaintContext {  
}


public class Color {
  
  static float[] FLOAT_TRIPLET = new float[3];
  static Color INSTANCE = new Color(0, 0, 0);
  
  
  private static void initIDs() { }
  
  private static void testColorValueRange(int r, int g, int b, int a) 
  { }

  private static void testColorValueRange(float r, float g, float b,
  float a) { }

  public Color(int r, int g, int b) { }

  public Color(int r, int g, int b, int a) { }

  public Color(int rgb) { }

  public Color(int rgba, boolean hasalpha) { }

  public Color(float r, float g, float b) { }

  public Color(float r, float g, float b, float a) { }

  public Color(ColorSpace cspace, float[] components, float alpha) { }

  public int getRed() {
    return 0;
  }

  public int getGreen() {
    return 0;
  }

  public int getBlue() {
    return 0;
  }

  public int getAlpha() {
    return 0;
  }

  public int getRGB() {
    return 0;
  }

  public Color brighter() {
    return INSTANCE;
  }

  public Color darker() {
    return INSTANCE;
  }

  public int hashCode() {
    return 0;
  }

  public boolean equals(Object obj) {
    return obj == this;
  }

  public String toString() {
    return toString();
  }

  public static Color decode(String nm) {
    return INSTANCE;
  }
 
  public static Color getColor(String nm) {
    return INSTANCE;
  }

  public static Color getColor(String nm, Color v) {
    return INSTANCE;
  }

  public static Color getColor(String nm, int v) {
    return INSTANCE;
  }

  public static int HSBtoRGB(float hue, float saturation,
  float brightness) {
    return 0;
  }

  public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals)
  {
    return FLOAT_TRIPLET;
  }

  public static Color getHSBColor(float h, float s, float b) {
    return INSTANCE;
  }

  public float[] getRGBComponents(float[] compArray) {
    return FLOAT_TRIPLET;
  }

  public float[] getRGBColorComponents(float[] compArray) {
    return FLOAT_TRIPLET;
  }

  public float[] getComponents(float[] compArray) {
    return FLOAT_TRIPLET;
  }

  public float[] getColorComponents(float[] compArray) {
    return FLOAT_TRIPLET;
  }

  public float[] getComponents(ColorSpace cspace, float[] compArray) 
  {
    return FLOAT_TRIPLET;
  }

  public float[] getColorComponents(ColorSpace cspace,
  float[] compArray) {
    return FLOAT_TRIPLET;
  }

  public ColorSpace getColorSpace() {
    return new ColorSpace();
  }

  public synchronized PaintContext createContext(ColorModel cm,
  Rectangle r, Rectangle2D r2d, AffineTransform xform, 
  RenderingHints hints) {
    return new PaintContext();
  }

  public int getTransparency() {
    return 0;
  }
  
}

