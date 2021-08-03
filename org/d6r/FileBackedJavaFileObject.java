package org.d6r;

import org.d6r.ClassPathUtil2;
import org.d6r.Reflect;

import sun.misc.Unsafe; 

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.parser.Keywords;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;
import javax.tools.JavaFileObject;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Modifier;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;



public class FileBackedJavaFileObject 
  implements JavaFileObject
{
  private final File file;
  private String src;
  private final URI uri; 
  private FileInputStream fis = null;
  private FileOutputStream fos = null;
  
  
  public FileBackedJavaFileObject(File srcFile) {
    this.file = srcFile;
    this.uri = this.file.toURI();
    try {
      this.src = FileUtils.readFileToString(this.file); 
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      this.src = "";
    }
  }
  
/** A
public abstract interface javax.tools.JavaFileObject {
*/
  // Methods
  public Modifier getAccessLevel() {
    return Modifier.PUBLIC;
  }
  public JavaFileObject.Kind getKind() {
    return JavaFileObject.Kind.SOURCE;
  }
  public NestingKind getNestingKind() {
    return NestingKind.TOP_LEVEL;
  }
  public boolean isNameCompatible(
  String simpleName, JavaFileObject.Kind kind)
  {
    String baseName = simpleName + kind.extension; 
    return kind.equals(getKind()) 
       && (  baseName.equals(uri.getPath()) 
          || file.toURI().getPath()
               .endsWith("/" + baseName)
    );
  } 
  
  // As javax.tools.FileObject
  // Methods
  public boolean delete() {
    return false;
  }
  public CharSequence getCharContent(boolean unknown) {
    return (CharSequence) src;
  }
  
  public long getLastModified() {
    return file.lastModified();
  }
  
  public String getName() {
    return file.getName();
  }
  
  public FileInputStream openInputStream() {     
    try {
      return (fis = FileUtils.openInputStream(file)); 
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return null;
    }
  }
  
  public FileOutputStream openOutputStream() {     
    try {
      return (fos = FileUtils.openOutputStream(file)); 
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      return null;
    }
  }
  
  public Reader openReader(boolean unknown) {
    return new StringReader(src); 
  }
  
  public Writer openWriter() {
    return new StringWriter(src.length());
  }
  public URI toUri() {
    return uri;
  }
  
}



  
  