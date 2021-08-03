package org.d6r;
import java.nio.charset.StandardCharsets;
import org.d6r.annotation.*;
import bsh.*;
import bsh.operators.Extension;
import java.lang.reflect.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import org.d6r.ReflectionUtil;
import static java.lang.System.arraycopy;
import org.apache.commons.lang3.reflect.TypeUtils;
import static org.d6r.MethodVisitor.getSimpleName;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
//import org.d6r.ITypeResolver;
import static org.d6r.Reflect.getGenericParameterTypes;
import static org.d6r.Reflect.getGenericReturnType;
import static org.d6r.Reflect.getTypeParameters;
import org.apache.commons.io.LineIterator;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import java.nio.charset.Charset;

//import org.d6r.IResolvedType;

//import org.apache.commons.lang3.ClassUtils;

public class dumpMembers {
  public static final String TAG
    = dumpMembers.class.getSimpleName();
    
  public static boolean LOGV;
  static {
    try {
      LOGV = Log.isLoggable(Log.SEV_VERBOSE);
    } catch (final Throwable e) {
      new RuntimeException(String.format(
        "org.d6r.dumpMembers.<clinit>: Unable to initialize " +
        "boolean property dumpMembers.LOGV " +
        "to the value `Log.isLoggable(Log.SEV_VERBOSE)` " +
        "(referencing the class 'org.d6r.Log'): " +
        "an error was encountered: %s", e), e
      ).printStackTrace();
    }
  }
  
  public static boolean verbose = LOGV;
  public static List<Throwable> exceptions;
  
  static Map<Class<?>, Object> typeMap
   = new HashMap<Class<?>, Object>();
  public static List<Throwable> errors = new ArrayList<>();
  
  static String EMPTY = "";
  static char ESC = (char)0x1b;
  
  static Method getClassSignatureAnnotation_Method = null;
  static Method getClassName_Method = null;
  
  public static PrintStream getOut(Interpreter i) {
    PrintStream ps = null;
    if (i != null && (ps = i.getOut()) != null) return ps;
    if ((ps = System.out) != null) return ps;
    return new PrintStream(new ByteArrayOutputStream());
  }
  public static PrintStream getErr(Interpreter i) {
    PrintStream ps = null;
    if (i != null && (ps = i.getErr()) != null) return ps;
    if ((ps = System.err) != null) return ps;
    return new PrintStream(new ByteArrayOutputStream());
  }
  public static PrintStream getOut() { return getOut(null); }
  public static PrintStream getErr() { return getErr(null); }
  
  static Constructor<?>[] NO_CTORS = new Constructor<?>[0];
  static Method[] NO_METHODS = new Method[0];
  static Field[] NO_FIELDS = new Field[0];
  static Member[] NO_MEMBERS = new Member[0];
  
  static Set<String> NO_DUMP_NAMES = Collections.emptySet();
  static boolean NO_DUMP_LOADED;
  
  static final int ACC_PRIVATE = 0x2;
  static final int ACC_STATIC = 0x8;
  static final int ACC_INTERFACE = 0x200;
  static final int ACC_ABSTRACT = 0x400;
  static final int ACC_SYNTHETIC = 0x1000;
  
  static Type[] NO_TYPES = new Type[0];
  
  static int ACC_SYNTH_ENUM_FIELD_V1 = ACC_SYNTHETIC | ACC_STATIC | ACC_PRIVATE;
  static int ACC_SYNTH_ENUM_FIELD_V2 = ACC_SYNTHETIC | ACC_STATIC;
  
  
  @Extension public static 
  Type[] getAllGenericInterfaces(Class<?> cls) 
  {
    Map<Class<?>, Type> ifs = new HashMap<Class<?>, Type>(); 
    for (Type iftype: cls.getGenericInterfaces()) { 
      Type rawtype = iftype;
      while (rawtype instanceof ParameterizedType) {
        rawtype = ((ParameterizedType) iftype).getRawType();
      }
      ifs.put((Class<?>) rawtype, iftype);
    } 
    for (Class<?> ifcls: cls.getInterfaces()) {
      if (ifs.containsKey(ifcls)) continue;
      ifs.put((Class<?>) ifcls, ifcls);
    } 
    return ifs.values().toArray(new Type[0]);
  }
  
  public static final String RULES_PATH = System.getProperty(
    "non.dumpable.rules.path",
    "/external_sd/_projects/sdk/bsh/trunk/out/non_dumpable.list"
  );
  
  static void loadNonDumpableRules() throws IOException {
    final File rulesFile = new File(RULES_PATH);
    final InputStream in;
    
    if (rulesFile.exists() && rulesFile.isFile() && rulesFile.length() > 0) {
      in = new FileInputStream(rulesFile);
    } else {
      final JarFile jar = ((JarURLConnection) 
        ClassInfo.getClassResource(bsh.Interpreter.class)
          .openConnection()
      ).getJarFile();
      final ZipEntry je = jar.getEntry("non_dumpable.list");
      if (je != null && je.getSize() != 0) {
        in = jar.getInputStream(je);
      } else {
        NO_DUMP_NAMES = Collections.emptySet();
        return;
      }
    }
    try {
      final LineIterator lit
        = IOUtils.lineIterator(in, StandardCharsets.UTF_8);
      Set<String> noDumpClassNames = new HashSet<String>();
      while (lit.hasNext()) { 
        String className = lit.nextLine().trim();
        if (className.length() == 0) continue;
        if (noDumpClassNames.add(className) && LOGV) {
          Log.v(TAG, "Added no-dump rule: class '%s'\n", className);
        }
      }
      NO_DUMP_NAMES = noDumpClassNames;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
  
  public static 
  void invoke(Interpreter i, CallStack cs, Object o) {
    getOut(i).println(dumpMembers(o));
  }
  
  public static Type getGenericSuperclass(Class<?> cls) {
    try {
      return cls.getGenericSuperclass();
    } catch (GenericSignatureFormatError e) {
      System.err.printf(
        "GenericSignatureFormatError: class signature for '%s'\n", 
        cls.getName()
      );
      return cls.getSuperclass();
    }
  }
  
  @Extension
  public static String dumpMembers(Object o) {
    exceptions = new ArrayList<Throwable>() {};
    Class<?> cls = null;
    Object obj = null;
    if (o == null) {
      new RuntimeException(
        "dumpMembers(Object o): o == null"        
      ).printStackTrace();
      return EMPTY;
    }
    if (o instanceof Class<?>) {
      if (((Class<?>)o).equals(Class.class)) {
        cls = (Class<Class<?>>) Class.class.getClass();
        obj = o;
      } else {
        cls = (Class<?>) o;
        obj = null;
      }
    } else if (o instanceof ClassIdentifier) {
      cls = ((ClassIdentifier) o).getTargetClass();
      obj = null;
    } else {
      cls = o.getClass();
      obj = o;
    }
    
    Type superType = getGenericSuperclass(cls);
    if (superType instanceof ParameterizedType 
     || superType instanceof GenericArrayType) {
      typeMap.put(cls, superType);
    }
    
    return dumpMembers(obj, cls, true);
  }
  
  public static 
  void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls) {
    env.println(dumpMembers(o, cls, verbose));
  }
  
  public static 
  void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls) {
    env.println(dumpMembers(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack, 
  Object o, Class<?> cls, boolean verbose) {
    env.println(dumpMembers(o, cls, verbose));
  }
  
  public static void invoke(Interpreter env, CallStack callstack,
  Class<?> o, Class<Class<?>> cls, boolean verbose) {
    env.println(dumpMembers(o, cls, verbose));
  }
  
  @Extension
  public static String[] getSig(GenericDeclaration target) {
    return getSig(target, target.getClass());
  } 
  
  @Extension
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
  
  @Extension
  public static String getMethodParamString(Object m) 
  {
    Type[] tvars = getTypeParameters(m, typeMap);
    if (tvars.length == 0) return null;
    StringBuilder sb = new StringBuilder(tvars.length * 24);
    sb.append("<");
    for (int i=0; i<tvars.length; i++) {
      if (i != 0) sb.append(", ");
      sb.append(tvars[i].toString());
    }
    sb.append(">");
    return sb.toString();
  }
  
  
  
  
  
  
  public static String dumpMembers(Object o, Class<?> cls, boolean verbose) {
    if (! NO_DUMP_LOADED) {
      try {
        loadNonDumpableRules();
      } catch (Throwable ex) {
        ex.printStackTrace();
      } finally {
        NO_DUMP_LOADED = true;
      }
    }

    StringBuilder sb = new StringBuilder(640);
    sb.append('\n');
    
    Type[] typeParams = getTypeParameters(cls);
    
    StringBuilder genSb = new StringBuilder(cls.getName());
    if (typeParams.length > 0) {
      genSb.append("\u001b[1;31m<");
      int tidx = 0;
      for (Type param: typeParams) {
        if (tidx != 0) genSb.append(", ");
        genSb.append(param.toString());
        tidx++;
      }
      genSb.append(">\u001b[0m");
    }
    String genLabel = colorize(genSb.toString(), "1;36");
    StringBuilder modsSb = ClassInfo.getModifiers(cls.getModifiers());
    sb.append(String.format(
      "\u001b[0;36m%s%s\u001b[1;35m%s \u001b[0;36m{\u001b[0m\n",
      modsSb,
      modsSb.length() == 0? EMPTY: " ",
      genLabel
    ));
    
   
    boolean isConst = !verbose && ClassInfo.isConstInterface(cls);
       
    Method[] methods = isConst
      ? NO_METHODS
      : cls.getDeclaredMethods();
    Field[] fields = isConst
      ? NO_FIELDS
      : cls.getDeclaredFields();
    Constructor[] ctors = isConst
      ? NO_CTORS
      : cls.getDeclaredConstructors();
    
    Member[] members = isConst
      ? NO_MEMBERS
      : new Member[ fields.length + ctors.length + methods.length ];
    
    if (!isConst) {
      arraycopy(fields, 0,members,0,                         fields.length);
      arraycopy(ctors,  0,members,fields.length,             ctors.length);
      arraycopy(methods,0,members,fields.length+ctors.length,methods.length);
    } else {
      appendColor(sb, "  // Only constant fields\n", "1;30", new Object[0]);
    }
    
    Member m = null;
    
    appendColor(sb, "  // As ", "1;30", new Object[]{});
    ParameterizedType _type = null;
    try { _type = (ParameterizedType) typeMap.get(cls); }
    catch (Throwable e) {}
    
    if (_type != null) {
      sb.append(colorize(
        typeToString(_type), "1;36"
      ).replace("\u000b", ""));

    } else {
      sb.append(colorize(ClassInfo.typeToName(cls.getName()), "1;36"))
        .append(colorize(
          getTypeNames((TypeVariable[]) (Object) getTypeParameters(cls)), "1;36"
        ));
    }
    
    sb.append('\n');
    
    nextMember:
    for (int n=0; n<members.length; n++) {
      if (isConst) break;
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
          secName, '\n'});
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
        paramClzs = getGenericParameterTypes(m);
        funcParamStr = getMethodParamString(m);
        if (m instanceof Method) {
          Method mthd = (Method) m;
          retType = getGenericReturnType(mthd);
        } else if (m instanceof Constructor) {
          Constructor<?> ctor = (Constructor<?>) m;
          retType = TypeUtils.wrap(cls).getType();
        }
        
        args = new StringBuilder(
          paramClzs.length * 12);
        
        for (int p=0; p<paramClzs.length; p++) {
          if (p > 0) args.append(", "); 
          args.append(colorize(
            getSimpleName(typeToString(paramClzs[p]))
              .replace("[]", "\u001b[0m\u001b[1;37m[]\u001b[0m"),
            "1;32"
          ).replace("\u000b", ""));
        }
        argsPart = args.toString();
        
        // end if (m <: Method|Constructor)
      } else if (m instanceof Field) {
        
        Field fld = (Field) m;
        int acc = fld.getModifiers();
        argsPart = null;
        retType = fld.getGenericType();
        final boolean isStatic = (acc & ACC_STATIC) != 0;
        
        /**
        private static int[] $SWITCH_TABLE$org$jf$dexlib2$Format = <null>; // V1
        static final int[] $SwitchMap$org$jf$dexlib2$Format // V2
        */
        if ((acc & ACC_SYNTH_ENUM_FIELD_V1) == ACC_SYNTH_ENUM_FIELD_V1 ||
            (acc & ACC_SYNTH_ENUM_FIELD_V2) == ACC_SYNTH_ENUM_FIELD_V2)
        {
          if (int[].class.equals(fld.getType())) {
            if (name.charAt(0) == '$' && name.startsWith(
               (acc & ACC_PRIVATE) != 0? "$SWITCH_TABLE$": "$SwitchMap$"))
            {
              continue nextMember;
            }
          }
        }
        
        try {
          fld.setAccessible(true);
        } catch (Throwable e) {
          throw Reflector.Util.sneakyThrow(e);
        }
        Object value = null;
        try {
          value = (o == null && !isStatic)? null: fld.get(o);
          Class<?> clsOfValue = value != null
            ? value.getClass()
            : Object.class;
          if (clsOfValue == null) clsOfValue = Object.class;
          String valClsName = clsOfValue != Object.class
            ? clsOfValue.getName()
            : (value != null)
                ? "Object"
                : "null";
          
          if ((valClsName != null && NO_DUMP_NAMES.contains(valClsName))
          ||  NO_DUMP_NAMES.contains(fld.getName())
          ||  value instanceof com.android.dex.Dex
          ||  value instanceof dalvik.system.DexFile)
          {
            valStr = String.format("\u001b[1;35m%s\u001b[0m", valClsName);
          } else {
            NonDumpable ann = fld.getAnnotation(NonDumpable.class);
            if (ann != null) {
              valStr
                = String.format("\u001b[1;35m%s\u001b[0m", ann.value());
              valStr = String.format(
                "\u001b[1;35m%s\u001b[0m", 
                ann.replacement().render(
                  value, ann.value()
                )
              );
            } else if (value == null) {
              valStr = "<null>";
            } else if ((ann = clsOfValue.getAnnotation(
              NonDumpable.class)) != null)
            {
              valStr = String.format(
                "\u001b[1;35m%s\u001b[0m", 
                ann.replacement().render(
                  value, ann.value()
                )
              );
            } else {
              valStr = Debug.ToString(value);
            }
          }
          
        } catch (Throwable e4) {
          valStr = String.format(
            "%s@%8x <toString() threw %s: \"%s\">",
            ClassInfo.getSimpleName(value),
            System.identityHashCode(value),
            TextUtil.str(e4),
            e4.getMessage()
          );
        }
        
     }// if field
      
      
  
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
      sb.append(colorize(
        getSimpleName(typeStr), "1;36"
      ).replace("\u000b", ""));
      sb.append(' ');
      sb.append(colorize(name,    "1;33"));
      if (argsPart != null) {
        sb.append(String.format(
          "%s%s%s", 
          "(", 
          colorize(
            getSimpleName(argsPart), "1;32", false
          ),
          ")"
        ));
      }
      if (valStr.length() > 0) {
        sb.append(String.format(" = %s", valStr));
      }
      sb.append(';');
      sb.append('\n');
      //);
      //sb.append(strMember);
    }
    
    
    
    
    sb.append(String.format(
      "%c[0;36m}%c[0m\n", 0x1b, 0x1b
    ));
    
    
    
    Type[] ifaceTypes;
    try {
      ifaceTypes = getAllGenericInterfaces(cls);
    } catch (Throwable e) {
      e.printStackTrace();
      ifaceTypes = cls.getInterfaces();
    }
    
    Class<?> superCls = cls.getSuperclass();
    Object oSuperType = superCls;
    try {
      oSuperType = cls.getGenericSuperclass();
      ParameterizedType superType 
        = (oSuperType instanceof ParameterizedType)
          ? (ParameterizedType) oSuperType
          : null;
      
      if (superType != null) {
        typeMap.put(superCls, superType);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    
    
    Object[] extendsArr = new Object[
      ifaceTypes.length + (cls.equals(Object.class)
          ? (oSuperType != null? 1: 0)
          : 0)
    ];
    
    System.arraycopy(ifaceTypes, 0, 
      extendsArr, 0, ifaceTypes.length);
    
    if (extendsArr.length > ifaceTypes.length) {
      extendsArr[ifaceTypes.length] = oSuperType;
    }
    
    for (Object extended: extendsArr) {
      Class<?> iCls = null;
      if (extended instanceof ParameterizedType) {
        iCls = (Class<?>)
          ((ParameterizedType) extended).getRawType();
        typeMap.put(iCls, (ParameterizedType) extended);
      } else {
        iCls = (Class<?>) extended;
      }
      sb.append('\n');
      sb.append( dumpMembers(o, iCls, verbose) );
    }
    
    if (superCls != null && superCls != Object.class) {
      sb.append((char)0x0A);
      sb.append(
        dumpMembers(o, superCls, verbose)
      );
    }
    //sb.append('\n');
    return sb.toString();
  }
  
  public static String colorize(Member[] members) {
    StringBuilder sb = new StringBuilder(76 * 100);
    for (Member m: members) {
      sb.append(colorize(m));
      sb.append('\n');
    }
    return sb.toString();
  }
  
  public static String colorize(Iterable<Member> members) {
    StringBuilder sb = new StringBuilder(76 * 100);
    for (Iterator<Member> it = members.iterator();
     it.hasNext();) 
    {  
      sb.append(colorize((Member) it.next()));
      sb.append('\n');
    }
    return sb.toString();
  }

  @Extension
  public static String colorize(Member m) {
    return colorize((Member) m, (Object) null);
  }
  
  public static String colorizeNoValue(Member m) {
    return colorize((Member) m, (Object) EMPTY);
  }
  
  public static String colorize(Member m, Object o) {
      if (m == null) return EMPTY;
      StringBuilder sb = new StringBuilder(48);
      Class<?> cls = m.getDeclaringClass();
      
      String name = m.getName();
      String argsPart = "";
      String valStr = "";
      Type type = null;
      Type retType = null;
      Type[] paramClzs = null;
      StringBuilder args; 
      String funcParamStr = null;
      
      if (m instanceof Method || m instanceof Constructor) {
        
        if (m instanceof Method) {
          Method mthd = (Method) m;
          retType = mthd.getGenericReturnType();
          paramClzs = mthd.getGenericParameterTypes();
          funcParamStr = getMethodParamString(mthd);
        } else if (m instanceof Constructor) {
          Constructor<?> ctor = (Constructor<?>) m;
          retType = TypeUtils.wrap(cls).getType();
          paramClzs = ctor.getGenericParameterTypes();
          funcParamStr = getMethodParamString(ctor);
        }
        
        args = new StringBuilder(paramClzs.length * 24);
        
        for (int p=0; p<paramClzs.length; p++) {
          if (p > 0) args.append(", "); 
          args.append(colorize(
            getSimpleName(typeToString(paramClzs[p]))
              .replace("[]", "\u001b[0m\u001b[1;37m[]\u001b[0m"),
            "1;32"
          ).replace("\u000b", ""));
        }
        argsPart = args.toString();
        

      } // Method | Constructor m
      else if (m instanceof Field) 
      {
        Field fld = (Field) m;
        argsPart = null;
        retType = fld.getType();
        try {
          retType = fld.getGenericType();
        } catch (Error e) {
          e.printStackTrace();
        }
        boolean isStatic = !((m.getModifiers() & ACC_STATIC) == 0);
        
        if (o == EMPTY || (!isStatic && o == null)) {
          valStr = EMPTY;
        } else {
          try {
            fld.setAccessible(true);
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
                    ClassInfo.getSimpleName(value),
                    System.identityHashCode(value),
                    TextUtil.str(e4),
                    e4.getMessage()
                  );
                }
              }
            }// not null
          } catch (Throwable e5) {
            System.err.print(e5.getMessage());
          }
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
      sb.append(colorize(
        getSimpleName(typeStr), "1;36"
      ).replace("\u000b", ""));
      sb.append(' ');
      sb.append(colorize(name,    "1;33"));
      if (argsPart != null) {
        sb.append(String.format(
          "%s%s%s", 
          "(", 
          colorize(getSimpleName(argsPart), "1;32", false),
          ")"
        ));
      }
      if (valStr.length() > 0) {
        sb.append(String.format(" = %s", valStr));
      }
      sb.append(';');
      sb.append((char) 0x0f);
      return sb.toString();
    }
    
    public static String colorize(String fmt, 
    String colorspec, boolean useColor) 
    {
      if (! useColor) return fmt;
      char esc = (char)0x1b;
      
      return String.format(
        "\u001b[%sm%s\u001b[0m", colorspec, fmt, "0"
      ) .replace("<", "\u001b[0;31m<\u001b[1;31m")
        .replace(">", "\u001b[0;31m>\u001b[1;31m")
        .replaceAll(
          String.format(">\u001b%c[1;31m([ ])", 0x5c),
          ">\u001b[0m$0"
        )
        .replaceAll("$", "\u001b[0m")
        .replace("[]", "\u001b[0m\u001b[1;37m[]\u001b[0m");
    }
    
    @Extension
    public static String colorize(String fmt, String colorspec) {
      return colorize(fmt, colorspec, true);
    }
    
    
    @Extension
    public static StringBuilder appendColor(StringBuilder sb, String fmt, String colorspec, Object[] args) {
      return sb.append(String.format(
        colorize(fmt, colorspec, true),
        args
      ));
    }
    
    
    @Extension
    public static String toGenericString(Type type) {
      return typeToString(type);
    }
    
    public static final char[] ARRAY_BRACKETS = new char[]{ '[', ']' };
    
    @Extension
    public static String typeToString(Type type) {
      if (type == null) return "?";
      
      
      StringBuilder sb = new StringBuilder(10);
      
      if (type instanceof Class<?>) {
        sb.append(ClassInfo.typeToName(((Class<?>) type).getName()));
      } else if (type instanceof TypeVariable) {
        sb.append('\u000b');
        sb.append(((TypeVariable) type).getName());
      } else {
        sb.append('\u000b');
        sb.append(ReflectionUtil.getClassName(type));
      }
      
      while (sb.length() > 0 && sb.charAt(0) == '[') {
        sb.delete(0, 1);
        sb.append(ARRAY_BRACKETS);    
      }
      
      int idx;
      if (sb.length() > 0 
      &&  sb.charAt(0) == 'L' 
      &&  (idx = sb.indexOf(";")) != -1) 
      {
        sb.delete(idx, idx+1);
        sb.delete(0, 1);
      } 
      return sb.toString();      
    }
    
    
    
    public static String getTypeNames(TypeVariable[] tvars) { 
      if (tvars.length == 0) return "";
      StringBuilder sb = new StringBuilder(); 
      
      for (int i=0; i<tvars.length; i++) {
        TypeVariable tvar = (TypeVariable) tvars[i]; 
        if (i>0) sb.append(", "); 
        
        sb.append(tvar.getName());  
      } 
      sb.append(">\u001b[0m"); 
      sb.insert(0, "\u001b[1;31m<\u001b[1;31m");
      
      return sb.toString();
    }
      
  
  
  public static Class<?> getClass(Object o) {
    if (o == null) return Object.class;    
    if (o instanceof Class<?>) return ((Class<?>) o);
    if (o instanceof ClassIdentifier) {
      return ((ClassIdentifier) o).getTargetClass();
    }
    
    Class<?> cls = null;
    String clsName = null;
    if (o instanceof String) {
      if (Capabilities.classExists((String) o)) {
        try {
          return Class.forName(
            (String) o, false, Thread.currentThread().getContextClassLoader()
          );
        } catch (LinkageError | ReflectiveOperationException ncdfe) {
          errors.add(ncdfe);
          System.err.println(ncdfe);                              
          return null;
        }
      }
    }

    if ( o instanceof ParameterizedType ) {
      ParameterizedType pType = (ParameterizedType) o;
      Class<?> rawType = (Class<?>) pType.getRawType();      
      typeMap.put(rawType, pType);
      return rawType;
    }
    try { 
      return (Class<?>) o.getClass(); 
    } catch (Throwable e3) {
      System.err.println("Failed to getClass() for object");
      System.err.println(e3.getMessage());
      e3.printStackTrace();
      return null;
    }

  }
    
    
    
}
  