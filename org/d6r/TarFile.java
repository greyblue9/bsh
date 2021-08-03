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


public class TarFile implements List<TarEntry> {
  
  TarInputStream tar;
  byte[] block = new byte[512];
  
  Map<String, TarEntry> entries;
  TarEntry extendedEntry;
  
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
  
  String srcName;
  String path;
  
  public TarFile(InputStream stream) {
    this.path = (stream instanceof FileInputStream)
      ? TarInputStream.getPath(stream): null;
    
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
  }
  
  public TarFile(File file) {
    this.tar = new TarInputStream(
      MarkableFileInputStream.from(file));
    this.path = file.getPath();
    this.srcName = file.getName();
  }
  
  public TarFile(String path) {
    this.tar = new TarInputStream(
      MarkableFileInputStream.from(path));
    this.path = path;
    
    PATH_TO_NAME_MCHR.reset(path);
    this.srcName = (PATH_TO_NAME_MCHR.find())
      ? PATH_TO_NAME_MCHR.replaceAll("$2")
      : this.path;
  }
  
  
  static final Object[] layout = { 
    "name", 0, 100, 
    "mode", 100, 8, 
    "uid", 108, 8,     
    "gid", 116, 8, 
    "size", 124, 12, 
    "mtime", 136, 12, 
    "chksum", 148, 8, 
    "typeflag", 156, 1, 
    "linkname", 157, 100, 
    "magic", 257, 6, 
    "version", 263, 2, 
    "uname", 265, 32, 
    "gname", 297, 32, 
    "devmajor", 329, 8, 
    "devminor", 337, 8,
    "prefix", 345, 133
  };
  
  public static final Triple<char[], String, String> 
    TMAGIC = Triple.of(
      new char[]{ 'u', 's', 't', 'a', 'r', '\0' }, 
      "TMAGIC", 
      "ustar and a null"
    );
  public static final Triple<char[], String, String> 
    OLDGNU_MAGIC = Triple.of(
      new char[] { 'u', 's', 't', 'a', 'r', ' ', ' ', '\0' },
      "OLDGNU_MAGIC",
      "7 chars and a null"
    );
  
  public static final Triple<char[], String, String> 
    TVERSION = Triple.of(
      new char[] { '0', '0' }, 
      "TVERSION", 
      "00 and no null"
    );
    
  public static final 
  Triple<Character, String, String> UNKNOWN_TYPEINFO 
    = Triple.of('?', "UNKTYPE", "unknown type");
  
  public static final 
  Map<Character, Triple<Character, String, String>> 
  TYPEINFO_MAP =
  new HashMap<Character, Triple<Character, String, String>>() 
  {{
    put('0',  Triple.of('0', "REGTYPE", "regular file"));
    put('\0', Triple.of('\0', "AREGTYPE", "regular file"));
    put('1',  Triple.of('1', "LNKTYPE", "link"));
    put('2',  Triple.of('2', "SYMTYPE", "reserved"));
    put('3',  Triple.of('3', "CHRTYPE", "char. special"));
    put('4',  Triple.of('4', "BLKTYPE", "block special"));
    put('5',  Triple.of('5', "DIRTYPE", "directory"));
    put('6',  Triple.of('6', "FIFOTYPE", "FIFO special"));
    put('7',  Triple.of('7', "CONTTYPE", "reserved"));
    put('x',  Triple.of('x', "XHDTYPE", "ext header"));
    put('g',  Triple.of('g', "XGLTYPE", "global ext header"));
    put('?',  UNKNOWN_TYPEINFO);
  }};
  

  
  public static final 
  Map<Integer, Triple<Integer, String, String>> modeMap =
  new HashMap<Integer, Triple<Integer, String, String>>() 
  {{
    put(04000, Triple.of(04000, "TSUID", 
      "set UID on execution"));
    put(02000, Triple.of(02000, "TSGID", 
      "set GID on execution"));    
    put(01000, Triple.of(01000, "TSVTX", "reserved"));
    put(00400, Triple.of(00400, "TUREAD", "read by owner"));
    put(00200, Triple.of(00200, "TUWRITE", "write by owner"));
    put(00100, Triple.of(00100, "TUEXEC", 
      "execute/search by owner"));
    put(00040, Triple.of(00040, "TGREAD", "read by group"));
    put(00020, Triple.of(00020, "TGWRITE", "write by group"));
    put(00010, Triple.of(00010, "TGEXEC", 
      "execute/search by group"));
    put(00004, Triple.of(00004, "TOREAD", "read by other"));
    put(00002, Triple.of(00002, "TOWRITE", "write by other"));
    put(00001, Triple.of(00001, "TOEXEC", 
      "execute/search by other"));
  }};
  
  static String ENCODING = "UTF-8";
  
  public static long ch;
  
  static {
    try {
      ch = ((Long)Reflect.findMethod(
        NativeConverter.class, "openConverter"
      ).invoke(null, "ASCII")).longValue();
    } catch (Throwable e) { 
      if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace();
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
  
  TarEntry entry;
  byte[] bytes;
  char[] charArray;
  String[] strArray;
  Object val;
  long total;
  
  public int read() {
    entries = //TreeMultimap.create(); //
      new ListOrderedMap<String, TarEntry>();
    
    extendedEntry = null;
    String name;
    long offs;
    long len;
    total = 0;
    StringBuilder sb;
    int idx = -3; 
    int emptyBlocks = 0;
    //byte[] bytes;
    //Object val;
    long datread = 0; 
    long sz;
    long lasttotal = 0;
    outer:
    do {
      sb = new StringBuilder(76 * 10); 
      
      entry = new TarEntry();
      inner:
      do { 
      
        datread = 0; 
        idx = -3;        
        lasttotal = total;
        try {
          if (verbosity >= DEBUG) {
            entry.put("headerStartTotal", total);
            entry.put("headerStartPos", tar.position());
          }
          int readNum = tar.read(block, 0, 512);
          if (readNum == -1) {
            System.err.println("read -1");
            break outer;
          }
          
          total += readNum;
          
          if (verbosity >= DEBUG) {
            System.err.printf("read returned %d\n", readNum);
            
            entry.put("block", block);
            entry.put("headerEndTotal", total);
            entry.put("headerEndPos", tar.position());
            
            ByteArrayOutputStream baos 
              = new ByteArrayOutputStream();
            HexDump.dump(block, 0, baos, 0); 
            String dump = baos.toString("ASCII");
            try { baos.close(); } catch (IOException e) { }
            entry.put("dump", dump);
            
            int pos = -1;          
            char[] charArray = new char[block.length];
            String[] strArray = new String[block.length];
            String strChar;
            while (++pos < block.length) {
              charArray[pos] = (char) ((byte)block[pos]);
              strChar = Character.valueOf(charArray[pos])
                          .toString();
              if (! StringUtils.isAsciiPrintable(strChar)) {
                int charValue = (int) block[pos];
                strChar = String.format("\\x%02x", charValue);
              }
              strArray[pos] = strChar;
            }
            
            entry.put("charBlock", charArray);
            entry.put("strBlock", strArray);
          }
          
          boolean empty = true;
          byte zero = (byte) 0;
          for (byte byte_: block) {
            if (byte_ != zero) {
              empty = false;
              break;
            }
          }
          if (empty) {
            emptyBlocks += 1;
            System.err.println("Empty block");
            if (emptyBlocks == 2) {            
              break outer;
            }
            continue outer;
          }
        
        } catch (IOException rEx) {
          System.err.println(rEx.toString());
          break inner;
        }
        if (verbosity >= TRACE) System.err.printf(
          "Entry %d\n======%s\n",
          entries.size(), 
          Integer.valueOf(entries.size()).toString()
            .replaceAll(".", "=")
        );
        
        while (idx + 3 < layout.length) { 
          idx += 3; 
          name = (String) layout[idx]; 
          //System.err.println(name);
          offs = ((Integer) layout[idx+1]).intValue(); 
          len = ((Integer) layout[idx+2]).intValue(); 
          int zeropos = 0; 
          while (zeropos < len 
          && block[(int)(offs+zeropos)] != 0) { 
            zeropos += 1; 
          } 
          bytes 
            = Arrays.copyOfRange(block, 
                (int)offs, (int)offs + (int)zeropos);
          //System.err.println(Arrays.toString(bytes));
          val = stringFromBytes(bytes); 
          if (name.equals("name") && bytes.length == 0) {
            System.err.println("name.length() == 0");
            
            break outer;
          }
          if (verbosity >= TRACE) System.err.printf(
            "- reading %s: %s\n",
            name, Arrays.toString(bytes)
          );
          if (len < 32 
          && !name.equals("magic") 
          && !name.equals("version") 
          && !name.equals("typeflag")
          && ((String) val).length() > 0) {
            try {
             val = Long.valueOf(((String)val).trim(), 8);
            } catch (NumberFormatException e) {
              try {
                val = Long.valueOf((long) Double.valueOf(
                  ((String)val).trim()).doubleValue());
              } catch (Throwable e2) { 
                System.err.printf("nfe: %s: [%s, %d, %d]\n",
                  e, name, offs, len
                );
              }
            }
          }
          
          if (verbosity >= TRACE) {
             System.err.printf(
               "entry %d: key=%s: read %d bytes: %s\n"
            +  "  .. converted to %s with val = %s\n",
               entries.size(), 
               name, 
               bytes.length,
               Arrays.toString(bytes),
               val != null?
                 val.getClass().getSimpleName(): "<null>",
               val instanceof String
                 ? String.format("\"%s\"", (String)val)
                 : (val != null? val.toString(): "null")
             );
          }
          
          /*sb.append(String.format(
            "name = %8s, offs = %3d, len = %3d [%12s] %12o\n",
            name, offs, len, val, 
            val instanceof Long? val: 0
          )); */
          entry.put(name, val);
        }  
        
        /*System.out.printf("\n\n%s\n\n", sb.toString());
        System.out.printf("size = %s\n", 
        Debug.ToString( entry.get("size")));*/
        /*try { System.err.printf("pos = %s\n", 
          Debug.ToString(tar.getChannel().position()));
        } catch (Throwable e) { e.printStackTrace(); }
        tar.skip(512);
        try { System.err.printf("pos = %s\n", 
          Debug.ToString(tar.getChannel().position()));
        } catch (Throwable e) { e.printStackTrace(); }*/
      } while (entry.get("size") instanceof Long 
            && entry.get("size").equals(Long.valueOf(0))); 
      
      if (entry.get("size") instanceof String) break;
      if (entry.get("size") != null 
      && (entry.get("size") instanceof Long)) 
      {                    
        sz = ((Long)entry.get("size")).longValue(); 
        
        long fblocks = sz / 512 + 1;
        
        long rem = sz % 512;
        if (rem == 0) fblocks--;
        
        long numread // = fblocks + 1; 
                     = fblocks + (rem > 0? 1: 0); 
        boolean skipdata 
          = skipAllData || sz > maxContentSize;

        byte[] data = skipdata?
          ZERO_BYTES: new byte[(int)fblocks * 512]; 

        if (verbosity >= TRACE) {
          System.err.printf("datread = %d\n", datread);
          System.err.printf("sz = %d\n", sz);
          System.err.printf("fblocks = %d\n", fblocks);
          System.err.printf("rem = %d\n", rem);
          System.err.printf("numread = %d\n", numread);        
          System.err.printf("data.length = %d\n",
            data.length);
          System.err.printf(
            "==== LOOP: datread {%d} < sz{%d}? %s ====\n",
            datread, sz, Boolean.valueOf(datread < sz)
          );
          
        }
        if (verbosity >= TRACE) {
          System.err.printf(
              "==== LOOP: datread {%d} < sz{%d}? %s ====\n",
             datread, sz, Boolean.valueOf(datread < sz)
          );
        }
        while (datread < fblocks * 512) {
          if (verbosity >= TRACE) System.err.println(
            "loop iteration");
          long count;
          try {
            if (verbosity >= TRACE) {
              System.err.printf(
                ">>  count = tar.read(\n"
              + "      (byte[%d]) data, \n"
              + "      (int) datread=%d, \n"
              + "      (int) ((fblocks * 512){%d} - datread{%d} = %d)\n"
              + "    );\n",
               data.length, 
               datread,
               (fblocks * 512) ,
                 datread,
               (fblocks * 512) - datread
              );
            }
            
            
            count = skipdata
              ? tar.skip( 
                  (int)( (fblocks*512) - datread ) 
                )
              : tar.read(
                  data, 
                  (int)  datread, 
                  (int)( (fblocks*512) - datread ) 
                );
            
            if (verbosity >= TRACE) {
              System.err.printf(
                skipdata?
                  "  - skipped %d bytes\n"
                : "  - read %d bytes\n", 
                count);
              System.err.printf("  - count := %d\n", count);
            }
          } catch (IOException rEx) {
            if (verbosity >= TRACE) rEx.printStackTrace();
            System.err.println(rEx.toString());
            System.err.println("BREAK outer!!!");
            break outer;
          } 
          
          if (verbosity >= TRACE) {
            System.err.printf(
              "  - datread {%d} += count {%d} --> %d\n",
             datread, count, datread + count
            );
            System.err.printf(
              "  - total {%d} += count {%d} --> %d\n",
             total, count, total + count
            );
          }
          
          datread += count;
          total += count;
          

        }
        
        entry.put(
          "content", 
          skipdata? 
            data:
            Arrays.copyOfRange(data, 0, (int)sz)
        );
      }
      
      if (entry.getType() != '0' 
      &&  entry.getExtendedKind() != null) 
      {
        extendedEntry = entry;
        if (verbosity >= DEBUG) {
          System.err.printf(
            "Storing extended entry %s\n",
            entry.getExtendedKind().toString()
          );
        }
      } else {
        if (extendedEntry != null) {
          if (verbosity >= DEBUG) {
            System.err.printf(
              "Attaching extended entry %s\n",
              extendedEntry.getExtendedKind().toString()
            );
          }
          try {
            TarEntry.parseExtended(entry, extendedEntry);
            entry.extendedEntry = extendedEntry;
            extendedEntry = null;
          } catch (Throwable e) { 
            if (verbosity >= DEBUG) e.printStackTrace();
            entries.put(
              (String) extendedEntry.get("name"), 
              extendedEntry
            );
            extendedEntry = null;
          }
        }
        entries.put(
          (String) entry.get("name"), 
          entry
        );
        if (verbosity >= NORMAL) {
          System.out.println(entry);
        } else if (verbosity >= MINIMAL) {
          System.out.println((String)entry.get("name"));
        } else if (verbosity >= SILENT) {
          // no entry list
        }
      }
      if (verbosity >= DEBUG) {
        System.err.printf(
          "Finished reading entry: %s\n",
          (String)entry.get("name")
        );
      }

    } while (true); //datread >= 0);    
    return (int) total;
  }
  
  public Map<String, ? extends TarEntry> getEntries() {
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
  
  public TarEntry getEntry(String name) {
    TarEntry te = this.getEntries().get(name);
    if (te != null) return te;
     Pattern ptrn = Pattern.compile(name, 
        Pattern.CASE_INSENSITIVE 
      | Pattern.DOTALL 
      | Pattern.MULTILINE 
      | Pattern.UNIX_LINES
    );
    for (Map.Entry<String, TarEntry> e: entries.entrySet()) {
      String eName = e.getKey();
      if (ptrn.matcher(eName).matches()) {
        return e.getValue();
      }
    }
    return null;
  }
  
  public InputStream getInputStream(TarEntry te) {
    byte[] content = te.getBytes();
    return new ByteArrayInputStream(content);
  }
  
  
  
  
  
  @Override
  public void add(int location, TarEntry entry) {
    throw new UnsupportedOperationException();
    //getEntries().put(location, entry.getName(), entry);
  }
  @Override
  public boolean add(TarEntry entry) {
    boolean contains 
      = getEntries().containsKey(entry.getName());
    entries.put(entry.getName(), entry);
    return contains;
  }
  @Override
  public boolean addAll(int location, Collection<? extends TarEntry> toAdd) {
    throw new UnsupportedOperationException();
    /*Collections.addAll(
      getEntries().values(), toAdd.toArray(new TarEntry[0])
    );*/
  }
  @Override
  public void clear() {
    if (entries != null) entries.clear();
  }
  @Override
  public boolean equals(Object object) {
    if (object == null) return false;
    if (!(object instanceof TarFile)) return false;
    return System.identityHashCode(this)
        == System.identityHashCode(object);
  }
  @Override
  public TarEntry get(int location) {
    return (TarEntry) 
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
  public Iterator<TarEntry> iterator() {
    return (Iterator<TarEntry>)
      (Iterator<?>) getEntries().values().iterator();
  }
  @Override
  public int lastIndexOf(Object object) {
    throw new UnsupportedOperationException();
    //return getEntries().values().lastIndexOf(object);
  }
  @Override
  public ListIterator<TarEntry> listIterator() {
    throw new UnsupportedOperationException();
    //return getEntries().values().listIterator();
  }
  @Override
  public ListIterator<TarEntry> listIterator(int location) {
    throw new UnsupportedOperationException();
    //return getEntries().values().listIterator(location);
  }
  @Override
  public TarEntry remove(int location) {
    TarEntry entry = get(location);
    remove(location);
    return entry;
  }
  @Override
  public TarEntry set(int location, TarEntry object) {
    throw new UnsupportedOperationException();
    //return getEntries().values().set(location, object);
  }
  @Override
  public List<TarEntry> subList(int start, int end) {
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
  public boolean addAll(Collection<? extends TarEntry> toAdd)
  {
    Collections.addAll(this, toAdd.toArray(new TarEntry[0]));
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
      "<TarFile+%s: %s [%s] @%8x>",
      tar.getCompressionByReflection(tar),
      srcName,
      entries != null
        ? String.format("%d entries", entries.size())
        : "Not yet parsed",
      this.hashCode()
    );
  }
  
}