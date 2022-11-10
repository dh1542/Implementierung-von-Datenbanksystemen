/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import java.nio.ByteBuffer;

public class DOSizeWrapper implements DataObject{
	private DataObject member;
	private int minSize;
	public DOSizeWrapper(DataObject mem, int sz) {
		member = mem;
		minSize = sz;
	}

	@Override
	public void read(int index, ByteBuffer bb){
		member.read(index, bb);
	}

	@Override
	public void write(int index, ByteBuffer bb){
		member.write(index, bb);
	}

	@Override
	public int size(){
		int ret = member.size();
		if (ret < minSize) return minSize;
		return ret;
	}

	@Override
	public DataObject copy(){
		return new DOSizeWrapper(member.copy(), minSize);
	}
}
