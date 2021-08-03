package org.d6r;

import bsh.Interpreter;
import bsh.CallStack;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.d6r.CharSequenceUtil.indexOf;


public class Format {
  public static final String TAG = "Format";
  static BufferedWriter bw;
  
  public static StringBuilder str(final CharSequence format, final Object... args) {
    final int len = format.length();
    int pos = -1;
    final StringBuilder spec = new StringBuilder(5);
    spec.append('%');
    char type;
    final StringBuilder text = new StringBuilder();
    int end = 0;
    final List<Object> tokens = new ArrayList<Object>();
    int lastpos = -2;
    int idx1, idx2, idx;
    do {
      idx1 = indexOf(format, '%', pos);
      idx2 = indexOf(format, '{', pos);
      idx = Math.min(
        (idx1 != -1 ? idx1 : Integer.MAX_VALUE),
        (idx2 != -1 ? idx2 : Integer.MAX_VALUE)
      );
      if (idx == Integer.MAX_VALUE) {
        idx = -1;
        pos = -1;
        break;
      }
      pos = idx;
      if (pos == lastpos) {
        Log.e(TAG, "bailing out: pos = %d, char = '%c'\n", pos, format.charAt(pos));
        break;
      }
      lastpos = pos;
      if (pos > end) {
        text.append(format.subSequence(end, pos));
      }
      spec.delete(0, spec.length());
      type = format.charAt(pos);
      spec.append(type);
      char ch;
      if (type == '%') {
        while (((ch = format.charAt(++pos)) >= '0' && ch <= '9') ||
                 ch == '-' || ch == '.')
        {
          spec.append(ch);
        }
      } else {
        while ((ch = format.charAt(++pos)) != '}') {
          spec.append(ch);
        }
      }
      spec.append(ch);
      end = pos + 1;
      tokens.add("<" + spec + ">");
      if (args.length >= tokens.size()) {
        Object arg = args[tokens.size() - 1];
        if (spec.charAt(0) == '{') {
          if (spec.toString().equals("{c}")) {
            text.append(
              (arg != null)? ClassInfo.typeToName(arg.getClass().getName()): "null"
            );
            continue;
          }
        }
        if (spec.charAt(0) == '%' && spec.length() > 2) {
          try {
            text.append(String.format(spec.toString(), args[tokens.size() - 1]));
            continue;
          } catch (Throwable ex) {
            new RuntimeException(String.format(
              "Format string caused %s:\n  " + 
              "  \"%s\"\n  " + 
              "  at argument %d, with format specifier '%s'",
              ex, format.toString().replace("\n", "\\n"), tokens.size(), spec
            ), ex).printStackTrace();
          }
        }
        text.append(args[tokens.size() - 1]);
        continue;
      } else {
        text.append(spec);
      }
    } while (true);
    if (end < format.length()) {
      text.append(format.subSequence(end, format.length()));
    }
    return text;
  }
  
  
  public static void printf(final CharSequence format, final Object... args) {
    if (format == null) return;
    final int length = format.length();
    if (length == 0) return;
    
    try {
      if (bw == null) bw = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(FileDescriptor.out),
        StandardCharsets.UTF_8.newEncoder()
      ));
      
      bw.append(str(format, args));
      if (format.charAt(length-1) == '\n') bw.flush();
    } catch (IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  
  public static StringBuilder invoke(final Interpreter in, final CallStack cs,
  final CharSequence format, final Object... args)
  {
    return str(format, args);
  }
}


