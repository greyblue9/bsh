package org.d6r;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.ListIterator;
import org.apache.commons.lang3.ArrayUtils;
/**
Decorates an iterator to support pushback of elements.

The decorator stores the pushed back elements in a LIFO manner: the last element
that has been pushed back, will be returned as the next element in a call to `#next()'.

The decorator does not support the removal operation. Any call to `#remove()' will
result in an `UnsupportedOperationException'.

@since 4.0
@version $Id: PushbackIterator.java 1686855 2015-06-22 13:00:27Z tn $
*/
public class DequeIterator<E> 
 implements Iterator<E>, ListIterator<E> {

  /** The iterator being decorated. */
  //protected final Iterator<? extends E> iterator;
  
  /** The LIFO queue containing the pushed back items. */
  protected Object[] items;
  protected int pos;
  
  /**
  Decorates the specified iterator to support one-element
  lookahead.
  
  If the iterator is already a `PushbackIterator',
  it is returned directly.
  
  @param <E>  the element type
  @param iterator - the iterator to decorate
  @return a new peeking iterator
  @throws NullPointerException if the iterator is null
  */
  public static <E> DequeIterator<E> dequeIterator(final
  Iterator<? extends E> iterator) 
  {
    if (iterator == null) {
      throw new NullPointerException(
        "Iterator must not be null"
      );
    }
    if (iterator instanceof DequeIterator<?>) {
      return (DequeIterator<E>) iterator;
    }
    return new DequeIterator<E>(iterator);
  }

  

  /**
  @param iterator - the iterator to decorate
  */
  public DequeIterator(final Iterator<? extends E> iterator) {
    super();
    items = CollectionUtil.<E>toArray(iterator);
    this.pos = -1;
  }
  
  /**
  Push back the given element to the iterator.
  
  Calling `#next()' immediately afterwards will return
  exactly this element.
  
  @param item - the element to push back to the iterator
  */
  /*public void pushback(final E item) {
    items.offerLast(item);
  }*/
  
  public boolean hasNext() {
    return 0 <= (pos + 1) && (pos + 1) < items.length;
  }
  
  public E next() {
    int newPos = pos + 1;
    if (0 <= newPos && newPos < items.length) {
      return (E) items[pos = newPos];
    }
    throw badIndex(newPos);
  }
  
  public boolean hasPrevious() {
    return 0 <= (pos - 1) && (pos - 1) < items.length;
  }
  
  public E previous() {
    int newPos = pos - 1;
    if (0 <= newPos && newPos < items.length) {
      return (E) items[pos = newPos];
    }
    throw badIndex(newPos);
  }
  
  protected NoSuchElementException badIndex(int newPos) {
    return new NoSuchElementException(String.format(
      "length = %d, requested index = %d", items.length, newPos
    ));
  }
  
  public void reset() {
    pos = -1;
  }
  
  public E[] toArray() {
    return Decompiler.array((E[]) items);
  }
  
  public void add(E object) {
    items = ArrayUtils.<E>add((E[]) items, pos + 1, object);
  }

  public int nextIndex() {
    return pos + 1;
  }
  
  public int previousIndex() {
    return pos - 1;
  }
  
  public void set(E object) {
    items[pos] = object;
  }
  
  /**
  This iterator will always throw an `UnsupportedOperationException'.
  
  @throws UnsupportedOperationException always
  */
  public void remove() {
    items = (E[]) ArrayUtils.remove(items, pos);
    pos -= 1;
  }
  
}