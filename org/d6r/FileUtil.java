package org.d6r;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import static org.d6r.TextUtil.str;


public class FileUtil {
  
  public static String readAllText(final File file) {
    try (final FileInputStream fis = new FileInputStream(file);
         final BufferedInputStream bis = new BufferedInputStream(fis))
    {
      return TextUtil.readAllText(bis, (int) file.length());
    } catch (final FileNotFoundException fnfe) {
      throw new IllegalArgumentException(String.format(
        "readAllText(file: %s): Argument 'file' is invalid: %s",
        str(file != null? file.getPath(): null), fnfe
      ), fnfe);
    } catch (final IOException ioe) {
      throw Reflector.Util.sneakyThrow(ioe);
    }
  }
    
}


