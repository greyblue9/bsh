package org.d6r;


import java.util.ArrayList;
import java.io.PrintStream;
import java.util.Arrays;


public class ClassResolveUtils {
  
  public static PrintStream debugOut = System.err;
  private static ArrayList<String> parts;
  
  public static Class<?> findInnerClass
  (String clzname)
  {
    return findInnerClass(
      clzname, 
      Thread.currentThread().getContextClassLoader()
    );
  }
  
  public static Class<?> findInnerClass
  (String clzname, ClassLoader ldr) 
  {
    return findInnerClass(clzname, ldr, false);
  }
  
  public static Class<?> findInnerClass
  (String clzname, boolean debug)
  {
    return findInnerClass(
      clzname, 
      Thread.currentThread().getContextClassLoader(),
      debug
    );
  }
  

  
  public synchronized static Class<?> findInnerClass
  (String clzname, ClassLoader ldr, boolean debug)
  {
    
    Class<?> cls = null; 
    parts = new ArrayList<String>() {};
    int idx = 0; 
    String part = null;
    
    while (true) { 
      idx = clzname.indexOf('.'); 
      if (idx == -1) break;  
      
      part = clzname.substring(0, idx); 
      clzname = clzname.substring(idx + 1); 
      parts.add(part); 
      
      if (debug) {
        debugOut.println(String.format(
          "[%s] [%s] (%d) : %s", 
          clzname, part, clzname.indexOf(-1),
          java.util.Arrays.toString(parts.toArray())
        ));
      }
    } 
    parts.add(clzname); 
    
    if (debug) {
      debugOut.println(Arrays.toString(parts.toArray())); 
    }
    
    Object[] _parts = parts.toArray(); 
    String chars = null; 
    
    int max = (int) Math.pow(2,_parts.length); 
    String fmt = String.format("%%%ds", _parts.length); 
    
    for (int i=0; i<max; i++) {
      chars = String.format(
        fmt, 
        Integer.toBinaryString(i)
          .replace(' ','.')
          .replace('0','.')
          .replace('1','$')
      ); 
      chars = chars.replace(' ','.'); 
      
      StringBuilder _clzname 
 = new StringBuilder(clzname.length()); 
      
      for (int j=0; j<_parts.length; j++) { 
        _clzname.append(_parts[j].toString()); 
        if (j != _parts.length - 1) { 
          _clzname.append(chars.charAt(j));
        } 
      }
      try {
        cls = ldr.loadClass(_clzname.toString()); 
        return cls;
      } catch (Throwable e) {} 
    } // end class name attempt loop
    
    return null;  
  }
  
}





