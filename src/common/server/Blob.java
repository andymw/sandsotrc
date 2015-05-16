package common.server;

import java.io.*;
import java.net.*;
import java.util.*;

import common.*;
import common.network.*;

/**
 * A pseudo-persistable class facilitating transfers of binary data between
 * the network and Key-Value store.
 */
public class Blob implements Persistable {

	private final Socket conn;
	private UUID uuid;

	/** Constructs a blob for reading a tuple from the client (network). */
	public Blob(UUID uuid, Socket conn) {
		this.conn = conn;
		this.uuid = uuid;
	}

	@Override
	// called when transfering from client to KVstore
	public void persist(OutputStream os) throws IOException {
		Utils.writeUUID(uuid, os); // write to kvstore
		// transfer from client to kvstore
		Utils.streamTransfer(new PacketizedInputStream(conn.getInputStream()), os);
	}

	@Override
	// called when transfering from KVstore to client
	public void reconstruct(InputStream is) throws IOException {
		uuid = Utils.readUUID(is); // read from kvstore
		Utils.writeUUID(uuid, conn.getOutputStream()); // send to client
		PacketizedOutputStream packout = new PacketizedOutputStream(
			conn.getOutputStream()); // transfer from kvstore to client
		Utils.streamTransfer(is, packout);
		packout.finish();
	}

}
