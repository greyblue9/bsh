
package org.d6r;

public class TypeNameUtils {
  
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

