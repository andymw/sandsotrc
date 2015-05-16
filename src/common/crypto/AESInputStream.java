package common.crypto;

import java.io.*;
import java.security.*;
import javax.crypto.*;

/**
 * An input stream that reads and decrypts AES/CBC/PKCS5-encrypted data
 * from an underlying input stream.
 */
public class AESInputStream extends InputStream {

	private CipherInputStream in;

	public AESInputStream(InputStream in, SecretKey userKey)
			throws InvalidKeyException, IOException {
		byte[] iv = new byte[16];
		int numRead = in.read(iv);
		if (numRead != 16) {
			throw new IOException("Could not read initialization vector.");
		}
		Cipher cipher = CryptoUtils.getAESCipher(userKey, iv, Cipher.DECRYPT_MODE);
		this.in = new CipherInputStream(in, cipher);
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte[] b, int offs, int len) throws IOException {
		return in.read(b, offs, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
