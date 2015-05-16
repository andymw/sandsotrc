package common.data;

import java.io.*;

import common.*;

public class KVStoreTest {

	private PersistentKeyValueStore<Foo> kvstore;

	public KVStoreTest() throws IOException {
		kvstore = new PersistentKeyValueStore<Foo>(new File("./KVStoreTest/"),
			100, 256);
	}

	static class Foo implements Persistable {
		private String a, b;
		public Foo(String a, String b) {
			this.a = a;
			this.b = b;
		}
		public void persist(OutputStream os) throws IOException {
			DataOutput output = new DataOutputStream(os);
			output.writeUTF(a);
			output.writeUTF(b);
		}
		public void reconstruct(InputStream is) throws IOException {
			DataInput input = new DataInputStream(is);
			a = input.readUTF();
			b = input.readUTF();
		}
		public String toString() {
			return "a=" + a + "; b=" + b;
		}
	}

	private void doPut(final String key, final Foo value) {
		System.out.println("Putting " + key + ": " + value);
		kvstore.putAsync(key, value).bind(new Transaction.Event<Foo>() {
			@Override
			public void process(Transaction<Foo> txn) {
				if (txn.succeeded) {
					System.out.println("Put " + key + ": " + value + " succeeded!");
				} else {
					System.out.println("Put " + key + ": " + value + " failed!");
				}
			}
		});
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		KVStoreTest test = new KVStoreTest();
		Foo foo = new Foo("hello", "world");
		test.doPut("foo1", foo);
		foo = new Foo("whatup", "this is a test");
		test.doPut("waddup", foo);
		foo = new Foo("CS 5430", "SAND and SOTRC are the names of our systems!");
		test.doPut("foo2", foo);
		foo = new Foo("Fourth", "May the fourth be with you");
		test.doPut("fourth", foo);
		test.kvstore.flush(); // wait for puts to complete
		System.out.println();

		test.kvstore.get("foo1", foo);
		System.out.println("foo1 is " + foo);
		test.kvstore.get("foo2", foo);
		System.out.println("foo2 is " + foo);
		test.kvstore.get("waddup", foo);
		System.out.println("waddup is " + foo);
		test.kvstore.get("foo1", foo);
		System.out.println("foo1 is " + foo);
		test.kvstore.get("waddup", foo);
		System.out.println("waddup is " + foo);
		test.kvstore.get("foo2", foo);
		System.out.println("foo2 is " + foo);
		test.kvstore.get("waddup", foo);
		System.out.println("waddup is " + foo);
		test.kvstore.get("fourth", foo);
		System.out.println("fourth is " + foo);
		test.kvstore.get("waddup", foo);
		System.out.println("waddup is " + foo);
		test.kvstore.get("fourth", foo);
		System.out.println("fourth is " + foo);
		test.kvstore.get("foo1", foo);
		System.out.println("foo1 is " + foo);
		test.kvstore.get("fourth", foo);
		System.out.println("fourth is " + foo);
		test.kvstore.get("foo2", foo);
		System.out.println("foo2 is " + foo);
		test.kvstore.get("fourth", foo);
		System.out.println("fourth is " + foo);
		test.kvstore.get("foo2", foo);
		System.out.println("foo2 is " + foo);
		test.kvstore.get("foo10", foo);
		System.out.println("foo10 is " + foo);
		test.kvstore.shutdown();
	}

}
