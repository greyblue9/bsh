package org.d6r;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;



public class ZipRecover {
  
  public static final byte[] MAGIC_PK0304 = new byte[] { 0x50, 0x4b, 0x03, 0x04 };
  
  
  public static void main(String... brokenZipPaths) throws Throwable {
    for (final String brokenZipPath: brokenZipPaths) {
      final File file = new File(brokenZipPath)
        .getCanonicalFile().getAbsoluteFile();
      
      final byte[] zipBytes = FileUtils.readFileToByteArray(file);
      final File tempDir = new File(
        System.getProperty(
          "outdir",
          String.format(
            "/tm/tmp/archive_fragments_%s_%s",
            FilenameUtils.removeExtension(file.getName()),
            Long.toString(System.currentTimeMillis(), 32)
          )
        )
      );
      
      if (!tempDir.mkdirs()) {
        try {
          throw Reflector.Util.sneakyThrow(
            Reflect.invoke(tempDir, "mkdirErrno", tempDir)
          );
        } catch (final Throwable t) {
          if (Reflector.getRootCause(t) instanceof IOException) {
            throw (IOException) Reflector.getRootCause(t);
          }
          final Throwable e = (t instanceof InvocationTargetException)
            ? ((InvocationTargetException) t).getTargetException()
            : t;
          throw new IOException(String.format(
            "Trouble invoking File@%08x('%s').mkdirErrno(): %s",
            System.identityHashCode(file), file.getPath(), e
          ), e);
        }
      }
      
      final Deque<Integer> ad = new ArrayDeque<>();
      
      int fragIndex = -1, last = -1, nextLast = -1, pos = -1;
      while ((pos = ByteUtil.indexOf(zipBytes, MAGIC_PK0304, ++last)) != -1) {
        ad.offer(pos);
        System.err.printf("Found zip signature at offset %d\n", pos);
        nextLast = last;
        last = pos;
      }
      
      final List<Pair<Integer, Integer>> pairs = new ArrayList<>();
      while (ad.size() > 1) {
        int start = ad.poll().intValue();
        int end = ad.peek().intValue();
        final Pair<Integer, Integer> pair = Pair.of(start, end);
        pairs.add(pair);
        System.err.println(pair);
      }
      
      for (final Pair<Integer, Integer> pair: pairs) {
        final byte[] data = Arrays.copyOfRange(
          zipBytes, pair.getKey().intValue(), pair.getValue().intValue()
        );
        final File outFile = new File(
          tempDir, String.format("fragment_%04d_%08d.zip", ++fragIndex, pos)
        );
        try {
          FileUtils.writeByteArrayToFile(outFile, data);
        } catch (final IOException ioe) {
          final String output = String.format("%s",
            PosixFileInputStream.pexecSync("mount", "-o", "remount,rw", "/")
          ).trim();
          if (output.length() != 0) {
            throw new IOException(String.format(
              "remount '/' failed; output was: '%s' (operation: writing '%s')",
              output, outFile.getPath()
            ), ioe);
          } else {
            FileUtils.writeByteArrayToFile(outFile, data);
          }
        } // catch
        System.err.printf(
          "Wrote '%s': %d bytes\n", outFile.getName(), outFile.length()
        );
      } // for pair: pairs
      
      System.out.printf(
        "Wrote %d fragments to directory: %s\n\n", pairs.size(), tempDir.getPath()
      );
    }
  }
}


