package org.d6r;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.net.*;
import javassist.ByteArrayClassPath;
import java.util.*;
import java.util.regex.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import org.d6r.annotation.*;
import org.apache.commons.io.*;
import org.apache.commons.io.input.*;
import org.apache.commons.io.output.*;
import org.apache.commons.lang3.StringUtils;
import java.nio.charset.Charset;
import static java.lang.System.identityHashCode;
import static java.lang.String.format;

public class ZipByteArrayClassPath extends ByteArrayClassPath {
  
  static void init() {
    String value = System.getProperty("java.protocol.handler.pkgs");
    List<String> entries;
    if (value == null) {
      entries = new ArrayList<String>();
    } else {
      entries = new ArrayList<String>(
        Arrays.asList(value.split("\\|"))
      );
    }
    if (! entries.contains("org.d6r.handler")) {
      entries.add("org.d6r.handler");
      StringBuilder sb = new StringBuilder();
      for (String entry: entries) {
        if (sb.length() != 0) sb.append('|');
        sb.append(entry);
      }
      System.setProperty("java.protocol.handler.pkgs", sb.toString());
    }
  }
  
  static {
    init();
  }
  
  // protected byte[] classfile;
  // protected java.lang.String classname;
  
  public static boolean DEBUG;
  protected static Field _urlField;
  @NonDumpable
  protected final Map<String, Object> zipMap;  
  
  public ZipByteArrayClassPath(String name, byte[] zipBytes) {
    super(name, zipBytes);
    this.zipMap = (Map<String, Object>) (Map<?, ?>) 
      ZipUtil.mapZip(zipBytes);
  }
  
  public ZipByteArrayClassPath(byte[] zipBytes) {
    this(
      String.format(
        "anonymous_byte_array_%08x_%d",
        System.identityHashCode(zipBytes), zipBytes.length
      ),
      zipBytes      
    );
  }
  
  public String getName() {
    return classname;
  }
  
  public byte[] getBytes() {
    return classfile;
  }
  
  @Override
  public void close() {
    System.err.println("close");
  }
  
  public List<? extends ZipEntry> list() {
    return ZipUtil.list(classfile);
  }
  
  public Enumeration<? extends ZipEntry> entries() {
    return Collections.enumeration(list());
  }
  
  @Override
  public URL find(final String classname) {
    
    final String name = ClassInfo.typeToName(classname);
    final String classNameAsPath
      = ClassInfo.classNameToPath(name, "class");
    
    if (DEBUG) System.err.printf("%s.find(classname: \"%s\")\n", 
      getClass().getName(), classname);
    
    final Object _bytes = zipMap.get(classNameAsPath);
    if (_bytes == null) return null;
    
    final byte[] bytes = (_bytes instanceof byte[])
      ? (byte[]) _bytes
      : ((String) _bytes).getBytes(StandardCharsets.UTF_8);
    
    final URL url =
      MemoryURLStreamHandler.urlForByteArray(bytes, classNameAsPath);
    
    if (DEBUG) System.err.printf(
      "%s.find(classname: \"%s\")\n  returning: %s\n",
      getClass().getName(), classname, url);    
    
    return url;    
  }
  
  @Override
  public InputStream openClassfile(String classname) {
    System.err.printf("%s.openClassfile(classname: \"%s\")\n", 
      getClass().getName(), classname);
    
    byte[] bytes = ZipUtil.toByteArray(
      classfile, 
      ClassInfo.classNameToPath(
        ClassInfo.typeToName(classname), "class"
      )
    );
    
    InputStream is = null;
    if (bytes != null) {
      is = new ByteArrayInputStream(bytes);
    }
    
    if (DEBUG) System.err.printf("%s.openClassfile(classname: \"%s\")"
      + "\n  returning: %s\n", getClass().getName(), classname, is);
    return is;
  }
  
  @Override
  public String toString() {
    return String.format(
      "%s(name: \"%s\", length: %d bytes, entries: %d)",
      getClass().getSimpleName(),
      classfile.length,
      zipMap.size()
    );
  }
  
  
  public static class MemoryURLConnection
              extends URLConnection
  {    
    protected byte[] data;
    protected FifoByteArrayOutputStream baos;
    protected Throwable fromGetOutputStream;
    public String normalizedPath;
    
    public MemoryURLConnection(final URL url, final byte[] data,
    final String normalizedPath)
    {
      this(url);
      this.data = data;
      this.normalizedPath = normalizedPath;
    }
    
    public MemoryURLConnection(final URL url) {
      super(url);
    }
    
    public byte[] setData(final byte[] data) {
      final byte[] oldData = data;
      if (baos != null) {
        ConcurrentModificationException cme;
        String message = "Data writer is in use";
        cme = new ConcurrentModificationException(message);
        if (fromGetOutputStream != null) 
          cme.setStackTrace(fromGetOutputStream.getStackTrace());
        throw cme;
      }
      this.data = data;
      return oldData;
    }
    
    @Override
    public void connect() {
      System.err.printf(
        "URL(%s).URLConnection(%s).connect() called",
        this.url, this
      );
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
      
      if (this.data == null) {
        if (this.baos != null) {
          this.baos.close();
        }
      }      
      if (data == null) throw new IllegalStateException(format(
        "URL(%s).openConnection()[%s].getInputStream(): "
        + "No data was written for this URL via getOutputStream()",
        this.url, this        
      ));
      return new ByteArrayInputStream(this.data);
    }
    
    @Override
    public OutputStream getOutputStream() {
      if (this.data != null) return new NullOutputStream();
      if (this.baos != null) return this.baos;
      
      fromGetOutputStream = new Error(String.format(
        "URL(%s).openConnection().getOutputStream() "
        + "called by thread: %s here",
        this.url, Thread.currentThread()
      ));
      
      this.baos = new FifoByteArrayOutputStream() {
        volatile boolean _closed;
        
        @Override
        public void close() {
          synchronized (this) {
            if (_closed) return;
            
            try {
              try {
                super.close();
              } catch (IOException ioe) {
                try {
                  ioe.printStackTrace();
                } catch (Throwable ex) {
                  System.err.println("IOException");
                }
              }
            } finally {
              if (MemoryURLConnection.this.data == null) {
                try (final InputStream baos_is = getInputStream()) {
                  try {
                    MemoryURLConnection.this.data 
                      = IOUtils.toByteArray(baos_is);
                  } catch (IOException copyEx) {
                    throw new RuntimeExceptionCompat(String.format(
                      ("copy:  baos: %s; baos_is: %s, url: %s; %s:\n"
                      + baos != null? baos.draw(): "(baos == null)"),
                      baos, baos_is, url, copyEx
                    ));
                  }
                } catch (IOException closeEx) {
                  try {
                    closeEx.printStackTrace();
                  } catch (Throwable ex) {
                    System.err.printf(
                      "<IOException + %s>:close()/printStackTrace()\n"
                      , ex.getClass().getSimpleName()
                    );
                  }
                } finally {
                  _closed = true;
                }
              }              
              this._closed = true;
              MemoryURLConnection.this.baos = null;
              MemoryURLConnection.this.fromGetOutputStream = null;
            }
          }
        } //@Override void close()
        
        @Override
        protected void finalize() throws Throwable {
          if (!_closed) close();
        } //@Override void finalize()
        
      }; // new FifoByteArrayOutputStream(){..}
      return baos;
    } // @Override OutputStream getOutputStream()
  
  } // static class MemoryURLConnection
  
  
  public static class MemoryURLStreamHandler
              extends URLStreamHandler
  {
    static Map<String, MemoryURLConnection> ucmap
      = Collections.synchronizedMap(new SoftHashMap<>());
    
    static MemoryURLStreamHandler DEFAULT;
    static final String SCHEME = "zipbytes";
    static final Matcher URL_PATH_MCHR = Pattern.compile(
      "^([a-zA-Z0-9_$]*:)*(/*?)(/|)([^/:!][^:!]*)(!.*|)$", Pattern.DOTALL
    ).matcher("");
    
    public MemoryURLStreamHandler() {
      super();
      if (DEFAULT == null) DEFAULT = this;
    }
    
    public static MemoryURLStreamHandler getDefault() {
      if (DEFAULT == null) DEFAULT = new MemoryURLStreamHandler();
      return DEFAULT;
    }
    
    
    
    @Override
    public URLConnection openConnection(URL url) {
      Matcher mchr = URL_PATH_MCHR.reset(url.getPath());
      String normalizedPath;
      if (mchr.find()) {
        normalizedPath = (mchr.group(3) != null? mchr.group(3): "").concat(
          (mchr.group(4) != null? mchr.group(4): "")
        );
      } else {
        normalizedPath = StringUtils.substringBefore(url.getPath(), "!");
      }
      
      MemoryURLConnection conn = ucmap.get(normalizedPath);
      if (conn == null) {
        conn = new MemoryURLConnection(url, (byte[])null, normalizedPath);
        ucmap.put(normalizedPath , conn);
      }
      return conn;
    }
    
    public static URL urlForByteArray(byte[] b, @Nullable String path)
    {
      try {
        final URL url = new URL(
          SCHEME, null, 0,
          (path != null)? path: Long.toString(identityHashCode(b),32),
          MemoryURLStreamHandler.getDefault()
        );
        URLConnection conn = url.openConnection();
        ((MemoryURLConnection) conn).setData(b);
        return url;
      } catch (MalformedURLException mue) {
        throw new RuntimeException(mue);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // static class MemoryURLStreamHandler
  
} // top-level class ZipByteArrayClassPath