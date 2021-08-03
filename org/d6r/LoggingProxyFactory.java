package org.d6r;

import java.lang.reflect.*;
import java.util.*;


/**
public final class java.lang.StackTraceElement {
    // As java.lang.StackTraceElement
    
    // Fields
    private static final int NATIVE_LINE_NUMBER = -2;
    String declaringClass = <null>;
    String fileName = <null>;
    String methodName = <null>;
    int lineNumber = <null>;
    
    // Constructors
    private StackTraceElement java.lang.StackTraceElement();
    public StackTraceElement java.lang.StackTraceElement(String, String, String, int);
    
    // Methods
    public boolean equals(Object);
    public String getClassName();
    public String getFileName();
    public int getLineNumber();
    public String getMethodName();
    public int hashCode();
    public boolean isNativeMethod();
    public String toString();
  }
*/
public class LoggingProxyFactory {
  
  public static <T> T newProxy(T object, Class<T> interfaceCls)
  {
    try { 
      return (T) Proxy.newProxyInstance(
        interfaceCls.getClassLoader(),
        new Class<?>[]{ interfaceCls },
        new LoggingInvocationHandler<T>((T)object)
      );
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
    }
    return null;
  }
  
  
  public static class LoggingInvocationHandler<T> 
    implements InvocationHandler 
  {  
    public T underlying;
    
    public LoggingInvocationHandler(T underlying) {
      this.underlying = underlying;
    }
    
    public Object invoke(Object pxy, Method m, Object[] args)
    {
      if (m.getName().equals("hashCode")) {
        return underlying.hashCode();
      }
      
      StringBuffer sb = new StringBuffer();
      sb.append(Debug.ToString(underlying));
      sb.append(".");
      sb.append(m.getName()); sb.append("(");
      Object ret = null;
      try {
        for (int i=0; args != null && i<args.length; i++) {
          if (i != 0)
          sb.append(", ");
          try {
            sb.append(Debug.ToString(args[i]));
          } catch (Throwable ex) {
            sb.append(args[i]);
          }
        }
        sb.append(")");
        ret = m.invoke(underlying, args);
        if (ret != null) {
          sb.append(" -> "); sb.append(Debug.ToString(ret));
        }
        System.out.println(sb);
      } catch (Exception e) {
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      }
      StackTraceElement ste = Debug.getCallingMethod(); 
      sb.append(String.format(
        "\n  called by %s.%s(..)\n  (%s:%d)",
        ste.getClassName(), ste.getMethodName(),
        ste.getFileName(), ste.getLineNumber()
      ));
      if (m.getReturnType().isInterface()
        && ! "hashCode".equals(m.getName()))
      {
        String stack = CollectionUtil.toString(
          new Error().getStackTrace()
        );
        int lpf1 = stack.indexOf("LoggingProxyFactory");
        if (lpf1 != -1) {
          int lpf2 = stack.indexOf(
            "LoggingProxyFactory", lpf1 + 1
          );
          if (lpf2 != -1) {
            int lpf3 = stack.indexOf(
              "LoggingProxyFactory", lpf2 + 1
            );
            if (lpf3 != -1) {
              int lpf4 = stack.indexOf(
                "LoggingProxyFactory", lpf3 + 1
              );
              if (lpf4 != -1) {
                return ret;
              }
            }
          }
        }
        return LoggingProxyFactory.newProxy(
          (Object)ret, (Class<Object>)(Object)m.getReturnType()
        );         
      }
      return ret;
    }
  
  }
  
}



