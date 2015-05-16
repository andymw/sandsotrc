package sotrc.common;

import java.io.*;
import java.security.*;
import common.*;
import common.crypto.*;

/**
 * Implements an RSA-based zero-knowledge challenge-response protocol over a network.
 */
public class NewChallengeResponse {

	// do not need digesting, challenges are small
	private static final String RSA_SPEC = "NONEwithRSA";

	public static class Response implements Persistable {
		private byte[] response;
		
		private Response(byte[] response) {
			this.response = response;
		}

		public static Response newForVerifying() {
			return new Response(null);
		}

		public void persist(OutputStream out) throws IOException {
			Utils.writeArray(response, out);
		}

		public void reconstruct(InputStream in) throws IOException {
			response = Utils.readArray(in);
		}

		public boolean verify(Challenge challenge, PublicKey key) {
			try {
				Signature sig = Signature.getInstance(RSA_SPEC);
				sig.initVerify(key);
				sig.update(challenge.challenge);
				return sig.verify(response);
			} catch (GeneralSecurityException e) { return false; }
		}
	}

	public static class Challenge implements Persistable {
		private byte[] challenge;
		
		private Challenge(byte[] challenge) {
			this.challenge = challenge;
		}

		public static Challenge newChallenge() {
			return new Challenge(CryptoUtils.getRandomArray());
		}

		public static Challenge newForResponding() {
			return new Challenge(null);
		}

		public Response respond(PrivateKey key) {
			byte[] response = null;
			try {
				Signature sig = Signature.getInstance(RSA_SPEC);
				sig.initSign(key);
				sig.update(challenge);
				response = sig.sign();
			} catch (NoSuchAlgorithmException impossible) {
			} catch (GeneralSecurityException e) {
				response = new byte[0];
			}
			return new Response(response);
		}

		public void persist(OutputStream out) throws IOException {
			Utils.writeArray(challenge, out);
		}

		public void reconstruct(InputStream in) throws IOException {
			challenge = Utils.readArray(in);
		}
	}

}
