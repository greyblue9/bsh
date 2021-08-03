package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.SimpleNode;

class BSHArguments extends SimpleNode {
  BSHArguments(int id) {
    super(id);
  }

  public Object[] getArguments(CallStack callstack, Interpreter interpreter) throws EvalError {
    super.touch(callstack);
    Object[] args = new Object[this.jjtGetNumChildren()];
    int i;
    if(this.evalled) {
      for(i = 0; i < args.length; ++i) {
        args[i] = (SimpleNode)this.jjtGetChild(i);
      }

      return args;
    } else {
      for(i = 0; i < args.length; ++i) {
        args[i] = ((SimpleNode)this.jjtGetChild(i)).eval(callstack, interpreter);
        if(args[i] == Primitive.VOID) {
          throw new EvalError("Undefined argument: " + ((SimpleNode)this.jjtGetChild(i)).getText(), this, callstack);
        }
      }

      this.evalResult = args;
      return args;
    }
  }

  public String[] getArgumentText() {
    String[] args = new String[this.jjtGetNumChildren()];

    for(int i = 0; i < args.length; ++i) {
      args[i] = ((SimpleNode)this.jjtGetChild(i)).getText();
    }

    return args;
  }
}