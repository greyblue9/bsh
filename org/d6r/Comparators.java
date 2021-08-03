package org.d6r;

import java.util.Comparator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.ArrayList;

public class Comparators {
  
  
  public static 
  <T> Comparator<? super T> comparingInt(final String fieldName) 
  {    
    
    return (Comparator<? super T>) new Comparator<T>() 
    {
      List<Throwable> errors = new ArrayList<Throwable>();      
      
      @Override
      public int compare(T o1, T o2) {
        
        Object operand1 = null;
        Object operand2 = null;
        try {
          operand1 = Reflect.getfldval(o1, fieldName);
        } catch (Throwable e) { 
          errors.add(e);
          operand1 = Integer.valueOf(0);         
        }
        try {
          operand2 = Reflect.getfldval(o2, fieldName);
        } catch (Throwable e) { 
          errors.add(e);
          operand2 = Integer.valueOf(0);         
        }
        if (operand1 instanceof Long) {
          operand1 
          = Integer.valueOf((int) ((Long)operand1).longValue());
        }
        if (operand2 instanceof Long) {
          operand2 
          = Integer.valueOf((int) ((Long)operand2).longValue());
        }
        if (operand1 instanceof Short) {
          operand1 
          = Integer.valueOf((int) ((Short)operand1).shortValue());
        }
        if (operand2 instanceof Short) {
          operand2 
          = Integer.valueOf((int) ((Short)operand2).shortValue());
        }
        if (!(operand1 instanceof Integer)) {
          try {
            operand1 = Integer.valueOf(
              Integer.parseInt(String.valueOf(operand1))
            );
          } catch (Throwable e) {
            errors.add(e);
          }
        }
        if (!(operand2 instanceof Integer)) {
          try {
            operand2 = Integer.valueOf(
              Integer.parseInt(String.valueOf(operand2))
            );
          } catch (Throwable e) {
            errors.add(e);
          }
        }
        
        Integer op1 = (Integer) operand1;
        Integer op2 = (Integer) operand2;
        
        return op1.compareTo(op2);
        
      }
      @Override
      public boolean equals(Object o) {
        return o != null 
            && System.identityHashCode(this)
            == System.identityHashCode(o);
     }
   };
  }
  
  public static 
  <T> Comparator<? super T> 
  comparingString(final String fieldName) 
  { 
    return (Comparator<? super T>) new Comparator<T>() 
    {
      @Override
      public int compare(T o1, T o2) {
        return 
          ((String) Reflect.getfldval(o1, fieldName))
            .compareTo(
              ((String) Reflect.getfldval(o2, fieldName))
            );
      }
      @Override
      public boolean equals(Object o) {
        return o != null 
            && System.identityHashCode(this)
            == System.identityHashCode(o);
     }
   };
  }
  
  public static <T> Comparator<T>
  comparingInvocation(final Class<? super T> declaringClass, 
  final String methodName, final Class<?>[] parameterTypes,
  final Object[] args) 
  { 
    final Method method = Reflect.findMethod(
      declaringClass, methodName, parameterTypes
    );
    if (method == null) {
      throw new RuntimeException(String.format(
        "Comparators.comparingInvocation: "
        + "No matching method found: %s.%s(%s)",
        declaringClass.getName(), methodName,
        StringUtils.join(parameterTypes)
      ));
    }
    
    return (Comparator<T>) new Comparator<T>() 
    {
      @Override
      public int compare(T o1, T o2) {
        Object result1 = null;
        Object result2 = null;
        try {
          result1 = method.invoke(o1, args);          
        } catch (ReflectiveOperationException e) {
          Throwable ex = e;
          if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException) ex)
              .getTargetException();
          }
          throw new RuntimeException(String.format(
            "Invocation of %s on %s threw %s: %s",
            method.toGenericString(), o1, 
            ex.getClass().getSimpleName(),
            ex.getMessage()
          ));
        }
        try {
          result2 = method.invoke(o2, args);          
        } catch (ReflectiveOperationException e) {
          Throwable ex = e;
          if (ex instanceof InvocationTargetException) {
            ex = ((InvocationTargetException) ex)
              .getTargetException();
          }
          throw new RuntimeException(String.format(
            "Invocation of %s on %s threw %s: %s",
            method.toGenericString(), o2,
            ex.getClass().getSimpleName(),
            ex.getMessage()
          ));
        }
        if (result1 instanceof Comparable) {
          return (
            (Comparable<Object>) (Object) result1
          ).compareTo((Object) result2);
        }
        if (result1 instanceof String) {
          return ((String) result1).compareTo(
            String.valueOf(result2)
          );
        }
        if (result1 instanceof Number) {
          return Double.valueOf(((Number)result1).doubleValue())
            .compareTo(
                 Double.valueOf(((Number)result2).doubleValue())
            );
        }
        return new ToStringComparator().compare(result1, result2);
      }
      
      @Override
      public boolean equals(Object o) {
        return o != null 
            && System.identityHashCode(this)
            == System.identityHashCode(o);
     }
   };
  }
  
}