package bsh;

import bsh.Capabilities;

public abstract class ReflectManager {
  private static ReflectManager rfm;

  public static ReflectManager getReflectManager() throws Capabilities.Unavailable {
    if(rfm == null) {
      try {
        Class clas = Class.forName("bsh.reflect.ReflectManagerImpl");
        rfm = (ReflectManager)clas.newInstance();
      } catch (Exception var2) {
        throw new Capabilities.Unavailable("Reflect Manager unavailable: " + var2);
      }
    }

    return rfm;
  }

  public static boolean RMSetAccessible(Object obj) throws Capabilities.Unavailable {
    return getReflectManager().setAccessible(obj);
  }

  public abstract boolean setAccessible(Object var1);
}
