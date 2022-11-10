/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TIDIndex implements DataObject {
	private int index;
	private boolean moved;
	private boolean fragmented;
	private boolean exported;
	private int size;

	// @param s size in bytes of a TIDIndex for a specific pageSize
	public TIDIndex(int s) {
		size = s;
	}
	/**
	 * Read the contents of this DataObjects from the ByteBuffer. Reverse operation to write
	 * @param index The index where to start reading. Use absolute get methods.
	 * @param bb The ByteBuffer to read from
	 */
	public void read(int ind, ByteBuffer bb) {
		// Little Endian
		int ret = 0;
		int mask = 0x1f;
		for (int i = 0; i < size; ++i){
			ret <<= 8;
			ret += bb.get(ind+i) & mask;
			mask = 0xff;
		}
		this.index = ret;
		exported = (bb.get(ind) & 0x20) != 0;
		moved = (bb.get(ind) & 0x40) != 0;
		fragmented = (bb.get(ind) & 0x80) != 0;
	}
	/**
	 * write the content of this DataObject to the ByteBuffer bb, starting at ind
	 * @param ind starting index for writing to the buffer: Use the absolute put methods of ByteBuffer
	 * @param bb The ByteBuffer to be written to
	 */
	public void write(int ind, ByteBuffer bb) {
		int tmp = index;
		for (int i = size-1; i >= 1; --i){
			bb.put(ind + i, (byte) (tmp & 0xff));
			tmp >>>= 8;
		}
		assert tmp <= 0x1f: "Something went very wrong with this index ("+index+")";
		bb.put(ind, (byte) (tmp | (moved ? 0x40 : 0) | (fragmented ? 0x80: 0) | (exported ? 0x20 : 0)));
	}
	/**
	 * Retrieve the size of this object. This means the size in byte it will take in a ByteBuffer
	 * @return The size in bytes
	 */
	public int size() {
		return size;
	}

	public DataObject copy() {
		TIDIndex ret = new TIDIndex(this.size);
		ret.index = this.index;
		ret.moved = this.moved;
		ret.fragmented = this.fragmented;
		ret.exported = this.exported;
		return ret;
	}

	public int index() {
		return index;
	};

	public boolean fragmented() {
		return fragmented;
	}

	public boolean moved() {
		return moved;
	}

	public boolean exported() {
		return exported;
	}

	public void setIndex(int newVal) {
		assert newVal >= 0;
		index = newVal;
	}

	public void setFragmented(boolean b) {
		assert !moved || !b;
		fragmented = b;
	}

	public void setMoved(boolean b) {
		assert !fragmented || !b;
		moved = b;
	}

	public void setExported(boolean b) {
		exported = b;
	}

	// @return: fragmented & moved (which is used as an end marker)
	public boolean isInvalid() {
		return fragmented && moved;
	}

	public void setEnd(int ind) {
		index = ind;
		fragmented = true;
		moved = true;
		exported = false;
	}

}
