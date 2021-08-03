package java.util.concurrent;


import java.util.Random;

public class ThreadLocalRandom extends Random
{
  private static final long multiplier = 25214903917L;
  private static final long addend = 11L;
  private static final long mask = 281474976710655L;
  private long rnd;
  boolean initialized;
  private long pad0;
  private long pad1;
  private long pad2;
  private long pad3;
  private long pad4;
  private long pad5;
  private long pad6;
  private long pad7;
  private static final ThreadLocal<ThreadLocalRandom> localRandom;
  private static final long serialVersionUID = -5851777807851030925L;
  
  ThreadLocalRandom() {
    this.initialized = true;
  }
  
  public static ThreadLocalRandom current() {
    return ThreadLocalRandom.localRandom.get();
  }
  
  public void setSeed(final long seed) {
    if (this.initialized) {
      throw new UnsupportedOperationException();
    }
    this.rnd = ((seed ^ 0x5DEECE66DL) & 0xFFFFFFFFFFFFL);
  }
  
  protected int next(final int bits) {
    this.rnd = (this.rnd*25214903917L+11L & 0xFFFFFFFFFFFFL);
    return (int) (this.rnd >>> 48-bits);
  }
  
  public int nextInt(final int least, final int bound) {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return this.nextInt(bound-least)+least;
  }
  
  public long nextLong(long n) {
    if (n <= 0L) {
      throw new IllegalArgumentException("n must be positive");
    }
    long offset = 0L;
    while (n >= 2147483647L) {
      final int bits = this.next(2);
      final long half = n >>> 1;
      final long nextn = ((bits & 0x2) == 0x0) ? half : (n-half);
      if ((bits & 0x1) == 0x0) {
        offset += n-nextn;
      }
      n = nextn;
    }
    return offset+this.nextInt((int) n);
  }
  
  public long nextLong(final long least, final long bound) {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return this.nextLong(bound-least)+least;
  }
  
  public double nextDouble(final double n) {
    if (n <= 0.0) {
      throw new IllegalArgumentException("n must be positive");
    }
    return this.nextDouble()*n;
  }
  
  public double nextDouble(final double least, final double bound) {
    if (least >= bound) {
      throw new IllegalArgumentException();
    }
    return this.nextDouble()*(bound-least)+least;
  }
  
  static {
    localRandom = new ThreadLocal<ThreadLocalRandom>() {
      protected ThreadLocalRandom initialValue() {
        return new ThreadLocalRandom();
      }
    };
  }
}
