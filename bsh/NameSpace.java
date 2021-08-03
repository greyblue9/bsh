package bsh;

import org.d6r.annotation.*;
import bsh.BSHBlock;
import bsh.BSHMethodDeclaration;
import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.BshMethod;
import bsh.CallStack;
import bsh.Capabilities;
import bsh.ClassIdentifier;
import bsh.EvalError;
import bsh.Factory;
import bsh.InstanceId;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.LHS;
import bsh.Modifiers;
import bsh.Name;
import bsh.NameSource;
import bsh.Primitive;
import bsh.Reflect;
import bsh.SimpleNode;
import bsh.This;
import bsh.UtilEvalError;
import bsh.Variable;
import bsh.operators.OperatorProvider;
import com.android.dex.Dex;
import gnu.trove.set.hash.THashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.CollectionUtil;
import org.d6r.Debug;
import org.d6r.DexUtil;
import org.d6r.DisplayType;
import org.d6r.IdentityHashSet;
import org.d6r.annotation.NonDumpable;
import org.d6r.RealArrayMap;
import org.d6r.Reflector;
import org.d6r.StringCollectionUtil;
import org.d6r.WeakHashSet;
import java.util.LinkedList;
import org.apache.commons.lang3.tuple.Triple;
import org.d6r.SoftHashMap;

import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.InputStream;
import static java.lang.String.format;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.d6r.PathInfo;      
import java.util.regex.Pattern;
import static org.d6r.CollectionUtil2.filter;
import java8.util.function.Function;
import static org.d6r.CollectionUtil.asIterable;
import org.d6r.Reflector;
import org.d6r.Reflector.Util;
import static java.lang.System.getenv;


@NonDumpable("NameSpace")
public class NameSpace implements BshBinding, Cloneable {

  public static final long serialVersionUID = 5004976946651004751L;
  public static final NameSpace JAVACODE = new NameSpace((BshClassManager)null, "Called from compiled Java code.");
  Interpreter interpreter;
  public final String name;
  @NonDumpable(replacement = DisplayType.CLASS_NAME)
  public BshBinding parent;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Map<String, Name> names;
  public Map<String, Variable> variables;
  public final Map<String, List<BshMethod>> methods;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Map<String, String> importedClasses;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Set<String> importedPackages;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Set<String> importedCommands;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Set<Object> importedObjects;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Set<Class<?>> importedStatic;
  @NonDumpable(replacement = DisplayType.SIZE)
  public final Set<NameSource.Listener> nameSourceListeners;
  public This thisReference;
  static Boolean defaultsLoaded;
  protected String packageName;
  @NonDumpable("[ExtendedMethod Provider]")
  public static OperatorProvider extendedMethods;
  @NonDumpable(replacement = DisplayType.SIZE)
  public static final Map<String, Class<?>> classCache;
  @NonDumpable(replacement = DisplayType.CLASS_NAME)
  public transient BshClassManager classManager;
  public SimpleNode callerInfoNode;
  protected boolean isMethod;
  protected boolean isClass;
  protected Class<?> classStatic;
  protected Object classInstance;
  public static boolean EMERGENCY;
  public static boolean allowReparent;
  protected ArrayDeque<BshBinding> lastParent;
  public InstanceId id;
  String sn;
  @NonDumpable(replacement = DisplayType.SIZE)
  static List<String> defaultClassImports;
  @NonDumpable(replacement = DisplayType.SIZE)
  static List<String> defaultPackageImports;
  
  @NonDumpable(replacement = DisplayType.SIZE)
  public static Set<BshBinding> all;
  
  @NonDumpable(replacement = DisplayType.SIZE)
  List<Triple<BshBinding, Throwable, String>> getParentCalls;

  static {
    JAVACODE.setIsMethod(true);
    defaultsLoaded = Boolean.TRUE;
    EMERGENCY = true;
    allowReparent = true;
    classCache = new WeakHashMap(64);
  }

  public static <K, V> Map<K, V> newMap() {
    return (Map)(new HashMap());
  }

  public static <K, V> Map<K, V> newMap(int size) {
    return (Map)(new HashMap(size));
  }

  public static <K, V> Map<K, V> newMap(Map<? extends K, ? extends V> map) {
    return (Map)(new HashMap(map));
  }

  public static <E> Set<E> newSet() {
    return (Set)(new THashSet());
  }

  public static <E> Set<E> newSet(int size) {
    return (Set)(new THashSet(size));
  }

  public static <E> Set<E> newSet(Collection<? extends E> coll) {
    return (Set)(new THashSet(coll));
  }

  public NameSpace(BshClassManager cm, BshBinding parent, String name) {
    this.interpreter = null;
    this.packageName = null;
    this.lastParent = new ArrayDeque();
    this.name = Factory.makeName(parent, name, this.getClass());
    this.parent = parent;
    this.classManager = cm;
   if (this.classManager != null) {
      this.classManager.addListener(this);
    }

    this.names = new NameSpace.BindingMap();
    this.variables = new NameSpace.BindingMap();
    this.methods = (Map)newMap();
    this.importedClasses = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedClasses //;
        : new NameSpace.BindingMap();
    this.importedPackages = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedPackages //;
        : newSet();
    this.importedCommands = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedCommands //;
        : newSet();
    this.importedObjects = //;
          newSet();
    this.importedStatic = //;
          newSet();
    this.nameSourceListeners = new IdentityHashSet<>();
    
    ((all == null) ? all = new WeakHashSet(): all).add(this);
    if (parent == null || importedCommandBase == null) {
      this.importCommands("bsh.command");
    }
  }

  public NameSpace(BshBinding parent, BshClassManager cm, String name) {
    this(cm, parent, name);
  }

  public NameSpace(BshClassManager cm, BshBinding parent) {
    this((BshClassManager)cm, (BshBinding)parent, (String)null);
  }

  public NameSpace(BshBinding parent, BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)parent, (String)null);
  }

  public NameSpace(BshClassManager cm, String name) {
    this((BshClassManager)cm, (BshBinding)null, name);
  }

  public NameSpace(String name, BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)null, name);
  }

  public NameSpace(BshBinding parent, String name) {
    this((BshClassManager)null, (BshBinding)parent, name);
  }

  public NameSpace(String name, BshBinding parent) {
    this((BshClassManager)null, (BshBinding)parent, name);
  }

  public NameSpace(BshClassManager cm) {
    this((BshClassManager)cm, (BshBinding)null, (String)null);
  }

  public NameSpace(BshBinding parent) {
    this((BshClassManager)null, (BshBinding)parent, (String)null);
  }

  public NameSpace(String name) {
    this((BshClassManager)null, (BshBinding)null, name);
  }

  public NameSpace(BshClassManager cm, BshBinding parent, String name, InstanceId id) {
    this.interpreter = null;
    this.packageName = null;
    this.lastParent = new ArrayDeque();
    this.id = id;
    this.name = Factory.makeName(parent, name, this.getClass());
    this.parent = parent;
    this.classManager = cm;
   if (this.classManager != null) {
      this.classManager.addListener(this);
    }

    this.names = new NameSpace.BindingMap();
    this.variables = new NameSpace.BindingMap();
    this.methods = (Map)newMap();
    this.importedClasses = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedClasses //;
        : new NameSpace.BindingMap();
    this.importedPackages = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedPackages //;
        : newSet();
    this.importedCommands = //;
      (parent instanceof NameSpace)
        ? ((NameSpace) parent).importedCommands //;
        : newSet();
    this.importedObjects = //;
          newSet();
    this.importedStatic = //;
          newSet();
    this.nameSourceListeners = new IdentityHashSet<>();
    
    ((all == null) ? all = new WeakHashSet(): all).add(this);
    if (parent == null || importedCommandBase == null) {
      this.importCommands("bsh.command");
    }
  }

  public void setClassStatic(Class clas) {
    this.classStatic = clas;
    this.importStatic(clas);
  }

  public void setClassInstance(Object instance) {
    this.classInstance = instance;
    this.importObject(instance);
  }

  public Object getClassInstance() throws UtilEvalError {
   if (this.classInstance != null) {
      return this.classInstance;
    } else if (this.classStatic != null) {
      throw new UtilEvalError("Can\'t refer to class instance from static context.");
    } else {
      throw new InterpreterError("Can\'t resolve class instance \'this\' in: " + this);
    }
  }

  public boolean isMethod() {
    return this.isMethod;
  }

  public boolean isClass() {
    return this.isClass;
  }

  public void setIsMethod(boolean value) {
    this.isMethod = value;
  }

  public void setIsClass(boolean value) {
    this.isClass = value;
  }
  
  @Override
  public Interpreter getInterpreter() {
    if (this.interpreter == null) {
      BshClassManager cm = this.getClassManager();
      if (cm == null) {
        System.err.println(
          "[WARN] NameSpace.getInterpreter(): "
          + "had to go to CollectionUtil to get it!"
        );
        try {
          this.interpreter = CollectionUtil.getInterpreter();
        } catch (Throwable e) {
          e.printStackTrace();
          return null;
        }
      } else {
        this.interpreter = cm.declaringInterpreter;
      }
    }
    return this.interpreter;
  }

  public void setInterpreter(Interpreter value) {
    if (value == null) throw new IllegalArgumentException(
     "NameSpace: I cannot setInterpreter() to NULL!"
    );
    this.interpreter = value;
  }

  public void setName(String name) {
    throw new UnsupportedOperationException("Name is reaad-only");
  }

  public String getName() {
    return this.name;
  }

  public void setNode(SimpleNode node) {
    this.callerInfoNode = node;
  }

  public SimpleNode getNode(boolean recurse) {
   if (this.callerInfoNode != null) {
      return this.callerInfoNode;
    } else if (!recurse) {
      return null;
    } else {
      Object cur = this;
      SimpleNode n = null;

      while((cur = ((BshBinding)cur).getParent()) != null) {
       if ((n = ((BshBinding)cur).getNode()) != null) {
          return n;
        }
      }

      return null;
    }
  }

  public SimpleNode getNode() {
    return this.getNode(true);
  }

  public SimpleNode getCallerInfoNode() {
    return this.getNode();
  }

  public Map<String, Variable> getVariables() {
    return this.variables;
  }

  public String getSimpleName() {
   if (this.sn != null) {
      return this.sn;
    } else {
      String[] sns = StringUtils.split(this.name, "/");
      this.sn = sns.length > 0?sns[sns.length - 1]:"???" + String.valueOf(this.hashCode());
      return this.sn;
    }
  }

  public Object get(String name, Interpreter interpreter) throws UtilEvalError {
    /*if(Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.get(String name=\"%s\", Interpreter interpreter=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, interpreter, this});
    }*/

    CallStack callstack = new CallStack(this);
    Object ret = this.getNameResolver(name).toObject(callstack, interpreter);
   if (Interpreter.DEBUG) {
      System.err.printf("    >>>  get(String name=\"%s\", Interpreter interpreter=%s) on %s \n    returning:  -->  %s\n", new Object[]{name, interpreter, this, ret});
    }

    return ret;
  }

  public void setVariable(String name, Object value, boolean strictJava) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.setVariable(String name=\"%s\", Object value=%s, boolean strictJava=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, value, Boolean.valueOf(strictJava), this});
    }

    boolean recurse = true;
    this.setVariable(name, value, strictJava, recurse);
  }

  public void setLocalVariable(String name, Object value, boolean strictJava) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.setLocalVariable(String name=\"%s\", Object value=%s, boolean strictJava=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, value, Boolean.valueOf(strictJava), this});
    }

    this.setVariable(name, value, strictJava, false);
  }

  public void setVariable(String name, Object value, boolean strictJava, boolean recurse) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.setVariable(String name=\"%s\", Object value=%s, boolean strictJava=%s, boolean recurse=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, value, Boolean.valueOf(strictJava), Boolean.valueOf(recurse), this});
    }

    this.ensureVariables();
   if (value == null) {
      value = Primitive.NULL;
    }

    NameSpace ns = this;
    Variable v = (Variable)this.variables.get(name);
   if (v == null && recurse) {
      while(v == null && (ns = (NameSpace)ns.parent) != null) {
        v = (Variable)ns.variables.get(name);
      }
    }

   if (v != null) {
      try {
        v.setValue(value, Variable.ASSIGNMENT);
        this.nameSpaceChanged();
      } catch (UtilEvalError var11) {
        try {
          org.d6r.Reflect.setfldval(v, "type", value == Primitive.NULL?Object.class:Factory.typeof(value));
          v.setValue(value, Variable.ASSIGNMENT);
          this.nameSpaceChanged();
        } catch (Throwable var10) {
          throw new UtilEvalError("Variable assignment: " + name + ": " + var11.getMessage() + ": " + var10.toString(), var11);
        }
      }
    } else {
      this.variables.put(name, new Variable(name, value, (Modifiers)null));
      this.nameSpaceChanged();
    }

  }

  public void ensureVariables() {
   if (Interpreter.DEBUG) {
      System.err.printf(" ensureVariables() on %s \n", new Object[]{this.name, this});
    }

   if (Interpreter.DEBUG) {
      System.err.printf("variables == %s\n", new Object[]{this.variables});
    }

   if (this.variables == null) {
      this.variables = newMap();
    }

  }

  public Variable unsetVariable(String name) {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.unsetVariable(String name=\"%s\") on %s \n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, this});
    }

   if (this.variables != null) {
      Variable var3;
      try {
        var3 = (Variable)this.variables.remove(name);
      } finally {
        this.nameSpaceChanged();
      }

      return var3;
    } else {
      return null;
    }
  }

  public Variable unset(String name) {
    return (Variable)this.variables.remove(name);
  }

  public String[] getVariableNames() {
    return (String[]) this.variables.keySet().toArray(new String[0]);
  }

  public String[] getMethodNames() {
    return (String[]) this.methods.keySet().toArray(new String[0]);
  }

  public BshMethod[] getMethods() {
    ArrayList ret = new ArrayList();
    Iterator var3 = this.methods.values().iterator();

    while(var3.hasNext()) {
      List list = (List)var3.next();
      ret.addAll(list);
    }

    return (BshMethod[]) ret.toArray(new BshMethod[0]);
  }

  public Map<String, List<BshMethod>> getMethodsByName() {
    return this.methods;
  }

  public BshBinding getParent() {
   if (Interpreter.DEBUG) {
      /*
      if (getParentCalls == null) {
        getParentCalls 
          = new LinkedList<Triple<BshBinding, Throwable, String>>();
      }
      getParentCalls.add(Triple.of(
        (BshBinding) this, 
        (Throwable) new Error(format(
          "getParent() called from %s \"%s\"",
          getClass().getSimpleName(),
          getName()
        )),
        this.parent != null? this.parent.getName(): "<parent is null>"
      ));
        
      System.err.printf(" ---> %s{%s}.getParent()  returning: %s \n            from: %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), this.parent, this});
      
      */
    }

    return this.parent;
  }

  public This getSuper(Interpreter declaringInterpreter) {
    return this.parent != null && this.parent.getThis(declaringInterpreter) != null?this.parent.getThis(declaringInterpreter):this.getThis(declaringInterpreter);
  }

  public This getGlobal(Interpreter declaringInterpreter) {
    return this.parent != null && this.parent.getGlobal(declaringInterpreter) != null?this.parent.getGlobal(declaringInterpreter):this.getThis(declaringInterpreter);
  }

  public This getThis(Interpreter declaringInterpreter) {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.getThis(Interpreter declaringInterpreter=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), declaringInterpreter, this});
    }

   if (this.thisReference == null) {
     if (Interpreter.DEBUG) {
        System.err.printf("  ... thisReferetce == %s\n", new Object[]{this.thisReference});
      }

      this.thisReference = This.getThis(this, declaringInterpreter);
     if (Interpreter.DEBUG) {
        System.err.printf("  ... thisReferetce == %s\n", new Object[]{this.thisReference});
      }
    }

   if (Interpreter.DEBUG) {
      System.err.printf("    >>>  %s{%s}.getThis(Interpreter declaringInterpreter=%s) \n    returning:  -->  %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), declaringInterpreter, this, this.thisReference});
    }

    return this.thisReference;
  }

  public BshClassManager getClassManager() {
    return this.getClassManager(true);
  }

  public BshClassManager getClassManager(boolean createIfNone) {
   if (this.classManager != null) {
      return this.classManager;
    } else if (this.parent != null && this.parent != JAVACODE && this.parent.getClassManager(false) != null) {
      return this.parent.getClassManager(false);
    } else {
     if (this.interpreter != null) {
       if (Interpreter.TRACE) {
          System.err.printf("getClassManager() creating bsh.BshClassManager name=%s, interpreter = %s \n", new Object[]{this.name, this.interpreter});
        }

        String stack = Arrays.toString(Thread.currentThread().getStackTrace());
       if (!stack.equals(stack.replaceAll("getClassManager.*getClassManager", "")) && Interpreter.TRACE) {
          System.err.println("Recursive cycle !");
        }
      }

      this.classManager = BshClassManager.createClassManager(this.interpreter);
     if (Interpreter.TRACE) {
        System.err.println("No class manager namespace:" + Debug.ToString(this));
      }

      return this.classManager;
    }
  }

  public void setClassManager(BshClassManager classManager) {
    throw new UnsupportedOperationException("ClassManager is reaad-only");
  }

  public void prune() {
   if (Interpreter.DEBUG) {
      System.err.printf("PRUNE called on %s!!! \n", new Object[]{this});
    }

  }

  public void setParent(BshBinding parent) {
    BshBinding oldParent = this.parent;
   if (Interpreter.DEBUG) {
      System.err.printf("NameSpace \"%s\"@%x: Parent is being changed: \"%s\"@%x -> \"%s\"@%x\n (allowReparent = %s)\n", new Object[]{this.getName(), Integer.valueOf(System.identityHashCode(this)), oldParent != null?oldParent.getName():"<null>", Integer.valueOf(oldParent != null?System.identityHashCode(oldParent):0), parent != null?parent.getName():"<null>", Integer.valueOf(parent != null?System.identityHashCode(parent):0), Boolean.valueOf(allowReparent)});
    }

   if (!allowReparent) {
      throw new UnsupportedOperationException(format("Parent is reaad-only (allowReparent == %s)", new Object[]{Boolean.valueOf(allowReparent)}));
    } else {
      this.lastParent.push(parent);
      this.parent = parent;
    }
  }

  public Object getVariable(String name) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.getVariable(String name=\"%s\") \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, this});
    }

    Object ret = this.getVariable(name, true);
   if (Interpreter.DEBUG) {
      System.err.printf("    >>>  %s{%s}.getVariable(String name=\"%s\") \n    on %s \n    returning:  -->  %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, this, Debug.ToString(ret)});
    }

    return ret;
  }

  public Object getVariable(String name, boolean recurse) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.getVariable(String name=\"%s\", boolean recurse = %s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, Boolean.valueOf(recurse), this});
    }

    Variable var = this.getVariableImpl(name, recurse);
    Object ret = this.unwrapVariable(var);
   if (Interpreter.DEBUG) {
      System.err.printf("    >>>  %s{%s}.getVariable(String name=\"%s\", boolean recurse = %s) \n    on %s \n    returning:  -->  %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, Boolean.valueOf(recurse), this, Debug.ToString(ret)});
    }

    return ret;
  }

  public Variable getVariableImpl(String name, boolean recurse) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.getVariableImpl(String name=\"%s\", boolean recurse = %s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, Boolean.valueOf(recurse), this});
    }

    NameSpace ns = this;
    Variable v = (Variable)this.variables.get(name);
   if (v == null && recurse) {
      while(v == null && (ns = (NameSpace)ns.parent) != null) {
        v = (Variable)ns.variables.get(name);
      }
    }

    Variable var = v;
   if (v == null && this.isClass()) {
      var = this.getImportedVar(name);
    }

   if (var == null) {
      var = (Variable)this.variables.get(name);
    }

   if (var == null && !this.isClass()) {
      var = this.getImportedVar(name);
    }

   if (Interpreter.DEBUG) {
      System.err.printf("    >>>  %s{%s}.getVariableImpl(String name=\"%s\", boolean recurse = %s) \n    on %s \n    returning:  -->  %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, Boolean.valueOf(recurse), this, Debug.ToString(var)});
    }

    return var;
  }

  public Pair<Variable, BshBinding> findVariable(String name) {
    return findVariable(name, this);
  }

  public static Pair<Variable, BshBinding> findVariable(String name, BshBinding ns) {
    Variable var = null;

    try {
     if (ns.isClass()) {
        var = ns.getImportedVar(name);
       if (var != null) {
          return Pair.of(var, ns);
        }
      }

     if (((NameSpace)ns).variables.containsKey(name)) {
        var = (Variable)((NameSpace)ns).variables.get(name);
        return Pair.of(var, ns);
      } else {
        var = ns.getImportedVar(name);
        BshBinding e;
        return var != null?Pair.of(var, ns):((e = ns.getParent()) == null?null:findVariable(name, e));
      }
    } catch (UtilEvalError var4) {
      throw new RuntimeException("findVariable: " + name + ", " + ns, var4);
    }
  }

  public Variable[] getDeclaredVariables() {
    return (Variable[]) this.variables.values().toArray(new Variable[0]);
  }

  public Object unwrapVariable(Variable var) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.unwrapVariable(Variable var=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), Debug.ToString(var), this});
    }

    Object ret = var == null?Primitive.VOID:var.getValue();
   if (Interpreter.DEBUG) {
      System.err.printf("  >>>   %s{%s}.unwrapVariable(Variable var=%s) \n    on %s \n    returning:  -->  %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), Debug.ToString(var), this, Debug.ToString(ret)});
    }

    return ret;
  }

  public void setTypedVariable(String name, Class<?> type, Object value, boolean isFinal) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.setTypedVariable(String name=\"%s\", Class<?> type=%s, Object value=%s, boolean isFinal=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, type, Debug.ToString(value), Boolean.valueOf(isFinal), this});
    }

    Modifiers modifiers = new Modifiers();
   if (isFinal) {
      modifiers.addModifier(1, "final");
    }

    this.setTypedVariable(name, type, value, modifiers);
  }

  public void setTypedVariable(String name, Class<?> type, Object value, Modifiers modifiers) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      System.err.printf(" %s{%s}.setTypedVariable(String name=\"%s\", Class<?> type=%s, Object value=%s, Modifiers modifiers=%s) \n    on %s\n", new Object[]{this.getClass().getSimpleName(), this.getSimpleName(), name, type, value, Debug.ToString(modifiers), this});
    }

    Variable v = this.getVariableImpl(name, false);
   if (EMERGENCY) {
     if (Interpreter.DEBUG) {
        System.err.println("variables.put [EMERGENCY]");
      }

      this.variables.put(name, new Variable(name, type, value, modifiers));
    } else if (v != null && v.getType() != null) {
     if (v.getType() != type) {
        try {
          this.unsetVariable(name);
        } catch (Throwable var7) {
          var7.printStackTrace();
        }

        v.setValue(value, Variable.DECLARATION);
      } else {
        v.setValue(value, Variable.ASSIGNMENT);
      }
    } else {
     if (Interpreter.DEBUG) {
        System.err.println("variables.put");
      }

      this.variables.put(name, new Variable(name, type, value, modifiers));
    }
  }

  public void setMethod(BshMethod method) throws UtilEvalError {
    setMethod0(method);
  }
  
  public void setMethod0(BshMethod method) throws UtilEvalError {
    String name = method.getName();
    List<BshMethod> list = (List)this.methods.get(name);
   if (list == null) {
      this.methods.put(
        name, (list = new ArrayList(Collections.singletonList(method)))
      );
    } else {
     if (!(list instanceof ArrayList)) {
        list = new ArrayList((Collection)list);
        this.methods.put(name, list);
      }
    }
    //if (this.id == null) {        
        Class[] methodParamTypes = method.getParameterTypes();
        String methodName = method.getName();
        ListIterator it = ((List)list).listIterator();
        boolean found = false;
        while(it.hasNext()) {
          BshMethod md = (BshMethod)it.next();
          Class[] ptypes = md.getParameterTypes();
          String mname = md.getName();
         if (methodName.equals(mname) 
          && Arrays.equals(methodParamTypes, ptypes)) {
            
           if (Interpreter.DEBUG) {
              System.err.printf(
                "Replacing method:\n  %s\nwith:\n  %s\n",
                md, method
              );
            }
            it.set(method);
            found = true;
            break;
          }
        }
        
        if (! found) list.add(method);
      //}
  }
  
  public BshMethod getMethod(String name, Class<?>[] sig) throws UtilEvalError {
    return this.getMethod(name, sig, false);
  }

  public BshMethod getMethod(String name, Class<?>[] sig, boolean declaredOnly) throws UtilEvalError {
    BshMethod method = null;
   if (method == null) {
      Object origList = (List)this.methods.get(name);
     if (origList == null) {
        Object list = this;

        do {
          list = ((BshBinding)list).getParent();
         if (list == null) {
            break;
          }

          origList = (List)((BshBinding)list).getMethodsByName().get(name);
        } while(origList == null);
      }

     if (origList == null) {
        origList = new ArrayList();
      }

      ArrayList var13 = new ArrayList((Collection)origList);
      Iterator it = var13.iterator();

      label67:
      while(true) {
        BshMethod candidates;
        BSHMethodDeclaration decl;
        Class[] ptypes;
        String mname;
        do {
          do {
           if (!it.hasNext()) {
             if (var13 != null) {
                Class[][] var14 = new Class[var13.size()][];

                int var15;
                for(var15 = 0; var15 < var14.length; ++var15) {
                  var14[var15] = ((BshMethod)var13.get(var15)).getParameterTypes();
                }

                var15 = Reflect.findMostSpecificSignature(sig, var14);
               if (var15 != -1) {
                  method = (BshMethod)var13.get(var15);
                }
              }
              break label67;
            }

            candidates = (BshMethod)it.next();
          } while(this.id == null);

          BSHBlock match = this.id.getBlock();
          decl = (BSHMethodDeclaration)Array.get(CollectionUtil.toArray(SimpleNode.DEFAULT.findChild(match.jjtGetParent(), BSHMethodDeclaration.class, 2)), 0);
          decl.insureNodesParsed();
          ptypes = decl.paramsNode.paramTypes;
          mname = decl.name;
        } while(Arrays.equals(ptypes, candidates.getParameterTypes()) && candidates.getName().equals(mname) && candidates.decl == decl);

        it.remove();
      }
    }

   if (method == null && !this.isClass() && declaredOnly) {
      method = this.getImportedMethod(name, sig);
    }

    return declaredOnly && method == null && this.parent != null?this.parent.getMethod(name, sig):method;
  }
  
  public static Set<String> operClasses;
  
  public void importClass(String name) { //;
    if (Interpreter.DEBUG) {
      Interpreter.debug(format("Importing class: [%s]", new Object[]{name}));
    }
    String simpleName = Name.suffix(name, 1);
    boolean changed = false;
      this.importedClasses /*;*/ .put(simpleName, name);
      changed = true;
    if (operClasses == null) {
      getOperatorClassNameWhitelist();
    }
    if (operClasses.contains(name) || name.startsWith("bsh.operator")) {
      this.getExtendedMethodProvider().cacheExtendedMethods(name);
      changed = true;
    }
    if (changed) this.nameSpaceChanged();
  }

  public void importPackage(String name) {
    if (Interpreter.DEBUG) {
      Interpreter.debug(format("Importing package: [%s]", new Object[]{name}));
    }
    this.importedPackages.remove(name);
    this.importedPackages.add(name);
    this.nameSpaceChanged();
  }
  
  
  static File importedCommandBase;
  
  public File importCommands(String name) {
   
   if (Interpreter.DEBUG) {
      Interpreter.debug(format("Importing commands: [%s]", new Object[]{name}));
    }

   final File bshHome = (importedCommandBase != null) //;
    ? importedCommandBase
    : (importedCommandBase = new File(
        (getenv().containsKey("HOME"))
          ? getenv("HOME") 
          : System.getProperty(
              "bsh.home",
              "/data/media/0"
            )
      ));
    
    
   
    name = name.replace('.', '/');
   if (!name.startsWith("/")) {
      name = "/" + name;
    }

   if (name.length() > 1 && name.endsWith("/")) {
      name = name.substring(0, name.length() - 1);
    }

    this.importedCommands.remove(name);
    this.importedCommands.add(name);
    this.nameSpaceChanged();
    return bshHome;
  }
  
  public Collection<String> getCommands() {
    return Arrays.asList((String[]) this.importedCommands.toArray(new String[0]));
  }
  

  
  public Object getCommand(String name, Class<?>[] argTypes,
    final Interpreter interpreter)
    throws UtilEvalError
  {
   if (Interpreter.DEBUG) {
      Interpreter.debug("getCommand: " + name);
    }
    
   if (name.indexOf("org.d6r.") == -1 && name.indexOf(46) == -1) {
      String bcm = "org.d6r.".concat(name);
     if (Capabilities.classExists(bcm)) {
       if (Interpreter.DEBUG) {
          Interpreter.debug("Going directly to compiled class [A]: " + bcm);
        }

        try {
          return Class.forName(
            bcm, false, NameSpace.class.getClassLoader()
          );
        } catch (Throwable var39) {
          var39.printStackTrace();
        }
      }
    }
    
    BshClassManager bcm1 = interpreter.getClassManager();
    Iterator var6 = this.importedCommands.iterator();
    
    while(var6.hasNext()) {
      String path = (String)var6.next();
      String className = path.equals("/")?name:path.substring(1).replace('/', '.').concat(".").concat(name);
     if (Interpreter.DEBUG) {
        System.err.printf(">> cmd className: \'%s\'\n", new Object[]{className});
      }

     if (Capabilities.classExists(className)) {
       if (Interpreter.DEBUG) {
          System.err.printf(">> exists\n", new Object[0]);
          Interpreter.debug("Going directly to compiled class: " + className);
       }

       Class scriptPath = bcm1.classForName(className);
       if (Interpreter.DEBUG) {
          System.err.printf(">> Class<?> cmdCls := <%s>\n", new Object[]{scriptPath != null?scriptPath.toString():"<NULL>"});
          Interpreter.debug("classForName returned: " + (scriptPath != null?scriptPath.toString():"<NULL>"));
       }

       if (scriptPath != null) {
          return scriptPath;
        }
      }
      
      
      final String scriptPath = String.format("bsh/commands/%s.bsh", name);
      
      
      
      
      final File bshHome = (importedCommandBase != null) //;
        ? importedCommandBase
        : interpreter.getNameSpace().importCommands("bsh.commands");
      
      final File scriptFile = new File(bshHome, scriptPath);
      
      if (Interpreter.DEBUG) {
        Interpreter.debug("searching for script @ " + scriptFile);
      }
      
      if (scriptFile.exists()) {
        try (final InputStream is = new FileInputStream(scriptFile)) {
          return loadScriptedCommand(
            is, name, argTypes, scriptFile.getAbsolutePath(), interpreter
          );
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      
      try {
        final URL clsRes = getClassResource(Interpreter.class);
        if (Interpreter.RES_DEBUG) Interpreter.resDebug(clsRes);
        if (clsRes != null) {
          final URLConnection conn = clsRes.openConnection();
          if (Interpreter.RES_DEBUG) Interpreter.resDebug(conn);
          if (conn instanceof JarURLConnection) {
            final JarURLConnection jarUrlConn = (JarURLConnection) conn;
            final ZipFile zipFile = jarUrlConn.getJarFile();
            final ZipEntry entry = zipFile.getEntry(scriptPath);
            if (entry != null) {
              try (final InputStream is = zipFile.getInputStream(entry)) {
                if (Interpreter.RES_DEBUG) Interpreter.resDebug(is);
                return loadScriptedCommand(
                  is, name, argTypes, format(
                    "jar:file://%s!/%s", scriptPath, zipFile.getName()
                  ), interpreter
                );
              }
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }
    }
    
    if (Interpreter.DEBUG) {
      Interpreter.debug(format(
        "Command resolution for `%s`, with argument types %s: "
        + "Falling through to parent; didn't find it in this namespace: %s",
        name, Arrays.toString(argTypes), getName()
      ));
    }

    if (this.parent != null) {
      return this.parent.getCommand(name, argTypes, interpreter);
    } else {
      if (Interpreter.DEBUG) {
        Interpreter.debug(format(
          "Command resolution for `%s`, returning NULL due to having no "
          + "parent namespace to fall back to for search!", name
        ));
      }
      return null;
    }
  }
  
  public BshMethod getImportedMethod(String name, Class<?>[] sig)
    throws UtilEvalError
  {
    BshClassManager bcm = this.getClassManager();
    Iterator var6 = this.importedObjects.iterator();

    Member method;
    while(var6.hasNext()) {
      Object clas = var6.next();
     if ((method = Reflect.resolveJavaMethod(bcm, Factory.typeof(clas), name, sig, false)) instanceof Method) {
        return BshMethod.valueOf((Method)method, clas);
      }
    }

    var6 = this.importedStatic.iterator();

    while(var6.hasNext()) {
      Class clas1 = (Class)var6.next();
     if ((method = Reflect.resolveJavaMethod(bcm, clas1, name, sig, true)) instanceof Method) {
        return BshMethod.valueOf((Method)method, (Object)null);
      }
    }

    return null;
  }

  public Variable getImportedVar(String name) throws UtilEvalError {
    Iterator var3 = this.importedObjects.iterator();

    while(var3.hasNext()) {
      Object clas = var3.next();
      Class field = clas.getClass();
      Field field1 = Reflect.resolveJavaField(field, name, false);

      try {
        field1.setAccessible(true);
      } catch (Exception var8) {
        ;
      }

     if (field1 != null) {
        return new Variable(name, field1.getType(), new LHS(clas, field1));
      }
    }

    var3 = this.importedStatic.iterator();

    while(var3.hasNext()) {
      Class clas1 = (Class)var3.next();
      Field field2 = Reflect.resolveJavaField(clas1, name, true);

      try {
        field2.setAccessible(true);
      } catch (Exception var7) {
        ;
      }

     if (field2 != null) {
        return new Variable(name, field2.getType(), new LHS(field2));
      }
    }

    return null;
  }

  public BshMethod loadScriptedCommand(InputStream in, String name, Class<?>[] argTypes, String resourcePath, Interpreter interpreter) throws UtilEvalError {
   if (Interpreter.DEBUG) {
      Interpreter.debug(format("loadScriptedCommand(InputStream in = %s, String name = %s, Class<?>[] argTypes = %s, String resourcePath = %s, Interpreter interpreter = %s)", new Object[]{in != null?in.toString():"<NULL>", name != null?name:"<NULL>", argTypes != null?Arrays.toString(argTypes):"<NULL>", resourcePath != null?resourcePath:"<NULL>", interpreter != null?interpreter.toString():"<NULL>"}));
    }

    try {
      interpreter.eval(new InputStreamReader(in), this, resourcePath);
    } catch (EvalError var7) {
      var7.printStackTrace();
      Interpreter.debug(var7.toString());
      throw new UtilEvalError("Error loading command `" + name + "`:\n  " + var7.getMessage(), var7);
    }

    BshMethod meth = this.getMethod(name, argTypes);
    return meth;
  }

  public void cacheClass(String name, Class<?> c) {
    classCache.put(name, c);
  }

  public Class<?> getClass(String name) throws UtilEvalError {
    Class c = this.getClassImpl(name);
    return c != null?c:(this.parent != null?this.parent.getClass(name):null);
  }

  public Class<?> getClassImpl(String name) throws UtilEvalError {
    Class c = (Class)classCache.get(name);
   if (c != null) {
      return c;
    } else {
      boolean unqualifiedName = !Name.isCompound(name);
     if (unqualifiedName) {
       if (c == null) {
          c = this.getImportedClassImpl(name);
        }

       if (c != null) {
          this.cacheClass(name, c);
          return c;
        }
      }

      c = this.classForName(name);
     if (c != null) {
       if (unqualifiedName) {
          this.cacheClass(name, c);
        }

        return c;
      } else {
       if (Interpreter.DEBUG) {
          Interpreter.debug(format("getClass(\"%s\") not found in %s", new Object[]{name, this.toString()}));
        }

        return null;
      }
    }
  }

  public Class<?> getImportedClassImpl(String name) throws UtilEvalError {
    String fullname = (String)this.importedClasses.get(name);
   if (fullname != null) {
      Class bcm2 = this.classForName(fullname);
     if (bcm2 == null) {
       if (Name.isCompound(fullname)) {
          try {
            bcm2 = this.getNameResolver(fullname).toClass();
          } catch (ClassNotFoundException var7) {
            var7.printStackTrace();
          }
        } else if (Interpreter.DEBUG) {
          Interpreter.debug("imported unpackaged name not found:" + fullname);
        }

       if (bcm2 != null) {
          this.getClassManager().cacheClassInfo(fullname, bcm2);
          return bcm2;
        } else {
          return null;
        }
      } else {
        return bcm2;
      }
    } else {
      Iterator s = this.importedPackages.iterator();

      while(s.hasNext()) {
        String bcm = (String)s.next();
        String s1 = bcm.concat(".").concat(name);
        Class c = this.classForName(s1);
       if (c != null) {
          return c;
        }
      }

      BshClassManager bcm1 = this.getClassManager();
     if (bcm1.hasSuperImport()) {
        String s2 = bcm1.getClassNameByUnqName(name);
       if (s2 != null) {
          return this.classForName(s2);
        }
      }

      return null;
    }
  }

  public Class<?> classForName(String name) {
    try {
      return this.getClassManager().classForName(name);
    } catch (Throwable var5) {
     if (Interpreter.TRACE) {
        System.err.printf("NameSpace{%s}.classForName(\"%s\") got %s: %s\n", new Object[]{Debug.ToString(this), var5.getClass().getSimpleName(), var5.getMessage()});
      }

      try {
        return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
      } catch (Throwable var4) {
       if (Interpreter.TRACE) {
          System.err.printf("Class.forName(\"%s\"): %s: %s\n", new Object[]{name, var4.getClass().getSimpleName(), var4.getMessage()});
        }

        return null;
      }
    }
  }

  public String[] getAllNames() {
    ArrayList allNames = new ArrayList(this.names.keySet());
    this.getAllNamesAux(allNames);
    return (String[]) allNames.toArray(new String[0]);
  }

  public void getAllNamesAux(Collection<? super String> list) {
    list.addAll(this.variables.keySet());
    list.addAll(this.methods.keySet());
   if (this.parent != null) {
      this.parent.getAllNamesAux(list);
    }

  }

  public void addNameSourceListener(NameSource.Listener listener) {
    this.nameSourceListeners.add(listener);
  }

  public void doSuperImport() throws UtilEvalError {
    this.getClassManager().doSuperImport();
  }

  public String toString() {
    return "NameSpace: " + (this.name == null?super.toString():this.name + " (" + super.toString() + ")") + (this.isClass()?" (isClass()) ":"") + (this.isMethod()?" (method) ":"") + (this.classStatic != null?" (class static) ":"") + (this.classInstance != null?" (class instance) ":"");
  }
  
  public void writeObject(ObjectOutputStream s) throws IOException {
    this.names.clear();
    s.defaultWriteObject();
  }

  public Object invokeMethod(String methodName, Object[] args, Interpreter interpreter) throws EvalError {
    return this.invokeMethod(methodName, args, interpreter, (CallStack)null, (SimpleNode)null);
  }

  public Object invokeMethod(String methodName, Object[] args, Interpreter interpreter, CallStack callstack, SimpleNode callerInfo) throws EvalError {
    return this.getThis(interpreter).invokeMethod(methodName, args, interpreter, callstack, callerInfo, false);
  }

  public void classLoaderChanged() {
    this.nameSpaceChanged();
  }

  public void nameSpaceChanged() {
    this.names.clear();
  }
  
  static final Map<String, URL> classResources = new HashMap<String, URL>();
  
  public static URL getClassResource(final Class<?> cls) {
    final String className = cls.getName();
    if (classResources.containsKey(className)) {
      return classResources.get(className);
    }
    final StringBuilder sb =
      new StringBuilder(className.length()+6).append(className);
    for (int pos; (pos = sb.indexOf(".")) != -1;) sb.replace(pos, pos+1, "/");
    final String resName = sb.append(".class").toString();
    final URL res = (
      (cls.getClassLoader() != null)
        ? cls.getClassLoader()
        : ClassLoader.getSystemClassLoader().getParent()
    ).getResource(resName);
    classResources.put(className, res);
    return res;
  }
  
  public static List<String> loadImportsList(String name) {
    URL res = getClassResource(Interpreter.class);
    if (System.getProperty("loadImports.debug") != null) System.err.printf(
      "getClassResource(Interpreter.class) -> (%s) %s\n",
      Factory.typeof(res).getName(), res
    );
    URLConnection conn = null;
    JarFile jar = null;
    Object err = null;
   if (res != null) {
      try {
        conn = res.openConnection();
       if (conn instanceof JarURLConnection) {
          jar = ((JarURLConnection)conn).getJarFile();
        }
      } catch (IOException var14) {
        err = Reflector.getRootCause(var14);
      }
    }

   if (jar != null) {
      label130: {
        ZipEntry ze = jar.getEntry(name);
       if (ze == null) {
          ZipEntry[] is = (ZipEntry[]) CollectionUtil.toArray(jar.entries());
          ZipEntry[] var10 = is;
          int var9 = is.length;
          int var8 = 0;

          while(true) {
           if (var8 >= var9) {
              err = new Error(format("[WARN] Configuration missing: \'%s\' in \'%s\'\n", new Object[]{name, jar.getName()}));
              break label130;
            }

            ZipEntry ex = var10[var8];
           if (name.equals(ex.getName())) {
              ze = ex;
              break;
            }

            ++var8;
          }
        }

        InputStream var18 = null;

        List var19;
        try {
          var18 = jar.getInputStream(ze);
          var19 = IOUtils.readLines(var18, Charset.defaultCharset());
        } catch (IOException var15) {
          Throwable var17 = Reflector.getRootCause(var15);
          err = new Error(format("[WARN] Configuration for \'%s\' from \'%s\': %s: %s\n", new Object[]{name, jar.getName(), var17.getClass().getSimpleName(), var17.getMessage()}));
          break label130;
        } finally {
         if (var18 != null) {
            IOUtils.closeQuietly(var18);
          }

        }

        return var19;
      }
    }

    System.err.printf("[WARN] Configuration for \'%s\' expected in main `.jar\'\n", new Object[]{name});
   if (err != null) {
      ((Throwable)err).printStackTrace();
    }

    return Collections.emptyList();
  }

  static synchronized List<String> getDefaultClassImports() {
   if (defaultClassImports == null) {
      defaultClassImports = loadImportsList("class_imports.list");
    }

    return defaultClassImports;
  }

  static synchronized List<String> getDefaultPackageImports() {
   if (defaultPackageImports == null) {
      defaultPackageImports = loadImportsList("package_imports.list");
    }

    return defaultPackageImports;
  }
  
  
  static synchronized Set<String> getOperatorClassNameWhitelist() {
    if (operClasses == null) {
      ((Set<String>) (operClasses = new THashSet()))
        .addAll((Collection<? extends String>) loadImportsList("oper_classes.list"));
    }
    return operClasses;
  }
  
  public void loadDefaultImports() {
    
   if (this.importedClasses.size() <= 10) {
     if (Interpreter.DEBUG) {
        System.err.println("loadDefaultImports() called.");
        System.err.println(Arrays.toString((new Error()).getStackTrace()));
      }

      Iterator dex = getDefaultClassImports().iterator();
      
      String names;
      while(dex.hasNext()) {
        names = (String)dex.next();
       if (names.length() > 7) {
          this.importClass(names);
        }
      }
      
      dex = getDefaultPackageImports().iterator();

      while(dex.hasNext()) {
        names = (String)dex.next();
       if (names.length() > 7) {
          this.importPackage(names);
        }
      }
      
      
     if (Interpreter.DEBUG) {
        System.err.println("end of importX... statements");
      }
      
      /*
      if (System.getProperty("java.vendor").indexOf("ndroid") != -1) {
        try {
          Method name = Class.class.getDeclaredMethod("getDex", new Class[0]);
          name.setAccessible(true);
          Dex dex1 = (Dex) name.invoke(Interpreter.class, new Object[0]);
          names1 = Arrays.asList(
            (String[]) StringCollectionUtil.toStringFilter(
              (new DexUtil(dex1)).getClassNames(),
              new Object[]{"^(?:bsh\\.|org\\.d6r\\.)[^.$]*$"}
            )
          );
      } catch (NoSuchMethodException var7) {
        if (Interpreter.TRACE) {
          System.err.println(var7.toString());
        }

        names1 = searchJarClassNames(
          new String[]{
            "^(?:bsh\\.(?:classpath\\.)?|org\\.d6r\\.)[^/.$]+\\.class$"
          }
        );
      } catch (Throwable var8) {
        throw new RuntimeException(var8);
      }
      
      for (final String name1: names1) {
        if (name1.length() == 0) continue;

        try {
          this.importClass(name1);
        } catch (Throwable var6) {
          System.out.printf(
            "%s: [%s]: %s\n",
            var6.getClass().getSimpleName(),
            name1, var6.getMessage() != null?var6.getMessage():""
          );
        }
      }
      */
    }
    
  }
  
  
  static final String CLASS_SUFFIX = ".class";

  public static Collection<String> searchJarClassNames(String[] regexes) {
    final List<String> matches = new ArrayList<String>();
    String path = "[unknown path]";
    JarFile zf = null;
    try {
      final URL res = getClassResource(Interpreter.class);
      path = org.d6r.PathInfo.getPathInfo(res).path;      
      final URLConnection _conn = res.openConnection();
      if (! (_conn instanceof JarURLConnection)) {
        throw new IllegalStateException(String.format(
          "Interpreter.class is pulled from a non-archive file: %s",
          _conn
        ));
      }
      // TODO - API / Utility method for this         
      //   (as a companion to (for ex.) NameSpace#getClassResource() ..)
      final JarURLConnection conn
         = (JarURLConnection) res.openConnection();
      zf = conn.getJarFile();
      
      
        
      final StringBuilder sb = new StringBuilder(72);
      final List<? extends ZipEntry> matchingEntries
        = filter(asIterable(zf.entries()), Pattern.compile(regexes[0]));
      for (final ZipEntry ze: matchingEntries) {
        if (ze.isDirectory()) continue;
        if (ze.getSize() <= 10L) continue;            
        final String name = ze.getName();
        
        sb.setLength(0);
         final int nameLen = name.length();
        if (nameLen < 6) continue;
        if (!CLASS_SUFFIX.equals(
            name.subSequence(nameLen-6,nameLen))) continue;
            
        int slashpos, lastpos = -1;
        do {
          slashpos = name.indexOf('/', lastpos+1);
          final CharSequence part = name.subSequence(
            lastpos+1,
            (slashpos != -1)? slashpos: name.length()-6
          );
          if (lastpos != -1) sb.append('.');
          sb.append(part);
          
          //if (true) System.out.printf(
          //"[%d, %d]: \"%s\"\n", lastpos+1, slashpos, sb
          //);
          lastpos = slashpos;
        } while (lastpos != -1);
        matches.add(sb.toString());
      }
      return matches;
    } catch (IOException ioe) {
      throw Util.sneakyThrow(ioe);
    }
  }


  public Name getNameResolver(String ambigname) {
    Name name = (Name)this.names.get(ambigname);
   if (name == null) {
      name = new Name(this, ambigname);
      this.names.put(ambigname, name);
    }

    return name;
  }

  public int getInvocationLine() {
    SimpleNode node = this.getNode();
   if (node != null) {
      int lineno = node.getLineNumber();
      return lineno > 0?lineno:1;
    } else {
      return 1;
    }
  }

  public String getInvocationText() {
    SimpleNode node = this.getNode();
    return node != null?node.getText():"<invoked from Java code>";
  }

  public static Class<?> identifierToClass(ClassIdentifier ci) {
    return ci.getTargetClass();
  }

  public void clear() {
    this.variables.clear();
    this.methods.clear();
    this.importedObjects.clear();
    this.names.clear();
  }

  public void importObject(Object obj) {
    this.importedObjects.remove(obj);
    this.importedObjects.add(obj);
    this.nameSpaceChanged();
  }

  public void importStatic(Class<?> clas) {
    this.importedStatic.remove(clas);
    this.importedStatic.add(clas);
    this.nameSpaceChanged();
  }

  public void setPackage(String packageName) {
    this.packageName = packageName;
  }

  public String getPackage() {
    return this.packageName != null?this.packageName:(this.parent != null?this.parent.getPackage():null);
  }

  public BshBinding copy() {
    try {
      NameSpace e = (NameSpace)this.clone();
      e.thisReference = null;
      e.names.putAll(this.names);
      e.methods.putAll(this.methods);
      e.variables.putAll(this.variables);
      e.importedClasses.putAll(this.importedClasses);
      e.importedStatic.addAll(this.importedStatic);
      e.importedObjects.addAll(this.importedObjects);
      e.importedPackages.addAll(this.importedPackages);
      e.importedCommands.addAll(this.importedCommands);
      return e;
    } catch (CloneNotSupportedException var2) {
      throw new IllegalStateException(var2);
    }
  }

  public <K, V> Map<K, V> clone(Map<K, V> map) {
    return map == null?null:newMap(map);
  }

  public <T> List<T> clone(List<T> list) {
    return list == null?null:new ArrayList(list);
  }

  public synchronized OperatorProvider getExtendedMethodProvider() {
   if (extendedMethods == null) {
      extendedMethods = new OperatorProvider();
    }

    return extendedMethods;
  }

  public static class BindingMap<K, V> extends RealArrayMap<K, V> {
    public BindingMap() {
    }

    public BindingMap(Map<K, V> map) {
      super(map);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();

      try {
        Iterator it = this.entrySet().iterator();

        while(it.hasNext()) {
          Entry entry = (Entry)it.next();
          Object value;
         if ((value = entry.getValue()) instanceof Variable) {
            Variable v = (Variable)value;
            String name = v.getName();
           if (name.length() + sb.length() > 255) {
              sb.append(" ...");
              break;
            }

           if (sb.length() > 0) {
              sb.append(", ");
            }

            sb.append(name);
          }
        }
      } catch (Throwable var8) {
        sb.append(format("<iteration threw %s: %s>", new Object[]{var8.getClass().getSimpleName(), var8.getMessage()}));
      }

      return format("%s [%d entries]: { %s }", new Object[]{this.getClass().getSimpleName(), Integer.valueOf(this.size()), sb.toString()});
    }
  }
}