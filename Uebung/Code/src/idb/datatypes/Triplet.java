package idb.datatypes;

// from: https://stackoverflow.com/a/6010861
public class Triplet<T, U, V> {
	private final T first;
	private final U second;
	private final V third;

	public Triplet(T first, U second, V third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public T first() { return first; }
	public U second() { return second; }
	public V third() { return third; }

	public String toString() {
		return "("+first+","+second+","+third+")";
	}
}
