package org.d6r.xstream;

import com.thoughtworks.xstream.converters.reflection.*;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import java.util.LinkedList;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.lang.reflect.*;
import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import java.util.regex.*;
import org.d6r.LazyMember;


public class FieldDict
     extends FieldDictionary
{
  /*
  private static final DictionaryEntry OBJECT_DICTIONARY_ENTRY;
  private transient Map dictionaryEntries;
  private final FieldKeySorter sorter;
  */
  
  static {
    //OBJECT_DICTIONARY_ENTRY = new DictionaryEntry(Collections.EMPTY_MAP, Collections.EMPTY_MAP);
  }
  
  public FieldDict() {
    super(new ImmutableFieldKeySorter());
  }
  
  public FieldDict(final FieldKeySorter sorter) {
    super(sorter);
  }
  
  @Override
  public Iterator serializableFieldsFor(final Class cls) {
    return this.fieldsFor(cls);
  }
  
  
  public static LazyMember<Method> BUILDMAP = LazyMember.of(
    "com.thoughtworks.xstream.converters.reflection.FieldDictionary",
    "buildMap", Class.class, Boolean.TYPE
  );
  
  final Matcher nameMchr = Pattern.compile(
      "dexElements|pathList|zipFile|cache|classLoader",
      Pattern.DOTALL | Pattern.MULTILINE
        | Pattern.UNIX_LINES
    ).matcher("");
  final Matcher  typeMchr = Pattern.compile(
      "ZipFile|JarFile|DexPathList|bsh.(Block)?NameSpace|"
      + "TypeLoader",
      Pattern.DOTALL | Pattern.MULTILINE
        | Pattern.UNIX_LINES
    ).matcher("");
  
  @Override
  public Iterator fieldsFor(final Class cls) {
    
    return IteratorUtils.filteredIterator(
     
        ((Map<Object, Field>) 
          BUILDMAP.invoke(this, cls, true)).values().iterator(),
        new Predicate<Field>() {
          @Override
          public boolean evaluate(Field f) {
            if (nameMchr.reset(f.getName()).find()) return false;
            if (typeMchr.reset(f.getType().getName()).find()) return false;
            return true;
          }
        }
    );
  }
  
  /*
  public Field field(final Class cls, final String name, 
  final Class definedIn)
  {
    final Field field = this.fieldOrNull(cls, name, definedIn);
    if (field == null) {
      throw new MissingFieldException(cls.getName(), name);
    }
    return field;
  }
  
  public Field fieldOrNull(final Class cls, final String name,
  final Class definedIn)
  {
    final Map fields = this.buildMap(cls, definedIn != null);
    final Field field = fields.get((definedIn != null)
      ? new FieldKey(name, definedIn, -1)
      : name);
    return field;
  }
  
  private <K> Map<K, Field> buildMap(final Class type, 
  final boolean tupleKeyed) 
  {
    Class cls = type;
    DictionaryEntry lastDictionaryEntry = null;
    final LinkedList<Class> superClasses = new LinkedList<Class>();
    while (lastDictionaryEntry == null) {
      if (Object.class.equals(cls) || cls == null) {
        lastDictionaryEntry = FieldDictionary.OBJECT_DICTIONARY_ENTRY;
      } else {
        lastDictionaryEntry = this.getDictionaryEntry(cls);
      }
      if (lastDictionaryEntry == null) {
        superClasses.addFirst(cls);
        cls = cls.getSuperclass();
      }
    }
    final Iterator<Class> iter = superClasses.iterator();
    while (iter.hasNext()) {
      cls = iter.next();
      DictionaryEntry newDictionaryEntry
        = this.buildDictionaryEntryForClass(cls, lastDictionaryEntry);
      synchronized (this) {
        final DictionaryEntry concurrentEntry = this.getDictionaryEntry(cls);
        if (concurrentEntry == null) {
          this.dictionaryEntries.put(cls, newDictionaryEntry);
        }
        else {
          newDictionaryEntry = concurrentEntry;
        }
      }
      lastDictionaryEntry = newDictionaryEntry;
    }
    return (Map<K, Field>) (Object) tupleKeyed 
      ? lastDictionaryEntry.getKeyedByFieldKey()
      : lastDictionaryEntry.getKeyedByFieldName();
  }
  
  private DictionaryEntry buildDictionaryEntryForClass(final Class cls, 
  final DictionaryEntry lastDictionaryEntry) 
  {
    final Map<String, Field> keyedByFieldName 
    = new HashMap<String, Field>(lastDictionaryEntry.getKeyedByFieldName());
    final Map<FieldKey, Field> keyedByFieldKey 
    = new OrderRetainingMap<FieldKey, Field>(
        lastDictionaryEntry.getKeyedByFieldKey()
      );
    final Field[] fields = cls.getDeclaredFields();
    if (JVM.reverseFieldDefinition()) {
      int i = fields.length >> 1;
      while (i-- > 0) {
        final int idx = fields.length-i-1;
        final Field field = fields[i];
        fields[i] = fields[idx];
        fields[idx] = field;
      }
    }
    for (int i = 0; i < fields.length; ++i) {
      final Field field2 = fields[i];
      if (!field2.isAccessible()) {
        field2.setAccessible(true);
      }
      final FieldKey fieldKey 
        = new FieldKey(field2.getName(), field2.getDeclaringClass(), i);
      final Field existent = keyedByFieldName.get(field2.getName());
      if (existent == null 
      || (existent.getModifiers() & 0x8) != 0x0 
      || (existent != null 
      && (field2.getModifiers() & 0x8) == 0x0)) 
      {
        keyedByFieldName.put(field2.getName(), field2);
      }
      keyedByFieldKey.put(fieldKey, field2);
    }
    final Map<FieldKey, Field> sortedFieldKeys 
      = this.sorter.sort(cls, keyedByFieldKey);
    return new DictionaryEntry(keyedByFieldName, sortedFieldKeys);
  }
  
  private synchronized DictionaryEntry getDictionaryEntry(final Class cls)
  {
    return this.dictionaryEntries.get(cls);
  }
  
  @Override
  public synchronized void flushCache() {
    this.dictionaryEntries.clear();
    if (this.sorter instanceof Caching) {
      ((Caching) this.sorter).flushCache();
    }
  }
  
  protected Object readResolve() {
    this.init();
    return this;
  }
  
  private static final class DictionaryEntry {
    
    private final Map<String, Field> keyedByFieldName;
    private final Map<FieldKey, Field> keyedByFieldKey;
    
    public DictionaryEntry(final Map<String, Field> keyedByFieldName,
    final Map<FieldKey, Field> keyedByFieldKey) 
    {
      this.keyedByFieldName = keyedByFieldName;
      this.keyedByFieldKey = keyedByFieldKey;
    }
    
    public Map<String, Field> getKeyedByFieldName() {
      return this.keyedByFieldName;
    }
    
    public Map<FieldKey, Field> getKeyedByFieldKey() {
      return this.keyedByFieldKey;
    }
  }*/
}