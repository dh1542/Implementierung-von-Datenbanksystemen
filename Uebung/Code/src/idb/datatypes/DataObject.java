package idb.datatypes;

import java.util.List;
import java.nio.ByteBuffer;
/**
 * This is the base interface for objects to be stored by the database.
 * It provides methods needed for serialization to a ByteBuffer
 * @author Christoph Merdes
 *
 */
public interface DataObject{
	/**
	 * Read the contents of this DataObjects from the ByteBuffer. Reverse operation to write
	 * @param index The index where to start reading. Use absolute get methods.
	 * @param bb The ByteBuffer to read from
	 */
	public void read(int index, ByteBuffer bb);
	/**
	 * write the content of this DataObject to the ByteBuffer bb, starting at index
	 * Note that for variable length DataObjects to work, the written data might contain some additional legth information
	 * which has to be written when this methode is called, aswell (and that has to be accounted for in size())
	 * @param index starting index for writing to the buffer: Use the absolute put methods of ByteBuffer
	 * @param bb The ByteBuffer to be written to
	 */
	public void write(int index, ByteBuffer bb);
	/**
	 *
	 * Retrieve the size of this object. This means the size in byte it will take in a ByteBuffer
	 * Note that this might be content dependend and therefore change when read is called.
	 * @return The size in bytes
	 */
	public int size();

	/**
	 *
	 * Copy this DataObject.
	 * The copy has to be a new object that returns the same size() as the original this object.
	 * It also has to be fully functional for calls to read or write.
	 * Note that it does not need to be the exact same class as this, it is sufficiant to be any DataObject that complies with the previous explenation.
	 */
	public DataObject copy();
	/**
	 * Read the contents of this DataObjects from the ByteBuffer, using the current index of the bytebuffer. Reverse operation to write
	 * Note that this - in contrast to the functions on byteBuffer itself - does not modify bb.position()
	 * @param bb The ByteBuffer to read from
	 */
	public default void read(ByteBuffer bb) {
		read(bb.position(), bb);
	}
	/**
	 * write the content of this DataObject to the ByteBuffer bb, starting at the current index of the bytebuffer.
	 * Note that this - in contrast to the functions on byteBuffer itself - does not modify bb.position()
	 * @param bb The ByteBuffer to be written to
	 */
	public default void write(ByteBuffer bb) {
		write(bb.position(), bb);
	}

	/**
	 * Writes part of the content of this DataObject to Bytebuffer bb, starting at index bufferOffset.
	 * Only writes size bytes, starting at objectOffset.
	 * Should only be used for writing fragmented records.
	 * Does not modify bb.position to be similar to all other methods on this interface.
	 */
	public default void writePart(int bufferOffset, ByteBuffer bb, int objectOffset, int size){
		ByteBuffer bbNew = ByteBuffer.allocate(size());
		write(bbNew);
		int oldPos = bb.position();
		bb.position(bufferOffset).put(bbNew.array(), objectOffset, size);
		bb.position(oldPos);
	}

	/**
	 * Reads the contents of this DataObject from the ByteBuffer.
	 * This function is used to retrieve fragmented records, where multiple buffer have to be read in order to get the whole information
	 * @param parts The list of Triplets used to combine all parts into a unfragmented piece of data.
	 * The first member of the triplet is the buffer that contains the information, the first Integer describes where to start reading in this buffer
	 * And the last Integer describes how many bytes are related to this DataObject.
	 */
	public default void readPart(List<Triplet<ByteBuffer, Integer, Integer>> parts/*{ByteBuffer, offsetBuf, size}[]*/){
		int newSize = 0;
		for (Triplet<ByteBuffer, Integer, Integer> t : parts) {
			assert t.third() > 0: "Parts need to be larger than 0 and this part is "+t.third();
			newSize += t.third();
		}
		ByteBuffer buffer = ByteBuffer.allocate(newSize);
		// fill the buffer.
		for(Triplet<ByteBuffer, Integer, Integer> tri: parts) {
			ByteBuffer buf = tri.first();
			int bufferOffset = tri.second();
			int size = tri.third();
			buffer.put(buf.array(), bufferOffset, size);
		}
		read(0, buffer);
		assert newSize == size();
	}

	/**
	 * Copies the contents of this DataObjects into other.
	 * This methode is equivalent to calling this.write(0, bb); other.read(0, bb);
	 * except that it uses a correctly sized fresh buffer.
	 */
	public default void transfer(DataObject other) {
		ByteBuffer buf = ByteBuffer.allocate(size());
		write(0, buf);
		other.read(0, buf);
	}
}

