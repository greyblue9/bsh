package bsh;

import bsh.BshBinding;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.NameSpace;
import bsh.SimpleNode;
import bsh.Token;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Stack;
import org.apache.commons.lang3.ArrayUtils;
import org.d6r.CollectionUtil;
import org.d6r.Debug;

public class CallStack implements Serializable, Iterable<BshBinding> {
  
  private static final long serialVersionUID = 0L;
  private final Stack<BshBinding> stack = new Stack();
  private static CallStack activeCallStack;
  private static Object sync = new Object();

  public static CallStack getActiveCallStack() {
    return activeCallStack;
    /*
    Object var0 = sync;
    
    synchronized(sync) {
      return (CallStack)CollectionUtil.clone(activeCallStack);
    }
    */
  }

  private static CallStack setActiveCallStack(CallStack self) {
    //Object var1 = sync;
    //synchronized(sync) {
      activeCallStack = self;
    //return self;
   //}
   return self;
  }

  public CallStack() {
    setActiveCallStack(this);
    if(Interpreter.TRACE) {
      Debug.debug("CallStack::<init>()");
    }

  }

  public Iterator<BshBinding> iterator() {
    return (new ArrayList(Arrays.asList((BshBinding[])this.stack.toArray(new BshBinding[0])))).iterator();
  }

  public Iterable<BshBinding> descendingIterable() {
    BshBinding[] bs = (BshBinding[])this.stack.toArray(new BshBinding[0]);
    ArrayUtils.reverse(bs);
    return Arrays.asList(bs);
  }

  public CallStack(BshBinding namespace) {
    setActiveCallStack(this);
    this.push(namespace);
  }

  public void clear() {
    this.stack.removeAllElements();
  }

  public void push(BshBinding ns) {
    setActiveCallStack(this);
    this.stack.push(ns);
  }

  public BshBinding top() {
    setActiveCallStack(this);
    return (BshBinding)this.stack.peek();
  }

  public BshBinding get(int depth) {
    setActiveCallStack(this);
    int size = this.stack.size();
    return (BshBinding)(depth >= size?NameSpace.JAVACODE:(BshBinding)this.stack.get(size - 1 - depth));
  }

  public void set(int depth, BshBinding ns) {
    setActiveCallStack(this);
    if(Interpreter.TRACE) {
      System.err.printf("Callstack.set(int depth = %d, BshBinding ns = %s)\n    this.stack = %s\n    stack = %s\n", new Object[]{Integer.valueOf(depth), Debug.ToString(ns), Debug.ToString(this.stack), Arrays.toString(Thread.currentThread().getStackTrace()).replaceAll("^.*(CallStack\\.set)", "    $1").replace(", ", "\n    ").replace("]", "")});
    }

    this.stack.set(this.stack.size() - 1 - depth, ns);
  }

  public BshBinding pop() {
    setActiveCallStack(this);
    if(Interpreter.TRACE) {
      System.err.printf("Callstack.pop()\n    this.stack = %s\n    stack = %s\n", new Object[]{Debug.ToString(this.stack), Arrays.toString(Thread.currentThread().getStackTrace()).replaceAll("^.*(CallStack\\.pop)", "    $1").replace(", ", "\n    ").replace("]", "")});
    }

    try {
      return (BshBinding)this.stack.pop();
    } catch (EmptyStackException var2) {
      throw new InterpreterError("pop on empty CallStack");
    }
  }

  public BshBinding swap(BshBinding newTop) {
    setActiveCallStack(this);
    if(Interpreter.TRACE) {
      System.err.printf("Callstack.swap(BshBinding newTop = %s)\n    this.stack = %s\n    stack = %s\n", new Object[]{Debug.ToString(newTop), Debug.ToString(this.stack), Arrays.toString(Thread.currentThread().getStackTrace()).replaceAll("^.*(CallStack\\.swap)", "    $1").replace(", ", "\n    ").replace("]", "")});
    }

    int last = this.stack.size() - 1;
    BshBinding oldTop = (BshBinding)this.stack.get(last);
    this.stack.set(last, newTop);
    return oldTop;
  }

  public int depth() {
    setActiveCallStack(this);
    return this.stack.size();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    Iterator var3 = this.descendingIterable().iterator();

    while(true) {
      while(var3.hasNext()) {
        BshBinding ns = (BshBinding)var3.next();
        String name = ns.getName();
        name = name.replace("/AnonymousBlock", "").replace("AnonymousBlock/", "");
        if(name.lastIndexOf(47) != -1) {
          name = name.substring(name.lastIndexOf(47) + 1);
        }

        if(ns instanceof NameSpace && ((NameSpace)ns).callerInfoNode != null) {
          SimpleNode node = ((NameSpace)ns).callerInfoNode;
          int lineno = node.getLineNumber();
          Token tok = node.firstToken;
          int colStart = -1;
          int colEnd = -1;
          if(tok != null) {
            colStart = tok.beginColumn;
            colEnd = tok.endColumn;
          }

          String source = node.getSourceFile();
          String text = node.getText();
          source = source != null?source.replaceAll("^.*/([^/][^/]*)$", "$1"):"Unknown Source";
          if(text == null) {
            text = "";
          }

          

          sb.append(String.format("    at %s(%s:%d)%s%s\n", new Object[]{name, source, Integer.valueOf(lineno), text.length() > 0?"\n      ":"", text}));
        } else {
          sb.append(String.format("    at %s(Unknown Source:-1)\n", new Object[]{name}));
        }
      }

      return sb.toString();
    }
  }

  public CallStack copy() {
    CallStack cs = new CallStack();
    cs.stack.addAll(this.stack);
    return cs;
  }
}