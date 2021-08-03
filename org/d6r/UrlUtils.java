package org.d6r;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Pattern;

import java.util.regex.Matcher;
import java.io.IOException;
import java.util.List;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.FileUtils;
//import com.android.dex.util.FileUtils;
//import android.os.FileUtils;

class UrlUtils {
  
    public static void main(String[] args)
    throws IOException, ClassNotFoundException 
    {
       if (args.length != 2) {
         System.err.println(
           "Usage: java -jar urlutils.jar URL HTML"
         );
         
         System.exit(10);
         return;
       }
     
       String html = encodeToSingleHtml(args[0], args[1]);
       System.out.println(html);
     
     /*Root root = Root.fromFile(args[0]);
      printHeaders(System.out);
      MemoryUsage baseline = MemoryUsage.baseline();
      for (LoadedClass loadedClass : root.loadedClasses.values()) {
         if (!loadedClass.systemClass) {
             continue;
         }
          printRow(System.out, baseline, loadedClass);
     }*/
    }
 
    public static String b64_encode(String data) {
      return b64_encode( data.getBytes() );
    }
 
    public static String b64_encode(byte[] data) {
     char[] tbl = {
         'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
         'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
         'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
         'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/' };
      StringBuilder buffer = new StringBuilder();
     int pad = 0;
     for (int i = 0; i < data.length; i += 3) {
          int b = ((data[i] & 0xFF) << 16) & 0xFFFFFF;
         if (i + 1 < data.length) {
             b |= (data[i+1] & 0xFF) << 8;
         } else {
             pad++;
         }
         if (i + 2 < data.length) {
             b |= (data[i+2] & 0xFF);
         } else {
             pad++;
         }
          for (int j = 0; j < 4 - pad; j++) {
             int c = (b & 0xFC0000) >> 18;
             buffer.append(tbl[c]);
             b <<= 6;
         }
     }
     for (int j = 0; j < pad; j++) {
         buffer.append("=");
     }
      return buffer.toString();
 }
 
 public static String b64_decodeToStr(String data) {
   return new String( b64_decode(data) );
 }
  public static byte[] b64_decode(String data)
 {
     int[] tbl = {
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54,
         55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2,
         3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
         20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30,
         31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
         48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
     byte[] bytes = data.getBytes();
     ByteArrayOutputStream buffer = new ByteArrayOutputStream();
     for (int i = 0; i < bytes.length; ) {
         int b = 0;
         if (tbl[bytes[i]] != -1) {
             b = (tbl[bytes[i]] & 0xFF) << 18;
         }
         // skip unknown characters
         else {
             i++;
             continue;
         }
          int num = 0;
         if (i + 1 < bytes.length && tbl[bytes[i+1]] != -1) {
             b = b | ((tbl[bytes[i+1]] & 0xFF) << 12);
             num++;
         }
         if (i + 2 < bytes.length && tbl[bytes[i+2]] != -1) {
             b = b | ((tbl[bytes[i+2]] & 0xFF) << 6);
             num++;
         }
         if (i + 3 < bytes.length && tbl[bytes[i+3]] != -1) {
             b = b | (tbl[bytes[i+3]] & 0xFF);
             num++;
         }
          while (num > 0) {
             int c = (b & 0xFF0000) >> 16;
             buffer.write((char)c);
             b <<= 8;
             num--;
         }
         i += 4;
     }
     return buffer.toByteArray();
 }



  public static String encodeToSingleHtml
  (String strUrl, String html)
  {
    Pattern imgRegex = Pattern.compile("<(img [^>]*?src=[\"']?)([^\\s\"'>]+)([\"']? ?[^>]*?)>", 2);
    Matcher imgMatcher = imgRegex.matcher(html);
    
    for (;;)
    {
      if (!imgMatcher.find())
      {
        String str2 = Pattern.compile("(<meta [^>]*?charset=[\"']?)([^>\\s\"']+)([^>]*?[\"']?.*?>)", 2).matcher(html).replaceFirst("$1UTF-8$3").replaceAll("<clipintent", "<");
        return str2;
      }
      if (imgMatcher.group(2).startsWith("data:")) continue;
      
      String str1 = getImageMimeType(imgMatcher.group(2));
      if (str1 == null) {
        continue;
      }
      try
      {
        URL url = new URL(
          new URL(strUrl), imgMatcher.group(2)
        );
        
        if (url == null) continue;
        
        StringBuilder sb = encodeBase64FromUrl(url);
        if (sb == null) {
          continue;
        }
        html = imgMatcher.replaceFirst("<clipintent" + imgMatcher.group(1) + "data:" + str1 + ";base64," + sb + imgMatcher.group(3) + ">");
        imgMatcher = imgRegex.matcher(html);
      }
      catch (MalformedURLException localMalformedURLException)
      {
        for (;;)
        {
          if ("true".equals(System.getProperty("printStackTrace"))) localMalformedURLException.printStackTrace();
          URL url = null;
        }
      }
    }
  }
  

  public static String encodeBase64(File file)
  throws IOException
  {
    int i = 67;
    
    StringBuilder sb1 = new StringBuilder(
      (int)(1.0D + 1.4D * file.length())
    );
    
    int j = 0;
    byte[] arrByte1 = new byte[3];
    FileInputStream inStream 
      = new FileInputStream(file);
    
    for (;;)
    {
      int k = inStream.read(arrByte1);
      if (k <= 0)
      {
        inStream.close();
        return sb1.toString();
      }
      byte[] arrByte2 = new byte[k];
      System.arraycopy(arrByte1, 0, arrByte2, 0, k);
      StringBuilder sbB64 = new StringBuilder(
        b64_encode(arrByte2)
      );
      if (i <= 0) {
        sb1.append(sbB64);
      } else {
        for (int m = 0; m < sbB64.length(); m++)
        {
          j++;
          sb1.append(sbB64.charAt(m));
          if (j >= i)
          {
            j = 0;
            sb1.append("\r\n");
          }
        }
      }
    }
  }
  
  public static String encodeBase64(byte[] pArrByte)
  {
    StringBuilder sb1 = new StringBuilder(
      b64_encode(pArrByte)
    );
    int i = 67;
    
    if (i <= 0) {
      return sb1.toString();
    }
    
    StringBuilder sb2 = new StringBuilder(
      sb1.length() + 2 * (sb1.length() / i)
    );
    
    int j = sb1.length();
    int k = 0;
    for (;;)
    {
      if (k >= j) {}
      for (;;)
      {
        
        if (k + i < j) {
          break;
        }
        sb2.append(sb1.substring(k));
      }
      sb2.append(sb1.substring(k, k + i));
      sb2.append("\r\n");
      k += i;
    }
    //return sb2.toString();
  }
  
  
  public static StringBuilder encodeBase64FromUrl
  (URL paramURL)
  {
    try
    {

      File f = FileUtils.toFile(paramURL);
      StringBuilder sb = new StringBuilder(
        b64_encode(
          FileUtils.readFileToString(f)
        )
      );
      return sb;
    }
    catch (IOException localIOException)
    {
      if ("true".equals(System.getProperty("printStackTrace"))) localIOException.printStackTrace();
    }
    return null;
  }
  
  
  public static String getImageMimeType(String paramString)
  {
    if (paramString.endsWith(".png")) {
      return "image/png";
    }
    if ((paramString.endsWith(".jpg")) || (paramString.endsWith(".jpeg"))) {
      return "image/jpeg";
    }
    if (paramString.endsWith(".gif")) {
      return "image/gif";
    }
    return null;
  }
  
}  
  