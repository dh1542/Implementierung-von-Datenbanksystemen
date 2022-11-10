/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.record;

import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.datatypes.DataObject;
import idb.datatypes.DOSizeWrapper;
import idb.datatypes.DOPartialWrapper;
import idb.datatypes.TID;
import idb.datatypes.Triplet;
import idb.buffer.BufferFullException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import idb.datatypes.TIDIndex;

public class TIDFile<D extends DataObject> implements DirectRecordFile<TID, D> {
	// Concept: TID. We need 3 bits for fragmented, moved and published. setting moved and fragmented inicates end of entries, the index is still set correctly to reflect the start of the free memory. Deleted is marked by beeing followed by the exact same index again (a size 0 entry is deleted).
	// We caluclate the number of needed bytes as index as follows: ceil((ld(pagesize)+3)/8)
	private DBBuffer buffer;
	private BlockFile bf;
	private int pageSize;
	private int indexSize;
	private TIDIndex indexHolder;


	// freeMemory can be initialized on construction or lazy, this has to be accounted for in testing. OR: we push one way and test it, leaving the rest for the contest.
	// Conclusion: We force an eager approach. The following arrayList has to contain the empy space for each block.
	private ArrayList<Integer> freeMemory;

	public TIDFile(DBBuffer buf, BlockFile bf) throws idb.buffer.BufferFullException, java.io.IOException {
		buffer = buf;
		pageSize = bf.bytes();
		// from: https://stackoverflow.com/a/21830188
		// indexSize = ((32 - Integer.numberOfLeadingZeros(pageSize - 1)) + 3 + 8 - 1) / 8
		indexSize = (42 - Integer.numberOfLeadingZeros(pageSize - 1)) / 8;
		assert indexSize <= 5: "Integers only have 4 bytes (+3 bits for flags)";
		indexHolder = new TIDIndex(indexSize);
		this.bf = bf;
		freeMemory = new ArrayList<>();
		for (int i = 0; i < bf.size(); ++i){
			freeMemory.add(readFreeMem(i));
		}
	}

	private int readFreeMem(int page) throws idb.buffer.BufferFullException, java.io.IOException{
		ByteBuffer mem = buffer.fix(bf, page);
		int pastTheEndPtr = loadEnd(mem);
		int beginPtr = indexHolder.index();
		buffer.unfix(bf, page);
		//System.out.println("Ret: " + (pastTheEndPtr - beginPtr));
		return pastTheEndPtr - beginPtr;
	}

	// loads the end of TIDIndex into indexHolder
	//@return position of the indexHolder in mem
	private int loadEnd(ByteBuffer mem) {
		int pastTheEndPtr = pageSize;
		int beginPtr = 0;
		do {
			indexHolder.read(pastTheEndPtr - indexSize, mem);
			pastTheEndPtr -= indexSize;
			beginPtr = indexHolder.index();
		} while (!indexHolder.isInvalid());
		//System.out.println("LE: " + pastTheEndPtr);
		return pastTheEndPtr;
	}

	@Override
	public TID insert(D objectP) throws IOException, BufferFullException{
		//NODO: object.size() has to be larger or equal to TID.size(), otherwise boom.
		//To make sure this is true, we use the DOSizeWrapper. As the wrapper does just modify size, even side-effects of calling read, write or size on D are preserved.
		return insertInternal(new DOSizeWrapper(objectP, new TID(0, 0).size()), true);
	}
	private TID insertInternal(DataObject object, boolean exported) throws IOException, BufferFullException {
		int size = object.size() + indexHolder.size(); // indexHolder.size is invariant
		// Note: one could optimize here and check for (oversized) entries
		if ( size > pageSize - indexHolder.size()) {
			// fragmentation
			int idx = bf.size();
			ByteBuffer bb = newPage();
			try{
				TID rid = new TID(0, 0);
				DOPartialWrapper remaining = new DOPartialWrapper(object, pageSize - indexHolder.size() - rid.size());
				object.writePart(rid.size(), bb, 0, pageSize - indexHolder.size() - rid.size());
				freeMemory.set(idx, 0);
				rid = insertInternal(remaining, false);
				rid.write(0, bb);
				indexHolder.setMoved(false);
				indexHolder.setFragmented(true);
				indexHolder.setExported(exported);
				indexHolder.setIndex(0);
				indexHolder.write(pageSize - indexHolder.size(), bb);
			}
			finally {
				buffer.unfix(bf, idx);
			}
			return new TID(idx, 0);
		}
		int index = 0;
		ByteBuffer bb = null;
		for (int i = 0; i < freeMemory.size(); ++i){
			if (freeMemory.get(i) >= size) {
				bb = buffer.fix(bf, i);
				index = i;
				break;
			}
		}
		if (bb == null) {
			index = bf.size();
			bb = newPage();
		}
		int indexOffset = loadEnd(bb);
		object.write(indexHolder.index(), bb);
		indexHolder.setMoved(false);
		indexHolder.setFragmented(false);
		indexHolder.setExported(exported);
		indexHolder.write(indexOffset, bb);
		indexHolder.setEnd(indexHolder.index() + object.size());
		indexHolder.write(indexOffset - indexSize, bb);
		freeMemory.set(index, freeMemory.get(index) - object.size() - indexSize);
		buffer.setDirty(bf, index);
		buffer.unfix(bf, index);
		return new TID(index, (pageSize - indexOffset) / indexSize - 1);
	}

	// Warning: keeps (bf, bf.size()) fixxed for futher operations, user has to unfix it
	private ByteBuffer newPage() throws java.io.IOException, idb.buffer.BufferFullException {
		int index = bf.size();
		bf.append(1);
		ByteBuffer mem = buffer.fix(bf, index);
		indexHolder.setEnd(0);
		indexHolder.write(pageSize - indexSize, mem);
		freeMemory.add(pageSize - indexSize);
		buffer.setDirty(bf, index);
		return mem;
	}

	@Override
	public void read(TID in, D out) throws IOException, BufferFullException, DeletedRecordException {
		ByteBuffer mem = buffer.fix(bf, in.getBlock());
		indexHolder.read(pageSize - indexSize * (in.getIndex()+1), mem);
		// fragmented does not contain any following Index
		if (indexHolder.fragmented()) {
			List<Triplet<ByteBuffer, Integer, Integer>> readings = new ArrayList<>();
			List<Integer> blocks = new ArrayList<>();
			TID cur = (TID) in.copy();
			while(indexHolder.fragmented()) {
				readings.add(new Triplet<>(mem, cur.size(), pageSize - indexSize - cur.size() /*TID Size is constant*/));
				blocks.add(cur.getBlock());
				cur.read(0, mem);
				mem = buffer.fix(bf, cur.getBlock());
				indexHolder.read(getOffsetforTIDIndex(cur.getIndex()), mem);
			}
			TIDIndex following = new TIDIndex(indexSize);
			following.read(getOffsetforTIDIndex(cur.getIndex()+1), mem);
			assert following.index() != indexHolder.index(): "Partially deleted Record";
			assert !indexHolder.moved() : "Parially moved Record";
			readings.add(new Triplet<>(mem, indexHolder.index(), following.index() - indexHolder.index()));
			out.readPart(readings);
			for(Integer i : blocks){
				buffer.unfix(bf, i);
			}
			buffer.unfix(bf, cur.getBlock());
			return;
		}
		TIDIndex following = new TIDIndex(indexSize);
		following.read(pageSize - indexSize * (in.getIndex() + 2), mem);
		if (following.index() == indexHolder.index()) {
			buffer.unfix(bf, in.getBlock());
			throw new DeletedRecordException("TID("+in.getBlock()+", "+in.getIndex()+")");
		}
		if (indexHolder.moved()) {
			TID tid = new TID(0,0);
			tid.read(indexHolder.index(), mem);
			buffer.unfix(bf, in.getBlock());
			read(tid, out);
			return;
		}
		out.read(indexHolder.index(), mem);
		buffer.unfix(bf, in.getBlock());
	}

	// moves @index+@offset : @index +@offset + @size to @index : @index+@size
	private static void moveForward(ByteBuffer bb, int index, int size, int offset) {
		assert offset >= 0;
		// as we move Forward, we can override the earlier
		for (int i=0; i < size; ++i){
			bb.put(index+i, bb.get(index+i+offset));
		}
	}

	// moves @index : @index + @size to @index + @offset : @index + @offset + @size
	private static void moveBackward(ByteBuffer bb, int index, int size, int offset) {
		assert offset >= 0;
		// as we move Backward, we can override the later
		for (int i = size - 1; i >= 0; --i){
			bb.put(index+offset+i, bb.get(index+i));
		}
	}

	private int getOffsetforTIDIndex(int tidIndex) {
		return pageSize - indexSize * (tidIndex+1);
	}

	@Override
	public void modify(TID position, D object) throws IOException, BufferFullException, DeletedRecordException {
		mod(position, new DOSizeWrapper(object, new TID(0, 0).size()), true);
	}

	private TID mod(TID position, DataObject object, boolean writeTID) throws IOException, BufferFullException, DeletedRecordException {
		ByteBuffer mem = buffer.fix(bf, position.getBlock());
		indexHolder.read(getOffsetforTIDIndex(position.getIndex()), mem);
		// Note: one could check indexHolder.exported in order to avoid writing an useless marker and recycle the TID. This is not mandatory
		if (indexHolder.moved()) {
			assert writeTID;
			TID tid = new TID(0,0);
			tid.read(indexHolder.index(), mem);
			tid = mod(tid, object, false);
			// Note: this code does not move a already moved record in the original block if there is some space left.
			// This might be an easy opportunity for improvement, but is not mandatory.
			indexHolder.read(getOffsetforTIDIndex(position.getIndex()), mem);
			tid.write(indexHolder.index(), mem);
			// Note: setDirty is in some cases optional. Again, not mandatory.
			buffer.setDirty(bf, position.getBlock());
			buffer.unfix(bf, position.getBlock());
			return tid;
		}
		if (indexHolder.fragmented()) {
			assert indexHolder.index() == 0 : "Broken fragmentation Header";
			if (object.size() <= pageSize - 2*indexSize){ // it now fits in this block.
				TID following = new TID(0, 0);
				following.read(0, mem);
				indexHolder.setFragmented(false);
				indexHolder.write(getOffsetforTIDIndex(0), mem);
				indexHolder.setEnd(object.size());
				indexHolder.write(getOffsetforTIDIndex(1), mem);
				object.write(0, mem);
				freeMemory.set(position.getBlock(), pageSize - 2*indexSize - object.size());
				buffer.setDirty(bf, position.getBlock());
				buffer.unfix(bf, position.getBlock());
				delete(following);
				return position;
			}
			TID tid = new TID(0, 0);
			tid.read(0, mem);
			tid = mod(tid, new DOPartialWrapper(object, pageSize - indexSize - tid.size()), false);
			tid.write(0, mem);
			object.writePart(tid.size(), mem, 0, pageSize - indexSize - tid.size());
			buffer.setDirty(bf, position.getBlock());
			buffer.unfix(bf, position.getBlock());
			return position;
		}
		// The more intuitive approach to read the data does not work, as TID's and Data are mixed, and we don't know a priori which is which (We have to be able to move an TID aswell)
		int currentStart = indexHolder.index();
		indexHolder.read(getOffsetforTIDIndex(position.getIndex()+1), mem);
		int nextStart = indexHolder.index();
		int dataMemberSize = nextStart - currentStart;
		if (nextStart == currentStart) {
			buffer.unfix(bf, position.getBlock());
			throw new DeletedRecordException("Modify deleted TID");
		}
		int firstIndexPos = loadEnd(mem);
		int freeStart = indexHolder.index();
		// old - new
		int sizeDiff = dataMemberSize - object.size();
		if (/* old >= new */ sizeDiff >= 0) {
			moveForward(mem, nextStart - sizeDiff, firstIndexPos - nextStart ,sizeDiff);
			object.write(currentStart, mem);
			modifyTIDIndex(mem, position.getIndex(), -sizeDiff, position.getBlock());
		}
		else if (-sizeDiff < freeMemory.get(position.getBlock())) {
			moveBackward(mem, nextStart, freeStart - nextStart, -sizeDiff);
			object.write(currentStart, mem);
			modifyTIDIndex(mem, position.getIndex(), -sizeDiff ,position.getBlock());
		}
		else{
			TID tid = insertInternal(object, false);
			indexHolder.read(getOffsetforTIDIndex(position.getIndex()), mem);
			assert tid.size() <= dataMemberSize; // We know we have eanough space now (as we limit the ammout of storage to TID-Size.
			sizeDiff = dataMemberSize - tid.size();
			if (writeTID) {
				moveForward(mem, nextStart - sizeDiff, firstIndexPos - nextStart, sizeDiff);
				tid.write(currentStart, mem);
				modifyTIDIndex(mem, position.getIndex(), -sizeDiff, position.getBlock());
				indexHolder.read(getOffsetforTIDIndex(position.getIndex()), mem);
				indexHolder.setMoved(true);
			}
			else {
				sizeDiff += tid.size();
				moveForward(mem, nextStart - sizeDiff, firstIndexPos - nextStart, sizeDiff);
				modifyTIDIndex(mem, position.getIndex(), -sizeDiff, position.getBlock());
				indexHolder.read(getOffsetforTIDIndex(position.getIndex()), mem);
			}
			indexHolder.write(getOffsetforTIDIndex(position.getIndex()), mem);
			buffer.setDirty(bf, position.getBlock());
			buffer.unfix(bf, position.getBlock());
			return tid;
		}
		buffer.setDirty(bf, position.getBlock());
		buffer.unfix(bf, position.getBlock());
		return position;
	}

	// @param: tidIndex: tidIndex and all before will be untouched.
	// modifies indexHolder, updates freeMemory
	private void modifyTIDIndex(ByteBuffer mem, int tidIndex, int diff, int blockNo){
		int firstIndexPos = loadEnd(mem);
		for (int i=1; i + tidIndex < (pageSize - firstIndexPos)/indexSize; ++i){
			indexHolder.read(getOffsetforTIDIndex(tidIndex + i), mem);
			indexHolder.setIndex(indexHolder.index() + diff);
			indexHolder.write(getOffsetforTIDIndex(tidIndex + i), mem);
		}
		freeMemory.set(blockNo, freeMemory.get(blockNo) - diff);
	}

	@Override
	public void delete(TID position) throws IOException, BufferFullException{
		ByteBuffer mem = buffer.fix(bf, position.getBlock());
		try {
			indexHolder.read(pageSize - indexSize * (position.getIndex()+1), mem);
			if (indexHolder.fragmented()) {
				assert position.getIndex() == 0: "Fragmentation Header is broken";
				TID rem = new TID(0, 0);
				rem.read(0, mem);
				indexHolder.setMoved(false);
				indexHolder.setFragmented(false);
				indexHolder.write(getOffsetforTIDIndex(0), mem);
				indexHolder.setEnd(0);
				indexHolder.write(getOffsetforTIDIndex(1), mem);
				freeMemory.set(position.getBlock(), pageSize - 2*indexSize);
				delete(rem);
				buffer.setDirty(bf, position.getBlock());
				return;
			}
			if (indexHolder.moved()){
				TID tid = new TID(0, 0);
				tid.read(indexHolder.index(), mem);
				// Note: one could not burn the inner TID as it is not given to the outside.
				// If this delete fails, we did not touch anything.
				delete(tid);
				// delete does modify indexHolder
				indexHolder.read(getOffsetforTIDIndex(position.getIndex()),mem);
			}
			int currentStart = indexHolder.index();
			indexHolder.read(pageSize - indexSize * (position.getIndex()+2), mem);
			int nextStart = indexHolder.index();
			int firstIndexPos = loadEnd(mem);
			int freeStart = indexHolder.index();
			moveForward(mem, currentStart, freeStart - nextStart, nextStart - currentStart);
			modifyTIDIndex(mem, position.getIndex(), currentStart - nextStart, position.getBlock());
			buffer.setDirty(bf, position.getBlock());
		} finally {
			buffer.unfix(bf, position.getBlock());
		}
	}

	@Override
	public View<D> view() {
		return new TIDView<D>(buffer, bf, indexSize, this);
	}
}
