package org.d6r;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class CollectionFactory {
  
  public static <E> Set<E> newSet() {
    return new HashSet<E>();
  }

  public static <E> Set<E> newSortedSet() {
    return new TreeSet<E>();
  }

  public static 
  <E extends Enum<E>> EnumSet<E> newSet(EnumSet<E> content) {
    return EnumSet.<E>copyOf(content);
  }
  
  public static 
  <E extends Enum<E>> EnumSet<E> newSet(E... values) 
  {
    EnumSet<E> es = Reflect.newInstance(EnumSet.class);
    Collections.addAll(es, values);
    return es;
  }
  
  public static <E> Set<E> newSet(Collection<E> coll) {
    return (Set<E>) (
      (isComparable(coll))
        ? (Object) new TreeSet<Comparable<?>>(
            (Collection<? extends Comparable<?>>) coll
          )
       : (Object) new HashSet<E>(coll)
      );
  }
  
  public static boolean isComparable(Iterable<?> itb) {
    Iterator<?> it = itb.iterator();
    if (! it.hasNext()) return false;
    Object firstItem = it.next();
    return (firstItem instanceof Comparable<?>);
  }
  
  public static <E> Set<E> newSet(E... content) {
    Set<E> res = new HashSet<E>();
    Collections.addAll(res, content);
    return res;
  }

  public static <E> Set<E> newOrderedSet() {
    return new LinkedHashSet<E>();
  }

  public static <E> Set<E> newOrderedSet(Collection<E> content) {
    return new LinkedHashSet<E>(content);
  }

  public static <E> Set<E> newOrderedSet(E... content) {
    LinkedHashSet res = new LinkedHashSet<E>();
    Collections.<E>addAll(res, content);
    return res;
  }
}
