package org.d6r;

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;

import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.JexlNode.Constant;
import org.apache.commons.jexl3.parser.ParserVisitor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.jexl3.internal.Script;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.ParameterizedType;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * <p>
 * It rebuilds an expression string from the tree and the start/end offsets of the cause in that string.
 * This implies that exceptions during evaluation do always carry the node that's causing the error.
 * </p>
 * @since 2.0
 */
public class JexlWalker<V> extends ParserVisitor implements JexlInfo.Detail {
  static final LazyMember<Field> SCRIPT_SCRIPT = LazyMember.of(
    "script", Script.class
  );
  
  /** The builder to compose messages. */
  public final StringBuilder builder = new StringBuilder();
  
  
  public Map<String, Integer> parameterDefs = new LinkedHashMap<>();
  public Map<String, JexlNode> defs = new LinkedHashMap<>();
  
  public final Queue<JexlNode> queue 
      = Collections.asLifoQueue(new ArrayDeque<JexlNode>());
  
  protected volatile JexlNode last;
  protected volatile JexlNode current;
  
  
  /** The cause of the issue to debug. */
  public JexlNode cause = null;

  /** The starting character location offset of the cause in the builder. */
  public int start = 0;

  /** The ending character location offset of the cause in the builder. */
  public int end = 0;

  /** The indentation level. */
  public int indentLevel = 0;

  /** Perform indentation?. */
  public int indent = 2;
  
  
  
  /**
  * Creates a Debugger.
  */
  public JexlWalker() {
  }
  
  public void reset() {
    builder.delete(0, builder.length());
    cause = null;
    start = end = indentLevel = indent = 0;
    parameterDefs.clear();
    defs.clear();
    queue.clear();
    last = current = null;
  }
  
  protected JexlNode top() {
    if (queue.isEmpty()) return null;
    return queue.peek();
  }
  
  /**
  * Position the debugger on the root of an expression.
  * @param jscript the expression
  * @return true if the expression was a {@link Script} instance,
  * false otherwise
  */
  public boolean debug(JexlExpression jscript) {
    if (jscript instanceof Script) {
      return debug(SCRIPT_SCRIPT.<JexlNode>getValue(jscript));
    } else {
      return false;
    }
  }

  /**
  * Position the debugger on the root of a script.
  * @param jscript the script
  * @return true if the script was a {@link Script} instance,
  * false otherwise
  */
  public boolean debug(JexlScript jscript) {
    if (jscript instanceof Script) {
      return debug(SCRIPT_SCRIPT.<JexlNode>getValue(jscript));
    } else {
      return false;
    }
  }

  /**
  * Seeks the location of an error cause (a node) in an expression.
  * @param node the node to debug
  * @return true if the cause was located, false otherwise
  */
  public boolean debug(JexlNode node) {
    return debug(node, true);
  }

  /**
  * Seeks the location of an error cause (a node) in an expression.
  * @param node the node to debug
  * @param r whether we should actively find the root node of the debugged node
  * @return true if the cause was located, false otherwise
  */
  public boolean debug(JexlNode node, boolean r) {
    reset();
    start = 0;
    end = 0;
    indentLevel = 0;
    if (node != null) {
      builder.setLength(0);
      cause = node;
      // make arg cause become the root cause
      JexlNode walk = node;
      if (r) {
        while (walk.jjtGetParent() != null) {
          walk = walk.jjtGetParent();
        }
      }
      acceptNode(walk, null);
    }
    return end > 0;
  }

  /**
  * @return The rebuilt expression
  */
  @Override
  public String toString() {
    return builder.toString();
  }

  /**
  * Rebuilds an expression from a JEXL node.
  * @param node the node to rebuilt from
  * @return the rebuilt expression
  * @since 3.0
  */
  public String data(JexlNode node) {
    
    start = 0;
    end = 0;
    indentLevel = 0;
    if (node != null) {
      reset();
      builder.setLength(0);
      cause = node;
      acceptNode(node, null);
    }
    return builder.toString();
  }

  /**
  * @return The starting offset location of the cause in the expression
  */
  @Override
  public int start() {
    return start;
  }

  /**
  * @return The end offset location of the cause in the expression
  */
  @Override
  public int end() {
    return end;
  }

  /**
  * Sets the indentation level.
  * @param level the number of spaces for indentation, none if less or equal to zero
  */
  public void setIndentation(int level) {
    if (level <= 0) {
      indent = 0;
    } else {
      indent = level;
    }
    indentLevel = 0;
  }
  
  
  public V accept(JexlNode node, V data) {
    reset();
    return acceptNode(node, data);
  }

  /**
  * Checks if a child node is the cause to debug 
  * and adds its representation to the rebuilt expression.
  * @param node the child node
  * @param data visitor pattern argument
  * @return visitor pattern value
  */
  public V acceptNode(JexlNode node, V data) {
    if (node == cause) {
      start = builder.length();
    }
    V value = (V) node.jjtAccept(this, (Object) data);
    if (node == cause) {
      end = builder.length();
    }
    return value;
  }
  
  /**
  * Adds a statement node to the rebuilt expression.
  * @param child the child node
  * @param data  visitor pattern argument
  * @return visitor pattern value
  */
  public V acceptStatement(JexlNode child, V data) {
    JexlNode parent = child.jjtGetParent();
    if (indent > 0 &&
    (parent instanceof ASTBlock || parent instanceof ASTJexlScript))
    {
      for (int i = 0; i < indentLevel; ++i) {
        for (int s = 0; s < indent; ++s) {
          builder.append(' ');
        }
      }
    }
    V value = acceptNode(child, data);
    // blocks, if, for & while dont need a ';' at end
    if (!(child instanceof ASTJexlScript
      || child instanceof ASTBlock
      || child instanceof ASTIfStatement
      || child instanceof ASTForeachStatement
      || child instanceof ASTWhileStatement))
    {
      builder.append(';');
      if (indent > 0) {
        builder.append('\n');
      } else {
        builder.append(' ');
      }
    }
    return value;
  }

  /**
  * Checks if a terminal node is the the cause to debug 
  * and adds its representation to the rebuilt expression.
  * @param node  the child node
  * @param image the child node token image (may be null)
  * @param data  visitor pattern argument
  * @return visitor pattern value
  */
  @Helper
  public V check(JexlNode node, String image, V data) {
    if (node == cause) {
      start = builder.length();
    }
    if (image != null) {
      builder.append(image);
    } else {
      builder.append(node.toString());
    }
    if (node == cause) {
      end = builder.length();
    }
    return data;
  }

  /**
  * Checks if the children of a node using infix notation is the cause to debug, adds the child node(s) representation to the
  * rebuilt expression.
  * @param node  the child node
  * @param infix the child node token
  * @param paren whether the child should be parenthesized
  * @param data  visitor pattern argument
  * @return visitor pattern value
  */
  public V infixChildren(JexlNode node, String infix, boolean paren, V data) {
    //child.jjtGetNumChildren() > 1;
    int num = node.jjtGetNumChildren();
    if (paren) {
      builder.append('(');
    }
    for (int i = 0; i < num; ++i) {
      if (i > 0) {
        builder.append(infix);
      }
      acceptNode(node.jjtGetChild(i), data);
    }
    if (paren) {
      builder.append(')');
    }
    return data;
  }

  /** Checks identifiers that contain spaces or punctuation
  * (but underscore, at-sign, sharp-sign and dollar).
  */
  public static final Matcher QUOTED_IDENTIFIER
    = Pattern.compile("[\\s]|[\\p{Punct}&&[^@#\\$_]]").matcher("");

  /**
  * Checks whether an identifier should be quoted or not.
  * @param str the identifier
  * @return true if needing quotes, false otherwise
  */
  public boolean needQuotes(String str) {
    return QUOTED_IDENTIFIER.reset(str).find()
      || "size".equals(str)
      || "empty".equals(str);
  }

  
  /**
  * A pseudo visitor for parameters.
  * @param p the parameter name
  * @param data the visitor argument
  * @return the parameter name to use
  */
  public String visitParameter(String name, V data) {
    return name;
  }

  public V visitFormalParameters(ASTJexlScript s, String[] params, V data) {
    for (String param : params) {
      parameterDefs.put(param, parameterDefs.size());
    }
    return data;
  }

  public V visitIdentifier(ASTIdentifier node, String name, V data) {
    defs.put(name, node);
    return data;
  }

  public V visitIdentifierAccess(ASTIdentifierAccess node, String name,
  JexlNode target, @Nullable String targetName, V data)
  {
    defs.put(name, node);
    return data;
  }
  
  public <T> V visitConstantValue(Constant<T> node, T value, Class<T> type,
  V data) 
  {
    System.err.printf(
      "Constant value: %s (%s) at %s\n",
      value, type.getName(), node
    );
    return data;
  }
  
  /**
  _______________________/ Visitor Implementation \_______________________
  */
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Helper {

    Class<? extends JexlNode>[] value() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface LiteralValue {

    Class<?> value() default Object.class;
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface BinaryOp {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Repr {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface UnaryOp {

    String value() default "";
  }

  /**
  * Rebuilds an additive expression.
  * @param node the node
  * @param op   the operator
  * @param data visitor pattern argument
  * @return visitor pattern value
  */
  @Helper({ ASTAddNode.class, ASTSubNode.class })
  protected V additiveNode(JexlNode node, String op, V data) {
    // need parenthesis if not in operator precedence order
    boolean paren = node.jjtGetParent() instanceof ASTMulNode || node.jjtGetParent() instanceof ASTDivNode || node.jjtGetParent() instanceof ASTModNode;
    int num = node.jjtGetNumChildren();
    if (paren) {
      builder.append('(');
    }
    acceptNode(node.jjtGetChild(0), data);
    for (int i = 1; i < num; ++i) {
      builder.append(op);
      acceptNode(node.jjtGetChild(i), data);
    }
    if (paren) {
      builder.append(')');
    }
    return data;
  }

  /**
  * Checks if the child of a node using prefix notation is the cause to 
  * debug. Adds the child node(s) representation to the
  * rebuilt expression.
  * @param node   the node
  * @param prefix the node token
  * @param data   visitor pattern argument
  * @return visitor pattern value
  */
  @Helper({ ASTUnaryMinusNode.class, ASTBitwiseComplNode.class })
  protected V prefixChild(JexlNode node, String prefix, V data) {
    boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
    builder.append(prefix);
    if (paren) {
      builder.append('(');
    }
    acceptNode(node.jjtGetChild(0), data);
    if (paren) {
      builder.append(')');
    }
    return data;
  }

  @BinaryOp("+")
  protected V atAdd(ASTAddNode node, V data) {
    return additiveNode(node, " + ", data);
  }

  @BinaryOp("-")
  protected V atSub(ASTSubNode node, V data) {
    return additiveNode(node, " - ", data);
  }

  @BinaryOp("&&")
  protected V atAnd(ASTAndNode node, V data) {
    return infixChildren(node, " && ", false, data);
  }

  @Repr("[$c0][$c1]..[$c#]")
  protected V atArrayAccess(ASTArrayAccess node, V data) {
    int num = node.jjtGetNumChildren();
    for (int i = 0; i < num; ++i) {
      builder.append('[');
      acceptNode(node.jjtGetChild(i), data);
      builder.append(']');
    }
    return data;
  }

  protected V atStringLiteral(ASTStringLiteral node, V data) {
    data = _doVisitConstant(node, data);
    builder.append('\'')
           .append(node.getLiteral().replace("'", "\\'"))
           .append('\'');
    return data;
  }
  
  protected V atExtendedLiteral(ASTExtendedLiteral node, V data) {
    builder.append("...");
    return data;
  }
  
  protected V atArrayLiteral(ASTArrayLiteral node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append("[ ");
    if (num > 0) {
      acceptNode(node.jjtGetChild(0), data);
      for (int i = 1; i < num; ++i) {
        builder.append(", ");
        acceptNode(node.jjtGetChild(i), data);
      }
    }
    builder.append(" ]");
    return data;
  }

  protected V atRange(ASTRangeNode node, V data) {
    return infixChildren(node, " .. ", false, data);
  }

  protected V atAssignment(ASTAssignment node, V data) {
    return infixChildren(node, " = ", false, data);
  }

  protected V atBitwiseAnd(ASTBitwiseAndNode node, V data) {
    return infixChildren(node, " & ", false, data);
  }

  protected V atBitwiseCompl(ASTBitwiseComplNode node, V data) {
    return prefixChild(node, "~", data);
  }

  protected V atBitwiseOr(ASTBitwiseOrNode node, V data) {
    boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
    return infixChildren(node, " | ", paren, data);
  }

  protected V atBitwiseXor(ASTBitwiseXorNode node, V data) {
    boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
    return infixChildren(node, " ^ ", paren, data);
  }

  protected V atBlock(ASTBlock node, V data) {
    builder.append('{');
    if (indent > 0) {
      indentLevel += 1;
      builder.append('\n');
    } else {
      builder.append(' ');
    }
    int num = node.jjtGetNumChildren();
    for (int i = 0; i < num; ++i) {
      JexlNode child = node.jjtGetChild(i);
      acceptStatement(child, data);
    }
    if (indent > 0) {
      indentLevel -= 1;
      for (int i = 0; i < indentLevel; ++i) {
        for (int s = 0; s < indent; ++s) {
          builder.append(' ');
        }
      }
    }
    builder.append('}');
    return data;
  }

  protected V atDiv(ASTDivNode node, V data) {
    return infixChildren(node, " / ", false, data);
  }

  protected V atEmptyFunction(ASTEmptyFunction node, V data) {
    builder.append("empty ");
    acceptNode(node.jjtGetChild(0), data);
    return data;
  }

  protected V atEmptyMethod(ASTEmptyMethod node, V data) {
    acceptNode(node.jjtGetChild(0), data);
    check(node, ".empty()", data);
    return data;
  }

  protected V atEQ(ASTEQNode node, V data) {
    return infixChildren(node, " == ", false, data);
  }

  protected V atER(ASTERNode node, V data) {
    return infixChildren(node, " =~ ", false, data);
  }

  protected V atSW(ASTSWNode node, V data) {
    return infixChildren(node, " =^ ", false, data);
  }

  protected V atEW(ASTEWNode node, V data) {
    return infixChildren(node, " =$ ", false, data);
  }

  protected V atNSW(ASTNSWNode node, V data) {
    return infixChildren(node, " !^ ", false, data);
  }

  protected V atNEW(ASTNEWNode node, V data) {
    return infixChildren(node, " !$ ", false, data);
  }

  @Repr("false")
  protected V atFalse(ASTFalseNode node, V data) {
    return check(node, "false", data);
  }

  @Repr("continue")
  protected V atContinue(ASTContinue node, V data) {
    return check(node, "continue", data);
  }

  @Repr("break")
  protected V atBreak(ASTBreak node, V data) {
    return check(node, "break", data);
  }

  protected V atForeachStatement(ASTForeachStatement node, V data) {
    builder.append("for(");
    acceptNode(node.jjtGetChild(0), data);
    builder.append(" : ");
    acceptNode(node.jjtGetChild(1), data);
    builder.append(") ");
    if (node.jjtGetNumChildren() > 2) {
      acceptStatement(node.jjtGetChild(2), data);
    } else {
      builder.append(';');
    }
    return data;
  }

  protected V atGE(ASTGENode node, V data) {
    return infixChildren(node, " >= ", false, data);
  }

  protected V atGT(ASTGTNode node, V data) {
    return infixChildren(node, " > ", false, data);
  }

  protected V atIdentifier(ASTIdentifier node, V data) {
    String image = node.getName();
    visitIdentifier(node, image, data);
    if (needQuotes(image)) {
      // quote it
      image = "'" + image.replace("'", "\\'") + "'";
    }
    return check(node, image, data);
  }

  protected V atIdentifierAccess(ASTIdentifierAccess node, V data) {
    JexlNode target = top();
    
    String targetName = (target instanceof ASTIdentifier)
      ? ((ASTIdentifier) target).getName()
      : null;
    
    String memberName = node.getName();
    data = visitIdentifierAccess(
      node, memberName, target, targetName, data
    );
    builder.append(".");
    if (needQuotes(memberName)) {
      builder.append('\'')
             .append(memberName.replace("'", "\\'"))
             .append('\'');
    } else {
      builder.append(memberName);
    }
    return data;
  }

  protected V atIfStatement(ASTIfStatement node, V data) {
    builder.append("if (");
    acceptNode(node.jjtGetChild(0), data);
    builder.append(") ");
    if (node.jjtGetNumChildren() > 1) {
      acceptStatement(node.jjtGetChild(1), data);
      if (node.jjtGetNumChildren() > 2) {
        builder.append(" else ");
        acceptStatement(node.jjtGetChild(2), data);
      }
    } else {
      builder.append(';');
    }
    return data;
  }

  @LiteralValue(BigDecimal.class)
  protected V atNumberLiteral(ASTNumberLiteral node, V data) {
    data = _doVisitConstant(node, data);
    return check(node, node.toString(), data);
  }

  protected V atJexlScript(ASTJexlScript node, V data) {
    // if lambda, produce parameters
    if (node instanceof ASTJexlLambda) {
      JexlNode parent = node.jjtGetParent();
      // use lambda syntax if not assigned
      boolean named = parent instanceof ASTAssignment;
      if (named) {
        builder.append("function");
      }
      builder.append('(');
      String[] params = node.getParameters();
      if (params != null && params.length > 0) {
        data = visitFormalParameters(node, params, data);
        builder.append(visitParameter(params[0], data));
        for (int p = 1; p < params.length; ++p) {
          builder.append(", ");
          builder.append(visitParameter(params[p], data));
        }
      }
      builder.append(')');
      if (named) {
        builder.append(' ');
      } else {
        builder.append("->");
      }
    // we will need a block...
    }
    // no parameters or done with them
    int num = node.jjtGetNumChildren();
    if (num == 1 && !(node instanceof ASTJexlLambda)) {
      data = acceptNode(node.jjtGetChild(0), data);
    } else {
      for (int i = 0; i < num; ++i) {
        JexlNode child = node.jjtGetChild(i);
        acceptStatement(child, data);
      }
    }
    return data;
  }

  protected V atLE(ASTLENode node, V data) {
    return infixChildren(node, " <= ", false, data);
  }

  protected V atLT(ASTLTNode node, V data) {
    return infixChildren(node, " < ", false, data);
  }

  protected V atMapEntry(ASTMapEntry node, V data) {
    acceptNode(node.jjtGetChild(0), data);
    builder.append(" : ");
    acceptNode(node.jjtGetChild(1), data);
    return data;
  }

  protected V atSetLiteral(ASTSetLiteral node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append("{ ");
    if (num > 0) {
      acceptNode(node.jjtGetChild(0), data);
      for (int i = 1; i < num; ++i) {
        builder.append(",");
        acceptNode(node.jjtGetChild(i), data);
      }
    }
    builder.append(" }");
    return data;
  }

  protected V atMapLiteral(ASTMapLiteral node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append("{ ");
    if (num > 0) {
      acceptNode(node.jjtGetChild(0), data);
      for (int i = 1; i < num; ++i) {
        builder.append(",");
        acceptNode(node.jjtGetChild(i), data);
      }
    } else {
      builder.append(':');
    }
    builder.append(" }");
    return data;
  }

  protected V atConstructor(ASTConstructorNode node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append("new(");
    acceptNode(node.jjtGetChild(0), data);
    for (int i = 1; i < num; ++i) {
      builder.append(", ");
      acceptNode(node.jjtGetChild(i), data);
    }
    builder.append(")");
    return data;
  }

  protected V atFunction(ASTFunctionNode node, V data) {
    int num = node.jjtGetNumChildren();
    if (num == 3) {
      acceptNode(node.jjtGetChild(0), data);
      builder.append(":");
      acceptNode(node.jjtGetChild(1), data);
      acceptNode(node.jjtGetChild(2), data);
    } else {
      acceptNode(node.jjtGetChild(0), data);
      acceptNode(node.jjtGetChild(1), data);
    }
    return data;
  }

  protected V atMethod(ASTMethodNode node, V data) {
    int num = node.jjtGetNumChildren();
    if (num == 2) {
      acceptNode(node.jjtGetChild(0), data);
      acceptNode(node.jjtGetChild(1), data);
    }
    return data;
  }

  protected V atArguments(ASTArguments node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append("(");
    if (num > 0) {
      acceptNode(node.jjtGetChild(0), data);
      for (int i = 1; i < num; ++i) {
        builder.append(", ");
        acceptNode(node.jjtGetChild(i), data);
      }
    }
    builder.append(")");
    return data;
  }

  protected V atMod(ASTModNode node, V data) {
    return infixChildren(node, " % ", false, data);
  }

  protected V atMul(ASTMulNode node, V data) {
    return infixChildren(node, " * ", false, data);
  }

  protected V atNE(ASTNENode node, V data) {
    return infixChildren(node, " != ", false, data);
  }

  protected V atNR(ASTNRNode node, V data) {
    return infixChildren(node, " !~ ", false, data);
  }

  protected V atNot(ASTNotNode node, V data) {
    builder.append("!");
    acceptNode(node.jjtGetChild(0), data);
    return data;
  }

  @Repr("null")
  protected V atNullLiteral(ASTNullLiteral node, V data) {
    check(node, "null", data);
    return data;
  }

  protected V atOr(ASTOrNode node, V data) {
    // need parenthesis if not in operator precedence order
    boolean paren = node.jjtGetParent() instanceof ASTAndNode;
    return infixChildren(node, " || ", paren, data);
  }

  protected V atReference(ASTReference node, V data) {
    int num = node.jjtGetNumChildren();
    for (int i = 0; i < num; ++i) {
      acceptNode(node.jjtGetChild(i), data);
    }
    return data;
  }

  protected V atReferenceExpression(ASTReferenceExpression node, V data) {
    JexlNode first = node.jjtGetChild(0);
    builder.append('(');
    acceptNode(first, data);
    builder.append(')');
    int num = node.jjtGetNumChildren();
    for (int i = 1; i < num; ++i) {
      builder.append("[");
      acceptNode(node.jjtGetChild(i), data);
      builder.append("]");
    }
    return data;
  }

  @Repr("return $c0")
  protected V atReturnStatement(ASTReturnStatement node, V data) {
    builder.append("return ");
    acceptNode(node.jjtGetChild(0), data);
    return data;
  }

  @Repr("size($c0)")
  protected V atSizeFunction(ASTSizeFunction node, V data) {
    builder.append("size ");
    acceptNode(node.jjtGetChild(0), data);
    return data;
  }

  @Repr("$c0.size()")
  protected V atSizeMethod(ASTSizeMethod node, V data) {
    acceptNode(node.jjtGetChild(0), data);
    check(node, ".size()", data);
    return data;
  }

  @LiteralValue(String.class)
  protected V atASTStringLiteral(ASTStringLiteral node, V data) {
    data = _doVisitConstant(node, data);
    String img = node.getLiteral().replace("'", "\\'");
    return check(node, "'" + img + "'", data);
  }

  protected V atTernary(ASTTernaryNode node, V data) {
    acceptNode(node.jjtGetChild(0), data);
    if (node.jjtGetNumChildren() > 2) {
      builder.append("? ");
      acceptNode(node.jjtGetChild(1), data);
      builder.append(" : ");
      acceptNode(node.jjtGetChild(2), data);
    } else {
      builder.append("?:");
      acceptNode(node.jjtGetChild(1), data);
    }
    return data;
  }
  
  private <T> V _doVisitConstant(Constant<T> node, V data) {
    Class<?> cls = node.getClass();
    Class<?> constantType = Void.class;
    outer:
    while (cls != null) {
      final Type[] ifaces = cls.getGenericInterfaces();
      for (int i=0, len=ifaces.length; i<len; ++i) {
        Type ifaceType = ifaces[i];
        if (! (ifaceType instanceof ParameterizedType)) continue;
        ParameterizedType pt = (ParameterizedType) ifaceType;
        if (! (pt.getRawType() instanceof Class<?>)) continue;
        Class<?> rawType = (Class<?>) pt.getRawType();
        if (! Constant.class.isAssignableFrom(rawType)) continue;        
        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length != 1) 
          throw new RuntimeException(String.format(
            "typeArgs.length != 1 (%s)", pt));
        Type typeArg = typeArgs[0];
        while (typeArg instanceof ParameterizedType) {
          typeArg = ((ParameterizedType) typeArg).getRawType();
        }
        if (! (typeArg instanceof Class<?>)) {
          System.err.printf(
            "[WARN] expected Constant<?> implementation with a concrete "
            + "type argument, but got the [%s] '%s' (Type: %s)\n",
            typeArg.getClass().getSimpleName(),
            typeArg, pt.toString()
          );
          constantType = Object.class;
        } else {
          constantType = (Class<?>) typeArg;
        }
        break outer;
      }
      cls = cls.getSuperclass();
    }
    
    return this.<T>visitConstantValue(
      (Constant<T>) node,
      (T) (Object) node.getLiteral(),
      (Class<T>) (Class<?>) constantType,
      data
    );
  }
  
  protected V atTrue(ASTTrueNode node, V data) {
    check(node, "true", data);
    return data;
  }
  
  protected V atUnaryMinus(ASTUnaryMinusNode node, V data) {
    return prefixChild(node, "-", data);
  }
  
  protected V atVar(ASTVar node, V data) {
    builder.append("var ");
    check(node, node.getName(), data);
    return data;
  }

  protected V atWhileStatement(ASTWhileStatement node, V data) {
    builder.append("while (");
    acceptNode(node.jjtGetChild(0), data);
    builder.append(") ");
    if (node.jjtGetNumChildren() > 1) {
      acceptStatement(node.jjtGetChild(1), data);
    } else {
      builder.append(';');
    }
    return data;
  }

  protected V atSetAdd(ASTSetAddNode node, V data) {
    return infixChildren(node, " += ", false, data);
  }

  protected V atSetSub(ASTSetSubNode node, V data) {
    return infixChildren(node, " -= ", false, data);
  }

  protected V atSetMult(ASTSetMultNode node, V data) {
    return infixChildren(node, " *= ", false, data);
  }

  protected V atSetDiv(ASTSetDivNode node, V data) {
    return infixChildren(node, " /= ", false, data);
  }

  protected V atSetMod(ASTSetModNode node, V data) {
    return infixChildren(node, " %= ", false, data);
  }

  protected V atSetAnd(ASTSetAndNode node, V data) {
    return infixChildren(node, " &= ", false, data);
  }

  protected V atSetOr(ASTSetOrNode node, V data) {
    return infixChildren(node, " |= ", false, data);
  }

  protected V atSetXor(ASTSetXorNode node, V data) {
    return infixChildren(node, " ^= ", false, data);
  }

  protected V atJxltLiteral(ASTJxltLiteral node, V data) {
    String img = node.getLiteral().replace("`", "\\`");
    return check(node, "`" + img + "`", data);
  }

  protected V atAnnotation(ASTAnnotation node, V data) {
    int num = node.jjtGetNumChildren();
    builder.append('@');
    builder.append(node.getName());
    if (num > 0) {
      builder.append("(");
      acceptNode(node.jjtGetChild(0), data);
      for (int i = 0; i < num; ++i) {
        builder.append(", ");
        JexlNode child = node.jjtGetChild(i);
        acceptStatement(child, data);
      }
      builder.append(")");
    }
    //null;
    return data;
  }

  protected V atAnnotatedStatement(ASTAnnotatedStatement node, V data) {
    int num = node.jjtGetNumChildren();
    for (int i = 0; i < num; ++i) {
      JexlNode child = node.jjtGetChild(i);
      acceptStatement(child, data);
    }
    return data;
  }
  
    
  /*
  @Override protected final V acceptStatement(JexlNode child, Object data) {
    return this.acceptStatement(child, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final V check(JexlNode node, String image, Object data) {
    return this.check(node, image, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final V infixChildren(JexlNode node, String infix, boolean paren, Object data) {
    return this.infixChildren(node, infix, paren, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final V visitParameter(String name, Object data) {
    return this.visitParameter(name, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final V prefixChild(JexlNode node, String prefix, Object data) {
    return this.prefixChild(node, prefix, (V) data);
    } finally {
      last = queue.poll();
    }
  }
*/ 


  @Override protected final Object visit(ASTAddNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atAdd(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSubNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSub(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTAndNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atAnd(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTArrayAccess node, Object data) {
    try {
      queue.offer(current = node);
      return this.atArrayAccess(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTExtendedLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atExtendedLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTArrayLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atArrayLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTRangeNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atRange(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTAssignment node, Object data) {
    try {
      queue.offer(current = node);
      return this.atAssignment(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBitwiseAndNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBitwiseAnd(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBitwiseComplNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBitwiseCompl(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBitwiseOrNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBitwiseOr(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBitwiseXorNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBitwiseXor(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBlock node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBlock(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTDivNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atDiv(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTEmptyFunction node, Object data) {
    try {
      queue.offer(current = node);
      return this.atEmptyFunction(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTEmptyMethod node, Object data) {
    try {
      queue.offer(current = node);
      return this.atEmptyMethod(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTEQNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atEQ(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTERNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atER(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSWNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSW(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTEWNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atEW(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNSWNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNSW(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNEWNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNEW(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTFalseNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atFalse(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTContinue node, Object data) {
    try {
      queue.offer(current = node);
      return this.atContinue(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTBreak node, Object data) {
    try {
      queue.offer(current = node);
      return this.atBreak(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTForeachStatement node, Object data) {
    try {
      queue.offer(current = node);
      return this.atForeachStatement(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTGENode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atGE(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTGTNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atGT(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTIdentifier node, Object data) {
    try {
      queue.offer(current = node);
      return this.atIdentifier(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTIdentifierAccess node, Object data) {
    try {
      queue.offer(current = node);
      return this.atIdentifierAccess(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTIfStatement node, Object data) {
    try {
      queue.offer(current = node);
      return this.atIfStatement(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNumberLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNumberLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTJexlScript node, Object data) {
    try {
      queue.offer(current = node);
      return this.atJexlScript(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTLENode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atLE(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTLTNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atLT(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTMapEntry node, Object data) {
    try {
      queue.offer(current = node);
      return this.atMapEntry(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTMapLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atMapLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTConstructorNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atConstructor(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTFunctionNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atFunction(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTMethodNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atMethod(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTArguments node, Object data) {
    try {
      queue.offer(current = node);
      return this.atArguments(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTModNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atMod(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTMulNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atMul(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNENode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNE(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNRNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNR(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNotNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNot(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTNullLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atNullLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTOrNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atOr(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTReference node, Object data) {
    try {
      queue.offer(current = node);
      return this.atReference(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTReferenceExpression node, Object data) {
    try {
      queue.offer(current = node);
      return this.atReferenceExpression(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTReturnStatement node, Object data) {
    try {
      queue.offer(current = node);
      return this.atReturnStatement(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSizeFunction node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSizeFunction(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSizeMethod node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSizeMethod(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTStringLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atStringLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTTernaryNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atTernary(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTTrueNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atTrue(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTUnaryMinusNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atUnaryMinus(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTVar node, Object data) {
    try {
      queue.offer(current = node);
      return this.atVar(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTWhileStatement node, Object data) {
    try {
      queue.offer(current = node);
      return this.atWhileStatement(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetAddNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetAdd(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetSubNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetSub(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetMultNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetMult(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetDivNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetDiv(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetModNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetMod(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetAndNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetAnd(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetOrNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetOr(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTSetXorNode node, Object data) {
    try {
      queue.offer(current = node);
      return this.atSetXor(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  @Override protected final Object visit(ASTJxltLiteral node, Object data) {
    try {
      queue.offer(current = node);
      return this.atJxltLiteral(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  // @Override
  protected final Object visit(ASTAnnotation node, Object data) {
    try {
      queue.offer(current = node);
      return this.atAnnotation(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
  // @Override
  protected final Object visit(ASTAnnotatedStatement node, Object data) {
    try {
      queue.offer(current = node);
      return this.atAnnotatedStatement(node, (V) data);
    } finally {
      last = queue.poll();
    }
  }
}

