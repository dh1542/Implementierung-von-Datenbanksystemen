/**
 * @file
 *
 * Interface for a KeyRecordFile.
 * It allows inserting, reading, modification and deletion of variable long records.
 * Compared to @DirectRecordFile, this file allows to select the key instead of using a internal struct.
 * Additionally, this file does not require handleling of records longer than the blockSize.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */
package idb.record;

import idb.datatypes.DataObject;
import idb.datatypes.Key;
import java.util.List;
import idb.buffer.BufferFullException;

import java.io.IOException;

public interface KeyRecordFile<D extends DataObject, K extends Key>
{
	/**
	 * Add a new key-value pair to the index file.
	 * It is guarantied that the following list of instructions does not result in any observable change to this KeyRecordFile:
	 * int sz = size(key, object);
	 * insert(key, object);
	 * delete(key, sz, object);
	 *
	 * Colloquial speeking it is guaranteed that the inserted object will be the one with the highest index for this key,
	 * and no indices for any key are modified during an insertion.
	 * @param object The value
	 * @param k The key
	 */
	public void insert(K key, D object) throws BufferFullException, IOException;
	/**
	 * Retrieve a values this key is mapped to
	 * @param k The key
	 * @param out The object to write to
	 * @param idx The idx to specify which key-value pair should be read
	 * @throws ArrayIndexOutOfBoundsException if idx >= size(position)
	 */
	public void read(K position, D out, int idx) throws BufferFullException, IOException;
	/**
	 * Returns the number of key-value pairs that contain the key position.
	 * @param position The key
	 * @param tmp A temporary storage space for one value.
	 * @return 0 if there is no mapping for this key, #mappings otherwise
	 */
	public int size(K position, D tmp) throws IOException, BufferFullException;
	/**
	 * Deletes a specific Key-Value pair.
	 * Note that this function might change the order of all pairs, therefore reusing an index after a
	 * modification is an error, except in the case stated in @insert.
	 *
	 * Colloquial speeking it is guarateed not to result in any modified indices if you remove the last element for one specific key.
	 * @param tmp A temporary storage space for one value.
	 * @throws ArrayIndexOutOfBoundsException if idx >= size(position)
	 */
	public void delete(K position, int idx, D tmp) throws IOException, BufferFullException;

	/**
	 * Informes this KeyRecordFile to save all information for imminent desctruction.
	 * This method does not close the buffer or the blockFiles.
	 * Its purpose is to save all relevant meta-information regarding the state of this recordFile,
	 * so it can be rebuild from disk. Using an KeyRecordFile after calling .close() is undefined behaviour.
	 */
	public void close() throws IOException, BufferFullException;

	/**
	 * Modifies a specific Key-Value pair.
	 * Note that this function might change the order of all pairs, therefore reusing an index after a
	 * modification is an error.
	 * @param tmp A temporary storage space for one value.
	 * @throws ArrayIndexOutOfBoundsException if idx >= size(position)
	 */
	public default void modify(K position, int idx, D object) throws IOException, BufferFullException {
		insert(position, object);
		delete(position, idx, object);
	}
	/**
	 * Remappes all content previously mapped to oldKey to newKey.
	 * Note that this function might change the order of all pairs, therefore reusing an index after a
	 * modification is an error.
	 * @param tmp A temporary storage space for one value. This is needed to move the data.
	 */
	public default void modifyKey(K oldKey, K newKey, D tmp) throws BufferFullException, IOException {
		assert !oldKey.equals(newKey);
		for(;size(oldKey, tmp) > 0;){
			read(oldKey, tmp, 0);
			insert(newKey, tmp);
			delete(oldKey, 0, tmp); // Note that this reuse of index 0 is only correct due to the specification for insert
		}
	}
	/**
	 * Delete all values this key is mapped to.
	 * Note that this function might change the order of all pairs, therefore reusing an index after a
	 * modification is an error.
	 * @param position The key
	 * @param tmp A temporary storage space for one value.
	 */
	public default void delete(K position, D tmp) throws IOException, BufferFullException{
		for(;size(position, tmp) > 0;){
			delete(position, 0, tmp);
		}
	}
}
