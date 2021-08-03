package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.KindNode;
import bsh.Node;
import bsh.ParserConstants;
import bsh.Token;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleNode<X extends EvalError> implements Node, Cloneable {
  public static SimpleNode JAVACODE = new SimpleNode(1) {
    public String getSourceFile() {
      return "/dev/null";
    }

    public int getLineNumber() {
      return 1;
    }

    public String getText() {
      return "<Compiled Java Code>";
    }
  };
  public static final SimpleNode DEFAULT = new SimpleNode(0) {
    public String getSourceFile() {
      return "";
    }

    public int getLineNumber() {
      return 1;
    }

    public String getText() {
      return "";
    }
  };
  public boolean evalled = false;
  public Object evalResult = null;
  int kind = 0;
  static int[] codes;
  protected SimpleNode parent;
  protected SimpleNode[] children = new SimpleNode[0];
  protected int id;
  Token firstToken;
  Token lastToken;
  String sourceFile;

  static {
    String[] images = ParserConstants.tokenImage;
    codes = new int[images.length];

    for(int i = 0; i < images.length; ++i) {
      int len = images[i].length();
      codes[i] = images[i].substring(1, len - 1).hashCode();
    }

    Arrays.sort(codes);
  }

  public SimpleNode clone() {
    try {
      return (SimpleNode)super.clone();
    } catch (Throwable var2) {
      if(Interpreter.DEBUG) {
        var2.printStackTrace();
      }

      return null;
    }
  }

  public int getKind() {
    return this instanceof KindNode?this.kind:0;
  }

  public void setKind(int kind) {
    this.kind = kind;
  }

  static boolean needSpaceAfter(String tokenImage) {
    if(tokenImage.length() == 1) {
      return tokenImage.equals("=")?true:(tokenImage.equals(",")?true:(tokenImage.equals(";")?true:(tokenImage.equals("{")?true:tokenImage.equals("}"))));
    } else {
      int hashCode = tokenImage.hashCode();
      int idx = Arrays.binarySearch(codes, hashCode);
      idx = idx >= 0?idx:-1;
      boolean isSpecial = idx != -1;
      return isSpecial;
    }
  }

  static boolean needSpaceBefore(String tokenImage) {
    if(tokenImage.length() == 1) {
      return tokenImage.equals("=")?true:(tokenImage.equals("{")?true:tokenImage.equals("}"));
    } else {
      int hashCode = tokenImage.hashCode();
      int idx = Arrays.binarySearch(codes, hashCode);
      idx = idx >= 0?idx:-1;
      boolean isSpecial = idx != -1;
      return isSpecial;
    }
  }

  public SimpleNode(int i) {
    this.id = i;
  }

  public void jjtOpen() {
  }

  public void jjtClose() {
  }

  public void jjtSetParent(Node n) {
    this.parent = (SimpleNode)n;
  }

  public <N extends Node> N jjtGetParent() {
  return (N) (Object) this.parent;
  }

  public void jjtAddChild(Node n, int i) {
    if(this.children == null) {
      this.children = new SimpleNode[i + 1];
    } else if(i >= this.children.length) {
      SimpleNode[] c = new SimpleNode[i + 1];
      System.arraycopy(this.children, 0, c, 0, this.children.length);
      this.children = c;
    }

    this.children[i] = (SimpleNode)n;
  }

  public <N extends Node> N jjtGetChild(int i) {
  return (N) (Object) this.children[i];
  }

  public <S extends SimpleNode> S getChild(int i) {
  return (S) (Object) (SimpleNode)this.jjtGetChild(i);
  }

  public int jjtGetNumChildren() {
    return this.children == null?0:this.children.length;
  }

  public String toString() {
    return this.toString((Set)null);
  }

  public String toString(Set<Token> vts) {
    StringBuilder sb = new StringBuilder(760);
    if(vts == null) {
      vts = new HashSet();
    }

    if(this.firstToken != null && !((Set)vts).contains(this.firstToken)) {
      ((Set)vts).add(this.firstToken);
      sb.append(this.firstToken.image);
    }

    int i = -1;
    SimpleNode[] var7 = this.children;
    int var6 = this.children.length;

    for(int var5 = 0; var5 < var6; ++var5) {
      SimpleNode ch = var7[var5];
      ++i;
      if(ch != null) {
        try {
          Method e = ch.getClass().getDeclaredMethod("getText", new Class[0]);
          e.setAccessible(true);
          sb.append(e.invoke(ch, new Object[0]));
        } catch (Throwable var11) {
          try {
            Method e2 = ch.getClass().getDeclaredMethod("toString", new Class[0]);
            e2.setAccessible(true);
            sb.append(e2.invoke(ch, new Object[0]));
          } catch (Throwable var10) {
            sb.append(ch.toString((Set)vts));
          }
        }
      }
    }

    if(this.lastToken != null && !((Set)vts).contains(this.lastToken)) {
      ((Set)vts).add(this.lastToken);
      sb.append(this.lastToken.image);
    }

    return sb.toString();
  }

  public String toString(String prefix) {
    return prefix + this.toString();
  }

  public void dump(String prefix) {
    System.out.println(this.toString(prefix));
    if(this.children != null) {
      for(int i = 0; i < this.children.length; ++i) {
        SimpleNode n = this.children[i];
        if(n != null) {
          n.dump(prefix + " ");
        }
      }
    }

  }

  public void prune() {
    this.jjtSetParent((Node)null);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws X {
    throw new AbstractMethodError();
  }
  
  public void touch(CallStack callstack) {
    BshBinding top = callstack.top();
    if (top instanceof NameSpace
    && ((NameSpace)top).callerInfoNode == null) {
      ((NameSpace)top).callerInfoNode = this;
    }
  }
  
  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }
  
  public String getSourceFile() {
    return this.sourceFile == null?(this.parent != null?this.parent.getSourceFile():"<unknown file>"):this.sourceFile;
  }

  public int getLineNumber() {
    int lineno = this.firstToken != null?this.firstToken.beginLine:1;
    return lineno > 0?lineno:1;
  }

  public String getText() {
    StringBuilder text = new StringBuilder(96);

    for(Token t = this.firstToken; t != null; t = t.next) {
      if(needSpaceBefore(t.image)) {
        text.append(" ");
      }

      text.append(t.image);
      if(needSpaceAfter(t.image)) {
        text.append(" ");
      }

      if(t == this.lastToken || t.image.equals("{") || t.image.equals(";")) {
        break;
      }
    }

    return text.toString();
  }

  public <S extends SimpleNode> S findChild(Class<S> cls) {
    int i = -1;
    int len = this.jjtGetNumChildren();

    SimpleNode node;
    do {
      ++i;
      if(i >= len) {
        return null;
      }
    } while(!cls.isInstance(node = (SimpleNode)this.jjtGetChild(i)));

  return (S) (Object) (SimpleNode)node;
  }

  public <S extends SimpleNode> List<S> findChild(Node thiz, Class<?> cls, int maxdepth) {
    ArrayDeque nodes = new ArrayDeque();
    ArrayList results = new ArrayList();
    ArrayDeque depths = new ArrayDeque();
    SimpleNode node = (SimpleNode)thiz;
    SimpleNode child = null;
    nodes.push((SimpleNode)thiz);
    depths.push(Integer.valueOf(0));
    if(cls.isInstance(node)) {
      results.add(node);
    }

    while(!nodes.isEmpty()) {
      node = (SimpleNode)nodes.poll();
      int depth = ((Integer)depths.poll()).intValue();
      int len = node.jjtGetNumChildren();

      for(int i = 0; i < len; ++i) {
        child = (SimpleNode)node.jjtGetChild(i);
        if(cls.isInstance(child)) {
          results.add(child);
        }

        if(depth < maxdepth) {
          nodes.offer(child);
          depths.offer(Integer.valueOf(depth + 1));
        }
      }
    }

    return (List)results;
  }
}