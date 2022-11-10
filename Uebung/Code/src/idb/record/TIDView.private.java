/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.record;

import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.datatypes.DataObject;

import idb.datatypes.TID;
import idb.datatypes.TIDIndex;
import idb.datatypes.Triplet;
import idb.buffer.BufferFullException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TIDView<D extends DataObject> implements View<D> {
	private DBBuffer buf;
	private BlockFile block;
	private int curBlock = 0;
	private int idx = 0;
	private int indexSize;
	private int pageSize;
	private TIDFile<D> file;
	public TIDView(DBBuffer bu, BlockFile bf, int indexSize, TIDFile<D> f){
		buf = bu;
		block = bf;
		file = f;
		this.indexSize = indexSize;
		this.pageSize = buf.getPagesize();
	}

	public void restart() {
		idx = 0;
		curBlock = 0;
	}

	private int getOffsetforTIDIndex(int tidIndex) {
		return pageSize - indexSize * (tidIndex+1);
	}

	public boolean next(D out) throws IOException, BufferFullException{
		do {
			if (curBlock >= block.size()) return false;
			int cB = curBlock;
			ByteBuffer bb = buf.fix(block, curBlock);
			try{
				TIDIndex indexSpace = new TIDIndex(indexSize);
				indexSpace.read(getOffsetforTIDIndex(idx), bb);
				idx++;
				if (indexSpace.isInvalid()) {
					idx = 0;
					curBlock++;
				}
				else if (indexSpace.exported()) {
					try{
						file.read(new TID(curBlock, idx-1), out);
						return true;
					} catch(DeletedRecordException dre) {
					}
					finally {
						if (indexSpace.fragmented()) {
							idx = 0;
							curBlock++;
						}
					}
				}
				else if(indexSpace.fragmented()) {
					idx = 0;
					curBlock++;
				}
			}
			finally{
				buf.unfix(block, cB);
			}
		}
		while(true);
	}
}
