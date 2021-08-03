package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;



@Repeatable(ReturnedValues.class)
@Retention(RUNTIME)
public @interface Returns {
  
  Class<?>[] type() default {};
  String value() default "";
}
