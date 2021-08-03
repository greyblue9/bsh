package org.d6r;

import org.apache.commons.lang3.StringUtils;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.Map;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;

class TypeLoaderBytecodeProvider implements IBytecodeProvider {

  ITypeLoader typeLoader;

  String dirPrefix;

  final Buffer buf;

  SoftHashMap<String, byte[]> _cache;
  public static boolean USE_ZIP_CACHE = true;

  public TypeLoaderBytecodeProvider(final ITypeLoader typeLoader, final String dirPrefix) {
    this._cache = new SoftHashMap<String, byte[]>();
    this.typeLoader = typeLoader;
    this.buf = new Buffer(65536);
    this.dirPrefix = dirPrefix;
  }
  
  Map<String, ZipFile> cache = new SoftHashMap<>();
  
  /** @Override */
  public byte[] getBytecode(final String externalPath, final String internalPath) 
    throws IOException
  {
    final File file = new File(externalPath);
    if (internalPath == null && file.exists()) {
      return FileUtils.readFileToByteArray(file);
    }
    if (file.exists()) {
      Throwable t = null;
      try {
        if (StringUtils.endsWith(file.getName(), ".jar") ||
            StringUtils.endsWith(file.getName(), ".apk") ||
            StringUtils.endsWith(file.getName(), ".zip"))
        {
          System.err.printf(
            "%s: reading bytecode from archive: %s\n",
            getClass().getSimpleName(), file
          );
          ZipFile archive = cache.get(externalPath);
          if (archive == null) {
            archive = new ZipFile(file);
            if (USE_ZIP_CACHE) cache.put(externalPath, archive);
          }
          try {
            final ZipEntry entry = archive.getEntry(internalPath);
            if (entry == null) {
              throw new IOException(
                "ZipFile [%s]: Entry not found: " + internalPath
              );
            }
            try {
              final InputStream is = archive.getInputStream(entry);
              try {
                return IOUtils.toByteArray(is);
              } finally {
                if (is != null) {
                  is.close();
                }
              }
            } finally {
            }
          } finally {
            if (archive != null) {
              if (!USE_ZIP_CACHE) archive.close();
            }
          }
        } else {
          System.err.printf(
            "%s: reading bytecode from file: %s\n",
            getClass().getSimpleName(), file
          );
          return FileUtils.readFileToByteArray(file);
        }
      } finally {
      }
    }
    String qualifClassNameOrNull = 
      (internalPath != null && StringUtils.endsWith(internalPath, ".class"))
        ? StringUtils.substringBeforeLast(internalPath, ".class")
        : null;
    return this.getBytecode(externalPath, internalPath, qualifClassNameOrNull);
  }
  
  @Override
  public byte[] getBytecode(final String externalPath, final String internalPath, 
    final String qualifiedClassName) throws IOException
  {
    String name = (qualifiedClassName != null) ? qualifiedClassName : ((internalPath != null) ? internalPath : ((externalPath != null) ? externalPath : null));
    if (name == null) {
      throw new IOException("getBytecode() called with all null arguments");
    }
    if (StringUtils.endsWith(name, ".class")) {
      name = StringUtils.substringBeforeLast(name, ".class");
    }
    if (this.dirPrefix != null && StringUtils.startsWith(name, this.dirPrefix)) {
      name = StringUtils.substringAfter(name, this.dirPrefix.concat("/"));
    }
    final String typeNameOrPath = (name.indexOf(46) != -1) ? ClassInfo.classNameToPath(name) : name;
    byte[] classBytes;
    if ((classBytes = this._cache.get(name)) != null) {
      System.err.printf("getBytecode() cache hit: [%s]\n", name);
      return classBytes;
    }
    System.err.printf("%s.getBytecode(externalPath: %s, internalPath: %s, qualifiedClassName: %s);\n    - caller: %s\n", this.getClass().getSimpleName(), (externalPath != null) ? "\"".concat(externalPath).concat("\"") : "null", (internalPath != null) ? "\"".concat(internalPath).concat("\"") : "null", (qualifiedClassName != null) ? "\"".concat(qualifiedClassName).concat("\"") : "null", Debug.getCallingMethod(4));
    System.err.printf("  - name = \"%s\"\n", name);
    System.err.printf("  - typeNameOrPath = \"%s\"\n", typeNameOrPath);
    this.buf.reset();
    final boolean found = this.typeLoader.tryLoadType(typeNameOrPath, this.buf);
    if (found) {
      classBytes = new byte[this.buf.size()];
      this.buf.read(classBytes, 0, classBytes.length);
      this._cache.put(name, classBytes);
      return classBytes;
    }
    throw new IOException("Not found: " + typeNameOrPath);
  }
}