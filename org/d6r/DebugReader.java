package org.d6r;
import com.android.dex.Annotation;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import com.android.dex.ClassData;
import com.android.dex.ClassDef;
import com.android.dex.Code.CatchHandler;
import com.android.dex.Code.Try;
import com.android.dex.Code;
import com.android.dex.Dex.Section;
import com.android.dex.Dex;
import com.android.dex.DexException;
import com.android.dex.DexFormat;
import com.android.dex.EncodedValue;
import com.android.dex.EncodedValueCodec;
import com.android.dex.EncodedValueReader;
import com.android.dex.FieldId;
import com.android.dex.Leb128;
import com.android.dex.MethodId;
import com.android.dex.Mutf8;
import com.android.dex.ProtoId;
import com.android.dex.SizeOf;
import com.android.dex.TableOfContents;
import com.android.dex.TypeList;
import com.android.dex.util.ByteArrayByteInput;
import com.android.dex.util.ByteInput;
import com.android.dex.util.ByteOutput;
import com.android.dex.util.ExceptionWithContext;
import com.android.dex.util.FileUtils;
import com.android.dex.util.Unsigned;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute.ClassSignature;
import javassist.bytecode.SignatureAttribute.Type;
import javassist.bytecode.SignatureAttribute;
import org.d6r.DebugReader.DebugInfo;
import org.d6r.annotation.*;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getMethodId;
import static org.d6r.ClassInfo.getItem;
import static org.d6r.ClassInfo.getCode;


public class DebugReader {
  
  public static final int DBG_LINE_BASE = -4;
  public static final int DBG_END_SEQUENCE = 0;
  public static final int DBG_ADVANCE_PC = 1;
  public static final int DBG_ADVANCE_LINE = 2;
  public static final int DBG_START_LOCAL = 3;
  public static final int DBG_START_LOCAL_EXTENDED = 4;
  public static final int DBG_END_LOCAL = 5;
  public static final int DBG_RESTART_LOCAL = 6;
  public static final int DBG_SET_PROLOGUE_END = 7;
  public static final int DBG_SET_EPILOGUE_BEGIN = 8;
  public static final int DBG_SET_FILE = 9;
  public static final int DBG_FIRST_SPECIAL = 10;
  public static final int DBG_LINE_RANGE = 15;
  public static final String EMPTY_STR = "";
  
  @NonDumpable("cache of Dex strings()")
  public static         Map<Dex, String[]> strsCache 
          = new WeakHashMap<Dex, String[]>();
  
  @NonDumpable("cache of Dex typeIds()")
  public static         Map<Dex, Integer[]> tidsCache 
          = new WeakHashMap<Dex, Integer[]>();
  
  @NonDumpable("cache of Section objects")
  public static         Map<Dex, Section> secCache 
          = new WeakHashMap<>();
  
  @NonDumpable
  public Dex dex;
  @NonDumpable
  protected String[] strings; 
  @NonDumpable
  protected Integer[] typeIds;
  TableOfContents toc;
  Section in;
  ByteBuffer buf;
  public static boolean VERBOSE = false;
  boolean namesOnly;
  public static boolean DEFAULT_NAMES_ONLY = false; //true;
  
  public static ScopedSection openAt(Dex dex, int offset) {
    Section sec = secCache.get(dex);
    if (sec == null) sec = dex.open(0);
    int origPos = sec.getPosition();
    return new ScopedSection(sec, offset);
  }
  
  public static DebugInfo getDebugInfo(Method mtd) {
    Code code = getCode(mtd);
    int dbgoffs = code.getDebugInfoOffset();
    Dex dex = getDex(mtd.getDeclaringClass());
    Section dbgsec = dex.open(dbgoffs);
    DebugReader dbgrdr = new DebugReader(dex);
    DebugInfo dbginfo = dbgrdr.read(dbgsec);
    return dbginfo;
  }  
  
  public DebugReader(Dex dex, boolean namesOnly) {
    this.dex = dex;
    
    if (strsCache.containsKey(dex)) {
      this.strings = strsCache.get(dex);
    } else {
      this.strings = dex.strings().toArray(new String[0]);
      strsCache.put(dex, this.strings);
    }
    if (tidsCache.containsKey(dex)) {
      this.typeIds = tidsCache.get(dex);
    } else {
      this.typeIds = dex.typeIds().toArray(new Integer[0]);
      tidsCache.put(dex, this.typeIds);
    }
    
    // this.typeIds = CollectionUtil.toArray(dex.typeIds());
    this.toc = dex.getTableOfContents();
    this.namesOnly = namesOnly;
  }
  
  public DebugReader(Dex dex) {
    this(dex, DEFAULT_NAMES_ONLY);
  }
  
  
  public static class LocalVariable {
    
    public final int addrStart; // uleb128 address delta.
    public final int addrEnd; // uleb128 address delta.
    public final TIntList addrDiffs; // uleb128 register number.
    
    public final int lineStart;// sleb128 line delta.
    public final int lineEnd;// sleb128 line delta.
    public final TIntList lineDiffs; // uleb128 register number.
    
    public final int registerNum;
    public final int nameIndex; // uleb128p1 string index. *
    public final String name;
    public final int typeIndex; // uleb128p1 type index. *
    public final String typeName;
    public final int sigIndex; // uleb128p1 string index. *
    public final String sig;    
    public final DwarfOp op;
    public final Type csig;
    
    
    public 
    LocalVariable(int addrStart, int addrEnd, TIntList addrDiffs,
    int lineStart, int lineEnd, TIntList lineDiffs, DwarfOp op,
    int registerNum, int nameIndex, String name, 
    int typeIndex, String typeName, int sigIndex, String sig)
    {
      super();
      this.addrStart = addrStart;
      this.addrEnd = addrEnd;
      this.addrDiffs = addrDiffs;
      this.lineStart = lineStart;
      this.lineEnd = lineEnd;
      this.lineDiffs = lineDiffs;      
      this.registerNum = registerNum;
      this.nameIndex = nameIndex;
      this.name = name;
      this.typeIndex = typeIndex;
      this.typeName = typeName;
      this.sigIndex = sigIndex;
      this.sig = sig;
      this.op = op;
      Object $csig = null;
      try {
        $csig = 
          (sig != null && sig.length() > 0
            ? SignatureAttribute.toTypeSignature(sig)
            : (typeName != null && typeName.length() > 0
                ? SignatureAttribute.toTypeSignature(typeName)
                : null));
      } catch (BadBytecode ex) {
      }
      this.csig = (Type) $csig;
    }
    
    @Override    
    public String toString() {
      return Debug.ToString(this);
    }
  }
  
  public static enum DwarfOp {
    DBG_LINE_BASE(-4, "LineBase"),
    DBG_END_SEQUENCE(0, "EndSequence"),
    DBG_ADVANCE_PC(1, "AdvancePc"),
    DBG_ADVANCE_LINE(2, "AdvanceLine"),
    DBG_START_LOCAL(3, "StartLocal"),
    DBG_START_LOCAL_EXTENDED(4, "StartLocalExtended"),
    DBG_END_LOCAL(5, "EndLocal"),
    DBG_RESTART_LOCAL(6, "RestartLocal"),
    DBG_SET_PROLOGUE_END(7, "SetPrologueEnd"),
    DBG_SET_EPILOGUE_BEGIN(8, "SetEpilogueBegin"),
    DBG_SET_FILE(9, "SetFile"),
    DBG_FIRST_SPECIAL(10, "FirstSpecial"),
    DBG_LINE_RANGE(15, "LineRange");
    
    public static final Matcher $mchr 
      = Pattern.compile("([a-z])([A-Z])").matcher("");
    public static final Map<Integer,DwarfOp> $cache
              = new TreeMap<Integer,DwarfOp>();
    public final int opcode;
    public final String opName;
    
    private DwarfOp(int opcode, String opName) 
    {
      this.opcode = opcode;
      this.opName = opName;
    } 
    
    public String toString() {
      return String.format(
        "%s(%d) [0x%2x]", opName, opcode, opcode
      );
    }
    
    public String getName() {
      return opName;
    }
    
    public static DwarfOp valueOf(int opcode) {
      int len;
      Enum<?>[] en;
      Enum<DwarfOp>[] enums = Arrays.copyOf(
        (len = 
          (en =
            DwarfOp.class.getEnumConstants()
          ).length) != -1 ? en: en,
        len,
        Enum[].class
      );
      int i = -1;
      while (++i < len) {
        if (((DwarfOp)enums[i]).opcode != opcode) continue;
        return (DwarfOp) enums[i];       
      }
      DwarfOp op = $cache.get(Integer.valueOf(opcode));
      if (op == null) {
        op = Reflect.allocateInstance(DwarfOp.class);
        Reflect.setfldval(op, "opcode", opcode);
        String opName;
        Reflect.setfldval(
          op, 
          "opName", 
          (opName = 
            (opcode < 0)
              ? String.format("Unknown<%d>", opcode)
              : String.format("Unknown<%d>", opcode)
          )
        );
        String name = 
         $mchr.reset(opName).replaceAll("$1_$2").toUpperCase();
        Reflect.setfldval(op, "name", name);
        $cache.put(Integer.valueOf(opcode), op);
      }
      return op;
      /*throw new IllegalArgumentException(String.format(
        "No enum constant of type %s "
        + "corresponds to the integer value `%d' (0x%08x).",
        DwarfOp.class.getName(),
        paramOpcode, paramOpcode
      ));*/
    } // valueOf
  }
  
  public static class DebugInfo {
    public final int paramsSize;
    public final String[] paramNames;
    public final int addrStart;
    public final int addrEnd;
    public final int lineStart;
    public final int lineEnd;
    public final List<LocalVariable> locals;
    public final List<DwarfOp> ops;
    public final String sourceName;
    public final int startPos;
    public final int endPos;
    public final int initialPosition;
    
    public DebugInfo(int paramsSize, String[] paramNames, 
    int addrStart, int addrEnd, int lineStart, int lineEnd, 
    List<LocalVariable> locals, List<DwarfOp> ops, 
    String sourceName, 
    int startPos, int endPos, int initialPosition) 
    {
      this.paramsSize = paramsSize;
      this.paramNames = paramNames;
      this.addrStart = addrStart;
      this.addrEnd = addrEnd;
      this.lineStart = lineStart;
      this.lineEnd = lineEnd;
      this.locals = locals;
      this.ops = ops;
      this.sourceName = sourceName;
      this.startPos = startPos;
      this.endPos = endPos;
      this.initialPosition = initialPosition;    
      
      for (int i=0; i<paramNames.length; i++)  {
        if (paramNames[i] == null || paramNames[i].isEmpty()) {
          if (locals != null && i < locals.size()) {
            LocalVariable lv = locals.get(i);
            if (lv.name == null && i+1< locals.size()) {
              lv = locals.get(i+1);
            }
            if (lv.name != null) paramNames[i] = lv.name;
          }
        }
      }
      
    }
    
    public String toString() {
      return String.format(
        "%s (%3d locals) ["
        + "start = 0x%08x, length = 0x%04x, next = 0x%08x"
        + "] initial = 0x%08x",
        getClass().getSimpleName(),
        locals.size(),
        startPos, endPos - startPos, endPos,
        initialPosition
      );
    }
  }

  public DebugInfo read() {
    return read(null);
  }
  
  public void position(int pos) {
    buf = Reflect.<ByteBuffer>getfldval(in, "data");
    buf.position(pos);
  }
  
  public int position() {
    buf = Reflect.<ByteBuffer>getfldval(in, "data");
    return buf.position();
  }
  
  public int reset() {
    buf = Reflect.<ByteBuffer>getfldval(in, "data");
    int initialPosition 
      = Reflect.<Number>getfldval(in, "initialPosition").intValue();
    buf.position(initialPosition);
    return buf.position();
  }
  
  public int offset() {
    buf = Reflect.<ByteBuffer>getfldval(in, "data");
    int initialPosition 
      = Reflect.<Number>getfldval(in, "initialPosition").intValue();
    int pos = buf.position();
    return pos - initialPosition;
  }
  
  public int remaining() {
    buf = Reflect.<ByteBuffer>getfldval(in, "data");
    return buf.remaining();
  }
  
  public ByteBuffer ByteBuffer() {
    return Reflect.<ByteBuffer>getfldval(in, "data");
  }
  
  public DebugInfo read(Section in) {
    if (in == null) {
      in = this.in;
      if (in == null) {     
        this.in = (in = dex.open(toc.debugInfos.off));
      }
    }
    
    //contentsOut.debugInfos.size++;
    int skipped = -1;
    int paramsSize;
    int startPos = in.getPosition();
    int endPos = 0;
    int initialPosition = Reflect.<Number>getfldval(
      in, "initialPosition"
    ).intValue();     
    int lineStart, addrStart = -256;
    
    do {
      if (++skipped > 0) {
        if (VERBOSE) System.err.printf(
          "Skipped %d 256-byte chunks...\n", skipped
        );
      }
      lineStart = in.readUleb128();
      addrStart += 128 * 2;
      //debugInfoOut.writeUleb128(lineStart);
      paramsSize = in.readUleb128();
    } while (paramsSize > 48 
         ||  paramsSize < 0 
         ||  lineStart < -2
         ||  lineStart > 100000);
    
    String[] paramNames = new String[paramsSize];    
    //debugInfoOut.writeUleb128(parametersSize);
    for (int p = 0; p < paramsSize; p++) {
      int paramNameIndex = in.readUleb128p1();
      String paramName = 
        paramNameIndex >= 0 && paramNameIndex < strings.length
          ? strings[paramNameIndex]
          : EMPTY_STR;
      paramNames[p] = paramName;
      /*debugInfoOut.writeUleb128p1(
        indexMap.adjustString(parameterName)
      );*/
    }
    
    
    int addr = addrStart; // uleb128 address delta.
    int vAddrStart = addrStart;
    
    int line = lineStart;
    int vLineStart = lineStart;
    
    TIntList addrDiffs = new TIntArrayList(12);
    TIntList lineDiffs = new TIntArrayList(12);
    
    DwarfOp op = null;
    DwarfOp vop = null;
    
    List<DwarfOp> opcodes = new ArrayList<DwarfOp>();
    
    int addrDiff = -1;
    // sleb128 line delta.
    int lineDiff = -1;
    int registerNum = -1; // uleb128 register number.
    // uleb128p1 string index. Needs indexMap adj
    int nameIndex = -1; 
    // uleb128p1 type index. Needs indexMap adj
    int typeIndex = -1;
    // uleb128p1 string index. Needs indexMap adj
    int sigIndex = -1;
    String name = EMPTY_STR;
    String typeName = EMPTY_STR;
    String sig = EMPTY_STR;
    boolean haveLocal = false;
    LocalVariable lv = null;
    List<LocalVariable> locals
      = new ArrayList<LocalVariable>(paramsSize * 3);
    DebugInfo result = null;
    int sourceNameIndex = -1;
    String sourceName = EMPTY_STR;
    int opcode = 0;
    int zeroes = 0;
    
    boolean end 
      = (namesOnly); // skip this part if we just wanted param names
    
    do {
      if (end) break;
      
      try {
        opcode = in.readByte();
        op = DwarfOp.valueOf(opcode);
        
        opcodes.add(op);
        if (VERBOSE) System.err.printf(
          "0x%08x + 0x%04x:  %s\n", 
          startPos, in.getPosition() - startPos, op
        );
        switch (opcode) {
  
          case DBG_END_SEQUENCE:
          case DBG_END_LOCAL:
          case DBG_RESTART_LOCAL:
            if (haveLocal) {
              lv = new LocalVariable(
                vAddrStart, addr, addrDiffs, 
                vLineStart, line, lineDiffs, vop, 
                registerNum, nameIndex, name, 
                typeIndex, typeName, sigIndex, sig
              );
              locals.add(lv);
              haveLocal = false;
            }
            if (opcode == DBG_END_SEQUENCE) {
              end = true;
              break;
            }
            // DBG_END_LOCAL or DBG_RESTART_LOCAL
            
            if (opcode == DBG_RESTART_LOCAL) {
              registerNum = in.readUleb128(); 
              haveLocal = true;
              addrDiffs.clear();
              lineDiffs.clear();
              vAddrStart = addr;
              vLineStart = line;
              vop = op;
            } else {
              nameIndex = -1;
              typeIndex = -1;
              sigIndex = -1;
            }
            break;
                      
          case DBG_ADVANCE_PC:
            addrDiff = in.readUleb128();
            addrDiffs.add(addrDiff);
            addr += addrDiff;
            break;
          
          case DBG_ADVANCE_LINE:
            lineDiff = in.readSleb128();
            lineDiffs.add(lineDiff);
            line += lineDiff;
            break;
          
          case DBG_START_LOCAL:
          case DBG_START_LOCAL_EXTENDED:
            if (haveLocal) locals.add(new LocalVariable(
                vAddrStart, addr, addrDiffs, 
                vLineStart, line, lineDiffs, vop, 
                registerNum, nameIndex, name, 
                typeIndex, typeName, sigIndex, sig
            ));            
            vop = op;
            haveLocal = true;
            registerNum = in.readUleb128();
            nameIndex = in.readUleb128p1();
            name =             
              nameIndex >= 0 && nameIndex < strings.length
                ? strings[nameIndex]
                : EMPTY_STR;
            typeIndex = in.readUleb128p1();
            typeName =
              typeIndex >= 0 && typeIndex < typeIds.length
                ? strings[ typeIds[typeIndex].intValue() ]
                : EMPTY_STR;
            addrDiffs.clear();
            lineDiffs.clear();
            vAddrStart = addr;
            vLineStart = line;            
            if (opcode == DBG_START_LOCAL_EXTENDED) {
              sigIndex = in.readUleb128p1();
              if (sigIndex >= 0 && sigIndex < strings.length) {
                sig = strings[sigIndex];
              } else {
                sig = EMPTY_STR;
              }
            } else {
              sigIndex = -1;
              sig = EMPTY_STR;
            }            
            break;
          
          case DBG_SET_FILE:
            sourceNameIndex = in.readUleb128p1();
            sourceName = strings[sourceNameIndex];
            break;
            
          case DBG_SET_PROLOGUE_END:
          case DBG_SET_EPILOGUE_BEGIN:
            break;
          default:
            
            break;            
        } // switch
        
      } catch (Throwable e) {
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        end = true;
      } // try / catch
      
      if (end || haveLocal) {
        lv = new LocalVariable(
          vAddrStart, addr, addrDiffs, 
          vLineStart, line, lineDiffs, vop,
          registerNum, nameIndex, name,
          typeIndex, typeName, sigIndex, sig
        );
        haveLocal = false;
        locals.add(lv);
      }
      
      endPos = in.getPosition();      
    } while (! end);
    
    result = new DebugInfo(
      paramsSize, paramNames, 
      addrStart, addr, lineStart, line, 
      locals, opcodes, sourceName,
      startPos, endPos, initialPosition          
    );
    return result;
  }
  
}