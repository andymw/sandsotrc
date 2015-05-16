package sand.client;

import java.io.*;
import java.util.*;

import common.*;
import common.crypto.*;
import common.network.*;

public class SandClientCLI {

	private static SandClientManager manager;
	private static BufferedReader br = new BufferedReader(
		new InputStreamReader(System.in));

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
		printFirstMenuHelp(manager.isConnectedToServer());

		for (String[] operationAndArgs = null;;) {
			printCommandSymbol("sand");
			try {
				operationAndArgs = br.readLine().split(" ");
				if ("exit".equals(operationAndArgs[0]))
					break;
				processFirstOperation(operationAndArgs);
			} catch (IOException e) {
				System.err.println(e.toString());
				close();
				return;
			}
		}

		System.out.println("Exiting ...");
		close();
	}

	// true if continue, false to signal break out
	private static void processFirstOperation(String[] cmdline) {
		char[] pass;
		switch (cmdline[0]) {
			// no need to handle "exit". we exit before we process it.
			case "":
				break;
			case "create": // create (this needs to enter user/pass generation subroutines)
				if (!manager.isConnectedToServer()) {
					System.out.println("  Unauthorized, not connected to server.");
					break;
				}
				if (cmdline.length < 2) {
					invalidCommand();
					break;
				}
				if (cmdline[1] == null || cmdline[1].length() == 0 || cmdline[1].contains(" ")) {
					System.out.println("Username should be at least length 1 and must not have spaces.");
					System.out.println("Exiting ...");
					break;
				}
				printUserPassRequirements("A password");
				pass = promptValidPassword();
				boolean enable = setUpTwoFactorRoutine();
				createRoutine(cmdline[1], pass, enable);
				break;
			case "login": // login <username>
				if (!manager.isConnectedToServer()) {
					System.out.println("Unauthorized, not connected to server.");
					break;
				}
				if (cmdline.length < 2) {
					invalidCommand();
					break;
				}
				pass = passwordPrompt(null, false); // no need to reprompt
				loginRoutine(cmdline[1], pass);
				break;
			case "offline": // offline cached mode. still needs
				if (cmdline.length < 2) {
					invalidCommand();
					break;
				}
				pass = passwordPrompt(null, false); // no need to reprompt
				loginOfflineRoutine(cmdline[1], pass);
				break;
			case "help":
				printFirstMenuHelp(manager.isConnectedToServer());
				break;
			default: // Unrecognized command
				invalidCommand();
				break;
		}
	}

	private static char[] promptValidPassword() {
		System.out.println("Let SAND autogenerate strong password suggestions?");
		System.out.print(">>");

		String strong = "y"; //default
		try {
			strong = br.readLine();
		} catch (IOException e) {
			System.err.println(e.toString());
			strong = "y";
		}

		if (strong != null && strong.length() != 0
				&& strong.toLowerCase().charAt(0) == 'y') {
			generatePasswordSuggestions();
		}

		char[] pass = passwordPrompt(null, true); // this checks for passwd criteria
		return pass;
	}

	private static void generatePasswordSuggestions() {
		System.out.print("Specify desired password length. (8+):");
		System.out.print(">>");

		int passLength = 12;
		try {
			passLength = Integer.parseInt(br.readLine());
		} catch (NumberFormatException | IOException e) {
			System.out.println("Unable to parse input. Defaulting to length 12.");
			passLength = 12;
		}

		System.out.println("Enter 'y' to keep generating, any other key to quit.");
		for(;;) {
			String strongPass = LoginUtilities.getRandomSecurePassword(passLength);
			System.out.println(strongPass);
			System.out.print(">>");
			String accept = "y"; // default

			try {
				accept = br.readLine();
				if (accept == null || accept.length() == 0 || accept.charAt(0) != 'y')
					return;
			} catch (IOException e) {
				return;
			}
		}
	}

	private static char[] passwordPrompt(String modifier, boolean reprompt) {
		for (;;) {
			System.out.print("Enter " + (modifier == null ? "" : (modifier + " "))
				+ "password: ");
			char[] password = System.console().readPassword();
			if (reprompt) { // reprompt also automatically turns on passwordChecker
				if (!LoginUtilities.guidelineChecker(String.valueOf(password))) {
					System.out.println("Password does not match criteria.");
					printUserPassRequirements("Password");
					continue; // try again from beginning
				}

				System.out.print("Re-enter password: ");
				char[] password2 = System.console().readPassword();
				if (Arrays.equals(password, password2)) {
					CryptoUtils.zeroPassword(password2);
					return password;
				} else {
					System.out.println("Passwords did not match.");
				}
			} else return password;
		}
		// LoginUtilities.getRandomSecurePassword(length)
	}

	private static void createRoutine(String username, char[] password, boolean enableTFA) {
		System.out.println("Creating account " + username + " ...");
		// TODO if local cached, manager needs to check if account is also not present first to prevent overwrite?
		String twoFactorToken = manager.createAccount(username, password, enableTFA);
		if (twoFactorToken != null) {
			System.out.println("Account creation successful!");
			if (enableTFA) {
				System.out.println("Please visit the following URL in a browser and scan the QR Code with Google Authenticator.");
				System.out.println(twoFactorToken);
			}
		} else {
			System.out.println("Account creation failed!");
		}
	}

	private static void loginRoutine(String username, char[] password)  {
		if (!manager.login(username, password)) {
			System.out.println("Login failed");
			return; // TODO any cleanup?
		}

		try {
			if (manager.needsTwoFactorAuthentication()) {
				System.out.print("Please enter generated two-factor authentication code: ");
				Long code = Long.parseLong(br.readLine());
				if(!manager.twoFactorAuthenticate(code)) { // will log out if unsuccessful
					System.out.println("Login failed.");
					return;
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.out.println("Login Failed. Try logging in again");
			try {
				manager.logout();
			} catch (IOException ioe) {}
			return;
		}

		System.out.println("Logging in...");

		if (manager.checkForAbnormalLogin()) {
			System.out.println("INFORMATION: You are logging in from an unusual new location.");
		}

		System.out.println("Login successful!");
		printLoginMenuHelp();

		for (String[] operationAndArgs = null;;) {
			printCommandSymbol("sand-" + manager.getCurrentUsername());
			try {
				operationAndArgs = br.readLine().split(" ");
				boolean continuePrompt = processLoginOperation(operationAndArgs);
				if (!continuePrompt) return; // most likely logout and delete-account
			} catch (IOException e) {
				System.err.println(e.toString());
				logout();
				return;
			}
		}
	}

	// return a flag that indicates whether to continue the prompt.
	private static boolean processLoginOperation(String[] cmdline) {
		switch (cmdline[0]) {
			case "lookup": // lookup <site/host> [<username>]
			case "lookupall":
			case "lookupid":
				processLookupOperation(cmdline);
				return true;
			case "":
				return true;
			case "add": // add <site/host> <username> <password> [<description>]
				if (cmdline.length < 4) {
					invalidCommand();
					return true;
				}
				addCredentialRoutine(cmdline[1], cmdline[2], cmdline[3],
					cmdline.length < 5 ? "" : cmdline[4] /*desc. optional*/);
				return true;
			case "remove": // remove <site/host> <username> -f
				if (cmdline.length < 3) {
					invalidCommand();
					return true;
				}
				removeCredentialRoutine(cmdline[1], cmdline[2],
					cmdline.length > 3);
				return true;
			case "removeall":
				removeCredentialRoutine(null, null, true);
				return true;
			case "removeid":
				if (cmdline.length < 2) {
					invalidCommand();
					return true;
				}
				removeSpecificCredentialRoutine(cmdline[1]);
				return true;
			case "edit":
				if (cmdline.length < 2) {
					invalidCommand();
					return true;
				}
				editCredentialRoutine(cmdline[1]);
				return true;
			case "change-username":
				changeUsernameRoutine();
				return true;
			case "change-password":
				changePasswordRoutine();
				return true;
			case "twofactor-toggle":
				toggleTwoFactorRoutine();
				return true;
			case "view-logs":
				if (cmdline.length < 2) {
					invalidCommand();
					return true;
				}
				try { viewLogsRoutine(Integer.parseInt(cmdline[1])); }
				catch (NumberFormatException e) {
					System.out.println("Unable to parse number.");
				}
				return true;
			case "clear-clientcache":
				clearClientCacheRoutine();
				return true;
			case "sync":
				syncRoutine();
				return true;
			case "delete-account":
				return !deleteAccountRoutine();
			case "help":
				printLoginMenuHelp();
				return true;
			case "help-full":
			case "helpfull":
				printLoginMenuHelpFull();
				return true;
			case "logout":
				logout();
				return false;
			default:
				invalidCommand();
				return true;
		}
	}

	private static void processLookupOperation(String[] cmdline) {
		switch (cmdline[0]) {
			case "lookup": // lookup <site/host> [<username>]
				// TODO add display descriptions option
				if (cmdline.length < 2) { invalidCommand(); break; }
				lookupCredentialRoutine(cmdline[1],
					cmdline.length < 3 ? "" : cmdline[2]);
				break;
			case "lookupid":
				if (cmdline.length < 2) { invalidCommand(); break; }
				lookupByUuid(cmdline[1]);
				break;
			case "lookupall":
				lookupAllCredentialsRoutine();
				break;
			default:
				invalidCommand();
				printLoginMenuHelp();
				break;
		}
	}

	/** All parameters must be non-NULL. */
	private static void addCredentialRoutine(String site, String user, String pass,
			String description) {
		System.out.println("Adding credential...");
		UUID uuid = manager.addCredential(site, user, pass, description);
		if (uuid == null) {
			System.out.println("Add credential FAILED!");
		} else {
			System.out.println("Credential added.");//was added with ID " + uuid.toString());
		}
	}

	/** username may be empty string. if so, username is not used in filter */
	private static void lookupCredentialRoutine(String site, String username) {
		List<CredentialTuple> creds = manager.lookupMatching(site, username);
		if (creds == null) {
			System.out.println("Credentials of hostname " + site + " not found");
			return;
		}
		for (CredentialTuple cred : creds) {
			System.out.println(cred);
		}
	}

	private static void lookupByUuid(String lastSixUuidDigits) {
		CredentialTuple tup = manager.lookupSpecific(lastSixUuidDigits);
		if (tup == null) {
			System.out.println("Credential not found.");
			return;
		}

		System.out.println(tup);
	}

	private static void lookupAllCredentialsRoutine() {
		List<CredentialTuple> creds = manager.lookupAll();
		if (creds == null) {
			System.out.println("No credential found.");
			return;
		}
		for (CredentialTuple cred : creds) {
			System.out.println(cred);
		}
	}

	// if forced, remove all credentials matching site/user
	private static void removeCredentialRoutine(String site, String user, boolean forced) {
		boolean success = manager.removeCredential(site, user, forced);
		if (!success) {
			// assuming logged in.
			System.out.println("Remove credential failed. No matching credentials," +
				"or too many matching.\nSuggestion: perform a lookup?");
		} else {
			System.out.println("Removal of credential(s) matching "
				+ (site == null ? "all" : user == null ? (site) : (site + "," + user))
				+ " succeeded.");
		}
	}

	private static void removeSpecificCredentialRoutine(final String lastSixUuidDigits) {
		CredentialTuple tup = manager.lookupSpecific(lastSixUuidDigits);
		if (tup == null) {
			System.out.println("Credential not found.");
			return;
		}

		if (manager.removeCredential(tup.getUUID())) {
			System.out.println("Removal of credential " + lastSixUuidDigits + " succeeded.");
		} else {
			System.out.println("Unable to remove credential " + lastSixUuidDigits);
		}
	}

	private static void editCredentialRoutine(final String lastSixUuidDigits) {
		CredentialTuple tup = manager.lookupSpecific(lastSixUuidDigits);
		if (tup == null) {
			System.out.println("Credential not found.");
			return;
		}

		System.out.println("\n  Credential: " + tup.toString());

		for (;;) {
			System.out.println("\nSpecify new values for the fields. (Enter to keep same)");
			System.out.print("Hostname: ");
			String newHostname = readString();
			tup.host = newHostname == null || newHostname.length() == 0
				? tup.host : newHostname;
			System.out.print("Username: ");
			String newUsername = readString();
			tup.username = newUsername == null || newUsername.length() == 0
				? tup.username : newUsername;
			System.out.print("Password: ");
			String newPassword = readString();
			tup.password = newPassword == null || newPassword.length() == 0
				? tup.password : newPassword;
			System.out.print("Description: ");
			String newDescription = readString();
			tup.description = newDescription == null || newDescription.length() == 0
				? tup.description : newDescription;

			System.out.println("\n  Credential: " + tup.toString());
			System.out.print("Okay to save? (y, n, or abort) ");

			char response = readChar();
			if (response == 'y' || response == 'Y') {
				if (manager.addCredential(tup) == null)
					System.out.println("Edit credential failed.");
				else System.out.println("Edit credential success.");
				return;
			}

			if (!(response == 'n' || response == 'N')) { // abort
				System.out.println("Edit of credential " + lastSixUuidDigits + " aborted.");
				return;
			}
		}
	}

	private static String readString() {
		try { return br.readLine(); }
		catch (IOException e) { return null; }
	}

	private static char readChar() {
		String str = readString();
		return str == null || str.length() == 0 ? '\0' : str.charAt(0);
	}

	private static void changeUsernameRoutine() {
		System.out.print("Enter current username: ");
		try {
			String currentUsername = br.readLine();
			if (currentUsername == null || !currentUsername.equals(manager.getCurrentUsername())) {
				System.out.println("Current username incorrect.");
				return;
			}
			System.out.print("Enter new username: ");
			String newUsername = br.readLine();
			if (newUsername == null || !manager.changeUsername(newUsername)) {
				System.out.println("Unable to change username.");
				return;
			}

			System.out.println("Username changed successfully! You are now "
				+ manager.getCurrentUsername());
		} catch (IOException e) {
			System.err.println("Unable to change username.");
			return;
		}

	}

	private static void changePasswordRoutine() {
		System.out.print("Enter current username: ");
		try {
			String currentUsername = br.readLine();
			if (currentUsername == null || !currentUsername.equals(manager.getCurrentUsername())) {
				System.out.println("Current username incorrect.");
				return;
			}
			char[] currentPass = passwordPrompt("current", false);
			char[] newPass = passwordPrompt("new", true);
			if (!manager.changePassword(currentPass, newPass)) {
				System.out.println("unable to change password.");
				return;
			}
		} catch (IOException e) {
			System.err.println("Unable to change password.");
			return;
		}
	}

	private static void toggleTwoFactorRoutine() {
		boolean isOn = manager.needsTwoFactorAuthentication();
		if (isOn) {
			System.out.println("Two factor has been on. Turning off...");
		} else {
			System.out.println("Two factor has been off. Turning on...");
			System.out.println("Please visit the following URL in a browser and scan the QR Code with Google Authenticator.");
			System.out.println(manager.getTwoFactorQRCode());
		}
		manager.toggleTwoFactor(!isOn);
	}

	private static void loginOfflineRoutine(String username, char[] pass) {
		if (!manager.loginOffline(username, pass)) {
			System.out.println("Login failed");
			return; // TODO any cleanup?
		}

		try {
			if(manager.needsTwoFactorAuthentication()) {
				System.out.print("Please enter generated two-factor authentication code: ");
				Long code = Long.parseLong(br.readLine());
				System.out.println(code);
				if(!manager.twoFactorAuthenticate(code)) {
					System.out.println("Login Failed.");
					return;
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.out.println("Login Failed. Try logging in again");
			try {
				manager.logout();
			} catch (IOException ioe) {}
			return;
		}

		System.out.println("Logging in offline...");

		System.out.println("Offline authentication successful.");
		printLoginOfflineMenuHelp();

		for (String[] operationAndArgs = null;;) {
			printCommandSymbol("sand-" + username);
			try {
				operationAndArgs = br.readLine().split(" ");
				if ("logout".equals(operationAndArgs[0])) {
					break;
				}
				processLookupOperation(operationAndArgs);
			} catch (IOException e) {
				System.err.println(e.toString());
				return;
			}
		}
		// no logout ops necessary.
	}

	private static void viewLogsRoutine(int entries) {
		if (entries < 1)
			System.out.println("No logs retrieved");
		List<String> logs = manager.getUserLogs(entries);

		if (logs == null) {
			System.out.println("Logs not found.");
			return;
		}

		for (String log : logs) {
			System.out.println(log);
		}
	}

	private static void clearClientCacheRoutine() {
		System.out.println(
			  "Clearing your local cache will not delete your credentials on\n"
			+ " SAND. After clearing, to retrieve your data, type 'sync' to\n"
			+ " re-retrieve your data. Note. this is considered a dangerous\n"
			+ " operation. Do this only if you are absolutely sure."
		);
		System.out.println("Are you sure you want to clear the cache of your account? (y or n)");
		char response = '\0';
		try {
			response = br.readLine().toLowerCase().charAt(0);
			if (response != 'y') {
				System.out.println("Operation aborted.");
				return;
			}
			if (manager.clearCache()) {
				System.out.println("Success.");
			} else {
				System.out.println("Cache clearing failed?");
			}
		} catch (StringIndexOutOfBoundsException | IOException e) {
			System.out.println("Operation aborted.");
			return;
		}
	}

	private static boolean setUpTwoFactorRoutine() {
		System.out.print("Enable two-factor authentication? (y or n) ");
		String response = null;
		try {
			response = br.readLine();
		} catch (IOException e) { }

		return response != null && response.length() > 0 && response.charAt(0) == 'y';
	}

	private static void syncRoutine() {
		if (manager.sync())
			System.out.println("Success.");
		else System.out.println("Man, we just failed.");
	}

	// returns success.
	private static boolean deleteAccountRoutine() {
		System.out.print("Are you sure you want to delete your account? All your data will be deleted locally. ");
		char response = '\0';
		try {
			response = br.readLine().toLowerCase().charAt(0);
		} catch (StringIndexOutOfBoundsException | IOException e) {
			System.out.println("Account deletion aborted.");
			return false;
		}

		if (response != 'y') {
			System.out.println("Account deletion aborted.");
			return false;
		}

		if (!manager.deleteAccount()) {
			System.out.println("Delete account failed.");
			return false; // means to continue
		}
		System.out.println("Delete account success.");
		return true; // do not continue
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
			+ "    P  A  S  S  W  O  R  D    M  A  N  A  G  E  R\n"
			+ "                     v1.02\n\n"
			+ "\nWelcome to S.A.N.D. Password Manager!\n"
			+ "Setting up ...\n";
		System.out.println(welcomeString);
	}

	private static void printFirstMenuHelp(boolean isConnected) {
		if (isConnected)
			System.out.println(" Options:\n"
				+ "   'create <username>' to create a new account,\n"
				+ "   'login <username>' to an existing account, update credentials, or modify account properties\n"
				+ "   'offline <username>' to view cached local credentials\n"
				+ "   'exit' to exit.");
		else System.out.println(" Options:\n"
				+ "   'offline <username>' to view cached local credentials\n"
				+ "   'exit' to exit.");
	}

	private static void printLoginMenuHelp() {
		System.out.println(" Options:\n"
			+ "   add credential:\tadd <<site/service>> <<username>> <<password>> [<<description>>]\n"
			+ "   lookup credential:\tlookup <<site/service>> [<<username>>]\n"
			+ "                   OR\tlookupid <<id>>\n"
			+ "                   OR\tlookupall\n"
			+ "   remove credential:\tremove <<site/service>> <<username>> [-f]\n"
			+ "                   OR\tremoveid <<id>>\n"
			+ "                   OR\tremoveall\n"
			+ "   edit credential:\tedit <<id>>\n"
			+ "   user account operations:\n"
			+ "	 \t\tchange-username\n"
			+ "	 \t\tchange-password\n"
			+ "	 \t\ttwofactor-toggle\n"
			+ "	 \t\tview-logs [number of entries]\n"
			+ "	 \t\tdelete-account\n"
			+ "   logout:\t\tlogout");
	}

	private static void printLoginMenuHelpFull() {
		System.out.println(" Options:\n"
			+ "   add credential:\tadd <<site/service>> <<username>> <<password>> [<<description>>]\n"
			+ "   lookup credential:\tlookup <<site/service>> [<<username>>]\n"
			+ "                   OR\tlookupid <<id>>\n"
			+ "                   OR\tlookupall\n"
			+ "   remove credential:\tremove <<site/service>> <<username>> [-f]\n"
			+ "                   OR\tremoveid <<id>>\n"
			+ "                   OR\tremoveall\n"
			+ "   edit credential:\tedit <<id>>\n"
			+ "   user account operations:\n"
			+ "	 \t\tchange-username\n"
			+ "	 \t\tchange-password\n"
			+ "	 \t\ttwofactor-toggle\n"
			+ "	 \t\tview-logs [number of entries]\n"
			+ "	 \t\tdelete-account\n"
			+ "	 \t\tclear-clientcache\n"
			+ "	 \t\tsync\n"
			+ "   view full help:\thelp-full\n"
			+ "   logout:\t\tlogout");
	}

	private static void printLoginOfflineMenuHelp() {
		System.out.println(" Options:\n"
			+ "   lookup credential:\tlookup <<site/service>> [<<username>>] OR lookupall\n"
			+ "   logout:\t\tlogout");
	}

	private static void printUserPassRequirements(String prefix) {
		System.out.println(prefix + ":\n  Must be at least 8 characters long\n"
			+ "  Must contain at least one alpha character\n"
			+ "  Must contain only alpha-numeric characters (0-9,a-z,A-Z)");
	}

	private static void printCommandSymbol(String modifier) {
		System.out.print(modifier + "# ");
	}

	private static void invalidCommand() {
		System.out.println(" Malformed command.");
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
		// TODO close all connections, shut down manager, null everything, exit safely
		try {
			manager.shutdown();
		} catch (InterruptedException ignore) { }
	}

	private static boolean checkArgs(final String[] args) {
		if (args.length == 0) {
			System.out.println(" Usage: java sand.client.SandClientCLI <host> [<port>]");
			return false;
		}

		return true;
	}
}
