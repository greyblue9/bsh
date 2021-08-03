package org.d6r;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;


public class MarkableFileInputStream 
  extends FilterInputStream 
{

  private FileChannel myFileChannel;
  private long mark = -1;
  public MarkableFileInputStream(FileInputStream fis) {
    super(fis);
    myFileChannel = fis.getChannel();
  }

  public static MarkableFileInputStream from(File file) {
    try {
      return new MarkableFileInputStream(
        new FileInputStream(file)
      );
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format(
        "MarkableFileInputStream.from(File): File not found: %s", file.toString()), e);
    }
  }
  
  public static MarkableFileInputStream from(String path) {
    try {
      return new MarkableFileInputStream(
        new FileInputStream(new File(path))
      );
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format(
        "MarkableFileInputStream.from(String): File not found: %s", path), e);
    }
  }
  
  @Override
  public boolean markSupported() {
    return true;
  }
  
  @Override
  public synchronized void mark(int readlimit) {
    try {
      mark = myFileChannel.position();
    } catch (IOException ex) {
      mark = -1;
    }
  }
  
  public synchronized long getMark() {
    return mark;
  }
  
  public synchronized boolean isMarked() {
    return mark >= 0;
  }
  
  @Override
  public synchronized void reset() throws IOException {
    if (mark == -1) {
      throw new IOException("not marked");
    }
    myFileChannel.position(mark);
  }
  
  public FileChannel getChannel() {
    return myFileChannel;
  }
  
}