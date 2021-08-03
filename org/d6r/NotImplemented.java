package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;


@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
public @interface NotImplemented {
  
  String value() default "";
  Class<?> target() default Void.class;
  String method() default "";
  Class<?>[] params() default { };
  
  String notes() default "";
  String see() default "";
  boolean intercept() default false;
  
}

