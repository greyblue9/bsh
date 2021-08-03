package org.d6r;

// import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.java.decompiler.util.DataInputFullStream;
// import org.jetbrains.java.decompiler.struct.StructContext;
// import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.*; // ContextUnit;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.*; // IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.d6r.annotation.*;
import static org.d6r.LazyMember.of;
  
import javassist.ClassPool;
// import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
// import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.*;
import org.apache.commons.io.FileUtils;
import org.d6r.PosixFileInputStream;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.Buffer;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.*;
import java.lang.reflect.Array;
import java.util.zip.*;
import java.util.jar.*;
import bsh.ClassIdentifier;
import bsh.Interpreter;
import bsh.CallStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.cojen.classfile.TypeLoaderClassFileDataLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import java.lang.reflect.Field;
import org.apache.commons.lang3.tuple.Pair;
import static java.lang.String.format;
import static org.d6r.RealArrayMap.toMap;
import static java.util.Arrays.asList;


class ResultSaver implements IResultSaver {
  
  static void printf(String format, Object... args) {
    System.err.printf(format, args);
  }
  
  IBytecodeProvider bytecodeProvider;
  File outDir;
  boolean write;
  public final @NonDumpable Map<String, String> sources = new TreeMap<>();
  
  public ResultSaver(File outDir, IBytecodeProvider bytecodeProvider,
  boolean writeToDisk) 
  {
    this.bytecodeProvider = bytecodeProvider;
    this.outDir = outDir;
    this.write = write;
  }
  
  public ResultSaver(IBytecodeProvider bytecodeProvider) {
    this.bytecodeProvider = bytecodeProvider;
    this.outDir = new File("/dev/urandom") {
      
      @Override
      public String getName() { return callOutBadTouch("getName()"); }
      @Override
      public String getPath() { return callOutBadTouch("getPath()"); }
      @Override
      public String getParent() { return callOutBadTouch("getParent()"); }
      @Override
      public String getCanonicalPath() { return callOutBadTouch("getCanonicalPath()");}
      @Override
      public String getAbsolutePath() { return callOutBadTouch("getAbsolutePath()"); }
      @Override
      public File getParentFile() { return callOutBadTouch("getParentFile()"); }
      @Override
      public File getCanonicalFile() { return callOutBadTouch("getCanonicalFile()"); }
      @Override
      public File getAbsoluteFile() { return callOutBadTouch("getAbsoluteFile()"); }
      @Override
      public URI toURI() { return callOutBadTouch("toURI()"); }
      @Override
      public URL toURL() { return callOutBadTouch("toURL()"); }
      @Override
      public boolean exists() { return callOutBadTouch("exists()"); }
      @Override
      public boolean isFile() { return callOutBadTouch("isFile()"); }
      @Override
      public boolean isDirectory() { return callOutBadTouch("isDirectory()"); }
      @Override
      public long length() { return callOutBadTouch("length()"); }
      
      public final <X> X callOutBadTouch(final String illFatedMethodCallAttempt) {
        throw new UnsupportedOperationException(String.format(
          "((File) ResultSaver.outDir).%s was called, " +
          "but this ResultSaver was created for in-memory use only.",
          illFatedMethodCallAttempt
        ));
      }
      
    };
    this.write = false;
  }
  
  public void saveFolder(String path) {
    printf("%s.saveFolder(path = \"%s\");\n", 
      getClass().getSimpleName(), path);
    if (write) {
      new File(outDir, path).mkdirs();
    }
  }
  
  @NotImplemented
  public void copyFile(String source, String path, String entryName) {
    printf("%s.copyFile(source = \"%s\", path = \"%s\", "
      + "entryName = \"%s\");\n", getClass().getSimpleName(),
      source, path, entryName);
  }
  
  @NotImplemented
  public void saveClassFile(String path, String qualifiedName,
  String entryName, String content, int[] mapping)
  {
    printf("%s.saveClassFile(path = \"%s\", qualifiedName = \"%s\", "
      + "entryName = \"%s\", content = String[%s], mapping = %s)\n",
      getClass().getSimpleName(), path, qualifiedName, entryName,
      content.length(), (mapping != null)
        ? format("int[%d]", mapping.length): "null"
    );
    sources.put(qualifiedName, content);
  }
  
  @NotImplemented
  public void createArchive(String path, String archiveName,
  Manifest manifest)
  {
    printf("%s.createArchive(path = \"%s\", archiveName = \"%s\", "
      + "manifest = %s);\n", getClass().getSimpleName(), path, 
      archiveName, manifest);
  }

  @NotImplemented
  public void saveDirEntry(String path, String archiveName,
  String entryName)
  {
    printf("%s.saveDirEntry(path = \"%s\", archiveName = \"%s\", "
      + "entryName = \"%s\");\n", getClass().getSimpleName(), path, 
      archiveName, entryName);
  }
  
  @NotImplemented
  public void copyEntry(String source, String path, String archiveName,
  String entryName) 
  {
    printf("%s.copyEntry(source = \"%s\", path = \"%s\", "
      + "archiveName = \"%s\", entryName = \"%s\");\n",
      getClass().getSimpleName(), source, path, archiveName,
      entryName);
  }
  
  public void saveClassEntry(String path, String archiveName,
  String qualifiedName, String entryName, String content)
  {
    printf("%s.saveClassEntry(path = \"%s\", archiveName = \"%s\", "
      + "qualifiedName = \"%s\", entryName = \"%s\", "
      + "content = String[%d]);\n",
      getClass().getSimpleName(), path, archiveName, qualifiedName, 
      entryName, content.length());
    
    sources.put(qualifiedName, content);
  }
  
  @NotImplemented
  public void closeArchive(String path, String archiveName) {
    printf("%s.saveDirEntry(path = \"%s\", archiveName = \"%s\");\n",
      getClass().getSimpleName(), path, archiveName);
  }
  
}


/*
class TypeLoaderBytecodeProvider implements IBytecodeProvider {
  
  ITypeLoader typeLoader;
  String dirPrefix;
  final Buffer buf;
  
  public TypeLoaderBytecodeProvider(ITypeLoader typeLoader,
  String dirPrefix) {
    this.typeLoader = typeLoader;
    this.buf = new Buffer(65536);
    this.dirPrefix = dirPrefix;
  }
  
  SoftHashMap<String,byte[]> _cache = new SoftHashMap<>();
  
  @Override
  public byte[] getBytecode(String externalPath, String internalPath) 
    throws IOException
  {
    File file = new File(externalPath);
    if (internalPath == null && file.exists()) {
      return FileUtils.readFileToByteArray(file);
    }
    
    if (file.exists()) {
      try (ZipFile archive = new ZipFile(file)) {
        ZipEntry entry = archive.getEntry(internalPath);
        if (entry == null) {
          throw new IOException(
            "ZipFile [%s]: Entry not found: " + internalPath
          );
        }
        try (InputStream is = archive.getInputStream(entry)) {
          return IOUtils.toByteArray(is);
        }
      }
    }
    
    return getBytecode(
      externalPath, internalPath, null //qualifiedClassName
    );
  }
  
  public byte[] getBytecode(String externalPath, String internalPath, 
  String qualifiedClassName)
    throws IOException
  {
    String name = qualifiedClassName != null
      ? qualifiedClassName
      : ((internalPath != null)
          ? internalPath
          : ((externalPath != null)
              ? externalPath
              : null));
    
    if (name == null) {
      throw new IOException(
        "getBytecode() called with all null arguments");
    }
    
    if (StringUtils.endsWith(name, ".class")) {
      name = StringUtils.substringBeforeLast(name, ".class");
    }
    if (dirPrefix != null
    &&  StringUtils.startsWith(name, dirPrefix)) {
      name = StringUtils.substringAfter(name, dirPrefix.concat("/"));
    }
    String typeNameOrPath = name.indexOf('.') != -1
      ? ClassInfo.classNameToPath(name)
      : name;
    
    byte[] classBytes;
    if ((classBytes = _cache.get(name)) != null) {
      System.err.printf("getBytecode() cache hit: [%s]\n", name);
      return classBytes;
    }
    
    System.err.printf("%s.getBytecode(externalPath: %s, "
      + "internalPath: %s, qualifiedClassName: %s);\n  "
      + "  - caller: %s\n",
      getClass().getSimpleName(), 
      externalPath!=null?"\"".concat(externalPath).concat("\""):"null",
      internalPath!=null?"\"".concat(internalPath).concat("\""):"null",
      qualifiedClassName != null
        ? "\"".concat(qualifiedClassName).concat("\"") :"null",
      Debug.getCallingMethod(4)
    );
    System.err.printf("  - name = \"%s\"\n", name);
    System.err.printf("  - typeNameOrPath = \"%s\"\n", typeNameOrPath);
    
    buf.reset();
    
    boolean found = typeLoader.tryLoadType(typeNameOrPath, buf);
    if (found) {
      classBytes = new byte[buf.size()];
      buf.read(classBytes, 0, classBytes.length);
      _cache.put(name, classBytes);
      return classBytes;
    }
    throw new IOException("Not found: " + typeNameOrPath);
  }
}
*/




class FernflowerEnvironment
  implements AutoCloseable
{
  
  static LazyMember<Field> UNITS = of("units", StructContext.class);
  static final String False = "0";
  static final String True  = "1";
  
  public static final Map<String,Object> DEFAULT_OPTIONS = toMap(asList(
    Pair.of(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, True),
    Pair.of(IFernflowerPreferences.DECOMPILE_INNER,              True),
    Pair.of(IFernflowerPreferences.DECOMPILE_ASSERTIONS,         True),
    Pair.of(IFernflowerPreferences.HIDE_EMPTY_SUPER,             True),
    Pair.of(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR,     True),
    Pair.of(IFernflowerPreferences.DECOMPILE_ENUM,               False), //?
    Pair.of(IFernflowerPreferences.REMOVE_GET_CLASS_NEW,         True), //?
    Pair.of(IFernflowerPreferences.ASCII_STRING_CHARACTERS,      True),
    Pair.of(IFernflowerPreferences.SYNTHETIC_NOT_SET,            True), //?
    Pair.of(IFernflowerPreferences.UNDEFINED_PARAM_TYPE_OBJECT,  True), //?
    Pair.of(IFernflowerPreferences.USE_DEBUG_VAR_NAMES,          True),
    Pair.of(IFernflowerPreferences.REMOVE_EMPTY_RANGES,          True),
    Pair.of(IFernflowerPreferences.LAMBDA_TO_ANONYMOUS_CLASS,    True),
    Pair.of(IFernflowerPreferences.NEW_LINE_SEPARATOR,           True),
    Pair.of(IFernflowerPreferences.FINALLY_DEINLINE,             False), //?
    Pair.of(IFernflowerPreferences.RENAME_ENTITIES,              False), //?
    Pair.of(IFernflowerPreferences.INDENT_STRING,                "  ")
  ));
  
  File outDir;
  
  PrintStreamLogger logger;
  Map<String, Object> options;
  IResultSaver resultSaver;
  Fernflower fernflower;
  
  StructContext structContext;
  LazyLoader lazyLoader;
  Map<String, ContextUnit> contextUnits;
  Set<Class<?>> primaryClasses;
  
  IBytecodeProvider bytecodeProvider;
  RelatedClassFinder relatedClassFinder;
  boolean clearContext;
  
  List<Throwable> errors = new LinkedList<>(); 
  List<Object> results = new ArrayList<>();
  
  
  public FernflowerEnvironment(String className, OutputStream logOut,
  @Nullable Map<String, Object> userOptions)
  {
    outDir = createTempDir(className);
    logger = new PrintStreamLogger(
      logOut instanceof PrintStream
        ? (PrintStream) logOut
        : (PrintStream) new PrintStream(logOut)
    );
    
    options = new TreeMap<String,Object>();
    options.putAll(IFernflowerPreferences.DEFAULTS);
    options.putAll(DEFAULT_OPTIONS);
    if (userOptions != null) options.putAll(userOptions);
    
    bytecodeProvider = new TypeLoaderBytecodeProvider(
      ProcyonUtil.getTypeLoader(), outDir.getPath()
    );
    resultSaver = new ResultSaver(outDir, bytecodeProvider, false);
    fernflower
      = new Fernflower(bytecodeProvider, resultSaver, options, logger);
    structContext = fernflower.getStructContext();
    lazyLoader = new LazyLoader(bytecodeProvider);
    contextUnits = UNITS.getValue(structContext);
    relatedClassFinder = new DefaultRelatedClassFinder(
      Thread.currentThread().getContextClassLoader()
    );
  }
  
  
  private File createTempDir(String className) {
    try {
      return PosixFileInputStream.createTemporaryDirectory(format(
        "decompiler_%s", className
      ));
    } catch (Exception ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  public void decompileContext() {
    try {
      fernflower.decompileContext();
    } finally {
      if (clearContext) fernflower.clearContext();
    }
  }
  
  public void addSpace(File file, boolean isOwn) {
    structContext.addSpace(file, isOwn);   
  }
  
   /**
    @param String ????     Path to base directory
       e.g.  /external_sd/_projects/sdk/bsh/trunk/out
             /external_sd/_projects/sdk/lib
             /data/local/tmp_clazzes
    
    @param String path     Base-relative path to the PACKAGE directory
                           for the element   (no leading/trailing '/')
       e.g.  com/example
             com/strobel/assembler/metadata
             
             
    @param File file       Filepath for the element??,
       e.g.  com/example/MyClazz.class
             lib/abc-aibs.jar
             bsh-mod.jar
             
     @param String name    Item name
       e.g.  entry name
             com/example/MyClazz.class
             META-INF/MANIFEFT.MF
             
     @param int type 
       from org.jetbrains.java.decompiler.struct.ContextUnit:
         ContextUnit.TYPE_FOLDER = 0;
         ContextUnit.TYPE_JAR = 1;
         ContextUnit.TYPE_ZIP = 2;
  */
  @Nonnull public
  Pair<ContextUnit,Map<String,Pair<StructClass,LazyLoader.Link>>>
  addArchive(ZipInputStream archive, @Nonnull String relPackageDirPath,
  @Nonnull File dirOrZipFileForExtCheckOnly, boolean own) 
    throws IOException
  {
    String path = relPackageDirPath;
    File file = dirOrZipFileForExtCheckOnly;
    int type = ContextUnit.TYPE_ZIP;
    
    Map<String,Pair<StructClass,LazyLoader.Link>> classMetaMap
      = new TreeMap<>();
    String unitKey
      = new File(new File(path), file.getName()).getPath();
    // Unit contains multiple classes (dir or archive)
    boolean isNew = ! contextUnits.containsKey(unitKey);
    ContextUnit unit = (isNew)
      ? new ContextUnit(
          type, // int type
          path, // String archivePath
          file.getName(), // String filename
          own, // boolean own
          resultSaver, // IResultSaver resultSaver
          fernflower // IDecompiledData decompiledData
        )
      : contextUnits.get(unitKey);
    if (isNew) contextUnits.put(unitKey, unit);
    
    ZipEntry entry;
    while ((entry = archive.getNextEntry()) != null) {
      String name = entry.getName();
      if (! entry.isDirectory()) {
        if (name.endsWith(".class")) {
          byte[] bytes = IOUtils.toByteArray(archive);
          StructClass cl = new StructClass(bytes, own, lazyLoader);
          System.err.printf(
            "  - created StructClass<%s>  (res: %d bytes)\n",
            cl.qualifiedName, bytes.length
          );
          structContext.getClasses().put(cl.qualifiedName, cl);
          unit.addClass(cl, name);
          LazyLoader.Link link = new LazyLoader.Link(
            LazyLoader.Link.ENTRY, 
            file.getAbsolutePath(), 
            name
          );
          lazyLoader.addClassLink(cl.qualifiedName, link);
          classMetaMap.put(cl.qualifiedName, Pair.of(cl, link));
        }
      }
    }
    return Pair.of(unit, classMetaMap);
  }
  
  
  @Nonnull public
  Pair<ContextUnit,Map<String,Pair<StructClass,LazyLoader.Link>>>
  addBytecodeClass(String className, byte[] classBytes, boolean own)
    throws IOException
  {
    String classNameAsPath = ClassInfo.classNameToPath(
      (StringUtils.endsWith(className, ".class"))
        ? StringUtils.substringBeforeLast(className, ".class")
        : className
    );
    System.err.printf(
      "addBytecodeClass:  classNameAsPath = \"%s\"\n", classNameAsPath
    );
    
    // pkg as path ?
    String path = (classNameAsPath.indexOf('/') != -1)
      ? StringUtils.substringBeforeLast(classNameAsPath, "/")
      : "";
    System.err.printf(
      "addBytecodeClass:  path = \"%s\"\n", path);
    
    String simpleName = (classNameAsPath.indexOf('/') != -1)
      ? StringUtils.substringAfterLast(classNameAsPath, "/")
      : classNameAsPath;
    System.err.printf(
      "addBytecodeClass:  simpleName = \"%s\"\n", simpleName);
    
    File file = new File(
      new File(outDir.getPath().concat("/").concat(path)),
      simpleName.concat(".class")
    );
    System.err.printf("addBytecodeClass:  file = File(%s)\n", file);
    
    int type = ContextUnit.TYPE_FOLDER;
    if (file.exists()
    && StringUtils.endsWith(file.getName(), ".class")
    && ( file.getPath().contains("/temp/") 
      || file.getPath().contains("/tmp")))
    {
      System.err.printf("Deleting existing file: [%s]\n", file);
      System.err.printf(
        "Success: %s\n", String.valueOf(file.delete()));
    }
    
    System.err.printf("Writing %d bytes to [%s] ...\n", 
      classBytes.length, file);
    
    if (!new File(file.getParent()).exists()) {
      if (!new File(file.getParent()).mkdirs()) {
        throw new IOException(String.format(
          "mkdirs() failed for [%s]", file.getParent()
        ));
      }
    }

    FileUtils.writeByteArrayToFile(file, classBytes);
    System.err.printf("Finished writing: [%s]\n", file);
      
    Map<String,Pair<StructClass,LazyLoader.Link>> classMetaMap
      = new TreeMap<>();
    String unitKey = new File(new File(path),file.getName()).getPath();
    // Unit contains multiple classes (dir or archive)
    boolean isNew = ! contextUnits.containsKey(unitKey);
    ContextUnit unit = (isNew)
      ? new ContextUnit(
          type, // int type
          StringUtils.substringBefore(
            classNameAsPath, "/".concat(file.getName())
          ), // path, // String archivePath
          file.getName(), // String filename
          own, // boolean own
          resultSaver, // IResultSaver resultSaver
          fernflower // IDecompiledData decompiledData
        )
      : contextUnits.get(unitKey);
    if (isNew) contextUnits.put(unitKey, unit);
    
    String name = classNameAsPath.concat(".class");
    StructClass cl = new StructClass(classBytes, own, lazyLoader);
    System.err.printf(
      "  - created StructClass<%s>  (res: %d bytes)\n",
      cl.qualifiedName, classBytes.length
    );
    structContext.getClasses().put(cl.qualifiedName, cl);
    unit.addClass(cl, name);
    LazyLoader.Link link = new LazyLoader.Link(
      LazyLoader.Link.ENTRY, file.getAbsolutePath(), name
    );
    lazyLoader.addClassLink(cl.qualifiedName, link);
    classMetaMap.put(cl.qualifiedName, Pair.of(cl, link));
    return Pair.of(unit, classMetaMap);
  }
  
  @Override
  public void close() {
    fernflower.clearContext();
  }
}




public class Decompiler {
  
  static boolean ALL_OWN_DEFAULT = false;
  static boolean SKIP_RELATED_DEFAULT = true;
  static boolean DECOMPILE_DEFAULT = true;
  
  // Experiments
  static boolean EXPER_EXTERNAL_NAME_IS_CANON_NAME = false;
  static boolean LOAD_CLASSES = true;
  static boolean ADD_SPACE_BEFORE_LOADING_CLASS = false;
  static boolean ADD_SPACE = true;
  
  static final Object SIG_externalPath_internalPath_qualifiedClassName = 3;
  static final Object SIG_externalPath_qualifiedClassName = 2;
  static Object var_IBP_getBytecode_SIG;
  static Method mtd_IBP_getBytecode;
  
  public static byte[] invoke_IBytecodeProvider_getBytecode(final IBytecodeProvider p,
    final String externalPath, final String internalPath, 
    final String qualifiedClassName)
  {
    found:
    while (mtd_IBP_getBytecode == null) {
      for (final Method mtd: IBytecodeProvider.class.getMethods()) {
        if (! "getBytecode".equals(mtd.getName())) continue;
        final Class<?>[] paramTypes = mtd.getParameterTypes();
        final int numParams = paramTypes.length;
        if (! (numParams == 2 || numParams == 3)) continue;
        if (! paramTypes[0].isAssignableFrom(String.class)) continue;
        if (! paramTypes[1].isAssignableFrom(String.class)) continue;
        mtd_IBP_getBytecode = mtd;
        if (numParams == 3 && paramTypes[2].isAssignableFrom(String.class)) {
          var_IBP_getBytecode_SIG = SIG_externalPath_internalPath_qualifiedClassName;
        } else {
          var_IBP_getBytecode_SIG = SIG_externalPath_qualifiedClassName;
        }
        break found;
      }
      throw new UnknownError(String.format(
        "Neither expected method variant of 'getBytecode' " +
        "[(String,String) or (String,String,String)] was found in the interface %s; " +
        "the available (public) methods are:\n  - %s",
        IBytecodeProvider.class.getName(),
        StringUtils.join(IBytecodeProvider.class.getMethods(), "\n  - ")
      ));
    }
    try {
      return (byte[]) mtd_IBP_getBytecode.invoke(
        p,
        (var_IBP_getBytecode_SIG == SIG_externalPath_qualifiedClassName)
          ? new Object[]{ externalPath, qualifiedClassName }
          : new Object[]{ externalPath, internalPath, qualifiedClassName }
      );
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public static FernflowerEnvironment decompile(String targetClassName)
  {
    return decompile(
      targetClassName, (FernflowerEnvironment) null);
  }
  
  public static FernflowerEnvironment decompile(String targetClassName,
  @Nullable FernflowerEnvironment env) {
    return decompile(
      targetClassName, env, ALL_OWN_DEFAULT);
  }
  
  public static FernflowerEnvironment decompile(String targetClassName,
  @Nullable FernflowerEnvironment env, boolean allOwn) {
    return decompile(
      targetClassName, env, allOwn, SKIP_RELATED_DEFAULT);
  }
  
  public static FernflowerEnvironment decompile(String targetClassName,
  @Nullable FernflowerEnvironment env, boolean allOwn,
  boolean skipRelated) {
    return decompile(
      targetClassName, env, allOwn, skipRelated, DECOMPILE_DEFAULT);
  }
  
  public static FernflowerEnvironment decompile(String targetClassName,
  boolean allOwn, boolean skipRelated, boolean decompile) {
    return decompile(
      targetClassName, (FernflowerEnvironment) null,
      allOwn, skipRelated, decompile);
  }
  
  public static FernflowerEnvironment decompile(String targetClassName,
  @Nullable FernflowerEnvironment env, final boolean allOwn, 
  final boolean skipRelated, final boolean decompile)
  {
    if (env == null) env = new FernflowerEnvironment(
      targetClassName, System.err, (Map<String, Object>) null
    );
    final File dest = env.outDir;
    final PrintStreamLogger logger = env.logger;
    final Fernflower ff = env.fernflower;
    final StructContext sctx = env.structContext;
    final LazyLoader loader = env.lazyLoader;
    final Map<String, ContextUnit> contextUnits = env.contextUnits;
    final Set<String> classes = new TreeSet<String>();
    classes.add(targetClassName);
    if (!skipRelated) {
      env.relatedClassFinder.tryFindRelated(targetClassName, classes);
    }
    System.err.printf(
      "*** Related classes: ***\n  - %s\n\n",
      StringUtils.join(classes, ",\n  - "));
    
    for (final String className : classes) {
      System.err.println("\n=====================================\n");
      
      if (EXPER_EXTERNAL_NAME_IS_CANON_NAME) System.err.println(
        "Note: flag enabled: [EXPER_EXTERNAL_NAME_IS_CANON_NAME] "
        + "\u001b[1;31m[EXPERIMENTAL]\u001b[0m");
      
      final String externalName = (EXPER_EXTERNAL_NAME_IS_CANON_NAME)
        ? className
        : (env.outDir.getPath().concat("/")
            .concat(ClassInfo.classNameToPath(className, "class")));
      final String internalName = ClassInfo.classNameToPath(className, "class");
      final String classNameAsPath = className.replace('.', '/');      
        // = ClassInfo.classNameToPath(className);
      
      final File fileForClassBytes
        = new File(dest, classNameAsPath.concat(".class"));
      
      final boolean own = allOwn || targetClassName.equals(className);
      final File pkgDir = new File(fileForClassBytes.getParent());
      System.err.printf("pkgdir = %s\n", pkgDir);
      
      if (!pkgDir.exists()) {
        pkgDir.mkdirs();
      }
      
      byte[] classBytes = null;
      /*
      try {
      /***/
        System.err.printf("calling getBytecode(%s, %s)\n",
          externalName, classNameAsPath);
        classBytes = invoke_IBytecodeProvider_getBytecode(
          env.bytecodeProvider, externalName, internalName, classNameAsPath
        );
      /*
      } catch (IOException ioe) {
        ioe.printStackTrace();
        env.errors.add(ioe);
        System.err.printf(
          "Failed to getBytecode() for [%s]: %s\n", className, ioe);
        throw Reflector.Util.sneakyThrow(ioe); // continue;
      }
      /***/
      
      if (ADD_SPACE_BEFORE_LOADING_CLASS) {
        System.err.printf(
          "addSpace(fileForClassBytes: %s, own: %s) "
          + "[ADD_SPACE_BEFORE_LOADING_CLASS] "
          + "\u001b[1;31m[EXPERIMENTAL]\u001b[0m\n",
        fileForClassBytes, Boolean.valueOf(own));
        env.addSpace(fileForClassBytes, own);
      }
      
      Pair<ContextUnit,Map<String,Pair<StructClass,LazyLoader.Link>>> 
        pair;
      
      if (LOAD_CLASSES) {
        try {        
          pair = env.addBytecodeClass(className, classBytes, own);
        } catch (IOException ioe2) {
          ioe2.printStackTrace();
          env.errors.add(ioe2);
          continue;
        }
        env.results.add(pair);
      }

      if (ADD_SPACE && !ADD_SPACE_BEFORE_LOADING_CLASS) {
        System.err.printf("addSpace(fileForClassBytes: %s, own: %s)\n",
        fileForClassBytes, Boolean.valueOf(own));
        env.addSpace(fileForClassBytes, own);
      }
    }
    
    if (decompile) {
      env.decompileContext();
    }
    if (env.clearContext) {
      System.err.println(
        "%s is being close()`d because property 'clearContext' == true"
      );
      env.close();
    }
    return env;
  }
  
  
  @Nullable
  public static FernflowerEnvironment decompile(Object o) {
    if (o instanceof CharSequence) {
      return decompile(o instanceof String? (String)o: o.toString());
    }
    if (o instanceof ClassIdentifier) {
      return decompile(
       ((ClassIdentifier)o).getTargetClass().getName());
    }
    if (o instanceof Class) {
      return decompile(((Class<?>)o).getName());
    }
    throw new UnsupportedOperationException(format(
      "Cannot decompile instance of type \"%s\": "
        + "Not recognized", (o != null)? o.getClass().getName(): "null"
    ));
  }
  
  @Obsolete(value = 
    "Use CollectionUtil.toArray(T... elems) for comparable behavior.",
    replacement = @MethodTag(
      type = CollectionUtil.class,
      desc = "static <T> T[] toArray(T...)"
    )
  )
  public static <T> T[] array(T... items) {
    return Decompiler.<T>array(Object.class, items);
  }
  
  @Obsolete(
    replacement = @MethodTag(
      type = CollectionUtil.class,
      desc = "static <T> T[] toArray(T...)"
    )
  )
  public static <T> T[] array(Class<? super T> failsafeClass,
  T... items)
  {
    int len = items != null? items.length: 0;
    if (len == 0) {
      return (T[]) (Object) (
        ((failsafeClass != null)
          ? ((Object[]) Array.newInstance(failsafeClass, 0))
          : ((Object[]) new Object[0]))
      );
    }
    Class<?> arrCls = null, lastConcrete = null, itemCls = null;
    checkRemainingItems:
    for (T item: items) {
      if (item == null) continue;
      if (arrCls == null) {
        arrCls = item.getClass(); lastConcrete = arrCls; continue;
      }
      itemCls = item.getClass();
      if(itemCls==arrCls || arrCls.isAssignableFrom(itemCls)) continue;
      System.err.printf("arrCls = %s; lastConcrete = %s\n",
        arrCls.getName(), lastConcrete.getName());
      nextConcrete:
      for(Class concrete:new Class<?>[]{lastConcrete,itemCls,arrCls}) {
        System.err.printf("* Hierarchy: %s\n", concrete);
        concreteSuper:
        while (concrete != Object.class) {
          System.err.printf("  - Considering %s...\n", concrete);
          if (concrete.isAssignableFrom(itemCls)
          &&  concrete.isAssignableFrom(arrCls)) {
            arrCls = concrete;
            if (!concrete.isInterface()) lastConcrete = concrete;
            continue checkRemainingItems;
          }
          for (Class<?> iface: concrete.getInterfaces()) {
            System.err.printf("    - Considering %s...\n", iface);
            if (iface.isAssignableFrom(itemCls)
            &&  iface.isAssignableFrom(arrCls)) {
              arrCls = iface;
              if (! concrete.isInterface()) lastConcrete = concrete;
              continue checkRemainingItems;
            }
          }
          concrete = concrete.getSuperclass();
        }
      } // for concrete in [ lastConcrete, itemCls ]
    } // for item: items
    if (arrCls != failsafeClass && failsafeClass != null
    &&  arrCls.isAssignableFrom(failsafeClass)) arrCls = failsafeClass;
    T[] arrOfSizeZero = (T[]) Array.newInstance(arrCls, 0);
    return Arrays.copyOf(items, items.length,
      (Class<? extends T[]>) (Object) arrOfSizeZero.getClass());
  }
  
}
