package sand.client;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;

import common.*;
import common.crypto.*;
import common.network.*;
import javax.crypto.*;
import sand.common.*;
import static sand.common.SandAction.ActionType.*;
import static common.network.StatusCode.*;

public class SandClientServer {

	private final String serverHost;
	private final int serverPort;
	private Socket conn;
	private InputStream in;
	private OutputStream out;
	private StatusCode code;
	private boolean abnormalLogin;

	/**
	 * Creates an instance of a server communicator for the client.
	 * Does not immediately attempt to establish a connection.
	 */
	public SandClientServer(String serverHost, int serverPort) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		code = new StatusCode(null);
	}

	public SandClientServer(String serverHost) {
		this(serverHost, SslDefaults.DEFAULT_SAND_PORT);
	}

	private void establishConnection() throws IOException {
		SSLSocket sock = SslDefaults.getClientSSLSocket(serverHost, serverPort);
		sock.startHandshake();
		conn = sock;
		in = conn.getInputStream();
		out = conn.getOutputStream();
	}

	private void sendAction(SandAction.ActionType type, String detail) throws IOException {
		new SandAction(type, detail).persist(out);
	}

	private StatusCode readCode() throws IOException {
		code.reconstruct(in);
		return code;
		}

	public UserAccount createAccount(String username, char[] password, boolean enable) throws IOException {
		// TODO: use exceptions to distinguish between different server error codes?
		sendAction(USER_CREATE, username);
		if (readCode().equals(CONTINUE)) {
			UserAccount ua = UserAccount.createNew(username, password, enable);
			ua.getKeys().persist(out);
			if (readCode().equals(OK))
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
		sendAction(LOGIN, username);
		if (!readCode().equals(ACCEPTED)) {
			return null; // the server will automatically sleep 2000
		}

		// else code == ACCEPTED
		Keys keys = Keys.readIn(in);

		ua = UserAccount.fromLogin(keys, username, password);
		ChallengeResponse cr = ChallengeResponse.readChallenge(in);
		readCode(); assert code.equals(CONTINUE); // server only sends continue
		cr.respond(ua.getPrivateKey()).send(out);
		code.reconstruct(in); // UNAUTHORIZED ABNORMAL or OK
		if (code.equals(UNAUTHORIZED)) {
			return null; // invalid password
		} else if (code.equals(ABNORMAL)) { // Abnormal login
			abnormalLogin = true;
		} // else code == OK

		return ua;
	}

	/**
	 * The manager is responsible for keeping track of the admin state.
	 * Method is does not protect against unauthorized users maliciously calling this.
	 */
	public boolean adminLogin(char[] password) throws IOException {
		sendAction(ADMIN_LOGIN, "admin");
		if (!readCode().equals(ACCEPTED)) {
			System.out.println("Admin login issues");
			return false;
		}

		try { // server sends over admin salt and wrapped key for client to decrypt
			byte[] salt = Utils.readArray(in);
			WrappedKey privKeyEnc = WrappedKey.readIn(in);
			SecretKey master = CryptoUtils.deriveAESKey(password,salt);
			PrivateKey privKey = (PrivateKey)privKeyEnc.unwrap(master);
			ChallengeResponse cr = ChallengeResponse.readChallenge(in);
			readCode(); assert code.equals(CONTINUE); // server only sends continue
			cr.respond(privKey).send(out);
		} catch (InvalidKeyException e) {
			return false;
		}

		return readCode().equals(OK);
	}

	public boolean loginWasAbnormal() {
		return abnormalLogin;
	}

	public void resetAbnormalLogin() {
		abnormalLogin = false;
	}

	private boolean addOrRemoveCredential(UserAccount account,
			Union<Encrypted<CredentialTuple>, CredentialDeletion> union, UUID uuid)
			throws IOException {
		sendAction(union.isT() ? ADDCREDENTIAL : REMOVECREDENTIAL, uuid.toString());
		if (!readCode().equals(ACCEPTED)) return false;
		DataInputStream dis = new DataInputStream(in);
		long revNum = dis.readLong();
		if (union.isT()) // store the rev num in the tuple
			union.getT().getT().revNum = revNum;
		if (!readCode().equals(CONTINUE)) return false;
		// send it to server.
		PacketizedOutputStream pos = new PacketizedOutputStream(out);
		union.persist(pos);
		pos.finish();
		return readCode().equals(OK);
	}

	/**
	 * all edits go through add/remove
	 */
	public boolean addCredential(UserAccount account, Encrypted<CredentialTuple> tup,
			UUID uuid) throws IOException {
		Union<Encrypted<CredentialTuple>, CredentialDeletion> union
			= new Union<Encrypted<CredentialTuple>, CredentialDeletion>(tup, null, true);
		return addOrRemoveCredential(account, union, uuid);
	}

	/**
	 * all edits go through add/remove
	 */
	public boolean removeCredential(UserAccount account, UUID uuid)
			throws IOException {
		// make delete request
		CredentialDeletion delete = new CredentialDeletion(uuid, account.getMACKey());
		Union<Encrypted<CredentialTuple>, CredentialDeletion> union
			= new Union<Encrypted<CredentialTuple>, CredentialDeletion>(
				null, delete, false);
		return addOrRemoveCredential(account, union, uuid);
	}

	public static interface SyncCallback {
		/** Called for each tuple returned by the server in a sync. */
		public void processAdd(UUID uuid, Encrypted<CredentialTuple> tuple);
		/** Called for each credential deletion returned by the server in a sync. */
		public void processDel(UUID uuid, CredentialDeletion deletion);
		/** Called for each tuple/deletion that fails to reconstruct during sync. */
		public void processFailure(UUID uuid, boolean wasAdd, IOException e);
	}

	public boolean sync(long revNum, Encrypted<CredentialTuple> tupReceiver,
			CredentialDeletion delReceiver, SyncCallback callback) throws IOException {
		sendAction(SYNC, Long.toString(revNum));
		if (!readCode().equals(ACCEPTED)) // BAD_REQUEST if long not parseable
			return false;
		Union<Encrypted<CredentialTuple>, CredentialDeletion> union =
			new Union<Encrypted<CredentialTuple>, CredentialDeletion>(
			tupReceiver, delReceiver, true);
		for ( ; ; revNum++) {
			UUID uuid = null;
			try {
				int b = in.read();
				if (b == 0) continue; // this rev. no longer valid
				else if (b == 4 /*EOT ASCII*/) break; // end of sync
				// else b == 1: receive revision
				uuid = Utils.readUUID(in);
				union.reconstruct(new PacketizedInputStream(in));
				if (union.isT()) { // make sure revNum match
					if (union.getT().getT().revNum == revNum) {
						callback.processAdd(uuid, union.getT());
					} else {
						callback.processFailure(uuid, true, null);
					}
				} else callback.processDel(uuid, union.getU());
			} catch (IOException e) {
				callback.processFailure(uuid, union.isT(), e);
			}
		}
		return true;
	}

	public boolean toggleTwoFactor(UserAccount account)
			throws IOException {
		sendAction(TOGGLE_TWOFACTOR, account.getUsername());
		if(readCode().equals(CONTINUE)) {
			account.getKeys().persist(out);
			if(readCode().equals(OK)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean changeUsername(UserAccount account, String newUsername)
			throws IOException {
		sendAction(USERNAME_CHANGE, newUsername);
		if (readCode().equals(OK)) { // else code == UNAUTHORIZED | SERVER_ERROR
			account.setUsername(newUsername);
			return true;
		}
		return false;
	}

	public UserAccount changePassword(UserAccount account, char[] oldPassword, char[] newPassword)
			throws IOException {
		UserAccount ua = null;
		sendAction(USERPASSWORD_CHANGE, account.getUsername());
		if (readCode().equals(NOT_FOUND)) {
			return null; // invalid username
		} else { // code == ACCEPTED
			Keys keys = Keys.readIn(in);
			ua = UserAccount.fromLogin(keys, account.getUsername(), oldPassword);
			ChallengeResponse cr = ChallengeResponse.readChallenge(in);
			readCode(); assert code.equals(CONTINUE); // server only sends continue
			cr.respond(ua.getPrivateKey()).send(out);
			code.reconstruct(in);// CONTINUE or UNAUTHORIZED
			if (code.equals(UNAUTHORIZED)) {
				return null; // invalid password
			} else { // Continue
				// create new keys with new password and send them
				ua.updateMasterKey(newPassword);
				ua.getKeys().persist(out);
				if(readCode().equals(SERVER_ERROR)) {
					return null;
				} // else OK
			}
		}
		return ua;
	}

	/**
	 * Retrieves up to entries number of log entries from server.
	 * The manager is responsible for keeping track of the admin state.
	 * Note how account is not used in this method.
	 */
	public List<String> getUserLogs(UserAccount account, int entries)
			throws IOException {
		if (entries < 1) return new ArrayList<String>(); // no entries
		sendAction(GET_LOGS, Integer.toString(entries));
		if (!readCode().equals(ACCEPTED)) {
			return null; // BAD_REQUEST if long not parseable
		}

		List<String> logs = new ArrayList<String>(entries);
		DataInputStream dis = new DataInputStream(in);
		for (;;) {
			String s = dis.readUTF();
			if (s.equals("")) break;
			logs.add(s);
		}
		return logs;
	}

	/**
	 * TODO add authorization? No, server has some of it too?
	 */
	public boolean blockUser(String username, boolean block)
			throws IOException {
		// block == false means unblock
		sendAction((block ? BLOCK_USER : UNBLOCK_USER), username);
		readCode();
		return code.equals(OK);
	}

	public boolean deleteAccount(UserAccount account)
			throws IOException {
		sendAction(USER_DELETE, "");
		return readCode().equals(OK);
	}

	public void logout() throws IOException {
		sendAction(LOGOUT, "");
		readCode(); assert code.equals(OK); // server always sends OK
	}

	public void disconnect() throws IOException {
		sendAction(DISCONNECT, "");
		readCode(); assert code.equals(OK); // server always sends OK
	}

	public void start() throws IOException {
		establishConnection();
	}

}
