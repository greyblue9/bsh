package sun.misc;

import java.util.Hashtable;

public final class Signal
{
  private static Hashtable handlers;
  private static Hashtable signals;
  private int number;
  private String name;
  
  public int getNumber() {
    return this.number;
  }
  
  public String getName() {
    return this.name;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof Signal)) {
      return false;
    }
    final Signal signal = (Signal)o;
    return this.name.equals(signal.name) && this.number == signal.number;
  }
  
  @Override
  public int hashCode() {
    return this.number;
  }
  
  @Override
  public String toString() {
    return "SIG" + this.name;
  }
  
  public Signal(final String s) {
    this.number = findSignal(s);
    this.name = s;
    if (this.number < 0) {
      throw new IllegalArgumentException("Unknown signal: " + s);
    }
  }
  
  public static synchronized SignalHandler handle(final Signal key, final SignalHandler value) throws IllegalArgumentException {
    long n = 0L;
    if (value instanceof NativeSignalHandler) {
      ((NativeSignalHandler)value).getHandler();
    } else {
      n = 2L;
    }
    final long n2 = n;
    final long handle0 = handle0(key.number, n2);
    if (handle0 == -1L) {
      throw new IllegalArgumentException("Signal already used by VM or OS: " + key);
    }
    Signal.signals.put(new Integer(key.number), key);
    synchronized (Signal.handlers) {
      final SignalHandler signalHandler = (SignalHandler) 
        Signal.handlers.get(key);
      Signal.handlers.remove(key);
      if (n2 == 2L) {
        Signal.handlers.put(key, value);
      }
      if (handle0 == 0L) {
        return SignalHandler.SIG_DFL;
      }
      if (handle0 == 1L) {
        return SignalHandler.SIG_IGN;
      }
      if (handle0 == 2L) {
        return signalHandler;
      }
      return new NativeSignalHandler(handle0);
    }
  }
  
  public static void raise(final Signal signal) throws IllegalArgumentException {
    if (Signal.handlers.get(signal) == null) {
      throw new IllegalArgumentException("Unhandled signal: " + signal);
    }
    raise0(signal.number);
  }
  
  private static void dispatch(final int value) {
    final Signal signal = (Signal) Signal.signals.get(new Integer(value));
    final SignalHandler signalHandler = (SignalHandler) 
      Signal.handlers.get(signal);
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        signalHandler.handle(signal);
      }
    };
    if (signalHandler != null) {
      new Thread(runnable, signal + " handler").start();
    }
  }
  
  private static native int findSignal(final String p0);
  
  private static native long handle0(final int p0, final long p1);
  
  private static native void raise0(final int p0);
  
  static {
    Signal.handlers = new Hashtable(4);
    Signal.signals = new Hashtable(4);
  }
}