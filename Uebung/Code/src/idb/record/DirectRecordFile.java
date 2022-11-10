/**
 * @file
 *
 * Interface for a DirectRecordFile.
 * It allows inserting, reading, modification and deletion of variable long records.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */

package idb.record;
import idb.datatypes.DataObject;
import idb.buffer.BufferFullException;

import java.io.IOException;

public interface DirectRecordFile<T, D extends DataObject>
{
	/**
	 * Inserts a record into the file.
	 * The returned T can be used to find this object for modification or deletion.
	 * T has to be stable, it is not allowed to be reused after deletion or internal movement.
	 */
	public T insert(D object) throws BufferFullException, IOException;

	/**
	 * Read a record from the file.
	 * As Java does not support new D() where D is a generic type parameter, we supply a D "to write into"
	 */
	public void read(T position, D output) throws BufferFullException, IOException, DeletedRecordException;

	/**
	 * Modifiy a record in this file.
	 * @param object used to override the old content of @param position, not as a return parameter like in @read
	 */
	public void modify(T position, D object) throws BufferFullException, IOException, DeletedRecordException;

	/**
	 * Delete a record in this file.
	 */
	public void delete(T position) throws BufferFullException, IOException;

	/**
	 * Returns a View for this directRecordFile.
	 * The view ccan be used to iterate over all inserted objects without knowledge of the relevant T.
	 * If a modification on this directRecordFile happens while the view is in use, the iteration might be
	 * incomplete or contain duplicates. However the view has to list all (new) objects (again) if
	 * one calls view.reset() after the modification.
	 */
	public View<D> view();
}
