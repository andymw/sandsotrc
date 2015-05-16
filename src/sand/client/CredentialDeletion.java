package sand.client;

import java.io.*;
import java.util.*;
import java.security.*;

import javax.crypto.*;

import common.*;
import common.crypto.*;

public class CredentialDeletion implements Persistable {

	private UUID uuid;
	private final SecretKey macKey;

	public CredentialDeletion(UUID uuid, SecretKey macKey) {
		this.uuid = uuid;
		this.macKey = macKey;
	}

	public void persist(OutputStream os) throws IOException {
		try {
			MACOutputStream out = new MACOutputStream(os, macKey);
			Utils.writeUUID(uuid, out);
			out.doFinal();
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @throws {@link IntegrityException} if the integrity of the
	 * reconstructed CredentialDeletion is invalid. */
	public void reconstruct(InputStream is) throws IOException {
		try {
			MACInputStream in = new MACInputStream(is, macKey);
			uuid = Utils.readUUID(in);
			in.doFinal();
		} catch (InvalidKeyException e) {
			throw new IOException(e);
		}
	}

}
