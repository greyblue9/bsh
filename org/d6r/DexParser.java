package org.d6r;

import com.android.dex.ClassDef;
import static org.d6r.ClassInfo.getCode;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;

import org.d6r.JavaDoc.DexCache;
import static org.d6r.JavaDoc.getDexCache;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dex.Dex.Section;
import com.android.dex.TableOfContents;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import libcore.reflect.AnnotationAccess;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Ordering;
import com.strobel.assembler.ir.attributes.LineNumberTableAttribute;
import com.strobel.assembler.ir.attributes.LineNumberTableEntry;
import java.lang.Integer;
import java.lang.Iterable;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class DexParser {
  public static boolean VERBOSE = false;
  public static boolean JRE = CollectionUtil.isJRE();
  
  public static String TAG = DexParser.class.getName();
  
  public static final int DBG_FIRST_SPECIAL = 0x0a;
  public static final int DBG_LAST_SPECIAL = 0xff;
  public static final int DBG_LINE_BASE = -4; // the smallest line number increment
  public static final int DBG_LINE_RANGE = 15;
          
  public static final Map<Dex,String[]> dex_string_cache = new IdentityHashMap<>();
  public static final Constructor<String> STRING_CTOR;
  static {
    Constructor<?> ctor = null;
    try {
      (ctor = String.class.getDeclaredConstructor(
        Integer.TYPE, Integer.TYPE, char[].class
      )).setAccessible(true);      
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
    } finally {
      STRING_CTOR = (Constructor<String>) ctor;
    }
  }
  
  public static class LineNumberTable 
    implements Iterable<Map.Entry<Integer, Integer>>
  {
    public SortedMap<Integer, Integer> entries = new TreeMap<>();
    
    public LineNumberTable() {
    }
    
    public LineNumberTable add(int offset, int lineNumber) {
      entries.put(Integer.valueOf(offset), Integer.valueOf(lineNumber));
      return this;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName())
        .append("{ ");
      int index = -1;
      for (final Map.Entry<Integer, Integer> e: entries.entrySet()) {
        if (++index != 0) sb.append(",  ");
        int offset = e.getKey();
        int lineNumber = e.getValue();
        sb.append(offset).append(" -> ").append(lineNumber);        
      }
      return sb.append(" }").toString();
    }
    
    public LineNumberTableAttribute toLineNumberAttribute() {
      LineNumberTableEntry[] entryArray = new LineNumberTableEntry[entries.size()];
      int index = -1;
      for (final Map.Entry<Integer, Integer> e: entries.entrySet()) {
        int offset = e.getKey();
        int lineNumber = e.getValue();
        entryArray[++index] = new LineNumberTableEntry(offset, lineNumber);
      }
      return new LineNumberTableAttribute(entryArray);
    }
    
    @Override
    public Iterator<Map.Entry<Integer, Integer>> iterator() {
      return (Iterator<Map.Entry<Integer, Integer>>) (Iterator<?>)
        entries.entrySet().iterator();
    }
    
    public SortedMap<Integer, Integer> entries() {
      return entries;
    }
    
    public ImmutableBiMap<Integer, Integer> entriesByLineNumber() {
      return (ImmutableBiMap<Integer,Integer>) (Object) ImmutableBiMap.builder()
        .putAll(entries.entrySet())
        .orderEntriesByValue(
          (Comparator<? super Object>) (Object) Ordering.natural()
        )
        .build()
        .inverse();
    }
  }
  
  public static String[] getDexStrings(Dex dex) {
    String[] dexStrings = dex_string_cache.get(dex);
    if (dexStrings == null) dex_string_cache.put(
      // dex, (dexStrings = dex.strings().toArray(new String[0]))
      dex, (dexStrings = readStrings(dex))
    );
    return dexStrings;
  }
  
  public static String[] readStrings(Dex dex) {
    TableOfContents toc = dex.getTableOfContents();
    TableOfContents.Section tocsec = toc.stringIds;
    int off = tocsec.off, size = tocsec.size, count = size;
    Section s = dex.open(off);
    ByteBuffer buf = Reflect.getfldval(s, "data");
    String[] strings = new String[count];
    int i = -1;
    while (++i < count) strings[i] = readString(buf);
    return strings;
  }  
  
  
  public static String readString(final ByteBuffer buf) {
    int charDataOffset = buf.getInt();
    //System.err.printf("  - charDataOffset: %d\n", charDataOffset);
    int position = buf.position();
    //System.err.printf("  - old position: %d\n", position);
    int limit = buf.limit();
    //System.err.printf("  - old limit: %d\n", limit);
    buf.position(charDataOffset);
    //System.err.printf("  - moved to position: %d\n", buf.position());
    buf.limit(buf.capacity());
    //System.err.printf("  - updated limit to: %d\n", buf.limit());
    int length = readUleb128(buf);
    char[] chars = new char[length];
    String decoded = decodeMutf8(buf, chars, length);
    buf.position(position);
    buf.limit(limit);
    return decoded;
  }
  
  
  public static int readUleb128(final ByteBuffer buf) {
    int n = 0, byteCount = 0, n3;
    boolean hasMore;
    do {
      n3 = (0xFF & ((int) buf.get()));
      n |= ((n3 & 0x7F) << (byteCount * 7));
      ++byteCount;
    } while ((n3 & 0x80) == 0x80 && byteCount < 5);
    if ((n3 & 0x80) != 0x80) return n;
    
    throw new RuntimeException(
      "invalid LEB128 sequence - (hasMore && byteCount == 4)"
    );
  }
  
  
  public static String decodeMutf8(final ByteBuffer buf, final char[] chars,
  final int declaredLength) 
  {
    /* System.err.printf(
      "  - decodeMutf8( \n" +
      "      - buf: %s, \n" +
      "      - chars: %s {.length = %d}, \n" +
      "      - declaredLength: %d, \n" +
      "    ) \n",
      buf, ClassInfo.typeToName(chars.getClass().getName()),
      chars.length, declaredLength
    )  */
    int start = buf.position();
    int charCount = 0;
    char c;
    while ((c = (char)(0xFF & buf.get())) != '\0') {
      if (c < '\u0080') {
        chars[charCount++] = c;
      } else if ((c & '\u00e0') == '\u00c0') {
        int n = 0xFF & buf.get();
        if ((n & 0xC0) == 0x80) {
          chars[charCount++] = (char)((c & '\u001f') << 6 | (n & 0x3F));
        } else chars[charCount++] = '?'; // bad second byte
      } else {
        if ((c & '\u00f0') == '\u00e0') {
          int n2 = 0xFF & buf.get();
          int n3 = 0xFF & buf.get();
          if ((n2 & 0xC0) == 0x80) {
            if ((n3 & 0xC0) == 0x80) {
              chars[charCount++] = (char)(
                ((c & '\u000f') << 12) | ((n2 & 0x3F) << 6) | (n3 & 0x3F));
            } else chars[charCount++] = '?'; // bad third byte
          } else chars[charCount++] = '?'; // bad second byte
        } else chars[charCount++] = '?'; // bad first byte
      }
    };
    try {
      return (String) STRING_CTOR.newInstance(
        0, (declaredLength < charCount)? declaredLength: charCount, chars
      );
    } catch (ReflectiveOperationException e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static class DebugInfo {
    public String className;
    public String methodName;
    public String signature;
    public String sourceFileName;
    public Code code;
    public Dex dex;
    
    public List<String> parameterNames;
    public int[] last_var_name_idx;
    public int[] last_var_type_idx;
    public int[] last_var_sig_idx;
    public boolean[] in_scope;
    public int lineStart;
    public LineNumberTable lineNumberTable = new LineNumberTable();
  }
  
  public static class PossibleError extends RuntimeException {
    public final Object[] context;
    
    public PossibleError(String s, Object... context) {
      super(s);
      this.context = context;
    }
  }
  
  public static <T> T indexInto(T[] array, int index, T defaultValue, Object tag) {
    if (array == null) {
      Log.w(TAG, (tag != null)
        ? "indexInto(T[],index:%d,T:%s,Object): array == null in lookup for %s"
        : "indexInto(T[],index:%d,T:%s,tag:%s): array == null",
        index, defaultValue, tag
      );
      return defaultValue;
    }
    if (index == -1 || index < 0) return defaultValue;
    final int length = array.length;
    if (length > index) return array[index];
    final Class<T> tcls = (Class<T>)(Class<?>) array.getClass().getComponentType();
    final String typeName 
      = ClassInfo.getSimpleName(ClassInfo.typeToName(tcls.getName()));
    final int shortfall = (index - length) + 1;
    final CharSequence errorType = (shortfall == 1)? "Off-by-One": "Logic";
    final String message = String.format(
      "indexInto(array: %1$s[%2$d], index: %3$d, defaultValue: %4$s, tag: %5$s): "+
      "*** Likely %s Error ***: " +
      "The request for the %1$s at index [%3$d] can never be fulfilled. " +
      "To be possible, the provided array *must* contain AT LEAST %7$d elements; "+
      "the array actually passed in has only %2$d, a shortfall of %8$d.",
      typeName, length, index, Debug.ToString(defaultValue), Debug.ToString(tag),
      errorType, // %6$s:  possible error kind
      index + 1, // %7$d:  minimum required size for index
      shortfall  // %8$d:  number of elements by which array is too small
    );
    Log.e(TAG, message, new PossibleError(message, RealArrayMap.toMap(
      "array", array, "index", index, "defaultValue", "tag", tag
    )));
    return defaultValue;
  }
  
  
  
  public static DebugInfo parseDebugInfo(final Member mtd) {
    final Class<?> cls = mtd.getDeclaringClass();
    final Dex dex = getDex(cls);
    final DexCache c = getDexCache(dex);
    final String[] strings = c.s;
    final int numStrings = strings.length;
    
    final int methodIndex = org.d6r.JavaDoc.getDexMethodIndex(mtd);
    
    final int typeIndex 
      = dex.declaringClassIndexFromMethodIndex(methodIndex);
    final int classDefIndex = dex.findClassDefIndexFromTypeIndex(typeIndex);
    final int accessFlags = mtd.getModifiers();
    final boolean is_static = ((accessFlags & Modifier.STATIC) != 0);
    final String methodName 
      = strings[dex.nameIndexFromMethodIndex(methodIndex)];
    final String className 
      = strings[dex.descriptorIndexFromTypeIndex(typeIndex)];
    // final MethodId methodId = DebugReader.getMethodId(dex, methodIndex);
    final ClassDef classDef = SourceUtil.getClassDef(dex, classDefIndex);
    final Code code = getCode(mtd);
    if (code == null) return null;
    final int debugInfoOffset = code.getDebugInfoOffset();
    final Section s = dex.open(debugInfoOffset);
    
    final DebugInfo info = new DebugInfo();
    info.className = cls.getName();
    info.methodName = mtd.getName();
    String signature = null;
    if (!JRE) {
      try {
        signature = AnnotationAccess.getSignature((AnnotatedElement) mtd);
      } catch (Exception ex) {
        Log.w("DexParser", "parseDebugInfo(%s): %s", mtd, ex);
        signature = ProcyonUtil.getErasedSignature(mtd);
      }
    } else {
      signature = ProcyonUtil.getErasedSignature(mtd);
    }
    info.code = code;
    info.dex = dex;
    
    
    // parse debug_info_item header
    final int line_start = s.readUleb128();
    final int parameters_size = s.readUleb128();
    final int[] parameter_names = new int[parameters_size];
    final String[] names = new String[parameters_size];
    for (int i = 0; i < parameters_size; ++i) {
      final int string_idx = s.readUleb128p1();
      parameter_names[i] = string_idx;
      final String name = 
        (string_idx >= 0 && string_idx < numStrings && strings[string_idx] != null)
          ? strings[string_idx]
          : String.format("p%s", i);
      names[i] = name;
      if (VERBOSE) System.err.printf("parameter[%d] name: %d \"%s\"\n", i, string_idx, name);
    }
    info.parameterNames = Arrays.asList(names);
    info.lineStart = line_start;

    // DWARF-3 state machine "registers"
    int address = 0;
    int last_address;
    int line = line_start;
    int last_line;
    int source_file;
    boolean prologue_end = false;
    boolean epilogue_begin = false;
    // "last active variable name+type index" per register
    int[] last_var_name_idx = new int[255];
    int[] last_var_type_idx = new int[255];
    int[] last_var_sig_idx  = new int[255];
    boolean[] in_scope = new boolean[255];
    info.last_var_name_idx = last_var_name_idx;
    info.last_var_type_idx = last_var_type_idx;
    info.last_var_sig_idx = last_var_sig_idx;
    info.in_scope = in_scope;
    
    source_file = classDef.getSourceFileIndex();    
    String source_file_name
      = indexInto(strings, source_file, null, "source file name (initial)");
    info.sourceFileName = source_file_name;
    
    if (VERBOSE) System.err.printf(
      "START(\n" +
      "  address: %d\n" +
      "  line: %d\n" +
      "  source_file: %d \"%s\"\n" +
      "  is_static: %s\n" +
      ")\n",
      address, line, source_file, source_file_name, Boolean.valueOf(is_static)
    );
    
    final LineNumberTable table = info.lineNumberTable;
    table.add(address, line);
    
    int op = 0;
    int count = 0;
    do {
      last_address = address;
      last_line = line;
      op = ((int) s.readByte()) & 0x000000FF;
      if (op == 0) {
        if (VERBOSE) System.err.printf(
          "DBG_END_SEQUENCE\n"
        );
      } else if (op == 1) {
        int addr_diff = s.readUleb128();
        address += addr_diff;
        if (VERBOSE) System.err.printf(
          "DBG_ADVANCE_PC: address += %d --> %d\n", addr_diff, address
        );
        if (VERBOSE) System.err.printf("line -> %d, address -> %d\n", line, address);
        // table.add(address, line);
      } else if (op == 2) {
        int line_diff = s.readUleb128();
        line += line_diff;
        if (VERBOSE) System.err.printf(
          "DBG_ADVANCE_LINE: line += %d --> %d\n", line_diff, line
        );
        if (VERBOSE) System.err.printf("line -> %d, address -> %d\n", line, address);
        // table.add(address, line);
      } else if (op == 3) {
        int register_num = s.readUleb128();
        int name_idx = s.readUleb128p1();
        int type_idx = s.readUleb128p1();
        last_var_name_idx[register_num] = name_idx;
        last_var_type_idx[register_num] = type_idx;
        last_var_sig_idx [register_num] = 0;
        in_scope[register_num] = true;
        String name = indexInto(strings, name_idx, null, "LV name (start)");
        String type = indexInto(
          strings, dex.descriptorIndexFromTypeIndex(type_idx), null, "LV typename"
        );
        if (VERBOSE) System.err.printf(
          "DBG_START_LOCAL: r%d := (" +
            "register_num: %d, name_idx: %d \"%s\", type_idx: %d \"%s\"" +
          ")\n",
          register_num, register_num, name_idx, name, type_idx, type
        );
      } else if (op == 4) {
        int register_num = s.readUleb128();
        int name_idx = s.readUleb128p1();
        int type_idx = s.readUleb128p1();
        int sig_idx = s.readUleb128p1();
        last_var_name_idx[register_num] = name_idx;
        last_var_type_idx[register_num] = type_idx;
        last_var_sig_idx [register_num] = sig_idx;
        in_scope[register_num] = true;
        String name = indexInto(strings, name_idx, null, "LV name (start-ext)");
        String type = indexInto(
          strings, dex.descriptorIndexFromTypeIndex(type_idx), null, "LV typename"
        );
        String sig = indexInto(strings, sig_idx, null, "LV signature");
        if (VERBOSE) System.err.printf(
          "DBG_START_LOCAL_EXTENDED: r%d := (" +
            "register_num: %d, name_idx: %d \"%s\", type_idx: %d \"%s\", " +
            "sig_idx: %d \"%s\", in_scope: %s" +
          ")\n",
          register_num, register_num, name_idx, name, type_idx, type, sig_idx, sig,
          Boolean.valueOf(in_scope[register_num])
        );
      } else if (op == 5) {
        /** 0x05: DBG_END_LOCAL {
          * uleb128 register_num; // register that contained local             
          marks a currently-live local variable as out of scope at the current
          address
        } */
        int register_num = s.readUleb128();
        in_scope[register_num] = false;
        int name_idx = last_var_name_idx[register_num];
        int type_idx = last_var_type_idx[register_num];
        int sig_idx  = last_var_sig_idx [register_num];
        String name = indexInto(strings, name_idx, null, "LV name (end)");
        String type = indexInto(
          strings, dex.descriptorIndexFromTypeIndex(type_idx), null, "LV typename"
        );
        String sig = indexInto(strings, sig_idx, null, "LV signature");
        if (VERBOSE) System.err.printf(
          "DBG_END_LOCAL: r%d := (" +
            "register_num: %d, name_idx: %d \"%s\", type_idx: %d \"%s\", " +
            "sig_idx: %d \"%s\", in_scope: %s" +
          ")\n",
          register_num, register_num, name_idx, name, type_idx, type, sig_idx, sig,
          Boolean.valueOf(in_scope[register_num])
        );
      } else if (op == 6) {
        /** 0x06: DBG_RESTART_LOCAL {
          * uleb128 register_num; // register to restart
          re-introduces a local variable at the current address. The name and type
          are the same as the last local that was live in the specified register.
        } */
        int register_num = s.readUleb128();
        in_scope[register_num] = true;
        int name_idx = last_var_name_idx[register_num];
        int type_idx = last_var_type_idx[register_num];
        int sig_idx  = last_var_sig_idx [register_num];
        String name = indexInto(strings, name_idx, null, "LV name (restart)");
        String type = indexInto(
          strings, dex.descriptorIndexFromTypeIndex(type_idx), null, "LV typename"
        );
        String sig = indexInto(strings, sig_idx, null, "LV signature");
        if (VERBOSE) System.err.printf(
          "DBG_RESTART_LOCAL: r%d := (" +
            "register_num: %d, name_idx: %d \"%s\", type_idx: %d \"%s\", " +
            "sig_idx: %d \"%s\", in_scope: %s" +
          ")\n",
          register_num, register_num, name_idx, name, type_idx, type, sig_idx, sig,
          Boolean.valueOf(in_scope[register_num])
        );
      } else if (op == 7) {
        /** 0x07: DBG_SET_PROLOGUE_END {
          sets the prologue_end state machine register, indicating that the next
          position entry that is added should be considered the end of a method
          prologue (an appropriate place for a method breakpoint). The prologue_end
          register is cleared by any special (>= 0x0a) opcode.
        } */
        prologue_end = true;
        if (VERBOSE) System.err.printf(
          "DBG_SET_PROLOGUE_END: prologue_end --> %s\n",
          Boolean.valueOf(prologue_end)
        );
      } else if (op == 8) {
        /** 0x08: DBG_SET_EPILOGUE_BEGIN {
          sets the epilogue_begin state machine register, indicating that the next
          position entry that is added should be considered the beginning of a
          method epilogue (an appropriate place to suspend execution before method
          exit). The epilogue_begin register is cleared by any special (>= 0x0a)
          opcode.
        } */
        epilogue_begin = true;
        if (VERBOSE) System.err.printf(
          "DBG_SET_EPILOGUE_BEGIN: epilogue_begin --> %s\n",
          Boolean.valueOf(epilogue_begin)
        );
      } else if (op == 9) {
        /** 0x09: DBG_SET_FILE {
          * uleb128p1 name_idx; // string index of source file name
          NO_INDEX if indicates that all subsequent line number entries make 
          reference to this source file name, instead of the default name specified
          in code_item unknown
        } */
        int name_idx = s.readUleb128p1();
        source_file = name_idx;
        source_file_name = strings[source_file];
        if (VERBOSE) System.err.printf(
          "DBG_SET_FILE: source_file := %d \"%s\"\n",
          source_file, source_file_name
        );
      } else { // op >= 10 (0x0A)
        prologue_end = false;
        epilogue_begin = false;
        int adjusted_opcode = op - DBG_FIRST_SPECIAL;
        line += DBG_LINE_BASE + (adjusted_opcode % DBG_LINE_RANGE);
        address += (adjusted_opcode / DBG_LINE_RANGE);
        if (VERBOSE) System.err.printf("line -> %d, address -> %d\n", line, address);
        table.add(address, line);
      }
    } while (op != 0 && address >= last_address && ++count < 500);
    
    return info;
  }
  
}
  