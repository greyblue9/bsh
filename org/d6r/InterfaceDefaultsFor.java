package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceDefaultsFor {
  
  public Class<?>[] value() default { };
  
}



