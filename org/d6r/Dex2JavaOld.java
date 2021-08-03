package org.d6r;

import java.io.ByteArrayOutputStream;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import com.android.dex.Dex;
import com.android.dex.ClassDef;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm3.ClassVisitor;
import org.objectweb.asm3.ClassWriter;

import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexException;
import com.googlecode.dex2jar.ir.ET;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.reader.io.DataIn;
import com.googlecode.dex2jar.v3.AnnotationNode;
import com.googlecode.dex2jar.v3.ClassVisitorFactory;
import com.googlecode.dex2jar.v3.Dex2AsmAnnotationAdapter;
import com.googlecode.dex2jar.v3.DexExceptionHandler;
import com.googlecode.dex2jar.v3.DexExceptionHandlerImpl;
import com.googlecode.dex2jar.v3.IrMethod2AsmMethod;
import com.googlecode.dex2jar.v3.Main;
import com.googlecode.dex2jar.v3.V3;
import com.googlecode.dex2jar.v3.V3ClassAdapter;
import com.googlecode.dex2jar.v3.V3CodeAdapter;
import com.googlecode.dex2jar.v3.V3FieldAdapter;
import com.googlecode.dex2jar.v3.V3InnerClzGather;
import com.googlecode.dex2jar.v3.V3MethodAdapter;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.lang.annotation.RetentionPolicy;
import org.d6r.Dex2JavaOld.Out;
import java8.util.function.BiConsumer;
import java.util.IdentityHashMap;
import org.apache.commons.lang3.tuple.Pair;


public class Dex2JavaOld {
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Out {
    String value() default "";
  }
  
  public static List<Pair<Object, Throwable>> errors = new ArrayList<>();
  public static boolean dumpErrors = false;
    
  public static class Args {
    @Nullable
    public ByteArrayOutputStream errBaos;
    public ByteArrayOutputStream jarBaos;
    public List<byte[]> jars = new ArrayList<>();
    @Nullable
    DexExceptionHandler handler;
    
    public byte[] input;
    
    public boolean overwrite = true;
    public boolean debugInfo = true;
    public boolean reuseReg = false;
    public boolean topologicalSort = true;
    public boolean optmizeSynchronized = true;
    public boolean printIR = false;
    public boolean verbose = true;
    
    
    public int apiLevel = 19;
    public Set<String> classNames;
    
    
    // Output
    @Dex2JavaOld.Out public Map<Method, Exception> exceptionMapOutput;
    @Dex2JavaOld.Out public final List<Throwable> errors 
      = new ArrayList<Throwable>();
    @Dex2JavaOld.Out public BiConsumer<String, byte[]> classBytesHandler;
    @Dex2JavaOld.Out public V3InnerClzGather afa = new V3InnerClzGather();
    @Dex2JavaOld.Out public ClassVisitorFactory classVisitorFactory;
    @Dex2JavaOld.Out public V3 v3;
    @Dex2JavaOld.Out Map<String, byte[]> byteOutputMap;
  }
  
  
  
  public static class ClassVisitorFactoryImpl
           implements ClassVisitorFactory
  {
    public static class ClsWriter 
                extends org.objectweb.asm3.ClassWriter
    {
      protected String name;
      protected byte[] data;
      protected BiConsumer<String, byte[]> onDone;
      
      public ClsWriter(String name, BiConsumer<String, byte[]> onDone) {
        super(ClassWriter.COMPUTE_MAXS);
        this.name = name;
        this.onDone = onDone;
      }
      
      public ClsWriter(String name) {
        this(name, (BiConsumer<String, byte[]>) null);
      }
      
      public boolean isDone() {
        return data != null;
      }
      
      public void setOnDone(BiConsumer<String, byte[]> onDone) {
        this.onDone = onDone;
        if (isDone() && onDone != null) onDone.accept(name, data);
      }
      
      @Override
      public void visitEnd() {
        super.visitEnd();
        try {
          this.data = this.toByteArray();
          if (isDone() && onDone != null) onDone.accept(name, data);
        } catch (Throwable e) {
          e.printStackTrace(System.err);
        }
      }
    }
    
    protected final Dex2JavaOld d2j;
    protected BiConsumer<String, byte[]> onDone;
        
    protected Map<String, ClsWriter> writers 
        = new IdentityHashMap<String, ClsWriter>();

    public ClassVisitorFactoryImpl(Dex2JavaOld d2j, 
    BiConsumer<String, byte[]> onDone) 
    {
      this.d2j = d2j;
      this.onDone = onDone;
    }
    
    @Override
    public ClassVisitor create(String name) {
      ClsWriter cw = new ClsWriter(name, onDone);
      writers.put(name, cw);
      System.err.printf(
        "Created %s(\"%s\", %s)\n", 
        cw.getClass().getSimpleName(), name, onDone
      );
      return cw;
    }
  }
  
  public static class UsageException extends RuntimeExceptionCompat {
    public UsageException(String message) {
      super(message);
    }
  }
  
  // -------------------- END NESTED CLASSES --------------------
  
  protected DexFileReader reader;
  protected DexExceptionHandler exceptionHandler;
  protected int readerConfig;
  protected boolean verbose = false;
  protected int v3Config;
  protected Set<String> dirs = new HashSet<String>();
  protected BiConsumer<String, byte[]> onDone;
  protected Object dest;
  protected V3InnerClzGather afa;
  protected Args args;
  
  // -------------------- END INSTANCE FIELDS --------------------
  
  public Dex2JavaOld(DexFileReader reader) {
    super();
    this.reader = reader;
  }
  
  // -------------------- END CONSTRUCTORS --------------------
  
  public static Dex2JavaOld from(DexFileReader reader) {
    return new Dex2JavaOld(reader);
  }
  
  public static Dex2JavaOld from(byte[] in) throws IOException {
    return from(new DexFileReader(in));
  }

  public static Dex2JavaOld from(DataIn in) throws IOException {
    return from(new DexFileReader(in));
  }

  public static Dex2JavaOld from(File in) throws IOException {
    return from(new DexFileReader(in));
  }

  public static Dex2JavaOld from(InputStream in) throws IOException {
    return from(new DexFileReader(in));
  }

  public static Dex2JavaOld from(String in) throws IOException {
    return from(new File(in));
  }
  
  // -------------------- END STATIC FACTORY METHODS --------------------
  
  public static byte[] convertOne(byte[] dexBytes, String className) { 
    Map<String, byte[]> outMap = dex2jari(
      Arrays.asList(dexBytes), className
    );
    return outMap.size() > 0? outMap.values().iterator().next(): null;
  }
  
  public static 
  Map<String, byte[]> dex2jari(final String... classNames) {
    return dex2jari(Collections.emptyList(), classNames);
  }
  
  public static
  Map<String, byte[]> dex2jari(final Iterable<byte[]> dexByteSources, 
  final String... classNames) 
  {
    final Dex2JavaOld.Args args = new Dex2JavaOld.Args();
    final Map<String, byte[]> byteMap 
      = (args.byteOutputMap = new TreeMap<String, byte[]>());
      
    args.errBaos = new ByteArrayOutputStream();
    
    
    List<Dex> dexes = new ArrayList<>();
    List<Set<String>> dexesClassNames = new ArrayList<Set<String>>();
    int index = -1;
    for (byte[] dexByteSource: dexByteSources) {
      index++;
      try {
        Dex dex = new Dex(dexByteSource);
        dexes.add(dex);
        DexUtil dexUtil = new DexUtil(dex);        
        Set<String> dexClassNames = new HashSet<String>(Arrays.asList(
          dexUtil.getClassNames()
        ));
        dexesClassNames.add(dexClassNames);
      } catch (IOException ioe) {
        new IOException(String.format(
          "Couldn't load byte array at index %d: %s",
          index, ioe
        )).printStackTrace();
      }
    }
    
    args.classBytesHandler = new 
      BiConsumer<String, byte[]>() {
        @Override public void accept(String nameAsPath, byte[] clsBytes) {
          String entryName = nameAsPath.concat(".class");
          byteMap.put(entryName, clsBytes);
          if (args.verbose) System.err.printf(
            "Finished class file (%5d bytes): %s\n",
            clsBytes.length,
            entryName
          );
        }
      };
    
    Dex dex = null;
    Map<Dex, List<String>> dexToClasNamesMap 
      = new IdentityHashMap<Dex, List<String>>();
    
    for (String className: classNames) {
      try {
        boolean found = false;
        for (int i=0; i<dexes.size(); i++) {
          dex = dexes.get(i);
          if (dexesClassNames.get(i).contains(className)) {
            System.err.printf(
              "[INFO] Found class '%s' in byte array dex source at idx %d\n",
              className, i
            );
          }
          found = true;
          break;
        }
        if (!found) {
          Class<?> cls = Class.forName(
            className, false, Thread.currentThread().getContextClassLoader()
          );
          dex = getDex(cls);
        }
        if (dex == null) {
          System.err.printf("class '%s': dex == null!", className);
        }
        
        List<String> classNamesInDex = dexToClasNamesMap.get(dex);
        if (classNamesInDex == null) {
          dexToClasNamesMap.put(
            dex, (classNamesInDex = new ArrayList<String>())
          );          
        }
        classNamesInDex.add(String.format(
          "L%s;", className.replace('.', '/')
        ));        
      } catch (Throwable e) { 
        e.printStackTrace();
      }
    }
    
    for (Map.Entry<Dex, List<String>> e: dexToClasNamesMap.entrySet()) {
      dex = e.getKey();
      List<String> classNamesInDex = e.getValue();
      args.classNames = new HashSet<String>(
        (Collection<String>) (Object) classNamesInDex
      );
      //args.jarBaos = new ByteArrayOutputStream();
      //args.input = dex.getBytes();
      Dex2JavaOld.convert(args, dex);
      //byte[] jar = args.jarBaos.toByteArray();
      //args.jars.add(jar);
    }
    
    return byteMap;
  }
  
  public static Args convert(@Nonnull Args args, Dex dex) {
    
    if (args == null) throw new UsageException("args == null");
    
    if (args.debugInfo && args.reuseReg) throw new UsageException(
      "args.debugInfo & args.reuseReg cannot be used together"
    );
    
    DexFileReader reader = readerCache.get(dex);
    if (reader == null) {
      reader = new DexFileReader(
        args.input != null? (byte[]) args.input: (byte[])dex.getBytes()
      );
      reader.setApiLevel(args.apiLevel);
      readerCache.put(dex, reader);
    }
    
    DexExceptionHandler handler
      = (args.handler != null)
          ? args.handler
          : (DexExceptionHandler) new DexExceptionHandlerImpl();
    
    if (handler instanceof DexExceptionHandlerImpl) {
      ((DexExceptionHandlerImpl) handler).skipDebug(! args.debugInfo);
    }
    
    Dex2JavaOld d2j = d2jCache.get(dex);
    if (d2j == null) {
      d2j = Dex2JavaOld.from(reader);
      d2jCache.put(dex, d2j);
    }
    d2j = d2j.withExceptionHandler(handler)
        .reUseReg(args.reuseReg)
        .topoLogicalSort(args.topologicalSort)
        .skipDebug(! args.debugInfo)
        .optimizeSynchronized(args.optmizeSynchronized)
        .printIR(args.printIR)
        .withDoneHandler(args.classBytesHandler)
        .verbose(args.verbose)
        .withArgs(args);
    
    try {
      d2j.doTranslate(null, dex);
    } catch (Throwable ex) {
      args.errors.add(ex);
      if (args.verbose) ex.printStackTrace();
    }
    
    Map<Method,Exception> exceptions 
      = (handler instanceof DexExceptionHandlerImpl)
          ? ((DexExceptionHandlerImpl) handler).getExceptions()
          : Collections.<Method,Exception>emptyMap();
    
    if (args.exceptionMapOutput == null) {
      args.exceptionMapOutput = new HashMap<Method, Exception>(exceptions);
    } else {
      args.exceptionMapOutput.putAll(exceptions);
    }
    
    
    
    if (exceptions != null && exceptions.size() > 0) {
      if (args.verbose) {
        try (PrintStream ps = new PrintStream(args.errBaos)) {
          for (Map.Entry<Method, Exception> entry: exceptions.entrySet()) {
            Method method = entry.getKey();
            Throwable ex = (Throwable) entry.getValue();
            ps.printf(
              "  - in method: %s\n" +
              "    %s\n" +
              "    exception: %s: %s\n",
              method, 
              Debug.ToString(method),
              ex.getClass().getName(), 
              ex.getMessage()
            );
            ex.printStackTrace(ps);
          }
        
          if (dumpErrors) {
            for (Map.Entry<Method, Exception> entry: exceptions.entrySet()) {
              Method method = entry.getKey();
              Throwable ex = (Throwable) entry.getValue();
              System.err.printf(
                "  - in method: %s\n" +
                "    exception: %s: %s\n",
                method, ex.getClass().getName(), ex.getMessage()
              );
              ex.printStackTrace(System.err);
            }            
          } // if dumpErrors
        } catch (Exception iow) {
          iow.printStackTrace();
        }
      } // if verbose
    } // if exceptions count > 0
    
    System.err.printf(
      args.errors.isEmpty() 
      && (exceptions == null || exceptions.size() == 0)
        ? "Finished.\n"
        : String.format("Finished with %d errors\n", exceptions.size())
    );
    return args;
  }
  
  // -------------------- END PUBLIC ENTRY POINTS --------------------
  
  static Map<Dex, V3InnerClzGather> gatherCache
    = new IdentityHashMap<>();
  static Map<Dex, DexFileReader> readerCache
    = new IdentityHashMap<>();
  static Map<Dex, Dex2JavaOld> d2jCache
    = new IdentityHashMap<>();
    
  public static void writeInnerClassCaches() {
    try {
      File serialPersistDir 
        = new File("/data/local/dexSerialData/innerClassGathers");
      if (!serialPersistDir.exists()) {
        serialPersistDir.mkdirs();
      }
      for (Map.Entry<Dex, V3InnerClzGather> e: gatherCache.entrySet()) {
        Dex dex = e.getKey();
        V3InnerClzGather icg = e.getValue();
        try {
          int dexSize 
            = Reflect.<ByteBuffer>getfldval(dex, "data").capacity();
          ClassDef firstDef = dex.classDefs().iterator().next();
          String firstDefClassName 
            = dex.typeNames().get(firstDef.getTypeIndex());
          Map.Entry<Integer, String> dexKey 
            = Pair.of(Integer.valueOf(dexSize), firstDefClassName); 
          System.err.printf(
            "dex key = %s [%d inner classes]\n", 
            dexKey, icg.getClasses().size()
          ); 
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos);
          oos.writeObject(icg); 
          oos.flush(); 
          oos.close(); 
          byte[] serialData = baos.toByteArray(); 
          
          System.err.printf(
            "  - Serialized data length: %d bytes\n", serialData.length
          );
          String fileName = String.format(
            "dex_%012d_%s.icg.ser", 
            dexSize, ClassInfo.typeToName(firstDefClassName)
          ); 
          File outputFile = new File(serialPersistDir, fileName); 
          FileUtils.writeByteArrayToFile(outputFile, serialData); 
          if (outputFile.exists()
          &&  outputFile.length() == serialData.length) { 
            System.err.printf(
              "  - Wrote to [%s], size = %d\n",
              outputFile.getPath(), outputFile.length()
            );
          } else { 
            System.err.printf(
              "  - Failed to write %d bytes to [%s]: %s\n",
              serialData.length, outputFile.getPath(), 
              outputFile.exists()
                ? (outputFile.length() == serialData.length
                    ? "Unknown error (length is correct)"
                    : String.format(
                        "Length incorrect; expected: %s, actual: %d",
                        serialData.length, outputFile.length()
                      )
                  )
                : "Unable to create output file"
            );
          }
        } catch (Throwable ex) {
          if (dumpErrors) ex.printStackTrace();
          errors.add(Pair.of(e, ex));
          continue;
        }
      } // end for Map.Entry<Dex, V3InnerClzGather> e: entrySet
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
  } // fn
  
  protected void doTranslate(final Object dest, Dex dex) 
    throws IOException 
  {
    if (reader.isOdex()) {
      throw new DexException(
        "dex-translator not support translate an odex file,"
        + " please refere smali http://code.google.com/p/smali/ "
        + "to convert odex to dex"
      );
    }

    
    ClassVisitorFactory cvf 
      = new ClassVisitorFactoryImpl((Dex2JavaOld) this, this.onDone);
    args.classVisitorFactory = cvf;
    
    V3InnerClzGather afa = gatherCache.get(dex);
    if (afa == null) {
      afa = new V3InnerClzGather();      
      if (args.verbose) System.err.println("Visiting inner classes ...");
      reader.accept(
        afa, DexFileReader.SKIP_CODE 
      );
      gatherCache.put(dex, afa);
    }

    // Prep inner classes
    // System.err.println("Converting ...");
    try {
      reader.accept(
        (args.v3 = new V3(
          afa.getClasses(), // Map<String, V3InnerClzGather.Clz> innerClz
          exceptionHandler, // DexExceptionHandler exceptionHandler
          cvf, // ClassVisitorFactory classVisitorFactory
          this.v3Config// int config
        )
        {
          @Override
          public DexClassVisitor visit(int access_flags, String className,
          String superClass, String[] interfaceNames)
          {
            boolean process = false;
            if (args.classNames != null) {
              if (args.classNames.contains(className)) {
                process = true;
              } else {
                for (String includedClassName: args.classNames) {
                  if (className.indexOf(includedClassName) == 0) {
                    process = true;
                    break;
                  }
                }
              }
            }
            if (! process) return null;
            if (verbose) System.err.printf(
              "Processing: %s ...\n", className
            );
            return super.visit(
              access_flags, className, superClass, interfaceNames
            );
          }
        }), 
        readerConfig
      );
    } catch (Exception e) {
      if (exceptionHandler == null) {
        throw e instanceof RuntimeException 
          ? (RuntimeException) e 
          : new RuntimeException(e);
      } else {
        exceptionHandler.handleFileException(e);
      }
    }
  }
  
  
  public DexExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }
  public DexFileReader getReader() {
    return reader;
  }
  public Object getDest() {
    return dest;
  }
  
  public Dex2JavaOld withOption(int constOptionBits, boolean enable) {
    this.v3Config = (enable)
      ? this.v3Config |   constOptionBits
      : this.v3Config & (~constOptionBits);
    return this;
  }
  public Dex2JavaOld withFlag(String name, Object value) {
    Reflect.setfldval(this, name, value);
    return this;
  }
  public Dex2JavaOld verbose() {
    return this.withFlag("verbose", true);
  }
  public Dex2JavaOld verbose(boolean flag) {
    return this.withFlag("verbose", flag);
  }
  public Dex2JavaOld reUseReg(boolean b) {
    return this.withOption(V3.REUSE_REGISTER, b);
  }
  public Dex2JavaOld topoLogicalSort(boolean b) {
    return this.withOption(V3.TOPOLOGICAL_SORT, b);
  }
  public Dex2JavaOld optimizeSynchronized(boolean b) {
    return this.withOption(V3.OPTIMIZE_SYNCHRONIZED, b);
  }
  public Dex2JavaOld printIR(boolean b) {
    return this.withOption(V3.PRINT_IR, b);
  }
  public Dex2JavaOld reUseReg() {
    return this.withOption(V3.REUSE_REGISTER, true);
  }
  public Dex2JavaOld optimizeSynchronized() {
    return this.withOption(V3.OPTIMIZE_SYNCHRONIZED, true);
  }
  public Dex2JavaOld printIR() {
    return this.withOption(V3.PRINT_IR, true);
  }
  public Dex2JavaOld withExceptionHandler(DexExceptionHandler exHandler) {
    return this.withFlag("exceptionHandler", exHandler);
  }
  public Dex2JavaOld withDoneHandler(BiConsumer<String, byte[]> onDone) {
    return this.withFlag("onDone", onDone);
  }
  public Dex2JavaOld withArgs(Args args) {
    return this.withFlag("args", args);
  }
  public Dex2JavaOld skipDebug(boolean enable) {
    this.readerConfig = (enable)
      ? this.readerConfig 
      : this.readerConfig & (~DexFileReader.SKIP_DEBUG);
    return this;
  }
  public Dex2JavaOld skipDebug() {
    this.readerConfig &= (~DexFileReader.SKIP_DEBUG);
    this.readerConfig &= (~DexFileReader.SKIP_DEBUG);
    return this;
  }
  public void setExceptionHandler(DexExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  
  protected void addDirEntry(String dir, ZipOutputStream zos) 
    throws IOException 
  {
    if (dirs.contains(dir)) return;
    dirs.add(dir);
    int i = dir.lastIndexOf('/');
    if (i > 0) {
      addDirEntry(dir.substring(0, i), zos);
    }
    zos.putNextEntry(new ZipEntry(dir + "/"));
    zos.closeEntry();
  }
  
  
  protected void saveTo(byte[] data, String name, Object dest) 
    throws IOException 
  {
    if (verbose) System.err.printf(
      "Writing %s: %s ...\n", 
      (dest instanceof ZipOutputStream)? "ZipEntry": "File",
      name
    );
    if (dest instanceof ZipOutputStream) {
      ZipOutputStream zos = (ZipOutputStream) dest;
      ZipEntry entry = new ZipEntry(name + ".class");
      int i = name.lastIndexOf('/');
      if (i > 0) {
        addDirEntry(name.substring(0, i), zos);
      }
      zos.putNextEntry(entry);
      zos.write(data);
      zos.closeEntry();
    } else {
      File dir = (File) dest;
      FileUtils.writeByteArrayToFile(new File(dir, name + ".class"), data);
    }
  }

  public void to(File file) throws IOException {
    if (file.exists() && file.isDirectory()) {
      this.dest = file;
      doTranslate(file, null);
    } else {
      OutputStream fos = FileUtils.openOutputStream(file);
      try {
        to(fos);
      } finally {
        IOUtils.closeQuietly(fos);
      }
    }
  }

  public void to(OutputStream os) throws IOException {
    ZipOutputStream zos = new ZipOutputStream(os);
    this.dest = zos;
    doTranslate(zos, null);
    zos.finish();
  }

  public void to(String file) throws IOException {
    to(new File(file));
  }
}