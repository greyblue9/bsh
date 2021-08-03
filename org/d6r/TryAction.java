package org.d6r;


import java.util.List;
import java.util.ArrayList;

public abstract class TryAction<V, R> 
{  
  public final List<Throwable> actionErrors 
    = new ArrayList<Throwable>();
    
  public V param;
  public Object result;
  public R resultOnError;  
  public boolean isRun;
  
  public TryAction(final V param, final R resultOnError)
  {
    this.param = (V) param;
    this.resultOnError = (R) resultOnError;
    this.result = (R) resultOnError;
    isRun = false;
  }
  
  public abstract R target(V param);
  
  public R run() {
    if (isRun) return (R) result;
    isRun = true;
    try {
    result = target((V) param);
    } catch (Throwable e) {
    actionErrors.add(e);
    String paramToStr = "???";
    String paramClassName = param != null
      ? param.getClass().getName()
      : "null";
    try {
      paramToStr = param == null
      ? "<NULL>"
      : param.toString();
    } catch (Throwable tse) {      
      paramToStr = String.format(
      "<(%s param).toString() threw %s: %s>",
      paramClassName,
      tse.getClass().getSimpleName(),
      tse.getMessage() != null
        ? tse.getMessage(): "no msg"
      );
    }
    System.err.printf(
     "Call to target((%s) param = %s) threw %s: [%s]\n",
      paramClassName, 
      paramToStr,
      e.getClass().getSimpleName(),
      e.getMessage() != null? e.getMessage(): "no msg"
    );
    }
    return (R) result;
  }

}





