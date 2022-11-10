package idb.datatypes;
/**
 * The base interface for keys in this database.
 * @author Christoph Merdes
 *
 */
public interface Key extends DataObject {
	/**
	 * compare this key with another
	 * @param k The key, which this is to be compared with
	 * @return True, if the Keys are equal
	 */
	public boolean equals(Key k);
	/**
	 * compute the hashvalue of this key
	 * @return The hash value
	 */
	public int hashCode();
}
