package common.network;

import java.io.*;

import static common.Utils.*;

/**
 * Reads "packets" by PacketizedOutputStream by chunks.
 */
public class PacketizedInputStream extends InputStream {

	private InputStream in;
	private int packetSize;
	private byte[] packetBuf;
	private int ptr;
	private int limit;

	public PacketizedInputStream(InputStream in) throws IOException {
		this.in = in;
		reset();
	}

	private void recvPacket() throws IOException {
		limit = readInt(in);
		if (in.read(packetBuf, 0, limit) != limit)
			throw new EOFException("Packet ended prematurely");
		ptr = 0;
	}

	private boolean recvOrEnd() throws IOException {
		if (ptr == limit) {
			if (limit < packetSize)
				return true;
			recvPacket();
		}
		return false;
	}

	@Override
	public int read() throws IOException {
		if (recvOrEnd())
			return -1;
		return packetBuf[ptr++];
	}

	@Override
	public int read(byte[] b, int offs, int len) throws IOException {
		int totalRead = 0;
		while (len > 0) {
			if (recvOrEnd())
				break;
			int numToRead = Math.min(len, limit - ptr);
			System.arraycopy(packetBuf, ptr, b, offs + totalRead, numToRead);
			totalRead += numToRead;
			ptr += numToRead;
			len -= numToRead;
		}
		return totalRead == 0 ? -1 : totalRead;
	}

	public void reset() throws IOException {
		packetSize = readInt(in);
		packetBuf = new byte[packetSize];
		ptr = 0;
		recvPacket();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
