package bsh.operators;

public enum OperatorType {
  PLUS("+", "plus", true),
  MINUS("-", "minus", true),
  TIMES("*", "times", true),
  DIVIDE("/", "divide", true),
  PLUS_EQUALS("+ = ", "plusEquals", false),
  MINUS_EQUALS("- = ", "minusEquals", false),
  TIMES_EQUALS("* = ", "timesEquals", false),
  DIVIDE_EQUALS("/ = ", "divideEquals", false),
  GETAT("[]", "getAt", false),
  PUTAT("[]", "putAt", false),
  CAST("()", "cast", false),
  UMINUS("-", "negate", true),
  POWER("**", "power", true),
  RANGE("..", "range", true);

  private String operator;
  private String methodName;
  private boolean allowLeftCast;

  private OperatorType(String symbol, String methodName, boolean allowLeftCast) {
    this.operator = symbol;
    this.methodName = methodName;
    this.allowLeftCast = allowLeftCast;
  }

  public String getSymbol() {
    return this.operator;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public boolean getAllowLeftCast() {
    return this.allowLeftCast;
  }

  public static OperatorType getType(int kind) {
    switch(kind) {
    case 103:
      return PLUS;
    case 104:
      return MINUS;
    case 105:
      return TIMES;
    case 106:
      return DIVIDE;
    case 107:
    case 108:
    case 109:
    case 110:
    case 111:
    case 112:
    case 113:
    case 114:
    case 115:
    case 116:
    case 117:
    case 118:
    default:
      return null;
    case 119:
      return PLUS_EQUALS;
    case 120:
      return MINUS_EQUALS;
    case 121:
      return TIMES_EQUALS;
    case 122:
      return DIVIDE_EQUALS;
    }
  }

  public static OperatorType find(String mname) {
    if(mname == null) {
      return null;
    } else {
      OperatorType[] types = values();

      for(int i = 0; i < types.length; ++i) {
        if(mname.equals(types[i].getMethodName())) {
          return types[i];
        }
      }

      return null;
    }
  }
}
