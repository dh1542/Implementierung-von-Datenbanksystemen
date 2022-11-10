/**
 * @file
 *
 * Interface for a sequential Record File.
 * It allows reading all elements sequentially. The order is the insertion order.
 * Compared to @DirectRecordFile, this interface does not support any modification apart from inserting at the end.
 * It does support records larger than a blockSize
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */
package idb.record;
import idb.datatypes.DataObject;

import idb.buffer.BufferFullException;
import java.io.IOException;

public interface SeqRecordFile<D extends DataObject> {
	/**
	 * inserts one DataObject at the end of this file.
	 */
	public void insert(D object)throws IOException, BufferFullException;

	/**
	 * Returns a view into this file.
	 * Contrary to the definition in @View, this view is required to return the Objects **in insertion order**
	 */
	public View<D> view();

	/**
	 * Prepares this SeqRecordFile for destruction.
	 * As in @KeyRecordFile, this can be used for writing some header-data.
	 * Does not close any Buffer / BlockFile.
	 */
	public void close()throws IOException, BufferFullException;
}
