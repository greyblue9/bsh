package org.d6r;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.d6r.ObjectUtil.None;
import org.d6r.Reflector;
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import java8.util.Optional;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.lang3.ClassUtils;
import java.io.Serializable;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.Token;
import org.antlr.runtime.CommonToken;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java8.util.concurrent.ForkJoinPool;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import gnu.trove.set.TIntSet;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ObjectUtil {

  public static String TAG = "ObjectUtil";

  static final Map<Class<?>, List<Field>> cache = new SoftHashMap<>();

  static final int STATIC = Modifier.STATIC;

  public static boolean VERBOSE = false;

  static long DEFAULT_TIMEOUT_MS = 75000;
  
  
  
  public static <T extends V, U extends V, V> V firstNonNull(T o1, U o2) {
    if (o1 != null) return o1;
    return o2;
  }
  
  
  public static List<Field> allFields(final Class<?> cls, final boolean includeStatic)
  {
    if (cls == null)
      throw new IllegalArgumentException(String.format("allFields() cls == null\n"));
    // System.err.printf("begin: %s\n", cls);
    if (cls == Object.class)
      return Collections.emptyList();
    List<Field> retList = cache.get(cls);
    Object __sls = null;
    if (retList == null) {
      List<Object[]> sls = new ArrayList<>();
      List<Field> allFields = new ArrayList<>();
      Class<?> c = cls;
      while (c != Object.class && c != null && !c.isPrimitive() && !c.isArray()) {
        final Field[] flds = c.getDeclaredFields();
        int sizeBefore = allFields.size();
        for (int i = 0, len = flds.length; i < len; ++i) {
          final Field fld = flds[i];
          if (!includeStatic && (fld.getModifiers() & STATIC) != 0)
            continue;
          fld.setAccessible(true);
          allFields.add(fld);
        }
        ;
        sls.add(new Object[] { c, sizeBefore });
        c = c.getSuperclass();
      }
      final int size = allFields.size();
      for (final Object[] sl : sls) {
        Class<?> clazz = (Class<?>) sl[0];
        int listBegin = (int) sl[1];
        List<Field> clazzAndSupFields = allFields.subList(listBegin, size);
        if (listBegin == 0)
          retList = clazzAndSupFields;
        cache.put(clazz, clazzAndSupFields);
      }
    }
    if (retList == null) {
      throw new RuntimeException(String.format("retList == null: cls = %s; sls = %s\n", cls, __sls));
    }
    // System.err.printf("end: %s\n", cls);
    return retList;
  }

  public static class None {

    public static final None NONE = new None();

    private None() {
    }

    public static <T> T get() {
      return (T) (Object) NONE;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object o) {
      return o == null;
    }

    @Override
    public String toString() {
      return EMPTY;
    }
  }

  public static final Object NONE = None.get();

  public static final Class<None> NO_TYPE = (Class<None>) None.class;

  public static final String EMPTY = "";

  public static <T> T coalesce(T o) {
    T defaultVal = (T) (Object) NONE;
    if (o instanceof Class<?>)
      defaultVal = (T) (Object) NO_TYPE;
    else if (o instanceof CharSequence) {
      defaultVal = (T) (Object) EMPTY;
    } else if (o instanceof Number) {
      defaultVal = (T) (Object) Integer.valueOf(0);
    }
    return o != null ? o : defaultVal;
  }

  public static int coalesceHash(Object obj) {
    return coalesce(obj).hashCode();
  }

  public static int coalesceHashCode(Object... elems) {
    int a = 0;
    for (int i = 0, len = elems.length; i < len; ++i) {
      final Object o = coalesce(elems[i]);
      final int h;
      try {
        if (o == null) {
          h = 0;
        } else if (o.getClass().isArray()) {
          if (o instanceof Object[])
            h = Arrays.hashCode((Object[]) o);
          else if (o instanceof byte[])
            h = Arrays.hashCode((byte[]) o);
          else if (o instanceof char[])
            h = Arrays.hashCode((char[]) o);
          else if (o instanceof double[])
            h = Arrays.hashCode((double[]) o);
          else if (o instanceof float[])
            h = Arrays.hashCode((float[]) o);
          else if (o instanceof int[])
            h = Arrays.hashCode((int[]) o);
          else if (o instanceof long[])
            h = Arrays.hashCode((long[]) o);
          else if (o instanceof short[])
            h = Arrays.hashCode((short[]) o);
          else if (o instanceof boolean[])
            h = Arrays.hashCode((boolean[]) o);
          else {
            h = Arrays.hashCode((Object[]) Reflector.invokeOrDefault(ArrayUtils.class, "toObject", (Object) o));
          }
        } else {
          h = o.hashCode();
        }
      } catch (Throwable hcError) {
        System.err.printf("[WARN] org.d6r.ObjectUtils.coalesceHashCode: an object of type " + "'%s' threw a %s while computing its hashCode()", (o != null) ? ClassInfo.typeToName(o.getClass().getName()) : "<null>", ExceptionUtils.getRootCause(hcError));
        a ^= ((((a *= 37) - 1) << 16) * (~(((a *= 1371) - 1) >> 16)));
        continue;
      }
      a ^= ((((a *= 37) + h) << 16) * (~(((a *= 1371) + h) >> 16)));
    }
    return a;
  }

  @bsh.operators.Extension
  public static <T> T getOrElse(Object first, T second) {
    if (first == null)
      return second;
    if (first instanceof Optional<?>) {
      final boolean firstHasValue = ((Optional<?>) first).isPresent();
      if (!(second instanceof Optional)) {
        final T unwrappedFirst = (firstHasValue) ? (((Optional<T>) first).get()) : null;
        return unwrappedFirst != null ? unwrappedFirst : second;
      } else {
        if (firstHasValue)
          return (T) first;
        return second;
      }
    }
    if (first instanceof Collection<?>) {
      final boolean firstHasValue = !((Collection<?>) first).isEmpty();
      if (!(second instanceof Collection)) {
        final T unwrappedFirst = (firstHasValue) ? ((Collection<T>) first).iterator().next() : null;
        return unwrappedFirst != null ? unwrappedFirst : second;
      } else {
        if (firstHasValue)
          return (T) first;
        return second;
      }
    }
    if (first instanceof Iterable) {
      final Iterator<T> it = ((Iterable<T>) first).iterator();
      final boolean firstHasValue = it.hasNext();
      if (!(second instanceof Iterable)) {
        final T unwrappedFirst = (firstHasValue) ? it.next() : null;
        return unwrappedFirst != null ? unwrappedFirst : second;
      } else {
        if (firstHasValue)
          return (T) first;
        return second;
      }
    }
    if (first != null) {
      if (second != null) {
        if (second.getClass().isInstance(first)) {
          return (T) first;
        } else {
          Throwable cause = null;
          final Set<Object> attempted = new LinkedHashSet<>();
          for (final Class<?> iface : ClassInfo.getInterfaces(second.getClass())) {
            attempted.add("(for target interface " + ClassInfo.simplifyName(ClassInfo.typeToName(iface.getName())) + ")");
            attempted.add("`first'");
            if (iface.isInstance(first) && iface.isInterface()) {
              if (iface.equals(Serializable.class))
                continue;
              return (T) first;
            }
          }
          try {
            Class<T> classOfT = (Class<T>) (Class<?>) second.getClass();
            attempted.add("(for target class " + ClassInfo.simplifyName(ClassInfo.typeToName(classOfT.getName())) + ")");
            if (!classOfT.equals(Object.class)) {
              attempted.add("`first' as " + ClassInfo.simplifyName(ClassInfo.typeToName(classOfT.getName())));
              if (classOfT.isInstance(first))
                return classOfT.cast(first);
            }
            final List<Object> objs = Reflect.searchObject(first, Object.class, false, 0, 4);
            outer: while (classOfT != null) {
              for (Object obj : objs) {
                if (obj == null || obj instanceof Object)
                  continue;
                attempted.add("`first' member of type " + ClassInfo.simplifyName(ClassInfo.typeToName(obj.getClass().getName())));
                if (classOfT.isInstance(obj))
                  return classOfT.cast(obj);
              }
              classOfT = (Class<T>) (Class<?>) classOfT.getSuperclass();
            }
          } catch (ClassCastException cce) {
            cause = cause != null ? cause : cce;
          }
          UnsupportedOperationException uoe = new UnsupportedOperationException(String.format("Cannot find a common type between '%s' and '%', nor an appropriate " + "conversion to obtain an instance from the first argument that is " + "compatible with the second. Tried: { %s }", ClassInfo.typeToName(first.getClass()), ClassInfo.typeToName(second.getClass()), StringUtils.join(attempted, ", ")));
          if (cause != null)
            uoe.initCause(cause);
          throw uoe;
        }
      } else {
        return (T) first;
      }
    } else {
      return second;
    }
  }

  /*
  public class HasValueResult<T, V> {    
    static Predicate<?> NONE_PREDICATE = new Predicate<Object>() { @Override 
      public boolean test(Object o) { return false; } };
    static Predicate<?> SONE_PREDICATE = new Predicate<Object>() { @Override 
      public boolean test(Object o) { return  true; } };
    
    public final boolean isKnown;
    public final Boolean hasValue;
    public final Function<T, V> getValueFunction;
    public final Predicate<T> hasValuePredicate;
    private final T input;
    
    private HasValueResult(T input) {
      this.input = input;
      this.hasValueFunction
        = getHasValueForType(input != null? input.getClass(): Void.TYPE);
      this.getValueFunction = getGetValueForType(Void.TYPE);
      
    }
    
    public static <T, ?> HasValueResult<T, ?> of(T object) {
      HasValueResult<T, ?> result = new HasValueResult<T, Void>();
      result.input = object;
      
    }
    
  
  public static  tryIsEmpty()
  */
  public static <T> Collection<T> searchObject(Object obj, Class<T> searchFor) {
    return searchObject(obj, searchFor, false);
  }

  public static <T> Collection<T> searchObject(Object obj, Class<T> searchFor, boolean includeStatic) {
    return searchObject(obj, searchFor, includeStatic, 0, 5);
  }

  public static class PropertyPath {

    public static final PropertyPath ROOT = new PropertyPath(null, "", null);

    private PropertyPath parent;

    private String name;

    private Object data;

    private String opString;

    public PropertyPath(PropertyPath parent, String name, Object data) {
      this.parent = parent != null ? parent : ROOT;
      this.name = name;
      this.data = data;
    }

    public static PropertyPath root() {
      return ROOT;
    }

    public PropertyPath object(Object obj, Object data) {
      if (obj == null)
        return new PropertyPath(this, "{null}", null);
      String name;
      if (obj instanceof CharSequence) {
        name = obj.toString();
      } else {
        name = String.format("{%s}", ClassInfo.typeToName(obj.getClass().getName()));
      }
      return new PropertyPath(this, name, data);
    }

    public PropertyPath field(Field fld, Object data) {
      return new PropertyPath(this, ".".concat(fld.getName()), data);
    }

    public PropertyPath index(int index, Object data) {
      return new PropertyPath(this, String.format("[%d]", index), data);
    }

    public String toString() {
      if (parent == null || parent == this || parent == ROOT)
        return name;
      return parent.toString().concat(this.name);
    }
  }

  public static <T> List<Pair<PropertyPath, T>> searchObjectPath(final Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth) {
    int idx = 0;
    Deque<Object> q = new ArrayDeque<Object>(64);
    Deque<Integer> d = new ArrayDeque<Integer>(64);
    Set<Integer> visited = new HashSet<Integer>(640);
    List<Object> al = new ArrayList<Object>(32);
    final PropertyPath obj_propertyPath = PropertyPath.root().object(obj, obj);
    TIntObjectMap<PropertyPath> pathMap = new TIntObjectHashMap<PropertyPath>();
    q.offerLast(obj);
    pathMap.put(System.identityHashCode(obj), obj_propertyPath);
    d.offerLast(depth);
    visited.add(System.identityHashCode(obj));
    while (!q.isEmpty()) {
      /*if (dots++ % 10 == 0) System.err.printf("%c[0m.", 0x1b); */
      Object o = q.pollFirst();
      PropertyPath o_propertyPath = pathMap.get(System.identityHashCode(o));
      depth = d.pollFirst().intValue();
      if (o == null)
        continue;
      Class<?> cls = o.getClass();
      if (searchFor.isAssignableFrom(cls)) {
        al.add(Pair.of(o_propertyPath, o));
      }
      if (cls.isPrimitive())
        continue;
      if (cls.isArray()) {
        if (ClassUtils.isPrimitiveOrWrapper(cls.getComponentType())) {
          continue;
        }
        if (!cls.getComponentType().isPrimitive()) {
          int i = -1;
          for (Object v : (Object[]) o) {
            PropertyPath v_propertyPath = o_propertyPath.index(++i, v);
            pathMap.put(System.identityHashCode(v), v_propertyPath);
            if (v == null || visited.contains(System.identityHashCode(v)))
              continue;
            if (depth + 1 < maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(depth + 1));
            }
            visited.add(System.identityHashCode(v));
          }
          continue;
        }
      }
      do {
        for (Field f : cls.getDeclaredFields()) {
          if (!includeStatic && (f.getModifiers() & Modifier.STATIC) != 0)
            continue;
          try {
            f.setAccessible(true);
            Object v = f.get(o);
            PropertyPath v_propertyPath = o_propertyPath.field(f, v);
            pathMap.put(System.identityHashCode(v), v_propertyPath);
            if (v == null || visited.contains(System.identityHashCode(v)))
              continue;
            if (depth + 1 < maxdepth) {
              q.offerFirst(v);
              d.offerFirst(Integer.valueOf(depth + 1));
            }
            visited.add(System.identityHashCode(v));
          } catch (Throwable ex) {
            ex.printStackTrace();
          }
        }
        cls = cls.getSuperclass();
      } while (cls != Object.class && cls != null);
    }
    return (List<Pair<PropertyPath, T>>) (List<?>) al;
  }

  public static <T> Collection<T> searchObject(Object obj, Class<T> searchFor, int maxdepth) {
    return searchObject(obj, searchFor, false, 0, maxdepth);
  }

  public static <U> Collection<U> searchObject(Object obj, Class<U> searchFor, 
  boolean includeStatic, int mindepth, int maxdepth) {
    return ObjectUtil.<U>searchObject(
      // obj, searchFor, includeStatic, mindepth, maxdepth, DEFAULT_TIMEOUT_MS
         obj, searchFor, includeStatic, mindepth, maxdepth, (long) DEFAULT_TIMEOUT_MS
    );
  }
  

    public static <T> Collection<T> searchObject(Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth, long timeout) {
      ExecutorService es = Executors.newFixedThreadPool(1);
      try {
        ObjectSearch<T> c = new ObjectSearch(obj, searchFor, includeStatic, depth, maxdepth);
        long start = System.nanoTime();
        Future<Collection<T>> f = es.submit(c);
        // Log.i(TAG, es);
        try {
          return f.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException iex) {
          long stop = System.nanoTime();
          Thread.currentThread().interrupt();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", iex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = iex;
          f.cancel(true);
          return c.result;
        } catch (ExecutionException eex) {
          long stop = System.nanoTime();
          if (Thread.currentThread().interrupted()) {
            Thread.currentThread().interrupt();
          }
          eex.printStackTrace();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", eex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = eex;
          f.cancel(true);
          return c.result;
        } catch (TimeoutException tex) {
          long stop = System.nanoTime();
          if (Thread.currentThread().interrupted()) {
            Thread.currentThread().interrupt();
          }
          tex.printStackTrace();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", tex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = tex;
          f.cancel(true);
          return c.result;
        }
      } finally {
        es.shutdownNow();
      }
    }


  static final boolean DEBUG_STACK_TRACES = "true".equals(System.getProperty("printStackTrace"));

  public static <V, R> R cloneFields(V p, R xp) {
    int cloned = 0;
    Class<?> cls = p.getClass();
    try {
      while (cls != null && cls != Object.class) {
        for (Field f : cls.getDeclaredFields()) {
          try {
            f.setAccessible(true);
            f.set(xp, f.get(p));
            cloned++;
          } catch (Throwable e) {
            try {
              Field f2 = xp.getClass().getDeclaredField(f.getName());
              f2.setAccessible(true);
              f2.set(xp, f.get(p));
              cloned++;
            } catch (Throwable e2) {
              continue;
            }
            continue;
          }
        }
        // fields
        cls = cls.getSuperclass();
      }
    } catch (Throwable e) {
      if (DEBUG_STACK_TRACES)
        e.printStackTrace();
    }
    ;
    return (R) xp;
  }

  public abstract static class Search<T, U> implements Callable<U> {

    final Object resultSync = new Object();

    Object obj;

    Class<T> searchFor;

    boolean includeStatic;

    int depth;

    int maxdepth;

    Throwable exception;

    volatile U result;

    @Override
    public String toString() {
      int count;
      synchronized (resultSync) {
        count = (result instanceof Collection) ? ((Collection) result).size() : -1;
      }
      return String.format("ObjectSearch{ for %s.class, depth: %d/%d, results: %s }", searchFor.getSimpleName(), depth, maxdepth, count >= 0 ? Integer.toString(count) : "-");
    }

    @Override
    public abstract U call();
  }

  public static class ObjectSearch<T> extends Search<T, Collection<T>> {

    public ObjectSearch(Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth) {
      this.obj = obj;
      this.searchFor = searchFor;
      this.includeStatic = includeStatic;
      this.depth = depth;
      this.maxdepth = maxdepth;
    }

    @Override
    public Collection<T> call() {
      int idx = 0;
      Deque<Object> q = new ArrayDeque<Object>(64);
      Deque<Integer> d = new ArrayDeque<Integer>(64);
      // Set<Integer> visited = new HashSet<Integer>(640); 
      final TIntSet visited = new TIntHashSet(8192);
      final List<Object> al = new ArrayList<Object>(32);
      //synchronized (resultSync) { 
      // this.result = (List<T>) (List<?>) al;
      // }
      q.offerLast(obj);
      d.offerLast(depth);
      int iter = 0;
      boolean stop = false;
      while (!q.isEmpty()) {
        if ((++iter % 100) == 0) {
          synchronized (resultSync) {
            this.result = (List<T>) (List<?>) al;
          }
        }
        if (Thread.currentThread().interrupted()) {
          stop = true;
          System.err.printf("interrupted; depth = %d/%d", depth, maxdepth).flush();
          synchronized (resultSync) {
            this.result = (List<T>) (List<?>) al;
          }
          Thread.currentThread().interrupt();
          break;
        }
        /*if (dots++ % 10 == 0) System.err.printf("%c[0m.", 0x1b); */
        final Object o = q.pollFirst();
        Integer identity = System.identityHashCode(o);
        if (visited.contains(identity))
          continue;
        visited.add(identity);
        depth = d.pollFirst().intValue();
        if (o == null)
          continue;
        else if (searchFor.isInstance(o))
          al.add(o);
        Class<?> cls = o.getClass();
        if (cls.isPrimitive() || ClassUtils.isPrimitiveOrWrapper(cls) || (cls.isArray() && ClassUtils.isPrimitiveOrWrapper(cls.getComponentType()))) {
          continue;
        }
        if (depth == maxdepth)
          continue;
        if (cls.isArray()) {
          for (int i = 0, len = Array.getLength(o); i < len; ++i) {
            Object elem = Array.get(o, i);
            if (elem == null)
              continue;
            q.offer(elem);
            d.offer(Integer.valueOf(depth + 1));
          }
          continue;
        }
        for (final Field fld: allFields(cls, includeStatic)) {
          try {
            final Object fldval = fld.get(o);
            if (fldval == null) continue;
            q.offer(fldval);
            d.offer(Integer.valueOf(depth + 1));
          } catch (IllegalAccessException iae) {
            iae.printStackTrace();
            System.err.println(iae + " (field: " + fld + ")");
          }
        }
      }
      // if (iter >= 1000) System.err.print("\n");
      synchronized (resultSync) {
        this.result = (List) al;
      }
      return (Collection<T>) (Object) result;
    }

    public static class PathSearch<T> extends Search<T, List<Pair<PropertyPath, T>>> {

      public PathSearch(Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth) {
        this.obj = obj;
        this.searchFor = searchFor;
        this.includeStatic = includeStatic;
        this.depth = depth;
        this.maxdepth = maxdepth;
      }

      @Override
      public List<Pair<PropertyPath, T>> call() {
        int idx = 0;
        Deque<Object> q = new ArrayDeque<Object>(64);
        Deque<Integer> d = new ArrayDeque<Integer>(64);
        // Set<Integer> visited = new HashSet<Integer>(640); 
        TIntSet visited = new TIntHashSet(8192);
        List<Object> al = new ArrayList<Object>(32);
        final PropertyPath obj_propertyPath = PropertyPath.root().object(obj, obj);
        // Map<Integer, PropertyPath> pathMap = new HashMap<>();
        TIntObjectMap<PropertyPath> pathMap = new TIntObjectHashMap<PropertyPath>();
        //synchronized (resultSync) { 
        result = (List) al;
        // }
        q.offerLast(obj);
        pathMap.put(System.identityHashCode(obj), obj_propertyPath);
        d.offerLast(depth);
        visited.add(System.identityHashCode(obj));
        byte iter = 0;
        boolean stop = false;
        while (!q.isEmpty()) {
          if ((++iter) == 0) {
            {
              this.result = (List) al;
            }
            if (Thread.currentThread().interrupted()) {
              stop = true;
              System.err.printf("interrupted; depth = %d/%d", depth, maxdepth).flush();
              this.result = (List) al;
              Thread.currentThread().interrupt();
              break;
            }
            /*if (dots++ % 10 == 0) System.err.printf("%c[0m.", 0x1b); */
            Object o = q.pollFirst();
            PropertyPath o_propertyPath = pathMap.get(System.identityHashCode(o));
            depth = d.pollFirst().intValue();
            if (o == null)
              continue;
            Class<?> cls = o.getClass();
            if (searchFor.isAssignableFrom(cls)) {
              al.add(Pair.of(o_propertyPath, o));
            }
            if (cls.isPrimitive())
              continue;
            if (cls.isArray()) {
              Class cmp = cls.getComponentType();
              
              if (cmp.isPrimitive() || ClassUtils.isPrimitiveOrWrapper(cmp)) {
                continue;
              }

                int i = -1;
                for (Object v : (Object[]) o) {
                  PropertyPath v_propertyPath = o_propertyPath.index(++i, v);
                  pathMap.put(System.identityHashCode(v), v_propertyPath);
                  if (v == null || visited.contains(System.identityHashCode(v)))
                    continue;
                  if (depth + 1 < maxdepth) {
                    q.offerFirst(v);
                    d.offerFirst(Integer.valueOf(depth + 1));
                  }
                  visited.add(System.identityHashCode(v));
                }
                continue;
            }
            do {
              for (Field f : cls.getDeclaredFields()) {
                if (!includeStatic && (f.getModifiers() & Modifier.STATIC) != 0)
                  continue;
                try {
                  f.setAccessible(true);
                  Object v = f.get(o);
                  PropertyPath v_propertyPath = o_propertyPath.field(f, v);
                  pathMap.put(System.identityHashCode(v), v_propertyPath);
                  if (v == null || visited.contains(System.identityHashCode(v)))
                    continue;
                  if (depth + 1 < maxdepth) {
                    q.offerFirst(v);
                    d.offerFirst(Integer.valueOf(depth + 1));
                  }
                  visited.add(System.identityHashCode(v));
                } catch (Throwable ex) {
                  ex.printStackTrace();
                }
              }
              cls = cls.getSuperclass();
            } while (cls != Object.class && cls != null);
          }
          synchronized (resultSync) {
            this.result = (List) al;
          }
          return (List<Pair<PropertyPath, T>>) (List<?>) al;
        }
        synchronized (resultSync) {
          this.result = (List) al;
        }
        return (List<Pair<PropertyPath, T>>) (List<?>) al;
      }
    }

    public static <T> List<Pair<PropertyPath, T>> searchObjectPath(Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth, long timeout) {
      ExecutorService es = Executors.newFixedThreadPool(1);
      try {
        PathSearch<T> c = new PathSearch(obj, searchFor, includeStatic, depth, maxdepth);
        long start = System.nanoTime();
        Future<List<Pair<PropertyPath, T>>> f = es.submit(c);
        System.err.println(f);
        System.err.println(es);
        try {
          return f.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException iex) {
          long stop = System.nanoTime();
          Thread.currentThread().interrupt();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", iex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = iex;
          f.cancel(true);
          return c.result;
        } catch (ExecutionException eex) {
          long stop = System.nanoTime();
          if (Thread.currentThread().interrupted()) {
            Thread.currentThread().interrupt();
          }
          eex.printStackTrace();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", eex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = eex;
          f.cancel(true);
          return c.result;
        } catch (TimeoutException tex) {
          long stop = System.nanoTime();
          if (Thread.currentThread().interrupted()) {
            Thread.currentThread().interrupt();
          }
          tex.printStackTrace();
          System.err.printf("Task stopped by %s in call to %s.get() after %d milliseconds\n", tex.getClass().getName(), f.getClass().getName(), stop - start);
          c.exception = tex;
          f.cancel(true);
          return c.result;
        }
      } finally {
        es.shutdownNow();
      }
    }

    public static <T> Future<List<Pair<PropertyPath, T>>> searchObjectPathAsync(Object obj, Class<T> searchFor, boolean includeStatic, int depth, int maxdepth) {
      ExecutorService es = ForkJoinPool.commonPool();
      PathSearch<T> c = new PathSearch(obj, searchFor, includeStatic, depth, maxdepth);
      Future<List<Pair<PropertyPath, T>>> f = es.submit(c);
      return f;
    }
  }
  
  
  
  
  
  /*public static String encodeObject(final Object object) {
    if (object == null) throw new IllegalArgumentException("object == null");
    
    return SimpleCodec.encode(
      SimpleCodec.getXStream()
                 .toXML(object)
                 .getBytes(StandardCharsets.UTF_8)
    );
  }
  
  
  public static <T> T decodeObject(final CharSequence encoded) {
    if (encoded == null) throw new IllegalArgumentException("encoded == null");
    final String encodedString = (encoded instanceof String)
       ? (String) encoded
       : encoded.toString();
    
    final Object thawed = SimpleCodec.getXStream().fromXML(
      new String(SimpleCodec.decode(str), StandardCharsets.UTF_8)
    );
    
    // Everything will throw ConcurrentModificationExceptions without this step
    //
    for (final Class<?> modTrackingType: new Class<?>[]{
           Iterable.class, Map.class, Iterator.class, Enumeration.class
         })
    {
      boolean errorPrinted = false;
      for (final Object inst: searchObject(thawed, modTrackingType, false, 0, 50)) {
        try {
          Reflect.setfldval(inst, "modCount", 0);
        } catch (final Error | ReflectiveOperationException e) {
          if (!errorPrinted) {
            errorPrinted = true;
            new RuntimeException(String.format(
              "Setting modCount on %s caused %s", ClassInfo.getSimpleName(inst), e
            ), e).printStackTrace();
          }
        }
      }
      return thawed;
    }
  }*/
  
  
}


