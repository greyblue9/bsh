package bsh.operators.standard;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapMethods implements Serializable {
  public static Object getAt(Map map, Object key) {
    return map.get(key);
  }

  public static Map getAt(Map map, Object[] keys) {
    LinkedHashMap result = new LinkedHashMap(keys.length * 4 / 3 + 3);

    for(int i = 0; i < keys.length; ++i) {
      Object key = keys[i];
      if(key != null) {
        result.put(key, map.get(key));
      }
    }

    return result;
  }

  public static void putAt(Map map, Object key, Object value) {
    map.put(key, value);
  }

  public static void putAt(Map map, Object[] keys, Map values) {
    for(int i = 0; i < keys.length; ++i) {
      Object key = keys[i];
      if(key != null) {
        map.put(key, values.get(key));
      }
    }

  }

  public static void putAt(Map map, Object[] keys, Object[] values) {
    if(keys.length != values.length) {
      throw new IllegalArgumentException("Number of key and value pairs does not match");
    } else {
      for(int i = 0; i < keys.length; ++i) {
        Object key = keys[i];
        if(key != null) {
          map.put(key, values[i]);
        }
      }

    }
  }
}
