package com.android.dex;

import com.android.dex.util.ExceptionWithContext;

public final class DexException extends ExceptionWithContext {

  public DexException(final String s) {
    super(s);
    if (s.indexOf("magic") != -1) printStackTrace();
  }

  public DexException(final Throwable t) {
    super(t);
    if (t.getMessage() != null && t.getMessage().indexOf("magic") != -1) {
      printStackTrace();
    }
  }
  
}




