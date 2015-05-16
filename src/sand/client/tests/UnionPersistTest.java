package sand.client.tests;

import java.io.*;
import java.util.*;

import common.*;
import common.data.*;
import sand.client.*;

public class UnionPersistTest {

	private PersistentKeyValueStore<Union<
		CredentialLookup, CredentialTuple>> kvstore;

	public UnionPersistTest() throws IOException {
		kvstore = new PersistentKeyValueStore<Union<
			CredentialLookup, CredentialTuple>>(
				new File("./UnionPersistTest/"), 64, 64);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		UnionPersistTest test = new UnionPersistTest();
		String host = "google.com";
		UUID key1 = UUID.randomUUID();
		UUID key2 = UUID.randomUUID();
		UUID key3 = UUID.randomUUID();
		String key1uname = "andy1", key1pass = "pass1";
		String key1desc = "This is a fairly long description for key 1.";
		String key2uname = "andy2", key2pass = "pass2";
		String key2desc = "This is a fairly long description for key 2. Slightly longer to check for retrieval issues.";
		String key3uname = "andy3", key3pass = "pass3";
		String key3desc = "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer"
			+ "This is a fairly long description for key 3. Slightly longer to check for retrieval issues. Even longer";
		CredentialLookup lookup;
		Set<UUID> keys;
		Union<CredentialLookup, CredentialTuple> union;

		// will expand by 1 every time we add new cred.
		// add key1.toString() make credential tuple.
		keys = new HashSet<UUID>();
		keys.add(key1);
		lookup = new CredentialLookup(keys);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, CredentialTuple.newForReconstruction(), true);
		System.out.println("Adding lookup entry for " + host);
		test.kvstore.put(host, union);

		System.out.println("Adding cred entry for " + key1);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, new CredentialTuple(
				UUID.randomUUID(), host, key1uname, key1pass, key1desc), false);
		test.kvstore.put(key1.toString(), union);

		System.out.println("Items in store:");
		test.kvstore.get(host, union);
		System.out.println(union);
		test.kvstore.get(key1.toString(), union);
		System.out.println(union);

		System.out.println();
		keys = new HashSet<UUID>();
		keys.add(key1);
		keys.add(key2);
		lookup = new CredentialLookup(keys);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, CredentialTuple.newForReconstruction(), true);
		System.out.println("Modifying lookup entry for " + host);
		test.kvstore.put(host, union);

		System.out.println("Adding cred entry for " + key2);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, new CredentialTuple(UUID.randomUUID(),
				host, key2uname, key2pass, key2desc), false);
		test.kvstore.put(key2.toString(), union);

		System.out.println("Items in store:");
		test.kvstore.get(host, union);
		System.out.println(union);
		test.kvstore.get(key1.toString(), union);
		System.out.println(union);
		test.kvstore.get(key2.toString(), union);
		System.out.println(union);


		System.out.println();
		keys = new HashSet<UUID>();
		keys.add(key1);
		keys.add(key2);
		keys.add(key3);
		lookup = new CredentialLookup(keys);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, CredentialTuple.newForReconstruction(), true);
		System.out.println("Modifying lookup entry for " + host);
		test.kvstore.put(host, union);

		System.out.println("Adding cred entry for " + key3);
		union = new Union<CredentialLookup, CredentialTuple>(
			lookup, new CredentialTuple(UUID.randomUUID(),
				host, key3uname, key3pass, key3desc), false);
		test.kvstore.put(key3.toString(), union);

		System.out.println("Items in store:");
		test.kvstore.get(host, union);
		System.out.println(union);
		test.kvstore.get(key1.toString(), union);
		System.out.println(union);
		test.kvstore.get(key2.toString(), union);
		System.out.println(union);
		test.kvstore.get(key3.toString(), union);
		System.out.println(union);

		test.kvstore.shutdown();
	}

}
