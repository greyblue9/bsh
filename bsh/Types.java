package bsh;

import bsh.InterpreterError;
import bsh.Primitive;
import bsh.Reflect;
import bsh.This;
import bsh.UtilEvalError;
import bsh.UtilTargetError;

public class Types {
  static final int CAST = 0;
  static final int ASSIGNMENT = 1;
  static final int JAVA_BASE_ASSIGNABLE = 1;
  static final int JAVA_BOX_TYPES_ASSIGABLE = 2;
  static final int JAVA_VARARGS_ASSIGNABLE = 3;
  static final int BSH_ASSIGNABLE = 4;
  static final int FIRST_ROUND_ASSIGNABLE = 1;
  static final int LAST_ROUND_ASSIGNABLE = 4;
  public static Primitive VALID_CAST = new Primitive(1);
  public static Primitive INVALID_CAST = new Primitive(-1);

  public static Class<?>[] getTypes(Object[] args) {
    if(args == null) {
      return new Class[0];
    } else {
      Class[] types = new Class[args.length];

      for(int i = 0; i < args.length; ++i) {
        if(args[i] == null) {
          types[i] = null;
        } else if(args[i] instanceof Primitive) {
          types[i] = ((Primitive)args[i]).getType();
        } else {
          types[i] = args[i].getClass();
        }
      }

      return types;
    }
  }

  public static boolean isSignatureAssignable(Class[] from, Class[] to, int round) {
    if(round != 3 && from.length != to.length) {
      return false;
    } else {
      int i;
      switch(round) {
      case 1:
        for(i = 0; i < from.length; ++i) {
          if(!isJavaBaseAssignable(to[i], from[i])) {
            return false;
          }
        }

        return true;
      case 2:
        for(i = 0; i < from.length; ++i) {
          if(!isJavaBoxTypesAssignable(to[i], from[i])) {
            return false;
          }
        }

        return true;
      case 3:
        return isSignatureVarargsAssignable(from, to);
      case 4:
        for(i = 0; i < from.length; ++i) {
          if(!isBshAssignable(to[i], from[i])) {
            return false;
          }
        }

        return true;
      default:
        throw new InterpreterError("bad case");
      }
    }
  }

  public static boolean isSignatureVarargsAssignable(Class[] from, Class[] to) {
    return false;
  }

  public static boolean isJavaAssignable(Class lhsType, Class rhsType) {
    return isJavaBaseAssignable(lhsType, rhsType) || isJavaBoxTypesAssignable(lhsType, rhsType);
  }

  public static boolean isJavaBaseAssignable(Class<?> lhsType, Class<?> rhsType) {
    if(lhsType == null) {
      return false;
    } else if(rhsType == null) {
      return !lhsType.isPrimitive();
    } else {
      if(lhsType.isPrimitive() && rhsType.isPrimitive()) {
        if(lhsType == rhsType) {
          return true;
        }

        if(rhsType == Byte.TYPE && (lhsType == Short.TYPE || lhsType == Integer.TYPE || lhsType == Long.TYPE || lhsType == Float.TYPE || lhsType == Double.TYPE)) {
          return true;
        }

        if(rhsType == Short.TYPE && (lhsType == Integer.TYPE || lhsType == Long.TYPE || lhsType == Float.TYPE || lhsType == Double.TYPE)) {
          return true;
        }

        if(rhsType == Character.TYPE && (lhsType == Integer.TYPE || lhsType == Long.TYPE || lhsType == Float.TYPE || lhsType == Double.TYPE)) {
          return true;
        }

        if(rhsType == Integer.TYPE && (lhsType == Long.TYPE || lhsType == Float.TYPE || lhsType == Double.TYPE)) {
          return true;
        }

        if(rhsType == Long.TYPE && (lhsType == Float.TYPE || lhsType == Double.TYPE)) {
          return true;
        }

        if(rhsType == Float.TYPE && lhsType == Double.TYPE) {
          return true;
        }
      } else if(lhsType.isAssignableFrom(rhsType)) {
        return true;
      }

      return false;
    }
  }

  public static boolean isJavaBoxTypesAssignable(Class lhsType, Class rhsType) {
    return lhsType == null?false:(lhsType == Object.class?true:(lhsType == Number.class && rhsType != Character.TYPE && rhsType != Boolean.TYPE?true:Primitive.wrapperMap.get(lhsType) == rhsType));
  }

  public static boolean isBshAssignable(Class toType, Class fromType) {
    try {
      return castObject(toType, fromType, (Object)null, 1, true) == VALID_CAST;
    } catch (UtilEvalError var3) {
      throw new InterpreterError("err in cast check: " + var3);
    }
  }

  public static Object castObject(Object fromValue, Class toType, int operation) throws UtilEvalError {
    if(fromValue == null) {
      throw new InterpreterError("null fromValue");
    } else {
      Class fromType = fromValue instanceof Primitive?((Primitive)fromValue).getType():fromValue.getClass();
      return castObject(toType, fromType, fromValue, operation, false);
    }
  }

  public static Object castObject(Class<?> toType, Class<?> fromType, Object fromValue, int operation, boolean checkOnly) throws UtilEvalError {
    if(checkOnly && fromValue != null) {
      throw new InterpreterError("bad cast params 1");
    } else if(!checkOnly && fromValue == null) {
      throw new InterpreterError("bad cast params 2");
    } else if(fromType == Primitive.class) {
      throw new InterpreterError("bad from Type, need to unwrap");
    } else if(fromValue == Primitive.NULL && fromType != null) {
      throw new InterpreterError("inconsistent args 1");
    } else if(fromValue == Primitive.VOID && fromType != Void.TYPE) {
      throw new InterpreterError("inconsistent args 2");
    } else if(toType == Void.TYPE) {
      throw new InterpreterError("loose toType should be null");
    } else if(toType != null && toType != fromType) {
      if(toType.isPrimitive()) {
        if(fromType != Void.TYPE && fromType != null && !fromType.isPrimitive()) {
          if(Primitive.isWrapperType(fromType)) {
            Class unboxedFromType = Primitive.unboxType(fromType);
            Primitive primFromValue;
            if(checkOnly) {
              primFromValue = null;
            } else {
              primFromValue = (Primitive)Primitive.wrap(fromValue, unboxedFromType);
            }

            return Primitive.castPrimitive(toType, unboxedFromType, primFromValue, checkOnly, operation);
          } else if(checkOnly) {
            return INVALID_CAST;
          } else {
            throw castError(toType, fromType, operation);
          }
        } else {
          return Primitive.castPrimitive(toType, fromType, (Primitive)fromValue, checkOnly, operation);
        }
      } else if(fromType != Void.TYPE && fromType != null && !fromType.isPrimitive()) {
        if(toType.isAssignableFrom(fromType)) {
          return checkOnly?VALID_CAST:fromValue;
        } else if(toType.isInterface() && This.class.isAssignableFrom(fromType)) {
          return checkOnly?VALID_CAST:((This)fromValue).getInterface(toType);
        } else if(Primitive.isWrapperType(toType) && Primitive.isWrapperType(fromType)) {
          return checkOnly?VALID_CAST:Primitive.castWrapper(toType, fromValue);
        } else if(checkOnly) {
          return INVALID_CAST;
        } else {
          throw castError(toType, fromType, operation);
        }
      } else {
        return Primitive.isWrapperType(toType) && fromType != Void.TYPE && fromType != null?(checkOnly?VALID_CAST:Primitive.castWrapper(Primitive.unboxType(toType), ((Primitive)fromValue).getValue())):(toType == Object.class && fromType != Void.TYPE && fromType != null?(checkOnly?VALID_CAST:((Primitive)fromValue).getValue()):Primitive.castPrimitive(toType, fromType, (Primitive)fromValue, checkOnly, operation));
      }
    } else {
      return checkOnly?VALID_CAST:fromValue;
    }
  }

  public static UtilEvalError castError(Class lhsType, Class rhsType, int operation) {
    return castError(Reflect.normalizeClassName(lhsType), Reflect.normalizeClassName(rhsType), operation);
  }

  public static UtilEvalError castError(String lhs, String rhs, int operation) {
    if(operation == 1) {
      return new UtilEvalError("Can\'t assign " + rhs + " to " + lhs);
    } else {
      ClassCastException cce = new ClassCastException("Cannot cast " + rhs + " to " + lhs);
      return new UtilTargetError(cce);
    }
  }
}
