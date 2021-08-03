package org.d6r;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Scanner;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import com.google.common.base.Function;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;


public class DateUtil {
  
  public static final TimeZone LOCAL_TZ;
  public static final String LOCAL_TZ_ID;
  public static final long LOCAL_TZ_RAW_OFFSET;
  public static final double LOCAL_TZ_OFFSET_HOURS;
  public static final Map<String, SimpleDateFormat> cache;
  
  static {
    LOCAL_TZ_ID = getLocalTzId();
    LOCAL_TZ = (LOCAL_TZ_ID != null)
      ? TimeZone.getTimeZone(LOCAL_TZ_ID)
      : TimeZone.getDefault();
    LOCAL_TZ_RAW_OFFSET = LOCAL_TZ.getRawOffset();
    LOCAL_TZ_OFFSET_HOURS 
      = ((double) LOCAL_TZ_RAW_OFFSET) / 1000D / 60D / 60D;
    Map<String, SimpleDateFormat> _cache;
    /*try {
      Class<?> cls_LocalCache = Class.forName(
        "com.google.common.cache.LocalCache");
      Class<?> cls_CacheBuilder = Class.forName(
        "com.google.common.cache.CacheBuilder");
      Class<?> cls_CacheLoader = Class.forName(
        "com.google.common.cache.CacheLoader");
      Method m_CL_from = cls_CacheLoader.getDeclaredMethod(
        "from", Function.class
      );
      m_CL_from.setAccessible(true);
      Constructor<?> ctor_CB = cls_CacheBuilder
        .getDeclaredConstructor();
      ctor_CB.setAccessible(true);
      Constructor<?> ctor_LC = cls_LocalCache
        .getDeclaredConstructor(
          cls_CacheBuilder, cls_CacheLoader
        );
      ctor_LC.setAccessible(true);
      Function<String, SimpleDateFormat> func = new 
      Function<String, SimpleDateFormat>() { 
        @Override
        public SimpleDateFormat apply(String key) { 
          return new SimpleDateFormat(key);
        }     
      };
      Object cacheLoader = m_CL_from.invoke(null, func);
      _cache = (Map<String, SimpleDateFormat>) (Map<?,?>)
        ctor_LC.newInstance(
          ctor_CB.newInstance(),
          m_CL_from.invoke(null, func)
        );
    } catch (ReflectiveOperationException ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();*/
      _cache = new WeakHashMap<String, SimpleDateFormat>(24);
    /*}*/
    cache = _cache;
  }
  
  static String getLocalTzId() {
    ProcessBuilder pb = new ProcessBuilder("date");
    Process proc = null;
    Closeable[] streams = new Closeable[3];
    String output = null;
    try {
      proc = pb.start();
      streams = new Closeable[] {
        proc.getOutputStream(), proc.getInputStream(), 
        proc.getErrorStream()        
      };
      output = IOUtils.toString((InputStream) streams[1]);
      if (output != null) output = output.trim(); 
      if (output == null || output.length() == 0) {
        try {
          System.err.printf(
            "Error running command `date`: [%s]\n",
            IOUtils.toString((InputStream) streams[2])
          );
        } catch (Throwable e) {
          if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        }
        return TimeZone.getDefault().getID();
      }
    } catch (IOException e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace(System.err);
      return TimeZone.getDefault().getID();
    } finally {
      for (Closeable stream: streams) {
        if (stream == null) continue;
        try {
          stream.close();
        } catch (Throwable e) {
          if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        }
      }
      if (proc != null) proc.destroy();
    }
    Scanner sc = new Scanner(output); 
    String cur;
    String[] ids = TimeZone.getAvailableIDs(); 
    while (sc.hasNext()) { 
      cur = sc.next(); 
      int i = Arrays.binarySearch(ids, 0, ids.length-1, cur);
      if (i < 0) continue; 
      return cur;
    }
    return null;
  }
  
  public static SimpleDateFormat getFormat(String fmt) {
    if (fmt == null || fmt.length() == 0) {
      throw new IllegalArgumentException(
        "DateUtil.getFormat(String fmt): "
        + "Format string must not be null or empty."
      );
    }
    fmt.intern();
    SimpleDateFormat sdf = cache.get(fmt);
    if (sdf == null) {
      cache.put(fmt, (sdf = new SimpleDateFormat(fmt)));
    }
    return sdf;
  }  
  
  public static Date getLocalDate() {
    return new Date(
      System.currentTimeMillis() + LOCAL_TZ_RAW_OFFSET
    ); 
  }
  
  public static long getLocalMillis() {
    return System.currentTimeMillis() + LOCAL_TZ_RAW_OFFSET;
  }
  
  public static long getLocalEpoch() {
    return System.currentTimeMillis() / 1000L;
  }
  
  public static String format(String fmt) {
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(new Date()); 
    return dateStr;
  }
  
  public static String format(Date date, String fmt) {
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(date); 
    return dateStr;
  }
  
  public static String format(long ms, String fmt) {
    Date date = new Date(ms);
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(date); 
    return dateStr;
  }
  
  
  public static String formatLocal(String fmt) {
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(getLocalDate());
    return dateStr;
  }
  
  public static String formatLocal(long ms, String fmt) {
    long localMillis = ms + LOCAL_TZ_RAW_OFFSET;
    Date localDate = new Date(localMillis);
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(localDate); 
    return dateStr;
  }
  
  public static String formatLocal(Date date, String fmt) {
    long localMillis
      = ((Long) Reflect.getfldval(date, "milliseconds"))
          .longValue() + LOCAL_TZ_RAW_OFFSET;
    Date localDate = new Date(localMillis);
    SimpleDateFormat sdf = getFormat(fmt); 
    String dateStr = sdf.format(localDate); 
    return dateStr;
  }
  
}
