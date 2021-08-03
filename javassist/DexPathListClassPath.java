package javassist;

import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;
import java.net.URLConnection;
import java.net.JarURLConnection;
import dalvik.system.BaseDexClassLoader;
import static org.d6r.ClassPathUtil2.getUnsafe;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;
import org.d6r.*;
import java.util.zip.*;
import java.util.jar.*;
import com.android.dex.*;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.d6r.Reflect;
import java.lang.reflect.Method;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import sun.misc.Unsafe;
import java.io.ByteArrayInputStream;
import dalvik.system.VMRuntime;
import java.lang.reflect.Array;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.Buffer;
import java.io.OutputStream;
import org.apache.commons.io.input.AutoCloseInputStream;


/**
static class DexPathList.Element {
  // As DexPathList$Element
  // Fields
  private final DexFile dexFile; //= DexFile@967690a8;
  private final File file; //= new File("/external_sd/_projects/sdk/bsh/trunk/bsh-mod.jar");
  private ZipFile zipFile; //= ZipFile@96848918;
  private final File zip; //= new File("/external_sd/_projects/sdk/bsh/trunk/bsh-mod.jar");
  private final boolean isDirectory; //= false;
  private boolean initialized; //= true;

  // Constructors
  public DexPathList.Element(File file, boolean isDirectory, File zip, DexFile dexFile);

 // Methods
  static DexFile access$000(DexPathList.Element x0)
  public URL findResource(String name)
  public void maybeInit()
  public String toString()
}

final class dalvik.system.DexPathList {
  // As dalvik.system.DexPathList
  // Fields
  private static final String APK_SUFFIX = .apk;
  private static final String DEX_SUFFIX = .dex;
  private static final String JAR_SUFFIX = .jar;
  private static final String ZIP_SUFFIX = .zip;
  private final ClassLoader definingContext; 
    //= XClassLoader;
  private final DexPathList.Element[] dexElements;
  private final 
    IOException[] dexElementsSuppressedExceptions;//= null;
  private final File[] nativeLibraryDirectories = new File[]{
    new File("/system/lib"), 
    new File("/system/vendor/lib"),
    new File("/system/usr/lib"), 
    new File("/system/framework")
  };
  
  public DexPathList(ClassLoader definingContext, String dexPath, String libraryPath, File optimizedDirectory);
  private static DexFile loadDexFile(File file, File optimizedDirectory);
  private static DexPathList.Element[] makeDexElements(
    ArrayList<File> files, File optimizedDirectory,
    ArrayList<IOException> suppressedExceptions);
  private static String optimizedPathFor(File path, File optimizedDirectory)
  private static void splitAndAdd(String searchPath, boolean directoriesOnly, ArrayList<File> resultList)
  private static ArrayList<File> splitDexPath(String path)
  private static File[] splitLibraryPath(String path)
  private static ArrayList<File> splitPaths(String path1, String path2, boolean wantDirectories)
  public Class findClass(String name, List<Throwable> suppressed)
  public String findLibrary(String libraryName)
  public java.net.URL findResource(String name)
  public Enumeration<java.net.URL> findResources(String name)
  public File[] getNativeLibraryDirectories()
  public String toString()
}
*/


public class DexPathListClassPath 
  implements ClassPath,
             ITypeLoader 
{
  public static final String TAG = DexPathListClassPath .class.getSimpleName();
  public static boolean JRE = CollectionUtil.isJRE();
  
  public Object pathList;
  public Object[] dexElements;
  public boolean isSynthetic;
  
  static DexPathListClassPath bootClassPath;
  static DexPathListClassPath defaultClassPath;
  
  
  public static Unsafe U = ClassPathUtil2.getUnsafe();
  
  public static Class<?> DexPathList 
    = classForName("dalvik.system.DexPathList");
  public static Class<?> DexPathList_Element
    = classForName("dalvik.system.DexPathList$Element");
  public static 
  Method DexPathList_findResource = Reflect.findMethod(
    DexPathList, "findResource", String.class);
  
  public InputStream lastIs;
  public static boolean doThrow = false;
  public static final List<Pair<String, Throwable>> errors = new ArrayList<>();
  
  
  
  public DexPathListClassPath(Object pathList) {
    this.pathList = pathList;
    this.dexElements 
      = Reflect.getfldval(pathList, "dexElements");
  }
  
  public DexPathListClassPath(Object[] dexElements) {
    this.pathList = newDexPathList(dexElements);
    this.dexElements = dexElements;
    this.isSynthetic = true;
  }
  
  
  
  public static 
  Object newDexPathList(Object[] dexElements) {
    Object dpl = Reflect.allocateInstance(DexPathList);
    Reflect.setfldval(dpl, "definingContext",
      Thread.currentThread().getContextClassLoader()
    );
    Reflect.setfldval(dpl, "dexElementsSuppressedExceptions",
      new IOException[0]
    );
     
    String[] libPaths = StringUtils.split(
      System.getProperty("java.library.path"), ":"
    ); 
    File[] libDirs = new File[libPaths.length]; 
    for (int i=0; i<libPaths.length; i+=1) { 
      libDirs[i] = new File(libPaths[i]);
    }
    Reflect.setfldval(dpl, "nativeLibraryDirectories",
      libDirs
    );
    /*Object[] dexElements 
      = Array.newInstance(DexPathList_Element, 1);
    dexElements[0]
      = Thread.currentThread().getContextClassLoader()
       .getPathList().dexElements[0];*/
    Reflect.setfldval(dpl, "dexElements", dexElements);
    return dpl;
  }
  
  public static Object[] makeDexElements(boolean setInitialized, 
  File... files) 
  {
    Log.e(
      TAG, "makeDexElements(setInitialized: %s, files: File[]{ %s }): " +
      "Allocating and opening entire DexPathList.Element[] set from scratch!",
      setInitialized, StringUtils.join(files, ", ")
    );
    if (JRE) throw new AssertionError(
      "Should not be used in Java VM; this class is meant for dalvik runtime ONLY!"
    );
    
    List<Object> dples = new ArrayList<Object>(); 
    for (File file: files) { 
      ZipFile zipFile = null;
      try {
        zipFile = new ZipFile(file);
      } catch (ZipException e) {
        Log.d(TAG, 
          "[WARN] [%s]: <Bad ZIP: %s>",
          file, e.getClass().getSimpleName(), e
        );
        continue;
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }
      Object el = Reflect.allocateInstance(DexPathList_Element);
      Reflect.setfldval(el, "file", file);
      Reflect.setfldval(el, "zip", file);
      Reflect.setfldval(el, "zipFile", zipFile);
      Reflect.setfldval(el, "initialized", Boolean.valueOf(setInitialized));
      Reflect.setfldval(el, "isDirectory", Boolean.valueOf(false));
      dples.add(el);
    }; 
    Object[] els = dples.toArray(
      (Object[]) Array.newInstance(DexPathList_Element, 0)
    );
    return els;
  }
  
  public static Class<?> classForName(String name) {
    try {
      return Class.forName(name, false,
        Thread.currentThread().getContextClassLoader());
    } catch (Exception e) { e.printStackTrace(); }
    return null;
  }
  
  @Override
  public void close() {
    Log.d(TAG, "DexPathListClassPath.close()");
  }
  
  @Override
  public URL find(String className) {
    URL url = find(className, false);
    return (url != null)
      ? url
      : find(className, true);
  }
  
  public URL find(String className, boolean doExpensiveLookup) {
    final String internalName = ClassInfo.classNameToPath(
      (StringUtils.endsWith(className, ".class"))
        ? StringUtils.substringBeforeLast(className, ".class")
        : className,
      null
    );
    final String dottedName = internalName.replace('/', '.');
    final String resPath = internalName.concat(".class");
    
    if (!JRE) {
      try {
        Object dexElement = DexFinder.findDexElement(dottedName);
        if (dexElement != null) {
          ZipFile zipFile = Reflect.getfldval(dexElement, "zipFile");
          if (zipFile == null) {
            Reflector.invokeOrDefault(dexElement, "maybeInit");
            zipFile = Reflect.getfldval(dexElement, "zipFile");
          }
          if (zipFile != null) {
            ZipEntry ze = zipFile.getEntry(
              internalName.concat(".class")
            );
            if (ze != null) {
              try (final InputStream is = zipFile.getInputStream(ze)) {
                final byte[] bytes = IOUtils.toByteArray(is);
                final URL url
                  = ZipByteArrayClassPath.MemoryURLStreamHandler.urlForByteArray(
                      bytes, ze.getName()
                    );
                try (final OutputStream os = url.openConnection().getOutputStream())
                {
                  IOUtils.write(bytes, os);
                }
                return url;
              }
            }
          }
        }
      } catch (final Exception e) {
        new RuntimeException(className, e).printStackTrace();
      }
    }
    
    try {
      final URL url 
        = (ClassLoader.getSystemClassLoader().getParent() != null)
            ? ClassLoader.getSystemClassLoader().getParent().getResource(resPath)
            : ClassLoader.getSystemResource(resPath);
      if (url != null) return url;
    } catch (Exception ex) { }
    
    if (! doExpensiveLookup) return null;
    
    try {
      final URL url 
        = Thread.currentThread().getContextClassLoader().getResource(resPath);
      if (url != null) return url;
    } catch (Exception ex) { }
    
    if (!JRE) {
      try {
        final URL url = (URL) DexPathList_findResource.invoke(pathList, resPath);
        if (url != null) return url;
      } catch (ReflectiveOperationException ex) {
        errors.add(Pair.of(className, ex));
      }
    }
    return ClassLoader.getSystemClassLoader().getResource(resPath);
  }
  

  
  @Override
  public boolean tryLoadType(String name, Buffer buf) {
    
    try (InputStream is = openClassfile(name)) {      
      Log.d(TAG, 
        "[INFO] name: '%s', buf = %s", name, buf
      );
      byte[] b = IOUtils.toByteArray(is);
      buf.putByteArray(b, 0, b.length);
      Log.d(TAG, 
        "[INFO] returning with %d bytes in buffer", b.length
      );
      return true;
    } catch (Exception ex) {
      errors.add(Pair.of(name, ex));
    }
    return false;
  }
  
  
  @Override
  public InputStream openClassfile(String className) 
    throws NotFoundException
  {
    final String internalName = ClassInfo.classNameToPath(
      (StringUtils.endsWith(className, ".class"))
        ? StringUtils.substringBeforeLast(className, ".class")
        : className,
      null
    );
    final String dottedName = internalName.replace('/', '.');
    final String resPath = internalName.concat(".class");
    
    byte[] bytes = null;
    try {
      if (!JRE) {
        try {
          Object dexElement = DexFinder.findDexElement(dottedName);
          if (dexElement != null) {
            ZipFile zipFile = Reflect.getfldval(dexElement, "zipFile");
            if (zipFile == null) {
              Reflector.invokeOrDefault(dexElement, "maybeInit");
              zipFile = Reflect.getfldval(dexElement, "zipFile");
            }
            if (zipFile != null) {
              ZipEntry ze = zipFile.getEntry(
                internalName.concat(".class")
              );
              if (ze != null) {
                return new AutoCloseInputStream(zipFile.getInputStream(ze));
              }
            }
          }
        } catch (final Exception e) {
          new RuntimeException(className, e).printStackTrace();
        }
      }
      
      final URL url1 = find(className, false);
      final URL url;
      if (url1 == null) {
        final URL url2 = find(className, true);
        url = (url1 != null)? url1: url2;
      } else {
        url = url1;
      }
      if (url == null) {
        if (doThrow) throw new NotFoundException(className);
        else return null;
      }
      
      final URLConnection conn = url.openConnection();
      conn.setUseCaches(
        url.toString().indexOf("framework/core.") != -1 ||
        url.toString().indexOf("bootclasspath") != -1 ||
        url.toString().indexOf("framework/framework.") != -1
      );
      final InputStream isRaw = conn.getInputStream();
      final InputStream is = new AutoCloseInputStream(isRaw);
      {
        return is;
      }
    } catch (IOException ioe) {
      throw (NotFoundException) (new NotFoundException(className).initCause(ioe));
    }
  }
  
  @Override
  public String toString() {
    return String.format(
      "DexPathListClassPath{ pathList.dexElements.length = %d }",
      dexElements.length
    );
  }
  
  public static DexPathListClassPath forBootClassPath() {
    if (bootClassPath != null) return bootClassPath;
    
    String bcp = ClassInfo.getBootClassPath();
    String[] bcpPaths = StringUtils.split(bcp, ":"); 
    List<File> files = new ArrayList<File>(); 
    for (String bcpPath: bcpPaths) {
      File extFile = new File(String.format(
        "/external_sd/_projects/sdk/framework/%s_dex2jar.jar",
         StringUtils.substringBefore(
           StringUtils.substringAfter(
             bcpPath, "/system/framework/"
           ),
           ".jar"
         )
       )); 
       files.add(extFile);
     }
     
     Object[] els = makeDexElements(true, files.toArray(new File[0]));
     bootClassPath = new DexPathListClassPath(els);
     bootClassPath.isSynthetic = true;
     return bootClassPath;
  }
  
  private static boolean initialized = false;
  
  
  public static List<ClassPath> getClassPaths(ClassPool pool) {
    List<ClassPath> classpaths = new ArrayList<ClassPath>(4);
    Object pathList
      = Reflect.getfldval(Reflect.getfldval(pool, "source"), "pathList");
    while (pathList != null) {
      Object head = Reflect.getfldval(pathList, "path");
      Object tail = Reflect.getfldval(pathList, "next");
      if (head instanceof ClassPath) {
        classpaths.add((ClassPath) head);
      }
      pathList = tail;
    }
    return classpaths;
  }
  
  
  
  
  public static ClassPool getDefault() {
    javassist.ClassPool cp = javassist.ClassPool.getDefault();
    
    if (initialized) return cp;
    initialized = true;
    
    if (JRE) return cp;
    synchronized (DexPathListClassPath.class) {
      if (getClassPaths(cp).size() < 2) {
        initialized = true;
        try {
          javassist.ClassPathList cplist = Reflect.getfldval(
            Reflect.getfldval(cp, "source"), "pathList"
          );
          
          Reflect.setfldval(
            cplist, "path", DexPathListClassPath.forBootClassPath()
          );
          Reflect.setfldval(
            cplist, "next", Reflect.newInstance(
              javassist.ClassPathList.class, 
              new DexPathListClassPath(
                (Object) Reflect.getfldval(
                  Thread.currentThread().getContextClassLoader(), 
                  "pathList"
                )
              ), 
              null // // List end sentinel ("Object")
            )
          );
          initialized = true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return cp;
  }
  
}
