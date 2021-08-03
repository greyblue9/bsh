package org.d6r;


import libcore.util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.collections4.iterators.BoundedIterator;
import org.apache.commons.collections4.IteratorUtils;


public class ObservableList<E> 
     extends AbstractList<E> 
  implements Cloneable, Serializable, RandomAccess, IObservable
{
  public static enum UpdateKind { ADD, REMOVE };
  
  public static class Update<U> {
    private final UpdateKind kind;
    private final U element;
    private final int index;
    public Update(UpdateKind kind, Object element, int index) {
      this.kind = kind;
      this.element = (U) element;
      this.index = index;
    }
    public UpdateKind getKind() {
      return kind;
    }
    public U getElement() {
      return element;
    }
    public int getIndex() {
      return index;
    }
  }
  
  protected List<Update<E>> batchUpdate(UpdateKind kind, int start, int len)
  {
    List<Update<E>> list = new ArrayList<>();
    for (int i=start; i<start+len; i++) {
      list.add(new Update<E>(kind, array[i], i));
    }
    if (kind == UpdateKind.REMOVE) Collections.reverse(list);
    return Collections.unmodifiableList(list);
  }
  
  protected void batchNotify(List<Update<E>> updates) {
    for (Update<E> update: updates) {
      _notifyObservers(update);
    }
  }
  
  private static final int MIN_CAPACITY_INCREMENT = 12;
  private static final long serialVersionUID = 8683452581122892189L;
  protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  
  protected transient Object[] array;
  protected boolean changed;
  protected List<IObserver> observers;
  protected int size;
  
  public ObservableList() {
    this.array = EMPTY_OBJECT_ARRAY;
    this.observers = new ArrayList<IObserver>();
  }
  
  public ObservableList(final int i) {
    if (i < 0) {
      throw new IllegalArgumentException("capacity < 0: "+i);
    }
    Object[] object;
    if (i == 0) {
      object = EMPTY_OBJECT_ARRAY;
    }
    else {
      object = new Object[i];
    }
    this.array = object;
  }
  
  public ObservableList(final Collection<? extends E> collection) {
    if (collection == null) {
      throw new NullPointerException("collection == null");
    }
    final Object[] array = collection.toArray();
    Object[] array2;
    if (array.getClass() != Object[].class) {
      array2 = new Object[array.length];
      System.arraycopy(array, 0, array2, 0, array.length);
    }
    else {
      array2 = array;
    }
    this.array = array2;
    this.size = array2.length;
  }
  
  private static int newCapacity(final int n) {
    int n2;
    if (n < 6) {
      n2 = 12;
    }
    else {
      n2 = n >> 1;
    }
    return n2+n;
  }
  
  private void readObject(final ObjectInputStream objectInputStream) 
    throws IOException, ClassNotFoundException 
  {
    objectInputStream.defaultReadObject();
    final int int1 = objectInputStream.readInt();
    if (int1 < this.size) {
      throw new InvalidObjectException("Capacity: "+int1+" < size: "+this.size);
    }
    Object[] object;
    if (int1 == 0) {
      object = EMPTY_OBJECT_ARRAY;
    }
    else {
      object = new Object[int1];
    }
    this.array = object;
    for (int i = 0; i < this.size; ++i) {
      this.array[i] = objectInputStream.readObject();
    }
  }
  
  static IndexOutOfBoundsException throwIndexOutOfBoundsException(final int i, final int j) {
    throw new IndexOutOfBoundsException("Invalid index "+i+", size is "+j);
  }
  
  private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
    objectOutputStream.defaultWriteObject();
    objectOutputStream.writeInt(this.array.length);
    for (int i = 0; i < this.size; ++i) {
      objectOutputStream.writeObject(this.array[i]);
    }
  }
  
  @Override
  public void add(final int n, final E e) {
    Object[] array = this.array;
    final int size = this.size;
    if (n > size || n < 0) {
      throwIndexOutOfBoundsException(n, size);
    }
    if (size < array.length) {
      System.arraycopy(array, n, array, n+1, size-n);
    }
    else {
      final Object[] array2 = new Object[newCapacity(size)];
      System.arraycopy(array, 0, array2, 0, n);
      System.arraycopy(array, n, array2, n+1, size-n);
      this.array = array2;
      array = array2;
    }
    array[n] = e;
    this.size = size+1;
    ++super.modCount;
    
    try {
      return;
    } finally {
      Update<E> update = new Update(UpdateKind.ADD, e, n);
      _notifyObservers(update);
    }
  }
  
  @Override
  public boolean add(final E e) {
    final Object[] array = this.array;
    final int size = this.size;
    Object[] array2;
    Update<E> update = new Update(UpdateKind.ADD, e, size);
    
    if (size == array.length) {
      int n;
      if (size < 6) {
        n = 12;
      }
      else {
        n = size >> 1;
      }
      array2 = new Object[n+size];

      System.arraycopy(array, 0, array2, 0, size);
      this.array = array2;
    }
    else {
      array2 = array;
    }
    array2[size] = e;
    this.size = size+1;
    ++super.modCount;
    try {
      return true;
    } finally {
      _notifyObservers(update);
    }
  }
  
  @Override
  public boolean addAll(final int n, final Collection<? extends E> collection) {
    final int size = this.size;
    if (n > size || n < 0) {
      throwIndexOutOfBoundsException(n, size);
    }
    final Object[] array = collection.toArray();
    final int length = array.length;
    if (length == 0) {
      return false;
    }
    Object[] array2 = this.array;
    final int size2 = size+length;
    if (size2 <= array2.length) {
      System.arraycopy(array2, n, array2, n+length, size-n);
    }
    else {
      final Object[] array3 = new Object[newCapacity(size2-1)];
      System.arraycopy(array2, 0, array3, 0, n);
      System.arraycopy(array2, n, array3, n+length, size-n);
      this.array = array3;
      array2 = array3;
    }
    System.arraycopy(array, 0, array2, n, length);
    this.size = size2;
    ++super.modCount;
    try {
      return true;
    } finally {      
      batchNotify(batchUpdate(UpdateKind.ADD, n, length));
    }
  }
  
  @Override
  public boolean addAll(final Collection<? extends E> collection) {
    final Object[] array = collection.toArray();
    final int length = array.length;
    if (length == 0) {
      return false;
    }
    final Object[] array2 = this.array;
    final int size = this.size;
    final int size2 = size+length;
    Object[] array3;
    if (size2 > array2.length) {
      array3 = new Object[newCapacity(size2-1)];
      System.arraycopy(array2, 0, array3, 0, size);
      this.array = array3;
    }
    else {
      array3 = array2;
    }
    System.arraycopy(array, 0, array3, size, length);
    this.size = size2;
    ++super.modCount;
    try {
      return true;
    } finally {      
      batchNotify(batchUpdate(UpdateKind.ADD, size, length));
    }
  }
  
  @Override
  public void addObserver(final IObserver observer) {
    if (observer == null) {
      throw new NullPointerException("observer == null");
    }
    synchronized (this) {
      if (!this.observers.contains(observer)) {
        this.observers.add(observer);
      }
    }
  }
  
  @Override
  public void clear() {
    
    if (this.size != 0) {
      List<Update<E>> batch = batchUpdate(UpdateKind.REMOVE, 0, this.size);
      try {        
        Arrays.fill(this.array, 0, this.size, null);
        this.size = 0;
        ++super.modCount;
      } finally {
        batchNotify(batch);
      }
    }
  }
  
  protected void clearChanged() {
    this.changed = false;
  }
  
  @Override
  public ObservableList clone() {
    try {
      final ObservableList observableList2 = (ObservableList) super.clone();
      observableList2.array = this.array.clone();
      return observableList2;
    }
    catch (CloneNotSupportedException ex) {
      throw new AssertionError(String.format(
        "clone unexpectedly failed; superclass = %s", 
        getClass().getSuperclass()
      ));
    }
  }
  
  @Override
  public boolean contains(final Object o) {
    final Object[] array = this.array;
    final int size = this.size;
    if (o != null) {
      int n = 0;
      while (true) {
        final boolean b = false;
        if (n >= size) {
          return b;
        }
        if (o.equals(array[n])) {
          break;
        }
        ++n;
      }
    }
    else {
      int n2 = 0;
      while (true) {
        final boolean b = false;
        if (n2 >= size) {
          return b;
        }
        if (array[n2] == null) {
          break;
        }
        ++n2;
      }
    }
    return true;
  }
  
  @Override
  public int countObservers() {
    return this.observers.size();
  }
  
  @Override
  public void deleteObserver(final IObserver observer) {
    synchronized (this) {
      this.observers.remove(observer);
    }
  }
  
  @Override
  public void deleteObservers() {
    synchronized (this) {
      this.observers.clear();
    }
  }
  
  public void ensureCapacity(final int n) {
    final Object[] array = this.array;
    if (array.length < n) {
      final Object[] array2 = new Object[n];
      System.arraycopy(array, 0, array2, 0, this.size);
      this.array = array2;
      ++super.modCount;
    }
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o != this) {
      final boolean b = o instanceof List;
      boolean b2 = false;
      if (!b) {
        return b2;
      }
      final List list = (List) o;
      final int size = this.size;
      final int size2 = list.size();
      b2 = false;
      if (size2 != size) {
        return b2;
      }
      final Object[] array = this.array;
      if (list instanceof RandomAccess) {
        for (int i = 0; i < size; ++i) {
          final Object o2 = array[i];
          final Object value = list.get(i);
          if (o2 == null) {
            b2 = false;
            if (value != null) {
              return b2;
            }
          }
          else {
            final boolean equals = o2.equals(value);
            b2 = false;
            if (!equals) {
              return b2;
            }
          }
        }
      }
      else {
        final Iterator<Object> iterator = list.iterator();
        for (final Object o3 : array) {
          final Object next = iterator.next();
          if (o3 == null) {
            b2 = false;
            if (next != null) {
              return b2;
            }
          }
          else {
            final boolean equals2 = o3.equals(next);
            b2 = false;
            if (!equals2) {
              return b2;
            }
          }
        }
      }
    }
    return true;
  }
  
  @Override
  public E get(final int n) {
    if (n >= this.size) {
      throwIndexOutOfBoundsException(n, this.size);
    }
    return (E) this.array[n];
  }
  
  public boolean hasChanged() {
    return this.changed;
  }
  
  @Override
  public int hashCode() {
    final Object[] array = this.array;
    int n = 1;
    int n3;
    for (int size = this.size, i = 0; i < size; ++i, n = n3) {
      final Object o = array[i];
      final int n2 = n*31;
      int hashCode;
      if (o == null) {
        hashCode = 0;
      }
      else {
        hashCode = o.hashCode();
      }
      n3 = n2+hashCode;
    }
    return n;
  }
  
  @Override
  public int indexOf(final Object o) {
    final Object[] array = this.array;
    final int size = this.size;
    int i = 0;
    if (o == null) {
      while (i < size) {
        if (array[i] == null) {
          return i;
        }
        ++i;
      }
    }
    else {
      while (i < size) {
        if (o.equals(array[i])) {
          return i;
        }
        ++i;
      }
    }
    return -1;
  }
  
  @Override
  public boolean isEmpty() {
    return this.size == 0;
  }
  
  @Override
  public Iterator<E> iterator() {
    Iterator<E> it0 = new ObjectArrayIterator<E>((E[]) array);
    Iterator<E> it = new BoundedIterator<E>(it0, 0, size);
    return it;
  }
  
  @Override
  public ListIterator<E> listIterator() {
    Iterator<E> it0 = new ObjectArrayIterator<E>((E[]) array);
    Iterator<E> it = new BoundedIterator<E>(it0, 0, size);
    Iterable<E> itb = IteratorUtils.asMultipleUseIterable(it);
    
    return CollectionUtil.listIterator(itb);
  }
  
  @Override
  public int lastIndexOf(final Object o) {
    final Object[] array = this.array;
    if (o != null) {
      for (int i = -1+this.size; i >= 0; --i) {
        if (o.equals(array[i])) {
          return i;
        }
      }
      return -1;
    }
    for (int i = -1+this.size; i >= 0; --i) {
      if (array[i] == null) {
        return i;
      }
    }
    return -1;
  }
  
  
  
  protected void _notifyObservers(final IObservable sender, final Object o) {
    this._notifyObservers(o);
  }
  
  protected void _notifyObservers(final Object o) {
    setChanged();
    notifyObservers(o);
  }
  
  
  @Override
  public void notifyObservers() {
    this.notifyObservers(null);
  }
  
  @Override
  public void notifyObservers(final Object o) {
      Label_0090:
      while (true) {
        synchronized (this) {
            if (!this.hasChanged()) {
              continue Label_0090;
            }
            this.clearChanged();
            final IObserver[] array2 = new IObserver[this.observers.size()];
            this.observers.toArray(array2);
            // monitorexit(this)
            if (array2 != null) {
              for (int length = array2.length, i = 0; i < length; ++i) {
                
                array2[i].update(this, o);
              }
            }
        }
        break Label_0090;
      }
  }
  
  @Override
  public E remove(final int n) {
    final Object[] array = this.array;
    final int size = this.size;
    if (n >= size) {
      throwIndexOutOfBoundsException(n, size);
    }
    final Object o = array[n];
    Update<E> update = new Update<E>(UpdateKind.REMOVE, o, n);
    final int n2 = n+1;
    final int size2 = size-1;
    
    System.arraycopy(array, n2, array, n, size2-n);
    array[size2] = null;
    this.size = size2;
    ++super.modCount;

    try {
      return (E) o;
    } finally {      
      _notifyObservers(update);
    }
  }
  
  @Override
  public boolean remove(final Object o) {
    final Object[] array = this.array;
    final int size = this.size;
    if (o != null) {
      for (int i = 0; i < size; ++i) {
        if (o.equals(array[i])) {
          Update<E> update = new Update<E>(UpdateKind.REMOVE, o, i);
          final int n = i+1;
          final int size2 = size-1;
          System.arraycopy(array, n, array, i, size2-i);
          array[size2] = null;
          this.size = size2;
          ++super.modCount;
          try {
            return true;
          } finally {      
            _notifyObservers(update);
          }
        }
      }
    } else {
      for (int j = 0; j < size; ++j) {
        if (array[j] == null) {
          Update<E> update = new Update<E>(UpdateKind.REMOVE, null, j);
          final int n2 = j+1;
          final int size3 = size-1;
          System.arraycopy(array, n2, array, j, size3-j);
          array[size3] = null;
          this.size = size3;
          ++super.modCount;
          try {
            return true;
          } finally {      
            _notifyObservers(update);
          }
        }
      }
    }
    return false;
  }
  
  @Override
  protected void removeRange(final int n, final int n2) {
    if (n == n2) {
      return;
    }
    final Object[] array = this.array;
    final int size = this.size;
    if (n >= size) {
      throw new IndexOutOfBoundsException("fromIndex "+n+" >= size "+this.size);
    }
    if (n2 > size) {
      throw new IndexOutOfBoundsException("toIndex "+n2+" > size "+this.size);
    }
    if (n > n2) {
      throw new IndexOutOfBoundsException("fromIndex "+n+" > toIndex "+n2);
    }
    List<Update<E>> batch = batchUpdate(UpdateKind.REMOVE, n, n2-n);
    System.arraycopy(array, n2, array, n, size-n2);
    final int n3 = n2-n;
    Arrays.fill(array, size-n3, size, null);
    this.size = size-n3;
    ++super.modCount;
    try {
      return;
    } finally {
      batchNotify(batch);
    }
  }
  
  @Override
  public E set(final int n, final E e) {
    final Object[] array = this.array;
    if (n >= this.size) {
      throwIndexOutOfBoundsException(n, this.size);
    }
    final Object o = array[n];
    Update<E> updateRem = new Update<E>(UpdateKind.REMOVE, o, n);
    Update<E> updateAdd = new Update<E>(UpdateKind.ADD, e, n);
    array[n] = e;
    try {
      return (E) o;
    } finally {
      batchNotify(Arrays.asList(updateRem, updateAdd));
    }
  }
  
  protected void setChanged() {
    this.changed = true;
  }
  
  @Override
  public int size() {
    return this.size;
  }
  
  @Override
  public Object[] toArray() {
    final int size = this.size;
    final Object[] array = new Object[size];
    System.arraycopy(this.array, 0, array, 0, size);
    return array;
  }
  
  @Override
  public <T> T[] toArray(final T[] array) {
    final int size = this.size;
    Object[] array2;
    if (array.length < size) {
      array2 = (Object[])
        Array.newInstance(array.getClass().getComponentType(), size);
    }
    else {
      array2 = array;
    }
    System.arraycopy(this.array, 0, array2, 0, size);
    if (array2.length > size) {
      array2[size] = null;
    }
    return (T[]) array2;
  }
  
  public void trimToSize() {
    final int size = this.size;
    if (size == this.array.length) {
      return;
    }
    List<Update<E>> batch = Collections.emptyList();
    if (size == 0) {
      this.array = EMPTY_OBJECT_ARRAY;
    } else {
      int diff = this.size - size;
      if (diff > 0) {
        batch = batchUpdate(UpdateKind.REMOVE, this.size, diff);
      }
      final Object[] array = new Object[size];
      System.arraycopy(this.array, 0, array, 0, size);
      this.array = array;
    }
    try {
      ++super.modCount;
      return;
    } finally {
      batchNotify(batch);
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(size * 16);
    sb.append('[');
    for (int i=0; i<size; i++) {
      if (i != 0) sb.append(", ");
      try {
        sb.append(array[i]);
      } catch (Throwable e) { 
        sb.append(String.format("<element %d threw %s>", i, e));
      }
    }
    sb.append(']');
    sb.append(String.format(" (%d observers)", countObservers()));
    return sb.toString();
  }
}




