package org.d6r;
import com.google.common.collect.Iterators;
import org.apache.commons.collections4.IteratorUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableBiMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.RandomAccess;
import java.io.Serializable;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import org.apache.commons.lang3.ClassUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import bsh.operators.Extension;
import java.util.Queue;
import java.util.ArrayDeque;


public class RealArrayMap<K, V>
  implements Map<K, V>, RandomAccess, Serializable
{
  public static class VoidValue
    implements Serializable, Cloneable, Comparable<Object>
  {
    public static VoidValue THE_ONE = new VoidValue();
    public VoidValue() { }    
    @Override public String toString() {
      return "";
    }
    @Override public int hashCode() {
      return 0;
    }
    @Override public boolean equals(Object o) {
      return o instanceof VoidValue;
    }
    @Override public VoidValue clone() {
      try {
        return (VoidValue) super.clone();
      } catch (CloneNotSupportedException e) { throw new Error(e); } 
    }
    @Override public int compareTo(Object that) {
      if (that instanceof VoidValue) return 0;
      if (that == null) return 0;
      if (!(that instanceof Comparable)) return 0;
      try {
        int cval = ((Comparable<Object>) that).compareTo((Object) this);
        return 0 - cval;
      } catch (Throwable t) {
        return 0;
      }
    }
  }
  
  static final Object[] EMPTY_OBJECTS = new Object[0];
  static final int[] EMPTY_INTS = new int[0];
  
  static final Object VOID_VALUE = VoidValue.THE_ONE;
  
  static final Matcher NAME_MCHR = Pattern.compile(
    "(?:key|val(?:[^$]|$)|left|right|first|second|1$|2$)",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL    
  ).matcher("");
  
  static Object[] mBaseCache;
  static int mBaseCacheSize;
  static Object[] mTwiceBaseCache;
  static int mTwiceBaseCacheSize;
  
  Object[] mArray = EMPTY_OBJECTS;
  int[] mHashes = EMPTY_INTS;
  int mSize;
  
  // Constructors
  public RealArrayMap(int initialCapacity) {
    if (initialCapacity > 0) ensureCapacity(initialCapacity);
  }
  
  public RealArrayMap() {
    this(0);
  }
  
  public RealArrayMap(Map<? extends K, ? extends V> map) {
    this(map.size());
    unsizedPutAll(map);
  }
  
  // Methods
  int indexOf(Object key, int hash) {
    int N = this.mSize;
    if (N == 0) {
      return -1;
    }
    int index = ContainerHelpers.binarySearch(
      this.mHashes, N, hash
    );
    if (index < 0 || key.equals(this.mArray[index << 1])) {
      return index;
    }
    int end = index + 1;
    while (end < N && this.mHashes[end] == hash) {
      if (key.equals(this.mArray[end << 1])) {
        return end;
      }
      end++;
    }
    int i = index - 1;
    while (i >= 0 && this.mHashes[i] == hash) {
      if (key.equals(this.mArray[i << 1])) {
        return i;
      }
      i--;
    }
    return end ^ -1;
  }
  
  int indexOfNull() {
    int N = this.mSize;
    if (N == 0) {
      return -1;
    }
    int index = ContainerHelpers.binarySearch(this.mHashes, N, 0);
    if (index < 0 || this.mArray[index << 1] == null) {
      return index;
    }
    int end = index + 1;
    while (end < N && this.mHashes[end] == 0) {
      if (this.mArray[end << 1] == null) {
        return end;
      }
      end++;
    }
    int i = index - 1;
    while (i >= 0 && this.mHashes[i] == 0) {
      if (this.mArray[i << 1] == null) {
        return i;
      }
      i--;
    }
    return end ^ -1;
  }
  
  public Map.Entry<K, V> entryForNullKey() {
    int nullIdx = indexOfNull();
    if (nullIdx == -1) return null;
    return get((int) nullIdx);
  }
  
  private void allocArrays(int size) {
    Object[] array;
    if (size == 8) {
      synchronized (RealArrayMap.class) {
        if (mTwiceBaseCache != null) {
          array = mTwiceBaseCache;
          this.mArray = array;
          mTwiceBaseCache = (Object[]) array[0];
          this.mHashes = (int[]) array[1];
          array[1] = null;
          array[0] = null;
          mTwiceBaseCacheSize--;
          return;
        }
      }
    } else if (size == 4) {
      synchronized (RealArrayMap.class) {
        if (mBaseCache != null) {
          array = mBaseCache;
          this.mArray = array;
          mBaseCache = (Object[]) array[0];
          this.mHashes = (int[]) array[1];
          array[1] = null;
          array[0] = null;
          mBaseCacheSize--;
          return;
        }
      }
    }
    this.mHashes = new int[size];
    this.mArray = new Object[(size << 1)];
  }

  private static 
  void freeArrays(int[] hashes, Object[] array, int size) {
    int i;
    if (hashes.length == 8) {
      synchronized (RealArrayMap.class) {
        if (mTwiceBaseCacheSize < 10) {
          array[0] = mTwiceBaseCache;
          array[1] = hashes;
          for (i = (size << 1) - 1; i >= 2; i--) {
            array[i] = null;
          }
          mTwiceBaseCache = array;
          mTwiceBaseCacheSize++;
        }
      }
    } else if (hashes.length == 4) {
      synchronized (RealArrayMap.class) {
        if (mBaseCacheSize < 10) {
          array[0] = mBaseCache;
          array[1] = hashes;
          for (i = (size << 1) - 1; i >= 2; i--) {
            array[i] = null;
          }
          mBaseCache = array;
          mBaseCacheSize++;
        }
      }
    }
  }

  @Override
  public void clear() {
    if (this.mSize != 0) {
      freeArrays(this.mHashes, this.mArray, this.mSize);
      this.mHashes = EMPTY_INTS;
      this.mArray = EMPTY_OBJECTS;
      this.mSize = 0;
    }
  }
  
  public void ensureCapacity(int minimumCapacity) {
    if (this.mHashes.length < minimumCapacity) {
      int[] ohashes = this.mHashes;
      Object[] oarray = this.mArray;
      allocArrays(minimumCapacity);
      if (this.mSize > 0) {
        System.arraycopy(
          ohashes, 0, this.mHashes, 0, this.mSize);
        System.arraycopy(
          oarray, 0, this.mArray, 0, this.mSize << 1);
      }
      freeArrays(ohashes, oarray, this.mSize);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    return indexOfKey(key) >= 0;
  }

  public int indexOfKey(Object key) {
    return key == null ? indexOfNull() : indexOf(key, key.hashCode());
  }

  int indexOfValue(Object value) {
    int N = this.mSize * 2;
    Object[] array = this.mArray;
    int i;
    if (value == null) {
      for (i = 1; i < N; i += 2) {
        if (array[i] == null) {
          return i >> 1;
        }
      }
    } else {
      for (i = 1; i < N; i += 2) {
        if (value.equals(array[i])) {
          return i >> 1;
        }
      }
    }
    return -1;
  }
  
  @Override
  public boolean containsValue(Object value) {
    return indexOfValue(value) >= 0;
  }
  
  @Override
  public V get(Object key) {
    int index = indexOfKey(key);
    return index >= 0 
      ? (V) this.mArray[(index << 1) + 1] 
      : null;
  }
  
  public Map.Entry<K, V> get(int index) {
    return (Map.Entry<K, V>) (Object) new SimpleEntry(
      this.mArray[index << 1], 
      this.mArray[(index << 1) + 1]
    );
  }

  public K keyAt(int index) {
    return (K) this.mArray[index << 1];
  }
  
  public V valueAt(int index) {
    return (V) this.mArray[(index << 1) + 1];
  }
  
  public V setValueAt(int index, V value) {
    index = (index << 1) + 1;
    V old = (V) this.mArray[index];
    this.mArray[index] = value;
    return old;
  }
  
  @Override
  public boolean isEmpty() {
    return this.mSize <= 0;
  }

  @Override
  public V put(K key, V value) {
    int hash;
    int index;
    int n = 8;
    if (key == null) {
      hash = 0;
      index = indexOfNull();
    } else {
      hash = key.hashCode();
      index = indexOf(key, hash);
    }
    if (index >= 0) {
      index = (index << 1) + 1;
      V old = (V) this.mArray[index];
      this.mArray[index] = value;
      return old;
    }
    index ^= -1;
    if (this.mSize >= this.mHashes.length) {
      if (this.mSize >= 8) {
        n = this.mSize + (this.mSize >> 1);
      } else if (this.mSize < 4) {
        n = 4;
      }
      int[] ohashes = this.mHashes;
      Object[] oarray = this.mArray;
      allocArrays(n);
      if (this.mHashes.length > 0) {
        System.arraycopy(ohashes, 0, this.mHashes, 0, ohashes.length);
        System.arraycopy(oarray, 0, this.mArray, 0, oarray.length);
      }
      freeArrays(ohashes, oarray, this.mSize);
    }
    if (index < this.mSize) {
      System.arraycopy(
        this.mHashes, index, 
        this.mHashes, index + 1, 
          this.mSize - index
      );
      System.arraycopy(
        this.mArray, index << 1, 
        this.mArray, (index + 1) << 1, 
          (this.mSize - index) << 1
      );
    }
    this.mHashes[index] = hash;
    this.mArray[index << 1] = key;
    this.mArray[(index << 1) + 1] = value;
    this.mSize++;
    return null;
  }

  @Override
  public V remove(Object key) {
    int index = indexOfKey(key);
    if (index >= 0) {
      return removeAt(index);
    }
    return null;
  }

  public V removeAt(int index) {
    int n = 8;
    V old = (V) this.mArray[(index << 1) + 1];
    if (this.mSize <= 1) {
      freeArrays(this.mHashes, this.mArray, this.mSize);
      this.mHashes = EMPTY_INTS;
      this.mArray = EMPTY_OBJECTS;
      this.mSize = 0;
    } else if (this.mHashes.length <= 8 || this.mSize >= this.mHashes.length / 3) {
      this.mSize--;
      if (index < this.mSize) {
        System.arraycopy(
          this.mHashes, index + 1, 
          this.mHashes, index, this.mSize - index
        );
        System.arraycopy(
          this.mArray, (index + 1) << 1, 
          this.mArray, index << 1, (this.mSize - index) << 1
        );
      }
      this.mArray[this.mSize << 1] = null;
      this.mArray[(this.mSize << 1) + 1] = null;
    } else {
      if (this.mSize > 8) {
        n = this.mSize + (this.mSize >> 1);
      }
      int[] ohashes = this.mHashes;
      Object[] oarray = this.mArray;
      allocArrays(n);
      this.mSize--;
      if (index > 0) {
        System.arraycopy(ohashes, 0, this.mHashes, 0, index);
        System.arraycopy(
          oarray, 0, this.mArray, 0, index << 1
        );
      }
      if (index < this.mSize) {
        System.arraycopy(ohashes, index + 1, this.mHashes, index, this.mSize - index);
        System.arraycopy(oarray, (index + 1) << 1, this.mArray, index << 1, (this.mSize - index) << 1);
      }
    }
    return old;
  }

  @Override
  public int size() {
    return this.mSize;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Map)) {
      return false;
    }
    Map<?, ?> map = (Map) object;
    if (size() != map.size()) {
      return false;
    }
    int i = 0;
    while (i < this.mSize) {
      try {
        K key = keyAt(i);
        V mine = valueAt(i);
        Object theirs = map.get(key);
        if (mine == null) {
          if (theirs != null || !map.containsKey(key)) {
            return false;
          }
        } else if (!mine.equals(theirs)) {
          return false;
        }
        i++;
      } catch (NullPointerException e) {
        return false;
      } catch (ClassCastException e2) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int[] hashes = this.mHashes;
    Object[] array = this.mArray;
    int result = 0;
    int i = 0;
    int v = 1;
    int s = this.mSize;
    while (i < s) {
      Object value = array[v];
      result 
        += (value == null ? 0 : value.hashCode()) 
          ^ hashes[i];
      i++;
      v += 2;
    }
    return result;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "{}";
    }
    StringBuilder buffer 
      = new StringBuilder(this.mSize * 28);
    buffer.append('{');
    for (int i = 0; i < this.mSize; i++) {
      if (i > 0) {
        buffer.append(", ");
      }
      Object key = keyAt(i);
      if (key != this) {
        buffer.append(key);
      } else {
        buffer.append("(this Map)");
      }
      buffer.append('=');
      Object value = valueAt(i);
      if (value != this) {
        buffer.append(value);
      } else {
        buffer.append("(this Map)");
      }
    }
    buffer.append('}');
    return buffer.toString();
  }
  
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    Map.Entry<K, V>[] es 
      = (Map.Entry<K, V>[]) new Map.Entry<?, ?>[mSize];
    
    for (int i=0; i<this.mSize; i+=1) {
      K key = keyAt(i);
      V value = valueAt(i);
      es[i] = new SimpleEntry(key, value);
    }
    return Collections.unmodifiableSet(
      new HashSet(Arrays.asList(es))
    );
  }
  
  @Override
  public Set<K> keySet() {
    K[] keys = (K[]) new Object[mSize];
    for (int i=0; i<this.mSize; i+=1) {
      keys[i] = keyAt(i);
    }
    return Collections.unmodifiableSet(
      new HashSet(Arrays.asList(keys))
    );
  }
  
  void unsizedPutAll(Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> entry
       : map.entrySet()) 
    {
      put(entry.getKey(), entry.getValue());
    }
  }
  
  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    ensureCapacity(mSize + map.size());
    unsizedPutAll(map);
  }



  public static <K, V> Map<K, V> toMap(
  Iterable<? extends Map.Entry<? extends K, ? extends V>> es)
  {
    final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    Set<K> keys = new HashSet<K>();
    Iterator<? extends Map.Entry<? extends K, ? extends V>> it 
      = es.iterator();
    outer:
    while (it.hasNext()) {
      Object next = ((Iterator<Object>)(Iterator<?>) it).next();
      if (! (next instanceof Map.Entry)) {
        Iterable<Iterable<Object>> itb = 
          (Iterable<Iterable<Object>>) (Object)
          IteratorUtils.asIterable(
            Iterators.concat(Arrays.asList(next).iterator(), it)
          );
        return toMap(itb, 0, 1);
      }
      
      Map.Entry<? extends K, ? extends V> entry 
        = (Map.Entry<? extends K, ? extends V>) (Object) next;
      K key = entry.getKey();
      if (! keys.add(key)) continue; 
      
      if (entry.getValue() == null) {
        System.err.printf(
          "Need to change null value on entry: %s\n", entry
        );
        try {
          ((Map.Entry<K, V>) (Object) entry)
            .setValue((V) (Object) VOID_VALUE);
          System.err.println("Regular setValue succeeded");
        } catch (Exception ex) {
          System.err.printf(
            "setValue() on %s threw %s\n", 
            entry.getClass().getName(), ex.toString()
          );
          if (! changeValue(entry, VOID_VALUE)) {
            System.err.printf(
              "change failed; skipping entry: %s\n", entry
            );
            continue outer;
          }
        }
      }      
      builder.put(entry.getKey(), entry.getValue());
    }
    /*Map<K, V> map = builder.putAll((
      Iterable<? extends Map.Entry<? extends K, ? extends V>>
      ) (Object) es
    ).build();*/
    Map<K, V> map = builder.build();
    if (! (map instanceof BiMap) && map instanceof ImmutableMap) {
      Map<K, V> mut = new TreeMap<K, V>();
      try {
        mut.putAll((Map<? extends K, ? extends V>) (Map<?, ?>) map);
        return mut;
      } catch (ClassCastException | NullPointerException notComparable) {
        mut = new HashMap<K, V>((Map<? extends K, ? extends V>) (Map<?, ?>) map);
        return mut;
      }
    }
    return map;
  }
  
  public static 
  boolean changeValue(Map.Entry<?, ?> entry, Object newValue) {
    if (entry instanceof ImmutablePair) {
      Reflect.setfldval(entry, "right", newValue);
      return entry.getValue() == newValue;
    }
    if (entry instanceof MutablePair) {
      ((Map.Entry<Object,Object>) (Object) entry)
        .setValue((Object) newValue);
      return entry.getValue() == newValue;
    }
    Class<?> eCls = entry.getClass();
    Map<Class<?>, Field> f1s = new HashMap<Class<?>, Field>();
    Triple<Class<Object>, Field, Field> best = null;
    do {
      for (Field fld: eCls.getDeclaredFields()) {
        Class<?> fldCls = fld.getType();
        String name = fld.getName();
        if (f1s.containsKey(fldCls)) {
          Field fld1 = f1s.get(fldCls);
          String name1 = fld1.getName();
          Field fld2 = fld;          
          String name2 = fld2.getName();
          if (best == null) {
            boolean canAssign = ClassUtils.isAssignable(
              newValue.getClass(), fldCls, true);
            if (! canAssign) continue; 
            best = Triple.of((Class<Object>) (Object) fldCls, fld1, fld2);
            continue; 
          }
          // best != null
          boolean canAssign = ClassUtils.isAssignable(
              newValue.getClass(), fldCls, true);          
          if (canAssign 
          &&  NAME_MCHR.reset(name1).find()
          &&  NAME_MCHR.reset(name2).find())
          {
            best = Triple.of((Class<Object>) (Object) fldCls, fld1, fld);
            continue; 
          }
        } else { // f1s.containsKey(fldCls) == false
          f1s.put((Class<?>) fldCls, fld);
          continue; 
        }
      }
      eCls = eCls.getSuperclass();
    } while (eCls != null && ! eCls.equals(Object.class));
    
    if (best == null) {
      System.err.println("best == null");
      return false;
    }
    try {
      Field vField = best.getRight();
      if (vField.getName().matches(
        ".*[Kk]ey|[Kk]|.*1.*|.*[Ll]eft.*|.*[Ff]irst.*"
      )) {
        vField = best.getMiddle();
      }
      System.err.printf("Assigning newValue to: \n%s\n%s\n",
        vField.getDeclaringClass().getName(),
        dumpMembers.colorize(vField));
      vField.setAccessible(true);
      /*vField.setModifiers(
        vField.getModifiers() ^ Modifier.FINAL
      );*/
      System.err.println(dumpMembers.colorize(vField));
      vField.set(entry, newValue);
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return false;       
    }
    return entry.getValue() == newValue;
  }
    
  
  public static 
  <K, V> Map<K, V> toMap(Iterable<Iterable<Object>> in0, int idxK, int idxV) 
  {
    List<Map.Entry<K, V>> ents 
      = new ArrayList<Map.Entry<K, V>>();
    
    for (Iterable<Object> row: in0) {
      int idx = -1;
      K k = null;
      V v = null;
      for (Object o: row) {
        idx++;
        if (idx == idxK) k = (K) o;
        if (idx == idxV) v = (V) o;
      }
      if (k == null || v == null) continue; 
      ents.add(Pair.of(k, v));
    }
    
    return toMap(
      (Iterable<? extends Map.Entry<? extends K, ? extends V>>)
      (Object) ents
    ); 
  }
  
  
  public static <K, V> Map<K, V> toMap(Map.Entry<?, ?>[] entryArray) {
    return toMap(
      (Iterable<? extends Map.Entry<? extends K, ? extends V>>)
      Arrays.asList(entryArray)
    );
  }
  
  @Extension
  public static <K, V> ImmutableBiMap<K, V> asBiMap(Map<K, V> map) {
    Method RIBIMAP_FROM_ENTRIES = null;
    Class<?> RegularImmutableBiMap = null;
    try {
      RegularImmutableBiMap = Class.forName(
        "com.google.common.collect.RegularImmutableBiMap",
        false, Thread.currentThread().getContextClassLoader()
      );
      (RIBIMAP_FROM_ENTRIES = RegularImmutableBiMap.getDeclaredMethod(
        "fromEntries", Map.Entry[].class
      )).setAccessible(true);
    } catch (ReflectiveOperationException roe) { 
      throw Reflector.Util.sneakyThrow(roe);
    }
    
    try {
      ImmutableBiMap<K, V> ret = (ImmutableBiMap<K, V>) 
      RIBIMAP_FROM_ENTRIES.invoke(
        null, (Object) map.entrySet().toArray(new Map.Entry<?, ?>[0])
      );
      return ret;
    } catch (Throwable iae) {
      Set<K> keys = new HashSet<>();
      Set<V> vals = new HashSet<>();
      List<Map.Entry<K, V>> entries = new LinkedList<>();
      for (Map.Entry<K, V> e: map.entrySet()) {
        if (keys.contains(e.getKey()) || vals.contains(e.getValue())) {
          continue;
        }
        keys.add(e.getKey());
        vals.add(e.getValue());
        
        entries.add(e);
      }
      try {
        ImmutableBiMap<K, V> ret = (ImmutableBiMap<K, V>) 
        RIBIMAP_FROM_ENTRIES.invoke(
          null, (Object) entries.toArray(new Map.Entry<?, ?>[0])
        );
        return ret;
      } catch (Throwable ex) {
        throw Reflector.Util.sneakyThrow(ex);
      } 
    } /*catch (ReflectiveOperationException roe) {
      throw Reflector.Util.sneakyThrow(roe);
    }*/
  }
  
  /*
  public static <K, V> Map<K, V> toMap(Object[] entryArray) {
    return toMap(
      (Iterable<? extends Map.Entry<? extends K, ? extends V>>)
      Arrays.asList(entryArray)
    );
  }
  */
  
  /*
  public static <K, V> Map<K, V> toMap(Object e) {
    if (e instanceof Map.Entry<?,?>[]) return toMap(
      (Map.Entry<?,?>[]) e
    );
    if (e instanceof Object[]) return toMap((Object[]) e);
    if (e instanceof Map<?,?>) return toMap(
      (Iterable<Map.Entry<K,V>>) ((Map<K,V>) e).entrySet()
    );
    if (e instanceof Iterable<?>) {
      return toMap((Iterable<Map.Entry<K,V>>) (Iterable<?>) e);
    }
    throw new UnsupportedOperationException(String.format(
      "Cannot create a map from a '%s'", 
      e != null? e.getClass().getName(): "<NULL>"
    ));
  }
  */
  
  /*public static <K, V> Map<K, V> toMap(
  Iterable<? extends Map.Entry<? extends K, ? extends V>> es)
  {
    ImmutableMap.Builder<K, V> builder 
      = ImmutableMap.builder();
    if (es instanceof Collection) {
      try { 
        ensureCapacity.invoke(
          builder, ((Collection<?>) es).size()
        );
      } catch (ReflectiveOperationException e) { } 
    }
    Map<K, V> map = builder
      .putAll(Arrays.asList(es))
      .build();
    return map;
  }*/
  
  @Override
  public Collection<V> values() {
    V[] values = (V[]) new Object[mSize];
    for (int i=0; i<this.mSize; i+=1) {
      values[i] = valueAt(i);
    }
    return Arrays.asList(values);
  }
  
   public static <K, V> Map<K, V> toMap(Object... dataKeysValues) {
    if (dataKeysValues.length == 3 && dataKeysValues[0] instanceof Iterable) {
      if (dataKeysValues[1] instanceof Integer &&
          dataKeysValues[2] instanceof Integer)
      {
        Iterator<Object> it = ((Iterable<Object>) dataKeysValues[0]).iterator();
        if (it.hasNext() == false || (it.next() instanceof Iterable)) {
          return (Map<K, V>) (Object) RealArrayMap.<K, V>toMap(
            (Iterable<Iterable<Object>>) dataKeysValues[0], // in0
            ((Integer) dataKeysValues[1]).intValue(), // idxK
            ((Integer) dataKeysValues[2]).intValue() // idxV
          );
        }
      }
    }
    
    Map<Object, Object> map = new HashMap<>();
    Queue<Object> q = new ArrayDeque<>();
    Collections.addAll(q, dataKeysValues);
    Object key = null;
    Object val = null;
    boolean gotKey = false, gotValue = false;
    
    while (! q.isEmpty() || (gotKey && gotValue)) {
      if (!gotKey) {
        Object o = q.poll();
        if (o instanceof String) {
          key = (String) o;          
          gotKey = true;
        } else if (o instanceof CharSequence) {
          key = ((CharSequence) o).toString();
          gotKey = true;
       /*} else if (o instanceof Class) {
          key = SIMPLE_NAME_MCHR.reset(((Class<?>) o).getName())
            .replaceFirst("$1");
          val = (Class<?>) o;
          gotKey = gotValue = true;*/
        } else if (o instanceof Map.Entry) {
          key = ((Map.Entry<String,Object>)o).getKey();
          val = ((Map.Entry<String,Object>)o).getValue();          
          gotKey = gotValue = true;
        } else if (o instanceof Iterable<?>) {
          for (final Object elem: ((Iterable<?>) o)) q.offer(elem);
        } else if (o instanceof Object[]) {
          for (final Object elem: ((Object[]) o)) q.offer(elem);
        } else if (o == null || o instanceof VoidValue) {
          key = null;
          gotKey = true;
        } else {
          if (!gotKey) {
            key = o;
            gotKey = true;
          } else if (!gotValue) {
            val = o;
            gotValue = true;
          }
        }
        continue;
      }
      if (!gotValue) {
        val = q.poll();
        gotValue = true;
      }
      if (gotKey && gotValue) {        
        map.put(key, val);
        gotKey = gotValue = false;
      }
    }
    boolean isKeyComparable = true;
    for (Object _key: map.keySet()) {
      if (! (_key instanceof Comparable)) {
        isKeyComparable = false;
        break;
      }
    }
    if (isKeyComparable) {
      try {
        SortedMap<Comparable<K>, V> tmap = new TreeMap<>();
        tmap.putAll((Map<Comparable<K>, V>) (Map<?, ?>) map);
        return (Map<K, V>) (Map<?, ?>) tmap;
      } catch (ClassCastException | NullPointerException notComparable) {
        isKeyComparable = false;
      }
    }
    return (Map<K, V>) (Map<?, ?>) map;
  }
  
  public static <K, V> Map<K, V> toMap(final Iterable<?> itb, final String keyExpr,
  final String valueExpr)
  { 
    final int size = (itb instanceof Collection)
      ? ((Collection) itb).size()
      : 0;
    
    final Map<K, V> map = new RealArrayMap<K, V>(size);
    final String[] keyParts = keyExpr.split(".");
    final String[] valParts = valueExpr.split(".");
    
    for (final Object item: itb) {
      Object key = null, value = null, cur = null;
      for (int i=0; i<2; ++i) {
        cur = item;
        if (cur == null) continue;
        final String[] parts = (i == 0)? keyParts: ((i == 1)? valParts: null);
        for (int p=0, plen=parts.length; p<plen; ++p) {
          final String part = parts[p];
          if (part.length() == 0) continue;
          final int openParenPos = part.indexOf("(");
          if (openParenPos != -1) {
            final String methodName = part.substring(0, openParenPos);
            cur = Reflect.invoke(cur.getClass(), cur, methodName);
          } else {
            cur = Reflect.getfldval(cur, part, false);
          }
          if (cur == null) break;
        }
        if (i == 0) key = cur;
        else      value = cur;
      }
      map.put((K) key, (V) value);
    }
    return map;
  }

}



/*
public class RealArrayMap<K, V> 
  int indexOf(Object key, int hash)
  int indexOfNull()
  void allocArrays(int size)
  static void freeArrays(int[] hashes, Object[] array, int size)
  void clear()
  void ensureCapacity(int minimumCapacity)
  boolean containsKey(Object key)
  int indexOfKey(Object key)
   int indexOfValue(Object value)
  boolean containsValue(Object value)
  V get(Object key)
  K keyAt(int index)
  V valueAt(int index)
  V setValueAt(int index, V value)
  boolean isEmpty()
  V put(K key, V value)
  V remove(Object key)
  V removeAt(int index)
  int size()
}

public interface Map<K, V> {
  //Set<Map.Entry<K, V>> entrySet()
  //boolean isEmpty()
  //Set<K> keySet()
  //void putAll(Map<? extends K, ? extends V> map)
  //Collection<V> values()
} 
*/