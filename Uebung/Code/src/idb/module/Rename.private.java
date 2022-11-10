/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.function.Function;

public class Rename implements Module {
	private Module previous;
	private Function<String, String> remapper;

	public Rename(Module prv, Function<String, String> map) {
		previous = prv;
		remapper = map;
	}

	@Override
	public NamedCombinedRecord pull() {
		NamedCombinedRecord ncr = previous.pull();
		if ( ncr == null) return null;
		return ncr.rename(remapper);
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
