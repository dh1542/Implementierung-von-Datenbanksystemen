/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.construct;

import java.lang.reflect.Constructor;

import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.record.DirectRecordFile;
import idb.record.KeyRecordFile;
import idb.record.SeqRecordFile;
import idb.record.View;
import idb.module.Module;
import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.Triplet;
import idb.datatypes.DataObject;
import idb.datatypes.Key;
import idb.datatypes.TID;
import idb.datatypes.Bool;
import idb.datatypes.DBString;
import idb.datatypes.DBInteger;

import idb.meta.Metadata;
import idb.meta.FileCache;


import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;


public class Util{
	public static BlockFile generateBlockFile(int blockSize) {
		return null;
	}

	public static DBBuffer generateSimpleBuffer(int pageSize) {
		return new idb.buffer.SimpleDBBuffer(pageSize);
	}

	public static <V extends DataObject> DirectRecordFile<idb.datatypes.TID, V> generateTID(DBBuffer buf, BlockFile bf) {
		return null;
	}

	public static DBBuffer generateClockBuffer(int pageSize, int capacity) {
		return null;
	}

	public static <V extends DataObject, K extends Key> KeyRecordFile<V, K> rebuildHash(DBBuffer buf, BlockFile regular, BlockFile overflow) {
		return null;
	}

	public static <V extends DataObject, K extends Key> KeyRecordFile<V, K> generateHash(DBBuffer buf, BlockFile regular, BlockFile overflow, double threshhold, int initCapacity) {
		return null;
	}

	public static <D extends DataObject> SeqRecordFile<D> generateSeqRecordFile(DBBuffer dbbuf, BlockFile bf) {
		return null;
	}

	public static <D extends DataObject> SeqRecordFile<D> rebuildSeqRecordFile(DBBuffer dbbuf, BlockFile bf) {
		return null;
	}


	public static Module generateProj(Module prev, String ...columns){
		return null;
	}
	public static Module generateRename(Module prev, Function<String, String> renameFunc){
		return null;
	}
	public static Module generateSel(Module prev, Predicate<NamedCombinedRecord> pred) {
		return null;
	}
	public static Module generateCross(Module left, Module right, String leftS, String rightS) {
		return null;
	}
	public static Module generateOrder(Module prev, boolean asc, String ...columns){
		return null;
	}
	public static Module generateGroup(Module p, List<Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>>> funcs, String ...var) {
		return null;
	}
	public static Module generateGroup(Module p, String ...var){
		return generateGroup(p, List.of(), var);
	}

	public static NamedCombinedRecord getNCR(List<NamedCombinedRecord.Type> types, List<String> names) {
		return null;
	}

	public static NamedCombinedRecord namedCombinedRecordFrom(int i, String name) {
		return null;
	}
	public static NamedCombinedRecord namedCombinedRecordFrom(boolean i, String name) {
		return null;
	}
	public static NamedCombinedRecord namedCombinedRecordFrom(String value, String name) {
		return null;
	}
	public static Module generateTableScan(View<NamedCombinedRecord> view, NamedCombinedRecord record){
		return null;
	}
	public static Module generateGen(Module m, Function<NamedCombinedRecord, NamedCombinedRecord> generator){
		return null;
	}
	public static Module generateStore(Module m, DBBuffer buf) {
		return null;
	}

	/**
	 * Delete all tupel that are produced in module from tidFile.
	 * In order to get the TID needed to call delete, one shall use the indexFile.
	 * Every NamedCombinedRecord produced by @m needs to contain a column named @indexName that is used to index into @indexFile
	 * Note that the caller can rely on indexFile beeing an index on a unique attribute (typically primary-key),
	 * therefore indexFile.size(getKey.apply(m.pull()))) can be assumed to be 1.
	 * It is up to the implementation to clean up the KeyRecordFile (although it is highly recomended).
	 * The implementation has to close the Module @m after usage, and may not close tidFile nor indexFile.
	 * Also, the implementation has to use @generateStore as m might be backed by tidFilen, and this could raise a concurrentModificationException
	 *
	 * This methode is private, but there are deleteIntIndex, deleteStringIndex and deleteBoolIndex to be used externally.
	 */
	private static <K extends Key> void delete(Module m, String indexName, DirectRecordFile<TID, NamedCombinedRecord> tidFile, KeyRecordFile<TID, K> indexFile, Function<NamedCombinedRecord, K> getKey, DBBuffer buf){
	}

	public static Metadata createMetadata(DBBuffer buf, FileCache fc, String[] paths) {
		return null;
	}

	public static Metadata reloadMetadata(DBBuffer buf, FileCache fc, String[] paths) {
		return null;
	}

	public static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> count(){
		return new Triplet<>(Util.namedCombinedRecordFrom(0, "count()"), "count()", (x, y) -> Util.namedCombinedRecordFrom(x.getInt("count()")+1, "count()"));
	}

	public static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> min(String s){
		String n = "min("+s+")";
		return new Triplet<>(Util.namedCombinedRecordFrom(Integer.MAX_VALUE, n), n, (x, y) -> y.getInt(s) < x.getInt(n) ? Util.namedCombinedRecordFrom(y.getInt(s), n) : x);
	}

	public static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> max(String s){
		String n = "max("+s+")";
		return new Triplet<>(Util.namedCombinedRecordFrom(Integer.MIN_VALUE, n), n, (x, y) -> y.getInt(s) > x.getInt(n) ? Util.namedCombinedRecordFrom(y.getInt(s), n) : x);
	}

	public static void deleteIntIndex(Module m, String indexName, DirectRecordFile<TID, NamedCombinedRecord> tidFile, KeyRecordFile<TID, DBInteger> indexFile, DBBuffer buf) {
		NamedCombinedRecord ncr = m.pull();
		if (ncr == null) return;
		m.reset();
		switch(ncr.getType(indexName)) {
			case INT: delete(m, indexName, tidFile, indexFile, x -> new DBInteger(x.getInt(indexName)), buf); break;
			default: throw new IllegalStateException("Type-Mismatch");
		}
	}

	public static void deleteStringIndex(Module m, String indexName, DirectRecordFile<TID, NamedCombinedRecord> tidFile, KeyRecordFile<TID, DBString> indexFile, DBBuffer buf) {
		NamedCombinedRecord ncr = m.pull();
		if (ncr == null) return;
		m.reset();
		switch(ncr.getType(indexName)) {
			case STRING: delete(m, indexName, tidFile, indexFile, x -> new DBString(x.getString(indexName)), buf); break;
			default: throw new IllegalStateException("Type-Mismatch");
		}
	}

	public static void deleteBoolIndex(Module m, String indexName, DirectRecordFile<TID, NamedCombinedRecord> tidFile, KeyRecordFile<TID, Bool> indexFile, DBBuffer buf) {
		NamedCombinedRecord ncr = m.pull();
		if (ncr == null) return;
		m.reset();
		switch(ncr.getType(indexName)) {
			case BOOL: delete(m, indexName, tidFile, indexFile, x -> new Bool(x.getBool(indexName)), buf); break;
			default: throw new IllegalStateException("Type-Mismatch");
		}
	}
}
