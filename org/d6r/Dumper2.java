
//package bsh;

import java.lang.reflect.Array;
import java.lang.reflect.*;
import java.util.*;



public class Dumper2 {
  
  //public static Dumper instance; 
  public static int maxDepth = 0;
  public static int maxLen = 0;
  public static int callCount = 0;
  public static HashMap<String, String> ignoreList;
  // = new HashMap<String, String>();
  
  public static HashMap<Object, Integer> visited;
  // = new HashMap<? extends Object, Integer>();
        
  private static Method fnFormat = null;
  
  public static void clearContext() {
    maxDepth = 0;
    maxLen = 0;
    callCount = 0;
    ignoreList = new HashMap<String, String> ();
    visited = new HashMap<Object, Integer>();
  }
  
             
  
  public static String dumpStr(Object o) 
  {
    return dumpStr(o, 3, 25, null);
  }
  
  public static String dumpStr(Object o, int _xDepth) 
  {
    return dumpStr(o, _xDepth, 25, null);
  }
  
  public static String dumpStr(Object o, int _xDepth, int _xLen)
  {
    return dumpStr(o, _xDepth, _xLen, null);
  }
  
  
  public static void dump(Object o) 
  {
    System.out.print(dumpStr(o, 3, 25, null));
  }
  
  public static void dump(Object o, int _xDepth) 
  {
    System.out.print(dumpStr(o, _xDepth, 25, null));
  }
  
  public static void dump(Object o, int _xDepth, int _xLen)
  {
    System.out.print(dumpStr(o, _xDepth, _xLen, null));
  }
  
  public static void dump(Object o, int _xDepth, int _xLen,
  String[] _ignoreList) 
  {
    System.out.print(dumpStr(o, _xDepth, _xLen, _ignoreList));
  }
  

  
  public static String dumpStr
  (Object o, int _xDepth, int _xLen, String[] _ignoreList) 
  {
    clearContext();
    maxDepth = _xDepth;
    maxLen = _xLen;

    if (_ignoreList != null) {
      for (int i = 0; i < Array.getLength(_ignoreList); i++) {
        int colonIdx = _ignoreList[i].indexOf(':');
        if (colonIdx == -1) {
          _ignoreList[i ] = _ignoreList[i] + ":";
        }
        ignoreList.put(_ignoreList[i], _ignoreList[i]);
      }
    }
    
    return _dump(o);
  }

  public static String _dump(Object o) {
    if (o == null) {
      return "<null>";
    }
    
    callCount++;
    StringBuffer tabs = new StringBuffer(8);
    for (int k = 0; k < callCount; k++) {
      tabs.append("  ");
    }
    String _tabs = tabs.toString();
    String _tabsL = _tabs; //.substring(1);
    
    StringBuffer buffer = new StringBuffer();
    Class oClass = o.getClass();
    
    String toStr = "";
    if (o != null) { 
      try {
        toStr = o.toString();
      } catch (Throwable tse) {
        toStr = "(toString() threw "
        + tse.getClass().getSimpleName()+")";
      }
    } //else toStr = "<null>";
    
  
    String oSimpleName 
 = getSimpleNameWithoutArrayQualifier(oClass);

    if (ignoreList.get(oSimpleName + ":") != null)
      return "<Ignored>";

    if (oClass.isArray()) {
      appendFormat(buffer, 
        "\n%s%s:\n%s[\n",
        _tabsL, toStr, _tabsL
      );
      //buffer.append("\n");
      //buffer.append(_tabsL);
      //buffer.append(toStr+":\n");
      //buffer.append(_tabsL);
      //buffer.append("[\n");
      int rowCount = maxLen == 0 ? Array.getLength(o) : Math.min(maxLen, Array.getLength(o));
      for (int i = 0; i < rowCount; i++) {
        buffer.append(_tabs);
        try {
          Object value = Array.get(o, i);
          buffer.append(dumpValue(value));
        } catch (Exception e) {
          buffer.append(e.getMessage());
        }
        if (i < Array.getLength(o) - 1)
          buffer.append(",");
        buffer.append("\n");
      }
      
      if (rowCount < Array.getLength(o)) {
        buffer.append(_tabs);
        appendFormat(buffer, 
          "%d more array elements...\n",
          Array.getLength(o) - rowCount    
        );
        buffer.append("\n");
      }
      
      buffer.append(_tabsL);
      buffer.append("]");
    } else {
      buffer.append("\n");
      buffer.append(_tabsL);
      buffer.append(toStr);
      buffer.append(":\n");
      buffer.append(_tabsL);
      buffer.append("{\n");
      buffer.append(_tabs);
      appendFormat(buffer, "hashCode: %x\n", o.hashCode());
      //buffer.append("\n");
      while (oClass != null && oClass != Object.class) {
        Field[] fields = oClass.getDeclaredFields();

        if (ignoreList.get(oClass.getSimpleName()) == null) {
          if (oClass != o.getClass()) {
            buffer.append(_tabsL);
            buffer.append("  Inherited from superclass " + oSimpleName + ":\n");
          }

          for (int i = 0; i < fields.length; i++) {

            String fSimpleName = getSimpleNameWithoutArrayQualifier(fields[i].getType());
            String fName = fields[i].getName();
            //if (fName.charAt(0) != 'm') continue;
            
            fields[i].setAccessible(true);
            buffer.append(_tabs);
            appendFormat(
              buffer,
              "%-10s %14s = ",
              "(" + fSimpleName + ")", fName
            );
            //buffer.append(" = ");

            if (ignoreList.get(":" + fName )== null &&
            ignoreList.get(fSimpleName + ":" + fName )== null &&
            ignoreList.get(fSimpleName + ":") == null) {

              try {
                Object value = fields[i].get(o);
                buffer.append(dumpValue(value));
              } catch (Exception e) {
                buffer.append(e.getMessage());
              }
              buffer.append("\n");
            }
            else {
            buffer.append("<Ignored>");
            buffer.append("\n");
            }
          }
          oClass = oClass.getSuperclass();
          oSimpleName = oClass.getSimpleName();
        }
        else {
          oClass = null;
          oSimpleName = "";
        }
      }
      buffer.append(_tabsL);
      buffer.append("}");
    }
    callCount--;
    return buffer.toString();
  }

  public static String dumpValue(Object value) {
    if (value == null) {
      return "<null>";
    }
    if (value.getClass().isPrimitive() ||
      value.getClass() == java.lang.Short.class ||
      value.getClass() == java.lang.Long.class ||
      value.getClass() == java.lang.String.class ||
      value.getClass() == java.lang.Integer.class ||
      value.getClass() == java.lang.Float.class ||
      value.getClass() == java.lang.Byte.class ||
      value.getClass() == java.lang.Character.class ||
      value.getClass() == java.lang.Double.class ||
      value.getClass() == java.lang.Boolean.class ||
      value.getClass() == java.util.Date.class ||
      value.getClass().isEnum()) {

      return value.toString();

    } else {

      //Integer visitedIndex = visited.get(value);
      //if (visitedIndex == null) {
      if (true) {
        //visited.put(value, callCount);
        if (maxDepth == 0 || callCount < maxDepth) {
          return _dump(value);
        }
        else {
          return ". . ."; //<Reached max recursion depth>";
        }
      }
      else {
        return "<Previously visited - see hashCode " + value.hashCode() + ">";
      }
    }
  }
  
  static void appendFormat(StringBuffer sb, String fmt,
  Object arg1) {
    sb.append(String.format(fmt, arg1));
  }
  static void appendFormat(StringBuffer sb, String fmt,
  Object arg1, Object arg2) {
    sb.append(String.format(fmt, arg1, arg2));
  }
  static void appendFormat(StringBuffer sb, String fmt,
  Object arg1, Object arg2, Object arg3) {
    sb.append(String.format(fmt, arg1, arg2, arg3));
  }
  static void appendFormat(StringBuffer sb, String fmt,
  Object arg1, Object arg2, Object arg3, Object arg4) {
    sb.append(String.format(fmt, arg1, arg2, arg3, arg4));
  }
  static void appendFormat(StringBuffer sb, String fmt,
  Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
  {
    sb.append(String.format(fmt, arg1, arg2, arg3, arg4, arg5));
  }
  
  static String getSimpleNameWithoutArrayQualifier
  (Class clazz) {
    String simpleName = clazz.getSimpleName();
    int indexOfBracket = simpleName.indexOf('['); 
    if (indexOfBracket != -1)
      return simpleName.substring(0, indexOfBracket);
    return simpleName;
  }
}