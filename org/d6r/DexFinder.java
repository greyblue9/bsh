package org.d6r;

import com.android.dex.Dex;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.DexVisitor;
import org.d6r.LazyMember;
import static org.d6r.LazyMember.of;
import org.d6r.Range;
import org.d6r.UnsafeUtil;
import org.apache.commons.io.FilenameUtils;


public class DexFinder {
  static final String TAG = "DexFinder";
  static final String CLASSNAME_MEMBLK = "java.nio.MemoryBlock";
  static final LazyMember<Field> FLD_DEX_DATA = of("data", Dex.class);
  static final boolean JRE = CollectionUtil.isJRE();
  
  static final LazyMember<Field> FLD_BUF_BLOCK =
    JRE? null: of("block", Buffer.class);
  static final LazyMember<Field> FLD_BLOCK_ADDR =
    JRE? null: of(CLASSNAME_MEMBLK, "address");
  static final LazyMember<Field> FLD_BLOCK_SIZE =
    JRE? null: of(CLASSNAME_MEMBLK, "size");
  
  static final Matcher INFO_MCHR = Pattern.compile(
    "^([a-z- ]{4}) ([0-9]{8}) ([0-9a-f]{1,}:[0-9a-f]{1,}) ([1-9][0-9]*) +" +
    "/data/dalvik-cache/([^\n]+)"
  ).matcher("");
  static final Matcher INFO_MCHR_ODEX = Pattern.compile(
    "^([a-z- ]{4}) ([0-9]{8}) ([0-9a-f]{1,}:[0-9a-f]{1,}) ([1-9][0-9]*) +" +
    "(/(?:data|system|cache|tm|external_sd)/[^\n]*)/([^\n/]+\\.o?dex)"
  ).matcher("");
  
 public static void ensureInitialized() throws ReflectiveOperationException {
   Class.forName(DexFinder.class.getName(), true, DexFinder.class.getClassLoader());
 }
 
 public static Pair<File, String> findDexPair(Dex dex) {
    if (JRE) return null;
    final Object data = FLD_DEX_DATA.getValue(dex);
    final Object block = FLD_BUF_BLOCK.getValue(data);
    final long start = FLD_BLOCK_ADDR.longValue(block);
    final long size = FLD_BLOCK_SIZE.longValue(block);
    final long end = start + size;
    
    final Range<Long> range = Range.closed(start, start);
    final Range<Long> alloc = UnsafeUtil.getMemoryRanges().ceiling(range);
    final String info = UnsafeUtil.memInfo.get(alloc);
    
    if (INFO_MCHR.reset(info).find()) {
      String TAG = "DexFinder";
      final String cacheFileName = INFO_MCHR.group(5);

      final String raw = cacheFileName.replace('@', '/');
      final String path = "/".concat(StringUtils.substringBeforeLast(raw, "/"));
      final String entryOrFileName = StringUtils.substringAfterLast(raw, "/");
      final File fileOrDir = new File(path);
      if (fileOrDir.exists()) {
        final boolean isJar = fileOrDir.isFile();
        final File file = isJar? fileOrDir: new File(fileOrDir, entryOrFileName);
        final String entryName = isJar? entryOrFileName: "";
        return Pair.of(file, entryName);
      } else {
        Log.i(
          TAG,
          String.format(
            "Cache miss; info: \"`s`\", cacheFileName: \"%s\", fileOrDir: File(%s)",
            info, cacheFileName, fileOrDir.getPath()
          )
        );
      }
    }
    
    if (INFO_MCHR_ODEX.reset(info).find()) {
      final File dir = new File(INFO_MCHR_ODEX.group(5));
      final String odexFileName = INFO_MCHR_ODEX.group(6);
      final File odexFile = new File(dir, odexFileName);
      if (! odexFile.exists()) return null;
      final String name = FilenameUtils.removeExtension(odexFileName);
      final File maybeDexFile = new File(dir, name.concat(".dex"));
      final File maybeJarFile = new File(dir, name.concat(".jar"));
      final File maybeApkFile = new File(dir, name.concat(".apk"));
      final File file;
      final String entryName;
      if (maybeDexFile.exists()) {
        file = maybeDexFile;
        entryName = "";
      } else {
        if (maybeJarFile.exists() || maybeApkFile.exists()) {
          file = (maybeJarFile.exists()) ? maybeJarFile : maybeApkFile;
          entryName = "classes.dex";
        } else {
          file = odexFile;
          entryName = "";
        }
      }
      return Pair.of(file, entryName);
    }
    
    return null;
  }
  
  public static Pair<File, String> findDexPair(Class<?> cls) {
    if (JRE) return null;
    return findDexPair(getDex(cls));
  }
  
  public static Pair<File, String> findDexPair(String className) {
    if (JRE) return null;
    final Class<?> cls = DexVisitor.classForName(className);
    if (cls == null) return null;
    return findDexPair(cls);
  }
  
  public static Object findDexElement(Object o) {
    if (JRE) return null;
    final Class<?> cls = dumpMembers.getClass(o);
    if (cls != null) {
      Object _pair = null, _file = null, _dexElement = null, _classLoader = null;
      try {
        final Pair<File, String> pair = DexFinder.findDexPair(cls);
        _pair = pair;
        if (pair != null) {
          final File file = pair.getKey();
          _file = file;
          final Object dexElement = ClassPathUtil2.findDexElement(
            (ClassLoader) (_classLoader = cls.getClassLoader()),
            file
          );
          if ((_dexElement = dexElement) != null) return dexElement;
        }
      } catch (Throwable t) { 
        throw (Error) new InternalError(String.format(
          "DexFinder.findDexElement(o: (%s) %s): Error getting dex pair (" +
          "pair: %s, file: %s, classLoader: %s, dexElement: %s" +
          "): %s",
          ClassInfo.typeToName(o), Dumper.tryToString(o),
          _pair, _file, _classLoader, _dexElement, t
        )).initCause(t);
      }
    }
    final Iterator<String> it
      = ClassPathUtil.findClassSource(cls.getName()).iterator();
    return (it.hasNext())
      ? ClassPathUtil2.findDexElement(
          Thread.currentThread().getContextClassLoader(),
          new File(it.next()).getAbsoluteFile()
        )
      : null;
  }
  
}





