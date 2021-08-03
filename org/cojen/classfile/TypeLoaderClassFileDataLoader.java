package org.cojen.classfile;


import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.Buffer;
import org.d6r.ProcyonUtil;
import org.d6r.Reflector;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;


public class TypeLoaderClassFileDataLoader 
  implements ClassFileDataLoader 
{
  protected static TypeLoaderClassFileDataLoader _default;
  protected ITypeLoader typeLoader;
  protected boolean doThrow = true;
  
  public TypeLoaderClassFileDataLoader(ITypeLoader typeLoader) {
    this.typeLoader = typeLoader;
  }
  
  public TypeLoaderClassFileDataLoader() {
    this(ProcyonUtil.getTypeLoader());
  }
  
  public static TypeLoaderClassFileDataLoader getDefault() {
    if (_default == null) {
      _default = new TypeLoaderClassFileDataLoader();
    }
    return _default;
  }
  
  @Override
  public InputStream getClassData(String className) {
    // StringBuilder sb = new StringBuilder(className.replace('.', '/'));
    /*if (sb.indexOf(";") == -1) {
      sb.insert(0, "L");
      sb.append(";");
    }*/
    // String internalName = sb.toString();
    System.err.printf("tryLoadType: '%s'\n", className);
    Buffer buf = new Buffer();
    if (typeLoader.tryLoadType(className.replace('.', '/'), buf)) {
      return new ByteArrayInputStream(buf.array());
    }
    
    if (doThrow) throw Reflector.Util.sneakyThrow(
      new IOException(String.format(
        "Failed to load type: '%s'", className
      ))
    );
    else return null;
  }
  
}
