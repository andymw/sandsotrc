package sand.client;

import common.data.*;
import common.crypto.*;

import java.io.*;
import java.util.*;
import javax.crypto.*;

import sand.common.*;

/**
 * Class handles local operations. Called by the manager.
 */
public class SandClientLocal {
	// guessing on how big a CredentialTuple object is
	private static final int BLOCKSIZE = 256;
	// a bit big, but avoids a rehash for a while
	private static final int INITIAL_CAPACITY = 256;

	/** A key-value store of credentials
	 * Format:
	 *   username -> [g1,g2] (CredentialLookup)
	 *   "g1" -> <h1,u1,p1> (CredentialTuple)
	 *   "g2" -> <h2,u2,p2>
	 * methods: Txn get, Txn put, Txn remove, void flush, void shutdown
	 */
	public PersistentKeyValueStore<CredentialLookup> credLookup;
	// for all users
	public PersistentKeyValueStore<Encrypted<CredentialTuple>> credentials;
	// useraccount cache lookup
	public PersistentKeyValueStore<UserAccount> localAccounts;
	// local salts for UserAccount reconstruction
	public PersistentKeyValueStore<PersistingSalt> accountSalts;

	/**
	 * Creates a SandClientLocal object. If the given filepath string is null,
	 *   creates a default PersistentKeyValueStore at "."
	 * Default: no client-side logging
	 */
	public SandClientLocal(String lookupdir, String datadir,
			String localAccountDir, String accountSaltsDir) {
		try {
			this.credLookup = new PersistentKeyValueStore<CredentialLookup>(
				new File(lookupdir),
				SandClientLocal.BLOCKSIZE,
				SandClientLocal.INITIAL_CAPACITY);
			this.credentials = new PersistentKeyValueStore<Encrypted<CredentialTuple>> (
				new File(datadir),
				SandClientLocal.BLOCKSIZE,
				SandClientLocal.INITIAL_CAPACITY);
			this.localAccounts = new PersistentKeyValueStore<UserAccount>(
				new File(localAccountDir),
				SandClientLocal.BLOCKSIZE,
				SandClientLocal.INITIAL_CAPACITY);
			this.accountSalts = new PersistentKeyValueStore<PersistingSalt>(
				new File(accountSaltsDir),
				SandClientLocal.BLOCKSIZE,
				SandClientLocal.INITIAL_CAPACITY);
		} catch (IOException e) {
			e.printStackTrace();
			this.credLookup = null;
			this.credentials = null;
			this.localAccounts = null;
			this.accountSalts = null;
		}
	}

	/**
	 * Look it up in kvstore.
	 * if not there, put in both a CredentialTuple and CredentialLookup
	 * Method is not responsible for checking if another credential with the
	 *   same host/pass/description exists.
	 * For each new lookup entry, add to userLookup.
	 */
	public boolean addCredential(UserAccount acct, Encrypted<CredentialTuple> tuple, UUID uuid) {
		if (acct == null || tuple == null || uuid == null) return false;

		String hashID = UserAccount.getUniqueID(acct);
		CredentialLookup lookup = new CredentialLookup();

		// get the lookup table for the user.
		credLookup.get(hashID, lookup); // if fails, no problem, it's new.

		// lookupobj: add uuid
		lookup.put(uuid);
		// credlookup: userhash -> updated lookupobj
		boolean success = credLookup.put(hashID, lookup);
		// creds: uuid -> data
		success = success && credentials.put(uuid.toString(), tuple);
		return success;
	}

	/**
	 * Removal of specific credential by uuid.
	 */
	public boolean removeCredential(UserAccount acct, UUID uuid) {
		if (acct == null || uuid == null) return false;

		String hashID = UserAccount.getUniqueID(acct);

		CredentialLookup lookup = new CredentialLookup();
		if (!credLookup.get(hashID, lookup)) {
			System.out.println("removeCredential : entry for " + acct.getUsername() + " not found");
			return false;
		}

		Encrypted<CredentialTuple> encTuple = new Encrypted<CredentialTuple>(
			CredentialTuple.newForReconstruction(), acct.getDataKey(), acct.getMACKey());
		boolean success = true;

		for (UUID credKey : lookup.credentialTupleKeys) {
			if (!credentials.get(credKey.toString(), encTuple)) {
				System.out.println("removeCredential: a failed get");
				continue;
			}

			encTuple.getT();

			if (credKey.equals(uuid)) { // found it
				// remove this one
				success = success && credentials.remove(credKey.toString());
				// now remove its pointer in the lookup list
				lookup.credentialTupleKeys.remove(credKey);
				// reconstruct lookup (overwrite)
				success = success && credLookup.put(hashID, lookup);
				break;
			}

		}

		// logs only if clientSideLogging enabled and operation was successful
		return success;
	}

	/**
	 * Returns a list of UUIDs relevant to a particular user account.
	 */
	public List<UUID> getAllCredentials(UserAccount acct) {
		if (acct == null) return null;

		String hashID = UserAccount.getUniqueID(acct);
		CredentialLookup lookup = new CredentialLookup();

		if (!credLookup.get(hashID, lookup)) {
			return new ArrayList<UUID>();
		}

		List<UUID> uuidList = new ArrayList<UUID>(lookup.credentialTupleKeys);
		return uuidList;
	}

	/**
	 * Given a user account, returns all credentials of the account.
	 */
	public List<CredentialTuple> getAllCredentialTuples(UserAccount acct) {
		List<UUID> uuidList = getAllCredentials(acct);
		if (uuidList == null) return null;

		List<CredentialTuple> credList = new ArrayList<CredentialTuple>();

		for (UUID uuid : uuidList) {
			credList.add(getCredentialOfUUID(acct, uuid));
		}

		return credList;
	}

	/**
	 * Essentially a map function of Uuid -> CredentialTuple.
	 */
	public CredentialTuple getCredentialOfUUID(
			UserAccount acct, UUID uuid) {
		if (uuid == null) return null;

		Encrypted<CredentialTuple> encTuple = new Encrypted<CredentialTuple>(
			CredentialTuple.newForReconstruction(), acct.getDataKey(), acct.getMACKey());

		if (credentials.get(uuid.toString(), encTuple)) {
			return encTuple.getT();
		}

		System.out.println("getCredentialOfUUID: a failed get");
		return null;
	}

	/**
	 * Returns String list representing credentials related to given host.
	 * if site is null, returns all results. if username is null, not used in filter
	 */
	public List<CredentialTuple> lookupCredentials(UserAccount acct,
			String site, String username) {
		if (acct == null) return null;
		if (site == null || site.equals(""))
			return getAllCredentialTuples(acct);

		String hashID = UserAccount.getUniqueID(acct);

		CredentialLookup lookup = new CredentialLookup();
		if (!credLookup.get(hashID, lookup)) {
			System.out.println("lookupCredentials : user entry not found");
			return null;
		}

		List<CredentialTuple> credList = new ArrayList<CredentialTuple>();
		// placeholder for tuple search
		Encrypted<CredentialTuple> encTuple = null;
		for (UUID credKey : lookup.credentialTupleKeys) {
			// we need a new encTuple every time.
			encTuple = new Encrypted<CredentialTuple>(CredentialTuple.newForReconstruction(),
				acct.getDataKey(), acct.getMACKey());
			if (credentials.get(credKey.toString(), encTuple)) {
				CredentialTuple tup = encTuple.getT();
				if (!tup.host.equals(site)) continue;
				if (username == null || username.equals("") ||
					tup.username.equals(username)) {
					credList.add(tup);
				}
			} else {
				System.out.println("lookupCredentialsByHostname: a failed get");
			}
		}

		return credList;
	}

	public CredentialTuple lookupSpecific(UserAccount acct, String lastSixUuidDigits) {
		if (acct == null) return null;
		if (lastSixUuidDigits == null || lastSixUuidDigits.length() != 6) return null;

		List<UUID> uuidList = getAllCredentials(acct);
		for (UUID uuid : uuidList) {
			String uuidString = uuid.toString();
			if (uuidString.substring(Math.max(0, uuidString.length() - 6))
					.equals(lastSixUuidDigits)) {
				return getCredentialOfUUID(acct, uuid);
			}
		}

		return null;
	}

	public boolean changeUsername(String oldUsername, String newUsername) {
		return localAccounts.move(oldUsername, newUsername)
			&& accountSalts.move(oldUsername, newUsername);
	}

	public boolean changePassword(UserAccount ua) {
		return localAccounts.put(ua.getUsername(), ua);
	}

	/**
	 * Lookup entry related to account. Delete all credentials.
	 * Does not de-register account from local data.
	 */
	public boolean clearCache(UserAccount acct) {
		List<UUID> uuids = getAllCredentials(acct);
		boolean success = true;
		for (UUID uuid : uuids) {
			success = success && removeCredential(acct, uuid);
		}
		return success;
	}


	/**
	 * Called from Manager during create account, sync, and logout. (?)
	 */
	public boolean persistAccount(UserAccount acct) {
		boolean success = true;
		success = success && accountSalts.put(acct.getUsername(), new PersistingSalt(acct.getSalt()));
		success = success && localAccounts.put(acct.getUsername(), acct);
		return success;
	}

	/**
	 * The
	 */
	public UserAccount rederiveAccount(String username, char[] password) {
		boolean success = true;
		PersistingSalt uaSalt = new PersistingSalt(null);
		success = success && accountSalts.get(username, uaSalt);
		if (!success) {
			System.out.println("rederiveAccount: Salt for username not found.");
			return null;
		}

		SecretKey masterKey = CryptoUtils.deriveAESKey(password, uaSalt.getSalt());

		UserAccount ua = new UserAccount(masterKey); // new for reconstruction
		// the following get fails with an incorrectly-made user account.
		success = success && localAccounts.get(username, ua);
		if (!success) {
			System.out.println("rederiveAccount: user account not found.");
			return null;
		}

		return ua;
	}

	public boolean deleteAccount(UserAccount acct) {
		return this.clearCache(acct)
			&& localAccounts.remove(acct.getUsername())
			&& accountSalts.remove(acct.getUsername());
	}

	public void shutdown() throws InterruptedException {
		credLookup.shutdown();
		credentials.shutdown();
		accountSalts.shutdown();
		localAccounts.shutdown();
	}
}
