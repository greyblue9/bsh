package org.d6r;

import java.lang.reflect.Type;

public class DummyType implements Type {
  final String _name;
  
  DummyType(final String name) {
    this._name = name;
  }
  
  @Override
  public String toString() {
    if (this._name != null) {
      return this._name;
    }
    return "(no name)";
  }
}


