package org.d6r;

import java.util.*;


public class SafeIterable<T> implements Iterable<T> {
  
  protected final boolean lazy;
  protected transient Iterable<T> dangerousCollection;
  protected Collection<T> copy;
  
  public SafeIterable(final Iterable<T> dangerousCollection, final boolean lazy) {
    this.lazy = lazy;
    if (lazy) {
      this.dangerousCollection = dangerousCollection;
      this.copy = null;
    } else {
      this.dangerousCollection = null;
      this.copy = copyToCollection(dangerousCollection.iterator());
    }
  }
  
  public static <E> Iterable<E> wrap(final Iterable<E> dangerousCollection) {
    return new SafeIterable<E>(dangerousCollection, false);
  }
  
  public static <E> Iterable<E> lazyWrap(final Iterable<E> dangerousCollection) {
    return new SafeIterable<E>(dangerousCollection, true);
  }
  
  protected static <E> Collection<E> copyToCollection(final Iterator<E> it) {
    final List<E> copy = new ArrayList<>();
    while (it.hasNext()) {
      final E item = it.next();
      copy.add(item);
    }
    return copy;
  }
  
  @Override
  public Iterator<T> iterator() {
    if (dangerousCollection != null) {
      // Check invariant
      if (!lazy) throw new Error("dangerousCollection != null when lazy == false");
      try {
        this.copy = copyToCollection(dangerousCollection.iterator());
      } finally {
        this.dangerousCollection = null;
      }
    }
    if (this.copy == null) throw new Error("iterator(): copy == null");
    
    return new Iterator<T>() {
      protected final Iterator<T> _iter = copy.iterator();
      
      @Override
      public boolean hasNext() {
        return this._iter.hasNext();
      } // new Iterator(){..}.hasNext()
      
      @Override
      public T next() {
        return this._iter.next();
      } // new Iterator(){..}.next()
      
      @Override
      public void remove() {
        final java.lang.reflect.Member mtd = getClass().getEnclosingMethod();
        throw new UnsupportedOperationException(
          (mtd != null)
            ? String.format(
                "remove() called on %s.%s()!",
                mtd.getDeclaringClass().getSimpleName(), mtd.getName()
              )
            : "remove() called on immutable Iterator"
        );
      } // Iterator(){}.remove()
      
      @Override
      public String toString() {
        final java.lang.reflect.Member mtd = getClass().getEnclosingMethod();
        return String.format(
          "%s.iterator() [immutable, %s]",
          (mtd != null)
            ? String.format("%s.%s", 
                mtd.getDeclaringClass().getSimpleName(), mtd.getName())
            : "Iterator",
          (hasNext()? "has item(s)": (copy.isEmpty()? "empty": "depleted"))
        );
      } // Iterator(){}.toString()
    }; // return new Iterator<T>(){..};
  } // iterator()
}


