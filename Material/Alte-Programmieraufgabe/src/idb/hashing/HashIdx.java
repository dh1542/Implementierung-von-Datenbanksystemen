package idb.hashing;

import idb.datatypes.DataObject;
import idb.datatypes.Key;

import java.io.IOException;
import java.util.List;

/**
 * Interface for index files using hashing. It allows for multiple usage of the same key.
 * @author Christoph Merdes
 *
 * @param <K> Class for keys. K must implement the Key-interface
 * @param <V> Class for values. V must implement the DataObject-interface
 */
public interface HashIdx<K extends Key, V extends DataObject> {
	/**
	 * Add a new key-value pair to the index file.
	 * @param k The key
	 * @param v The value
	 */
	public void put(K k, V v);
	/**
	 * Retrieve all values this key is mapped to
	 * @param k The key
	 * @return List of all values the key k is mapped to. Remember that multiple mappings for the same key are allowed.
	 * Returns an empty List if the key does not exist
	 */
	public List<V> get(K k);
	/**
	 * most important: Writes the header. This is necessary
	 * for reloading the data in the next program run. Does not
	 * necessarily close any segments.
	*/
	public void close() throws IOException;
}
