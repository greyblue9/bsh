/*
 * Copyright (C) 2007-2010 J?lio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package org.d6r;

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



import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import java.util.EnumSet;
import java.io.PrintWriter;
import java.util.Set;

/*
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.EqualsVisitor;
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter;
*/
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
/*
import com.github.javaparser.ast.visitor.HashCodeVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.TreeStructureVisitor;
*/
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;



/**
 * A visitor that creates a simple visualisation of the AST.
 */
public class JavaParserVisitor<A, N>
     extends VoidVisitorAdapter<A>
{
  public final Class<N> nodeType;
  public final Set<N> nodes;
  public PrintWriter pw;
  
  public JavaParserVisitor(Class<N> nodeType) {
    this.nodeType = nodeType;
    this.nodes = new IdentityHashSet<>();
  }
  
  public JavaParserVisitor() {
    this((Class<N>) (Class<?>) Node.class);
  }
  
  public Set<N> toSet() {
    return Collections.unmodifiableSet(this.nodes);
  }

  public void exitNode(Node n, final A arg) {
  }

  public void enterNode(Node n, final A arg) {
    if (nodeType.isInstance(n)) nodes.add((N) n);
  }
  
  public void onAttribute(final Node node, final String name, 
  final EnumSet<Modifier> modifiers, final A arg)
  {
    if (pw != null) pw.println(name + ": " + modifiers);
  }

  public void onAttribute(final Node node, final String name, 
  final Enum<?> e, final A arg)
  {
    if (pw != null) pw.println(name + ": " + e);
  }

  public void onAttribute(final Node node, final String name, 
  final String content, final A arg)
  {
    if (pw != null) pw.println(name + ": " + content);
  }

  public void onAttribute(final Node node, final String name, 
  final boolean value, final A arg)
  {
    if (pw != null) pw.println(name + ": " + value);
  }

  @Override
  public void visit(NodeList n, final A arg) {
    for (final Node x: (NodeList<Node>) n) {
      x.accept(this, arg);
    }
  }
  
  public void visit(AnnotationDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getMembers().accept(this, arg);
    n.getName().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(AnnotationMemberDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    if (n.getDefaultValue().isPresent()) n.getDefaultValue().get().accept(this, arg);
    n.getName().accept(this, arg);
    n.getType().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ArrayAccessExpr n, final A arg) {
    enterNode(n, arg);
    n.getIndex().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ArrayCreationExpr n, final A arg) {
    enterNode(n, arg);
    n.getElementType().accept(this, arg);
    if (n.getInitializer().isPresent()) n.getInitializer().get().accept(this, arg);
    n.getLevels().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ArrayCreationLevel n, final A arg) {
    enterNode(n, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getDimension().isPresent()) n.getDimension().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ArrayInitializerExpr n, final A arg) {
    enterNode(n, arg);
    n.getValues().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ArrayType n, final A arg) {
    enterNode(n, arg);
    n.getComponentType().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(AssertStmt n, final A arg) {
    enterNode(n, arg);
    n.getCheck().accept(this, arg);
    if (n.getMessage().isPresent()) n.getMessage().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(AssignExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "operator", n.getOperator(), arg);
    n.getTarget().accept(this, arg);
    n.getValue().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(BinaryExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "operator", n.getOperator(), arg);
    n.getLeft().accept(this, arg);
    n.getRight().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(BlockComment n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "content", n.getContent(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(BlockStmt n, final A arg) {
    enterNode(n, arg);
    n.getStatements().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(BooleanLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(BreakStmt n, final A arg) {
    enterNode(n, arg);
    if (n.getLabel().isPresent()) n.getLabel().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(CastExpr n, final A arg) {
    enterNode(n, arg);
    n.getExpression().accept(this, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(CatchClause n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    n.getParameter().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(CharLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ClassExpr n, final A arg) {
    enterNode(n, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ClassOrInterfaceDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isInterface", n.isInterface(), arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getExtendedTypes().accept(this, arg);
    n.getImplementedTypes().accept(this, arg);
    n.getTypeParameters().accept(this, arg);
    n.getMembers().accept(this, arg);
    n.getName().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ClassOrInterfaceType n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    if (n.getScope().isPresent()) n.getScope().get().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(CompilationUnit n, final A arg) {
    enterNode(n, arg);
    n.getImports().accept(this, arg);
    if (n.getModule().isPresent()) n.getModule().get().accept(this, arg);
    if (n.getPackageDeclaration().isPresent()) n.getPackageDeclaration().get().accept(this, arg);
    n.getTypes().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ConditionalExpr n, final A arg) {
    enterNode(n, arg);
    n.getCondition().accept(this, arg);
    n.getElseExpr().accept(this, arg);
    n.getThenExpr().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ConstructorDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getBody().accept(this, arg);
    n.getName().accept(this, arg);
    n.getParameters().accept(this, arg);
    n.getThrownExceptions().accept(this, arg);
    n.getTypeParameters().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ContinueStmt n, final A arg) {
    enterNode(n, arg);
    if (n.getLabel().isPresent()) n.getLabel().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(DoStmt n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    n.getCondition().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(DoubleLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(EmptyMemberDeclaration n, final A arg) {
    enterNode(n, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(EmptyStmt n, final A arg) {
    enterNode(n, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(EnclosedExpr n, final A arg) {
    enterNode(n, arg);
    if (n.getInner().isPresent()) n.getInner().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(EnumConstantDeclaration n, final A arg) {
    enterNode(n, arg);
    n.getArguments().accept(this, arg);
    n.getClassBody().accept(this, arg);
    n.getName().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(EnumDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getEntries().accept(this, arg);
    n.getImplementedTypes().accept(this, arg);
    n.getMembers().accept(this, arg);
    n.getName().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ExplicitConstructorInvocationStmt n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isThis", n.isThis(), arg);
    n.getArguments().accept(this, arg);
    if (n.getExpression().isPresent()) n.getExpression().get().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ExpressionStmt n, final A arg) {
    enterNode(n, arg);
    n.getExpression().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(FieldAccessExpr n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    if (n.getScope().isPresent()) n.getScope().get().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(FieldDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getVariables().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ForStmt n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    if (n.getCompare().isPresent()) n.getCompare().get().accept(this, arg);
    n.getInitialization().accept(this, arg);
    n.getUpdate().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ForeachStmt n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    n.getIterable().accept(this, arg);
    n.getVariable().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(IfStmt n, final A arg) {
    enterNode(n, arg);
    n.getCondition().accept(this, arg);
    if (n.getElseStmt().isPresent()) n.getElseStmt().get().accept(this, arg);
    n.getThenStmt().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ImportDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isAsterisk", n.isAsterisk(), arg);
    onAttribute(n, "isStatic", n.isStatic(), arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(InitializerDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isStatic", n.isStatic(), arg);
    n.getBody().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(InstanceOfExpr n, final A arg) {
    enterNode(n, arg);
    n.getExpression().accept(this, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(IntegerLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(IntersectionType n, final A arg) {
    enterNode(n, arg);
    n.getElements().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(JavadocComment n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "content", n.getContent(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(LabeledStmt n, final A arg) {
    enterNode(n, arg);
    n.getLabel().accept(this, arg);
    n.getStatement().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(LambdaExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isEnclosingParameters", n.isEnclosingParameters(), arg);
    n.getBody().accept(this, arg);
    n.getParameters().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(LineComment n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "content", n.getContent(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(LocalClassDeclarationStmt n, final A arg) {
    enterNode(n, arg);
    n.getClassDeclaration().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(LongLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(MarkerAnnotationExpr n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(MemberValuePair n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    n.getValue().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(MethodCallExpr n, final A arg) {
    enterNode(n, arg);
    n.getArguments().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getScope().isPresent()) n.getScope().get().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(MethodDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    if (n.getBody().isPresent()) n.getBody().get().accept(this, arg);
    n.getType().accept(this, arg);
    n.getName().accept(this, arg);
    n.getParameters().accept(this, arg);
    n.getThrownExceptions().accept(this, arg);
    n.getTypeParameters().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(MethodReferenceExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "identifier", n.getIdentifier(), arg);
    n.getScope().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(NameExpr n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(Name n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "identifier", n.getIdentifier(), arg);
    n.getAnnotations().accept(this, arg);
    if (n.getQualifier().isPresent()) n.getQualifier().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(NormalAnnotationExpr n, final A arg) {
    enterNode(n, arg);
    n.getPairs().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(NullLiteralExpr n, final A arg) {
    enterNode(n, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ObjectCreationExpr n, final A arg) {
    enterNode(n, arg);
    if (n.getAnonymousClassBody().isPresent()) n.getAnonymousClassBody().get().accept(this, arg);
    n.getArguments().accept(this, arg);
    if (n.getScope().isPresent()) n.getScope().get().accept(this, arg);
    n.getType().accept(this, arg);
    if (n.getTypeArguments().isPresent()) n.getTypeArguments().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(PackageDeclaration n, final A arg) {
    enterNode(n, arg);
    n.getAnnotations().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(Parameter n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isVarArgs", n.isVarArgs(), arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getAnnotations().accept(this, arg);
    n.getName().accept(this, arg);
    n.getType().accept(this, arg);
    n.getVarArgsAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(PrimitiveType n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "type", n.getType(), arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ReturnStmt n, final A arg) {
    enterNode(n, arg);
    if (n.getExpression().isPresent()) n.getExpression().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SimpleName n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "identifier", n.getIdentifier(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SingleMemberAnnotationExpr n, final A arg) {
    enterNode(n, arg);
    n.getMemberValue().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(StringLiteralExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "value", n.getValue(), arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SuperExpr n, final A arg) {
    enterNode(n, arg);
    if (n.getClassExpr().isPresent()) n.getClassExpr().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SwitchEntryStmt n, final A arg) {
    enterNode(n, arg);
    if (n.getLabel().isPresent()) n.getLabel().get().accept(this, arg);
    n.getStatements().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SwitchStmt n, final A arg) {
    enterNode(n, arg);
    n.getEntries().accept(this, arg);
    n.getSelector().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(SynchronizedStmt n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    n.getExpression().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ThisExpr n, final A arg) {
    enterNode(n, arg);
    if (n.getClassExpr().isPresent()) n.getClassExpr().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ThrowStmt n, final A arg) {
    enterNode(n, arg);
    n.getExpression().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(TryStmt n, final A arg) {
    enterNode(n, arg);
    n.getCatchClauses().accept(this, arg);
    if (n.getFinallyBlock().isPresent()) n.getFinallyBlock().get().accept(this, arg);
    n.getResources().accept(this, arg);
    if (n.getTryBlock().isPresent()) n.getTryBlock().get().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(TypeExpr n, final A arg) {
    enterNode(n, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(TypeParameter n, final A arg) {
    enterNode(n, arg);
    n.getName().accept(this, arg);
    n.getTypeBound().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(UnaryExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "operator", n.getOperator(), arg);
    n.getExpression().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(UnionType n, final A arg) {
    enterNode(n, arg);
    n.getElements().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(UnknownType n, final A arg) {
    enterNode(n, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(VariableDeclarationExpr n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getAnnotations().accept(this, arg);
    n.getVariables().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(VariableDeclarator n, final A arg) {
    enterNode(n, arg);
    if (n.getInitializer().isPresent()) n.getInitializer().get().accept(this, arg);
    n.getName().accept(this, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(VoidType n, final A arg) {
    enterNode(n, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(WhileStmt n, final A arg) {
    enterNode(n, arg);
    n.getBody().accept(this, arg);
    n.getCondition().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(WildcardType n, final A arg) {
    enterNode(n, arg);
    if (n.getExtendedType().isPresent()) n.getExtendedType().get().accept(this, arg);
    if (n.getSuperType().isPresent()) n.getSuperType().get().accept(this, arg);
    n.getAnnotations().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ModuleDeclaration n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "isOpen", n.isOpen(), arg);
    n.getAnnotations().accept(this, arg);
    n.getModuleStmts().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  public void visit(ModuleRequiresStmt n, final A arg) {
    enterNode(n, arg);
    onAttribute(n, "modifiers", n.getModifiers(), arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  @Override()
  public void visit(ModuleExportsStmt n, final A arg) {
    enterNode(n, arg);
    n.getModuleNames().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  @Override()
  public void visit(ModuleProvidesStmt n, final A arg) {
    enterNode(n, arg);
    n.getType().accept(this, arg);
    n.getWithTypes().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  @Override()
  public void visit(ModuleUsesStmt n, final A arg) {
    enterNode(n, arg);
    n.getType().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }

  @Override
  public void visit(ModuleOpensStmt n, final A arg) {
    enterNode(n, arg);
    n.getModuleNames().accept(this, arg);
    n.getName().accept(this, arg);
    if (n.getComment().isPresent()) n.getComment().get().accept(this, arg);
    exitNode(n, arg);
  }
}

