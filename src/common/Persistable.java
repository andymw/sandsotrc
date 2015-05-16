package common;

import java.io.*;

public interface Persistable {

	/**
	 * Persist the state of the object to the given DataOutput
	 */
	public void persist(OutputStream output) throws IOException;

	/**
	 * Read an object state from the given DataInput and set the state of
	 * the current object to the reconstructed state.
	 */
	public void reconstruct(InputStream input) throws IOException;

}
