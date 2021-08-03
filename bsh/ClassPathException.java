package bsh;

import bsh.UtilEvalError;

public class ClassPathException extends UtilEvalError {
  public ClassPathException(String msg) {
    super(msg);
  }
}
