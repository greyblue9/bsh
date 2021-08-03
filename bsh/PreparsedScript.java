package bsh;

import bsh.BshBinding;
import bsh.BshMethod;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.This;
import bsh.UtilEvalError;
import bsh.classpath.ClassManagerImpl;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PreparsedScript {
  private final BshMethod _method;
  private final Interpreter _interpreter;

  public PreparsedScript(String source) throws EvalError {
    this(source, getDefaultClassLoader());
  }

  private static ClassLoader getDefaultClassLoader() {
    ClassLoader cl = null;

    try {
      cl = Thread.currentThread().getContextClassLoader();
    } catch (SecurityException var2) {
      ;
    }

    if(cl == null) {
      cl = PreparsedScript.class.getClassLoader();
    }

    return cl != null?cl:ClassLoader.getSystemClassLoader();
  }

  public PreparsedScript(String source, ClassLoader classLoader) throws EvalError {
    ClassManagerImpl classManager = new ClassManagerImpl();
    classManager.setClassLoader(classLoader);
    BshBinding nameSpace = Factory.get(NameSpace.class).make(new Object[]{classManager, "global"});
    this._interpreter = new Interpreter(new StringReader(""), System.out, System.err, false, nameSpace, (Interpreter)null, (String)null);

    try {
      This e = (This)this._interpreter.eval("__execute() { " + source + "\n" + "}\n" + "return this;");
      this._method = e.getNameSpace().getMethod("__execute", new Class[0], false);
    } catch (UtilEvalError var6) {
      throw new IllegalStateException(var6);
    }
  }

  public Object invoke(Map<String, ?> context) throws EvalError {
    BshBinding nameSpace = Factory.get(NameSpace.class).make(new Object[]{this._interpreter.getClassManager(), "BeanshellExecutable"});
    nameSpace.setParent(this._interpreter.getNameSpace());
    BshMethod method = new BshMethod(this._method.getName(), this._method.getReturnType(), this._method.getParameterNames(), this._method.getParameterTypes(), this._method.methodBody, nameSpace, this._method.getModifiers());
    Iterator var5 = context.entrySet().iterator();

    while(var5.hasNext()) {
      Entry result = (Entry)var5.next();

      try {
        Object e = result.getValue();
        nameSpace.setVariable((String)result.getKey(), e != null?e:Primitive.NULL, false);
      } catch (UtilEvalError var7) {
        throw new EvalError("cannot set variable \'" + (String)result.getKey() + '\'', var7);
      }
    }

    Object result1 = method.invoke(new Object[0], this._interpreter);
    return result1 instanceof Primitive?(((Primitive)result1).getType() == Void.TYPE?null:((Primitive)result1).getValue()):result1;
  }

  public void setOut(PrintStream value) {
    this._interpreter.setOut(value);
  }

  public void setErr(PrintStream value) {
    this._interpreter.setErr(value);
  }
}
