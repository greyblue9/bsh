package org.d6r;

import java.io.Serializable;


public class IllegalActionError extends Error implements Serializable {
  
  private static final long serialVersionUID = 0L;
  protected boolean causeInitialized;
  
  public IllegalActionError() {
  }
  
  public IllegalActionError(final char detailMessage) {
    this((Object) String.valueOf(detailMessage));
  }
  
  public IllegalActionError(final double detailMessage) {
    this((Object) Double.toString(detailMessage));
  }
  
  public IllegalActionError(final float detailMessage) {
    this((Object) Float.toString(detailMessage));
  }
  
  public IllegalActionError(final int detailMessage) {
    this((Object) Integer.toString(detailMessage));
  }
  
  public IllegalActionError(final long detailMessage) {
    this((Object) Long.toString(detailMessage));
  }
  
  public IllegalActionError(final boolean detailMessage) {
    this((Object) String.valueOf(detailMessage));
  }
  
  public IllegalActionError(final Object detailMessage) {
    super(
      (detailMessage instanceof String)
        ? (String) detailMessage
        : String.valueOf(detailMessage)
    );
    if (detailMessage instanceof Throwable && !causeInitialized) {
      super.initCause((Throwable) detailMessage);
      causeInitialized = true;
    }
  }
  
  public IllegalActionError(final String detailMessage, final Throwable cause) {
    super(detailMessage, cause);
    causeInitialized = true;
  }
  
  public IllegalActionError(final Object detailMessage, final Throwable cause) {
    this(detailMessage);
    if (cause != null && !causeInitialized) {
      super.initCause(cause);
      causeInitialized = true;
    }
  }
  
  public IllegalActionError addThrowable(final Throwable t) {
    if (t == null) return this;
    if (!causeInitialized) {
      causeInitialized = true;
      super.initCause(t);
    } else {
      super.addSuppressed(t);
    }
    return this;
  }
  
}


