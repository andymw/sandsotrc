package common.data;

import java.io.*;
import common.*;
import static common.data.Transaction.*;

/**
 * A concurrent file-backed persistent key-value store
 */
public class PersistentKeyValueStore<T extends Persistable> {

	private final TransactionProcessor<T> processor;
	private boolean shuttingDown;

	public PersistentKeyValueStore(File backingDir, int blockSize, int initCapacity)
	throws IOException {
		if (backingDir == null) {
			System.out.println("backingDir is null. setting to default '.'");
			backingDir = new File(".");
		}

		if (!PersistentKeyValueStore.checkDirOrCreate(backingDir)) {
			// means, it failed to create
			System.out.println("Directory failed to create. Not exiting...");
		}

		processor = new TransactionProcessor<T>(backingDir, blockSize, initCapacity);
		processor.start();
	}

	private static boolean checkDirOrCreate(File directory) {
		if (directory == null)
			return false;
		if (!directory.exists()) {
			boolean result = false;
			try {
				directory.mkdirs();
				result = true;
			} catch (SecurityException se) {
				se.printStackTrace();
				// handle?
			}
			return result;
		}
		return true;
	}

	private void putTxn(Transaction<T> txn) {
		try {
			processor.putTransaction(txn);
		} catch (InterruptedException e) {
			txn.succeeded = false;
			txn.setCompleted();
		}
	}

	/**
	 * Does an asynchronous get operation, reading the mapped value
	 * into the given container.
	 * @param container The object that will receive the value bound to the key.
	 *    This parameter may not be null.
	 * @return A {@link Transaction} object representing the asynchronous transaction
	 */
	public Transaction<T> getAsync(String key, T container) {
		if (shuttingDown) return null;
		Transaction<T> txn = new Transaction<T>(Type.GET, key, container);
		putTxn(txn);
		return txn;
	}

	/**
	 * Does an asynchronous put operation.
	 * @param value The value to associate with the given key. The state of this
	 *  object should <em>not</em> be mutated until the transaction is complete.
	 * @return A {@link Transaction} object representing the asynchronous transaction
	 */
	public Transaction<T> putAsync(String key, T value) {
		if (shuttingDown) return null;
		Transaction<T> txn = new Transaction<T>(Type.PUT, key, value);
		putTxn(txn);
		return txn;
	}

	/**
	 * Asynchronously removes the value bound to the given key, if any.
	 */
	public Transaction<T> removeAsync(String key) {
		if (shuttingDown) return null;
		Transaction<T> txn = new Transaction<T>(Type.DEL, key, null);
		putTxn(txn);
		return txn;
	}

	/**
	 * Asynchronously re-maps the value associated with a given key to a new key.
	 */
	public Transaction<T> moveAsync(String key, String newKey) {
		if (shuttingDown) return null;
		Transaction<T> txn = new Transaction<T>(key, newKey);
		putTxn(txn);
		return txn;
	}

	private boolean tryWait(Transaction<T> txn) {
		if (txn == null) return false;
		try {
			txn.waitCompleted();
			return txn.succeeded();
		} catch (InterruptedException e) { return false; }
	}

	/**
	 * Does a synchronous get operation.
	 * @param container The object that will receive the value bound to the key.
	 * @return true if a value was retrieved and the container represents that value
	 */
	public boolean get(String key, T container) {
		return tryWait(getAsync(key, container));
	}

	/**
	 * @return the long value mapped to key, or -1 if none
	 */
	public long getLong(String key) {
		if (shuttingDown) return -1L;
		Transaction<T> txn = new Transaction<T>(key);
		putTxn(txn);
		if (tryWait(txn)) {
			return txn.l;
		} else return -1L;
	}

	/**
	 * Does a synchronous put operation.
	 * @return true if the operation succeeded
	 */
	public boolean put(String key, T container) {
		return tryWait(putAsync(key, container));
	}

	public boolean putLong(String key, long value) {
		if (shuttingDown) return false;
		Transaction<T> txn = new Transaction<T>(key, value);
		putTxn(txn);
		return tryWait(txn);
	}

	public boolean containsKey(String key) {
		if (shuttingDown) return false;
		Transaction<T> txn = new Transaction<T>(key, Transaction.Type.EXISTS);
		putTxn(txn);
		return tryWait(txn);
	}

	/**
	 * Does a synchronous remove operation.
	 * @return true if the operation succeeded
	 */
	public boolean remove(String key) {
		return tryWait(removeAsync(key));
	}

	public boolean move(String key, String newKey) {
		return tryWait(moveAsync(key, newKey));
	}

	/**
	 * Block the calling thread until all pending
	 * asynchronous transactions have been persisted.
	 */
	public void flush() throws InterruptedException {
		processor.waitCompleted();
	}

	/**
     * Shuts down the key-value store, ensuring all pending operations are
	 * persisted, and releases the underlying resources.
	 */
	public void shutdown() throws InterruptedException {
		shuttingDown = true;
		processor.shutdown();
	}

}
