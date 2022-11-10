/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import java.nio.ByteBuffer;

public class Bool implements Key{
	boolean b;
	public Bool(boolean b) {
		this.b = b;
	}
	@Override
	public boolean equals(Key o) {
		if (!(o instanceof Bool)) return false;
		return b == ((Bool)o).b;
	}
	@Override
	public int hashCode() {
		return b? 1:0;
	}
	@Override
	public Bool copy() {
		return new Bool(b);
	}
	@Override
	public int size() {
		return 1;
	}
	@Override
	public void write(int i, ByteBuffer b){
		b.put(i, (byte)hashCode());
	}
	@Override
	public void read(int i, ByteBuffer b) {
		this.b = b.get(i)==1;
	}
	public boolean getValue() {
		return b;
	}
	public String toString() {
		return "" + b;
	}
}
