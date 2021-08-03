package bsh;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;

public class Remote {
  public static void main(String[] args) throws Exception {
    if(args.length < 2) {
      System.out.println("usage: Remote URL(http|bsh) file [ file ] ... ");
      System.exit(1);
    }

    String url = args[0];
    String text = getFile(args[1]);
    int ret = eval(url, text);
    System.exit(ret);
  }

  public static int eval(String url, String text) throws IOException {
    String returnValue = null;
    if(url.startsWith("http:")) {
      returnValue = doHttp(url, text);
    } else {
      if(!url.startsWith("bsh:")) {
        throw new IOException("Unrecognized URL type.Scheme must be http:// or bsh://");
      }

      returnValue = doBsh(url, text);
    }

    try {
      return Integer.parseInt(returnValue);
    } catch (Exception var4) {
      return 0;
    }
  }

  static String doBsh(String url, String text) {
    String host = "";
    String port = "";
    String returnValue = "-1";

    try {
      url = url.substring(6);
      int ex = url.indexOf(":");
      host = url.substring(0, ex);
      port = url.substring(ex + 1, url.length());
    } catch (Exception var11) {
      System.err.println("Bad URL: " + url + ": " + var11);
      return returnValue;
    }

    try {
      System.out.println("Connecting to host : " + host + " at port : " + port);
      Socket ex1 = new Socket(host, Integer.parseInt(port) + 1);
      OutputStream out = ex1.getOutputStream();
      InputStream in = ex1.getInputStream();
      sendLine(text, out);
      BufferedReader bin = new BufferedReader(new InputStreamReader(in));

      String line;
      while((line = bin.readLine()) != null) {
        System.out.println(line);
      }

      returnValue = "1";
      return returnValue;
    } catch (Exception var12) {
      System.err.println("Error communicating with server: " + var12);
      return returnValue;
    }
  }

  private static void sendLine(String line, OutputStream outPipe) throws IOException {
    outPipe.write(line.getBytes());
    outPipe.flush();
  }

  static String doHttp(String postURL, String text) {
    String returnValue = null;
    StringBuilder sb = new StringBuilder();
    sb.append("bsh.client=Remote");
    sb.append("&bsh.script=");
    sb.append(URLEncoder.encode(text));
    String formData = sb.toString();

    try {
      URL e2 = new URL(postURL);
      HttpURLConnection urlcon = (HttpURLConnection)e2.openConnection();
      urlcon.setRequestMethod("POST");
      urlcon.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
      urlcon.setDoOutput(true);
      urlcon.setDoInput(true);
      PrintWriter pout = new PrintWriter(new OutputStreamWriter(urlcon.getOutputStream(), "8859_1"), true);
      pout.print(formData);
      pout.flush();
      int rc = urlcon.getResponseCode();
      if(rc != 200) {
        System.out.println("Error, HTTP response: " + rc);
      }

      returnValue = urlcon.getHeaderField("Bsh-Return");
      BufferedReader bin = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));

      String line;
      while((line = bin.readLine()) != null) {
        System.out.println(line);
      }

      System.out.println("Return Value: " + returnValue);
    } catch (MalformedURLException var11) {
      System.out.println(var11);
    } catch (IOException var12) {
      System.out.println(var12);
    }

    return returnValue;
  }

  static String getFile(String name) throws FileNotFoundException, IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader bin = new BufferedReader(new FileReader(name));

    String line;
    while((line = bin.readLine()) != null) {
      sb.append(line).append("\n");
    }

    return sb.toString();
  }
}
