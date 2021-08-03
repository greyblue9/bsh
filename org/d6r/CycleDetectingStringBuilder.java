package org.d6r;

import org.d6r.OpenStringBuffer;
import java.util.*;


public class CycleDetectingStringBuilder extends OpenStringBuffer {
  
  public final Set<Object> seen = new IdentityHashSet<>();
  protected boolean stop = false;
  
  public CycleDetectingStringBuilder() {
    super();
  }
  
  @Override
  public OpenStringBuffer append(Object obj) {
     if (!stop) {
       if (obj == null) {
         this.appendNull();
       } else {
         if (!seen.add(obj)) {
           append0("<recursion>");
           stop = true;
           return this;
         }
         if (!stop) {
         String s = Debug.ToString(obj);
         if (s.indexOf("StackOverflowError") != -1) {
           append0(s.length() > 256? s.substring(0, 253) + "...": s);
           stop = true;
           return this;
         } 
         append0(s.length() > 256? s.substring(0, 253) + "...": s);
       }
     }
    }
    return this;
  }
  
  @Override
  public OpenStringBuffer append(String s) {
    if (!stop) {
      this.append0(
        (s != null)
          ? (s.length() > 256? s.substring(0, 253) + "...": s)
          : "null"
      );
    }
    return this;
  }
  
}



