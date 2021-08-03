package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Retention(RUNTIME)
public @interface Doc {
  String value() default "";
  String[] details() default "";
  Class<?>[] see() default {};
  Class<?>[] seealso() default {};
}

