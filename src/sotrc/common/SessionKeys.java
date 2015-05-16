package sotrc.common;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import common.*;
import common.crypto.*;

/**
 * Wraps the encryption and MAC session keys used during a chat.
 */
public class SessionKeys implements Persistable {

	private SecretKey encKey, macKey;

	private byte[] enc, mac;

	private PrivateKey pShortTerm, pLongTerm;
	private PublicKey PShortTerm, PLongTerm;

	public static SessionKeys makeNew(PublicKey shortTerm, PublicKey longTerm) {
		SessionKeys s = new SessionKeys(
			CryptoUtils.generateAESKey(), CryptoUtils.generateAESKey());
		s.setupPersist(shortTerm, longTerm);
		return s;
	}

	public static SessionKeys newForReconstruction(PrivateKey shortTerm, PrivateKey longTerm) {
		SessionKeys s = new SessionKeys(null, null);
		s.setupReconstruct(shortTerm, longTerm);
		return s;
	}

	/** For server to call. */
	public static SessionKeys newForTransmission() {
		return new SessionKeys(null, null);
	}
	
	private SessionKeys(SecretKey encKey, SecretKey macKey) {
		this.encKey = encKey;
		this.macKey = macKey;
	}

	public SecretKey getEncKey() { return encKey; }

	public SecretKey getMACKey() { return macKey; }

	public void setupPersist(PublicKey shortTerm, PublicKey longTerm) {
		PShortTerm = shortTerm;
		PLongTerm = longTerm;
	}

	private void setupReconstruct(PrivateKey shortTerm, PrivateKey longTerm) {
		pShortTerm = shortTerm;
		pLongTerm = longTerm;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		byte[] encCiph, macCiph;
		if (enc != null && mac != null) { // persisting in encrypted form
			encCiph = enc;
			macCiph = mac;
		} else {
			// encrypt keys before persisting
			if (PShortTerm == null || PLongTerm == null)
				throw new IllegalStateException("Persisting keys have not been set.");
			try {
				encCiph = CryptoUtils.encryptRSA(PLongTerm,
					CryptoUtils.encryptRSA(PShortTerm, encKey.getEncoded()));
				macCiph = CryptoUtils.encryptRSA(PLongTerm,
					CryptoUtils.encryptRSA(PShortTerm, macKey.getEncoded()));
			} catch (GeneralSecurityException e) {
				throw new IOException(e);
			}
		}

		Utils.writeArray(encCiph, os);
		Utils.writeArray(macCiph, os);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		enc = Utils.readArray(is);
		mac = Utils.readArray(is);
		if (pShortTerm != null && pLongTerm != null) {
			// decrypt the keys
			try {
				enc = CryptoUtils.decryptRSA(pShortTerm,
					CryptoUtils.decryptRSA(pLongTerm, enc));
				mac = CryptoUtils.decryptRSA(pShortTerm,
					CryptoUtils.decryptRSA(pLongTerm, mac));
			} catch (GeneralSecurityException e) {
				throw new IOException(e);
			}
			encKey = new SecretKeySpec(enc, "AES");
			enc = null;
			macKey = new SecretKeySpec(mac, "AES");
			mac = null;
		}
	}

}
