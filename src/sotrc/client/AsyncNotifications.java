package sotrc.client;

public class AsyncNotifications {

	public String username;
	public String message;
	/**
	 * 0 = start chat, 1 = message, 2 = user left, 3 = chat end
	 */
	public byte type;
	public Chat chat;

	public AsyncNotifications(String username, String message, byte type, Chat chat) {
		this.username = username;
		this.message = message;
		this.type = type;
		this.chat = chat;
	}
}
