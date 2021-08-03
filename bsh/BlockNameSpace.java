package bsh;

import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.BshMethod;
import bsh.Factory;
import bsh.InstanceId;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.This;
import bsh.UtilEvalError;
import org.d6r.Debug;

public class BlockNameSpace extends NameSpace {
  public BlockNameSpace(BshClassManager cm, BshBinding parent, String name) {
    super(cm, parent, Factory.makeName(parent, name, BlockNameSpace.class));
  }

  public BlockNameSpace(BshBinding parent, BshClassManager cm, String name) {
    this(cm, parent, name);
  }

  public BlockNameSpace(BshClassManager cm, BshBinding parent) {
    this((BshClassManager)cm, (BshBinding)parent, (String)null);
  }

  public BlockNameSpace(BshBinding parent, BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)parent, (String)null);
  }

  public BlockNameSpace(BshClassManager cm, String name) {
    this((BshClassManager)cm, (BshBinding)null, (String)name);
  }

  public BlockNameSpace(String name, BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)null, (String)name);
  }

  public BlockNameSpace(BshBinding parent, String name) {
    this((BshClassManager)null, (BshBinding)parent, (String)name);
  }

  public BlockNameSpace(String name, BshBinding parent) {
    this((BshClassManager)null, (BshBinding)parent, (String)name);
  }

  public BlockNameSpace(BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)null, (String)null);
  }

  public BlockNameSpace(BshBinding parent) {
    this((BshClassManager)null, (BshBinding)parent, (String)null);
  }

  public BlockNameSpace(String name) {
    this((BshClassManager)null, (BshBinding)null, (String)name);
  }

  public BlockNameSpace(BshBinding parent, String name, InstanceId id) {
    this((BshClassManager)null, parent, name, id);
    if(Interpreter.DEBUG) {
      System.err.printf("BlockNameSpace(BshBinding parent, String name, InstanceId id = %s\n", new Object[]{Debug.ToString(id)});
    }

  }

  public BlockNameSpace(BshClassManager cm, BshBinding parent, String name, InstanceId id) {
    super(cm, parent, name, id);
    if(Interpreter.DEBUG) {
      System.err.printf("BlockNameSpace(BshClassManager cm, BshBinding parent, String name, InstanceId id = %s\n", new Object[]{Debug.ToString(id)});
    }

  }

  public void setVariable(String name, Object value, boolean strictJava, boolean recurse) throws UtilEvalError {
    if(this.weHaveVar(name)) {
      super.setVariable(name, value, strictJava, false);
    } else {
      this.getParent().setVariable(name, value, strictJava, recurse);
    }

  }

  public void setBlockVariable(String name, Object value) throws UtilEvalError {
    super.setVariable(name, value, false, false);
  }

  private boolean weHaveVar(String name) {
    try {
      return super.getVariableImpl(name, false) != null;
    } catch (UtilEvalError var3) {
      return false;
    }
  }

  private NameSpace getNonBlockParent() {
    BshBinding parent = super.getParent();
    return parent instanceof BlockNameSpace?((BlockNameSpace)parent).getNonBlockParent():(NameSpace)parent;
  }

  public This getThis(Interpreter declaringInterpreter) {
    return this.thisReference != null?this.thisReference:(this.id == null?this.getNonBlockParent().getThis(declaringInterpreter):This.getThis(this, declaringInterpreter));
  }

  public This getSuper(Interpreter declaringInterpreter) {
    return this.getNonBlockParent().getSuper(declaringInterpreter);
  }

  public void importClass(String name) {
    if(this.parent == null) {
      if(Interpreter.DEBUG) {
        System.err.printf("BlockNameSpace{%s}.importClass(String name=\"%s\"): parent == null", new Object[]{this.toString(), name});
      }

    } else {
      try {
        this.getParent().importClass(name);
      } catch (UtilEvalError var3) {
        if(Interpreter.DEBUG) {
          var3.printStackTrace();
        }
      }

    }
  }

  public void importPackage(String name) {
    this.getParent().importPackage(name);
  }
  
  @Override
  public void setMethod(BshMethod method) throws UtilEvalError {
    if (Interpreter.DEBUG) {
      EvalError ee = new EvalError(
        String.format(
          "BlockNameSpace.setMethod(bsh.BshMethod method = %s) called",
          method
        ), 
        CallStack.getActiveCallStack(), method.decl
      );
      ee.getData().put("BlockNameSpace", this);
      ee.getData().put("method", method);
      ee.getData().put("id", id);
      ee.printStackTrace();
    }
    ((NameSpace) getParent()).setMethod0(method);
  }
  
  @Override
  public void setMethod0(BshMethod method) throws UtilEvalError {
    super.setMethod0(method);
  }
  
}



