package org.d6r;

import java.util.Comparator;

public class NaturalOrdering
  implements Comparator<Object>
{
  public static final NaturalOrdering INSTANCE = new NaturalOrdering(false);
  public static final NaturalOrdering INSTANCE_REVERSE = new NaturalOrdering(true);
  public static Comparator<Object> FALLBACK = new ToStringComparator(false);
  public static Comparator<Object> FALLBACK_REVERSE = new ToStringComparator(true);
  
  public final boolean reverse;
  
  public NaturalOrdering(final boolean reverse) {
    this.reverse = reverse;
  }
  
  @Override
  public int compare(Object left, Object right) {
    if (left instanceof Comparable<?> && right instanceof Comparable) {
      try {
        return comparableCompare((Comparable<?>) left, (Comparable<?>) right);
      } catch (ClassCastException cce) {
        return (reverse? FALLBACK_REVERSE: FALLBACK).compare(left, right);
      } catch (Throwable e) {
        Log.w("NaturalOrderibN.compare(%s, %s): %s", ClassInfo.typeToName(left),
          ClassInfo.typeToName(right), ClassInfo.typeToName(e), e.getMessage());
      }
    }
    if (left != null && right != null) {
      return (reverse? FALLBACK_REVERSE: FALLBACK).compare(left, right);
    }
    return Integer.compare(
      System.identityHashCode(left), System.identityHashCode(right)
    ) * (reverse? -1: 1);
  }
  
  public int comparableCompare(Comparable<?> left, Comparable<?> right) {
    if (left == null) left = (Comparable<?>) 0;
    if (right == null) right = (Comparable<?>) 0;
    if (left.getClass() != right.getClass()) {
      if (left instanceof Boolean) {
        left = (Comparable<?>) (((Boolean) left).booleanValue()? 1: 0);
      }
      if (right instanceof Boolean) {
        right = (Comparable<?>) (((Boolean) right).booleanValue()? 1: 0);
      }
      if (left instanceof Number && right instanceof Number) {
        Class<?> top = Integer.class;
        if (left instanceof Long || right instanceof Long) top = Long.class;
        if (left instanceof Double || right instanceof Double) top = Double.class;
        if (top == Integer.class) {
          left = ((Number) left).intValue();
          right = ((Number) right).intValue();
        } else if (top == Long.class) {
          left = ((Number) left).longValue();
          right = ((Number) right).longValue();
        } else if (top == Double.class) {
          left = ((Number) left).doubleValue();
          right = ((Number) right).doubleValue();
        } else {
          left = left.toString();
          right = right.toString();
        }
      } else {
        left = left.toString();
        right = right.toString();
      }
    }
    Throwable suppressed = null;
    try {
      return reverse
        ? ((Comparable<Object>) right).compareTo((Object) left)
        : ((Comparable<Object>) left).compareTo((Object) right);
    } catch (final ClassCastException e1) { 
      suppressed = e1;
    }
    try {
      return reverse
        ? (-1 * ((Comparable<Object>) left).compareTo((Object) right))
        : (-1 * ((Comparable<Object>) right).compareTo((Object) left));        
    } catch (final ClassCastException e2) { 
      e2.addSuppressed(suppressed);
      throw e2;
    }
  }
}