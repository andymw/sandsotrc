package sotrc.common;

import java.io.*;
import common.*;

/**
 * Actions sent client -> server
 */
public class ClientAction implements Persistable {
	private ActionType actionType;
	private String detail; // username reported or blocked, or chat id?

	public static enum ActionType {
		LOGIN, LOGOUT, USER_CREATE, USER_DELETE, CHANGE_PASSWORD, DISCONNECT,
		START_CHAT, LEAVE_CHAT, SEND_MESSAGE, REPORT_USER,
		ADD_CONTACT, REMOVE_CONTACT, GET_CONTACTS,
		ADMIN_LOGIN, BLOCK_USER, UNBLOCK_USER, GET_REPORTS
	}

	public static String stringOfActionType(ActionType type) {
		String s;
		switch (type) {
		case LOGIN:
			s = "User login"; break;
		case LOGOUT:
			s = "User logout"; break;
		case USER_CREATE:
			s = "User account creation"; break;
		case USER_DELETE:
			s = "User account deletion"; break;
		case CHANGE_PASSWORD:
			s = "User password change"; break;
		case REPORT_USER:
			s = "User reported user"; break;
		case START_CHAT:
			s = "User started chat"; break;
		case LEAVE_CHAT:
			s = "User left chat"; break;
		case ADD_CONTACT:
			s = "User added contact"; break;
		case REMOVE_CONTACT:
			s = "User removed contact"; break;
		case SEND_MESSAGE:
			s = "User send message"; break;
		case ADMIN_LOGIN:
			s = "Administrator login"; break;
		case BLOCK_USER:
			s = "Administrator blocked user"; break;
		case UNBLOCK_USER:
			s = "Administrator unblocked user"; break;
		case GET_REPORTS:
			s = "Administrator get reports"; break;
		case DISCONNECT:
			s = "User disconnect"; break;
		default:
			s = "!!Unrecognized user action!!"; break;
		}
		return s;
	}

	/** Construct a ClientAction to use for reconstruction. */
	public ClientAction() {
		this.actionType = ActionType.DISCONNECT;
		this.detail = "";
	}

	/**
	 * We might need a better constructor. -- amw275
	 */
	public ClientAction(ActionType type, String detail) {
		this.actionType = type;
		this.detail = detail;
	}

	public ActionType getType() {
		return actionType;
	}

	public String getDetail() {
		return detail;
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(actionType.toString());
		dos.writeUTF(detail);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		actionType = ActionType.valueOf(dis.readUTF());
		detail = dis.readUTF();
	}

	public String toString() {
		return stringOfActionType(this.actionType)
			+ (detail == null ? " [null detail]" : " " + detail);
	}

}
