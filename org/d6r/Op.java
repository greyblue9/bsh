package org.d6r;

enum Op {
  
  A("a"),
  A_A("aA"),
  ARGS("args"),
  B("b"),
  B_B("bB"),
  C_C("cC"),
  CASES("cases"),
  DIST_REG("distReg"),
  ELEM_WIDTH("elemWidth"),
  END("end"),
  FIELD("field"),
  FIRSTCASE("first_case"),
  FROM_OR_TO_REG("fromOrToReg"),
  FROM_REG("fromReg"),
  HANDLERS("handlers"),
  INIT_LENGTH("initLength"),
  LABEL("label"),
  LABELS("labels"),
  LASTCASE("last_case"),
  LINE("line"),
  METHOD("method"),
  NAME("name"),
  OBJ_REG("objReg"),
  OP_AGET("opAget"),
  OP_CHECK_CAST("opCheckCast"),
  OP_CONST("opConst"),
  OP_GOTO("opGoto"),
  OPCODE("opcode"),
  R1("r1"),
  R2("r2"),
  REG("reg"),
  SAVE_TO("saveTo"),
  SIGNATURE("signature"),
  START("start"),
  TO_REG("toReg"),
  TOTAL("total"),
  TYPE("type"),
  TYPES("types"),
  VALUE("value"),
  VALUES("values"),
  XT("xt"),
  XTA("xta"),
  XTB("xtb");
  
  private final String text;

  /**
  @param text
  */
  private Op(final String text) {
    this.text = text;
  }
  
  /**
  (non-Javadoc)
  @see java.lang.Enum#toString()
  */
  @Override
  public String toString() {
    return text;
  }
  
  /**
  @return op name
  */
  public String getName() {
    return text;
  }
}
  