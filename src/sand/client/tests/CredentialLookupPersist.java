package sand.client.tests;

import java.io.*;
import java.util.*;

import common.data.*;
import sand.client.*;

public class CredentialLookupPersist {

	private PersistentKeyValueStore<CredentialLookup> kvstore;

	public CredentialLookupPersist() throws IOException {
		kvstore = new PersistentKeyValueStore<CredentialLookup>(
				new File("./CredentialLookupPersist/"), 512, 512);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		CredentialLookupPersist test = new CredentialLookupPersist();
		String ghost = "google.com";
		UUID gkey1 = UUID.randomUUID();
		UUID gkey2 = UUID.randomUUID();
		UUID gkey3 = UUID.randomUUID();
		Set<UUID> gkeys = new HashSet<UUID>();
		gkeys.add(gkey1); gkeys.add(gkey2); gkeys.add(gkey3);

		String fhost = "facebook.com";
		UUID fkey1 = UUID.randomUUID();
		UUID fkey2 = UUID.randomUUID();
		UUID fkey3 = UUID.randomUUID();
		Set<UUID> fkeys = new HashSet<UUID>();
		fkeys.add(fkey1); fkeys.add(fkey2); fkeys.add(fkey3);

		CredentialLookup glookup = new CredentialLookup(gkeys);
		CredentialLookup flookup = new CredentialLookup(fkeys);

		System.out.println(glookup);
		System.out.println(flookup);


		if (!test.kvstore.put(fhost, flookup)) {
			System.out.println("put messed up on " + fhost);
		}
		if (!test.kvstore.put(ghost, glookup)) {
			System.out.println("put messed up on " + ghost);
		}
		test.kvstore.flush();

		CredentialLookup placeholder = new CredentialLookup();
		if (test.kvstore.get(fhost, placeholder)) {
			System.out.println(placeholder);
		} else {
			System.out.println("failed to get " + fhost);
		}
		if (test.kvstore.get(ghost, placeholder)) {
			System.out.println(placeholder);
		} else {
			System.out.println("failed to get " + ghost);
		}

		test.kvstore.shutdown();
	}

}
