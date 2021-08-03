
package org.d6r;

public class NotImplementedException 
  extends UnsupportedOperationException
{
  
  private static long serialVersionUID = 20131021L;
  private String code;
  
  public NotImplementedException(String message, Throwable cause, 
  String code) 
  {
    super(message, cause);
    this.code = code;
  }
  
  public NotImplementedException(String message, Throwable cause) 
  {
    this(message, cause, null);
  }
  
  public NotImplementedException(String message) {
    this(message, (String)null);
  }

  public NotImplementedException(Throwable cause) {
    this(cause, null);
  }
  
  public NotImplementedException() {
    this("The method called is not implemented");
  }
  
  public NotImplementedException(String message, String code) {
    super(message);
    this.code = code;
  }
  
  public NotImplementedException(Throwable cause, String code) {
    super(cause);
    this.code = code;
  }
  
  
  public String getCode() {
    return this.code;
  }
}
