package bsh;

import bsh.BSHType;
import bsh.BSHVariableDeclarator;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.Node;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import bsh.Variable;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.Debug;

public class BSHTypedVariableDeclaration extends SimpleNode {
  public Modifiers modifiers;
  public static Object exception1 = null;
  public static Object exception2 = null;
  public static BSHVariableDeclarator[] EMPTY_BVDA
    = new BSHVariableDeclarator[0];

  public BSHTypedVariableDeclaration(int id) {
    super(id);
  }

  private BSHType getTypeNode() {
    return (BSHType) this.jjtGetChild(0);
  }

  public String toString() {
    BSHType typeNode = getTypeNode();
    BSHVariableDeclarator[] decls = getDeclarators();
    StringBuilder sb = new StringBuilder(32)
      .append(typeNode.getText())
      .append(' ');
    int i=-1;
    for (BSHVariableDeclarator decl: decls) {      
      if (++i != 0) sb.append(", ");
      CharSequence varName = decl.name;
      SimpleNode[] init = decl.children;
      CharSequence initializer = (init.length != 0)
        ? init[0].getText(): "";
      sb.append(String.format(
        initializer.length() > 0
          ? "%s = %s" : "%s",
        varName, initializer
      ));
    }
    return sb.toString();
  }
  
  Class<?> evalType(CallStack callstack, Interpreter interpreter) throws EvalError {
    BSHType typeNode = this.getTypeNode();
    return typeNode.getType(callstack, interpreter);
  }

  public BSHVariableDeclarator[] getDeclarators() {
    int n = this.jjtGetNumChildren();
    byte start = 1;
    BSHVariableDeclarator[] bvda = new BSHVariableDeclarator[n - start];

    for(int i = start; i < n; ++i) {
      bvda[i - start] = (BSHVariableDeclarator)this.jjtGetChild(i);
    }

    return bvda;
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    BshBinding namespace = null;
    BSHType typeNode = null;
    Object type = null;
    BSHVariableDeclarator[] bvda = EMPTY_BVDA;
    boolean bvdaIndex = false;
    BSHVariableDeclarator dec = null;
    Object name = null;
    Object value = null;
    boolean strictJava = false;
    Pair foundVar = null;
    Class foundType = null;

    try {
      namespace = callstack.top();
      typeNode = this.getTypeNode();
      typeNode.getType(callstack, interpreter);
      bvda = this.getDeclarators();

      for(int var22 = 0; var22 < bvda.length; ++var22) {
        dec = bvda[var22];
        value = dec.eval(typeNode, callstack, interpreter);
        Object e2 = null;

        try {
          foundVar = namespace.findVariable(dec.name);
          if(foundVar != null) {
            foundType = ((Variable)foundVar.getLeft()).getType();
            if(foundType != null && value != null && value != Primitive.NULL && !foundType.isAssignableFrom(value.getClass()) && ((BshBinding)foundVar.getRight()).unset(dec.name) != null) {
              foundVar = null;
            }
          }
        } catch (Throwable var20) {
          if(Interpreter.DEBUG) {
            var20.printStackTrace(Interpreter.debug);
          }

          Interpreter.debug.println(var20);
        }

        try {
          namespace.setVariable(dec.name, value, strictJava, false);
        } catch (UtilEvalError var19) {
          exception1 = var19;
          if(Interpreter.DEBUG) {
            var19.printStackTrace();
          }

          throw var19.toEvalError(this, callstack);
        }
      }

      return value;
    } catch (EvalError var21) {
      exception2 = var21;
      if(Interpreter.DEBUG) {
        var21.printStackTrace();
      }

      if(Interpreter.DEBUG) {
        System.err.printf("\n{ \n  namespace = %s, \n  type = %s, \n  dec = %s \n}", new Object[]{Debug.ToString(namespace), Debug.ToString(type), Debug.ToString(dec)});
      }

      Variable var = null;
      Class realType = null;

      try {
        var = namespace.getVariableImpl(dec.name, true);
        if(var != null) {
          realType = var.getType();
        }
      } catch (Throwable var18) {
        if(Interpreter.DEBUG) {
          var18.printStackTrace();
        }
      }

      EvalError e3 = new EvalError(var21.getMessage(), (SimpleNode)this.jjtGetParent(), callstack, var21);
      e3.setNode((Node)(value != null?this:dec));
      throw e3;
    }
  }

  public String getTypeDescriptor(CallStack callstack, Interpreter in, String defaultPackage) {
    return this.getTypeNode().getTypeDescriptor(callstack, in, defaultPackage);
  }
}