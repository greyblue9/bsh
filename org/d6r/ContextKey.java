package org.d6r;

import java.util.*;
import org.junit.Test;
import org.junit.Assert;
import java.lang.reflect.Field;

public class ContextKey<L, R>
  implements Map.Entry<L, R>,
             Comparable<ContextKey<?, ?>>
{
  private static transient CharSequence _simpleName;
  private static final Map<Long, ContextKey> cache = new TreeMap<>();
  
  private final L obj1;
  private final R obj2;
  private final long longHashCode; // NEVER 0L
  
  
  protected ContextKey(L o1, R o2) {
    this.obj1 = o1;
    this.obj2 = o2;
    this.longHashCode = longHashCode(o1, o2);
  }
  
  
  
  @Override
  public int compareTo(ContextKey<?, ?> other) {
    long other_longHashCode = (other != null)? other.longHashCode: 0L;
    return Long.signum(this.longHashCode - other_longHashCode);
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ContextKey)) return false;
    long other_longHashCode = ((ContextKey) other).longHashCode;
    return this.longHashCode == other_longHashCode;
  }
  
  
  @Override
  public L getKey() {
    return obj1;
  }
  
  @Override
  public R getValue() {
    return obj2;
  }
  
  // @Override
  public L getLeft() {
    return obj1;
  }
  
  // @Override
  public R getRight() {
    return obj2;
  }
  
  @Override
  public R setValue(R newValue) {
    throw new UnsupportedOperationException(String.format(
      "ContextKey.setValue(%s): ContextKey is immutable", newValue
    ));
  }
  
  public static <L, R> ContextKey<L, R> of(L left, R right) {
    long combinedIdentityHash = longHashCode(left, right);
    Long boxed_combinedIdentityHash = Long.valueOf(combinedIdentityHash);
    
    synchronized (cache) {
      ContextKey<?, ?> cached = cache.get(boxed_combinedIdentityHash);
      if (cached != null) return (ContextKey<L, R>) cached;
      
      ContextKey<L, R> newKey = new ContextKey<L, R>(left, right);
      cache.put(boxed_combinedIdentityHash, newKey);
      return newKey;
    }
  }
  
  @Deprecated
  public static <L> ContextKey<L, Void> of(L left) {
    return of(left, (Void) null);
  }
  
  @Override
  public String toString() {
    if (_simpleName == null) _simpleName = getClass().getSimpleName();
        
    String cn1 = (obj1 != null? obj1.getClass(): Void.class).getName();
    String cn2 = (obj2 != null? obj2.getClass(): Void.class).getName();
    
    return new StringBuilder(255)
      .append("\u001b[1;33m")
      .append(_simpleName)
      .append("\u001b[0;31m<\u001b[1;31m")
      .append(cn1)
      .append("\u001b[0m@\u001b[1;35m")
      .append(obj1)
      .append("\u001b[0m")
      .append(", ")
      .append("\u001b[1;31m")
      .append(cn2)
      .append("\u001b[0m@\u001b[1;35m")
      .append(obj2)
      .append("\u001b[0m\u001b[0;31m>\u001b[0m").toString();
  }
  
  
  public int hashCode() {
    int left = (int) ((longHashCode & 0xFFFFFFFF00000000L) >> 32);
    int right = (int) ((longHashCode & 0xFFFFFFFFL) >> 0);
    return (left * 3280811) ^ (right * 1741589337);
  }
  
  
  protected static long longHashCode(Object o1, Object o2) {
    long left
      = (int) ((Integer)  
          ((o1 instanceof Integer)
            ? (Integer) o1
            : Integer.valueOf(System.identityHashCode(o1)))).intValue();
    long right
      = (int) ((Integer)  
          ((o2 instanceof Integer)
            ? (Integer) o2
            : Integer.valueOf(System.identityHashCode(o2)))).intValue();
    if (left == 0 && right == 0) {
      left += 1; 
      right -= 1;
    }
    return (0xFFFFFFFF00000000L & (left  << 32)) 
         | (0x00000000FFFFFFFFL & (right <<  0));
  }
  
  protected static long longHashCode(int left, int right) {
    if (left == 0 && right == 0) {
      left += 1; 
      right -= 1;
    }
    return (0xFFFFFFFF00000000L & (((long) left)  << 32)) 
         | (0x00000000FFFFFFFFL & (((long) right) <<  0));
  }
  
  
  @Test
  public static void main() {
    StringBuilder sb = new StringBuilder(1000);
    System.err.println();
    for (Map.Entry<Thread, StackTraceElement[]> e:
         Thread.getAllStackTraces().entrySet())
    {
      sb.append(String.format("Thread: %s\n", e.getKey()));
      for (StackTraceElement el: e.getValue()) {
        sb.append(String.format("\tat %s\n", el.toString()));
      }
      sb.append("\n\n");
    }
    System.out.println(sb);
    sb = null;
    
    Boolean oldDa = null;
    Field _assertionsDisabled = null;
    try {
      _assertionsDisabled
        = ContextKey.class.getDeclaredField("$assertionsDisabled");
      _assertionsDisabled.setAccessible(true);
      System.err.println(_assertionsDisabled.toGenericString());
      oldDa = (Boolean) _assertionsDisabled.get(null);
      System.err.printf("oldDa = %s\n", oldDa);
      _assertionsDisabled.set(null, Boolean.FALSE);
      System.err.printf("saved %s setting: %s\n", 
        _assertionsDisabled.getName(), oldDa);
      System.err.printf("updated %s setting; new value: %s\n", 
        _assertionsDisabled.getName(), _assertionsDisabled.get(null));
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
    }    
    
    try {
      Map<Object,Object> map = new HashMap<>();
      map.put("name", "David");
      map.put("otherKey", null);
      
      ContextKey key1 = ContextKey.of(Thread.currentThread(), map);
      System.err.println(      
        "key1 = ContextKey.of(Thread.currentThread(), map);");
      System.err.printf("ContextKey key1 [%016x] = %s\n", 
        (Long) longHashCode(key1.obj1, key1.obj2), key1);
      System.err.printf(
        "ContextKey key1.hashCode(): %08x\n", key1.hashCode());
      System.err.println();        
      
      System.err.printf("Map hashCode: %08x  ->  %s\n", map.hashCode(), map);
      System.err.printf("Removing from map: %s\n", map.remove("name"));
      System.err.printf("Map hashCode: %08x  ->  %s\n", map.hashCode(), map);
      System.err.println();
   
      System.err.printf("ContextKey key1 [%016x] = %s\n", 
        (Long) longHashCode(key1.obj1, key1.obj2), key1);
      System.err.printf(
        "ContextKey key1.hashCode(): %08x\n", key1.hashCode());
      System.err.println();
      
      ContextKey key2 = ContextKey.of(Thread.currentThread(), map);
      System.err.println(
        "key2 = ContextKey.of(Thread.currentThread(), map);");
      System.err.printf("ContextKey key2 [%016x] = %s\n", 
        (Long) longHashCode(key2.obj1, key2.obj2), key2);
      System.err.printf(
        "ContextKey key2.hashCode(): %08x\n", key2.hashCode());
      System.err.println();
      
      System.err.printf(
        "key1.equals(key2): %s\n",  Boolean.valueOf(key1.equals(key2)));
      assert key1.equals(key2):                    "key1.equals(key2)";
      System.err.printf(
        "key2.equals(key1): %s\n",  Boolean.valueOf(key2.equals(key1)));
      assert key2.equals(key1):                    "key2.equals(key1)";
      System.err.println();
      
      System.err.printf(
        "key1.compareTo(key2): %s\n",    key1.compareTo(key2));
      assert key1.compareTo(key2) == 0: "key1.compareTo(key2) == 0";
      System.err.println();
   
      ContextKey key3 = new ContextKey(Thread.currentThread(), map);
      System.err.println(
        "key3 = new ContextKey(Thread.currentThread(),map);");
      System.err.printf("ContextKey key3 [%016x] = %s\n", 
        (Long) longHashCode(key3.obj1, key3.obj2), key3);
      System.err.printf(
        "ContextKey key3.hashCode(): %08x\n", key3.hashCode());
      System.err.println();
      
      ContextKey key4 = new ContextKey(Runtime.getRuntime(), map);
      System.err.println(
        "key4 = new ContextKey(Runtime.getRuntime(), map);");
      System.err.printf("ContextKey key4 [%016x] = %s\n", 
        (Long) longHashCode(key4.obj1, key4.obj2), key4);
      System.err.printf(
        "ContextKey key4.hashCode(): %08x\n", key4.hashCode());
      System.err.println();
      
      assert key1.hashCode() == key3.hashCode():
            "key1.hashCode() == key3.hashCode():";
      
      System.err.printf(
        "key1.equals(key3): %s\n",  Boolean.valueOf(key1.equals(key3)));
      assert key1.equals(key3):                  "key1.equals(key3)";
      System.err.printf(
        "key2.equals(key3): %s\n",  Boolean.valueOf(key2.equals(key3)));
      assert key2.equals(key3):                  "key2.equals(key3)";
      System.err.printf(
        "key1.equals(key2): %s\n",  Boolean.valueOf(key1.equals(key2)));
      assert key1.equals(key2):                  "key1.equals(key2)";
      System.err.println();
      
      System.err.printf(
        "key1.equals(key4): %s\n",  Boolean.valueOf(key1.equals(key4)));
      assert !key1.equals(key4):                  "!key1.equals(key4)";
      System.err.printf(
        "key3.equals(key4): %s\n",  Boolean.valueOf(key3.equals(key4)));
      assert !key3.equals(key4):                  "!key3.equals(key4)";
      System.err.printf(
        "key4.equals(key3): %s\n",  Boolean.valueOf(key4.equals(key3)));
      assert !key4.equals(key3):                  "!key4.equals(key3)";
      
      Assert.assertSame("(Equal keys, equals() contract: 1 - 3): "
        + "key1.equals(key3) should return true", key1, key3);
      Assert.assertSame("(Equal keys, equals() contract: 2 - 3): "
        + "key2.equals(key3) should return true", key2, key3);
      Assert.assertSame("(Transitive  equals() contract: 3 - 2): "
        + "key3.equals(key2) should return true", key3, key2);
      System.err.println();
      
      Assert.assertNotSame("(Unequal keys,equals() contract: 1 - 4): "
        + "key1.equals(key4) should return false", key1, key4);
      Assert.assertSame("(Equal keys,  equals() contract: 3 - 1): "
        + "key3.equals(key1) should return true",  key3, key1);
      Assert.assertNotSame("(Transitive   equals() contract: 4 - 3): "
        + "key4.equals(key3) should return false", key4, key3);      
      System.err.println();
      
      System.err.printf(
        "key1.compareTo(key4): %s\n",  key1.compareTo(key4));
      System.err.printf(
        "key4.compareTo(key1): %s\n",  key4.compareTo(key1));
      System.err.printf(
        "key1.compareTo(null): %s\n",  key1.compareTo(null));
      System.err.printf(
        "key4.compareTo(null): %s\n",  key4.compareTo(null));
      
      Assert.assertEquals(
        "key1.compareTo(key4) == (-key4.compareTo(key1))",
        key1.compareTo(key4),
        (key4.compareTo(key1) * (-1))
      );
      System.err.println("finished!");
      
      
    } finally {
      if (oldDa != null) {
        try {
          _assertionsDisabled.set(null, oldDa);
          System.err.printf("restored %s setting: %s\n", 
            _assertionsDisabled.getName(), oldDa);
          
        } catch (ReflectiveOperationException ex) {
          ex.printStackTrace();
        } finally {
          _assertionsDisabled = null;
        }
      }
    }
  }
  
}




