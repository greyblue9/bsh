package org.d6r;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.IOUtils;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import static org.d6r.TextUtil.toString;
import static org.d6r.TextUtil.toByteArray;
import static org.d6r.TextUtil.toCharArray;


/**
Dumps data in hexadecimal format.

Provides a single function to take an array of bytes and display 
it in hexadecimal form.

Origin of code: POI.
@version $Id: HexDump.java 1471767 2013-04-24 23:24:19Z sebb $
*/
public class HexDumps {

  /**
  Instances should NOT be constructed in standard programming.
  */
  public HexDumps() {
    super();
  }

  /**
  Dump an array of bytes to an OutputStream. The output is 
  formatted for human inspection, with a hexadecimal offset 
  followed by the hexadecimal values of the next 16 bytes of data 
  and the printable ASCII characters (if any) that those bytes 
  represent printed per each line of output.
  
  The offset argument specifies the start offset of the data array
  within a larger entity like a file or an incoming stream. For 
  example, if the data array contains the third kibibyte of a 
  file, then the offset argument should be set to 2048. The offset
  value printed at the beginning of each line indicates where in 
  that larger entity the first byte on that line is located.
  
  All bytes between the given index (inclusive) and the end of the
  data array are dumped. (*)
  
  @param data - the byte array to be dumped
  @param offset - offset of the byte array within a larger entity
  @param stream - the OutputStream to which the data is to be  
    written
  @param index - initial index into the byte array
  
  @throws IOException is thrown if anything goes wrong writing
  the data to stream
  @throws ArrayIndexOutOfBoundsException if the index is
  outside the data array's bounds
  
  @throws IllegalArgumentException if the output stream is null
  */
  public static String dump(byte[] data, int index, long length,
  int offset)
  {
    if (index < 0 || index >= data.length) {
      throw new ArrayIndexOutOfBoundsException(
        "illegal index: " + index + " into array of length " 
        + data.length
      );
    }
    int est = 67;
    ByteArrayOutputStream stream = null;
    final StringBuilder sb = new StringBuilder(est);
    try {
      stream = new ByteArrayOutputStream();
      long display_offset = offset + index;
      int stopIndex 
        = Math.min(index + ((int)length), data.length);
      
      for (int j = index; j < stopIndex; j += 16) {
        
        int chars_read = stopIndex - j;
        if (chars_read > 16) {
          chars_read = 16;
        }
        dump(sb, display_offset).append(' ');
        for (int k = 0; k < 16; k++) {
          if (k < chars_read) {
            dump(sb, data[k + j]);
          } else {
            sb.append(" ");
          }
          sb.append(' ');
        }
        for (int k = 0; k < chars_read; k++) {
          if (data[k + j] >= ' ' && data[k + j] < 127) {
            sb.append((char) data[k + j]);
          } else {
            sb.append('.');
          }
        }
        sb.append(EOL);
        // make explicit the dependency on the default encoding
        byte[] bytes = toByteArray(toCharArray(sb));
        stream.write(bytes);
        stream.flush();
        
        sb.setLength(0);
        display_offset += chars_read;
      }
    } catch (Throwable e) { 
      Reflector.Util.sneakyThrow(e);
    } finally {
      if (stream != null) {
        IOUtils.closeQuietly(stream);
      }
    }
    byte[] bytes = stream.toByteArray();
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
  The line-separator (initializes to "line.separator" 
  system property.
  */
  public static final String EOL 
    = System.getProperty("line.separator");

  private static final char[] _hexcodes = { 
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
    'A', 'B', 'C', 'D', 'E', 'F'
  };

  private static final int[] _shifts = { 
    28, 24, 20, 16, 12, 8, 4, 0
  };

  /**
  Dump a long value into a StringBuilder.
  @param _lsb the StringBuilder to dump the value in
  @param value he long value to be dumped
  @return StringBuilder containing the dumped value.
  */
  private static StringBuilder dump(final StringBuilder _lsb, 
  final long value) 
  {
    for (int j = 0; j < 8; j++) {
      _lsb.append(_hexcodes[(int) (value >> _shifts[j]) & 15]);
    }
    return _lsb;
  }

  /**
  Dump a byte value into a StringBuilder.
  @param _csb the StringBuilder to dump the value in
  @param value he byte value to be dumped
  @return StringBuilder containing the dumped value.
  */
  private static StringBuilder dump(final StringBuilder _csb, 
  final byte value) {
    for (int j = 0; j < 2; j++) {
      _csb.append(_hexcodes[value >> _shifts[j + 6] & 15]);
    }
    return _csb;
  }
}


