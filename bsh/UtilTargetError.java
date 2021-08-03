package bsh;

import bsh.CallStack;
import bsh.RemoveMe;
import bsh.SimpleNode;
import bsh.TargetError;
import bsh.UtilEvalError;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.d6r.RealArrayMap;

@RemoveMe({"To little usfulness, confusing, sscatterbrained API."})
public class UtilTargetError extends UtilEvalError
  implements Map<String, Object>, Iterable<Entry<String, Object>>, Serializable
{
  private static final long serialVersionUID = 1L;
  
  public Throwable t;
  
  public UtilTargetError(String message, Throwable cause) {
    super(message != null?message:(cause != null?((Throwable)cause).toString():"UtilTargetError from ".concat((new Error()).getStackTrace()[3].toString())));
    if(cause == null) {
      cause = new Error(String.format("Unknown error from %s", new Object[]{(new Error()).getStackTrace()[3]}));
    }

    org.d6r.Reflect.setfldval(this, "cause", this.t = (Throwable)cause);
  }

  public UtilTargetError(Throwable cause) {
    this((String)null, cause);
  }

  public TargetError toEvalError(String msg, SimpleNode node, CallStack callstack) {
    if(msg == null) {
      msg = this.getMessage();
    } else {
      msg = msg + ": " + this.getMessage();
    }

    return new TargetError(msg, this.t, node, callstack, false);
  }

  public String toString() {
    if(this.getMessage() == null && this.t != null) {
      Throwable cause = (Throwable)org.d6r.Reflect.getfldval(this, "cause");
      return this.getClass().getSimpleName().concat(": ").concat(this.t != null?this.t.toString():(cause != null?cause.toString():"<no cause information>"));
    } else {
      return this.getClass().getSimpleName().concat(": ").concat(this.getMessage());
    }
  }
  
  public Throwable getCause() {
    return (Throwable)org.d6r.Reflect.getfldval(this, "cause");
  }
  
  public Map<String, Object> getData() {
    Object ss = org.d6r.Reflect.get(this, "stackState");
    Object map;
    if(ss instanceof Map) {
      map = (Map)ss;
    } else {
      if(ss instanceof int[]) {
        this.getStackTrace();
      }

      map = new RealArrayMap(4);
      org.d6r.Reflect.setfldval(this, "stackState", map);
    }
    
    return (Map)map;
  }
  
  public void clear() {
    this.getData().clear();
  }
  
  public boolean containsKey(Object key) {
    return this.getData().containsKey(key);
  }

  public boolean containsValue(Object value) {
    return this.getData().containsValue(value);
  }

  public Set<Entry<String, Object>> entrySet() {
    return this.getData().entrySet();
  }

  public Object get(Object key) {
    return this.getData().get(key);
  }

  public boolean isEmpty() {
    return this.getData().isEmpty();
  }

  public Set<String> keySet() {
    return this.getData().keySet();
  }

  public Object put(String key, Object value) {
    return this.getData().put(key, value);
  }

  public void putAll(Map<? extends String, ? extends Object> inMap) {
    this.getData().putAll(inMap);
  }

  public Object remove(Object key) {
    return this.getData().remove(key);
  }

  public int size() {
    return this.getData().size();
  }

  public Collection<Object> values() {
    return this.getData().values();
  }

  public Iterator<Entry<String, Object>> iterator() {
    return this.entrySet().iterator();
  }
}

