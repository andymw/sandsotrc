package sotrc.common;

import java.io.*;
import java.util.UUID;

import common.*;

public class ChatMessage implements Persistable {

	public UUID chatRoomID;
	public UUID messageID;
	public String senderHostname;
	public String[] destinationHostname; // Potentially a list of recipients
	public String messageContents;

	//TODO message status - delivered / failed etc

	public ChatMessage(UUID chatRoomID, UUID messageID, String senderHostname, String[] destinationHostname,String messageContents) {
		this.chatRoomID = chatRoomID;
		this.messageID = messageID;
		this.senderHostname = senderHostname;
		this.destinationHostname = destinationHostname; //May need to use arraycopy
		this.messageContents = messageContents;
	}

	public static String arrayToString(String[] inputArray) {
		
		StringBuilder stringBuilder = new StringBuilder();
		for (String iteratorString : inputArray) {
			stringBuilder.append(iteratorString);
			stringBuilder.append(",");
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return stringBuilder.toString();
	}

	public void persist(OutputStream os) throws IOException {
		DataOutput output = new DataOutputStream(os);
		output.writeUTF(chatRoomID.toString());
		output.writeUTF(messageID.toString());
		output.writeUTF(senderHostname);
		output.writeUTF(arrayToString(destinationHostname));
		output.writeUTF(messageContents);
	}

	public void reconstruct(InputStream is) throws IOException {
		DataInput input = new DataInputStream(is);
		chatRoomID = UUID.fromString(input.readUTF());
		messageID = UUID.fromString(input.readUTF());
		senderHostname = input.readUTF();
		destinationHostname = input.readUTF().split(",");
		messageContents = input.readUTF();
	}

}
