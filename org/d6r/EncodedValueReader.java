package org.d6r;

import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dex.FieldId;
import com.android.dex.EncodedValue;
import com.android.dex.EncodedValueCodec;
import com.android.dex.Leb128;
import com.android.dex.util.ByteArrayByteInput;
import com.android.dex.util.ByteInput;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
Pull parser for encoded values.
*/
public class EncodedValueReader implements Iterator<Object> { 

  
  public interface Value<V> {
    V getValue();
  }
  public interface Index<I> {
    int getIndex();
  }
  
  public class EncodedString 
    implements Value<String>, Index<String>
  {
    int index;
    String value;
    public EncodedString(int index) {
      this.index = index;
      this.value = dex.strings().get(index);    
    }
    @Override public String toString() {
      return value;
    }
    @Override public String getValue() {
      return value;
    }
    @Override public int getIndex() {
      return index;
    }
  }
  
  public class EncodedType 
    implements Value<String>, Index<Type>
  {
    int typeIndex;
    int stringIndex;    
    String value;    
    public EncodedType(int typeIndex) {
      this.typeIndex = typeIndex;
      this.stringIndex = dex.typeIds().get(typeIndex);
      this.value = dex.strings().get(this.stringIndex);    
    }
    @Override public String toString() {
      return value;
    }
    @Override public String getValue() {
      return value;
    }
    @Override public int getIndex() {
      return typeIndex;
    }
  }
  
  public class EncodedField 
    implements Value<FieldId>, Index<FieldId>
  {
    int fieldIndex;
    FieldId fieldId;
    
    public EncodedField(int index) {
      this.fieldIndex = index;
      this.fieldId = dex.fieldIds().get(fieldIndex);
    }
    @Override public String toString() {
      return fieldId.toString();
    }
    @Override public FieldId getValue() {
      return fieldId;
    }
    @Override public int getIndex() {
      return fieldIndex;
    }
  }
  
  public class EncodedEnum
    implements Value<FieldId>, Index<FieldId>
  {
    int fieldIndex;
    FieldId fieldId;
    
    public EncodedEnum(int index) {
      this.fieldIndex = index;
      this.fieldId = dex.fieldIds().get(fieldIndex);
    }
    @Override public String toString() {
      return String.format("Enum<%s>", fieldId);
    }
    @Override public FieldId getValue() {
      return fieldId;
    }
    @Override public int getIndex() {
      return fieldIndex;
    }
  }
  
  public class EncodedMethod
    implements Value<MethodId>, Index<MethodId>
  {
    int methodIndex;
    MethodId methodId;
    
    public EncodedMethod(int index) {
      this.methodIndex = index;
      this.methodId = dex.methodIds().get(methodIndex);
    }
    @Override public String toString() {
      return methodId.toString();
    }
    @Override public MethodId getValue() {
      return methodId;
    }
    @Override public int getIndex() {
      return methodIndex;
    }
  }
  
  public enum EncodedNull {
    NULL;
  }
  

  
  public static final int ENCODED_BYTE = 0x00;
  public static final int ENCODED_SHORT = 0x02;
  public static final int ENCODED_CHAR = 0x03;
  public static final int ENCODED_INT = 0x04;
  public static final int ENCODED_LONG = 0x06;

  public static final int ENCODED_FLOAT = 0x10;
  public static final int ENCODED_DOUBLE = 0x11;
  public static final int ENCODED_STRING = 0x17;
  public static final int ENCODED_TYPE = 0x18;
  public static final int ENCODED_FIELD = 0x19;

  public static final int ENCODED_ENUM = 0x1b;
  public static final int ENCODED_METHOD = 0x1a;
  public static final int ENCODED_ARRAY = 0x1c;
  public static final int ENCODED_ANNOTATION = 0x1d;
  public static final int ENCODED_NULL = 0x1e;
  public static final int ENCODED_BOOLEAN = 0x1f;
  /**
  placeholder type if the type is not yet known 
  */
  public static final int MUST_READ = -1;
  
  public volatile Dex dex;
  public volatile ByteInput in;
  
  public int type = ENCODED_ANNOTATION;
  public int arg;
  
  /**
  from `readAnnotation()'
  */
  public int size = MUST_READ;
  public int annotationType = MUST_READ;
  public int annotationTypeNameIndex = MUST_READ;
  public String annotationTypeName = null;
  
  /**
  Creates a new encoded value reader whose only value is the
  specified known type. This is useful for encoded values without 
  a type prefix, such as class_def_item's encoded_array or
  annotation_item's encoded_annotation.
  */
  public EncodedValueReader(Dex dex, ByteInput in, int knownType) {
    this.dex = dex;
    this.in = in;
    this.type = knownType;
  }
  public EncodedValueReader(Dex dex, EncodedValue in, int knownType) {
    this(dex, in.asByteInput(), knownType);
  }
  
  public EncodedValueReader(Dex dex, ByteInput in) {
    this(dex, in, ENCODED_ANNOTATION);
  }
  public EncodedValueReader(Dex dex, EncodedValue in) {
    this(dex, in.asByteInput());
  }
  
  public EncodedValueReader(Dex dex, byte[] bytes) {
    this(dex, new ByteArrayByteInput(bytes));
  }
  public EncodedValueReader(Dex dex, byte[] bytes, int knownType) {
    this(dex, new ByteArrayByteInput(bytes), knownType);
  }
  
  public static ByteInput newByteInput(byte[] bytes) {
    ByteInput bi = new ByteArrayByteInput(bytes);
    return bi;
  }
  public static ByteInput newByteInput(byte[] bytes, int position) {
    ByteInput bi = newByteInput(bytes);
    Reflect.setfldval(bi, "position", Integer.valueOf(position));
    return bi;
  }
  
  public static class StateBag<T> {
    public final Map<Field, Object> values
       = new HashMap<Field, Object>();
    public byte[] dataBytes;
    public int    dataPosition;
    public Class<T> savedClass;
    
    public StateBag(T o) {
      savedClass = (Class<T>) (Class<?>) o.getClass();
      Class<?> c = savedClass;
      do {
        for (Field fld: c.getDeclaredFields()) {
          int acc = fld.getModifiers();
          if ((acc & Modifier.STATIC) != 0) continue; 
          
          Class<?> declType = fld.getType();
          Object val = null;
          if (ByteInput.class.isAssignableFrom(declType)) {
            ByteInput bi = Reflect.getfldval(o, fld.getName());
            this.dataBytes = Reflect.getfldval(bi, "bytes");
            this.dataPosition = Reflect.getfldval(bi, "position");
            val = newByteInput(this.dataBytes, this.dataPosition);
          } else {
          
            if ((acc & Modifier.VOLATILE) != 0) continue; 
            try {
              fld.setAccessible(true);
              val = fld.get(o);
            } catch (ReflectiveOperationException ex) {
              if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
              continue;
            }
          }
          values.put(fld, val);
        }
        c = c.getSuperclass();
      } while (c != null);
    }
    public T restore(T target) {
      for (Map.Entry<Field, Object> entry: values.entrySet()) {
        Field fld = entry.getKey();
        Object val = entry.getValue();
        try {
          fld.setAccessible(true);
          fld.set(target, val);
        } catch (ReflectiveOperationException ex) {
          if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
          continue; 
        }
      }
      return target;
    }
  }
  
  @Override
  public String toString() {
    int typeIdIndex;
    int size;
    if (annotationType == -1) {
      StateBag<EncodedValueReader> state 
        = new StateBag<EncodedValueReader>(this);
      seekTo(0);
      size = readAnnotation();
      typeIdIndex = this.annotationType;
      state.restore(this);
    } else {
      typeIdIndex = this.annotationType;
      size = this.size;
    }
    int stringIndex = dex.typeIds().get(typeIdIndex);
    String typeName = dex.strings().get(stringIndex);
    return String.format(
      "@%s[%d items]: { position = %d, length = %d, hasNext = %s }",
      DexVisitor.typeToName(typeName),
      size,
      position(), length(), Boolean.valueOf(hasNext()).toString()
    );
  }
  
  public byte[] getBytes() {
    return Reflect.<byte[]>getfldval(in, "bytes");
  }
  
  public int position() {
    return Reflect.<Integer>getfldval(in, "position").intValue();
  }
  
  public EncodedValueReader seekTo(int newPosition) {
    if (! checkBounds(newPosition)) {
      int len = length();
      throw new IndexOutOfBoundsException(String.format(
        "Illegal seek: length = %d [0, %d]; requested index = %d",
        len, len - 1, newPosition
      ));
    }
    
    Reflect.setfldval(in, "position", Integer.valueOf(newPosition));
    return this;
  }
  
  public boolean checkBounds(int requestedIndex) {
    int len = length();
    return 0 <= requestedIndex && requestedIndex < len;
  }
  
  public int length() {
    return getBytes().length;
  }
  
  public int remaining() {
    return length() - position();    
  }
  
  @Override
  public boolean hasNext() {
    return remaining() > 0;
  }
  
  @Override
  public void remove() {
    Object o = new Object(){};
    Method thisMethod = o.getClass().getEnclosingMethod();
    throw new UnsupportedOperationException(String.format(
      "%s %s.%s(%s) not supported",
      thisMethod.getGenericReturnType(),
      thisMethod.getDeclaringClass().getName(),
      thisMethod.getName(),
      StringUtils.join(thisMethod.getGenericParameterTypes(), ", ")
    ));
  }
  
  /**
  Returns the type of the next value to read.
  */
  public int peek() {
    if (type == MUST_READ) {
      int argAndType = in.readByte() & 0xff;
      type = argAndType & 0x1f;
      arg = (argAndType & 0xe0) >> 5;
    }
    return type;
  }

  /**
  Begins reading the elements of an array, returning the array's 
  size. The caller must follow up by calling a read method for 
  each element in the array. 
  
  For example, this reads a byte array: 
  
    int arraySize = readArray();
    for (int i = 0, i < arraySize; i++) {
    readByte();
    }
  
  */
  public int readArray() {
    checkType(ENCODED_ARRAY);
    type = MUST_READ;
    return Leb128.readUnsignedLeb128(in);
  }

  /**
  Begins reading the fields of an annotation, returning the number
  of fields. The caller must follow up by making alternating calls
  to `readAnnotationName()' and another read method.
  
  For example, this reads an annotation whose fields are all 
  bytes:
  
    int fieldCount = readAnnotation();
    int annotationType = getAnnotationType();
    for (int i = 0; i < fieldCount; i++) {
      readAnnotationName();
      readByte();
    }
  
  */
  public int readAnnotation() {
    checkType(ENCODED_ANNOTATION);
    type = MUST_READ;
    annotationType = Leb128.readUnsignedLeb128(in);
    size = Leb128.readUnsignedLeb128(in);
    annotationTypeNameIndex = dex.typeIds().get(annotationType);
    annotationTypeName = dex.strings().get(annotationTypeNameIndex);
    return size;
  }
  
  /**
  Returns the type of the annotation just returned by
  `readAnnotation()'.
  
  This method's value is undefined unless the most recent call was
  to `readAnnotation()'.
  */
  public int getAnnotationType() {
    return annotationType;
  }

  public int readAnnotationName() {
    return Leb128.readUnsignedLeb128(in);
  }

  public byte readByte() {
    checkType(ENCODED_BYTE);
    type = MUST_READ;
    return (byte) EncodedValueCodec.readSignedInt(in, arg);
  }

  public short readShort() {
    checkType(ENCODED_SHORT);
    type = MUST_READ;
    return (short) EncodedValueCodec.readSignedInt(in, arg);
  }

  public char readChar() {
    checkType(ENCODED_CHAR);
    type = MUST_READ;
    return (char) EncodedValueCodec.readUnsignedInt(
      in, arg, false
    );
  }

  public int readInt() {
    checkType(ENCODED_INT);
    type = MUST_READ;
    return EncodedValueCodec.readSignedInt(in, arg);
  }

  public long readLong() {
    checkType(ENCODED_LONG);
    type = MUST_READ;
    return EncodedValueCodec.readSignedLong(in, arg);
  }

  public float readFloat() {
    checkType(ENCODED_FLOAT);
    type = MUST_READ;
    return Float.intBitsToFloat(
      EncodedValueCodec.readUnsignedInt(in, arg, true)
    );
  }

  public double readDouble() {
    checkType(ENCODED_DOUBLE);
    type = MUST_READ;
    return Double.longBitsToDouble(
      EncodedValueCodec.readUnsignedLong(in, arg, true)
    );
  }

  public int readString() {
    checkType(ENCODED_STRING);
    type = MUST_READ;
    return EncodedValueCodec.readUnsignedInt(in, arg, false);
  }

  public int readType() {
    checkType(ENCODED_TYPE);
    type = MUST_READ;
    return EncodedValueCodec.readUnsignedInt(in, arg, false);
  }

  public int readField() {
    checkType(ENCODED_FIELD);
    type = MUST_READ;
    return EncodedValueCodec.readUnsignedInt(in, arg, false);
  }

  public int readEnum() {
    checkType(ENCODED_ENUM);
    type = MUST_READ;
    return EncodedValueCodec.readUnsignedInt(in, arg, false);
  }

  public int readMethod() {
    checkType(ENCODED_METHOD);
    type = MUST_READ;
    return EncodedValueCodec.readUnsignedInt(in, arg, false);
  }

  public void readNull() {
    checkType(ENCODED_NULL);
    type = MUST_READ;
  }

  public boolean readBoolean() {
    checkType(ENCODED_BOOLEAN);
    type = MUST_READ;
    return arg != 0;
  }

  /**
  Skips a single value, including its nested values if it is an 
  array or annotation.
  */
  public void skipValue() {
    switch((peek())) {
      case ENCODED_BYTE:
        readByte();
        break;
      case ENCODED_SHORT:
        readShort();
        break;
      case ENCODED_CHAR:
        readChar();
        break;
      case ENCODED_INT:
        readInt();
        break;
      case ENCODED_LONG:
        readLong();
        break;
      case ENCODED_FLOAT:
        readFloat();
        break;
      case ENCODED_DOUBLE:
        readDouble();
        break;
      case ENCODED_STRING:
        readString();
        break;
      case ENCODED_TYPE:
        readType();
        break;
      case ENCODED_FIELD:
        readField();
        break;
      case ENCODED_ENUM:
        readEnum();
        break;
      case ENCODED_METHOD:
        readMethod();
        break;
      case ENCODED_ARRAY:
        for (int i = 0, size = readArray(); i < size; i++) {
          skipValue();
        }
        break;
      case ENCODED_ANNOTATION:
        for (int i = 0, size = readAnnotation(); i < size; i++) {
          readAnnotationName();
          skipValue();
        }
        break;
      case ENCODED_NULL:
        readNull();
        break;
      case ENCODED_BOOLEAN:
        readBoolean();
        break;
      default:
    }
  }


  /**
  Skips a single value, including its nested values if it is an 
  array or annotation.
  */
  @Override
  public Object next() {
    int type = peek();
    
    switch (type) {
      case ENCODED_BYTE:    return Byte.valueOf(readByte());
      case ENCODED_SHORT:   return Short.valueOf(readShort());
      case ENCODED_CHAR:    return Character.valueOf(readChar());
      case ENCODED_INT:     return Integer.valueOf(readInt());
      case ENCODED_LONG:    return Long.valueOf(readLong());
      case ENCODED_FLOAT:   return Float.valueOf(readFloat());
      case ENCODED_DOUBLE:  return Double.valueOf(readDouble());
      case ENCODED_STRING:  return new EncodedString(readString());
      case ENCODED_TYPE:    return new EncodedType(readType());
      case ENCODED_FIELD:   return new EncodedField(readField());
      case ENCODED_ENUM:    return new EncodedEnum(readEnum());
      case ENCODED_METHOD:  return new EncodedMethod(readMethod());
      case ENCODED_ARRAY: {
        int size = readArray();
        List<Object> items = new ArrayList<Object>(size);
        for (int i=0; i<size; i++) {
          items.add(next());
        }
        return items;
      }
      case ENCODED_ANNOTATION: {
        int size = readAnnotation();
        List<Pair<String, Object>> pairs
          = new ArrayList<Pair<String, Object>>(size);
        for (int i=0; i<size; i++) {
          EncodedString name = new EncodedString(readAnnotationName());
          Object value = next();
          pairs.add(Pair.<String,Object>of(name.getValue(), value));
        }
        return pairs;
      }
      case ENCODED_NULL:    return EncodedNull.NULL;
      case ENCODED_BOOLEAN: return Boolean.valueOf(readBoolean());
      default:
        System.err.printf("Unhandled type: %d (0x%02x)\n", type, type);
        return Integer.valueOf(type);
    }
  }

  public void checkType(int expected) {
    if (peek() != expected) {
      throw new IllegalStateException(
        String.format(
          "Expected %x but was %x", expected, peek()
        )
      );
    }
  }
  
}

