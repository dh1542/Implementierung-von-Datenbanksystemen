/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.ByteBuffer;

public class DBString implements Key{
	private String s;
	public DBString(String s){
		this.s = s;
	}

	@Override
	public int size() {
		return s.getBytes(UTF_8).length + Integer.SIZE / Byte.SIZE;
	}

	@Override
	public DBString copy() {
		return new DBString(s);
	}

	@Override
	public void read(int idx, ByteBuffer bb){
		int sz = bb.getInt(idx);
		byte[] bytes = new byte[sz];
		for(int i=0; i < sz; ++i){
			bytes[i] = bb.get(idx+Integer.SIZE / Byte.SIZE + i);
		}
		s = new String(bytes, UTF_8);
	}

	public String content() {
		return s;
	}

	@Override
	public void write(int idx, ByteBuffer bb){
		byte[] bytes = s.getBytes(UTF_8);
		int sz = bytes.length;
		bb.putInt(idx, sz);
		for(int i=0; i < sz; ++i){
			bb.put(idx + Integer.SIZE / Byte.SIZE + i, bytes[i]);
		}
		s = new String(bytes, UTF_8);
	}

	public boolean equals(Key k) {
		if (k instanceof DBString) {
			return s.equals(((DBString)k).s);
		}
		return false;
	}

	public int hashCode() {
		return s.hashCode();
	}

	public String toString() {
		return content();
	}
}
