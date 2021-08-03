package org.d6r;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;

public class FifoByteArrayOutputStream extends OutputStream {
  
  public static int DEFAULT_BYTE_ARRAY_SIZE = 128;
  static final String EMPTY_STR = "";
  public int BYTE_ARRAY_SIZE;
  public boolean DEBUG = false;
  
  protected List<byte[]> arrays;
  protected int getArrIdx = -1;
  protected int getArrPos = -1;
  protected byte[] getArr;
  protected int available = 0;
  private static Object lock = new Object();
  
  protected int putArrIdx = -1; // measured from end of arrays
  protected int putArrPos = -1; // 0 == arrays.size() - 1
  protected byte[] putArr;
  protected List<FifoInputStream> allocatedInputStreams;
  
  public static int width = -1;
  public static int height = -1;
  public static Boolean initialized = null;  

  
  public FifoByteArrayOutputStream(int discreteBufferSizeBytes) {
    super();
    if (initialized == null) initConsoeEnv();
    this.BYTE_ARRAY_SIZE = discreteBufferSizeBytes;
    this.arrays = new ArrayList<byte[]>();
    this.allocatedInputStreams = new ArrayList<FifoInputStream>();
  }
  public FifoByteArrayOutputStream() {
    this(DEFAULT_BYTE_ARRAY_SIZE);
  }
  
  public void reset() {
    this.arrays = new ArrayList<byte[]>();
    this.getArrIdx = -1;
    this.getArrPos = -1;
    this.getArr = null;
    
    this.putArrIdx = -1;
    this.putArrPos = -1;
    this.putArr = null;
    this.available = 0;
    
    Iterator<FifoInputStream> it = allocatedInputStreams.iterator();
    while (it.hasNext()) { 
      FifoInputStream crnt = it.next();
      crnt.invalid = false;
      crnt.invalidCause = null;
      crnt.readError = null;
    }
  }
  
  public class FifoInputStream extends InputStream {
    
    Throwable readError;
    boolean   invalid = false;
    InvalidatedError invalidCause;
    
    @Override
    public int available() {
      return FifoByteArrayOutputStream.this.available();
    }
    
    @Override
    public int read() {
      if (invalid) throw invalidCause;
      try {    
        return (0xFF & ((int) get()));
      } catch (InvalidatedError ie) {
        return -1;
      }
    }
    
    @Override
    public int read(byte[] buffer) {
      return read(buffer, 0, buffer.length);
    }
    
    
     /**
    Reads up to lenbytes of data from the input stream into
    an array of bytes. An attempt is made to read as many as
    lenbytes, but a smaller number may be read.
    The number of bytes actually read is returned as an integer.
    This method blocks until input data is available, end of file is
    detected, or an exception is thrown.
    If bis null, a
    NullPointerExceptionis thrown.
    If offis negative, or lenis negative, or
    offset+lenis greater than the length of the array
    b, then an IndexOutOfBoundsExceptionis
    thrown.
    If lenis zero, then no bytes are read and
    0is returned; otherwise, there is an attempt to read at
    least one byte. If no byte is available because the stream is at end of
    file, the value -1is returned; otherwise, at least one
    byte is read and stored into b.
    The first byte read is stored into element b[offset], the
    next one into b[offset+1], and so on. The number of bytes read
    is, at most, equal to len. Let kbe the number of
    bytes actually read; these bytes will be stored in elements
    b[offset]through b[offset+k-1],
    leaving elements b[offset+k]through
    b[offset+len-1]unaffected.
    In every case, elements b[0]through
    b[offset]and elements b[offset+len]through
    b[b.length-1]are unaffected.
    If the first byte cannot be read for any reason other than end of
    file, then an IOExceptionis thrown. In particular, an
    IOExceptionis thrown if the input stream has been closed.
    The read(b,off,len)method
    for class InputStreamsimply calls the method
    read()repeatedly. If the first such call results in an
    IOException, that exception is returned from the call to
    the read(b,off,len)method. If
    any subsequent call to read()results in a
    IOException, the exception is caught and treated as if it
    were end of file; the bytes read up to that point are stored into
    band the number of bytes read before the exception
    occurred is returned. Subclasses are encouraged to provide a more
    efficient implementation of this method.
    @param the buffer into which the data is read.
    @param ff he start offset in array b
    at which the data is written.
    @param en he maximum number of bytes to read.
    @return he total number of bytes read into the buffer, or
    -1if there is no more data because the end of
    the stream has been reached.
    @exception OException f an I/O error occurs.
    @exception ullPointerException f bis null.
    @see ava.io.InputStream#read()
    */
    @Override
    public int read(byte[] buffer, int offset, int byteCount) {
      if (buffer == null) {
        throw new NullPointerException("buffer == null");
      } else if (
        (offset < 0) || (offset > buffer.length)
        || (byteCount < 0) || ((offset + byteCount) > buffer.length)
        || ((offset + byteCount) < 0)) 
      {
        throw new IndexOutOfBoundsException();
      } else if (byteCount == 0) return 0;
      int val = read();
      if (val == -1) return -1;
      buffer[offset] = (byte) val;
      int read = 1;
      try {
        for (; read < byteCount; read++) {
          val = read();
          if (val == -1) break;
          buffer[offset+ read] = (byte) val;
        }
      } catch (Exception ioex) {
        readError = ioex;
      }
      return read;
    }
  }
  
  public int available() {
    return available;
  }
  
  protected byte get() { 
    if (getArrIdx == -1 || getArr == null) {
      getArrIdx = 0; 
      getArr = arrays.get(arrays.size() - 1 - getArrIdx); 
      getArrPos = 0;
    }
    if (getArrIdx > -1 && putArrIdx > -1 
    &&  getArrPos > -1 && putArrPos  > -1
    &&  arrays.size() - 1 - getArrIdx == putArrIdx 
    &&  getArrPos == putArrPos) {
      throw new InvalidatedError("reached end of data"); 
    }
    if (getArrPos >= getArr.length) { 
      popArrayAndUpdateGetIndexes();
      if (arrays.size() - 1 - getArrIdx < 0
      ||  arrays.size() == 0)
      {
        getArrPos = -1;
        getArr = null;
        getArrIdx = -1;
        throw new InvalidatedError("reached end of data"); 
      }
    }
    if (DEBUG) log(
      "reading from array (%d, %d)\n",
      arrays.size() - 1 - getArrIdx, getArrPos
    );
    try {
      if (getArr == null) {
        getArrPos = -1;
        getArr = null;
        getArrIdx = -1;
        throw new InvalidatedError("reached end of data"); 
      }
      byte ret = getArr[getArrPos++];
      available--;
      return ret;
    } finally {
    }
  }
  
  byte[] popArrayAndUpdateGetIndexes() {
    if (DEBUG) {
      log("- removing (last) array at index %d\n", arrays.size() - 1);
      log("### BEFORE: ###\n");
      System.err.println(draw());
    }      
    byte[] toRemove = arrays.get(arrays.size() - 1);
    arrays.remove(arrays.size() - 1);
    // getArrIdx++;
    getArr = arrays.get(arrays.size() - 1 - getArrIdx);
    getArrPos = 0;
    if (DEBUG) {
      log("### AFTER: ###\n");
      System.err.println(draw());
    }
    return toRemove;
  }
  
  protected void put(byte b) { 
    if (putArrIdx == -1 || putArrPos == putArr.length) { 
      arrays.add(0, putArr = new byte[BYTE_ARRAY_SIZE]); 
      putArrIdx = 0; 
      putArrPos = 0;
    }
    if (DEBUG) log(
      "writing 0x%02x (%3d) '%c' to array[%d][%d]\n", 
      b, b, (char) b, putArrIdx, putArrPos
    );
    try {
      putArr[putArrPos] = b;
      putArrPos++;
      available++;
    } finally {      
    }
  }
  
  
  StringBuilder[] sbs;
  
  FifoByteArrayOutputStream reset5() {
    sbs = new StringBuilder[5];
    for (int i=0; i<sbs.length; i++) sbs[i] = new StringBuilder();
    return this;
  }
  
  FifoByteArrayOutputStream append5(String... strs) {
    int maxlen = 0;
    for (int i=0; i<strs.length; i++) {
      if (strs[i] == null) strs[i] = EMPTY_STR;
      maxlen = Math.max(maxlen, strs[i].length());
    }
    for (int i=0; i<strs.length; i++) {
      if (strs[i] == null) strs[i] = EMPTY_STR;
      sbs[i].append(strs[i]);
      int strlen = strs[i].length();
      int diff = maxlen - strlen;
      while (diff > 0) {
        sbs[i].append(' ');
        diff--;
      }
    }
    return this;
  }
  
  String toString5() {
    StringBuilder sb = new StringBuilder(256);
    for (int i=0; i<sbs.length; i++) {
      sb.append(sbs[i].substring(0, Math.min(sbs[i].length(), width))); 
      sb.append('\n');
    }
    String ret = sb.toString();
    Arrays.fill(sbs, null);
    return ret;
  }
  
  /**
  0 arrays@|  0        |  1       |
  1        | ::        | ::       |
  2       [| byte[32], | byte[32] |]
  3 put    | (0,  6)   |          |
  4 get    |           | (0, 78)  |
  */
  public String draw() {
    if (initialized == null) initConsoeEnv();
    reset5().append5(
      "arrays @", 
      "        ",
      "       [",
      " get @  ",
      " put @  "
    );
    for (int idx=0; idx<arrays.size(); idx++) {
      append5("|","|","|","|","|").append5(
        String.format(    " %d", idx),
                          " ::",
        String.format(    " byte[%d],", arrays.get(idx).length),
        (arrays.size() - 1 
        - getArrIdx == idx)
          ? String.format(" (%d, %d)", idx, getArrPos)
          :               "         ",
         (putArrIdx == idx)
          ? String.format(" (%d, %d)", idx, putArrPos)
          :               "         "
      );
    }
    return append5("|","|","|","|","|")
          .append5(null, null, "]", null, null)
          .toString5();
  }
  
  
  
  
  
  
  @Override
  public void write(int oneByte) {
    put((byte) oneByte);
  }
  
  @Override
  public void write(byte[] buffer) {
    int idx = -1, len = buffer.length;
    while (++idx < len) {
      put(buffer[idx]);
    }
  }
  
  @Override
  public void write(byte[] buffer, int byteOffset, int byteCount) {
    int idx = byteOffset - 1, max = byteOffset + byteCount;
    while (++idx < max) {
      put(buffer[idx]);
    }
  }
  
  public FifoInputStream getInputStream() {
    return new FifoInputStream();
  }
  
  
  
  public static class InvalidatedError extends RuntimeException {
    public InvalidatedError(String message) {
      super(message);
    }
    public InvalidatedError(String message, Throwable cause) {
      super(message, cause);
    }
    public InvalidatedError(Throwable cause) {
      super(cause);
    }
  }
  
  static void log(CharSequence format, Object... args) {
    String fmt = (format instanceof String)
      ? (String) format
      : (String) format.toString();
    
    try {
      System.err.printf(fmt, (Object[])args);
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      System.err.printf(
        "%s / %s\n", fmt, StringUtils.join(args, ", ")
      );
    } 
  }
  
   static void initConsoeEnv() {
    synchronized (lock) {
      if (initialized == Boolean.TRUE) return;
      initialized = Boolean.TRUE;
    }
    try {
      for (String tsv: StringCollectionUtil.matchAll(
        PosixFileInputStream.pexecSync(
          "mksh", "-c", "typeset -p COLUMNS; typeset -p LINES"
        ), "(?<= |^|\n)([a-zA-Z0-9_\\$\\[\\]]+)=([^\n]*)", "$1\t$2"
      )) 
      { 
        String name  = StringUtils.substringBefore(tsv, "\t");
        String value = StringUtils.substringAfter(tsv, "\t"); 
        if (name.equals("COLUMNS")) { 
          width = Integer.valueOf(value, 10);
        } else if (name.equals("LINES")) { 
          height = Integer.valueOf(value, 10);
        }
      }
    } catch (Throwable e) {
      width = 55;
      height = 35;
    }
  }
}