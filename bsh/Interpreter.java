package bsh;

import bsh.BshBinding;
import bsh.BshClassManager;
import bsh.CallStack;
import bsh.CommandLineReader;
import bsh.ConsoleInterface;
import bsh.EvalError;
import bsh.Factory;
import bsh.InterpreterError;
import bsh.JJTParserState;
import bsh.JavaCharStream;
import bsh.LHS;
import bsh.Name;
import bsh.NameSpace;
import bsh.NameSpaceFactory;
import bsh.ParseException;
import bsh.Parser;
import bsh.Primitive;
import bsh.Reflect;
import bsh.ReflectError;
import bsh.ReturnControl;
import bsh.SimpleNode;
import bsh.TargetError;
import bsh.TokenMgrError;
import bsh.UtilEvalError;
import bsh.Variable;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.d6r.CollectionUtil;
import org.d6r.CollectionUtil2;
import org.d6r.Debug;
import org.d6r.LoggingProxyFactory;
import org.d6r.PosixFileInputStream;
import org.d6r.Reflector;
import org.d6r.Reflector.Util;

public class Interpreter implements ConsoleInterface, Serializable {
  public static void dbgprintln(Object... args) {}
  
  public static final String VERSION = "2.3.0_exp1";
  public static boolean DEBUG = option("debug", false);
  public static boolean TRACE = option("trace", false);
  public static boolean PROFILE = option("profile", false);
  public static Method FINALIZE = org.d6r.Reflect.findMethod(Object.class, "finalize", new Class[0]);
  public static boolean LOCALSCOPING = option("localscoping", true);
  public static boolean LOCALINTERPRETERS = option("bsh.localinterpreters", true);
  public static boolean EVALNEWCALLSTACK = option("bsh.evalnewcallstack", true);
  static Object looper;
  Throwable lastError;
  static ArrayDeque<Throwable> errors;
  static Set<String> STE_BLACKLIST;
  public static final String BUILTIN_BSH;
  public static final String BUILTIN_BSH_INTERACTIVE;
  public static final String BUILTIN_EVAL_ONLY;
  public static final String BUILTIN_BSH_SYSTEM;
  public static final String BUILTIN_SHARED;
  public static final String BUILTIN_HELP;
  public static final String BUILTIN_CWD;
  public static final Object object;
  public static transient PrintStream debug;
  static final String systemLineSeparator;
  private static Object bshSystem;
  transient BshClassManager bcm;
  public static boolean RES_DEBUG;
  private boolean strictJava;
  private boolean compatibility;
  private static boolean shutdownOnExit;
  transient Parser parser;
  public BshBinding namespace;
  transient Reader in;
  transient PrintStream out;
  transient PrintStream err;
  transient ConsoleInterface console;
  final transient Interpreter parent;
  String sourceFileInfo;
  private boolean exitOnEOF;
  protected boolean evalOnly;
  protected boolean interactive;
  private boolean showResults;
  public static boolean LOG_METHODS;
  
  static {
    if (!CollectionUtil.isJRE()) {
      try {
        Class.forName("android.os.Looper");
      } catch (final Throwable t) {
        throw new Error(String.format(
          "Wrong result returned by CollectionUtil.isJRE(): %s: %s",
          Boolean.valueOf(CollectionUtil.isJRE()),
          t
        ));
      }
      try {
        if ((looper = android.os.Looper.myLooper()) == null) {
          android.os.Looper.prepare();
          looper = android.os.Looper.myLooper();
        }
      } catch (Throwable e) {
        System.err.printf(
          "[WARN] Could not prepare looper: %s: %s\n",
          e.getClass().getSimpleName(), e.getMessage()
        );
      }
    }
    errors = new ArrayDeque();
    STE_BLACKLIST = new TreeSet(Arrays.asList(new String[]{"bsh.BSHPrimaryExpression".intern(), "bsh.BSHAllocationExpression".intern(), "bsh.Reflect".intern(), "java.lang.reflect.Method".intern(), "java.lang.reflect.Constructor".intern(), "dalvik.system.XClassLoader".intern(), "com.android.internal.util.WithFramework".intern()}));
    BUILTIN_BSH = "bsh".intern();
    BUILTIN_BSH_INTERACTIVE = "bsh.interactive".intern();
    BUILTIN_EVAL_ONLY = "bsh.evalOnly".intern();
    BUILTIN_BSH_SYSTEM = "bsh.system".intern();
    BUILTIN_SHARED = "bsh.shared".intern();
    BUILTIN_HELP = "bsh.help".intern();
    BUILTIN_CWD = "bsh.cwd".intern();
    object = new Object();
    debug = System.err;
    systemLineSeparator = "\n".intern();
    bshSystem = null;
    staticInit();
    RES_DEBUG = "true".equals(System.getProperty("resdebug"));
    shutdownOnExit = false;
  }

  public static Throwable addError(Throwable exception) {
    errors.offerLast(exception);
    return exception;
  }

  public static OutputStream emergencyGetStderrStream() {
    Object os = org.d6r.Reflect.getfldval(debug, "out");
    if(os instanceof FileOutputStream) {
      return (FileOutputStream)os;
    } else {
      try {
        return new FileOutputStream(FileDescriptor.err);
      } catch (NoSuchFieldError var6) {
        FileDescriptor fd = new FileDescriptor();
        PosixFileInputStream.setInt(fd, 2);

        try {
          return new FileOutputStream(fd);
        } catch (Throwable var5) {
          var5.printStackTrace(System.out);
          Object out = System.err;

          while(out instanceof OutputStream && !(out instanceof FileOutputStream)) {
            try {
              out = (OutputStream)org.d6r.Reflect.getfldval(System.err, "out");
            } catch (Throwable var4) {
              var4.printStackTrace(System.out);
              break;
            }
          }

          return (OutputStream)out;
        }
      }
    }
  }

  public static boolean option(String name, boolean def) {
    String prop = System.getProperty(name);
    if(prop == null) {
      return def;
    } else if(prop.isEmpty()) {
      return true;
    } else {
      char first = prop.charAt(0);
      return first == 116 || first == 84 || first == 121 || first == 89 || first == 101 || first == 69 || first == 49 || first == 43 || "on".equals(prop) || "ON".equals(prop);
    }
  }

  public static Object getBshSystemNs() {
    if(bshSystem == null) {
      Object var0 = object;
      synchronized(object) {
        if(bshSystem == null) {
          try {
            bshSystem = LoggingProxyFactory.newProxy(NameSpaceFactory.get(NameSpace.class).make(new Object[]{null, null, BUILTIN_BSH_SYSTEM}), BshBinding.class);
          } catch (Throwable var2) {
            System.err.println(var2.getClass().getName() + ": " + var2.getMessage());
            bshSystem = LoggingProxyFactory.newProxy(new HashMap(), Map.class);
          }
        }
      }
    }

    return bshSystem;
  }

  public static void resDebug(Object... resources) {
    debug.printf("[RES] Resources acquired: %s\n", new Object[]{StringUtils.join(resources, ", ")});
  }

  public static BshBinding getDefaultBinding(BshClassManager cm) {
    return Interpreter.BindingHolder.getDefault(cm);
  }
  
  static Boolean _isConsoleInput;
  public static boolean isConsoleInput() {
    if (_isConsoleInput == null) {
      final String stdinTarget = PosixFileInputStream.readlink(
        String.format("/proc/self/fd/%d", 0)
      );
      _isConsoleInput = Boolean.valueOf(
        Pattern.compile(
          "/(?:sys|dev|proc)(?:/[^/]+)*/[pt]t[mys][ptmysx]*(?:/[0-9]+)"
        ).matcher(stdinTarget).matches()
      );
    }
    return _isConsoleInput.booleanValue();
  }
  
  public Interpreter(Reader in, PrintStream out, PrintStream err, boolean interactive, BshBinding namespace, Interpreter parent, String sourceFileInfo) {
    this.lastError = null;
    this.strictJava = false;
    this.compatibility = false;
    this.exitOnEOF = false;
    this.evalOnly = false;
    this.interactive = isConsoleInput();
    this.showResults = isConsoleInput();
    if(TRACE) {
      System.err.printf("new Interpreter: <init>(reader=%s, out=%s, err=%s,interactive=%s, namespace=%s, parent=%s,sourceFileInfo=%s)\n", new Object[]{in, out, err, Boolean.valueOf(interactive), namespace, parent, sourceFileInfo});
    }

    this.parser = new Parser(in);
    long t1 = 0L;
    if(DEBUG) {
      t1 = System.currentTimeMillis();
    }

    this.in = in;
    this.out = out;
    this.err = err;
    this.interactive = interactive;
    debug = err;
    this.parent = parent;
    if(parent != null) {
      this.setStrictJava(parent.getStrictJava());
    }

    this.bcm = (parent != null)
      ? (parent.bcm != null)
          ? parent.bcm
          : BshClassManager.createClassManager(this)
      : BshClassManager.createClassManager(this);
    
    final Map<String, String> properties
       = (Map<String, String>) (Object) System.getProperties();
    
    for (final Map.Entry<String,String> ent: properties.entrySet()) {
      if (ent.getKey().startsWith("set:")) {
        String[] parts = ent.getKey().split(":");
        String name = parts[1];
        String val = ent.getValue();
        Object value = null;
        try {
          Field fld = Interpreter.class.getDeclaredField(name);
          fld.setAccessible(true);
          value = (Boolean.TYPE.isAssignableFrom(fld.getType()) ||
                   Boolean.class.isAssignableFrom(fld.getType()))
                    ? Boolean.parseBoolean(val)
                    : (String) val;
          if (value == null) continue;
          fld.set(this, value);
        } catch (ReflectiveOperationException ex) {
          new RuntimeException(String.format(
            "Error settimg interpreter field from system property '%s' " +
            "with value '%s', parsed value = %s: %s",
            ent.getKey(), ent.getValue(), value, ex
          ), ex).printStackTrace();
        }
      }
    }
    
    this.sourceFileInfo = sourceFileInfo;
    if(TRACE) {
      System.err.printf("this.bcm = %s\n", new Object[]{this.bcm});
    }

    BshBinding ns = parent != null?(parent.namespace != null?parent.namespace:getDefaultBinding(this.bcm)):getDefaultBinding(this.bcm);
      if(ns == null) {
      ns = Factory.get(NameSpace.class).make(new Object[]{this.bcm, "global"});
    }

    this.namespace = ns;
    if(TRACE) {
      System.err.printf("this.namespace = %s\n", new Object[]{ns});
    }

    try {
      ns.loadDefaultImports();
    } catch (Throwable var14) {
      System.err.printf("loadDefaultImports threw %s:\n", new Object[]{var14.getClass().getSimpleName()});
      var14.printStackTrace();
    }

    if(interactive) {
      try {
        this.loadRCFiles();
      } catch (Throwable var13) {
        System.err.printf("loadRCFiles() threw %s:\n", new Object[]{var13.getClass().getSimpleName()});
        var13.printStackTrace();
      }
    }

    if(DEBUG) {
      long t2 = System.currentTimeMillis();
      debug("Time to initialize interpreter: " + (t2 - t1));
    }

  }

  public Interpreter(Reader in, PrintStream out, PrintStream err, boolean interactive, BshBinding namespace) {
    this(in, out, err, interactive, namespace, (Interpreter)null, (String)null);
  }

  public Interpreter(Reader in, PrintStream out, PrintStream err, boolean interactive) {
    this(in, out, err, interactive, (BshBinding)null);
  }
  
  public Interpreter(Reader in, PrintStream out, PrintStream err) {
    this(in, out, err, true);
  }
  
  public Interpreter(ConsoleInterface console, BshBinding namespace) {
    this(console.getIn(), console.getOut(), console.getErr(), true, namespace);
    this.setConsole(console);
  }

  public Interpreter(ConsoleInterface console) {
    this(console, (BshBinding)null);
  }

  public Interpreter() {
    this(new StringReader(""), System.out, System.err, false, (BshBinding)null);
    this.evalOnly = true;
  }

  public void setConsole(ConsoleInterface console) {
    this.console = console;
    this.setOut(console.getOut());
    this.setErr(console.getErr());
  }
  
  public BshBinding getNameSpace() {
    if (TRACE) {
      System.err.println("getNameSpace()");
    }

    final BshBinding ns = (this.namespace != null)
        ? (BshBinding) this.namespace
        : Factory.<NameSpace>get(NameSpace.class).make(this.bcm, "global");
    
    if (TRACE) {
      System.err.printf(
        "Interpreter<%s>.getNameSpace() returning: %s\n", this, ns
      );
    }
    
    return ns;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
    

  public static Object invokeMain(Class clas, String[] args)
    throws ReflectiveOperationException, UtilEvalError
  {
    Member main = Reflect.resolveJavaMethod(
      (BshClassManager)null, clas, "main",
      new Class[]{String[].class}, true
    );
    if (main instanceof Method) {
      return org.d6r.Reflect.invoke(main, (String)null, args);
    }
    return null;
  }

  public Object run() {
    Object result = null;
    
    if(this.evalOnly) {
      throw new UnsupportedOperationException("Interpreter.run() cannot be called if evalOnly is set (e.g., no standard input stream)");
    } else {
      Object __error__ = null;
      boolean eof = false;
      Object lastRet = null;
      Object ret = null;
      boolean stop = false;

      while(!eof) {
        CallStack callstack = new CallStack(this.getNameSpace());
        SimpleNode node = null;

        try {
          if(this.interactive) {
            System.out.print(this.getBshPrompt() + "\r");
          }

          eof = this.Line();
          while (this.get_jjtree().nodeArity() > 0) {
            node = (SimpleNode)this.get_jjtree().rootNode();
            if(DEBUG) {
              try {
                node.dump(">");
              } catch (Throwable var41) {
                System.err.println(var41.toString());
              }

              try {
                System.err.println(node.toString());
              } catch (Throwable var40) {
                System.err.println(var40.toString());
              }
            }

            Primitive e = Primitive.VOID;
            boolean var52 = false;

            
            try {
              result = node.eval(callstack, this);
              
            } catch (Throwable var42) {
              Throwable e0 = var42;
              result = Primitive.VOID;
              errors.add(var42);
              var52 = true;
              if(!errors.isEmpty()) {
                Throwable exc = (Throwable)errors.peekLast();
                if(exc.getCause() == var42) {
                  e0 = exc;
                }

                Iterator it = errors.descendingIterator();
                int i = -1;
                Object _node = null;
                Object method = null;
                Object member = null;
                Object ns = null;

                while(it.hasNext()) {
                  ++i;
                  if(i >= 10) {
                    break;
                  }

                  Throwable t = (Throwable)it.next();
                  if(e0 instanceof EvalError) {
                    if(_node == null) {
                      _node = org.d6r.Reflect.getfldval(t, "node");
                      if(_node != null) {
                        ((EvalError)e0).put("node", _node);
                      }
                    }
                    
                    final Collection<Member> mbs = Errors.getMembers(t);
                    
                    if (method == null) {
                      Collection<Method> methods 
                        = CollectionUtil2.typeFilter(mbs, Method.class);
                      if (! methods.isEmpty()) {
                        method = methods.iterator().next();
                        ((EvalError) e0).put("method", method);
                      }
                    }

                    if (member == null) {
                      if (! mbs.isEmpty()) {
                        member = mbs.iterator().next();
                        ((EvalError) e0).put("member", member);
                      }
                    }

                    if (ns == null) {
                      ns = org.d6r.Reflect.getfldval(t, "ns");
                      if(ns != null) {
                        ((EvalError) e0).put("ns", ns);
                      }
                    }
                  }

                  if(t instanceof ReflectError) {
                    ReflectError re = (ReflectError)t;
                    if(org.d6r.Reflect.getfldval(re, "stackState") instanceof Map) {
                      Map data = re.getData();
                      if(e0 instanceof EvalError) {
                        ((EvalError)e0).putAll(data);
                        Iterator var21 = data.entrySet().iterator();

                        while(var21.hasNext()) {
                          Entry ent = (Entry)var21.next();
                          System.err.printf("%20s  =  %s\n", new Object[]{ent.getKey(), Debug.ToString(ent.getValue())});
                        }
                      }
                      break;
                    }
                  }
                }
              }

              __error__ = e0;
              e0.printStackTrace(debug);
              CollectionUtil.getInterpreter().setu("$_e", e0);

              try {
                this.getNameSpace().setVariable("$_e", e0, false);
              } catch (Throwable var39) {
                var39.printStackTrace();
              }
            }

            if(callstack.depth() > 5) {
              throw new InterpreterError("Callstack growing: " + callstack);
            }

            if (result instanceof ReturnControl) {
              final ReturnControl ctrl = (ReturnControl) result;
              /**
              public int kind;
              public Object value;
              public Throwable cause;
              public SimpleNode returnPoint;
              */
              int kind = ctrl.kind;
              Object value = ctrl.value;
              Throwable cause = ctrl.cause;
              SimpleNode returnPoint = ctrl.returnPoint;
              
              if (returnPoint != null) {
                System.err.printf(
                  "returnPoint = %s\n    : %s\n",
                  returnPoint, Debug.ToString(returnPoint)
                );
              }
              System.err.printf(
                "ReturnControl received from eval = %s\n    : %s\n",
                  ctrl, Debug.ToString(ctrl)
              );
              
              ArrayList<SimpleNode> oldTree
                = org.d6r.Reflect.getfldval(this.get_jjtree(), "nodes");
              ArrayList<SimpleNode> copy = (ArrayList<SimpleNode>) oldTree.clone();
              this.get_jjtree().reset();
              
              if (cause != null) {
                if (cause instanceof EvalError && callstack.depth() != 0) {
                  ((EvalError) cause).callstack = callstack.copy();
                } else if (callstack.depth() != 0) {
                  System.err.printf("\nCallStack: \n %s\n\n",callstack.toString());
                }
                throw Reflector.Util.sneakyThrow(cause);
              } else {
                result = value;
              }
              this.get_jjtree().reset();
              if (callstack.depth() > 1) {
                callstack.clear();
                callstack.push(this.getNameSpace());
              }
              break;
            }
            
            if(result != Primitive.VOID) {
              this.setu("$__", lastRet);
              this.setu("$_", result);
              lastRet = result;
              if(this.showResults) {
                this.println(result);
              }
            }
            
            break; // ("if"-style goto)
          } 
        } catch (ParseException var43) {
          this.error("Parser Error: " + var43.getMessage(DEBUG));
          if(DEBUG) {
            var43.printStackTrace();
          }

          this.setu("$node", node);
          if(!this.interactive) {
            eof = true;
          }

          this.parser.reInitInput(this.in);
          __error__ = var43;
        } catch (InterpreterError var44) {
          this.error("Internal Error: " + var44.getMessage());
          var44.printStackTrace();
          if(!this.interactive) {
            eof = true;
          }

          __error__ = var44;
        } catch (TargetError var45) {
          this.error("// Uncaught Exception: " + var45);
          var45.printStackTrace(DEBUG, this.err);
          if(!this.interactive) {
            eof = true;
          }

          __error__ = var45;
        } catch (EvalError var46) {
          if(this.interactive) {
            this.error("EvalError: " + var46.getMessage());
          } else {
            this.error("EvalError: " + var46.getRawMessage());
          }

          if(DEBUG) {
            var46.printStackTrace();
          }

          if(!this.interactive) {
            eof = true;
          }

          __error__ = var46;
        } catch (Exception var47) {
          this.error("Unknown error: " + var47);
          if(DEBUG) {
            var47.printStackTrace();
          }

          if(!this.interactive) {
            eof = true;
          }

          __error__ = var47;
        } catch (TokenMgrError var48) {
          this.error("Error parsing input: " + var48);
          this.parser.reInitTokenInput(this.in);
          if(!this.interactive) {
            eof = true;
          }

          __error__ = var48;
        } catch (Throwable var49) {
          if(DEBUG) {
            var49.printStackTrace();
          }

          EvalError evalError = new EvalError(String.format("[SEVERE] Sourced file: %s: uncaught %s: %s", new Object[]{this.sourceFileInfo, var49.getClass().getSimpleName(), var49.getMessage(), node, callstack, var49}), node, callstack, var49);
          evalError.printStackTrace();
          if(!this.interactive) {
            eof = true;
          }
        } finally {
          if(__error__ != null) {
            this.setu("$_e", __error__);
          }

          __error__ = null;
          this.get_jjtree().reset();
          if(callstack.depth() > 1) {
            callstack.clear();
            callstack.push(this.getNameSpace());
          }

        }

        if(callstack.depth() == 1) {
          if(System.err != null) {
            try {
              FINALIZE.invoke(System.err, new Object[0]);
            } catch (Throwable var38) {
              var38.printStackTrace(System.out);
            }
          }

          try {
            System.setErr(new PrintStream(emergencyGetStderrStream()));
          } catch (Throwable var37) {
            var37.printStackTrace(System.out);
          }
        }
      }

    }
    return result;
  }

  public Object source(String filename, BshBinding evalBinding) throws FileNotFoundException, IOException, EvalError {
    if(DEBUG) {
      debug("Sourcing file: " + filename);
    }

    BufferedReader sourceIn = new BufferedReader(new FileReader(filename));
    
    Object var6;
    try {
      var6 = this.eval(sourceIn, evalBinding, filename);
    } finally {
      sourceIn.close();
    }

    return var6;
  }

  public Object source(String filename) throws FileNotFoundException, IOException, EvalError {
    return this.source(filename, this.getNameSpace());
  }

  public Object eval(Reader in, BshBinding evalBinding, String sourceFileInfo) throws EvalError {
    return this.eval(in, evalBinding, sourceFileInfo, new CallStack(this.getNameSpace()));
  }

  public Object eval(Reader in, BshBinding evalBinding, String sourceFileInfo, CallStack callstack) throws EvalError {
    Object retVal = null;
    if(DEBUG) {
      debug("eval: evalBinding = " + evalBinding);
    }

    Interpreter interp;
    if(LOCALINTERPRETERS) {
      if(DEBUG) {
        debug.printf("eval(Reader,BshBinding,String,CallStack): creating new Interpreter (with parent = %s) because LOCALINTERPRETERS == true", new Object[]{this});
      }

      interp = new Interpreter(in, this.out, this.err, false, evalBinding, this, sourceFileInfo);
    } else {
      if(DEBUG) {
        debug.printf("eval(Reader,BshBinding,String,CallStack): Reusing current Interpreter (this = %s) because LOCALINTERPRETERS == false", new Object[]{this});
      }

      interp = this;
    }

    if(EVALNEWCALLSTACK) {
      if(DEBUG) {
        debug.printf("eval(Reader,BshBinding,String,CallStack): Creating new CallStack (binding = %s)because EVALNEWCALLSTACK == true", new Object[]{evalBinding});
      }

      callstack = new CallStack(evalBinding);
    }

    try {
      BshBinding outgoingException = callstack.top();
      boolean eof = false;
      Object last = this.getu("$_");

      while(!eof) {
        SimpleNode node = null;

        try {
          EvalError ee;
          try {
            eof = interp.Line();
            if(interp.get_jjtree().nodeArity() != 0) {
              node = (SimpleNode)interp.get_jjtree().rootNode();
              node.setSourceFile(sourceFileInfo);
              if(TRACE) {
                this.err.printf("// %s", new Object[]{node.getText()});
              }

              org.d6r.Reflect.setfldval(callstack.top(), "callerInfoNode", node);
              retVal = node.eval(callstack, interp);
              if(callstack.depth() > 1) {
                throw new InterpreterError("Callstack growing: " + callstack);
              }

              if(retVal instanceof ReturnControl) {
                retVal = ((ReturnControl)retVal).value;
                interp.setu("$_rctl", retVal);
                break;
              }

              if(retVal != Primitive.VOID && retVal != Primitive.NULL && retVal != null && retVal != Void.TYPE) {
                interp.setu("$__", last);
                interp.setu("$_", retVal);
                last = retVal;
                if(interp.showResults) {
                  this.println(retVal);
                }
              }
            }
          } catch (ParseException var27) {
            interp.setu("$_pex", var27);
            ee = new EvalError("Sourced file: " + sourceFileInfo + " parser Error: " + var27.getMessage(DEBUG), node, callstack);
            org.d6r.Reflect.setfldval(ee, "cause", var27);
            var27.setErrorSourceFile(sourceFileInfo);
            if(DEBUG) {
              this.error(var27.getMessage(DEBUG));
              ee.printStackTrace();
            }

            throw ee;
          } catch (InterpreterError var28) {
            ee = new EvalError("Sourced file: " + sourceFileInfo + " internal Error: " + var28.getMessage(), node, callstack, var28);
            org.d6r.Reflect.setfldval(ee, "cause", var28);
            ee.printStackTrace();
            throw ee;
          } catch (TargetError var29) {
            TargetError e1 = var29;
            if(var29.getNode() == null) {
              var29.setNode(node);
            }

            if(DEBUG) {
              var29.printStackTrace();
            }

            try {
              e1.reThrow("Sourced file: " + sourceFileInfo);
            } catch (Throwable var26) {
              org.d6r.Reflect.setfldval(var26, "cause", var29);
              throw Util.sneakyThrow(var29);
            }
          } catch (EvalError var30) {
            EvalError e = var30;
            if(var30.getNode() == null) {
              var30.setNode(node);
            }

            if(DEBUG) {
              var30.printStackTrace();
            }

            try {
              e.reThrow("Sourced file: " + sourceFileInfo);
            } catch (Throwable var25) {
              org.d6r.Reflect.setfldval(var25, "cause", var30);
              throw Util.sneakyThrow(var30);
            }
          } catch (Exception var31) {
            ee = new EvalError("Sourced file: " + sourceFileInfo + " unknown error: " + var31.getMessage(), node, callstack, var31);
            if(DEBUG) {
              ee.printStackTrace();
            }

            throw ee;
          } catch (TokenMgrError var32) {
            ee = new EvalError("Sourced file: " + sourceFileInfo + " Token Parsing Error: " + var32.getMessage(), node, callstack, var32);
            if(DEBUG) {
              ee.printStackTrace();
            }

            throw ee;
          } catch (Throwable var33) {
            ee = new EvalError(String.format("Sourced file: %s: internal Error: %s", new Object[]{sourceFileInfo, var33}), node, callstack, var33);
            org.d6r.Reflect.setfldval(ee, "cause", var33);
            if(DEBUG) {
              ee.printStackTrace();
            }

            throw ee;
          }
        } finally {
          interp.get_jjtree().reset();
          if(callstack.depth() > 1) {
            callstack.clear();
            callstack.push(evalBinding);
          }

        }
      }
    } catch (Throwable var35) {
      if(!(var35 instanceof ParseException) && !(var35.getCause() instanceof ParseException)) {
        this.setu("$__e", this.getu("$_e"));
        this.setu("$_e", var35);
      }

      throw Util.sneakyThrow(var35);
    }

    return Primitive.unwrap(retVal);
  }

  public Object eval(Reader in) throws EvalError {
    return this.eval(in, this.getNameSpace(), "eval stream");
  }

  public Object eval(String statements) throws EvalError {
    if(DEBUG) {
      debug("eval(String): " + statements);
    }

    return this.eval(statements, this.getNameSpace());
  }

  public Object eval(String statements, BshBinding evalBinding) throws EvalError {
    String s = statements.endsWith(";")?statements:statements + ";";
    return this.eval(new StringReader(s), evalBinding != null?evalBinding:this.getNameSpace(), "inline evaluation of: ``" + this.showEvalString(s) + "\'\'");
  }

  private String showEvalString(String s) {
    s = s.replace('\n', ' ');
    s = s.replace('\r', ' ');
    if(s.length() > 80) {
      s = s.substring(0, 80) + " . . . ";
    }

    return s;
  }

  public final void error(Object o) {
    if(this.console != null) {
      if(o instanceof Throwable) {
        o = Reflector.getRootCause((Throwable)o);
      }

      try {
        this.console.error("// Error: " + o + "\n");
      } catch (Throwable var4) {
        if(System.err != null) {
          System.err.printf("Interpreter.error(Object): <%s.toString() threw %s>\n", new Object[]{o.getClass().getSimpleName(), var4.getClass().getSimpleName()});
        }
      }
    } else {
      try {
        this.err.println("// Error: " + o);
        this.err.flush();
      } catch (Throwable var3) {
        if(System.err != null) {
          System.err.printf("Interpreter.error(Object): <%s.toString() threw %s>\n", new Object[]{o.getClass().getSimpleName(), var3.getClass().getSimpleName()});
        }
      }
    }

  }

  public Reader getIn() {
    return this.in;
  }

  public PrintStream getOut() {
    return this.out;
  }

  public PrintStream getErr() {
    return this.err;
  }
  
  public static boolean USE_STRING_VALUE = Boolean.valueOf(
    System.getProperty("bsh.printing.use_string_value", "false")
  ).booleanValue();
  
  public static final int DEFAULT_MAX_STRING_LENGTH = (1 << 17);
  public static int MAX_STRING_LENGTH = Integer.parseInt(
    System.getProperty(
      "bsh.printing.max_length",
      Integer.toString(DEFAULT_MAX_STRING_LENGTH, 10)
    )
  );
  static StringBuilder sb = new StringBuilder(
    MAX_STRING_LENGTH >= 0? MAX_STRING_LENGTH: DEFAULT_MAX_STRING_LENGTH
  );
  
  public final void println(Object o) {
    try {
      sb.setLength(0);
      sb.append(USE_STRING_VALUE? getStringValue(o): o);
      if (sb.length() > MAX_STRING_LENGTH) {
        sb.setLength(MAX_STRING_LENGTH-3);
        sb.append("...");
      }
    } catch (StackOverflowError soe) {
      StackTraceElement[] stes = org.d6r.Reflect.getfldval(soe, "stackTrace");
      sb.setLength(0);
      sb.append("stack overflow @ ")
        .append((stes != null && stes.length > 0)? stes[0]: "<no stack>");
    } catch (Throwable e) {
      Throwable cause
        = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
      sb.setLength(0);
      sb.append(cause).append('\n')
        .append(
          (o != null)
            ? org.d6r.ClassInfo.typeToName(o.getClass().getName())
            : "<null>"
        )
        .append('@')
        .append(Integer.toHexString(System.identityHashCode(o)));
    }
    
    if (console != null) {
      console.println(sb);
    } else {
      out.println(sb);
    }
  }
  
  public final void print(Object o) {
    String str = getStringValue(o);
    if (console != null) {
      console.print(str);
    } else {
      out.print(str);
      out.flush();
    }
  }
  
  public static String getStringValue(Object o) {
    Throwable ex = null, toStringEx = null;
    try {
      return o instanceof String? (String) o: String.valueOf(o);
    } catch (Throwable e) {
      ex = e;
      toStringEx = Reflector.getRootCause(e);
    }
        
    StringBuilder sb = new StringBuilder();
    StackTraceElement[] stes = toStringEx.getStackTrace();
    StackTraceElement culprit = null;
    for (int i=0, len=stes.length; i<len; ++i) {
      StackTraceElement ste = stes[i];
      if (ste.getMethodName().equals("toString")) {
        culprit = ste;
        break;
      }     
    }
    String exStr;
    try {
      exStr = toStringEx.getMessage();
    } catch (Throwable e2) {
      exStr = String.format(
        "<%s EXCEPTION getMessage() threw %s!>",
        toStringEx.getClass().getName(),
        e2.getClass().getName()          
      );
    }
    if (exStr == null) exStr = "";
      
    if (culprit != null) {
      sb.append("\u001b[1;33m[WARN]\u001b[0m  ")
        .append("toString() call on ")
        .append(o.getClass().getName())
        .append("instance failed:\n  ")
        .append("\u001b[1;31m")
        .append(culprit.getClassName())
        .append(".")
        .append(culprit.getMethodName())
        .append("\u001b[0m() threw ")
        .append(toStringEx.getClass().getSimpleName())
        .append(
          exStr.length()>0 && !exStr.equals(toStringEx.getClass().getName())
            ? ": ".concat(exStr)
            : ""
        )
        .append(" at line ")
        .append(Integer.valueOf(culprit.getLineNumber()))
        .append('\n');
    } else {
      sb.append("\u001b[1;33m[WARN]\u001b[0m  ")
        .append("toString() call on ")
        .append(o.getClass().getName())
        .append("instance threw ")
        .append(toStringEx.getClass().getSimpleName())
        .append(
          exStr.length()>0 && !exStr.equals(toStringEx.getClass().getName())
            ? ": ".concat(exStr)
            : ""
        )
        .append('\n');
    }
    sb.append(o.getClass().getName())
      .append('@')
      .append(String.format("%08x", System.identityHashCode(o)));
    return sb.toString();
  }
  
  public static final void debug(String s) {
    if (DEBUG) {
      debug.println("// Debug: " + s);
    }
  }

  public Object get(String name) throws EvalError {
    if(name == null) {
      throw new NullPointerException("get: name == null");
    } else {
      if(TRACE) {
        System.err.printf("get\"%s\");", new Object[]{name});
      }

      if(BUILTIN_BSH.equals(name)) {
        return this.getNameSpace().getThis(this);
      } else if(BUILTIN_BSH_INTERACTIVE.equals(name)) {
        return Primitive.unwrap((Object)Boolean.valueOf(this.interactive));
      } else if(BUILTIN_EVAL_ONLY.equals(name)) {
        return Primitive.unwrap((Object)Boolean.valueOf(this.evalOnly));
      } else if(BUILTIN_BSH_SYSTEM.equals(name)) {
        return getBshSystemNs();
      } else if(BUILTIN_SHARED.equals(name)) {
        return getBshSystemNs();
      } else if(BUILTIN_HELP.equals(name)) {
        return getBshSystemNs();
      } else if(BUILTIN_CWD.equals(name)) {
        return System.getProperty("user.dir");
      } else {
        try {
          Object e = this.getNameSpace().get(name, this);
          return Primitive.unwrap(e);
        } catch (UtilEvalError var3) {
          throw var3.toEvalError(String.format("Error: get(\"%s\") on namespace: %s from interpreter: %s threw %s: %s", new Object[]{name, this.getNameSpace(), this, var3.getClass().getSimpleName(), var3.getMessage()}), SimpleNode.JAVACODE, new CallStack(this.getNameSpace()));
        }
      }
    }
  }

  public Object getu(String name) {
    try {
      return this.get(name);
    } catch (EvalError var3) {
      throw new InterpreterError(String.format("getu(\"%s\") -> %s: %s", new Object[]{name, var3.getClass().getSimpleName(), var3.getMessage()}));
    }
  }

  public void set(String name, Object value) throws EvalError {
    if(value == null) {
      value = Primitive.NULL;
    }

    CallStack callstack = new CallStack();

    try {
      if(Name.isCompound(name)) {
        LHS e = this.getNameSpace().getNameResolver(name).toLHS(callstack, this);
        e.assign(value, false);
      } else {
        this.getNameSpace().setVariable(name, value, false);
      }

    } catch (UtilEvalError var5) {
      throw var5.toEvalError(SimpleNode.JAVACODE, callstack);
    }
  }

  public void setu(String name, Object value) {
    ((NameSpace)this.namespace).variables.put(name, new Variable(name, value));
  }

  public void set(String name, long value) throws EvalError {
    this.set(name, new Primitive(value));
  }

  public void set(String name, int value) throws EvalError {
    this.set(name, new Primitive(value));
  }

  public void set(String name, double value) throws EvalError {
    this.set(name, new Primitive(value));
  }

  public void set(String name, float value) throws EvalError {
    this.set(name, new Primitive(value));
  }

  public void set(String name, boolean value) throws EvalError {
    this.set(name, new Primitive(value));
  }

  public void unset(String name) throws EvalError {
    CallStack callstack = new CallStack(this.getNameSpace());

    try {
      LHS e = this.getNameSpace().getNameResolver(name).toLHS(callstack, this);
      if(e.type != 0) {
        throw new EvalError("Can\'t unset, not a variable: " + name, SimpleNode.JAVACODE, new CallStack());
      } else {
        if(e.nameSpace != null) {
          e.nameSpace.unsetVariable(name);
        }

      }
    } catch (UtilEvalError var4) {
      throw new EvalError(var4.getMessage(), SimpleNode.JAVACODE, new CallStack(this.getNameSpace()));
    }
  }

  public Object getInterface(Class interf) throws EvalError {
    return this.getNameSpace().getThis(this).getInterface(interf);
  }

  public JJTParserState get_jjtree() {
    return this.parser.jjtree;
  }

  public JavaCharStream get_jj_input_stream() {
    return this.parser.jj_input_stream;
  }

  private boolean Line() throws ParseException {
    return this.parser.Line();
  }

  void loadRCFiles() {
    try {
      String e = System.getProperty("user.home") + File.separator + ".bshrc";
      this.source(e, this.getNameSpace());
    } catch (Exception var2) {
      if(DEBUG) {
        debug("Could not find rc file: " + var2);
      }
    }

  }

  public File pathToFile(String fileName) throws IOException {
    File file = new File(fileName);
    if(!file.isAbsolute()) {
      String cwd = System.getProperty("user.dir");
      file = new File(cwd + File.separator + fileName);
    }

    return new File(file.getCanonicalPath());
  }

  public static void redirectOutputToFile(String filename) {
    try {
      PrintStream e = new PrintStream(new FileOutputStream(filename));
      System.setOut(e);
      System.setErr(e);
    } catch (IOException var2) {
      System.err.println("Can\'t redirect output to file: " + filename);
    }

  }

  public void setClassLoader(ClassLoader externalCL) {
    this.getClassManager().setClassLoader(externalCL);
  }

  public BshClassManager getClassManager() {
    return this.bcm != null?this.bcm:BshClassManager.createClassManager(this);
  }

  public void setStrictJava(boolean b) {
    this.strictJava = b;
  }

  public boolean getStrictJava() {
    return this.strictJava;
  }

  static void staticInit() {
    try {
      debug = System.err;
      String e = System.getProperty("outfile");
      if(e != null) {
        redirectOutputToFile(e);
      }
    } catch (Throwable var1) {
      System.err.println("Could not init static:" + var1);
    }

  }

  public String getSourceFileInfo() {
    return this.sourceFileInfo != null?this.sourceFileInfo:"<unknown source>";
  }

  public Interpreter getParent() {
    return this.parent;
  }

  public void setOut(PrintStream out) {
    if(this.out != null) {
      this.out.flush();
    }

    this.out = out == null?new PrintStream(new ByteArrayOutputStream()):out;
  }

  public void setErr(PrintStream err) {
    if(this.err != null) {
      this.err.flush();
    }

    this.err = err == null?new PrintStream(new ByteArrayOutputStream()):err;
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    if(this.console != null) {
      this.setOut(this.console.getOut());
      this.setErr(this.console.getErr());
    } else {
      this.setOut(System.out);
      this.setErr(System.err);
    }

  }

  private String getBshPrompt() {
    return this.interactive?"bsh % ":"";
  }

  public void setExitOnEOF(boolean value) {
    this.exitOnEOF = false;
  }

  public void setShowResults(boolean showResults) {
    this.showResults = showResults;
  }

  public boolean getShowResults() {
    return this.showResults;
  }

  public static void setShutdownOnExit(boolean value) {
    shutdownOnExit = false;
  }

  public boolean getCompatibility() {
    return this.compatibility;
  }

  public void setCompatibility(boolean z) {
    this.compatibility = z;
  }

  public void setNameSpace(BshBinding ns) {
    this.namespace = ns;
  }

  private static class BindingHolder {
    private static BshBinding $$global$$ = null;

    public static BshBinding getDefault(BshClassManager cm) {
      if(Interpreter.TRACE) {
        System.err.println("getDefault");
      }

      if($$global$$ == null) {
        $$global$$ = Factory.get(NameSpace.class).make(new Object[]{cm, "global"});
      }

      return $$global$$;
    }
  }
  
  
  
  
  
  
  
  
  public static void main(String[] args) throws Throwable {
    String filename;
    String[] bshArgs = Arrays.copyOf(args, args.length);
    String[] newArgs = new String[bshArgs.length];
    
    InputStream src = System.in;
    Reader in = new CommandLineReader(new InputStreamReader(src));
    Interpreter interpreter = new Interpreter(in, System.out, System.err);
    interpreter.setu("bsh.args", bshArgs);
    Throwable lastError = null;
    boolean ranInterpreter = false;
    Object result = null;
    
    argsLoop:
    while (bshArgs.length > 0) {
      
      String arg = bshArgs[0];
      newArgs = new String[Math.max(bshArgs.length - 1, 0)];
      System.arraycopy(bshArgs, 1, newArgs, 0, newArgs.length);
      bshArgs = newArgs;
      StringBuilder sb = new StringBuilder();
      for (final String a: bshArgs) {
        if (sb.length() != 0) sb.append(", ");
        sb.append(
          (a == null)
            ? "null"
            : String.format("\"%s\"", StringEscapeUtils.escapeJava(a))
        );
      }
      sb.insert(0, "{ ").append(" }");
      
      System.err.printf(
        "arg = %s; bshArgs = String[]%s\n",
        arg, sb
      );
      if (arg.length() > 1 && arg.charAt(0) == '-') {
        char c = arg.charAt(1);
        int ci = (int) c;
        switch (ci) {
          case 'e': {
            String evalStr = bshArgs[0];
            newArgs = new String[Math.max(bshArgs.length - 1, 0)];
            System.arraycopy(bshArgs, 1, newArgs, 0, newArgs.length);
            bshArgs = newArgs;
            Reader evalReader = new StringReader(evalStr);
            BshBinding ns = interpreter.getNameSpace();
            CallStack cs = new CallStack(ns);
            try {
              interpreter.setu("bsh.args", bshArgs);
              ranInterpreter = true;
              result = null;
              result = interpreter.eval(evalReader, ns, "<eval input>", cs);
            } catch (final EvalError ee) {
              lastError = ee;
              ee.printStackTrace();
            }
            break;
          }
          case 'i': {
            interpreter.setu("bsh.args", bshArgs);
            ranInterpreter = true;
            result = null;
            result = interpreter.run();
            break;
          }
          default: {
            break;
          }
        }
      } else if (StringUtils.endsWith(arg, ".bsh")) {
        final File bshInputFile;
        try {
          bshInputFile = new File(arg);
          if (bshInputFile.exists()) {
            interpreter.setu("bsh.args", bshArgs);
            ranInterpreter = true;
            result = interpreter.source(
              bshInputFile.getAbsolutePath(),
              interpreter.getNameSpace()
              // getDefaultBinding(interpreter.getClassManager())
            );
          } else {
            throw new RuntimeException();
          }
        } catch (final IOException | RuntimeException e) {
          if (e instanceof IOException) {
            if (lastError == null) {
              lastError = e;
            } else {
              lastError.addSuppressed(e);
            }
          }
          // Put this argument back.
          newArgs = new String[bshArgs.length+1];
          newArgs[0] = arg;
          System.arraycopy(bshArgs, 0, newArgs, 1, bshArgs.length);
          bshArgs = newArgs;
        }
      } else {
        // ??
      }
      if (result != null) {
        System.err.printf(
          "lastError from last invocation: %s\n", lastError
        );
        System.err.printf(
          "result from last invocation:    %s\n", result
        );
      }
      final Class<?> mainClass = (result instanceof Class)
        ? (Class<?>) result
        : null;
      if (mainClass != null) {
        result = null;
        try {
          result = invokeMain(mainClass, bshArgs);
        } catch (Throwable e) {
          Object te = e;
          if (e instanceof InvocationTargetException) {
            te = ((InvocationTargetException) e).getTargetException();
            dbgprintln(
              "Class: " + result + " main method threw exception:" + te
            );
          }
          if (lastError == null) {
            lastError = e;
          } else {
            e.addSuppressed(lastError);
            lastError = e;
          }
        }
        if (result != null) {
          System.err.printf(
            "lastError from last invocation: %s\n", lastError
          );
          System.err.printf(
            "result from invokeMain on %s:   %s\n", mainClass, result
          );
        }
      }
    } // while bshArgs.length
    
    if (!ranInterpreter) {
      if (lastError != null) lastError.printStackTrace();
      interpreter.run();
    } else {
      if (lastError != null) {
        throw lastError;
      }
    }
  }

  
  
  
  
  
  
  
  
}