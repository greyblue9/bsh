package bsh;

import bsh.ExternalNameSpace;
import bsh.Factory;

public class ExternalNameSpaceFactory extends Factory<ExternalNameSpace> {
  protected static final ExternalNameSpaceFactory INSTANCE = new ExternalNameSpaceFactory();

  public ExternalNameSpaceFactory() {
    this.clazz = ExternalNameSpace.class;
  }
}
