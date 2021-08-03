package org.d6r;

import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.d6r.ByteUtil;


public class CommandParser {
  
  protected static final Charset CHARSET
    = StandardCharsets.UTF_8;
  
  static final boolean JRE = CollectionUtil.isJRE();
  
  protected static Boolean _verboseEnabled;
  
  
  public static Pair<String, List<String>> getCommand(final int pid) {
    final String cmdlinePath = String.format("/proc/%d/cmdline", pid);
    final byte[] cmdline;
    try {      
      cmdline = FileUtils.readFileToByteArray(new File(cmdlinePath));
    } catch (java.io.IOException ioe) {
      throw new RuntimeException(String.format(
        "Cannot read command line from %s$1: %s", cmdlinePath, ioe
      ));
    }
    final int
      sepIndex  = ByteUtil.indexOf(cmdline, new byte[]{ (byte) 0x0 }),
      argsIndex = (sepIndex == -1) ? cmdline.length: sepIndex + 1;
    final String nullSep = String.valueOf(Character.toChars((char) 0));
    final String[] args = new String(
      cmdline, argsIndex, cmdline.length - argsIndex, CHARSET
    ).split(nullSep);
    final String cmdName = new String(cmdline, 0, argsIndex, CHARSET);
    final Pair<String, List<String>> command
      = Pair.of(cmdName, Arrays.asList(args));
    return command;
  }
  
  public static Pair<String, List<String>> getCommand() {
    final int pid =org.d6r.PosixFileInputStream.getPid();
    if (pid != 0) return getCommand(pid);
    return Pair.of(
      "java", Arrays.asList(System.getProperty("java.vm.args").split(" "))
    );
  }
  
  public static int myPid() {
    if (!JRE) {
      return org.d6r.PosixFileInputStream.getPid();
    }
    return 0;
  }
  
  public static boolean isVerboseEnabled() {
    if (_verboseEnabled == null) {
      if (JRE &&
          Log.isLoggable(Log.SEV_VERBOSE) ||
          Boolean.getBoolean("VERBOSE") ||
          Boolean.getBoolean("DEBUG"))
      {
        return ((_verboseEnabled = Boolean.TRUE)).booleanValue();
      }
      
      final int pid = myPid();
      if (pid == 0) {
        return ((_verboseEnabled = Boolean.FALSE)).booleanValue();
      }
      
      final List<String> args = getCommand(pid).getValue();
      _verboseEnabled = Boolean.valueOf(
        args.contains("-v")            ||
        args.contains("-v")            ||
        args.contains("-debug")        ||
        args.contains("-verbose")      ||
        args.contains("--debug")       ||
        args.contains("--verbose")     ||
        args.contains("-verbose:class")||
        args.contains("-Ddebug")       ||
        args.contains("-Ddebug=true")  ||
        args.contains("-Dverbosetrue") ||
        args.contains("-h")            ||
        args.contains("--help")
      );
      if (_verboseEnabled == Boolean.FALSE) {
        final Iterator<String> it = args.iterator();
        while (it.hasNext() && _verboseEnabled == Boolean.FALSE) {
          final String arg = it.next();
          if (arg == null) continue;
          final String argLc = arg.toLowerCase();
          if (argLc.indexOf("verbose") != -1
          &&  argLc.indexOf("false")   == -1) {
            _verboseEnabled = Boolean.TRUE;
          } else {
            if (arg.indexOf("debug") != -1
            &&  arg.indexOf("false") == -1) {
              _verboseEnabled = Boolean.TRUE;
            }
          }
        } // while loop (arg: args)
      }
    }
    return _verboseEnabled.booleanValue();
  }
}