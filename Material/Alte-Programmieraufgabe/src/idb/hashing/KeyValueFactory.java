package idb.hashing;

import idb.datatypes.DataObject;
import idb.datatypes.Key;
/**
 * This factory is used by the Implementation of HashIdx to generate the
 * right objects.
 * @author Christoph Merdes
 *
 * @param <K> The Class for the keys, must implement Key
 * @param <D> The class for the values, must implement DataObject
 */
public interface KeyValueFactory<K extends Key, D extends DataObject>{
	/**
	 * Generate a key object
	 * @return The Key generated
	 */
	public K createKey();
	/**
	 * Generate a value object.
	 * @return The DataObject generated
	 */
	public D createDataObject();
}
