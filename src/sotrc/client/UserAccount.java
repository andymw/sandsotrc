package sotrc.client;

import java.io.*;
import java.security.*;

import javax.crypto.*;

import common.*;
import common.crypto.*;

import sotrc.common.*;

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
	private SecretKey dataKey;
	private SecretKey macKey;
	private PrivateKey privKey;  // should only be persisted in wrapped form
	private PublicKey pubKey;

	private UserAccount() { }

	public UserAccount(SecretKey masterKey) {
		this.masterKey = masterKey;
	}

	public static UserAccount createNew(String username, char[] password) {
		UserAccount ua = new UserAccount();
		ua.username = username;
		ua.salt = CryptoUtils.getRandomArray();
		KeyPair authKeys = CryptoUtils.generateRSAKeyPair(4096);
		ua.privKey = authKeys.getPrivate();
		ua.pubKey = authKeys.getPublic();
		ua.macKey = CryptoUtils.generateAESKey();
		ua.dataKey = CryptoUtils.generateAESKey();
		ua.masterKey = CryptoUtils.deriveAESKey(password, ua.salt);
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
		try {
			ua.privKey = (PrivateKey)keys.getWrappedPrivateKey().unwrap(ua.masterKey);
			ua.macKey = (SecretKey)keys.getWrappedMACKey().unwrap(ua.masterKey);
			ua.dataKey = (SecretKey)keys.getWrappedDataKey().unwrap(ua.masterKey);
		} catch (InvalidKeyException impossible) { }
		return ua;
	}

	public Keys getKeys() {
		try {
			WrappedKey privKeyEnc = CryptoUtils.wrapKey(masterKey, privKey);
			WrappedKey dataKeyEnc = CryptoUtils.wrapKey(masterKey,dataKey);
			WrappedKey macKeyEnc = CryptoUtils.wrapKey(masterKey, macKey);
			return new Keys(salt, dataKeyEnc, macKeyEnc, privKeyEnc, pubKey);
		} catch (InvalidKeyException impossible) { return null; }
	}

	public void setFromKeys(Keys keys) throws InvalidKeyException {
		this.salt = keys.getSalt();
		this.pubKey = keys.getPublicKey();
		this.privKey = (PrivateKey)keys.getWrappedPrivateKey().unwrap(masterKey);
	}

	public void updateMasterKey(char[] newPass) {
		masterKey = CryptoUtils.deriveAESKey(newPass, salt);
	}

	public byte[] getSalt() { return salt; }
	
	public PrivateKey getPrivateKey() { return privKey; }
	
	public SecretKey getMasterKey() { return masterKey; }

	public SecretKey getDataKey() { return dataKey; }

	public SecretKey getMACKey() { return macKey; }
	
	public String getUsername() { return username; }
        
        public PublicKey getPublicKey() { return pubKey; }
	
	public void setUsername(String un) { username = un; }

	/**
	 * Generates a unique String identification for the UserAccount.
	 * Constructed from hash(salt, datakey), both of which are permanent for an account.
	 * Precondition: acct is not null.
	 */
/*	public static String getUniqueID(UserAccount acct) {
		return CryptoUtils.keyedHash(acct.getSalt(), acct.getDataKey());
	}
*/

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
		dos.writeUTF(this.username);
		getKeys().persist(os);
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
		this.username = dis.readUTF();
		Keys keyReconstruct = Keys.newForReconstruction();
		keyReconstruct.reconstruct(is);
		try {
			this.setFromKeys(keyReconstruct);
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

}
