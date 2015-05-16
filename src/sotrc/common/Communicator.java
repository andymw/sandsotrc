package sotrc.common;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import common.*;
import common.network.*;

/**
 *
 */
public class Communicator<T extends Persistable, U extends Persistable>
implements Runnable {

	private static final byte REQUEST = 0;
	private static final byte RESPONSE = 1;

	private final Random random;
	private final InputStream in;
	private final OutputStream out;
	private final RequestHandler<U> handler;
	private final PersistableFactory<U> factory;

// current response that needs to be processed
	private int currentResponseID;
	private boolean responseProcessed = true;

	private Exception exception;
	private Callback<Exception> exceptionCallback;

	private ExecutorService executor;

	private volatile boolean running;

	public static interface RequestHandler<U extends Persistable> {
		public void processRequest(int requestID, U request);
	}

	public Communicator(InputStream in, OutputStream out,
			PersistableFactory<U> factory, RequestHandler<U> handler) {
		this.in = in;
		this.out = out;
		this.factory = factory;
		this.handler = handler;
		this.random = new Random(); // doesn't need to be secure, just nonce-like
	}

	public synchronized int sendRequest(T request) throws IOException {
		out.write(REQUEST);
		int id = random.nextInt();
		Utils.writeInt(id, out);
		request.persist(out);
		out.flush();
		return id;
	}

	public synchronized Persistable readResponse(int requestID, Persistable receiver, 
				boolean packetized) throws IOException {
		while ((currentResponseID != requestID || responseProcessed) && running) {
			try { wait(); }
			catch (InterruptedException e) { return null; }
		}
		if (!running) throw new IOException("Communicator shutting down.");
		// now the next thing in the socket receive buffer is our response
		if (packetized) {
			PacketizedInputStream packin = new PacketizedInputStream(in);
			receiver.reconstruct(packin);
		} else {
			receiver.reconstruct(in);
		}
		responseProcessed = true;
		return receiver;
	}

	public synchronized Persistable readResponse(int requestID, Persistable receiver)
			throws IOException {
		return readResponse(requestID, receiver, false);
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * The persistable must not be mutated after calling this method.
	 */
	public synchronized void sendResponse(int requestID, Persistable response,
			boolean packetized) throws IOException {
		out.write(RESPONSE);
		Utils.writeInt(requestID, out);
		if (packetized) {
			PacketizedOutputStream packout = new PacketizedOutputStream(out);
			response.persist(packout);
			packout.finish();
		} else {
			response.persist(out);
		}
		out.flush();
	}

	public synchronized void sendResponse(int requestID, Persistable response)
			throws IOException {
		sendResponse(requestID, response, false);
	}

	public synchronized void shutdown() {
		running = false;
		notifyAll();
	}

	class ProcessorThread implements Runnable {
		private int id; private U request;
		public ProcessorThread(int id, U request) {
			this.id = id; this.request = request;
		}
		@Override public void run() {
			handler.processRequest(id, request);
		}
	}

	public void registerExceptionCallback(Callback<Exception> callback) {
		exceptionCallback = callback;
	}

	@Override
	public void run() {
		int type, id;

		running = true;
		while (running) {
			try {
				type = in.read(); // REQUEST or RESPONSE
				id = Utils.readInt(in);
			} catch (IOException e) { exception = e; break; }
			if (type == RESPONSE) {
				currentResponseID = id;
				responseProcessed = false;
				synchronized (this) { notifyAll(); }
				if (!responseProcessed) {
					// wait a little and try again - a bit of a hack
					try { Thread.sleep(200); }
					catch (InterruptedException ignore) { }
					synchronized (this) { notifyAll(); }
					if (!responseProcessed)
						throw new RuntimeException("A response fell on deaf ears!");
				}
			} else if (type == REQUEST) {
				final U request = factory.newT();
				try { request.reconstruct(in); }
				catch (IOException e) { exception = e; break; }
				if (executor == null)
					new Thread(new ProcessorThread(id, request)).start();
				else executor.submit(new ProcessorThread(id, request));
			} else
				System.out.println("Something ELSE!");
		}
		if (exception != null && exceptionCallback != null) {
			System.out.println("Processing exception " + exception);
			exceptionCallback.process(exception);
		}
		try {
			in.close();
			out.close();
		} catch (IOException ignore) { }
	}


/// TESTING!!!!!!!

	static class PseudoSocketPair {
		PipedInputStream Ain, Bin;
		PipedOutputStream Aout, Bout;

		public PseudoSocketPair() throws IOException {
			Ain = new PipedInputStream();
			Bin = new PipedInputStream();
			Bout = new PipedOutputStream(Ain);
			Aout = new PipedOutputStream(Bin);
		}
	}

	static Random r = new Random();

	static class Foo implements Persistable {
		byte a;
		byte b;
		public Foo() { a = (byte) r.nextInt(256); b = (byte) r.nextInt(256); }
		public Foo(byte x, byte y) { a=x;b=y;}
		public void persist(OutputStream out) throws IOException {
			out.write(a);
			out.write(b);
		}
		public void reconstruct(InputStream is) throws IOException {
			a = (byte)is.read();
			b = (byte)is.read();
		}
		public String toString() { return a + " " + b; }
	}

	static class FooFactory implements PersistableFactory<Foo> {
		public Foo newT() { return new Foo(); }
	}

	static class BarFactory implements PersistableFactory<Bar> {
		public Bar newT() { return new Bar(); }
	}

	static class Bar implements Persistable {
		byte a;
		byte b;
		byte c;
		public Bar() { a = (byte) r.nextInt(256); b = (byte) r.nextInt(256); c = 42;}
		public Bar(byte x, byte y, byte z) { a=x;b=y;c=z; }
		public void persist(OutputStream out) throws IOException {
			out.write(a);
			out.write(b);
			out.write(c);
		}
		public void reconstruct(InputStream is) throws IOException {
			a = (byte)is.read();
			b = (byte)is.read();
			c = (byte)is.read();
		}
		public String toString() { return a + " " + b + " " + c; }
	}

	static Communicator<Foo, Bar> serverComm;
	static Communicator<Bar, Foo> clientComm;

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Key: (client left, server right)");
		System.out.println("1CSRq> = client sends request");
		System.out.println("2SRRq> = server receives request");
		System.out.println("3SSRp> = server sends response");
		System.out.println("4CRRp> = client receives response");
		System.out.println();
		System.out.println("1SSRq< = server sends request");
		System.out.println("2CRRq< = client receives request");
		System.out.println("3CSRp< = client sends response");
		System.out.println("4SRRp< = server receives response");
		System.out.println();

		Thread.sleep(1000);
		PseudoSocketPair psp = new PseudoSocketPair();
		serverComm = new Communicator<Foo, Bar>(
				psp.Ain, psp.Aout, new BarFactory(), new RequestHandler<Bar>() {
					public void processRequest(int id, Bar bar) {
						System.out.println(id + "   2SRRq> bar " + bar);
						Foo foo = new Foo();
						System.out.println(id + "     3SSRp> foo " + foo);
						try {
						serverComm.sendResponse(id, foo);
						} catch (Exception e) { }
					}
				});
		new Thread(serverComm).start();
	
		// one server issuing random requests	
		new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < 7; i++) {
					try { Thread.sleep(r.nextInt(200)); }
					catch (InterruptedException ignore) { }
					Foo f = new Foo();
					Bar response = new  Bar();
					try {
					int id = serverComm.sendRequest(f);
					System.out.println(id + " 1SSRq< foo " + f);
					serverComm.readResponse(id, response);
					System.out.println(id + "       4SRRp< bar " + response);
					} catch (Exception e) { e.printStackTrace(); }
				}
			}
		}).start();

		clientComm = new Communicator<Bar, Foo>(
				psp.Bin, psp.Bout, new FooFactory(), new RequestHandler<Foo>() {
					public void processRequest(int id, Foo foo) {
						System.out.println(id + "   2CRRq< foo " + foo);
						Bar bar = new Bar();
						System.out.println(id + "     3CSRp< bar " + bar);
						try {
						clientComm.sendResponse(id, bar);
						} catch (Exception e) { }
					}
				});
		new Thread(clientComm).start();

		// client threads sending random requests
		for (int i = 0; i < 2; i++) {
			new Thread(new Runnable() {
				public void run() {
					for (int j = 0; j < 3; j++) {
						try { Thread.sleep(r.nextInt(500)); }
						catch (InterruptedException ignore) { }
						Bar b = new Bar();
						Foo response = new Foo();
						try {
						int id = clientComm.sendRequest(b);
						System.out.println(id + " 1CSRq> bar " + b);
						clientComm.readResponse(id, response);
						System.out.println(id + "       4CRRp> foo " + response);
						} catch (Exception e) { e.printStackTrace(); }
					}
				}
			}).start();
		}
	}

}
