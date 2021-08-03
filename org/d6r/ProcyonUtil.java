package org.d6r;

import com.google.common.base.Supplier;
import org.d6r.PosixFileInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import com.strobel.core.SafeCloseable;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ast.AstOptimizer;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Node;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.signatures.MetadataFactory;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.ir.attributes.*;
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.ConstantPool.TypeInfoEntry;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.java.BraceEnforcement;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.JavaLanguage;
import com.strobel.decompiler.languages.java.Wrapping;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.EntityDeclaration;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.transforms.EliminateSyntheticAccessorsTransform;
import com.strobel.decompiler.languages.java.ast.transforms.DeclareVariablesTransform;
import com.strobel.decompiler.languages.java.ast.transforms.AddStandardAnnotationsTransform;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.Statement;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.core.CollectionUtilities;
import org.apache.commons.lang3.tuple.Triple;
import com.android.dex.Dex;
import dalvik.system.VMRuntime;
import org.d6r.PosixFileInputStream;
import libcore.reflect.AnnotationAccess;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.security.CodeSource;
import bsh.NameSpace;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.d6r.LazyMember.of;
import static org.d6r.Reflector.firstNonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.annotation.Doc;
import com.strobel.componentmodel.UserDataStore;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import com.strobel.core.Predicate;
import com.strobel.decompiler.languages.java.ast.transforms.TransformationPipeline;

import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.componentmodel.FrugalKeyMap;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.internal.runners.model.MultipleFailureException;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.decompiler.ast.AstKeys;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.componentmodel.Key;

import com.strobel.assembler.ir.attributes.LineNumberTableAttribute;
import com.strobel.decompiler.languages.java.LineNumberTableConverter;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
  
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.FluentIterable;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflect.setfldval;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import bsh.operators.Extension;
import static org.d6r.ClassInfo.getDex;
import static org.d6r.ClassInfo.getClassResource;
import static org.d6r.ClassInfo.getClassPath;
import static org.d6r.ClassInfo.getBootClassPath;
import static org.d6r.ClassInfo.getFullClassPath;
import com.strobel.decompiler.ast.Variable;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowGraphBuilder;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.StackBehavior;
import com.strobel.assembler.ir.*;

import static com.strobel.core.VerifyArgument.*;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.ParameterReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.ast.JavaResolver;
import com.strobel.decompiler.semantics.ResolveResult;

/**
public class ClasspathTypeLoader
  implements ITypeLoader
{
  private static final java.util.logging.Logger LOG;
  static java.lang.String PATH_SEP;
  static javassist.ClassPool cp;

  public ClasspathTypeLoader();
  public ClasspathTypeLoader(String classPath);
  public static String getBootClassPath();
  public boolean tryLoadType(String internalName, Buffer buffer);
}
*/

public class ProcyonUtil {
  
  static final String  TAG = ProcyonUtil.class.getSimpleName();
  
  static Method GET_INTERNAL_NAME_FROM_CLASSFILE_METHOD;

  public static final Key ANONYMOUS_BASE_TYPE_REFERENCE
    = Keys.ANONYMOUS_BASE_TYPE_REFERENCE;
  public static final Key AST_BUILDER = Keys.AST_BUILDER;
  public static final Key CONSTANT_VALUE = Keys.CONSTANT_VALUE;
  public static final Key DYNAMIC_CALL_SITE = Keys.DYNAMIC_CALL_SITE;
  public static final Key<FieldDefinition> FIELD_DEFINITION = Keys.FIELD_DEFINITION;
  public static final Key<MemberReference> MEMBER_REFERENCE = Keys.MEMBER_REFERENCE;
  public static final Key<MethodDefinition> METHOD_DEFINITION = Keys.METHOD_DEFINITION;
  public static final Key PACKAGE_REFERENCE = Keys.PACKAGE_REFERENCE;
  public static final Key<ParameterDefinition> PARAMETER_DEFINITION = Keys.PARAMETER_DEFINITION;
  public static final Key PARENT_LAMBDA_BINDING
    = AstKeys.PARENT_LAMBDA_BINDING;
  public static final Key SWITCH_INFO = AstKeys.SWITCH_INFO;
  public static final Key TYPE_ARGUMENTS = AstKeys.TYPE_ARGUMENTS;
  public static final Key<TypeDefinition> TYPE_DEFINITION = Keys.TYPE_DEFINITION;
  public static final Key<TypeReference> TYPE_REFERENCE = Keys.TYPE_REFERENCE;
  public static final Key<Variable> VARIABLE = Keys.VARIABLE;
  public static final Key<VariableDefinition> VARIABLE_DEFINITION = Keys.VARIABLE_DEFINITION;

  public static final Key[] ALL_KEYS = {
    ANONYMOUS_BASE_TYPE_REFERENCE,
    AST_BUILDER,
    CONSTANT_VALUE,
    DYNAMIC_CALL_SITE,
    FIELD_DEFINITION,
    MEMBER_REFERENCE,
    METHOD_DEFINITION,
    PACKAGE_REFERENCE,
    PARAMETER_DEFINITION,
    PARENT_LAMBDA_BINDING,
    SWITCH_INFO,
    TYPE_ARGUMENTS,
    TYPE_DEFINITION,
    TYPE_REFERENCE,
    VARIABLE,
    VARIABLE_DEFINITION
  };
  
  // === Maintained by global getters/setters ===
  static ITypeLoader typeLoader;
  static IMetadataResolver metadataResolver;
  static DecompilerSettings decompilerSettings;
  static JavaFormattingOptions javaFormattingOptions;
  static DecompilationOptions _options;
  
  // === Maintained by custom AstBuilder ===
  static boolean hadVerifyError; 
  static AstBuilder g_astBuilder;
  static DecompilerContext g_ctx;
  static BlockStatement g_blockStatement;
  static TypeDefinition g_type;
  static MethodDefinition g_method;
  static Iterable<ParameterDeclaration> g_params;
  static Throwable g_exception;
  static ITypeLoader g_typeLoader;
  
  
  static Method _createConstructor;
  static Method _createField;
  static Method _createMethod;
  static Method _createMethodBody;
  
  static boolean DO_FULL_DECOMPILATION = Boolean.parseBoolean(
    System.getProperty("do.full.decompilation", "true")
  );
  static boolean NO_FIX_MEMBERS = Boolean.getBoolean("no.fix.members");
  static boolean NO_IMPROVE_SUBPAR = Boolean.getBoolean("noimprove.subpar");
  static boolean REUSE_CONTEXT = Boolean.parseBoolean(
    System.getProperty("reuse.context", "true")
  );
  
  
  public static boolean USE_DEFAULT_CLASSPATH = true;
  public static String INDENT_TOKEN = "  ";
  public static int DEFAULT_WRITER_CAPACITY = 4096;
  
  static List<? extends Class<IAstTransform>> transformerClasses;
  static final List<IAstTransform> xforms = new ArrayList<>();
  static LazyMember<Method> HASH_PRIMITIVE_NAME
    = of(MetadataSystem.class, "hashPrimitiveName", String.class);
  static LazyMember<Method> LOAD_CLASS
    = of(ClassLoader.class, "loadClass", String.class, Boolean.TYPE);
  static LazyMember<Field> PRIMITIVE_TYPES_BY_DESCRIPTOR
    = of(MetadataSystem.class, "PRIMITIVE_TYPES_BY_DESCRIPTOR");
  static LazyMember<Constructor<?>> METHOD_DEFINITION_CTOR
    = of(MethodDefinition.class, "<init>", new Class<?>[0]);
  static LazyMember<Method> ASTBUILDER_GET_CONTEXT
    = of(AstBuilder.class, "getContext", new Class<?>[0]);
  static LazyMember<Method> LANG_CREATE_ASTBUILDER
    = of(JavaLanguage.class, "createAstBuilder", DecompilationOptions.class,
         TypeDefinition.class, Boolean.TYPE);
  static LazyMember<Method> LANG_RUN_TRANSFORMS
    = of(JavaLanguage.class, "runTransforms", AstBuilder.class, 
          DecompilationOptions.class, IAstTransform.class);
  
  static LazyMember<Method> CREATE_TYPE_PARAMETERS = LazyMember.of(
    AstBuilder.class, "createTypeParameters", List.class);
  static LazyMember<Method> ENTITYDECL_SET_MODIFIERS = LazyMember.of(
    EntityDeclaration.class, "setModifiers", AstNode.class, Collection.class);
  static LazyMember<Constructor<AstMethodBodyBuilder>> ASTMETHODBODYBUILDER_CTOR =
    LazyMember.of(AstMethodBodyBuilder.class, "<init>",
      AstBuilder.class, MethodDefinition.class, DecompilerContext.class);
  static final LazyMember<Method> CFR_READ_CLASS = of(
    ClassFileReader.class, "readClass", new Class<?>[0]);
  static final LazyMember<Constructor<ClassFileReader>> CFR_CTOR = of(
    ClassFileReader.class, "<init>", Integer.TYPE, IMetadataResolver.class,
    Integer.TYPE, Integer.TYPE, Buffer.class, ConstantPool.class, Integer.TYPE,
    TypeInfoEntry.class, TypeInfoEntry.class, TypeInfoEntry[].class
  );

  static String CPKEY = "com.strobel.assembler.ir.ConstantPool$Key";
  static LazyMember<Field> CP_TAG = of("_tag", CPKEY);
  static LazyMember<Field> CP_REF_INDEX1 = of("_refIndex1", CPKEY);
  static LazyMember<Field> CP_REF_INDEX2 = of("_refIndex2", CPKEY);
  static LazyMember<Field> CP_STRING_VAL1 = of("_stringValue1", CPKEY);
  static LazyMember<Field> CP_STRING_VAL2 = of("_stringValue2", CPKEY);
  static LazyMember<Field> CP_INT_VAL = of("_intValue", CPKEY);
  static LazyMember<Field> CP_LONG_VAL = of("_longValue", CPKEY);
  //static Map<TypeDefinition,DecompilerContext> contexts = new IdentityHashMap<>();
  static LazyMember<Field> MDS_INSTANCE = of("_instance", MetadataSystem.class);
  static LazyMember<Field> MDS_TLDR = of("_typeLoader", MetadataSystem.class);
  
  public static final Map<Integer, Pair<String,Integer>> CONST_POOL_TAGS =
    RealArrayMap.toMap(Arrays.<Pair<Integer, Pair<String,Integer>>>asList(
      Pair.of(7,  Pair.of("Class", 2)),
      Pair.of(6,  Pair.of("Double", 8)),
      Pair.of(9,  Pair.of("Fieldref", 4)),
      Pair.of(4,  Pair.of("Float", 4)),
      Pair.of(3,  Pair.of("Integer", 4)),
      Pair.of(11, Pair.of("InterfaceMethodref", 4)),
      Pair.of(5,  Pair.of("Long", 8)),
      Pair.of(15, Pair.of("MethodHandle", 4)),
      Pair.of(10, Pair.of("Methodref", 4)),
      Pair.of(12, Pair.of("NameAndType", 4)),
      Pair.of(8,  Pair.of("String", 2)),
      Pair.of(1,  Pair.of("Utf8", 2))
    ));
    
  
  static boolean _initialized;
  static void ensureInit() {
      if (_initialized) return;
      _initialized = true;
    final IMetadataResolver resolver = getMetadataResolver();
    if (resolver instanceof MetadataSystem) {
      MDS_INSTANCE.setValue(null, resolver);
      Reflect.setfldval(PosixUtil.class, "$assertionsDisabled", Boolean.FALSE);
      if (MetadataSystem.instance() != resolver) throw new AssertionError(
        "MetadataSystem.instance() != ProcyonUtil.metadataResolver after set"
      );
      MDS_TLDR.setValue(resolver, getTypeLoader());
    }
  }
  
  
  public static ITypeLoader getTypeLoader() {
    if (typeLoader == null) {
      ensureInit();
      typeLoader = (USE_DEFAULT_CLASSPATH)
        ? new ClasspathTypeLoader()
        : new ClasspathTypeLoader(getFullClassPath());
    }
    return typeLoader;
  }

  public static IMetadataResolver getMetadataResolver() {
    if (!_initialized) ensureInit();
    if (metadataResolver == null) {
          final ITypeLoader tl = getTypeLoader();
          Reflect.setfldval(
            MetadataSystem.class, "_instance",
            (metadataResolver = new MetadataSystem(tl))
          );
          return metadataResolver;
    } else {
        return metadataResolver;
    }
  }
  
  static boolean isAcceptableResolver(final IMetadataResolver resolver,
    final boolean isFullResolverRequired)
  {
    if (resolver == null) return false;
    if (!isFullResolverRequired) return true;
    final TypeReference test = resolver.lookupType("java/lang/Object");
    if (test == null) return false;
    if (test instanceof TypeDefinition) return true;
    final TypeDefinition testDef = test.resolve();
    if (testDef != null) return true;
    if (resolver.resolve(test) != null) return true;
    return false;
  }
  
  public static class TwoResolversInOne implements IMetadataResolver {
    private final IMetadataResolver _first;
    private final IMetadataResolver _second;
    
    public TwoResolversInOne(final IMetadataResolver first,
      final IMetadataResolver second)
    {
      _first = first;
      _second = second;
    }
    public TwoResolversInOne(final IMetadataResolver first) {
      this(first, getMetadataResolver());
    }
    @Override
    public TypeReference lookupType(final String desc) {
      final TypeReference result = _first.lookupType(desc);
      if (result != null) return result;
      return _second.lookupType(desc);
    }
    @Override
    public void popFrame() {
      _first.popFrame();
    }
    @Override
    public void pushFrame(final IResolverFrame frame) {
      _first.pushFrame(frame);
    }
    @Override
    public FieldDefinition resolve(final FieldReference member) {
      final FieldDefinition result = _first.resolve(member);
      if (result != null) return result;
      return _second.resolve(member);
    }
    @Override
    public MethodDefinition resolve(final MethodReference member) {
      final MethodDefinition result = _first.resolve(member);
      if (result != null) return result;
      return _second.resolve(member);
    }
    @Override
    public TypeDefinition resolve(final TypeReference type) {
      final TypeDefinition result = _first.resolve(type);
      if (result != null) return result;
      return _second.resolve(type);
    }
  }
  
  public static IMetadataResolver getResolver(final IMetadataTypeMember mb)
  {
    final IMetadataResolver resolver = getResolver(mb, false);
    if (isAcceptableResolver(resolver, true)) return resolver;
    return (resolver != null)
      ? new TwoResolversInOne(resolver)
      : getMetadataResolver();
  }
  
  public static IMetadataResolver getResolver(IMetadataTypeMember mb,
    final boolean requireFullResolver)
  {
    try {
      TypeDefinition td = (mb instanceof TypeDefinition)
        ? (TypeDefinition) mb
        : null;
      
      if (td != null) return td.getResolver();
      if (mb instanceof MethodReference) {
        if (((MethodReference)mb).resolve() != null) {
          return
           ((MethodReference)mb).resolve().getDeclaringType().getResolver();
        }
      }
      if (mb instanceof FieldReference) {
        if (((FieldReference)mb).resolve() != null) {
          return
            ((FieldReference)mb).resolve().getDeclaringType().getResolver();
        }
      }
      for (TypeReference declaringType = mb.getDeclaringType();
           declaringType != null ;
           declaringType = declaringType.getDeclaringType())
      {
        if (declaringType instanceof TypeDefinition) {
          if (MetadataHelper.isSameType(
              declaringType, BuiltinTypes.Object)) {
            break;
          }
          return ((TypeDefinition) declaringType).getResolver();
        }
        if (declaringType != null) {
          final TypeDefinition dtDef = declaringType.resolve();
          if (dtDef != null) {
            IMetadataResolver dtResolver = null;
            if (mb instanceof TypeReference) {
              final TypeDefinition r = dtDef.resolve((TypeReference)mb);
              if (r != null) dtResolver = r.getResolver();
            }
            if (mb instanceof FieldReference) {
              final FieldDefinition r = dtDef.resolve((FieldReference)mb);
              if (r != null) dtResolver
                = r.getDeclaringType().getResolver();
            }
            if (mb instanceof MethodReference) {
              final MethodDefinition r = dtDef.resolve((MethodReference)mb);
              if (r != null) dtResolver
                = r.getDeclaringType().getResolver();
            }
            if (dtResolver != null &&
                dtResolver != IMetadataResolver.EMPTY)
            {
              if (isAcceptableResolver(dtResolver, requireFullResolver))
                return dtResolver;
            }
          }
        }
      }
      
      final Object parentObject = Reflect.getfldval(mb, "this$0");
      if (parentObject instanceof CoreMetadataFactory) {
        final CoreMetadataFactory mf = (CoreMetadataFactory) parentObject;
        final TypeDefinition ownerType = Reflect.getfldval(mf, "_owner");
        if (ownerType != null) {
          final IMetadataResolver ownResolver = ownerType.getResolver();
          if (ownResolver != null &&
              ownResolver != IMetadataResolver.EMPTY)
          {
            if (isAcceptableResolver(ownResolver, requireFullResolver))
              return ownResolver;
          }
        }
      }
      final IMetadataResolver staticResolver
        = ProcyonUtil.getMetadataResolver();
      final IMetadataResolver objectResolver
        = BuiltinTypes.Object.getResolver();
      
      if (staticResolver instanceof MetadataSystem) {
        return staticResolver;
      }
      return objectResolver;
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
  }

  public static String getClassNameAsPath(String className) {
    if (className.charAt(0) == 'L'
    &&  className.charAt(className.length() - 1) == ':') {
      className = new StringBuilder(className)
        .delete(className.length() - 1, className.length())
        .delete(0, 1)
        .toString();
    }
    if (className.indexOf('.') != -1) {
      className = className.replace('.', '/');
    }
    return className;
  }

  public static boolean addTypeDefinition(TypeDefinition typeDef) {
    IMetadataResolver mdr = getMetadataResolver();
    if (mdr instanceof MetadataSystem) {
      ((MetadataSystem) mdr).addTypeDefinition(typeDef);
      return true;
    }
    return false;
  }

  public static TypeDefinition getTypeDefinition(String className) {
    final IMetadataResolver mdr = getMetadataResolver();
    String classNameAsPath = getClassNameAsPath(className);
    TypeReference typeRef = getMetadataResolver().lookupType(classNameAsPath);
    ClassInfo.generateTypes = true;
    if (typeRef == null) return getTypeDefinition(
      ClassInfo.generateClass(
        className, "java.lang.Object", new String[0], false
      )
    );
    TypeDefinition typeDef = typeRef.resolve();
    return typeDef;
  }
  
  @Doc("TypeDefinition returned will be isolated in a new MetadataSystem.")
  public static TypeDefinition getTypeDefinition0(final byte[] classBytes) {
    Buffer classBuffer = new Buffer(classBytes);
    String classNameAsPath = getInternalNameFromClassFile(classBuffer);
    ITypeLoader typeLoader = new ArrayTypeLoader(classBytes);
    IMetadataResolver mds = new MetadataSystem(typeLoader);
    TypeReference typeRef = mds.lookupType(classNameAsPath);
    TypeDefinition typeDef = typeRef.resolve();
    return typeDef;
  }
  
  @Doc("TypeDefinition returned will use current (default) MetadataSystem.")
  public static TypeDefinition getTypeDefinition(final byte[] classBytes) {
    final IMetadataResolver mdr = getMetadataResolver();
    final ClassFileReader cfr = getClassFileReader(classBytes);
    final TypeDefinition td = CFR_READ_CLASS.invoke(cfr);
    return td;
  }
  
  @Doc("TypeDefinition returned will use IMetadataResolver from classFileReader.")
  public static TypeDefinition getTypeDefinition(
    final ClassFileReader classFileReader)
  {
    final IMetadataResolver mdr = getMetadataResolver();
    return CFR_READ_CLASS.invoke(classFileReader);
  }
  
  
  public static class ClassFileInfo {
    
    public IMetadataResolver resolver;
    public int majorVer;
    public int minorVer;
    public Buffer buffer;
    public ConstantPool cp;
    public int accessFlags;
    public TypeInfoEntry thisClassEntry;
    public TypeInfoEntry baseClassEntry;
    public TypeInfoEntry[] interfaceEntries;
    
    public final int magic;
    public final int thisCpIndex;
    public final int baseCpIndex;
    public final int interfaceCount;
    public final int cpBegOff;
    public final int cpEndOff;
    public final String thisClassName;
    public final String baseClassName;
    
    public ClassFileInfo(final Buffer buffer) {
         this.buffer = buffer;
               magic = buffer.readInt();
            minorVer = buffer.readUnsignedShort();
            majorVer = buffer.readUnsignedShort();
            cpBegOff = buffer.position();
                  cp = ConstantPool.read(buffer);
            cpEndOff = buffer.position();
         accessFlags = buffer.readUnsignedShort();
         thisCpIndex = buffer.readUnsignedShort();
         baseCpIndex = buffer.readUnsignedShort();
      interfaceCount = buffer.readUnsignedShort();
      
      interfaceEntries = new TypeInfoEntry[interfaceCount];
      for (int i=0; i<interfaceCount; ++i) {
        final int interfaceEntryIndex = buffer.readUnsignedShort();
        interfaceEntries[i] = (TypeInfoEntry) cp.get(interfaceEntryIndex);
      }
      thisClassEntry = (TypeInfoEntry) cp.get(thisCpIndex);
      baseClassEntry = (TypeInfoEntry) cp.get(baseCpIndex);
      // "pkg.a.b.AClass" format
      thisClassName = ClassInfo.typeToName(thisClassEntry.getName());
      baseClassName = ClassInfo.typeToName(baseClassEntry.getName());
    }
    
    @Override
    public String toString() {
      return String.format(
        "<%s for [%s]: (%s)>",
        getClass().getSimpleName(), thisClassName, baseClassName
      );
    }
  }
  
  
  public static ClassFileReader getClassFileReader(final byte[] classBytes,
    final IMetadataResolver resolver, final boolean useRealClassFileVersion)
  {
    final Buffer buffer = new Buffer(classBytes);
    final ClassFileInfo cfi = new ClassFileInfo(buffer);
    final ClassFileReader classFileReader = CFR_CTOR.newInstance(
      ClassFileReader.OPTION_PROCESS_CODE |
      ClassFileReader.OPTION_PROCESS_ANNOTATIONS, // int options
      firstNonNull(resolver, getMetadataResolver()), // IMetadataResolver resolver
      (useRealClassFileVersion)? cfi.majorVer: 52, // int majorVersion
      (useRealClassFileVersion)? cfi.minorVer: 0, // int minorVersion
      cfi.buffer, // Buffer buffer
      cfi.cp, // ConstantPool constantPool
      cfi.accessFlags, // int accessFlags
      cfi.thisClassEntry, // TypeInfoEntry thisClassEntry
      cfi.baseClassEntry, // TypeInfoEntry baseClassEntry
      cfi.interfaceEntries // TypeInfoEntry[] interfaceEntries
    );
    return classFileReader;
  }
  
  public static ClassFileReader getClassFileReader(final byte[] classBytes) {
    return getClassFileReader(classBytes, (IMetadataResolver) null, false);
  }
  
  public static TypeDefinition getTypeDefinition(Class<?> cls) {
    return getTypeDefinition(cls.getName());
  }

  public static TypeDefinition getTypeDefinition(Object ci) {
    return getTypeDefinition(typeof(ci));
  }
  
  public static JavaFormattingOptions getJavaFormattingOptions() {
    if (javaFormattingOptions == null) {
      JavaFormattingOptions fmtOpts = JavaFormattingOptions.createDefault();
      initFormattingOptions(fmtOpts);
      javaFormattingOptions = fmtOpts;
    }
    return javaFormattingOptions;
  }

  public static DecompilerSettings getDecompilerSettings() {
    if (decompilerSettings == null) {
      decompilerSettings = new DecompilerSettings();
      JavaFormattingOptions fmtOpts = getJavaFormattingOptions();
      decompilerSettings.setJavaFormattingOptions(fmtOpts);
      decompilerSettings.setSimplifyMemberReferences(true);
      decompilerSettings.setForceExplicitImports(true);
      decompilerSettings.setIncludeErrorDiagnostics(true);
      decompilerSettings.setIncludeLineNumbersInBytecode(true);
      decompilerSettings.setMergeVariables(true);
      decompilerSettings.setRetainRedundantCasts(false);
      decompilerSettings.setTypeLoader(getTypeLoader());
      
      decompilerSettings.setAlwaysGenerateExceptionVariableForCatchBlocks(false);
      decompilerSettings.setFlattenSwitchBlocks(true);
    }
    return decompilerSettings;
  }



  public static DecompilationOptions getDecompilationOptions() {
    if (_options != null) return _options;
    final DecompilationOptions options = new DecompilationOptions();
    try {
      options.setSettings(getDecompilerSettings());
      options.setFullDecompilation(DO_FULL_DECOMPILATION);
      return options;
    } finally {
      _options = options;
    }
  }
  
  public static JavaLanguage getLanguage() {
    return (JavaLanguage) getDecompilerSettings().getLanguage();
  }

  static <W extends Writer> W tryGetWriter(ITextOutput textOutput,
  boolean doThrow)
  {
    Object[] result = Reflect.findField(textOutput, Writer.class);
    if (result == null || result.length < 2) {
      if (doThrow) throw new RuntimeException(String.format(
        "Cannot obtain Writer from ITextOutput of type %s:\n%s",
        Dumper.dumpStr(textOutput, 2, 10)
      ));
      return null;
    }
    Writer writer = (Writer) result[1];
    return (W) writer;
  }

  public static boolean tryFlush(ITextOutput textOutput) {
    Writer writer = tryGetWriter(textOutput, false);
    if (writer == null) return false;
    Method flushMethod = Reflect.findMethod(writer, "flush", new Class[0]);
    if (flushMethod == null) return false;
    try {
      flushMethod.setAccessible(true);
      flushMethod.invoke(writer);
      return true;
    } catch (ReflectiveOperationException ex) {
      ex.printStackTrace();
      return false;
    }
  }

  public static PlainTextOutput newPlainTextOutput() {
    StringWriter sw = new StringWriter(DEFAULT_WRITER_CAPACITY);
    PlainTextOutput textOutput = new PlainTextOutput(sw);
    textOutput.setIndentToken(INDENT_TOKEN);
    textOutput.setUnicodeOutputEnabled(false);
    return textOutput;
  }

  public static String toString(ITextOutput textOutput) {
    try {
      StringWriter sw = tryGetWriter(textOutput, true);
      String src = sw.getBuffer().toString();
      return src;
    } catch (ClassCastException cce) {
      throw new UnsupportedOperationException(String.format(
        "Cannot obtain output from ITextOutput of type %s: "
        + "Underlying writer is not a StringWriter! (actual type = %s)",
        textOutput.getClass().getName(),
        typeof(tryGetWriter(textOutput, false)).getClass()
          .getName().concat(".class")
      ), cce);
    }
  }
  
  static final Set<TypeDefinition> processed = new IdentityHashSet<>();
  

  public static DecompilationResult decompile(String... classNames) {
    return decompile(getMetadataResolver(), classNames);
  }

  public static DecompilationResult decompile(TypeDefinition typeDef) {
    if (typeDef == null) return null;
    IMetadataResolver mdr = typeDef.getResolver();
    String className = typeDef.getFullName();
    return decompile(mdr, className);
  }

  public static DecompilationResult decompile(IMetadataResolver mdr,
  String... classNames)
  {
     Language lang = getLanguage();
     DecompilationOptions options = getDecompilationOptions();
     DecompilationResult result = new DecompilationResult();

     StringBuilder sepB = new StringBuilder(77);
     while (sepB.length() < 67) sepB.append("__________");
     String sep = sepB.append('\n').toString();

     final File outDir
       = PosixFileInputStream.createTemporaryDirectory("tmp_procyon_dc_output");
     
     boolean hadFileWriteFailure = false;
     
     for (String className: classNames) {
       try {
         TypeDefinition typeDef = getTypeDefinition(className);
         if (typeDef == null) {
           System.err.printf("Skipping failed class: %s\n", className);
           continue;
         }
         System.err.printf(
           "Decompiling %s: %s\n",
           typeDef.getClass().getSimpleName(), typeDef
         );
         try {
           if (processed.add(typeDef)) {
             DeobfuscationUtilities.processType(typeDef);
           }
         } catch (Exception e) {
           new RuntimeException(String.format(
             "DeobfuscationUtilities.processType(typeDef = %s) failed",
             typeDef
           )).printStackTrace();
         }
         System.err.printf("Decompiling: %s\n", typeDef);
         ITextOutput textOutput = newPlainTextOutput();
         lang.decompileType(typeDef, textOutput, options);
         tryFlush(textOutput);

         System.err.println("Collecting decompiler output ...\n");
         String src = toString(textOutput);
         result.put(typeDef, src);
         if (!hadFileWriteFailure) {
           File destFile
             = new File(outDir, ClassInfo.classNameToPath(className, "java"));
           File destDir = destFile.getParentFile();
           boolean dirOk = (destDir.exists() && destDir.isDirectory());
           try {
             if (!dirOk && !(dirOk = destDir.mkdirs())) {
               throw (Exception) (Object)
                  Reflector.invokeOrDefault(destDir, "mkdirErrno");
             }
             destFile.createNewFile();
             FileUtils.writeStringToFile(destFile, src, StandardCharsets.UTF_8);
             System.err.printf(
               "Wrote to: %s  [%d bytes]\n", destFile.getPath(), 
               new File(destFile.getPath()).length()
             );
           } catch (Exception e) {           
             e.printStackTrace();
           } finally {
             if (!destFile.exists()) hadFileWriteFailure = true;
           }
         }
         System.err.printf("Output length: %d chars\n", src.length());
         
         System.err.printf("Finished decompiling %s\n", className);
       } catch (Exception ex) {
         new RuntimeException(
           String.format(
             "*** Failed to decompile class: %s: %s ***\n",
             className, ex.getClass().getSimpleName()
           ), ex
         ).printStackTrace();
       } finally {
         // System.err.println(sep);
       }
     }
     return result;
  }



  public static ConstructorDeclaration createConstructor(
  AstBuilder astBuilder, MethodDefinition method)
  {
    try {
      if (_createConstructor == null) {
        (_createConstructor = AstBuilder.class.getDeclaredMethod(
          "createConstructor", MethodDefinition.class
        )).setAccessible(true);
      }
      return (ConstructorDeclaration)
        _createConstructor.invoke(astBuilder, method);
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }

  public static FieldDeclaration createField(AstBuilder astBuilder,
  FieldDefinition field)
  {
    try {
      if (_createField == null) {
        (_createField = AstBuilder.class.getDeclaredMethod(
          "createField", FieldDefinition.class
        )).setAccessible(true);
      }
      return (FieldDeclaration)
        _createField.invoke(astBuilder, field);
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }

  
  public static final Map<Throwable, Object> errorState = new IdentityHashMap<>();
  
  public static MethodDeclaration createMethod(AstBuilder astBuilder,
  MethodDefinition method)
  {
    try {
      if (_createMethod == null) {
        (_createMethod = AstBuilder.class.getDeclaredMethod(
          "createMethod", MethodDefinition.class
        )).setAccessible(true);
      }
      return (MethodDeclaration)
        _createMethod.invoke(astBuilder, method);
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }

  public static BlockStatement createMethodBody(AstBuilder astBuilder,
  MethodDefinition method, Iterable<ParameterDeclaration> parameters)
  {
    try {
      if (_createMethodBody == null) {
        (_createMethodBody = AstBuilder.class.getDeclaredMethod(
          "createMethodBody", MethodDefinition.class, Iterable.class
        )).setAccessible(true);
      }
      return (BlockStatement)
        _createMethodBody.invoke(astBuilder, method, parameters);
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  static DecompilerContext decompilerContext;
  static AstBuilder astBuilder;

  public static DecompilerContext getDecompilerContext() {
    if (!contextStack.isEmpty()) {
      return contextStack.peek();
    }
    
    if (decompilerContext == null) {
      decompilerContext = new DecompilerContext(getDecompilerSettings());
    }
    return decompilerContext;
  }
  
  static Stack<DecompilerContext> contextStack = new Stack<>();
  
  public static class ContextSupplier 
    implements Supplier<DecompilerContext>, SafeCloseable
  {
    final DecompilerContext outerContext;
    final DecompilerContext innerContext;
    
    public ContextSupplier(DecompilerSettings settings) {
      this.outerContext = getDecompilerContext();
      this.innerContext = new DecompilerContext(settings);
      contextStack.push(this.innerContext);
    }
    
    @Override
    public void close() {
      DecompilerContext popped = contextStack.pop();
      if (popped != this.innerContext) {
        throw new Error(String.format(
          "Popped context (%s: %s) is not the one pushed (%s: %s)",
          popped, Debug.ToString(popped),
          this.innerContext, Debug.ToString(this.innerContext)
        ));
      }
    }
    
    @Override
    public DecompilerContext get() {
      return this.innerContext;
    }
    
    public DecompilerContext getOuter() {
      return this.outerContext;
    }
  }
  
  public static ContextSupplier getInnerContextSupplier(
    final DecompilerSettings settingsForChildContext)
  {
    if (decompilerContext == null) {
      decompilerContext = new DecompilerContext(getDecompilerSettings());
    }
    return new ContextSupplier(settingsForChildContext);
  }
  
  
  public static <M extends EntityDeclaration>
  M decompileToAst(Member member) {
    return decompileToAst(getMethodDefinition(member));
  }

  public static <M extends EntityDeclaration>
  M decompileToAst(MethodReference methodRef) {
    return decompileToAst(methodRef, true);
  }

  public static <M extends EntityDeclaration>
  M decompileToAst(MethodReference methodRef, boolean optimize)
  {
    return decompileToAst(methodRef, optimize, true);
  }
  
  public static <M extends EntityDeclaration>
  M decompileToAst(MethodReference methodRef, boolean optimize, boolean allowFix)
  {
    return decompileToAst(methodRef, optimize, allowFix, true);
  }
  
  public static <M extends EntityDeclaration>
  M decompileToAst(MethodReference methodRef, boolean optimize, boolean allowFix,
    boolean allowTransform)
  {
    MethodDefinition md;
    IMetadataResolver mdr;
    if (methodRef instanceof MethodDefinition) {
      md = (MethodDefinition) methodRef;
    } else {
      md = methodRef.resolve();
      if (md == null) {
        if (methodRef.getDeclaringType() instanceof TypeDefinition) {
          md = ((TypeDefinition) methodRef.getDeclaringType()
                ).getResolver().resolve(methodRef);
        }
      }
      if (md == null) {
        mdr = getResolver(methodRef);
        if (mdr != null) {
          md = mdr.resolve(methodRef);
        }
      }
      if (md == null) md = MetadataSystem.instance().resolve(methodRef);
      if (md == null) {
        throw new AssertionError(TextUtil.str(methodRef));
      }
    }
    
    mdr = getResolver(md);
    if (mdr == null) mdr = MetadataSystem.instance();
    
    TypeDefinition td = md.getDeclaringType().resolve();
    AstBuilder astb = createAstBuilder(
      getDecompilationOptions(), td, true
    );
    DecompilerContext ctx = null;
    if (astb != null) ctx = ASTBUILDER_GET_CONTEXT.invoke(astb);
    if (ctx == null) ctx = getDecompilerContext();
    Keys.AST_BUILDER.set(ctx, astb);
    
    // ctx.setCurrentType(td);
    ctx.setCurrentMethod(md);
    astb.setDecompileMethodBodies(true);
    try {
      DeobfuscationUtilities.processType(td);
    } catch (Exception ex) {
      new RuntimeException(String.format(
        "An exception [%s] was thrown during a call to "
        + "DeobfuscationUtilities.processType(td = %s) "
        + "while attempting to decompile the %s %s to an AST: %s",
        ex.getClass().getSimpleName(), td,
        methodRef.getClass().getSimpleName(), methodRef, ex
      ), ex).printStackTrace();
    }
    
    Boolean origHaveTransformationsRun = (allowTransform)
      ? null
      : Reflect.getfldval(astb, "_haveTransformationsRun");
    if (!allowTransform && Boolean.FALSE.equals(origHaveTransformationsRun)) {
      Reflect.setfldval(astb, "_haveTransformationsRun", Boolean.TRUE);
    }
    try {
      M member = (M) ((methodRef.getName().equals("<init>"))
       ? (Object) createConstructor(astb, md)
       : (Object) createMethod(astb, md));
      if (allowFix && !NO_FIX_MEMBERS) {
        if (!hasError(member)) return member;
        return (M) fixMember(member);
      } else {
        return member;
      }
    } finally {
      if (origHaveTransformationsRun != null) {
        Reflect.setfldval(
          astb, "_haveTransformationsRun", origHaveTransformationsRun
        );
      }
    }
  }
  
  public static boolean isMethodLike(final AstNode node) {
    return ! node.getChildByRole(Roles.BODY).isNull();
  }
  
  
  public static CompilationUnit decompileToAst(TypeReference typeRef) {
    return decompileToAst(typeRef, true, true, true);
  }
  
  public static CompilationUnit decompileToAst(TypeReference typeRef, 
    boolean optimize, boolean transform, boolean allowFix)
  {
    return decompileToAst(typeRef, optimize, transform, allowFix, true);
  }
  
  public static CompilationUnit decompileToAst(TypeReference typeRef, 
    boolean optimize, boolean transform, boolean allowFix, boolean allowTransform)
  {
    if (typeRef == null) {
      throw new IllegalArgumentException(String.format(
        "decompileToAst(TypeReference typeRef: %s boolean optimize: %s, " +
        "boolean transform: %s, boolean allowFix: %s, " +
        "boolean allowTransform: %s): typeRef == null",
        typeRef, optimize, transform, allowFix, allowTransform
      ));
    }
    if  (typeRef.getDeclaringType() == null && allowFix) {
      counts.clear();
    }
    
    DecompilerContext ctx = getDecompilerContext();
    // new DecompilerContext(getDecompilerSettings());
    AstBuilder astb = Keys.AST_BUILDER.get(ctx);
    if (astb == null) {
      astb = new AstBuilder(ctx);
      Keys.AST_BUILDER.set(ctx, astb);
    }
    
    TypeDefinition td = null;
    if (typeRef instanceof TypeDefinition) {
      td = (TypeDefinition) typeRef;
    } else {
      IMetadataResolver mdr = getResolver(typeRef);
      td = mdr.resolve(typeRef);
    }
    
    final TypeDefinition oldCurrentType = ctx.getCurrentType();
    final MethodDefinition oldCurrentMethod = ctx.getCurrentMethod();
    if (td != null) ctx.setCurrentType(td);
    boolean oldDecompileMethodBodies = astb.getDecompileMethodBodies();
    try {
    // ctx.setCurrentMethod(md);
      astb.setDecompileMethodBodies(true);
      boolean transformed = false;
      if (optimize) {
        if (td != null) {
          try {
            DeobfuscationUtilities.processType(td);
            transformed = true;
          } catch (IllegalArgumentException iae) {
            transformed = false;
          }
        }
      }
      CompilationUnit cu = decompileTypeToAst(
        td, // TypeDefinition type
        getDecompilationOptions(), // DecompilationOptions options
        allowTransform // boolean allowTransform
      );
      
      final Set<EntityDeclaration> added = new IdentityHashSet<>();
      
      while (allowFix && !NO_FIX_MEMBERS) {
        List<EntityDeclaration> broken = findMembersWithError(cu);
        int size = broken.size(), lastSize = -1;
        if (size == 0) break;
        
        boolean wasFixed = false;
        List<EntityDeclaration> fixed = new ArrayList<>();
        int attempts = 0;
        do {
          ++attempts;
          wasFixed = true;
          lastSize = size;
          Log.d(TAG, "Fixing... # broken: %d\n", size);
          // astb.setDecompileMethodBodies(false);
          fixed.addAll(findMembersWithError(cu));
          Collections.reverse(broken);
          fixMembers(broken);
        } while (attempts < 10 &&
                ((size = (broken = findMembersWithError(cu)).size()) > 0) &&
                lastSize != size);
         
        while  (((size = (broken = findMembersWithError(cu)).size()) > 0) &&
               ++attempts < 20)
        {
          Log.d(TAG, "Fallback fixes ... %d methods need repair", size);
          for (Iterator<EntityDeclaration> it = broken.iterator(); it.hasNext();) {
            EntityDeclaration mdec = it.next();
            if (!isMethodLike(mdec)) continue;
            fixMember(mdec);
            if (!hasError(mdec)) {
              fixed.add(mdec);
              it.remove();
            }
          }
        }
      }    
      return cu;
    } finally {
      ctx.setCurrentType(oldCurrentType);
      ctx.setCurrentMethod(oldCurrentMethod);
      astb.setDecompileMethodBodies(oldDecompileMethodBodies);
    }
  }


  public static List<? extends Class<IAstTransform>> getTransformerClassesJRE() {
    if (transformerClasses != null) return (List) transformerClasses;
    try {
      transformerClasses = new ArrayList<>();
      URL url = getClassResource(IAstTransform.class);
      JarFile jf = ((JarURLConnection) url.openConnection()).getJarFile();
      
      Map<String, ZipEntry> em = Reflect.getfldval(jf, "entries");
      List<String> entryNames;
      if (em != null) {
        entryNames = Arrays.asList(em.keySet().toArray(new String[0]));
      } else {
        CodeSource codeSource = Reflect.invoke(
          JarFile.class, jf, "getCodeSource", true,
          new Object[]{ url, url.getFile() }
        );
        Method mtd = java.util.jar.JarFile.class.getDeclaredMethod(
          "entryNames", java.security.CodeSource[].class
        );
        mtd.setAccessible(true);
        Enumeration<String> en = (Enumeration<String>) mtd.invoke(
          jf, new Object[]{ new java.security.CodeSource[]{ codeSource } }
        );
        entryNames = Arrays.asList(
          CollectionUtil.toArray(CollectionUtil.asIterable(en)));
      }
      List<String> clsEntries = CollectionUtil2.filter(
        entryNames, Pattern.compile("^[a-zA-Z0-9_$/]*\\.class$").matcher("")
      );
      List<String> impls = CollectionUtil2.filter(
        clsEntries, Pattern.compile(
          "^com/strobel/decompiler/languages/java/ast/transforms/" +
          "((?!IAstTransform)[a-zA-Z0-9_$/])*\\.class$"
        )
      );
      String[] classNames = ClassInfo.typeToName(CollectionUtil.flatten(
        StringCollectionUtil.matchLines(
          StringUtils.join(impls, "\n"), "([^\n$ ,.]*)\\.class"
        )
      ).toArray(new String[0]));
      
      Class<?>[] classes = CollectionUtil2.to(classNames, Class.class);
      Collections.addAll((Collection) transformerClasses, classes);
      transformerClasses.remove(DeclareVariablesTransform.class);
      
      return (List<? extends Class<IAstTransform>>) (Object) 
        transformerClasses;
    } catch (Exception e) {
      e.printStackTrace();
      throw Reflector.Util.sneakyThrow(e);
    }
  }
  
  public static <N extends AstNode> N transform(N astNode) {
    return transform(astNode, false);
  }

  public static <N extends AstNode> N transform(N astNode, boolean fallback) {
    final DecompilerContext dc = getDecompilerContext();

    List<Object> data = getDataList((UserDataStore) astNode);
    TypeReference typeRef = null;
    TypeDefinition typeDef = null;
    MemberReference memberRef = null;
    IMemberDefinition md = null;
    for (Object item: data) {
      if (item instanceof MemberReference) {
        memberRef = (MemberReference) item;
        typeRef = memberRef.getDeclaringType();
        typeDef = (typeRef instanceof TypeDefinition)
          ? (TypeDefinition) typeRef
          : (TypeDefinition) getResolver(typeRef).resolve(typeRef);
      }
      if (item instanceof TypeReference) {
        typeDef = (typeRef instanceof TypeDefinition)
          ? (TypeDefinition) typeRef
          : (TypeDefinition) getResolver(typeRef).resolve(typeRef);
      }
      if (memberRef instanceof IMemberDefinition) {
        md = (IMemberDefinition) memberRef;
      } else if (memberRef instanceof MethodReference) {
        MethodReference methodRef = (MethodReference) memberRef;
        if (md == null) md = methodRef.resolve();
        if (md == null) md = getMetadataResolver().resolve(methodRef);
      }
    }
    if (md != null || memberRef != null) {
      if (typeRef == null) typeRef = (TypeReference) 
        ((MemberReference) (md!=null? md: memberRef)).getDeclaringType();
      typeDef = typeRef.resolve();
      if (typeDef == null) typeDef = getMetadataResolver().resolve(typeRef);
    }
    
    final TypeDefinition oldCurrentType = dc.getCurrentType();
    final MethodDefinition oldCurrentMethod = dc.getCurrentMethod();
    try {
      if (typeDef == null || (md == null && astNode instanceof EntityDeclaration)) 
      {
        for (AstNode node = astNode; node != null; node = node.getParent()) {
 //       List<Object> data = getDataList((UserDataStore) node);
          if (data.isEmpty()) continue;
          for (Object meta: data) {
            if (meta instanceof IMemberDefinition) {
              IMemberDefinition imd = (IMemberDefinition) meta;
              if (imd instanceof MethodDefinition && md == null) {
                md = (MethodDefinition) imd;
                typeDef = (typeDef != null)
                 ? typeDef
                 : imd.getDeclaringType() != null
                     ? ((MethodDefinition) imd).getDeclaringType()
                     : typeDef;
              } else if (imd instanceof TypeDefinition && typeDef == null) {
                typeDef = typeDef != null? typeDef: (TypeDefinition) imd;
              } else if (typeDef == null) {
                if (imd.getDeclaringType() != null) {
                  typeRef = imd.getDeclaringType();
                  if (typeRef != null) {
                    TypeDefinition typeDef1 = (typeRef instanceof TypeDefinition)
                      ? (TypeDefinition) typeRef
                      : (TypeDefinition) typeRef.resolve();
                    if (typeDef1 != null) {
                      typeDef = typeDef1;
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (typeDef != null) dc.setCurrentType(typeDef);
      if (md != null) dc.setCurrentMethod((MethodDefinition) md);
      
      final TypeDefinition origTypeDef = typeDef;
      final MethodDefinition origMd = md != null? (MethodDefinition) md: null;
      
      Predicate<IAstTransform> stopCond = new Predicate<IAstTransform>() {
        @Override
        public boolean test(final IAstTransform transform) {
          TypeDefinition td = dc.getCurrentType();
          if (td == null) {
            if (origTypeDef == null) return true;
            dc.setCurrentType(origTypeDef);
          }
          MethodDefinition md2 = dc.getCurrentMethod();
          if (origMd != null && md2 == null) {
            dc.setCurrentMethod(origMd);
          }
          return 
           (astNode instanceof EntityDeclaration && dc.getCurrentMethod() == null)
           || dc.getCurrentType() == null;
        }
      };
      // xforms.clear();
      if (!fallback) {
        TransformationPipeline.runTransformationsUntil(
          astNode, // AstNode node
          stopCond, // Predicate<IAstTransform> abortCondition
          dc // DecompilerContext context
        );
        return astNode;
      }
      
      try {
        List<? extends Class<IAstTransform>> classes = getTransformerClassesJRE();
        nextTransformer:
        for (final Iterator<? extends Class<IAstTransform>> it =classes.iterator();
             it.hasNext();)
        {
          final Class<IAstTransform> transCls = it.next();
          if (transCls.getName().equals(
                "com.strobel.decompiler.languages.java.ast.DefiniteAssignmentAnalysis") ||
              transCls.getName().equals(
                "com.strobel.decompiler.languages.java.ast.DeclareVariablesTransform"))
          {
            it.remove(); continue nextTransformer;
          }
          
          if (! IAstTransform.class.isAssignableFrom(transCls) ||
              (transCls.getModifiers() & Modifier.ABSTRACT) != 0 ||
               transCls.isSynthetic())
          {
            System.err.printf(
              "[INFO] Removing transformer class '%s' from list.\n",
              ClassInfo.typeToName(transCls.getName())
            );
            it.remove();
            continue;
          }
          Constructor<?> ctor = null;
          IAstTransform xf = null;
          try {
            for (Constructor _ctor:
                 transCls.getDeclaredConstructors())
            {
              ctor = _ctor;
              Class<?>[] ptypes = ctor.getParameterTypes();
              try {
                if (ptypes.length > 1) {
                  it.remove(); continue nextTransformer;
                }
                if (ptypes.length == 1) {
                  if (ptypes[0] == DecompilerContext.class) {
                    ctor.setAccessible(true);
                    xf = (IAstTransform) ctor.newInstance(dc);
                  } else {
                    it.remove(); continue nextTransformer;
                  }
                } else {
                  ctor.setAccessible(true);
                  xf = (IAstTransform) ctor.newInstance();
                }
              } catch (final ReflectiveOperationException e) {
                e.printStackTrace();
                it.remove(); continue nextTransformer;
              } finally {
                if (xf == null) {
                  it.remove(); continue nextTransformer;
                }
              }
              
              if (xforms != null) xforms.add(xf);
              if (xf != null) xf.run(astNode);
              break;
            }
          } catch (Exception rex) {
            if (it != null) it.remove(); 
            new RuntimeException(String.format(
              "Exception running or instantiating %s (instance: %s  %s): %s",
              transCls, xf, xf != null? Debug.ToString(xf): "", rex
            ), rex).printStackTrace();
            continue nextTransformer;
          } // end catch for no default ctor
        } // for cls in classes
      } catch (Exception e3) {
        new RuntimeException(String.format(
          "Fallback transforms threw exception: %s\n  %s\n",
          e3, StringUtils.join(xforms, "\n  ")
        ), e3).printStackTrace();
      }
      return astNode;
    } finally {
      dc.setCurrentType(oldCurrentType);
      dc.setCurrentMethod(oldCurrentMethod);
    }
  }

  public static DecompilationResult decompile(String className) {
    return decompile(getMetadataResolver(), new String[]{ className });
  }

  public static DecompilationResult decompile(byte[] classBytes) {
    ITypeLoader loader = new ArrayTypeLoader(classBytes);
    IMetadataResolver mds = new MetadataSystem(loader);
    String className = getClassName(classBytes);
    System.err.printf("Class name: %s\n", className);
    return decompile(mds, new String[]{ className });
  }

  public static DecompilationResult decompile(Class<?> cls) {
    try {
      TypeDefinition typeDef = getTypeDefinition(cls);
      return decompile(typeDef);
    } catch (Exception e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }

  public static DecompilationResult decompile(Object ci) {
    return decompile(typeof(ci));
  }

  public static DecompilationResult decompile(Pattern ptrn) {
    String[] classNames = ClassPathUtil.searchClassPath(ptrn);
    return decompile(classNames);
  }


  public static byte[] dex2jar(Class<?> cls) {
    return Dex2Java.convertOne(getDex(cls).getBytes(), cls.getName());
  }

  public static String getClassName(byte[] classBytes) {
    Buffer classBuffer = new Buffer(classBytes);
    String internalName = getInternalNameFromClassFile(classBuffer);
    return internalName.replace('/', '.');
  }

  public static String getInternalNameFromClassFile(Buffer classBuffer) {
    if (GET_INTERNAL_NAME_FROM_CLASSFILE_METHOD == null) {
      GET_INTERNAL_NAME_FROM_CLASSFILE_METHOD = Reflect.findMethod(
        ArrayTypeLoader.class, "getInternalNameFromClassFile",
        Buffer.class
      );
    }
    try {
      return (String) GET_INTERNAL_NAME_FROM_CLASSFILE_METHOD.invoke(
        null, classBuffer
      );
    } catch (ReflectiveOperationException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }


  public static LineNumberTableConverter
  getLineNumberConverter(MethodDefinition md)
  {
    List<SourceAttribute> attrs = md.getSourceAttributes();
    List<LineNumberTableAttribute> linenoAttrs
      = CollectionUtil2.typeFilter(attrs, LineNumberTableAttribute.class);
    if (linenoAttrs.isEmpty()) return null;
    LineNumberTableAttribute linenoAttr = linenoAttrs.iterator().next();
    return new LineNumberTableConverter(linenoAttr);
  }

  public static Range<Integer> getLineNumberRange(MethodDefinition md) {
    LineNumberTableConverter converter = getLineNumberConverter(md);
    if (converter == null) return Range.singleton(Integer.valueOf(0));

    int[] linenos = Reflect.getfldval(converter, "_offset2LineNo");
    int min = Ints.min(linenos);
    int max = Ints.max(linenos);
    return Range.closed(min, max);
  }

  public static void initFormattingOptions(JavaFormattingOptions fmtOpts) {
    fmtOpts.AlignEmbeddedIfStatements = true;
    fmtOpts.AlignEmbeddedUsingStatements = true;
    fmtOpts.AllowIfBlockInline = true;
    fmtOpts.AnnotationBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.AnonymousClassBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.ArrayInitializerBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.ArrayInitializerWrapping = Wrapping.WrapIfTooLong;
    fmtOpts.BlankLinesAfterImports = 2;
    fmtOpts.BlankLinesAfterPackageDeclaration = 1;
    fmtOpts.BlankLinesBeforeFirstDeclaration = 1;
    fmtOpts.BlankLinesBetweenEventFields = 0;
    fmtOpts.BlankLinesBetweenFields = 0;
    fmtOpts.BlankLinesBetweenMembers = 1;
    fmtOpts.BlankLinesBetweenTypes = 2;
    fmtOpts.ClassBraceStyle = BraceStyle.NextLine;
    fmtOpts.ConstructorBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.EnumBraceStyle = BraceStyle.NextLine;
    fmtOpts.EventAddBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.EventBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.EventRemoveBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.FixedBraceEnforcement = BraceEnforcement.DoNotChange;
    fmtOpts.ForBraceEnforcement = BraceEnforcement.DoNotChange;
    fmtOpts.ForEachBraceEnforcement = BraceEnforcement.DoNotChange;
    fmtOpts.IfElseBraceEnforcement = BraceEnforcement.DoNotChange;
    fmtOpts.IndentBlocks = true;
    fmtOpts.IndentBreakStatements = true;
    fmtOpts.IndentCaseBody = true;
    fmtOpts.IndentClassBody = true;
    fmtOpts.IndentEnumBody = true;
    fmtOpts.IndentInterfaceBody = true;
    fmtOpts.IndentMethodBody = true;
    fmtOpts.IndentNamespaceBody = true;
    fmtOpts.IndentSwitchBody = false;
    fmtOpts.InitializerBlockBraceStyle = BraceStyle.DoNotChange;
    fmtOpts.InterfaceBraceStyle = BraceStyle.NextLine;
    fmtOpts.KeepCommentsAtFirstColumn = true;
    fmtOpts.MethodBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.PlaceCatchOnNewLine = false;
    fmtOpts.PlaceElseIfOnNewLine = false;
    fmtOpts.PlaceElseOnNewLine = false;
    fmtOpts.PlaceFinallyOnNewLine = false;
    fmtOpts.PlaceWhileOnNewLine = false;
    fmtOpts.SpaceAfterBracketComma = true;
    fmtOpts.SpaceAfterConditionalOperatorCondition = true;
    fmtOpts.SpaceAfterConditionalOperatorSeparator = true;
    fmtOpts.SpaceAfterConstructorDeclarationParameterComma = true;
    fmtOpts.SpaceAfterDelegateDeclarationParameterComma = false;
    fmtOpts.SpaceAfterFieldDeclarationComma = true;
    fmtOpts.SpaceAfterForSemicolon = true;
    fmtOpts.SpaceAfterIndexerDeclarationParameterComma = true;
    fmtOpts.SpaceAfterLocalVariableDeclarationComma = true;
    fmtOpts.SpaceAfterMethodCallParameterComma = true;
    fmtOpts.SpaceAfterMethodDeclarationParameterComma = true;
    fmtOpts.SpaceAfterNewParameterComma = true;
    fmtOpts.SpaceAfterTypecast = true;
    fmtOpts.SpaceAroundAdditiveOperator = false;
    fmtOpts.SpaceAroundAssignment = true;
    fmtOpts.SpaceAroundBitwiseOperator = true;
    fmtOpts.SpaceAroundEqualityOperator = true;
    fmtOpts.SpaceAroundLogicalOperator = true;
    fmtOpts.SpaceAroundMultiplicativeOperator = false;
    fmtOpts.SpaceAroundNullCoalescingOperator = true;
    fmtOpts.SpaceAroundRelationalOperator = true;
    fmtOpts.SpaceAroundShiftOperator = true;
    fmtOpts.SpaceBeforeArrayDeclarationBrackets = false;
    fmtOpts.SpaceBeforeBracketComma = false;
    fmtOpts.SpaceBeforeCatchParentheses = true;
    fmtOpts.SpaceBeforeConditionalOperatorCondition = true;
    fmtOpts.SpaceBeforeConditionalOperatorSeparator = true;
    fmtOpts.SpaceBeforeConstructorDeclarationParameterComma = false;
    fmtOpts.SpaceBeforeConstructorDeclarationParentheses = false;
    fmtOpts.SpaceBeforeDelegateDeclarationParameterComma = false;
    fmtOpts.SpaceBeforeDelegateDeclarationParentheses = false;
    fmtOpts.SpaceBeforeFieldDeclarationComma = false;
    fmtOpts.SpaceBeforeForParentheses = true;
    fmtOpts.SpaceBeforeForSemicolon = false;
    fmtOpts.SpaceBeforeForeachParentheses = true;
    fmtOpts.SpaceBeforeIfParentheses = true;
    fmtOpts.SpaceBeforeIndexerDeclarationBracket = true;
    fmtOpts.SpaceBeforeIndexerDeclarationParameterComma = false;
    fmtOpts.SpaceBeforeLocalVariableDeclarationComma = false;
    fmtOpts.SpaceBeforeMethodCallParameterComma = false;
    fmtOpts.SpaceBeforeMethodCallParentheses = false;
    fmtOpts.SpaceBeforeMethodDeclarationParameterComma = false;
    fmtOpts.SpaceBeforeMethodDeclarationParentheses = false;
    fmtOpts.SpaceBeforeNewParameterComma = false;
    fmtOpts.SpaceBeforeNewParentheses = false;
    fmtOpts.SpaceBeforeSwitchParentheses = true;
    fmtOpts.SpaceBeforeSynchronizedParentheses = true;
    fmtOpts.SpaceBeforeUsingParentheses = true;
    fmtOpts.SpaceBeforeWhileParentheses = true;
    fmtOpts.SpaceBetweenEmptyConstructorDeclarationParentheses = false;
    fmtOpts.SpaceBetweenEmptyDelegateDeclarationParentheses = false;
    fmtOpts.SpaceBetweenEmptyMethodCallParentheses = false;
    fmtOpts.SpaceBetweenEmptyMethodDeclarationParentheses = false;
    fmtOpts.SpaceInNamedArgumentAfterDoubleColon = true;
    fmtOpts.SpaceWithinConstructorDeclarationParentheses = false;
    fmtOpts.SpaceWithinDelegateDeclarationParentheses = false;
    fmtOpts.SpaceWithinEnumDeclarationParentheses = false;
    fmtOpts.SpaceWithinIndexerDeclarationBracket = false;
    fmtOpts.SpaceWithinMethodCallParentheses = false;
    fmtOpts.SpaceWithinMethodDeclarationParentheses = false;
    fmtOpts.SpacesBeforeBrackets = true;
    fmtOpts.SpacesBetweenEmptyNewParentheses = false;
    fmtOpts.SpacesWithinBrackets = false;
    fmtOpts.SpacesWithinCastParentheses = false;
    fmtOpts.SpacesWithinCatchParentheses = false;
    fmtOpts.SpacesWithinForParentheses = false;
    fmtOpts.SpacesWithinForeachParentheses = false;
    fmtOpts.SpacesWithinIfParentheses = false;
    fmtOpts.SpacesWithinNewParentheses = false;
    fmtOpts.SpacesWithinParentheses = false;
    fmtOpts.SpacesWithinSwitchParentheses = false;
    fmtOpts.SpacesWithinSynchronizedParentheses = false;
    fmtOpts.SpacesWithinUsingParentheses = false;
    fmtOpts.SpacesWithinWhileParentheses = false;
    fmtOpts.StatementBraceStyle = BraceStyle.EndOfLine;
    fmtOpts.UsingBraceEnforcement = BraceEnforcement.DoNotChange;
    fmtOpts.WhileBraceEnforcement = BraceEnforcement.AddBraces;
  }




  public static <E>
  List<E> getDataList(final UserDataStore node, final Key... dataKeys) {
    List<E> values = null;
    final Key[] keys = (dataKeys.length != 0)
      ? dataKeys
      : ALL_KEYS;

    for (int i=0, len=keys.length; i<len; ++i) {
      Key key = keys[i];
      Object value = node.getUserData(key);
      if (value == null) continue;
      if (values == null) values = new ArrayList<E>();
      values.add((E) value);
    }
    return values != null? values: Collections.<E>emptyList();
  }

  public static <E>
  E getDataItem(final UserDataStore node, final Class<E> valueClass) {
    for (int i=0, len=ALL_KEYS.length; i<len; ++i) {
      Key key = ALL_KEYS[i];
      Object value = node.getUserData(key);
      if (value == null || ! valueClass.isInstance(value)) continue;
      return (E) value;
    }
    return null;
  }

  public static <E>
  Map<Key,E> getDataMap(final UserDataStore node, final Key... dataKeys) {
    Map<Key,E> values = null;
    final Key[] keys = (dataKeys.length != 0)
      ? dataKeys
      : ALL_KEYS;

    for (int i=0, len=keys.length; i<len; ++i) {
      Key key = keys[i];
      Object value = node.getUserData(key);
      if (value == null) continue;
      if (values == null) values = new HashMap<Key,E>();
      values.put(key, (E) value);
    }
    return (Map<Key,E>) (Object) (
      (values != null)
        ? (Object) values
        : (Object) Collections.<List,E>emptyMap()
    );
  }




  public static TypeReference getTypeReference(Class<?> cls)  {
    TypeReference tr = null;
    int dimensions = 0;
    Class<?> c = cls;
    try {
      while (c.isArray()) {
        c = c.getComponentType();
        dimensions++;
      }
      if (c.isPrimitive()) {
        int typeIdx
          = (Integer) HASH_PRIMITIVE_NAME.get().invoke(null, c.getName());
        tr = (TypeReference) ((Object[])
          PRIMITIVE_TYPES_BY_DESCRIPTOR.get().get(null))[typeIdx];
      } else {
        tr = getTypeDefinition(c);
      }
      while (dimensions > 0) {
        tr = tr.makeArrayType();
        dimensions--;
      }
      return tr;
    } catch (Exception e) {
      throw Reflector.Util.sneakyThrow(e);
    }
  }

  public static String[] buildRawSignatureParts(Class<?>... pts) {
    String[] sigparts = new String[pts.length];
    for (int i=0,len=pts.length; i<len; ++i) {
      Class<?> cls = pts[i];
      String sigpart = cls.isArray()
        ? cls.getName().replace('.', '/')
        : (cls.isPrimitive()
            ? String.valueOf(Character.toUpperCase(
                cls.getName()
                   .replace("boolean", "Z").replace("long", "J").charAt(0)
              ))
            : new StringBuilder(cls.getName().length() + 2)
                .append('L')
                .append(cls.getName().replace('.', '/'))
                .append(';').toString()
          );
          sigparts[i] = sigpart;
    }
    return sigparts;
  }

  public static String buildRawSignature(Class<?>... pts) {
    StringBuilder sb =new StringBuilder(pts.length * 16);
    for (int i=0,len=pts.length; i<len; ++i) {
      Class<?> cls = pts[i];
      sb.append(
        cls.isArray()
          ? cls.getName().replace('.', '/')
          : (cls.isPrimitive()
              ? String.valueOf(Character.toUpperCase(
                  cls.getName()
                     .replace("boolean", "Z").replace("long", "J").charAt(0)
                ))
              : new StringBuilder(cls.getName().length() + 2)
                  .append('L')
                  .append(cls.getName().replace('.', '/'))
                  .append(';').toString()
            )
      );
    }
    return sb.toString();
  }


  public static String getRawMethodSig(Member m) {
    return String.format(
      "(%s)%s",
      buildRawSignature(
        (m instanceof Method)
          ? ((Method) m).getParameterTypes()
          : ((Constructor<?>) m).getParameterTypes()
      ),
      buildRawSignature(
        (m instanceof Method) ? ((Method) m).getReturnType() : Void.TYPE
      )
    );
  }

  public static List<TypeReference> getTypeReferences(Class<?>... types) {
    TypeReference[] trs = new TypeReference[types.length];
    for (int i=0,len=trs.length; i<len; ++i) {
      trs[i] = getTypeReference(types[i]);
    }
    return Arrays.asList(trs);
  }

  
  public static MethodDefinition getMethodDefinition3(Member method) {
    Class<?> declaringClass = method.getDeclaringClass();
    TypeDefinition typeDef = getTypeDefinition(declaringClass);
    Class<?>[] pTypes = (method instanceof Constructor<?>)
      ? ((Constructor<?>) method).getParameterTypes()
      : ((Method) method).getParameterTypes();
    String methodName = (method instanceof Constructor<?>)
      ? "<init>"
      : ((Method) method).getName();
    String rawSig = getRawMethodSig(method);

    return findMatchingMethodDefinition(typeDef, methodName, rawSig);
  }
  
  
  public static MethodDefinition getConstructorDefinition(
  Constructor<?> ctor)
  {
    Class<?> declaringClass = ctor.getDeclaringClass();
    TypeDefinition typeDef = getTypeDefinition(declaringClass);
    Class<?>[] pTypes = ctor.getParameterTypes();
    String methodName = "<init>";
    String rawSig = String.format("(%s)V", buildRawSignature(pTypes));

    return findMatchingMethodDefinition(typeDef, methodName, rawSig);
  }


  public static MethodDefinition getStaticInitDefinition(
  Class<?> declaringClass)
  {
    TypeDefinition typeDef = getTypeDefinition(declaringClass);
    String methodName = "<clinit>";
    String rawSig = "()V";

    return findMatchingMethodDefinition(typeDef, methodName, rawSig);
  }


  public static MethodDefinition findMatchingMethodDefinition(
  TypeDefinition typeDef, String methodName, String rawSignature)
  {
    for (MethodDefinition thisMethodDef: typeDef.getDeclaredMethods()) {
      if (! methodName.equals(thisMethodDef.getName())) continue;
      if (! rawSignature.equals(thisMethodDef.getErasedSignature())) continue;
      return thisMethodDef;
    }
    return null;
  }

  public static ZipFile[] addJar(ZipFile zipFile) {
    String zipFilePath = PosixFileInputStream.resolve(zipFile.getName());

    ITypeLoader tl = getTypeLoader();
    if (! (tl instanceof ClasspathTypeLoader)) return null;
    ClasspathTypeLoader cptl = (ClasspathTypeLoader) tl;
    ZipFile[] zips = cptl.getJars2();
    boolean found = false;
    for (ZipFile zip: zips) {
      String path = PosixFileInputStream.resolve(zip.getName());
      if (path.equals(zipFilePath)) {
        found = true;
        break;
      }
    }
    if (found) return zips;
    ZipFile[] newZips = ArrayUtils.addAll(zips, new ZipFile[] { zipFile });
    Reflect.setfldval(cptl, "jars2", newZips);
    return newZips;
  }


  public static
    Pair<Map<String, byte[]>, Map<String, List<BootstrapMethodsTableEntry>>>
  getBootstrapMaps(ZipFile zipFile)
  {
    ZipEntry[] ents = CollectionUtil.toArray(
      CollectionUtil2.filter(
        CollectionUtil.asIterable(zipFile.entries()),
        Pattern.compile("^.+?\\.class$").matcher("")
      )
    );
   Map<String, byte[]> zipByteMap = new TreeMap<String, byte[]>();
   Map<String, List<BootstrapMethodsTableEntry>> bootstrapMap
     = new TreeMap<>();

   for (ZipEntry ze: ents) {
      String name = ze.getName();
      String className = StringUtils.substringBeforeLast(name, ".class")
        .replace('/', '.');
      try (InputStream is = zipFile.getInputStream(ze)) {
        byte[] clsBytes = IOUtils.toByteArray(is);
        List<BootstrapMethodsTableEntry> bootstrapItems
          = getBootstrapItems(clsBytes);
        if (bootstrapItems.isEmpty()) continue;
        System.err.printf(
          "Found bootstrap items: class '%s', entry '%s'\n",
          className, name
        );
        bootstrapMap.put(className, bootstrapItems);
        zipByteMap.put(name, clsBytes);
      } catch (Exception ex) {
        // errorState.put(ex, Pair.of(zipFile, ze));
        Throwable rc = ExceptionUtils.getRootCause(ex);
        System.err.printf(
          "* Exception processing '%s': %s: %s\n",
          className,
          rc.getClass().getSimpleName(),
          rc.toString().replaceAll(
            rc.getClass().getName().replace('$', '.').concat("(?:: )?"), ""
          )
        );
        continue;
      }
    }
    return Pair.of(zipByteMap, bootstrapMap);
  }

  public static List<BootstrapMethodsTableEntry> getBootstrapItems(byte[] b)
  {
    byte[] clsBytes = b;

    TypeDefinition td = getTypeDefinition(clsBytes);

    String className = td.getFullName();
    List<SourceAttribute> attrs = td.getSourceAttributes();
    List<BootstrapMethodsAttribute> bootstrapAttrs
      = CollectionUtil2.typeFilter(attrs, BootstrapMethodsAttribute.class);
    if (bootstrapAttrs.isEmpty()) return Collections.emptyList();
    BootstrapMethodsAttribute bsattr = bootstrapAttrs.iterator().next();

    List<BootstrapMethodsTableEntry> entries
      = bsattr.getBootstrapMethods();

    for (BootstrapMethodsTableEntry entry: entries) {
      MethodReference mr = entry.getMethod();
      MethodHandle mh = entry.getMethodHandle();
      List<Object> args = entry.getArguments();
      System.err.printf(StringUtils.join(Arrays.asList(
        "class %s (%d total entries)",
        "  - MethodReference: %s",
        "  -    MethodHandle: %s",
        "  -       Arguments: %s"
      ), "\n"), className, entries.size(), mr, mh, args);
    }
    return entries;
  }
  
  
  static LazyMember<Field> MDS_TYPE_LOADER = of(
    "_typeLoader", "com.strobel.assembler.metadata.MetadataSystem");
  static LazyMember<Field> MDS_TYPES = of(
    "_types", "com.strobel.assembler.metadata.MetadataSystem");
  
  public static TypeDefinition removeType(IMetadataResolver mr,
  String className)
  {
    String name = ClassInfo.typeToName(className);
    String classNameAsPath = getClassNameAsPath(name);
    ITypeLoader typeLoader = MDS_TYPE_LOADER.getValue(mr);
    
    Map<String, ?> mrTypeMap = MDS_TYPES.getValue(mr);
    Map<String, ?> tlTypeMap = getfldval(typeLoader, "cache");
    Object oldDef = null, tmpDef = null;
    if (mrTypeMap != null && mrTypeMap.containsKey(classNameAsPath)) {
      tmpDef = mrTypeMap.remove(classNameAsPath);
      if (tmpDef instanceof TypeDefinition) oldDef = tmpDef;
    }
    if (tlTypeMap != null && tlTypeMap.containsKey(classNameAsPath)) {
      tmpDef = tlTypeMap.remove(classNameAsPath);
      if (tmpDef instanceof TypeDefinition) oldDef = tmpDef;
    }
    if (tlTypeMap != null && tlTypeMap.containsKey(name)) {
      tmpDef = tlTypeMap.remove(name);
      if (tmpDef instanceof TypeDefinition) oldDef = tmpDef;
    }
    return (TypeDefinition) oldDef;
  }
  
  public static Pair<TypeDefinition, byte[]> getDexTypeDefinition(
  byte[] dexBytes, String className, @Nullable IMetadataResolver mr)
  {
    byte[] classBytes = Dex2Java.convertOne(dexBytes, className);
    if (classBytes == null || classBytes.length == 0) {
      throw new RuntimeException(String.format(
        "Could not resolve type [%s] from dex bytes (length=%d): "
        + "Dex2Java.convertOne(%s, \"%s\") returned %s",
        className, dexBytes.length, classBytes, className,
        classBytes == null? "null": "empty byte array"
      ));
    }
    return Pair.of(getTypeDefinition(classBytes, mr), classBytes);
  }
  
  public static TypeDefinition getTypeDefinition(byte[] classBytes, 
  @Nullable IMetadataResolver mr)
  {
    String classNameAsPath
      = getInternalNameFromClassFile(new Buffer(classBytes)),
      className = ClassInfo.typeToName(classNameAsPath);
    
    ITypeLoader typeLoader,
      arrayTypeLoader = new ArrayTypeLoader(classBytes);
    
    if (mr != null) {
      ITypeLoader existingTypeLoader = MDS_TYPE_LOADER.getValue(mr);
      typeLoader = new CompositeTypeLoader(new ITypeLoader[] {
        arrayTypeLoader, existingTypeLoader
      });
    } else {
      typeLoader = arrayTypeLoader;
    }
    
    IMetadataResolver arrayMetadataResolver
      = new MetadataSystem(typeLoader);
    TypeReference tr = arrayMetadataResolver.lookupType(
        ClassInfo.classNameToPath(className));
    if (tr == null) throw new RuntimeException(String.format(
      "Could not resolve type [%s] from "
      + "classBytes (length=%d): %s.lookupType(\"%s\") returned null",
      className, classBytes.length, 
      arrayMetadataResolver.getClass().getName(),
      ClassInfo.classNameToPath(className)
    ));
    TypeDefinition td = tr.resolve();
    if (td == null) throw new RuntimeException(String.format(
      "Could not fully resolve type [%s] from "
      + "classBytes (length=%d); TypeReference: %s: "
      + "%s.resolve() returned null",
      className, classBytes.length, tr, tr.getClass().getName()
    ));
    return td;
  }
  
  
  
  
  public static MethodDefinition getMethodDefinition2(Member method) {
    
    final Class<?> declaringClass = method.getDeclaringClass();
    final Class<?>[] pTypes = Reflect.getParameterTypes(method);
    
    final String rawSignature = (method instanceof Constructor<?>)
      ? String.format("(%s)V", getErasedSignature(pTypes))
      : getErasedSignature((Method) method);
    final String methodName = (method instanceof Constructor<?>)
      ? "<init>"
      : ((Method) method).getName();
    
    final TypeReference declaringType = getTypeReference(declaringClass);
    final TypeDefinition td = (declaringType instanceof TypeDefinition)
      ? (TypeDefinition) declaringType
      : declaringType.resolve();
    
    final List<MethodDefinition> choices = new LinkedList<>();
    for (final MethodDefinition md: td.getDeclaredMethods()) {
      if (! methodName.equals(md.getName())) continue;
      choices.add(md);
      if (rawSignature.equals(md.getErasedSignature())) return md;
    }
    
    final RuntimeException ex = new RuntimeException(String.format(
      "Didn't find matching %s in %s '%s': %s %s; choices: %s",
      method.getClass().getSimpleName().toLowerCase(),
      td.getClass().getSimpleName(),
      ClassInfo.typeToName(td.getErasedSignature()), methodName, rawSignature,
      (choices.isEmpty())? "(none)": "\n  - ",
      StringUtils.join(choices,      "\n  - ")
    ));
    
    // errorState.put(ex, Triple.of(method, td, choices));
    throw ex;
  }
  
  @Extension
  public static <D extends IMetadataTypeMember>
  D getAstData(AstNode node, Class<D> dataClass)
  {
    List<?> dataList = getDataList(node);
    for (Object dataItem: dataList) {
      if (dataClass.isAssignableFrom(dataItem.getClass()))
        return (D) dataItem;
      try {
        Method resolve 
          = Reflect.getMember(dataItem.getClass(), "resolve");
        if (resolve != null 
        && dataClass.isAssignableFrom(resolve.getReturnType())) 
        {
          resolve.setAccessible(true);
          Object item = resolve.invoke(dataItem, new Object[0]);
          if (item != null) {
            return (D) item;
          }
        }
      } catch (Exception e) { 
      }
    }    
    return null;
  }
  
  @Extension
  public static <E extends IMetadataTypeMember> 
  List<E> getAstDataMembers(AstNode node, Class<E> memberDataClass)
  {
    return (List<E>) (Object) CollectionUtil2.typeFilter(
      (List<E>) (Object)
      (
        new SelectTransformer.Default(new
          Function<EntityDeclaration, E>() {
            @Override
            public E apply(EntityDeclaration m) {
              return (E) (Object) getAstData(m, memberDataClass);
            }
          }
        ).select(
          CollectionUtil2.typeFilter(
            node.getDescendantsAndSelf(),
            EntityDeclaration.class
          )
        )
      ),
      memberDataClass
    );
  }
  
  
  
  
  public static String getErasedSignature(Class<?>... pts) { 
    StringBuilder sb =new StringBuilder(pts.length * 16);
    for (int i=0,len=pts.length; i<len; ++i) {
      Class<?> cls = pts[i]; 
      sb.append(
        cls.isArray()
          ? cls.getName().replace('.', '/')
          : (cls.isPrimitive()
              ? String.valueOf(Character.toUpperCase(
                  cls.getName()
                     .replace("boolean", "Z")
                     .replace("long", "J").charAt(0)
                ))
              : new StringBuilder(cls.getName().length() + 2)
                  .append('L')
                  .append(cls.getName().replace('.', '/'))
                  .append(';').toString()
            )
      );
    }
    return sb.toString();
  }
  
  
  public static String getErasedSignature(Member member) {
    boolean isMethod = member instanceof Method;
    return String.format(
      "(%s)%s",
      getErasedSignature(
        (member instanceof Method)
          ? ((Method) member).getParameterTypes()
          : ((Constructor<?>) member).getParameterTypes()
      ),
      (member instanceof Method)
        ? getErasedSignature(((Method) member).getReturnType())
        : "V"
    );
  }
  



  
  public static String getErasedSignature(MethodReference md) {
    List<ParameterDefinition> params = md.getParameters(); 
    if (params == null) params = Collections.emptyList();
    StringBuilder sb = new StringBuilder(params.size() * 24);
    sb.append('(');
    for (ParameterDefinition param: params) {
      sb.append(param.getParameterType().getErasedSignature());
    }
    TypeReference retType ;
    sb.append(')')
      .append(
        (retType = md.getReturnType()) != null
          ? retType.getErasedSignature()
          : "V"
      );
    return sb.toString();
  }
  
  public static String[]
  getErasedSignatureParts(MethodReference md) 
  {
    List<ParameterDefinition> params = md.getParameters(); 
    if (params == null) params = Collections.emptyList();
    
    int paramsLen = params.size();;
    String[] paramSig = new String[paramsLen];
    for (int i=0; i<paramsLen; ++i) {
      paramSig[i] 
        = params.get(i).getParameterType().getErasedSignature();
    }
    return paramSig;
  }
  
  
  //        ______________________________
  // ______/  MethodReference ==> Member  \_________________________
    
  @Extension
  public static <M extends Member> M getMethod(MethodReference md) {
    String[] paramSig = getErasedSignatureParts(md);
    int paramsLen = paramSig.length;
    String name = md.getName();
    Class<?>[] paramTypes = CollectionUtil2.toClasses(paramSig);
    Class<?> declaringType
      = DexVisitor.classForName(md.getDeclaringType().getErasedSignature());
    AccessibleObject ao = (AccessibleObject)
      Reflect.getDeclaredConstructorOrMethod(
        declaringType, name, paramTypes
      );
    if (ao != null) try {
      ao.setAccessible(true);
    } catch (Exception ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
    return (M) (Object) ao;
  }
  
  
  //        _______________________________
  // ______/  Member ==> MethodDefinition  \_________________________
  
  @Extension
  public static MethodDefinition getMethodDefinition(Member member) {
    Class declaringClass = member.getDeclaringClass();

    TypeDefinition typeRef = getTypeDefinition(declaringClass);
    boolean isMethod = member instanceof Method;
    Class<?>[] paramTypes = (isMethod)
      ? ((Method) member).getParameterTypes()
      : ((Constructor<?>) member).getParameterTypes();
    
    String name = (isMethod) ? ((Method) member).getName(): "<init>";
    String sig = ProcyonUtil.getErasedSignature(member);
    MethodDefinition md
      = findMatchingMethodDefinition(typeRef, name, sig);
    if (md != null) return md;
    // If the method was not found in the TypeDefinition returned by the main
    // metadata resolver, then the runtime method comes from a different 
    // version of the class (or variant, platform, etc.) than the one the
    // type loader found / has available.
    
    // In the case of Android, we can detect and circumvent this issue by 
    // grabbing the *Android* runtime class definition (thanks to the
    // Android-specific public method java.lang.Class#getDex()) and convert
    // the relevant class from dex format to to java bytecode '.class' 
    // format on the fly, remove the old type from the resolver, and explicitly 
    // load the newly-converted one to find the real method (including all 
    // code attributes, debug data, annotations, etc.)
    
    // Generate '.class' bytecode & decompile definition
    // from dex class
    removeType(getMetadataResolver(), declaringClass.getName());
    final Pair<TypeDefinition, byte[]> pair = getDexTypeDefinition(
      getDex(declaringClass).getBytes(),
      declaringClass.getName(),
      getMetadataResolver()
    );
    TypeDefinition td = pair.getKey();
    byte[] classBytes = pair.getValue();
    loadType(
      td, classBytes, getMetadataResolver(),
      CollectionUtil.getClassPool()
    );
    return findMatchingMethodDefinition(td, name, sig);
  }
  
  public static MethodDefinition getDexMethodDefinition(final Member mtd,
    @Nullable IMetadataResolver mr)
  {
    final Class<?> cls = mtd.getDeclaringClass();
    final Pair<TypeDefinition, byte[]> result = getDexTypeDefinition(
      getDex(cls).getBytes(), cls.getName(), mr
    );
    final TypeDefinition td = result.getKey();
    final MethodDefinition md = findMatchingMethodDefinition(
      td, (mtd instanceof Method)? ((Method) mtd).getName(): "<init>", 
      getRawMethodSig(mtd)
    );
    if (mr instanceof MetadataSystem) {
      removeType(mr, cls.getName());
      ((MetadataSystem) mr).addTypeDefinition(td);
    }
    return md;
  }
  
  /**
  Generate '.class' bytecode & decompile TypeDefinition
  from dex class bytes
  */
  public static void loadType(TypeDefinition td, byte[] classBytes,
  @Nullable IMetadataResolver mr, @Nullable ClassPool cp) 
  {
    String className = ClassInfo.typeToName(td.getErasedSignature());
    if (mr != null) {
      if (mr instanceof MetadataSystem) {
        ((MetadataSystem) mr).addTypeDefinition(td);
        TypeReference tref
          = mr.lookupType(className.replace('.', '/'));
        if (tref != null && tref != td) {
          System.err.printf(
            "[INFO] Removing %s {%s} from %s {%s}\n",
            tref.getClass().getSimpleName(), tref,
            mr.getClass().getSimpleName(), mr
          );
          removeType(mr, className);
          ((MetadataSystem) mr).addTypeDefinition(td);
          tref = mr.lookupType(className.replace('.', '/'));
          if (tref != td) {
            System.err.printf(
              "[WARN] Failed to update type in %s\n", mr);
          }
        }
      } else {
        Map<String, TypeDefinition> mrTypeMap
          = Reflect.getfldval(mr, "_types");
        if (mrTypeMap != null) {
          mrTypeMap.put(getClassNameAsPath(className), td);
        }
      }
    }
    if (cp != null) {
      ByteArrayClassPath bacp = new ByteArrayClassPath(
        ClassInfo.typeToName(td.getErasedSignature()), classBytes
      );
      Object oldHead = getfldval(getfldval(cp, "source"), "pathList");
      Object newHead = Reflect.newInstance(
        DexVisitor.classForName("javassist.ClassPathList"), 
        bacp, oldHead
      );
      setfldval(getfldval(cp, "source"), "pathList", newHead);
    }
  }
  
  
  public static MethodDefinition makeFakeOrphanMethodDefinition(Member method) {
    MethodDefinition mdef;
    try {
      mdef = (MethodDefinition) METHOD_DEFINITION_CTOR.get().newInstance();
    } catch (Exception e) {
      throw Reflector.Util.sneakyThrow(e);
    }
    String methodName = (method instanceof Constructor)
      ? "<init>": ((Method) method).getName();
    Reflector.invokeOrDefault(mdef, "setName", methodName);
    // Create a metadata factory for this "[generic] type scope"
    Class<?> declaringClass = method.getDeclaringClass();
    TypeDefinition dctd = getTypeDefinition(declaringClass);
    Reflector.invokeOrDefault(mdef, "setDeclaringType", dctd);
    Class<?> returnType = (method instanceof Constructor)
      ? Void.TYPE: ((Method)method).getReturnType();
    TypeReference rttr = getTypeReference(returnType);
    Reflector.invokeOrDefault(mdef, "setReturnType", rttr);
    MetadataFactory mdf = Reflect.newInstance(
      CoreMetadataFactory.class, dctd, getResolver(dctd), dctd
    );

    Class<?>[] pTypes = (method instanceof Constructor)
      ? ((Constructor)method).getParameterTypes()
      : ((Method)method).getParameterTypes();

    String rawSig = (method instanceof Constructor<?>)
      ? String.format("(%s)V", buildRawSignature(pTypes))
      : getRawMethodSig((Method) method);

    Reflect.setfldval(mdef, "_erasedSignature", rawSig);
    Reflect.setfldval(
      mdef, "_signature",
      AnnotationAccess.getSignature((AnnotatedElement) method)
    );

    IMethodSignature mSig = Reflector.invokeOrDefault(
      mdf, "makeMethodSignature",
      rttr, // TypeReference returnType
      getTypeReferences(pTypes), // List<TypeReference> parameterTypes
      Collections.emptyList(), // List<GenericParameter> genericParameters
      Collections.emptyList()  // List<TypeReference> thrownTypes
    );
    ParameterDefinitionCollection coln
      = Reflect.newInstance(ParameterDefinitionCollection.class, mSig);
    Reflector.invokeOrDefault(coln, "setDeclaringType", dctd);
    for (int i=0,len=pTypes.length; i<len; i++) {
      coln.add(mSig.getParameters().get(i));
    }
    Reflect.setfldval(mdef, "_parameters", coln);
    Reflect.setfldval(
      mdef, "_parametersView", Collections.unmodifiableList(coln)
    );
    MethodDefinition realDef;
    try {
      if ((realDef = mdef.resolve()) != null) return realDef;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return mdef;
  }
  
  
  //        _________
  // ______/  Misc.  \_________________________
  
  @Extension
  public static MethodDefinition getOverriddenMethod(
  MethodDefinition md)
  {
    Set<String> seen = new TreeSet<String>();
    String sig = md.getErasedSignature();
    String name = md.getName();
    TypeDefinition dtype = md.getDeclaringType().resolve();
    String dtypeName = dtype.getErasedSignature();
    ArrayDeque<TypeReference> q = new ArrayDeque<TypeReference>(); 
    q.offer(dtype);
    while (! q.isEmpty()) {
      TypeReference stypeRef = q.pollFirst();
      String stypeName = stypeRef.getErasedSignature();
      if (seen.contains(stypeName)) continue;
      seen.add(stypeName);
      
      TypeDefinition stype = (stypeRef instanceof TypeDefinition)
        ? (TypeDefinition) stypeRef
        : stypeRef.resolve();
      
      if (! stypeName.equals(dtypeName)) {
        MethodDefinition superMd 
         = findMatchingMethodDefinition(stype, name, sig);
        if (superMd != null) return superMd;
      }
      if ("java.lang.Object".equals(stypeName) || stypeName == null) {
        continue;
      }
      List<TypeReference> gptypes = FluentIterable.concat(
        Arrays.asList(stype.getBaseType()), 
        stype.getExplicitInterfaces()
      ).toList();
      q.addAll(gptypes);
    }
    return null; // Optional.empty();
  }
  
  
  
  @Extension
  public static List<? extends MethodDeclaration> decompileToAst(
  Iterable<? extends Member> members) 
  {
    List<MethodDeclaration> mdecls = new LinkedList<>();
    for (Member member: members) {
      MethodDeclaration mdecl = decompileToAst(member);
      if (mdecl == null) continue;
      mdecls.add(mdecl);
    }
    return mdecls;
  }
  
  public static class HasErrorContent implements Predicate<Comment> {
    static final String ERROR_TEXT = "This method could not be decompiled.";
    static final int MAX_WHITESPACE = 4;
    public static final HasErrorContent INSTANCE = new HasErrorContent();
    @Override
    public boolean test(final Comment comment) {
      final int pos = comment.getContent().indexOf(ERROR_TEXT);
      return pos != -1 && pos <= MAX_WHITESPACE;
    }
  }
  
  public static boolean hasError(final EntityDeclaration ast) {
    final BlockStatement body = ast.getChildByRole(Roles.BODY);
    if (body.isNull()) return false;
    final AstNodeCollection<Comment> comments
     = body.getChildrenByRole(Roles.COMMENT);
    return (comments.firstOrNullObject(HasErrorContent.INSTANCE) != null);
  }
  
  public static List<EntityDeclaration> findMembersWithError(
    final AstNode ast)
  {
    final Iterable<AstNode> nodes = ast.getDescendantsAndSelf();
    final List<EntityDeclaration> members
      = CollectionUtil2.typeFilter(nodes, EntityDeclaration.class);
    for (final Iterator<EntityDeclaration> it = members.iterator();
         it.hasNext();) 
    {
      final EntityDeclaration member = it.next();
      if (!hasError(member)) it.remove();
    }
    return members;
  }
  
  public static List<EntityDeclaration> fixMembers(
    final Iterable<EntityDeclaration> members)
  {
    return fixMembers(members, new HashSet<>());
  }
  
  
  
  public static List<EntityDeclaration> fixMembers(
    final Iterable<EntityDeclaration> members, final Set<Object> seen)
  {
    final List<EntityDeclaration> replacements = new ArrayList<>();
    
    final List<EntityDeclaration> reverseList = new ArrayList<>();
    reverseList.addAll(CollectionUtilities.toList(members));
    
    for (final EntityDeclaration member: members) {
      replacements.add(
        0,
        hasError(member) ? fixMember(member): member
      );
    }
    return replacements;
  }
  
  public static final Map<Triple<String, String, String>, Integer> counts
    = new HashMap<>();
    
  public static Triple<String, String, String> getKey(IMemberDefinition md) {
    if (md == null) return Triple.of("", "", "");
    
    String name = md.getName();
    String sig = (md instanceof MemberReference)
      ? ((MemberReference) md).getErasedSignature()
      : "";
    TypeReference declaringType = md.getDeclaringType();
    String declaringTypeName = declaringType != null
      ? declaringType.getErasedSignature()
      : "";
    
    return Triple.of(
      declaringTypeName != null? declaringTypeName: "",
      name != null? name: "",
      sig != null? sig: ""
    );
  }
  
  public static int incrementAndGet(IMemberDefinition md) {
    if (md == null) return FIX_LIMIT;
    final  Triple<String, String, String> key = getKey(md);
    synchronized (counts) {
      final int curCount = counts.containsKey(key)? counts.get(key).intValue(): 0;
      final int updatedCount = (curCount < FIX_LIMIT)? curCount + 1: curCount;
      counts.put(key, Integer.valueOf(updatedCount));
      return updatedCount;
    }
  }
  
  public static int FIX_LIMIT = 200;
  
  
  public static EntityDeclaration fixMember(final EntityDeclaration member) 
  {
    final MethodDefinition md = 
        (METHOD_DEFINITION.get(member) != null)
          ? (MethodDefinition) METHOD_DEFINITION.get(member)
          : (MEMBER_REFERENCE.get(member) instanceof MethodDefinition)
              ? (MethodDefinition) MEMBER_REFERENCE.get(member)
              : (MEMBER_REFERENCE.get(member) != null)
                  ? ((MethodDefinition) Reflector.invokeOrDefault(
                      MEMBER_REFERENCE.get(member), "resolve"
                    ))
                  : null;
    if (md == null) throw new RuntimeException(String.format(
      "Failed to resolve MethodDeclaration for entity declaration: %s", 
      member
    ));
    int numFixes = incrementAndGet(md);
    if (numFixes == 20) {
      Reflect.setfldval(md, "_body", null);
      astBuilder = null;
      decompilerContext = new DecompilerContext(getDecompilerSettings());
    }
    if (numFixes > FIX_LIMIT) {
      throw new Error("fix limit");
      
    }
    TypeDefinition td = md.getDeclaringType();
    Deque<TypeDefinition> q = new ArrayDeque<>();
    q.offerLast(td);
    List<MethodDefinition> ml = new ArrayList<>();
    while (!q.isEmpty()) {
      TypeDefinition type = q.pollLast();
      Collection<MethodDefinition> mds = type.getDeclaredMethods();
      ml.addAll(mds);
      System.err.printf(
        "Found %d methods in %s %s\n",
        mds.size(),
        (type.getErasedSignature().indexOf('$') == -1)
          ? "type"
          : "nested type",
        ClassInfo.typeToName(type.getErasedSignature())
      );
      for (final TypeDefinition nestedType: type.getDeclaredTypes()) {
        q.offerLast(nestedType);
      }
    };
    Collections.reverse(ml);
    List<EntityDeclaration> masts = new ArrayList<>();
    MethodDefinition _curMd = null;
    for (MethodDefinition m: ml) {
      try {
      Reflect.setfldval(m, "_body", null);
      
      AstBuilder astb = Keys.AST_BUILDER.get(decompilerContext);
      if (astb != null) {
        Keys.AST_BUILDER.set(decompilerContext, null);
        // astb.setDecompileMethodBodies(true);
      }
      final MethodBody body = m.getBody();
      Reflect.setfldval(body, "_isFrozen", Boolean.FALSE);
      final Collection<?> handlers
        = Reflect.getfldval(body, "_exceptionHandlers");
      Reflect.setfldval(handlers, "_isFrozen", Boolean.FALSE);
      handlers.clear();
      // System.err.println(CollectionUtil.printR(handlers));
      if (MetadataResolver.areEquivalent(
        m.getDeclaringType(), md.getDeclaringType()) &&
        m.getErasedSignature().equals(md.getErasedSignature()) &&
        m.getName().equals(md.getName()))
      {
        _curMd = m;
      }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (!hasError(member)) return member;
    if (_curMd == null) _curMd = md;
    // if (!String.valueOf(lastFix).equals(String.valueOf(_curMd))) {
      try {
        decompilerContext.setCurrentType(_curMd.getDeclaringType());
        decompilerContext.setCurrentMethod(_curMd);
        final MethodBody mbody = _curMd.getBody();
        Reflect.setfldval(mbody, "_isFrozen", Boolean.FALSE);
        final Collection<?> handlers
          = Reflect.getfldval(mbody, "_exceptionHandlers");
        Reflect.setfldval(handlers, "_isFrozen", Boolean.FALSE);
        handlers.clear();
        final EntityDeclaration decl = decompileToAst(_curMd, false, false);
        final BlockStatement body = decl.getChildByRole(Roles.BODY);
        member.getChildByRole(Roles.BODY).remove();
        body.remove();
        member.addChild(body, Roles.BODY);
      } finally {
        decompilerContext.setCurrentType(_curMd.getDeclaringType());
        decompilerContext.setCurrentMethod(_curMd);
      }
    //} else {
    //return member;
    //}
    return member;
  }
  static MethodDefinition lastFix = null;
  
  public static Class<?> typeof(final Object obj) {
    if (obj == null) return Object.class;
    final Class<?> c = obj.getClass();
    if (Class.class.isAssignableFrom(c)) {
      return (Class<?>) obj;
    } else if (c.getName().equals("bsh.ClassIdentifier")) {
      try {
        final Method getTargetClass = c.getDeclaredMethod("getTargetClass");
        getTargetClass.setAccessible(true);
        return (Class<?>) getTargetClass.invoke(obj);
      } catch (final ReflectiveOperationException | LinkageError ex) {
        ex.printStackTrace();
      }
    }
    return c;
  }
  
  
  public static Pair<com.strobel.decompiler.ast.AstBuilder, ControlFlowGraph>
    getCfg(final MethodDefinition md)
  {
    final MethodBody body = md.getBody();
    final InstructionCollection oldInstructions = body.getInstructions();
    final InstructionCollection newInstructions 
      = com.strobel.decompiler.ast.AstBuilder.copyInstructions(oldInstructions);
    
    final Map<Instruction, Instruction> insnMap = new IdentityHashMap<>();
    for (int i = 0, nsz = newInstructions.size(); i < nsz; ++i) {
      insnMap.put(newInstructions.get(i), oldInstructions.get(i));
    }
    
    final List<ExceptionHandler> exceptionHandlers = body.getExceptionHandlers();
    
    final List<ExceptionHandler> remappedExceptionHandlers
      = com.strobel.decompiler.ast.AstBuilder.remapHandlers(
          exceptionHandlers, newInstructions);
    
    Collections.sort(remappedExceptionHandlers);
    
    final com.strobel.decompiler.ast.AstBuilder.Builder builder
      = com.strobel.decompiler.ast.AstBuilder.builder(getDecompilerContext())
        .setBody(body)
        .setOptimize(false)
        .setInstructions(newInstructions)
        .setOriginalInstructionMap(insnMap)
        .setExceptionHandlers(remappedExceptionHandlers);
      
    final com.strobel.decompiler.ast.AstBuilder astBuilder = builder.build();
    
    astBuilder.pruneExceptionHandlers();
    // generate CFG
    final ControlFlowGraph cfg
      = ControlFlowGraphBuilder.build(newInstructions, remappedExceptionHandlers);
    builder.setCfg(cfg);
    return Pair.of(astBuilder, cfg);
  }
  
  
  
  
  public static CompilationUnit decompileTypeToAst(final TypeDefinition type,
    final DecompilationOptions options, boolean allowTransform)
  {
    final boolean isSingleMember = false;
    
    final AstBuilder builder = createAstBuilder(options, type, isSingleMember);
    
    Boolean origHaveTransformationsRun = (!allowTransform)
      ? Reflect.getfldval(builder, "_haveTransformationsRun")
      : null;
    if (!allowTransform) {
      Reflect.setfldval(builder, "_haveTransformationsRun", Boolean.TRUE);
    }
    try {
      builder.addType(type);
    } finally {
      if (!allowTransform && origHaveTransformationsRun != null) {
        Reflect.setfldval(
          builder, "_haveTransformationsRun", origHaveTransformationsRun
        );
      }
    }
    if (allowTransform) {
      final Boolean haveTransformationsRun2
        = Reflect.getfldval(builder, "_haveTransformationsRun");
      if (Boolean.FALSE.equals(haveTransformationsRun2)) {
        final JavaLanguage javaLanguage = getLanguage();
        final DecompilerContext ctx = ASTBUILDER_GET_CONTEXT.invoke(builder)
          != null? ASTBUILDER_GET_CONTEXT.invoke(builder):
           getDecompilerContext();
        
        try {
          LANG_RUN_TRANSFORMS.invoke(
            javaLanguage,
            builder, options, new AddStandardAnnotationsTransform(ctx)
          );
        } catch (final Exception t) {
          t.printStackTrace();
        } finally {
          Reflect.setfldval(builder, "_haveTransformationsRun", Boolean.TRUE);
        }
      }
    }
    return builder.getCompilationUnit();
  }
  
  
  static final Class<? extends VariableReference> CLS_UNKNOWN_VARIABLE_REFERENCE
    = (Class<? extends VariableReference>) (Class<?>) DexVisitor.classForName("com.strobel.assembler.metadata.VariableDefinitionCollection$UnknownVariableReference");
  
  static StringBuilder dsb = new StringBuilder(1024);
  
  @SuppressWarnings("UnusedParameters")
  public static AstBuilder createAstBuilder(final DecompilationOptions options,
    final TypeDefinition currentType, final boolean isSingleMember)
  {
    final DecompilerSettings settings = options.getSettings();
    final DecompilerContext context = REUSE_CONTEXT
      ? getDecompilerContext()
      : new DecompilerContext();
    decompilerContext = (g_ctx = context);
    context.setCurrentType((g_type = currentType));
    // context.setCurrentMethod(null); DO NOT USE
    context.setSettings(settings);
    
    do {
      if (hadVerifyError) {
        return LANG_CREATE_ASTBUILDER.invoke(
          (JavaLanguage) getLanguage(), options, currentType, isSingleMember
        );
      }
      try {
        return new AstBuilder(context) {
          {
            g_astBuilder = this;
            Keys.AST_BUILDER.set(context, this);
          }
          
          @Override
          protected BlockStatement createMethodBody(final MethodDefinition method,
            final Iterable<ParameterDeclaration> parameters)
          {
            g_method = method;
            g_type = method.getDeclaringType();
            g_params = parameters;
            final DecompilerContext prevCtx = g_ctx;
            if ((g_ctx = Reflect.getfldval(this, "_context")) == null) {
              g_ctx = (prevCtx != null) ? prevCtx : context;
            }
            g_typeLoader = options.getSettings().getTypeLoader();
            
            if (_decompileMethodBodies) {
              try {
                return (g_blockStatement =
                  AstMethodBodyBuilder.createMethodBody(
                    this, method, _context, parameters
                  )
                );
              } catch (final Exception t) {
                g_exception = t;
                Reflect.setfldval(method, "_body", null);
                
                com.strobel.assembler.ir.attributes.CodeAttribute codeAttr 
                  = CollectionUtil2.typeFilter(
                    (Collection<?>) Reflect.getfldval(method, "_sourceAttributes"),
                    com.strobel.assembler.ir.attributes.CodeAttribute.class
                  ).iterator().next();
                final Collection<?> exEntriesView = 
                  Reflect.getfldval(codeAttr, "_exceptionTableEntries");
    
                final MethodBody mbody = method.getBody();
                
                System.err.printf("\nmbody = %s\n\n", mbody);
                Class cls = mbody != null? mbody.getClass(): null;
                dsb.setLength(0);
                try {
                  while (cls != null) {
                    for (Field fld: cls.getDeclaredFields()) {
                      if ((fld.getModifiers() & Modifier.STATIC) != 0) continue;
                      fld.setAccessible(true);
                      Object val = fld.get(mbody);
                      
                      
                      boolean interesting =
                         cls.getName().startsWith("com.strob") ||
                         cls.getName().indexOf("util") != -1 ||
                         cls.getName().indexOf("oll") != -1;
                      
                      dsb.append(String.format(
                         (interesting)
                           ?  "\u001b[1;33m%s\u001b[0m = " +
                              "(\u001b[1;36m%s\u001b[0m)  %s\n" +
                              "    %s\n"
                           : "\u001b[0;33m%s\u001b[0m = " +
                             "(\u001b[0;36m%s\u001b[0m)  %s\n",
                        fld.getName(),
                        ClassInfo.getSimpleName(val),
                        Dumper.tryToString(val),
                        interesting ? Debug.ToString(val).replace("\n", "\n    ") : ""
                      ));
                      if (val instanceof Object[]) {
                        val = Arrays.asList((Object[]) val);
                      }
                      
                      if (val instanceof Iterable<?>) {
                        dsb.append("    items: [\n");
                        for (final Object o: (Iterable<?>) val) {
                          dsb.append("      ")
                             .append(Dumper.tryToString(o).replace("\n", "\n      "))
                             .append('\n');
                        }
                        dsb.append("\n    ]\n");
                      }
                    }
                    cls = cls.getSuperclass();
                  }
                  
                } catch (final ReflectiveOperationException | 
                               RuntimeException | 
                               Error roe)
                {
                  roe.printStackTrace();
                } finally {
                  System.err.println(dsb);
                }
                if (exEntriesView != null) exEntriesView.clear();
                Reflect.setfldval(mbody, "_isFrozen", Boolean.FALSE);
                final Collection<?> handlers
                  = Reflect.getfldval(mbody, "_exceptionHandlers");
                Reflect.setfldval(handlers, "_isFrozen", Boolean.FALSE);
                handlers.clear();
                Reflect.setfldval(
                  method, "_body", new java.lang.ref.SoftReference<MethodBody>(mbody)
                );
                Keys.AST_BUILDER.set(context, this);
                
                
                for (final Instruction insn: mbody.getInstructions()) {
                  if (!insn.getOpCode().isLoad()) continue;
                  final int operandCount = insn.getOperandCount();
                  if (operandCount == 0) continue;
                  if (insn.getOpCode().getOperandType() == OperandType.Local) {
                    continue;
                  }
                  final Object operand = insn.getOperand(0);
                  if (! (operand instanceof VariableReference)) continue;
                  final Variable var = convertToVariable((VariableReference) operand);
                  insn.setOperand(var);
                }
                
                Boolean o_oldState = Reflect.getfldval(
                  this, "_haveTransformationsRun"
                );
                boolean oldState = (o_oldState != null) 
                  ? o_oldState.booleanValue()
                  : false;
                try {
                  //Reflect.setfldval(this, "_haveTransformationsRun", Boolean.TRUE);
                  final BlockStatement stmt = AstMethodBodyBuilder.createMethodBody(
                    this, method, context, parameters
                  );
                  return (g_blockStatement = stmt);
                } catch (final Exception e) {
                  // e.addSuppressed(t);
                  e.printStackTrace();
                } finally {
                  Reflect.setfldval(this, "_haveTransformationsRun", oldState);
                }
              }
            }
            return null;
          }
        };
      } catch (final LinkageError e) {
        e.printStackTrace();
        hadVerifyError = true;
        System.err.println(e);
      }
    } while (true);
  }
  
  public static Variable convertToVariable(final Object obj) {
    if (obj instanceof Variable) {
      return (Variable) obj;
    }
    if (obj instanceof VariableReference) {
      return convertToVariable((VariableReference) obj);
    }
    return null;
    /*throw new Error(String.format(
      "Don't know how to convert object to Variable: (%s) %s:\n  %s",
      obj != null? obj.getClass().getName(): "null",
      obj,
      Debug.ToString(obj)
    ));*/
  }
  
  public static Variable convertToVariable(final VariableReference vref) {
    final TypeReference variableType = vref.getVariableType();
    final TypeReference declaringType = vref.getDeclaringType();
    final String name = vref.getName();
    final int slot = vref.getSlot();
    final VariableDefinition vdef =
      (!CLS_UNKNOWN_VARIABLE_REFERENCE.isInstance(vref))
        ? vref.resolve()
        : null;
    final VariableDefinition resolved = vref.resolve();
    final ParameterDefinition param = (resolved != null)
      ? resolved.getParameter()
      : null;
    final boolean fromMetadata = (vdef != null)
      ? vdef.isFromMetadata() : false;
    
    final Variable var = new Variable();
    var.setType(variableType);
    var.setName(name != null ? name.replace("$", "_") : null);
    // var.setSlot(slot);
    Reflect.setfldval(var, "origin", vdef != null? vdef: vref);
    
    try {
      var.setOriginalVariable(resolved);
      var.setOriginalParameter(param);
    } catch (UnsupportedOperationException uoe) {
      boolean found = false;
      MethodDefinition mdef
        = ProcyonUtil.getDecompilerContext().getCurrentMethod();
      if (mdef != null) {
        MethodBody mbody = mdef.getBody();
        VariableDefinitionCollection vars = mbody.getVariables();
        for (VariableDefinition _vdef: vars) {
          if (_vdef.getSlot() == ((VariableReference) vref).getSlot()) {
            Log.d(
              "Inlining", "Recovered VariableDefinition: %s: %s",
              _vdef, Debug.ToString(_vdef)
            );
            var.setOriginalVariable(_vdef);
            var.setName(_vdef.getName());
            var.setType(_vdef.getVariableType());
          }
          found = true;
          break;
        }
      } 
      if (!found) {
        Log.e(
          "Inlining",
          "Cannot resolve operand: (%s) %s:\n  %s\n",
          vref.getClass().getSimpleName(),
          vref,
          Debug.ToString(vref)
        );
      }
    }
    

    if (vdef != null) var.setOriginalVariable(vdef);
    if (vdef != null && vdef.isParameter()) {
      var.setOriginalParameter(vdef.getParameter());
    }
    if (vdef != null) var.setGenerated(fromMetadata);
    var.setType(variableType);
    return var;
  }
  
  
  
  public static ResolveResult resolveTypeFromVariable(
    final Variable variable)
  {
    if (variable == null) return null;
    TypeReference type = variable.getType();
    if (type == null) {
      if (variable.isParameter()) {
        final ParameterDefinition parameter
          = variable.getOriginalParameter();
        if (parameter != null) type = parameter.getParameterType();
      }
      final VariableDefinition originalVariable
        = variable.getOriginalVariable();
      if (originalVariable != null) {
        type = originalVariable.getVariableType();
      }
    }
    return (type != null) ? new ResolveResult(type) : null;
  }
  
  
  public static ResolveResult resolveTypeFromMember(
    final MemberReference member)
  {
    if (member == null) return null;
    
    if (member instanceof FieldReference) {
      return new ResolveResult(((FieldReference) member).getFieldType());
    }
    
    if (! (member instanceof MethodReference)) return null;
    
    final MethodReference method = (MethodReference) member;
    return (method.isConstructor())
      ? new ResolveResult(method.getDeclaringType())
      : new ResolveResult(method.getReturnType());
  }
  
  static final TIntSet existsCache = new TIntHashSet(512);
  static final TIntSet notExistsCache = new TIntHashSet(512);
  
  public static boolean typeExists(String name, CoreMetadataFactory mf) {
    if (name == null) return false;
    if (existsCache.contains(name.hashCode())) return true;
    if (notExistsCache.contains(name.hashCode())) return false;
    
    final IMetadataResolver res = Reflect.getfldval(mf, "_resolver");
    if (res == getMetadataResolver()) {
      ITypeLoader typeLoader = getTypeLoader();
      boolean oldGen = ClassInfo.generateTypes;
      try {
        ClassInfo.generateTypes = false;
        Buffer buf = new Buffer();
        
        return (typeLoader.tryLoadType(
          ClassInfo.typeToName(name).replace('.', '/'), buf
        ));
      } finally {
        ClassInfo.generateTypes = oldGen;
      }
    }
    boolean exists = 
      Thread.currentThread().getContextClassLoader().getResource(
        ClassInfo.classNameToPath(name, "class")) != null ||
        DexVisitor.classForName(name) != null;
    (exists? existsCache: notExistsCache).add(name.hashCode());
    return exists;
  }
  
  
}