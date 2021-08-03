package bsh;

import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.Capabilities;
import bsh.ClassGenerator;
import bsh.ClassIdentifier;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.SimpleNode;
import bsh.StringUtil;
import bsh.This;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.UtilTargetError;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.d6r.CollectionUtil;
import org.d6r.Debug;
import org.d6r.Reflector;
import org.d6r.annotation.NonDumpable;

public class Name implements Serializable {
  public BshBinding namespace;
  public String value = null;
  String evalName;
  String lastEvalName;
  static String FINISHED = null;
  Object evalBaseObject;
  int callstackDepth;
  public Class<?> asClass;
  public Class<?> classOfStaticMethod;

  void reset() {
    if(Interpreter.TRACE) {
      Debug.debug("Name:: reset()");
    }

    this.evalName = this.value;
    this.evalBaseObject = null;
    this.callstackDepth = 0;
  }

  Name(BshBinding namespace, String s) {
    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: <init> (BshBinding namespace = %s, String s = %s)", new Object[]{namespace, s}));
    }

    this.namespace = namespace;
    this.value = s;
  }

  public Object toObject(CallStack callstack, Interpreter interpreter) throws UtilEvalError {
    if(Interpreter.TRACE) {
      Interpreter.debug(String.format("Name:: toObject(CallStack callstack, Interpreter interpreter)", new Object[]{callstack, interpreter}));
    }

    return this.toObject(callstack, interpreter, false);
  }

  public synchronized Object toObject(CallStack callstack, Interpreter interpreter, boolean forceClass) throws UtilEvalError {
    if(Interpreter.TRACE) {
      Interpreter.debug(String.format("Name:: toObject(CallStack callstack = %s, Interpreter interpreter = %s, boolean forceClass = %s)", new Object[]{callstack, interpreter, Boolean.valueOf(forceClass)}));
    }

    this.reset();
    Object obj = null;

    while(this.evalName != null) {
      if(Interpreter.DEBUG) {
        System.err.printf("Name = %s\n", new Object[]{Debug.ToString(this)});
      }

      obj = this.consumeNextObjectField(callstack, interpreter, forceClass, false);
      if(Interpreter.DEBUG) {
        System.err.printf("obj -> = %s\n", new Object[]{Debug.ToString(obj)});
        System.err.printf("Name = %s\n", new Object[]{Debug.ToString(this)});
      }
    }

    if(obj == null) {
      throw new InterpreterError("null value in toObject()");
    } else {
      return obj;
    }
  }

  Object completeRound(String lastEvalName, String nextEvalName, Object returnObject) {
    if(Interpreter.TRACE) {
      Interpreter.debug(String.format("Name:: completeRound(String lastEvalName = %s, String nextEvalName = %s, Object returnObject = %s)", new Object[]{lastEvalName, nextEvalName, returnObject}));
    }

    if(returnObject == null) {
      throw new InterpreterError("lastEvalName = " + lastEvalName);
    } else {
      this.lastEvalName = lastEvalName;
      this.evalName = nextEvalName;
      this.evalBaseObject = returnObject;
      return returnObject;
    }
  }

  public static Object tryMethodId(Class<?> cls, String name) {
    ArrayList methods = new ArrayList() {
      public String toString() {
        return String.format("%d candidate(s):\n  - %s\n", new Object[]{Integer.valueOf(this.size()), StringUtils.join(this.toArray(), "\n  - ")});
      }
    };

    do {
      try {
        Method[] var6;
        int var5 = (var6 = cls.getDeclaredMethods()).length;

        for(int var4 = 0; var4 < var5; ++var4) {
          Method mtd = var6[var4];
          if(mtd.getName().equals(name)) {
            mtd.setAccessible(true);
            methods.add(mtd);
          }
        }
      } catch (Throwable var7) {
        ;
      }

      cls = cls.getSuperclass();
    } while(cls != null);

    if(methods.size() == 0) {
      return null;
    } else {
      return methods;
    }
  }
  
  static @NonDumpable BshClassManager _bcm;
  static @NonDumpable Interpreter _globalInterpreter;
  static @NonDumpable NameSpace _globalNs;
  
  static @NonDumpable Factory sFactory = new Factory() {
    {
      Name._bcm = Factory.classManager;
      Name._globalInterpreter = Name._bcm.declaringInterpreter;
      Name._globalNs = (NameSpace) Name._globalInterpreter.getNameSpace();
    }
  };
  
  static Class<?> lookupClassQuick(String name) {
    String fullName = _globalNs.importedClasses.get(name);
    if (fullName == null) return null;
    return _bcm.classForName(fullName);
  }

  Object consumeNextObjectField(CallStack callstack, Interpreter interpreter, 
    boolean forceClass, boolean autoAllocateThis)
    throws UtilEvalError 
  {
    int dotPos = this.evalName.indexOf('.');
    
    if (this.evalBaseObject == null && dotPos == -1 && !forceClass) {
      Object varName = this.resolveThisFieldReference(
        callstack, this.namespace, interpreter, this.evalName, false
      );
      if (varName != Primitive.VOID && varName != Primitive.NULL) {
        return this.completeRound(this.evalName, FINISHED, varName);
      }
    }
    
    String namePart = prefix(this.evalName, 1);
    
    if (Interpreter.TRACE) {
      Interpreter.debug(String.format(
        "Name(%s).consumeNextObjectField(): evalBaseObject = %s, evalName = %s: "
        + "prefix := %s",
        this, evalBaseObject, evalName, namePart
      ));
    }
    
    Object clsName;
    int nd = countParts(this.evalName);
    
    String result = null;
    Class<?> resolvedClass = null;
    if ((this.evalBaseObject == null || this.evalBaseObject instanceof This)) {
      
      boolean var17 = false;
      
      
      boolean e = true;
      
      
      if(Interpreter.DEBUG) {
        Interpreter.debug("trying class: " + this.evalName);
      }
      
      int var21;
      for(var21 = nd; var21 > 0; --var21) {
        result = prefix(this.evalName, var21);
        
        if(Interpreter.DEBUG) {
          Interpreter.debug(result);
        }
        
        if (Character.isUpperCase(result.charAt(0))) {
          if ((resolvedClass = lookupClassQuick(result)) != null) break;
        }
      }
      
      if (resolvedClass != null) {
        return this.completeRound(
          result, suffix(this.evalName, countParts(this.evalName) - var21),
          new ClassIdentifier(resolvedClass)
        );
      }

      if(Interpreter.DEBUG) {
        Interpreter.debug("not a class, trying var prefix " + this.evalName);
      }
    }
    
    

    
    if ((this.evalBaseObject == null || this.evalBaseObject instanceof This || Proxy.isProxyClass(Factory.typeof(this.evalBaseObject))) && !forceClass) {
      if(Interpreter.DEBUG) {
        Interpreter.debug("trying to resolve variable: " + namePart);
      }

      Object field;
      if(this.evalBaseObject == null) {
        field = this.resolveThisFieldReference(callstack, this.namespace, interpreter, namePart, false);
      } else {
        clsName = org.d6r.Reflect.getfldval(this.evalBaseObject, "h");
        if(clsName instanceof This.Handler) {
          clsName = org.d6r.Reflect.getfldval(clsName, "this$0");
          if(clsName instanceof This) {
            this.evalBaseObject = (This)clsName;
            if(Interpreter.DEBUG) {
              System.err.printf("Found \'This\': %s\n", new Object[]{clsName});
            }
          }
        }

        field = this.resolveThisFieldReference(callstack, ((This)this.evalBaseObject).namespace, interpreter, namePart, true);
      }

      if(field != Primitive.VOID) {
        if(Interpreter.DEBUG) {
          Interpreter.debug("resolved variable: " + namePart + " in namespace: " + this.namespace);
        }

        return this.completeRound(namePart, suffix(this.evalName), field);
      }
    }
    
    
    if ((this.evalBaseObject == null || this.evalBaseObject instanceof This)) {
      int var21;
      
      for(var21 = nd; var21 > 0; --var21) {
        result = prefix(this.evalName, var21);
      
        if ((resolvedClass = this.namespace.getClass(result)) != null) {
          break;
        }

        if (Capabilities.classExists(result)) {
          try {
            resolvedClass = Class.forName(
              result, false, Thread.currentThread().getContextClassLoader()
            );
            break;
          } catch (NoClassDefFoundError | ClassNotFoundException var14) {
            System.err.println(var14);
            System.err.println(var14.getCause());
            interpreter.setu("$__e", var14);
          }
        }
      }

      if(resolvedClass != null) {
        return this.completeRound(result, suffix(this.evalName, countParts(this.evalName) - var21), new ClassIdentifier(resolvedClass));
      }

      if(Interpreter.DEBUG) {
        Interpreter.debug("not a class, trying var prefix " + this.evalName);
      }
    }

    if((this.evalBaseObject == null || this.evalBaseObject instanceof This) && !forceClass && autoAllocateThis) {
      BshBinding var18 = this.evalBaseObject == null?this.namespace:((This)this.evalBaseObject).namespace;
      This var27 = ((NameSpace)Factory.get(NameSpace.class).make(new Object[]{var18, "auto: " + namePart})).getThis(interpreter);
      var18.setVariable(namePart, var27, false);
      return this.completeRound(namePart, suffix(this.evalName), var27);
    } else if(this.evalBaseObject == null) {
      if(!isCompound(this.evalName)) {
        return this.completeRound(this.evalName, FINISHED, Primitive.VOID);
      } else {
        throw new UtilEvalError("Class or variable not found: " + this.evalName);
      }
    } else if(this.evalBaseObject == Primitive.NULL) {
      throw new UtilTargetError(new NullPointerException("Null Pointer while evaluating: " + this.value));
    } else if(this.evalBaseObject == Primitive.VOID) {
      throw new UtilEvalError("Undefined variable or class name while evaluating: " + this.value);
    } else if(this.evalBaseObject instanceof Primitive) {
      throw new UtilEvalError("Can\'t treat primitive like an object. Error while evaluating: " + this.value);
    } else {
      String var23;
      Object var24;
      BshBinding var28;
      if(!(this.evalBaseObject instanceof ClassIdentifier)) {
        if(forceClass) {
          throw new UtilEvalError(this.value + " does not resolve to a class name.");
        } else {
          String var20 = prefix(this.evalName, 1);
          if(var20.equals("length") && this.evalBaseObject.getClass().isArray()) {
            Primitive var31 = new Primitive(Array.getLength(this.evalBaseObject));
            return this.completeRound(var20, suffix(this.evalName), var31);
          } else {
            try {
              if(Interpreter.TRACE) {
                System.err.printf("Name: %s: calling Reflect.getObjectFieldValue(evalBaseObject=%s, field=%s)\n", new Object[]{Debug.ToString(this), Debug.ToString(this.evalBaseObject), Debug.ToString(var20)});
              }

              clsName = Reflect.getObjectFieldValue(this.evalBaseObject, var20);
              if(Interpreter.TRACE) {
                System.err.printf("Name: Reflect.getObjectFieldValue returned: %s\n", new Object[]{Debug.ToString(clsName)});
              }

              var24 = this.completeRound(var20, suffix(this.evalName), clsName);
              return var24;
            } catch (ReflectError var12) {
              
              
              
              if (this.evalBaseObject != null) {
                String suffixName = prefix(this.evalName, 1);
                Class<?> objectCls = this.evalBaseObject.getClass();
                Object maybeMethodId = tryMethodId(objectCls, suffixName);
                if (maybeMethodId != null) {
                  return this.completeRound(
                    suffixName, suffix(this.evalName), maybeMethodId
                  );
                }
              }
              
              // No such field
              
              if (var12.args == null) var12.args = new Object[]{
                callstack, interpreter, forceClass, autoAllocateThis
              };
              if (var12.name == null) var12.name = this;
              if (var12.namespace == null) var12.namespace = this.namespace;
              if (var12.callstack == null) var12.callstack = callstack;
              if (var12.node == null) {
                try {
                  SimpleNode node 
                    = (SimpleNode) 
                        ((NameSpace) callstack.top()).callerInfoNode;
                  if (node != null) var12.setNode(node);                  
                  // var12.otherNode = CallStack.getActiveCallStack().top();
                } catch (Throwable ignore) { ignore.printStackTrace(); }
              }
              /*
              Iterator var29 = callstack.iterator();
              while (var29.hasNext()) {
                var28 = (BshBinding)var29.next();
                SimpleNode var32;
                if((var32 = var28.getNode()) != null) {
                  if(var12.node == null) {
                    var12.node = var32;
                  } else {
                    if(var12.otherNode != null) {
                      break;
                    }

                    var12.otherNode = var32;
                  }
                }
              }
              */
              Map<String,Object> exData = var12.getData();
              exData.put("evalBaseObject", this.evalBaseObject);
              exData.put("evalName", this.evalName);
              exData.put("lastEvalName", this.lastEvalName);
              if (autoAllocateThis) {
                exData.put("autoAllocateThis", Boolean.TRUE);
              }
              if (forceClass) exData.put("forceClass", Boolean.TRUE);
              if (Interpreter.DEBUG) var12.printStackTrace();

              // System.err.println(var12);
              // System.err.println(var12.getCause());
              // interpreter.setu("$__e2", var12);
              // is it String.length?
              if ("length".equals(var20) 
              && this.evalBaseObject instanceof String) {
                Integer var26 = Integer.valueOf(
                  ((String) this.evalBaseObject).length()
                );
                var24 = this.completeRound(
                  var20, suffix(this.evalName), var26
                );
                return var24;
              } else {
                var23 = 
                  (this.evalBaseObject != null)
                    ? (
                      this.evalBaseObject.getClass().isPrimitive()
                        ? this.evalBaseObject.getClass().getCanonicalName()
                        : this.evalBaseObject.getClass().getName()
                      )
                    : "<null>";                    
                
                UtilEvalError noSuchFieldEvalError = new UtilEvalError(
                  String.format(
                    "No such field: `%s` of class: %s\n",
                    var20, var23                    
                  ),
                  var12 /*
                  new NoSuchFieldException(
                    String.format("%s.%s", var23, var20)
                  )
                  */
                );
                throw noSuchFieldEvalError;
              }
            }
          }
        }
      } else {
        Class var16 = ((ClassIdentifier)this.evalBaseObject).getTargetClass();
        var23 = prefix(this.evalName, 1);
        if(var23.equals("this")) {
          for(var28 = this.namespace; var28 != null; var28 = var28.getParent()) {
            if(var28.getClassInstance() != null && var28.getClassInstance().getClass() == var16) {
              return this.completeRound(var23, suffix(this.evalName), var28.getClassInstance());
            }
          }

          throw new UtilEvalError("Can\'t find enclosing \'this\' instance of class: " + var16);
        } else {
          var24 = null;

          try {
            if(Interpreter.DEBUG) {
              Interpreter.debug("Name call to getStaticFieldValue, class: " + var16 + ", field:" + var23);
            }

            var24 = Reflect.getStaticFieldValue(var16, var23);
          } catch (ReflectError var13) {
            if(Interpreter.DEBUG) {
              Interpreter.debug("field reflect error: " + var13);
            }
          }

          if(var24 == null) {
            result = var16.getName() + "$" + var23;
            Class var30 = this.namespace.getClass(result);
            if(var30 != null) {
              var24 = new ClassIdentifier(var30);
            }
          }

          if(var24 == null) {
            Object var25 = tryMethodId(var16, var23);
            if(var25 == null) {
              throw new UtilEvalError("No static field or inner class: " + var23 + " of " + var16);
            }

            var24 = var25;
          }

          return this.completeRound(var23, suffix(this.evalName), var24);
        }
      }
    }
  }

  Object resolveThisFieldReference(CallStack callstack, BshBinding thisNameSpace, Interpreter interpreter, String varName, boolean specialFieldsVisible) throws UtilEvalError {
    if(Interpreter.DEBUG) {
      System.err.printf("resolveThisFieldReference(CallStack callstack = %s, BshBinding thisNameSpace = %s, Interpreter interpreter = %s, String varName = %s, boolean specialFieldsVisible = %s)\n", new Object[]{Debug.ToString(callstack), Debug.ToString(thisNameSpace), interpreter, varName, Boolean.valueOf(specialFieldsVisible)});
    }

    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: resolveThisFieldReference(CallStack callstacresolveThisFieldReference(CallStack callstack = %s, BshBinding thisNameSpace = %s, Interpreter interpreter = %s, String varName = %s, boolean specialFieldsVisible = %s", new Object[]{callstack, thisNameSpace, interpreter, varName, Boolean.valueOf(specialFieldsVisible)}));
    }

    Object staticValue;
    NameSpace var9;
    This var10;
    if(varName.equals("this")) {
      if(specialFieldsVisible) {
        throw new UtilEvalError("Redundant to call .this on This type");
      } else {
        var10 = thisNameSpace.getThis(interpreter);
        var9 = var10.getNameSpace();
        staticValue = var10;
        BshBinding classNameSpace = getClassNameSpace(var9);
        if(classNameSpace != null) {
          if(isCompound(this.evalName)) {
            staticValue = classNameSpace.getThis(interpreter);
          } else {
            staticValue = classNameSpace.getClassInstance();
          }
        }

        return staticValue;
      }
    } else if(varName.equals("super")) {
      var10 = thisNameSpace.getSuper(interpreter);
      var9 = var10.getNameSpace();
      if(var9.getParent() != null && var9.getParent().isClass()) {
        var10 = var9.getParent().getThis(interpreter);
      }

      return var10;
    } else {
      Object obj = null;
      if(varName.equals("global")) {
        obj = thisNameSpace.getGlobal(interpreter);
      }

      if(obj == null && specialFieldsVisible) {
        if(varName.equals("namespace")) {
          obj = thisNameSpace;
        } else if(varName.equals("variables")) {
          obj = thisNameSpace.getVariableNames();
        } else if(varName.equals("methods")) {
          obj = thisNameSpace.getMethodNames();
        } else if(varName.equals("interpreter")) {
          if(!this.lastEvalName.equals("this")) {
            throw new UtilEvalError("Can only call .interpreter on literal \'this\'");
          }

          obj = interpreter;
        }
      }

      if(obj == null && specialFieldsVisible && varName.equals("caller")) {
        if(!this.lastEvalName.equals("this") && !this.lastEvalName.equals("caller")) {
          throw new UtilEvalError("Can only call .caller on literal \'this\' or literal \'.caller\'");
        } else if(callstack == null) {
          throw new InterpreterError("no callstack");
        } else {
          var10 = callstack.get(++this.callstackDepth).getThis(interpreter);
          return var10;
        }
      } else {
        if(obj == null && specialFieldsVisible && varName.equals("callstack")) {
          if(!this.lastEvalName.equals("this")) {
            throw new UtilEvalError("Can only call .callstack on literal \'this\'");
          }

          if(callstack == null) {
            throw new InterpreterError("no callstack");
          }

          obj = callstack;
        }

        if(obj == null) {
          obj = thisNameSpace.getVariable(varName);
        }

        if(obj == null) {
          staticValue = resolveStatic(varName);
          if(staticValue != Primitive.VOID) {
            return staticValue;
          }
        }

        if(obj == null) {
          throw new InterpreterError("null this field ref:" + varName);
        } else {
          return obj;
        }
      }
    }
  }

  static BshBinding getClassNameSpace(BshBinding thisNameSpace) {
    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: getClassNameSpace(BshBinding thisNameSpace = %s)", new Object[]{thisNameSpace}));
    }

    return thisNameSpace.isClass()?thisNameSpace:(thisNameSpace.isMethod() && thisNameSpace.getParent() != null && thisNameSpace.getParent().isClass()?thisNameSpace.getParent():null);
  }

  public static Object resolveStatic(String varName) {
    NameSpace ns = (NameSpace)CollectionUtil.getInterpreter().getNameSpace();
    Iterator var3 = ns.importedStatic.iterator();

    while(var3.hasNext()) {
      Class cls = (Class)var3.next();
      Field[] var7;
      int var6 = (var7 = cls.getDeclaredFields()).length;

      for(int var5 = 0; var5 < var6; ++var5) {
        Field fld = var7[var5];
        if((fld.getModifiers() & 8) != 0 && fld.getName().equals(varName)) {
          try {
            fld.setAccessible(true);
            Object e = fld.get((Object)null);
            return e;
          } catch (Throwable var9) {
            var9.printStackTrace();
          }
        }
      }
    }

    return Primitive.VOID;
  }

  public synchronized Class toClass() throws ClassNotFoundException, UtilEvalError {
    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: toClass()", new Object[0]));
    }

    if(this.asClass != null) {
      return this.asClass;
    } else {
      this.reset();
      if(this.evalName.equals("var")) {
        return this.asClass = null;
      } else {
        Class clas = this.namespace.getClass(this.evalName);
        if(clas == null) {
          Object obj = null;

          try {
            obj = this.toObject((CallStack)null, (Interpreter)null, true);
          } catch (UtilEvalError var4) {
            ;
          }

          if(obj instanceof ClassIdentifier) {
            clas = ((ClassIdentifier)obj).getTargetClass();
          }
        }

        if(clas == null) {
          throw new ClassNotFoundException("Class: " + this.value + " not found in namespace");
        } else {
          this.asClass = clas;
          return this.asClass;
        }
      }
    }
  }

  public synchronized LHS toLHS(CallStack callstack, Interpreter interpreter) throws UtilEvalError {
    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: toLHS(CallStack callstack = %s, Interpreter interpreter = %s)", new Object[]{callstack, interpreter}));
    }

    this.reset();
    LHS lhs;
    if(!isCompound(this.evalName)) {
      if(this.evalName.equals("this")) {
        throw new UtilEvalError("Can\'t assign to \'this\'.");
      } else {
        lhs = new LHS(this.namespace, this.evalName, false);
        return lhs;
      }
    } else {
      Object obj = null;

      try {
        while(this.evalName != null && isCompound(this.evalName)) {
          obj = this.consumeNextObjectField(callstack, interpreter, false, false);
        }
      } catch (UtilEvalError var7) {
        throw new UtilEvalError("LHS evaluation: " + var7.getMessage());
      }

      if(this.evalName == null && obj instanceof ClassIdentifier) {
        throw new UtilEvalError("Can\'t assign to class: " + this.value);
      } else if(obj == null) {
        throw new UtilEvalError("Error in LHS: " + this.value);
      } else if(obj instanceof This) {
        if(!this.evalName.equals("namespace") && !this.evalName.equals("variables") && !this.evalName.equals("methods") && !this.evalName.equals("caller")) {
          Interpreter.debug("found This reference evaluating LHS");
          boolean e1 = !this.lastEvalName.equals("super");
          return new LHS(((This)obj).namespace, this.evalName, e1);
        } else {
          throw new UtilEvalError("Can\'t assign to special variable: " + this.evalName);
        }
      } else if(this.evalName != null) {
        try {
          if(obj instanceof ClassIdentifier) {
            Class e = ((ClassIdentifier)obj).getTargetClass();
            lhs = Reflect.getLHSStaticField(e, this.evalName);
            return lhs;
          } else {
            lhs = Reflect.getLHSObjectField(obj, this.evalName);
            return lhs;
          }
        } catch (ReflectError var6) {
          throw new UtilEvalError("Field access: " + var6, var6);
        }
      } else {
        throw new InterpreterError("Internal error in lhs...");
      }
    }
  }

  public Object invokeMethod(Interpreter interpreter, Object[] args, CallStack callstack, SimpleNode callerInfo) throws UtilEvalError, EvalError, ReflectError, InvocationTargetException {
    if(Interpreter.TRACE) {
      Debug.debug(String.format("Name:: invokeMethod(Interpreter interpreter = %s, Object[] args = %s, CallStack callstack = %s, SimpleNode callerInfo = %s)", new Object[]{interpreter, args, callstack, callerInfo}));
    }

    String methodName = suffix(this.value, 1);
    BshClassManager bcm = interpreter.getClassManager();
    BshBinding namespace = callstack.top();
    if(this.classOfStaticMethod != null) {
      return Reflect.invokeStaticMethod(bcm, this.classOfStaticMethod, methodName, args);
    } else if(!isCompound(this.value)) {
      return this.invokeLocalMethod(interpreter, args, callstack, callerInfo);
    } else {
      String prefix = prefix(this.value);
      if(prefix.equals("super") && countParts(this.value) == 2) {
        This targetName = namespace.getThis(interpreter);
        NameSpace obj = targetName.getNameSpace();
        BshBinding clas = getClassNameSpace(obj);
        if(clas != null) {
          Object instance = clas.getClassInstance();
          return ClassGenerator.getClassGenerator().invokeSuperclassMethod(bcm, instance, methodName, args);
        }
      }

      Name targetName1 = namespace.getNameResolver(prefix);
      if (callerInfo != null) callerInfo.touch(callstack);
      Object obj1 = targetName1.toObject(callstack, interpreter);
      if(!(obj1 instanceof ClassIdentifier)) {
        if(obj1 instanceof Primitive) {
          if (obj1 == Primitive.VOID) {
            
            throw new EvalError(String.format(
              "Unresolved name: \"%s\" in expression: %s",
              prefix, evalName
            ));
          }
          if(obj1 == Primitive.NULL) {
            throw new UtilTargetError(new NullPointerException("Null Pointer in Method Invocation of " + methodName + "() on variable: " + targetName1));
          }

          if(Interpreter.DEBUG) {
            Interpreter.debug("Attempt to access method on primitive... allowing bsh.Primitive to peek through for debugging");
          }
        }

        return Reflect.invokeObjectMethod(obj1, methodName, args, interpreter, callstack, callerInfo);
      } else {
        if(Interpreter.DEBUG) {
          Interpreter.debug("invokeMethod: trying static - " + targetName1);
        }

        Class clas1 = ((ClassIdentifier)obj1).getTargetClass();
        this.classOfStaticMethod = clas1;
        if(clas1 != null) {
          return Reflect.invokeStaticMethod(bcm, clas1, methodName, args);
        } else {
          throw new UtilEvalError("invokeMethod: unknown target: " + targetName1);
        }
      }
    }
  }

  Object invokeLocalMethod(Interpreter interpreter, Object[] args, CallStack callstack, SimpleNode callerInfo) throws EvalError {
    if(Interpreter.DEBUG) {
      Interpreter.debug("invokeLocalMethod: " + this.value);
    }

    if(interpreter == null) {
      throw new InterpreterError("invokeLocalMethod: interpreter = null");
    } else {
      String commandName = this.value;
      Class[] argTypes = Types.getTypes(args);
      BshMethod meth = null;
      BshBinding ns = this.namespace;
      BshClassManager bcm = null;
      Object commandObject = Void.TYPE;

      BshMethod invokeMethod;
      for(invokeMethod = null; ns != null; ns = ns.getParent()) {
        try {
          meth = ns.getMethod(commandName, argTypes);
        } catch (UtilEvalError var16) {
          throw var16.toEvalError("Local method invocation", callerInfo, callstack);
        }

        if(meth != null) {
          return meth.invoke(args, interpreter, callstack, callerInfo);
        }

        if(commandObject == Void.TYPE) {
          commandObject = null;

          try {
            commandObject = interpreter.getNameSpace().getCommand(commandName, argTypes, interpreter);
            if(commandObject != null) {
              break;
            }
          } catch (UtilEvalError var18) {
            commandObject = null;
            throw var18.toEvalError("Error loading command: ", callerInfo, callstack);
          }
        }
      }

      if(bcm == null) {
        bcm = interpreter.getClassManager();
      }

      if(commandObject == null) {
        try {
          invokeMethod = this.namespace.getMethod("invoke", new Class[2]);
        } catch (UtilEvalError namePart) {
          throw namePart.toEvalError("Local method invocation", callerInfo, callstack);
        }

        if(invokeMethod != null) {
          return invokeMethod.invoke(new Object[]{commandName, args}, interpreter, callstack, callerInfo);
        } else {
          throw new EvalError("Command not found: " + StringUtil.methodString(commandName, argTypes), callerInfo, callstack);
        }
      } else if(commandObject instanceof BshMethod) {
        return ((BshMethod)commandObject).invoke(args, interpreter, callstack, callerInfo);
      } else if(commandObject instanceof Class) {
        Class cmdCls = (Class)commandObject;

        try {
          return Reflect.invokeCompiledCommand(cmdCls, args, interpreter, callstack);
        } catch (UtilEvalError var17) {
          EvalError ee = var17.toEvalError(String.format("Error invoking compiled command (cmdCls = %s, args = %s, callerInfo = %s)\n", new Object[]{Debug.ToString(cmdCls), Debug.ToString(args), Debug.ToString(callerInfo)}), callerInfo, callstack, var17);
          if(Interpreter.DEBUG && Interpreter.DEBUG) {
            ee.printStackTrace();
          }

          throw ee;
        }
      } else {
        throw new InterpreterError("invalid command type");
      }
    }
  }

  public static boolean isCompound(String value) {
    return value.indexOf(46) != -1;
  }

  static int countParts(String value) {
    if(value == null) {
      return 0;
    } else {
      int count = 0;

      for(int index = -1; (index = value.indexOf(46, index + 1)) != -1; ++count) {
        ;
      }

      return count + 1;
    }
  }

  static String prefix(String value) {
    return !isCompound(value)?null:prefix(value, countParts(value) - 1);
  }

  static String prefix(String value, int parts) {
    if(parts < 1) {
      return null;
    } else {
      int count = 0;
      int index = -1;

      while((index = value.indexOf(46, index + 1)) != -1) {
        ++count;
        if(count >= parts) {
          break;
        }
      }

      return index == -1?value:value.substring(0, index);
    }
  }

  static String suffix(String name) {
    return !isCompound(name)?null:suffix(name, countParts(name) - 1);
  }

  public static String suffix(String value, int parts) {
    if(parts < 1) {
      return null;
    } else {
      int count = 0;
      int index = value.length() + 1;

      while((index = value.lastIndexOf(46, index - 1)) != -1) {
        ++count;
        if(count >= parts) {
          break;
        }
      }

      return index == -1?value:value.substring(index + 1);
    }
  }

  public String toString() {
    return this.value;
  }
}