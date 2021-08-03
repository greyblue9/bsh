package org.d6r;
import org.d6r.annotation.*;
import bsh.Factory;
import com.google.common.collect.Lists;
import javafile.api.BlockComment;
import javafile.api.Comment;
import javafile.api.CompilationUnit;
import javafile.api.ImportDeclaration;
import javafile.api.LineComment;
import javafile.api.PackageDeclaration;
import javafile.api.TypeParameter;
import javafile.api.Node;
import javafile.api.body.AnnotationDeclaration;
import javafile.api.body.AnnotationMemberDeclaration;
import javafile.api.body.BodyDeclaration;
import javafile.api.body.CatchParameter;
import javafile.api.body.ClassOrInterfaceDeclaration;
import javafile.api.body.ConstructorDeclaration;
import javafile.api.body.EmptyMemberDeclaration;
import javafile.api.body.EmptyTypeDeclaration;
import javafile.api.body.EnumConstantDeclaration;
import javafile.api.body.EnumDeclaration;
import javafile.api.body.FieldDeclaration;
import javafile.api.body.InitializerDeclaration;
import javafile.api.body.JavadocComment;
import javafile.api.body.MethodDeclaration;
import javafile.api.body.ModifierSet;
import javafile.api.body.Parameter;
import javafile.api.body.Resource;
import javafile.api.body.TypeDeclaration;
import javafile.api.body.VariableDeclarator;
import javafile.api.body.VariableDeclaratorId;
import javafile.api.expr.AnnotationExpr;
import javafile.api.expr.ArrayAccessExpr;
import javafile.api.expr.ArrayCreationExpr;
import javafile.api.expr.ArrayInitializerExpr;
import javafile.api.expr.AssignExpr;
import javafile.api.expr.BinaryExpr;
import javafile.api.expr.BooleanLiteralExpr;
import javafile.api.expr.CastExpr;
import javafile.api.expr.CharLiteralExpr;
import javafile.api.expr.ClassExpr;
import javafile.api.expr.ConditionalExpr;
import javafile.api.expr.DoubleLiteralExpr;
import javafile.api.expr.EnclosedExpr;
import javafile.api.expr.Expression;
import javafile.api.expr.FieldAccessExpr;
import javafile.api.expr.InstanceOfExpr;
import javafile.api.expr.IntegerLiteralExpr;
import javafile.api.expr.IntegerLiteralMinValueExpr;
import javafile.api.expr.LambdaExpr;
import javafile.api.expr.LongLiteralExpr;
import javafile.api.expr.LongLiteralMinValueExpr;
import javafile.api.expr.MarkerAnnotationExpr;
import javafile.api.expr.MemberValuePair;
import javafile.api.expr.MethodCallExpr;
import javafile.api.expr.MethodReferenceExpr;
import javafile.api.expr.NameExpr;
import javafile.api.expr.NormalAnnotationExpr;
import javafile.api.expr.NullLiteralExpr;
import javafile.api.expr.ObjectCreationExpr;
import javafile.api.expr.QualifiedNameExpr;
import javafile.api.expr.SingleMemberAnnotationExpr;
import javafile.api.expr.StringLiteralExpr;
import javafile.api.expr.SuperExpr;
import javafile.api.expr.ThisExpr;
import javafile.api.expr.UnaryExpr;
import javafile.api.expr.VariableDeclarationExpr;
import javafile.api.stmt.AssertStmt;
import javafile.api.stmt.BlockStmt;
import javafile.api.stmt.BreakStmt;
import javafile.api.stmt.CatchClause;
import javafile.api.stmt.ContinueStmt;
import javafile.api.stmt.DoStmt;
import javafile.api.stmt.EmptyStmt;
import javafile.api.stmt.ExplicitConstructorInvocationStmt;
import javafile.api.stmt.ExpressionStmt;
import javafile.api.stmt.ForStmt;
import javafile.api.stmt.ForeachStmt;
import javafile.api.stmt.IfStmt;
import javafile.api.stmt.LabeledStmt;
import javafile.api.stmt.ReturnStmt;
import javafile.api.stmt.Statement;
import javafile.api.stmt.SwitchEntryStmt;
import javafile.api.stmt.SwitchStmt;
import javafile.api.stmt.SynchronizedStmt;
import javafile.api.stmt.ThrowStmt;
import javafile.api.stmt.TryStmt;
import javafile.api.stmt.TypeDeclarationStmt;
import javafile.api.stmt.WhileStmt;
import javafile.api.type.ClassOrInterfaceType;
import javafile.api.type.PrimitiveType;
import javafile.api.type.ReferenceType;
// import javafile.api.type.Type;
import javafile.api.type.VoidType;
import javafile.api.type.WildcardType;
import java.util.Iterator;
import java.util.List;
import java.util.*;
import javafile.api.visitor.VoidVisitor;
import java.lang.reflect.*;
import org.apache.commons.lang3.tuple.Pair;


/**
@author Julio Vilmar Gesser
*/
public class JavaVisitor implements VoidVisitor<Object> {

  public Set<Class<?>> filter = null;
  public static boolean LOG = false;

  public Object delegate = null;

  public Set<Object> visited = new HashSet<Object>();
  public Set<Pair<Object, Object>> visitedPairs = new HashSet<>();
  public Set<Pair<Class<? extends Node>, Method>> methods = new HashSet<>();

  public Object lastResult;
  
  @NonDumpable
  public List<Throwable> exs = new ArrayList<Throwable>();

  public void setFilter(Class<?>... allowedTypes) {
    this.filter = new HashSet<Class<?>>(Arrays.asList(allowedTypes));
  }
  
  public void at(Object obj) {
    at(obj, null);
  }
  
  public <T> void atList(List<T> list, Object arg) {
    if (list == null) return;
    for (T item: list) {
      at(item, arg);
    }
  }
    
  public void at(Object obj, Object arg) {
    if (obj == null) return;
    
    if (obj instanceof List) {
      if (((List)obj).isEmpty()) return;
      Object item = ((List) obj).get(0);
      System.err.printf(
        "Traversing List<%s> ...\n", item.getClass().getName()
      );
      atList((List) obj, arg);
      return;
    } else if (obj instanceof Iterable) {
      List<Object> items = Lists.newLinkedList((Iterable<Object>) obj);
      if (items.isEmpty()) return;
      Object item = ((Iterable) obj).iterator().next();
      System.err.printf(
        "Traversing LinkedList<%s> ...\n", item.getClass().getName()
      );
      atList((List) obj, arg);
      return;
    }
    
    try { 
      if (this.filter != null) {
        boolean ok = false;
        for (Class<?> type : filter) {
          if (!type.isInstance(obj)) continue;
          ok = true;
          break;
        }
        if (!ok) return;
      }
    } catch (Throwable e) { e.printStackTrace(); }
    
    visited.add(obj);
    
    if (arg != null) {
      try {
        visitedPairs.add(Pair.of(obj, arg));
      } catch (Throwable e) { e.printStackTrace(); }
    }
    
    
    if (delegate != null) {
      try {
        Class<?> objType = obj.getClass();
        String className = objType.getName();
        int lioDot = className.lastIndexOf('.');
        int lioDollar = className.lastIndexOf('$');
        int lio = Math.max(lioDot, lioDollar);
        String simple = (lio != -1) 
          ? className.substring(lio + 1) 
          : className;
          
        Object _result = null;
        for (String methodName : new String[] { "at".concat(simple), "at" })
        {
          try {
            if (LOG) System.err.printf(
              " - Looking for method: %s(%s) ...\n", 
              simple, methodName
            );
            Method m1 = Reflect.findMethod(delegate, "at".concat(simple));
            if (m1 != null) {
              System.err.printf(" - found: %s\n", m1.toGenericString());
              Class<?>[] pTypes = m1.getParameterTypes();
              Object[] args = new Object[pTypes.length];
              if (pTypes.length > 0 && pTypes[0].isAssignableFrom(objType)) 
              {
                args[0] = obj;
              }
              if (pTypes.length > 1 
              &&  arg != null
              &&  pTypes[1].isAssignableFrom(arg.getClass())) {
                args[1] = arg;
              }
              Object result = m1.invoke(delegate, args);
              if (_result == null) _result = result;
            }
          } catch (Throwable e) {
            Throwable cause = Reflector.getRootCause(e);
            if (!(cause instanceof ReflectiveOperationException)) {
              if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
            }
          }
        }
        if (_result != null) lastResult = _result;
      } catch (Throwable ex) { ex.printStackTrace(); }
    }
    
    if (obj instanceof Node) discoverAndVisitImmediate((Node)obj, arg);
  }
  
  
  public void discoverAndVisitImmediate(Node n, Object arg) {
    Class<?> c = n.getClass();
    do { 
      for (Method md: c.getDeclaredMethods()) { 
        if (! md.getName().startsWith("get")) continue; 
        // System.out.println(md.getGenericReturnType()); 
        Class<?> retCls = md.getReturnType();
        if (Node.class.isAssignableFrom(retCls)) { 
          // System.err.println(dumpMembers.colorize(md));
          try {
            
            md.setAccessible(true); 
            if (LOG) System.err.printf(
              "%s.invoke(%s)\n", md.getName(), 
              ClassInfo.simplifyName(Factory.typeof(n).getName())
            );
            Node child = (Node) md.invoke(n);
            if (child == null) continue;
            if (visited.contains(child)) continue;
            //System.err.println(retNode); 
            methods.add(
              (Pair<Class<? extends Node>,Method>) (Object)
              Pair.of(n.getClass(), md
            ));
            at(child, arg);
          } catch (Throwable e) { 
            System.err.println(md.toGenericString() + " threw " + e);
          }
        } else if (List.class.isAssignableFrom(retCls)) { 
          
          Type grType = md.getGenericReturnType();
          if (! (grType instanceof ParameterizedType)) continue;
          ParameterizedType pt = (ParameterizedType) grType;
          Type[] actual = pt.getActualTypeArguments();
          if (actual.length != 1) continue; 
          if (!(actual[0] instanceof Class)) {
            System.err.println("Skip: " + grType);
            continue;
          }
          Class<?> lCls = (Class<?>) actual[0];
          if (! Node.class.isAssignableFrom(lCls)) continue; 
          List<? extends Node> nodeList = null;
          // System.err.println(dumpMembers.colorize(md));
          try {
            md.setAccessible(true); 
            if (LOG) System.err.printf(
              "%s.invoke(%s)\n", md.getName(), 
              ClassInfo.simplifyName(Factory.typeof(n).getName())
            );
            nodeList = (List<? extends Node>) md.invoke(n); 
            methods.add(
              (Pair<Class<? extends Node>,Method>) (Object)
              Pair.of(n.getClass(), md
            ));
            if (nodeList == null) continue;
            atList(nodeList, arg);
          } catch (Throwable e) { 
            exs.add(e);
            System.err.println(md.toGenericString() + " threw " + e);
          }
        }
      }
    } while ((c = c.getSuperclass()) != Object.class && c != null);
  }
  
  


  @Override
  public void visit(CompilationUnit n, Object arg) {
    if (n.getPackage() != null) {
      at(n.getPackage());
      n.getPackage().accept(this, arg);
    }
    if (n.getImports() != null) {
      for (ImportDeclaration i : n.getImports()) {
        at(i);
        i.accept(this, arg);
      }
    }
    if (n.getTypes() != null) {
      for (Iterator<TypeDeclaration> i = n.getTypes().iterator();
           i.hasNext();) 
      {
        TypeDeclaration decl = i.next();
        at(decl, arg);
        decl.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
  }

  @Override
  public void visit(PackageDeclaration n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getAnnotations(), arg);
    at(n.getName());
    n.getName().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(NameExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(QualifiedNameExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getQualifier());
    n.getQualifier().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ImportDeclaration n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.isStatic()) {
    }
    at(n.getName());
    n.getName().accept(this, arg);
    if (n.isAsterisk()) {
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Object arg) {
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    if (n.isInterface()) {
    } else {
    }
    at(n.getTypeParameters(), arg);
    if (n.getExtends() != null) {
      for (Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext(); ) {
        ClassOrInterfaceType c = i.next();
        at(c);
        c.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getImplements() != null) {
      for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext(); ) {
        ClassOrInterfaceType c = i.next();
        at(c);
        c.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getMembers() != null) {
      at(n.getMembers(), arg);
    }
    // printer.unindent();
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EmptyTypeDeclaration n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getJavaDoc(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(JavadocComment n, Object arg) {
    at(n);
  }

  @Override
  public void visit(ClassOrInterfaceType n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getScope() != null) {
      at(n.getScope());
      n.getScope().accept(this, arg);
    }
    at(n.getTypeArgs(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(TypeParameter n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getTypeBound() != null) {
      for (Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext(); ) {
        ClassOrInterfaceType c = i.next();
        at(c);
        c.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(PrimitiveType n, Object arg) {
    at(n.getBeginComments(), arg);
    switch((n.getType())) {
      case Boolean:
        break;
      case Byte:
        break;
      case Char:
        break;
      case Double:
        break;
      case Float:
        break;
      case Int:
        break;
      case Long:
        break;
      case Short:
        break;
    }
    at(n);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ReferenceType n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getType());
    n.getType().accept(this, arg);
    for (int i = 0; i < n.getArrayCount(); i++) {
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(WildcardType n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getExtends() != null) {
      at(n.getExtends());
      n.getExtends().accept(this, arg);
    }
    if (n.getSuper() != null) {
      at(n.getSuper());
      n.getSuper().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(FieldDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    at(n.getType());
    n.getType().accept(this, arg);
    for (Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext(); ) {
      VariableDeclarator var = i.next();
      at(var);
      var.accept(this, arg);
      if (i.hasNext()) {
      }
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(VariableDeclarator n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getId());
    n.getId().accept(this, arg);
    if (n.getInit() != null) {
      at(n.getInit());
      n.getInit().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(VariableDeclaratorId n, Object arg) {
    at(n.getBeginComments(), arg);
    for (int i = 0; i < n.getArrayCount(); i++) {
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ArrayInitializerExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getValues() != null) {
      for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext(); ) {
        Expression expr = i.next();
        at(expr);
        expr.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(VoidType n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ArrayAccessExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getName());
    n.getName().accept(this, arg);
    at(n.getIndex());
    n.getIndex().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ArrayCreationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getType());
    n.getType().accept(this, arg);
    if (n.getDimensions() != null) {
      for (Expression dim : n.getDimensions()) {
        at(dim);
        dim.accept(this, arg);
      }
      for (int i = 0; i < n.getArrayCount(); i++) {
      }
    } else {
      for (int i = 0; i < n.getArrayCount(); i++) {
      }
      at(n.getInitializer());
      n.getInitializer().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(AssignExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getTarget());
    n.getTarget().accept(this, arg);
    switch((n.getOperator())) {
      case assign:
        break;
      case and:
        break;
      case or:
        break;
      case xor:
        break;
      case plus:
        break;
      case minus:
        break;
      case rem:
        break;
      case slash:
        break;
      case star:
        break;
      case lShift:
        break;
      case rSignedShift:
        break;
      case rUnsignedShift:
        break;
    }
    at(n.getValue());
    n.getValue().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(BinaryExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getLeft());
    n.getLeft().accept(this, arg);
    switch((n.getOperator())) {
      case or:
        break;
      case and:
        break;
      case binOr:
        break;
      case binAnd:
        break;
      case xor:
        break;
      case equals:
        break;
      case notEquals:
        break;
      case less:
        break;
      case greater:
        break;
      case lessEquals:
        break;
      case greaterEquals:
        break;
      case lShift:
        break;
      case rSignedShift:
        break;
      case rUnsignedShift:
        break;
      case plus:
        break;
      case minus:
        break;
      case times:
        break;
      case divide:
        break;
      case remainder:
        break;
    }
    at(n.getRight());
    n.getRight().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(CastExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getType());
    n.getType().accept(this, arg);
    at(n.getExpr());
    n.getExpr().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ClassExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getType());
    n.getType().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ConditionalExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getCondition());
    n.getCondition().accept(this, arg);
    at(n.getThenExpr());
    n.getThenExpr().accept(this, arg);
    at(n.getElseExpr());
    n.getElseExpr().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EnclosedExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getInner());
    n.getInner().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(FieldAccessExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getScope());
    n.getScope().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(InstanceOfExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getExpr());
    n.getExpr().accept(this, arg);
    at(n.getType());
    n.getType().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(CharLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(DoubleLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(IntegerLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(LongLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(IntegerLiteralMinValueExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(LongLiteralMinValueExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(StringLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(BooleanLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(NullLiteralExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ThisExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getClassExpr() != null) {
      at(n.getClassExpr());
      n.getClassExpr().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(SuperExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getClassExpr() != null) {
      at(n.getClassExpr());
      n.getClassExpr().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(MethodCallExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getScope() != null) {
      at(n.getScope());
      n.getScope().accept(this, arg);
    }
    at(n.getTypeArgs(), arg);
    at(n.getArgs(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(LambdaExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    /**
    if (n.getScope() != null) {
    at(n.getScope());
    n.getScope().accept(this, arg);
    }
    at(n.getTypeArgs(), arg);
    at(n.getArgs(), arg);
    */
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ObjectCreationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getScope() != null) {
      at(n.getScope());
      n.getScope().accept(this, arg);
    }
    at(n.getTypeArgs(), arg);
    at(n.getType());
    n.getType().accept(this, arg);
    at(n.getArgs(), arg);
    if (n.getAnonymousClassBody() != null) {
      at(n.getAnonymousClassBody(), arg);
      // printer.unindent();
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(UnaryExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    switch((n.getOperator())) {
      case positive:
        break;
      case negative:
        break;
      case inverse:
        break;
      case not:
        break;
      case preIncrement:
        break;
      case preDecrement:
        break;
    }
    at(n.getExpr());
    n.getExpr().accept(this, arg);
    switch((n.getOperator())) {
      case posIncrement:
        break;
      case posDecrement:
        break;
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ConstructorDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    at(n.getTypeParameters(), arg);
    if (n.getTypeParameters() != null) {
    }
    if (n.getParameters() != null) {
      for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
        Parameter p = i.next();
        at(p);
        p.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getThrows() != null) {
      for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext(); ) {
        NameExpr name = i.next();
        at(name);
        name.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    at(n.getBlock());
    n.getBlock().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(MethodDeclaration n, Object arg) {
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    at(n.getTypeParameters(), arg);
    if (n.getTypeParameters() != null) {
    }
    at(n.getType());
    n.getType().accept(this, arg);
    if (n.getParameters() != null) {
      for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
        Parameter p = i.next();
        at(p);
        p.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    for (int i = 0; i < n.getArrayCount(); i++) {
    }
    if (n.getThrows() != null) {
      for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext(); ) {
        NameExpr name = i.next();
        at(name);
        name.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getBody() == null) {
    } else {
      at(n.getBody());
      n.getBody().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(Parameter n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getAnnotations(), arg);
    at(n.getModifiers());
    at(n.getType());
    n.getType().accept(this, arg);
    if (n.isVarArgs()) {
    }
    at(n.getId());
    n.getId().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(CatchParameter n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getAnnotations(), arg);
    at(n.getModifiers());
    at(n.getTypeList().get(0));
    n.getTypeList().get(0).accept(this, arg);
    for (int i = 1; i < n.getTypeList().size(); i++) {
      at(n.getTypeList().get(i));
      n.getTypeList().get(i).accept(this, arg);
    }
    at(n.getId());
    n.getId().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(Resource n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getAnnotations(), arg);
    at(n.getModifiers());
    at(n.getType());
    n.getType().accept(this, arg);
    at(n.getId());
    n.getId().accept(this, arg);
    at(n.getExpression());
    n.getExpression().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.isThis()) {
      at(n.getTypeArgs(), arg);
    } else {
      if (n.getExpr() != null) {
        at(n.getExpr());
        n.getExpr().accept(this, arg);
      }
      at(n.getTypeArgs(), arg);
    }
    at(n.getArgs(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(VariableDeclarationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getAnnotations(), arg);
    at(n.getModifiers());
    at(n.getType());
    n.getType().accept(this, arg);
    for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext(); ) {
      VariableDeclarator v = i.next();
      at(v);
      v.accept(this, arg);
      if (i.hasNext()) {
      }
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(TypeDeclarationStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getTypeDeclaration());
    n.getTypeDeclaration().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(AssertStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getCheck());
    n.getCheck().accept(this, arg);
    if (n.getMessage() != null) {
      at(n.getMessage());
      n.getMessage().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(BlockStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getInternalComments(), arg);
    // printer.unindent();
    if (n.getStmts() != null) {
      for (Statement s : n.getStmts()) {
        at(s);
        s.accept(this, arg);
      }
      // printer.unindent();
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(LabeledStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getStmt());
    n.getStmt().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EmptyStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ExpressionStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getExpression());
    n.getExpression().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(SwitchStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getSelector());
    n.getSelector().accept(this, arg);
    if (n.getEntries() != null) {
      for (SwitchEntryStmt e : n.getEntries()) {
        at(e);
        e.accept(this, arg);
      }
      // printer.unindent();
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(SwitchEntryStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getLabel() != null) {
      at(n.getLabel());
      n.getLabel().accept(this, arg);
    } else {
    }
    if (n.getStmts() != null) {
      for (Statement s : n.getStmts()) {
        at(s);
        s.accept(this, arg);
      }
    }
    // printer.unindent();
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(BreakStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getId() != null) {
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ReturnStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getExpr() != null) {
      at(n.getExpr());
      n.getExpr().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EnumDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    if (n.getImplements() != null) {
      for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext(); ) {
        ClassOrInterfaceType c = i.next();
        at(c);
        c.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getEntries() != null) {
      for (Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext(); ) {
        EnumConstantDeclaration e = i.next();
        at(e);
        e.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    if (n.getMembers() != null) {
      at(n.getMembers(), arg);
    } else {
      if (n.getEntries() != null) {
      }
    }
    // printer.unindent();
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EnumConstantDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    if (n.getArgs() != null) {
      at(n.getArgs(), arg);
    }
    if (n.getClassBody() != null) {
      at(n.getClassBody(), arg);
      // printer.unindent();
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(EmptyMemberDeclaration n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getJavaDoc(), arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(InitializerDeclaration n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getJavaDoc(), arg);
    if (n.isStatic()) {
    }
    at(n.getBlock());
    n.getBlock().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(IfStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getCondition());
    n.getCondition().accept(this, arg);
    at(n.getThenStmt());
    n.getThenStmt().accept(this, arg);
    if (n.getElseStmt() != null) {
      at(n.getElseStmt());
      n.getElseStmt().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(WhileStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getCondition());
    n.getCondition().accept(this, arg);
    at(n.getBody());
    n.getBody().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ContinueStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getId() != null) {
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(DoStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getBody());
    n.getBody().accept(this, arg);
    at(n.getCondition());
    n.getCondition().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ForeachStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getVariable());
    n.getVariable().accept(this, arg);
    at(n.getIterable());
    n.getIterable().accept(this, arg);
    at(n.getBody());
    n.getBody().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ForStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getInit() != null) {
      for (Iterator<Expression> i = n.getInit().iterator(); i.hasNext(); ) {
        Expression e = i.next();
        at(e);
        at(e);
        e.accept(this, arg);
        if (i.hasNext()) {
          
        }        
      }
    }
    if (n.getCompare() != null) {
      at(n.getCompare());
      n.getCompare().accept(this, arg);
    }
    if (n.getUpdate() != null) {
      for (Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext(); ) {
        
        Expression e = i.next();
        at(e);
        at(e);
        e.accept(this, arg);
        if (i.hasNext()) {
          
        }
      }
    }
    at(n.getBody());
    n.getBody().accept(this, arg);
    at(n.getBody());
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(ThrowStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getExpr());
    n.getExpr().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(SynchronizedStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getExpr());
    n.getExpr().accept(this, arg);
    at(n.getBlock());
    n.getBlock().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(TryStmt n, Object arg) {
    at(n.getBeginComments(), arg);
    if (n.getResources() != null && n.getResources().size() > 0) {
      at(n.getResources().get(0));
      n.getResources().get(0).accept(this, arg);
      for (int i = 1; i < n.getResources().size(); i++) {
        at(n.getResources().get(i));
        n.getResources().get(i).accept(this, arg);
      }
    }
    at(n.getTryBlock());
    n.getTryBlock().accept(this, arg);
    if (n.getCatchs() != null) {
      for (CatchClause c : n.getCatchs()) {
        at(c);
        c.accept(this, arg);
      }
    }
    if (n.getFinallyBlock() != null) {
      at(n.getFinallyBlock());
      n.getFinallyBlock().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(CatchClause n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getExcept());
    n.getExcept().accept(this, arg);
    at(n.getCatchBlock());
    n.getCatchBlock().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(AnnotationDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    if (n.getMembers() != null) {
      at(n.getMembers(), arg);
    }
    // printer.unindent();
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(AnnotationMemberDeclaration n, Object arg) {
    at(n.getJavaDoc(), arg);
    at(n.getAnnotations(), arg);
    at(n.getBeginComments(), arg);
    at(n.getModifiers());
    at(n.getType());
    n.getType().accept(this, arg);
    if (n.getDefaultValue() != null) {
      at(n.getDefaultValue());
      n.getDefaultValue().accept(this, arg);
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(MarkerAnnotationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getName());
    n.getName().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(SingleMemberAnnotationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getName());
    n.getName().accept(this, arg);
    at(n.getMemberValue());
    n.getMemberValue().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(NormalAnnotationExpr n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getName());
    n.getName().accept(this, arg);
    if (n.getPairs() != null) {
      for (Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext(); ) {
        MemberValuePair m = i.next();
        at(m);
        m.accept(this, arg);
        if (i.hasNext()) {
        }
      }
    }
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(MemberValuePair n, Object arg) {
    at(n.getBeginComments(), arg);
    at(n.getValue());
    n.getValue().accept(this, arg);
    at(n.getEndComments(), arg);
  }

  @Override
  public void visit(LineComment n, Object arg) {
    // No longer used
    at(n);
    n.accept(this, arg);
  }

  @Override
  public void visit(BlockComment n, Object arg) {
    // No longer used
    at(n);
    n.accept(this, arg);
  }

  @Override
  public void visit(Comment n, Object arg) {
    // No longer used
    at(n);
    n.accept(this, arg);
  }

  @Override
  public void visit(MethodReferenceExpr n, Object arg) {
    // TODO: Print method reference data
    at(n.getBeginComments(), arg);
    //n.getBeginComments().accept(this, arg);
    at(n.getEndComments(), arg);
    //n.getEndComments().accept(this, arg);
  }
  
}




