package org.d6r;

import java.lang.reflect.Method;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInput;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
//import java.util.RandomAccess;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
// import dalvik.system.CloseGuard;

import org.entityfs.RandomAccess;
import org.entityfs.RandomAccessCloseObserver;
import org.entityfs.support.lang.ObjectFactory;
import org.entityfs.support.io.RandomAccessMode;
import org.entityfs.util.io.ByteArrayInputStreamFactory;

import org.apache.commons.io.input.CloseShieldInputStream;
import de.schlichtherle.io.rof.ReadOnlyFile;

/**
public class java.io.RandomAccessFile {
  void <init>(File file, String mode)
  void <init>(String fileName, String mode)
  void close()
  long getFilePointer()
  long length()
  int read()
  int read(byte[] buffer)
  int read(byte[] buffer, int byteOffset, int byteCount)
  void seek(long offset)
  void setLength(long newLength)
  int skipBytes(int count)
  void write(byte[] buffer)
  void 
    write(byte[] buffer, int byteOffset, int byteCount)
  void write(int oneByte)
}
*/        

/**
error @ line 123 of /storage/extSdCard/_projects/sdk/bsh/trunk/src/org/d6r/RandomAccessStream.java :
   File cannot be resolved to a type
        super(new File("/dev/null"), "r");
                  ^^^^

error @ line 213 of /storage/extSdCard/_projects/sdk/bsh/trunk/src/org/d6r/RandomAccessStream.java :
   This method must return a result of type long
        public long calculateLength() {
                    ^^^^^^^^^^^^^^^^^

error @ line 230 of /storage/extSdCard/_projects/sdk/bsh/trunk/src/org/d6r/RandomAccessStream.java :
   Unhandled exception type IOException
        inputStream.close();
        ^^^^^^^^^^^^^^^^^^^

error @ line 242 of /storage/extSdCard/_projects/sdk/bsh/trunk/src/org/d6r/RandomAccessStream.java :
   Iterator cannot be resolved to a type
        Iterator<RandomAccessCloseObserver> iterator
        ^^^^^^^^

error @ line 247 of /storage/extSdCard/_projects/sdk/bsh/trunk/src/org/d6r/RandomAccessStream.java :
   The method notifyClosed(org.entityfs.RandomAccess) in the type RandomAccessCloseObserver is not applicable for the arguments (java.util.RandomAccess)
        ob.notifyClosed((RandomAccess) this);
           ^^^^^^^^^^^^
*/         


class FileInputStreamFactory 
  implements ObjectFactory<InputStream>, Seekable
{
  private final FileInputStream fis;
  private final FileChannel fc;
  private final FileDescriptor fd;
  private CloseShieldInputStream csis;
    
  public FileInputStreamFactory(final FileInputStream fis) {
    this.fis = fis;
    this.fc = fis.getChannel();
    try {
      this.fd = fis.getFD();
    } catch (IOException ioe) { 
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  @Override
  public InputStream create() {
    try {
      fc.position(0L);
      return (csis = new CloseShieldInputStream(fis));
    } catch (IOException ioe) { 
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  @Override
  public void seek(long newPos) {
    try {
      fc.position(newPos);
    } catch (Throwable ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      throw new RuntimeException(String.format(
        "Attempted to seek to position %d (0x%x), but channel threw %s",
        newPos, newPos, Reflector.getRootCause(ex)
      ), ex);
    }
  }
  
  public FileDescriptor getFD() {
    return fd;
  }
  
  public FileChannel getChannel() {
    return fc;
  }
}


class SeekableInputStreamFactory 
  implements ObjectFactory<InputStream>, Seekable
{
  private final InputStream is;
  private CloseShieldInputStream csis;
  
  Method seek;
  Class<?>[] pTypes;
  
  static final String[] names = {
    "position", "setPosition", "seek", "seekTo",
    "setFilePointer"
  };
  
  public SeekableInputStreamFactory(final InputStream is) {
    this.is = is;
    Class<?> cls = is.getClass();
    for (String name: names) {
      try {
        seek = cls.getMethod(name, Long.TYPE);
      } catch (NoSuchMethodException ex) { 
        try {
          seek = cls.getMethod(name, Integer.TYPE);
        } catch (NoSuchMethodException ex2) {
          continue;
        }
      }
      break;
    }
    if (seek == null) {
      throw new IllegalArgumentException(
        "InputStream must support at least one of the following methods: "
        .concat(Arrays.toString(names))
        .concat(" with a single parameter of type int or long")
      );
    }
    pTypes = seek.getParameterTypes();
  }
  
  @Override
  public void seek(long newPos) {
    Object arg = (pTypes[0] == Long.TYPE)
      ? (Object) Long.valueOf(newPos)
      : (Object) Integer.valueOf((int) newPos);
    try {
      seek.invoke(is, arg);
    } catch (ReflectiveOperationException ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      throw new RuntimeException(String.format(
        "Attempted to seek to position %d (0x%x), but stream threw %s",
        newPos, newPos, Reflector.getRootCause(ex)
      ), ex);
    }
  }
  
  @Override
  public InputStream create() {
    try {
      seek(0L);
      return (csis = new CloseShieldInputStream(is));
    } catch (Throwable ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
}


interface Seekable {
  void seek(long absoluteByteOffset);
}


public class RandomAccessStream 
     extends RandomAccessFile
  implements RandomAccess, DataInput, Seekable, ReadOnlyFile
{
  private final List<RandomAccessCloseObserver> m_closeObservers;
  private boolean m_closed;
  private long m_curPos;
  private long m_fileLength;
  private InputStream m_in;
  private final ObjectFactory<? extends InputStream> m_streamFactory;
  private final Seekable m_seekable;
  
  public boolean DEBUG; 
  static Field CHANNEL;
  static Field FD;
  static Field GUARD;
  static Field SCRATCH;
  static Field MODE;
  static Field SYNC_METADATA;
  static {
    try {
      (CHANNEL = RandomAccessFile.class.getDeclaredField(
        "channel")).setAccessible(true); 
      (FD = RandomAccessFile.class.getDeclaredField(
        "fd")).setAccessible(true);
      (GUARD = RandomAccessFile.class.getDeclaredField(
        "guard")).setAccessible(true);
      (SCRATCH = RandomAccessFile.class.getDeclaredField(
        "scratch")).setAccessible(true);
      (MODE = RandomAccessFile.class.getDeclaredField(
        "mode")).setAccessible(true);
      (SYNC_METADATA = RandomAccessFile.class
        .getDeclaredField("syncMetadata"))
        .setAccessible(true);
    } catch (ReflectiveOperationException ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
    }
  }
  
  <T> T set(Field field, T value) {
    try {
      field.set(this, value);
    } catch (ReflectiveOperationException ex) {
      if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
    }
    return value;
  }
  
  public FileChannel setChannel(FileChannel ch) {
    return set(CHANNEL, ch);
  }
  public FileDescriptor setFD(FileDescriptor fd) {
    return set(FD, fd);
  }
  public Closeable setCloseGuard(Closeable guard) {
    return set(GUARD, guard);
  }
  public byte[] setScratch(byte[] scratch) {
    return set(SCRATCH, scratch);
  }
  public int setMode(int mode) {
    return set(MODE, mode);
  }
  public boolean setSyncMetadata(boolean syncMetadata) {
    return set(SYNC_METADATA, syncMetadata);
  }
  /*
  private java.nio.channels.FileChannel channel = <null>;
  private java.io.FileDescriptor fd = <null>;
  private final dalvik.system.CloseGuard guard = <null>;
  private final B[] scratch = <null>;
  private int mode = <null>;
  private boolean syncMetadata = <null>;
  */
  
  
  
  public RandomAccessStream(ObjectFactory<? extends InputStream> fact) 
    throws FileNotFoundException
  {
    this(fact, -1L);
  }
  
  public RandomAccessStream(final byte[] byteArray) 
    throws FileNotFoundException
  {
    this(new ByteArrayInputStreamFactory(byteArray), -1L);
  }
  
  
  
  public RandomAccessStream(ObjectFactory<? extends InputStream> fact,
  long fileLength) 
    throws FileNotFoundException
  {
    super(new File("/dev/null"), "r");
    m_curPos = 0L;
    m_closed = false;
    m_closeObservers = new ArrayList<RandomAccessCloseObserver>(1);
    m_streamFactory = fact;
    m_seekable = (m_streamFactory instanceof Seekable)
      ? (Seekable) m_streamFactory
      : (Seekable) this;
    m_fileLength = fileLength;
    m_in = m_streamFactory.create();
  }
  
  public RandomAccessStream(InputStream is)
    throws FileNotFoundException
  {
    this(getFactory(is));
    if (m_streamFactory instanceof FileInputStreamFactory) {
      setFD(((FileInputStreamFactory) m_streamFactory).getFD());
      setChannel(((FileInputStreamFactory) m_streamFactory).getChannel());
    }
    setCloseGuard(null);
    setSyncMetadata(true);
    setMode(PosixUtil.O_RDONLY);
  }
  
  static <S extends InputStream> ObjectFactory<S> getFactory(S stream) {
    try {
      return (ObjectFactory<S>) (
        (stream instanceof FileInputStream)
          ? (Object) new FileInputStreamFactory((FileInputStream) stream)
          : (Object) new SeekableInputStreamFactory(stream)
      );      
    } catch (Throwable e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  
  public long pos() {
    return this.m_curPos;
  }
  
  private void assertNotClosed() {
    if (this.m_closed || ! getChannel().isOpen()) {
      throw new IllegalStateException("Closed");
    }
  }
  
  private void ensureHasInputStream() {
    if (this.m_in != null) return;
     
    this.m_in = this.m_streamFactory.create();
    long needPos = m_curPos;
    long pos = pos();
    if (needPos > pos) this.skip(needPos - pos);
    else if (needPos < pos) throw new IllegalStateException(
      String.format("needPos = %d; crntPos = %d", needPos, pos)
    );
  }
  
  private void newStream() {
    this.m_in = this.m_streamFactory.create();
    this.m_curPos = 0;
  }
  
  private long skip(long count) {
    long last = pos();
    long maybeSkip = skip(count, true);
    long now = pos();
    return now - last;
  }
  
  private long skip(long count, boolean throwFail) {
    long amtSkipped = 0;
    while (count > 0L) {
      try {
        amtSkipped = this.m_in.skip(count);
        count -= amtSkipped;
        this.m_curPos += amtSkipped;
        continue;
      } catch (IOException ex) {
        if (throwFail) throw new RuntimeException(ex);
      }
      break;
    }
    return amtSkipped;
  }
  
  @Override
  public int skipBytes(int count) {
    //return (int) skipBytes((long) count);
    long skipped = skip(count, true);
    return (int) skipped;
  }
  
  @Override
  public long getFilePointer() {
    return this.m_curPos;
  }
  
  public RandomAccessMode getMode() {
    return RandomAccessMode.READ_ONLY;
  }
  
  @Override
  public long length() {
    this.assertNotClosed();
    if (this.m_fileLength < 0L) {
      this.m_fileLength = this.calculateLength();
    }
    return this.m_fileLength;
  }
  
  public void addCloseObserver(final 
  RandomAccessCloseObserver randomAccessCloseObserver) { 
    this.m_closeObservers.add(
      randomAccessCloseObserver
    );
  }
  
  public int available() {
    this.assertNotClosed();
    try {
      return (int) length() - (int) m_curPos;
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return 0;
    }
  }
  
  public long calculateLength() {
    long pos = pos();
    
    InputStream inputStream 
      = this.m_streamFactory.create();
    try {
      long len = 0;
      for (
        long available = (long) inputStream.available();
        available > 0; 
        available = (long) inputStream.available())
      {
        len += inputStream.skip(available);
      }
      // inputStream.close();
      this.m_in = null;
      ensureHasInputStream();
      return len;
    } catch (IOException e) {
      throw Reflector.Util.sneakyThrow(e);
    } catch (Throwable th) {
      throw Reflector.Util.sneakyThrow(th);
    }
    // throw new IllegalStateException("calculateLength failed");
  }
  
  @Override
  public void close() {
    System.err.printf("\u001b[1;41;37m  CLOSING!!!  \u001b[0m\n");
    new Error("Being closed").printStackTrace();
    System.err.printf("\u001b[1;41;37m  CLOSING!!!  \u001b[0m\n");
    
    if (!this.m_closed) {
      try {
        if (this.m_in != null) {
          this.m_in.close();
        }
        this.m_closed = true;
        for (RandomAccessCloseObserver ob: m_closeObservers) {
          if (ob == null) continue; 
          ob.notifyClosed((RandomAccess) this);
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      } finally {
        this.m_closed = true;
      }
    }
  }
  
  @Override
  protected void finalize() throws Throwable {
    this.close();
    super.finalize();
  }
  
  public void flush() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(this.toString());
  }
  
  @Override
  public int read() {
    assertNotClosed();
    ensureHasInputStream();
    try {
      int read = this.m_in.read();
      this.m_curPos++;
      return read;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public int read(byte[] array) {
    if (DEBUG) System.err.printf("read(byte[%d])\n", array.length);
    assertNotClosed();
    ensureHasInputStream();
    try {
      int read = this.m_in.read(array);
      if (read > 0) {
        this.m_curPos += (long) read;
      }
      return read;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public int read(byte[] array, int offset, int count) {
    if (DEBUG) System.err.printf(
      "read(byte[%d], %d, %d) -> ", array.length, offset, count
    );
    assertNotClosed();
    ensureHasInputStream();
    try {
      int read = this.m_in.read(array, offset, count);
      if (read > 0) {
        this.m_curPos += (long) read;
        /*if (getChannel() != null) {
          getChannel().position(m_curPos);
        }*/
      }
      
      if (DEBUG && count == 4) {
        ByteBuffer bb = ByteBuffer.wrap(array);
        int val = bb.getInt();
        if (DEBUG) System.err.printf("%08x\n", val);        
      }
      // System.err.printf("%d\n", read);    
      return read;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  @Override
  public void seek(long position) {
    if (DEBUG) System.err.printf("seek(%d)\n", position);
    assertNotClosed();
    ensureHasInputStream();
    if (position > pos()) {
      skip(position - this.m_curPos);
      this.m_curPos = position;
    } else if (position < this.m_curPos) {
      if (m_seekable != null && m_seekable != this) {
        m_seekable.seek(position);
        this.m_curPos = position;
        return;
      }
      this.m_in = m_streamFactory.create();
      if (position > 0) skip(position);
      this.m_curPos = position;
    }
  }
  
  @Override
  public void setLength(final long n) 
    throws UnsupportedOperationException 
  {
    throw new UnsupportedOperationException();
  }
  
  public long skipBytes(final long n) {
    if (DEBUG) System.err.printf("skipBytes(%d) -> ", n);
    long skip = 0L;
    if (n == 0) return 0;
    this.assertNotClosed();
    this.ensureHasInputStream();
    try {
      skip = this.m_in.skip(n);
      this.m_curPos += skip;
      if (DEBUG) System.err.printf("%d\n", skip);
      return skip;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  @Override
  public void write(int n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void write(byte[] array)
    throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void write(byte[] array, int offset, int count) 
    throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
}
