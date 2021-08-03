package org.d6r;

import com.googlecode.dex2jar.DexType;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.reader.io.ArrayDataIn;
import com.googlecode.dex2jar.visitors.DexFileVisitor;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexLabel;

import org.d6r.Insn.LHSKind;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
 
public class CodeVisitor implements DexCodeVisitor {
  
  static final Random RANDOM 
    = new Random(System.currentTimeMillis());
  
  
  public String clsName;
  public Method method;
  public Insn[] insns = new Insn[100];
  
  public class InsnFactory {
    int nextInsnIdx = 0;
    
    int prevLine = 0;
    int currentLine = 0;
    
    @SafeVarargs    
    public final Insn newInsn(String name, Object... args) {
      Insn insn = new Insn(
        clsName, method, nextInsnIdx, name, args
      );
      insn.lineNumber = currentLine;
      return insn;
    }
    
    public void atLine(int lineNo) {
      prevLine = currentLine;
      currentLine = lineNo;
    }
    
    public void add(Insn insn) {
      if (nextInsnIdx == insns.length) {
        Insn[] newArr = new Insn[insns.length * 4];
        if (RANDOM.nextInt() % 15 == 0) System.gc();
        /*System.err.printf(
          "Instruction list growing: %d --> %d\n",
          insns.length, newArr.length);*/
        System.arraycopy(insns, 0, newArr, 0, insns.length);
        insns = newArr;
      }
      insns[nextInsnIdx++] = insn;
      insn.finish();
    }
  }
  
  final InsnFactory insnFactory;
  
  public int getLength() {
    return insnFactory.nextInsnIdx;
  }
  
  public int size() {
    return insnFactory.nextInsnIdx;
  }
  
  public List<Insn> getInsns() {
    Insn[] insnArr = new Insn[ insnFactory.nextInsnIdx ];
    System.arraycopy(insns, 0, insnArr, 0, insnArr.length);
    Arrays.sort(insnArr, Insn.OFFSET_COMPARATOR);
    return Arrays.asList(insnArr);
  }
  
  public List<Insn> getInsns(String name) {
    int i = -1;
    Insn crnt;
    ArrayList<Insn> insnList = new ArrayList<Insn>(12);
    while (++i < insns.length && (crnt = insns[i]) != null) {
      if (! crnt.name.equals(name)) continue; 
      insnList.add(crnt);
    }
    Collections.sort(insnList, Insn.OFFSET_COMPARATOR);
    return insnList;
  }
  
  public CodeVisitor(String clsName, Method method) {
    this.clsName = clsName;
    this.method = method;
    this.insnFactory = this.new InsnFactory();
  }
  
  @Override
  public void visitArrayStmt(int opAget, int formOrToReg,
  int arrayReg, int indexReg, int xt) {
    // handle ArrayStmt here
    LHSKind lhsKind;
    switch(opAget) {
      case 75:
        // array[index] = locals[from]
        lhsKind = LHSKind.Array; 
        break;
      case 68:
        // locals[from] = array[index]
        lhsKind = LHSKind.Local; 
        break;
      default: lhsKind = null; break;
    }
    Insn newArrayStmt = insnFactory.newInsn(
      "ArrayStmt", 
      opAget, formOrToReg, arrayReg, indexReg, xt
    );    
    newArrayStmt.setArgNames(
      "opAget", 
      "formOrToReg", "arrayReg", "indexReg", "xt"
    );
    newArrayStmt.lhsKind = lhsKind;
    newArrayStmt.opcode = opAget;
    insnFactory.add(newArrayStmt);        
  }

  @Override
  public void visitBinopLitXStmt(int opcode, int aA, int bB,
  int cC) {
    // handle BinopLitXStmt here
    Insn newBinopLitXStmt = insnFactory.newInsn(
      "BinopLitXStmt", opcode, aA, bB, cC 
    ); 
    newBinopLitXStmt.setArgNames(
      "opcode", "aA", "bB", "cC"
    );
    newBinopLitXStmt.opcode = opcode;
    insnFactory.add(newBinopLitXStmt);
  }

  @Override
  public void visitBinopStmt(int opcode, int toReg, int r1,
  int r2, int xt) {
    // handle BinopStmt here
    Insn newBinopStmt = insnFactory.newInsn(
      "BinopStmt", opcode, toReg, r1, r2, xt 
    ); 
    newBinopStmt.setArgNames(
      "opcode", "toReg", "r1", "r2", "xt"
    );
    newBinopStmt.opcode = opcode;
    insnFactory.add(newBinopStmt);
  }

  @Override
  public void visitClassStmt(int opcode, int a, int b,
  String type) {
    // handle ClassStmt here
    Insn newClassStmt = insnFactory.newInsn(
      "ClassStmt", opcode, a, b, type 
    ); 
    newClassStmt.setArgNames(
      "opcode", "a", "b", "type"
    );
    newClassStmt.opcode = opcode;
    insnFactory.add(newClassStmt);
  }

  @Override
  public void visitClassStmt(int opCheckCast, int saveTo,
  String type) {
    // handle ClassStmt here
    Insn newClassStmt = insnFactory.newInsn(
      "ClassStmt", opCheckCast, saveTo, type 
    ); 
    newClassStmt.setArgNames(
      "opCheckCast", "saveTo", "type"
    );
    newClassStmt.opcode = opCheckCast;
    insnFactory.add(newClassStmt);
  }

  @Override
  public void visitCmpStmt(int opcode, int distReg,
  int bB, int cC, int xt) {
    // handle CmpStmt here
    Insn newCmpStmt = insnFactory.newInsn(
      "CmpStmt", opcode, distReg, bB, cC, xt 
    ); 
    newCmpStmt.setArgNames(
      "opcode", "distReg", "bB", "cC", "xt"
    );
    newCmpStmt.opcode = opcode;
    insnFactory.add(newCmpStmt);
  }

  @Override
  public void visitConstStmt(int opConst, int toReg,
  Object value, int xt) {
    // handle ConstStmt here
    Insn newConstStmt = insnFactory.newInsn(
      "ConstStmt", opConst, toReg, value, xt 
    ); 
    newConstStmt.setArgNames(
      "opConst", "toReg", "value", "xt"
    );
    newConstStmt.opcode = opConst;
    insnFactory.add(newConstStmt);
  }

  @Override
  public void visitFieldStmt(int opcode, int fromOrToReg,
  Field field, int xt) {
    // handle FieldStmt here
    Insn newFieldStmt = insnFactory.newInsn(
      "FieldStmt", opcode, fromOrToReg, field, xt 
    ); 
    newFieldStmt.setArgNames(
      "opcode", "fromOrToReg", "field", "xt"
    );
    newFieldStmt.opcode = opcode;    
    insnFactory.add(newFieldStmt);
  }

  @Override
  public void visitFieldStmt(int opcode, int fromOrToReg,
  int objReg, Field field, int xt) {
    // handle FieldStmt here
    Insn newFieldStmt = insnFactory.newInsn(
      "FieldStmt", opcode, fromOrToReg, objReg, field, xt 
    ); 
    newFieldStmt.setArgNames(
      "opcode", "fromOrToReg", "objReg", "field", "xt"
    );
    newFieldStmt.opcode = opcode;
    insnFactory.add(newFieldStmt);
  }

  @Override
  public void visitFillArrayStmt(int opcode, int aA,
  int elemWidth, int initLength, Object[] values) {
    // handle FillArrayStmt here
    Insn newFillArrayStmt = insnFactory.newInsn(
      "FillArrayStmt", opcode, aA, elemWidth, initLength, values 
    ); 
    newFillArrayStmt.setArgNames(
      "opcode", "aA", "elemWidth", "initLength", "values"
    );
    newFillArrayStmt.opcode = opcode;
    insnFactory.add(newFillArrayStmt);
  }

  @Override
  public void visitFilledNewArrayStmt(int opcode, int[] args,
  String type) {
    // handle FilledNewArrayStmt here
    Insn newFilledNewArrayStmt = insnFactory.newInsn(
      "FilledNewArrayStmt", opcode, args, type 
    ); 
    newFilledNewArrayStmt.setArgNames(
      "opcode", "args", "type"
    );
    newFilledNewArrayStmt.opcode = opcode;
    insnFactory.add(newFilledNewArrayStmt);
  }

  @Override
  public void visitJumpStmt(int opcode, int a, int b,
  DexLabel label) {
    // handle JumpStmt here
    Insn newJumpStmt = insnFactory.newInsn(
      "JumpStmt", opcode, a, b, label 
    ); 
    newJumpStmt.setArgNames(
      "opcode", "a", "b", "label"
    );
    newJumpStmt.opcode = opcode;
    insnFactory.add(newJumpStmt);
  }

  @Override
  public void visitJumpStmt(int opConst, int reg,
  DexLabel label) {
    // handle JumpStmt here
    Insn newJumpStmt = insnFactory.newInsn(
      "JumpStmt", opConst, reg, label 
    ); 
    newJumpStmt.setArgNames(
      "opConst", "reg", "label"
    );
    newJumpStmt.opcode = opConst;
    insnFactory.add(newJumpStmt);
  }

  @Override
  public void visitJumpStmt(int opGoto,
  DexLabel label) {
    // handle JumpStmt here
    Insn newJumpStmt = insnFactory.newInsn(
      "JumpStmt", opGoto, label 
    ); 
    newJumpStmt.setArgNames(
      "opGoto", "label"
    );
    newJumpStmt.opcode = opGoto;
    insnFactory.add(newJumpStmt);
  }

  @Override
  public void visitLookupSwitchStmt(int opcode, int aA,
  DexLabel label, int[] cases, DexLabel[] labels) {
    // handle LookupSwitchStmt here
    Insn newLookupSwitchStmt = insnFactory.newInsn(
      "LookupSwitchStmt", opcode, aA, label, cases, labels 
    ); 
    newLookupSwitchStmt.setArgNames(
      "opcode", "aA", "label", "cases", "labels"
    );
    newLookupSwitchStmt.opcode = opcode;
    insnFactory.add(newLookupSwitchStmt);
  }

  @Override
  public void visitMethodStmt(int opcode, int[] args,
  Method method) {
    // handle MethodStmt here
    Insn newMethodStmt = insnFactory.newInsn(
      "MethodStmt", opcode, args, method 
    ); 
    newMethodStmt.setArgOps(
      Op.OPCODE, Op.ARGS, Op.METHOD
    );
    newMethodStmt.opcode = opcode;
    insnFactory.add(newMethodStmt);
  }

  @Override
  public void visitMonitorStmt(int opcode,
  int reg) {
    // handle MonitorStmt here
    Insn newMonitorStmt = insnFactory.newInsn(
      "MonitorStmt", opcode, reg 
    ); 
    newMonitorStmt.setArgNames(
      "opcode", "reg"
    );
    newMonitorStmt.opcode = opcode;
    insnFactory.add(newMonitorStmt);
  }

  @Override
  public void visitMoveStmt(int opConst, int toReg,
  int xt) {
    // handle MoveStmt here
    Insn newMoveStmt = insnFactory.newInsn(
      "MoveStmt", opConst, toReg, xt 
    ); 
    newMoveStmt.setArgNames(
      "opConst", "toReg", "xt"
    );
    newMoveStmt.opcode = opConst;
    insnFactory.add(newMoveStmt);
  }

  @Override
  public void visitMoveStmt(int opcode, int toReg,
  int fromReg, int xt) {
    // handle MoveStmt here
    Insn newMoveStmt = insnFactory.newInsn(
      "MoveStmt", opcode, toReg, fromReg, xt 
    ); 
    newMoveStmt.setArgNames(
      "opcode", "toReg", "fromReg", "xt"
    );
    newMoveStmt.opcode = opcode;
    insnFactory.add(newMoveStmt);
  }

  @Override
  public void visitReturnStmt(int opcode) {
    // handle ReturnStmt here
    Insn newReturnStmt = insnFactory.newInsn(
      "ReturnStmt", opcode 
    ); 
    newReturnStmt.setArgNames(
      "opcode"
    );
    newReturnStmt.opcode = opcode;
    insnFactory.add(newReturnStmt);
  }

  @Override
  public void visitReturnStmt(int opConst, int reg,
  int xt) {
    // handle ReturnStmt here
    Insn newReturnStmt = insnFactory.newInsn(
      "ReturnStmt", opConst, reg, xt 
    ); 
    newReturnStmt.setArgNames(
      "opConst", "reg", "xt"
    );
    newReturnStmt.opcode = opConst;
    insnFactory.add(newReturnStmt);
  }

  @Override
  public void visitTableSwitchStmt(int opcode, int aA,
  DexLabel label, int first_case, int last_case,
  DexLabel[] labels) {
    // handle TableSwitchStmt here
    Insn newTableSwitchStmt = insnFactory.newInsn(
      "TableSwitchStmt", opcode, aA, label, first_case, last_case, labels 
    ); 
    newTableSwitchStmt.setArgNames(
      "opcode", "aA", "label", "first_case", "last_case", "labels"
    );
    newTableSwitchStmt.opcode = opcode;
    insnFactory.add(newTableSwitchStmt);
  }

  @Override
  public void visitUnopStmt(int opcode, int toReg,
  int fromReg, int xt) {
    // handle UnopStmt here
    Insn newUnopStmt = insnFactory.newInsn(
      "UnopStmt", opcode, toReg, fromReg, xt 
    ); 
    newUnopStmt.setArgNames(
      "opcode", "toReg", "fromReg", "xt"
    );
    newUnopStmt.opcode = opcode;
    insnFactory.add(newUnopStmt);
  }

  @Override
  public void visitTryCatch(DexLabel start, DexLabel end,
  DexLabel[] handlers, String[] types) {
    // handle TryCatch here
    Insn newTryCatch = insnFactory.newInsn(
      "TryCatch", start, end, handlers, types 
    ); 
    newTryCatch.setArgNames(
      "start", "end", "handlers", "types"
    );
    insnFactory.add(newTryCatch);
  }

  @Override
  public void visitArguments(int total, int[] args) {
    // handle Arguments here
    Insn newArguments = insnFactory.newInsn(
      "Arguments", total, args 
    ); 
    newArguments.setArgNames(
      "total", "args"
    );
    insnFactory.add(newArguments);
  }


  @Override
  public void visitEnd() {
    // handle End here
    Insn newEnd = insnFactory.newInsn(
      "End"
    ); 
    newEnd.setArgNames();
    insnFactory.add(newEnd);
  }

  @Override
  public void visitLabel(DexLabel label) {
    // handle Label here
    Insn newLabel = insnFactory.newInsn(
      "Label", label 
    ); 
    newLabel.setArgNames("label");
    insnFactory.add(newLabel);
  }


  @Override
  public void visitLineNumber(int line, DexLabel label) {
    insnFactory.atLine(line);
    // handle LineNumber here
    Insn newLineNumber = insnFactory.newInsn(
      "LineNumber", line, label 
    ); 
    newLineNumber.setArgNames(
      "line", "label"
    );
    newLineNumber.lineNumber = line;
    insnFactory.add(newLineNumber);
  }

  @Override
  public void visitLocalVariable(String name, String type,
  String signature, DexLabel start, DexLabel end,
  int reg) {
    // handle LocalVariable here
    Insn newLocalVariable = insnFactory.newInsn(
      "LocalVariable", name, type, signature, start, end, reg 
    ); 
    newLocalVariable.setArgNames(
      "name", "type", "signature", "start", "end", "reg"
    );
    insnFactory.add(newLocalVariable);
  }

  @Override
  public void visitUnopStmt(int opcode, int toReg,
  int fromReg, int xta, int xtb) {
    // handle UnopStmt here
    Insn newUnopStmt = insnFactory.newInsn(
      "UnopStmt", opcode, toReg, fromReg, xta, xtb 
    ); 
    newUnopStmt.setArgNames(
      "opcode", "toReg", "fromReg", "xta", "xtb"
    );
    newUnopStmt.opcode = opcode;
    insnFactory.add(newUnopStmt);
  }
  
}
