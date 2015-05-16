package sand.server;

import java.io.*;
import java.util.*;

import common.*;
import sand.common.*;

/**
 * Encapsulates all the info maintained by the server about a specific user account.
 */
class Account implements Persistable {

	Keys keys;
	long revNum;
	long minRev;
	String loginLocation;
	UUID uuid;

	public Account() {
		this(Keys.newForReconstruction());
	}

	public Account(Keys keys) {
		this.keys = keys;
		revNum = minRev = 0L;
		uuid = UUID.randomUUID();
		loginLocation = ""; // "Brazil" for testing
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		keys.persist(os);
		dos.writeLong(revNum);
		dos.writeLong(minRev);
		dos.writeUTF(loginLocation);
		dos.writeLong(uuid.getMostSignificantBits());
		dos.writeLong(uuid.getLeastSignificantBits());
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		if (keys == null) keys = Keys.newForReconstruction();
		keys.reconstruct(is);
		revNum = dis.readLong();
		minRev = dis.readLong();
		loginLocation = dis.readUTF();
		uuid = new UUID(dis.readLong(), dis.readLong());
	}

}
