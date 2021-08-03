package org.d6r;

import org.ow2.asmdex.*;
import org.ow2.asmdex.encodedValue.*;
import org.ow2.asmdex.instruction.*;
import org.ow2.asmdex.lowLevelUtils.*;
import org.ow2.asmdex.specificAnnotationParser.*;
import org.ow2.asmdex.specificAnnotationVisitors.*;
import org.ow2.asmdex.structureCommon.*;
import org.ow2.asmdex.structureReader.*;
import org.ow2.asmdex.structureWriter.*;
import org.ow2.asmdex.tree.*;
import org.ow2.asmdex.util.*;
import java.io.*;
import java.util.*;
import static org.d6r.DexlibAdapter.PrimitiveTypes;


public class MethodSignature
  extends ApplicationVisitor 
{
  private List<String> methodsList;

  public MethodSignature(List<String> methodsList) {
    super(Opcodes.ASM4);
    this.methodsList = methodsList;
  }

  static String getDecName(String dexType) {
    if (dexType.startsWith("[")) {
      return getDecName(dexType.substring(1)) + "[]";
    }
    
    if (dexType.startsWith("L")) {
      String name = dexType.substring(
        1, 
        dexType.length() - 1
      );
      return name.replace('/', '.');
    }

    if (PrimitiveTypes.containsKey(dexType)) {
      return PrimitiveTypes.get(dexType);
    }
    return "void";
  }

  public static String popType(String desc) {
    return desc.substring(
      nextTypePosition(desc, 0)
    );
  }

  public static String popReturn(String desc) {
    return desc.substring(
      0, 
      desc.indexOf(popType(desc))
    );
  }

  public static int nextTypePosition
  (String desc, int pos) 
  {
    while (desc.charAt(pos )== '[') pos++;
    if (desc.charAt(pos )== 'L') {
      pos = desc.indexOf(';', pos);
    }
    pos++;
    return pos;
  }

}


