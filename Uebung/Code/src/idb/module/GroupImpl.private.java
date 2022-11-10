/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;
import idb.datatypes.NamedCombinedRecord;

import java.util.Comparator;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.List;

public class GroupImpl implements Module {
	private Module prev;
	private boolean start = true;
	private NamedCombinedRecord next = null;
	private Comparator<NamedCombinedRecord> comp;
	private List<BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> funcs;
	private List<NamedCombinedRecord> inits;
	private String[] vars;
	private List<String> gen;
	// Some notes: aggFuncs shall be (new, old) -> new, while in is the first new for this function
	// var needs to contain the agg names to keep them around.
	public GroupImpl(Module p, List<BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> aggFuncs, List<NamedCombinedRecord> in, List<String> generated, String ...var){
		prev = new OrderImpl(p, true, var);
		funcs = aggFuncs;
		inits = in;
		vars = var;
		gen = generated;
		comp = Util.getComparator(vars, true);
	}

	public void close() {
		prev.close();
	}

	public void reset() {
		prev.reset();
		start = true;
		next = null;
	}

	public NamedCombinedRecord pull() {
		if (start) {
			next = prev.pull();
			start = false;
		}
		if (next == null) {
			return null;
		}
		NamedCombinedRecord[] cur = new NamedCombinedRecord[inits.size()];
		for(int i = 0; i < cur.length; i++) {
			cur[i] = funcs.get(i).apply(inits.get(i), next);
		}
		NamedCombinedRecord nxt;
		do{
			nxt = prev.pull();
			if (nxt == null) break;
			if (comp.compare(next, nxt) != 0) break;
			for(int i = 0; i < cur.length; i++) {
				cur[i] = funcs.get(i).apply(cur[i], nxt);
			}
		}
		while(true);
		NamedCombinedRecord modified = next;
		next = nxt;
		for(int i = 0; i < cur.length; ++i){
			modified = modified.combine(cur[i]);
		}
		return modified.get(x->Stream.concat(Arrays.stream(vars),gen.stream()).anyMatch(x::equals));
	}
}
