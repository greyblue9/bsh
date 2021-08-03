package org.d6r.ssl;

import javax.net.ssl.SSLSocketFactory;

import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.HttpsURLConnection;
import org.d6r.Reflector;

public final class EnhancedSSLSocketFactory {
  
    public static final SSLSocketFactory INSTANCE
      = new StrongCipherSSLSocketFactory(
          // CertificatePinning.SSL_CONTEXT.getSocketFactory()
        );

    public EnhancedSSLSocketFactory() {
    }
    
    public static SSLSocketFactory getInstance() {
      return INSTANCE;
    }
}


/* compiled from: src */
class StrongCipherSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory mInnerSSocketFactory;

    public StrongCipherSSLSocketFactory(SSLSocketFactory sSLSocketFactory) {
        mInnerSSocketFactory = sSLSocketFactory;
    }

    public StrongCipherSSLSocketFactory() {
        this(HttpsURLConnection.getDefaultSSLSocketFactory());
    }

    public Socket createSocket(Socket socket, String str, int i, boolean z)
    {
      try {
        Socket createSocket
          = mInnerSSocketFactory.createSocket(socket, str, i, z);
        enforceStrongCipher(createSocket);
        return createSocket;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }

    public String[] getDefaultCipherSuites() {
        return mInnerSSocketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return mInnerSSocketFactory.getSupportedCipherSuites();
    }

    public Socket createSocket(String str, int i)
    {
      try {
        Socket createSocket
          = mInnerSSocketFactory.createSocket(str, i);
        enforceStrongCipher(createSocket);
        return createSocket;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }

    public Socket createSocket(InetAddress inetAddress, int i)
    {
      try {
        Socket createSocket
          = mInnerSSocketFactory.createSocket(inetAddress, i);
        enforceStrongCipher(createSocket);
        return createSocket;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }

    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) 
    {
      try {
        Socket createSocket
          = mInnerSSocketFactory.createSocket(str, i, inetAddress, i2);
        enforceStrongCipher(createSocket);
        return createSocket;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }

    public Socket createSocket(InetAddress inetAddress, int i,
    InetAddress inetAddress2, int i2)
    {
      try {
        Socket createSocket 
          = mInnerSSocketFactory.createSocket(inetAddress, i, inetAddress2, i2);
        enforceStrongCipher(createSocket);
        return createSocket;
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }

    private static final boolean ENABLED_ON_THIS_PLATFORM = true;
    private static final int MIN_ANDROID_VERSION = 10;
    
    public static void enforceStrongCipher(Socket socket) {
        if (socket instanceof SSLSocket) {
            setEnabledProtocols((SSLSocket) socket);
        } else {
            /*
            throw new SecurityException(String.format(
              "SSLSocket is required, %s is not supported",
              socket.getClass().getCanonicalName()
            ));
            */
        }
    }

    public static void setEnabledProtocols(SSLSocket sSLSocket) {
      try {
        if (ENABLED_ON_THIS_PLATFORM) {
            String[] supportedProtocols = sSLSocket.getSupportedProtocols();
            String[] obj = new String[supportedProtocols.length];
            int i = 0;
            for (String str : supportedProtocols) {
                if (str.equals("TLSv1") || 
                    str.equals("TLSv1.1") || 
                    str.equals("TLSv1.2"))
                 {
                    int i2 = i + 1;
                    obj[i] = str;
                    i = i2;
                }
            }
            
            if (i == 0) {
                // throw new SecurityException("Device does not support TLS");
            }
            String[] obj2 = new String[i];
            System.arraycopy(obj, 0, obj2, 0, i);
            sSLSocket.setEnabledProtocols(obj2);
        }
      } catch (Throwable e) {
        throw Reflector.Util.sneakyThrow(e);
      }
    }
}


