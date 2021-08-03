package org.d6r;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Comparator;
import com.github.javaparser.symbolsolver.javaparsermodel.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.model.declarations.ReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.LineIterator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

import bsh.ClassIdentifier;
import bsh.Capabilities;
import com.android.dex.Dex;
import com.android.dex.ClassDef;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.nio.charset.Charset;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import bsh.Capabilities;

import com.github.javaparser.Provider;
import com.github.javaparser.StreamProvider;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.CommentsInserter;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParseResult;
import java8.util.Optional;
import com.github.javaparser.Problem;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.github.javaparser.printer.PrettyPrintVisitor;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.input.AutoCloseInputStream;


public class SourceTypeSolver
  implements TypeSolver
{
  private TypeSolver parent;
  protected static SourceTypeSolver _default;

  private List<TypeSolver> elements = new ArrayList<>();

  public SourceTypeSolver(TypeSolver... elements) {
    for (TypeSolver el : elements) {
      add(el);
    }
  }
  
  public SourceTypeSolver() {
    this(
      new JavaParserTypeSolver(new File(
        "/storage/extSdCard/_projects/sdk/bsh/trunk/src"
      )),
      new JavaParserTypeSolver(new File(
        "/external_sd/_projects/sdk/android_ics_mr1/luni/src/main/java"
      )),
      new SourcePathTypeSolver(null),
      new ReflectionTypeSolver(false)
    );
  }
  
  public static synchronized SourceTypeSolver getDefault() {
    if (_default == null) {
      _default = new SourceTypeSolver();      
    }
    return _default;
  }
  
  @Override
  public TypeSolver getRoot() {
    return parent != null? parent: this;
  }
  
  @Override
  public TypeSolver getParent() {
    return parent;
  }

  @Override
  public void setParent(TypeSolver parent) {
    this.parent = parent;
  }
  
  public void add(TypeSolver typeSolver) {
    this.elements.add(typeSolver);
    typeSolver.setParent(this);
  }
  
  @Override
  public SymbolReference<ReferenceTypeDeclaration>
  tryToSolveType(String name) 
  {
    for (TypeSolver ts: elements) {
      SymbolReference<ReferenceTypeDeclaration> res 
        = ts.tryToSolveType(name);
      if (res.isSolved()) {
        return res;
      }
    }
    return SymbolReference.unsolved(ReferenceTypeDeclaration.class);
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
}
