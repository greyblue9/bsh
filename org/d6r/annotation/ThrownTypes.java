package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;



@Retention(RUNTIME)
public @interface ThrownTypes {
  
  Throws[] value() default {};
}
