package org.d6r;

import org.d6r.PosixFileInputStream;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import jadx.core.dex.attributes.annotations.Annotation;
import jadx.core.dex.attributes.annotations.Annotation.Visibility;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.DecodeException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.d6r.PosixFileInputStream;
import jadx.api.IJadxArgs;
import org.d6r.JadxArgs;
import java.io.File;
import jadx.core.utils.files.InputFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.ClassNode;
import org.apache.commons.io.FileUtils;

import com.android.dex.Dex;
import com.android.dex.Dex.Section; // read actual data
import com.android.dex.TableOfContents;
/**
// Metadata holder.
public static class TableOfContents.Section {
  public int byteCount = 29160;
  public int off = 1611272;
  public int size = 1357;
  public final short type = 8196;
  
  // ctor
  public TableOfContents.Section<init>(int type);
  // methods
  public int compareTo(Section section);
  public boolean exists();
  public String toString();
}
*/
import com.android.dex.Dex;
import com.android.dex.EncodedValueReader;
import com.android.dex.TableOfContents;
import com.google.common.collect.ImmutableBiMap;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.ConstUtil;
import org.d6r.JadxArgs;
import org.d6r.Reflect;
import org.d6r.Reflector;
import jadx.core.dex.nodes.parser.EncValueParser;

public class AnnotationsParser {

  private static final Visibility[] VISIBILITIES = {
      Visibility.BUILD,
      Visibility.RUNTIME,
      Visibility.SYSTEM
  };

  public DexNode dex;
  public ClassNode cls;
  public RootNode root;
  public InputFile input;
  public Dex clsDex;
  public Class<?> clazz;
  public TableOfContents toc;
  public IJadxArgs args;
  
  public static final ImmutableBiMap<Integer, String> CONST_MAP = 
    ConstUtil.constantMap(EncodedValueReader.class,"ENCODED_",-1);
  
  public static final LazyMember<Constructor<InputFile>> INPUT_FILE_CTOR = 
    LazyMember.of(InputFile.class, "<init>", File.class);
  
  public static class TocSectionInfo {

    public final Dex clsDex;
    public final TableOfContents toc;
    public final int index;
    public final int off;
    public final Field fld;
    public final String name;
    public final TableOfContents.Section tocSec;
    public final int size;
    
    public TocSectionInfo(Dex clsDex, int off) {
      this.clsDex = clsDex;      
      this.toc = clsDex.getTableOfContents();
      this.off = off;
      Pair<Field, TableOfContents.Section> pair 
        = findBySectionStart(this.toc, this.off);
      this.fld = pair.getKey();
      this.name = this.fld.getName();
      this.tocSec = pair.getValue();
      this.size = this.tocSec.size;
      this.index = getTocSecIndex(this.toc, this.tocSec);      
    }
    
    public static Pair<Field, TableOfContents.Section>
    findBySectionStart(TableOfContents toc, int initPos) 
    {
      for (Field fld: TableOfContents.class.getDeclaredFields()) {
        if (!fld.getType().equals(TableOfContents.Section.class)) {
          continue; 
        }
        TableOfContents.Section thisFldTocSec = null; 
        try {
          fld.setAccessible(true);
          thisFldTocSec = (TableOfContents.Section) fld.get(toc);
        } catch (ReflectiveOperationException ex) {
          throw Reflector.Util.sneakyThrow(ex);
        }
        int thisFldOff = thisFldTocSec.off;
        if (thisFldOff != initPos) continue; 
        return Pair.of(fld, thisFldTocSec);
       }
       return null;
    }
    public static int 
    getTocSecIndex(TableOfContents toc, 
    TableOfContents.Section ts)
    {
      int idx = -1; 
      TableOfContents.Section[] tocSecs = toc.sections; 
      int len = tocSecs.length; 
      while (++idx < len) { 
        if (tocSecs[idx].equals(ts)) return idx;
      }
      return -1;      
    }
  }
  
  public static ByteBuffer reset(Dex.Section s) { 
    int initPos 
     = Reflect.<Integer>getfldval(s,"initialPosition").intValue();
    ByteBuffer secBuf = Reflect.getfldval(s, "data");
    secBuf.position(initPos);
    return secBuf;
  }
  
  public TocSectionInfo lookupTocSection(Dex.Section s) {
    int initPos 
     = Reflect.<Integer>getfldval(s,"initialPosition").intValue();
    return new TocSectionInfo(clsDex, initPos);
 }

  public AnnotationsParser(Class<?> clazz) {
    this.clazz = clazz;
    this.clsDex = getDex(clazz);    
    this.toc = this.clsDex.getTableOfContents();
    try {
      File outDir = PosixFileInputStream.createTemporaryDirectory("out");       
      File dexFile = File.createTempFile(String.format(
        "dexfile_%s", clazz.getName().replace('$', '_')
      ), ".dex"); 
      byte[] bytes = clsDex.getBytes();
      FileUtils.writeByteArrayToFile(dexFile, bytes); 
      IJadxArgs args = new JadxArgs(outDir.getAbsolutePath());
      this.args = args;
      InputFile inputFile = INPUT_FILE_CTOR.newInstance(dexFile); 
      this.input = inputFile;
      RootNode root = new RootNode(args);
      this.root = root;
      root.load(Arrays.asList(inputFile));
      ClassNode classNode 
        = root.searchClassByName(clazz.getName());
      this.cls = classNode;
      DexNode dexNode = root.getDexNodes().get(0);
      this.dex = dexNode;
    } catch (Throwable e) { 
      throw Reflector.Util.sneakyThrow(e);
    }
  }

  public void parse(int offset) throws DecodeException {
    Section section = dex.openSection(offset);

    // TODO read as unsigned int
    int classAnnotationsOffset = section.readInt();
    int fieldsCount = section.readInt();
    int annotatedMethodsCount = section.readInt();
    int annotatedParametersCount = section.readInt();
   ClassNode cls = (ClassNode) this.cls;
   
    if (classAnnotationsOffset != 0) {
      cls.addAttr(readAnnotationSet(classAnnotationsOffset));
    }

    for (int i = 0; i < fieldsCount; i++) {
      FieldNode f = cls.searchFieldById(section.readInt());
      f.addAttr(readAnnotationSet(section.readInt()));
    }

    for (int i = 0; i < annotatedMethodsCount; i++) {
      MethodNode m = cls.searchMethodById(section.readInt());
      m.addAttr(readAnnotationSet(section.readInt()));
    }

    for (int i = 0; i < annotatedParametersCount; i++) {
      MethodNode mth = cls.searchMethodById(section.readInt());
      // read annotation ref list
      Section ss = dex.openSection(section.readInt());
      int size = ss.readInt();
      MethodParameters params = new MethodParameters(size);
      for (int j = 0; j < size; j++) {
        params.getParamList().add(readAnnotationSet(ss.readInt()));
      }
      mth.addAttr(params);
    }
  }

  private AnnotationsList readAnnotationSet(int offset) 
   throws DecodeException 
 {
    if (offset == 0) {
      return AnnotationsList.EMPTY;
    }
    Section section = dex.openSection(offset);
    int size = section.readInt();
    if (size == 0) {
      return AnnotationsList.EMPTY;
    }
    List<Annotation> list = new ArrayList<Annotation>(size);
    for (int i = 0; i < size; i++) {
      Section anSection = dex.openSection(section.readInt());
      Annotation a = readAnnotation(dex, anSection, true);
      list.add(a);
    }
    return new AnnotationsList(list);
  }

  public static Annotation readAnnotation(DexNode dex, Section s, boolean readVisibility) throws DecodeException {
    EncValueParser parser = new EncValueParser(dex, s);
    Visibility visibility = null;
    if (readVisibility) {
      byte v = s.readByte();
      visibility = VISIBILITIES[v];
    }
    int typeIndex = s.readUleb128();
    int size = s.readUleb128();
    Map<String, Object> values = new LinkedHashMap<String, Object>(size);
    for (int i = 0; i < size; i++) {
      String name = dex.getString(s.readUleb128());
      values.put(name, parser.parseValue());
    }
    ArgType type = dex.getType(typeIndex);
    Annotation annotation = new Annotation(visibility, type, values);
    if (!type.isObject()) {
      throw new DecodeException("Incorrect type for annotation: " + annotation);
    }
    return annotation;
  }
}
