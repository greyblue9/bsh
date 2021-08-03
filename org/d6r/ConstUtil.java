package org.d6r;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableBiMap;

public class ConstUtil {
  
  public static final int ACC_PUBLIC = 1;
  public static final int ACC_PRIVATE = 2;
  public static final int ACC_PROTECTED = 4;
  public static final int ACC_STATIC = 8;
  public static final int ACC_FINAL = 16;
  public static final int ACC_SYNCHRONIZED = 32;
  public static final int ACC_VOLATILE = 64;
  public static final int ACC_VARARGS = 128;
  public static final int ACC_NATIVE = 256;
  public static final int ACC_INTERFACE = 512;
  public static final int ACC_ABSTRACT = 1024;
  public static final int ACC_STRICT = 2048;
  public static final int ACC_SYNTHETIC = 4096;
  public static final int ACC_ANNOTATION = 8192;
  public static final int ACC_ENUM = 16384;
  public static final int ACC_CONST = (ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
  
  public static Set<String> constantNames(Class<?> cls, String prefix, long value) 
  {
    Set<String> names = new HashSet<String>();
    for (Field fld: cls.getDeclaredFields()) {
      if (! Modifier.isStatic(fld.getModifiers())) continue; //;
      Class<?> type = fld.getType();
      if (! ClassUtils.isPrimitiveOrWrapper(type)) continue;
      String name = fld.getName();
      if (! name.startsWith(prefix)) continue;
      
      Object val;
      try {
        fld.setAccessible(true);
        val = fld.get(null);
      } catch (IllegalAccessException iae) {
        if ("true".equals(System.getProperty("printStackTrace"))) iae.printStackTrace();
        continue;
      }
      long longVal = ((Number) val).longValue();
      if ((value & longVal) == 0) continue; 
      names.add(name);
    }
    return names;
  }  
  
  public static
  ImmutableBiMap<Integer, String>  constantMap(Class<?> cls, 
  long value) 
  {
    return constantMap(cls, null, value);
  }
  
  public static
  ImmutableBiMap<Integer, String> constantMap(Class<?> cls, 
  String prefix, long value) 
  {
    Map<Integer, String> map = new TreeMap<Integer, String>();
    for (Field fld: cls.getDeclaredFields()) {
      if (! Modifier.isStatic(fld.getModifiers())) continue; //;
      Class<?> type = fld.getType();
      if (! ClassUtils.isPrimitiveOrWrapper(type)) continue;
      String name = fld.getName();
      if (prefix != null && ! name.startsWith(prefix)) continue;
      
      Object val;
      try {
        fld.setAccessible(true);
        val = fld.get(null);
      } catch (IllegalAccessException iae) {
        if ("true".equals(System.getProperty("printStackTrace"))) iae.printStackTrace();
        continue;
      }
      long longVal = ((Number) val).longValue();
      if ((value & longVal) != longVal) continue; 
      map.put(Integer.valueOf((int) longVal), name);
    }
    return ImmutableBiMap.<Integer,String>builder().putAll(map).build();
  }  
  
  
	public static String getDebugString(final Class<?> cls,
	final String prefix, final long value)
	{
	 return String.format(
	   "<%s> (0x%x) (b'%s)", StringUtils.join(
	     ConstUtil.constantNames(cls, prefix, value), " | "
	   ), value, Long.toBinaryString(value)
	 );
	}
	
	
	
	
	public static boolean putConstValueOrSkip(final Field fld,
	  final Map<String, ?> constantValues)
	{
	  final Map<String, Object> constValues
	     = (Map<String, Object>) (Map<?, ?>) constantValues;
	  final int acc = fld.getModifiers();
	  if (((acc & ACC_CONST) ^ ACC_CONST) != 0) return false;
	  final Class<?> type, fldType = fld.getType();
	  if (fldType.isArray()) {
	    Class<?> cmpCls = fldType;
	    while ((cmpCls = cmpCls.getComponentType()).isArray()) ;
	    type = cmpCls;
	  } else {
	    type = fldType;
	  }
	  if (! (fldType == String.class || ClassUtils.isPrimitiveOrWrapper(type))) {
	    return false;
	  }
	  final String name = fld.getName();
    try {
      Object value = fld.get(null);
      if (value == null) return false;
      constValues.put(name, value);
      return true;
    } catch (ReflectiveOperationException roe) {
      roe.printStackTrace();
      return false;
    }
	}
	
  public static Map mapConsts(final Class<?> cls) {
    final Set<String> prefixes = new TreeSet<>();
    Map<String, Integer> constValues = new HashMap<>();
    
    for (final Field fld: cls.getDeclaredFields()) {
      if (!putConstValueOrSkip(fld, constValues)) continue;
    }
    
    int maxLen = 0;
    for (final String name: constValues.keySet()) {
      final int first = name.indexOf('_');
      if (first == -1) continue;
      
      final int last = name.lastIndexOf('_');
      final int len = name.length();
      final char firstChar = name.charAt(0);
      final String prefix;
      if (firstChar == 'O' || firstChar == 'o') {
        if (name.toLowerCase().startsWith("opc")) {
          if (prefixes.add(prefix = (String) name.subSequence(0, first))) {
            maxLen = Math.max(len, maxLen);
          }
          continue;
        }
      }
      final String afterLast  = (String) name.subSequence(last+1, len);
      final String beforeLast = (String) name.subSequence(0, last);
      prefix = (StringUtils.isNumeric(afterLast))
        ? StringUtils.substringBeforeLast(beforeLast, "_")
        : beforeLast;
      if (prefixes.add(prefix)) maxLen = Math.max(len, maxLen);
    }
    Map<String, Map<String, Integer>> map = new TreeMap<>();
    final StringBuilder sb = new StringBuilder(maxLen + 1);
    
    for (final String prefix: prefixes) {
      Map<String, Integer> consts = constantMap(
        cls,
        new StringBuilder(prefix.length()+1).append(prefix).append('_').toString(), 
        -1
      ).inverse();
      if (consts.size() < 2)
        continue;
      map.put(prefix, consts);
    }
    return map;
  }
  

	
}
