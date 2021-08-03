package org.d6r;


import java.util.Arrays;
import java8.util.Spliterators;
import java8.util.stream.StreamSupport;
import java8.util.Spliterator;
import java.lang.reflect.Field;
import java8.util.stream.DoubleStream;
import java8.util.stream.IntStream;
import java8.util.stream.LongStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.collections4.IterableUtils;


public class NumberUtil {
  
  public static int sum(Iterable<Integer> ints) {
    final Integer[] arr = IterableUtils.toList(ints).toArray(new Integer[0]);
    return sum(arr);
  }
  
  public static int sum(Integer... ints) {
    int[] arr = ArrayUtils.toPrimitive(ints);
    return sum(arr);
  }
  
  public static int sum(int[] ints) {
    return StreamSupport.intStream(
      Spliterators.spliterator(ints, 0).trySplit(), true
    ).sum();
  }
  
  public static int[] collect(Iterable<? extends Object> objs, String fieldName) {
    
    final int size = IterableUtils.size(objs);
    final int[] ints = new int[size];
    Field fld = null;
    int i = -1;
    Class<?> cls = null;
    for (final Object o: objs) {
      if (o == null) continue;
      try {
        if (fld == null || !cls.isInstance(o)) {
          fld = Reflect.getfld((cls = o.getClass()), fieldName);
          fld.setAccessible(true);
        }
        final int value = fld.getInt(o);
        ints[++i] = value;
      } catch (final ReflectiveOperationException ex) {
        Log.w("NumberUtil", ex);
      }
    }
    return i == size? ints: Arrays.copyOfRange(ints, 0, i);
  }
  
}



 
 
