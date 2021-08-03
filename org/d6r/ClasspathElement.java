package org.d6r;

import java.util.Iterator;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.net.JarURLConnection;;


interface ClasspathResource {
  URLConnection openConnection();
  JarURLConnection openJarConnection();
  ZipFile getZipFile();
  ZipEntry getEntry(String name);
  InputStream getInputStream();
  InputStream getInputStream(String entryName);
  InputStream getInputStream(ZipEntry entry);
  byte[] toByteArray();
  byte[] toByteArray(String entryName);
  byte[] toByteArray(ZipEntry entry);  
  long length();
  File getFile();
  String getPath();
  URL toURL();
  URI toURI();
}

public interface ClasspathElement<S, R> {
  
  public static enum FileFormat {
    DEX,
    ODEX,
    ZIP,
    JAR,
    CLASS;
  }
  
  boolean canLoadClasses();
  byte[] getBytes();
  byte[] getBytes(String resourceName);
  ClassLoader getClassLoader();
  Iterable<Class<?>> getClasses();
  String[] getClassNames();
  S getClassStore();
  ZipEntry getEntry(String resourceName);
  File getFile();
  FileFormat getFileFormat();
  long getLength();
  long getLength(String resourceName);
  ClasspathResource getResource(String name);
  Iterable<R> getResources();
  InputStream getResourceAsStream(String resourceName);
  Object getWrapped();
  ZipFile getZipFile();
  boolean isDirectory();
  boolean isLive();
  Iterator<R> iterator();
  <T> Class<T> loadClass(String name);
}