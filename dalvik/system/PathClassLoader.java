/*
  * Copyright (C) 2007 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package dalvik.system;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.lang.reflect.Method;


/**
  * Provides a simple {@link ClassLoader} implementation that operates on a list
  * of files and directories in the local file system, but does not attempt to
  * load classes from the network. Android uses this class for its system class
  * loader and for its application class loader(s).
  */
public class PathClassLoader extends ClassLoader {
  
  public static Class<PathClassLoader> _getClass() {
    return PathClassLoader.class;
  }
  /**
    Separates complete paths (e.g. $PATH or %PATH%, CLASSPATH,
      etc.).
    ':' on *nix, ';' on Windows
  */
  public static final char PATH_SEP_CHR = File.pathSeparatorChar;
  public static final String PATH_SEP = File.pathSeparator;

  /**
    Separates path components (i.e. (sub-)directories).
    '/' on *nix, '\\' on Windows
  */
  public static final char DIR_SEP_CHR = File.separatorChar;
  public static final String DIR_SEP = File.separator;
  
  public String path;
  public String libPath;
  public List<Throwable> exceptions = new ArrayList<Throwable>();   
  public static final Method loadClassBinaryName;
  static {
    Method lcbn = null;
    try {
      lcbn = DexFile.class.getDeclaredMethod( 
        "loadClassBinaryName", 
        String.class, ClassLoader.class, List.class
      );
      if (!lcbn.isAccessible()) {
        lcbn.setAccessible(true);
      }
    } catch (NoSuchMethodException nsme) {
      nsme.printStackTrace(); 
    //} catch (IllegalAccessException iae) {
    //  iae.printStackTrace(); 
    } catch (Throwable ex) {
      ex.printStackTrace(); 
    } finally {
      loadClassBinaryName = lcbn;
    }
    
  }
  /*
    * Parallel arrays for jar/apk files.
    *
    * (could stuff these into an object and have a single array;
    * improves clarity but adds overhead)
    */
  public String[] mPaths;
  public File[] mFiles;
  public ZipFile[] mZips;
  public DexFile[] mDexs;
  
  

  /**
    * Native library path.
    */
  public List<String> libraryPathElements;

  /**
    * Creates a {@code PathClassLoader} that operates on a given list of files
    * and directories. This method is equivalent to calling
    * {@link #PathClassLoader(String, String, ClassLoader)} with a
    * {@code null} value for the second argument (see description there).
    *
    * @param path
    *      the list of files and directories
    *
    * @param parent
    *      the parent class loader
    */
  public PathClassLoader(String path, ClassLoader parent) {
    this(path, null, parent);
    
    System.err.printf(
      "PathClassLoader.<init>(path = %s, parent = %s)\n",
      path, parent
    );
  }

  /**
    * Creates a {@code PathClassLoader} that operates on two given
    * lists of files and directories. The entries of the first list
    * should be one of the following:
    *
    * <ul>
    * <li>Directories containing classes or resources.
    * <li>JAR/ZIP/APK files, possibly containing a "classes.dex" file.
    * <li>"classes.dex" files.
    * </ul>
    *
    * The entries of the second list should be directories containing
    * native library files. Both lists are separated using the
    * character specified by the "path.separator" system property,
    * which, on Android, defaults to ":".
    *
    * @param path
    *      the list of files and directories containing classes and
    *      resources
    *
    * @param libPath
    *      the list of directories containing native libraries
    *
    * @param parent
    *      the parent class loader
    */
  public PathClassLoader
  (String path, String libPath, ClassLoader parent) 
  {
    super(parent);

    System.err.printf(
      "PathClassLoader.<init>(path = %s, libPath = %s, parent = %s)\n",
      path, libPath, parent
    );
    if (path == null)
      throw new NullPointerException();

    this.path = path;
    this.libPath = libPath;

    mPaths = path.split(":");
    int length = mPaths.length;

    //System.out.println("PathClassLoader: " + mPaths);
    mFiles = new File[length];
    mZips = new ZipFile[length];
    mDexs = new DexFile[length];

    boolean wantDex = true;
    // System.getProperty("android.vm.dexfile", "").equals("true");

    /* open all Zip and DEX files up front */
    for (int i = 0; i < length; i++) {
      //System.out.println("My path is: " + mPaths[i]);
      File pathFile = new File(mPaths[i]);
      mFiles[i ] = pathFile;

      if (pathFile.isFile()) {
        try {
          mZips[i ] = new ZipFile(pathFile);
        }
        catch (IOException ioex) {
          // expecting IOException and ZipException
          //System.out.println("Failed opening '" + pathFile + "': " + ioex);
          //ioex.printStackTrace();
        }
        if (wantDex) {
          /* we need both DEX and Zip, because dex has no resources */
          try {
            mDexs[i ] = new DexFile(pathFile);
          }
          catch (IOException ioex) {
            System.err.printf(
              ">>> Dex is invalid: [%s] <<< (%s)\n",
              mPaths[i],
              ioex.getMessage()
            );
          }
        }
      }
    }

    /*
     * Native libraries may exist in both the system and application library
     * paths, so we use this search order for these paths:
     *   1. This class loader's library path for application libraries
     *   2. The VM's library path from the system property for system libraries
     * This order was reversed prior to Gingerbread; see http://b/2933456
     
     */
    libraryPathElements = new ArrayList<String>();
    
    if (libPath != null) {
      System.err.println("libPath != null");
      
      for (String pathElement: libPath.split(PATH_SEP)) {
        String cleanedPathElem = cleanupPathElement(pathElement);
        System.err.printf(
          "Adding libPath elem (from libPath): %s\n",
           cleanedPathElem
        );
        libraryPathElements.add(cleanedPathElem);
      }
    } else {
      System.err.println("libPath == null");
    }
    
    String systemLibraryPath 
 = System.getProperty("java.library.path", ".");
    
    if (! systemLibraryPath.isEmpty()) {
      
      for (String pathElement: systemLibraryPath.split(PATH_SEP))
      {
        String cleanedPathElem = cleanupPathElement(pathElement);
        System.err.printf(
          "Adding libPath elem (from java.library.path): %s\n",
          cleanedPathElem
        );
        libraryPathElements.add(cleanedPathElem);
      }
    } else {
      System.err.println(
        "systemLibraryPath (java.library.path).isEmpty(): true"
      );
    }
  }
  
  public static Class<?> loadClassBinaryName
  (DexFile self, String nme, ClassLoader ldr, List<Throwable> lt)
  {
    if (loadClassBinaryName == null) {
      throw new RuntimeException(String.format(
        "%s.loadClassBinaryName(): reflected method DexFile.loadClassBinaryName could not be found. Cannot invoke (DexFile = %s, name = %s, ClassLoader = %s, List<Throwable >= %s)",
        _getClass().getSimpleName(),
        self, nme, ldr, lt
      ));
    }
    
    try {
      System.err.printf(
        "%s.loadClassBinaryName(DexFile = %s, name = %s, ClassLoader = %s, List<Throwable >= %s) [invokes DexFile method]\n",
        _getClass().getSimpleName(),
        self, nme, ldr, lt
      );

      Class<?> cls = (Class<?>) loadClassBinaryName.invoke(
        self, nme, ldr, lt
      );
      System.err.printf("Result (Class<?>): %s\n", cls);
      return cls;
    } catch (Throwable ex) { 
      ex.printStackTrace(); 
    }
    return null;
  }

  /**
    * Returns a path element that includes a trailing file separator.
    */
  public String cleanupPathElement(String path) {
    System.err.printf(
      "PathClassLoader.cleanupPathElement(path = %s)\n",
      path
    );
    return path.endsWith(File.separator) ? path : (path + File.separator);
  }

  /**
    * Finds a class. This method is called by {@code loadClass()} after the
    * parent ClassLoader has failed to find a loaded class of the same name.
    *
    * @param name
    *      The "binary name" of the class to search for, in a
    *      human-readable form like "java.lang.String" or
    *      "java.net.URLClassLoader$3$1".
    * @return the {@link Class} object representing the class
    * @throws ClassNotFoundException
    *       if the class cannot be found
    */
  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException
  {
    //System.out.println("PathClassLoader " + this + ": findClass '" + name + "'");
    System.err.printf(
      "PathClassLoader.findClass(name = %s)\n",
      name
    );

    byte[] data = null;
    int length = mPaths.length;

    for (int i = 0; i < length; i++) {
      //System.out.println("My path is: " + mPaths[i]);

      if (mDexs[i] != null) {

        Class<?> clazz = loadClassBinaryName(
          mDexs[i], name, this, exceptions
        );
        
        if (clazz != null)
          return clazz;
      } else if (mZips[i] != null) {
        String fileName = name.replace('.', '/') + ".class";
        data = loadFromArchive(mZips[i], fileName);
      } else {
        File pathFile = mFiles[i];
        if (pathFile.isDirectory()) {
          String fileName = 
            mPaths[i] + "/" + name.replace('.', '/') + ".class";
          data = loadFromDirectory(fileName);
        } else {
          //System.out.println("PathClassLoader: can't find '"
          //  + mPaths[i] + "'");
        }

      }

      /*--this doesn't work in current version of Dalvik--
      if (data != null) {
        System.out.println("--- Found class " + name
          + " in zip[" + i + "] '" + mZips[i].getName() + "'");
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
          String packageName = name.substring(0, dotIndex);
          synchronized (this) {
            Package packageObj = getPackage(packageName);
            if (packageObj == null) {
              definePackage(packageName, null, null,
                  null, null, null, null, null);
            }
          }
        }

        return defineClass(name, data, 0, data.length);
      }
      */
    }

    throw new ClassNotFoundException(name + " in loader " + this);
  }

  /**
    * Finds a resource. This method is called by {@code getResource()} after
    * the parent ClassLoader has failed to find a loaded resource of the same
    * name.
    *
    * @param name
    *      The name of the resource to find
    * @return the location of the resource as a URL, or {@code null} if the
    *     resource is not found.
    */
  @Override
  protected URL findResource(String name) {
    //java.util.logging.Logger.global.severe("findResource: " + name);
    System.err.printf(
      "PathClassLoader.findResource(name = %s)\n",
      name
    );

    int length = mPaths.length;

    for (int i = 0; i < length; i++) {
      URL result = findResource(name, i);
      if(result != null) {
        return result;
      }
    }

    return null;
  }

  /**
    * Finds an enumeration of URLs for the resource with the specified name.
    *
    * @param resName
    *      the name of the resource to find.
    * @return an enumeration of {@code URL} objects for the requested resource.
    */
  @Override
  protected Enumeration<URL> findResources(String resName) {
    System.err.printf(
      "PathClassLoader.findResources(resName = %s)\n",
      resName
    );
    
    int length = mPaths.length;
    ArrayList<URL> results = new ArrayList<URL>();

    for (int i = 0; i < length; i++) {
      URL result = findResource(resName, i);
      if(result != null) {
        results.add(result);
      }
    }
    return new EnumerateListArray<URL>(results);
  }

  public URL findResource(String name, int i) {
    System.err.printf(
      "PathClassLoader.findResource(name = %s, i = %d)\n",
      name, i
    );
    
    File pathFile = mFiles[i];
    ZipFile zip = mZips[i];
    if (zip != null) {
      if (isInArchive(zip, name)) {
        //System.out.println("  found " + name + " in " + pathFile);
        try {
          // File.toURL() is compliant with RFC 1738 in always
          // creating absolute path names. If we construct the
          // URL by concatenating strings, we might end up with
          // illegal URLs for relative names.
          return new URL("jar:" + pathFile.toURL() + "!/" + name);
        }
        catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
    } else if (pathFile.isDirectory()) {
      File dataFile = new File(mPaths[i] + "/" + name);
      if (dataFile.exists()) {
        //System.out.println("  found resource " + name);
        try {
          // Same as archive case regarding URL construction.
          return dataFile.toURL();
        }
        catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
    } else if (pathFile.isFile()) {
    } else {
      System.err.println("PathClassLoader: can't find '"
        + mPaths[i] + "'");
    }
    return null;
  }

  /*
    * Load the contents of a file from a file in a directory.
    *
    * Returns null if the class wasn't found.
    */
  public byte[] loadFromDirectory(String path) {
    System.err.printf(
      "PathClassLoader.loadFromDirectory(path = %s)\n",
      path
    );
    
    RandomAccessFile raf;
    byte[] fileData;

    //System.out.println("Trying to load from " + path);
    try {
      raf = new RandomAccessFile(path, "r");
    }
    catch (FileNotFoundException fnfe) {
      //System.out.println("  Not found: " + path);
      return null;
    }

    try {
      fileData = new byte[(int) raf.length()];
      raf.read(fileData);
      raf.close();
    }
    catch (IOException ioe) {
      System.err.println("Error reading from " + path);
      // swallow it, return null instead
      fileData = null;
    }

    return fileData;
  }

  /*
    * Load a class from a file in an archive.  We currently assume that
    * the file is a Zip archive.
    *
    * Returns null if the class wasn't found.
    */
  public byte[] loadFromArchive(ZipFile zip, String name) {
    System.err.printf(
      "PathClassLoader.loadFromArchive(zip = %s, name = %s)\n",
      tostr(zip), name
    );
    
    ZipEntry entry;

    entry = zip.getEntry(name);
    if (entry == null)
      return null;

    ByteArrayOutputStream byteStream;
    InputStream stream;
    int count;

    /*
     * Copy the data out of the stream.  Because we got the ZipEntry
     * from a ZipFile, the uncompressed size is known, and we can set
     * the initial size of the ByteArrayOutputStream appropriately.
     */
    try {
      stream = zip.getInputStream(entry);
      byteStream = new ByteArrayOutputStream((int) entry.getSize());
      byte[] buf = new byte[4096];
      while ((count = stream.read(buf)) > 0)
        byteStream.write(buf, 0, count);

      stream.close();
    }
    catch (IOException ioex) {
      //System.out.println("Failed extracting '" + archive + "': " +ioex);
      return null;
    }

    //System.out.println("  loaded from Zip");
    return byteStream.toByteArray();
  }

  public static String tostr(ZipFile zf) {
    if (zf == null) return "<null>";
    return String.format(
      "%s[%s]", zf.getClass().getSimpleName(), zf.getName()
    );
  }

  /*
    * Figure out if "name" is a member of "archive".
    */
  public boolean isInArchive(ZipFile zip, String name) {
    System.err.printf(
      "PathClassLoader.isInArchive(zip = %s, name = %s)\n",
      tostr(zip), name
    );
    
    return zip.getEntry(name) != null;
  }

  /**
    * Finds a native library. This class loader first searches its own library
    * path (as specified in the constructor) and then the system library path.
    * In Android 2.2 and earlier, the search order was reversed.
    *
    * @param libname
    *      The name of the library to find
    * @return the complete path of the library, or {@code null} if the library
    *     is not found.
    */
  public String findLibrary(String libname) {
    System.err.printf(
      "PathClassLoader.findLibrary(libname = %s)\n",
      libname
    );
    
    String fileName = System.mapLibraryName(libname);
    for (String pathElement : libraryPathElements) {
      String pathName = pathElement + fileName;
      File test = new File(pathName);

      if (test.exists()) {
        return pathName;
      }
    }

    return null;
  }

  /**
    * Returns package information for the given package. Unfortunately, the
    * PathClassLoader doesn't really have this information, and as a non-secure
    * ClassLoader, it isn't even required to, according to the spec. Yet, we
    * want to provide it, in order to make all those hopeful callers of
    * <code>myClass.getPackage().getName()</code> happy. Thus we construct a
    * Package object the first time it is being requested and fill most of the
    * fields with dummy values. The Package object is then put into the
    * ClassLoader's Package cache, so we see the same one next time. We don't
    * create Package objects for null arguments or for the default package.
    * <p>
    * There a limited chance that we end up with multiple Package objects
    * representing the same package: It can happen when when a package is
    * scattered across different JAR files being loaded by different
    * ClassLoaders. Rather unlikely, and given that this whole thing is more or
    * less a workaround, probably not worth the effort.
    *
    * @param name
    *      the name of the class
    * @return the package information for the class, or {@code null} if there
    *     is not package information available for it
    ;
    */
  @Override
  protected Package getPackage(String name) {
    System.err.printf(
      "PathClassLoader.getPackage(name = %s)\n",
      name
    );
    
    if (name != null && !name.isEmpty()) {
      synchronized(this) {
        Package pack = super.getPackage(name);

        if (pack == null) {
          pack = definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
        }

        return pack;
      }
    }

    return null;
  }

  /*
    * Create an Enumeration for an ArrayList.
    */
  public static class EnumerateListArray<T> implements Enumeration<T> {
    public ArrayList mList;
    public int i = 0;

    EnumerateListArray(ArrayList list) {
      System.err.printf(
        "%s.<init>(list = %s)\n",
        getClass().getSimpleName(),
        list
      );

      mList = list;
    }

    public boolean hasMoreElements() {
      System.err.printf(
        "%s.hasMoreElements()\n",
        getClass().getSimpleName()
      );

      return i < mList.size();
    }

    public T nextElement() {
      System.err.printf(
        "%s.nextElement(list = %s)\n",
        getClass().getSimpleName()
      );

      if (i >= mList.size())
        throw new NoSuchElementException();
      return (T) mList.get(i++);
    }
  };

  public String toString () {
    //System.err.printf(
    //  "%s.toString()\n",
    //  getClass().getSimpleName()
    //);
    
    return String.format(
      "%s( mDexs = %s, libraryPathElements = %s )",
      getClass().getName(),
      mDexs,
      libraryPathElements
    );
  }
}


