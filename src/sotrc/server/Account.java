package sotrc.server;

import java.io.*;
import java.util.*;

import common.*;
import sotrc.common.*;

/**
 * Encapsulates all the info maintained by the server about a specific user account.
 */
class Account implements Persistable {

	Keys keys;
	UUID uuid;
	Set<UUID> contactUUIDs;

	public Account() {
		this(Keys.newForReconstruction());
	}

	public Account(Keys keys) {
		this.keys = keys;
		uuid = UUID.randomUUID();
		contactUUIDs = new HashSet<UUID>();
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		keys.persist(os);
		Utils.writeUUID(uuid, os);
		dos.writeInt(contactUUIDs.size());
		for (UUID uuid : contactUUIDs) {
			Utils.writeUUID(uuid, dos);
		}
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		if (keys == null) keys = Keys.newForReconstruction();
		keys.reconstruct(dis);
		uuid = Utils.readUUID(dis);
		int size = dis.readInt();
		contactUUIDs = new HashSet<UUID>(size);
		for (int i = 0; i < size; i++) {
			contactUUIDs.add(Utils.readUUID(dis));
		}
	}

}
