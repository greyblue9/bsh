package org.d6r;


//package bsh;


import bsh.*;

import java.lang.reflect.*;
import java.util.*;
import libcore.reflect.*;
import org.d6r.ReflectionUtil;
import org.apache.commons.lang3.reflect.TypeUtils;
import java.util.regex.*;
import bsh.operators.Extension;

//import org.apache.commons.lang3.ClassUtils;


@SuppressWarnings({"rawtypes"})
public class Dumper {
  
  //public static Dumper instance; 
  public static int maxDepth = 0;
  public static int maxLen = 0;
  public static int callCount = 0;
  public static HashMap<String, String> ignoreList;
  public static Pattern[] ignoreRegexes;
  //        = new HashMap<String, String>();
  
  public static HashMap<Object, Integer> visited;
  //        = new HashMap<? extends Object, Integer>();
  
  private static Method fnFormat = null;
  
  public static 
            Map<Class<?>, Object> typeMap
  = new HashMap<Class<?>, Object>();
  
  public static boolean verbose = true;
  public static List<Throwable> exceptions;
  
  private static Method getClassSignatureAnnotation_Method = null;
  private static Method getClassName_Method = null;
  public static boolean excludeStatic = true; 

  
  public static void clearContext() {
    maxDepth = 0;
    maxLen = 0;
    callCount = 0;
    ignoreList = new HashMap<String, String> ();
    visited    = new HashMap<Object, Integer>();
    ignoreRegexes = new Pattern[] {
      Pattern.compile(
        "beginColumn|beginLine|endColumn|endLine|sourceFile"
      )
    };
  }
  
  public static Class<Dumper> _getClass() {
    return Dumper.class;
  }

  @Extension
  public static String dumpStr(Object o) 
  {
    return dumpStr(o, 3);
  }
  
  @Extension
  public static String dumpStr(Object o, int _xDepth) 
  {
    return dumpStr(o, _xDepth, 25);
  }
  
  @Extension
  public static String dumpStr(Object o, int _xDepth, int _xLen)
  {
    return dumpStr(o, _xDepth, _xLen, (String[]) null);
  }
  
  @Extension
  public static void dump(Object o) 
  {
    System.out.print(dumpStr(o));
  }
  
  public static void dump(Object o, int _xDepth) 
  {
    System.out.print(dumpStr(o, _xDepth));
  }
  
  public static void dump(Object o, int _xDepth, int _xLen)
  {
    System.out.print(dumpStr(o, _xDepth, _xLen));
  }
  
  public static void dump(Object o, int _xDepth, int _xLen,
  String[] _ignoreList) 
  {
    System.out.print(dumpStr(o, _xDepth, _xLen, _ignoreList));
  }
  
  public static void dump(Object o, int _xDepth, int _xLen,
  Pattern[] _ignoreRegexes) 
  {
    System.out.print(dumpStr(o, _xDepth, _xLen, _ignoreRegexes));
  }
  
  @Extension
  public static void dump(boolean noStatic, Object... params) 
  {
    boolean oldExcludeStatic = excludeStatic;
    excludeStatic = noStatic;
    try { 
      dumpInvoke("dump", params);
    } finally {
      excludeStatic = oldExcludeStatic;
    }
  }
  
  @Extension
  public static String dumpStr(boolean noStatic, Object... params) 
  {
    boolean oldExcludeStatic = excludeStatic;
    excludeStatic = noStatic;
    try { 
      return (String) dumpInvoke("dumpStr", params);
    } finally {
      excludeStatic = oldExcludeStatic;
    }
  }
  
  public static Object dumpInvoke
  (String name, Object... params) 
  {
    Class<?>[] types = new Class<?>[params.length];
    int idx = 0;
    for (Object param: params) {
      types[idx++] = (param == null || idx-1 == 0)
        ? Object.class
        : param.getClass();
    }
    try {
      Method m = Dumper.class.getDeclaredMethod(name, types);
      return m.invoke(null, params);
    } catch (Throwable e) { 
      System.err.printf(
        "Trouble invoking %s.%s(%s): %s: %s\n  %s\n\n",
        _getClass().getName(),
        name,
        Arrays.toString(types).replaceAll("^\\[(.*)\\]$", "$1"),
        e.getClass().getName(),
        e.getMessage(),
        Arrays.toString(e.getStackTrace())
      );
    }
    return Void.TYPE;
  }
  
  
  public static String _toString(Object o) {
    try { 
      return o != null? o.toString(): "null";
    } catch (Exception e) {
      return "{"+e.getClass().getName()+"}";
    }
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
          _ignoreList[i] = _ignoreList[i] + ":";
        }
        ignoreList.put(_ignoreList[i], _ignoreList[i]);
      }
    }
    
    return _dump(o);
  }
  
  public static String dumpStr
  (Object o, int _xDepth, int _xLen, Pattern[] _ignoreRegexes) 
  {
    clearContext();
    maxDepth = _xDepth;
    maxLen = _xLen;

    if (_ignoreRegexes != null) {
      ignoreRegexes = _ignoreRegexes;
    }
    return _dump(o);
  }
  
  
  public static int tryHashCode(final Object o) {
    if (o == null) return 0;
    try {
      return o.hashCode();
    } catch (Throwable e) {
      StackTraceElement ste 
        = Reflector.getRootCause(e).getStackTrace()[0];
      String className = ste.getClassName();
      String methodName = ste.getMethodName();
      int lineno = ste.getLineNumber();
      String sourceFile = ste.getFileName();
      System.err.printf(
        "[WARN] %s.hashCode(): %s threw %s in method `%s', "
        + "at line %d of %s: %s (returning hashCode -> 0)\n",
        o.getClass().getName(), className,
        e.getClass().getSimpleName(), methodName,
        lineno, sourceFile, e.getMessage()
      );
      return 0;
    }
  }

  public static String tryToString(final Object o) {
    if (o == null) return null;
    String toStr = "???";

    try {
      
      toStr = o.toString();
    } catch (Throwable tse) {
      
      if (tse.getCause() != null
      && tse.getMessage() != null) 
      {
        toStr = String.format(
          "<![ toString() threw %s: Caused by %s: '%s' ]!>",
          tse.getClass().getSimpleName(),
          tse.getCause().getClass().getSimpleName(),
          tse.getCause().getMessage()
        );
      } else if (tse.getMessage() != null) {
        toStr = String.format(
          "<![ toString() threw %s: '%s' ]!>",
          tse.getClass().getSimpleName(),
          tse.getMessage()
        );
      } else { // no message
        toStr = String.format(
          "<![ toString() threw %s: [null message] ]!>",
          tse.getClass().getSimpleName()
        );
      }
      // end toStr = [Ex handling]
    }// end catch (Throwable tse) {...}
    return o == null
      ? "<null>"
      : String.format(
        "%s  (%s)", toStr, o.getClass().getSimpleName()
      );
  }

  public static String _dump(Object o) {
    if (o == null) {
      return "<null>";
    }
    
    callCount++;
    if (o instanceof String) {
      return String.format(
        "%s  (%s)",
        ((String)o).length() > 256
          ? ((String)o).substring(0, 252).concat("...")
          : ((String)o),
        o.getClass().getSimpleName()
      );
    }    
    StringBuilder tabs = new StringBuilder(8);
    for (int k = 0; k < callCount; k++) {
      tabs.append("  ");
    }
    String _tabs = tabs.toString();
    String _tabsL = _tabs; //.substring(1);
    
    StringBuilder buffer = new StringBuilder();
    Class oClass = o.getClass();
    //Type[] types = o.getTypeParameters();
    
    String toStr = tryToString(o);
    
  
    String oSimpleName 
      = getSimpleNameWithoutArrayQualifier(oClass);
    
    if (ignoreList.get(oSimpleName + ":") != null)
      return "<Ignored>";

    if (oClass.isArray()) {
      appendFormat(buffer, 
        "\n%s%s:\n%s[\n",
        new Object[]{ 
          _tabsL, toStr, _tabsL
        }
      );
      //buffer.append("\n");
      //buffer.append(_tabsL);
      //buffer.append(toStr+":\n");
      //buffer.append(_tabsL);
      //buffer.append("[\n");
      
      int rowCount = maxLen == 0 
        ? Array.getLength(o) 
        : Math.min(maxLen, Array.getLength(o));
      
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
          new Object[]{ 
            Integer.valueOf(Array.getLength(o) - rowCount)    
          }
        );
        buffer.append("\n");
      }
      
      buffer.append(_tabsL);
      buffer.append("]");
      
    } else {
      appendFormat(
        buffer,
        "\n%s%s:\n%s{\n%shashCode: %x\n",
        new Object[]{ 
          _tabsL, toStr, _tabsL, _tabs, 
          Integer.valueOf(o.hashCode())
        }
      );
      
      while (oClass != null && oClass != Object.class) {
        Field[] fields = oClass.getDeclaredFields();
        
        if (ignoreList.get(oClass.getSimpleName()) == null) {
          if (oClass != o.getClass()) {
            buffer.append(_tabsL);
            buffer.append("// As " + oSimpleName + ":\n");
          }

          fieldLoop:
          for (int i = 0; i < fields.length; i++) {
            if (excludeStatic 
            &&  Modifier.isStatic(fields[i].getModifiers())) {
              continue; 
            } 
            //String fSimpleName 
            // = getSimpleNameWithoutArrayQualifier(
            //     = fields[i].getGetGenericType().toString();
                 
               
            String fSimpleName 
              = colorize(
                  typeToString(
                    fields[i].getGenericType()
                     
                  ) .replace("java.lang.",""),
                  "1;36"
                );
                   
            String fName = fields[i].getName();
            Class<?> fClass = fields[i].getDeclaringClass();
            
            if(ignoreList.get(":" + fName) != null 
            || ignoreList.get(fSimpleName + ":" + fName) 
               != null
            || ignoreList.get(fSimpleName + ":") != null) 
            {
               continue; 
            }
            
            for (Pattern ptrn: ignoreRegexes) {
              if (ptrn.matcher(fName).matches()) {
                 continue fieldLoop;
              }
              if (ptrn.matcher(fClass.getName()).matches()) {
                 continue fieldLoop;
              }
            }
            
            fields[i].setAccessible(true);
            buffer.append(_tabs);
            appendFormat(
              buffer,
              "%-10s %14s = ",
              new Object[]{ 
                "(" + fSimpleName + ")", fName
              }
            );
            //buffer.append(" = ");

            try {
              Object value = fields[i].get(o);
              String dumped = dumpValue(value);
              buffer.append( 
                (dumped.length() > 64480)
                  ? dumped.substring(0, 64480)
                  : dumped 
              );
            } catch (Throwable e) {
              buffer.append(e.getMessage());
            }
            buffer.append("\n");
            
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
    
    Class<?> vClass = value.getClass();
    
    if (
      vClass.isPrimitive() ||
      vClass == java.lang.Short.class ||
      vClass == java.lang.Long.class ||
      vClass == java.lang.String.class ||
      vClass == java.lang.Integer.class ||
      vClass == java.lang.Float.class ||
      vClass == java.lang.Byte.class ||
      vClass == java.lang.Character.class ||
      vClass == java.lang.Double.class ||
      vClass == java.lang.Boolean.class ||
      vClass == java.util.Date.class ||
      vClass.isEnum()) {

      return String.format("%s  (%s)", value.toString(),
        value.getClass().getSimpleName());

    } else {

      Integer visitedIndex = visited.get(value);
      if (visitedIndex == null) {
      //if (true) {
        visited.put(value, callCount);
        if (value instanceof AbstractCollection ||
            value instanceof Map) 
        {
          return _dump(value); 
        } else if (maxDepth == 0 || callCount < maxDepth) {
          return _dump(value);
        } else {
          
          return String.format(
            "<(%s) %s @ depth gte %d . . .>",
            (value != null
              ? value.getClass().getSimpleName() 
              : "<null>"),
            (value != null
              ? tryToString(value)
              : "<null>"),
            callCount + 1
          ); //<Reached max recursion depth>";
        }
      } else {
        return String.format("<%s@%x>", 
          value.getClass().getSimpleName(), value.hashCode());
      }
    }
  }
  
  
  public static StringBuilder appendFormat(StringBuilder sb,
  String fmt, Object... args)
  {
    try {
      if (fnFormat == null) {
        fnFormat = String.class.getDeclaredMethod(
          "format", 
          new Class[]{ String.class, Object[].class }
        );
      }
      String fmtStr = (String) fnFormat.invoke(null, 
        new Object[]{ fmt, args });
      sb.append(fmtStr);
    } catch (Exception e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
    }
    return sb;
  }
  
  static String getSimpleNameWithoutArrayQualifier
  (Class clazz) {
    String simpleName = clazz.getSimpleName();
    int indexOfBracket = simpleName.indexOf('['); 
    if (indexOfBracket != -1)
      return simpleName.substring(0, indexOfBracket);
    return simpleName;
  }
  
  
  
    public static String colorize(String fmt, String colorspec, boolean useColor) {
      if (! useColor) return fmt;
      char esc = (char)0x1b;
      
      return String.format(
        "%c[%sm%s%c[%sm", 
        (char)0x1b, colorspec, 
        fmt, 
        (char)0x1b, "0"
      )         .replace("<", 
                  String.format("%c[0;31m<%c[1;31m",esc,esc))
                .replace(">", 
                  String.format("%c[0;31m>%c[1;31m",esc,esc))
                .replaceAll(
                  String.format(">%c%c[1;31m([ ])",esc,0x5c), 
                  String.format(">%c[0m$0",esc))
                .replaceAll("$", 
                  String.format("%c[0m",esc))
                .replace("[]", 
                  String.format("%c[1;37m[]%c[0m",esc,esc));
    }
    
    public static String colorize(String fmt, String colorspec) {
      return colorize(fmt, colorspec, true);
    }
    
    
    
    public static StringBuilder appendColor(StringBuilder sb, String fmt, String colorspec, Object[] args) {
      return sb.append(String.format(
        colorize(fmt, colorspec, true),
        args
      ));
    }
    
    
    
    
    public static String typeToString(Type type) {
      
      if (getClassName_Method == null) {
        try {
          getClassName_Method
            = ReflectionUtil.class.getDeclaredMethod(
            "getClassName",
            new Class<?>[]{ Type.class }
          );
        } catch (Exception e) {
          return type.toString();
        }
      }
      
      String typeStr
        = (type != null)
            ? ReflectionUtil.getClassName(type)
            : "";
           
      while (typeStr.length() > 0 && typeStr.charAt(0) == '[') {
        typeStr = typeStr.substring(1, typeStr.length()) + "[]";
    
      }  
      if (typeStr.length() > 0 && typeStr.charAt(0) == 'L' && typeStr.indexOf(';') != -1) {
        typeStr = typeStr.substring(1);
        typeStr = typeStr.replace(";", "");
      }
      return typeStr.replace(String.valueOf((char)0x0A), "");
      
    }
    
    
    
    public static String getTypeNames(TypeVariable[] tvars) { 
      if (tvars.length == 0) return "";
      StringBuilder sb = new StringBuilder(); 
      
      for (int i=0; i<tvars.length; i++) {
        TypeVariableImpl tvar 
          = (TypeVariableImpl) tvars[i]; 
        if (i>0) sb.append(", "); 
        
        sb.append(tvar.getName());  
      } 
      sb.append(">"); 
      sb.insert(0, "<");
      
      return sb.toString();
    }
      
  
  
    public static Class<?> getClass(Object o) 
    {
      Class<?> cls = null;
      String clsName = null;
      if (o instanceof ClassIdentifier) {
        try { 
          cls = (Class<?>) ((ClassIdentifier) o).getTargetClass();
          return cls;
        } catch (Throwable e) {
          clsName 
            = String.valueOf(o).substring(
              String.valueOf(o).indexOf(":") + 2 
          );
        }
      } // bsh.ClassIdentifier
      
      if (o instanceof Class<?>) {
        return ((Class<?>) o);
      }
      
      if ( o instanceof String) {
        try { 
          cls = Thread.currentThread().getContextClassLoader()
            .loadClass((String)o);
          if (cls != null) return (Class<?>) cls;
      } catch (Throwable e1) {
        clsName = (String) o;
      }
    }
    
    if (clsName != null) {
      try { 
        cls = Thread.currentThread().getContextClassLoader()
          .loadClass(clsName);
        if (cls != null) return (Class<?>) cls;
      } catch (Throwable e2) {
        System.err.println("Failed to loadClass() for: ["
          + clsName + "]");
        System.err.println(e2.getMessage());
        if ("true".equals(System.getProperty("printStackTrace"))) e2.printStackTrace();
        return (Class<?>) Void.class;
      }
    } // bsh.ClassIdentifier
    
    if ( o instanceof ParameterizedTypeImpl ) {
      ParameterizedTypeImpl pType 
        = (ParameterizedTypeImpl) o;
      Class<?> rawType = (Class<?>) pType.getRawType();
      
      typeMap.put(rawType, pType);
      return rawType;
    }
    
    try { 
      cls = (Class<?>) o.getClass(); 
    } catch (Throwable e3) {
      System.err.println("Failed to getClass() for object");
      System.err.println(e3.getMessage());
      if ("true".equals(System.getProperty("printStackTrace"))) e3.printStackTrace();
    }
    
    if (cls == null) return (Class<?>) Void.class;
    return null;
  }

    
 
  
}