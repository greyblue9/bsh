package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Node;
import bsh.SimpleNode;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class TargetError extends EvalError implements Serializable {
  
  private final boolean inNativeCode;
  Throwable throwable;
  transient GenericDeclaration context;
  transient Object[] args;
  transient Object receiver;

  public TargetError(String msg, Throwable t, SimpleNode node, CallStack callstack, boolean inNativeCode) {
    super(msg, (Node)node, (CallStack)callstack, t);
    this.inNativeCode = inNativeCode;
  }

  public TargetError(Throwable t, SimpleNode node, CallStack callstack) {
    this(
      ExceptionUtils.getRootCauseMessage(t),
      t, node, callstack, false
    );
  }

  public Throwable getThrowable() {
    return this.throwable;
  }

  public GenericDeclaration getContext() {
    return this.context;
  }

  public Object[] getArguments() {
    return this.args;
  }

  public Object getReceiver() {
    return this.receiver;
  }

  public Throwable getTarget() {
    Throwable target = this.getCause();
    if(target == this || target == null) {
      target = this.throwable;
    }

    return target instanceof InvocationTargetException?((InvocationTargetException)target).getTargetException():(target instanceof TargetError && target != this?((TargetError)target).getTarget():target);
  }

  public String getMessage() {
    Throwable target = getTarget();
    try {
    return String.format(
      "%s\nTarget exception: %s: %s",
      super.getMessage(),
      target.getClass().getSimpleName(), target.getMessage()
    );
    } catch (Throwable e2) {
      return org.d6r.Reflect.getfldval(target, "detailMessage");
    }
  }

  public void printStackTrace(boolean dbg, PrintStream err) {
    if (dbg) {
      err.println("--- Target Stack Trace ---");
    }    
    super.printStackTrace(err);
  }
  
  private String printTargetError() {
    StringBuilder sb = new StringBuilder(128);
    if(this.getCause() != null && this.getCause() != this) {
      sb.append(this.xPrintTargetError(this.getCause())).append('\n');
    }

    if(this.throwable != null && this.throwable != this) {
      sb.append(this.xPrintTargetError(this.throwable)).append('\n');
    }

    return sb.toString();
  }
  
  public String xPrintTargetError(final Throwable t) {
    StringBuilder result = new StringBuilder();
    Throwable target = t;
    Throwable lastTarget = null;
    
    while (target != null && target != lastTarget) { 
      lastTarget = target;
      target = TargetError.getTargetException(target, true);
      
      result.append(String.format(
        "Nested: %s\n", target
      ));
    }
    return result.toString();
  }

  public static Throwable getTargetException(final Throwable t,
    final boolean singleStep)
  {
    Throwable target = t;
    Throwable lastTarget = null;
    
    while (!singleStep || (lastTarget == null && target != null) &&
           target != lastTarget &&
           target instanceof UndeclaredThrowableException ||
           target instanceof InvocationTargetException ||
           target instanceof TargetError ||
           target instanceof EvalError)
    {
      lastTarget = target;
      
      if (target instanceof EvalError &&
        !(target instanceof TargetError))
      {
        final EvalError e = (EvalError) target;
        final Map<String, Object> data = e.getData();
        
        if (data.containsKey("exception")) {
          target = (Throwable) data.get("exception");
        }
        if (target == lastTarget && data.containsKey("throwables")) {
          final Object values = data.get("throwables");
          final Iterable<?> valuesIterable = (values instanceof Object[])
            ? (Iterable<?>) Arrays.<Object>asList((Object[]) values)
            : (Iterable<?>) values;
          for (final Object obj: valuesIterable) {
            if (obj instanceof Throwable && obj != target) {
              target = (Throwable) obj;
              break;
            }
          }
        }
      } else {
        target = (target instanceof UndeclaredThrowableException)
          ? ((UndeclaredThrowableException) target).getUndeclaredThrowable()
          : (target instanceof InvocationTargetException)
              ? ((InvocationTargetException) target).getTargetException()
              : (target instanceof TargetError)
                  ? ((TargetError) target).getTarget()
                  : target;
      }
    }
    return (target != null) ? target : lastTarget;
  }
  
  public boolean inNativeCode() {
    return this.inNativeCode;
  }
}






