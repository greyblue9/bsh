package org.d6r;

import java.io.*;
import java.util.*;
import java.lang.*;
import org.apache.commons.io.IOUtils;
import java.util.regex.*;
import java.util.zip.*;


public class Unzip {
  
  public static class UnzipArgs {
    public static final Matcher MATCH_ALL
      = Pattern.compile("^.").matcher("");
    
    List<File> zipFiles;
    File out;
    List<Matcher> ptrns;
    public UnzipArgs() {
      zipFiles = new ArrayList<File>();
      out = new File(System.getProperty("user.dir"));
      if (!out.exists()) {
        try {
          out 
            = new File(new File(".").getCanonicalPath());
        } catch (IOException ioex) {
          if ("true".equals(System.getProperty("printStackTrace"))) ioex.printStackTrace();
        }
        out = new File("");
      }
    }
  }
  
  public static void main(String... args) {
    
    UnzipArgs opts = new UnzipArgs();
    
    Iterator<String> it = Arrays.asList(args).iterator();
    while (it.hasNext()) {
      String arg = it.next();      
      File argFile = new File(arg);
      boolean exists = argFile.exists();
      boolean isDir = exists
        ? argFile.isDirectory(): false;
      
      if (exists) {
        if (isDir) {
          opts.out = argFile;
          System.err.printf(
            "Output directory: %s\n", opts.out
          );
          continue;
        }
        /*if (ARCHIVE_MATCHER.reset(arg).find()) {
          opts.zipFiles.add(argFile);
          continue;
        }*/
        opts.zipFiles.add(argFile);
        continue;
      }
      opts.ptrns.add(Pattern.compile(        
         arg,
         Pattern.CASE_INSENSITIVE 
           | Pattern.DOTALL 
           | Pattern.MULTILINE
           | Pattern.UNIX_LINES
      ).matcher(""));
    }
    
    List<ZipEntry> entries = unzip(opts);
    System.err.printf(
      "[ %d ] Processed %d entries in %d zipfile(s)\n", 
      entries.size() > 0? 0: 1,
      entries.size(),
      opts.zipFiles.size()
    );
  }
  
  public static List<ZipEntry> unzip(UnzipArgs opts) {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    
    for (File zipFile: opts.zipFiles) {
      System.out.printf("==> %s\n", zipFile); 
      ZipInputStream zis = null;
      InputStream fis = null;
      try {
        fis = new PosixFileInputStream(zipFile);
        zis = new ZipInputStream(fis);
        entries = new ArrayList<ZipEntry>(); 
        ZipEntry entry = null; 
        
        while ((entry = zis.getNextEntry()) != null) {
          try {
            Date lastMod = new Date(entry.getTime());
            long size = entry.getSize();
            String name = entry.getName();
            while (name.charAt(0) == '/') {
              name = name.substring(1);
            }
            
            System.out.printf(            
              "%12s %25s %s\n", 
              size, lastMod.toString().substring(
                0, 19
              ), 
              name
            );
            entries.add(entry);
            
            int lsl = name.lastIndexOf('/'); 
            if (lsl == entry.getName().length()-1) {
              // directory entry
              continue; 
            }
            if (lsl > 0) {
              String dirName = name.substring(0, lsl);
              File maybeDir = new File(dirName);
              
              if (! maybeDir.isDirectory()
              &&    maybeDir.length() == 0) {
                // empty file
                maybeDir.delete();
              } 
              if (! maybeDir.exists()) {
                // Create dir before extracting file
                maybeDir.mkdirs();
              }
            }
            
            File outFile = new File(name);
            if (! outFile.exists()) {
              outFile.createNewFile();
            }
            OutputStream fos = null;
            try { 
              fos = new FileOutputStream(outFile);
              IOUtils.copy(zis, fos);
            } finally { 
              try {
                if (fos != null) {
                  fos.flush();
                  fos.close();
                }
              } catch (Throwable e) { }
            }
          } catch (Throwable e) {
            // Entry exception
            System.err.printf("[%s]: %s\n", entry, e);
            continue;
          }
        } // entry loop
        

      } catch (Throwable zfEx) {
        System.err.printf("[%s]: %s\n", zipFile, zfEx);
      } finally {
        try {
          if (zis != null) zis.close();
        } catch (IOException ex) { }
      } // zipfile try-finally
    } // end of zipfiles loop
    
    return entries;
  } // unzip(..)

}