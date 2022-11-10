/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.meta;

import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.DBString;
import idb.datatypes.Bool;
import idb.datatypes.DBInteger;
import idb.datatypes.TID;
import idb.datatypes.DataObject;

import idb.record.KeyRecordFile;
import idb.record.DirectRecordFile;

import java.util.List;

public interface Metadata{
	/**
	 * Modifies all necesary Metadata to add a new relation to this database.
	 * Note that removing an column is not possible.
	 * A Relation has to have at least one index for the primary key, this methode has to establish that too.
	 * @param primaryKey: The name for the primary key or null if this relation should use a surrogateKey.
	 * A surrogateKey will be using the name "_Surrogate".
	 * Depending on your implementation of ClockBuffer, you might not be able to use different blockSizes in one database.
	 * @throws IllegalArgumentException if columns.size() and types.size() do not match
	 * @throws IllegalArgumentException if @columns do not contain @primaryKey and @primaryKey is not null.
	 * @throws IllegalArgumentException if at least one String in @columns does contain any letter that are not a-z or A-Z
	 * @throws IllegalArgumentException if blockSize is negative
	 * @throws IllegalStateException if a relation named @name already exists
	 */
	public void addRelation(String name, List<String> columns, List<NamedCombinedRecord.Type> types, String primaryKey, int blockSize);

	/**
	 * Modifies all necesary Metadata to remove a relation from this database.
	 * This method has to delete all related files aswell.
	 * @throws NoSuchElementException if no relation named @name exists.
	 */
	public void dropRelation(String name);

	/**
	 * Extracts if a relation named @name exists in this database.
	 *
	 */
	public boolean hasRelation(String name);

	/**
	 * Modifies all necesary Metadata to add a new Column to an existing relation.
	 * Also, this methode modifies all files related to @relName to contains defaultValue as entry for colName
	 * Note that removing an column for an existing relation is not possible.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws IllegalArgumentException if a column named @colName already existed.
	 */
	public void addIntColumn(String relName, String colName, int defaultValue);

	/**
	 * Modifies all necesary Metadata to add a new Column to an existing relation.
	 * Also, this methode modifies all files related to @relName to contains defaultValue as entry for colName
	 * Note that removing an column for an existing relation is not possible.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws IllegalArgumentException if a column named @colName already existed.
	 */
	public void addStringColumn(String relName, String colName, String defaultValue);

	/**
	 * Modifies all necesary Metadata to add a new Column to an existing relation.
	 * Also, this methode modifies all files related to @relName to contains defaultValue as entry for colName
	 * Note that removing an column for an existing relation is not possible.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws IllegalArgumentException if a column named @colName already existed.
	 */
	public void addBoolColumn(String relName, String colName, boolean defaultValue);

	/**
	 * Returns the type for the attribute colName of relName.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no column named @colName exists in @relName.
	 *
	 */
	public NamedCombinedRecord.Type getType(String relName, String colName);


	/**
	 * Returns if this database has an index for relation @relName and columns @cols
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no cols contains a String s where no column named s exists in @relName.
	 *
	 */
	public boolean hasIndex(String relName, String cols);

	/**
	 * Returns a KeyRecordFile containing an index for the attribute colName of relName.
	 * This KeyRecordFile is only to be used for queries, and modifieing it results in undefined behaviour.
	 * Deleting an already deleted TID for cleanup-purposes is ok.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no column named @colName exists in @relName.
	 * @throws NoSuchElementException if there is no index for @relName and @colName.
	 * @throws IllegalStateException if getType(relName, colname) != STRING
	 *
	 */
	public KeyRecordFile<TID, DBString> getStringIndex(String relName, String colName);

	/**
	 * Returns a KeyRecordFile containing an index for the attribute colName of relName.
	 * This KeyRecordFile is only to be used for queries, and modifieing it results in undefined behaviour.
	 * Deleting an already deleted TID for cleanup-purposes is ok.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no column named @colName exists in @relName.
	 * @throws NoSuchElementException if there is no index for @relName and @colName.
	 * @throws IllegalStateException if getType(relName, colname) != INT
	 *
	 */
	public KeyRecordFile<TID, DBInteger> getIntIndex(String relName, String colName);

	/**
	 * Returns a KeyRecordFile containing an index for the attribute colName of relName.
	 * This KeyRecordFile is only to be used for queries, and modifieing it results in undefined behaviour.
	 * Deleting an already deleted TID for cleanup-purposes is ok.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no column named @colName exists in @relName.
	 * @throws NoSuchElementException if there is no index for @relName and @colName.
	 * @throws IllegalStateException if getType(relName, colname) != Bool
	 *
	 */
	public KeyRecordFile<TID, Bool> getBoolIndex(String relName, String colName);

	/**
	 * Returns a list of all columns for a given relation. The list is not ordered.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 *
	 */
	public List<String> getColumns(String relName);

	/**
	 * Returns a the name of the primary Key for a given relation.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 *
	 */
	public String getPrimaryKey(String relName);

	/**
	 * Returns a NamedCombinedRecord for reading the TID file related to relName
	 * @throws NoSuchElementException if no relation named @relName exists.
	 */
	public NamedCombinedRecord getNCR(String relName);

	/**
	 * Returns a NamedCombinedRecord for reading the TID file related to relName.
	 * Inserting into this TID-File is dissallowed, as well as modify.
	 * Deletion may be used.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 */
	public DirectRecordFile<TID, NamedCombinedRecord> getTIDFile(String relName);

	/**
	 * Creates an index for relation relName, based on the column colName.
	 * This method has to modify all metadata, aswell as setting up the index itself and filling it with all data.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws NoSuchElementException if no column named @colName exists in @relName.
	 * @throws IllegalStateException if this index already exists
	 */
	public void createIndex(String relName, String colName);

	/**
	 * Insert a set of values into the relation relName.
	 * This method has to find all related indices and update them aswell as inserting the data.
	 * Note that the supplied data does not contain the value for the surrogate-key, if there is one.
	 * Also note that while this uses a List to keep the order between NamedCombinedRecord and columns, the user
	 * does not have to supply a specific order (like: tupel-order). The Metadata has to reorder the entries when combining the NCRs.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 * @throws IllegalArgumentException if columns.size() and values.size() do not match
	 * @throws IllegalArgumentException if columns.size() and getColumns(relName).size() do not match and there is no surrogate-key
	 * @throws IllegalArgumentException if columns.size() and getColumns(relName).size()-1 do not match and there is a surrogate-key
	 * @throws IllegalArgumentException if columns contains "_Surrogate"
	 * @throws NoSuchElementException if at least one column from @relName does not exists in @columns.
	 * @throws NoSuchElementException if for a n values.get(n).getType(columns.get(n)) throws NoSuchElementException
	 * @throws IllegalStateException if this primary-Key is already used.
	 * @throws IllegalStateException Type-Mismatch: if for a n valees.get(n).getType() != getType(relName, columns.get(n);
	 */
	public void insert(String relName, List<String> columns, List<NamedCombinedRecord> values);

	/**
	 * Extracts if a column named @colName in relation @relName exists in this database.
	 * @throws NoSuchElementException if no relation named @relName exists.
	 *
	 */
	public default boolean hasColumn(String relName, String colName) {
		return getColumns(relName).stream().anyMatch(colName::equals);
	}
}
