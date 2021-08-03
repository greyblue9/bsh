/*
 ** Authored by Timothy Gerard Endres
 ** <mailto:time@ice.com>  <http://www.ice.com>
 **
 ** This work has been placed into the public domain.
 ** You may use this work in any way and for any purpose you wish.
 **
 ** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
 ** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
 ** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
 ** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
 ** REDISTRIBUTION OF THIS SOFTWARE.
 **
 */
package org.d6r;
import java.io.*;
import java.nio.channels.FileChannel;

import java.util.zip.GZIPInputStream;
import org.d6r.LoggingProxyFactory;
import org.d6r.Reflect;
import android.os.Process;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import org.apache.commons.io.HexDump;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import org.apache.excalibur.bzip2.CBZip2InputStream;
//import org.tukaani.xz.LZMAInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
//import org.apache.commons.compress.archivers.sevenz.Coder;
//import org.apache.commons.compress.archivers.sevenz.Coders.BZIP2Decoder;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * The TarInputStream reads a UNIX tar archive as an InputStream.
 * methods are provided to position at each successive entry in
 * the archive, and the read each entry as a normal input stream
 * using read().
 *
 * @version $Revision: 1.1 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see TarBuffer
 * @see TarHeader
 * @see TarEntry
 */
public class TarInputStream extends FilterInputStream 
{

  protected boolean debug;

  protected boolean hasHitEOF;

  protected int entrySize;

  protected int entryOffset;

  protected byte[] oneBuf;

  protected byte[] readBuf;

  public static final long DEFAULT_BLKSIZE = 10240;
  public static final long DEFAULT_RCDSIZE = 512;
  public static final long NULL = Long.MIN_VALUE;
  
  public static int STREAMBUF_SIZE = 160 * 512;
  protected FileChannel ch;
  
  public static boolean USE_EXCALIBUR = false;
  public static Class<?> cls_Coder;
  public static Constructor<?> ctor_Coder;
  public static Class<?> cls_BZIP2Decoder;
  public static Constructor<?> ctor_BZIP2Decoder;
  public static Method m_BZIP2Decoder_decode;
  
  
  static {
    try {
      cls_Coder = Class.forName(
        "org.apache.commons.compress.archivers.sevenz.Coder",
        false, Thread.currentThread().getContextClassLoader()
      );
      ctor_Coder = cls_Coder.getDeclaredConstructor();
      ctor_Coder.setAccessible(true);
      
      cls_BZIP2Decoder = Class.forName(        "org.apache.commons.compress.archivers.sevenz.Coders$BZIP2Decoder",
        false, Thread.currentThread().getContextClassLoader()
      );
      ctor_BZIP2Decoder 
        = cls_BZIP2Decoder.getDeclaredConstructor();
      ctor_BZIP2Decoder.setAccessible(true);
      m_BZIP2Decoder_decode 
        = cls_BZIP2Decoder.getDeclaredMethod(
            "decode", String.class, InputStream.class,
            Long.TYPE, cls_Coder, byte[].class
          );
      m_BZIP2Decoder_decode.setAccessible(true);
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      USE_EXCALIBUR = true;
    }
  }
  
  public TarInputStream(InputStream inStream) {
    super(getAutoDetectInputStream(inStream));
    
    this.readBuf = new byte[512 * 4096];
    this.oneBuf = new byte[1];
    this.debug = true;
    this.hasHitEOF = false;

    /*LoggingProxyFactory.newProxy(
      this.new EntryAdapter(), EntryFactory.class
    );*/
    this.ch = getChannel(this);
  }
  
  public static FileChannel getChannel(InputStream stream) {
    Object o = stream;
    do {
      o = Reflect.getfldval(o, "in");
      if (o instanceof FileInputStream) {
        return ((FileInputStream) o).getChannel();
      }
    } while (o != null);
    return null;
  }
  
  public FileChannel getChannel() {
    if (this.ch == null) this.ch = getChannel(this);
    return this.ch;
  }  
  
  public FileDescriptor getFD() {
    FileChannel fc = getChannel();
    if (fc == null) return null;
    return (FileDescriptor) Reflect.getfldval(fc, "fd");
  }
  
  public static String getPath(InputStream is) { 
    Object in = is; 
    while (!(in instanceof FileInputStream) 
    &&     !(in instanceof PosixFileInputStream)
    &&       in != null) 
    {
      in = Reflect.getfldval(in, "in");     
    }
    FileDescriptor fd = null;
    try {
      if (in instanceof FileInputStream) {
        fd = ((FileInputStream)in).getFD();
      } else if (in instanceof PosixFileInputStream) {
        fd = ((PosixFileInputStream)in).getFD();
      } 
      if (fd == null) return null;
      int fdNo = PosixFileInputStream.getInt(fd);
      if (fdNo < 0) return null;
      Method m_readlink = File.class.getDeclaredMethod(
        "readlink", String.class); 
      m_readlink.setAccessible(true); 
      Object target = m_readlink.invoke(null,
        String.format("/proc/%d/fd/%d",org.d6r.PosixFileInputStream.getPid(), fdNo)
      );
      if (target instanceof String) return (String) target;
    } catch (Throwable e) { 
      System.err.println(e.toString());
    }
    return null;
  }
  
  
  public long position() {
    try {
      return this.getChannel().position();
    } catch (IOException e) { 
    } catch (NullPointerException e) {     
    }
    return 0;
  }
  
  public String toString() {
    try {
      FileChannel ch = getChannel();
      if (ch != null) {
        return String.format("%s@%x { position = %s }", 
          getClass().getName(), System.identityHashCode(this),
          ch.position(), available()
        );
      }
    } catch (Throwable e) { }
    return String.format("%s@%x { available = %d }", 
      getClass().getName(), System.identityHashCode(this),
      available()
    );
  }

    /**
     * Sets the debugging flag.
     *
     * @param debugF True to turn on debugging.
     */
  public void setDebug(boolean debugF) {
    this.debug = debugF;
  }



    /**
     * Closes this stream. Calls the TarBuffer's close() method.
     */
  public void close() {
    try {
      this.in.close();
    } catch (IOException ex) {
      logWarning(ex, String.format(
        "%s.close()", getClass().getSimpleName()
      ));
    }
  }
  
  
  public void logWarning(Throwable ex, String fmt, 
    Object... args) 
  {
    if (System.err == null) return;
    System.err.printf(
      "[WARN] %s threw %s: %s", 
      String.format(fmt, args).replace("%", "%%"),
      ex.getClass().getSimpleName(),
      ex.getMessage()
    );
  }



    /**
     * Get the available data that can be read from the current
     * entry in the archive. This does not indicate how much data
     * is left in the entire archive, only in the current entry.
     * This value is determined from the entry's size header field
     * and the amount of data already read from the current entry.
     *
     *
     * @return The number of available bytes for the current entry.
     */
  public int available() {
    try  {
      return super.available();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

    /**
     * Skip bytes in the input buffer. This skips bytes in the
     * current entry's data, not the entire archive, and will
     * stop at the end of the current entry's data if the number
     * to skip extends beyond that point.
     *
     * @param numToSkip The number of bytes to skip.
     */
  @Override
  public long skip(long amount) {
    long skcnt = 0;
    do {
      long cnt = -2;
      try {
        cnt = super.skip(amount - skcnt);
        if (cnt < 0) {
          throw new RuntimeException(
            "skip --> "+String.valueOf(cnt)
          );
        }
      } catch (IOException e) {
        throw new RuntimeException(
          "skipping error: "+ String.valueOf(e), e
        );
      }
      skcnt += cnt;
    } while (skcnt < amount);
    return skcnt;
    //int numToSkip) {
    // REVIEW
    // This is horribly inefficient, but it ensures that we
    // properly skip over bytes via the TarBuffer...
    //
    /*byte[] skipBuf = new byte[8 * 1024];
    for (int num = numToSkip; num > 0; ) {
      int numRead;
      try {
        numRead = this.read(
          skipBuf, 0, (num > skipBuf.length 
            ? skipBuf.length: num)
        );
      } catch (IOException ex) {
        logWarning(ex, String.format(
          "%s.skip(int numToSkip = %d)",
          getClass().getSimpleName(), numToSkip
        ));
        return;
      }
      if (numRead == -1) break;
      num -= numRead;
    }*/
  }

    /**
     * Since we do not support marking just yet, we return false.
     *
     * @return False.
     */
  public boolean markSupported() {
    return super.markSupported();
  }
  
  public static int getInt(byte[] byteArray, int index) {
    return (int) ((int)byteArray[index]) & 0xFF;
  }
  
  public static int byteval(byte[] byteArray, int index) { 
    return (int)byteArray[index] & (int)255;
  }
  
  public static long getLong(byte[] byteArray, int index) {
    return (long) (
      (((long)byteArray[index]) << 8L)
    + (((long)byteArray[index]) & 0x00FFL)
    );
  }
  
  public static int UNCOMPRESSED = 0;
  public static int GZIP = 1;
  public static int BZIP2 = 2;
  public static int LZMA = 3;
  public static int XZ = 4;
  
  
  public static int getCompression(InputStream is) {
    is.mark(3); 
    byte[] mgc = new byte[3]; 
    
    try {
      
      is.read(mgc, 0, 3); 
      is.reset(); 
    
      if (getInt(mgc, 0) == 0x1F 
       && getInt(mgc, 1) == 0x8B 
       && getInt(mgc, 2) == 0x08) return GZIP;
       
      if (getInt(mgc, 0) == 0x42 
       && getInt(mgc, 1) == 0x5A) {
         if (USE_EXCALIBUR) {
           // throw away 'B' 'Z'
           System.err.println(
             "Skipping over 'B' 'Z' signature..."
           );
           is.read(mgc, 0, 2);
         }
         return BZIP2;
      }
      
      if (getInt(mgc, 0) == 0x5D
       && getInt(mgc, 1) == 0x00) {
         return LZMA;
      }
      
      if (getInt(mgc, 0) == 0xFD
       && getInt(mgc, 1) == 0x37
       && getInt(mgc, 2) == 0x7A) {
         return XZ;
      }
      
      return UNCOMPRESSED;
      
    } catch (IOException e) {
      throw new RuntimeException(e);  
    }
  }
  
  static String getCompressionByReflection(InputStream is) {
    Object in = is;
    do {
      if (in instanceof GZIPInputStream) return "gzip";
      if (in instanceof CBZip2InputStream) return "bzip2";
      if (in instanceof BZip2CompressorInputStream) {
        return "bzip2";
      }
      if (in instanceof LZMACompressorInputStream) return "lzma";
      if (in instanceof XZCompressorInputStream) return "xz";
      in = Reflect.getfldval(in, "in");
    } while (in != null);
    return "uncompressed";
  }
  
  public String getCompression() {
    return getCompressionByReflection(this);
  }
  
  
  public static InputStream getAutoDetectInputStream(
  InputStream origIs) 
  {
    InputStream is = origIs.markSupported()
      ? origIs
      : new BufferedInputStream(origIs);
    int compression = getCompression(is);
    
    try {
      if (compression == GZIP) {
        return new BufferedInputStream(
          new GZIPInputStream(is, STREAMBUF_SIZE),
          STREAMBUF_SIZE
        );
      }
      if (compression == BZIP2) {
        if (! USE_EXCALIBUR) {
          // sevenzip
          try {
            Object coder = ctor_Coder.newInstance();
            Object bz2Decoder 
              = ctor_BZIP2Decoder.newInstance();
            BZip2CompressorInputStream bz2In 
              = (BZip2CompressorInputStream) 
                  m_BZIP2Decoder_decode.invoke(bz2Decoder,
                    "stream.bz2", // ???
                    is, // b-zippd data input stream
                    -1L, // read until hit end-of-stream
                    coder, // no options/properties (?)
                    new byte[STREAMBUF_SIZE] // buffer (?)
                  );
            return new BufferedInputStream(
              bz2In, STREAMBUF_SIZE
            );
          } catch (Throwable e) {
            if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
          }
        }
        // excalibur
        return new BufferedInputStream(
          new CBZip2InputStream(is), STREAMBUF_SIZE
        );
      }
      if (compression == LZMA) {
        return new BufferedInputStream(
          new LZMACompressorInputStream(is), STREAMBUF_SIZE
        );
      }
      if (compression == XZ) {
        return new BufferedInputStream(
          new XZCompressorInputStream(is), STREAMBUF_SIZE
        );
      }
      return is;
    } catch (IOException e) {
      throw new RuntimeException(e);  
    }
  }
  
  

    /**
     * Since we do not support marking just yet, we do nothing.
     *
     * @param markLimit The limit to mark.
     */
  public void mark(int markLimit) {
    super.mark(markLimit);
  }

    /**
     * Since we do not support marking just yet, we do nothing.
     */
  public void reset() {
    try {
      super.reset();
    } catch (Throwable e) { 
      throw new RuntimeException(e);
    }
  }


}