package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ThisInterfaceParam {
  
  public int value() default 0;
  public String name() default "this";
  
}



