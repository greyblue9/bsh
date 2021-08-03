package org.d6r;

import java.util.Map;
import java.util.HashMap;

public class DexlibAdapter {
  
  
  private static final String INT = "int";
  private static final String VOID = "void";
  private static final String CHAR = "char";
  private static final String DOUBLE = "double";
  private static final String FLOAT = "float";
  private static final String LONG = "long";
  private static final String SHORT = "short";
  private static final String BOOLEAN = "boolean";
  private static final String BYTE = "byte";
  private static final String OBJECT = "java.lang.Object";
  
  public static Map<Character, String> PrimitiveTypes
      = new HashMap<Character, String>();
  static {
      PrimitiveTypes.put('I', INT);
      PrimitiveTypes.put('V', VOID);
      PrimitiveTypes.put('C', CHAR);
      PrimitiveTypes.put('D', DOUBLE);
      PrimitiveTypes.put('F', FLOAT);
      PrimitiveTypes.put('J', LONG);
      PrimitiveTypes.put('S', SHORT);
      PrimitiveTypes.put('Z', BOOLEAN);
      PrimitiveTypes.put('B', BYTE);
      PrimitiveTypes.put('L', OBJECT);
  }
  
  public static final String getName(char shorty) {
    switch (shorty) {
      case 'I': return INT;
      case 'V': return VOID;
      case 'C': return CHAR;
      case 'D': return DOUBLE;
      case 'F': return FLOAT;
      case 'J': return LONG;
      case 'S': return SHORT;
      case 'Z': return BOOLEAN;
      case 'B': return BYTE;
      case 'L': return OBJECT;
      default: 
        throw new IllegalArgumentException(String.format(
          "Shorty type not recognized: '%c' (0x%2x)", 
          shorty, (short) shorty
        ));
    }
  }
}