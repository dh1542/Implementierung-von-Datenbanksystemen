/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.buffer;

import java.nio.ByteBuffer;
import java.io.IOException;

import idb.block.BlockFile;

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
		return data.duplicate();
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
	public void clear(BlockFile file, int blockNo) throws BufferNotEmptyException, IOException {
		clear(file, blockNo, true);
	}
	// clears this page and write the data back to the disc if needed
	public void clear(BlockFile file, int blockNo, boolean override) throws BufferNotEmptyException, IOException {
		if (numFix != 0) {
			throw new BufferNotEmptyException("Cannot clear a fixed page");
		}
		try{
			if (!dirty) {
				return;
			}
			dirty = false;
			file.write(blockNo, data);
		}
		finally {
			for (int i=0; override && ( i < data.capacity()); ++i){
				data.put(i, (byte) i);
			}
		}
	}

	// loads the content of the block into this page
	public void load(BlockFile file, int blockNo) throws BufferFullException, IOException {
		if (numFix != 0) {
			throw new BufferFullException("Cannot load into a fixed page");
		}
		dirty = false;
		file.read(blockNo, data);
	}
}
