package bsh;

import bsh.CallStack;
import bsh.ErrorWithContext;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.SimpleNode;
import java.io.Serializable;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.CollectionUtil;

public class UtilEvalError extends Error implements Serializable, ErrorWithContext {
  
  private static final long serialVersionUID = 1L;  
  
  transient Object context;

  public UtilEvalError(String msg, Throwable cause, Object ctx) {
    super(msg, cause);
    this.context = ctx;
  }
  
  public UtilEvalError(String msg, Throwable cause) {
    this(msg, cause, (Object)null);
  }

  public UtilEvalError(Throwable cause) {
    this(String.format("%s: %s", new Object[]{cause.getClass().getName(), cause.getMessage()}), cause);
  }

  public UtilEvalError(String msg) {
    this(msg, (Throwable)null);
  }

  public UtilEvalError() {
    this((String)null);
  }

  public Object getContext() {
    return this.context;
  }
  
  public EvalError toEvalError(String msg, SimpleNode node, CallStack cs,
  Throwable cause) 
  {
    if (Interpreter.DEBUG) {
      this.printStackTrace();
    }

    if(this.context == null) {
      this.context = Pair.of(node, cs);
    }

    return new EvalError(
      (msg != null)
        ? String.format("%s:\n    %s", msg, this.getMessage())
        : this.getMessage(), 
      node, 
      cs, 
      cause
    );
  }
  
  public EvalError toEvalError(String msg, SimpleNode node, CallStack cs) {
    return this.toEvalError(msg, node, cs, this);
  }

  public EvalError toEvalError(SimpleNode node, CallStack cs) {
    return this.toEvalError((String)null, node, cs);
  }
}