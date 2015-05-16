package sotrc.common;

import java.io.*;
import common.*;

/**
 * Basically a persistable string.
 * To be wrapped in Encrypted to send over.
 */
public class Message implements Persistable {
	private String message;

	public Message(String message) {
		this.message = message;
	}

	@Override public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(message);
	}
	@Override public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		this.message = dis.readUTF();
	}

	@Override
	public String toString() {
		return this.message;
	}
}
