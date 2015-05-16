package common.crypto;

import java.io.*;
import java.security.*;
import common.*;

/**
 * Implements an RSA-based zero-knowledge challenge-response protocol over a network.
 */
public class ChallengeResponse {

	// do not need digesting, challenges are small
	private static final String RSA_SPEC = "NONEwithRSA";

	private final byte[] challenge;
	private final boolean mode;
	private byte[] response;

	private ChallengeResponse(byte[] challenge, boolean mode) {
		this.challenge = challenge;
		this.mode = mode;
	}

	/**
	 * Creates a new ChallengeResponse in challenge mode, with a one-time challenge.
	 */
	public static ChallengeResponse newChallenge() {
		return new ChallengeResponse(CryptoUtils.getRandomArray(), false);
	}

	/**
	 * Creates a new ChallengeResponse in response mode, with challenge read from stream.
	 */
	public static ChallengeResponse readChallenge(InputStream is) throws IOException {
		return new ChallengeResponse(Utils.readArray(is), true);
	}

	/**
	 * In challenge mode, sends the challenge; in response mode, sends the response.
	 */
	public ChallengeResponse send(OutputStream os) throws IOException {
		if (mode) { // responder
			if (response == null)
				throw new IllegalStateException("You have not responded.");
			Utils.writeArray(response, os);
		} else { // challenger
			Utils.writeArray(challenge, os);
		}
		return this;
	}

	/**
	 * Responds to a challenge with the given private key. (Only valid in response mode.)
	 */
	public ChallengeResponse respond(PrivateKey key) {
		if (!mode) throw new IllegalStateException("You are the challenger.");
		try {
			Signature sig = Signature.getInstance(RSA_SPEC);
			sig.initSign(key);
			sig.update(challenge);
			response = sig.sign();
		} catch (NoSuchAlgorithmException impossible) {
		} catch (InvalidKeyException e) {
			response = new byte[0];
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		
		return this;
	}

	/**
	 * Reads a response from the given input stream. (Only valid in challenge mode.)
	 */
	public ChallengeResponse readResponse(InputStream is) throws IOException {
		if (mode) throw new IllegalStateException("You are the responder.");
		response = Utils.readArray(is);
		return this;
	}

	/**
	 * Verifies that the received response is valid.
	 * Must have called {@link #readResponse} first. (Only valid in challenge mode.)
	 */
	public boolean verify(PublicKey key) {
		if (challenge == null)
			throw new IllegalStateException("Challenge has not been responded to.");
		if (mode) throw new IllegalStateException("You are the responder.");
		try {
			Signature sig = Signature.getInstance(RSA_SPEC);
			sig.initVerify(key);
			sig.update(challenge);
			return sig.verify(response);
		} catch (GeneralSecurityException e) { return false; }
	}

}
