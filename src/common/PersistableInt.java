package common;

import java.io.*;

public class PersistableInt implements Persistable {

	int i;

	public PersistableInt(int i) {
		this.i = i;
	}

	public PersistableInt() {

	}

	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(i);
	}

	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		i = dis.readInt();
	}

	public int getInt() {
		return i;
	}

}
