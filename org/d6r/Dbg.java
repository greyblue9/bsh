package org.d6r;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.d6r.ClassInfo.typeToName;
import static java.lang.System.identityHashCode;
import static org.d6r.ClassInfo.getMessage;
import org.apache.commons.lang3.*;
import org.d6r.*;
import java.io.*;
import java.util.*;
import static org.d6r.ClassInfo.*;
import static org.d6r.ClassInfo.getMessage;

public class Dbg {
  
  public static String defensiveToString(Object obj) {
    if (obj == null) return "null";
    if (obj instanceof String) return String.format("\"%s\"", obj);
    String repr = null;
    try {
      repr = obj.toString();
    } catch (Throwable e) {
      Throwable c;
      try { c = getRootCause(e); } catch (Throwable ign) { c = e; }
      repr = String.format(
        "<%s@0x%08x #toString() threw %s%s%s>",
        typeToName(obj), identityHashCode(obj),
        typeToName(c), getMessage(c)
      );
    }
    return repr != null? repr: "???";
  }
  
  
  public static class AppenderIoException
            extends RuntimeException
  { 
    public final Exception thrown;
    public final Appendable thrower;
    public final String methodName;
    public final List<Object> args;
    
    public AppenderIoException(Exception ioe, Appendable thrower,
    String methodName, Object... arguments) {
      super(genMessage(ioe, thrower, methodName, arguments), ioe);
      this.thrown = ioe;
      this.thrower = thrower;
      this.methodName = methodName != null? methodName: "append";
      this.args = Arrays.asList(arguments);
    }
    
    public static final String genMessage(Exception ioe,
    Object thrower, String methodName, Object[] arguments)
    { 
      return String.format(
        "Appendable object (%s %s) threw %s from %s(%s); cause: %s",
        typeToName(thrower), defensiveToString(thrower),          
        typeToName(ioe), methodName, 
        StringUtils.substringBeforeLast(
          defensiveToString(Arrays.asList(arguments)), "]"
        ).substring(1),
        ClassInfo.getMessage(ioe),
        ClassInfo.getMessage(getRootCause(ioe))
      );
    }
  }
  
  
  public static class StrAppender
           implements Appendable
  {
    private Object _strAccum;
    
    public static CharSequence fmt(Object fmt) {
      return fmt(fmt, new Object[0]);
    }
    
    public static CharSequence fmt(Object _fmt, Object... args) {
      String fmt = (String) ((_fmt instanceof String)
        ? (String) (_fmt)
        : (String) (_fmt != null? _fmt.toString(): "null"));
      
      int start = 0, len = fmt.length(),
        end = 0, pos = -1, lastpos = -2;
      int idx1, idx2, idx, pctCount = 0;
      char type, last = fmt.charAt(len - 1);
      StringBuilder spec = new StringBuilder(5).append('%'),
                    text = new StringBuilder();
      List<Object> tokens = new ArrayList<Object>();
      do {
        idx1 = fmt.indexOf('%', pos);
        idx2 = fmt.indexOf('{', pos);
        idx = Math.min((idx1 != -1 ? idx1 : Integer.MAX_VALUE),
                       (idx2 != -1 ? idx2 : Integer.MAX_VALUE));
        if (idx == Integer.MAX_VALUE) {
          idx = -1;
          pos = -1;
          break;
        }
        pos = idx;
        if (pos == lastpos) {
          WARN("bailing out: pos = %d, char = '%c'\n",
            pos, fmt.charAt(pos));
          break;
        }
        
        lastpos = pos;
        if (pos > end) {
          text.append(fmt.subSequence(end, pos));
        }
        spec.delete(0, spec.length()).append(type = fmt.charAt(pos));
        char ch;
        if (type == '%') {
          while (((ch = fmt.charAt(++pos)) >= '0' && ch <= '9')
                || ch == '-' || ch == '.')
          {
            spec.append(ch);
          }
        } else {
          while ((ch = fmt.charAt(++pos)) != '}') spec.append(ch);
        }
        spec.append(ch);
        end = pos + 1;
        tokens.add("<" + spec + ">");
        if (args.length >= tokens.size()) {
          Object arg = args[tokens.size() - 1];
          if (spec.charAt(0) == '{') {
            if (spec.length() == 3 && String.valueOf(spec).equals("{c}"))
            {
              text.append((arg != null)
                ? ClassInfo.typeToName(arg.getClass().getName()): "null"
              );
              continue;
            }
          } else if (spec.charAt(0) == '%' && spec.length() > 2) {
            try {
              text.append(
                String.format(spec.toString(), args[tokens.size() - 1])
              );
              continue;
            } catch (Throwable e) {
              Throwable ex;
              try { ex = Reflector.getRootCause(e); } 
              catch (Throwable ignore) { ex = e; }            
              WARN("Format string caused %s: \n"
                + "    \"%s\" \n" 
                + "  at argument %d, with format specifier '%s': \n" 
                + "  %s\n\n",
                ex.getClass().getSimpleName(),
                fmt.toString().replace("\n", "\\n"),
                tokens.size(),
                spec, 
                ex.getMessage() != null? ex.getMessage(): ex.toString()
              );
            }
          }
          text.append(args[tokens.size() - 1]);
        } else {
          text.append(spec);
        }
      } while (true);
      
      if (end < fmt.length()) {
        text.append(fmt.subSequence(end, fmt.length()));
      }
      return text;
    }
    
    
    public StrAppender(StringBuilder sb) {
      _strAccum = sb;
    }
    @Override
    public StrAppender append(char p0) {
      if (_strAccum instanceof StringBuilder) {
        ((StringBuilder) _strAccum).append(p0);
      }
      return this;
    }    
    @Override
    public StrAppender append(CharSequence p0) {
      if (_strAccum instanceof StringBuilder) {
        ((StringBuilder) _strAccum).append(p0);
      }
      return this;
    }
    @Override
    public StrAppender append(CharSequence csq, int start, int end) {
      if (_strAccum instanceof StringBuilder) {
        ((StringBuilder) _strAccum).append(csq, start, end);
      }
      return this;
    }
    
    @Override
    public String toString() {
      return _strAccum != null
        ? _strAccum.toString()
        : String.format(
          "<uninitialized %s>", getClass().getSimpleName());
    }
  }

}
  
  