package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Origin {
  Class<?>[] value() default {};
  OriginSource[] sources() default {};
  boolean modified() default false;
  String originalPath() default "";
  String comments() default "";
  String[] removals() default {};
  String[] additions() default {};  
}
