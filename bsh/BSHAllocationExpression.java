package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHArguments;
import bsh.BSHArrayDimensions;
import bsh.BSHBlock;
import bsh.BSHPrimitiveType;
import bsh.BlockNameSpace;
import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.ClassGenerator;
import bsh.ClassGeneratorUtil;
import bsh.ClassIdentifier;
import bsh.EvalError;
import bsh.Factory;
import bsh.GeneratedClass;
import bsh.InstanceId;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.Name;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.SimpleNode;
import bsh.TargetError;
import bsh.This;
import bsh.Types;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.d6r.ClassPathUtil2;
import org.d6r.Debug;
import org.d6r.DexVisitor;
import org.d6r.Dumper;
import sun.misc.Unsafe;

public class BSHAllocationExpression extends SimpleNode {
  private static int innerClassCount = 0;
  public SimpleNode type;
  public SimpleNode args;
  static Unsafe unsafe = ClassPathUtil2.getUnsafe();
  static Class<? extends List> EMPTY_LIST_CLASS;

  BSHAllocationExpression(int id) {
    super(id);
    this.type = SimpleNode.DEFAULT;
    this.args = SimpleNode.DEFAULT;
  }

  public void jjtClose() {
    this.type = (SimpleNode)this.jjtGetChild(0);
    this.args = (SimpleNode)this.jjtGetChild(1);
    super.jjtClose();
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.type instanceof BSHAmbiguousName) {
      BSHAmbiguousName name = (BSHAmbiguousName)this.type;
      return this.args instanceof BSHArguments?this.objectAllocation(name, (BSHArguments)this.args, callstack, interpreter):this.objectArrayAllocation(name, (BSHArrayDimensions)this.args, callstack, interpreter);
    } else {
      return this.primitiveArrayAllocation((BSHPrimitiveType)this.type, (BSHArrayDimensions)this.args, callstack, interpreter);
    }
  }

  private Object objectAllocation(BSHAmbiguousName nameNode, BSHArguments argumentsNode, CallStack callstack, Interpreter interpreter) throws EvalError {
    BshBinding namespace = callstack.top();
    Object[] args = argumentsNode.getArguments(callstack, interpreter);
    if(args == null) {
      throw new EvalError("Null args in new.", this, callstack);
    } else {
      nameNode.toObject(callstack, interpreter, false);
      Object obj = nameNode.toObject(callstack, interpreter, true);
      Class type = null;
      if(obj instanceof ClassIdentifier) {
        type = ((ClassIdentifier)obj).getTargetClass();
        boolean hasBody = this.jjtGetNumChildren() > 2;
        if(hasBody) {
          BSHBlock body = (BSHBlock)this.jjtGetChild(2);
          return type.isInterface()?this.constructWithInterfaceBody(type, args, body, callstack, interpreter):this.constructWithClassBody(type, args, body, callstack, interpreter);
        } else {
          return this.constructObject(type, args, callstack, interpreter);
        }
      } else {
        throw new EvalError("Unknown class: " + nameNode.text, this, callstack);
      }
    }
  }

  Object constructObject(Class<?> type, Object[] args, CallStack callstack, Interpreter interpreter) throws EvalError {
    Object attemptedCtor;
    if(Interpreter.TRACE) {
      try {
        Interpreter.debug.printf("%s#constructObject(Class<?> type = %s, Object[] args = %s, CallStack callstack = %s, Interpreter interpreter = %s);\n", new Object[]{this.getClass().getSimpleName(), type, Arrays.deepToString(args), callstack, interpreter});
      } catch (Throwable var24) {
        var24.printStackTrace();
        StringBuilder obj = new StringBuilder(76);
        if(args == null) {
          obj.append("<NULL>");
        } else {
          obj.append(args.getClass().getComponentType().getName());
          obj.append(String.format("[%d]{ ", new Object[]{Integer.valueOf(args.length)}));
          Object[] instanceNameSpace = args;
          int ths = args.length;

          for(int className = 0; className < ths; ++className) {
            attemptedCtor = instanceNameSpace[className];
            if(obj.length() != 0) {
              obj.append(", ");
            }

            if(attemptedCtor == null) {
              obj.append("<null>");
            } else {
              obj.append(Dumper.dumpStr(attemptedCtor, 1, 15));
            }
          }

          obj.append(" }");
        }

        String var26 = obj.toString();
        Interpreter.debug.printf("constructObject(Class<?> type = %s, Object[] args = %s, CallStack callstack = %s, Interpreter interpreter = %s);\n", new Object[]{type != null?type:"<null>", var26, callstack != null?callstack:"<null>", interpreter != null?interpreter:"<null>"});
      }
    }

    boolean isGeneratedClass = GeneratedClass.class.isAssignableFrom(type);
    if(isGeneratedClass) {
      Interpreter.debug.printf("ClassGeneratorUtil.registerConstructorContext(callstack = %s, interpreter = %s);", new Object[]{callstack, interpreter});
      ClassGeneratorUtil.registerConstructorContext(callstack, interpreter);
    }

    Object var25 = null;
    attemptedCtor = null;

    try {
      var25 = Reflect.constructObject(type, args);
    } catch (ReflectError var21) {
      Interpreter.addError(var21);
      Constructor var27 = (Constructor)var21.getAttempted();
      if(var25 == null) {
        throw new EvalError(String.format("Cannot create an instance of type:\n  `%s\'\nwith arguments: %s:\n  %s\nCalled from: %s\n", new Object[]{type.getName(), Arrays.toString(Types.getTypes(args)), var21.getMessage(), callstack.top()}), this, callstack, var21);
      }
    } catch (InvocationTargetException var22) {
      InvocationTargetException var28 = var22;
      Interpreter.addError(var22);
      Throwable var29 = var22.getTargetException();
      if(var29 != null) {
        Interpreter.addError(var29);
      }

      TargetError var33 = null;

      try {
        ByteArrayOutputStream e2 = new ByteArrayOutputStream();
        PrintStream bufPs = new PrintStream(e2);
        var28.printStackTrace(bufPs);
        bufPs.flush();
        e2.flush();
        IOUtils.closeQuietly(bufPs);
        IOUtils.closeQuietly(e2);
        String dump = new String(e2.toByteArray());
        Interpreter.debug("The constructor threw an exception:\n");
        Interpreter.debug(dump);
        if(Interpreter.DEBUG) {
          var28.printStackTrace(Interpreter.debug);
        }

        Object var31 = var28.getTargetException();
        if(var31 != null) {
          e2 = new ByteArrayOutputStream();
          bufPs = new PrintStream(e2);
          ((Throwable)var31).printStackTrace(bufPs);
          bufPs.flush();
          e2.flush();
          IOUtils.closeQuietly(bufPs);
          IOUtils.closeQuietly(e2);
          dump = new String(e2.toByteArray());
          Interpreter.debug("The target exception was:\n");
          Interpreter.debug(dump);
          if(Interpreter.DEBUG) {
            ((Throwable)var31).printStackTrace(Interpreter.debug);
          }
        } else {
          var31 = var28;
        }

        var33 = new TargetError(String.format("Invalid constructor invocation: %s(%s): \n  %s: \n    %s", new Object[]{type.getName(), Arrays.toString(args).replaceAll("^\\[(.*)\\]$", "$1"), var31.getClass().getSimpleName(), ((Throwable)var31).getMessage()}), (Throwable)var31, this, callstack, true);
        var33.context = (GenericDeclaration)attemptedCtor;
        var33.args = args;
        var33.throwable = var28;
      } catch (Throwable var20) {
        var20.printStackTrace();
      }

      if(var33 != null) {
        throw var33;
      }
    } finally {
      if(isGeneratedClass) {
        if(Interpreter.DEBUG) {
          System.err.println("ClassGeneratorUtil.registerConstructorContext(null, null);");
        }

        ClassGeneratorUtil.registerConstructorContext((CallStack)null, (Interpreter)null);
      }

    }

    if(isGeneratedClass) {
      String var30 = type.getName();
      if(var30.indexOf("$") == -1) {
        return var25;
      }

      This var34 = callstack.top().getThis((Interpreter)null);
      BshBinding var32 = Name.getClassNameSpace(var34.getNameSpace());
      if(var32 != null && var30.startsWith(var32.getName() + "$")) {
        ClassGenerator.getClassGenerator().setInstanceNameSpaceParent(var25, var30, var32);
      }
    }

    return var25;
  }

  private Object constructWithClassBody(Class<?> type, Object[] args, BSHBlock block, CallStack callstack, Interpreter interpreter) throws EvalError {
    if(Interpreter.DEBUG) {
      System.err.printf("%s[%s].constructWithClassBody(Class type = %s, Object[] args = %s, BSHBlock block [%s] = %s, CallStack callstack = %s, Interpreter interpreter = %s)\n", new Object[]{this.getClass().getSimpleName(), Debug.ToString(this), type, Debug.ToString(args), block, Debug.ToString(block), callstack, interpreter});
      System.err.printf("callstack.top() = [%s] %s\n", new Object[]{callstack.top(), Debug.ToString(callstack.top())});
    }

    String name = callstack.top().getName() + "$" + ++innerClassCount;
    Modifiers modifiers = new Modifiers();
    modifiers.addModifier(0, "public");
    Class clas = ClassGenerator.getClassGenerator().generateClass(name, modifiers, (Class[])null, type, block, false, callstack, interpreter);

    try {
      return Reflect.constructObject(clas, args);
    } catch (Exception var11) {
      Object cause = var11;
      if(var11 instanceof InvocationTargetException) {
        cause = ((InvocationTargetException)var11).getTargetException();
      }

      throw new EvalError("Error constructing inner class instance: " + var11, this, callstack, (Throwable)cause);
    }
  }

  private Object constructWithInterfaceBody(Class<?> type, Object[] args, BSHBlock body, CallStack callstack, Interpreter interpreter) throws EvalError {
    if(EMPTY_LIST_CLASS == null) {
      EMPTY_LIST_CLASS =(Class<? extends List>) (Object) DexVisitor.classForName("java.util.Collections$SingletonList");
    }

    BshBinding namespace = callstack.top();
    InstanceId instanceId = new InstanceId(type, body);
    NameSpace local = (NameSpace)Factory.get(BlockNameSpace.class).make(new Object[]{namespace, String.format("AnonymousClass_%s_%d", new Object[]{type.getName(), Integer.valueOf(instanceId.getClassIndex())}), instanceId});
    local.variables.putAll(((NameSpace)callstack.top()).variables);
    if(Interpreter.DEBUG) {
      System.err.println(local.variables.entrySet());
    }

    if(Interpreter.DEBUG) {
      System.err.printf("local = %s [%s]\n", new Object[]{local, Debug.ToString(local)});
    }

    try {
      if(Interpreter.DEBUG) {
        System.err.printf("pushing namespace: %s ...\n", new Object[]{local});
      }

      callstack.push(local);
      if(Interpreter.DEBUG) {
        System.err.println(callstack);
      }

      if(Interpreter.DEBUG) {
        System.err.printf("body{%s}.eval(callstack, interpreter, true)\n", new Object[]{body});
      }

      Object result = body.eval(callstack, interpreter, true);
      if(result instanceof BshMethod) {
        BshMethod md = (BshMethod)result;
        local.setMethod(md);
        String mName = ((BshMethod)result).getName();
        List<BshMethod> mds = (List)local.methods.get(mName);
        if(Interpreter.DEBUG) {
          System.err.printf("mds = %s\n", new Object[]{mds});
        }

        if(mds == null || EMPTY_LIST_CLASS.isInstance(mds)) {
          mds = new ArrayList();
          local.methods.put(mName, mds);
          if(Interpreter.DEBUG) {
            System.err.println(mds.getClass());
          }
        }

        ((List)mds).add(md);
      } else if(Interpreter.DEBUG) {
        System.err.printf("result = %s\n", new Object[]{Debug.ToString(result)});
      }
    } finally {
      if(Interpreter.DEBUG) {
        System.err.printf("popping namespace: %s ...\n", new Object[]{callstack.top()});
      }

      callstack.pop();
      if(Interpreter.DEBUG) {
        System.err.println(callstack);
      }

    }

    local.importStatic(type);
    return local.getThis(interpreter).getInterface(type);
  }

  private Object objectArrayAllocation(BSHAmbiguousName nameNode, BSHArrayDimensions dimensionsNode, CallStack callstack, Interpreter interpreter) throws EvalError {
    BshBinding namespace = callstack.top();
    Class type = nameNode.toClass(callstack, interpreter);
    if(type == null) {
      throw new EvalError("Class " + nameNode.getName(namespace) + " not found.", this, callstack);
    } else {
      return this.arrayAllocation(dimensionsNode, type, callstack, interpreter);
    }
  }

  private Object primitiveArrayAllocation(BSHPrimitiveType typeNode, BSHArrayDimensions dimensionsNode, CallStack callstack, Interpreter interpreter) throws EvalError {
    Class type = typeNode.getType();
    return this.arrayAllocation(dimensionsNode, type, callstack, interpreter);
  }

  private Object arrayAllocation(BSHArrayDimensions dimensionsNode, Class type, CallStack callstack, Interpreter interpreter) throws EvalError {
    Object result = dimensionsNode.eval(type, callstack, interpreter);
    return result != Primitive.VOID?result:this.arrayNewInstance(type, dimensionsNode, callstack);
  }

  private Object arrayNewInstance(Class type, BSHArrayDimensions dimensionsNode, CallStack callstack) throws EvalError {
    if(dimensionsNode.numUndefinedDims > 0) {
      Object e = Array.newInstance(type, new int[dimensionsNode.numUndefinedDims]);
      type = e.getClass();
    }

    try {
      return Array.newInstance(type, dimensionsNode.definedDimensions);
    } catch (NegativeArraySizeException var5) {
      throw new TargetError(var5, this, callstack);
    } catch (Exception var6) {
      throw new EvalError("Can\'t construct primitive array: " + var6.getMessage(), this, callstack);
    }
  }
}