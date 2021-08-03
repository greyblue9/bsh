package org.d6r;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArrayBuilder {

  protected static byte[] TRUE = TextUtil.toByteArray("true");
  protected static byte[] FALSE = TextUtil.toByteArray("false");

  protected byte[] buf;
  protected int count;

  public ByteArrayBuilder() {
    this(256);
  }

  public ByteArrayBuilder(int capacity) {
    this.buf = new byte[capacity];
  }

  public byte[] buffer() {
    return buf;
  }

  public int length() {
    return count;
  }

  public void setLength(int newCount) {
    count = newCount;
  }

  public int capacity() {
    return buf.length;
  }

  public byte byteAt(int index) {
    return buf[index];
  }

  public void crop(int offset) {
    if (offset < count) {
      count -= offset;
      System.arraycopy(buf, offset, buf, 0, count);
    } else {
      count = 0;
    }
  }

  public byte[] trim() {
    if (buf.length > count) {
      buf = Arrays.copyOf(buf, count);
    }
    return buf;
  }

  public byte[] toByteArray() {
    byte[] result = new byte[count];
    System.arraycopy(buf, 0, result, 0, count);
    return result;
  }
  
  public byte[] getBytes() {
    return toByteArray();
  }

  public String toString() {
    return TextUtil.catv(
      TextUtil.toString(toByteArray())
    );
  }
  
  

  public ByteArrayBuilder append(byte b) {
    ensureCapacity(1);
    buf[count++] = b;
    return this;
  }

  public ByteArrayBuilder append(byte[] b) {
    return append(b, 0, b.length);
  }

  public ByteArrayBuilder insert(int index, byte[] data, 
  int srcOffset, int length) 
  {
    ensureCapacity(length);
    
    byte[] x = Arrays.copyOfRange(buf, index, count);   
    System.arraycopy(data, srcOffset, buf, index, length);
    System.arraycopy(x, 0, buf, index + length, x.length);
    
    count += length;
    return this;
  }
  
  public ByteArrayBuilder insert(int index, byte[] b) {
   return insert(index, b, 0, b.length);
  }
  
  public ByteArrayBuilder append(byte[] b, int offset, int length) {
    ensureCapacity(length);
    System.arraycopy(b, offset, buf, count, length);
    count += length;
    return this;
  }

  public ByteArrayBuilder append(ByteBuffer bb, int length) {
    ensureCapacity(length);
    bb.get(buf, count, length);
    count += length;
    return this;
  }

  public ByteArrayBuilder append(String s) {
    byte[] bytes = TextUtil.toByteArray(s);
    ensureCapacity(bytes.length);
    System.arraycopy(bytes, 0, buf, count, bytes.length);
    count += bytes.length;
    return this;
  }

  public ByteArrayBuilder append(boolean b) {
    append(b ? TRUE : FALSE);
    return this;
  }

  public ByteArrayBuilder append(char c) {
    ensureCapacity(1);
    buf[count++] = (byte) c;
    return this;
  }

  public ByteArrayBuilder append(int n) {
    ensureCapacity(11);
    appendNumber(n);
    return this;
  }

  public ByteArrayBuilder append(long n) {
    ensureCapacity(20);
    appendNumber(n);
    return this;
  }

  public ByteArrayBuilder appendCodePoint(int c) {
    ensureCapacity(3);
    if (c <= 0x7f) {
      buf[count++] = (byte) c;
    } else if (c <= 0x7ff) {
      buf[count] = (byte) (0xc0 | ((c >>> 6) & 0x1f));
      buf[count + 1] = (byte) (0x80 | (c & 0x3f));
      count += 2;
    } else {
      buf[count] = (byte) (0xe0 | ((c >>> 12) & 0x0f));
      buf[count + 1] = (byte) (0x80 | ((c >>> 6) & 0x3f));
      buf[count + 2] = (byte) (0x80 | (c & 0x3f));
      count += 3;
    }
    return this;
  }

  public ByteArrayBuilder appendHex(int n) {
    ensureCapacity(8);
    for (int i = count + 8; --i >= count; n >>>= 4) {
      int digit = n & 0x0f;
      buf[i] = (byte) (digit < 10 ? digit + '0' : digit + ('a' - 10));
    }
    count += 8;
    return this;
  }

  public ByteArrayBuilder appendHex(long n) {
    ensureCapacity(16);
    for (int i = count + 16; --i >= count; n >>>= 4) {
      int digit = (int) n & 0x0f;
      buf[i] = (byte) (digit < 10 ? digit + '0' : digit + ('a' - 10));
    }
    count += 16;
    return this;
  }

  private void ensureCapacity(int required) {
    if (count + required > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(count + required, buf.length << 1));
    }
  }

  private void appendNumber(long n) {
    if (n < 0) {
      buf[count++] = '-';
      n = -n;
    }
    int i = count;
    for (long limit = 10; n > limit && limit > 0; limit *= 10) {
      i++;
    }
    count = i + 1;
    do {
      buf[i--] = (byte) (n % 10 + '0');
      n /= 10;
    } while (n != 0);
  }
}