package org.d6r;
import org.d6r.AnnotationNode;
import org.d6r.AnnotationNode.Item;
import com.googlecode.dex2jar.DexType;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;
import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;
import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexLabel;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.BadBytecode;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Modifier;
import org.d6r.Insn.LHSKind;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;

public class FieldVisitor implements DexFieldVisitor {

  public Map<String, AnnotationNode> AVs 
    = new HashMap<String, AnnotationNode>();

  String declClsName;
  Object element;
  String annClsName;
  DexVisitor dv;
  FieldInfo fi;
  String signature;

  SignatureAttribute.ObjectType sig = null;

  public FieldVisitor(String declClsName, Object element, String annClsName, DexVisitor dv, FieldInfo fi) {
    this.declClsName = declClsName;
    this.annClsName = annClsName;
    this.dv = dv;
    this.fi = fi;
    this.fi.fv = this;
  }

  @Override
  public DexAnnotationVisitor visitAnnotation(String annoClsName, boolean vs) {
    if (!dv.visitAnnotations) return null;
    AnnotationNode an = new AnnotationNode(annoClsName, vs);
    AVs.put(annoClsName, an);
    if (dv.verbose) System.out.printf("  - annotation: @%s (%s)\n", annoClsName, Boolean.valueOf(vs).toString());
    if (dv.verbose) return LoggingProxyFactory.newProxy(an, DexAnnotationVisitor.class);
    return an;//new Annotation(clsName, null, s1);

  }

  @Override
  public void visitEnd() {
    //
    for (Map.Entry<String, AnnotationNode> ent : AVs.entrySet()) {
      String annType = ent.getKey();
      if (!"Ldalvik/annotation/Signature;".equals(annType)) continue;
      AnnotationNode an = ent.getValue();
      try {
        signature = StringUtils.join(((Collection<String>) an.items.get(0).value).toArray(), "");
        sig = SignatureAttribute.toFieldSignature(signature);
        fi.typeName = sig.toString();
        fi.signature = signature;
      } catch (BadBytecode bb) {
        if ("true".equals(System.getProperty("printStackTrace"))) bb.printStackTrace();
      } catch (Throwable ex) {
        if ("true".equals(System.getProperty("printStackTrace"))) ex.printStackTrace();
      }
    }
  }

  @Override
  public String toString() {
    return Debug.ToString(this);
  }
}

