package org.d6r;

import java.io.*;
import java.util.*;
import bsh.Interpreter;
import bsh.CallStack;
import org.d6r.CollectionUtil;

public class ls
{
  public static void ls()
  {
    String str = System.getProperty("user.dir");
    File dir = new File(str);
    File[] entries = dir.listFiles();
    Arrays.sort(entries);
    String[] entryNames = new String[entries.length];
    int i = 0;
    for (int j = 0; j < entries.length; j++)
    {
      File entry = entries[j];
      entryNames[j ] = entry.getName();
    }
    CollectionUtil.print(entryNames);
    System.err.printf("total %d: %s\n\n", new Object[] { entries.length, str });
  }
  
  public static void ls(String path)
  {
    File dir = new File(path);
    File[] entries = dir.listFiles();
    Arrays.sort(entries);
    String[] entryNames = new String[entries.length];
    int i = 0;
    for (int j = 0; j < entries.length; j++)
    {
      File entry = entries[j];
      entryNames[j ] = entry.getName();
    }
    CollectionUtil.print(entryNames);
    System.err.printf("total %d: %s\n\n", new Object[] { entries.length, path });
  }
      
  public static void invoke(Interpreter env, CallStack stack,
  String... paths) 
  { 
    if (paths.length == 0) {
      ls(); 
      return;
    }
    for (String path: paths) {
      ls(path);
    }
  }
  
}

/* Location:           /storage/extSdCard/_projects/sdk/bsh/trunk/out
  * Qualified Name:     ls
  * Java Class Version: 6 (50.0)
  * JD-Core Version:    0.7.1
  */
 