package dalvik.system;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import org.d6r.*;
import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import java.net.*;
import com.android.dex.Dex;


class CloseGuard implements Closeable {
  Throwable allocationSite;
  boolean closed;
  
  public CloseGuard(final Throwable allocationSite) {
    this.allocationSite = (allocationSite != null)? allocationSite: new Throwable();
    if (this.allocationSite.getStackTrace().length == 0 && getClass() == null) {
      closed = true;
    }
  }
  
  @Override
  public void close() {
    closed = true;
  }
  
  public void warnIfOpen() {
  }
  
  public static CloseGuard get() {
    return new CloseGuard(new Throwable());
  }
  
}

/**
Manipulates DEX files. The class is similar in principle to
{@link java.util.zip.ZipFile}. It is used primarily by class loaders.
<p>
Note we don't directly open and read the DEX file here. They're
memory-mapped
read-only by the VM.
*/
public final class DexFile {
  
  static final Map<Integer, DexFile> cookieJar = new TreeMap<>();
  static Method ClassLoader_loadClass;
  static Method ClassLoader_defineClass;
  static {
    try {
      (ClassLoader_loadClass = ClassLoader.class.getDeclaredMethod(
        "loadClass", String.class, Boolean.TYPE
      )).setAccessible(true);
      (ClassLoader_defineClass = ClassLoader.class.getDeclaredMethod(
        "defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE
      )).setAccessible(true);
    } catch (ReflectiveOperationException roe) {
      roe.printStackTrace();
      throw new AssertionError(
        "DexFile: Could not resolve ClassLoader's loadClass(..) " +
        "and/or defineClass(..)"
        , roe
      );
    }
  }
  
  
  private int mCookie;
  private final String mFileName;
  private final CloseGuard guard = CloseGuard.get();
  String[] classNames;
  byte[] magic;
  byte[] dexBytes;
  Dex dex;
  boolean isZip;
  boolean invalid;
  Throwable exception;
  


  
  /**
  Opens a DEX file from a given File object. This will usually be a
  ZIP/JAR
  file with a "classes.dex" inside.
  The VM will generate the name of the corresponding file in
  /data/dalvik-cache and open it, possibly creating or updating
  it first if system permissions allow. Don't pass in the name of
  a file in /data/dalvik-cache, as the named file is expected to be
  in its original (pre-dexopt) state.
  @param file
  the File object referencing the actual DEX file
  @throws IOException
  if an I/O error occurs, such as the file not being found or
  access rights missing for opening it
  */
  public DexFile(final File file) throws IOException {
    this(file.getAbsolutePath());
  }
  
  /**
  Opens a DEX file from a given filename. This will usually be a ZIP/JAR
  file with a "classes.dex" inside.
  The VM will generate the name of the corresponding file in
  /data/dalvik-cache and open it, possibly creating or updating
  it first if system permissions allow. Don't pass in the name of
  a file in /data/dalvik-cache, as the named file is expected to be
  in its original (pre-dexopt) state.
  @param fileName
  the filename of the DEX file
  @throws IOException
  if an I/O error occurs, such as the file not being found or
  access rights missing for opening it
  */
  public DexFile(final String fileName) throws IOException {
    mCookie = openDexFile(fileName, null, 0);
    cookieJar.put(mCookie, this);
    mFileName = fileName;
  }
  
  /**
  Opens a DEX file from a given filename, using a specified file
  to hold the optimized data.
  @param sourceName
  Jar or APK file with "classes.dex".
  @param outputName
  File that will hold the optimized form of the DEX data.
  @param flags
  Enable optional features.
  */
  private DexFile(final String sourceName, final String outputName, final int flags)
    throws IOException
  {
    mCookie = openDexFile(sourceName, outputName, flags); // *
    cookieJar.put(mCookie, this);
    mFileName = sourceName;
  }
  
  private DexFile(final String sourceName, final String outputName, final int flags,
    final int cookie)
  {
    mCookie = cookie;
    cookieJar.put(mCookie, this);
    mFileName = sourceName;
  }
  
  /**
  Open a DEX file, specifying the file in which the optimized DEX
  data should be written. If the optimized form exists and appears
  to be current, it will be used; if not, the VM will attempt to
  regenerate it.
  This is intended for use by applications that wish to download
  and execute DEX files outside the usual application installation
  mechanism. This function should not be called directly by an
  application; instead, use a class loader such as
  dalvik.system.DexClassLoader.
  @param sourcePathName
  Jar or APK file with "classes.dex". (May expand this to include
  "raw DEX" in the future.)
  @param outputPathName
  File that will hold the optimized form of the DEX data.
  @param flags
  Enable optional features. (Currently none defined.)
  @return
  A new or previously-opened DexFile.
  @throws IOException
  If unable to open the source or output file.
  */
  public static DexFile loadDex(final String sourcePathName,
    final String outputPathName, final int flags) throws IOException
  {
    /**
    TODO: we may want to cache previously-opened DexFile objects.
    The cache would be synchronized with close(). This would help
    us avoid mapping the same DEX more than once when an app
    decided to open it multiple times. In practice this may not
    be a real issue.
    */
    return new DexFile(sourcePathName, outputPathName, flags);
  }

  /**
  Gets the name of the (already opened) DEX file.
  @return the file name
  */
  public String getName() {
    return mFileName;
  }

  /**
  Closes the DEX file.
  <p>
  This may not be able to release any resources. If classes from this
  DEX file are still resident, the DEX file can't be unmapped.
  @throws IOException
  if an I/O error occurs during closing the file, which
  normally should not happen
  */
  public void close() {
    guard.close();
    // closeDexFile(mCookie);
    mCookie = 0;
  }

  /**
  Loads a class. Returns the class on success, or a {@code null}
  reference
  on failure.
  <p>
  If you are not calling this from a class loader, this is most likely
  not
  going to do what you want. Use {@link Class#forName(final String)} instead.
  <p>
  The method does not throw {@link ClassNotFoundException} if the class
  isn't found because it isn't reasonable to throw exceptions wildly
  every
  time a class is not found in the first DEX file we look at.
  @param name
  the class name, which should look like "java/lang/String"
  @param loader
  the class loader that tries to load the class (in most cases
  the caller of the method
  @return the {@link Class} object representing the class, or {@code
  null}
  if the class cannot be loaded
  */
  public Class<?> loadClass(final String name, final ClassLoader loader,
    final List<Throwable> suppressed) 
  {
    ClassNotFoundException cnfe = null;
    try {
      try {
        try {
          Class<?> c = (Class<?>) ClassLoader_loadClass.invoke(loader, name, false);
          return c;
        } catch (InvocationTargetException ite) {
          throw ite.getTargetException();
        }
      } catch (ClassNotFoundException e) {
        cnfe = e;
      } catch (Throwable t) {
        throw (UnknownError) new UnknownError().initCause(t);
      }
      
      final byte[] clsBytes
        = Dex2Java.convertOne(new Dex(new File(mFileName)).getBytes(), name);
      final Class<?> c2 = (Class<?>) ((clsBytes != null)
        ? ClassLoader_defineClass.invoke(loader, name, clsBytes, 0, clsBytes.length)
        : null);
      if (c2 != null) return c2;
      if (suppressed != null && cnfe != null) {
        suppressed.add(cnfe);
        return null;
      }
      throw cnfe;
    } catch (final ReflectiveOperationException
                | IOException
                | LinkageError
                | TypeNotPresentException e)
    {
      if (suppressed != null) {
        if (e != cnfe) suppressed.add(e);
        return null;
      }
      throw new RuntimeException(String.format(
        "DexFile@%08x(mCookie: %d, mFileName: '%s'): " +
        "loadClass with name: '%s', loader: %s@%08x failed: %s",
        System.identityHashCode(this), mCookie, mFileName,
        name, (loader != null) ? loader.getClass().getName() : null,
        System.identityHashCode(loader), e
      ), e);
    }
  }
  
  public Class<?> loadClass(final String name, final ClassLoader loader) {
    return loadClass(name, loader, (List<Throwable>) null);
  }
  
  /**
  See {@link #loadClass(final String, ClassLoader)}.
  This takes a "binary" class name to better match ClassLoader semantics.
  @hide
  */
  public Class<?> loadClassBinaryName(final String name, ClassLoader loader) {
    // return defineClass(name, loader, mCookie);
    return loadClass(name.replace('/', '.'), loader);
  }

  private static Class<?> defineClass(final String name, ClassLoader loader,
    final int cookie)
  {
    return cookieJar.get(cookie).loadClass(name, loader);
  }
  /**
  Enumerate the names of the classes in this DEX file.
  @return an enumeration of names of classes contained in the DEX file,
  in
  the usual internal form (like "java/lang/String").
  */
  public Enumeration<String> entries() {
    return new DFEnum(this);
  }

  /**
  Helper class.
  */
  private class DFEnum implements Enumeration<String> {

    private int mIndex;

    private String[] mNameList;

    DFEnum(final DexFile df) {
      mIndex = 0;
      mNameList = getClassNameList(mCookie);
    }

    public boolean hasMoreElements() {
      return (mIndex < mNameList.length);
    }

    public String nextElement() {
      return mNameList[mIndex++];
    }
  }

  /**
  return a String array with class names
  */
  private static String[] getClassNameList(final int cookie) {
    final DexFile dexFile = cookieJar.get(cookie);
    return dexFile.getClassNames();
  }
  
  
  private String[] getClassNames() {
    if (this.classNames == null) {
      final Dex dex = readDex();
      if (invalid) return (this.classNames = new String[0]);
      
      final int classDefCount = dex.getTableOfContents().classDefs.size;
      final String[] classNames = new String[classDefCount];
      this.classNames = classNames;
      final String[] strs = dex.strings().toArray(new String[0]);
      for (int classDefIndex=0; classDefIndex<classDefCount; ++classDefIndex) {
        final int typeIndex = dex.typeIndexFromClassDefIndex(classDefIndex);
        final int typeId = dex.typeIds().get(typeIndex).intValue();
        final String binaryName = strs[typeId];
        final int len = binaryName.length();
        final String className 
          = ((String) binaryName.subSequence(1, len-1)).replace('/', '.');
        classNames[classDefIndex] = className;
      }
    }
    return this.classNames;
  }
  
  /**
  Called when the class is finalized. Makes sure the DEX file is closed.
  @throws IOException
  if an I/O error occurs during closing the file, which
  normally should not happen
  */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (guard != null) {
        guard.warnIfOpen();
      }
      close();
    } finally {
      // super.finalize();
    }
  }
  
  /**
  Open a DEX file. The value returned is a magic VM cookie. On
  failure, an IOException is thrown.
  */
  private static int openDexFile(final String sourceName, final String outputName, 
    final int flags) throws IOException
  {
    final int cookie = System.identityHashCode(sourceName);
    if (cookieJar.containsKey(cookie)) return cookie;
    
    final DexFile dexFile = new DexFile(sourceName, outputName, flags, cookie);
    cookieJar.put(cookie, dexFile);
    
    dexFile.readDex();
    return cookie; // *
  }
  
  
  
  private Dex readDex() {
    if (this.dex != null) return this.dex;
    final File file = new File(mFileName);
    try (final FileInputStream fis = new FileInputStream(file);
         final BufferedInputStream bis = new BufferedInputStream(fis))
    {
      bis.mark(5);
      final byte[] magic = new byte[4];
      this.magic = magic;
      bis.read(magic);
      bis.reset();
      final boolean isZip =
        (magic[0] == 'P' && magic[1] == 'K' && magic[2] == 3 && magic[3] == 4);
      this.isZip = isZip;
      final ZipFile zipFile = (isZip) ? new ZipFile(file) : null;
      try {
        final ZipEntry dexEntry = (zipFile != null)
          ? zipFile.getEntry("classes.dex")
          : null;
        try (final InputStream dexIn
               = (dexEntry != null) ? zipFile.getInputStream(dexEntry) : bis)
        {
          final int dexLen
            = (dexEntry != null) ? (int) dexEntry.getSize() : (int) file.length();
          final byte[] dexBytes = new byte[dexLen];
          int read, ttl = 0;
          while ((ttl += (read = dexIn.read(dexBytes, ttl, dexLen-ttl))) < dexLen);
          this.dexBytes = dexBytes;
          this.dex = new Dex(dexBytes);
          this.isZip = isZip;
          return this.dex;
        }
      } finally {
        if (zipFile != null) zipFile.close();
      }
    } catch (final IOException ioe) {
      ioe.printStackTrace();
      invalid = true;
      exception = ioe;
      return null;
    }
  }
  
  /**
  Open a DEX file based on a {@code byte[]}. The value returned
  is a magic VM cookie. On failure, a RuntimeException is thrown.
  */
  private static int openDexFile(byte[] fileContents) {
    throw new UnsupportedOperationException("memory dex file");
  }
  
  /**
  Close DEX file.
  */
  private static void closeDexFile(int cookie) {
    cookieJar.get(cookie).close();
  }
  /**
  Returns true if the VM believes that the apk/jar file is out of date
  and should be passed through "dexopt" again.
  @param fileName the absolute path to the apk/jar file to examine.
  @return true if dexopt should be called on the file, false otherwise.
  @throws java.io.FileNotFoundException if fileName is not readable,
  not a file, or not present.
  @throws java.io.IOException if fileName is not a valid apk/jar file or
  if problems occur while parsing it.
  @throws java.lang.NullPointerException if fileName is null.
  @throws dalvik.system.StaleDexCacheError if the optimized dex file
  is stale but exists on a read-only partition.
  */
  
  private static Class<?> defineClass(final String name,
    ClassLoader loader, final int cookie, List<Throwable> suppressed)
  {
    return cookieJar.get(cookie).loadClass(name, loader, suppressed);
  }
  
  
  private static Class<?> defineClassNative(final String name,
    ClassLoader loader, final int cookie) throws ClassNotFoundException
  {
    return cookieJar.get(cookie).loadClass(name, loader);
  }
  
  
  private static int openDexFileNative(final String inputFile,
    String optimizedOutputFile, final int cookie) throws IOException
  {
    throw new IOException("Not Implemented");
  }
  
  
  public Class<?> loadClassBinaryName(final String name,
    ClassLoader loader, List<Throwable> suppressed)
  {
    return loadClass(name.replace('/', '.'), loader, suppressed);
  }
  
}


