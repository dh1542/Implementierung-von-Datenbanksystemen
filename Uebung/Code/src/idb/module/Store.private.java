/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;
import idb.record.View;
import idb.record.SeqRecordFile;
import idb.record.SeqRecordFileImpl;
import idb.block.BlockFile;
import idb.block.BlockImpl;
import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
public class Store implements Module{
	private Module m;
	private DBBuffer buf;
	private View<NamedCombinedRecord> v = null;
	private File f = null;
	private NamedCombinedRecord type = null;

	public Store(Module prev, DBBuffer cl) {
		m = prev;
		buf = cl;
	}

	@Override
	public void reset() {
		v.restart();
	}
	@Override
	public void close() {
		if (f != null)
			f.delete();
	}
	@Override
	public NamedCombinedRecord pull(){
		try{
			if (v == null) {
				f = File.createTempFile("store", null);
				BlockFile bf = new BlockImpl(buf.getPagesize());
				try{
					bf.open(f.getCanonicalPath(), "rw");
				}
				catch(FileNotFoundException fnfe)
				{
					throw new RuntimeException("This is stragne", fnfe);
				}
				SeqRecordFile<NamedCombinedRecord> srf = new SeqRecordFileImpl<>(buf, bf);
				NamedCombinedRecord pulled = m.pull();
				while(pulled != null) {
					type = pulled;
					srf.insert(pulled);
					pulled = m.pull();
				}
				v = srf.view();
				m.close();
			}
			if (type == null) {
				return null;
			}
			NamedCombinedRecord read = type.copy();
			if (v.next(read)) {
				return read;
			}
			else {
				return null;
			}
		} catch(IOException | BufferFullException exe) {
			throw new RuntimeException("Exception in Store", exe);
		}
	}
}
