package org.d6r;
import org.d6r.annotation.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.apache.commons.jexl3.internal.Closure;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.io.FileUtils;
import java.util.concurrent.Callable;
import com.google.common.base.Function;
import bsh.operators.Extension;
import bsh.BshBinding;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.Name;
import bsh.ClassIdentifier;
import bsh.CallStack;
import java.nio.charset.Charset;
import java.io.PrintStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
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
import org.d6r.annotation.*;
import org.apache.commons.jexl3.internal.Script;
import android.util.FastImmutableArraySet;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.Scope.Frame;
import org.apache.commons.jexl3.internal.Interpreter;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.internal.Closure;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.regex.*;

import org.apache.commons.jexl3.internal.*;
import org.apache.commons.jexl3.internal.Scope.Frame;
import org.apache.commons.jexl3.parser.*;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.*; 
import org.apache.commons.jexl3.internal.Interpreter;


@Retention(RetentionPolicy.RUNTIME)
@interface Command {
  String value() default "";
}


final class EnvironmentDetection {
  // must be boxed
  static ThreadLocal<? extends Reference<?>> container;
  
  public static <T> T get(Class<T> cls) {
    if (container             == null 
    ||  container.get()       == null
    ||  container.get().get() == null) {
      if (! isInterpreterPresent()) return null;
    }
    ThreadLocal<WeakReference<Object>> mContainer
      = (ThreadLocal<WeakReference<Object>>) (ThreadLocal<?>) container;
    if (mContainer == null) return null;
    WeakReference<Object> mRef = mContainer.get();
    if (mRef == null) return null;
    Object mReferent = mRef.get();
    if (mReferent == null) return null;
    if (cls.isInstance(mReferent)) return cls.cast(mReferent);
    throw new IllegalStateException(String.format(
      "EnvironmentDetection.container's reference currently holds an "
      + "instance of type '%s' (@ %08x), but the requested type is "
      + "incompatible (via EnvironmentDetection.get(%s.class)",
      ClassInfo.typeToName(mReferent.getClass().getName()),
      System.identityHashCode(mReferent),
      ClassInfo.typeToName(cls.getName())
    ));
  }
  
  public static synchronized void setBindingRef(final Object namespaceValue) 
  {
    ThreadLocal<WeakReference<Object>> mContainer
     = (container == null || container.get() == null)
        ? new ThreadLocal<WeakReference<Object>>()
        :    (ThreadLocal<WeakReference<Object>>) (ThreadLocal<?>) container;
    // System.err.printf("setting mContainer to namespaceValue: %s\n", 
    //   namespaceValue);
    mContainer.set(new WeakReference<Object>(namespaceValue));
    container = mContainer;
  }
  
  public static boolean isInterpreterPresent() {
      try {
        ClassLoader ctxLoader
          = Thread.currentThread().getContextClassLoader();
        Class<?> bindingHolderClass
          = Class.forName("bsh.Interpreter$BindingHolder", true, ctxLoader);
        Field namespaceField 
          = bindingHolderClass.getDeclaredField("$$global$$");
        namespaceField.setAccessible(true);
        Object namespaceValue = namespaceField.get(null);
        if (namespaceValue == null) {
          return false;
        }
        Class<?> namespaceClass = namespaceValue.getClass();
        Class<?> bindingClass
          = Class.forName("bsh.BshBinding", true, ctxLoader);
        if (bindingClass.isAssignableFrom(namespaceClass)) {
          setBindingRef(namespaceValue);
          return true;
        }
        return false;
      } catch (ReflectiveOperationException ex) {
        // if (Boolean.TRUE.toString().equals(System.getProperty("debug"))) {
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
        return false;
        // }
      } catch (Throwable ex) {
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
        // if (Boolean.TRUE.toString().equals(System.getProperty("debug"))) {
        throw Reflector.Util.sneakyThrow(ex);
        // }
      }
  }
  
  
}


public class FunctionUtil {
  
  public static void main(String... args) {
    try {
      Object ctx = getContext();
      System.err.println(ctx);
      System.err.println(Debug.ToString(((Context)ctx).innerContext));
      System.err.println(Debug.ToString(ctx));
      if (ctx != null) {
        Dumper.dump(EnvironmentDetection.class);
        System.err.println(dumpMembers.dumpMembers(ctx));
      }
      System.err.println(Debug.ToString(ctx));
    } catch (Throwable e) {
      Dumper.dump(EnvironmentDetection.class);
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      System.exit(1);
    }
  }
  
  static JexlLogger _logger;
  static JexlEngine _engine;
  
  static JexlContext _defaultContext;
  static Map<String, Object> _defaultMap;
  
  @NonDumpable
  public static final List<Throwable> errors = new ArrayList<Throwable>();
  static short nextErrorIndex = 0;
  public static boolean printErrors;
  
  
  public static JexlLogger getLogger() {
    if (_logger == null) _logger = new JexlLogger();
    return _logger;
  }
  
  public static Map<String, Object> getDefaultMap() {
    if (_defaultMap == null) {
      _defaultMap = new HashMap<String, Object>(64);
    }
    return _defaultMap;
  }
  
  public static JexlContext getContext() {
    if (_defaultContext == null) {
      boolean hasIt = EnvironmentDetection.isInterpreterPresent();
      
      /*System.err.printf(
        "EnvironmentDetection.isInterpreterPresent() returned: %s\n", hasIt
      );*/
      
      
      _defaultContext = null;
      if (hasIt) {
        BshBinding binding 
          = EnvironmentDetection.get(BshBinding.class);
        /*System.err.printf(
          "EnvironmentDetection.get(BshBinding.class) returned: %s\n", 
          Debug.ToString(binding)
        );*/
        if (binding != null) {
          bsh.Interpreter in = binding.getInterpreter();
          /*System.err.printf(
            "binding.getInterpreter() returned: %s\n", 
            Debug.ToString(in)
          );*/
          if (in != null) {
            _defaultContext = Context.valueOf(in);
            /*System.err.printf(
              "Context.valueOf(in) returned: %s\n", 
              Debug.ToString(_defaultContext)
            );*/
            if (_defaultContext != null) return _defaultContext;
          }
          
        }
      }
      System.err.printf("falling back to broken context!!\n");
      
      _defaultContext = Context.valueOf(getDefaultMap());
      
    }
    return (_defaultContext);
  }
  
  public static JexlEngine getEngine() {
    if (_engine == null) {
      JexlBuilder jexlBuilder = new JexlBuilder();
      // TODO: Fix ctor selection with null args
      JexlUberspect us
        = new Uberspect(getLogger(), jexlBuilder.strategy());
      jexlBuilder.uberspect(us);
      _engine = jexlBuilder.create();
    }
    return _engine;
  }
  
  @Command("evalJexl")
  @Extension
  public static <T> T eval(String exprText) {
    return eval(exprText, false);
  }
  
  @Command("evalJexl")
  public static <T> T eval(String exprText, boolean logIt) {
    return eval(
      (Iterable<Map.Entry<String, Object>>) (Object) Collections.emptyList(),
      new String[]{ exprText }
    );
  }
  
  @Command("evalJexl")
  public static <T> T eval(final Iterable<Map.Entry<String, Object>> vars,
  final String... exprTexts)
  {
    final JexlEngine jexlEngine = getEngine();
    final String exprText = StringUtils.join(exprTexts, "\n");
    
    boolean empty = (vars.iterator().hasNext() == false);
    final JexlContext jexlContext;
    if (empty) {
      jexlContext = getContext();
    } else {
      jexlContext = Context.valueOf(getContext(), RealArrayMap.toMap(vars));
    }
    return eval(jexlEngine, jexlContext, exprText);
  }
  
  @Command("evalJexl")
  public static <T> T eval(JexlEngine jexlEngine, JexlContext jexlContext, 
  String exprText)
  {
    Script jexlScript = (Script) jexlEngine.createScript(exprText);
    return (T) (Object) jexlScript.evaluate(jexlContext);
  }
  
  public static <T> T delegate(Class<T> iface, String... strs) {
    if (strs[0].trim().indexOf("return") != 0) {
      strs[0] = String.format("return %s", strs[0]);
    }
    return Delegate.create(
      iface, 
      (Closure) eval(getEngine(), getContext(), StringUtils.join(strs,'\n')),
      getContext()
    );
  }
  
  public static <T> T delegate(Class<T> iface,
  Iterable<Map.Entry<String, Object>> vars, String... strs)
  {
    if (strs[0].trim().indexOf("return") != 0) {
      strs[0] = String.format("return %s", strs[0]);
    }
    Map<String, Object> map = RealArrayMap.toMap(vars);
    JexlContext context = Context.valueOf(getContext(), map);
    return Delegate.create(
      iface, 
      (Closure) eval(getEngine(), context, StringUtils.join(strs, '\n')),
      context
    );
  }
  
  // Helpers
  
  
  @Extension public static <V, R>
  List<R> select(Iterable<? extends V> itb, final Closure closure) {
    final Function<V, R> func = toFunction(closure);
    final List<R> outList = new ArrayList<R>(32);
    
    for (final V crnt: itb) {
      try {
        final R result = func.apply(crnt);
        if (result == null) continue; 
        outList.add(result);
      } catch (Throwable e) {
        onError(e);
        continue;
      }
    }
    return outList;
  }
  
  @Extension public static <V, R>
  R[] select(V[] arr, Closure closure) {
    return (R[]) CollectionUtil.toArray(select(Arrays.asList(arr), closure));
  }
    
  @Extension public static <V, R>
  R[] where(V[] arr, Closure closure) {
    final JexlContext ctx = getContext();
    final List items = select(Arrays.asList(arr), closure);
    //final Callable callable = closure.callable(getContext());
    final Object[] args = new Object[1];
    for (final Iterator it = items.iterator(); it.hasNext();) {
      final Object elem = it.next();
      if (elem == null) {
        it.remove();
        continue; 
      }      
      args[0] = elem;
      Object result = closure.execute(ctx, args);
      boolean keep = (boolean) 
          (result instanceof Boolean)
            ? (boolean) ((Boolean) result).booleanValue()
            : (boolean) ((result instanceof Number)
                ? (boolean) (Integer.valueOf(
                    ((Number) result).intValue()
                  ).compareTo(Integer.valueOf(0)) != 0)
                : (boolean) (result != null));
      if (!keep) it.remove();
    }
    return (R[]) CollectionUtil.toArray((List<R>) (Object) items);
  }
  
  
  @Extension public static <V, R>
  List<R> where(Iterable<V> items, Closure closure) {
    final JexlContext ctx = getContext();
    final List retList = new ArrayList();
    final Object[] args = new Object[1];
    for (final Iterator it = items.iterator(); it.hasNext();) {
      final Object elem = it.next();
      if (elem == null) continue;
      args[0] = elem;
      Object result = closure.execute(ctx, args);
      boolean keep = (boolean) 
          (result instanceof Boolean)
            ? (boolean) ((Boolean) result).booleanValue()
            : (boolean) ((result instanceof Number)
                ? (boolean) (Integer.valueOf(
                    ((Number) result).intValue()
                  ).compareTo(Integer.valueOf(0)) != 0)
                : (boolean) (result != null));
      if (!keep) continue; 
      retList.add(elem);
    }
    return (List<R>) (Object) retList;
  }
  
  /*public static Function<V, R> bind(String expr, Object... vars) {
    map = RealArrayMap.toMap(Arrays.asList(Pair.of("r", 10)));
    // String src = "m -> { Integer.parseInt(m) + r + 6; }"; 
    Parser parser1
      = new org.apache.commons.jexl3.parser.Parser(new StringReader(src));
    try {
      s = parser1.parse(      
        new org.apache.commons.jexl3.JexlInfo("<stdin>", 1, 1),
        src,
        new org.apache.commons.jexl3.internal.Scope(null, null),
        true, // allowRegisters
        false // expression
      );
    } catch (org.apache.commons.jexl3.JexlException.Parsing e) { 
      src = String.format("return ({ %s });", src);
      parser1 = new Parser(new StringReader(src));
      s = parser1.parse(
        new JexlInfo("<stdin>", 1, 1),
        src,
        new org.apache.commons.jexl3.internal.Scope(null, null),
        true,
        false
      );
    }    
    Closure clo = new org.apache.commons.jexl3.internal.Closure(
      (interp = new org.apache.commons.jexl3.internal.Interpreter(
        FunctionUtil.getEngine(),
        Context.valueOf(FunctionUtil.getContext(), map),
        s.scope.createFrame(null))
      ),
      s.script().children[0]
    );
    
    fn = new Function() {
      public Object apply(Object paramValue) { 
        return clo.execute(
          Context.valueOf(FunctionUtil.getContext(), map),
          paramValue
        );
      }
      public String toString() {
        return clo.toString()
          + "with " 
          + StringUtils.join(map.entrySet(), ", ");
      }
    };
    return fn;
  }*/
  
  public static <V, W, R> 
  BoundClosure<V, W, R> newClosure(String script, JexlContext context) {
    return new BoundClosure<V, W, R>(script, context);
  }
  
  public static <V, W, R> 
  BoundClosure<V, W, R> newClosure(String script) {
    return new BoundClosure<V, W, R>(script, getContext());
  }
  
  static final Matcher SIMPLE_NAME_MCHR
    = Pattern.compile("^.*[.$]([^.$]+)$").matcher("");

  public static <V, W, R> 
  BoundClosure<V, W, R> bind(String script, Object... dataKeysValues) {
    if (!script.startsWith("return ")) script = "return + ({ "+script+" })";
    
    Map<String, Object> overlayMap = new HashMap<>();
    
    Queue<Object> q = new ArrayDeque<>();
    Collections.addAll(q, dataKeysValues);
    // System.err.println(q);
    String key = null;
    Object val = null;
    boolean gotKey = false, gotValue = false;
    // System.err.println(q);
    
    while (! q.isEmpty() || (gotKey && gotValue)) {
      if (!gotKey) {
        // key == null
        // System.err.println("key == null, so popping next.");
        Object o = q.poll();
        // System.err.printf("o = (q.poll()) -> %s\n    queue now: %s\n", o, q);
        if (o instanceof String) {
          // System.err.printf(" - o is String, so assigning it to 'key'.\n");
          key = (String) o;          
          gotKey = true;
        } else if (o instanceof CharSequence) {
          key = ((CharSequence) o).toString();
          gotKey = true;
        } else if (o instanceof Class) {
          key = SIMPLE_NAME_MCHR.reset(((Class<?>) o).getName())
            .replaceFirst("$1");
          val = (Class<?>) o;
          gotKey = gotValue = true;
        } else if (o instanceof Map.Entry) {
          key = ((Map.Entry<String,Object>)o).getKey();
          val = ((Map.Entry<String,Object>)o).getValue();          
          gotKey = gotValue = true;
        } else if (o instanceof Iterable<?>) {
          for (final Object elem: ((Iterable<?>) o)) q.offer(elem);
        } else if (o instanceof Object[]) {
          for (final Object elem: ((Object[]) o)) q.offer(elem);
        } else if (o == null || o instanceof Void) {
          key = null;
          gotKey = true;
        } else {
          System.err.printf(
            "[WARN] Don't know how to put %s [%s] into jexl context map.\n",
            o.getClass().getName(), Debug.ToString(o)
          );
        }
        continue;
      }
      if (!gotValue) {
        val = q.poll();
        gotValue = true;
        // System.err.printf("val = (q.poll()) -> %s\n    queue now: %s\n", val, q);
      }
      if (gotKey && gotValue) {        
        overlayMap.put(key, val);
        gotKey = gotValue = false;
      }
    }
        
    return new BoundClosure<V, W, R>(
      script, overlayMap
    );
  }
  
  
  public static class BoundClosure<V, W, R>
           implements com.google.common.base.Function<V, R>, 
                      java8.util.function.Function<V, R>,
                      java8.util.function.BiFunction<V, W, R>,
                      java8.util.function.Predicate<V>,
                      SelectTransformer<V, R>     
  {
    final JexlContext ctx;
    final Closure closure;
    final org.apache.commons.jexl3.internal.Interpreter interp;
    
    public BoundClosure(String text, Map<String, Object> overlay) {
      this(text, ((Context) FunctionUtil.getContext()).overlay(overlay));
    }
    
    public BoundClosure(String text, JexlContext context) {
      try {
        JexlEngine eng = FunctionUtil.getEngine();
        Parser parser = new Parser(new StringReader(text));
        JexlInfo info = eng.createInfo();
        Scope scope0 = new Scope(null, new String[0]);
        ASTJexlScript ast = parser.parse(info, text, scope0, true, false); 
        org.apache.commons.jexl3.internal.Script script
          = Reflect.newInstance(
              org.apache.commons.jexl3.internal.Script.class,
              (org.apache.commons.jexl3.internal.Engine) eng,
              text,
              (org.apache.commons.jexl3.parser.ASTJexlScript) ast
            ); 
        String[] names = script.getParameters(); 
        Scope scope = new Scope(null, names);
        Frame frame = scope.createFrame(null);
        org.apache.commons.jexl3.internal.Interpreter in 
          = Reflector.invokeOrDefault(
              eng, "createInterpreter", new Object[]{ context, frame }
            );
        this.interp = in;
        final Object x = script.evaluate(context);
        if (!(x instanceof Closure)) {
          throw new IllegalArgumentException(String.format(
            "The Jexl script:\n  %s\ndid not evaluate() to a Closure. "
            + "Result was a %s: %s",
            script, x != null? x.getClass().getName(): "null", x
          ));
        }
        this.ctx = context;
        this.closure = (Closure) x;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }
    
    @Override
    public String toString() {
      return String.format(
        "%s\t\t[BoundClosure]", closure
      );
    }
    
    @Override
    public R apply(V param) {
      try {
        return (R) closure.execute(ctx, param);
      } catch (Throwable e) { 
        e.printStackTrace();
        Reflector.Util.sneakyThrow(e);
        onError(e); 
        return null;
      }
    }
    
    @Override
    public R apply(V param1, W param2) {
      try {
        return (R) closure.execute(ctx, param1, param2);
      } catch (Throwable e) { 
        e.printStackTrace();
        Reflector.Util.sneakyThrow(e);
        onError(e); 
        return null;
      }
    }
    
    @Override
    public R transform(V param) {
      return apply(param);
    }
    
    public List<R> applyAll(Iterable<? extends V> itb) {
      final Iterator<? extends V> it = itb.iterator();
      V crnt;
      R result;
      ArrayList<R> outList = new ArrayList<R>(32);
      while (it.hasNext()) {
        crnt = it.next();
        //System.err.println(crnt);
        result = apply(crnt);
        //System.err.println(result);
        if (result == null) continue; 
        outList.add(result);
      }
      return outList;
    }
    
    @Override
    public List<R> select(Iterable<? extends V> itb) {
      return applyAll(itb);
    }
    
    
    @Override
    public boolean test(V param) {
      try {
        Object obj = closure.execute(ctx, param);
        if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        if (obj instanceof Number) {
          if (((Number) obj).equals(Integer.valueOf(0))) return false;
          return true;
        }
        return false;
      } catch (Throwable e) { 
        onError(e);         
        return false;
      }
    }
  }
  
  
  public static class JexlFunction<V, R>
    implements com.google.common.base.Function<V, R>, 
               java8.util.function.Function<V, R>,
               SelectTransformer<V, R>
  {
    final JexlContext ctx;
    final Closure closure;
    
    
    public JexlFunction(JexlContext ctx, Closure closure) {
      this.ctx = ctx;
      this.closure = closure;
    }
    
    public JexlFunction(Closure closure) {
      this(getContext(), closure);
    }
    
    public String toString() {
      return String.format(
        "%s\t[JexlFunction]", closure
      );
    }
    
    @Override
    public R apply(V param) {
      try {
        return (R) closure.execute(ctx, param);
      } catch (Throwable e) { 
        onError(e); 
        return null;
      }
    }
    
    @Override
    public R transform(V param) {
      return apply(param);
    }
    
    public List<R> applyAll(Iterable<? extends V> itb) {
      final Iterator<? extends V> it = itb.iterator();
      V crnt;
      R result;
      ArrayList<R> outList = new ArrayList<R>(32);
      while (it.hasNext()) {
        crnt = it.next();
        result = apply(crnt);
        if (result == null) continue; 
      }
      return outList;
    }
    
    @Override
    public List<R> select(Iterable<? extends V> itb) {
      return applyAll(itb);
    }
        
    public R[] applyAll(V... arr) {
      Class<V> cmpClass 
        = (Class<V>) arr.getClass().getComponentType();
      R result;
      ArrayList<R> outList = new ArrayList<R>(32);
      for (V crnt: arr) {
        result = apply(crnt);
        if (result == null) continue; 
      }
      try {
        return (R[]) outList.toArray(
          (R[]) Array.newInstance(cmpClass, 0)
        );
      } catch (Throwable e) {
        if (e instanceof ArrayStoreException 
        ||  e instanceof ClassCastException) {
          return outList.size() > 0
            ? (R[]) CollectionUtil.toArray(outList)
            : (R[]) Array.newInstance(cmpClass, 0);
        }
        onError(e); 
        return (R[]) new Object[0];
      }
    }
    
  }
  
  @Extension
  public static <V, R>
  List<R> select(Iterable<? extends V> itb, BoundClosure closure) 
  {
    return closure.select(itb);
  }
  
  @Extension
  public static <V, R> R[] select(V[] arr, BoundClosure closure) 
  {
    return CollectionUtil.<R>toArray(closure.select(Arrays.asList(arr)));
  }
  
  
  @Extension public static <V, R>
  JexlFunction<V, R> toFunction(final Closure closure) {
    return new JexlFunction(closure);
  }
  
  
  
  
  static void onError(Throwable thr) { errors.add(thr); }
  static void throwAny(Throwable t) { FunctionUtil.<Error>throwAny0(t); }
  static <T extends Throwable> void throwAny0(Throwable t) throws T {
    throw (T) t;
  }
  
}