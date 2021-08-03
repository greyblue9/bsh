package org.d6r;


import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Modified {
  public static enum Aspect {
    ACCESS,
    RETURN_TYPE,
    PARAMETER_TYPE,
    TYPE_PARAMETERS,
    SIGNATURE,
    PACKAGE,
    NAME,
    ANNOTATIONS;
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target({
    ElementType.TYPE, ElementType.METHOD, 
    ElementType.ANNOTATION_TYPE
  })
  public @interface Change {
    public Aspect type();
    public String previous();
    public String current();    
  }
  
  public Change[] value();
}






