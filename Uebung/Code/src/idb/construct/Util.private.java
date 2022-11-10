/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.construct;

import java.lang.reflect.Constructor;

import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;
import idb.record.DirectRecordFile;
import idb.record.KeyRecordFile;
import idb.record.TIDFile;
import idb.record.HashImpl;
import idb.record.SeqRecordFile;
import idb.record.SeqRecordFileImpl;
import idb.record.View;
import idb.module.Module;
import idb.module.ProjImpl;
import idb.module.SelImpl;
import idb.module.OrderImpl;
import idb.module.GroupImpl;
import idb.module.Generate;
import idb.module.Rename;
import idb.module.Cross;
import idb.module.Store;
import idb.module.TableScan;
import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.NamedCombinedRecordImpl;
import idb.datatypes.Triplet;
import idb.datatypes.DataObject;
import idb.datatypes.Key;
import idb.datatypes.TID;
import idb.datatypes.Bool;
import idb.datatypes.DBString;
import idb.datatypes.DBInteger;

import idb.meta.Metadata;
import idb.meta.FileCache;
import idb.meta.MetaImpl;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

import java.io.IOException;


public class Util{
	public static BlockFile generateBlockFile(int blockSize) {
		return new idb.block.BlockImpl(blockSize);
	}

	public static DBBuffer generateSimpleBuffer(int pageSize) {
		return new idb.buffer.SimpleDBBuffer(pageSize);
	}

	public static <V extends DataObject> DirectRecordFile<idb.datatypes.TID, V> generateTID(DBBuffer buf, BlockFile bf) {
		try{
			return new TIDFile<V>(buf, bf);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static DBBuffer generateClockBuffer(int pageSize, int capacity) {
		return new idb.buffer.ClockDBBuffer(pageSize, capacity);
	}

	public static <V extends DataObject, K extends Key> KeyRecordFile<V, K> rebuildHash(DBBuffer buf, BlockFile regular, BlockFile overflow) {
		try{
			return HashImpl.reconstruct(regular, overflow, buf);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <V extends DataObject, K extends Key> KeyRecordFile<V, K> generateHash(DBBuffer buf, BlockFile regular, BlockFile overflow, double threshhold, int initCapacity) {
		try{
			return new HashImpl<>(regular, overflow, buf, threshhold, initCapacity);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static <D extends DataObject> SeqRecordFile<D> generateSeqRecordFile(DBBuffer dbbuf, BlockFile bf) {
		try{
			return new SeqRecordFileImpl<D>(dbbuf, bf);
		} catch(Exception e) {
			throw new RuntimeException("uncaught exception", e);
		}
	}

	public static <D extends DataObject> SeqRecordFile<D> rebuildSeqRecordFile(DBBuffer dbbuf, BlockFile bf) {
		try{
			return SeqRecordFileImpl.<D>reconstruct(dbbuf, bf);
		} catch(Exception e) {
			throw new RuntimeException("uncaught exception", e);
		}
	}


	public static Module generateProj(Module prev, String ...columns){
		return new ProjImpl(prev, x -> Arrays.stream(columns).anyMatch(x::equals));
	}
	public static Module generateRename(Module prev, Function<String, String> renameFunc){
		return new Rename(prev, renameFunc);
	}
	public static Module generateSel(Module prev, Predicate<NamedCombinedRecord> pred) {
		return new SelImpl(prev, pred);
	}
	public static Module generateCross(Module left, Module right, String leftS, String rightS) {
		return new Cross(left, right, leftS, rightS);
	}
	public static Module generateOrder(Module prev, boolean asc, String ...columns){
		return new OrderImpl(prev, asc, columns);
	}
	public static Module generateGroup(Module p, List<Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>>> funcs, String ...var) {
		return generateGroup(p, funcs.stream().map(Triplet::third).collect(Collectors.toList()), funcs.stream().map(Triplet::first).collect(Collectors.toList()), funcs.stream().map(Triplet::second).collect(Collectors.toList()), var);
	}
	private static Module generateGroup(Module p, List<BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> aggFuncs, List<NamedCombinedRecord> in, List<String> gen, String ...var){
		return new GroupImpl(p, aggFuncs, in, gen,var);
	}
	public static Module generateGroup(Module p, String ...var){
		return generateGroup(p, List.of(), var);
	}

	public static NamedCombinedRecord getNCR(List<NamedCombinedRecord.Type> types, List<String> names) {
		return new NamedCombinedRecordImpl(names, types);
	}

	public static NamedCombinedRecord namedCombinedRecordFrom(int i, String name) {
		return new NamedCombinedRecordImpl(i, name);
	}
	public static NamedCombinedRecord namedCombinedRecordFrom(boolean i, String name) {
		return new NamedCombinedRecordImpl(i, name);
	}
	public static NamedCombinedRecord namedCombinedRecordFrom(String value, String name) {
		return new NamedCombinedRecordImpl(value, name);
	}
	public static Module generateTableScan(View<NamedCombinedRecord> view, NamedCombinedRecord record){
		return new TableScan(view, record);
	}
	public static Module generateGen(Module m, Function<NamedCombinedRecord, NamedCombinedRecord> generator){
		return new Generate(m, generator);
	}
	public static Module generateStore(Module m, DBBuffer buf) {
		return new Store(m, buf);
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
		m = generateStore(m, buf);
		try{
			for(NamedCombinedRecord ncr = m.pull(); ncr != null; ncr = m.pull()) {
				K k = getKey.apply(ncr);
				TID out = new TID(0, 0);
				indexFile.read(k, out, 0);
				tidFile.delete(out);
				indexFile.delete(k, 0, out);
			}
		}
		catch(IOException | BufferFullException ex) {
			throw new RuntimeException("This is delete: ", ex);
		}
		finally {
			m.close();
		}
	}

	public static Metadata createMetadata(DBBuffer buf, FileCache fc, String[] paths) {
		return MetaImpl.createFreshMetadata(buf, fc, paths);
	}

	public static Metadata reloadMetadata(DBBuffer buf, FileCache fc, String[] paths) {
		return new MetaImpl(buf, fc, paths);
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
