package common.crypto;

import common.network.PacketizedOutputStream;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * A passthrough output stream that writes data to an underlying output stream
 * and in addition computes a running MAC tag of all bytes written.
 */
public class MACOutputStream extends OutputStream {

	private Mac mac = null;
	private final OutputStream output;
	private final PacketizedOutputStream packout;
	private boolean finalized;

	public MACOutputStream(OutputStream output, SecretKey HMACKey)
			throws InvalidKeyException, IOException {
		this.output = output;
		this.packout = new PacketizedOutputStream(output);
		try {
			mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec HMACKeySpec = new SecretKeySpec(
				HMACKey.getEncoded(), "HmacSHA256");
			mac.init(HMACKeySpec);
		} catch (NoSuchAlgorithmException impossible) { }
	}

	@Override
	public void write(int b) throws IOException {
		packout.write(b);
		mac.update((byte)b);
	}

	@Override
	public void write(byte[] dataToWrite, int offs, int len) throws IOException {
		packout.write(dataToWrite, offs, len);
		mac.update(dataToWrite, offs, len);
	}

	@Override
	public void flush() { }

	/**
	 * Calculates and retrieves the MAC tag of all bytes written to the stream.
	 * Writing to the stream after this method has been called
	 * results in undefined behavior.
	 */
	public void doFinal() throws IOException {
		packout.finish();
		byte[] tag = mac.doFinal();
		output.write(tag);
		output.flush();
		finalized = true;
	}

	@Override
	public void close() throws IOException {
		if (!finalized) doFinal();
		output.close();
	}

}
