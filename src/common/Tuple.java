package common;

public class Tuple<X, Y> {
	public final X x;
	public final Y y;
	public Tuple(final X x, final Y y) { this.x = x; this.y = y; }

	@Override public String toString() {
		return this.x.toString() + "," + this.y.toString();
	}

	@Override public boolean equals(final Object obj) {
		if (!(obj instanceof Tuple))
			return false;
		Tuple<?, ?> tupobj = (Tuple<?, ?>) obj;
		return tupobj.x.equals(this.x) && tupobj.y.equals(this.y);
	}
}
