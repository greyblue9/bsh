package bsh;

import bsh.KindNode;
import bsh.ParserConstants;
import java.io.Serializable;

public class Token implements KindNode, Serializable {
  private static final long serialVersionUID = 1L;
  public int beginLine;
  public int beginColumn;
  public int endLine;
  public int endColumn;
  public String image;
  public Token next;
  public Token specialToken;
  int kind;

  public int getKind() {
    return this.kind;
  }

  public void setKind(int kind) {
    this.kind = kind;
  }

  public Object getValue() {
    return null;
  }

  public Token() {
    this(0);
  }

  public Token(int kind) {
    this(kind, (String)null);
  }

  public Token(int kind, String image) {
    this.kind = kind;
    this.image = image;
  }

  public String toString() {
    return toString(this.kind, this.image);
  }

  public static String getKindString(int kind) {
    Integer kindVal = Integer.valueOf(kind);
    String kindStr = (String)ParserConstants.kindMap.get(kindVal);
    if(kindStr == null) {
      ParserConstants.kindMap.put(kindVal, kindStr = String.format("kind(%d)", new Object[]{Integer.valueOf(kind)}));
    }

    return kindStr;
  }

  public static String getKindString(Object obj) {
    return obj instanceof Integer?getKindString(((Integer)obj).intValue()):(obj instanceof Token?getKindString(((Token)obj).getKind()):(obj instanceof KindNode?getKindString(((KindNode)obj).getKind()):"kind(???)"));
  }

  public static String toString(int kind, String image) {
    if(kind != 0) {
      StringBuilder sb = new StringBuilder(16);
      sb.append(getKindString(kind));
      if(image != null && image.length() > 0) {
        sb.append(" ");
        sb.append(image);
      }

      return sb.toString();
    } else {
      return image;
    }
  }

  public static String toString(String image, int kind) {
    if(kind != 0) {
      StringBuilder sb = new StringBuilder(16);
      if(image != null && image.length() > 0) {
        sb.append(image);
        sb.append(" ");
      }

      sb.append(getKindString(kind));
      return sb.toString();
    } else {
      return image;
    }
  }

  public static Token newToken(int ofKind, String image) {
    return new Token(ofKind, image);
  }

  public static Token newToken(int ofKind) {
    return newToken(ofKind, (String)null);
  }
}
