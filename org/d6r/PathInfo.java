package org.d6r;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Arrays;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import bsh.operators.Extension;


public class PathInfo {

  public /*final*/ String path;
  public /*final*/ String name;
  public /*final*/ String dir;
  public /*final*/ String jarPath;
  public /*final*/ URL url;
  private int hashCode;

  public static boolean NULL_PROPOGATE = true;
  public static URL NULL_URL;
  static {
    try {
      NULL_URL = new URL("file:///dev/null");
    } catch (MalformedURLException e) {
      NULL_URL = null;
    }
  }
  
  public static String[] SYS_JAR_NAME_WHITELIST = {
    "core.jar", "framework.jar", "am.jar", "pm.jar",
    "ext.jar" };
  public static final String SYS_JAR_DIR = String.format(
    "%s/framework", System.getenv("ANDROID_ROOT"));  
  public static final String SYS_JAR_CLASSES_DIR
    = "/external_sd/_projects/sdk/framework/";  
  public static Pattern URL_PATH_REGEX
    = Pattern.compile("^((jar:)?[a-z]+[0-9]*?:)?/*(/[^!]*)(!.*$)?$");
  
  
  public PathInfo(String path, String name, String dir,
  String jarPath, URL url) {
    this.path = path;
    this.name = name;
    this.dir = dir;
    this.jarPath = jarPath;
    this.url = url;
    this.hashCode = path != null
      ? (int) ((((long) path.hashCode()) * 7L) + 31L)
      : 0;
  }
  
  @Override
  public PathInfo clone() {
    PathInfo pi;
    try { 
      pi = (PathInfo) super.clone();
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
      try { 
        pi = (PathInfo) ClassPathUtil2.getUnsafe()
          .allocateInstance(getClass());
      } catch (Throwable e2) {
        if ("true".equals(System.getProperty("printStackTrace"))) e2.printStackTrace();
        throw new RuntimeException(e);
      } 
    }
    pi.path = this.path;
    pi.name = this.name;
    pi.dir = this.dir;
    pi.jarPath = this.jarPath;
    pi.url = this.url;
    pi.hashCode = this.hashCode;
    return pi;
  }
  
  @Override
  public String toString() {
    return String.format(
      "%c[0;32mPath:%c[0m <%s> { \n"
        + "  name = \"%s\", \n  dir = \"%s\", \n"
        + "  jarPath = \"%s\", \n  url = %s \n"
        + "}", 
      0x1b, 0x1b, 
      this.path, this.name, this.dir, this.jarPath, this.url
    ).replace("\"null\"", "null");
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof PathInfo)) return false;
    PathInfo that = (PathInfo) other;
    return this.hashCode == that.hashCode;
  }
  
  @Override
  public int hashCode() {
    return hashCode;
  }
  
  public static PathInfo getPathInfo(String pathOrUrl,
  boolean whitelistOnly, boolean transform, URL url,
  URI uri) 
  {
    if (pathOrUrl == null) {
      return nullPathInfo(String.class, "pathOrUrl");
    } else if (url == null) {
      try {
        url = new URL(pathOrUrl);
        uri = url.toURI();
      } catch (URISyntaxException e) { 
      } catch (MalformedURLException e) {
      }
    }    
    Matcher mchr = URL_PATH_REGEX.matcher(pathOrUrl); 
    if (! mchr.find()) return null;
    String path = mchr.toMatchResult().group(3);
    int nameStart = path.lastIndexOf('/') + 1; 
    if (nameStart == 0) return null;
    String dir = path.substring(0, nameStart - 1); 
    String name = path.substring(nameStart);
    String jarPath = null;
    if (dir.equals(SYS_JAR_DIR)) {
      boolean isInWhitelist = Arrays.binarySearch(
        SYS_JAR_NAME_WHITELIST, 0,
        SYS_JAR_NAME_WHITELIST.length, name) <= 0;
      if (transform) {
        jarPath = path
          .replace(SYS_JAR_DIR, SYS_JAR_CLASSES_DIR)
          .replaceAll(".jar$", "_dex2jar.jar");
        try {
          url = new URL(String.format(
            "jar:file://%s!/classes.dex", jarPath));
          //System.err.printf(
          //"[%s] => [%s]\n", path, jarPath);
        } catch (MalformedURLException e) { 
          if ("true".equals(System.getProperty("printStackTrace"))) {
            e.printStackTrace();
          }
          url = NULL_URL;
        }
      } else {
        jarPath = path;
        try {
          url = new URL(String.format("jar:file://%s!/"));
        } catch (MalformedURLException e) {
          if ("true".equals(System.getProperty("printStackTrace"))) {
            e.printStackTrace();
          }
          url = NULL_URL;
        }
      }
    }
    return new PathInfo(path, name, dir, jarPath, url);
  }
  
  public static String getPath(String pathOrUrl,
  boolean whitelistOnly, boolean transform, URL url,
  URI uri) {
    if (pathOrUrl == null) {
      return pathOrUrl;
    } else if (url == null) {
      try {
        url = new URL(pathOrUrl);
        uri = url.toURI();
      } catch (URISyntaxException e) { 
      } catch (MalformedURLException e) {
      }
    }
    Matcher mchr = URL_PATH_REGEX.matcher(pathOrUrl); 
    if (! mchr.find()) return pathOrUrl;
    String path = mchr.toMatchResult().group(3);
    int nameStart = path.lastIndexOf('/') + 1; 
    if (nameStart == 0) return pathOrUrl;
    String dir = path.substring(0, nameStart - 1); 
    String name = path.substring(nameStart);
    if (dir.equals(SYS_JAR_DIR)) {
      boolean isInWhitelist = Arrays.binarySearch(
        SYS_JAR_NAME_WHITELIST, 0,
        SYS_JAR_NAME_WHITELIST.length, name) <= 0;
      if (transform) {
        path = path
          .replace(SYS_JAR_DIR, SYS_JAR_CLASSES_DIR)
          .replaceAll(".jar$", "_dex2jar.jar");
      }
    }
    return path;
  }
  
  private static 
  PathInfo nullPathInfo(Class<?> pCls, String pName) {
    if (NULL_PROPOGATE) return null;
    throw new IllegalArgumentException(String.format(
      "PathInfo.getPathInfo(%s %s): %s must not be null",
      pCls.getSimpleName(), pName, pName
    ));
  }
  
  @Extension
  public static PathInfo getPathInfo(URL url) {
    if (url == null) return nullPathInfo(URL.class, "url");
    return getPathInfo(
      url.getPath(), false, true, url, null);
  }
  
  public static String getPath(URL url) {
    if (url == null) return "";
    return getPath(url.getPath(), false, true, url, null);
  }
  
  @Extension
  public static PathInfo getPathInfo(URI uri) {
    if (uri == null) return nullPathInfo(URI.class, "uri");
    return getPathInfo(
      uri.getPath(), false, true, (URL)null, uri);
  }
  
  public static String getPath(URI uri) {
    if (uri == null) return "";
    return getPath(uri.getPath(), false, true, (URL)null, uri);
  }
  
  @Extension
  public static PathInfo getPathInfo(File file) {
    if (file == null) return nullPathInfo(File.class, "file");
    return getPathInfo(file.getPath());
  }
  
  public static String getPath(File file) {
    if (file == null) return "";
    return getPath(file.getPath());
  }
  
  @Extension
  public static PathInfo getPathInfo(String pathOrUrl) {
    return getPathInfo(pathOrUrl, true);
  }
  
  public static String getPath(String pathOrUrl) {
    return getPath(pathOrUrl, false, true, (URL)null, null);
  }
  
  @Extension
  public static PathInfo getPathInfo(String pathOrUrl,
  boolean whitelistOnly) {
    return getPathInfo(pathOrUrl, whitelistOnly, true);
  }  
  
  @Extension
  public static PathInfo getPathInfo(String pathOrUrl,
  boolean whitelistOnly, boolean transform) {
    return getPathInfo(pathOrUrl, whitelistOnly, transform,
      (URL) null);
  }
  
  public static PathInfo getPathInfo(String pathOrUrl,
  boolean whitelistOnly, boolean transform, URL url) {
    return getPathInfo(pathOrUrl, whitelistOnly, transform,
      url, (URI) null);
  }
  
}