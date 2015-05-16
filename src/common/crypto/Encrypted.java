package common.crypto;

import java.io.*;
import java.security.*;

import javax.crypto.*;
import common.*;

/**
 * encrypt-then-mac
 */
public class Encrypted<T extends Persistable> implements Persistable {

	private final T persistable;
	private final SecretKey dataKey; // for AES. algorithm = AES
	private final SecretKey macKey; // for MAC. algorithm = HmacSHA512

	/**
	 * takes in a persistable T, a key for AES, and a key for MAC.
	 */
	public Encrypted(T persistable, SecretKey dataKey, SecretKey macKey) {
		this.persistable = persistable;
		this.dataKey = dataKey;
		this.macKey = macKey;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		try {
			MACOutputStream macout = new MACOutputStream(os, macKey);
			AESOutputStream aesout = new AESOutputStream(macout, dataKey);
			persistable.persist(aesout);
			aesout.doFinal();
			macout.doFinal();
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		try {
			MACInputStream macin = new MACInputStream(is, macKey);
			AESInputStream in = new AESInputStream(macin, dataKey);
			persistable.reconstruct(in);
			macin.doFinal();
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

	public T getT() {
		return persistable;
	}

    public String toString() {
        return this.persistable.toString();
    }

}
