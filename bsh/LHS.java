package bsh;

import bsh.BshBinding;
import bsh.CollectionManager;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.UtilEvalError;
import bsh.UtilTargetError;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorType;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.d6r.Reflect.JREAccess;
import static org.d6r.CollectionUtil.isJRE;

class LHS implements ParserConstants, Serializable {
  
  static final boolean JRE = isJRE();
  BshBinding nameSpace;
  boolean localVar;
  static final int VARIABLE = 0;
  static final int FIELD = 1;
  static final int PROPERTY = 2;
  static final int INDEX = 3;
  static final int METHOD_EVAL = 4;
  int type;
  String varName;
  String propName;
  Field field;
  Object object;
  Object index;
  private ExtendedMethod getAtMethod = null;
  private ExtendedMethod putAtMethod = null;

  LHS(BshBinding nameSpace, String varName) {
    throw new Error("namespace lhs");
  }

  LHS(BshBinding nameSpace, String varName, boolean localVar) {
    this.type = 0;
    this.localVar = localVar;
    this.varName = varName;
    this.nameSpace = nameSpace;
  }

  LHS(Field field) {
    this.type = 1;
    this.object = null;
    this.field = field;
  }

  LHS(Object object, Field field) {
    if(object == null) {
      throw new NullPointerException("constructed empty LHS");
    } else {
      this.type = 1;
      this.object = object;
      this.field = field;
    }
  }

  LHS(Object object, String propName) {
    if(object == null) {
      throw new NullPointerException("constructed empty LHS");
    } else {
      this.type = 2;
      this.object = object;
      this.propName = propName;
    }
  }

  LHS() {
  }

  public static LHS indexLHS(Object target, Object keyIndex, Interpreter interpreter) {
    LHS lhs = new LHS();
    lhs.type = 3;
    lhs.object = target;
    lhs.index = keyIndex;
    lhs.nameSpace = interpreter.getNameSpace();
    return lhs;
  }

  public Object getValue() throws UtilEvalError {
    if(this.type == 0) {
      return this.nameSpace.getVariable(this.varName);
    } else if(this.type == 1) {
      try {
        this.field.setAccessible(true);
        Object type11 = this.field.get(this.object);
        return Primitive.wrap(type11, this.field.getType());
      } catch (IllegalAccessException var4) {
        throw new UtilEvalError("Can\'t read field: " + this.field + ": " + var4.toString());
      }
    } else if(this.type == 2) {
      try {
        return Reflect.getObjectProperty(this.object, this.propName);
      } catch (ReflectError var5) {
        Interpreter.debug(var5.getMessage());
        throw new UtilEvalError("No such property: " + this.propName);
      }
    } else if(this.type == 3) {
      Class type1 = this.object != null?this.object.getClass():null;
      Class type2 = this.index != null?this.index.getClass():null;
      this.getAtMethod = OperatorProvider.findMethod(this.nameSpace, OperatorType.GETAT.getMethodName(), this.getAtMethod, false, new Class[]{type1, type2});
      if(this.getAtMethod != null) {
        return this.getAtMethod.eval(new Object[]{this.object, this.index});
      } else if(this.index instanceof Integer) {
        try {
          return Reflect.getIndex(this.object, ((Integer)this.index).intValue());
        } catch (Exception var6) {
          throw new UtilEvalError(String.format("Array access: %s[%s]: %s", new Object[]{this.object, this.index, var6}), var6);
        }
      } else {
        throw new UtilEvalError("No object[index] subscript method found for data types.");
      }
    } else {
      throw new InterpreterError("LHS type");
    }
  }

  public Object assign(Object val, boolean strictJava) throws UtilEvalError {
    if(this.type == 0) {
      if(this.localVar) {
        this.nameSpace.setLocalVariable(this.varName, val, strictJava);
      } else {
        this.nameSpace.setVariable(this.varName, val, strictJava);
      }
    } else {
      Object val2;
      if(this.type == 1) {
        try {
          val2 = val instanceof Primitive
            ? ((Primitive) val).getValue()
            : val;
          
          if (JRE && (this.field.getModifiers() & Modifier.FINAL) != 0) {
            JREAccess.accessfld(
              this.object, // final Object inst
              this.field, // final Field fld
              val2, // final Object newValue
              true // final boolean isSet
            );
          } else {
            this.field.setAccessible(true);
            this.field.set(this.object, val2);
          }
          return val;
        } catch (NullPointerException var11) {
          throw new UtilEvalError("LHS (" + this.field.getName() + ") not a static field.", var11);
        } catch (IllegalAccessException var12) {
          throw new UtilEvalError("LHS (" + this.field.getName() + ") can\'t access field: " + var12, var12);
        } catch (IllegalArgumentException var13) {
          String type11 = val instanceof Primitive?((Primitive)val).getType().getName():val.getClass().getName();
          throw new UtilEvalError("Argument type mismatch. " + (val == null?"null":type11) + " not assignable to field " + this.field.getName());
        }
      }

      if(this.type == 2) {
        CollectionManager val21 = CollectionManager.getCollectionManager();
        if(val21.isMap(this.object)) {
          val21.putInMap(this.object, this.propName, val);
        } else {
          try {
            Reflect.setObjectProperty(this.object, this.propName, val);
          } catch (ReflectError var10) {
            Interpreter.debug("Assignment: " + var10.getMessage());
            throw new UtilEvalError("No such property: " + this.propName);
          }
        }
      } else {
        if(this.type == 3) {
          val2 = Primitive.unwrap(val);
          Class type1 = this.object != null?this.object.getClass():null;
          Class type2 = this.index != null?this.index.getClass():null;
          Class type3 = val2 != null?val2.getClass():null;
          this.putAtMethod = OperatorProvider.findMethod(this.nameSpace, OperatorType.PUTAT.getMethodName(), this.putAtMethod, false, new Class[]{type1, type2, type3});
          if(this.putAtMethod != null) {
            return this.putAtMethod.eval(new Object[]{this.object, this.index, val2});
          }

          if(this.object.getClass().isArray() && this.index instanceof Integer) {
            try {
              Reflect.setIndex(this.object, ((Integer)this.index).intValue(), val);
              return val;
            } catch (UtilTargetError var8) {
              throw var8;
            } catch (Exception var9) {
              throw new UtilEvalError("Assignment: " + var9.getMessage());
            }
          }

          throw new UtilEvalError("No object[index]=value subscript method found for data types.");
        }

        throw new InterpreterError("unknown lhs");
      }
    }

    return val;
  }

  public String toString() {
    return "LHS: " + (this.field != null?"field = " + this.field.toString():"") + (this.varName != null?" varName = " + this.varName:"") + (this.nameSpace != null?" nameSpace = " + this.nameSpace.toString():"");
  }
}