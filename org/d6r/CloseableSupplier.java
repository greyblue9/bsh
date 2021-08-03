package org.d6r;


public interface CloseableSupplier<R>
         extends com.google.common.base.Supplier<R>,
                 com.strobel.functions.Supplier<R>,
                 java8.util.function.Supplier<R>,
                 java.io.Closeable,
                 AutoCloseable //,
                 // java.util.function.Supplier<R>
{
  @Override void close();
  
  @Override R get();
}


