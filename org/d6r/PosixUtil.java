package org.d6r;

import static org.d6r.PosixUtil.PosixAccess.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashSet;
import java.util.Set;
import libcore.io.OsConstants;
import libcore.io.Posix;
import libcore.io.StructAddrinfo;
import libcore.io.StructFlock;
import libcore.io.StructGroupReq;
import libcore.io.StructLinger;
import libcore.io.StructPasswd;
import libcore.io.StructPollfd;
import libcore.io.StructStat;
import libcore.io.StructStatVfs;
import libcore.io.StructTimeval;
import libcore.io.StructUcred;
import libcore.io.StructUtsname;
import libcore.util.MutableInt;
import libcore.util.MutableLong;
import java.util.*;
import org.apache.commons.lang3.ClassUtils;
import org.d6r.Reflector;



public class PosixUtil {
  
  public static final int AF_INET = 2;
  public static final int AF_INET6 = 10;
  public static final int AF_UNIX = 1;
  public static final int AF_UNSPEC = 0;
  
  public static final int AI_ADDRCONFIG = 1024;
  public static final int AI_ALL = 256;
  public static final int AI_CANONNAME = 2;
  public static final int AI_NUMERICHOST = 4;
  public static final int AI_NUMERICSERV = 8;
  public static final int AI_PASSIVE = 1;
  public static final int AI_V4MAPPED = 2048;
  
  public static final int FD_CLOEXEC = 1;
  
  public static final int F_DUPFD = 0;
  public static final int F_GETFD = 1;
  public static final int F_GETFL = 3;
  public static final int F_GETLK = 5;
  public static final int F_GETLK64 = 12;
  public static final int F_GETOWN = 9;
  public static final int F_OK = 0;
  public static final int F_RDLCK = 0;
  public static final int F_SETFD = 2;
  public static final int F_SETFL = 4;
  public static final int F_SETLK = 6;
  public static final int F_SETLK64 = 13;
  public static final int F_SETLKW = 7;
  public static final int F_SETLKW64 = 14;
  public static final int F_SETOWN = 8;
  public static final int F_UNLCK = 2;
  public static final int F_WRLCK = 1;
  
  public static final int IP_MULTICAST_IF = 32;
  public static final int IP_MULTICAST_LOOP = 34;
  public static final int IP_MULTICAST_TTL = 33;
  public static final int IP_TOS = 1;
  public static final int IP_TTL = 2;
  
  public static final int MS_ASYNC = 1;
  public static final int MS_INVALIDATE = 2;
  public static final int MS_SYNC = 4;
  
  public static final int NI_DGRAM = 16;
  public static final int NI_NAMEREQD = 4;
  public static final int NI_NOFQDN = 1;
  public static final int NI_NUMERICHOST = 2;
  public static final int NI_NUMERICSERV = 8;
  
  public static final int O_ACCMODE = 3;
  public static final int O_APPEND = 1024;
  public static final int O_CREAT = 64;
  public static final int O_DIRECT = 040000;
  public static final int O_DSYNC = 010000;
  public static final int O_EXCL = 128;
  public static final int O_NOCTTY = 256;
  public static final int O_NOFOLLOW = 32768; // = 0400000;
  public static final int O_NONBLOCK = 2048;
  public static final int O_RDONLY = 0;
  public static final int O_RDWR = 2;
  public static final int O_SYNC = 1052672;
  public static final int O_TRUNC = 512;
  public static final int O_WRONLY = 1;
  

  
  public static final int PROT_EXEC = 4;
  public static final int PROT_NONE = 0;
  public static final int PROT_READ = 1;
  public static final int PROT_WRITE = 2;
    
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;
  public static final int SEEK_SET = 0;

  public static final int SO_BINDTODEVICE = 25;
  public static final int SO_BROADCAST = 6;
  public static final int SO_DEBUG = 1;
  public static final int SO_DONTROUTE = 5;
  public static final int SO_ERROR = 4;
  public static final int SO_KEEPALIVE = 9;
  public static final int SO_LINGER = 13;
  public static final int SO_OOBINLINE = 10;
  public static final int SO_PASSCRED = 16;
  public static final int SO_PEERCRED = 17;
  public static final int SO_RCVBUF = 8;
  public static final int SO_RCVLOWAT = 18;
  public static final int SO_RCVTIMEO = 20;
  public static final int SO_REUSEADDR = 2;
  public static final int SO_SNDBUF = 7;
  public static final int SO_SNDLOWAT = 19;
  public static final int SO_SNDTIMEO = 21;
  public static final int SO_TYPE = 3;
  
  public static final int  S_IFBLK = 0x6000;
  public static final int  S_IFCHR = 0x2000;
  public static final int  S_IFDIR = 0x4000;
  public static final int  S_IFIFO = 0x1000;
  public static final int  S_IFLNK = 0xa000;
  public static final int   S_IFMT = 0xf000;
  public static final int  S_IFREG = 0x8000;
  public static final int S_IFSOCK = 0xc000;
  public static final int  S_IRGRP = 0x0020;
  public static final int  S_IROTH = 0x0004;
  public static final int  S_IRUSR = 0x0100;
  public static final int  S_IRWXG = 0x0038;
  public static final int  S_IRWXO = 0x0007;
  public static final int  S_IRWXU = 0x01c0;
  public static final int  S_ISGID = 0x0400;
  public static final int  S_ISUID = 0x0800;
  public static final int  S_ISVTX = 0x0200;
  public static final int  S_IWGRP = 0x0010;
  public static final int  S_IWOTH = 0x0002;
  public static final int  S_IWUSR = 0x0080;
  public static final int  S_IXGRP = 0x0008;
  public static final int  S_IXOTH = 0x0001;
  public static final int  S_IXUSR = 0x0040;
  
  public static final int R_OK = 4;
  public static final int W_OK = 2;
  public static final int X_OK = 1;
  
  public static Posix POSIX; 
  
  public static final boolean JRE = CollectionUtil.isJRE();
  static final Map<Integer, AutoCloseable> closeables = new HashMap<>();
  
  
  public static class PosixAccess {
    static Constructor<Posix> ctor;
    
    static Method accept_FileDescriptor_InetSocketAddress;
    static Method access_String_int;
    static Method bind_FileDescriptor_InetAddress_int;
    static Method chmod_String_int;
    static Method chown_String_int_int;
    static Method close_FileDescriptor;
    static Method connect_FileDescriptor_InetAddress_int;
    static Method dup_FileDescriptor;
    static Method dup2_FileDescriptor_int;
    static Method environ;
    static Method execv_String_String;
    static Method execve_String_String_String;
    static Method fchmod_FileDescriptor_int;
    static Method fchown_FileDescriptor_int_int;
    static Method fcntlFlock_FileDescriptor_int_StructFlock;
    static Method fcntlLong_FileDescriptor_int_long;
    static Method fcntlVoid_FileDescriptor_int;
    static Method fdatasync_FileDescriptor;
    static Method fstat_FileDescriptor;
    static Method fstatvfs_FileDescriptor;
    static Method fsync_FileDescriptor;
    static Method ftruncate_FileDescriptor_long;
    static Method gai_strerror_int;
    static Method getaddrinfo_String_StructAddrinfo;
    static Method getegid;
    static Method getenv_String;
    static Method geteuid;
    static Method getgid;
    static Method getnameinfo_InetAddress_int;
    static Method getpeername_FileDescriptor;
    static Method getpid;
    static Method getppid;
    static Method getpwnam_String;
    static Method getpwuid_int;
    static Method getsockname_FileDescriptor;
    static Method getsockoptByte_FileDescriptor_int_int;
    static Method getsockoptInAddr_FileDescriptor_int_int;
    static Method getsockoptInt_FileDescriptor_int_int;
    static Method getsockoptLinger_FileDescriptor_int_int;
    static Method getsockoptTimeval_FileDescriptor_int_int;
    static Method getsockoptUcred_FileDescriptor_int_int;
    static Method gettid;
    static Method getuid;
    static Method if_indextoname_int;
    static Method inet_pton_int_String;
    static Method ioctlInetAddress_FileDescriptor_int_String;
    static Method ioctlInt_FileDescriptor_int_MutableInt;
    static Method isatty_FileDescriptor;
    static Method kill_int_int;
    static Method lchown_String_int_int;
    static Method listen_FileDescriptor_int;
    static Method lseek_FileDescriptor_long_int;
    static Method lstat_String;
    static Method mincore_long_long_byte;
    static Method mkdir_String_int;
    static Method mlock_long_long;
    static Method mmap_long_long_int_int_FileDescriptor_long;
    static Method msync_long_long_int;
    static Method munlock_long_long;
    static Method munmap_long_long;
    static Method open_String_int_int;
    static Method pipe;
    static Method poll_StructPollfd_int;
    static Method pread_FileDescriptor_ByteBuffer_long;
    static Method pread_FileDescriptor_byte_int_int_long;
    static Method pwrite_FileDescriptor_ByteBuffer_long;
    static Method pwrite_FileDescriptor_byte_int_int_long;
    static Method read_FileDescriptor_ByteBuffer;
    static Method read_FileDescriptor_byte_int_int;
    static Method readv_FileDescriptor_Object_int_int;
    static Method recvfrom_FileDescriptor_ByteBuffer_int_InetSocketAddress;
    static Method recvfrom_FileDescriptor_byte_int_int_int_InetSocketAddress;
    static Method remove_String;
    static Method rename_String_String;
    static Method sendfile_FileDescriptor_FileDescriptor_MutableLong_long;
    static Method sendto_FileDescriptor_ByteBuffer_int_InetAddress_int;
    static Method sendto_FileDescriptor_byte_int_int_int_InetAddress_int;
    static Method setegid_int;
    static Method setenv_String_String_boolean;
    static Method seteuid_int;
    static Method setgid_int;
    static Method setsid;
    static Method setsockoptByte_FileDescriptor_int_int_int;
    static Method setsockoptGroupReq_FileDescriptor_int_int_StructGroupReq;
    static Method setsockoptIfreq_FileDescriptor_int_int_String;
    static Method setsockoptInt_FileDescriptor_int_int_int;
    static Method setsockoptIpMreqn_FileDescriptor_int_int_int;
    static Method setsockoptLinger_FileDescriptor_int_int_StructLinger;
    static Method setsockoptTimeval_FileDescriptor_int_int_StructTimeval;
    static Method setuid_int;
    static Method shutdown_FileDescriptor_int;
    static Method socket_int_int_int;
    static Method socketpair_int_int_int_FileDescriptor_FileDescriptor;
    static Method stat_String;
    static Method statvfs_String;
    static Method strerror_int;
    static Method strsignal_int;
    static Method symlink_String_String;
    static Method sysconf_int;
    static Method tcdrain_FileDescriptor;
    static Method tcsendbreak_FileDescriptor_int;
    static Method umask_int;
    static Method uname;
    static Method unsetenv_String;
    static Method waitpid_int_MutableInt_int;
    static Method write_FileDescriptor_ByteBuffer;
    static Method write_FileDescriptor_byte_int_int;
    static Method writev_FileDescriptor_Object_int_int;
    static Method preadBytes_FileDescriptor_Object_int_int_long;
    static Method pwriteBytes_FileDescriptor_Object_int_int_long;
    static Method readBytes_FileDescriptor_Object_int_int;
    static Method recvfromBytes_FileDescriptor_Object_int_int_int_InetSocketAddress;
    static Method sendtoBytes_FileDescriptor_Object_int_int_int_InetAddress_int;
    static Method umaskImpl_int;
    static Method writeBytes_FileDescriptor_Object_int_int;
    
    
    static {
      if (! JRE) initMethods();
    }
    
    private static void initMethods() {
      try {
        (ctor = (Constructor<Posix>) (Constructor<?>) 
          Posix.class.getDeclaredConstructor()).setAccessible(true);
        POSIX = (Posix) ctor.newInstance();
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      }
      
      try {
        (accept_FileDescriptor_InetSocketAddress = Posix.class.getDeclaredMethod("accept", FileDescriptor.class, InetSocketAddress.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (access_String_int = Posix.class.getDeclaredMethod("access", String.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (bind_FileDescriptor_InetAddress_int = Posix.class.getDeclaredMethod("bind", FileDescriptor.class, InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (chmod_String_int = Posix.class.getDeclaredMethod("chmod", String.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (chown_String_int_int = Posix.class.getDeclaredMethod("chown", String.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (close_FileDescriptor = Posix.class.getDeclaredMethod("close", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (connect_FileDescriptor_InetAddress_int = Posix.class.getDeclaredMethod("connect", FileDescriptor.class, InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (dup_FileDescriptor = Posix.class.getDeclaredMethod("dup", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (dup2_FileDescriptor_int = Posix.class.getDeclaredMethod("dup2", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (environ = Posix.class.getDeclaredMethod("environ")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (execv_String_String = Posix.class.getDeclaredMethod("execv", String.class, String[].class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (execve_String_String_String = Posix.class.getDeclaredMethod("execve", String.class, String[].class, String[].class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fchmod_FileDescriptor_int = Posix.class.getDeclaredMethod("fchmod", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fchown_FileDescriptor_int_int = Posix.class.getDeclaredMethod("fchown", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fcntlFlock_FileDescriptor_int_StructFlock = Posix.class.getDeclaredMethod("fcntlFlock", FileDescriptor.class, Integer.TYPE, StructFlock.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fcntlLong_FileDescriptor_int_long = Posix.class.getDeclaredMethod("fcntlLong", FileDescriptor.class, Integer.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fcntlVoid_FileDescriptor_int = Posix.class.getDeclaredMethod("fcntlVoid", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fdatasync_FileDescriptor = Posix.class.getDeclaredMethod("fdatasync", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fstat_FileDescriptor = Posix.class.getDeclaredMethod("fstat", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fstatvfs_FileDescriptor = Posix.class.getDeclaredMethod("fstatvfs", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (fsync_FileDescriptor = Posix.class.getDeclaredMethod("fsync", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (ftruncate_FileDescriptor_long = Posix.class.getDeclaredMethod("ftruncate", FileDescriptor.class, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (gai_strerror_int = Posix.class.getDeclaredMethod("gai_strerror", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getaddrinfo_String_StructAddrinfo = Posix.class.getDeclaredMethod("getaddrinfo", String.class, StructAddrinfo.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getegid = Posix.class.getDeclaredMethod("getegid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getenv_String = Posix.class.getDeclaredMethod("getenv", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (geteuid = Posix.class.getDeclaredMethod("geteuid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getgid = Posix.class.getDeclaredMethod("getgid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getnameinfo_InetAddress_int = Posix.class.getDeclaredMethod("getnameinfo", InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getpeername_FileDescriptor = Posix.class.getDeclaredMethod("getpeername", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getpid = Posix.class.getDeclaredMethod("getpid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getppid = Posix.class.getDeclaredMethod("getppid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getpwnam_String = Posix.class.getDeclaredMethod("getpwnam", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getpwuid_int = Posix.class.getDeclaredMethod("getpwuid", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockname_FileDescriptor = Posix.class.getDeclaredMethod("getsockname", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptByte_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptByte", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptInAddr_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptInAddr", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptInt_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptInt", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptLinger_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptLinger", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptTimeval_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptTimeval", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getsockoptUcred_FileDescriptor_int_int = Posix.class.getDeclaredMethod("getsockoptUcred", FileDescriptor.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (gettid = Posix.class.getDeclaredMethod("gettid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (getuid = Posix.class.getDeclaredMethod("getuid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (if_indextoname_int = Posix.class.getDeclaredMethod("if_indextoname", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (inet_pton_int_String = Posix.class.getDeclaredMethod("inet_pton", Integer.TYPE, String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (ioctlInetAddress_FileDescriptor_int_String = Posix.class.getDeclaredMethod("ioctlInetAddress", FileDescriptor.class, Integer.TYPE, String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (ioctlInt_FileDescriptor_int_MutableInt = Posix.class.getDeclaredMethod("ioctlInt", FileDescriptor.class, Integer.TYPE, MutableInt.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (isatty_FileDescriptor = Posix.class.getDeclaredMethod("isatty", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (kill_int_int = Posix.class.getDeclaredMethod("kill", Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (lchown_String_int_int = Posix.class.getDeclaredMethod("lchown", String.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (listen_FileDescriptor_int = Posix.class.getDeclaredMethod("listen", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (lseek_FileDescriptor_long_int = Posix.class.getDeclaredMethod("lseek", FileDescriptor.class, Long.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (lstat_String = Posix.class.getDeclaredMethod("lstat", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (mincore_long_long_byte = Posix.class.getDeclaredMethod("mincore", Long.TYPE, Long.TYPE, byte[].class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (mkdir_String_int = Posix.class.getDeclaredMethod("mkdir", String.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (mlock_long_long = Posix.class.getDeclaredMethod("mlock", Long.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (mmap_long_long_int_int_FileDescriptor_long = Posix.class.getDeclaredMethod("mmap", Long.TYPE, Long.TYPE, Integer.TYPE, Integer.TYPE, FileDescriptor.class, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (msync_long_long_int = Posix.class.getDeclaredMethod("msync", Long.TYPE, Long.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (munlock_long_long = Posix.class.getDeclaredMethod("munlock", Long.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (munmap_long_long = Posix.class.getDeclaredMethod("munmap", Long.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (open_String_int_int = Posix.class.getDeclaredMethod("open", String.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pipe = Posix.class.getDeclaredMethod("pipe")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (poll_StructPollfd_int = Posix.class.getDeclaredMethod("poll", StructPollfd[].class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pread_FileDescriptor_ByteBuffer_long = Posix.class.getDeclaredMethod("pread", FileDescriptor.class, ByteBuffer.class, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pread_FileDescriptor_byte_int_int_long = Posix.class.getDeclaredMethod("pread", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pwrite_FileDescriptor_ByteBuffer_long = Posix.class.getDeclaredMethod("pwrite", FileDescriptor.class, ByteBuffer.class, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pwrite_FileDescriptor_byte_int_int_long = Posix.class.getDeclaredMethod("pwrite", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (read_FileDescriptor_ByteBuffer = Posix.class.getDeclaredMethod("read", FileDescriptor.class, ByteBuffer.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (read_FileDescriptor_byte_int_int = Posix.class.getDeclaredMethod("read", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (readv_FileDescriptor_Object_int_int = Posix.class.getDeclaredMethod("readv", FileDescriptor.class, Object[].class, int[].class, int[].class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (recvfrom_FileDescriptor_ByteBuffer_int_InetSocketAddress = Posix.class.getDeclaredMethod("recvfrom", FileDescriptor.class, ByteBuffer.class, Integer.TYPE, InetSocketAddress.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (recvfrom_FileDescriptor_byte_int_int_int_InetSocketAddress = Posix.class.getDeclaredMethod("recvfrom", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, InetSocketAddress.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (remove_String = Posix.class.getDeclaredMethod("remove", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (rename_String_String = Posix.class.getDeclaredMethod("rename", String.class, String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (sendfile_FileDescriptor_FileDescriptor_MutableLong_long = Posix.class.getDeclaredMethod("sendfile", FileDescriptor.class, FileDescriptor.class, MutableLong.class, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (sendto_FileDescriptor_ByteBuffer_int_InetAddress_int = Posix.class.getDeclaredMethod("sendto", FileDescriptor.class, ByteBuffer.class, Integer.TYPE, InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (sendto_FileDescriptor_byte_int_int_int_InetAddress_int = Posix.class.getDeclaredMethod("sendto", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setegid_int = Posix.class.getDeclaredMethod("setegid", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setenv_String_String_boolean = Posix.class.getDeclaredMethod("setenv", String.class, String.class, Boolean.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (seteuid_int = Posix.class.getDeclaredMethod("seteuid", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setgid_int = Posix.class.getDeclaredMethod("setgid", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsid = Posix.class.getDeclaredMethod("setsid")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptByte_FileDescriptor_int_int_int = Posix.class.getDeclaredMethod("setsockoptByte", FileDescriptor.class, Integer.TYPE, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptGroupReq_FileDescriptor_int_int_StructGroupReq = Posix.class.getDeclaredMethod("setsockoptGroupReq", FileDescriptor.class, Integer.TYPE, Integer.TYPE, StructGroupReq.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptIfreq_FileDescriptor_int_int_String = Posix.class.getDeclaredMethod("setsockoptIfreq", FileDescriptor.class, Integer.TYPE, Integer.TYPE, String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptInt_FileDescriptor_int_int_int = Posix.class.getDeclaredMethod("setsockoptInt", FileDescriptor.class, Integer.TYPE, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptIpMreqn_FileDescriptor_int_int_int = Posix.class.getDeclaredMethod("setsockoptIpMreqn", FileDescriptor.class, Integer.TYPE, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptLinger_FileDescriptor_int_int_StructLinger = Posix.class.getDeclaredMethod("setsockoptLinger", FileDescriptor.class, Integer.TYPE, Integer.TYPE, StructLinger.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setsockoptTimeval_FileDescriptor_int_int_StructTimeval = Posix.class.getDeclaredMethod("setsockoptTimeval", FileDescriptor.class, Integer.TYPE, Integer.TYPE, StructTimeval.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (setuid_int = Posix.class.getDeclaredMethod("setuid", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (shutdown_FileDescriptor_int = Posix.class.getDeclaredMethod("shutdown", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (socket_int_int_int = Posix.class.getDeclaredMethod("socket", Integer.TYPE, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (socketpair_int_int_int_FileDescriptor_FileDescriptor = Posix.class.getDeclaredMethod("socketpair", Integer.TYPE, Integer.TYPE, Integer.TYPE, FileDescriptor.class, FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (stat_String = Posix.class.getDeclaredMethod("stat", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (statvfs_String = Posix.class.getDeclaredMethod("statvfs", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (strerror_int = Posix.class.getDeclaredMethod("strerror", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (strsignal_int = Posix.class.getDeclaredMethod("strsignal", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (symlink_String_String = Posix.class.getDeclaredMethod("symlink", String.class, String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (sysconf_int = Posix.class.getDeclaredMethod("sysconf", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (tcdrain_FileDescriptor = Posix.class.getDeclaredMethod("tcdrain", FileDescriptor.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (tcsendbreak_FileDescriptor_int = Posix.class.getDeclaredMethod("tcsendbreak", FileDescriptor.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (umask_int = Posix.class.getDeclaredMethod("umask", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (uname = Posix.class.getDeclaredMethod("uname")).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (unsetenv_String = Posix.class.getDeclaredMethod("unsetenv", String.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (waitpid_int_MutableInt_int = Posix.class.getDeclaredMethod("waitpid", Integer.TYPE, MutableInt.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (write_FileDescriptor_ByteBuffer = Posix.class.getDeclaredMethod("write", FileDescriptor.class, ByteBuffer.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (write_FileDescriptor_byte_int_int = Posix.class.getDeclaredMethod("write", FileDescriptor.class, byte[].class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (writev_FileDescriptor_Object_int_int = Posix.class.getDeclaredMethod("writev", FileDescriptor.class, Object[].class, int[].class, int[].class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (preadBytes_FileDescriptor_Object_int_int_long = Posix.class.getDeclaredMethod("preadBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (pwriteBytes_FileDescriptor_Object_int_int_long = Posix.class.getDeclaredMethod("pwriteBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE, Long.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (readBytes_FileDescriptor_Object_int_int = Posix.class.getDeclaredMethod("readBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (recvfromBytes_FileDescriptor_Object_int_int_int_InetSocketAddress = Posix.class.getDeclaredMethod("recvfromBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, InetSocketAddress.class)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (sendtoBytes_FileDescriptor_Object_int_int_int_InetAddress_int = Posix.class.getDeclaredMethod("sendtoBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, InetAddress.class, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (umaskImpl_int = Posix.class.getDeclaredMethod("umaskImpl", Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }
      try {
        (writeBytes_FileDescriptor_Object_int_int = Posix.class.getDeclaredMethod("writeBytes", FileDescriptor.class, Object.class, Integer.TYPE, Integer.TYPE)).setAccessible(true); 
      } catch (ReflectiveOperationException ex0) {
        Throwable ex = ex0;
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace(); 
      }    
    }
  }
  
  public static FileDescriptor accept(FileDescriptor fd, InetSocketAddress peerAddress)
  {
    try {
      return (FileDescriptor)
        accept_FileDescriptor_InetSocketAddress.invoke(POSIX, fd, peerAddress);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static boolean access(String path, int mode)
  {
    try {
      return (boolean)
        access_String_int.invoke(POSIX, path, mode);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void bind(FileDescriptor fd, InetAddress address, int port)
  {
    try {
      bind_FileDescriptor_InetAddress_int.invoke(POSIX, fd, address, port);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void chmod(String path, int mode)
  {
    try {
      chmod_String_int.invoke(POSIX, path, mode);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void chownchown(String path, int uid, int gid)
  {
    try {
      chown_String_int_int.invoke(POSIX, path, uid, gid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void close(FileDescriptor fd) {
    if (JRE) {
      if (fd == null) throw new IllegalArgumentException("fd == null");
      final int fdNo = ((Integer) Reflect.getfldval(fd, "fd")).intValue();
      final AutoCloseable ac = closeables.get(fdNo);
      try {
        ac.close();
      } catch (final Exception ioe) {
        ioe.printStackTrace();
      } finally {
        closeables.remove(fdNo);
      }
      return;
    }
    
    try {
      close_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void connect(FileDescriptor fd, InetAddress address, int port)
  {
    try {
      connect_FileDescriptor_InetAddress_int.invoke(POSIX, fd, address, port);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static FileDescriptor dup(FileDescriptor oldFd)
  {
    try {
      return (FileDescriptor)
        dup_FileDescriptor.invoke(POSIX, oldFd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static FileDescriptor dup2(FileDescriptor oldFd, int newFd)
  {
    try {
      return (FileDescriptor)
        dup2_FileDescriptor_int.invoke(POSIX, oldFd, newFd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String[] environ()
  {
    try {
      return (String[])
        environ.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void execv(String filename, String[] argv)
  {
    try {
      execv_String_String.invoke(POSIX, filename, argv);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void execve(String filename, String[] argv, String[] envp)
  {
    try {
      execve_String_String_String.invoke(POSIX, filename, argv, envp);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void fchmod(FileDescriptor fd, int mode)
  {
    try {
      fchmod_FileDescriptor_int.invoke(POSIX, fd, mode);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void fchown(FileDescriptor fd, int uid, int gid)
  {
    try {
      fchown_FileDescriptor_int_int.invoke(POSIX, fd, uid, gid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int fcntlFlock(FileDescriptor fd, int cmd, StructFlock arg)
  {
    try {
      return (int)
        fcntlFlock_FileDescriptor_int_StructFlock.invoke(POSIX, fd, cmd, arg);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int fcntlLong(FileDescriptor fd, int cmd, long arg)
  {
    try {
      return (int)
        fcntlLong_FileDescriptor_int_long.invoke(POSIX, fd, cmd, arg);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int fcntlVoid(FileDescriptor fd, int cmd)
  {
    try {
      return (int)
        fcntlVoid_FileDescriptor_int.invoke(POSIX, fd, cmd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void fdatasync(FileDescriptor fd)
  {
    try {
      fdatasync_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructStat fstat(FileDescriptor fd)
  {
    try {
      return (StructStat)
        fstat_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructStatVfs fstatvfs(FileDescriptor fd)
  {
    try {
      return (StructStatVfs)
        fstatvfs_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void fsync(FileDescriptor fd)
  {
    try {
      fsync_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void ftruncate(FileDescriptor fd, long length)
  {
    try {
      ftruncate_FileDescriptor_long.invoke(POSIX, fd, length);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String gai_strerror(int error)
  {
    try {
      return (String)
        gai_strerror_int.invoke(POSIX, error);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static InetAddress[] getaddrinfo(String node, StructAddrinfo hints)
  {
    try {
      return (InetAddress[])
        getaddrinfo_String_StructAddrinfo.invoke(POSIX, node, hints);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getegid()
  {
    try {
      return (int)
        getegid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String getenv(String name)
  {
    try {
      return (String)
        getenv_String.invoke(POSIX, name);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int geteuid()
  {
    try {
      return (int)
        geteuid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getgid()
  {
    try {
      return (int)
        getgid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String getnameinfo(InetAddress address, int flags)
  {
    try {
      return (String)
        getnameinfo_InetAddress_int.invoke(POSIX, address, flags);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static SocketAddress getpeername(FileDescriptor fd)
  {
    try {
      return (SocketAddress)
        getpeername_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getpid()
  {
    try {
      return (int)
        getpid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getppid()
  {
    try {
      return (int)
        getppid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructPasswd getpwnam(String name)
  {
    try {
      return (StructPasswd)
        getpwnam_String.invoke(POSIX, name);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructPasswd getpwuid(int uid)
  {
    try {
      return (StructPasswd)
        getpwuid_int.invoke(POSIX, uid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static SocketAddress getsockname(FileDescriptor fd)
  {
    try {
      return (SocketAddress)
        getsockname_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getsockoptByte(FileDescriptor fd, int level, int option)
  {
    try {
      return (int)
        getsockoptByte_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static InetAddress getsockoptInAddr(FileDescriptor fd, int level, int option)
  {
    try {
      return (InetAddress)
        getsockoptInAddr_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getsockoptInt(FileDescriptor fd, int level, int option)
  {
    try {
      return (int)
        getsockoptInt_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructLinger getsockoptLinger(FileDescriptor fd, int level, int option)
  {
    try {
      return (StructLinger)
        getsockoptLinger_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructTimeval getsockoptTimeval(FileDescriptor fd, int level, int option)
  {
    try {
      return (StructTimeval)
        getsockoptTimeval_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructUcred getsockoptUcred(FileDescriptor fd, int level, int option)
  {
    try {
      return (StructUcred)
        getsockoptUcred_FileDescriptor_int_int.invoke(POSIX, fd, level, option);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int gettid()
  {
    try {
      return (int)
        gettid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int getuid()
  {
    try {
      return (int)
        getuid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String if_indextoname(int index)
  {
    try {
      return (String)
        if_indextoname_int.invoke(POSIX, index);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static InetAddress inet_pton(int family, String address)
  {
    try {
      return (InetAddress)
        inet_pton_int_String.invoke(POSIX, family, address);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static InetAddress ioctlInetAddress(FileDescriptor fd, int cmd, String interfaceName)
  {
    try {
      return (InetAddress)
        ioctlInetAddress_FileDescriptor_int_String.invoke(POSIX, fd, cmd, interfaceName);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int ioctlInt(FileDescriptor fd, int cmd, MutableInt arg)
  {
    try {
      return (int)
        ioctlInt_FileDescriptor_int_MutableInt.invoke(POSIX, fd, cmd, arg);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static boolean isatty(FileDescriptor fd)
  {
    try {
      return (boolean)
        isatty_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void kill(int pid, int signal)
  {
    try {
      kill_int_int.invoke(POSIX, pid, signal);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void lchown(String path, int uid, int gid)
  {
    try {
      lchown_String_int_int.invoke(POSIX, path, uid, gid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void listen(FileDescriptor fd, int backlog)
  {
    try {
      listen_FileDescriptor_int.invoke(POSIX, fd, backlog);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static long lseek(FileDescriptor fd, long offset, int whence)
  {
    try {
      return (long)
        lseek_FileDescriptor_long_int.invoke(POSIX, fd, offset, whence);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructStat lstat(String path)
  {
    try {
      return (StructStat)
        lstat_String.invoke(POSIX, path);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void mincore(long address, long byteCount, byte[] vector)
  {
    try {
      mincore_long_long_byte.invoke(POSIX, address, byteCount, vector);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void mkdir(String path, int mode)
  {
    try {
      mkdir_String_int.invoke(POSIX, path, mode);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void mlock(long address, long byteCount)
  {
    try {
      mlock_long_long.invoke(POSIX, address, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset)
  {
    try {
      return (long)
        mmap_long_long_int_int_FileDescriptor_long.invoke(POSIX, address, byteCount, prot, flags, fd, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void msync(long address, long byteCount, int flags)
  {
    try {
      msync_long_long_int.invoke(POSIX, address, byteCount, flags);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void munlock(long address, long byteCount)
  {
    try {
      munlock_long_long.invoke(POSIX, address, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void munmap(long address, long byteCount)
  {
    try {
      munmap_long_long.invoke(POSIX, address, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  
  
  public static StandardOpenOption[] toOpenOptions(final int mode) {
    final List<StandardOpenOption> options = new ArrayList<>();
    final int accmode = mode & O_ACCMODE;
    switch (accmode) {
      case O_RDONLY:
      case O_RDWR:
        options.add(StandardOpenOption.READ);
        if (accmode == O_RDONLY) break;
        // fallthrough
      case O_WRONLY:
        options.add(StandardOpenOption.WRITE);
        break;
    }
    if ((mode & O_CREAT)   != 0) options.add(StandardOpenOption.CREATE);
    if ((mode & O_EXCL)    != 0) options.add(StandardOpenOption.CREATE_NEW);
    if ((mode & O_TRUNC)   != 0) options.add(StandardOpenOption.TRUNCATE_EXISTING);
    if ((mode & O_APPEND)  != 0) options.add(StandardOpenOption.APPEND);
    if ((mode & O_SYNC)    != 0) options.add(StandardOpenOption.SYNC);
    if ((mode & O_DSYNC)   != 0) options.add(StandardOpenOption.DSYNC);
    if ((mode & O_NOFOLLOW) != 0) {
      throw new UnsupportedOperationException(String.format(
        "Attempted open() call (mode = 0x%X) with %s (0x%X) option flag set",
        mode, "O_NOFOLLOW", O_NOFOLLOW
      ));
    } else if ((mode & O_NONBLOCK) != 0) {
      throw new UnsupportedOperationException(String.format(
        "Attempted open() call (mode = 0x%X) with %s (0x%X) option flag set",
        mode, "O_NONBLOCK", O_NONBLOCK
      ));
    } else if ((mode & O_NOCTTY) != 0) {
      throw new UnsupportedOperationException(String.format(
        "Attempted open() call (mode = 0x%X) with %s (0x%X) option flag set",
        mode, "O_NOCTTY", O_NOCTTY
      ));
    }
    return options.toArray(new StandardOpenOption[0]);
  }
  
  
  
  public static FileDescriptor open(String path, int flags, int mode) {
    if (JRE) {
      try {
        final StandardOpenOption[] openOptions = toOpenOptions(mode);
        
        final SeekableByteChannel ch = Files.newByteChannel(
          Paths.get(new File(path).getAbsoluteFile().getCanonicalFile().getPath()),
          openOptions
        );
        final FileDescriptor fd = Reflect.getfldval(ch, "fd");
        final int fdNo = ((Integer) Reflect.getfldval(fd, "fd")).intValue();
        closeables.put(fdNo, ch);
        return fd;
      } catch (final IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    try {
      return (FileDescriptor)
        open_String_int_int.invoke(POSIX, path, flags, mode);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static FileDescriptor[] pipe()
  {
    try {
      return (FileDescriptor[])
        pipe.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int poll(StructPollfd[] fds, int timeoutMs)
  {
    try {
      return (int)
        poll_StructPollfd_int.invoke(POSIX, fds, timeoutMs);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int pread(FileDescriptor fd, ByteBuffer buffer, long offset)
  {
    try {
      return (int)
        pread_FileDescriptor_ByteBuffer_long.invoke(POSIX, fd, buffer, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int pread(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset)
  {
    try {
      return (int)
        pread_FileDescriptor_byte_int_int_long.invoke(POSIX, fd, bytes, byteOffset, byteCount, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int pwrite(FileDescriptor fd, ByteBuffer buffer, long offset)
  {
    try {
      return (int)
        pwrite_FileDescriptor_ByteBuffer_long.invoke(POSIX, fd, buffer, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int pwrite(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, long offset)
  {
    try {
      return (int)
        pwrite_FileDescriptor_byte_int_int_long.invoke(POSIX, fd, bytes, byteOffset, byteCount, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int read(FileDescriptor fd, ByteBuffer buffer)
  {
    try {
      return (int)
        read_FileDescriptor_ByteBuffer.invoke(POSIX, fd, buffer);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount)
  {
    try {
      return (int)
        read_FileDescriptor_byte_int_int.invoke(POSIX, fd, bytes, byteOffset, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int readv(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts)
  {
    try {
      return (int)
        readv_FileDescriptor_Object_int_int.invoke(POSIX, fd, buffers, offsets, byteCounts);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int recvfrom(FileDescriptor fd, ByteBuffer buffer, int flags, InetSocketAddress srcAddress)
  {
    try {
      return (int)
        recvfrom_FileDescriptor_ByteBuffer_int_InetSocketAddress.invoke(POSIX, fd, buffer, flags, srcAddress);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int recvfrom(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetSocketAddress srcAddress)
  {
    try {
      return (int)
        recvfrom_FileDescriptor_byte_int_int_int_InetSocketAddress.invoke(POSIX, fd, bytes, byteOffset, byteCount, flags, srcAddress);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void remove(String path)
  {
    try {
      remove_String.invoke(POSIX, path);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void rename(String oldPath, String newPath)
  {
    try {
      rename_String_String.invoke(POSIX, oldPath, newPath);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static long sendfile(FileDescriptor outFd, FileDescriptor inFd, MutableLong inOffset, long byteCount)
  {
    try {
      return (long)
        sendfile_FileDescriptor_FileDescriptor_MutableLong_long.invoke(POSIX, outFd, inFd, inOffset, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int sendto(FileDescriptor fd, ByteBuffer buffer, int flags, InetAddress inetAddress, int port)
  {
    try {
      return (int)
        sendto_FileDescriptor_ByteBuffer_int_InetAddress_int.invoke(POSIX, fd, buffer, flags, inetAddress, port);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int sendto(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount, int flags, InetAddress inetAddress, int port)
  {
    try {
      return (int)
        sendto_FileDescriptor_byte_int_int_int_InetAddress_int.invoke(POSIX, fd, bytes, byteOffset, byteCount, flags, inetAddress, port);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setegid(int egid)
  {
    try {
      setegid_int.invoke(POSIX, egid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setenv(String name, String value, boolean overwrite)
  {
    try {
      setenv_String_String_boolean.invoke(POSIX, name, value, overwrite);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void seteuid(int euid)
  {
    try {
      seteuid_int.invoke(POSIX, euid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setgid(int gid)
  {
    try {
      setgid_int.invoke(POSIX, gid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int setsid()
  {
    try {
      return (int)
        setsid.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptByte(FileDescriptor fd, int level, int option, int value)
  {
    try {
      setsockoptByte_FileDescriptor_int_int_int.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptGroupReq(FileDescriptor fd, int level, int option, StructGroupReq value)
  {
    try {
      setsockoptGroupReq_FileDescriptor_int_int_StructGroupReq.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptIfreq(FileDescriptor fd, int level, int option, String value)
  {
    try {
      setsockoptIfreq_FileDescriptor_int_int_String.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptInt(FileDescriptor fd, int level, int option, int value)
  {
    try {
      setsockoptInt_FileDescriptor_int_int_int.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptIpMreqn(FileDescriptor fd, int level, int option, int value)
  {
    try {
      setsockoptIpMreqn_FileDescriptor_int_int_int.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptLinger(FileDescriptor fd, int level, int option, StructLinger value)
  {
    try {
      setsockoptLinger_FileDescriptor_int_int_StructLinger.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setsockoptTimeval(FileDescriptor fd, int level, int option, StructTimeval value)
  {
    try {
      setsockoptTimeval_FileDescriptor_int_int_StructTimeval.invoke(POSIX, fd, level, option, value);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void setuid(int uid)
  {
    try {
      setuid_int.invoke(POSIX, uid);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void shutdown(FileDescriptor fd, int how)
  {
    try {
      shutdown_FileDescriptor_int.invoke(POSIX, fd, how);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static FileDescriptor socket(int domain, int type, int protocol)
  {
    try {
      return (FileDescriptor)
        socket_int_int_int.invoke(POSIX, domain, type, protocol);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void socketpair(int domain, int type, int protocol, FileDescriptor fd1, FileDescriptor fd2)
  {
    try {
      socketpair_int_int_int_FileDescriptor_FileDescriptor.invoke(POSIX, domain, type, protocol, fd1, fd2);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructStat stat(String path)
  {
    try {
      return (StructStat)
        stat_String.invoke(POSIX, path);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructStatVfs statvfs(String path)
  {
    try {
      return (StructStatVfs)
        statvfs_String.invoke(POSIX, path);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String strerror(int errno)
  {
    try {
      return (String)
        strerror_int.invoke(POSIX, errno);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String strsignal(int signal)
  {
    try {
      return (String)
        strsignal_int.invoke(POSIX, signal);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void symlink(String oldPath, String newPath)
  {
    try {
      symlink_String_String.invoke(POSIX, oldPath, newPath);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static long sysconf(int name)
  {
    try {
      return (long)
        sysconf_int.invoke(POSIX, name);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void tcdrain(FileDescriptor fd)
  {
    try {
      tcdrain_FileDescriptor.invoke(POSIX, fd);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void tcsendbreak(FileDescriptor fd, int duration)
  {
    try {
      tcsendbreak_FileDescriptor_int.invoke(POSIX, fd, duration);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int umask(int mask)
  {
    try {
      return (int)
        umask_int.invoke(POSIX, mask);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static StructUtsname uname()
  {
    try {
      return (StructUtsname)
        uname.invoke(POSIX);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static void unsetenv(String name)
  {
    try {
      unsetenv_String.invoke(POSIX, name);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int waitpid(int pid, MutableInt status, int options)
  {
    try {
      return (int)
        waitpid_int_MutableInt_int.invoke(POSIX, pid, status, options);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int write(FileDescriptor fd, ByteBuffer buffer)
  {
    try {
      return (int)
        write_FileDescriptor_ByteBuffer.invoke(POSIX, fd, buffer);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount)
  {
    try {
      return (int)
        write_FileDescriptor_byte_int_int.invoke(POSIX, fd, bytes, byteOffset, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int writev(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts)
  {
    try {
      return (int)
        writev_FileDescriptor_Object_int_int.invoke(POSIX, fd, buffers, offsets, byteCounts);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int preadBytes(FileDescriptor fd, Object buffer, int bufferOffset, int byteCount, long offset)
  {
    try {
      return (int)
        preadBytes_FileDescriptor_Object_int_int_long.invoke(POSIX, fd, buffer, bufferOffset, byteCount, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int pwriteBytes(FileDescriptor fd, Object buffer, int bufferOffset,
    int byteCount, long offset)
  {
    if (JRE) {
      try {
        if (buffer == null) throw new IllegalArgumentException("buffer == null");
        final int fdNo = ((Integer) Reflect.getfldval(fd, "fd")).intValue();
        final SeekableByteChannel ch = (SeekableByteChannel) closeables.get(fdNo);
        long origPos = ch.position();
        try {
          ch.position(offset);
          if (buffer instanceof ByteBuffer) {
            return ch.write(
              (ByteBuffer) ((ByteBuffer) buffer)
                .duplicate().position(bufferOffset).limit(byteCount)
            );
          } else if (buffer instanceof byte[]) {
            return ch.write(
              (ByteBuffer) ByteBuffer.wrap((byte[]) buffer)
                .duplicate().position(bufferOffset).limit(byteCount)
            );
          } else {
            throw new UnsupportedOperationException(buffer.getClass().getName());
          }
        } finally {
          ch.position(origPos);
        }
      } catch (final IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    
    
    try {
      return (int)
        pwriteBytes_FileDescriptor_Object_int_int_long.invoke(POSIX, fd, buffer, bufferOffset, byteCount, offset);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int readBytes(FileDescriptor fd, Object buffer, int offset, int byteCount)
  {
    try {
      return (int)
        readBytes_FileDescriptor_Object_int_int.invoke(POSIX, fd, buffer, offset, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int recvfromBytes(FileDescriptor fd, Object buffer, int byteOffset, int byteCount, int flags, InetSocketAddress srcAddress)
  {
    try {
      return (int)
        recvfromBytes_FileDescriptor_Object_int_int_int_InetSocketAddress.invoke(POSIX, fd, buffer, byteOffset, byteCount, flags, srcAddress);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int sendtoBytes(FileDescriptor fd, Object buffer, int byteOffset, int byteCount, int flags, InetAddress inetAddress, int port)
  {
    try {
      return (int)
        sendtoBytes_FileDescriptor_Object_int_int_int_InetAddress_int.invoke(POSIX, fd, buffer, byteOffset, byteCount, flags, inetAddress, port);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int umaskImpl(int mask)
  {
    try {
      return (int)
        umaskImpl_int.invoke(POSIX, mask);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static int writeBytes(FileDescriptor fd, Object buffer, int offset, int byteCount)
  {
    try {
      return (int)
        writeBytes_FileDescriptor_Object_int_int.invoke(POSIX, fd, buffer, offset, byteCount);
    } catch (ReflectiveOperationException ex0) {
      Throwable ex = ex0;
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException) ex).getTargetException();
      }
      Reflector.Util.sneakyThrow(ex);
      throw new RuntimeException(); 
    }
  }
  
  public static String errnoName(int errno) {
    return OsConstants.errnoName(errno);
  }
  
  public static String errnoString(int errno) {
    return String.format(
      "%s (%s)", strerror(errno), errnoName(errno)
    );
  }
  
  public static Set<String> oflagNames(int oflags) {
    return flagNames(PosixUtil.class, "O_", oflags);
  }
  
  public static Set<String> fmodeNames(int fmode) {
    return flagNames(PosixUtil.class, "F_", fmode);
  }
  
  public static Set<String> fileTypeNames(int fmode) {
    int ifmt = (fmode & S_IFMT);
    return flagNames(PosixUtil.class, "S_IF", ifmt);
  }
  
  public static
  Set<String> flagNames(Class<?> cls, String prefix, long bits) {
    Set<String> names = new HashSet<String>();
    for (Field fld: cls.getDeclaredFields()) {
      if (! Modifier.isStatic(fld.getModifiers())) continue; //;
      Class<?> type = fld.getType();
      if (! ClassUtils.isPrimitiveOrWrapper(type)) continue;
      String name = fld.getName();
      if (! name.startsWith(prefix)) continue;
      
      Object val;
      try {
        fld.setAccessible(true);
        val = fld.get(null);
      } catch (IllegalAccessException iae) {
        if ("true".equals(System.getProperty("printStackTrace"))) iae.printStackTrace();
        continue;
      }
      long longVal = ((Number) val).longValue();
      if ((bits & longVal) == 0) continue; 
      names.add(name);
    }
    return names;
  }  
  
  public static
  Map<Integer, String> flagMap(Class<?> cls, String prefix, 
  long bits) 
  {
    Map<Integer, String> map = new TreeMap<Integer, String>();
    for (Field fld: cls.getDeclaredFields()) {
      if (! Modifier.isStatic(fld.getModifiers())) continue; //;
      Class<?> type = fld.getType();
      if (! ClassUtils.isPrimitiveOrWrapper(type)) continue;
      String name = fld.getName();
      if (prefix != null && ! name.startsWith(prefix)) continue;
      
      Object val;
      try {
        fld.setAccessible(true);
        val = fld.get(null);
      } catch (IllegalAccessException iae) {
        if ("true".equals(System.getProperty("printStackTrace"))) iae.printStackTrace();
        continue;
      }
      long longVal = ((Number) val).longValue();
      if ((bits & longVal) != longVal) continue; 
      map.put(Integer.valueOf((int) longVal), name);
    }
    return map;
  }  
}



