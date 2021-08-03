package org.d6r;

import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;

import javassist.bytecode.SignatureAttribute.*;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.BadBytecode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.googlecode.dex2jar.Method;
import java8.util.Optional;


public class LVInsnTransformer
  implements SelectTransformer<Insn, String>,
             com.google.common.base.Function<Insn, String>, 
             java8.util.function.Function<Insn, String>
{
  public static final LVInsnTransformer INSTANCE 
    = new LVInsnTransformer();
  
  public static final Pattern pkgRegex = Pattern.compile(
    "([^A-Za-ln-z0-9])java\\.(?:lang|util|io|net)\\."
  );
  
  public static String toString(ClassVisitor cv) {
    StringBuilder sb = new StringBuilder(76 * 10);
    sb.append('\n');
    sb.append(dumpMembers.colorize(cv.toString(), "1;32"));
    sb.append(dumpMembers.colorize(" {\n", "0;36"));
    
    for (MethodVisitor mv: cv.getMethods()) {
      sb.append("  ");
      sb.append(toString(mv));
      sb.append('\n');
    }
    
    sb.append(dumpMembers.colorize("}\n", "0;36"));
    return sb.toString();
  }
  
  public static List<Parameter> getParameters(CodeVisitor cv) {
    int expectedNumParams = cv.method.getParameterTypes().length;
    Insn argsInsn = cv.insns[0];
    int[] argRegNos = (
      (Optional<int[]>) (Optional<?>) argsInsn.get("args")
    ).get();
    Parameter[] params = new Parameter[argRegNos.length];
    List<Parameter> paramsRaw = Arrays.asList(params);
    List<Insn> lvInsns = cv.getInsns("LocalVariable");
    
    int paramsOffset = params.length - expectedNumParams;
    
    for (Insn lvInsn: lvInsns) { 
      Optional<Integer> reg = lvInsn.get("reg");
      int paramIndex = reg.isPresent()
        ? ArrayUtils.indexOf(argRegNos, reg.get().intValue())
        : -1;
      if (paramIndex != -1) {
        String name = (String) lvInsn.args[0];
        String desc = (String) lvInsn.args[1];
        String sig =  (String) lvInsn.args[2];
        params[paramIndex] = new Parameter(
          paramIndex, desc, paramsOffset, name, sig
        );
      }     
    }    
    
    for (int i=paramsOffset, len=params.length; i<len; ++i) {
      if (params[i] == null) {
        String typeDesc = cv.method.getParameterTypes()[i - paramsOffset];
        params[i] = new Parameter(
          i,
          typeDesc,
          paramsOffset,
          String.format("p%d", i),
          typeDesc
        );
      }
    }  
    return paramsRaw.subList(paramsOffset, params.length);
  }
  
  
  public static class Parameter {
    public final int index;
    public final String desc;
    public final int indexOffset;
    public final String name;
    public final String sig;
    public final String typeName;
    public Parameter(int index, String desc, int indexOffset, String name,
    String sig)
    {
      this.index = index;
      this.desc = desc;
      this.indexOffset = indexOffset;
      this.name = name;
      this.sig = sig;
      this.typeName = getTypeName(desc, sig);      
    }
    
    public static String getTypeName(String paramTypeDesc, 
    String paramSignature)
    {
      String clsSigStr;
      if (paramSignature == null) {
        clsSigStr = ClassInfo.typeToName(paramTypeDesc, "void");
      } else if (paramSignature.charAt(0) == 'L') {
        try {
          ObjectType clsSig
            = SignatureAttribute.toFieldSignature(paramSignature);
          clsSigStr = clsSig.toString();
        } catch (BadBytecode bbEx) {
          clsSigStr = paramSignature;
        }
      } else if (paramSignature.charAt(0) == 'T') {
        clsSigStr = paramSignature.substring(1, paramSignature.indexOf(';'));
      } else {
        clsSigStr = paramSignature;
      }
      return clsSigStr;
    }
  }
  
  
  public static String toString(MethodVisitor mv) {
    List<Parameter> params = getParameters(mv.getCode());
    List<String> paramStrings = new ArrayList<>(params.size());
    for (Parameter param: params) {
      paramStrings.add(
        String.format(
          "%s %s",
          dumpMembers.colorize(param.typeName, "1;32"), 
          param.name
        )
      );
    }
    
    Method method = mv.method;
    String name = mv.getName();
    int numParams = params.size();
    
    String methodStr = String.format(
      "%s %c[1;33m%s%c[0m(%s)",
       dumpMembers.colorize(DexVisitor.typeToName(
         method.getReturnType(), "void"
       ), "1;36"), 
       0x1b, 
       name, 
       0x1b, 
       StringUtils.join(paramStrings, ", ")
    ); 
    return pkgRegex.matcher(methodStr).replaceAll("$1");
  }
  
  @Override
  public String transform(Insn insn) {
    String paramName = (String) insn.args[0];
    String paramTypeDesc = (String) insn.args[1];
    String paramSignature = (String) insn.args[2];
    
    String clsSigStr;
    
    if (paramSignature == null) { 
      clsSigStr 
        = DexVisitor.typeToName(paramTypeDesc, "void");
    } else if (paramSignature.charAt(0) == 'L') {
      try {
        /** /
        ClassSignature clsSig;
        clsSig = SignatureAttribute.toClassSignature(
          paramSignature);
        /* */
        ObjectType clsSig;
        clsSig = SignatureAttribute.toFieldSignature(
          paramSignature);
        clsSigStr = clsSig.toString();
        /*
        if (clsSigStr.indexOf("<>") == 0
        && clsSig instanceof ClassType) 
        {
          clsSigStr = ((ClassType) clsSig).getSuperClass().toString();
        }
        */
      } catch (BadBytecode bbEx) {
        if ("true".equals(System.getProperty("printStackTrace"))) bbEx.printStackTrace();
        clsSigStr = paramSignature;
      }
      
    } else if (paramSignature.charAt(0) == 'T') {
      clsSigStr = paramSignature.substring(
        1, paramSignature.indexOf(';'));
    } else {
      clsSigStr = paramSignature;
    }
    return String.format(
      "%s %s",
      dumpMembers.colorize(clsSigStr, "1;32"), 
      paramName != null? paramName: "var"
    );
  }
  
  @Override
  public List<String> select(Iterable<? extends Insn> insns) {
    ArrayList<String> paramStrs = new ArrayList<String>();
    
    for (Insn insn: insns) {
      try {
        paramStrs.add(transform(insn));
      } catch (Throwable ex) { }
    }
    return paramStrs;
  }
  
  
  @Override
  public String apply(Insn insn) {
    return transform(insn);
  }
  
}
