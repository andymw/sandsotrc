package common.network;

import java.io.*;

import static common.Utils.*;

/**
 * Splits up the stream into specified packet sizes
 * to be received by PacketizedInputStream to read chunks at a time
 */
public class PacketizedOutputStream extends OutputStream {

	private OutputStream out;
	private int packetSize;
	private byte[] packetBuf;
	private int ptr;
	private boolean finished;

	public PacketizedOutputStream(OutputStream out, int packetSize) throws IOException {
		this.out = out;
		this.packetSize = packetSize;
		packetBuf = new byte[packetSize];
		writeInt(packetSize, out);
	}

	public PacketizedOutputStream(OutputStream out) throws IOException {
		this(out, 1024);
	}

	private void sendPacket() throws IOException {
		writeInt(ptr, out); // size of packet
		out.write(packetBuf, 0, ptr);
		ptr = 0;
	}

	@Override
	public void write(int b) throws IOException {
		packetBuf[ptr++] = (byte)b;
		if (ptr == packetSize)
			sendPacket();
	}

	@Override
	public void write(byte[] b, int offs, int len) throws IOException {
		while (len > 0) {
			int numToCopy = Math.min(len, packetSize - ptr);
			System.arraycopy(b, offs, packetBuf, ptr, numToCopy);
			len -= numToCopy;
			offs += numToCopy;
			ptr += numToCopy;
			if (ptr == packetSize)
				sendPacket();
		}
	}

	@Override
	public void flush() { }

	public void finish() throws IOException {
		if (!finished) {
			sendPacket();
			finished = true;
		}
		out.flush();
	}

	public void restart() throws IOException {
		if (!finished)
			throw new IOException("Current transmission is not finished.");
		finished = false;
		writeInt(packetSize, out);
	}

	@Override
	public void close() throws IOException {
		finish();
		out.flush();
		out.close();
	}

}
