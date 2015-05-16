package common.data;

import java.io.*;
import java.util.concurrent.*;

import common.*;

class TransactionProcessor<T extends Persistable> extends Thread {

	private final BlockingQueue<Transaction<T>> txnQueue;
	private final BlockingQueue<Transaction<T>> eventQueue;
	private final Thread eventProcessor;
	private final HashIndex index;
	private final DataStore store;
	private volatile boolean running;

	TransactionProcessor(File backingDir, int blockSize, int initCapacity)
	throws IOException {
		super();
		setDaemon(true);
		txnQueue = new LinkedBlockingQueue<Transaction<T>>();
		eventQueue = new LinkedBlockingQueue<Transaction<T>>();
		eventProcessor = new Thread(new Runnable() {
			@Override public void run() {
				while (running) {
					try {
						Transaction<T> txn = eventQueue.take();
						txn.boundEvent.process(txn);
						synchronized (eventQueue) {
							if (eventQueue.size() == 0) {
								eventQueue.notify();
							}
						}
					} catch (InterruptedException e) { break; }
				}
			}
		});
		eventProcessor.setDaemon(true);
		index = new HashIndex(backingDir, initCapacity);
		store = new DataStore(backingDir, blockSize);
	}

	void putTransaction(Transaction<T> txn) throws InterruptedException {
		txnQueue.put(txn);
	}

	void waitCompleted() throws InterruptedException {
		synchronized (txnQueue) {
			while (txnQueue.size() > 0) {
				txnQueue.wait();
			}
		}
		synchronized (eventQueue) {
			while (eventQueue.size() > 0) {
				eventQueue.wait();
			}
		}
	}

	private void processTransaction(Transaction<T> txn) {
		switch (txn.type) {
		case GET:
			try {
				long addr = index.lookup(txn.key);
				if (addr == 0L) { // probably because key is not in index
					txn.succeeded = false;
				} else {
					InputStream is = store.getInputStream(addr);
					txn.value.reconstruct(is);
					is.close();
					txn.succeeded = true;
				}
			} catch (IOException e) {
				//e.printStackTrace();
				txn.succeeded = false;
			}
			break;
		case GETL:
			try {
				txn.l = index.lookup(txn.key, true);
				txn.succeeded = true;
			} catch (IOException e) {
				txn.succeeded = false;
			}
			break;
		case PUT:
			try {
				long addr = index.updateAddr(txn.key, store.nextBlock());
				if (addr == store.nextBlock()) // not overwriting prev. value
					addr = store.allocNew();
				OutputStream os = store.getOutputStream(addr);
				txn.value.persist(os);
				os.close();
				txn.succeeded = true;
			} catch (IOException e) {
				txn.succeeded = false;
			}
			break;
		case PUTL:
			try {
				txn.succeeded = index.putLong(txn.key, txn.l);
			} catch (IOException e) {
				//e.printStackTrace();
				txn.succeeded = false;
			}
			break;
		case DEL:
			try {
				long addr = index.delete(txn.key);
				if (addr != 0L) {
					store.freeBlocks(addr);
				}
				txn.succeeded = true;
			} catch (IOException e) {
				txn.succeeded = false;
			}
			break;
		case MOVE:
			try {
				txn.succeeded = false;
				long addr = index.lookup(txn.key);
				if (addr != 0L && index.updateAddr(txn.newKey, addr) == addr) {
					index.delete(txn.key);
					txn.succeeded = true;
				}
			} catch (IOException e) { }
			break;
		case EXISTS:
			try {
				index.lookup(txn.key, true); // throws exception if not found
				txn.succeeded = true;
			} catch (IOException e) { txn.succeeded = false; }
		}
		txn.setCompleted();
		if (txn.boundEvent != null) {
			try {
				eventQueue.put(txn);
			} catch (InterruptedException ignore) { }
		}
	}

	/**
	 * Gracefully shuts down the transaction processor as soon as possible.
	 * Blocks the calling thread until shutdown is complete.
	 * Does not process all pending transactions, but does not leave any
	 * transaction in an inconsistent half-processed state.
	 */
	public void shutdown() throws InterruptedException {
		running = false;
		interrupt();
		eventProcessor.interrupt();
		for (Transaction<T> txn : txnQueue) {
			txn.succeeded = false;
			txn.setCompleted();
		}
		join();
		try {
			index.close();
		} catch (IOException ignore) { }
		try {
			store.close();
		} catch (IOException ignore) { }
	}

	@Override
	public void run() {
		running = true;
		eventProcessor.start();
		while (running) {
			try {
				// process one transaction
				processTransaction(txnQueue.take());
			} catch (InterruptedException e) { break; }
			index.rehashOne(); // process one rehash
			if (txnQueue.size() == 0) {
				synchronized (txnQueue) {
					txnQueue.notify();
				}
				// keep rehashing until rehash complete
				// or another transaction arrives
				while (running && txnQueue.size() == 0 && index.rehashOne());
			}
		}
	}

}
