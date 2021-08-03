package bsh.classpath;

import bsh.ClassPathException;
import bsh.Interpreter;
import bsh.NameSource;
import bsh.StringUtil;
import bsh.classpath.ClassPathListener;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class BshClassPath implements ClassPathListener, NameSource {
  String name;
  public List<URL> path;
  public List<BshClassPath> compPaths;
  public Map<String, Collection<BshClassPath>> packageMap;
  public Map<String, BshClassPath.ClassSource> classSource;
  public boolean mapsInitialized;
  public BshClassPath.UnqualifiedNameTable unqNameTable;
  public boolean nameCompletionIncludesUnqNames;
  Vector<WeakReference<ClassPathListener>> listeners;
  static URL[] userClassPathComp;
  static BshClassPath userClassPath;
  static BshClassPath bootClassPath;
  List nameSourceListeners;
  static BshClassPath.MappingFeedback mappingFeedbackListener;

  public BshClassPath(String name) {
    this.nameCompletionIncludesUnqNames = true;
    this.listeners = new Vector();
    this.name = name;
    this.reset();
  }

  public BshClassPath(String name, URL[] urls) {
    this(name);
    this.add(urls);
  }

  public void setPath(URL[] urls) {
    this.reset();
    this.add(urls);
  }

  public void addComponent(BshClassPath bcp) {
    if(this.compPaths == null) {
      this.compPaths = new ArrayList();
    }

    this.compPaths.add(bcp);
    bcp.addListener(this);
  }

  public void add(URL[] urls) {
    this.path.addAll(Arrays.asList(urls));
    if(this.mapsInitialized) {
      this.map(urls);
    }

  }

  public void add(URL url) throws IOException {
    this.path.add(url);
    if(this.mapsInitialized) {
      this.map(url);
    }

  }

  public URL[] getPathComponents() {
    return (URL[])this.getFullPath().toArray(new URL[0]);
  }

  public Set getClassesForPackage(String pack) {
    this.insureInitialized();
    HashSet set = new HashSet();
    Collection c = (Collection)this.packageMap.get(pack);
    if(c != null) {
      set.addAll(c);
    }

    if(this.compPaths != null) {
      for(int i = 0; i < this.compPaths.size(); ++i) {
        Set var5 = ((BshClassPath)this.compPaths.get(i)).getClassesForPackage(pack);
        if(var5 != null) {
          set.addAll(var5);
        }
      }
    }

    return set;
  }

  public BshClassPath.ClassSource getClassSource(String className) {
    BshClassPath.ClassSource cs = (BshClassPath.ClassSource)this.classSource.get(className);
    if(cs != null) {
      return cs;
    } else {
      this.insureInitialized();
      cs = (BshClassPath.ClassSource)this.classSource.get(className);
      if(cs == null && this.compPaths != null) {
        for(int i = 0; i < this.compPaths.size() && cs == null; ++i) {
          cs = ((BshClassPath)this.compPaths.get(i)).getClassSource(className);
        }
      }

      return cs;
    }
  }

  public void setClassSource(String className, BshClassPath.ClassSource cs) {
    this.classSource.put(className, cs);
  }

  public void insureInitialized() {
    this.insureInitialized(true);
  }

  protected void insureInitialized(boolean topPath) {
    if(topPath && !this.mapsInitialized) {
      this.startClassMapping();
    }

    if(this.compPaths != null) {
      for(int i = 0; i < this.compPaths.size(); ++i) {
        ((BshClassPath)this.compPaths.get(i)).insureInitialized(false);
      }
    }

    if(!this.mapsInitialized) {
      this.map((URL[])this.path.toArray(new URL[0]));
    }

    if(topPath && !this.mapsInitialized) {
      this.endClassMapping();
    }

    this.mapsInitialized = true;
  }

  protected List getFullPath() {
    ArrayList list = new ArrayList();
    if(this.compPaths != null) {
      for(int i = 0; i < this.compPaths.size(); ++i) {
        List l = ((BshClassPath)this.compPaths.get(i)).getFullPath();
        Iterator it = l.iterator();

        while(it.hasNext()) {
          Object o = it.next();
          if(!list.contains(o)) {
            list.add(o);
          }
        }
      }
    }

    list.addAll(this.path);
    return list;
  }

  public String getClassNameByUnqName(String name) {
    this.insureInitialized();
    BshClassPath.UnqualifiedNameTable unqNameTable = this.getUnqualifiedNameTable();
    Object obj = unqNameTable.get(name);
    if(obj instanceof BshClassPath.AmbiguousName) {
      List names = ((BshClassPath.AmbiguousName)obj).get();
      return (String)names.get(0);
    } else {
      return (String)obj;
    }
  }

  public BshClassPath.UnqualifiedNameTable getUnqualifiedNameTable() {
    if(this.unqNameTable == null) {
      this.unqNameTable = this.buildUnqualifiedNameTable();
    }

    return this.unqNameTable;
  }

  public BshClassPath.UnqualifiedNameTable buildUnqualifiedNameTable() {
    BshClassPath.UnqualifiedNameTable unqNameTable = new BshClassPath.UnqualifiedNameTable();
    if(this.compPaths != null) {
      for(int it = 0; it < this.compPaths.size(); ++it) {
        Set s = ((BshClassPath)this.compPaths.get(it)).classSource.keySet();
        Iterator it1 = s.iterator();

        while(it1.hasNext()) {
          unqNameTable.add((String)it1.next());
        }
      }
    }

    Iterator var5 = this.classSource.keySet().iterator();

    while(var5.hasNext()) {
      unqNameTable.add((String)var5.next());
    }

    return unqNameTable;
  }

  public String[] getAllNames() {
    this.insureInitialized();
    ArrayList names = new ArrayList();
    Iterator it = this.getPackagesSet().iterator();

    while(it.hasNext()) {
      String pack = (String)it.next();
      names.addAll(removeInnerClassNames(this.getClassesForPackage(pack)));
    }

    if(this.nameCompletionIncludesUnqNames) {
      names.addAll(this.getUnqualifiedNameTable().keySet());
    }

    return (String[])names.toArray(new String[0]);
  }

  void map(URL[] urls) {
    for(int i = 0; i < urls.length; ++i) {
      try {
        this.map(urls[i]);
      } catch (IOException var5) {
        String s = "Error constructing classpath: " + urls[i] + ": " + var5;
        this.errorWhileMapping(s);
      }
    }

  }

  void map(URL url) throws IOException {
    String name = url.getFile();
    File f = new File(name);
    if(f.isDirectory()) {
      this.classMapping("Directory " + f.toString());
      this.map(traverseDirForClasses(f), new BshClassPath.DirClassSource(f));
    } else if(isArchiveFileName(name)) {
      this.classMapping("Archive: " + url);
      this.map(searchJarForClasses(url), new BshClassPath.JarClassSource(url));
    } else {
      String s = "Not a classpath component: " + name;
      this.errorWhileMapping(s);
    }

  }

  public void map(String[] classes, Object source) {
    for(int i = 0; i < classes.length; ++i) {
      this.mapClass(classes[i], source);
    }

  }

  public void mapClass(String className, Object source) {
    String[] sa = splitClassname(className);
    String pack = sa[0];
    String clas = sa[1];
    Object set = (Set)this.packageMap.get(pack);
    if(set == null) {
      set = new HashSet();
      this.packageMap.put(pack, //;
       (Collection<BshClassPath>) (Object) set);
    }

    ((Set)set).add(className);
    BshClassPath.ClassSource obj = (BshClassPath.ClassSource)this.classSource.get(className);
    if(obj == null && source instanceof BshClassPath.ClassSource) {
      this.classSource.put(className, (BshClassPath.ClassSource)source);
    }

  }

  public void reset() {
    this.path = new ArrayList();
    this.compPaths = null;
    this.clearCachedStructures();
  }

  public void clearCachedStructures() {
    this.mapsInitialized = false;
    this.packageMap = new HashMap();
    this.classSource = new HashMap();
    this.unqNameTable = null;
    this.nameSpaceChanged();
  }

  public void classPathChanged() {
    this.clearCachedStructures();
    this.notifyListeners();
  }

  static String[] traverseDirForClasses(File dir) throws IOException {
    List list = traverseDirForClassesAux(dir, dir);
    return (String[])list.toArray(new String[0]);
  }

  static List traverseDirForClassesAux(File topDir, File dir) throws IOException {
    ArrayList list = new ArrayList();
    String top = topDir.getAbsolutePath();
    File[] children = dir.listFiles();

    for(int i = 0; i < children.length; ++i) {
      File child = children[i];
      if(child.isDirectory()) {
        list.addAll(traverseDirForClassesAux(topDir, child));
      } else {
        String name = child.getAbsolutePath();
        if(isClassFileName(name)) {
          if(!name.startsWith(top)) {
            throw new IOException("problem parsing paths");
          }

          name = name.substring(top.length() + 1);
          name = canonicalizeClassName(name);
          list.add(name);
        }
      }
    }

    return list;
  }

  public static Object getfldval(Object o, String pName) {
    return getfldval(o, (Class)null, pName);
  }

  public static Object getfldval(Object o, Class cls, String pName) {
    if(o == null) {
      return "null";
    } else {
      if(cls == null) {
        cls = o.getClass();
      }

      if(cls == null) {
        return "null";
      } else {
        String lf = (new Character('\n')).toString();
        Field[] fields = cls.getDeclaredFields();
        StringBuilder sb = new StringBuilder(75);
        sb.append(" ==  = As " + cls.getName() + " ==  = " + lf);

        for(int superCls = 0; superCls < fields.length; ++superCls) {
          Field field = fields[superCls];
          String name = field.getName();
          if(name.equals(pName)) {
            try {
              field.setAccessible(true);
              boolean e = Modifier.isStatic(field.getModifiers());
              Object val = field.get(e?null:o);
              return val;
            } catch (IllegalAccessException var11) {
              System.err.print(var11.toString());
              return null;
            }
          }
        }

        Class var12 = cls.getSuperclass();
        if(var12 != null && var12 != Object.class) {
          return getfldval(o, var12, pName);
        } else {
          System.err.println(sb.toString());
          return null;
        }
      }
    }
  }

  public static Throwable dumpCause(Throwable e, String what, boolean quiet) {
    if(e == null) {
      return new RuntimeException(String.format("Null exception passed to dumpCause while %s", new Object[]{what}));
    } else {
      Throwable cause = e.getCause();
      if(cause == null || cause.equals(e)) {
        try {
          cause = (Throwable)getfldval(e, "cause");
        } catch (Throwable var5) {
          ;
        }
      }

      if(!quiet) {
        System.err.println(String.format("***: Encountered a problem while %s:", new Object[]{what}));
      }

      if(cause != null && !cause.equals(e)) {
        if(quiet) {
          return cause;
        }

        System.err.println(String.format("Recovered cause of type [%s]:\n\n  Message: >>>  %s  <<<\n\n  Stack:\n\n    %s\n", new Object[]{cause.getClass().getSimpleName(), cause.getMessage(), Arrays.toString(cause.getStackTrace()).replace(", ", "\n  ").replaceAll("^\\[?(.*)\\]?", "$1")}));
        cause.printStackTrace();
      }

      if(!quiet && !e.equals(cause)) {
        System.err.println(String.format("Encountered [%s] while %s:\n\nMessage:  %s\n\n  Stack:\n\n %s", new Object[]{e.getClass().getSimpleName(), what, e.getMessage(), Arrays.toString(e.getStackTrace()).replace(", ", "\n  ").replaceAll("^\\[?(.*)\\]?", "$1")}));
        e.printStackTrace();
      }

      return cause != null?cause:e;
    }
  }

  public static String[] toNiceNames(Object[] names, int count) {
    if(names == null) {
      return new String[0];
    } else {
      int len = names.length;
      if(count < len) {
        len = count;
      }

      String[] classNames = new String[count];
      String entry = null;

      for(int i = 0; i < len; ++i) {
        if(names[i] == null) {
          classNames[i] = "";
        } else {
          entry = names[i].toString().replace('/', '.');
          if(entry.charAt(0) != 91 && entry.length() >= 3) {
            entry = entry.substring(1, entry.length() - 1);
            classNames[i] = entry;
          } else {
            classNames[i] = "";
          }
        }
      }

      return classNames;
    }
  }

  public static String[] getDexStreamClasses(InputStream cdexIs) {
    if(cdexIs == null) {
      return new String[0];
    } else {
      PrintStream out = System.err;
      Dex dex = null;

      try {
        dex = new Dex(cdexIs);
      } catch (Throwable var8) {
        dumpCause(var8, String.format("instantiating new Dex in getDexStreamClasses(InputStream cdexIs) with cdexIs = %s", new Object[]{cdexIs != null?cdexIs.toString():"<null>"}), false);
        return BshClassPath.Strings.EmptyArray;
      }

      Iterator iter = dex.classDefs().iterator();
      Object[] types = dex.typeNames().toArray();
      Object[] names = new Object[types.length];
      int count = 0;
      ClassDef crnt = null;

      while(iter.hasNext()) {
        crnt = (ClassDef)iter.next();
        names[count++] = types[crnt.getTypeIndex()].toString();
        if(count == names.length) {
          break;
        }
      }

      return toNiceNames(names, count);
    }
  }

  public static String[] getDexZipClasses(ZipFile zip) {
    if(zip == null) {
      return new String[0];
    } else {
      try {
        ZipEntry e = zip.getEntry("classes.dex");
        PrintStream out = System.err;
        InputStream dexStream = zip.getInputStream(e);
        return getDexStreamClasses(dexStream);
      } catch (Throwable var5) {
        dumpCause(var5, String.format("Preparing ZIP for stream search: %s", new Object[]{zip != null?zip.toString():"zip = <null>"}), false);
        return new String[0];
      }
    }
  }

  public static String[] getDexClassNames(String filePath) {
    try {
      if(filePath == null || filePath.length() < 4) {
        return new String[0];
      }

      Object e2 = null;

      try {
        new File(filePath);
      } catch (Throwable var4) {
        dumpCause(var4, String.format("opening File: %s", new Object[]{filePath}), false);
        return new String[0];
      }

      String lcExt = filePath.substring(filePath.length() - 4).toLowerCase();
      if(lcExt.equals(".dex")) {
        return getDexStreamClasses(new FileInputStream(new File(filePath)));
      }

      if(lcExt.equals(".jar")) {
        try {
          return getDexZipClasses(new ZipFile(new File(filePath)));
        } catch (Throwable var5) {
          dumpCause(var5, String.format("executing \'getDexZipClasses(new ZipFile(new File(filePath))\' with filePath = %s", new Object[]{filePath != null?filePath:"<null>"}), false);
        }
      }
    } catch (FileNotFoundException var6) {
      dumpCause(var6, String.format("running getDexClassNames(String filePath): File not found [%s]", new Object[]{filePath}), false);
    }

    return new String[0];
  }

  static String[] searchJarForClasses(URL jar) throws IOException {
    Vector v = new Vector();
    InputStream in = jar.openStream();
    ZipInputStream zin = new ZipInputStream(in);

    while(true) {
      ZipEntry ze;
      while((ze = zin.getNextEntry()) != null) {
        String sa = ze.getName();
        if(isClassFileName(sa)) {
          boolean var10000 = Interpreter.DEBUG;
          v.addElement(canonicalizeClassName(sa));
        } else if(sa.equals("classes.dex")) {
          try {
            String[] dexEx = getDexClassNames(jar.getPath());

            for(int i = 0; i < dexEx.length; ++i) {
              if(Interpreter.DEBUG) {
                System.err.println("jar/.dex Class: " + dexEx[i]);
              }

              v.addElement(dexEx[i]);
            }
          } catch (Exception var8) {
            System.err.println("Enumerating dex classes in " + jar.getPath() + " threw exception:\n" + " " + var8.getClass().getName());
            if(var8.getMessage() != null) {
              System.err.println("\nMessage: " + var8.getMessage());
            }

            if(var8.getCause() != null) {
              System.err.println("\nCause: " + var8.getCause().toString());
            }

            if(var8.getSuppressed() != null) {
              System.err.println("\nSuppressed: " + Arrays.toString(var8.getSuppressed()));
            }

            System.err.println(Arrays.toString(var8.getStackTrace()));
          }
        }
      }

      zin.close();
      String[] var9 = new String[v.size()];
      v.copyInto(var9);
      return var9;
    }
  }

  public static boolean isClassFileName(String name) {
    return name.toLowerCase().endsWith(".class");
  }

  public static boolean isArchiveFileName(String name) {
    name = name.toLowerCase();
    return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".dex");
  }

  public static String canonicalizeClassName(String name) {
    String classname = name.replace('/', '.');
    classname = classname.replace('\\', '.');
    if(classname.startsWith("class ")) {
      classname = classname.substring(6);
    }

    if(classname.endsWith(".class")) {
      classname = classname.substring(0, classname.length() - 6);
    }

    return classname;
  }

  public static String[] splitClassname(String classname) {
    classname = canonicalizeClassName(classname);
    int i = classname.lastIndexOf(".");
    String classn;
    String packn;
    if(i == -1) {
      classn = classname;
      packn = "<unpackaged>";
    } else {
      packn = classname.substring(0, i);
      classn = classname.substring(i + 1);
    }

    return new String[]{packn, classn};
  }

  public static Collection removeInnerClassNames(Collection col) {
    ArrayList list = new ArrayList();
    list.addAll(col);
    Iterator it = list.iterator();

    while(it.hasNext()) {
      String name = (String)it.next();
      if(name.indexOf("$") != -1) {
        it.remove();
      }
    }

    return list;
  }

  public static URL[] getUserClassPathComponents() throws ClassPathException {
    if(userClassPathComp != null) {
      return userClassPathComp;
    } else {
      String cp = System.getProperty("java.class.path");
      String[] paths = StringUtil.split(cp, File.pathSeparator);
      URL[] urls = new URL[paths.length];

      try {
        for(int e = 0; e < paths.length; ++e) {
          urls[e] = (new File((new File(paths[e])).getCanonicalPath())).toURL();
        }
      } catch (IOException var4) {
        throw new ClassPathException("can\'t parse class path: " + var4);
      }

      userClassPathComp = urls;
      return urls;
    }
  }

  public Set getPackagesSet() {
    this.insureInitialized();
    HashSet set = new HashSet();
    set.addAll(this.packageMap.keySet());
    if(this.compPaths != null) {
      for(int i = 0; i < this.compPaths.size(); ++i) {
        set.addAll(((BshClassPath)this.compPaths.get(i)).packageMap.keySet());
      }
    }

    return set;
  }

  public void addListener(ClassPathListener l) {
    this.listeners.addElement(new WeakReference(l));
  }

  public void removeListener(ClassPathListener l) {
    this.listeners.removeElement(l);
  }

  void notifyListeners() {
    Enumeration e = this.listeners.elements();

    while(e.hasMoreElements()) {
      WeakReference wr = (WeakReference)e.nextElement();
      ClassPathListener l = (ClassPathListener)wr.get();
      if(l == null) {
        this.listeners.removeElement(wr);
      } else {
        l.classPathChanged();
      }
    }

  }

  public static BshClassPath getUserClassPath() throws ClassPathException {
    if(userClassPath == null) {
      userClassPath = new BshClassPath("User Class Path", getUserClassPathComponents());
    }

    return userClassPath;
  }

  public static BshClassPath getBootClassPath() throws ClassPathException {
    if(bootClassPath == null) {
      try {
        String[] e = System.getenv("BOOTCLASSPATH").split(":");
        URL[] urls = new URL[e.length];

        for(int p = 0; p < e.length; ++p) {
          urls[p] = new URL("file:" + e[p]);
        }

        bootClassPath = new BshClassPath("Boot Class Path", urls);
      } catch (MalformedURLException var3) {
        throw new ClassPathException(" can\'t find boot jar: " + var3);
      }
    }

    return bootClassPath;
  }

  public static String getRTJarPath() {
    Class clz = Class.class;
    URL resUrl = clz.getResource("/java/lang/String.class");
    if(resUrl == null) {
      resUrl = clz.getResource("/classes.dex");
    }

    if(resUrl == null) {
      return null;
    } else {
      String urlString = resUrl.toExternalForm();
      if(!urlString.startsWith("jar:file:")) {
        return null;
      } else {
        int i = urlString.indexOf("!");
        return i == -1?null:urlString.substring("jar:file:".length(), i);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    URL[] urls = new URL[args.length];

    for(int bcp = 0; bcp < args.length; ++bcp) {
      urls[bcp] = (new File(args[bcp])).toURL();
    }

    new BshClassPath("Test", urls);
  }

  public String toString() {
    return "BshClassPath " + this.name + "(" + super.toString() + ") path = " + this.path + "\n" + "compPaths = {" + this.compPaths + " }";
  }

  void nameSpaceChanged() {
    if(this.nameSourceListeners != null) {
      for(int i = 0; i < this.nameSourceListeners.size(); ++i) {
        ((NameSource.Listener)this.nameSourceListeners.get(i)).nameSourceChanged(this);
      }

    }
  }

  public void addNameSourceListener(NameSource.Listener listener) {
    if(this.nameSourceListeners == null) {
      this.nameSourceListeners = new ArrayList();
    }

    this.nameSourceListeners.add(listener);
  }

  public static void addMappingFeedback(BshClassPath.MappingFeedback mf) {
    if(mappingFeedbackListener != null) {
      throw new RuntimeException("Unimplemented: already a listener");
    } else {
      mappingFeedbackListener = mf;
    }
  }

  void startClassMapping() {
    if(mappingFeedbackListener != null) {
      mappingFeedbackListener.startClassMapping();
    } else {
      System.err.println("Start ClassPath Mapping");
    }

  }

  void classMapping(String msg) {
    if(mappingFeedbackListener != null) {
      mappingFeedbackListener.classMapping(msg);
    } else {
      System.err.println("Mapping: " + msg);
    }

  }

  void errorWhileMapping(String s) {
    if(mappingFeedbackListener != null) {
      mappingFeedbackListener.errorWhileMapping(s);
    } else {
      System.err.println(s);
    }

  }

  void endClassMapping() {
    if(mappingFeedbackListener != null) {
      mappingFeedbackListener.endClassMapping();
    } else {
      System.err.println("End ClassPath Mapping");
    }

  }

  public static class AmbiguousName {
    public final BshClassPath.AmbiguousName.NameComparator SORT_BY_PACKAGE_IMPORTANCE = new BshClassPath.AmbiguousName.NameComparator();
    public List list = new ArrayList();

    public void add(String name) {
      this.list.add(name);
    }

    public List get() {
      Collections.sort(this.list, this.SORT_BY_PACKAGE_IMPORTANCE);
      return this.list;
    }

    public class NameComparator implements Comparator<String> {
      public int compare(String a, String b) {
        return a.indexOf("java.") == 0?-10:(b.indexOf("java.") == 0?10:(a.indexOf("libcore.") == 0?-5:(b.indexOf("libcore.") == 0?5:(a.indexOf("android.") == 0?-2:(b.indexOf("android.") == 0?2:(a.length() - b.length()) / (a.length() + b.length()))))));
      }

      public boolean equals(Object o) {
        return o instanceof BshClassPath.AmbiguousName.NameComparator;
      }
    }
  }

  public abstract static class ClassSource {
    Object source;

    abstract byte[] getCode(String var1);
  }

  public static class DirClassSource extends BshClassPath.ClassSource {
    DirClassSource(File dir) {
      this.source = dir;
    }

    public File getDir() {
      return (File)this.source;
    }

    public String toString() {
      return "Dir: " + this.source;
    }

    public byte[] getCode(String className) {
      return readBytesFromFile(this.getDir(), className);
    }

    public static byte[] readBytesFromFile(File base, String className) {
      String n = className.replace('.', '\u0000') + ".class";
      File file = new File(base, n);
      if(file != null && file.exists()) {
        try {
          FileInputStream ie = new FileInputStream(file);
          DataInputStream dis = new DataInputStream(ie);
          byte[] bytes = new byte[(int)file.length()];
          dis.readFully(bytes);
          dis.close();
          return bytes;
        } catch (IOException var7) {
          throw new RuntimeException("Couldn\'t load file: " + file);
        }
      } else {
        return null;
      }
    }
  }

  public static class GeneratedClassSource extends BshClassPath.ClassSource {
    GeneratedClassSource(byte[] bytecode) {
      this.source = bytecode;
    }

    public byte[] getCode(String className) {
      return (byte[])this.source;
    }
  }

  public static class JarClassSource extends BshClassPath.ClassSource {
    JarClassSource(URL url) {
      this.source = url;
    }

    public URL getURL() {
      return (URL)this.source;
    }

    public byte[] getCode(String className) {
      throw new Error("Unimplemented");
    }

    public String toString() {
      return "Jar: " + this.source;
    }
  }

  public interface MappingFeedback {
    void startClassMapping();

    void classMapping(String var1);

    void errorWhileMapping(String var1);

    void endClassMapping();
  }

  public static class Strings {
    static final String Empty = "";
    static final String[] EmptyArray = new String[0];
  }

  static class UnqualifiedNameTable extends HashMap {
    void add(String fullname) {
      String name = BshClassPath.splitClassname(fullname)[1];
      Object have = super.get(name);
      if(have == null) {
        super.put(name, fullname);
      } else if(have instanceof BshClassPath.AmbiguousName) {
        ((BshClassPath.AmbiguousName)have).add(fullname);
      } else {
        BshClassPath.AmbiguousName an = new BshClassPath.AmbiguousName();
        an.add((String)have);
        an.add(fullname);
        super.put(name, an);
      }

    }
  }
}