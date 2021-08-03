package org.d6r;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class CharSequenceUtil {
  
  public static int indexOf(final CharSequence s, final int searchChar,
  final int startIndex)
  {
    if (s instanceof String) {
      return ((String) s).indexOf(searchChar, startIndex);
    }
    if (s instanceof StringBuilder) {
      return ((StringBuilder) s).indexOf(
        String.valueOf(Character.toChars(searchChar)), startIndex);
    }
    if (s instanceof StringBuffer) {
      return ((StringBuffer) s).indexOf(
        String.valueOf(Character.toChars(searchChar)), startIndex);
    }
    if (s instanceof NumberedLines) {
      return ((NumberedLines) s).indexOf(searchChar, startIndex);
    }
    
    final int start = (startIndex < 0) ? 0: startIndex;
    for (int i=start, sz=s.length(); i<sz; ++i) {
      if (s.charAt(i) == searchChar) return i;
    }
    return -1;
  }
   
  public static int indexOf(final CharSequence s, final int searchChar) {
    return indexOf(s, searchChar, 0);
  }
  
  
  
  
  public static int indexOf(final CharSequence s, final CharSequence search,
  final int startIndex)
  {
    final String searchStr = (search instanceof String)
      ? (String) search
      : search.toString();
    
    if (s instanceof String) {
      return ((String) s).indexOf(searchStr, startIndex);
    }
    if (s instanceof StringBuilder) {
      return ((StringBuilder) s).indexOf(searchStr, startIndex);
    }
    if (s instanceof StringBuffer) {
      return ((StringBuffer) s).indexOf(searchStr, startIndex);
    }
    if (s instanceof NumberedLines) {
      return ((NumberedLines) s).indexOf(search, startIndex);
    }
    
    final int start = (startIndex < 0) ? 0: startIndex;
    for (int i=start, sz=s.length()-search.length(); i<sz; ++i) {
      if (s.charAt(i) == search.charAt(i)) {
        inner:
        for (int j=0; j<search.length(); ++j) {
          if (s.charAt(i+j) != search.charAt(j)) break inner;          
        }
        return i;
      }
    }
    return -1;
  }
  
  public static int indexOf(final CharSequence s, final CharSequence search) {
    return indexOf(s, search, 0);
  }
  
  static final LazyMember<Method> SHARE_VALUE = LazyMember.of(
    "java.lang.AbstractStringBuilder",
    CollectionUtil.isJRE()
      ? "getValue"
      : "shareValue",
    new Class[0]
  );
  
  
  public static String toString(final CharSequence cs) {
    if (cs instanceof String) return (String) cs;
    if (cs instanceof StringBuilder || cs instanceof StringBuffer) {
      final char[] ca = SHARE_VALUE.invoke(cs);
      return ca != null? String.valueOf(ca): cs.toString();
    }
    if (cs.length() >= 512) {
      for (final Field fld: cs.getClass().getDeclaredFields()) {
        try {
          if (fld.getType() == char[].class) {
            fld.setAccessible(true);
            final char[] ca = (char[]) fld.get(cs);
            if (ca != null && ca.length == cs.length()) return String.valueOf(ca);
          } else if (fld.getType() == String.class) {
            fld.setAccessible(true);
            final String s = (String) fld.get(cs);
            if (s != null && s.length() == cs.length()) return s;
          }
        } catch (ReflectiveOperationException roe) { }
      }
    }
    return cs.toString();
  }
  
  
  public static int lastIndexOf(final CharSequence s, final int searchChar,
  final int startIndex)
  {
    if (s instanceof String) {
      return ((String) s).indexOf(searchChar, startIndex);
    }
    final String searchStr = String.valueOf(Character.toChars(searchChar));
    if (s instanceof StringBuilder) {
      return ((StringBuilder) s).lastIndexOf(searchStr, startIndex);
    } else if (s instanceof StringBuffer) {
      return ((StringBuffer) s).lastIndexOf(searchStr, startIndex);
    }
    return s.toString().lastIndexOf(searchChar, startIndex);
  }
   
  public static int lastIndexOf(final CharSequence s, final int searchChar) 
  {
    return lastIndexOf(s, searchChar, s.length());
  }
  
  /*
  public static int lastIndexOf(final CharSequence s,
  final CharSequence search, final int startIndex)
  {
    final String searchStr = (search instanceof String)
      ? (String) searchStr
      : searchStr.toString();
    
    if (s instanceof String) {
      return ((String) s).lastIndexOf(searchStr, startIndex);
    }
    if (s instanceof StringBuilder) {
      return ((StringBuilder) s).lastIndexOf(searchStr, startIndex);
    }
    if (s instanceof StringBuffer) {
      return ((StringBuffer) s).lastIndexOf(searchStr, startIndex);
    }
    return s.toString().lastIndexOf(searchStr, startIndex);
  }
  */
  
  public static int lastIndexOf(final CharSequence s,
  final CharSequence search)
  {
    return lastIndexOf(s, search, search.length());
  }
  


  public static int lastIndexOf(final char[] value,
  final CharSequence subString, final int startIndex)
  {
    final int thisLen = value.length,
             subCount = subString.length();
    
    if (subCount > thisLen || startIndex < 0) return -1;
    if (subCount <= 0) return (startIndex < thisLen)? startIndex: thisLen;
    
    int start = Math.min(startIndex, thisLen - subCount);
    final CharSequence firstChar = String.valueOf(subString.charAt(0));
    while (true) {
      final int i = lastIndexOf(value, firstChar, start);
      if (i == -1) return -1;
      
      int o1 = i;
      int o2 = 0;
      while (++o2 < subCount && value[++o1] == subString.charAt(o2));
      
      if (o2 == subCount) return i;
      start = i - 1;
    }
  }
  
  public static int lastIndexOf(final CharSequence s,
  final CharSequence subString, final int startIndex)
  {
    if (s instanceof String 
    ||  s instanceof StringBuilder || s instanceof StringBuffer)
    {
      final String searchStr = (subString instanceof String)
         ? (String) subString
         : subString.toString();
      if (s instanceof String)
        return ((String) s).lastIndexOf(searchStr, startIndex);      
      if (s instanceof StringBuilder)
        return ((StringBuilder) s).lastIndexOf(searchStr, startIndex);
      if (s instanceof StringBuffer)
        return ((StringBuffer) s).lastIndexOf(searchStr, startIndex);
    }
        
    final int thisLen = s.length(),
             subCount = subString.length();
    
    if (subCount > thisLen || startIndex < 0) return -1;
    if (subCount <= 0) return (startIndex < thisLen)? startIndex: thisLen;
    
    int start = Math.min(startIndex, thisLen - subCount);
    final char firstChar = subString.charAt(0);
    while (true) {
      final int i = lastIndexOf(s, firstChar, start);
      if (i == -1) return -1;
      
      int o1 = i, o2 = 0;
      while (++o2 < subCount && s.charAt(++o1) == subString.charAt(o2)) ;
      
      if (o2 == subCount) return i;
      start = i - 1;
    }
  }
  
  
  static final boolean DEFAULT_USE_SHORTCUTS   = true;
  static final String  DEFAULT_OVERFLOW_SUFFIX = " ... <%d more chars>";

    
  
  public static CharSequence subSequence(final CharSequence s, final int start) {
    return subSequence(s, start, s.length(), DEFAULT_USE_SHORTCUTS);
  }
  
  public static CharSequence subSequence(final CharSequence s, final int start, 
  final int end)
  {
    return subSequence(s, start, end, DEFAULT_USE_SHORTCUTS);
  }
  
  public static CharSequence subSequence(final CharSequence s, final int start, 
  final int end, final boolean useShortcuts)
  {
    if (useShortcuts) {
      if (s instanceof String)
        return ((String) s).subSequence(start, end);
      if (s instanceof StringBuilder)
        return ((StringBuilder) s).subSequence(start, end);
      if (s instanceof StringBuffer)
        return ((StringBuffer) s).subSequence(start, end);
      if (s instanceof NumberedLines)
        return ((NumberedLines) s).subSequence(start, end);
      if (s instanceof OpenStringBuffer)
        return ((OpenStringBuffer) s).subSequence(start, end);
    }
    final int sublen = end - start;
    final StringBuilder sb = new StringBuilder(sublen);
    for (int i = start; i < end; ++i) {
      sb.append(s.charAt(i));
    }
    return sb;
  }
  
  
  
  
  public static String substring(final CharSequence s, final int start) {
    return substring(s, start, s.length(), DEFAULT_USE_SHORTCUTS);
  }
  
  public static String substring(final CharSequence s, final int start, 
  final int end)
  {
    return substring(s, start, end, DEFAULT_USE_SHORTCUTS);
  }
  
  public static String substring(final CharSequence s, final int start, 
  final int end, final boolean useShortcuts)
  {
    if (useShortcuts) {
      if (s instanceof String         || 
          s instanceof StringBuilder  || 
          s instanceof StringBuffer   ||
          s instanceof NumberedLines  ||
          s instanceof OpenStringBuffer)
      {
        final CharSequence subseq
          = (s instanceof String)
              ? ((String) s).subSequence(start, end)
              : (s instanceof StringBuilder)
                  ? ((StringBuilder) s).subSequence(start, end)
                  : (s instanceof StringBuffer)
                      ? ((StringBuffer) s).subSequence(start, end)
                      : (s instanceof NumberedLines)
                          ? ((NumberedLines) s).subSequence(start, end)
                          : ((OpenStringBuffer) s).subSequence(start, end);
                          
        if (subseq instanceof String) return (String) subseq;
      }
    } // useShortcuts
    
    return toString(subSequence(s, start, end, false));
  }
  
  public static CharSequence lengthCap(final CharSequence s, final int maxLength) {
    return lengthCap(s, maxLength, (String) null);
  }
  
  
  public static CharSequence lengthCap(final CharSequence s, final int maxLength, 
  final String optionalOverflowSuffixFormat)
  {
    final int sLen = s.length();
    if (sLen <= maxLength) return s;
    final String overflowSuffixFormat = (optionalOverflowSuffixFormat != null)
      ? optionalOverflowSuffixFormat
      : DEFAULT_OVERFLOW_SUFFIX;
    final boolean hasFmt = (indexOf(overflowSuffixFormat, '%', 0) != -1);
    final int roughMaxSuffixLen
      = ((hasFmt)
          ? String.format(overflowSuffixFormat, (Object) 10000)
          : overflowSuffixFormat
        ).length();
    final int sUsedLen = maxLength - roughMaxSuffixLen;
    final CharSequence suffix = (hasFmt)
      ? String.format(
          overflowSuffixFormat, (Object) (maxLength - sUsedLen - roughMaxSuffixLen)
        )
      : overflowSuffixFormat;    
    final StringBuilder sb = new StringBuilder(maxLength)
      .append(subSequence(s, 0, sUsedLen))
      .append(suffix);
    int sblen;
    while ((sblen = sb.length()) > maxLength && sblen >= sUsedLen+1) {
      sb.delete(sUsedLen, sUsedLen + 1);
    }
    return sb;
  }
  
  
}



