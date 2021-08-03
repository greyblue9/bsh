package org.d6r;

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.lang3.tuple.Pair;


public class ListMap<K, V> 
     extends ListOrderedMap<K, V>
  implements Iterable<Entry<K, V>>
{
  
  public static ListMap<String, String> toMap(final String... items) {
    return(ListMap <String, String>) (ListMap <?, ?>)
           ListMap.<Object, Object>toMap((Object[]) items);
  }
  
  public static <K, V> ListMap<K, V> toMap(final Object... items) {
    if (items.length % 2 == 1 && items[0] instanceof Object[]) {
      return toMap((Object[]) (Object) items[0]);
    }
    return toMap(new RealArrayMap<K, V>(items.length), items);
  }
  
  public static <K, V> ListMap<K, V> toMap(final Map<K, V> backingMap,
  final Object... items)
  {  
    if (items.length % 2 == 1 && items[0] instanceof Object[]) {
      return toMap(backingMap, (Object[]) (Object) items[0]);
    }
    
    boolean isKey = true;
    K key = null;
    V value = null;
    final ListMap<K, V> map = new ListMap<K, V>(backingMap);
    
    for (final Object item: items) {
      if (isKey) key = (K) item;
      else map.put(key, (value = (V) item));
      
      if ((isKey = !isKey)) {
        key = null;
        value = null;
      }
    }
    return map;
  }
  
  
  public ListMap(final Map<K, V> map) {
    super(map);
  }
  
  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new Iterator<Entry<K, V>>() {
      final OrderedMapIterator<K, V> it = ListMap.super.mapIterator();
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }
      @Override
      public Entry<K, V> next() {
        final K key = it.next();
        final V value = it.getValue();
        return (key != null && value != null)
          ? Pair.<K, V>of(key, value)
          : new AbstractMap.SimpleEntry<K, V>(key, value);
      }
      @Override
      public void remove() {
        it.remove();
      }
      
      @Override
      public String toString() {
        return getClass().getEnclosingMethod().getGenericReturnType().toString();
      }
    };
  }
}

