package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHPrimarySuffix;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.LHS;
import bsh.Node;
import bsh.SimpleNode;
import org.d6r.Debug;
import org.d6r.Reflector.Util;

public class BSHPrimaryExpression<B> extends SimpleNode<EvalError> {
  public B base;
  public BSHPrimarySuffix<?>[] suffixes;
  boolean initialized;
  String str;

  BSHPrimaryExpression(int id) {
    super(id);
  }

  void ensureInit() {
    if(!this.initialized) {
      this.initialized = true;
      int idx = -1;
      int numChildren = this.jjtGetNumChildren();
      if(numChildren == 0) {
        this.base = (B) (Object) "NUM CHILDREN == ZERO!";
        this.suffixes = new BSHPrimarySuffix[0];
      } else {
        this.suffixes = new BSHPrimarySuffix[numChildren - 1];

        while(true) {
          ++idx;
          if(idx >= numChildren) {
            return;
          }

          if(idx == 0) {
            this.base = this.jjtGetChild(0);
          } else {
            this.suffixes[idx - 1] = (BSHPrimarySuffix)this.jjtGetChild(idx);
          }
        }
      }
    }
  }

  public String toString() {
    return this.getText();
  }

  public Object eval(CallStack cs, Interpreter interp) throws EvalError {
    this.ensureInit();
    super.touch(cs);
    return this.eval(false, cs, interp);
  }

  public LHS toLHS(CallStack callstack, Interpreter interpreter) throws EvalError {
    this.ensureInit();
    Object obj = this.eval(true, callstack, interpreter);
    if(!(obj instanceof LHS)) {
      throw new EvalError("Can\'t assign to:", this, callstack);
    } else {
      return (LHS)obj;
    }
  }

  public static Node find(Node node, Object obj) {
    if(node == obj) {
      return node;
    } else if(node == null) {
      return null;
    } else {
      int len = node.jjtGetNumChildren();

      for(int i = 0; i < len; ++i) {
        Node child = node.jjtGetChild(i);
        if(child == obj) {
          return child;
        }

        Node fromChild = find(child, obj);
        if(fromChild != null) {
          return fromChild;
        }
      }

      return null;
    }
  }

  public <T> T eval(boolean toLHS, CallStack cs, Interpreter interp) throws EvalError {
    this.ensureInit();
    Object obj = this.jjtGetChild(0);
    int numChildren = this.jjtGetNumChildren();

    for(int e = 1; e < numChildren; ++e) {
      obj = ((BSHPrimarySuffix)this.jjtGetChild(e)).doSuffix(obj, toLHS, cs, interp);
      if(Interpreter.DEBUG) {
        System.err.printf("obj = %s\n", new Object[]{Debug.ToString(obj)});
      }
    }

    if(obj instanceof SimpleNode) {
      if(obj instanceof BSHAmbiguousName) {
        BSHAmbiguousName var8 = (BSHAmbiguousName)obj;
        obj = toLHS?var8.toLHS(cs, interp):var8.toObject(cs, interp);
      } else {
        if(toLHS) {
          throw new EvalError(String.format("Can\'t assign to prefix:\n      %s\n at %s", new Object[]{obj, cs}), this, cs);
        }

        if(Interpreter.DEBUG) {
          System.err.printf("toLHS = %s\n", new Object[]{Boolean.valueOf(toLHS)});
          System.err.printf("cs = %s\n", new Object[]{Debug.ToString(cs)});
          System.err.printf("interp = %s\n", new Object[]{interp});
          System.err.printf("this = %s\n", new Object[]{Debug.ToString(this)});
          System.err.printf("obj = %s\n", new Object[]{Debug.ToString(obj)});
          System.err.printf("jjt = %s\n", new Object[]{Debug.ToString(interp.get_jjtree())});
          System.err.printf("jjtps = %s\n", new Object[]{Debug.ToString(org.d6r.Reflect.getfldval(org.d6r.Reflect.getfldval(interp, "parser"), "jjtree"))});
        }

        try {
          obj = ((SimpleNode)obj).eval(cs, interp);
        } catch (Throwable var7) {
          throw Util.sneakyThrow(var7);
        }
      }
    }

    if(toLHS) {
      if(obj instanceof LHS) {
  return (T) (Object) obj;
      } else {
  return (T) (Object) ((LHS)obj).getValue();
      }
    } else {
  return (T) (Object) obj;
    }
  }
}