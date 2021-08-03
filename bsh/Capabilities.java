package bsh;

import bsh.BshClassManager;
import bsh.UtilEvalError;
import java.util.WeakHashMap;
import dalvik.system.XClassLoader;
import org.d6r.SoftHashMap;
import java.lang.reflect.Method;

public class Capabilities {
  private static volatile boolean accessibility = true;
  private static SoftHashMap<String, Object> classes 
           = new SoftHashMap<>();
           
  public static Object NULL = new Object();

  public static boolean haveSwing() {
    return classExists("javax.swing.JButton");
  }

  public static boolean haveAccessibility() {
    return true;
  }

  public static void setAccessibility(boolean b) 
    throws Capabilities.Unavailable 
  {
    BshClassManager.clearResolveCache();
  }
  
  static Method classForName;

  public static boolean classExists(String name) {
    ClassLoader ldr = Thread.currentThread().getContextClassLoader();
    if (classes.containsKey(name)) {
      return classes.get(name) instanceof Class;
    } else {
      Class c;
      try {
        if (ldr != null &&  ldr.getClass().getName().equals(
          "dalvik.system.XClassLoader") &&
          !System.getProperty("java.specification.name").startsWith("Java"))
        {
          if (classForName == null) {
            (classForName = ldr.getClass().getDeclaredMethod(
              "classForName", String.class
            )).setAccessible(true);
          }
          c = (Class<?>) classForName.invoke(ldr, name);
        } else {
          c = Class.forName(name, false, ldr);
        }
      } catch (Throwable var3) {
        classes.put(name, var3);
        return false;
      } 
      if (c == null) classes.put(name, NULL);
      else classes.put(name, c);
      
      return c != null;
    }
  }

  public static class Unavailable extends UtilEvalError {
    public Unavailable(String s) {
      super(s);
    }
  }
}
