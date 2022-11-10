package idb.buffer;

import java.nio.ByteBuffer;

public class Page{
	public Page(int pagesize){
		data = ByteBuffer.allocate(pagesize);
		dirty = false;
		numFix = 0;
	}
	
	private int numFix;
	private ByteBuffer data;
	private boolean dirty;

	public int getNumFix(){
		return numFix;	
	}
	public ByteBuffer getData(){
		return data;	
	}
	public boolean isDirty(){
		return dirty;	
	}
	public void setDirty(){
		dirty = true;	
	}
	public void fix(){
		numFix++;	
	}
	public void unfix(){
		numFix--;	
	}
}
