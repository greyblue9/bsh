package org.cojen.classfile;

import java.io.InputStream;
import java.io.IOException;


public interface ClassFileDataLoader {

  InputStream getClassData(String className) throws IOException;
  
}
