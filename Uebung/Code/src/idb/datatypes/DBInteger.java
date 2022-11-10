package idb.datatypes;

import java.nio.ByteBuffer;

public class DBInteger implements Key {
	private int value;

	public DBInteger(int val){
		this.value = val;
	}


	@Override
	public void read(int index, ByteBuffer bb) {
		value = bb.getInt(index);
	}

	@Override
	public void write(int index, ByteBuffer bb) {
		bb.putInt(index, value);
	}

	@Override
	public int size() {
		return Integer.SIZE/Byte.SIZE;
	}

	@Override
	public DBInteger copy() {
		return new DBInteger(value);
	}

	public int hashCode(){
		return value;
	}

	public boolean equals(Key o){
		return value == ((DBInteger)o).getValue();
	}

	public int getValue() {
		return value;
	}
	public String toString(){
		return ""+value;
	}

}
