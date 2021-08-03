package org.d6r;

import java.io.IOException;
import java.net.*;
import java.util.*;
import dalvik.system.*;
import java.lang.reflect.*;
import static org.d6r.Reflect.getfldval;


public class ClassLoaders {
  
  public static List<URL> getResources(BaseDexClassLoader ldr, String name) {
    return getResources((ClassLoader) ldr, name);
  }
  
  public static List<URL> getResources(ClassLoader ldr, String name) {
    try {
      List<URL> urls = null;
      for (Enumeration<URL> en = ldr.getResources(name);
           en.hasMoreElements();)
      {
        if (urls == null) urls = new LinkedList<URL>();
        urls.add(en.nextElement());
      }
      return urls;
    } catch (IOException ex) {
      return Collections.emptyList();
    }
  }
  
  public static List<URL> getResources(String name) {
    return getResources(
      Thread.currentThread().getContextClassLoader(), name
    );
  }
  
  static LazyMember<Method> GET_BOOT_CP_RES = LazyMember.of(
    "java.lang.VMClassLoader", "getBootClassPathResource", 
    String.class, Integer.TYPE
  );
  
  public static List<URL> getBootResources(String name) {
    String resName = null;
    List<URL> urls = new LinkedList<URL>();
    int i = 0;
    do {
      try {
        if ((resName = (String) GET_BOOT_CP_RES.get().invoke(
             null, "classes.dex", i++)) != null)
        {
          urls.add(new URL(resName));
        }
      } catch (MalformedURLException murlex) {
        System.err.printf(
          "[WARN] getBootClassPathResource() returned malformed URL: %s\n",
          resName
        );
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    } while (resName != null);
    return urls;
  }
  
}

