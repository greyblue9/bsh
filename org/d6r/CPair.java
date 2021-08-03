package org.d6r;

import java.util.*;
import org.d6r.ToStringComparator;
import org.d6r.annotation.*;
import static org.d6r.ClassInfo.getSimpleName;


public class CPair<A, B, E>
  implements 
    Comparable<CPair<?, ?, ?>>,
    Map.Entry<A, B>,
    Iterable<E>
  {
  
  protected static final Comparator<Object> COMPARATOR = new ToStringComparator();
  protected static <Z> Comparator<Z> comparator() {
    return (Comparator<Z>) (Object) COMPARATOR;
  }
  protected String _cachedToString;
  
  protected final A item1;
  protected final B item2;
  protected final List<E> items;
    
  public CPair(A item1, B item2) {
    this.item1 = item1;
    this.item2 = item2;
    this.items = Collections.<E>unmodifiableList(
      Arrays.<E>asList((E) (Object) item1, (E) (Object) item2)
    );
  }

  public static <TA, TB> CPair<TA, TB, ?> of(TA item1, TB item2) {
    return new CPair<TA, TB, Object>(item1, item2);
  }
  
  @Override
  public A getKey() { return item1; }
  
  
  @Override
  public B getValue() { return item2; }
  
  @Override
  @Throws(UnsupportedOperationException.class)
  public B setValue(final B newVal) {
    throw new UnsupportedOperationException(String.format(
      "setValue(%2$s newVal: (%3$s) %4$s) is not allowed on CPair<%1$s, %2$s>", 
      getSimpleName(item1), getSimpleName(item2),
      getSimpleName(newVal), newVal
    ));
  }
  
  @Override
  public Iterator<E> iterator() {
    return items.iterator();
  }
  
  @Override
  public String toString() {
    if (_cachedToString == null) {
      String s1, s2;
      try {
        s1 = item1 != null? item1.toString(): null;
      } catch (final Throwable ex) {
        s1 = String.format("<item1.toString() threw %s>", ex);
      }
      try {
        s2 = item2 != null? item2.toString(): null;
      } catch (final Throwable ex) {
        s2 = String.format("<item2.toString() threw %s>", ex);
      }
      _cachedToString = String.format(
        "CPair<%1$s, %2$s>(%3$s, %4$s)", 
        getSimpleName(item1), getSimpleName(item2), s1, s2
      );
    }
    return _cachedToString;
  }
  
  public E[] toArray() {
    final E[] array = CollectionUtil.toArray(this.items);
    return array;
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof CPair)) {
      return false;
    }
    final CPair otherPair = (CPair<?, ?, ?>) other;
    boolean same1 = otherPair.item1 == item1, same2 = otherPair.item2 == item2;
    if (same1 && same2)
      return true;
    if (same1) {
    } else if (item1 == null) {
      same1 = otherPair.item1 == null;
    } else if (item1 != null && item1.getClass().isInstance(otherPair.item1)) {
      if (item1 instanceof Comparable && otherPair.item1 instanceof Comparable) {
        same1 = ((Comparable) item1).compareTo(otherPair.item1) == 0 ||
                ((Comparable) otherPair.item1).compareTo(item1) == 0;
      } else {
        same1 = item1.equals(otherPair.item1);
      }
    } else {
      same1 = item1.equals(otherPair.item1);
    }
    if (same2) {
    } else if (item2 == null) {
      same2 = otherPair.item2 == null;
    } else if (item2 != null && item2.getClass().isInstance(otherPair.item2)) {
      if (item2 instanceof Comparable && otherPair.item2 instanceof Comparable) {
        same2 = ((Comparable) item2).compareTo(otherPair.item2) == 0 ||
                ((Comparable) otherPair.item2).compareTo(item2) == 0;
      } else {
        same2 = item2.equals(otherPair.item2);
      }
    } else {
      same2 = item2.equals(otherPair.item2);
    }
    return same1 && same2;
  }

  @Override
  public int compareTo(final CPair<?, ?, ?> otherPair) {
    boolean same1 = otherPair.item1 == item1, same2 = otherPair.item2 == item2;
    if (same1 && same2)
      return 0;
    int signum1, signum2;
    if (same1) {
      signum1 = 0;
    } else if (item1 == null) {
      signum1 = (otherPair.item1 == null) ? 0 : -1;
    } else if (item1 != null && item1.getClass().isInstance(otherPair.item1)) {
      if (item1 instanceof Comparable && otherPair.item1 instanceof Comparable) {
        signum1 = ((Comparable) item1).compareTo(otherPair.item1);
        int signum1r = ((Comparable) otherPair.item1).compareTo(item1);
        if (signum1r == -signum1) {
        } else {
          signum1 = 0;
        }
      } else {
        same1 = item1.equals(otherPair.item1);
        signum1 = same1 ? 0 
                        : comparator().compare(item1, otherPair.item1);
      }
    } else {
      same1 = item1.equals(otherPair.item1);
      signum1 = same1 ? 0 : comparator().compare(item1, otherPair.item1);
    }
    if (same2) {
      signum2 = 0;
    } else if (item2 == null) {
      signum2 = (otherPair.item2 == null) ? 0 : -1;
    } else if (item2 != null && item2.getClass().isInstance(otherPair.item2)) {
      if (item2 instanceof Comparable && otherPair.item2 instanceof Comparable) {
        signum2 = ((Comparable) item2).compareTo(otherPair.item2);
        int signum1r = ((Comparable) otherPair.item2).compareTo(item2);
        if (signum1r == -signum2) {
        } else {
          signum2 = 0;
        }
      } else {
        same2 = item2.equals(otherPair.item2);
        signum2 = same2 ? 0 : comparator().compare(item2, otherPair.item2);
      }
    } else {
      same2 = item2.equals(otherPair.item2);
      signum2 = same2 ? 0 : comparator().compare(item2, otherPair.item2);
    }
    if (signum1 == 0)
      return signum2;
    return signum1;
  }

  @Override
  public int hashCode() {
    int hashCode1 = item1 != null ? item1.hashCode() : 0;
    int hashCode2 = item2 != null ? item2.hashCode() : 0;
    return hashCode1 ^ (((hashCode2 & 0x0000FFFF) << 16) | ((hashCode2 & 0xFFFF0000) >>> 16));
  }
}


