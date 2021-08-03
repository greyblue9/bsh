package org.d6r;

import java.util.concurrent.*;
import java.util.*;

public class ConcurrentHashSet<E> 
     extends AbstractSet<E> 
  implements Set<E>
{
  private final Map<E, Boolean> _map;
  private transient Set<E> _keys;
  

  public static final int TSHIFT = 2;
  public static final int TBASE = 0x10;
  public static final int MAX_SEGMENTS = 0x10000;
  public static final int MAXIMUM_CAPACITY = 0x40000000;
  public static final float DEFAULT_LOAD_FACTOR = 0.75f;
  
  public ConcurrentHashSet() {
    this(TBASE, DEFAULT_LOAD_FACTOR, TBASE);
  }
  
  public ConcurrentHashSet(int initialCapacity)  {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, TBASE);
  }
  
  public ConcurrentHashSet(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, TBASE);
  }
  
  public ConcurrentHashSet(int initialCapacity, float loadFactor, 
  int concurrencyLevel)
  {
    this._map = new ConcurrentHashMap<E, Boolean>(
      initialCapacity, loadFactor, concurrencyLevel
    );
    this._keys = this._map.keySet();
  }
  
  public ConcurrentHashSet(Set<? extends E> s)  {
    this(
      Math.max((int) ((float) s.size() / (DEFAULT_LOAD_FACTOR + 1)), TBASE),
      DEFAULT_LOAD_FACTOR, 
      TBASE
    );
    this.addAll(s);
  }
  
  @Override
  public boolean add(final E e) {
    return this._map.put(e, Boolean.TRUE) == null;
  }
  
  @Override
  public void clear() {
    this._map.clear();
  }
  
  @Override
  public boolean contains(final Object o) {
    return this._map.containsKey(o);
  }
  
  @Override
  public boolean containsAll(final Collection<?> c) {
    return this._keys.containsAll(c);
  }
  
  @Override
  public boolean equals(final Object o) {
    return o == this || this._keys.equals(o);
  }
  
  @Override
  public int hashCode() {
    return this._keys.hashCode();
  }
  
  @Override
  public boolean isEmpty() {
    return this._map.isEmpty();
  }
  
  @Override
  public Iterator<E> iterator() {
    return this._keys.iterator();
  }
  
  @Override
  public boolean remove(final Object o) {
    return this._map.remove(o) != null;
  }
  
  @Override
  public boolean removeAll(final Collection<?> c) {
    return this._keys.removeAll(c);
  }
  
  @Override
  public boolean retainAll(final Collection<?> c) {
    return this._keys.retainAll(c);
  }
  
  @Override
  public int size() {
    return this._map.size();
  }
  
  @Override
  public Object[] toArray() {
    return this._keys.toArray();
  }
  
  @Override
  public <T> T[] toArray(final T[] a) {
    return this._keys.toArray(a);
  }
  
  @Override
  public String toString() {
    return this._keys.toString();
  }
}



