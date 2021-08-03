package org.d6r;

import java.io.Serializable;
import java.util.Comparator;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import javax.annotation.Nullable;

import java.util.*;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import com.google.common.collect.BoundType;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Booleans;

// import com.google.common.collect.EmptyContiguousSet;
// import com.google.common.collect.RegularContiguousSet;
import static java.lang.String.format;
import org.apache.commons.lang3.ClassUtils;
/**
 * A range (or "interval") defines the <i>boundaries</i> around a contiguous span of values of some
 * {@code Comparable} type; for example, "integers from 1 to 100 inclusive." Note that it is not
 * possible to <i>iterate</i> over these contained values. To do so, pass this range instance and
 * an appropriate {@link DiscreteDomain} to {@link ContiguousSet#create}.
 *
 * <h3>Types of ranges</h3>
 *
 * <p>Each end of the range may be bounded or unbounded. If bounded, there is an associated
 * <i>endpoint</i> value, and the range is considered to be either <i>open</i> (does not include the
 * endpoint) or <i>closed</i> (includes the endpoint) on that side. With three possibilities on each
 * side, this yields nine basic types of ranges, enumerated below. (Notation: a square bracket
 * ({@code [ ]}) indicates that the range is closed on that side; a parenthesis ({@code ( )}) means
 * it is either open or unbounded. The construct {@code {x | statement}} is read "the set of all
 * <i>x</i> such that <i>statement</i>.")
 *
 * <blockquote>
 * <table>
 * <tr><th>Notation        <th>Definition               <th>Factory method
 * <tr><td>{@code (a..b)}  <td>{@code {x | a < x < b}}  <td>{@link Range#open open}
 * <tr><td>{@code [a..b]}  <td>{@code {x | a <= x <= b}}<td>{@link Range#closed closed}
 * <tr><td>{@code (a..b]}  <td>{@code {x | a < x <= b}} <td>{@link Range#openClosed openClosed}
 * <tr><td>{@code [a..b)}  <td>{@code {x | a <= x < b}} <td>{@link Range#closedOpen closedOpen}
 * <tr><td>{@code (a..+?)} <td>{@code {x | x > a}}      <td>{@link Range#greaterThan greaterThan}
 * <tr><td>{@code [a..+?)} <td>{@code {x | x >= a}}     <td>{@link Range#atLeast atLeast}
 * <tr><td>{@code (-?..b)} <td>{@code {x | x < b}}      <td>{@link Range#lessThan lessThan}
 * <tr><td>{@code (-?..b]} <td>{@code {x | x <= b}}     <td>{@link Range#atMost atMost}
 * <tr><td>{@code (-?..+?)}<td>{@code {x}}              <td>{@link Range#all all}
 * </table>
 * </blockquote>
 *
 * <p>When both endpoints exist, the upper endpoint may not be less than the lower. The endpoints
 * may be equal only if at least one of the bounds is closed:
 *
 * <ul>
 * <li>{@code [a..a]} : a singleton range
 * <li>{@code [a..a); (a..a]} : {@linkplain #isEmpty empty} ranges; also valid
 * <li>{@code (a..a)} : <b>invalid</b>; an exception will be thrown
 * </ul>
 *
 * <h3>Warnings</h3>
 *
 * <ul>
 * <li>Use immutable value types only, if at all possible. If you must use a mutable type, <b>do
 *     not</b> allow the endpoint instances to mutate after the range is created!
 * <li>Your value type's comparison method should be {@linkplain Comparable consistent with equals}
 *     if at all possible. Otherwise, be aware that concepts used throughout this documentation such
 *     as "equal", "same", "unique" and so on actually refer to whether {@link Comparable#compareTo
 *     compareTo} returns zero, not whether {@link Object#equals equals} returns {@code true}.
 * <li>A class which implements {@code Comparable<UnrelatedType>} is very broken, and will cause
 *     undefined horrible things to happen in {@code Range}. For now, the Range API does not prevent
 *     its use, because this would also rule out all ungenerified (pre-JDK1.5) data types. <b>This
 *     may change in the future.</b>
 * </ul>
 *
 * <h3>Other notes</h3>
 *
 * <ul>
 * <li>Instances of this type are obtained using the static factory methods in this class.
 * <li>Ranges are <i>convex</i>: whenever two values are contained, all values in between them must
 *     also be contained. More formally, for any {@code c1 <= c2 <= c3} of type {@code C}, {@code
 *     r.contains(c1) && r.contains(c3)} implies {@code r.contains(c2)}). This means that a {@code
 *     Range<Integer>} can never be used to represent, say, "all <i>prime</i> numbers from 1 to
 *     100."
 * <li>When evaluated as a {@link Predicate}, a range yields the same result as invoking {@link
 *     #contains}.
 * <li>Terminology note: a range {@code a} is said to be the <i>maximal</i> range having property
 *     <i>P</i> if, for all ranges {@code b} also having property <i>P</i>, {@code a.encloses(b)}.
 *     Likewise, {@code a} is <i>minimal</i> when {@code b.encloses(a)} for all {@code b} having
 *     property <i>P</i>. See, for example, the definition of {@link #intersection intersection}.
 * </ul>
 *
 * <h3>Further reading</h3>
 *
 * <p>See the Guava User Guide article on
 * <a href="https://github.com/google/guava/wiki/RangesExplained">{@code Range}</a>.
 *
 * @author Kevin Bourrillion
 * @author Gregory Kick
 * @since 10.0
 */

@SuppressWarnings("rawtypes")
public class Range<C extends Comparable>
  implements Predicate<C>,
             Comparable<Range<C>>,
             Serializable
             
{

  public static final Func<Range, Cut> LOWER_BOUND_FN
    = AccessorFunc.getForField(Range.class, "lowerBound");
  

  public static final Func<Range, Cut> UPPER_BOUND_FN
    = AccessorFunc.getForField(Range.class, "upperBound");

  public static final Ordering<Range<?>> RANGE_LEX_ORDERING
    = new RangeLexOrdering();

  public static <C extends Comparable<?>>
  Range<C> create(final Cut<C> lowerBound, final Cut<C> upperBound) {
    return new Range<C>(lowerBound, upperBound);
  }
  
  @Override
  public int compareTo(Range<C> other) {
    return RANGE_LEX_ORDERING.compare(this, other);
  }
  
  
  
  
  public static final Map<Class<? extends Comparable>, 
                          Queue<? extends Formatter<? extends Comparable>>> 
    formats = new IdentityHashMap<>();
  
  
  public static interface Formatter<TC extends Comparable>
                  extends Func<TC, String>
  {
    
     public static class Default<TC1 extends Comparable> 
              implements Formatter<TC1>
     {
       static final Default<?> DEFAULT = new Default<Comparable<?>>();
       
       public static <TC2 extends Comparable> Formatter<TC2> getDefault() {
         return (Formatter<TC2>) (Formatter<?>) DEFAULT;
       }
       
       @Override
       public String apply(TC1 it) {
         return String.format("%s", it);
       }
     }
     
     
     String apply(TC point);
  }
  
  public static class SimpleFormatter<T extends Comparable>
           implements Formatter<T>
  {
    final Class<T> type;
    final String format;
    
    public SimpleFormatter(Class<T> boundType, String format) {
      this.type = boundType;
      this.format = format;
    }
    
    @Override
    public String apply(T point) {
      return String.format(format, point);
    }
  }
  
  public static <O extends Comparable>
  Formatter<O> pushFormat(Class<O> boundType, final String format) 
  {
    if (boundType.isPrimitive()) 
      boundType = (Class<O>) (Class<?>) ClassUtils.primitiveToWrapper(boundType);
    
    final Queue<? extends Formatter<O>> q = getFormatLifoQueue(boundType);
    final Formatter<O> newFormatter = new SimpleFormatter<O>(boundType, format);
    ((Queue) q).offer((Formatter) newFormatter);
    return newFormatter;
  }
  
  public static <O extends Comparable> Formatter<O> popFormat(Class<O> boundType) 
  {
    if (boundType.isPrimitive()) 
      boundType = (Class<O>) (Class<?>) ClassUtils.primitiveToWrapper(boundType);
    
    final Queue<? extends Formatter<O>> q = getFormatLifoQueue(boundType);
    if (q.isEmpty()) return Formatter.Default.getDefault();
    return (Formatter<O>) (Formatter<O>) ((Queue) q).poll();
  }
  
  public static <O extends Comparable> Formatter<O> 
  getFormatter(Class<O> boundType)
  {
    if (boundType.isPrimitive()) 
      boundType = (Class<O>) (Class<?>) ClassUtils.primitiveToWrapper(boundType);
    
    final Queue<? extends Formatter<O>> q = getFormatLifoQueue(boundType);
    
    Formatter<O> front = (q.isEmpty())
      ? Formatter.Default.getDefault()
      : q.peek();
    return (front != null) ? front : Formatter.Default.getDefault();
  }
  
  public static <O extends Comparable> Queue<? extends Formatter<O>>
  getFormatLifoQueue(Class<O> boundType)
  {
    if (boundType.isPrimitive()) 
      boundType = (Class<O>) (Class<?>) ClassUtils.primitiveToWrapper(boundType);
    
    Queue<? extends Formatter<O>> q 
      = (Queue<? extends Formatter<O>>) (Queue<?>) formats.get(boundType);
    if (q == null) formats.put(
      boundType, (q = Collections.asLifoQueue(new ArrayDeque<>()))
    );
    return q;
  }
  
  public static <O extends Comparable> Formatter<O> getFormatter(O bound) {
    return getFormatter((Class<O>) (
      ((bound != null)
        ? (Class<?>) bound.getClass()
        : (Class<?>) Object.class)
    ));
  }
  
  /**
  * Returns a range that contains all values strictly greater than {@code
  * lower} and strictly less than {@code upper}.
  *
  * @throws IllegalArgumentException if {@code lower} is greater than <i>or
  *     equal to</i> {@code upper}
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> open(C lower, C upper) {
    return create(Cut.aboveValue(lower), Cut.belowValue(upper));
  }

  /**
  * Returns a range that contains all values greater than or equal to
  * {@code lower} and less than or equal to {@code upper}.
  *
  * @throws IllegalArgumentException if {@code lower} is greater than {@code
  *     upper}
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> closed(C lower, C upper) {
    return create(Cut.belowValue(lower), Cut.aboveValue(upper));
  }

  /**
  * Returns a range that contains all values greater than or equal to
  * {@code lower} and strictly less than {@code upper}.
  *
  * @throws IllegalArgumentException if {@code lower} is greater than {@code
  *     upper}
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> closedOpen(C lower, C upper) {
    return create(Cut.belowValue(lower), Cut.belowValue(upper));
  }

  /**
  * Returns a range that contains all values strictly greater than {@code
  * lower} and less than or equal to {@code upper}.
  *
  * @throws IllegalArgumentException if {@code lower} is greater than {@code
  *     upper}
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> openClosed(C lower, C upper) {
    return create(Cut.aboveValue(lower), Cut.aboveValue(upper));
  }

  /**
  * Returns a range that contains any value from {@code lower} to {@code
  * upper}, where each endpoint may be either inclusive (closed) or exclusive
  * (open).
  *
  * @throws IllegalArgumentException if {@code lower} is greater than {@code
  *     upper}
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> range(C lower, BoundType lowerType, C upper, BoundType upperType) {
    checkNotNull(lowerType);
    checkNotNull(upperType);
    Cut<C> lowerBound = (lowerType == BoundType.OPEN) ? Cut.aboveValue(lower) : Cut.belowValue(lower);
    Cut<C> upperBound = (upperType == BoundType.OPEN) ? Cut.belowValue(upper) : Cut.aboveValue(upper);
    return create(lowerBound, upperBound);
  }

  /**
  * Returns a range that contains all values strictly less than {@code
  * endpoint}.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> lessThan(C endpoint) {
    return create(Cut.<C>belowAll(), Cut.belowValue(endpoint));
  }

  /**
  * Returns a range that contains all values less than or equal to
  * {@code endpoint}.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> atMost(C endpoint) {
    return create(Cut.<C>belowAll(), Cut.aboveValue(endpoint));
  }

  /**
  * Returns a range with no lower bound up to the given endpoint, which may be
  * either inclusive (closed) or exclusive (open).
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> upTo(C endpoint, BoundType boundType) {
    switch(boundType) {
      case OPEN:
        return lessThan(endpoint);
      case CLOSED:
        return atMost(endpoint);
      default:
        throw new AssertionError();
    }
  }

  /**
  * Returns a range that contains all values strictly greater than {@code
  * endpoint}.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> greaterThan(C endpoint) {
    return create(Cut.aboveValue(endpoint), Cut.<C>aboveAll());
  }

  /**
  * Returns a range that contains all values greater than or equal to
  * {@code endpoint}.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> atLeast(C endpoint) {
    return create(Cut.belowValue(endpoint), Cut.<C>aboveAll());
  }

  /**
  * Returns a range from the given endpoint, which may be either inclusive
  * (closed) or exclusive (open), with no upper bound.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> downTo(C endpoint, BoundType boundType) {
    switch(boundType) {
      case OPEN:
        return greaterThan(endpoint);
      case CLOSED:
        return atLeast(endpoint);
      default:
        throw new AssertionError();
    }
  }

  private static final Range<Comparable> ALL = new Range<Comparable>(Cut.belowAll(), Cut.aboveAll());

  /**
  * Returns a range that contains every value of type {@code C}.
  *
  * @since 14.0
  */
  @SuppressWarnings("unchecked")
  public static <C extends Comparable<?>> Range<C> all() {
    return (Range) ALL;
  }

  /**
  * Returns a range that {@linkplain Range#contains(Comparable) contains} only
  * the given value. The returned range is {@linkplain BoundType#CLOSED closed}
  * on both ends.
  *
  * @since 14.0
  */
  public static <C extends Comparable<?>> Range<C> singleton(C value) {
    return closed(value, value);
  }

  /**
  * Returns the minimal range that
  * {@linkplain Range#contains(Comparable) contains} all of the given values.
  * The returned range is {@linkplain BoundType#CLOSED closed} on both ends.
  *
  * @throws ClassCastException if the parameters are not <i>mutually
  *     comparable</i>
  * @throws NoSuchElementException if {@code values} is empty
  * @throws NullPointerException if any of {@code values} is null
  * @since 14.0
  */
  public static <C extends Comparable<?>>
  Range<C> encloseAll(Iterable<C> values)
  {
    checkNotNull(values);
    Iterator<C> valueIterator = values.iterator();
    C min = checkNotNull(valueIterator.next());
    C max = min;
    while (valueIterator.hasNext()) {
      C value = checkNotNull(valueIterator.next());
      min = Ordering.natural().min(min, value);
      max = Ordering.natural().max(max, value);
    }
    return closed(min, max);
  }

  final Cut<C> lowerBound;

  final Cut<C> upperBound;

  private Range(Cut<C> lowerBound, Cut<C> upperBound) {
    this.lowerBound = checkNotNull(lowerBound);
    this.upperBound = checkNotNull(upperBound);
    if (lowerBound.compareTo(upperBound) > 0 || lowerBound == Cut.<C>aboveAll() || upperBound == Cut.<C>belowAll()) {
      throw new IllegalArgumentException("Invalid range: " + toString(lowerBound, upperBound));
    }
  }

  /**
  * Returns {@code true} if this range has a lower endpoint.
  */
  public boolean hasLowerBound() {
    return lowerBound != Cut.belowAll();
  }

  /**
  * Returns the lower endpoint of this range.
  *
  * @throws IllegalStateException if this range is unbounded below (that is, {@link
  *     #hasLowerBound()} returns {@code false})
  */
  public C lowerEndpoint() {
    return lowerBound.endpoint();
  }

  /**
  * Returns the type of this range's lower bound: {@link BoundType#CLOSED} if the range includes
  * its lower endpoint, {@link BoundType#OPEN} if it does not.
  *
  * @throws IllegalStateException if this range is unbounded below (that is, {@link
  *     #hasLowerBound()} returns {@code false})
  */
  public BoundType lowerBoundType() {
    return lowerBound.typeAsLowerBound();
  }

  /**
  * Returns {@code true} if this range has an upper endpoint.
  */
  public boolean hasUpperBound() {
    return upperBound != Cut.aboveAll();
  }

  /**
  * Returns the upper endpoint of this range.
  *
  * @throws IllegalStateException if this range is unbounded above (that is, {@link
  *     #hasUpperBound()} returns {@code false})
  */
  public C upperEndpoint() {
    return upperBound.endpoint();
  }

  /**
  * Returns the type of this range's upper bound: {@link BoundType#CLOSED} if the range includes
  * its upper endpoint, {@link BoundType#OPEN} if it does not.
  *
  * @throws IllegalStateException if this range is unbounded above (that is, {@link
  *     #hasUpperBound()} returns {@code false})
  */
  public BoundType upperBoundType() {
    return upperBound.typeAsUpperBound();
  }

  /**
  * Returns {@code true} if this range is of the form {@code [v..v)} or {@code (v..v]}. (This does
  * not encompass ranges of the form {@code (v..v)}, because such ranges are <i>invalid</i> and
  * can't be constructed at all.)
  *
  * <p>Note that certain discrete ranges such as the integer range {@code (3..4)} are <b>not</b>
  * considered empty, even though they contain no actual values.  In these cases, it may be
  * helpful to preprocess ranges with {@link #canonical(DiscreteDomain)}.
  */
  public boolean isEmpty() {
    return lowerBound.equals(upperBound);
  }

  /**
  * Returns {@code true} if {@code value} is within the bounds of this range. For example, on the
  * range {@code [0..2)}, {@code contains(1)} returns {@code true}, while {@code contains(2)}
  * returns {@code false}.
  */
  public boolean contains(C value) {
    checkNotNull(value);
    // let this throw CCE if there is some trickery going on
    return lowerBound.isLessThan(value) && !upperBound.isLessThan(value);
  }

  /**
  * @deprecated Provided only to satisfy the {@link Predicate} interface; use {@link #contains}
  *     instead.
  */
  @Deprecated
  @Override
  public boolean apply(C input) {
    return contains(input);
  }

  /**
  * Returns {@code true} if every element in {@code values} is {@linkplain #contains contained} in
  * this range.
  */
  public boolean containsAll(Iterable<? extends C> values) {
    if (Iterables.isEmpty(values)) {
      return true;
    }
    // this optimizes testing equality of two range-backed sets
    if (values instanceof SortedSet) {
      SortedSet<? extends C> set = cast(values);
      Comparator<?> comparator = set.comparator();
      if (Ordering.natural().equals(comparator) || comparator == null) {
        return contains(set.first()) && contains(set.last());
      }
    }
    for (C value : values) {
      if (!contains(value)) {
        return false;
      }
    }
    return true;
  }

  /**
  * Returns {@code true} if the bounds of {@code other} do not extend outside the bounds of this
  * range. Examples:
  *
  * <ul>
  * <li>{@code [3..6]} encloses {@code [4..5]}
  * <li>{@code (3..6)} encloses {@code (3..6)}
  * <li>{@code [3..6]} encloses {@code [4..4)} (even though the latter is empty)
  * <li>{@code (3..6]} does not enclose {@code [3..6]}
  * <li>{@code [4..5]} does not enclose {@code (3..6)} (even though it contains every value
  *     contained by the latter range)
  * <li>{@code [3..6]} does not enclose {@code (1..1]} (even though it contains every value
  *     contained by the latter range)
  * </ul>
  *
  * <p>Note that if {@code a.encloses(b)}, then {@code b.contains(v)} implies
  * {@code a.contains(v)}, but as the last two examples illustrate, the converse is not always
  * true.
  *
  * <p>Being reflexive, antisymmetric and transitive, the {@code encloses} relation defines a
  * <i>partial order</i> over ranges. There exists a unique {@linkplain Range#all maximal} range
  * according to this relation, and also numerous {@linkplain #isEmpty minimal} ranges. Enclosure
  * also implies {@linkplain #isConnected connectedness}.
  */
  public boolean encloses(Range<C> other) {
    return lowerBound.compareTo(other.lowerBound) <= 0 && upperBound.compareTo(other.upperBound) >= 0;
  }

  /**
  * Returns {@code true} if there exists a (possibly empty) range which is {@linkplain #encloses
  * enclosed} by both this range and {@code other}.
  *
  * <p>For example,
  * <ul>
  * <li>{@code [2, 4)} and {@code [5, 7)} are not connected
  * <li>{@code [2, 4)} and {@code [3, 5)} are connected, because both enclose {@code [3, 4)}
  * <li>{@code [2, 4)} and {@code [4, 6)} are connected, because both enclose the empty range
  *     {@code [4, 4)}
  * </ul>
  *
  * <p>Note that this range and {@code other} have a well-defined {@linkplain #span union} and
  * {@linkplain #intersection intersection} (as a single, possibly-empty range) if and only if this
  * method returns {@code true}.
  *
  * <p>The connectedness relation is both reflexive and symmetric, but does not form an {@linkplain
  * Equivalence equivalence relation} as it is not transitive.
  *
  * <p>Note that certain discrete ranges are not considered connected, even though there are no
  * elements "between them."  For example, {@code [3, 5]} is not considered connected to {@code
  * [6, 10]}.  In these cases, it may be desirable for both input ranges to be preprocessed with
  * {@link #canonical(DiscreteDomain)} before testing for connectedness.
  */
  public boolean isConnected(Range<C> other) {
    return lowerBound.compareTo(other.upperBound) <= 0 && other.lowerBound.compareTo(upperBound) <= 0;
  }

  /**
  * Returns the maximal range {@linkplain #encloses enclosed} by both this range and {@code
  * connectedRange}, if such a range exists.
  *
  * <p>For example, the intersection of {@code [1..5]} and {@code (3..7)} is {@code (3..5]}. The
  * resulting range may be empty; for example, {@code [1..5)} intersected with {@code [5..7)}
  * yields the empty range {@code [5..5)}.
  *
  * <p>The intersection exists if and only if the two ranges are {@linkplain #isConnected
  * connected}.
  *
  * <p>The intersection operation is commutative, associative and idempotent, and its identity
  * element is {@link Range#all}).
  *
  * @throws IllegalArgumentException if {@code isConnected(connectedRange)} is {@code false}
  */
  public Range<C> intersection(Range<C> connectedRange) {
    int lowerCmp = lowerBound.compareTo(connectedRange.lowerBound);
    int upperCmp = upperBound.compareTo(connectedRange.upperBound);
    if (lowerCmp >= 0 && upperCmp <= 0) {
      return this;
    } else if (lowerCmp <= 0 && upperCmp >= 0) {
      return connectedRange;
    } else {
      Cut<C> newLower = (lowerCmp >= 0) ? lowerBound : connectedRange.lowerBound;
      Cut<C> newUpper = (upperCmp <= 0) ? upperBound : connectedRange.upperBound;
      return create(newLower, newUpper);
    }
  }

  /**
  * Returns the minimal range that {@linkplain #encloses encloses} both this range and {@code
  * other}. For example, the span of {@code [1..3]} and {@code (5..7)} is {@code [1..7)}.
  *
  * <p><i>If</i> the input ranges are {@linkplain #isConnected connected}, the returned range can
  * also be called their <i>union</i>. If they are not, note that the span might contain values
  * that are not contained in either input range.
  *
  * <p>Like {@link #intersection(Range) intersection}, this operation is commutative, associative
  * and idempotent. Unlike it, it is always well-defined for any two input ranges.
  */
  public Range<C> span(Range<C> other) {
    int lowerCmp = lowerBound.compareTo(other.lowerBound);
    int upperCmp = upperBound.compareTo(other.upperBound);
    if (lowerCmp <= 0 && upperCmp >= 0) {
      return this;
    } else if (lowerCmp >= 0 && upperCmp <= 0) {
      return other;
    } else {
      Cut<C> newLower = (lowerCmp <= 0) ? lowerBound : other.lowerBound;
      Cut<C> newUpper = (upperCmp >= 0) ? upperBound : other.upperBound;
      return create(newLower, newUpper);
    }
  }

  /**
  * Returns the canonical form of this range in the given domain. The canonical form has the
  * following properties:
  *
  * <ul>
  * <li>equivalence: {@code a.canonical().contains(v) == a.contains(v)} for all {@code v} (in other
  *     words, {@code ContiguousSet.create(a.canonical(domain), domain).equals(
  *     ContiguousSet.create(a, domain))}
  * <li>uniqueness: unless {@code a.isEmpty()},
  *     {@code ContiguousSet.create(a, domain).equals(ContiguousSet.create(b, domain))} implies
  *     {@code a.canonical(domain).equals(b.canonical(domain))}
  * <li>idempotence: {@code a.canonical(domain).canonical(domain).equals(a.canonical(domain))}
  * </ul>
  *
  * <p>Furthermore, this method guarantees that the range returned will be one of the following
  * canonical forms:
  *
  * <ul>
  * <li>[start..end)
  * <li>[start..+?)
  * <li>(-?..end) (only if type {@code C} is unbounded below)
  * <li>(-?..+?) (only if type {@code C} is unbounded below)
  * </ul>
  */
  public Range<C> canonical(DiscreteDomain<C> domain) {
    checkNotNull(domain);
    Cut<C> lower = lowerBound.canonical(domain);
    Cut<C> upper = upperBound.canonical(domain);
    return (lower == lowerBound && upper == upperBound) ? this : create(lower, upper);
  }

  /**
  * Returns {@code true} if {@code object} is a range having the same endpoints and bound types as
  * this range. Note that discrete ranges such as {@code (1..4)} and {@code [2..3]} are <b>not</b>
  * equal to one another, despite the fact that they each contain precisely the same set of values.
  * Similarly, empty ranges are not equal unless they have exactly the same representation, so
  * {@code [3..3)}, {@code (3..3]}, {@code (4..4]} are all unequal.
  */
  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Range) {
      Range<?> other = (Range<?>) object;
      return lowerBound.equals(other.lowerBound) && upperBound.equals(other.upperBound);
    }
    return false;
  }

  /** Returns a hash code for this range. */
  @Override
  public int hashCode() {
    return lowerBound.hashCode() * 31 + upperBound.hashCode();
  }

  /**
  * Returns a string representation of this range, such as {@code "[3..5)"} (other examples are
  * listed in the class documentation).
  */
  private String _cached;
  
  @Override
  public String toString() {
    if (_cached != null) return _cached;
    final C lowValue = lowerEndpoint(), highValue = upperEndpoint();
    final Formatter<C> formatter = getFormatter(lowValue);
    final BoundType lowerBoundType = lowerBound.typeAsLowerBound();
    final BoundType upperBoundType = upperBound.typeAsUpperBound();
    final char lowerEndChar = (lowerBoundType == BoundType.CLOSED)? '[': '(';
    final char upperEndChar = (upperBoundType == BoundType.CLOSED)? ']': ')';
    
    final StringBuilder sb = new StringBuilder(20);
    return (_cached = (sb
      .append(lowerEndChar)
      .append(formatter.apply(lowValue))
      .append("..")
      .append(formatter.apply(highValue))
      .append(upperEndChar)
    ).toString());
  }
  
  public static <CS extends Comparable> String toString(Cut<CS> c1, Cut<CS> c2) {
    final CS lowValue = c1.endpoint(), highValue = c2.endpoint();
    final Formatter<CS> formatter = getFormatter(lowValue);
    final BoundType lowerBoundType = c1.typeAsLowerBound();
    final BoundType upperBoundType = c2.typeAsUpperBound();
    final char lowerEndChar = (lowerBoundType == BoundType.CLOSED)? '[': '(';
    final char upperEndChar = (upperBoundType == BoundType.CLOSED)? ']': ')';

    return new StringBuilder(20)
      .append(lowerEndChar)
      .append(formatter.apply(lowValue))
      .append("..")
      .append(formatter.apply(highValue))
      .append(upperEndChar)
      .toString();
  }
  
  private String format = "<%s>";

  /**
  * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
  */
  private static <T> SortedSet<T> cast(Iterable<T> iterable) {
    return (SortedSet<T>) iterable;
  }

  Object readResolve() {
    if (this.equals(ALL)) {
      return all();
    } else {
      return this;
    }
  }

  // this method may throw CCE
  @SuppressWarnings("unchecked")
  static int compareOrThrow(Comparable left, Comparable right) {
    return left.compareTo(right);
  }

  /**
  * Needed to serialize sorted collections of Ranges.
  */
  private static class RangeLexOrdering extends Ordering<Range<?>> implements Serializable {

    @Override
    public int compare(Range<?> left, Range<?> right) {
      return ComparisonChain.start().compare(left.lowerBound, right.lowerBound).compare(left.upperBound, right.upperBound).result();
    }

    private static final long serialVersionUID = 0;
  }

  private static final long serialVersionUID = 0;

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, @Nullable final Object p2, @Nullable final Object p3, @Nullable final Object p4) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2, p3, p4));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, @Nullable final Object p2, @Nullable final Object p3) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2, p3));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, @Nullable final Object p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, final long p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, final int p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1, final char p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final long p1, @Nullable final Object p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final long p1, final long p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final long p1, final int p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final long p1, final char p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final int p1, @Nullable final Object p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final int p1, final long p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final int p1, final int p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final int p1, final char p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final char p1, @Nullable final Object p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final char p1, final long p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final char p1, final int p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final char p1, final char p2) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1, p2));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, @Nullable final Object p1) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final long p1) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final int p1) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T obj, @Nullable final String errorMessageTemplate, final char p1) {
    if (obj == null) {
      throw new NullPointerException(format(errorMessageTemplate, p1));
    }
    return obj;
  }

  
  public static <T> T checkNotNull(final T reference, @Nullable final String errorMessageTemplate, @Nullable final Object... errorMessageArgs) {
    if (reference == null) {
      throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
    }
    return reference;
  }

  
  public static <T> T checkNotNull(final T reference, @Nullable final Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  
  public static <T> T checkNotNull(final T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }
}



abstract class Cut<C extends Comparable>
    implements Comparable<Cut<C>>, 
               Serializable
{

  final C endpoint;

  private static final long serialVersionUID = 0L;

  Cut(@Nullable final C endpoint) {
    this.endpoint = endpoint;
  }

  abstract boolean isLessThan(final C p0);

  abstract BoundType typeAsLowerBound();

  abstract BoundType typeAsUpperBound();

  abstract Cut<C> withLowerBoundType(final BoundType p0, final DiscreteDomain<C> p1);

  abstract Cut<C> withUpperBoundType(final BoundType p0, final DiscreteDomain<C> p1);

  abstract void describeAsLowerBound(final StringBuilder p0, String format);
  
  void describeAsLowerBound(final StringBuilder sb) {
    describeAsLowerBound(sb, "%s");
  }

  abstract void describeAsUpperBound(final StringBuilder p0, String format);

  void describeAsUpperBound(final StringBuilder sb) {
    describeAsUpperBound(sb, "%s");
  }
  
  abstract C leastValueAbove(final DiscreteDomain<C> p0);

  abstract C greatestValueBelow(final DiscreteDomain<C> p0);

  Cut<C> canonical(final DiscreteDomain<C> domain) {
    return this;
  }

  @Override
  public int compareTo(final Cut<C> that) {
    if (that == belowAll()) {
      return 1;
    }
    if (that == aboveAll()) {
      return -1;
    }
    final int result = Range.compareOrThrow(this.endpoint, that.endpoint);
    if (result != 0) {
      return result;
    }
    return Booleans.compare(this instanceof AboveValue, that instanceof AboveValue);
  }

  C endpoint() {
    return this.endpoint;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Cut) {
      final Cut<C> that = (Cut<C>) obj;
      try {
        final int compareResult = this.compareTo(that);
        return compareResult == 0;
      } catch (ClassCastException ex) {
      }
    }
    return false;
  }

  static <C extends Comparable> Cut<C> belowAll() {
    return (Cut<C>) BelowAll.INSTANCE;
  }

  static <C extends Comparable> Cut<C> aboveAll() {
    return (Cut<C>) AboveAll.INSTANCE;
  }

  static <C extends Comparable> Cut<C> belowValue(final C endpoint) {
    return new BelowValue<C>(endpoint);
  }

  static <C extends Comparable> Cut<C> aboveValue(final C endpoint) {
    return new AboveValue<C>(endpoint);
  }

  private static final class BelowAll extends Cut<Comparable<?>> {

    private static final BelowAll INSTANCE;

    private static final long serialVersionUID = 0L;

    private BelowAll() {
      super(null);
    }

    @Override
    Comparable<?> endpoint() {
      throw new IllegalStateException("range unbounded on this side");
    }

    @Override
    boolean isLessThan(final Comparable<?> value) {
      return true;
    }

    @Override
    BoundType typeAsLowerBound() {
      throw new IllegalStateException();
    }

    @Override
    BoundType typeAsUpperBound() {
      throw new AssertionError((Object) "this statement should be unreachable");
    }

    @Override
    Cut<Comparable<?>> withLowerBoundType(final BoundType boundType, final DiscreteDomain<Comparable<?>> domain) {
      throw new IllegalStateException();
    }

    @Override
    Cut<Comparable<?>> withUpperBoundType(final BoundType boundType, final DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError((Object) "this statement should be unreachable");
    }

    @Override
    void describeAsLowerBound(final StringBuilder sb, String format) {
      sb.append("(-?");
    }

    @Override
    void describeAsUpperBound(final StringBuilder sb, String format) {
      throw new AssertionError();
    }

    @Override
    Comparable<?> leastValueAbove(final DiscreteDomain<Comparable<?>> domain) {
      return domain.minValue();
    }

    @Override
    Comparable<?> greatestValueBelow(final DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError();
    }

    @Override
    Cut<Comparable<?>> canonical(final DiscreteDomain<Comparable<?>> domain) {
      try {
        return Cut.belowValue(domain.minValue());
      } catch (NoSuchElementException e) {
        return this;
      }
    }

    @Override
    public int compareTo(final Cut<Comparable<?>> o) {
      return (o == this) ? 0 : -1;
    }

    @Override
    public String toString() {
      return "-?";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    static {
      INSTANCE = new BelowAll();
    }
  }

  private static final class AboveAll extends Cut<Comparable<?>> {

    private static final AboveAll INSTANCE;

    private static final long serialVersionUID = 0L;

    private AboveAll() {
      super(null);
    }

    @Override
    Comparable<?> endpoint() {
      throw new IllegalStateException("range unbounded on this side");
    }

    @Override
    boolean isLessThan(final Comparable<?> value) {
      return false;
    }

    @Override
    BoundType typeAsLowerBound() {
      throw new AssertionError((Object) "this statement should be unreachable");
    }

    @Override
    BoundType typeAsUpperBound() {
      throw new IllegalStateException();
    }

    @Override
    Cut<Comparable<?>> withLowerBoundType(final BoundType boundType, final DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError((Object) "this statement should be unreachable");
    }

    @Override
    Cut<Comparable<?>> withUpperBoundType(final BoundType boundType, final DiscreteDomain<Comparable<?>> domain) {
      throw new IllegalStateException();
    }

    @Override
    void describeAsLowerBound(final StringBuilder sb, String format) {
      throw new AssertionError();
    }

    @Override
    void describeAsUpperBound(final StringBuilder sb, String format) {
      sb.append("+?)");
    }

    @Override
    Comparable<?> leastValueAbove(final DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError();
    }

    @Override
    Comparable<?> greatestValueBelow(final DiscreteDomain<Comparable<?>> domain) {
      return domain.maxValue();
    }

    @Override
    public int compareTo(final Cut<Comparable<?>> o) {
      return (o != this) ? 1 : 0;
    }

    @Override
    public String toString() {
      return "+?";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    static {
      INSTANCE = new AboveAll();
    }
  }

  private static final class BelowValue<C extends Comparable> extends Cut<C> {

    private static final long serialVersionUID = 0L;

    BelowValue(final C endpoint) {
      super(Range.checkNotNull((C) endpoint));
    }

    @Override
    boolean isLessThan(final C value) {
      return Range.compareOrThrow(this.endpoint, value) <= 0;
    }

    @Override
    BoundType typeAsLowerBound() {
      return BoundType.CLOSED;
    }

    @Override
    BoundType typeAsUpperBound() {
      return BoundType.OPEN;
    }

    @Override
    Cut<C> withLowerBoundType(final BoundType boundType, final DiscreteDomain<C> domain) {
      switch(boundType) {
        case CLOSED:
          {
            return this;
          }
        case OPEN:
          {
            final C previous = domain.previous(this.endpoint);
            return (previous == null) ? Cut.belowAll() : new AboveValue<C>(previous);
          }
        default:
          {
            throw new AssertionError();
          }
      }
    }

    @Override
    Cut<C> withUpperBoundType(final BoundType boundType, final DiscreteDomain<C> domain) {
      switch(boundType) {
        case CLOSED:
          {
            final C previous = domain.previous(this.endpoint);
            return (previous == null) ? Cut.aboveAll() : new AboveValue<C>(previous);
          }
        case OPEN:
          {
            return this;
          }
        default:
          {
            throw new AssertionError();
          }
      }
    }

    @Override
    void describeAsLowerBound(final StringBuilder sb, String format) {
      sb.append('[').append(format(
        this.endpoint instanceof Number? format: "%s",
        this.endpoint
      ));
    }
    
   
    @Override
    void describeAsUpperBound(final StringBuilder sb, String format) {
      sb.append(format(
        this.endpoint instanceof Number? format: "%s",
        this.endpoint
      )).append(')');
    }

    @Override
    C leastValueAbove(final DiscreteDomain<C> domain) {
      return this.endpoint;
    }

    @Override
    C greatestValueBelow(final DiscreteDomain<C> domain) {
      return domain.previous(this.endpoint);
    }

    @Override
    public int hashCode() {
      return this.endpoint.hashCode();
    }

    @Override
    public String toString() {
      return "\\" + this.endpoint + "/";
    }
  }

  private static final class AboveValue<C extends Comparable> extends Cut<C> {

    private static final long serialVersionUID = 0L;

    AboveValue(final C endpoint) {
      super(Range.checkNotNull((C) endpoint));
    }

    @Override
    boolean isLessThan(final C value) {
      return Range.compareOrThrow(this.endpoint, value) < 0;
    }

    @Override
    BoundType typeAsLowerBound() {
      return BoundType.OPEN;
    }

    @Override
    BoundType typeAsUpperBound() {
      return BoundType.CLOSED;
    }

    @Override
    Cut<C> withLowerBoundType(final BoundType boundType, final DiscreteDomain<C> domain) {
      switch(boundType) {
        case OPEN:
          {
            return this;
          }
        case CLOSED:
          {
            final C next = domain.next(this.endpoint);
            return (next == null) ? Cut.belowAll() : Cut.belowValue(next);
          }
        default:
          {
            throw new AssertionError();
          }
      }
    }

    @Override
    Cut<C> withUpperBoundType(final BoundType boundType, final DiscreteDomain<C> domain) {
      switch(boundType) {
        case OPEN:
          {
            final C next = domain.next(this.endpoint);
            return (next == null) ? Cut.aboveAll() : Cut.belowValue(next);
          }
        case CLOSED:
          {
            return this;
          }
        default:
          {
            throw new AssertionError();
          }
      }
    }

    @Override
    void describeAsLowerBound(final StringBuilder sb, String format) {
      sb.append('(').append(format(
        this.endpoint instanceof Number? format: "%s",
        this.endpoint
      ));
    }

    @Override
    void describeAsUpperBound(final StringBuilder sb, String format) {
      sb.append(format(
        this.endpoint instanceof Number? format: "%s",
        this.endpoint
      )).append(']');
    }

    @Override
    C leastValueAbove(final DiscreteDomain<C> domain) {
      return domain.next(this.endpoint);
    }

    @Override
    C greatestValueBelow(final DiscreteDomain<C> domain) {
      return this.endpoint;
    }

    @Override
    Cut<C> canonical(final DiscreteDomain<C> domain) {
      final C next = this.leastValueAbove(domain);
      return (next != null) ? Cut.belowValue(next) : Cut.aboveAll();
    }

    @Override
    public int hashCode() {
      return ~this.endpoint.hashCode();
    }

    @Override
    public String toString() {
      return "/" + this.endpoint + "\\";
    }
  }
}