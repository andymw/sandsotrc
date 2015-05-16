package common.crypto;

import java.io.*;
import java.security.*;
import javax.crypto.*;

/**
 * An output stream that encrypts data under AES/CBC/PKCS5 and writes the
 * ciphertext to an underlying output stream.
 */
public class AESOutputStream extends OutputStream {

	private final Cipher cipher;
	private final OutputStream out;
	private boolean finalized;

	public AESOutputStream(OutputStream out, SecretKey userKey)
			throws InvalidKeyException, IOException {
		byte[] iv = CryptoUtils.getRandomArray();
		this.cipher = CryptoUtils.getAESCipher(userKey, iv, Cipher.ENCRYPT_MODE);
		out.write(iv);
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(cipher.update(new byte[] {(byte)b}));
	}

	@Override
	public void write(byte[] b, int offs, int len) throws IOException {
		out.write(cipher.update(b, offs, len));
	}

	/**
	 * Finalize the encryption, writing out the last padded block,
	 * while leaving the underlying output stream open for further writing.
	 */
	public void doFinal() throws IOException {
		if (!finalized) {
			try {
				out.write(cipher.doFinal());
				out.flush();
			} catch (IllegalBlockSizeException | BadPaddingException impossible) { }
			finalized = true;
		}
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		doFinal();
		out.flush();
		out.close();
	}

}
