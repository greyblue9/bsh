package org.d6r;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import com.googlecode.dex2jar.reader.MultiDexFileReader;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.reader.IDexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import org.apache.commons.lang3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;


public class Dex2jarMultidex {
  
  public static final String TAG = Dex2jarMultidex.class.getSimpleName();;
  
  
  public static Object dex2jar_multidex() throws Exception {
    final String inpath = System.getProperty("input.file");
    final String outpath = System.getProperty("output.file");
    if (outpath == null) {
      return dex2jar_multidex(inpath);
    } else {
      return dex2jar_multidex(inpath, outpath);
    }
  }
  
  
  public static Object dex2jar_multidex(String inpath) throws Exception {
    final File outdir = new File(".").getAbsoluteFile().getCanonicalFile();
    Log.d(TAG, "outdir = '%s'", outdir);
    final File infile = new File(inpath).getAbsoluteFile();
    Log.d(TAG, "inpath = '%s'", inpath);
    
    final String infileName = FilenameUtils.removeExtension(infile.getName());
    final String outfileName = String.format("%s_dex2jar.jar", infileName);
    final String outfileExt = "jar";
    final File outfile = new File(outdir, outfileName);
    Log.d(TAG, "outfile = '%s'", outfile);
    if (outfile.exists()) {
      long fileTime = outfile.lastModified();
      final String backupFileName = String.format(
        "%s.%s.bak.%s",
        outfileName, 
        FastDateFormat.getInstance("yyyy-MM-dd_HHmmss").format(fileTime),
        outfileExt
      );
      final File backupfile = new File(outdir, backupFileName);
      Log.d(TAG, "Output file '%s' exists; renaming to %s", outfile, backupfile);
      FileUtils.moveFile(outfile, backupfile);
      if (outfile.exists()) {
        throw new IOException(String.format(
          "backup of '%s' to '%s' failed", outfile, backupfile
        ));
      }
    } 
    return dex2jar_multidex(infile.getPath(), outfile.getPath());
  }
  
  
  public static Object dex2jar_multidex(String inpath, String outpath) 
    throws Exception 
  {
    final File infile = new File(inpath);
    final File outfile = new File(outpath);
    

    final byte[] inbytes = FileUtils.readFileToByteArray(infile.getAbsoluteFile());
    // DexFileReader reader = MultiDexFileReader.open(inbytes);
    final IDexFileReader reader = MultiDexFileReader.open(inbytes);
    
    Log.i(TAG, "Opened reader for '%s': %s", infile, reader);
    
    final Dex2jar v3 = Dex2jar.from(reader).skipDebug(false);
    Log.d(TAG, "Created V3 instance for '%s': %s", infile.getName(), v3);
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      Log.i(TAG, "Translating '%s' ...", infile);
      v3.to(baos);
      final byte[] zipBytes = baos.toByteArray();
      Log.d(TAG, "Translation completed for '%s': result is %d bytes", 
        infile.getName(), zipBytes.length);
      baos.close();
      
      Log.d(TAG, "Preparing output archive for '%s' ...", outfile);
      final Map<String, byte[]> bzm = new TreeMap<>();
      if (DexRemix.isZip(inpath)) bzm.putAll(ZipUtil.mapZip(inbytes));
      bzm.putAll(ZipUtil.mapZip(zipBytes));
      
      Log.d(TAG, "Removing signature-bound entries ...");
      for (String key: CollectionUtil2.filter(
        bzm.keySet(),
        Pattern.compile(
        "^META-INF/(?:CERT[^/]*|MANIFEST.MF|[^/]*\\.SF|INDEX.LIST)$").matcher("")))
      {
        Log.d(TAG, "  - Removing '%s' ...", key);
        bzm.remove(key);
      }
      
      Log.i(
        TAG, "Writing output archive '%s' (%d entries) ...", outfile, bzm.size()
      );
      FileUtils.writeByteArrayToFile(outfile, ZipUtil.writeZip(bzm));
      System.err.println(outfile);
      Log.i(
        TAG,
        "Wrote output jar archive to: '%s' (%d bytes)", outfile, outfile.length()
      );
      return v3;
    }
  }
  
  
  public static void main(final String... args) throws Exception {
    switch (args.length) {
      case 0:
        dex2jar_multidex();
        break;
      case 1:
        dex2jar_multidex(args[0]);
        break;
      case 2: {
        if (new File(args[0]).exists()) {
          if (new File(args[1]).exists()) {
            throw new IllegalArgumentException("Usage: %s INPUT [OUTPUT_JAR]");
          } else {
            dex2jar_multidex(args[0], args[1]);
          }
        } else {
          if (new File(args[1]).exists()) {
            dex2jar_multidex(args[1], args[0]);
          } else {
            throw new IllegalArgumentException("Usage: %s INPUT [OUTPUT_JAR]");
          }
        }
        break;
      }
      default:
        throw new IllegalArgumentException("Usage: %s INPUT [OUTPUT_JAR]");
    }
  }
  
}


 