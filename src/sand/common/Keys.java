package sand.common;

import java.io.*;
import java.security.*;
import common.crypto.*;
import common.*;

public class Keys implements Persistable {

	private byte[] salt;
	private WrappedKey dataKey;
	private WrappedKey macKey;
	private WrappedKey privKey;
	private PublicKey pubKey;
	private boolean persistable;
	private String twoFactorToken;

	private Keys() { persistable = false; }

	/** This Keys object CANNOT be persisted until it has first been reconstructed. */
	public static Keys newForReconstruction() {
		return new Keys();
	}

	public Keys(byte[] salt, WrappedKey dataKey, WrappedKey macKey,
			WrappedKey privKey, PublicKey pubKey, String twoFactorToken) {
		this.salt = salt;
		this.dataKey = dataKey;
		this.macKey = macKey;
		this.privKey = privKey;
		this.pubKey = pubKey;
		this.twoFactorToken = twoFactorToken;
		persistable = true;
	}

	public static Keys readIn(InputStream is) throws IOException {
		Keys keys = new Keys();
		keys.reconstruct(is);
		return keys;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		if (!persistable)
			throw new IllegalStateException("Cannot persist nullary Keys.");
		Utils.writeArray(salt, os);
		dataKey.persist(os);
		macKey.persist(os);
		privKey.persist(os);
		Utils.writeArray(twoFactorToken.getBytes(), os);
		Utils.writeArray(pubKey.getEncoded(), os);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		salt = Utils.readArray(is);
		dataKey = WrappedKey.readIn(is);
		macKey = WrappedKey.readIn(is);
		privKey = WrappedKey.readIn(is);
		twoFactorToken = new String(Utils.readArray(is));
		pubKey = CryptoUtils.decodeRSAPublicKey(Utils.readArray(is));
		persistable = true;
	}

	public byte[] getSalt() { return salt; }
	public WrappedKey getWrappedDataKey() { return dataKey; }
	public WrappedKey getWrappedMACKey() { return macKey; }
	public WrappedKey getWrappedPrivateKey() { return privKey; }
	public PublicKey getPublicKey() { return pubKey; }
	public String getTwoFactorToken() { return twoFactorToken; }

}
