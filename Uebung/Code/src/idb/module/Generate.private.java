/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;
import java.util.function.Function;
public class Generate implements Module{
	private Module m;
	private Function<NamedCombinedRecord, NamedCombinedRecord> calc;

	public Generate(Module prev, Function<NamedCombinedRecord, NamedCombinedRecord> cl) {
		m = prev;
		calc = cl;
	}

	@Override
	public void reset() {
		m.reset();
	}
	@Override
	public void close() {
		m.close();
	}
	@Override
	public NamedCombinedRecord pull(){
		NamedCombinedRecord pulled = m.pull();
		if (pulled == null) return null;
		return pulled.combine(calc.apply(pulled));
	}
}
