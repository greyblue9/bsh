package org.d6r;
import org.apache.commons.io.input.AutoCloseInputStream;
import java.nio.charset.StandardCharsets;
import org.jsoup.nodes.*;
import java.util.*;
import java.io.*;
import org.apache.commons.lang3.StringEscapeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;
import java8.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.d6r.ClassInfo;
import org.d6r.DexParser.DebugInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.android.dex.Dex;
import com.android.dex.*;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.ArrayUtils;


public class JavaDoc
  implements ParameterNameSupplier
{
  public static final JavaDoc INSTANCE = new JavaDoc();
  
  protected static int MID_SIZE = 8;
  protected static int PROTOID_SIZE = 8;
  protected static Matcher SPACE_MCHR
    = Pattern.compile("\\s\\s*", Pattern.DOTALL).matcher("");
  
  protected final Map<String, ZipFile> zimap = new HashMap<>();
  protected final Map<String, Document> docMap = new SoftHashMap<>();
  
  @Override
  public Optional<List<String>> getParameterNames(final Member mtd) {
    final String className = mtd.getDeclaringClass().getName();
    final Document doc = lookupJavaDoc(className);
    if (doc == null) return Optional.empty();
    final String synopsis = getDocText(doc, mtd);
    if (synopsis == null || synopsis.indexOf('(') == -1) return Optional.empty();
    final List<String> names = new ArrayList<>();
    final String[] parts =
      StringUtils.substringBetween(synopsis, "(", ")").split(",");
    for (final String part: parts) {
      names.add(StringUtils.substringAfterLast(part.trim(), " "));
    }
    return Optional.of(names);
  }
  
  @Nullable
  public Document lookupJavaDoc(final String className) {
    final File jdocZip = new File(
      new File("/external_sd/_projects/sdk/javadocs"),
      (className.indexOf("android") != -1) ? "android.zip" : "jdk.zip"
    );
    if (!jdocZip.exists()) {
      Log.w("JavaDoc", String.format("File is missing: '%s'", jdocZip.getPath()));
      return null;
    }
    final ZipFile docZip;
    try {
      if (zimap.containsKey(jdocZip.getName())) {
        docZip = zimap.get(jdocZip.getName());
      } else {
        zimap.put(jdocZip.getName(), (docZip = new ZipFile(jdocZip)));
      }
      final Document doc;
      if (docMap.containsKey(className)) {
        doc = docMap.get(className);
      } else {
        final String entryName = ClassInfo.classNameToPath(className, "html");
        final ZipEntry zipEntry = docZip.getEntry(entryName);
        if (zipEntry == null) return null;
        final byte[] docBytes = IOUtils.toByteArray(
          new AutoCloseInputStream(docZip.getInputStream(zipEntry)));
        final String html = new String(docBytes, StandardCharsets.UTF_8)
          .replace("&nbsp;", " ");
        docMap.put(className, (doc = Jsoup.parseBodyFragment(html)));
      }
      return doc;
    } catch (final IOException ioe) {
      throw new RuntimeException(String.format("Error reading '%s'", jdocZip), ioe);
    }
  }
  
  public static class NotMatchedException extends RuntimeException {
    Member mtd;
    String sel;
    List<String> typeNames;
    Document doc;
    Elements anchors;
    public NotMatchedException(Member mtd, String sel, List<String> typeNames, 
      Document doc, Elements anchors)
    {
      super(String.format(
        "No matching anchor found for selector \"%s\"",
        StringEscapeUtils.escapeJava(sel)
      ));
      this.mtd = mtd;
      this.sel = sel;
      this.typeNames = typeNames;
      this.doc = doc;
      this.anchors = anchors;
    }
  }
  
  @Nullable
  public static String getDocText(final Document doc, final Member mtd) {
    if (mtd instanceof Field) throw new IllegalArgumentException("Field");
    final List<String> typeNames = new ArrayList<>();
    final Class<?>[] ptypes = (mtd instanceof Constructor)
      ? ((Constructor<?>) mtd).getParameterTypes()
      : ((Method) mtd).getParameterTypes();
    for (final Class<?> c: ptypes) {
      final String name = ClassInfo.typeToName(c.getName());
      typeNames.add(name);
    }
    final String methodName = (mtd instanceof Constructor)
      ? ClassInfo.getSimpleName(mtd.getDeclaringClass().getName())
      : ((Method) mtd).getName();
    final String anchor = String.format(
      "%s(%s)", methodName, StringUtils.join(typeNames, ", "));
    final String anchor2 = String.format(
      "%s-%s-", methodName, StringUtils.join(typeNames, "-").replace("[]", ":A"));
    final String sel
      = String.format("a[name=\"%s\"], a[name=\"%s\"]", anchor, anchor2);
    final Elements matched = doc.select(sel);
    Node node;
    if (matched.isEmpty()) {
      final Elements anchors = doc.select(
        String.format("a[name^=\"%1$s(\"], a[name^=\"%1$s-\"]", methodName));
      final Map<Integer, Element> best = new TreeMap<>();
      for (final Element a: anchors) {
        final String name = a.attr("name");
        final int dist = Math.min(
          StringUtils.getLevenshteinDistance(name, anchor),
          StringUtils.getLevenshteinDistance(name, anchor2)
        );
        best.put(Integer.valueOf(dist), a);
      }
      if (best.isEmpty()) return null;
      final Element bestMatch = best.entrySet().iterator().next().getValue();
      node = bestMatch;
    } else {
      node = matched.iterator().next();
    }
    while (node != null &&
         !((node instanceof Element) || !"pre".equals(((Element) node).tagName())))
    {
      node = node.nextSibling();
    }
    if (node == null) return null;
    final String textRaw = ((Element) node).text();
    return SPACE_MCHR.reset(textRaw.trim()).replaceAll(" ");
  }
  
  
  
  /*
  Given a method, determine the method's index.
  
  We could simply store this in the Method*, but that would cost 4 bytes
  per method.  Instead we plow through the DEX data.
  
  We have two choices: look through the class method data, or look through
  the global method_ids table.  The former is awkward because the method
  could have been defined in a superclass or interface.  The latter works
  out reasonably well because it's in sorted order, though we're still left
  doing a fair number of string comparisons.
  */
  public static int getDexMethodIndex(final Member method) {
    final Dex dex = ClassInfo.getDex(method.getDeclaringClass());
    if (dex == null) return 0;
    int hi = dex.getTableOfContents().methodIds.size -1;
    int lo = 0;
    int cur = 0;
    final TableOfContents.Section tsec = dex.getTableOfContents().methodIds;
    final TableOfContents.Section ptsec = dex.getTableOfContents().protoIds;
    final Dex.Section s = dex.open(tsec.off);
    final Dex.Section ps = dex.open(ptsec.off);
    final int initial = Reflect.<Integer>getfldval(s, "initialPosition");
    final int pinitial = Reflect.<Integer>getfldval(ps, "initialPosition");
    final ByteBuffer buf = Reflect.getfldval(s, "data");
    final ByteBuffer pbuf = Reflect.getfldval(ps, "data");
    while (hi >= lo) {
      cur = (lo + hi) / 2;
      final int off = initial + (cur * MID_SIZE);
      buf.position(off);
      final MethodId methodId = s.readMethodId();
      final int cmp = compareMethodStr(dex, methodId, method, ps, pbuf, pinitial);
      if (cmp < 0) lo = cur + 1;
      else if (cmp > 0) hi = cur - 1;
      else break;
    }
    return cur;
  }
  
  
  /*
  Compare the attributes (class name, method name, method signature) of
  the specified method to "method".
  */
  public static int compareMethodStr(Dex dex, MethodId methodId,
    final Member method, Dex.Section ps, ByteBuffer pbuf, int pinitial)
  {
    final DexCache c = getDexCache(dex);
    
    final String typeName = c.s[
        c.t[methodId.getDeclaringClassIndex()]
      ];
    final String classDesc = String.format(
      "L%s;",
      ClassInfo.classNameToPath(method.getDeclaringClass().getName(), null)
    );
    
    int result = typeName.compareTo(classDesc);
    
    if (result == 0) {
      final String methodName = (method instanceof Constructor<?>)
        ? "<init>"
        : ((Method) method).getName();
      final String name = c.s[methodId.getNameIndex()];
      result = name.compareTo(methodName);
      if (result == 0) {
        int protoIndex = methodId.getProtoIndex();
        ProtoId p = c.p[protoIndex];
        String shorty;
        if (c.sh.containsKey(System.identityHashCode(method))) {
          shorty = c.sh.get(System.identityHashCode(method));
        } else {
          final Class<?>[] parameterTypes =
            (method instanceof Constructor<?>)
              ? ((Constructor<?>) method).getParameterTypes()
              : ((Method) method).getParameterTypes();
          
          final StringBuilder shb 
          = new StringBuilder(parameterTypes.length+1);
          for (int i=-1,len=parameterTypes.length; i<len; ++i) {
            final Class<?> cls = (i == -1)
              ? ((method instanceof Method)
                  ? ((Method) method).getReturnType()
                  : Void.TYPE)
              : parameterTypes[i];
            
            char shortyChr = (cls.isPrimitive())
              ? ClassInfo.primitiveShorty(cls.getCanonicalName(), false)
              : 'L';
            shb.append(shortyChr);
          }
          c.sh.put(System.identityHashCode(method), (shorty=shb.toString()));
        }
        String pshorty = c.s[p.getShortyIndex()];
        result = pshorty.compareTo(shorty);
        if (result == 0) {
          String protoDesc = dexProtoGetMethodDescriptor(dex, p, c);
          String methodDesc = ProcyonUtil.getErasedSignature(method);
          
          return protoDesc.compareTo(methodDesc);
        }
      }
    }
    return result;
  }
  
  
  
  public static final Map<Integer, DexCache> caches = 
    new SoftHashMap<>();
  
  public static class DexCache {
    public final String[] s;
    public final int[] t;
    public final ProtoId[] p;
    public final Map<Integer, String> sh = new HashMap<>();
    
    public DexCache(Dex dex) {
      this.s = dex.strings().toArray(new String[0]);
      this.p = dex.protoIds().toArray(new ProtoId[0]);
      this.t = ArrayUtils.toPrimitive(dex.typeIds().toArray(new Integer[0]));
    }
  }
  
  public static DexCache getDexCache(final Dex dex) {
    if (caches.containsKey(System.identityHashCode(dex))) {
      return caches.get(System.identityHashCode(dex));
    }
    
    final DexCache cache = new DexCache(dex);
    caches.put(System.identityHashCode(dex), cache);
    return cache;
  }
  
  public static String dexProtoGetMethodDescriptor(Dex dex, ProtoId protoId,
    DexCache c)  
  {
    int tloff = protoId.getParametersOffset();
    TypeList typeList 
      = tloff != 0
          ? dex.open(tloff).readTypeList()
          : null;
    short[] types = typeList != null? typeList.getTypes(): new short[0];
    int length = 3; // parens and terminating '\0'
    int paramCount = (typeList == null) ? 0 : types.length;
    int i;
    String[] typeNames = new String[types.length];
    for (i = 0; i < paramCount; i++) {
      int idx = types[i];
      String typeName = c.s[c.t[idx]];
      length += typeName.length();
      typeNames[i] = typeName;
    }
    String returnType = c.s[c.t[protoId.getReturnTypeIndex()]];
    length += returnType.length();
    final StringBuilder descb = new StringBuilder(length).append('(');
    for (final String typeName: typeNames) {
      descb.append(typeName);
    }
    return descb.append(')').append(returnType).toString();
  }
  
}


interface ParameterNameSupplier {
  Optional<List<String>> getParameterNames(final Member method);
}


class DexParameterNameSupplier implements ParameterNameSupplier {
  public static final DexParameterNameSupplier INSTANCE =
    new DexParameterNameSupplier();
  
  @Override
  public Optional<List<String>> getParameterNames(final Member method) {
    final DebugInfo info = DexParser.parseDebugInfo(method);
    if (info == null) return Optional.empty();
    return Optional.of(info.parameterNames);
  }
}

class CompositeParameterNameSupplier implements ParameterNameSupplier {
  public static final CompositeParameterNameSupplier INSTANCE =
    new CompositeParameterNameSupplier(
      JavaDoc.INSTANCE,
      DexParameterNameSupplier.INSTANCE
    );
  
  protected final ParameterNameSupplier[] suppliers;
  
  public CompositeParameterNameSupplier(final ParameterNameSupplier... suppliers) {
    this.suppliers = suppliers;
  }
  
  @Override
  public Optional<List<String>> getParameterNames(final Member method) {
    for (final ParameterNameSupplier supplier: suppliers) {
      final Optional<List<String>> result = supplier.getParameterNames(method);
      if (result.isPresent()) return result;
    }
    return Optional.empty();
  }
}

