package org.d6r;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class ByteUtils {
  
  static ByteBuffer bb = ByteBuffer.allocate(32);
  
  
  public static byte[] longToBytes(long x) {
    bb.clear();
    //ByteArrayBuffer[position=0,limit=32,capacity=32]
    bb.putLong(x);
    //ByteArrayBuffer[position=4,limit=32,capacity=32]
    bb.flip();
    //ByteArrayBuffer[position=0,limit=4,capacity=32]
    byte[] bytes = new byte[bb.position()]; 
    bb.get(bytes);
    //ByteArrayBuffer[position=4,limit=4,capacity=32]
    //Arrays.toString(bytes);
    //[0, 0, 127, -1]    
    return bytes;
  }
  
  public static long bytesToLong (byte[] bytes) {
    long value = 0L;
    long pv = 0;
    long pvi = 0;
    for(int i=0; i<bytes.length; i++) {
      int byteidx = bytes.length - i - 1;
      byte b = bytes[byteidx];
      long lbv = 0x7FFFFFFFFFFFFFFFL & (byte)(b >> 0);
      int li = 1;
      for (int j=0; j<8; j++) {      
        long jpv = (long) Math.pow(2D, (double)pv + j);
        value += (lbv & li) != 0? jpv: 0L;
        li *= 2;
      }
      pv += 8;
    }
    return value;
  }
  
  public static long bytesToLongLE (byte[] bytes) {
    long value = 0L;
    long pv = 0;
    long pvi = 0;
    for(int i=0; i<bytes.length; i++) {
      int byteidx = i;
      byte b = bytes[byteidx];
      long lbv = 0x7FFFFFFFFFFFFFFFL & (byte)(b >> 0);
      int li = 1;
      for (int j=0; j<8; j++) {      
        long jpv = (long) Math.pow(2D, (double)pv + j);
        value += (lbv & li) != 0? jpv: 0L;
        li *= 2;
      }
      pv += 8;
    }
    return value;
  }
  
  public static byte[] flipOrder(byte[] b) {
    byte[] out = new byte[b.length];
    for (int i=0; i<b.length; i+=1) { 
      out[i] = b[(i / 4)*4 + (((i % 4) +2) % 4)];       
    }
    return out;
  }
  
  
  public static int bytesToInt(byte[] bytes) {
    long longval = bytesToLong(bytes);
    return (int) (0xFFFFFFFFL & longval);
  }
  
  public static byte[] intToBytes(int x) {
    bb.clear();
    bb.putInt(x);
    bb.flip();
    byte[] bytes = new byte[bb.position()]; 
    bb.get(bytes);
    return bytes;
    
  }

  public static byte[] shortToBytes(short x) {
    bb.clear();
    bb.putShort(x);
    bb.flip();
    byte[] bytes = new byte[bb.position()]; 
    bb.get(bytes);
    return bytes;
  }
  
  public static short bytesToShort(byte[] bytes) {
    long longval = bytesToLong(bytes);
    return (short) (0xFFL & longval);
  }
  
  public static byte[] floatToBytes(float x) {
    bb.clear();
    bb.putFloat(x);
    bb.flip();
    byte[] bytes = new byte[bb.position()]; 
    bb.get(bytes);
    return bytes;
  }
  public static float bytesToFloat(byte[] bytes) {
    bb.clear();
    bb.put(bytes);
    bb.flip(); //need flip 
    try {
      return bb.getFloat();
    } catch (Throwable e) {
      TarFile.warn(e, Float.TYPE, Arrays.toString(bytes));
    }
    return 0F;
  }
  
  public static byte[] doubleToBytes(double x) {
    bb.clear();
    bb.putDouble(x);
    bb.flip();
    byte[] bytes = new byte[bb.position()]; 
    bb.get(bytes);
    return bytes;
  }
  public static double bytesToDouble(byte[] bytes) {
    bb.clear();
    bb.put(bytes);
    bb.flip(); //need flip 
    try {
      return bb.getDouble();
    } catch (Throwable e) {
      TarFile.warn(e, Double.TYPE, Arrays.toString(bytes));
    }
    return (double) 0;
  }
  
  
}
