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



public class LibUtil {
  
  static final Set<String> loadedSos = new TreeSet<>();
  static final Set<String> loadedClassNames = new TreeSet<>();
  
  public static Map<String, Pair<ZipEntry, byte[]>> load(Class<?> cls) {
    final Map<String, Pair<ZipEntry, byte[]>> ok = new TreeMap<>();
    JarFile zf = null;
    try {
      zf = ((JarURLConnection) 
        NameSpace.getClassResource(cls).openConnection()).getJarFile();
      Reflect.setfldval(zf, "guard", null);
      Reflect.setfldval(Reflect.getfldval(zf, "raf"), "guard", null);
      List<String> entryNames = CollectionUtil2.filter(
        ((LinkedHashMap<String,ZipEntry>) 
          Reflect.getfldval(zf, "entries")).keySet(),
        Pattern.compile("^lib/arm((?!64)[^/])*?/.*?\\.so[0-9.]*$")
      );
      for (final String entryName : entryNames) {
        final String fileName = StringUtils.substringAfterLast(entryName, "/");
        final String soname
          = StringUtils.substringBeforeLast(fileName, ".so").substring(3);
        final ZipEntry ze = zf.getEntry(entryName);
        System.err.printf("Preparing library \"%s\" ...\n", soname);
        byte[] libBytes = ZipUtil.toByteArray(zf, entryName);
        byte[] data = new byte[80];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putLong(ze.getCrc());
        bb.putLong(ze.getTime());
        bb.putLong(ze.getSize());
        final byte[] utfBytes = ze.getName().getBytes(StandardCharsets.UTF_8);
        if (utfBytes != null) {
          bb.put(utfBytes, 0, Math.min(utfBytes.length, bb.remaining()));
        }
        if (bb.remaining() != 0) {
          final byte[] extra = ze.getExtra();
          if (extra != null)
            bb.put(extra, 0, Math.min(extra.length, bb.remaining()));
        }
        String ub64 = Base64.encode(data).replace('=', '-').replace('/', '_');
        String tempPath = System.getProperty("java.io.tmpdir");
        File solibFile = new File(
         new File(
          new File(
            new File(
              new File(new File(tempPath), "tmp_solib"),
              Pattern.compile("([^a-zA-Z0-9-])").matcher(soname).replaceAll("_")
            ), ub64
          ),
          "lib"
         ),
         String.format("lib%s.so", soname)
        );
        if (!solibFile.exists()) {
          FileUtils.writeByteArrayToFile(solibFile, libBytes);
          System.err.printf(
            "Wrote '%s' (%d bytes)\n", solibFile.getPath(), libBytes.length
          );
        } else {
          System.err.printf(
            "Reusing existing object '%s' (%d bytes)\n",
            solibFile.getPath(), libBytes.length
          );
        }
        
        
        final Object pathList
          = Reflect.getfldval(cls.getClassLoader(), "pathList");
        final File[]
          origFiles0 = Reflect.getfldval(pathList, "nativeLibraryDirectories"),
          origFiles  = (origFiles0 != null)? origFiles0: new File[0];
        final Runtime rt = Runtime.getRuntime();
        
        Reflect.setfldval(rt, "mLibPaths", ArrayUtils.addAll(
          Reflect.<String[]>getfldval(rt, "mLibPaths"),
          new String[] { solibFile.getParentFile().getPath() + "/" }
        ));
        
        Reflect.setfldval(
          pathList, "nativeLibraryDirectories",
          ArrayUtils.addAll(origFiles, new File[] { solibFile.getParentFile() })
        );
        System.err.printf("Loading library '%s' ...", soname);
        System.loadLibrary(soname);
        loadedSos.add(soname);
        loadedClassNames.add(cls.getName());
        ok.put(soname, Pair.of(ze, libBytes));
      }
    } catch (Throwable e) { 
      e.printStackTrace();
    } finally {
      /*
      if (zf != null) {
        try {
          zf.close();
        } catch (NullPointerException | IOException ioe) {
          try {
            PosixUtil.close(
              Reflect.<RandomAccessFile>getfldval(zf, "raf").getFD()
            );
          } catch (Throwable e) { e.printStackTrace(); }
          ioe.printStackTrace();
        }
      }
      */
    }
    return ok;
  }
  
}

