package idb.datatypes;

import java.util.function.Predicate;
import java.util.function.Function;

/**
 * @file
 *
 * Interface for a NamedCombinedRecord.
 * It allows extraction and combination of NamedCombinedRecords using column names.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */
public interface NamedCombinedRecord extends DataObject {
	public static enum Type {
		BOOL, STRING, INT;
	}
	/**
	 * Produce a new NamedCombinedRecord that contains all elements of this and other.
	 * One can safely assume that instanceof(other) == instanceof(this), combining different implementations
	 * of NamedCombinedRecord should not happen.
	 * As a NamedComibinedRecord defines a layout, the order for serialisation of the returned object has to be
	 * the order of this, followed by the order of other.
	 * @throws IllegalArgumentException if at least one name is used in this and other.
	 * @return a newly created NamedCombinedRecord, containing all elements of this and other.
	 */
	public NamedCombinedRecord combine(NamedCombinedRecord other);

	/**
	 * Produce a new NamedCombinedRecord that contains all elements of this,
	 * but modifies all values that appear in other to the value in other.
	 * One can safely assume that instanceof(other) == instanceof(this), combining different implementations
	 * of NamedCombinedRecord should not happen.
	 * As a NamedComibinedRecord defines a layout, the order for serialisation of the returned object has to be
	 * the order of this.
	 * @throws NoSuchElementException if at least one name in other is not already existing in this.
	 * @throws IllegalStateException if at least one name in other has a different type than in this
	 * @return a newly created NamedCombinedRecord, containing the order of this, but modified each value to other if it is present in other.
	 */
	public NamedCombinedRecord modifyValues(NamedCombinedRecord other);

	/**
	 * Produce a new NamedCombinedRecord that contains only elements where the name fulfills
	 * the predicate.
	 * @throws NoSuchElementException if no name fullfilled the predicate
	 * @return a newly created NamedCombinedRecord, containing all element whose name fulfilles the predicate.
	 */
	public NamedCombinedRecord get(Predicate<String> keep);

	/**
	 * Returns the type for a given name
	 * @throws NoSuchElementException if no name is equal to @name
	 */
	public Type getType(String name);

	/**
	 * Returns the entry for a given name converted as String
	 * @throws NoSuchElementException if no name is equal to @name
	 * @throws IllegalStateException if getType(name) != STRING
	 */
	public String getString(String name);

	/**
	 * Returns the entry for a given name converted as Integer
	 * @throws NoSuchElementException if no name is equal to @name
	 * @throws IllegalStateException if getType(name) != INT
	 */
	public int getInt(String name);

	/**
	 * Returns the entry for a given name converted as boolean
	 * @throws NoSuchElementException if no name is equal to @name
	 * @throws IllegalStateException if getType(name) != BOOL
	 */
	public boolean getBool(String name);

	/**
	 * Returns a new entry where all names of this entry are substituted by calling mapping.apply(oldName).
	 * As rename is used for copy(), the order of elements might not change.
	 */
	public NamedCombinedRecord rename(Function<String, String> mapping);

	/**
	 * Produce a new NamedCombinedRecord that contains only elements where the name fulfills
	 * the predicate.
	 * @throws NoSuchElementException if no name fullfilled the predicate
	 * @return a newly created NamedCombinedRecord, containing all element whose name fulfilles the predicate.
	 */
	public default NamedCombinedRecord get(String name) {
		return get(name::equals);
	}

	@Override
	public default NamedCombinedRecord copy() {
		return rename(x->x);
	}
}
