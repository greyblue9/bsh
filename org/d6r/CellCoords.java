package org.d6r;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
Get latitude and longitude from cellid, lac, MCC, MNC

I have an .asmx webservice that gets the cellid, MCC, MNC and LAC and I want to use opencellid to get the latitude and longitude from these information, I didn't find resources in the internet, so I don't kow where to start. if you can help me to find out how to do that, or give me an alternative solution to find latitude and longitude from these informations it would be nice. - Thanks

@see http://android-coding.blogspot.com/2011/06/convert-celllocation-to-real-location.html

@class class CellCoords

@example: Call the class to get the latitude and logitude
wherever you want, along with inputs and extra info,
or diagbostic info in the casenof error:


    Triple<int[], String[], double[]> result
      = CellCoords.toLatLng(MCC, MNC, lac, cellid);
    
    // -- optional
    int[] inputs = result.getLeft();
    String info
      = StringUtils.join(result.getMiddle(), "\n");
    System.err.println(info);    
    // -- end optional 
    
    double lat = result.getRight()[0];
    double lng = result.getRight()[1];
    // optional
    System.err.printf("coords:  %f, +f \n", lat, lng);


@author David Reilly AKA greyblue9
@since 2016
*/
public class CellCoords {
  

  public static String API_URL  
    = "http://www.google.com/glm/mmap";
  
  /**
   @param CID - The shortCID parameter follows heuristic
   experiences: 
   
     * Sometimes UMTS CIDs are build up from the original
       GSM CID (lower 4 hex digits), and
     * The RNC-ID are left shifted into the upper 4 
       digits. 
  */
  public static boolean shortCID = false;
  
  public static 
  byte[] postData(int MCC, int MNC, int LAC, int CID) {
    byte[] pd = new byte[] {
      0x00, 0x0e,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00,      
      0x00, 0x00,
      0x00, 0x00,
      0x1b,
      0x00, 0x00, 0x00, 0x00, // Offset 0x11
      0x00, 0x00, 0x00, 0x00, // Offset 0x15
      0x00, 0x00, 0x00, 0x00, // Offset 0x19
      0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, // Offset 0x1f
      0x00, 0x00, 0x00, 0x00, // Offset 0x23
      0x00, 0x00, 0x00, 0x00, // Offset 0x27
      0x00, 0x00, 0x00, 0x00, // Offset 0x2b
      (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
      0x00, 0x00, 0x00, 0x00    
    };
    boolean isUMTSCell = ((long) CID > 65535);
    // Attempt to resolve the cell using the GSM CID part
    
    if (shortCID) CID &= 0xFFFF;
    
    pd[0x1c] = (isUMTSCell)
             ? (byte) 5  // UTMS: 6 hex
             : (byte) 3; // GSM: 4 hex digits
    
    pd[0x11] = (byte)((MNC >> 24) & 0xFF);
    pd[0x12] = (byte)((MNC >> 16) & 0xFF);
    pd[0x13] = (byte)((MNC >> 8) & 0xFF);
    pd[0x14] = (byte)((MNC >> 0) & 0xFF);
    pd[0x15] = (byte)((MCC >> 24) & 0xFF);
    pd[0x16] = (byte)((MCC >> 16) & 0xFF);
    pd[0x17] = (byte)((MCC >> 8) & 0xFF);
    pd[0x18] = (byte)((MCC >> 0) & 0xFF);
    pd[0x27] = (byte)((MNC >> 24) & 0xFF);
    pd[0x28] = (byte)((MNC >> 16) & 0xFF);
    pd[0x29] = (byte)((MNC >> 8) & 0xFF);
    pd[0x2a] = (byte)((MNC >> 0) & 0xFF);
    pd[0x2b] = (byte)((MCC >> 24) & 0xFF);
    pd[0x2c] = (byte)((MCC >> 16) & 0xFF);
    pd[0x2d] = (byte)((MCC >> 8) & 0xFF);
    pd[0x2e] = (byte)((MCC >> 0) & 0xFF);
    pd[0x1f] = (byte)((CID >> 24) & 0xFF);
    pd[0x20] = (byte)((CID >> 16) & 0xFF);
    pd[0x21] = (byte)((CID >> 8) & 0xFF);
    pd[0x22] = (byte)((CID >> 0) & 0xFF);
    pd[0x23] = (byte)((LAC >> 24) & 0xFF);
    pd[0x24] = (byte)((LAC >> 16) & 0xFF);
    pd[0x25] = (byte)((LAC >> 8) & 0xFF);
    pd[0x26] = (byte)((LAC >> 0) & 0xFF);
    return pd;
  }
  
  public static Triple<int[], String[], double[]>
  toLatLng(int MCC, int MNC, int LAC, int CID) 
  {
    int[]    input  = new int[] { MCC, MNC, LAC, CID };
    String[] info   = new String[0];
    double[] latlng = new double[] { -1.0, -1.0 };
    
    byte[] pd = postData(MCC, MNC, LAC, CID);
    byte[] ps = new byte[0];
    InputStream respStream = null;
    OutputStream postStream = null;
    String responseText = "";
    int statusCode = -1;
    String statusText = "(Mot started sending.)";
    HttpURLConnection conn = null;
    try {
      try {
        URL url = new URL(API_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(pd.length);
        Reflect.setfldval(conn, 
          "contentType", "application/binary"
        );
        postStream = conn.getOutputStream();
        postStream.write(pd, 0, pd.length);
        postStream.flush();
        
        respStream = conn.getInputStream();
        ps  = IOUtils.toByteArray(respStream);
        responseText = new String(ps, "UTF-8");
        //respStream.flush();
        statusCode = conn.getResponseCode();      
        statusText = conn.getResponseMessage();
      } finally {
        IOUtils.closeQuietly(postStream);
        IOUtils.closeQuietly(respStream);
        conn = null;
      }
      /*
        HttpWebResponse res 
          = (HttpWebResponse)req.GetResponse();
        byte[] ps = new byte[res.ContentLength];
        int totalBytesRead = 0;
        while (totalBytesRead < ps.Length) {
          totalBytesRead += res.GetResponseStream().Read(
            ps, totalBytesRead, 
            ps.Length - totalBytesRead
          );
        }
       */
      if (statusCode != 200) { // HTTP/1.x 200 OK
        info = new String[]{ String.format(
          "HTTP error %d", statusCode
        ), statusText, new String(ps,"UTF-8") };
        return Triple.of(input, info, latlng);
      }    
      // .. HTTP 200 OK ..
        
         short opcode1 =
        (short)(
                 (ps[0] <<  8)
               | (ps[1] <<  0) 
        );
        byte  opcode2 =
        (byte)(
                 (ps[2] <<  0)
        );
        int  ret_code =
        (int) (
                 (ps[3] << 24)
              |  (ps[4] << 16)
              |  (ps[5] <<  8)
              |  (ps[6] <<  0)
        );
        if (ret_code != 0) {
          info = new String[] { 
            String.format("ret_code != 0: %d", ret_code),
            statusText,
            new String(ps, "UTF-8") 
          };
          return Triple.of(input, info, latlng);
        }
        // ret_code == 0
        
        double lat = ((double) (
              (ps[ 7] << 24)
            | (ps[ 8] << 16)
            | (ps[ 9] <<  8)
            | (ps[10] <<  0)
        )) / 1000000D;
  
        double lon = ((double) (
              (ps[11] << 24)
            | (ps[12] << 16)
            | (ps[13] <<  8)
            | (ps[14] <<  0)
        )) / 1000000D;
  
      latlng[0] = lat;
      latlng[1] = lon;     
        
    } catch (Exception e2) {
      if ("true".equals(System.getProperty("printStackTrace"))) e2.printStackTrace();
      info = new String[] { 
        String.format(
          "[HTTP %d] %s: %s", statusCode, statusText
        ), 
        String.format(
          "%s: %s (at %s ...)",
          e2.getClass().getSimpleName(), 
          e2.getMessage(),
          StringUtils.join(
            ExceptionUtils.getRootCauseStackTrace(e2),
            "\n"
          )
        ),
        responseText
      };
    }
    return Triple.of(input, info, latlng);
  }
  
}