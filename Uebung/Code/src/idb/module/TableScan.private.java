/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;
import idb.record.View;

public class TableScan implements Module{
	private NamedCombinedRecord data;
	private View<NamedCombinedRecord> view;

	public TableScan(View<NamedCombinedRecord> nV, NamedCombinedRecord item) {
		view = nV;
		data = item.copy();
	}

	@Override
	public void reset() {
		view.restart();
	}
	@Override
	public void close() {
	}
	@Override
	public NamedCombinedRecord pull(){
		try{
			if (view.next(data)) return data.copy();
		} catch(Exception e) {
			throw new RuntimeException("This is an exception in TableScan: ", e);
		}
		return null;
	}
}
