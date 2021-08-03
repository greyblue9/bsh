package org.d6r;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;


public class DefaultRelatedClassFinder
  implements RelatedClassFinder, 
             AutoCloseable
{

  protected ClassLoader loader;

  public final Map<String, Throwable> errors
     = new TreeMap<String, Throwable>();
  
  
  public DefaultRelatedClassFinder(@Nullable ClassLoader loader) {
    if (loader != null)
      this.loader = loader;
  }
  
  
  @Override
  public boolean tryFindRelated(String className,
  Set<? super String> dest)
  {
    if (loader == null)
      return false;
    if (errors.containsKey(className))
      return false;
    Class<?> cls = null;
    try {
      cls = Class.forName(className, false, loader);
      if (cls == null) {
        errors.put(className, new Error(String.format("Class.forName(\"%s\", false, %s@@0x%08x) returned null", className, loader.getClass().getName(), System.identityHashCode(loader))));
        return false;
      }
      return collectClasses(cls, dest);
    } catch (ReflectiveOperationException | LinkageError cnfe) {
      errors.put(className, cnfe);
      return false;
    }
  }

  public Collection<String> getClassNames(Iterable<Class<?>> ic) {
    List<String> classNames = new LinkedList<String>();
    for (Class<?> cls : ic) {
      classNames.add(cls.getName());
    }
    return classNames;
  }

  public boolean collectClasses(Class<?> c, Set<? super String> dest) {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.addAll(
      (Collection<? extends Class<?>>) ClassInfo.getInterfaces(c)
    );
    classes.addAll(
      (Collection<? extends Class<?>>) ClassInfo.findInnerClasses(c)
    );
    List<Class<?>> additional = new ArrayList<Class<?>>();
    for (Class<?> foundClass : classes) {
      additional.addAll(
        (Collection<? extends Class<?>>) 
        ClassInfo.findInnerClasses(foundClass)
      );
    }
    classes.addAll(additional);
    return dest.addAll(getClassNames(classes));
  }
  
  @Override
  public void close() {
    try {
      System.err.printf(
        "[INFO] %s releasing ClassLoader", getClass().getSimpleName()
      );
    } finally {
      loader = null;
    }
  }
  
}

