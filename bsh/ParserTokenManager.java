package bsh;

import bsh.JavaCharStream;
import bsh.ParserConstants;
import bsh.Token;
import bsh.TokenMgrError;
import java.io.IOException;
import java.io.PrintStream;

public class ParserTokenManager implements ParserConstants {
  public PrintStream debugStream;
  static final long[] jjbitVec0 = new long[]{0L, 0L, -1L, -1L};
  static final long[] jjbitVec1 = new long[]{-2L, -1L, -1L, -1L};
  static final long[] jjbitVec3 = new long[]{2301339413881290750L, -16384L, 4294967295L, 432345564227567616L};
  static final long[] jjbitVec4 = new long[]{0L, 0L, 0L, -36028797027352577L};
  static final long[] jjbitVec5 = new long[]{0L, -1L, -1L, -1L};
  static final long[] jjbitVec6 = new long[]{-1L, -1L, 65535L, 0L};
  static final long[] jjbitVec7 = new long[]{-1L, -1L, 0L, 0L};
  static final long[] jjbitVec8 = new long[]{70368744177663L, 0L, 0L, 0L};
  static final int[] jjnextStates = new int[]{66, 67, 69, 46, 47, 52, 53, 56, 57, 15, 65, 70, 82, 17, 19, 61, 63, 9, 26, 27, 29, 2, 3, 5, 11, 12, 15, 26, 27, 31, 29, 35, 38, 39, 35, 40, 38, 48, 49, 15, 56, 57, 15, 72, 73, 75, 78, 79, 81, 13, 14, 20, 21, 23, 28, 30, 32, 50, 51, 54, 55, 58, 59};
  public static final String[] jjstrLiteralImages = new String[]{"", null, null, null, null, null, null, null, null, null, "abstract", "boolean", "break", "class", "byte", "case", "catch", "char", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "switch", "synchronized", "transient", "throw", "throws", "true", "try", "void", "volatile", "while", null, null, null, null, null, null, null, null, null, null, null, null, null, "(", ")", "{", "}", "[", "]", ";", ",", ".", "=", ">", "@gt", "<", "@lt", "!", "~", "?", ":", "==", "<=", "@lteq", ">=", "@gteq", "!=", "||", "@or", "&&", "@and", "++", "--", "+", "-", "*", "/", "&", "@bitwise_and", "|", "@bitwise_or", "^", "%", "<<", "@left_shift", ">>", "@right_shift", ">>>", "@right_unsigned_shift", "+=", "-=", "*=", "/=", "&=", "@and_assign", "|=", "@or_assign", "^=", "%=", "<<=", "@left_shift_assign", ">>=", "@right_shift_assign", ">>>=", "@right_unsigned_shift_assign", "..."};
  public static final String[] lexStateNames = new String[]{"DEFAULT"};
  static final long[] jjtoToken = new long[]{2305843009213692929L, -387L, 255L};
  static final long[] jjtoSkip = new long[]{1022L, 0L, 0L};
  static final long[] jjtoSpecial = new long[]{896L, 0L, 0L};
  protected JavaCharStream input_stream;
  private final int[] jjrounds;
  private final int[] jjstateSet;
  protected char curChar;
  int curLexState;
  int defaultLexState;
  int jjnewStateCnt;
  int jjround;
  int jjmatchedPos;
  int jjmatchedKind;

  public void setDebugStream(PrintStream ds) {
    this.debugStream = ds;
  }

  private final int jjStopStringLiteralDfa_0(int pos, long active0, long active1, long active2) {
    switch(pos) {
    case 0:
      if((active1 & 131072L) == 0L && (active2 & 128L) == 0L) {
        if((active0 & 1152921504606845952L) != 0L) {
          this.jjmatchedKind = 70;
          return 44;
        }

        if((active0 & 62L) != 0L) {
          return 0;
        }

        if((active1 & 288234774198222848L) != 0L) {
          return 65;
        }

        return -1;
      }

      return 11;
    case 1:
      if((active0 & 4301258752L) != 0L) {
        return 44;
      } else {
        if((active0 & 1152921500305587200L) != 0L) {
          if(this.jjmatchedPos != 1) {
            this.jjmatchedKind = 70;
            this.jjmatchedPos = 1;
          }

          return 44;
        }

        return -1;
      }
    case 2:
      if((active0 & 72058900781727744L) != 0L) {
        return 44;
      } else {
        if((active0 & 1080862599528053760L) != 0L) {
          if(this.jjmatchedPos != 2) {
            this.jjmatchedKind = 70;
            this.jjmatchedPos = 2;
          }

          return 44;
        }

        return -1;
      }
    case 3:
      if((active0 & 900716275798195200L) != 0L) {
        if(this.jjmatchedPos != 3) {
          this.jjmatchedKind = 70;
          this.jjmatchedPos = 3;
        }

        return 44;
      } else {
        if((active0 & 180146461168812032L) != 0L) {
          return 44;
        }

        return -1;
      }
    case 4:
      if((active0 & 603623088562974720L) != 0L) {
        return 44;
      } else {
        if((active0 & 297093187235220480L) != 0L) {
          if(this.jjmatchedPos != 4) {
            this.jjmatchedKind = 70;
            this.jjmatchedPos = 4;
          }

          return 44;
        }

        return -1;
      }
    case 5:
      if((active0 & 19527893449179136L) != 0L) {
        return 44;
      } else {
        if((active0 & 295579692563958784L) != 0L) {
          this.jjmatchedKind = 70;
          this.jjmatchedPos = 5;
          return 44;
        }

        return -1;
      }
    case 6:
      if((active0 & 13194442573824L) != 0L) {
        return 44;
      } else {
        if((active0 & 295566498121384960L) != 0L) {
          this.jjmatchedKind = 70;
          this.jjmatchedPos = 6;
          return 44;
        }

        return -1;
      }
    case 7:
      if((active0 & 6773172015726592L) != 0L) {
        this.jjmatchedKind = 70;
        this.jjmatchedPos = 7;
        return 44;
      } else {
        if((active0 & 288793326105658368L) != 0L) {
          return 44;
        }

        return -1;
      }
    case 8:
      if((active0 & 4521329252368384L) != 0L) {
        return 44;
      } else {
        if((active0 & 2251842763358208L) != 0L) {
          this.jjmatchedKind = 70;
          this.jjmatchedPos = 8;
          return 44;
        }

        return -1;
      }
    case 9:
      if((active0 & 2251799813685248L) != 0L) {
        this.jjmatchedKind = 70;
        this.jjmatchedPos = 9;
        return 44;
      } else {
        if((active0 & 42949672960L) != 0L) {
          return 44;
        }

        return -1;
      }
    case 10:
      if((active0 & 2251799813685248L) != 0L) {
        if(this.jjmatchedPos != 10) {
          this.jjmatchedKind = 70;
          this.jjmatchedPos = 10;
        }

        return 44;
      }

      return -1;
    case 11:
      if((active0 & 2251799813685248L) != 0L) {
        return 44;
      }

      return -1;
    default:
      return -1;
    }
  }

  private final int jjStartNfa_0(int pos, long active0, long active1, long active2) {
    return this.jjMoveNfa_0(this.jjStopStringLiteralDfa_0(pos, active0, active1, active2), pos + 1);
  }

  private int jjStopAtPos(int pos, int kind) {
    this.jjmatchedKind = kind;
    this.jjmatchedPos = pos;
    return pos + 1;
  }

  private int jjMoveStringLiteralDfa0_0() {
    switch(this.curChar) {
    case '\t':
      return this.jjStartNfaWithStates_0(0, 2, 0);
    case '\n':
      return this.jjStartNfaWithStates_0(0, 5, 0);
    case '\f':
      return this.jjStartNfaWithStates_0(0, 4, 0);
    case '\r':
      return this.jjStartNfaWithStates_0(0, 3, 0);
    case ' ':
      return this.jjStartNfaWithStates_0(0, 1, 0);
    case '!':
      this.jjmatchedKind = 87;
      return this.jjMoveStringLiteralDfa1_0(0L, 4294967296L, 0L);
    case '%':
      this.jjmatchedKind = 112;
      return this.jjMoveStringLiteralDfa1_0(0L, 0L, 1L);
    case '&':
      this.jjmatchedKind = 107;
      return this.jjMoveStringLiteralDfa1_0(0L, 576460786663161856L, 0L);
    case '(':
      return this.jjStopAtPos(0, 73);
    case ')':
      return this.jjStopAtPos(0, 74);
    case '*':
      this.jjmatchedKind = 105;
      return this.jjMoveStringLiteralDfa1_0(0L, 144115188075855872L, 0L);
    case '+':
      this.jjmatchedKind = 103;
      return this.jjMoveStringLiteralDfa1_0(0L, 36028934457917440L, 0L);
    case ',':
      return this.jjStopAtPos(0, 80);
    case '-':
      this.jjmatchedKind = 104;
      return this.jjMoveStringLiteralDfa1_0(0L, 72057868915834880L, 0L);
    case '.':
      this.jjmatchedKind = 81;
      return this.jjMoveStringLiteralDfa1_0(0L, 0L, 128L);
    case '/':
      this.jjmatchedKind = 106;
      return this.jjMoveStringLiteralDfa1_0(0L, 288230376151711744L, 0L);
    case ':':
      return this.jjStopAtPos(0, 90);
    case ';':
      return this.jjStopAtPos(0, 79);
    case '<':
      this.jjmatchedKind = 85;
      return this.jjMoveStringLiteralDfa1_0(0L, 562950221856768L, 2L);
    case '=':
      this.jjmatchedKind = 82;
      return this.jjMoveStringLiteralDfa1_0(0L, 134217728L, 0L);
    case '>':
      this.jjmatchedKind = 83;
      return this.jjMoveStringLiteralDfa1_0(0L, 11259000142168064L, 40L);
    case '?':
      return this.jjStopAtPos(0, 89);
    case '@':
      return this.jjMoveStringLiteralDfa1_0(0L, 5788339470597095424L, 84L);
    case '[':
      return this.jjStopAtPos(0, 77);
    case ']':
      return this.jjStopAtPos(0, 78);
    case '^':
      this.jjmatchedKind = 111;
      return this.jjMoveStringLiteralDfa1_0(0L, Long.MIN_VALUE, 0L);
    case 'a':
      return this.jjMoveStringLiteralDfa1_0(1024L, 0L, 0L);
    case 'b':
      return this.jjMoveStringLiteralDfa1_0(22528L, 0L, 0L);
    case 'c':
      return this.jjMoveStringLiteralDfa1_0(1024000L, 0L, 0L);
    case 'd':
      return this.jjMoveStringLiteralDfa1_0(7340032L, 0L, 0L);
    case 'e':
      return this.jjMoveStringLiteralDfa1_0(58720256L, 0L, 0L);
    case 'f':
      return this.jjMoveStringLiteralDfa1_0(2080374784L, 0L, 0L);
    case 'g':
      return this.jjMoveStringLiteralDfa1_0(2147483648L, 0L, 0L);
    case 'i':
      return this.jjMoveStringLiteralDfa1_0(270582939648L, 0L, 0L);
    case 'l':
      return this.jjMoveStringLiteralDfa1_0(274877906944L, 0L, 0L);
    case 'n':
      return this.jjMoveStringLiteralDfa1_0(3848290697216L, 0L, 0L);
    case 'p':
      return this.jjMoveStringLiteralDfa1_0(65970697666560L, 0L, 0L);
    case 'r':
      return this.jjMoveStringLiteralDfa1_0(70368744177664L, 0L, 0L);
    case 's':
      return this.jjMoveStringLiteralDfa1_0(4362862139015168L, 0L, 0L);
    case 't':
      return this.jjMoveStringLiteralDfa1_0(139611588448485376L, 0L, 0L);
    case 'v':
      return this.jjMoveStringLiteralDfa1_0(432345564227567616L, 0L, 0L);
    case 'w':
      return this.jjMoveStringLiteralDfa1_0(576460752303423488L, 0L, 0L);
    case '{':
      return this.jjStopAtPos(0, 75);
    case '|':
      this.jjmatchedKind = 109;
      return this.jjMoveStringLiteralDfa1_0(0L, 2305843017803628544L, 0L);
    case '}':
      return this.jjStopAtPos(0, 76);
    case '~':
      return this.jjStopAtPos(0, 88);
    default:
      return this.jjMoveNfa_0(6, 0);
    }
  }

  private int jjMoveStringLiteralDfa1_0(long active0, long active1, long active2) {
    try {
      this.curChar = this.input_stream.readChar();
    } catch (IOException var8) {
      this.jjStopStringLiteralDfa_0(0, active0, active1, active2);
      return 1;
    }

    switch(this.curChar) {
    case '&':
      if((active1 & 34359738368L) != 0L) {
        return this.jjStopAtPos(1, 99);
      }
      break;
    case '+':
      if((active1 & 137438953472L) != 0L) {
        return this.jjStopAtPos(1, 101);
      }
      break;
    case '-':
      if((active1 & 274877906944L) != 0L) {
        return this.jjStopAtPos(1, 102);
      }
      break;
    case '.':
      return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 0L, active2, 128L);
    case '<':
      if((active1 & 562949953421312L) != 0L) {
        this.jjmatchedKind = 113;
        this.jjmatchedPos = 1;
      }

      return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 0L, active2, 2L);
    case '=':
      if((active1 & 134217728L) != 0L) {
        return this.jjStopAtPos(1, 91);
      }

      if((active1 & 268435456L) != 0L) {
        return this.jjStopAtPos(1, 92);
      }

      if((active1 & 1073741824L) != 0L) {
        return this.jjStopAtPos(1, 94);
      }

      if((active1 & 4294967296L) != 0L) {
        return this.jjStopAtPos(1, 96);
      }

      if((active1 & 36028797018963968L) != 0L) {
        return this.jjStopAtPos(1, 119);
      }

      if((active1 & 72057594037927936L) != 0L) {
        return this.jjStopAtPos(1, 120);
      }

      if((active1 & 144115188075855872L) != 0L) {
        return this.jjStopAtPos(1, 121);
      }

      if((active1 & 288230376151711744L) != 0L) {
        return this.jjStopAtPos(1, 122);
      }

      if((active1 & 576460752303423488L) != 0L) {
        return this.jjStopAtPos(1, 123);
      }

      if((active1 & 2305843009213693952L) != 0L) {
        return this.jjStopAtPos(1, 125);
      }

      if((active1 & Long.MIN_VALUE) != 0L) {
        return this.jjStopAtPos(1, 127);
      }

      if((active2 & 1L) != 0L) {
        return this.jjStopAtPos(1, 128);
      }
      break;
    case '>':
      if((active1 & 2251799813685248L) != 0L) {
        this.jjmatchedKind = 115;
        this.jjmatchedPos = 1;
      }

      return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 9007199254740992L, active2, 40L);
    case 'a':
      return this.jjMoveStringLiteralDfa2_0(active0, 4947869532160L, active1, 1152921573326323712L, active2, 0L);
    case 'b':
      return this.jjMoveStringLiteralDfa2_0(active0, 1024L, active1, 87960930222080L, active2, 0L);
    case 'e':
      return this.jjMoveStringLiteralDfa2_0(active0, 71468256854016L, active1, 0L, active2, 0L);
    case 'f':
      if((active0 & 4294967296L) != 0L) {
        return this.jjStartNfaWithStates_0(1, 32, 44);
      }
      break;
    case 'g':
      return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 2148532224L, active2, 0L);
    case 'h':
      return this.jjMoveStringLiteralDfa2_0(active0, 603623087556132864L, active1, 0L, active2, 0L);
    case 'i':
      return this.jjMoveStringLiteralDfa2_0(active0, 402653184L, active1, 0L, active2, 0L);
    case 'l':
      return this.jjMoveStringLiteralDfa2_0(active0, 545267712L, active1, 1125900447907840L, active2, 4L);
    case 'm':
      return this.jjMoveStringLiteralDfa2_0(active0, 25769803776L, active1, 0L, active2, 0L);
    case 'n':
      return this.jjMoveStringLiteralDfa2_0(active0, 240534945792L, active1, 0L, active2, 0L);
    case 'o':
      if((active0 & 2097152L) != 0L) {
        this.jjmatchedKind = 21;
        this.jjmatchedPos = 1;
      }

      return this.jjMoveStringLiteralDfa2_0(active0, 432345842331682816L, active1, 4611686035607257088L, active2, 0L);
    case 'r':
      return this.jjMoveStringLiteralDfa2_0(active0, 112616378963333120L, active1, 22517998136852480L, active2, 80L);
    case 't':
      return this.jjMoveStringLiteralDfa2_0(active0, 844424930131968L, active1, 0L, active2, 0L);
    case 'u':
      return this.jjMoveStringLiteralDfa2_0(active0, 37383395344384L, active1, 0L, active2, 0L);
    case 'w':
      return this.jjMoveStringLiteralDfa2_0(active0, 1125899906842624L, active1, 0L, active2, 0L);
    case 'x':
      return this.jjMoveStringLiteralDfa2_0(active0, 33554432L, active1, 0L, active2, 0L);
    case 'y':
      return this.jjMoveStringLiteralDfa2_0(active0, 2251799813701632L, active1, 0L, active2, 0L);
    case '|':
      if((active1 & 8589934592L) != 0L) {
        return this.jjStopAtPos(1, 97);
      }
    }

    return this.jjStartNfa_0(0, active0, active1, active2);
  }

  private int jjMoveStringLiteralDfa2_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(0, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(1, active0, active1, active2);
        return 2;
      }

      switch(this.curChar) {
      case '.':
        if((active2 & 128L) != 0L) {
          return this.jjStopAtPos(2, 135);
        }
        break;
      case '=':
        if((active2 & 2L) != 0L) {
          return this.jjStopAtPos(2, 129);
        }

        if((active2 & 8L) != 0L) {
          return this.jjStopAtPos(2, 131);
        }
        break;
      case '>':
        if((active1 & 9007199254740992L) != 0L) {
          this.jjmatchedKind = 117;
          this.jjmatchedPos = 2;
        }

        return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 0L, active2, 32L);
      case 'a':
        return this.jjMoveStringLiteralDfa3_0(active0, 4785074604220416L, active1, 0L, active2, 0L);
      case 'b':
        return this.jjMoveStringLiteralDfa3_0(active0, 35184372088832L, active1, 0L, active2, 0L);
      case 'c':
        return this.jjMoveStringLiteralDfa3_0(active0, 4398046511104L, active1, 0L, active2, 0L);
      case 'e':
        return this.jjMoveStringLiteralDfa3_0(active0, 4096L, active1, 1125899906842624L, active2, 4L);
      case 'f':
        return this.jjMoveStringLiteralDfa3_0(active0, 1048576L, active1, 0L, active2, 0L);
      case 'i':
        return this.jjMoveStringLiteralDfa3_0(active0, 721710636379144192L, active1, 22605959067074560L, active2, 80L);
      case 'l':
        return this.jjMoveStringLiteralDfa3_0(active0, 288232575242076160L, active1, 0L, active2, 0L);
      case 'n':
        return this.jjMoveStringLiteralDfa3_0(active0, 2252075095031808L, active1, 1152921573326323712L, active2, 0L);
      case 'o':
        return this.jjMoveStringLiteralDfa3_0(active0, 158330211272704L, active1, 0L, active2, 0L);
      case 'p':
        return this.jjMoveStringLiteralDfa3_0(active0, 25769803776L, active1, 0L, active2, 0L);
      case 'r':
        if((active0 & 1073741824L) != 0L) {
          return this.jjStartNfaWithStates_0(2, 30, 44);
        }

        if((active1 & 17179869184L) != 0L) {
          this.jjmatchedKind = 98;
          this.jjmatchedPos = 2;
        }

        return this.jjMoveStringLiteralDfa3_0(active0, 27584547717644288L, active1, 4611686018427387904L, active2, 0L);
      case 's':
        return this.jjMoveStringLiteralDfa3_0(active0, 34368160768L, active1, 0L, active2, 0L);
      case 't':
        if((active0 & 68719476736L) != 0L) {
          this.jjmatchedKind = 36;
          this.jjmatchedPos = 2;
        } else if((active1 & 1048576L) != 0L) {
          this.jjmatchedKind = 84;
          this.jjmatchedPos = 2;
        } else if((active1 & 4194304L) != 0L) {
          this.jjmatchedKind = 86;
          this.jjmatchedPos = 2;
        }

        return this.jjMoveStringLiteralDfa3_0(active0, 71058120065024L, active1, 2684354560L, active2, 0L);
      case 'u':
        return this.jjMoveStringLiteralDfa3_0(active0, 36028797039935488L, active1, 0L, active2, 0L);
      case 'w':
        if((active0 & 1099511627776L) != 0L) {
          return this.jjStartNfaWithStates_0(2, 40, 44);
        }
        break;
      case 'y':
        if((active0 & 72057594037927936L) != 0L) {
          return this.jjStartNfaWithStates_0(2, 56, 44);
        }
      }

      return this.jjStartNfa_0(1, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa3_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(1, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(2, active0, active1, active2);
        return 3;
      }

      switch(this.curChar) {
      case '=':
        if((active2 & 32L) != 0L) {
          return this.jjStopAtPos(3, 133);
        }
        break;
      case '_':
        return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 4611686018427387904L, active2, 0L);
      case 'a':
        return this.jjMoveStringLiteralDfa4_0(active0, 288230377092288512L, active1, 0L, active2, 0L);
      case 'b':
        return this.jjMoveStringLiteralDfa4_0(active0, 4194304L, active1, 0L, active2, 0L);
      case 'c':
        return this.jjMoveStringLiteralDfa4_0(active0, 2251799813750784L, active1, 0L, active2, 0L);
      case 'd':
        if((active0 & 144115188075855872L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 57, 44);
        }

        if((active1 & 68719476736L) != 0L) {
          this.jjmatchedKind = 100;
          this.jjmatchedPos = 3;
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 1152921504606846976L, active2, 0L);
      case 'e':
        if((active0 & 16384L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 14, 44);
        }

        if((active0 & 32768L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 15, 44);
        }

        if((active0 & 8388608L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 23, 44);
        }

        if((active0 & 36028797018963968L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 55, 44);
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 137472507904L, active1, 2684354560L, active2, 0L);
      case 'f':
        return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 1125899906842624L, active2, 4L);
      case 'g':
        if((active0 & 274877906944L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 38, 44);
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 22517998136852480L, active2, 80L);
      case 'i':
        return this.jjMoveStringLiteralDfa4_0(active0, 563499709235200L, active1, 0L, active2, 0L);
      case 'k':
        return this.jjMoveStringLiteralDfa4_0(active0, 4398046511104L, active1, 0L, active2, 0L);
      case 'l':
        if((active0 & 2199023255552L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 41, 44);
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 576495945265448960L, active1, 0L, active2, 0L);
      case 'm':
        if((active0 & 16777216L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 24, 44);
        }
        break;
      case 'n':
        return this.jjMoveStringLiteralDfa4_0(active0, 4503599627370496L, active1, 0L, active2, 0L);
      case 'o':
        if((active0 & 2147483648L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 31, 44);
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 27021614944092160L, active1, 0L, active2, 0L);
      case 'r':
        if((active0 & 131072L) != 0L) {
          return this.jjStartNfaWithStates_0(3, 17, 44);
        }

        return this.jjMoveStringLiteralDfa4_0(active0, 140737488355328L, active1, 0L, active2, 0L);
      case 's':
        return this.jjMoveStringLiteralDfa4_0(active0, 67379200L, active1, 0L, active2, 0L);
      case 't':
        return this.jjMoveStringLiteralDfa4_0(active0, 1425001429861376L, active1, 87960930222080L, active2, 0L);
      case 'u':
        return this.jjMoveStringLiteralDfa4_0(active0, 70368744177664L, active1, 0L, active2, 0L);
      case 'v':
        return this.jjMoveStringLiteralDfa4_0(active0, 8796093022208L, active1, 0L, active2, 0L);
      }

      return this.jjStartNfa_0(2, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa4_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(2, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(3, active0, active1, active2);
        return 4;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 1152921504606846976L, active2, 0L);
      case '`':
      case 'b':
      case 'd':
      case 'f':
      case 'g':
      case 'j':
      case 'm':
      case 'o':
      case 'p':
      default:
        break;
      case 'a':
        return this.jjMoveStringLiteralDfa5_0(active0, 13228499271680L, active1, 4611686018427387904L, active2, 0L);
      case 'c':
        return this.jjMoveStringLiteralDfa5_0(active0, 1688849860263936L, active1, 0L, active2, 0L);
      case 'e':
        if((active0 & 67108864L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 26, 44);
        }

        if((active0 & 576460752303423488L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 59, 44);
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 17600775981056L, active1, 0L, active2, 0L);
      case 'h':
        if((active0 & 65536L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 16, 44);
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 2251799813685248L, active1, 22517998136852480L, active2, 80L);
      case 'i':
        return this.jjMoveStringLiteralDfa5_0(active0, 316659349323776L, active1, 0L, active2, 0L);
      case 'k':
        if((active0 & 4096L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 12, 44);
        }
        break;
      case 'l':
        if((active0 & 134217728L) != 0L) {
          this.jjmatchedKind = 27;
          this.jjmatchedPos = 4;
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 272629760L, active1, 0L, active2, 0L);
      case 'n':
        return this.jjMoveStringLiteralDfa5_0(active0, 33554432L, active1, 0L, active2, 0L);
      case 'q':
        if((active1 & 536870912L) != 0L) {
          return this.jjStopAtPos(4, 93);
        }

        if((active1 & 2147483648L) != 0L) {
          return this.jjStopAtPos(4, 95);
        }
        break;
      case 'r':
        return this.jjMoveStringLiteralDfa5_0(active0, 70523363001344L, active1, 0L, active2, 0L);
      case 's':
        if((active0 & 8192L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 13, 44);
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 4503599627370496L, active1, 0L, active2, 0L);
      case 't':
        if((active0 & 262144L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 18, 44);
        }

        if((active0 & 536870912L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 29, 44);
        }

        if((active0 & 140737488355328L) != 0L) {
          return this.jjStartNfaWithStates_0(4, 47, 44);
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 288230376151711744L, active1, 1125899906842624L, active2, 4L);
      case 'u':
        return this.jjMoveStringLiteralDfa5_0(active0, 1048576L, active1, 0L, active2, 0L);
      case 'v':
        return this.jjMoveStringLiteralDfa5_0(active0, 549755813888L, active1, 0L, active2, 0L);
      case 'w':
        if((active0 & 9007199254740992L) != 0L) {
          this.jjmatchedKind = 53;
          this.jjmatchedPos = 4;
        }

        return this.jjMoveStringLiteralDfa5_0(active0, 18014398509481984L, active1, 87960930222080L, active2, 0L);
      }

      return this.jjStartNfa_0(3, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa5_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(3, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(4, active0, active1, active2);
        return 5;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 1125899906842624L, active2, 4L);
      case '`':
      case 'b':
      case 'j':
      case 'k':
      case 'o':
      case 'p':
      case 'q':
      default:
        break;
      case 'a':
        return this.jjMoveStringLiteralDfa6_0(active0, 3072L, active1, 1152921504606846976L, active2, 0L);
      case 'c':
        if((active0 & 35184372088832L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 45, 44);
        }

        if((active0 & 281474976710656L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 48, 44);
        }

        return this.jjMoveStringLiteralDfa6_0(active0, 17592186044416L, active1, 0L, active2, 0L);
      case 'd':
        return this.jjMoveStringLiteralDfa6_0(active0, 33554432L, active1, 0L, active2, 0L);
      case 'e':
        if((active0 & 4194304L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 22, 44);
        }

        if((active0 & 549755813888L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 39, 44);
        }
        break;
      case 'f':
        return this.jjMoveStringLiteralDfa6_0(active0, 137438953472L, active1, 0L, active2, 0L);
      case 'g':
        return this.jjMoveStringLiteralDfa6_0(active0, 4398046511104L, active1, 0L, active2, 0L);
      case 'h':
        if((active0 & 1125899906842624L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 50, 44);
        }
        break;
      case 'i':
        return this.jjMoveStringLiteralDfa6_0(active0, 292733975779082240L, active1, 87960930222080L, active2, 0L);
      case 'l':
        return this.jjMoveStringLiteralDfa6_0(active0, 269484032L, active1, 0L, active2, 0L);
      case 'm':
        return this.jjMoveStringLiteralDfa6_0(active0, 8589934592L, active1, 0L, active2, 0L);
      case 'n':
        if((active0 & 70368744177664L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 46, 44);
        }

        return this.jjMoveStringLiteralDfa6_0(active0, 34360262656L, active1, 0L, active2, 0L);
      case 'r':
        return this.jjMoveStringLiteralDfa6_0(active0, 2251799813685248L, active1, 0L, active2, 0L);
      case 's':
        if((active0 & 18014398509481984L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 54, 44);
        }

        return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4611686018427387904L, active2, 0L);
      case 't':
        if((active0 & 17179869184L) != 0L) {
          return this.jjStartNfaWithStates_0(5, 34, 44);
        }

        return this.jjMoveStringLiteralDfa6_0(active0, 571746046443520L, active1, 22517998136852480L, active2, 80L);
      }

      return this.jjStartNfa_0(4, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa6_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(4, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(5, active0, active1, active2);
        return 6;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 22517998136852480L, active2, 80L);
      case '`':
      case 'b':
      case 'd':
      case 'g':
      case 'h':
      case 'i':
      case 'j':
      case 'k':
      case 'm':
      case 'p':
      case 'q':
      case 'r':
      case 'v':
      case 'w':
      case 'x':
      default:
        break;
      case 'a':
        return this.jjMoveStringLiteralDfa7_0(active0, 137438953472L, active1, 0L, active2, 0L);
      case 'c':
        return this.jjMoveStringLiteralDfa7_0(active0, 34359739392L, active1, 0L, active2, 0L);
      case 'e':
        if((active0 & 4398046511104L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 42, 44);
        }

        if((active0 & 8796093022208L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 43, 44);
        }

        return this.jjMoveStringLiteralDfa7_0(active0, 4503608217305088L, active1, 0L, active2, 0L);
      case 'f':
        return this.jjMoveStringLiteralDfa7_0(active0, 562949953421312L, active1, 0L, active2, 0L);
      case 'l':
        return this.jjMoveStringLiteralDfa7_0(active0, 288230376151711744L, active1, 0L, active2, 0L);
      case 'n':
        if((active0 & 2048L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 11, 44);
        }
        break;
      case 'o':
        return this.jjMoveStringLiteralDfa7_0(active0, 2251799813685248L, active1, 0L, active2, 0L);
      case 's':
        if((active0 & 33554432L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 25, 44);
        }

        return this.jjMoveStringLiteralDfa7_0(active0, 0L, active1, 5765821383871299584L, active2, 4L);
      case 't':
        if((active0 & 1048576L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 20, 44);
        }

        return this.jjMoveStringLiteralDfa7_0(active0, 17592186044416L, active1, 0L, active2, 0L);
      case 'u':
        return this.jjMoveStringLiteralDfa7_0(active0, 524288L, active1, 0L, active2, 0L);
      case 'y':
        if((active0 & 268435456L) != 0L) {
          return this.jjStartNfaWithStates_0(6, 28, 44);
        }
      }

      return this.jjStartNfa_0(5, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa7_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(5, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(6, active0, active1, active2);
        return 7;
      }

      switch(this.curChar) {
      case 'c':
        return this.jjMoveStringLiteralDfa8_0(active0, 137438953472L, active1, 0L, active2, 0L);
      case 'd':
      case 'f':
      case 'g':
      case 'j':
      case 'k':
      case 'l':
      case 'm':
      case 'o':
      case 'q':
      case 'r':
      default:
        break;
      case 'e':
        if((active0 & 524288L) != 0L) {
          return this.jjStartNfaWithStates_0(7, 19, 44);
        }

        if((active0 & 288230376151711744L) != 0L) {
          return this.jjStartNfaWithStates_0(7, 58, 44);
        }

        return this.jjMoveStringLiteralDfa8_0(active0, 17626545782784L, active1, 87960930222080L, active2, 0L);
      case 'h':
        return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 1125899906842624L, active2, 4L);
      case 'i':
        return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 4611686018427387904L, active2, 0L);
      case 'n':
        return this.jjMoveStringLiteralDfa8_0(active0, 6755408030990336L, active1, 0L, active2, 0L);
      case 'p':
        if((active0 & 562949953421312L) != 0L) {
          return this.jjStartNfaWithStates_0(7, 49, 44);
        }
        break;
      case 's':
        return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 1157425104234217472L, active2, 16L);
      case 't':
        if((active0 & 1024L) != 0L) {
          return this.jjStartNfaWithStates_0(7, 10, 44);
        }
        break;
      case 'u':
        return this.jjMoveStringLiteralDfa8_0(active0, 0L, active1, 18014398509481984L, active2, 64L);
      }

      return this.jjStartNfa_0(6, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa8_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(6, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(7, active0, active1, active2);
        return 8;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 87960930222080L, active2, 0L);
      case '`':
      case 'a':
      case 'b':
      case 'c':
      case 'f':
      case 'j':
      case 'k':
      case 'l':
      case 'm':
      case 'p':
      case 'q':
      case 'r':
      case 's':
      default:
        break;
      case 'd':
        if((active0 & 17592186044416L) != 0L) {
          return this.jjStartNfaWithStates_0(8, 44, 44);
        }
        break;
      case 'e':
        if((active0 & 137438953472L) != 0L) {
          return this.jjStartNfaWithStates_0(8, 37, 44);
        }
        break;
      case 'g':
        return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 4611686018427387904L, active2, 0L);
      case 'h':
        return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 4503599627370496L, active2, 16L);
      case 'i':
        return this.jjMoveStringLiteralDfa9_0(active0, 2251799813685248L, active1, 1154047404513689600L, active2, 4L);
      case 'n':
        return this.jjMoveStringLiteralDfa9_0(active0, 0L, active1, 18014398509481984L, active2, 64L);
      case 'o':
        return this.jjMoveStringLiteralDfa9_0(active0, 34359738368L, active1, 0L, active2, 0L);
      case 't':
        if((active0 & 4503599627370496L) != 0L) {
          return this.jjStartNfaWithStates_0(8, 52, 44);
        }

        return this.jjMoveStringLiteralDfa9_0(active0, 8589934592L, active1, 0L, active2, 0L);
      }

      return this.jjStartNfa_0(7, active0, active1, active2);
    }
  }

  private int jjMoveStringLiteralDfa9_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(7, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(8, active0, active1, active2);
        return 9;
      }

      switch(this.curChar) {
      case 'a':
        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 17592186044416L, active2, 0L);
      case 'f':
        if((active0 & 34359738368L) != 0L) {
          return this.jjStartNfaWithStates_0(9, 35, 44);
        }

        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 1125899906842624L, active2, 4L);
      case 'g':
        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 1152921504606846976L, active2, 0L);
      case 'i':
        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 4503599627370496L, active2, 16L);
      case 'n':
        if((active1 & 4611686018427387904L) != 0L) {
          return this.jjStopAtPos(9, 126);
        }
      default:
        return this.jjStartNfa_0(8, active0, active1, active2);
      case 'o':
        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 70368744177664L, active2, 0L);
      case 's':
        if((active0 & 8589934592L) != 0L) {
          return this.jjStartNfaWithStates_0(9, 33, 44);
        }

        return this.jjMoveStringLiteralDfa10_0(active0, 0L, active1, 18014398509481984L, active2, 64L);
      case 'z':
        return this.jjMoveStringLiteralDfa10_0(active0, 2251799813685248L, active1, 0L, active2, 0L);
      }
    }
  }

  private int jjMoveStringLiteralDfa10_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(8, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(9, active0, active1, active2);
        return 10;
      }

      switch(this.curChar) {
      case 'e':
        return this.jjMoveStringLiteralDfa11_0(active0, 2251799813685248L, active1, 0L, active2, 0L);
      case 'f':
        return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 4503599627370496L, active2, 16L);
      case 'i':
        return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 18014398509481984L, active2, 64L);
      case 'n':
        if((active1 & 1152921504606846976L) != 0L) {
          return this.jjStopAtPos(10, 124);
        }

        return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 17592186044416L, active2, 0L);
      case 'r':
        if((active1 & 70368744177664L) != 0L) {
          return this.jjStopAtPos(10, 110);
        }
      default:
        return this.jjStartNfa_0(9, active0, active1, active2);
      case 't':
        if((active1 & 1125899906842624L) != 0L) {
          this.jjmatchedKind = 114;
          this.jjmatchedPos = 10;
        }

        return this.jjMoveStringLiteralDfa11_0(active0, 0L, active1, 0L, active2, 4L);
      }
    }
  }

  private int jjMoveStringLiteralDfa11_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if(((active0 &= old0) | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(9, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(10, active0, active1, active2);
        return 11;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa12_0(active0, 0L, active1, 0L, active2, 4L);
      case 'd':
        if((active0 & 2251799813685248L) != 0L) {
          return this.jjStartNfaWithStates_0(11, 51, 44);
        } else if((active1 & 17592186044416L) != 0L) {
          return this.jjStopAtPos(11, 108);
        }
      default:
        return this.jjStartNfa_0(10, active0, active1, active2);
      case 'g':
        return this.jjMoveStringLiteralDfa12_0(active0, 0L, active1, 18014398509481984L, active2, 64L);
      case 't':
        if((active1 & 4503599627370496L) != 0L) {
          this.jjmatchedKind = 116;
          this.jjmatchedPos = 11;
        }

        return this.jjMoveStringLiteralDfa12_0(active0, 0L, active1, 0L, active2, 16L);
      }
    }
  }

  private int jjMoveStringLiteralDfa12_0(long old0, long active0, long old1, long active1, long old2, long active2) {
    if((active0 & old0 | (active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(10, old0, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var14) {
        this.jjStopStringLiteralDfa_0(11, 0L, active1, active2);
        return 12;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 16L);
      case 'a':
        return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 4L);
      case 'n':
        return this.jjMoveStringLiteralDfa13_0(active1, 18014398509481984L, active2, 64L);
      default:
        return this.jjStartNfa_0(11, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa13_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(11, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(12, 0L, active1, active2);
        return 13;
      }

      switch(this.curChar) {
      case 'a':
        return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 16L);
      case 'e':
        return this.jjMoveStringLiteralDfa14_0(active1, 18014398509481984L, active2, 64L);
      case 's':
        return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 4L);
      default:
        return this.jjStartNfa_0(12, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa14_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(12, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(13, 0L, active1, active2);
        return 14;
      }

      switch(this.curChar) {
      case 'd':
        return this.jjMoveStringLiteralDfa15_0(active1, 18014398509481984L, active2, 64L);
      case 's':
        return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 20L);
      default:
        return this.jjStartNfa_0(13, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa15_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(13, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(14, 0L, active1, active2);
        return 15;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa16_0(active1, 18014398509481984L, active2, 64L);
      case 'i':
        return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 4L);
      case 's':
        return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 16L);
      default:
        return this.jjStartNfa_0(14, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa16_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(14, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(15, 0L, active1, active2);
        return 16;
      }

      switch(this.curChar) {
      case 'g':
        return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 4L);
      case 'i':
        return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 16L);
      case 's':
        return this.jjMoveStringLiteralDfa17_0(active1, 18014398509481984L, active2, 64L);
      default:
        return this.jjStartNfa_0(15, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa17_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(15, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(16, 0L, active1, active2);
        return 17;
      }

      switch(this.curChar) {
      case 'g':
        return this.jjMoveStringLiteralDfa18_0(active1, 0L, active2, 16L);
      case 'h':
        return this.jjMoveStringLiteralDfa18_0(active1, 18014398509481984L, active2, 64L);
      case 'n':
        if((active2 & 4L) != 0L) {
          return this.jjStopAtPos(17, 130);
        }
      default:
        return this.jjStartNfa_0(16, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa18_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(16, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(17, 0L, active1, active2);
        return 18;
      }

      switch(this.curChar) {
      case 'i':
        return this.jjMoveStringLiteralDfa19_0(active1, 18014398509481984L, active2, 64L);
      case 'n':
        if((active2 & 16L) != 0L) {
          return this.jjStopAtPos(18, 132);
        }
      default:
        return this.jjStartNfa_0(17, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa19_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(17, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(18, 0L, active1, active2);
        return 19;
      }

      switch(this.curChar) {
      case 'f':
        return this.jjMoveStringLiteralDfa20_0(active1, 18014398509481984L, active2, 64L);
      default:
        return this.jjStartNfa_0(18, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa20_0(long old1, long active1, long old2, long active2) {
    if(((active1 &= old1) | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(18, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(19, 0L, active1, active2);
        return 20;
      }

      switch(this.curChar) {
      case 't':
        if((active1 & 18014398509481984L) != 0L) {
          this.jjmatchedKind = 118;
          this.jjmatchedPos = 20;
        }

        return this.jjMoveStringLiteralDfa21_0(active1, 0L, active2, 64L);
      default:
        return this.jjStartNfa_0(19, 0L, active1, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa21_0(long old1, long active1, long old2, long active2) {
    if((active1 & old1 | (active2 &= old2)) == 0L) {
      return this.jjStartNfa_0(19, 0L, old1, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var10) {
        this.jjStopStringLiteralDfa_0(20, 0L, 0L, active2);
        return 21;
      }

      switch(this.curChar) {
      case '_':
        return this.jjMoveStringLiteralDfa22_0(active2, 64L);
      default:
        return this.jjStartNfa_0(20, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa22_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(20, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(21, 0L, 0L, active2);
        return 22;
      }

      switch(this.curChar) {
      case 'a':
        return this.jjMoveStringLiteralDfa23_0(active2, 64L);
      default:
        return this.jjStartNfa_0(21, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa23_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(21, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(22, 0L, 0L, active2);
        return 23;
      }

      switch(this.curChar) {
      case 's':
        return this.jjMoveStringLiteralDfa24_0(active2, 64L);
      default:
        return this.jjStartNfa_0(22, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa24_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(22, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(23, 0L, 0L, active2);
        return 24;
      }

      switch(this.curChar) {
      case 's':
        return this.jjMoveStringLiteralDfa25_0(active2, 64L);
      default:
        return this.jjStartNfa_0(23, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa25_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(23, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(24, 0L, 0L, active2);
        return 25;
      }

      switch(this.curChar) {
      case 'i':
        return this.jjMoveStringLiteralDfa26_0(active2, 64L);
      default:
        return this.jjStartNfa_0(24, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa26_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(24, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(25, 0L, 0L, active2);
        return 26;
      }

      switch(this.curChar) {
      case 'g':
        return this.jjMoveStringLiteralDfa27_0(active2, 64L);
      default:
        return this.jjStartNfa_0(25, 0L, 0L, active2);
      }
    }
  }

  private int jjMoveStringLiteralDfa27_0(long old2, long active2) {
    if((active2 &= old2) == 0L) {
      return this.jjStartNfa_0(25, 0L, 0L, old2);
    } else {
      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var6) {
        this.jjStopStringLiteralDfa_0(26, 0L, 0L, active2);
        return 27;
      }

      switch(this.curChar) {
      case 'n':
        if((active2 & 64L) != 0L) {
          return this.jjStopAtPos(27, 134);
        }
      default:
        return this.jjStartNfa_0(26, 0L, 0L, active2);
      }
    }
  }

  private int jjStartNfaWithStates_0(int pos, int kind, int state) {
    this.jjmatchedKind = kind;
    this.jjmatchedPos = pos;

    try {
      this.curChar = this.input_stream.readChar();
    } catch (IOException var5) {
      return pos + 1;
    }

    return this.jjMoveNfa_0(state, pos + 1);
  }

  private int jjMoveNfa_0(int startState, int curPos) {
    int startsAt = 0;
    this.jjnewStateCnt = 83;
    int i = 1;
    this.jjstateSet[0] = startState;
    int kind = Integer.MAX_VALUE;

    while(true) {
      if(++this.jjround == Integer.MAX_VALUE) {
        this.ReInitRounds();
      }

      long var14;
      if(this.curChar < 64) {
        var14 = 1L << this.curChar;

        do {
          --i;
          switch(this.jjstateSet[i]) {
          case 0:
            if((8589934591L & var14) != 0L) {
              if(kind > 6) {
                kind = 6;
              }

              this.jjCheckNAdd(0);
            }
            break;
          case 1:
            if(this.curChar == 33) {
              this.jjCheckNAddStates(21, 23);
            }
            break;
          case 2:
            if((-9217L & var14) != 0L) {
              this.jjCheckNAddStates(21, 23);
            }
            break;
          case 3:
            if((9216L & var14) != 0L && kind > 8) {
              kind = 8;
            }
            break;
          case 4:
            if(this.curChar == 10 && kind > 8) {
              kind = 8;
            }
            break;
          case 5:
            if(this.curChar == 13) {
              this.jjstateSet[this.jjnewStateCnt++] = 4;
            }
            break;
          case 6:
            if((8589934591L & var14) != 0L) {
              if(kind > 6) {
                kind = 6;
              }

              this.jjCheckNAdd(0);
            } else if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddStates(3, 9);
            } else if(this.curChar == 47) {
              this.jjAddStates(10, 12);
            } else if(this.curChar == 36) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            } else if(this.curChar == 34) {
              this.jjstateSet[this.jjnewStateCnt++] = 41;
            } else if(this.curChar == 39) {
              this.jjAddStates(13, 14);
            } else if(this.curChar == 46) {
              this.jjCheckNAdd(11);
            } else if(this.curChar == 35) {
              this.jjstateSet[this.jjnewStateCnt++] = 1;
            }

            if((287667426198290432L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(8, 9);
            } else if(this.curChar == 48) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddStates(15, 17);
            } else if(this.curChar == 34) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 7:
            if((287667426198290432L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(8, 9);
            }
            break;
          case 8:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(8, 9);
            }
          case 9:
          case 12:
          case 15:
          case 19:
          case 27:
          case 49:
          case 53:
          case 57:
          case 61:
          default:
            break;
          case 10:
            if(this.curChar == 46) {
              this.jjCheckNAdd(11);
            }
            break;
          case 11:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddStates(24, 26);
            }
            break;
          case 13:
            if((43980465111040L & var14) != 0L) {
              this.jjCheckNAdd(14);
            }
            break;
          case 14:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddTwoStates(14, 15);
            }
            break;
          case 16:
            if(this.curChar == 39) {
              this.jjAddStates(13, 14);
            }
            break;
          case 17:
            if((-549755823105L & var14) != 0L) {
              this.jjCheckNAdd(18);
            }
            break;
          case 18:
            if(this.curChar == 39 && kind > 66) {
              kind = 66;
            }
            break;
          case 20:
            if((566935683072L & var14) != 0L) {
              this.jjCheckNAdd(18);
            }
            break;
          case 21:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAddTwoStates(22, 18);
            }
            break;
          case 22:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAdd(18);
            }
            break;
          case 23:
            if((4222124650659840L & var14) != 0L) {
              this.jjstateSet[this.jjnewStateCnt++] = 24;
            }
            break;
          case 24:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAdd(22);
            }
            break;
          case 25:
            if(this.curChar == 34) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 26:
            if((-17179878401L & var14) != 0L) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 28:
            if((566935683072L & var14) != 0L) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 29:
            if(this.curChar == 34 && kind > 67) {
              kind = 67;
            }
            break;
          case 30:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAddStates(27, 30);
            }
            break;
          case 31:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 32:
            if((4222124650659840L & var14) != 0L) {
              this.jjstateSet[this.jjnewStateCnt++] = 33;
            }
            break;
          case 33:
            if((71776119061217280L & var14) != 0L) {
              this.jjCheckNAdd(31);
            }
            break;
          case 34:
          case 40:
            if(this.curChar == 34) {
              this.jjCheckNAddTwoStates(35, 38);
            }
            break;
          case 35:
            if((-17179869185L & var14) != 0L) {
              this.jjCheckNAddStates(31, 33);
            }
            break;
          case 36:
            if(this.curChar == 34 && kind > 68) {
              kind = 68;
            }
            break;
          case 37:
            if(this.curChar == 34) {
              this.jjstateSet[this.jjnewStateCnt++] = 36;
            }
            break;
          case 38:
            if(this.curChar == 34) {
              this.jjstateSet[this.jjnewStateCnt++] = 37;
            }
            break;
          case 39:
            if(this.curChar == 34) {
              this.jjCheckNAddStates(34, 36);
            }
            break;
          case 41:
            if(this.curChar == 34) {
              this.jjstateSet[this.jjnewStateCnt++] = 34;
            }
            break;
          case 42:
            if(this.curChar == 34) {
              this.jjstateSet[this.jjnewStateCnt++] = 41;
            }
            break;
          case 43:
            if(this.curChar == 36) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            }
            break;
          case 44:
            if((287948969894477824L & var14) != 0L) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            }
            break;
          case 45:
            if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddStates(3, 9);
            }
            break;
          case 46:
            if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddTwoStates(46, 47);
            }
            break;
          case 47:
            if(this.curChar == 46) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddStates(37, 39);
            }
            break;
          case 48:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddStates(37, 39);
            }
            break;
          case 50:
            if((43980465111040L & var14) != 0L) {
              this.jjCheckNAdd(51);
            }
            break;
          case 51:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddTwoStates(51, 15);
            }
            break;
          case 52:
            if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddTwoStates(52, 53);
            }
            break;
          case 54:
            if((43980465111040L & var14) != 0L) {
              this.jjCheckNAdd(55);
            }
            break;
          case 55:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 64) {
                kind = 64;
              }

              this.jjCheckNAddTwoStates(55, 15);
            }
            break;
          case 56:
            if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddStates(40, 42);
            }
            break;
          case 58:
            if((43980465111040L & var14) != 0L) {
              this.jjCheckNAdd(59);
            }
            break;
          case 59:
            if((287948901175001088L & var14) != 0L) {
              this.jjCheckNAddTwoStates(59, 15);
            }
            break;
          case 60:
            if(this.curChar == 48) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddStates(15, 17);
            }
            break;
          case 62:
            if((287948901175001088L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(62, 9);
            }
            break;
          case 63:
            if((71776119061217280L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(63, 9);
            }
            break;
          case 64:
            if(this.curChar == 47) {
              this.jjAddStates(10, 12);
            }
            break;
          case 65:
            if(this.curChar == 42) {
              this.jjstateSet[this.jjnewStateCnt++] = 76;
            } else if(this.curChar == 47) {
              if(kind > 7) {
                kind = 7;
              }

              this.jjCheckNAddStates(0, 2);
            }

            if(this.curChar == 42) {
              this.jjCheckNAdd(71);
            }
            break;
          case 66:
            if((-9217L & var14) != 0L) {
              if(kind > 7) {
                kind = 7;
              }

              this.jjCheckNAddStates(0, 2);
            }
            break;
          case 67:
            if((9216L & var14) != 0L && kind > 7) {
              kind = 7;
            }
            break;
          case 68:
            if(this.curChar == 10 && kind > 7) {
              kind = 7;
            }
            break;
          case 69:
            if(this.curChar == 13) {
              this.jjstateSet[this.jjnewStateCnt++] = 68;
            }
            break;
          case 70:
            if(this.curChar == 42) {
              this.jjCheckNAdd(71);
            }
            break;
          case 71:
            if((-4398046511105L & var14) != 0L) {
              this.jjCheckNAddTwoStates(71, 72);
            }
            break;
          case 72:
            if(this.curChar == 42) {
              this.jjCheckNAddStates(43, 45);
            }
            break;
          case 73:
            if((-145135534866433L & var14) != 0L) {
              this.jjCheckNAddTwoStates(74, 72);
            }
            break;
          case 74:
            if((-4398046511105L & var14) != 0L) {
              this.jjCheckNAddTwoStates(74, 72);
            }
            break;
          case 75:
            if(this.curChar == 47 && kind > 9) {
              kind = 9;
            }
            break;
          case 76:
            if(this.curChar == 42) {
              this.jjCheckNAddTwoStates(77, 78);
            }
            break;
          case 77:
            if((-4398046511105L & var14) != 0L) {
              this.jjCheckNAddTwoStates(77, 78);
            }
            break;
          case 78:
            if(this.curChar == 42) {
              this.jjCheckNAddStates(46, 48);
            }
            break;
          case 79:
            if((-145135534866433L & var14) != 0L) {
              this.jjCheckNAddTwoStates(80, 78);
            }
            break;
          case 80:
            if((-4398046511105L & var14) != 0L) {
              this.jjCheckNAddTwoStates(80, 78);
            }
            break;
          case 81:
            if(this.curChar == 47 && kind > 69) {
              kind = 69;
            }
            break;
          case 82:
            if(this.curChar == 42) {
              this.jjstateSet[this.jjnewStateCnt++] = 76;
            }
          }
        } while(i != startsAt);
      } else if(this.curChar < 128) {
        var14 = 1L << (this.curChar & 63);

        do {
          --i;
          switch(this.jjstateSet[i]) {
          case 2:
            this.jjAddStates(21, 23);
            break;
          case 6:
          case 44:
            if((576460745995190270L & var14) != 0L) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            }
            break;
          case 9:
            if((17592186048512L & var14) != 0L && kind > 60) {
              kind = 60;
            }
            break;
          case 12:
            if((137438953504L & var14) != 0L) {
              this.jjAddStates(49, 50);
            }
            break;
          case 15:
            if((343597383760L & var14) != 0L && kind > 64) {
              kind = 64;
            }
            break;
          case 17:
            if((-268435457L & var14) != 0L) {
              this.jjCheckNAdd(18);
            }
            break;
          case 19:
            if(this.curChar == 92) {
              this.jjAddStates(51, 53);
            }
            break;
          case 20:
            if((5700160604602368L & var14) != 0L) {
              this.jjCheckNAdd(18);
            }
            break;
          case 26:
            if((-268435457L & var14) != 0L) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 27:
            if(this.curChar == 92) {
              this.jjAddStates(54, 56);
            }
            break;
          case 28:
            if((5700160604602368L & var14) != 0L) {
              this.jjCheckNAddStates(18, 20);
            }
            break;
          case 35:
            this.jjAddStates(31, 33);
            break;
          case 49:
            if((137438953504L & var14) != 0L) {
              this.jjAddStates(57, 58);
            }
            break;
          case 53:
            if((137438953504L & var14) != 0L) {
              this.jjAddStates(59, 60);
            }
            break;
          case 57:
            if((137438953504L & var14) != 0L) {
              this.jjAddStates(61, 62);
            }
            break;
          case 61:
            if((72057594054705152L & var14) != 0L) {
              this.jjCheckNAdd(62);
            }
            break;
          case 62:
            if((541165879422L & var14) != 0L) {
              if(kind > 60) {
                kind = 60;
              }

              this.jjCheckNAddTwoStates(62, 9);
            }
            break;
          case 66:
            if(kind > 7) {
              kind = 7;
            }

            this.jjAddStates(0, 2);
            break;
          case 71:
            this.jjCheckNAddTwoStates(71, 72);
            break;
          case 73:
          case 74:
            this.jjCheckNAddTwoStates(74, 72);
            break;
          case 77:
            this.jjCheckNAddTwoStates(77, 78);
            break;
          case 79:
          case 80:
            this.jjCheckNAddTwoStates(80, 78);
          }
        } while(i != startsAt);
      } else {
        int e = this.curChar >> 8;
        int i1 = e >> 6;
        long l1 = 1L << (e & 63);
        int i2 = (this.curChar & 255) >> 6;
        long l2 = 1L << (this.curChar & 63);

        do {
          --i;
          switch(this.jjstateSet[i]) {
          case 0:
            if(jjCanMove_0(e, i1, i2, l1, l2)) {
              if(kind > 6) {
                kind = 6;
              }

              this.jjCheckNAdd(0);
            }
            break;
          case 2:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjAddStates(21, 23);
            }
            break;
          case 6:
            if(jjCanMove_0(e, i1, i2, l1, l2)) {
              if(kind > 6) {
                kind = 6;
              }

              this.jjCheckNAdd(0);
            }

            if(jjCanMove_2(e, i1, i2, l1, l2)) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            }
            break;
          case 17:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjstateSet[this.jjnewStateCnt++] = 18;
            }
            break;
          case 26:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjAddStates(18, 20);
            }
            break;
          case 35:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjAddStates(31, 33);
            }
            break;
          case 43:
          case 44:
            if(jjCanMove_2(e, i1, i2, l1, l2)) {
              if(kind > 70) {
                kind = 70;
              }

              this.jjCheckNAdd(44);
            }
            break;
          case 66:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              if(kind > 7) {
                kind = 7;
              }

              this.jjAddStates(0, 2);
            }
            break;
          case 71:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjCheckNAddTwoStates(71, 72);
            }
            break;
          case 73:
          case 74:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjCheckNAddTwoStates(74, 72);
            }
            break;
          case 77:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjCheckNAddTwoStates(77, 78);
            }
            break;
          case 79:
          case 80:
            if(jjCanMove_1(e, i1, i2, l1, l2)) {
              this.jjCheckNAddTwoStates(80, 78);
            }
          }
        } while(i != startsAt);
      }

      if(kind != Integer.MAX_VALUE) {
        this.jjmatchedKind = kind;
        this.jjmatchedPos = curPos;
        kind = Integer.MAX_VALUE;
      }

      ++curPos;
      if((i = this.jjnewStateCnt) == (startsAt = 83 - (this.jjnewStateCnt = startsAt))) {
        return curPos;
      }

      try {
        this.curChar = this.input_stream.readChar();
      } catch (IOException var13) {
        return curPos;
      }
    }
  }

  private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2) {
    switch(hiByte) {
    case 0:
      if((jjbitVec0[i2] & l2) != 0L) {
        return true;
      }

      return false;
    default:
      return false;
    }
  }

  private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2) {
    switch(hiByte) {
    case 0:
      if((jjbitVec0[i2] & l2) != 0L) {
        return true;
      }

      return false;
    default:
      return (jjbitVec1[i1] & l1) != 0L;
    }
  }

  private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2) {
    switch(hiByte) {
    case 0:
      if((jjbitVec4[i2] & l2) != 0L) {
        return true;
      }

      return false;
    case 48:
      if((jjbitVec5[i2] & l2) != 0L) {
        return true;
      }

      return false;
    case 49:
      if((jjbitVec6[i2] & l2) != 0L) {
        return true;
      }

      return false;
    case 51:
      if((jjbitVec7[i2] & l2) != 0L) {
        return true;
      }

      return false;
    case 61:
      if((jjbitVec8[i2] & l2) != 0L) {
        return true;
      }

      return false;
    default:
      return (jjbitVec3[i1] & l1) != 0L;
    }
  }

  public ParserTokenManager(JavaCharStream stream) {
    this.debugStream = System.out;
    this.jjrounds = new int[83];
    this.jjstateSet = new int[166];
    this.curLexState = 0;
    this.defaultLexState = 0;
    this.input_stream = stream;
  }

  public ParserTokenManager(JavaCharStream stream, int lexState) {
    this(stream);
    this.SwitchTo(lexState);
  }

  public void ReInit(JavaCharStream stream) {
    this.jjmatchedPos = this.jjnewStateCnt = 0;
    this.curLexState = this.defaultLexState;
    this.input_stream = stream;
    this.ReInitRounds();
  }

  private void ReInitRounds() {
    this.jjround = -2147483647;

    for(int i = 83; i-- > 0; this.jjrounds[i] = Integer.MIN_VALUE) {
      ;
    }

  }

  public void ReInit(JavaCharStream stream, int lexState) {
    this.ReInit(stream);
    this.SwitchTo(lexState);
  }

  public void SwitchTo(int lexState) {
    if(lexState < 1 && lexState >= 0) {
      this.curLexState = lexState;
    } else {
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", 2);
    }
  }

  protected Token jjFillToken() {
    String im = jjstrLiteralImages[this.jjmatchedKind];
    String curTokenImage = im == null?this.input_stream.GetImage():im;
    int beginLine = this.input_stream.getBeginLine();
    int beginColumn = this.input_stream.getBeginColumn();
    int endLine = this.input_stream.getEndLine();
    int endColumn = this.input_stream.getEndColumn();
    Token t = Token.newToken(this.jjmatchedKind, curTokenImage);
    t.beginLine = beginLine;
    t.endLine = endLine;
    t.beginColumn = beginColumn;
    t.endColumn = endColumn;
    return t;
  }

  public Token getNextToken() {
    Token specialToken = null;
    boolean curPos = false;

    while(true) {
      Token matchedToken;
      try {
        this.curChar = this.input_stream.BeginToken();
      } catch (IOException var9) {
        this.jjmatchedKind = 0;
        matchedToken = this.jjFillToken();
        matchedToken.specialToken = specialToken;
        return matchedToken;
      }

      this.jjmatchedKind = Integer.MAX_VALUE;
      this.jjmatchedPos = 0;
      int var11 = this.jjMoveStringLiteralDfa0_0();
      if(this.jjmatchedKind == Integer.MAX_VALUE) {
        int error_line = this.input_stream.getEndLine();
        int error_column = this.input_stream.getEndColumn();
        String error_after = null;
        boolean EOFSeen = false;

        try {
          this.input_stream.readChar();
          this.input_stream.backup(1);
        } catch (IOException var10) {
          EOFSeen = true;
          error_after = var11 <= 1?"":this.input_stream.GetImage();
          if(this.curChar != 10 && this.curChar != 13) {
            ++error_column;
          } else {
            ++error_line;
            error_column = 0;
          }
        }

        if(!EOFSeen) {
          this.input_stream.backup(1);
          error_after = var11 <= 1?"":this.input_stream.GetImage();
        }

        throw new TokenMgrError(EOFSeen, this.curLexState, error_line, error_column, error_after, this.curChar, 0);
      }

      if(this.jjmatchedPos + 1 < var11) {
        this.input_stream.backup(var11 - this.jjmatchedPos - 1);
      }

      if((jjtoToken[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
        matchedToken = this.jjFillToken();
        matchedToken.specialToken = specialToken;
        return matchedToken;
      }

      if((jjtoSpecial[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
        matchedToken = this.jjFillToken();
        if(specialToken == null) {
          specialToken = matchedToken;
        } else {
          matchedToken.specialToken = specialToken;
          specialToken = specialToken.next = matchedToken;
        }
      }
    }
  }

  private void jjCheckNAdd(int state) {
    if(this.jjrounds[state] != this.jjround) {
      this.jjstateSet[this.jjnewStateCnt++] = state;
      this.jjrounds[state] = this.jjround;
    }

  }

  private void jjAddStates(int start, int end) {
    do {
      this.jjstateSet[this.jjnewStateCnt++] = jjnextStates[start];
    } while(start++ != end);

  }

  private void jjCheckNAddTwoStates(int state1, int state2) {
    this.jjCheckNAdd(state1);
    this.jjCheckNAdd(state2);
  }

  private void jjCheckNAddStates(int start, int end) {
    do {
      this.jjCheckNAdd(jjnextStates[start]);
    } while(start++ != end);

  }
}
