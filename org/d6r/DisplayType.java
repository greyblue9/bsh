package org.d6r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.d6r.annotation.NonDumpable;
import org.d6r.annotation.*;
import org.d6r.ClassInfo;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import org.w3c.dom.NodeList;
import org.apache.commons.lang3.StringUtils;

public enum DisplayType {
  STRING() {
    @Override
    public String render(Object obj, String metadata) {
      return metadata;
    }
  },
  SUMMARY() {
    @Override
    public String render(Object obj, String metadata) {
      return new Summary().apply((NonDumpable) null, obj).toString();
    }
  },
  CLASS_NAME() {
    @Override
    public String render(Object obj, String metadata) {
      if (obj == null) return "null";
      Class<?> type = obj.getClass();
      StringBuilder sb = new StringBuilder(
        ClassInfo.typeToName(type.getName())
      );
      Type[] tparams = type.getTypeParameters();
      if (tparams.length == 0) return sb.toString();
      sb.append('<');
      sb.append(StringUtils.join(tparams, ", "));
      sb.append('>');
      return sb.toString();
    }
  },
  SIZE() {
    @Override
    public String render(Object obj, String metadata) {
      if (obj == null) return "null";
      Class<?> type = obj.getClass();
      int size = -1;
      if (type.isArray()) {
        size = Array.getLength(obj);
      } else if (CharSequence.class.isAssignableFrom(type)) {
        size = ((CharSequence) obj).length();
      } else if (Collection.class.isAssignableFrom(type)) {
        size = ((Collection<?>) obj).size();
      } else if (Map.class.isAssignableFrom(type)) {
        size = ((Map<?, ?>) obj).size();
      } else if (NodeList.class.isAssignableFrom(type)) {
        size = ((NodeList) obj).getLength();
      } 
      if (size > -1) return String.format(
        "%s (size = %d)",
        CLASS_NAME.render(obj, metadata), size
      );
      return String.format(
        "%s (unknown size)",
        CLASS_NAME.render(obj, metadata)
      );
    }
  };
  
  public String render(Object obj, String metadata) {
    return Debug.ToString(this);
  }
}
