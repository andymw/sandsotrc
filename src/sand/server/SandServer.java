package sand.server;

import java.io.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

import common.*;
import common.admin.*;
import common.data.*;
import common.network.*;
import common.server.*;

public class SandServer implements Runnable {

	public static final int MAX_CLIENTS = 16;
	private static int serverPort = SslDefaults.DEFAULT_SAND_PORT;

	final PersistentKeyValueStore<Account> accounts;
	final PersistentKeyValueStore<Blob> credentials;
	final PersistentKeyValueStore<Persistable> blockedUsers; // used like hash set
	private final ExecutorService pool;
	private int activeClients;
	private volatile boolean running;

	public SandServer() throws IOException {
		accounts = new PersistentKeyValueStore<Account>(
			new File(SandServerProperties.SAND_ACCOUNTS_DIR), 512, 512);
		credentials = new PersistentKeyValueStore<Blob>(
			new File(SandServerProperties.SAND_CREDENTIALS_DIR), 512, 512);
		blockedUsers = new PersistentKeyValueStore<Persistable>(
			new File(SandServerProperties.SAND_BLOCKEDUSERS_DIR), 512, 512);
		pool = Executors.newCachedThreadPool();
		AdminKeyManager.getPublicKey();
	}

	Account getAccount(String username) {
		Account account = new Account();
		return accounts.get(username, account) ? account : null;
	}

	boolean updateAccount(String username, Account account) {
		return accounts.put(username, account);
	}

	synchronized void disconnect() {
		--activeClients;
		notify();
	}

	public void stop() {
		running = false;
	}

	boolean isRunning() {
		return running;
	}

	public void run() {
		SSLServerSocket server = null;
		try {
			//server = SslDefaults.getServerSandSSLSocket(); // 5430
			server = SslDefaults.getServerSSLSocket(serverPort);
		} catch (IOException e) {
			System.err.println("Server could not bind to port " + serverPort);
			return;
		}

		SslDefaults.printServerSocketInfo(server);
		running = true;

		SandLogger.populateSearchEntries(); // get entries into memory.

		while (running) {
			synchronized (this) {
				while (activeClients >= MAX_CLIENTS) {
					try { wait(); } // until we can accept a new client
					catch (InterruptedException e) { running = false; break; }
				}
			}

			try {
				// accept a new client
				SSLSocket socket = (SSLSocket) server.accept();
				System.out.println("Accepting new client...");
				SslDefaults.printSocketInfo(socket);
				pool.submit(new SandClientHandler(socket, this));
				++activeClients;
			} catch (IOException e) {
				System.err.println("Could not accept new client.");
			}
		}

		System.out.println("Server shutting down...");
		pool.shutdown();
		try {
			pool.awaitTermination(15L, TimeUnit.SECONDS);
			accounts.shutdown();
			credentials.shutdown();
		} catch (InterruptedException ignore) { }
	}

	public static void main(final String[] args) {
		if (args.length > 0) { // args[0] could be a port number.
			try {
				serverPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Could not set port. Defaulting to "
					+ SslDefaults.DEFAULT_SAND_PORT);
				serverPort = SslDefaults.DEFAULT_SAND_PORT;
			}
		}

		final Thread serverThread = Thread.currentThread();
		try {
			final SandServer server = new SandServer();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					if (server.isRunning()) {
						server.stop();
						serverThread.interrupt();
					}
				}
			}));
			server.run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) { // why exception
			e.printStackTrace();
		}
	}

}
