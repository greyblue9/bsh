package bsh;

import com.google.common.collect.ImmutableBiMap;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.d6r.ConstUtil;

public class Modifiers implements Serializable {
  public static final int CLASS = 0;
  public static final int FIELD = 1;
  public static final int METHOD = 2;
  int value;
  static ImmutableBiMap<Integer, String> BY_VALUE 
    = ConstUtil.constantMap(Modifier.class, -1L);
  static Map<String, Integer> BY_NAME;

  static {
    BY_NAME = BY_VALUE.inverse();
  }
  
  public static int intValue(String name) {
    Integer val = (Integer)BY_NAME.get(name);
    if(val == null) {
      val = (Integer)BY_NAME.get(name.toUpperCase());
    }

    if(val == null) {
      System.err.printf("Modifier.intValue(\"%s\"): BY_NAME (%s) has no entry for \"%s\"\n", new Object[]{name, BY_NAME, name.toUpperCase()});
      return 0;
    } else {
      return val.intValue();
    }
  }

  public static String toString(int val) {
    return Modifier.toString(val);
  }

  public String toString() {
    return toString(this.value);
  }

  protected void insureNo(int mod, int ctx) {
  }

  protected void validateForClass() {
    this.validateForMethod();
    this.insureNo(32, 0);
    this.insureNo(256, 0);
  }

  protected void validateForField() {
    this.insureNo(32, 1);
    this.insureNo(256, 1);
    this.insureNo(1024, 1);
  }

  protected void validateForMethod() {
    this.insureNo(64, 2);
    this.insureNo(128, 2);
  }

  public void addModifier(int context, String name) {
    boolean isProt = this.hasModifier(4);
    boolean isPriv = this.hasModifier(2);
    boolean isPubl = this.hasModifier(1);
    int n = 0;
    if(isProt) {
      ++n;
    }

    if(isPriv) {
      ++n;
    }

    if(isPubl) {
      ++n;
    }

    if(n > 1) {
      throw new IllegalStateException("public/protected/protected cannot be used in combination.");
    } else {
      if(context == 0) {
        this.validateForClass();
      } else if(context == 1) {
        this.validateForField();
      } else if(context == 2) {
        this.validateForMethod();
      }

    }
  }

  public boolean hasModifier(String name) {
    int val = intValue(name);
    return val == 0?false:(this.value & val) == val;
  }

  public boolean hasModifier(int val) {
    return val == 0?false:(this.value & val) == val;
  }
}