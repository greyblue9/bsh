package org.d6r;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LittleEndianDataInputStream 
     extends InputStream 
  implements DataInput
{
  /**
  To get at high level readFully methods of DataInputStream
  */
  protected DataInputStream dis;// 
  /**
  To get at the low-level read methods of InputStream
  */
  protected InputStream in;
  /*
  Work array for buffering input
  */
  protected byte w[];
  
  
  public LittleEndianDataInputStream(InputStream in) {
    this.in = getBuffered(in, 8192);
    this.dis = new DataInputStream(this.in);
    this.w = new byte[8];
  }
  
  public static InputStream getBuffered(InputStream is, int cap) 
  {
    try {
      if (is.markSupported()) return is;    
      return new BufferedInputStream(is, cap);
    } catch (Throwable ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  @Override
  public int available() throws IOException {
    return dis.available();
  }

  @Override
  public short readShort() throws IOException {
    dis.readFully(w, 0, 2);
    return (short) ((w[1] & 0xff) << 8
         | (w[0] & 0xff));
  }

  /**
  Note, returns int even though it reads a short.
  */
  @Override
  public int readUnsignedShort() throws IOException {
    dis.readFully(w, 0, 2);
    return ((w[1] & 0xff) << 8
         | (w[0] & 0xff));
  }

  /**
  like DataInputStream.readChar except little endian.
  */
  @Override
  public char readChar() throws IOException {
    dis.readFully(w, 0, 2);
    return (char) ((w[1] & 0xff) << 8
         | (w[0] & 0xff));
  }

  /**
  like DataInputStream.readInt except little endian.
  */
  @Override
  public int readInt() throws IOException {
    dis.readFully(w, 0, 4);
    return (w[3]) << 24 
         | (w[2] & 0xff) << 16 
         | (w[1] & 0xff) << 8 
         | (w[0] & 0xff);
  }

  /**
  like DataInputStream.readLong except little endian.
  */
  @Override
  public long readLong() throws IOException {
    dis.readFully(w, 0, 8);
    return (long) (w[7]) << 56
         | (long) (w[6] & 0xff) << 48 
         | (long) (w[5] & 0xff) << 40 
         | (long) (w[4] & 0xff) << 32 
         | (long) (w[3] & 0xff) << 24 
         | (long) (w[2] & 0xff) << 16 
         | (long) (w[1] & 0xff) << 8 
         | (long) (w[0] & 0xff);
  }

  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public int read(byte b[], int off, int len) 
    throws IOException 
  {
    return in.read(b, off, len);
  }

  @Override
  public void readFully(byte b[]) throws IOException {
    dis.readFully(b, 0, b.length);
  }

  @Override
  public void readFully(byte b[], int off, int len) 
    throws IOException 
  {
    dis.readFully(b, off, len);
  }

  @Override
  public int skipBytes(int n) throws IOException {
    return dis.skipBytes(n);
  }

  @Override
  public boolean readBoolean() throws IOException {
    return dis.readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return dis.readByte();
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return dis.readUnsignedByte();
  }

  @Deprecated
  @Override
  public String readLine() throws IOException {
    return dis.readLine();
  }

  @Override
  public String readUTF() throws IOException {
    return dis.readUTF();
  }

  @Override
  public void close() throws IOException {
    dis.close();
  }
  
  @Override
  public boolean markSupported() {
    return in.markSupported();
  }
  
  @Override
  public void mark(int length) {
    try {
      in.mark(length);
    } catch (Throwable ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  @Override
  public void reset() {
    try {
      in.reset();
    } catch (Throwable ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  
}
