package org.d6r;

import java.io.BufferedReader;
import com.google.common.base.Function;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;

public class ProcessTest {
  
  public static void debug(Object... args) {
    System.err.printf(
      "[DEBUG] %s\n", Arrays.asList(args)
    );
  }
  
  // main method for testability:
  // replace with private void exec(String command)
  public static void main(String... args) 
    throws Exception
  {
    exec(args);
  }
    
  
  public static void exec(String... cmd)
    throws Exception
  {
    // Create a lock that will be shared between reader
    // threads. The lock is fair to minimize starvation
    // possibilities.
    final ReentrantLock lock = new ReentrantLock(true);
    // exec the command: I use nslookup for testing on
    // Windows because it is interactive, and prints to
    // stderr too.
    final Process p = Runtime.getRuntime().exec(cmd);
    // Create a thread to handle output from process --
    // uses a test consumer
    final Thread outThread = createThread(
      p.getInputStream(), lock, 
      new Function<String, Void>() {
        @Override
        public Void apply(final String line) {
          System.out.print(line);
          return null;
        }
      }
    );
    outThread.setName("outThread");
    outThread.start();
    // create a thread to handle error from process
    // (test consumer, again)
    final Thread errThread = createThread(
      p.getErrorStream(), lock, 
      new Function<String, Void>() {
        @Override
        public Void apply(final String line) {
          System.err.print(line);
          return null;
        }
      }
    );
    errThread.setName("errThread");
    errThread.start();
    // create a thread to handle input to process --
    // read from stdin for testing purpose
    final PrintWriter writer 
      = new PrintWriter(p.getOutputStream());
    
    final Thread inThread = createThread(
      System.in, null, 
      new Function<String, Void>() {
        @Override
        public Void apply(final String line) {
          writer.print(line);
          writer.flush();
          return null;
        }
      }
    );
    inThread.setName("inThread");
    inThread.start();
    // Create a thread to handle termination gracefully.
    // Not really needed in this simple
    // scenario, but on a real application we don't want
    // to block the UI until process dies
    final Thread endThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // wait until process is done
          p.waitFor();
          debug("process exit");
          // signal threads to exit
          outThread.interrupt();
          errThread.interrupt();
          inThread.interrupt();
          // close process streams
          p.getOutputStream().close();
          p.getInputStream().close();
          p.getErrorStream().close();
          // wait for threads to exit
          outThread.join();
          errThread.join();
          inThread.join();
          debug("exit");
        } catch(final Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });
    endThread.setName("endThread");
    endThread.start();
    // wait for full termination -- process and related
    // threads by cascade joins
    endThread.join();
    debug("END");
  }
  // convenience method to create a specific reader
  // thread with exclusion by lock behavior
  public static <T> 
  Thread createThread(final InputStream input,
  final ReentrantLock lock, 
  final Function<String, T> consumer)
  {
    return new Thread(new Runnable() {
      @Override
      public void run() {
      // wrap input to be buffered (enables ready()) and
      // to read chars using explicit encoding may be
      // relevant in some case
      final BufferedReader reader = new BufferedReader(
        new InputStreamReader(input)
      );
      // create a char buffer for reading
      final char[] buffer = new char[8192];
      try {
        // repeat until EOF or interruption
        while(true) {
          try {
          // wait for your turn to bulk read
            if (lock != null 
            && !lock.isHeldByCurrentThread()) {
              lock.lockInterruptibly();
            }
            // when there's nothing to read, pass the
            // hand (bulk read ended)
            if (!reader.ready()) {
              if (lock != null) {
                lock.unlock();
              }
              // this enables a soft busy-waiting loop,
              // that simultates non-blocking reads
              Thread.sleep(100);
              continue;
            }
            // perform the read, as we are sure it will
            // not block (input is "ready")
            final int len = reader.read(buffer);
            if (len == -1) return;
            // transform to string an let consumer
            // consume it
            final String str 
              = new String(buffer, 0, len);
            
            consumer.apply(str);            
          } catch (InterruptedException e) {
            // catch interruptions either when sleeping
            // and waiting for lock, and restore
            // interrupted flag -- 
            // not necessary in this case, however it's a
            // best practice
            Thread.currentThread().interrupt();
            return;
          } catch(IOException e) {
            throw new RuntimeException(
              e.getMessage(), e
            );
          }
        }
      } finally {
        // protect the lock against unhandled exceptions
        if (lock != null && lock.isHeldByCurrentThread())
        {
          lock.unlock();
        }
        debug("exit");
      }
    } // run
  }); // new Runnable() { ... }
   
  } // createThread
}

