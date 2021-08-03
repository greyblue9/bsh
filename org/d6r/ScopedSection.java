package org.d6r;
import com.android.dex.Annotation;
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
import java.lang.reflect.*;
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
import org.apache.commons.lang3.StringUtils;
import org.d6r.DebugReader.DebugInfo;
import org.d6r.annotation.*;
import java.nio.ByteBuffer;

public class ScopedSection {
  public static boolean DEBUG;
  static final Object DEF_VALUE = new Object();
  
  Section sec;
  ByteBuffer data;
  int initialPosition;
  String name;
  
  int startOffset;
  int pos;

  
  protected <R> R invoke(final String name, final Object... args) {
    final Class<?>[] types = bsh.Types.getTypes(args);
    if (sec == null) throw new IllegalStateException("sec == null");
    Method method = null;
    Class<?> declaringClass = sec.getClass();
    do {
      try {
        method = declaringClass.getDeclaredMethod(name, types);
      } catch (final Throwable e) {
        if (DEBUG) {
          new RuntimeException(String.format(
            "%s: invoke(name: \"%s\", args: new Object[]{ %s }); with " +
            "declaringClass = %s.class and types = new Class<?>[] { %s }",
            e.getClass().getSimpleName(), name, StringUtils.join(args, ", "),
            ClassInfo.typeToName(declaringClass.getName()),
            StringUtils.join(types, ", ")
          ), e).printStackTrace();
        }
      }
    } while (
      method == null && declaringClass != null && Object.class != declaringClass);
    if (method == null) throw new AssertionError(String.format(
      "Method '%s' not found in %s!", name, sec
    ));
    try {
      method.setAccessible(true);
      final Object rs = method.invoke(sec, args);
      if (DEBUG) Log.d(
        "ScopedSection", "returning %s@%08x: %s",
        (rs != null) ? rs.getClass().getName(): null,
        System.identityHashCode(rs), rs
      );
      return (R) rs;
    } catch (final InvocationTargetException ite) {
      throw Reflector.Util.sneakyThrow(ite.getTargetException());
    } catch (final IllegalAccessException iae) {
      throw (AssertionError) new AssertionError().initCause(iae);
    }
  }
  
  public ScopedSection(Section section, int startOffset) {
    sec = section;
    data = Reflect.getfldval(sec, "data");
    initialPosition = Reflect.<Integer>getfldval(sec, "initialPosition").intValue();
    name = Reflect.getfldval(sec, "name");
    this.startOffset = startOffset;
    this.pos = startOffset;
  }
  
  public byte[] getBytesFrom(int start) {
    int origPos = sec.getPosition();
    try {
      byte[] result = invoke("getBytesFrom", start);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public CatchHandler readCatchHandler(int offset) {
    int origPos = sec.getPosition();
    try {
      CatchHandler result = invoke("readCatchHandler", offset);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public CatchHandler[] readCatchHandlers() {
    int origPos = sec.getPosition();    
    try {
      data.position(pos);
      CatchHandler[] result = invoke("readCatchHandlers");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public ClassData readClassData() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      ClassData result = invoke("readClassData");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public Code readCode() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int registersSize = sec.readUnsignedShort();
      int insSize = sec.readUnsignedShort();
      int outsSize = sec.readUnsignedShort();
      int triesSize = sec.readUnsignedShort();
      int debugInfoOffset = sec.readInt();
      int instructionsSize = sec.readInt();
      if (instructionsSize < 0) {
        Log.d("ScopedSection", "No code in method");
        return null;
      }
    } finally {
      data.position(origPos);
    }
    try {
      data.position(pos);
      Code result = invoke("readCode");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public ClassData.Field[] readFields(int count) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      ClassData.Field[] result
        = invoke("readFields", count);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public ClassData.Method[] readMethods(int count) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      ClassData.Method[] result
        = invoke("readMethods", count);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public Try[] readTries(int triesSize, CatchHandler[] catchHandlers) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      Try[] result 
        = invoke("readTries", triesSize, catchHandlers);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int getPosition() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = sec.getPosition();
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public Annotation readAnnotation() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      Annotation result = invoke("readAnnotation");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public byte readByte() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      byte result = invoke("readByte");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public byte[] readByteArray(int length) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      byte[] result = invoke("readByteArray", length);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public ClassDef readClassDef() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      ClassDef result = invoke("readClassDef");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public EncodedValue readEncodedArray() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      EncodedValue result = invoke("readEncodedArray");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public FieldId readFieldId() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      FieldId result = invoke("readFieldId");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int readInt() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = invoke("readInt");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public MethodId readMethodId() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      MethodId result = invoke("readMethodId");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public ProtoId readProtoId() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      ProtoId result = invoke("readProtoId");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public short readShort() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      short result = invoke("readShort");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public short[] readShortArray(int length) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      short[] result = invoke("readShortArray", length);
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int readSleb128() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = invoke("readSleb128");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public String readString() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      String result = invoke("readString");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public TypeList readTypeList() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      TypeList result = invoke("readTypeList");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int readUleb128() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = invoke("readUleb128");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int readUleb128p1() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = invoke("readUleb128p1");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public int readUnsignedShort() {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      int result = invoke("readUnsignedShort");
      return result;
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
  
  public void skip(int count) {
    int origPos = sec.getPosition();
    try {
      data.position(pos);
      sec.skip(count);
    } finally {
      pos = data.position();
      data.position(origPos);
    }
  }
   
}