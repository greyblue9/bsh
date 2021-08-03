package org.d6r;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import dalvik.system.DexFile;
import dalvik.system.BaseDexClassLoader;
//import org.apache.commons.io.IOUtils;
import org.d6r.Reflect;
import static org.d6r.Reflect.*;
import sun.misc.Unsafe;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({ "unused", "unchecked", "rawtypes", "all" })
public class ClassPathUtil2 {

  private static int total = 0;
  private static final String lf = "\n";
  static final Class<?>[] EMPTY_CLASSES = new Class[0];
  static final Class<?>[] OBJECT_ALONE = { Object.class };

  public static final String F1OF1 = "val$c";
  public static final String F1OF2 = "first";
  public static final String F2OF2 = "second";
  public static final String F_ARRAYLIST_ARRAY;

  public static Class<?> c1;
  public static Class<?> c2EnIn1;
  public static Class<?> BOOTCLASSLOADER;
  public static Field f1of1;
  public static Field f1of2;
  public static Field f2of2;
  public static Field fArrayList_array;
  public static Method getClassNameList;
  public static boolean DEBUG;

  static List<Throwable> errors = null;
  static void addError(Throwable e) {
    (errors != null? errors: (errors = new ArrayList<Throwable>())).add(e);
  }
  public static List<Throwable> getErrors() { return errors; }
  
  private static Boolean _jre;
  
  static boolean isJRE() {
    if (_jre != null) return ((Boolean) _jre).booleanValue();
    try {
      Boolean result = CollectionUtil.isJRE();
      if (result == null) return true;
      _jre = (Boolean) result;
      return ((Boolean) result).booleanValue();
    } catch (Throwable e) {
      e.printStackTrace();
      return true;
    }
  }
   
  static {
    if (!isJRE()) {
      try {
        c1 = Class.forName("java.util.Collections$3");
        (f1of1 = c1.getDeclaredField(F1OF1)).setAccessible(true);
        BOOTCLASSLOADER = Class.forName(
          "java.lang.BootClassLoader", false,
          Thread.currentThread().getContextClassLoader()
        );
        c2EnIn1 = Class.forName("java.lang.TwoEnumerationsInOne");
        (f1of2 = c2EnIn1.getDeclaredField(F1OF2)).setAccessible(true);
        (f2of2 = c2EnIn1.getDeclaredField(F2OF2)).setAccessible(true);
      } catch (Throwable e1) {
        e1.printStackTrace();
      }
    }
    
    F_ARRAYLIST_ARRAY = isJRE()? "elementData": "array";

    try {
      (fArrayList_array = ArrayList.class.getDeclaredField(
        F_ARRAYLIST_ARRAY)
      ).setAccessible(true);
    } catch (Throwable e3) { System.err.println(e3); }
  }
  
  
  
  @SuppressWarnings({"varargs"})
  public static Class<?>[] getCommonSuperclasses(Class<?>... types) {
    Set<Class<?>> commonIfaces = new HashSet<Class<?>>();
    Set<Class<?>> visited = new HashSet<Class<?>>();
    Class<?>[] all = OBJECT_ALONE;
    for (Class<?> item: types) {
      if (! visited.add(item)) continue;

      Class<?> c = item;
      do {
        commonIfaces.add(c);
        commonIfaces.addAll(ClassInfo.getInterfaces(c));
      } while ((c = c.getSuperclass()) != null);
      
      all = commonIfaces.toArray(EMPTY_CLASSES);
      ArrayUtils.reverse(all);
      for (int i=all.length-1; i>=0; --i) {
        Class<?> cls = all[i];
        for (Class<?> pc: types) {
          if (! cls.isAssignableFrom(pc)) {
            all[i] = null;
            break;
          }
        }
      }
    }
    Class<?>[] result = combineArrays(all);
    return (result.length == 0)
      ? OBJECT_ALONE
      : result;
  }
  
  public static <T> T[] nonNulls(T[] array) {
    return combineArrays(array);
  }
  
  public static <T> T[] combineArrays(T[]... arrays) {
    Object[] out = null;
    Class<?> cls = null;
    int max = 0;
    int next = 0;
    boolean write = false;
    do { 
      for (int a=0, alen=arrays.length; a<alen; ++a) {
        Object[] array = arrays[a];
        Class<?> aCls = array.getClass().getComponentType();
        cls = (cls == null)
          ? aCls
          : ((cls.isAssignableFrom(aCls))
              ? cls
              : getCommonSuperclasses(cls, aCls)[0]);
        for (int i=0, len=array.length; i<len; ++i) {
          Object o = array[i];
          if (o == null) continue;
          if (write) out[next++] = o; 
          else max++;
        }
      }
      if ((write = !write)) out = (Object[]) Array.newInstance(cls, max);
    } while (write);
    return (T[]) out;
  }
  
  public static Collection<URL> getClassLoaderEntries(ClassLoader ldr) {
    try {
      Enumeration<URL> en = ldr.getResources("classes.dex");
      
      if (en == null) {
        System.err.println("[WARN] getClassLoaderEntries: "
          + "ClassLoader.getResources(\"classes.dex\") returned NULL");
        return Collections.emptyList();
      }
      
      Object[] combined;
      if (c2EnIn1 != null && c2EnIn1.isAssignableFrom(en.getClass())) {
        // en <: java.lang.TwoEnumerationsInOne
        Object[] array1 
          = (Object[]) fArrayList_array.get(f1of1.get(f1of2.get(en)));
        Object[] array2 
          = (Object[]) fArrayList_array.get(f1of1.get(f2of2.get(en)));
        combined = combineArrays(array1, array2);
        return (List<URL>) (List<?>) Arrays.asList(combined);
      } else if (c1 != null && c1.isAssignableFrom(en.getClass())) {
        // en <: java.util.Collections$3 [AKA Collections.enumeration()]
        Object[] array0 = (Object[]) fArrayList_array.get(f1of1.get(en));
        combined = combineArrays(array0);
      } else {
        return Arrays.asList(
          CollectionUtil.toArray(CollectionUtil.asIterable(en))
        );
      }
      return (List<URL>) (List<?>) Arrays.asList(combined);
      
    } catch (Throwable e) {
      Collection<URL> rs;
      rs = Reflect.searchObject(ldr, URL.class, true, 0, 4);
      if (! rs.isEmpty()) return rs;
      rs = Reflect.searchObject(ldr, URL.class, true, 0, 8);
      if (! rs.isEmpty()) return rs;
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static Collection<URL> getAllClasspathEntries() {
    ClassLoader ldr = Thread.currentThread().getContextClassLoader();
    if (ldr instanceof BaseDexClassLoader) {
      return getClassLoaderEntries(ldr);
    }
    try {
      ArrayList<URL> set = new ArrayList<URL>(64);
      while (ldr != null) {
        try {
          Collection<URL> entries = getClassLoaderEntries(ldr);
          Collections.addAll(set, entries.toArray(new URL[0]));
        } catch (Throwable ex) {
          ex.printStackTrace();
          System.err.println(
            "ERROR: Collection<URL> entries = getClassLoaderEntries(ldr);");
          System.err.println(ex.getClass().getSimpleName());
          System.err.println("\n >>> " + ex.getMessage() + " <<<\n");
          System.err.println(ex.getCause().toString());
          System.err.println(getfldval(ex, "cause").toString());
          System.err.println(String.format(
            "ldr = %s\n dr.getClass() = %s\n", 
            ldr == null ? "<NULL>" : ldr.toString(), 
            ldr == null ? "<NULL>" : ldr.getClass().getName()
          ));
        }
        ldr = ldr.getParent();
        break;
      }
      return set;
    } catch (Throwable e9) {
      e9.printStackTrace();
    }
    return Collections.<URL>emptyList();
  }

  public static Unsafe getUnsafe() {
    return UnsafeUtil.getUnsafe();
  }

  public static String[] getDexClasses(String path) {
    String[] names = new String[0];
    try {
      Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
      Method DexPathList_splitPaths = Reflect.findMethod(dexPathListClass, "splitPaths");
      Method DexPathList_makeDexElements = Reflect.findMethod(dexPathListClass, "makeDexElements");
      Method DexPathList_findClass = null;
      try {
        DexPathList_findClass = dexPathListClass.getDeclaredMethod("findClass", new Class[] { String.class, List.class });
        DexPathList_findClass.setAccessible(true);
      } catch (Throwable e) {
        e.printStackTrace();
      }
      String nativeLibPath = System.getProperty("java.library.path");
      Object /**
      DexPathList
      */
      pathList = getUnsafe().allocateInstance(dexPathListClass);
      ArrayList<File> classPathFiles = (ArrayList<File>) DexPathList_splitPaths.invoke(null, path, nativeLibPath, false);
      ArrayList<Throwable> exList = new ArrayList<>();
      Object[] /**
      DexPathList$Element[]
      */
      dexElements = (Object[]) DexPathList_makeDexElements.invoke(null, classPathFiles, // TODO: Check this parameter
      // (this usage is just a wild guess)
      new File("/data/local/tmp_clazzes"), exList);
      ArrayList<File> libraryPathFiles = (ArrayList<File>) DexPathList_splitPaths.invoke(null, path, nativeLibPath, true);
      Reflect.setfldval(pathList, "nativeLibraryDirectories", // set (File[]) DexPathList.nativeLibraryDirectories
      libraryPathFiles.toArray(new File[0]));
      Reflect.setfldval(pathList, "dexElements", dexElements);
      DexFile df = (DexFile) Reflect.getfldval(
        (
          (Object[]) 
          Reflect.getfldval(pathList, "dexElements")
        )[0], 
        "dexFile"
      );
      int cookie = 0;
      Object oCookie = Reflect.getfldval(df, "mCookie");
      if (!(oCookie instanceof Integer)) {
        DexUtil du = new DexUtil(path);
        names = du.getClassNames();
        return names;
        /*
        System.err.printf("[WARN] getDexClasses(String path = \"%s\"): Skipping DexFile `%s`: DexFile.mCookie = (%s) `%s`, but expected Integer.\n", path, df != null ? df.toString() : "null", oCookie != null ? oCookie.getClass() : "null", oCookie != null ? oCookie.toString() : "null");
        return new String[0];*/
      }
      cookie = ((Integer) oCookie).intValue();
      Method DexFile_getClassNameList;
      try {
        DexFile_getClassNameList = DexFile.class.getDeclaredMethod(
          "getClassNameList", Integer.TYPE);
        DexFile_getClassNameList.setAccessible(true);
        names = (String[]) DexFile_getClassNameList.invoke(null, cookie);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return names;
    } catch (Throwable e2) {
      e2.printStackTrace();
    }
    return names;
  }

  public static Object findDexElement(ClassLoader ldr, File fileToFind) {
    if (DEBUG) Log.d("ClassPathUtil2",
      "findDexElement(ClassLoader ldr: %s, File fileToFind: %s)", ldr, fileToFind);
    
    if (fileToFind == null) throw new IllegalArgumentException(String.format(
      "findDexElement(ldr: %s, fileToFind: %s): fileToFind == null",
      ldr, fileToFind
    ));
    String pathToFind = PosixFileInputStream.resolve(
      PosixFileInputStream.normalizePath(fileToFind.getPath())
    );
    if (pathToFind == null) {
      Log.w(
        "findDexElement", 
        "pathToFind = null (from PosixFileInputStream.normalizePath); " +
        "fileToFind = %s", fileToFind
      );
      pathToFind = fileToFind != null? fileToFind.getPath(): null;
    }
    
    final Object pathList = 
      (ldr == null || BOOTCLASSLOADER.isInstance(ldr) ||
      (pathToFind != null && pathToFind.startsWith("/system/framework/")))
        ? javassist.DexPathListClassPath.forBootClassPath().pathList
        : Reflect.getfldval(ldr, "pathList");
    
    if (pathList != null) {
      Object[] dexElements = Reflect.getfldval(pathList, "dexElements");
      if (dexElements != null) {
        
        final File extFwkDir = new File("/external_sd/_projects/sdk/framework/");
        for (Object dexElement : dexElements) {
          File elemFile = Reflect.getfldval(dexElement, "file");
          if (elemFile == null) continue;
          final String name = elemFile.getName().replace("_dex2jar.jar", ".jar");
          final String nameNoExt = StringUtils.substringBeforeLast(name, ".");
          final int nameLen = name.length();
          final String ext
            =  (String) name.subSequence(nameLen - nameNoExt.length(), nameLen);
          
          final File[] files = (pathToFind.startsWith("/system/framework/"))
            ? new File[] {
                new File(fileToFind.getParentFile(), name),
                new File(
                  fileToFind.getParentFile(),
                  String.format("%s_dex2jar%s", nameNoExt, ext)
                )
              }
            : new File[] { elemFile };
          if (DEBUG) Log.d("ClassPathUtil2",
            "findDexElement: files = %s (fileToFind = %s)",
              Arrays.asList(files), fileToFind);
    
          for (final File file: files) {
            if (file.getPath().equals(pathToFind)) {
              return dexElement;
            }
            final File resElemFile = PosixFileInputStream.resolve(file);
            if (resElemFile.getPath().equals(pathToFind)) {
              return dexElement;
            }
            final File resFileToFind
              = PosixFileInputStream.resolve(new File(pathToFind));
            if (resElemFile.getPath().equals(resFileToFind.getPath())) {
              return dexElement;
            }
          } // files loop
        }
      }
    }
    return (ldr != null)? findDexElement(null, fileToFind): null;
  }

  public static DexFile getDexFile(Object dexElement) {
    return (DexFile) Reflect.getfldval(dexElement, "dexFile");
  }

  public static String[] getClassNames(DexFile dexFile) {
    int cookie = tryGetCookie(dexFile);
    if (cookie == NO_COOKIE) {
      return new DexUtil(
        (String) Reflect.getfldval(dexFile, "mFileName")
      ).getClassNames();
    }
    try {
      if (getClassNameList == null) (getClassNameList 
        = DexFile.class.getDeclaredMethod("getClassNameList", Integer.TYPE)
      ).setAccessible(true);
      
      return
        (String[]) getClassNameList.invoke(null, Integer.valueOf(cookie));
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }

  public static ZipFile getZipFile(Object dexElement) {
    try {
      ZipFile zf = Reflect.getfldval(dexElement, "zipFile");
      if (zf == null) zf = new ZipFile(
        (File) Reflect.getfldval(dexElement, "file")
      );
      return zf;
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }

  public static int NO_COOKIE = -1;

  public static int tryGetCookie(DexFile dexFile) {
    try {
      Object objCookie = Reflect.getfldval(dexFile, "mCookie");
      if (!(objCookie instanceof Integer)) return NO_COOKIE;
      return ((Integer) objCookie).intValue();
    } catch (ClassCastException ex) {
      ex.printStackTrace();
      return NO_COOKIE;
    }
  }
}