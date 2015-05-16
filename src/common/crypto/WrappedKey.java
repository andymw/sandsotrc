package common.crypto;

import java.security.*;
import java.io.*;

import common.*;

/**
 * A persistable key that has been encrypted ("wrapped") under another key.
 */
public class WrappedKey implements Key, Persistable {

	private String algorithm;
	private int type;
	private byte[] wrapped;

	WrappedKey() { }

	WrappedKey(String algorithm, int type, byte[] wrapped) {
		this.algorithm = algorithm;
		this.type = type;
		this.wrapped = wrapped;
	}

	public static WrappedKey readIn(InputStream in) throws IOException {
		WrappedKey key = new WrappedKey();
		key.reconstruct(in);
		return key;
	}

	public static WrappedKey newForReconstruction() {
		return new WrappedKey();
	}

	/**
	 * Unwraps (decrypts) the WrappedKey with the given Key.
	 */
	public Key unwrap(Key decKey) throws InvalidKeyException {
		return CryptoUtils.unwrapKey(wrapped, decKey, algorithm, type);
	}

	@Override
	public byte[] getEncoded() { return null; }

	@Override
	public String getAlgorithm() { return algorithm; }

	@Override
	public String getFormat() { return null; }

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(algorithm);
		dos.writeInt(type);
		Utils.writeArray(wrapped, os);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		algorithm = dis.readUTF();
		type = dis.readInt();
		wrapped = Utils.readArray(is);
	}

}
