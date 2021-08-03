package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHPrimitiveType;
import bsh.BshClassManager;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.Name;
import bsh.SimpleNode;
import java.lang.reflect.Array;

class BSHType extends SimpleNode implements BshClassManager.Listener {
  private Class baseType;
  private int arrayDims;
  private Class type;
  String descriptor;

  BSHType(int id) {
    super(id);
  }

  public void addArrayDimension() {
    ++this.arrayDims;
  }

  SimpleNode getTypeNode() {
    return (SimpleNode)this.jjtGetChild(0);
  }

  public String toString() {
    return this.getText().trim();
  }

  public String getTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    if(this.descriptor != null) {
      return this.descriptor;
    } else {
      SimpleNode node = this.getTypeNode();
      String descriptor;
      if(node instanceof BSHPrimitiveType) {
        descriptor = getTypeDescriptor(((BSHPrimitiveType)node).type);
      } else {
        String i = ((BSHAmbiguousName)node).text;
        BshClassManager bcm = interpreter.getClassManager();
        String definingClass = bcm.getClassBeingDefined(i);
        Class clas = null;
        if(definingClass == null) {
          try {
            clas = ((BSHAmbiguousName)node).toClass(callstack, interpreter);
          } catch (EvalError var11) {
            ;
          }
        } else {
          i = definingClass;
        }

        if(clas != null) {
          descriptor = getTypeDescriptor(clas);
        } else if(defaultPackage != null && !Name.isCompound(i)) {
          descriptor = "L" + defaultPackage.replace('.', '/') + "/" + i + ";";
        } else {
          descriptor = "L" + i.replace('.', '/') + ";";
        }
      }

      for(int var12 = 0; var12 < this.arrayDims; ++var12) {
        descriptor = "[" + descriptor;
      }

      this.descriptor = descriptor;
      return descriptor;
    }
  }

  public Class getType(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.type != null) {
      return this.type;
    } else {
      SimpleNode node = this.getTypeNode();
      if(node instanceof BSHPrimitiveType) {
        this.baseType = ((BSHPrimitiveType)node).getType();
      } else {
        this.baseType = ((BSHAmbiguousName)node).toClass(callstack, interpreter);
      }

      if(this.arrayDims > 0) {
        try {
          int[] e = new int[this.arrayDims];
          Object obj = Array.newInstance(this.baseType, e);
          this.type = obj.getClass();
        } catch (Exception var6) {
          throw new EvalError("Couldn\'t construct array type", this, callstack);
        }
      } else {
        this.type = this.baseType;
      }

      interpreter.getClassManager().addListener(this);
      return this.type;
    }
  }

  public Class getBaseType() {
    return this.baseType;
  }

  public int getArrayDims() {
    return this.arrayDims;
  }

  public void classLoaderChanged() {
    this.type = null;
    this.baseType = null;
  }

  public static String getTypeDescriptor(Class clas) {
    if(clas == Boolean.TYPE) {
      return "Z";
    } else if(clas == Character.TYPE) {
      return "C";
    } else if(clas == Byte.TYPE) {
      return "B";
    } else if(clas == Short.TYPE) {
      return "S";
    } else if(clas == Integer.TYPE) {
      return "I";
    } else if(clas == Long.TYPE) {
      return "J";
    } else if(clas == Float.TYPE) {
      return "F";
    } else if(clas == Double.TYPE) {
      return "D";
    } else if(clas == Void.TYPE) {
      return "V";
    } else {
      String name = clas.getName().replace('.', '/');
      return !name.startsWith("[") && !name.endsWith(";")?"L" + name.replace('.', '/') + ";":name;
    }
  }
}
