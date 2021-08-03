package org.d6r;

import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;
import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.DexType;
import com.googlecode.dex2jar.DexLabel;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import static org.d6r.ClassInfo.simplifyName;

/**
@author Panxiaobo <pxb1988@gmail.com>
@author greyblue9 <greyblue9@gmail.com>

@version $Rev$
*/
public class AnnotationNode implements DexAnnotationVisitor {

  public static class Item {
    
    public String name;
    public Object value;

    /**
    @param name
    @param value
    */
    public Item(String name, Object value) {
      super();
      this.name = name;
      this.value = value;
    }
  }

  public String type;
  public boolean visible;

  public final List<Item> items = new ArrayList<Item>(5);

  @SuppressWarnings("unchecked")
  public static void accept(String name, Object v, AnnotationVisitor av) {
    if (v instanceof AnnotationNode) {
      AnnotationNode a = (AnnotationNode) v;
      DexAnnotationVisitor av1 = av.visitAnnotation(name, a.type);
      accept(a.items, av1);
      av1.visitEnd();
    } else if (v instanceof Field) {
      Field e = (Field) v;
      av.visitEnum(name, e.getOwner(), e.getName());
    } else if (v instanceof List) {
      List<Object> list = (List<Object>) v;
      DexAnnotationVisitor av1 = av.visitArray(name);
      for (Object i : list) {
        accept(null, i, av1);
      }
      av1.visitEnd();
    } else if (v instanceof Method) {
      Method method = (Method) v;
      /*DexAnnotationVisitor av1 = av.visitAnnotation(
        name, "Lcom.googlecode.Method;"
      );*/
      /*av1.visit("owner", method.getOwner());
      av1.visit("name", method.getName());
      av1.visit("desc", method.getType().getDesc());*/
      // av1.visitEnd();
      av.visit(name, v);
    } else if (v instanceof DexType) {
      av.visit(name, ((DexType) v).desc);
    } else {
      av.visit(name, v);
    }
  }

  static void accept(List<Item> items, AnnotationVisitor av) {
    for (Item item : items) {
      accept(item.name, item.value, av);
    }
  }

  @SuppressWarnings("unchecked")
  public static void accept(String name, Object v, DexAnnotationVisitor av) {
    if (v instanceof AnnotationNode) {
      AnnotationNode a = (AnnotationNode) v;
      DexAnnotationVisitor av1 = av.visitAnnotation(name, a.type);
      accept(a.items, av1);
      av1.visitEnd();
    } else if (v instanceof Field) {
      Field e = (Field) v;
      av.visitEnum(name, e.getOwner(), e.getName());
    } else if (v instanceof List) {
      List<Object> list = (List<Object>) v;
      DexAnnotationVisitor av1 = av.visitArray(name);
      for (Object i : list) {
        accept(null, i, av1);
      }
      av1.visitEnd();
    } else if (v instanceof Field) {
      Field e = (Field) v;
      av.visitEnum(name, e.getOwner(), e.getName());
    } else if (v instanceof Method) {
      Method method = (Method) v;
      av.visit(name, v);
    } else {
      av.visit(name, v);
    }
  }
  
  static void accept(List<Item> items, DexAnnotationVisitor av) {
    for (Item item : items) {
      accept(item.name, item.value, av);
    }
  }

  public AnnotationNode() {
    super();
  }

  public AnnotationNode(String type, boolean visible) {
    super();
    this.type = type;
    this.visible = visible;
  }

  public void accept(DexAnnotationAble a) {
    DexAnnotationVisitor av = a.visitAnnotation(type, visible);
    accept(items, av);
    av.visitEnd();
  }

  public void accept(DexAnnotationVisitor av) {
    accept(items, av);
  }

  public void accept(FieldVisitor fv) {
    DexAnnotationVisitor av = fv.visitAnnotation(type, visible);
    accept(items, av);
    av.visitEnd();
  }

  /**  
  @see DexAnnotationVisitor.visit(String, Object)
  */
  @Override
  public void visit(String name, Object value) {
    items.add(new Item(name, value));
  }

  /**  
  @see DexAnnotationVisitor.visitAnnotation(String, String)
  */
  @Override
  public DexAnnotationVisitor visitAnnotation(String name, String desc) {
    AnnotationNode ann = new AnnotationNode(desc, true);
    items.add(new Item(name, ann));
    return ann;
  }

  public static class ArrayV implements DexAnnotationVisitor {

    List<Object> list;

    public ArrayV(List<Object> list2) {
      this.list = list2;
    }

    @Override
    public void visit(String name, Object value) {
      list.add(value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
      list.add(new Field(desc, value, null));
    }

    @Override
    public DexAnnotationVisitor visitAnnotation(String name, String desc) {
      AnnotationNode node = new AnnotationNode(desc, true);
      list.add(node);
      return node;
    }

    @Override
    public DexAnnotationVisitor visitArray(String name) {
      List<Object> list = new ArrayList<Object>(5);
      list.add(list);
      return new ArrayV(list);
    }

    @Override
    public void visitEnd() {
    }
  }

  /**  
  @see DexAnnotationVisitor.visitArray(String)
  */
  @Override
  public DexAnnotationVisitor visitArray(String name) {
    List<Object> list = new ArrayList<Object>(5);
    items.add(new Item(name, list));
    return new ArrayV(list);
  }

  /**  
  @see DexAnnotationVisitor.visitEnd()
  */
  @Override
  public void visitEnd() {
  }

  /**  
  @see DexAnnotationVisitor.visitEnum(String, String,
  String)
  */
  @Override
  public void visitEnum(String name, String desc, String value) {
    items.add(new Item(name, new Field(desc, value, null)));
  }

  public void accept(ClassVisitor cv) {
    DexAnnotationVisitor av = cv.visitAnnotation(type, visible);
    accept(items, av);
    av.visitEnd();
  }
  
  
  
  
  static final String EMPTY_STRING = "";
  
  
  public static int count(Iterable<AnnotationNode> annotations) {
    int count = 0;
    for (AnnotationNode annotation: annotations) {
      if (annotation.type.indexOf("/Signature") != -1) continue; 
      if (annotation.type.indexOf("/Throws") != -1) continue; 
      count++;
    }
    return count;
  }
  
  public static String render(AnnotationNode... annotations) {
    StringBuilder sb = new StringBuilder(annotations.length * 80);
    for (AnnotationNode annotation: annotations) {
      if (annotation.type.indexOf("/Signature") != -1) continue; 
      if (annotation.type.indexOf("/Throws") != -1) continue; 
      
      if (sb.length() > 0) sb.append("\n");
      sb.append(renderAnnotationItems(annotation, 2, -8));
    }
    return sb.toString();
  }
  
  
  
  
  public String getThrowsString(AnnotationNode throwsNode) {    
    if (throwsNode == null) return EMPTY_STRING;
    List<String> seenTypes = new ArrayList<String>(5);
    
    // AnnotationNode throwsNode = AVs.get(ANNOTATION_THROWS);
    AnnotationNode.Item[] items 
      = ((List<?>) Reflect.getfldval(throwsNode, "items"))
          .toArray(new AnnotationNode.Item[0]);
    StringBuilder throwsSb = new StringBuilder(76);
    List<?> elements = (List<?>) items[0].value;
    DexType[] types = elements.toArray(new DexType[0]);
    
    for (DexType type: types) { 
      String exClassName = simplifyName(types[0].desc);
      if (seenTypes.contains(exClassName)) continue; 
      seenTypes.add(exClassName);
      if (throwsSb.length() > 0) throwsSb.append(", "); 
      throwsSb.append(dumpMembers.colorize(exClassName, "1;31"));
    }; 
    throwsSb.insert(0, dumpMembers.colorize(" throws ", "1;30")); 
    return throwsSb.toString();
  }
  
  
  public static String renderEnumLiteral(Field fld) {
    String fieldName = fld.getName();
    String owner = fld.getOwner();
    String className = null;
    if (owner != null) {
      className = ClassInfo.typeToName(owner);
      String simple = ClassInfo.simplifyName(className);
      String simpleNested 
        = className.replaceAll("^.*\\$([A-Z][^$.]*)$", "$1");
      String enumTypeName 
        = simpleNested.length() < simple.length() ? simpleNested : simple;
      String str = String.format(
        "\u001b[1;36m%s\u001b[0;36m.\u001b[1;37m%s\u001b[0m", 
        enumTypeName, fieldName
      );
      return str;
    }
    return dumpMembers.dumpMembers(fld);
  }
  
  public static String renderClassLiteral(String className) {
    String simple = ClassInfo.simplifyName(className);
    String str = String.format(
      "\u001b[1;32m%s\u001b[0m", 
      className
    ).concat("\u001b[0;36m.class\u001b[0m");
    if (str.indexOf(className) != -1 && !className.equals(simple)) {
      str = str.substring(
        0, str.indexOf(className)
      ).concat(simple).concat(str.substring(
        str.indexOf(className) + className.length()
      ));
    }
    return str;
  }
  
  public static String renderAnnotationItems(AnnotationNode an, 
  int indentSize, int justify) 
  {
    String str = "", desc = "";
    String justSpec = (justify != -1) 
      ? Integer.valueOf(justify).toString() : "";
    String indent = String.format(
      "%" + Integer.toString(indentSize) + "s", " "
    );
    String annTypeSimple 
      = ClassInfo.simplifyName(ClassInfo.typeToName(an.type));
    AnnotationNode.Item[] ims 
      = an.items.toArray(new AnnotationNode.Item[0]);
    List sbitems = new ArrayList();
    for (AnnotationNode.Item itm : ims) {
      StringBuilder sb = new StringBuilder(50);
      List<Object> vals = itm.value instanceof List ? 
        (List) itm.value : Collections.singletonList(itm.value);
      String key = itm.name;
      for (Object val : vals) {
        if (sb.length() > 0) sb.append(", ");
        if (val instanceof com.googlecode.dex2jar.DexType) {
          desc = ((com.googlecode.dex2jar.DexType) val).desc;
          String className = ClassInfo.typeToName(desc);
          str = renderClassLiteral(className);
          sb.append(str);
        } else if (val instanceof Field) {
          str = renderEnumLiteral((Field) val);
          sb.append(str);
        } else if (val instanceof AnnotationNode) {
          str = renderAnnotationItems(
            (AnnotationNode) val, indentSize + 2, justify
          );
          sb.append(str);
        } else {
          sb.append(val instanceof CharSequence 
            ? String.format("\u001b[1;33m\"%s\"\u001b[0m", val) 
            : String.valueOf(val) 
                + " [" + val.getClass().getSimpleName() + "]");
        }
      }
      if (itm.value instanceof List && ((List) itm.value).size() > 1) {
        if (((List) itm.value).size() >= 3) {
          sb.insert(0, ("\n" + indent + indent).replaceAll(" $", ""));
          sb.append(("\n" + indent).replaceAll(" $", ""));
        }
        sb.insert(0, "\u001b[0;32m{\u001b[0m ");
        sb.append(" \u001b[0;32m}\u001b[0m");
      }
      sb.insert(
        0, String.format(
          ("value".equals(key)? "": "\u001b[0m%".concat(justSpec).concat(
            "s\u001b[0m \u001b[1;30m=\u001b[0m ")), key
        )
      );
      sbitems.add(sb);
    }
    return (indent + "\u001b[1;35m@".concat(annTypeSimple)
      .concat(sbitems.size() > 1? "\u001b[0m(\n".concat(indent)
        : (sbitems.size() == 1? "\u001b[0m(": "\u001b[0m"))
      .concat(StringUtils.join(
        sbitems, ",\n".concat(indent)
      )).concat(sbitems.size() > 1? "\n)"
        : (sbitems.size() == 1? ")": ""))
    );
  }
  
}

