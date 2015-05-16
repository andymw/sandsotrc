package sotrc.client;

import java.io.*;
import java.util.*;

import common.network.*;

// import javax.swing.JFrame;
// import sotrc.client.gui.LogsGUI;

public class SotrcAdminCLI {
	private static ClientManager manager;

	public static void main(final String[] args) {
		if (!checkArgs(args))
			System.exit(0);

		int serverPort = SslDefaults.DEFAULT_SOTRC_PORT;
		if (args.length > 1) {
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Using default port " + SslDefaults.DEFAULT_SOTRC_PORT);
				serverPort = SslDefaults.DEFAULT_SOTRC_PORT;
			}
		}

		printWelcome();
		manager = new ClientManager(args[0], serverPort);
		if (!processLogin()) {
			System.out.println("Exiting ...");
			System.exit(0);
		}

		printMenuHelp();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		for (String[] operationAndArgs = null;;) {
			printCommandSymbol("sotrc-admin");
			try {
				operationAndArgs = br.readLine().split(" ");
				if ("exit".equals(operationAndArgs[0]))
					break;
				processOperation(operationAndArgs);
			} catch (IOException e) {
				System.err.println(e.toString());
				logout();
				close();
				return;
			}
		}

		System.out.println("Exiting ...");
		logout();
		close();
	}

	// true if continue, false to signal break out
	private static boolean processLogin() {
		char[] pass;
		pass = passwordPrompt(false); // no need to reprompt
		return loginRoutine(pass);
	}

	private static char[] passwordPrompt(boolean reprompt) {
		System.out.print("Enter password: ");
		char[] password = System.console().readPassword();
		return password;
	}


	private static boolean loginRoutine(char[] password) {
		if (!manager.adminLogin(password)) {
			System.out.println("Login failed");
			return false; // any cleanup?
		}

		System.out.println("Login successful!");
		return true;
	}

	private static void processOperation(String[] cmdline) {
		switch (cmdline[0]) {
			case "":
				break;
			case "block":
				if (cmdline.length != 2) {
					invalidCommand();
					break;
				}
				blockRoutine(cmdline[1]);
				break;
			case "unblock":
				if (cmdline.length != 2) {
					invalidCommand();
					break;
				}
				unblockRoutine(cmdline[1]);
				break;
			case "reports": // view all reports
				if (cmdline.length < 2) {
					invalidCommand();
					break;
				}
				try { viewReportsRoutine(Integer.parseInt(cmdline[1])); }
				catch (NumberFormatException e) {
					System.out.println("Unable to parse number.");
				}
				break;
			case "help":
				printMenuHelp();
				break;
			default:
				invalidCommand();
				break;
		}
	}

	private static void unblockRoutine(String username) {
		System.out.println("Unblocking " + username);
		if (!manager.blockUser(username, false)) {
			System.out.println("Unsuccessful operation.");
			return;
		}

		System.out.println("Unblocked " + username);
	}

	private static void blockRoutine(String username) {
		System.out.println("Blocking " + username);
		if (!manager.blockUser(username, true)) {
			System.out.println("Unsuccessful operation.");
			return;
		}

		System.out.println("Blocked " + username);
	}

	private static void viewReportsRoutine(int entries) {
		if (entries < 1)
			System.out.println("No logs retrieved");
		List<String> logs = manager.adminGetUserReports(entries);
		if (logs == null) {
			System.out.println("Logs not found.");
			return;
		}

		for (String log : logs) {
			System.out.println(log);
		}
	}

	private static void printWelcome() {
		String welcomeString
		    = "   SSSSS  OOOOOOO       RRRRRR   CCCCC\n"
			+ "  S     S O     O TTTTT R     R C     C\n"
			+ "  S       O     O   T   R     R C\n"
			+ "   SSSSS  O     O   T   RRRRRR  C\n"
			+ "        S O     O   T   R   R   C\n"
			+ "  S     S O     O   T   R    R  C     C\n"
			+ "   SSSSS  OOOOOOO   T   R     R  CCCCC\n\n"
			+ "     A\n"
			+ "    A A   DDDDD  M    M  III  N    N\n"
			+ "   A   A  D    D MM  MM   I   NN   N\n"
			+ "  A     A D    D M MM M   I   N N  N\n"
			+ "  AAAAAAA D    D M    M   I   N  N N\n"
			+ "  A     A D    D M    M   I   N   NN\n"
			+ "  A     A DDDDD  M    M  III  N    N\n";
		System.out.println(welcomeString);
	}

	private static void printMenuHelp() {
		System.out.println(" Options:\n"
			+ "   'reports <<number of recent entries>>'\t\tview recent activity\n"
			+ "   'block <<username>>'\tblock user account\n"
			+ "   'unblock <<username>>' unblock user account\n"
			+ "   'exit'\t\tlogout and exit.");
	}

	private static void printCommandSymbol(String modifier) {
		System.out.print(modifier + "$ ");
	}

	private static void invalidCommand() {
		System.out.println(" Invalid command.");
	}

	/**
	 * loginRoutine calls this at the end.
	 */
	private static void logout() {
		try {
			manager.logout();
		} catch (IOException e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * first menu calls this to close connection.
	 */
	private static void close() {
		try {
			manager.disconnect();
		} catch (IOException ignore) { }
	}

	private static boolean checkArgs(final String[] args) {
		if (args.length == 0) {
			System.out.println(" Usage: java sotrc.client.SotrcAdminCLI <host> [<port>]");
			return false;
		}
		return true;
	}

}
