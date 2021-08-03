package org.d6r;

import java.util.Comparator;

/**
 * Compares a collection of objects using the {@link Object#toString()} method
 * and doing a string comparison.
 * Can ignore case or invert the comparison as well.
 *
 * @author Chris
 * @since  21-Dec-07
 */
public class ToStringComparator implements Comparator<Object> 
{
  public boolean ignoreCase;
  public boolean invert;
  
  private static String NULL_STRING = "";

  public ToStringComparator() {
    this(true, false);
  }
  
  public ToStringComparator(boolean ignoreCase) {
    this(ignoreCase, false);
  }
  
  public ToStringComparator(boolean ignoreCase, 
  boolean invert) 
  {
    this.ignoreCase = ignoreCase;
    this.invert = invert;
  }
  
  @Override
  public int compare(Object o1, Object o2) {
    String s1 = o1 == null
    ? NULL_STRING
    : String.valueOf(o1);
    String s2 = o2 == null
    ? NULL_STRING
    : String.valueOf(o2);
    
    if (invert) {
    String temp = s1;
    s1 = s2;
    s2 = temp;
    }
    return ignoreCase 
    ? s1.compareToIgnoreCase(s2) 
    : s1.compareTo(s2);
  }
  
  @Override
  public boolean equals(Object otherComparator) {
    if (otherComparator == null) return false;
    
    if (! getClass().equals(otherComparator.getClass())) {
    return false;
    }
    
    ToStringComparator other 
    = (ToStringComparator) otherComparator;
    
    return this.ignoreCase == other.ignoreCase
      && this.invert   == other.invert;
  }
}
  
  
  
  