/**
error @ line 43 of /storage/extSdCard/_projects/sdk/relproxy/src/org/d6r/YConsole.java :
    Cannot make a static reference to the non-static field YConsole.console
        YConsole.console = console;
        ^^^^^^^^^^^^^^^^
error @ line 57 of /storage/extSdCard/_projects/sdk/relproxy/src/org/d6r/YConsole.java :
    The blank final field console may not have been initialized
        public YConsole(IInterpreter pInterpreter) {
               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
error @ line 85 of /storage/extSdCard/_projects/sdk/relproxy/src/org/d6r/YConsole.java :
    The method parseInput(String, List<Object>) from the type IInterpreter refers to the missing type List
        hasRequestedQuit = interpreter.parseInput(line, result);
                                       ^^^^^^^^^^
*/
package org.d6r;

import java.util.concurrent.BrokenBarrierException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import java.util.regex.*;
/**
* Sends text back and forth between the command line and an
* Interpreter. JDK less than 6.
*/
public class YConsole {
  
  // static
  public static YConsole current = null;
  
  // instance
  public IInterpreter interpreter;
  public BufferedInputStream in = (BufferedInputStream) System.in;
  public PrintStream out = System.out;
  public PrintStream err = System.err;
  
  
  

  public YConsole(IInterpreter pInterpreter) {
    if (pInterpreter == null) {
      throw new IllegalArgumentException(
        "Interpreter cannot be null.");
    }
    this.interpreter = pInterpreter;
  }
  
  public YConsole(String iClsName) {
    try {
      Class<?> theClass = Class.forName(iClsName);
      interpreter = (IInterpreter)theClass.newInstance();
      return;
    } catch (ClassNotFoundException ex) {
      System.err.println(ex + " Interpreter class must be in class path.");
    } catch (InstantiationException ex) {
      System.err.println(ex + " Interpreter class must be concrete.");
    } catch (IllegalAccessException ex) {
      System.err.println(ex + " Interpreter class must have a no-arg constructor.");
    }
    
  }
  
  
  public static YConsole getDefault() {
    if (current == null) {
      current = new YConsole(
        ToyInterpreter.class.getName()
      );
    };
    return current;
  }
  
  
  /**
  Build and launch a specific <code>Interpreter</code>, whose package-qualified name is passed in on the command line
; */
  public static void main(String... args) {    
    YConsole console = new YConsole(args[0]);
    YConsole.current = console;
    console.run();
  }
  
   
  /**
  Display a prompt, wait for a full line of input, and then parse
  the input using an Interpreter.
  *
  Exit when <code>Interpreter.parseInput</code> returns true.
  */
  
  
  
  public void run() {
    display(interpreter.getHelloPrompt());

    //pass each line of input to interpreter, and display
    //interpreter's result
    InputStreamReader isr = new InputStreamReader(in);
    BufferedReader stdin = new BufferedReader(isr);
    boolean hasRequestedQuit = true;
    String line = null;
    boolean keepRunning = true;
    List<Object> result = new ArrayList<Object>();
    try {
      while (keepRunning) {
        line = stdin.readLine();
        //note that "result" is passed as an "out" parameter
        keepRunning 
 = interpreter.parseInput(line, result);
        display(result);
        result.clear();
      }
    }
    catch (IOException ex) {
      System.err.println(ex);
    }
    finally {
      shutdown(stdin);
    }
  }
  
  /**
  Display some text to stdout.
  The result of toString() is used.
  */
  private void display(Object aText) 
  {
    if (! (aText instanceof Object)) {
      out.println("");
      return;
    }
    StringBuilder sb = new StringBuilder(76 * 10);
    Class<?> cls = aText.getClass();
    String str = null;
    if (cls.isArray()) {
      int aLen = Array.getLength(aText);
      for (int i=0; i<aLen; i++) {
        if (i == 255) {
          sb.append(String.format("[... %d more array elements ...]\n", aLen - i));
          break;
        }
        Object val = Array.get(aText, i);
        try {
          str = val != null? val.toString(): "null";
        } catch (Throwable e) {
          sb.append(String.format(
           "%s.toString() threw %s: [%s]\n",
            cls.getComponentType(), 
            e.getClass().getSimpleName(),
            e.getMessage() != null? e.getMessage(): ""
          ));
          continue; 
        }
        int len;
        sb.append((len = str.length()) > 512?
          str.substring(0, 512): str
        );
      }
      return;
    }
    
    try {
      str = aText.toString();
    } catch (Throwable e) {
      out.println(sb.toString());
      out.flush();
      err.println(String.format(
       "%s.toString() threw %s: [%s]\n",
        cls, 
        e.getClass().getSimpleName(),
        e.getMessage() != null? e.getMessage(): ""
      ));
      err.flush();
      return;
    }
    
    out.println(str);
    out.flush();
  }

  private void shutdown(Reader aStdin){
    try {
      aStdin.close();
    }
    catch (IOException ex){
      System.err.println(ex);
    }
  }
} 



/**
An example run:
java -cp . Console InheritanceInterpreter

Please enter a class name>as;k

Invalid.  Example:"java.lang.String">java.lang.String

The inheritance tree:

class java.lang.String

class java.lang.Object

Please enter a class name>

Invalid.  Example:"java.lang.String">

Invalid.  Example:"java.lang.String">....

Invalid.  Example:"java.lang.String">a;lskf

Invalid.  Example:"java.lang.String">java.sql.SQLWarning

The inheritance tree:

class java.sql.SQLWarning

class java.sql.SQLException

class java.lang.Exception

class java.lang.Throwable

class java.lang.Object

Please enter a class name>java.util.GregorianCalendar

The inheritance tree:

class java.util.GregorianCalendar

class java.util.Calendar

class java.lang.Object

Please enter a class name>exit


Bye.
*/



/*
JDK < 6

The Console class was added in Java 6. 
The following is an extended example of using an older version of the JDK. 
Here, input is read from the console in a continuous loop. 
As well, it has separated the problem into several parts, such that some parts can be 
reused in other console applications.

As in the previous example, the user inputs a package-qualified class name, and the corresponding
inheritance tree is displayed.
;
*/