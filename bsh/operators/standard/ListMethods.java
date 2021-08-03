package bsh.operators.standard;

import bsh.operators.Extension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.d6r.CollectionUtil;
import org.d6r.Debug;

public class ListMethods implements Serializable {
  @Extension
  public static List asList(double[] array) {
    return Arrays.asList(new double[][]{array});
  }

  @Extension
  public static List asList(float[] array) {
    return Arrays.asList(new float[][]{array});
  }

  @Extension
  public static List asList(int[] array) {
    return Arrays.asList(new int[][]{array});
  }

  @Extension
  public static List asList(String[] array) {
    return Arrays.asList(array);
  }

  public static <E> E getAt(List<? extends E> list, Integer index) {
    return (E) CollectionUtil.getAt(list, index.intValue());
  }

  public static <E> E getAt(ListIterator<? extends E> lit, Integer index) {
    return (E) CollectionUtil.getAt(lit, index.intValue());
  }

  public static <E> E getAt(Object traversible, Integer index) {
    if(traversible instanceof List) {
      return (E) getAt((Object)((List)traversible), (Integer)index);
    } else if(traversible instanceof ListIterator) {
      return (E) getAt((Object)((ListIterator)traversible), (Integer)index);
    } else if(traversible instanceof Iterable) {
      return (E) getAt((Object)((Iterable)traversible), (Integer)index);
    } else {
      Object[] ex1;
      if(traversible instanceof Iterator) {
        ex1 = CollectionUtil.toArray((Iterator)traversible);
        return ex1 != null? (E) ex1[index.intValue()]: (E)null;
      } else if(traversible instanceof Enumeration) {
        ex1 = CollectionUtil.toArray((Enumeration)traversible);
        return ex1 != null? (E)ex1[index.intValue()]: (E)null;
      } else {
        RuntimeException ex;
        (ex = new RuntimeException(String.format("ListMethods#getAt(Object traversible=%s, Integer index=%s): \n  Cannot handle type \'%s\': \n    %s \n\n", new Object[]{traversible != null?String.format("%s@%8x", new Object[]{traversible.getClass().getName(), Integer.valueOf(System.identityHashCode(traversible))}):"<null>", index != null?index.toString():"<null>", traversible != null?traversible.getClass().getName():"<null>", Debug.ToString(traversible)}))).printStackTrace();
        throw ex;
      }
    }
  }

  public static <E> E getAt(Iterable<? extends E> itb, Integer index) {
    return (E) CollectionUtil.getAt(itb, index.intValue());
  }

  public static <E> List<E> getAt(List list, int[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      result.add(list.get(j));
    }

    return (List<E>) result;
  }

  public static void putAt(List list, Integer index, Integer value) {
    list.set(index.intValue(), value);
  }

  public static void putAt(List list, int[] indices, int[] values) {
    for(int i = 0; i < indices.length; ++i) {
      int j = indices[i];
      list.set(j, Integer.valueOf(values[i]));
    }

  }

  public static <E> List<E> cast(int[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      result.add(Integer.valueOf(indices[i]));
    }

    return (List<E>) result;
  }

  public static <E> List<E> cast(float[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      result.add(Float.valueOf(indices[i]));
    }

    return (List<E>) result;
  }

  public static <E> List<E> cast(double[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      result.add(Double.valueOf(indices[i]));
    }

    return (List<E>) result;
  }

  public static <E> List<E> cast(Object[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      result.add(indices[i]);
    }

    return (List<E>) result;
  }

  public static <E> List<E> cast(String[] indices) {
    ArrayList result = new ArrayList(indices.length);

    for(int i = 0; i < indices.length; ++i) {
      result.add(indices[i]);
    }

    return (List<E>) result;
  }
}