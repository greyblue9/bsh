package org.d6r;

import org.d6r.PosixFileInputStream;
import java.io.File;
import jadx.api.IJadxArgs;
import java.io.IOException;


public class JadxArgs implements IJadxArgs {

  public File outDir;
  
  public JadxArgs(final File outDir) {
    this.setOutDir(outDir);
  }
  
  public JadxArgs(final String outDirPath) {
    this(new File(outDirPath));
  }
  
  public JadxArgs() {
    this(PosixFileInputStream.createTemporaryDirectory("jadx"));
    System.err.printf(
      "[INFO] Using directory for output: [%s]\n", 
      outDir.getPath()
    );
  }
  
  public void setOutDir(final String outDirPath) {
    this.setOutDir(new File(outDirPath));
  }
  
  public void setOutDir(final File outDir) {
    if (outDir.exists()) {
      if (!outDir.isDirectory()) {
        throw new IllegalArgumentException(String.format(
          "Output dir '%s' exists and is not a directory", outDir.getPath()
        ));
      }
    } else {
      outDir.mkdirs();
    }
    this.outDir = outDir;
  }

  @Override
  public int getDeobfuscationMaxLength() {
    return 16;
  }
  
  @Override
  public int getDeobfuscationMinLength() {
    return 1;
  }
  
  @Override
  public File getOutDir() {
    return this.outDir;
  }

  @Override
  public boolean escapeUnicode() {
    return true;
  }
  
  @Override
  public int getThreadsCount() {
    return 16;
  }
  
  @Override
  public boolean isCFGOutput() {
    return false;
  }
  
  @Override
  public boolean isDeobfuscationForceSave() {
    return false;
  }
  
  @Override
  public boolean isDeobfuscationOn() {
    return false;
  }
  
  @Override
  public boolean isExportAsGradleProject() {
    return false;
  }
  
  @Override
  public boolean isFallbackMode() {
    return false;
  }
  
  @Override
  public boolean isRawCFGOutput() {
    return false;
  }
  
  @Override
  public boolean isReplaceConsts() {
    return true;
  }
  
  @Override
  public boolean isShowInconsistentCode() {
    return true;
  }
  
  @Override
  public boolean isSkipResources() {
    return false;
  }

  @Override
  public boolean isSkipSources() {
    return false;
  }  
  
  @Override
  public boolean isVerbose() {
    return false;
  }
  
  @Override
  public boolean useSourceNameAsClassAlias() {
    return true;
  }
}

