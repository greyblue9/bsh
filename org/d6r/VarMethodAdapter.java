package org.d6r;

import org.d6r.annotation.*;
import org.objectweb.asm.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.MethodBody;
import java.lang.reflect.Modifier;
import java8.util.Optional;
import java.util.*;
import javassist.bytecode.SignatureAttribute;
import org.apache.commons.lang3.tuple.Pair;
import javassist.bytecode.BadBytecode;
import org.d6r.InternalNames;

public class VarMethodAdapter extends MethodVisitor {
   
  public static class VarInfo {
    Label label;
    int opcodeV = -1;
    int var = -1;
    int opcodeT = -1;
    String type = null;
    
    public VarInfo(Label label) {
      this.label = label;
    }
  }
  
  public static List<Pair<VarMethodAdapter, Throwable>> errors
    = new LinkedList<Pair<VarMethodAdapter, Throwable>>();
  
  
  @NonDumpable static Map<Integer, String> OPCODES;
  public List<Label> labels = new ArrayList<>();
  public final List<String> summaries = new ArrayList<>();
    
  int methodAccess;
  String methodName;
  String methodDesc;
  String methodSignature;
  String[] methodExceptions;
  MethodDefinition md;
  
  public VarMethodAdapter(MethodVisitor mv, int access, String name,
  String desc, String signature, String[] exceptions, MethodDefinition md) 
  {
    super(Opcodes.ASM5, mv);
    this.methodAccess = access;
    this.methodName = name;
    this.methodDesc = desc;
    this.methodSignature = signature;
    this.methodExceptions = exceptions;
    this.md = md;
  }  
  
  public static String getOpcodeName(int opcode) {
    if (OPCODES == null) {
      OPCODES = ConstUtil.constantMap(Opcodes.class, "", -1);
    }
    return OPCODES.containsKey(Integer.valueOf(opcode))
      ? OPCODES.get(Integer.valueOf(opcode))
      : String.format("UNKNOWN_OP<%d>", opcode);
  }
  
  public Optional<Pair<Label, Label>> getRange() {
    if (labels.size() == 0) {
      return Optional.<Pair<Label, Label>>empty();
    }
    return Optional.of(Pair.of(labels.get(0), labels.get(labels.size()-1)));
  }
  
  @Override
  public void visitLabel(Label label) {
    if (! labels.contains(label)) {
      labels.add(label);
    }
    super.visitLabel(label);
  }
  
  @Override
  public void visitEnd() {
    Optional<Pair<Label, Label>> maybeRange = getRange();
    try {
      if (! maybeRange.isPresent()) throw new IllegalStateException(
        String.format("Missing range: %s: %s", md, Debug.ToString(this))
      );
      
      Pair<Label, Label> range = maybeRange.get();
      SignatureAttribute.MethodSignature msig = toMethodSignature();
      SignatureAttribute.Type[] jTypes = msig.getParameterTypes();
      List<VariableDefinition> varDefs = getVariables();
      List<ParameterDefinition> paramDefs = getParameters();
      
      int voffset = 0;
      // add "this" if needed (instance methods)
      if ((methodAccess & Modifier.STATIC) == 0) {
        String declTypeName = md.getDeclaringType().getErasedSignature();
        String declTypeSig = md.getDeclaringType().getSignature();
        System.err.printf("Adding 'this' var of type [%s] ...\n",
          declTypeName);
        
        this.visitLocalVariable(
          "this",
          declTypeName,
          declTypeSig,
          range.getLeft(),
          range.getRight(),
          (voffset++) // index: 0 , then  voffset := 1
        );
      }
      
      for (int pidx=0, // 0 is "source" parameter 1
               vidx=pidx+voffset,
               plen=jTypes.length,
               vlen=plen+voffset;
           vidx<vlen;
           pidx = ((vidx++) - voffset) + 1)
      {
        SignatureAttribute.Type jType = (pidx < plen)
              ? jTypes[pidx]
              : null;
        VariableDefinition vd = (vidx < varDefs.size())
          ? varDefs.get(vidx)
          : null;
        ParameterDefinition pd = (pidx < paramDefs.size())
          ? paramDefs.get(pidx)
          : null;
        System.err.printf(
          "varDefs.get(vidx: %d) = %s,\n" +
          "paramDefs.get(pidx: %d) = %s;\n",
          vidx, Debug.ToString(vd),
          pidx, Debug.ToString(pd)          
        );
        
        final String desc;
        final String signature 
          = (vd != null)
              ? vd.getVariableType().getSignature()
              : ((pd != null)
                  ? pd.getParameterType().getSignature()
                  : null);
        final Label start = range.getLeft();
        final Label end = range.getRight();
        final int index = vidx;
        final String name = 
          (vd != null && vd.isFromMetadata())
            ? vd.getName()
            : ((pidx < plen)
                ? String.format(
                    "param%d", pidx + 1
                  )
                : String.format(
                    "var%d", Math.max(0, vidx-(plen-voffset)) + 1
                  )
              );
        
        System.err.printf("\n* pidx[%2d] var[%2d] (+%d): \"%s\"\n\n", 
          pidx, vidx, voffset, name);
        
        if (jType instanceof SignatureAttribute.BaseType) {
          String jvmname = Character.valueOf(
           ((SignatureAttribute.BaseType)jType).getDescriptor()).toString();
          System.err.printf("  - jvmname = %s\n", jvmname);
          Class<?> cls = InternalNames.getClass(
            Thread.currentThread().getContextClassLoader(), jvmname);
          System.err.printf("  -     cls = %s\n", cls);
          desc = ProcyonUtil.buildRawSignature(cls);
        } else if (jType instanceof SignatureAttribute.ObjectType) {
          String jvmname = ((SignatureAttribute.ObjectType) jType).encode();
          System.err.printf("  - jvmname = %s\n", jvmname);
          Class<?> cls = InternalNames.getClass(
            Thread.currentThread().getContextClassLoader(), jvmname);
          System.err.printf("  -     cls = %s\n", cls);
          desc = ProcyonUtil.buildRawSignature(cls);
        } else if (jType instanceof SignatureAttribute.Type) {
          String jvmname = jType.jvmTypeName();
          System.err.printf("  - jvmname = %s\n", jvmname);
          String className = ClassInfo.typeToName(jvmname);
          System.err.printf("  -      cn = %s\n", className);
          Class<?> cls = DexVisitor.classForName(className);
          System.err.printf("  -     cls = %s\n", cls);
          desc = ProcyonUtil.buildRawSignature(cls);
        } else {
          desc = (vd != null)
               ? vd.getVariableType().getErasedSignature()
               : ((pd != null)
                   ? pd.getParameterType().getErasedSignature()
                   : null);
        }
        System.err.printf("  -    desc = %s\n", desc);
        
        this.visitLocalVariable(
          name,
          desc,
          signature,
          start,
          end,
          index // <==> vidx
        );
        
        String summary = String.format(
          ( "this.visitLocalVariable(  // (pidx,vidx,voffset) = (%d,%d,%d)"
          + "\n  name = '%s',\n  desc = '%s',\n  signature = '%s',"
          + "\n  start = Label(%s): %s,\n  end = Label(%s): %s,"
          + "\n  index = %d"
          + "\n);"
          + "\n"
          ).replaceAll("(^|\n)", "$1  "),
          pidx, vidx, voffset,
          name, desc, signature,
          start, Debug.ToString(start), end, Debug.ToString(end),
          index
        );
        summaries.add(summary);
        System.err.println(summary);          
      }
      
    } catch (Exception ex) {
      errors.add(Pair.of(this, ex));      
      new RuntimeException(methodDesc, ex).printStackTrace();
    }
    
    super.visitEnd();
  }
  
  @Override
  public void visitLocalVariable(String name, String desc, 
  String signature, Label start, Label end, int index)
  {
    // Put your rename logic here
    System.err.printf(
      "  - LV index: %2d; variable name: %s\n", 
      index, name
    );
    System.err.println(Arrays.asList(
      name, desc, signature, 
      Debug.ToString(start), Debug.ToString(end),
      index
    ));
    
    super.visitLocalVariable(name, desc, signature, start, end, index);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  private SignatureAttribute.MethodSignature _msig;
  private MethodBody _body;
  private List<VariableDefinition> _varDefs;
  private List<ParameterDefinition> _paramDefs;
  
  
  public SignatureAttribute.MethodSignature toMethodSignature() {
    if (_msig == null) try {
       _msig = SignatureAttribute.toMethodSignature(methodDesc);
    } catch (BadBytecode bb) { throw Reflector.Util.sneakyThrow(bb); }
    return _msig;
  }
  
  public List<ParameterDefinition> getParameters() {
    if (_paramDefs == null) try {
       _paramDefs = md.getParameters();
    } catch (Exception ex) { throw Reflector.Util.sneakyThrow(ex); }
    return _paramDefs;
  }
  
  public MethodBody getBody() {
    if (_body == null) try { 
       _body = md.getBody();
    } catch (Exception ex) { throw Reflector.Util.sneakyThrow(ex); }
    return _body;
  }
  
  public List<VariableDefinition> getVariables() {
    if (_varDefs == null) try {
        _varDefs = getBody().getVariables();
    } catch (Exception ex) { throw Reflector.Util.sneakyThrow(ex); }
    return _varDefs;
  }
  
  
}


