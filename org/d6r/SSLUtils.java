/**
Copyright 2016 The Netty Project
The Netty Project licenses this file to you under the Apache License,
version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
*/
package org.d6r;

import java.nio.charset.StandardCharsets;
import static java.lang.Math.min;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateException;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.OpenSSLX509Certificate;
import com.android.org.conscrypt.OpenSSLX509CertificateFactory;

/**
Utility methods for SSL packet processing. Copied from the Netty project.

This is a public class to allow testing to occur on Android via CTS.
*/
public final class SSLUtils {
  
  public static final String SSL3_TXT_ADH_DES_196_CBC_SHA = "ADH-DES-CBC3-SHA";
  public static final String SSL3_TXT_ADH_DES_40_CBC_SHA = "EXP-ADH-DES-CBC-SHA";
  public static final String SSL3_TXT_ADH_DES_64_CBC_SHA = "ADH-DES-CBC-SHA";
  public static final String SSL3_TXT_ADH_RC4_128_MD5 = "ADH-RC4-MD5";
  public static final String SSL3_TXT_ADH_RC4_40_MD5 = "EXP-ADH-RC4-MD5";
  public static final String SSL3_TXT_DH_DSS_DES_192_CBC3_SHA = "DH-DSS-DES-CBC3-SHA";
  public static final String SSL3_TXT_DH_DSS_DES_40_CBC_SHA = "EXP-DH-DSS-DES-CBC-SHA";
  public static final String SSL3_TXT_DH_DSS_DES_64_CBC_SHA = "DH-DSS-DES-CBC-SHA";
  public static final String SSL3_TXT_DH_RSA_DES_192_CBC3_SHA = "DH-RSA-DES-CBC3-SHA";
  public static final String SSL3_TXT_DH_RSA_DES_40_CBC_SHA = "EXP-DH-RSA-DES-CBC-SHA";
  public static final String SSL3_TXT_DH_RSA_DES_64_CBC_SHA = "DH-RSA-DES-CBC-SHA";
  public static final String SSL3_TXT_EDH_DSS_DES_192_CBC3_SHA = "EDH-DSS-DES-CBC3-SHA";
  public static final String SSL3_TXT_EDH_DSS_DES_40_CBC_SHA = "EXP-EDH-DSS-DES-CBC-SHA";
  public static final String SSL3_TXT_EDH_DSS_DES_64_CBC_SHA = "EDH-DSS-DES-CBC-SHA";
  public static final String SSL3_TXT_EDH_RSA_DES_192_CBC3_SHA = "EDH-RSA-DES-CBC3-SHA";
  public static final String SSL3_TXT_EDH_RSA_DES_40_CBC_SHA = "EXP-EDH-RSA-DES-CBC";
  public static final String SSL3_TXT_EDH_RSA_DES_64_CBC_SHA = "EDH-RSA-DES-CBC-SHA";
  public static final String SSL3_TXT_FZA_DMS_FZA_SHA = "FZA-FZA-CBC-SHA";
  public static final String SSL3_TXT_FZA_DMS_NULL_SHA = "FZA-NULL-SHA";
  public static final String SSL3_TXT_FZA_DMS_RC4_SHA = "FZA-RC4-SHA";
  public static final String SSL3_TXT_RSA_DES_192_CBC3_SHA = "DES-CBC3-SHA";
  public static final String SSL3_TXT_RSA_DES_40_CBC_SHA = "EXP-DES-CBC-SHA";
  public static final String SSL3_TXT_RSA_DES_64_CBC_SHA = "DES-CBC-SHA";
  public static final String SSL3_TXT_RSA_IDEA_128_SHA = "IDEA-CBC-MD5";
  public static final String SSL3_TXT_RSA_NULL_MD5 = "NULL-MD5";
  public static final String SSL3_TXT_RSA_NULL_SHA = "NULL-SHA";
  public static final String SSL3_TXT_RSA_RC2_40_MD5 = "EXP-RC2-CBC-MD5";
  public static final String SSL3_TXT_RSA_RC4_128_MD5 = "RC4-MD5";
  public static final String SSL3_TXT_RSA_RC4_128_SHA = "RC4-SHA";
  public static final String SSL3_TXT_RSA_RC4_40_MD5 = "EXP-RC4-MD5";
  public static final byte[] SSL3_MD_CLIENT_FINISHED_CONST = { 0x43,0x4C,0x4E,0x54 };
  public static final byte[] SSL3_MD_SERVER_FINISHED_CONST = { 0x53,0x52,0x56,0x52 };
  public static final int SSL3_AD_BAD_CERTIFICATE = 42;
  public static final int SSL3_AD_BAD_RECORD_MAC = 20; 	// fatal
  public static final int SSL3_AD_CERTIFICATE_EXPIRED = 45;
  public static final int SSL3_AD_CERTIFICATE_REVOKED = 44;
  public static final int SSL3_AD_CERTIFICATE_UNKNOWN = 46;
  public static final int SSL3_AD_CLOSE_NOTIFY = 0;
  public static final int SSL3_AD_DECOMPRESSION_FAILURE = 30; 	// fatal
  public static final int SSL3_AD_HANDSHAKE_FAILURE = 40; 	// fatal
  public static final int SSL3_AD_ILLEGAL_PARAMETER = 47; 	// fatal
  public static final int SSL3_AD_NO_CERTIFICATE = 41;
  public static final int SSL3_AD_UNEXPECTED_MESSAGE = 10; 	// fatal
  public static final int SSL3_AD_UNSUPPORTED_CERTIFICATE = 43;
  public static final int SSL3_AL_FATAL = 2;
  public static final int SSL3_AL_WARNING = 1;
  public static final int SSL3_CK_ADH_DES_196_CBC_SHA = 0x0300001B;
  public static final int SSL3_CK_ADH_DES_40_CBC_SHA = 0x03000019;
  public static final int SSL3_CK_ADH_DES_64_CBC_SHA = 0x0300001A;
  public static final int SSL3_CK_ADH_RC4_128_MD5 = 0x03000018;
  public static final int SSL3_CK_ADH_RC4_40_MD5 = 0x03000017;
  public static final int SSL3_CK_DH_DSS_DES_192_CBC3_SHA = 0x0300000D;
  public static final int SSL3_CK_DH_DSS_DES_40_CBC_SHA = 0x0300000B;
  public static final int SSL3_CK_DH_DSS_DES_64_CBC_SHA = 0x0300000C;
  public static final int SSL3_CK_DH_RSA_DES_192_CBC3_SHA = 0x03000010;
  public static final int SSL3_CK_DH_RSA_DES_40_CBC_SHA = 0x0300000E;
  public static final int SSL3_CK_DH_RSA_DES_64_CBC_SHA = 0x0300000F;
  public static final int SSL3_CK_EDH_DSS_DES_192_CBC3_SHA = 0x03000013;
  public static final int SSL3_CK_EDH_DSS_DES_40_CBC_SHA = 0x03000011;
  public static final int SSL3_CK_EDH_DSS_DES_64_CBC_SHA = 0x03000012;
  public static final int SSL3_CK_EDH_RSA_DES_192_CBC3_SHA = 0x03000016;
  public static final int SSL3_CK_EDH_RSA_DES_40_CBC_SHA = 0x03000014;
  public static final int SSL3_CK_EDH_RSA_DES_64_CBC_SHA = 0x03000015;
  public static final int SSL3_CK_FZA_DMS_FZA_SHA = 0x0300001D;
  public static final int SSL3_CK_FZA_DMS_NULL_SHA = 0x0300001C;
  public static final int SSL3_CK_FZA_DMS_RC4_SHA = 0x0300001E;
  public static final int SSL3_CK_RSA_DES_192_CBC3_SHA = 0x0300000A;
  public static final int SSL3_CK_RSA_DES_40_CBC_SHA = 0x03000008;
  public static final int SSL3_CK_RSA_DES_64_CBC_SHA = 0x03000009;
  public static final int SSL3_CK_RSA_IDEA_128_SHA = 0x03000007;
  public static final int SSL3_CK_RSA_NULL_MD5 = 0x03000001;
  public static final int SSL3_CK_RSA_NULL_SHA = 0x03000002;
  public static final int SSL3_CK_RSA_RC2_40_MD5 = 0x03000006;
  public static final int SSL3_CK_RSA_RC4_128_MD5 = 0x03000004;
  public static final int SSL3_CK_RSA_RC4_128_SHA = 0x03000005;
  public static final int SSL3_CK_RSA_RC4_40_MD5 = 0x03000003;
  public static final int SSL3_MASTER_SECRET_SIZE = 48;
  public static final int SSL3_MAX_SSL_SESSION_ID_LENGTH = 32;
  public static final int SSL3_RANDOM_SIZE = 32;
  public static final int SSL3_RS_BLANK = 1;
  public static final int SSL3_RS_DATA = 0; // ?
  public static final int SSL3_RS_ENCODED = 2;
  public static final int SSL3_RS_PART_READ = 4;
  public static final int SSL3_RS_PART_WRITE = 5;
  public static final int SSL3_RS_PLAIN	= 3;
  public static final int SSL3_RS_READ_MORE = 3;
  public static final int SSL3_RS_WRITE_MORE = 0; // ?
  public static final int SSL3_RT_ALERT = 21;
  public static final int SSL3_RT_APPLICATION_DATA = 23;
  public static final int SSL3_RT_CHANGE_CIPHER_SPEC = 20;
  public static final int SSL3_RT_HANDSHAKE = 22;
  public static final int SSL3_RT_HEADER_LENGTH = 5;
  public static final int SSL3_RT_MAX_DATA_SIZE = (1024*1024);
  public static final int SSL3_RT_MAX_EXTRA = (16384);
  // public static final int SSL3_RT_MAX_EXTRA = (14000);


  public static final int SSL3_RT_MAX_PLAIN_LENGTH = 16384;
  
  public static final int SSL3_RT_MAX_COMPRESSED_LENGTH = (1024+SSL3_RT_MAX_PLAIN_LENGTH);
  public static final int SSL3_RT_MAX_ENCRYPTED_LENGTH = (1024+SSL3_RT_MAX_COMPRESSED_LENGTH);
  public static final int SSL3_RT_MAX_PACKET_SIZE = (SSL3_RT_MAX_ENCRYPTED_LENGTH+SSL3_RT_HEADER_LENGTH);
  
  public static final int SSL3_SESSION_ID_SIZE = 32;
  public static final int SSL3_SSL_SESSION_ID_LENGTH = 32;
  public static final int SSL3_VERSION = 0x0300;
  public static final int SSL3_VERSION_MAJOR = 0x03;
  public static final int SSL3_VERSION_MINOR = 0x00;
  
  public static final byte TLS_CT_DSS_FIXED_DH = 4;
  public static final byte TLS_CT_DSS_SIGN = 2;
  public static final byte TLS_CT_ECDSA_FIXED_ECDH = 66;
  public static final byte TLS_CT_ECDSA_SIGN = 64;
  public static final byte TLS_CT_RSA_FIXED_DH = 3;
  public static final byte TLS_CT_RSA_FIXED_ECDH = 65;
  public static final byte TLS_CT_RSA_SIGN = 1;
  
  static final boolean USE_ENGINE_SOCKET_BY_DEFAULT = Boolean.parseBoolean(
    System.getProperty(
      "com.android.org.conscrypt.useEngineSocketByDefault", "false"
    )
  );

  private static final int MAX_PROTOCOL_LENGTH = 255;
  private static final Charset US_ASCII = StandardCharsets.US_ASCII;
  
  // TODO(nathanmittler): Should these be in NativeConstants?
  public static enum SessionType {

    /**
    Identifies OpenSSL sessions.
    */
    OPEN_SSL(1), /**
    Identifies OpenSSL sessions with OCSP stapled data.
    */
    OPEN_SSL_WITH_OCSP(2), /**
    Identifies OpenSSL sessions with TLS SCT data.
    */
    OPEN_SSL_WITH_TLS_SCT(3);

    SessionType(int value) {
      this.value = value;
    }

    static boolean isSupportedType(int type) {
      return type == OPEN_SSL.value
          || type == OPEN_SSL_WITH_OCSP.value
          || type == OPEN_SSL_WITH_TLS_SCT.value;
    }

    final int value;
  }

  /**
  States for SSL engines.
  */
  public static final class EngineStates {

    private EngineStates() {
    }

    /**
    The engine is constructed, but the initial handshake hasn't been started
    */
    static final int STATE_NEW = 0;

    /**
    The client/server mode of the engine has been set.
    */
    static final int STATE_MODE_SET = 1;

    /**
    The handshake has been started
    */
    static final int STATE_HANDSHAKE_STARTED = 2;

    /**
    Listeners of the handshake have been notified of completion but the handshake call
    hasn't returned.
    */
    static final int STATE_HANDSHAKE_COMPLETED = 3;

    /**
    The handshake call returned but the listeners have not yet been notified.
    This is expected behaviour in cut-through mode, where SSL_do_handshake returns 
    before the handshake is
    complete. We can now start writing data to the socket.
    */
    static final int STATE_READY_HANDSHAKE_CUT_THROUGH = 4;

    /**
    The handshake call has returned and the listeners have been notified.
    Ready to begin writing data.
    */
    static final int STATE_READY = 5;

    /**
    The inbound direction of the engine has been closed.
    */
    static final int STATE_CLOSED_INBOUND = 6;

    /**
    The outbound direction of the engine has been closed.
    */
    static final int STATE_CLOSED_OUTBOUND = 7;

    /**
    The engine has been closed.
    */
    static final int STATE_CLOSED = 8;
  }

  /**
  This is the maximum overhead when encrypting plaintext as defined by
  <a href="https://www.ietf.org/rfc/rfc5246.txt">rfc5264,
  <a href="https://www.ietf.org/rfc/rfc5289.txt">rfc5289and openssl implementation itself.
  Please note that we use a padding of 16 here as openssl uses PKC#5 which uses 16 bytes
  whilethe spec itself allow up to 255 bytes. 16 bytes is the max for PKC#5 (which handles it
  the same way as PKC#7) as we use a block size of 16. See <a
  href="https://tools.ietf.org/html/rfc5652#section-6.3">rfc5652#section-6.3.
  16 (IV) + 48 (MAC) + 1 (Padding_length field) + 15 (Padding) + 1 (ContentType) + 2
  (ProtocolVersion) + 2 (Length)
  TODO: We may need to review this calculation once TLS 1.3 becomes available.
  */
  private static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 15 + 48 + 1 + 16 + 1 + 2 + 2;

  private static final int MAX_ENCRYPTION_OVERHEAD_DIFF = Integer.MAX_VALUE - MAX_ENCRYPTION_OVERHEAD_LENGTH;

  /**
  Key type: RSA certificate. 
  */
  private static final String KEY_TYPE_RSA = "RSA";

  /**
  Key type: Elliptic Curve certificate. 
  */
  private static final String KEY_TYPE_EC = "EC";
  
  /**
  Key type: Diffie-Hellman
  */
  private static final String KEY_TYPE_DH_DSS = "DH_DSS";
  private static final String KEY_TYPE_NULL = "NULL";
  
  static X509Certificate[] decodeX509CertificateChain(byte[][] certChain) throws java.security.cert.CertificateException {
    CertificateFactory certificateFactory = getCertificateFactory();
    int numCerts = certChain.length;
    X509Certificate[] decodedCerts = new X509Certificate[numCerts];
    for (int i = 0; i < numCerts; i++) {
      decodedCerts[i] = decodeX509Certificate(certificateFactory, certChain[i]);
    }
    return decodedCerts;
  }

  private static CertificateFactory getCertificateFactory() {
    try {
      return CertificateFactory.getInstance("X.509");
    } catch (java.security.cert.CertificateException e) {
      return null;
    }
  }

  private static X509Certificate decodeX509Certificate(CertificateFactory certificateFactory, byte[] bytes) throws java.security.cert.CertificateException {
    if (certificateFactory != null) {
      return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(bytes));
    }
    return OpenSSLX509Certificate.fromX509Der(bytes);
  }
  
  /**
  SSL_CIPHER_get_kx_name returns a string that describes the key-exchange method 
  used by cipher. For example, "ECDHE_ECDSA". TLS 1.3 AEAD-only ciphers return 
  the string "GENERIC".
  
  OPENSSL_EXPORT const char *SSL_CIPHER_get_kx_name(const SSL_CIPHER *cipher);
  */
  
  /**
  Returns key type constant suitable for calling X509KeyManager.chooseServerAlias or
  X509ExtendedKeyManager.chooseEngineServerAlias. Returns `null' for key exchanges 
  that do not use X.509 for server authentication.
  */
  static String getServerX509KeyType(long sslCipherNative) throws SSLException {
    // String kx_name = NativeCrypto.SSL_CIPHER_get_kx_name(sslCipherNative);
    // TODO: FIXME
    String kx_name = NativeCrypto.SSL_SESSION_cipher(sslCipherNative);
    if (false) throw new SSLException("impossible");
    
    if (kx_name == null
    ||  kx_name.indexOf("NULL") != -1)
    {
      return KEY_TYPE_NULL;
    } else if (
        kx_name.equals("RSA")
    ||  kx_name.equals("DHE_RSA")
    ||  kx_name.equals("ECDHE_RSA"))
    {
      return KEY_TYPE_RSA;
    } else if (kx_name.equals("ECDHE_ECDSA")) {
      return KEY_TYPE_EC;
    } else if (
        kx_name.indexOf("DSS") != -1
    ||  kx_name.indexOf("DH")  != -1) {
      return KEY_TYPE_DH_DSS;
    } else {
      System.err.printf(
        "[WARN] getServerX509KeyType: Unknown key type: %s\n", kx_name
      );
      return null;
    }
  }
  
  /**
  Similar to getServerKeyType, but returns value given TLS
  ClientCertificateType byte values from a CertificateRequest
  message for use with X509KeyManager.chooseClientAlias or
  X509ExtendedKeyManager.chooseEngineClientAlias.
  
  Visible for testing.
  */
  static String getClientKeyType(byte clientCertificateType) {
    // See also http://www.ietf.org/assignments/tls-parameters/tls-parameters.xml
    switch((clientCertificateType)) {
      case TLS_CT_RSA_SIGN:
      case TLS_CT_RSA_FIXED_DH:
      case TLS_CT_RSA_FIXED_ECDH:
        return KEY_TYPE_RSA; // RFC rsa_sign
      case TLS_CT_ECDSA_SIGN:
      case TLS_CT_ECDSA_FIXED_ECDH:      
        return KEY_TYPE_EC; // RFC ecdsa_sign
      case TLS_CT_DSS_SIGN:
      case TLS_CT_DSS_FIXED_DH:
        return KEY_TYPE_DH_DSS;
      case 0:
        return KEY_TYPE_NULL;
      
      default:
        throw new RuntimeException(String.format(
          "unknown client key type: %1$d (0x%1$02x)",
          clientCertificateType
        ));
    }
  }

  /**
  Gets the supported key types for client certificates based on the
  `ClientCertificateType' values provided by the server.
  @param clientCertificateTypes `ClientCertificateType' values provided by the server.
  See https://www.ietf.org/assignments/tls-parameters/tls-parameters.xml.
  @return supported key types that can be used in `X509KeyManager.chooseClientAlias' and
  `X509ExtendedKeyManager.chooseEngineClientAlias'.
  Visible for testing.
  */
  static Set<String> getSupportedClientKeyTypes(byte[] clientCertificateTypes) {
    Set<String> result = new HashSet<String>(clientCertificateTypes.length);
    for (byte keyTypeCode : clientCertificateTypes) {
      String keyType = SSLUtils.getClientKeyType(keyTypeCode);
      if (keyType == null) {
        // Unsupported client key type -- ignore
        continue;
      }
      result.add(keyType);
    }
    return result;
  }

  static byte[][] encodeIssuerX509Principals(X509Certificate[] certificates) throws CertificateEncodingException {
    byte[][] principalBytes = new byte[certificates.length][];
    for (int i = 0; i < certificates.length; i++) {
      principalBytes[i] = certificates[i].getIssuerX500Principal().getEncoded();
    }
    return principalBytes;
  }

  /**
  Converts the peer certificates into a cert chain.
  */
  static javax.security.cert.X509Certificate[] toCertificateChain(X509Certificate[] certificates) throws SSLPeerUnverifiedException {
    try {
      javax.security.cert.X509Certificate[] chain = new javax.security.cert.X509Certificate[certificates.length];
      for (int i = 0; i < certificates.length; i++) {
        byte[] encoded = certificates[i].getEncoded();
        chain[i] = javax.security.cert.X509Certificate.getInstance(encoded);
      }
      return chain;
    } catch (CertificateEncodingException e) {
      SSLPeerUnverifiedException exception = new SSLPeerUnverifiedException(e.getMessage());
      exception.initCause(exception);
      throw exception;
    } catch (CertificateException e) {
      SSLPeerUnverifiedException exception = new SSLPeerUnverifiedException(e.getMessage());
      exception.initCause(exception);
      throw exception;
    }
  }

  /**
  Calculates the minimum bytes required in the encrypted output buffer for the given number of
  plaintext source bytes.
  */
  static int calculateOutNetBufSize(int pendingBytes) {
    return min(SSL3_RT_MAX_PACKET_SIZE, MAX_ENCRYPTION_OVERHEAD_LENGTH + min(MAX_ENCRYPTION_OVERHEAD_DIFF, pendingBytes));
  }

  /**
  Wraps the given exception if it's not already a `SSLHandshakeException'.
  */
  static SSLHandshakeException toSSLHandshakeException(Throwable e) {
    if (e instanceof SSLHandshakeException) {
      return (SSLHandshakeException) e;
    }
    return (SSLHandshakeException) new SSLHandshakeException(e.getMessage()).initCause(e);
  }

  /**
  Wraps the given exception if it's not already a `SSLException'.
  */
  static SSLException toSSLException(Throwable e) {
    if (e instanceof SSLException) {
      return (SSLException) e;
    }
    return new SSLException(e);
  }

  static String toProtocolString(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    return new String(bytes, US_ASCII);
  }

  static byte[] toProtocolBytes(String protocol) {
    if (protocol == null) {
      return null;
    }
    return protocol.getBytes(US_ASCII);
  }

  /**
  Decodes the given list of protocols into `String's.
  @param protocols the encoded protocol list
  @return the decoded protocols or `EmptyArray#BYTE' if `protocols' is
  empty.
  @throws NullPointerException if protocols is `null'.
  */
  static String[] decodeProtocols(byte[] protocols) {
    if (protocols.length == 0) {
      return EmptyArray.STRING;
    }
    int numProtocols = 0;
    for (int i = 0; i < protocols.length; ) {
      int protocolLength = protocols[i];
      if (protocolLength < 0 || protocolLength > protocols.length - i) {
        throw new IllegalArgumentException("Protocol has invalid length (" + protocolLength + " at position " + i + "): " + (protocols.length < 50 ? Arrays.toString(protocols) : protocols.length + " byte array"));
      }
      numProtocols++;
      i += 1 + protocolLength;
    }
    String[] decoded = new String[numProtocols];
    for (int i = 0, d = 0; i < protocols.length; ) {
      int protocolLength = protocols[i];
      decoded[d++] = protocolLength > 0 ? new String(protocols, i + 1, protocolLength, US_ASCII) : "";
      i += 1 + protocolLength;
    }
    return decoded;
  }

  /**
  Encodes a list of protocols into the wire-format (length-prefixed 8-bit strings).
  Requires that all strings be encoded with US-ASCII.
  @param protocols the list of protocols to be encoded
  @return the encoded form of the protocol list.
  @throws IllegalArgumentException if protocols is `null', or if any element is
  `null' or an empty string.
  */
  static byte[] encodeProtocols(String[] protocols) {
    if (protocols == null) {
      throw new IllegalArgumentException("protocols array must be non-null");
    }
    if (protocols.length == 0) {
      return EmptyArray.BYTE;
    }
    // Calculate the encoded length.
    int length = 0;
    for (int i = 0; i < protocols.length; ++i) {
      String protocol = protocols[i];
      if (protocol == null) {
        throw new IllegalArgumentException("protocol[" + i + "] is null");
      }
      int protocolLength = protocols[i].length();
      // Verify that the length is valid here, so that we don't attempt to allocate an array
      // below if the threshold is violated.
      if (protocolLength == 0 || protocolLength > MAX_PROTOCOL_LENGTH) {
        throw new IllegalArgumentException("protocol[" + i + "] has invalid length: " + protocolLength);
      }
      // Include a 1-byte prefix for each protocol.
      length += 1 + protocolLength;
    }
    byte[] data = new byte[length];
    for (int dataIndex = 0, i = 0; i < protocols.length; ++i) {
      String protocol = protocols[i];
      int protocolLength = protocol.length();
      // Add the length prefix.
      data[dataIndex++] = (byte) protocolLength;
      for (int ci = 0; ci < protocolLength; ++ci) {
        char c = protocol.charAt(ci);
        if (c > Byte.MAX_VALUE) {
          // Enforce US-ASCII
          throw new IllegalArgumentException("Protocol contains invalid character: " + c + "(protocol=" + protocol + ")");
        }
        data[dataIndex++] = (byte) c;
      }
    }
    return data;
  }

  /**
  Return how much bytes can be read out of the encrypted data. Be aware that this method will
  not increase the readerIndex of the given `ByteBuffer'.
  @param buffers The `ByteBuffer's to read from. Be aware that they must have at least
  `com.android.org.conscrypt.NativeConstants#SSL3_RT_HEADER_LENGTH' bytes to read, otherwise it will
  throw an `IllegalArgumentException'.
  @return length The length of the encrypted packet that is included in the buffer. This will
  return `-1' if the given `ByteBuffer' is not encrypted at all.
  @throws IllegalArgumentException Is thrown if the given `ByteBuffer' has not at least
  `com.android.org.conscrypt.NativeConstants#SSL3_RT_HEADER_LENGTH' bytes to read.
  */
  static int getEncryptedPacketLength(ByteBuffer[] buffers, int offset) {
    ByteBuffer buffer = buffers[offset];
    // Check if everything we need is in one ByteBuffer. If so we can make use of the fast-path.
    if (buffer.remaining() >= SSL3_RT_HEADER_LENGTH) {
      return getEncryptedPacketLength(buffer);
    }
    // We need to copy 5 bytes into a temporary buffer so we can parse out the packet length
    // easily.
    ByteBuffer tmp = ByteBuffer.allocate(SSL3_RT_HEADER_LENGTH);
    while (tmp.hasRemaining()) {
      buffer = buffers[offset++];
      int pos = buffer.position();
      int limit = buffer.limit();
      if (buffer.remaining() > tmp.remaining()) {
        buffer.limit(pos + tmp.remaining());
      }
      try {
        tmp.put(buffer);
      } finally {
        // Restore the original indices.
        buffer.limit(limit);
        buffer.position(pos);
      }
    }
    // Done, flip the buffer so we can read from it.
    tmp.flip();
    return getEncryptedPacketLength(tmp);
  }

  private static int getEncryptedPacketLength(ByteBuffer buffer) {
    int pos = buffer.position();
    // SSLv3 or TLS - Check ContentType
    switch((unsignedByte(buffer.get(pos)))) {
      case SSL3_RT_CHANGE_CIPHER_SPEC:
      case SSL3_RT_ALERT:
      case SSL3_RT_HANDSHAKE:
      case SSL3_RT_APPLICATION_DATA:
        break;
      default:
    }
    // SSLv3 or TLS - Check ProtocolVersion
    int majorVersion = unsignedByte(buffer.get(pos + 1));
    if (majorVersion != 3) {
      // Neither SSLv3 or TLSv1 (i.e. SSLv2 or bad data)
      return -1;
    }
    // SSLv3 or TLS
    int packetLength = unsignedShort(buffer.getShort(pos + 3)) + SSL3_RT_HEADER_LENGTH;
    if (packetLength <= SSL3_RT_HEADER_LENGTH) {
      // Neither SSLv3 or TLSv1 (i.e. SSLv2 or bad data)
      return -1;
    }
    return packetLength;
  }

  private static short unsignedByte(byte b) {
    return (short) (b & 0xFF);
  }

  private static int unsignedShort(short s) {
    return s & 0xFFFF;
  }

  private SSLUtils() {
  }
}