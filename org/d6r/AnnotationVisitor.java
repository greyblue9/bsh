package org.d6r;

import org.d6r.AnnotationNode.Item;
//import com.googlecode.dex2jar.v3.AnnotationNode;
import com.googlecode.dex2jar.DexType;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.reader.io.ArrayDataIn;
import com.googlecode.dex2jar.visitors.DexFileVisitor;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;
import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexLabel;
/*
import java.lang.reflect.Modifier;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import sun.misc.Unsafe;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

import gnu.trove.map.hash.THashMap;
//import gnu.trove.set.hash.THashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.d6r.Insn.LHSKind;
import bsh.Capabilities;
import bsh.ClassIdentifier;
import static bsh.classpath.BshClassPath.toNiceNames;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute; 
import javassist.bytecode.SignatureAttribute.Type;
import javassist.bytecode.SignatureAttribute.MethodSignature;
*/
class AnnotationCollector implements DexAnnotationAble {
  
  public String declClsName;
  public Object element;
  private DexVisitor dv;
  
  public AnnotationCollector(String declClsName, 
  Object element, DexVisitor dv)
  {
    this.declClsName = declClsName;
    this.element = element;
    this.dv = dv;
  }
  
  // As com.googlecode.dex2jar.visitors.DexAnnotationAble
  // Methods
  @Override
  public DexAnnotationVisitor visitAnnotation(
  String annClsName, boolean hasMore) {    
    if (! dv.visitAnnotations) return null;
    
    AnnotationVisitor av = new AnnotationVisitor(
      declClsName, element, annClsName, dv
    );
    if (dv.verbose) {
      return LoggingProxyFactory.newProxy(
        av, DexAnnotationVisitor.class
      );
    }
    return av;
  }
  
}


class AnnotationVisitor implements DexAnnotationVisitor {
  // As com.googlecode.dex2jar.visitors.DexAnnotationVisitor
  // Methods
  public String declClsName;
  public Object element;
  public String annClsName;
  private DexVisitor dv;
  
  public int depth = 0;
  public String indent = "";
  
  public AnnotationVisitor(String declClsName,
  Object element, String annClsName, DexVisitor dv) 
  {
    this.declClsName = declClsName;
    this.element = element;
    this.annClsName = annClsName;
    this.dv = dv;
    if (dv.verbose) {
      System.err.printf(
        "AnnotationVisitor.<init>(String declClsName = \"%s\", "
        + "Object element = %s, String annClsName = %s, "
        + "DexVisitor dv = %s)\n",
        declClsName,
        Debug.ToString(element), 
        annClsName,
        dv
      );
    }
  }
  
  @Override
  public void visit(String key, Object value) {
    if (dv.verbose) System.out.printf(
      "    %s visit(\"%s\", \"%s\")\n",
      indent, key, Debug.ToString(value)
    );
  }
  
  @Override
  public DexAnnotationVisitor visitAnnotation(
  String name, String str) {
    if (dv.verbose) System.out.printf(
      "    %s visitAnnotation(\"%s\", \"%s\")\n",
      indent, name, str
    );    
    depth += 1;
    updateIndent();    
    return this;
  }
  
  @Override
  public DexAnnotationVisitor visitArray(String name) {
    if (dv.verbose) System.out.printf(
      "    %s visitArray(\"%s\")\n",
      indent, name
    );
    depth += 1;
    updateIndent();
    return this;
  }

  @Override
  public void visitEnum(String s1, String s2, String s3) {
    if (dv.verbose) System.out.printf(
      "    %s visitEnum(\"%s\", \"%s\", \"%s\")\n",
      indent, s1, s2, s3
    );
  }
  
  @Override
  public void visitEnd() {
    if (depth > 0) depth -= 1;
    updateIndent();
  }
  
  public void updateIndent() {
    StringBuilder sb = new StringBuilder(depth * 2);
    for (int i=0; i<depth; i+=1) {
      sb.append("  ");
    }
    this.indent = sb.toString();
  }
  
}