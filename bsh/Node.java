package bsh;

import java.io.Serializable;

interface Node extends Serializable {
  void jjtOpen();

  void jjtClose();

  void jjtSetParent(Node var1);

  <N extends Node> N jjtGetParent();

  void jjtAddChild(Node var1, int var2);

  <N extends Node> N jjtGetChild(int var1);

  int jjtGetNumChildren();
}
