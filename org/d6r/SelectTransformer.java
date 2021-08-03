package org.d6r;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.Transformer;

public interface SelectTransformer<V, R>
         extends Transformer<V, R> 
{
  public static class Default<V1, R1> 
           implements SelectTransformer<V1, R1>
  {
    protected final Transformer<V1, R1> transformer;
    protected final java8.util.function.Function<V1, R1> function8;
    protected final com.google.common.base.Function<V1, R1> functionG;
    
    public boolean KEEP_ERRORS;
    public Collection<Throwable> errors;
    
    
    public Default(@Nonnull Transformer<V1, R1> transformer) {
      if (transformer == null) {
        throw new NullPointerException("transformer == null");
      }
      this.transformer = transformer;
      this.function8 = null;
      this.functionG = null;
    }
    
    public Default(@Nonnull java8.util.function.Function<V1, R1> function8) {
      if (function8 == null) {
        throw new NullPointerException("function8 == null");
      }
      this.transformer = null;
      this.function8 = function8;
      this.functionG = null;
    }
    
    public Default(
    @Nonnull com.google.common.base.Function<V1, R1> functionG) 
    {
      if (functionG == null) {
        throw new NullPointerException("functionG == null");
      }
      this.transformer = null;
      this.function8 = null;
      this.functionG = functionG;
    }
    
    @Override
    @Nonnull
    public List<R1> select(Iterable<? extends V1> iterable) {
      List<R1> outputs = new LinkedList<R1>();
      for (Iterator<? extends V1> it = iterable.iterator(); it.hasNext();) {
        try {
          V1 value = it.next();
          if (value == null) continue;
          R1 outputValue = transform(value);
          if (outputValue == null) continue;
          outputs.add(outputValue);
        } catch (Throwable ex) {
          if (KEEP_ERRORS) {
            if (errors == null) errors = new LinkedList<Throwable>();
            errors.add(ex);
          }
        }
      }
      return outputs;
    }
    
    @Override
    @Nullable
    public R1 transform(V1 value) {
      return (transformer != null)
        ? (R1) transformer.transform(value)
        : ((functionG != null)
            ? (R1) functionG.apply(value)
            : (R1) function8.apply(value));
    }
  }
  
  @Nonnull
  List<R> select(Iterable<? extends V> iterable);
  
  @Override
  @Nullable
  R transform(V value);
}




