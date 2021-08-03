package bsh;

import bsh.BSHBlock;
import bsh.BSHFormalParameters;
import bsh.BSHReturnType;
import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.Modifiers;

public class DelayedEvalBshMethod extends BshMethod {
  String returnTypeDescriptor;
  BSHReturnType returnTypeNode;
  String[] paramTypeDescriptors;
  BSHFormalParameters paramTypesNode;
  transient CallStack callstack;
  transient Interpreter interpreter;

  DelayedEvalBshMethod(String name, String returnTypeDescriptor, BSHReturnType returnTypeNode, String[] paramNames, String[] paramTypeDescriptors, BSHFormalParameters paramTypesNode, BSHBlock methodBody, BshBinding declaringNameSpace, Modifiers modifiers, CallStack callstack, Interpreter interpreter) {
    super(name, (Class)null, paramNames, (Class[])null, (BSHBlock)methodBody, declaringNameSpace, modifiers);
    this.returnTypeDescriptor = returnTypeDescriptor;
    this.returnTypeNode = returnTypeNode;
    this.paramTypeDescriptors = paramTypeDescriptors;
    this.paramTypesNode = paramTypesNode;
    this.callstack = callstack;
    this.interpreter = interpreter;
  }

  public String getReturnTypeDescriptor() {
    return this.returnTypeDescriptor;
  }

  public Class getReturnType() {
    if(this.returnTypeNode == null) {
      return null;
    } else {
      try {
        return this.returnTypeNode.evalReturnType(this.callstack, this.interpreter);
      } catch (EvalError var2) {
        throw new InterpreterError("can\'t eval return type: " + var2);
      }
    }
  }

  public String[] getParamTypeDescriptors() {
    return this.paramTypeDescriptors;
  }

  public Class[] getParameterTypes() {
    try {
      return (Class[])this.paramTypesNode.eval(this.callstack, this.interpreter);
    } catch (EvalError var2) {
      throw new InterpreterError("can\'t eval param types: " + var2);
    }
  }
}
