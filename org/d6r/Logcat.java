package org.d6r;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;
import org.d6r.PosixFileInputStream;
import com.google.common.collect.FluentIterable;
import com.google.common.base.Predicate;
import com.google.common.base.Function;


public class Logcat {
  
  protected static final Matcher LOGCAT_REMOVE_PTRN = Pattern.compile(
    "\\bGC_|_GC\\b|libjavacore.so|JNI_OnLoad|libnativehelper.so"
    + "|Trying to load lib|Added shared lib|JIT code",
    Pattern.DOTALL | Pattern.UNIX_LINES
  ).matcher("");
  
   protected static final Matcher LOGCAT_DVM_PTRN = Pattern.compile(
     "dalvikvm\\([0-9 ]+\\): ([^\n]*)" ,
    Pattern.DOTALL | Pattern.UNIX_LINES
  ).matcher("");
  
  
  public static enum LogcatSeverity {
    FATAL('F', "Fatal"),
    ERROR('E', "Error"),
    WARNING('W', "Warning"),
    VERBOSE('V', "Verbose"),
    DEBUG('D', "Debug"),
    INFO('I', "Info");
    
    private final char shortForm;
    private final String displayName;
    
    private LogcatSeverity(char shortForm, String displayName) {
      this.shortForm = shortForm;
      this.displayName = displayName;
    }
    
    public static LogcatSeverity parse(String s) {
      if (s.length() == 1) {
        char c = s.charAt(0);
        switch (c) {
          case 'F': return FATAL;
          case 'E': return ERROR;
          case 'W': return WARNING;
          case 'V': return VERBOSE;
          case 'D': return DEBUG;
          case 'I': return INFO;
          default: throw new UnsupportedOperationException(String.format(
            "Character not implemented: '%c'", c
          ));
        }
      } else {
        return Enum.valueOf(LogcatSeverity.class, s.toUpperCase());
      }
    }
    
    public String toString() {
      return this.displayName;
    }
  }
  

  public static final Matcher ENTRY_MCHR = Pattern.compile(
    "([A-Z])/([^:(\\s]+) *?\\( *([0-9]+)\\): ([^\n]*)", 
    Pattern.DOTALL | Pattern.UNIX_LINES
  ).matcher("");
  
  public static Object[] parseLogLine(final CharSequence line) {
    synchronized (ENTRY_MCHR) {
      if (ENTRY_MCHR.reset(line).find()) {
        String sev = ENTRY_MCHR.group(1);
        String tag = ENTRY_MCHR.group(2);
        int pid = Integer.parseInt(ENTRY_MCHR.group(3), 10);
        String message = ENTRY_MCHR.group(4);
        return new Object[]{ sev, tag, pid, message };
      }
    }
    return null;
  }
  
  
  public static class LogcatEntry {
    
    public final LogcatSeverity severity;
    public final String tag;
    public final int pid;
    public final String message;
    
    public LogcatEntry(Object[] parsedEntryParts) {
      final Object[] parts = parsedEntryParts;
      if (parts != null) {
        this.severity = LogcatSeverity.parse((String) parts[0]);
        this.tag = (String) parts[1];
        this.pid = ((Integer) parts[2]).intValue();
        this.message = (String) parts[3];
      } else {
        this.severity = null;
        this.tag = null;
        this.pid = 0;
        this.message = null;
      }
    }
    
    public LogcatEntry(String logLine) {
      this(parseLogLine(logLine));
    }
    
    public LogcatSeverity getSeverity() {
      return this.severity;
    }
    
    public String getTag() {
      return this.tag;
    }
    
    public int getPid() {
      return this.pid;
    }
    
    public String getMessage() {
      return this.message;
    }
    
    public String toString() {
      return String.format(
        "[%s] %s (%5d):\t%s",
        severity, tag, pid, message
      );
    }
    
    
    static List<String> getMessages(final List<LogcatEntry> entries) {      
      final List<String> messages = new ArrayList<>(entries.size()); 
    
      for (final LogcatEntry entry: entries) {
        if (entry.message == null) continue; 
        messages.add(entry.message);
      }
      return messages;
    }
    
  }
  
  public static List<LogcatEntry> parseEntries(
  final Iterable<? extends CharSequence> lines)
  {
    final List<LogcatEntry> entries = new LinkedList<>(); 
    for (final CharSequence line: lines) {
      final Object[] parts = parseLogLine(line);
      if (parts == null) continue;
      final LogcatEntry entry = new LogcatEntry(parts);
      entries.add(entry);
    }
    return entries;
  }
  
  public static List<LogcatEntry> parseEntries(final CharSequence logText) {
    return parseEntries(lines(logText));
  }
  
  public static List<String> getMessages(final List<LogcatEntry> entries) {
    return LogcatEntry.getMessages(entries);
  }
  
  public static Iterable<CharSequence> lines(final CharSequence s) {
    final List<CharSequence> lines = new LinkedList<CharSequence>(); 
    final int strlen = s.length();
    {
      int last = 0, index = -1;
      do {
        final int start = last;
        index = StringUtils.indexOf(s, '\n', last);
        final int end = (index != -1)? index: strlen;
        last = index+1;
        lines.add(s.subSequence(start, end));
      } while (index != -1);
    }
    return lines;
  }
  
  
  
  public static List<String> getAllLines() {
    return getLines(true);
  }
  
  public static List<LogcatEntry> getProcessEntries() {
    final List<String> lines = getLines(false);
    final int myPid = org.d6r.PosixFileInputStream.getPid();
    return FluentIterable
      .from(parseEntries(lines))
      .filter(
        new Predicate<LogcatEntry>() {
          @Override
          public boolean apply(final LogcatEntry entry) {
            return entry.getPid() == myPid;
          }
        }
      )
      .toList();
  }
  
  
  public static List<String> getProcessMessages() {
    return FluentIterable
      .from(getProcessEntries())
      .transform(
        new Function<LogcatEntry, String>() {
          @Override
          public String apply(final LogcatEntry entry) {
            return entry.getMessage();
          }
        }
      )
      .toList();
  }
  
  public static List<String> getLines(boolean returnAll) {
    String[] lineArr = StringUtils.split(
      PosixFileInputStream.pexecSync(
        "logcat", "-d", "*:s", "dalvikvm:*", "VFY:*", "dexopt:*",
        "DEXOPT:*"
      ),
      "\n"
    );
    if (lineArr.length == 0) return Collections.emptyList();
    
    /*boolean allStrings = true;
    for (final CharSequence first: lineArr) {
      if (first == null) continue;
      if (! (first instanceof String)) allStrings = false;
    }*/
    
    List<String> lines = new ArrayList<>(Arrays.asList(lineArr));
    Iterator<String> it = lines.iterator();
    Set<String> seen = new TreeSet<>();
    
    while (it.hasNext()) { 
      String line = it.next();
      if (seen.contains(line)) {
        it.remove();
        continue;
      }
      seen.add(line);
      if (LOGCAT_REMOVE_PTRN.reset(line).find()) {
        it.remove();
        continue;
      }
      if (! returnAll) {
        if (! LOGCAT_DVM_PTRN.reset(line).find()) {
          it.remove();
          continue;
        }
      }
    }
    
    return lines;
  }
  
  
}

