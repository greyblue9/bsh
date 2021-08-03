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
import java.util.TreeSet;
import java.util.Comparator;
import com.github.javaparser.symbolsolver.javaparsermodel.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.model.declarations.ReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.model.declarations.ReferenceTypeDeclaration;
import  com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.Comment;


class JavaParserUtil {
  
  static boolean DEBUG_THIS;
  static final Charset UTF_8 = StandardCharsets.UTF_8;
  static final String TAG = "JavaParserUtil";
  
  public static CompilationUnit parseJava(Object srcUrlPathFileOrString) {
    // Create parser here
    final JavaParser jp = createParser();
    final StringBuilder colorBuilder = new StringBuilder(640);
    InputStream srcIn = null;
    if (String.valueOf(srcUrlPathFileOrString).length() < 255) {
      if (new File(srcUrlPathFileOrString.toString()).exists()) {
        srcIn = IOStream.from(new File(srcUrlPathFileOrString.toString()));
      }
    }
    if (srcIn == null) srcIn = new ByteArrayInputStream(
      srcUrlPathFileOrString.toString().getBytes(UTF_8)
    );
    final Provider srcProvider = new StreamProvider(
      new InputStreamReader(new AutoCloseInputStream(srcIn), UTF_8)
    );
    
    final ParseResult pr
      = jp.parse(ParseStart.COMPILATION_UNIT, srcProvider);
    
    Optional<CommentsCollection> opt = pr.getCommentsCollection();
    TreeSet<Comment> comments = (opt.isPresent())
        ? opt.get().getComments()
        : new TreeSet<Comment>(new ToStringComparator());
    boolean ok = pr.getResult().isPresent();
    if (DEBUG_THIS) Log.d(TAG, "ok = %s\n", Boolean.valueOf(ok));
    Collection<Problem> problems = (ok)
      ? Collections.emptyList() 
      : pr.getProblems();
    final Optional<?> maybeCompUnit = pr.getResult();
    if (maybeCompUnit.isPresent()) {
      CompilationUnit cu = (CompilationUnit) maybeCompUnit.get();
      onCompilationUnit(cu);
      return cu;
    } else {
      problems = pr.getProblems();
      Throwable parseError = problems.iterator().next().getCause().get();
      parseError.printStackTrace();
      // if (parseError instanceof Problem) ((Problem) parseError).print();
      throw Reflector.Util.sneakyThrow(parseError);
    }
  }
    
  
  public static JavaParser createParser() {
    
    final ParserConfiguration parseConf = new ParserConfiguration();
    if (DEBUG_THIS) Log.d(TAG, "parseConf: %s\n", parseConf);
    parseConf.setAttributeComments(true);
    parseConf.setDoNotAssignCommentsPrecedingEmptyLines(false);
    parseConf.setDoNotConsiderAnnotationsAsNodeStartForCodeAttribution(
      false);
    parseConf.setTabSize(2);
    JavaParser.setStaticConfiguration(parseConf);
    // Create parser here
    
    final JavaParser jp = new JavaParser(parseConf);
    if (DEBUG_THIS) Log.d(TAG, "jp = %s\n", Debug.ToString(jp));
    final CommentsInserter commentsInserter = Reflect.newInstance(
      CommentsInserter.class, parseConf
    );
    Reflect.setfldval(jp, "commentsInserter", commentsInserter);
    return jp;
  }
  
  static final Map<CompilationUnit, Throwable> errors = new HashMap<>();
  
  static void onCompilationUnit(CompilationUnit ast) {
    try {
      String pkg = String.valueOf(
        ast.getPackageDeclaration().get().getName()
      );
      String className = StringUtils.join(
        Arrays.asList(pkg, ast.getTypes().iterator().next().getName()), "."
      );
      SourceTypeSolver sts = SourceTypeSolver.getDefault();
      Collection<?> elements = Reflect.getfldval(sts, "elements");
      TypeSolver jpts = CollectionUtil2.typeFilter(
        elements, JavaParserTypeSolver.class
      ).iterator().next();
      
      Reflect.<Map<String, CompilationUnit>>getfldval(
        jpts, "parsedFiles"
      ).put(ClassInfo.classNameToPath(className, "java"), ast);
      
      ReferenceTypeDeclaration refTypeDecl;
      refTypeDecl = new JavaParserClassDeclaration(
        (ClassOrInterfaceDeclaration) ast.getTypes().iterator().next(), sts
      );
      Reflect.<Map<String, ReferenceTypeDeclaration>>getfldval(
        jpts, "foundTypes"
      ).put(className, refTypeDecl);
      Log.d(TAG, 
        "Added %s for parsed type '%s'",
        refTypeDecl.getClass().getSimpleName(), className
      );
    } catch (Throwable e) {
      errors.put(ast, e);
      e.printStackTrace();
    }
  }
  
}


