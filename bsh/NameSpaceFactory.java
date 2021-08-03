package bsh;

import bsh.Factory;
import bsh.NameSpace;

public class NameSpaceFactory extends Factory<NameSpace> {
  protected static final NameSpaceFactory INSTANCE = new NameSpaceFactory();

  public NameSpaceFactory() {
    this.clazz = NameSpace.class;
  }
}
