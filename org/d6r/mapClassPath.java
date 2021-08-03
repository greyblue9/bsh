package org.d6r;
import bsh.*;
import java.util.*;

public class mapClassPath {
    
  public static Map<String, String[]> invoke
  (Interpreter env, CallStack stack) 
  { 
    return ClassPathUtil.mapClassPath();
  }
   
}


