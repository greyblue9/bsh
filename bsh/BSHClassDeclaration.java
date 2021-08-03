package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHBlock;
import bsh.CallStack;
import bsh.ClassGenerator;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.SimpleNode;

class BSHClassDeclaration extends SimpleNode {
  static final String CLASSINITNAME = "_bshClassInit";
  String name;
  Modifiers modifiers;
  int numInterfaces;
  boolean extend;
  boolean isInterface;
  private Class<?> generatedClass;

  BSHClassDeclaration(int id) {
    super(id);
  }

  public synchronized Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.generatedClass == null) {
      this.generatedClass = this.generateClass(callstack, interpreter);
    }

    return this.generatedClass;
  }

  private Class<?> generateClass(CallStack callstack, Interpreter interpreter) throws EvalError {
    int child = 0;
    Class superClass = null;
    if(this.extend) {
      BSHAmbiguousName interfaces = (BSHAmbiguousName)this.jjtGetChild(child++);
      superClass = interfaces.toClass(callstack, interpreter);
    }

    Class[] var8 = new Class[this.numInterfaces];

    for(int block = 0; block < this.numInterfaces; ++block) {
      BSHAmbiguousName node = (BSHAmbiguousName)this.jjtGetChild(child++);
      var8[block] = node.toClass(callstack, interpreter);
      if(!var8[block].isInterface()) {
        throw new EvalError("Type: " + node.text + " is not an interface!", this, callstack);
      }
    }

    BSHBlock var9;
    if(child < this.jjtGetNumChildren()) {
      var9 = (BSHBlock)this.jjtGetChild(child);
    } else {
      var9 = new BSHBlock(25);
    }

    return ClassGenerator.getClassGenerator().generateClass(this.name, this.modifiers, var8, superClass, var9, this.isInterface, callstack, interpreter);
  }

  public String toString() {
    return "ClassDeclaration: " + this.name;
  }
}
