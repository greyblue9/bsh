package org.d6r.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.d6r.DisplayType;


@Retention(RetentionPolicy.RUNTIME)
public @interface NonDumpable {
  public String value() default "[non-dumpable]";
  public Class<?> renderer() default Void.class;
  public DisplayType replacement() default DisplayType.STRING;
}

