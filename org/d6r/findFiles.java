package org.d6r;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import bsh.Interpreter;
import bsh.CallStack;
import org.apache.commons.io.*;
import java.util.jar.*;
import java.util.zip.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;

public class findFiles  {
  
  public static boolean DEBUG = false;
  public static int DEFAULT_MAX_DEPTH = 12;
  
  public static File[] findJarFiles(String path) {
    
    File dir = new File(path); 
    Collection<File> files = (Collection<File>) 
      FileUtils.listFiles(
       dir, 
       new String[]{ "jar" },
       true
     ); 
    return files.toArray(new File[0]);  
  }

  public static File[] findFiles(String path, 
  String... extensions) 
  {
    
    File dir = new File(path); 
    Collection<File> files = (Collection<File>)
      FileUtils.listFiles(
       dir, 
       extensions,
       true
     ); 
    return files.toArray(new File[0]); 
  }
      
  public static File[] invoke
  (Interpreter env, CallStack stack, String path, String... exts) 
  { 
    if (exts.length == 0) {
      return findJarFiles(path);
    }
    return findFiles(path, exts);
  }
  
  public static <T> T[] findFiles
  (String path, Class<T> retCls, String... exts) 
  { 
    File[] result = null;
    if (exts.length == 0) {
      result = findJarFiles(path);
    } else {
      result = findFiles(path, exts);
    }
    Object[] outarr 
 = (Object[]) Array.newInstance(retCls, result.length);
    int idx = 0;
    if (retCls.equals(URL.class)) {
      for (File file: result) { 
        try {
          outarr[idx ] = new URL(String.format(
            (file.getPath().indexOf(".jar") != -1
              ? "jar:file://%s!/"
              : "file://%s"
            ), 
            file.getPath()
          ));
        } catch (Throwable e) {
          System.err.println(e.toString());
          outarr[idx ] = null;
        }
        idx++;
      }
    } else if (retCls.equals(JarFile.class)) {
      for (File file: result) { 
        try {
          outarr[idx ] = (
            (JarURLConnection) new URL(
              String.format("jar:file://%s!/", file.getPath())
            ).openConnection() 
          ).getJarFile();
        } catch (Throwable e) {
          System.err.println(e.toString());
          outarr[idx ] = null;
        }
        idx++;
      }
    } else if (retCls.equals(String.class)) {
      for (File file: result) {
        outarr[idx ] = file.getPath();
        idx++;
      }
    } else {
      return null;
    }
    return (T[]) outarr;     
  }
  
  public static <T> 
  T[] invoke(Interpreter env, CallStack stack, 
  String path, Class<T> retCls, String... exts) 
  { 
    return findFiles(path, retCls, exts);
  }
  
  
  public static class EmptyIterator<T>
    implements Iterator<T> 
  {
    public static final EmptyIterator<?> THE_ONE
      = new EmptyIterator<Object>();
    public static <E> EmptyIterator<E> get() {
      return (EmptyIterator<E>) THE_ONE;
    }
    
    @Override public boolean hasNext() { return false; }
    @Override public T next() {
      throw new NoSuchElementException(String.format(
        "%s.next() called on emptyIterator() {size: 0}",
        getClass().getName()
      ));
    }
    @Override public void remove() {
      throw new IllegalStateException(String.format(
        "%s.remove() called on emptyIterator() {size: 0}",
        getClass().getName()
      ));
    }
  }
  
  
  public static
  List<? extends File> iterate(File root, int maxDepth, 
  FilenameFilter fileFilter, FilenameFilter dirFilter) {
    List<File> al = new ArrayList<File>(); 
    
    if (root == null) {
      root = new File(System.getProperty("user.dir"));
    }
    if (! root.exists()) {
      System.err.printf(
        "[WARN] findFiles.iterate: "
      + "root '%s' does not exist", root.getPath()
      );
      return Collections.emptyList(); //EmptyIterator.get();
    }
        
    ArrayDeque<Object[]> input = new ArrayDeque<Object[]>(); 
    Object[] rootNode = new Object[]{ root, 0 };
    input.offer(rootNode); 
    int fdepth; 
    int maxdepth = 2; 
    Object[] node; 
    File f, ffile;
    while (! input.isEmpty()) { 
      node = input.pollFirst(); 
      if (node == null) continue; 
      ffile = (File) node[0];
      if (ffile == null) continue;
      fdepth = (int) node[1];
      
      if (ffile.isDirectory()) {
        if (dirFilter != null 
        && !dirFilter.accept(ffile, ffile.getName())) {
          continue; 
        }
        if (fdepth <= maxDepth) {
          for (File child: ffile.listFiles()) {
            input.offerLast(new Object[]{ child, fdepth+1 });
          }
        }
      } else {
        if (fileFilter != null 
        && !fileFilter.accept(ffile, ffile.getName())) {
          continue; 
        }
        if (DEBUG) System.err.printf(
          "[depth = %4d] %s\n", fdepth, ffile.getPath()
        );
        al.add(ffile);
      }
    } 
    return al;
  }
  
  public static
  List<? extends File> iterate(
  File root, int maxDepth, FilenameFilter fileFilter) {
    return iterate(root, maxDepth, fileFilter, null);
  }
  
  public static
  List<? extends File> iterate(
  final File root, int maxDepth, final String regex) 
  {
    final Pattern ptrn = Pattern.compile(
      regex, 
      Pattern.CASE_INSENSITIVE 
        | Pattern.DOTALL 
        | Pattern.UNIX_LINES
    );
    return iterate(root, maxDepth, ptrn);
  }
  
  public static
  List<? extends File> iterate(
  final String root, int maxDepth, final String regex) {
    return iterate(new File(root), maxDepth, regex);
  }  
  
  public static
  List<? extends File> iterate(
  final File root, int maxDepth, final Pattern regex) 
  {
    final Matcher mchr = regex.matcher("");
    
    final FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File file, String name) {
        return file != null && name != null && (
             mchr.reset( name ).matches() 
          || mchr.reset( file.getPath() ).matches()
        );        
      }
    };
    return iterate(root, maxDepth, filter);
  }
  
  public static
  List<? extends File> iterate(
  final String root, int maxDepth, final Pattern ptrn) {
    return iterate(new File(root), maxDepth, ptrn);
  }  
  
  public static 
  List<? extends File> iterate(File root, String regex) {
    return iterate(root, DEFAULT_MAX_DEPTH, regex);
  }
  
  public static 
  List<? extends File> iterate(File root, Pattern ptrn) {
    return iterate(root, DEFAULT_MAX_DEPTH, ptrn);
  }
  
  public static 
  List<? extends File> iterate(String root, String regex) {
    return iterate(root, DEFAULT_MAX_DEPTH, regex);
  }
  
  public static
  List<? extends File> iterate(String root, Pattern ptrn) {
    return iterate(root, DEFAULT_MAX_DEPTH, ptrn);
  }
  
  
  public static
  List<? extends File> iterate(File root, int maxDepth) {
    return iterate(root, maxDepth, (FilenameFilter)null);
  }
  
  public static List<? extends File> iterate(File root) {
    return iterate(root, DEFAULT_MAX_DEPTH);
  }
  
  public static List<? extends File> iterate() {
    return iterate(null);
  }
  
  
  public static List<? extends File> invoke(
  Interpreter env, CallStack stk, File root, int maxDepth,
  String regex) {
    return iterate(root, maxDepth, regex);
  }
  
  
  
  public static List<? extends File> invoke(
  Interpreter in, CallStack stk, File root, String regex) {
    return iterate(root, regex);
  }
  
  public static List<? extends File> invoke(
  Interpreter in, CallStack stk, File root, Pattern ptrn) {
    return iterate(root, ptrn);
  }
  
  public static List<? extends File> invoke(
  Interpreter in, CallStack stk, String root, String regex) {
    return iterate(root, regex);
  }
  
  public static List<? extends File> invoke(
  Interpreter in, CallStack stk, String root, Pattern ptrn) {
    return iterate(root, ptrn);
  }
  
  
  public static List<? extends File> invoke(
  Interpreter env, CallStack stk, File root, int maxDepth,
  Pattern ptrn) {
    return iterate(root, maxDepth, ptrn);
  }
  
  public static List<? extends File> invoke(
  Interpreter env, CallStack stk, File root, int maxDepth) { 
    return iterate(root, maxDepth);
  }
  
  public static List<? extends File> invoke(
  Interpreter env, CallStack stk, File root) { 
    return iterate(root);
  }
  
  public static List<? extends File> invoke(
  Interpreter env, CallStack stk) { 
    return iterate();
  }
  
  
  
}

/* Location:           /storage/extSdCard/_projects/sdk/bsh/trunk/out
  * Qualified Name:     ls
  * Java Class Version: 6 (50.0)
  * JD-Core Version:    0.7.1
  */
 