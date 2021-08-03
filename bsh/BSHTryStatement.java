package bsh;

import bsh.BSHBlock;
import bsh.BSHFormalParameter;
import bsh.BlockNameSpace;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.Node;
import bsh.Null;
import bsh.Primitive;
import bsh.ReflectError;
import bsh.SimpleNode;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.apache.commons.lang3.ClassUtils;
import org.d6r.Reflector.Util;


public class BSHTryStatement extends SimpleNode {
    
  protected boolean D; // debug flag
  public static Modifiers EMPTY_MODIFIERS = new Modifiers();
  
  protected BSHBlock tryBlock;
  protected List<BSHFormalParameter> catchParams;
  protected List<BSHBlock> catchBlocks;
  protected BSHBlock finallyBlock;
  
  public BSHTryStatement(int id) {
    super(id);
    D = Interpreter.DEBUG;
  }

  public BSHTryStatement d(String fmt, Object... args) {
    if(D) {
      for(int e = 0; e < args.length; ++e) {
        if(args[e] == null) {
          args[e] = Null.NULL;
        } else {
          Class<?> cls = args[e].getClass();
          if(!ClassUtils.isPrimitiveOrWrapper(cls)) {
            args[e] = args[e];
          }
        }
      }

      try {
        System.err.printf(fmt, args);
      } catch (Throwable var5) {
        System.err.printf(
          "[WARN] String.format error:\n  fmt = \"%s\"\nexception = %s\n",
          var5
        );
        var5.printStackTrace();
      }

    }
    return this;
  }
  
  public BSHTryStatement dl(String fmt, Object... args) {
    return d(fmt.concat("\n"), args);
  }
  
  @Override
  public Object eval(CallStack callstack, Interpreter interpreter) 
    throws EvalError 
  {
    super.touch(callstack);
    Deque<BshBinding> unwound = new ArrayDeque<BshBinding>();
    BSHBlock tryBlock = (BSHBlock)this.jjtGetChild(0);
    List<BSHFormalParameter> catchParams = new LinkedList<>();
    List<BSHBlock> catchBlocks = new ArrayList<>();
    int nchild = jjtGetNumChildren();
    Node node = null;

    for(int i = 1; 
        i<nchild 
          && (node = this.jjtGetChild(i++)) instanceof BSHFormalParameter; 
        node = null) 
    {
      BSHFormalParameter node1;
      BSHBlock node2;
      catchParams.add((node1 = (BSHFormalParameter) node));
      catchBlocks.add((node2 = (BSHBlock) this.jjtGetChild(i++)));
      if(D)dl("added catch block: \n  %s\n  %s", node1, node2);
    }
    if(D)dl("catch params (%d): %s", catchParams.size(), catchParams);
    
    BSHBlock finallyBlock = (node != null) 
      ? (BSHBlock)node
      : null;    
    if(D)dl("finally block: %s", finallyBlock);
    
    int callstackDepth = callstack.depth();
    Object target = null;
    Object thrown = null;
    Object ret = null;
    Object tryCallstack = null;
    Object _ite = null;

    try {
      tryBlock.touch(callstack);
      return tryBlock.eval(callstack, interpreter);
      
    } catch (Throwable tryBlockEvalExc) {
      interpreter.setu("$exception", tryBlockEvalExc);
      interpreter.setu("$node", tryBlock);
      
      Set<Throwable> throwables = new LinkedHashSet<Throwable>(
        Arrays.asList(tryBlockEvalExc)
      );
      Throwable e = tryBlockEvalExc;
      
      while (tryBlockEvalExc instanceof InvocationTargetException) {
        tryBlockEvalExc 
          = ((InvocationTargetException) e).getTargetException();
        throwables.add(tryBlockEvalExc);
      }
      Throwable cur;
      while (tryBlockEvalExc instanceof EvalError 
      && (cur = tryBlockEvalExc.getCause()) != tryBlockEvalExc
      &&  cur != null) {
        tryBlockEvalExc = cur;
        throwables.add(tryBlockEvalExc);
      }
      while (tryBlockEvalExc instanceof InvocationTargetException) {
        tryBlockEvalExc
        = ((InvocationTargetException) tryBlockEvalExc).getTargetException();
        throwables.add(tryBlockEvalExc);      
      }

      if(D)dl(" TRY BLOCK EVAL threw %s; ret --> %s", tryBlockEvalExc, ret);
      
      boolean canCatchThisException = false;
      Iterator<BSHBlock> catchIt = catchBlocks.iterator();
      Iterator<BSHFormalParameter> catchParamIt = catchParams.iterator();

      while (catchParamIt.hasNext()) {
        BSHFormalParameter catchDecl = catchParamIt.next();
        BSHBlock catchBlock = catchIt.next();
        if(D)dl("[catchDecl = %s]", catchDecl);
        
        Object evalled = catchDecl.eval(callstack, interpreter);
        if(D)dl("  - eval returned %s", evalled);

        Class<?> catchesClass = catchDecl.type;
        if(D)dl("  - catchesClass = %s", catchesClass);

        if(catchesClass == null) catchesClass = Throwable.class;
        
        Class<?> exClass = tryBlockEvalExc.getClass();
        canCatchThisException = catchesClass.isAssignableFrom(exClass);
        if (canCatchThisException) {
          e = tryBlockEvalExc;
        } else {
          for (Throwable th: throwables) {
            if (th != null && catchesClass.isAssignableFrom(th.getClass())) {
              canCatchThisException = true;
              e = th;
              if(D)dl(
                "    --> found in set: canCatchThisException(%s): %s",
                th.getClass().getName(), canCatchThisException
              );
            }
          }
        }
        if(D)dl("    --> canCatchThisException: %s", canCatchThisException);

        if (canCatchThisException) {
          if(D)dl("  - catchBlock = %s", catchBlock);

          BshBinding enclosingBinding = callstack.top();
          if(D)dl("  - enclosingBinding = %s", enclosingBinding);

          if(D)dl("  - Making namespace for catch block ...");

          BlockNameSpace catchBinding 
            = Factory.get(BlockNameSpace.class).make(enclosingBinding);
          if(D)dl("  - catchBinding = %s", catchBinding);
          if(D)dl("  - Set var \'%s\' in catch ns:", catchDecl.name);

          catchBinding.setBlockVariable(catchDecl.name, e);
          catchBinding.setTypedVariable(
            catchDecl.name, catchesClass, e, EMPTY_MODIFIERS
          );
          
          if(D)dl(
            "callstack.depth() = %d; callstackDepth = %d",
            callstack.depth(), callstackDepth
          );

          int depth = callstack.depth();
          int amtToPop = depth - callstackDepth;
          int popped = -1;
          
          
          if(D)dl("Unwinding %d stack frames ...", amtToPop);

          while(true) {
            ++popped; // initial -> 0
            if(popped >= amtToPop) {
              if(D)dl("popped callstacks %s\n:", unwound);

              Object evalResult;
              try {
                if(D)dl("Calling swap() with catch binding ...");

                callstack.swap(catchBinding);
                if(D)dl("evalling catch block ...");
                catchBlock.touch(callstack);
                evalResult = catchBlock.eval(callstack, interpreter);
                if(D)dl("evalResult = %s", evalResult);
              } finally {
                if(D)dl("Swapping back out of catch namespace ...");
                callstack.swap(enclosingBinding);
              }

              return evalResult != null
                ? evalResult
                : Primitive.VOID;
            }
            
            unwound.add(callstack.pop());
          }
        }
      }
      throw Util.sneakyThrow(e);
    } // catch (Throwable tryBlockEvalExc) 
      finally { 
      if (unwound != null) interpreter.setu("$unwound", unwound);
    }
  }
}