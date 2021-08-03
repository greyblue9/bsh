package org.d6r;
import java.io.Serializable;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.Map;

/**
  A quadruple consisting of four elements.
  
  This class is an abstract implementation defining the
  basic API.
  
  It refers to the elements as:
   - 'left' of type L, 
        via getLeft(), getKey() , 
            ((Pair<L, R>) getOuter()).getKey()
   - 'middle' of type M, 
        via getMiddle(), 
            ((Pair<M, N>) getInner()).getKey()
   - 'next' of type N, 
        via getNext() , 
            ((Pair<M, N>) getInner()).getValue()
   - 'right' of type R, 
       via getRight(), getValue(),
            ((Pair<L, R>) getOuter()).getValue()
  
  Subclass implementations may be mutable or immutable.
  However, there is no restriction on the type of the
  stored objects that may be stored.
  
  If mutable objects are stored in the triple, then the
   triple itself effectively becomes mutable.
  
  @param <L> the left element type
  @param <M> the middle element type
  @param <N> the right element type
  @param <R> the right element type
*/
public abstract class Quad<L, M, N, R> 
 implements 
   Comparable<Quad<L, M, N, R>>, 
   Map.Entry<L, R>,
   Serializable
{
  /** Serialization version */
  private static final long serialVersionUID = 1L;

  /**
  Obtains an immutable triple of from three objects inferring the generic types.
  This factory allows the triple to be created using inference to
  obtain the generic types.
  @param <L> the left element type
  @param <M> the middle element type
  @param <R> the right element type
  @param left  the left element, may be null
  @param middle the middle element, may be null
  @param right  the right element, may be null
  @return a triple formed from the three parameters, not null
*/
  public static <L, M, N, R> 
  Quad<L, M, N, R> of(final L left, final M middle,
  final N next, final R right) 
  {
    return new ImmutableQuad<L, M, N, R>(
      left, middle, next, right
    );
  }

  //-----------------------------------------------------------------------
  /**
    Gets the left element from this triple.
    
    @return the left element, may be null
   */
  public abstract L getLeft();
  
  @Override
  public abstract L getKey();

  /**
    Gets the middle element from this triple.
    
    @return the middle element, may be null
   */
  public abstract M getMiddle();
  

  /**
    Gets the middle element from this triple.
    
    @return the middle element, may be null
   */
  public abstract N getNext();

  /**
    Gets the right element from this triple.
    
    @return the right element, may be null
   */
  public abstract R getRight();
  
  @Override
  public abstract R getValue();
  //-----------------------------------------------------------------------
  /**
    Compares the triple based on the left element, followed by the middle element,
    finally the right element.
    The types must be {@code Comparable}.
    
    @param other  the other triple, not null
    @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(final Quad<L, M, N, R> other) {
    return new CompareToBuilder()
      .append(getLeft(),   other.getLeft())
      .append(getMiddle(), other.getMiddle())
      .append(getNext(),   other.getNext())
      .append(getRight(),  other.getRight())
      .toComparison();
  }

  /**
    Compares this triple to another based on the three
    elements.    
    @param obj - the object to compare to, null returns
           false
    @return true if the elements of the triple are equal 
    @note ObjectUtils.equals(Object, Object) has been
          deprecated in 3.2
   */
  @SuppressWarnings( "deprecation" )
  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Quad)) return false;
    final Quad<?, ?, ?, ?> oth = (Quad<?, ?, ?, ?>) obj;
    return 
         ObjectUtils.equals(getLeft(),   oth.getLeft())
      && ObjectUtils.equals(getMiddle(), oth.getMiddle())
      && ObjectUtils.equals(getNext(),   oth.getNext())
      && ObjectUtils.equals(getRight(),  oth.getRight());
  }

  /**
    Returns a suitable hash code.
    
    @return the hash code
   */
  @Override
  public int hashCode() {
    return 
    (getLeft() == null?   0: getLeft().hashCode()) ^
    (getMiddle() == null? 0: getMiddle().hashCode()) ^
    (getNext() == null?   0: getNext().hashCode()) ^
    (getRight() == null?  0: getRight().hashCode());
  }

  /**
    Returns a String representation of this triple using the format {@code ($left,$middle,$right)}.
    
    @return a string describing this object, not null
   */
  @Override
  public String toString() {
    return new StringBuilder()
    .append('(')
    .append(getLeft())
    .append(',')
    .append(getMiddle())
    .append(',')
    .append(getNext())
    .append(',')
    .append(getRight())
    .append(')')
    .toString();
  }

  /**
    Formats the receiver using the given format.
    
    This uses {@link java.util.Formattable} to perform the formatting. Three variables may
    be used to embed the left and right elements. Use {@code %1$s} for the left
    element, {@code %2$s} for the middle and {@code %3$s} for the right element.
    The default format used by {@code toString()} is {@code (%1$s,%2$s,%3$s)}.
    
    @param format  the format string, optionally containing {@code %1$s}, {@code %2$s} and {@code %3$s}, not null
    @return the formatted string, not null
   */
  public String toString(final String format) {
    return String.format(
      format, 
      getLeft(), getMiddle(), getNext(), getRight()
    );
  }

}

/**
 * <p>An immutable triple consisting of three {@code Object} elements.</p>
 * 
 * <p>Although the implementation is immutable, there is no restriction on the objects
 * that may be stored. If mutable objects are stored in the triple, then the triple
 * itself effectively becomes mutable. The class is also {@code final}, so a subclass
 * can not add undesirable behaviour.</p>
 * 
 * <p>#ThreadSafe# if all three objects are thread-safe</p>
 *
 * @param <L> the left element type
 * @param <M> the middle element type
 * @param <R> the right element type
 *
 * @version $Id: ImmutableQuad.java 1592817 2014-05-06 17:57:38Z britter $
 * @since 3.2
 */
class ImmutableQuad<L, M, N, R> extends Quad<L, M, N, R>
{
  /** Serialization version */
  private static final long serialVersionUID = 1L;

  /** Left object */
  public final L left;
  /** Middle object */
  public final M middle;
  /** Next object */
  public final N next;
  /** Right object */
  public final R right;

  /**
   * <p>Obtains an immutable triple of from three objects inferring the generic types.</p>
   * 
   * <p>This factory allows the triple to be created using inference to
   * obtain the generic types.</p>
   * 
   * @param <L> the left element type
   * @param <M> the middle element type
   * @param <R> the right element type
   * @param left  the left element, may be null
   * @param middle  the middle element, may be null
   * @param right  the right element, may be null
   * @return a triple formed from the three parameters, not null
   */
  public static <L, M, N, R> 
  ImmutableQuad<L, M, N, R> of(final L left, 
  final M middle, final N next, final R right) 
  {
    return new ImmutableQuad<L, M, N, R>(
      left, middle, next, right
    );
  }

  /**
   * Create a new triple instance.
   *
   * @param left  the left value, may be null
   * @param middle the middle value, may be null
   * @param right  the right value, may be null
   */
  public ImmutableQuad(final L left, final M middle,
  final N next, final R right) 
  {
    super();
    this.left = left;
    this.middle = middle;
    this.next = next;
    this.right = right;
  }

  //-----------------------------------------------------------------------
  /**
   * {@inheritDoc}
   */
  @Override
  public L getLeft() {
    return left;
  }
  
  @Override
  public L getKey() {
    return left;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public M getMiddle() {
    return middle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public N getNext() {
    return next;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public R getRight() {
    return right;
  }
  
  @Override
  public R getValue() {
    return right;
  }
  
  @Override
  public R setValue(R value) {
    throw new UnsupportedOperationException(    
      String.format(
        "%s.setValue", getClass().getSimpleName()
      )
    );
  }
    

}