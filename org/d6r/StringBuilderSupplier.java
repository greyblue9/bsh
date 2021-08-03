package org.d6r;

import java.util.IdentityHashMap;
import java.util.*;
import java.io.*;
import javax.annotation.*;
import com.strobel.core.SafeCloseable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.base.*;


public class StringBuilderSupplier
  implements CloseableSupplier<StringBuilder>
{
  protected static final ThreadLocal<StringBuilderCache> LEGACY_CACHE =
  new ThreadLocal<StringBuilderCache>() {
    @Override
    public StringBuilderCache initialValue() {
      return new StringBuilderCache();
    }
  };
  
  @Nonnull
  public static CloseableSupplier<StringBuilder> getInstance() {
    return new StringBuilderSupplier(LEGACY_CACHE.get());
  }
  
  
  private boolean _released;
  protected final StringBuilderCache _cache;
  protected final Set<StringBuilder> _owned = new IdentityHashSet<>();
  
  
  protected StringBuilderSupplier(final @Nullable StringBuilderCache cache) {
    this._cache = (cache != null) ? cache : LEGACY_CACHE.get();
  }
  
  @Override
  public StringBuilder get() {
    if (_released) throw new IllegalStateException(
      "StringBuilderSupplier.get() was called on an " +
      "already-_released StringBuilderSupplier."
    );
    final StringBuilder sb = _cache.getStringBuilder();
    if (sb == null) throw new UnknownError(
      "StringBuilder cache returned null; you may have used up all of its" +
      "available StringBuilders!"
    );
    _owned.add(sb);
    return sb;
  }
  
  @Override
  public void close() {
    try {
      for (final StringBuilder sb: _owned) {
        _cache.releaseStringBuilder(sb);
      }
    } finally {
      _released = true;
      _owned.clear();
    }     
  }
  
  
}
  
/*
public static class StringBuilderCache {
 final StringBuilder[] _sbs;
volatile int inUse = 0;
volatile int inUseCount = 0;
final int capacity;
static final int MAX_CAPACITY = 32;
final @Nonnull Map<StringBuilder, Integer> _owned
= new IdentityHashMap<>();
public StringBuilderCache(final int capacity) {
_capacity = Math.min(MAX_CAPACITY, Math.max(capacity, 4));
_sbs = new StringBuilder[_capacity];
}
public StringBuilder getStringBuilder() {
final StringBuilder sb;
int index;
try {
  int bit = 1;
  index = 0;
  do {
    if ((inUse & bit) == 0) {
      inUse |= bit;
      final StringBuilder sb;
      if (_sbs[index] == null) _sbs[index] = new StringBuilder(256);
      else (sb = _sbs[index]).setLength(0);
      return sb;
    }
  } while ((bit <<= 1) != 0 && ++index != capacity);
  growCapacity();
  return (sb = getStringBuilder());
} finally {
  _owned.put(sb, Integer.valueOf(index));
}
}

public void releaseStringBuilder(final StringBuilder sb) {
final Integer oIndex = _owned.get(sb);
final int index;

if (oIndex == null || _sbs[(index = oIndex.intValue())] != sb) {
  throw new IllegalArgumentException(String.format(
    "%s@%08x: An attempt was made to release an object to a non-owning" +
    "cache. (oIndex: %d, _sbs: sb: StringBuilder@%08x[%s], _owned = %s)", 
    ClassInfo.getSimpleName(this), System.identityHashCode(this),
    oIndex, Debug.ToString(_sbs),
    System.identityHashCode(sb), TextUtil.str(sb), _owned
  ));
}
final int bit = Math.pow(2, index);
if ((inUse & bit) == 0) throw new IllegalStateException(String.format(
  "expected (inUse & bit) != 0, but got %3$d (0x%3$X). " +
  "inUse = %1$d (0x%1$X), bit = %2$d (0x%2$X)",
  inUse, bit, (inUse & bit)
));
inUse ^= bit;
}
}
*/
