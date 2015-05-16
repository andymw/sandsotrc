package common;

import java.io.*;

/**
 * Written specially for CredentialLookup and CredentialTuple usage
 */
public class Union<T extends Persistable, U extends Persistable> implements Persistable {
	public T t;
	public U u;
	private boolean isT;

	public Union(T t, U u, boolean isT) {
		this.t = t;
		this.u = u;
		this.isT = isT;
	}

	public void setT(T t) { this.t = t; isT = true; }

	public void setU(U u) { this.u = u; isT = false; }

	public T getT() { return isT ? t : null; }

	public U getU() { return isT ? null : u; }

	public boolean isT() { return isT; }

	public void persist(OutputStream output) throws IOException {
		if (isT) {
			output.write(0);
			t.persist(output);
		} else {
			output.write(1);
			u.persist(output);
		}
	}

	public void reconstruct(InputStream input) throws IOException {
		if (input.read() == 0) {
			isT = true;
			t.reconstruct(input);
		} else {
			isT = false;
			u.reconstruct(input);
		}
	}

	public String toString() {
		if (isT) {
			return t.toString();
		} else {
			return u.toString();
		}
	}

}
