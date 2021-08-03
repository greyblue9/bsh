
package cf;

import java.io.*;
import java.util.*;

import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.Dumpable;

import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.state.*;


public class CfDumper implements Dumper {
  
  public static PrintStream out = System.out;
  public int count = 0;
  public int indent = 0; 
  
  @Override
  public void addSummaryError(Method mthd, String str) {
    out.printf(
      "addSummaryError(%s %s = %s, %s %s = %s)\n",
      "Method", "mthd", mthd,
      "String", "str", str
    );
  }  
  
  @Override
  public boolean canEmitClass(JavaTypeInstance jtypeinst) {
    out.printf(
      "canEmitClass(%s %s = %s)\n",
      "JavaTypeInstance", "jtypeinst", 
      jtypeinst
    );
    return true;
  }  
  
  @Override
  public void close() {
    out.printf(
      "close()\n"
    );
  }  
  
  @Override
  public Dumper dump(JavaTypeInstance jtypeinst) {
    out.printf(
      "dump(%s %s = %s)\n",
      "JavaTypeInstance", "jtypeinst", 
      jtypeinst
    );
    return this;
  }  
  
  @Override
  public Dumper dump(Dumpable dumpableItem) {
    count += 1;
    out.printf(
      "dump(%s %s = %s)\n",
      "Dumpable", "dumpableItem", 
      dumpableItem
    );
    return this;
  }  
  
  @Override
  public void dump(List<? extends Dumpable> dumpableItems) {
    count += dumpableItems.size();
    out.printf(
      " dump(List<Dumpable> dumpableItems = %s) \n",
      dumpableItems
    );
  }  
  
  @Override
  public Dumper endCodeln() {
    out.printf(
      "endCodeln()\n"
    );
    return this;
  }  
  
  @Override
  public void enqueuePendingCarriageReturn() {
    out.printf(
      "enqueuePendingCarriageReturn()\n"
    );
  }  
  
  @Override
  public int getIndent() {
    out.printf(
      "getIndent()\n"
    );
    return indent;
  }  
  
  @Override
  public int getOutputCount() {
    out.printf(
      "getOutputCount()\n"
    );
    return count;
  }  
  
  @Override
  public TypeUsageInformation getTypeUsageInformation() {
    out.printf(
      "getTypeUsageInformation()\n"
    );
    return null;
  }  
  
  @Override
  public void indent(int level) {
    indent += level;
    out.printf(
      "indent(%s %s = %s)\n",
      "int", "level", 
      level
    );
  }  
  
  @Override
  public Dumper newln() {
    out.printf(
      "newln()\n"
    );
    return this;
  }  
  
  @Override
  public Dumper identifier(String name) {
    out.printf(
      "identifier(%s %s = %s)\n",
      "String", "name",
      name
    );
    return this;
  }
  
  @Override
  public Dumper print(char chr) {
    out.printf(
      "print(%s %s = %s)\n",
      "char", "chr", 
      chr
    );
    return this;
  }  
  
  @Override
  public Dumper print(String str) {
    out.printf(
      "print(%s %s = %s)\n",
      "String", "str", 
      str
    );
    return this;
  }  
  
  @Override
  public void printLabel(String label) {
    out.printf(
      "printLabel(%s %s = %s)\n",
      "String", "label", 
      label
    );
  }  
  
  @Override
  public Dumper removePendingCarriageReturn() {
    out.printf(
      "removePendingCarriageReturn()\n"
    );
    return this;
  }  
  
}
  