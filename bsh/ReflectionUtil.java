package bsh;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtil {
  private static final String TYPE_CLASS_NAME_PREFIX = "class ";
  private static final String TYPE_INTERFACE_NAME_PREFIX = "interface ";

  public static String getClassName(Type type) {
    if(type == null) {
      return "";
    } else {
      String className = type.toString();
      if(className.startsWith("class ")) {
        className = className.substring("class ".length());
      } else if(className.startsWith("interface ")) {
        className = className.substring("interface ".length());
      }

      return className;
    }
  }

  public static Class getClass(Type type) throws ClassNotFoundException {
    String className = getClassName(type);
    return className != null && !className.isEmpty()?Class.forName(className):null;
  }

  public static Object newInstance(Type type) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class clazz = getClass(type);
    return clazz == null?null:clazz.newInstance();
  }

  public static Type[] getParameterizedTypes(Object object) {
    Type superclassType = object.getClass().getGenericSuperclass();
    return !ParameterizedType.class.isAssignableFrom(superclassType.getClass())?null:((ParameterizedType)superclassType).getActualTypeArguments();
  }

  public static boolean hasDefaultConstructor(Class clazz) throws SecurityException {
    Class[] empty = new Class[0];

    try {
      clazz.getConstructor(empty);
      return true;
    } catch (NoSuchMethodException var3) {
      return false;
    }
  }

  public static Class getFieldClass(Class clazz, String name) {
    if(clazz != null && name != null && !name.isEmpty()) {
      Class propertyClass = null;
      Field[] var6;
      int var5 = (var6 = clazz.getDeclaredFields()).length;

      for(int var4 = 0; var4 < var5; ++var4) {
        Field field = var6[var4];
        field.setAccessible(true);
        if(field.getName().equalsIgnoreCase(name)) {
          propertyClass = field.getType();
          break;
        }
      }

      return propertyClass;
    } else {
      return null;
    }
  }

  public static Class getMethodReturnType(Class clazz, String name) {
    if(clazz != null && name != null && !name.isEmpty()) {
      name = name.toLowerCase();
      Class returnType = null;
      Method[] var6;
      int var5 = (var6 = clazz.getDeclaredMethods()).length;

      for(int var4 = 0; var4 < var5; ++var4) {
        Method method = var6[var4];
        if(method.getName().equals(name)) {
          returnType = method.getReturnType();
          break;
        }
      }

      return returnType;
    } else {
      return null;
    }
  }

  public static Object getEnumConstant(Class clazz, String name) {
    return clazz != null && name != null && !name.isEmpty()?Enum.valueOf(clazz, name):null;
  }
}
