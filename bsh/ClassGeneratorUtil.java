package bsh;

import bsh.BSHAmbiguousName;
import bsh.BSHArguments;
import bsh.BSHBlock;
import bsh.BSHMethodInvocation;
import bsh.BSHPrimaryExpression;
import bsh.BSHType;
import bsh.BshBinding;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.ClassGenerator;
import bsh.DelayedEvalBshMethod;
import bsh.EvalError;
import bsh.Factory;
import bsh.GeneratedClass;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Modifiers;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.Reflect;
import bsh.SimpleNode;
import bsh.TargetError;
import bsh.This;
import bsh.Types;
import bsh.UtilEvalError;
import bsh.Variable;
import bsh.org.objectweb.asm.ClassWriter;
import bsh.org.objectweb.asm.CodeVisitor;
import bsh.org.objectweb.asm.Constants;
import bsh.org.objectweb.asm.Label;
import bsh.org.objectweb.asm.Type;
// import bsh.org.objectweb.asm.ByteVector;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.d6r.HexDump;
import org.d6r.ClassInfo;
import java.util.Arrays;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflect.setfldval;

public class ClassGeneratorUtil implements Constants {
  static final String BSHSTATIC = "_bshStatic";
  private static final String BSHTHIS = "_bshThis";
  static final String BSHSUPER = "_bshSuper";
  static final String BSHINIT = "_bshInstanceInitializer";
  private static final String BSHCONSTRUCTORS = "_bshConstructors";
  private static final int DEFAULTCONSTRUCTOR = -1;
  private static final String OBJECT = "Ljava/lang/Object;";
  private final String className;
  private final String origClassName;
  private final String fqClassName;
  private final Class<?> superClass;
  private final String superClassName;
  private final Class<?>[] interfaces;
  private final Variable[] vars;
  private final Constructor<?>[] superConstructors;
  private final DelayedEvalBshMethod[] constructors;
  private final DelayedEvalBshMethod[] methods;
  private final BshBinding classStaticNameSpace;
  private final Modifiers classModifiers;
  private boolean isInterface;
  private static final ThreadLocal<BshBinding> CONTEXT_NAMESPACE = new ThreadLocal();
  private static final ThreadLocal<Interpreter> CONTEXT_INTERPRETER = new ThreadLocal();

  public ClassGeneratorUtil(Modifiers classModifiers, String className, String packageName, Class<?> superClass, Class<?>[] interfaces, Variable[] vars, DelayedEvalBshMethod[] bshmethods, BshBinding classStaticNameSpace, boolean isInterface) {//;
    this.classModifiers = classModifiers;
    this.origClassName = className;
    System.err.printf("this.origClassName == \"%s\"\n", origClassName);
    if (origClassName.indexOf('/') != -1) {
      className = StringUtils.substringAfterLast(origClassName, "/");
      System.err.printf("className == \"%s\"\n", className);
      packageName = StringUtils.substringBeforeLast(origClassName, "/")
        .replace('/', '.');
      System.err.printf("packageName == \"%s\"\n", packageName);
    } else {
      className = (origClassName.indexOf('.') != -1)
        ? StringUtils.substringAfterLast(origClassName, ".")
        : origClassName;
      System.err.printf("className == \"%s\"\n", className);
    }
    this.className = className;
    System.err.printf("className == \"%s\"\n", className);
    System.err.printf("packageName == \"%s\"\n", packageName);
    
    String fq;
    if (packageName != null) {
      fq = (packageName.replace('.', '/') + "/" + className).replaceAll("^/+", "");
    } else {
      fq = className;
    }
    
    System.err.printf("this.fqClassName == \"%s\"\n", fq);
    this.fqClassName = fq;
    if(superClass == null) {
      superClass = Object.class;
    }
    
    this.superClass = superClass;
    System.err.printf("this.superClassName == \"%s\"\n", new Object[]{this.superClassName = Type.getInternalName(superClass)});
    if(interfaces == null) {
      interfaces = new Class[0];
    }

    this.interfaces = interfaces;
    this.vars = vars;
    this.classStaticNameSpace = classStaticNameSpace;
    this.superConstructors = superClass.getDeclaredConstructors();
    ArrayList consl = new ArrayList();
    ArrayList methodsl = new ArrayList();
    String classBaseName = getBaseName(className);
    DelayedEvalBshMethod[] var17 = bshmethods;
    int var16 = bshmethods.length;

    for(int var15 = 0; var15 < var16; ++var15) {
      DelayedEvalBshMethod e = var17[var15];
      if(e.getName().equals(classBaseName)) {
        consl.add(e);
      } else {
        methodsl.add(e);
      }
    }

    this.constructors = (DelayedEvalBshMethod[])consl.toArray(new DelayedEvalBshMethod[0]);
    this.methods = (DelayedEvalBshMethod[])methodsl.toArray(new DelayedEvalBshMethod[0]);

    try {
      classStaticNameSpace.setLocalVariable("_bshConstructors", this.constructors, false);
    } catch (UtilEvalError var18) {
      throw new InterpreterError("can\'t set cons var");
    }

    this.isInterface = isInterface;
  }

  public byte[] generateClass() {
    int classMods = getASMModifiers(this.classModifiers) | 1;
    if(this.isInterface) {
      classMods |= 512;
    }

    String[] interfaceNames = new String[this.interfaces.length + (this.isInterface?0:1)];

    for(int sourceFile = 0; sourceFile < this.interfaces.length; ++sourceFile) {
      interfaceNames[sourceFile] = Type.getInternalName(this.interfaces[sourceFile]);
    }

    if(!this.isInterface) {
      interfaceNames[this.interfaces.length] = Type.getInternalName(GeneratedClass.class);
    }

    System.err.printf("Class Innfo:  \n  classMods=%d, \n  fqClassName=%s, \n  superClassName=%s, \n  interfaceNames=%s, \n  ", new Object[]{Integer.valueOf(classMods), this.fqClassName, this.superClassName, interfaceNames});
    String var14 = "BeanShell Generated via ASM (www.objectweb.org)";
    ClassWriter cw = new ClassWriter(false);
    cw.visit(classMods, this.fqClassName, this.superClassName, interfaceNames, var14);
    System.err.printf("Class Innfo:  \n  classMods=%d, \n  fqClassName=%s, \n  superClassName=%s, \n  interfaceNames=%s, \n  sourceFile=%s\n", new Object[]{Integer.valueOf(classMods), this.fqClassName, this.superClassName, interfaceNames, var14});
    if(!this.isInterface) {
      generateField("_bshThis" + this.className, "Lbsh/This;", 1, cw);
      generateField("_bshStatic" + this.className, "Lbsh/This;", 9, cw);
    }

    Variable[] var8 = this.vars;
    int modifiers = this.vars.length;

    int method;
    for(method = 0; method < modifiers; ++method) {
      Variable hasConstructor = var8[method];
      String type = hasConstructor.getTypeDescriptor();
      System.err.println(type);
      if(!hasConstructor.hasModifier("private") && type != null) {
        int returnType;
        if(this.isInterface) {
          returnType = 25;
        } else {
          returnType = getASMModifiers(hasConstructor.getModifiers());
        }

        System.err.printf("var.getName() == \"%s\"\n", new Object[]{hasConstructor.getName()});
        generateField(hasConstructor.getName(), type, returnType, cw);
      }
    }

    boolean var15 = false;

    for(method = 0; method < this.constructors.length; ++method) {
      if(!this.constructors[method].hasModifier("private")) {
        modifiers = getASMModifiers(this.constructors[method].getModifiers());
        this.generateConstructor(method, this.constructors[method].getParamTypeDescriptors(), modifiers, cw);
        var15 = true;
      }
    }

    if(!this.isInterface && !var15) {
      this.generateConstructor(-1, new String[0], 1, cw);
    }

    DelayedEvalBshMethod[] var18 = this.methods;
    int var17 = this.methods.length;

    for(modifiers = 0; modifiers < var17; ++modifiers) {
      DelayedEvalBshMethod var16 = var18[modifiers];
      String var19 = var16.getReturnTypeDescriptor();
      if(!var16.hasModifier("private") && var19 != null) {
        int modifiers1 = getASMModifiers(var16.getModifiers());
        if(this.isInterface) {
          modifiers1 |= 1025;
        }

        generateMethod(this.className, this.fqClassName, var16.getName(), var19, var16.getParamTypeDescriptors(), modifiers1, cw);
        boolean isStatic = (modifiers1 & 8) > 0;
        boolean overridden = this.classContainsMethod(this.superClass, var16.getName(), var16.getParamTypeDescriptors());
        if(!isStatic && overridden) {
          generateSuperDelegateMethod(this.superClassName, var16.getName(), var19, var16.getParamTypeDescriptors(), modifiers1, cw);
        }
      }
    }

    return cw.toByteArray();
  }
  
  private static int getASMModifiers(Modifiers modifiers) {
    int mods = 0;
    if(modifiers == null) {
      return mods;
    } else {
      if(modifiers.hasModifier("public")) {
        ++mods;
      }

      if(modifiers.hasModifier("protected")) {
        mods += 4;
      }

      if(modifiers.hasModifier("static")) {
        mods += 8;
      }

      if(modifiers.hasModifier("synchronized")) {
        mods += 32;
      }

      if(modifiers.hasModifier("abstract")) {
        mods += 1024;
      }

      return mods;
    }
  }
  
  private static void generateField(String fieldName, String type, int modifiers, 
  ClassWriter cw)
  {
    Object bv = getfldval(cw, "fields");
    final int start = (bv != null) ? (Integer) getfldval(bv, "length") : 0;
    
    System.err.printf(
      "generateField[pos: %d](" +
        "fieldName: %s, type: %s, modifiers: %d [%s], cw: %s[0x%08x])\n", 
      start, fieldName, type, modifiers, ClassInfo.getModifiers(modifiers),
      cw, System.identityHashCode(cw)
    );
    
    cw.visitField(
      modifiers, fieldName, type, 
      (Object) null // constantValue
    );
    
    if (bv == null) bv = getfldval(cw, "fields");
    final int end = (Integer) getfldval(bv, "length");
    final byte[] data = getfldval(bv, "data");
    final byte[] fieldBytes = Arrays.copyOfRange(data, start, end);
    
    System.err.printf(
      "cw[0x%08x].visitField(modifiers: %d [%s], fieldName: %s, type: %s, null) " +
      "wrote %d bytes of data to cw.fields. length: %d -> %d\n" +
      "%s\n",
      System.identityHashCode(cw), modifiers, ClassInfo.getModifiers(modifiers),
      fieldName, type, 
      (end - start), start, end,
      HexDump.dump(fieldBytes)
    );
  }
  
  private static void generateMethod(String className, String fqClassName,
  String methodName, String returnType, String[] paramTypes, int modifiers,
  ClassWriter cw)
  {
    Object exceptions = null;
    boolean isStatic = (modifiers & 8) != 0;
    if(returnType == null) {
      returnType = "Ljava/lang/Object;";
    }
    
    String methodDescriptor = getMethodDescriptor(returnType, paramTypes);
    System.err.printf(" methodDescriptor == \"%s\"\n", new Object[]{methodDescriptor});
    CodeVisitor cv = cw.visitMethod(
      modifiers, methodName, methodDescriptor, (String[])exceptions
    );
    if ((modifiers & 1024) == 0) {
      if(isStatic) {
        cv.visitFieldInsn(
          178, fqClassName, "_bshStatic" + className, "Lbsh/This;");
        System.err.printf(
          "cv.visitFieldInsn(178, fqClassName: %s, " +
          "\"_bshStatic\" + className: %s, \"Lbsh/This;\"",
          fqClassName, "_bshStatic" + className
        );
      } else {
        cv.visitVarInsn(25, 0);
        cv.visitFieldInsn(
          180, fqClassName, "_bshThis" + className, "Lbsh/This;");
        System.err.printf(
          "cv.visitFieldInsn(180, fqClassName: %s, " +
          "\"_bshThis\" + className: %s, \"Lbsh/This;\"",
          fqClassName, "_bshThis" + className
        );
      }
      cv.visitLdcInsn(methodName);
      generateParameterReifierCode(paramTypes, isStatic, cv);
      cv.visitInsn(1);
      cv.visitInsn(1);
      cv.visitInsn(1);
      cv.visitInsn(4);
      cv.visitMethodInsn(182, "bsh/This", "invokeMethod", Type.getMethodDescriptor(Type.getType(Object.class), new Type[]{Type.getType(String.class), Type.getType(Object[].class), Type.getType(Interpreter.class), Type.getType(CallStack.class), Type.getType(SimpleNode.class), Type.getType(Boolean.TYPE)}));
      cv.visitMethodInsn(184, "bsh/Primitive", "unwrap", "(Ljava/lang/Object;)Ljava/lang/Object;");
      generateReturnCode(returnType, cv);
      cv.visitMaxs(20, 20);
    }
  }

  void generateConstructor(int index, String[] paramTypes, int modifiers, ClassWriter cw) {
    int argsVar = paramTypes.length + 1;
    int consArgsVar = paramTypes.length + 2;
    Object exceptions = null;
    String methodDescriptor = getMethodDescriptor("V", paramTypes);
    CodeVisitor cv = cw.visitMethod(modifiers, "<init>", methodDescriptor, (String[])exceptions);
    generateParameterReifierCode(paramTypes, false, cv);
    cv.visitVarInsn(58, argsVar);
    this.generateConstructorSwitch(index, argsVar, consArgsVar, cv);
    cv.visitVarInsn(25, 0);
    cv.visitLdcInsn(this.className);
    cv.visitVarInsn(25, argsVar);
    System.err.println(GeneratedClass.class.getName());
    System.err.println(GeneratedClass.class.getName().replace('.', '/'));
    cv.visitMethodInsn(184, "bsh/ClassGeneratorUtil", "initInstance", "(L" + GeneratedClass.class.getName().replace('.', '/') + ";Ljava/lang/String;[Ljava/lang/Object;)V");
    cv.visitInsn(177);
    cv.visitMaxs(20, 20);
  }

  void generateConstructorSwitch(int consIndex, int argsVar, int consArgsVar, CodeVisitor cv) {
    Label defaultLabel = new Label();
    Label endLabel = new Label();
    int cases = this.superConstructors.length + this.constructors.length;
    Label[] labels = new Label[cases];

    int index;
    for(index = 0; index < cases; ++index) {
      labels[index] = new Label();
    }

    cv.visitLdcInsn(this.superClass.getName());
    cv.visitFieldInsn(178, this.fqClassName, "_bshStatic" + this.className, "Lbsh/This;");
    cv.visitVarInsn(25, argsVar);
    cv.visitIntInsn(16, consIndex);
    cv.visitMethodInsn(184, "bsh/ClassGeneratorUtil", "getConstructorArgs", "(Ljava/lang/String;Lbsh/This;[Ljava/lang/Object;I)Lbsh/ClassGeneratorUtil$ConstructorArgs;");
    cv.visitVarInsn(58, consArgsVar);
    cv.visitVarInsn(25, consArgsVar);
    cv.visitFieldInsn(180, "bsh/ClassGeneratorUtil$ConstructorArgs", "selector", "I");
    cv.visitTableSwitchInsn(0, cases - 1, defaultLabel, labels);
    index = 0;

    int i;
    for(i = 0; i < this.superConstructors.length; ++index) {
      doSwitchBranch(index, this.superClassName, getTypeDescriptors(this.superConstructors[i].getParameterTypes()), endLabel, labels, consArgsVar, cv);
      ++i;
    }

    for(i = 0; i < this.constructors.length; ++index) {
      doSwitchBranch(index, this.fqClassName, this.constructors[i].getParamTypeDescriptors(), endLabel, labels, consArgsVar, cv);
      ++i;
    }

    cv.visitLabel(defaultLabel);
    cv.visitVarInsn(25, 0);
    cv.visitMethodInsn(183, this.superClassName, "<init>", "()V");
    cv.visitLabel(endLabel);
  }

  private static void doSwitchBranch(int index, String targetClassName, String[] paramTypes, Label endLabel, Label[] labels, int consArgsVar, CodeVisitor cv) {
    cv.visitLabel(labels[index]);
    cv.visitVarInsn(25, 0);
    String[] var10 = paramTypes;
    int var9 = paramTypes.length;

    String descriptor;
    for(int var8 = 0; var8 < var9; ++var8) {
      descriptor = var10[var8];
      String method;
      if(descriptor.equals("Z")) {
        method = "getBoolean";
      } else if(descriptor.equals("B")) {
        method = "getByte";
      } else if(descriptor.equals("C")) {
        method = "getChar";
      } else if(descriptor.equals("S")) {
        method = "getShort";
      } else if(descriptor.equals("I")) {
        method = "getInt";
      } else if(descriptor.equals("J")) {
        method = "getLong";
      } else if(descriptor.equals("D")) {
        method = "getDouble";
      } else if(descriptor.equals("F")) {
        method = "getFloat";
      } else {
        method = "getObject";
      }

      cv.visitVarInsn(25, consArgsVar);
      String className = "bsh/ClassGeneratorUtil$ConstructorArgs";
      String retType;
      if(method.equals("getObject")) {
        retType = "Ljava/lang/Object;";
      } else {
        retType = descriptor;
      }

      cv.visitMethodInsn(182, className, method, "()" + retType);
      if(method.equals("getObject")) {
        cv.visitTypeInsn(192, descriptorToClassName(descriptor));
      }
    }

    descriptor = getMethodDescriptor("V", paramTypes);
    cv.visitMethodInsn(183, targetClassName, "<init>", descriptor);
    cv.visitJumpInsn(167, endLabel);
  }

  private static String getMethodDescriptor(String returnType, String[] paramTypes) {
    StringBuilder sb = new StringBuilder("(");
    String[] var6 = paramTypes;
    int var5 = paramTypes.length;

    for(int var4 = 0; var4 < var5; ++var4) {
      String paramType = var6[var4];
      sb.append(paramType);
    }

    sb.append(')').append(returnType);
    return sb.toString();
  }

  private static void generateSuperDelegateMethod(String superClassName, String methodName, String returnType, String[] paramTypes, int modifiers, ClassWriter cw) {
    Object exceptions = null;
    if(returnType == null) {
      returnType = "Ljava/lang/Object;";
    }

    String methodDescriptor = getMethodDescriptor(returnType, paramTypes);
    CodeVisitor cv = cw.visitMethod(modifiers, "_bshSuper" + methodName, methodDescriptor, (String[])exceptions);
    cv.visitVarInsn(25, 0);
    int localVarIndex = 1;
    String[] var13 = paramTypes;
    int var12 = paramTypes.length;

    for(int var11 = 0; var11 < var12; ++var11) {
      String paramType = var13[var11];
      if(isPrimitive(paramType)) {
        cv.visitVarInsn(21, localVarIndex);
      } else {
        cv.visitVarInsn(25, localVarIndex);
      }

      localVarIndex += !paramType.equals("D") && !paramType.equals("J")?1:2;
    }

    cv.visitMethodInsn(183, superClassName, methodName, methodDescriptor);
    generatePlainReturnCode(returnType, cv);
    cv.visitMaxs(20, 20);
  }

  boolean classContainsMethod(Class clas, String methodName, String[] paramTypes) {
    while(clas != null) {
      Method[] methods = clas.getDeclaredMethods();
      Method[] var8 = methods;
      int var7 = methods.length;

      for(int var6 = 0; var6 < var7; ++var6) {
        Method method = var8[var6];
        if(method.getName().equals(methodName)) {
          String[] methodParamTypes = getTypeDescriptors(method.getParameterTypes());
          boolean found = true;

          for(int j = 0; j < methodParamTypes.length; ++j) {
            if(!paramTypes[j].equals(methodParamTypes[j])) {
              found = false;
              break;
            }
          }

          if(found) {
            return true;
          }
        }
      }

      clas = clas.getSuperclass();
    }

    return false;
  }

  private static void generatePlainReturnCode(String returnType, CodeVisitor cv) {
    if(returnType.equals("V")) {
      cv.visitInsn(177);
    } else if(isPrimitive(returnType)) {
      short opcode = 172;
      if(returnType.equals("D")) {
        opcode = 175;
      } else if(returnType.equals("F")) {
        opcode = 174;
      } else if(returnType.equals("J")) {
        opcode = 173;
      }

      cv.visitInsn(opcode);
    } else {
      cv.visitTypeInsn(192, descriptorToClassName(returnType));
      cv.visitInsn(176);
    }

  }

  private static void generateParameterReifierCode(String[] paramTypes, boolean isStatic, CodeVisitor cv) {
    cv.visitIntInsn(17, paramTypes.length);
    cv.visitTypeInsn(189, "java/lang/Object");
    int localVarIndex = isStatic?0:1;

    for(int i = 0; i < paramTypes.length; ++i) {
      String param = paramTypes[i];
      cv.visitInsn(89);
      cv.visitIntInsn(17, i);
      if(isPrimitive(param)) {
        byte opcode;
        if(param.equals("F")) {
          opcode = 23;
        } else if(param.equals("D")) {
          opcode = 24;
        } else if(param.equals("J")) {
          opcode = 22;
        } else {
          opcode = 21;
        }

        String type = "bsh/Primitive";
        cv.visitTypeInsn(187, type);
        cv.visitInsn(89);
        cv.visitVarInsn(opcode, localVarIndex);
        cv.visitMethodInsn(183, type, "<init>", "(" + param + ")V");
      } else {
        cv.visitVarInsn(25, localVarIndex);
      }

      cv.visitInsn(83);
      localVarIndex += !param.equals("D") && !param.equals("J")?1:2;
    }

  }

  private static void generateReturnCode(String returnType, CodeVisitor cv) {
    if(returnType.equals("V")) {
      cv.visitInsn(87);
      cv.visitInsn(177);
    } else if(isPrimitive(returnType)) {
      short opcode = 172;
      String type;
      String meth;
      if(returnType.equals("B")) {
        type = "java/lang/Byte";
        meth = "byteValue";
      } else if(returnType.equals("I")) {
        type = "java/lang/Integer";
        meth = "intValue";
      } else if(returnType.equals("Z")) {
        type = "java/lang/Boolean";
        meth = "booleanValue";
      } else if(returnType.equals("D")) {
        opcode = 175;
        type = "java/lang/Double";
        meth = "doubleValue";
      } else if(returnType.equals("F")) {
        opcode = 174;
        type = "java/lang/Float";
        meth = "floatValue";
      } else if(returnType.equals("J")) {
        opcode = 173;
        type = "java/lang/Long";
        meth = "longValue";
      } else if(returnType.equals("C")) {
        type = "java/lang/Character";
        meth = "charValue";
      } else {
        type = "java/lang/Short";
        meth = "shortValue";
      }

      cv.visitTypeInsn(192, type);
      cv.visitMethodInsn(182, type, meth, "()" + returnType);
      cv.visitInsn(opcode);
    } else {
      cv.visitTypeInsn(192, descriptorToClassName(returnType));
      cv.visitInsn(176);
    }

  }

  public static ClassGeneratorUtil.ConstructorArgs getConstructorArgs(String superClassName, This classStaticThis, Object[] consArgs, int index) {
    DelayedEvalBshMethod[] constructors;
    try {
      constructors = (DelayedEvalBshMethod[])classStaticThis.getNameSpace().getVariable("_bshConstructors");
    } catch (Exception var24) {
      throw new InterpreterError("unable to get instance initializer: " + var24);
    }

    if(index == -1) {
      return ClassGeneratorUtil.ConstructorArgs.DEFAULT;
    } else {
      DelayedEvalBshMethod constructor = constructors[index];
      if(constructor.methodBody.jjtGetNumChildren() == 0) {
        return ClassGeneratorUtil.ConstructorArgs.DEFAULT;
      } else {
        String altConstructor = null;
        BSHArguments argsNode = null;
        SimpleNode firstStatement = (SimpleNode)constructor.methodBody.jjtGetChild(0);
        if(firstStatement instanceof BSHPrimaryExpression) {
          firstStatement = (SimpleNode)firstStatement.jjtGetChild(0);
        }

        if(firstStatement instanceof BSHMethodInvocation) {
          BSHMethodInvocation consArgsNameSpace = (BSHMethodInvocation)firstStatement;
          BSHAmbiguousName consArgNames = consArgsNameSpace.getNameNode();
          if(consArgNames.text.equals("super") || consArgNames.text.equals("this")) {
            altConstructor = consArgNames.text;
            argsNode = consArgsNameSpace.getArgsNode();
          }
        }

        if(altConstructor == null) {
          return ClassGeneratorUtil.ConstructorArgs.DEFAULT;
        } else {
          BshBinding var25 = Factory.get(NameSpace.class).make(new Object[]{classStaticThis.getNameSpace(), "consArgs"});
          String[] var26 = constructor.getParameterNames();
          Class[] consArgTypes = constructor.getParameterTypes();

          for(int callstack = 0; callstack < consArgs.length; ++callstack) {
            try {
              var25.setTypedVariable(var26[callstack], consArgTypes[callstack], consArgs[callstack], (Modifiers)null);
            } catch (UtilEvalError var23) {
              throw new InterpreterError("err setting local cons arg:" + var23);
            }
          }

          CallStack var27 = new CallStack();
          var27.push(var25);
          Interpreter interpreter = classStaticThis.declaringInterpreter;

          Object[] args;
          try {
            args = argsNode.getArguments(var27, interpreter);
          } catch (EvalError var22) {
            throw new InterpreterError("Error evaluating constructor args: " + var22);
          }

          Class[] argTypes = Types.getTypes(args);
          args = Primitive.unwrap(args);
          Class superClass = interpreter.getClassManager().classForName(superClassName);
          if(superClass == null) {
            throw new InterpreterError("can\'t find superclass: " + superClassName);
          } else {
            Constructor[] superCons = superClass.getDeclaredConstructors();
            if(altConstructor.equals("super")) {
              int var28 = Reflect.findMostSpecificConstructorIndex(argTypes, superCons);
              if(var28 == -1) {
                throw new InterpreterError("can\'t find constructor for args!");
              } else {
                return new ClassGeneratorUtil.ConstructorArgs(var28, args);
              }
            } else {
              Class[][] candidates = new Class[constructors.length][];

              int i;
              for(i = 0; i < candidates.length; ++i) {
                candidates[i] = constructors[i].getParameterTypes();
              }

              i = Reflect.findMostSpecificSignature(argTypes, candidates);
              if(i == -1) {
                throw new InterpreterError("can\'t find constructor for args 2!");
              } else {
                int selector = i + superCons.length;
                int ourSelector = index + superCons.length;
                if(selector == ourSelector) {
                  throw new InterpreterError("Recusive constructor call.");
                } else {
                  return new ClassGeneratorUtil.ConstructorArgs(selector, args);
                }
              }
            }
          }
        }
      }
    }
  }

  static void registerConstructorContext(CallStack callstack, Interpreter interpreter) {
    if(callstack != null) {
      CONTEXT_NAMESPACE.set(callstack.top());
    } else {
      CONTEXT_NAMESPACE.remove();
    }

    if(interpreter != null) {
      CONTEXT_INTERPRETER.set(interpreter);
    } else {
      CONTEXT_INTERPRETER.remove();
    }

  }

  public static void initInstance(GeneratedClass instance, String className, Object[] args) {
    Class[] sig = Types.getTypes(args);
    CallStack callstack = new CallStack();
    This instanceThis = getClassInstanceThis(instance, className);
    Interpreter interpreter;
    Object instanceNameSpace;
    if(instanceThis == null) {
      This constructorName = getClassStaticThis(instance.getClass(), className);
      interpreter = (Interpreter)CONTEXT_INTERPRETER.get();
      if(interpreter == null) {
        interpreter = constructorName.declaringInterpreter;
      }

      BSHBlock e;
      try {
        e = (BSHBlock)constructorName.getNameSpace().getVariable("_bshInstanceInitializer");
      } catch (Exception var13) {
        throw new InterpreterError("unable to get instance initializer: " + var13);
      }

      if(CONTEXT_NAMESPACE.get() != null) {
        instanceNameSpace = constructorName.getNameSpace().copy();
        ((BshBinding)instanceNameSpace).setParent((BshBinding)CONTEXT_NAMESPACE.get());
      } else {
        instanceNameSpace = Factory.get(NameSpace.class).make(new Object[]{constructorName.getNameSpace(), className});
      }

      ((BshBinding)instanceNameSpace).setIsClass(true);
      instanceThis = ((BshBinding)instanceNameSpace).getThis(interpreter);

      try {
        LHS e1 = Reflect.getLHSObjectField(instance, "_bshThis" + className);
        e1.assign(instanceThis, false);
      } catch (Exception var12) {
        throw new InterpreterError("Error in class gen setup: " + var12);
      }

      ((BshBinding)instanceNameSpace).setClassInstance(instance);
      callstack.push((BshBinding)instanceNameSpace);

      try {
        e.evalBlock(callstack, interpreter, true, ClassGenerator.ClassNodeFilter.CLASSINSTANCE);
      } catch (Exception var11) {
        throw new InterpreterError("Error in class initialization: " + var11, var11);
      }

      callstack.pop();
    } else {
      interpreter = instanceThis.declaringInterpreter;
      instanceNameSpace = instanceThis.getNameSpace();
    }

    String constructorName1 = getBaseName(className);

    try {
      BshMethod e3 = ((BshBinding)instanceNameSpace).getMethod(constructorName1, sig, true);
      if(args.length > 0 && e3 == null) {
        throw new InterpreterError("Can\'t find constructor: " + className);
      } else {
        if(e3 != null) {
          e3.invoke(args, interpreter, callstack, (SimpleNode)null, false);
        }

      }
    } catch (Exception var14) {
      Exception e2 = var14;
      if(var14 instanceof TargetError) {
        e2 = (Exception)((TargetError)var14).getTarget();
      }

      if(e2 instanceof InvocationTargetException) {
        e2 = (Exception)((InvocationTargetException)e2).getTargetException();
      }

      throw new InterpreterError("Error in class initialization.", e2);
    }
  }

  private static This getClassStaticThis(Class clas, String className) {
    try {
      return (This)Reflect.getStaticFieldValue(clas, "_bshStatic" + className);
    } catch (Exception var3) {
      throw new InterpreterError("Unable to get class static space: " + var3);
    }
  }

  static This getClassInstanceThis(Object instance, String className) {
    try {
      Object e = Reflect.getObjectFieldValue(instance, "_bshThis" + className);
      return (This)Primitive.unwrap(e);
    } catch (Exception var3) {
      throw new InterpreterError("Generated class: Error getting This" + var3);
    }
  }

  private static boolean isPrimitive(String typeDescriptor) {
    return typeDescriptor.length() == 1;
  }

  private static String[] getTypeDescriptors(Class[] cparams) {
    String[] sa = new String[cparams.length];

    for(int i = 0; i < sa.length; ++i) {
      sa[i] = BSHType.getTypeDescriptor(cparams[i]);
    }

    return sa;
  }

  private static String descriptorToClassName(String s) {
    if(!s.startsWith("[") && s.startsWith("L")) {
      String cn = s.substring(1, s.length() - 1);
      System.err.printf("descriptorToClassName(String s=%s) -> %s\n", new Object[]{s, cn});
      return cn;
    } else {
      return s;
    }
  }

  private static String getBaseName(String className) {
    int i = className.indexOf("$");
    if(i == -1) {
      return className;
    } else {
      String bn = className.substring(i + 1);
      System.err.printf("getBaseName(String className=%s) -> %s\n", new Object[]{className, bn});
      return bn;
    }
  }

  public static class ConstructorArgs {
    public static final ClassGeneratorUtil.ConstructorArgs DEFAULT = new ClassGeneratorUtil.ConstructorArgs();
    public int selector = -1;
    Object[] args;
    int arg;

    ConstructorArgs() {
    }

    ConstructorArgs(int selector, Object[] args) {
      this.selector = selector;
      this.args = args;
    }

    Object next() {
      return this.args[this.arg++];
    }

    public boolean getBoolean() {
      return ((Boolean)this.next()).booleanValue();
    }

    public byte getByte() {
      return ((Byte)this.next()).byteValue();
    }

    public char getChar() {
      return ((Character)this.next()).charValue();
    }

    public short getShort() {
      return ((Short)this.next()).shortValue();
    }

    public int getInt() {
      return ((Integer)this.next()).intValue();
    }

    public long getLong() {
      return ((Long)this.next()).longValue();
    }

    public double getDouble() {
      return ((Double)this.next()).doubleValue();
    }

    public float getFloat() {
      return ((Float)this.next()).floatValue();
    }

    public Object getObject() {
      return this.next();
    }
  }
}