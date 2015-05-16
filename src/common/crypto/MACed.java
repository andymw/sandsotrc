package common.crypto;

import java.io.*;
import java.security.*;

import javax.crypto.*;
import common.*;

/**
 * encrypt-then-mac
 */
public class MACed<T extends Persistable> implements Persistable {

	private final T persistable;
	private final SecretKey macKey; // for MAC. algorithm = HmacSHA512

	/**
	 * takes in a persistable T, a key for AES, and a key for MAC.
	 */
	public MACed(T persistable, SecretKey macKey) {
		this.persistable = persistable;
		this.macKey = macKey;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		try {
			MACOutputStream macout = new MACOutputStream(os, macKey);
			persistable.persist(macout);
			macout.doFinal();
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		try {
			MACInputStream macin = new MACInputStream(is, macKey);
			persistable.reconstruct(macin);
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
