package bsh;
import java.util.*;
import java.lang.reflect.*;
import org.d6r.Reflect;
import org.d6r.IdentityHashSet;
import java.lang.annotation.AnnotationTypeMismatchException;


/**
class bsh.EvalError {
  public volatile bsh.BshMethod bshMethod = <null>;
  private volatile Field[] fields = <null>;
  volatile Field fld = <null>;
  volatile Method method = <null>;
}
class bsh.ReflectError {
  volatile Member attempted = <null>;
}
*/
public class Errors {

  public static Collection<Member> getMembers(Throwable e) {
    final Collection<Member> mbs = new IdentityHashSet<>();
    return getMembers(e, mbs);
  }
  
  public static Collection<Member> getMembers(Throwable e, Collection<Member> mbs) 
  { 
    if (e instanceof ReflectError) {
      final ReflectError re = (ReflectError) e;
      if (re.attempted != null) mbs.add(re.attempted);
    }
    
    if (e instanceof EvalError) {
      final EvalError ee = (EvalError) e;
      if (ee.method != null) mbs.add(ee.method);
      if (ee.fld != null) mbs.add(ee.fld);
      if (ee.bshMethod != null) {
        final Member bmtdMember
          = Reflect.getfldval(ee.bshMethod, "javaMethod", true);
        if (bmtdMember != null) mbs.add(bmtdMember);
      }
      final Map<String, Object> data = ee.getData();
      for (final Map.Entry<String, Object> entry: data.entrySet()) {
        final Object value = entry.getValue();
        if (value instanceof Member) {
          mbs.add((Member) value);
          continue;
        }
        final String key = entry.getKey();
        if ("exception".equals(key)) {
          try {
            getMembers((Throwable) entry.getValue());
          } catch (Throwable ex) { e.printStackTrace(); }
          continue;
        }
      }
    }
    
    if (e instanceof AnnotationTypeMismatchException) {
      final AnnotationTypeMismatchException ae
         = (AnnotationTypeMismatchException) e;
      final Method element = ae.element();
      if (element != null) mbs.add(element);
    }
    
    return mbs;
  }
}