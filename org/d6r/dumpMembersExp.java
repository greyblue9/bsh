package org.d6r;
import bsh.*;

import java.lang.reflect.*;
import java.util.*;
import java.lang.reflect.*;
import org.d6r.ReflectionUtil;
import org.apache.commons.lang3.reflect.TypeUtils;
//import org.d6r.ITypeResolver;
//import org.d6r.IResolvedType;
//import org.apache.commons.lang3.ClassUtils;

public class dumpMembersExp {
  
  public static 
              Map<Class<?>, Object> typeMap
 = new HashMap<Class<?>, Object>();
  
  public static HashSet<Type> visitedTypes  
 = new HashSet<Type>();
  
  public static final String ABBREVIATOR_KEY = "abbreviator";
  public static final String ABBREVIATOR_DEFAULT = "^java\\.(?:(?:lang|util|io)\\.)?(?:(?:reflect|regex|zip|jar)\\.)?";
  public static final String EMPTY_STR = "";
  
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
    visitedTypes.clear();
    if (obj != null) typeMap.put(cls, obj);
    env.println(dumpMembersExp(obj, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls) {
    visitedTypes.clear();
    env.println(dumpMembersExp(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls) 
  {
    visitedTypes.clear();
    env.println(dumpMembersExp(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls, boolean verbose) {
    visitedTypes.clear();
    env.println(dumpMembersExp(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls, boolean verbose) {
    visitedTypes.clear();
    env.println(dumpMembersExp(o, cls, verbose));
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
  
  public static String dumpMembersExp
  (Object o, Class<?> cls, boolean verbose) {
  
    DumpMemberOptions opts 
 = new DumpMemberOptions() {{
          put(ABBREVIATOR_KEY, ABBREVIATOR_DEFAULT);
        }};
    opts.verbose = verbose;
    visitedTypes.clear();
    return dumpMembersExp(o, cls, opts);
  }
  
  
  public static <M extends GenericDeclaration> 
    String getMethodParamString(M m) 
  {
    TypeVariable[] tvars = m.getTypeParameters();
    if (tvars.length == 0) return null;
    StringBuilder sb = new StringBuilder(tvars.length * 24);
    sb.append("<");
    for (int i=0; i<tvars.length; i++) {
      if (i != 0) sb.append(", ");
      sb.append(tvars[i].toString());
    }
    sb.append(">");
    return sb.toString();
    //String genStr = m.toGenericString(); 
    //String outStr = genStr.replaceAll("^[^<\\(\\)]+(<[^\\(]+>) (.*) ([^ \\(]+)\\((.*)\\)$", "$1"); 
    //if (genStr.equals(outStr)) return null; 
    //return outStr;
  }
  
  public static String dumpMembersExp
  (Object o, Class<?> cls, DumpMemberOptions options) 
  {
    //options.visited = visitedTypes;
    StackTraceElement[] stack 
 = Thread.currentThread().getStackTrace();
    String stackStr = Arrays.toString(stack);
    int idx1 = stackStr.indexOf("dumpMembersExp");
    if (idx1 != -1) {
      int idx2 = stackStr.indexOf("dumpMembersExp", idx1+1);
      if (idx2 == -1) {
        visitedTypes.clear();
      }
    }
    
    
    
    boolean verbose = options.verbose;
    
    
    
    visitedTypes.add((Type) cls);
    String ABBREVIATOR = options.containsKey(ABBREVIATOR_KEY)
      ? (String) options.get(ABBREVIATOR_KEY)
      : ABBREVIATOR_DEFAULT;
    options.put(ABBREVIATOR_KEY, ABBREVIATOR);
    
    Type[] typeParams = new Type[0];
    /*try {
      typeParams = options.rt.typeParametersFor(cls).toArray(new Type[0]);
    } catch (Exception e) {
      typeParams = cls.getTypeParameters();
    }*/
    
    if (typeParams.length == 0) {
      typeParams = cls.getTypeParameters();
    }
    
    if (typeParams.length == 0) {
      try { 
        typeParams 
 = ((ParameterizedType) typeMap.get(cls))
              .getActualTypeArguments();
      } catch (Throwable e) {}
    }
        
    StringBuilder genSb = new StringBuilder(cls.getName());
    if  (typeParams.length > 0) {
      genSb.append("<");
      int tidx = 0;
      for (Type param: typeParams) {
        if (tidx != 0) genSb.append(", ");
        genSb.append(param.toString());
        tidx++;
      }
      genSb.append(">");
    }
    String genLabel = colorize(genSb.toString(), "1;36");
    
   
    char lf = (char)0x0A;
    StringBuilder sb = new StringBuilder(1024);
    sb.append(lf);
    
    sb.append(String.format(
      "%c[0;36m%s %s %c[1;35m%s %c[0;36m{%c[0m\n",
      0x1b,
      Modifier.toString(cls.getModifiers()),
      cls.isInterface()? "interface": "class",
      0x1b,
      genLabel,
      0x1b,
      0x1b
    ));
    
    
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
    
    appendColor(sb, "// As %s", "1;30", new Object[]{
      genLabel
    });
    
    
    //ParameterizedType _type = null;
    //try { _type = (ParameterizedType) typeMap.get(cls); }
    //catch (Throwable e) {}
    /*
    if (_type != null) {
      sb.append(colorize(
        typeToString(_type),
        "1;36"
      ));
    } else {
      sb.append(colorize(
        cls.getName()
          + getTypeNames(cls.getTypeParameters()), 
        "1;36"
      ));
    }
    */
      /*
      appendColor(sb, "%s", "1;36", new Object[]{ cls.getName() });
      appendColor(sb, getTypeNames(cls.getTypeParameters()),
      "1;31", new Object[]{});*/
    
    
    
    
    
    
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
        sb.append(lf);
        appendColor(sb, "// ", "1;30",  new Object[]{});
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
      String funcParamStr = null;
      if (m instanceof Method || m instanceof Constructor) 
      {
        
        if (m instanceof Method) {
          Method mthd = (Method) m;
          retType = mthd.getGenericReturnType();
          paramClzs = mthd.getGenericParameterTypes();
          funcParamStr = getMethodParamString(mthd);
        } else if (m instanceof Constructor) {
          Constructor ctor = (Constructor<?>) m;
          retType = TypeUtils.wrap(cls).getType();
          paramClzs = ctor.getGenericParameterTypes();
          name = "<init>";
          funcParamStr = getMethodParamString(ctor);
        }
        
        args = new StringBuilder(
          paramClzs.length * 12);
        
        for (int p=0; p<paramClzs.length; p++) {
          if (p > 0) args.append(", "); 
          args.append(
            colorize(
             typeToString( paramClzs[p] )
              .replaceAll(ABBREVIATOR, EMPTY_STR)
              .replace("[]", colorize("[]", "1;37")),
              "1;32"
            )
          );
        }
        argsPart = args.toString();
        

      } // Method | Constructor m
      else if (m instanceof Field) 
      {
        Field fld = (Field) m;
        
        argsPart = null;
        retType = fld.getGenericType();
        boolean isStatic = Modifier.isStatic(((Field)m).getModifiers());
        
        try {
          fld.setAccessible(true);
        } catch (Throwable e) {}
        
        Object value = null;
        try {
          // get value
          if (isStatic) {
            value = fld.get(null);
          } else if (o == null) {
            for (Map.Entry<Class<?>,Object> ent: typeMap.entrySet()) {
              if (ent.getValue() == null) continue;
              Class<?> c = ent.getKey();
              Object inst = ent.getValue();
              if (fld.getDeclaringClass().isAssignableFrom(inst.getClass()))
              {
                value = fld.get(inst);
                break;
              }
            }
          } else if (fld.getDeclaringClass().isAssignableFrom(o.getClass())) 
          {
            value = fld.get(o);
          } else if (o != null) {
            Field outerFld = null;
            Class cl = o.getClass();
            outer:
            do {
              for (Field f: o.getClass().getDeclaredFields()) {
                if (! f.getType().isAssignableFrom(fld.getDeclaringClass())) 
                {
                  continue;
                }
                outerFld = f;
                break outer;
              }
              cl = cl.getSuperclass();
            } while (cl != Object.class && cl != null);
            
            if (outerFld != null) {
              try { 
                outerFld.setAccessible(true);
                Object outer = outerFld.get(o);
                value = fld.get(outer);
              } catch (Throwable e) { System.err.println(e); }
            }
          }
          
          if (value == null) {
            valStr = "null";
          } else {
            if (value.getClass().isArray()) {
              int len = Array.getLength(value);
              StringBuilder toString = new StringBuilder(25 * 5);
              toString.append( value.getClass()
                  .getComponentType().getSimpleName());
              toString.append("[] { ");
              if (value.getClass().getComponentType()
                .isPrimitive()) {
                for (int i=0; i<25 && i<len; i++) {
                  if (i > 0) toString.append(", ");
                  toString.append( String.valueOf(
                    Array.get(value, i)) );
                }
              } else {
                try {
                  Object[] arr = (Object[]) value;
                  for (int i=0; i<25 && i<len; i++) {
                    if (i > 0) toString.append(", ");
                    toString.append(
                      (arr[i] != null)
                        ? arr[i].toString()
                        : "null"
                    );
                    
                  }
                } catch (Throwable e4) {
                  toString.append( String.format(
                    "%s@%8x <toString() threw %s: \"%s\">",
                    value.getClass().getName(),
                    value.hashCode(),
                    e4.getClass().getSimpleName(),
                    e4.getMessage()
                  ));
                  exceptions.add(e4);
                }
              }
              toString.append(" }");
              valStr = toString.toString();
            } else { // not array
              try {
                valStr = value.toString()
                  .replace("interface ", "")
                  .replace("class ", "");
              } catch (Throwable e4) {
                valStr = String.format(
                  "%s@%8x <toString() threw %s: \"%s\">",
                  value.getClass().getName(),
                  value.hashCode(),
                  e4.getClass().getSimpleName(),
                  e4.getMessage()
                );
                exceptions.add(e4);
              }
            }
          }// not null
        } catch (Throwable e5) {
          System.out.println(String.format(
            "Exception printing field: %s\n  (%s) m = [%s]\n",
            e5.getClass().getSimpleName(),
            m == null? "null": m.getClass().getName(),
            m == null? "null": m.toString()
          ));
          
          if ("true".equals(System.getProperty("printStackTrace"))) e5.printStackTrace(); 
          exceptions.add(e5);
          return sb.toString();
        }
      }
      String typeStr = typeToString(retType);
      String modsStr = Modifier.toString(m.getModifiers());
      String strMember = "";
      
      sb.append("  ");
      sb.append(colorize(modsStr, "1;30"));
      if (modsStr.length() > 0) sb.append(' ');
      
      if (funcParamStr != null && funcParamStr.length() >0)
      {
        sb.append(colorize(funcParamStr, "1;35"));
        sb.append(' ');
      }
      
      sb.append(colorize(typeStr.replace("java.lang.",""), "1;36").replace("35m", "36m"));
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
        sb.append(String.format(" = %s", colorize(valStr, "1;37")));
      }
      sb.append(';');
      sb.append(lf);
      //);
      //sb.append(strMember);
    }
    
    Class<?> superCls 
 = (cls.equals(Object.class)
        || cls.getName().equals("java.lang.Object"))
          ? null
          : cls.getSuperclass();
     
    if (superCls != null) {
      
      /** /
      Object oSuperType = cls.getGenericSuperclass();
      ParameterizedType superType 
 = (oSuperType instanceof ParameterizedType)
          ? (ParameterizedType) oSuperType
          : null;
      /* */
      Type superType = cls.getGenericSuperclass();
      if (superType != null) {
        typeMap.put(superCls, superType);
      }
      
      Object[] extendsArr = new Object[
          cls.getGenericInterfaces().length + 1
      ];
      
      System.arraycopy( 
        cls.getGenericInterfaces(), 
        0, 
        extendsArr, 
        0, 
        cls.getGenericInterfaces().length 
      );
      
      extendsArr[ extendsArr.length-1 ]
 = cls.getGenericSuperclass();
      
      Class<?>[] innerClasses
        = ClassInfo.findRelatedClasses(cls).toArray(new Class[0]);      
        // = cls.getDeclaredClasses();
      Class<?> innerCls = null;
      for (int icIdx=0; icIdx<innerClasses.length; icIdx++) 
      {
        innerCls = innerClasses[icIdx];
        if (visitedTypes.contains(innerCls)) continue; 
        //visitedTypes.add(innerCls); 
        String innerDumpHdr = colorize(
          "  // As inner " 
            + (innerCls.isInterface() ? "interface " : "class ") 
            + typeToString(innerCls), 
          "1;30"
        );
        String innerDump = EMPTY_STR;
        try {
          innerDump = dumpMembersExp(o, innerCls, options);
        } catch (Throwable e) { 
          System.err.println(
            e.getClass().getSimpleName()
            + ": " + e.getMessage() 
            + "@ dumpMembersExp.java: thrown in innerClasses try block by { innerDump = dumpMembersExp(..) .. } (innerCls = "
            + String.valueOf(innerCls)
          ); 
          exceptions.add(e);
          //innerDump = dumpMembers(null, innerCls, verbose);
        }
        sb.append(innerDumpHdr);
        sb.append(lf);
        sb.append("  ");
        sb.append(innerDump.replace("\n", "\n  "));
      }
      
      
      sb.append(String.format(
        "%c[0;36m}%c[0m\n", 0x1b, 0x1b
      ));
            
      for (Object extended: extendsArr)
      {
        Class<?> iCls = null;
        
        if (extended instanceof ParameterizedType) {
          iCls = (Class<?>) ((ParameterizedType) extended).getRawType();
          typeMap.put(iCls, (ParameterizedType) extended);
        } else {
          iCls = (Class<?>) extended;
        }
        if (visitedTypes.contains(iCls)) continue; 
        //options.visited.add(iCls); 
        sb.append(lf);
        String superDump = EMPTY_STR;
        try {
          superDump = dumpMembersExp( o, iCls, options);
        } catch (Throwable e) { 
          System.err.println(
            e.getClass().getSimpleName()
            + ": " + e.getMessage() 
            + "@ dumpMembersExp.java: thrown in (extended: extendsArr [superclass/iface]) try block by { superDump = dumpMembersExp(..) .. } (innerCls = "
            + String.valueOf(innerCls)
          ); 
          exceptions.add(e);
          sb.append(superDump);
          sb.append(lf);
          //innerDump = dumpMembers(null, innerCls, verbose);
        }
      }
      
      
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
  