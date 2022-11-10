/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.function.Predicate;

public class ProjImpl implements Module {
	private Module previous;
	private Predicate<String> projFilter;

	public ProjImpl(Module prv, Predicate<String> attr) {
		previous = prv;
		projFilter = attr;
	}

	@Override
	public NamedCombinedRecord pull() {
		NamedCombinedRecord ncr = previous.pull();
		if (ncr == null) return null;
		return ncr.get(projFilter);
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
