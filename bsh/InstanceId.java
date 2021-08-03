package bsh;

import bsh.BSHBlock;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import org.apache.commons.lang3.mutable.MutableInt;

public class InstanceId {
  static WeakHashMap<Class<?>, MutableInt> next = new WeakHashMap();
  WeakReference<Class<?>> classRef;
  volatile BSHBlock block;
  int clsIndex;
  int id;

  public InstanceId(Class<?> cls, BSHBlock block) {
    this.classRef = new WeakReference(cls);
    this.block = block;
    this.clsIndex = getNextIndex(cls);
    this.id = getId(cls, this.clsIndex);
  }

  public static int getId(Class<?> cls, int clsIndex) {
    int clsHashCode = cls.hashCode();
    int id = clsHashCode ^ clsIndex;
    return id;
  }

  public static int getNextIndex(Class<?> cls) {
    int clsHashCode = cls.hashCode();
    MutableInt mi = (MutableInt)next.get(cls);
    if(mi == null) {
      mi = new MutableInt(0);
      next.put(cls, mi);
    }

    mi.add(1);
    return mi.intValue();
  }

  public int hashCode() {
    return this.id;
  }

  public boolean equals(Object other) {
    return other == null?false:(other.getClass() != this.getClass()?false:(this.classRef.get() != ((InstanceId)other).classRef.get()?false:this.clsIndex == ((InstanceId)other).clsIndex));
  }

  public Class<?> getInstanceClass() {
    return (Class)this.classRef.get();
  }

  public int getClassIndex() {
    return this.clsIndex;
  }

  public BSHBlock getBlock() {
    return this.block;
  }
}
