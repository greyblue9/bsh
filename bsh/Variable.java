package bsh;

import bsh.Factory;
import bsh.Interpreter;
import bsh.LHS;
import bsh.Modifiers;
import bsh.Primitive;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map.Entry;
import org.apache.commons.lang3.ClassUtils;
import org.d6r.ClassInfo;
import org.d6r.Debug;
import static java.lang.String.format;
import org.apache.commons.lang3.StringEscapeUtils;
import org.d6r.CharSequenceUtil;


public class Variable<V> implements Serializable, Entry<String, V> {
  public static int DECLARATION = 0;
  public static int ASSIGNMENT = 1;
  protected Integer calcCode;
  String name;
  Class<? super V> type;
  String typeDescriptor;
  V value;
  Modifiers modifiers;
  LHS lhs;
  static PrintStream dbg;

  static {
    dbg = System.err;
  }
  
  private static String contextNamePastTense(final int context) {
    return context == ASSIGNMENT
      ? "assigned"
      : ((context == DECLARATION)      
          ? "declared"
          : format("UnknownContext<%d>", context));
  }
  
  private static StringBuilder quote(final Object obj) {
    if (obj == null) return null;
    final StringBuilder sb = (obj instanceof StringBuilder)
      ? (StringBuilder) obj
      : null;
    if (sb != null) {
      return new StringBuilder(sb).insert(0, '\"').append('"');
    }
    final CharSequence cs = (obj instanceof CharSequence)
      ? (CharSequence) obj
      : String.valueOf(obj);
    return new StringBuilder(cs.length() + 10)
      .append('\"')
      .append(
        StringEscapeUtils.escapeJava(CharSequenceUtil.toString(cs))
     ).append('\"');
  }
  
  private static CharSequence debugDisplayValue(final Object obj) {
    final CharSequence cs = (obj instanceof CharSequence)
      ? (CharSequence) obj
      : String.valueOf(obj);
    if (cs == null) return Debug.ToString(obj);
    
    return quote(
      (cs.length() > 65)
        ? ((cs instanceof String)
            ? ((String) cs).subSequence(0, 65)
            : new StringBuilder(cs).delete(65,
                ((CharSequence) cs).length()))
        : cs
    ).append("...");
  }
  
  
  public Variable(final String name, Class<? super V> type, final V value, 
  final Modifiers modifiers)
  {
    this.name = name;
    this.type = type;
    this.set(value, DECLARATION);
    this.modifiers = modifiers;
    if (Interpreter.DEBUG) dbg.printf(
      "new Variable(name: '%s', type: %s, value: %s, modifiers: %s)\n",
      name,
      (type != null)? ClassInfo.typeToName(type): "null",
      debugDisplayValue(value),
      modifiers
    );
  }

  public Variable(final String name, final Class<? super V> type,
  final LHS lhs)
  {
    this.name = name;
    this.type = type;
    this.lhs = lhs;
    this.set(null, DECLARATION);
    if (Interpreter.DEBUG) dbg.printf(
      "new Variable(name: '%s', type: %s, lhs: %s)\n",
      name,
      (type != null)? ClassInfo.typeToName(type.getClass()): "null",
      Debug.ToString(lhs)
    );
  }

  public Variable(final String name, final V value,
  final Modifiers modifiers)
  {
    this.name = name;
    this.set(value, DECLARATION);
    this.modifiers = modifiers;
    if (Interpreter.DEBUG) dbg.printf(
      "new Variable(name: '%s', value: %s, modifiers: %s)\n",
      name, debugDisplayValue(value), modifiers
    );
  }

  public Variable(final String name, final String typeDescriptor,
  final V value, final Modifiers modifiers)
  {
    this.name = name;
    this.typeDescriptor = typeDescriptor;
    this.set(value, DECLARATION);
    this.modifiers = modifiers;
    if (Interpreter.DEBUG) dbg.printf(
      "new Variable(name: '%s', typeDescriptor: '%s', value: %s, "
      + "modifiers: %s)\n",
      name, typeDescriptor, debugDisplayValue(value), modifiers
    );
  }
  
  public Variable(String name, V value) {
    this.name = name;
    this.set(value, DECLARATION);
    if (Interpreter.DEBUG) dbg.printf(
      "new Variable(name: '%s', value: %s)\n",
      name, debugDisplayValue(value)
    );
  }
  
  
  public Variable(Entry<? extends CharSequence, ? extends V> copyFrom) {
    this(String.valueOf(copyFrom.getKey()), (Class)((Class)(copyFrom.getValue() != null?copyFrom.getValue().getClass():null)), copyFrom.getValue(), (Modifiers)null);
  }

  public String getKey() {
    return this.name;
  }

  public V getValue() {
    return this.value;
  }

  public V setValue(V value, int context) {
    if(Interpreter.DEBUG) {
      dbg.printf("var@%8x [%s] %s: setValue(%s, ctx)\n", new Object[]{Integer.valueOf(this.hashCode()), this.name, contextNamePastTense(context), debugDisplayValue(value)});
    }

    return this.set(value, context);
  }
  
  public V setValue(V value) {
    Object ret = this.set(value, ASSIGNMENT);
  return (V) (Object) ret;
  }

  public V set(V val, int context) {
    if (lhs != null) {
      lhs.assign(val, false);
    } else {
      this.value = val;      
    }
    return val;
  }
  
  public V get() {
    final Object raw = (lhs != null)
      ? lhs.getValue()
      : value;
    return (value instanceof Primitive)
      ? (V) (Object) Primitive.unwrap(value)
      : (V) raw;
  }

  public LHS getLHS() {
    return this.lhs;
  }

  public Class<? super V> getType() {
    return this.type;
  }

  public String getTypeDescriptor() {
    return this.typeDescriptor;
  }
  
  public Modifiers getModifiers() {
    return this.modifiers;
  }
  
  public String getName() {
    return this.name;
  }
  
  public boolean hasModifier(String name) {
    return this.modifiers != null && this.modifiers.hasModifier(name);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(76);
    
    if (lhs != null) sb.insert(0, "/* LHS */ ");
    
    if (typeDescriptor != null) sb.append(String.format(
      "/* type descriptor: %s */ ", quote(typeDescriptor)
    ));
    
    if (type != null) sb.append(ClassInfo.typeToName(type)).append(' ');
    
    sb.append(name);
    
    if (this.value == null) {
      sb.append(" /* uninit */;");
    } else if (this.value == Primitive.VOID) {
      sb.append(" /* void */;");
    } else if (this.value == Primitive.NULL) {
      sb.append(" = null;");
    } else {
      sb.append(" = ").append(debugDisplayValue(value)).append(';');
    }
    
    return sb.toString();
  }
}

