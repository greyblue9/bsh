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
import org.d6r.annotation.Doc;


public class LoggingXClassLoader extends XClassLoader {
  
  public final Set<Object> badDexElems = new IdentityHashSet<>();
  public final ArrayDeque<String> q = new ArrayDeque<>();
  public static final Method DexFile_defineClassNative;
  public ClassLoader parentToUse;
  
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
  
  public static Boolean _isVerboseEnabled;
  
  public static boolean isVerboseEnabled() {
    if (_isVerboseEnabled == null) {
      try {
        _isVerboseEnabled = CommandParser.isVerboseEnabled();
      } catch (LinkageError e) {
        _isVerboseEnabled = Boolean.FALSE;
        e.printStackTrace();
      } catch (Throwable e) {
        _isVerboseEnabled = Boolean.TRUE;
        e.printStackTrace();
      } finally {
        if (_isVerboseEnabled == null) {
          _isVerboseEnabled = Boolean.TRUE;
        }
      }
    }
    return _isVerboseEnabled.booleanValue();
  }
  
  
  public static Class<LoggingXClassLoader> _getClass() {
    return LoggingXClassLoader.class;
  }
  
  
  public synchronized Class<?> findClass(String name)
    throws ClassNotFoundException
  {

    StringBuilder sb = new StringBuilder(q.size() * 2);
    
    for (int j=0; j<q.size(); ++j) sb.append("  ");
    
    q.offer(name);
    if (q.size() == 1) System.err.println();
    System.err.printf(
      "%s\u001b[1;30mfindClass: %s\u001b[0m\n", sb, name
    );
    
    
    System.err.flush();
    List<Throwable> suppressedExceptions = null;
    
    Object[] dexElements 
      = getfldval(getPathList(), "dexElements");
    
    boolean systemClass = false;
    if ( name.startsWith("java.") || name.startsWith("libcore.")
     ||  name.startsWith("android.") || name.startsWith("com.android.")
     ||  name.startsWith("dalvik"))
    {
      
      systemClass = true;
    }
    
    
    String source = "self";
    
    /*
    if (systemClass) {
      System.err.printf(
        "%s  \u001b[1;30m(system class)\u001b[0m\n", sb);
      
      Class<?> c = null;
      try {
        c = parentToUse.Reflector.<Class<?>>invokeOrDefault(
          getParent(), "findLoadedClass", new Object[]{ name }
        );
        if (c != null) {
          source = "getParent().findLoadedClass()";
          System.err.printf(
            "%s  \u001b[1;30m-> %s %s [via %s@%08x.%s]\u001b[0m\n",
            sb,
            java.lang.reflect.Modifier.toString(c.getModifiers()),
            c.getName(),
            getParent().getClass().getSimpleName(),
            System.identityHashCode(getParent()),
            source
          );
          System.err.flush();
          if (!q.isEmpty()) q.poll();
          return c;
        }
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        suppressedExceptions.add(e);
        systemClass = false;
      } catch (Throwable e) {
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        suppressedExceptions.add(e);
      }
      
      
      
      try {
        if (parentToUse == null
        && (parentToUse = getParent()) == null)
        {
          Class<?> cls_bootClassLoader = super.findLoadedClass(
            "java.lang.BootClassLoader"
          );
          Method bootClassLoader_getInstance = cls_bootClassLoader
            .getDeclaredMethod("getInstance", new Class[0]);
          bootClassLoader_getInstance.setAccessible(true);          
          parentToUse = (ClassLoader) 
            bootClassLoader_getInstance.invoke(null);
          System.err.println(parentToUse);
        }
      } catch (Throwable e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      
      try {
        c = Reflector.<Class<?>>invokeOrDefault(
          parentToUse, "findClass", new Object[]{ name }
        );
        if (c != null) {
          source = "getParent().findClass()";
          System.err.printf(
            "%s  \u001b[1;30m-> findClass returning -> "
            + "%s %s [via %s@%08x.%s]\u001b[0m\n",
            sb,
            java.lang.reflect.Modifier.toString(c.getModifiers()),
            c.getName(),
            getParent().getClass().getSimpleName(),
            System.identityHashCode(getParent()),
            source
          );
          System.err.flush();
          if (!q.isEmpty()) q.poll();
          return c;
        }
      } catch (Throwable e) {
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        suppressedExceptions.add(e);
      }
      
    }
    
    */
    
    try {
        Class<?> c = super.findLoadedClass(name);
        if (c != null) {
          source = "super.findLoadedClass()";
          System.err.printf(
            "%s  \u001b[1;30m-> findClass returning -> "
            + "%s %s [via %s@%08x.%s]\u001b[0m\n",
            sb,
            java.lang.reflect.Modifier.toString(c.getModifiers()),
            c.getName(),
            getClass().getSuperclass().getSimpleName(),
            System.identityHashCode(this),
            source
          );
          System.err.flush();
          if (!q.isEmpty()) q.poll();
          return c;
        }
    } catch (Throwable e) {
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        suppressedExceptions.add(e);
    }
    
    
    
    for (int i=0; i<dexElements.length; ++i) {
      if (dexElements[i] == null) continue;
      if (badDexElems.contains(dexElements[i])) continue;
      DexFile dexFile = getfldval(dexElements[i], "dexFile");
      if (dexFile == null) {
        System.err.printf("[WARN] Invalid dex: %s\n", dexElements[i]);
        badDexElems.add(dexElements[i]);
        continue;
      }
      Integer mCookie = Reflect.<Integer>getfldval(dexFile, "mCookie");
      if (mCookie == null) continue;      
      try {
        Class<?> c = (Class<?>) DexFile_defineClassNative.invoke(
          null, name, this, mCookie
        );
        if (c != null) {
          System.err.printf(
            "%s  \u001b[1;30m-> findClass returning -> "
            + "%s %s [%s @%08x]\u001b[0m\n",
            sb,
            java.lang.reflect.Modifier.toString(c.getModifiers()),
            c.getName(),
            dexElements[i],
            System.identityHashCode(dexElements[i])
          );
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
          continue;
        }

        LinkageError le = new LinkageError(String.format(
          "findClass: Loading class '%s' from %s threw %s:\n  %s",
          name, dexElements[i], e,
          CollectionUtil.toString(
            Reflector.getRootCause(e).getStackTrace()
          )
        ), e);
        if (suppressedExceptions == null) {
          suppressedExceptions = new ArrayList<Throwable>();
        }
        le.printStackTrace();
        suppressedExceptions.add(le);
        
        le.printStackTrace();
      }
    }
    

    final ClassNotFoundException cnfe = new ClassNotFoundException(
      String.format(
        "Didn't find class \"%s\" on path: %s@%08x [%d elements]; "
        + "ClassLoader: %s@%08x; Parent: %s%08x", 
        name, 
        getPathList().getClass(),
        System.identityHashCode(getPathList()),
        dexElements.length,
        getClass().getName(),
        System.identityHashCode(this),
        getParent().getClass().getName(),
        System.identityHashCode(getParent())
      )
    );
    if (suppressedExceptions != null) {
      for (Throwable t : suppressedExceptions) {
        cnfe.addSuppressed(t);
      }
    }

    
    System.err.println(q);
    final StackTraceElement[] stes = cnfe.getStackTrace();
    final StackTraceElement ste
      = ("bsh.Capabilities".equals(stes[5].getClassName()))
          ? stes[6]
          : stes[5];
    
    boolean show;    
    if (ste.getClassName().startsWith("bsh.")) {
      show = isVerboseEnabled();
    } else {
      show = true;
    }
    
    System.err.printf(
      "\u001b[0;31m  findClass '%s' throwing CFNE: "
      + "\u001b[1;31m%s\u001b[0m(caller: %s on thread %s)\n",
      name, name, ste, Thread.currentThread()
    );
    if (!q.isEmpty()) q.poll();
    if (show) cnfe.printStackTrace(System.err);
    
    throw cnfe;
  }
  
  public URL findResource(String name) {
    System.err.printf("findResource: %s\n", new Object[] { name });
    return getPathList().findResource(name);
  }
  
  public Enumeration<URL> findResources(String name) {
    System.err.printf("findResources: %s\n", new Object[] { name });
    return (Enumeration<URL>) getPathList().findResources(name);
  }
  
  public String findLibrary(String name) {
    System.err.printf("findLibrary: %s\n", new Object[] { name });
    return getPathList().findLibrary(name);
  }
  
  public synchronized Package getPackage(String name) {
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
    StringBuilder result = new StringBuilder();
    File[] nativeLibraryDirectories;
    for (int length = (nativeLibraryDirectories = getPathList().getNativeLibraryDirectories()).length, i = 0; i < length; ++i) {
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
      "%s@%08x (parent: %s@%08x)",
      this.getClass().getName(),
      System.identityHashCode(this),
      this.getParent().getClass().getName(),
      System.identityHashCode(this.getParent())
    );
  }
  
  
  public static void main(String... argv) { 
    if (argv.length < 1 
    ||  argv[0].equals("-h") 
    ||  argv[0].equals("--help") 
    ||  argv[0].equals("--usage") 
    ||  argv[0].equals("/?"))
    {
      System.err.printf(
        "Usage: dalvikvm ... %s MainClass [MainArgs...]\n",
        LoggingXClassLoader.class.getName()
      );
      System.exit(1);
    }
    
    String className = argv[0];
    String[] args = new String[argv.length - 1];
    System.arraycopy(argv, 1, args, 0, args.length);
    
    ClassLoader self = null;
    /*System.err.printf(
      "Creating %s ...\n",
      LoggingXClassLoader.class.getName()
    );*/
    try {
      self = LoggingXClassLoader.class.getDeclaredConstructor()
        .newInstance();
    } catch (Throwable e) { 
      if (isVerboseEnabled()) e.printStackTrace(); 
      System.exit(2);
    }
    
    /*System.err.printf(
      "Setting context class loader to:\n  %s\n",
      self.toString()
    );*/
    Thread.currentThread().setContextClassLoader(self);
    
    BaseDexClassLoader unwantedCl 
      = (BaseDexClassLoader)
          LoggingXClassLoader.class.getClassLoader();
    
    if (!(unwantedCl instanceof LoggingXClassLoader)) {
      /*System.err.println(
        "Nulling-out unwanted class loader ...");*/
      try {
        Constructor<DexPathList> dplCtor 
        = DexPathList.class
          .getDeclaredConstructor(
            ClassLoader.class, 
            String.class, 
            String.class, 
            File.class
          ); 
        dplCtor.setAccessible(true); 
        DexPathList emptyDpl 
          = (DexPathList) dplCtor.newInstance(
          self, "", "", null
        ); 
        setfldval(unwantedCl, "pathList", emptyDpl);
        setfldval(unwantedCl, "originalPath", "");
        setfldval(self, "parent", unwantedCl.getParent());
        setfldval(unwantedCl, "parent", self);
        
        setfldval(
          unwantedCl, "originalLibraryPath", ""
        );
        Class<?> sysLoaderInnerCls = Class.forName(
          "java.lang.ClassLoader$SystemClassLoader"
        );
        Field fld_ldr = sysLoaderInnerCls.getDeclaredField(
          "loader"
        );
        fld_ldr.setAccessible(true);
        fld_ldr.set(null, self);
      } catch (Throwable e) {
        if (isVerboseEnabled()) e.printStackTrace(); 
      }
    }
    
    Class<?> mainCls = null;
    try {
      mainCls = self.loadClass(className);
    } catch (Throwable e) {
      RuntimeException ex = new RuntimeException(
        "Failed to find main method in class: "
          + className,
         e
      );
      ex.printStackTrace();
      System.exit(3);
    }
    String methodName = "main";
    Class<?>[] paramTypes 
      = new Class<?>[]{ String[].class };
    Method main = null;
    try {
      main = mainCls
        .getDeclaredMethod(methodName, paramTypes);
      if (!main.isAccessible()) main.setAccessible(true);
    } catch (Throwable e) {
      RuntimeException ex = new RuntimeException(
        "Failed to find main method in class: "
          + className,
         e
      );
      ex.printStackTrace(); 
      System.exit(4);
    }
    try {
      main.invoke(null, new Object[]{ args });
    } catch (Throwable e) { 
      if (isVerboseEnabled()
      ) e.printStackTrace(); 
      System.exit(5);
    }
  }
  
  public static boolean tryLoadSoLib(String path, StringBuilder sb) {
    Throwable e = null;
    boolean isFile = path.indexOf(".so") != -1 && path.startsWith("/");
    try {
      if (isFile) {
        System.load(path);
        Runtime.getRuntime().load(path);
      } else {
        System.loadLibrary(path);
        Runtime.getRuntime().loadLibrary(path);
      }
      return true;
    } catch (Throwable _e) {
      e = (_e instanceof InvocationTargetException)
        ? ((InvocationTargetException) _e).getTargetException()
        : _e;
    }
    if (e == null) {
      sb.append(String.format(
        "%s: Unknown load failure", path
      ));
      return false;
    }
    final String msg = getfldval(e, "detailMessage");    
    if (e instanceof LinkageError) {
      LinkageError ule = (LinkageError) e;
      try {
        String str = msg.replaceAll(
              "[:;] (caused by )?", "\n  because ")
            .replace(
              "because could not load library", 
              "while attempting to load"
            );

        String rclib = str.substring(
          str.lastIndexOf("to load ") + 9, 
          str.indexOf("\"", str.lastIndexOf("to load") + 9)
        );
        String[] lines = str.split("\n");
        String[] rev = new String[lines != null? lines.length: 0];
        for (int i=0; i<lines.length; i++) {
          rev[lines.length - (i+1)] = lines[i];
        }        
        String fstr = java.util.regex.Pattern.compile(
          "^  because (.*)\n(dlopen failed)\n?$",
          java.util.regex.Pattern.CASE_INSENSITIVE | 
          java.util.regex.Pattern.DOTALL | 
          java.util.regex.Pattern.MULTILINE | 
          java.util.regex.Pattern.UNIX_LINES
        ).matcher(
          java.util.Arrays.toString(lines)
            .replaceAll("^\\[(.*)\\]$", "$1").replaceAll(", ", "\n")
        ).replaceAll("$2 for \"" + rclib + "\": $1")
         .replaceAll(
          "needed by (\"[^\"]*\")", 
          "\n    (needed by $1)\n"
        ).replace(
          String.format(
            "\"%s\"", 
              new java.io.File(path).getName()),
          String.format(
            "\"%s\"\n      @ '%s'", 
              new java.io.File(path).getName(), path)
        ).replaceFirst(": ", "\n\nCaused by ")
         .replaceAll("\n", "\n  ")
         .replaceFirst("( *while)", "\n$1");
       
        sb.append(fstr);
      } catch (Throwable strerr) {
        sb.append(msg);
        System.err.println(strerr);
      }
    } else {
      sb.append(msg != null? msg: e.toString());
    }
    return false; 
  }
  
  
  private static Class<?> getTargetClass(Object objectOrClass) {
    if (objectOrClass instanceof Class<?>) {
      return (objectOrClass == Class.class)
        ? Class.class
        : (Class<?>) objectOrClass;
    } else return objectOrClass.getClass();
  }
  
  private static Object getTarget(Object objectOrClass) {
    if (objectOrClass instanceof Class<?>) {
      return (objectOrClass == Class.class)
        ? (Object) Class.class
        : null;
    } else return objectOrClass;
  }
  
  
  protected static <T> T getfldval(Object objectOrClass, String name) {
    Class<?> cls = getTargetClass(objectOrClass);
    Object obj = getTarget(objectOrClass);
    try {
      final Field fld = getfld(objectOrClass, name);
      if (fld == null) return null;
      return (T) (Object) fld.get(obj);
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  protected static boolean setfldval(Object objectOrClass, String name,
  Object newVal)
  {
    Class<?> cls = getTargetClass(objectOrClass);
    Object obj = getTarget(objectOrClass);
    try {
      final Field fld = getfld(objectOrClass, name);
      if (fld == null) return false;
      fld.set(obj, newVal);
      return true;
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    } 
  }
  
  protected static Field getfld(Object objectOrClass, String name) {
    Class<?> cls = getTargetClass(objectOrClass);
    Object obj = getTarget(objectOrClass);
    Throwable lastError = null;
    boolean staticBypass = false;
    do {
      for (Class<?> c = cls; c != null && c != Object.class; 
                    c = c.getSuperclass())
      {
        @Doc("blah") Field fld = null;
        try {
          fld = c.getDeclaredField(name);
          if (! staticBypass) {
            int acc = fld.getModifiers();
            if (obj == null && (acc & Modifier.STATIC) != 0) continue;
          }
          fld.setAccessible(true);
          return fld;
        } catch (NoSuchFieldException nsfe) {
          lastError = nsfe;
        } catch (Throwable iae) {
          if (isVerboseEnabled()) iae.printStackTrace();
          System.err.printf(
            "%s getting '%s' from '%s' instance "
            + "via field: %s: %s\n",
            iae.getClass().getSimpleName(),
            name, cls.getName(), fld, iae
          );
        }
      }
    } while (!staticBypass && (staticBypass = true));
    
    if (lastError != null) lastError.printStackTrace();
    return null;
  }
  
  
  /*
  static <T> T invoke(Object target, String name, Object... args) {
    Class<?> cls = getTargetClass(objectOrClass);
    Object obj = getTarget(objectOrClass);
    for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
      
      try {
        Field fld = c.getDeclaredField(name);
        int acc = fld.getModifiers();
        if (obj == null && (acc & Modifier.STATIC) != 0) continue;
        fld.setAccessible(true);
        return (T) (Object) fld.get(obj);
      } catch (NoSuchFieldException nsfe) {
        lastError = nsfe;
      } catch (IllegalAccessException iae) {
        if (DEBUG) iae.printStackTrace();
        System.err.printf(
          "IllegalAccessException getting '%s' from '%s' instance "
          + "via field: %s: %s\n",
          name, cls.getName(), fld, iae
        );
      }
    }
    return (T) (Object) defaultValue;
  }
  
  */
}