package org.d6r;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;


public interface WhereFilter<E> {
  
  public static class Impl<T>
    implements WhereFilter<T>, BooleanPredicate<T>
  {
    protected final BooleanPredicate<T> pred;
    protected final List<Pair<T, Throwable>> errors
         = new ArrayList<Pair<T, Throwable>>();
    
    public Impl(BooleanPredicate pred) {
      this.pred = pred;
    }
    
    @Override
    public boolean test(T element) {
      return pred.test(element);
    }
    
    @Override
    public List<T> get(Iterable<? extends T> itb) {
      final List<T> list = new ArrayList<T>();
      for (final T next: itb) {
        if (next == null) continue; 
        try {
          if (! this.test(next)) continue;
          list.add(next);
        } catch (Throwable e) { 
          errors.add(Pair.of(next, e));
        }
      }
      return list;
    }
    
    public List<Pair<T, Throwable>> getErrors() {
      return Collections.unmodifiableList(errors);
    }
  }
  
  
  List<E> get(Iterable<? extends E> iterable);
  
}

