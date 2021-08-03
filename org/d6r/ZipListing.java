package org.d6r;


import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.*;
import java.util.regex.*;
import java.lang.reflect.*;

public class ZipListing {
  
  public static final Field BUFFEREDINPUTSTREAM_POS
    = LazyMember.<Field>of("pos", BufferedInputStream.class).get();
  
  static {
    try {
      BUFFEREDINPUTSTREAM_POS.setAccessible(true);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  public static <T> T[] shift(final T[] arr) {
    return shift(arr, 0);
  }
  
  public static <T> T[] shift(final T[] arr, final int start) {
    final T[] retArr = (T[]) Array.newInstance(
      arr.getClass().getComponentType(), arr.length - 1
    );
    System.arraycopy(arr, 0, retArr, 0, start);
    System.arraycopy(arr, start + 1, retArr, start, arr.length - start - 1);
    return retArr;
  }
  
  static RuntimeException FAIL(final String message) {
    System.err.println(message);
    return new RuntimeException(message);
  }
  
  public static void main(String... args)
    throws Throwable
  {
    boolean fileEnabled = false;
    Pattern ptrn = null;
    Matcher mchr = null;
    FileDescriptor inFD = null;
    File outFile = null;
    if (args.length == 1 && args[0].startsWith("-") &&
        Arrays.binarySearch(new int[]{ 0, 1 }, 0, 2, args[0].indexOf("-h")) >= 0)
    {
      System.err.println(
        "Usage:  ziplist [options ...] [--out OUT_JARFILE] [entry-pattern]\n" +
        "\n" +
        "Options: \n" +
        "       --file, --type         Show `file(1)' entry magic signature (exp)"+
        "      --out, --outfile       Write entries as read to a new zip archive\n"
      );
      return;
    }
    do {
      for (String arg = (args.length>0)? args[0]: null;
           args.length>0;
           arg = (args.length>0)? args[0]: null)
      {
        
        try {
          if (arg.equals("--file") || arg.equals("--type")) {
            fileEnabled = true;
            continue;
          }
          if (arg.equals("--outfile") || arg.equals("--out")) {
            if (args.length >= 2) {
              outFile = new File(args[1]);
              args = shift(args);
              continue;
            } else {
              System.err.println("--outfile requires am argiment.");
              throw new IllegalArgumentException("--outfile requires am argiment.");
            }
          }
          if (arg.indexOf('*')  == -1 &&
              arg.indexOf('\\') == -1 && 
              arg.indexOf('?')  == -1)
          {
            File file = null;
            try {
              file = new File(arg);
              if (file.exists()) {
                inFD = PosixUtil.open(
                  file.getAbsolutePath(), PosixUtil.O_RDONLY, 0
                );
                continue;
              } else {
                file = null;
              }
              if (false) throw new IOException();
            } catch (IOException e) {
              file = null;
            }
          }
          try {
            ptrn = Pattern.compile(arg);
            mchr = ptrn.matcher("");
            continue;
          } catch (final java.util.regex.PatternSyntaxException e) {
            System.err.println(e.getMessage());
          }
          
          
          throw FAIL("Error: Unrecognized argument: \"%s\"\n");          
        } finally {
          if (args.length > 0) args = shift(args);
        }
      }
    } while (false);
    if (inFD == null) inFD = FileDescriptor.in;
    
    
    
    
    final InputStream in = new FileInputStream(inFD);
    
    
    final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(
        Reflect.<FileDescriptor>getfldval(FileDescriptor.class, "out")
      ), StandardCharsets.ISO_8859_1
    ));
    
    
    if (outFile != null) {
      ZipOutputStream _zos = null;
      File outDir = outFile.getParentFile();
      if (!outDir.exists() && !outDir.mkdirs()) {
        throw (RuntimeException) Reflector.invokeOrDefault(outDir, "mkdirErrno");
      }
      boolean finished = false;
      outFile.createNewFile();
      boolean entryClosed = true;
      int position;
      try (final BufferedInputStream bis = new BufferedInputStream(in);
           final ZipInputStream zis = new ZipInputStream(bis);
           final FileOutputStream fos = new FileOutputStream(outFile);
           final BufferedOutputStream bos = new BufferedOutputStream(fos);
           final ZipOutputStream zos = new ZipOutputStream(fos))
      {
        _zos = zos;
        for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
          entryClosed = false;
          long total = 0;
          position = BUFFEREDINPUTSTREAM_POS.getInt(bis);
          if (mchr != null && ! mchr.reset(ze.getName()).find()) continue;
          
          bw.write(String.format(
            "@+%-8d %10d %s\n", position, ze.getSize(), ze.getName()));
          bw.flush();
          if (ze.isDirectory()) {
            zos.putNextEntry(ze);
            zos.closeEntry();
            zis.closeEntry();
            continue;
          }
          final byte[] buf = new byte[4];
          InputStream fbaos_is = null;
          try (final InputStream zin = new CloseShieldInputStream(zis);
               final FifoByteArrayOutputStream fbaos
               = new FifoByteArrayOutputStream())
          {
            long size = (ze.getSize() > 0)
                ? ze.getSize()
                : Long.MAX_VALUE;
            long read = 0;
            entryRead:
            while (total < size) {
              try {
                read = zin.read(buf, 0, 4);
              } catch (IOException e) {
                try {
                  read = zin.read(buf, 0, 2);
                } catch (IOException e2) {
                  try {
                    read = zin.read(buf, 0, 2);
                  } catch (IOException e3) { 
                    bw.write(String.format(
                      "Read %d of %d bytes: entry '%s'\n",
                      total, size, ze.getName()
                    ));
                    bw.flush();
                    break entryRead;
                  }
                }
              }
              total += read;
              fbaos.write(buf, 0, (int)(read < 0L? 0L: (0x7FFFFFFF & ((int)read))));
            }
            fbaos_is = fbaos.getInputStream();
          } catch (IOException ioe) {
            ioe.printStackTrace(); 
          } catch (Throwable e) { 
            e.printStackTrace();
          } finally {
            zis.closeEntry();
          }
          
          if (fbaos_is != null) {
            ze.setSize(total);
            long written = -1;
            try {
              zos.putNextEntry(ze);
              written = IOUtils.copyLarge(fbaos_is, zos);
            } finally {
              zos.closeEntry();
              entryClosed = true;
              bw.write(String.format(
                "Wrote %d of %d bytes: entry '%s'\n",
                written, total, ze.getName()
              ));
              bw.flush();
            }
          }
          
          if (!entryClosed) {
            try {
              zos.closeEntry();
              entryClosed = true;
            } catch (Throwable e) { System.err.println(e); }
          }
        }// ENTRY FOR
        zos.finish();
        finished = true;
      } finally {// MAIN TRY
        if (!finished && _zos != null) {
          try {
             _zos.finish();
          } catch (Throwable e) { System.err.println(e); }
        }
      }
      
      bw.flush();
      System.out.println();
      System.out.println(outFile.getAbsoluteFile().getCanonicalFile());      
      return;
    }
    
    final ProcessBuilder FILE;
    if (fileEnabled) {
      FILE = new ProcessBuilder(new String[]{ "file", "-npsNzF|", "-" });
      FILE.redirectErrorStream(true);
    } else {
      FILE = null;
    }
    try (final BufferedInputStream bis = new BufferedInputStream(in);
         final ZipInputStream zis = new ZipInputStream(bis))
    {
      CharSequence type = null;
      Process file = null;
      int position;
      for (ZipEntry ze = null; (ze = zis.getNextEntry()) != null;) {
        position = BUFFEREDINPUTSTREAM_POS.getInt(bis);
        if (mchr != null && ! mchr.reset(ze.getName()).find()) continue;
        
        bw.write(String.format("%12d %s\n", ze.getSize(), ze.getName()));
        bw.flush();
        
        if (ze.getSize() == 0) continue;
        type = null;
        if (fileEnabled) {
          if (file != null) file.destroy();
          file = FILE.start();
          try (final OutputStream file_stdin = file.getOutputStream();
               final BufferedOutputStream pos 
               = new BufferedOutputStream(file_stdin))
          {
            IOUtils.copyLarge(zis, pos);
            pos.flush();
          } catch (IOException ioe) {
            ioe.printStackTrace();
            continue; 
          }
          
          
          try (final InputStream file_stdout = file.getInputStream();
               final InputStreamReader isr
               = new InputStreamReader(file_stdout, StandardCharsets.ISO_8859_1);
               final BufferedReader br = new BufferedReader(isr))
          {
            try {
              while (br.ready()) {
                final String line = IOUtils.toString(file_stdout);
                final int sepPos = line.indexOf('|');
                if (sepPos != -1) {
                  type = line.subSequence(sepPos+1, line.length());
                  break;
                }
              }
              if (type != null) bw.write(String.format(
                "%12d %s [%s]\n", ze.getSize(), ze.getName(), type
              ));
              file.waitFor();
              while (br.ready()) {
                final String line = IOUtils.toString(file_stdout);
                final int sepPos = line.indexOf('|');
                if (sepPos != -1) {
                  type = line.subSequence(sepPos+1, line.length());
                }
              }
            } catch (InterruptedException ie) {
              System.err.println(ie);
              Thread.currentThread().interrupt();
              if (br.ready()) {
                final String line = IOUtils.toString(file_stdout);
                final int sepPos = line.indexOf('|');
                if (sepPos != -1) {
                  type = line.subSequence(sepPos+1, line.length());
                }
              }
            } finally {
              try {
                if (br.ready()) {
                  final String line = IOUtils.toString(file_stdout);
                  final int sepPos = line.indexOf('|');
                  if (sepPos != -1) {
                    type = line.subSequence(sepPos+1, line.length());
                  }
                }
                if (type != null) bw.write(String.format(
                  "%12d %s [%s]\n", ze.getSize(), ze.getName(), type
                ));
                bw.flush();
              } catch (IOException ioe) {
                ioe.printStackTrace();
              }
            }         
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        
        } else {
          bw.write(String.format(
            "0x%08x\t%12d\t%s\n", position, ze.getSize(), ze.getName()
          ));
        }
      }
    }
  }  
  
}