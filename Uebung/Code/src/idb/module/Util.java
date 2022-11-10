/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

import java.util.Comparator;
public class Util{
	public static Comparator<NamedCombinedRecord> getComparator(String attr, boolean ascending){
		java.util.function.Function<Comparator<NamedCombinedRecord>, Comparator<NamedCombinedRecord>> map = java.util.function.Function.identity();
		if (ascending) map = Comparator::reversed;
		return map.apply((left, right) ->
			left.getType(attr) == NamedCombinedRecord.Type.INT ?
			(Integer.valueOf(left.getInt(attr)).compareTo(right.getInt(attr))) :
			(left.getType(attr) == NamedCombinedRecord.Type.STRING ?
				(left.getString(attr).compareTo(right.getString(attr))) :
				Comparator.comparingInt((NamedCombinedRecord f) -> f.getBool(attr)? 1 : 0).compare(left, right)));
	}
	public static Comparator<NamedCombinedRecord> getComparator(String[] attr, boolean ascending){
		Comparator<NamedCombinedRecord> cmp = (left, right) -> 0;
		for (String s: attr) {
			cmp = cmp.thenComparing(getComparator(s, ascending));
		}
		return cmp;
	}
}
