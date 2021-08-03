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
import bsh.Variable;
import bsh.UtilEvalError;
import bsh.Capabilities;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlContext.AnnotationProcessor;
import org.apache.commons.jexl3.JexlContext.EnhancedAnnotationProcessor;

import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Triple;

import dalvik.system.XClassLoader.InvocationRecord;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.Scope.Frame;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class Context
  implements JexlContext 
{
  
  static LazyMember<Constructor<?>> NAME_CTOR = LazyMember.of(
    "bsh.Name", "<init>", BshBinding.class, String.class
  );
   
  static LazyMember<Method> TO_OBJECT = LazyMember.of(
    "bsh.Name", "toObject", CallStack.class, Interpreter.class
  );
  
  static <T> Class<T> typeof(T o) {
    return (Class<T>) (
      (o != null) ? (Object) o.getClass()
                  : (Object) Object.class);
  }
  
  static String nameof(Object o, boolean simplifyCommon) {
    if (o == null) return "null";
    String className
      = ((o instanceof Class<?>)? ((Class<?>) o): o.getClass()).getName();
    return (simplifyCommon)
      ? ClassInfo.simplifyName(ClassInfo.typeToName(className))
      : ClassInfo.typeToName(className);
  }
  
   
   
   
  // static vars
  protected static final Map<ContextKey, JexlContext> contexts
               = new TreeMap<ContextKey, JexlContext>();
    
  // instance vars
  final JexlContext innerContext;
  public final ContextKey key;
  
  
  public static class InterpreterContext
           implements JexlContext,
                      Serializable
  {  
    private final Interpreter interp;
    private final BshBinding namespace;
    private Map<String, Object> clsImports;
    private Map<String, Variable> bshVarMap;
    
    public InterpreterContext(Interpreter interp) {
      this.interp = interp;
      this.namespace = interp.getNameSpace();
      if (this.namespace == null) System.err.println("namespace == null");
      this.clsImports =
        Reflect.<Map<String, Object>>getfldval(namespace, "importedClasses");
      this.bshVarMap = namespace.getVariables();
    }
    
    @Override
    public Object get(String varname) {
      Variable var = bshVarMap.get(varname);
      if (var == null) {
        try {
          if (this.clsImports == null) {
            this.clsImports
              = Reflect.<Map<String, Object>>getfldval(
                  namespace, "importedClasses"
                );
          }
          // bsh.Name
          Object bshName = NAME_CTOR.get().newInstance(namespace, varname);
          // bsh.Name
          Object obj = TO_OBJECT.get()
              .invoke(bshName, new CallStack(), interp);
          if (obj instanceof ClassIdentifier) {
            return (Class<?>) ((ClassIdentifier)obj).getTargetClass();
          }
        } catch (Throwable e) {
          if ("true".equals(System.getProperty("printStackTrace")))
            e.printStackTrace();
        }
      }
      // var != null
      try {
        Object value = namespace.unwrapVariable(var);
        return value;
      } catch (UtilEvalError uee) {
        // if ("true".equals(System.getProperty("printStackTrace")))
          uee.printStackTrace();
        return null;
      }
    }
    
    @Override
    public boolean has(String name) {
      return clsImports.containsKey(name) || get(name) != null;
    }
    
    @Override
    public void set(String name, Object value) {
      try {
        namespace.setVariable(name, value, false);
      } catch (UtilEvalError uee) {
        // if ("true".equals(System.getProperty("printStackTrace")))
          uee.printStackTrace();
      }
    }
  }
  
  
  public static class ImportAwareContext
           implements JexlContext,
                      Serializable
  {
    private static Map<String, String> clsImports;
    private final  Map<String, Object> vars;
    
    public ImportAwareContext(Map<String, Object> vars) {
      this.vars = vars;
      loadDefaultImports();
    }
    
    @Override
    public Object get(String name) {
      if (clsImports == null) loadDefaultImports();
      
      if (! vars.containsKey(name) && clsImports.containsKey(name)) {
        try {
          return (Class<?>) Class.forName(
            clsImports.get(name), false,
            Thread.currentThread().getContextClassLoader()
          );
        } catch (Throwable ex) {
          if ("true".equals(System.getProperty("printStackTrace")))
            ex.printStackTrace();
        }
      }
      return vars.get(name);
    }
    
    @Override
    public boolean has(String name) {
      return vars.containsKey(name) || clsImports.containsKey(name);
    }
    
    @Override
    public void set(String name, Object value) {
      vars.put(name, value);
    }
    
    public Object readResolve(ObjectInputStream ois) 
      throws ClassNotFoundException, OptionalDataException, IOException
    {
      Object obj = ois.readObject();
      if (obj instanceof ImportAwareContext) {
        loadDefaultImports();
      }
      return obj;
    }
    
    static synchronized void loadDefaultImports() {
      if (clsImports != null && clsImports.size() > 50) return;
      clsImports = new HashMap<String, String>();
      Iterable<String> classNames = (Iterable<String>) (Object)
        Reflector.invokeOrDefault(NameSpace.class, "getDefaultClassImports");
      ClassLoader ldr = Thread.currentThread().getContextClassLoader();
       for (String className: classNames) {
        System.err.println(className);
        try {          
          Class<?> cls = Class.forName(
            className, false, ldr
          );
          String simpleName 
            = Dumper.getSimpleNameWithoutArrayQualifier(cls);
          System.err.println(simpleName);
          clsImports.put(simpleName, className);
        } catch (Throwable e) { 
          if ("true".equals(System.getProperty("printStackTrace"))) 
            e.printStackTrace();
        }
      }
    }
  }
  
  
  public static class OverlayContext
              extends AbstractContext 
           implements JexlContext
  {
    private final JexlContext underlying;
    private final Map<String, Object> overlay;
    
    public OverlayContext(JexlContext base, Map<String, Object> overlay) {
      this.underlying = base;
      this.overlay = overlay;      
    }
    
    @Override
    public Object get(String name) {
      if (overlay.containsKey(name)) {
        return overlay.get(name);
      }
      return underlying.get(name);
    }
    
    @Override
    public boolean has(String name) {
      return overlay.containsKey(name) || underlying.has(name);
    }
    
    @Override
    public void set(String name, Object value) {
      overlay.put(name, value);
    }
  }
  
  
  
  protected Context(Interpreter interp) {
    this.innerContext = new InterpreterContext(interp);
    this.key = ContextKey.of(interp);
  }
  
  protected Context(Map<String, Object> vars) {
    this.innerContext = new ImportAwareContext(vars);
    this.key = ContextKey.of(vars);
  }
  
  protected Context(JexlContext base, Map<String, Object> overlay) {
    this.innerContext = new OverlayContext(base, overlay);
    this.key = ContextKey.of(base, overlay);
  }
  
  
  public static JexlContext valueOf(Interpreter interp) {
    ContextKey key = ContextKey.of(interp);
    if (contexts.containsKey(key)) return contexts.get(key);
    Context context = new Context(interp);
    contexts.put(key, context);
    return context;
  }
  
  public static JexlContext valueOf(Map<String, Object> vars) {
    ContextKey key = ContextKey.of(vars);
    if (contexts.containsKey(key)) return contexts.get(key);
    Context context = new Context(vars);
    contexts.put(key, context);
    return context;
  }
  
  public static JexlContext valueOf(JexlContext ctx, 
  Map<String,Object> overlay) 
  {
    ContextKey key = ContextKey.of(ctx, overlay);
    if (contexts.containsKey(key)) return contexts.get(key);
    Context context = new Context(ctx, overlay);
    contexts.put(key, context);
    return context;
  }
  
  public JexlContext overlay(Map<String,Object> overlay) {
    ContextKey key = ContextKey.of(overlay, innerContext);
    if (contexts.containsKey(key)) return contexts.get(key);
    JexlContext context = new OverlayContext(innerContext, overlay);
    contexts.put(key, context);
    return context;
  }
  
  public static JexlContext valueOf(ContextKey key) {
    if (contexts.containsKey(key)) return contexts.get(key);
    throw new IllegalArgumentException(String.format(
      "Key does not refer to a valid Context: %s", key
    ));
  }
  
  
  @Override
  public Object get(String name) {
    return innerContext.get(name);
  }
  
  @Override
  public boolean has(String name) {
    return innerContext.has(name);
  }
  
  @Override
  public void set(String name, Object value) {
    innerContext.set(name, value);
  }
  
}



class AbstractContext
implements JexlContext,
           AnnotationProcessor,
           EnhancedAnnotationProcessor
{
  
  public final Map<String, Object> defaultMap= new HashMap<>();  
  public final List<AnnotatedCall<AnnotationProcessor>>
    annotatedCalls = new ArrayList<>();
  
  
  public AbstractContext() {
  }
    
  @Override
  public Object get(String name) {
    System.err.printf(
      "%s.get('%s')\n",
      getClass().getSimpleName(),
      name
    );
    return defaultMap.get(name);
  }
  
  @Override
  public boolean has(String name) {
    System.err.printf(
      "%s.has('%s')\n",
      getClass().getSimpleName(),
      name
    );
    return defaultMap.containsKey(name);
  }
  
  @Override
  public void set(String name, Object value) {
    System.err.printf(
      "%s.set('%s', %s%s)\n",
      getClass().getSimpleName(),
      name,
      value != null
        ? String.format(
            "(%s) ", ClassInfo.typeToName(value.getClass().getName())
          )
        : "",
      toString(value)
    );
    defaultMap.put(name, value);
  }
  
  public String toString(Object value) {
    try {
      return value != null? value.toString(): "null";
    } catch (Throwable e) {
      Throwable cause = ExceptionUtils.getRootCause(e);
      if (cause == null) cause = e;
      String message = Reflect.getfldval(e, "detailMessage");
      message = (message != null) ? String.format(": %s", message): "";
      return String.format(
        "%s@%08x <%s threw %s%s at %s>",
        value.getClass().getName(),
        ClassInfo.simplifyName(
          ClassInfo.typeToName(value.getClass().getName())
        ),
        e.getClass().getSimpleName(),
        message,
        e.getStackTrace().length != 0? e.getStackTrace()[0]: "[empty stack]"
      );
    }
  }
  
  @Override
  public Object processAnnotation(final String name, final Object[] args, 
  final Callable<Object> proceed)
    throws Exception
  {
    StringBuilder sb = new StringBuilder();
    for (Object value: (args != null? args: new Object[0])) {
      sb.append(sb.length() != 0? ", ": "").append(toString(value));
    }    
    System.err.printf(
        "%s.processAnnotation(\n"
      + "  name: '%s', \n"
      + "  args[%d]: %s, \n"
      + "  proceed: (%s) %s \n"
      + ")\n", ClassInfo.typeToName(getClass().getName()), name,
      args != null? args.length: -1, args != null? sb: "null",
      ClassInfo.typeToName(proceed.getClass().getName()), toString(proceed)
    );
    
    final AnnotatedCall<AnnotationProcessor> call = new 
          AnnotatedCall<>(name, args, proceed, this);
    annotatedCalls.add(call);
    System.err.println(call);
    
    Object result = proceed.call();
    System.err.printf(
      "result: (%s) %s\n",
      result != null
        ? ClassInfo.typeToName(result.getClass().getName())
        : "null",
      toString(result)
    );
    return result;
  }
  
  public class AnnotatedCall<AP extends AnnotationProcessor> {
    public final InvocationRecord trace;
    public final String name;
    public final List<Object> args;
    public final JexlNode statement;
    public final Scope.Frame frame;
    public final JexlContext context;
    public final Callable<Object> proceed;
    public final Reference<AP> processor;
    
    public AnnotatedCall(final String name, final Object[] args, 
    final JexlNode statement, final Scope.Frame frame,
    final JexlContext context, final Callable<Object> proceed,
    final AP processor)
    {
      this.trace = new InvocationRecord(
        AbstractContext.this, // receiver,
        AbstractContext.this.getClass(), // classOfReceiver
        "processAnnotation",
        new Object[]{ name, args, statement, frame, context, proceed }
      );
      this.name = name;
      this.args = Arrays.asList(
        (args != null)? CollectionUtil.clone(args): new Object[0]
      );
      this.statement = statement;
      this.frame = frame;
      this.context = context;
      this.proceed = proceed;
      this.processor = new SoftReference<>(processor);
    }
    
    public AnnotatedCall(final String name, final Object[] args, 
    final Callable<Object> proceed, final AP processor)
    {
      this.trace = new InvocationRecord(
        AbstractContext.this, // receiver,
        AbstractContext.this.getClass(), // classOfReceiver
        "processAnnotation",
        new Object[]{ name, args, proceed }
      );
      this.name = name;
      this.args = Arrays.asList(
        (args != null)? CollectionUtil.clone(args): new Object[0]
      );
      this.statement = null;
      this.frame = null;
      this.context = null;
      this.proceed = proceed;
      this.processor = new SoftReference<>(processor);
    }
  }
  
  @Override
  public Object processAnnotation(String name, Object[] args,
  JexlNode statement, Scope.Frame frame, JexlContext context,
  Callable<Object> proceed)
    throws Exception
  {
    StringBuilder sb = new StringBuilder();
    for (Object value: (args != null? args: new Object[0])) {
      sb.append(sb.length() != 0? ", ": "").append(toString(value));
    }    
    System.err.printf(
        "%s.processAnnotation(\n"
      + "  name: '%s', \n"
      + "  args[%d]: %s, \n"
      + "  statement: %s, \n"
      + "  frame: %s, \n"
      + "  context: %s, \n"
      + "  proceed: (%s) %s \n"
      + ")\n", ClassInfo.typeToName(getClass().getName()), name,
      args != null? args.length: -1, args != null? sb: "null",
      Debug.ToString(statement), Debug.ToString(frame), toString(context),
      ClassInfo.typeToName(proceed.getClass().getName()), toString(proceed)
    );
    
    final AnnotatedCall<AnnotationProcessor> call = new 
      AnnotatedCall<>(name, args, statement, frame, context, proceed, this);
    annotatedCalls.add(call);
    System.err.println(call);
    
    Object result = proceed.call();
    System.err.printf(
      "result: (%s) %s\n",
      result != null
        ? ClassInfo.typeToName(result.getClass().getName())
        : "null",
      toString(result)
    );
    return result;
  }
  
  
}

