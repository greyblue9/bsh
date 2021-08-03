package org.d6r;

import java.io.*;
import java.util.Map;

/**
 * The TarInputStream reads a UNIX tar archive as an InputStream.
 * methods are provided to position at each successive entry in
 * the archive, and the read each entry as a normal input stream
 * using read().
 *
 * @version $Revision: 1.1 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see TarBuffer
 * @see TarHeader
 * @see TarEntry
 */
public interface ITarInputStream {

   int available();
   void close();
    
   int getRecordSize();
   boolean isGzipped();
   void mark(int markLimit);
   boolean markSupported();
   void read() throws IOException;
   
   int read(byte[] buf) throws IOException;
   int read(byte[] buf, int offset, int numToRead)
      throws IOException;
      
   void logWarning(Throwable ex, String fmt, Object... args);
    
   void reset();
   void skip(int numToSkip) throws IOException;
}


