package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Node;
import bsh.Token;
import java.io.Serializable;


public final class ParseException extends EvalError implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private String sourceFile;
  protected boolean specialConstructor;
  public Token currentToken;
  public int[][] expectedTokenSequences;
  public String[] tokenImage;
  protected String eol;

  public ParseException(Token currentTokenVal, int[][] expectedTokenSequencesVal, String[] tokenImageVal) {
    super(initialise(currentTokenVal, expectedTokenSequencesVal, tokenImageVal), (Node)null, (CallStack)null);
    this.sourceFile = "<unknown>";
    this.eol = System.getProperty("line.separator", "\n");
    this.specialConstructor = false;
    this.currentToken = currentTokenVal;
    this.expectedTokenSequences = expectedTokenSequencesVal;
    this.tokenImage = tokenImageVal;
  }

  public void setErrorSourceFile(String file) {
    this.sourceFile = file;
  }

  public String getErrorSourceFile() {
    return this.sourceFile;
  }

  public ParseException() {
    this("");
    this.specialConstructor = false;
  }

  public ParseException(String message) {
    super(message);
    this.sourceFile = "<unknown>";
    this.eol = System.getProperty("line.separator", "\n");
    this.specialConstructor = false;
  }

  public ParseException(String message, Throwable cause) {
    super(message, cause);
    this.sourceFile = "<unknown>";
    this.eol = System.getProperty("line.separator", "\n");
    this.specialConstructor = false;
  }

  public String getMessage() {
    return this.getMessage(false);
  }

  public String getMessage(boolean debug) {
    if(!this.specialConstructor) {
      return super.getRawMessage();
    } else {
      String expected = "";
      int maxSize = 0;

      for(int retval = 0; retval < this.expectedTokenSequences.length; ++retval) {
        if(maxSize < this.expectedTokenSequences[retval].length) {
          maxSize = this.expectedTokenSequences[retval].length;
        }

        for(int tok = 0; tok < this.expectedTokenSequences[retval].length; ++tok) {
          expected = expected + this.tokenImage[this.expectedTokenSequences[retval][tok]] + " ";
        }

        if(this.expectedTokenSequences[retval][this.expectedTokenSequences[retval].length - 1] != 0) {
          expected = expected + "...";
        }

        expected = expected + this.eol + "    ";
      }

      String var7 = "In file: " + this.sourceFile + " Encountered \"";
      Token var8 = this.currentToken.next;

      for(int i = 0; i < maxSize; ++i) {
        if(i != 0) {
          var7 = var7 + " ";
        }

        if(var8.kind == 0) {
          var7 = var7 + this.tokenImage[0];
          break;
        }

        var7 = var7 + add_escapes(var8.image);
        var8 = var8.next;
      }

      var7 = var7 + "\" at line " + this.currentToken.next.beginLine + ", column " + this.currentToken.next.beginColumn + "." + this.eol;
      if(debug) {
        if(this.expectedTokenSequences.length == 1) {
          var7 = var7 + "Was expecting:" + this.eol + "    ";
        } else {
          var7 = var7 + "Was expecting one of:" + this.eol + "    ";
        }

        var7 = var7 + expected;
      }

      return var7;
    }
  }

  private static String initialise(Token currentToken, int[][] expectedTokenSequences, String[] tokenImage) {
    String eol = System.getProperty("line.separator", "\n");
    StringBuffer expected = new StringBuffer();
    int maxSize = 0;

    for(int retval = 0; retval < expectedTokenSequences.length; ++retval) {
      if(maxSize < expectedTokenSequences[retval].length) {
        maxSize = expectedTokenSequences[retval].length;
      }

      for(int tok = 0; tok < expectedTokenSequences[retval].length; ++tok) {
        expected.append(tokenImage[expectedTokenSequences[retval][tok]]).append(' ');
      }

      if(expectedTokenSequences[retval][expectedTokenSequences[retval].length - 1] != 0) {
        expected.append("...");
      }

      expected.append(eol).append("    ");
    }

    String var9 = "Encountered \"";
    Token var10 = currentToken.next;

    for(int i = 0; i < maxSize; ++i) {
      if(i != 0) {
        var9 = var9 + " ";
      }

      if(var10.kind == 0) {
        var9 = var9 + tokenImage[0];
        break;
      }

      var9 = var9 + " " + tokenImage[var10.kind];
      var9 = var9 + " \"";
      var9 = var9 + add_escapes(var10.image);
      var9 = var9 + " \"";
      var10 = var10.next;
    }

    var9 = var9 + "\" at line " + currentToken.next.beginLine + ", column " + currentToken.next.beginColumn;
    var9 = var9 + "." + eol;
    if(expectedTokenSequences.length == 1) {
      var9 = var9 + "Was expecting:" + eol + "    ";
    } else {
      var9 = var9 + "Was expecting one of:" + eol + "    ";
    }

    var9 = var9 + expected.toString();
    return var9;
  }

  static String add_escapes(String str) {
    StringBuffer retval = new StringBuffer();

    for(int i = 0; i < str.length(); ++i) {
      switch(str.charAt(i)) {
      case '\u0000':
        break;
      case '\b':
        retval.append("\\b");
        break;
      case '\t':
        retval.append("\\t");
        break;
      case '\n':
        retval.append("\\n");
        break;
      case '\f':
        retval.append("\\f");
        break;
      case '\r':
        retval.append("\\r");
        break;
      case '\"':
        retval.append("\\\"");
        break;
      case '\'':
        retval.append("\\\'");
        break;
      case '\\':
        retval.append("\\\\");
        break;
      default:
        char ch;
        if((ch = str.charAt(i)) >= 32 && ch <= 126) {
          retval.append(ch);
        } else {
          String s = "0000" + Integer.toString(ch, 16);
          retval.append("\\u" + s.substring(s.length() - 4, s.length()));
        }
      }
    }

    return retval.toString();
  }

  public int getErrorLineNumber() {
    if(this.currentToken != null) {
      return this.currentToken.next.beginLine;
    } else {
      String message = this.getMessage();
      int index = message.indexOf(" at line ");
      if(index > -1) {
        message = message.substring(index + 9);
        index = message.indexOf(44);

        try {
          if(index == -1) {
            return Integer.parseInt(message);
          }

          return Integer.parseInt(message.substring(0, index));
        } catch (NumberFormatException var4) {
          ;
        }
      }

      return -1;
    }
  }

  public String getErrorText() {
    int maxSize = 0;

    for(int retval = 0; retval < this.expectedTokenSequences.length; ++retval) {
      if(maxSize < this.expectedTokenSequences[retval].length) {
        maxSize = this.expectedTokenSequences[retval].length;
      }
    }

    String var5 = "";
    Token tok = this.currentToken.next;

    for(int i = 0; i < maxSize; ++i) {
      if(i != 0) {
        var5 = var5 + " ";
      }

      if(tok.kind == 0) {
        var5 = var5 + this.tokenImage[0];
        break;
      }

      var5 = var5 + add_escapes(tok.image);
      tok = tok.next;
    }

    return var5;
  }
}