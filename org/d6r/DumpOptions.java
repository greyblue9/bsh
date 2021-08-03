package org.d6r;

import java.util.*;
import java.lang.reflect.*;
import java.lang.reflect.*;

public abstract class DumpOptions<C, N> 
  extends Options<C, Set<N>> 
{
  public Type type;
  public Class<C> cmdType;
  public Class<N> setContainedType;
  public Type setType;

  
  public boolean verbose = false;
  public int minDepth = 0;
  public int maxDepth = 6;
  public Set<N> visited = new HashSet<N>();
  
  public DumpOptions() {

    fillInTypes(getClass());
    type = (Type) getClass().getGenericSuperclass();
    
  }

  public void fillInTypes(Class<?> cls) 
  {
    Type superclass = cls.getGenericSuperclass();
    if (superclass instanceof Class) {
      return;
    }
    
    try {
      ParameterizedType pType 
 = (ParameterizedType) superclass;
      cmdType = (Class<C>) pType.getActualTypeArguments()[0];
      setContainedType 
 = (Class<N>) pType.getActualTypeArguments()[1];
      //setContainedType 
      // = (Class<N>) (superclass.getGenericSuperclass(.getActualTypeArguments()[1];
      
      ParameterizedType sett = 
        (ParameterizedType)
          ((ParameterizedType) cls.getSuperclass()
            .getGenericSuperclass()
          ).getActualTypeArguments()[1];
      
      try {
        Field fargs 
 = ParameterizedType.class
            .getDeclaredField("args"); 
        fargs.setAccessible(true); 
        
//        ListOfTypes lot = (ListOfTypes) fargs.get(sett); 
        Object lot = fargs.get(sett); 
        Field ftypes = null;
        for (Field fld: lot.getClass().getDeclaredFields()) {
          if (fld.getType().isAssignableFrom(Collection.class)) {
            ftypes = fld;
            ftypes.setAccessible(true);
            break;
          }
        }
        if (ftypes != null) {
          ParameterizedType superType 
 = (ParameterizedType) cls.getGenericSuperclass();
          List<Type> list = (List<Type>) ftypes.get(lot);
          list.set(0, superType.getActualTypeArguments()[1]);
          setType = sett;
        }
      } catch (Throwable e) {
        System.err.printf(
          "%s: %s\n", 
          e.getClass().getSimpleName(),
          e.getMessage() != null? e.getMessage(): "<no message>"
        );
      }
        
    } catch (Throwable e) {
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace(); 
    }
  }

  
}