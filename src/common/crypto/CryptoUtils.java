package common.crypto;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

import common.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Static class containing utility methods for deriving AES and MAC keys.
 */
public class CryptoUtils {

	private static MessageDigest hasher = null;
	private static Cipher cipherAES = null;
	private static SecureRandom random = new SecureRandom();

	public static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
	public static final int PBKDF_ITERATIONS = 65536;
	public static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA1";
	public static final String MAC_ALGORITHM = "HmacSHA256";
	public static final String RSA_ALGORITHM = "RSA";
	public static final String RSA_PADDING_ALGORITHM
		= "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

	static {
		try {
			hasher = MessageDigest.getInstance("SHA-256");
			cipherAES = Cipher.getInstance(AES_ALGORITHM);
		} catch (GeneralSecurityException impossible) { }
	}

	private CryptoUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Derives a 256-bit AES key from the provided password and salt,
	 * using {@link #PBKDF_ITERATIONS} iterations of PBKDF.
	 */
	public static SecretKey deriveAESKey(char[] password, byte[] salt) {
		return deriveAESKey(password, salt, 256);
	}

	/**
	 * Derives an AES key of the specified length from the provided password
	 * and salt, using {@link #PBKDF_ITERATIONS} iterations of PBKDF.
	 */
	public static SecretKey deriveAESKey(char[] password, byte[] salt, int length) {
		PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF_ITERATIONS, length);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
			byte[] keyBytes = factory.generateSecret(spec).getEncoded();
			return new SecretKeySpec(keyBytes, "AES");
		} catch (GeneralSecurityException impossible) { }
		return null;
	}

	/**
	 * By default, encrypts using AES_ALGORITHM: AES256/CBC/PKCS5Padding
	 *  with 64-bit salt, 256-bit iv
	 * Protocol: salt is written first, then iv, then rest of file
	 */
	public static void encryptAndWriteFile(String filename, byte[] file, char[] password)
			throws IOException {
		byte[] salt = CryptoUtils.getRandomArray(8); // 64 bits
		SecretKey key = CryptoUtils.deriveAESKey(password, salt);
		AESOutputStream aesos = null;

		try {
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(salt); // unencrypted
			aesos = new AESOutputStream(fos, key);
			aesos.write(file); // encrypt then write
			aesos.flush();
		} catch (InvalidKeyException e) {
			System.err.println(e.toString());
			System.out.println("unable to encrypt and write file");
		} finally {
			try {
				aesos.close();
			} catch (Exception e) { }
		}
	}

	/**
	 * Decrypts assuming AES_ALGORITHM: AES256/CBC/PKCS5Padding
	 * Assumes 64-bit salt as first bytes, then 256-bit iv.
	 */
	public static byte[] decryptFile(String filename, char[] password)
			throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		byte[] salt = new byte[8];
		fis.read(salt);

		SecretKey key = CryptoUtils.deriveAESKey(password, salt);
		// aesinputstream's responsibility to read the iv
		AESInputStream aesis = null;
		try {
			aesis = new AESInputStream(fis, key);
			byte[] plaintext = Utils.readStreamFully(aesis);
			return plaintext;
		} catch (InvalidKeyException ike) {
			System.err.println(ike.toString());
			System.err.println("unable to decrypt file");
		}
		System.err.println("this part shouldn't happen");
		return null;
	}

	/**
	 * The only guarantee made by this method is that
	 * keyedHash(s, key) == keyedHash(s, key) for all invocations.
	 * Precondition: s and key are not null.
	 */
	public static String keyedHash(String s, SecretKey key) {
		// consider repeating action more than once.
		CryptoUtils.hasher.reset();
		CryptoUtils.hasher.update(key.getEncoded());
		CryptoUtils.hasher.update(s.getBytes(UTF_8));
		return Arrays.toString(hasher.digest());
	}

	/**
	 * The only guarantee made by this method is that
	 * keyedHash(bytes, key) == keyedHash(bytes, key) for all invocations.
	 * Precondition: bytes and key are not null.
	 */
	public static String keyedHash(byte[] bytes, SecretKey key) {
		CryptoUtils.hasher.reset();
		CryptoUtils.hasher.update(key.getEncoded());
		CryptoUtils.hasher.update(bytes);
		return Arrays.toString(hasher.digest());
	}

	/**
	 * Generates an AES key of the specified length derived from secure random bytes.
	 */
	public static SecretKey generateAESKey(int length) {
		try {
			KeyGenerator gen = KeyGenerator.getInstance("AES");
			gen.init(length);
			return gen.generateKey();
		} catch (NoSuchAlgorithmException impossible) { }
		return null;
	}

	public static SecretKey generateAESKey() {
		return generateAESKey(256);
	}

	/**
	 * Generates a MAC key derived from secure random bytes.
	 */
	public static SecretKey generateMACKey() {
		try {
			return KeyGenerator.getInstance(MAC_ALGORITHM).generateKey();
		} catch (NoSuchAlgorithmException impossible) { }
		return null;
	}

	/**
	 * Generates an RSA-2048 key pair.
	 */
	public static KeyPair generateRSAKeyPair() {
		return generateRSAKeyPair(2048);
	}

	public static KeyPair generateRSAKeyPair(int length) {
		try {
			KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
			keygen.initialize(length);
			return keygen.genKeyPair();
		} catch (NoSuchAlgorithmException impossible) { return null; }
	}

	/**
	 * Encrypts ("wraps") a key under another encryption key.
	 */
	public static WrappedKey wrapKey(Key encKey, Key wrapKey) throws InvalidKeyException {
		try {
			byte[] iv = getRandomArray();
			cipherAES.init(Cipher.WRAP_MODE, encKey, new IvParameterSpec(iv));
			byte[] enc = cipherAES.wrap(wrapKey);
			int type = wrapKey instanceof SecretKey ? Cipher.SECRET_KEY
				: wrapKey instanceof PublicKey ? Cipher.PUBLIC_KEY
				: wrapKey instanceof PrivateKey ? Cipher.PRIVATE_KEY : -1;
			return new WrappedKey(wrapKey.getAlgorithm(), type, concat(iv, enc));
		} catch (IllegalBlockSizeException
		       | InvalidAlgorithmParameterException impossible) { return null; }
	}

	static Key unwrapKey(byte[] keyEnc, Key decKey, String algo, int type)
	throws InvalidKeyException {
		byte[] iv = Arrays.copyOfRange(keyEnc, 0, 16);
		byte[] enc = Arrays.copyOfRange(keyEnc, 16, keyEnc.length);
		try {
			cipherAES.init(Cipher.UNWRAP_MODE, decKey, new IvParameterSpec(iv));
			return cipherAES.unwrap(enc, algo, type);
		} catch (InvalidAlgorithmParameterException
		       | NoSuchAlgorithmException impossible) { }
		return null;
	}

	public static PublicKey decodeRSAPublicKey(byte[] enc) {
		try {
			KeySpec spec = new X509EncodedKeySpec(enc);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(spec);
		} catch (GeneralSecurityException impossible) {
			return null;
		}
	}

	public static byte[] encryptRSA(PublicKey key, byte[] plain) throws
			IllegalBlockSizeException, InvalidKeyException, BadPaddingException {
		try {
			Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] enc = cipher.doFinal(plain);
			return enc;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException impossible) {
			return null;
		}
	}

	public static byte[] decryptRSA(PrivateKey key, byte[] enc) throws
			IllegalBlockSizeException, InvalidKeyException, BadPaddingException {
		try {
			Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] dec = cipher.doFinal(enc);
			return dec;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException impossible) {
			return null;
		}
	}

	public static long getKeyFingerprint(Key key) {
		hasher.reset();
		byte[] hash = hasher.digest(key.getEncoded());
		long fingerprint = 0L;
		for (int i = 0; i < hash.length; i += 8) {
			long next = 0L;
			for (int j = 0; j < 8; j++) {
				next <<= 8;
				next |= hash[i+j] & 0xFF;
			}
			fingerprint ^= next;
		}
		return fingerprint;
	}

	public static void zeroPassword(char[] password) {
		for (int i = 0; i < password.length; password[i++] = '\0');
	}

	/**
	 * Constructs and returns an initialized AES cipher.
	 */
	static Cipher getAESCipher(Key key, byte[] iv, int mode)
	throws InvalidKeyException {
		try {
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
			cipher.init(mode, key, new IvParameterSpec(iv));
			return cipher;
		} catch (InvalidAlgorithmParameterException |
		         NoSuchAlgorithmException | NoSuchPaddingException impossible) { }
		return null;
	}

	public static byte[] getRandomArray() {
		return CryptoUtils.getRandomArray(16);
	}

	public static byte[] getRandomArray(int length) {
		byte[] iv = new byte[length];
		random.nextBytes(iv);
		return iv;
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] arr = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, arr, a.length, b.length);
		return arr;
	}

}
