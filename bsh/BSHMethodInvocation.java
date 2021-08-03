package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHArguments;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Name;
import bsh.ReflectError;
import bsh.SimpleNode;

import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import org.d6r.Reflector.Util;

import org.d6r.Debug;
import java.util.List;
import java.util.LinkedList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;


public class BSHMethodInvocation extends SimpleNode<ReflectError> {
  public BSHMethodInvocation(int id) {
    super(id);
  }

  public BSHAmbiguousName getNameNode() {
    return (BSHAmbiguousName)this.jjtGetChild(0);
  }

  public BSHArguments getArgsNode() {
    return (BSHArguments)this.jjtGetChild(1);
  }

  public Object eval(CallStack callstack, Interpreter interpreter)
    throws ReflectError
  {
    super.touch(callstack);
    
    if (Interpreter.TRACE) System.err.printf(
      "BSHMethodInvocation.eval(callstack=%s, interpreter=%s)\n",
      new Object[]{callstack, interpreter}
    );
    
    BshBinding namespace = callstack.top();
    if (namespace == null) namespace = interpreter.getNameSpace();
    
    
    BSHAmbiguousName nameNode = this.getNameNode();
    if (nameNode == null) throw new IllegalStateException(String.format(
      "nameNode == null; this: %s", Debug.ToString(this)
    ));
    BSHArguments argsNode = this.getArgsNode();
    if (argsNode == null) throw new IllegalStateException(String.format(
      "argsNode == null; this: %s", Debug.ToString(this)
    ));
    
    Name name = null;
    Object[] args = null;
    Object result = null;
    CallStack callstack0 = null;
    /*if(namespace == null 
    || && namespace.getParent() == null 
    || && namespace.getParent().isClass()
    || !nameNode.text.equals("super") // ADDED '(', ')'
    && !nameNode.text.equals("this")))) { // TODO: XXX??!
       return namespace.getParent();*/
    
    if (callstack.top().getName().equals("global")) {
      callstack = CallStack.getActiveCallStack();
    }
    
    CallStack savedCallstack = null;
    
    try {
      
      try {
        nameNode.touch(callstack);
        name = nameNode.getName(namespace);
      } finally {
        //if (name != null) super.touch(callstack);
      }
      try {
        argsNode.touch(callstack);
        args = argsNode.getArguments(callstack, interpreter);
      } finally {
        //if (args != null) super.touch(callstack);
      }
      try {
        this.touch(callstack);
        savedCallstack = callstack.copy();
        result = name.invokeMethod(interpreter, args, callstack, this);
        return result;
      } finally {
        //if (result == null) {
        //callstack0 = CallStack.getActiveCallStack().copy();
        //}
      } 
      
    
    } catch (Throwable rawThrowable) {
        
        List<Throwable> prunedThrowables = new LinkedList<>();
        Throwable ex = rawThrowable, lastEx = null;
        do {
          lastEx = ex;          
          if (lastEx != null) prunedThrowables.add(lastEx);
          if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException)ex).getTargetException();
          } else if (ex instanceof UndeclaredThrowableException) {
            ex = ((UndeclaredThrowableException)ex).getUndeclaredThrowable();
          } else if (ex instanceof ExceptionInInitializerError) {
            Throwable t1 = ((ExceptionInInitializerError)ex).getException();
            Throwable t2 = ((ExceptionInInitializerError)ex).getCause();
            if (t1 == null && t2 != null) { t1 = t2; t2 = null; }
            if (t2 != null && t2 != t1 && t2 != ex && t2 != rawThrowable) {
              t1.addSuppressed(t2);
            }
            if (t1 == null) break;
            ex = t1;
          } else break;
        } while (lastEx != ex && ex != null);
        
        
        ReflectError re = (ex instanceof ReflectError)
          ? (ReflectError) ex
          : null;
        if (re != null) throw re;
        
        Object base = name.evalBaseObject;
        Class cls = base != null && !(base instanceof Primitive)
          ? base.getClass()
          : name.classOfStaticMethod;
        Method method = null;
        
        if (cls != null) {
          String memberName = (String) 
            org.d6r.Reflect.getfldval(name, "evalName");
          boolean dot = false;
          if (memberName != null) {
            int dot1 = memberName.lastIndexOf('.');
            if (dot1 != -1) {
              memberName = memberName.substring(dot1 + 1);
            }
            if (memberName != null) {
              method = org.d6r.Reflect.findMethod(cls, memberName);
            }
          }
        }
        
        String message
          = org.d6r.Reflect.getfldval(ex, "detailMessage", false);
        
        re = new ReflectError(          
          String.format(
            (base != null)
              ? "Trouble invoking method %2$s.%1$s in expression '%3$s': "+
                "%4$s%5$s"
              : "Trouble invoking function %1$s in expression '%3$s': "+
                "%4$s%5$s",
            nameNode.getText(),
            base,
            (jjtGetParent() != null)
              ? (jjtGetParent().jjtGetParent() != null)
                  ? jjtGetParent().jjtGetParent()
                  : jjtGetParent()
              : this,
            ex.getClass().getSimpleName(), // %4
            (message != null? String.format(": %s", message): "")
          ),
          ex,
          method
        );
        
        re.setNode(this);
        re.callstack = callstack0 != null
          ? callstack0
          : callstack;          
        
        re.getData().put("exception", ex);
        re.getData().put("args", args);
        re.getData().put("throwables", prunedThrowables);
        re.getData().put("methodInvocation", this);
        re.getData().put(
          "node", callstack0 != null ? callstack0.top(): this
        );
        re.getData().put("callstack", savedCallstack);
        throw re;
      /*catch (EvalError var16) {
        var16.setNode(this);
        var16.callstack = CallStack.getActiveCallStack().copy();
        var16.getData().put("args", args);
        throw Util.sneakyThrow(var16);
      }*/
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(760);
    sb.append(this.getNameNode().getText());
    sb.append("(");
    String[] args = this.getArgsNode().getArgumentText();
    sb.append(StringUtils.join(args, ", "));
    sb.append(")");
    return sb.toString();
  }
}