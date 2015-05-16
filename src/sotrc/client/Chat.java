package sotrc.client;

import java.util.*;

import sotrc.common.*;
import common.*;

public class Chat {
	public final UUID uuid;
	public final SessionKeys keys;
	public final Map<String, Tuple<Long, FingerprintState>> keyFingerprints;

	public Chat(final UUID uuid, final SessionKeys keys) {
		this.uuid = uuid;
		this.keys = keys;
		this.keyFingerprints = new HashMap<String, Tuple<Long, FingerprintState>>();
	}

	public void addParticipant(String username, Tuple<Long, FingerprintState> keyFingerprint) {
		keyFingerprints.put(username, keyFingerprint);
	}

	public boolean containsParticipant(String username) {
		return keyFingerprints.containsKey(username);
	}

	public Tuple<Long, FingerprintState> getUserFingerprintState(String username) {
		return keyFingerprints.get(username);
	}

	public UUID getUUID() {
		return uuid;
	}
}
