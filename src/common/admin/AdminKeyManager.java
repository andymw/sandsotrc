package common.admin;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.SecretKey;
import common.*;
import common.crypto.*;

public class AdminKeyManager {
	private static final String PRIVATE_KEY_LOC = "../adminkeys/admin";
	private static final String PUBLIC_KEY_LOC = "../adminkeys/admin.pub.der";
	private static final String ENC_PRIVATE_KEY_LOC = "../adminkeys/admin.aes256cbc";
	private static final String DEC_PRIVATE_KEY_LOC = "../adminkeys/admin.dec";
	private static final String PRIVATE_WRAPPED_KEY_LOC = "../adminkeys/wrappedKey";
	private static final String ADMIN_SALT_LOC = "../adminkeys/adminsalt";

	public static void main(String[] args) {
		getPrivateKey();
		getPublicKey();
	}

	/**
	 * To be used by the client.
	 * Attempts to decrypt admin private key with password.
	 */
	public static PrivateKey getPrivateKey() {
		return getPrivateKey(false);
	}

	/**
	 * To be used by the client.
	 * Attempts to decrypt admin private key with password.
	 * By default, does not write private key.
	 * On bad password, could return null.
	 */
	public static PrivateKey getPrivateKey(boolean writeKey) {
		byte[] dec = decryptPrivateKey();
		if (dec == null)
			return null;
		if (writeKey) {
			try { Utils.writeBinaryFile(DEC_PRIVATE_KEY_LOC, dec); }
			catch (IOException e) { System.err.println(e.toString()); }
		}
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(dec);
		KeyFactory kf = null;
		try {
			kf = KeyFactory.getInstance("RSA");
			return kf.generatePrivate(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			System.err.println(e.toString());
		}
		return null;
	}

	/**
	 * To be used by the server.
	 */
	public static PublicKey getPublicKey() {
		// from http://stackoverflow.com/questions/11410770/java-load-rsa-public-key-from-file
		byte[] pub = null;
		try {
			pub = Utils.readBinaryFile(PUBLIC_KEY_LOC);
		} catch (IOException e) {
			System.err.println(e.toString());
		}
		X509EncodedKeySpec spec = new X509EncodedKeySpec(pub);
		KeyFactory kf = null;
		try {
			kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			System.err.println(e.toString());
		}
		return null;
	}

	private static byte[] decryptPrivateKey() {
		System.out.print("Enter AES encryption password for admin private key: ");
		char[] pass = System.console().readPassword();
		try {
			byte[] dec = CryptoUtils.decryptFile(ENC_PRIVATE_KEY_LOC, pass);
			return dec;
		} catch (IOException e) {
			System.err.println("Exception: unable to decrypt private key with password.");
		}
		return null;
	}

	// performed initially to make the aes256cbc file. do not perform again
	@SuppressWarnings("unused")
	private static void encryptPrivateKey() throws IOException {
		System.out.println("Encrypting private key.");
		System.out.print("Enter AES encryption password: ");
		char[] pass = System.console().readPassword();
		byte[] priv = Utils.readBinaryFile(PRIVATE_KEY_LOC);
		CryptoUtils.encryptAndWriteFile(ENC_PRIVATE_KEY_LOC, priv, pass);
	}


	/**
	 * Should only be run once. This creates the necessary key and salt to send to an admin
	 * client wishing to connect
	 */
	@SuppressWarnings("unused")
	private static void wrapAndSavePrivateKey() throws IOException, InvalidKeyException {
		PrivateKey adminKey = getPrivateKey();
		System.out.print("Enter the ADMIN password: ");
		char[] pass = System.console().readPassword();
		byte[] salt = CryptoUtils.getRandomArray();
		SecretKey masterKey = CryptoUtils.deriveAESKey(pass, salt);
		WrappedKey privKeyEnc = CryptoUtils.wrapKey(masterKey, adminKey);
		File file = new File(PRIVATE_WRAPPED_KEY_LOC);
		FileOutputStream fop = new FileOutputStream(file);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}
		privKeyEnc.persist(fop);
		fop.close();

		// save the salt
		Files.write(Paths.get(ADMIN_SALT_LOC), salt);
	}

	public static byte[] getAdminSalt() throws IOException {
		byte[] data = Files.readAllBytes(Paths.get(ADMIN_SALT_LOC));
		return data;
	}

	public static WrappedKey getAdminWrappedKey() throws IOException {
		FileInputStream fis = new FileInputStream(PRIVATE_WRAPPED_KEY_LOC);
		WrappedKey key = WrappedKey.readIn(fis);
		return key;
	}

	/* previous code read strings repeatedly to hash against DIGEST *//*
	// for the password, please seek SAND for information.
	// DIGEST is a SHA-256 hash of the password (no salting. hmm)
	private static final String DIGEST = "dc497644afdca9914830b0a11a668a683cd578263800216e1013a36f5a05fe4a";

	public static void main(String[] args) throws Exception {
		System.out.println("Admin verifier.");

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		boolean again = true;

		// eventually, the encrypted password needs to be sent to server
		// server decrypts and computes hash? sends yes/no back to client?
		while (again) {
			byte[] digest = getPasswordAndHash(md);
			System.out.println("Digest "
				+ (byteArrayMatches(digest) ? "matches!" : "does not match"));
			System.out.print("again? (y or n) ");
			String userin = br.readLine();
			again = userin.toLowerCase().charAt(0) == 'y';
		}

		System.out.println("\nDone.");
	}

	private static byte[] getPasswordAndHash(MessageDigest md) throws Exception {
		md.reset();
		System.out.print("Enter password: ");
		md.update(new String(System.console().readPassword()).getBytes("UTF-8"));
		return md.digest();
	}

	// from http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	public static boolean byteArrayMatches(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("%02x", b & 0xff));
		return sb.toString().equals(DIGEST);
	}
	*/
}
