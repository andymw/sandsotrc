package common.data;

import java.io.*;
import java.math.*;
import java.security.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

class HashIndex {
	private static final String INDEX_ONE_FILE = ".index.1";
	private static final String INDEX_TWO_FILE = ".index.2";

	private static final byte EMPTY = 0;
	private static final byte ADDR = 1;
	private static final byte LONG = 2;

	private static final float MAX_LOAD_FACTOR = 0.7f;

	// 16-byte hash + 1 byte type + 8-byte ptr
	private static final int ROW_SIZE = 25;
	private static final int HEADER_SIZE = 12;

	private final File backingDir;
	private RandomAccessFile index1, index2;
	private int size1, size2;
	private int capacity1, capacity2;
	private final byte[] hashBuf;
	private final MessageDigest sha256hasher;

	/**
	 * Creates a new HashIndex, or restores a previously persisted index,
	 * from the given backing directory.
	 * @param initCapacity
	 *   The initial capacity of the index, if it is created anew.
	 *   This parameter is ignored if a persisted index already exists.
	 */
	HashIndex(File backingDir, int initCapacity) throws IOException {
		this.backingDir = backingDir;
		File index1file = new File(backingDir, INDEX_ONE_FILE);
		File index2file = new File(backingDir, INDEX_TWO_FILE);
		index2 = new RandomAccessFile(index2file, "rw");
		if (index2.length() > 0) {
			index2.readInt(); // header size
			size2 = index2.readInt();
			capacity2 = index2.readInt();
			if (index1file.exists()) {
				index1 = new RandomAccessFile(index1file, "rw");
				index1.readInt(); // header size
				size1 = index1.readInt();
				capacity1 = index1.readInt();
			}
		} else {
			capacity2 = initCapacity;
			index2.setLength(HEADER_SIZE + capacity2 * ROW_SIZE);
			writeHeader(index2, size2, capacity2);
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException impossible) { }
		sha256hasher = md;
		hashBuf = new byte[16];
	}

	/**
	 * Returns the row address where the given key is located,
	 * or where it would be inserted if it is not found.
	 * Also seeks to that address in the index file.
	 */
	private long probe(byte[] hash, RandomAccessFile index, int capacity)
	throws IOException {
		long idx = mod(hash, capacity) * ROW_SIZE + HEADER_SIZE;
		for (;;) {
			index.seek(idx);
			if (index.read() == EMPTY)
				break;
			index.read(hashBuf);
			if (Arrays.equals(hash, hashBuf)) {
				break; // address of row
			} else {
				idx += ROW_SIZE; // skip to next row
			}
		}
		index.seek(idx);
		return idx;
	}

	private void writeHeader(RandomAccessFile index, int size, int capacity)
	throws IOException {
		index.seek(0L);
		index.writeInt(HEADER_SIZE);
		index.writeInt(size);
		index.writeInt(capacity);
	}

	/**
     * Looks up the address or long associated with the given key.
	 * @param throwEx throw IOException instead of returning 0L for not found
	 * @return the address/long, or 0L if key is not mapped.
	 */
	long lookup(String key, boolean throwEx) throws IOException {
		byte[] hash = hash(key);
		long block = probe(hash, index2, capacity2);
		if (index2.read() == EMPTY) { // not found in index2 - check index
			if (index1 == null) {
				if (throwEx) throw new IOException("Not found.");
				else return 0L;
			}
			block = probe(hash, index1, capacity1);
			if (index1.read() == EMPTY) {
				if (throwEx) throw new IOException("Not found.");
				return 0L; // not found in either
			}
			index1.seek(block + 17); // skip hash and type
			return index1.readLong();
		} else {
			index2.seek(block + 17); // skip hash and type
			return index2.readLong();
		}
	}

	/**
	 * Equivalent to lookup(key, false).
     * @see #lookup(String, boolean)
	 */
	long lookup(String key) throws IOException {
		return lookup(key, false);
	}

	/**
	 * Deletes the mapping associated with the given key, if any.
	 * @return The address previously mapped to the key, or 0L if none
	 */
	long delete(String key) throws IOException {
		byte[] hash = hash(key);
		long block = probe(hash, index2, capacity2);
		long ptr = 0L;
		int type = index2.read();
		if (type != EMPTY) { // delete from index2
			index2.seek(block + 17);
			ptr = (type == ADDR) ? index2.readLong() : 0L;
			size2--;
			index2.seek(block);
			index2.write(EMPTY);
			writeHeader(index2, size2, capacity2);
		} else if (index1 != null) {
			block = probe(hash, index1, capacity1);
			type = index1.read();
			if (type != EMPTY) { // delete from index1
				index1.seek(block + 17);
				ptr = (type == ADDR) ? index1.readLong() : 0L;
				size1--;
				index1.seek(block);
				index1.write(EMPTY);
				writeHeader(index1, size1, capacity1);
			}
		}
		return ptr;
	}

	/**
	 * If there is already an address associated with the given key,
	 * returns that address and ignores the {@code addr} argument;
	 * else stores the given address and returns the same address.
	 */
	long updateAddr(String key, long addr) throws IOException {
		byte[] hash = hash(key);
		long block;
		if (index1 != null) { // look in index1
			block = probe(hash, index1, capacity1);
			if (index1.read() != EMPTY) { // found in index1
				index1.seek(block + 17);
				return index1.readLong();
			}
		}
		block = probe(hash, index2, capacity2);
		if (index2.read() != EMPTY) { // found in index2
			index2.seek(block + 17);
			return index2.readLong();
		}

		// not found - create new row
		if ((float)size2 / capacity2 > MAX_LOAD_FACTOR && index1 == null) {
			// start a new doubling
			move2to1();
			block = probe(hash, index2, capacity2);
		}
		index2.seek(block);
		index2.write(ADDR);
		index2.write(hash);
		index2.writeLong(addr);
		size2++;
		writeHeader(index2, size2, capacity2);
		return addr;
	}

	boolean putLong(String key, long val) throws IOException {
		byte[] hash = hash(key);
		long block;
		if (index1 != null) { // look in index 1
			block = probe(hash, index1, capacity1);
			int type = index1.read();
			if (type == LONG) { // found in index1 - overwrite
				index1.seek(block + 17);
				index1.writeLong(val);
				return true;
			} else if (type == ADDR) return false; // wrong type
		}
		block = probe(hash, index2, capacity2);
		if (index2.read() == ADDR) return false; // wrong type
		index2.seek(block);
		index2.write(LONG);
		index2.write(hash);
		index2.writeLong(val);
		return true;
	}

	/**
	 * Closes the HashStore, freeing any underlying resources.
	 */
	void close() throws IOException {
		index2.close();
		if (index1 != null) {
			index1.close();
		}
	}

	private void move2to1() throws IOException {
		size1 = size2;
		capacity1 = capacity2;
		size2 = 0;
		capacity2 *= 2;
		index2.close();
		new File(backingDir, INDEX_TWO_FILE).renameTo(new File(backingDir, INDEX_ONE_FILE));
		index1 = new RandomAccessFile(new File(backingDir, INDEX_ONE_FILE), "rw");
		index2 = new RandomAccessFile(new File(backingDir, INDEX_TWO_FILE), "rw");
		index2.setLength(HEADER_SIZE + capacity2 * ROW_SIZE);
	}

	/**
	 * If the table is still split, rehashes and moves one entry from index to index2.
	 * @return true if an entry was rehashed, false otherwise
	 */
	boolean rehashOne() {
		// TODO
		return false;
	}

	private static long mod(byte[] b, long m) {
		return new BigInteger(b)
			.mod(BigInteger.valueOf(m)).longValue();
	}

	// least significant 128 bits of SHA-256 hash
	private byte[] hash(String input) {
		sha256hasher.reset();
		byte[] trunc = new byte[16];
		byte[] hash = sha256hasher.digest(input.getBytes(UTF_8));
		System.arraycopy(hash, 16, trunc, 0, 16);
		return trunc;
	}

}
