/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.record;

import idb.datatypes.DataObject;
import idb.datatypes.Key;
import idb.datatypes.Triplet;
import idb.buffer.DBBuffer;
import idb.block.BlockFile;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import idb.buffer.BufferFullException;

import java.io.IOException;

public class HashImpl<D extends DataObject, K extends Key> implements KeyRecordFile<D, K>
{
	private int position;
	private int size;
	private DBBuffer buf;
	private BlockFile regular;
	private BlockFile overflow;
	private long freeBytes;
	private double threshhold;

	private HashImpl(BlockFile r, BlockFile o, DBBuffer bu){
		buf = bu;
		overflow = o;
		regular = r;
	}

	/**
	 * creates a new HashImpl. Not suiteable to reconstruct an already existing HashImpl from storage. use @reconstruct for this purpose.
	 * Assumes both r and o point to already opened fresh files of size 0
	 */
	public HashImpl(BlockFile r, BlockFile o, DBBuffer bu, double thresh, int initCapacity) throws IOException, BufferFullException{
		this(r, o, bu);
		position = 0;
		size = initCapacity;
		freeBytes = initCapacity * (r.bytes() - getHeaderSize());
		threshhold = thresh;
		overflow.append(1); // metadataBlock
		ByteBuffer bb = buf.fix(overflow, getInvalidPointer());
		writeOffsetPointer(bb, getInvalidPointer()); // No free memory available
		buf.setDirty(overflow, getInvalidPointer());
		buf.unfix(overflow, getInvalidPointer());
		writeHeader();
		ByteBuffer empty = ByteBuffer.allocate(r.bytes());
		writeOffsetPointer(empty, getInvalidPointer()); // End of chain
		writeFreeMemory(empty, r.bytes() - getHeaderSize());
		for(int i=0; i < initCapacity; ++i){
			regular.write(i, empty); // Directly writing to regular is ok, as we know this page is not in the buffer (we got assuered that regular is of size 0)
		}
	}

	public static <D extends DataObject, K extends Key> HashImpl<D, K> reconstruct(BlockFile regular, BlockFile overflow, DBBuffer buf){
		HashImpl<D, K> out = new HashImpl<>(regular, overflow, buf);
		try {
			out.readHeader();
		}
		catch(IOException | BufferFullException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		}
		return out;
	}

	private int hash(int code) {
		int res = Math.abs(code % size);
		if (res < position) {
			res = Math.abs(code % (2*size));
		}
		return res;
	}
	@Override
	public void insert(K key, D object) throws IOException, BufferFullException {
		int pos = hash(key.hashCode());
		int size = object.size() + key.size();
		assert buf.getPagesize() - getHeaderSize() >= size: "This data is too large for an Hash-Impl";
		ByteBuffer bb = buf.fix(regular, pos);
		int cur = pos;
		BlockFile curFile = regular;
		for (int next = getOffsetPointer(bb); next != getInvalidPointer(); next = getOffsetPointer(bb)) { // Note: Squeezing data in is against the spec
			buf.unfix(curFile, cur);
			cur = next;
			curFile = overflow;
			bb = buf.fix(curFile, cur);
		}
		if (getFreeMemory(bb) < size) {
			int next = alloc();
			writeOffsetPointer(bb, next);
			buf.setDirty(curFile, cur);
			buf.unfix(curFile, cur);
			cur = next;
			curFile = overflow;
			bb = buf.fix(curFile, cur);
			writeOffsetPointer(bb, getInvalidPointer());
			writeFreeMemory(bb, buf.getPagesize() - getHeaderSize());
			buf.setDirty(curFile, cur);
		}
		int inBlockPos = startOffset(bb);
		int freeMem = getFreeMemory(bb);
		assert freeMem >= size;
		writeFreeMemory(bb, freeMem - size);
		key.write(inBlockPos, bb);
		object.write(inBlockPos + key.size(), bb);
		buf.setDirty(curFile, cur);
		buf.unfix(curFile, cur);
		freeBytes -= size;
		if (freeBytes < (1-threshhold) * regular.size() * (regular.bytes() - getHeaderSize())) {
			// split
			split(key, object);
		}
	}

	@Override
	public void close() throws IOException, BufferFullException{
		writeHeader();
	}

	private void writeHeader() throws IOException, BufferFullException {
		ByteBuffer bb = buf.fix(overflow, getInvalidPointer());
		int idx = Integer.SIZE / Byte.SIZE; // we need to skip the offsetPointer that is used for the freeList.
		bb.position(idx);
		bb.putInt(position);
		bb.putInt(size);
		bb.putLong(freeBytes);
		bb.putDouble(threshhold);
		buf.setDirty(overflow, getInvalidPointer());
		buf.unfix(overflow, getInvalidPointer());
	}

	private void readHeader() throws IOException, BufferFullException {
		ByteBuffer bb = buf.fix(overflow, getInvalidPointer());
		int idx = Integer.SIZE / Byte.SIZE; // we need to skip the offsetPointer that is used for the freeList.
		bb.position(idx);
		position = bb.getInt();
		size = bb.getInt();
		freeBytes = bb.getLong();
		threshhold = bb.getDouble();
		buf.unfix(overflow, getInvalidPointer());
	}

	private void split(K toCopyK, D toCopyD) throws IOException, BufferFullException{
		int toSplit = position++;
		if (position == size) {
			position = 0;
			size = 2* size;
		}
		int regSize = regular.size();
		regular.append(1);
		ByteBuffer newBlock = buf.fix(regular, regSize);
		ByteBuffer oldBlock = buf.fix(regular, toSplit);
		initializeData(newBlock);
		buf.setDirty(regular, regSize);
		long fB = freeBytes;
		freeBytes = Long.MAX_VALUE; // Note: Double-split cannot happen in this context, even if the threshhold still does not fit. We split after the next insertion.
		// Keep these blocks fixxed during reorganisation
		// First we "free" toCopyK and toCopyD of there content, by transfering copieing it and transfer it back before finishing
		Key keyCopy = toCopyK.copy();
		DataObject doCopy = toCopyD.copy();
		{
			int cur = toSplit;
			BlockFile curBF = regular;
			ByteBuffer curBB = buf.fix(curBF, cur);
			int follow = getOffsetPointer(curBB);
			do {
				List<Triplet<Key, DataObject, Integer>> allData = listBlockContent(curBB, toCopyK, toCopyD);
				if (curBF == overflow) {
					free(cur);
				}
				else {
					initializeData(curBB);
					buf.setDirty(curBF, cur);
				}
				for (Triplet<Key, DataObject, Integer> t : allData) {
					t.first().transfer(toCopyK);
					t.second().transfer(toCopyD);
					insert(toCopyK, toCopyD);
				}
				buf.unfix(curBF, cur);
				curBF = overflow;
				cur = follow;
				if (cur == getInvalidPointer()) break;
				curBB = buf.fix(curBF, cur);
				follow = getOffsetPointer(curBB);
			}
			while(true);
		}
		keyCopy.transfer(toCopyK);
		doCopy.transfer(toCopyD);
		freeBytes = fB + this.buf.getPagesize() - getHeaderSize();
		buf.unfix(regular, toSplit);
		buf.unfix(regular, regSize);
	}

	private static int getOffsetPointer(ByteBuffer buf) {
		int res = buf.getInt(0);
		return res;
	}
	private static int getFreeMemory(ByteBuffer buf) {
		int res = buf.getInt(Integer.SIZE / Byte.SIZE);
		return res;
	}
	private static int getHeaderSize() {
		return 2*Integer.SIZE/Byte.SIZE;
	}
	private static int getInvalidPointer() {
		return 0;
	}
	private static void writeOffsetPointer(ByteBuffer buf, int nxt) {
		buf.putInt(0, nxt);
	}
	private static void writeFreeMemory(ByteBuffer buf, int free) {
		buf.putInt(Integer.SIZE / Byte.SIZE, free);
	}
	private void initializeData(ByteBuffer buf) {
		writeOffsetPointer(buf, getInvalidPointer());
		writeFreeMemory(buf, this.buf.getPagesize() - getHeaderSize());
	}
	private int startOffset(ByteBuffer buf) {
		return this.buf.getPagesize() - getFreeMemory(buf);
	}
	private int alloc() throws IOException, BufferFullException{
		ByteBuffer bb = buf.fix(overflow, getInvalidPointer());
		int offset = getOffsetPointer(bb);
		if (offset != getInvalidPointer()) {
			// We have found a free block. get the following block and insert it into the current one.
			ByteBuffer bf = buf.fix(overflow, offset);
			int following = getOffsetPointer(bf);
			buf.unfix(overflow, offset);
			writeOffsetPointer(bb, following);
			buf.setDirty(overflow, getInvalidPointer());
		}
		else {
			offset = overflow.size();
			overflow.append(1);
		}
		buf.unfix(overflow, getInvalidPointer());
		return offset;
	}
	private void free(int block) throws IOException, BufferFullException{
		assert block != getInvalidPointer();
		ByteBuffer head = buf.fix(overflow, getInvalidPointer());
		int offset = getOffsetPointer(head);
		ByteBuffer cur = buf.fix(overflow, block);
		writeOffsetPointer(cur, offset);
		buf.setDirty(overflow, block);
		buf.unfix(overflow, block);
		writeOffsetPointer(head, block);
		buf.setDirty(overflow, getInvalidPointer());
		buf.unfix(overflow, getInvalidPointer());
	}
	private List<Triplet<Key, DataObject, Integer>> listBlockContent(ByteBuffer buf, K toCopyK, D toCopyD) {
		List<Triplet<Key, DataObject, Integer>> ret = new ArrayList<>();
		Key k = toCopyK.copy();
		DataObject d = toCopyD.copy();
		for (int offset = getHeaderSize(); offset != startOffset(buf); offset += k.size() + d.size()) {
			assert offset < startOffset(buf);
			k.read(offset, buf);
			d.read(offset + k.size(), buf);
			ret.add(new Triplet<>(k.copy(), d.copy(), offset));
		}
		return ret;
	}
	private List<DataObject> matchingBlockContent(ByteBuffer bb, K key, D toCopyD) {
		return listBlockContent(bb, key, toCopyD).stream().filter(t -> t.first().equals(key)).map(t->t.second()).collect(Collectors.toList());
	}

	@Override
	public void read(K position, D out, int idx) throws IOException, BufferFullException{
		int pos = hash(position.hashCode());
		ByteBuffer bb = buf.fix(regular, pos);
		int cur = pos;
		BlockFile curFile = regular;
		do{
			List<DataObject> contents = matchingBlockContent(bb, position, out);
			if (contents.size() > idx) {
				contents.get(idx).transfer(out);
				buf.unfix(curFile, cur);
				return;
			}
			idx -= contents.size();
			int nxt = getOffsetPointer(bb);
			buf.unfix(curFile, cur);
			cur = nxt;
			curFile = overflow;
			if (cur == getInvalidPointer()) {
				throw new ArrayIndexOutOfBoundsException();
			}
			bb = buf.fix(curFile, cur);
		}
		while(idx >= 0);
		throw new RuntimeException("This is unreachable");
	}
	@Override
	public void delete(K position, int idx, D tmp) throws IOException, BufferFullException
	{
		int pos = hash(position.hashCode());
		ByteBuffer bb = buf.fix(regular, pos);
		int cur = pos;
		BlockFile curFile = regular;
		do{
			List<Triplet<Key, DataObject, Integer>> contents = listBlockContent(bb, position, tmp).stream().filter(t->t.first().equals(position)).collect(Collectors.toList());
			if (contents.size() > idx) {
				Key k = position.copy();
				DataObject d = tmp;
				int offset = contents.get(idx).third();
				k.read(offset, bb);
				d.read(offset+k.size(), bb);
				int size = k.size()+d.size();
				int end = startOffset(bb);
				while(offset + size < end) {
					k.read(offset + size, bb);
					d.read(offset + k.size()+size, bb);
					int add = k.size() + d.size();
					k.write(offset, bb);
					d.write(offset + k.size(), bb);
					offset += add;
				}
				writeFreeMemory(bb, getFreeMemory(bb) + size);
				freeBytes += size;
				buf.setDirty(curFile, cur);
				buf.unfix(curFile, cur);
				return;
			}
			idx -= contents.size();
			int nxt = getOffsetPointer(bb);
			buf.unfix(curFile, cur);
			cur = nxt;
			curFile = overflow;
			if (cur == getInvalidPointer()) {
				throw new ArrayIndexOutOfBoundsException();
			}
			bb = buf.fix(curFile, cur);
		}
		while(idx >= 0);
		throw new RuntimeException("This is unreachable");
	}
	@Override
	public int size(K position, D tmp) throws IOException, BufferFullException{
		int pos = hash(position.hashCode());
		ByteBuffer bb = buf.fix(regular, pos);
		int cur = pos;
		BlockFile curFile = regular;
		int out = 0;
		do{
			List<DataObject> contents = matchingBlockContent(bb, position, tmp);
			out += contents.size();
			int nxt = getOffsetPointer(bb);
			buf.unfix(curFile, cur);
			cur = nxt;
			curFile = overflow;
			if (cur == getInvalidPointer()) {
				return out;
			}
			bb = buf.fix(curFile, cur);
		}
		while(true);
	}
}
