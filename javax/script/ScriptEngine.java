package javax.script;

import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
ScriptEngine is the fundamental interface whose methods must be
fully functional in every implementation of this specification.

These methods provide basic scripting functionality.  Applications written to this
simple interface are expected to work with minimal modifications in every implementation.
It includes methods that execute scripts, and ones that set and get values.

The values are key/value pairs of two types.  The first type of pairs consists of
those whose keys are reserved and defined in this specification or  by individual
implementations.  The values in the pairs with reserved keys have specified meanings.

The other type of pairs consists of those that create Java language Bindings, the values are
usually represented in scripts by the corresponding keys or by decorated forms of them.
@author Mike Grogan
@since 1.6
*/
public interface ScriptEngine {

  /**
  Reserved key for a named value that passes
  an array of positional arguments to a script.
  */
  public static final String ARGV = "javax.script.argv";

  /**
  Reserved key for a named value that is
  the name of the file being executed.
  */
  public static final String FILENAME = "javax.script.filename";

  /**
  Reserved key for a named value that is
  the name of the ScriptEngine implementation.
  */
  public static final String ENGINE = "javax.script.engine";

  /**
  Reserved key for a named value that identifies
  the version of the ScriptEngine implementation.
  */
  public static final String ENGINE_VERSION = "javax.script.engine_version";

  /**
  Reserved key for a named value that identifies
  the short name of the scripting language.  The name is used by the
  ScriptEngineManager to locate a ScriptEngine
  with a given name in the getEngineByName method.
  */
  public static final String NAME = "javax.script.name";

  /**
  Reserved key for a named value that is
  the full name of Scripting Language supported by the implementation.
  */
  public static final String LANGUAGE = "javax.script.language";

  /**
  Reserved key for the named value that identifies
  the version of the scripting language supported by the implementation.
  */
  public static final String LANGUAGE_VERSION = "javax.script.language_version";

  /**
  Causes the immediate execution of the script whose source is the String
  passed as the first argument.  The script may be reparsed or recompiled before
  execution.  State left in the engine from previous executions, including
  variable values and compiled procedures may be visible during this execution.
  @param script The script to be executed by the script engine.
  @param context A ScriptContext exposing sets of attributes in
  different scopes.  The meanings of the scopes ScriptContext.GLOBAL_SCOPE,
  and ScriptContext.ENGINE_SCOPE are defined in the specification.
  
  The ENGINE_SCOPE Bindings of the ScriptContext contains the
  bindings of scripting variables to application objects to be used during this
  script execution.
  @return The value returned from the execution of the script.
  @throws ScriptException if an error occurrs in script. ScriptEngines should create and throw
  ScriptException wrappers for checked Exceptions thrown by underlying scripting
  implementations.
  @throws NullPointerException if either argument is null.
  */
  public Object eval(String script, ScriptContext context) throws ScriptException;

  /**
  Same as eval(String, ScriptContext) where the source of the script
  is read from a Reader.
  @param reader The source of the script to be executed by the script engine.
  @param context The ScriptContext passed to the script engine.
  @return The value returned from the execution of the script.
  @throws ScriptException if an error occurrs in script.
  @throws NullPointerException if either argument is null.
  */
  public Object eval(Reader reader, ScriptContext context) throws ScriptException;

  /**
  Executes the specified script.  The default ScriptContext for the ScriptEngine
  is used.
  @param script The script language source to be executed.
  @return The value returned from the execution of the script.
  @throws ScriptException if error occurrs in script.
  @throws NullPointerException if the argument is null.
  */
  public Object eval(String script) throws ScriptException;

  /**
  Same as eval(String) except that the source of the script is
  provided as a Reader
  @param reader The source of the script.
  @return The value returned by the script.
  @throws ScriptException if an error occurrs in script.
  @throws NullPointerException if the argument is null.
  */
  public Object eval(Reader reader) throws ScriptException;

  /**
  Executes the script using the Bindings argument as the ENGINE_SCOPE
  Bindings of the ScriptEngine during the script execution.  The
  Reader, Writer and non-ENGINE_SCOPE Bindings of the
  default ScriptContext are used. The ENGINE_SCOPE
  Bindings of the ScriptEngine is not changed, and its
  mappings are unaltered by the script execution.
  @param script The source for the script.
  @param n The Bindings of attributes to be used for script execution.
  @return The value returned by the script.
  @throws ScriptException if an error occurrs in script.
  @throws NullPointerException if either argument is null.
  */
  public Object eval(String script, Bindings n) throws ScriptException;

  /**
  Same as eval(String, Bindings) except that the source of the script
  is provided as a Reader.
  @param reader The source of the script.
  @param n The Bindings of attributes.
  @return The value returned by the script.
  @throws ScriptException if an error occurrs.
  @throws NullPointerException if either argument is null.
  */
  public Object eval(Reader reader, Bindings n) throws ScriptException;

  /**
  Sets a key/value pair in the state of the ScriptEngine that may either create
  a Java Language Binding to be used in the execution of scripts or be used in some
  other way, depending on whether the key is reserved.  Must have the same effect as
  getBindings(ScriptContext.ENGINE_SCOPE).put.
  @param key The name of named value to add
  @param value The value of named value to add.
  @throws NullPointerException if key is null.
  @throws IllegalArgumentException if key is empty.
  */
  public void put(String key, Object value);

  /**
  Retrieves a value set in the state of this engine.  The value might be one
  which was set using setValue or some other value in the state
  of the ScriptEngine, depending on the implementation.  Must have the same effect
  as getBindings(ScriptContext.ENGINE_SCOPE).get
  @param key The key whose value is to be returned
  @return the value for the given key
  @throws NullPointerException if key is null.
  @throws IllegalArgumentException if key is empty.
  */
  public Object get(String key);

  /**
  Returns a scope of named values.  The possible scopes are:
  
  
  ScriptContext.GLOBAL_SCOPE - The set of named values representing global
  scope. If this ScriptEngine is created by a ScriptEngineManager,
  then the manager sets global scope bindings. This may be null if no global
  scope is associated with this ScriptEngine
  ScriptContext.ENGINE_SCOPE - The set of named values representing the state of
  this ScriptEngine.  The values are generally visible in scripts using
  the associated keys as variable names.
  Any other value of scope defined in the default ScriptContext of the ScriptEngine.
  
  
  
  The Bindings instances that are returned must be identical to those returned by the
  getBindings method of ScriptContext called with corresponding arguments on
  the default ScriptContext of the ScriptEngine.
  @param scope Either ScriptContext.ENGINE_SCOPE or ScriptContext.GLOBAL_SCOPE
  which specifies the Bindings to return.  Implementations of ScriptContext
  may define additional scopes.  If the default ScriptContext of the ScriptEngine
  defines additional scopes, any of them can be passed to get the corresponding Bindings.
  @return The Bindings with the specified scope.
  @throws IllegalArgumentException if specified scope is invalid
  */
  public Bindings getBindings(int scope);

  /**
  Sets a scope of named values to be used by scripts.  The possible scopes are:
  *
  
  ScriptContext.ENGINE_SCOPE - The specified Bindings replaces the
  engine scope of the ScriptEngine.
  
  ScriptContext.GLOBAL_SCOPE - The specified Bindings must be visible
  as the GLOBAL_SCOPE.
  
  Any other value of scope defined in the default ScriptContext of the ScriptEngine.
  *
  
  
  The method must have the same effect as calling the setBindings method of
  ScriptContext with the corresponding value of scope on the default
  ScriptContext of the ScriptEngine.
  @param bindings The Bindings for the specified scope.
  @param scope The specified scope.  Either ScriptContext.ENGINE_SCOPE,
  ScriptContext.GLOBAL_SCOPE, or any other valid value of scope.
  @throws IllegalArgumentException if the scope is invalid
  @throws NullPointerException if the bindings is null and the scope is
  ScriptContext.ENGINE_SCOPE
  */
  public void setBindings(Bindings bindings, int scope);

  /**
  Returns an uninitialized Bindings.
  @return A Bindings that can be used to replace the state of this ScriptEngine.
  */
  public Bindings createBindings();

  /**
  Returns the default ScriptContext of the ScriptEngine whose Bindings, Reader
  and Writers are used for script executions when no ScriptContext is specified.
  @return The default ScriptContext of the ScriptEngine.
  */
  public ScriptContext getContext();

  /**
  Sets the default ScriptContext of the ScriptEngine whose Bindings, Reader
  and Writers are used for script executions when no ScriptContext is specified.
  @param context A ScriptContext that will replace the default ScriptContext in
  the ScriptEngine.
  @throws NullPointerException if context is null.
  */
  public void setContext(ScriptContext context);

  /**
  Returns a ScriptEngineFactory for the class to which this ScriptEngine belongs.
  @return The ScriptEngineFactory
  */
  public ScriptEngineFactory getFactory();
}