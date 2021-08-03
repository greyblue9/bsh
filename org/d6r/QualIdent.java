package org.d6r;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.reflect.ParameterizedType;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;


public class QualIdent
  implements 
    ParameterizedType, Cloneable, Comparable<QualIdent>
{  
  public final String name;
  public final String pkgName;
  public final String simpleName;
  
  public final boolean isImport;
  public final boolean isWildcard;  
  
  final String rawInput;
  final String rawName;
  final String[] qualIds;
  final String[] pkgParts;
  private int calcCode = UNINIT_INT;
  
  private static final String KW_IMPORT = "import";
  private static final int UNINIT_INT = Integer.MIN_VALUE;
  private static Map<String, QualIdent> cache 
    = new WeakHashMap<String, QualIdent>();
    
  public static QualIdent from(String typeName) {    
    QualIdent ident;
    typeName = typeName.trim();
    if ((ident = cache.get(typeName)) != null) {
      return ident;
    }
    cache.put(typeName, (ident = new QualIdent(typeName)));
    return ident;
  }
    
  public QualIdent(String typeName) {
    int imptIdx;
    typeName = typeName.trim();
    this.rawInput = typeName;
    if ((imptIdx = typeName.indexOf(KW_IMPORT)) != -1) {
      this.isImport = true; 
      typeName = typeName.substring( 
        imptIdx + KW_IMPORT.length()
      ).trim();
    } else {
      this.isImport = false;
    }
    this.rawName = typeName;
    this.name = getNiceName(typeName);
    this.qualIds = StringUtils.split(this.name, '.');
    if (qualIds.length > 1) {
      this.pkgParts = Arrays.copyOfRange(
        qualIds, 0, qualIds.length - 1
      );
      this.pkgName = StringUtils.join(this.pkgParts, ".");
      this.simpleName
        = this.qualIds[this.qualIds.length - 1];
      this.isWildcard = "*".equals(this.simpleName);
    } else {
      this.pkgParts = new String[0];
      this.pkgName = "";
      this.simpleName = this.name;
      this.isWildcard = false;
    }  
  }
  
  
  public String[] split() {
    return Arrays.copyOf(this.qualIds, this.qualIds.length);
  }
  
  public String get(int idx) {
    if (idx >= 0 && idx < this.qualIds.length) {
      return this.qualIds[idx];
    }
    while (idx < 0) {
      idx += this.qualIds.length;
    }
    if (idx >= this.qualIds.length) {
      return null;
    }
    return this.qualIds[idx];
  }
  
  public static String getNiceName(String typeName) {
    int semiPos, arrayDimBktPos, leadingLpos, slashPos;
    while ((semiPos = typeName.indexOf(';')) != -1) {
      typeName = typeName.substring(0, semiPos)
        .concat(typeName.substring(semiPos + 1));
    }
    while ((arrayDimBktPos = typeName.indexOf('[')) == 0) {
      typeName = typeName.substring(arrayDimBktPos  + 1)
        .concat("[]");
    }    
    while ((leadingLpos = typeName.indexOf('L')) == 0) {
      typeName = typeName.substring(leadingLpos + 1);
    }
    while ((slashPos = typeName.indexOf('/')) != -1) {
      typeName = typeName.substring(0, slashPos)
        .concat(".")
        .concat(typeName.substring(slashPos + 1));
    }
    if (typeName.length() == 1 
    || typeName.indexOf('[') == 1) {
      char shorty = typeName.charAt(0);
      String primitiveName 
        = DexlibAdapter.getName(shorty);
      typeName 
        = primitiveName.concat(typeName.substring(1));
    }
    return typeName;
  }
    
  public String getName() {
    return this.name != null? this.name: "?";
  }    
  
  public String getSimpleName() {
    return this.simpleName;
  }    
  
  public String getPackageName() {
    return this.pkgName;
  }
  
  @Override
  public Type[] getActualTypeArguments() {
    return new Type[0];
  }
  
  @Override
  public Type getOwnerType() {
    return this;
  }
  
  @Override
  public Type getRawType() {
    return this;
  }
  
  @Override
  public String toString() {
    return this.name;
  }    
  
  @Override
  public int hashCode() {
    int code;
    if ((code = this.calcCode) == UNINIT_INT) {
      code = 0;
      code = code * 37 + this.name.hashCode();
      code = code * 37 + this.pkgName.hashCode();
      code = code * 37 + this.simpleName.hashCode();
      code = code * 37 + (this.isWildcard? 1: 0);
      code = code * 37 + (this.isImport? 1: 0);
      this.calcCode = code;
    }
    return code;
  }
  
  @Override
  public boolean equals(Object that) {
    if (!(that instanceof QualIdent)) return false;
    return this.hashCode() == ((QualIdent)that).hashCode();
  }
  
  @Override
  public QualIdent clone() {    
    QualIdent cl;
    try { 
      cl = (QualIdent) super.clone();
    } catch (CloneNotSupportedException cnsEx) {
      throw new UnsupportedOperationException(
        getClass().getName().concat(" cannot be cloned: ")
          .concat(cnsEx.getClass().getSimpleName()),
        cnsEx
      );
    }
    Reflect.setfldval(cl, "name", this.name);
    Reflect.setfldval(cl, "pkgName", this.pkgName);
    Reflect.setfldval(cl, "simpleName", this.simpleName);
    Reflect.setfldval(cl, "isImport", this.isImport);
    Reflect.setfldval(cl, "isWildcard", this.isWildcard);
        
    Reflect.setfldval(cl, "rawInput", this.rawInput);
    Reflect.setfldval(cl, "rawName", this.rawName);
    if (this.qualIds != null) {
      try { Reflect.setfldval(
        cl, "qualIds", this.qualIds.clone());
      } catch (Throwable e) { Reflect.setfldval(cl, 
        "qualIds", Arrays.copyOf(qualIds, qualIds.length));
      }
    }
    if (this.pkgParts != null) {
      try { Reflect.setfldval(
        cl, "pkgParts", this.pkgParts.clone());
      } catch (Throwable e) { Reflect.setfldval(cl, 
        "pkgParts", Arrays.copyOf(pkgParts, pkgParts.length));
      }
    }    
    return cl;
  }
  
  @Override
  public int compareTo(QualIdent other) {
    if (other == null) return 1;
    return getName().compareTo(other.getName());
  }
  
  @Override 
  protected void finalize() {
    String fStr = String.format(
      "%s is being finalized!", this.name
    );
    System.err.println(fStr);
    fStr = null;
    for (Map.Entry<String, QualIdent> entry: cache.entrySet())
    {
      if (this.equals(entry.getValue())) {        
        cache.remove(entry.getKey());
      }
      entry = null;            
    }    
  }
}






