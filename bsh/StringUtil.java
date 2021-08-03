package bsh;

import bsh.Reflect;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.d6r.DexVisitor;
import org.d6r.MethodVisitor;

public class StringUtil {
  public static String[] split(String s, String delim) {
    ArrayList v = new ArrayList();
    StringTokenizer st = new StringTokenizer(s, delim);

    while(st.hasMoreTokens()) {
      v.add(st.nextToken());
    }

    return (String[])v.toArray(new String[0]);
  }

  public static String maxCommonPrefix(String one, String two) {
    int i;
    for(i = 0; one.regionMatches(0, two, 0, i); ++i) {
      ;
    }

    return one.substring(0, i - 1);
  }

  public static String methodString(String name, Class<?>[] types) {
    return methodString(name, types, (String[])null);
  }

  public static String methodString(String name, Class<?>[] types, String[] paramNames) {
    return methodString(name, types, paramNames, (Class)null);
  }

  public static String methodString(String name, Class<?>[] types, String[] paramNames, Class<?> returnType) {
    StringBuilder sb = new StringBuilder(72);
    if(returnType != null) {
      sb.append("\u001b[1;36m");
      sb.append(MethodVisitor.STANDARD_PACKAGES.reset(DexVisitor.typeToName(returnType.getName())).replaceAll("$1$2").replaceAll("\\[\\]", "\u001b[1;37m[]\u001b[0m"));
      sb.append("\u001b[0m ");
    }

    sb.append("\u001b[1;33m");
    sb.append(name);
    sb.append("\u001b[0m");
    sb.append('(');
    int i = -1;
    int len = types.length;

    while(true) {
      ++i;
      if(i >= len) {
        sb.append(')');
        return sb.toString();
      }

      Class cls = types[i];
      if(i > 0) {
        sb.append(", ");
      }

      if(cls != null) {
        sb.append("\u001b[1;32m");
        sb.append(MethodVisitor.STANDARD_PACKAGES.reset(DexVisitor.typeToName(cls.getName())).replaceAll("$1$2").replaceAll("\\[\\]", "\u001b[1;37m[]\u001b[0m"));
        sb.append("\u001b[0m");
        if(paramNames != null) {
          sb.append(' ');
          sb.append(paramNames[i]);
        }
      } else {
        sb.append("?");
      }
    }
  }

  public static String normalizeClassName(Class type) {
    return Reflect.normalizeClassName(type);
  }
}
