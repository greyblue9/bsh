package bsh;

import bsh.Interpreter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.d6r.AccessFlags;

public class Modifiers2 implements Serializable {
  public static final int CLASS = 0;
  public static final int METHOD = 1;
  public static final int FIELD = 2;
  final EnumSet<AccessFlags> modifiers = newEnumSet(AccessFlags.class, new AccessFlags[0]);
  private static final Class<?> enumSetClass;
  private static final Constructor<?> enumSetCtor;

  static {
    Class cls = null;
    Constructor ctor = null;

    try {
      cls = Class.forName("java.util.MiniEnumSet");
      (ctor = cls.getDeclaredConstructor(new Class[]{Class.class, Enum[].class})).setAccessible(true);
    } catch (Throwable var3) {
      if(Interpreter.DEBUG) {
        var3.printStackTrace();
      }
    }

    enumSetClass = cls;
    enumSetCtor = ctor;
  }

  public static <E extends Enum<E>> EnumSet<E> newEnumSet(Class<E> enumCls, E... values) {
    try {
      return (EnumSet)enumSetCtor.newInstance(new Object[]{enumCls, values});
    } catch (Throwable var3) {
      if(Interpreter.DEBUG) {
        var3.printStackTrace();
      }

      return null;
    }
  }

  public Set<String> toStringSet() {
    HashSet set = new HashSet();
    Iterator var3 = this.modifiers.iterator();

    while(var3.hasNext()) {
      AccessFlags af = (AccessFlags)var3.next();
      set.add(af.toString());
    }

    return set;
  }

  public void addModifier(int context, String name) {
    this.modifiers.add(AccessFlags.valueOf(name.toLowerCase()));
    int count = 0;
    if(this.hasModifier("private")) {
      ++count;
    }

    if(this.hasModifier("protected")) {
      ++count;
    }

    if(this.hasModifier("public")) {
      ++count;
    }

    if(count > 1) {
      throw new IllegalStateException("public/private/protected cannot be used in combination.");
    } else {
      switch(context) {
      case 0:
        this.validateForClass();
        break;
      case 1:
        this.validateForMethod();
        break;
      case 2:
        this.validateForField();
      }

    }
  }

  public boolean hasModifier(String name) {
    return this.modifiers.contains(AccessFlags.valueOf(name.toLowerCase()));
  }

  private void validateForMethod() {
    this.insureNo("volatile", "Method");
    this.insureNo("transient", "Method");
  }

  private void validateForField() {
    this.insureNo("synchronized", "Variable");
    this.insureNo("native", "Variable");
    this.insureNo("abstract", "Variable");
  }

  private void validateForClass() {
    this.validateForMethod();
    this.insureNo("native", "Class");
    this.insureNo("synchronized", "Class");
  }

  private void insureNo(String modifier, String context) {
    if(this.hasModifier(modifier)) {
      throw new IllegalStateException(context + " cannot be declared \'" + modifier + "\'");
    }
  }

  public String toString() {
    return StringUtils.join(this.modifiers, " ");
  }
}
