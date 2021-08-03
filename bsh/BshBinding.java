package bsh;

import bsh.BshClassManager;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.Name;
import bsh.NameSource;
import bsh.SimpleNode;
import bsh.This;
import bsh.UtilEvalError;
import bsh.Variable;
import bsh.operators.OperatorProvider;
import java.io.InputStream;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public interface BshBinding 
         extends NameSource, 
                 BshClassManager.Listener 
{
  
  static String EMPTY = "".intern();
  
  boolean isMethod();
  boolean isClass();
  void setIsMethod(boolean flg);
  void setIsClass(boolean flg);
  Interpreter getInterpreter();
  void setInterpreter(Interpreter in);
  void addNameSourceListener(NameSource.Listener listener);
  void cacheClass(String s, Class<?> cls);
  Class<?> classForName(String s)
    throws UtilEvalError;
  void classLoaderChanged();
  void clear();
  BshBinding copy();
  void doSuperImport()
    throws UtilEvalError;
  void ensureVariables();
  Object get(String s, Interpreter in)
    throws UtilEvalError;
  String[] getAllNames();
  void getAllNamesAux(Collection<? super String> namesOutput);
  SimpleNode getCallerInfoNode();
  Class<?> getClass(String s)
    throws UtilEvalError;
  Class<?> getClassImpl(String s)
    throws UtilEvalError;
  Object getClassInstance()
    throws UtilEvalError;
  BshClassManager getClassManager();
  BshClassManager getClassManager(boolean flg);
  Object getCommand(String s, Class<?>[] types, Interpreter in) 
    throws UtilEvalError;
  Collection<String> getCommands();
  Variable[] getDeclaredVariables();
  OperatorProvider getExtendedMethodProvider();
  This getGlobal(Interpreter in);
  Class<?> getImportedClassImpl(String s)
    throws UtilEvalError;
  BshMethod getImportedMethod(String s, Class<?>[] types) 
    throws UtilEvalError;
  Variable getImportedVar(String s)
    throws UtilEvalError;
  int getInvocationLine();
  String getInvocationText();
  String[] getMethodNames();
  BshMethod getMethod(String s, Class<?>[] types)
    throws UtilEvalError;
  BshMethod getMethod(String s, Class<?>[] types, boolean flg) 
    throws UtilEvalError;
  BshMethod[] getMethods();
  Map<String, List<BshMethod>> getMethodsByName();
  String getName();
  Name getNameResolver(String s);
  SimpleNode getNode();
  String getPackage();
  BshBinding getParent();
  This getSuper(Interpreter in);
  This getThis(Interpreter in);
  Object getVariable(String s)
    throws UtilEvalError;
  Object getVariable(String s, boolean flg)
    throws UtilEvalError;
  Variable getVariableImpl(String s, boolean flg)
    throws UtilEvalError;
  String[] getVariableNames();
  Map<String, Variable> getVariables();
  void setTypedVariable(String s, Class<?> cls, Object o, Modifiers var4)
    throws UtilEvalError;
  void setTypedVariable(String s, Class<?> cls, Object o, boolean flg)
    throws UtilEvalError;
  void setVariable(String s, Object o, boolean flg)
    throws UtilEvalError;
  void setVariable(String s, Object o, boolean flg, boolean flg2)
    throws UtilEvalError;
  void importClass(String className)
    throws UtilEvalError;
  File importCommands(String s)
    throws UtilEvalError;
  void importObject(Object o);
  void importPackage(String packageName);
  void importStatic(Class<?> cls);
  Object invokeMethod(String s, Object[] arr, Interpreter in) 
    throws EvalError;
  Object invokeMethod(String s, Object[] arr, Interpreter in, CallStack cs, 
    SimpleNode node) 
    throws EvalError;
  void loadDefaultImports() 
    throws UtilEvalError;
  BshMethod loadScriptedCommand(InputStream var1, String s, Class<?>[] types,
    String s2, Interpreter in)
    throws UtilEvalError;
  void nameSpaceChanged();
  void prune();
  void setClassInstance(Object o);
  void setClassManager(BshClassManager var1);
  void setClassStatic(Class<?> cls);
  void setLocalVariable(String s, Object o, boolean flg)
    throws UtilEvalError;
  void setMethod(BshMethod var1)
    throws UtilEvalError;
  void setName(String s);
  void setNode(SimpleNode var1);
  void setPackage(String s);
  void setParent(BshBinding var1);
  Variable unsetVariable(String s);
  Variable unset(String s);
  Object unwrapVariable(Variable var1)
    throws UtilEvalError;
  Pair<Variable, BshBinding> findVariable(String s);
}
