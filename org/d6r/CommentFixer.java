package org.d6r;


import com.google.common.base.Function;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javafile.api.Comment;
import javafile.api.visitor.CommentFormatter;
import javafile.api.visitor.CommentFormatter.CommentLocation;



public class CommentFixer extends CommentFormatter {
  
  public static boolean VERBOSE = false;
  
  public final int indentWidth;
  public final String maxIndent;
  // public final List<CommentCall> calls = new ArrayList<>();
  public final List<Comment> comments = new ArrayList<>();
  
  
  public CommentFixer(final int indentWidth) {
    super();
    this.indentWidth = indentWidth;
    this.maxIndent = StringUtils.repeat(' ', (int) ((50.0 / indentWidth)*indentWidth));
  }
  
  public CommentFixer() {
    this(2);
  }
  
  private String getIndent(int level) {
    return (String)
      maxIndent.subSequence(0, Math.min(level * indentWidth, maxIndent.length()));
  }
  
  private @NonDumpable final StringBuilder buf  = new StringBuilder();
  private @NonDumpable final StringBuilder buf2 = new StringBuilder();
  
  private static StringBuilder removeCarriageReturns(final StringBuilder sb) {
    for (int idx = sb.indexOf("\r"); idx != -1; sb.delete(idx, idx+1)); // remove CR's
    return sb;
  }
  
  

  
  public String format(final String text, final int indentLevel,
    final CommentLocation commentLocation, final Comment apiComment)
  {
    if (apiComment != null) comments.add(apiComment);
    buf.setLength(0);
    buf.append(apiComment != null? apiComment.getContent(): text);
    if (VERBOSE) System.err.printf("text = \"%s\"\n", buf);
    
    final StringBuilder sb = removeCarriageReturns(buf);

    /*
    CommentCall call = new CommentCall(text, indentLevel, commentLocation, apiComment);
    */
    
    final String indent = getIndent(indentLevel);
    if (sb.toString().trim().indexOf("\n") == -1) {
      return new StringBuilder(sb.toString().replaceAll("///*", "").trim())
               .insert(0, "// ")
               .insert(0, indent)
               .append("\n")
               .toString();
    }
    
    final boolean mut = true;
    for (int pos = 0,
         mlCommentBeginSeqPos = sb.indexOf("/*", pos), 
         mlCommentEndSeqPos = sb.indexOf("*/", mlCommentBeginSeqPos + 2);
         
         mlCommentBeginSeqPos != -1 && mlCommentEndSeqPos != -1;
         
         pos = (mlCommentEndSeqPos = sb.indexOf(
           "*/", (mlCommentBeginSeqPos = sb.indexOf("/*", (mut ? pos : pos+2))) + 2
         )))
    {

      sb.delete(mlCommentEndSeqPos, mlCommentEndSeqPos + 2)
        .delete(mlCommentBeginSeqPos, mlCommentBeginSeqPos + 2);
      pos -= 4;
      while (sb.length() > mlCommentBeginSeqPos &&
             "*".equals(sb.subSequence(mlCommentBeginSeqPos, mlCommentBeginSeqPos+1)))
      {
     
        sb.delete(mlCommentBeginSeqPos, mlCommentBeginSeqPos + 1);
        --mlCommentEndSeqPos;
      }
      buf2.setLength(0);
      final StringBuilder csb = buf2
        .append(sb.subSequence(mlCommentBeginSeqPos, mlCommentEndSeqPos-2));
      for (int lineStart = 0, lineEnd = csb.indexOf("\n", lineStart);
          csb.length() > lineStart && lineStart != -1 && csb.length() >= lineEnd;
          lineEnd = (lineEnd = csb.indexOf("\n", (lineStart = lineEnd + 1))) != -1
            ? lineEnd : csb.length())
      {
        if (VERBOSE)
          System.err.printf("line: [%s]\n", csb.subSequence(lineStart, lineEnd));
        int starPos;
        while ((starPos = csb.indexOf("*")) != -1) csb.delete(starPos, starPos + 1);
      }
      sb.replace(mlCommentBeginSeqPos, mlCommentEndSeqPos, csb.toString());
    }
    
    /*
    for (int lineStart = 0, lineEnd = sb.indexOf("\n", lineStart);
             sb.length() > lineStart && lineStart != -1 && sb.length() >= lineEnd;
             lineEnd = sb.indexOf("\n", (lineStart = lineEnd+1)))
    {
      System.err.printf("line: [%s]\n", sb.subSequence(lineStart, lineEnd));
    }
    */
    
    final CharSequence[] lines = sb.toString().split("\n");
    final int lineCount = lines.length;
    final boolean multiline = (lineCount > 1);
    
    int minLeadingSpaceChars = Integer.MAX_VALUE;
    for (final CharSequence line: lines) {
      final int len = line.length();
      int pos = 0;
      while (pos < len && Character.isWhitespace(line.charAt(pos++))) ;
      if (pos == len) continue;
      final int numLeadingSpaceChars = pos;
      minLeadingSpaceChars = Math.min(numLeadingSpaceChars, minLeadingSpaceChars);
    }
    
    buf2.setLength(0);
    final StringBuilder lb = buf2.append(indent);
    final int indentLength = indent.length();
    
    for (int i=0; i<lineCount; ++i) {
      final String line = (String) lines[i];
      final int lineLength = line.length();
      if (lineLength < minLeadingSpaceChars) {
        lines[i] = indent;
        continue;
      }
      lb.setLength(indentLength);
      lines[i] = lb.append(
          ((String) lines[i]).subSequence(minLeadingSpaceChars, lines[i].length())
        )
        .toString();
    }
    
    final String ret = new StringBuilder(indent)
      .append("/**\n")
      .append(
        StringUtils.join(lines, "\n")
      ).append("\n").append(indent).append("*/").append("\n").toString();
    //call.setReturnValue(ret);
    //calls.add(call);
    return ret;
  }
  
  @Override
  public String format(final String text, final int indentLevel,
    final CommentLocation commentLocation)
  {
    return format(text, indentLevel, commentLocation, null);
  }
  
  @Override
  public String format(Comment apiComment, int indentLevel,
  CommentLocation commentLocation) 
  {
    return format(apiComment.getContent(), indentLevel, commentLocation, apiComment);
    /*
    CommentCall call 
      = new CommentCall(apiComment, indentLevel, commentLocation);
    String ret 
      = super.format(apiComment, indentLevel, commentLocation);
    call.setReturnValue(ret);
    calls.add(call);
    return ret;
    */
  }
  
  @Override
  public String formatStringAsJavadoc(String input) {
    return format(input, 0, null, null);
    /*
    CommentCall call = new CommentCall(input);
    String ret = super.formatStringAsJavadoc(input);
    call.setReturnValue(ret);
    calls.add(call);
    return ret;
    */
  }
  
  @Override
  public String toString() {
    return String.format(
      "%s { indentWidth = %d }",
      getClass().getSimpleName(), indentWidth
    ); 
  }
  
  /*
  public static class CommentCall {
    
    String comment;
    Comment apiComment;
    String input;
    int indentLevel;
    CommentLocation commentLocation;
    Method method;
    Function<CommentCall, String> toString;
    Object returnValue;
    
    public CommentCall(String comment, int indentLevel,
      CommentLocation commentLocation, Comment apiComment) 
    {
      try {
        (this.method = CommentFixer.class.getDeclaredMethod(
          "format", String.class, Integer.TYPE, CommentLocation.class
        )).setAccessible(true);
      } catch (ReflectiveOperationException e) { 
        throw Reflector.Util.sneakyThrow(e);
      }
      this.apiComment = apiComment;
      this.comment = comment;
      this.indentLevel = indentLevel;
      this.commentLocation = commentLocation;
      this.input = comment;
      
      this.toString = new Function<CommentCall, String>() {
        @Override
        public String apply(CommentCall cc) {
          return String.format(
            "format(String comment = [\n%s\n], "
            +"int indentLevel = %d, "
            +"CommentLocation commentLocation = %s) --> [\n%s\n]",
            cc.comment, cc.indentLevel, 
            Debug.ToString(cc.commentLocation),
            (String) cc.returnValue
          );
        }
      };
    }
    
    public CommentCall(Comment apiComment, int indentLevel,
      CommentLocation commentLocation) 
    {
      try {
        (this.method = CommentFixer.class.getDeclaredMethod(
          "format", Comment.class, Integer.TYPE, CommentLocation.class
        )).setAccessible(true);
      } catch (ReflectiveOperationException e) { 
        throw Reflector.Util.sneakyThrow(e);
      }
      this.comment = apiComment.getContent();
      this.apiComment = apiComment;
      this.indentLevel = indentLevel;
      this.commentLocation = commentLocation;
      this.input = comment;
      
      this.toString = new Function<CommentCall, String>() {
        @Override
        public String apply(final CommentCall cc) {
          return String.format(
            "format(Comment apiComment = \n%s\n, "
            +"int indentLevel = %d, "
            +"CommentLocation commentLocation = %s) --> [\n%s\n]",
            Debug.ToString(cc.apiComment), cc.indentLevel, 
            Debug.ToString(cc.commentLocation),
            (String) cc.returnValue
          );
        }
      };
    }
    
    public CommentCall(String input) {
      try {
        (this.method = CommentFixer.class.getDeclaredMethod(
          "formatStringAsJavadoc", String.class
        )).setAccessible(true);
      } catch (ReflectiveOperationException e) { 
        throw Reflector.Util.sneakyThrow(e);
      }
      this.comment = input;
      this.input = input;
      
      this.toString = new Function<CommentCall, String>() {
        @Override
        public String apply(CommentCall cc) {
          return String.format(
            "formatStringAsJavadoc(String input = [\n%s\n])"
            +" --> [\n%s\n]",
            cc.input, 
            (String) cc.returnValue
          );
        }
      };
    }
    
    public void setReturnValue(Object oReturn) {
      this.returnValue = oReturn;
      if (VERBOSE) System.err.println(this.toString());
    }
    
    @Override
    public String toString() {
      return toString.apply(this);
    }
  }
  */
}



