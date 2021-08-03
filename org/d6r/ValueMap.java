
package org.d6r;

import java.io.Serializable;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;

abstract interface IValueMap<E>
   extends Map<Object, E>,
           Iterable<E>, RandomAccess
{
  // Collection only
  <T> T[] toArray(T[] contents);
  ArrayList<Object> toArrayList();
  Iterator<E> iterator();
  Object[] toArray();
  boolean add(E object);
  boolean addAll(Collection<? extends E> collection);
  boolean contains(Object object);
  boolean containsAll(Collection<?> collection);
  // boolean remove(Object object);
  boolean removeAll(Collection<?> collection);
  boolean retainAll(Collection<?> collection);
  
  // Map only
  Collection<E> values();
  Set<Entry<Object, E>> entrySet();
  Set<Object> keySet();
  E get(Object key);
  E put(Object key, E value);
  E remove(Object key);
  boolean containsKey(Object key);
  boolean containsValue(Object value);
  boolean equals(Object object);
  int hashCode();
  void putAll(Map<? extends Object, ? extends E> map);
  
  // Common
  boolean isEmpty();
  void clear();
  String toString();
  int size();
}           


public abstract class ValueMap<E>
  implements IValueMap<E> 
{
  Set<Object> keySet;
  Collection<E> valuesCollection;
  int nextIndex = 0;
  
  public ValueMap() {
    super();
  }
  
  public ValueMap(Map<?, ? extends E> map) {
    this();
    putAll((Map) map);
  }
  
  public ValueMap(Iterable<? extends Map.Entry<?, ? extends E>> ents) {
    this();    
    for (Map.Entry<?, ? extends E> e: ents) {
      put(e.getKey(), e.getValue());
    }
  }
  
  
  public Object nextKeyFor(E object, boolean mapSemantics) {
    if (mapSemantics) return Integer.valueOf(object.hashCode());
    else return Integer.valueOf(nextIndex++);
  }
  
  public boolean add(E object) {
    return put(nextKeyFor(object, false), object) == null;
  }
  
  public void clear() {
    this.entrySet().clear();
  }
  
  public boolean containsKey(final Object key) {
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    if (key != null) {
      while (it.hasNext()) {
        if (key.equals(it.next().getKey())) {
          return true;
        }
      }
    }
    else {
      while (it.hasNext()) {
        if (it.next().getKey() == null) {
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean containsValue(final Object value) {
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    if (value != null) {
      while (it.hasNext()) {
        if (value.equals(it.next().getValue())) {
          return true;
        }
      }
    }
    else {
      while (it.hasNext()) {
        if (it.next().getValue() == null) {
          return true;
        }
      }
    }
    return false;
  }
  
  public Set<Entry<Object, E>> entrySet() {
    Set<Map.Entry<Object, E>> es = CollectionFactory.newSet();
    for (Object key: keySet()) {
      es.add(Pair.of(key, ((Map<Object, E>) this).get(key)));
    }
    return es;
  }
  
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Map)) {
      return false;
    }
    final Map<?, ?> map = (Map<?, ?>)object;
    if (this.size() != map.size()) {
      return false;
    }
    try {
      for (final Entry<Object, E> entry : this.entrySet()) {
        final Object key = entry.getKey();
        final E mine = entry.getValue();
        final Object theirs = map.get(key);
        if (mine == null) {
          if (theirs != null || !map.containsKey(key)) {
            return false;
          }
          continue;
        }
        else {
          if (!mine.equals(theirs)) {
            return false;
          }
          continue;
        }
      }
    }
    catch (NullPointerException ignored) {
      return false;
    }
    catch (ClassCastException ignored2) {
      return false;
    }
    return true;
  }
  
  public E get(final Object key) {
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    if (key != null) {
      while (it.hasNext()) {
        final Entry<Object, E> entry = it.next();
        if (key.equals(entry.getKey())) {
          return entry.getValue();
        }
      }
    }
    else {
      while (it.hasNext()) {
        final Entry<Object, E> entry = it.next();
        if (entry.getKey() == null) {
          return entry.getValue();
        }
      }
    }
    return null;
  }
  
  public int hashCode() {
    int result = 0;
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    while (it.hasNext()) {
      result += it.next().hashCode();
    }
    return result;
  }
  
  
  public Set<Object> keySet() {
    if (this.keySet == null) {
      this.keySet = new AbstractSet<Object>() {
        
        public boolean contains(final Object object) {
          return ValueMap.this.containsKey(object);
        }
        
        public int size() {
          return ValueMap.this.size();
        }
        
        public Iterator<Object> iterator() {
          return new Iterator<Object>() {
            Iterator<Entry<Object, E>> setIterator
              = ValueMap.this.entrySet().iterator();
            
            public boolean hasNext() {
              return this.setIterator.hasNext();
            }
            
            public Object next() {
              return this.setIterator.next().getKey();
            }
            
            public void remove() {
              this.setIterator.remove();
            }
          };
        }
      };
    }
    return this.keySet;
  }
  
  public E put(final Object key, final E value) {
    throw new UnsupportedOperationException();
  }
  
  public void putAll(final Map<? extends Object, ? extends E> map) {
    for (final Entry<? extends Object, ? extends E> entry: map.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }
  
  public E remove(final Object key) {
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    if (key != null) {
      while (it.hasNext()) {
        final Entry<Object, E> entry = it.next();
        if (key.equals(entry.getKey())) {
          it.remove();
          return entry.getValue();
        }
      }
    }
    else {
      while (it.hasNext()) {
        final Entry<Object, E> entry = it.next();
        if (entry.getKey() == null) {
          it.remove();
          return entry.getValue();
        }
      }
    }
    return null;
  }
  
  public int size() {
    return this.entrySet().size();
  }
  
  public String toString() {
    if (this.isEmpty()) {
      return "{}";
    }
    final StringBuilder buffer = new StringBuilder(this.size() * 28);
    buffer.append('{');
    final Iterator<Entry<Object, E>> it = this.entrySet().iterator();
    while (it.hasNext()) {
      final Entry<Object, E> entry = it.next();
      final Object key = entry.getKey();
      if (key != this) {
        buffer.append(key);
      }
      else {
        buffer.append("(this Map)");
      }
      buffer.append('=');
      final Object value = entry.getValue();
      if (value != this) {
        buffer.append(value);
      }
      else {
        buffer.append("(this Map)");
      }
      if (it.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append('}');
    return buffer.toString();
  }
  
  public Collection<E> values() {
    if (this.valuesCollection == null) {
      this.valuesCollection = new AbstractCollection<E>() {
        
        public int size() {
          return ValueMap.this.size();
        }
        
        public boolean contains(final Object object) {
          return ValueMap.this.containsValue(object);
        }
        
        public Iterator<E> iterator() {
          return new Iterator<E>() {
            Iterator<Entry<Object, E>> setIterator 
              = ValueMap.this.entrySet().iterator();
            
            public boolean hasNext() {
              return this.setIterator.hasNext();
            }
            
            public E next() {
              return this.setIterator.next().getValue();
            }
            
            public void remove() {
              this.setIterator.remove();
            }          
          };
        }
      };
    }
    return this.valuesCollection;
  }
  
  // __________ Collection __________
  
  public boolean addAll(Collection<? extends E> collection) {
    boolean result = false;
    final Iterator<? extends E> it = collection.iterator();
    while (it.hasNext()) {
      if (this.add(it.next())) {
        result = true;
      }
    }
    return result;
  }
  
  public boolean contains(Object object) {
    final Iterator<E> it = this.iterator();
    if (object != null) {
      while (it.hasNext()) {
        if (object.equals(it.next())) {
          return true;
        }
      }
    }
    else {
      while (it.hasNext()) {
        if (it.next() == null) {
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean containsAll(Collection<?> collection) {
    final Iterator<?> it = collection.iterator();
    while (it.hasNext()) {
      if (!this.contains(it.next())) {
        return false;
      }
    }
    return true;
  }
  
  public boolean isEmpty() {
    return this.size() == 0;
  }
  
  public Iterator<E> iterator() {
    return values().iterator();
  }
  
  public boolean _collectionRemove(Object object) {
    final Iterator<?> it = this.iterator();
    if (object != null) {
      while (it.hasNext()) {
        if (object.equals(it.next())) {
          it.remove();
          return true;
        }
      }
    }
    else {
      while (it.hasNext()) {
        if (it.next() == null) {
          it.remove();
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean collectionRemove(Object object) {
    return _collectionRemove(object);
  }
  
  public boolean removeAll(Collection<?> collection) {
    boolean result = false;
    final Iterator<?> it = this.iterator();
    while (it.hasNext()) {
      if (collection.contains(it.next())) {
        it.remove();
        result = true;
      }
    }
    return result;
  }
  
  public boolean retainAll(Collection<?> collection) {
    boolean result = false;
    final Iterator<?> it = this.iterator();
    while (it.hasNext()) {
      if (!collection.contains(it.next())) {
        it.remove();
        result = true;
      }
    }
    return result;
  }
  
  public Object[] toArray() {
    return this.toArrayList().toArray();
  }
  
  public <T> T[] toArray(T[] contents) {
    return this.toArrayList().toArray(contents);
  }
  
  public ArrayList<Object> toArrayList() {
    final ArrayList<Object> result 
      = new ArrayList<Object>(this.size());
    for (final E entry : this) {
      result.add(entry);
    }
    return result;
  }
    
}

