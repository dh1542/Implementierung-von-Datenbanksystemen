/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.function.Predicate;

public class Cross implements Module {
	private Module left, right;
	private String leftN, rightN;
	private boolean init = true;
	private NamedCombinedRecord leftR;

	// LeftName may be ommited.
	public Cross(Module prvL, Module prvR, String leftName, String rightName) {
		left = prvL;
		right = prvR;
		leftN = leftName;
		rightN = rightName;
		assert leftN == null || !leftN.equals(rightN);
	}

	private void pullLeft() {
		leftR = left.pull();
		if (leftR == null) return;
		if (leftN != null)
			leftR = leftR.rename(x -> leftN+"."+x);
	}

	@Override
	public NamedCombinedRecord pull() {
		if (init) {
			pullLeft();
			init = false;
		}
		if (leftR == null) {
			return null;
		}
		NamedCombinedRecord rightR = right.pull();
		if (rightR != null)
			return leftR.combine(rightR.rename(x->rightN+"."+x));
		right.reset();
		pullLeft();
		return pull();
	}

	@Override
	public void reset() {
		left.reset();
		right.reset();
		init = true;
	}

	@Override
	public void close() {
		left.close();
		right.close();
	}
}
