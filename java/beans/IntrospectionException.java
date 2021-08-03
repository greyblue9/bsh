package java.beans;


public class IntrospectionException extends Exception {

  public IntrospectionException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public IntrospectionException(String message) {
    super(message);
  }
  
  public IntrospectionException(Throwable cause) {
    super(cause);
  }
  
  public IntrospectionException() {
    super();
  }

}

