package org.d6r;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.VariableDefinition;

import java.util.*;

public class VarClassAdapter extends ClassVisitor {
  
  Map<String, VarMethodAdapter> mmap = new RealArrayMap<>();
  
  String className;
  TypeDefinition td; 
  List<MethodDefinition> mds = new ArrayList<>();

  
  public VarClassAdapter(ClassVisitor cv, String className) {
    super(Opcodes.ASM5, cv);
    this.className = className;
    this.td = ProcyonUtil.getTypeDefinition(className);    
    this.mds = td.getDeclaredMethods();
  }
  
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
  String signature, String[] exceptions) 
  {
    MethodVisitor mv = cv.visitMethod(
      access, name, desc, signature, exceptions
    );
    System.err.printf("name = \"%s\", desc = \"%s\"\n", name, desc);
    MethodDefinition mdMatch = null;
    for (MethodDefinition md: mds) {
      if (md.getName().equals(name)) {
        String erasedSig = md.getErasedSignature();
        System.err.printf(
          "  - name = \"%s\", erasedSig = \"%s\"\n",
          md.getName(),
          erasedSig
        );
        if (desc.equals(erasedSig)) {
          System.err.printf("Match: %s\n", md);
          mdMatch = md;
          break;
        }
      }
    }
    System.err.printf(
      "Method: %s %s\n", name, 
      signature != null? signature: desc
    );
    VarMethodAdapter vma = new VarMethodAdapter(
      mv, access, name, desc, signature, exceptions, mdMatch
    );
    mmap.put(name.concat(desc), vma);
    /*
    try {
      VarMethodAdapter vma = ProxyCreator.create(
        VarMethodAdapter.class, mv, access, name, desc, signature,
        exceptions, mdMatch
      );
      mmap.put(name.concat(desc), vma);
      return vma;
    } catch (Exception e) { 
      throw Reflector.Util.sneakyThrow(e);
    }*/
    return vma;
  }
}

