package org.d6r;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FilterOutputStream;


public class BitOutputStream
     extends FilterOutputStream 
{
  private int bitBuffer;
  private int bitCount;
  // private final OutputStream out;
  
  public BitOutputStream(OutputStream out) {
    super(out);
  }
  
  @Override
  public void flush() throws IOException {
    if (this.bitCount > 0) {
      writeBits(8 - this.bitCount, 0);
    }
  }
  
  @Override
  public void write(int oneByte) throws IOException {
    writeBits(8, oneByte & '\u00ff');
  }  
  
  public void writeBits(int count, int value) throws IOException {
    int bitCount = this.bitCount;
    int bitBuffer = this.bitBuffer | value << 32 - count >>> bitCount;

    int var5;
    for(var5 = bitCount + count; var5 >= 8; var5 -= 8) {
      this.out.write(bitBuffer >>> 24);
      bitBuffer <<= 8;
    }

    this.bitBuffer = bitBuffer;
    this.bitCount = var5;
  }

  public void writeBoolean(boolean value) throws IOException {
    this.bitCount++;
    
    byte byteVal = value? (byte)1: (byte)0;
    
    this.bitBuffer |= byteVal << (32 - this.bitCount);
    
    if (this.bitCount == 8) {
      this.out.write(bitBuffer >>> 24);
      this.bitBuffer = 0;
      this.bitCount = 0;
    }
  }

  public void writeInteger(int value) throws IOException {
    writeBits(16, '\uffff' & value >>> 16);
    writeBits(16, value & '\uffff');
  }

  public void writeUnary(int value) throws IOException {
    int value1 = value;

    while(true) {
      int value2 = value1 - 1;
      if(value1 <= 0) {
        writeBoolean(false);
        return;
      }

      writeBoolean(true);
      value1 = value2;
    }
  }
  
  @Override
  public boolean equals(Object that) {
    return this == that;
  }
  
  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }
  
  @Override
  public String toString() {
    return String.format(
        "%s@%08x {\n"
      + "   bitBuffer:  %s\n"
      + "               0x%08x\n"
      + "               %du\n"
      + "               %d,\n"
      + "    bitCount:  %d\n"
      + "}",
      getClass().getSimpleName(),
      System.identityHashCode(this),
      String.format("%32s", Integer.toBinaryString(bitBuffer))
        .replace(' ', '0'),
      bitBuffer,
      (((long) bitBuffer) & 0xFFFFFFFFL),
      bitBuffer,
      bitCount
    );
  }



}




