package org.d6r;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.d6r.IOStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.writer.ClassSection;
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.FileDataStore;

import java.lang.reflect.Method;

import org.jf.dexlib2.iface.ClassDef;
//import org.jf.dexlib2.writer.pool.PoolClassDef;
import org.jf.dexlib2.writer.pool.ClassPool;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.*;

import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.*;
import org.jf.dexlib2.writer.pool.*;
import org.jf.dexlib2.Opcodes;
import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import java.util.zip.*;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import android.os.Environment;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.*;

// import com.google.common.base.Supplier;
// import com.strobel.functions.Supplier;
// import java.util.function.Consumer;
// import java.util.function.Supplier;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import org.apache.commons.collections4.functors.CatchAndRethrowClosure;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.jf.dexlib2.dexbacked.raw.RawDexFile;
import org.apache.commons.collections4.map.ListOrderedMap;

import static org.apache.commons.collections4.IterableUtils.*;

import static org.d6r.CollectionUtil2.*;
import static org.d6r.CollectionUtil.*;
import java.util.regex.*;
import static org.d6r.Reflect.*;


class MultiDexBackedDexFile<DF extends DexFile>
  implements MultiDexContainer<DF>, DexFile
{
  static Opcodes DEFAULT_OPCODES = DexRemix.getOpcodes();
  
  final Opcodes _opcodes;
  final ListOrderedMap<String, DF> _dexEnts;
  
  static <X> X as(final Object o) {
    return (X) o;
  }
  
  public MultiDexBackedDexFile(
    @Nonnull  final Map<String, ? extends DF> dexEntryMap,
    @Nullable final Opcodes opcodes)
  {
    if (dexEntryMap == null) throw new IllegalArgumentException();
    this._opcodes = opcodes != null ? opcodes : DEFAULT_OPCODES; 
    this._dexEnts = as(ListOrderedMap.listOrderedMap(dexEntryMap));
  }
  
  public MultiDexBackedDexFile(
    @Nonnull final Map<String, ? extends DF> dexEntryMap)
  {
    this(dexEntryMap, (Opcodes) null);
  }
  
  @Nonnull
  @Override
  public List<String> getDexEntryNames() {
    return Arrays.asList(_dexEnts.keySet().toArray(new String[0]));
  }
  
  @Nullable
  @Override
  public DF getEntry(@Nonnull final String entryName) {
    return _dexEnts.get(entryName);
  }
  
  @Nonnull
  @Override
  public Opcodes getOpcodes() {
    return DexRemix.getOpcodes();
  }
  
  @Override
  public Set<? extends ClassDef> getClasses() {
    final Set<Object> s = new LinkedHashSet<>();
    for (final DF df: _dexEnts.valueList()) {
      final Set<? extends ClassDef> allClassDefs = df.getClasses();
      s.addAll(allClassDefs);
    }
    return as(s);
  }
} 


public class DexRemix {
  
  public static final String TAG = DexRemix.class.getSimpleName();
  
  public static MultiDexContainer<DexBackedDexFile> getDexBackedDexFile(String filePath) {
    if (cache.containsKey(filePath)) return cache.get(filePath);
    try (final FileInputStream fis = new FileInputStream(new File(filePath))) {
      cache.put(filePath, getDexBackedDexFile(new BufferedInputStream(fis), filePath));
      return cache.get(filePath);
    } catch (Throwable t) {
      if (t instanceof RuntimeException) throw (RuntimeException) t;
      else throw new RuntimeException(t);
    }
  }
  
  public static byte[] writeDex(final DexPool dexPool) {
    final MemoryDataStore memoryStore = new MemoryDataStore(0x8000);
    final byte[] dexBytes;
    try {
      dexPool.writeTo(memoryStore);
      dexBytes = IOUtils.toByteArray(memoryStore.readAt(0));
    } catch (final Throwable tx) {
      throw Reflector.Util.sneakyThrow(tx);
    } finally {   
    }
    return dexBytes;
  }
  
  /**
  public static byte[] extractFromDex(final String[] classNames,
    final InputStream... dexFileStreams)
  {
    final MemoryDataStore memoryStore = new MemoryDataStore(0x8000);
    final byte[] dexBytes;
    
    try {
      dexPool.writeTo(memoryStore);
    } catch (final Throwable tx) { throw Reflector.Util.sneakyThrow(tx); } finally {
      dexBytes = IOUtils.toByteArray(memoryStore.readAt(0));
    }
    return dexBytes;
  }
  */
  
  
  public static MultiDexContainer<DexBackedDexFile> getDexBackedDexFile(
    final InputStream dexOrZipStream)
  {
    try {
      final FileDescriptor fd
        = CollectionUtil.firstOrDefault(ObjectUtil.searchObject(
            dexOrZipStream, FileDescriptor.class, false, 0, 4
          ));
      if (fd != null && PosixFileInputStream.getInt(fd) > 2) {
        final String path = PosixFileInputStream.getPath(fd);
        if (path != null && new File(path).exists()) {
          return getDexBackedDexFile(dexOrZipStream, path);
        }
      }
    } catch (Throwable e) {
      System.err.println(e);
    }
    return getDexBackedDexFile(dexOrZipStream, null);
  }
  
  public static MultiDexContainer<DexBackedDexFile> getDexBackedDexFile(
    final byte[] dexBytes)
  {
    try (final InputStream dexIn = new ByteArrayInputStream(dexBytes)) {
      return getDexBackedDexFile(dexIn);
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  public static MultiDexContainer<DexBackedDexFile> getDexBackedDexFile(
    final InputStream dexOrZipStream,
    final @Nullable CharSequence pathForExceptionMessage)
  {
    return getDexBackedDexFile(dexOrZipStream, pathForExceptionMessage, false);
  }
    
  public static MultiDexContainer<DexBackedDexFile> getDexBackedDexFile(
    final InputStream dexOrZipStream,
    final @Nullable CharSequence pathForExceptionMessage,
    final boolean allowJarToDex)
  {
    try (final InputStream is = new BufferedInputStream(dexOrZipStream)) {
      is.mark(4);
      int byte0 = is.read();
      is.reset();
      
      if (byte0 < 0) throw new IllegalStateException(String.format(
        "%s@%08x.read() for byte 0 of [%s] %s", 
        is == null ? "InputStream" : is.getClass().getSimpleName(),
        is == null ? 0 : System.identityHashCode(is),
        dexOrZipStream, 
        (byte0 == -2)
          ? "did not return (or was never called)" 
          : String.format("returned %d", byte0)
      ));
      boolean isZip = (byte0 == (int) 'P');
      
      
      final BufferedInputStream bis = new BufferedInputStream(is);
      final Map<String, ? super DexBackedDexFile> dexEnts
          = ListOrderedMap.listOrderedMap(new HashMap<>());
     
      if (isZip) {
        byte[] entBytes = null;
        int dexEntryNum = 0;
        while (true) 
        {
          String entryName = (++dexEntryNum  == 1)
            ? "classes.dex"
            : String.format("classes%d.dex", dexEntryNum);
          bis.mark(Integer.MAX_VALUE);
          try {
          try (final InputStream csis = new CloseShieldInputStream(bis)) {
            entBytes = (pathForExceptionMessage != null)
              ? ZipUtil.toByteArray(
                  pathForExceptionMessage.toString(), entryName
                )
              : ZipUtil.toByteArray(csis, entryName);
            if (entBytes == null) break;
            dexEnts.put(
              entryName,
              new RawDexFile(
                DexRemix.getOpcodes(), entBytes
              )
            );
            System.err.printf(
              "read dex entry: [%s] (%d bytes)\n",
              entryName, entBytes.length
            );
          } catch (final Exception ex) {
            ex.printStackTrace();
          } catch (final Throwable tx) {
            tx.printStackTrace();
          }
          } finally {
            bis.reset();
          }
        };
      } else {
        dexEnts.put(
          "classes.dex",
          DexBackedDexFile.fromInputStream(DexRemix.getOpcodes(), bis)
        );
      }
      
      return new MultiDexBackedDexFile<DexBackedDexFile>(
        (Map<String, DexBackedDexFile>) (Object) dexEnts
      );
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }

  public static Opcodes getOpcodes() {
    try {
      Method[] forApiMethods = CollectionUtil2.filter(
        Opcodes.class.getDeclaredMethods(),
        Pattern.compile(
          "^public static org.jf.dexlib2.Opcodes ([^ ]*\\.)?forApi\\("
        )
      );
      Map<Class<?>, Object> argMap = new java.util.IdentityHashMap<>();
      argMap.put(Integer.TYPE, 19); // api
      argMap.put(Boolean.TYPE, Boolean.valueOf(true)); // experimental
      
      Method bestMethod = null;  
      int bestNumMatches = -1;  
      Object[] bestArgs = null;
      
      nextOverload:
      for (Method mtd: forApiMethods) {
        int numMatches = 0;
        List<Object> args = new ArrayList<>();
       Class<?>[] pTypes = mtd.getParameterTypes();
        for (Class<?> pType: pTypes) {
          if (argMap.containsKey(pType)) {
            numMatches++;
            args.add(argMap.get(pType));
          } else {
            continue nextOverload;
          }
        }
        if (numMatches > bestNumMatches) {
          bestNumMatches = numMatches;
          bestArgs = args.toArray();
          bestMethod = mtd;
        }
      }
      if (bestMethod == null) throw new RuntimeException(String.format(
        "No matching overload of 'org.jf.dexlib2.Opcodes#forApi(..)' "
        + "could be found for the arguments %s", argMap
      ));
      // System.err.printf("Best overload is: %s\n", bestMethod);      
      Opcodes opcodes = null;
      try {
        bestMethod.setAccessible(true);
        Object ret = bestMethod.invoke(null, (Object[]) bestArgs);
        if (!(ret instanceof Opcodes)) {
          throw new IllegalArgumentException(String.format(
            "forApi did not return an instance of '%s'!",
            Opcodes.class.getName()
          ));
        }
        opcodes = (Opcodes) ret;
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(
          "Opcodes could not be acquired", ex);
      }
      return opcodes;
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    }    
  }
  
  public static DexPool newDexPool() {
    try {
      Opcodes opcodes = getOpcodes();
      StringPool stringPool = new StringPool();
      TypePool typePool = new TypePool(stringPool);
      TypeListPool typeListPool = new TypeListPool(typePool);
      ProtoPool protoPool 
        = new ProtoPool(stringPool, typePool, typeListPool);
      FieldPool fieldPool
        = new FieldPool(stringPool, typePool);
      MethodPool methodPool 
        = new MethodPool(stringPool, typePool, protoPool);
      AnnotationPool annotationPool = new AnnotationPool(
        stringPool, typePool, fieldPool, methodPool
      );
      AnnotationSetPool annotationSetPool
        = new AnnotationSetPool(annotationPool);
      ClassPool classPool = new ClassPool(
        stringPool, typePool, fieldPool, methodPool,
        annotationSetPool, typeListPool
      );
      DexPool dexPool = Reflect.newInstance(
        DexPool.class,
        opcodes, stringPool, typePool, protoPool, fieldPool,
        methodPool, classPool, typeListPool, annotationPool,
        annotationSetPool
      );
      return dexPool;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }    
  }
  
  public static byte[] peekFile(String filePath, int offset, int count)
  {
    if (CollectionUtil.isJRE()) {
      try {
        FileInputStream fis = new FileInputStream(new File(filePath));
        byte[] buffer = new byte[count];
        IOUtils.readFully(fis, buffer, 0, buffer.length);
        IOUtils.closeQuietly(fis);
        return buffer;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
    
    FileDescriptor fd
      = PosixUtil.open(filePath, PosixUtil.O_RDONLY, 0);
    byte[] buffer = new byte[count];
    try {
      PosixUtil.pread(fd, buffer, 0, count, offset);
    } catch (final Throwable tx) { throw Reflector.Util.sneakyThrow(tx); } finally {
      PosixUtil.close(fd);
    }
    return buffer;
  }
  
  public static boolean isZip(String filePath) {
    byte[] magic = peekFile(filePath, 0, 4);
    return( magic[0] == (byte) 0x50
        &&  magic[1] == (byte) 0x4B
        &&  magic[2] == (byte) 0x03
        &&  magic[3] == (byte) 0x04 );
  }
  
  public static boolean isDex(String filePath) {
    byte[] magic = peekFile(filePath, 0, 8);
    return( magic[0] == (byte) 'd' 
        &&  magic[1] == (byte) 'e'
        &&  magic[2] == (byte) 'x'
        &&  magic[3] == (byte) 0x0A
        &&  magic[4] == (byte) '0'
        &&  magic[5] == (byte) '3'
        &&  magic[7] == (byte) 0x00 );
  }
  
  static DexPool lastPool;
  
  static <T> ArrayList<T> toList(final Collection<?> from) {
    final Collection<?> c = (from != null)
      ? from
      : Collections.emptyList();
    return new ArrayList<T>((Collection<? extends T>) c);
  }
  
  static <T extends ClassDef> List<T> classDefs(@Nullable DexFile df) 
  {
    final Collection<?> c = (df != null)
      ? df.getClasses()
      : Collections.emptyList();
    return new ArrayList<T>((Collection<? extends T>) c);
  }
  
  
  public static byte[] remixDex(@Nullable DexBackedDexFile removeFrom,
  @Nullable DexBackedDexFile addFrom, Matcher classDefMatcher)
  {
    try {
      Collection<ClassDef> baseDexClassDefs = classDefs(removeFrom);
      Collection<ClassDef> sourceDexClassDefs = classDefs(addFrom);
      
      Collection<ClassDef> classDefsToRemove
        = CollectionUtil2.filter(baseDexClassDefs, classDefMatcher);
      Collection<ClassDef> classDefsToAdd
        = CollectionUtil2.filter(sourceDexClassDefs, classDefMatcher);
      
      List<ClassDef> finalClassDefs = toList(baseDexClassDefs);
      finalClassDefs.removeAll(classDefsToRemove);
      finalClassDefs.addAll(classDefsToAdd);
      Collections.sort(finalClassDefs, new ToStringComparator());
      
      DexPool pool = newDexPool();
      lastPool = pool;
      
      ClassPool classSection
        = Reflect.getfldval(pool, "classSection");
      
      int i = -1, total = finalClassDefs.size();
      for (ClassDef classDef: finalClassDefs) {
        System.err.printf(
          "Adding %4d / %4d: %s ...\n", 
          (++i) + 1, total, classDef
        );
        // org.jf.dexlib2.writer.builder.BuilderClassDef
        //  .internClass()
        // org.jf.dexlib2.writer.builder.BuilderClassDef classDef
        Reflector.invokeOrDefault(
          classSection,
          "intern",
          classDef
        );
        // classSection.internClass(classDef);
      }
      
      byte[] dexResultBytes = writeDex(pool);
      return dexResultBytes;
    } catch (Throwable t) {
      t.printStackTrace();
      throw Reflector.Util.sneakyThrow(t);
    }
  }
  
  public static class NewDex
    extends AbstractCollection<ClassDef>
    implements
      AutoCloseable,
      Supplier<byte[]>
  {
    protected final CatchAndRethrowClosure<ClassDef> INTERNER =
      new CatchAndRethrowClosure<ClassDef>() {
        private Method CLASSDEF_INTERN_MTD;
        private boolean _maybeCanDirectlyCallIntern = true;
        
        @Override
        public void executeAndThrow(final ClassDef c) throws Throwable {
          if (_maybeCanDirectlyCallIntern) {
            try {
              section.intern(c);
            } catch (final IllegalAccessError | NoSuchMethodError e) {
              _maybeCanDirectlyCallIntern = false;
              try {
                CLASSDEF_INTERN_MTD =
                  Reflect.findMethod(section.getClass(), "intern");
              } catch (final Throwable ignore) {
              }
              if (CLASSDEF_INTERN_MTD == null) {
                CLASSDEF_INTERN_MTD =
                  Reflect.findMethod(section.getClass(), "internClass");
              }
              CLASSDEF_INTERN_MTD.invoke(section, c);
            }
          } else {
            CLASSDEF_INTERN_MTD.invoke(section, c);
          }
        }
      };
    
    @Nullable protected DexPool pool = newDexPool();
    @Nullable protected ClassPool section = 
      Reflect.getfldval(pool, "classSection");
    
    public NewDex() {
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public Iterator<ClassDef> iterator() {
      return (Iterator<ClassDef>) (Object)
        section.getSortedClasses().iterator();
    }
    
    @Override
    public int size() {
      return section.getSortedClasses().size();
    }
    
    public ClassPool getClassSection() {
      return this.section;
    }
    public DexPool getDexPool() {
      return this.pool;
    }
    @Override
    public byte[] get()  {
      return DexRemix.writeDex(this.pool);
    }
    
    @Override
    public boolean add(final ClassDef classDef) {
      INTERNER.execute(classDef);
      return true;
    }
    
    @Override
    public boolean addAll(final Collection<? extends ClassDef> classDefs) {
      int prevSize = size();
      IterableUtils.forEach(classDefs, INTERNER);
      return size() > prevSize;
    }
    
  }
  
  static Map<Object,MultiDexContainer<DexBackedDexFile>> cache = new HashMap<>();
  
  public static byte[] remixDex(final String path, final String iregex) {
    return remixDex(
      path,
      Pattern.compile(
        iregex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL
      ).matcher("")
    );
  }
  
  public static byte[] remixDex(final String path, final Matcher keepMchr) {
    try (final NewDex newDex = new NewDex()) {
      final MultiDexContainer<DexBackedDexFile> mdc
        = getDexBackedDexFile(path);
      for (final String entryName: mdc.getDexEntryNames()) {
        final DexBackedDexFile dbdf = mdc.getEntry(entryName);
        final Iterable<ClassDef> allClassDefs = (Iterable) dbdf.getClasses();
        final Iterable<ClassDef> filteredDefs = (Iterable) 
        IterableUtils.filteredIterable(
          allClassDefs, new Predicate<ClassDef>() {
            @Override
            public boolean evaluate(final ClassDef c) {
              return keepMchr.reset(c.getType()).find();
            }
          }
        );
        CollectionUtils.addAll(newDex, filteredDefs);
      }
      return newDex.get();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  
  
  public static byte[] remixDex(final MultiDexContainer<?> mdc,
    final Matcher keepMchr)
  {
    try (final NewDex newDex = new NewDex()) {
      final String[] entryNames
        = mdc.getDexEntryNames().toArray(new String[0]);
      final int entryCount = entryNames.length;
      
      for (int i=0; i<entryCount; ++i) {
        final String entryName = entryNames[i];
        Log.w(
          TAG, "Reading dex entry '%s' (%d of %d) ...",
          entryName, i+1, entryCount
        );
        final DexFile dbdf = mdc.getEntry(entryName);
        final Iterable<ClassDef> allClassDefs = (Iterable) dbdf.getClasses();
        final Iterable<ClassDef> filteredDefs = (Iterable) 
        IterableUtils.filteredIterable(
          allClassDefs, new Predicate<ClassDef>() {
            @Override
            public boolean evaluate(final ClassDef c) {
              return keepMchr.reset(c.getType()).find();
            }
          }
        );
        CollectionUtils.addAll(newDex, filteredDefs);
      }
      return newDex.get();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  
  protected static final Matcher LOGCAT_REMOVE_PTRN = Pattern.compile(
    "\\bGC_|_GC\\b|libjavacore.so|libnativehelper.so",
    Pattern.DOTALL | Pattern.UNIX_LINES
  ).matcher("");
  
   protected static final Matcher LOGCAT_DVM_PTRN = Pattern.compile(
     "(?:System\\.out|System\\.err|VFY|dexopt|android_runtime|dalvikvm)[^(]*\\([0-9 ]+\\): ([^\n]*)",
    Pattern.DOTALL | Pattern.UNIX_LINES
  ).matcher("");
  
  public static List<String> logcat() {
    return logcat(true);
  }
    
  public static List<String> logcat(boolean returnAll) {
    String[] lineArr = StringUtils.split(
      PosixFileInputStream.pexecSync(
        "logcat", "-d", "*:s", "dalvikvm:*", "VFY:*", "dexopt:*",
        "DEXOPT:*"
      ),
      "\n"
    );
    List<String> lines = new ArrayList<>(Arrays.asList(lineArr));
    Iterator<String> it = lines.iterator();
    Set<String> seen = new TreeSet<>();
    
    while (it.hasNext()) { 
      String line = it.next();
      if (seen.contains(line)) {
        it.remove();
        continue;
      }
      seen.add(line);
      if (LOGCAT_REMOVE_PTRN.reset(line).find()) {
        it.remove();
        continue;
      }
      if (! returnAll) {
        if (! LOGCAT_DVM_PTRN.reset(line).find()) {
          it.remove();
          continue;
        }
      }
    }
    
    return lines;
  }
  
  
  
  
  public static class TempFile extends File implements AutoCloseable {
    static final File tempDir;
    
    static String canWrite(String dirPath, String fallback) {
      if (dirPath == null) return fallback;
      
      File dir = new File(dirPath);
      if (dir.exists() && !dir.isDirectory()) return fallback;      
      if (!dir.exists() && !dir.mkdirs()) return fallback;
      if (!dir.exists()) return fallback;
      
      if (CollectionUtil.isJRE() || android.os.Process.myUid() == 0) {
        return dirPath;
      }
      return dir.canWrite() ? dirPath : fallback;
    }
    
    private final File file;
    private final String absPath;
    private boolean deleted;
    
    public TempFile(String extension) {
      super(absPath(_createTempFile(
          "tmpdex_DexFileStore",
          StringUtils.startsWith(extension, ".")
            ? extension
            : (".".concat(extension))
        )));
      this.absPath = super.getPath();
      this.file = new File(this.absPath);
    }
    
    static String absPath(final File f) {
      try {
        if (false) throw new IOException();
        return f.getAbsolutePath();
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    
    static File _createTempFile(final String prefix, final String suffix) {
      try {
        if (false) throw new IOException();
        return File.createTempFile(prefix, suffix);
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    public File getFile() {
      return this.file;
    }
    
    @Override
    public String getPath() {
      return this.absPath;
    }
    
    public byte[] toByteArray() {
      try {
        return FileUtils.readFileToByteArray(file);
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    public TempFile write(byte[] bytes) {
      try {
        FileUtils.writeByteArrayToFile(file, bytes);
      } catch (IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
      return this;
    }
    
    @Override
    public boolean delete() {
      if (deleted) return true;
      try {
        if (file.delete()) {
          deleted = true;
          return true;
        }
        PosixUtil.remove(this.absPath);
        deleted = true;
        return true;
      } catch (Throwable ex) {
        Reflector.getRootCause(ex).printStackTrace();
      }
      return false;
    }
    
    @Override
    public void close() {
      if (!deleted) delete();
    }
    
    @Override
    protected void finalize() throws Throwable {
      if (!deleted) delete();
    }
    
    static {
      String writableDirPath;
      writableDirPath = canWrite(System.getProperty("java.temp.dir"), null);
      if (writableDirPath == null) 
        writableDirPath = canWrite(System.getenv("TMPDIR"), null);
      if (writableDirPath == null) 
        writableDirPath = canWrite(System.getenv("TMP"), null);
      if (writableDirPath == null) 
        writableDirPath = canWrite(String.format(
          "%s/.tmp", Environment.getLegacyExternalStorageDirectory()), null);
      if (writableDirPath == null) 
        writableDirPath = canWrite("/data/local/tmp_clazzes", null);
      if (writableDirPath == null) 
        writableDirPath = canWrite("/data/local/temp", null);
      if (writableDirPath == null) 
        writableDirPath = canWrite("/tmp", null);
      if (writableDirPath == null) 
        writableDirPath = canWrite("/sdcard/.tmp", null);
      
      if (writableDirPath == null) {
        new RuntimeException(
          "No writable temp dir! Using current directory as a fallback."
        ).printStackTrace();
        try {
          writableDirPath = new File("./").getAbsolutePath();
        } catch (Throwable e) {
          writableDirPath = ".";
        }
      }
      tempDir = new File(writableDirPath);
    }
  }
  
  
}
  
  