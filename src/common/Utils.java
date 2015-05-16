package common;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

/**
 * Class containing various static utility methods.
 */
public class Utils {

	private Utils () { }

	private static final BigInteger BI_2_64 = BigInteger.ONE.shiftLeft(64);

	public static void streamTransfer(InputStream is, OutputStream os)
	throws IOException {
		byte[] buffer = new byte[1024];
		int numRead;
		while ( (numRead = is.read(buffer)) > 0 ) {
			os.write(buffer, 0, numRead);
		}
	}

	public static byte[] readStreamFully(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		streamTransfer(is, os);
		return os.toByteArray();
	}

	public static void writeInt(int i, OutputStream os) throws IOException {
		byte[] intBuf = new byte[4];
		intBuf[0] = (byte)(i >> 24);
		intBuf[1] = (byte)(i >> 16);
		intBuf[2] = (byte)(i >> 8);
		intBuf[3] = (byte)i;
		os.write(intBuf);
	}

	public static int readInt(InputStream is) throws IOException {
		byte[] intBuf = new byte[4];
		if (is.read(intBuf) != 4)
			throw new EOFException("Stream ended prematurely.");
		int i = (intBuf[0] & 0xFF) << 24;
		i |= (intBuf[1] & 0xFF) << 16;
		i |= (intBuf[2] & 0xFF) << 8;
		i |= intBuf[3] & 0xFF;
		return i;
	}

	/**
	 * Writes an array to the OutputStream that can be read by a
	 * corresponding call to {@link #readArray}.
	 */
	public static void writeArray(byte[] arr, OutputStream os) throws IOException {
		writeInt(arr.length, os);
		os.write(arr);
	}

	/**
	 * Reads an array from the InputStream that has been written by a
	 * corresponding call to {@link #writeArray}.
	 */
	public static byte[] readArray(InputStream is) throws IOException {
		byte[] b = new byte[readInt(is)];
		if (is.read(b) != b.length)
			throw new EOFException("Stream ended prematurely.");
		return b;
	}

	public static void writeUUID(UUID uuid, OutputStream is) throws IOException {
		DataOutputStream dos = new DataOutputStream(is);
		dos.writeLong(uuid.getMostSignificantBits());
		dos.writeLong(uuid.getLeastSignificantBits());
	}

	public static UUID readUUID(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		return new UUID(dis.readLong(), dis.readLong());
	}

	public static byte[] readBinaryFile(String filename)
			throws IOException {
		Path path = Paths.get(filename);
		return Files.readAllBytes(path);
	}

	public static void writeBinaryFile(String filename, byte[] bytes)
			throws IOException {
		Path path = Paths.get(filename);
		Files.write(path, bytes);
	}

	public static String longToAlphaNumString(long l) {
		BigInteger bi = BigInteger.valueOf(l);
		if (l < 0) bi = bi.add(BI_2_64);
		return bi.toString(36);
	}

	public static long longFromAlphaNumString(String s) {
		BigInteger bi = new BigInteger(s, 36);
		if (bi.compareTo(BI_2_64) >= 0) {
			bi = bi.subtract(BI_2_64);
		}
		return bi.longValue();
	}
}
