package common.network;

import java.io.*;
import common.*;

public class StatusCode implements Persistable {

	/** Sent to indicate the server is expecting more input from client. */
	public static final String CONTINUE = "100";
	/** Sent to indicate an operation succeeded. */
	public static final String OK = "200";
	/** Sent to indicate a client request was accepted
	 * and the server is sending more data.
	 * SOTRC: accepted invite. */
	public static final String ACCEPTED = "202";
	/** Used for abnormal login status. */
	public static final String ABNORMAL = "306";
	/** Sent to indicate an invalid request from a client. */
	public static final String BAD_REQUEST = "400";
	/** Sent when a client tries to perform an operation while not logged in,
	 * and also to indicate an unsuccessful login attempt with bad password. */
	public static final String UNAUTHORIZED = "401";
	/** Sent to indicate a particular username is already in use. */
	public static final String FORBIDDEN = "403";
	/** Sent to indicate an unsuccessful login attempt with nonexistent username. */
	public static final String NOT_FOUND = "404";
	/** SOTRC: rejecting an invitation or chat. */
	public static final String REJECTED = "418"; // I'm a teapot.
	/** Sent to indicate an unspecified internal server error. */
	public static final String SERVER_ERROR = "500";

	private String code;
	private final byte[] buf = new byte[3];

	public StatusCode(String code) {
		this.code = code;
	}

	public StatusCode setTo(String code) {
		this.code = code;
		return this;
	}

	public static StatusCode read(InputStream is) throws IOException {
		StatusCode code = new StatusCode(null);
		code.reconstruct(is);
		return code;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		os.write(code.getBytes("UTF-8"), 0, 3);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		is.read(buf);
		code = new String(buf, "UTF-8");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof StatusCode ? code.equals(((StatusCode)obj).code)
			: obj instanceof String ? code.equals((String)obj) : false;
	}

	@Override
	public String toString() {
		return code;
	}

}
