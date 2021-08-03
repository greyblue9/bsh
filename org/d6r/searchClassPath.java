package org.d6r;
import bsh.*;
import java.lang.reflect.*;
import java.util.*;
import libcore.reflect.*;
import java.util.regex.*;
import org.d6r.ReflectionUtil;
import org.apache.commons.lang3.reflect.TypeUtils;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import java.net.URL;
//import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.io.FileUtils;
import java.io.File;

public class searchClassPath {
  
  private static Set<String> missingClasses 
    = new HashSet<String>(250);
    
  public static Matcher REGEX_XFORM_MATCHER
    = Pattern.compile("([A-Za-z])\\.([A-Za-z])")
             .matcher("");
  public static final Matcher UNESCAPED_MCHR
    = Pattern.compile("^[a-zA-Z0-9_.]+(?:\\$[a-zA-Z0-9_$]*)?$").matcher("");
  
  String[] invoke(Interpreter in, CallStack cs,
  String regex) {
    return searchClassPath(regex);
  }
  
  public static String[] searchClassPath(String regex) {
    String innerRegex 
      = (UNESCAPED_MCHR.reset(regex).matches())
          ? regex
          : REGEX_XFORM_MATCHER.reset(regex).replaceAll("$1[\\.\\$]$2");
    return ClassPathUtil.searchClassPath(
      Pattern.compile(innerRegex, Pattern.CASE_INSENSITIVE)
    );
    /*
    TreeSet<String> classNames = new TreeSet<String>();
    
    Collection<URL> entries;
    try { 
      entries = ClassPathUtil.getAllClasspathEntries();
    } catch (Throwable roe) {
      if ("true".equals(System.getProperty("printStackTrace"))) roe.printStackTrace();
      Reflector.Util.sneakyThrow(roe);
      throw new RuntimeException(roe);
    }
    
    for (URL entry: entries) {
      try {
        PathInfo pi = PathInfo.getPathInfo(entry.toString());
        if (pi == null || pi.path == null) continue; 
        Dex dex = new Dex(FileUtils.readFileToByteArray(
          new File(pi.path)
        ));
        int mNameIdx = dex.strings().indexOf("<clinit>");
        if (mNameIdx == -1) {
          Collections.addAll(
            classNames, new DexUtil(dex).getClassNames()
          );
          continue; 
        }
        String[] names = CollectionUtil.toArray(dex.typeNames());
        int skipped = 0;
        for (MethodId mid: dex.methodIds()) { 
          if (mid.getNameIndex() == mNameIdx) {
            skipped += 1;
            continue; 
          }
          classNames.add(DexVisitor.typeToName(
            names[mid.getDeclaringClassIndex()]
          ));
        }
        System.err.printf(
          "searchClassPath: skipped %d classes in %s "
          + "with <clinit> block\n", skipped, pi.path
        );
      } catch (Throwable e) { 
        System.err.println(Reflector.getRootCause(e));
      }
    }
    
    return ((Collection<String>) (Object) 
      StringCollectionUtil.toStringFilter(
        (Collection<String>) classNames,
        regex
      )
    ).toArray(new String[0]);
    //return ClassPathUtil.searchClassPath(regex);
    */
  }
  
  public static 
  String[] invoke(Interpreter in, CallStack cs,
  String... regexes) 
  {
    return searchClassPath(regexes);
  }
  
  public static 
  String[] searchClassPath(String... regexes) {    
    List<String> arrRegex 
      = new ArrayList<String>(regexes.length);
    
    for (int i=0; i<regexes.length; i++) {
      if (regexes[i ] == null) continue; 
      arrRegex.add(
        REGEX_XFORM_MATCHER.reset(regexes[i]).replaceAll(
          "$1[\\.\\$]$2"
        )
      );
    }
    if (arrRegex.size() == 0) return new String[0];
    
    String[] marr = ClassPathUtil.searchClassPath(
      arrRegex.toArray(new String[0])
    );
    
    Set<String> hs = new HashSet<String>(marr.length);
    Collections.addAll(hs, marr);
    
    String[] uniqArr = hs.toArray(new String[0]); 
    try {
      Arrays.sort(uniqArr); 
    } catch (Throwable e) {
      System.err.printf(
        "Problem sorting class names: %s\n",
        e.getClass().getSimpleName()
      );
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
    }
    return uniqArr;
  }
  
  
  public static Class<?>[] invoke(Interpreter in, 
  CallStack cs, List<Throwable> outExceptionList, 
  String... regexes) 
  {
    return searchClassPath(outExceptionList, regexes);
  }
  
  public static Class<?>[] searchClassPath(
  List<Throwable> outExceptionList, String... regexes) 
  {
    return searchClassPath( 
      outExceptionList, 
      Thread.currentThread().getContextClassLoader(),
      regexes
    );
  }
  
  
  
  public static Class<?>[] invoke(Interpreter in, 
  CallStack cs, List<Throwable> outExceptionList, 
  ClassLoader ldr, String... regexes) 
  {
    return searchClassPath(outExceptionList, ldr, regexes);
  }  
  
  public static Class<?>[] searchClassPath( 
  List<Throwable> outExceptionList, ClassLoader ldr,
  String... regexes) 
  {
    return searchClassPath(
      outExceptionList, ldr, regexes, false
    );
  }  
  
  public static Class<?>[] invoke(Interpreter in, 
  CallStack cs, List<Throwable> outExceptionList, 
  ClassLoader ldr, String[] regexes, 
  boolean printExceptions) 
  {
    return searchClassPath(
      outExceptionList, ldr, regexes, printExceptions
    );
  }  
  
  public static Class<?>[] searchClassPath( 
  List<Throwable> outExceptionList, ClassLoader ldr,
  String[] regexes, boolean printExceptions) 
  {
    if (outExceptionList == null) {
      outExceptionList = new ArrayList<Throwable>();
    }
    
    String[] classNames = searchClassPath(regexes);
    HashSet<Class<?>> classes = new HashSet<Class<?>>();
    
    for (String crntName: classNames) { 
      try {
        if (! printExceptions 
        && missingClasses.contains(crntName)) continue; 
        
        Class<?> crntCls 
          = Class.forName(crntName, false, ldr);
        classes.add(crntCls);
      } catch (Throwable ex) { 
        missingClasses.add(crntName);
        outExceptionList.add(ex);
      }
    }
    
    Class<?>[] arr = classes.toArray(new Class<?>[0]);
    Comparator<Class<?>> clsComp = new ClassNameComparator();
    Arrays.sort(arr, clsComp);
    
    if (printExceptions) {
      for (Throwable e: outExceptionList) {
        System.err.printf("<%s: [%s]>\n", 
          e.getClass().getSimpleName(),
          e.getMessage() != null? e.getMessage(): "[no msg]"
        );
      }
    }
    return arr;
  }
  
  
  public static Class<?>[] invoke(Interpreter in, 
  CallStack cs, List<Throwable> outExceptionList,
  String regex) 
  { 
    return searchClassPath(outExceptionList, regex);
  }
  
  public static Class<?>[] searchClassPath(
  List<Throwable> errs, String regex) 
  { 
    if (errs == null) errs = new ArrayList<Throwable>(); 
    Class<?>[] arr
      = searchClassPath(errs, new String[]{ regex });
    
    Comparator<Class<?>> clsComp = new ClassNameComparator();
    Arrays.sort(arr, clsComp);
    for (Throwable e: errs) {
      System.err.printf(
        "<%s: [%s]>\n", 
        e.getClass().getSimpleName(),
        e.getMessage() != null? e.getMessage(): "[no message]"
      );
    }
    return arr;
  }    
  
  public static class ClassNameComparator 
    implements Comparator<Class<?>> 
  {
    @Override
    public int compare(Class<?> a, Class<?> b) {
      if (a == null) {
        if (b == null) return 0;
        return -1;
      }
      if (b == null) return 1;
      return a.getName().compareTo(b.getName());
    }
    
    public boolean equals(Class<?> a, Class<?> b) { 
      if (a == null) return false;
      if (b == null) return false;
      return 
          a.getName().equals(b.getName());
       //&& a.getClassLoader() == b.getClassLoader();
    }  
  }
  
}