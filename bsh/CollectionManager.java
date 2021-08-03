package bsh;

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public final class CollectionManager {
  private static final CollectionManager manager = new CollectionManager();

  public static synchronized CollectionManager getCollectionManager() {
    return manager;
  }

  public boolean isBshIterable(Object obj) {
    try {
      this.getBshIterator(obj);
      return true;
    } catch (IllegalArgumentException var3) {
      return false;
    }
  }

  public Iterator getBshIterator(final Object obj) throws IllegalArgumentException {
    if(obj == null) {
      throw new NullPointerException("Cannot iterate over null.");
    } else if(obj instanceof Enumeration) {
      final Enumeration array = (Enumeration)obj;
      return new Iterator() {
        public boolean hasNext() {
          return array.hasMoreElements();
        }

        public Object next() {
          return array.nextElement();
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    } else if(obj instanceof Iterator) {
      return (Iterator)obj;
    } else if(obj instanceof Iterable) {
      return ((Iterable)obj).iterator();
    } else if(obj.getClass().isArray()) {
      return new Iterator() {
        private int index = 0;
        private final int length = Array.getLength(obj);

        public boolean hasNext() {
          return this.index < this.length;
        }

        public Object next() {
          return Array.get(obj, this.index++);
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    } else if(obj instanceof CharSequence) {
      return this.getBshIterator(obj.toString().toCharArray());
    } else {
      throw new IllegalArgumentException("Cannot iterate over object of type " + obj.getClass());
    }
  }

  public boolean isMap(Object obj) {
    return obj instanceof Map;
  }

  public Object getFromMap(Object map, Object key) {
    return ((Map)map).get(key);
  }

  public Object putInMap(Object map, Object key, Object value) {
    return ((Map)map).put(key, value);
  }
}
