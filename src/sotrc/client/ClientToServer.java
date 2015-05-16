package sotrc.client;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

import common.*;
import common.crypto.*;
import common.network.*;
import javax.crypto.*;
import sotrc.common.*;
import static sotrc.common.ClientAction.ActionType.*;
import static common.network.StatusCode.*;
import static sotrc.common.NewChallengeResponse.Challenge;


public class ClientToServer extends Observable {

	private final String serverHost;
	private final int serverPort;
	private StatusCode code;
	private Communicator<ClientAction, Notification> comm;
	public final Map<UUID, Chat> activeChats; // when initialized, will never be null
	/**
	 * non-null only when logged in.
	 * logging out needs to clear this.
	 */
	private PrivateKey longTermPriv;
	public final Set<Contact> contactsSet;

	/**
	 * Creates an instance of a server communicator for the client.
	 * Does not immediately attempt to establish a connection.
	 */
	public ClientToServer(String serverHost, int serverPort) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		activeChats = new ConcurrentHashMap<UUID, Chat>();
		code = new StatusCode(null);
		contactsSet = new HashSet<Contact>();
	}

	private void establishConnection() throws IOException {
		// sets comm.
		SSLSocket sockclient = SslDefaults.getClientSSLSocket(serverHost, serverPort);
		sockclient.startHandshake();
		comm = new Communicator<ClientAction, Notification>(
			sockclient.getInputStream(),
			sockclient.getOutputStream(),
			new PersistableFactory<Notification>() {
				@Override public Notification newT() { return new Notification(); }
			},
			new Communicator.RequestHandler<Notification>() {
				@Override public void processRequest(int id, Notification n) {
					try {
						handleNotification(id, n);
					} catch (IOException e) {
						System.err.println("Unable to process a notification.");
						System.err.println(e.toString());
					}
				}
			});
		new Thread(comm).start();
	}

	private int sendAction(ClientAction.ActionType type, String detail) throws IOException {
		int id = comm.sendRequest(new ClientAction(type, detail));
		return id;
	}

	private StatusCode readCode(int id) throws IOException {
		comm.readResponse(id, code);
		return code;
	}

	private void sendCode(int id, String code) throws IOException {
		comm.sendResponse(id, this.code.setTo(code));
	}

	public UserAccount createAccount(String username, char[] password) throws IOException {
		// TODO: use exceptions to distinguish between different server error codes?
		int id = sendAction(USER_CREATE, username);
		if (readCode(id).equals(CONTINUE)) {
			UserAccount ua = UserAccount.createNew(username, password);
			comm.sendResponse(id, ua.getKeys());
			if (readCode(id).equals(OK))
				return ua;
		} else System.err.println("Unable to create account. Name probably already exists.");

		return null; // username already exists OR client not logged in
	}

	/**
	 * A ClientServer login does not automatically sync.
	 * NOTE: it is up to the manager to call sync seprately.
	 */
	public UserAccount login(String username, char[] password)
			throws IOException {
		UserAccount ua = null;
		int id = sendAction(LOGIN, username);
		if (!readCode(id).equals(ACCEPTED)) {
			return null;
		}

		Keys keys = Keys.newForReconstruction();
		comm.readResponse(id, keys);
		ua = UserAccount.fromLogin(keys, username, password);

		Challenge challenge = Challenge.newForResponding();
		comm.readResponse(id, challenge);
		readCode(id); assert code.equals(CONTINUE);
		comm.sendResponse(id, challenge.respond(ua.getPrivateKey()));
		readCode(id);
		if (code.equals(UNAUTHORIZED)) {
			return null;
		}

		longTermPriv = ua.getPrivateKey(); // for handleChatStart
		this.activeChats.clear();
		this.contactsSet.clear();
		resetAndGetContacts(ua); // repopulates contacts list
		return ua;
	}

	/**
	 * The manager is responsible for keeping track of the admin state.
	 * Method is does not protect against unauthorized users maliciously calling this.
	 */
	public boolean adminLogin(char[] password) throws IOException {
		int id = sendAction(ADMIN_LOGIN, "admin");
		if (!readCode(id).equals(ACCEPTED)) {
			System.out.println("Admin login issues");
			return false;
		}

		ByteArray ba = new ByteArray();
		comm.readResponse(id, ba); // admin salt.
		WrappedKey privKeyEnc = WrappedKey.newForReconstruction();
		comm.readResponse(id, privKeyEnc); // wrapped key
		SecretKey master = CryptoUtils.deriveAESKey(password, ba.getArray());
		PrivateKey privKey = null;
		try {
			privKey = (PrivateKey)privKeyEnc.unwrap(master);
		} catch (InvalidKeyException e) {
			System.err.println("ClientToServer adminLogin " + e.toString());
			return false;
		}
		// prepare ChallengeResponse
		Challenge challenge = Challenge.newForResponding();
		comm.readResponse(id, challenge);
		readCode(id); assert code.equals(CONTINUE);
		comm.sendResponse(id, challenge.respond(privKey));
		readCode(id);
		return code.equals(OK);
	}

	public UserAccount changePassword(UserAccount account, char[] oldPassword, char[] newPassword)
			throws IOException {
		UserAccount ua = null;
		int id = sendAction(CHANGE_PASSWORD, account.getUsername());
		readCode(id);
		if (!code.equals(ACCEPTED)) {
			System.out.println("ClientToServer changePassword got code " + code.toString());
			return null; // invalid username
		}

		Keys keys = Keys.newForReconstruction();
		comm.readResponse(id, keys);
		ua = UserAccount.fromLogin(keys, account.getUsername(), oldPassword);

		Challenge challenge = Challenge.newForResponding();
		comm.readResponse(id, challenge);
		readCode(id); assert code.equals(CONTINUE);
		comm.sendResponse(id, challenge.respond(ua.getPrivateKey()));
		readCode(id);
		if (code.equals(UNAUTHORIZED)) {
			return null; // invalid pass
		} // else continue

		// create new keys with new password and send them
		ua.updateMasterKey(newPassword);
		comm.sendResponse(id, ua.getKeys());
		if (readCode(id).equals(SERVER_ERROR)) {
			return null;
		} // else OK
		return ua;
	}

	public StatusCode reportUser(String username) throws IOException {
		int id = sendAction(REPORT_USER, username);
		if (!readCode(id).equals(OK)) {
			// could be that provided username is not part of active chat.
			System.out.println("ClientToServer reportUser got " + code.toString());
		}
		return new StatusCode(code.toString());
	}

	/**
	 * TODO add authorization? No, server has some of it too?
	 */
	public boolean blockUser(String username, boolean block)
			throws IOException {
		// block == false means unblock
		int id = sendAction((block ? BLOCK_USER : UNBLOCK_USER), username);
		return readCode(id).equals(OK);
	}

	/**
	 * Retrieves up to entries number of log entries from server.
	 * The manager is responsible for keeping track of the admin state.
	 * Note how account is not used in this method.
	 */
	public List<String> getUserReports(int entries)
			throws IOException {
		if (entries < 1) return new ArrayList<String>(); // no entries

		int id = sendAction(GET_REPORTS, Integer.toString(entries));
		if (!readCode(id).equals(ACCEPTED)) {
			System.err.println("Got code " + code.toString());
			return null; // BAD_REQUEST if long not parseable, UNAUTHORIZED if not admin
		}

		List<String> logs = new ArrayList<String>(entries);
		Message mess = new Message("");
		for (;;) {
			comm.readResponse(id, mess);
			if (mess.toString().equals("")) break;
			logs.add(mess.toString());
		}
		return logs;
	}

	public StatusCode deleteAccount(UserAccount account) throws IOException {
		return cleanup(USER_DELETE);
	}

	private StatusCode cleanup(ClientAction.ActionType action) throws IOException {
		this.longTermPriv = null;
		this.activeChats.clear();
		this.contactsSet.clear();
		int id = sendAction(action, "");
		readCode(id);
		return new StatusCode(code.toString());
	}

	/**
	 * start chat with "user" send to server.
	 * response will be NOT_FOUND, or OK.
	 * returns only NOT_FOUND, REJECTED, or OK
	 */
	public Chat startChat(UserAccount acct, String user) throws IOException {
		int id = sendAction(START_CHAT, user);
		if (!readCode(id).equals(ACCEPTED)) {
			System.out.println("Got code " + code.toString());
			return null;
		}
		System.out.println("Negotiating keys... (might take a while)");
		// expecting PLB and PSB to come from server.
		ByteArray balong = (ByteArray)comm.readResponse(id, new ByteArray());
		PublicKey longTerm = CryptoUtils.decodeRSAPublicKey(balong.getArray());

		// A initiated request. add B if not in contacts.
		long fingerprint = CryptoUtils.getKeyFingerprint(longTerm);
		FingerprintState state = checkFingerprint(fingerprint, user);

		ByteArray bashort = (ByteArray)comm.readResponse(id, new ByteArray());
		PublicKey shortTerm = CryptoUtils.decodeRSAPublicKey(bashort.getArray());
		// generate data/mac keys encrypted with both RSA public keys.
		readCode(id); assert code.equals(CONTINUE);
		SessionKeys keys = SessionKeys.makeNew(shortTerm, longTerm);
		comm.sendResponse(id, keys);
		// expect an OK, which means OK from B.
		readCode(id);
		if (!code.equals(OK)) {
			System.out.println("Was not OK. Perhaps other end was not able to negotiate.");
			return null;
		}
		// need uuid
		PersistableUUID puuid = new PersistableUUID();
		comm.readResponse(id, puuid);
		readCode(id);
		if (!code.equals(OK)) { return null; }

		Chat chat = new Chat(puuid.getUUID(), keys);
		chat.addParticipant(user, new Tuple<Long, FingerprintState>(fingerprint, state));
		this.activeChats.put(chat.uuid, chat);
		return chat;
	}

	public StatusCode sendMessage(UUID uuid, String message) throws IOException {
		if (uuid == null || message == null) return null;
		// find relevant chat.
		Chat chat = this.activeChats.get(uuid);
		if (chat == null) return null;
		int id = sendAction(SEND_MESSAGE, uuid.toString());
		readCode(id); // either NOT_FOUND or CONTINUE
		if (!code.equals(CONTINUE)) {
			System.out.println("Chat not found by server?"); return null;
		}
		// encrypt message with data/mac keys.
		Encrypted<Message> encMess = new Encrypted<Message>(
			new Message(message), chat.keys.getEncKey(), chat.keys.getMACKey());
		// send over.
		comm.sendResponse(id, encMess, true);
		readCode(id);
		return new StatusCode(code.toString());
	}

	public boolean addContact(UserAccount account, Contact contact) throws IOException {
		int id = sendAction(ADD_CONTACT,contact.getUUID().toString());
		readCode(id);
		if (!code.equals(CONTINUE)) return false;

		Encrypted<Contact> encContact = new Encrypted<Contact>(contact,
			account.getDataKey(),account.getMACKey());
		comm.sendResponse(id, encContact, true);
		if (!readCode(id).equals(OK)) return false;
		contactsSet.add(contact);
		return true;
	}

	public boolean removeContact(Contact contact) throws IOException {
		int id = sendAction(REMOVE_CONTACT, contact.getUUID().toString());
		if (readCode(id).equals(OK)) {
			contactsSet.remove(contact);
			return true;
		}
		return false;
	}

	/**
	 * This getter has the side effect of clearing the current contactsSet,
	 *   issues a call to the server, re-adds to the contactsSet,
	 * and returns the list.
	 */
	private Set<Contact> resetAndGetContacts(UserAccount account) throws IOException {
		int id = sendAction(GET_CONTACTS,"");
		readCode(id);
		if (!code.equals(ACCEPTED)) return null;

		PersistableInt pInt = new PersistableInt();
		comm.readResponse(id, pInt,false);
		contactsSet.clear();
		//contactsSet.ensureCapacity(pInt.getInt());
		for(int i = 0; i < pInt.getInt(); i++) {
			Encrypted<Contact> newContact = new Encrypted<Contact>(new Contact(),
				account.getDataKey(),account.getMACKey());
			comm.readResponse(id, newContact, false);
			contactsSet.add(newContact.getT());
		}

		return Collections.unmodifiableSet(contactsSet);
	}

	public Contact findContactByName(String username) {
		for (Contact c : contactsSet) {
			if (c.getUsername().equals(username)) {
				return c;
			}
		}
		return null;
	}


	public StatusCode endChat(UUID uuid) throws IOException {
		if (uuid == null) return null;
		// find relevant chat.
		Chat chat = this.activeChats.get(uuid);
		if (chat == null) return null;

		int id = sendAction(LEAVE_CHAT, uuid.toString());
		readCode(id); // UNAUTHORIZED, NOT_FOUND, OK
		this.activeChats.remove(uuid); // regardless of server response.
		return new StatusCode(code.toString());
	}

	public void logout() throws IOException {
		StatusCode code = cleanup(LOGOUT);
		assert code.equals(OK);
	}

	public void disconnect() throws IOException {
		StatusCode code = cleanup(DISCONNECT);
		assert code.equals(OK);
		comm.shutdown();
	}

	public void start() throws IOException {
		establishConnection();
	}

	/***********************************************************/
	/***************** SERVER HANDLER CODE *********************/
	/***********************************************************/

	private void handleNotification(int id, final Notification notif) throws IOException {
		String detail = notif.detail;
		UUID uuid = notif.getUUID();
		switch (notif.type) {
		case INVITATION:
			System.out.println("Got invitation?");
			sendCode(id, OK);
			break;
		case MESSAGE:
			System.out.println("Message. OK, but REJECT possible in future?");
			sendCode(id, OK);
			handleMessage(id, uuid, detail);
			break;
		case CHAT_START:
			// detail is other participant.
			System.out.println("Chat start. OK, but REJECT possible in future?");
			sendCode(id, OK);
			handleChatStart(id, uuid, detail);
			break;
		case CHAT_END:
			sendCode(id, OK);
			handleChatEnd(id, uuid, detail);
			break;
		case USER_LEFT:
			sendCode(id, OK);
			handleUserLeft(id, uuid, detail);
			break;
		default:
			// unrecognized notif
			System.out.println("Unrecognized notification.");
			sendCode(id, BAD_REQUEST);
		}
	}

	private boolean handleChatStart(int reqId, UUID uuid, String otherParticipant)
			throws IOException {
		// we've already sent an ok.
		ByteArray baPLA = new ByteArray();
		comm.readResponse(reqId, baPLA);
		PublicKey keyPLA = CryptoUtils.decodeRSAPublicKey(baPLA.getArray());
		readCode(reqId); assert code.equals(CONTINUE);
		KeyPair pair = CryptoUtils.generateRSAKeyPair(2048); // short-term is smaller.
		// send over PSB
		PublicKey pub = pair.getPublic();
		ByteArray ba = new ByteArray(pub.getEncoded());
		comm.sendResponse(reqId, ba);
		readCode(reqId);
		if (!code.equals(ACCEPTED)) {
			System.out.println("Participant on other end probably hung up?");
			return false;
		}
		// expect the encrypted mess from A
		SessionKeys keys = SessionKeys.newForReconstruction(
			pair.getPrivate(), longTermPriv);
		try {
			comm.readResponse(reqId, keys);
			if (keys == null) {
				sendCode(reqId, REJECTED);
				return false;
			}
			sendCode(reqId, OK);
			Chat chat = new Chat(uuid, keys);

			// A initiated request. We, B, don't want to add contact A yet (prompt in GUI later)
			long fingerprint = CryptoUtils.getKeyFingerprint(keyPLA);
			FingerprintState state = checkFingerprint(fingerprint, otherParticipant);
			chat.addParticipant(otherParticipant, new Tuple<Long, FingerprintState>(fingerprint, state));
			this.activeChats.put(uuid, chat);
			System.out.println("Correctly ran chat start protocol.");
			AsyncNotifications notifs = new AsyncNotifications(
				otherParticipant, "", (byte)0, chat);
			this.setChanged();
			this.notifyObservers(notifs);
			return true;
		} catch (IOException e) {
			System.err.println(e.toString());
			sendCode(reqId, REJECTED);
			return false;
		}
	}

	
	private boolean handleMessage(int reqId, UUID uuid, String otherParticipant)
			throws IOException {
		// we've already sent an ok. expect an encrypted message.
		Chat chat = this.activeChats.get(uuid);
		if (chat == null) return false;
		Encrypted<Message> encMess = new Encrypted<Message>(
			new Message(""), chat.keys.getEncKey(), chat.keys.getMACKey());
		comm.readResponse(reqId, encMess, true);
		// broadcast
		AsyncNotifications notifs = new AsyncNotifications(
			otherParticipant, encMess.getT().toString(), (byte)1, chat);
		this.setChanged();
		this.notifyObservers(notifs);
		return true;
	}

	private boolean handleUserLeft(int reqId, UUID uuid, String otherParticipant)
	{
		// ok already sent.
		Chat chat = this.activeChats.get(uuid);
		if (chat == null) {
			System.err.println("Chat " + uuid.toString() + " was null. don't know how to handle user left.");
			return true; // if not active, ok, whatever
		}

		AsyncNotifications notifs = new AsyncNotifications(
			otherParticipant, "", (byte)2, chat);
		this.setChanged();
		this.notifyObservers(notifs);
		return true;
	}

	private boolean handleChatEnd(int reqId, UUID uuid, String detail)
	{
		// don't need detail.
		Chat chat = this.activeChats.get(uuid);
		if (chat == null) {
			System.err.println("Chat " + uuid.toString() + " was already null. Whatever");
			return true; // if not active, ok, whatever
		}
		this.activeChats.remove(chat);
		AsyncNotifications notifs = new AsyncNotifications(
			"", "", (byte)3, chat);
		this.setChanged();
		this.notifyObservers(notifs);
		return true;
	}

	private FingerprintState checkFingerprint(long fingerprint, String user) {
		FingerprintState state = FingerprintState.NEITHER;

		for (Contact c : this.contactsSet) {
			if (c.getUsername().equals(user)) {
				if (c.getKeyFingerprint() != fingerprint) {
					System.out.println("checkFingerprint: INAUTHENTIC. "
						+ user + " is not who we believe them to be!");
					state = FingerprintState.INAUTHENTIC;
				} else if (c.isAuthenticated()) {
					state = FingerprintState.AUTHENTIC;
				}
				break;
			}
		}

		return state;
	}
}
