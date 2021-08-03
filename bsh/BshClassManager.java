package bsh;

import bsh.Capabilities;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.Name;
import bsh.UtilEvalError;
import bsh.classpath.ClassManagerImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;


public class BshClassManager {
  public Interpreter declaringInterpreter;
  public ClassLoader externalClassLoader;
  
  public transient Map<String, Class> absoluteClassCache 
         = new TreeMap<String, Class>();
         
  public transient Set<String> absoluteNonClasses 
         = new TreeSet<String>();
         
  public static BshClassManager bcm = null;
  
  public transient Map<BshClassManager.SignatureKey, Method> 
    resolvedObjectMethods = new HashMap<>();
  
  public transient Map<BshClassManager.SignatureKey, Method>
    resolvedStaticMethods = new HashMap<>();
    
  public transient Set<String> definingClasses 
         = new TreeSet<String>();
  
  public transient Map<String, String> definingClassesBaseNames 
         = new TreeMap<String, String>();
         
  public static final Map<BshClassManager, Object> classManagers 
        = new WeakHashMap<>();

  static void clearResolveCache() {
    BshClassManager[] managers 
      = (BshClassManager[]) classManagers.keySet().toArray(
          new BshClassManager[0]
        );
    BshClassManager[] var4 = managers;
    int var3 = managers.length;

    for(int var2 = 0; var2 < var3; ++var2) {
      BshClassManager m = var4[var2];
      m.resolvedObjectMethods = new HashMap();
      m.resolvedStaticMethods = new HashMap();
    }

  }

  public static BshClassManager createClassManager(Interpreter interpreter) {
    BshClassManager manager = bcm;
    if(bcm != null) {
      return bcm;
    } else {
      manager = bcm = new ClassManagerImpl();
      if(interpreter != null) {
        manager.declaringInterpreter = interpreter;
      } else {
        System.err.println("BCM interpreter == null");
      }

      classManagers.put(manager, (Object)null);
      return manager;
    }
  }

  public boolean classExists(String name) {
    return this.classForName(name) != null;
  }

  public Class classForName(String name) {
    if (this.isClassBeingDefined(name)) {
      throw new InterpreterError(
        "Attempting to load class in the process of being defined: " + name
      );
    }
    Class<?> clas = null;
    /*if (name.indexOf('.') != name.lastIndexOf('.')
    &&  name.indexOf('/') == -1
    && ! StringUtils.endsWith(name, ".class")
    && ! StringUtils.endsWith(name, ".java")
    &&   Capabilities.classExists(name))
    {*/
       try {
         clas = this.plainClassForName(name);
       } catch (ClassNotFoundException var4) {
         var4.printStackTrace();
       }
    /*} else {
      if (Interpreter.DEBUG) System.err.printf(
        "BshClassManager: Skipping classForName(\"%s\")\n",
        name
      );
    }*/
      /*if (clas == null && this.declaringInterpreter.getCompatibility()) {
        clas = this.loadSourceClass(name);
      }*/

    return clas;
  }

  public Class<?> loadSourceClass(String name) {
    String fileName = '/' + name.replace('.', '/') + ".java";
    InputStream in = this.getResourceAsStream(fileName);
    if(in == null) {
      return null;
    } else {
      try {
        Interpreter.debug("Loading class from source file: " + fileName);
        this.declaringInterpreter.eval((Reader)(new InputStreamReader(in)));
      } catch (EvalError var6) {
        if(Interpreter.DEBUG && Interpreter.DEBUG) {
          var6.printStackTrace();
        }
      }

      try {
        return this.plainClassForName(name);
      } catch (ClassNotFoundException var5) {
        Interpreter.debug("Class not found in source file: " + name);
        return null;
      }
    }
  }

  public Class plainClassForName(String name) throws ClassNotFoundException {
    Class c = null;
    if (this.externalClassLoader != null) {
      c = this.externalClassLoader.loadClass(name);
    } else {
      c = Class.forName(
        name, false, Thread.currentThread().getContextClassLoader()
      );
    }

    this.cacheClassInfo(name, c);
    return c;
  }

  public URL getResource(String path) {
    URL url = null;
    if(this.externalClassLoader != null) {
      url = this.externalClassLoader.getResource(path.substring(1));
    }

    if (url == null) {
      url = Interpreter.class.getResource(path);
    }

    return url;
  }

  public InputStream getResourceAsStream(String path) {
    InputStream in = null;
    if (this.externalClassLoader != null) {
      in = this.externalClassLoader.getResourceAsStream(path.substring(1));
    }

    if(in == null) {
      in = Interpreter.class.getResourceAsStream(path);
    }

    return in;
  }

  public void cacheClassInfo(String name, Class value) {
    if(value != null) {
      this.absoluteClassCache.put(name, value);
    } else {
      this.absoluteNonClasses.add(name);
    }

  }

  public void cacheResolvedMethod(Class clas, Class[] types, Method method) {
    if(Interpreter.DEBUG) {
      Interpreter.debug("cacheResolvedMethod putting: " + clas + " " + method);
    }

    BshClassManager.SignatureKey sk = new BshClassManager.SignatureKey(clas, method.getName(), types);
    if(Modifier.isStatic(method.getModifiers())) {
      this.resolvedStaticMethods.put(sk, method);
    } else {
      this.resolvedObjectMethods.put(sk, method);
    }

  }

  public Method getResolvedMethod(Class clas, String methodName, Class[] types, boolean onlyStatic) {
    BshClassManager.SignatureKey sk = new BshClassManager.SignatureKey(clas, methodName, types);
    Method method = (Method)this.resolvedStaticMethods.get(sk);
    if(method == null && !onlyStatic) {
      method = (Method)this.resolvedObjectMethods.get(sk);
    }

    if(Interpreter.DEBUG) {
      if(method == null) {
        Interpreter.debug("getResolvedMethod cache MISS: " + clas + " - " + methodName);
      } else {
        Interpreter.debug("getResolvedMethod cache HIT: " + clas + " - " + method);
      }
    }

    return method;
  }

  public void clearCaches() {
    this.absoluteNonClasses = Collections.synchronizedSet(new HashSet());
    this.absoluteClassCache = new WeakHashMap();
    this.resolvedObjectMethods = new HashMap();
    this.resolvedStaticMethods = new HashMap();
  }

  public void setClassLoader(ClassLoader externalCL) {
    this.externalClassLoader = externalCL;
    this.classLoaderChanged();
  }

  public void addClassPath(URL path) throws IOException {
  }

  public void reset() {
    this.clearCaches();
  }

  public void setClassPath(URL[] cp) throws UtilEvalError {
    throw cmUnavailable();
  }

  public void reloadAllClasses() throws UtilEvalError {
    throw cmUnavailable();
  }

  public void reloadClasses(String[] classNames) throws UtilEvalError {
    throw cmUnavailable();
  }

  public void reloadPackage(String pack) throws UtilEvalError {
    throw cmUnavailable();
  }

  public void doSuperImport() throws UtilEvalError {
    throw cmUnavailable();
  }

  public boolean hasSuperImport() {
    return false;
  }

  public String getClassNameByUnqName(String name) throws UtilEvalError {
    throw cmUnavailable();
  }

  public void addListener(BshClassManager.Listener l) {
  }

  public void removeListener(BshClassManager.Listener l) {
  }

  public void dump(PrintWriter pw) {
    pw.println("BshClassManager: no class manager.");
  }

  public void definingClass(String className) {
    String baseName = Name.suffix(className, 1);
    int i = baseName.indexOf("$");
    if(i != -1) {
      baseName = baseName.substring(i + 1);
    }

    String cur = (String)this.definingClassesBaseNames.get(baseName);
    this.definingClasses.add(className);
    this.definingClassesBaseNames.put(baseName, className);
  }

  public boolean isClassBeingDefined(String className) {
    return this.definingClasses.contains(className);
  }

  public String getClassBeingDefined(String className) {
    String baseName = Name.suffix(className, 1);
    return (String)this.definingClassesBaseNames.get(baseName);
  }

  public void doneDefiningClass(String className) {
    String baseName = Name.suffix(className, 1);
    this.definingClasses.remove(className);
    this.definingClassesBaseNames.remove(baseName);
  }

  public Class defineClass(String name, byte[] code) {
    throw new InterpreterError("Can\'t create class (" + name + ") without class manager package.");
  }

  public void classLoaderChanged() {
  }

  public static UtilEvalError cmUnavailable() {
    return new Capabilities.Unavailable("ClassLoading features unavailable.");
  }

  public interface Listener {
    void classLoaderChanged();
  }

  static class SignatureKey {
    Class clas;
    Class[] types;
    String methodName;
    int hashCode = 0;

    SignatureKey(Class clas, String methodName, Class[] types) {
      this.clas = clas;
      this.methodName = methodName;
      this.types = types;
    }

    public int hashCode() {
      if(this.hashCode == 0) {
        this.hashCode = this.clas.hashCode() * this.methodName.hashCode();
        if(this.types == null) {
          return this.hashCode;
        }

        for(int i = 0; i < this.types.length; ++i) {
          int hc = this.types[i] == null?21:this.types[i].hashCode();
          this.hashCode = this.hashCode * (i + 1) + hc;
        }
      }

      return this.hashCode;
    }

    public boolean equals(Object o) {
      BshClassManager.SignatureKey target = (BshClassManager.SignatureKey)o;
      if(this.types == null) {
        return target.types == null;
      } else if(this.clas != target.clas) {
        return false;
      } else if(!this.methodName.equals(target.methodName)) {
        return false;
      } else if(this.types.length != target.types.length) {
        return false;
      } else {
        for(int i = 0; i < this.types.length; ++i) {
          if(this.types[i] == null) {
            if(target.types[i] != null) {
              return false;
            }
          } else if(!this.types[i].equals(target.types[i])) {
            return false;
          }
        }

        return true;
      }
    }
  }
}