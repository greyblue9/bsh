package org.d6r;

import java.nio.charset.StandardCharsets;
import dx2.cf.direct.DirectClassFile;
import dx2.cf.direct.StdAttributeFactory;
import dx2.dex.DexOptions;
import dx2.dex.cf.CfOptions;
import dx2.dex.cf.CfTranslator;
import dx2.dex.file.ClassDefItem;
import dx2.dex.file.DexFile;
import dx2.util.ByteArray;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java8.util.concurrent.ForkJoinTask;
import java8.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import static org.d6r.Reflect.setfldval;

public class JavaToDex {

  static int TARGET_API_LEVEL = 21; // was: 18
  static Constructor<DexFile> dxDexFileCtor;
  static Class<?>[] dxDexFileCtorParamTypes;
  public static boolean DEBUG = "true".equals(System.getProperty("debug"));
  public static boolean WRITE_CLASSFILE = true;
  public static Method loadDex;
  public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
  public static final Charset UTF_8 = StandardCharsets.UTF_8;
  
  static DexOptions dexopts = new DexOptions();
  static Method translate0;
  static String CF_PATH = "/dev/null";
  
  static LazyMember<Constructor<? extends Callable<?>>> ANON_CALLABLE_CTOR
    = LazyMember.of("org.d6r.JavaToDex$1", "<init>", 
        byte[].class, String.class, DexFile.class, Entry.class
      );
  
  public static CfOptions newCfOpts() {
    CfOptions cfopts = new CfOptions();
    cfopts.localInfo = true;
    cfopts.optimize = true;
    return cfopts;
  }
  
  static {
    try {
      (dxDexFileCtor = (Constructor<DexFile>) (Constructor<?>)
        DexFile.class.getDeclaredConstructors()[0]).setAccessible(true);
      dxDexFileCtorParamTypes = dxDexFileCtor.getParameterTypes();
    } catch (Throwable ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      dxDexFileCtor = null;
      dxDexFileCtorParamTypes = null;
    }
    try {
      dexopts.targetApiLevel = TARGET_API_LEVEL;
      (translate0 = CfTranslator.class.getDeclaredMethod(
        "translate0", String.class, byte[].class, CfOptions.class,
        DexOptions.class       
      )).setAccessible(true);
    } catch (ReflectiveOperationException var5) {
      try {
        (translate0 = CfTranslator.class.getDeclaredMethod(
          "translate0", DirectClassFile.class, byte[].class, CfOptions.class,
          DexOptions.class, DexFile.class
        )).setAccessible(true);
      } catch (ReflectiveOperationException var4) {
        if ("true".equals(System.getProperty("printStackTrace"))) var4.printStackTrace();
      }
    }
  }
  
  public static DexFile createDxDexFile() {
    Object[] args = new Object[dxDexFileCtorParamTypes.length];
    if (dxDexFileCtorParamTypes.length != 0 
    &&  dxDexFileCtorParamTypes[0] == DexOptions.class) {
      DexOptions dxDexFile = new DexOptions();
      dxDexFile.targetApiLevel = TARGET_API_LEVEL;
      args[0] = dxDexFile;
    }
    DexFile dxDexFile1;
    try {
      dxDexFile1 = (DexFile) dxDexFileCtor.newInstance(args);
    } catch (ReflectiveOperationException ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      dxDexFile1 = null;
    }
    return dxDexFile1;
  }

  public static byte[] dexClassBytes(String name, byte[] bytes) {
    Pair<byte[], String> pair = dexBytes(name, bytes, false);
    return pair != null? pair.getKey(): null;
  }
  
  public static Pair<byte[], String> dexBytes(String name, byte[] bytes) {
    return dexBytes(name, bytes, true);
  }

  public static ClassDefItem dexClass(String name, byte[] bytes, DexFile df) 
  {
    ByteArray ba = new ByteArray(bytes);
    DirectClassFile dcf = new DirectClassFile(ba, CF_PATH, false);
    if (df == null) df = createDxDexFile();
    setfldval(dcf, "strictParse", Boolean.FALSE);
    dcf.setAttributeFactory(new StdAttributeFactory());
    Object baos = null;
    Object pw = null;
    try {
      ClassDefItem cdi = (ClassDefItem) (
        translate0.getParameterTypes().length == 5
        ? translate0.invoke(null, dcf, bytes, newCfOpts(), dexopts, df)
        : translate0.invoke(null, CF_PATH, bytes, newCfOpts(), dexopts)
      );
      return cdi;
    } catch (Throwable ex) { throw Reflector.Util.sneakyThrow(ex); }
  }

  public static Pair<byte[], String> dexBytes(String name, byte[] bytes,
  boolean dump)
  {
    ByteArrayOutputStream baos =  null;
    ByteArrayOutputStream dumpBaos = null;    
    PrintWriter dumpPw = null;
    Pair<byte[], String> pair;
    try {
      DexFile df = createDxDexFile();
      ByteArray ba = new ByteArray(bytes);
      DirectClassFile dcf = new DirectClassFile(ba, "/dev/null", false);
      setfldval(dcf, "strictParse", Boolean.FALSE);
      dcf.setAttributeFactory(new StdAttributeFactory());
      ClassDefItem cdi = (ClassDefItem) (
        translate0.getParameterTypes().length == 5
        ? translate0.invoke(null, dcf, bytes, newCfOpts(), dexopts, df)
        : translate0.invoke(null, CF_PATH, bytes, newCfOpts(), dexopts)
      );
      df.add(cdi);
      if (dump || DEBUG) {
        dumpPw = new PrintWriter(dumpBaos = new ByteArrayOutputStream());
      }
      df.setDumpWidth(Integer.MAX_VALUE);
      baos = new ByteArrayOutputStream();
      df.writeTo(baos, dumpPw, dump);
      byte[] dexBytes = baos.toByteArray();
      dexBytes[6] = (byte) '5'; // from 'dex\n 035\0'
      String output = null;
      if (dump || DEBUG) {
        dumpPw.flush();
        dumpBaos.flush();
        output = new String(dumpBaos.toByteArray(), UTF_8);
        if (DEBUG) System.err.println(output);
      }
      pair = Pair.of(dexBytes, output);
    } catch (Throwable ex) {
      throw Reflector.Util.sneakyThrow(ex); 
    } finally {
      if (dumpPw != null) IOUtils.closeQuietly(dumpPw);
      if (dumpBaos != null) IOUtils.closeQuietly(dumpBaos);
      if (baos != null) IOUtils.closeQuietly(baos);
    }
    return pair;
  }
  
  public static byte[] dexZip(Map<String, byte[]> zm, int threads, long timeoutSecs)
  {
    ExecutorService es = Executors.newFixedThreadPool(threads);
    Deque<Callable<?>> callables = new ArrayDeque<>();
    List<Future<Entry<String, Optional<Triple<?, ?, ClassDefItem>>>>> 
      futures = new ArrayList();
    DexFile df = createDxDexFile();
    
    for (Entry<String,byte[]> entry: zm.entrySet()) {
      String name = entry.getKey();
      int slash, dot, lastdot;
      boolean valid = false;
      if ((slash = name.indexOf('/')) != -1) {
        if ((dot = name.indexOf('.')) != -1) {
          if ((lastdot = name.lastIndexOf('.')) == dot) {
            if (".class".equals(name.subSequence(lastdot, name.length()))) {
              valid = true;
            } 
          }
        }
      } else {
        if ((dot = name.indexOf('.')) != -1) {
          name = new StringBuilder(name.length() + 6)
            .append(name.replace('.', '/'))
            .append(".class")
            .toString();
          valid = true;
        }
      }
      if (!valid) {
        System.err.printf("[INFO] Skipping non-class entry: [%s]\n", name);
        continue;
      }
      Callable callable = ANON_CALLABLE_CTOR.newInstance(
        entry.getValue(), 
        ClassInfo.typeToName(StringUtils.substringBefore(name, ".class")),
        df, 
        Pair.of(name, entry.getValue())
      ); 
      callables.add(callable);       
    };
    
    try {
      while (! callables.isEmpty()) { 
        Callable<?> callable = callables.poll();
        Future<?> rs = es.submit(callable);
        futures.add((Future) rs);
      };
      System.err.printf("Shutting down %s ...\n", es);
      es.shutdown(); 
      // System.err.printf("  - returned: %s\n", coln);
      es.awaitTermination(timeoutSecs, TimeUnit.SECONDS); 
    } catch (InterruptedException inte) {
      Thread.currentThread().interrupt();
      System.err.printf("*** Aborted: %s ***\n");
    } catch (Throwable e) {
      throw Reflector.Util.sneakyThrow(e);
    }
    
    List<ClassDefItem> cdis = new LinkedList<ClassDefItem>(); 
    
    for (Future<Entry<String, Optional<Triple<?, ?, ClassDefItem>>>> f: futures) { 
      Entry<String, Optional<Triple<?, ?, ClassDefItem>>>
        outcome = Reflect.getfldval(f, "outcome");
      System.err.printf(
        "%-5s [%s] %s: %s\n", 
        f.isDone() ? "done": "...", 
        Reflect.<Integer>getfldval(f, "state"), 
        (outcome != null)
          ? (outcome.getValue().isPresent()? "OK": "xx")
          : "??", 
        (outcome != null)
          ? outcome.getKey()
          : null
      );
      
      if (outcome != null && outcome.getValue().isPresent()) {
        ClassDefItem cdi = outcome.getValue().get().getRight();
        cdis.add(cdi);
        System.err.printf(
          "Adding %s ...\n", 
          ClassInfo.typeToName(
            cdi.getThisClass().getDescriptor().getString()
          )
        ); 
        try {
          cdis.add(cdi);
          df.add(cdi);
        } catch (Throwable e) { 
          e.printStackTrace();
          continue;
        }
      } else {
        try {
          final Entry<String, Optional<Triple<?, ?, ClassDefItem>>> entry = f.get();
          final Optional<Triple<?, ?, ClassDefItem>> optional = entry.getValue();
          final ClassDefItem cdi = optional.isPresent()
            ? optional.get().getRight()
            : null;
          if (cdi != null) {
            cdis.add(cdi);
            try {
              df.add(cdi);
            } catch (Throwable e) { 
              e.printStackTrace();
              continue;
            }
          }
        } catch (final InterruptedException ie) {
          ie.printStackTrace();
          Thread.currentThread().interrupt();
        } catch (final ExecutionException ee) {
          ee.printStackTrace();
        }
      }
    }
    
    final Writer humanOut = null;
    
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      df.writeTo(baos, humanOut, false);
      final byte[] dexBytes = baos.toByteArray();
      dexBytes[6] = (byte) '5';
      return dexBytes;
    } catch (IOException e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  
  public static Optional<Triple<byte[], DexFile,      List<Future<Pair<String,Optional<Triple<String,Entry<String,byte[]>,ClassDefItem>>>>>>>
  dex(Map<String,byte[]> zm)
  {
    return dex(zm, 6);
  }
  
  public static Optional<Triple<byte[], DexFile,     
  List<Future<Pair<String,Optional<Triple<String,Entry<String,byte[]>,ClassDefItem>>>>>>>
  dex(Map<String,byte[]> zm, int nThreads)
  {
    return dex(
      zm,
      null
    );
  }
  
  public static Optional<Triple<byte[], DexFile, List<Future<Pair<String,Optional<Triple<String,Entry<String,byte[]>,ClassDefItem>>>>>>>
  dex(Map<String,byte[]> zm, ExecutorService executor)
  {
    
    List<Entry<String, byte[]>> entList = new ArrayList<>(zm.entrySet());
    Iterator<Entry<String, byte[]>> it = entList.iterator();
    while (it.hasNext()) { 
      Entry<String, byte[]> entry = it.next();      
      String name = entry.getKey();
      if (!StringUtils.endsWith(name, ".class")) {
        it.remove();
        continue;
      }
      byte[] bytes = entry.getValue();
      if (bytes.length < 4) {
        System.err.printf(
          "[WARN] Severely truncated classfile: entry '%s'; skipping\n", name
        );
        it.remove();
        continue;
      }
      if (bytes[0] == (byte) 0xCA && bytes[1] == (byte) 0xFE 
      &&  bytes[2] == (byte) 0xBA && bytes[3] == (byte) 0xBE) {
        // valid sig
      } else {
        System.err.printf(
          "[WARN] Invalid magic on entry '%s'; skipping\n", name
        );
        it.remove();
        continue;
      }
    }
    
    Entry<String, byte[]>[] ents = entList.toArray(
      (Entry<String, byte[]>[]) new Entry<?, ?>[0]
    );
    
    System.err.printf("%d entries\n", ents.length);
    
    final DexFile df = createDxDexFile();
    
    List<
      Callable<Pair<String, Optional<Triple<String, Entry<String,byte[]>,
      ClassDefItem>>>>
    > callables = new LinkedList<>();
    
    int extSuffixLen = ".class".length();
    
    for (int i=0, len=ents.length; i<len; ++i) {
      
      final Entry<String,byte[]> ent = ents[i];    
      String name = ent.getKey();
      int nameLen = name.length();
      StringBuilder cnsb = new StringBuilder(name)
        .delete(nameLen - extSuffixLen, nameLen);
      int slashPos;
      while ((slashPos = cnsb.indexOf("/")) != -1) {
        cnsb.replace(slashPos, slashPos+1, ".");
      }
      final String className = cnsb.toString();
      System.err.printf("Adding task for: %s ...\n", className); 
      final byte[] clsBytes = ent.getValue();   
      
      Callable<
        Pair<String, Optional<Triple<String, Entry<String,byte[]>, ClassDefItem>>>> 
        callable = new Callable<
          Pair<String, Optional<Triple<String, Entry<String,byte[]>, ClassDefItem>>>>()
      {
        private final byte[] classBytes = clsBytes;
        private final String typeName = className;
        private ClassDefItem classDefItem;
        private final DexFile dexFileInProgress = df;
        private final Entry<String,byte[]> entry = ent;
        
        @Override 
        public Pair<String, 
        Optional<Triple<String, Entry<String,byte[]>, ClassDefItem>>> call() 
        {
          System.err.printf("Processing: %s ...\n", typeName);      
          boolean success = false;
          try { 
            classDefItem = dexClass(typeName, classBytes, dexFileInProgress);
          } catch (Throwable ex) {
            classDefItem = null;
            if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
            Throwable rc = ExceptionUtils.getRootCause(ex);
            if (rc == null) rc = ex;
            System.err.printf(
              "[WARN] Failed to translate class '%s': %s: %s\n",
              typeName, rc.getClass().getSimpleName(), rc.getMessage()
            );
          }
          if (classDefItem != null) {
            success = true;
          }
          if (!success) {
            return Pair.of(typeName, Optional.empty());
          }
          return Pair.of(typeName, Optional.of(
            Triple.of(typeName, entry, classDefItem)
          ));
        }
      }; // end anon Callable decl + initialization
      
      callables.add(callable);
    } // Entry<String,byte[]> loop
    
    // DO REAL WORK ***
    List<Future<Pair<String, 
      Optional<Triple<String, Entry<String,byte[]>, ClassDefItem>>>>> 
        tasks = null;
    
    Set<String> failedClassNames = new TreeSet<>();
    Set<String> successfulClassNames = new TreeSet<>();
    
    for (final Callable<?> c: callables) {
      String className = "UNKNOWN";
      try {
        
        Pair<String, Optional<Triple<String, Entry<String,byte[]>, ClassDefItem>>> result
        
          =
       (Pair<String,Optional<Triple<String,Map.Entry<String,byte[]>,ClassDefItem>>>)
            c.call();
        if (result.getValue().isPresent()) {
          System.err.printf("  - Skipping failed classDef: '%s'\n",
            result.getKey());
          continue;
        }
        Triple<String, Entry<String,byte[]>, ClassDefItem> tr
          = result.getValue().get();
        className = tr.getLeft();
        Entry<String,byte[]> ze = tr.getMiddle();
        ClassDefItem classDefItem = tr.getRight();
        
        System.err.printf(
          "  - Adding ClassDefItem for '%s':\n      tr = %s\n",
          className,
          tr
        );
        try { 
          df.add(classDefItem);
        } catch (Throwable ex) {
          failedClassNames.add(className);
          if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
          Throwable rc = ExceptionUtils.getRootCause(ex);
          if (rc == null) rc = ex;
          System.err.printf(
            "[WARN] Failed to add ClassDefItem '%s' to dex: %s: %s\n",
            className, rc.getClass().getSimpleName(), rc.getMessage()
          );
          continue; 
        }
      } catch (Throwable e) {
        failedClassNames.add(className);
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        Throwable rc = ExceptionUtils.getRootCause(e);
        if (rc == null) rc = e;
        System.err.printf(
          "[ERROR] Unexpected %s encountered while integrating results for "
          + "class '%s': %s: %s\n",
          e.getClass().getSimpleName(),          
          className, rc.getClass().getSimpleName(), 
            rc.getMessage() != null? rc.getMessage(): e.getMessage()
        );
        continue; 
      }
      successfulClassNames.add(className);
    } // for loop over tasks
    
    if (successfulClassNames.isEmpty()) {
      System.err.printf(
        "[FATAL] No classes were successfully processed.\n"
      );
      return Optional.empty();
    }
    
    byte[] dexBytes;
    try {
      dexBytes = df.toDex(null, false); 
      dexBytes[6] = (byte) '5';    
      return Optional.of(Triple.of(dexBytes, df, tasks));
    } catch (IOException ioe) {
      if ("true".equals(System.getProperty("printStackTrace"))) ioe.printStackTrace();
      return Optional.empty();
    }
  }
}