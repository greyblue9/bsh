package org.d6r;
import com.ibm.icu.text.CharsetDetector;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import org.d6r.Reflector.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public class IOStream {
  
  public static final String TAG = IOStream.class.getSimpleName();
  static Charset CHARSET = StandardCharsets.UTF_8;
  
  static class CharInputStream extends InputStream {
    private final CharSequence cs;
    private final int len;
    private int pos = 0;
    
    public CharInputStream(CharSequence cs) {
      this.cs = cs;
      this.len = cs.length();
    }
    
    @Override
    public int read() {
      try {
        return (pos < len)
          ? (int) cs.charAt(pos)
          : -1;
      } finally {
        pos++;
      }
    }
    
    @Override
    public int available() {
      return len - pos;
    }
  }
  
  
  public static void closeQuietly(final AutoCloseable closeable) {
    try {
      if (closeable != null) closeable.close();
    } catch (final Throwable t) {
      if (Log.isLoggable(Log.SEV_WARN)) {
        Log.w(TAG, new IOException(String.format(
          "Exception while calling close() on %s: %s: %s",
          ClassInfo.getSimpleName(closeable), Debug.ToString(closeable), t
        ), t));
      }
    }
  }
  
  
  public static byte[] readFileAsByteArray(final String absolutePath) {
    if (absolutePath == null) {
      throw new IllegalArgumentException("absolutePath == null");
    }
    final File file = new File(absolutePath);
    if (!file.exists()) {
      throw Util.sneakyThrow(new FileNotFoundException(absolutePath));
    }
    
    try {
      return FileUtils.readFileToByteArray(file);
    } catch (final IOException ioe) {
      throw Util.sneakyThrow(new IOException(String.format(
        "Error reading bytes from file '%s' (%d bytes): %s",
        file.getAbsolutePath(), file.length(), ioe
      ), ioe));
    }
  }
  
  
  public static String readFileAsString(final String absolutePath) {
    if (absolutePath == null) {
      throw new IllegalArgumentException("absolutePath == null");
    }
    final File file = new File(absolutePath);
    if (!file.exists()) {
      throw Util.sneakyThrow(new FileNotFoundException(absolutePath));
    }
    
    FileChannel ch = null;
    try (final FileInputStream fis = new FileInputStream(file);
         final InputStream bis = new BufferedInputStream(fis);
         final Reader rdr =
           new CharsetDetector().setText(bis).detect().getReader())
    {
      ch = fis.getChannel();
      return IOUtils.toString(rdr);
    } catch (final IOException ioe) {
      long pos = -1;
      try {
        if (ch != null) pos = ch.position();
      } catch (final IOException ioe2) {
        ioe.addSuppressed(ioe);
      }
      throw Util.sneakyThrow(new IOException(String.format(
        "Error reading text from file '%s' (%d bytes) near offset %s: %s",
        file.getAbsolutePath(), file.length(),
        (pos != -1)
          ? Long.toString(pos, 10)
          : (ch != null)
              ? String.format("(error getting position from %s)", ch)
              : "(channel not available)",
        ioe
      ), ioe));
    } finally {
      if (ch != null) closeQuietly(ch);
    }
  }
  
  public static void close(final FileDescriptor fd) {
    if (!CollectionUtil.isJRE()) {
      PosixUtil.close(fd);
    } else {
      try {
        PosixUtil.close(fd);
      } catch (final Throwable t) {
        throw new NotImplementedException(String.format(
          "IOStream.close(fd: %s) <%s>",
          fd, t
        ), t);
      }
    }
  }
  
  public static void closeQuietly(final FileDescriptor fd) {
    try {
      close(fd);
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
  
  public static InputStream from(File file) {
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  public static InputStream from(CharSequence pathOrString) {
    //try {
      return
        (pathOrString instanceof String && isPath((String) pathOrString))
          ? from(new File((String) pathOrString))
          : new CharInputStream(pathOrString);
    //} catch (IOException ioe) {
    //Reflector.Util.sneakyThrow(ioe);
    //}
  }
  
  public static InputStream from(URL url) {
    try {
      URLConnection conn = url.openConnection();
      conn.setUseCaches(false);
      return conn.getInputStream();
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  public static OutputStream to(File file) {
    try {
      return new BufferedOutputStream(new FileOutputStream(file));
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }      
  }
  
  static boolean isPath(String str) {
    return str.length() < 512 && new File(str).exists();
  }
}


