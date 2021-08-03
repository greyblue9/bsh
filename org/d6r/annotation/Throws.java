package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;



@Repeatable(ThrownTypes.class)
@Retention(RUNTIME)
public @interface Throws {
  
  Class<?>[] value() default {};
  String when() default "";
}
