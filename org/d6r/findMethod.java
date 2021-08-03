package org.d6r;

import bsh.Interpreter;
import bsh.CallStack;
import bsh.ClassIdentifier;
import org.d6r.Reflect;


public class findMethod {
  
  
  public static Object invoke(Interpreter env, CallStack cs, 
  Object o, String name, Class<?>... paramTypes) {
    
    return Reflect.findMethod(o, name, paramTypes);
    
  }
  
  public static Object invoke(Interpreter env, CallStack cs,
  Class<?> cls,  String name, Class<?>... paramTypes) {
    
    return Reflect.findMethod(cls, name, paramTypes);
    
  }
  
  public static Object invoke(Interpreter env, CallStack cs,
  ClassIdentifier ci,  String name, Class<?>... paramTypes) {
    
    return Reflect.findMethod(
      ci.getTargetClass(), name, paramTypes
    );
    
  }
  
  public static Object invoke(Interpreter env, CallStack cs,
  String clsName, String methodName, Class<?>... paramTypes) {
    
    return Reflect.findMethod(
      clsName, methodName, paramTypes
    );
    
  }
   
}


