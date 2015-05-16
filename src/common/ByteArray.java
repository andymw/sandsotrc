package common;

import java.io.*;

public class ByteArray implements Persistable {

	private byte[] arr;

	public ByteArray() { }
	
	public ByteArray(byte[] arr) {
		this.arr = arr;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		Utils.writeArray(arr, os);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		arr = Utils.readArray(is);
	}

	public byte[] getArray() { return arr; }

}
