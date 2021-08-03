package java.lang;


public class ReflectiveOperationException extends Exception {
  
  public ReflectiveOperationException() {
  
  }

  public ReflectiveOperationException(final String detailMessage) {
    super(detailMessage);
  }

  public ReflectiveOperationException(final String detailMessage,
  final Throwable throwable) 
  {
    super(detailMessage, throwable);
  }

  public ReflectiveOperationException(final Throwable throwable) {
    super(throwable);
  }
  
}



