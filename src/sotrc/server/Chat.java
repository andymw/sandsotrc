package sotrc.server;

import java.util.*;

public class Chat {

	public UUID ChatID;
	HashSet<String> participantHashSet; //TODO Need to switch it to participant Objects later
	//arraylist of chat message objects

	public Chat(String[] participants) {
		this.ChatID = UUID.randomUUID();
		participantHashSet = new HashSet<String>();
		for (String user : participants) {
			participantHashSet.add(user);
		}
	}

	public UUID getUUID() {
		return ChatID;
	}

	//return number of active participants
	public int getActiveParticipantCount() {
		return participantHashSet.size();
	}

	public Set<String> participantSet() {
		return Collections.unmodifiableSet(participantHashSet);
	}

	//add user
	public boolean addParticipant(String participant) {
		return participantHashSet.add(participant);
	}

	//remove user - once a user quits
	public boolean removeParticipant(String participant) {
		return participantHashSet.remove(participant);
	}
}
