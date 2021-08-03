package org.d6r;

import bsh.NameSpace;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import java.io.File;
import java.io.IOException;
import java.lang.Object;
import java.lang.Runtime;
import java.lang.String;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.util.MutableBoolean;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflect.setfldval;


public class MultiDexUtil {
  
  static final LazyMember<Method> OPENDEXFILE_NATIVE = LazyMember.of(
    "dalvik.system.DexFile", "openDexFileNative",
    new Class[]{ String.class, String.class, Integer.TYPE }
  );
  static final LazyMember<Constructor<?>> DEXELEMENT_CTOR = LazyMember.of(
    "dalvik.system.DexPathList$Element", "<init>",
    new Class[]{ File.class, Boolean.TYPE, File.class, DexFile.class }
  );
  static final LazyMember<Field> BSHCAP_CLZS = LazyMember.of(
    "classes", "bsh.Capabilities"
  );
  
  static final Set<Object> loadedDexElements = new HashSet<>();
  static final Set<String> loadedClassNames = new TreeSet<>();
  
  static final Matcher STRIP_MCHR = Pattern.compile("([^a-zA-Z0-9-])").matcher("");
  static final Matcher SECONDARY_DEX_MCHR
    = Pattern.compile("^classes[1-9][0-9]*.dex$").matcher("");
  static final File MULTIDEX_CACHE_DIR
    = new File(new File(System.getProperty("java.io.tmpdir")), "tmp_multidex");
  
  
  public static Map<String, Pair<ZipEntry, Object>> load(Class<?> cls) {
    final Map<String, Pair<ZipEntry, Object>> ok = new TreeMap<>();
    JarFile zf = null;
    try {
      zf = ((JarURLConnection) 
        NameSpace.getClassResource(cls).openConnection()).getJarFile();
      Reflect.setfldval(zf, "guard", null);
      Reflect.setfldval(Reflect.getfldval(zf, "raf"), "guard", null);
      List<String> entryNames = CollectionUtil2.filter(
        ((LinkedHashMap<String,ZipEntry>) 
          Reflect.getfldval(zf, "entries")).keySet(),
        SECONDARY_DEX_MCHR
      );
      for (final String entryName : entryNames) {
        final String fileName = entryName;
        final ZipEntry ze = zf.getEntry(entryName);
        System.err.printf(
          "Preparing secondary dex \"%s\" from ZipFile[%s] ...\n",
          entryName, zf.getName()
        );
        final String encodedName = generateEncodedNameForEntry(ze);
        final File cachedDexFile = new File(
          new File(
            new File(MULTIDEX_CACHE_DIR, stripNonAlphanumeric(entryName)), 
            encodedName
          ), entryName
        );
        long bytesWritten;
        if (!cachedDexFile.exists()) {
          final File dir = cachedDexFile.getParentFile();
          if (!dir.exists() && !dir.mkdirs()) {
            Reflector.invokeOrDefault(dir, "mkdirErrno");
          }
          try (final InputStream is = zf.getInputStream(ze);
               final FileOutputStream fos = new FileOutputStream(cachedDexFile);
               final OutputStream os = new BufferedOutputStream(fos))
          {
            bytesWritten = IOUtils.copyLarge(is, os);
            if (bytesWritten != ze.getSize()) {
              throw new IllegalStateException(String.format(
                "Bytes written to File[%s] (%d) != Size in ZipEntry[%s] (%d); " +
                "ZipFile[%s]",
                cachedDexFile.getPath(), bytesWritten, ze.getName(), ze.getSize(),
                zf.getName()
              ));
            }
          }
          System.err.printf(
            "Wrote '%s' (%d bytes) for ZipEntry[%s]\n",
            cachedDexFile.getPath(), bytesWritten, entryName
          );
        } else {
          System.err.printf(
            "Reusing existing object '%s' (%d bytes) for ZipEntry[%s]\n",
            cachedDexFile.getPath(), cachedDexFile.length(), entryName
          );
        }
        final Object dexElement = appendClassPathFile(
          cachedDexFile.getAbsolutePath(), cls.getClassLoader()
        );
        final DexFile dexFile = ClassPathUtil2.getDexFile(dexElement);
        loadedDexElements.add(dexElement);
        loadedClassNames.add(cls.getName());
        ok.put(
          String.format("%s!/%s", zf.getName(), entryName),
          Pair.of(ze, dexElement)
        );
      }
    } catch (Throwable e) { 
      e.printStackTrace();
    } finally {
    }
    return ok;
  }
  
  public static String generateEncodedNameForEntry(final ZipEntry ze) {
    byte[] data = new byte[80];
    ByteBuffer bb = ByteBuffer.wrap(data);
    bb.putLong(ze.getCrc());
    bb.putLong(ze.getTime());
    bb.putLong(ze.getSize());
    String entryName = ze.getName();
    final byte[] utfBytes = entryName.getBytes(StandardCharsets.UTF_8);
    if (utfBytes != null) {
    bb.put(utfBytes, 0, Math.min(utfBytes.length, bb.remaining()));
    }
    if (bb.remaining() != 0) {
    final byte[] extra = ze.getExtra();
    if (extra != null)
      bb.put(extra, 0, Math.min(extra.length, bb.remaining()));
    }
    String ub64 = Base64.encode(data).replace('=', '-').replace('/', '_');
    return ub64;
  }
  
  public static String stripNonAlphanumeric(String entryName) {
    return STRIP_MCHR.reset(entryName).replaceAll("_");
  }
  
  
  
  public static Object appendClassPathFile(String dexOrZipPath, ClassLoader ldr) {
    if (dexOrZipPath == null || dexOrZipPath.length() == 0) {
      final RuntimeException iae = new IllegalArgumentException(String.format(
        "appendClassPathFile: dexOrZipPath == %s",
        dexOrZipPath == null? "null": String.format("\"%s\"",dexOrZipPath)
      ));
      Log.e("appendClassPathFile", iae);
      iae.printStackTrace();
      throw iae;
    }
    final File file = new File(dexOrZipPath);
    if (! file.exists() || file.isDirectory() || ! file.canRead()) {
      final RuntimeException iae = new IllegalArgumentException(String.format(
        "appendClassPathFile: dexOrZipPath \"%s\": %s",
        dexOrZipPath, (file.exists())
          ? (file.isDirectory()? "Is a directory": "Cannot read"): "Does not exist"
      ));
      Log.e("appendClassPathFile", iae);
      iae.printStackTrace();
      throw iae;
    }
    final int cookie;
    {
      Integer _cookie;
      try {
        _cookie = OPENDEXFILE_NATIVE.invoke(null, file.getAbsolutePath(), null, 0);
        if (_cookie == null) throw new RuntimeException(String.format(
          "Received null result (cookie) from %s",
          OPENDEXFILE_NATIVE.get().toGenericString()
        ));
      } catch (final Exception exc) {
        Throwable e = exc;
        while (e instanceof InvocationTargetException) {
          e = ((InvocationTargetException) e).getTargetException();
        }
        Throwable cause;
        while (RuntimeException.class == e.getClass() && e.getMessage() == null &&
              (cause = e.getCause()) != e && cause != null)
        {
          e = cause;
        }
        final RuntimeException iae = new IllegalArgumentException(String.format(
          "appendClassPathFile: Dex is invalid: \"%s\": %s", file.getPath(), e
        ));
        Log.w("appendClassPathFile", iae);
        iae.printStackTrace();
        return null;
      }
      if (_cookie == null) throw new AssertionError("nonnull var _cookie is null");
      cookie = _cookie.intValue();
    }
    final DexFile df = Reflect.allocateInstance(DexFile.class);
    setfldval(df, "mFileName", file.getPath());
    setfldval(df, "mCookie", cookie);
    final Object[] args = {
      file.getAbsoluteFile(),
      file.isDirectory(),
      ((!file.isDirectory() && !StringUtils.endsWith(file.getName(), "dex"))
        ? file.getAbsoluteFile(): null),
      df
    };
    final Object dexElement = DEXELEMENT_CTOR.newInstance(args);
    if (dexElement == null) throw new AssertionError(String.format(
      "dexElement == null; args: %s; ctor: %s",
      Debug.ToString(args), DEXELEMENT_CTOR.get().toGenericString()
    ));
    final Object pathList = getfldval(ldr, "pathList");
    final Object[] origDexElements = getfldval(pathList, "dexElements");
    
    setfldval(pathList, "dexElements", ArrayUtils.addAll(
      // NOTE: Arrays both have type DexPathList.Element[] at runtime
      (Object[]) origDexElements,
      (Object[]) CollectionUtil.toArray(Arrays.asList(dexElement))
    ));
    
    // Clear various classpath-related state
    try {
      final Object badClasses = getfldval(ldr, "badClasses", false);
      if (badClasses != null) Reflector.invokeOrDefault(badClasses, "clear");
      final Object interp = CollectionUtil.getInterpreter();
      if (interp != null) {
        final Object nonClasses = Reflect.get(interp, "bcm", "absoluteNonClasses");
        if (nonClasses != null) ((Collection<?>) nonClasses).clear();
        final MutableBoolean success = new MutableBoolean(false);
        BSHCAP_CLZS.tryGet(success);
        if (success.value) {
          final Object clazzes = BSHCAP_CLZS.getValue(null);
          if (clazzes instanceof Map<?, ?>) ((Map<?, ?>) clazzes).clear();
          if (clazzes instanceof Collection<?>) ((Collection<?>) clazzes).clear();
        }
      }
    } catch (RuntimeException rex) {
      throw rex;
    } catch (Exception ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
    return dexElement;
  }
}