package bsh;

import bsh.ParserConstants;
import bsh.SimpleNode;

public class ReturnControl implements ParserConstants {
  public int kind;
  public Object value;
  public Throwable cause;
  public SimpleNode returnPoint;

  public ReturnControl(int kind, Object value, SimpleNode returnPoint, Throwable cause) {
    this.kind = kind;
    this.value = value;
    this.returnPoint = returnPoint;
    this.cause = cause;
  }

  public ReturnControl(int kind, Object value, SimpleNode returnPoint) {
    this(kind, value, returnPoint, (Throwable)null);
  }
}
