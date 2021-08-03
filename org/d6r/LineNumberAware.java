

package org.d6r;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Map;


public interface LineNumberAware {
  
  int getLineNumber(CharSequence line);
  
  CharSequence getLine(int lineNumber);
  
  String[] getContextLines(int lineNumber, int context);
  
  String[] getLines(int lineNumberFrom, int lineNumberTo);
  
  Map<Integer, String> getLineMap();
  
}