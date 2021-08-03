package org.slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;
import org.slf4j.helpers.Util;
// import org.slf4j.impl.StaticLoggerBinder;
// import org.slf4j.impl.SimpleLogger;


import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import java.util.Date;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import org.slf4j.helpers.Util;
import java.text.SimpleDateFormat;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Properties;
import org.slf4j.helpers.MarkerIgnoringBase;


public class LoggerFactory {

 public static class SimpleLogger extends MarkerIgnoringBase {

  private static final long serialVersionUID = -632788891211436180L;

  private static final String CONFIGURATION_FILE = "simplelogger.properties";

  private static long START_TIME;

  private static final Properties SIMPLE_LOGGER_PROPS;

  private static final int LOG_LEVEL_TRACE = 0;

  private static final int LOG_LEVEL_DEBUG = 10;

  private static final int LOG_LEVEL_INFO = 20;

  private static final int LOG_LEVEL_WARN = 30;

  private static final int LOG_LEVEL_ERROR = 40;

  private static boolean INITIALIZED;

  private static int DEFAULT_LOG_LEVEL;

  private static boolean SHOW_DATE_TIME;

  private static String DATE_TIME_FORMAT_STR;

  private static DateFormat DATE_FORMATTER;

  private static boolean SHOW_THREAD_NAME;

  private static boolean SHOW_LOG_NAME;

  private static boolean SHOW_SHORT_LOG_NAME;

  private static String LOG_FILE;

  private static PrintStream TARGET_STREAM;

  private static boolean LEVEL_IN_BRACKETS;

  private static String WARN_LEVEL_STRING;

  public static final String SYSTEM_PREFIX = "org.slf4j.simpleLogger.";

  public static final String DEFAULT_LOG_LEVEL_KEY = "org.slf4j.simpleLogger.defaultLogLevel";

  public static final String SHOW_DATE_TIME_KEY = "org.slf4j.simpleLogger.showDateTime";

  public static final String DATE_TIME_FORMAT_KEY = "org.slf4j.simpleLogger.dateTimeFormat";

  public static final String SHOW_THREAD_NAME_KEY = "org.slf4j.simpleLogger.showThreadName";

  public static final String SHOW_LOG_NAME_KEY = "org.slf4j.simpleLogger.showLogName";

  public static final String SHOW_SHORT_LOG_NAME_KEY = "org.slf4j.simpleLogger.showShortLogName";

  public static final String LOG_FILE_KEY = "org.slf4j.simpleLogger.logFile";

  public static final String LEVEL_IN_BRACKETS_KEY = "org.slf4j.simpleLogger.levelInBrackets";

  public static final String WARN_LEVEL_STRING_KEY = "org.slf4j.simpleLogger.warnLevelString";

  public static final String LOG_KEY_PREFIX = "org.slf4j.simpleLogger.log.";

  protected int currentLogLevel;

  private transient String shortLogName;

  private static String getStringProperty(final String name) {
    String prop = null;
    try {
      prop = System.getProperty(name);
    } catch (SecurityException ex) {
    }
    return (prop == null) ? SimpleLogger.SIMPLE_LOGGER_PROPS.getProperty(name) : prop;
  }

  private static String getStringProperty(final String name, final String defaultValue) {
    final String prop = getStringProperty(name);
    return (prop == null) ? defaultValue : prop;
  }

  private static boolean getBooleanProperty(final String name, final boolean defaultValue) {
    final String prop = getStringProperty(name);
    return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
  }

  static void init() {
    SimpleLogger.INITIALIZED = true;
    loadProperties();
    final String defaultLogLevelString = getStringProperty("org.slf4j.simpleLogger.defaultLogLevel", null);
    if (defaultLogLevelString != null) {
      SimpleLogger.DEFAULT_LOG_LEVEL = stringToLevel(defaultLogLevelString);
    }
    SimpleLogger.SHOW_LOG_NAME = getBooleanProperty("org.slf4j.simpleLogger.showLogName", SimpleLogger.SHOW_LOG_NAME);
    SimpleLogger.SHOW_SHORT_LOG_NAME = getBooleanProperty("org.slf4j.simpleLogger.showShortLogName", SimpleLogger.SHOW_SHORT_LOG_NAME);
    SimpleLogger.SHOW_DATE_TIME = getBooleanProperty("org.slf4j.simpleLogger.showDateTime", SimpleLogger.SHOW_DATE_TIME);
    SimpleLogger.SHOW_THREAD_NAME = getBooleanProperty("org.slf4j.simpleLogger.showThreadName", SimpleLogger.SHOW_THREAD_NAME);
    SimpleLogger.DATE_TIME_FORMAT_STR = getStringProperty("org.slf4j.simpleLogger.dateTimeFormat", SimpleLogger.DATE_TIME_FORMAT_STR);
    SimpleLogger.LEVEL_IN_BRACKETS = getBooleanProperty("org.slf4j.simpleLogger.levelInBrackets", SimpleLogger.LEVEL_IN_BRACKETS);
    SimpleLogger.WARN_LEVEL_STRING = getStringProperty("org.slf4j.simpleLogger.warnLevelString", SimpleLogger.WARN_LEVEL_STRING);
    SimpleLogger.LOG_FILE = getStringProperty("org.slf4j.simpleLogger.logFile", SimpleLogger.LOG_FILE);
    SimpleLogger.TARGET_STREAM = computeTargetStream(SimpleLogger.LOG_FILE);
    if (SimpleLogger.DATE_TIME_FORMAT_STR != null) {
      try {
        SimpleLogger.DATE_FORMATTER = new SimpleDateFormat(SimpleLogger.DATE_TIME_FORMAT_STR);
      } catch (IllegalArgumentException e) {
        Util.report("Bad date format in simplelogger.properties; will output relative time", e);
      }
    }
  }

  private static PrintStream computeTargetStream(final String logFile) {
    if ("System.err".equalsIgnoreCase(logFile)) {
      return System.err;
    }
    if ("System.out".equalsIgnoreCase(logFile)) {
      return System.out;
    }
    try {
      final FileOutputStream fos = new FileOutputStream(logFile);
      final PrintStream printStream = new PrintStream(fos);
      return printStream;
    } catch (FileNotFoundException e) {
      Util.report("Could not open [" + logFile + "]. Defaulting to System.err", e);
      return System.err;
    }
  }

  private static void loadProperties() {
    final InputStream in = AccessController.doPrivileged((PrivilegedAction<InputStream>) new PrivilegedAction() {

      public Object run() {
        final ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
        if (threadCL != null) {
          return threadCL.getResourceAsStream("simplelogger.properties");
        }
        return ClassLoader.getSystemResourceAsStream("simplelogger.properties");
      }
    });
    if (null != in) {
      try {
        SimpleLogger.SIMPLE_LOGGER_PROPS.load(in);
        in.close();
      } catch (IOException ex) {
      }
    }
  }

  public SimpleLogger(final String name) {
    this.currentLogLevel = 20;
    this.shortLogName = null;
    if (!SimpleLogger.INITIALIZED) {
      init();
    }
    this.name = name;
    final String levelString = this.recursivelyComputeLevelString();
    if (levelString != null) {
      this.currentLogLevel = stringToLevel(levelString);
    } else {
      this.currentLogLevel = SimpleLogger.DEFAULT_LOG_LEVEL;
    }
  }

  String recursivelyComputeLevelString() {
    String tempName = this.name;
    String levelString = null;
    for (int indexOfLastDot = tempName.length(); levelString == null && indexOfLastDot > -1; levelString = getStringProperty("org.slf4j.simpleLogger.log." + tempName, null), indexOfLastDot = String.valueOf(tempName).lastIndexOf(".")) {
      tempName = tempName.substring(0, indexOfLastDot);
    }
    return levelString;
  }

  private static int stringToLevel(final String levelStr) {
    if ("trace".equalsIgnoreCase(levelStr)) {
      return 0;
    }
    if ("debug".equalsIgnoreCase(levelStr)) {
      return 10;
    }
    if ("info".equalsIgnoreCase(levelStr)) {
      return 20;
    }
    if ("warn".equalsIgnoreCase(levelStr)) {
      return 30;
    }
    if ("error".equalsIgnoreCase(levelStr)) {
      return 40;
    }
    return 20;
  }

  private void log(final int level, final String message, final Throwable t) {
    if (!this.isLevelEnabled(level)) {
      return;
    }
    final StringBuffer buf = new StringBuffer(32);
    if (SimpleLogger.SHOW_DATE_TIME) {
      if (SimpleLogger.DATE_FORMATTER != null) {
        buf.append(this.getFormattedDate());
        buf.append(' ');
      } else {
        buf.append(System.currentTimeMillis() - SimpleLogger.START_TIME);
        buf.append(' ');
      }
    }
    if (SimpleLogger.SHOW_THREAD_NAME) {
      buf.append('[');
      buf.append(Thread.currentThread().getName());
      buf.append("] ");
    }
    if (SimpleLogger.LEVEL_IN_BRACKETS) {
      buf.append('[');
    }
    switch((level)) {
      case 0:
        {
          buf.append("TRACE");
          break;
        }
      case 10:
        {
          buf.append("DEBUG");
          break;
        }
      case 20:
        {
          buf.append("INFO");
          break;
        }
      case 30:
        {
          buf.append(SimpleLogger.WARN_LEVEL_STRING);
          break;
        }
      case 40:
        {
          buf.append("ERROR");
          break;
        }
    }
    if (SimpleLogger.LEVEL_IN_BRACKETS) {
      buf.append(']');
    }
    buf.append(' ');
    if (SimpleLogger.SHOW_SHORT_LOG_NAME) {
      if (this.shortLogName == null) {
        this.shortLogName = this.computeShortName();
      }
      buf.append(String.valueOf(this.shortLogName)).append(" - ");
    } else if (SimpleLogger.SHOW_LOG_NAME) {
      buf.append(String.valueOf(this.name)).append(" - ");
    }
    buf.append(message);
    this.write(buf, t);
  }

  void write(final StringBuffer buf, final Throwable t) {
    SimpleLogger.TARGET_STREAM.println(buf.toString());
    if (t != null) {
      t.printStackTrace(SimpleLogger.TARGET_STREAM);
    }
    SimpleLogger.TARGET_STREAM.flush();
  }

  private String getFormattedDate() {
    final Date now = new Date();
    final String dateText;
    synchronized ((SimpleLogger.DATE_FORMATTER)) {
      dateText = SimpleLogger.DATE_FORMATTER.format(now);
    }
    return dateText;
  }

  private String computeShortName() {
    return this.name.substring(this.name.lastIndexOf(".") + 1);
  }

  private void formatAndLog(final int level, final String format, final Object arg1, final Object arg2) {
    if (!this.isLevelEnabled(level)) {
      return;
    }
    final FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
    this.log(level, tp.getMessage(), tp.getThrowable());
  }

  private void formatAndLog(final int level, final String format, final Object... arguments) {
    if (!this.isLevelEnabled(level)) {
      return;
    }
    final FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
    this.log(level, tp.getMessage(), tp.getThrowable());
  }

  protected boolean isLevelEnabled(final int logLevel) {
    return logLevel >= this.currentLogLevel;
  }

  public boolean isTraceEnabled() {
    return this.isLevelEnabled(0);
  }

  public void trace(final String msg) {
    this.log(0, msg, null);
  }

  public void trace(final String format, final Object param1) {
    this.formatAndLog(0, format, param1, null);
  }

  public void trace(final String format, final Object param1, final Object param2) {
    this.formatAndLog(0, format, param1, param2);
  }

  public void trace(final String format, final Object... argArray) {
    this.formatAndLog(0, format, argArray);
  }

  public void trace(final String msg, final Throwable t) {
    this.log(0, msg, t);
  }

  public boolean isDebugEnabled() {
    return this.isLevelEnabled(10);
  }

  public void debug(final String msg) {
    this.log(10, msg, null);
  }

  public void debug(final String format, final Object param1) {
    this.formatAndLog(10, format, param1, null);
  }

  public void debug(final String format, final Object param1, final Object param2) {
    this.formatAndLog(10, format, param1, param2);
  }

  public void debug(final String format, final Object... argArray) {
    this.formatAndLog(10, format, argArray);
  }

  public void debug(final String msg, final Throwable t) {
    this.log(10, msg, t);
  }

  public boolean isInfoEnabled() {
    return this.isLevelEnabled(20);
  }

  public void info(final String msg) {
    this.log(20, msg, null);
  }

  public void info(final String format, final Object arg) {
    this.formatAndLog(20, format, arg, null);
  }

  public void info(final String format, final Object arg1, final Object arg2) {
    this.formatAndLog(20, format, arg1, arg2);
  }

  public void info(final String format, final Object... argArray) {
    this.formatAndLog(20, format, argArray);
  }

  public void info(final String msg, final Throwable t) {
    this.log(20, msg, t);
  }

  public boolean isWarnEnabled() {
    return this.isLevelEnabled(30);
  }

  public void warn(final String msg) {
    this.log(30, msg, null);
  }

  public void warn(final String format, final Object arg) {
    this.formatAndLog(30, format, arg, null);
  }

  public void warn(final String format, final Object arg1, final Object arg2) {
    this.formatAndLog(30, format, arg1, arg2);
  }

  public void warn(final String format, final Object... argArray) {
    this.formatAndLog(30, format, argArray);
  }

  public void warn(final String msg, final Throwable t) {
    this.log(30, msg, t);
  }

  public boolean isErrorEnabled() {
    return this.isLevelEnabled(40);
  }

  public void error(final String msg) {
    this.log(40, msg, null);
  }

  public void error(final String format, final Object arg) {
    this.formatAndLog(40, format, arg, null);
  }

  public void error(final String format, final Object arg1, final Object arg2) {
    this.formatAndLog(40, format, arg1, arg2);
  }

  public void error(final String format, final Object... argArray) {
    this.formatAndLog(40, format, argArray);
  }

  public void error(final String msg, final Throwable t) {
    this.log(40, msg, t);
  }

  static {
    SimpleLogger.START_TIME = System.currentTimeMillis();
    SIMPLE_LOGGER_PROPS = new Properties();
    SimpleLogger.INITIALIZED = false;
    SimpleLogger.DEFAULT_LOG_LEVEL = 20;
    SimpleLogger.SHOW_DATE_TIME = false;
    SimpleLogger.DATE_TIME_FORMAT_STR = null;
    SimpleLogger.DATE_FORMATTER = null;
    SimpleLogger.SHOW_THREAD_NAME = true;
    SimpleLogger.SHOW_LOG_NAME = true;
    SimpleLogger.SHOW_SHORT_LOG_NAME = false;
    SimpleLogger.LOG_FILE = "System.err";
    SimpleLogger.TARGET_STREAM = null;
    SimpleLogger.LEVEL_IN_BRACKETS = false;
    SimpleLogger.WARN_LEVEL_STRING = "WARN";
  }
}




  
  
  static final String CODES_PREFIX = "http://www.slf4j.org/codes.html";
  static final String NO_STATICLOGGERBINDER_URL = "http://www.slf4j.org/codes.html#StaticLoggerBinder";
  static final String MULTIPLE_BINDINGS_URL = "http://www.slf4j.org/codes.html#multiple_bindings";
  static final String NULL_LF_URL = "http://www.slf4j.org/codes.html#null_LF";
  static final String VERSION_MISMATCH = "http://www.slf4j.org/codes.html#version_mismatch";
  static final String SUBSTITUTE_LOGGER_URL = "http://www.slf4j.org/codes.html#substituteLogger";
  static final String UNSUCCESSFUL_INIT_URL = "http://www.slf4j.org/codes.html#unsuccessfulInit";
  static final String UNSUCCESSFUL_INIT_MSG = "org.slf4j.LoggerFactory could not be successfully initialized. See also http://www.slf4j.org/codes.html#unsuccessfulInit";
  static final int UNINITIALIZED = 0;
  static final int ONGOING_INITIALIZATION = 1;
  static final int FAILED_INITIALIZATION = 2;
  static final int SUCCESSFUL_INITIALIZATION = 3;
  static final int NOP_FALLBACK_INITIALIZATION = 4;
  static int INITIALIZATION_STATE = 0;

  private static final String[] API_COMPATIBILITY_LIST
    = new String[]{ "1.5", "1.6", "1.7", "1.8" };
  private static String STATIC_LOGGER_BINDER_PATH
    = null; //"org/slf4j/impl/StaticLoggerBinder.class";
  
  public static final ILoggerFactory FACTORY =  new ILoggerFactory() {
    @Override
    public Logger getLogger(String name) {
      return LoggerFactory.getLogger(name);
    }
  };
  
  
  
  private LoggerFactory() {
  }

  static void reset() {
    INITIALIZATION_STATE = SUCCESSFUL_INITIALIZATION;
  }
  
  static {
    reset();
  }

  private static final void performInitialization() {
  }

  private static boolean messageContainsOrgSlf4jImplStaticLoggerBinder(String msg) {
    return false;
  }

  private static final void bind() {
  }

  static void failedBinding(Throwable t) {
  }

  private static final void emitSubstituteLoggerWarning() {
    return; 
    /*
    List loggerNameList = TEMP_FACTORY.getLoggerNameList();
    if (loggerNameList.size() == 0) {
      return;
    }
    Util.report("The following loggers will not work because they were created");
    Util.report("during the default configuration phase of the underlying logging system.");
    Util.report("See also http://www.slf4j.org/codes.html#substituteLogger");
    for (int i = 0; i < loggerNameList.size(); ++i) {
      String loggerName = (String)loggerNameList.get(i);
      Util.report(loggerName);
    }
    */
  }

  private static final void versionSanityCheck() {
  }

  private static Set findPossibleStaticLoggerBinderPathSet() {
    return Collections.emptySet();
  }

  private static boolean isAmbiguousStaticLoggerBinderPathSet(
  Set staticLoggerBinderPathSet)
  {
    return false;
  }

  private static void reportMultipleBindingAmbiguity(Set staticLoggerBinderPathSet) {
  }

  private static void reportActualBinding(Set staticLoggerBinderPathSet) {
  }

  public static Logger getLogger(String name) {
    return new SimpleLogger(name);
  }

  public static Logger getLogger(Class clazz) {
    return LoggerFactory.getLogger(clazz.getName());
  }


  public static ILoggerFactory getILoggerFactory() {
    return FACTORY;
  }
}


