package org.d6r;

import org.d6r.PosixFileInputStream;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import d2jcd9.com.googlecode.d2j.dex.Dex2jar;
import d2jcd9.com.googlecode.d2j.dex.DexExceptionHandler;
import d2jcd9.com.googlecode.dex2jar.tools.BaksmaliBaseDexExceptionHandler;
import d2jcd9.com.googlecode.dex2jar.tools.Dex2jarCmd;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.d6r.PosixFileInputStream;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.Dex2Java.Out;
import org.d6r.annotation.*;


public class DexConverter {
  
  public static Pair<Dex2jar, byte[]> dex2jar(byte[] inputBytes) {
    
    boolean isZip = inputBytes[0] == (byte) 'P' 
                 && inputBytes[1] == (byte) 'K';
    
    byte[] dexBytes = (isZip)
      ? ZipUtil.toByteArray(inputBytes, "classes.dex")
      : inputBytes;
    
    File tempDir = PosixFileInputStream.createTemporaryDirectory(String.format(
      "tmp_dex2jar__%s",
      Long.toString(System.currentTimeMillis(), 32)
    ));
    File outputJarFile = PosixFileInputStream.resolve(
      new File(tempDir, Long.toString(
        ((dexBytes.length << 8) * System.currentTimeMillis()), 32
      ))
    );
    Path outputJarPath = Paths.get(outputJarFile.getPath());
    
    DexExceptionHandler exHandler
      = new BaksmaliBaseDexExceptionHandler();
      
    try {
      Dex2jar d2j = Dex2jar.from(dexBytes)
        .optimizeSynchronized()
        .topoLogicalSort()
        .skipDebug(false)
        .withExceptionHandler(exHandler);
      
      byte[] outputJarBytes = null;
      
      try {
        d2j.to(outputJarPath);
      } catch (Throwable e) { 
        e.printStackTrace();
      } finally {
        outputJarBytes = Files.exists(outputJarPath)
          ? Files.readAllBytes(outputJarPath)
          : null;
        if (Files.exists(outputJarPath)) {
          FileDeleteStrategy.FORCE.delete(outputJarFile);        
        }
      }
      return Pair.of(d2j, outputJarBytes);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
}

