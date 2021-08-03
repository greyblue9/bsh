package org.d6r;

import java.util.TreeMap;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;

import static android.os.Process.myPid;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import dalvik.system.BaseDexClassLoader;
import org.apache.commons.lang3.StringEscapeUtils;
import com.android.dex.Dex;
import com.android.dex.ClassDef;
import org.d6r.annotation.Returns;
import org.d6r.annotation.NonDumpable;
import com.android.dx.io.DexIndexPrinter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.d6r.ClassInfo.SunLoader;
import org.d6r.ClassInfo.CLoader;
import static org.d6r.CollectionUtil.sysJarsWhitelist;
import static org.d6r.CollectionUtil.SYS_JAR_DIR;

import bsh.Capabilities;
import com.google.common.collect.Sets;
import bsh.operators.Extension;

import sun.misc.Unsafe;
import sun.misc.URLClassPath;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import org.apache.commons.collections4.map.ListOrderedMap;
import static java.lang.String.format;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.util.regex.*;
import java.lang.reflect.*;
import static org.d6r.Reflect.*;




class ClasspathDataHolder {
  public static final String TAG
    = ClasspathDataHolder.class.getSimpleName();
  
  static int classpathMapSizeAtLastUpdate = -1;
  static Set<String> lastKeySet = CollectionFactory.newSet();
  @NonDumpable
  public static SortedSet<String> allClasses = new TreeSet<>();
  
  public static void update(final Map<String, String[]> classpathMap) {
    if (classpathMap == null) return;
    if (classpathMap.size() == classpathMapSizeAtLastUpdate) {
      return;
    }
    final int beforeSize = allClasses.size();
    final Set<String> addedKeys
        = Sets.difference(classpathMap.keySet(), lastKeySet);
    if (addedKeys.isEmpty()) return;
    final Iterable<String>[] iterables
       = (Iterable<String>[]) new Iterable<?>[addedKeys.size() + 1];
    int nextIndex = 0;
    iterables[nextIndex++] = allClasses;
    for (final String addedKey: addedKeys) {
      if (!(classpathMap.get(addedKey) instanceof String[])) {
        classpathMap.remove(addedKey);
        continue;
      }
      String[] classNames = classpathMap.get(addedKey);
      iterables[nextIndex++] = Arrays.<String>asList(classNames);
    }
    final Iterable<String> allNamesIterable = Iterables.concat(iterables);
    final SortedSet<String> allNames = Sets.newTreeSet(allNamesIterable);
    classpathMapSizeAtLastUpdate = classpathMap.size();
    lastKeySet.addAll(addedKeys);
    final int afterSize = (allClasses = allNames).size();
    final int added = afterSize - beforeSize;
    Log.d(TAG, "New size: %d (+%d classes)", afterSize, added);
    allClasses = allNames;
  }
}



@SuppressWarnings({"unused", "unchecked", "rawtypes", "all"})
public class ClassPathUtil {
  
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
  
  @Returns(type = URLClassPath.class)
  public static final LazyMember<Field> FLD_CLASSLOADER_UCP = LazyMember.of(
    "java.net.URLClassLoader", "ucp"
  );
  
  @Returns(type = URLClassPath.class)
  public static final LazyMember<Field> FLD_BOOT_UCP = LazyMember.of(
    "sun.misc.Launcher$BootClassPathHolder", "bcp"
  );
  
  public static boolean LOGV = Log.isLoggable(Log.SEV_VERBOSE);
  public static boolean LOGD = Log.isLoggable(Log.SEV_DEBUG);
  
  public static boolean SCAN_DEBUG
    = "true".equals(System.getProperty("scan.debug"));
  
  public static final String EMPTY_STRING = "";
  
  @NonDumpable
  public static final Map<String, String[]> classpathMap = newMap();
  
  private static int total = 0;
  
  private static final String lf = String.valueOf((char) 0x0a);

  public static final String F1OF1 = "val$c";
  public static final String F1OF2 = "first";
  public static final String F2OF2 = "second";
  
  public static Class<?> e1Cls;
  public static Class<?> e2Cls;

  public static Field f1of1;
  public static Field f1of2;
  public static Field f2of2;
  public static Matcher URL_TO_PATH_MATCHER
    = Pattern.compile("^([^:]+:)*/*(/[^:!]+)(!.*)*$")
             .matcher("");
             
  public static Matcher[] BLACKLIST_MATCHERS = {
    Pattern.compile(
      "(?:core-junit|ext\\.|telephony|sec_edm|smartfaceservice|codeaurora|uiautomator|secmo|sec[^/]*security|automatorviewer|gnujpdf|jol[_-]|javac_proguard_dex2jar|jline-2.9-source|settings|framework2|commonimsinterface|jna|jni|webview|chromium|chrome|webkit).*(?:apk|zip|dex|jar)",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    ).matcher("")
  };
  
  public static final File indexesDir = new File(
    "/external_sd/_projects/__jar_index__"
  );
  
  
  public static final String CLASS_SUFFIX = ".class";
  public static final int SUFFIX_LEN = 6; // CLASS_SUFFIX.length();
  
  public static int mapJreClasspath(final ClassLoader classLoader) {
    return mapClassPathJre2();
  }
   
  public static String TAG = ClassPathUtil.class.getSimpleName();
  public static final Map<Matcher, String> INDEX_MRMAP = RealArrayMap.toMap(
    (Map.Entry<Matcher, String>[]) (Map.Entry<?, ?>[])
    new Map.Entry<?, ?>[]{
      Pair.of(Pattern.compile("//*", Pattern.DOTALL).matcher(""), "@"),
      Pair.of(Pattern.compile("^@@*|@@*$", Pattern.DOTALL).matcher(""), "")
    }
  );
  
  
  @Nullable
  public static File getIndexFileFor(final URL url) {
    final String path = PathInfo.getPathInfo(url).path;
    final File rawFile = new File(path);
    if (!rawFile.exists() || rawFile.isDirectory()) return null;
    final long fileSize = rawFile.length();
    if (fileSize < 10L) return null;
    String modifiedPathForName = path;
    for (final Map.Entry<Matcher, String> entry: INDEX_MRMAP.entrySet()) {
      if (!entry.getKey().reset(modifiedPathForName).find()) continue;
      modifiedPathForName = entry.getKey().replaceAll(entry.getValue());
    }
    
    final File indexFile = new File(
      new File(indexesDir, ".index"), // subdir
      String.format("%s___%d.index", modifiedPathForName, fileSize) // name
    );
    if (!indexFile.exists()) {
      new File(indexFile.getParent()).mkdirs();
    }
    return indexFile;
  }
  
  
  static final String[] ZIP_EXTS = {
    "jar", "JAR", "zip", "ZIP", "apk", "APK", "war", "ear"  
  };
  static {
    Arrays.sort(ZIP_EXTS);
  }
  
  public static boolean  isProbablyJarFile(final String path) {
    final int pathLen = path.length();
    if (pathLen > 4 && path.charAt(pathLen-4) == '.') {
      final int result = Arrays.binarySearch(
        ZIP_EXTS, 0, ZIP_EXTS.length,
        (String) path.subSequence(pathLen - 3, pathLen),
        String.CASE_INSENSITIVE_ORDER
      );
      if (result >= 0) return true;
    }
    
    final String name = FilenameUtils.getName(path);
    final String validNamePart = (String) name.subSequence(
      (!name.isEmpty() && name.charAt(0) == '.') ? 1 : 0, name.length()
    );
    final Iterable<String> orderedExts = IterableUtils.reversedIterable(
      Arrays.asList(StringUtils.stripAll(
        StringUtils.split(validNamePart, "."), " \t\r\n"
      ))
    );
    for (final String ext: orderedExts) {
      switch (StringUtils.lowerCase(ext)) {
        case "jar":
        case "zip":
        case "apk":
        case "aar":
        case "war":
        case "ear":
        case "pack":
        case "xpi":
        case "z01":
          return true;
        case "class":
        case "dex":
        case "odex":
          return false;
      }
    }
    return false;
  }
  
  private static final Map<String, URLStreamHandler> _handlers
      = new TreeMap<>();
  
  public static final URLStreamHandler getHandler(final String urlScheme) {
    final String protocol = StringUtils.lowerCase(urlScheme);
    if (_handlers.containsKey(protocol)) {
      return _handlers.get(protocol);
    }
    final String classNamePattern = (isJRE())
      ? "sun.net.www.protocol.%1$s.Handler"
      : "libcore.net.url.%2$sHandler";
    final String protocolUcFirst
      = StringUtils.capitalize(protocol);
    final String className
        = String.format(classNamePattern, protocol, protocolUcFirst);
    try {
      final Class<? extends URLStreamHandler> c = 
       (Class<? extends URLStreamHandler>) (Class<?>)
         Class.forName(
            className, false, ClassLoader.getSystemClassLoader()
          );
      final Constructor<?> ctor = c.getDeclaredConstructor();
      ctor.setAccessible(true);
      final URLStreamHandler handler
         = (URLStreamHandler) ctor.newInstance();
      _handlers.put(protocol, handler);
      return handler;
    } catch (final ReflectiveOperationException | LinkageError e) {
      throw new Error(String.format(
        "Unable to %s a URLStreamHandler for the protocol '%s' using " +
        "class name \"%s\" due to unexpected error: %s",
        (e instanceof LinkageError || e instanceof ClassNotFoundException)
          ? "look up class via Class.forName(..) to create"
          : "default-instantiate",
        protocol, className, e
      ), e);
    }
  }
  
  static final LazyMember<Method> URLSTREAMHANDLER_OPEN_CONNECTION =
    LazyMember.of(URLStreamHandler.class, "openConnection", URL.class);
  
  public static int mapClassPathJre2() {
    final String TAG = "mapClassPathJre2";
    
    final Map<String, String[]> cpm = classpathMap;
    int added = 0;
    
    try {
      nextClasspathItem:
      for (final URL url: getAllClasspathEntries()) {
        if (LOGV) Log.v(TAG, "Processing url: %s", url);
        
        final File indexFile = getIndexFileFor(url);
        if (indexFile == null) continue;
        
        final String path = PathInfo.getPathInfo(url).path;
        String[] classNames = null;
        if (cpm.containsKey(path) &&
            (classNames = cpm.get(path)) != null &&
            classNames.length > 0)
        {
          continue;
        }
        boolean readIndex = false;
        List<String> names = null;
        try {
          if (indexFile.exists()) {
            if (LOGV) Log.v(TAG,
              "Classpath item '%s' URL(\"%s\") has existing index at [%s]" +
              " with size = %d bytes.",
              path,
              StringEscapeUtils.escapeJava(url.toString()),
              indexFile.getAbsolutePath(),
              indexFile.length()
            );
            final List<String> lines = Files.readAllLines(
              Paths.get(indexFile.getAbsolutePath()),
              StandardCharsets.ISO_8859_1
            );
            if (LOGV) Log.v(TAG, "Read lines to list of size %d: %s",
              lines.size(), lines.subList(0, Math.min(lines.size(), 4))
            );
            names = lines;
            classNames = names.toArray(new String[0]);
            readIndex = true;
            continue nextClasspathItem;
          }
          
          // Index missing or not usable
          if (isProbablyJarFile(path)) {
            final URL jarUrl = ("file".equals(url.getProtocol()))
              ? ClassInfo.toJarURL(url, "")
              : ("jar".equals(url.getProtocol()))
                  ? url
                  : null;
            final URLStreamHandler handler = getHandler("jar");
            final JarURLConnection jconn =
              URLSTREAMHANDLER_OPEN_CONNECTION.invoke(
                handler, jarUrl
              );
            if (jconn == null) {
              Log.e(
                TAG, "jconn == null; handler: %s, url: %s",
                handler, url
              );
              continue nextClasspathItem;
            }
            jconn.setUseCaches(true);
            final JarFile zipFile = jconn.getJarFile();
            names = ClassInfo.getClassNamesFromEntries(zipFile, true);
            classNames = names.toArray(new String[0]);
            Log.d(
              TAG, "Jar file from %s.getJarFile(): %s [%s] -> %d names",
              jconn.getClass().getSimpleName(),
              zipFile, zipFile.getName(), names.size()
            );
            continue nextClasspathItem;
          }
          
          final DexUtil dexUtil;
          try {
            dexUtil = new DexUtil(path);
            classNames = dexUtil.getClassNames();
            names = Arrays.asList(classNames);
          } catch (final Throwable t) {
            Log.wtf(
              TAG, "Bogus DEX file: [%s]! " +
              "Final attempt failed reading from URL(\"%s\"): %s",
              path, url, t
            );
            continue nextClasspathItem;
          }
        } catch (final IOException ioe) {
          ioe.printStackTrace(System.err);
          Log.e(
            TAG, "Exception encountered scanning element [%s] with " +
            "URL(\"%s\"): %s",
            path, url, ioe
          );
        } finally {
          if (classNames != null) {
            cpm.put(path, classNames);
            added += classNames.length;            
          }
          try {
            if (classNames != null && !readIndex) {
              if (LOGV) Log.v(
                TAG, "Read %d classes from [%s]%s",
                classNames.length, path, (readIndex) ? " (indexed)" : ""
              );

              if (indexFile.exists()) {
                try {
                  indexFile.delete();
                } catch (final Exception ioe) {
                  Log.e(TAG, ioe);
                  try {
                    FileUtils.forceDelete(indexFile);
                  } catch (final IOException ioe2) {
                    Log.e(TAG, ioe2);
                    ioe2.addSuppressed(ioe);
                    ioe2.printStackTrace();
                  }
                }
              }
              try {
                indexFile.createNewFile();
                indexFile.setWritable(true);
                indexFile.setReadable(true);
              } catch (final IOException e) {
                Log.w(TAG, e);
              }
              // Collections.sort(names);
              try {
                if (LOGV) Log.v(
                  TAG, "Writing %d lines to index file: %s ...",
                  names.size(), indexFile
                );
                FileUtils.writeLines(
                  indexFile, StandardCharsets.ISO_8859_1.name(), names
                );
                if (LOGV) {
                  Log.v(
                    TAG, "New filesize is --> %d bytes; verifying... ", 
                    new File(indexFile.getAbsolutePath()).length()
                  );
                  final List<String> read = FileUtils.readLines(
                    new File(indexFile.getAbsolutePath()),
                    StandardCharsets.ISO_8859_1
                  );
                  if (LOGV) Log.v(
                    TAG, "Read %d lines from %s", read.size(), indexFile
                  );
                }
              } catch (final IOException ioex) {
                ioex.printStackTrace();
              }
            }
          } catch (final Throwable t2) {
            t2.printStackTrace();
          }
        }
      } // forentry in getAllClasspathEntries()
      
      return added;
    } finally {
      if (added > 0) ClasspathDataHolder.update(cpm);
    }
  }
  
  
  
  public static <K, V> Map<K, V> newMap() {
    return (Map<K, V>) (Object) new THashMap();
  }
  
  public static <K, V> Map<K, V> newMap(int size) {
    return (Map<K, V>) (Object) new THashMap(size);
  }
  
  public static <K, V>
  Map<K, V> newMap(Map<? extends  K, ? extends V> map) {
    return (Map<K, V>) (Object) new THashMap(map);
  }
  
  public static <E> Set<E> newSet() {
    return (Set<E>) (Object) new THashSet();
  }
  
  public static <E> Set<E> newSet(int size) {
    return (Set<E>) (Object) new THashSet(size);
  }
  
  public static <E> 
  Set<E> newSet(Collection<? extends E> coll) {
     return (Set<E>) (Object) new THashSet(coll);
  }
  
  
  
  
  
  static {
    if (!isJRE()) {
      try {
        final VMRuntime vmr = VMRuntime.getRuntime();
        try {
          vmr.clearGrowthLimit();
        } catch (Throwable e) { 
          System.err.println(e.toString());
          throw Reflector.Util.sneakyThrow(e);          
        }
        try {
          vmr.startJitCompilation();
        } catch (Throwable e) { 
          System.err.println(e.toString());
        }
      } catch (final Throwable e) {
        e.printStackTrace();
      }
      try {
        e1Cls = Class.forName("java.util.Collections$3");
        f1of1 = e1Cls.getDeclaredField(F1OF1);
        f1of1.setAccessible(true);
      } catch (Throwable e1) {
        System.err.println(e1);
        e1Cls = null;
        f1of1 = null;
      }
      try {
        e2Cls = Class.forName(
          "java.lang.TwoEnumerationsInOne");
        f1of2 = e2Cls.getDeclaredField(F1OF2);
        f1of2.setAccessible(true);
        f2of2 = e2Cls.getDeclaredField(F2OF2);
        f2of2.setAccessible(true);
      } catch (Throwable e2) {
        System.err.println(e2);
        e2Cls = null;
        f1of2 = null;
        f2of2 = null;
      }
    } else {
      e1Cls = null;
      f1of1 = null;
      e2Cls = null;
      f1of2 = null;
      f2of2 = null;
    }
  }
  
  public static String[] SYS_JAR_NAME_WHITELIST 
    = CollectionUtil.sysJarsWhitelist;
  public static final String SYS_JAR_DIR = String.format(
    "%s/framework", System.getenv("ANDROID_ROOT"));
  public static final String SYS_JAR_CLASSES_DIR
    = "/external_sd/_projects/sdk/framework/";
  public static Pattern URL_PATH_REGEX
    = Pattern.compile("^((jar:)?file:)?/*(/[^!]*)(!.*$)?$");


  
  public static Collection<String> getClassNames(String... paths) {
    Method m = null;
    if (!isJRE()) {
      try {
        m = DexFile.class.getDeclaredMethod(
          "getClassNameList",  Integer.TYPE
        );
        m.setAccessible(true);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
    
    final SortedSet<String> all = new TreeSet<String>();
    
    outer:
    for (String path: paths) {
      String altPath = path;
      if (!isJRE()) {
        for (Matcher mchr: BLACKLIST_MATCHERS) {
          if (mchr.reset(path).find()) {
            if (LOGV) Log.v(TAG, 
              "Skipping blacklisted jar: [%s]\n", path
            );
            continue outer;
          }
        }
      }
      
      path = PosixFileInputStream.resolve(path);
      
      if (path.indexOf("/system/framework/") == 0) {
        altPath = path.replace(
          "/system/framework/", 
          "/external_sd/_projects/sdk/framework/"
        );
      }
      
      String[] classNames = (classpathMap.containsKey(path))
        ? classpathMap.get(path)
        : ClassInfo.getClassNamesFromEntries(path).toArray(new String[0]);
      
      
      System.err.printf("getClassNames: %s\n", altPath);
      
      String fileName = path.substring(path.lastIndexOf('/') + 1);
      
      if (classNames == null) {
        try {
          DexUtil du = new DexUtil(path);
          classNames = du.getClassNames();
        } catch (Throwable e) {
          e.printStackTrace();
          if (!(e instanceof IOException)) {
            Log.f(TAG, "exception on %s: %s", path, e);
            Log.f(TAG, e);
            throw Reflector.Util.sneakyThrow(e);
          }
          System.err.printf(
            "[WARN] %s: Dex is invalid: %s: %s\n",
            path, e.getClass().getSimpleName(), e.getMessage()
          );
        }
      }
      
      if (classNames != null) {
        Arrays.sort(classNames);
        classpathMap.put(path, classNames);
        if (altPath != null) classpathMap.put(altPath, classNames);
        Collections.addAll(all, classNames);
      }
    }
    return all;
  }
  
  
  public static Map<String, String[]> mapClassPath() {
    if (isJRE()) {
      mapClassPathJre2();
      return classpathMap;
    }
    Method m = null;
    try {      
      m = DexFile.class.getDeclaredMethod(
        "getClassNameList",  Integer.TYPE
      );
      m.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    
    BaseDexClassLoader ldr = (BaseDexClassLoader) 
      Thread.currentThread().getContextClassLoader();
    Object[] elems = Reflect.getfldval(
      Reflect.getfldval(ldr, "pathList"), "dexElements"
    );
    Map<String, String[]> mm = newMap();
    String[] classNames = null;
    
    outer:
    for (Object elem : elems) {
      if (elem == null) continue; 
      File file = Reflect.getfldval(elem, "file");
      for (Matcher mchr: BLACKLIST_MATCHERS) {
        if (mchr.reset(file.getPath()).find()
        ||  mchr.reset(elem.toString()).find()) 
        {
          if (LOGV) Log.v(TAG, 
            "Skipping blacklisted jar: [%s]\n", elem
          );
          continue outer; 
        }
      }
      DexFile dexfile = Reflect.getfldval(elem, "dexFile");
      String path = file.getPath();
      String fileName 
        = path.substring(path.lastIndexOf('/') + 1);
      try {
        Object cookie = null;
        cookie = Reflect.getfldval(dexfile, "mCookie");
        classNames = (String[]) m.invoke(null, cookie);
        Arrays.sort(classNames);
        mm.put(path, classNames);
      } catch (Throwable e) {
        // System.err.println(e.getClass().getName() + ":");
        if (elem != null) System.err.println(elem);
        // System.err.println(e.getMessage());
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        if (e.getCause() != null) {
         System.err.println(e.getCause());
        }
      }
    } // for elem
    
    
    String[] entryPaths 
      = System.getenv("BOOTCLASSPATH").split(":");
    outer:
    for (int p = 0; p < entryPaths.length; p++) {
      String path = entryPaths[p];
      for (Matcher mchr: BLACKLIST_MATCHERS) {
        if (mchr.reset(path).find()) {
          if (LOGV) Log.v(TAG, 
            "Skipping blacklisted jar: [%s]\n", path
          );
          continue outer;
        }
      }
      String altPath = path.replace(
        "/system/framework/", 
        "/external_sd/_projects/sdk/framework/"
      );
      System.err.println(altPath);
      String fileName      
        = path.substring(path.lastIndexOf('/') + 1);
      if (fileName.indexOf("webviewchromium") != -1) {
        continue; 
      }
      try {
        DexFile dexfile = new DexFile(altPath);
        Object cookie = Reflect.getfldval(dexfile, "mCookie");
        classNames = (String[]) m.invoke(null, cookie);
        Arrays.sort(classNames);
        mm.put(path, classNames);
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      }
    }
    return mm;
  }
  
  public static String findClass(String className) {
    final Map<String, String[]> classpathMap = mapClassPath();
    for (Entry<String, String[]> entry: 
         classpathMap.entrySet()) 
    {
      String[] classNames = entry.getValue();
      String path;
      int result = Arrays.binarySearch(
        classNames, 0, classNames.length - 1, className
      ); 
      if (result < 0) continue; 
      return (path = entry.getKey());
    }     
    return null;
  }
  
  public static Matcher classNamePattern(String className) {
    return Pattern.compile(String.format(
      "^%s$", 
      className.replace("$", "\\$").replace(".", "[.$]")
    )).matcher("");
  }
  
  
  public static Collection<String> findClassSource(final String... names) {
    for (final String s: names) {
      if (!SourceVersion.isName(s)) {
        return findMatchingDexClassJarsSlow(names);
      }
    }
    final Collection<String> results = new ArrayList<>(names.length);
    for (final String className: names) {
      try {
        final URL jarUrl = findClassSource.findClassSource(className);
        if (jarUrl == null) continue;
        
        final URI fileUri = ("file".equals(jarUrl.getProtocol()))
          ? jarUrl.toURI()
          : URI.create(StringUtils.substringBefore(jarUrl.getFile(), "!/"));
        
        final String us = jarUrl.toString();
        results.add(
          (fileUri.isAbsolute())
            ? fileUri.getRawPath()
            : String.valueOf(Paths.get(fileUri).toAbsolutePath())
        );
      } catch (final URISyntaxException use) {
        throw Reflector.Util.sneakyThrow(use);
      }
    }
    return results;
  }
    
  public static Collection<String> findMatchingDexClassJarsSlow(
    final String... classRegexes)
  {
    Matcher[] ptrns = new Matcher[classRegexes.length];
    int idx = 0;
    for (String classRegex: classRegexes) {
      ptrns[idx] = SourceVersion.isName(classRegex)
          ? classNamePattern(classRegex)
          : StringCollectionUtil.toMatcher(classRegex);
      idx += 1;
    }
    return findClassSource(ptrns);
  }
  
  public static Collection<String> findClassSource
  (Matcher... clsptrns) 
  { 
    if (clsptrns.length == 0) {
      return Collections.<String>emptyList();
    }
    Map<String, String[]> cpMap = mapClassPath(); 
    Set<String> sourcePaths = newSet();
    
    Iterator<Map.Entry<String, String[]>> it  
      = cpMap.entrySet().iterator(); 
    Map.Entry<String, String[]> crntCpEnt = null; 
    while (it.hasNext()) { 
      crntCpEnt = it.next();
      if (!(crntCpEnt.getValue() instanceof String[])) {
        it.remove();
        continue; 
      }
      String sourcePath = crntCpEnt.getKey(); 
      String[] names = crntCpEnt.getValue(); 
/*->*/Object[] result = StringCollectionUtil.matchingSubset(
        Arrays.asList(names), clsptrns
      );
      if (result.length > 0) {
        sourcePaths.add(sourcePath);
      }
    }
    return sourcePaths;
  }
  
  public static String toString(Object... objects) {
    if (objects == null) return "<NULL>";
    if (objects.length == 1) {
      Object[] array = null;
      if (objects[0].getClass().isArray()) {
        array = (Object[]) objects[0];
      }
      String str = (objects[0] == null) ? "<null>" : ((array != null) ? Arrays.toString(Arrays.copyOfRange(array, 0, array.length > 25 ? 25 : array.length)) : (objects[0].toString()));
      return str.length() > 255 ? str.substring(0, 255) : str;
    }
        // length > 1
    StringBuilder sb = new StringBuilder(objects.length * 16);
    for (int i = 0; i < objects.length; i++) {
      sb.append(String.format("[%d/%d ] = (%s) %s\n", i + 1, objects.length, objects[i ] == null ? "null" : objects[i].getClass().getName(), toString(objects[i])));
    }
    return sb.toString();
  }

  public static String[] searchClassPath(String regex) {
    return searchClassPath(new Pattern[] { 
      Pattern.compile(regex, 2)  
    });
  }
  
  public static String[] searchClassPath(Pattern regex) {
    return searchClassPath(new Pattern[] { regex });
  }
  
  public static Collection<String> flattenValues(
  Map<String, String[]> classpathMap) 
  {
    Collection<String[]> allClasses2d 
      = classpathMap.values();
    int size = 0;
    int idx = 0;
    for (String[] sl: allClasses2d) {
      size += sl.length;
    }
    String[] allStrings = new String[size];
    for (String[] sl: allClasses2d) {
      System.arraycopy(sl, 0, allStrings, idx, sl.length);
      idx += sl.length;
    }
    return Arrays.asList(allStrings);
  }

  public static String[] searchClassPath(Pattern[] regexes) 
  {
    getAllClasspathNames();
    List<String> results = null;
    for (final Pattern ptrn: regexes) {
      List<String> matchArr = CollectionUtil2.filter(
        ClasspathDataHolder.allClasses,
        ptrn
      );
      if (matchArr.size() == 0) {
        continue;
      }
      if (regexes.length == 1) return matchArr.toArray(new String[0]);
      else {
        ((List<String>)
            (results != null ? results : (results = new ArrayList<>()))
        ).addAll(matchArr);
      }
    }
    return (results != null)
      ? results.toArray(new String[0])
      : new String[0];
  }

  public static String[] searchClassPath(String[] regexes) {
    getAllClasspathNames();
    List<String> results = null;
    for (final String regex: regexes) {
      List<String> matchArr = CollectionUtil2.filter(
        ClasspathDataHolder.allClasses,
        regex
      );
      if (matchArr.size() == 0) {
        continue;
      }
      if (regexes.length == 1) return matchArr.toArray(new String[0]);
      else {
        ((List<String>)
            (results != null ? results : (results = new ArrayList<>()))
        ).addAll(matchArr);
      }
    }
    return (results != null)
      ? results.toArray(new String[0])
      : new String[0];
  }
  
  
  private static <E> Collection<E> castOrCopyToCollection(
    final Iterable<E> iterable)
  {
    if (iterable instanceof Collection) {
      return (Collection)iterable;
    }
    return (Collection<E>) Lists.newArrayList(iterable.iterator());
  }
  
  
  public static Collection<URL> getClassLoaderEntries
  (ClassLoader ldr) 
    throws 
    IllegalAccessException, 
    NoSuchMethodException, 
    InvocationTargetException, 
    IOException
  {
    if (isJRE()) {
      if (ldr == Thread.currentThread().getContextClassLoader()) {
        return ClassPathUtil.getAllClasspathEntries();
      }
      return castOrCopyToCollection(new CLoader(ldr, null));
    }
    
    Object resources = null;
    try {
      resources = ldr.getResources("classes.dex");
    } catch (IOException ioEx) {
      System.err.println(
        "ERROR: getClassLoaderEntries:\n  \n  " +
        "Object resources == NULL\n  " +
        "(assigned from ldr.getResources(\"classes.dex\")\n  "
      );
      return Collections.emptyList();
    }
    // resources <: java.util.Collections$3
    if (resources.getClass().isAssignableFrom(e1Cls)) {
      return (Collection<URL>) f1of1.get(resources);
    }
    // resources <: java.lang.TwoEnumerationsInOne
    if (resources.getClass().isAssignableFrom(e2Cls)) {
      Collection<URL> combined = new ArrayList<URL>(2);
      for (Field f_of2 : new Field[] { f1of2, f2of2 }) {
        Object coll = (Object) f_of2.get(resources);
        //Object coll = f1of2.get(o_of2);
        if (coll instanceof Collection) {
          Collection<URL> c_ = (Collection<URL>) coll;
          outer1:
          for (URL entry : c_) {
            if (!isJRE()) {
              for (Matcher mchr: BLACKLIST_MATCHERS) {
                if (mchr.reset(entry.toString()).find()) {
                  if (LOGV) Log.v(TAG, 
                    "Skipping blacklisted jar: [%s]\n", entry
                  );
                  continue outer1;
                }
              }
            }
            combined.add(entry);
          }
        } else if (coll instanceof Enumeration) {
          Enumeration<URL> e_ = (Enumeration<URL>) coll;
          outer2:
          while (e_.hasMoreElements()) {
            URL entry = e_.nextElement();
            if (!isJRE()) {
              for (Matcher mchr: BLACKLIST_MATCHERS) {
                if (mchr.reset(entry.toString()).find()) {
                  if (LOGV) Log.v(TAG, 
                    "Skipping blacklisted jar: [%s]\n", entry
                  );
                  continue outer2;
                }
              }
            }
            // System.out.println(entry);
            PathInfo pi = PathInfo.getPathInfo(entry);
            
            if (SYS_JAR_DIR.equals(pi.dir)
            && Arrays.binarySearch(sysJarsWhitelist, 0,
            sysJarsWhitelist.length, pi.name) <= 0) { 
              System.err.printf("Skip: %s\n", pi.path); 
              continue;
            }
            combined.add(entry);
          }
        } else {
          System.out.println("coll is other:");
          System.out.println(coll.getClass().getName());
        }
      }
      return combined;
    }
    Collection<URL> combined = new ArrayList<URL>();
    outer3:
    for (final URL entry: CollectionUtil.asIterable(
      (Enumeration<URL>)resources))
    {
      PathInfo pi = PathInfo.getPathInfo(entry);
      if (!isJRE() && SYS_JAR_DIR.equals(pi.dir) &&
          Arrays.binarySearch(
            sysJarsWhitelist, 0, sysJarsWhitelist.length, pi.name) <= 0)
      { 
        System.err.printf("Skip: %s\n", pi.path); 
        continue;
      }
      combined.add(entry);
    }
    return combined;
  }
  
  public static Collection<URL> getAllClasspathEntries() { 
    if (isJRE()) {
      return FluentIterable.concat(
        new SunLoader(
          Thread.currentThread().getContextClassLoader(), null
        ),
        new SunLoader(
          Thread.currentThread().getContextClassLoader().getParent(), null
        ),
        new SunLoader(null, null)
      ).toSortedSet(Ordering.usingToString());
    }
    
    return FluentIterable.concat(
      new CLoader(
        Thread.currentThread().getContextClassLoader(), null
      ),
      new CLoader(
         Thread.currentThread().getContextClassLoader().getParent(), null
      )
    ).toSortedSet(Ordering.usingToString());
  }
  
  
  public static Collection<String> getAllClasspathNames() {
    return getAllClasspathNames(null);
  }
  
  public static Collection<String> getAllClasspathNames(ClassLoader loader) 
  {
      try {
        return getAllClasspathNamesInternal(
          (loader != null)
            ? loader
            : Thread.currentThread().getContextClassLoader()
        );
      } catch (final Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
  }
  
  
  public static final Matcher DEX_ELEMENT_PATH_MATCHER
    = Pattern.compile("^[^\"]*\"([^\"]*)\".*$").matcher("");
  public static final String  DEX_ELEMENT_PATH_REPLACE = "$1";
  public static final File FRAMEWORK_JARS_DIR =
    new File("/external_sd/_projects/sdk/framework/");
  
  private static Collection<String> getAllClasspathNamesInternal(ClassLoader ldr) 
    throws 
    IllegalAccessException, 
    NoSuchMethodException, 
    InvocationTargetException, 
    IOException 
  {
    if (classpathMap == null || classpathMap.isEmpty()) {
      Reflect.setfldval(
        ClassPathUtil.class, "classpathMap", new TreeMap<>());
      mapClassPath();
      total = 0;
    } 
    if (ldr == null) ldr = Thread.currentThread().getContextClassLoader();
    int added;
    if (isJRE()) {
      added = mapClassPathJre2();
      return
        Collections.unmodifiableSortedSet(ClasspathDataHolder.allClasses);
    } else {
      added = 0;
      String simpleClassName
        = classpathMap.getClass().getSimpleName();
      Method gcn_m = DexFile.class.getDeclaredMethod(
        "getClassNameList", Integer.TYPE
      );    
      gcn_m.setAccessible(true);
      Object pathList = Reflect.getfldval(ldr, "pathList");
      IOException[] dexExs = Reflect.getfldval(
        pathList, "dexElementsSuppressedExceptions"); 
      Object[] dexElements 
        = Reflect.getfldval(pathList, "dexElements");
      int dexExIdx = 0;
      
      int dexElementCount = dexElements.length;
      int dexElementIndex = -1;
      
     
      while (++dexElementIndex < dexElementCount) {
        
        Object elem = dexElements[dexElementIndex];
        System.err.println(elem);
        
        File file = Reflect.getfldval(elem, "file");
        if (file == null) file = elem != null
          ? new File(elem.toString()) 
          : new File("/dev/null");
        String path = file.getPath();
        if (classpathMap.containsKey(path)) continue;
        if ("/dev/null".equals(path) || file == null) continue;       
        PathInfo pi = PathInfo.getPathInfo(path);
        if (pi == null || pi.path == null) continue; 
        if (classpathMap.containsKey(pi.path)) continue; 
        
        if (SCAN_DEBUG) System.err.printf(
          "Processing element %3d of %-3d: %s\n"
          + "  - path = [%s]\n",
          dexElementIndex + 1,
          dexElementCount,
          elem, path
        );
        if (elem == null) continue;
        
        DexFile df = null;
        String altPath = path;
        if (SCAN_DEBUG) System.err.println("  - Checking blacklist..");
        Label_blacklist_outer:
        for (Matcher mchr: BLACKLIST_MATCHERS) {
          if (mchr.reset(path).find()) {
            if (LOGV) Log.v(TAG, 
              "Skipping blacklisted jar: [%s]\n", path
            );
            continue Label_blacklist_outer;
          }
        }
        if (SCAN_DEBUG) System.err.println("     - Done.");
        
        if (path.indexOf("/system/framework/") != -1) {
          path = DEX_ELEMENT_PATH_MATCHER.reset(path)
            .replaceAll(DEX_ELEMENT_PATH_REPLACE);
          altPath = path.replace(
            "/system/", "/external_sd/_projects/sdk/"
          );
          if (altPath != null && altPath.length() != path.length()) {
            if (SCAN_DEBUG) System.err.printf(
              "  - df = new DexFile(\"%s\");\n", altPath
            );
            try {
              df = new DexFile(altPath);
            } catch (Throwable e) { 
              System.err.printf("[%s] %s\n", e, altPath);
              classpathMap.put(path, new String[0]);
              continue;
            }
            if (SCAN_DEBUG) System.err.println("    - Done.");
          }
        } else {
          Object dfOrString = Reflect.getfldval(elem, "dexFile");
          if (!(dfOrString instanceof DexFile)) {
            continue;
          }
          df = (DexFile) dfOrString;
        }
        
        if (df == null) continue; 
        // for NPE
        if (path == null) {
          System.err.println(
            "WARNING: null (@ variable 'fileName'), read from field "
            + "`filename' on DexFile (@ variable 'df'), "
            + "read from field `dexFile' on DexPathList.Element "
            + "(@ variable 'elem')"
          );
          // System.err.println(
          // "Skipping to next DPE (@ next variable 'elem')");
        }
        if (classpathMap.containsKey(path)) continue;
        Object oCookie = Reflect.getfldval(df, "mCookie");
        if (!(oCookie instanceof Integer)) {
          System.out.println(String.format(
            "%s is invalid:\n  %s", 
            elem.toString(), dexExs[dexExIdx++].getMessage()
          ));
          continue;
        }
        Integer mCookie = (Integer) oCookie;
        String[] classNames 
          = (String[]) gcn_m.invoke(null, mCookie.intValue());
        classpathMap.put(path, classNames);
        total += classNames.length;
        added += classNames.length;
        
        System.err.println(String.format(
          "%s size -> %7d: [%s]", 
          simpleClassName, total, path
        ));
      }
      
      
      // system part    
      String[] paths = getBootClassPathPaths();
      int size = paths.length;
      
      // String[][] classNames = new String[size][];
      
      int i = -1;
      while (++i < size) {
        String systemPath = paths[i];
        if (classpathMap.containsKey(systemPath)) {
          total += classpathMap.get(systemPath).length;
          continue;
        }
        File systemFile = new File(systemPath);
        String fileName = systemFile.getName();
        File extFile = new File(FRAMEWORK_JARS_DIR, fileName);
        if (! extFile.exists()) { 
          new RuntimeException(String.format(
            "Warning: need copy of system dex file at %s\n",
            extFile.getPath()
          )).printStackTrace();
          continue;
        }
        File indexFile = getIndexFileFor(extFile.toURL());
        
        Collection<String> names = null;
        String[] namesArray = null;
        if (indexFile.exists()) {
          // have index; just read it
          if (SCAN_DEBUG) System.err.printf(
            "Using index for [%s] @ [%s]\n", fileName, indexFile
          );
          names = new ArrayList<>(
                (Collection<? extends String>) (Collection<?>)
                FileUtils.readLines(
                  new File(indexFile.getPath()),
                  StandardCharsets.ISO_8859_1
                )
              );
          namesArray = names.toArray(new String[0]);
          if (SCAN_DEBUG) System.err.printf(
            "Number of entries: %d\n", namesArray.length
          );        
        } else { 
          DexUtil dexUtil = new DexUtil(extFile);
          namesArray = dexUtil.getClassNames();
          names = Arrays.asList(namesArray);
        }
        
        if (!indexFile.exists()) {
          // Write index
          try {
            Files.write(
              Paths.get(indexFile.getPath()),
              names,
              StandardCharsets.ISO_8859_1,
              StandardOpenOption.WRITE,
              StandardOpenOption.CREATE_NEW,
              StandardOpenOption.TRUNCATE_EXISTING
            );
          } catch (IOException ioe) {
            System.err.println(ioe);
          }          
          if (SCAN_DEBUG) System.err.printf(
            "Wrote index: [%s]\n", indexFile.getPath()
          );
        }
        
        // classNames[i] = names;
        classpathMap.put(systemPath, namesArray);
        total += namesArray.length;
        added += namesArray.length;
        System.err.println(String.format(
          "%s size -> %7d: [%s] <via getBootClassPathPaths()>", 
          simpleClassName, total, systemPath
        ));
      } // end loop from 0 to size-1 (bootclasspath paths)
    }
    
    // Collect all
    if (added > 0) ClasspathDataHolder.update(classpathMap);
    return Collections.unmodifiableSortedSet(
      ClasspathDataHolder.allClasses
    );
  }
  
  static Class<?> c_VMClassLoader;
  static Method getBootClassPathSize;
  static Method getBootClassPathResource;
  static {
    if (!isJRE()) {
      c_VMClassLoader = DexVisitor.classForName("java.lang.VMClassLoader");
      getBootClassPathSize = Reflect.findMethod(
        c_VMClassLoader, "getBootClassPathSize", new Class<?>[0]
      );
      getBootClassPathResource = Reflect.findMethod(
        c_VMClassLoader, "getBootClassPathResource", String.class, int.class
      );
    }
  }
  
  public static String[] getBootClassPathPaths() {
    try {
      if (isJRE()) {
        return StringUtils.split(
          System.getProperty("sun.boot.class.path", ""), ":"
        );
      }
      
      int size 
        = ((Integer) getBootClassPathSize.invoke(null)).intValue();
      String[] paths = new String[size];
      int i = -1;
      while (++i < size) {
        paths[i] = PathInfo.getPathInfo(
          (String) getBootClassPathResource.invoke(
            null, "classes.dex", i
          )
        ).path;
      }
      return paths;
    } catch (ReflectiveOperationException ex) {
      ex.printStackTrace();
    }
    return new String[0];
  }
  
  
  
  public static class ClassList<U>
  extends java.util.ArrayList<U>
  {
    private final boolean immutableEmpty;
    public static final ClassList<?> EMPTY = new ClassList<Object>(0, true);
    private static final Class<?>[] NO_CLASS_ARRAY = { };
    
    public ClassList() {
      this(64);
    }
    public ClassList(final int capacity) {
      super(capacity);
      this.immutableEmpty = capacity == 0;
    }
    private ClassList(final int capacity, final boolean immutableEmpty) {
      super(capacity);
      this.immutableEmpty = immutableEmpty;
    }
  }
  
  public static ClassList<?> getImpls
  (Class<?>[] clzs, String tgt)
  {
    Class<?> tgtClass = null;
    List<? super Class<?>> matches = new ClassList<>(
      (int) Math.ceil(clzs.length / 7.5) + 1
    );
    
    try {
      tgtClass = Thread.currentThread().getContextClassLoader()
        .loadClass(tgt);   
      } catch (ClassNotFoundException e) {
      System.err.printf(
        "%s loading %s: [%s]\n",
        e.getClass().getSimpleName(),
        tgt.toString(),
        e.getMessage() != null? e.getMessage(): "<null>"
      );
      return (ClassList) matches;
    }
    int i = 0;
    for (; i<clzs.length; i++) {
      if (tgtClass.isAssignableFrom(clzs[i])) {
        matches.add(clzs[i]);
      }
      if (i % 100 == 0) {
        System.err.printf(
         "\u001b[1;30m[%3d of %3d] Processing %s ...\u001b[0m\n",
          i, clzs.length,
          clzs[i].toString()
        );
      }
    }
    return (ClassList) matches;
  }
  
  public static ClassList getImpls(String regex, String tgt) {
    Class<?>[] clzs = searchClassPath.invoke(
      null, null, null, new String[]{ regex }
    );
    return getImpls(clzs, tgt);
  }
  
  static Matcher EXPAND_MCHR
    = Pattern.compile("([^a-zA-Z0-9_])(com.sun|javax?)([^a-zA-Z0-9_])").matcher("");
  static String  EXPAND_REPLACEMENT
    = "$1(?:javax?|(?:com\\.)?sun)$3";
  
  private static String expandPattern(final String orig, boolean expand) {
    if (!expand) return orig;
    return EXPAND_MCHR.reset(orig).replaceAll(EXPAND_REPLACEMENT);
  }
  
  public static ClassList getImpls(final String iface) {
    return getImpls(
      iface,
      false
    );
  }
  
  public static ClassList getImpls(final String iface, final boolean expand)
  {
    final String[] parts = StringUtils.split(iface, ".$/");
    
    switch (parts.length) {
      case 0:  return ClassList.EMPTY;
      case 1: 
      case 2:  return getImpls(
        expandPattern(
          format(
            "^%s[.$]?",
            parts[0].replaceAll("\\$.*$", ".*?")
          ),
          expand
        ),
        iface
      );
      case 3:  return getImpls(
        expandPattern(
          format(
            "^%s[.$]",
            parts[0].replaceAll("[\\$.]", ".?")
          ),
          expand
        ),
        iface
      );
      default:  return getImpls(
        expandPattern(
          format(
            "^%s[.$]?%s", 
            parts[0].replaceAll("[\\$.]", ".?"),
            parts[1].replaceAll("\\$.*$", "")
          ),
          expand
        ),
        iface
      );
    }
  }
  
  public static String[] toNiceNames(Collection<?> names) {
    return toNiceNames(names.toArray());
  }
  
  private static final SortedSet<String> _tempSortedSet = new TreeSet<>();
  
  public static String[] toNiceNames(Object[] _names) {
    final Set<String> names = _tempSortedSet;
    names.clear();
    for (final Object o: _names) {
      names.add(ClassInfo.typeToName(o));
    }
    return names.toArray(new String[0]);
  } 
  
  
  
  
}