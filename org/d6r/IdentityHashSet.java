package org.d6r;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;


public class IdentityHashSet<E>
  implements Set<E>, Collection<E>, Cloneable
{
  protected static final Object VALUE = "";
  protected IdentityHashMap<E, Object> delegate;
  
  public IdentityHashSet() {
    delegate = new IdentityHashMap<E, Object>();
  }
  
  public IdentityHashSet(final Collection<? extends E> original) {
    this();
    addAll((Collection<? extends E>) original);
  }
  
  /**
  Returns a count of how many objects this `Collection' contains.
  
  In this class this method is declared abstract and has to be implemented
  by concrete `Collection' implementations.
  @return how many objects this `Collection' contains, or `Integer.MAX_VALUE'
  if there are more than `Integer.MAX_VALUE' elements in this
  `Collection'.
  */
  @Override
  public int size() {
    return delegate.size();
  }
  
  @Override
  public boolean contains(final Object o) {
    return delegate.containsKey(o);
  }
  
  /**
  Returns an instance of `Iterator' that may be used to access the
  objects contained by this `IdentityHashSet'. The order in which the 
  elements are returned by the `Iterator' is not defined.
  
  @return an iterator for accessing the `Set' contents.
  */
  @Override
  public Iterator<E> iterator() {
    return delegate.keySet().iterator();
  }
  
  @Override
  public boolean add(final E o) {
    if (delegate.containsKey(o)) return false;
    delegate.put(o, VALUE);
    return true;
  }
  
  /**
  Removes one instance of the specified object from this `Collection' if one
  is contained (optional). This implementation iterates over this
  `Collection' and tests for each element `e' returned by the iterator,
  whether `e' is equal to the given object. If `object != null'
  then this test is performed using `object.equals(e)', otherwise
  using `object == null'. If an element equal to the given object is
  found, then the `remove' method is called on the iterator and
  `true' is returned, `false' otherwise. If the iterator does
  not support removing elements, an `UnsupportedOperationException'
  is thrown.
  @param object
  the object to remove.
  @return `true' if this `Collection' is modified, `false'
  otherwise.
  @throws UnsupportedOperationException
  if removing from this `Collection' is not supported.
  @throws ClassCastException
  if the object passed is not of the correct type.
  @throws NullPointerException
  if `object' is `null' and this `Collection'
  doesn't support `null' elements.
  */
  @Override
  public boolean remove(final Object o) {
    if (! delegate.containsKey(o)) return false;
    delegate.remove(o);
    return true;
  }
  
  /**
  Removes all elements from this `Collection', leaving it empty (optional).
  */
  @Override
  public void clear() {
    //delegate.entrySet().clear();
    delegate.clear();
  }
  
  @Override
  public int hashCode() {
    final int PRIME = 3196347;
    int result = 377571917;
    Iterator<?> it = delegate.keySet().iterator();
    while (it.hasNext()) { 
      Object elem = it.next();      
      result ^= (
        (elem == null) 
          ? 1007 
          : (System.identityHashCode(elem.hashCode()) * PRIME)
      );
    }
    return result;
  }
  
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final IdentityHashSet<?> other = (IdentityHashSet<?>) obj;
    if (delegate == null) {
      if (other.delegate != null) {
        return false;
      }
    }
    
    if (delegate.size() != other.delegate.size()) return false;
    return delegate.keySet().containsAll(other.delegate.keySet());
  }
  
  @Override
  public String toString() {
    return delegate.keySet().toString();
  }
  
  @Override
  public IdentityHashSet<E> clone() {
    try {
      IdentityHashSet<E> c = (IdentityHashSet<E>) (Object) super.clone();
      c.delegate = (IdentityHashMap<E, Object>) (Object) delegate.clone();
      return c;
    } catch (Throwable ex) {
      CloneNotSupportedException cnse = new CloneNotSupportedException();
      cnse.initCause(ex);
      throw Reflector.Util.sneakyThrow(cnse);
    }
  }
  
  
  /**
  Attempts to add all of the objects contained in `collection'
  to the contents of this `Collection' (optional). This implementation
  iterates over the given `Collection' and calls `add' for each
  element. If any of these calls return `true', then `true' is
  returned as result of this method call, `false' otherwise. If this
  `Collection' does not support adding elements, an {@code
  UnsupportedOperationException} is thrown.
  
  If the passed `Collection' is changed during the process of adding elements
  to this `Collection', the behavior depends on the behavior of the passed
  `Collection'.
  @param collection
  the collection of objects.
  @return `true' if this `Collection' is modified, `false'
  otherwise.
  @throws UnsupportedOperationException
  if adding to this `Collection' is not supported.
  @throws ClassCastException
  if the class of an object is inappropriate for this
  `Collection'.
  @throws IllegalArgumentException
  if an object cannot be added to this `Collection'.
  @throws NullPointerException
  if `collection' is `null', or if it contains
  `null' elements and this `Collection' does not support
  such elements.
  */
  public boolean addAll(Collection<? extends E> collection) {
    boolean result = false;
    Iterator<? extends E> it = collection.iterator();
    while (it.hasNext()) {
      if (add(it.next())) {
        result = true;
      }
    }
    return result;
  }
  
  /**
  Tests whether this `Collection' contains all objects contained in the
  specified `Collection'. This implementation iterates over the specified
  `Collection'. If one element returned by the iterator is not contained in
  this `Collection', then `false' is returned; `true' otherwise.
  @param collection
  the collection of objects.
  @return `true' if all objects in the specified `Collection' are
  elements of this `Collection', `false' otherwise.
  @throws ClassCastException
  if one or more elements of `collection' isn't of the
  correct type.
  @throws NullPointerException
  if `collection' contains at least one `null'
  element and this `Collection' doesn't support `null'
  elements.
  @throws NullPointerException
  if `collection' is `null'.
  */
  public boolean containsAll(Collection<?> collection) {
    Iterator<?> it = collection.iterator();
    while (it.hasNext()) {
      if (!delegate.containsKey(it.next())) {
        return false;
      }
    }
    return true;
  }

  /**
  Returns if this `Collection' contains no elements. This implementation
  tests, whether `size' returns 0.
  @return `true' if this `Collection' has no elements, `false'
  otherwise.
  @see #size
  */
  public boolean isEmpty() {
    return delegate.size() == 0;
  }
  
  /**
  Removes all occurrences in this `Collection' of each object in the
  specified `Collection' (optional). After this method returns none of the
  elements in the passed `Collection' can be found in this `Collection'
  anymore.
  
  This implementation iterates over this `Collection' and tests for each
  element `e' returned by the iterator, whether it is contained in
  the specified `Collection'. If this test is positive, then the {@code
  remove} method is called on the iterator. If the iterator does not
  support removing elements, an `UnsupportedOperationException' is
  thrown.
  @param collection
  the collection of objects to remove.
  @return `true' if this `Collection' is modified, `false'
  otherwise.
  @throws UnsupportedOperationException
  if removing from this `Collection' is not supported.
  @throws ClassCastException
  if one or more elements of `collection' isn't of the
  correct type.
  @throws NullPointerException
  if `collection' contains at least one `null'
  element and this `Collection' doesn't support `null'
  elements.
  @throws NullPointerException
  if `collection' is `null'.
  */
  public boolean removeAll(Collection<?> collection) {
    boolean result = false;
    Iterator<?> it = iterator();
    while (it.hasNext()) {
      if (collection.contains(it.next())) {
        it.remove();
        result = true;
      }
    }
    return result;
  }

  /**
  Removes all objects from this `Collection' that are not also found in the
  `Collection' passed (optional). After this method returns this `Collection'
  will only contain elements that also can be found in the `Collection'
  passed to this method.
  
  This implementation iterates over this `Collection' and tests for each
  element `e' returned by the iterator, whether it is contained in
  the specified `Collection'. If this test is negative, then the {@code
  remove} method is called on the iterator. If the iterator does not
  support removing elements, an `UnsupportedOperationException' is
  thrown.
  @param collection
  the collection of objects to retain.
  @return `true' if this `Collection' is modified, `false'
  otherwise.
  @throws UnsupportedOperationException
  if removing from this `Collection' is not supported.
  @throws ClassCastException
  if one or more elements of `collection'
  isn't of the correct type.
  @throws NullPointerException
  if `collection' contains at least one
  `null' element and this `Collection' doesn't support
  `null' elements.
  @throws NullPointerException
  if `collection' is `null'.
  */
  public boolean retainAll(Collection<?> collection) {
    boolean result = false;
    Iterator<?> it = iterator();
    while (it.hasNext()) {
      if (!collection.contains(it.next())) {
        it.remove();
        result = true;
      }
    }
    return result;
  }
  

  public Object[] toArray() {
    int size = size(), index = 0;
    Iterator<?> it = iterator();
    Object[] array = new Object[size];
    while (index < size) {
      array[index++] = it.next();
    }
    return array;
  }
  

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] contents) {
    int size = size(), index = 0;
    if (size > contents.length) {
      Class<?> ct = contents.getClass().getComponentType();
      contents = (T[]) Array.newInstance(ct, size);
    }
    for (E entry : this) {
      contents[index++] = (T) entry;
    }
    if (index < contents.length) {
      contents[index] = null;
    }
    return contents;
  }

  /**
  Returns the string representation of this `Collection'. The presentation
  has a specific format. It is enclosed by square brackets ("[]"). Elements
  are separated by ', ' (comma and space).
  @return the string representation of this `Collection'.
  */
  /*@Override
  public String toString() {
    if (isEmpty()) {
      return "[]";
    }
    StringBuilder buffer = new StringBuilder(size() * 16);
    buffer.append('[');
    Iterator<?> it = iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next != this) {
        buffer.append(next);
      } else {
        buffer.append("(this Collection)");
      }
      if (it.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append(']');
    return buffer.toString();
  }*/
}



