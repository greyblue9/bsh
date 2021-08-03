package javax.script;

import java.util.List;

/**
ScriptEngineFactory is used to describe and instantiate
ScriptEngines.

Each class implementing ScriptEngine has a corresponding factory
that exposes metadata describing the engine class.
The ScriptEngineManager
uses the service provider mechanism described in the Jar File Specification to obtain
instances of all ScriptEngineFactories available in
the current ClassLoader.
@since 1.6
*/
public interface ScriptEngineFactory {

  /**
  Returns the full  name of the ScriptEngine.  For
  instance an implementation based on the Mozilla Rhino Javascript engine
  might return Rhino Mozilla Javascript Engine.
  @return The name of the engine implementation.
  */
  public String getEngineName();

  /**
  Returns the version of the ScriptEngine.
  @return The ScriptEngine implementation version.
  */
  public String getEngineVersion();

  /**
  Returns an immutable list of filename extensions, which generally identify scripts
  written in the language supported by this ScriptEngine.
  The array is used by the ScriptEngineManager to implement its
  getEngineByExtension method.
  @return The list of extensions.
  */
  public List<String> getExtensions();

  /**
  Returns an immutable list of mimetypes, associated with scripts that
  can be executed by the engine.  The list is used by the
  ScriptEngineManager class to implement its
  getEngineByMimetype method.
  @return The list of mime types.
  */
  public List<String> getMimeTypes();

  /**
  Returns an immutable list of  short names for the ScriptEngine, which may be used to
  identify the ScriptEngine by the ScriptEngineManager.
  For instance, an implementation based on the Mozilla Rhino Javascript engine might
  return list containing {&quot;javascript&quot;, &quot;rhino&quot;}.
  */
  public List<String> getNames();

  /**
  Returns the name of the scripting langauge supported by this
  ScriptEngine.
  @return The name of the supported language.
  */
  public String getLanguageName();

  /**
  Returns the version of the scripting language supported by this
  ScriptEngine.
  @return The version of the supported language.
  */
  public String getLanguageVersion();

  /**
  Returns the value of an attribute whose meaning may be implementation-specific.
  Keys for which the value is defined in all implementations are:
  
  ScriptEngine.ENGINE
  ScriptEngine.ENGINE_VERSION
  ScriptEngine.NAME
  ScriptEngine.LANGUAGE
  ScriptEngine.LANGUAGE_VERSION
  
  
  The values for these keys are the Strings returned by getEngineName,
  getEngineVersion, getName, getLanguageName and
  getLanguageVersion respectively.
  A reserved key, THREADING, whose value describes the behavior of the engine
  with respect to concurrent execution of scripts and maintenance of state is also defined.
  These values for the THREADING key are:
  
  null - The engine implementation is not thread safe, and cannot
  be used to execute scripts concurrently on multiple threads.
  &quot;MULTITHREADED&quot; - The engine implementation is internally
  thread-safe and scripts may execute concurrently although effects of script execution
  on one thread may be visible to scripts on other threads.
  &quot;THREAD-ISOLATED&quot; - The implementation satisfies the requirements
  of &quot;MULTITHREADED&quot;, and also, the engine maintains independent values
  for symbols in scripts executing on different threads.
  &quot;STATELESS&quot; - The implementation satisfies the requirements of
  &quot;THREAD-ISOLATED&quot;.  In addition, script executions do not alter the
  mappings in the Bindings which is the engine scope of the
  ScriptEngine.  In particular, the keys in the Bindings
  and their associated values are the same before and after the execution of the script.
  
  
  Implementations may define implementation-specific keys.
  @param key The name of the parameter
  @return The value for the given parameter. Returns null if no
  value is assigned to the key.
  */
  public Object getParameter(String key);

  /**
  Returns a String which can be used to invoke a method of a  Java object using the syntax
  of the supported scripting language.  For instance, an implementaton for a Javascript
  engine might be;
  
  
  
  public String getMethodCallSyntax(String obj,
  String m, String... args) {
  String ret = obj;
  ret += "." + m + "(";
  for (int i = 0; i < args.length; i++) {
  ret += args[i];
  if (i < args.length - 1) {
  ret += ",";
  }
  }
  ret += ")";
  return ret;
  }
  *
  *
  
  @param obj The name representing the object whose method is to be invoked. The
  name is the one used to create bindings using the put method of
  ScriptEngine, the put method of an ENGINE_SCOPE
  Bindings,or the setAttribute method
  of ScriptContext.  The identifier used in scripts may be a decorated form of the
  specified one.
  @param m The name of the method to invoke.
  @param args names of the arguments in the method call.
  @return The String used to invoke the method in the syntax of the scripting language.
  */
  public String getMethodCallSyntax(String obj, String m, String... args);

  /**
  Returns a String that can be used as a statement to display the specified String  using
  the syntax of the supported scripting language.  For instance, the implementaton for a Perl
  engine might be;
  
  
  public String getOutputStatement(String toDisplay) {
  return "print(" + toDisplay + ")";
  }
  
  @param toDisplay The String to be displayed by the returned statement.
  @return The string used to display the String in the syntax of the scripting language.
  */
  public String getOutputStatement(String toDisplay);

  /**
  Returns A valid scripting language executable progam with given statements.
  For instance an implementation for a PHP engine might be:
  
  
  public String getProgram(String... statements) {
  $retval = "&lt;?\n";
  int len = statements.length;
  for (int i = 0; i < len; i++) {
  $retval += statements[i] + ";\n";
  }
  $retval += "?&gt;";
  }
  
  @param statements The statements to be executed.  May be return values of
  calls to the getMethodCallSyntax and getOutputStatement methods.
  @return The Program
  */
  public String getProgram(String... statements);

  /**
  Returns an instance of the ScriptEngine associated with this
  ScriptEngineFactory. A new ScriptEngine is generally
  returned, but implementations may pool, share or reuse engines.
  @return A new ScriptEngine instance.
  */
  public ScriptEngine getScriptEngine();
}