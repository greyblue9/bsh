package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHArguments;
import bsh.BSHType;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.CollectionManager;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.SimpleNode;
import bsh.TargetError;
import bsh.UtilEvalError;
import bsh.operators.ExtendedMethod;
import bsh.operators.OperatorProvider;
import bsh.operators.OperatorType;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import org.d6r.CollectionUtil;
import org.d6r.Debug;

public class BSHPrimarySuffix<S> extends SimpleNode {
  public static final int CLASS = 0;
  public static final int INDEX = 1;
  public static final int NAME = 2;
  public static final int PROPERTY = 3;
  public int operation;
  Object index;
  String field;
  private ExtendedMethod getAtMethod = null;
  private ExtendedMethod putAtMethod = null;

  BSHPrimarySuffix(int id) {
    super(id);
  }

  public String getMemberName() {
    return this.field;
  }

  public String toString() {
    return this.getText();
  }

  public Object doSuffix(S obj, boolean toLHS, CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.operation == 0) {
      if(obj instanceof BSHType) {
        if(toLHS) {
          throw new EvalError("Can\'t assign .class", this, callstack);
        } else {
          BshBinding e = callstack.top();
          return ((BSHType)obj).getType(callstack, interpreter);
        }
      } else {
        throw new EvalError("Attempt to use .class suffix on non class.", this, callstack);
      }
    } else {
      if(obj instanceof SimpleNode) {
        if(obj instanceof BSHAmbiguousName) {
          obj = (S) (Object) ((BSHAmbiguousName)obj).toObject(callstack, interpreter);
        } else {
          obj = (S) (Object) ((SimpleNode)obj).eval(callstack, interpreter);
        }
      } else if(obj instanceof LHS) {
        try {
          obj = (S) (Object) ((LHS)obj).getValue();
        } catch (UtilEvalError var8) {
          throw var8.toEvalError(this, callstack);
        }
      }

      try {
        switch(this.operation) {
        case 1:
          return this.doIndex(obj, toLHS, callstack, interpreter);
        case 2:
          return this.doName(obj, toLHS, callstack, interpreter);
        case 3:
          return this.doProperty(toLHS, obj, callstack, interpreter);
        default:
          throw new InterpreterError("Unknown suffix type");
        }
      } catch (ReflectError var6) {
        throw new EvalError("reflection error: " + var6, this, callstack, var6);
      } catch (InvocationTargetException var7) {
        throw new TargetError("target exception", var7.getTargetException(), this, callstack, true);
      }
    }
  }

  private Object doName(Object obj, boolean toLHS, CallStack callstack, Interpreter interpreter) throws EvalError, ReflectError, InvocationTargetException {
    try {
      if(this.field.equals("length") && obj.getClass().isArray()) {
        if(toLHS) {
          throw new EvalError("Can\'t assign array length", this, callstack);
        } else {
          return new Primitive(Array.getLength(obj));
        }
      } else if(this.jjtGetNumChildren() == 0) {
        return toLHS?Reflect.getLHSObjectField(obj, this.field):Reflect.getObjectFieldValue(obj, this.field);
      } else {
        Object[] e = ((BSHArguments)this.jjtGetChild(0)).getArguments(callstack, interpreter);

        try {
          return Reflect.invokeObjectMethod(obj, this.field, e, interpreter, callstack, this);
        } catch (ReflectError var10) {
          throw new EvalError("Error in method invocation: " + var10.getMessage(), this, callstack, var10);
        } catch (InvocationTargetException var11) {
          String msg = "Method Invocation " + this.field;
          Throwable te = var11.getTargetException();
          boolean isNative = true;
          if(te instanceof EvalError) {
            if(te instanceof TargetError) {
              isNative = ((TargetError)te).inNativeCode();
            } else {
              isNative = false;
            }
          }

          throw new TargetError(msg, te, this, callstack, isNative);
        }
      }
    } catch (UtilEvalError var12) {
      throw var12.toEvalError(this, callstack);
    }
  }

  static Object getIndexAux(Object obj, CallStack callstack, Interpreter interpreter, SimpleNode callerInfo) throws EvalError {
    Object indexVal = ((SimpleNode)callerInfo.jjtGetChild(0)).eval(callstack, interpreter);
    return Primitive.unwrap(indexVal);
  }

  private Object doIndex(Object indexed, boolean toLHS, CallStack callstack, Interpreter interpreter) throws EvalError, ReflectError {
    Object index = getIndexAux(indexed, callstack, interpreter, this);
    if(toLHS) {
      return LHS.indexLHS(indexed, index, interpreter);
    } else {
      Object obj = indexed;
      Class cls = indexed != null?indexed.getClass():null;
      Class indexCls = index != null?index.getClass():null;
      if(Enumeration.class.isAssignableFrom(cls)) {
        cls = (obj = CollectionUtil.asIterable((Enumeration)indexed)).getClass();
      }

      this.getAtMethod = OperatorProvider.findMethod(interpreter.getNameSpace(), OperatorType.GETAT.getMethodName(), this.getAtMethod, true, new Class[]{cls, indexCls});
      boolean isArray = cls.isArray();
      boolean isIterable = Iterable.class.isAssignableFrom(cls);
      int idx = Integer.MIN_VALUE;
      if(Interpreter.TRACE) {
        System.err.printf("index = (%s) %s\n", new Object[]{index.getClass(), Debug.ToString(index)});
      }

      if(!(index instanceof Number) || !isArray && !isIterable) {
        if(Map.class.isAssignableFrom(cls)) {
          Map e2 = (Map)obj;
          return e2.containsKey(index)?e2.get(index):Primitive.NULL;
        }
      } else {
        try {
          if(Interpreter.TRACE) {
            System.err.println("try");
          }

          idx = ((Number)index).intValue();
          if(Interpreter.TRACE) {
            System.err.printf("idx = %d\n", new Object[]{Integer.valueOf(idx)});
          }
        } catch (Throwable var15) {
          if(Interpreter.DEBUG) {
            var15.printStackTrace();
          }
        }

        if (Interpreter.TRACE) {
          System.err.printf("idx = %d\n", new Object[]{Integer.valueOf(idx)});
        }

        if (idx != Integer.MIN_VALUE && idx < 0) {
          boolean e = true;
          int e1;
          if(isArray) {
            e1 = Array.getLength(obj);
          } else if(obj instanceof Collection) {
            e1 = ((Collection<?>) obj).size();
          } else if (obj instanceof Iterable<?>) {
            e1 = CollectionUtil.sizeof((Iterable<?>) obj);
          } else if (obj instanceof Iterator) {
            Object[] objs = org.d6r.Reflect.findField(obj, Collection.class, 1);
            if (objs.length >= 2 && objs[1] != null) {
              e1 = ((Collection<?>) objs[1]).size();
            } else {
              e1 = 0;
            }
          } else {
            e1 = 0;
          }

          if (-idx >= e1) {
            int bulkAdd = (int)((double)(-idx) / (double)e1) * e1;
            idx += bulkAdd;
          }

          if (idx < 0) {
            idx += e1;
          }

          if(isArray) {
            return Array.get(obj, idx);
          }

          return CollectionUtil.getAt((Iterable)obj, idx);
        }
      }

      if(this.getAtMethod != null) {
        return this.getAtMethod.eval(new Object[]{obj, index});
      } else if(index instanceof Integer) {
        try {
          return Reflect.getIndex(obj, ((Integer)index).intValue());
        } catch (UtilEvalError var14) {
          throw var14.toEvalError(this, callstack);
        }
      } else {
        throw new EvalError(String.format("No object[index] subscript method found for data types:\n   <%s>[%s]", new Object[]{cls.getName(), indexCls.getName()}), this, callstack);
      }
    }
  }

  private Object doProperty(boolean toLHS, Object obj, CallStack callstack, Interpreter interpreter) throws EvalError {
    if(obj == Primitive.VOID) {
      throw new EvalError("Attempt to access property on undefined variable or class name", this, callstack);
    } else if(obj instanceof Primitive) {
      throw new EvalError("Attempt to access property on a primitive", this, callstack);
    } else {
      Object value = ((SimpleNode)this.jjtGetChild(0)).eval(callstack, interpreter);
      if(!(value instanceof String)) {
        throw new EvalError("Property expression must be a String or identifier.", this, callstack);
      } else if(toLHS) {
        return new LHS(obj, (String)value);
      } else {
        CollectionManager cm = CollectionManager.getCollectionManager();
        if(cm.isMap(obj)) {
          Object e = cm.getFromMap(obj, value);
          Primitive e1;
          return e == null?(e1 = Primitive.NULL):e;
        } else {
          try {
            return Reflect.getObjectProperty(obj, (String)value);
          } catch (UtilEvalError var8) {
            throw var8.toEvalError("Property: " + value, this, callstack);
          } catch (ReflectError var9) {
            throw new EvalError("No such property: " + value, this, callstack);
          }
        }
      }
    }
  }
}