package org.d6r;

import bsh.operators.Extension;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import bsh.Capabilities;
import bsh.CallStack;
import bsh.ClassIdentifier;
import bsh.Interpreter;
import com.android.dex.Dex;
import com.android.dex.ClassDef;
import org.apache.commons.io.filefilter.RegexFileFilter;
import com.google.common.base.Functions;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.concurrent.Callable;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.ir.attributes.SourceFileAttribute;
import com.strobel.core.CollectionUtilities;
import javax.annotation.Nullable;
import java.io.*;
import org.d6r.IOStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.nio.charset.StandardCharsets;

class findSource {
  public static URL invoke(Interpreter i, CallStack t,
  ClassIdentifier ci) {
    return SourceUtil.findSource(ci);
  }
  
  public static URL invoke(Interpreter i, CallStack t, Class<?> cls) {
    return SourceUtil.findSource(cls);
  }
  
  public static URL invoke(Interpreter i, CallStack t, 
  String className) {
    return SourceUtil.findSource(className);
  }
}

class getSource {
  public static String invoke(Interpreter i, CallStack t, Object obj) {
    return SourceUtil.getSource(obj);
  }
  
  public static String invoke(Interpreter i, CallStack t, String clsName) {
    return SourceUtil.getSource((String) clsName);
  }
}


public class SourceUtil {
  
  protected static File[] allDirs;
  public static boolean GLOB_DEBUG = false;
  public static boolean VERBOSE = false;
  
  
  public static boolean DEFAULT_FILESYSTEM_FIRST = false;
  static final String TAG = "SourceUtil";
  static final String TAG_FS = "SourceUtil.findSource";
  
  public static URL findSource(ClassIdentifier ci) {
    if (ci == null) throw new IllegalArgumentException("ci == null");
    final Class<?> cls = (Class<?>) ((ClassIdentifier) ci).getTargetClass();
    return findSource(cls);
  }
  
  
  public static URL findSource(Class<?> cls) {
    if (cls == null) throw new IllegalArgumentException("cls == null");
    return findSource(cls.getName()); 
  }
  
    
  public static String getClassSourceFileName(Class<?> cls) {
    if (cls == null) throw new IllegalArgumentException("cls == null");
    final String className = cls.getName();
    if (className.startsWith("sun.reflect.Generated")) return null;
    String fileName = null;
    try {
      Dex dex = getDex(cls);
      String desc = String.format("L%s;", cls.getName().replace('.', '/'));
      int typeIndex = dex.findTypeIndex(desc);
      int classDefIndex = dex.findClassDefIndexFromTypeIndex(typeIndex); 
      ClassDef classDef = SourceUtil.getClassDef(dex, classDefIndex);
      int cdIdx = classDefIndex;
      int sourceFileStringIndex = classDef.getSourceFileIndex();
      if (sourceFileStringIndex >= 0) {
        fileName = dex.strings().get(sourceFileStringIndex);
        if (fileName != null && (
          StringUtils.endsWith(fileName, ".java") ||
          StringUtils.endsWith(fileName, ".scala") ||
          StringUtils.endsWith(fileName, ".kt") ||
          StringUtils.endsWith(fileName, ".groovy")))
        {
          if (VERBOSE && Log.isLoggable(Log.SEV_DEBUG)) {
            String simpleFileName = guessSourceFileName(className);
            if (simpleFileName != null && !simpleFileName.equals(fileName)) {
              Log.d(TAG, "class '%s' compiled from '%s'\n", className, fileName);
            }
          }
          return fileName;
        }
      }
    } catch (final Exception t) {
      Log.w(TAG, t);
    }
    if (fileName != null) return fileName;
    return guessSourceFileName(className);
  }
  
  public static String guessSourceFileName(String className) {
    if (className == null) throw new IllegalArgumentException("className == null");
    if (className.startsWith("sun.reflect.Generated")) return null;
    int lastDotPos = className.lastIndexOf('.');
    String pkgPart = (lastDotPos != -1)
      ? className.substring(0, lastDotPos)
      : "";
    String pkgPartSeparator = (lastDotPos != -1)
      ? className.substring(lastDotPos, lastDotPos + 1)
      : "";
    String compilationUnitPart = (lastDotPos != -1)
      ? className.substring(lastDotPos + 1)
      : className;
    int firstDollarPosInCuPart = compilationUnitPart.indexOf('$');
    if (firstDollarPosInCuPart == -1) {
      return isLegalIdentifier(compilationUnitPart)
        ? compilationUnitPart.concat(".java")
        : null;
    }
    // likely nested
    if (firstDollarPosInCuPart == 0) {
      // '$' is probably part of actual declared class name
      // Not making any more assumptions based on name
      return compilationUnitPart.concat(".java");
    }
    final String ident = compilationUnitPart.substring(0, firstDollarPosInCuPart);
    return isLegalIdentifier(ident)? ident.concat(".java"): null;
  }
  
  public static boolean isLegalIdentifier(final CharSequence name) {
    if (name == null) throw new IllegalArgumentException("name == null");
    final String id = CharSequenceUtil.toString(name);
    final int len = id.length();
    int cp = id.codePointAt(0);
    if (!Character.isJavaIdentifierStart(cp)) return false;
    for (int pos=Character.charCount(cp); pos<len; pos+=Character.charCount(cp)) {
      cp = id.codePointAt(pos);
      if (!Character.isJavaIdentifierPart(cp)) return false;
    }
    return true;
  }
  
  public static ClassDef getClassDef(Dex dex, int cdIdx) {
    if (dex == null) throw new IllegalArgumentException("dex == null");
    Dex.Section s = dex.open(dex.getTableOfContents().classDefs.off);
    int pos0 = s.getPosition();  
    ClassDef def, def0 = s.readClassDef();
    int pos = s.getPosition(), itemSize = pos - pos0;
    if (cdIdx == 0) return def0;
    if (cdIdx > 1) s.skip((cdIdx - 1) * itemSize);  
    def = s.readClassDef();
    return def;
  }

  
  static class SourceNotFoundException extends RuntimeExceptionCompat {
    public SourceNotFoundException(String message) {
      super(message);
    }
    
    public SourceNotFoundException(String message, Throwable cause) {
      super(message, cause);
    } 
  }
  
  
  public static String getSource(Object object) {
    if (object == null) throw new IllegalArgumentException("object == null");
    if (object instanceof CharSequence) {
      return getSource((String) CharSequenceUtil.toString((CharSequence) object));
    }
    if (object == null) throw new IllegalArgumentException("object == null");
    final Class<?> cls = dumpMembers.getClass(object); 
    if (cls == null) throw new IllegalArgumentException(String.format(
      "Cannot determine class which corresponds to object of type %s: %s",
      ClassInfo.typeToName(object.getClass().getName()),
      Dumper.tryToString(object)
    ));
    try {
      return getSource(cls.getName());
    } catch (Exception e) {
      new SourceNotFoundException(cls.getName(), e).printStackTrace();
    }
    return null;
  }
  
  public static String getSource(String clsName) {
    if (clsName == null) throw new IllegalArgumentException("clsName == null");
    final URL sourceUrl = findSource(clsName);
    if (sourceUrl == null) return null;
    return TextUtil.readAllText(sourceUrl);
  }
  
  public static URL findSource(Object object) {
    if (object == null) throw new IllegalArgumentException("object == null");
    final Class<?> cls = dumpMembers.getClass(object); 
    if (cls == null) throw new IllegalArgumentException(String.format(
      "Cannot determine class which corresponds to object of type %s: %s",
      ClassInfo.typeToName(object.getClass().getName()),
      Dumper.tryToString(object)
    ));
    return findSource(cls.getName());
  }
  
  public static URL findSource(final String clsName) {
    if (clsName == null) throw new IllegalArgumentException("clsName == null");
    return findSource(clsName,  DEFAULT_FILESYSTEM_FIRST);
  }
  
  public static URL findSource(final String clsName, boolean fileSystemFirst) {
    if (clsName == null) throw new IllegalArgumentException("clsName == null");
    if (clsName.startsWith("sun.reflect.Generated")) return null;
    List<URL> retUrls = findSource(clsName, fileSystemFirst, 1);
    if (retUrls.isEmpty()) return null;
    return retUrls.iterator().next();
  }
  
  public static List<URL> findSource(final String clsName, boolean fileSystemFirst,
    int maxCount)
  {
    if (clsName == null) throw new IllegalArgumentException("clsName == null");
    return findSource(clsName, fileSystemFirst, maxCount, -1);
  }
  
  public static List<URL> findSource(final String clsName, boolean fileSystemFirst,
    int maxCount, int lineNumber)
  {
    if (clsName == null) throw new IllegalArgumentException("clsName == null");
    if (clsName.startsWith("sun.reflect.Generated")) return Collections.emptyList();
    List<URL> retUrls = new ArrayList<>();
    
    if (maxCount == 0) return retUrls;
    if (VERBOSE && Log.isLoggable(Log.SEV_VERBOSE)) {
      Log.v(TAG, "findSource(clsName: \"%s\", fileSystemFirst: %s)",
      clsName, fileSystemFirst);
    }
    final String srcResPath = ClassInfo.classNameToPath(
      StringUtils.substringBefore(clsName, "$"), "java"
    );
    if (VERBOSE && Log.isLoggable(Log.SEV_VERBOSE)) {
      Log.v(TAG_FS, "srcResPath := \"%s\"", srcResPath);
    }
    
    final ClassLoader clsLoader = Thread.currentThread().getContextClassLoader();
    final Deque<Throwable> suppressed = new ArrayDeque<>();

    final Class<?> cls = DexVisitor.classForName(clsName);
    final boolean clsExists = (cls != null);
    
    final ClassLoader ldr = cls != null? cls.getClassLoader(): null;
    //final boolean isBoot
    //= (ldr != null && ldr.getClass().getName().indexOf("BootClassLoader") != -1);
    //if (!isBoot) {
      final Object dexElement = (clsExists)? DexFinder.findDexElement(cls): null;
      if (Log.isLoggable(Log.SEV_VERBOSE)) {
        Log.v(TAG_FS, "dexElement := %s", dexElement);
      }
      final URL srcResUrl = (dexElement != null)
        ? Reflector.invoke(dexElement, "findResource", srcResPath)
        : null;
      
      final String tgtfn;
        try {
          tgtfn = (clsExists)
            ? cls.getClassLoader() != null
                ? getClassSourceFileName(cls)
                : String.format(
                    "%s.java",
                    ClassInfo.getSimpleName(ClassInfo.typeToName(clsName))
                  )
            : String.format(
                "%s.java",
                ClassInfo.getSimpleName(ClassInfo.typeToName(clsName))
              );
          if (tgtfn != null) {
            // "com.example"
            final String pkg = clsName
              .replaceAll("^(.*)\\.([^.$]+)(?:\\$.*)?$", "$1");
            // "com/example/RealSourceFile.java"
            final String tgtResPath = pkg.concat(".")
              .concat(StringUtils.substringBefore(tgtfn, ".java"))
              .replace('.', '/')
              .concat(".java");
            final URL tgtResUrl;
            if (dexElement != null) {
              tgtResUrl = Reflector.invoke(dexElement, "findResource", tgtResPath);
              if (VERBOSE && Log.isLoggable(Log.SEV_VERBOSE)) {
                Log.v(TAG_FS, "tgtResUrl := %s", tgtResUrl);
              }
              if (tgtResUrl != null) {
                retUrls.add(tgtResUrl);
                if (maxCount <= retUrls.size()) return retUrls;
              }
            } else {
              final URL tgtResUrl_CL, tgtResUrl_Ex;
              if (fileSystemFirst) {
                retUrls.addAll(findSourceEx(tgtResPath));
                if (maxCount <= retUrls.size()) return retUrls;
                // secondary
                tgtResUrl_CL = getResource(clsLoader, tgtResPath);
                if (VERBOSE && Log.isLoggable(Log.SEV_VERBOSE)) {
                  Log.v(TAG_FS, "tgtResUrl_CL := %s", tgtResUrl_CL);
                }
                if (exists(tgtResUrl_CL)) {
                  retUrls.add(tgtResUrl_CL);
                  if (maxCount <= retUrls.size()) return retUrls;
                }
              } else {
                tgtResUrl = getResource(clsLoader, tgtResPath);
                if (tgtResUrl != null) {
                  if (exists(tgtResUrl)) {
                    retUrls.add(tgtResUrl);
                    if (maxCount <= retUrls.size()) return retUrls;
                  }
                }
                // secondary
                retUrls.addAll(findSourceEx(tgtResPath));
                if (maxCount <= retUrls.size()) return retUrls;
              }
            }
          }
  
        } catch (final Exception e) {
          if (suppressed.isEmpty()) e.printStackTrace();
          suppressed.offerLast(e);
          e.printStackTrace();
        }
      //}
    // }
    // fallback
    retUrls.addAll(findSourceEx(srcResPath));
    if (retUrls.size() > 0) return retUrls;
    try {
      final URL lastResortUrl = lastResortFindSource(clsName);
      if (lastResortUrl != null) {
        retUrls.add(lastResortUrl);
        Log.i(TAG, String.format(
          "Found source using last resort method: '%s' --> [%s]",
          clsName, lastResortUrl));
        return retUrls;
      }
    } catch (final IOException ioex) {
      if (suppressed.isEmpty()) ioex.printStackTrace();
      suppressed.offerLast(ioex);
      ioex.printStackTrace();
    }
    // ::sigh::
    final SourceNotFoundException snfe = new SourceNotFoundException(clsName);
    while (! suppressed.isEmpty()) {
      final Throwable e = suppressed.pollFirst();
      if (snfe.getCause() == null || snfe.getCause() == snfe) {
        snfe.initCause(e);
      } else snfe.addSuppressed(e);
    }
    snfe.printStackTrace();
    return Collections.emptyList();
  }
  
  
  public static List<URL> findSourceEx(String srcResPath) {
    if (srcResPath == null) {
      throw new IllegalArgumentException("srcResPath == null");
    }
    if (srcResPath.indexOf("sun/reflect/Generated") != -1) {
      return Collections.emptyList();
    }
    final List<URL> urls = new ArrayList<URL>();
    try {
      final File[] allDirs = getDirectorySet();
      for (int i=0, len=allDirs.length; i<len; ++i) {
        final File srcBaseDir = allDirs[i];
        if (srcBaseDir == null || ! srcBaseDir.exists()) continue;
        final File srcFile = new File(srcBaseDir, srcResPath);
        if (! srcFile.exists()) continue;      
        
        final URL srcUrl = srcFile.getAbsoluteFile().getCanonicalFile().toURL();
        if (exists(srcUrl)) urls.add(srcUrl);
      }
      return urls;
    } catch (Exception e) {
      e.printStackTrace();
      return urls;
    }
  }
  

  static final Pattern dirPtrn
    = Pattern.compile("^.*?[\"']*([^\"' ]+[^ \"']*/[^ '\"]+[^ \"' ]*)[\"']*.*?$");
  
  public static File[] getDirectorySet() {
    if (allDirs != null) return allDirs;
    
    if (VERBOSE) Log.d(TAG, "Building directory set ...");
    final List<File> dirs = new ArrayList<File>();
    final List<String> rawDirs;
    try {
      rawDirs = CollectionUtil2.filter(
        (Collection<String>) (Collection<?>) CollectionUtil.flatten(
          StringCollectionUtil.matchLines(
            FileUtils.readFileToString(new File("/system/bin/findsrc")),
            dirPtrn.pattern()
          )
        ),
        Pattern.compile("^/[^$#]+$").matcher("")
       );
       for (final String dirString: rawDirs) {
         if (dirString.indexOf('*') == -1) {
           final File dir = new File(dirString);
           if (dir.exists() && dir.isDirectory()) {
             dirs.add(dir);
           };
           continue; 
         };
         final List<String> expanded = SourceUtil.expandDirGlob(dirString);
         for (final String expandedPath: expanded) {
           // already validated (exists() && isDirectory() via expandDirGlob)
           dirs.add(new File(expandedPath));
         };
      };
      allDirs = dirs.toArray(new File[0]);
      if (VERBOSE) Log.d(TAG,
        "Building directory set complete. Directories: %d", dirs.size()
      );
      return allDirs;
    } catch (final IOException ioe) {
      printStackTrace(ioe);
      return allDirs;
    }
  }
  
  static final Map<String, Boolean> existsCache = new SoftHashMap<>();

  
  public static boolean exists(URL url, boolean acceptMissingEntryExc) {
    if (url == null) return false;
    if (url.toString().indexOf("sun/reflect/Generated") != -1) {
      return false;     
    }
    if (VERBOSE) Log.d(
      TAG, "exists(URL(\"%s\"), acceptMissingEntryExc: %s)",
      url, acceptMissingEntryExc
    );
    if (url == null) return false;
    try {
      final String urlString = url.toString();
      final Boolean ans = existsCache.get(urlString);
      if (ans != null) return ans.booleanValue();
      
      final PathInfo pi = PathInfo.getPathInfo(urlString);
      final String rest = (urlString.indexOf("!/") != -1)
        ? StringUtils.substringAfter(urlString, "!")
        : null;
      final Path path = (rest != null)
        ? Paths.get(pi.dir, "src", rest)
        : Paths.get(pi.dir, pi.name);      
      
      final boolean fileResult = path.toFile().exists();
      if (fileResult) {
        existsCache.put(urlString, fileResult);
        return fileResult;
      }
      
      final boolean fileResult2 = (rest != null)
        ? Paths.get(pi.dir, pi.name).toFile().exists()
        : false;
      existsCache.put(urlString, fileResult2);

        
      // String decodedFile = UriCodec.decode(jarFileURL.getFile());
      // return new JarFile(new File(decodedFile), true, ZipFile.OPEN_READ);  
      return fileResult2;
    } catch (final Exception e) { 
      Log.wtf(TAG, e);
      e.printStackTrace();
      return false;
    }
  }
  
  
  @Extension
  public static boolean exists(URL url) {
    return exists(url, true);
  }
  
  
  

  public static List<String> expandDirGlob(String path) {
    if (path == null) throw new IllegalArgumentException("path == null");
    try {
      String[] parts = StringUtils.split(path, "/");
      Deque<String> q = new ArrayDeque<String>();
      q.offer("/");
      StringBuilder pb = new StringBuilder();
      String part = null, prefix = null;
      File d = null, dir = null;
      File[] dirs = null;
      for (int i = 0; i < parts.length; i++) {
        part = parts[i];
        int sz = q.size();
        int s = -1;
        while (++s < sz) {
          prefix = q.pollFirst();
          if ((d = new File(prefix, part)).exists() 
          &&   d.isDirectory())       
          {
            q.offerLast(d.getPath());
          }
          if (part.equals("*")) {
            dirs = new File(prefix).listFiles();
            for (int j = 0; j < dirs.length; j++) {
              dir = dirs[j];
              if (dir.isDirectory()) {
                q.offerLast(dir.getPath());
              }
            }
          }
        }
      }
      return new ArrayList<String>(q);
    } catch (Exception e) { 
      printStackTrace(e);
      return new ArrayList<String>();
    }
  }
  
  public static List<String> expandGlob(String path) {
    if (path == null) throw new IllegalArgumentException("path == null");
    try {
      List<String> results = new ArrayList<String>(); 
      List<String> dirs = expandDirGlob(path); 
      if (dirs.size() > 0) return dirs;
      
      String parentPath = StringUtils.substringBeforeLast(path, "/");
      dirs = expandDirGlob(parentPath); 
      Pattern fileRegex = Pattern.compile(
        "^".concat(path.substring(parentPath.length() + 1)    
          .replace(".", "\\.")
          .replace('?', '.')
          .replace("*", "[^/]*")
        ).concat("$")
      );
      FilenameFilter filt = new RegexFileFilter(fileRegex);
      if (GLOB_DEBUG) System.err.printf(
        "[DEBUG] regex = \"%s\"\n", fileRegex.pattern()
      );
      for (String dir: dirs) {
        int prefixLen = dir.length() + 1;
        StringBuilder sb
          = new StringBuilder(prefixLen + 128)
              .append(dir).append('/');
        String[] files = new File(dir).list(filt);
        if (files == null || files.length == 0) continue;
        // Collections.addAll(results, files);
        for (int i=0, len=files.length; i<len; ++i) {          
          results.add(
            sb.replace(prefixLen, sb.length(), files[i]).toString()
          );
        }
      }
      return results;
    } catch (Exception e) { 
      printStackTrace(e);
      return new ArrayList<String>();
    }    
  }
  
  static File TEMP_ROOT = new File(
    new File("/mnt/shell/emulated/0"), ".tmp"
  );
  
  static File _tempDir;
  static List<String> SRC_DIR_NAMES = Arrays.asList(
    ".", "java", "src", "src1", "origsrc", "csrc", "src/main/java"
  );
  
  public static File getSourceFile(String name) {
    return getSourceFile(name, -1);
  }
  
  @Nullable
  public static File getSourceFile(String name, int lineNumber) {
    if (name == null) throw new IllegalArgumentException("name == null");
    if (name.startsWith("sun.reflect.Generated")) return null;
    List<File> files = getSourceFiles(name, lineNumber == -1? 1: 100, -1);
    if (lineNumber == -1) {
      if (files.isEmpty()) {
        Log.w(TAG, "Should not be empty list here", new AssertionError());
        return asFile("", name);
      }
      return files.iterator().next();
    }
    File f = null;
    for (final File file: files) {
      try {
        Collection<String> lines;
        NumberedLines nl = new NumberedLines(
          (lines = FileUtils.readLines(file, StandardCharsets.UTF_8))
            .toArray(new String[0])
        );
        if (lines.size() < lineNumber) continue;
        if (nl.getLine(lineNumber).toString()
              .replaceAll("//.*$", "")
              .replaceAll("^ *\\*[^/].*$", "")
              .replaceAll("\\s|[{};]", "")
              .length() != 0)
        {
          System.err.println(file);
          f = file;
          break;
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
    return f;
  }
  
  public static List<File> getSourceFiles(String name, int max, int lineNumber) {
    if (name == null) throw new IllegalArgumentException("name == null");
    if (name.startsWith("sun.reflect.Generated")) return Collections.emptyList();
    Throwable exception = null;
    List<File> files = new ArrayList<>();
    List<URL> surls = findSource(name, DEFAULT_FILESYSTEM_FIRST, max, lineNumber);
    nextUrl:
    for (URL surl: surls) {
      try {
        PathInfo pi = PathInfo.getPathInfo(surl); 
        if (!(surl.openConnection() instanceof JarURLConnection)) {
          files.add(PosixFileInputStream.resolve(new File(pi.path)));
          if (files.size() >= max) break;
          continue;
        }
        String inSrcPath = Pattern.compile(
          "^(?:(?:[a-z]+:)|[!/]+)*/*"
        ).matcher(StringUtils.substringAfter(
          surl.getPath(), pi.path
        )).replaceAll("");
        
        File localSrcFile = null;
        for (String srcDirName: SRC_DIR_NAMES) {
          localSrcFile = PosixFileInputStream.resolve(new File(
            new File(new File(pi.dir), srcDirName).toURI().resolve(
              inSrcPath
            ).getPath()
          ));
          if (! localSrcFile.exists()) { 
            localSrcFile = PosixFileInputStream.resolve(new File(
              new File(new File(pi.dir), srcDirName).toURI().resolve(
                inSrcPath
              ).getPath()
            ));
          }
          if (! localSrcFile.exists()) { 
            localSrcFile = PosixFileInputStream.resolve(new File(
              new File(new File(pi.dir), srcDirName).toURI().resolve(
                "main/java/".concat(inSrcPath)
              ).getPath()
            ));
          }
          if (localSrcFile.exists()) files.add(localSrcFile);
          if (files.size() >= max) break nextUrl;
          continue;
        }
        
        if (_tempDir == null) _tempDir = new File(
          TEMP_ROOT, String.format(
            "source_export_%s",
            Long.toString(System.currentTimeMillis(), 32)
          )
        );
        if (! _tempDir.exists()) _tempDir.mkdirs();
        
        localSrcFile = new File(
          _tempDir, ClassInfo.classNameToPath(name, "java")
        );
        if (localSrcFile.exists()) localSrcFile.delete();
        
        File parentDir = localSrcFile.getParentFile();
        if (! parentDir.exists()) parentDir.mkdirs(); 
        
        boolean ok = localSrcFile.createNewFile();
        try (OutputStream os = new BufferedOutputStream(
          new FileOutputStream(localSrcFile))) 
        {
          try (InputStream is = surl.openConnection().getInputStream())
          {
            IOUtils.copy(is, os);
          }
        } catch (IOException ioe) {
          exception = (exception == null) ? ioe : exception;
          ioe.printStackTrace();
          continue;
        }
        files.add(localSrcFile);
        if (files.size() >= max) break;
      } catch (Exception ex) {
        exception = (exception == null) ? ex : exception;
        ex.printStackTrace();
      }
    }
    if (files.isEmpty()) {
      if (exception != null) {
        new SourceNotFoundException(name, exception).printStackTrace();
      }
    }
    return files;
  }
  
  
  static final File TMPDIR = new File(
    System.getProperty("java.io.tmpdir", ".")
  ).getAbsoluteFile();
    

  
  public static URL lastResortFindSource(String className)
    throws IOException
  {
    if (className == null) throw new IllegalArgumentException("className == null");
    
    Class<?> cls = null;
    try {
      cls = DexVisitor.classForName(className);
    } catch (final Exception e) {
      Log.w(TAG, String.format("Warning: DexVisitor.classForName(%s) threw %s", 
        className, e));
      Log.w(TAG, e);
    }
    
    if (cls == null) {
      Log.w(TAG, String.format(
        "Warning: DexVisitor.classForName(%s) returned null", className));
      try {
        cls = Class.forName(
          className, false, ClassLoader.getSystemClassLoader()
        );
      } catch (Exception e) {
        Log.w(TAG, String.format("Warning: Class.forName(%s) threw %s", 
          className, e));
        Log.w(TAG, e);
      }
    }
    
    final ClassLoader contextLdr;
    final ClassLoader ldr = (cls != null)
      ? cls.getClassLoader()
      : ((contextLdr = Thread.currentThread().getContextClassLoader()));    
    
    final URL cres = (cls != null) ? ClassInfo.getClassResource(cls)
      : getResource(
          Thread.currentThread().getContextClassLoader(),
          ClassInfo.classNameToPath(className, "class")
        );
    
    final File jdkFile = new File(
      new File("/external_sd/_projects/sdk/jdk/src"),
      ClassInfo.classNameToPath(
        StringUtils.substringBefore(className, "$"),
        "java"
      )
    );
    
    if (jdkFile.exists()) {
      final URL fileUrl = jdkFile.toURL();
      return fileUrl;
    }
    
    final byte[] classBytes = cres != null && exists(cres)
      ? IOUtils.toByteArray(cres)
      : null;
    
    final TypeDefinition td = (classBytes != null)
      ? ProcyonUtil.getTypeDefinition(classBytes)
      : ProcyonUtil.getTypeDefinition(className.replace('.', '/'));
    
    final SourceFileAttribute srcattr
      = (td != null)
          ? CollectionUtilities.firstOrDefault(CollectionUtilities.ofType(
              td.getSourceAttributes(), SourceFileAttribute.class
            ))
          : null;
    
    final String srcfileName = (srcattr != null)
      ? srcattr.getSourceFile()
      : SourceUtil.guessSourceFileName(className);
    
    final int lastDot = className.lastIndexOf('.');
    final String resPath = (lastDot != -1)
      ? String.format(
          "%s/%s",
          className.substring(0, lastDot).replace('.', '/'),
          srcfileName
        )
      : srcfileName;
    
    final URL resFinal = getResource(ldr, resPath);
    
    if (resFinal != null && exists(resFinal)) return resFinal;
    
    File jdkFileUsingSourceFile = new File(
      new File("/external_sd/_projects/sdk/jdk/src"),
      resPath
    );
    if (jdkFileUsingSourceFile.exists()) {
      final URL fileUrl = jdkFileUsingSourceFile.toURL();
      return fileUrl;
    }
    
    if (td != null) {
      try {
        final File file = asFile(
          ProcyonUtil.decompileToAst(td).getText(),
          className
        );
        return file.toURL();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return asFile("", className).toURL();
    
    /*
    final URL url = (cres != null)
      ? new URL(cres.toString().replaceAll("(?:\\$[^/]*)?\\.class$", ".java"))
      : getResource(
          ldr,
          ClassInfo.classNameToPath(
            StringUtils.substringBefore(className, "$"),
            "java"
          )
        );
    */
  }
  
  public static File asFile(String s, @Nullable String className) {
    try {
      final File emptyFile;
      if (className != null) {
        emptyFile = File.createTempFile(
          StringUtils.substringBeforeLast(
            ClassInfo.classNameToPath(ClassInfo.typeToName(className), "java"), "."
          ),
          ".java",
          TMPDIR
        );
      } else {
        emptyFile = File.createTempFile("temp_srcfile", ".java");
      }
      emptyFile.createNewFile();
      emptyFile.setReadable(true);
      emptyFile.setWritable(true);
      try (final FileOutputStream fos = new FileOutputStream(emptyFile)) {
        IOUtils.copy(
          new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
          fos
        );
        fos.flush();
        return emptyFile;
      }
    } catch (IOException ioe) {
      printStackTrace(ioe);
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
  
  public static URL getResource(final ClassLoader classLoaderMaybeNull,
    final String name)
  {
    return (classLoaderMaybeNull != null)
      ? classLoaderMaybeNull.getResource(name)
      : ClassLoader.getSystemResource(name);
  }
  
  public static <T extends Throwable> T printStackTrace(final T throwable) {
    throwable.printStackTrace();
    return throwable;
  }
  
}