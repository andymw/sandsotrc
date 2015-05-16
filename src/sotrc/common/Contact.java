package sotrc.common;

import java.io.*;
import java.util.UUID;

import common.*;

public class Contact implements Persistable {
	private String username;

	private long keyFingerprint;

	private boolean authenticated;

	private UUID uuid;

	public Contact(String username, long keyFingerprint) {
		this.username = username;
		this.keyFingerprint = keyFingerprint;
		this.uuid = UUID.randomUUID();
	}

	public Contact() { }

	@Override
	public boolean equals(Object other) {
		return other instanceof Contact
			&& ((Contact)other).username.equals(this.username)
			&& ((Contact)other).keyFingerprint == this.keyFingerprint;
	}

	public boolean authenticate(long keyFingerprint) {
		return (authenticated = (keyFingerprint == this.keyFingerprint));
	}

	public String getUsername() { return username; }

	public long getKeyFingerprint() { return keyFingerprint; }

	public boolean isAuthenticated() { return authenticated; }

	public UUID getUUID() { return uuid; }

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		Utils.writeUUID(uuid, dos);
		dos.writeUTF(username);
		dos.writeLong(keyFingerprint);
		dos.write(authenticated ? 1 : 0);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		uuid = Utils.readUUID(dis);
		username = dis.readUTF();
		keyFingerprint = dis.readLong();
		authenticated = (dis.read() == 1);
	}

	public String getContactDisplayString() {
	    return username + (authenticated ? SotrcProperties.VERIFIED_SUFFIX : "");
	}

	@Override
	public String toString() {
		return username;
	}
}
