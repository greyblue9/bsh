
package org.d6r;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.lang.reflect.Modifier;
 
public class LabelInfo {
    int line;
    int offset;
    public LabelInfo(int line, int offset) {
      this.line = line;
      this.offset = offset;
    }
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }
    public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
    }
}
  