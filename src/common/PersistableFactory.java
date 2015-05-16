package common;

public interface PersistableFactory<T extends Persistable> {
	public T newT();
}
