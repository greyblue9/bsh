package org.d6r;

import java.lang.AssertionError;
import java.lang.Class;
import java.lang.NoSuchFieldException;
import java.lang.ReflectiveOperationException;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Throwable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.d6r.Func;

public class AccessorFunc<T, R>
  implements Func<T, R>,
             AutoCloseable
{ 
  protected static final int NO_ARITY = Integer.MIN_VALUE;
  
  protected final Reference<? extends Member> _member;
  protected final Reference<? extends Class<T>> _targetType;
  protected final Reference<? extends Class<R>> _retType;
  protected final boolean _isStatic;
  protected final int _arity;
  
  protected AccessorFunc(final Member member) {
    _member = new SoftReference<>((Member) member);
    _targetType = new SoftReference<>((Class<T>) member.getDeclaringClass());
    _retType = new SoftReference<>((Class<R>) getReturnType(member));
    _isStatic = Modifier.isStatic(member.getModifiers());
    _arity = getArity(member);
  }
  
  
  public static <T, R>
  AccessorFunc<T,R> getForField(final Class<T> targetClass,final String name)
  {
    Throwable error = null;
    Field mb = null;
    for (Class<?> c = targetClass; c != null; c = c.getSuperclass()) {
      try {
        (mb = c.getDeclaredField(name)).setAccessible(true);
        return new AccessorFunc<T, R>(mb);
      } catch (ReflectiveOperationException ex) {
        if (error == null)
          error = ex;
        if (!(ex instanceof NoSuchFieldException))
          break;
      }
    }
    final RuntimeException e = new RuntimeException(String.format(
        "Could not resolve field '%s' in class '%s' or any superclass; " +
        "The first error was: %s", name, targetClass.getName(), error));
    if (error != null)
        e.initCause(error);
    throw e;
  }
  
  @Override
  public R apply(T instance) {
    final Member m = _member.get();
    
    if ( !(_isStatic || m instanceof Constructor<?>)
      && ! _targetType.get().isInstance(instance))
    {
      throw new IllegalArgumentException(String.format(
        "%s.apply(T[:%s] instance): instance must be of type '%s', or a " +
        "subclass thereof; Got: '%s' instance. Selected member: %s",
        ClassInfo.simplifyName(
          ClassInfo.typeToName(_targetType.get().getName())),
        ClassInfo.simplifyName(
          ClassInfo.typeToName(_retType.get().getName())),
        m
      ));      
    }
    
    final Object inst = (_isStatic || m instanceof Constructor<?>)
      ? null : instance;
    if (m instanceof Field) {
      try {
        return (R) ((Field) m).get(inst);
      } catch (ReflectiveOperationException roEx) {
        throw new RuntimeException(String.format(
          "%s: Field[%s].get(instance: %s) threw %s.",
          getClass().getSimpleName(), ((Field) m).toGenericString(),
          (inst != null)? ToStringBuilder.reflectionToString(inst): "null"
        ), roEx);
      }
    }
    
    final Object[] args = new Object[_arity];
    boolean ready = false;
    if (_arity == 0) {
      ready = true;
    } else if (instance != null) {
      // arity >= 1
      if (instance.getClass() == Object[].class) {
        final int argsProvided
          = Math.min(((Object[]) instance).length, args.length);
        System.arraycopy((Object[]) instance, 0, args, 0, argsProvided);
        ready = true;
      } else if (inst != instance) {
        // arity >= 1,
        // instance is not Object[]
        args[0] = instance;
        ready = true;
      }
    }
    if (!ready) throw new UnsupportedOperationException(String.format(
      "%s: Incorrect arguments for %s: got: %s; built: %s",
      getClass().getSimpleName(), m,
      instance instanceof Object[]
        ? Arrays.deepToString((Object[]) instance)
        : String.valueOf(instance),
      Arrays.deepToString(args)
    ));
    
    try {
      if (m instanceof Method) {
        final Method md = (Method) m;
        return (R) md.invoke(instance, args);
      }
      final Constructor<?> ctor = (Constructor<?>) m;
      return (R) (Object) ctor.newInstance(args);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  protected static <U> Class<U> getReturnType(final Member m) {
    Class<?> ret = null;
    if (m instanceof Field) ret = ((Field) m).getType();
    else if (m instanceof Method) ret = ((Method) m).getReturnType();
    else {
      if (!(m instanceof Constructor<?>)) throw new AssertionError();
      ret = m.getDeclaringClass();
    }
    return (Class<U>) (Class<?>) ret;
  }
  
  protected static int getArity(final Member m) {
    if (m instanceof Field) return NO_ARITY;
    if (m instanceof Method) {
      return ((Method) m).getParameterTypes().length;
    }
    return ((Constructor<?>) m).getParameterTypes().length;
  }
  
  @Override
  public void close() {
    System.err.printf(
      "[WARN] AccessorFunc for %s is being closed!\n",
      _member.get()
    );
    this._member.clear();
    this._targetType.clear();
    this._retType.clear();
  }
}
