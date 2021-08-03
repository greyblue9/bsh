package bsh;

import bsh.SimpleNode;

class BSHPrimitiveType extends SimpleNode {
  public Class type;

  BSHPrimitiveType(int id) {
    super(id);
  }

  public Class getType() {
    return this.type;
  }
}
