package sand.client;

import java.io.IOException;
import java.util.*;

import common.*;
import common.crypto.*;
import common.network.*;
import java.util.concurrent.TimeUnit;

import sand.common.*;

public class SandClientManager {

	private SandClientLocal localClient;
	private SandClientServer serverClient;
	private UserAccount userAccount;
	private boolean isConnected;
	private boolean adminAuthorized;
	private boolean loggedInOffline;
	private String host;

	// directories set by default. Manager initializes local directory with these values
	String lookupDir; // usernamehash -> [all uuids]
	String dataDir; // uuid -> [data] (for all users)
	String localAccountDir; // username -> encrypted user account.
	String accountSaltsDir; // username -> byte[] salt

	/**
	 * TODO consider throwing the IOException instead of catching
	 * Currently tries to establish a connection with the server.
	 * (no offline mode)
	 */
	public SandClientManager(String host, int port) {
		// TODO future: read properties file to set fields.
		this.lookupDir = DefaultProperties.SAND_LOOKUP_DIR;
		this.dataDir = DefaultProperties.SAND_DATA_DIR;
		this.localAccountDir = DefaultProperties.SAND_ACCOUNTS_DIR;
		this.accountSaltsDir = DefaultProperties.SAND_SALTS_DIR;

		// sets this.isConnected
		this.host = host;
		connect(host, port);

		this.localClient = new SandClientLocal(lookupDir, dataDir,
			localAccountDir, accountSaltsDir);
	}
	public SandClientManager(String host) {
		this(host, SslDefaults.DEFAULT_SAND_PORT);
	}
	public SandClientManager() {
		this(SslDefaults.DEFAULT_HOST, SslDefaults.DEFAULT_SAND_PORT);
	}

	public boolean connect(String host) {
		return connect(host, SslDefaults.DEFAULT_SAND_PORT);
	}

	public boolean connect(String host, int port) {
		this.host = host;
		try {
			this.serverClient = new SandClientServer(host, port);
			serverClient.start(); // establishes connection
			this.isConnected = true;
			return true;
		} catch (IOException e) {
			System.err.println(e.toString());
			System.out.println("Most likely cause: server is not up and running.");
			this.isConnected = false;
			return false;
		}
	}

	/**
	 * If !isConnected, userAccount should probably also be null.
	 */
	public boolean isConnectedToServer() {
		return isConnected;
	}

	public String createAccount(String username, char[] pass) {
		return createAccount(username,pass,false);
	}

	/**
	 * Create User Account
	 * Does not log in.
	 */
	public String createAccount(String username, char[] pass, boolean enable) {
		if (!isConnected)
			return null;

		try {
			this.userAccount = serverClient.createAccount(username, pass, enable);
			if (userAccount == null) {
				return null;
			}
			// persist new useraccount locally.
			boolean success = this.localClient.persistAccount(this.userAccount);
			if (!success) {
				System.out.println("Error: Unable to persist account locally.");
			}

			String token = getTwoFactorQRCode();
			this.userAccount = null; // not logged in.
			return token;
		} catch (IOException e) {
			System.err.println(e.toString());
			return null;
		}
	}

	// Login
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
			userAccount = serverClient.login(username, pass);
			if (userAccount == null) {
				System.out.println("Authentication failed.");
				return false;
			}

			// always persist useraccount. we might have created it on a different device.
			// this device needs it now.
			boolean success = this.localClient.persistAccount(this.userAccount);
			if (!success) {
				System.out.println("Error: Unable to persist account locally.");
			}
			// always sync. TODO get revision number from useraccount.
			sync();
		} catch (IOException e) {
			System.err.println(e.toString());
		}
		return userAccount != null;
	}

	public boolean twoFactorAuthenticate(long code) throws IOException {
		//Two Factor
		if (userAccount.isTwoFactorEnabled()) {
			if (code == 0) {
				this.logout();
				return false;
			}
			long t = new Date().getTime() / TimeUnit.SECONDS.toMillis(30);
			if (!TwoFactorAuth.getCodeVerification(userAccount.getTwoFactorToken(), code, t)) {
				this.logout();
				return false;
			}
		}
		return true;
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
			loggedIn = serverClient.adminLogin(pass);
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

	/**
	 * User account should already exist locally.
	 */
	public boolean loginOffline(String username, char[] pass) {
		if (loggedInOffline) {
			System.out.println("Already logged in?");
			return false;
		}
		if (userAccount != null) {
			System.out.println("Already logged in?");
			return false;
		}

		userAccount = localClient.rederiveAccount(username, pass);
		if (userAccount == null) {
			System.out.println("Failed offline login. Account not found.");
		}

		loggedInOffline = userAccount != null;
		return loggedInOffline;
	}

	/**
	 * Returns true if not isConnectedToServer already.
	 */
	public void logout() throws IOException {
		if (!isConnectedToServer() || this.serverClient == null) {
			return; // nothing to do, success.
		}

		if (this.userAccount == null && !adminAuthorized) {
			System.out.println("Not logged in, not an admin.");
			loggedInOffline = false; // just to be sure
			return;
		}

		this.serverClient.logout();

		if (adminAuthorized) {
			System.out.println("Logging out admin.");
			adminAuthorized = false;
			loggedInOffline = false; // just to be sure
			return;
		}

		// persist useraccount locally.
		boolean success = this.localClient.persistAccount(this.userAccount);
		if (!success) {
			System.out.println("Unable to persist account locally.");
		}

		this.userAccount = null;
		this.adminAuthorized = false;
		loggedInOffline = false; // just to be sure
	}

	public UUID addCredential(String service, String nickname,
			String password, String description) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return null;
		}
		if (userAccount == null) return null; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: Not connected to server.");
			return null;
		}

		UUID uuid = UUID.randomUUID();
		CredentialTuple cred = new CredentialTuple(uuid, service, nickname, password, description);

		return addCredential(cred);
	}

	/**
	 * Adds credential tuple. Tuple itself contains a UUID, so if a tuple of
	 * the uuid already exists, overwrites on both server and client.
	 */
	public UUID addCredential(CredentialTuple tup) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return null;
		}
		if (userAccount == null) return null; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: Not connected to server.");
			return null;
		}

		Encrypted<CredentialTuple> enc = new Encrypted<CredentialTuple>(
			tup, userAccount.getDataKey(), userAccount.getMACKey());
		return addCredential(enc);
	}

	private UUID addCredential(Encrypted<CredentialTuple> enc) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return null;
		}

		UUID uuid = enc.getT().getUUID();
		// comment out. the following 2 lines meant only for client-only testing.
		//localClient.addCredential(userAccount, enc, uuid);
		//return null;

		try {
			boolean success = serverClient.addCredential(userAccount, enc, uuid);
			if (!success) {
				System.out.println("addCredential failed due to server refusal.");
				return null;
			}
			success = success && localClient.addCredential(userAccount, enc, uuid);
			if (!success) {
				System.out.println("addCredential failed due to local failure.");
				return null;
			}

			return success ? uuid : null;
		} catch (IOException e) {
			System.out.println("addCredential failed due to IOException");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Username may be null or empty string. if so, not used in filter.
	 * Returns a list of credential tuples based on given host/service and (optional) username
	 */
	public List<CredentialTuple> lookupMatching(String service, String username) {
		if (userAccount == null) return null;
		// completely local operation. no way for server to do lookups.
		return localClient.lookupCredentials(userAccount, service, username);
	}

	public CredentialTuple lookupSpecific(String lastSixUuidDigits) {
		if (userAccount == null) return null;
		// completely local operation.
		return localClient.lookupSpecific(userAccount, lastSixUuidDigits);
	}

	public List<CredentialTuple> lookupAll() {
		if (userAccount == null) return null;
		// completely local op.
		return localClient.getAllCredentialTuples(userAccount);
	}

	/**
	 * Service can be null. If forced, all matching tuples get deleted regardless.
	 * Username can be null. That means that, if forced, all matching Tuples get deleted regardless
	 * False if (total) operation unsuccessful.
	 */
	public boolean removeCredential(String service, String username, boolean forced) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}
		if (userAccount == null) return false; // not logged in?
		if (!isConnectedToServer()) {
			System.err.println("Error: Not connected to server.");
			return false;
		}

		// protocol: find all locally first. service and username may be null.
		List<CredentialTuple> matches = localClient.lookupCredentials(userAccount, service, username);

		if (matches == null || matches.isEmpty() || (matches.size() > 1 && (!forced)))
			return false; // do nothing.

		boolean success = true;
		for (CredentialTuple tuple : matches) {
			UUID uuid = tuple.getUUID(); // is this the best way to do this?
			try {
				success = success && serverClient.removeCredential(userAccount, uuid);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// TODO: should the corresponding local delete happen after each success,
		//   or if all deletions succeed (i.e. atomic operation?)
		// If server fails midway, how does the server rollback from prior succesful ops?
		// else issue remove server delete requests one by one
		//   do corresponding local op

		for (CredentialTuple tuple : matches) {
			UUID uuid = tuple.getUUID(); // is this the best way to do this?
			success = success && localClient.removeCredential(userAccount, uuid);
		}

		return success;
	}

	/**
	 * separate from the other removeCredential (which does batch operations)
	 * Intended for removal of specific credential.
	 */
	public boolean removeCredential(UUID uuid) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}
		if (userAccount == null) return false; // not logged in?
		if (!isConnectedToServer()) {
			System.err.println("Error: not connected to server.");
			return false;
		}

		boolean success = true;
		try {
			success = success && serverClient.removeCredential(userAccount, uuid);
			if (!success)
				return false;

			success = success && localClient.removeCredential(userAccount, uuid);
			return success;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean changeUsername(String newUsername) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}
		if (userAccount == null) return false; // not logged in?
		if (!isConnected)
			return false;
		if (newUsername == null) return false;

		boolean success = true;
		String oldUsername = userAccount.getUsername();
		try {
			success = serverClient.changeUsername(userAccount, newUsername);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!success)
			return false;

		success = localClient.changeUsername(oldUsername, newUsername);
		if (!success) {
			System.out.println("ChangeUsername local failed... but server didn't fail... interesting");
			System.out.println("User account not persisting.");
			return false;
		}

		return success;
	}

	public boolean changePassword(char[] oldPassword, char[] newPassword) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}
		if (userAccount == null) return false; // not logged in?
		if (!isConnected)
			return false;
		if (newPassword == null) return false;

		UserAccount ua = null;
		try {
			ua = serverClient.changePassword(userAccount, oldPassword, newPassword);
		} catch (IOException e) {
			return false;
		}

		if (ua == null) return false;

		if (!localClient.changePassword(ua)) {
			System.out.println("Change password local failed... but server didn't fail... interesting");
			return false;
		}

		userAccount = ua;
		return true;
	}

	public boolean clearCache() {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}
		if (!isConnected) {
			System.out.println("You are not connected to the server.");
			System.out.println("Cache-clearing is unauthorized.");
			return false; // probably unnecessary, despite being all local.
		}
		if (userAccount == null)
			return false;

		boolean success = true;
		success = localClient.clearCache(userAccount);
		System.out.println(success ? "Success." : "Fail.");
		return success;
	}

	public boolean checkForAbnormalLogin() {
		if (!isConnected)
			return false;
		boolean abnormal = serverClient.loginWasAbnormal();
		serverClient.resetAbnormalLogin();
		return abnormal;
	}

	public String getTwoFactorQRCode(){
		if (userAccount == null) {
			System.out.println("Error: User account is null.");
			return null;
		}

		return TwoFactorAuth.getQRcodeURL(
			userAccount.getTwoFactorToken(),
			this.host, userAccount.getUsername());
	}

	public boolean toggleTwoFactor(boolean enable) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}

		if (!isConnected)
			return false;

		if (userAccount == null) {
			System.out.println("Error: User account is null.");
			return false;
		}

		userAccount.setTwoFactor(enable);
		try {
			return serverClient.toggleTwoFactor(userAccount);
		} catch (IOException e) {
			return false;
		}
	}

	public String getCurrentUsername() {
		if (userAccount == null) return null;
		return userAccount.getUsername();
	}

	// for use with sync()
	private SandClientServer.SyncCallback callbackRoutines
		= new SandClientServer.SyncCallback() {
			@Override
			public void processAdd(UUID uuid, Encrypted<CredentialTuple> enc) {
				if (!localClient.addCredential(userAccount, enc, uuid))
					System.out.println("ERROR: unable to add credential during sync");
			}
			@Override
			public void processDel(UUID uuid, CredentialDeletion del) {
				if (!localClient.removeCredential(userAccount, uuid))
					System.out.println("ERROR: unable to delete credential during sync");
			}
			@Override
			public void processFailure(UUID uuid, boolean wasAdd, IOException e) {
				if (e instanceof IntegrityException) {
					System.err.println("WHOA. You've been hacked.");
				}
				System.out.println("Unable to " + (wasAdd ? "add" : "delete") + " a credential.");
				System.out.println("  You may be offline, or not properly authenticated to the system.");
			}
		};

	public boolean sync() {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}

		if (userAccount == null) return false; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: not connected to server.");
			return false;
		}
		return sync(0); // lol need to change
	}

	// assumes connected and authenticated.
	private boolean sync(long revNum) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}

		boolean success = true;
		Encrypted<CredentialTuple> enc = new Encrypted<CredentialTuple>(
			CredentialTuple.newForReconstruction(),
			userAccount.getDataKey(), userAccount.getMACKey());
		CredentialDeletion del = new CredentialDeletion(null, userAccount.getMACKey());

		try {
			success = success && serverClient.sync(revNum, enc, del,
				callbackRoutines);
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
		}

		return success;
	}

	/**
	 * Issues a call to the server for the user's own logs.
	 * Returned list could be null if not authenticated properly,
	 *   or due to some other failure, and needs to be handled.
	 */
	public List<String> getUserLogs(int entries) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return null;
		}

		if (userAccount == null) return null; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: not connected to server.");
			return null;
		}

		try {
			return serverClient.getUserLogs(this.userAccount, entries);
		} catch (IOException e) {
			System.err.println(e.toString());
			return null;
		}
	}

	public List<String> adminGetUserLogs(int entries) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return null;
		}

		if (!adminAuthorized) {
			System.out.println("Unauthorized");
			return null;
		}

		try {
			return serverClient.getUserLogs(null, entries);
		} catch (IOException e) {
			System.err.println(e.toString());
			return null;
		}

	}

	/**
	 * block == false means unblock.
	 */
	public boolean blockUser(String username, boolean block) {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}

		if (!adminAuthorized) {
			System.out.println("Unauthorized");
			return false;
		}

		try {
			return serverClient.blockUser(username, block);
		} catch (IOException e) {
			System.out.println("Exception, unsuccessful operation.");
			return false;
		}
	}

	public boolean deleteAccount() {
		if (loggedInOffline) {
			System.out.println("Unauthorized.");
			return false;
		}

		if (userAccount == null) return false; // not logged in
		if (!isConnectedToServer()) {
			System.err.println("Error: not connected to server.");
			return false;
		}

		boolean success = true;
		try {
			success = success && serverClient.deleteAccount(userAccount);
		} catch (IOException e) {
			System.err.println(e.toString());
			return false;
		}

		if (!success)
			return false;

		// delete from local cache
		success = localClient.deleteAccount(userAccount); // rm all credentials and data locally
		userAccount = null;
		return success;
	}

	public void disconnect() throws IOException {
		if (isConnected) {
			serverClient.disconnect();
			isConnected = false;
		}
	}

	public void shutdown() throws InterruptedException {
		try { disconnect(); }
		catch (IOException ignore) { }
		localClient.shutdown();
	}

	public boolean needsTwoFactorAuthentication() {
		return userAccount.isTwoFactorEnabled();
	}
}
