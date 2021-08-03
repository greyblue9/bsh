
package org.d6r;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
  public abstract void debug(Object);
  public abstract void debug(Object, Throwable);
  public abstract void error(Object);
  public abstract void error(Object, Throwable);
  public abstract void fatal(Object);
  public abstract void fatal(Object, Throwable);
  public abstract void info(Object);
  public abstract void info(Object, Throwable);
  public abstract boolean isDebugEnabled();
  public abstract boolean isErrorEnabled();
  public abstract boolean isFatalEnabled();
  public abstract boolean isInfoEnabled();
  public abstract boolean isTraceEnabled();
  public abstract boolean isWarnEnabled();
  public abstract void trace(Object);
  public abstract void trace(Object, Throwable);
  public abstract void warn(Object);
  public abstract void warn(Object, Throwable);
*/

public class JexlLogger 
  implements Log 
{
  public static PrintStream buffer;
  public static ByteArrayOutputStream baos;
  
  static boolean trace = true;
  static boolean debug = true;
  static boolean info = true;
  static boolean warn = true;
  static boolean error = true;
  static boolean fatal = true;
  
  static final String TRACE = "TRACE".intern();
  static final String DEBUG = "DEBUG".intern();
  static final String INFO = "INFO".intern();
  static final String WARN = "WARN".intern();
  static final String ERROR = "ERROR".intern();
  static final String FATAL = "FATAL".intern();
  
  static {
    baos = new ByteArrayOutputStream();
    buffer = new PrintStream(baos);
  }
  
  void log(String level, Throwable throwable) {
    if (buffer != null) {
      buffer.flush();
      //try { 
      buffer.close();
      //} catch (IOException e) {}
      buffer = null;
    }
    if (baos != null) {
      try { baos.flush(); } catch (IOException e) {}
      try { baos.close(); } catch (IOException e) {}
      baos = null;
    }
    baos = new ByteArrayOutputStream();
    buffer = new PrintStream(baos);
    if ("true".equals(System.getProperty("printStackTrace"))) throwable.printStackTrace(buffer);
    try { baos.flush(); } catch (IOException e) {}
    try { baos.close(); } catch (IOException e) {}
    String trace = baos.toString();
    System.err.printf("[%s] %s\n", level, throwable);
  }
  
  void log(String level, Object object) {
    if (object instanceof Throwable) {
      log(level, (Throwable) object);
      return;
    }
    System.err.printf("[%s] %s\n", level, object);
  }
  
  @Override
  public void debug(Object object) {
    if (!isDebugEnabled()) return;
    log(DEBUG, object);
  }
  
  @Override
  public void debug(Object object, Throwable throwable) {
    if (!isDebugEnabled()) return;
    log(DEBUG, object);
    log(DEBUG, throwable);
  }
  
  @Override
  public void trace(Object object) {
    if (!isTraceEnabled()) return;
    log(TRACE, object);
  }
  
  @Override
  public void trace(Object object, Throwable throwable) {
    if (!isTraceEnabled()) return;
    log(TRACE, object);
    log(TRACE, throwable);
  }
  
  @Override
  public void warn(Object object) {
    if (!isWarnEnabled()) return;
    log(WARN, object);
  }
  
  @Override
  public void warn(Object object, Throwable throwable) {
    if (!isWarnEnabled()) return;
    log(WARN, object);
    log(WARN, throwable);
  }
  
  @Override
  public void info(Object object) {
    if (!isInfoEnabled()) return;
    log(INFO, object);
  }
  
  @Override
  public void info(Object object, Throwable throwable) {
    if (!isInfoEnabled()) return;
    log(INFO, object);
    log(INFO, throwable);
  }
  
  @Override
  public void error(Object object) {
    if (!isErrorEnabled()) return;
    log(ERROR, object);
  }
  
  @Override
  public void error(Object object, Throwable throwable) {
    if (!isErrorEnabled()) return;
    log(ERROR, object);
    log(ERROR, throwable);
  }
  
  @Override
  public void fatal(Object object) {
    if (!isFatalEnabled()) return;
    log(FATAL, object);
  }
  
  @Override
  public void fatal(Object object, Throwable throwable) {
    if (!isFatalEnabled()) return;
    log(FATAL, object);
    log(FATAL, throwable);
  }

  @Override
  public boolean isDebugEnabled() {
    return debug;
  }
  
  @Override
  public boolean isErrorEnabled() {
    return error;
  }
  
  @Override
  public boolean isFatalEnabled() {
    return fatal;
  }
  
  @Override
  public boolean isInfoEnabled() {
    return info;
  }
  
  @Override
  public boolean isTraceEnabled() {
    return trace;
  }
  
  @Override
  public boolean isWarnEnabled() {
    return warn;
  }

}

