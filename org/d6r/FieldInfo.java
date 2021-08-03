package org.d6r;

import java.lang.reflect.Modifier;
import org.apache.commons.lang3.StringUtils;
import org.d6r.FieldVisitor;
import org.d6r.dumpMembers;
import org.d6r.AnnotationNode;


public class FieldInfo {

  String clsName;
  String typeName;
  String typeDesc;
  String signature;

  String name;
  FieldVisitor fv;
  int acc;

  public FieldInfo(String clsName, String typeName, 
  String typeDesc, String name, int accessFlags) 
  {
    this.clsName = clsName;
    this.typeName = typeName;
    this.typeDesc = typeDesc;
    this.name = name;
    this.acc = accessFlags;
  }
  
  public String toFieldString() {
    return String.format(
      "  \u001b[1;30m%s\u001b[0m" + "%s" + "%s %s;",
      Modifier.toString(acc), acc != 0
        ? " " 
        : "",
      dumpMembers.colorize(typeName, "1;36"), name
    );
  }

  @Override
  public String toString() {
    if (fv == null) return toFieldString();
    
    StringBuilder sb = new StringBuilder(76);
    
    //for (FieldVisitor fv: cv.fvs) {
      String annstr = "";
      if (fv.AVs.size() > 0) {
        annstr = AnnotationNode.render(
          fv.AVs.values().toArray(new AnnotationNode[0])
        ); 
        /*if (annstr.length() >= 2 && annstr.charAt(0) == ' ') 
        {
          annstr = annstr.substring(2); 
        }*/
        sb.append(annstr);
      } 
      sb.append(
        StringUtils.substringBefore(toFieldString(), "\n")
      );
       
    //}
    return sb.toString();
  }
}


