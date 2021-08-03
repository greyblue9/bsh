package bsh;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.script.ScriptContext;

public class ScriptContextEngineView implements Map<String, Object> {
  ScriptContext context;

  public ScriptContextEngineView(ScriptContext context) {
    this.context = context;
  }

  public int size() {
    return this.totalKeySet().size();
  }

  public boolean isEmpty() {
    return this.totalKeySet().size() == 0;
  }

  public boolean containsKey(Object key) {
    return key instanceof String?this.context.getAttributesScope((String)key) != -1:false;
  }

  public boolean containsValue(Object value) {
    Set values = this.totalValueSet();
    return values.contains(value);
  }

  public Object get(Object key) {
    return this.context.getAttribute((String)key);
  }

  public Object put(String key, Object value) {
    Object oldValue = this.context.getAttribute(key, 100);
    this.context.setAttribute(key, value, 100);
    return oldValue;
  }

  public void putAll(Map<? extends String, ? extends Object> t) {
    this.context.getBindings(100).putAll(t);
  }

  public Object remove(Object okey) {
    String key = (String)okey;
    Object oldValue = this.context.getAttribute(key, 100);
    this.context.removeAttribute(key, 100);
    return oldValue;
  }

  public void clear() {
    this.context.getBindings(100).clear();
  }

  public Set<String> keySet() {
    return this.totalKeySet();
  }

  public Collection<Object> values() {
    return this.totalValueSet();
  }

  public Set<Entry<String, Object>> entrySet() {
    throw new Error("unimplemented");
  }

  private Set<String> totalKeySet() {
    HashSet keys = new HashSet();
    List scopes = this.context.getScopes();
    Iterator var4 = scopes.iterator();

    while(var4.hasNext()) {
      int i = ((Integer)var4.next()).intValue();
      keys.addAll(this.context.getBindings(i).keySet());
    }

    return Collections.unmodifiableSet(keys);
  }

  private Set<Object> totalValueSet() {
    HashSet values = new HashSet();
    List scopes = this.context.getScopes();
    Iterator var4 = scopes.iterator();

    while(var4.hasNext()) {
      int i = ((Integer)var4.next()).intValue();
      values.addAll(this.context.getBindings(i).values());
    }

    return Collections.unmodifiableSet(values);
  }
}
