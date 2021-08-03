package bsh;

import org.d6r.Reflect;
import bsh.EvalError;
import java.lang.reflect.Member;
import java.io.Serializable;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ReflectError extends EvalError implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  transient Member attempted;
  
  public ReflectError() {
    this("");
  }

  public ReflectError(String s) {
    this(s, (Node)null);
  }

  public ReflectError(String s, Node node) {
    this(s, (Node)node, (CallStack)null);
  }

  public ReflectError(String s, CallStack callstack) {
    this(s, (Node)null, (CallStack)callstack);
  }

  public ReflectError(String s, Node node, CallStack callstack) {
    this(s, (Node)node, (CallStack)callstack, (Throwable)null);
  }

  public ReflectError(String s, CallStack callstack, Node node) {
    this(s, (Node)node, (CallStack)callstack, (Throwable)null);
  }

  public ReflectError(String s, Throwable cause) {
    this(s, (Node)null, (CallStack)null, cause);
  }
  
  public ReflectError(String s, Throwable cause, Member failed) {
    this(s, (Node)null, (CallStack)null, cause, failed);
  }

  public ReflectError(String s, CallStack callstack, Throwable cause) {
    this(
      s, 
          (Node) (callstack != null ? callstack.top() : null), 
          callstack, cause, 
          (Object[]) null
    );
  }

  public ReflectError(String s, Node node, Throwable cause) {
    this(s, node, (CallStack)null, cause, (Object[])null);
  }

  public ReflectError(String s, Throwable cause, Node node) {
    this(s, node, (CallStack)null, cause, (Object[])null);
  }

  public ReflectError(String s, Node node, CallStack callstack, Throwable cause)
  {
    this(s, node, callstack, cause, (Object[])null);
  }

  public ReflectError(String s, CallStack callstack, Node node, Throwable cause)
  {
    this(s, node, callstack, cause, (Object[])null);
  }
  
  public ReflectError(String s, Node node, CallStack callstack,
  Throwable cause, Object... extras)
  {
    super(s, node, callstack, cause, extras);
    try {
      // this.message = s;
      // this.node = node;
      EvalError causeAsEE = (cause instanceof EvalError)
        ? (EvalError) cause
        : null;
      
      if (this.callstack == null || this.callstack.depth() <= 1) {
        this.callstack =
          (callstack != null && callstack.depth() > 1)
            ? callstack.copy()
            : (causeAsEE != null && causeAsEE.callstack != null)
                ? causeAsEE.callstack.copy()
                : (causeAsEE != null && causeAsEE.__callstack__ != null)
                    ? causeAsEE.__callstack__.copy()
                    : null; // CallStack.getActiveCallStack().copy();
      }
      if (extras != null) {
        this.extras = extras;
        for (Object extra: extras) {
          if (extra instanceof Member) this.attempted = (Member) extra;
        }
      }
      
      if (causeAsEE != null) {
        if (__callstack__ == null) {
          Reflect.setfldval(this, "__callstack__", causeAsEE.__callstack__);
        }
        if (node == null) node = causeAsEE.node;
        if (fakeBlock == null) {
          Reflect.setfldval(this, "fakeBlock", causeAsEE.fakeBlock);
        }
        if (bshMethod == null) bshMethod = causeAsEE.bshMethod;
      }
      
    } finally {
      if (!(  cause instanceof ParseException
           || cause instanceof UtilEvalError
           || ExceptionUtils.getRootCause(this) instanceof ParseException))
      {
        Interpreter.addError(this);
      }
    }
  }
  
  public Member getAttempted() {
    return this.attempted;
  }
  
  
}
