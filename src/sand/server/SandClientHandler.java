package sand.server;

import java.io.*;
import java.net.*;
import java.util.UUID;

import common.*;
import common.network.*;
import common.admin.AdminKeyManager;
import common.crypto.ChallengeResponse;
import static common.network.StatusCode.*;
import common.server.*;

import sand.common.*;
import sand.server.LocationUtilities;

public class SandClientHandler implements Runnable {

	private final Socket conn;
	private final SandServer server;
	private final InputStream in;
	private final OutputStream out;
	private final StatusCode code;
	private boolean disconnected = false;

	/** Invariant: null whenever client is not authenticated as a user. */
	String activeUserName = null;
	Account currentAccount = null;
	boolean admin = false;

	public SandClientHandler(Socket conn, SandServer server) throws IOException {
		this.conn = conn;
		this.server = server;
		this.in = conn.getInputStream();
		this.out = conn.getOutputStream();
		this.code = new StatusCode(null);
	}

	private void statusCode(String code) throws IOException {
		this.code.setTo(code).persist(out);
	}

	// assumes logged out
	private void handleUserCreate(String username) throws IOException {
		if (username == null || username.contains(" ")) {
			statusCode(FORBIDDEN); // no spaces allowed
			return;
		}
		Account account = new Account();
		if (server.accounts.get(username, account)) {
			statusCode(FORBIDDEN); // username in use
		} else { // accepted
			statusCode(CONTINUE);
			account.keys.reconstruct(in);
			boolean success = server.accounts.put(username, account);
			statusCode(success ? OK : SERVER_ERROR);
		}
	}

	/**
	 * @return a detail message to log in the case of abnormal login, else null
	 */
	private String handleLogin(String username)
			throws IOException, InterruptedException {
		String detail = null;
		if (server.blockedUsers.containsKey(username)) { // login blocked
			statusCode(UNAUTHORIZED);
			return detail;
		}
		Account account = server.getAccount(username);
		boolean abnormalLoginStatus = false;

		if (account == null) {
			Thread.sleep(2000); // to prevent attacks
			statusCode(NOT_FOUND); // invalid username
		} else {
			statusCode(ACCEPTED);
			account.keys.persist(out); // send user's (encrypted) keys
			// run Challenge-Response authentication protocol
			ChallengeResponse cr = ChallengeResponse.newChallenge().send(out);
			statusCode(CONTINUE);
			// need to perform TFA here
			if (cr.readResponse(in).verify(account.keys.getPublicKey())) {
				activeUserName = username;
				currentAccount = account;
				// check abnormal login status
				if (account.loginLocation.equals("")) {
					account.loginLocation = LocationUtilities.loginLocationFinder(
						conn.getInetAddress().toString());
				} else {
					System.out.println("Logging in from " + account.loginLocation);
					if (LocationUtilities.abnormalLoginChecker(conn.getInetAddress()
							.toString(), account.loginLocation)) {
						abnormalLoginStatus = true;
						account.loginLocation = LocationUtilities.loginLocationFinder(
							conn.getInetAddress().toString());
						System.out.println("Abnormal Login detected");
					}
				}
				if (abnormalLoginStatus) {
					detail = "Abnormal Login Status: " + abnormalLoginStatus;
					statusCode(ABNORMAL);
				} else statusCode(OK);
			} else {
				// invalid pass
				Thread.sleep(2000); // to prevent brute force attacks.
				statusCode(UNAUTHORIZED);
			}
		}
		return detail;
	}

	private String handleAdminLogin(String username) throws IOException, InterruptedException {
		String detail = null;
		// No need to check account
		statusCode(ACCEPTED);
		Utils.writeArray(AdminKeyManager.getAdminSalt(), out); // send salt
		AdminKeyManager.getAdminWrappedKey().persist(out);  // send wrapped key
		// run Challenge-Response authentication protocol
		ChallengeResponse cr = ChallengeResponse.newChallenge().send(out);
		statusCode(CONTINUE);
		// need to perform TFA here?
		if (cr.readResponse(in).verify(AdminKeyManager.getPublicKey())) {
			activeUserName = username;
			currentAccount = null;
			statusCode(OK);
			admin = true;
		} else {
			Thread.sleep(2000); // to prevent brute-force attacks
			statusCode(UNAUTHORIZED);
		}
		return detail;
	}

	private void handleTwoFactorToggle(String username) throws IOException {
		Account account = server.getAccount(username);
		if (account == null) {
			statusCode(NOT_FOUND); // invalid username
		} else {
			statusCode(CONTINUE);
			account.keys.reconstruct(in);
			boolean success = server.accounts.put(username, account);
			statusCode(success ? OK : SERVER_ERROR);
		}
	}

	private String handlePasswordChange(String username) throws IOException {
		String detail = null;
		statusCode(ACCEPTED);
		currentAccount.keys.persist(out); // send user's (encrypted) keys
		// run Challenge-Response authentication protocol
		ChallengeResponse cr = ChallengeResponse.newChallenge().send(out);
		statusCode(CONTINUE);
		// need to perform TFA here
		if (cr.readResponse(in).verify(currentAccount.keys.getPublicKey())) {
			//Don't need to update i just needed to authenticate
			statusCode(CONTINUE);
			currentAccount.keys.reconstruct(in); // get keys with new password
			boolean success = server.accounts.put(activeUserName, currentAccount);
			statusCode(success ? OK : SERVER_ERROR);
		} else {
			statusCode(UNAUTHORIZED);
		}
		return detail;
	}

	// assume already authenticated
	private void handleAddRemoveCredential(String userPrefix, String credUUIDstr)
			throws IOException {
		// garbage-collect last change to this credential
		String lastUpdateKey = userPrefix + credUUIDstr;
		long lastUpdate = server.credentials.getLong(lastUpdateKey);
		if (lastUpdate >= 0L) // there exists an old version
			server.credentials.removeAsync(userPrefix + lastUpdate);
		server.credentials.putLong(lastUpdateKey, currentAccount.revNum);
		statusCode(ACCEPTED); // send revision number to client
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeLong(currentAccount.revNum);
		statusCode(CONTINUE); // accept credential tuple or deletion from client
		String key = userPrefix + currentAccount.revNum++; // incr revnum
		boolean success =
		  server.credentials.put(key, new Blob(UUID.fromString(credUUIDstr), conn))
		  && server.accounts.put(activeUserName, currentAccount);
		statusCode(success ? OK : SERVER_ERROR);
	}

	/**
	 * If client is not authenticated, send UNAUTHORIZED and return false.
	 * Else return true.
	 */
	private boolean checkUserAuthenticated() throws IOException {
		if (currentAccount == null)
			statusCode(UNAUTHORIZED);
		return activeUserName != null;
	}

	private boolean checkLoggedOut() throws IOException {
		if (currentAccount != null || activeUserName != null) {
			statusCode(BAD_REQUEST);
			return false;
		} else return true;
	}

	private void handleAction(final SandAction action) throws IOException {
		String userString = currentAccount == null ?
				"**INVALID USER OR ADMIN**" : currentAccount.uuid.toString();
		String detail = null;
		switch (action.getType()) {
		case LOGIN:
			userString = action.getDetail();
			if (!checkLoggedOut()) break;
			try { detail = handleLogin(userString); }
			catch (InterruptedException e) { detail = null; }
			break;
		case LOGOUT: // always succeeds
			activeUserName = null;
			currentAccount = null;
			admin = false;
			statusCode(OK);
			break;
		case USER_CREATE:
			userString = action.getDetail();
			if (checkLoggedOut())
				handleUserCreate(action.getDetail());
			break;
		case USER_DELETE: // asynchronously delete user account info + creds
			if (!checkUserAuthenticated()) break;
			server.accounts.removeAsync(activeUserName);
			String prefix = currentAccount.uuid.toString();
			for (long l = 0; l < currentAccount.revNum; l++) {
				server.credentials.removeAsync(prefix + l);
			}
			currentAccount = null;
			activeUserName = null;
			statusCode(OK);
			break;
		case TOGGLE_TWOFACTOR:
			if (!checkUserAuthenticated()) break;
			String username = action.getDetail();
			handleTwoFactorToggle(username);
			break;
		case USERNAME_CHANGE:
			if (!checkUserAuthenticated()) break;
			String newUsername = action.getDetail();
			if (server.accounts.move(activeUserName, newUsername)) {
				activeUserName = newUsername;
				statusCode(OK);
			} else { statusCode(SERVER_ERROR); }
			break;
		case USERPASSWORD_CHANGE:
			if (!checkUserAuthenticated()) break;
			userString = action.getDetail();
			handlePasswordChange(userString);
			break;
		case ADMIN_LOGIN:
			userString = action.getDetail();
			if (!checkLoggedOut()) break;
			try { detail = handleAdminLogin(userString); }
			catch (InterruptedException e) { detail = null; }
			break;
		case ADDCREDENTIAL:
			if (checkUserAuthenticated()) {
				String uuidStr = currentAccount.uuid.toString();
				handleAddRemoveCredential(uuidStr, action.getDetail());
			}
			break;
		case REMOVECREDENTIAL:
			if (checkUserAuthenticated()) {
				String uuidStr = currentAccount.uuid.toString();
				handleAddRemoveCredential(uuidStr, action.getDetail());
			}
			break;
		case SYNC:
			if (!checkUserAuthenticated()) break;
			long clientRevNum = 0;
			try { clientRevNum = Long.parseLong(action.getDetail()); }
			catch (NumberFormatException e) { statusCode(BAD_REQUEST); break; }
			statusCode(ACCEPTED);
			Blob blob = new Blob(null, conn);
			String uuidPrefix = currentAccount.uuid.toString();
			for (long i = clientRevNum; i < currentAccount.revNum; i++) {
				// transfer UUID + encrypted credential through packetized stream
				String key = uuidPrefix + i;
				if (server.credentials.containsKey(key)) {
					out.write(1); // we're sending a revision
					server.credentials.get(uuidPrefix + i, blob);
				} else out.write(0); // skip this revision
			}
			out.write(4 /*EOT ASCII*/); // end of sync
			break;
		case GET_LOGS: // user can see own logs; admin can see all logs
			if (!admin && !checkUserAuthenticated()) {
				statusCode(BAD_REQUEST);
				break;
			}
			int entries = 0;
			try { entries = Integer.parseInt(action.getDetail()); }
			catch (NumberFormatException e) { statusCode(BAD_REQUEST); break; }
			statusCode(ACCEPTED);
			// admin can see all logs
			String filter1 = (admin ? "" : "user <" + activeUserName + ">");
			String filter2 = (admin ? "" : "user <" + currentAccount.uuid.toString() + ">");
			final DataOutputStream dos = new DataOutputStream(out);
			SandLogger.searchRecentEntries(filter1, filter2, entries,
				new Callback<String>() {
					@Override public void process(String s) {
						try { dos.writeUTF(s); }
						catch (IOException ignore) { }
					}
				});
			dos.writeUTF(""); // signal end of logs
			break;
		case BLOCK_USER:
			if (admin) {
				server.blockedUsers.putLong(action.getDetail(), 0L);
				statusCode(OK);
			} else statusCode(UNAUTHORIZED);
			break;
		case UNBLOCK_USER:
			if (admin) {
				server.blockedUsers.remove(action.getDetail());
				statusCode(OK);
			} else statusCode(UNAUTHORIZED);
			break;
		case DISCONNECT:
			statusCode(OK);
			disconnected = true;
			break;
		default:
			// unrecognized action
			statusCode(BAD_REQUEST);
		}
		SandLogger.logAction(action,
			(admin ? userString : ("user <" + userString + ">")),
			code, detail);
	}

	@Override
	public void run() {
		SandAction action = new SandAction();
		while (!disconnected && server.isRunning()) {
			try {
				action.reconstruct(in);
				handleAction(action);
			} catch (EOFException e) {
				System.out.println("Disconnecting due to EOF.");
				disconnected = true;
			} catch (ProtocolException e) {
				System.out.println("Client sent malformed Action.");
				System.out.println("Disconnecting...");
				disconnected = true;
			} catch (IOException e) {
				System.out.println("Client handler got IOException. " +
					"Probably disconnected?");
				System.out.println("Disconnecting...");
				disconnected = true;
			}
		}
		server.disconnect();
	}

}
