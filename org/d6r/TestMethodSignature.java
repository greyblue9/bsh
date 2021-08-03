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

public class TestMethodSignature {
  
  public static String popType(String desc) {
      return desc.substring(nextTypePosition(desc,0));
  }
  public static int nextTypePosition(String desc, int pos) {
      while(desc.charAt(pos )== '[') pos++;
      if (desc.charAt(pos )== 'L') pos = desc.indexOf(';', pos);
      pos++;
      return pos;
  }
  
}


