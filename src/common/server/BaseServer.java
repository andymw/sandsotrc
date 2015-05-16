package common.server;

import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

public class BaseServer implements Runnable {

	public interface Handler {
		/**
		 * @return true if the server should accept another client connection
		 */
		public boolean shouldAccept();
		/**
		 * Called when the server has accepted a new client connection
		 */
		public void onAccept(SocketChannel client);
		/**
		 * Called when there is data available to read from the client
		 * @return true if the server should maintain the client connection
		 *   or false if it should disconnect
		 */
		public boolean read(SocketChannel client);
		/**
		 * Called when a client has disconnected
		 */
		public void onDisconnect(SocketChannel client);
	}

	private Selector selector;
	private ServerSocketChannel ssc;
	private int port;
	private Handler handler;

	public static enum State {
		UNINITIALIZED, INITIALIZED, RUNNING, STOPPING, STOPPED, ERROR
	};
	private volatile State state = State.UNINITIALIZED;

	public BaseServer(int port, Handler handler) {
		this.port = port;
		this.handler = handler;
	}

	public void init() {
		if (state != State.UNINITIALIZED)
			return;
		try {
			selector = Selector.open();
			ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress(port));
			ssc.configureBlocking(false);
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			state = State.INITIALIZED;
		} catch (IOException e) {
			state = State.ERROR;
		}
	}

	public void run() {
		synchronized (this) {
			if (state == State.UNINITIALIZED)
				init();
			if (state == State.ERROR)
				return;
			state = State.RUNNING;
		}

		while (state == State.RUNNING) {
			try {
				selector.select();
			} catch (IOException e) {
				state = State.ERROR;
				return;
			}

			Iterator<SelectionKey> iter;
			for (iter = selector.selectedKeys().iterator(); iter.hasNext();) {
				SelectionKey key = iter.next();
				iter.remove();
				if (key.isAcceptable() && handler.shouldAccept()) {
					// accept a new client connection
					try {
						SocketChannel client = ssc.accept();
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ);
						handler.onAccept(client);
					} catch (ClosedChannelException ignore) {
					} catch (IOException e) {
						state = State.ERROR;
						return;
					}
				}
				else if (key.isReadable()) {
					SocketChannel client = (SocketChannel) key.channel();
					if (!handler.read(client)) {
						key.cancel();
						handler.onDisconnect(client);
					}
				}
			}
		}
		state = State.STOPPED;
	}

	public synchronized void stop() {
		state = State.STOPPING;
	}

	public synchronized State getState() {
		return state;
	}

}
