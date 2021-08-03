
package org.d6r;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.lang.reflect.Modifier;
 
public class VariableInfo {
  
  
  public final String name;
  public final String type;
  public final String signature;
  public final int index;
  public final LabelInfo start;
  public final LabelInfo[] ends;
  public final LabelInfo[] restarts;
  
  private static final LabelInfo[] EMPTY_LABEL_ARRAY
 = new LabelInfo[0];
  private static final String STR_NULL = "null";

  
  public VariableInfo(String name, String type) {
    this(name, type, null);
  }
  
  public VariableInfo(String name, String type, String signature) {
    this(name, type, signature, 0);
  }
  
  public VariableInfo(String name, String type, String signature, int index) {
    this(name, type, signature, index, null);
  }
  
  public VariableInfo(String name, String type, String signature, int index, LabelInfo start) {
    this(name, type, signature, index, start, (LabelInfo[]) null);
  }
  
  public VariableInfo(String name, String type, String signature, int index, LabelInfo start, LabelInfo end) {
    this(
      name, type, signature, index, 
      start, 
      new LabelInfo[] { end }
    );
  }
  
  public VariableInfo(String name, String type, String signature, int index, LabelInfo start, LabelInfo[] ends)
  {
    this(
      name, type, signature, index, 
      start, 
      ends,
      null
    );
  }
  
  public VariableInfo(String name, String type, String signature, int index, LabelInfo start, LabelInfo[] ends, LabelInfo[] restarts) 
  {
    this.name = name;
    
    this.type = 
      (  type != null 
     &&  type.length() != 0 
     && !type.equals(STR_NULL))
       ? type
       : "java.lang.Object";
    
    this.signature = 
      (  signature != null 
     &&  signature.length() != 0 
     && !signature.equals(STR_NULL))
       ? signature
       : type;
    
    this.start = start;
    
    this.ends
 = ends != null && ends.length > 0
       ? ends
       : EMPTY_LABEL_ARRAY;
    
    this.restarts 
 = restarts != null && restarts.length > 0
       ? restarts
       : EMPTY_LABEL_ARRAY;
    
    this.index = index;
  }
  
  
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }
  
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder(76 * 10);
    
    sb.append(signature);
    sb.append(' ');
    sb.append(name);
    
    return sb.toString();
  }
  
}
