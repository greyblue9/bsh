package sun.misc;

final class NativeSignalHandler implements SignalHandler
{
  private final long handler;
  
  long getHandler() {
    return this.handler;
  }
  
  NativeSignalHandler(final long handler) {
    this.handler = handler;
  }
  
  @Override
  public void handle(final Signal signal) {
    handle0(signal.getNumber(), this.handler);
  }
  
  private static native void handle0(final int p0, final long p1);
}
