package org.d6r;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.declarations.ReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.resolution.UnsolvedSymbolException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java8.util.Optional;
import org.d6r.SourceUtil;
import org.apache.commons.lang3.StringUtils;
/**
@author greyblue9
*/
public class SourcePathTypeSolver implements TypeSolver {

  private File srcDir;
  private TypeSolver parent;

  private Map<String, CompilationUnit> parsedFiles
    = new HashMap<String, CompilationUnit>();
  private Map<String, List<CompilationUnit>> parsedDirectories
    = new HashMap<>();
  private Map<String, ReferenceTypeDeclaration> foundTypes
    = new HashMap<>();

  public static final Set<String> JAVA_LANG_BASIC_NAMES 
    = CollectionFactory.newSet(
       "AbstractMethodError", "Appendable", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException", "AssertionError", "AutoCloseable", "Boolean", "Byte", "CharSequence", "Character", "Class", "ClassCastException", "ClassCircularityError", "ClassFormatError", "ClassLoader", "ClassNotFoundException", "CloneNotSupportedException", "Cloneable", "Comparable", "Compiler", "Daemons", "Deprecated", "Double", "Enum", "EnumConstantNotPresentException", "Error", "Exception", "ExceptionInInitializerError", "FindBugsSuppressWarnings", "Float", "FunctionalInterface", "IllegalAccessError", "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException", "IllegalStateException", "IllegalThreadStateException", "IncompatibleClassChangeError", "IndexOutOfBoundsException", "InheritableThreadLocal", "InstantiationError", "InstantiationException", "Integer", "IntegralToString", "InternalError", "InterruptedException", "Iterable", "LinkageError", "Long", "Math", "NegativeArraySizeException", "NoClassDefFoundError", "NoSuchFieldError", "NoSuchFieldException", "NoSuchMethodError", "NoSuchMethodException", "NullPointerException", "Number", "NumberFormatException", "Object", "OutOfMemoryError", "Override", "Package", "Process", "ProcessBuilder", "Readable", "ReflectiveOperationException", "Runnable", "Runtime", "RuntimeException", "RuntimePermission", "SafeVarargs", "SecurityException", "SecurityManager", "Short", "StackOverflowError", "StackTraceElement", "StrictMath", "String", "StringBuffer", "StringBuilder", "StringIndexOutOfBoundsException", "SuppressWarnings", "System", "Thread", "ThreadDeath", "ThreadGroup", "ThreadLocal", "Throwable", "TypeNotPresentException", "UnknownError", "UnsatisfiedLinkError", "UnsupportedClassVersionError", "UnsupportedOperationException", "VerifyError", "VirtualMachineError", "Void"
  );

  public SourcePathTypeSolver(File srcDir) {
    this.srcDir = srcDir;
  }

  @Override
  public String toString() {
    return String.format(
     "%s{ srcDir: %s, parent: %s, # found: %d }",
     getClass().getSimpleName(),
     srcDir, parent, foundTypes.size()
    );
  }

  @Override
  public TypeSolver getParent() {
    return parent;
  }

  @Override
  public void setParent(TypeSolver parent) {
    this.parent = parent;
  }

  private CompilationUnit parse(File srcFile) throws FileNotFoundException {
    if (!parsedFiles.containsKey(srcFile.getAbsolutePath())) {
      parsedFiles.put(srcFile.getAbsolutePath(), JavaParser.parse(srcFile));
    }
    return parsedFiles.get(srcFile.getAbsolutePath());
  }

  private List<CompilationUnit> parseDirectory(File srcDirectory) throws FileNotFoundException {
    if (!parsedDirectories.containsKey(srcDirectory.getAbsolutePath())) {
      List<CompilationUnit> units = new ArrayList<>();
      File[] files = srcDirectory.listFiles();
      if (files == null) throw new FileNotFoundException(srcDirectory.getAbsolutePath());
      for (File file : files) {
        if (file.getName().toLowerCase().endsWith(".java")) {
          units.add(parse(file));
        }
      }
      parsedDirectories.put(srcDirectory.getAbsolutePath(), units);
    }
    return parsedDirectories.get(srcDirectory.getAbsolutePath());
  }
  
  
  @Override
  public TypeSolver getRoot() {
    return parent != null ? parent : this;
  }
  
  
  @Override
  public ReferenceTypeDeclaration solveType(String name)
    throws UnsolvedSymbolException 
  {
    SymbolReference<ReferenceTypeDeclaration> res 
      = tryToSolveType(name);
    if (res.isSolved()) {
      return res.getCorrespondingDeclaration();
    } else {
      throw new UnsolvedSymbolException(name);
    }
  }
  
  
  @Override
  public SymbolReference<ReferenceTypeDeclaration>
  tryToSolveType(String name)
  {
    // TODO support enums
    // TODO support interfaces
    if (foundTypes.containsKey(name)) {
      return SymbolReference.solved(foundTypes.get(name));
    }
    
    SymbolReference<ReferenceTypeDeclaration> result
      = tryToSolveTypeUncached(name);
    
    if (result.isSolved()) {
      foundTypes.put(name, result.getCorrespondingDeclaration());
    }
    return result;
  }
  
  
  private SymbolReference<ReferenceTypeDeclaration> 
  attemptParse(File srcFile, String typeName)
  {
    if (srcFile == null || ! srcFile.exists()) return null;
    try {
      CompilationUnit compilationUnit = parse(srcFile);
      Optional<com.github.javaparser.ast.body.TypeDeclaration<?>>
        astTypeDeclaration = Navigator.findType(
          compilationUnit, typeName
        );
      
      if (astTypeDeclaration.isPresent()) {
        return SymbolReference.solved(
          JavaParserFacade.get(this).getTypeDeclaration(
            astTypeDeclaration.get()
          )
        );
      }
    } catch (FileNotFoundException e) {
      // Ignore
      e.printStackTrace();
    }
    try {
      List<CompilationUnit> compilationUnits 
        = parseDirectory(srcFile.getParentFile());
      for (CompilationUnit compilationUnit : compilationUnits) {
        Optional<com.github.javaparser.ast.body.TypeDeclaration<?>> 
          astTypeDeclaration = Navigator.findType(
            compilationUnit, typeName
          );
        if (astTypeDeclaration.isPresent()) {
          return SymbolReference.solved(
            JavaParserFacade.get(this).getTypeDeclaration(
              astTypeDeclaration.get()
            )
          );
        }
      }
    } catch (FileNotFoundException e) {
      // Ignore
      e.printStackTrace();
    }
    return null;
  }
  
  
  
  private SymbolReference<ReferenceTypeDeclaration> 
  tryToSolveTypeUncached(String name)
  {
    if (! StringUtils.startsWith(name, "java.lang.")
    &&  JAVA_LANG_BASIC_NAMES.contains(
          StringUtils.substringAfterLast(name, ".")))
    {
      return SymbolReference.unsolved(ReferenceTypeDeclaration.class);
    }
    
    
    String[] nameElements = name.split("\\.");
    List<String> qualNames = new LinkedList<String>();
    
    for (int i = nameElements.length; i>0; i--) {
      
      String filePath = srcDir != null
        ? srcDir.getAbsolutePath()
        : null;
      if (srcDir != null) { 
        for (int j = 0; j < i; j++) {
          filePath += "/" + nameElements[j];
        }
        filePath += ".java";
      }
      
      File srcFile = srcDir != null
        ? new File(filePath)
        : null;
      
      String typeName = "";
      for (int j = i - 1; j < nameElements.length; j++) {
        if (j != i - 1) {
          typeName += ".";
        }
        typeName += nameElements[j];
        qualNames.add(typeName);
        // System.err.println(typeName);
      }

      
      if (srcDir != null) {
        SymbolReference<ReferenceTypeDeclaration> result
          = attemptParse(new File(filePath), typeName);
        if (result != null) return result;
      }
    }
    
    // Try to locate source
    System.err.printf("* looking up: [%s]\n", name);
    File locSrcFile = SourceUtil.getSourceFile(name);
    
    if (locSrcFile != null) {
      System.err.printf("   - found: [%s]\n", locSrcFile);
      
      for (String typeName: qualNames) {
        // System.err.printf("typeName: %s\n", typeName);
        SymbolReference<ReferenceTypeDeclaration> result
          = attemptParse(locSrcFile, typeName);
        if (result != null) return result;
      }
    }
    
    // Failure result
    return SymbolReference.unsolved(ReferenceTypeDeclaration.class);
  }

}
