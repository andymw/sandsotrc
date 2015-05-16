package sotrc.server;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

import common.*;
import common.data.*;
import common.network.*;
import sotrc.common.*;

public class SotrcServer implements Runnable {

	public static final int MAX_CLIENTS = 32;
	private static int serverPort = SslDefaults.DEFAULT_SOTRC_PORT;

	final PersistentKeyValueStore<Account> accounts;
	final PersistentKeyValueStore<Persistable> blockedUsers; // used like hash set
	final PersistentKeyValueStore<Encrypted> contacts; // encrypted blobs

	final Map<String, Communicator<Notification, ClientAction>> loggedInUsers;
	final Map<UUID, Chat> chatLookup; // chat UUID -> chat
	final Map<String, Set<UUID>> userToChatIDs; // username -> chat

	private final ExecutorService pool;

	private boolean running;
	private int activeClients;

	public SotrcServer() throws IOException {
		accounts = new PersistentKeyValueStore<Account>(
			new File(ServerProperties.SOTRC_ACCOUNTS_DIR), 512, 512);
		blockedUsers = new PersistentKeyValueStore<Persistable>(
			new File(ServerProperties.SOTRC_BLOCKEDUSERS_DIR), 512, 512);
		contacts = new PersistentKeyValueStore<Encrypted>(
			new File(ServerProperties.SOTRC_CONTACTS_DIR), 512, 512);
		pool = Executors.newCachedThreadPool();
		chatLookup = new ConcurrentHashMap<UUID, Chat>();
		userToChatIDs = new ConcurrentHashMap<String, Set<UUID>>();
		loggedInUsers = new ConcurrentHashMap<String, Communicator<Notification, ClientAction>>();
	}

	public synchronized void disconnect() {
		activeClients--;
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * API for userToChatIDs map.
	 */
	synchronized void addUserToChat(String username, Chat chat) {
		Set<UUID> userChatList = null;
		if (!this.userToChatIDs.containsKey(username)) {
			userChatList = new HashSet<UUID>();
		} else {
			userChatList = userToChatIDs.get(username);
		}
		userChatList.add(chat.getUUID());
		this.userToChatIDs.put(username, userChatList);
	}

	public void run() {
		SSLServerSocket pullServer = null;
		try {
			//server = SslDefaults.getServerSandSSLSocket(); // 5430
			pullServer = SslDefaults.getServerSSLSocket(serverPort);
		} catch (IOException e) {
			System.err.println("Server could not bind to port " + serverPort);
			return;
		}

		SslDefaults.printServerSocketInfo(pullServer);
		running = true;

		UserReports.populateSearchEntries(); // get user reports into memory.

		while (running) {
			synchronized (this) {
				while (activeClients >= MAX_CLIENTS) {
					try { wait(); }
					catch (InterruptedException e) { running = false; break; }
				}
			}

			try {
				SSLSocket socket = (SSLSocket) pullServer.accept();
				System.out.println("Accepting new client...");
				SslDefaults.printSocketInfo(socket);
				ClientHandler handler = new ClientHandler(socket, this);
				Communicator<?, ?> c = handler.getCommunicator();
				c.setExecutor(pool); // requests will be handled in thread pool
				pool.submit(c); // pool also runs listening loop
			} catch (IOException e) {
				System.err.println("Could not accept new client.");
			}
		}

		System.out.println("Server shutting down...");
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) { // args[0] could be a port number.
			try {
				serverPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Could not set port. Defaulting to "
					+ SslDefaults.DEFAULT_SOTRC_PORT);
				serverPort = SslDefaults.DEFAULT_SOTRC_PORT;
			}
		}

		new SotrcServer().run();
	}

}
