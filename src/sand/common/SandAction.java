package sand.common;

import java.io.*;
import java.net.*;
import common.*;

public class SandAction implements Persistable {
	public static enum ActionType {
		LOGIN, LOGOUT, USER_CREATE, USER_DELETE, USERNAME_CHANGE,
		USERPASSWORD_CHANGE, TOGGLE_TWOFACTOR, ADMIN_LOGIN,
		ADDCREDENTIAL, REMOVECREDENTIAL,
		SYNC, DISCONNECT, GET_LOGS, BLOCK_USER, UNBLOCK_USER,
	};
	private ActionType actionType;
	private String detail;
	private String osString;

	public SandAction() { }

	public SandAction(ActionType type, String detail) {
		this.actionType = type;
		this.detail = detail == null ? "" : detail;
		this.osString = System.getProperty("os.name") + " "
			+ System.getProperty("os.version") + " " + System.getProperty("os.arch");
	}

	@Override
	public void persist(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(actionType.toString());
		dos.writeUTF(detail);
		dos.writeUTF(osString);
	}

	@Override
	public void reconstruct(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		try {
			actionType = ActionType.valueOf(dis.readUTF());
			detail = dis.readUTF();
			osString = dis.readUTF();
		} catch (IllegalArgumentException | UTFDataFormatException e) {
			throw new ProtocolException("Malformed serialized Action object.");
		}
	}

	public String getDetail() {
		return detail;
	}

	public ActionType getType() {
		return actionType;
	}

	public static String stringOfActionType(ActionType type) {
		String s;
		switch (type) {
		case ADMIN_LOGIN:
			s = "Admin login"; break;
		case BLOCK_USER:
			s = "Admin blocked user"; break;
		case UNBLOCK_USER:
			s = "Admin unblocked user"; break;
		case LOGIN:
			s = "User login"; break;
		case LOGOUT:
			s = "User logout"; break;
		case USER_CREATE:
			s = "User account creation"; break;
		case USER_DELETE:
			s = "User account deletion"; break;
		case USERNAME_CHANGE:
			s = "Username change"; break;
		case USERPASSWORD_CHANGE:
			s = "User password change"; break;
		case ADDCREDENTIAL:
			s = "Add credential"; break;
		case REMOVECREDENTIAL:
			s = "Remove credential"; break;
		case GET_LOGS:
			s = "User requested logs"; break;
		case SYNC:
			s = "User sync"; break;
		case DISCONNECT:
			s = "User disconnect"; break;
		default:
			s = "!!Unrecognized user action!!"; break;
		}
		return s;
	}

	public String toString() {
		return SandAction.stringOfActionType(this.actionType) + " "
			+ (detail == null ? "" : detail) + " (" + this.osString + ")";
	}

}
