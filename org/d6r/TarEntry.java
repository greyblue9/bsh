package org.d6r;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.apache.commons.io.HexDump;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.map.ListOrderedMap;
//import com.google.common.collect.TreeMultimap;



class TarEntry extends HashMap<String, Object>
  implements Comparable<TarEntry> {
  
  TarEntry extendedEntry;
  Pair<String, String> extendedKind;
  Boolean hasExtendedKind = null;
  
  @Override 
  public int compareTo(TarEntry other) {
    if (other == null) return 1;
    return this.toString().compareTo(other.toString());
  }
  
  @Override 
  public boolean equals(Object other) {
    if (!(other instanceof  TarEntry)) return false;
    return this.toString().equals(
      ((TarEntry) other).toString()
    );
  }
  
  public String getName() {
    return tryGetString("name", "???");
  }
  
  int alignmentBoundary = 4;

  public int getHeaderPadCount() {
    if (this.alignmentBoundary == 0) { return 0; }
    int size = 110 + 1;  // Name has terminating null
    String name = (String) get("name");
    if (name != null) {
      size += name.length();
    }
    int remain = size % this.alignmentBoundary;
    if (remain > 0) {
      return this.alignmentBoundary - remain;
    }
    return 0;
  }

  /**
   * Get the number of bytes needed to pad the data to the
     alignment boundary.
   *
   * @return the number of bytes needed to pad the data
     (0,1,2,3)
   */
  public int getDataPadCount() {
    if (this.alignmentBoundary == 0) { return 0; }
    long size = ((Long) get("size")).longValue();
    int remain = (int) (size % this.alignmentBoundary);
    if (remain > 0) {
      return this.alignmentBoundary - remain;
    }
    return 0;
  }
  
  public static void parseExtended(TarEntry te, TarEntry ext)
  { 
    if (ext == null) return; 
    String txt = ext.getContent(); 
    String[] lines = txt.split("\n"); 
    for (String line: lines) { 
      int sppos = line.indexOf(' '); 
      int eqpos = line.indexOf('='); 
      if (sppos == -1 || eqpos == -1) {
        continue;
      }
      String attname = line.substring(sppos+1, eqpos); 
      String attval = line.substring(eqpos+1); 
      /*System.err.printf(
        "attribute: [%s] = \"%s\"\n", attname, attval
      ); */
      if (attname.indexOf("time") != -1) {
        //System.err.println("  - Parsing as time");
        Date date = null;
        try {
          long longval 
            = Long.valueOf(attval, 10).longValue() * 1000L;
          //System.err.printf("  - longval = %s\n", longval);
          date = new Date(longval);
          //System.err.printf("  - date = %s\n", date);
          te.put(attname, date);
          continue; 
        } catch (Throwable e1) {
          //System.err.println(e1);
          try {
            double dval 
              = Double.valueOf(attval).doubleValue();
            //System.err.printf("  - dval = %s\n", dval);
            long lval = (long) (dval * 1000D);
            //System.err.printf("  - lval = %s\n", lval);
            date = new Date(lval);
            //System.err.printf("  - date = %s\n", date);
            te.put(attname, date);
            continue; 
          } catch (Throwable e2) { 
            System.err.println(e2);
          }
        } 
      }
      te.put(attname, attval);
    }
  }
  
  public static Date getDate(Object dateObj) {
    
    if (dateObj instanceof Date) return (Date) dateObj;
    
    if (dateObj instanceof byte[]) {
      dateObj = TarFile.stringFromBytes((byte[]) dateObj);
    }
    if (dateObj instanceof Integer) {
      dateObj = Long.valueOf((long)
        ((Integer) dateObj).intValue()
      );
    }
    if (dateObj instanceof String) {
      String strDate = (String) dateObj;
      try {
        dateObj = Long.valueOf(strDate, 10)
          .longValue() * 1000L;
      } catch (Throwable e1) { 
        try {
          dateObj = Long.valueOf((long)(
            Double.valueOf(strDate).doubleValue() * 1000D
          ));
        } catch (Throwable e2) { 
          //System.err.println(e2.toString());
          try {
            dateObj = new Date(strDate);
          } catch (Throwable e3) { 
            System.err.println(e3.toString());
            return TarEntry.BAD_DATE;
          } 
        }
      }
    }
    if (dateObj instanceof Double) {
      dateObj = Long.valueOf((long)(
        ((Double) dateObj).doubleValue() * 1000D
      ));
    }
    if (dateObj instanceof Long) {
      long longdate = ((Long) dateObj).longValue();
      if (longdate < 1400000) {
        return TarEntry.BAD_DATE;
      }
      while (Math.log10(longdate) < 10.5) {
        longdate *= 1000L;
      }
      while (Math.log10(longdate) > 13.5) {
        longdate /= 1000L;
      }
      dateObj = new Date(longdate);
    }
    if (dateObj instanceof Date) return (Date) dateObj;
    return TarEntry.BAD_DATE;
  }
  
  Date cachedDate;
  public Date lastModified() {
    if (cachedDate == null) {
      return (cachedDate = getDate(get("mtime")));
    }
    return cachedDate;
  }
  
  static final Date BAD_DATE = new Date(0L);
  
  
  Character cachedType = null;
  
  public char getType() {
    if (cachedType != null) return cachedType.charValue();

    String typeFlag = tryGetString("typeflag", "?");
    if (typeFlag.length() > 0 
    &&  typeFlag.charAt(0) <= (char) 0x0F) {
      typeFlag 
        = Integer.toHexString((int)typeFlag.charAt(0));
    }
    return (cachedType = Character.valueOf(
      typeFlag.length()>0? typeFlag.charAt(0): '?'
    )).charValue();
  }
  
  
  Triple<Character, String, String> cachedTypeInfo = null;
  
  public Triple<Character, String, String> getTypeInfo() {
    if (cachedTypeInfo != null) return cachedTypeInfo;

    Character typeChar = Character.valueOf(getType());
    Triple<Character, String, String> info 
      = TarFile.TYPEINFO_MAP.get(typeChar);
    if (info == null) {
      return Triple.of(typeChar, "UNRTYPE", String.format(
        "unrecognized type '%c' (0x%02x)", 
        typeChar, (int) typeChar.charValue()
      ));
    }
    return (cachedTypeInfo = info);
  }
  
  public static String getModeString(long mode) {
    StringBuilder sb = new StringBuilder(9); 
    for (int shift = 6; shift >=0; shift -=3) {
      sb.append(String.format(
        "%c%c%c",
        ((mode >>> shift) & 4) != 0 ? 'r': '-', 
        ((mode >>> shift) & 2) != 0 ? 'w': '-', 
        ((mode >>> shift) & 1) != 0 ? 'x': '-'
      ));       
    }
    String modeStr = sb.toString();
    return modeStr;
  }
  
  public long getMode() {
    Object val = get("mode");
    if (val instanceof Long) {
      return ((Long) val).longValue();
    }
    return -1;
  }
  
  String cachedToString;
  
  @Override 
  public String toString() {
    if (cachedToString != null) return cachedToString;

    Map<String, Object> e = this;
    String typeFlag = tryGetString("typeflag", "?");
    if (typeFlag.length()>0 
    &&  typeFlag.charAt(0) <=(char) 0x0F) {
      typeFlag 
        = Integer.toHexString((int)typeFlag.charAt(0));
    }
    if (typeFlag.equals("0")) {
      typeFlag = "f";
    }
    Date date = lastModified();
    String dateStr = (date == BAD_DATE)
      ? "Unknown Date": date.toLocaleString();
    
    long mode = getMode();
    return (cachedToString = String.format(
      "%s%-9s %5s/%-4d %10d %20s %s %s %s", 
      
      tryGetString("typeflag", "?"),
      mode != -1
        ? getModeString(mode)
        : String.format("[mode=%s]", e.get("mode")),
      tryGetString("uname", "?"),
      e.get("gid") instanceof Long?  (Long) e.get("gid"):  0,
      e.get("size") instanceof Long? (Long) e.get("size"): 0,
      dateStr,
      tryGetString("prefix", ""),
      tryGetString("name", "?"),
      tryGetString("linkname", "")
    ));
  }
  
  String tryGetString(String key, String defaultVal) {
    Object name = get(key);
    if (name instanceof String) return (String) name;
    if (name == null) return defaultVal;
    try {
      return name.toString();
    } catch (Throwable ex) { }
    return defaultVal;
  }
  
  <T> T getOrDefault(String key, T defaultVal) {
    if (defaultVal instanceof String) {
      return (T) tryGetString(key, (String) defaultVal);
    }
    Object val = get(key);
    if (val == null) return defaultVal;
    Class<T> cls = defaultVal != null
      ? (Class<T>) (Object) defaultVal.getClass()
      : (Class<T>) (Object) Object.class;
    if (cls.isInstance(val)) return (T) val;
    if (Number.class.isAssignableFrom(cls)
    && val instanceof Number) 
    {
     if (cls == Byte.class) 
       return (T) Byte.valueOf((byte) val);
     if (cls == Character.class) 
       return (T) Character.valueOf((char) val);
     if (cls == Short.class) 
       return (T) Short.valueOf((short) val);
     if (cls == Integer.class) 
       return (T) Integer.valueOf((int) val);
     if (cls == Long.class) 
       return (T) Long.valueOf((long) val);
     if (cls == Float.class) 
       return (T) Float.valueOf((float) val);
     if (cls == Double.class) 
       return (T) Double.valueOf((double) val);
    }
    return defaultVal;
  }
  
  public String getUser() {
    return getOrDefault(
      "uname", 
      getOrDefault("uid", "?")
    );
  }
  
  public int getUid() {
    return getOrDefault("uid", Integer.valueOf(-1))
      .intValue();
  }
  
  public int getGid() {
    return getOrDefault("gid", Integer.valueOf(-1))
      .intValue();
  }
  
  static String EMPTY_CONTENT = "";
  
  public String getContent() {
    Object content = get("content");
    if (content instanceof String) {
      return (String) content;
    }
    if (content instanceof byte[]) {
      return TarFile.stringFromBytes((byte[]) content);
    }
    return EMPTY_CONTENT;
  }
  
  public byte[] getBytes() {
    Object content = get("content");
    if (content instanceof String) {
      try {
        return ((String) content).getBytes(TarFile.ENCODING);
      } catch (UnsupportedEncodingException uee) {
        throw new RuntimeException(uee);
      }
    }
    if (content instanceof byte[]) {
      return (byte[]) content;
    }
    return new byte[0];
  }
    
  public Pair<String, String> getExtendedKind() {
    if (extendedKind != null) return extendedKind;
    if (hasExtendedKind == Boolean.FALSE) return null;
    
    String name = tryGetString("name", "");
    int fileNameSlashPos = name.lastIndexOf('/');
    if (fileNameSlashPos == -1) {
      hasExtendedKind = Boolean.FALSE;
      return null;
    }
    int immediateDirSlashPos
      = name.lastIndexOf('/', fileNameSlashPos-1);
    String immediateDirName = name.substring(
      immediateDirSlashPos + 1, fileNameSlashPos);
    if (immediateDirName.length() < 3) {
      hasExtendedKind = Boolean.FALSE;
      return null;
    }
    int immDirDotPos = immediateDirName.indexOf('.');
    if (immDirDotPos < 1) {
      hasExtendedKind = Boolean.FALSE;
      return null;
    }
    String extHeaderNs 
      = immediateDirName.substring(0, immDirDotPos);
    String extHeaderSuffix 
      = immediateDirName.substring(immDirDotPos + 1);
    hasExtendedKind = Boolean.TRUE;
    extendedKind = Pair.of(extHeaderNs, extHeaderSuffix);
    return extendedKind;
  }
  
}

