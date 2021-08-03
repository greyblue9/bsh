package org.d6r;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.NotSerializableException;

import java.math.BigInteger;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.d6r.xstream.FieldDict;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.mapper.ImplicitCollectionMapper;
import com.thoughtworks.xstream.mapper.OuterClassMapper;
import com.thoughtworks.xstream.mapper.DefaultImplementationsMapper;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;



public class SimpleCodec {
  
  protected static Object xs;
  

  public static byte[] decode(final String encodedString) {
    final byte[] compressed = new BigInteger(encodedString, 32).toByteArray();
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
         final GZIPInputStream gzis = new GZIPInputStream(bais))
    {
      return IOUtils.toByteArray(gzis);
    } catch (final IOException ioe) {
      throw new AssertionError(ioe);
    }
  }
  
  public static String encode(final byte[] data) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
         final OutputStream gzos = new GZIPOutputStream(baos);
         final InputStream bais = new ByteArrayInputStream(data))
    {
      IOUtils.copy(bais, gzos);
      gzos.flush();
      gzos.close();
      baos.close();
      return new BigInteger(baos.toByteArray()).toString(32);
    } catch (final IOException ioe) {
      throw new AssertionError(ioe);
    }
  }
  
  
  
  
  public static XStream getXStream() {
    if (xs != null) return (XStream) xs;
    final ClassLoaderReference clref = new ClassLoaderReference(
      Thread.currentThread().getContextClassLoader()
    );
    final Mapper mapper = new ImplicitCollectionMapper(
      new OuterClassMapper(new DefaultImplementationsMapper(
        new DefaultMapper(clref)
      ))
    );
    final FieldDict dict = new FieldDict();
    final SunUnsafeReflectionProvider rp = new SunUnsafeReflectionProvider(dict);
    final XmlFriendlyReplacer replacer = new XmlFriendlyReplacer();
    final DomDriver driver = new DomDriver("UTF-8", replacer);
    return (XStream) (xs = new XStream(rp, driver, clref, mapper));
  }
  
  public static String encodeObject2(final Object obj) {
    if (obj instanceof Serializable) {
      final Serializable sobj = (Serializable) obj;
      try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
           final OutputStream gzos = new GZIPOutputStream(baos);
           final ObjectOutputStream oos = new ObjectOutputStream(gzos))
      {
        oos.writeObject(sobj);
        oos.flush();
        oos.close();
        gzos.flush();
        gzos.close();
        baos.close();
        return new BigInteger(baos.toByteArray()).toString(32);
      } catch (final NotSerializableException nse) {
      } catch (final IOException ioe) {
        throw Reflector.Util.sneakyThrow(ioe);
      }
    }
    
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
         final OutputStream gzos = new GZIPOutputStream(baos))
    {
      getXStream().toXML(obj, gzos);
      gzos.flush();
      gzos.close();
      baos.close();
      return new BigInteger(baos.toByteArray()).toString(32);
    } catch (final IOException ioe) {
      throw new AssertionError(ioe);
    }
  }
  
  
  
  public static String encodeObject(final Object object) {
    if (object == null) throw new IllegalArgumentException("object == null");
    
    return SimpleCodec.encode(
      SimpleCodec.getXStream()
                 .toXML(object)
                 .getBytes(StandardCharsets.UTF_8)
    );
  }
  
  
  static final Class<?>[] MOD_COUNTERS 
    = { Iterable.class, Map.class, Iterator.class, Enumeration.class };
  
  
  public static <T> T decodeObject(final CharSequence encoded) {
    if (encoded == null) throw new IllegalArgumentException("encoded == null");
    final String encodedString = (encoded instanceof String)
       ? (String) encoded
       : encoded.toString();
    
    final T thawed = (T) SimpleCodec.getXStream().fromXML(
      new String(SimpleCodec.decode(encodedString), StandardCharsets.UTF_8)
    );
    
    // Everything will throw ConcurrentModificationExceptions without this step
    return fixErroneousModCountsInObjectGraph(thawed);
  }
  
  
  public static <T> T fixErroneousModCountsInObjectGraph(final T thawed) {
    // Everything will throw ConcurrentModificationExceptions without this step
    //
    for (final Class<?> modTrackingType: MOD_COUNTERS) {
      boolean errorPrinted = false;
      for (final Object inst:
           ObjectUtil.searchObject(thawed, modTrackingType, false, 0, 50)) {
        try {
          Reflect.setfldval(inst, "modCount", 0);
        } catch (final Throwable t) {
          if (!errorPrinted) {
            errorPrinted = true;
            new RuntimeException(String.format(
              "Setting modCount on %s caused %s", ClassInfo.getSimpleName(inst), t
            ), t).printStackTrace();
          }
        }
      }
    }
    return (T) thawed;
  }
  
  
}

