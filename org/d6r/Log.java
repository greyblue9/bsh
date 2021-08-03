package org.d6r;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java8.util.function.Supplier;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import java.nio.charset.StandardCharsets;

public class Log implements java.io.Serializable, java.io.Flushable {
  
  public static final String TAG = Log.class.getSimpleName();
  
  
  
  public static final int SEV_VERBOSE = 1;
  public static final int SEV_DEBUG = 2;
  public static final int SEV_INFO = 4;
  public static final int SEV_WARN = 8;
  public static final int SEV_ERROR = 16;
  public static final int SEV_SEVERE = 32;
  public static final int SEV_FATAL = 64;
  public static final int SEV_WTF = 128;
  public static final int SEV_ALL = 255;
  
  public static Log v(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_VERBOSE, message, args);
  }
  public static Log v(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_VERBOSE, message, exc);
  }
  public static Log v(String tag, Object obj) {
    return INSTANCE.log(tag, SEV_VERBOSE, String.format(
      "instance [%s@0x%08x]:  %s / %s",
      obj != null? obj.getClass().getName():"<NULL>", 
      System.identityHashCode(obj),
      obj != null? Reflector.invokeOrDefault(obj, "toString"): "<NULL>",
      obj != null? Debug.ToString(obj): "<NULL>"
    ));
  }
  
  @Override
  public void flush() {
    return;
  }
  
  public static Log d(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_DEBUG, message, args);
  }
  public static Log d(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_DEBUG, message, exc);
  }
  public static Log d(String tag, Object obj) {
    return logObject(tag, SEV_DEBUG, obj);
  }
  
  
  public static Log i(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_INFO, message, args);
  }
  public static Log i(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_INFO, message, exc);
  }
  public static Log i(String tag, Object obj) {
    return logObject(tag, SEV_INFO, obj);
  }
  
  
  public static Log w(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_WARN, message, args);
  }
  public static Log w(String tag, String message,Throwable exc) {
    return INSTANCE.log(tag, SEV_WARN, message, exc);
  }
  public static Log w(String tag, Object obj) {
    return logObject(tag, SEV_WARN, obj);
  }
  
  
  public static Log e(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_ERROR, message, args);
  }
  public static Log e(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_ERROR, message, exc);
  }
  public static Log e(String tag, Object obj) {
    return logObject(tag, SEV_ERROR, obj);
  }
  
  
  public static Log s(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_SEVERE, message, args);
  }
  public static Log s(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_SEVERE, message, exc);
  }
  public static Log s(String tag, Object obj) {
    return logObject(tag, SEV_SEVERE, obj);
  }
  
  
  public static Log f(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_FATAL, message, args);
  }
  public static Log f(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_FATAL, message, exc);
  }
  public static Log f(String tag, Object obj) {
    return logObject(tag, SEV_FATAL, obj);
  }
  
  
  public static Log wtf(String tag, String message, Object... args) {
    return INSTANCE.log(tag, SEV_WTF, message, args);
  }
  public static Log wtf(String tag, String message, Throwable exc) {
    return INSTANCE.log(tag, SEV_WTF, message, exc);
  }
  public static Log wtf(String tag, Object obj) {
    return logObject(tag, SEV_WTF, obj);
  }
  
  
  
  static final Object[] NO_ARGS = { };
  
  static Log logObject(final String tag, final int severity, final Object obj) {
    
    if (obj instanceof Object[]) {
      final Object[] arr = (Object[]) obj;
      final int arrLength = arr.length;
      if (arrLength != 0 && arr[0] instanceof CharSequence) {
        return INSTANCE.log(
          tag,
          severity,
          CharSequenceUtil.toString((CharSequence) arr[0]),
          (Object[]) (
            (arrLength > 1) ? Arrays.copyOfRange(arr, 1, arrLength) : NO_ARGS)
        );
      }
    }
    
    final String message;
    final String correctedTag;
    final Object[] args;
    final int realSeverity;
    if (tag != null && tag.indexOf('%') != -1 || tag.indexOf('=') != -1 ||
        tag.indexOf('@') != -1)
    {
      message = tag;
      correctedTag = null;
      args = (obj instanceof Object[]) ? (Object[]) obj: new Object[]{ obj };
      realSeverity = severity;
    } else if (obj instanceof CharSequence) {
      message = CharSequenceUtil.toString((CharSequence) obj);
      correctedTag = tag;
      args = NO_ARGS;
      realSeverity = severity;
    } else if (obj instanceof Throwable) {
      Throwable cause;
      String msg1, msg2;
      message = (msg1 = ((Throwable) obj).getMessage()) != null && msg1.length() > 0
        ? msg1
        : (cause = Reflector.getRootCause((Throwable) obj)) != null &&
            (msg2 = cause.getMessage()) != null && msg2.length() > 0
              ? msg2
              : obj.toString();
      correctedTag = obj.getClass().getSimpleName();
      args = new Object[]{ (Throwable) obj };
      realSeverity = Math.max(SEV_WARN, severity);
    } else {
      final Object[] array = (obj instanceof Object[])
        ? (Object[]) obj
        : new Object[]{ obj };
      final StringBuilder sb = new StringBuilder(80);
      Throwable exc = null;
      for (int i=0, len=array.length; i<len; ++i) {
        final Object o = array[i];
        if (o instanceof Throwable) {
          if (exc == null) {
            exc = new LoggedException(tag, i, len, (Throwable) o);
          } else {
            exc.addSuppressed(new LoggedException(tag, i, len, (Throwable) o));
          }
        }
        sb.append(String.format(
          "\n  - argument %d: (%s) @%08x: %s <%s>",
          i, ClassInfo.getSimpleName(o), System.identityHashCode(o),
          Dumper.tryToString(o), Debug.ToString(o)
        ));
      }
      if (exc != null) exc.printStackTrace();
      message = sb.insert(0, String.format("%d arguments logged:", array.length))
        .toString();
      correctedTag = tag;
      realSeverity = severity;
      args = NO_ARGS;
    }
    
    return INSTANCE.log(correctedTag, severity, message, args);
  }
  
  public static class LoggedException extends RuntimeException {
    public LoggedException(final String messageOrTag, final int argIndex,
      final int argsLength, final Throwable t)
    {
      super(String.format(
        "%s in Log directive in argument at index %d (# items = %d), " +
        "with message or tag \"%s\": %s",
        ClassInfo.getSimpleName(t),
        argIndex, argsLength,
        messageOrTag,
        Dumper.tryToString(t)
      ), t);
    }
  }
  
  public static String getSeverityName(final int severity) {
    switch (severity) {
      case SEV_VERBOSE: return "VERBOSE";
      case SEV_DEBUG: return "DEBUG";
      case SEV_INFO: return "INFO";
      case SEV_WARN: return "WARN";
      case SEV_ERROR: return "ERROR";
      case SEV_SEVERE: return "SEVERE";
      case SEV_FATAL: return "FATAL";
      case SEV_WTF: return "WTF";
      default: return String.format("UNKNOWN[severity: %d]", severity);
    }
  }
  
  public static int getSeverityColor(final int severity) {
    switch (severity) {
      case SEV_VERBOSE: return 030;
      case SEV_DEBUG: return 2;
      case SEV_INFO: return 6;
      case SEV_WARN: return 3;
      case SEV_ERROR: return 1;
      case SEV_SEVERE: return 5;
      case SEV_FATAL: return 017;
      case SEV_WTF: return 067;
      default: return 4;
    }
  }
  
  public static final
    Supplier<PrintWriter> STDERR_SUPPLIER = new Supplier<PrintWriter>() {
      PrintWriter nullWriter; 
      PrintWriter stderrWriter; 
      @Override
      public PrintWriter get() {
        if (System.err == null) {
          if (nullWriter == null) {
            nullWriter = new PrintWriter(new NullOutputStream());
          }
          return nullWriter;
        }
        if (stderrWriter == null) stderrWriter = new PrintWriter(
          new OutputStreamWriter(
            new BufferedOutputStream(System.err, 1024), StandardCharsets.UTF_8
          )
        );
        return stderrWriter;
      }
    };
  
  public static Log INSTANCE = new Log(
    Integer.parseInt(
      System.getProperty("log.level", Integer.toString(SEV_ALL ^ SEV_VERBOSE))
    ),
    STDERR_SUPPLIER
  );
  
  
    
  public boolean silent;
  public int enabledLevels;
  public final Supplier<PrintWriter> pwSource;
  
  public Log(int enabledSeverityLevels, Supplier<PrintWriter> writerSource) {
    this.enabledLevels = enabledSeverityLevels;
    this.pwSource = writerSource != null? writerSource: STDERR_SUPPLIER;
  }
  
  public void mute(boolean newSilent) {
    this.silent = newSilent;
  }
  public boolean isMute() {
    return this.silent;
  }
  
  public PrintWriter pw() {
    return pwSource.get();
  }
  
  
  public int getEnabledLevels() {
    return enabledLevels;
  }
  public boolean isEnabled(int severity) {
    return (enabledLevels & severity) != 0;
  }
  public static boolean isLoggable(final int severity) {
    return (INSTANCE.enabledLevels & severity) != 0;
  }
  public boolean isDebugEnabled() { return !silent && isEnabled(SEV_DEBUG); }
  public boolean isVerboseEnabled() { return !silent && isEnabled(SEV_VERBOSE); }
  public boolean isEnabled() { return !silent && isEnabled(SEV_ALL); }
  
  
  boolean flushing = true;
  public Log stopFlush() {
    this.flushing = false;
    return this;
  }
  public Log startFlush() {
    this.flushing = true;
    return this;
  }
  
  static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(
    "MM-dd-yy HH:mm:ss.SSS", TimeZone.getDefault(), Locale.getDefault()
  );
  
  
  String lastTag = null;
  int lastSeverity;
  
  
  public Log log(final String tag, final int severity, final String message,
  Object... args)
  {
    if (args.length == 1 && args[0] instanceof Throwable){
      return log(tag, severity, message, (Throwable) args[0]);
    }
    
    if (!isEnabled(severity)) return this;
    final String severityName = getSeverityName(severity);
    final PrintWriter pw = pw();
    String formattedMessage = message;
    try {
      if (args.length != 0) {
        try {
          if (formattedMessage.indexOf("%d") != -1) {
            formattedMessage = formattedMessage.replace("%d", "%s");
          }
          formattedMessage = String.format(message, args);
        } catch (IllegalArgumentException iaeFmtEx) {
          int lastpos = -1;
          int pos;
          int len = formattedMessage.length();
          while ((pos = formattedMessage.indexOf('%', lastpos+1))!= -1) {
            lastpos = pos;
            if (len > pos+1) {
              loop:
              do {
                switch (formattedMessage.charAt(pos+1)) {
                  case '%':
                    lastpos++;
                    continue;
                  case '0':
                  case '1':
                  case '2':
                  case '3': 
                  case '4': 
                  case '5': 
                  case '6':
                  case '7':
                  case '8':
                  case '9':
                    ++pos;
                    continue;
                  case '$':
                    ++pos;
                    continue;
                  case 'd':
                    formattedMessage = String.format(
                      "%s%%s%s",
                      formattedMessage.subSequence(0, pos+1),
                      formattedMessage.subSequence(pos+2, len)
                    );
                    lastpos = pos+1;
                    break loop;
                  default:
                    lastpos = pos+1;
                    break loop;
                }
              } while (pos+2 < len);
            }
          }
          try {
            formattedMessage = String.format(
              formattedMessage, args
            );
          } catch (Throwable e) {
            formattedMessage = message + String.format(
             " [ %s | format args: %s ]", iaeFmtEx, Debug.ToString(args)
            );
          }
        }
      }
      
      final int colors = getSeverityColor(severity);
      final int backColor = ((070 & colors) >>> 4);
      final int foreColor = ((007 & colors) >>> 0);
      
      pw.printf(
        //"\u001b[0;36m%1$s @ %2$s\u001b[0m \u001b[s;3%5$cm[%3$s]\u001b[0m: %4$s\n",
        (tag == null || tag.equals(lastTag)) && (severity == lastSeverity)
          ? "  \u001b[0m: %4$s\n"
          : "\u001b[0;36m%1$s \u001b[%5$sm[%3$s]\u001b[0m: %4$s\n",
        tag,
        DATE_FORMAT.format(System.currentTimeMillis()),
        String.valueOf(Character.toChars(severityName.charAt(0))),
        formattedMessage,
        String.format(
          "%1d;%02d;%03d",
          (foreColor != 0)?  1: 0,
          (backColor != 0)? (40 | backColor): 0,
          (foreColor != 0)? (30 | backColor): 0
        )
      );
      return this;
    } finally {
      if (flushing) pw.flush();
      lastSeverity = severity;
      lastTag = tag;
    }
  }
  
  public Log log(String tag, int severity, String message, Throwable exc) {
    if (!isEnabled(severity)) return this;
    final String[] stackTraceStr = ExceptionUtils.getRootCauseStackTrace(exc);
    this.log(tag, severity, "BEGINNING OF LOG RECORD ==>", true)
        .log(tag, severity, message, true);
    for (int i=0,len=stackTraceStr.length; i<len; ++i) {
      this.log(tag, severity, stackTraceStr[i], true);
    }
    try {
      return this.log(tag, severity, "(See message and stack trace above)", true)
                 .log(tag, severity, "<== END OF LOG RECORD", true);
    } finally {
      if (flushing) this.pw().flush();
    }
  }
  
}