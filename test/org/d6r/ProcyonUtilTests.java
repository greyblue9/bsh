package test.org.d6r;

import org.d6r.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.languages.java.ast.*;
import bsh.Factory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.builder.EqualsBuilder;


public class ProcyonUtilTests {
  
  public static void testRoundTrip() {
    
    Member[] members = {
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[0]),
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[]{ Integer.TYPE }),
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[]{ Collection.class }),
      Reflect.getDeclaredConstructorOrMethod(
        TreeMap.class, "entrySet", new Class[0]),
      Reflect.getDeclaredConstructorOrMethod(
        TreeMap.class, "put", new Class[]{ Object.class,Object.class}),
      Reflect.getDeclaredConstructorOrMethod(
        Reflect.class, "<clinit>", new Class[0])
    };
    
  
    
    int passes = 0;
    int failures = 0;
    int i = -1;
    for (Member ctor: members)
    {
      ++i;
      if (i == 0) {
         System.err.printf("    \n*** Constructora ***\n\n");
      } else if (i == 3) {
         System.err.printf("    \n*** Methods ***\n\n");
      } else if (i == 5) {
         System.err.printf("    \n*** Static Initializers ***\n\n");
      }
      
      try {
        System.err.printf(
          "    \ntesting getMethodDefinition((%s) %s)\n",
          ctor.getClass().getSimpleName(),
          (ctor instanceof Method)
              ? ((Method) ctor).toGenericString()
              : ((Constructor<?>) ctor).toGenericString()
        );
        
        MethodDefinition md = ProcyonUtil.getMethodDefinition(ctor);
        System.err.printf(
          "      -> (%s)  %s\n", 
          md == null? "<NULL>" : md.getClass().getSimpleName(),
          md
        );
        
        System.err.printf(
          "    testing getMethod((%s) %s)\n",
          md == null? "<NULL>" : md.getClass().getSimpleName(),
          md
        );
        
        Member member = ProcyonUtil.getMethod(md);
        System.err.printf(
          "      -> (%s)  %s\n", 
          member == null? "<NULL>" : member.getClass().getSimpleName(),
          (member instanceof Method)
            ? ((Method) member).toGenericString()
            : ((Constructor<?>) member).toGenericString()        
        );
      
        
        boolean identical = 
          ctor.getClass().equals(Factory.typeof(member))
          && 
            ((member instanceof Method)
            ? ((Method) member).toGenericString()
            : ((Constructor<?>) member).toGenericString())
          .equals(
            (ctor instanceof Method)
              ? ((Method) ctor).toGenericString()
              : ((Constructor<?>) ctor).toGenericString()
          )
          && ctor.getDeclaringClass().equals(
               member.getDeclaringClass()
             );
        
        
        boolean equal = (ctor.equals(member));
        
        System.err.printf(
          "      - identical? %s\n  - equal? %s\n",
          Boolean.valueOf(identical),
          Boolean.valueOf(equal)
        );
        
        if (! identical || ! equal) {
          failures++;
          System.err.printf(
            "    \u001b[1;31m[FAILURE]\u001b[0m\n"
            + "  (a) %s\n    -> (b) %s ->\n      --/-> (c) %s\n",
            (ctor instanceof Method)
                ? ((Method) ctor).toGenericString()
                : ((Constructor<?>) ctor).toGenericString(),
            md,
            (member instanceof Method)
              ? ((Method) member).toGenericString()
              : ((Constructor<?>) member).toGenericString()        
          );
        } else {
          passes++;        
          System.err.printf(
            "    \u001b[1;32m[SUCCESS]\u001b[0m\n"
          );
        }
      } catch (Exception e) { 
          failures++;
          System.err.printf(
            "    \u001b[1;31m[FAILURE] EXCEPTION \u001b[1;36m%s\u001b[0m\n"
            + "  %s\n",
            Reflector.getRootCause(e).getClass().getSimpleName(),
            StringUtils.join(
              ExceptionUtils.getRootCauseStackTrace(e), "\n  "
            )
          );
          Reflector.getRootCause(e).printStackTrace();
      }
    }
    
    System.err.printf(
      "    \n==================== TOTAL ====================\n"
      + "     Passes:   %d\n"
      + "   Failures:   %d\n"
      + "\n"
      + "Grand Total:   %d / %d  (%5f %%)\n",
      passes, failures, 
      passes, (passes+failures),
       ((double) ( (int)
         (((double)passes) * 10000D / ((double) (passes+failures)))
       )) / 100D
    );  
  }
  
  
  
  public static void testRoundTrip2() {
    
    Member[] members = {
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[0]),
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[]{ Integer.TYPE }),
      Reflect.getDeclaredConstructorOrMethod(
        ArrayList.class, "<init>", new Class[]{ Collection.class }),
      Reflect.getDeclaredConstructorOrMethod(
        TreeMap.class, "entrySet", new Class[0]),
      Reflect.getDeclaredConstructorOrMethod(
        TreeMap.class, "put", new Class[]{ Object.class,Object.class}),
      Reflect.getDeclaredConstructorOrMethod(
        Reflect.class, "<clinit>", new Class[0])
    };
    
  
    
    int passes = 0;
    int failures = 0;
    int i = -1;
    for (Member ctor: members)
    {
      ++i;
      if (i == 0) {
         System.err.printf("    \n*** Constructora ***\n\n");
      } else if (i == 3) {
         System.err.printf("    \n*** Methods ***\n\n");
      } else if (i == 5) {
         System.err.printf("    \n*** Static Initializers ***\n\n");
      }
      
      try {
        System.err.printf(
          "    \ntesting getMethodDefinition((%s) %s)\n",
          ctor.getClass().getSimpleName(),
          (ctor instanceof Method)
              ? ((Method) ctor).toGenericString()
              : ((Constructor<?>) ctor).toGenericString()
        );
        
        MethodDefinition md = ProcyonUtil.getMethodDefinition2(ctor);
        System.err.printf(
          "      -> (%s)  %s\n", 
          md == null? "<NULL>" : md.getClass().getSimpleName(),
          md
        );
        
        System.err.printf(
          "    testing getMethod((%s) %s)\n",
          md == null? "<NULL>" : md.getClass().getSimpleName(),
          md
        );
        
        Member member = ProcyonUtil.getMethod(md);
        System.err.printf(
          "      -> (%s)  %s\n", 
          member == null? "<NULL>" : member.getClass().getSimpleName(),
          (member instanceof Method)
            ? ((Method) member).toGenericString()
            : ((Constructor<?>) member).toGenericString()        
        );
      
        
        boolean identical = 
          ctor.getClass().equals(Factory.typeof(member))
          && 
            ((member instanceof Method)
            ? ((Method) member).toGenericString()
            : ((Constructor<?>) member).toGenericString())
          .equals(
            (ctor instanceof Method)
              ? ((Method) ctor).toGenericString()
              : ((Constructor<?>) ctor).toGenericString()
          )
          && ctor.getDeclaringClass().equals(
               member.getDeclaringClass()
             );
        
        
        boolean equal = (ctor.equals(member));
        
        System.err.printf(
          "      - identical? %s\n  - equal? %s\n",
          Boolean.valueOf(identical),
          Boolean.valueOf(equal)
        );
        
        if (! identical || ! equal) {
          failures++;
          System.err.printf(
            "    \u001b[1;31m[FAILURE]\u001b[0m\n"
            + "  (a) %s\n    -> (b) %s ->\n      --/-> (c) %s\n",
            (ctor instanceof Method)
                ? ((Method) ctor).toGenericString()
                : ((Constructor<?>) ctor).toGenericString(),
            md,
            (member instanceof Method)
              ? ((Method) member).toGenericString()
              : ((Constructor<?>) member).toGenericString()        
          );
        } else {
          passes++;        
          System.err.printf(
            "    \u001b[1;32m[SUCCESS]\u001b[0m\n"
          );
        }
      } catch (Exception e) { 
          failures++;
          System.err.printf(
            "    \u001b[1;31m[FAILURE] EXCEPTION \u001b[1;36m%s\u001b[0m\n"
            + "  %s\n",
            Reflector.getRootCause(e).getClass().getSimpleName(),
            StringUtils.join(
              ExceptionUtils.getRootCauseStackTrace(e), "\n  "
            )
          );
          Reflector.getRootCause(e).printStackTrace();
      }
    }
    
    System.err.printf(
      "    \n==================== TOTAL ====================\n"
      + "     Passes:   %d\n"
      + "   Failures:   %d\n"
      + "\n"
      + "Grand Total:   %d / %d  (%5f %%)\n",
      passes, failures, 
      passes, (passes+failures),
       ((double) ( (int)
         (((double)passes) * 10000D / ((double) (passes+failures)))
       )) / 100D
    );  
  }

}



