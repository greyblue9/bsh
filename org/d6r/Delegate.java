package org.d6r;

import org.apache.commons.jexl3.internal.Closure;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Function;
import bsh.operators.Extension;
import bsh.Interpreter;
import bsh.BshBinding;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.Name;
import bsh.ClassIdentifier;
import bsh.CallStack;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.*;
import java.util.*;
import java.lang.reflect.Array;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.d6r.FunctionUtil.JexlFunction;
import org.apache.commons.jexl3.internal.Script;

import static bsh.Reflect.getParameterTypes;
import static bsh.Reflect.getReturnType;
import static bsh.Reflect.gatherMethodsRecursive;
import static org.d6r.Reflect.findMostSpecificSignature;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.lang3.*;
import javassist.util.proxy.*;
import bsh.Factory;
import bsh.CallStack;
import bsh.ClassIdentifier;
import bsh.Interpreter;

public class Delegate {
  public static boolean DEBUG = false;
  
  public static final Set<Throwable> errors = new HashSet<Throwable>();
  
  public static class Helper<T> {
    static ProxyFactory pf;
    
    final MethodHandler mh;
    final Object targetInstance;
    final Class<?>[] ifaces;
    ProxyObject inst;
    
    public Helper(MethodHandler mh, Object targetInstance, 
    Class<?>... ifaces) 
    {
      this.mh = mh;
      this.targetInstance = targetInstance;
      this.ifaces = ifaces;
    }
    
    public T getInstance() {
      if (Helper.pf == null) Helper.pf = new ProxyFactory();
      try {
        if (this.inst != null) return (T) (Object) this.inst;
        Helper.pf.setInterfaces(this.ifaces);
        Helper.pf.setHandler(this.mh);        
        this.inst = (ProxyObject) Helper.pf.createClass().newInstance();
        return (T) (Object) this.inst;
      } catch (Throwable e) { 
        if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
        throw Reflector.Util.sneakyThrow(e);
      }
    }
    
    public Object getTarget() {
      return targetInstance;
    }
  }
  
    
  /**
  public class Object {
  
    public void <init>()
    private native Object internalClone(Cloneable var0)
    protected Object clone() throws CloneNotSupportedException
    public boolean equals(Object o)
  
    @FindBugsSuppressWarnings(FI_EMPTY)
    protected void finalize() throws Throwable
    public final native Class<?> getClass()
    public native int hashCode()
    public final native void notify()
    public final native void notifyAll()
    public String toString()
    public final void wait() throws InterruptedException
    public final void wait(long millis) throws InterruptedException
    public final native void wait(long var0, int var1) 
      throws InterruptedException
  }
  */
  
  public static <T> Object invoke(Interpreter in, CallStack cs, 
  final ClassIdentifier<T> ifaceClass, final ClassIdentifier<?> methodClass, 
  final String methodName)
  {
    return create(ifaceClass, methodClass, methodName);
  }
  
  
  public static <T> Object invoke(Interpreter in, CallStack cs, 
  final ClassIdentifier<T> ifaceClass, final Object targetInstance, 
  final String methodName)
  {
    if (targetInstance instanceof ClassIdentifier<?>) return create(
      ifaceClass.getTargetClass(), 
      ((ClassIdentifier<?>) targetInstance).getTargetClass(), 
      (Object) null,
      methodName
    );
    
    return create(
      ifaceClass.getTargetClass(), 
      ((Object) targetInstance).getClass(), 
      (Object) targetInstance,      
      methodName
    );
  }
  
  
  public static <T> 
  T create(ClassIdentifier<T> ifaceClassId, ClassIdentifier<?> methodClassId,
  String methodName)
  {
    return create(
      ifaceClassId.getTargetClass(), methodClassId.getTargetClass(), 
      methodName
    );
  }
  
  
  public static <T> T create(final Class<T> iface, final Class<?> mtdClass, 
  final String mtdName)
  {
    return create(iface, mtdClass, (Object) null, mtdName);
  }
  
  
  static final String[] objectMethodNames = {
    "clone", "equals", "finalize", "getClass", "hashCode", "internalClone", 
    "notify", "notifyAll", "toString", "wait"
  };
  
  static String typeToString(Type type) {
    return (type instanceof Class)
      ? ClassInfo.simplifyName(ClassInfo.typeToName(
          ((Class<?>) type).getName()
        )) 
      : String.valueOf(type);    
  }
  
  public static <T> T create(Class<T> iface, Closure targetInstance) {
    return create(iface, targetInstance, FunctionUtil.getContext());
  }
  
  public static <T> T create(final Class<T> iface,
  final Closure targetInstance, final JexlContext context) 
  {
    final Member selected;
    int idx = 0;
    Method[] ifaceMethods = iface.getDeclaredMethods();
    
    Object[] params = targetInstance.getParameters();
    if (params == null) params = new Object[0];
    int numParams = params.length;
    Method ifaceMethod = null;
    outer:
    for (int i=0; i<=2; i++) {
      for (Method m: ifaceMethods) {
        String methodName = m.getName();
        if (i < 2 && Arrays.binarySearch(
          objectMethodNames, 0, objectMethodNames.length,
          methodName) < 0
        ) 
        {
          continue; 
        }
        if (i < 1 && m.getParameterTypes().length != numParams) continue;    
        ifaceMethod = m;
        break outer;
      }
    }
    final Method ifaceMd = ifaceMethod;
    
    final Helper[] hlp = new Helper[1];
    MethodHandler _mh = new MethodHandler() {
      
      String _cachedToString;

      public Object invoke(Object self, Method overridden, Method forwarder, 
      Object[] args) throws Throwable
      {
        final Helper<T> helper = (Helper<T>) (Helper<?>) hlp[0];
        final MethodHandler mh = helper.mh;
        final Object target = helper.getTarget();        
        
        if (DEBUG) {
          System.err.printf(
            "overridden: %s\n", dumpMembers.colorize(overridden));
          System.err.printf(
            "forwarder:  %s\n", 
            forwarder != null?  dumpMembers.colorize(forwarder): "null");
        }
        
        String origName = overridden.getName();
        Class<?>[] pTypes = overridden.getParameterTypes();
        Class origClass = overridden.getDeclaringClass();
        
        if (origName.equals("hashCode") && pTypes.length == 0) {
          return self != null
            ? System.identityHashCode(self)
            : System.identityHashCode(mh);
        }
        if (origName.equals("equals") && pTypes.length == 1) {
          return self != null
            ? self == args[0]
            : mh   == args[0];
        }
        if (origName.equals("finalize") && pTypes.length == 0) {
          return null;
        }
        if (origName.equals("clone") && pTypes.length == 0) {
          return CollectionUtil.clone(
            this, Reflect.allocateInstance(getClass()),
            new HashSet(), 0, 12
          );
        }
        if (origName.equals("toString") && pTypes.length == 0) {
          if (_cachedToString == null) {
            Method[] objMethods = Object.class.getDeclaredMethods(); 
            Set objMdNames = new TreeSet();
            for (Method md: objMethods) { 
               objMdNames.add(md.getName());
            } 
            Method ifaceMd = null; 
            for (Method md: iface.getDeclaredMethods()) { 
              if (objMdNames.contains(md.getName())) continue; 
              ifaceMd = md; 
              break;
            }
            StringBuilder sb = new StringBuilder(76); 
            Type[] gptypes = ifaceMd.getGenericParameterTypes(); 
            String[] cpnames = targetInstance.getParameters(); 
            for (int i=0; i<cpnames.length; i++) { 
              if (i > 0) sb.append(", "); 
              sb.append(typeToString(gptypes[i])).append(' ')
                .append(cpnames[i]);
            }
            Type rettype = ifaceMd.getGenericReturnType(); 
            sb.insert(0, " ".concat(ifaceMd.getName()).concat("(")); 
            sb.insert(0, typeToString(rettype));
            sb.append(") ").append(StringUtils.substringBeforeLast(
              StringUtils.substringAfter(
                targetInstance.toString(), "->"
              ), "}"
            )).append("\n} "); 
            sb.insert(0, ' ').insert(
              0, Modifier.toString(ifaceMd.getModifiers() 
                   & (~(Modifier.ABSTRACT)))
            ); 
            sb.append("  implements ").append(typeToString(iface));
            _cachedToString = sb.toString();
          }
          return _cachedToString;
        }
        
        if (origClass == Object.class && forwarder != null) {
          return forwarder.invoke(self, args);
        }
        
        if (origClass == Object.class
        || ((forwarder != null 
          && (forwarder.getDeclaringClass().equals(Object.class)
           )))
        ) {          
          return null;
        }
        
        return targetInstance.execute(context, args);
      }
    };
    
    final Helper<T> helper = new Helper<T>(_mh, targetInstance, iface);
    
    
    hlp[0] = helper;
    
    T obj = helper.getInstance();
    return (T) obj;
  }

  
  public static <T> T create(final Class<T> iface, final Class<?> mtdClass, 
  final Object targetInstance, final String mtdName)
  {
    final Member selected;
    int idx = 0;
    Method[] ifaceMethods = iface.getDeclaredMethods();
    final Method ifaceMethod = ifaceMethods[idx];
    Class<?>[] ifaceMethodParamTypes = ifaceMethod.getParameterTypes();
    Class<?> ifaceMethodRetType = ifaceMethod.getReturnType();
    List<Member> allMethods = new ArrayList<Member>();
    Member _selected = Reflect.findMember(
      mtdClass, mtdName, ifaceMethodParamTypes
    );
    final Helper[] hlp = new Helper[1];
    
    
    List<Class<?>[]> sigs = new ArrayList<Class<?>[]>();
    sigs.add(getParameterTypes(_selected));
    
    Class<?>[][] sigArr = new Class<?>[sigs.size()][];    
    System.arraycopy(
      sigs.toArray(new Class[0][]), 0, sigArr, 0, sigArr.length
    );
    
    int midx = _selected != null? 0: -1;
    
    if (_selected == null) {
      throw new RuntimeException(String.format(
        "Cannot find a compatible method named %s in the class %s; "
        + "candidates are:\n\n - %s\n", 
        mtdName, mtdClass.getName(), StringUtils.join(allMethods, ",\n - ")
      ));
    }
    
    selected = _selected;
    final Method selectedMethod 
      = (selected instanceof Method)? (Method) selected: null;
    final Constructor<?> selectedCtor
      = (selected instanceof Constructor)? (Constructor<?>) selected: null;
    
    System.err.println(dumpMembers.colorize(selected));
    
    MethodHandler _mh = new MethodHandler() {

      public Object invoke(Object self, Method overridden, Method forwarder, 
      Object[] args) throws Throwable
      {
        final Helper<T> helper = (Helper<T>) (Helper<?>) hlp[0];
        final MethodHandler mh = helper.mh;
        final Object target = helper.getTarget();
        
        
        if (DEBUG)System.err.printf(
          "overridden: %s\n", dumpMembers.colorize(overridden)
        );
       
        if (DEBUG)System.err.printf(
          "forwarder:  %s\n", 
          forwarder != null?  dumpMembers.colorize(forwarder): "null"
        );
        
        String origName = overridden.getName();
        Class<?>[] pTypes = overridden.getParameterTypes();
        Class origClass = overridden.getDeclaringClass();
        
        if (origName.equals("hashCode") 
        &&  Arrays.equals(pTypes, new Class[0])) 
        {
          return self != null
            ? System.identityHashCode(self)
            : System.identityHashCode(mh);
        }
        if (origName.equals("equals")
        &&  Arrays.equals(pTypes, new Class[]{ Object.class }))
        {
          return self != null
            ? self == args[0]
            : mh   == args[0];
        }
        if (origName.equals("finalize")
        &&  Arrays.equals(pTypes, new Class[0]))
        {
          return null;
        }
        if (origName.equals("toString")
        &&  Arrays.equals(pTypes, new Class[0]))
        {
          return String.format(
            "Delegate@%08x{\n  "
            + "iface = %s.class,\n  "
            + "ifaceMethod = %s,\n  "
            + "targetClass = %s\n  "
            + "targetMethod = %s\n  "
            + "}",
            (self != null)
              ? System.identityHashCode(self)
              : System.identityHashCode(mh),
            ClassInfo.typeToName(iface.getName()),
            ifaceMethod.toGenericString(),
            ClassInfo.typeToName(mtdClass.getName()),
            selectedMethod.toGenericString()
          );            
        }
        
        
        if (origClass == Object.class && forwarder != null) {
          return forwarder.invoke(self, args);
        }
        
        if (origClass.equals(Object.class)
        || ((forwarder != null 
          && (forwarder.getDeclaringClass().equals(Object.class) 
            || args.length != getParameterTypes(selected).length)
           ))
        ) {          
          return null;
        }
        
        
        if (overridden.getName().equals(origName)) {
          Object receiver = null;
          if (selected instanceof Method) {
            Method md = (Method) selected;
            Class<?> dc = md.getDeclaringClass();
            if (Modifier.isStatic(md.getModifiers())) {
              receiver = null;
            } else { // instance method
              Class<?>[] sParams = md.getParameterTypes();
              
              if (target != null && dc.isInstance(target)) {
                System.err.printf(
                  "[INFO] promoted 'target' (%s) to receiver\n",
                  ClassInfo.typeToName(target.getClass().getName())
                );
                receiver = target;
              } else 
              if (self != null && dc.isInstance(self)) {
                System.err.printf(
                  "[INFO] promoted 'self' (%s) to receiver\n",
                  ClassInfo.typeToName(self.getClass().getName())
                );
                receiver = self;
              } else 
              if (args.length > 0 && args[0] != null
               && args.length == sParams.length + 1) 
              {
                if (dc.isAssignableFrom(args[0].getClass()))
                {
                  // shift first arg into receiver position
                  System.err.printf(
                    "[INFO] promoted argument 0 (%s) to receiver\n",
                    ClassInfo.typeToName(args[0].getClass().getName())
                  );
                  receiver = args[0];
                  args = ArrayUtils.remove(args, 0);                  
                }                
              } else if (
                  sParams.length > 0
               && sParams.length == args.length + 1
               || (
                   sParams.length > 0
                && (args.length == 0 
                  || ! sParams[0].isAssignableFrom(args[0].getClass()))))
              {
                
                if (target != null && sParams[0].isAssignableFrom(
                    target.getClass()))
                {
                  System.err.printf(
                    "[INFO] promoted 'target' (%s) to parameter 0\n",
                    ClassInfo.typeToName(target.getClass().getName())
                  );
                  args = ArrayUtils.addAll(new Object[]{ target }, args);
                  receiver = null;
                } else if (
                    self != null && sParams[0].isAssignableFrom(
                    self.getClass()))
                {
                  System.err.printf(
                    "[INFO] promoted 'self' (%s) to parameter 0\n",
                    ClassInfo.typeToName(self.getClass().getName())
                  );
                  args = ArrayUtils.addAll(new Object[]{ self }, args);
                  receiver = null;
                } else if (dc.isAssignableFrom(args[0].getClass())) {
                  // shift first arg into receiver position
                  System.err.printf(
                    "[INFO] promoted argument 0 (%s) to receiver\n",
                    ClassInfo.typeToName(args[0].getClass().getName())
                  );
                  receiver = args[0];
                  args = ArrayUtils.remove(args, 0);                  
                }
              }
            }
          } else {
            receiver = null;
          }
          
          
          try {
            Object ret = (selected instanceof Method)
            ? (Object)((Method) selected).invoke(receiver, args)
            : (Object)((Constructor<?>) selected).newInstance(args);
            if (DEBUG) System.err.println(
                "returning " + Factory.typeof(ret)
            );
            return ret;
          } catch (Throwable e) {
            System.err.println(e);
            Dumper.dump(this, 2);
            if (DEBUG) e.printStackTrace();
            
            errors.add(e);
            return null;
          }
          
        }
        
        return null;
      }

      
    };
    
    final Helper<T> helper = new Helper<T>(_mh, targetInstance, iface);
    
    
    hlp[0] = helper;
    
    T obj = helper.getInstance();
    return (T) obj;
  }

}