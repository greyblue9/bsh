package org.d6r;


import bsh.Capabilities;
import bsh.ClassIdentifier;
import bsh.classpath.AndroidClassLoader;
import bsh.operators.Extension;
import com.android.dex.ClassData;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dex.TableOfContents;
import com.google.common.base.Optional;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.strobel.assembler.metadata.ArrayTypeLoader;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import dalvik.system.BaseDexClassLoader;
import dx2.cf.direct.AttributeFactory;
import dx2.cf.direct.DirectClassFile;
import dx2.cf.direct.StdAttributeFactory;
import dx2.cf.iface.ParseException;
import java.io.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.net.*;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtClassType2;
import javassist.CtNewClass2;
import javassist.JarClassPath;
import javassist.NotFoundException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import libcore.reflect.InternalNames;
import com.google.common.collect.Iterables;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.annotation.NonDumpable;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.INTERFACE;
import static java.util.Collections.singletonMap;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getFullClassPath;
import static org.d6r.CollectionUtil2.typeFilter;
import static org.d6r.DebugReader.openAt;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflect.setfldval;
import static org.d6r.TextUtil.str;
import java.io.*;
import sun.misc.URLClassPath;
// import static org.d6r.StringBuilderSupplier.getStringBuilder;
// import static org.d6r.StringBuilderSupplier.releaseStringBuilder;

import java.lang.reflect.Member;


public class ClassInfo {
  
  public static final String TAG = ClassInfo.class.getSimpleName();
  public static boolean generateTypes = true;
  
  public static final boolean JRE = CollectionUtil.isJRE();
  public static boolean BRUTE_FORCE_DISABLED = Boolean.parseBoolean(
    System.getProperty("brute.force.disabled", "false")
  );
  public static boolean ALLOW_GEN_MISSING = Boolean.parseBoolean(
    System.getProperty("allow.gen.missing", "true")
  );
  public static final File DEFAULT_DEX_INDEX_DIR
    = new File("/external_sd/_projects/__dex_index__");
  
  public static File DEX_INDEX_DIR = new File(System.getProperty(
    "dex.index.dir",
    (DEFAULT_DEX_INDEX_DIR.exists() && !DEFAULT_DEX_INDEX_DIR.isFile())
      ? DEFAULT_DEX_INDEX_DIR.getPath()
      : "/dev/null"
  ));
  
  
  public static final Set<String> BOGUS_SUFFIXES = CollectionFactory.newSet(
    "classes",
    "lib/jfr.jar",
    "lib/management-agent.jar",
    "lib/resources.jar",
    "lib/security/local_policy.jar",
    "lib/sunrsasign.jar"
  );
  
  
  @NonDumpable("[ Class-Boolean Cache for isConstInterface(..) ]")
  public static Map<Class<?>, Boolean> isConst;
  @NonDumpable("[ Class-Boolean Cache for hasState(..) ]")
  public static Map<Class<?>, Boolean> hasState;
  
  public static final List<Object> errors = new ArrayList<>();
  
  private static SortedSet<Character> PRIMITIVE_SHORTY_CHARACTERS;
  private static SortedSet<String>    PRIMITIVE_SHORTY_STRINGS;
  private static SortedSet<String>    PRIMITIVE_CANONICAL_NAMES;
  
  static final Map<Object, Map<String, Dex>> dexCache = new SoftHashMap<>();
  static final Map<Object, String> dexLocations = new HashMap<>();
  
  static LazyMember<Method> GET_BOOTSTRAP_RES = LazyMember.of(
    ClassLoader.class, "getBootstrapResources", String.class
  );
  static final String EMPTY_STRING = "";
  public static final int ANNOTATION = 8192;
  public static final int ENUM = 16384;
  
  public static final Matcher STANDARD_PACKAGES
    = Pattern.compile(
      "(^| |<|\\()"
      + "(?:java\\.lang(?:\\.reflect)?|java\\.util|"
      + "javax\\.annotation|"
      + "java\\.io|com\\.google\\.common\\.annotations|"
      + "^[^$]*\\.annotations|"
      + "org\\.intellij\\.lang\\.annotations|"
      + "org\\.jetbrains\\.annotations|"
      + "android.view.ViewDebug|"
      + "org\\.apache\\.commons\\.lang3(?:\\.tuple)?|org\\.d6r|"
      + "dalvik\\.annotation)[.$]([^.]+|<[^()=;]*>|[^.]+,)$"
    ).matcher("");
  public static final String STANDARD_REPLACEMENT = "$1$2";
  
  static final Matcher CLEAN_CLASSPATH_MCHR
    = Pattern.compile("^:*|:*$|(?<=:):*").matcher("");
  public static final String CLASS_SUFFIX = ".class";
  public static final int CLASS_SUFFIX_LEN = 6;
  
  public static final Matcher BOOTCLASSPATH_MATCHER
    = Pattern.compile("(?<=^|:)/system/framework/([^:]*)").matcher("");
  public static final String  BOOTCLASSPATH_REPLACEMENT
    = "/external_sd/_projects/sdk/framework/$1";
  public static boolean EXPRINTED = false;
  
  static Object bootstrapHolder;
  
  static Optional<Class<?>> NameSpace_cls;
  static Optional<Method>   NameSpace_getClassResource;
  static final Object DEX_CTOR;
  static {
    Object lm = null;
    try {
      lm = LazyMember.of(Dex.class, "<init>", ByteBuffer.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    DEX_CTOR = lm;
  }
  
  public static boolean isConstInterface(Class<?> cls) {

    Boolean val = null;
    if (isConst == null) isConst = new WeakHashMap<Class<?>, Boolean>();
    else if ((val = isConst.get(cls)) != null) return val.booleanValue(); 
    try { 
      if (! cls.isInterface() 
         || cls.getDeclaredMethods().length > 0
         || cls.getDeclaredConstructors().length > 0)
      {
        return (val = Boolean.FALSE).booleanValue();
      }
      Field[] flds = cls.getDeclaredFields();
      int i = -1, len = flds.length;
      while (++i < len) { 
        int mods = flds[i].getModifiers();
        if ((mods | (~(Modifier.STATIC | Modifier.FINAL))) != -1) 
        { // not all bits set
          return (val = Boolean.FALSE).booleanValue();
        }
      }
      return (val = Boolean.TRUE).booleanValue();
    } finally { 
      if (val != null) isConst.put(cls, val);
    }
  }
  
  public static boolean hasState(Class<?> cls, boolean considerSuper) {
    if (hasState == null) hasState = new WeakHashMap<Class<?>, Boolean>();
    Boolean val = hasState.get(cls);
    if (val != null) {
      if (!considerSuper || val == Boolean.TRUE) return val.booleanValue();
    }
    // *** considerSuper == true ***
    Class<?> cur = cls;
    outer:
    do {
      inner:
      while (val == null || val == Boolean.FALSE) {
        if (val == null && (val = hasState.get(cls) == null)) {
          Field[] flds = cur.getDeclaredFields();
          val = Boolean.valueOf(flds.length != 0);
          hasState.put(cls, val);
        }
        if (cur.getSuperclass() == null) break outer;      
      }
      // back exit
      if (val == Boolean.TRUE && cur != null) hasState.put(cls, val);
    } while (false); // "break due to class" exit
    
    if (val == null) {
      System.err.printf(
        "[WARN] hasState(Class<?> cls, boolean considerSuper): "
        + "val == null at end of iteration; cur = %s; assuming false!",
        cls, Boolean.valueOf(considerSuper), cur
      );
      val = Boolean.FALSE;
    }
    return val.booleanValue();
  }
  
  
  @NotImplemented("Untested method")
  public static Set<? extends Class<?>> findInnerClasses(Class<?> cls) {
    Deque<Class<?>> 
      que = new ArrayDeque<Class<?>>(Arrays.asList(cls)),
      out = new ArrayDeque<Class<?>>(); 
    ClassLoader ldr 
      = Thread.currentThread().getContextClassLoader();
    
    while (! que.isEmpty()) { 
      Class<?> cl = que.pollLast();
      
      Class<?>[] declaredNested = cl.getDeclaredClasses();
      Collections.addAll(out, declaredNested);
      Collections.addAll(que, declaredNested);
      
      int anonIdx = 0;
      Class<?> anonCls = null;
      do {
        String anonClassName 
          = StringUtils.join(cl.getName(), "$", ++anonIdx);
          
        if (! Capabilities.classExists(anonClassName)) break; 
        anonCls = null;
        try {
          Reflect.classForName.setAccessible(true);
          anonCls = (Class<?>) Reflect.classForName.invoke(
            null, anonClassName, false, ldr
          );
        } catch (Exception ex) {
          System.err.println(ExceptionUtils.getRootCause(ex));
        }
        out.push(anonCls); 
        que.offerFirst(anonCls);
      } while (anonCls != null);
    } 
    
    Class<?>[] outarr = out.toArray(new Class[0]);
    Arrays.sort(outarr, new ToStringComparator());
    return new HashSet<Class<?>>(Arrays.asList(outarr));
  }
  
  
  
  public static Set<? extends Class<?>> getInterfaces(final Class<?> cls) {
    final List<Class<?>> clsList = new ArrayList<>(16);
    final Set<Class<?>> seen = new IdentityHashSet<Class<?>>();
    final Deque<Class<?>> q = new ArrayDeque<>(
       (Collection<? extends Class<?>>) Arrays.asList(cls)
    );
    while (! q.isEmpty()) {
      final Class<?> c = q.poll();
      seen.add(c);
      clsList.add(c);
      
      try { 
        final Class<?>[] ifaces = c.getInterfaces();
        final int len = ifaces.length;
        for (int i=0; i<len; ++i) {
          final Class<?> iface = ifaces[i];
          if (seen.add(iface)) q.offer(iface);
        }
        if (false) throw new ReflectiveOperationException();
      } catch (final ReflectiveOperationException | LinkageError e) {
        e.printStackTrace();
      }
      
      try {
        final Class<?> sc = c.getSuperclass();
        if (sc != null && sc != Object.class && seen.add(sc)) q.offer(sc);
        if (false) throw new ReflectiveOperationException();
      } catch (final ReflectiveOperationException | LinkageError e) {
        e.printStackTrace();
      }
    }
    return new LinkedHashSet<>((Collection<? extends Class<?>>) clsList);
  }
  
  
  
  
  @NotImplemented("Untested method")
  public static Set<? extends Class<?>> findRelatedClasses(Class<?> cls) {
    Set<Class<?>> classes = new HashSet<Class<?>>();
    classes.addAll( getInterfaces(cls) );
    classes.addAll( 
      (Collection<? extends Class<?>>) (Object) 
      findInnerClasses(cls) 
    );      
    List<Class<?>> additional = new ArrayList<Class<?>>();
    for (Class<?> iface: classes) {
      additional.addAll( 
        (Collection<? extends Class<?>>) 
        (Object) findInnerClasses(iface) 
      );      
    }
    classes.addAll(additional);
    return classes;
  }
  
  public static String simplifyName(Object className) {
    if (className == null) return "NULL";
    final String str;
    if (className instanceof String) {
      str = (String) className;
    } else if (className instanceof CharSequence) {
      str = ((CharSequence) className).toString();
    } else if (className instanceof Class<?>) {
      str = ((Class<?>) className).getName();
    } else {
      str = String.valueOf(className);
    }
    return STANDARD_PACKAGES.reset(
      DexVisitor.typeToName( str )
    ).replaceAll(STANDARD_REPLACEMENT);
  }
  
  public static String typeToName(String type) {
    return typeToName(type, null);
  }
  
  public static String getGenericTypeName(final Class<?> cls) {
    if (cls == null) return "null";
    final TypeVariable[] typeVars;
    TypeVariable[] _tvs = null;
    try {
      _tvs = cls.getTypeParameters();
    } catch (final Error e) {
      Log.w(TAG, String.format(
        "Generic signature for %s is malformed: %s", cls.getName(), e
      ), new Object[0]);
    }
    typeVars = (_tvs == null)? new TypeVariable[0]: _tvs;
    return String.format(
      typeVars.length == 0? "%1$s": "%1$s<%2$s>",
      typeToName(cls.getName()),
      typeVars.length == 0? StringUtils.join(typeVars, ", "): null
    );
  }
  
  public static String getInternalName(final String signature) {
    final char[] caSignature = signature.toCharArray();
    int beginIndex, length = signature.length();
    for (beginIndex = 0;
         beginIndex < length && caSignature[beginIndex] == '[';
       ++beginIndex)
    {
      ;
    }
    if (beginIndex < length && caSignature[beginIndex] == 'L') {
        ++beginIndex;
        --length;
        return String.valueOf(caSignature, beginIndex, length);
    }
    return (beginIndex == 0)
      ? signature
      : String.valueOf(caSignature, beginIndex, length);
  }
  
  public static String getSimpleName(Object className) {
    if (className == null) return "NULL";
    String str;
    if (className instanceof String) {
      str = (String) className;
    } else if (className instanceof CharSequence) {
      str = ((CharSequence) className).toString();
    } else if (className instanceof Class<?>) {
      final Class<?> cls = (Class<?>) className;
      str = cls.getName();
      try {
        if (cls.isAnonymousClass()) {
          Class<?>[] ifaces;
          Class<?> sc, base = ((sc = cls.getSuperclass()) != Object.class)
            ? sc
            : ((ifaces = cls.getInterfaces()).length != 0)
                ? ifaces[0]
                : Object.class; 
          return String.format(
            "%2$s(%1$s)",
            getSimpleName(base), getSimpleName(cls)
          );
        }
        str = cls.getName();
      } catch (LinkageError | InternalError e) {
        Log.w(
          "ClassInfo", 
          String.format(
            "getSimpleName: Encountered %s while reading " +
            "basic properties of class '%s'; returning entire name ... %s",
            e.getClass().getSimpleName(), str, e
          ), e
        );
        return cls.getName();
      }
    } else if (className instanceof ClassIdentifier) {
      str = ((ClassIdentifier) className).getTargetClass().getName();
    } else {
      return getSimpleName(className.getClass());
    }
    str = ClassInfo.typeToName(str);
    
    int MIN_NAME_LEN = 3;
    int lastDot = str.lastIndexOf('.');
    if (lastDot == -1) return str;
    
    int lenAfterDot = str.length() - (lastDot+1);
    if (lenAfterDot < MIN_NAME_LEN) return str;
    
    int lastDollar = str.lastIndexOf('$');
    final CharSequence simpleName = str.subSequence(lastDot+1, str.length());
    if (lastDollar == -1 || lastDollar < lastDot) {
      return simpleName instanceof String
        ? (String) simpleName
        : simpleName.toString();
    }
    final StringBuilder sb = new StringBuilder(simpleName);
    
    int idx = lastDollar - (lastDot+1);
    do {
      sb.replace(idx, idx+1, ".");
    } while ((idx = sb.indexOf("$")) != -1);
    return sb.toString();
  }
  
  
  static volatile StringBuilder[] sbCache = new StringBuilder[31];
  static volatile int inUse = 0;
  public static StringBuilder getStringBuilder() {
    int bit = 1, index = 0;
    do {
      if ((inUse & bit) == 0) {
        inUse |= bit;
        if (sbCache[index] == null) sbCache[index] = new StringBuilder(256)
          .append("sb # ").append(index);
        final StringBuilder sb = sbCache[index];
        sb.setLength(0);
        return sb;
      }
    } while (((bit <<= 1) & 0x7FFFFFFF) != 0 && ++index != 31);
    return new StringBuilder();
  }
  public static void releaseStringBuilder(final StringBuilder sb) {
    for (int i=0, bit = 1; i<31; i += ((bit <<= 1) != 0)? 1: 0) {
      if (sbCache[i] == sb) {
        inUse ^= bit;
        return;
      }
    }
  }
  
  
  public static String typeToName(final String orig, String ifnull) {
    // catch "B[][]"
    if (orig == null) return ifnull;
    if (orig.length() == 0) return orig;
    
    final StringBuilder sb = getStringBuilder();
    final StringBuilder arrSb = getStringBuilder();
    try {
      int len = orig.length();
      if (len >= CLASS_SUFFIX_LEN &&
          CLASS_SUFFIX.equals(orig.subSequence(len-CLASS_SUFFIX_LEN, len)) &&
          orig.lastIndexOf('.', len - CLASS_SUFFIX_LEN - 1) == -1)
      {
        sb.append(orig.subSequence(0, len-CLASS_SUFFIX_LEN));
      } else {
        sb.append(orig);
      }
      
      if ((sb.length() > 1 && sb.charAt(0) == '[') ||
          (sb.length() > 2 && sb.charAt(0) == 'L'))
      {
        while (sb.length() > 1 && sb.charAt(0) == '[') {
          sb.delete(0, 1);
          arrSb.append("[]");
        }
        if (sb.length()>2 && sb.charAt(0)=='L' && sb.charAt(sb.length()-1) ==';') {
          sb.delete(sb.length()-1, sb.length());
          sb.delete(0, 1);
        }
      }
      // failure
      if (sb.length() == 0) {
        System.err.printf("\nUnable to simplify '%s'!\n", orig);
        releaseStringBuilder(sb);
        releaseStringBuilder(arrSb);
        return orig != null? orig: "NULL";
      }
      // primitive -> name
      if (sb.length() == 1) {
        char chr = sb.charAt(0);
        String stype = primitiveName(chr, sb, false, false);
        sb.delete(0, 1).insert(0, stype);
      }
      
      int slashPos;
      while ((slashPos = sb.indexOf("/")) != -1) {
        sb.replace(slashPos, slashPos+1, ".");
      }
      return sb.append(arrSb).toString();
    } finally {
      releaseStringBuilder(sb);
      releaseStringBuilder(arrSb);
    }
  }
  
  
  public static String primitiveName(char shorty, 
  CharSequence valueForNonPrimitive, boolean throwIfOther,
  boolean throwIfObject)
  {
    switch (shorty) {
      case 'B': return "byte";
      case 'C': return "char";
      case 'D': return "double";
      case 'F': return "float";
      case 'I': return "int";
      case 'J': return "long";
      case 'S': return "short";
      case 'Z': return "boolean";
      case 'V': return "void";
      case 'L': return (throwIfObject)
        ? (String) (Object) Reflector.Util.sneakyThrow(
            new IllegalArgumentException(String.format(
              "ClassInfo.primitiveName('%c', %s, %s, %s): "
              + "Object ('L') not allowed",
              shorty,
              (valueForNonPrimitive != null)
                ? String.format("\"%s\"", valueForNonPrimitive): null,
              (Boolean) throwIfOther, (Boolean) throwIfObject
            ))
          )
        : String.valueOf(valueForNonPrimitive);
      default: return (throwIfOther)
        ? (String) (Object) Reflector.Util.sneakyThrow(
            new IllegalArgumentException(String.format(
              "ClassInfo.primitiveName('%c', %s, %s, %s): "
              + "Invalid shorty/primitive type descriptor: '%c'",
              shorty,
              (valueForNonPrimitive != null)
                ? String.format("\"%s\"", valueForNonPrimitive): null,
              (Boolean) throwIfOther, (Boolean) throwIfObject,
              shorty
            ))
          )
        : String.valueOf(valueForNonPrimitive);
    }
  }
  
  static Field s_accfld;
  
  public static boolean enableAssertions(Class<?> targetClass, boolean en) {
    try {
      final Field fld = targetClass.getDeclaredField("$assertionsDisabled");
      fld.setAccessible(true);
      if ((fld.getModifiers() & Modifier.FINAL) != 0 && CollectionUtil.isJRE()) {
        final Field accfld;
        if (s_accfld != null) {
          accfld = s_accfld;
        } else {
          (accfld = (s_accfld = Field.class.getDeclaredField("modifiers"))
          ).setAccessible(true);
        }
        int oldacc = accfld.getInt(fld);
        int newacc = (oldacc & (~Modifier.FINAL));
        accfld.setInt(fld, newacc);
      }
      final Boolean oldFldValue = (Boolean) fld.get(null);
      final Boolean newFldValue = Boolean.valueOf(! en);
      fld.set(null, newFldValue);
      return ! ((oldFldValue == null) ? false : oldFldValue.booleanValue());
    } catch (ReflectiveOperationException | NullPointerException | Error t) {
      Throwable e = t;
      if (e instanceof InvocationTargetException) {
        e = ((InvocationTargetException) e).getTargetException();
      }
      Throwable rootCause = Reflector.getRootCause(e);
      throw new UnsupportedOperationException(String.format(
        "enableAssertions(targetClass: %s, en: %s) failed to set desired "
        + "class assertion status: %s%s%s",
        targetClass.getName(), Boolean.valueOf(en),
        e, (rootCause != null? format(" (cause: %s)", rootCause): ""),
        (e instanceof NoSuchFieldException ||
         rootCause instanceof NoSuchFieldException)
          ? format("; Available fields: \n  - %s\n", StringUtils.join(
              targetClass.getDeclaredFields(), "\n  - "))
          : ""
      ));
    }
  }
  
  static final String[] CANONICAL = {
    "boolean", "byte", "char", "double", "float", "int", "long", "short",
    "void"
  };
  
  static final char[] SHORTY = {
    'Z', 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'V'
  };
  
  static {
    for (String s: CANONICAL) s.intern();
    
    boolean aePrev = enableAssertions(ClassInfo.class, true);
    assert (CANONICAL.length == SHORTY.length)
      : "CANONICAL.length == SHORTY.length";
    assert (Arrays.binarySearch(CANONICAL, "void") >= 0)
      : "Arrays.binarySearch(CANONICAL, \"void\") >= 0";
    enableAssertions(ClassInfo.class, aePrev);
  }
  
  
  /**
  @throws `IllegalArgumentException' If `canonicalName' is an ('[]', etc.)
  */
  public static char primitiveShorty(String canonical, boolean ignoreAr) {
    final int bracketIdx = canonical.indexOf('[');
    if (bracketIdx != -1 && ! ignoreAr) throw new IllegalArgumentException(
      String.format("lookupShortyByName(canonical: \"%a\", ignoreAr: %s): "
        + "Array brackets must not be present unless ignoreAr == true",
        String.valueOf(canonical), Boolean.valueOf(ignoreAr)
    ));    
    final String lkupStr = ((bracketIdx != -1)
      ? String.valueOf(canonical.subSequence(0, bracketIdx))
      : canonical).intern();
    int index = Arrays.binarySearch(CANONICAL, 0, CANONICAL.length, lkupStr);
    if (index < 0) throw new IllegalArgumentException(
      String.format("lookupShortyByName(canonical: \"%s\", ignoreAr: %s): "
        + "Resolved canonical name (\"%s\") does not correspond to a "
        + "primitive type. The allowed values are: { \"%s\" }.",
        String.valueOf(canonical), Boolean.valueOf(ignoreAr),
        lkupStr, StringUtils.join(CANONICAL, "\", \"")
    ));
    return SHORTY[index];
  }
  
  public static String primitiveName(char shorty) {
    return primitiveName(shorty, null, true, true);
  }
  
  public static String primitiveToObject(char shorty) {
    String name = primitiveName(shorty, "object", true, false);
    StringBuilder sb = new StringBuilder(name.length() + 10);
    return sb.append("java.lang.")
             .append(name)
             .delete(10, 11)
             .insert(10, Character.toUpperCase(name.charAt(0)))
             .toString();
  }
  
  public static String[] typeToName(String[] types) {
    return typeToName(types, null);
  }
  
  public static 
  String[] typeToName(String[] types, String ifNull) 
  {
    if (types == null) return new String[0];
    String[] classNames = new String[types.length];
    int i = -1;
    String type;
    do {
      i += 1;
      try {
        classNames[i] = typeToName(types[i], ifNull);
      } catch (ArrayIndexOutOfBoundsException ex) {
        return classNames;
      } catch (NullPointerException ex) {
        classNames[i] = ifNull;
      }
    } while (true);
  }
  
  static void WARN(String s, Object... args) {
    System.err.printf("[WARN] ClassInfo: " + s, (Object[]) args);
    System.err.println();
  }
  
  public static String typeToName(Object obj) {
    if (obj == null) return "null";
    String typeName = "BADBADBAD";
    
    if (!(obj instanceof String)) {
      if (obj instanceof CharSequence) {
        try {
          if (((CharSequence) obj).length() > 255) {
            typeName = obj.getClass().getName();
          } else {
            try { 
              typeName = obj.toString();
            } catch (Exception e) { 
              typeName = obj.getClass().getName(); 
              WARN("<%s@0x%08x.toString() threw %s",
                obj.getClass().getName(), System.identityHashCode(obj),
                e.getClass().getSimpleName(), Debug.ToString(e));
            }
          }
        } finally {
        }
      } else {
        if (obj instanceof Class) {
          typeName = ((Class) obj).getName();
        } else {
          if (obj instanceof bsh.ClassIdentifier) {
            typeName
              = ((bsh.ClassIdentifier) obj).getTargetClass().getName();
          } else {
            typeName = obj.getClass().getName();
          }        
        }
      }
    } else {
      typeName = (String) obj;
    }
    return typeToName((String) typeName, "null");
  }
  
  static final LazyMember<Field> THROWABLE_DETAIL_MSG = LazyMember.of(
    "detailMessage", Throwable.class
  );
  
  public static String getMessage(Throwable ex) {
    if (ex == null) return "[ClassInfo.tryGetMessage: ex == null]";
    StringBuilder sb = new StringBuilder(76);    
    try {
      String msg = ex.getMessage();
      String exClsName;
      if (msg == null || msg.length() == 0) msg = ex.toString();
      
      if (msg.startsWith((exClsName = ex.getClass().getName()))) {
        int exNameEnd = exClsName.length();
        int msgLen = msg.length();        
        int remain = msgLen - exNameEnd;
        if (remain <= 0) return "";
        if (remain > 2) return sb.append(msg.subSequence(
          exNameEnd + ((msg.charAt(exNameEnd) == ':')? 2: 0),
          msgLen
        )).toString();
        return sb.append(
          (msg.charAt(exNameEnd) == ':')
            ? ""
            : msg.subSequence(exNameEnd, msgLen)
        ).toString();
      }
      return msg.toString();
    } catch (Exception e2) {
      CharSequence msg = THROWABLE_DETAIL_MSG.getValue(ex);
      if (msg == null) {
        Iterator<CharSequence> it = Reflect.searchObject(
          ex, CharSequence.class, false, 0, 2).iterator();
        msg = it.hasNext()? it.next(): "";
      }
      sb.append(format(
        "[%s <[WARN]: %s@%08x #getMessage() threw %s @ %s>]",
        msg,
        ex.getClass().getSimpleName(), System.identityHashCode(ex),
        e2.getClass().getSimpleName(), e2.getStackTrace()[0]
      ));
      return sb.toString();
    }
  }
  
  public static String backupTypeToName(String type) {
    String name = type.replace('/', '.');
    int semiPos;
    if ((semiPos = name.indexOf(';')) != -1 ) {
      name = name.substring(0, semiPos);
    }
    while (name.charAt(0) == '[') {
      name = name.substring(1).concat("[]");;
    }
    if (name.charAt(0) == 'L') name = name.substring(1);
    return name;
  }
  
  public static String classNameToPath(@Nonnull final String className,
  @Nullable final String optionalExt)
  {
    final StringBuilder sb = new StringBuilder(
      className.length()
      + ((optionalExt != null) ? (optionalExt.length() + 1) : 0)
    );
    sb.append(className);
    int idx;
    if (sb.charAt((idx = sb.length()-1)) == ';') {
      sb.delete(idx, idx+1);
      while (sb.charAt(0) == '[') sb.delete(0, 1);
      if (sb.charAt(0) == 'L') sb.delete(0, 1);
    }
    while ((idx = sb.indexOf(".")) != -1) {
      sb.replace(idx, idx + 1  , "/");
    }
    
    if (optionalExt == null || optionalExt.length() == 0) {
      return sb.toString();
    }
    
    if (optionalExt.charAt(0) != '.') sb.append('.');
    sb.append(optionalExt);
    return sb.toString();
  }
  
  public static String classNameToPath(@Nonnull final String className)
  {
    return classNameToPath(className, null);
  }
  
  public static String getClassName(@Nonnull final InputStream is)
    throws Exception
  {
    DataInputStream dis = new DataInputStream(is);
    dis.readLong(); // skip header and class version
    int cpcnt = (dis.readShort()&0xffff)-1;
    int[] classes = new int[cpcnt];
    String[] strings = new String[cpcnt];
    for(int i=0; i<cpcnt; i++) {
        int t = dis.read();
        if(t==7) classes[i] = dis.readShort()&0xffff;
        else if(t==1) strings[i] = dis.readUTF();
        else if(t==5 || t==6) { dis.readLong(); i++; }
        else if(t==8) dis.readShort();
        else dis.readInt();
    }
    dis.readShort(); // skip access flags
    return strings[classes[(dis.readShort()&0xffff)-1]-1]
      .replace('/', '.');
  }
  
  public static String getClassName(@Nonnull final byte[] classBytes)
  {
    try {
      final String className = getClassName(
        new ByteArrayInputStream(classBytes)
      );
      return className;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  };
  
  /*
  public static String getClassName(byte[] bytes) {
    DirectClassFile dcf = new DirectClassFile(bytes, "/dev/null", false);
    AttributeFactory attrFactory = new StdAttributeFactory();
    dcf.setAttributeFactory(attrFactory);
    try {
      dcf.parse();
    } catch (Exception e) {
      Reflector.Util.sneakyThrow(e);
    }
    return typeToName(dcf.getThisClass().getClassType().getClassName());
  }
  */
  
  public static SortedSet<String> getPrimitiveCanonicalNames() {
    if (PRIMITIVE_CANONICAL_NAMES == null) {
      final SortedSet<String> primitiveCanonicalNames = new TreeSet<>(); 
      primitiveCanonicalNames.addAll(DexlibAdapter.PrimitiveTypes.values()); 
      primitiveCanonicalNames.remove("java.lang.Object");
      PRIMITIVE_CANONICAL_NAMES
        = Collections.unmodifiableSortedSet(primitiveCanonicalNames);
    }
    return PRIMITIVE_CANONICAL_NAMES;
  }
  
  public static SortedSet<Character> getPrimitiveShortyCharacters() {
    if (PRIMITIVE_SHORTY_CHARACTERS == null) {
      final SortedSet<Character> primitiveShortyChars = new TreeSet<>(); 
      primitiveShortyChars.addAll(DexlibAdapter.PrimitiveTypes.keySet()); 
      primitiveShortyChars.remove(Character.valueOf('L'));
      PRIMITIVE_SHORTY_CHARACTERS
        = Collections.unmodifiableSortedSet(primitiveShortyChars);
    }
    return PRIMITIVE_SHORTY_CHARACTERS;
  }
  
  public static SortedSet<String> getPrimitiveShortyStrings() {
    if (PRIMITIVE_SHORTY_STRINGS == null) {
      final SortedSet<String> primitiveShortyStrings = new TreeSet<>(); 
      for (final Character boxedChr: DexlibAdapter.PrimitiveTypes.keySet()) {
        final char ch = boxedChr.charValue();
        if (ch == 'L') continue;
        final String str = String.valueOf((char) ch);
        primitiveShortyStrings.add(str);
      }
      PRIMITIVE_SHORTY_STRINGS
        = Collections.unmodifiableSortedSet(primitiveShortyStrings);
    }
    return PRIMITIVE_SHORTY_STRINGS;
  }
  
  public static List<Field> getFieldsByType(Class<?> declaring,
  Class<?> fieldType, boolean recurse)
  {
    Class<?> c = declaring;
    List<Field> ret = new ArrayList<>();
    String className;
    do {
      Field[] fds;
      className = c.getName();
      try {
        fds = c.getDeclaredFields();
        for (int i=0, len=fds.length; i<len; ++i) {
          Field fld = fds[i];
          if (! fieldType.isAssignableFrom(fld.getType())) continue;
          ret.add(fld);
        }
        declaring = declaring.getSuperclass();
        if (false) throw new ReflectiveOperationException();        
      } catch (LinkageError | ReflectiveOperationException clEx) {
        RuntimeException rex;
        errors.add(rex = new RuntimeException(String.format(
          "Failure loading fields from class %s: %s", className, clEx
        ), clEx));
        rex.printStackTrace();
        if (declaring != null) declaring = declaring.getSuperclass();
      }     
    } while (recurse && declaring != Object.class && declaring != null);
    return ret;
  }
  
  public static Class<?> getRootComponentType(final Class<?> c) {
    Class maybeArrayClass = c;
    while (maybeArrayClass != null && maybeArrayClass.isArray()) {
      maybeArrayClass = maybeArrayClass.getComponentType();
    }
    return maybeArrayClass;
  }
  
  public static SortedSet<String> getReferencedClassNames(final Class<?> c) {
    if (c == null) return new TreeSet<>();
    final Class<?> cls = getRootComponentType(c);
    final com.sun.tools.classfile.ConstantPool constPool
      = TypeTools.getConstantPool(cls);
    final CONSTANT_Class_info[] clsItems
      = typeFilter(TypeTools.pool(constPool), CONSTANT_Class_info.class);
    final SortedSet<String> names = new TreeSet<>();
    for (CONSTANT_Class_info clsItem: clsItems) {
      final String binaryName, name;
      try {
        binaryName = clsItem.getName();
      } catch (com.sun.tools.classfile.ConstantPoolException cpex) {
        throw Reflector.Util.sneakyThrow(cpex);
      }
      if (binaryName.charAt(0) == '[') {        
        final int lastBracketPos = binaryName.lastIndexOf('[');
        final char firstTypeChar = binaryName.charAt(lastBracketPos+1);
        if (firstTypeChar != 'L') continue;
        // +2 on 1st (starting) index accounts for skipping the last '['
        //    AND the leading 'L' on refarr "type part."
        // -1 on 2nd (ending) index accounts for trailing ';'
        //    on refarr "type part."
        name = binaryName.substring(lastBracketPos+2, binaryName.length()-1)
                         .replace('/', '.');
      } else {
        name = binaryName.replace('/', '.');
      }
      names.add(name);
    }
    return names;
  }
  
  /**
  @param className - The primary class name to import (along with its
      primary class dependencies).
      
      The format of `className' matches what is returned by
      calling `java.lang.Class.getName()' on the `Class' instance.
  
  @returns byte[] - A '.jar' file, in byte array form, containing entries
      for the primary class (the class specified by `className'), as well as
      entries for each primary (direct) class dependency thereof.
  ;
  */
  static final Set<String> missing = new HashSet<>();
  
  public static byte[] importDexClasses(/*;*/ String className,
  boolean includeDependencies, boolean addToClassPath)
  {
    /*Log.d(TAG, "importDexClasses(className: %s, includeDependencies: %s, " +
      "addToClassPath: %s)", className, includeDependencies, addToClassPath);
    */
    if (missing.contains(className)) return null;
    
    Class<?> cls = DexVisitor.classForName(className);
    StringBuilder sb = (cls != null)? null: new StringBuilder(className);
    int lastDot = className.length() + 1;
    URL cres;
    {
      String altClassName = className;
      while ((cres = getClassResource(altClassName)) == null && 
        ((lastDot = altClassName.lastIndexOf('.', lastDot-1)) != -1))
      {
        if (lastDot == -1) break;
        sb.replace(lastDot, lastDot+1, "$");
        altClassName = sb.toString();
      
        if (cres != null) {
          Log.e(
            TAG, "WARNING: className was inaccurate: \"%s\", " +
            "should be \"%s\"",className, altClassName
          );
          className = altClassName;
          break;
        }
      }
    }
    
    byte[] jarBytes = null;
    final byte[] dexBytes = 
      (cls != null)
        ? getDex(className).getBytes()
        : getDexBytesFromClassNameFallback(className);
    if (dexBytes == null) {
      missing.add(className);
      return null;
    }
    System.err.printf(
      "ClassInfo: [%s]: dexBytes := %s: from %s\n",
      className,
      (dexBytes != null)? String.format("byte[%d]", dexBytes.length): "null",
      (cls != null)
        ? String.format("getDex(<%s>).getBytes()", cls)
        : String.format(
            "getDexBytesFromClassNameFallback('%s')", className)
    );
    
    //if (dexBytes == null && cls == null) return null;
    URL inputFilePath = getClassResource(className);
    
    if (inputFilePath == null) {
      missing.add(className);
      return null;
    }
    
    try {
      if ((! includeDependencies || cls == null) && dexBytes != null) {
        final byte[] selDexBytes = DexRemix.remixDex(
          DexRemix.getDexBackedDexFile(new ByteArrayInputStream(dexBytes)),
          Pattern.compile(
            "^L" + ClassInfo.typeToName(className).replace("$", ".") + ";"
          ).matcher("")
        );
        jarBytes = Dex2Java2.dex2jar(selDexBytes);
      } else {
        jarBytes = Dex2Java2.dex2jarWithClassDependencies(cls);
      }
      if (addToClassPath) {
        ZipByteArrayClassPath zbacp = null;        
        CollectionUtil.getClassPool().insertClassPath(
          (zbacp = new ZipByteArrayClassPath(jarBytes))
        );
      }
    } catch (Exception e) {
      String message = String.format(
        "[ERROR] ClassInfo.importDexClasses(className: \"%s\"): " +
        "Could not convert and/or import the class '%s': %s\n",
        className, className, e
      );
      throw new RuntimeException(message, e);
    }
    
    if (jarBytes == null) {
      throw new IllegalStateException(String.format(
        "importDexClasses(className: \"%s\", includeDependencies: %s, "
        + "addToClassPath: %s): jarBytes == null", //;
        className, Boolean.valueOf(includeDependencies),
        Boolean.valueOf(addToClassPath)   
      ));
    }
    return jarBytes;
  }
  
  public static byte[] getDexBytesFromClassNameFallback(String className) {
    Log.w(
      TAG, "getDexBytesFromClassNameFallback(%s) called", str(className)
    );
    final Map<String, String[]> classpathMap = ClassPathUtil.mapClassPath();
    final Set<? extends Map.Entry<String, String[]>> entries
      = classpathMap.entrySet();
    for (final Map.Entry<String, String[]> entry: entries) {
      final String fileName;
      final String[] classNames = entry.getValue();      
      if (ArrayUtils.indexOf(classNames, className) == -1) continue;
      System.err.printf("className = %s, entry.getKey() = %s\n", 
        className, entry.getKey());
      final File file = new File(fileName = entry.getKey());
      final byte[] dexBytes;
      try {
        dexBytes = FileUtils.readFileToByteArray(file);
        byte[] actualDexBytes = (dexBytes.length > 10 && 
           (dexBytes[0] == (byte)'P' || dexBytes[0] == (byte)'d'))
             ? unwrapDexBytesIfInsideZip(dexBytes)
             : dexBytes;
        DexUtil du = new DexUtil(file);
        if (!ArrayUtils.contains(du.getClassNames(), className)) {
          Log.w(TAG, "False positive for %s\n", fileName);
          continue;
        }
        if (actualDexBytes != null && 
            actualDexBytes.length >= 32)
        {
          return actualDexBytes;
        }
      } catch (IOException ioe) {
        Log.w(TAG, new Error(String.format(
          "getDexBytesFromClassNameFallback(\"%s\") encountered error: %s",
          str(className), ioe
        )));
      }
    }
    return null;    
  }
  
  public static byte[] unwrapDexBytesIfInsideZip(byte[] dexOrZipBytes) {
    return (dexOrZipBytes[0] == (byte) 'P')
      ? ZipUtil.toByteArray(dexOrZipBytes, "classes.dex")
      : dexOrZipBytes;
  }
  
  
  public static <W> W castWrapper(Object boxed, Class<W> boxedTargetType) {
    if (boxed == null) return (W) null;
       
    Class<?> boxedSourceType = boxed.getClass();
    return (W) castWrapper(
      ClassUtils.wrapperToPrimitive(boxedTargetType), boxed
    );
  }
  
  private static Object castWrapper(Class<?> toType, Object value) {
    if (toType == null) throw new IllegalArgumentException("toType == null");
    if (value == null)  throw new IllegalArgumentException("value == null" );
    if (!toType.isPrimitive()) {
      if (! ClassUtils.isPrimitiveOrWrapper(toType)) {
        throw new IllegalArgumentException(String.format(
          "Invalid type in castWrapper; not primitive: %s", toType.getName()
        ));
      } else {
        toType = ClassUtils.wrapperToPrimitive(toType);
      }
    }
    
    if (value instanceof Boolean) {
      if (toType != Boolean.TYPE) {
        value = Integer.valueOf(((Boolean) value).booleanValue()? 1: 0);
      } else return value;
    }
    if (value instanceof Character) value = Integer.valueOf((char) value);
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException(String.format(
        "Invalid type in castWrapper; expected toType <: Number; got: %s",
        toType.getSimpleName()
      ));
    }
    final Number number = (Number) value;
    if (toType == Byte.TYPE) return Byte.valueOf(number.byteValue());
    if (toType == Short.TYPE) return Short.valueOf(number.shortValue());
    if (toType == Character.TYPE) {
      return Character.valueOf((char) number.intValue());
    }
    if (toType == Integer.TYPE) return Integer.valueOf(number.intValue());
    if (toType == Long.TYPE) return Long.valueOf(number.longValue());
    if (toType == Float.TYPE) return Float.valueOf(number.floatValue());
    if (toType == Double.TYPE) return Double.valueOf(number.doubleValue());
    if (toType == Boolean.TYPE) {
      return Boolean.valueOf(number.intValue() != 0);
    }
    throw new IllegalArgumentException(String.format(
      "castWrapper: unexpected toType: %s", toType.getSimpleName()
    ));
  }
  
  
  public static List<Class<?>> descListToClasses(final List<String> descs,
  final @Nullable ClassLoader loader)
  {
    final List<Class<?>> classes = new ArrayList<>();
    for (int i=0, len=descs.size(); i<len; ++i) {
      final String typeDesc = descs.get(i);
      final Class<?> typeCls;
      try {
        typeCls = InternalNames.getClass(
          ((loader != null)
            ? (ClassLoader) loader
            : (ClassLoader) Thread.currentThread().getContextClassLoader()
          ),
          typeDesc
        );
        classes.add(typeCls);
      } catch (Exception e) {
        new RuntimeException(String.format(
          "Could not resolve descriptor to class: \"%s\"", typeDesc
        ), e).printStackTrace();
      }
    }
    return classes;
  }
  
  
  
  
  public static StringBuilder getModifiers(int acc) {
    final StringBuilder sb = new StringBuilder(48).append(Modifier.toString(
      acc & ~(INTERFACE) & ((acc & INTERFACE) != 0? ~(ABSTRACT): 0xFFFFFFFF)
    ));
    if (sb.length() > 0) sb.append(' ');
    if ((acc & ANNOTATION) != 0) sb.append('@');
    return sb.append(
      ((acc & INTERFACE) != 0)
        ? "interface" : (((acc & ENUM) != 0)? "enum": "class")
    ).append(" ");
  }
  
  public static StringBuilder getModifiers(Object clzRef) {
    if (clzRef instanceof Integer) {
      return getModifiers((int) ((Integer) clzRef).intValue());
    }
    if (clzRef == null) return new StringBuilder(0);
    Class<?> cls = dumpMembers.getClass(clzRef);
    return getModifiers(
      (int) ((cls != null)? cls: clzRef.getClass()).getModifiers()
    );
  }
  
  public static Object parseTypeSignature(final Collection<String> sig) {
    Matcher TYPE_MCHR = Pattern.compile("^[a-zA-Z0-9_$.]*;?$").matcher(""); 
    Deque<String> generics = new ArrayDeque<>(),
                     types = new ArrayDeque<>();
    final List<String> parts = new ArrayList<>(sig);
    for(final ListIterator<String> it = parts.listIterator(); it.hasNext();)
    {
      int index = it.nextIndex(); 
      String part = it.next();
      
      int lastBracket = part.lastIndexOf('[');
      char schar = part.charAt(lastBracket+1);
      if ((schar != 'L' || part.indexOf(';') != -1) && 
          TYPE_MCHR.reset(part).matches())
      { 
       types.offerLast(ClassInfo.typeToName(part));
       System.err.printf(
         "enqueued regular type; %s\n", ClassInfo.typeToName(part)
       );
       it.remove();
       continue;
     }
     if (part.startsWith("<")) {
       String argsFor = generics.peekLast();
       types.offerLast(part);
       System.err.printf(
         "Entered generic arguments context for %s  (%s)\n",
         ClassInfo.typeToName(argsFor), part
       );
       it.remove();
       continue;
     }
     
     final String type;
     
     if (part.startsWith(">")) { 
       String argsFor = generics.pollLast().concat(";");
       String typeArg;
       List<String> typeArgs = new LinkedList<>();
       while (! (typeArg = types.pollLast()).startsWith("<")) {
         typeArgs.add(typeArgs.size(), typeArg);
       }
       System.err.printf(
         "Exited generic arguments context for %s  (%s)\n",
         ClassInfo.typeToName(argsFor), part
       );
       type = String.format(
         typeArgs.isEmpty()? "%s": "%s<%s>",
         ClassInfo.typeToName(argsFor), StringUtils.join(
           ClassInfo.typeToName(typeArgs.toArray(new String[0])), ", ")
       );
       System.err.printf("  completed generic type: %s\n", type);
       it.remove();
       continue;
     } else {
       type = String.format("%s;", part);
     }
     
     System.err.printf(
       "Enqueuing raw/generic base type: %s  (%s)\n",
       type, ClassInfo.typeToName(part)
     );
     generics.offerLast(type);
     it.remove();
     continue;
    }
    
    return Pair.of(generics, types);
  }
  
  static Map<String, byte[]> cache = new HashMap<>();
  static Map<String, CtClass> ctcache = new HashMap<>();
  
  
  static final LazyMember<Constructor<? extends CtClass>> CTNEWCLASS_CTOR
    = LazyMember.of(
        "javassist.CtNewClass", "<init>",
        String.class, ClassPool.class, Boolean.TYPE, CtClass.class
      );
  
  public static byte[] generateClass(String className, String superClassName, 
    String[] interfaceNames, boolean isInterface)
  {
    if (generateTypes == false) return null;
    if (cache.containsKey(ClassInfo.typeToName(className))) {
      return cache.get(ClassInfo.typeToName(className));
    }
    if (ALLOW_GEN_MISSING == false) {
      throw new IllegalActionError(String.format(
        "generateClass(className: %s, superClassName: %s, interfaceNames: %s, " +
        "isInterface: %s)",
        str(className), str(superClassName), str(interfaceNames), isInterface
      ));
    }
    try {
      final ClassPool cp = CollectionUtil.getClassPool();
      final CtClass superCtClass;
      if (DexVisitor.classForName(superClassName) != null) {
        superCtClass = cp.get(superClassName);
      } else {
        superCtClass = new CtClassType2(
          new ByteArrayInputStream(
            generateClass(
              superClassName, cp.get("java.lang.Object"), new String[0], false
            )
          ),
          cp
        );
      }
      return generateClass(
        className, superCtClass, 
        interfaceNames, isInterface
      );
    } catch (Throwable e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  

  
  public static CtClass makeNestedClass(CtClass declaring, String innerName,
    boolean isStatic)
  {
    if (generateTypes == false) return null;
    Reflect.setfldval(declaring, "wasFrozen", false);
    final String name
      = String.format(
          "%s%c%s",
          declaring.getClassFile().getName(),
          '$',
          innerName
        );
    CtClass ct = ctcache.get(name);
    if (ct == null) {
      try {
        ct = declaring.makeNestedClass(innerName, isStatic);
      } catch (RuntimeException e) {
        try {
          ct = CollectionUtil.getClassPool().get(name);
        } catch (Exception e2) {
          throw Reflector.Util.sneakyThrow(e2);
        }
      }
      if (ct != null) ctcache.put(name, ct);
    }
    Reflect.setfldval(ct, "wasFrozen", false);
    return ct;
  }
  
  public static byte[] generateClass(String className, CtClass superCtClass, 
    String[] interfaceNames, boolean isInterface)
  {
    if (generateTypes == false) return null;
    className = className.replace('/', '.');
    
    Log.d(
      TAG, "generateClass(%s, super: (%s) %s, ifaces: %s, iface: %s)",
      className, getSimpleName(superCtClass), superCtClass,
      Arrays.asList(interfaceNames), isInterface
    );
    
    if (cache.containsKey(ClassInfo.typeToName(className))) {
      Log.d(
        TAG, "Returning cached type: %s",
        cache.get(ClassInfo.typeToName(className))
      );
      return cache.get(ClassInfo.typeToName(className));
    }
    
    String internalName = className.replace('.', '/');
    String[] parts = internalName.split("$");
    
    
    Deque<String> q = new ArrayDeque<>();
    q.offer(internalName);
    Log.d(TAG, "q = %s", q);
    Deque<String> nested = new ArrayDeque<>();
    Collections.addAll(nested, Arrays.copyOfRange(parts, 1, parts.length));
    Log.d(TAG, "nested = %s", nested);
    try {
      
      final ClassPool cp = CollectionUtil.getClassPool();
      Log.d(TAG, "cp = %s", cp);
      
      Deque<CtClass> ctq = new ArrayDeque<>();
      boolean isStatic = true;
      List<CtClass> finished = new ArrayList<>();
      CtClass last = null;
      byte[] lastBytes = null;
        
      while (true) {
        Log.d(TAG, "q = %s", q);
        if (q.isEmpty()) {
          Log.d(TAG, "loop stopping");
          break;
        } else {
          String name = q.poll();
          Log.d(TAG, "name = %s", name);
          String dcn = StringUtils.substringBefore(name, "$");
          String rest = StringUtils.substringAfter(name, "$");
          Log.d(TAG, "dcn = %s", dcn);
          Log.d(TAG, "rest = %s", rest);
          if (rest.length() > 0)
            Collections.addAll(nested, StringUtils.split(rest, "$"));
          Log.d(TAG, "nested = %s", nested);
          final CtClassType2 ct = new CtNewClass2(
            ClassInfo.typeToName(dcn), // String name
            CollectionUtil.getClassPool(), // ClassPool pool
            isInterface, // boolean isInterface
            superCtClass // CtClass superClass
          );
          for (final String ifaceName: ClassInfo.typeToName(interfaceNames)) {
            if (DexVisitor.classForName(ifaceName) != null) {
              final CtClass ifaceCtClass = cp.get(ifaceName);
              ct.addInterface(ifaceCtClass);
            } else {
              CtClass ifaceCt = new CtClassType2(
                new ByteArrayInputStream(
                  generateClass(
                    ifaceName, cp.get("java.lang.Object"), new String[0], true
                  )
                ),
                cp
              );
              ct.addInterface(ifaceCt);
            }
          }
          ctq.offer(ct);
          finished.add(ct);
          while (!nested.isEmpty()) {
            Log.d(TAG, "ctq = %s, nested = %s", ctq, nested);
            CtClass declaring = ctq.poll();
            Log.d(TAG, "declaring = %s", declaring);
            String innerName = nested.poll();
            Log.d(TAG, "innerName = %s", innerName);
            CtClass nestedCt = makeNestedClass(declaring, innerName, isStatic);
            Log.d(TAG, "nestedCt = %s", nestedCt);
            finished.add(nestedCt);
            Log.d(TAG, "finished = %s", finished);
            ctq.offer(nestedCt);
            Log.d(TAG, "ctq = %s, nested = %s", ctq, nested);
          }
        }
        Log.d(TAG, "end of nested loop", new Object[0]);
        Log.d(TAG, "finished = %s", finished);
        Collections.reverse(finished);
        Log.d(TAG, "finished = %s", finished);
        
        for (CtClass ct: finished) {
          Log.d(TAG, "ct = %s", ct);
          className = ct.getName();
          Log.d(TAG, "className = %s", className);
          byte[] classBytes;
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try (final OutputStream csos = new CloseShieldOutputStream(baos);
               final DataOutputStream dos = new DataOutputStream(csos))
          {
            if (false) throw new NotFoundException("");
            ct.toBytecode(dos);
            classBytes = baos.toByteArray();
            Log.d(TAG, "classBytes = %s", classBytes);
            cache.put(className, classBytes);
            String realClassName = getClassName(classBytes);
            ProcyonUtil.addTypeDefinition(
              ProcyonUtil.getTypeDefinition(classBytes)
            );
            lastBytes = classBytes;
            Log.d(TAG, "finished classBytes: %s (name: %s)", classBytes,
              realClassName);
            Log.d(TAG, "lastBytes = %s", lastBytes);
          } catch (IOException ioe) {
            ioe.printStackTrace();
            throw Reflector.Util.sneakyThrow(ioe);
          } catch (NotFoundException jvstNfe) {
            jvstNfe.printStackTrace();
            if (!Object.class.getName().equals(
                ClassInfo.typeToName(superCtClass.getName())))
            {
              classBytes = generateClass(
                className, Object.class.getName(), new String[0], isInterface
              );
              Log.d(TAG, "classBytes = %s", classBytes);
              cache.put(className, classBytes);
              ProcyonUtil.addTypeDefinition(
                new MetadataSystem(
                  new ArrayTypeLoader(classBytes)
                ).lookupType(className.replace('.', '/')).resolve()
              );
              lastBytes = classBytes;
              Log.d(TAG, "finished classBytes: %s", classBytes);
              Log.d(TAG, "lastBytes = %s", lastBytes);
            }
            else throw Reflector.Util.sneakyThrow(jvstNfe);
          }
          Log.d(TAG, "finished ct: %s; classBytes = %s", ct, classBytes);
        } // for (ct: finished)
      }
      return lastBytes;
    } catch (Throwable e) {
      e.printStackTrace();
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  
  public static class CLoader implements Iterable<URL> {
    final ClassLoader ldr;
    final String name;
    public CLoader(final ClassLoader ldr, String name) {
      this.ldr = ldr;
      this.name = name;
    }    
    @Override
    public Iterator<URL> iterator() {
      if (ldr == null) return Collections.<URL>emptyList().iterator();
      try {
        return CollectionUtil.asIterable(ldr.getResources(name))
          .iterator();
      } catch (IOException ioe) {
        return Collections.<URL>emptyList().iterator();
      }
    }
  }
  
  public static class SunLoader implements Iterable<URL> {
    
    static final com.google.common.base.Predicate<URL> NOT_BOGUS_PREDICATE =
      new com.google.common.base.Predicate<URL>() {
        @Override
        public boolean apply(final URL url) {
          final String urlStr = url.toString();
          final int fragSepAt = urlStr.indexOf("!/");
          final int fragSepAt2 = urlStr.indexOf('#');
          final int fragAt = (fragSepAt != -1)
            ? (fragSepAt2 != -1)
                ? Math.min(fragSepAt, fragSepAt2)
                : fragSepAt2
            : fragSepAt;
          final String adjustedPath = (fragAt != -1)
             ? (String) urlStr.subSequence(0, fragAt)
             : urlStr;
          for (final String suffix: BOGUS_SUFFIXES) {
            if (adjustedPath.endsWith(suffix)) {
              return false; // ==> bogus
            }
          }
          return true;
        }
      };
    
    static final LazyMember<Method> UCP_FIND_RES = LazyMember.of(
      "sun.misc.Launcher$BootClassPathHolder", "findResources",
      new Class[]{ String.class, Boolean.TYPE }
    );
    final Object ldr;
    final String name;
    public SunLoader(final Object ldr, String name) {
      this.ldr = ldr;
      this.name = name;
    }
    @Override
    public Iterator<URL> iterator() {
      if (name == null) {
        return Iterables.filter(
          Arrays.<URL>asList(((URLClassPath) (
          (ldr instanceof URLClassPath)
            ? (URLClassPath) ldr
            : (ldr instanceof ClassLoader)
                ? Reflect.<URLClassPath>getfldval(ldr, "ucp")
                : Reflect.<URLClassPath>getfldval(
                    DexVisitor.classForName(
                      "sun.misc.Launcher$BootClassPathHolder"
                    ), "bcp")
          )).getURLs()), 
          NOT_BOGUS_PREDICATE
        ).iterator();
      } else {
        return Iterables.filter(
          EnumerationUtils.<URL>toList(((URLClassPath) (
            (ldr instanceof URLClassPath)
              ? (URLClassPath) ldr
              : (ldr instanceof ClassLoader)
                  ? Reflect.<URLClassPath>getfldval(ldr, "ucp")
                  : Reflect.<URLClassPath>getfldval(
                      DexVisitor.classForName(
                        "sun.misc.Launcher$BootClassPathHolder"
                      ), "bcp")
          )).findResources(name, true)),
          NOT_BOGUS_PREDICATE
        ).iterator();
      }
    }
  }
  

  static Set<String> none = new HashSet<>();
  static Object bootstrapLoader;
  
  @Nullable
  public static final Pair<String, Dex> findDexInClasspathBruteForce(
    final String className)
  {
    if (className == null || none.contains(className) ||
        className.indexOf('-') != -1 ||
        className.indexOf('$') == 0)
    {
      return null;
    }
    
    if (BRUTE_FORCE_DISABLED) {
      Log.w(TAG, "Brute force dex search is disabled (className: %s).",
      className);
      return null;
    }
    String path = "classes.dex";
    
    Log.d(TAG, "findDexInClasspathBruteForce(\"%s\")", className);
    final Iterable<URL> urls = Iterables.<URL>concat(
      (Iterable<URL>[]) (Object) (
        new Iterable<?>[] {
          new CLoader(Thread.currentThread().getContextClassLoader(), path),
          new CLoader(
            Thread.currentThread().getContextClassLoader().getParent(),
            path),
          new SunLoader(
            (bootstrapLoader != null)
              ? bootstrapLoader
              : (bootstrapLoader =
                  Reflect.getfldval(DexVisitor.classForName(
                    "sun.misc.Launcher$BootClassPathHolder"), "bcp")), path)
        }
      )
    );
    
    
    try {
        for (final URL url: urls) {
          final URLConnection conn = url.openConnection();
          try (final InputStream in = conn.getInputStream()) {
            final Dex dex = new Dex(in);
            final DexUtil du = new DexUtil(dex);
            if (ArrayUtils.indexOf(du.getClassNames(), className) != -1) {
              System.err.printf("returning %s, %s\n", url, className);
              return Pair.of(url.toString(), dex);
            }
          } catch (IOException ioex) {
            Log.w(TAG, ioex);
            continue;
          } catch (Throwable t) {
            t.printStackTrace();
            continue;
          }
          if (!(conn instanceof JarURLConnection)) continue;
          final JarURLConnection jconn = (JarURLConnection) conn;
          final ZipFile zf = jconn.getJarFile();
          System.err.printf("looking in ZipFile: %s\n", zf.getName());
          String entryName;
          ZipEntry ze;
          for (int dexEntryNumber = 2;
              (ze = zf.getEntry((entryName = String.format(
                "classes%d.dex", dexEntryNumber)))) != null;
              ++ dexEntryNumber)
          {
            try (final InputStream in = zf.getInputStream(ze)) {
              final Dex dex = new Dex(in);
              final DexUtil du = new DexUtil(dex);
              if (ArrayUtils.indexOf(du.getClassNames(), className) != -1) {
                Log.d(TAG, "Found class '%s' in secondary dex: '%s' of jar '%s'",
                  className, entryName, zf.getName());
                System.err.printf("returning %s, %s\n", 
                  zf.getName(), className);
                return Pair.of(
                  String.format("jar:file://%s!/%s", zf.getName(), entryName), dex
                );
              }
            } catch (IOException ioex) {
              Log.w(TAG, ioex);
              continue;
            } catch (Throwable t) {
              t.printStackTrace();
              continue;
            }
          }
        }
    } catch (final IOException ioe) {
      ioe.printStackTrace();
      throw Reflector.Util.sneakyThrow(ioe);
    }
    Log.w(TAG, "findDexBruteForce returning null for \"%s\"",
      StringEscapeUtils.escapeJava(className));
    none.add(className);
    return null;
  }
  
  public static URI toURI(final Object obj) {
    if (obj instanceof URI) return (URI) obj;
    try {
      if (obj instanceof URL) {
        return ((URL) obj).toURI();
      }
      if (obj instanceof File) {
        return ((File) obj).toURI();
      }
      final String path;
      if (obj instanceof Path) {
        final Path p = (Path) obj;
        path = (p.isAbsolute()
          ? Paths.get("//", StringUtils.join(p, File.separator))
          : Paths.get(".", p.toString())).toString();
      } else if (obj instanceof String) {
        path = PathInfo.getPathInfo((String) obj).path;
      } else {
        path = PathInfo.getPathInfo(obj.toString()).path;
      }
      final URI uri = new File(path).exists()
        ? new File(path).getCanonicalFile().getAbsoluteFile().toURI()
        : new URI(String.format("file:%s", path));
      return uri;
    } catch (final URISyntaxException | MalformedURLException e) {
      throw (RuntimeException) Reflector.Util.sneakyThrow(e).initCause(
        new  IllegalArgumentException(String.format(
          "Badly-formatted URL: \"%s\"",
          StringEscapeUtils.escapeJava(((URL) obj).toString())
        ))
      );
    } catch (final IOException e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static Dex android_getDex(final Class<?> cls) {
    try {
      return (Dex) 
        Class.class.getDeclaredMethod("getDex").invoke(cls);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
  
  @Extension
  public static Dex getDex(final Class<?> cls) {
    if (cls == null) throw new IllegalArgumentException("cls == null");
    if (! JRE) return android_getDex(cls);
    return getDex(cls, (URL) null);
  }
  
  @Extension
  public static Dex getDex(final Class<?> cls, @Nullable URL location) {
    if (cls == null) throw new IllegalArgumentException("cls == null");
    if (! JRE) return android_getDex(cls);
    
    final URI locationUri;
    final URL locationUrl;
    if (location != null) {
      final URI uri = toURI(location);
      if ("file".equals(uri.getScheme())) {
        locationUrl = location;
        locationUri = uri;
      } else {
        locationUri = toURI(locationUrl = getLocation(cls));
      }
    } else {
      locationUri = toURI(locationUrl = getLocation(cls));
    }
    
    return getDex(cls.getName(), locationUrl);
  }
  
  public static Dex getDex(final String className) {
    if (className == null) throw new IllegalArgumentException("className == null");
    try {
      final Class<?> cls = Class.forName(
        className, false, Thread.currentThread().getContextClassLoader()
      );
      if (!JRE) return android_getDex(cls);
      return getDex(className, getLocation(cls));
    } catch (final ClassNotFoundException | Error e) {
      if (!(e instanceof ClassNotFoundException || 
            e instanceof NoClassDefFoundError ||
            e instanceof ReflectiveOperationException))
      {
        e.printStackTrace();
      }
      return getDex(className, (URL) null);
    }
  }
  
  
  public static int getDexClassDefIndex(Dex dex, String className) {
    final String desc = String.format("L%s;", className.replace('.', '/'));
    final int typeIndex = dex.findTypeIndex(desc);
    try {
      final int classDefIndex = (typeIndex >= 0)
        ? dex.findClassDefIndexFromTypeIndex(typeIndex)
        : -1;
      return classDefIndex >= 0?classDefIndex: -1;
    } catch (final IndexOutOfBoundsException ex) {
      return -1;
    }
  }
  
  @Extension
  public static int getDexClassDefIndex(Class<?> cls) {
    Dex dex = getDex(cls);
    return getDexClassDefIndex(dex, cls.getName());
  }
  
  @Extension
  public static ClassDef getClassDef(Class<?> cls) {
    Dex dex = getDex(cls);
    int classDefIndex = getDexClassDefIndex(dex, cls.getName());
    ClassDef classDef = SourceUtil.getClassDef(dex, classDefIndex);
    return classDef;
  } 
  
  public static ClassDef getClassDef(String className) {
    Dex dex = getDex(className);
    int classDefIndex = getDexClassDefIndex(dex, className);
    ClassDef classDef = SourceUtil.getClassDef(dex, classDefIndex);
    return classDef;
  }
  
  
  public static MethodId getMethodId(Dex dex, int methodIndex) {
    TableOfContents toc = dex.getTableOfContents();
    TableOfContents.Section tocsec = toc.methodIds;
    Dex.Section sec = dex.open(tocsec.off);
    
    int initial = sec.getPosition();
    MethodId mid = sec.readMethodId();
    int pos = sec.getPosition();
    if (methodIndex == 0) return mid;
    int size = pos - initial;
    Reflect.<ByteBuffer>getfldval(
      dex, "data"
    ).position(initial + (methodIndex*size));
    return sec.readMethodId();
  }
  
  
  public static <T> T getItem(Dex dex, Class<T> itemCls, int itemIndex) {
    TableOfContents toc = dex.getTableOfContents();
    String name = itemCls.getSimpleName();
    String lcItemsName
      = name.substring(0, 1).toLowerCase().concat(name.substring(1)).concat("s");
    try {
      for (final Field fld: TableOfContents.class.getDeclaredFields()) {
        if (! fld.getName().equals(lcItemsName)) continue;
        TableOfContents.Section tocsec = (TableOfContents.Section) fld.get(toc); 
        Dex.Section sec = dex.open(tocsec.off);
        Object item = Reflector.invokeOrDefault(sec, String.format("read%s",name));
        int pos = sec.getPosition();
        if (itemIndex == 0) return (T) item;
        int size = pos - tocsec.off;
        ByteBuffer data = Reflect.getfldval(sec, "data"); 
        data.position(tocsec.off + (itemIndex * size));
        return (T) Reflector.invokeOrDefault(sec, String.format("read%s", name));
      }
      throw new RuntimeException(String.format(
        "Not found: field %s.%s", toc.getClass().getName(), lcItemsName
      ));
    } catch (ReflectiveOperationException e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static Code getCode(Member mtd) {
    Dex dex = getDex(mtd.getDeclaringClass());
    int methodIndex = JavaDoc.getDexMethodIndex(mtd);
    MethodId mid = dex.methodIds().get(methodIndex);
    int typeIndex = mid.getDeclaringClassIndex();
    int classDefIndex = dex.findClassDefIndexFromTypeIndex(typeIndex);
    ClassDef classDef = SourceUtil.getClassDef(dex, classDefIndex);
    int cdatoff = classDef.getClassDataOffset();
    ClassData cdat = openAt(dex, cdatoff).readClassData();
    ClassData.Method cdm = null;
    for (ClassData.Method _cdm: cdat.allMethods()) { 
      if (_cdm.getMethodIndex() != methodIndex) continue;
      cdm = _cdm;
      break;
    }
    if (cdm == null) throw new RuntimeException("not found");
    int codeoffs = cdm.getCodeOffset();
    Code code = openAt(dex, codeoffs).readCode();
    return code;
  }
  
  static final LazyMember<Method> GET_BOOTCLASSPATH_SIZE = LazyMember.of(
    "java.lang.VMClassLoader", "getBootClassPathSize", new Class<?>[0]
  );
  static final LazyMember<Method> GET_BOOTCLASSPATH_RES = LazyMember.of(
    "java.lang.VMClassLoader",
    "getBootClassPathResource", String.class, int.class
  );
  
  
  static List<URL> getBootstrapResources_android(final String name) {
    final List<URL> results = new ArrayList<URL>();
    final int classpathSize = GET_BOOTCLASSPATH_SIZE.<Integer>invoke(null);
    for (int i=0; i<classpathSize; ++i) {
      final String res = GET_BOOTCLASSPATH_RES.invoke(null, name, i);
      if (res == null) continue;
      try {
        results.add(new URL(res));
      } catch (final MalformedURLException mue) {
        throw (AssertionError) (new AssertionError().initCause(mue));
      }
    }
    return results;
  }
  
  static List<URL> getBootstrapResources_jre(final String name) {
    return EnumerationUtils.toList(
      GET_BOOTSTRAP_RES.<Enumeration<URL>>invoke(null, name)
    );
  }
  
  public static List<URL> getBootstrapResources(final String name) {
    return JRE
      ? getBootstrapResources_jre(name)
      : getBootstrapResources_android(name);
  }
  
  
  public static final String JAR_URL_HANDLER_PROPERTY = "jar.url.handler";
  public static final String JAR_URL_HANDLER_CLASSNAME_JRE
    = "sun.net.www.protocol.jar.Handler";
  public static final String JAR_URL_HANDLER_CLASSNAME_ANDROID
    = "org.apache.harmony.luni.internal.net.www.protocol.jar.Handler";
  

  static URLStreamHandler JAR_HANDLER;
  public static URLStreamHandler getJarURLStreamHandler() {
    if (JAR_HANDLER != null) return JAR_HANDLER;
    
    return (JAR_HANDLER = Reflect.newInstance(
      (Class<? extends URLStreamHandler>) (Class<?>)
      DexVisitor.classForName(
        System.getProperty(
          JAR_URL_HANDLER_PROPERTY,
          (CollectionUtil.isJRE())
            ? JAR_URL_HANDLER_CLASSNAME_JRE
            : JAR_URL_HANDLER_CLASSNAME_ANDROID
        )
      ),
      new Object[]{ }
    ));
  }
  
  static Map<CharSequence, URL> urlCache = new SoftHashMap<>();
  
  public static URL toURL(final File file) {
    try {
      return toURL(file.getCanonicalFile().getAbsoluteFile().getPath());
    } catch (final IOException ioe) {
      throw new IllegalArgumentException(String.format(
        "File(\"%s\").getCanonicalFile() threw %s",
        StringEscapeUtils.escapeJava(file.getPath()), ioe
      ), ioe);
    }
  }
  
  public static URL toURL(final String spec) {
    if (urlCache.containsKey(spec)) return urlCache.get(spec);
    final URL url;
    try {
      urlCache.put(spec, (url = new URL(spec)));
      return url;
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(String.format(
        "invalid URL spec \"%s\": %s", StringEscapeUtils.escapeJava(spec), e
      ), e);
    }
  }
  
  public static URL toJarURL(final URL fileUrl, final String entryName) {
    try {
      if ("jar".equals(toURI(fileUrl).getScheme())) {
        return fileUrl;
      }
      final URL jarUrl = new URL(
        fileUrl, 
        String.format(
          "jar:%s:%s!/%s", 
          toURI(fileUrl).getScheme(), fileUrl.getFile(), 
          StringUtils.stripStart(
            entryName != null? entryName: "classes.dex", "/"
          ),
          getJarURLStreamHandler()
        )
      );
      return jarUrl;
    } catch (final MalformedURLException mue) {
      mue.printStackTrace();
      throw Reflector.Util.sneakyThrow(mue);
    }
  }
  
  @Nullable
  public static URL getLocation(final @Nullable Class<?> cls) {
    if (cls == null) return null;
    final ProtectionDomain pd = cls.getProtectionDomain();
    if (pd == null) return null;
    final CodeSource codeSource = pd.getCodeSource();
    final URL locationUrl = (codeSource != null)
      ? codeSource.getLocation()
      : null;
    if (locationUrl != null) return locationUrl;
    else {
      try {
        URL jarUrl = ClassInfo.getClassResource(cls);
        URLConnection conn = jarUrl.openConnection();
        
        return (conn instanceof JarURLConnection)
          ? ((JarURLConnection) conn).getJarFileURL()
          : jarUrl;
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
  }
  
  
  public static Map.Entry<String, Dex> quickLocateDexEntryWithClass(
    final Map<String, Dex> dexMap, final String className)
  {
    final String desc = String.format(
      "L%s;", classNameToPath(className, null)
    );
    
    for (final Map.Entry<String, Dex> e: dexMap.entrySet()) {
      final Dex dex = e.getValue();
      final int typeIndex = dex.findTypeIndex(desc);
      if (typeIndex < 0) continue;
      final int classDefIndex 
        = dex.findClassDefIndexFromTypeIndex(typeIndex);
      if (classDefIndex < 0) continue; 
      return e;
    }
    return null;
  }
  
  
  
  public static Dex getDex(final String className,
    final @Nullable URL locationUrl)
  {
    if (className == null) throw new IllegalArgumentException("className == null");
    Log.d(TAG, "getDex(\"%s\")", className);
    final String classNameAsPath = classNameToPath(className, "class");
    
    Map.Entry<String, Dex> foundEntry = null;
    Map<String, Dex> dexMap = dexCache.get(
      locationUrl != null? locationUrl: className
    );
    if (dexMap != null) {
      foundEntry = quickLocateDexEntryWithClass(dexMap, className);
      if (foundEntry != null) return foundEntry.getValue();
    } else {
      dexCache.put(
        locationUrl != null? locationUrl: className,
        (dexMap = new LinkedHashMap<>())
      );
    }
    
    
    String location = locationUrl != null? locationUrl.getFile(): null;
    final Class<?> cls = DexVisitor.classForName(className);
    final URL url = (cls != null)
      ? ClassInfo.getClassResource(cls)
      : (locationUrl != null)
          ? toJarURL(
              locationUrl,
              JRE
                ? ClassInfo.classNameToPath(className, "class")
                : "classes.dex"
            )
          : ClassLoader.getSystemClassLoader().getResource(classNameAsPath);
    try {
      if (url != null) {
        final URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
          final JarURLConnection jconn = (JarURLConnection) conn;
          final ZipFile zf = jconn.getJarFile();
          
          String entryName;
          ZipEntry ze;
          for (int dexEntryNumber = 1;
              (ze = zf.getEntry((entryName = String.format(
                dexEntryNumber != 1? "classes%d.dex": "classes.dex",
                dexEntryNumber
              )))) != null;
              ++dexEntryNumber)
          {
            if (dexMap.containsKey(entryName)) continue;
            try (final InputStream in = zf.getInputStream(ze)) {
              final Dex dex = new Dex(in);
              dexMap.put(entryName, dex);
              if (foundEntry == null) {
                foundEntry = quickLocateDexEntryWithClass(
                  Collections.singletonMap(entryName, dex), className
                );
                if (foundEntry != null) {
                  Log.d(TAG, "Found class '%s' in dex: '%s' of jar '%s'",
                    className, entryName, zf.getName());
                  return foundEntry.getValue();
                }
              }
            }
          }
        }
      }
      
      final Pair<String, Dex> pair = findDexInClasspathBruteForce(className);
      if (pair != null) {
        foundEntry = quickLocateDexEntryWithClass(
         Collections.singletonMap(pair.getKey(), pair.getValue()), className
        );
        if (foundEntry != null) {
         Log.d(TAG, "Found class '%s' in brute force search; key in pair: '%s'",
           className, foundEntry.getKey());
         return foundEntry.getValue();
        }
        location = pair.getKey();
        dexLocations.put(className, location);
        dexMap = new LinkedHashMap<>(
          Collections.singletonMap(className, pair.getValue())
        );
        foundEntry = dexMap.entrySet().iterator().next();
        dexCache.put(location, dexMap);
        dexCache.put(className, dexMap);
        return pair.getValue();
      }
      
      Exception ultimateException = null;
      if (url != null) {
        try (final InputStream is = url.openConnection().getInputStream()) {
          final byte[] classBytes = IOUtils.toByteArray(is);
          final byte[] dexBytes = AndroidClassLoader.dexClassBytes(
            className, classBytes
          );
          Dex dex = new Dex(dexBytes);
          dexLocations.put(className, (location = url.getFile()));
          dexMap = new LinkedHashMap<>(singletonMap(className, dex));
          foundEntry = dexMap.entrySet().iterator().next();
          dexCache.put(location, dexMap);
          dexCache.put(className, dexMap);
          return dex;
        } catch (final IOException e2) {
          ultimateException = e2;
        }
      }
      
      final Enumeration<URL> res1;
      try {
        res1 = ClassLoader.getSystemClassLoader().getParent().getResources(
            classNameAsPath
          );
        while (res1.hasMoreElements()) {
          final URL res = res1.nextElement();
          try {
            byte[] classBytes = IOUtils.toByteArray(res);
            byte[] dexBytes
              = AndroidClassLoader.dexClassBytes(className, classBytes);
            Dex dex = new Dex(dexBytes);
            dexLocations.put(className, (location = res.getFile()));
            dexMap = new LinkedHashMap<>(singletonMap(className, dex));
            foundEntry = dexMap.entrySet().iterator().next();
            dexCache.put(location, dexMap);
            dexCache.put(className, dexMap);
            return dex;
          } catch (Exception e3) {
            if (ultimateException == null) ultimateException = e3;
            else ultimateException.addSuppressed(e3);
          }
        }
      } catch (IOException ioe) {
        if (ultimateException == null) ultimateException = ioe;
        else ultimateException.addSuppressed(ioe);
      }
      
      final Enumeration<URL> res2;
      try {
        res2 = GET_BOOTSTRAP_RES.invoke(null, classNameAsPath);
        while (res2.hasMoreElements()) {
          final URL res = res2.nextElement();
          try {
            byte[] classBytes = IOUtils.toByteArray(res);
            byte[] dexBytes
              = AndroidClassLoader.dexClassBytes(className, classBytes);
            Dex dex = new Dex(dexBytes);
            dexLocations.put(className, (location = res.getFile()));
            dexMap = new LinkedHashMap<>(singletonMap(className, dex));
            foundEntry = dexMap.entrySet().iterator().next();
            dexCache.put(location, dexMap);
            dexCache.put(className, dexMap);
            return dex;
          } catch (Exception e4) {
            if (ultimateException == null) ultimateException = e4;
            else ultimateException.addSuppressed(e4);
          }
        }
      } catch (Exception ioe) {
        if (ultimateException == null) ultimateException = ioe;
        else ultimateException.addSuppressed(ioe);
      }
      if (ultimateException != null) {
        throw Reflector.Util.sneakyThrow(ultimateException);
      }
      return (foundEntry != null) ? foundEntry.getValue(): null;
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  
  public static Dex getDex(final String path, final String dexEntryName) {
    Log.d(TAG, "getDex(path: \"%s\", dexEntryName: %s)", path,
      dexEntryName != null? String.format("\"%s\"", dexEntryName): null);
    if (! new File(path).exists()) return null;
    try {
      final String dataEnvvar = System.getenv("ANDROID_DATA");
      final String dataDirPath = (dataEnvvar != null)? dataEnvvar: "/data";
      final File cacheDir = new File(new File(dataDirPath), "dalvik-cache");
      final File cacheFile = new File(
        cacheDir, 
        new StringBuilder(path.length() + 10).append(
          path.replace('/', '@').subSequence(
            ((path.length() > 0 && path.charAt(0) == '/')? 1: 0),
            path.length()
          )
        ).append(
          (dexEntryName != null)
            ? new StringBuilder(dexEntryName.length()+1)
                .append("@").append(dexEntryName)
            : ""
        ).toString()
      );
      if (cacheFile.exists()) {
        try (final FileInputStream fis = new FileInputStream(cacheFile);
             final FileChannel fch = fis.getChannel())
        {
          final ByteBuffer[] buffers = {
            ByteBuffer.allocate(40), // skip "optimized" header ("dey 036")
            ByteBuffer.allocateDirect((int) (fch.size() - 40))
          };
          fch.read(buffers);
          return ((LazyMember<Constructor<Dex>>) DEX_CTOR)
            .<Dex>newInstance(buffers[1]);
        }
      } else {
        final URL url = new URL(
          (dexEntryName != null)
            ? String.format("jar:file://%s!/%s", path, dexEntryName)
            : String.format("file://%s", path)
        );
        try (final InputStream is = url.openConnection().getInputStream()) {
          return new Dex(is);
        }
      }
    } catch (final IOException e) {
      final String message = String.format(
        "Error in getDex(path: %s, dexEntryName: %s): %s",
        path, dexEntryName, e
      );
      Log.e(TAG, message);
      Log.e(TAG, e);
      throw new RuntimeException(message, e);
    }
  }
  
  public static URL getClassResource(final Class<?> cls) {
    if (cls == null) throw new IllegalArgumentException("cls == null");
    /*
    if (NameSpace_getClassResource == null) {
      try {
        NameSpace_cls = Optional.of(Class.forName(
          "bsh.NameSpace", false, Thread.currentThread().getContextClassLoader()
        ));
        (NameSpace_getClassResource = Optional.of(
          NameSpace_cls.get().getDeclaredMethod("getClassResource", Class.class)
        )).get().setAccessible(true);
      } catch (final ReflectiveOperationException | LinkageError ex) {
        NameSpace_cls = Optional.absent();
        NameSpace_getClassResource = Optional.absent();
      }
    }
    
    if (NameSpace_getClassResource.isPresent()) {
      try {
        final URL res = (URL) NameSpace_getClassResource.get().invoke(null, cls);
        if (res != null) return res;
      } catch (final ReflectiveOperationException | LinkageError ex) {
        ex.printStackTrace();
      }
    }*/
    // fallback
    final String clsResPath = ClassInfo.classNameToPath(cls.getName(), "class");
    final ClassLoader cl = (cls.getClassLoader() != null)
      ? cls.getClassLoader()
      : ClassLoader.getSystemClassLoader().getParent();
    final URL res = cl.getResource(clsResPath);
    return res;
  }
  
  
  
  public static final String TAG_CP_DBG = TAG + "/CP_DBG";
  public static boolean CP_DEBUG
     = Boolean.getBoolean("debug.classpath")
    || "1".equals(System.getenv("CLASSPATH_VERBOSE"))
    || Log.isLoggable(Log.SEV_VERBOSE);
  
  static ClassInfo _it = new ClassInfo();
  
  static ClassInfo CP_DBG(final String message) {
    if (!CP_DEBUG) return _it;
    Log.INSTANCE.log(
      TAG_CP_DBG, // String tag
      Log.SEV_INFO, // int severity
      message, // String message
      Log.NO_ARGS// Object[] args
    );
    return _it;
  }
  ClassInfo v(final String name, final Object value) {
    if (!CP_DEBUG) return this;
    Log.INSTANCE.log(
      TAG_CP_DBG, // String tag
      Log.SEV_DEBUG, // int severity
      "   .. %20s = %s", // String message
      new Object[]{ name, value }
    );
    return this;
  }
  

  public static URL getClassResource(final String className) {
    return CollectionUtil.firstOrDefault(
      getClassResources(
        className,
        false // wantMultiple
      )
    );
  }
  
  public static List<URL> getClassResources(final String className, 
    final boolean wantMultiple)
  {
    if (className == null) {
      throw new IllegalArgumentException("className == null");
    }

    final String clsResPath = ClassInfo.classNameToPath(className, "class");
    final Class<?> cls = DexVisitor.classForName(className);
    CP_DBG("GCR: <<Plan A Start>>")
        .v("clsResPath", clsResPath);
    
    if (cls != null) {
      CP_DBG("GCR: Class loaded; taking short route..")
        .v("className", className)
        .v("cls", cls);
      final URL loc = getLocation(cls);
      CP_DBG("GCR: Got URL (or null)from getLocation(Class<?>)")
        .v("loc", loc);
      
      if (loc != null) {
        URL classFileJarUrl = toJarURL(loc, clsResPath);
        CP_DBG("GCR [ret] converted URL via toJarUrl(loc, clsResPath)")
          .v("classFileJarUrl", classFileJarUrl);
        return Collections.singletonList(classFileJarUrl);
      } else {
        CP_DBG("GCR: couldn't get locatin via Class<?>!");
      }
    }
    
    
    CP_DBG("GCR: <<Plan C Start>>: plain ClassLoader resource lookup");
    final ClassLoader clsClassLoaderOrNull = (cls != null)
      ? cls.getClassLoader()
      : null;
    CP_DBG("GCR: get ClassLoader from cls (if cls nonnull)")
        .v("clsClassLoaderOrNull", clsClassLoaderOrNull);
    
    if (clsClassLoaderOrNull != null) {
      final URL urlFromLoader
        = clsClassLoaderOrNull.getResource(clsResPath);
      CP_DBG("GCR: Get URL from non-null loader provided by Class<?>")
          .v("urlFromLoader", urlFromLoader); 
      if (urlFromLoader != null) {
        final URL classFileJarUrl = toJarURL(urlFromLoader, clsResPath);
        CP_DBG("GCR [ret] converted URL via toJarUrl(URL, String)")
            .v("classFileJarUrl", classFileJarUrl);
        return Collections.singletonList(classFileJarUrl);
      }
    }
    
    
    CP_DBG("GCR: <<Plan D Start>>: bootstrap resource lookup");
    final List<URL> bootstrapResults = getBootstrapResources(clsResPath);
    CP_DBG("GCR: Got List<URL> from getBootstrapResources(String)")
        .v("bootstrapResults", bootstrapResults);
      
    
    final List<URL> returnJarUrls = new ArrayList<>();
    
    
    if (! bootstrapResults.isEmpty()) {
      CP_DBG("GCR [ret] bootstrapResults URL (first one)")
          .v("bootstrapResults.size()", bootstrapResults.size()); 
      for (final URL bootstrapResult: bootstrapResults) {
        final URL classFileJarUrl = toJarURL(bootstrapResult, clsResPath);
        CP_DBG("GCR converted URL via toJarUrl(URL, String)")
            .v("bootstrapResult", bootstrapResult)
            .v("classFileJarUrl", classFileJarUrl);
        returnJarUrls.add(classFileJarUrl);
        if (!wantMultiple) return returnJarUrls;
      }
    }
    
    
    final ClassLoader ctxClassLoader
        = Thread.currentThread().getContextClassLoader();
    final ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
    final ClassLoader startClassLoader =
        (ctxClassLoader != null &&
        (cls == null || ctxClassLoader != cls.getClassLoader()))
          ? ctxClassLoader
          : appClassLoader;
    for (ClassLoader l = startClassLoader; l != null; l = l.getParent()) {
      final URL loaderResult = l.getResource(clsResPath);
      if (loaderResult != null) {
        returnJarUrls.add(toJarURL(loaderResult, clsResPath));
        if (!wantMultiple) return returnJarUrls;
        break;
      }
    }
    if (ctxClassLoader != appClassLoader &&
        ctxClassLoader != null && appClassLoader != null)
    {
      final ClassLoader otherCl = (startClassLoader == ctxClassLoader)
        ? appClassLoader
        : ctxClassLoader;
      for (ClassLoader l = otherCl; l != null; l = l.getParent()) {
        final URL loaderResult = l.getResource(clsResPath);
        if (loaderResult != null) {
          returnJarUrls.add(toJarURL(loaderResult, clsResPath));
          if (!wantMultiple) return returnJarUrls;
          break;
        }
      }
    }
    return returnJarUrls;
  }
  
  
  public static String cleanClassPath(final String classpath) {
    return CLEAN_CLASSPATH_MCHR.reset(classpath).replaceAll("");
  }

  public static String getClassPath() {
    return System.getProperty("java.class.path");
  }
  
  public static String getBootClassPath() {
    if (System.getProperty("sun.boot.class.path") != null) {
      return System.getProperty("sun.boot.class.path");
    }
    try {
      return BOOTCLASSPATH_MATCHER.reset(
       ClassInfo.getBootClassPath()
      ).replaceAll(BOOTCLASSPATH_REPLACEMENT);
    } catch (Exception e) {
      e.printStackTrace();
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static String getFullClassPath(final boolean userIsFirst) {
    return cleanClassPath(String.format(
      "%s:%s",
      userIsFirst ? getClassPath() : getBootClassPath(),
      userIsFirst ? getBootClassPath() : getClassPath()
    ));
  }
  
  public static String getFullClassPath() {
    return getFullClassPath(false);
  }
  
  public static Object appendClassPathFile(String path, boolean append) {
    if ((!(path instanceof String)) || path.length() == 0) {
      Log.w("appendClassPathFile", "Bad input argument: path == void or null (or empty)");
      return null;
    }
    final File file;
    try {
      file = new File(path)
        .getAbsoluteFile()
        .getCanonicalFile();
    } catch (final IOException ioe) {
      throw new RuntimeException(String.format(
        "ClassInfo.appendClassPathFile(path: %s, append: %s) failed: " +
        "Conversion of the corresponding File(\"%s\") (absolute: File(\"%s\")) " +
        "to canonical form failed: %s",
        path, append,
        new File(path).getPath(), new File(path).getAbsoluteFile().getPath(), ioe
      ), ioe);
    }
    if (!file.exists()) return null;
    
    ClassLoader ldr = Thread.currentThread().getContextClassLoader();
    if (ldr == null) ldr = ClassLoader.getSystemClassLoader();
    
    if (ldr instanceof BaseDexClassLoader) {
      Object[] existing = CollectionUtil2.filter(
        Reflect.<Object[]>getfldval(
          getfldval(ldr, "pathList"), "dexElements"
        ), Pattern.quote(file.getPath())
      );
      if (existing.length != 0) return existing[0];
    
      int cookie = Reflector.invokeOrDefault(
        dalvik.system.DexFile.class,
        "openDexFileNative",
        new Object[]{ file.getAbsolutePath(), null, 0 }
      );
      dalvik.system.DexFile df
        = Reflect.allocateInstance(dalvik.system.DexFile.class);
      setfldval(df, "mFileName", file.getAbsolutePath());
      setfldval(df, "mCookie", cookie);
      LazyMember<Constructor<?>> ctor = LazyMember.of(
        "dalvik.system.DexPathList$Element", "<init>",
        File.class, Boolean.TYPE, File.class, dalvik.system.DexFile.class
      );
      Object dexElement = ctor.newInstance(
        file.getAbsoluteFile(), file.isDirectory(),
        (!file.isDirectory() && !StringUtils.endsWith(file.getName(), "dex"))
          ? new File(file.getAbsolutePath())
          : null,
        df
      );
      Object[] existingElems = getfldval(
        getfldval(ldr, "pathList"), "dexElements"
      );
      Object[] newElems = (dexElement != null)
        ? (Object[]) CollectionUtil.toArray(Arrays.asList(dexElement))
        : (Object[]) Array.newInstance(
            DexVisitor.classForName("dalvik.system.DexPathList$Element"), 0
          );
      
      setfldval(
        getfldval(ldr, "pathList"),
        "dexElements",
        (append)
          ? ArrayUtils.addAll(existingElems, newElems)
          : ArrayUtils.addAll(newElems, existingElems)
      );
      
      Reflect.<Set<?>>getfldval(
        getfldval(CollectionUtil.getInterpreter(), "bcm"),
        "absoluteNonClasses"
      ).clear();
      Reflect.<Map<?, ?>>getfldval(bsh.Capabilities.class, "classes").clear();
      if (bsh.Capabilities.classExists("dalvik.system.XClassLoader")) {
        final Object badClasses = Reflect.getfldval(
          dalvik.system.XClassLoader.class, "badClasses"
        );
        if (badClasses instanceof Map<?, ?>) {
          ((Map<?, ?>) badClasses).clear();
        } else if (badClasses instanceof Collection<?>) {
          ((Collection<?>) badClasses).clear();
        }
      }
      return dexElement;
    }
    
    if (ldr instanceof URLClassLoader) {
      try {
        final URL jarUrl = new URL(String.format(
          "jar:file://%s!/", file.getPath()
        ));
        Reflector.invokeOrDefault(
          ldr, "addURL", new Object[]{ jarUrl }
        );
        return jarUrl;
      } catch (final MalformedURLException mue) {
        throw new RuntimeException(String.format(
          "ClassInfo.appendClassPathFile(path: %s, append: %s) failed: " +
          "Creation of the URL failed for call to URLClassLoader.addURL(URL) " +
          "on %s@%08x: %s",
          path, append,
          (ldr != null) ? ldr.getClass().getName() : "<null>",
          System.identityHashCode(ldr), mue
        ), mue);
      }
    }
    
    throw new UnsupportedOperationException(String.format(
      "Do not know how to add a classpath element to loaders of type '%s' " +
      "(path: '%s')",
      ldr.getClass().getName(), file.getPath()
    ));
  }
  
  
  public static Object appendClassPathFile(String path) {
    return appendClassPathFile(path, true);
  }
  
  
  
  public static List<String> getClassNamesFromEntries(final String jar) {
    if (ClassPathUtil.classpathMap != null &&
        ClassPathUtil.classpathMap.containsKey(jar))
    {
      return Arrays.asList(ClassPathUtil.classpathMap.get(jar));
    }
    
    try (final ZipFile zipFile = new ZipFile(jar)) {
      return getClassNamesFromEntries(zipFile, true);
    } catch (final IOException ioEx) {
      ioEx.printStackTrace();
      return Collections.emptyList();
    }
  }
  
  public static List<String> getClassNamesFromEntries(final ZipFile jar) {
    return getClassNamesFromEntries(jar, true); // includeNested == false
  }
  
  static StringBuilder sNameSb;
  
  public static List<String> getClassNamesFromEntries(final ZipFile jar,
    final boolean includeNested)
  {
    final StringBuilder sb
      = (sNameSb != null? sNameSb: (sNameSb = new StringBuilder(128)));
    final List<String> classNames = new ArrayList<>(64);
    
    for (final Enumeration<? extends ZipEntry> en = jar.entries();
         en.hasMoreElements();)
    {
      final ZipEntry ze = en.nextElement();
      if (ze.isDirectory()) continue;
      final int size = (int) ze.getSize();
      if (size > 0 && size < 10) continue;
      final String name = ze.getName();
      final int len = name.length();
      final int classNameLen = len - CLASS_SUFFIX_LEN;
      if (classNameLen < 1) continue;
      if (name.charAt(len - CLASS_SUFFIX_LEN) != '.') continue;
      final int dollarPos = name.indexOf('$');
      if (!includeNested && dollarPos != -1) {
        continue;
      }
      if (!CLASS_SUFFIX.equals((String)name.subSequence(classNameLen, len)))
      {
        continue;
      }
      sb.setLength(0);
      int lastPos = -1;
      for (int slashPos = -1;
              (slashPos = name.indexOf('/', slashPos + 1)) != -1;
              lastPos = slashPos)
      {
        if (lastPos != -1) sb.append('.');
        sb.append(name, lastPos+1, slashPos);
      }
      if (lastPos != -1) sb.append('.');
      sb.append(name, lastPos+1, len - ClassInfo.CLASS_SUFFIX_LEN);
      classNames.add(sb.toString());
    }
    return classNames;
  }
  
   public static Map<String, byte[]> importDexClasses(/*;*/String className) { //;
      byte[] zipBytes = importDexClasses(className, true, true);
      if (zipBytes == null) return Collections.emptyMap();
      return getAndAddAllJarEntries(zipBytes);
   }
    
   public static Map<String, byte[]> getAndAddAllJarEntries(final byte[] zipBytes)
   {
     if (zipBytes == null) return Collections.emptyMap();
      Map<String, byte[]> zm = ZipUtil.mapZip(zipBytes);
      List<ITypeLoader> ldrs = new ArrayList<>(); 
      for (Map.Entry<String, byte[]> ent: zm.entrySet()) {
        ITypeLoader ldr = new ArrayTypeLoader(ent.getValue());
        ldrs.add(ldr);
      }
      if (ldrs.isEmpty()) return Collections.emptyMap();
      
      ITypeLoader ctl = new CompositeTypeLoader(ldrs.toArray(new ITypeLoader[0]));
      MetadataSystem mds = new MetadataSystem(ctl);
      for (Map.Entry<String, byte[]> ent: zm.entrySet()) {
        TypeReference tr = mds.lookupType(
          StringUtils.substringBeforeLast(ent.getKey(), ".class")
        );
        TypeDefinition td = tr.resolve();
        if (td != null) {
          ((MetadataSystem) ProcyonUtil.getMetadataResolver()
          ).addTypeDefinition(td);
          Log.d(TAG, "Addded TypeDefinition: %s\n", td);
        }
      }
      return zm;
   }
   
   public static Map<String, byte[]> loadSelectedClassesFromDexed(String jarPath,
     @Nullable String className)
   {
     try {
       final ByteArrayOutputStream os = new ByteArrayOutputStream();
         Dex2jar.from(
          (className != null)
            ? DexRemix.remixDex(
                jarPath,
                Pattern.compile(
                  StringUtils.substringBefore(
                    className, "$"
                  ).replaceAll("[^a-zA-Z0-9]", ".?")
                ).matcher("")
              )
            : FileUtils.readFileToByteArray(new File(jarPath))
        ).skipDebug(false).to(os);
        byte[] zipBytes = os.toByteArray();
        if (zipBytes == null) return Collections.emptyMap();
        return getAndAddAllJarEntries(zipBytes);
      } catch (RuntimeException e) {
        return Collections.emptyMap();
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
   }
   
   // from JarDecompiler

     
  // fixme
  public static byte[] getDexSubset(String apkOrDexPath,
    DexBackedDexFile dbdf,
    String[] classNamesToInclude) throws Throwable
  {
    List<String> classNames =
      Arrays.asList(ClassInfo.typeToName(classNamesToInclude));
    Log.d(
      "getDexSubset",
      "getDexSubset(apkOrDexPath: %s, dbdf: %s, classNamesToInclude: %s)",
      apkOrDexPath, dbdf, Arrays.toString(classNamesToInclude)
    );
    List<DexBackedClassDef> classDefs = new ArrayList<DexBackedClassDef>();
    com.android.dex.Dex dex = new com.android.dex.Dex(
      (byte[]) Reflect.getfldval(dbdf, "buf")
    );
    Log.d("getDexSubset", "dex = %s (classDefs: %s)", dex, dex.classDefs());
    final DexPool dexPool = DexRemix.newDexPool();
    final org.jf.dexlib2.writer.pool.ClassPool classPool
      = Reflect.getfldval(dexPool, "classSection");
    for (final String className: classNames) {
      int classDefIndex = ClassInfo.getDexClassDefIndex(dex, className);
      int classDefOffset = (classDefIndex != -1)
        ? dbdf.getClassDefItemOffset(classDefIndex)
        : -1;
      Log.d(
        "getDexSubset", "className = %s; classDefIndex: %d, classDefOffset: %d", 
        className, classDefIndex, classDefOffset
      );
      if (classDefIndex == -1) continue;
      final DexBackedClassDef dbcd = new DexBackedClassDef(
        dbdf, classDefOffset
      );
      Log.d("getDexSubset", "dbcd = %s", dbcd);
      try {
        classPool.intern(dbcd);
        Log.d("getDexSubset", "added %s to %s", dbcd, classPool);
      } catch (org.jf.util.ExceptionWithContext ex) {
        if (ex.getMessage().indexOf("has already been interned") == -1) {
          throw Reflector.Util.sneakyThrow(ex);
        }
      }
    }
    final byte[] originalDexBuf = Reflect.getfldval(dbdf, "buf");
    
    final int capacity = 0x2000;
    final DexDataStore store = new MemoryDataStore(capacity);
    Log.d("getDexSubset", "store = %d (capacity: %d)", store, capacity);
    dexPool.writeTo(store);
    store.close();
    final byte[] dexBytes = IOUtils.toByteArray(store.readAt(0));
    Log.d("getDexSubset", "dexBytes = %d (length: %d)", dexBytes, dexBytes.length);
    return dexBytes;
  }
  

  public static Map<String, TypeDefinition> getTypeDefinitions_enjarify(
    String apkOrDexPath,
    String className,
    String[] classNames)
      throws Throwable
  {
    final MultiDexContainer<DexBackedDexFile> mdc
        = DexRemix.getDexBackedDexFile(apkOrDexPath);
    final Map<String, byte[]> zm = new TreeMap<>();
    final List<ITypeLoader> arrayLoaders = new ArrayList<>();
    
    for (final String entryName: mdc.getDexEntryNames()) {
      final DexBackedDexFile db_df = mdc.getEntry(entryName);
      Log.w(
        TAG, "TD-ENJARIFY (apkOrDexFile: %s) ...\n  " +
        "using results from MultiDexContainer<DexBackedDexFile>: %s ...",
        apkOrDexPath, Debug.ToString(mdc)
      );
      
      
      byte[] dexBytes = getDexSubset(apkOrDexPath, db_df, classNames);
      
      File tempDex = File.createTempFile("classes", ".dex");
      Log.d("getTypeDefinitions_enjarify", "tempDex = %s", tempDex);
      File outputJar = new File(
        tempDex.getParentFile(),
        String.format(
          "%s_%s_enjarify.jar",
          org.apache.commons.io.FilenameUtils.removeExtension(
            tempDex.getName()
          ),
          System.currentTimeMillis()
        )
      );
      Log.d("getTypeDefinitions_enjarify", "outputJar = %s", outputJar);
      FileUtils.writeByteArrayToFile(tempDex, dexBytes);
      Log.d("getTypeDefinitions_enjarify", "tempDex.length(): %s", tempDex.length());
      String output = PosixFileInputStream.pexecSync(
        "enjarify", "-f", "-o", outputJar.getPath(), tempDex.getPath()
      );
      Log.d("getTypeDefinitions_enjarify", "output = %s", output);
      if (!outputJar.exists()) {
        throw new RuntimeException(String.format(
          "enjarify task failed for %s: %s", apkOrDexPath, outputJar.getPath()
        ));
      }
    
    
      
      File outputJarGeneric = new File(
        tempDex.getParentFile(),
        String.format(
          "%s_generic.jar",
          org.apache.commons.io.FilenameUtils.removeExtension(
            outputJar.getName()
          )
        )
      );
      Log.d(
        "getTypeDefinitions_enjarify",
        "outputJarGeneric = %s", outputJarGeneric
      );
      final byte[] outputJarGenericBytes = ZipUtil.writeZip(
        addGenericSignatures(outputJar.getPath(), db_df, classNames)
      );
      FileUtils.writeByteArrayToFile(outputJarGeneric, outputJarGenericBytes);
      zm.putAll(ZipUtil.mapZip(outputJarGenericBytes));
      
      for (final Map.Entry<String, byte[]> entry: zm.entrySet()) {
        final String classNameAsPath = entry.getKey();
        final byte[] classBytes = entry.getValue();
        arrayLoaders.add(new ArrayTypeLoader(classBytes));
      }
    }
    
    arrayLoaders.add(ProcyonUtil.getTypeLoader());
    final MetadataSystem mds = new MetadataSystem(
      new CompositeTypeLoader(arrayLoaders.toArray(new ITypeLoader[0]))
    );
    final Map<String, TypeDefinition> tdmap = new TreeMap<>();
    
    for (final String classNameAsPath: zm.keySet()) {
      final int lastDotPos = classNameAsPath.lastIndexOf('.');
      if (lastDotPos < 1) throw new IllegalActionError(classNameAsPath);
      final String classNameAsPathNoExtension
        = (String) classNameAsPath.subSequence(0, lastDotPos);
      final TypeReference type
        = mds.lookupType(classNameAsPathNoExtension);
      final TypeDefinition typeDefinition
        = VerifyArgument.notNull(type, "type").resolve();
      Log.d(TAG, "Loaded typeDefinition: %s\n", typeDefinition);
      String typeName = ClassInfo.typeToName(type.getInternalName());
      ProcyonUtil.addTypeDefinition(typeDefinition);
      tdmap.put(typeName, typeDefinition);
    }
    
    // Reflect.setfldval(mds, "_typeLoader", ProcyonUtil.getTypeLoader());
    Log.d(TAG, "tdmap = %s", tdmap);
    return tdmap;
  }
  
  
  public static Map<String, byte[]> addGenericSignatures(
     String jarPath, DexBackedDexFile db_df, String[] classNames)
     throws Throwable
   {
    if (jarPath == null && System.getProperty("input.file") != null) {
      jarPath = System.getProperty("input.file");
    }
    Map<String, Map<String, Map<String, Object>>> annsByClass = new TreeMap();
    Map<String, Map<String, Object>> annMap = new TreeMap<>();
    Map<String, String> sigMap = new TreeMap<>();
    for (DexBackedClassDef dbcd : db_df.getClasses()) {
      if  (ArrayUtils.indexOf(classNames, ClassInfo.typeToName(dbcd.getType()))==-1)
        continue;
      final Iterable<DexBackedAnnotation> anns =
        (Iterable<DexBackedAnnotation>) (Iterable<?>) dbcd.getAnnotations();
      for (final DexBackedAnnotation m: anns) {
        String annotationType = m.getType();
        Map<String, Object> inner = new RealArrayMap<>();
        for (final DexBackedAnnotationElement n:
             (Iterable<DexBackedAnnotationElement>) (Iterable<?>) m.getElements())
        {
          String name = n.getName();
          Object value = n.getValue();
          if (value instanceof DexBackedArrayEncodedValue) {
            value = ((DexBackedArrayEncodedValue) value).getValue();
          }
          if (value instanceof Iterable<?>) {
            value = StringUtils.join(
              CollectionUtil2.invokeAll((Iterable<?>) value, "getValue"), ""
            );
          }
          inner.put(name, value);
          if ("Ldalvik/annotation/Signature;".equals(annotationType)) {
            sigMap.put(dbcd.getType(), (String) value);
          }
        }
        annMap.put(annotationType, inner);
      }
      annsByClass.put(dbcd.getType(), annMap);

      if (annMap.size() > 0) {
        System.err.printf(
          "Read %d annotations from %s: [%s]\n", annMap.size(),
          ClassInfo.typeToName(dbcd.getType()),
          Arrays.asList(
            ClassInfo.typeToName(annMap.keySet().toArray(new String[0]))
          )
        );
      }
    }
    
    javassist.ClassPool cp2 = new javassist.ClassPool(
      CollectionUtil.getClassPool()
    );
    final JarClassPath jarClassPath;
    cp2.insertClassPath(
      jarClassPath = Reflect.newInstance(JarClassPath.class, jarPath)
    );
    final Map<String, byte[]> zm;
    try (final ZipFile zf = new ZipFile(jarPath)) {
      zm = new TreeMap<>();
      zm.putAll(ZipUtil.mapBytes(zf));
    }
    for (final Map.Entry<String, String> sigEntry : sigMap.entrySet()) {
      String className = ClassInfo.typeToName(sigEntry.getKey());
      String signature = sigEntry.getValue();
      System.err.printf("Adding signature to %s: %s\n", className, signature);
      try {
        CtClass ct = cp2.get(className);
        if (((Boolean) Reflect.getfldval(ct, "wasPruned")).booleanValue())
          ct.stopPruning(true);
        if (((Boolean) Reflect.getfldval(ct, "wasFrozen")).booleanValue())
          ct.defrost();
        ct.setGenericSignature(signature);
        byte[] classBytes = ct.toBytecode();
        zm.put(ClassInfo.classNameToPath(className, "class"), classBytes);
      } catch (javassist.NotFoundException nfe) {
        System.err.println(nfe);
      }
    }
    for (final Object object: new Object[]{ cp2, jarClassPath }) {
      Class<?> cls = object.getClass();
      while (cls != Object.class && cls != null) {
        for (final Field fld: cls.getDeclaredFields()) {
          if ((fld.getModifiers() & Modifier.STATIC) != 0) continue;
          if (fld.getType().isPrimitive()) continue;
          Reflect.setfldval(object, fld.getName(), null);
        }
        cls = cls.getSuperclass();
      }
    }
    // System.gc();
    return zm;
  }
   

  public static Pair<String, byte[]> getClassBytes(final String className) {
    byte[] bestClassBytes = null;
    int maxBytes = 0;
    File bestFile = null;
    List<File> files = null;
    
    final String resPath = ClassInfo.classNameToPath(className, "class");
    
    final File dir = DEX_INDEX_DIR;
    if (dir.exists() && !dir.isFile()) {
      final int len = className.length();
      if (len == 0) throw new IllegalArgumentException("className == \"\"");
      final StringBuilder prefixSb = new StringBuilder(len);
      File indexFile = null;
      found:
      do {
        for (int prefixLen = 0; prefixLen < len; ++prefixLen) {
          prefixSb.append(
            Character.toLowerCase(className.charAt(prefixLen))
          );
          if ((indexFile = new File(dir, prefixSb.toString())).exists()) {
            break found;
          }
        }
        Log.w(
          TAG, "Missing dex index file (className: %s)", str(className)
        );
        return null;
      } while (false);
      
      final String[] lines = PosixFileInputStream.pexecSync(
        RealArrayMap.toMap("LC_ALL", "C"),
        "/bin/grep", "-e", String.format("^%s\t", className),
        indexFile.getPath()
      ).split("\n");
      
      for (final String line: lines) {
        final int indexOfTab = line.indexOf('\t');
        if (indexOfTab == -1) continue;
        final String name = (String) line.subSequence(0, indexOfTab); 
        if (!name.equals(className)) continue;
        final File file = new File(
          (String) line.subSequence(name.length()+1, line.length())
        );
        if (!file.exists() || !file.isFile()) continue;
        (files != null ? files : (files = new LinkedList<>())).add(file);
        try {
          final byte[] classBytes = ZipUtil.toByteArray(
            file.getPath(), resPath
          );
          if (classBytes == null) continue;
          if (classBytes.length > maxBytes) {
            bestFile = file;
            bestClassBytes = classBytes;
            maxBytes = classBytes.length;
          }
          Log.w(
            TAG,
            "Read [%s] from [%s] (master index '%s', %d bytes)",
            className,
            file.getPath().replace("/external_sd/_projects/sdk/", ""),
            indexFile.getName(),
            classBytes.length
          );
        } catch (final Exception e) {
          continue;
        }
      }
    } // endif (dir = DEX_INDEX_DIR.)exists()
    
    if (bestClassBytes == null && files != null) {
      for (final File file: files) {
        final Map<String, byte[]> zm;
        try {
          zm = ClassInfo.loadSelectedClassesFromDexed(
            file.getPath(), className
          );
          if (zm == null || !zm.containsKey(resPath)) continue;
          bestClassBytes = zm.get(resPath);
          bestFile = file;
          break;
        } catch (final Exception e) {
          continue;
        }
      }
    }
    return (bestClassBytes != null)
      ? Pair.of(bestFile.getPath(), bestClassBytes)
      : null;
  }
  
  public static Map<String, byte[]> tryFindClass(String className) {
    String line = CollectionUtil.firstOrDefault(
          CollectionUtil2.filter(
            PosixFileInputStream.pexecSync(
              "finddex2", className
            ).split("\n"),
            "=="
          )
        );
    if (line == null) return null;

    
    File dex = new File(StringUtils.substringAfter(line, "==> "));
    String jar = StringUtils.substringBeforeLast(
      dex.getPath().replace("/data/dalvik-cache","").replaceAll("@", "/"),
      "/"
    );
    String entryName = StringUtils.substringAfterLast(
      dex.getPath().replace("/data/dalvik-cache","").replaceAll("@", "/"),
      "/"
    );
    if (!new File(jar).exists() || !new File(jar).isFile()) {
      jar = String.format("%s/%s", jar, entryName);
      entryName = null;
    }
    Log.i(
      TAG, "Located %s in %s (entryName: %s)",
      className, jar, entryName
    );
    
    // ClassInfo.appendClassPathFile(jar);
    // byte[] zipBytes = ClassInfo.importDexClasses(className, true, true);
    return ClassInfo.loadSelectedClassesFromDexed(jar, className);
  }
   
}