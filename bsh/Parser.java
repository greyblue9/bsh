package bsh;

import bsh.BSHAllocationExpression;
import bsh.BSHAmbiguousName;
import bsh.BSHArguments;
import bsh.BSHArrayDimensions;
import bsh.BSHArrayInitializer;
import bsh.BSHAssignment;
import bsh.BSHBinaryExpression;
import bsh.BSHBlock;
import bsh.BSHCastExpression;
import bsh.BSHClassDeclaration;
import bsh.BSHEnhancedForStatement;
import bsh.BSHForStatement;
import bsh.BSHFormalComment;
import bsh.BSHFormalParameter;
import bsh.BSHFormalParameters;
import bsh.BSHIfStatement;
import bsh.BSHImportDeclaration;
import bsh.BSHLiteral;
import bsh.BSHMethodDeclaration;
import bsh.BSHMethodInvocation;
import bsh.BSHPackageDeclaration;
import bsh.BSHPrimaryExpression;
import bsh.BSHPrimarySuffix;
import bsh.BSHPrimitiveType;
import bsh.BSHReturnStatement;
import bsh.BSHReturnType;
import bsh.BSHStatementExpressionList;
import bsh.BSHSwitchLabel;
import bsh.BSHSwitchStatement;
import bsh.BSHTernaryExpression;
import bsh.BSHThrowStatement;
import bsh.BSHTryStatement;
import bsh.BSHType;
import bsh.BSHTypedVariableDeclaration;
import bsh.BSHUnaryExpression;
import bsh.BSHVariableDeclarator;
import bsh.BSHWhileStatement;
import bsh.Interpreter;
import bsh.JJTParserState;
import bsh.JavaCharStream;
import bsh.Modifiers;
import bsh.Node;
import bsh.ParseEvent;
import bsh.ParseException;
import bsh.ParserConstants;
import bsh.ParserTokenManager;
import bsh.ParserTreeConstants;
import bsh.Primitive;
import bsh.SimpleNode;
import bsh.Token;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class Parser implements ParserTreeConstants, ParserConstants {
  protected JJTParserState jjtree;
  boolean retainComments;
  public ParserTokenManager token_source;
  JavaCharStream jj_input_stream;
  public Token token;
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos;
  private Token jj_lastpos;
  private int jj_la;
  private boolean jj_lookingAhead;
  private boolean jj_semLA;
  private final Parser.LookaheadSuccess jj_ls;

  public void setRetainComments(boolean b) {
    this.retainComments = b;
  }

  void jjtreeOpenNodeScope(Node n) {
    ((SimpleNode)n).firstToken = this.getToken(1);
  }

  void jjtreeCloseNodeScope(Node n) {
    ((SimpleNode)n).lastToken = this.getToken(0);
  }

  void reInitInput(Reader in) {
    this.ReInit(in);
  }

  public SimpleNode popNode() {
    return this.jjtree.nodeArity() > 0?(SimpleNode)this.jjtree.popNode():null;
  }

  void reInitTokenInput(Reader in) {
    this.jj_input_stream.ReInit(in, this.jj_input_stream.getEndLine(), this.jj_input_stream.getEndColumn());
  }

  public static void main(String[] args) throws IOException, ParseException {
    boolean print = false;
    int i = 0;
    if(args[0].equals("-p")) {
      ++i;
      print = true;
    }

    while(i < args.length) {
      FileReader in = new FileReader(args[i]);
      Parser parser = new Parser(in);
      parser.setRetainComments(true);

      while(!parser.Line()) {
        if(print) {
          System.out.println(parser.popNode());
        }
      }

      ++i;
    }

  }

  boolean isRegularForStatement() {
    byte curTok = 1;
    int var3 = curTok + 1;
    Token tok = this.getToken(curTok);
    if(tok.kind != 30) {
      return false;
    } else {
      tok = this.getToken(var3++);
      if(tok.kind != 73) {
        return false;
      } else {
        while(true) {
          tok = this.getToken(var3++);
          switch(tok.kind) {
          case 0:
            return false;
          case 79:
            return true;
          case 90:
            return false;
          }
        }
      }
    }
  }

  ParseException createParseException(String message, Exception e) {
    Token errortok = this.token;
    int line = errortok.beginLine;
    int column = errortok.beginColumn;
    String var10000;
    if(errortok.kind == 0) {
      var10000 = tokenImage[0];
    } else {
      var10000 = errortok.image;
    }

    return new ParseException("Parse error at line " + line + ", column " + column + " : " + message, e);
  }

  int parseInt(String s) throws NumberFormatException {
    byte radix;
    int i;
    if(!s.startsWith("0x") && !s.startsWith("0X")) {
      if(s.startsWith("0") && s.length() > 1) {
        radix = 8;
        i = 1;
      } else {
        radix = 10;
        i = 0;
      }
    } else {
      radix = 16;
      i = 2;
    }

    int result = 0;

    for(int len = s.length(); i < len; ++i) {
      if(result < 0) {
        throw new NumberFormatException("Number too big for integer type: " + s);
      }

      result *= radix;
      int digit = Character.digit(s.charAt(i), radix);
      if(digit < 0) {
        throw new NumberFormatException("Invalid integer type: " + s);
      }

      result += digit;
    }

    return result;
  }

  long parseLong(String s) throws NumberFormatException {
    byte radix;
    int i;
    if(!s.startsWith("0x") && !s.startsWith("0X")) {
      if(s.startsWith("0") && s.length() > 1) {
        radix = 8;
        i = 1;
      } else {
        radix = 10;
        i = 0;
      }
    } else {
      radix = 16;
      i = 2;
    }

    long result = 0L;

    for(int len = s.length(); i < len; ++i) {
      if(result < 0L) {
        throw new NumberFormatException("Number too big for long type: " + s);
      }

      result *= (long)radix;
      int digit = Character.digit(s.charAt(i), radix);
      if(digit < 0) {
        throw new NumberFormatException("Invalid long type: " + s);
      }

      result += (long)digit;
    }

    return result;
  }

  public boolean Line(boolean stopOnEof) throws ParseException {
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 0:
      Interpreter.debug("End of File!");
      this.jj_consume_token(0);
      Object result = ParseEvent.EOF.fire(this, new Object[0]);
      boolean shouldEnd = stopOnEof;
      if(result instanceof Boolean) {
        shouldEnd = ((Boolean)result).booleanValue();
      }

      return shouldEnd;
    default:
      if(this.jj_2_1(1)) {
        this.BlockStatement();
        return false;
      } else {
        this.jj_consume_token(-1);
        throw this.createParseException("Malformed input; missing return?", new Exception("Error in Line(boolean stopOnEof)"));
      }
    }
  }

  public boolean Line() throws ParseException {
    return this.Line(true);
  }

  public final Modifiers Modifiers(int context, boolean lookahead) throws ParseException {
    Modifiers mods = null;

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 10:
      case 27:
      case 39:
      case 43:
      case 44:
      case 45:
      case 48:
      case 49:
      case 51:
      case 52:
      case 58:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 10:
          this.jj_consume_token(10);
          break;
        case 27:
          this.jj_consume_token(27);
          break;
        case 39:
          this.jj_consume_token(39);
          break;
        case 43:
          this.jj_consume_token(43);
          break;
        case 44:
          this.jj_consume_token(44);
          break;
        case 45:
          this.jj_consume_token(45);
          break;
        case 48:
          this.jj_consume_token(48);
          break;
        case 49:
          this.jj_consume_token(49);
          break;
        case 51:
          this.jj_consume_token(51);
          break;
        case 52:
          this.jj_consume_token(52);
          break;
        case 58:
          this.jj_consume_token(58);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        if(!lookahead) {
          try {
            if(mods == null) {
              mods = new Modifiers();
            }

            mods.addModifier(context, this.getToken(0).image);
          } catch (IllegalStateException var5) {
            throw this.createParseException(var5.getMessage(), var5);
          }
        }
        break;
      default:
        return mods;
      }
    }
  }

  public final void ClassDeclaration() throws ParseException {
    BSHClassDeclaration jjtn000 = new BSHClassDeclaration(1);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Modifiers mods = this.Modifiers(0, false);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 13:
        this.jj_consume_token(13);
        break;
      case 37:
        this.jj_consume_token(37);
        jjtn000.isInterface = true;
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }

      Token name = this.jj_consume_token(70);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 25:
        this.jj_consume_token(25);
        this.AmbiguousName();
        jjtn000.extend = true;
      }

      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 33:
        this.jj_consume_token(33);
        int numInterfaces = this.NameList();
        jjtn000.numInterfaces = numInterfaces;
      }

      this.Block();
      this.jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      this.jjtreeCloseNodeScope(jjtn000);
      jjtn000.modifiers = mods;
      jjtn000.name = name.image;
    } catch (Throwable var10) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var10 instanceof RuntimeException) {
        throw (RuntimeException)var10;
      }

      if(var10 instanceof ParseException) {
        throw (ParseException)var10;
      }

      throw (Error)var10;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void MethodDeclaration() throws ParseException {
    BSHMethodDeclaration jjtn000 = new BSHMethodDeclaration(2);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Token t = null;

    try {
      Modifiers mods = this.Modifiers(2, false);
      jjtn000.modifiers = mods;
      if(this.jj_2_2(Integer.MAX_VALUE)) {
        t = this.jj_consume_token(70);
        jjtn000.name = t.image;
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 11:
        case 14:
        case 17:
        case 22:
        case 29:
        case 36:
        case 38:
        case 47:
        case 57:
        case 70:
          this.ReturnType();
          t = this.jj_consume_token(70);
          jjtn000.name = t.image;
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }

      this.FormalParameters();
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 54:
        this.jj_consume_token(54);
        int count = this.NameList();
        jjtn000.numThrows = count;
      default:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 75:
          this.Block();
          break;
        case 76:
        case 77:
        case 78:
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        case 79:
          this.jj_consume_token(79);
        }
      }
    } catch (Throwable var10) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var10 instanceof RuntimeException) {
        throw (RuntimeException)var10;
      }

      if(var10 instanceof ParseException) {
        throw (ParseException)var10;
      }

      throw (Error)var10;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void PackageDeclaration() throws ParseException {
    BSHPackageDeclaration jjtn000 = new BSHPackageDeclaration(3);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(42);
      this.AmbiguousName();
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ImportDeclaration() throws ParseException {
    BSHImportDeclaration jjtn000 = new BSHImportDeclaration(4);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Token s = null;
    Token t = null;

    try {
      if(this.jj_2_3(3)) {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 48:
          s = this.jj_consume_token(48);
        default:
          this.jj_consume_token(34);
          this.AmbiguousName();
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 81:
            t = this.jj_consume_token(81);
            this.jj_consume_token(105);
          default:
            this.jj_consume_token(79);
            this.jjtree.closeNodeScope(jjtn000, true);
            jjtc000 = false;
            this.jjtreeCloseNodeScope(jjtn000);
            if(s != null) {
              jjtn000.staticImport = true;
            }

            if(t != null) {
              jjtn000.importPackage = true;
            }
          }
        }
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 34:
          this.jj_consume_token(34);
          this.jj_consume_token(105);
          this.jj_consume_token(79);
          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          jjtn000.superImport = true;
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var9) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var9 instanceof RuntimeException) {
        throw (RuntimeException)var9;
      }

      if(var9 instanceof ParseException) {
        throw (ParseException)var9;
      }

      throw (Error)var9;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void VariableDeclarator() throws ParseException {
    BSHVariableDeclarator jjtn000 = new BSHVariableDeclarator(5);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Token t = this.jj_consume_token(70);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 82:
        this.jj_consume_token(82);
        this.VariableInitializer();
      default:
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.name = t.image;
      }
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void VariableInitializer() throws ParseException {
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 11:
    case 14:
    case 17:
    case 22:
    case 26:
    case 29:
    case 36:
    case 38:
    case 40:
    case 41:
    case 47:
    case 55:
    case 57:
    case 60:
    case 64:
    case 66:
    case 67:
    case 68:
    case 70:
    case 73:
    case 87:
    case 88:
    case 101:
    case 102:
    case 103:
    case 104:
      this.Expression();
      break;
    case 75:
      this.ArrayInitializer();
      break;
    default:
      this.jj_consume_token(-1);
      throw new ParseException();
    }

  }

  public final void ArrayInitializer() throws ParseException {
    BSHArrayInitializer jjtn000 = new BSHArrayInitializer(6);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(75);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 75:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.VariableInitializer();

        while(this.jj_2_4(2)) {
          this.jj_consume_token(80);
          this.VariableInitializer();
        }
      default:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 80:
          this.jj_consume_token(80);
        default:
          this.jj_consume_token(76);
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void FormalParameters() throws ParseException {
    BSHFormalParameters jjtn000 = new BSHFormalParameters(7);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(73);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 29:
      case 36:
      case 38:
      case 47:
      case 70:
        this.FormalParameter();

        label115:
        while(true) {
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 80:
            this.jj_consume_token(80);
            this.FormalParameter();
            break;
          default:
            break label115;
          }
        }
      default:
        this.jj_consume_token(74);
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void FormalParameter() throws ParseException {
    BSHFormalParameter jjtn000 = new BSHFormalParameter(8);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Token t;
      if(this.jj_2_5(2)) {
        this.Type();
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 135:
          this.jj_consume_token(135);
          jjtn000.setVarargs();
        default:
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 85:
            this.TypeArguments();
          default:
            t = this.jj_consume_token(70);
            this.jjtree.closeNodeScope(jjtn000, true);
            jjtc000 = false;
            this.jjtreeCloseNodeScope(jjtn000);
            jjtn000.name = t.image;
          }
        }
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 70:
          t = this.jj_consume_token(70);
          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          jjtn000.name = t.image;
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void Type() throws ParseException {
    BSHType jjtn000 = new BSHType(9);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 29:
      case 36:
      case 38:
      case 47:
        this.PrimitiveType();
        break;
      case 70:
        this.AmbiguousName();
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }

      while(this.jj_2_6(2)) {
        this.jj_consume_token(77);
        this.jj_consume_token(78);
        jjtn000.addArrayDimension();
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ReturnType() throws ParseException {
    BSHReturnType jjtn000 = new BSHReturnType(10);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 29:
      case 36:
      case 38:
      case 47:
      case 70:
        this.Type();
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 85:
          this.TypeArguments();
          return;
        default:
          return;
        }
      case 57:
        this.jj_consume_token(57);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.isVoid = true;
        return;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      } else if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      } else {
        throw (Error)var7;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void PrimitiveType() throws ParseException {
    BSHPrimitiveType jjtn000 = new BSHPrimitiveType(11);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
        this.jj_consume_token(11);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Boolean.TYPE;
        break;
      case 14:
        this.jj_consume_token(14);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Byte.TYPE;
        break;
      case 17:
        this.jj_consume_token(17);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Character.TYPE;
        break;
      case 22:
        this.jj_consume_token(22);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Double.TYPE;
        break;
      case 29:
        this.jj_consume_token(29);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Float.TYPE;
        break;
      case 36:
        this.jj_consume_token(36);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Integer.TYPE;
        break;
      case 38:
        this.jj_consume_token(38);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Long.TYPE;
        break;
      case 47:
        this.jj_consume_token(47);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.type = Short.TYPE;
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void AmbiguousName() throws ParseException {
    BSHAmbiguousName jjtn000 = new BSHAmbiguousName(12);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Token t = this.jj_consume_token(70);
      StringBuilder s = new StringBuilder(t.image);

      while(this.jj_2_7(2)) {
        this.jj_consume_token(81);
        t = this.jj_consume_token(70);
        s.append("." + t.image);
      }

      this.jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      this.jjtreeCloseNodeScope(jjtn000);
      jjtn000.text = s.toString();
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final int NameList() throws ParseException {
    byte count = 0;
    this.AmbiguousName();
    int var2 = count + 1;

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 80:
        this.jj_consume_token(80);
        this.AmbiguousName();
        ++var2;
        break;
      default:
        return var2;
      }
    }
  }

  public final void Expression() throws ParseException {
    if(this.jj_2_8(Integer.MAX_VALUE)) {
      this.Assignment();
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.ConditionalExpression();
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    }

  }

  public final void Assignment() throws ParseException {
    BSHAssignment jjtn000 = new BSHAssignment(13);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.PrimaryExpression();
      int op = this.AssignmentOperator();
      jjtn000.operator = op;
      this.Expression();
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final int AssignmentOperator() throws ParseException {
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 82:
      this.jj_consume_token(82);
      break;
    case 119:
      this.jj_consume_token(119);
      break;
    case 120:
      this.jj_consume_token(120);
      break;
    case 121:
      this.jj_consume_token(121);
      break;
    case 122:
      this.jj_consume_token(122);
      break;
    case 123:
      this.jj_consume_token(123);
      break;
    case 125:
      this.jj_consume_token(125);
      break;
    case 127:
      this.jj_consume_token(127);
      break;
    case 128:
      this.jj_consume_token(128);
      break;
    case 129:
      this.jj_consume_token(129);
      break;
    case 130:
      this.jj_consume_token(130);
      break;
    case 131:
      this.jj_consume_token(131);
      break;
    case 132:
      this.jj_consume_token(132);
      break;
    case 133:
      this.jj_consume_token(133);
      break;
    case 134:
      this.jj_consume_token(134);
      break;
    default:
      this.jj_consume_token(-1);
      throw new ParseException();
    }

    Token t = this.getToken(0);
    return t.kind;
  }

  public final void ConditionalExpression() throws ParseException {
    this.ConditionalOrExpression();
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 89:
      this.jj_consume_token(89);
      this.Expression();
      this.jj_consume_token(90);
      BSHTernaryExpression jjtn001 = new BSHTernaryExpression(14);
      boolean jjtc001 = true;
      this.jjtree.openNodeScope(jjtn001);
      this.jjtreeOpenNodeScope(jjtn001);

      try {
        this.ConditionalExpression();
      } catch (Throwable var7) {
        if(jjtc001) {
          this.jjtree.clearNodeScope(jjtn001);
          jjtc001 = false;
        } else {
          this.jjtree.popNode();
        }

        if(var7 instanceof RuntimeException) {
          throw (RuntimeException)var7;
        } else {
          if(var7 instanceof ParseException) {
            throw (ParseException)var7;
          }

          throw (Error)var7;
        }
      } finally {
        if(jjtc001) {
          this.jjtree.closeNodeScope(jjtn001, 3);
          this.jjtreeCloseNodeScope(jjtn001);
        }

      }
    default:
    }
  }

  public final void ConditionalOrExpression() throws ParseException {
    Token t = null;
    this.ConditionalAndExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 97:
      case 98:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 97:
          t = this.jj_consume_token(97);
          break;
        case 98:
          t = this.jj_consume_token(98);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.ConditionalAndExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void ConditionalAndExpression() throws ParseException {
    Token t = null;
    this.InclusiveOrExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 99:
      case 100:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 99:
          t = this.jj_consume_token(99);
          break;
        case 100:
          t = this.jj_consume_token(100);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.InclusiveOrExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void InclusiveOrExpression() throws ParseException {
    Token t = null;
    this.ExclusiveOrExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 109:
      case 110:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 109:
          t = this.jj_consume_token(109);
          break;
        case 110:
          t = this.jj_consume_token(110);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.ExclusiveOrExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void ExclusiveOrExpression() throws ParseException {
    Token t = null;
    this.AndExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 111:
        t = this.jj_consume_token(111);
        this.AndExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void AndExpression() throws ParseException {
    Token t = null;
    this.EqualityExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 107:
      case 108:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 107:
          t = this.jj_consume_token(107);
          break;
        case 108:
          t = this.jj_consume_token(108);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.EqualityExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void EqualityExpression() throws ParseException {
    Token t = null;
    this.InstanceOfExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 91:
      case 96:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 91:
          t = this.jj_consume_token(91);
          break;
        case 96:
          t = this.jj_consume_token(96);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.InstanceOfExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void InstanceOfExpression() throws ParseException {
    Token t = null;
    this.RelationalExpression();
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 35:
      t = this.jj_consume_token(35);
      this.Type();
      BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
      boolean jjtc001 = true;
      this.jjtree.openNodeScope(jjtn001);
      this.jjtreeOpenNodeScope(jjtn001);

      try {
        this.jjtree.closeNodeScope(jjtn001, 2);
        jjtc001 = false;
        this.jjtreeCloseNodeScope(jjtn001);
        jjtn001.kind = t.kind;
      } finally {
        if(jjtc001) {
          this.jjtree.closeNodeScope(jjtn001, 2);
          this.jjtreeCloseNodeScope(jjtn001);
        }

      }
    default:
    }
  }

  public final void RelationalExpression() throws ParseException {
    Token t = null;
    this.ShiftExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 83:
      case 84:
      case 85:
      case 86:
      case 92:
      case 93:
      case 94:
      case 95:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 83:
          t = this.jj_consume_token(83);
          break;
        case 84:
          t = this.jj_consume_token(84);
          break;
        case 85:
          t = this.jj_consume_token(85);
          break;
        case 86:
          t = this.jj_consume_token(86);
          break;
        case 87:
        case 88:
        case 89:
        case 90:
        case 91:
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        case 92:
          t = this.jj_consume_token(92);
          break;
        case 93:
          t = this.jj_consume_token(93);
          break;
        case 94:
          t = this.jj_consume_token(94);
          break;
        case 95:
          t = this.jj_consume_token(95);
        }

        this.ShiftExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      case 87:
      case 88:
      case 89:
      case 90:
      case 91:
      default:
        return;
      }
    }
  }

  public final void ShiftExpression() throws ParseException {
    Token t = null;
    this.AdditiveExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 113:
      case 114:
      case 115:
      case 116:
      case 117:
      case 118:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 113:
          t = this.jj_consume_token(113);
          break;
        case 114:
          t = this.jj_consume_token(114);
          break;
        case 115:
          t = this.jj_consume_token(115);
          break;
        case 116:
          t = this.jj_consume_token(116);
          break;
        case 117:
          t = this.jj_consume_token(117);
          break;
        case 118:
          t = this.jj_consume_token(118);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.AdditiveExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void AdditiveExpression() throws ParseException {
    Token t = null;
    this.MultiplicativeExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 103:
      case 104:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 103:
          t = this.jj_consume_token(103);
          break;
        case 104:
          t = this.jj_consume_token(104);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.MultiplicativeExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void MultiplicativeExpression() throws ParseException {
    Token t = null;
    this.UnaryExpression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 105:
      case 106:
      case 112:
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 105:
          t = this.jj_consume_token(105);
          break;
        case 106:
          t = this.jj_consume_token(106);
          break;
        case 112:
          t = this.jj_consume_token(112);
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }

        this.UnaryExpression();
        BSHBinaryExpression jjtn001 = new BSHBinaryExpression(15);
        boolean jjtc001 = true;
        this.jjtree.openNodeScope(jjtn001);
        this.jjtreeOpenNodeScope(jjtn001);

        try {
          this.jjtree.closeNodeScope(jjtn001, 2);
          jjtc001 = false;
          this.jjtreeCloseNodeScope(jjtn001);
          jjtn001.kind = t.kind;
          break;
        } finally {
          if(jjtc001) {
            this.jjtree.closeNodeScope(jjtn001, 2);
            this.jjtreeCloseNodeScope(jjtn001);
          }

        }
      default:
        return;
      }
    }
  }

  public final void UnaryExpression() throws ParseException {
    Token t = null;
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 11:
    case 14:
    case 17:
    case 22:
    case 26:
    case 29:
    case 36:
    case 38:
    case 40:
    case 41:
    case 47:
    case 55:
    case 57:
    case 60:
    case 64:
    case 66:
    case 67:
    case 68:
    case 70:
    case 73:
    case 87:
    case 88:
      this.UnaryExpressionNotPlusMinus();
      break;
    case 101:
      this.PreIncrementExpression();
      break;
    case 102:
      this.PreDecrementExpression();
      break;
    case 103:
    case 104:
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 103:
        t = this.jj_consume_token(103);
        break;
      case 104:
        t = this.jj_consume_token(104);
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }

      this.UnaryExpression();
      BSHUnaryExpression jjtn001 = new BSHUnaryExpression(16);
      boolean jjtc001 = true;
      this.jjtree.openNodeScope(jjtn001);
      this.jjtreeOpenNodeScope(jjtn001);

      try {
        this.jjtree.closeNodeScope(jjtn001, 1);
        jjtc001 = false;
        this.jjtreeCloseNodeScope(jjtn001);
        jjtn001.kind = t.kind;
        break;
      } finally {
        if(jjtc001) {
          this.jjtree.closeNodeScope(jjtn001, 1);
          this.jjtreeCloseNodeScope(jjtn001);
        }

      }
    default:
      this.jj_consume_token(-1);
      throw new ParseException();
    }

  }

  public final void PreIncrementExpression() throws ParseException {
    Token t = null;
    t = this.jj_consume_token(101);
    this.PrimaryExpression();
    BSHUnaryExpression jjtn001 = new BSHUnaryExpression(16);
    boolean jjtc001 = true;
    this.jjtree.openNodeScope(jjtn001);
    this.jjtreeOpenNodeScope(jjtn001);

    try {
      this.jjtree.closeNodeScope(jjtn001, 1);
      jjtc001 = false;
      this.jjtreeCloseNodeScope(jjtn001);
      jjtn001.kind = t.kind;
    } finally {
      if(jjtc001) {
        this.jjtree.closeNodeScope(jjtn001, 1);
        this.jjtreeCloseNodeScope(jjtn001);
      }

    }

  }

  public final void PreDecrementExpression() throws ParseException {
    Token t = null;
    t = this.jj_consume_token(102);
    this.PrimaryExpression();
    BSHUnaryExpression jjtn001 = new BSHUnaryExpression(16);
    boolean jjtc001 = true;
    this.jjtree.openNodeScope(jjtn001);
    this.jjtreeOpenNodeScope(jjtn001);

    try {
      this.jjtree.closeNodeScope(jjtn001, 1);
      jjtc001 = false;
      this.jjtreeCloseNodeScope(jjtn001);
      jjtn001.kind = t.kind;
    } finally {
      if(jjtc001) {
        this.jjtree.closeNodeScope(jjtn001, 1);
        this.jjtreeCloseNodeScope(jjtn001);
      }

    }

  }

  public final void UnaryExpressionNotPlusMinus() throws ParseException {
    Token t = null;
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 87:
    case 88:
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 87:
        t = this.jj_consume_token(87);
        break;
      case 88:
        t = this.jj_consume_token(88);
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }

      this.UnaryExpression();
      BSHUnaryExpression jjtn001 = new BSHUnaryExpression(16);
      boolean jjtc001 = true;
      this.jjtree.openNodeScope(jjtn001);
      this.jjtreeOpenNodeScope(jjtn001);

      try {
        this.jjtree.closeNodeScope(jjtn001, 1);
        jjtc001 = false;
        this.jjtreeCloseNodeScope(jjtn001);
        jjtn001.kind = t.kind;
        break;
      } finally {
        if(jjtc001) {
          this.jjtree.closeNodeScope(jjtn001, 1);
          this.jjtreeCloseNodeScope(jjtn001);
        }

      }
    default:
      if(this.jj_2_9(Integer.MAX_VALUE)) {
        this.CastExpression();
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 11:
        case 14:
        case 17:
        case 22:
        case 26:
        case 29:
        case 36:
        case 38:
        case 40:
        case 41:
        case 47:
        case 55:
        case 57:
        case 60:
        case 64:
        case 66:
        case 67:
        case 68:
        case 70:
        case 73:
          this.PostfixExpression();
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    }

  }

  public final void CastLookahead() throws ParseException {
    if(this.jj_2_10(2)) {
      this.jj_consume_token(73);
      this.PrimitiveType();
    } else if(this.jj_2_11(Integer.MAX_VALUE)) {
      this.jj_consume_token(73);
      this.AmbiguousName();
      this.jj_consume_token(77);
      this.jj_consume_token(78);
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 73:
        this.jj_consume_token(73);
        this.AmbiguousName();
        this.jj_consume_token(74);
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 26:
        case 41:
        case 55:
        case 57:
        case 60:
        case 64:
        case 66:
        case 67:
        case 68:
          this.Literal();
          return;
        case 40:
          this.jj_consume_token(40);
          return;
        case 70:
          this.jj_consume_token(70);
          return;
        case 73:
          this.jj_consume_token(73);
          return;
        case 87:
          this.jj_consume_token(87);
          return;
        case 88:
          this.jj_consume_token(88);
          return;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    }

  }

  public final void PostfixExpression() throws ParseException {
    Token t = null;
    if(this.jj_2_12(Integer.MAX_VALUE)) {
      this.PrimaryExpression();
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 101:
        t = this.jj_consume_token(101);
        break;
      case 102:
        t = this.jj_consume_token(102);
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }

      BSHUnaryExpression jjtn001 = new BSHUnaryExpression(16);
      boolean jjtc001 = true;
      this.jjtree.openNodeScope(jjtn001);
      this.jjtreeOpenNodeScope(jjtn001);

      try {
        this.jjtree.closeNodeScope(jjtn001, 1);
        jjtc001 = false;
        this.jjtreeCloseNodeScope(jjtn001);
        jjtn001.kind = t.kind;
        jjtn001.postfix = true;
      } finally {
        if(jjtc001) {
          this.jjtree.closeNodeScope(jjtn001, 1);
          this.jjtreeCloseNodeScope(jjtn001);
        }

      }
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
        this.PrimaryExpression();
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    }

  }

  public final void CastExpression() throws ParseException {
    BSHCastExpression jjtn000 = new BSHCastExpression(17);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      if(this.jj_2_13(Integer.MAX_VALUE)) {
        this.jj_consume_token(73);
        this.Type();
        this.jj_consume_token(74);
        this.UnaryExpression();
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 73:
          this.jj_consume_token(73);
          this.Type();
          this.jj_consume_token(74);
          this.UnaryExpressionNotPlusMinus();
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void PrimaryExpression() throws ParseException {
    BSHPrimaryExpression jjtn000 = new BSHPrimaryExpression(18);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.PrimaryPrefix();

      while(true) {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 75:
        case 77:
        case 81:
          this.PrimarySuffix();
          break;
        case 76:
        case 78:
        case 79:
        case 80:
        default:
          return;
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      } else if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      } else {
        throw (Error)var7;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void MethodInvocation() throws ParseException {
    BSHMethodInvocation jjtn000 = new BSHMethodInvocation(19);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.AmbiguousName();
      this.Arguments();
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void PrimaryPrefix() throws ParseException {
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 26:
    case 41:
    case 55:
    case 57:
    case 60:
    case 64:
    case 66:
    case 67:
    case 68:
      this.Literal();
      break;
    case 40:
      this.AllocationExpression();
      break;
    case 73:
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      break;
    default:
      if(this.jj_2_14(Integer.MAX_VALUE)) {
        this.MethodInvocation();
      } else if(this.jj_2_15(Integer.MAX_VALUE)) {
        this.Type();
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 70:
          this.AmbiguousName();
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    }

  }

  public final void PrimarySuffix() throws ParseException {
    BSHPrimarySuffix jjtn000 = new BSHPrimarySuffix(20);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Token t = null;

    try {
      if(this.jj_2_16(2)) {
        this.jj_consume_token(81);
        this.jj_consume_token(13);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.operation = 0;
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 75:
          this.jj_consume_token(75);
          this.Expression();
          this.jj_consume_token(76);
          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          jjtn000.operation = 3;
          break;
        case 76:
        case 78:
        case 79:
        case 80:
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        case 77:
          this.jj_consume_token(77);
          this.Expression();
          this.jj_consume_token(78);
          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          jjtn000.operation = 1;
          break;
        case 81:
          this.jj_consume_token(81);
          t = this.jj_consume_token(70);
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 73:
            this.Arguments();
          default:
            this.jjtree.closeNodeScope(jjtn000, true);
            jjtc000 = false;
            this.jjtreeCloseNodeScope(jjtn000);
            jjtn000.operation = 2;
            jjtn000.field = t.image;
          }
        }
      }
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void Literal() throws ParseException {
    BSHLiteral jjtn000 = new BSHLiteral(21);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Token x;
      String literal;
      char ch;
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 26:
      case 55:
        boolean b = this.BooleanLiteral();
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.value = new Primitive(b);
        break;
      case 41:
        this.NullLiteral();
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.value = Primitive.NULL;
        break;
      case 57:
        this.VoidLiteral();
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.value = Primitive.VOID;
        break;
      case 60:
        x = this.jj_consume_token(60);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        literal = x.image;
        ch = literal.charAt(literal.length() - 1);
        if(ch != 108 && ch != 76) {
          try {
            jjtn000.value = new Primitive(this.parseInt(literal));
          } catch (NumberFormatException var19) {
            throw this.createParseException(var19.getMessage(), var19);
          }
        } else {
          literal = literal.substring(0, literal.length() - 1);

          try {
            jjtn000.value = new Primitive(this.parseLong(literal));
          } catch (NumberFormatException var20) {
            throw this.createParseException(var20.getMessage(), var20);
          }
        }
        break;
      case 64:
        x = this.jj_consume_token(64);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        literal = x.image;
        ch = literal.charAt(literal.length() - 1);
        if(ch != 102 && ch != 70) {
          if(ch == 100 || ch == 68) {
            literal = literal.substring(0, literal.length() - 1);
          }

          jjtn000.value = new Primitive((new Double(literal)).doubleValue());
        } else {
          literal = literal.substring(0, literal.length() - 1);
          jjtn000.value = new Primitive((new Float(literal)).floatValue());
        }
        break;
      case 66:
        x = this.jj_consume_token(66);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);

        try {
          jjtn000.charSetup(x.image.substring(1, x.image.length() - 1));
          break;
        } catch (Exception var18) {
          throw this.createParseException("Error parsing character: " + x.image, var18);
        }
      case 67:
        x = this.jj_consume_token(67);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);

        try {
          jjtn000.stringSetup(x.image.substring(1, x.image.length() - 1));
          break;
        } catch (Exception var17) {
          throw this.createParseException("Error parsing string: " + x.image, var17);
        }
      case 68:
        x = this.jj_consume_token(68);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);

        try {
          jjtn000.value = x.image.substring(3, x.image.length() - 3);
          break;
        } catch (Exception var16) {
          throw this.createParseException("Error parsing long string: " + x.image, var16);
        }
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    } catch (Throwable var21) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var21 instanceof RuntimeException) {
        throw (RuntimeException)var21;
      }

      if(var21 instanceof ParseException) {
        throw (ParseException)var21;
      }

      throw (Error)var21;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final boolean BooleanLiteral() throws ParseException {
    switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
    case 26:
      this.jj_consume_token(26);
      return false;
    case 55:
      this.jj_consume_token(55);
      return true;
    default:
      this.jj_consume_token(-1);
      throw new ParseException();
    }
  }

  public final void NullLiteral() throws ParseException {
    this.jj_consume_token(41);
  }

  public final void VoidLiteral() throws ParseException {
    this.jj_consume_token(57);
  }

  public final void Arguments() throws ParseException {
    BSHArguments jjtn000 = new BSHArguments(22);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(73);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.ArgumentList();
      default:
        this.jj_consume_token(74);
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ArgumentList() throws ParseException {
    this.Expression();

    while(true) {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 80:
        this.jj_consume_token(80);
        this.Expression();
        break;
      default:
        return;
      }
    }
  }

  public final void TypeArguments() throws ParseException {
    this.jj_consume_token(85);
    this.jj_consume_token(70);
    this.jj_consume_token(83);
  }

  public final void AllocationExpression() throws ParseException {
    BSHAllocationExpression jjtn000 = new BSHAllocationExpression(23);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      if(this.jj_2_18(2)) {
        this.jj_consume_token(40);
        this.PrimitiveType();
        this.ArrayDimensions();
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 40:
          this.jj_consume_token(40);
          this.AmbiguousName();
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 85:
            this.TypeArguments();
          default:
            switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
            case 73:
              this.Arguments();
              if(this.jj_2_17(2)) {
                this.Block();
              }

              return;
            case 74:
            case 75:
            case 76:
            default:
              this.jj_consume_token(-1);
              throw new ParseException();
            case 77:
              this.ArrayDimensions();
              return;
            }
          }
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ArrayDimensions() throws ParseException {
    BSHArrayDimensions jjtn000 = new BSHArrayDimensions(24);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      if(this.jj_2_21(2)) {
        while(true) {
          this.jj_consume_token(77);
          this.Expression();
          this.jj_consume_token(78);
          jjtn000.addDefinedDimension();
          if(!this.jj_2_19(2)) {
            while(this.jj_2_20(2)) {
              this.jj_consume_token(77);
              this.jj_consume_token(78);
              jjtn000.addUndefinedDimension();
            }
            break;
          }
        }
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 77:
          while(true) {
            this.jj_consume_token(77);
            this.jj_consume_token(78);
            jjtn000.addUndefinedDimension();
            switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
            case 77:
              break;
            default:
              this.ArrayInitializer();
              return;
            }
          }
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void Statement() throws ParseException {
    if(this.jj_2_22(2)) {
      this.LabeledStatement();
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.StatementExpression();
        this.jj_consume_token(79);
        break;
      case 21:
        this.DoStatement();
        break;
      case 32:
        this.IfStatement();
        break;
      case 50:
        this.SwitchStatement();
        break;
      case 59:
        this.WhileStatement();
        break;
      case 75:
        this.Block();
        break;
      case 79:
        this.EmptyStatement();
        break;
      default:
        if(this.isRegularForStatement()) {
          this.ForStatement();
        } else {
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 12:
            this.BreakStatement();
            break;
          case 19:
            this.ContinueStatement();
            break;
          case 30:
            this.EnhancedForStatement();
            break;
          case 46:
            this.ReturnStatement();
            break;
          case 51:
            this.SynchronizedStatement();
            break;
          case 53:
            this.ThrowStatement();
            break;
          case 56:
            this.TryStatement();
            break;
          default:
            this.jj_consume_token(-1);
            throw new ParseException();
          }
        }
      }
    }

  }

  public final void LabeledStatement() throws ParseException {
    this.jj_consume_token(70);
    this.jj_consume_token(90);
    this.Statement();
  }

  public final void Block() throws ParseException {
    BSHBlock jjtn000 = new BSHBlock(25);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(75);

      while(this.jj_2_23(1)) {
        this.BlockStatement();
      }

      this.jj_consume_token(76);
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      } else if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      } else {
        throw (Error)var7;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void BlockStatement() throws ParseException {
    if(this.jj_2_24(Integer.MAX_VALUE)) {
      this.ClassDeclaration();
    } else if(this.jj_2_25(Integer.MAX_VALUE)) {
      this.MethodDeclaration();
    } else if(this.jj_2_26(Integer.MAX_VALUE)) {
      this.MethodDeclaration();
    } else if(this.jj_2_27(Integer.MAX_VALUE)) {
      this.TypedVariableDeclaration();
      this.jj_consume_token(79);
    } else if(this.jj_2_28(1)) {
      this.Statement();
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 34:
      case 48:
        this.ImportDeclaration();
        break;
      case 42:
        this.PackageDeclaration();
        break;
      case 69:
        this.FormalComment();
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    }

  }

  public final void FormalComment() throws ParseException {
    BSHFormalComment jjtn000 = new BSHFormalComment(26);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      Token t = this.jj_consume_token(69);
      this.jjtree.closeNodeScope(jjtn000, this.retainComments);
      jjtc000 = false;
      this.jjtreeCloseNodeScope(jjtn000);
      jjtn000.text = t.image;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, this.retainComments);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void EmptyStatement() throws ParseException {
    this.jj_consume_token(79);
  }

  public final void StatementExpression() throws ParseException {
    this.Expression();
  }

  public final void SwitchStatement() throws ParseException {
    BSHSwitchStatement jjtn000 = new BSHSwitchStatement(27);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(50);
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      this.jj_consume_token(75);

      while(true) {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 15:
        case 20:
          this.SwitchLabel();

          while(this.jj_2_29(1)) {
            this.BlockStatement();
          }
          break;
        default:
          this.jj_consume_token(76);
          return;
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      } else if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      } else {
        throw (Error)var7;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void SwitchLabel() throws ParseException {
    BSHSwitchLabel jjtn000 = new BSHSwitchLabel(28);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 15:
        this.jj_consume_token(15);
        this.Expression();
        this.jj_consume_token(90);
        break;
      case 20:
        this.jj_consume_token(20);
        this.jj_consume_token(90);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.isDefault = true;
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void IfStatement() throws ParseException {
    BSHIfStatement jjtn000 = new BSHIfStatement(29);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(32);
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      this.Statement();
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 23:
        this.jj_consume_token(23);
        this.Statement();
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void WhileStatement() throws ParseException {
    BSHWhileStatement jjtn000 = new BSHWhileStatement(30);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(59);
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      this.Statement();
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void DoStatement() throws ParseException {
    BSHWhileStatement jjtn000 = new BSHWhileStatement(30);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(21);
      this.Statement();
      this.jj_consume_token(59);
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      this.jj_consume_token(79);
      this.jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      this.jjtreeCloseNodeScope(jjtn000);
      jjtn000.isDoStatement = true;
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ForStatement() throws ParseException {
    BSHForStatement jjtn000 = new BSHForStatement(31);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Object t = null;

    try {
      this.jj_consume_token(30);
      this.jj_consume_token(73);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 10:
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 27:
      case 29:
      case 36:
      case 38:
      case 39:
      case 40:
      case 41:
      case 43:
      case 44:
      case 45:
      case 47:
      case 48:
      case 49:
      case 51:
      case 52:
      case 55:
      case 57:
      case 58:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.ForInit();
        jjtn000.hasForInit = true;
      }

      this.jj_consume_token(79);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.Expression();
        jjtn000.hasExpression = true;
      }

      this.jj_consume_token(79);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.ForUpdate();
        jjtn000.hasForUpdate = true;
      }

      this.jj_consume_token(74);
      this.Statement();
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void EnhancedForStatement() throws ParseException {
    BSHEnhancedForStatement jjtn000 = new BSHEnhancedForStatement(32);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Token t = null;

    try {
      if(this.jj_2_30(4)) {
        this.jj_consume_token(30);
        this.jj_consume_token(73);
        t = this.jj_consume_token(70);
        this.jj_consume_token(90);
        this.Expression();
        this.jj_consume_token(74);
        this.Statement();
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.varName = t.image;
      } else {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 30:
          this.jj_consume_token(30);
          this.jj_consume_token(73);
          this.Type();
          t = this.jj_consume_token(70);
          this.jj_consume_token(90);
          this.Expression();
          this.jj_consume_token(74);
          this.Statement();
          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          jjtn000.varName = t.image;
          break;
        default:
          this.jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      }

      if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      }

      throw (Error)var8;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ForInit() throws ParseException {
    Object t = null;
    if(this.jj_2_31(Integer.MAX_VALUE)) {
      this.TypedVariableDeclaration();
    } else {
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.StatementExpressionList();
        break;
      default:
        this.jj_consume_token(-1);
        throw new ParseException();
      }
    }

  }

  public final void TypedVariableDeclaration() throws ParseException {
    BSHTypedVariableDeclaration jjtn000 = new BSHTypedVariableDeclaration(33);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    Object t = null;

    try {
      Modifiers mods = this.Modifiers(1, false);
      this.Type();
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 85:
        this.TypeArguments();
      default:
        this.VariableDeclarator();

        while(true) {
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 80:
            this.jj_consume_token(80);
            this.VariableDeclarator();
            break;
          default:
            this.jjtree.closeNodeScope(jjtn000, true);
            jjtc000 = false;
            this.jjtreeCloseNodeScope(jjtn000);
            jjtn000.modifiers = mods;
            return;
          }
        }
      }
    } catch (Throwable var9) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var9 instanceof RuntimeException) {
        throw (RuntimeException)var9;
      } else if(var9 instanceof ParseException) {
        throw (ParseException)var9;
      } else {
        throw (Error)var9;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void StatementExpressionList() throws ParseException {
    BSHStatementExpressionList jjtn000 = new BSHStatementExpressionList(34);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.StatementExpression();

      while(true) {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 80:
          this.jj_consume_token(80);
          this.StatementExpression();
          break;
        default:
          return;
        }
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      } else if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      } else {
        throw (Error)var7;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  public final void ForUpdate() throws ParseException {
    this.StatementExpressionList();
  }

  public final void BreakStatement() throws ParseException {
    BSHReturnStatement jjtn000 = new BSHReturnStatement(35);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(12);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 70:
        this.jj_consume_token(70);
      default:
        this.jj_consume_token(79);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.kind = 12;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ContinueStatement() throws ParseException {
    BSHReturnStatement jjtn000 = new BSHReturnStatement(35);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(19);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 70:
        this.jj_consume_token(70);
      default:
        this.jj_consume_token(79);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.kind = 19;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ReturnStatement() throws ParseException {
    BSHReturnStatement jjtn000 = new BSHReturnStatement(35);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(46);
      switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
      case 11:
      case 14:
      case 17:
      case 22:
      case 26:
      case 29:
      case 36:
      case 38:
      case 40:
      case 41:
      case 47:
      case 55:
      case 57:
      case 60:
      case 64:
      case 66:
      case 67:
      case 68:
      case 70:
      case 73:
      case 87:
      case 88:
      case 101:
      case 102:
      case 103:
      case 104:
        this.Expression();
      default:
        this.jj_consume_token(79);
        this.jjtree.closeNodeScope(jjtn000, true);
        jjtc000 = false;
        this.jjtreeCloseNodeScope(jjtn000);
        jjtn000.kind = 46;
      }
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void SynchronizedStatement() throws ParseException {
    BSHBlock jjtn000 = new BSHBlock(25);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(51);
      this.jj_consume_token(73);
      this.Expression();
      this.jj_consume_token(74);
      this.Block();
      this.jjtree.closeNodeScope(jjtn000, true);
      jjtc000 = false;
      this.jjtreeCloseNodeScope(jjtn000);
      jjtn000.isSynchronized = true;
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void ThrowStatement() throws ParseException {
    BSHThrowStatement jjtn000 = new BSHThrowStatement(36);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);

    try {
      this.jj_consume_token(53);
      this.Expression();
      this.jj_consume_token(79);
    } catch (Throwable var7) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var7 instanceof RuntimeException) {
        throw (RuntimeException)var7;
      }

      if(var7 instanceof ParseException) {
        throw (ParseException)var7;
      }

      throw (Error)var7;
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }

  }

  public final void TryStatement() throws ParseException {
    BSHTryStatement jjtn000 = new BSHTryStatement(37);
    boolean jjtc000 = true;
    this.jjtree.openNodeScope(jjtn000);
    this.jjtreeOpenNodeScope(jjtn000);
    boolean closed = false;

    try {
      this.jj_consume_token(56);
      this.Block();

      while(true) {
        switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
        case 16:
          this.jj_consume_token(16);
          this.jj_consume_token(73);
          this.FormalParameter();
          this.jj_consume_token(74);
          this.Block();
          closed = true;
          break;
        default:
          switch(this.jj_ntk == -1?this.jj_ntk():this.jj_ntk) {
          case 28:
            this.jj_consume_token(28);
            this.Block();
            closed = true;
          }

          this.jjtree.closeNodeScope(jjtn000, true);
          jjtc000 = false;
          this.jjtreeCloseNodeScope(jjtn000);
          if(!closed) {
            throw this.generateParseException();
          }

          return;
        }
      }
    } catch (Throwable var8) {
      if(jjtc000) {
        this.jjtree.clearNodeScope(jjtn000);
        jjtc000 = false;
      } else {
        this.jjtree.popNode();
      }

      if(var8 instanceof RuntimeException) {
        throw (RuntimeException)var8;
      } else if(var8 instanceof ParseException) {
        throw (ParseException)var8;
      } else {
        throw (Error)var8;
      }
    } finally {
      if(jjtc000) {
        this.jjtree.closeNodeScope(jjtn000, true);
        this.jjtreeCloseNodeScope(jjtn000);
      }

    }
  }

  private boolean jj_2_1(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_1();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_2(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_2();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_3(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_3();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_4(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_4();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_5(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_5();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_6(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_6();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_7(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_7();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_8(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_8();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_9(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_9();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_10(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_10();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_11(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_11();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_12(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_12();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_13(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_13();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_14(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_14();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_15(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_15();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_16(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_16();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_17(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_17();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_18(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_18();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_19(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_19();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_20(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_20();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_21(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_21();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_22(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_22();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_23(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_23();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_24(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_24();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_25(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_25();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_26(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_26();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_27(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_27();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_28(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_28();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_29(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_29();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_30(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_30();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_2_31(int xla) {
    this.jj_la = xla;
    this.jj_lastpos = this.jj_scanpos = this.token;

    try {
      return !this.jj_3_31();
    } catch (Parser.LookaheadSuccess var3) {
      return true;
    }
  }

  private boolean jj_3R_132() {
    return this.jj_3R_44()?true:this.jj_scan_token(70);
  }

  private boolean jj_3R_130() {
    return this.jj_scan_token(37);
  }

  private boolean jj_3R_213() {
    if(this.jj_3R_117()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_218());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_89() {
    return this.jj_3R_124();
  }

  private boolean jj_3R_66() {
    return this.jj_scan_token(17);
  }

  private boolean jj_3R_101() {
    if(this.jj_scan_token(75)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_170()) {
        this.jj_scanpos = xsp;
      }

      xsp = this.jj_scanpos;
      if(this.jj_scan_token(80)) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(76);
    }
  }

  private boolean jj_3R_57() {
    return this.jj_3R_41();
  }

  private boolean jj_3R_105() {
    return this.jj_3R_39();
  }

  private boolean jj_3R_53() {
    return this.jj_3R_98();
  }

  private boolean jj_3R_83() {
    return this.jj_3R_118();
  }

  private boolean jj_3R_171() {
    if(this.jj_3R_173()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_178());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_32() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_58()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_59()) {
        return true;
      }
    }

    do {
      xsp = this.jj_scanpos;
    } while(!this.jj_3_6());

    this.jj_scanpos = xsp;
    return false;
  }

  private boolean jj_3R_184() {
    if(this.jj_scan_token(70)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_188()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_77() {
    return this.jj_scan_token(57);
  }

  private boolean jj_3R_44() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_77()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_78()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_46() {
    return this.jj_scan_token(54)?true:this.jj_3R_80();
  }

  private boolean jj_3R_147() {
    return this.jj_scan_token(68);
  }

  private boolean jj_3R_188() {
    return this.jj_scan_token(82)?true:this.jj_3R_31();
  }

  private boolean jj_3R_221() {
    return this.jj_3R_223();
  }

  private boolean jj_3R_65() {
    return this.jj_scan_token(11);
  }

  private boolean jj_3R_38() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_65()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_66()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_67()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_68()) {
            this.jj_scanpos = xsp;
            if(this.jj_3R_69()) {
              this.jj_scanpos = xsp;
              if(this.jj_3R_70()) {
                this.jj_scanpos = xsp;
                if(this.jj_3R_71()) {
                  this.jj_scanpos = xsp;
                  if(this.jj_3R_72()) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_175() {
    if(this.jj_3R_177()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_190());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_193() {
    return this.jj_3R_202();
  }

  private boolean jj_3R_70() {
    return this.jj_scan_token(38);
  }

  private boolean jj_3R_190() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(85)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(86)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(83)) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(84)) {
            this.jj_scanpos = xsp;
            if(this.jj_scan_token(92)) {
              this.jj_scanpos = xsp;
              if(this.jj_scan_token(93)) {
                this.jj_scanpos = xsp;
                if(this.jj_scan_token(94)) {
                  this.jj_scanpos = xsp;
                  if(this.jj_scan_token(95)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }

    return this.jj_3R_177();
  }

  private boolean jj_3R_104() {
    return this.jj_3R_135();
  }

  private boolean jj_3R_140() {
    if(this.jj_3R_154()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_166());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_128() {
    return this.jj_scan_token(53)?true:(this.jj_3R_41()?true:this.jj_scan_token(79));
  }

  private boolean jj_3R_108() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_16()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_136()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_137()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_138()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3_16() {
    return this.jj_scan_token(81)?true:this.jj_scan_token(13);
  }

  private boolean jj_3R_90() {
    return this.jj_3R_125();
  }

  private boolean jj_3R_55() {
    return this.jj_3R_100();
  }

  private boolean jj_3R_73() {
    if(this.jj_scan_token(73)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_110()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(74);
    }
  }

  private boolean jj_3R_156() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_21()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_164()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_198() {
    return this.jj_scan_token(28)?true:this.jj_3R_40();
  }

  private boolean jj_3R_119() {
    if(this.jj_scan_token(32)) {
      return true;
    } else if(this.jj_scan_token(73)) {
      return true;
    } else if(this.jj_3R_41()) {
      return true;
    } else if(this.jj_scan_token(74)) {
      return true;
    } else if(this.jj_3R_48()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_192()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3_29() {
    return this.jj_3R_28();
  }

  private boolean jj_3R_211() {
    return this.jj_3R_97();
  }

  private boolean jj_3R_202() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_211()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_212()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3_25() {
    return this.jj_3R_43()?true:(this.jj_3R_44()?true:(this.jj_scan_token(70)?true:this.jj_scan_token(73)));
  }

  private boolean jj_3R_219() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(88)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(87)) {
        return true;
      }
    }

    return this.jj_3R_199();
  }

  private boolean jj_3R_216() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_219()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_220()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_221()) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean jj_3R_71() {
    return this.jj_scan_token(29);
  }

  private boolean jj_3R_137() {
    if(this.jj_scan_token(81)) {
      return true;
    } else if(this.jj_scan_token(70)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_152()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_205() {
    return this.jj_3R_214();
  }

  private boolean jj_3R_212() {
    return this.jj_3R_213();
  }

  private boolean jj_3R_165() {
    if(this.jj_3R_168()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_174());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_153() {
    return this.jj_scan_token(80)?true:this.jj_3R_41();
  }

  private boolean jj_3R_110() {
    return this.jj_3R_139();
  }

  private boolean jj_3_14() {
    return this.jj_3R_39();
  }

  private boolean jj_3R_50() {
    return this.jj_3R_96();
  }

  private boolean jj_3R_63() {
    return this.jj_scan_token(73)?true:(this.jj_3R_29()?true:(this.jj_scan_token(77)?true:this.jj_scan_token(78)));
  }

  private boolean jj_3_9() {
    return this.jj_3R_37();
  }

  private boolean jj_3R_207() {
    return this.jj_3R_216();
  }

  private boolean jj_3_26() {
    if(this.jj_3R_43()) {
      return true;
    } else if(this.jj_scan_token(70)) {
      return true;
    } else if(this.jj_3R_45()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_46()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(75);
    }
  }

  private boolean jj_3R_94() {
    return this.jj_3R_129();
  }

  private boolean jj_3R_100() {
    return this.jj_scan_token(69);
  }

  private boolean jj_3R_85() {
    return this.jj_3R_120();
  }

  private boolean jj_3R_51() {
    return this.jj_3R_96();
  }

  private boolean jj_3R_112() {
    if(this.jj_3R_140()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_163()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_150() {
    return this.jj_scan_token(57);
  }

  private boolean jj_3R_217() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(105)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(106)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(112)) {
          return true;
        }
      }
    }

    return this.jj_3R_199();
  }

  private boolean jj_3R_214() {
    return this.jj_scan_token(101)?true:this.jj_3R_35();
  }

  private boolean jj_3_7() {
    return this.jj_scan_token(81)?true:this.jj_scan_token(70);
  }

  private boolean jj_3_15() {
    return this.jj_3R_32()?true:(this.jj_scan_token(81)?true:this.jj_scan_token(13));
  }

  private boolean jj_3R_180() {
    return this.jj_scan_token(33)?true:this.jj_3R_80();
  }

  private boolean jj_3_5() {
    if(this.jj_3R_32()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_33()) {
        this.jj_scanpos = xsp;
      }

      xsp = this.jj_scanpos;
      if(this.jj_3R_34()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(70);
    }
  }

  private boolean jj_3R_52() {
    return this.jj_3R_97()?true:this.jj_scan_token(79);
  }

  private boolean jj_3R_192() {
    return this.jj_scan_token(23)?true:this.jj_3R_48();
  }

  private boolean jj_3_4() {
    return this.jj_scan_token(80)?true:this.jj_3R_31();
  }

  private boolean jj_3R_186() {
    if(this.jj_3R_189()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_208());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_91() {
    return this.jj_3R_126();
  }

  private boolean jj_3R_29() {
    if(this.jj_scan_token(70)) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_7());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_178() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(91)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(96)) {
        return true;
      }
    }

    return this.jj_3R_173();
  }

  private boolean jj_3R_177() {
    if(this.jj_3R_186()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_200());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_152() {
    return this.jj_3R_73();
  }

  private boolean jj_3R_102() {
    return this.jj_3R_134();
  }

  private boolean jj_3R_61() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_102()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_103()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_104()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_105()) {
            this.jj_scanpos = xsp;
            if(this.jj_3R_106()) {
              this.jj_scanpos = xsp;
              if(this.jj_3R_107()) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_166() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(97)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(98)) {
        return true;
      }
    }

    return this.jj_3R_154();
  }

  private boolean jj_3R_158() {
    return this.jj_3R_156();
  }

  private boolean jj_3R_109() {
    return this.jj_3R_134();
  }

  private boolean jj_3_19() {
    return this.jj_scan_token(77)?true:(this.jj_3R_41()?true:this.jj_scan_token(78));
  }

  private boolean jj_3R_116() {
    return this.jj_scan_token(80)?true:this.jj_3R_29();
  }

  private boolean jj_3R_40() {
    if(this.jj_scan_token(75)) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_23());

      this.jj_scanpos = xsp;
      return this.jj_scan_token(76);
    }
  }

  private boolean jj_3_20() {
    return this.jj_scan_token(77)?true:this.jj_scan_token(78);
  }

  private boolean jj_3R_45() {
    if(this.jj_scan_token(73)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_79()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(74);
    }
  }

  private boolean jj_3R_36() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(82)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(121)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(122)) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(128)) {
            this.jj_scanpos = xsp;
            if(this.jj_scan_token(119)) {
              this.jj_scanpos = xsp;
              if(this.jj_scan_token(120)) {
                this.jj_scanpos = xsp;
                if(this.jj_scan_token(123)) {
                  this.jj_scanpos = xsp;
                  if(this.jj_scan_token(127)) {
                    this.jj_scanpos = xsp;
                    if(this.jj_scan_token(125)) {
                      this.jj_scanpos = xsp;
                      if(this.jj_scan_token(129)) {
                        this.jj_scanpos = xsp;
                        if(this.jj_scan_token(130)) {
                          this.jj_scanpos = xsp;
                          if(this.jj_scan_token(131)) {
                            this.jj_scanpos = xsp;
                            if(this.jj_scan_token(132)) {
                              this.jj_scanpos = xsp;
                              if(this.jj_scan_token(133)) {
                                this.jj_scanpos = xsp;
                                if(this.jj_scan_token(134)) {
                                  return true;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_206() {
    return this.jj_3R_215();
  }

  private boolean jj_3R_92() {
    return this.jj_3R_127();
  }

  private boolean jj_3R_120() {
    return this.jj_scan_token(59)?true:(this.jj_scan_token(73)?true:(this.jj_3R_41()?true:(this.jj_scan_token(74)?true:this.jj_3R_48())));
  }

  private boolean jj_3R_124() {
    if(this.jj_scan_token(12)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(70)) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(79);
    }
  }

  private boolean jj_3R_125() {
    if(this.jj_scan_token(19)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(70)) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(79);
    }
  }

  private boolean jj_3R_225() {
    return this.jj_scan_token(73)?true:(this.jj_3R_32()?true:(this.jj_scan_token(74)?true:this.jj_3R_216()));
  }

  private boolean jj_3R_161() {
    return this.jj_scan_token(55);
  }

  private boolean jj_3R_155() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_161()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_162()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_98() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_3()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_133()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3_3() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(48)) {
      this.jj_scanpos = xsp;
    }

    if(this.jj_scan_token(34)) {
      return true;
    } else if(this.jj_3R_29()) {
      return true;
    } else {
      xsp = this.jj_scanpos;
      if(this.jj_3R_30()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(79);
    }
  }

  private boolean jj_3R_176() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(107)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(108)) {
        return true;
      }
    }

    return this.jj_3R_171();
  }

  private boolean jj_3_13() {
    return this.jj_scan_token(73)?true:this.jj_3R_38();
  }

  private boolean jj_3R_118() {
    if(this.jj_scan_token(50)) {
      return true;
    } else if(this.jj_scan_token(73)) {
      return true;
    } else if(this.jj_3R_41()) {
      return true;
    } else if(this.jj_scan_token(74)) {
      return true;
    } else if(this.jj_scan_token(75)) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_191());

      this.jj_scanpos = xsp;
      return this.jj_scan_token(76);
    }
  }

  private boolean jj_3R_160() {
    if(this.jj_3R_165()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_172());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_149() {
    return this.jj_scan_token(41);
  }

  private boolean jj_3R_42() {
    return this.jj_scan_token(70)?true:(this.jj_scan_token(90)?true:this.jj_3R_48());
  }

  private boolean jj_3R_203() {
    return this.jj_3R_213();
  }

  private boolean jj_3R_183() {
    return this.jj_3R_60();
  }

  private boolean jj_3R_64() {
    if(this.jj_scan_token(73)) {
      return true;
    } else if(this.jj_3R_29()) {
      return true;
    } else if(this.jj_scan_token(74)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(88)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(87)) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(73)) {
            this.jj_scanpos = xsp;
            if(this.jj_scan_token(70)) {
              this.jj_scanpos = xsp;
              if(this.jj_scan_token(40)) {
                this.jj_scanpos = xsp;
                if(this.jj_3R_109()) {
                  return true;
                }
              }
            }
          }
        }
      }

      return false;
    }
  }

  private boolean jj_3R_170() {
    if(this.jj_3R_31()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_4());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3_8() {
    return this.jj_3R_35()?true:this.jj_3R_36();
  }

  private boolean jj_3R_200() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(113)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(114)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(115)) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(116)) {
            this.jj_scanpos = xsp;
            if(this.jj_scan_token(117)) {
              this.jj_scanpos = xsp;
              if(this.jj_scan_token(118)) {
                return true;
              }
            }
          }
        }
      }
    }

    return this.jj_3R_186();
  }

  private boolean jj_3R_168() {
    if(this.jj_3R_171()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_176());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3_21() {
    if(this.jj_3_19()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_19());

      this.jj_scanpos = xsp;

      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_20());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_220() {
    return this.jj_3R_222();
  }

  private boolean jj_3R_182() {
    return this.jj_3R_40();
  }

  private boolean jj_3R_33() {
    return this.jj_scan_token(135);
  }

  private boolean jj_3R_129() {
    if(this.jj_scan_token(56)) {
      return true;
    } else if(this.jj_3R_40()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_197());

      this.jj_scanpos = xsp;
      xsp = this.jj_scanpos;
      if(this.jj_3R_198()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_80() {
    if(this.jj_3R_29()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_116());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_142() {
    return this.jj_scan_token(30)?true:(this.jj_scan_token(73)?true:(this.jj_3R_32()?true:(this.jj_scan_token(70)?true:(this.jj_scan_token(90)?true:(this.jj_3R_41()?true:(this.jj_scan_token(74)?true:this.jj_3R_48()))))));
  }

  private boolean jj_3_31() {
    return this.jj_3R_43()?true:(this.jj_3R_32()?true:this.jj_scan_token(70));
  }

  private boolean jj_3R_226() {
    if(this.jj_3R_35()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(101)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(102)) {
          return true;
        }
      }

      return false;
    }
  }

  private boolean jj_3R_223() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_226()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_227()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_37() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_10()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_63()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_64()) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean jj_3_10() {
    return this.jj_scan_token(73)?true:this.jj_3R_38();
  }

  private boolean jj_3R_169() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(99)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(100)) {
        return true;
      }
    }

    return this.jj_3R_160();
  }

  private boolean jj_3R_67() {
    return this.jj_scan_token(14);
  }

  private boolean jj_3R_121() {
    return this.jj_scan_token(21)?true:(this.jj_3R_48()?true:(this.jj_scan_token(59)?true:(this.jj_scan_token(73)?true:(this.jj_3R_41()?true:(this.jj_scan_token(74)?true:this.jj_scan_token(79))))));
  }

  private boolean jj_3R_96() {
    if(this.jj_3R_43()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_131()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_132()) {
          return true;
        }
      }

      if(this.jj_3R_45()) {
        return true;
      } else {
        xsp = this.jj_scanpos;
        if(this.jj_3R_181()) {
          this.jj_scanpos = xsp;
        }

        xsp = this.jj_scanpos;
        if(this.jj_3R_182()) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(79)) {
            return true;
          }
        }

        return false;
      }
    }
  }

  private boolean jj_3_24() {
    if(this.jj_3R_43()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(13)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(37)) {
          return true;
        }
      }

      return false;
    }
  }

  private boolean jj_3R_179() {
    return this.jj_scan_token(25)?true:this.jj_3R_29();
  }

  private boolean jj_3R_135() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_18()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_151()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_60() {
    return this.jj_scan_token(85)?true:(this.jj_scan_token(70)?true:this.jj_scan_token(83));
  }

  private boolean jj_3_18() {
    return this.jj_scan_token(40)?true:(this.jj_3R_38()?true:this.jj_3R_156());
  }

  private boolean jj_3R_148() {
    return this.jj_3R_155();
  }

  private boolean jj_3R_48() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_22()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_81()) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(79)) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_82()) {
            this.jj_scanpos = xsp;
            if(this.jj_3R_83()) {
              this.jj_scanpos = xsp;
              if(this.jj_3R_84()) {
                this.jj_scanpos = xsp;
                if(this.jj_3R_85()) {
                  this.jj_scanpos = xsp;
                  if(this.jj_3R_86()) {
                    this.jj_scanpos = xsp;
                    this.jj_lookingAhead = true;
                    this.jj_semLA = this.isRegularForStatement();
                    this.jj_lookingAhead = false;
                    if(!this.jj_semLA || this.jj_3R_87()) {
                      this.jj_scanpos = xsp;
                      if(this.jj_3R_88()) {
                        this.jj_scanpos = xsp;
                        if(this.jj_3R_89()) {
                          this.jj_scanpos = xsp;
                          if(this.jj_3R_90()) {
                            this.jj_scanpos = xsp;
                            if(this.jj_3R_91()) {
                              this.jj_scanpos = xsp;
                              if(this.jj_3R_92()) {
                                this.jj_scanpos = xsp;
                                if(this.jj_3R_93()) {
                                  this.jj_scanpos = xsp;
                                  if(this.jj_3R_94()) {
                                    return true;
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3_22() {
    return this.jj_3R_42();
  }

  private boolean jj_3R_181() {
    return this.jj_scan_token(54)?true:this.jj_3R_80();
  }

  private boolean jj_3R_76() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(43)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(44)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(45)) {
          this.jj_scanpos = xsp;
          if(this.jj_scan_token(51)) {
            this.jj_scanpos = xsp;
            if(this.jj_scan_token(27)) {
              this.jj_scanpos = xsp;
              if(this.jj_scan_token(39)) {
                this.jj_scanpos = xsp;
                if(this.jj_scan_token(52)) {
                  this.jj_scanpos = xsp;
                  if(this.jj_scan_token(58)) {
                    this.jj_scanpos = xsp;
                    if(this.jj_scan_token(10)) {
                      this.jj_scanpos = xsp;
                      if(this.jj_scan_token(48)) {
                        this.jj_scanpos = xsp;
                        if(this.jj_scan_token(49)) {
                          return true;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_43() {
    Token xsp;
    do {
      xsp = this.jj_scanpos;
    } while(!this.jj_3R_76());

    this.jj_scanpos = xsp;
    return false;
  }

  private boolean jj_3R_189() {
    if(this.jj_3R_199()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_217());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_117() {
    return this.jj_3R_41();
  }

  private boolean jj_3R_107() {
    return this.jj_3R_29();
  }

  private boolean jj_3R_97() {
    if(this.jj_3R_43()) {
      return true;
    } else if(this.jj_3R_32()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_183()) {
        this.jj_scanpos = xsp;
      }

      if(this.jj_3R_184()) {
        return true;
      } else {
        do {
          xsp = this.jj_scanpos;
        } while(!this.jj_3R_185());

        this.jj_scanpos = xsp;
        return false;
      }
    }
  }

  private boolean jj_3R_62() {
    return this.jj_3R_108();
  }

  private boolean jj_3R_227() {
    return this.jj_3R_35();
  }

  private boolean jj_3_17() {
    return this.jj_3R_40();
  }

  private boolean jj_3R_145() {
    return this.jj_scan_token(66);
  }

  private boolean jj_3R_88() {
    return this.jj_3R_123();
  }

  private boolean jj_3R_191() {
    if(this.jj_3R_201()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3_29());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3_6() {
    return this.jj_scan_token(77)?true:this.jj_scan_token(78);
  }

  private boolean jj_3R_136() {
    return this.jj_scan_token(77)?true:(this.jj_3R_41()?true:this.jj_scan_token(78));
  }

  private boolean jj_3R_174() {
    return this.jj_scan_token(111)?true:this.jj_3R_168();
  }

  private boolean jj_3R_122() {
    if(this.jj_scan_token(30)) {
      return true;
    } else if(this.jj_scan_token(73)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_193()) {
        this.jj_scanpos = xsp;
      }

      if(this.jj_scan_token(79)) {
        return true;
      } else {
        xsp = this.jj_scanpos;
        if(this.jj_3R_194()) {
          this.jj_scanpos = xsp;
        }

        if(this.jj_scan_token(79)) {
          return true;
        } else {
          xsp = this.jj_scanpos;
          if(this.jj_3R_195()) {
            this.jj_scanpos = xsp;
          }

          return this.jj_scan_token(74)?true:this.jj_3R_48();
        }
      }
    }
  }

  private boolean jj_3R_195() {
    return this.jj_3R_203();
  }

  private boolean jj_3R_208() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(103)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(104)) {
        return true;
      }
    }

    return this.jj_3R_189();
  }

  private boolean jj_3R_162() {
    return this.jj_scan_token(26);
  }

  private boolean jj_3R_209() {
    return this.jj_scan_token(15)?true:(this.jj_3R_41()?true:this.jj_scan_token(90));
  }

  private boolean jj_3R_201() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_209()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_210()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_151() {
    if(this.jj_scan_token(40)) {
      return true;
    } else if(this.jj_3R_29()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_157()) {
        this.jj_scanpos = xsp;
      }

      xsp = this.jj_scanpos;
      if(this.jj_3R_158()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_159()) {
          return true;
        }
      }

      return false;
    }
  }

  private boolean jj_3R_194() {
    return this.jj_3R_41();
  }

  private boolean jj_3_11() {
    return this.jj_scan_token(73)?true:(this.jj_3R_29()?true:this.jj_scan_token(77));
  }

  private boolean jj_3R_172() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(109)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(110)) {
        return true;
      }
    }

    return this.jj_3R_165();
  }

  private boolean jj_3R_163() {
    return this.jj_scan_token(89)?true:(this.jj_3R_41()?true:(this.jj_scan_token(90)?true:this.jj_3R_112()));
  }

  private boolean jj_3R_141() {
    return this.jj_scan_token(70);
  }

  private boolean jj_3R_81() {
    return this.jj_3R_40();
  }

  private boolean jj_3R_187() {
    return this.jj_scan_token(35)?true:this.jj_3R_32();
  }

  private boolean jj_3R_87() {
    return this.jj_3R_122();
  }

  private boolean jj_3R_131() {
    return this.jj_scan_token(70);
  }

  private boolean jj_3R_111() {
    return this.jj_3R_35()?true:(this.jj_3R_36()?true:this.jj_3R_41());
  }

  private boolean jj_3R_74() {
    return this.jj_3R_111();
  }

  private boolean jj_3R_41() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_74()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_75()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_143() {
    return this.jj_scan_token(60);
  }

  private boolean jj_3R_134() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_143()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_144()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_145()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_146()) {
            this.jj_scanpos = xsp;
            if(this.jj_3R_147()) {
              this.jj_scanpos = xsp;
              if(this.jj_3R_148()) {
                this.jj_scanpos = xsp;
                if(this.jj_3R_149()) {
                  this.jj_scanpos = xsp;
                  if(this.jj_3R_150()) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_167() {
    return this.jj_scan_token(77)?true:this.jj_scan_token(78);
  }

  private boolean jj_3R_204() {
    Token xsp = this.jj_scanpos;
    if(this.jj_scan_token(103)) {
      this.jj_scanpos = xsp;
      if(this.jj_scan_token(104)) {
        return true;
      }
    }

    return this.jj_3R_199();
  }

  private boolean jj_3R_199() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_204()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_205()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_206()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_207()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_222() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_224()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_225()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_93() {
    return this.jj_3R_128();
  }

  private boolean jj_3_12() {
    if(this.jj_3R_35()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(101)) {
        this.jj_scanpos = xsp;
        if(this.jj_scan_token(102)) {
          return true;
        }
      }

      return false;
    }
  }

  private boolean jj_3R_35() {
    if(this.jj_3R_61()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_62());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_99() {
    return this.jj_scan_token(42)?true:this.jj_3R_29();
  }

  private boolean jj_3R_47() {
    return this.jj_3R_60();
  }

  private boolean jj_3R_56() {
    return this.jj_3R_101();
  }

  private boolean jj_3R_31() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_56()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_57()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_58() {
    return this.jj_3R_38();
  }

  private boolean jj_3R_139() {
    if(this.jj_3R_41()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_153());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_39() {
    return this.jj_3R_29()?true:this.jj_3R_73();
  }

  private boolean jj_3R_185() {
    return this.jj_scan_token(80)?true:this.jj_3R_184();
  }

  private boolean jj_3R_215() {
    return this.jj_scan_token(102)?true:this.jj_3R_35();
  }

  private boolean jj_3R_173() {
    if(this.jj_3R_175()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_187()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_86() {
    return this.jj_3R_121();
  }

  private boolean jj_3R_82() {
    return this.jj_3R_117()?true:this.jj_scan_token(79);
  }

  private boolean jj_3R_84() {
    return this.jj_3R_119();
  }

  private boolean jj_3R_123() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_30()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_142()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3_30() {
    return this.jj_scan_token(30)?true:(this.jj_scan_token(73)?true:(this.jj_scan_token(70)?true:(this.jj_scan_token(90)?true:(this.jj_3R_41()?true:(this.jj_scan_token(74)?true:this.jj_3R_48())))));
  }

  private boolean jj_3R_75() {
    return this.jj_3R_112();
  }

  private boolean jj_3R_164() {
    if(this.jj_3R_167()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_167());

      this.jj_scanpos = xsp;
      return this.jj_3R_101();
    }
  }

  private boolean jj_3R_210() {
    return this.jj_scan_token(20)?true:this.jj_scan_token(90);
  }

  private boolean jj_3R_68() {
    return this.jj_scan_token(47);
  }

  private boolean jj_3R_59() {
    return this.jj_3R_29();
  }

  private boolean jj_3_2() {
    return this.jj_scan_token(70)?true:this.jj_scan_token(73);
  }

  private boolean jj_3R_138() {
    return this.jj_scan_token(75)?true:(this.jj_3R_41()?true:this.jj_scan_token(76));
  }

  private boolean jj_3R_30() {
    return this.jj_scan_token(81)?true:this.jj_scan_token(105);
  }

  private boolean jj_3R_127() {
    return this.jj_scan_token(51)?true:(this.jj_scan_token(73)?true:(this.jj_3R_41()?true:(this.jj_scan_token(74)?true:this.jj_3R_40())));
  }

  private boolean jj_3R_146() {
    return this.jj_scan_token(67);
  }

  private boolean jj_3R_106() {
    return this.jj_3R_32();
  }

  private boolean jj_3R_54() {
    return this.jj_3R_99();
  }

  private boolean jj_3R_95() {
    if(this.jj_3R_43()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_scan_token(13)) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_130()) {
          return true;
        }
      }

      if(this.jj_scan_token(70)) {
        return true;
      } else {
        xsp = this.jj_scanpos;
        if(this.jj_3R_179()) {
          this.jj_scanpos = xsp;
        }

        xsp = this.jj_scanpos;
        if(this.jj_3R_180()) {
          this.jj_scanpos = xsp;
        }

        return this.jj_3R_40();
      }
    }
  }

  private boolean jj_3R_78() {
    if(this.jj_3R_32()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_113()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_72() {
    return this.jj_scan_token(22);
  }

  private boolean jj_3R_113() {
    return this.jj_3R_60();
  }

  private boolean jj_3R_69() {
    return this.jj_scan_token(36);
  }

  private boolean jj_3R_159() {
    if(this.jj_3R_73()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3_17()) {
        this.jj_scanpos = xsp;
      }

      return false;
    }
  }

  private boolean jj_3R_218() {
    return this.jj_scan_token(80)?true:this.jj_3R_117();
  }

  private boolean jj_3R_126() {
    if(this.jj_scan_token(46)) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_196()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(79);
    }
  }

  private boolean jj_3_27() {
    if(this.jj_3R_43()) {
      return true;
    } else if(this.jj_3R_32()) {
      return true;
    } else {
      Token xsp = this.jj_scanpos;
      if(this.jj_3R_47()) {
        this.jj_scanpos = xsp;
      }

      return this.jj_scan_token(70);
    }
  }

  private boolean jj_3_1() {
    return this.jj_3R_28();
  }

  private boolean jj_3R_224() {
    return this.jj_scan_token(73)?true:(this.jj_3R_32()?true:(this.jj_scan_token(74)?true:this.jj_3R_199()));
  }

  private boolean jj_3_23() {
    return this.jj_3R_28();
  }

  private boolean jj_3R_133() {
    return this.jj_scan_token(34)?true:(this.jj_scan_token(105)?true:this.jj_scan_token(79));
  }

  private boolean jj_3R_144() {
    return this.jj_scan_token(64);
  }

  private boolean jj_3R_154() {
    if(this.jj_3R_160()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_169());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_197() {
    return this.jj_scan_token(16)?true:(this.jj_scan_token(73)?true:(this.jj_3R_114()?true:(this.jj_scan_token(74)?true:this.jj_3R_40())));
  }

  private boolean jj_3R_103() {
    return this.jj_scan_token(73)?true:(this.jj_3R_41()?true:this.jj_scan_token(74));
  }

  private boolean jj_3R_49() {
    return this.jj_3R_95();
  }

  private boolean jj_3R_28() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3R_49()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_50()) {
        this.jj_scanpos = xsp;
        if(this.jj_3R_51()) {
          this.jj_scanpos = xsp;
          if(this.jj_3R_52()) {
            this.jj_scanpos = xsp;
            if(this.jj_3_28()) {
              this.jj_scanpos = xsp;
              if(this.jj_3R_53()) {
                this.jj_scanpos = xsp;
                if(this.jj_3R_54()) {
                  this.jj_scanpos = xsp;
                  if(this.jj_3R_55()) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  private boolean jj_3R_196() {
    return this.jj_3R_41();
  }

  private boolean jj_3R_34() {
    return this.jj_3R_60();
  }

  private boolean jj_3R_115() {
    return this.jj_scan_token(80)?true:this.jj_3R_114();
  }

  private boolean jj_3_28() {
    return this.jj_3R_48();
  }

  private boolean jj_3R_114() {
    Token xsp = this.jj_scanpos;
    if(this.jj_3_5()) {
      this.jj_scanpos = xsp;
      if(this.jj_3R_141()) {
        return true;
      }
    }

    return false;
  }

  private boolean jj_3R_79() {
    if(this.jj_3R_114()) {
      return true;
    } else {
      Token xsp;
      do {
        xsp = this.jj_scanpos;
      } while(!this.jj_3R_115());

      this.jj_scanpos = xsp;
      return false;
    }
  }

  private boolean jj_3R_157() {
    return this.jj_3R_60();
  }

  public Parser(InputStream stream) {
    this(stream, (String)null);
  }

  public Parser(InputStream stream, String encoding) {
    this.jjtree = new JJTParserState();
    this.retainComments = false;
    this.jj_lookingAhead = false;
    this.jj_ls = new Parser.LookaheadSuccess((Parser.LookaheadSuccess)null);

    try {
      this.jj_input_stream = new JavaCharStream(stream, encoding, 1, 1);
    } catch (UnsupportedEncodingException var4) {
      throw new RuntimeException(var4);
    }

    this.token_source = new ParserTokenManager(this.jj_input_stream);
    this.token = new Token();
    this.jj_ntk = -1;
  }

  public void ReInit(InputStream stream) {
    this.ReInit(stream, (String)null);
  }

  public void ReInit(InputStream stream, String encoding) {
    try {
      this.jj_input_stream.ReInit(stream, encoding, 1, 1);
    } catch (UnsupportedEncodingException var4) {
      throw new RuntimeException(var4);
    }

    this.token_source.ReInit(this.jj_input_stream);
    this.token = new Token();
    this.jj_ntk = -1;
    this.jjtree.reset();
  }

  public Parser(Reader stream) {
    this.jjtree = new JJTParserState();
    this.retainComments = false;
    this.jj_lookingAhead = false;
    this.jj_ls = new Parser.LookaheadSuccess((Parser.LookaheadSuccess)null);
    this.jj_input_stream = new JavaCharStream(stream, 1, 1);
    this.token_source = new ParserTokenManager(this.jj_input_stream);
    this.token = new Token();
    this.jj_ntk = -1;
  }

  public void ReInit(Reader stream) {
    this.jj_input_stream.ReInit((Reader)stream, 1, 1);
    this.token_source.ReInit(this.jj_input_stream);
    this.token = new Token();
    this.jj_ntk = -1;
    this.jjtree.reset();
  }

  public Parser(ParserTokenManager tm) {
    this.jjtree = new JJTParserState();
    this.retainComments = false;
    this.jj_lookingAhead = false;
    this.jj_ls = new Parser.LookaheadSuccess((Parser.LookaheadSuccess)null);
    this.token_source = tm;
    this.token = new Token();
    this.jj_ntk = -1;
  }

  public void ReInit(ParserTokenManager tm) {
    this.token_source = tm;
    this.token = new Token();
    this.jj_ntk = -1;
    this.jjtree.reset();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken = this.token;
    if(this.token.next != null) {
      this.token = this.token.next;
    } else {
      this.token = this.token.next = this.token_source.getNextToken();
    }

    this.jj_ntk = -1;
    if(this.token.kind == kind) {
      return this.token;
    } else {
      this.token = oldToken;
      throw this.generateParseException();
    }
  }

  private boolean jj_scan_token(int kind) {
    if(this.jj_scanpos == this.jj_lastpos) {
      --this.jj_la;
      if(this.jj_scanpos.next == null) {
        this.jj_lastpos = this.jj_scanpos = this.jj_scanpos.next = this.token_source.getNextToken();
      } else {
        this.jj_lastpos = this.jj_scanpos = this.jj_scanpos.next;
      }
    } else {
      this.jj_scanpos = this.jj_scanpos.next;
    }

    if(this.jj_scanpos.kind != kind) {
      return true;
    } else if(this.jj_la == 0 && this.jj_scanpos == this.jj_lastpos) {
      throw this.jj_ls;
    } else {
      return false;
    }
  }

  public final Token getNextToken() {
    if(this.token.next != null) {
      this.token = this.token.next;
    } else {
      this.token = this.token.next = this.token_source.getNextToken();
    }

    this.jj_ntk = -1;
    return this.token;
  }

  public final Token getToken(int index) {
    Token t = this.jj_lookingAhead?this.jj_scanpos:this.token;

    for(int i = 0; i < index; ++i) {
      if(t.next != null) {
        t = t.next;
      } else {
        t = t.next = this.token_source.getNextToken();
      }
    }

    return t;
  }

  private int jj_ntk() {
    return (this.jj_nt = this.token.next) == null?(this.jj_ntk = (this.token.next = this.token_source.getNextToken()).kind):(this.jj_ntk = this.jj_nt.kind);
  }

  public ParseException generateParseException() {
    Token errortok = this.token.next;
    int line = errortok.beginLine;
    int column = errortok.beginColumn;
    String mess = errortok.kind == 0?tokenImage[0]:errortok.image;
    return new ParseException("Parse error at line " + line + ", column " + column + ".  Encountered: " + mess);
  }

  public final void enable_tracing() {
  }

  public final void disable_tracing() {
  }

  private static final class LookaheadSuccess extends Error {
    private LookaheadSuccess() {
    }

    // $FF: synthetic method
    LookaheadSuccess(Parser.LookaheadSuccess var1) {
      this();
    }
  }
}
