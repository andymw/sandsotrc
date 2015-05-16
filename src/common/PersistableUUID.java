package common;

import java.io.*;
import java.util.UUID;

public class PersistableUUID implements Persistable {

	private UUID uuid;

	public PersistableUUID() { }

	public PersistableUUID(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public void persist(OutputStream out) throws IOException {
		Utils.writeUUID(uuid, out);
	}

	@Override
	public void reconstruct(InputStream in) throws IOException {
		uuid = Utils.readUUID(in);
	}

	public UUID getUUID() {
		return this.uuid;
	}
}
