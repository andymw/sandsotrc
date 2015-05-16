package common.data;

import java.io.*;

class DataStoreOutputStream extends OutputStream {

	private final DataStore store;
	private final long startBlock;
	private long currentBlock;
	private int bytesWritten;

	DataStoreOutputStream(DataStore store, long startBlock) {
		this.startBlock = startBlock;
		this.currentBlock = startBlock;
		this.store = store;
		bytesWritten = 4; // reserve space for length information
		store.lockBlock(startBlock, true);
	}

	private void prepareWrite() throws IOException {
		int offset = bytesWritten % store.blockSize;
		if (offset == 0)
			currentBlock = store.getNext(currentBlock);
		store.backingFile.seek(currentBlock + offset);
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (store.backingFile) {
			prepareWrite();
			store.backingFile.write(b);
		}
		bytesWritten++;
	}

	@Override
	public void write(byte[] bytes, int offs, int len) throws IOException {
		while (len > 0) {
			int numToWrite = Math.min(len,
				store.blockSize - (bytesWritten % store.blockSize));
			if (numToWrite == store.blockSize) // will be moving to new block
				numToWrite = Math.min(len, store.blockSize);
			synchronized (store.backingFile) {
				prepareWrite();
				store.backingFile.write(bytes, offs, numToWrite);
			}
			len -= numToWrite;
			offs += numToWrite;
			bytesWritten += numToWrite;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (store.backingFile) {
			store.truncate(currentBlock);
			store.backingFile.seek(startBlock);
			store.backingFile.writeInt(bytesWritten - 4);
		}
		store.lockBlock(startBlock, false);
	}

}
