package org.d6r;
import java.util.*;

public interface IInterpreter {
  
  boolean parseInput(String line, List<Object> outResult);
  
  String getHelloPrompt();
  
}

