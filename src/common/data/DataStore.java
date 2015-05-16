package common.data;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

class DataStore {
	private static final String STORE_NAME = ".store";

	final int blockSize;
	private long freeBlock, numBlocks;
	final RandomAccessFile backingFile;
	private final Map<Long, Lock> blockLocks;

	private static final int HEADER_SIZE = 24;

	/**
	 * Creates a new DataStore, or restores a previously existed store,
	 * from the given backing directory.
	 * @param blockSize
	 *   The block szie of the store, if it is created anew.
	 *   This parameter is ignored if a persisted store already exists.
	 */
	DataStore(File backingDir, int blockSize) throws IOException {
		this.backingFile = new RandomAccessFile(new File(backingDir, STORE_NAME), "rwd");
		blockLocks = new HashMap<Long, Lock>();
		if (backingFile.length() == 0) { // initialize new store
			this.blockSize = blockSize;
			freeBlock = 0L;
			writeHeader();
		} else { // restore from existing
			backingFile.readInt(); // header size
			this.blockSize = backingFile.readInt();
			freeBlock = backingFile.readLong();
			numBlocks = backingFile.readLong();
		}
	}

	private void writeHeader() throws IOException {
		backingFile.seek(0L);
		backingFile.writeInt(HEADER_SIZE);
		backingFile.writeInt(blockSize);
		backingFile.writeLong(freeBlock); // first free block pointer
		backingFile.writeLong(numBlocks);
	}

	/**
	 * Allocates a new block.
	 * @return the byte offset of the new block
	 */
	long allocNew() throws IOException {
		long newBlock;
		if (freeBlock == 0L) {
			// extend file - create new block at end
			newBlock = nextBlock();
			numBlocks++;
			backingFile.setLength(newBlock + blockSize + 8);
		} else {
			// take a block from the free list
			backingFile.seek(freeBlock + blockSize);
			newBlock = freeBlock;
			freeBlock = backingFile.readLong();
		}
		backingFile.seek(newBlock + blockSize);
		backingFile.writeLong(0L);
		writeHeader();
		return newBlock;
	}

	/**
	 * Returns the block address that would be returned by the next call to
	 * {@link allocNew}, without actually allocating the block.
	 */
	long nextBlock() throws IOException {
		return freeBlock != 0L ? freeBlock : numBlocks * (blockSize+8) + HEADER_SIZE;
	}

	void lockBlock(long blocknum, boolean lock) {
		Lock l;
		if (blockLocks.containsKey(blocknum)) {
			l = blockLocks.get(blocknum);
		} else {
			l = new ReentrantLock();
			blockLocks.put(blocknum, l);
		}
		if (lock)
			l.lock();
		else l.unlock();
	}

	/**
	 * Allocates a new block and sets the curBlock's pointer to the new blcok.
	 */
	long allocNext(long curBlock) throws IOException {
		long newBlock = allocNew();
		backingFile.seek(curBlock + blockSize);
		backingFile.writeLong(newBlock);
		return newBlock;
	}

	/**
	 * Returns the next block referenced by curBlock, allocating a new block
	 * if curBlock does not reference another block.
	 */
	long getNext(long curBlock) throws IOException {
		backingFile.seek(curBlock + blockSize);
		long nextBlock = backingFile.readLong();
		if (nextBlock == 0L)
			nextBlock = allocNext(curBlock);
		return nextBlock;
	}

	/**
	 * Frees all blocks referenced by the current block, but not the current block.
	 * Call this after completing a write that overwrites previous data.
	 */
	void truncate(long curBlock) throws IOException {
		backingFile.seek(curBlock + blockSize);
		long nextBlock = backingFile.readLong();
		if (nextBlock == 0L) return;
		backingFile.seek(curBlock + blockSize);
		backingFile.writeLong(0L);
		freeBlocks(nextBlock);
	}

	/**
	 * Frees the given block and all blocks referenced by it.
	 */
	void freeBlocks(long blockptr) throws IOException {
		long curBlock;
		long nextBlock = blockptr;
		do { // seek to the end of this block chain
			curBlock = nextBlock;
			backingFile.seek(curBlock + blockSize);
			nextBlock = backingFile.readLong();
		} while (nextBlock != 0L);
		// prepend the chain to the free list
		backingFile.seek(curBlock + blockSize);
		backingFile.writeLong(freeBlock);
		freeBlock = blockptr;
		writeHeader();
	}

	InputStream getInputStream(long blocknum) throws IOException {
		return new DataStoreInputStream(this, blocknum);
	}

	OutputStream getOutputStream(long blocknum) {
		return new DataStoreOutputStream(this, blocknum);
	}

	void close() throws IOException {
		backingFile.close();
	}

}
