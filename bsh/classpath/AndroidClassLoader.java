package bsh.classpath;
import org.d6r.ClassInfo;
import android.os.Process;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.command.dexer.Main.Arguments;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.util.ByteArray;
import dalvik.system.XClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.PosixUtil;
import org.d6r.Reflect;
import org.d6r.Reflector;
import org.d6r.CollectionUtil;
import org.d6r.PosixFileInputStream;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.Stack;

public class AndroidClassLoader extends XClassLoader {
  public static boolean JRE;
  private static boolean triedRemount;
  static Class<?> AppGlobals;
  static Method getInitialApplication;
  static Object app;
  static Object apk;
  static String dataDir = "/data/local/tmp_clazzes";
  public static final String TAG = "ACL";
  static int TARGET_API_LEVEL;
  static Constructor<DexFile> dxDexFileCtor;
  static Class<?>[] dxDexFileCtorParamTypes;
  public static Method findResource;
  public static Field parentFld;
  public List<URL> urls;
  public Map<String, Class<?>> clsMap;
  public static boolean DEBUG;
  public static boolean WRITE_CLASSFILE;
  public static boolean argsHasDexOpts;
  public static Method loadDex;
  static byte[] dexBytes;
  static Arguments args;
  static CfOptions cfopts;
  static DexOptions dexopts;
  static DexFile outputDex;
  static Method translate0;
  static Class<?> APC_CLASS;
  static Constructor<?> APC_CTOR;
  static Field APC_ALLOWED;
  
  static Object thePermissionCollection;
  static {
    if (Boolean.FALSE.equals((JRE = CollectionUtil.isJRE()))) {
      try {
        AppGlobals = Class.forName(
          "android.app.AppGlobals",
          false, ClassLoader.getSystemClassLoader()
        );
        getInitialApplication = (AppGlobals != null)
          ? AppGlobals.getDeclaredMethod("getInitialApplication")
          : null;
        app = (getInitialApplication != null)
          ? getInitialApplication.invoke(null)
          : null;
        Field apkField = (app != null)
          ? app.getClass().getDeclaredField("mLoadedApk")
          : null;
        if (apkField != null) apkField.setAccessible(true);
        apk = (apkField != null)
          ? apkField.get(app)
          : null;
        dataDir = (apk != null)
          ? (String) apk.getClass()
              .getDeclaredMethod("getDataDir").invoke(apk)
          : dataDir;
      } catch (final Throwable t) {
        t.printStackTrace();
      }
      

      
      dataDir = apk != null?(String)Reflector.invoke(apk, "getDataDir", new Object[0]):"/data/local/tmp_clazzes";
    } else {
      dataDir = "/data/local/tmp_clazzes";
      try {
        System.setSecurityManager(null);
        APC_CLASS = Class.forName(
          "java.security.AllPermissionCollection", false,
          ClassLoader.getSystemClassLoader()
        );
        (APC_CTOR = APC_CLASS.getDeclaredConstructor()).setAccessible(true);
        thePermissionCollection = APC_CTOR.newInstance();
        (APC_ALLOWED = APC_CLASS.getDeclaredField("all_allowed"))
          .setAccessible(true);
        APC_ALLOWED.setBoolean(thePermissionCollection, true);
      } catch (Throwable e) {
        throw new Error(e);
      }
    }
    
    TARGET_API_LEVEL = 18;
    DEBUG = "true".equals(System.getProperty("debug"));
    WRITE_CLASSFILE = true;

    try {
      (loadDex = dalvik.system.DexFile.class.getDeclaredMethod("loadDex", new Class<?>[]{String.class, String.class, Integer.TYPE})).setAccessible(true);
      (parentFld = ClassLoader.class.getDeclaredField("parent")).setAccessible(true);
      (findResource = ClassLoader.class.getDeclaredMethod("findResource", new Class<?>[]{String.class})).setAccessible(true);
    } catch (ReflectiveOperationException var7) {
      var7.printStackTrace();
    }

    Field[] var3;
    int var2 = (var3 = Arguments.class.getDeclaredFields()).length;

    for(int ee2 = 0; ee2 < var2; ++ee2) {
      Field ee = var3[ee2];
      if (ee.getName().equals("dexOptions")) {
        argsHasDexOpts = true;
        break;
      }
    }

    try {
      dxDexFileCtor = (Constructor<DexFile>) (Object) 
        DexFile.class.getDeclaredConstructors()[0];
      dxDexFileCtor.setAccessible(true);
      dxDexFileCtorParamTypes = dxDexFileCtor.getParameterTypes();
    } catch (Throwable var6) {
      var6.printStackTrace();
      dxDexFileCtor = null;
      dxDexFileCtorParamTypes = null;
    }

    cfopts = new CfOptions();
    dexopts = new DexOptions();

    try {
      cfopts.localInfo = true;
      cfopts.optimize = true;
      dexopts.targetApiLevel = TARGET_API_LEVEL;
      (translate0 = CfTranslator.class.getDeclaredMethod("translate0", new Class<?>[]{String.class, byte[].class, CfOptions.class, DexOptions.class})).setAccessible(true);
    } catch (ReflectiveOperationException var5) {
      try {
        (translate0 = CfTranslator.class.getDeclaredMethod("translate0", new Class<?>[]{DirectClassFile.class, byte[].class, CfOptions.class, DexOptions.class, DexFile.class})).setAccessible(true);
      } catch (ReflectiveOperationException var4) {
        var4.printStackTrace();
      }
    }

  }

  public static File getOptDir() {
    String dir = (dataDir != null)
      ? dataDir
      : "/data/local/tmp_clazzes/";
    File file = new File(dir);
    file.mkdirs();
    return file;
  }

  public static String getDexPath() {
    return dataDir;
  }

  public static String getLibPath() {
    return System.getProperty("java.library.path");
  }

  public static ClassLoader getDefaultParent() {
    return Thread.currentThread().getContextClassLoader();
  }

  public AndroidClassLoader(String dexPath, File optDir, String libPath, ClassLoader parent, URL[] urls) {
    super(dexPath, optDir, libPath, parent);
    this.clsMap = new WeakHashMap();
    this.urls = new ArrayList(Arrays.asList(urls != null?urls:new URL[0]));
    if (!optDir.exists()) {
      optDir.mkdirs();
    }

  }

  public AndroidClassLoader(String dexPath, File optDir, String libPath, ClassLoader parent) {
    this(dexPath, optDir, libPath, parent, (URL[])null);
  }

  public AndroidClassLoader(File optDir, String libPath, ClassLoader parent, URL[] urls) {
    this(getDexPath(), optDir, libPath, parent, urls);
  }

  public AndroidClassLoader(File optDir, String libPath, ClassLoader parent) {
    this((File)optDir, (String)libPath, (ClassLoader)parent, (URL[])null);
  }

  public AndroidClassLoader(String libPath, ClassLoader parent, URL[] urls) {
    this(getOptDir(), libPath, parent, urls);
  }

  public AndroidClassLoader(String libPath, ClassLoader parent) {
    this((String)libPath, (ClassLoader)parent, (URL[])null);
  }

  public AndroidClassLoader(ClassLoader parent, URL[] urls) {
    this(getLibPath(), parent, urls);
  }

  public AndroidClassLoader(URL[] urls, ClassLoader parent) {
    this(parent, urls);
  }

  public AndroidClassLoader(ClassLoader parent) {
    this((ClassLoader)parent, (URL[])null);
  }

  public AndroidClassLoader(URL[] urls) {
    this(getDefaultParent(), urls);
  }

  public AndroidClassLoader() {
    this((URL[])null);
  }

  public void addURL(URL url) {
    if (!this.urls.contains(url)) {
      this.urls.add(url);
    }
  }

  public URL[] getURLs() {
    return (URL[])Arrays.copyOf((URL[])this.urls.toArray(new URL[0]), this.urls.size());
  }

  public static boolean setfldval(Object o, String field_name, Object newval) {
    Class<?> clazz = null;
    if (o instanceof String) {
      try {
        clazz = Class.forName((String)o);
      } catch (Throwable var13) {
        throw new RuntimeException(var13);
      }
    } else if (o instanceof Class<?>) {
      clazz = (Class<?>)o;
    } else {
      clazz = o.getClass();
    }

    if (clazz == null) {
      return false;
    } else {
      Field field = null;

      try {
        field = clazz.getDeclaredField(field_name);
        field.setAccessible(true);
      } catch (Throwable var12) {
        Throwable modifiers = var12;
        NoSuchFieldException is_static_field = new NoSuchFieldException(String.format("AndroidClassLoader.setfldval(%s, %s, %s): No such field: %s.%s\n", new Object[]{String.valueOf(o), field_name, newval, clazz.getName(), field_name}));

        try {
          is_static_field.initCause(modifiers);
        } catch (Throwable var10) {
          ;
        }

        is_static_field.printStackTrace();
        return false;
      }

      int modifiers1 = field.getModifiers();
      boolean is_static_field1 = (modifiers1 & 8) != 0;
      Object refObj = o;
      if (is_static_field1) {
        refObj = null;
      }

      Class<?> value = Void.class;

      Object value1;
      try {
        value1 = field.get(refObj);
        field.set(refObj, newval);
      } catch (Throwable var11) {
        throw new RuntimeException(var11);
      }

      return value1 == newval;
    }
  }

  public static DexFile createDxDexFile() {
    Object[] args = new Object[dxDexFileCtorParamTypes.length];
    if (dxDexFileCtorParamTypes.length != 0 && dxDexFileCtorParamTypes[0] == DexOptions.class) {
      DexOptions dxDexFile = new DexOptions();
      dxDexFile.targetApiLevel = TARGET_API_LEVEL;
      args[0] = dxDexFile;
    }

    DexFile dxDexFile1;
    try {
      dxDexFile1 = (DexFile)dxDexFileCtor.newInstance(args);
    } catch (ReflectiveOperationException var3) {
      var3.printStackTrace();
      dxDexFile1 = null;
    }

    return dxDexFile1;
  }

  public static byte[] dexClassBytes(String name, byte[] bytes) {
    Pair pair = dexBytes(name, bytes, false);
    return pair != null?(byte[])pair.getKey():null;
  }

  public static Pair<byte[], String> dexBytes(String name, byte[] bytes) {
    return dexBytes(name, bytes, true);
  }

  public static ClassDefItem dexClass(String name, byte[] bytes, DexFile df) {
    ByteArray ba = new ByteArray(bytes);
    DirectClassFile dcf = new DirectClassFile(
      ba, name.replace('.', '/').concat(".class"), false
    );
    if (df == null) {
      df = createDxDexFile();
    }

    setfldval(dcf, "strictParse", Boolean.FALSE);
    dcf.setAttributeFactory(new StdAttributeFactory());
    Object baos = null;
    Object pw = null;

    try {
      ClassDefItem e = (ClassDefItem) 
        ((translate0.getParameterTypes().length == 5)
          ? translate0.invoke(
              (Object)null, new Object[]{
                dcf, bytes, cfopts, dexopts, df              
              }
            )
          : translate0.invoke((Object) null, new Object[]{
                name.replace('.', '/').concat(".class"), 
                bytes, cfopts, dexopts             
              }
            )
          );
      return e;
    } catch (Throwable var8) {
      var8.printStackTrace();
      return null;
    }
  }

  public static Pair<byte[], String> dexBytes(String name, byte[] bytes, boolean dump) {
    ByteArrayOutputStream baos = null;
    PrintWriter pw = null;

    Pair var12;
    try {
      DexFile e = createDxDexFile();
      ByteArray ba = new ByteArray(bytes);
      String path;
      DirectClassFile dcf = new DirectClassFile(
       ba, 
       (path = "/data/local/tmp_clazzes/" 
         + ClassInfo.classNameToPath(name, "class")),
       false
      );
      setfldval(dcf, "strictParse", Boolean.FALSE);
      dcf.setAttributeFactory(new StdAttributeFactory());
      ClassDefItem cdi = (ClassDefItem)(translate0.getParameterTypes().length == 5?translate0.invoke((Object)null, new Object[]{dcf, bytes, cfopts, dexopts, e}):translate0.invoke((Object)null, new Object[]{
        path, bytes, cfopts, dexopts}));
      e.add(cdi);
      if (dump || DEBUG) {
        baos = new ByteArrayOutputStream();
        pw = new PrintWriter(baos);
      }

      byte[] dexBytes = e.toDex(pw, false);
      dexBytes[6] = 53;
      String output = null;
      if (dump || DEBUG) {
        pw.flush();
        baos.flush();
        output = new String(baos.toByteArray(), "UTF-8");
        if (DEBUG) {
          System.err.println(output);
        }
      }

      var12 = Pair.of(dexBytes, output);
    } catch (Throwable var19) {
      var19.printStackTrace();
      throw new RuntimeException(var19);
    } finally {
      try {
        if (pw != null) {
          pw.close();
        }

        if (baos != null) {
          baos.close();
        }
      } catch (Throwable var18) {
        ;
      }

    }

    return var12;
  }
  
  Map<String, Throwable> defExceptions = new HashMap<>();
  Stack<ClassContext> stk = new Stack<>();
  
  public static class ClassContext {
    String name;
    String op;
    Throwable ex;
    public ClassContext(String name, String op) {
      this.name = name;
      this.op = op;
    }
    @Override
    public String toString() {
      if (ex == null) return String.format("%s class '%s'", op, name);
      return String.format(
        "%s class '%s' <FAILED with %s>", op, name, ex
      );
    }
  }
  
  public <T> Class<?> defineClass(final String name, final byte[] clsBytes)
    throws AndroidClassLoader.FatalLoadingError
  {
    return this.defineClass(name, clsBytes, (ClassLoader) null);
  }
  
  
  public <T> Class<?> defineClass(final String className, 
    final byte[] clsBytes,
    final @Nullable ClassLoader loadContext)
    throws AndroidClassLoader.FatalLoadingError
  {
    final ClassContext curContext = new ClassContext(className, "defining");
    stk.push(curContext);
    try {
    if (className == null) throw new Error(
      "AndroidClassLoader.defineClass: className == null"
    );
    if (clsBytes == null) throw new Error(
      "AndroidClassLoader.defineClass: clsBytes == null"
    );
    
    byte[]  dexBytes = null;
    String dexPath = null;
    String odexPath = null;
    File file = null;
    byte bytesWritten = 0;
    final String name = (className != null && className.indexOf('/') != -1)
      ? className.replace('/', '.')
      : className;
      
    final String fileName = String.format(
      "%s__%d__%s",
      name.replaceAll("[^A-Za-z0-9.$]", "_"),
      PosixFileInputStream.getPid(),
      Long.valueOf((long) (Math.random() * 1.0E7D))  
    );
    
    URL url = null;
    try {
      if (WRITE_CLASSFILE || JRE) {
        File clsOutFile = new File(
          String.format("%s/%s.class", dataDir, fileName)
        );
        FileUtils.writeByteArrayToFile(
          new File(String.format("%s/%s.class", dataDir, fileName)),
          clsBytes
        );
        url = clsOutFile.toURL();
        /*System.err.printf(
          "[INFO] ACL: Wrote pre-dexed classfile for '%s' to '%s'\n",
          name, clsOutFile.getAbsoluteFile().getCanonicalFile().getPath()
        );*/
      }
    } catch (final IOException var18) {
      var18.printStackTrace();
    }
    
    try {
      final Class<?> brandNewClass;
      
      if (JRE) {
        final ProtectionDomain pd = new ProtectionDomain(
          new CodeSource(url, new CodeSigner[0]),
          (PermissionCollection) thePermissionCollection
        );
        
        brandNewClass = super.defineClass(
          name, clsBytes, 0, clsBytes.length, pd
        );
      } else {
        Class<?> clazzResult = null;
        final byte[] var20 = dexClassBytes(name, clsBytes);
        dexPath = String.format("%s/%s.dex", new Object[]{dataDir, fileName});
        odexPath = String.format("%s/%s.odex", new Object[]{dataDir, fileName});
        String[] var27;
        int var25 = (var27 = new String[]{dexPath, odexPath}).length;
        for (int var22 = 0; var22 < var25; ++var22) {
          String e = var27[var22];
          file = new File(e);
          if (file.exists()) file.delete();
          try {
            file.createNewFile();
            file.setReadable(true, true);
            file.setWritable(true, true);
            file.setExecutable(true, true);
          } catch (final IOException ioe) {
            if (!System.getProperty("java.io.tmpdir", "").startsWith("/tm/") ||
                 triedRemount)
            {
              throw new LinkageError(String.format(
                "Failure writing out 'dex' bytes to '%s': %s", file, ioe
              ), ioe);
            }
            triedRemount = true;
            
            final java.lang.Process p;
            try {
              p = Runtime.getRuntime().exec(
                "/system/xbin/su root -c " +
                "/system/bin/toolbox mount -o remount,rw /"
              );
            } catch (final IOException pioe) {
              final IllegalStateException error = (IllegalStateException)
                new IllegalStateException(pioe);
              error.addSuppressed(ioe);
              throw error;
            }
            try {
              try (final InputStream in = p.getInputStream();
                   final InputStream er = p.getErrorStream();
                   final OutputStream ou = p.getOutputStream())
              {
                p.waitFor();
              } catch (final IOException ioe2) {
                ioe2.addSuppressed(ioe);
                ioe2.printStackTrace();
              } finally {
                p.destroy();
              }
            } catch (final InterruptedException iex) {
              iex.addSuppressed(ioe);
              p.destroy();
              iex.printStackTrace();
            }
          }
        }
        FileDescriptor var23 = PosixUtil.open(dexPath, 578, 1);
        boolean bufferOffset = false;
        int byteCount, var10003 = byteCount = var20.length;
        long fileOffset = 0L;
        
        PosixUtil.pwriteBytes(var23, var20, 0, var10003, 0L);
        PosixUtil.close(var23);
        int var21 = (int)file.length();
        dalvik.system.DexFile var26 = (dalvik.system.DexFile) loadDex.invoke(
         null, dexPath, null, // odexPath,
         (int) 0 // cookie
        );
        brandNewClass = var26.loadClass(
          name,
          (loadContext != null)? loadContext: this.getParent()
        );
      }
      
      if (brandNewClass != null) this.clsMap.put(name, brandNewClass);
      return brandNewClass;
        
    } catch (Throwable originalFatalError) {
      
      final AndroidClassLoader.FatalLoadingError fex =
        new AndroidClassLoader.FatalLoadingError(
            name, // String className,
            clsBytes, // byte[] clsBytes,
            dexBytes, // byte[] dexBytes,
            dexPath, // String dexPath,
            odexPath, // String odexPath,
            bytesWritten, // int bytesWritten,
            originalFatalError // Throwable error
        );
      defExceptions.put(name, fex);
      for (ClassContext ctx: stk) {
        if (ctx == curContext) continue;
        if (ctx.ex == null) continue;
        fex.addSuppressed(ctx.ex);
      }
      // fex.initCause(originalFatalError);
      {
        final Throwable[] supp = originalFatalError.getSuppressed();
        final int slen = supp.length;
        for (int i=0; i<slen && supp != null; ++i) {
          fex.addSuppressed(supp[i]);
        }
      }
      curContext.ex = fex;
      fex.printStackTrace();
      throw fex;
    }
    } finally {
      stk.pop();
    }
  }
  
  public void linkClass(Class<?> cls) {
    System.err.printf("\n%s: LinkClass(Class<?>) called with Class<?> = [%s]\n", new Object[]{"ACL", cls != null?cls.toString():"<null>"});
    System.err.printf("%s: Callstack:\n  %s\n\n", new Object[]{"ACL", Arrays.toString(Thread.currentThread().getStackTrace())});
  }
  
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    //System.err.printf("loadClass(\"%s\", %s)\n", new Object[]{name, Boolean.valueOf(resolve)});
    final ClassContext curContext = new ClassContext(name, "loading");
    stk.push(curContext);
    try {
    Class<?> cls = (Class<?>)this.clsMap.get(name);
    ClassNotFoundException orig = null;
    if (cls != null) {
      return cls;
    } else {
      try {
        cls = this.findLoadedClass(name);
        if (cls == null) {
          cls = this.findSystemClass(name);
        }

        if (cls != null) {
          return cls;
        }
      } catch (Throwable var27) {
        defExceptions.put(name, var27);
        curContext.ex = var27;
        for (ClassContext ctx: stk) {
          if (ctx == curContext) continue;
          if (ctx.ex == null) continue;
          var27.addSuppressed(ctx.ex);
        }
        orig = (var27 instanceof ClassNotFoundException)
          ? (ClassNotFoundException) var27
          : new ClassNotFoundException(var27.getMessage(), var27);
      }
      
      URLConnection conn = null;
      InputStream is = null;
      
      try {
        String ex = String.format(
          "%s.class", new Object[]{name.replace('.', '/')}
        );
        System.err.printf("... getResources(\"%s\")\n", new Object[]{ex});
        URL url = null;
        
        try {
          url = (URL)findResource.invoke(this, new Object[]{ex});
        } catch (Throwable var24) {
          System.err.println(var24);
        }
        
        if (url == null) {
          url = getSystemResource(ex);
        }

        if (url == null) {
          url = this.getResource(ex);
        }

        if (url == null) {
          try {
            url = (URL)findResource.invoke(Thread.currentThread().getContextClassLoader(), new Object[]{ex});
          } catch (Throwable var23) {
            System.err.println(var23);
          }
        }

        if (url == null) {
          Thread.currentThread().getContextClassLoader();
          url = ClassLoader.getSystemResource(ex);
        }

        if (url == null) {
          url = Thread.currentThread().getContextClassLoader().getResource(ex);
        }

        if (url != null) {
          System.err.println(url);
          conn = url.openConnection();
          conn.setUseCaches(false);
          System.err.println(conn);
          is = conn.getInputStream();
          System.err.println(is);
          byte[] classBytes = IOUtils.toByteArray(is);
          System.err.printf("define: %s\n", new Object[]{url});
          cls = this.defineClass(name, classBytes);
          System.err.printf("  define -> OK { %s }\n", new Object[]{cls});
          this.clsMap.put(name, cls);
          Class<?> var11 = cls;
          return var11;
        }
      } catch (Throwable var25) {
        orig = (var25 instanceof ClassNotFoundException)
          ? (ClassNotFoundException) var25
          : new ClassNotFoundException(name, var25);
        curContext.ex = var25;
        defExceptions.put(name, var25);
        for (ClassContext ctx: stk) {
          if (ctx == curContext) continue;
          if (ctx.ex == null) continue;
          var25.addSuppressed(ctx.ex);
        }
        var25.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException var22) {
            ;
          }
        }

      }

      if (orig != null) {
        throw orig;
      } else {
        return null;
      }
    }
    } finally {
      stk.pop();
    }
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return this.loadClass(name, false);
  }

  public Class<?> findClass(String name) throws ClassNotFoundException {
    return this.loadClass(name, false);
  }

  public class FatalLoadingError extends Error {
    public String className;
    public byte[] clsBytes;
    public byte[] dexBytes;
    public String dexPath;
    public String odexPath;
    public int bytesWritten;
    public Throwable error;
    public Stack<ClassContext> contexts;
    
    public FatalLoadingError(String className, byte[] clsBytes, byte[] dexBytes, String dexPath, String odexPath, int bytesWritten, Throwable error) {
      super(
        String.format(
        "Failed to define class \'%s\' " +
        "(dexPath = \"%s\", bytesWritten = %d)%s: %s",
         className, dexPath, Integer.valueOf(bytesWritten),
         stk.size() > 1
           ? String.format(
               " (class contexts: %s)",
               stk
             )
           : "",
         error
        ),
        error
      );
      this.className = className;
      this.clsBytes = clsBytes;
      this.dexBytes = dexBytes;
      this.dexPath = dexPath;
      this.odexPath = odexPath;
      this.bytesWritten = bytesWritten;
      this.error = error;
      this.contexts = (Stack<ClassContext>) stk.clone();
    }
    
    public FatalLoadingError(String message, Throwable cause) {
      super(message, cause);
    }
  }
}



