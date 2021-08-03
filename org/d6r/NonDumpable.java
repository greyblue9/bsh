package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NonDumpable {
  public String value() default "[non-dumpable]";
  public DisplayType replacement() default DisplayType.STRING;
}

