package common.crypto;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class SslCertificateTests {
	private static Certificate getServerCertificate() throws Exception {
		String keystorefile = "../certificate/serverside/sandsotrcpistore.jks";
		char[] password = "!T21s46fe-sand".toCharArray();
		String alias = "sandsotrcpi"; // our key name?

		FileInputStream fIn = new FileInputStream(keystorefile);
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(fIn, password);

		Certificate cert = ks.getCertificate(alias);
		return cert;
	}

	private static Certificate getClientCertificate() throws Exception {
		String keystorefile = "../certificate/clientside/5430ts.jks";
		char[] password = "!T21s46fe-sand".toCharArray();
		String alias = "root"; // their key name

		FileInputStream fIn = new FileInputStream(keystorefile);
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(fIn, password);

		Certificate cert = ks.getCertificate(alias);
		return cert;
	}

	public static void main(String[] args) throws Exception {
		Certificate certs = getServerCertificate();
		Certificate certc = getClientCertificate();
		System.out.println("certs.verify(certc);");
		certs.verify(certc.getPublicKey());
		System.out.println("if this gets printed, then passed.");
		System.out.println("certc.verify(certc);");
		certc.verify(certc.getPublicKey());
		System.out.println("if this gets printed, then passed.");
		System.out.println("certc.verify(certs);");
		certc.verify(certs.getPublicKey());
		System.out.println("if this gets printed, then WHAT?!");
	}

}
