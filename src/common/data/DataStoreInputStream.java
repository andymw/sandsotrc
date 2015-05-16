package common.data;

import java.io.*;

class DataStoreInputStream extends InputStream {

	private final DataStore store;
	private long startBlock, currentBlock;
	private int length;
	private int bytesRead;

	DataStoreInputStream(DataStore store, long startBlock) throws IOException {
		this.startBlock = startBlock;
		this.currentBlock = startBlock;
		this.store = store;
		store.backingFile.seek(startBlock);
		synchronized (store.backingFile) {
			length = store.backingFile.readInt() + 4;
		}
		bytesRead = 4;
		store.lockBlock(startBlock, true);
	}

	private void prepareRead() throws IOException {
		int offset = bytesRead % store.blockSize;
		if (offset == 0)
			currentBlock = store.getNext(currentBlock);
		store.backingFile.seek(currentBlock + offset);
	}

	@Override
	public int read() throws IOException {
		synchronized (store.backingFile) {
			if (bytesRead == length)
				return -1;
			prepareRead();
			bytesRead++;
			return store.backingFile.read();
		}
	}

	@Override
	public int read(byte[] bytes, int offs, int len) throws IOException {
		if (bytesRead == length)
			return -1;
		len = Math.min(len, length - bytesRead); // can't read past end of stream
		int ret = len;
		while (len > 0) {
			int numToRead = Math.min(len,
				store.blockSize - (bytesRead % store.blockSize));
			if (numToRead == store.blockSize) // will be moving to new block
				numToRead = Math.min(len, store.blockSize);
			synchronized (store.backingFile) {
				prepareRead();
				if (store.backingFile.read(bytes, offs, numToRead) != numToRead)
					throw new IOException("Store file corrupted.");
			}
			len -= numToRead;
			offs += numToRead;
			bytesRead += numToRead;
		}
		return ret;
	}

	@Override
	public void close() {
		store.lockBlock(startBlock, false);
	}

}
