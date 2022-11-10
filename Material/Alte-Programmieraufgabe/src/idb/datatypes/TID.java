package idb.datatypes;

import java.nio.ByteBuffer;

public class TID implements DataObject{
	private int block;
	private int index;
	
	public TID(int block, int index){
		this.block = block;
		this.index = index;
	}
	

	public int getBlock(){
		return block;
	}
	
	public int getIndex(){
		return index;
	}

	public void read(int index, ByteBuffer bb){
		block = bb.getInt(index);
		this.index = bb.getInt(index+Integer.SIZE/Byte.SIZE);
	}

	public void write(int index, ByteBuffer bb){
		bb.putInt(index, block);
		bb.putInt(index+Integer.SIZE/Byte.SIZE, this.index);
	}

	public int size(){
		return (Integer.SIZE/Byte.SIZE)*2;	
	}
	
	public String toString(){
		return "Block: "+block+" Index: "+index;
	}
}
