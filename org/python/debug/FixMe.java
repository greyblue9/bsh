package org.python.debug;

import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.direct.AttributeFactory;
import dalvik.system.DexFile;
import com.android.dx.dex.DexOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class FixMe {
  //public static String apkname = null;
  //public static String apppath;
  public static boolean isinitialized = false;
  public static String ps1 = ">>>";
  public static String ps2 = "+++";
  
  static Constructor<com.android.dx.dex.file.DexFile> 
    dxDexFileCtor;
  static Class<?>[] dxDexFileCtorParamTypes;
  static Method translate;
  static boolean translateNeedsDexOptions;
  static CfOptions CF_OPTIONS;
  static AttributeFactory ATTR_FACTORY;
  static int TARGET_API_LEVEL = 18;
  static File TEMP_CLASSES_DIR 
    = new File("/data/local/tmp_clazzes");
  static Matcher FILENAME_STRIPPER 
    = Pattern.compile(
        "[^A-Za-z0-9$_]+", 
        Pattern.DOTALL | Pattern.MULTILINE
      ).matcher("");
  static String FILENAME_REPLACEMENT_STRING = "_";
  static String TAG = "org.python.debug.FixMe";
  public static boolean DEBUG 
    = "true".equals(System.getProperty("fixme.debug"));
  
  public static boolean initialize() {
    return (isinitialized = true);
  }
  
  public static Class<?> getDeclaringClass(Class<?> c) 
    throws ClassNotFoundException 
  {
    return c.getDeclaringClass();
    /*try {
      return c.getDeclaringClass();
    } catch (Exception e) {
      String[] elements = c.getName().replace('.', '/').split("\\$");
      String name = elements[0];
      for (int i = 1; i < elements.length - 1; i++) {
        name = new StringBuilder(String.valueOf(name)).append("$").append(elements[i]).toString();
      }
      if (elements.length == 1) {
        return null;
      }
      return getClassByName(new StringBuilder(apkpath).append(apkname).toString(), name);
    }*/
  }

  public static Class<?> getClassByName(String classname) 
  {
    try {
      return Class.forName(
        classname,
        false,
        Thread.currentThread().getContextClassLoader()
      );
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } catch (NoClassDefFoundError e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static Class<?> getClassByName(String filename, 
  String classname) {
    try {
      return new DexFile(
        new File(filename)
      ).loadClass(
        classname, 
        Thread.currentThread().getContextClassLoader()
      );
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static String fixPath(String path) {
    if (File.separatorChar == '\\') {
      path = path.replace('\\', '/');
    }
    int index = path.lastIndexOf("/./");
    if (index != -1) {
      return path.substring(index + 3);
    }
    return path.startsWith("./") 
      ? path.substring(2) : path;
  }
  

  static {
    if (DEBUG) { 
      /*System.out.println(org.d6r.dumpMembers.dumpMembers(
        null, CfTranslator.class, true
      ));*/
    }
    try {
      dxDexFileCtor 
        = (Constructor<com.android.dx.dex.file.DexFile>)
            com.android.dx.dex.file.DexFile.class
              .getDeclaredConstructors()[0];
      dxDexFileCtor.setAccessible(true);
      dxDexFileCtorParamTypes
        = dxDexFileCtor.getParameterTypes();
    } catch (Throwable e) {
      e.printStackTrace();
      dxDexFileCtor = null;
      dxDexFileCtorParamTypes = null;
    }
    
    try {
      translate = CfTranslator.class.getDeclaredMethod(
        "translate", String.class, byte[].class, 
        CfOptions.class, DexOptions.class
      );
      translateNeedsDexOptions = true;
      translate.setAccessible(true);
    } catch (NoSuchMethodException nsme) {
      try {
        translate = CfTranslator.class.getDeclaredMethod(
          "translate", String.class, byte[].class, 
          CfOptions.class
        );
        translateNeedsDexOptions = false;
        translate.setAccessible(true);
      } catch (ReflectiveOperationException ex2) {
        try {
          translate 
            = CfTranslator.class.getDeclaredMethod(
            "translate", DirectClassFile.class, 
            byte[].class, CfOptions.class, 
            DexOptions.class,
            com.android.dx.dex.file.DexFile.class
          );
          translateNeedsDexOptions = true;
          translate.setAccessible(true);
          ATTR_FACTORY = new StdAttributeFactory();
        } catch (ReflectiveOperationException ex3) {
          ex3.printStackTrace();
          translate = null;
          translateNeedsDexOptions = false;
        }
      }
    } catch (ReflectiveOperationException ex1) {
      ex1.printStackTrace();
      translate = null;
      translateNeedsDexOptions = false;
    }
    
    if (! TEMP_CLASSES_DIR.exists()) {
      boolean ok = TEMP_CLASSES_DIR.mkdirs();        
      if (!ok) System.err.printf(
        "Unable to create directory: %s\n",
        TEMP_CLASSES_DIR
      );
    }
  }
  
  public static 
  com.android.dx.dex.file.DexFile createDxDexFile() {
    Object[] args 
      = new Object[dxDexFileCtorParamTypes.length];
    if (dxDexFileCtorParamTypes.length != 0 
    &&  dxDexFileCtorParamTypes[0] == DexOptions.class) {
      DexOptions dexOptions = new DexOptions();
      dexOptions.targetApiLevel = TARGET_API_LEVEL;
      args[0] = dexOptions;
    }
    com.android.dx.dex.file.DexFile dxDexFile;
    try {
      dxDexFile = (com.android.dx.dex.file.DexFile)
        dxDexFileCtor.newInstance(args);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
      dxDexFile = null;
    }
    return dxDexFile;
  }
  
  public static ClassDefItem translate(
  String filePath, byte[] bytes, CfOptions cfOptions, 
  com.android.dx.dex.file.DexFile dxDexFile) 
  {
    DexOptions dexOptions = (translateNeedsDexOptions)
      ? new DexOptions()
      : null;
    if (dexOptions != null) {
      dexOptions.targetApiLevel = TARGET_API_LEVEL;
    }
    
    Object[] args;
    if (translateNeedsDexOptions) {
      if (translate.getParameterTypes()[0] 
        == DirectClassFile.class) 
      {
        DirectClassFile dcf = new DirectClassFile(
          bytes, filePath, false /*strictParse*/
        );
        dcf.setAttributeFactory(ATTR_FACTORY);
        args = new Object[] { 
          dcf, bytes, cfOptions, dexOptions, dxDexFile
        };
      } else {
        args = new Object[] { 
          filePath, bytes, cfOptions, dexOptions
        };
      }
    } else {
      args = new Object[] { filePath, bytes, cfOptions };
    }
    
    ClassDefItem classDefItem;
    try {
      classDefItem 
        = (ClassDefItem) translate.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
      classDefItem = null;
    }
    return classDefItem;
  }
  
  public static 
  Class<?> getDexClass(String name, byte[] clazzBytes) 
    throws IOException 
  {
    String fileNameWithoutExtension
      = FILENAME_STRIPPER.reset(name)
          .replaceAll(FILENAME_REPLACEMENT_STRING);
    String clsFileName 
      = fileNameWithoutExtension.concat(".class");
    String dexFileName 
      = fileNameWithoutExtension.concat(".dex");
    String apkFileName 
      = fileNameWithoutExtension.concat(".apk");
    
    if (DEBUG) System.err.printf(
      "[DEBUG] getDexClass(): clsFileName = %s\n",
      clsFileName
    );
    File clsFile = new File(TEMP_CLASSES_DIR,clsFileName);
    if (DEBUG) System.err.printf(
      "[DEBUG] getDexClass(): clsFile = %s\n",
      clsFile
    );
    if (clsFile.exists()) clsFile.delete();
    clsFile.createNewFile();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(clsFile);
      fos.write(clazzBytes);
    } finally {
      if (fos != null) try {
        fos.close(); 
      } catch (IOException ignored) {}
    }
    
    com.android.dx.dex.file.DexFile 
      outputDex = createDxDexFile();
    if (DEBUG) System.err.printf(
      "[DEBUG] getDexClass(): outputDex = %s\n",
      outputDex
    );
    if (outputDex == null) {
      RuntimeException error = new RuntimeException(
        String.format(
          "[FATAL] %s: Attempt to create new instance of "
          + "class %s via createDxDexFile() failed: "
          + "returned %s",
          TAG, 
          dxDexFileCtor.getDeclaringClass().getName(),
          String.valueOf(outputDex)
        )
      );
      error.printStackTrace();
      throw error;
    }
    
    /**
      public static com.android.dx.dex.file.ClassDefItem
        translate(
          String filePath, byte[] bytes, 
          CfOptions cfOptions, DexOptions dexOptions);
    */
    String filePath 
      = fixPath(name.replace('.', '/') + ".class");
    if (DEBUG) System.err.printf(
      "[DEBUG] getDexClass(): filePath = %s\n",
      filePath
    );
    
    if (CF_OPTIONS == null) CF_OPTIONS = new CfOptions();
    
    
    ClassDefItem classDefItem = translate(
      filePath, clazzBytes, CF_OPTIONS, outputDex
    );
    if (DEBUG) System.err.printf(
      "[DEBUG] getDexClass(): classDefItem = %s\n",
      classDefItem
    );
    
    // Add dexed class def'n to output dex file
    outputDex.add(classDefItem);
    
    File apkFile = new File(TEMP_CLASSES_DIR,apkFileName);
    ZipOutputStream zos = null;
    fos = null;    
    try {
      zos = new ZipOutputStream(      
        fos = new FileOutputStream(apkFile)
      );
      zos.putNextEntry(new ZipEntry("classes.dex"));
      outputDex.writeTo(zos, null, false);
      zos.closeEntry();
    } finally {
      if (zos != null) {
        try { zos.close(); } catch (IOException ign) {}
      }
      if (fos != null) {
        try { fos.close(); } catch (IOException ign) {}
      }
    }
    /*getClassByName(
      apppath, "org/python/core/PyFunctionTable"
    );
    getClassByName(
      apppath, "org/python/core/PyRunnable"
    );
    getClassByName(
      apppath, "org/python/core/PyFunctionTable"
    );*/
    
    Class<?> newClass = getClassByName(
      apkFile.getPath(),  
      name.replace('/', '.')
    );    
    return newClass;
  }
}

