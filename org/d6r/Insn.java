package org.d6r;


import com.googlecode.dex2jar.DexType;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexLabel;

import java.lang.reflect.Modifier;
import sun.misc.Unsafe;
import java.util.Comparator;
import java8.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import javax.annotation.Nullable;

//import org.d6r.Insn.LHSKind;

public class Insn implements Comparable<Insn> {
  
  public final String clsName;
  public final Method method;
  public final int index;
  public final String name;
  public Object[] args;
  public String[] argNames;
  
  public int lineNumber;
  public int opcode;

  final String  defaultArgNamePrefix;
  final StringBuilder argNameBuilder;

  static final Object[] NO_ARGS      = new Object[0];
  static final String[] NO_ARG_NAMES = new String[0];  
  static final Comparator<Insn> OFFSET_COMPARATOR
    = new InsnOffsetComparator();
  
  static long DEX_LABEL_OFFSET_OFFSET;
  static Unsafe u;
  static {
    u = ClassPathUtil2.getUnsafe();
    try {
      DEX_LABEL_OFFSET_OFFSET 
        = u.objectFieldOffset(
            DexLabel.class.getDeclaredField("offset")
      );      
    } catch (Throwable e) { e.printStackTrace(); }
  }
  
  public static int getOffset(DexLabel label) {
    return u.getInt(label, DEX_LABEL_OFFSET_OFFSET);
  }
  
  /**
  Returns the first available offset if present; 
  or (Integer) null otherwise.
  */
  @Nullable
  public Integer getOffset() {
    /**
    DexLabel end
    DexLabel label
    DexLabel start
    DexLabel[] handlers
    DexLabel[] labels
    */
    final Optional<DexLabel> label = get("label");
    if (label.isPresent()) return getOffset(label.get());    
    final Optional<DexLabel> start = get("start");
    if (start.isPresent()) return getOffset(start.get());
    final Optional<DexLabel> end = get("end");
    if (end.isPresent()) return getOffset(end.get());
    
    final Optional<DexLabel[]> labels = get("labels");
    if (labels.isPresent()) {
      final DexLabel[] arrLabels = labels.get();
      if (arrLabels.length != 0) {
        return getOffset(arrLabels[0]);
      }
    }
    final Optional<DexLabel[]> handlers = get("handlers");
    if (handlers.isPresent()) {
      final DexLabel[] arrHandlers = handlers.get();
      if (arrHandlers.length != 0) {
        return getOffset(arrHandlers[0]);
      }
    }
    
    return (Integer) null;
  }
  
  @Override
  public int compareTo(final Insn other) {
    return OFFSET_COMPARATOR.compare(this, other);
  }
  
  public static enum LHSKind {
    Array(75),
    Local(68);
    private int value;
    private LHSKind(int value) {
      this.value = value;
    }
    public int getValue() {
      return value;
    }
  }
  public LHSKind lhsKind;

  public Insn(String clsName, Method method, int index,
  String name, Object... args) 
  {
    this.clsName = clsName;
    this.method = method;
    this.index = index;
    this.name = name;
    this.args = args;
    
    this.defaultArgNamePrefix = "arg";
    this.argNameBuilder = new StringBuilder(
      defaultArgNamePrefix.length() + 4
    );
  }
  
  public void setArgNames(String... argNames) {
    this.argNames = argNames;
  }
  
  public void setArgOps(Op... ops) {
    String[] argStrs = new String[ops.length];
    for (int i=0; i<ops.length; i+=1) {
       argStrs[i] = ops[i].getName();
    }
    this.argNames = argStrs;
  }
  
  static final String EMPTY_STRING = "";
  
  public void finish() {
    if (args == null) {
      args = NO_ARGS;
      argNames = NO_ARG_NAMES;
    } else if (argNames == null) {
      argNames = (args.length == 0)
        ? NO_ARG_NAMES
        : generateArgNames(args.length, defaultArgNamePrefix);
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(360);
    sb.append(name);
    if (args == null || argNames == null) {
      sb.append("<< unfinished >>");
      return sb.toString();
    }
    sb.append("{ ");
    int argIdx = -1;
    while (++argIdx != args.length) {
      if (argIdx != 0) sb.append(", ");
      String argName = argNames[argIdx];
      Object value = args[argIdx];
      sb.append(String.format(
        value instanceof String? "%s = \"%s\"": "%s = %s",
        argName, 
        value != null
          ? (value.getClass().isPrimitive()
              ? value: Debug.ToString(value))
          : "null"
      ));      
    }
    sb.append(" }");
    return sb.toString();
  }
  
  public String[] generateArgNames(final int numArgs, final String prefix) {
    int argIdx = -1, prefixLen = prefix.length();
    final String[] names = new String[numArgs];
    final StringBuilder sb = argNameBuilder.replace(
      0, argNameBuilder.length(), prefix
    );
    while (++argIdx != numArgs) {
      names[argIdx] = sb.replace(
        prefixLen, sb.length(), String.valueOf(argIdx)
      ).toString();       
    }
    return names;
  }
  
  public <V> Optional<V> get(String argName) {
    String[] argNames = this.argNames;
    int argIndex = ArrayUtils.indexOf(argNames, argName);
    Optional<Object> value = (argIndex >= 0 && argIndex < argNames.length)
      ? Optional.<Object>of(args[argIndex])
      : Optional.<Object>empty();
    return (Optional<V>) (Optional<?>) value;
  }
  
  public int getLineNumber() {
    return lineNumber >= 1? lineNumber: 1;
  }
}



class InsnOffsetComparator implements Comparator<Insn> { 

  public final boolean reverse;
  
  public InsnOffsetComparator() {
    this(false);
  }
  public InsnOffsetComparator(boolean reverse) {
    this.reverse = reverse;
  }
  
  public boolean isReverse() {
    return this.reverse;
  }
  
  
  @Override
  public int compare(final Insn a, final Insn b) {
    final Integer offsetA = a != null? a.getOffset(): null;
    final Integer offsetB = b != null? b.getOffset(): null;
    if (offsetA != null) {
      if (offsetB != null) {
        final int intA = offsetA.intValue(), intB = offsetB.intValue();
        return (reverse)
          ? ((intA < intB)? -1: (intA == intB)? 0: 1) ^ 0xFFFFFFFF + 1
          :  (intA < intB)? -1: (intA == intB)? 0: 1;
      } else {
        final int intA = offsetA.intValue();
        return (reverse)
          ? (intA >> 31 | -intA >>> 31) ^ 0xFFFFFFFF + 1
          :  intA >> 31 | -intA >>> 31;
      }
    } else {
      if (offsetB != null) {
        final int intB = offsetB.intValue();
        return (reverse)
          ? (-(intB >> 31 | -intB >>> 31)) ^ 0xFFFFFFFF + 1
          :  -(intB >> 31 | -intB >>> 31);
      } else {
        return 0;
      }
    }
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof InsnOffsetComparator)) return false;
    return ((InsnOffsetComparator)o).reverse == this.reverse;
  }
  
  
}
