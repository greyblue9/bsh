package org.d6r;

import java.lang.reflect.Field;
import java.util.WeakHashMap;
import org.d6r.Reflector;


public class StackTraceUtil {
  
  private static WeakHashMap<Object, StackTraceElement> cache
           = new WeakHashMap<Object, StackTraceElement>(32);
  
  static Field STACK_TRACE;
  
  public static Integer getKey(Object... keyParts) {
    int hashCode = 0xFAFAFAFA;
    
    for (int i=0, len=keyParts.length; i<len; ++i) {
      Object obj = keyParts[i];
      try {
        hashCode ^= (obj != null? obj.hashCode(): 0x12345678);
      } catch (Throwable e) {
        System.err.printf(
          "[WARN] Object.hashCode() threw %s for %s instance: %s\n",
          e.getClass().getSimpleName(),
          obj != null? String.valueOf(obj.getClass()): "<null>",
          (e.getMessage() != null)
            ? e.getMessage()
            : Reflector.getRootCause(e).getMessage()
        ); 
      }
      hashCode ^= ((hashCode << 11) | (hashCode & 0x000007FF));
    }
    
    return Integer.valueOf(hashCode);
  }
  
  public static String getSourceFileName(String className) {
    int lastDot = className.lastIndexOf('.');
    int dollarAfterDot = className.indexOf('$', lastDot+1);
    return (
      (dollarAfterDot != -1 && dollarAfterDot > lastDot+1)
        ? className.substring(lastDot+1, dollarAfterDot)
        : ((lastDot != -1)
            ? className.substring(lastDot+1)
            : className)
    ).concat(".java");
  }
  
  public static StackTraceElement getElement(Class<?> cls, String mtdName) {
    Integer hc = getKey(cls, mtdName);
    StackTraceElement ste = cache.get(hc);
    if (ste == null) {
      String className = cls.getName();
      String srcFileName = getSourceFileName(className);
      cache.put(hc, (ste = new StackTraceElement(      
        className, // String cls
        mtdName, // String method
        getSourceFileName(className), // String file
        1 // int line
      )));
    }
    return ste;
  }
  
  public static int count(StackTraceElement[] stes, Class<?> cls) {
    String className = cls.getName();
    int hc = className.hashCode();
    int count = 0;
    for (int i=0, len=stes.length; i<len; ++i) {
      count += (hc == stes[i].getClassName().hashCode()) ? 1: 0;      
    }
    return count;
  }
  
  public static int count(StackTraceElement[] stes, Class<?> cls, 
  String mtdName) 
  {
    String className = cls.getName();    
    int classNameHc = className.hashCode();
    int methodNameHc = mtdName.hashCode();
    int count = 0;
    for (int i=0, len=stes.length; i<len; ++i) {
      count += (
            classNameHc == stes[i].getClassName().hashCode()
        && methodNameHc == stes[i].getMethodName().hashCode()
      ) ? 1: 0;
    }
    return count;
  }
  
  public static int count(Class<?> cls) {
    return count(getStackTrace(), cls);
  }
  
  public static int count(Class<?> cls, String mtdName) {
    return count(getStackTrace(), cls, mtdName);
  }
  
  public static int count(Throwable ex, Class<?> cls) {
    return count(getStackTrace(ex), cls);
  }
  
  public static int count(Throwable ex, Class<?> cls, String mtdName) {
    return count(getStackTrace(ex), cls, mtdName);
  }
  
  public static StackTraceElement[] getStackTrace(Throwable ex) {
    try {
      if (STACK_TRACE == null) {
        STACK_TRACE = Throwable.class.getDeclaredField("stackTrace");
        STACK_TRACE.setAccessible(true);
      }
      StackTraceElement[] stackTrace 
        = (StackTraceElement[]) STACK_TRACE.get(ex);
      if (stackTrace != null) return stackTrace;
      return ex.getStackTrace();
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return new StackTraceElement[0];
    }
  }
  
  public static StackTraceElement[] getStackTrace() {
    return getStackTrace(new Error());
  }
  
}
