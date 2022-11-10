/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import java.nio.ByteBuffer;
import java.util.List;

public class DOPartialWrapper implements DataObject{
	private DataObject member;
	private int usedOff;
	public DOPartialWrapper(DataObject mem, int sz) {
		member = mem;
		usedOff = sz;
	}

	@Override
	public void read(int index, ByteBuffer bb){
		throw new RuntimeException("This operation is not supported");
	}

	@Override
	public void readPart(List<Triplet<ByteBuffer, Integer, Integer>> parts/*{ByteBuffer, offsetBuf, size}[]*/){
		throw new RuntimeException("This operation is not supported");
	}

	@Override
	public void write(int index, ByteBuffer bb){
		member.writePart(index, bb, usedOff, size());
	}

	@Override
	public void writePart(int bufferOffset, ByteBuffer bb, int objectOffset, int size){
		member.writePart(bufferOffset, bb, objectOffset + usedOff, size);
	}

	@Override
	public int size(){
		return member.size() - usedOff;
	}

	@Override
	public DataObject copy(){
		return new DOSizeWrapper(member.copy(), usedOff);
	}
}
