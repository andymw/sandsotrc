package sotrc.server;

import java.io.*;
import java.io.IOException;
import java.net.*;

import sotrc.common.ChatMessage;

public class PushHandler {

	public DataOutputStream pushStream;
	public DataInputStream responseStream;

	public PushHandler(Socket s) {
		try{ 
			this.pushStream = new DataOutputStream(s.getOutputStream());
			this.responseStream = new DataInputStream(s.getInputStream());
		} catch(IOException e) {
			System.err.println("Server isn't connecting");
		}
	}

	/*
	* Push Handlers : Send ACTION, read response
	*/

	public boolean pushInvitation(String username) {
		
		try {
			String invitationMessage = "User " + username + " would like to chat with you";
			// pushStream.writeUTF(serverAction.invitationAction(invitationMessage));
			// TODO reading strings in now need to convert to codes
			if (responseStream.readUTF().equals("OK")) {
				return true;
			}
		} catch (IOException e) {
			System.err.println("Client probably disconnected");
			return false;
		}
		return false;
	}

	public boolean pushDisconnect(String username) {

		try {
			String disconnectMessage = "User " + username + " has disconnected from the chat";
			// pushStream.writeUTF(serverAction.disconnectAction(disconnectMessage));
			if (responseStream.readUTF().equals("OK")) {
				return true;
			}
		} catch (IOException e) {
			System.err.println("Client probably disconnected");
			return false;
		}
		return false;
	}

	public boolean pushMessage(ChatMessage message) {

		try {
			//TODO create an dataoutputstream to send objects
			// pushStream.write(message);
			if (responseStream.readUTF().equals("OK")) {
				return true;
			}
		} catch (IOException e) {
			System.err.println("Client probably disconnected");
			return false;
		}
		return false;
	}

}
