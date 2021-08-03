package org.d6r;

public interface Func<V, R>
         extends com.google.common.base.Function<V, R>,
                 java8.util.function.Function<V, R>
{
  R apply(V value);
}

