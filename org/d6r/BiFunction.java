package org.d6r;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import com.google.common.base.Function;

/**
  Determines an output value based on two input values; 
  a pre-Java-8 version of {@code java.util.function.BiFunction}.
 
  The {@link Functions} class provides common functions and
  related utilites (for the unary Function<V,R> interface only).
 
  See the Guava User Guide article on
  https://github.com/google/guava/wiki/FunctionalExplained
  (The use of {@code Function}).
 
  This interface is now a legacy type. 
  Use {@code java.util.function.Function} (or the appropriate
  primitive specialization such as {@code ToIntFunction}) instead
  whenever possible.
  
  Otherwise, at least reduce explicit dependencies on this type 
  by using lambda expressions or method references instead of 
  classes, leaving your code easier to migrate in the future.
 
  To use an existing function (say, named {@code function}) in a 
  context where the other type of function is expected, use the 
  method reference {@code function::apply}. A future version of
  {@code com.google.common.base.Function} will be made to extend
  {@code java.util.function.Function}, making conversion code
  necessary only in one direction. At that time, this interface
  will be officially discouraged.
 
  @author Kevin Bourrillion
  @since 2.0
*/
public interface BiFunction<V, W, R> {
  
  /**
    Returns the result of applying this function to {@code 
    input}. This method is generally expected, but not absolutely
    required, to have the following properties:
       
      - Its execution does not cause any observable side effects.
      - The computation is consistent with equals(Object); 
        that is, when
        
          Objects.equal(
            Pair.of(a, b), Pair.of(c, d)
          ) --> true
        
        it is implied that:
        
          Objects.equal(
            BiFunction.apply(a, b), BiFunction.apply(c, d)
          ) --> true
    
    @throws NullPointerException: if {@code input} is null and
    this function does not accept null arguments
  */
  @Nullable
  R apply(@Nullable V input1, @Nullable W input2);
  
  /**
    Indicates whether another object is equal to this function.
   
    Most implementations will have no reason to override the behavior of {@link Object#equals}.
    However, an implementation may also choose to return {@code true} whenever {@code object} is a
    {@link Function} that it considers interchangeable with this one. "Interchangeable"
    typically means that {@code Objects.equal(this.apply(f), that.apply(f))} is true for all
    {@code f} of type {@code F}. Note that a {@code false} result from this method does not imply
    that the functions are known not to be interchangeable.
  */
  @Override
  boolean equals(@Nullable Object object);
}


class BiFunctions {
  
  public static class Partial<A, B, T>
    implements Function<B, T>
  {
    final A input1;
    final BiFunction<A, B, T> biFunction;
    
    public Partial(final BiFunction<A, B, T> biFunc, A input1) {
      this.input1 = input1;
      this.biFunction = biFunc;
    }
    
    @Override
    public T apply(@Nullable B input2) {
      return biFunction.apply(input1, input2);
    }
    
    public Function<B, T> toFunction() {
      return (Function<B, T>) (Object) this;
    }
  }
  
  @Nullable
  public static <V, W, R> Function<W, R>
  curry(final BiFunction<V, W, R> bifn, @Nullable final V input1)
  {
    return new Partial<V, W, R>(bifn, input1);    
  }
  
}

