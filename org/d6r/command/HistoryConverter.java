package org.d6r.command;

import java.nio.charset.StandardCharsets;
import java.io.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang3.*;
import org.apache.commons.io.*;
import org.apache.commons.io.HexDump;

import org.d6r.*;
import org.d6r.FifoByteArrayOutputStream;
import org.d6r.FifoByteArrayOutputStream.FifoInputStream;


public class HistoryConverter {
  
  static final Matcher WC_OUTPUT_MCHR
    = Pattern.compile(
        "^([0-9]+)\\s+.*$", Pattern.DOTALL | Pattern.UNIX_LINES
      ).matcher("");
  
  
  
  static final byte[] FIRST_ENTRY_HEADER = { 
    (byte) 0xAB, (byte) 0xCD, (byte) 0xFF, 0, 0, 0, (byte) 0x01    
  };
  static final byte[] ENTRY_HEADER = { 
    (byte) 0x20, (byte) 0xFF    
  };
  //0, 0, 0, (byte) 0x01
  
  
  static final String NULL_CHAR_STRING = String.valueOf(
    Character.toChars(0x00)[0]);
  static final String FF_CHAR_STRING = String.valueOf(
    Character.toChars(0xFF)[0]);
    
    
  public static byte[] encodeUnsigned(long value) {
    int uintValue = (int) ((0xFFFFFFFFL & value));
    return Arrays.copyOfRange(
      ByteUtil.encodeUnsignedValue(
        Integer.reverseBytes(uintValue), 0
      ),
      1,
      5
    );
  }
  
  // static final byte[] ENTRY_SEPARATOR = { 0, (byte) 0xFF, 0, 0, 0 };
  static final byte[] ENTRY_TRAILER = { 0 };
  
  public static byte[] convert(Iterator<String> lit, int lineCount,
  long fileSize)
  {
    int lno = 0; 
    ByteArrayBuilder bab
      = new ByteArrayBuilder((int) (fileSize + ((lineCount+1) * 5))); 
    
    Charset cs = StandardCharsets.UTF_8;
    CharsetEncoder enc = cs.newEncoder();
    final String NULL_CHAR_STRING
      = String.valueOf(Character.toChars(0x00)[0]);
    
    while (lit.hasNext()) {
      String rawLine = lit.next();
      ++lno;
      String entry = rawLine
          .replace(FF_CHAR_STRING,   "")
          .replace(NULL_CHAR_STRING, "");
      
      if ((lno % 1000) == 1) {
        System.err.printf(
          "Processed %d lines of %d (%3.2f%%)...\n",
          lno-1,
          lineCount, 100.0 * (((float)(lno-1)) / ((float)lineCount))
        );
      }
      
      CharBuffer cb = CharBuffer.wrap(entry);
      ByteBuffer bb = null;
      try {
        bb = enc.encode(cb);
      } catch (CharacterCodingException cce) {
        new RuntimeException(String.format(
          "%s on entry (catv'd): \"%s\"", cce, TextUtil.catv(entry)
        ), cce).printStackTrace();
        bb = ByteBuffer.wrap(entry.getBytes(cs));
      }      
      if (bb == null) {
        String entryRepr = null;
        try (FifoByteArrayOutputStream os = new 
             FifoByteArrayOutputStream();
             FifoInputStream is = os.getInputStream();)
        {
          HexDump.dump(entry.getBytes(cs), 0L, os, 0);
          os.flush();
          entryRepr = IOUtils.toString(is, cs);
        } catch (IOException ioEx) {
          ioEx.printStackTrace();        
        }
        throw new RuntimeException(String.format(
          "ByteBuffer bb == null; "
          + "from ByteBuffer.wrap(entry.getBytes(%s)):\n%s",
          cs, entryRepr != null
            ? entryRepr
            : Arrays.toString(entry.getBytes(cs))
        ));
      }
      
      if (lno == 1) {
        bab.append(FIRST_ENTRY_HEADER);
      } else {
        bab.append(ENTRY_HEADER);
        bab.append(encodeUnsigned(lno));
      }
      bab.append(bb, bb.limit());
      bab.append(ENTRY_TRAILER);
    }
    
    return bab.getBytes();
  }
  
  public static byte[] processFile(File inputFile) 
    throws IOException
  {
    long inputFileSize = inputFile.length();
    int inputFileLineCount = getLineCount(inputFile);
    Iterator<String> lit = FileUtils.lineIterator(inputFile);
    return convert(lit, inputFileLineCount, inputFileSize);
  }
  
  public static int getLineCount(File file) {
    String path = PosixFileInputStream.resolve(file).getPath();
    if (! new File(path).exists()) {
      throw new IllegalArgumentException(String.format(
        "File doesn't exist: %s", path
      ));
    }
    
    String output = null, output2 = null;
    output = PosixFileInputStream.pexecSync("wc", "-l", path);
    if (WC_OUTPUT_MCHR.reset(output).matches()) {
      return Integer.parseInt(
        WC_OUTPUT_MCHR.reset(output).replaceAll("$1"), 10);
    }
    
    output2 = PosixFileInputStream.pexecSync(
      "busybox", "wc", "-l", path);
    if (WC_OUTPUT_MCHR.reset(output2).matches()) {
      return Integer.parseInt(
        WC_OUTPUT_MCHR.reset(output2).replaceAll("$1"), 10);
    }
    
    throw new RuntimeException(String.format(
      "Problems running both 'wc' and 'busybox wc': "
      + "'wc' output: \"%s\"; 'busybox wc' output: \"%s\"",
      output, output2
    ));
  }
  
  
  public static void main(String... args) throws Throwable {
    boolean writeHistoryFile;
    if (args.length > 0 && "-w".equals(args[0])) {
      args = ArrayUtils.remove(args, 0);
      writeHistoryFile = true;
    } else {
      writeHistoryFile = false;
    }
    
    File inputFile = new File(
      args.length > 0
        ? args[0]
        : "/sdcard/mksh_hist_ascii.txt"
    );
    
    System.err.printf(
      "Using input file: '%s'\n", inputFile
    );
    System.err.printf(
      "Write history file: %s\n", Boolean.valueOf(writeHistoryFile)
    );
    
    File outputFile = new File(
      new File(PosixFileInputStream.cwd()),
      String.format(
        "mksh_hist_processed__%10d_%s.sh",
        (long) (System.currentTimeMillis() / 1000.0),
        Long.toString(System.nanoTime(), 32)
      )
    );
    outputFile.createNewFile();
    
    System.err.printf(
      "Using output file: '%s'\n", outputFile
    );
    
    byte[] outputBytes = processFile(inputFile);
    System.err.printf(
      "\nConverted output size: %d bytes\n", outputBytes.length
    );
    
    FileUtils.writeByteArrayToFile(outputFile, outputBytes);
    System.err.printf(
      "[ 0 ] Wrote %d of %d bytes to '%s'\n'",
      outputFile.length(), outputBytes.length,
      outputFile.getPath()
    );
    if (writeHistoryFile) {
      File histFile = new File("/data/media/0/mksh_histfile.sh");
      if (histFile.exists()) {
        if (!histFile.delete()) {
          System.err.println(PosixFileInputStream.pexecSync(
            "busybox", "chattr", "-A", "-D", "-i", histFile.getPath()
          ));
          System.err.println(PosixFileInputStream.pexecSync(
            "busybox", "rm", "-vf", "--", histFile.getPath()
          ));
        }        
      }
      FileUtils.writeByteArrayToFile(histFile, outputBytes);
      System.err.printf(
        "[ 0 ] Wrote %d of %d bytes to '%s'\n'",
        histFile.length(), outputBytes.length,
        histFile.getPath()
      );
      for (File file: new File[]{ 
        histFile,
        new File(
          new File("/mnt/shell/emulated/0"), histFile.getName()
        )
      })
      {
        System.err.println(PosixFileInputStream.pexecSync(
          "busybox", "chattr", "+A", file.getPath()
        ));
        System.err.println(PosixFileInputStream.pexecSync(
          "busybox", "chown", 
          String.format(
            "0.%d", PosixUtil.getpwnam("sdcard_rw").pw_gid
          ),
          file.getPath()
        ));      
        System.err.println(PosixFileInputStream.pexecSync(
          "busybox", "chmod", "0777", file.getPath()
        ));
        System.err.println(PosixFileInputStream.pexecSync(
          "toolbox", "restorecon", "-rV",
           file.getPath()
        ));
        System.err.println(PosixFileInputStream.pexecSync(
          "busybox", "lsattr", file.getPath()
        ));
        System.err.println(PosixFileInputStream.pexecSync(
          "toolbox", "ls", "-lZ", file.getPath()
        ));
      }
      System.err.println(PosixFileInputStream.pexecSync(
        "busybox", "killall", "-9", "mksh", "mksh-static-printf", "sh"
      ));
    }
  } 
}




  