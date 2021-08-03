package org.d6r;


public class RuntimeExceptionCompat
     extends RuntimeException
{
  public RuntimeExceptionCompat() {
    super();
  }

  public RuntimeExceptionCompat(String message) {
    super(message);
  }

  public RuntimeExceptionCompat(String message, Throwable cause) {
    super(message, cause);
  }
  
  public RuntimeExceptionCompat(String message, Throwable cause, 
  boolean enableSuppression)
  {
    this(message, cause);
  }
  
  public RuntimeExceptionCompat(String message, Throwable cause, 
  boolean enableSuppression, boolean writableStackTrace)
  {
    this(message, cause, enableSuppression);
  }
  
  public RuntimeExceptionCompat(Throwable cause) {
    super(cause);
  }
}


