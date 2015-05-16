package sotrc.client;

import java.io.*;
import java.net.*;
import common.network.*;

public class BasicChatClient {

	private Socket socket;
	private BufferedReader serverIn, stdIn;
	private PrintWriter out;

	public BasicChatClient(String hostname) throws Exception {
		socket = new Socket(hostname,SslDefaults.DEFAULT_SOTRC_PORT);
		serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		out = new PrintWriter(socket.getOutputStream(), true);
		new Reader().start();
		new Writer().start();
	}

	private class Reader extends Thread {
		public Reader() { super(); }
		public void run() {
			try {
			String line;
			while ((line = serverIn.readLine()) != null) {
				System.out.println(line);
			}
			} catch (IOException ignore) { }
		}
	}

	private class Writer extends Thread {
		public Writer() { super(); }
		public void run() {
			try {
			String line;
			while ((line = stdIn.readLine()) != null) {
				out.println(line);
			}
			} catch (IOException ignore) { }
		}
	}

	public static void main(String[] args) throws Exception {
		new BasicChatClient(args[0]);
	}

}
