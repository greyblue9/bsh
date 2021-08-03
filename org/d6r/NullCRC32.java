package org.d6r;

import java.io.*;

import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;


public class NullCRC32
     extends CRC32
{
  
  public static boolean VERBOSE 
    = (System.getProperty("NullCRC32.Test") != null);
  
  public static PrintStream Log = VERBOSE
    ? (PrintStream) System.err
    : new PrintStream(new ByteArrayOutputStream());
  
  public ZipInputStream zis;
  public byte[] zis_hdrBuf;
  
  public NullCRC32(ZipInputStream zis) {
    super(); // CRC32.<init>();
    this.zis = zis;
    this.zis_hdrBuf = getByteBufferReference();
    Reflect.setfldval(zis, "crc", this);
  }
  
  public long getRawCrc() {
    return (long) Reflect.<Long>getfldval(this, "crc");
  }
  
  public long getTbytes() {
    return (long) Reflect.<Long>getfldval(this, "tbytes");
  }
  
  
  
  public byte[] getByteBufferReference() {
    byte[] buf = Reflect.<byte[]>getfldval(zis, "hdrBuf");
    Log.printf(
      (buf != null
        ? "[INFO] hdrBuf got byte[] array (size = %d)\n"
        : "[WARN] hdrBuf got <NULL>"),
      buf != null? buf.length: -1     
    );
    return buf;
  }
  
  @Override
  public long getValue() {
    Log.println("getValue() called");
    if (zis_hdrBuf == null) {
      Log.printf("[INFO] Re-grabbing reference to hdrBuf array ...\n");
      zis_hdrBuf = getByteBufferReference();
      if (zis_hdrBuf == null) {
        Log.println("[WARN] Still null! returning getValue() -> 0x0");
        return 0L;
      }
    }
    
    byte[] fourBytesReversed = Arrays.copyOfRange(zis_hdrBuf, 10, 14);
    
    long expectedValue = (0xFFFFFFFFL & (long) (
      Integer.reverseBytes(
        ByteUtil.byteArrayToInt(fourBytesReversed)
      )
    ));    
    Log.printf(
      "[INFO] returning getValue() -> 0x%08xL \n"
      + " from 4bRev: %s  -->  expected: %s\n",
      expectedValue, 
      briefHexDump(fourBytesReversed),
      briefHexDump((int) (expectedValue & 0xFFFFFFFFL))
    );
    return expectedValue;
  }
  
  @Override
  public void reset() {
    Log.printf("[INFO] %s(%s) called\n", 
      dumpMembers.colorize("reset", "1;31"),
      StringUtils.join(Arrays.asList(), ", ")
    );
    Log.printf("  - state before super.reset(): \n    %s\n",      
      Debug.ToString(this).replace("\n", "\n    ")
    );
    super.reset();
    Log.printf("  - state after super.reset(): \n    %s\n",      
      Debug.ToString(this).replace("\n", "\n    ")
    );
  }
  
  @Override
  public void update(int val) {
    super.update(val);
  }
  @Override
  public void update(byte[] buf) {
    super.update(buf);
  }
  @Override
  public void update(byte[] buf, int offset, int byteCount) {
    super.update(buf, offset, byteCount);
  }
    
  
  static String briefHexDump(int rev) {
    byte[] bArray = new byte[4];    
    ByteUtil.writeInt(bArray, 0, rev);
    return briefHexDump(bArray);
  }
    
  static String briefHexDump(byte[] bytes) {
    return hexDumpR(bytes, 0, bytes.length).replaceAll(
      "[0-9A-F]+\\s+((?:(?:[0-9A-F] *){7})(?:[0-9A-F])) *"  +
      "([^ \n]*[^\n]*)",   "$1 | $2"
    );
  }
  
  static String hexDumpR(byte[] bytes, int start, int len) {
    return com.android.internal.util.HexDump
      .dumpHexString(bytes, start, len)
      .replaceAll(" ([0-9A-F]{2})\\b", "$1")
      .replaceAll("\\b(0x[0-9A-F]{6})([0-9A-F]{2})", "$1_$2 ")
      .replaceAll("([0-9A-F]{4})([0-9A-F]{4})", "$1 $2 ")
      .replaceAll("0x([0-9A-F]{6})_([0-9A-F]{2})", "$1$2 ");
  }
}








