package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Retention(RUNTIME)
public @interface Obsolete {
  String value() 
    default "This method is obsolete. Please avoid using it.";
  MethodTag[] replacement() default {};
}


