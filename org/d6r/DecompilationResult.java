package org.d6r;

import org.d6r.annotation.*;
import bsh.ClassIdentifier;
import bsh.Factory;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.signatures.MetadataFactory;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.java.BraceEnforcement;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.JavaLanguage;
import com.strobel.decompiler.languages.java.Wrapping;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import dalvik.system.VMRuntime;
import libcore.reflect.AnnotationAccess;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import com.strobel.componentmodel.UserDataStore;
import com.strobel.core.Predicate;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.componentmodel.FrugalKeyMap;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.internal.runners.model.MultipleFailureException;


public class DecompilationResult {
  
  public static class Entry 
           implements Map.Entry<String, String>,
                      Comparable<Entry>
  {
    static final String[] EXCLUDE_FIELDS = new String[0];
    
    public final TypeReference type;
    private String source;
    public Entry(TypeReference typeRef, String decompiledSource) {
      type = typeRef;
      source = decompiledSource;
    }
    public TypeDefinition getTypeDefinition() {
      return type instanceof TypeDefinition ? (TypeDefinition) type : null;
    }
    public TypeReference getType() {
      return type;
    }
    public String getName() {
      return type.getErasedSignature();
    }
    public String getSource() {
      return source;
    }
    @Override
    public String getKey() { 
      return ClassInfo.classNameToPath(getName(), "java");
    }
    @Override
    public String getValue() {
      return getSource();
    }
    @Override public String setValue(String newSource) {
      String oldValue = source;
      source = newSource;
      return oldValue;       
    }
    @Override public boolean equals(Object other) {
      return EqualsBuilder.reflectionEquals(
        this, other, false, Entry.class, new String[0]
      );
    }
    @Override public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(
        0x49A9B3CF, // int initialNonZeroOddNumber
        0x14ACB, // int multiplierNonZeroOddNumber
        this, false, Entry.class, EXCLUDE_FIELDS
      );
    }
    @Override
    public int compareTo(Entry other) {
      return getName().compareTo(other.getName());
    }
    @Override 
    public String toString() {  
      return new StringBuilder()
        .append(ClassInfo.classNameToPath(getName(), "java"))
        .append(
          source.length() > 255
            ? new StringBuilder(255)
                .append(source.substring(0, 252))
                .append("...")
            : source
        )
        .toString();
    }
  }
  @NonDumpable("[too long]")
  protected Map<String, Entry> resultMap;    
  
  public DecompilationResult() {
    resultMap = new TreeMap<String, Entry>();      
  }
  
  public void put(TypeReference typeRef, String decompiledSource) {
    resultMap.put(
      typeRef.getErasedSignature(), new Entry(typeRef, decompiledSource)
    );
  }
  
  public Map<String, String> toMap() {
    return new RealArrayMap<String, String>(
      (Map<String, String>) (Object) this.resultMap
    );
  }
  
  public String getSource() {
    if (resultMap.size() > 0) {
      return resultMap.values().iterator().next().getSource();
    }
    return null;
  }    
  public Entry get(String className) {
    return resultMap.get(className);
  }    
  public String getSource(String className) {
    return resultMap.containsKey(className)
      ? resultMap.get(className).getSource()
      : null;
  }
  public Set<Map.Entry<String, Entry>> entrySet() {
    return resultMap.entrySet();
  }
  public Set<String> keySet() {
    return resultMap.keySet();
  }
  public Collection<Entry> values() {
    return resultMap.values();
  }
  public Iterable<Entry> entries() {
    return resultMap.values();
  }
  @Override public String toString() {
    if (resultMap.size() == 1) {
      return getSource();
    }
    return String.format(
      "%s{ %d sources }", getClass().getSimpleName(), resultMap.size()
    );
  }
}
