package org.d6r;

import com.google.common.base.Function;
import java.util.Arrays;
import java.util.ArrayList;

public class MinXml {
  
  public static class Node {  
    String nodeName;
    // String or ArrayList
    Object nodeValue;
    
    public Node(String name, Object value) {
      this.nodeName = name;
      this.nodeValue = value;
    }
  }
  
  public static Function IDENTITY_PARSER = new Function() { 
    public Object apply(Object x) { 
      return x;
    } 
    public boolean equals(Object other) { 
      return other instanceof Function && this == other;
    }
  };
  

  String source;
  int cursor = 0;
  ArrayList lastNodeList;
  Function textnodeParser;
  Object documents;
  
  public MinXml(String source) {
    this(source, (Function) null);
  }
  
  public MinXml(String source, Function textnodeParser) {
    this.textnodeParser = (textnodeParser == null)
      ?  IDENTITY_PARSER: textnodeParser;
  
    this.source = source;
    this.cursor = 0;
    this.documents = this.subTree();
  }
  
  
  public Object subTree() {
    ArrayList nodeList = (lastNodeList = new ArrayList());
    int i = 0; 
    while (this.cursor <= this.source.length()) {
      int start = this.cursor;
      int tagStart = this.source.indexOf('<', this.cursor);
      if (tagStart == -1) return nodeList;
      int tagEnd = this.source.indexOf('>', tagStart);
      this.cursor = tagEnd+1;
      String tag = this.source.substring(
        tagStart + 1, tagEnd
      );
      if (tag.charAt(0) == '/') { // found end tag
        if (i == 0) { // no child-elements, there was chardata
          return this.textnodeParser.apply(
            this.source.substring(start, tagStart)
          );
        } else { // child elemets
          return nodeList;
        }
      } else if (tag.charAt(tag.length() - 1) == '/') {
        // found empty tag
        //nodeList[i++] 
        nodeList.add(
          new Node(tag.substring(0, tag.length()-1), "")
        );
        i += 1;
      } else { // found start tag, parse childnodes
        nodeList.add(new Node(tag, this.subTree()));
        i += 1;
      }
    }
    return nodeList;
  }
  
  
  public static String parseCharRefs(String input) {
    int i = -1;
    String s = "" + input;
    while ((i = s.indexOf('&', i+1)) !=-1) {
      int i2 = s.indexOf(';', i);
      String charCodeStr = s.substring(i+2, i2);
      System.err.printf("char code: [%s]\n", charCodeStr);
      char charVal = '?';
      if (charCodeStr.length() >= 2) {
        if (charCodeStr.charAt(0) == 'x') {
          charVal = (char) 
            Integer.valueOf(charCodeStr.substring(1), 16)
              .intValue();
        } else {
          charVal = (char)
            Integer.valueOf(charCodeStr, 10)
              .intValue();
        }
      }
      s = String.format(
        "%s%c%s", s.substring(0, i), charVal, s.substring(i2 + 1)
      );
    }
    return s;
  }
  
  
  public static String foldWhiteSpace(String input) {
    StringBuilder s = new StringBuilder(parseCharRefs(input));
    char[] ws = { '\t', '\n', '\r', ' ' };
    boolean isSpace = true;    
    s = s.append(' ');
    StringBuilder t = new StringBuilder(8);
    for (int i=0; i<s.length(); i++) {
      // if ws.indexOf(s.charAt(i)) != -1) { .. }
      if (Arrays.binarySearch(ws, 0, 4, s.charAt(i)) >= 0) {
        if (! isSpace) {
          t.append(' ');
          isSpace = true;         
        }
      } else {
        t.append(s.charAt(i));
        isSpace = false;      
      }
    }
    return t.substring(0, t.length() - 1);
  }
  
  
  public static String prettyprint(ArrayList nl, String indent) {
    String oldIndent = indent;
    String newIndent = indent.concat("  ");
    StringBuilder s = new StringBuilder(64); 
    int len = nl.size();
    int i = -1;
    Node cn;
    while (++i < len && ((cn = ((Node)nl.get(i))) != null)) {
      if (oldIndent.length() > 0) s.append('\n');
      s.append(oldIndent)
       .append('<').append(cn.nodeName).append('>');
      
      if (cn.nodeValue instanceof String) {
        s.append((String) cn.nodeValue); 
      } else {
        s.append(
          prettyprint((ArrayList) cn.nodeValue, newIndent)
        ).append('\n').append(oldIndent);
      }
      s.append("</").append(cn.nodeName).append('>');
    }
    return s.toString();
  }
  
}


  