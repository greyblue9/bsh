package org.d6r.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;



@Repeatable(Params.class)
@Retention(RUNTIME)
public @interface Param {

  String name() default "?";
  public Class<?> type() default Object.class;
  Doc[] doc() default {};
  int index() default -1;


}



