package bsh.servlet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SimpleTemplate {
  StringBuffer buff;
  static String NO_TEMPLATE = "NO_TEMPLATE";
  static Map<String, String> templateData = new HashMap();
  static boolean cacheTemplates = true;

  public static SimpleTemplate getTemplate(String file) {
    String templateText = (String)templateData.get(file);
    if(templateText != null && cacheTemplates) {
      if(templateText.equals(NO_TEMPLATE)) {
        return null;
      }
    } else {
      try {
        FileReader e = new FileReader(file);
        templateText = getStringFromStream((Reader)e);
        templateData.put(file, templateText);
      } catch (IOException var3) {
        templateData.put(file, NO_TEMPLATE);
      }
    }

    return templateText == null?null:new SimpleTemplate(templateText);
  }

  public static String getStringFromStream(InputStream ins) throws IOException {
    return getStringFromStream((Reader)(new InputStreamReader(ins)));
  }

  public static String getStringFromStream(Reader reader) throws IOException {
    StringBuffer sb = new StringBuffer();
    BufferedReader br = new BufferedReader(reader);

    String line;
    while((line = br.readLine()) != null) {
      sb.append(line + "\n");
    }

    return sb.toString();
  }

  public SimpleTemplate(String template) {
    this.init(template);
  }

  public SimpleTemplate(Reader reader) throws IOException {
    String template = getStringFromStream(reader);
    this.init(template);
  }

  public SimpleTemplate(URL url) throws IOException {
    String template = getStringFromStream(url.openStream());
    this.init(template);
  }

  private void init(String s) {
    this.buff = new StringBuffer(s);
  }

  public void replace(String param, String value) {
    int[] range;
    while((range = this.findTemplate(param)) != null) {
      this.buff.replace(range[0], range[1], value);
    }

  }

  int[] findTemplate(String name) {
    String text = this.buff.toString();
    int len = text.length();
    int start = 0;

    while(start < len) {
      int cstart = text.indexOf("<!--", start);
      if(cstart == -1) {
        return null;
      }

      int cend = text.indexOf("-->", cstart);
      if(cend == -1) {
        return null;
      }

      cend += "-->".length();
      int tstart = text.indexOf("TEMPLATE-", cstart);
      if(tstart == -1) {
        start = cend;
      } else if(tstart > cend) {
        start = cend;
      } else {
        int pstart = tstart + "TEMPLATE-".length();

        int pend;
        for(pend = pstart; pend < len; ++pend) {
          char param = text.charAt(pend);
          if(param == 32 || param == 9 || param == 45) {
            break;
          }
        }

        if(pend >= len) {
          return null;
        }

        String var11 = text.substring(pstart, pend);
        if(var11.equals(name)) {
          return new int[]{cstart, cend};
        }

        start = cend;
      }
    }

    return null;
  }

  public String toString() {
    return this.buff.toString();
  }

  public void write(PrintWriter out) {
    out.println(this.toString());
  }

  public void write(PrintStream out) {
    out.println(this.toString());
  }

  public static void main(String[] args) throws IOException {
    String filename = args[0];
    String param = args[1];
    String value = args[2];
    FileReader fr = new FileReader(filename);
    String templateText = getStringFromStream((Reader)fr);
    SimpleTemplate template = new SimpleTemplate(templateText);
    template.replace(param, value);
    template.write(System.out);
  }

  public static void setCacheTemplates(boolean b) {
    cacheTemplates = b;
  }
}
