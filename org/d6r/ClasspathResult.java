package org.d6r;
import java.util.*;


public class ClasspathResult<T> extends ArrayList<T> {
  
  private List<T> unhandled = new ArrayList<T>();
  
  public void addUnhandled(T item) {
    unhandled.add(item);
  }
  public Collection<T> getUnhandled() {
    return Collections.<T>unmodifiableCollection(unhandled);
  }
  public T getUnhandled(int idx) {
    return unhandled.get(idx);
  }
  public Iterator unhandledIterator() {
    return unhandled.<T>iterator();
  }
  
  
  
}
