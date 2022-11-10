/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.function.Predicate;

public class SelImpl implements Module {
	private Module previous;
	private Predicate<NamedCombinedRecord> selFilter;

	public SelImpl(Module prv, Predicate<NamedCombinedRecord> select) {
		previous = prv;
		selFilter = select;
	}

	@Override
	public NamedCombinedRecord pull() {
		NamedCombinedRecord ncr;
		do{
			ncr = previous.pull();
			if (ncr == null) return null;
		}
		while (!selFilter.test(ncr));
		return ncr;
	}

	@Override
	public void reset() {
		previous.reset();
	}

	@Override
	public void close() {
		previous.close();
	}
}
