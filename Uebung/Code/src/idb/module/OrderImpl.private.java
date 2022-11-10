/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.Comparator;
import java.util.function.Predicate;

public class OrderImpl implements Module {
	private Module previous;
	private int dupl = 0;
	private NamedCombinedRecord delivered = null;
	private Comparator<NamedCombinedRecord> cmp;


	public OrderImpl(Module prv, boolean asc, String ...attrs) {
		previous = prv;
		cmp = (left, right) -> 0;
		for(String attr : attrs) {
			cmp = cmp.thenComparing(Util.getComparator(attr, asc));
		}
	}

	@Override
	public NamedCombinedRecord pull() {
		Predicate<NamedCombinedRecord> allowed;
		if (delivered == null) {
			allowed = x -> true;
		}
		else if (dupl == 0){
			allowed = x -> cmp.compare(x, delivered) < 0;
		}
		else {
			allowed = x -> cmp.compare(x, delivered) == 0;
		}
		NamedCombinedRecord best = null;
		//dupl = 0;
		NamedCombinedRecord ncr;
		int newDupl = 0;
		do {
			ncr = previous.pull();
			if (ncr == null){
				break;
			}
			if (!allowed.test(ncr)) {continue; }
			boolean wasNull = false;
			if (best == null) {
				best = ncr;
				wasNull =  true;
			}
			if (dupl != 0 && newDupl == dupl - 1) {
				break;
			}
			if (wasNull) continue;
			if (cmp.compare(ncr, best) < 0) {
				continue;
			}
			if (cmp.compare(ncr, best) == 0) {
				newDupl++;
				best = ncr;
			}
			if (cmp.compare(ncr, best) > 0) {
				newDupl = 0;
				best = ncr;
			}
		} while(true);
		if (best == null) return null;
		dupl = newDupl;
		delivered = best;
		previous.reset();
		return delivered;
	}

	@Override
	public void reset() {
		previous.reset();
		delivered = null;
		dupl = 0;
	}

	@Override
	public void close() {
		previous.close();
	}
}
