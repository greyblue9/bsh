package org.d6r;

import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.FieldId;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.android.dex.TableOfContents;
import com.android.dex.TableOfContents.Section;
import java.lang.reflect.Method;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.HashMap;

/**
 * Executable that prints all indices of a dex file.
 */
public class DexUtil {
    
  
  public abstract static class TryAction<V, R> {
    public final List<Throwable> actionErrors 
      = new ArrayList<Throwable>();
      
    public Object param;
    public Object result;
    public Object resultOnError;
    
    public boolean isRun;
    
    public TryAction(final V param, final R resultOnError)
    {
      this.param = (V) param;
      this.resultOnError = (R) resultOnError;
      this.result = (R) resultOnError;
      isRun = false;
    }
    
    public abstract R target(V param);
    
    public R run() {
      if (isRun) return (R) result;
      isRun = true;
      try {
        result = target((V) param);
      } catch (Throwable e) {
        actionErrors.add(e);
        String paramToStr = "???";
        String paramClassName = param != null
          ? param.getClass().getName()
          : "null";
        try {
          paramToStr = param == null
            ? "<NULL>"
            : param.toString();
        } catch (Throwable tse) {          
          paramToStr = String.format(
            "<(%s param).toString() threw %s: %s>",
            paramClassName,
            tse.getClass().getSimpleName(),
            tse.getMessage() != null
              ? tse.getMessage(): "no msg"
          );
        }
        System.err.printf(
       "Call to target((%s) param = %s) threw %s: [%s]\n",
          paramClassName, 
          paramToStr,
          e.getClass().getSimpleName(),
          e.getMessage() != null? e.getMessage(): "no msg"
        );
      }
      return (R) result;
    }
  }
/**
  public class com.android.dex.Dex {
    public static Dex create(java.nio.ByteBuffer)
      throws IOException
    public void writeHashes()
      throws IOException
    public void writeTo(java.io.OutputStream)
      throws IOException
    public void writeTo(java.io.File)
      throws IOException
    public B[] computeSignature()
      throws IOException
    private void loadFrom(java.io.InputStream)
      throws IOException
    public int computeChecksum()
      throws IOException
  }
  
  public class com.android.dex.TableOfContents {
    private void readHeader(com.android.dex.Dex$Section)
      throws UnsupportedEncodingException
    public void writeMap(com.android.dex.Dex$Section)
      throws IOException
    public void writeHeader(com.android.dex.Dex$Section)
      throws IOException
    public void readFrom(com.android.dex.Dex)
      throws IOException
    private void readMap(com.android.dex.Dex$Section)
      throws IOException
  }
*/


  public final Dex dex;
  

  public final TableOfContents tableOfContents;
  public boolean isValid;
  public final List<Throwable> errors 
    = new ArrayList<Throwable>();
  
  public DexUtil(Dex dex, String pathName) {
    Dex _dex = dex;
    TableOfContents _tableOfContents = null;
    try {
      _tableOfContents = _dex.getTableOfContents();
      isValid = true;
    } catch (Exception e) {
      isValid = false;
      errors.add(e);
      System.err.printf(
        "[%s]: Dex file is invalid: %s\n",
        pathName != null
          ? pathName
          : "unspecified dex file",
        e.getMessage() != null? e.getMessage(): "no msg"
      );
    } catch (Throwable e) {
      isValid = false;
      errors.add(e);
      e.printStackTrace();
    }
    this.dex = _dex;
    this.tableOfContents = _tableOfContents;
  }
  
  public DexUtil(Dex dex) {
    this(dex, null);
  }
  
  public DexUtil(File file, String pathName) { 
    this(fileToDex(file), pathName);
  }
  
  public DexUtil(File file) { 
    this(file, file.getPath());
  }
  
  public DexUtil(URL url, String pathName) { 
    this(urlToDex(url), pathName);
  }
  
  public DexUtil(URL url) { 
    this(urlToDex(url), url.toString());
  }
  
  public DexUtil(String dexFilePath) {
    this(new File(dexFilePath), dexFilePath);
  }
  
  public static Dex urlToDex(URL url) {
    try {      
      String uriString = url.toURI().toString();
      String className = lookupDexMap.get(uriString);
      
      if (className != null) {
        try {
          Class<?> cls = Class.forName(
            className, false, 
            ClassLoader.getSystemClassLoader()
          );
          Method m_getDex 
            = Class.class.getDeclaredMethod("getDex");
          Dex dex = (Dex) m_getDex.invoke(cls);
          if (dex != null) return dex;
        } catch (Throwable e) { 
          e.printStackTrace(); 
        }
      }
      
      // method 2
      String path = uriString.replaceAll(
        "^([^:]+:)*/*(/[^:!]+)(!.*)*$", "$2"
      );
      if (uriString.indexOf("!/classes.dex") != -1
       || path.indexOf(".dex") == path.length() - 4) 
      {
        InputStream is = null;
        try {          
          URLConnection conn = url.openConnection();
          conn.setUseCaches(false); 
          is = conn.getInputStream();
          return new Dex(is);
        } finally {
          if (is != null) is.close();
        }
      }
      
      // method 3
      System.err.println("DexUtil: method 3");
      
      return new Dex(new File(path));
    } catch (Throwable e) { 
      (new RuntimeException(String.format(
        "[%s]: Dex file is invalid: %s",
        url != null? url.toString(): "(URL == null)",
        e.getMessage() != null? e.getMessage(): "no msg"
      ))).printStackTrace();
    }
    return null;
  }
  
  public static Dex fileToDex(File file) {
    try {
      return new Dex(file);
    } catch (Throwable e) { 
      (new RuntimeException(String.format(
        "[%s]: Dex file is invalid: %s",
        file.getPath(),
        e.getMessage() != null? e.getMessage(): "no msg"
      ))).printStackTrace();
    }
    return null;
  }
  
  public void printMap() {
    if (!isValid) return;
    (new TryAction<TableOfContents,Object>(
      tableOfContents, (Object) null) 
    { @Override
      public Object target (TableOfContents toc)
      {     
        for (Section section: toc.sections) {
          if (section.off == -1) continue; 
          
          System.out.printf(
            "section %s off=%s size=%s byteCount=%s\n",
            Integer.toHexString(section.type),
            Integer.toHexString(section.off),
            Integer.toHexString(section.size),
            Integer.toHexString(section.byteCount)
          );
        }
        return null;
      }
    }).run();
  }

  public void printStrings() throws IOException {
    if (!isValid) return;
    int index = 0;
    for (String string : dex.strings()) {
      System.out.println("string " + index + ": " + string);
      index++;
    }
  }

  public void printTypeIds() throws IOException {
    if (!isValid) return;
    int index = 0;
    for (Integer type : dex.typeIds()) {
      System.out.println("type " + index + ": " + dex.strings().get(type));
      index++;
    }
  }

  public void printProtoIds() throws IOException {
    if (!isValid) return;
    int index = 0;
    for (ProtoId protoId : dex.protoIds()) {
      System.out.println("proto " + index + ": " + protoId);
      index++;
    }
  }

  public void printFieldIds() throws IOException {
    if (!isValid) return;
    int index = 0;
    for (FieldId fieldId : dex.fieldIds()) {
      System.out.println("field " + index + ": " + fieldId);
      index++;
    }
  }

  public void printMethodIds() throws IOException {
    if (!isValid) return;
    int index = 0;
    for (MethodId methodId : dex.methodIds()) {
      System.out.println("methodId " + index + ": " + methodId);
      index++;
    }
  }

  public void printTypeLists() throws IOException {
    if (!isValid) return;
    if (tableOfContents.typeLists.off == -1) {
      System.out.println("No type lists");
      return;
    }
    Dex.Section in 
      = dex.open(tableOfContents.typeLists.off);
    for (int i=0; i<tableOfContents.typeLists.size; i++) {
      int size = in.readInt();
      System.out.print("Type list i=" + i + ", size=" + size + ", elements=");
      for (int t = 0; t < size; t++) {
        System.out.print(" " + dex.typeNames().get((int) in.readShort()));
      }
      if (size % 2 == 1) {
        in.readShort(); // retain alignment
      }
      System.out.println();
    }
  }

  /*public void printClassDefs() {
    if (!isValid) return;
    int index = 0;
    for (ClassDef classDef : dex.classDefs()) {
      System.out.println("class def " + index + ": " + classDef);
      index++;
    }
  }
  */
  public int printClassDefs() {
    if (!isValid) return 0;
    return (new TryAction<Iterable<ClassDef>, Integer>(
      dex.classDefs(), Integer.valueOf(0)) 
    { @Override
      public Integer target (Iterable<ClassDef> defs)
      {     
        int index = 0;
        for (ClassDef classDef: defs) {
          System.out.printf(
            "class def %d: %s\n",
            index++,
            classDef
          );
        }
        return Integer.valueOf(index);
      }
    }).run().intValue();
  }
  
  public int appendClassDefs(final StringBuilder sb) {
    if (!isValid) return 0;
    return (new TryAction<Iterable<ClassDef>, Integer>(
      dex.classDefs(), Integer.valueOf(0))
    { @Override
      public Integer target (Iterable<ClassDef> defs)
      {     
        int index = 0;
        for (ClassDef classDef: defs) {
          sb.append(String.format(
            "class def %d: %s\n",
            index++,
            classDef
          ));
        }
        return Integer.valueOf(index);
      }
    }).run().intValue();
  }



  public String[] getClassNames() {
    if (!isValid) return new String[0];
    return (new TryAction<Iterable<ClassDef>, String[]>(
      dex.classDefs(), new String[0])
    { @Override
      public String[] target (Iterable<ClassDef> defs)
      {
        ArrayList<String> names 
          = new ArrayList<String>(350);
        
        int index = 0;
        String toStr, name;
        for (ClassDef classDef: defs) {
          toStr = classDef.toString();
          if (toStr.length() < 3) continue; 
          name = toStr.substring(1, toStr.indexOf(';'))
                      .replace('/', '.');
          names.add(name);
        }
        return names.toArray(new String[0]);
      }
    }).run();
  }

  public static void main(String... paths) {
    int idx = -1;
    
    for (String path: paths) {
      
      idx += 1;
      DexUtil dip = new DexUtil(path);
      if (! dip.isValid) {
        System.err.printf(
          "Skipping invalid file: %s\n",
          path
        );
        continue; 
      }
      if (paths.length > 1) {
        System.out.printf(
          "%s==> %s\n", 
          idx > 0? "\n": "",
          path
        );
      }
      
      StringBuilder sb = new StringBuilder(76 * 250);
      int numDefs = dip.appendClassDefs(sb);
      System.out.println(sb.toString());
      sb = null;
      /** /
      indexPrinter.printMap();
      indexPrinter.printStrings();
      indexPrinter.printTypeIds();
      indexPrinter.printProtoIds();
      indexPrinter.printFieldIds();
      indexPrinter.printMethodIds();
      indexPrinter.printTypeLists();
      */      
      System.err.printf("\ntotal: %d\n", numDefs);
    }
  }
  
  
  
  public static final Map<String,String> lookupDexMap
   = new HashMap<String,String>() {
    { 
      put("jar:file:///system/framework/bouncycastle.jar!/classes.dex", "com.android.org.bouncycastle.asn1.ASN1Encodable");
      put("jar:file:///system/framework/android.policy.jar!/classes.dex", "com.android.internal.policy.impl.BarController$1");
      put("jar:file:///system/framework/webviewchromium.jar!/classes.dex", "com.android.org.chromium.android_webview.AndroidProtocolHandler");
      put("jar:file:///system/framework/core-junit.jar!/classes.dex", "junit.extensions.ActiveTestSuite$1");
      put("jar:file:///system/framework/services.jar!/classes.dex", "android.service.tima.ITimaISLCallback");
      put("jar:file:///system/framework/secocsp.jar!/classes.dex", "com.sec.android.org.bouncycastle.asn1.ocsp.BasicOCSPResponse");
      put("jar:file:///system/framework/framework2.jar!/classes.dex", "android.opengl.EGL14");
      put("jar:file:///system/framework/stayrotation.jar!/classes.dex", "com.sec.android.smartface.SmartScreen");
      put("jar:file:///system/framework/okhttp.jar!/classes.dex", "com.android.okhttp.Address");
      put("jar:file:///system/framework/smartfaceservice.jar!/classes.dex", "com.sec.android.smartface.CameraController$CameraListener");
      put("jar:file:///system/framework/WfdCommon.jar!/classes.dex", "com.qualcomm.wfd.ExtendedRemoteDisplay$1");
      put("jar:file:///system/framework/core.jar!/classes.dex", "java.lang.Object");
      put("jar:file:///system/framework/voip-common.jar!/classes.dex", "android.net.rtp.AudioCodec");
      put("jar:file:///system/framework/apache-xml.jar!/classes.dex", "org.apache.xalan.Version");
      put("jar:file:///system/framework/commonimsinterface.jar!/classes.dex", "com.samsung.commonimsinterface.imscommon.CameraState");
      put("jar:file:///system/framework/mms-common.jar!/classes.dex", "com.google.android.mms.ContentType");
      put("jar:file:///system/framework/sec_edm.jar!/classes.dex", "android.app.enterprise.Account$1");
      put("jar:file:///system/framework/org.codeaurora.Performance.jar!/classes.dex", "org.codeaurora.Performance");
      put("jar:file:///system/framework/seccamera.jar!/classes.dex", "com.sec.android.seccamera.SecCamera$1");
      put("jar:file:///system/framework/telephony-common.jar!/classes.dex", "android.provider.Telephony$BaseMmsColumns");
      put("jar:file:///system/framework/scrollpause.jar!/classes.dex", "com.sec.dmc.smartux.ConfMaker");
      put("jar:file:///system/framework/ext.jar!/classes.dex", "com.android.i18n.phonenumbers.AlternateFormatsCountryCodeSet");
      put("jar:file:///system/framework/conscrypt.jar!/classes.dex", "com.android.org.conscrypt.AbstractSessionContext$1");
      put("jar:file:///system/framework/framework.jar!/classes.dex", "android.Manifest$permission");
      
      put("jar:file:////system/framework/framework2.jar!/classes.dex", "android.opengl.EGL14");
    put("jar:file:////system/framework/framework.jar!/classes.dex", "android.Manifest$permission");
    put("jar:file:////system/framework/commonimsinterface.jar!/classes.dex", "com.samsung.commonimsinterface.imscommon.CameraState");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-collections4-4.1-bin/commons-collections4-4.1.jar!/classes.dex", "org.apache.commons.collections4.ArrayStack");
    put("jar:file:////system/framework/scrollpause.jar!/classes.dex", "com.sec.dmc.smartux.ConfMaker");                                                      put("jar:file:////system/framework/mms-common.jar!/classes.dex", "com.google.android.mms.ContentType");
    put("jar:file:////external_sd/_projects/sdk/jadx/jadx.jar!/classes.dex", "LogCatBroadcaster");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-discovery-0.5-bin/commons-discovery-0.5.jar!/classes.dex", "org.apache.commons.discovery.DiscoveryException");
    put("jar:file:////external_sd/_projects/sdk/lib/excalibur-bzip2-1.0.jar!/classes.dex", "org.apache.excalibur.bzip2.BZip2Constants");
    put("jar:file:////system/framework/secocsp.jar!/classes.dex", "com.sec.android.org.bouncycastle.asn1.ocsp.BasicOCSPResponse");                           put("jar:file:////external_sd/_projects/sdk/commons/commons-cli-1.3.1-bin/commons-cli-1.3.1.jar!/classes.dex", "org.apache.commons.cli.ParseException");                                                                                                                                                          put("jar:file:////external_sd/_projects/sdk/javassist/lib/jboss_deps.jar!/classes.dex", "EDU.oswego.cs.dl.util.concurrent.Barrier");
    put("jar:file:////external_sd/_projects/sdk/lib/lombok-ast-0.2.3.jar!/classes.dex", "JavaFile");
    put("jar:file:////system/framework/pm.jar!/classes.dex", "com.android.commands.pm.Pm$1");                                                                put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/javafile.jar!/classes.dex", "JavaFile");
    put("jar:file:////external_sd/_projects/sdk/commons-lang3-3.4.jar!/classes.dex", "org.apache.commons.lang3.AnnotationUtils");
    put("jar:file:////system/framework/core.jar!/classes.dex", "java.lang.Object");                                                                          put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/dedexer.jar!/classes.dex", "hu.uw.pallergabor.dedexer.Annotation");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-lang3-3.4-bin/commons-lang3-3.4.jar!/classes.dex", "org.apache.commons.lang3.builder.ToStringStyle");
    put("jar:file:////external_sd/_projects/sdk/lib/cfr_0_117.jar!/classes.dex", "org.benf.cfr.reader.BugCheck");
    put("jar:file:////external_sd/_projects/sdk/lib/archives.jar!/classes.dex", "SevenZip.CRC");                                                             put("jar:file:////external_sd/_projects/sdk/bin/javassist.jar!/classes.dex", "com.cedarsoftware.util.io.JsonObject");
    put("jar:file:////external_sd/_projects/sdk/lib/dx-asmdex-dexmaker.jar!/classes.dex", "com.android.ddmlib.AdbCommandRejectedException");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-exec-1.3-bin/commons-exec-1.3.jar!/classes.dex", "org.apache.commons.exec.CommandLine$1");
    put("jar:file:////external_sd/_projects/sdk/commonscommons-collections-3.2.2-bin/commons-collections-3.2.2.jar!/classes.dex", "org.apache.commons.collections.Buffer");
    put("jar:file:////system/framework/okhttp.jar!/classes.dex", "com.android.okhttp.Address");
    put("jar:file:////external_sd/_projects/sdk/bin/dex2jar_all.jar!/classes.dex", "com.android.dx.Version");
    put("jar:file:////system/framework/WfdCommon.jar!/classes.dex", "com.qualcomm.wfd.ExtendedRemoteDisplay$1");                                             put("jar:file:////system/framework/telephony-common.jar!/classes.dex", "android.provider.Telephony$BaseMmsColumns");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-compress-1.11-bin/commons-compress-1.11.jar!/classes.dex", "org.apache.commons.compress.PasswordRequiredException");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-io-2.5-bin/commons-io-2.5.jar!/classes.dex", "org.apache.commons.io.ByteOrderMark");         put("jar:file:////system/framework/org.codeaurora.Performance.jar!/classes.dex", "android.Manifest$permission");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-beanutils-1.9.2-bin/commons-beanutils-1.9.2.jar!/classes.dex", "org.apache.commons.beanutils.BaseDynaBeanMapDecorator$MapEntry");
    put("jar:file:////external_sd/_projects/sdk/lib/commons_all.jar!/classes.dex", "org.apache.commons.lang3.builder.ToStringStyle");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-logging-1.2-bin/commons-logging-1.2.jar!/classes.dex", "org.apache.commons.logging.Log");
    put("jar:file:////system/framework/bouncycastle.jar!/classes.dex", "com.android.org.bouncycastle.asn1.ASN1Encodable");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/javafile_cl.jar!/classes.dex", "JavaFile");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-imaging-1.0-20130811.122228-3.jar!/classes.dex", "org.apache.commons.imaging.ColorTools");
    put("jar:file:////external_sd/_projects/sdk/lib/javamanager.jar!/classes.dex", "LogCatBroadcaster");
    put("jar:file:////external_sd/_projects/sdk/lib/org.jf.jar!/classes.dex", "org.jf.baksmali.Adaptors.AnnotationFormatter");                               put("jar:file:////system/framework/conscrypt.jar!/classes.dex", "com.android.org.conscrypt.AbstractSessionContext$1");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/commons_collections_4.4.1.jar!/classes.dex", "org.apache.commons.collections4.ArrayStack");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-net-3.5-bin/commons-net-3.5.jar!/classes.dex", "org.apache.commons.net.DatagramSocketClient");                                                                                                                                                        put("jar:file:////external_sd/_projects/sdk/lib/text2pdf.jar!/classes.dex", "android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat$AccessibilityServiceInfoVersionImpl");
    put("jar:file:////system/framework/seccamera.jar!/classes.dex", "com.sec.android.seccamera.SecCamera$1");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-digester3-3.2-bin/commons-digester3-3.2.jar!/classes.dex", "org.apache.commons.digester3.Rule");
    put("jar:file:////external_sd/_projects/sdk/lib/at4j_full_1.1.2.jar!/classes.dex", "SevenZip.CRC");                                                      put("jar:file:////external_sd/_projects/sdk/commons/commons-pool-1.6-bin/commons-pool-1.6.jar!/classes.dex", "org.apache.commons.pool.KeyedObjectPool");                                                                                                                                                          put("jar:file:////external_sd/_projects/sdk/commons/commons-validator-1.4.1-bin/commons-validator-1.4.1.jar!/classes.dex", "org.apache.commons.validator.Arg");                                                                                                                                                   put("jar:file:////system/framework/sec_edm.jar!/classes.dex", "android.app.enterprise.Account$1");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-dbcp2-2.1.1-bin/commons-dbcp2-2.1.1.jar!/classes.dex", "org.apache.commons.dbcp2.AbandonedTrace");
    put("jar:file:////external_sd/_projects/sdk/lib/guava.jar!/classes.dex", "com.google.common.annotations.Beta");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-jxpath-1.3-bin/commons-jxpath-1.3.jar!/classes.dex", "org.apache.commons.jxpath.AbstractFactory");                                                                                                                                                    put("jar:file:////system/framework/am.jar!/classes.dex", "com.android.commands.am.Am$1");
    put("jar:file:////external_sd/_projects/sdk/lib/LzmaAlone.jar!/classes.dex", "SevenZip.CRC");
    put("jar:file:////external_sd/_projects/sdk/d2j/pkg/showjava.jar!/classes.dex", "antlr.ANTLRError");                                                     put("jar:file:////system/framework/core-junit.jar!/classes.dex", "junit.extensions.ActiveTestSuite$1");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-jexl-3.0-bin/commons-jexl3-3.0.jar!/classes.dex", "org.apache.commons.jexl3.JexlArithmetic$ArrayBuilder");
    put("jar:file:////external_sd/_projects/sdk/lib/sqlite.jar!/classes.dex", "SQLite.Authorizer");
    put("jar:file:////external_sd/_projects/sdk/lib/brut.jar!/classes.dex", "brut.androlib.Androlib");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-configuration2-2.0-bin/commons-configuration2-2.0.jar!/classes.dex", "org.apache.commons.configuration2.event.EventListener");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-codec-1.10-bin/commons-codec-1.10.jar!/classes.dex", "org.apache.commons.codec.Decoder");
    put("jar:file:////external_sd/_projects/sdk/dictzip/dictzip.jar!/classes.dex", "org.dict.zip.DictZipFileUtils");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-scxml-0.9-bin/commons-scxml-0.9.jar!/classes.dex", "org.apache.commons.scxml.Builtin$1");
    put("jar:file:////external_sd/_projects/sdk/lib/antlr4-runtime-4.5.jar!/classes.dex", "org.abego.treelayout.Configuration$AlignmentInLevel");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/antlr_4.5.3_complete__javafile.jar!/classes.dex", "JavaFile");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/org.jf.jar!/classes.dex", "org.jf.baksmali.Adaptors.AnnotationFormatter");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/lib/paranamer_mini.jar!/classes.dex", "com.thoughtworks.paranamer.BytecodeReadingParanamer$1");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-net-3.5-bin/commons-net-examples-3.5.jar!/classes.dex", "examples.Main");
    put("jar:file:////system/framework/webviewchromium.jar!/classes.dex", "com.android.org.chromium.android_webview.AndroidProtocolHandler");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-pool2-2.4.2-bin/commons-pool2-2.4.2.jar!/classes.dex", "org.apache.commons.pool2.KeyedPooledObjectFactory");
    put("jar:file:////system/framework/stayrotation.jar!/classes.dex", "com.sec.android.smartface.SmartScreen");
    put("jar:file:////system/framework/ext.jar!/classes.dex", "com.android.i18n.phonenumbers.AlternateFormatsCountryCodeSet");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-daemon-1.0.15-bin/commons-daemon-1.0.15.jar!/classes.dex", "org.apache.commons.daemon.Daemon");
    put("jar:file:////system/framework/voip-common.jar!/classes.dex", "android.net.rtp.AudioCodec");
    put("jar:file:////system/framework/apache-xml.jar!/classes.dex", "org.apache.xalan.Version");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-dbutils-1.6-bin/commons-dbutils-1.6.jar!/classes.dex", "org.apache.commons.dbutils.AbstractQueryRunner");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-configuration-1.10-bin/commons-configuration-1.10.jar!/classes.dex", "org.apache.commons.configuration.AbstractConfiguration$1");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-jexl-2.1.1-bin/commons-jexl-2.1.1.jar!/classes.dex", "org.apache.commons.jexl2.JexlInfo");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-validator-1.5.1-bin/commons-validator-1.5.1.jar!/classes.dex", "org.apache.commons.validator.Arg");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-math3-3.6.1-bin/commons-math3-3.6.1.jar!/classes.dex", "org.apache.commons.math3.Field");
    put("jar:file:////external_sd/_projects/sdk/perflib_442r1/perflib_442r1.jar!/classes.dex", "com.android.SdkConstants");
    put("jar:file:////system/framework/android.policy.jar!/classes.dex", "com.android.internal.policy.impl.BarController$1");
    put("jar:file:////external_sd/_projects/sdk/jbzip2/jbzip2_0.9.1.jar!/classes.dex", "demo.Compress");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-email-1.4-bin/commons-email-1.4.jar!/classes.dex", "org.apache.commons.mail.ByteArrayDataSource");
    put("jar:file:////external_sd/_projects/sdk/javassist/javassist.jar!/classes.dex", "javassist.CannotCompileException");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-dbcp-1.4-bin/commons-dbcp-1.4.jar!/classes.dex", "org.apache.commons.dbcp.AbandonedConfig");
    put("jar:file:////system/framework/services.jar!/classes.dex", "android.opengl.EGL14");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-lang-2.6-bin/commons-lang-2.6.jar!/classes.dex", "org.apache.commons.lang.ArrayUtils");
    put("jar:file:////system/framework/smartfaceservice.jar!/classes.dex", "com.sec.android.smartface.CameraController$CameraListener");
    put("jar:file:////external_sd/_projects/sdk/netbeans_classfile/netbeans_classfile.jar!/classes.dex", "org.netbeans.modules.classfile.Access");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-dbcp-1.3-bin/commons-dbcp-1.3.jar!/classes.dex", "org.apache.commons.dbcp.AbandonedConfig");
    put("jar:file:////external_sd/_projects/sdk/bin/javac-1.0.jar!/classes.dex", "com.sun.javadoc.AnnotationDesc$ElementValuePair");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-csv-1.4-bin/commons-csv-1.4.jar!/classes.dex", "org.apache.commons.csv.Assertions");
    put("jar:file:////external_sd/_projects/sdk/commons/commons-chain-1.2-bin/commons-chain-1.2.jar!/classes.dex", "org.apache.commons.chain.Catalog");
    put("jar:file:////external_sd/_projects/sdk/bsh/trunk/bsh-mod.jar!/classes.dex", "Dumper2");
      
    }
  };
  
  /*public static class DexMap 
    extends HashMap<String, String>
    implements Serializable, Cloneable
  {
    Map<String, String> byClassName;
    
    public DexMap() {
      super();
    }
    public DexMap(int capacity) {
      super(capacity);
    }
    public DexMap(int capacity, float loadFactor) {
      super(capacity, loadFactor);
    }
    public 
    DexMap(Map<? extends String, ? extends String> map) {
      this(capacityForInitSize(map.size()));
      constructorPutAll(map);
    }
    
    @Override
    public void put(String uri, String className) {
      super.put(uri, className);
    }    
    
    @Override
    public void 
    putAll(Map<? extends String, ? extends String> map) {
      Iterator<Map.Entry<? extends String, ? extends String>>
        it = map.entrySet()())super.put(uri, className);
    }    
  }*/
  
}