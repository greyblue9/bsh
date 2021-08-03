
package org.d6r;

import com.android.dex.Annotation;
// import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import com.android.dex.ClassData;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.android.dex.DexException;
import com.android.dex.FieldId;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.android.dex.SizeOf;
import com.android.dex.TableOfContents;
import com.android.dex.TypeList;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.*;
import java.io.*;
import java8.util.Optional;
import static java.lang.Math.ceil;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.android.dx.io.CodeReader;
import com.android.dx.io.Opcodes;
import com.android.dx.io.instructions.DecodedInstruction;
import com.android.dx.io.instructions.ShortArrayCodeOutput;

import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.IndexMap;

import org.d6r.ADexMerger.IdMerger;
import org.d6r.ADexMerger.SortableType;
import org.d6r.ADexMerger.WriterSizes;
import org.d6r.ADexMerger.InstructionTransformer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;



public class ADexMerger {
  
  public static boolean VERBOSE;
  Map<Integer, DecodedInstruction[]> insns = new HashMap<>();
  
  Dex dexA;
  Dex dexOut;
  Object result;
  boolean beenProcessed;
  
  CollisionPolicy collisionPolicy;
  WriterSizes writerSizes;
  int compactWasteThreshold;
  
  TableOfContents aContents;
  IndexMap aIndexMap;
  InstructionTransformer aInstructionTransformer;
  Dex.Section headerOut;
  Dex.Section idsDefsOut;
  TableOfContents contentsOut;
  Dex.Section mapListOut;
  Dex.Section typeListOut;
  Dex.Section annotationSetRefListOut;
  Dex.Section annotationSetOut;
  Dex.Section classDataOut;
  Dex.Section codeOut;
  Dex.Section stringDataOut;
  Dex.Section debugInfoOut;
  Dex.Section annotationOut;
  Dex.Section encodedArrayOut;
  Dex.Section annotationsDirectoryOut;
  
  /*
  public static class IndexMapData {
    public Dex target;
    public int[] stringIds;
    public short[] fieldIds;
    public short[] methodIds;
    public short[] protoIds;
    public short[] typeIds;
    public Map<Integer, Integer> annotationDirectoryOffsets;
    public Map<Integer, Integer> annotationOffsets;
    public Map<Integer, Integer> annotationSetOffsets;
    public Map<Integer, Integer> annotationSetRefListOffsets;
    public Map<Integer, Integer> staticValuesOffsets;
    public Map<Integer, Integer> typeListOffsets;
  }
  */
  @NonDumpable
  static Set<? super Throwable> errors;
  static Map<Throwable, Object> errorData;
  
  static <T extends Throwable> T error(T throwable, Object... objs) {
    if (errors == null) {
      errors = new IdentityHashSet<>();
      errorData = new IdentityHashMap<>();
    }
    
    if (errors.add(throwable)) {
      errorData.put(throwable, Arrays.asList(objs));
      try {
        for (final Object obj: objs) {
          try {
            System.err.println(Debug.ToString(obj));
          } catch (Throwable e0) {
            try {
              System.err.println(obj);
            } catch (Throwable e1) { 
              System.err.println(Debug.tryToString(e1,0,0));
              System.err.println(Debug.tryToString(obj,0,0));
            }
          }
        }
      } catch (Throwable e2) {
        System.err.println(e2.getClass().getName());
        if (! (e2 instanceof StackOverflowError) 
        &&  ! (e2 instanceof OutOfMemoryError)) e2.printStackTrace();
      }
      try {
        throwable.printStackTrace();
      } catch (Throwable e3) {
        System.err.println(e3.getClass().getName());
        if (! (e3 instanceof StackOverflowError) 
        &&  ! (e3 instanceof OutOfMemoryError)) {
          Throwable rc = ExceptionUtils.getRootCause(e3);
          rc.printStackTrace();
        }
      }
    }
    return throwable;
  }
  
  public ADexMerger(@Nonnull final Dex dex) {
    this(dex, (CollisionPolicy) null, (WriterSizes) null);
  }
  
  public ADexMerger(@Nonnull final Dex dex,
  @Nullable final CollisionPolicy _collisionPolicy,
  @Nullable final WriterSizes _writerSizes)
  {
    try {
      this.dexA = dex;
      this.collisionPolicy = (_collisionPolicy != null)
        ? _collisionPolicy
        : CollisionPolicy.KEEP_FIRST;
      this.writerSizes = (_writerSizes != null)
        ? _writerSizes
        : new WriterSizes(dexA);
      this.compactWasteThreshold = 1024 * 1024;
      try {
        this.dexOut = new Dex(writerSizes.size());
      } catch (IOException ioException) {
        throw Reflector.Util.sneakyThrow(ioException);
      }
      this.aContents = dexA.getTableOfContents();
      this.aIndexMap = new IndexMap(dexOut, aContents);
      
      this.aInstructionTransformer = new InstructionTransformer(aIndexMap);
      
      this.headerOut = dexOut.appendSection(writerSizes.header, "header");
      this.idsDefsOut = dexOut.appendSection(writerSizes.idsDefs, "ids defs");
      
      this.contentsOut = dexOut.getTableOfContents();
      contentsOut.dataOff = dexOut.getNextSectionStart();
      contentsOut.mapList.off = dexOut.getNextSectionStart();
      contentsOut.mapList.size = 1;
      
      this.mapListOut = dexOut.appendSection(writerSizes.mapList, "map list");
      contentsOut.typeLists.off = dexOut.getNextSectionStart();
      this.typeListOut = dexOut.appendSection(
        writerSizes.typeList, "typelist");
      contentsOut.annotationSetRefLists.off = dexOut.getNextSectionStart();
      this.annotationSetRefListOut = dexOut.appendSection(
        writerSizes.annotationsSetRefList, "annotation set ref list");
      contentsOut.annotationSets.off = dexOut.getNextSectionStart();
      this.annotationSetOut = dexOut.appendSection(
        writerSizes.annotationsSet, "annotation sets");
      contentsOut.classDatas.off = dexOut.getNextSectionStart();
      this.classDataOut = dexOut.appendSection(
        writerSizes.classData, "class data");
      contentsOut.codes.off = dexOut.getNextSectionStart();
      this.codeOut = dexOut.appendSection(
        writerSizes.code, "code");
      contentsOut.stringDatas.off = dexOut.getNextSectionStart();
      this.stringDataOut = dexOut.appendSection(
        writerSizes.stringData, "string data");
      contentsOut.debugInfos.off = dexOut.getNextSectionStart();
      this.debugInfoOut = dexOut.appendSection(
        writerSizes.debugInfo, "debug info");
      contentsOut.annotations.off = dexOut.getNextSectionStart();
      this.annotationOut = dexOut.appendSection(
        writerSizes.annotation, "annotation");
      contentsOut.encodedArrays.off = dexOut.getNextSectionStart();
      this.encodedArrayOut = dexOut.appendSection(
        writerSizes.encodedArray, "encoded array");
      contentsOut.annotationsDirectories.off = dexOut.getNextSectionStart();
      this.annotationsDirectoryOut = dexOut.appendSection( 
        writerSizes.annotationsDirectory, "annotations directory");
      contentsOut.dataSize
        = dexOut.getNextSectionStart() - contentsOut.dataOff;
    } catch (Throwable ex) {
      error(ex);
    }
  }
  
  
  public Dex parse() {
    return this.parse(false);
  }
  
  public Dex parse(boolean allowCompact) {
    synchronized (this) {
      if (! beenProcessed) {
        beenProcessed = true;
        try {
          mergeStringIds();
          mergeTypeIds();
          mergeTypeLists();
          mergeProtoIds();
          mergeFieldIds();
          mergeMethodIds();
          mergeAnnotations();
          unionAnnotationSetsAndDirectories();
          mergeClassDefs();
      
          // write the header
          this.contentsOut.header.off = 0;
          this.contentsOut.header.size = 1;
          this.contentsOut.fileSize = dexOut.getLength();
          this.contentsOut.computeSizesFromOffsets();
          this.contentsOut.writeHeader(this.headerOut);
          this.contentsOut.writeMap(this.mapListOut);
          
          // generate and write the hashes
          dexOut.writeHashes();
          
          if (allowCompact) dexOut = compact(dexOut);
          this.result = dexOut;
        } catch (Throwable ex) {
          this.result = ex;
        } finally {
        }
      }
    }
    if (this.result instanceof Throwable) {
      throw Reflector.Util.sneakyThrow((Throwable) result);
    } else {
      return (Dex) result;
    }  
  }
  
  
  /**
  Name and structure of a type. Used to order types such that each type is
  preceded by its supertype and implemented interfaces.
  */
  public static class SortableType {
  
    public static final Comparator<SortableType> NULLS_LAST_ORDER = new 
    Comparator<SortableType>() {
      @Override public int compare(SortableType a, SortableType b) {
        if (a == b) {
          return 0;
        }
        if (b == null) {
          return -1;
        }
        if (a == null) {
          return 1;
        }
        if (a.depth != b.depth) {
          return a.depth - b.depth;
        }
        return a.getTypeIndex() - b.getTypeIndex();
      }
    };
  
    public final Dex dex;
    public final IndexMap indexMap;
    public ClassDef classDef;
    public int depth = -1;
    public Object mirror;
  
    public SortableType(Dex dex, IndexMap indexMap, ClassDef classDef) {
      this(dex, indexMap, classDef, -1);
    }
    
    public SortableType(Dex dex, IndexMap indexMap, ClassDef classDef,
    int depth)
    {
      this.dex = dex;
      this.indexMap = indexMap;
      this.classDef = classDef;
      this.depth = depth;
    }
    
    public Dex getDex() {
      return dex;
    }
  
    public IndexMap getIndexMap() {
      return indexMap;
    }
  
    public ClassDef getClassDef() {
      return classDef;
    }
  
    public int getTypeIndex() {
      return classDef.getTypeIndex();
    }
    
    /**
    Assigns this type's depth if the depths of its supertype and implemented
    interfaces are known. Returns false if the depth couldn't be computed
    yet.
    */
    public boolean tryAssignDepth(SortableType[] types) {
      int max;
      if (classDef.getSupertypeIndex() == ClassDef.NO_INDEX) {
        max = 0;// this is Object.class or an interface
  
      } else {
        SortableType sortableSupertype
          = types[classDef.getSupertypeIndex()];
        if (sortableSupertype == null) {
          max = 1;// unknown, so assume it's a root.
  
        } else if (sortableSupertype.depth == -1) {
          return false;
        } else {
          max = sortableSupertype.depth;
        }
      }
      for (short interfaceIndex : classDef.getInterfaces()) {
        SortableType implemented = types[interfaceIndex];
        if (implemented == null) {
          max = Math.max(max, 1);// unknown, so assume it's a root.
  
        } else if (implemented.depth == -1) {
          return false;
        } else {
          max = Math.max(max, implemented.depth);
        }
      }
      depth = max + 1;
      return true;
    }
  
    public boolean isDepthAssigned() {
      return depth != -1;
    }
  }
  
  
  public static class InstructionTransformer {
  
    public final CodeReader reader;
  
    public DecodedInstruction[] mappedInstructions;
  
    public int mappedAt;
  
    public IndexMap indexMap;
    
    Map<Integer, DecodedInstruction[]> insns;
  
    public InstructionTransformer(IndexMap indexMap) {
      this.reader = new CodeReader();
      this.indexMap = indexMap;
      this.reader.setAllVisitors(new GenericVisitor());
      this.reader.setStringVisitor(new StringVisitor());
      this.reader.setTypeVisitor(new TypeVisitor());
      this.reader.setFieldVisitor(new FieldVisitor());
      this.reader.setMethodVisitor(new MethodVisitor());
    }
    
    public InstructionTransformer() {
      this((IndexMap) null);
    }
    
    public short[] transform(short[] encodedInstructions)
      throws DexException
    {
      return this.transform(
         (this.indexMap != null
           ? this.indexMap
           : InstructionTransformer.this.indexMap), encodedInstructions);
    }
    
    public short[] transform(IndexMap _indexMap, 
    short[] encodedInstructions) 
      throws DexException
    {
      DecodedInstruction[] decodedInstructions 
        = DecodedInstruction.decodeAll(encodedInstructions);
      int size = decodedInstructions.length;
      IndexMap _origIndexMap = this.indexMap;
      //System.err.printf(
      //"(this.indexMap) _origIndexMap: %s\n", _origIndexMap);
      short[] ret = null;
      DecodedInstruction __insn = null;
      ShortArrayCodeOutput out = null;
      try {
        try {
          try {
            mappedInstructions = new DecodedInstruction[size];
            mappedAt = 0;
            reader.visitAll(decodedInstructions);
            out = new ShortArrayCodeOutput(size);
            for (DecodedInstruction instruction : mappedInstructions) {
              __insn = instruction;
              if (instruction != null) {
                instruction.encode(out);
              }
            }
          } finally {
            ret = (out != null? out.getArray(): encodedInstructions);
          }
        } finally {
          this.indexMap = _origIndexMap;
        }
        return ret;
      } catch (Throwable e) { 
        throw Reflector.Util.sneakyThrow(
          error(e, _origIndexMap, _indexMap, encodedInstructions,
            decodedInstructions, mappedInstructions, reader, 
            out, __insn)
        ); 
      }
      // this.indexMap = null;
      // return out.getArray();
    }
    public class GenericVisitor implements CodeReader.Visitor {
  
      public void visit(DecodedInstruction[] all, DecodedInstruction one) {
        mappedInstructions[mappedAt++] = one;
      }
    }
  
    public class StringVisitor implements CodeReader.Visitor {
  
      public void visit(DecodedInstruction[] all, DecodedInstruction one) {
        int stringId = one.getIndex();
        int mappedId =
         (InstructionTransformer.this.indexMap).adjustString(stringId);
        boolean isJumbo = (one.getOpcode() == Opcodes.CONST_STRING_JUMBO);
        jumboCheck(isJumbo, mappedId);
        mappedInstructions[mappedAt++] = one.withIndex(mappedId);
      }
    }
  
    public class FieldVisitor implements CodeReader.Visitor {
  
      public void visit(DecodedInstruction[] all, DecodedInstruction one) {
        int fieldId = one.getIndex();
        int mappedId = 
         (InstructionTransformer.this.indexMap) .adjustField(fieldId);
        boolean isJumbo = (one.getOpcode() == Opcodes.CONST_STRING_JUMBO);
        jumboCheck(isJumbo, mappedId);
        mappedInstructions[mappedAt++] = one.withIndex(mappedId);
      }
    }
  
    public class TypeVisitor implements CodeReader.Visitor {
  
      public void visit(DecodedInstruction[] all, DecodedInstruction one) {
        int typeId = one.getIndex();
        int mappedId = 
         (InstructionTransformer.this.indexMap).adjustType(typeId);
        boolean isJumbo = (one.getOpcode() == Opcodes.CONST_STRING_JUMBO);
        jumboCheck(isJumbo, mappedId);
        mappedInstructions[mappedAt++] = one.withIndex(mappedId);
      }
    }
  
    public class MethodVisitor implements CodeReader.Visitor {
  
      public void visit(DecodedInstruction[] all, DecodedInstruction one) {
        int methodId = one.getIndex();
        int mappedId = 
         (InstructionTransformer.this.indexMap).adjustMethod(methodId);
        boolean isJumbo = (one.getOpcode() == Opcodes.CONST_STRING_JUMBO);
        jumboCheck(isJumbo, mappedId);
        mappedInstructions[mappedAt++] = one.withIndex(mappedId);
        if (! insns.containsKey(methodId)) {
          insns.put(methodId, mappedInstructions);
        }
      }
    }
    
    
  
    public static void jumboCheck(boolean isJumbo, int newIndex) {
      if (!isJumbo && (newIndex > 0xffff)) {
        throw new RuntimeException(
          "Cannot merge new index " + newIndex 
          + " into a non-jumbo instruction!"
        );
      }
    }
  }
  
  

  public static class WriterSizes {
      public int header;
      public int idsDefs;
      public int mapList;
      public int typeList;
      public int classData;
      public int code;
      public int stringData;
      public int debugInfo;
      public int encodedArray;
      public int annotationsDirectory;
      public int annotationsSet;
      public int annotationsSetRefList;
      public int annotation;
      
      public WriterSizes(final Dex dex) {
        this.header = 112;
        try {
          this.plus(dex.getTableOfContents(), false);          
        } catch (Throwable e) {
          (ADexMerger.error(new RuntimeException(String.format(
            "Problem calling ((WriterSizes) this).plus(%s, false): %s",
            e
          ), e))).printStackTrace();
        }
        this.fourByteAlign();
      }
      
      public WriterSizes(ADexMerger dexMerger) {
        this.header = 112;
        this.header = dexMerger.headerOut.used();
        this.idsDefs = dexMerger.idsDefsOut.used();
        this.mapList = dexMerger.mapListOut.used();
        this.typeList = dexMerger.typeListOut.used();
        this.classData = dexMerger.classDataOut.used();
        this.code = dexMerger.codeOut.used();
        this.stringData = dexMerger.stringDataOut.used();
        this.debugInfo = dexMerger.debugInfoOut.used();
        this.encodedArray = dexMerger.encodedArrayOut.used();
        this.annotationsDirectory = dexMerger.annotationsDirectoryOut.used();
        this.annotationsSet = dexMerger.annotationSetOut.used();
        this.annotationsSetRefList = dexMerger.annotationSetRefListOut.used();
        this.annotation = dexMerger.annotationOut.used();
        this.fourByteAlign();
      }
      
      public void plus(final TableOfContents contents, final boolean exact) {
        this.idsDefs
          += contents.stringIds.size * 4 
           + contents.typeIds.size * 4 
           + contents.protoIds.size * 12 
           + contents.fieldIds.size * 8 
           + contents.methodIds.size * 8 
           + contents.classDefs.size * 32;
        this.mapList = 4 + contents.sections.length * 12;
        this.typeList += fourByteAlign(contents.typeLists.byteCount);
        this.stringData += contents.stringDatas.byteCount;
        this.annotationsDirectory 
          += contents.annotationsDirectories.byteCount;
        this.annotationsSet += contents.annotationSets.byteCount;
        this.annotationsSetRefList 
          += contents.annotationSetRefLists.byteCount;
        if (exact) {
          this.code += contents.codes.byteCount;
          this.classData += contents.classDatas.byteCount;
          this.encodedArray += contents.encodedArrays.byteCount;
          this.annotation += contents.annotations.byteCount;
          this.debugInfo += contents.debugInfos.byteCount;
        } else {
          this.code += (int) ceil(contents.codes.byteCount * 1.25);
          this.classData += (int) ceil(contents.classDatas.byteCount * 1.34);
          this.encodedArray += contents.encodedArrays.byteCount * 2;
          this.annotation += (int) ceil(contents.annotations.byteCount * 2);
          this.debugInfo += contents.debugInfos.byteCount * 2;
        }
      }
      
      public void fourByteAlign() {
        this.header = fourByteAlign(this.header);
        this.idsDefs = fourByteAlign(this.idsDefs);
        this.mapList = fourByteAlign(this.mapList);
        this.typeList = fourByteAlign(this.typeList);
        this.classData = fourByteAlign(this.classData);
        this.code = fourByteAlign(this.code);
        this.stringData = fourByteAlign(this.stringData);
        this.debugInfo = fourByteAlign(this.debugInfo);
        this.encodedArray = fourByteAlign(this.encodedArray);
        this.annotationsDirectory = fourByteAlign(this.annotationsDirectory);
        this.annotationsSet = fourByteAlign(this.annotationsSet);
        this.annotationsSetRefList
          = fourByteAlign(this.annotationsSetRefList);
        this.annotation = fourByteAlign(this.annotation);
      }
      
      static int fourByteAlign(final int position) {
        return position + 3 & 0xFFFFFFFC;
      }
      
      public int size() {
        return this.header 
             + this.idsDefs + this.mapList + this.typeList
             + this.classData + this.code + this.stringData 
             + this.debugInfo + this.encodedArray 
             + this.annotationsDirectory + this.annotationsSet 
             + this.annotationsSetRefList + this.annotation;
      }
  }
  
  
  
     /**
     * Reads an IDs section of two dex files and writes an IDs section of a
     * merged dex file. Populates maps from old to new indices in the
      process.
     */
    class IdMerger<T extends Comparable<T>> {
        final Dex.Section out;
        final Class<T> type;
        
        public IdMerger(Class<T> itemType, Dex.Section out) {
            this.type = itemType;
            this.out = out;
        }

        /**
         * Merges already-sorted sections, reading only two values into memory
         * at a time.
         */
        public final void mergeSorted() {
            TableOfContents.Section aSection 
              = getSection(dexA.getTableOfContents());
            getSection(contentsOut).off = out.getPosition();

            Dex.Section inA = aSection.exists() ? dexA.open(aSection.off) : null;
            int aOffset = -1;
            int aIndex = 0;
            int outCount = 0;
            T a = null;

            while (true) {
                if (a == null && aIndex < aSection.size) {
                    aOffset = inA.getPosition();
                    a = read(inA, aIndexMap, aIndex);
                }

                // Write the smaller of a and b.
                // If they're equal, write only once
                boolean advanceA = a != null;

                T toWrite = null;
                if (advanceA) {
                    toWrite = a;
                    updateIndex(aOffset, aIndexMap, aIndex++, outCount);
                    a = null;
                    aOffset = -1;
                }
                if (toWrite == null) {
                    break; // advanceA == false && advanceB == false
                }
                write(toWrite);
                outCount++;
            }

            getSection(contentsOut).size = outCount;
        }

        /**
         Merges unsorted sections by reading them completely into memory,
         and sorting in memory.
         */
        public final void mergeUnsorted() {
            getSection(contentsOut).off = out.getPosition();

            List<UnsortedValue> all = new ArrayList<UnsortedValue>();
            all.addAll(readUnsortedValues(dexA, aIndexMap));
            Collections.sort(all);

            int outCount = 0;
            for (int i = 0; i < all.size(); ) {
                UnsortedValue e1 = all.get(i++);
                updateIndex(e1.offset, getIndexMap(e1.source), e1.index, outCount - 1);

                while (i < all.size() && e1.compareTo(all.get(i)) == 0) {
                    UnsortedValue e2 = all.get(i++);
                    updateIndex(e2.offset, getIndexMap(e2.source), e2.index, outCount - 1);
                }

                write(e1.value);
                outCount++;
            }

            getSection(contentsOut).size = outCount;
        }

        List<UnsortedValue> readUnsortedValues(Dex source,
        IndexMap indexMap)
        {
            TableOfContents.Section section
              = getSection(source.getTableOfContents());
            
            if (!section.exists()) return Collections.emptyList();

            List<UnsortedValue> result = new ArrayList<UnsortedValue>();
            Dex.Section in = source.open(section.off);
            for (int i = 0; i < section.size; i++) {
                int offset = in.getPosition();
                T value = read(in, indexMap, 0);
                result.add(
                  new UnsortedValue(source, indexMap, value, i, offset)
                );
            }
            return result;
        }

        public TableOfContents.Section getSection(TableOfContents tableOfContents)
        {
          if (type == String.class) return tableOfContents.stringIds;
          else if (type == Integer.class) return tableOfContents.typeIds;
          else if (type == TypeList.class) return tableOfContents.typeLists;
          else if (type == ProtoId.class) return tableOfContents.protoIds;
          else if (type == FieldId.class) return tableOfContents.fieldIds;
          else if (type == MethodId.class) return tableOfContents.methodIds;
          else if (type == Annotation.class) return tableOfContents.annotations;
          else {
            throw new IllegalArgumentException(
              type != null
                ? type.getName()
                : "null"
            );
          }
        }
        
        public T read(Dex.Section in, IndexMap indexMap, int index) {
          T ret = null;
          if (type == String.class) {
            ret = (T) (Object) in.readString();
          } else if (type == Integer.class) {
            int stringIndex = in.readInt();
            ret = (T) (Object) indexMap.adjustString(stringIndex);
          } else if (type == TypeList.class) {
            ret = (T) (Object) indexMap.adjustTypeList(in.readTypeList());
          } else if (type == ProtoId.class) {
            ret = (T) (Object) indexMap.adjust(in.readProtoId());
          } else if (type == FieldId.class) {
            ret = (T) (Object) indexMap.adjust(in.readFieldId());
          } else if (type == MethodId.class) {
            ret = (T) (Object) indexMap.adjust(in.readMethodId());
          } else if (type == Annotation.class) {
            ret = (T) (Object) indexMap.adjust(in.readAnnotation());
          } else {
            throw new IllegalArgumentException(
              type != null
                ? type.getName()
                : "null"
            );
          }
          if (VERBOSE) System.err.printf(
            "read() returning: %s\n", Debug.ToString(ret)
          );
          return ret;
        }
        
        public void updateIndex(int offset, IndexMap indexMap,
        int oldIndex, int newIndex)
        { 
          if (VERBOSE) System.err.printf(
            "<%s>updateIndex(int offset: %d, IndexMap indexMap: %s, " +
            "int oldIndex: %d, int newIndex: %d)\n", 
            type != null? type.getClass().getSimpleName(): "null",
            offset, Debug.ToString(indexMap), oldIndex, newIndex
          );
          if (newIndex < 0 || newIndex > 0xffff) {
            if (type == Integer.class ||
                type == ProtoId.class ||
                type == FieldId.class ||
                type == MethodId.class)
            {
              throw new ArrayIndexOutOfBoundsException(String.format(
                "Too many %s items; " +
                "required ID exceeds representable range " +
                "[0, 0xFFFF];" +
                "newIndex: %d (0x%x), oldIndex: %d (0x%x)",
                type.getSimpleName(), newIndex, newIndex, oldIndex, oldIndex
              ));
            }            
          } 
          
          if (type == String.class) {
            indexMap.stringIds[oldIndex] = newIndex;           
          } else if (type == Integer.class) {
            indexMap.typeIds[oldIndex] = (short) newIndex;            
          } else if (type == TypeList.class) {
            indexMap.putTypeListOffset(offset, typeListOut.getPosition());
          } else if (type == ProtoId.class) {
            indexMap.protoIds[oldIndex] = (short) newIndex;
          } else if (type == FieldId.class) {
            indexMap.fieldIds[oldIndex] = (short) newIndex;
          } else if (type == MethodId.class) {
            indexMap.methodIds[oldIndex] = (short) newIndex;
          } else if (type == Annotation.class) {
            indexMap.putAnnotationOffset(
              offset, annotationOut.getPosition());
          } else {
            throw new IllegalArgumentException(
              type != null
                ? type.getName()
                : "null"
            );
          }
        }
        
        public void write(T value) {
          if (VERBOSE) System.err.printf(
            "write: (T: %s) value = %s\n", 
            value != null? value.getClass().getName(): "null",
            Debug.tryToString(value, 0, 2)
          );
          if (type == String.class) {
            contentsOut.stringDatas.size++;
            idsDefsOut.writeInt((int) stringDataOut.getPosition());
            stringDataOut.writeStringData((String) value);
          } else if (type == Integer.class) {
            idsDefsOut.writeInt((Integer) value);
          } else if (type == TypeList.class) {
            typeListOut.writeTypeList((TypeList) value);
          } else if (type == ProtoId.class) {
            ((ProtoId) value).writeTo(idsDefsOut);
          } else if (type == FieldId.class) {
            ((FieldId) value).writeTo(idsDefsOut);
          } else if (type == MethodId.class) {
            ((MethodId) value).writeTo(idsDefsOut);
          } else if (type == Annotation.class) {
            ((Annotation) value).writeTo(annotationOut);
          } else {
            throw new IllegalArgumentException(
              type != null
                ? type.getName()
                : "null"
            );
          }
        }
        
        class UnsortedValue implements Comparable<UnsortedValue> {
            final Dex source;
            final IndexMap indexMap;
            final T value;
            final int index;
            final int offset;

            public UnsortedValue(Dex source, IndexMap indexMap, T value, int index,
            int offset) {
                this.source = source;
                this.indexMap = indexMap;
                this.value = value;
                this.index = index;
                this.offset = offset;
            }

            public int compareTo(UnsortedValue unsortedValue) {
                return value.compareTo(unsortedValue.value);
            }
        }
        
    }
    
         
    
  public Dex compact(Dex input) {
    // We use pessimistic sizes when merging dex files. If those sizes
    // result in too many bytes wasted, compact the result. To compact,
    // simply merge the result with itself.
    WriterSizes compactedSizes = new WriterSizes(this);
    int wastedByteCount = writerSizes.size() - compactedSizes.size();
    if (wastedByteCount >  + compactWasteThreshold) {
      ADexMerger compacter = new ADexMerger(
        dexOut, CollisionPolicy.FAIL, compactedSizes
      );
      return compacter.parse(true);
    }
    return input;
  }


    public IndexMap getIndexMap(Dex dex) {
        if (dex == dexA) {
            return aIndexMap;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void mergeStringIds() {
        this.new IdMerger<String>(String.class, idsDefsOut).mergeSorted();
    }
    public void mergeTypeIds() {
        this.new IdMerger<Integer>(Integer.class, idsDefsOut).mergeSorted();
    }
    public void mergeTypeLists() {
      IdMerger<TypeList> merger = null;
      try {
        (merger = this.new IdMerger<TypeList>(
          TypeList.class, typeListOut
        )).mergeUnsorted();
      } catch (Throwable e) {
        error(e, merger, typeListOut);
      }
    }
    
    public void mergeProtoIds() {
        this.new IdMerger<ProtoId>(ProtoId.class, idsDefsOut).mergeSorted();
    }
    public void mergeFieldIds() {
        this.new IdMerger<FieldId>(FieldId.class, idsDefsOut).mergeSorted();
    }
    public void mergeMethodIds() {
        this.new IdMerger<MethodId>(
          MethodId.class, idsDefsOut
        ).mergeSorted();
    }
    public void mergeAnnotations() {
        this.new IdMerger<Annotation>(
          Annotation.class, annotationOut
        ).mergeUnsorted();
    }

    public void mergeClassDefs() {
        SortableType[] types = getSortedTypes();
        contentsOut.classDefs.off = idsDefsOut.getPosition();
        contentsOut.classDefs.size = types.length;

        for (SortableType type : types) {
            Dex in = ((SortableType) type).getDex();
            IndexMap indexMap = (in == dexA) ? aIndexMap : null;
            
            transformClassDef(in, type.getClassDef(), indexMap);
        }
    }

    /**
     * Returns the union of classes from both files, sorted in order such that
     * a class is always preceded by its supertype and implemented interfaces.
     */
    SortableType[] getSortedTypes() {
        // size is pessimistic; doesn't include arrays
        SortableType[] sortableTypes = new SortableType[contentsOut.typeIds.size];
        readSortableTypes(sortableTypes, dexA, aIndexMap);


        /*
         * Populate the depths of each sortable type. This makes D iterations
         * through all N types, where 'D' is the depth of the deepest type. For
         * example, the deepest class in libcore is Xalan's KeyIterator, which
         * is 11 types deep.
         */
        while (true) {
            boolean allDone = true;
            for (SortableType sortableType : sortableTypes) {
                if (sortableType != null && !sortableType.isDepthAssigned()) {
                    allDone &= sortableType.tryAssignDepth(sortableTypes);
                }
            }
            if (allDone) {
                break;
            }
        }

        // Now that all types have depth information, the result can be sorted
        Arrays.sort(sortableTypes, SortableType.NULLS_LAST_ORDER);

        // Strip nulls from the end
        int firstNull = Arrays.asList(sortableTypes).indexOf(null);
        return firstNull != -1
                ? Arrays.copyOfRange(sortableTypes, 0, firstNull)
                : sortableTypes;
    }
    

    static String c_className = "com.android.dx.merge.SortableType";
    Optional<Field> fld_indexMap;
    Field fld_classDef;
    Field fld_depth;
    Field fld_dex;
    Class<?> c;
    
    
    public SortableType convertFromObject(Object o)
    {
      if (o == null) return null;
      if (this.fld_indexMap == null) initConverterFields(o.getClass());
      
      try {
        final Dex dex = (Dex) fld_dex.get(o);
        final IndexMap indexMap = (IndexMap) ((fld_indexMap.isPresent())
            ? (Object) fld_indexMap.get().get(o)
            : (Object) null);
        final ClassDef classDef = (ClassDef) fld_classDef.get(o);
        final Integer _objDepth
            = (Integer) ((fld_depth != null)? fld_depth.get(o): null);
        final int depth = (_objDepth != null)? _objDepth.intValue(): -1;
        
        final SortableType st
          = new SortableType(dex, indexMap, classDef, depth);
        st.mirror = o;
        return st;
      } catch (ReflectiveOperationException ex) {
        throw Reflector.Util.sneakyThrow(ex);
      }
    }
    
    public <ST> ST converToObject(SortableType st)
    {
      if (st == null) return null;
      if (this.fld_indexMap == null) initConverterFields(st.getClass());
      
      final ST o = (ST) Reflect.allocateInstance(c);
      if (o == null) throw new AssertionError(String.format(
        "Reflect.allocateInstance(%s) returned null", c
      ));
      try {
        fld_dex.set(o, st.dex);
      } catch (IllegalAccessException iae) {
        Reflector.Util.sneakyThrow(iae);
      }
      if (fld_indexMap.isPresent()) {
        try {
          fld_indexMap.get().set(o, st.indexMap);
        } catch (IllegalAccessException iae) { iae.printStackTrace(); }
      }
      try {
        fld_classDef.set(o, st.classDef);
      } catch (IllegalAccessException iae) {
        Reflector.Util.sneakyThrow(iae);
      }
      if (fld_depth != null) {
        try {
          fld_depth.set(o, st.depth);
        } catch (IllegalAccessException iae) { iae.printStackTrace(); }
      }
      if (st.mirror == null) st.mirror = o;
      return o;
    }
    
    public void initConverterFields(Class<?> stCls) {
      try {
        c = (stCls != null) 
           ? stCls
           : Class.forName(
               c_className, false,
               Thread.currentThread().getContextClassLoader()
             );
        (fld_classDef = c.getDeclaredField("classDef")).setAccessible(true);
        (fld_depth = c.getDeclaredField("depth")).setAccessible(true);
        (fld_dex = c.getDeclaredField("dex")).setAccessible(true);
        Field f_indexMap = null;
        try {
          f_indexMap = c.getDeclaredField("indexMap");
          try {
            f_indexMap.setAccessible(true);
            this.fld_indexMap = Optional.of(f_indexMap);
          } catch (Throwable iae) {
            this.fld_indexMap = Optional.empty();
            new RuntimeException(String.format(
              "[WARN] Type `%s' has an `indexMap' field (%s), " +
              "but an %s was thrown while calling setAccessible(true): %s",
              c.getName(), f_indexMap.toGenericString(),
              iae.getClass().getSimpleName(), iae
            ), iae).printStackTrace();
          }
        } catch (NoSuchFieldException nsfe) {
          this.fld_indexMap = Optional.empty();
          System.err.printf(
            "[INFO] No `indexMap' field is available in type `%s': %s\n",
            c.getName(), nsfe
          );
        }
      } catch (ReflectiveOperationException ex) {
        try {
          this.fld_indexMap = Optional.empty();
          final String message = String.format(
            "%s#initConverterFields(Class<?> stCls: %s): %s",
            getClass().getName(), stCls, ex.getClass().getSimpleName()
          );
          System.err.printf("[ERROR] %s\n", message).flush();
          try {
            ex.addSuppressed(new RuntimeException(message));
          } catch (Throwable t) { }
        } finally {
          throw Reflector.Util.sneakyThrow(ex);
        }
      }
    }
    
    /**
    Reads just enough data on each class so that we can sort it and then
    find it later.
    */
    public SortableType _IndexMap_adjust(final IndexMap indexMap, 
    final SortableType sortableType)
    {
      final Dex inputDex = ((SortableType) sortableType).getDex();
      final ClassDef inputClassDef = sortableType.getClassDef();
      final ClassDef adjustedClassDef = indexMap.adjust(inputClassDef);
      
      return new SortableType(inputDex, indexMap, adjustedClassDef);
    }
    
    /*
    static final LazyMember IM_ADJUST_TYPE = LazyMember.of(
      IndexMap.class, "adjustType", Integer.TYPE);
    static final LazyMember IM_ADJUST_TYPELIST_OFFSET = LazyMember.of(
      IndexMap.class, "adjustType", Integer.TYPE);
    static final LazyMember IM_TARGET = LazyMember.of(
      "target", IndexMap.class);
    
    
    public ClassDef _IndexMap_adjust(final IndexMap indexMap, 
    final ClassDef classDef)
    {
      return new ClassDef(
        indexMap.target,
        classDef.getOffset(),
import java8.util.Optional;
        indexMap.adjustType(classDef.getTypeIndex()),
        classDef.getAccessFlags(),
        indexMap.adjustType(classDef.getSupertypeIndex()), 
        indexMap.adjustTypeListOffset(classDef.getInterfacesOffset()), 
        classDef.getSourceFileIndex(),
        classDef.getAnnotationsOffset(),
        classDef.getClassDataOffset(),
        classDef.getStaticValuesOffset()
      );
    }*/
    
    public void readSortableTypes(
    SortableType[] sortableTypes, Dex buffer,
    IndexMap indexMap)
    {
      for (final ClassDef classDef: buffer.classDefs()) {
        // SortableType sortableType
        // = indexMap.adjust(new SortableType(buffer, classDef));
        
        final SortableType
          inputType      = new SortableType(buffer, indexMap, classDef),
          adjustedType   = _IndexMap_adjust(indexMap, inputType);
        
        final int 
          inputTypeIndex = inputType.getTypeIndex(),
          adjTypeIndex   = adjustedType.getTypeIndex();
        
        if (sortableTypes[adjTypeIndex] == null) {
          sortableTypes[adjTypeIndex] = adjustedType;
        } else if (collisionPolicy != CollisionPolicy.KEEP_FIRST) {
          throw new DexException(String.format(
            "Multiple dex files define '%s'; typeIndex[A, B] = [%d, %d]",
            buffer.typeNames().get(classDef.getTypeIndex()),
            inputTypeIndex,
            adjTypeIndex
          ));
        } else {
          // keep existing type 
          //   (don't replace it with the newer one at `adjTypeIndex')
        }
      }
    }
    
    /**
     * Copy annotation sets from each input to the output.
     *
     * TODO: this may write multiple copies of the same annotation set.
     * We should shrink the output by merging rather than unioning
     */
    public void unionAnnotationSetsAndDirectories() {
        transformAnnotationSets(dexA, aIndexMap);
        transformAnnotationSetRefLists(dexA, aIndexMap);
        transformAnnotationDirectories(dexA, aIndexMap);
        transformStaticValues(dexA, aIndexMap);
    }

    public void transformAnnotationSets(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSets;
        if (section.exists()) {
            Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSet(indexMap, setIn);
            }
        }
    }

    public void transformAnnotationSetRefLists(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSetRefLists;
        if (section.exists()) {
            Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSetRefList(indexMap, setIn);
            }
        }
    }

    public void transformAnnotationDirectories(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationsDirectories;
        if (section.exists()) {
            Dex.Section directoryIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationDirectory(directoryIn, indexMap);
            }
        }
    }

    public void transformStaticValues(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().encodedArrays;
        if (section.exists()) {
            Dex.Section staticValuesIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformStaticValues(staticValuesIn, indexMap);
            }
        }
    }

    /**
     * Reads a class_def_item beginning at {@code in} and writes the index and
     * data.
     */
    public void transformClassDef(Dex in, ClassDef classDef, IndexMap indexMap) {
        idsDefsOut.assertFourByteAligned();
        idsDefsOut.writeInt(classDef.getTypeIndex());
        idsDefsOut.writeInt(classDef.getAccessFlags());
        idsDefsOut.writeInt(classDef.getSupertypeIndex());
        idsDefsOut.writeInt(classDef.getInterfacesOffset());

        int sourceFileIndex = indexMap.adjustString(classDef.getSourceFileIndex());
        idsDefsOut.writeInt(sourceFileIndex);

        int annotationsOff = classDef.getAnnotationsOffset();
        idsDefsOut.writeInt(indexMap.adjustAnnotationDirectory(annotationsOff));

        int classDataOff = classDef.getClassDataOffset();
        if (classDataOff == 0) {
            idsDefsOut.writeInt(0);
        } else {
            idsDefsOut.writeInt(classDataOut.getPosition());
            ClassData classData = in.readClassData(classDef);
            transformClassData(in, classData, indexMap);
        }

        int staticValuesOff = classDef.getStaticValuesOffset();
        idsDefsOut.writeInt(indexMap.adjustStaticValues(staticValuesOff));
    }

    /**
     * Transform all annotations on a class.
     */
    public void transformAnnotationDirectory(
            Dex.Section directoryIn, IndexMap indexMap) {
        contentsOut.annotationsDirectories.size++;
        annotationsDirectoryOut.assertFourByteAligned();
        indexMap.putAnnotationDirectoryOffset(
                directoryIn.getPosition(), annotationsDirectoryOut.getPosition());

        int classAnnotationsOffset = indexMap.adjustAnnotationSet(directoryIn.readInt());
        annotationsDirectoryOut.writeInt(classAnnotationsOffset);

        int fieldsSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(fieldsSize);

        int methodsSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(methodsSize);

        int parameterListSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(parameterListSize);

        for (int i = 0; i < fieldsSize; i++) {
            // field index
            annotationsDirectoryOut.writeInt(indexMap.adjustField(directoryIn.readInt()));

            // annotations offset
            annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }

        for (int i = 0; i < methodsSize; i++) {
            // method index
            annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));

            // annotation set offset
            annotationsDirectoryOut.writeInt(
                    indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }

        for (int i = 0; i < parameterListSize; i++) {
            // method index
            annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));

            // annotations offset
            annotationsDirectoryOut.writeInt(
                    indexMap.adjustAnnotationSetRefList(directoryIn.readInt()));
        }
    }

    /**
     * Transform all annotations on a single type, member or parameter.
     */
    public void transformAnnotationSet(IndexMap indexMap, Dex.Section setIn) {
        contentsOut.annotationSets.size++;
        annotationSetOut.assertFourByteAligned();
        indexMap.putAnnotationSetOffset(setIn.getPosition(), annotationSetOut.getPosition());

        int size = setIn.readInt();
        annotationSetOut.writeInt(size);

        for (int j = 0; j < size; j++) {
            annotationSetOut.writeInt(indexMap.adjustAnnotation(setIn.readInt()));
        }
    }

    /**
     * Transform all annotation set ref lists.
     */
    public void transformAnnotationSetRefList(IndexMap indexMap, Dex.Section refListIn) {
        contentsOut.annotationSetRefLists.size++;
        annotationSetRefListOut.assertFourByteAligned();
        indexMap.putAnnotationSetRefListOffset(
                refListIn.getPosition(), annotationSetRefListOut.getPosition());

        int parameterCount = refListIn.readInt();
        annotationSetRefListOut.writeInt(parameterCount);
        for (int p = 0; p < parameterCount; p++) {
            annotationSetRefListOut.writeInt(indexMap.adjustAnnotationSet(refListIn.readInt()));
        }
    }

    public void transformClassData(Dex in, ClassData classData, IndexMap indexMap) {
        contentsOut.classDatas.size++;

        ClassData.Field[] staticFields = classData.getStaticFields();
        ClassData.Field[] instanceFields = classData.getInstanceFields();
        ClassData.Method[] directMethods = classData.getDirectMethods();
        ClassData.Method[] virtualMethods = classData.getVirtualMethods();

        classDataOut.writeUleb128(staticFields.length);
        classDataOut.writeUleb128(instanceFields.length);
        classDataOut.writeUleb128(directMethods.length);
        classDataOut.writeUleb128(virtualMethods.length);

        transformFields(indexMap, staticFields);
        transformFields(indexMap, instanceFields);
        transformMethods(in, indexMap, directMethods);
        transformMethods(in, indexMap, virtualMethods);
    }

    public void transformFields(IndexMap indexMap, ClassData.Field[] fields) {
        int lastOutFieldIndex = 0;
        for (ClassData.Field field : fields) {
            int outFieldIndex = indexMap.adjustField(field.getFieldIndex());
            classDataOut.writeUleb128(outFieldIndex - lastOutFieldIndex);
            lastOutFieldIndex = outFieldIndex;
            classDataOut.writeUleb128(field.getAccessFlags());
        }
    }

    public void transformMethods(Dex in, IndexMap indexMap, ClassData.Method[] methods) {
        int lastOutMethodIndex = 0;
        for (ClassData.Method method : methods) {
            int outMethodIndex = indexMap.adjustMethod(method.getMethodIndex());
            classDataOut.writeUleb128(outMethodIndex - lastOutMethodIndex);
            lastOutMethodIndex = outMethodIndex;

            classDataOut.writeUleb128(method.getAccessFlags());

            if (method.getCodeOffset() == 0) {
                classDataOut.writeUleb128(0);
            } else {
                codeOut.alignToFourBytesWithZeroFill();
                classDataOut.writeUleb128(codeOut.getPosition());
                transformCode(in, in.readCode(method), indexMap);
            }
        }
    }

    public void transformCode(Dex in, Code code, IndexMap indexMap) {
        contentsOut.codes.size++;
        codeOut.assertFourByteAligned();

        codeOut.writeUnsignedShort(code.getRegistersSize());
        codeOut.writeUnsignedShort(code.getInsSize());
        codeOut.writeUnsignedShort(code.getOutsSize());

        Code.Try[] tries = code.getTries();
        Code.CatchHandler[] catchHandlers = code.getCatchHandlers();
        codeOut.writeUnsignedShort(tries.length);

        int debugInfoOffset = code.getDebugInfoOffset();
        if (debugInfoOffset != 0) {
            codeOut.writeInt(debugInfoOut.getPosition());
            transformDebugInfoItem(in.open(debugInfoOffset), indexMap);
        } else {
            codeOut.writeInt(0);
        }

        short[] instructions = code.getInstructions();
        InstructionTransformer transformer = (in == dexA)
                ? aInstructionTransformer
                : null;
        transformer.insns = this.insns;
        short[] newInstructions = transformer.transform(instructions);
        codeOut.writeInt(newInstructions.length);
        codeOut.write(newInstructions);

        if (tries.length > 0) {
            if (newInstructions.length % 2 == 1) {
                codeOut.writeShort((short) 0); // padding
            }

            /*
             * We can't write the tries until we've written the catch handlers.
             * Unfortunately they're in the opposite order in the dex file so we
             * need to transform them out-of-order.
             */
            Dex.Section triesSection = dexOut.open(codeOut.getPosition());
            codeOut.skip(tries.length * SizeOf.TRY_ITEM);
            int[] offsets = transformCatchHandlers(indexMap, catchHandlers);
            transformTries(triesSection, tries, offsets);
        }
    }

    /**
     * Writes the catch handlers to {@code codeOut} and returns their indices.
     */
    int[] transformCatchHandlers(IndexMap indexMap, Code.CatchHandler[] catchHandlers) {
        int baseOffset = codeOut.getPosition();
        codeOut.writeUleb128(catchHandlers.length);
        int[] offsets = new int[catchHandlers.length];
        for (int i = 0; i < catchHandlers.length; i++) {
            offsets[i] = codeOut.getPosition() - baseOffset;
            transformEncodedCatchHandler(catchHandlers[i], indexMap);
        }
        return offsets;
    }

    public void transformTries(Dex.Section out, Code.Try[] tries,
            int[] catchHandlerOffsets) {
        for (Code.Try tryItem : tries) {
            out.writeInt(tryItem.getStartAddress());
            out.writeUnsignedShort(tryItem.getInstructionCount());
            out.writeUnsignedShort(catchHandlerOffsets[tryItem.getCatchHandlerIndex()]);
        }
    }

    static final byte DBG_END_SEQUENCE = 0x00;
    static final byte DBG_ADVANCE_PC = 0x01;
    static final byte DBG_ADVANCE_LINE = 0x02;
    static final byte DBG_START_LOCAL = 0x03;
    static final byte DBG_START_LOCAL_EXTENDED = 0x04;
    static final byte DBG_END_LOCAL = 0x05;
    static final byte DBG_RESTART_LOCAL = 0x06;
    static final byte DBG_SET_PROLOGUE_END = 0x07;
    static final byte DBG_SET_EPILOGUE_BEGIN = 0x08;
    static final byte DBG_SET_FILE = 0x09;

    public void transformDebugInfoItem(Dex.Section in, IndexMap indexMap) {
        contentsOut.debugInfos.size++;
        int lineStart = in.readUleb128();
        debugInfoOut.writeUleb128(lineStart);

        int parametersSize = in.readUleb128();
        debugInfoOut.writeUleb128(parametersSize);

        for (int p = 0; p < parametersSize; p++) {
            int parameterName = in.readUleb128p1();
            debugInfoOut.writeUleb128p1(indexMap.adjustString(parameterName));
        }

        int addrDiff;    // uleb128   address delta.
        int lineDiff;    // sleb128   line delta.
        int registerNum; // uleb128   register number.
        int nameIndex;   // uleb128p1 string index.    Needs indexMap adjustment.
        int typeIndex;   // uleb128p1 type index.      Needs indexMap adjustment.
        int sigIndex;    // uleb128p1 string index.    Needs indexMap adjustment.

        while (true) {
            int opcode = in.readByte();
            debugInfoOut.writeByte(opcode);

            switch (opcode) {
            case DBG_END_SEQUENCE:
                return;

            case DBG_ADVANCE_PC:
                addrDiff = in.readUleb128();
                debugInfoOut.writeUleb128(addrDiff);
                break;

            case DBG_ADVANCE_LINE:
                lineDiff = in.readSleb128();
                debugInfoOut.writeSleb128(lineDiff);
                break;

            case DBG_START_LOCAL:
            case DBG_START_LOCAL_EXTENDED:
                registerNum = in.readUleb128();
                debugInfoOut.writeUleb128(registerNum);
                nameIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                typeIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustType(typeIndex));
                if (opcode == DBG_START_LOCAL_EXTENDED) {
                    sigIndex = in.readUleb128p1();
                    debugInfoOut.writeUleb128p1(indexMap.adjustString(sigIndex));
                }
                break;

            case DBG_END_LOCAL:
            case DBG_RESTART_LOCAL:
                registerNum = in.readUleb128();
                debugInfoOut.writeUleb128(registerNum);
                break;

            case DBG_SET_FILE:
                nameIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                break;

            case DBG_SET_PROLOGUE_END:
            case DBG_SET_EPILOGUE_BEGIN:
            default:
                break;
            }
        }
    }

    public void transformEncodedCatchHandler(Code.CatchHandler catchHandler, IndexMap indexMap) {
        int catchAllAddress = catchHandler.getCatchAllAddress();
        int[] typeIndexes = catchHandler.getTypeIndexes();
        int[] addresses = catchHandler.getAddresses();

        if (catchAllAddress != -1) {
            codeOut.writeSleb128(-typeIndexes.length);
        } else {
            codeOut.writeSleb128(typeIndexes.length);
        }

        for (int i = 0; i < typeIndexes.length; i++) {
            codeOut.writeUleb128(indexMap.adjustType(typeIndexes[i]));
            codeOut.writeUleb128(addresses[i]);
        }

        if (catchAllAddress != -1) {
            codeOut.writeUleb128(catchAllAddress);
        }
    }

    public void transformStaticValues(Dex.Section in, IndexMap indexMap) {
        contentsOut.encodedArrays.size++;
        indexMap.putStaticValuesOffset(in.getPosition(), encodedArrayOut.getPosition());
        indexMap.adjustEncodedArray(in.readEncodedArray()).writeTo(encodedArrayOut);
    }


}