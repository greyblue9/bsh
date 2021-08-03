package org.d6r;

import java.util.concurrent.BrokenBarrierException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;


public class ToyInterpreter implements IInterpreter  {
    
  public static Pattern EXIT_PATTERN = Pattern.compile(
  "^\\s*(?:exit|quit)(?:\\(\\))?\\s*(?:;.*|\\s*)$");
  
  public String prompt = "yi % ";
  
  
  @Override
  public boolean parseInput
  (String line, List<Object> outResult) 
  {
    if (line == null || line.length() == 0
    || EXIT_PATTERN.matcher(line).matches()) 
    {
      return exit(0);
    }
    
    return true;
  }
  
  
  @Override
  public String getHelloPrompt() {
    return prompt;
  }
  
  
  public boolean exit(int status) {
    System.err.printf(
      "\n[ %d ] %s out!\n", getClass().getSimpleName(),
      status
    );
    System.exit(status);
    Throwable e = new BrokenBarrierException(
      String.format(
        "%s: Attempt to call System.exit(%d) did not terminate program flow",
        getClass().getSimpleName(),
        status
      ));
    if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
    return false;
  }



}

    