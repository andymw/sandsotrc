package sand.common;

import java.io.*;
import java.util.*;

import common.*;

/**
 * Used by client-side api to persist user salts (unencrypted, unmac'd)
 *   for user account reconstruction
 */
public class PersistingSalt implements Persistable {

	private byte[] salt;

	public PersistingSalt(byte[] salt) {
		this.salt = salt;
	}

	public byte[] getSalt() { return salt; }

	@Override
	public void persist(OutputStream os) throws IOException {
		Utils.writeArray(salt, os);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		salt = Utils.readArray(is);
	}

	/**
	 * @return a hex representation of the byte array.
	 */
	@Override
	public String toString() {
		Formatter formatter = new Formatter();
		for (byte b : this.salt) {
			formatter.format("%02x", b);
		}
		String ret = formatter.toString();
		formatter.close();
		return ret;
	}

}
