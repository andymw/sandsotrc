package sand.client;

import java.io.*;
import java.security.*;

import javax.crypto.*;

import common.*;
import common.crypto.*;

import sand.common.*;

/**
 * Consists of a permanent data key for encryption, permanent mac key for mac,
 *   permanent public key and private key, permanent salt,
 *   and variable username, salt, and master key (derived from user passwd + salt).
 * Stored locally with stright-up username lookup.
 */
public class UserAccount implements Persistable {

	private String username;
	private byte[] salt;
	// @transient - should never persist the master key
	private SecretKey masterKey;
	private SecretKey macKey; // should only be persisted in wrapped form
	private SecretKey dataKey;
	private PrivateKey privKey;
	private PublicKey pubKey;
	private String twoFactorToken;
	private boolean enableTwoFactor;
	private long revisionNumber;

	private UserAccount() {
		this.revisionNumber = 0;
	}

	public UserAccount(SecretKey masterKey) {
		this.masterKey = masterKey;
		this.revisionNumber = 0;
	}

	public static UserAccount createNew(String username, char[] password) {
		return createNew(username,password,false);
	}

	public static UserAccount createNew(String username, char[] password, boolean enable) {
		UserAccount ua = new UserAccount();
		ua.username = username;
		ua.salt = CryptoUtils.getRandomArray();
		ua.dataKey = CryptoUtils.generateAESKey();
		ua.macKey = CryptoUtils.generateMACKey();
		KeyPair authKeys = CryptoUtils.generateRSAKeyPair();
		ua.privKey = authKeys.getPrivate();
		ua.pubKey = authKeys.getPublic();
		ua.masterKey = CryptoUtils.deriveAESKey(password, ua.salt);
		ua.twoFactorToken = TwoFactorAuth.createSecret();
		ua.enableTwoFactor = enable;
		return ua;
	}

	/**
	 * Used by client-side server handler.
	 */
	public static UserAccount fromLogin(Keys keys, String username, char[] password) {
		UserAccount ua = new UserAccount(); // revision number = 0 default;
		ua.username = username;
		ua.salt = keys.getSalt();
		ua.masterKey = CryptoUtils.deriveAESKey(password, ua.salt);
		ua.pubKey = keys.getPublicKey();
		String tfInfo = keys.getTwoFactorToken();
		ua.twoFactorToken = tfInfo.substring(0,16);
		ua.enableTwoFactor = (tfInfo.substring(16).equals("1"));
		try {
			ua.macKey = (SecretKey)keys.getWrappedMACKey().unwrap(ua.masterKey);
			ua.dataKey = (SecretKey)keys.getWrappedDataKey().unwrap(ua.masterKey);
			ua.privKey = (PrivateKey)keys.getWrappedPrivateKey().unwrap(ua.masterKey);
		} catch (InvalidKeyException impossible) { }
		return ua;
	}

	public Keys getKeys() {
		try {
			WrappedKey dataKeyEnc = CryptoUtils.wrapKey(masterKey, dataKey);
			WrappedKey macKeyEnc = CryptoUtils.wrapKey(masterKey, macKey);
			WrappedKey privKeyEnc = CryptoUtils.wrapKey(masterKey, privKey);
			return new Keys(salt, dataKeyEnc, macKeyEnc, privKeyEnc, pubKey, twoFactorToken+enabledString());
		} catch (InvalidKeyException impossible) { return null; }
	}

	private String enabledString() {
		if (enableTwoFactor)
			return "1";
		else
			return "0";
	}

	public void setFromKeys(Keys keys) throws InvalidKeyException {
		this.salt = keys.getSalt();
		this.macKey = (SecretKey)keys.getWrappedMACKey().unwrap(masterKey);
		this.dataKey = (SecretKey)keys.getWrappedDataKey().unwrap(masterKey);
		this.pubKey = keys.getPublicKey();
		this.privKey = (PrivateKey)keys.getWrappedPrivateKey().unwrap(masterKey);
		String tfInfo = keys.getTwoFactorToken();
		this.twoFactorToken = tfInfo.substring(0,16);
		this.enableTwoFactor = (tfInfo.substring(16).equals("1"));
	}

	public void updateMasterKey(char[] newPass) {
		masterKey = CryptoUtils.deriveAESKey(newPass, salt);
	}

	public byte[] getSalt() { return salt; }
	public PrivateKey getPrivateKey() { return privKey; }
	public SecretKey getMasterKey() { return masterKey; }
	public SecretKey getMACKey() { return macKey; }
	public SecretKey getDataKey() { return dataKey; }
	public String getUsername() { return username; }
	public long getRevisionNumber() { return revisionNumber; }
	public void setUsername(String un) { username = un; }
	public String getTwoFactorToken() { return twoFactorToken; }
	public boolean isTwoFactorEnabled() { return enableTwoFactor; }
	public void setTwoFactor(boolean enabled) { enableTwoFactor = enabled; }

	/**
	 * Generates a unique String identification for the UserAccount.
	 * Constructed from hash(salt, datakey), both of which are permanent for an account.
	 * Precondition: acct is not null.
	 */
	public static String getUniqueID(UserAccount acct) {
		return CryptoUtils.keyedHash(acct.getSalt(), acct.getDataKey());
	}

	/**
	 * It is the ClientManager's responsibility to rederive the master key.
	 *   <code>ua.masterKey = CryptoUtils.deriveAESKey(password, ua.salt);</code>
	 * Considering not using the persist and reconstruct api.
	 */
	@Override
	public void persist(OutputStream os) throws IOException {
		// we must persist the keys object.
		// master key does not need to be persisted, but derived from password.
		DataOutputStream dos = new DataOutputStream(os);
		getKeys().persist(os);
		dos.writeUTF(this.username);
		dos.writeLong(this.revisionNumber);
	}

	/**
	 * It is the ClientManager's responsibility to rederive the master key.
	 *   <code>ua.masterKey = CryptoUtils.deriveAESKey(password, ua.salt);</code>
	 * Considering not using the persist and reconstruct api.
	 */
	@Override
	public void reconstruct(InputStream is) throws IOException {
		// reconstruct the keys object.
		DataInputStream dis = new DataInputStream(is);
		Keys keyReconstruct = Keys.newForReconstruction();
		keyReconstruct.reconstruct(is);
		try {
			this.setFromKeys(keyReconstruct);
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
		this.username = dis.readUTF();
		this.revisionNumber = dis.readLong();
	}

}
