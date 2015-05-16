package common.network;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

/**
 * We're using the same set of stores for SAND and SOTRC.
 */
public class SslDefaults {
	public static final String[] PROTOCOLS = {
		"SSLv2Hello", "TLSv1.2",
	};
	public static final String[] CIPHERSUITES = {
		"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
		  "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
		"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
		  "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
		//"TLS_ECDHE_ECDSA_WITH_NULL_SHA", // deprecated for log transmission
		//  "TLS_ECDHE_RSA_WITH_NULL_SHA", // deprecated for log transmission
	}; // https://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html

	public static final String DEFAULT_HOST = "sotrc.homenet.org";
	public static final int DEFAULT_SAND_PORT = 5430;
	public static final int DEFAULT_SOTRC_PORT = 5999;

	private static final String TRUST_STORE_LOC =
		"../certificate/clientside/5430ts.jks";
	private static final String KEY_STORE_LOC =
		"../certificate/serverside/sandsotrcpistore.jks";
	private static final boolean VERIFY_HOSTNAME = false; // toggle
	private static final String TRUST_STORE_PASSWD = "!T21s46fe-sand";

	public static SSLSocket getClientSandSSLSocket() throws IOException {
		return getClientSSLSocket(DEFAULT_HOST, DEFAULT_SAND_PORT);
	}

	public static SSLSocket getClientSSLSocket(String hostname, int port) throws IOException {
		return getClientSSLSocket(hostname, port, VERIFY_HOSTNAME);
	}
	private static SSLSocket getClientSSLSocket(String hostname, int port, boolean verifyHostname)
			throws IOException {
		System.setProperty("javax.net.ssl.trustStore", TRUST_STORE_LOC);
		System.setProperty("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWD);
		SSLSocketFactory f =
			(SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket c = (SSLSocket) f.createSocket(hostname, port);
		c.setEnabledProtocols(SslDefaults.PROTOCOLS);
		c.setEnabledCipherSuites(SslDefaults.CIPHERSUITES);

		// verify hostname
		if (verifyHostname) {
			SSLParameters sslParams = new SSLParameters();
			sslParams.setEndpointIdentificationAlgorithm("HTTPS");
			c.setSSLParameters(sslParams);
		}

		//c.startHandshake();
		return c;
	}

	public static SSLServerSocket getServerSandSSLSocket() throws IOException {
		return getServerSSLSocket(DEFAULT_SAND_PORT);
	}

	public static SSLServerSocket getServerSSLSocket(int port) throws IOException {
		System.out.print("Enter server keystore passwd for SSL: ");
		char[] keyPassword = System.console().readPassword();
		return getServerSSLSocket(port, keyPassword);
	}

	public static SSLServerSocket getServerSSLSocket(int port, char[] keyPassword)
			throws IOException {
		if (keyPassword == null)
			throw new IOException("Server SSL Socket password passed in is null.");

		FileInputStream keyFile = null;
		try {
			keyFile = new FileInputStream(KEY_STORE_LOC);
		} catch (FileNotFoundException e) {
			System.err.println(e.toString());
			System.err.println("returning null for SSLServerSocket!");
			return null;
		}

		try {
			// init keystore
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(keyFile, keyPassword);

			// init KeyManagerFactory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyPassword);
			// init KeyManager
			KeyManager keyManagers[] = keyManagerFactory.getKeyManagers();

			// init the SSL context
			SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(keyManagers, null, new SecureRandom());

			// get SSL Engine
			//SSLEngine engine = sslContext.createSSLEngine();

			// get the socket factory
			SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
			SSLServerSocket s
				= (SSLServerSocket) ssf.createServerSocket(port);
			s.setEnabledProtocols(SslDefaults.PROTOCOLS);
			s.setEnabledCipherSuites(SslDefaults.CIPHERSUITES);
			return s;
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException
				| CertificateException | UnrecoverableKeyException e) {
			System.err.println(e.toString());
			System.err.println("returning null for SSLServerSocket");
		}
		return null;
	}

	public static void printSocketInfo(SSLSocket s) {
		System.out.println("  Remote address/port = " + s.getInetAddress().toString() + ":" + s.getPort());
		System.out.println("  Local socket address = " + s.getLocalSocketAddress().toString());
		System.out.println("  Need client authentication = " + s.getNeedClientAuth());

		SSLSession ss = s.getSession();
		System.out.println("  Protocol = " + ss.getProtocol());
		System.out.println("  Cipher suite = " + ss.getCipherSuite());
	}

	public static void printServerSocketInfo(SSLServerSocket s) {
		//System.out.println("  Socket address = " + s.getInetAddress().toString());
		System.out.println("  Socket port = " + s.getLocalPort());
		//System.out.println("  Need client authentication = " + s.getNeedClientAuth());
		//System.out.println("  Want client authentication = " + s.getWantClientAuth());
		//System.out.println("  Use client mode = " + s.getUseClientMode());
	}

}
