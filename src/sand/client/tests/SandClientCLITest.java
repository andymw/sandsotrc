package sand.client.tests;

import java.io.*;
import java.util.*;

import sand.client.*;

public class SandClientCLITest {

	private static SandClientManager manager;
	private static final String TEST_USER = "sanduser";
	private static final String TEST_PASS = "password123";
	private static final String TEST_BAD_PASS = "pass67890";

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Test assumes a clean server running on localhost.");
		manager = new SandClientManager("localhost"); // default 5430
		phaseOne();

		Thread.sleep(2000);
		phaseTwo();

		Thread.sleep(2000);
		phaseThree();

		Thread.sleep(2000);
		phaseFour();

		manager.logout();
		manager.shutdown();

		System.out.println("All tests pass.");
	}

	private static void phaseOne() throws IOException {
		System.out.println("\nPhase 1:\n  connect, login, logout, add, lookup, remove");
		manager.disconnect();
		boolean pass = true;
		pass = connectTest();
		checkPassed("connectTest", pass);

		pass = createTest(TEST_USER, TEST_PASS.toCharArray());
		checkPassed("createTest", pass);

		pass = loginAndLogoutTest(TEST_USER, TEST_PASS.toCharArray());
		checkPassed("loginAndLogoutTest", pass);

		pass = loginAddOneCredentialAndLookupTest(TEST_USER, TEST_PASS.toCharArray());
		checkPassed("loginAddOneCredentialAndLookupTest", pass);

		pass = removeGoogleGuserTest();
		checkPassed("removeGoogleGuserTest", pass);

		pass = lookupTestonMultipleCredentials();
		checkPassed("lookupTestonMultipleCredentials", pass);

		pass = removeTestonMultipleCredentials();
		checkPassed("removeTestonMultipleCredentials", pass);

		System.out.println("all tests pass. logging out and disconnecting.");
		manager.logout();
		manager.disconnect();
	}

	private static void phaseTwo() throws IOException {
		System.out.println("\nPhase 2\n  offline and account tests.");
		manager.connect("localhost");
		manager.login(TEST_USER, TEST_PASS.toCharArray());
		System.out.println("Adding 6 credentials.");
		manager.addCredential("google", "guser", "gpass", "gdesc");
		manager.addCredential("google", "guser", "gpass", "gdesc");
		manager.addCredential("google", "guser2", "gpass22", "gdesc22");
		manager.addCredential("google", "guser3", "gpass33", "gdesc33");
		manager.addCredential("facebook", "fu", "fp", "fd");
		manager.addCredential("facebook", "fu2", "fp2", "fd2");

		System.out.println("Logging out to begin offline testing.");
		manager.logout();

		boolean pass = true;
		pass = offlineReconstructAccount(TEST_USER, TEST_PASS.toCharArray());
		checkPassed("offlineReconstructAccount", pass);

		pass = offlineLookupCredentials();
		checkPassed("offlineLookupCredentials", pass);

		pass = offlineAddRemoveFail();
		checkPassed("offlineAddRemoveFail", pass);

		manager.logout(); // noop if offline
		manager.disconnect();
	}

	private static void phaseThree()  throws IOException {
		System.out.println("\nPhase 3\n  edit credential, username, pass, cacheing");
		manager.connect("localhost");
		manager.login(TEST_USER, TEST_PASS.toCharArray()); // sync not working. after add/rm fail

		boolean pass = true;
		pass = editCredential();
		checkPassed("editCredential", pass);

		pass = editUsername();
		checkPassed("editUsername", pass);

		pass = editPassword();
		checkPassed("editPassword", pass);

		pass = cacheTests();
		checkPassed("cacheTests", pass);

		manager.logout();
		manager.disconnect();
	}

	private static void phaseFour()  throws IOException {
		System.out.println("\nPhase 4\n  authorization, delete account");
		manager.connect("localhost");
		manager.login(TEST_USER, TEST_PASS.toCharArray());

		boolean pass = runBadTests();
		checkPassed("runBadTests", pass);

		manager.logout();
		manager.disconnect();
	}


	// assumes disconnected at first.
	private static boolean connectTest() {
		System.out.println("disconnected. testing reconnection...");
		return manager.connect("localhost");
	}

	private static boolean createTest(String user, char[] pass)
			throws IOException {
		System.out.println("createTest with " + user);
		boolean cont = true;
		String qrLocation = manager.createAccount(user, pass);
		cont = cont && (qrLocation != null);
		return cont;
	}

	private static boolean loginAndLogoutTest(String user, char[] pass)
			throws IOException {
		System.out.println("loginAndLogoutTest with " + user);
		boolean cont = true;
		cont = cont && manager.login(user, pass);
		manager.logout();
		return cont;
	}

	private static boolean loginAddOneCredentialAndLookupTest(String user, char[] pass) {
		System.out.println("loginAddOneCredentialAndLookupTest");
		boolean cont = true;
		cont = cont && manager.login(user, pass);
		manager.addCredential("google", "guser", "gpass", "gdesc");
		List<CredentialTuple> tups = manager.lookupAll();

		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		return cont && tups.size() == 1;
	}

	private static boolean removeGoogleGuserTest() {
		System.out.println("removeGoogleGuserTest");
		boolean cont = manager.removeCredential("google", "guser", false);
		List<CredentialTuple> tups = manager.lookupAll();
		return cont && tups.size() == 0;
	}

	private static boolean lookupTestonMultipleCredentials() {
		System.out.println("lookupTestonMultipleCredentials");
		System.out.println("adding 15 credentials.");
		manager.addCredential("google", "guser", "gpass", "gdesc");
		manager.addCredential("google", "guser", "gpass", "gdesc");
		manager.addCredential("google", "guser", "gpass2", "gdesc2");
		manager.addCredential("google", "guser2", "gpass22", "gdesc22");
		manager.addCredential("google", "guser3", "gpass33", "gdesc33");
		manager.addCredential("google", "guser4", "gpass44", "gdesc44");
		manager.addCredential("google", "guser5", "gpass55", "gdesc55");
		manager.addCredential("facebook", "fu", "fp", "fd");
		manager.addCredential("facebook", "fu2", "fp2", "fd2");
		manager.addCredential("facebook", "fu3", "fp3", "fd3");
		manager.addCredential("facebook", "fu4", "fp4", "fd4");
		manager.addCredential("linkedin", "lu", "lp1", "ld1");
		manager.addCredential("linkedin", "lu", "lp2", "ld2");
		manager.addCredential("linkedin", "lu", "lp3", "ld3");
		manager.addCredential("linkedin", "lu", "lp4", "ld4");
		List<CredentialTuple> tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 15)) return false;
		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		System.out.println("looking up google...");
		tups = manager.lookupMatching("google", null);
		if (!assertTupleAmount(tups, 7)) return false;
		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		System.out.println("looking up linkedin lu...");
		tups = manager.lookupMatching("linkedin", "lu");
		if (!assertTupleAmount(tups, 4)) return false;
		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		System.out.println("looking up facebook fu3...");
		tups = manager.lookupMatching("facebook", "fu3");
		if (!assertTupleAmount(tups, 1)) return false;
		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		System.out.println("looking up google guser...");
		tups = manager.lookupMatching("google", "guser");
		if (!assertTupleAmount(tups, 3)) return false;
		for (CredentialTuple tup : tups) {
			System.out.println(tup);
		}

		System.out.println("looking up random...");
		tups = manager.lookupMatching("random", null);
		if (!assertTupleAmount(tups, 0)) return false;

		return true;
	}

	private static boolean removeTestonMultipleCredentials() {
		System.out.println("removeTestonMultipleCredentials");
		System.out.println("removing facebook fu4");
		if (!manager.removeCredential("facebook", "fu4", false)) {
			System.out.println("manager removeCredential failed.");
			return false;
		}
		List<CredentialTuple> tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 14)) return false;

		System.out.println("removing linkedin lu");
		if (manager.removeCredential("linkedin", "lu", false)) {
			System.out.println("manager removeCredential succeeded when it shouldn't.");
			return false;
		}

		System.out.println("removing linkedin lu (forced)");
		if (!manager.removeCredential("linkedin", "lu", true)) {
			System.out.println("manager removeCredential failed.");
			return false;
		}
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 10)) return false;

		System.out.println("removing facebook (forced)");
		if (!manager.removeCredential("facebook", null, true)) {
			System.out.println("manager removeCredential failed.");
			return false;
		}
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 7)) return false;

		System.out.println("removing all credentials");
		if (!manager.removeCredential(null, null, true)) {
			System.out.println("manager removeCredential failed.");
			return false;
		}
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 0)) return false;

		return true;
	}

	private static boolean offlineReconstructAccount(String user, char[] pass) {
		System.out.println("offlineReconstructAccount");
		return manager.loginOffline(user, pass);
	}

	private static boolean offlineLookupCredentials() {
		System.out.println("offlineLookupCredentials");
		List<CredentialTuple> tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 6)) return false;

		tups = manager.lookupMatching("google", "guser");
		if (!assertTupleAmount(tups, 2)) return false;

		tups = manager.lookupMatching("facebook", null);
		if (!assertTupleAmount(tups, 2)) return false;

		System.out.println("looking up random...");
		tups = manager.lookupMatching("random", null);
		if (!assertTupleAmount(tups, 0)) return false;

		return true;
	}

	private static boolean offlineAddRemoveFail() {
		System.out.println("offlineAddRemoveFail");
		if (manager.addCredential("facebook", "futest", "fptest", "fdtest") != null) {
			System.out.println("add succeeded?");
			return false;
		}
		List<CredentialTuple> tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 6)) return false;

		if (manager.removeCredential("facebook", null, true)) {
			System.out.println("remove succeeded?");
			return false;
		}
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 6)) return false;

		return true;
	}

	private static boolean editCredential() {
		UUID uuid = manager.addCredential("sand", "user", "pass", "desc");
		String lastSix = uuid.toString();
		lastSix = lastSix.substring(lastSix.length() - 6);
		System.out.println(lastSix);

		CredentialTuple tup = manager.lookupSpecific(lastSix);
		System.out.println(tup);
		tup.host = "EDITED";
		if (manager.addCredential(tup) ==  null)
			return false;
		tup = manager.lookupSpecific(lastSix);
		System.out.println(tup);
		return true;
	}

	private static boolean editUsername() throws IOException {
		if (!manager.changeUsername("what"))
			return false;

		manager.logout();
		if (manager.login(TEST_USER, TEST_PASS.toCharArray()))
			return false;
		if (!manager.login("what", TEST_PASS.toCharArray()))
			return false;

		if (!manager.changeUsername(TEST_USER))
			return false;

		manager.logout();
		if (manager.login("what", TEST_PASS.toCharArray()))
			return false;
		if (!manager.login(TEST_USER, TEST_PASS.toCharArray()))
			return false;

		return true;
	}

	private static boolean editPassword() throws IOException {
		if (!manager.changePassword(
				TEST_PASS.toCharArray(), TEST_BAD_PASS.toCharArray())) {
			System.out.println("Change password to testbadpass failed");
			return false;
		}

		manager.logout();
		if (manager.login(TEST_USER, TEST_PASS.toCharArray())) {
			System.out.println("Login succeeded?");
			return false;
		}
		if (!manager.login(TEST_USER, TEST_BAD_PASS.toCharArray())) {
			System.out.println("New login failed");
			return false;
		}

		if (!manager.changePassword(
				TEST_BAD_PASS.toCharArray(), TEST_PASS.toCharArray())) {
			System.out.println("New change failed");
			return false;
		}

		manager.logout();
		if (manager.login(TEST_USER, TEST_BAD_PASS.toCharArray())) {
			System.out.println("Changed login succeeded?");
			return false;
		}
		if (!manager.login(TEST_USER, TEST_PASS.toCharArray())) {
			System.out.println("Unable to login originally.");
			return false;
		}

		return true;
	}

	private static boolean cacheTests() throws IOException {
		if (!manager.clearCache())
			return false;
		List<CredentialTuple> tups = manager.lookupAll();
		if (tups != null && !assertTupleAmount(tups, 0)) return false;
		if (!manager.sync())
			return false;
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 7)) return false;

		if (!manager.clearCache())
			return false;
		tups = manager.lookupAll();
		if (tups != null && !assertTupleAmount(tups, 0)) return false;
		manager.logout();
		manager.login(TEST_USER, TEST_PASS.toCharArray());
		tups = manager.lookupAll();
		if (!assertTupleAmount(tups, 7)) return false;

		return true;
	}

	private static boolean runBadTests() throws IOException {
		if (manager.login(TEST_USER, TEST_PASS.toCharArray())) {
			return false;
		}

		if (manager.loginOffline(TEST_USER, TEST_PASS.toCharArray())) {
			return false;
		}

		manager.removeCredential(null, null, true); // remove everything.
		manager.addCredential("google", "guser", "gpass", ""); // add 1
		manager.logout();
		if (manager.removeCredential("google", "guser", true)) {
			System.out.println("Removal while logged out.");
			return false;
		}

		if (manager.addCredential("google2", "guser2", "gpass2", "2") != null) {
			System.out.println("Adding while logged out.");
			return false;
		}

		List<CredentialTuple> tups = manager.lookupAll();
		if (tups != null) {
			System.out.println("Should be null while logged out");
			return false;
		}

		if (manager.changeUsername("hello")) {
			System.out.println("Changing username while logged out");
			return false;
		}
		if (manager.changePassword(TEST_PASS.toCharArray(), TEST_BAD_PASS.toCharArray())) {
			System.out.println("Changing pass while logged out");
			return false;
		}

		if (manager.clearCache()) {
			System.out.println("Clearing cache while logged out");
			return false;
		}


		if (manager.sync()) {
			System.out.println("Syncing cache while logged out");
			return false;
		}

		if (manager.deleteAccount()) {
			System.out.println("delete account succeeded?");
			return false;
		}


		manager.login(TEST_USER, TEST_PASS.toCharArray());
		if (manager.adminGetUserLogs(1000) != null) {
			System.out.println("Admin logs?");
			return false;
		}


		if (manager.blockUser("sanduser", true)) {
			System.out.println("Block user suceeded");
			return false;
		}

		if (!manager.deleteAccount()) {
			System.out.println("unable to delete account");
			return false;
		}

		return true;
	}

	private static void checkPassed(String testname, boolean pass) {
		if (!pass) {
			System.out.println(testname + " failed.");
			System.exit(-1);
		}
		System.out.println(testname + " passed");
	}

	private static boolean assertTupleAmount(List<CredentialTuple> tups, int amt) {
		if (tups.size() != amt) {
			System.out.println("tuples found: " + tups.size() + ". should be " + amt);
			return false;
		}
		return true;
	}
}
