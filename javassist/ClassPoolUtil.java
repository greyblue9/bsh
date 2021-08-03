package javassist;

import java.io.File;
import java.io.FileInputStream;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;

import javassist.ClassPool;
import javassist.ClassPoolTail;
import javassist.ClassPathList;
import javassist.ClassPath;
import javassist.JarClassPath;

import org.d6r.*;


public class ClassPoolUtil {

  static ClassPool defaultPool = null;
  static Map<String, Object> items = new HashMap<>();
  static String BASE = "/external_sd/_projects/sdk/bsh/trunk";
 
  // updates Javassist's default Class pool
  public static ClassPool getDefault() {
    if (defaultPool == null) {
      synchronized (BASE) {
        if (defaultPool == null) { 
          defaultPool = getClassPool(
            BASE, 
            ClassPool.getDefault(),
            new File(BASE, "bsh-mod.jar")
          );
        }
      }
    }
    return defaultPool;
  }

  // returns new ClassPool (I think)
  public static ClassPool getClassPool() {
    return getClassPool(BASE);
  }
  
  public static ClassPool getClassPool(String projBase) {
    return getClassPool(projBase, new ClassPool());
  }

  public static ClassPool getClassPool(String projBase, 
  ClassPool useClassPool) 
  {
    return getClassPool(projBase, useClassPool, new File[0]);
  }
  
  public static ClassPool getClassPool(String projBase, 
  ClassPool useClassPool, File... extraFiles)
  {
    ClassPool cp = useClassPool; 
    ClassPoolTail cpt 
      = (ClassPoolTail) Reflect.getfldval(cp, "source");
    ClassPathList cpl 
      = (ClassPathList) Reflect.getfldval(cpt, "pathList");
    
    HashSet<File> files = new HashSet<File>();
    while (cpl != null) {
      ClassPath cpth 
        = (ClassPath) Reflect.getfldval(cpl, "path");
      if (cpth instanceof JarClassPath) {
        JarClassPath jcp = (JarClassPath) cpth;
        JarFile jf
          = (JarFile) Reflect.getfldval(jcp, "jarfile");
        String path 
          = (String) Reflect.getfldval(jf, "filename");
        File crntFile = new File(path);
        files.add(crntFile);
      } else {
        // single '.class' file?
      }
      cpl = (ClassPathList) Reflect.getfldval(cpl, "next");
    }
    // CollectionUtil.print(files.toArray());
    if (extraFiles != null) {
      Collections.addAll(files, extraFiles);
    }
    FileInputStream fis = null;
    try {
      
      String buildScript = IOUtils.toString(
        fis = new FileInputStream(
          new File(projBase, "build.sh")
        )
      );
      Pattern jarRegex = Pattern.compile(
        "-l[\t \"]*([A-Za-z_.0-9./-]+\\.jar)"
      );
      Matcher mr = jarRegex.matcher(buildScript);
      
      while (mr.find()) {
        String libJarPath = mr.group(1).toString();
        if (libJarPath.indexOf("/") == 0) {
          files.add(new File(mr.group(1)));
        } else {
          files.add(new File(
            projBase, 
            libJarPath.replaceAll("\\.\\./", "")
          ));
        }
      }
      File[] libFiles = files.toArray(new File[0]);
      //CollectionUtil.print(libFiles);
      for (File jar : libFiles) {
        if (jar == null) continue; 
        if (! jar.exists()) {
          System.err.printf(
            "[WARN]: ClassPoolUtil: "
            + "Skipping non-existent jar '%s' "
            + "from File[] array.\n",
            jar.getPath()
          );
        }
        try {
          cp.appendClassPath(jar.getPath());
        } catch (NotFoundException nfe) {
          System.err.printf(
            "[WARN]: ClassPoolUtil: "
            + "ClassPool.appendClassPath(String path) "
            + "threw %s for jar file '%s': %s\n",
            nfe.getClass().getSimpleName(),
            jar.getPath(),
            nfe.getMessage() != null
              ? nfe.getMessage(): "[no message]"
          );
          try {
            Throwable fakeThrowable = new Throwable();
            fakeThrowable.fillInStackTrace();
          
            System.err.printf(
              "  Stack at %s: %s\n", 
              Arrays.toString(fakeThrowable.getStackTrace())
            );
          } catch (Throwable ignored) {}
        } // catch (NotFoundException nfe) 
      } // for File jar: libFiles
      
      return cp;
      
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (Throwable ignored) {}
      }
    }
  }


}

