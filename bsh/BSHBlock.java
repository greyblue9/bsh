package bsh;

import bsh.BSHClassDeclaration;
import bsh.BSHVariableDeclarator;
import bsh.BlockNameSpace;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.d6r.Debug;

public class BSHBlock extends SimpleNode {
  
  public boolean isSynchronized = false;
  
  public static List<Object> bvdResults = new LinkedList<Object>();
  public static List<BSHBlock> blocks = new LinkedList<>();  
  
  BSHBlock(int id) {
    super(id);
    blocks.add(this);
  }
  
  public String toString() {
    int numChildren = this.jjtGetNumChildren();
    int startChild = this.isSynchronized ?1 :0;
    int numBlocks = numChildren - startChild;
    
    StringBuilder sb = new StringBuilder(76);
    if (isSynchronized) sb.append("synchronized ");
    sb.append("{\n");

    for (int i = startChild; i < numChildren; ++i) {
      SimpleNode node = (SimpleNode) jjtGetChild(i);
      sb.append("  ").append(node).append(";\n");
    }
    return sb.append("} ").toString();    
  }
  
  public Object eval(CallStack callstack, Interpreter interpreter)
    throws EvalError 
  {
    super.touch(callstack);
    return this.eval(callstack, interpreter, false);
  }
  
  public Object eval(CallStack callstack, Interpreter interpreter, boolean overrideNamespace) throws EvalError {
    
    if(Interpreter.DEBUG && overrideNamespace) {
      System.err.println("OVERRIDING NAMESPACE");
    }

    Object syncValue = null;
    if (this.isSynchronized) {
      SimpleNode ret = (SimpleNode)this.jjtGetChild(0);
      syncValue = ret.eval(callstack, interpreter);
    }

    Object ret1;
    if(this.isSynchronized) {
      synchronized(syncValue) {
        ret1 = this.evalBlock(callstack, interpreter, overrideNamespace, (BSHBlock.NodeFilter)null);
      }
    } else {
      ret1 = this.evalBlock(callstack, interpreter, overrideNamespace, (BSHBlock.NodeFilter)null);
    }

    return ret1;
  }

  public Object evalBlock(CallStack callstack, Interpreter interpreter, boolean overrideNamespace, BSHBlock.NodeFilter nodeFilter) throws EvalError {
    Object ret = Primitive.VOID;
    BshBinding enclosingNameSpace = null;
    BlockNameSpace bodyNameSpace = null;
    if(Interpreter.TRACE) {
      System.err.printf("Object evalBlock(CallStack callstack = %s, Interpreter interpreter = %s, boolean overrideNamespace = %s, NodeFilter nodeFilter = %s)\n  stack = %s\n", new Object[]{callstack != null?callstack.toString():"null", Debug.ToString(interpreter), Boolean.valueOf(overrideNamespace), Debug.ToString(nodeFilter), Arrays.toString(Thread.currentThread().getStackTrace()).replaceAll("^.*(CallStack\\.swap)", "  $1").replace(", ", "\n  ").replace("]", "")});
    }

    enclosingNameSpace = callstack.top();
    if(Interpreter.DEBUG) {
      System.err.printf("enclosingNameSpace = callstack.top()\nenclosingNameSpace := %s\n", new Object[]{enclosingNameSpace});
    }
    
    
    bodyNameSpace
      = BlockNameSpaceFactory.get().getReusableWithParent(
          enclosingNameSpace
        );
    
    if (Interpreter.DEBUG) System.err.printf(
      "BSHBlock: new bodyNameSpace created: %s\n", 
      bodyNameSpace    
    );

    callstack.swap(bodyNameSpace);
    if(Interpreter.DEBUG) {
      System.err.printf("callstack: %s\n", new Object[]{callstack});
    }

    int startChild = this.isSynchronized ?1 :0;
    int numChildren = this.jjtGetNumChildren();

    try {
      int i;
      SimpleNode node;
      for(i = startChild; i < numChildren; ++i) {
        
        node = (SimpleNode)this.jjtGetChild(i);
        
        if(Interpreter.DEBUG) {
          System.err.printf("scanning child (%s) %d of %d: [%s]\n", new Object[]{node.getClass().getSimpleName(), Integer.valueOf(i - startChild + 1), Integer.valueOf(numChildren - startChild), node});
        }

        if(nodeFilter != null && !nodeFilter.isVisible(node)) {
          if(Interpreter.DEBUG) {
            System.err.println("Skipping due to nodeFilter");
          }
          continue;
        }
        
        if(node instanceof BSHClassDeclaration) {
          if(Interpreter.DEBUG) {
            System.err.println("Calling node.eval() on BSHClassDeclaration");
          }

          node.eval(callstack, interpreter);
        }
        
        if(node instanceof BSHVariableDeclarator) {
          if(Interpreter.DEBUG) {
            System.err.println("Calling node.eval() on BSHVariableDeclarator");
          }

          Object result = ((BSHVariableDeclarator) node).eval(
            callstack, interpreter
          );
          
          if(Interpreter.DEBUG) {
            System.err.printf("Result: <%s> %s\n", new Object[]{result, Debug.ToString(result)});
          }
        }
      }

      for(i = startChild; i < numChildren; ++i) {
        node = (SimpleNode)this.jjtGetChild(i);
        if(Interpreter.DEBUG) {
          System.err.printf("scanning child (%s) %d of %d: [%s]\n", new Object[]{node.getClass().getSimpleName(), Integer.valueOf(i - startChild + 1), Integer.valueOf(numChildren - startChild), node});
        }

        if(!(node instanceof BSHClassDeclaration) && (nodeFilter == null || nodeFilter.isVisible(node))) {
          ret = node.eval(callstack, interpreter);
          if(ret instanceof ReturnControl) {
            return ret;
          }
        }
      }

      return ret;
    } finally {
      if(!overrideNamespace) {
        callstack.swap(enclosingNameSpace);
      }

    }
  }

  public interface NodeFilter {
    boolean isVisible(SimpleNode var1);
  }
}
