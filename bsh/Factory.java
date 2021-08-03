package bsh;

import bsh.BlockNameSpace;
import bsh.BlockNameSpaceFactory;
import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.ExternalNameSpace;
import bsh.ExternalNameSpaceFactory;
import bsh.InstanceId;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.NameSpaceFactory;
import bsh.Primitive;
import bsh.This;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.d6r.ClassPathUtil2;
import org.d6r.Debug;
import org.d6r.dumpMembers;

public class Factory<U extends BshBinding> {
  public static boolean REUSE = true;
  protected Class<U> clazz;
  protected static BshClassManager classManager = null;
  protected Map<String, U> nsMap = new HashMap();
  protected static Map<String, This> thisMap = new HashMap();
  public static final String NS_SEPARATOR = "/";
  public static final int NS_SEPARATOR_LEN = "/".length();

  public Factory() {
    try {
      this.clazz = (Class)((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    } catch (Exception var2) {
      this.clazz = (Class)BshBinding.class;
    }

  }

  public static This getThis(NameSpace namespace, Interpreter declInterp) {
    This thiz = (This)thisMap.get(namespace.getName());
    if(thiz == null) {
      thiz = new This(namespace, declInterp);
      thisMap.put(namespace.getName(), thiz);
    }

    return thiz;
  }

  public static Class<?> typeof(Object object) {
    return object == null?Object.class:object.getClass();
  }

  public static <T extends BshBinding> Factory<T> get(Class<T> cls) {
    if(Interpreter.DEBUG) {
      StackTraceElement retFactory = Debug.getCallingMethod();
      if(Interpreter.TRACE) {
        System.err.printf(":: %s.%s(in %s @ line %s) is requesting a Factory<%s>.\n", new Object[]{retFactory.getClassName(), retFactory.getMethodName(), retFactory.getFileName(), Integer.valueOf(retFactory.getLineNumber()), cls.getSimpleName()});
      }
    }

    Object retFactory1;
    if(NameSpace.class.equals(cls)) {
      retFactory1 = NameSpaceFactory.INSTANCE;
    } else if(BlockNameSpace.class.equals(cls)) {
      retFactory1 = BlockNameSpaceFactory.INSTANCE;
    } else {
      if(!ExternalNameSpace.class.equals(cls)) {
        throw new RuntimeException(String.format("Factory<%s> is not implemented", new Object[]{cls.getName()}));
      }

      retFactory1 = ExternalNameSpaceFactory.INSTANCE;
    }

    if(Interpreter.DEBUG) {
      System.err.printf("  -> Factory returning instance: %s\n", new Object[]{retFactory1.toString()});
    }

    return (Factory)retFactory1;
  }

  public static String getNameSpaceNameFromArgs(Class<?> nsClass, Object... args) {
    String nameSuffix = null;
    BshBinding parent = null;
    Object[] var7 = args;
    int var6 = args.length;

    for(int var5 = 0; var5 < var6; ++var5) {
      Object arg = var7[var5];
      if(arg != null) {
        if(nameSuffix == null && arg instanceof String) {
          nameSuffix = (String)arg;
        } else if(parent == null && parent instanceof BshBinding) {
          parent = (BshBinding)arg;
        }
      }
    }

    return makeName(parent, nameSuffix, nsClass);
  }

  public static String makeName(BshBinding parent, String nameSuffix, Class<?> thizClass) {
    if(nameSuffix == null) {
      if(thizClass == BlockNameSpace.class) {
        nameSuffix = "AnonymousBlock";
      } else if(thizClass == NameSpace.class) {
        nameSuffix = "Anonymous";
      } else if(thizClass == ExternalNameSpace.class) {
        nameSuffix = "AnonymousExternalMap";
      } else {
        nameSuffix = "AnonymousUnknown:".concat(thizClass.getSimpleName());
      }
    }

    if(parent == null) {
      return nameSuffix;
    } else {
      String parentName = parent.getName();
      int pnPos = nameSuffix.indexOf(parentName);
      if(pnPos == 0) {
        int name = nameSuffix.indexOf("/");
        if(name == parentName.length()) {
          nameSuffix = nameSuffix.substring(name + NS_SEPARATOR_LEN);
        }
      }

      String name1 = parentName.concat("/").concat(nameSuffix);
      int lastGlobalIdx = name1.lastIndexOf("/global");
      if(lastGlobalIdx != -1) {
        name1 = name1.substring(lastGlobalIdx + 1);
      }

      return name1;
    }
  }

  public U make(Object... args) {
    boolean useCache = true;
    InstanceId id = null;
    if(classManager == null) {
      Object[] pidx = args;
      int ctor = args.length;

      for(int u = 0; u < ctor; ++u) {
        Object clazz = pidx[u];
        if(clazz instanceof BshClassManager) {
          if(Interpreter.TRACE) {
            System.err.printf("Factory<%s> stealing class manager <%s> from call to make()..\n", new Object[]{this.clazz.getSimpleName(), clazz});
          }

          classManager = (BshClassManager)clazz;
          if(clazz instanceof InstanceId) {
            useCache = false;
            id = (InstanceId)clazz;
            System.err.println(Debug.ToString(args));
          }
        }
      }
    }

    BshBinding var30;
    if(REUSE && useCache) {
      String var28 = getNameSpaceNameFromArgs(this.clazz, args);
      var30 = (BshBinding)this.nsMap.get(var28);
      if(var30 != null) {
        if(Interpreter.DEBUG) {
          System.err.println("Returning cached NS: " + var28);
        }

  return (U) (Object) var30;
      }
    }

    Class var29 = this.clazz;
    if(id != null) {
      var29 = BlockNameSpace.class;
    }

    if(Interpreter.TRACE) {
      System.err.println(Arrays.toString(var29.getDeclaredConstructors()));
    }

    Constructor var31 = findBestMatch(var29.getDeclaredConstructors(), args);
    if(Interpreter.DEBUG) {
      System.err.println(dumpMembers.colorize(var31));
      System.err.println(this.getClass());
      System.err.println(var31);
    }

    int var32 = -1;
    int var10;
    if(classManager != null) {
      Class[] var11;
      var10 = (var11 = var31.getParameterTypes()).length;

      for(int ex2 = 0; ex2 < var10; ++ex2) {
        Class ex = var11[ex2];
        ++var32;
        if(ex.isAssignableFrom(BshClassManager.class) && args[var32] == null) {
          if(Interpreter.DEBUG) {
            System.err.printf("Factory intervening for constructor %s\n", new Object[]{var31});
          }

          args[var32] = classManager;
        }
      }
    }

    if(Interpreter.DEBUG) {
      try {
        System.err.println(this.getClass().getGenericSuperclass());
        System.err.println(Arrays.deepToString(args));
        System.err.println(var31.toGenericString());
        System.err.println("  " + Arrays.toString(Thread.currentThread().getStackTrace()).replaceAll(", ", "\n  "));
      } catch (Throwable var24) {
        System.err.println("Factory: Error printing: " + var24.toString());
      }
    }

    try {
      var30 = (BshBinding)var31.newInstance(args);
      if(Interpreter.DEBUG) {
        try {
          System.err.printf("make() ctor.newInstance() returned: %s\n", new Object[]{Debug.ToString(var30)});
        } catch (Throwable var23) {
          ;
        }

        System.err.printf("make() putting entry into nsMap: [\"%s\": %s]\n", new Object[]{var30.getName(), var30});
      }

      this.nsMap.put(var30.getName(), (U) var30);
      if(Interpreter.DEBUG) {
        try {
          System.err.printf(Debug.ToString(var30), new Object[0]);
        } catch (Throwable var22) {
          ;
        }
      }

  return (U) (Object) var30;
    } catch (Exception var27) {
      System.err.println("ctor.newInstance() threw:");
      if(Interpreter.DEBUG) {
        var27.printStackTrace();
      }

      try {
        var30 = (BshBinding)ClassPathUtil2.getUnsafe().allocateInstance(var29);
        if(Interpreter.DEBUG) {
          System.err.println("falling back to allocateInstance...");
        }

        Object[] var12 = args;
        int var34 = args.length;

        for(var10 = 0; var10 < var34; ++var10) {
          Object var33 = var12[var10];
          if(var33 != null) {
            Class argType = var33.getClass();
            Class cl = var29;

            for(boolean found = false; cl != null && !found; cl = cl.getSuperclass()) {
              Field[] var19;
              int var18 = (var19 = cl.getDeclaredFields()).length;

              for(int var17 = 0; var17 < var18; ++var17) {
                Field f = var19[var17];
                int acc = f.getModifiers();
                if((acc & 8) == 0 && (acc & 1024) == 0 && (acc & 4096) == 0 && f.getType().isAssignableFrom(argType)) {
                  if(Interpreter.TRACE) {
                    System.err.printf("Assigning field: %s\n", new Object[]{f.toGenericString()});
                  }

                  try {
                    if(Interpreter.TRACE) {
                      System.err.printf("  - value: <%s>\n", new Object[]{Debug.toString(var33)});
                    }
                  } catch (Throwable var25) {
                    if(Interpreter.TRACE) {
                      System.err.printf("  - {value}.toString() threw %s\n", new Object[]{var25.getClass().getSimpleName()});
                    }
                  }

                  f.setAccessible(true);
                  f.set(var30, var33);
                  found = true;
                  break;
                }
              }
            }
          }
        }

        if(Interpreter.TRACE) {
          System.err.printf("make() putting: [\"%s\": %s]\n", new Object[]{Arrays.toString(args), Debug.ToString(var30)});
        }

        this.nsMap.put(var30.getName(), (U) var30);
        if(Interpreter.TRACE) {
          System.err.printf("make() returning: %s\n", new Object[]{Debug.ToString(var30)});
        }

  return (U) (Object) var30;
      } catch (Throwable var26) {
        if(Interpreter.DEBUG) {
          var26.printStackTrace();
        }

        System.err.println("make() at catchall");
        System.err.println("make() returning null");
        return null;
      }
    }
  }

  public static <T> Constructor<T> findBestMatch(Constructor<T>[] ctors, Object[] args) {
    int bestScore = 0;
    Constructor bestCtor = null;

    try {
      Constructor[] var7 = ctors;
      int var6 = ctors.length;

      for(int var5 = 0; var5 < var6; ++var5) {
        Constructor e = var7[var5];
        Class[] pTypes = e.getParameterTypes();
        int score = pTypes.length != args.length && !e.isVarArgs()?0:2;
        if(Interpreter.TRACE) {
          System.err.printf("Considering: %s\n", new Object[]{e.toGenericString()});
        }

        if(Interpreter.TRACE) {
          System.err.printf("int score = %d\n", new Object[]{Integer.valueOf(score)});
        }

        if(score > bestScore) {
          if(Interpreter.TRACE) {
            System.err.println("score > bestScore");
          }

          bestScore = score;
          bestCtor = e;
          if(Interpreter.TRACE) {
            System.err.printf("bestScore = %d; bestCtor = %s\n", new Object[]{Integer.valueOf(score), e});
          }
        }

        if((pTypes = e.getParameterTypes()).length != args.length) {
          if(Interpreter.TRACE) {
            System.err.printf("pTypes = %s\nargs.length = %d\nisVarArgs() = %s\npTypes.length > args.length + 1 = %s\n", new Object[]{Arrays.asList(pTypes), Integer.valueOf(args.length), Boolean.valueOf(e.isVarArgs()), Boolean.valueOf(pTypes.length > args.length + 1)});
          }

          if(!e.isVarArgs() && pTypes.length > args.length + 1) {
            if(Interpreter.TRACE) {
              System.err.printf("Skipping: param len (%d) != args.length %d, and either not varargs, or has >= <n. args> + 1 params\n", new Object[]{Integer.valueOf(pTypes.length), Integer.valueOf(args.length)});
            }
            continue;
          }
        }

        score += 3;

        for(int i = 0; i < pTypes.length && i < args.length; ++i) {
          if(Interpreter.TRACE) {
            System.err.printf("pTypes[%d]: %s\n", new Object[]{Integer.valueOf(i), pTypes[i]});
          }

          if(args[i] == null) {
            if(Interpreter.TRACE) {
              System.err.printf("args[i] == null\npTypes[i].isPrimitive() = %s\n", new Object[]{Boolean.valueOf(pTypes[i].isPrimitive())});
            }

            if(pTypes[i].isPrimitive()) {
              if(Interpreter.TRACE) {
                System.err.printf("break;", new Object[0]);
              }
              break;
            }

            if(Interpreter.TRACE) {
              System.err.printf("continue;", new Object[0]);
            }
          } else {
            Class primCls = null;
            if(pTypes[i].isPrimitive()) {
              primCls = (Class)Primitive.wrapperMap.get(args[i].getClass());
            }

            if(!pTypes[i].isAssignableFrom(args[i].getClass()) && (primCls == null || !pTypes[i].isAssignableFrom(primCls))) {
              if(Interpreter.TRACE) {
                System.err.printf("not assignable: %s <- %s\n", new Object[]{pTypes[i], args[i].getClass()});
              }
              break;
            }

            score += 10;
            if(Interpreter.DEBUG) {
              System.err.printf("score += 10 -> %d\n", new Object[]{Integer.valueOf(score)});
            }

            Class a_pTypeSuper = pTypes[i];
            boolean subtypeLevel = false;

            while((a_pTypeSuper = a_pTypeSuper.getSuperclass()) != null) {
              ++score;
              if(Interpreter.DEBUG) {
                System.err.printf("[%s] score += 1 -> %d\n", new Object[]{a_pTypeSuper.getName(), Integer.valueOf(score)});
              }
            }
          }
        }

        if(score > bestScore) {
          if(Interpreter.TRACE) {
            System.err.printf("score of this ctor (%d / %s)\n > score of best ctor (%d / %s)\n", new Object[]{Integer.valueOf(score), e.toGenericString(), Integer.valueOf(bestScore), bestCtor != null?bestCtor.toGenericString():"null"});
          }

          bestCtor = e;
          bestScore = score;
        }
      }

      if(Interpreter.DEBUG) {
        System.err.printf("findBestMatch: bestCtor = %s\n", new Object[]{bestCtor});
      }

      if(bestCtor != null) {
        return bestCtor;
      }
    } catch (Throwable var15) {
      System.err.println("findBestMatch: Exception caught!");
      if(Interpreter.DEBUG) {
        var15.printStackTrace();
      }
    }

    try {
      if(Interpreter.DEBUG) {
        System.err.println("findBestMatch returning Object.<init>");
      }

      return (Constructor)Object.class.getDeclaredConstructor(new Class[0]);
    } catch (Throwable var14) {
      if(Interpreter.DEBUG) {
        var14.printStackTrace();
      }

      System.err.println("findBestMatch returning null");
      return null;
    }
  }
}