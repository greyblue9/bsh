package org.d6r;

import org.d6r.annotation.*;
import dalvik.system.BaseDexClassLoader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.d6r.IOStream;
import libcore.io.Streams;
import dalvik.system.DexFile;
import bsh.Factory;
import bsh.ClassIdentifier;
import bsh.operators.Extension;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.d6r.CollectionUtil;
import org.d6r.Reflect;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import javassist.CtClass;
import javassist.ClassPool;
import javassist.CtMethod;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import com.android.org.chromium.com.google.common.base.Joiner;

public class CollectionUtil2 {
  
  
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ClassConverter {
  }
  
  
  public static final Object[] NO_ARGS = { };
  public static int INITIAL_CAPACITY = 32;
  public static int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE 
    | Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES;
  public static Matcher DECOLOR 
    = Pattern.compile("\u001b\\[[0-9;]*m?").matcher("");
  public static Matcher DEGENERIFY
    = Pattern.compile("<.*$").matcher("");
  
  public static boolean DEBUG = false;
  public static PrintStream dbg = System.err;
  
  @NonDumpable("[All Exceptions Encountered]")
  public static final List<Throwable> errors = new ArrayList<Throwable>();
  public static Map<String, Object> urlStringToDexElementCache;
  
  
  
  @Extension
  public static <T> List<T> typeFilter(Iterable<?> in, Class<T> cls) {
    int capacity = sizeof(in);
    if (capacity == -1) capacity = INITIAL_CAPACITY;
    List<T> filtered = new ArrayList<T>(capacity);
    Iterator<?> it = in.iterator();
    Object elem;
    while (it.hasNext()) { 
      elem = it.next();
      if (elem == null) continue; 
      if (! cls.isInstance(elem)) continue; 
      T tElem = (T) elem;
      filtered.add(tElem);
    }
    return filtered;
  }
  
  @Extension
  public static <T> 
  T[] typeFilter(Object[] in, Class<T> cls) 
  {
    List<T> filtered = typeFilter(Arrays.asList(in), cls);
    return filtered.isEmpty()
      ? (T[]) Array.<T>newInstance(cls, 0)
      : CollectionUtil.toArray(filtered);
  }
  
  @Extension
  public static <T> 
  List<T> invokeAll(final Iterable<?> in, final String methodName, final int argPos,
    Object... args)
  {
    int capacity = sizeof(in);
    if (capacity == -1) capacity = INITIAL_CAPACITY;
    final List<T> filtered = new ArrayList<T>(capacity);
    final Iterator<?> it = in.iterator();
    // ensure assignment to args element is safe
    if (args != NO_ARGS && args.getClass() != Object[].class) {
      final Object[] oArgs = new Object[args.length];
      System.arraycopy(args, 0, oArgs, 0, args.length);
      args = oArgs;
    }
    final boolean doAssign = args != NO_ARGS && argPos >= 0 && argPos < args.length;
    while (it.hasNext()) { 
      Object elem = it.next();
      if (elem == null) continue;
      if (doAssign) args[argPos] = elem;
      try {
        final T result = (T) (
          (argPos != -1)
            ? Reflect.invokeMethod(elem, methodName, args)
            : Reflect.invokeMethod(
                args[0], methodName, Arrays.copyOfRange(args, 1, args.length)
              )
        );
        if (result == null) continue;
        filtered.add(result);
      } catch (Exception ite) {
        Log.w("invokeAll", ((ite instanceof InvocationTargetException)
          ? ((InvocationTargetException) ite).getTargetException().toString()
          : ite.toString()));
      } catch (Throwable e) {
      }
    }
    return filtered;
  }
  
  
  @Extension
  public static <T> 
  List<T> invokeAll(final Iterable<?> in, final Member method, final int argPos,
    Object... args)
  {
    try {
      ((AccessibleObject) method).setAccessible(true);
    } catch (SecurityException iae) {
      throw Reflector.Util.sneakyThrow(iae);
    }
    int capacity = sizeof(in);
    if (capacity == -1) capacity = INITIAL_CAPACITY;
    final List<T> filtered = new ArrayList<T>(capacity);
    final Iterator<?> it = in.iterator();
    // ensure assignment to args element is safe
    if (args != NO_ARGS && args.getClass() != Object[].class) {
      Object[] oArgs = new Object[args.length];
      System.arraycopy(args, 0, oArgs, 0, args.length);
      args = oArgs;
    }
    boolean doAssign = (args != NO_ARGS && argPos >= 0 && argPos < args.length);
    int numParams = method instanceof Method
      ? ((Method) method).getParameterTypes().length
      : ((Constructor<?>) method).getParameterTypes().length;
    while (it.hasNext()) {
      Object elem = it.next();
      if (elem == null) continue;
      T result = null;
      if (doAssign) args[argPos] = elem;
      try {
        result = (T) (
          (method instanceof Method)
            ? ((Method) method).invoke(
                (argPos != -1) ? args[argPos] : elem,
                args.length == numParams
                  ? args
                  : args.length > numParams
                      ? Arrays.copyOfRange(args, 1, args.length)
                      : ArrayUtils.addAll(args, new Object[numParams - args.length])
              )
            : ((Constructor<?>) method).newInstance(args)
        );
        if (result == null) continue;
        filtered.add(result);
      } catch (RuntimeException ite) {
        throw Reflector.Util.sneakyThrow(ite);
      } catch (final ReflectiveOperationException ite) {
        if (ite instanceof InvocationTargetException) {
          throw Reflector.Util.sneakyThrow(
            ((InvocationTargetException) ite).getTargetException()
          );
        }
        Log.e("invokeAll", ite);
      }
    }
    return filtered;
  }
  
  @Extension
  public static <T> List<T> invokeAll(final Iterable<?> in, final String methodName)
  {
    return invokeAll(in, methodName, 0, NO_ARGS);
  }
  
  
  
  // === FILTERALL + INVOKEALL 2.0 ===
  
  @Extension
  public static <T> List<T> filterAll(final Object target,
    final String name, final Iterable<T> items)
  {
    Method method = null;
    final Object[] args = new Object[1];
    try {
      final List<Object> results = new ArrayList<>();
      for (final Object arg: items) {
        if (arg == null) continue;
        if (method == null) method = findApplicableMethod(target, name, arg);
        args[0] = arg;
        final Object result = method.invoke(target, args);
        if (isTrueOrNonEmpty(result)) {
          results.add(arg);
        }
      }
      return (List<T>) (List<?>) results;
    } catch (Throwable e) {
      e.addSuppressed(new Throwable("method: " + method + "; args: "
        + Arrays.asList(args)));
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  @Extension
  public static <T> List<T> filterAll(final Object target,
    final Iterable<T> items, final String name)
  {
    return filterAll(target, name, items);
  }
  
  @Extension
  public static <T> List<T> filterAll(final Iterable<T> items,
    final String name, final Object target)
  {
    return filterAll(target, name, items);
  }
  
  @Extension
  public static <T> List<T> filterAll(final T[] items,
    final String name, final Object target)
  {
    return filterAll(target, name, Arrays.asList(items));
  }
    
  @Extension
  public static <T> List<T> filterAll(final Object target,
    final String name,  final T[] items)
  {
    return filterAll(target, name, Arrays.asList(items));
  }

  @Extension
  public static <T> List<T> filterAllOnResult(final Object target,
    final String name,  final Iterable<T> items, final String filterName,
    final Object filterTarget)
  {
    return filterAllOnResult(target, name, items, filterTarget, filterName);
  }
  
  @Extension
  public static <T> List<T> filterAllOnResult(final Object target,
    final String name,  final Iterable<T> items, final Object filterTarget, 
    final String filterName)
  {
    Method method = null, predicate = null;
    final Object[] args = new Object[1];
    try {
      final List<T> results = new ArrayList<>();
      boolean subjectsReversed = false;
      for (final T arg: items) {
        if (arg == null) continue;
        if (method == null) method = findApplicableMethod(target, name, arg);
        args[0] = arg;

        final Object intermediateResult = method.invoke(target, args);
        if (predicate == null) {
          predicate = findApplicableMethod(
            filterTarget, filterName, intermediateResult
          );
          if (predicate == null) {
            predicate = findApplicableMethod(
              intermediateResult, filterName, filterTarget
            );
            if (predicate == null) throw new NoSuchMethodException(String.format(
              "mo applicable filter/predicate method could be found in either " +
              "(1) the class '%1$s', taking a '%2$s' argument, or " +
              "(1) the class '%2$s', taking a '%1$s' argument.",
              filterTarget.getClass().getSimpleName(),
              intermediateResult.getClass().getSimpleName()
            ));
            // else:
            subjectsReversed = true;
          }
        }
        
        // Have_Predicate
        final Object target2;
        args[0] = subjectsReversed ? filterTarget       : intermediateResult;
        target2 = subjectsReversed ? intermediateResult : filterTarget;
        final Object filterOutcome = predicate.invoke(target2, args);
        if (isTrueOrNonEmpty(filterOutcome)) {
          results.add(arg);
        }
      }
      return results;
    } catch (Throwable e) {
      e.addSuppressed(new Throwable("method: " + method + "; args: "
        + Arrays.asList(args)));
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  

  @Extension
  public static <T> List<T> invokeAll(final Object target,
    final String name, final Iterable<?> items)
  {
    Method method = null;
    final Object[] args = new Object[1];
    try {
      final List<T> results = new ArrayList<>();
      for (final Object arg: items) {
        if (arg == null) continue;
        if (method == null) method = findApplicableMethod(target, name, arg);
        args[0] = arg;
        final T result = (T) method.invoke(target, args);
        results.add(result);
      }
      return results;
    } catch (Throwable e) {
      e.addSuppressed(new Throwable("method: " + method + "; args: "
        + Arrays.asList(args)));
      throw Reflector.Util.sneakyThrow(e);
    }
  }


  
  

  public static Method findApplicableMethod(Object target,
    String methodName, Object arg)
  {
    final Class<?>[] types =
      new Class<?>[]{ target != null ? target.getClass() : Object.class };
    final List<Member> members = Reflect.getMembers(target.getClass());
    final List<Method> methods
      = CollectionUtil2.typeFilter(members, Method.class);
    for (final Iterator<Method> i = methods.iterator(); i.hasNext();) {
      final Method m = i.next();
      if (! methodName.equals(m.getName())) i.remove();
      else if (m.getParameterTypes().length != 1) i.remove();
    }
    final Method method =
         (Method) Reflect.findMostSpecificMethod(types, (List) methods);
    return (method != null)
      ? method
      : (methods.isEmpty()
          ? null
          : methods.get(0));
  }

  public static boolean isTrueOrNonEmpty(final Object result) {
    if (result == null) return false;
    if (result instanceof Boolean) return (boolean) result;
    if (result instanceof CharSequence)
      return (((CharSequence)result).length() != 0);
    if (result instanceof Number) return (((Number)result).intValue() != 0);
    if (result instanceof Iterable<?>)
      return (((Iterable<?>)result).iterator().hasNext());
    if (result instanceof Iterator<?>)
      return (((Iterator<?>)result).hasNext());
    if (result instanceof Enumeration<?>)
      return (((Enumeration<?>)result).hasMoreElements());
    if (result.getClass().isArray()) return (Array.getLength(result) != 0);
    if (result instanceof Map<?, ?>) return ! ((Map<?, ?>)result).isEmpty();
    if (result instanceof java8.util.Optional)
      return ((java8.util.Optional<?>) result).isPresent();
    if (result instanceof com.google.common.base.Optional<?>)
      return ((com.google.common.base.Optional<?>) result).isPresent();
    if ("java.util.Optional".equals(result.getClass().getName()))
      return Reflector.<Boolean>invokeOrDefault(result, "isPresent");
    if (result instanceof Reference<?>)
      return ((Reference<?>) result).get() != null;
    return true;
  }
  
  
  
  // ===
  
  
  @Extension
  public static <T> List<T> filter(Iterable<T> in, Matcher mchr) {
    int capacity = sizeof(in);
    if (capacity == -1) capacity = INITIAL_CAPACITY;
    List<T> filtered = new ArrayList<T>(capacity);
    for (T elem: in) {
      if (elem == null) continue;       
      try {
        String entry = elem.toString();
        if (entry.indexOf('\u001b') != -1) {
          entry = TextUtil.colorrm(entry);
        }
        if (mchr.reset(entry).find()) {
          filtered.add(elem);
        }
      } catch (Throwable e) {
        continue;
      }
    }
    return filtered;
  }
  
  @Extension
  public static <T> T[] filter(T[] in, Matcher mchr) {
    List<T> filtered = new ArrayList<T>(in.length);
    for (T elem: in) {
      if (elem == null) continue;       
      try {
        String entry = elem.toString();
        if (entry.indexOf('\u001b') != -1) {
          entry = TextUtil.colorrm(entry);
        }
        if (mchr.reset(entry).find()) {
          filtered.add(elem);
        }
      } catch (Throwable e) {
        continue;
      }
    }
    return (T[]) filtered.toArray(
      newArray(in.getClass().getComponentType(), 0)
    );
  }
  
  @Extension
  public static <T> List<T> filter(Iterable<T> in, Pattern ptrn) {
    return filter(in, ptrn.matcher(""));
  }
  
  @Extension
  public static <T> T[] filter(T[] in, Pattern ptrn) {
    return filter(in, ptrn.matcher(""));
  }
  
  @Extension
  public static <T> List<T> filter(Iterable<T> in, String regex) {
    return filter(in, Pattern.compile(regex, PATTERN_FLAGS));
  }
  
  @Extension
  public static <T> T[] filter(T[] in, String regex) {
    return filter(in, Pattern.compile(regex, PATTERN_FLAGS));
  }
  
  public static int sizeof(Object collection) {
    return sizeof(collection, 0);
  }
  
  public static int sizeof(Object collection, int depth) {
    if (depth > 2) return -1;
    if (collection == null) return 0;
    if (collection instanceof Collection<?>) {
      return ((Collection<?>) collection).size();
    }
    if (collection instanceof Map<?,?>) {
      return ((Map<?,?>) collection).size();
    }
    if (collection instanceof Iterable<?>) {
      final Iterable<?> b = (Iterable<?>) collection;
      int size = 0;
      for (final Iterator<?> i = b.iterator(); i.hasNext() && ++size > 0; i.next());
      return size;
    }
    if (collection instanceof Object[]) {
      return ((Object[]) collection).length;
    }
    if (collection instanceof Object[]) {
      return ((Object[]) collection).length;
    }
    Class<?> cls = collection.getClass();
    if (cls.isArray()) return Array.getLength(collection);
    for (Field fld: cls.getDeclaredFields()) {
      if ((fld.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }
      Class<?> type = fld.getType();       
      if (Collection.class.isAssignableFrom(type)
      ||  Map.class.isAssignableFrom(type)
      ||  type.isArray()) 
      {
        Object value = null;
        try {
          fld.setAccessible(true);
          value = fld.get(collection);
          if (value == null) continue;
          int size = sizeof(value, depth + 1);
          if (size != -1) return size;
        } catch (ReflectiveOperationException ex) { }
      }
    }
    return -1;
  }
  
  public static <T>
  T[] newArray(Class<T> componentType, int length) {
    return (T[]) Array.newInstance(componentType, length);
  }
  
  @Extension
   public static <I, O> O[] to(I[] inputs, Class<O> outputCls,
   Object... args) {
     return to(inputs, outputCls, false, args);
   }
   
   @Extension
   public static <I, O> List<O> to(Iterable<? extends I> inputs, 
   ClassIdentifier<O> outputCi, Object... args)
   { 
     return new ArrayList<O>(Arrays.<O>asList(
       (O[]) to(
         (I[]) CollectionUtil.<I>toArray(inputs),
         outputCi.getTargetClass(), 
         args
       )
     ));
   }
   
   @Extension
   public static <I, O> List<O> to(Iterable<? extends I> inputs, 
   ClassIdentifier<O> outputCi)
   { 
     return new ArrayList<O>(Arrays.<O>asList(
       (O[]) to(
         (I[]) CollectionUtil.<I>toArray(inputs),
         outputCi.getTargetClass(), 
         new Object[0]
       )
     ));
   }
   
   @Extension
   public static <I, O>
   O[] to(I[] inputs, ClassIdentifier<O> outputCi, Object... args)
   {
     return CollectionUtil.<O>sort(
       to(inputs, outputCi.getTargetClass(), args)
     );
   }
   
   @Extension
   public static <I, O>
   O[] to(I[] inputs, ClassIdentifier<O> outputCi) 
   {
     return CollectionUtil.<O>sort(
       to(inputs, outputCi.getTargetClass(), new Object[0])
     );
   }
   
   @Extension
   public static <I, O> O[] to(I[] inputs,
   ClassIdentifier<O> outputCi, boolean fatal, Object... args) 
   {
     return CollectionUtil.<O>sort(
       to(inputs, outputCi.getTargetClass(), fatal, args)
     );
   }
   
   @Extension
   public static <I> Class<?>[] toClasses(I[] inputs) {
     List<Class<?>> ret = new ArrayList<Class<?>>(inputs.length);
     char shorty;
     Class<?> cls;
     int inputIdx = -1;
     final StringBuilder sb = new StringBuilder(120);
     for (I elem: inputs) {
       inputIdx++;
       String name = elem instanceof String
         ? (String) elem
         : DEGENERIFY.reset(
             DECOLOR.reset(elem.toString()).replaceAll("")
           ).replaceAll("");
       if (DEBUG) dbg.printf("\n* input[%d]: %s\n", inputIdx, name);
       if (DEBUG) dbg.printf("  - name = \"%s\"\n", name);
       do {           
         int len = name.length();
         int lbkt = -1, lstart = -1;
         if (DEBUG) dbg.printf("  - lbkt = %d\n", lbkt);
         if (DEBUG) dbg.printf("  - len = %d\n", len);
         if (len == 1 ||
            (len >= 2 && 
            (name.charAt(0) != '[' || (name.indexOf(';') != -1 && 
                                       name.indexOf('L') != -1))))
         {
           lbkt = name.lastIndexOf('[');
           // lstart = (lbkt != -1) ? lbkt+1 : 0;
           sb.setLength(0);
           if (DEBUG) dbg.printf("  - branch 1\n");
           if (name.length() >= 3) {
             if (name.indexOf('[') == -1) {
               sb.append(name);
               if (sb.charAt(0) == 'L' && sb.charAt(len-1) == '$') {
                 sb.delete(len-1, len).delete(0, 1);
               }
             }
             int slashpos;
             while ((slashpos = sb.indexOf("/")) != -1) {
               sb.replace(slashpos, slashpos+1, ".");
             }
             name = sb.toString();
           }
           cls = DexVisitor.classForName(name);
           break;
         } else 
           if (name.length() >= 2
           &&  Character.isUpperCase(shorty = name.charAt(2)) 
           && name.indexOf(';') == -1 && name.charAt(0) == '[')
           // primitive arrays
         {
           sb.setLength(0);
           if (DEBUG) dbg.printf("  - branch 2\n");
           if (DEBUG) dbg.printf("  - shorty = %c\n", shorty);
           if (DexlibAdapter.PrimitiveTypes.containsKey(shorty)) {
             if (DEBUG) dbg.printf("  - containsKey = true\n");
             name = (sb.append('[').append(shorty).toString());
             if (DEBUG) dbg.printf("  - name = \"%s\"\n", name);
             if (shorty == 'V') {
               cls = Void.TYPE;
             } else {
               Class<?> arrCls = DexVisitor.classForName(
                 new StringBuilder(2)
                   .append('[').append(shorty).toString()
               );
               if (DEBUG) dbg.printf("  - arrCls = %s\n", arrCls);
               cls = arrCls.getComponentType();
               break;
             }
           }
           if (DEBUG) dbg.printf("  - containsKey = false\n");
           cls = DexVisitor.classForName(name);
           break;
         } else {
           if (DEBUG) dbg.printf("  - branch 3\n");
           cls = DexVisitor.classForName(name);
           break;
         }
       } while (false);
       // System.out.printf("name = [%s], cls = [%s]\n",
       //   name, cls);
       if (cls == null) continue;
       ret.add(cls);
     }
     return ret.toArray(new Class[0]);
   }
   
   @Extension
   public static <I, O> O[] to(I[] inputs, Class<O> outputCls,
   boolean fatal, Object... args) 
   {
     if (inputs == null) throw new IllegalArgumentException(
       "inputs == null"
     );
     if (outputCls == null) throw new IllegalArgumentException(
       "outputCls == null"
     );
     if (args == null) throw new IllegalArgumentException(
       "args == null"
     );
     if (outputCls.equals(Class.class) && args.length == 0) {
       return (O[]) toClasses(inputs);
     }
     if (inputs.length == 0) {
       return (O[]) Array.newInstance(outputCls, 0);
     }
     Class<?> inCls = inputs.getClass().getComponentType();
     if (DEBUG) dbg.printf("inCls: %s\n", Debug.ToString(inCls));
     Class<?> inCls2 = inCls;
     if (DEBUG) dbg.printf("inCls2: %s\n", Debug.ToString(inCls2));
     if (inCls == Object.class 
     && inputs.length > 0
     && inputs[0] != null) {
       inCls2 = inputs[0].getClass();
       if (DEBUG) dbg.printf("  --> inCls2: %s\n", Debug.ToString(inCls2));
     }
     if (DEBUG) dbg.printf(
       "to(\n  I[] inputs = %s,\n  Class<O> outputCls = %s,\n  "
       + "boolean fatal = %s,\n  , Object... args = %s\n);\n",
       Debug.ToString(inputs), Debug.ToString(outputCls),
       Boolean.valueOf(fatal), Debug.ToString(args)
     );
     List<O> outputs = Collections.emptyList();
     nextHandler:
     for (Method method: CollectionUtil2.class.getDeclaredMethods()) {
       if (DEBUG) dbg.println("* Considering: " + method.toGenericString());
       if (! ((method.getModifiers() & Modifier.STATIC) != 0 
       &&  method.isAnnotationPresent(ClassConverter.class)) ) continue;
       
       try {         
         ClassConverter ann = method.getAnnotation(ClassConverter.class);
         if (DEBUG) dbg.printf("ClassConverter ann = %s\n", 
           Debug.ToString(ann));
         Class<?>[] pTypes = method.getParameterTypes();
         if (DEBUG) dbg.printf("pTypes: %s\n", Debug.ToString(pTypes));
         Class<?> from   = pTypes[0];
         if (DEBUG) dbg.printf("from: %s\n", Debug.ToString(from));
         Class<?> to     = method.getReturnType();
         if (DEBUG) dbg.printf("to: %s\n", Debug.ToString(to));
         if (args.length != pTypes.length - 1) {
           if (DEBUG) dbg.printf(
             "> rejecting b/c args.length (%d) != pTypes.length (%d) -1\n",
             args.length, pTypes.length
           );
           continue;
         }
         if (! from.isAssignableFrom(inCls) 
         &&  ! from.isAssignableFrom(inCls2)) {
           if (DEBUG) dbg.printf(
             "> rejecting b/c from(%s) is neither assignable "
             + "from inCls(%s) nor inCls2(%s)\n",
             from, inCls, inCls2
           );
           continue;
         }
         if (! outputCls.isAssignableFrom(to)) {
           if (DEBUG) dbg.printf(
             "> rejecting b/c outputCls(%s) not assignableFrom to(%s)\n",
             outputCls, to
           );
           continue;
         }
         Object[] fnArgs = new Object[pTypes.length];
         System.arraycopy(args, 0, fnArgs, 1, fnArgs.length - 1);
         if (DEBUG) dbg.printf("fnArgs: %s\n", Debug.ToString(fnArgs));
         try {
           int i = -1, len = inputs.length;
           method.setAccessible(true);
           nextElement:
           while (++i < len) {
             try {
               fnArgs[0] = inputs[i];
               if (DEBUG) dbg.printf(
                 "invoke/fnArgs[0] = %s\n", Debug.ToString(fnArgs[0])
               );
               
               Object result = method.invoke(null, fnArgs);
               if (DEBUG) dbg.printf(
                 "invoke/result = %s\n", Debug.ToString(result)
               );
               
               if (result == null) continue nextElement;
               if (outputs.isEmpty()) {
                 outputs = new ArrayList<O>(inputs.length);
               }
               outputs.add((O) result);
             } catch (Throwable e) {
               errors.add(e);
               if (e instanceof IllegalArgumentException 
               &&  outputs.isEmpty()) 
               {
                 continue nextHandler;
               }
               if (DEBUG) e.printStackTrace();
               continue nextElement;
             }             
           }
         } catch (Throwable e) {
           errors.add(e);
           if (DEBUG) e.printStackTrace();
           if (outputs.isEmpty()) continue nextHandler;
           continue;
         }
         return (O[]) CollectionUtil.toArray(outputs);
       } catch (Throwable e) { 
         errors.add(e);
         if (DEBUG) e.printStackTrace();
         if (outputs.isEmpty()) continue;
       }
     }
     
     outputs = new ArrayList<O>(inputs.length);
     Constructor<O> ctor = null;
     Object[] theArgs = new Object[args.length + 1];
     System.arraycopy(args, 0, theArgs, 1, args.length);
     
     for (I elem: inputs) {
       theArgs[0] = elem;
       if (ctor == null) {
         try {
           ctor = Factory.findBestMatch(
             (Constructor<O>[]) (Object)
               outputCls.getDeclaredConstructors(),
             theArgs
           );
         } catch (Throwable e) { throw new RuntimeException(e); }
         if (ctor == null) {
           throw new RuntimeException(String.format(
             "No constructor fits: %s.<init>(%s)",
             outputCls.getName(),
             StringUtils.join(args, ", ")
           ));
         }
         try {
           ctor.setAccessible(true);
         } catch (Throwable e) { e.printStackTrace(); }
       } // if ctor == null
       O obj;
       try {
         obj = (O) ctor.newInstance((Object[]) theArgs);
       } catch (Throwable e) { 
         errors.add(e);
         if (fatal) Reflector.Util.sneakyThrow(e);
         if (DEBUG) e.printStackTrace();
         continue; 
       }
       outputs.add(obj);
     }
     
     return outputs.toArray(
       (O[]) Array.newInstance(outputCls, outputs.size())
     );
   }
   
   
  // === CONVERTERS ===
  
  @ClassConverter
  public static CtClass toCtClass(Class<?> cls) throws Throwable {
    return toCtClass(cls.getName());
  }
  
  @ClassConverter
  public static CtClass toCtClass(String className) throws Throwable {
    return CollectionUtil.getClassPool().get(className);
  }
  
  @ClassConverter
  public static String toString(Class<?> cls) {
    String className = cls.getName();
    try {
      return DexVisitor.typeToName(className);
    } catch (Throwable e) {
      return className;
    }
  }
  
  @ClassConverter
  public static String toString(ClassIdentifier<?> ci) {
    return toString(ci.getTargetClass());
  }
  
  @ClassConverter
  public static <N extends Number> 
  N numberValue(String numberStr, Class<N> numberClass)
    throws Throwable 
  {
    Method valueOf 
      = Reflect.getMember(numberClass, "valueOf", String.class);
    return (N) valueOf.invoke(null, numberStr);
  }
  
  @ClassConverter
  public static <N extends Number> 
  N numberValue(String numberStr, int base, Class<N> numberClass)
    throws Throwable 
  {
    Method valueOf = Reflect.getMember(
      numberClass, "valueOf", String.class, Integer.TYPE
    );
    return (N) valueOf.invoke(null, numberStr, Integer.valueOf(base));
  }
  
  @ClassConverter
  public static Map.Entry<String, byte[]> toClassBytes(Class<?> cls) 
    throws Throwable
  {    
    String resName = cls.getName().replace('.', '/').concat(".class");
    Iterable<URL> urls = ClassLoaders.getResources(
      (BaseDexClassLoader)
      Thread.currentThread().getContextClassLoader(), resName      
    );
    for (URL url: urls) {
      InputStream is = null;
      try {
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        is = conn.getInputStream();
        return new SimpleEntry(resName, Streams.readFully(is));
      } catch (Throwable ioe) {
        errors.add(ioe);
        if (DEBUG) ioe.printStackTrace();
        continue;
      } finally {
        if (is != null) IOStream.closeQuietly(is);
      }
    }
    return null;
  }
  
  @ClassConverter
  public static Long longValue(String longStr) {
    return Long.valueOf(longStr, 10);
  }
  
  @ClassConverter
  public static Integer intValue(String intStr) {
    return Integer.valueOf(intStr, 10);
  }
  
  @ClassConverter
  public static CtMethod toCtMethod(Member member) throws Throwable {
    if (!(member instanceof Method)) return null;
    Method method = (Method) member;
    ClassPool cp = CollectionUtil.getClassPool();
    Class<?>[] paramTypes = method.getParameterTypes();
    CtClass[] ctParamTypes = new CtClass[paramTypes.length];
    for (int i=0; i<paramTypes.length; i++) {
      ctParamTypes[i] = toCtClass(paramTypes[i]);
    }
    CtClass declaringCtClass = toCtClass(method.getDeclaringClass());
    return declaringCtClass.getDeclaredMethod(
      method.getName(), ctParamTypes
    );
  } 
  
  
  
  protected static LazyMember<Field> MATCHER_INPUT = LazyMember.of(
    "input", Matcher.class);
  
  protected static LazyMember<Field> MATCHER_FOUND_MATCH = LazyMember.of(
    "matchFound", Matcher.class);
  
  
  @Extension
  public static List<String> values(Matcher dmchr, boolean includeGroupZero) 
  {
    final String input = MATCHER_INPUT.getValue(dmchr);
    final boolean matchFound 
      = MATCHER_FOUND_MATCH.<Boolean>getValue(dmchr).booleanValue(); 
    if (input == null) throw new IllegalArgumentException(String.format(
      "Specified Matcher has no input text; Pattern: \"%s\"",
      dmchr.pattern()
    ));
    if (! matchFound) { 
      if (! dmchr.find()) { 
        throw new IllegalStateException(
          "No successful match was found, and a subsequent call to find() "
          + "on the Matcher failed to produce one."
        );
      };
    };
    final int groupCount = dmchr.groupCount();
    List<String> list
      = new ArrayList<>( (groupCount-1) + (includeGroupZero? 1: 0) ); 
    for (int i=0, len=groupCount+1; i<len; i+=1) {
      final String val = dmchr.group(i);
      if (i != 0 || includeGroupZero) list.add((val != null)? val: "");
    }
    return list;
  }
    
  @Extension
  public static List<String> values(Matcher dmchr) { 
    return values(dmchr, false);
  }
  
  @Extension
  public static StringBuilder join(final Iterable<?> itb, final String sep) {
    final StringBuilder sb = new StringBuilder(256);
    return Joiner.on(sep).appendTo(sb, itb);
  }
  
  @Extension
  public static StringBuilder join(final CharSequence[] strs, final String sep) {
    final StringBuilder sb = new StringBuilder(256);
    return Joiner.on(sep).appendTo(sb, Arrays.asList(strs));
  }
  
  @Extension
  public static StringBuilder join(final Iterable<?> itb, final char sep) {
    final StringBuilder sb = new StringBuilder(256);
    return Joiner.on(String.valueOf(sep)).appendTo(sb, itb);
  }
  
  @Extension
  public static StringBuilder join(final CharSequence[] strs, final char sep) {
    final StringBuilder sb = new StringBuilder(256);
    return Joiner.on(String.valueOf(sep)).appendTo(sb, Arrays.asList(strs));
  }
  
  @Extension
  public static StringBuilder join(final Object[] strs, final Object sep) {
    final String strSep;
    if (sep instanceof bsh.Primitive) {
      strSep = ((bsh.Primitive) sep).getValue().toString();
    } else if (sep instanceof String) {
      strSep = (String) sep;
    } else if (sep instanceof CharSequence) {
      strSep = ((CharSequence) sep).toString();
    } else if (sep instanceof Character) {
      strSep = ((Character) sep).toString();
    } else {
      strSep = (sep != null)? sep.toString(): "";
    }
    final StringBuilder sb = new StringBuilder(256);
    return CollectionUtil2.join(Arrays.asList(strs), (String) strSep);
  }
}