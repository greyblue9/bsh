package org.d6r;


public class NoSSLv3Factory
  implements javax.net.ssl.X509TrustManager, javax.net.ssl.HostnameVerifier
{
  
  public NoSSLv3Factory() {
    try {
      Reflect.setfldval(
        Reflect.getfldval(
          Reflector.invokeOrDefault(
            Class.forName(
              "javax.net.ssl.DefaultSSLServerSocketFactory",
              true,
              ClassLoader.getSystemClassLoader()
            ),
            "getDefault",
            new Object[0]
          ),
          "sslParameters"
        ),
        "enabledProtocols",
        new String[]{ "TLSv1" }
      );
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public boolean verify(String var0, javax.net.ssl.SSLSession var1) {
    return true;
  }
  
  @Override
  public void checkClientTrusted(java.security.cert.X509Certificate[] var0,
    String var1) throws java.security.cert.CertificateException
  {
  }
  
  @Override
  public void checkServerTrusted(java.security.cert.X509Certificate[] var0,
    String var1) throws java.security.cert.CertificateException
  {
  }
  
  @Override
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return null;
  }
  
}



