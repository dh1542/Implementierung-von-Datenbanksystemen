/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.record;
import idb.datatypes.DataObject;
import idb.datatypes.DBInteger;
import idb.datatypes.Triplet;

import idb.buffer.DBBuffer;
import idb.block.BlockFile;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import idb.buffer.BufferFullException;
import java.io.IOException;
public class SeqRecordFileImpl<D extends DataObject> implements SeqRecordFile<D> {
	private DBBuffer buf;
	private BlockFile bf;
	private int freeBlock;
	private int freeIdx;
	private int pageSize;

	private SeqRecordFileImpl(DBBuffer buffer, BlockFile bFile, int freeB, int freeI){
		bf = bFile;
		buf = buffer;
		freeBlock = freeB;
		freeIdx = freeI;
		pageSize = buf.getPagesize();
	}

	/**
	 * creates a new SeqRecordFileImpl. Not suiteable to reconstruct an already existing one from storage. use @reconstruct for this purpose.
	 * Assumes bFile point to already opened fresh file of size 0
	 */
	public SeqRecordFileImpl(DBBuffer buffer, BlockFile bFile) throws IOException{
		this(buffer, bFile, 0, 2*Integer.SIZE / Byte.SIZE);
		bf.append(1);
	}

	public static <D2 extends DataObject> SeqRecordFileImpl<D2> reconstruct(DBBuffer buffer, BlockFile bFile) throws IOException, BufferFullException{
		ByteBuffer bb = buffer.fix(bFile, 0);
		SeqRecordFileImpl<D2> ret = new SeqRecordFileImpl<D2>(buffer, bFile, bb.getInt(0), bb.getInt(Integer.SIZE / Byte.SIZE));
		buffer.unfix(bFile, 0);
		return ret;
	}

	@Override
	public void close() throws IOException, BufferFullException{
		ByteBuffer bb = buf.fix(bf, 0);
		bb.putInt(0, freeBlock);
		bb.putInt(Integer.SIZE / Byte.SIZE, freeIdx);
		buf.setDirty(bf, 0);
		buf.unfix(bf, 0);
	}

	@Override
	public void insert(D object) throws IOException, BufferFullException{
		// We prefix each object with its size
		// If there is not eanough space for the int, we go to the next block
		DBInteger ik = new DBInteger(object.size());
		if (ik.size() + freeIdx > pageSize) {
			// there is not eanough space for a length entry
			bf.append(1);
			freeIdx = 0;
			freeBlock++;
		}

		ByteBuffer bb = buf.fix(bf, freeBlock);
		ik.write(freeIdx, bb);
		buf.setDirty(bf, freeBlock);
		freeIdx += ik.size();

		if (object.size() + freeIdx <= pageSize) {
			// No fragmentation needed
			object.write(freeIdx, bb);
			freeIdx += object.size();
			buf.unfix(bf, freeBlock);
			return;
		}
		else {
			// use the remaining space
			object.writePart(freeIdx, bb, 0, pageSize - freeIdx);
			int objectIndex = pageSize - freeIdx;
			do{
				bf.append(1);
				freeIdx = 0;
				buf.unfix(bf, freeBlock);
				freeBlock++;
				bb = buf.fix(bf, freeBlock);
				if (object.size() - objectIndex <= pageSize) {
					object.writePart(freeIdx, bb, objectIndex, object.size() - objectIndex);
					freeIdx = object.size() - objectIndex;
					buf.setDirty(bf, freeBlock);
					buf.unfix(bf, freeBlock);
					return;
				}
				object.writePart(freeIdx, bb, objectIndex, pageSize);
				objectIndex += pageSize;
				buf.setDirty(bf, freeBlock);
			} while(true);
		}
	}

	public View<D> view() {
		return new View<D>(){
			int curBlock = 0;
			int curIdx = 2*Integer.SIZE / Byte.SIZE;
			@Override
			public void restart(){
				curBlock = 0;
				curIdx = 2*Integer.SIZE / Byte.SIZE;
			}
			@Override
			public boolean next(D d) throws IOException, BufferFullException{
				if (curBlock > freeBlock) return false;
				if (curBlock == freeBlock && curIdx >= freeIdx) return false;
				DBInteger ik = new DBInteger(0);
				ByteBuffer bb = buf.fix(bf, curBlock);
				try{
					ik.read(curIdx, bb);
					int value = ik.getValue();
					curIdx += Integer.SIZE / Byte.SIZE;
					if (value + curIdx <= pageSize) {
						//unfragmented
						d.read(curIdx, bb);
						curIdx += value;
						assert d.size() == value;
						return true;
					}
					// fragmentation
					List<Triplet<ByteBuffer, Integer, Integer>> parts = new ArrayList<>();
					List<Integer> blocks = new ArrayList<>();
					do {
						Triplet<ByteBuffer, Integer, Integer> part = new Triplet<>(bb, curIdx, pageSize - curIdx);
						parts.add(part);
						blocks.add(curBlock);
						value -= (pageSize - curIdx);
						buf.setDirty(bf, curBlock);
						curBlock++;
						curIdx = 0;
						bb = buf.fix(bf, curBlock);
					}
					while(value > pageSize);
					parts.add(new Triplet<>(bb, 0, value));
					curIdx = value;
					d.readPart(parts);
					for (int i: blocks) {
						buf.unfix(bf, i); //TODO: check use-after-free correctly
					}
					return true;
				}
				finally{
					buf.unfix(bf, curBlock);
					if (curIdx + ik.size() > pageSize) {
						curBlock++;
						curIdx = 0;
					}
				}
			}
		};
	}

}
