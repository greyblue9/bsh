package org.d6r;
/**
A helper to use Javassist effectively
*/
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

//import javassist.compiler.ast.Pair;
import javassist.expr.MethodCall;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.tuple.Pair;

public class JavassistHelper implements Runnable {

  public Set<String> classNames = new HashSet<String>();

  public static     Map<String, CtClass> definedClasses 
    = new LinkedHashMap<String, CtClass>();

  public static ClassPool pool;

  public static boolean frozen;

  static {
    pool = ClassPool.getDefault();
    pool.appendClassPath(
      new ClassClassPath(JavassistHelper.class)
    );
  }

  public CtClass get(String className) 
    throws NotFoundException
  {
    if (!definedClasses.containsKey(className)) {
      definedClasses.put(className, pool.get(className));
      classNames.add(className);
    }
    return definedClasses.get(className);
  }

  public void add(CtClass clazz) {
    classNames.add(clazz.getName());
    definedClasses.put(clazz.getName(), clazz);
  }

  public static void defineClasses() 
    throws CannotCompileException
  {
    if (frozen) {
      new Exception(
        "Attempted to defined patched classes again"
      ).printStackTrace();
      return;
    }
    for (String name : definedClasses.keySet()) {
      final CtClass clazz = definedClasses.get(name);
      if (clazz.isFrozen() || !clazz.isModified()) {
        // assume that ij-legacy did something about it
        continue;
      }
      try {
        clazz.toClass();
      } catch (CannotCompileException e) {
        final Throwable cause = e.getCause();
        if (cause != null && !(cause instanceof LinkageError)) {
          throw e;
        }
      }
    }
    frozen = true;
  }

  @Override
  public final void run() {
    if (frozen) {
      System.err.println(
        "Attempted to patch classes again: " 
        + getClass().getName()
      );
      return;
    }
    try {
      instrumentClasses();
    } catch (BadBytecode e) {
      e.printStackTrace();
    } catch (NotFoundException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (CannotCompileException e) {
      System.err.println(e.getMessage() + "\n" + e.getReason());
      e.printStackTrace();
      Throwable cause = e.getCause();
      if (cause != null) cause.printStackTrace();
    }
  }

  public <R> R instrumentClasses() 
    throws BadBytecode, CannotCompileException,    
           NotFoundException
  {
    return null;
  }

  public String getLatestArg(MethodCall call, int skip) 
    throws BadBytecode {
    int[] indices = new int[skip + 1];
    int counter = 0;
    MethodInfo info = ((CtMethod) call.where()).getMethodInfo();
    CodeIterator iterator = info.getCodeAttribute().iterator();
    int currentPos = call.indexOfBytecode();
    while (iterator.hasNext()) {
      int pos = iterator.next();
      if (pos >= currentPos) break;
      switch((iterator.byteAt(pos))) {
        case Opcode.LDC:
          indices[(counter++) % indices.length] 
            = iterator.byteAt(pos + 1);
          break;
        case Opcode.LDC_W:
          indices[(counter++) % indices.length] 
            = iterator.u16bitAt(pos + 1);
          break;
      }
    }
    if (counter < skip) return null;
    return info.getConstPool().getStringInfo(
      indices[(indices.length + counter - skip) % indices.length]
    );
  }
  
  

  public boolean hasClass(String name) {
    try {
      return pool.get(name) != null;
    } catch (NotFoundException e) {
      return false;
    }
  }

  public boolean hasField(CtClass clazz, String name) {
    try {
      return clazz.getField(name) != null;
    } catch (NotFoundException e) {
      return false;
    }
  }

  public boolean hasMethod(CtClass clazz, String name, 
  String signature) 
  {
    try {
      return clazz.getMethod(name, signature) != null;
    } catch (NotFoundException e) {
      return false;
    }
  }

  public static String stripPackage(String className) {
    int lastDot = -1;
    for (int i = 0; ; i++) {
      if (i >= className.length()) {
        return className.substring(lastDot + 1);
      }
      char c = className.charAt(i);
      if (c == '.' || c == '$') lastDot = i; 
      else if (c >= 'A' && c <= 'Z') ; // continue
      else if (c >= 'a' && c <= 'z') ; // continue
      // continue
      else if (i > lastDot + 1 && c >= '0' && c <= '9') ;
      else {
        return className.substring(lastDot + 1);
      }
    }
  }

  public static void verify(CtClass clazz, PrintStream output) {
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(stream);
      clazz.getClassFile().write(out);
      out.flush();
      out.close();
      verify(stream.toByteArray(), output);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void verify(byte[] bytecode, PrintStream out) {
    try {
      /*
      final File[] files = FileUtils.getAllVersions(
        new File(System.getProperty("ij.dir"), "jars"),
        "jruby.jar"
      );
      final URL[] urls = new URL[files.length];
      for (int i = 0; i < urls.length; i++) {
        urls[i] = files[i].toURI().toURL();
      }
      */
      ClassLoader loader 
        = Thread.currentThread().getContextClassLoader();
      Class<?> readerClass = loader.loadClass(
        "org.objectweb.asm3.ClassReader"
      );
      java.lang.reflect.Constructor<?> ctor 
        = readerClass.getConstructor(bytecode.getClass());
      Object reader = ctor.newInstance(bytecode);
      Class<?> checkerClass = loader.loadClass(
        "org.objectweb.asm3.util.CheckClassAdapter"
      );
      java.lang.reflect.Method verify = checkerClass.getMethod(
        "verify", readerClass, Boolean.TYPE, PrintWriter.class
      );
      verify.invoke(null, reader, false, new PrintWriter(out));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void verify(PrintStream out) {
    for (String name : classNames) {
      out.println("Verifying class " + name);
      verify(definedClasses.get(name), out);
    }
  }

  public static void disassemble(CtClass clazz, PrintStream out) 
  {
    disassemble(clazz, out, false);
  }

  public static void disassemble(CtClass clazz, PrintStream out, 
  boolean evenSuperclassMethods)
  {
    out.println("Class " + clazz.getName());
    for (CtConstructor ctor : clazz.getConstructors()) try {
      disassemble(ctor.toMethod(ctor.getName(), clazz), out);
    } catch (CannotCompileException e) {
      e.printStackTrace(out);
    }
    for (CtMethod method : clazz.getDeclaredMethods()) {
      if (evenSuperclassMethods 
      ||  method.getDeclaringClass().equals(clazz)) {
        disassemble(method, out);
      }
    }
  }

  public static void disassemble(CtMethod method, 
  PrintStream out) 
  {
    out.println(method.getLongName());
    new InstructionPrinter(out).print(method);
    out.println("");
  }

  public void writeJar(File path) 
    throws IOException
  {
    JarOutputStream jar = new JarOutputStream(
      new FileOutputStream(path)
    );
    DataOutputStream dataOut = new DataOutputStream(jar);
    for (String name : classNames) {
      CtClass clazz = definedClasses.get(name);
      ZipEntry entry = new ZipEntry(
        clazz.getName().replace('.', '/') + ".class"
      );
      jar.putNextEntry(entry);
      clazz.getClassFile().write(dataOut);
      dataOut.flush();
    }
    jar.close();
  }
  

  public static 
  Pair<int[], int[]> getIndexesAndLengths(CtBehavior cb) 
  { 
    CodeAttribute ca = cb.getMethodInfo2().getCodeAttribute(); 
    CodeIterator ci = ca.iterator();
    List<Integer> lengths = new ArrayList<Integer>(); 
    List<Integer> startIndexes = new ArrayList<Integer>(); 
    
    int pos = 0, idx = 0, lastIdx = 0,lastPos = 0, 
        length = ci.getCodeLength(); 
    ci.setMark(length); 
    do { 
      lastPos = pos;
      pos = ((Integer) Reflect.getfldval(ci, "currentPos"))
        .intValue();
      if (pos - lastPos != 0) {
        lengths.add(Integer.valueOf(pos - lastPos));
        startIndexes.add(Integer.valueOf(lastPos));
      }
      if (!ci.hasNext()) break; 
      try {
        idx = ci.next();
      } catch (BadBytecode bb) {
        throw (RuntimeException) Reflector.Util.sneakyThrow(bb);
      }
      
    } while (true);
    
    return Pair.of(
      ArrayUtils.toPrimitive(
        startIndexes.toArray(new Integer[0])
      ),
      ArrayUtils.toPrimitive(lengths.toArray(new Integer[0]))
    );
  }
  
  public static byte[] getBytecode(CtBehavior cb) {
    byte[] bc = Reflect.getfldval(
      cb.getMethodInfo2().getCodeAttribute(), "info"
    );
    return bc;
  }
  
  public static byte[][] getInsns(CtBehavior cb) {
    Pair<int[], int[]> pair = getIndexesAndLengths(cb); 
    int[] indexes = pair.getLeft(); 
    int[] lengths = pair.getRight(); 
    byte[] bc = getBytecode(cb);
    byte[][] insns = new byte[lengths.length][];
    byte[] insn;
    int bidx = -1, offset = 0, len = 0;
    
    CodeIterator ci 
      = cb.getMethodInfo2().getCodeAttribute().iterator();
    Iterator<Integer> it = new ArrayIterator(lengths);
    while (it.hasNext()) { 
      bidx += 1; 
      //try {
      len = it.next().intValue(); 
      //} catch (BadBytecode bb) {
        //throw (RuntimeException) Reflector.Util.sneakyThrow(bb);
      //}
      insn = Arrays.copyOfRange(bc, offset, offset + len);
      insns[bidx] = insn;
      offset += len;
    }
    return insns;
  }
  
  
  
}