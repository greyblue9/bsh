package org.d6r;

public class ConstructorTest {
  
  public String name;
  public boolean boolVal;
  
  public ConstructorTest(String name) {
    this.name = name != null? name: "default";
    System.err.printf(
      "ConstructorTest::<init>(String name = \"%s\")\n",
      name
    );
  }
  
  public ConstructorTest(String name, boolean boolVal) {
    this.name = name != null? name: "default";
    this.boolVal = boolVal;
    System.err.printf(
      "ConstructorTest::<init>("
      + "String name = \"%s\", boolean boolVal = %s)\n",
      name,
      Boolean.valueOf(boolVal)
    );
  }
  
  public ConstructorTest(String name, Boolean boolVal) {
    this.name = name != null? name: "default";
    this.boolVal = boolVal != null? 
      boolVal.booleanValue(): Boolean.TRUE;
    System.err.printf(
      "ConstructorTest::<init>("
      + "String name = \"%s\", Boolean boolVal = %s)\n",
      name,
      boolVal
    );    
  }
  
  @Override
  public String toString() {
    return String.format(
      "ConstructorTest{"
      + "name: \"%s\", boolVal: %s}", 
      name,
      Boolean.valueOf(boolVal)
    );
  }
  
}
