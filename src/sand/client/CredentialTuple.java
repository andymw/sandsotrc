package sand.client;

import java.io.*;
import java.util.*;

import common.*;

public class CredentialTuple implements Persistable {
	private UUID uuid;
	public long revNum;
	public String host; // site, device, or service
	public String username;
	public String password;
	public String description; // possibly very long
	// also, picture?

	/**
	 * Null constructor. Should probably be used only by
	 *   SandClient for get/put/rm ops.
	 */
	private CredentialTuple() {
		this(null, null, null, null, null);
	}

	/**
	 * This CredentialTuple object CANNOT be persisted until it has first been reconstructed.
	 */
	public static CredentialTuple newForReconstruction() {
		return new CredentialTuple();
	}

	public CredentialTuple(UUID uuid,
				String host, String username, String password) {
		this(uuid, host, username, password, null);
	}

	public CredentialTuple(UUID uuid, String host,
			String username, String password, String description) {
		this.uuid = uuid;
		this.host = host;
		this.username = username;
		this.password = password;
		this.description = description;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * From Persistable interface
	 * Persist the state of the object to the given DataOutput
	 * Assumption: fields are not null at this point
	 */
	public void persist(OutputStream os) throws IOException {
		DataOutputStream output = new DataOutputStream(os);
		Utils.writeUUID(uuid, output);
		output.writeLong(revNum);
		output.writeUTF(host);
		output.writeUTF(username);
		output.writeUTF(password);
		output.writeUTF(description);
	}
	/**
	* From Persistable interface
	* Read an object state from the given DataInput and set the state of
	* the current object to the reconstructed state.
	*/
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream input = new DataInputStream(is);
		uuid = Utils.readUUID(input);
		revNum = input.readLong();
		host = input.readUTF();
		username = input.readUTF();
		password = input.readUTF();
		description = input.readUTF();
	}

	/**
	 * String format: "uuid\thost\tusername\tpassword\tdescription"
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String uuidString = this.uuid == null ? "!!!!!!!!" : this.uuid.toString();
		sb.append(uuidString.substring(Math.max(0, uuidString.length() - 6)));
		sb.append("\t");
		sb.append(this.host == null ? "[null]" : this.host);
		sb.append("\t");
		sb.append(this.username == null ? "[null]" : this.username);
		sb.append("\t");
		sb.append(this.password == null ? "[null]" : this.password);
		sb.append("\t");
		sb.append(this.description == null ? "[null]" : this.description);
		return sb.toString();
	}

	/**
	 * String format: "uuid\thost\tusername\tpassword"
	 */
	public String toStringNoDescription() {
		StringBuilder sb = new StringBuilder();
		String uuidString = this.uuid == null ? "!!!!!!!!" : this.uuid.toString();
		sb.append(uuidString.substring(Math.max(0, uuidString.length() - 6)));
		sb.append("\t");
		sb.append(this.host == null ? "[null]" : this.host);
		sb.append("\t");
		sb.append(this.username == null ? "[null]" : this.username);
		sb.append("\t");
		sb.append(this.password == null ? "[null]" : this.password);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof CredentialTuple)) return false;

		CredentialTuple tup = (CredentialTuple) o;

		return tup.uuid.toString().equals(this.uuid.toString())
			&& tup.host.equals(this.host)
			&& tup.username.equals(this.username)
			&& tup.password.equals(this.password)
			&& tup.description.equals(this.description);
	}
}
