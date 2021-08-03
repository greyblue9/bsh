package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHBlock;
import bsh.BSHFormalParameters;
import bsh.BSHReturnType;
import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.Node;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import java.util.Arrays;
import java.util.List;


public class BSHMethodDeclaration extends SimpleNode {
  
  public String name;
  BSHReturnType returnTypeNode;
  BSHFormalParameters paramsNode;
  BSHBlock blockNode;
  int firstThrowsClause;
  public Modifiers modifiers;
  Class<?> returnType;
  int numThrows = 0;

  BSHMethodDeclaration(int id) {
    super(id);
  }

  synchronized void insureNodesParsed() {
    if(this.paramsNode == null) {
      Node firstNode = this.jjtGetChild(0);
      this.firstThrowsClause = 1;
      if(firstNode instanceof BSHReturnType) {
        this.returnTypeNode = (BSHReturnType)firstNode;
        this.paramsNode = (BSHFormalParameters)this.jjtGetChild(1);
        if(this.jjtGetNumChildren() > 2 + this.numThrows) {
          this.blockNode = (BSHBlock)this.jjtGetChild(2 + this.numThrows);
        }

        ++this.firstThrowsClause;
      } else {
        this.paramsNode = (BSHFormalParameters)this.jjtGetChild(0);
        this.blockNode = (BSHBlock)this.jjtGetChild(1 + this.numThrows);
      }

    }
  }

  Class<?> evalReturnType(CallStack callstack, Interpreter interpreter) throws EvalError {
    this.insureNodesParsed();
    return this.returnTypeNode == null?null:this.returnTypeNode.evalReturnType(callstack, interpreter);
  }

  String getReturnTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    this.insureNodesParsed();
    return this.returnTypeNode == null?null:this.returnTypeNode.getTypeDescriptor(callstack, interpreter, defaultPackage);
  }

  public BSHReturnType getReturnTypeNode() {
    this.insureNodesParsed();
    return this.returnTypeNode;
  }

  public Object eval(CallStack callstack, Interpreter interpreter)
    throws EvalError 
  {
    super.touch(callstack);
    this.returnType = this.evalReturnType(callstack, interpreter);
    this.evalNodes(callstack, interpreter);
    BshBinding namespace = callstack.top();
    BshMethod bshMethod = new BshMethod(this, namespace, this.modifiers);
    
    if (Interpreter.DEBUG) {
      System.err.printf(
        "eval called on %s;\n"
        + "  - callstack = %s\n"
        + "  - namespace = %s\n"
        + "  - bshMethod = %s\n",
        this, callstack, namespace, bshMethod
      );
    }
    
    try {
      namespace.setMethod(bshMethod);
      return bshMethod;
    } catch (UtilEvalError var6) {
      var6.printStackTrace();
      throw var6.toEvalError(this, callstack);
    }
  }
  
  private void evalNodes(CallStack callstack, Interpreter interpreter) 
    throws EvalError 
  {
    this.insureNodesParsed();

    int i;
    for(i = this.firstThrowsClause; i < this.numThrows + this.firstThrowsClause; ++i) {
      ((BSHAmbiguousName)this.jjtGetChild(i)).toClass(callstack, interpreter);
    }

    this.paramsNode.eval(callstack, interpreter);
    if(interpreter.getStrictJava()) {
      for(i = 0; i < this.paramsNode.paramTypes.length; ++i) {
        if(this.paramsNode.paramTypes[i] == null) {
          throw new EvalError("[StrictMode] Undeclared argument type, parameter: " + this.paramsNode.getParamNames()[i] + " in method: " + this.name, this, callstack, (Throwable)null, new Object[]{this.paramsNode, this.name});
        }
      }

      if(this.returnType == null) {
        throw new EvalError("[StrictMode] Undeclared return type for method: " + this.name, this, callstack, (Throwable)null, new Object[]{this.paramsNode, this.name});
      }
    }

  }
  
  
  
  public BSHFormalParameters getParameters() {
    return (BSHFormalParameters) findChild(BSHFormalParameters.class);
  }
  
  public BSHBlock getBody() {
    return (BSHBlock) findChild(BSHBlock.class);
  }
  
  public List<SimpleNode> getStatements() {
    BSHBlock body = getBody();
    int stmtCount = body.jjtGetNumChildren();
    
    SimpleNode[] stmts = new SimpleNode[stmtCount];
    for (int i=0; i<stmtCount; ++i) {
      stmts[i] = (SimpleNode) body.jjtGetChild(i);
    }
    return Arrays.asList(stmts);
  }
  
  public String toString() {
    this.insureNodesParsed();
    return String.format(
      "[MethodDeclaration: %s] %s %s(%s) {\n  %s\n}",
      name,
      getReturnTypeNode(),
      name,
      getParameters(),
      getBody()
    );
  }
}


