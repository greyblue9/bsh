package org.d6r;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.*;
import org.apache.commons.io.HexDump;
import libcore.icu.NativeConverter;
import static org.d6r.ByteUtils.*;

class CpioEntry extends TarEntry {
  
  @Override
  public char getType() {
    if (cachedType != null) return cachedType.charValue();
    int mode = (int) getLong("mode");
    int i = -4; 
    Character glyph = null;
    while (i < CpioFile.modes.length - 4) { 
      i += 4; 
      int mask = ((Integer) CpioFile.modes[i]).intValue(); 
      if ((mask & mode) == 0) continue; 
      glyph = (Character) CpioFile.modes[i+1]; 
      if (glyph != null) { 
        cachedType = glyph;
        return glyph.charValue();
      } 
    }
    return (cachedType = (glyph = Character.valueOf('-')));
  }
  
  
  @Override 
  public String toString() {
    if (cachedToString != null) return cachedToString;
    Date date = lastModified();
    String dateStr = (date == BAD_DATE)
      ? "Unknown Date": date.toLocaleString();
    long mode = getMode();
    return (cachedToString = String.format(
      "%c%-9s %5s/%-4d %10d %20s %s %s",       
      getType(),
      mode != -1
        ? getModeString(mode)
        : String.format("[mode=%s]", get("mode")),
      get("uid"),
      get("gid") instanceof Long?  (Long) get("gid"):  0,
      get("size") instanceof Long? (Long) get("size"): 0,
      dateStr,
      tryGetString("prefix", ""),
      tryGetString("name", "?")
      //, tryGetString("linkname", "")
    ));
  }
  
  
  public long getLong(String key) {
    Object val = get(key);
    if (val == null) return 0L;
    if (val instanceof Long) {
      return ((Long)val).longValue();
    }
    if (val instanceof Integer) {
      return (long) ((Integer)val).intValue();
    }
    if (val instanceof Short) {
      return (long) ((Short)val).shortValue();
    }
    if (val instanceof Character) {
      return (long) ((Character)val).charValue();
    }
    if (val instanceof Byte) {
      return (long) ((Byte)val).byteValue();
    }
    if (val instanceof Double) {
      return (long) ((Double)val).doubleValue();
    }
    if (val instanceof Float) {
      return (long) ((Float)val).floatValue();
    }
    if (val instanceof byte[]) {
      val = CpioFile.stringFromBytes((byte[]) val)
                .replaceAll("[^0-9a-fA-F]", "");
    }
    if (val instanceof String) {
      Throwable ex1 = null;
      try {
        return Long.valueOf((String)val, 16).longValue();
      } catch (Exception ex) { ex1 = ex; }
      try {
        return Long.valueOf((String)val).longValue();
      } catch (Exception ex) {}
      try {
        return Long.valueOf((String)val, 8).longValue();
      } catch (Exception ex) {}      
      (new RuntimeException(
        "CpioEntry.getLong(): "
        + "Cannot turn String into long value", ex1
      )).printStackTrace();
    }
    try {
      return Long.valueOf(val.toString());
    } catch (Throwable e) { 
      (new RuntimeException(String.format(
        "CpioEntry.getLong(): "
        + "Cannot turn %s into long value via toString()", 
        val.getClass().getSimpleName()
      ), e)).printStackTrace();
    }
    return 0L;
  }

}

public class CpioFile implements List<CpioEntry> {
  
  TarInputStream tar;
  byte[] block = new byte[512];
  
  Map<String, CpioEntry> entries;
  
  public static int SILENT = 0;
  public static int MINIMAL = 1;
  public static int NORMAL = 3;
  public static int DEBUG = 5;
  public static int TRACE = 10;
  public int verbosity = MINIMAL;
  
  public static final byte[] ZERO_BYTES = new byte[0];
  public boolean skipAllData = false;
  public long maxContentSize = 65535;
  
  public static Pattern PATH_TO_NAME_PTRN
    = Pattern.compile("^(.*[/\\\\])([^/\\\\]+)$");
  public static Matcher PATH_TO_NAME_MCHR 
    = PATH_TO_NAME_PTRN.matcher("");
  
  public static Pattern NON_PRINTABLE_PTRN
    = Pattern.compile("[^\\x09-\\x9f]");
  public static Matcher NON_PRINTABLE_MCHR 
    = NON_PRINTABLE_PTRN.matcher("");
  
  
  
  String srcName;
  String path;
  
  public CpioFile(InputStream stream) {
    this.path = TarInputStream.getPath(stream);
    
    if (this.path != null) {
      PATH_TO_NAME_MCHR.reset(this.path);
      this.srcName = (PATH_TO_NAME_MCHR.find())
        ? PATH_TO_NAME_MCHR.replaceAll("$2")
        : this.path;
    } else {
      this.srcName = stream.getClass().getSimpleName()
        .concat("@")
        .concat(
          String.valueOf(System.identityHashCode(stream))
        );
    }
    tar = new TarInputStream(stream);
    this.layout = getLayout(tar);
  }
  
  public CpioFile(File file) {
    try {
      this.tar = new TarInputStream(
        new PosixFileInputStream(file)
      );
    } catch (FileNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    this.path = file.getPath();
    this.srcName = file.getName();
    this.layout = getLayout(tar);
  }
  
  public CpioFile(String path) {
    try {
      this.tar = new TarInputStream(
        new PosixFileInputStream(path)
      );
    } catch (FileNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    this.path = path;
    
    PATH_TO_NAME_MCHR.reset(path);
    this.srcName = (PATH_TO_NAME_MCHR.find())
      ? PATH_TO_NAME_MCHR.replaceAll("$2")
      : this.path;

    this.layout = getLayout(tar);
  }
  
  public CpioFile(String path, String regex) {
    this(path);
    this.matcher = Pattern.compile(
      regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    ).matcher("");
  }
  
  public CpioFile(String path, Pattern ptrn) {
    this(path);
    this.matcher = ptrn.matcher("");
  }
  
  
  static final Object[] NEWC_HEADER = { 
    "magic", 0, 6,
    "ino", 6, 8,
    "mode", 14, 8,
    "uid", 22, 8,
    "gid", 30, 8,
    "nlink", 38, 8,
    "mtime", 46, 8,
    "size", 54, 8,
    "devmajor", 62, 8,
    "devminor", 70, 8,
    "rdevmajor", 78, 8,
    "rdevminor", 86, 8,
    "namesize", 94, 8,
    "check", 102, 8,
    "name", 110, "namesize",
    "content", -1, "size"
  };
  
  static final Object[] OLDC_HEADER = { 
    "magic", 0, 6,
    "dev", 6, 6,
    "ino", 12, 6,
    "mode", 18, 6,
    "uid", 24, 6,
    "gid", 30, 6,
    "nlink", 36, 6,
    "rdev", 42, 6,
    "mtime", 48, 11,
    "namesize", 59, 6,
    "size", 65, 11,
    "name", 76, "namesize",
    "content", -1, "size"
  };
  /**
  Bytes  Field Name
    2  magic
    2  dev
    2  ino
    2  mode
    2  uid
    2  gid
    2  nlink
    2  rdev
    4  mtime
    2  namesize
    4  filesize
  */
  static final Object[] OLDB_HEADER = { 
    "magic", 0, 2,
    "dev", 2, 2,
    "ino", 4, 2,
    "mode", 6, 2,
    "uid", 8, 2,
    "gid", 10, 2,
    "nlink", 12, 2,
    "rdev", 14, 2,
    "mtime", 16, 4,
    "namesize", 20, 2,
    "size", 22, 4,
    "name", 26, "namesize",
    "content", -1, "size"
  };
  
  public int getAlign() {
    if (layout == NEWC_HEADER) return 4;
    if (layout == OLDC_HEADER) return 0;
    if (layout == OLDB_HEADER) return 2;
    return 0;
  }
  
  public int getRadix() {
    if (layout == NEWC_HEADER) return 16;
    if (layout == OLDC_HEADER) return 8;
    if (layout == OLDB_HEADER) return 0;
    return 0;
  } 
  
  public String getFormatName() {
    if (layout == NEWC_HEADER) return "newc";
    if (layout == OLDC_HEADER) return "oldc";
    if (layout == OLDB_HEADER) return "binary";
    return "unknown";
  }
  
  public static Object[] getLayout(InputStream is) {
    Object[] retLayout;
    byte[] magic = new byte[6];
    try {
      is.mark(magic.length);
      is.read(magic, 0, magic.length);
      if ((char) magic[0] == '0') {
        if ((char) magic[5] == '1') {
          retLayout = NEWC_HEADER;          
        } else {
          retLayout = OLDC_HEADER;
        }
      } else {
        retLayout = OLDB_HEADER;
      }
      is.reset();
      return retLayout;
    } catch (IOException e) {
      throw new RuntimeException(e);  
    }
    /*throw new IllegalArgumentException(String.format(
      "Unknown header format: magic = %s",
      Arrays.toString(magic)
    ));*/
  }
  
  public Object[] layout = NEWC_HEADER;
  
  public static Object[] modes = new Object[]{ 
    0x0000400, null, "C_IRUSR", "read by owner",
    0x0000200, null, "C_IWUSR", "write by owner",
    0x0000100, null, "C_IXUSR", "execute by owner",
    0x0000040, null, "C_IRGRP", "read by group",
    0x0000020, null, "C_IWGRP", "write by group",
    0x0000010, null, "C_IXGRP", "execute by group",
    0x0000004, null, "C_IROTH", "read by others",
    0x0000002, null, "C_IWOTH", "write by others",
    0x0000001, null, "C_IXOTH", "execute by others",
    0x0004000, null, "C_ISUID", "set user ID",
    0x0002000, null, "C_ISGID", "set group ID",
    0x0001000, null, "C_ISVTX", "restricted deletion",
    0x0040000, 'd', "C_ISDIR", "directory",
    0x0010000, 'F', "C_ISFIFO", "FIFO",
    0x0100000, 'f', "C_ISREG", "regular file",
    0x0060000, 'b', "C_ISBLK", "block special",
    0x0020000, 'c', "C_ISCHR", "character special",
    0x0110000, null, "C_ISCTG", "reserved",
    0x0120000, 'l', "C_ISLNK", "symbolic link",
    0x0140000, 's', "C_ISSOCK", "socket"
  };

  static String ENCODING = "UTF-8";
  static byte[] EMPTY_BYTES = new byte[0];
  
  public static long ch;
  
  static {
    try {
      ch = ((Long)Reflect.findMethod(
        NativeConverter.class, "openConverter"
      ).invoke(null, "ASCII")).longValue();
    } catch (Throwable e) { 
      e.printStackTrace();
    }
  }
  
  
  public static String stringFromBytes(byte[] bytes) {
    try {
      int len = bytes.length; 
      char[] chrOut = new char[len]; 
      int result = NativeConverter.decode(
        ch, bytes, len, chrOut, len, new int[]{0, 0}, true
      ); 
      return String.valueOf(chrOut);
    } catch (Throwable e) { 
      System.err.println(e.toString());
    }
    return new String(bytes);
  }
  
  
  
  public void setVerbosity(int level) {
    this.verbosity = level;
  }
  
  CpioEntry entry;
  byte[] bytes;
  byte[] lookAhead;
  char[] charArray;
  String[] strArray;
  Object val;
  long total;
  ArrayList<Integer> backups = new ArrayList<Integer>();
  boolean done = false;
  int lastBackup = 0;
  int avail;
  Matcher matcher = null;
  
  
  public int read() {
    return read(Integer.MAX_VALUE);
  }
  
  public long pos = 0;
  public long mark = 0;
  
  
  public int read(int maxEnt) {
    if (entries == null) {
      entries = new ListOrderedMap<String, CpioEntry>();
    }
    int entriesRead = 0;
    if (done) return entriesRead;
    
    outer:
    while  // //(avail = tar.available()) >= 100
    (entriesRead < maxEnt && !done) 
    {
      mark = pos;
      //tar.mark(256);
      
      CpioEntry entry = (this.entry = new CpioEntry()); 
      entry.alignmentBoundary = getAlign();
      int radix = getRadix();
      int len = -1; 
      int i = -3;
      byte[] bytes;
      Object value;
      
      try {
        inner:
        while (i + 3 < layout.length && !done) { 
          i += 3; 
          
          String key = (String) layout[i];
          if (verbosity >= DEBUG) System.err.printf(
            "Read [%s]:", key
          );
          
          int offs = ((Integer) layout[i+1]).intValue();
          Object rawval = layout[i+2];
          if (rawval instanceof String) {
            rawval = entry.get((String) rawval);
          }
          if (rawval instanceof Integer) { 
            len = ((Integer) rawval).intValue();
          } else if (rawval instanceof Long) { 
            len = (int) ((Long) rawval).longValue();
          } else { 
            System.err.printf("rawval == %s\n", rawval);
            break outer;
          } // indirect len
          String entryName;
          if (len < 65536 
          || (matcher != null 
           && matcher.reset(
                (entryName = (String)entry.get("name"))
              ).matches()))
          {
            bytes = (this.bytes = new byte[len]); 
            int read = tar.read(bytes, 0, len);
            if (verbosity >= DEBUG) System.err.printf(
              "  read %d bytes: %s\n", 
              read, bytes.length < 128?
                      Arrays.toString(bytes):
                      bytes.toString()
            );
            if (read < 0) {
              System.err.printf("Read returned %d\n", read);
              break;
            }
            pos += read;
          } else {
            bytes = EMPTY_BYTES;
            int read = 0;
            while (read < len) {
              read += tar.skip(len - read);
            }
          }
          if (! "name".equals(key) 
          &&  ! "content".equals(key)) { 
            // decode ASCII hex number value
            if (radix != 0) {
              value = Long.valueOf(
                CpioFile.stringFromBytes(bytes), radix
              );
            } else {
              switch (bytes.length) {
                case 1:  value = bytesToLongLE(bytes);
                  break;
                case 2:  value = bytesToLongLE(bytes);
                  break;
                case 4:
                  value = bytesToLongLE(flipOrder(bytes));
                  break;
                default:
                  if (bytes.length % 4 == 0) {
                    value = bytesToLongLE(flipOrder(bytes));
                  } else {
                    value = bytesToLongLE(bytes);
                  }
              }
              
            }
            if  (verbosity >= DEBUG
            && !(value instanceof String)) {
              System.err.printf(
                "  parsed numeric value: %d\t0%o\t0x%x\n",
                value, value, value
              );
            }
            // Add the (numeric) value to the entry for key
            entry.put(key, value);
          } else { 
            // value is String or raw bytes
            String strv = len > 0
              ? CpioFile.stringFromBytes(bytes)
              : "";
            if (key.equals("name")) {
              strv = strv.substring(0, strv.length() - 1);
            }
            if (verbosity >= TRACE) {
              NON_PRINTABLE_MCHR.reset(strv);
              System.err.printf(
                "Parsed string value: \"%s\"\n",
                NON_PRINTABLE_MCHR.replaceAll("?")
              );
            }
            value = strv;
            // Add the (String) value to the entry for key 
            entry.put(key, value); 
            if (len > 0) {
              if (verbosity >= TRACE) HexDump.dump(
                bytes, 0, System.err, 0
              );
            } 
          
          }
          boolean isName, isContent;
          if (  (isName = "name".equals(key))
          || (isContent = "content".equals(key)))
          {
            if (isName 
            &&"TRAILER!!!".equals((String)entry.get("name")))
            {
              //System.err.println((String)entry.get("name"));
              done = true;
              break outer;
            }
            long toSkip = isName
              ? entry.getHeaderPadCount()
              : entry.getDataPadCount();
            if (toSkip != 0) {
              long skip = tar.skip(toSkip);
              if (verbosity >= DEBUG) System.err.printf(
                "[%s] skip data padding: %d/%d\n",
                key, skip, toSkip
              );
              if (skip < 0) {
                System.err.printf(
                  "Skip returned %d\n", skip);
                break;
              }
              pos += skip;
            }
          }
        } // while (i + 3 < layout.length)
      } catch (IOException ioEx) {
        System.err.println(ioEx.toString());
        ioEx.printStackTrace();        
        done = true;
        break;
      } catch (Throwable e) { 
        e.printStackTrace();
        break;
      }
      entries.put(entry.getName(), entry);
      entriesRead += 1;
      if (verbosity >= NORMAL) {
        System.out.println(entry.toString());        
      } else if (verbosity >= MINIMAL) {
        System.out.println(entry.getName());
      }
    }
    
    if (avail < 100) done = true;
    
    return entriesRead;
  }
  
  public void mark(int limit) {
    try {
      tar.mark(limit);
    } catch (Throwable e) { 
      throw new RuntimeException(e);     
    }
  }
  public void reset() {
    try {
      tar.reset();
    } catch (Throwable e) { 
      throw new RuntimeException(e);     
    }
  }
  
  
  public Map<String, ? extends CpioEntry> getEntries() {
    if (entries == null) {
      read();
    }
    return entries;
  }
  
  public static boolean equals(Object obj1, Object obj2) {
    if (obj1 == null || obj2 == null) return false;
    try {
      return obj1.equals(obj2);
    } catch (Throwable e) { }
    try {
      return obj1.hashCode() == obj2.hashCode();
    } catch (Throwable e) { }
    return System.identityHashCode(obj1)
        == System.identityHashCode(obj2);
  }
  
  public static void warn(Throwable ex, Class<?> cls,
  String input) 
  {
    System.err.printf(
      "[WARN] %s\n", 
      StringUtils.join(
        new Object[]{ ex, cls, input }, ": "
      )
    );
  }
  
  public CpioEntry getEntry(String name) {
    CpioEntry te = this.getEntries().get(name);
    if (te != null) return te;
     Pattern ptrn = Pattern.compile(name, 
        Pattern.CASE_INSENSITIVE 
      | Pattern.DOTALL 
      | Pattern.MULTILINE 
      | Pattern.UNIX_LINES
    );
    for (Map.Entry<String, CpioEntry> e: entries.entrySet()) 
    {
      String eName = e.getKey();
      if (ptrn.matcher(eName).matches()) {
        return e.getValue();
      }
    }
    return null;
  }
  
  public InputStream getInputStream(CpioEntry te) {
    byte[] content = te.getBytes();
    return new ByteArrayInputStream(content);
  }
  
  
  
  
  
  @Override
  public void add(int location, CpioEntry entry) {
    throw new UnsupportedOperationException();
    //getEntries().put(location, entry.getName(), entry);
  }
  @Override
  public boolean add(CpioEntry entry) {
    boolean contains 
      = getEntries().containsKey(entry.getName());
    entries.put(entry.getName(), entry);
    return contains;
  }
  @Override
  public boolean addAll(int location, Collection<? extends CpioEntry> toAdd) {
    throw new UnsupportedOperationException();
    /*Collections.addAll(
      getEntries().values(), toAdd.toArray(new CpioEntry[0])
    );*/
  }
  @Override
  public void clear() {
    if (entries != null) entries.clear();
  }
  @Override
  public boolean equals(Object object) {
    if (object == null) return false;
    if (!(object instanceof CpioFile)) return false;
    return System.identityHashCode(this)
        == System.identityHashCode(object);
  }
  @Override
  public CpioEntry get(int location) {
    return (CpioEntry) 
      getEntries().values().toArray()[location];
  }
  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }  
  @Override
  public int indexOf(Object object) {
    throw new UnsupportedOperationException();
    //return getEntries().values().indexOf(object);
  }
  @Override
  public Iterator<CpioEntry> iterator() {
    return (Iterator<CpioEntry>)
      (Iterator<?>) getEntries().values().iterator();
  }
  @Override
  public int lastIndexOf(Object object) {
    throw new UnsupportedOperationException();
    //return getEntries().values().lastIndexOf(object);
  }
  @Override
  public ListIterator<CpioEntry> listIterator() {
    throw new UnsupportedOperationException();
    //return getEntries().values().listIterator();
  }
  @Override
  public ListIterator<CpioEntry> listIterator(int location) {
    throw new UnsupportedOperationException();
    //return getEntries().values().listIterator(location);
  }
  @Override
  public CpioEntry remove(int location) {
    CpioEntry entry = get(location);
    remove(location);
    return entry;
  }
  @Override
  public CpioEntry set(int location, CpioEntry object) {
    throw new UnsupportedOperationException();
    //return getEntries().values().set(location, object);
  }
  @Override
  public List<CpioEntry> subList(int start, int end) {
    throw new UnsupportedOperationException();
    // return getEntries().values().subList(start, end);
  }
  
  @Override
  public boolean contains(Object object) {
    return getEntries().values().contains(object);
  }
  @Override
  public boolean containsAll(Collection<?> collection) {
    return getEntries().values().containsAll(collection);
  }
  
  @Override
  public boolean isEmpty() {
    return getEntries().values().isEmpty();
  }
  
  @Override
  public boolean removeAll(Collection<?> collection) {
    throw new UnsupportedOperationException();
    //return getEntries().values().removeAll(collection);
  }
  @Override
  public boolean retainAll(Collection<?> collection) {
    return getEntries().values().retainAll(collection);
  }
  @Override
  public int size() {
    return getEntries().values().size();
  }
  @Override
  public Object[] toArray() {
    return getEntries().values().toArray();
  }
  @Override
  public <E> E[] toArray(E[] contents) {
    return (E[]) getEntries().values().toArray(contents);
  }
  @Override
  public boolean addAll(Collection<? extends CpioEntry> toAdd)
  {
    Collections.addAll(this, toAdd.toArray(new CpioEntry[0]));
    return true;
  }
  @Override
  public boolean remove(Object toRemove) 
  {
    return getEntries().values().remove(toRemove);
  }
  @Override
  public String toString() {
    return String.format(
      "<CpioFile+%s (%s format): %s [%s] @%8x>",
      tar.getCompressionByReflection(tar),
      getFormatName(),
      srcName,
      entries != null
        ? String.format("%d entries", entries.size())
        : "Not yet parsed",
      hashCode()
    );
  }
  
}