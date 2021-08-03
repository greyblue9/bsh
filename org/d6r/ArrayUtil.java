package org.d6r;

import bsh.operators.Extension;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.d6r.Dumper;
import org.d6r.StringCollectionUtil;

public class ArrayUtil {
  
  @Extension
  public static <T> T[] removeNulls(T[] arr) {
    int i = 0;
    int j = arr.length - 1;
    while (i <= j) {
      if (arr[j] == null) {
        j -= 1;
      } else if (arr[i] != null) {
        i += 1;
      } else {
        arr[i] = arr[j];
        arr[j] = null;
        i += 1;
        j -= 1;
      }
    }
    return (T[]) Arrays.copyOfRange(arr, 0, i);
  }

  @Extension
  public static <T> T[] removeMatching(T[] arr, Matcher mchr) {
    int i = 0;
    int j = arr.length - 1;
    while (i <= j) {
      mchr.reset(Dumper.tryToString(arr[j]));
      if (arr[j] == null
      ||  mchr.reset(Dumper.tryToString(arr[j])).find()) 
      {
        j -= 1;
      } else if (arr[i] == null
      || mchr.reset(Dumper.tryToString(arr[i])).find()) 
      {
        i += 1;
      } else {
        arr[i] = arr[j];
        arr[j] = null;
        i += 1;
        j -= 1;
      }
    }
    return (T[]) Arrays.copyOfRange(arr, 0, i);
  }

  @Extension
  public static <T> T[] removeMatching(T[] arr, Pattern ptrn) {
    return removeMatching(arr, ptrn.matcher(""));
  }
  
  @Extension
  public static <T> T[] removeMatching(T[] arr, String regex) {
    return 
      removeMatching(arr, StringCollectionUtil.toPattern(regex));
  }
  
  public static void checkPositionIndexes(final int start, final int end,
  final int size)
  {
    if (!(start < 0 || end < start || end > size)) {
      return;
    }
    if (!(start < 0 || start > size) && !(end < 0 || end > size)) {
      throw new IndexOutOfBoundsException(String.format(
        "end index (%1$d) must not be less than start index (%2$d)", end, start
      ));
    }
    final int index = (start < 0 || start > size)? start: end;
    throw new IllegalArgumentException(String.format(
      (index < 0) ? "%1$s index (%2$d) must not be negative" :
      (size  < 0) ? "negative size: %3$d" :
                    "%1$s index (%2$d) must not be greater than size (%3$d)",
      (start < 0 || start > size)? "start": "end",
      index,
      size
    ));   
  }



}


