package bsh;

import bsh.BSHBlock;
import bsh.BSHClassDeclaration;
import bsh.BSHFormalParameters;
import bsh.BSHMethodDeclaration;
import bsh.BSHReturnType;
import bsh.BSHTypedVariableDeclaration;
import bsh.BSHVariableDeclarator;
import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.CallStack;
import bsh.Capabilities;
import bsh.ClassGeneratorUtil;
import bsh.DelayedEvalBshMethod;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Modifiers;
import bsh.NameSpace;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.SimpleNode;
import bsh.This;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.Variable;
import bsh.classpath.AndroidClassLoader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public final class ClassGenerator {
  private static ClassGenerator cg;
  private static final String DEBUG_DIR = System.getProperty("bsh.debugClasses");
  public static transient AndroidClassLoader acl = null;

  public static ClassGenerator getClassGenerator() {
    if(cg == null) {
      cg = new ClassGenerator();
    }

    return cg;
  }

  public Class generateClass(String name, Modifiers modifiers, Class[] interfaces, Class superClass, BSHBlock block, boolean isInterface, CallStack callstack, Interpreter interpreter) throws EvalError {
    return generateClassImpl(name, modifiers, interfaces, superClass, block, isInterface, callstack, interpreter);
  }

  public Object invokeSuperclassMethod(BshClassManager bcm, Object instance, String methodName, Object[] args) throws UtilEvalError, ReflectError, InvocationTargetException {
    return invokeSuperclassMethodImpl(bcm, instance, methodName, args);
  }

  public void setInstanceNameSpaceParent(Object instance, String className, BshBinding parent) {
    This ithis = ClassGeneratorUtil.getClassInstanceThis(instance, className);
    ithis.getNameSpace().setParent(parent);
  }

  public static Class generateClassImpl(String name, Modifiers modifiers, Class[] interfaces, Class superClass, BSHBlock block, boolean isInterface, CallStack callstack, Interpreter interpreter) throws EvalError {
    try {
      Capabilities.setAccessibility(true);
    } catch (Capabilities.Unavailable var28) {
      throw new EvalError("Defining classes currently requires reflective Accessibility.", block, callstack);
    }

    BshBinding enclosingNameSpace = callstack.top();
    String packageName = enclosingNameSpace.getPackage();
    String className = enclosingNameSpace.isClass()?enclosingNameSpace.getName() + "$" + name:name;
    String fqClassName = packageName != null && packageName.length() != 0?packageName + "." + className:className;
    BshClassManager bcm = interpreter.getClassManager();
    bcm.definingClass(fqClassName);
    BshBinding classStaticNameSpace = Factory.get(NameSpace.class).make(new Object[]{enclosingNameSpace, className});
    classStaticNameSpace.setIsClass(true);
    callstack.push(classStaticNameSpace);
    block.evalBlock(callstack, interpreter, true, ClassGenerator.ClassNodeFilter.CLASSCLASSES);
    Variable[] variables = getDeclaredVariables(block, callstack, interpreter, packageName);
    DelayedEvalBshMethod[] methods = getDeclaredMethods(block, callstack, interpreter, packageName);
    ClassGeneratorUtil classGenerator = new ClassGeneratorUtil(modifiers, className, packageName, superClass, interfaces, variables, methods, classStaticNameSpace, isInterface);
    byte[] code = classGenerator.generateClass();
    if(DEBUG_DIR != null) {
      try {
        FileOutputStream genClass = new FileOutputStream(DEBUG_DIR + '/' + className + ".class");
        genClass.write(code);
        genClass.close();
      } catch (IOException var27) {
        throw new IllegalStateException("cannot create file " + DEBUG_DIR + '/' + className + ".class", var27);
      }
    }

    try {
      if(acl == null) {
        acl = new AndroidClassLoader(new URL[]{new URL("file:///storage/extSdCard/_projects/sdk/jadx/jadx-core/lib/rt.jar")}, AndroidClassLoader.class.getClassLoader());
      }
    } catch (Exception var29) {
      throw new RuntimeException(var29);
    }

    Class genClass1 = acl.defineClass(fqClassName, code);
    enclosingNameSpace.cacheClass(fqClassName.replace('$', '.'), genClass1);
    enclosingNameSpace.cacheClass(fqClassName, genClass1);

    try {
      classStaticNameSpace.setLocalVariable("_bshInstanceInitializer", block, false);
    } catch (UtilEvalError var26) {
      throw new InterpreterError("unable to init static: " + var26);
    }

    classStaticNameSpace.setClassStatic(genClass1);
    block.evalBlock(callstack, interpreter, true, ClassGenerator.ClassNodeFilter.CLASSSTATIC);
    callstack.pop();

    try {
      if(!genClass1.isInterface()) {
        String e = "_bshStatic" + className;

        try {
          LHS e31 = Reflect.getLHSStaticField(genClass1, e);
          e31.assign(classStaticNameSpace.getThis(interpreter), false);
        } catch (Exception var24) {
          ;
        }
      }
    } catch (Throwable var25) {
      var25.printStackTrace();

      try {
        String e3 = "_bshStatic" + className;

        try {
          LHS lhs = Reflect.getLHSStaticField(genClass1, e3);
          lhs.assign(classStaticNameSpace.getThis(interpreter), false);
        } catch (Exception var22) {
          ;
        }
      } catch (Throwable var23) {
        var23.printStackTrace();
      }
    }

    bcm.doneDefiningClass(fqClassName);
    bcm.cacheClassInfo(fqClassName, genClass1);
    return genClass1;
  }

  static Variable[] getDeclaredVariables(BSHBlock body, CallStack callstack, Interpreter interpreter, String defaultPackage) {
    ArrayList vars = new ArrayList();

    for(int child = 0; child < body.jjtGetNumChildren(); ++child) {
      SimpleNode node = (SimpleNode)body.jjtGetChild(child);
      if(node instanceof BSHTypedVariableDeclaration) {
        BSHTypedVariableDeclaration tvd = (BSHTypedVariableDeclaration)node;
        Modifiers modifiers = tvd.modifiers;
        String type = tvd.getTypeDescriptor(callstack, interpreter, defaultPackage);
        BSHVariableDeclarator[] vardec = tvd.getDeclarators();
        BSHVariableDeclarator[] var14 = vardec;
        int var13 = vardec.length;

        for(int var12 = 0; var12 < var13; ++var12) {
          BSHVariableDeclarator aVardec = var14[var12];
          String name = aVardec.name;

          try {
            Variable var = new Variable(name, type, (Object)null, modifiers);
            vars.add(var);
          } catch (UtilEvalError var17) {
            ;
          }
        }
      }
    }

    return (Variable[])vars.toArray(new Variable[vars.size()]);
  }

  static DelayedEvalBshMethod[] getDeclaredMethods(BSHBlock body, CallStack callstack, Interpreter interpreter, String defaultPackage) throws EvalError {
    ArrayList methods = new ArrayList();

    for(int child = 0; child < body.jjtGetNumChildren(); ++child) {
      SimpleNode node = (SimpleNode)body.jjtGetChild(child);
      if(node instanceof BSHMethodDeclaration) {
        BSHMethodDeclaration md = (BSHMethodDeclaration)node;
        md.insureNodesParsed();
        Modifiers modifiers = md.modifiers;
        String name = md.name;
        String returnType = md.getReturnTypeDescriptor(callstack, interpreter, defaultPackage);
        BSHReturnType returnTypeNode = md.getReturnTypeNode();
        BSHFormalParameters paramTypesNode = md.paramsNode;
        String[] paramTypes = paramTypesNode.getTypeDescriptors(callstack, interpreter, defaultPackage);
        DelayedEvalBshMethod bm = new DelayedEvalBshMethod(name, returnType, returnTypeNode, md.paramsNode.getParamNames(), paramTypes, paramTypesNode, md.blockNode, (BshBinding)null, modifiers, callstack, interpreter);
        methods.add(bm);
      }
    }

    return (DelayedEvalBshMethod[])methods.toArray(new DelayedEvalBshMethod[methods.size()]);
  }

  public static Object invokeSuperclassMethodImpl(BshClassManager bcm, Object instance, String methodName, Object[] args) throws UtilEvalError, ReflectError, InvocationTargetException {
    String superName = "_bshSuper" + methodName;
    Class clas = instance.getClass();
    Member superMethod = Reflect.resolveJavaMethod(bcm, clas, superName, Types.getTypes(args), false);
    if(superMethod != null) {
      return Reflect.invokeMethod((Method)superMethod, instance, args);
    } else {
      Class superClass = clas.getSuperclass();
      superMethod = Reflect.resolveExpectedJavaMethod(bcm, superClass, instance, methodName, args, false);
      if(superMethod != null) {
        return Reflect.invokeMethod((Method)superMethod, instance, args);
      } else {
        RuntimeException re = new RuntimeException(String.format("\nClassGeneratorUtil.invokeSuperclassMethodImpl(\n    BshClassManager bcm = %s,\n    Object instance = %s,\n    String methodName = %s,\n    Object[] args = %s\n): <<ERROR>>: No superMethod returned \n  by Reflect.resolveJavaMethod \n  OR Reflect.resolveExpectedJavaMethod! \nClass clas    = %s\nClass superClass = %s\n", new Object[]{bcm, instance, methodName, args != null?Arrays.asList(args):"<NULL>", clas, superClass}));
        re.printStackTrace();
        throw re;
      }
    }
  }

  static class ClassNodeFilter implements BSHBlock.NodeFilter {
    public static final int STATIC = 0;
    public static final int INSTANCE = 1;
    public static final int CLASSES = 2;
    public static ClassGenerator.ClassNodeFilter CLASSSTATIC = new ClassGenerator.ClassNodeFilter(0);
    public static ClassGenerator.ClassNodeFilter CLASSINSTANCE = new ClassGenerator.ClassNodeFilter(1);
    public static ClassGenerator.ClassNodeFilter CLASSCLASSES = new ClassGenerator.ClassNodeFilter(2);
    int context;

    private ClassNodeFilter(int context) {
      this.context = context;
    }

    public boolean isVisible(SimpleNode node) {
      return this.context == 2?node instanceof BSHClassDeclaration:(node instanceof BSHClassDeclaration?false:(this.context == 0?this.isStatic(node):(this.context == 1?!this.isStatic(node):true)));
    }

    boolean isStatic(SimpleNode node) {
      return node instanceof BSHTypedVariableDeclaration?((BSHTypedVariableDeclaration)node).modifiers != null && ((BSHTypedVariableDeclaration)node).modifiers.hasModifier("static"):(node instanceof BSHMethodDeclaration?((BSHMethodDeclaration)node).modifiers != null && ((BSHMethodDeclaration)node).modifiers.hasModifier("static"):(node instanceof BSHBlock?false:false));
    }
  }
}
