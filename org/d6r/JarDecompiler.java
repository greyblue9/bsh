package org.d6r;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.ArrayTypeLoader;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import org.apache.commons.io.FilenameUtils;
import com.strobel.core.VerifyArgument;
import static org.d6r.TextUtil.str;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.googlecode.dex2jar.v3.DexExceptionHandler;
import com.googlecode.dex2jar.tools.BaksmaliBaseDexExceptionHandler;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.reader.MultiDexFileReader;
import d2jcd9.com.github.marschall.com.sun.nio.zipfs.ZipPath;
import d2jcd9.com.github.marschall.com.sun.nio.zipfs.ZipFileSystem;
import d2jcd9.com.github.marschall.com.sun.nio.zipfs.ZipFileSystemProvider;
import javassist.ClassPool;
import javassist.ClassPoolTail;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.io.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.transforms.TransformationPipeline;
import com.strobel.decompiler.languages.java.ast.transforms.ReplaceResourceIdsTransform;
import com.strobel.assembler.metadata.*;
import java.util.zip.*;
import java.util.regex.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javassist.CtClass;
import bsh.NameSpace;
import java.util.jar.JarFile;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.JarURLConnection;
import javassist.ClassPoolTail;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflect.setfldval;
import java.util.concurrent.atomic.AtomicInteger;
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.decompiler.DecompilerContext;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.apache.commons.collections4.IterableUtils;
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement;
import org.jf.dexlib2.writer.pool.DexPool;
// import org.jf.dexlib2.writer.pool.ClassPool;
import java.lang.reflect.Method;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.writer.ClassSection;
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.io.MemoryDataStore;
// import org.jf.dexlib2.writer.pool.ClassPool;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.*;
   
import static org.d6r.ClassInfo.getDexSubset;
import static org.d6r.ClassInfo.getTypeDefinitions_enjarify;
import static org.d6r.ClassInfo.addGenericSignatures;
// import com.strobel.decompiler.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.BlockStatement;

public class JarDecompiler {
  
  static JarDecompilerContext _context = new JarDecompilerContext();
  
  public static List<String> getArgs() {
    List<List<CharSequence>> procs = StringCollectionUtil.matchLines(
      PosixFileInputStream.pexecSync(
        "ppid_info", "--mi", Integer.toString(PosixFileInputStream.getPid())
      ),
      "^([0-9]+)\t([0-9]+)\t([^ \t]*)(.*)[\t ]([^\t ]*)$"
    );
    for (Iterator<List<CharSequence>> it = procs.iterator(); it.hasNext();) {
      List<CharSequence> pts = it.next();
      String comm = pts.get(pts.size() - 1).toString();
      if ("java".equals(comm.trim()) ||
          comm.startsWith("ionice") ||
          comm.startsWith("renice") ||
         !(comm.startsWith("bsh") || comm.startsWith("procyonDecompileJar"))
         )
      {
        it.remove();
        continue;
      }
    }
    final List<String> allArgs = new ArrayList<>();
    for (final List<CharSequence> pts:
      IterableUtils.reversedIterable(procs))
    {
      final String[] args = ArrayUtils.removeElements(
        StringUtils.split(
          String.valueOf(pts.get(3)).trim(), " "
        ),
        new String[]{ 
          String.valueOf(pts.get(2)),
          String.valueOf(pts.get(pts.size()-1))
        }
      );
      Collections.addAll(allArgs, args);
    }
    return allArgs;
  }
  
  
  public static class JarDecompilerContext implements Cloneable {
    public DecompilerContext ctx;
    public TypeDefinition type;
    public MethodDefinition method;
    public BlockStatement stmt;
    public AstBuilder astBuilder;
    public Iterable<?> params;
    public Throwable exception;
    
    public URI jarUri;
    public ZipFileSystem zfs;
    public Collection<DexFileReader> readers;
    public ZipFile zipFile;
    public MultiDexFileReader mdfr;
    public Dex2jar d2j;
    public BaksmaliBaseDexExceptionHandler exceptionHandler;
    
    @Override
    public JarDecompilerContext clone() {
      final JarDecompilerContext j;
      try {
        j = (JarDecompilerContext) super.clone();
      } catch (final CloneNotSupportedException cnse) { 
        throw new AssertionError();
      }
      j.ctx = this.ctx;
      j.d2j = this.d2j;
      j.exceptionHandler = this.exceptionHandler;
      j.jarUri = this.jarUri;
      j.mdfr = this.mdfr;
      j.method = this.method;
      j.readers = this.readers;
      j.stmt = this.stmt;
      j.type = this.type;
      j.zfs = this.zfs;
      j.zipFile = this.zipFile;
      return j;
    }
  }
  
  public static final List<Map.Entry<Throwable, JarDecompilerContext>> 
    errors = new ArrayList<>();
  
  public static <T extends Throwable> T addError(final T error) {
    try {
      final JarDecompilerContext jdctx = _context;
      jdctx.ctx = ProcyonUtil.g_ctx;
      jdctx.type = ProcyonUtil.g_type;
      jdctx.method = ProcyonUtil.g_method;
      jdctx.stmt = ProcyonUtil.g_blockStatement;
      jdctx.astBuilder = ProcyonUtil.g_astBuilder;
      jdctx.params = ProcyonUtil.g_params;
      jdctx.exception = ProcyonUtil.g_exception;
      
      final JarDecompilerContext jdctx2 = jdctx.clone();
      
      errors.add(Pair.of(error, jdctx2));
      
      final Throwable cause = Reflector.getRootCause(error);
      if (cause instanceof StackOverflowError) {
        final StackOverflowError soe = (StackOverflowError) cause;
        StackTraceElement[] stes = Reflect.getfldval(soe, "stackTrace");
        if (stes == null) stes = soe.getStackTrace();
        if (stes != null && stes.length >= 80) {
          int length = stes.length;
          int cap = Math.min(length >>> 1, 80);
          Reflect.setfldval(
            soe, "stackTrace",
            ArrayUtils.addAll(
              Arrays.copyOfRange(stes, 0, cap),
              ArrayUtils.addAll(
                new StackTraceElement[] {
                  new StackTraceElement(
                    "  [ ", 
                    String.format(
                      ".. 90 frames omitted ... ]  ",
                      length - (cap + cap)
                    ),
                    "", -1
                  )
                },
                Arrays.copyOfRange(stes, length - cap, length)
              )
            )
          );
        }
      } // if StackOverflowError
      try {
        error.printStackTrace(System.err);
      } catch (final Throwable t) {
        final Throwable error2 = new RuntimeException(String.format(
          "Error printing stavk trace for %s!", error.getClass().getName()
        ), t);
        error2.printStackTrace(System.err);
        throw error2;
      }
      if (FATAL_ERRORS) throw Reflector.Util.sneakyThrow(error);
      return error;
    } catch (Throwable error3) {
      if (FATAL_ERRORS) throw Reflector.Util.sneakyThrow(error3);
      if (error3 != error) {
        error.addSuppressed(error3);
      }
      return error;
    }
  }
  
  
  
  public static Set<Object> state = new IdentityHashSet<>();

  public static boolean DEBUG = Boolean.parseBoolean(
    System.getProperty("debug", "false"));
  public static boolean FATAL_ERRORS = Boolean.parseBoolean(
    System.getProperty("fatal.errors", "false"));
  public static boolean ALL_TOP_LEVEL = Boolean.parseBoolean(
    System.getProperty("all.top.level", "false"));
  public static boolean LOAD_LAMBDAS = Boolean.parseBoolean(
    System.getProperty("load.lambdas", "false"));
  
  static LazyMember<Method> TR_setDeclaringType = LazyMember.of(
    TypeReference.class, "setDeclaringType", TypeReference.class
  );
  static LazyMember<Method> MD_setDeclaringType = LazyMember.of(
    MethodDefinition.class, "setDeclaringType", TypeDefinition.class
  );
  public static String TAG = "addApkOrDexClasspathEntries";
  
  /*public static NameSpace ns = (NameSpace) (
                 (CollectionUtil.getInterpreter() != null)
                   ? CollectionUtil.getInterpreter().getNameSpace()
                   : null
                );*/
  
  // public static Map variables = ns != null ? ns.getVariables() : new HashMap();
    
  public static List<ZipFile> result = null;
  
  // insert supporting/standalone functions here
  // define a static function "script_main()" as entry point
  
  public static List<ZipFile> addApkOrDexClasspathEntries() {
    if (result != null) return result;
    final List<String> apkPaths = Arrays.asList(CollectionUtil2.filter(
      ClassInfo.getClassPath().split(":"),
      Pattern.compile(
        "\\.apk$|\\.o?dex$|^/system/framework/|^/data/app/|^/data/dalvik-cache/"
      ).matcher("")
    ));

    final List<File> apkFiles = new ArrayList<>();
    
    if (System.getProperty("input.file") != null) {
      final File file =
        new File(System.getProperty("input.file")).getAbsoluteFile();
      if (file.exists()) apkFiles.add(file);
    }
    
    for (final String path: apkPaths) {
      final File file = new File(path).getAbsoluteFile();
      if (file.exists()) {
        apkFiles.add(file);
      }
    }
    
    if (!apkFiles.isEmpty()) {
      result = addApkOrDexClasspathEntries(apkFiles);
      return result;
    } else {
      return new ArrayList<>();
    }
  }
  
  public static List<File> getFilesFromArgs() {
    List<String> paths = new ArrayList<>(Arrays.asList(
      StringUtils.split(
        StringUtils.join(
          CollectionUtil2.filter(
            getArgs(), "\\.(?:apk|jar|dex|aar|war)"
          ), "\n"
        ), "\t\n :=;"
      )
    ));
    final List<File> files = new ArrayList<>();
    for (final String path: paths) {
      final File file;
      try {
        file = new File(path).getAbsoluteFile();
        if (file.exists() && file.isFile()) {
          files.add(file);
        }
      } catch (final Exception ioe) {
      }
    }
    if (!files.isEmpty()) {
      System.setProperty("input.file", files.get(0).getPath());
    }
    return files;
  }
  
  public static List<ZipFile> addApkOrDexClasspathEntries(
    List<File> apkFiles)
  {
    if (apkFiles.isEmpty() || apkFiles.get(0) == null ||
       !apkFiles.get(0).isFile())
    {
      apkFiles = getFilesFromArgs();
    }
    final List<ZipFile> createdJarFiles = new ArrayList<>();
    try {
      String TAG = "addApkOrDexClasspathEntries2";
      ITypeLoader root = ProcyonUtil.getTypeLoader();
      
      for (File apkFile: apkFiles) {
        if (apkFile.getPath().trim().length() == 0) continue;
        if (!apkFile.exists() ||
            !apkFile.isFile() ||
            !(new File(apkFile.getAbsolutePath()).exists()))
        {
          continue;
        }
        try {
          apkFile = apkFile.getCanonicalFile();
        } catch (IOException ioe) {
          ioe.printStackTrace(System.err);
          continue;
        }
        boolean hasClasses = false;
        ZipFile zipFile = null;
        File tempFile = null;
        ZipFileSystemProvider zfsProv = new ZipFileSystemProvider();
        state.add(zfsProv);
        Map<String, Object> env = null;
        
        try {
          zipFile = new ZipFile(apkFile);
          ZipEntry[] classEnts = CollectionUtil2.filter(CollectionUtil.toArray(
             CollectionUtil.asIterable(zipFile.entries())
          ), ".\\.class$");
          hasClasses = (classEnts.length > 0);
        } catch (java.util.zip.ZipException notAZip) {
        }
        
        if (hasClasses) {
          tempFile = apkFile;
          env = Collections.emptyMap();
        } else {
          tempFile = new File(
            (new File("/mnt/shell/emulated/0").exists())
              ? new File("/mnt/shell/emulated/0")
              : new File(System.getProperty("java.io.tmpdir", "/tmp")),
            String.format(
              "%s_dex2jar_%d.jar",
              FilenameUtils.removeExtension(apkFile.getName()),
              System.currentTimeMillis()
            )
          );
          env = RealArrayMap.toMap("create", "true");
        }
        
        _context = new JarDecompilerContext();
        
        // ===== shared code =====
        String uriString = String.format("jar:%s", tempFile.toURI());
        Log.d(
          TAG,
          "Creating Jar URI: %s",
          uriString
        );
        URI jarUri = new URI(uriString);
        _context.jarUri = jarUri;
        ZipFileSystem zfs;
        try {
          zfs = (ZipFileSystem) zfsProv.newFileSystem(jarUri, env);
        } catch (final IllegalArgumentException iae) {
          throw addError(iae);
        }
        _context.zfs = zfs;
        state.add(zfs);
        // ===== end sharedd codee =====
        
        if (hasClasses) {
          // todo?
        } else {
          Log.d(TAG, "Importing classpath dex/apk: [%s]", apkFile);
          Log.d(TAG, "Creating output jar file: [%s]", tempFile);
          
          Collection<DexFileReader> readers = new ArrayList<DexFileReader>();
          _context.readers = readers;
          state.add(readers);
          zipFile = null;
          byte[] arscBytes = null;
          try {
            zipFile = new ZipFile(apkFile);
            _context.zipFile = zipFile;
            for (int i=1; i<10; ++i) {
              ZipEntry entry = zipFile.getEntry(
                (i != 1) ? "classes%d.dex" : "classes.dex"
              );
              if (entry == null) break;
              Log.d(
                TAG, "Reading '%s' (%d bytes)", entry.getName(), entry.getSize());
              byte[] dexBytes = IOUtils.toByteArray(
                new org.apache.commons.io.input.AutoCloseInputStream(
                  zipFile.getInputStream(entry)
                )
              );
              readers.add(new DexFileReader(dexBytes));
              ZipPath classesDexZipPath
                = zfs.getPath(entry.getName(), new String[0]);
              state.add(classesDexZipPath);
              OutputStream os = zfs.provider().newOutputStream(classesDexZipPath);
              IOUtils.copyLarge(new ByteArrayInputStream(dexBytes), os);
              os.flush();
              os.close();
            }
            for (final String entryName: Arrays.asList(
              "resources.arsc", "AndroidManifest.xml"))
            {
              ZipEntry arscEntry = zipFile.getEntry(entryName);
              if (arscEntry == null && StringUtils.endsWith(entryName, "arsc")) {
                ZipEntry[] ents = CollectionUtil2.filter(
                  CollectionUtil.toArray(
                    CollectionUtil.asIterable(zipFile.entries())
                  ), String.format(
                     "\\.%s", StringUtils.substringAfterLast(entryName, ".")
                  )
                );
                if (ents.length != 0) {
                  arscEntry = zipFile.getEntry(ents[0].toString());
                }
              }
              if (arscEntry != null) {
                arscBytes = ZipUtil.toByteArray(zipFile, arscEntry.getName());
                Log.d(TAG, "Adding '%s' entry (%d bytes) from '%s' ...",
                  arscEntry.getName(), arscEntry.getSize(), zipFile.getName());
                ZipPath arscZipPath =
                  (ZipPath) zfs.getPath(entryName, new String[0]);
                state.add(arscZipPath);
                OutputStream os = zfs.provider().newOutputStream(arscZipPath);
                IOUtils.copyLarge(new ByteArrayInputStream(arscBytes), os);
                os.flush();
                os.close();
              }
            }
          } catch (IOException notAZip) {
            readers.add(new DexFileReader(apkFile));
            ZipPath classesDexZipPath = zfs.getPath("classes.dex", new String[0]);
            state.add(classesDexZipPath);
            OutputStream os = zfs.provider().newOutputStream(classesDexZipPath);
            IOUtils.copyLarge(
              new org.apache.commons.io.input.AutoCloseInputStream(
                new FileInputStream(apkFile)
              ), os
            );
            os.flush();
            os.close();
          }
          int prevLevel = Log.INSTANCE.enabledLevels;
          try {
            Log.INSTANCE.enabledLevels = (254 | prevLevel);
            Log.d(
              TAG, "Creating MultiDexFileReader for input using %d reader%s ...",
              readers.size(), (readers.size() != 1) ? "s" : ""
            );
            final MultiDexFileReader mdfr = new MultiDexFileReader(readers);
            _context.mdfr = mdfr;
            state.add(mdfr);
            Log.d(TAG, "Opened reader: %s ...", mdfr);
            final Dex2jar d2j = Dex2jar.from(mdfr);
            state.add(d2j);
            Log.d(TAG, "Created V3 dispatcher: %s ...", d2j);
            ZipPath rootzp = (ZipPath) zfs.getRootDirectories().iterator().next();
            state.add(rootzp);
            _context.d2j = d2j;
            
            final BaksmaliBaseDexExceptionHandler exceptionHandler =
              new BaksmaliBaseDexExceptionHandler();
            _context.exceptionHandler = exceptionHandler;
            
            Log.d(TAG, "Created exception saver: %s ...", exceptionHandler);
            
            d2j.optimizeSynchronized(true)
               .printIR(false)
               .skipDebug(false)
               .topoLogicalSort(true)
               // .noCode(false)
               .reUseReg(false)
               .skipDebug(false)
               .setExceptionHandler(exceptionHandler);
               
            Log.d(TAG, "Configured Dex2jar V3 flags ...");
            Log.d(TAG, "Starting transformation operations ...");
            d2j.to(zfs);
          } finally {
            Log.INSTANCE.enabledLevels = prevLevel;
            zfs.close();
          }
        } // !hasClasses
        
        
        //variables.put("zfs", new bsh.Variable("_zfs", Pair.of("_zfs", zfs)));
        Log.d(TAG, "Wrote \"%s\" (%d bytes) `_zfs`", tempFile, tempFile.length());
        
        URL jarUrl =
          new URL(String.format("jar:file:%s!/classes.dex", tempFile.getPath()));
        URL jarUrl2 =
          new URL(String.format("file:%s", tempFile.getPath()));
        ClassInfo.appendClassPathFile(tempFile.getPath());
        final ClassLoader cl 
          = Thread.currentThread().getContextClassLoader();
        if (cl instanceof URLClassLoader) {
          Reflector.invokeOrDefault(
            ((URLClassLoader) cl), "addURL", new Object[]{ jarUrl }
          );
          Reflector.invokeOrDefault(
            ((URLClassLoader) cl), "addURL", new Object[]{ jarUrl2 }
          );
          System.setProperty("java.boot.class.path", System.getProperty(
            "java.boot.class.path") + ":" + tempFile.getPath());
        } else {
          ClassInfo.appendClassPathFile(apkFile.getPath());
        }
        System.setProperty("java.boot.class.path",
          tempFile.getPath() + ":" + apkFile.getPath() + ":" +
          System.getProperty("java.boot.class.path"));
        
        JarURLConnection conn = (JarURLConnection) jarUrl.openConnection();
        state.add(conn);
        conn.setUseCaches(true);
        final JarFile jarFile;
        JarFile _jarFile = null;
        try {
          _jarFile = conn.getJarFile();
        } catch (final IOException ioe) {
        }
        jarFile = (_jarFile != null)
          ? _jarFile
          : new JarFile(PathInfo.getPathInfo(jarUrl.toString()).path);
        createdJarFiles.add(jarFile);
       
        Log.i(TAG, "Inject JarTypeLoader for jar: %s", jarFile.getName());
        Reflect.setfldval(
          ProcyonUtil.getMetadataResolver(),
          "_typeLoader",
          ProcyonUtil.typeLoader = new CompositeTypeLoader(
            new JarTypeLoader(jarFile),
            ProcyonUtil.getTypeLoader()
          )
        );
        Log.i(
          TAG, "TypeLoader(s): %s: %s", ProcyonUtil.typeLoader,
          Debug.ToString(ProcyonUtil.typeLoader)
        );
        // update typeLoaders array (local-var-only change)
        Log.d(TAG, "Created output jar file: [%s]", tempFile);
      } // for File apkFile: apkFiles
    } catch (Exception e) {
      addError(e);
    }
    Log.d(TAG, "Created output jar files: %s", createdJarFiles);
    return createdJarFiles;
  }
  
  
  public static void script_main() {
  }
  
  
  public static final String CLASS_SUFFIX = ".class";
  
  public static void decompile(String jarPath, String filter, boolean exit)
    throws Throwable
  {
    Log.i(TAG, "decompile(%s, %s, %s)", jarPath, filter, exit);
    
    final String wholeClasspath = System.getProperty(
      "java.boot.class.path",
      TextUtil.prependIfMissing(
        System.getProperty(
          "java.boot.class.path", ClassInfo.getBootClassPath()
        ),
        ":",
        ClassInfo.getClassPath()
      )
    );
    
    System.setProperty("java.boot.class.path", wholeClasspath);
    Log.i(TAG, "wholeClasspath = %s", wholeClasspath);
    
    if (System.getProperty("input.file") == null) {
      System.setProperty("input.file", jarPath);
    }
    
    final List<ZipFile> zipFiles = addApkOrDexClasspathEntries(
      new ArrayList<File>(Arrays.asList(new File(jarPath)))
    );
    Log.d(TAG, "addApkOrDexClasspathEntries() returned: %s", zipFiles);
    
    final ZipFile zipFile = (!zipFiles.isEmpty())
      ? zipFiles.get(0)
      : new ZipFile(new File(jarPath));
    
    final List<String> names = ClassInfo.getClassNamesFromEntries(
      zipFile,
      ALL_TOP_LEVEL // include nested
    );
    String[] classNames = names.toArray(new String[0]);
    int count = classNames.length;
    System.err.printf(
      "Found %d top-level classes in jar '%s' ...\n", count, jarPath
    );
    if (filter != null) {
      classNames = CollectionUtil2.filter(classNames, filter);
      System.err.printf(
        "Filtered out %d classes, leaving total #: %d\n",
        count - classNames.length, classNames.length
      );
      count = classNames.length;
    }
    File jar = new File(jarPath);
    String jarFileName = jar.getName();
    File cwd = new File(".");
    String jarName = FilenameUtils.removeExtension(jarFileName);
    
    final Map<String, String> props = (Map<String, String>) (Object) 
      System.getProperties();
    
    final String outDirPath;
    File rawOutDir;
    if (props.containsKey("output.dir")) {
      rawOutDir = new File(outDirPath = (String) props.get("output.dir"));
    } else {
      final String outDirName = String.format(
        "%s.src_decompiled.%s", 
        StringEscapeUtils.escapeJava(
          jarName
        ).replaceAll("[^a-zA-Z0-9_ $.-]", "__"),
        FastDateFormat.getInstance("yyyy-MM-dd_HHmmss").format(
          System.currentTimeMillis()
        )
      );
      outDirPath = (
        rawOutDir = new File(cwd, outDirName)
      ).getAbsolutePath();
    }
    if (!rawOutDir.exists() && !rawOutDir.mkdirs()) {
      rawOutDir = new File(PosixFileInputStream.resolve(outDirPath));
      if (!rawOutDir.exists() && !rawOutDir.mkdirs()) {
        throw new Error(String.format(
          "Cannot create output directory %s or %s",
          str(outDirPath), str(rawOutDir.getPath())
        ));
      }
    } else if (!rawOutDir.isDirectory() || rawOutDir.isFile()) {
      throw new Error(String.format(
        "Cannot overwrite existing file with output directory: %s or %s",
        str(outDirPath), str(rawOutDir.getPath())
      ));
    }
    
    final File outDir = rawOutDir;
    outDir.mkdirs();
    System.err.printf("Using source output directory '%s' ...\n", outDir);
    
    System.err.printf("Added jar typeLoader's jar collection ...\n");
    ProcyonUtil.addJar(zipFile);
    
    AtomicInteger suppress = null;
    int oldSuppressionLevel = 0, suppressionLevel = 0;
    if (Boolean.parseBoolean(System.getProperty("suppress.handlers", "false"))) {
      try {
        suppress = Reflect.getfldval(
          com.strobel.assembler.metadata.ExceptionHandlerMapper.class,
          "suppressionDepth"
        );
        if (suppress != null) {
           oldSuppressionLevel = suppress.get();
           suppressionLevel = suppress.incrementAndGet();
           Log.d(TAG,
             "Permanently suppressing exception handlers during session: %d",
             suppressionLevel);
        }
      } catch (Exception e) {
        Log.e(TAG, String.format("exceptionHandler suppression is unavailable: %s",
          Reflector.getRootCause(e)));
      }
    }
    
    
    final long start = System.currentTimeMillis();
    int i = -1, completed = 0, failed = 0, partials = 0;
    
    final int startAtIndex
      = Integer.parseInt(System.getProperty("start.index", "0"), 10);
    
    List<String> partialClassNames = new ArrayList<>();
    List<String> failedClassNames = new ArrayList<>();
    
    new ReplaceResourceIdsTransform(
      ProcyonUtil.getDecompilerContext()
    );
    Class<?>[] pipeline_classes = ArrayUtils.addAll(
      CollectionUtil.toArray(
        ProcyonUtil.getTransformerClassesJRE()
      ),
      new Class<?>[]{ ReplaceResourceIdsTransform.class }
    );
    ProcyonUtil.transformerClasses = new ArrayList(
      Arrays.asList(pipeline_classes)
    );
        
    for (String className: classNames) {
      com.strobel.assembler.metadata.ExceptionHandlerMapper
          .suppressionDepth.incrementAndGet();
      
      final int classIndex = (++i);
      if (classIndex < startAtIndex) {
        completed++;
        continue;
      }
      
      boolean ok = true;
      // if (i % 30 == 0) System.gc();
      File outFile = new File(
        outDir + "/" + ClassInfo.classNameToPath(className, "java")
      );
      if (outFile.exists()) {
        if (outFile.length() > 0L) {
          completed++;
          continue;
        }
        outFile.delete();
      }
      
      new File(
        StringUtils.substringBeforeLast(outFile.getPath(),"/")
      ).mkdirs();
      outFile.createNewFile();
      System.err.printf(
        "\n\u001b[1;31m==\u001b[1;35m===\u001b[0m" +
        " Processing %d / %d: " +
        "\u001b[1;36m%s\u001b[0m " +
        "\u001b[1;35m===\u001b[1;31m==\u001b[0m",
        (i+1), count, className
      );
      
      // if (i % 250 == 0) System.gc();
      TypeDefinition td = null;
      CompilationUnit cu = null;
      // ProcyonUtil.decompilerContext = null;
      // ProcyonUtil.astBuilder = null;
      errors.clear();
      
      try {
        _context.method = null;
        _context.ctx = null;
        td = ProcyonUtil.getTypeDefinition(className);
        _context.type = td;
        try {
          cu = ProcyonUtil.decompileToAst(td);
          // System.err.printf("Decompiling to: '%s'\n\n", outFile);
          ok = true;
        } catch (final Throwable e) {
          ok = false;
          addError(e);
          try {
            
            List<String> tdClassNames
              = new ArrayList<>(Arrays.asList(className));
            for (ConstantPool.TypeInfoEntry entry:
                CollectionUtil2.typeFilter(
                  Reflect.<Map<Object, Object>>getfldval(
                    Reflect.getfldval(td, "_constantPool"), "_entryMap"
                  ).values(), ConstantPool.TypeInfoEntry.class))
            {
              String typeName = ClassInfo.typeToName(entry.getName());
              if (typeName.startsWith(className) &&
                  typeName.length() > className.length() &&
                  typeName.charAt(className.length()) == '$')
              {
                tdClassNames.add(ClassInfo.typeToName(entry.getName()));
              }
            }
            
            String brokenClassName =
                ProcyonUtil.g_type != null
                  ? ClassInfo.typeToName(
                      ProcyonUtil.g_type
                        .getInternalName())
                  : className;
            if (! brokenClassName.equals(className)) {
              tdClassNames.add(brokenClassName);
            }
            Map<String, TypeDefinition> tdmap = getTypeDefinitions_enjarify(
              System.getProperty("input.file", jarPath),
              className,
              tdClassNames.toArray(new String[0])
            );
            if (! brokenClassName.equals(className)) {
              ((MetadataSystem)
                ProcyonUtil.getMetadataResolver()).addTypeDefinition(
                tdmap.get(brokenClassName)
              );
            }

            
            
            try {
              td = tdmap.get(className);
              cu = ProcyonUtil.decompileToAst(td, true, true, true, true);
            } catch (final Throwable e3) {
              addError(e3);
              try {
                ProcyonUtil.decompilerContext = null;
                cu = ProcyonUtil.decompileToAst(td, false, false, false, false);
                final DecompilerContext ctx = ProcyonUtil.g_ctx;
                try {
                  TransformationPipeline.runTransformationsUntil(
                    cu, null, ctx
                  );
                } catch (final Throwable e5) {
                  addError(e5);
                }
              } catch (Throwable e4) {
                addError(e4);
              }
            }
            if (cu != null) {
              ok = true;
            } else {
              for (final ConstantPool.TypeInfoEntry entry:
                    CollectionUtil2.typeFilter(
                      Reflect.<Map<Object, Object>>getfldval(
                        Reflect.getfldval(td, "_constantPool"), "_entryMap"
                      ).values(), ConstantPool.TypeInfoEntry.class))
              {
                final String typeName = ClassInfo.typeToName(entry.getName());
                if (true ||
                    typeName.startsWith(className) &&
                    typeName.length() > className.length() &&
                    typeName.charAt(className.length()) == '$')
                {
                  tdClassNames.add(ClassInfo.typeToName(entry.getName()));
                }
              }
              
              brokenClassName =
                ProcyonUtil.g_type != null
                  ? ClassInfo.typeToName(
                      ProcyonUtil.g_type
                        .getInternalName())
                  : className;
              if (! brokenClassName.equals(className)) {
                tdClassNames.add(brokenClassName);
              }
              tdmap = getTypeDefinitions_enjarify(
                System.getProperty("input.file", jarPath),
                className,
                tdClassNames.toArray(new String[0])
              );
              for (final Map.Entry<String, TypeDefinition> entry:
                   tdmap.entrySet())
              { 
                final String typeName = entry.getKey();
                ProcyonUtil.removeType(
                  ProcyonUtil.getMetadataResolver(), typeName
                );
                ((MetadataSystem) ProcyonUtil.getMetadataResolver()
                  ).addTypeDefinition(entry.getValue());
              }
              try {
                td = tdmap.get(className);
                cu = ProcyonUtil.decompileToAst(td);
              } catch (final Throwable e5) {
                addError(e5);
                cu = ProcyonUtil.decompileToAst(
                  td, false, false, false, false
                );
                try {
                  ProcyonUtil.transform(cu, true);
                } catch (final Throwable e6) {
                  addError(e6);
                }
              }
            }
          } catch (final Throwable e5) {
            addError(e5);
            Log.w(
              TAG,
              "Trying emergency decompile (press CTRL+\\ to abort) ..."
            );
            System.err.println(PosixFileInputStream.pexecSync(
              "emergency_decompile",
              new File(zipFile.getName()).getAbsoluteFile().getPath(),
              className,
              outDir.getAbsoluteFile().getPath()
            ));
          }
        }
      } catch (final Throwable ex) {
        addError(ex);
        addError(new RuntimeException(String.format(
          "Fatal error parsing class '%s': %s", className, ex
        ), ex));
      }
      if (cu != null) {
        String text = cu.getText();
        if (!errors.isEmpty()) {
          ok = false;
          StringBuilder sb = new StringBuilder("\n/**\n");
          for (final Map.Entry<Throwable, JarDecompilerContext> p: errors) {
            final Throwable err = p.getKey();
            final JarDecompilerContext jdctx = p.getValue();
            sb.append("\n")
              .append(StringUtils.repeat("-", 72))
              .append("\n")
              .append(TextUtil.colorrm(
                ExceptionUtils.getStackTrace(err)))
              .append("\n")
              .append(TextUtil.colorrm(Debug.ToString(jdctx)));
          }
          text = sb.insert(0, text).append("\n\n*/\n\n").toString();
        }
        FileUtils.writeStringToFile(outFile, text);
      }
      
      final long outSize = outFile.length();
      if (outSize >= 0L) {
        if (ok) {
          completed++;
          System.err.printf(
            " [OK] (%d bytes)", outSize
          );
        } else {
          partials++;
          partialClassNames.add(className);
          System.err.printf(
            "[PART] Wrote file '%s' (%d bytes)\n", outFile, outSize
          );
        }
      } else {
        failed++;
        failedClassNames.add(className);
        System.err.printf("[FAIL] Failed on class '%s'\n", className);
      }
    }
    
    long dur = (System.currentTimeMillis() - start);
    long durSec = dur / 1000;
    long secondsCount = durSec % 60;
    durSec -= secondsCount;
    long minutesCount = durSec / 60;
    
    System.err.printf(
      "\n\n" +
      "*** Decompilation completed in %1$d minute%2$s and %3$d second%4$s! ***\n" +
      "Summary:\n" +
      "        %5$4d succeeded\n" +
     ((partials > 0) ?
      "        %6$4d partials     (\"%7$s\")\n": "") +
     ((failed > 0) ?
      "        %8$4d failed       (\"%9$s\")\n": "") +
      "        ----------------\n" +
      "Total:  %10$4d\n\n",
      minutesCount,
      minutesCount != 1? "s": "",
      secondsCount,
      secondsCount != 1? "s": "",
      
      completed, // %5
      
      partials, // %6
      StringUtils.join(partialClassNames, "\", \""), // %7
      
      failed, // %8
      StringUtils.join(partialClassNames, "\", \""), // %9
      
      count // %10
    );
    
    if (suppress != null && oldSuppressionLevel != suppressionLevel) {
      suppress.decrementAndGet();
    }
    
    if (exit) {
      System.exit(Integer.valueOf((completed > 0 && failed == 0)? 0: 1).intValue());
    }
  }
  
  
  
  public static void decompile(String jarPath, String filter)
    throws Throwable
  {
    decompile(jarPath, filter, false);
  }
  
  public static void main(String... args)
    throws Throwable
  {
    script_main();
    
    String inputFileProp = System.getProperty(
      "input.file", System.getProperty("input.path")
    );
    
    
    if (inputFileProp == null || inputFileProp.length()==0) {
      inputFileProp = addApkOrDexClasspathEntries(Arrays.asList())
        .get(0).getName();
    }
    
    String inputFilePath = (inputFileProp != null)
      ? inputFileProp
      : (args.length != 0)
          ? args[0]
          : null;
    
    String filterProp = System.getProperty("input.filter");
      String filter = (filterProp != null)
      ? filterProp
      : "^((?!android.support|android.arch|org.apache.commons|" +
        "com.google.*(?:common|gson|protobuf|gms))[^$])*$";
      
      decompile(inputFileProp.trim(), filter, false);
    
  }
  

}