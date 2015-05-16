package common.data;

public class Transaction<T> {

	public static enum Type { GET, GETL, PUT, PUTL, DEL, MOVE, EXISTS }

	public static interface Event<T> {
		public void process(Transaction<T> txn);
	}

	public final Type type;
	public final String key;
	public final T value;
	String newKey;
	long l;
	boolean succeeded;
	private boolean completed;
	Event<T> boundEvent;

	Transaction(Type type, String key, T value) {
		this.type = type;
		this.key = key;
		this.value = value;
	}

	Transaction(String key, String newKey) {
		this.type = Type.MOVE;
		this.key = key;
		this.newKey = newKey;
		this.value = null;
	}

	Transaction(String key, long value) {
		this.type = Type.PUTL;
		this.key = key;
		this.l = value;
		this.value = null;
	}

	Transaction(String key) {
		this(key, Type.GETL);
	}

	Transaction(String key, Type type) {
		this.type = type;
		this.key = key;
		this.value = null;
	}

	public boolean isCompleted() {
		return completed;
	}

	public boolean succeeded() {
		return succeeded;
	}

	public void bind(Event<T> event) {
		boundEvent = event;
	}

	synchronized void setCompleted() {
		completed = true;
		notifyAll();
	}

	public synchronized void waitCompleted() throws InterruptedException {
		while (!completed) {
			wait();
		}
	}

}
