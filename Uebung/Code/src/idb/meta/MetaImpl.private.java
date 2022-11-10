/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.meta;

import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.NamedCombinedRecordImpl;
import idb.datatypes.DBString;
import idb.datatypes.Bool;
import idb.datatypes.DBInteger;
import idb.datatypes.Key;
import idb.datatypes.DataObject;
import idb.datatypes.TID;

import idb.module.Module;
import idb.module.*;

import idb.construct.Util;

import idb.record.KeyRecordFile;
import idb.record.DirectRecordFile;
import idb.record.TIDFile;
import idb.record.DeletedRecordException;

import java.util.List;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import idb.block.BlockFile;

import idb.buffer.BufferFullException;
import idb.buffer.DBBuffer;
/*
 * Idea: Use two Relations with fixed layout to store all necessary information to read other relations
 * Attr(Name, Rel: REL, Type: String, Pos: Int, Primary-Key: Boolean, Surrogate-Key: Boolean, _ _Surrogate _ : Int) (uses surrogateKey)
 * Files(_Name_, Rel: REL, Type: String, BlockSize: Int) Type can be "TID" or "IDX_N" / "IDX_Overflow_N" where N is a number
 */

public class MetaImpl implements Metadata {

	private DirectRecordFile<TID, NamedCombinedRecord> attr;
	private DirectRecordFile<TID, NamedCombinedRecord> files;
	private KeyRecordFile<TID, DBInteger> attr_index;
	private KeyRecordFile<TID, DBString> files_index;
	private FileCache fileCache;
	private DBBuffer dbbuf;

	public static final String path = "data/";
	public static final int blockSize = 4096;

	public MetaImpl(DBBuffer pbuf, FileCache pFileCache, String[] paths) {
		assert paths.length == 6;
		dbbuf = pbuf;
		fileCache = pFileCache;
		// to restore these files we need to use the fileCache, otherwise we do not get to close them properly.
		attr = fileCache.getTID(paths[0], blockSize);
		files = fileCache.getTID(paths[1], blockSize);
		attr_index = fileCache.getIntIndex(paths[2], paths[3], blockSize, blockSize);
		files_index = fileCache.getStringIndex(paths[4], paths[5], blockSize, blockSize);
		//System.out.println("p4: "+ paths[4]+", p5:" + paths[5]);
		//System.out.println("p1: "+ paths[1]);
	}

	public static Metadata createFreshMetadata(DBBuffer pbuf, FileCache pFileCache, String[] paths) {
		assert paths.length == 6;
		try(BlockFile bf1 = Util.generateBlockFile(blockSize);
				BlockFile bf2 = Util.generateBlockFile(blockSize);
				BlockFile bf3 = Util.generateBlockFile(blockSize);
				BlockFile bf4 = Util.generateBlockFile(blockSize)){
			bf1.open(paths[2], "rw");
			bf2.open(paths[3], "rw");
			bf3.open(paths[4], "rw");
			bf4.open(paths[5], "rw");
			Util.generateHash(pbuf, bf1, bf2, 0.8, 13).close();
			Util.generateHash(pbuf, bf3, bf4, 0.8, 13).close();
			pbuf.flush(); // see note in addRelation
		}
		catch(IOException | BufferFullException ioe){
			throw new RuntimeException("Unable to construct Hashes", ioe);
		}
		return new MetaImpl(pbuf, pFileCache, paths);
	}

	private NamedCombinedRecord attrNCR(){
		NamedCombinedRecord out = new NamedCombinedRecordImpl("", "Name");
		out = out.combine(new NamedCombinedRecordImpl("", "Rel"));
		out = out.combine(new NamedCombinedRecordImpl("", "Type"));
		out = out.combine(new NamedCombinedRecordImpl(0, "Pos"));
		out = out.combine(new NamedCombinedRecordImpl(false, "Primary-Key"));
		out = out.combine(new NamedCombinedRecordImpl(false, "Surrogate-Key"));
		out = out.combine(new NamedCombinedRecordImpl(0, "_Surrogate"));
		return out;
	}

	private NamedCombinedRecord filesNCR(){
		NamedCombinedRecord out = new NamedCombinedRecordImpl("", "Name");
		out = out.combine(new NamedCombinedRecordImpl("", "Rel"));
		out = out.combine(new NamedCombinedRecordImpl("", "Type"));
		out = out.combine(new NamedCombinedRecordImpl(0, "BlockSize"));
		return out;
	}

	public boolean hasRelation(String name){
		Module m = new TableScan(attr.view(), attrNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(name));
		boolean res = ( m.pull() != null);
		m.close();
		return res;
	}

	//@throws NoSucheElementException if no Relation relName or no Column colName exists.
	private NamedCombinedRecord getAttr(String relName, String colName) {
		Module m = new TableScan(attr.view(), attrNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		m = new SelImpl(m, a -> a.getString("Name").equals(colName));
		NamedCombinedRecord ncr = m.pull();
		m.close();
		if (ncr == null) throw new NoSuchElementException("This attr does not exists in this relation");
		return ncr;
	}

	public String getPrimaryKey(String relName) {
		return getPrimaryKeyInternal(relName).getString("Name");
	}

	private NamedCombinedRecord getPrimaryKeyInternal(String relName){
		Module m = new TableScan(attr.view(), attrNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		m = new SelImpl(m, a -> a.getBool("Primary-Key"));
		NamedCombinedRecord ncr = m.pull();
		m.close();
		if (ncr == null) throw new NoSuchElementException("This relation does not exist");
		return ncr;
	}

	public NamedCombinedRecord.Type getType(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		return getType(ncr);
	}

	private NamedCombinedRecord.Type getType(NamedCombinedRecord ncr) {
		switch(ncr.getString("Type")){
			case "B": return NamedCombinedRecord.Type.BOOL;
			case "I": return NamedCombinedRecord.Type.INT;
			case "S": return NamedCombinedRecord.Type.STRING;
			default: throw new RuntimeException("This should bever happenr");
		}
	}

	public boolean hasIndex(String relName, String colName) {
		return indexFile(relName, colName) != null;
	}

	private NamedCombinedRecord indexFile(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		return indexFileFromType(indexType(ncr.getInt("Pos")), relName);
	}

	private String indexType(int position) {
		return "IDX_"+position;
	}

	private String indexOverflowType(int position) {
		return "IDX_Overflow_"+position;
	}

	private NamedCombinedRecord indexFileFromType(String typeString, String relName){
		Module m = new TableScan(files.view(), filesNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		m = new SelImpl(m, a -> a.getString("Type").equals(typeString));
		NamedCombinedRecord out = m.pull();
		m.close();
		return out;
	}

	public List<String> getColumns(String relName) {
		Module m = new TableScan(attr.view(), attrNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		List<String> out = new ArrayList<>();
		NamedCombinedRecord ncr = m.pull();
		while(ncr != null) {
			out.add(ncr.getString("Name"));
			ncr = m.pull();
		}
		m.close();
		if (out.size() == 0) throw new NoSuchElementException("This relation does not exist");
		return out;
	}

	public NamedCombinedRecord getNCR(String relName) {
		Module m = new TableScan(attr.view(), attrNCR());
		try{
			m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
			m = new OrderImpl(m, true, "Pos");
			NamedCombinedRecord ncr = m.pull();
			if (ncr == null) throw new NoSuchElementException("This relation does not exits");
			NamedCombinedRecord out = new NamedCombinedRecordImpl(List.of(ncr.getString("Name")), List.of(getType(ncr)));
			ncr = m.pull();
			while(ncr != null) {
				out = out.combine(new NamedCombinedRecordImpl(List.of(ncr.getString("Name")), List.of(getType(ncr))));
				ncr = m.pull();
			}
			return out;
		}
		finally{
			m.close();
		}
	}

	public DirectRecordFile<TID, NamedCombinedRecord> getTIDFile(String relName){
		Module m = new TableScan(files.view(), filesNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		m = new SelImpl(m, a -> a.getString("Type").equals("TID"));
		NamedCombinedRecord ncr = m.pull();
		m.close();
		if (ncr == null) throw new NoSuchElementException("This relation does not exist");
		String path = ncr.getString("Name");
		return fileCache.getTID(path, ncr.getInt("BlockSize"));
	}

	public KeyRecordFile<TID, DBString> getStringIndex(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		if (getType(ncr) != NamedCombinedRecord.Type.STRING) throw new IllegalStateException("Type Mismatch");
		NamedCombinedRecord indexFile = indexFileFromType(indexType(ncr.getInt("Pos")), relName);
		if (indexFile == null) throw new NoSuchElementException("This index does not exist");
		NamedCombinedRecord indexOverflowFile = indexFileFromType(indexOverflowType(ncr.getInt("Pos")), relName);
		return fileCache.getStringIndex(indexFile.getString("Name"), indexOverflowFile.getString("Name"), indexFile.getInt("BlockSize"), indexOverflowFile.getInt("BlockSize"));
	}

	public KeyRecordFile<TID, DBInteger> getIntIndex(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		if (getType(ncr) != NamedCombinedRecord.Type.INT) throw new IllegalStateException("Type Mismatch");
		NamedCombinedRecord indexFile = indexFileFromType(indexType(ncr.getInt("Pos")), relName);
		if (indexFile == null) throw new NoSuchElementException("This index does not exist");
		NamedCombinedRecord indexOverflowFile = indexFileFromType(indexOverflowType(ncr.getInt("Pos")), relName);
		return fileCache.getIntIndex(indexFile.getString("Name"), indexOverflowFile.getString("Name"), indexFile.getInt("BlockSize"), indexOverflowFile.getInt("BlockSize"));
	}

	public KeyRecordFile<TID, Bool> getBoolIndex(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		if (getType(ncr) != NamedCombinedRecord.Type.BOOL) throw new IllegalStateException("Type Mismatch");
		NamedCombinedRecord indexFile = indexFileFromType(indexType(ncr.getInt("Pos")), relName);
		if (indexFile == null) throw new NoSuchElementException("This index does not exist");
		NamedCombinedRecord indexOverflowFile = indexFileFromType(indexOverflowType(ncr.getInt("Pos")), relName);
		return fileCache.getBoolIndex(indexFile.getString("Name"), indexOverflowFile.getString("Name"), indexFile.getInt("BlockSize"), indexOverflowFile.getInt("BlockSize"));
	}

	public void dropRelation(String relName) {
		// get all files and delete them.
		Module m = new TableScan(files.view(), filesNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		NamedCombinedRecord ncr = m.pull();
		if (ncr == null){
			m.close();
			throw new NoSuchElementException("This relation does not exist");
		}
		for(; ncr != null; ncr = m.pull()){
			File f = new File(ncr.getString("Name"));
			f.delete();
		}
		// call delete on files where name matches
		m.reset();
		Util.deleteStringIndex(m, "Name", files, files_index, dbbuf); // m is dead now.
		// call delete on attr where name matches
		m = new TableScan(attr.view(), attrNCR());
		m = new SelImpl(m, a -> a.getString("Rel").equals(relName));
		Util.deleteIntIndex(m, "_Surrogate", attr, attr_index, dbbuf); // m is dead now.
	}

	public void insert(String relName, List<String> columns, List<NamedCombinedRecord> values) {
		List<String> realColumn = getColumns(relName);
		if (values.size () != columns.size()) throw new IllegalArgumentException("The list sizes do not match up");
		if (columns.contains("_Surrogate")) throw new IllegalArgumentException("_Surrogate may not be supplied");
		if (realColumn.contains("_Surrogate")) {
			// We need to figure out which Key can be used.
			NamedCombinedRecord surrAddition = Util.namedCombinedRecordFrom(getSurrogate(relName), "_Surrogate");
			columns = new ArrayList<>(columns);
			columns.add("_Surrogate");
			values = new ArrayList<>(values);
			values.add(surrAddition);
		}
		else {
			// We need to test if the primaryKey is already used
			String pk = getPrimaryKey(relName);
			int idx = columns.indexOf(pk);
			if (idx == -1) {
				throw new NoSuchElementException("Missing pk, no SurrogateKey");
			}
			Module m = Util.generateTableScan(getTIDFile(relName).view(), getNCR(relName));
			final List<NamedCombinedRecord> vl = values;
			switch(getType(relName, pk)) {
				case STRING: m = Util.generateSel(m, x -> x.getString(pk).equals(vl.get(idx).getString(pk))); break;
				case INT: m = Util.generateSel(m, x -> x.getInt(pk) == vl.get(idx).getInt(pk)); break;
				case BOOL: m = Util.generateSel(m, x -> x.getBool(pk) == vl.get(idx).getBool(pk)); break;
			}
			boolean used = (m.pull() != null);
			m.close();
			if (used) throw new IllegalStateException("This PrimaryKey is already used");
		}
		if (realColumn.size() != columns.size()) {
			throw new IllegalArgumentException("The number of columns / values does not match the necessary number");
		}
		Module m = Util.generateTableScan(attr.view(), attrNCR());
		m = Util.generateSel(m, x -> x.getString("Rel").equals(relName));
		m = Util.generateOrder(m, true, "Pos");
		NamedCombinedRecord inOrder = Util.getNCR(List.of(), List.of());
		for(NamedCombinedRecord ncr = m.pull(); ncr != null; ncr = m.pull()){
			int idx = columns.indexOf(ncr.getString("Name"));
			if (idx == -1) {
				throw new NoSuchElementException("Missing attribute: " + ncr.getString("Name"));
			}
			if (values.get(idx).getType(columns.get(idx)) != getType(ncr)) {
				throw new IllegalStateException("Attribute of wrong type: " + ncr.getString("Name"));
			}
			inOrder = inOrder.combine(values.get(idx));
		}
		m.close();

		try {
			TID res = getTIDFile(relName).insert(inOrder);

			// Now we have to insert the TID into all indices
			m = Util.generateTableScan(files.view(), filesNCR());
			m = Util.generateSel(m, x -> x.getString("Rel").equals(relName));
			m = Util.generateCross(m, Util.generateTableScan(files.view(), filesNCR()), "Files", "Overflow");
			m = Util.generateSel(m, x -> x.getString("Overflow.Rel").equals(relName));
			m = Util.generateSel(m, x -> x.getString("Files.Type").startsWith("IDX_"));
			m = Util.generateSel(m, x -> x.getString("Overflow.Type").startsWith("IDX_Overflow_"));
			m = Util.generateSel(m, x -> (! x.getString("Files.Type").contains("Overflow")));
			m = Util.generateSel(m, x -> x.getString("Files.Type").split("_")[1].equals(x.getString("Overflow.Type").split("_")[2]));
			m = Util.generateGen(m, x -> Util.namedCombinedRecordFrom(Integer.parseInt(x.getString("Files.Type").split("_")[1]), "_Pos"));
			m = Util.generateCross(m, Util.generateTableScan(attr.view(), attrNCR()), null, "Attr");
			m = Util.generateSel(m, x -> x.getString("Attr.Rel").equals(relName));
			m = Util.generateSel(m, x -> x.getInt("Attr.Pos") == x.getInt("_Pos"));
			m = Util.generateProj(m, "Files.Name", "Overflow.Name", "Attr.Type", "Attr.Name", "Files.BlockSize", "Overflow.BlockSize");
			m = Util.generateRename(m, x -> x.equals("Attr.Type") ? "Type" : x);
			for(NamedCombinedRecord ncr = m.pull(); ncr != null; ncr = m.pull()) {
				switch(getType(ncr)) {
					case INT: fileCache.getIntIndex(ncr.getString("Files.Name"), ncr.getString("Overflow.Name"), ncr.getInt("Files.BlockSize"), ncr.getInt("Overflow.BlockSize")).insert(new DBInteger(inOrder.getInt(ncr.getString("Attr.Name"))), res); break;
					case STRING: fileCache.getStringIndex(ncr.getString("Files.Name"), ncr.getString("Overflow.Name"), ncr.getInt("Files.BlockSize"), ncr.getInt("Overflow.BlockSize")).insert(new DBString(inOrder.getString(ncr.getString("Attr.Name"))), res); break;
					case BOOL: fileCache.getBoolIndex(ncr.getString("Files.Name"), ncr.getString("Overflow.Name"), ncr.getInt("Files.BlockSize"), ncr.getInt("Overflow.BlockSize")).insert(new Bool(inOrder.getBool(ncr.getString("Attr.Name"))), res); break;
				}
			}

			m.close();
		}
		catch(BufferFullException | IOException e) {
			throw new RuntimeException("This is bad", e);
		}
	}

	private int getSurrogate(String relName) {
		return getSurrogate(new TableScan(getTIDFile(relName).view(), getNCR(relName)));
	}

	// takes ownership
	private int getSurrogate(Module m) {
		m = Util.generateGen(m, x -> idb.construct.Util.namedCombinedRecordFrom(1, "_Matcher"));
		m = Util.generateGroup(m, List.of(idb.construct.Util.max("_Surrogate")), "_Matcher");
		NamedCombinedRecord ncr = m.pull();
		int res = ncr == null ? 0 : ncr.getInt("max(_Surrogate)") + 1;
		m.close();
		return res;
	}

	public void createIndex(String relName, String colName) {
		NamedCombinedRecord ncr = getAttr(relName, colName);
		NamedCombinedRecord existing = indexFileFromType(indexType(ncr.getInt("Pos")), relName);
		if (existing != null) throw new IllegalStateException("This index is already there");
		File dir = new File(path);
		try{
			File regular = File.createTempFile("reg", ".data", dir);
			File overflow = File.createTempFile("ofl", ".data", dir);
			NamedCombinedRecord regNCR = Util.namedCombinedRecordFrom(regular.getCanonicalPath(), "Name");
			NamedCombinedRecord oflNCR = Util.namedCombinedRecordFrom(overflow.getCanonicalPath(), "Name");
			regNCR = regNCR.combine(Util.namedCombinedRecordFrom(relName,"Rel"));
			oflNCR = oflNCR.combine(Util.namedCombinedRecordFrom(relName,"Rel"));
			regNCR = regNCR.combine(Util.namedCombinedRecordFrom("IDX_"+ncr.getInt("Pos"),"Type"));
			oflNCR = oflNCR.combine(Util.namedCombinedRecordFrom("IDX_Overflow_"+ncr.getInt("Pos"),"Type"));
			regNCR = regNCR.combine(Util.namedCombinedRecordFrom(blockSize,"BlockSize"));
			oflNCR = oflNCR.combine(Util.namedCombinedRecordFrom(blockSize,"BlockSize"));
			insertFileNCR(regNCR);
			insertFileNCR(oflNCR);

			try(BlockFile bf = Util.generateBlockFile(blockSize); BlockFile bfO = Util.generateBlockFile(blockSize)) {
				bf.open(regular.getCanonicalPath(), "rw");
				bfO.open(overflow.getCanonicalPath(), "rw");
				switch(getType(ncr)){
					case INT: KeyRecordFile<TID, DBInteger> krfi = Util.generateHash(dbbuf, bf, bfO, 0.8, 13);
							  createIndexData(relName, x -> new DBInteger(x.getInt(colName)), krfi);
							  break;
					case STRING: KeyRecordFile<TID, DBString> krfs = Util.generateHash(dbbuf, bf, bfO, 0.8, 13);
								 createIndexData(relName, x -> new DBString(x.getString(colName)), krfs);
								 break;
					case BOOL: KeyRecordFile<TID, Bool> krfb = Util.generateHash(dbbuf, bf, bfO, 0.8, 13);
							   createIndexData(relName, x -> new Bool(x.getBool(colName)), krfb);
				}
				dbbuf.flush(); // see addRelation
			}

		} catch(IOException ioe){
			throw new RuntimeException("This is an IOException", ioe);
		}
	}

	// closes newIndex
	private <K extends Key> void createIndexData(String relName, Function<NamedCombinedRecord, K> mapper, KeyRecordFile<TID, K> newIndex){
		NamedCombinedRecord ncr = getPrimaryKeyInternal(relName);
		switch(getType(ncr)){
			case INT: createIndexData(relName, mapper, newIndex, x -> new DBInteger(x.getInt(ncr.getString("Name"))), getIntIndex(relName, ncr.getString("Name"))); break;
			case STRING: createIndexData(relName, mapper, newIndex, x -> new DBString(x.getString(ncr.getString("Name"))), getStringIndex(relName, ncr.getString("Name"))); break;
			case BOOL: createIndexData(relName, mapper, newIndex, x -> new Bool(x.getBool(ncr.getString("Name"))), getBoolIndex(relName, ncr.getString("Name"))); break;
		}
	}

	// oldIndex has to be a unique Index, closes newIndex
	private <K extends Key, K2 extends Key> void createIndexData(String relName, Function<NamedCombinedRecord, K> newMapper, KeyRecordFile<TID, K> newIndex, Function<NamedCombinedRecord, K2> oldMapper, KeyRecordFile<TID, K2> oldIndex) {
		TID out = new TID(0, 0);
		DirectRecordFile<TID, NamedCombinedRecord> drf = getTIDFile(relName);
		Module m = Util.generateTableScan(drf.view(), getNCR(relName));
		for(NamedCombinedRecord ncr = m.pull(); ncr != null; ncr = m.pull()) {
			try{
				oldIndex.read(oldMapper.apply(ncr), out, 0);
				newIndex.insert(newMapper.apply(ncr), out);
			}
			catch (IOException | BufferFullException id) {
				throw new RuntimeException("wrapped ex", id);
			}
		}
		try{
			newIndex.close();
		}
		catch (IOException | BufferFullException ex) {
			throw new RuntimeException("Closing ex: ", ex);
		}
	}

	private void insertFileNCR(NamedCombinedRecord ncr) {
		try{
			TID t = files.insert(ncr);
			files_index.insert(new DBString(ncr.getString("Name")), t);
		} catch(IOException | BufferFullException ex) {
			throw new RuntimeException("InsertFile", ex);
		}
	}

	// does handle surrogate
	private void insertAttrNCR(NamedCombinedRecord ncr) {
		try{
			Module m = Util.generateTableScan(attr.view(),attrNCR());
			ncr = ncr.combine(Util.namedCombinedRecordFrom(getSurrogate(m), "_Surrogate"));
			TID t = attr.insert(ncr);
			attr_index.insert(new DBInteger(ncr.getInt("_Surrogate")), t);
		} catch(IOException | BufferFullException ex) {
			throw new RuntimeException("insertAttr", ex);
		}
	}

	public void addRelation(String name, List<String> columns, List<NamedCombinedRecord.Type> types, String primaryKey, int blockSize) {

		if (hasRelation(name)) {
			throw new IllegalStateException("This relation is already in this database");
		}

		if (types.size() != columns.size()) {
			throw new IllegalArgumentException("The sizes do not match");
		}

		if (blockSize < 0) {
			throw new IllegalArgumentException("The blocksize needs to be positive");
		}

		assert blockSize == this.blockSize;

		final String allowed_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

		if (columns.stream().anyMatch(x -> !x.chars().allMatch(y -> allowed_chars.indexOf(y) != -1))) {
			throw new IllegalArgumentException("Invalid names " + columns);
		}

		int idxOfPrimary;
		boolean primaryIsSurrogate;
		if (primaryKey != null) {
			idxOfPrimary = columns.indexOf(primaryKey);
			if (idxOfPrimary == -1) throw new IllegalArgumentException("Primary Key is not there");
			primaryIsSurrogate = false;
		}
		else {
			primaryIsSurrogate = true;
			idxOfPrimary = columns.size();
			columns = new ArrayList<>(columns);
			columns.add("_Surrogate");
			types = new ArrayList<>(types);
			types.add(NamedCombinedRecord.Type.INT);
		}


		File dir = new File(path);
		// Optional: reoder the types and columns to improve the layout.
		// <Blanc>

		// Now, create the files and insert them into the meta-relations.
		// This includes creating the hashIndex and closing it to init its meta-data.
		try(BlockFile bf = Util.generateBlockFile(blockSize); BlockFile hf = Util.generateBlockFile(blockSize); BlockFile hfO = Util.generateBlockFile(blockSize);
				FileDeletor reg = new FileDeletor(File.createTempFile("tid", ".data", dir)); FileDeletor hash = new FileDeletor(File.createTempFile("reg", ".data", dir));
				FileDeletor hashO = new FileDeletor(File.createTempFile("ofl", ".data", dir))) {
			bf.open(reg.f.getCanonicalPath(), "rw");
			hf.open(hash.f.getCanonicalPath(), "rw");
			hfO.open(hashO.f.getCanonicalPath(), "rw");
			NamedCombinedRecord ncr = Util.namedCombinedRecordFrom(reg.f.getCanonicalPath(), "Name");
			NamedCombinedRecord ncrH = Util.namedCombinedRecordFrom(hash.f.getCanonicalPath(), "Name");
			NamedCombinedRecord ncrO = Util.namedCombinedRecordFrom(hashO.f.getCanonicalPath(), "Name");
			try {
				Util.generateHash(dbbuf, hf, hfO, 0.8, 13).close();
				// Note: generateHash uses the dbbufer with hf, hfO, which will be closed as soon as this scope is left.
				// to make sure we do not die here, we flush the dbbuffer after generating this hash
				dbbuf.flush();
			} catch(BufferFullException bfe) {
				throw new RuntimeException("Setup-problems: " , bfe);
			}
			ncr = ncr.combine(Util.namedCombinedRecordFrom(name, "Rel"));
			ncrH = ncrH.combine(Util.namedCombinedRecordFrom(name, "Rel"));
			ncrO = ncrO.combine(Util.namedCombinedRecordFrom(name, "Rel"));
			ncr = ncr.combine(Util.namedCombinedRecordFrom("TID", "Type"));
			ncrH = ncrH.combine(Util.namedCombinedRecordFrom("IDX_"+idxOfPrimary, "Type"));
			ncrO = ncrO.combine(Util.namedCombinedRecordFrom("IDX_Overflow_"+idxOfPrimary, "Type"));
			ncr = ncr.combine(Util.namedCombinedRecordFrom(blockSize, "BlockSize"));
			ncrH = ncrH.combine(Util.namedCombinedRecordFrom(blockSize, "BlockSize"));
			ncrO = ncrO.combine(Util.namedCombinedRecordFrom(blockSize, "BlockSize"));
			//System.out.println("T1:" + ncr);
			insertFileNCR(ncr);
			reg.clear();
			try{
				//System.out.println("T2:" + ncrO);
				insertFileNCR(ncrO);
				hashO.clear();
				//System.out.println("T3:" + ncrH);
				insertFileNCR(ncrH);
				hash.clear();
				for(int i=0; i < types.size(); ++i){
					ncr = Util.namedCombinedRecordFrom(columns.get(i), "Name");
					ncr = ncr.combine(Util.namedCombinedRecordFrom(name, "Rel"));
					String typeS;
					switch(types.get(i)) {
						case INT: typeS = "I"; break;
						case STRING: typeS = "S"; break;
						case BOOL: typeS = "B"; break;
						default: throw new RuntimeException("Unknown Type: "+ types.get(i));
					}
					ncr = ncr.combine(Util.namedCombinedRecordFrom(typeS, "Type"));
					ncr = ncr.combine(Util.namedCombinedRecordFrom(i, "Pos"));
					boolean isPrimary = (i == idxOfPrimary);
					ncr = ncr.combine(Util.namedCombinedRecordFrom(isPrimary, "Primary-Key"));
					ncr = ncr.combine(Util.namedCombinedRecordFrom(isPrimary && primaryIsSurrogate, "Surrogate-Key"));
					insertAttrNCR(ncr);
				}
			}
			catch(Exception e){
				e.printStackTrace();
				dbbuf.flush();
				dropRelation(name);
				throw e;
			}

		}
		catch(IOException ex) {
			throw new RuntimeException("Caught IOEx:", ex);
		}
	}

	// RAII wrapper for Files.
	private class FileDeletor implements AutoCloseable
	{
		public File f;
		public FileDeletor(File pf){
			f = pf;
		}

		public void close() throws IOException {
			if (f != null) f.delete();
		}

		public void clear(){
			f = null;
		}
	}

	private int freeAttrPos(String relName) {
		Module m = Util.generateTableScan(attr.view(), attrNCR());
		m = Util.generateSel(m, x -> x.getString("Rel").equals(relName));
		m = Util.generateGroup(m, List.of(Util.max("Pos")), "Rel");
		int res = m.pull().getInt("max(Pos)") + 1;
		m.close();
		return res;
	}

	public void addIntColumn(String relName, String colName, int defaultValue) {
		addColumn(relName, colName, "I", Util.namedCombinedRecordFrom(defaultValue, colName));
	}

	public void addStringColumn(String relName, String colName, String defaultValue) {
		addColumn(relName, colName, "S", Util.namedCombinedRecordFrom(defaultValue, colName));
	}

	public void addBoolColumn(String relName, String colName, boolean defaultValue) {
		addColumn(relName, colName, "B", Util.namedCombinedRecordFrom(defaultValue, colName));
	}

	private void addColumn(String relName, String colName, String typeString, NamedCombinedRecord defaultNCR){
		NamedCombinedRecord pkAttr = getPrimaryKeyInternal(relName);
		switch(getType(pkAttr)){
			case STRING: addColumn(relName, colName, getTIDFile(relName), x -> new DBString(x.getString(pkAttr.getString("Name"))), getStringIndex(relName, pkAttr.getString("Name")), typeString, defaultNCR); break;
			case INT: addColumn(relName, colName, getTIDFile(relName), x -> new DBInteger(x.getInt(pkAttr.getString("Name"))), getIntIndex(relName, pkAttr.getString("Name")), typeString, defaultNCR); break;
			case BOOL: addColumn(relName, colName, getTIDFile(relName), x -> new Bool(x.getBool(pkAttr.getString("Name"))), getBoolIndex(relName, pkAttr.getString("Name")), typeString, defaultNCR); break;
		}
	}

	private <K extends Key> void addColumn(String relName, String colName, DirectRecordFile<TID, NamedCombinedRecord> tidFile, Function<NamedCombinedRecord, K> mapper, KeyRecordFile<TID, K> krf, String typeString, NamedCombinedRecord defaultNCR) {
		if(getColumns(relName).contains(colName)) {
			throw new IllegalArgumentException("This column already exists");
		}
		//after that, we need to read the contens using the old NCR
		Module m = Util.generateTableScan(tidFile.view(), getNCR(relName));
		// we need to use a store as concurrent modification in tidFile will otherwise stop us
		m = Util.generateStore(m, dbbuf);

		NamedCombinedRecord ncr = Util.namedCombinedRecordFrom(colName, "Name");
		ncr = ncr.combine(Util.namedCombinedRecordFrom(relName, "Rel"));
		ncr = ncr.combine(Util.namedCombinedRecordFrom(typeString, "Type"));
		ncr = ncr.combine(Util.namedCombinedRecordFrom(freeAttrPos(relName), "Pos"));
		ncr = ncr.combine(Util.namedCombinedRecordFrom(false, "Primary-Key"));
		ncr = ncr.combine(Util.namedCombinedRecordFrom(false, "Surrogate-Key"));
		insertAttrNCR(ncr);

		for(NamedCombinedRecord data = m.pull(); data != null; data = m.pull()) {
			try{
				TID out = new TID(0, 0);
				krf.read(mapper.apply(data), out, 0);
				NamedCombinedRecord modData = data.combine(defaultNCR);
				tidFile.modify(out, modData);
			}
			catch(BufferFullException | IOException ex) {
				throw new RuntimeException("Caught Modification Exception", ex);
			}
			catch(DeletedRecordException dre) {
				throw new RuntimeException("This is impossible", dre);
			}
		}

		m.close();
	}

}
