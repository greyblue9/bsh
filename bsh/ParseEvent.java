
package bsh;
import java.util.*;

public enum ParseEvent{

  EOF; 
  
  static WeakHashMap<Object, List<Object>> subscribersBySource
   = new WeakHashMap<Object, List<Object>>();
  
  
  public Object fire(Object source, Object... data) {
    ParseEvent evt = this;
    List<Object> subscribers = subscribersBySource.get(source);
    if (subscribers == null || subscribers.isEmpty()) {
      return Null.NULL;
    }
      
    Object result = Null.NULL;
    for (Object subscriber: subscribers) {
      if (subscriber instanceof EventReceiver) {
        try {
          result 
            = ((EventReceiver) subscriber).receive(source, evt, data);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
    return result;
  } 
  
  
  public static void 
  subscribeTo(Object source, EventReceiver subscriber) 
  {
    List<Object> subscribers = subscribersBySource.get(source);
    if (subscribers == null) {
      subscribersBySource.put(
        source, (subscribers = new ArrayList<Object>())
      );      
    }
    subscribers.add(subscriber);
  }
  
  
}


