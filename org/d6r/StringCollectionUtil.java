package org.d6r;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.collections4.set.ListOrderedSet;
import bsh.operators.Extension;
import bsh.Factory;
import bsh.ClassIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
  * Static utility methods for working with collections of strings.
  *
  * @author Carl Harris
  */
public class StringCollectionUtil {
  
  public static final int estimate(int numInput) {
    return (int) (numInput / 25);
  }
  
  public static final String[] EMPTY_STRING_ARRAY 
    = new String[0];
  public static final Collection<String> EMPTY_COLL
    = new ArrayList<String>(0) {
      transient String[] array = { };
    };
  public static final String EMPTY_STRING = "";
  
  public static final Collection<String> emptyStringList() {
    return Collections.<String>emptyList();
  }
  
  public static int PTRN_FLAGS
    = Pattern.CASE_INSENSITIVE 
    | Pattern.DOTALL 
    | Pattern.MULTILINE 
    | Pattern.UNIX_LINES;
  
  public static Matcher IMPOSSIBLE_MATCHER 
    = Pattern.compile("$^").matcher(EMPTY_STRING);
  
  
  @Extension 
  public static Matcher[] toMatchers(Object... regexes) {
    if (regexes.length == 1 
    &&  regexes[0] instanceof Iterable) {
      return toMatchers((Iterable<?>) regexes[0]);
    }
    int i = -1, len = regexes.length;
    Matcher[] matchers = new Matcher[len];
    Object regex;
    while (++i < len && (regex = regexes[i]) != null) {
      matchers[i] = toMatcher(regex);
    }
    return matchers;
  }
  
  @Extension 
  public static Matcher[] toMatchers(Iterable<?> regexes) { 
    List<Matcher> matchers = new ArrayList<Matcher>();
    for(Iterator<?> it = regexes.iterator(); it.hasNext();) {
      Object regex = it.next();
      matchers.add(toMatcher(regex));
    }
    return matchers.toArray(new Matcher[0]);
  }
  
  @Extension 
  public static Matcher toMatcher(Object regex) {
    if (regex instanceof String) {
      regex = Pattern.compile((String) regex, PTRN_FLAGS);
    }
    if (regex instanceof Pattern) {
      regex = ((Pattern) regex).matcher(EMPTY_STRING);
    }
    if (regex instanceof Matcher) {
      return (Matcher) regex;
    }
    return IMPOSSIBLE_MATCHER;
  }
  
  
  public static Pattern toPattern(String input) {
    return Pattern.compile(input, PTRN_FLAGS);
  }
  
  
  public static Pattern[] toPatterns(String... inputs) {
    Pattern[] ptrns = new Pattern[inputs.length];
    int i = -1;
    while (++i < inputs.length) {
      ptrns[i] = Pattern.compile(inputs[i], PTRN_FLAGS);
    }
    return ptrns;
  }
  
  @Extension public static <U> U toStringFilter(Object items, String regex) 
  {
    if (items instanceof Object[]) {
      return (U) toStringFilter((Object[]) items, new String[]{ regex });
    }
    if (items instanceof Iterable<?>) {
      return (U) toStringFilter(
        (Iterable<Object>) (Iterable<?>) items, 
        new String[]{ regex }
      );
    }
    return (U) CollectionUtil.flatten(Arrays.asList(
      CollectionUtil.selectLines(items, regex)
    ));
  }
  
  @Extension public static <T>
  T[] toStringFilter(T[] arr, Object... regexes) {
    if (regexes.length > 0
    &&  regexes[0] instanceof Matcher) {
      return toStringFilter(arr, (Matcher[]) regexes);
    }
    return toStringFilter(arr, toMatchers(regexes));
  }
  
  @Extension public static <T> 
  T[] toStringFilter(T[] arr, Matcher... regexes)
  { 
    if (arr == null) {
      System.err.println("toStringFilter: array == null");
      return (T[]) new Object[0];
    }    
    if (arr.length == 0) return arr;
    Set<T> out = new ListOrderedSet<T>(); 
    for (Matcher mchr: regexes) {
      for (T elem: arr) {
        if (elem == null) continue;
        try {
          String entry = elem.toString();
          if (entry.indexOf('\u001b') != -1) {
            entry = TextUtil.colorrm(entry);
          }
          if (mchr.reset(entry).find()) {
            out.add(elem);
          }
        } catch (Throwable e) { }
      }
    }

    return (T[]) out.toArray((T[]) Array.<T>newInstance(
      (Class<T>) arr.getClass().getComponentType(), 0
    ));
  }
  
  
  @Extension public static <T> 
  Collection<T> toStringFilter(Iterable<T> coll, 
  Object... regexes) {    
    if (coll == null) throw new NullPointerException(
      "toStringFilter: collection == null"
    );
    Matcher[] matchers = toMatchers(regexes);
    Set<T> out = new ListOrderedSet<T>(); 
    for (Matcher mchr: matchers) {
      for (T elem: coll) {
        if (elem == null) continue;
        try {
          String entry = elem.toString();
          if (entry.indexOf('\u001b') != -1) {
            entry = TextUtil.colorrm(entry);
          }
          if (mchr.reset(entry).find()) {
            out.add(elem);
          }
        } catch (Throwable e) { }
      }
    }
    return out;
  }
  
  
  /**
    * Retains all values in the subject collection that are matched by
    * at least one of a collection of regular expressions.
    * <p>
    * This method is a convenience overload for
    * {@link #retainMatching(Collection, Collection)}.
    *   
    * @param values subject value collection 
    * @param matchers matchers to match
    */
  @Extension public static <T>
  Collection<T> retainMatching(Collection<T> values, 
  Object... regexes) {    
    if (values == null) throw new NullPointerException(
      "retainMatching: collection == null"
    );
    if (regexes.length == 0) return values;
    Matcher[] matchers = toMatchers(regexes);
    List<T> matches = new ArrayList<T>(values.size());
    for (Matcher mchr: matchers) {
      for (T elem: values) {
        if (elem == null) continue;
        try {
          String entry = elem.toString();
          if (entry.indexOf('\u001b') != -1) {
            entry = TextUtil.colorrm(entry);
          }
          if (mchr.reset(entry).find()) {
            matches.add(elem);
          }
        } catch (Throwable e) { }
      }
    }
    values.retainAll(matches);
    return values;
  }
  
  public static Matcher getMatcher(String regex) {
    return Pattern.compile(regex, PTRN_FLAGS)
                  .matcher(EMPTY_STRING);
  }
  
  public static 
  Matcher getMatcher(String regex, String input) {
    return Pattern.compile(regex, PTRN_FLAGS)
                  .matcher(input);
  }
  
  
/**     ______________________________
_______/                              \______________________
            Collection<String> input
*/
  
  // String or String[] pattern(s) 
  @Extension public static <T> 
  T[] matchingSubset(Iterable<T> values, Object... regexes) 
  {
    if (regexes.length == 0) return (T[]) new Object[0];
    if (values == null) throw new NullPointerException(
      "matchingSubset: collection == null"
    );
    Matcher[] matchers = toMatchers(regexes);    
    List<T> matches = new ArrayList<T>();
    int size = 0;
    Class<?> cls = null;
    for (Matcher mchr: matchers) {
      for (T value: values) {
        if (value == null) continue;
        if (cls == null) {
          cls = value.getClass();
        } else if (!cls.isInstance(value)) {
          cls = value.getClass();
        }
        try {
          String entry = value.toString();
          if (entry.indexOf('\u001b') != -1) {
            entry = TextUtil.colorrm(entry);
          }
          if (mchr.reset(entry).find()) {
            matches.add(value);
            size++;
          }
        } catch (Throwable e) { e.printStackTrace(); }
      }
    }
    return (T[]) matches.toArray(
      (T[]) Array.newInstance(
        cls != null? cls: Object.class, size
      )
    );
  }
  
 
/**     ______________________________
_______/                              \_____________________
             String[] values input                     
*/
  
 
  @Extension public static <T> 
  T[] matchingSubset(T[] values, Object... regexes) {
    if (values == null) throw new NullPointerException(
      "matchingSubset: values == null"
    );
    Matcher[] matchers = toMatchers(regexes);    
    Class<T> cls 
      = (Class<T>) values.getClass().getComponentType();    
    if (matchers.length == 0) return (T[])Arrays.copyOfRange(
      values, 0, 0, values.getClass()
    );    
    T[] matches 
      = (T[]) Array.newInstance(cls, values.length);    
    int nextIndex = 0;
    for (Matcher mchr: matchers) {
      for (T value: values) {
        if (value == null) continue;
        if (cls == null) {
          cls = (Class<T>) (Class<?>) value.getClass();
        } else if (!cls.isInstance(value)) {
          cls = (Class<T>) (Class<?>) value.getClass();
        }
        try {
          String entry = value.toString();
          if (entry.indexOf('\u001b') != -1) {
            entry = TextUtil.colorrm(entry);
          }
          if (mchr.reset(entry).find()) {
            matches[nextIndex++] = value;
          }
        } catch (Throwable e) { e.printStackTrace(); }
      }
    }
    return (T[]) Arrays.copyOfRange(
      matches, 0, nextIndex, values.getClass()
    );
  }
  
 
  
 
  /**
    * Removes all values in the subject collection that are matched by
    * at least one of a collection of regular expressions.
    * <p>
    * This method is a convenience overload for
    * {@link #removeMatching(Collection, Collection)}.
    * 
    * @param values subject value collection 
    * @param matchers matchers to match
    */
  @Extension public static <T>
  Collection<? extends T> removeMatching(Collection<? extends T> values,
  Object... ptrns)
  {
    boolean unsafe = false;
    Matcher[] matchers = toMatchers(ptrns);
    int i = 0;
    for (Iterator<? extends T> it = values.iterator(); it.hasNext(); ++i) {
      T value = it.next();
      String strValue = EMPTY_STRING;
      try {
        strValue = value.toString();
      } catch (Throwable e) {
        String clsName = ClassInfo.typeToName(
          value != null ? value.getClass().getName() : "null"
        );
        System.err.printf(
          "[WARN] skipping `%s' @ index [%d], "
          + "because the %s.toString() method threw %s\n",
          clsName,
          i,
          ClassInfo.simplifyName(clsName),
          e.getClass().getSimpleName(),
          Reflector.getRootCause(e)
        );
        continue;
        /*if (!unsafe) {
          unsafe = true;
          System.err.printf(
            "Warning: resorting to unsafe string extraction due to %s...\n",
            e
          );
        }
        byte[] bytes = new Pointer(value).getBytes(128);
        UnsafeUtil.changeClass(bytes, char[].class);
        char[] chars = (char[]) (Object) bytes;
        strValue = UnsafeUtil.toString(chars); */
      }
      for (Matcher mchr: matchers) {
        if (mchr.reset(strValue).find()) {
          it.remove();
        }
      }
    }
    return values;
  }
  
  /**
    * Removes all values in the subject collection that are matched by
    * at least one of a collection of regular expressions.
    * <p>
    * The semantics of this method are conceptually similar to
    * {@link Collection#removeAll(Collection)}, but uses pattern matching
    * instead of exact matching.
    * 
    * @param values subject value collection 
    * @param matchers matchers to match
    */
  @Extension public static <T>
  void removeMatching(Collection<T> values, 
  Collection<?> regexesColl) {
    removeMatching(values, toMatchers(regexesColl));
  }
  
  @Extension 
  public static <T> T[] removeMatching(T[] values, Object... ptrns) {
    Matcher[] matchers = toMatchers(ptrns);
    return ArrayUtils.removeElements(
      values,
      StringCollectionUtil.toStringFilter(values, matchers)
    );
  }
  
  
  @Extension public static
  List<String> matchAll(CharSequence input, Matcher mchr) {
     List<String> matches = new ArrayList<String>();
     mchr.reset(
       (input instanceof String? (String) input: input.toString())
     );
     while (mchr.find()) { 
       matches.add(mchr.group(1));
     } 
     return matches;
   }
   
   @Extension public static 
   List<String> matchAll(CharSequence input, Pattern ptrn) { 
     return matchAll(input, ptrn.matcher(""));
   }
   
   @Extension public static
   List<String> matchAll(CharSequence input, String regex) { 
     return matchAll(input, Pattern.compile(regex));
   }
   
   @Extension public static 
   List<String> matchAll(CharSequence input, Matcher mchr,
   CharSequence format) 
   {
     List<String> matches = new ArrayList<String>();
     mchr.reset(
       (input instanceof String? (String) input: input.toString())
     );
     String fmt = (format instanceof String)
       ? (String) format
       : format.toString();
     
     StringBuilder match = new StringBuilder(160);
     while (mchr.find()) { 
       match = match.replace(0, match.length(), fmt);
       int groupNum = -1; 
       while (++groupNum <= mchr.groupCount()) { 
         String search = "$".concat(String.valueOf(groupNum));
         int pos; 
         while ((pos = match.indexOf(search))!= -1) {
           match.replace(
             pos, pos + search.length(), mchr.group(groupNum)
           );
         }
       }
       matches.add(match.toString());
     }
     return matches;
   }
   
   @Extension public static
   List<List<CharSequence>> matchLines(CharSequence input, 
   String regex) 
   { 
     Matcher mchr = Pattern.compile(
       regex, 
       Pattern.CASE_INSENSITIVE 
       | Pattern.DOTALL 
       | Pattern.MULTILINE 
       | Pattern.UNIX_LINES).matcher(""); 
     NumberedLines nl = new NumberedLines(
       (input instanceof String
         ? (String) input: input.toString())); 
     int[] lens = nl.getLengths(); 
     int nlines = lens.length; 
     List lineResults = new ArrayList(nlines); for (int l=0; l<nlines; l+=1) { List lineGroupLists = new ArrayList(); CharSequence line = nl.getLine(l+1); mchr.reset(line); while (mchr.find()) { List groups = new ArrayList(mchr.groupCount()); for (int g=1; g<=mchr.groupCount(); g+=1) { groups.add(mchr.group(g)); }; lineGroupLists.add(groups); }; lineResults.addAll(lineGroupLists); }; return lineResults;
   }
   
   @Extension public static 
   List<String> matchAll(CharSequence input, Pattern ptrn,
   CharSequence format) {
     return matchAll(input, ptrn.matcher(""), format);
   }
   
   @Extension public static
   List<String> matchAll(CharSequence input, String regex,
   CharSequence format) {
     return matchAll(input, Pattern.compile(regex), format);
   }
   
   
   @Extension public static 
   List<String> matches(Object mchr, CharSequence input,
   CharSequence format) {     
     if (mchr instanceof String) {
       mchr = Pattern.compile((String) mchr);
     }
     if (mchr instanceof Pattern) {
       mchr = ((Pattern) mchr).matcher("");
     }
     if (mchr instanceof Matcher) {
       return matchAll(input, (Matcher) mchr, format);
     }
     return matches((String) mchr.toString(), input, format);
   }
   
   
}