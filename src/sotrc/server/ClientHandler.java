package sotrc.server;

import java.io.*;
import java.net.*;
import java.util.*;

import common.*;
import common.admin.*;
import common.crypto.*;
import common.network.*;
import static common.network.StatusCode.*;

import sotrc.common.*;
import static sotrc.common.NewChallengeResponse.*;
import static sotrc.common.Notification.*;

/**
 * Manages the "pull" side of the SOTRC protocol:
 * handles requests from the client.
 */
public class ClientHandler {

	private final SotrcServer server;
	private final StatusCode code;
	final Communicator<Notification, ClientAction> comm;

	String currentUsername;
	Account currentAccount;
	boolean admin = false;

	public ClientHandler(Socket conn, SotrcServer server) throws IOException {
		this.comm = new Communicator<Notification, ClientAction>(
			conn.getInputStream(),
			conn.getOutputStream(),
			new PersistableFactory<ClientAction>() {
				public ClientAction newT() { return new ClientAction(); }
			},
			new Communicator.RequestHandler<ClientAction>() {
				public void processRequest(int id, ClientAction req) {
					try {
						handleAction(id, req);
					} catch (IOException e) {
						System.err.println("Got IOException in client handler: "
							+ e.getMessage() + ". Disconnecting.");
						comm.shutdown();
					}
				}
			}
		);
		this.server = server;
		this.code = new StatusCode(null);
	}

	public Communicator<Notification, ClientAction> getCommunicator() {
		return comm;
	}

	private StatusCode readCodeFrom(int reqId, String username) throws IOException {
		Communicator<Notification, ClientAction> comm = server.loggedInUsers.get(username);
		comm.readResponse(reqId, code);
		return code;
	}

	private int push(Notification not, String username) throws IOException {
		Communicator<Notification, ClientAction> comm = server.loggedInUsers.get(username);
		return comm.sendRequest(not);
	}

	private void statusCode(int reqId, String code) throws IOException {
		statusCode(reqId, comm, code);
	}

	private void statusCode(int reqId, Communicator<?, ?> comm, String code)
			throws IOException {
		comm.sendResponse(reqId, this.code.setTo(code));
	}

	private boolean checkUserAuthenticated(int reqId) throws IOException {
		if (currentAccount == null)
			statusCode(reqId, UNAUTHORIZED);
		return currentUsername != null;
	}
	
	private boolean checkLoggedOut(int reqId) throws IOException {
		if (currentAccount != null || currentUsername != null) {
			statusCode(reqId, BAD_REQUEST);
			return false;
		} else return true;
	}

	// assumes currently logged out
	private void handleUserCreate(int reqId, String username) throws IOException {
		if (username == null || username.contains(" ")
			|| username.contains(SotrcProperties.VERIFIED_SUFFIX)) {
			statusCode(reqId, FORBIDDEN);
			return;
		}
		Account account = new Account();
		if (server.accounts.get(username, account)) {
			statusCode(reqId, FORBIDDEN);
		} else {
			statusCode(reqId, CONTINUE);
			comm.readResponse(reqId, account.keys);
			boolean success = server.accounts.put(username, account);
			statusCode(reqId, success ? OK : SERVER_ERROR);
		}
	}

	private boolean runChallengeResponse(int reqId, Keys keys) throws IOException {
		Challenge ch = Challenge.newChallenge();
		comm.sendResponse(reqId, ch);
		statusCode(reqId, CONTINUE);
		Response re = Response.newForVerifying();
		comm.readResponse(reqId, re);
		return re.verify(ch, keys.getPublicKey());
	}

	private boolean runAdminChallengeResponse(int reqId) throws IOException {
		Challenge ch = Challenge.newChallenge();
		comm.sendResponse(reqId, ch);
		statusCode(reqId, CONTINUE);
		Response re = Response.newForVerifying();
		comm.readResponse(reqId, re);
		return re.verify(ch, AdminKeyManager.getPublicKey());
	}
	
	private void handleLogin(int requestId, String username)
			throws IOException, InterruptedException {
		if (server.blockedUsers.containsKey(username)) { // login blocked
			Thread.sleep(1000); // prevent brute-force attack
			statusCode(requestId, UNAUTHORIZED);
			return;
		}
		Account account = new Account();
		if (!server.accounts.get(username, account)) {
			statusCode(requestId, NOT_FOUND); // invalid username
			return;
		}

		statusCode(requestId, ACCEPTED);
		comm.sendResponse(requestId, account.keys);
		if (runChallengeResponse(requestId, account.keys)) {
			currentUsername = username;
			currentAccount = account;
			server.loggedInUsers.put(username, comm);
			statusCode(requestId, OK);
		} else {
			Thread.sleep(2000); // prevent brute-force attack
			statusCode(requestId, UNAUTHORIZED);
		}
	}

	private void handlePasswordChange(int reqId, String username) throws IOException {
		if (!username.equals(currentUsername)) {
			statusCode(reqId, BAD_REQUEST);
			return;
		}

		// you cannot change passwords if you have active chats.
		if (server.userToChatIDs.containsKey(username)
				&& server.userToChatIDs.get(username).size() > 0) {
			System.out.println("User attempted to change password with active chats.");
			statusCode(reqId, FORBIDDEN);
			return;
		}

		statusCode(reqId, ACCEPTED);
		comm.sendResponse(reqId, currentAccount.keys); // send user's (encrypted) keys
		if (runChallengeResponse(reqId, currentAccount.keys)) {
			statusCode(reqId, CONTINUE);
			comm.readResponse(reqId, currentAccount.keys); // get keys with new password
			boolean success = server.accounts.put(currentUsername, currentAccount);
			statusCode(reqId, success ? OK : SERVER_ERROR);
		} else {
			statusCode(reqId, UNAUTHORIZED);
		}
	}

	private boolean negotiateKeys(int aId, int bId, String otherParticipant)
			throws IOException {
		Communicator<?, ?> bComm = server.loggedInUsers.get(otherParticipant);
		statusCode(aId, ACCEPTED); // notify A that B agrees to key exchange
		ByteArray baPLA = new ByteArray(currentAccount.keys.getPublicKey().getEncoded());
		bComm.sendResponse(bId, baPLA); // send B A's long-term public key (PLA)
		statusCode(bId, bComm, CONTINUE); // ask B to send over ephemeral key
		Account bAccount = new Account(); // get B's account info (l.t. pub key)
		server.accounts.get(otherParticipant, bAccount); // assume succeeds
		comm.sendResponse(aId, new ByteArray( // send B's long-term pub key to A
				bAccount.keys.getPublicKey().getEncoded())); // encoded in X.509
		Persistable bKey = bComm.readResponse(bId, new ByteArray()); // get B's key
		comm.sendResponse(aId, bKey); // send B's ephemeral key to A
		statusCode(aId, CONTINUE); // ask A to send encrypted session keys
		Persistable sKeys = comm.readResponse(aId, SessionKeys.newForTransmission());
		statusCode(bId, bComm, ACCEPTED); // notify B we're about to send keys
		bComm.sendResponse(bId, sKeys); // send encrypted session keys to B
		// make sure B's OK, then send OK to A
		boolean success = readCodeFrom(bId, otherParticipant).equals(OK);
		statusCode(aId, success ? OK : REJECTED);
		return success;
	}

	private void handleStartChat(int reqId, String otherParticipant) throws IOException {
		if (currentUsername.equals(otherParticipant)) {
			statusCode(reqId, BAD_REQUEST); // can't chat with yourself
			return;
		}
		if (!server.loggedInUsers.containsKey(otherParticipant)) {
			statusCode(reqId, NOT_FOUND);
			return; // B not logged in
		}
		String[] users = new String[] { otherParticipant, currentUsername };
		Chat chat = new Chat(users);
		int bId = push(ChatStart(chat.getUUID(), currentUsername), otherParticipant);
		if (!readCodeFrom(bId, otherParticipant).equals(OK)) {
			statusCode(reqId, REJECTED);
			return; // B rejected chat
		}
		if (!negotiateKeys(reqId, bId, otherParticipant)) {
			return;
		}
		comm.sendResponse(reqId, new PersistableUUID(chat.getUUID()));
		statusCode(reqId, OK);

		server.chatLookup.put(chat.getUUID(), chat);
		server.addUserToChat(currentUsername, chat);
		server.addUserToChat(otherParticipant, chat);
	}

	private void removeCurrentUserFromChat(UUID chatUUID) throws IOException {
		Chat chat = server.chatLookup.get(chatUUID);
		if (chat == null) return;
		chat.removeParticipant(currentUsername);
		boolean ending = chat.getActiveParticipantCount() < 2;
		for (String participant : chat.participantSet()) {
			int id = push(ending ? ChatEnd(chat.getUUID())
								 : UserLeft(chat.getUUID(), currentUsername),
								 	participant);
			readCodeFrom(id, participant); // should be OK
		}
	}

	private void handleLeaveChat(int reqId, UUID chatUUID) throws IOException {
		if (!server.chatLookup.containsKey(chatUUID)) {
			statusCode(reqId, NOT_FOUND);
			return;
		}

		server.chatLookup.get(chatUUID);
		// you can only send messages to chats you belong to.
		if (!server.userToChatIDs.get(currentUsername).contains(chatUUID)) {
			System.out.println("User " + currentUsername + " not in chat " + chatUUID.toString());
			statusCode(reqId, NOT_FOUND); // chat doesn't exist
			return;
		}

		statusCode(reqId, OK);
		// remove handles when participant count < 2.
		removeCurrentUserFromChat(chatUUID);
	}

	private void handleSendMessage(int reqId, UUID chatUUID) throws IOException {
		// the chat needs to exist, firstly.
		if (!server.chatLookup.containsKey(chatUUID)) {
			System.out.println("Chat not found for " + chatUUID.toString());
			statusCode(reqId, NOT_FOUND); // chat doesn't exist
			return;
		}

		// you can only send messages to chats you belong to.
		if (!server.userToChatIDs.get(currentUsername).contains(chatUUID)) {
			System.out.println("Chat " + chatUUID.toString() + " not found for user " + currentUsername);
			statusCode(reqId, NOT_FOUND); // chat doesn't exist
			return;
		}

		Chat chat = server.chatLookup.get(chatUUID);
		statusCode(reqId, CONTINUE); // notify sender to send (packetized) message
		Persistable mess = comm.readResponse(reqId, new Encrypted(), true);
		statusCode(reqId, OK);
		for (String participant : chat.participantSet()) {
			if (participant.equals(currentUsername)) continue;
			Communicator<?, ?> pushComm = server.loggedInUsers.get(participant);
			int id = push(Message(chatUUID, currentUsername), participant);
			if (readCodeFrom(id, participant).equals(OK)) // ready to recv
			pushComm.sendResponse(id, mess, true); // send packetized message
		}
	}

	private void handleReportUser(int reqId, String offendingUser) throws IOException {
		// offendingUser should be part of a chat participated by currentUsername
		boolean found = false;
		UUID chatUUID = null;
		// lookup all chats for offendingUser. if multiple chats have offendingUser,
		// doesn't matter. it's a reported offendingUser.
		Set<UUID> chats = server.userToChatIDs.get(currentUsername);
		if (chats == null) { // none found
			statusCode(reqId, NOT_FOUND); return;
		}

		for (UUID chatID : chats) {
			Chat chat = server.chatLookup.get(chatID);
			if (chat == null) continue;
			if (chat.participantSet().contains(offendingUser)) {
				found = true;
				chatUUID = chatID;
				break;
			}
		}

		if (!found) { statusCode(reqId, NOT_FOUND); return; }
		UserReports.logReport(chatUUID, currentUsername, offendingUser, null);
		statusCode(reqId, OK);
	}

	private void handleAdminLogin(int reqId, String username)
			throws IOException, InterruptedException {
		if (admin) { // already logged in as admin?
			statusCode(reqId, BAD_REQUEST);
			return;
		}

		statusCode(reqId, ACCEPTED);
		// protocol: send salt, send wrapped key
		ByteArray ba = new ByteArray(AdminKeyManager.getAdminSalt());
		comm.sendResponse(reqId, ba);
		WrappedKey wrk = AdminKeyManager.getAdminWrappedKey();
		comm.sendResponse(reqId, wrk);

		if (runAdminChallengeResponse(reqId)) {
			currentUsername = "admin"; // should == username
			currentAccount = null;
			statusCode(reqId, OK);
			admin = true;
		} else {
			Thread.sleep(2000); // to prevent brute-force attacks
			statusCode(reqId, UNAUTHORIZED);
			admin = false;
		}
	}

	private void removeCurrentUserFromAllChats() throws IOException {
		Set<UUID> chatIDs = server.userToChatIDs.get(currentUsername);
		if (chatIDs != null) {
			for (UUID id : chatIDs) {
				removeCurrentUserFromChat(id);
			}
		}
	}

	private void handleLogout(int reqId) throws IOException {
		statusCode(reqId, OK);
		if (!admin) {
			// we don't want to remove a user whose name is actually admin
			removeCurrentUserFromAllChats();
			server.loggedInUsers.remove(currentUsername);
		}
		setStateToLoggedOut();
	}

	private void setStateToLoggedOut() {
		admin = false;
		currentUsername = null;
		currentAccount = null;
	}

	private void handleUserDelete(int reqId, String detail) throws IOException {
		for (UUID uuid : currentAccount.contactUUIDs) {
			server.contacts.removeAsync(currentUsername + uuid.toString());
		}
		server.blockedUsers.removeAsync(currentUsername);
		removeCurrentUserFromAllChats();
		server.accounts.removeAsync(currentUsername);
		setStateToLoggedOut();
		statusCode(reqId, OK);
	}
	
	private void handleAddContact(int reqId, String uuidStr) throws IOException {
		currentAccount.contactUUIDs.add(UUID.fromString(uuidStr));
		statusCode(reqId, CONTINUE);
		Encrypted blob = (Encrypted)comm.readResponse(reqId, new Encrypted(), true);
		boolean success = server.contacts.put(currentUsername + uuidStr, blob);
		server.accounts.put(currentUsername, currentAccount);
		statusCode(reqId, success ? OK : SERVER_ERROR); // return early!
	}

	private void handleRemoveContact(int reqId, String uuidStr) throws IOException {
		if (!currentAccount.contactUUIDs.remove(UUID.fromString(uuidStr))) {
			statusCode(reqId, NOT_FOUND);
		}
		server.accounts.putAsync(currentUsername, currentAccount);
		server.contacts.removeAsync(currentUsername + uuidStr);
		statusCode(reqId, OK);
	}

	private void handleGetContacts(int reqId) throws IOException {
		statusCode(reqId, ACCEPTED);
		comm.sendResponse(reqId, new PersistableInt(currentAccount.contactUUIDs.size()));
		Encrypted blob = new Encrypted();
		for (UUID uuid : currentAccount.contactUUIDs) {
			server.contacts.get(currentUsername + uuid.toString(), blob);
			comm.sendResponse(reqId, blob);
		}
	}

	private void handleAction(final int reqId, final ClientAction action) throws IOException {
		switch (action.getType()) {
		case LOGIN:
			if (!checkLoggedOut(reqId)) break;
			try {
				handleLogin(reqId, action.getDetail());
			} catch (InterruptedException e) { } // not much to do
			break;
		case LOGOUT:
			handleLogout(reqId);
			break;
		case USER_CREATE:
			if (checkLoggedOut(reqId))
				handleUserCreate(reqId, action.getDetail());
			break;
		case USER_DELETE:
			if (checkUserAuthenticated(reqId))
				handleUserDelete(reqId, action.getDetail());
			break;
		case CHANGE_PASSWORD:
			if (checkUserAuthenticated(reqId))
				handlePasswordChange(reqId, action.getDetail());
			break;
		case DISCONNECT:
			handleLogout(reqId);
			comm.shutdown();
			server.disconnect();
			break;
		case START_CHAT:
			if (checkUserAuthenticated(reqId))
				handleStartChat(reqId, action.getDetail());
			break;
		case SEND_MESSAGE:
			if (checkUserAuthenticated(reqId))
				handleSendMessage(reqId, UUID.fromString(action.getDetail()));
			break;
		case LEAVE_CHAT:
			if (checkUserAuthenticated(reqId))
				handleLeaveChat(reqId, UUID.fromString(action.getDetail()));
			break;
		case REPORT_USER:
			if (checkUserAuthenticated(reqId))
				handleReportUser(reqId, action.getDetail());
			break;
		case ADD_CONTACT:
			if (checkUserAuthenticated(reqId))
				handleAddContact(reqId, action.getDetail());
			break;
		case REMOVE_CONTACT:
			if (checkUserAuthenticated(reqId))
				handleRemoveContact(reqId, action.getDetail());
			break;
		case GET_CONTACTS:
			if (checkUserAuthenticated(reqId))
				handleGetContacts(reqId);
			break;
		case ADMIN_LOGIN:
			if (!checkLoggedOut(reqId)) break; // could return BAD_REQUEST
			try {
				handleAdminLogin(reqId, action.getDetail());
			} catch (InterruptedException e) { }
			break;
		case BLOCK_USER:
			if (admin) {
				server.blockedUsers.putLong(action.getDetail(), 0L);
				statusCode(reqId, OK);
			} else statusCode(reqId, UNAUTHORIZED);
			break;
		case UNBLOCK_USER:
			if (admin) {
				server.blockedUsers.remove(action.getDetail());
				statusCode(reqId, OK);
			} else statusCode(reqId, UNAUTHORIZED);
			break;
		case GET_REPORTS:
			if (!admin) {
				statusCode(reqId, UNAUTHORIZED);
				return;
			}
			int entries = 0;
			try { entries = Integer.parseInt(action.getDetail()); }
			catch (NumberFormatException e) { statusCode(reqId, BAD_REQUEST); break; }
			statusCode(reqId, ACCEPTED);
			UserReports.searchRecentEntries(entries,
				new Callback<String>() {
					@Override public void process(String s) {
						try { comm.sendResponse(reqId, new Message(s)); }
						catch (IOException e) { System.err.println(e.toString()); }
					}
				});
			comm.sendResponse(reqId, new Message("")); // signal end of logs
			break;
		default:
			statusCode(reqId, BAD_REQUEST);
		}
	}

}
