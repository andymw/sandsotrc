package sotrc.common;

import java.io.*;
import java.util.*;
import common.*;

/**
 * Notifications of SOTRC events pushed from server to client.
 */
public class Notification implements Persistable {
	public Type type;
	private UUID uuid;
	public String detail; // username reported or blocked, or chat id?

	public enum Type {
		INVITATION, // (username)
		MESSAGE, //(ChatUUID)
		CHAT_START, //(ChatUUID, otherUser)
		CHAT_END, //(ChatUUID)
		USER_LEFT // (ChatUUID, leftUser)
	}

	private Notification(Type type, UUID uuid, String detail) {
		this.type = type;
		this.uuid = uuid;
		this.detail = detail;
	}

	public Notification() { }

/////// Factory "constructors" /////////////////////////////////////////////////
	public static Notification Message(UUID chatUUID, String username) {
		return new Notification(Type.MESSAGE, chatUUID, username);
	}

	public static Notification ChatStart(UUID chatUUID, String otherParticipiant) {
		return new Notification(Type.CHAT_START, chatUUID, otherParticipiant);
	}

	public static Notification ChatEnd(UUID chatUUID) {
		return new Notification(Type.CHAT_END, chatUUID, null);
	}

	public static Notification UserLeft(UUID chatUUID, String leftUser) {
		return new Notification(Type.USER_LEFT, chatUUID, leftUser);
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(type.toString());
		switch (type) {
		case INVITATION: dos.writeUTF(detail); break;
		case CHAT_START: case USER_LEFT: case MESSAGE:
			dos.writeUTF(detail); // no break. fall down to write UUID
		case CHAT_END:
			Utils.writeUUID(uuid, dos);
		}
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		type = Type.valueOf(dis.readUTF());
		switch (type) {
		case INVITATION: detail = dis.readUTF(); break;
		case CHAT_START: case USER_LEFT: case MESSAGE:
			detail = dis.readUTF(); // no break. fall down to read UUID
		case CHAT_END:
			uuid = Utils.readUUID(dis);
		}
	}

	public UUID getUUID() { return uuid; }
	public String getDetail() { return detail; }

	@Override
	public String toString() {
		return type.toString() + ": "
			+ (uuid == null ? "[null]" : uuid.toString()) + ", "
			+ (detail == null ? "[null]" : detail);
	}

}
