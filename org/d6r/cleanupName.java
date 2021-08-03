package org.d6r;


import java.util.regex.Pattern;
import bsh.Interpreter;
import bsh.CallStack;

public class cleanupName {
  
  public static Pattern ptrn = Pattern.compile(
    "^(.*[^A-Za-z0-9_-]+)([A-Za-z0-9_-]+)[^A-Za-z0-9_-]*$"
  );

  public static String cleanupName(String name) {
    return ptrn.matcher(name).replaceAll("$1");
  }
  
  public static String invoke(Interpreter env, CallStack cs, 
  String name) 
  {
    return cleanupName(name);
  }
}


