package org.d6r;

import org.objectweb.asm3.ClassWriter;
import org.objectweb.asm3.ClassVisitor;
import java.util.IdentityHashMap;
import java.util.Map;
import java8.util.function.BiConsumer;
import com.googlecode.dex2jar.v3.ClassVisitorFactory;

public class ClassVisitorFactoryImpl implements ClassVisitorFactory {

  protected final Dex2Java d2j;

  protected BiConsumer<String, byte[]> onDone;

  protected Map<String, ClsWriter> writers;

  public ClassVisitorFactoryImpl(final Dex2Java d2j, final BiConsumer<String, byte[]> onDone) {
    this.writers = new IdentityHashMap<String, ClsWriter>();
    this.d2j = d2j;
    this.onDone = onDone;
  }

  @Override
  public ClassVisitor create(final String name) {
    final ClsWriter cw = new ClsWriter(name, this.onDone);
    this.writers.put(name, cw);
    System.err.printf("Created %s(\"%s\", %s)\n", cw.getClass().getSimpleName(), name, this.onDone);
    return cw;
  }
  
  
  
  public static class ClsWriter extends ClassWriter {

    protected String name;

    protected byte[] data;

    protected BiConsumer<String, byte[]> onDone;

    public ClsWriter(final String name, final BiConsumer<String, byte[]> onDone) {
      super(1);
      this.name = name;
      this.onDone = onDone;
    }

    public ClsWriter(final String name) {
      this(name, null);
    }

    public boolean isDone() {
      return this.data != null;
    }

    public void setOnDone(final BiConsumer<String, byte[]> onDone) {
      this.onDone = onDone;
      if (this.isDone() && onDone != null) {
        onDone.accept(this.name, this.data);
      }
    }

    @Override
    public void visitEnd() {
      super.visitEnd();
      try {
        this.data = this.toByteArray();
        if (this.isDone() && this.onDone != null) {
          this.onDone.accept(this.name, this.data);
        }
      } catch (Throwable e) {
        e.printStackTrace(System.err);
      }
    }
  }
}

