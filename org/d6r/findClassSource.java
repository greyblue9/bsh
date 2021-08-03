package org.d6r;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import org.apache.commons.lang3.StringUtils;


public class findClassSource {
  
  static final LazyMember<Method> getBootstrapResources = LazyMember.of(
    ClassLoader.class, "getBootstrapResources", String.class
  );
  
  public static URL findClassSource(final String name) {
    final String classNameAsPath = ClassInfo.classNameToPath(
      (name.indexOf('/') != -1) ? ClassInfo.typeToName(name): name,
      "class"
    );
    final URL[] urlArray = CollectionUtil.toArray(
      CollectionUtil.asIterable(
        getBootstrapResources.<Enumeration<? extends URL>>invoke(
          null, classNameAsPath
        )
      ), URL.class
    );  
    final List<URL> urls = new ArrayList<URL>(Arrays.<URL>asList(urlArray));
    
    Class<?> cls = null;
    try {
      cls = Class.forName(
        name,
        false,
        Thread.currentThread().getContextClassLoader()
      );
    } catch (final Throwable e) {
      e.printStackTrace();
      try {
        cls = Class.forName(
          name, false, ClassLoader.getSystemClassLoader()
        );
      } catch (final Throwable e2) {
        e.printStackTrace();
      }
    }
    
    final ClassLoader loader = (cls != null) ? cls.getClassLoader() : null;
    
    if (loader == null) {
      for (final URL url: urls) {
        return url;
      }
    }
    for (final ClassLoader altLoader: new ClassLoader[]{
      null,
      Thread.currentThread().getContextClassLoader().getParent(),
      Thread.currentThread().getContextClassLoader(),
      ClassLoader.getSystemClassLoader().getParent(),
      ClassLoader.getSystemClassLoader(),
      findClassSource.class.getClassLoader(),
      (findClassSource.class.getClassLoader() != null)
       ? findClassSource.class.getClassLoader().getParent()
       : Class.class.getClassLoader()
    })
    {
      if (loader == null && altLoader == null) continue;
      try {
        final Enumeration<URL> en = (
          (altLoader != null) ? altLoader : loader
        ).getResources(classNameAsPath);
        if (en == null) continue;
        while (en.hasMoreElements()) {
          final URL current = en.nextElement();
          if (current != null) return current;
          throw new AssertionError(String.format(
            "loader[%s].hasMoreElements() && nextElement() == null", loader
          ));
        }
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    for (final URL url: urls) {
      return url;
    }
    return null;
  }
  
  
  public static Object invoke(bsh.Interpreter in, bsh.CallStack cs, Object o)
  {
    if (CollectionUtil.isJRE()) {
      return findClassSource(
        (o instanceof CharSequence)
          ? String.valueOf((CharSequence) o)
          : StringUtils.substringAfterLast(String.format("%s", o), " ")
      );
    }
    
    return DexFinder.findDexElement(o);
  }
}
