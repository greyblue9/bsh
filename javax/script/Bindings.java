package javax.script;

import java.util.Map;

/**
A mapping of key/value pairs, all of whose keys are
Strings.
@author Mike Grogan
@since 1.6
*/
public interface Bindings extends Map<String, Object> {

  /**
  Set a named value.
  @param name The name associated with the value.
  @param value The value associated with the name.
  @return The value previously associated with the given name.
  Returns null if no value was previously associated with the name.
  @throws NullPointerException if the name is null.
  @throws IllegalArgumentException if the name is empty String.
  */
  public Object put(String name, Object value);

  /**
  Adds all the mappings in a given Map to this Bindings.
  @param toMerge The Map to merge with this one.
  @throws NullPointerException
  if toMerge map is null or if some key in the map is null.
  @throws IllegalArgumentException
  if some key in the map is an empty String.
  */
  public void putAll(Map<? extends String, ? extends Object> toMerge);

  /**
  Returns true if this map contains a mapping for the specified
  key.  More formally, returns true if and only if
  this map contains a mapping for a key k such that
  (key==null ? k==null : key.equals(k)).  (There can be
  at most one such mapping.)
  @param key key whose presence in this map is to be tested.
  @return true if this map contains a mapping for the specified
  key.
  @throws NullPointerException if key is null
  @throws ClassCastException if key is not String
  @throws IllegalArgumentException if key is empty String
  */
  public boolean containsKey(Object key);

  /**
  Returns the value to which this map maps the specified key.  Returns
  null if the map contains no mapping for this key.  A return
  value of null does not necessarily indicate that the
  map contains no mapping for the key; it's also possible that the map
  explicitly maps the key to null.  The containsKey
  operation may be used to distinguish these two cases.
  More formally, if this map contains a mapping from a key
  k to a value v such that (key==null ? k==null :
  key.equals(k)), then this method returns v; otherwise
  it returns null.  (There can be at most one such mapping.)
  @param key key whose associated value is to be returned.
  @return the value to which this map maps the specified key, or
  null if the map contains no mapping for this key.
  @throws NullPointerException if key is null
  @throws ClassCastException if key is not String
  @throws IllegalArgumentException if key is empty String
  */
  public Object get(Object key);

  /**
  Removes the mapping for this key from this map if it is present
  (optional operation).   More formally, if this map contains a mapping
  from key k to value v such that
  (key==null ?  k==null : key.equals(k)), that mapping
  is removed.  (The map can contain at most one such mapping.)
  Returns the value to which the map previously associated the key, or
  null if the map contained no mapping for this key.  (A
  null return can also indicate that the map previously
  associated null with the specified key if the implementation
  supports null values.)  The map will not contain a mapping for
  the specified  key once the call returns.
  @param key key whose mapping is to be removed from the map.
  @return previous value associated with specified key, or null
  if there was no mapping for key.
  @throws NullPointerException if key is null
  @throws ClassCastException if key is not String
  @throws IllegalArgumentException if key is empty String
  */
  public Object remove(Object key);
}