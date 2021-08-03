package org.d6r.etc;

import org.d6r.*;

import edu.rice.cs.dynamicjava.Options;
import edu.rice.cs.dynamicjava.interpreter.ImportContext;
import edu.rice.cs.dynamicjava.interpreter.TypeContext;
import bsh.classpath.AndroidClassLoader;
import edu.rice.cs.dynamicjava.interpreter.RuntimeBindings;
import edu.rice.cs.dynamicjava.interpreter.Interpreter;
import edu.rice.cs.plt.tuple.Option;

import edu.rice.cs.dynamicjava.symbol.DJClass;
import edu.rice.cs.dynamicjava.symbol.type.VariableType;
import edu.rice.cs.dynamicjava.symbol.type.Type;
import edu.rice.cs.dynamicjava.symbol.LocalVariable;
import edu.rice.cs.dynamicjava.symbol.TypeSystem;
import edu.rice.cs.dynamicjava.symbol.StandardTypeSystem;
import edu.rice.cs.dynamicjava.symbol.ExtendedTypeSystem;
import com.google.common.base.Function;
import edu.rice.cs.dynamicjava.interpreter.InterpreterException;

import java.util.*;


public class DynJava {
  
  static DynJava _instance;
  
  Options opts;
  ExtendedTypeSystem ts;
  ClassLoader parentLoader;
  AndroidClassLoader acl;
  TypeContext ctx;
  
  Map<DJClass, Object> thisVals;
  Map<VariableType, Type> tvars;
  Map<LocalVariable, Object> vars;
  RuntimeBindings bindings;
  
  Interpreter interp;
  static List<Throwable> errors;
  
  public static boolean packCaptureVars = true;
  public static boolean boxingInMostSpecific = true;
  public static boolean useExplicitTypeArgs = true;
  public static boolean strictClassEquality = false;

  public DynJava() {
    this.opts = Options.DEFAULT;
    this.ts = new ExtendedTypeSystem(
      opts, 
      packCaptureVars, boxingInMostSpecific, 
      useExplicitTypeArgs, strictClassEquality
    );
    this.parentLoader = Thread.currentThread().getContextClassLoader();
    this.acl = new AndroidClassLoader(parentLoader);    
    this.ctx = new ImportContext(acl, opts);
    
    this.vars = new HashMap<LocalVariable, Object>();
    this.tvars = new HashMap<VariableType, Type>();
    this.thisVals = new HashMap<DJClass, Object>();    
    this.bindings = new RuntimeBindings(null, vars, tvars, thisVals);
    
    this.interp = new Interpreter(opts, ctx, bindings);
    this.errors = new ArrayList<Throwable>();
  }
  
  public static DynJava getInstance() { //;
    if (_instance == null) { //;
      _instance = new DynJava();
    }
    return _instance;
  }
  
  public <T> T eval(String src) { //; 
    try {
      Option<T> optrslt = (Option<T>) (Option<?>) interp.interpret(src);
      T rslt = (T) (
        (optrslt.isSome())
          ? (Object) optrslt.unwrap()
          : (Object) null
      ); 
      return rslt;
    } catch (Throwable e) { //; 
      errors.add(e);
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
    }
    return null;
  }
  
  
  public static Object invoke(bsh.Interpreter in, bsh.CallStack cs,   
  Object... args) 
  {
    return getInstance().eval((String) args[0]);
  }
  
  public Map<LocalVariable, Object> getVariables() { //;
    return this.vars;
  }
  
  public LocalVariable getLocalVariable(String name) { //;
    return ctx.getLocalVariable(name, ts);
  }
  
  public TypeContext importField(DJClass c, String name) { //;
    return ctx.importField(c, name);
  }
  public TypeContext importMemberClass(DJClass outer, String name) { //;
    return ctx.importMemberClass(outer, name);
  }
  public TypeContext importMemberClasses(DJClass outer) { //;
    return ctx.importMemberClasses(outer);
  }
  public TypeContext importMethod(DJClass c, String name) { //;
    return ctx.importMethod(c, name);
  }
  public TypeContext importStaticMembers(DJClass c) { //;
    return ctx.importStaticMembers(c);
  }
  public TypeContext importTopLevelClass(DJClass c) { //;
    return ctx.importTopLevelClass(c);
  }
  public TypeContext importTopLevelClasses(String pkg) { //;
    return ctx.importTopLevelClasses(pkg);    
  }
  
  public DJClass getThis() {
    return ctx.getThis();
  }
  public DJClass getThis(Type expected, TypeSystem ts) {
    return ctx.getThis(expected, ts);
  }
  public DJClass getThis(String className) {
    return ctx.getThis(className);
  }
  
  public static <V, R> Function<V, R> fn(final String src) {
    
    return new Function<V, R>() {      
      DynJava dj = getInstance();
      String source = src;
      
      @Override
      public R apply(V val) {
        try {
          Object rslt = dj.interp.interpret(source);        
          return (R) rslt;
        } catch (InterpreterException ex) {
          throw Reflector.Util.sneakyThrow(ex);
        }
        //return null;
      }
      
      @Override
      public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != getClass()) return false;
        return source.equals((String) Reflect.getfldval(other, "source"));
      }
      
    }; 
  }
}


