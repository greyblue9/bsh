package bsh;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.Primitive;
import bsh.SimpleNode;

public final class BSHLiteral extends SimpleNode {
  public static volatile boolean internStrings = true;
  public Object value;

  BSHLiteral(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    if(this.value == null) {
      throw new InterpreterError("Null in bsh literal: " + this.value);
    } else {
      return this.value;
    }
  }

  private char getEscapeChar(char ch) {
    switch(ch) {
    case '\"':
    case '\'':
    case '\\':
    default:
      break;
    case 'b':
      ch = 8;
      break;
    case 'f':
      ch = 12;
      break;
    case 'n':
      ch = 10;
      break;
    case 'r':
      ch = 13;
      break;
    case 't':
      ch = 9;
    }

    return ch;
  }

  public void charSetup(String str) {
    char ch = str.charAt(0);
    if(ch == 92) {
      ch = str.charAt(1);
      if(Character.isDigit(ch)) {
        ch = (char)Integer.parseInt(str.substring(1), 8);
      } else {
        ch = this.getEscapeChar(ch);
      }
    }

    this.value = new Primitive((new Character(ch)).charValue());
  }

  void stringSetup(String str) {
    StringBuilder buffer = new StringBuilder();
    int len = str.length();

    for(int s = 0; s < len; ++s) {
      char ch = str.charAt(s);
      if(ch == 92) {
        ++s;
        ch = str.charAt(s);
        if(!Character.isDigit(ch)) {
          ch = this.getEscapeChar(ch);
        } else {
          int endPos = s;

          for(int max = Math.min(s + 2, len - 1); endPos < max && Character.isDigit(str.charAt(endPos + 1)); ++endPos) {
            ;
          }

          ch = (char)Integer.parseInt(str.substring(s, endPos + 1), 8);
          s = endPos;
        }
      }

      buffer.append(ch);
    }

    String var8 = buffer.toString();
    if(internStrings) {
      var8 = var8.intern();
    }

    this.value = var8;
  }
}
