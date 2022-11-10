package idb.datatypes;

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
	 * @param index starting index for writing to the buffer: Use the absolute put methods of ByteBuffer
	 * @param bb The ByteBuffer to be written to
	 */
	public void write(int index, ByteBuffer bb);
	/**
	 * Retrieve the size of this object. This means the size in byte it will take in a ByteBuffer
	 * @return The size in bytes
	 */
	public int size();
}
