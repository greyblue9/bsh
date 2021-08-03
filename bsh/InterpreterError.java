package bsh;

public class InterpreterError extends RuntimeException {
  public InterpreterError(String s) {
    super(s);
  }

  public InterpreterError(String s, Throwable cause) {
    super(s, cause);
  }
}
