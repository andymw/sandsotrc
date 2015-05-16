package sand.client;

import java.io.*;
import java.util.*;

import common.*;

/**
 * "google.com" -> [g1,g2]
 * "g1" -> <h1,u1,p1>
 * "g2" -> <h2,u2,p2>
 * CredentialLookup is the object that represents [g1,g2]
 */
public class CredentialLookup implements Persistable {
	// list of username, password, description. also, picture?
	// Format: "host-entrynum". example: "google.com-1"
	public Set<UUID> credentialTupleKeys;

	public CredentialLookup() {
		this(new HashSet<UUID>());
	}
	public CredentialLookup(Set<UUID> tupleKeys) {
		this.credentialTupleKeys = tupleKeys == null
			? new HashSet<UUID>() : tupleKeys;
	}

	public void put(UUID key) {
		credentialTupleKeys.add(key);
	}

	/**
	 * From Persistable interface
	 * Persist the state of the object to the given DataOutput
	 * Persist order: host, numKeys, keys.
	 */
	public void persist(OutputStream os) throws IOException {
		DataOutputStream output = new DataOutputStream(os);
		output.writeInt(credentialTupleKeys.size());
		for (UUID tupleKey : credentialTupleKeys) {
			Utils.writeUUID(tupleKey, output);
		}
	}

	/**
	 * From Persistable interface
	 * Read an object state from the given DataInput and set the state of
	 * the current object to the reconstructed state.
	 * Persist order: host, numKeys, keys. (see persist)
	 * !! Assumes credentialTupleKeys is not null
	 */
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream input = new DataInputStream(is);
		credentialTupleKeys.clear();
		int tupleCount = input.readInt();
		for (int i = 0; i < tupleCount; ++i) {
			credentialTupleKeys.add(Utils.readUUID(input));
		}
	}

	/**
	 * Format: "host: [ptr1,..,ptrn]"
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if (this.credentialTupleKeys != null) {
			for (UUID credKey : this.credentialTupleKeys) {
				sb.append(credKey.toString());
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	// testing
	public static void main(String[] args) {
		Set<UUID> keys = new HashSet<UUID>();
		keys.add(UUID.randomUUID());
		CredentialLookup lookup = new CredentialLookup(keys);
		System.out.println(lookup.toString());
	}
}
