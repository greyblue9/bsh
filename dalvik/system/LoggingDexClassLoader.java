package dalvik.system;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.d6r.Reflect;
import org.d6r.IdentityHashSet;
import org.d6r.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.util.Enumeration;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import dalvik.system.DexFile;
import dalvik.system.*;


public class LoggingDexClassLoader extends BaseDexClassLoader {
  
  public DexPathList pathList;
  
  public final Set<Object> badDexElems = new IdentityHashSet<>();
  public final ArrayDeque<String> q = new ArrayDeque<>();
  public static final Method DexFile_defineClassNative;
  
  static {
    Method md = null;
    try {
      (md = DexFile.class.getDeclaredMethod(
        "defineClassNative", String.class, BaseDexClassLoader.class,
        Integer.TYPE
      )).setAccessible(true);
      System.err.println("1: " + md.getName());
    } catch (NoSuchMethodException nsme) {
      try {
        (md = DexFile.class.getDeclaredMethod(
          "defineClassNative", String.class, ClassLoader.class,
          Integer.TYPE
        )).setAccessible(true);
        System.err.println("2: " + md.getName());
      } catch (Throwable e2) {
        e2.printStackTrace();
      }
    } catch (Throwable e1) {
      e1.printStackTrace();
    }
    DexFile_defineClassNative = md;   
  }
  
  public LoggingDexClassLoader(final String path, final File optDir,
  final String libPath, final ClassLoader parent)
  {
    super(path, optDir, libPath, parent);
    
    try {
      Field plf 
        = BaseDexClassLoader.class.getDeclaredField("pathList");
      System.err.printf("plf: %s\n", plf);
      plf.setAccessible(true);
      DexPathList pl = (DexPathList) plf.get(this);
      ((LoggingDexClassLoader) this).pathList = pl;
      
      //Reflect.setfldval(pathList, "definingContext", this);
      
    } catch (Throwable e) {
      e.printStackTrace();
      pathList
        = new DexPathList((ClassLoader) this, path, libPath, optDir);
    }
  }
  
  public LoggingDexClassLoader(final String dexPath,
  final File optDir, final String libPath)
  {
    this(
      dexPath, optDir, libPath,
      ClassLoader.getSystemClassLoader()
    );
  }
  
  public LoggingDexClassLoader(final String dexPath,
  final String libPath, final File optDir)
  {
    this(
      dexPath, optDir, libPath,
      ClassLoader.getSystemClassLoader()
    );
  }
  
  public LoggingDexClassLoader(final String dexPath,
  final String libPath) 
  {
    this(dexPath, libPath, new File("/data/local/tmp_clazzes"));
    if (! new File("/data/local/tmp_clazzes").exists()) {
      new File("/data/local/tmp_clazzes").mkdirs();
    }
  }
  
  public LoggingDexClassLoader(final String dexPath) {
    this(dexPath, System.getProperty("java.library.path"));
  }
  
  public LoggingDexClassLoader() {
    this(System.getProperty("java.class.path"));
  }
  
  public static Class<LoggingDexClassLoader> _getClass() {
    return LoggingDexClassLoader.class;
  }
  
  
  public synchronized Class<?> findClass(final String name)
    throws ClassNotFoundException
  {
    q.offer(name);
    StringBuilder sb = new StringBuilder(q.size() * 2);
    
    for (int j=0; j<q.size(); ++j) sb.append("  ");
    
    if (q.size() == 1) System.err.println();
    System.err.printf(
      "%sfindClass: %s\n", sb, name
    );
    
    
    System.err.flush();
    List<Throwable> suppressedExceptions = null;
    
    Object[] dexElements = Reflect.getfldval(
      this.pathList, "dexElements"
    );
    
    
    for (int i=0; i<dexElements.length; ++i) {
      if (dexElements[i] == null) continue;
      if (badDexElems.contains(dexElements[i])) continue;
      DexFile dexFile = Reflect.getfldval(dexElements[i], "dexFile");
      Integer mCookie = Reflect.<Integer>getfldval(dexFile, "mCookie");
      if (mCookie == null) continue;      
      try {
        Class<?> c = (Class<?>) DexFile_defineClassNative.invoke(
          null, name, this, mCookie
        );
        if (c != null) {
          System.err.printf(
            "  findClass returning -> %s %s [%s @%08x]\n",
            java.lang.reflect.Modifier.toString(c.getModifiers()),
            c.getName(),
            dexElements[i],
            System.identityHashCode(dexElements[i])
          );
          System.err.flush();
          System.err.flush();
          if (!q.isEmpty()) q.poll();
          return c;
        }
      } catch (Throwable e) {
        while (e instanceof InvocationTargetException
            || e instanceof UndeclaredThrowableException)
        {
          if (e instanceof InvocationTargetException) e = 
            ((InvocationTargetException)e).getTargetException();
          if (e instanceof UndeclaredThrowableException) e = 
            ((UndeclaredThrowableException)e).getUndeclaredThrowable();
        }
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        suppressedExceptions.add(e);
        
        if (e instanceof IOException) { 
          badDexElems.add(dexElements[i]);
          System.err.printf(
            "  >> Adding %s to bad dexElements set due to %s\n",
            dexElements[i],
            e.getClass().getSimpleName()
          );
          System.err.flush();
          continue;
        }

        new LinkageError(String.format(
          "findClass: Loading class '%s' from %s threw %s:\n  %s",
          name, dexElements[i], e,
          StringUtils.join(
            ExceptionUtils.getRootCauseStackTrace(e), "\n  "
          )
        ), e).printStackTrace();
        System.err.flush();
      }
    }
    
    final ClassNotFoundException cnfe = new ClassNotFoundException(
      String.format(
        "Didn't find class \"%s\" on path: %s@%08x [%d elements]; "
        + "ClassLoader: %s@%08x; Parent: %s%08x", 
        name, 
        this.pathList.getClass(),
        System.identityHashCode(this.pathList),
        dexElements.length,
        getClass().getName(),
        System.identityHashCode(this),
        getParent().getClass().getName(),
        System.identityHashCode(getParent())
      )
    );
    if (suppressedExceptions != null) {
      for (final Throwable t : suppressedExceptions) {
        cnfe.addSuppressed(t);
      }
    }
    
    System.err.println(q);
    
    System.err.printf(
      "\u001b[0;31m  findClass '%s' throwing CFNE: "
      + "\u001b[1;31m%s\u001b[0m(caller: %s on thread %s)\n",
      name, name, new Error().getStackTrace()[2],
      Thread.currentThread()
    );
    if (!q.isEmpty()) q.poll();
    System.err.flush();
    // 
    throw cnfe;
  }
  
  public URL findResource(final String name) {
    System.err.printf("findResource: %s\n", new Object[] { name });
    return this.pathList.findResource(name);
  }
  
  public Enumeration<URL> findResources(final String name) {
    System.err.printf("findResources: %s\n", new Object[] { name });
    return (Enumeration<URL>) this.pathList.findResources(name);
  }
  
  public String findLibrary(final String name) {
    System.err.printf("findLibrary: %s\n", new Object[] { name });
    return this.pathList.findLibrary(name);
  }
  
  public synchronized Package getPackage(final String name) {
    System.err.printf("getPackage: %s\n", new Object[] { name });
    Package packageR = null;
    synchronized (this) {
      if (name != null && !name.isEmpty()) {
        packageR = super.getPackage(name);
        if (packageR == null) {
          packageR = this.definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", (URL)null);
        }
      }
    }
    return packageR;
  }
  
  public String getLdLibraryPath() {
    System.err.printf("getLdLibraryPath\n", new Object[0]);
    final StringBuilder result = new StringBuilder();
    File[] nativeLibraryDirectories;
    for (int length = (nativeLibraryDirectories = this.pathList.getNativeLibraryDirectories()).length, i = 0; i < length; ++i) {
      final Object directory = nativeLibraryDirectories[i];
      if (result.length() > 0) {
        result.append(':');
      }
      result.append(directory);
    }
    return result.toString();
  }
  
  public String toString() {
    return String.format(
      "%s@%08x [%d elements] (parent: %s@%08x)",
      this.getClass().getName(),
      System.identityHashCode(this),
      Reflect.<Object[]>getfldval(this.pathList, "dexElements").length,
      this.getParent().getClass().getName(),
      System.identityHashCode(this.getParent())
    );
  }
  
  public static void main(final String... argv) {
    if (argv.length < 1 || argv[0].equals("-h") || argv[0].equals("--help") || argv[0].equals("--usage") || argv[0].equals("/?")) {
      System.err.printf("Usage: dalvikvm ... %s MainClass [MainArgs...]\n", new Object[] { _getClass().getName() });
      System.exit(1);
    }
    final String className = argv[0];
    final String[] args = new String[argv.length - 1];
    System.arraycopy((Object)argv, 1, (Object)args, 0, args.length);
    ClassLoader self = null;
    System.err.printf("Creating %s ...\n", new Object[] { _getClass().getName() });
    
    try {
      self = (ClassLoader)_getClass().getDeclaredConstructor(
        new Class[0]
      ).newInstance(new Object[0]);
      System.err.println("created 1");
      
    } catch (Throwable e) {
      System.err.println(e.getClass().getName());
      System.err.println(e.getCause());
      System.err.println(
        StringUtils.join(
          ExceptionUtils.getRootCauseStackTrace(e), "\n"
        )
      );
      return;
    }
    
    System.err.printf("Created %s ...\n", new Object[] { self });
    
    try {
      ClassLoader sysLoader = null;
      try {
        sysLoader = Reflect.getfldval(
          Class.forName("java.lang.ClassLoader$SystemClassLoader",
            false, ClassLoader.getSystemClassLoader().getParent()),
          "loader"
        );
        System.err.printf("system loader: %s\n", sysLoader);
        
        Reflect.setfldval(
            sysLoader,
            "loader",
            self
        );
        System.err.println("system loader set");
      } catch (Throwable e) { 
        e.printStackTrace();
      }
      /*
      if (Thread.currentThread().getContextClassLoader() != self) {
        Reflect.setfldval(        
          Thread.currentThread().getContextClassLoader(),
          "parent", self
        );
        System.err.println("context loader.parent set");
      
        Reflect.setfldval(        
          Thread.currentThread().getContextClassLoader(),
          "pathList", 
          Reflect.getfldval(self, "pathList")
        );
        System.err.println("context loader.pathList set");
        
        Thread.currentThread().setContextClassLoader(self);
        
        System.err.println("context loader set");
      }*/
    } catch (Throwable e) { 
      e.printStackTrace();
    }

    
    System.err.printf("Setting context class loader to:\n  %s\n", new Object[] { self.toString() });
    
    
    Thread.currentThread().setContextClassLoader(self);
    final BaseDexClassLoader unwantedCl
      = (BaseDexClassLoader)_getClass().getClassLoader();
    if (!(unwantedCl instanceof LoggingDexClassLoader)) {
      System.err.println("Nulling-out unwanted class loader ...");
      try {
        final Constructor<DexPathList> dplCtor = (Constructor<DexPathList>)DexPathList.class.getDeclaredConstructor(new Class[] { ClassLoader.class, String.class, String.class, File.class });
        dplCtor.setAccessible(true);
        final DexPathList emptyDpl = (DexPathList)dplCtor.newInstance(new Object[] { self, "", "", null });
        Reflect.setfldval((Object)unwantedCl, "pathList", (Object)emptyDpl);
        Reflect.setfldval((Object)unwantedCl, "originalPath", "");
        Reflect.setfldval((Object)unwantedCl, "originalLibraryPath", "");
      }
      catch (Throwable e2) {
        e2.printStackTrace();
      }
    }
    
    Class<?> mainCls = null;
    try {
      mainCls = (Class<?>) self.loadClass(className);
    } catch (Throwable e3) {
      new RuntimeException(new StringBuilder("Failed to find main class: ").append(className).toString(), e3).printStackTrace();
      System.exit(3);
    }
    final String methodName = "main";
    final Class[] paramTypes = { String[].class };
    
    System.err.printf(
      "Searching for method: %s.%s(%s) ...\n", new Object[] {
         mainCls.getName(),
         methodName, 
         Arrays.toString((Object[])paramTypes)
           .replaceAll("^\\[(.*)\\]$", "")
      }
    );
    Method main = null;
    try {
      main = mainCls.getDeclaredMethod(methodName, paramTypes);
      if (!main.isAccessible()) {
        main.setAccessible(true);
      }
    } catch (Throwable e4) {
      new RuntimeException(new StringBuilder("Failed to find main method in class: ").append(className).toString(), e4).printStackTrace();
      System.exit(4);
    }
    try {
      main.invoke((Object)null, new Object[] { args });
    } catch (Throwable e4) {
      try {
        System.err.printf(
          "=== Class resolve queue: ===\n    %s\n",
          (Object) 
          Arrays.toString(
            Reflect.<ArrayDeque>getfldval(self, "q").toArray()
          )
        );
      } catch (Throwable e2) { e2.printStackTrace(); } 
      
      try {
        org.d6r.Dumper.dump(e4, 6, 20);
      } catch (Throwable e5) { }
      try {
        org.d6r.Dumper.dump(main, 4, 10);
      } catch (Throwable e5) { }
      
      e4.printStackTrace(System.out);
      e4.printStackTrace(System.err);
      
      try {
        System.err.println(
          dumpMembersExp.dumpMembersExp(
            null, main.getDeclaringClass(), true
          )
        );
      } catch (Throwable e6) {  }
      try {
        System.err.println(
          dumpMembers.dumpMembers(
            null, main.getDeclaringClass(), true
          )
        );
      } catch (Throwable e7) {  }
      System.exit(5);
    }
  }
}

