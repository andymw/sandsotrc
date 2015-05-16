package sand.client;

import java.io.*;
import java.util.*;

import common.network.*;

public class SandAdminCLI {
	private static SandClientManager manager;

	public static void main(final String[] args) {
		if (!checkArgs(args))
			System.exit(0);

		int port = SslDefaults.DEFAULT_SAND_PORT;
		if (args.length > 1) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Using port " + SslDefaults.DEFAULT_SAND_PORT + ".");
				port = SslDefaults.DEFAULT_SAND_PORT;
			}
		}

		printWelcome();
		manager = new SandClientManager(args[0], port);
		if (!processLogin()) {
			System.out.println("Exiting ...");
			System.exit(0);
		}

		printMenuHelp();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		for (String[] operationAndArgs = null;;) {
			printCommandSymbol("sand");
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
			case "logs": // view all logs
				if (cmdline.length < 2) {
					invalidCommand();
					break;
				}
				try { viewLogsRoutine(Integer.parseInt(cmdline[1])); }
				catch (NumberFormatException e) {
					System.out.println("Unable to parse number.");
				}
				break;
			case "creates":
				System.out.println("Unimplemented");
				break;
			case "logins":
				System.out.println("Unimplemented");
				break;
			case "changes":
				System.out.println("Unimplemented");
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
			case "blocklist":
				System.out.println("Unimplemented");
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

	private static void viewLogsRoutine(int entries) {
		/*
		LogsGUI gui = new LogsGUI();
		gui.setTitle("S.A.N.D.");
		gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		gui.pack();
		gui.setVisible(true);
		*/

		if (entries < 1)
			System.out.println("No logs retrieved");
		List<String> logs = manager.adminGetUserLogs(entries);
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
			= " ________      _____     ______        ___  ________\n"
			+ "/        \\    /     \\    |     \\      |   |         \\\n"
			+ "|   ___   |  /       \\   |      \\     |   |   ____   \\\n"
			+ "|  |   \\__| /    _    \\  |   |\\  \\    |   |  |    \\   |\n"
			+ "|  \\_____  /    / \\    \\ |   | \\  \\   |   |  |     |  |\n"
			+ "\\        \\/    /   \\    \\|   |  \\  \\  |   |  |     |  |\n"
			+ " \\____    |   /_____\\    \\   |   \\  \\ |   |  |     |  |\n"
			+ "__    \\   |               \\  |    \\  \\|   |  |     /  |\n"
			+ "| \\___|   | ___________    \\ |     \\      |  |____/   /\n"
			+ "|         //           \\    \\|      \\     |          /\n"
			+ "\\________/o             \\____o       \\____o_________/o\n"
			+ "                         ADMIN\n";
		System.out.println(welcomeString);
	}

	private static void printMenuHelp() {
		System.out.println(" Options:\n"
			/*
			+ "   'creates'\t\tview account creations,\n"
			+ "   'logins'\t\tview login and logout logs\n"
			+ "   'changes'\t\tview master password changes (for multiple users)\n"
			*/
			+ "   'logs <<number of recent entries>>'\t\tview recent activity\n"
			+ "   'block <<username>>'\tblock user account\n"
			+ "   'unblock <<username>>' unblock user account\n"
			+ "   'exit'\t\tlogout and exit.");
	}

	private static void printCommandSymbol(String modifier) {
		System.out.print(modifier + "# ");
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
			System.out.println(" Usage: java sand.client.SandAdminCLI <host> [<port>]");
			return false;
		}
		return true;
	}

}
