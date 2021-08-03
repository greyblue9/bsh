package sun.misc;

public interface SignalHandler
{
  public static final SignalHandler SIG_DFL = new NativeSignalHandler(0L);
  public static final SignalHandler SIG_IGN = new NativeSignalHandler(1L);
  
  void handle(final Signal p0);
}
