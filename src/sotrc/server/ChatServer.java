package sotrc.server;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import common.server.*;
import common.network.*;

public class ChatServer implements BaseServer.Handler {

	private Set<SocketChannel> clients;
	private ByteBuffer buffer, addressBuf;

	public ChatServer() {
		clients = new HashSet<SocketChannel>();
		buffer = ByteBuffer.allocateDirect(1024);
		addressBuf = ByteBuffer.allocateDirect(32);
	}

	public boolean shouldAccept() {
		return true;
	}

	public void onAccept(SocketChannel client) {
		clients.add(client);
	}

	public boolean read(SocketChannel client) {
		boolean success = true;
		try {
			int numRead = client.read(buffer);
			if (numRead < 0) {
				success = false;
			} else {
				addressBuf.put(client.getRemoteAddress().toString().getBytes());
				addressBuf.put(": ".getBytes());
				addressBuf.flip();
				buffer.flip();
				for (SocketChannel c : clients) {
					if (!c.getRemoteAddress().equals(client.getRemoteAddress())) {
						c.write(addressBuf);
						c.write(buffer);
						addressBuf.rewind();
						buffer.rewind();
					}
				}
				addressBuf.clear();
			}
			buffer.clear();
		} catch (IOException e) {
		}
		return success;
	}

	public void onDisconnect(SocketChannel client) {
		clients.remove(client);
	}

	public static void main(String[] args) {
		new BaseServer(SslDefaults.DEFAULT_SOTRC_PORT, new ChatServer()).run();
	}

}
