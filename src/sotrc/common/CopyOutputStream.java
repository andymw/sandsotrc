package sotrc.common;

import java.io.*;
import java.util.*;

class LabeledOutputStream extends FilterOutputStream {

	String label;

	LabeledOutputStream(OutputStream out, String label) {
		super(out);
		this.label = label;
	}

}

public class CopyOutputStream extends OutputStream {

	public static class FailedSinksException extends IOException {
		private List<String> failed;
		FailedSinksException() { failed = new ArrayList<String>(); }
		void addLabel(String label) {
			failed.add(label);
		}
	}

	private List<LabeledOutputStream> sinks;

	public CopyOutputStream() {
		sinks = new ArrayList<LabeledOutputStream>();
	}

	public synchronized void addSink(OutputStream sink, String label) {
		sinks.add(new LabeledOutputStream(sink, label));
	}

	public synchronized void removeSink(String label) {
		for (LabeledOutputStream sink : sinks) {
			if (sink.label.equals(label)) {
				sinks.remove(sink);
				return;
			}
		}
	}

	@Override
	public synchronized void write(int b) throws IOException {
		FailedSinksException failed = null;
		for (LabeledOutputStream sink : sinks) {
			try {
				sink.write(b);
			} catch (IOException e) {
				if (failed == null) failed = new FailedSinksException();
				failed.addLabel(sink.label);
			}
		}
		if (failed != null) throw failed;
	}

	@Override
	public synchronized void write(byte[] b, int offs, int len) throws IOException {
		FailedSinksException failed = null;
		for (LabeledOutputStream sink : sinks) {
			try {
				sink.write(b, offs, len);
			} catch (IOException e) {
				if (failed == null) failed = new FailedSinksException();
				failed.addLabel(sink.label);
			}
		}
		if (failed != null) throw failed;
	}

	@Override
	public synchronized void flush() throws IOException {
		for (OutputStream sink : sinks)
			sink.flush();
	}

	@Override
	public synchronized void close() throws IOException {
		for (OutputStream sink : sinks)
			sink.close();
	}

}
