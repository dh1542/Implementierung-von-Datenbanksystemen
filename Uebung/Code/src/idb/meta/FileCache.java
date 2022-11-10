/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.meta;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import idb.record.DirectRecordFile;
import idb.record.KeyRecordFile;

import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.TID;
import idb.datatypes.Triplet;
import idb.datatypes.Key;
import idb.datatypes.DataObject;
import idb.datatypes.DBInteger;
import idb.datatypes.Bool;
import idb.datatypes.DBString;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;

import idb.block.BlockFile;

import java.io.IOException;
import java.io.FileNotFoundException;

import static idb.construct.Util.*;

import java.nio.ByteBuffer;
public class FileCache implements AutoCloseable {
	private Map<String, SoftReference<DirectRecordFile<TID, NamedCombinedRecord>>> tidMap = new HashMap<>();
	private Map<String, SoftReference<KeyRecordFile>> keyMap = new HashMap<>();
	private Map<String, SoftReference<KeyRecordFile<TID, DBInteger>>> intMap = new HashMap<>();
	private Map<String, SoftReference<KeyRecordFile<TID, Bool>>> boolMap = new HashMap<>();
	private Map<String, SoftReference<KeyRecordFile<TID, DBString>>> stringMap = new HashMap<>();
	private List<Triplet<WeakReference<BFProxy>,BlockFile,Void>> cleanupBFFiles = new ArrayList<>();
	private List<Triplet<WeakReference<KeyRecordFile>,KeyRecordFile,Void>> cleanupHashFiles = new ArrayList<>();
	private DBBuffer buf;

	public FileCache(DBBuffer buffer) {
		buf = buffer;
	}

	/*
	 * Returns a DirectRecordFile for path @path. If this file is already open and in the cache, blockSize is ignored.
	 * Otherwise blockSize is used to build a new TIDFile and insert this into the Cache.
	 * It is garanteed that the returned file will be identical to a previously returned file for this path if there is still at least
	 * one strong reference to the earlier returned DRF.
	 */
	public DirectRecordFile<TID, NamedCombinedRecord> getTID(String path, int blockSize) {
		checkDeleted();
		SoftReference<DirectRecordFile<TID, NamedCombinedRecord>> ref = tidMap.get(path);
		DirectRecordFile<TID, NamedCombinedRecord> out;
		if (ref == null || (out = ref.get()) == null) return createTID(path, blockSize); // createTID will override the old reference
		return out;
	}

	/*
	 * Returns a KeyRecordFile for path @path. If this file is already open and in the cache, blockSize and overflowPath is ignored.
	 * Otherwise these are used to build a new HsahFile and insert this into the Cache.
	 * It is garanteed that the returned file will be identical to a previously returned file for this path if there is still at least
	 * one strong reference to the earlier returned KRF.
	 * This variant is used for Indices that use a int as key. There are variants for Bool and DBString aswell.
	 * Note that depending on your implementation of ClockBuffer it might not be possible to use the buffer with different blockSizes.
	 */
	public KeyRecordFile<TID, DBInteger> getIntIndex(String path, String overflowPath, int blockSize, int blockSizeOverflow) {
		return getIndex(path, overflowPath, blockSize, intMap, cleanupHashFiles, blockSizeOverflow);
	}

	/*
	 * Returns a KeyRecordFile for path @path. If this file is already open and in the cache, blockSize and overflowPath is ignored.
	 * Otherwise these are used to build a new HsahFile and insert this into the Cache.
	 * It is garanteed that the returned file will be identical to a previously returned file for this path if there is still at least
	 * one strong reference to the earlier returned KRF.
	 * This variant is used for Indices that use a bool as key. There are variants for DBInteger and DBString aswell.
	 * Note that depending on your implementation of ClockBuffer it might not be possible to use the buffer with different blockSizes.
	 */
	public KeyRecordFile<TID, Bool> getBoolIndex(String path, String overflowPath, int blockSize, int blockSizeOverflow) {
		return getIndex(path, overflowPath, blockSize, boolMap, cleanupHashFiles, blockSizeOverflow);
	}

	/*
	 * Returns a KeyRecordFile for path @path. If this file is already open and in the cache, blockSize and overflowPath is ignored.
	 * Otherwise these are used to build a new HsahFile and insert this into the Cache.
	 * It is garanteed that the returned file will be identical to a previously returned file for this path if there is still at least
	 * one strong reference to the earlier returned KRF.
	 * This variant is used for Indices that use a DBString as key. There are variants for Bool and DBInteger aswell.
	 * Note that depending on your implementation of ClockBuffer it might not be possible to use the buffer with different blockSizes.
	 */
	public KeyRecordFile<TID, DBString> getStringIndex(String path, String overflowPath, int blockSize, int blockSizeOverflow) {
		return getIndex(path, overflowPath, blockSize, stringMap, cleanupHashFiles, blockSizeOverflow);
	}

	private DirectRecordFile<TID, NamedCombinedRecord> createTID(String path, int blockSize) {
		BlockFile bf = open(path, blockSize);
		DirectRecordFile<TID, NamedCombinedRecord> out = generateTID(buf, bf);
		tidMap.put(path, new SoftReference<>(out));
		return out;
	}

	private BlockFile open(String path, int blockSize) {
		BlockFile bf = generateBlockFile(blockSize);
		try{
			try{
				bf.open(path, "rw");
			}
			catch(FileNotFoundException fnfe){
				throw new RuntimeException("This file seems to have disappeard from the filesystem, check if the path was valid", fnfe);
			}
			catch(IOException ioe) {
				throw new RuntimeException("Something very bad happeded", ioe);
			}
			BFProxy proxy = new BFProxy(bf);
			cleanupBFFiles.add(new Triplet<>(new WeakReference<>(proxy), bf, null));
			// to make sure bf is not closed as soon as we return out, we override it.
			// As the proxy is already inserted into cleanupBFFiles, we can drop the reference and it'll be closed later.
			bf = generateBlockFile(blockSize);
			return proxy;
		}
		finally{
			try{
				bf.close();
			}
			catch(IOException ioe) {}
		}
	}

	private void checkDeleted() {
		for(Iterator<Triplet<WeakReference<BFProxy>,BlockFile,Void>> it = cleanupBFFiles.iterator(); it.hasNext();){
			Triplet<WeakReference<BFProxy>, BlockFile, Void> item = it.next();
			if (item.first().get() == null){
				try{
					item.second().close();
				} catch(IOException ioe) {
					throw new RuntimeException("This is close failing: ", ioe);
				}
				it.remove();
			}
		}
		for(Iterator<Triplet<WeakReference<KeyRecordFile>,KeyRecordFile,Void>> it = cleanupHashFiles.iterator(); it.hasNext();){
			Triplet<WeakReference<KeyRecordFile>, KeyRecordFile, Void> item = it.next();
			if (item.first().get() == null){
				try{
					item.second().close();
				} catch(IOException ioe) {
					throw new RuntimeException("This is close failing: ", ioe);
				} catch(idb.buffer.BufferFullException bfe) {
					throw new RuntimeException("This is close failing: ", bfe);
				}
				it.remove();
			}
		}
	}

	private <K extends Key> KeyRecordFile<TID, K> getIndex(String path, String overflowPath, int blockSize, Map<String, SoftReference<KeyRecordFile<TID, K>>> cache, List<Triplet<WeakReference<KeyRecordFile>, KeyRecordFile, Void>> cleanup, int blockSizeOverflow) {
		checkDeleted();
		SoftReference<KeyRecordFile<TID, K>> ref = cache.get(path);
		KeyRecordFile<TID, K> out;
		if (ref == null || (out = ref.get()) == null) return loadIndex(path, overflowPath, blockSize, cache, cleanup, blockSizeOverflow); // this will override the old reference
		return out;
	}

	private <K extends Key> KeyRecordFile<TID, K> loadIndex(String path, String overflowPath, int blockSize, Map<String, SoftReference<KeyRecordFile<TID, K>>> cache, List<Triplet<WeakReference<KeyRecordFile>, KeyRecordFile, Void>> cleanup, int blockSizeOverflow) {
		BlockFile bf = open(path, blockSize);
		BlockFile bfO = open(overflowPath, blockSizeOverflow);
		KeyRecordFile<TID, K> out = rebuildHash(buf, bf, bfO);
		KeyRecordFile<TID, K> proxy = new KeyProxy<>(out);
		cache.put(path, new SoftReference<>(proxy));
		cleanup.add(new Triplet<>(new WeakReference<>(proxy), out, null));
		return out;
	}


	public void close() {
		for(Triplet<WeakReference<KeyRecordFile>, KeyRecordFile, Void> t: cleanupHashFiles) {
			try{
				t.second().close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		try{
			buf.flush();
		} catch(Exception e) {
			throw new RuntimeException("Close failed: ", e);
		}
		for(Triplet<WeakReference<BFProxy>, BlockFile, Void> t: cleanupBFFiles) {
			try{
				t.second().close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}


class BFProxy implements BlockFile{
	private BlockFile bf;
	public BFProxy(BlockFile other) {
		bf = other;
	}

	@Override
	public void open(String filename, String mode) throws IOException{
		bf.open(filename, mode);
	}

	@Override
	public void close() throws IOException {
		bf.close();
	}

	@Override
	public void append(int num) throws IOException {
		bf.append(num);
	}

	@Override
	public void write(int blockNo, ByteBuffer bb) throws IOException{
		bf.write(blockNo, bb);
	}

	@Override
	public void read(int blockNo, ByteBuffer bb) throws IOException{
		bf.read(blockNo, bb);
	}

	@Override
	public int size() throws IOException {
		return bf.size();
	}

	@Override
	public int bytes() {
		return bf.bytes();
	}

	@Override
	public void drop(int amm) throws IOException {
		bf.drop(amm);
	}

	@Override
	public String filename() {
		return bf.filename();
	}
}

class KeyProxy<D extends DataObject, K extends Key> implements KeyRecordFile<D, K> {
	private KeyRecordFile<D, K> object;
	public KeyProxy(KeyRecordFile<D, K> other) {
		object = other;
	}

	@Override
	public void insert(K key, D dataObject) throws BufferFullException, IOException{
		object.insert(key, dataObject);
	}

	@Override
	public void read(K key, D dataObject, int idx) throws BufferFullException, IOException{
		object.read(key, dataObject, idx);
	}

	@Override
	public void delete(K key, int idx, D dataObject) throws BufferFullException, IOException{
		object.delete(key, idx, dataObject);
	}

	@Override
	public void close() throws BufferFullException, IOException{
		object.close();
	}

	@Override
	public int size(K key, D dataObject) throws BufferFullException, IOException{
		return object.size(key, dataObject);
	}
}
