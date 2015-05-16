package common.crypto;

import common.network.PacketizedInputStream;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * A passthrough input stream that reads data from an underlying input stream
 * and additionally calculates a running MAC tag for the data.
 */
public class MACInputStream extends InputStream {

	private Mac mac = null;
	private final InputStream input;
	private final PacketizedInputStream packin;

	public MACInputStream(InputStream input, SecretKey hmacKey)
			throws InvalidKeyException, IOException {
		this.input = input;
		this.packin = new PacketizedInputStream(input);
		try {
			mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec HMACKeySpec = new SecretKeySpec(
				hmacKey.getEncoded(), "HmacSHA256");
			mac.init(HMACKeySpec);
		} catch (NoSuchAlgorithmException impossible) { }
	}

	@Override
	public int read() throws IOException {
		int b = packin.read();
		if (b != -1)
			mac.update((byte)b);
		return b;
	}

	public int read(byte[] b, int offs, int len) throws IOException {
		int num = packin.read(b, offs, len);
		if (num > 0)
			mac.update(b, offs, num);
		return num;
	}

	/**
	 * Calculates and retrieves the MAC tag of all bytes read from the stream.
	 * Reading from the stream after this method has been called
	 * results in undefined behavior.
	 * @throws {@link IntegrityException} if MAC tag check failed.
	 */
	public void doFinal() throws IOException {
		byte[] tag = mac.doFinal();
		byte[] tagRead = new byte[tag.length];
		if (input.read(tagRead) != tag.length || !Arrays.equals(tag, tagRead)) {
			throw new IntegrityException("MAC tag check failed.");
		}
	}

	@Override
	public void close() throws IOException {
		input.close();
	}

}
