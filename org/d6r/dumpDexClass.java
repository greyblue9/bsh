package org.d6r;

import org.netbeans.modules.classfile.ClassFile;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import org.netbeans.modules.classfile.Parameter;
import com.android.dex.Dex;
import javassist.bytecode.SignatureAttribute;
import org.d6r.Reflector.Util;
import bsh.Interpreter;
import bsh.CallStack;
import java.util.Map;
import bsh.Primitive;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import org.apache.commons.lang3.StringUtils;
import com.android.dex.Dex;
import java.util.WeakHashMap;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;

//import gnu.trove.map.THashMap;

public class dumpDexClass {
  
  static Map<Dex, byte[]> resolvedDexBytes
    = new WeakHashMap<Dex, byte[]>();
  
  static String EMPTY_STRING = "";
  
  static DexVisitor _dv = null;
  
  public static String invoke(Interpreter in, CallStack cs, 
  Object obj, boolean dumpSuper)
  {
    if (obj instanceof bsh.ClassIdentifier) {
      obj = ((bsh.ClassIdentifier)obj).getTargetClass();
    }
    return dumpDexClass((Class<?>)obj, dumpSuper);
  }
  
  public static String invoke(Interpreter in, CallStack cs, 
  Object obj)
  {
    if (obj instanceof bsh.ClassIdentifier) {
      obj = ((bsh.ClassIdentifier)obj).getTargetClass();
    }
    return dumpDexClass((Class<?>)obj, true);
  }
  
  
  
  public static String dumpDexClass(DexVisitor dv, String clsName)
  { 
    dv.visit();
    _dv = dv;
    Class<?> cls = null;
    try {
      cls = Class.forName(
        clsName, false,
        Thread.currentThread().getContextClassLoader()
      );
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return EMPTY_STRING;
    }
    
    /*if (clsName.indexOf("$") != -1) {
      return dumpMembers.dumpMembers(null, cls, true);
    }*/
    
    StringBuilder sb = new StringBuilder(200 * 40);
    try {
      sb.append('\n');
  
      if (cls.getTypeParameters().length > 0) {
        sb.append(
          dumpMembers.colorize(
           String.format(
            "%s<%s>",
              cls.getName(), 
              StringUtils.join(cls.getTypeParameters(), ", ")
           ),
           "1;36"
          )
        );
        sb.append(dumpMembers.colorize(" {","0;36"));
      } else {
        sb.append(
          dumpMembers.colorize(cls.getName() + " {", "0;36")
        );
      }
      
      sb.append("\n");
      for (MethodVisitor mv: dv.getSingleClass().getMethods()) {
        sb.append("\n  ");
        sb.append(mv.toString());
      }
      sb.append(dumpMembers.colorize("\n}\n", "0;36"));
      
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      Util.sneakyThrow(e);
      
      throw new RuntimeException(e);
      //System.out.println(sb.toString());
      //System.err.println(e); 
      //return sb.toString();
    };
    
    System.err.print("...");
    
    Class<?> superCls = cls.getSuperclass();
    if (superCls != null && ! Object.class.equals(superCls)) {    
      sb.append(dumpDexClass(dv, superCls.getName()));  
    }
    dv = null;
    System.gc();    
    return sb.toString();
  }
  
  static Map<Dex, DexVisitor> dvs = new HashMap<Dex, DexVisitor>();
  
  public static String dumpDexClass(Class cls, boolean dmSuper) { 
    
    String clsName = cls.getName();
    Dex dex = getDex(cls);
    ByteBuffer data = Reflect.getfldval(dex, "data");
    data.position(0);
    byte[] dexBytes = new byte[data.remaining()];
    data.get(dexBytes);
    data.position(0);
    DexVisitor dv = new DexVisitor(dexBytes, clsName);
    dv.visit();
    String[] clsNames = dv.clsNames.toArray(new String[0]);
    StringBuilder allSb = new StringBuilder(200 * 40);
    
    for (int i=0; i<clsNames.length; i+=1) {
      clsName = clsNames[i];
      cls = DexVisitor.classForName(clsName);
      //lastDex = dex;
      //dex = getDex(cls);
      /*if (lastDex != dex || dv == null) {
        dv = dvs.get(dex);
        if (dv == null) {
          ByteBuffer data = Reflect.getfldval(dex, "data");
          dexBytes = new byte[data.remaining()];
          data.position(0);
          data.get(dexBytes);
          dv = new DexVisitor(dexBytes, clsName);
          dvs.put(dex, dv);
          dv.visit();
        }
      }
      if (clsNames.length == 1 && dv.clsNames.size() > 1 
      &&  dv.clsNames.contains(clsName)) {
        clsNames = dv.clsNames.toArray(new String[0]);
        i = -1;
        continue;
      }
      if (dv.getClass(clsName) == null) {
        dv.clsNames = DexVisitor.newSet(clsName);
        dv.addSuperclasses();
        Reflect.setfldval(dv, "visited", false);
        dv.visit();
      }*/
      //System.err.printf("%s - from %s\n", clsName, dv);
      ClassVisitor cv = dv.getClass(clsName);
      StringBuilder sb = new StringBuilder(300);
      //System.out.println(CollectionUtil.id(cv));
      sb.append('\n');
      try {
        TypeVariable[] typeVars = cls.getTypeParameters();    
        if (typeVars.length > 0) {
          sb.append(dumpMembers.colorize(String.format(
              "%s<%s> {\n", 
              clsName, StringUtils.join(typeVars, ", ")), "0;36"
          ));
        } else {
          sb.append(
            dumpMembers.colorize(clsName.concat(" {\n"), "0;36")
          );
        }
        for (MethodVisitor mv: cv.getMethods()) {
          sb.append("\n  ");
          sb.append(mv.toString());
        }
        
        sb.append(dumpMembers.colorize("\n}\n", "0;36"));   
        
        allSb.append(sb.toString());
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      }
    }
    
    try {
      return allSb.toString();
    } finally {
      System.gc();
    }
  }

}



  