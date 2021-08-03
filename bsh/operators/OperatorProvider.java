package bsh.operators;

import bsh.BshBinding;
import bsh.Capabilities;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OperatorProvider implements Serializable
{
  Map<String, Set<Method>> namedMethods;
  Map<Class<?>, Set<Method>> castMethods;
  
  public OperatorProvider() {
    this.namedMethods = new ConcurrentHashMap<String, Set<Method>>();
    this.castMethods = new ConcurrentHashMap<Class<?>, Set<Method>>();
  }
  
  public void cacheExtendedMethods(final String className) {
    if (!className.startsWith("org.d6r") && !className.startsWith("bsh.op")) {
      return;
    }
    if (!Capabilities.classExists(className)) {
      return;
    }
    
    Class<?> clazz = null;
    try {
      clazz = Class.forName(
        className, false, Thread.currentThread().getContextClassLoader()
      );
    } catch (Throwable t) {
      t.printStackTrace();
      return;
    } 
    final ConcurrentHashMap<String, Set<Method>> allMethods =
      new ConcurrentHashMap<>();
    cacheMethods(clazz, allMethods);
    
    for (final Map.Entry<String,Set<Method>> allCastMethods:
         allMethods.entrySet())
    {
      final String name = allCastMethods.getKey();
      final Set<? extends Method> returnType = allCastMethods.getValue();
      Set<Method> allTypes = this.namedMethods.get(name);
      if (allTypes == null) {
        allTypes = new HashSet<Method>();
        this.namedMethods.put(name, allTypes);
      }
      allTypes.addAll(returnType);
      this.namedMethods.put(name, allTypes);
    } 
    
    final Set<Method> castOpMethods
      = defaultEmpty(namedMethods.get(OperatorType.CAST.getMethodName()));
    
    for (final Method castOpMethod: castOpMethods) {
      final Class<?> returnType = castOpMethod.getReturnType();
      final Class<?>[] typesAssignableFromReturn
        = OperatorUtil.getAllSuperTypes(returnType);
      for (final Class<?> resultType: typesAssignableFromReturn) {
        this.addCastMethod(resultType, castOpMethod);
      }
    } // end for
  }
  
  private void addCastMethod(final Class<?> returnType, final Method method) {
    Set<Method> cmethods = this.castMethods.get(returnType);
    if (cmethods == null) {
      cmethods = new HashSet<Method>();
      this.castMethods.put(returnType, cmethods);
    }
    cmethods.add(method);
  }
  
  private static void cacheMethods(final Class<?> methodProvider, final Map<String, Set<Method>> allMethods) {
    System.setSecurityManager(null);
    try {
      final Method[] methodsWithName = methodProvider.getDeclaredMethods();
      for (int j=0; j<methodsWithName.length; ++j) {
        final Method method = methodsWithName[j];
        final int mods = method.getModifiers();
        final String mname = method.getName();
        if (Modifier.isStatic(mods) && Modifier.isPublic(mods) && (method.isAnnotationPresent(Extension.class) || OperatorType.find(mname) != null)) {
          Set<Method> mset = allMethods.get(mname);
          if (mset == null) {
            mset = new HashSet<Method>();
            allMethods.put(mname, mset);
          }
          mset.add(method);
        }
      } 
    } catch (Throwable e) {
      new RuntimeException(methodProvider.getName(), e).printStackTrace();
    } 
  }
  
  public static ExtendedMethod findMethod(final BshBinding namespace, final String name, final ExtendedMethod cachedMethod, final boolean allowFirstTypeCast, final Class<?>... types) {
    OperatorProvider op = namespace.getExtendedMethodProvider();
    ExtendedMethod em = op.findMethod2(namespace, name, cachedMethod, allowFirstTypeCast, types);
    for (BshBinding parent = namespace.getParent(); em == null && parent != null; em = op.findMethod2(namespace, name, cachedMethod, allowFirstTypeCast, types), parent = parent.getParent()) {
      op = parent.getExtendedMethodProvider();
    } 
    return em;
  }
  
  public ExtendedMethod findMethod2(final BshBinding namespace, final String name, final ExtendedMethod cachedMethod, final boolean allowFirstTypeCast, final Class<?>... types) {
    if (cachedMethod != null && cachedMethod.matchTypes(types)) {
      return cachedMethod;
    }
    final Set<Method> methods = this.namedMethods.get(name);
    if (methods == null) {
      return null;
    }
    
    ExtendedMethod result
      = this.findBestMethod(namespace, types, methods, allowFirstTypeCast);
    if (result == null && !"getAt".equals(name) && !"putAt".equals(name)) {
      boolean arrays = false;
      final Class<?>[] types2 = new Class<?>[types.length];
      final boolean[] isArrayElement = new boolean[types.length];
      for (int i=0; i<types.length; ++i) {
        types2[i] = types[i];
        if (types2[i] != null && types2[i].isArray()) {
          types2[i] = types2[i].getComponentType();
          arrays = true;
          isArrayElement[i] = true;
        } else {
          isArrayElement[i] = false;
        }
      } 
      if (arrays) {
        result = this.findMethod2(
          namespace, name, null, allowFirstTypeCast, types2  
        );
        if (result != null) {
          final ArrayMethod arrayMethod = new ArrayMethod(
            result, types, isArrayElement
          );
          return arrayMethod;
        }
      }
    }
    return result;
  }
  
  public static Method findCastMethod(final BshBinding namespace, final Class<?> classType, final Class<?> targetType, final Method cachedMethod) {
    OperatorProvider op = namespace.getExtendedMethodProvider();
    Method em = op.findCastMethod(classType, targetType, cachedMethod);
    for (BshBinding parent = namespace.getParent(); em == null && parent != null; em = op.findCastMethod(classType, targetType, cachedMethod), parent = parent.getParent()) {
      op = parent.getExtendedMethodProvider();
    } 
    return em;
  }
  
  public Method findCastMethod(final Class<?> classType, final Class<?> targetType, final Method cachedMethod) {
    if (classType == null || targetType == null) {
      return null;
    }
    if (cachedMethod != null && targetType.equals(cachedMethod.getReturnType())) {
      if (classType.isAssignableFrom(cachedMethod.getParameterTypes()[0])) {}
      return cachedMethod;
    }
    return this.findCastMethod(classType, targetType);
  }
  
  private Method findCastMethod(final Class<?> classType, final Class<?> targetType) {
    final Set<Method> methods = this.castMethods.get(targetType);
    if (methods == null) {
      return null;
    }
    for (final Method method : methods) {
      final Class<?>[] types = method.getParameterTypes();
      if (types[0].equals(classType)) {
        return method;
      }
    } 
    return null;
  }
  
  private ExtendedMethod findBestMethod(final BshBinding namespace, final Class<?>[] types, final Set<Method> methods, final boolean allowFirstTypeCast) {
    final int nparms = types.length;
    final int ndim = types.length;
    for (int lda=0; lda<types.length; ++lda) {
      if (types[lda] == null) {
        return null;
      }
    } 
    final int[] var18 = new int[ndim];
    var18[0] = 1;
    if (ndim > 1) {
      var18[1] = 2;
      if (allowFirstTypeCast) {
        var18[1] = 3;
      }
    }
    for (int indices = 2; indices < ndim; ++indices) {
      var18[indices] = var18[indices - 1] * 3;
    } 
    final int[][] methodParameterBits = new int[methods.size()][];
    final Method[] methodsWithName = new Method[methods.size()];
    int methodIndex = 0;
    for (final Method method : methods) {
      methodsWithName[methodIndex] = method;
      final Class<?>[] bestMethod = method.getParameterTypes();
      if (bestMethod.length == nparms) {
        int[] mtypes = new int[nparms];
        for (int castMethods2=0; castMethods2<types.length; ++castMethods2) {
          final Class<?> i = types[castMethods2];
          Method castMethod;
          if (bestMethod[castMethods2] != null) {
            if (bestMethod[castMethods2].equals(i)) {
              mtypes[castMethods2] = 0;
            } else if (bestMethod[castMethods2].isAssignableFrom(i) &&
              bestMethod[castMethods2].isArray() == i.isArray())
            {
              mtypes[castMethods2] = 1;
            } else {
              if (castMethods2 <= 0 && !allowFirstTypeCast) {
                mtypes = null;
                break;
              }
              castMethod = findCastMethod(
                namespace, i, bestMethod[castMethods2], null
              );
              if (castMethod == null) {
                mtypes = null;
                break;
              }
              mtypes[castMethods2] = 2;
            }
          } else {
            // == null
            if (castMethods2 <= 0 && !allowFirstTypeCast) {
              mtypes = null;
              break;
            }
            castMethod = findCastMethod(
              namespace, i, bestMethod[castMethods2], null
            );
            if (castMethod == null) {
              mtypes = null;
              break;
            }
            mtypes[castMethods2] = 2;
          }
        }
        methodParameterBits[methodIndex] = mtypes;
      }
      ++methodIndex;
    } 
    int bestMatchIndex = -1;
    int lowestRank = Integer.MAX_VALUE;
    for (int i=0; i<methodParameterBits.length; ++i) {
      final int[] mtypes = methodParameterBits[i];
      if (mtypes != null) {
        final int computedRank = this.computeRank(mtypes, var18);
        if (computedRank < lowestRank) {
          lowestRank = computedRank;
          bestMatchIndex = i;
        }
      }
    } 
    if (bestMatchIndex < 0) return null;
    
    final Method matchingMethod = methodsWithName[bestMatchIndex];
    final Class<?>[] parameterTypes = matchingMethod.getParameterTypes();
    final Method[] paramCastMethods = new Method[parameterTypes.length];
    for (int i=0; i<paramCastMethods.length; ++i) {
      if (!types[i].isAssignableFrom(parameterTypes[i])) {
        paramCastMethods[i] = findCastMethod(
          namespace, types[i], parameterTypes[i], null
        );
      }
    } 
    return new BasicMethod(matchingMethod, paramCastMethods, types);
  }
  
  public final int computeRank(final int[] indices, final int[] lda) {
    int rank = 0;
    for (int i=0; i<indices.length; ++i) {
      rank += indices[i] * lda[i];
    } 
    return rank;
  }
  
  public static Map<String, Set<Method>> findAllExtendedMethods(
    final BshBinding namespace)
  {
    final ConcurrentHashMap<String,Set<Method>> result =
      new ConcurrentHashMap<>();
    
    BshBinding n = namespace;
    for (; n != null && n.getParent() != null; n = n.getParent()) ;
      
    final OperatorProvider op = n.getExtendedMethodProvider();
    for (final Map.Entry<String,Set<Method>> entry:
         op.namedMethods.entrySet())
    {
      final String mname = entry.getKey();
      final Set<Method> methods = entry.getValue();
      final Set<Method> methodsWithName;
      if (result.containsKey(mname)) {
        methodsWithName = result.get(mname);
      } else {
        result.put(mname, (methodsWithName = new HashSet<Method>()));
      }
      methodsWithName.addAll(methods);
    }
    return result;
  }
  
  
  private static <T> Set<T> defaultEmpty(final Set<? extends T> set) {
    return (set != null)
      ? (Set<T>) (Object) set
      : (Set<T>) (Object) Collections.emptySet();
  }
}

