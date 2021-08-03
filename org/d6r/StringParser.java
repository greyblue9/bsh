package org.d6r;

import com.google.common.base.CharMatcher;

import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang3.StringUtils;
// import org.apache.commons.collections.ExtendedProperties;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.Serializable;
import java.math.BigInteger;
import java.math.BigDecimal;

public class StringParser { 

  public static final Matcher INTEGER_PTRN 
    = Pattern.compile("^-?[0-9]+$").matcher("");
  public static final Matcher DOUBLE_PTRN 
    = Pattern.compile("^-?[0-9]+\\.[0-9]+$").matcher("");
  
  
  public static <T> String formatValue(T v) {
    if (v instanceof Object[]) v = (T)Arrays.asList((Object[])v);
    if (v instanceof Iterable<?>) {
      StringBuilder sb = new StringBuilder(76);
      sb.append('[');
      int idx = -1;
      for (Object item: (Iterable<Object>) v) {
        idx += 1;
        if (idx != 0) sb.append(",");
        sb.append(formatValue((T) item));
      }
      sb.append(']');
      return sb.toString();
    }
    if (v == null) return "null";
    return String.format("%s", v);
  }
  
  public static <T extends Serializable> T parse(String input) {
    String inputEscaped 
      = ("[".concat(input).concat("]"))
          .replace("[", "[\\<\\[")
          .replace("]", "]\\>\\]")
          .replace(", ", ",")
          .replace("], ", "],")
          .replaceAll("\\s*,\\s*", ",");   
    String[] elems 
      = StringUtils.split(inputEscaped, "[,]");
    int i = -1, len = elems.length; 
    while (++i < len) { 
      if (elems[i].equals("\\<\\")) elems[i] = "[";
      else if (elems[i].equals("\\>\\")) elems[i] = "]";
    }
    
    Deque<Object> lists = new ArrayDeque<Object>();   
    lists.offerLast(new ArrayDeque<Object>());
    
    Deque<String> toks = new ArrayDeque<String>();
    Collections.addAll(toks, elems);
    
    Deque<Object> output = new ArrayDeque<Object>();
    while (! toks.isEmpty()) {     
      String tok = toks.pollFirst();
      if (tok.equals("[")) { 
        lists.offerLast(new ArrayDeque<Object>());
        continue;
      }
      if (tok.equals("]")) { 
        Object cur = lists.pollLast(); 
        ((Collection<Object>)(Object) lists.peekLast()).add(cur);
        continue; 
      }
      if (tok.equals(",")) continue;
      Object objTok = tok;

      if (INTEGER_PTRN.reset(tok).find()) {
        try {
          objTok = Integer.valueOf(tok, 10);
        } catch (NumberFormatException infe) {
          try {
            objTok = Long.valueOf(tok, 10);
          } catch (NumberFormatException lnfe) {
            objTok = new BigInteger(tok);
          }
        }
      } else if (DOUBLE_PTRN.reset(tok).find()) {
        try {
          objTok = Double.valueOf(tok);
        } catch (NumberFormatException bnnfe) {
          objTok = new BigDecimal(tok);
        }
      } else if ("true".equals(tok)) {
        objTok = Boolean.TRUE;
      } else if ("false".equals(tok)) {
        objTok = Boolean.FALSE;
      }
      if (lists.peekLast() != null) {
        ((Collection<Object>)(Object)lists.peekLast()).add(objTok);
      } else { 
        output.add(tok);
      }
    }
    output.addAll(lists); 
    Object result = (
      (Iterable<Iterable<Iterable<Object>>>)(Object) output
    ).iterator().next().iterator().next(); 
    if (input.indexOf('[') != -1 && input.charAt(0) == '[') {
      result = ((Iterable<Object>) result).iterator().next();
    }
    if (input.indexOf('[') == -1 && result instanceof Collection 
    && ((Collection)result).size() == 1) {
      result = ((Iterable<Object>) result).iterator().next();
    }
    return (T) (Object) result;
  }
}
