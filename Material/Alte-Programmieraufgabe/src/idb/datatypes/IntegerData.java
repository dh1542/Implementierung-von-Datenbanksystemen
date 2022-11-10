package idb.datatypes;

import java.nio.ByteBuffer;

public class IntegerData implements Key {
	private int value;
	
	public IntegerData(int val){
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
	
	public int hashCode(){
		return value;
	}
	
	public boolean equals(Key o){
		//System.out.println(((IntegerData)o).getValue()+" "+value);
		return value == ((IntegerData)o).getValue();
	}

	public int getValue() {
		return value;
	}
	public String toString(){
		return ""+value;
	}

}
