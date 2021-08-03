package bsh;

import bsh.BSHAmbiguousName;
import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.Interpreter;
import bsh.Name;
import bsh.NameSpace;
import bsh.Node;
import bsh.SimpleNode;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.*;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.d6r.Debug;
import org.d6r.CollectionUtil;
import org.d6r.RealArrayMap;


public class EvalError extends RuntimeException implements Map<String, Object>, Iterable<Entry<String, Object>>, Serializable {
  public transient Node node;
  protected String message;
  public transient CallStack callstack;
  public transient Name name;
  public transient BSHAmbiguousName nameNode;
  public transient BshBinding namespace;
  public transient BshMethod bshMethod;
  public transient Object[] args;
  transient Interpreter interpreter;
  public Throwable ex;
  public transient Node otherNode;
  public Serializable ser;
  transient Method method;
  transient Field fld;
  public File file;
  public transient BshBinding ns;
  public transient Object[] extras;
  private transient Field[] fields;
  
  public final transient BSHBlock fakeBlock;
  public final transient CallStack __callstack__;
  
  private static final long serialVersionUID = 2L;
    
  {
    fakeBlock = new BSHBlock(0);
    try {
      fakeBlock.children = ((ArrayList<SimpleNode>) 
        ((ArrayList<SimpleNode>) (Object)
        org.d6r.Reflect.getfldval(
          (Object) org.d6r.CollectionUtil.getInterpreter().get_jjtree(),
          "nodes", true
        )).clone()).toArray(new SimpleNode[0]);
    } catch (Exception e) {
      System.err.println(e);
      fakeBlock.children = ((ArrayList<SimpleNode>) (Object)
        org.d6r.Reflect.getfldval(
          (Object) org.d6r.CollectionUtil.getInterpreter().get_jjtree(),
          "nodes", true
        )).toArray(new SimpleNode[0]);
    }
    __callstack__ = CallStack.getActiveCallStack().copy();
    if (__callstack__ != null && __callstack__.depth() == 1) {
      ((NameSpace) __callstack__.top()).callerInfoNode = fakeBlock;
    }
  }

  public EvalError() {
    this(org.d6r.Debug.getCallingMethod(2).toString());
  }

  public EvalError(String s) {
    this(s, (Node)null);
  }

  public EvalError(String s, Node node) {
    this(s, (Node)node, (CallStack)null);
  }

  public EvalError(String s, CallStack callstack) {
    this(s, (Node)null, (CallStack)callstack);
  }

  public EvalError(String s, Node node, CallStack callstack) {
    this(s, (Node)node, (CallStack)callstack, (Throwable)null);
  }

  public EvalError(String s, CallStack callstack, Node node) {
    this(s, (Node)node, (CallStack)callstack, (Throwable)null);
  }

  public EvalError(String s, Throwable cause) {
    this(s, (Node)null, (CallStack)null, cause);
  }

  public EvalError(String s, CallStack callstack, Throwable cause) {
    this(s, (Node)(callstack != null?callstack.top():null), callstack, cause, (Object[])null);
  }

  public EvalError(String s, Node node, Throwable cause) {
    this(s, node, (CallStack)null, cause, (Object[])null);
  }

  public EvalError(String s, Throwable cause, Node node) {
    this(s, node, (CallStack)null, cause, (Object[])null);
  }

  public EvalError(String s, Node node, CallStack callstack, Throwable cause) {
    this(s, node, callstack, cause, (Object[])null);
  }

  public EvalError(String s, CallStack callstack, Node node, Throwable cause) {
    this(s, node, callstack, cause, (Object[])null);
  }

  public EvalError(String s, Node node, CallStack callstack, Throwable cause, Object... extras) {
    super(s, cause);

    this.callstack 
      = ((callstack != null)
          ? callstack
          : (this.callstack != null
             ? this.callstack
             : CallStack.getActiveCallStack())
        ).copy();
    if (extras != null) this.extras = extras;
    this.namespace = (this.callstack != null && this.callstack.depth() > 0)
      ? this.callstack.top()
      : null;
    this.node = node != null? node: getSimpleNode();
    Interpreter.addError(this);
  }
  
  
  
  
  @Nonnull
  public SimpleNode getSimpleNode() {    

    SimpleNode _node = SimpleNode.DEFAULT;
    
    if ((_node = (SimpleNode) this.node) != null
    ||  (_node = (SimpleNode) this.getData().get("node")) != null
    ||  (_node = (SimpleNode) this.otherNode) != null) {
      return _node;
    }
        
    Throwable c = org.d6r.Reflect.getfldval(this, "cause");
    if (c instanceof TargetError) {
      this.getData().putAll(((TargetError) c).getData());
    }
    if (c != this && c instanceof EvalError) {
      final EvalError ce = (EvalError) c; 
      if ((_node = (SimpleNode) ce.node) != null
      ||  (_node = (SimpleNode) ce.getData().get("node")) != null
      ||  (_node = (SimpleNode) ce.otherNode) != null) {
        return _node;
      }
    }
    
    c = (Throwable) getData().get("exception");
    if (c != this && c instanceof EvalError) {
      final EvalError ce = (EvalError) c; 
      if ((_node = (SimpleNode) ce.node) != null
      ||  (_node = (SimpleNode) ce.getData().get("node")) != null
      ||  (_node = (SimpleNode) ce.otherNode) != null) {
        return _node;
      }
    }
    
    if (callstack != null && callstack.depth() > 0) {
      if ((_node = ((NameSpace) callstack.top()).callerInfoNode) != null) {
        return _node;
      }
      try {
        for (Iterator<NameSpace> it = ((Iterable<NameSpace>) 
            (Iterable<?>) callstack).iterator(); it.hasNext();)
        {
          NameSpace ns = it.next();
          if ((ns.callerInfoNode) != null) {
            if (ns.getName().equals("global") 
            ||  ns.callerInfoNode instanceof BSHMethodDeclaration) {
              ns.callerInfoNode = null; 
            }
            if (ns.getNode(false) == null
            ||  ns.getNode(false) == ns.callerInfoNode) {
              return ns.callerInfoNode;
            }
          }
          
        }
      } catch (ClassCastException exc) {
        System.err.println(exc);
      }
    }
    
    Throwable var12 = (Throwable) this.getData().get("exception");
    if (var12 != null && var12 instanceof EvalError && var12 != this) {
      return ((EvalError) var12).getSimpleNode();
    }
    return SimpleNode.DEFAULT;
  }
  
  @Nullable
  public SimpleNode getNode() {
    return (SimpleNode)  this.node;
  }
  
  @Nonnull
  public void setNode(Node node) {
    if (!(node instanceof SimpleNode)) return;
    this.node = (SimpleNode) node;
  }

  @Nonnull
  @Override
  public String getMessage() {
    try {
      StringBuilder sb = new StringBuilder(160);
      
      SimpleNode theNode = getSimpleNode();
      SimpleNode context = theNode;
      if (theNode != null) {      
        if (theNode instanceof BSHMethodDeclaration) {
          context = (SimpleNode) theNode.jjtGetParent();
          theNode = (SimpleNode)
            ((BSHMethodDeclaration)theNode).jjtGetChild(1);
        } else {
          if ( context != null 
          &&   context.jjtGetParent() != null 
          && !(context.jjtGetParent() instanceof BSHMethodDeclaration)) {
            context = (SimpleNode) context.jjtGetParent();
          }
        } 
        if ( context != null 
        &&   context.jjtGetParent() != null 
        && !(context.jjtGetParent() instanceof BSHMethodDeclaration)) {
          context = (SimpleNode) context.jjtGetParent();
        }
        Token firstToken = theNode.firstToken;
        Token lastToken  = theNode.lastToken;
        int lineStart = firstToken != null? firstToken.beginLine: 0;
        int lineEnd = lastToken != null? lastToken.endLine: 0;
        int colStart = firstToken != null? firstToken.beginColumn: 0;
        int colEnd = lastToken != null? lastToken.endColumn: 0;
        String file = theNode != null? theNode.sourceFile: "<unknown>";
        String tnStr;
        sb.append(String.format(
          ": \n  at Lines: %d-%d, Columns: %d-%d\n  in File:  %s\n\n"
          + "    %s\n    %s\n\n", 
          lineStart, lineEnd, colStart, colEnd,
          file != null? file: "<stdin>",
          (tnStr = theNode.toString()),
          (context != null && context != theNode)
            ? context.toString().replace(
              tnStr, "\u001b[1;31m".concat(tnStr).concat("\u001b[0m")
            )
            : ""
        ));
      }
      
      if (this.callstack != null) {
        sb.append("\n").append(this.callstack);
      } else if (this.getData().get("callstack") != null) {
        sb.append("\n").append(this.getData().get("callstack"));
      }
      /*
      if (this.fields != null) {
        Field[] var6 = this.fields;
        int var5 = this.fields.length;
  
        for(int var4 = 0; var4 < var5; ++var4) {
          Field field = var6[var4];
  
          try {
            field.setAccessible(true);
            Object o = field.get(this);
            if (o != null) {
              sb.append(String.format(
                "  %s: %s\n", 
                field.getName(), 
                (!(o instanceof BshBinding || o instanceof Map))
                  ? Debug.ToString(o)
                  : o.getClass().getSimpleName()
                    + "@" +Integer.toHexString(System.identityHashCode(o))
              ));
            }
          } catch (Throwable var8) {
            System.err.println(var8);
          }
        }
      }
      */
      
      if (this.callstack != null && callstack.depth() > 1) {
        sb.append('\n');
        sb.append(this.getScriptStackTrace());
      }
      
      return sb.insert(0, this.getRawMessage()).toString();
    } catch (Throwable _fatalMsg) {
      new Error(
        "EvalError.getMessage() crashed!", _fatalMsg
      ).printStackTrace();
      return super.getMessage();
    }
  }

  public void reThrow(String msg) throws EvalError {
    this.prependMessage(msg);
    throw this;
  }

  @Nonnull
  public String getErrorText() {
    SimpleNode sn = this.getSimpleNode();
    return sn != null?sn.getText():"";
  }

  public int getErrorLineNumber() {
    SimpleNode sn = this.getSimpleNode();
    return sn != null?sn.getLineNumber():0;
  }

  @Nonnull
  public String getErrorSourceFile() {
    SimpleNode sn = this.getSimpleNode();
    return sn != null? sn.getSourceFile(): "<stdin>";
  }

  public String getScriptStackTrace() {
    if (this.callstack == null) {
      return "<Unknown>";
    } else {
      String trace = "";
      CallStack stack = this.callstack.copy();
      StringBuilder sb = new StringBuilder(76);

      for(Iterator var5 = this.callstack.iterator(); var5.hasNext(); sb.append('\n')) {
        BshBinding ns = (BshBinding)var5.next();
        SimpleNode node = ns.getNode();
        if (ns.isMethod()) {
          sb.append(String.format("\t at %s", new Object[]{ns.getName()}));
        }

        if (node != null) {
          sb.append(String.format("(%s:%d)\n\t  %s", new Object[]{node.getSourceFile(), Integer.valueOf(node.getLineNumber()), node.getText()}));
        }
      }

      return trace;
    }
  }

  public String getRawMessage() {
    return org.d6r.Reflect.getfldval(this, "detailMessage");
  }

  private void prependMessage(String s) {
    if (s != null) {
      if (this.message == null) {
        this.message = s;
      } else {
        this.message = s + " : " + this.message;
      }
    }
  }

  public Map<String, Object> getData() {
    Object ss = org.d6r.Reflect.get(this, "stackState");
    Object map;
    if (ss instanceof Map) {
      map = (Map)ss;
    } else {
      if (ss instanceof int[]) {
        this.getStackTrace();
      }

      map = new RealArrayMap(4);
      org.d6r.Reflect.setfldval(this, "stackState", map);
    }

    return (Map)map;
  }

  public void clear() {
    this.getData().clear();
  }

  public boolean containsKey(Object key) {
    return this.getData().containsKey(key);
  }

  public boolean containsValue(Object value) {
    return this.getData().containsValue(value);
  }

  public Set<Entry<String, Object>> entrySet() {
    return this.getData().entrySet();
  }

  public Object get(Object key) {
    return this.getData().get(key);
  }

  public boolean isEmpty() {
    return this.getData().isEmpty();
  }

  public Set<String> keySet() {
    return this.getData().keySet();
  }

  public Object put(String key, Object value) {
    return this.getData().put(key, value);
  }

  public void putAll(Map<? extends String, ? extends Object> inMap) {
    this.getData().putAll(inMap);
  }

  public Object remove(Object key) {
    return this.getData().remove(key);
  }

  public int size() {
    return this.getData().size();
  }

  public Collection<Object> values() {
    return this.getData().values();
  }

  public Iterator<Entry<String, Object>> iterator() {
    return this.entrySet().iterator();
  }
}