package sotrc.server;

import java.io.*;

import common.*;

public class Encrypted implements Persistable {
	
	private byte[] ciphertext;

	public Encrypted() {
		ciphertext = null;
	}

	@Override
	public void persist(OutputStream out) throws IOException {
		out.write(ciphertext); // out will really be a packetized stream
	}

	/** This is really required to be a PacketizedInputStream. */
	@Override
	public void reconstruct(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Utils.streamTransfer(in, baos);
		ciphertext = baos.toByteArray();
	}

}
