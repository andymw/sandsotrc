package sotrc.client;

import java.io.IOException;
import java.util.*;

import common.*;
import common.crypto.*;
import common.network.*;

import sotrc.common.*;
import sotrc.client.Chat;

public class ClientManager extends Observable implements Observer {

	private boolean isConnected;
	private boolean adminAuthorized;
	private UserAccount userAccount;
	private ClientToServer clientToServer;

	/**
	 * TODO consider throwing the IOException instead of catching
	 * Currently tries to establish a connection with the server.
	 * (no offline mode)
	 */
	public ClientManager(String host, int port) {
		// sets this.isConnected
		connect(host, port);
		clientToServer.addObserver(this);
	}

	// TODO Setup default hostname and port(s)
	public ClientManager(String host) {
		this(host, SslDefaults.DEFAULT_SOTRC_PORT);
	}
	
	public ClientManager() {
		this(SslDefaults.DEFAULT_HOST, SslDefaults.DEFAULT_SOTRC_PORT);
	}

	public boolean connect(String host) {
		return connect(host, SslDefaults.DEFAULT_SOTRC_PORT);
	}

	public boolean connect(String host, int port) {
		try {
			this.clientToServer = new ClientToServer(host, port);
			clientToServer.start(); // establishes connection
			this.isConnected = true;
			return true;
		} catch (IOException e) {
			System.err.println(e.toString());
			System.out.println("Most likely cause: server is not up and running.");
			this.isConnected = false;
			return false;
		}
	}

	public boolean isConnectedToServer() {
		return isConnected;
	}

	/**
	 * Create User Account
	 * Does not log in.
	 */
	public boolean createAccount(String username, char[] pass) {
		if (!isConnected)
			return false;

		try {
			this.userAccount = clientToServer.createAccount(username, pass);
			if (userAccount == null) {
				return false;
			}

			this.userAccount = null;
			return true; // right?
		} catch (IOException e) {
			System.err.println(e.toString());
			this.userAccount = null; // make sure.
			return false;
		}
	}

	public boolean login(String username, char[] pass) {
		if (!isConnected) {
			System.out.println("Not connected or not authenticated.");
			return false;
		}

		if (userAccount != null) {
			System.out.println("Already logged in?");
			return false;
		}

		try {
			userAccount = clientToServer.login(username, pass);
			if (userAccount == null) {
				System.out.println("Authentication failed.");
				return false;
			}

		} catch (IOException e) {
			System.err.println(e.toString());
		}
		return userAccount != null;
	}

	// Login
	public boolean adminLogin(char[] pass) {
		if (!isConnected) {
			System.out.println("Not connected or not authenticated.");
			return false;
		}

		if (adminAuthorized) {
			System.out.println("Already admin authorized. Strange.");
			return true;
		}

		boolean loggedIn = false;
		try {
			loggedIn = clientToServer.adminLogin(pass);
			if (!loggedIn) {
				System.out.println("Authentication failed.");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		adminAuthorized = loggedIn;
		return loggedIn;
	}

	public List<String> adminGetUserReports(int entries) {
		if (!adminAuthorized) {
			System.out.println("Unauthorized");
			return null;
		}

		try {
			return clientToServer.getUserReports(entries);
		} catch (IOException e) {
			System.err.println(e.toString());
			return null;
		}
	}

	/**
	 * Returns true if not isConnectedToServer already.
	 */
	public void logout() throws IOException {
		if (!isConnectedToServer() || this.clientToServer == null) {
			return; // nothing to do, success.
		}

		if (this.userAccount == null && !adminAuthorized) {
			System.out.println("Not logged in, not an admin.");
			return;
		}

		this.clientToServer.logout();

		if (adminAuthorized) {
			System.out.println("Logging out admin.");
			adminAuthorized = false;
			return;
		}

		//TODO need to logout on serverToClient also

		this.userAccount = null;
		this.adminAuthorized = false;
	}

	public boolean changePassword(char[] oldPassword, char[] newPassword) {
		if (userAccount == null) return false; // not logged in?
		if (!isConnected)
			return false;
		if (newPassword == null) return false;

		UserAccount ua = null;
		try {
			ua = clientToServer.changePassword(userAccount, oldPassword, newPassword);
		} catch (IOException e) {
			return false;
		}

		if (ua == null) return false;

		userAccount = ua;
		return true;
	}

	public String getCurrentUsername() {
		if (userAccount == null) return null;
		return userAccount.getUsername();
	}

	/**
	 * block == false means unblock.
	 */
	public boolean blockUser(String username, boolean block) {
		if (!adminAuthorized) {
			System.out.println("Unauthorized");
			return false;
		}

		try {
			return clientToServer.blockUser(username, block);
		} catch (IOException e) {
			System.out.println("Exception, unsuccessful operation.");
			return false;
		}
	}

	public boolean deleteAccount() {
		if (userAccount == null) return false; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: not connected to server.");
			return false;
		}

		try {
			StatusCode code = clientToServer.deleteAccount(userAccount);
			userAccount = null;
			adminAuthorized = false;
			return code.equals(StatusCode.OK);
		} catch (IOException e) {
			System.err.println(e.toString());
			return false;
		}
	}

	public boolean sendInvite(String username) {
		if (username.equals("")) {
			return false;
		}
		boolean success = true;

		// TODO need to decide if this is sync or async
		// try{
		// 	success = success && clientToServer.sendInvite(username);
		// } catch (IOException e) {
		// 	System.err.println(e.toString());
		// 	return false;
		// }

		return success;
	}

	public Chat startChat(String otherParticipant) {
		try {
			Chat chatTuple = clientToServer.startChat(this.userAccount,otherParticipant);
			if (chatTuple == null) return null;
			addContact(otherParticipant);
			return chatTuple;
		} catch (IOException e) {
			System.err.println("Error connecting to other user");
			return null;
		}
	}

	public boolean sendMessage(UUID uuid, String message) {
		if (uuid == null || message.length() < 1 || clientToServer.activeChats.size() == 0) {
			return false;
		}

		// find chat uuid in activeChats. if not found, handle appropriately.
		// call clientToServer
		try {
			StatusCode sendCode = clientToServer.sendMessage(uuid, message);
			return sendCode != null && sendCode.equals(StatusCode.OK);
		} catch (IOException e) {
			System.err.println(e.toString());
			return false;
		}
	}

	/**
	 * Must be currently chatting with username.
	 */
	public boolean addContact(String username) {
		if (clientToServer.findContactByName(username) != null) {
			return true;
		}

		try {
			for (Chat c : clientToServer.activeChats.values()) {
				if (c.containsParticipant(username)) {
					Tuple<Long, ?> tup = c.getUserFingerprintState(username);
					return clientToServer.addContact(userAccount, new Contact(username, tup.x));
				}
			}
		} catch (IOException e) {
			System.out.println("Error adding contact");
			return false;
		}
		return false;
	}

	public boolean removeContact(String username) {
		Contact ct = clientToServer.findContactByName(username);
		if (ct != null) {
			try {
				return clientToServer.removeContact(ct);
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	public boolean isInContacts(String username) {
		for (Contact c : clientToServer.contactsSet ) {
			if (c.getUsername().equals(username)) {
				return true;
			}
		}
		return false;
	}

	public boolean reportUser(String username) {
		if (username == null || username.equals("")) {
			return false;
		}

		try {
			StatusCode code = clientToServer.reportUser(username);
			return code.equals(StatusCode.OK);
		} catch (IOException e) {
			System.err.println(e.toString());
			return false;
		}
	}

	public boolean endChat(UUID chatID) {
		try {
			if (chatID == null) {
				System.out.println("Null chat ID passed to manager");
				return false;
			}
			StatusCode endStatus = clientToServer.endChat(chatID);
			if (endStatus == null || !endStatus.equals(StatusCode.OK)) {
				return false;
			}
			return true;
		} catch (IOException e) {
			System.err.println("Something went wrong unable to end chat");
			return false;
		}
	}

	public void disconnect() throws IOException {
		if (isConnected) {
			clientToServer.disconnect();
			isConnected = false;
		}
	}

	public void shutdown() {
		try { disconnect(); }
		catch (IOException ignore) { }
	}

	public int getNumberOfActiveChats() {
		return clientToServer.activeChats.size();
	}
		
		public long getUserFingerprint() {
				return CryptoUtils.getKeyFingerprint(userAccount.getPublicKey());
		}

	@Override
	public void update(Observable o, Object object) {
		// pass it to GUI 
		if (object instanceof AsyncNotifications) {
			AsyncNotifications notifs = (AsyncNotifications) object;
			if (notifs.type == 0) {
				clientToServer.activeChats.put(notifs.chat.getUUID(), notifs.chat);
			} else if (notifs.type == 2) { // user left
				// nothing to do?
			} else if (notifs.type == 3) { // chat ended
				clientToServer.activeChats.remove(notifs.chat.getUUID());
			}
			setChanged();
			notifyObservers(notifs);
		} else {
			System.err.println("Don't know what this is, man.");
		}
	}

	/**
	 * If print matches, repersists the contact with authenticated state
	 *   to the server, so upon next login, contact shows as authenticated.
	 */
	public boolean verifyUserAndRepersist(long print, String selectedUser) {
		for ( Contact c : clientToServer.contactsSet ) {
			if (c.getUsername().equals(selectedUser)) {
				if (!c.authenticate(print)) return false;
				// repersist contact as authenticated.
				try {
					if (!clientToServer.addContact(userAccount, c))
						System.err.println("Verified, but unable to persist?");
				} catch (IOException e) {
					System.err.println("Verified, but unable to persist?");
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Assumes at time of call that contactsSet is correct.
	 */
	public Set<Contact> getContacts() {
		return Collections.unmodifiableSet(clientToServer.contactsSet);
	}
}
