package bsh;

import bsh.BSHType;
import bsh.BlockNameSpace;
import bsh.BshBinding;
import bsh.CallStack;
import bsh.CollectionManager;
import bsh.EvalError;
import bsh.Factory;
import bsh.Interpreter;
import bsh.Modifiers;
import bsh.ParserConstants;
import bsh.Primitive;
import bsh.ReturnControl;
import bsh.SimpleNode;
import bsh.UtilEvalError;
import java.util.Iterator;

class BSHEnhancedForStatement extends SimpleNode implements ParserConstants {
  String varName;

  BSHEnhancedForStatement(int id) {
    super(id);
  }

  public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    Class elementType = null;
    SimpleNode statement = null;
    BshBinding enclosingNameSpace = callstack.top();
    SimpleNode firstNode = (SimpleNode)this.jjtGetChild(0);
    int nodeCount = this.jjtGetNumChildren();
    SimpleNode expression;
    if(firstNode instanceof BSHType) {
      elementType = ((BSHType)firstNode).getType(callstack, interpreter);
      expression = (SimpleNode)this.jjtGetChild(1);
      if(nodeCount > 2) {
        statement = (SimpleNode)this.jjtGetChild(2);
      }
    } else {
      expression = firstNode;
      if(nodeCount > 1) {
        statement = (SimpleNode)this.jjtGetChild(1);
      }
    }

    BlockNameSpace eachNameSpace = (BlockNameSpace)Factory.get(BlockNameSpace.class).make(new Object[]{enclosingNameSpace});
    callstack.swap(eachNameSpace);
    Object iteratee = expression.eval(callstack, interpreter);
    if(iteratee == Primitive.NULL) {
      throw new EvalError("The collection, array, map, iterator, or enumeration portion of a for statement cannot be null.", this, callstack);
    } else {
      CollectionManager cm = CollectionManager.getCollectionManager();
      if(!cm.isBshIterable(iteratee)) {
        throw new EvalError("Can\'t iterate over type: " + iteratee.getClass(), this, callstack);
      } else {
        Iterator iterator = cm.getBshIterator(iteratee);
        Object returnControl = Primitive.VOID;

        while(iterator.hasNext()) {
          try {
            Object breakout = iterator.next();
            if(breakout == null) {
              breakout = Primitive.NULL;
            }

            if(elementType != null) {
              eachNameSpace.setTypedVariable(this.varName, elementType, breakout, new Modifiers());
            } else {
              eachNameSpace.setVariable(this.varName, breakout, false);
            }
          } catch (UtilEvalError var16) {
            throw var16.toEvalError("for loop iterator variable:" + this.varName, this, callstack);
          }

          boolean breakout1 = false;
          if(statement != null) {
            Object ret = statement.eval(callstack, interpreter);
            if(ret instanceof ReturnControl) {
              switch(((ReturnControl)ret).kind) {
              case 12:
                breakout1 = true;
              case 19:
              default:
                break;
              case 46:
                returnControl = ret;
                breakout1 = true;
              }
            }
          }

          if(breakout1) {
            break;
          }
        }

        callstack.swap(enclosingNameSpace);
        return returnControl;
      }
    }
  }
}
