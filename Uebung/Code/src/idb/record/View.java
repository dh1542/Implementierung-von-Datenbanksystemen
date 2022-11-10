/**
 * @file
 *
 * Interface for a View into an existing file.
 * It allows reading all elements sequentially. The order is not defined.
 * Compared to @SeqRecordFile, this interface does not support any modification, its only use is pure inspection.
 * Also, the order of elements is not stable and cannot be used for anything.
 * Concurrent modifications in the underlying datastruct during the iteration may result in arbitrary unexpected results.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */
package idb.record;

import idb.datatypes.DataObject;

import idb.buffer.BufferFullException;
import java.io.IOException;
public interface View<D extends DataObject> {
	/**
	 * Restarts the iteration.
	 */
	public void restart();

	/**
	 * Returns @true if the operation successfully wrote the next element to @out, @false if no next element was found
	 */
	public boolean next(D out) throws IOException, BufferFullException;

	/*
	 * A view does not allow any modifications, therefore it does not need a close()
	 */
}
