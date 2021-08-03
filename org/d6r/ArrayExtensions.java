package org.d6r;

import bsh.operators.Extension;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import java.lang.reflect.Array;

public class ArrayExtensions {
  
  /*@Extension
  public static <T> T[] remove(T[] arr, int index) {
    return ArrayUtils.addAll(
      Arrays.copyOfRange(arr, 0, index),
      Arrays.copyOfRange(
        arr,
        index < arr.length-1? index+1: arr.length,
        arr.length
      )
    );      
  }*/
  
  @Extension
  public static <T> T[] reverse(T[] arr) {
    if (arr == null) return null;
    final T[] ret = (T[])
      Array.newInstance(arr.getClass().getComponentType(), arr.length);
    for (int i=0, len=arr.length; i<len; ++i) {
      final int retIndex = len - (i+1);
      ret[retIndex] = arr[i];
    }
    return ret;
  }  
  
  @Extension
  public static <T> T[] remove(T[] arr, int index) {
    if (arr == null) return null;
    return ArrayUtils.addAll(
      Arrays.copyOfRange(arr, 0, index),
      Arrays.copyOfRange(
        arr,
        index < arr.length-1? index+1: arr.length,
        arr.length
      )
    );      
  }
  
  @Extension
  public static <T> T[] remove(T[] arr, int startIndex, int endIndex) {
    if (arr == null) return null;
    return ArrayUtils.addAll(
      Arrays.copyOfRange(arr, 0, startIndex),
      Arrays.copyOfRange(
        arr,
        endIndex < arr.length? endIndex: arr.length,
        arr.length
      )
    );      
  }
  
  @Extension
  public static <T> T[] subSequence(T[] arr, int startIndex, int endIndex) {
    if (arr == null) return null;
    return Arrays.copyOfRange(arr, startIndex, endIndex); 
  }
  
  @Extension
  public static <T> T[] takeExactly(T[] arr, int count) {
    if (arr == null) throw new IllegalArgumentException(String.format(
      "take(arr, %1$d): arr == null", count
    ));
        
    final int len = arr.length;
    if (len < count) throw new IllegalArgumentException(String.format(
      "take(arr, %1$d): arr.length == %2$d; %2$d < %1$d", count, len
    ));
        
    return Arrays.copyOfRange(arr, 0, count); 
  }
  
  @Extension
  public static <T> T[] take(T[] arr, int count) {
    if (arr == null) return null;
    final int len = arr.length;
    return Arrays.copyOfRange(arr, 0, (count>len)? len: count);
  }
  
  @Extension
  public static <T> T firstOrDefault(T[] arr) {
    for (int i=0, len=(arr!=null? arr.length: 0); i<len; ++i) {
      if (arr[i] != null) return arr[i];
    }
    return null;
  }
  
  @Extension
  public static <T> T firstOrDefault(T[] arr, T defaultValue) {
    for (int i=0, len=(arr!=null? arr.length: 0); i<len; ++i) {
      if (arr[i] != null) return arr[i];
    }
    return defaultValue;
  }
  
  
  
}