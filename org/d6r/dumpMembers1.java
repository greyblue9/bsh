package org.d6r;
import bsh.*;

import java.lang.reflect.*;
import java.util.*;
import java.lang.reflect.*;
import org.d6r.ReflectionUtil;
import org.apache.commons.lang3.reflect.TypeUtils;

//import org.apache.commons.lang3.ClassUtils;

public class dumpMembers1 {
  
  public static 
            Map<Class<?>, Object> typeMap
 = new HashMap<Class<?>, Object>();
  
  public static boolean verbose = true;
  public static List<Throwable> exceptions;
  
  private static Method getClassSignatureAnnotation_Method = null;
  private static Method getClassName_Method = null;
  
  public static void invoke(Interpreter env, CallStack callstack,
  Object o) 
  {
    exceptions = new ArrayList<Throwable>() {};
    Class<?> cls = null;
    Object obj = null;
    if (o == null) {
      env.println("'o' is null");
      return;
    }
    if (o instanceof Class<?>) {
      cls = (Class<?>) o;
      obj = null;
    } else if (o instanceof ClassIdentifier) {
      cls = (Class<?>) ((ClassIdentifier) o).getTargetClass();
      obj = null;
    } else {
      cls = o.getClass();
      obj = o;
      Type superType = cls.getGenericSuperclass();
      if (superType instanceof ParameterizedType) {
        typeMap.put(cls, superType);
      }
    }
    env.println(dumpMembers1(obj, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls) {
    
    env.println(dumpMembers1(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls) 
  {
    env.println(dumpMembers1(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls, boolean verbose) {
    env.println(dumpMembers1(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls, boolean verbose) {
    env.println(dumpMembers1(o, cls, verbose));
  }
  
  public static String[] getSig(GenericDeclaration target) {
    return getSig(target, target.getClass());
  } 
  
  public static String[] getSig(GenericDeclaration target, Class<? extends GenericDeclaration> targetClass)
  {
    String[] arr = null;
    try {
      if ( getClassSignatureAnnotation_Method == null ) {
        getClassSignatureAnnotation_Method = 
          AccessibleObject.class.getDeclaredMethod(
            "getClassSignatureAnnotation",new Class[]{  
              Class.class });
        getClassSignatureAnnotation_Method.setAccessible(true); 
      }
      Object oArr = getClassSignatureAnnotation_Method
        .invoke(targetClass, target); 
      int len = Array.getLength(oArr); 
      arr = (String[]) Array.newInstance(String.class, len);
      System.arraycopy(oArr, 0, arr, 0, len); 
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return arr;
  }
  
  public static String dumpMembers1(Object o, Class<?> cls, boolean verbose) {
  
    /*Method gcn
 = ReflectionUtil.class.getDeclaredMethod("getClassName",
          new Class<?>[]{ Type.class });*/
    
    char lf = (char)0x0A;
    StringBuilder sb = new StringBuilder(1024);
    sb.append(lf);
       
    Method[] methods = cls.getDeclaredMethods();
    Field[] fields = cls.getDeclaredFields();
    Constructor[] ctors = cls.getDeclaredConstructors();
    
    Member[] members 
 = new Member[
        fields.length + ctors.length + methods.length ];
    
    System.arraycopy(fields, 0,  
      members, 0, 
      fields.length);
    System.arraycopy(ctors, 0,   
      members, fields.length,  
      ctors.length);
    System.arraycopy(methods, 0, 
      members, fields.length + ctors.length, 
      methods.length);
    
    Member m = null;
    
    appendColor(sb, "// As ", "1;30", new Object[]{});
    
    
    ParameterizedType _type = null;
    try { _type = (ParameterizedType) typeMap.get(cls); }
    catch (Throwable e) {}
    
    if (_type != null) {
      sb.append(colorize( 
        typeToString(_type),"1;36"));
      /*appendColor(sb, "%s", "1;36", new Object[]{ 
        ReflectionUtil.getClassName(_type) });*/
    } else {
      sb.append(colorize(
        cls.getName()
        + getTypeNames(cls.getTypeParameters()) ,"1;36"));
      /*
      appendColor(sb, "%s", "1;36", new Object[]{ cls.getName() });
      appendColor(sb, getTypeNames(cls.getTypeParameters()),
      "1;31", new Object[]{});*/
    }
    
    
    sb.append(lf);
    
    
    for (int n=0; n<members.length; n++) {
      
      String secName = null;
      if (n == fields.length + ctors.length) {
        secName = "Methods";
      } else if (n == fields.length) {
        secName = "Constructors"; 
      } else if (n == 0) {
        secName = "Fields"; 
      } 
      if (secName != null) {
        appendColor(sb, "  // ", "1;30",  new Object[]{});
        appendColor(sb, "%s%c", "0;36", new Object[]{ 
          secName, lf});
      }
      
      
      m = members[n];
      
      String name = m.getName();
      String argsPart = "";
      String valStr = "";
      Type type = null;
      Type retType = null;
      Type[] paramClzs = null;
      StringBuilder args; 
      
      if (m instanceof Method || m instanceof Constructor) 
      {
        
        if (m instanceof Method) {
          retType = ((Method)m).getGenericReturnType();
          paramClzs = ((Method)m).getGenericParameterTypes();
          
        } else if (m instanceof Constructor) {
          retType = TypeUtils.wrap(cls).getType();
          paramClzs 
 = ((Constructor<?>)m).getGenericParameterTypes();
          name = "<init>";
        }
        
        args = new StringBuilder(
          paramClzs.length * 12);
        
        for (int p=0; p<paramClzs.length; p++) {
          if (p > 0) args.append(", "); 
          args.append(
            colorize(
             typeToString( paramClzs[p] )
              .replace("java.lang.","")
              .replace("", "")
              .replace("[]", colorize("[]", "1;37")),
              "1;32"
            )
          );
        }
        argsPart = args.toString();
        

      } // Method | Constructor m
      else if (m instanceof Field) 
      {
        
        argsPart = null;
        retType = ((Field)m).getGenericType();
        boolean isStatic 
 = Modifier.isStatic(((Field)m).getModifiers());
        
        try {
          ((Field)m).setAccessible(true);
        } catch (Throwable e) {}
        try {
          Object value = null;
          
          if (o == null && !isStatic) {
            value = null;
          } else {
            value = ((Field) m).get(o);
          }
          
          if (value == null) {
            valStr = "<null>";
          } else {
            if (value.getClass().isArray()) {
              int len = Array.getLength(value);
              StringBuilder toString = new StringBuilder(25 * 5);
              toString.append("{ ");
              for (int i=0; i<25 && i<len; i++) {
                if (i > 0) toString.append(", ");
                toString.append( Array.get(value, i).toString() );
              }
              toString.append("}");
              valStr = toString.toString();
            } else { // not array
              try {
                valStr = value.toString();
              } catch (Throwable e4) {
                valStr = String.format(
                  "%s@%8x <toString() threw %s: \"%s\">",
                  value.getClass().getName(),
                  value.hashCode(),
                  e4.getClass().getSimpleName(),
                  e4.getMessage()
                );
              }
            }
          }// not null
        } catch (Throwable e5) {
          System.err.print(e5.getMessage());
        }
      }
      
  
      String typeStr = typeToString(retType);
      String modsStr = Modifier.toString(m.getModifiers());
      String strMember = "";
      
      sb.append("  ");
      sb.append(colorize(modsStr, "1;30"));
      if (modsStr.length() > 0) sb.append(' ');
      sb.append(colorize(typeStr.replace("java.lang.",""), "1;36"));
      sb.append(' ');
      sb.append(colorize(name,    "1;33"));
      if (argsPart != null) {
        sb.append(String.format(
          "%s%s%s", 
          "(", 
          colorize(argsPart.replace("java.lang.",""), "1;32", false),
          ")"
        ));
      }
      if (valStr.length() > 0) {
        sb.append(String.format(" = %s", valStr));
      }
      sb.append(';');
      sb.append(lf);
      //);
      //sb.append(strMember);
    }
    
    Object[] extendsArr = new Object[
      cls.getGenericInterfaces().length 
      + (cls.equals(Object.class)
          ? (cls.getGenericSuperclass() != null? 1: 0)
          : 0)
    ];
    System.arraycopy( cls.getGenericInterfaces(), 0, extendsArr, 0, cls.getGenericInterfaces().length );
    if (extendsArr.length > cls.getGenericInterfaces().length) {
      extendsArr[cls.getGenericInterfaces().length]
 = cls.getGenericSuperclass();
    }
          
    for (Object extended: extendsArr)
    {
      Class<?> iCls = null;
      
      if (extended instanceof ParameterizedType) {
        iCls = (Class<?>) ((ParameterizedType) extended).getRawType();
        typeMap.put(iCls, (ParameterizedType) extended);
      } else {
        iCls = (Class<?>) extended;
      }
      
      sb.append(lf);
      sb.append( dumpMembers1(o, iCls, verbose) );
    }
    
    Class<?> superCls = cls.getSuperclass();
    
    Object oSuperType = cls.getGenericSuperclass();
    ParameterizedType superType 
 = (oSuperType instanceof ParameterizedType)
        ? (ParameterizedType) oSuperType
        : null;
    
    if (superType != null) {
      typeMap.put(superCls, superType);
    }
    
    if (superCls != null && superCls != Object.class ) {
      sb.append((char)0x0A);
      sb.append(
        dumpMembers1(o, superCls, verbose)
      );
    }
    //sb.append(lf);
    return sb.toString();
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
        TypeVariable tvar 
 = (TypeVariable) tvars[i]; 
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
    
    if ( o instanceof ParameterizedType ) {
      ParameterizedType pType 
 = (ParameterizedType) o;
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
  