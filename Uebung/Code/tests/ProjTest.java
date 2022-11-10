import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import idb.datatypes.NamedCombinedRecord;
import idb.datatypes.DataObject;
import idb.datatypes.Triplet;
import idb.datatypes.TID;
import idb.datatypes.DBString;

import idb.module.Module;

import idb.block.BlockFile;

import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;
import idb.record.DeletedRecordException;

import idb.record.DirectRecordFile;
import idb.record.KeyRecordFile;
import idb.record.View;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.File;

import idb.construct.Util;

class Util2 {
	public static Function<Integer, ? extends DBBuffer> clockBufferGenerator(int capacity) {
		return i -> Util.generateClockBuffer(i, capacity);
	}
}

class NInts implements NamedCombinedRecord
{
	private List<String> names;
	private ArrayList<Integer> values;
	private NInts() {
		names = new ArrayList<>();
		values = new ArrayList<>();
	}
	public NInts(int hundr, int ten, int one) {
		names = Arrays.asList("H", "T", "O");
		values = new ArrayList<>(Arrays.asList(hundr, ten, one));
	}
	public NInts(int val, String n) {
		names = Arrays.asList(n);
		values = new ArrayList<>();
		values.add(val);
	}
	@Override
	public NamedCombinedRecord combine(NamedCombinedRecord other) {
		if (other instanceof NInts) {
			NInts nother = (NInts) other;
			if (nother.names.stream().anyMatch(names::contains)) {
				throw new IllegalArgumentException("Duplicated Names");
			}
			NInts out = new NInts();
			out.values.addAll(this.values);
			out.names.addAll(this.names);
			out.values.addAll(nother.values);
			out.names.addAll(nother.names);
			return out;
		}
		else {
			assert false;
			return null;
		}
	}
	@Override
	public NamedCombinedRecord get(Predicate<String> keep) {
		NInts out = new NInts();
		for(int i=0; i < names.size(); i++){
			if (keep.test(names.get(i))) {
				out.names.add(names.get(i));
				out.values.add(values.get(i));
			}
		}
		if (out.names.size() == 0) throw new NoSuchElementException("No name fulfilled the predicate");
		return out;
	}
	@Override
	public boolean equals(Object other) {
		if (other instanceof NamedCombinedRecord) {
			NamedCombinedRecord nother = (NamedCombinedRecord) other;
			for (int i=0; i < names.size(); ++i){
				try{
					if (nother.getInt(names.get(i)) != getInt(names.get(i))) return false;
				} catch(IllegalStateException | NoSuchElementException nex) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	@Override
	public String toString() {
		String out = "";
		for(int i=0; i < names.size(); ++i){
			out += "("+names.get(i) + ":" +values.get(i)+")";
		}
		return out;
	}
	@Override
	public int hashCode() {
		return 1;
	}
	@Override
	public Type getType(String name){
		get(name);
		return Type.INT;
	}
	@Override
	public String getString(String name) {
		get(name);
		throw new IllegalStateException("This contains only ints");
	}
	@Override
	public boolean getBool(String name) {
		get(name);
		throw new IllegalStateException("This contains only ints");
	}
	@Override
	public int getInt(String name) {
		return ((NInts) get(name)).values.get(0);
	}
	@Override
	public NamedCombinedRecord rename(Function<String, String> renamer) {
		NInts out = new NInts();
		for (int i=0; i < names.size(); ++i){
			out.values.add(values.get(i));
			out.names.add(renamer.apply(names.get(i)));
		}
		return out;
	}

	@Override
	public NamedCombinedRecord modifyValues(NamedCombinedRecord ncr) {
		NInts nints = (NInts) ncr;
		if (nints.names.stream().anyMatch(x -> !names.contains(x))) throw new NoSuchElementException("Wrong Names");
		NInts out = new NInts();
		for(int i=0; i < values.size(); ++i){
			int idx = nints.names.indexOf(names.get(i));
			if (idx == -1) {
				out.values.add(values.get(i));
			}
			else {
				out.values.add(nints.values.get(idx));
			}
		}
		out.names = names;
		return out;
	}
	@Override
	public int size() {
		return values.size() * Integer.SIZE / Byte.SIZE;
	}
	@Override
	public NInts copy() {
		NInts out = new NInts();
		out.values.addAll(values);
		out.names.addAll(names);
		return out;
	}
	@Override
	public void write(int idx, ByteBuffer buf) {
		for(int x : values) {
			buf.putInt(idx, x);
			idx += Integer.SIZE / Byte.SIZE;
		}
	}
	@Override
	public void read(int idx, ByteBuffer buf) {
		values = new ArrayList<>();
		for(String s : names) {
			values.add(buf.getInt(idx));
			idx += Integer.SIZE / Byte.SIZE;
		}
	}
}
class NIntSource implements Module{
	int cnt = 0;
	int sum = 0;
	int max;
	public NIntSource(int m) {
		max = m;
	}
	@Override
	public void close() {
		assertTrue(cnt >= 0);
		cnt = -1;
	}
	@Override
	public void reset() {
		assertTrue(cnt >= 0);
		cnt = 0;
	}
	@Override
	public NamedCombinedRecord pull() {
		assertTrue(cnt >= 0);
		sum++;
		int cur = cnt++;
		if (cur >= max) {
			cur = max;
			return null;
		}
		return new NInts(cur/100, cur/10%10, cur%10);
	}

	public int getCnt() {
		return cnt;
	}
	public int getSum() {
		return sum;
	}
}
class NullSource implements Module{
	public NullSource() {}
	@Override
	public void close() {
	}
	@Override
	public void reset() {
	}
	@Override
	public NamedCombinedRecord pull() {
		return null;
	}
}
public class ProjTest {

	@Test
	public void hundrets(){
		Module m = Util.generateProj(new NIntSource(5), "H", "O");
		//Proj has to keep the order
		NamedCombinedRecord ncr = m.pull();
		assertEquals(0, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		final NamedCombinedRecord n = ncr;
		assertThrows(NoSuchElementException.class, () -> n.getInt("T"));
		ncr = m.pull();
		assertEquals(1, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		final NamedCombinedRecord na = ncr;
		assertThrows(NoSuchElementException.class, () -> na.getInt("T"));
		ncr = m.pull();
		assertEquals(2, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		final NamedCombinedRecord nam = ncr;
		assertThrows(NoSuchElementException.class, () -> nam.getInt("T"));
		ncr = m.pull();
		assertEquals(3, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		final NamedCombinedRecord name = ncr;
		assertThrows(NoSuchElementException.class, () -> name.getInt("T"));
		ncr = m.pull();
		assertEquals(4, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		final NamedCombinedRecord named = ncr;
		assertThrows(NoSuchElementException.class, () -> named.getInt("T"));
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void tens(){
		Module m = Util.generateProj(new NIntSource(50), "H", "T");
		NamedCombinedRecord ncr;
		//Proj has to keep the order
		for(int i=0; i < 10; i++) {
			ncr = m.pull();
			assertEquals(0, ncr.getInt("T"));
			assertEquals(0, ncr.getInt("H"));
			final NamedCombinedRecord n = ncr;
			assertThrows(NoSuchElementException.class, () -> n.getInt("O"));
		}
		for(int i=0; i < 10; i++) {
			ncr = m.pull();
			assertEquals(1, ncr.getInt("T"));
			assertEquals(0, ncr.getInt("H"));
			final NamedCombinedRecord na = ncr;
			assertThrows(NoSuchElementException.class, () -> na.getInt("O"));
		}
		for(int i=0; i < 10; i++) {
			ncr = m.pull();
			assertEquals(2, ncr.getInt("T"));
			assertEquals(0, ncr.getInt("H"));
			final NamedCombinedRecord nam = ncr;
			assertThrows(NoSuchElementException.class, () -> nam.getInt("O"));
		}
		for(int i=0; i < 10; i++) {
			ncr = m.pull();
			assertEquals(3, ncr.getInt("T"));
			assertEquals(0, ncr.getInt("H"));
			final NamedCombinedRecord name = ncr;
			assertThrows(NoSuchElementException.class, () -> name.getInt("O"));
		}
		for(int i=0; i < 10; i++) {
			ncr = m.pull();
			assertEquals(4, ncr.getInt("T"));
			assertEquals(0, ncr.getInt("H"));
			final NamedCombinedRecord named = ncr;
			assertThrows(NoSuchElementException.class, () -> named.getInt("O"));
		}
		for(int i=0; i < 10; i++) {
			assertNull(m.pull());
		}
		m.close();
	}
	@Test
	public void nullTest(){
		Module m = Util.generateProj(new NullSource(), "H", "T");
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void invalidTest(){
		Module m = Util.generateProj(new NIntSource(5), "S");
		assertThrows(NoSuchElementException.class, () -> m.pull());
		m.close();
	}
}

class SelTest {
	@Test
	public void oneResult(){
		Module m = Util.generateSel(new NIntSource(11), x -> x.getInt("T") > 0);
		NamedCombinedRecord ncr = m.pull();
		assertEquals(0, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(1, ncr.getInt("T"));
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void noResult(){
		Module m = Util.generateSel(new NIntSource(11), x -> x.getInt("H") > 0);
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void nullTest(){
		Module m = Util.generateSel(new NullSource(), x -> true);
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void multResults(){
		Module m = Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2
		assertTrue(results.add(m.pull())); //12
		assertTrue(results.add(m.pull())); //22
		assertNull(m.pull());
		assertNull(m.pull());
		assertEquals(Set.of(new NInts(0, 0, 2), new NInts(0, 1, 2), new NInts(0, 2, 2)), results);
		m.close();
	}
}

class RenTest {
	@Test
	public void nullTest(){
		Module m = Util.generateRename(new NullSource(), x -> x.equals("H")?"J":x);
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void renameTens(){
		Module m = Util.generateRename(new NIntSource(5), x -> x.equals("T")?"J":x);
		// Rename has to keep the order
		NamedCombinedRecord ncr = m.pull();
		assertEquals(0, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord n = ncr;
		assertThrows(NoSuchElementException.class, () -> n.getInt("T"));
		ncr = m.pull();
		assertEquals(1, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord na = ncr;
		assertThrows(NoSuchElementException.class, () -> na.getInt("T"));
		ncr = m.pull();
		assertEquals(2, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord nam = ncr;
		assertThrows(NoSuchElementException.class, () -> nam.getInt("T"));
		ncr = m.pull();
		assertEquals(3, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord name = ncr;
		assertThrows(NoSuchElementException.class, () -> name.getInt("T"));
		ncr = m.pull();
		assertEquals(4, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord named = ncr;
		assertThrows(NoSuchElementException.class, () -> named.getInt("T"));
		assertNull(m.pull());
		m.close();
	}
}

class CrossTest {
	@Test
	public void nullLTest(){
		Module m = Util.generateCross(new NullSource(), new NIntSource(5), "A", "B");
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void nullRTest(){
		Module m = Util.generateCross(new NIntSource(5), new NullSource(), "A", "B");
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void multResults(){
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateCross(mL, m2, "A", "B");
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2, 30
		assertTrue(results.add(m.pull())); //2, 31
		assertTrue(results.add(m.pull())); //12, 30
		assertTrue(results.add(m.pull())); //12, 31
		assertTrue(results.add(m.pull())); //22, 30
		assertTrue(results.add(m.pull())); //22, 31
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord left1 = new NInts(0, 0, 2).rename(x -> "A."+x);
		NamedCombinedRecord left2 = new NInts(0, 1, 2).rename(x -> "A."+x);
		NamedCombinedRecord left3 = new NInts(0, 2, 2).rename(x -> "A."+x);
		NamedCombinedRecord right1 = new NInts(0, 3, 0).rename(x -> "B."+x);
		NamedCombinedRecord right2 = new NInts(0, 3, 1).rename(x -> "B."+x);
		assertEquals(Set.of(left1.combine(right1), left1.combine(right2), left2.combine(right1), left2.combine(right2), left3.combine(right1), left3.combine(right2)), results);
		m.close();
	}
	@Test
	public void omitLeftName(){
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateCross(mL, m2,null , "B");
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2, 30
		assertTrue(results.add(m.pull())); //2, 31
		assertTrue(results.add(m.pull())); //12, 30
		assertTrue(results.add(m.pull())); //12, 31
		assertTrue(results.add(m.pull())); //22, 30
		assertTrue(results.add(m.pull())); //22, 31
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord left1 = new NInts(0, 0, 2);
		NamedCombinedRecord left2 = new NInts(0, 1, 2);
		NamedCombinedRecord left3 = new NInts(0, 2, 2);
		NamedCombinedRecord right1 = new NInts(0, 3, 0).rename(x -> "B."+x);
		NamedCombinedRecord right2 = new NInts(0, 3, 1).rename(x -> "B."+x);
		assertEquals(Set.of(right1.combine(left1), left1.combine(right2), left2.combine(right1), left2.combine(right2), left3.combine(right1), left3.combine(right2)), results);
		m.close();
	}

}

class OrderTest {
	@Test
	public void nullTest(){
		Module m = Util.generateOrder(new NullSource(), true, "H", "T", "O");
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void singleASC() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateOrder(Util.generateCross(mL, m2, "A", "B"), true, "B.T", "A.T", "B.O", "B.H");
		NamedCombinedRecord ncr;
		ncr = m.pull(); // 2, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(0, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 2, 31
		assertEquals(2, ncr.getInt("A.O"), ncr.toString());
		assertEquals(0, ncr.getInt("A.H"), ncr.toString());
		assertEquals(0, ncr.getInt("A.T"), ncr.toString());
		assertEquals(1, ncr.getInt("B.O"), ncr.toString());
		assertEquals(0, ncr.getInt("B.H"), ncr.toString());
		assertEquals(3, ncr.getInt("B.T"), ncr.toString());
		ncr = m.pull(); // 12, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(1, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 12, 31
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(1, ncr.getInt("A.T"));
		assertEquals(1, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 22, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(2, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 22, 31
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(2, ncr.getInt("A.T"));
		assertEquals(1, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void singleDESC() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateOrder(Util.generateCross(mL, m2, "A", "B"), false, "B.T", "A.T", "B.O", "B.H");
		NamedCombinedRecord ncr;
		ncr = m.pull(); // 22, 31
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(2, ncr.getInt("A.T"));
		assertEquals(1, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 22, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(2, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 12, 31
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(1, ncr.getInt("A.T"));
		assertEquals(1, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 12, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(1, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		ncr = m.pull(); // 2, 31
		assertEquals(2, ncr.getInt("A.O"), ncr.toString());
		assertEquals(0, ncr.getInt("A.H"), ncr.toString());
		assertEquals(0, ncr.getInt("A.T"), ncr.toString());
		assertEquals(1, ncr.getInt("B.O"), ncr.toString());
		assertEquals(0, ncr.getInt("B.H"), ncr.toString());
		assertEquals(3, ncr.getInt("B.T"), ncr.toString());
		ncr = m.pull(); // 2, 30
		assertEquals(2, ncr.getInt("A.O"));
		assertEquals(0, ncr.getInt("A.H"));
		assertEquals(0, ncr.getInt("A.T"));
		assertEquals(0, ncr.getInt("B.O"));
		assertEquals(0, ncr.getInt("B.H"));
		assertEquals(3, ncr.getInt("B.T"));
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void pair() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateOrder(Util.generateCross(mL, m2, "A", "B"), false, "A.T");
		NamedCombinedRecord left = new NInts(0,2,2).rename(x->"A."+x);
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		NamedCombinedRecord r1 = new NInts(0,3,0).rename(x->"B."+x);
		NamedCombinedRecord r2 = new NInts(0,3,1).rename(x->"B."+x);
		assertTrue(results.add(m.pull()));
		assertTrue(results.add(m.pull()));
		assertEquals(Set.of(r1.combine(left), left.combine(r2)), results);
		results.clear();
		left = new NInts(0,1,2).rename(x->"A."+x);
		assertTrue(results.add(m.pull()));
		assertTrue(results.add(m.pull()));
		assertEquals(Set.of(r1.combine(left), left.combine(r2)), results);
		results.clear();
		left = new NInts(0,0,2).rename(x->"A."+x);
		assertTrue(results.add(m.pull()));
		assertTrue(results.add(m.pull()));
		assertEquals(Set.of(r1.combine(left), left.combine(r2)), results);
		m.close();
	}
}

class GroupTest {
	@Test
	public void nullTest(){
		Module m = Util.generateGroup(new NullSource(), "H", "T", "O");
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void noFuncs() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateGroup(Util.generateCross(mL, m2, "A", "B"), "A.O", "A.T", "B.O", "B.T");
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2, 30
		assertTrue(results.add(m.pull())); //2, 31
		assertTrue(results.add(m.pull())); //12, 30
		assertTrue(results.add(m.pull())); //12, 31
		assertTrue(results.add(m.pull())); //22, 30
		assertTrue(results.add(m.pull())); //22, 31
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord left1 = new NInts(0, 0, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord left2 = new NInts(0, 1, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord left3 = new NInts(0, 2, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord right1 = new NInts(0, 3, 0).get(x -> !x.equals("H")).rename(x -> "B."+x);
		NamedCombinedRecord right2 = new NInts(0, 3, 1).get(x -> !x.equals("H")).rename(x -> "B."+x);
		assertEquals(Set.of(left1.combine(right1), left1.combine(right2), left2.combine(right1), left2.combine(right2), left3.combine(right1), left3.combine(right2)), results);
		m.close();
	}
	private static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> max(String name){
		return new Triplet<>(new NInts(Integer.MIN_VALUE, "max("+name+")"), "max("+name+")", (l, r) -> l.getInt("max("+name+")") >= r.getInt(name) ? l : new NInts(r.getInt(name), "max("+name+")"));
	}
	private static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> min(String name){
		return new Triplet<>(new NInts(Integer.MAX_VALUE, "min("+name+")"), "min("+name+")", (l, r) -> l.getInt("min("+name+")") <= r.getInt(name) ? l : new NInts(r.getInt(name), "min("+name+")"));
	}
	private static Triplet<NamedCombinedRecord, String, BiFunction<NamedCombinedRecord, NamedCombinedRecord, NamedCombinedRecord>> count(String name){
		return new Triplet<>(new NInts(0, "count("+name+")"), "count("+name+")", (l, r) -> new NInts(l.getInt("count("+name+")")+1, "count("+name+")"));
	}
	@Test
	public void lotsFuncs() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateGroup(Util.generateCross(mL, m2, "A", "B"), List.of(max("A.H"), min("B.H"), count("B.H")), "A.O", "A.T", "B.O");
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2, 30
		assertTrue(results.add(m.pull())); //2, 31
		assertTrue(results.add(m.pull())); //12, 30
		assertTrue(results.add(m.pull())); //12, 31
		assertTrue(results.add(m.pull())); //22, 30
		assertTrue(results.add(m.pull())); //22, 31
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord left1 = new NInts(0, 0, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord left2 = new NInts(0, 1, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord left3 = new NInts(0, 2, 2).get(x -> !x.equals("H")).rename(x -> "A."+x);
		NamedCombinedRecord right1 = new NInts(0, 3, 0).get(x -> !x.equals("H")).rename(x -> "B."+x);
		NamedCombinedRecord right2 = new NInts(0, 3, 1).get(x -> !x.equals("H")).rename(x -> "B."+x);
		NamedCombinedRecord add = new NInts(0, "max(A.H)").combine(new NInts(0, "min(B.H)")).combine(new NInts(1, "count(B.H)"));
		assertEquals(Set.of(left1.combine(right1).combine(add), add.combine(left1).combine(right2), add.combine(left2.combine(right1)), add.combine(left2).combine(right2), left3.combine(right1).combine(add), left3.combine(right2).combine(add)), results);
		m.close();
	}
	@Test
	public void trueGrouping() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m2 = Util.generateSel(new NIntSource(32), x -> x.getInt("T") == 3);
		Module m = Util.generateGroup(Util.generateCross(mL, m2, "A", "B"), List.of(max("B.O"), min("B.O"), count("B.H")),  "A.T");
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2, 30 //2, 31
		assertTrue(results.add(m.pull())); //12, 30  //12, 31
		assertTrue(results.add(m.pull())); //22, 30  //22, 31
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord left1 = new NInts(0, 0, 2).get(x -> x.equals("T")).rename(x -> "A."+x);
		NamedCombinedRecord left2 = new NInts(0, 1, 2).get(x -> x.equals("T")).rename(x -> "A."+x);
		NamedCombinedRecord left3 = new NInts(0, 2, 2).get(x -> x.equals("T")).rename(x -> "A."+x);
		NamedCombinedRecord add = new NInts(1, "max(B.O)").combine(new NInts(0, "min(B.O)")).combine(new NInts(2, "count(B.H)"));
		assertEquals(Set.of(left1.combine(add), add.combine(left2), add.combine(left3)), results);
		assertThrows(NoSuchElementException.class, () -> results.iterator().next().getInt("A.O"));
		assertThrows(NoSuchElementException.class, () -> results.iterator().next().getInt("A.H"));
		assertThrows(NoSuchElementException.class, () -> results.iterator().next().getInt("B.O"));
		assertThrows(NoSuchElementException.class, () -> results.iterator().next().getInt("B.H"));
		assertThrows(NoSuchElementException.class, () -> results.iterator().next().getInt("B.T"));
		m.close();
	}
}

class NamedCombinedRecordTest {
	@Test
	public void unbalancedNamesTypes(){
		assertThrows(Exception.class, () -> Util.getNCR(List.of(NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING), List.of("A", "B", "C")));
	}
	@Test
	public void checkTypes(){
		NamedCombinedRecord ncr = Util.getNCR(List.of(NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL, NamedCombinedRecord.Type.INT), List.of("Ai", "Bs", "Cb", "Di"));
		assertEquals(NamedCombinedRecord.Type.INT, ncr.getType("Ai"));
		assertEquals(NamedCombinedRecord.Type.STRING, ncr.getType("Bs"));
		assertEquals(NamedCombinedRecord.Type.BOOL, ncr.getType("Cb"));
		assertEquals(NamedCombinedRecord.Type.INT, ncr.getType("Di"));
		assertThrows(NoSuchElementException.class, () -> ncr.getType("E"));
	}
	@Test
	public void checkInt(){
		NamedCombinedRecord ncr = Util.namedCombinedRecordFrom(5, "A");
		assertEquals(NamedCombinedRecord.Type.INT, ncr.getType("A"));
		assertThrows(NoSuchElementException.class, () -> ncr.getType("E"));
		assertEquals(5, ncr.getInt("A"));
		assertThrows(IllegalStateException.class, () -> ncr.getString("A"));
	}
	@Test
	public void checkString(){
		NamedCombinedRecord ncr = Util.namedCombinedRecordFrom("5", "A");
		assertEquals(NamedCombinedRecord.Type.STRING, ncr.getType("A"));
		assertThrows(NoSuchElementException.class, () -> ncr.getType("E"));
		assertEquals("5", ncr.getString("A"));
		assertThrows(IllegalStateException.class, () -> ncr.getBool("A"));
	}
	@Test
	public void checkBool(){
		NamedCombinedRecord ncr = Util.namedCombinedRecordFrom(true, "A");
		assertEquals(NamedCombinedRecord.Type.BOOL, ncr.getType("A"));
		assertThrows(NoSuchElementException.class, () -> ncr.getType("E"));
		assertEquals(true, ncr.getBool("A"));
		assertThrows(IllegalStateException.class, () -> ncr.getString("A"));
	}
	@Test
	public void combineFails() {
		NamedCombinedRecord l = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord r = Util.namedCombinedRecordFrom(2, "A");
		assertThrows(IllegalArgumentException.class, () -> l.combine(r));
	}
	@Test
	public void combineFails2() {
		NamedCombinedRecord l = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord r = Util.namedCombinedRecordFrom("2", "A");
		assertThrows(IllegalArgumentException.class, () -> l.combine(r));
	}
	@Test
	public void combineUse(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
		ncr1.transfer(ncr2);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
	}
	@Test
	public void getFails() {
		NamedCombinedRecord l = Util.namedCombinedRecordFrom(5, "A");
		assertThrows(NoSuchElementException.class, () -> l.get("B"::equals));
	}
	@Test
	public void getUse(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord otherInts = Util.namedCombinedRecordFrom(10, "A").combine(Util.namedCombinedRecordFrom(50, "B"));
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3);
		NamedCombinedRecord ints = sum.get( x -> x.length() == 1);
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("A"));
		assertEquals(5, ints.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("B"));
		assertEquals(2, ints.getInt("B"));
		assertThrows(NoSuchElementException.class, () -> ints.getType("Ba"));
		otherInts.transfer(ints);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("A"));
		assertEquals(10, ints.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("B"));
		assertEquals(50, ints.getInt("B"));
		assertThrows(NoSuchElementException.class, () -> ints.getType("Ba"));
	}
	@Test
	public void combinedBool(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord ncr4 = Util.namedCombinedRecordFrom(true, "Bo");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3).combine(ncr4);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertTrue(sum.getBool("Bo"));
		assertThrows(NoSuchElementException.class, () -> sum.getBool("E"));
		assertThrows(IllegalStateException.class, () -> sum.getBool("A"));
	}
	@Test
	public void combinedInt(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord ncr4 = Util.namedCombinedRecordFrom(true, "Bo");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3).combine(ncr4);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertTrue(sum.getBool("Bo"));
		assertThrows(NoSuchElementException.class, () -> sum.getInt("E"));
		assertThrows(IllegalStateException.class, () -> sum.getInt("Ba"));
	}
	@Test
	public void combinedString(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord ncr4 = Util.namedCombinedRecordFrom(true, "Bo");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3).combine(ncr4);
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("A"));
		assertEquals(5, sum.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("B"));
		assertEquals(2, sum.getInt("B"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertEquals("Suppenkueche", sum.getString("Ba"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("E"));
		assertTrue(sum.getBool("Bo"));
		assertThrows(NoSuchElementException.class, () -> sum.getString("E"));
		assertThrows(IllegalStateException.class, () -> sum.getString("Bo"));
	}
	@Test
	public void rename(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord ncr4 = Util.namedCombinedRecordFrom(true, "Bo");
		NamedCombinedRecord sumU = ncr1.combine(ncr2).combine(ncr3).combine(ncr4);
		NamedCombinedRecord sum = sumU.rename(x -> x + "O");
		assertThrows(NoSuchElementException.class, () -> sum.getType("A"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("B"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("Ba"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("Bo"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("AO"));
		assertEquals(5, sum.getInt("AO"));
		assertEquals(NamedCombinedRecord.Type.INT, sum.getType("BO"));
		assertEquals(2, sum.getInt("BO"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("BaO"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("EO"));
		assertEquals("Suppenkueche", sum.getString("BaO"));
		assertEquals(NamedCombinedRecord.Type.STRING, sum.getType("BaO"));
		assertThrows(NoSuchElementException.class, () -> sum.getType("EO"));
		assertTrue(sum.getBool("BoO"));
		assertThrows(NoSuchElementException.class, () -> sum.getString("EO"));
		assertThrows(IllegalStateException.class, () -> sum.getString("BoO"));
		NamedCombinedRecord altNames = Util.namedCombinedRecordFrom(0, "_1");
		altNames = altNames.combine(Util.namedCombinedRecordFrom(0, "_2"));
		altNames = altNames.combine(Util.namedCombinedRecordFrom("", "_3"));
		altNames = altNames.combine(Util.namedCombinedRecordFrom(false, "_$"));

		sumU.transfer(altNames);
		assertEquals(5, altNames.getInt("_1"));
		assertEquals(2, altNames.getInt("_2"));
		assertEquals("Suppenkueche", altNames.getString("_3"));
		assertTrue(altNames.getBool("_$"));

		sum.transfer(altNames);
		assertEquals(5, altNames.getInt("_1"));
		assertEquals(2, altNames.getInt("_2"));
		assertEquals("Suppenkueche", altNames.getString("_3"));
		assertTrue(altNames.getBool("_$"));
	}
	@Test
	public void renameCopy(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom("Suppenkueche", "Ba");
		NamedCombinedRecord otherInts = Util.namedCombinedRecordFrom(10, "A").combine(Util.namedCombinedRecordFrom(50, "B"));
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3);
		NamedCombinedRecord ints = sum.get( x -> x.length() == 1);
		NamedCombinedRecord renamed = ints.rename( x-> x + "9");
		assertEquals(NamedCombinedRecord.Type.INT, renamed.getType("A9"));
		assertEquals(5, renamed.getInt("A9"));
		assertEquals(NamedCombinedRecord.Type.INT, renamed.getType("B9"));
		assertEquals(2, renamed.getInt("B9"));
		assertThrows(NoSuchElementException.class, () -> renamed.getType("B"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("A"));
		assertEquals(5, ints.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("B"));
		assertEquals(2, ints.getInt("B"));
		assertThrows(NoSuchElementException.class, () -> ints.getType("Ba"));
		otherInts.transfer(ints);
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("A"));
		assertEquals(10, ints.getInt("A"));
		assertEquals(NamedCombinedRecord.Type.INT, ints.getType("B"));
		assertEquals(50, ints.getInt("B"));
		assertThrows(NoSuchElementException.class, () -> ints.getType("Ba"));
		assertEquals(NamedCombinedRecord.Type.INT, renamed.getType("A9"));
		assertEquals(5, renamed.getInt("A9"));
		assertEquals(NamedCombinedRecord.Type.INT, renamed.getType("B9"));
		assertEquals(2, renamed.getInt("B9"));
	}
	@Test
	public void size(){
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(2, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom(25, "Ba");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3);
		assertEquals(3*Integer.SIZE / Byte.SIZE, sum.size());
	}

	@Test
	public void modValues() {
		NamedCombinedRecord ncr1 = Util.namedCombinedRecordFrom(5, "A");
		NamedCombinedRecord ncr2 = Util.namedCombinedRecordFrom(10, "B");
		NamedCombinedRecord ncr3 = Util.namedCombinedRecordFrom(14, "Ba");
		NamedCombinedRecord sum = ncr1.combine(ncr2).combine(ncr3);
		NamedCombinedRecord ncr4 = Util.namedCombinedRecordFrom(6, "B");
		NamedCombinedRecord ncr5 = Util.namedCombinedRecordFrom("7", "Ba");
		NamedCombinedRecord ncr6 = Util.namedCombinedRecordFrom(7, "Ba7");
		assertThrows(NoSuchElementException.class, () -> sum.modifyValues(ncr4.combine(ncr6)));
		assertEquals(5, sum.getInt("A"));
		assertEquals(10, sum.getInt("B"));
		assertEquals(14, sum.getInt("Ba"));
		assertThrows(IllegalStateException.class, () -> sum.modifyValues(ncr4.combine(ncr5)));
		assertEquals(5, sum.getInt("A"));
		assertEquals(10, sum.getInt("B"));
		assertEquals(14, sum.getInt("Ba"));
		NamedCombinedRecord mod = sum.modifyValues(ncr4);
		assertEquals(5, sum.getInt("A"));
		assertEquals(10, sum.getInt("B"));
		assertEquals(14, sum.getInt("Ba"));
		assertEquals(5, mod.getInt("A"));
		assertEquals(6, mod.getInt("B"));
		assertEquals(14, mod.getInt("Ba"));
		sum.transfer(mod);
		assertEquals(5, mod.getInt("A"));
		assertEquals(10, mod.getInt("B"));
		assertEquals(14, mod.getInt("Ba"));
	}
}

class TableScanTest{
	private Function<Integer, ? extends BlockFile> bfgenerator;
	private Function<Integer, ? extends DBBuffer> bufferGenerator;
	private File testFile;
	private BlockFile bf;
	private DBBuffer buf;
	private DirectRecordFile<TID,NamedCombinedRecord> tidF;
	@BeforeEach
	public void init() throws IOException{
		bfgenerator = x -> Util.generateBlockFile(x);
		bufferGenerator = Util2.clockBufferGenerator(200);
		testFile = File.createTempFile("foobar", null);
		int pageSize = 4096;
		bf = bfgenerator.apply(pageSize);
		bf.open(testFile.getCanonicalPath(), "rw");
		buf = bufferGenerator.apply(pageSize);
	}
	private void gen(NamedCombinedRecord ignored){
		tidF =  Util.<NamedCombinedRecord>generateTID(buf, bf);
	}
	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null) buf.close();
		buf = null;
		if (bf != null) bf.close();
		bf = null;
		testFile.delete();
	}
	@Test
	public void empty() throws Exception {
		gen(new NInts(0, 0, 0));
		Module m = Util.generateTableScan(tidF.view(), new NInts(0, 1, 5));
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void one() throws Exception {
		gen(new NInts(0, 0, 0));
		tidF.insert(new NInts(4, 8, 3));
		Module m = Util.generateTableScan(tidF.view(), new NInts(0, 1, 5));
		NamedCombinedRecord ncr = m.pull();
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertEquals(new NInts(4,8,3), ncr);
		m.close();
	}
	@Test
	public void oneRestart() throws Exception {
		gen(new NInts(0, 0, 0));
		tidF.insert(new NInts(4, 8, 3));
		Module m = Util.generateTableScan(tidF.view(), new NInts(0, 1, 5));
		assertNotNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		m.reset();
		NamedCombinedRecord ncr = m.pull();
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertEquals(new NInts(4,8,3), ncr);
		m.close();
	}
	@Test
	public void oneReuse() throws Exception {
		gen(new NInts(0, 0, 0));
		tidF.insert(new NInts(4, 8, 3));
		NInts start = new NInts(0, 1, 5);
		Module m = Util.generateTableScan(tidF.view(), start);
		NamedCombinedRecord ncr = m.pull();
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertEquals(new NInts(4,8,3), ncr);
		assertEquals(new NInts(0, 1, 5), start);
		m.close();
	}
	@Test
	public void many() throws Exception {
		gen(new NInts(0, 0, 0));
		HashSet<NInts> expResults = new HashSet<>();
		for(int i=0; i < 2_000; ++i){
			tidF.insert(new NInts(i, "name"));
			assertTrue(expResults.add(new NInts(i, "name")));
		}
		NInts start = new NInts(0, "name");
		HashSet<NamedCombinedRecord> trueRes = new HashSet<>();
		Module m = Util.generateTableScan(tidF.view(), start);
		do{
			NamedCombinedRecord ncr = m.pull();
			if (ncr == null) break;
			assertTrue(trueRes.add(ncr));
		}
		while(true);
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertEquals(expResults, trueRes);
		m.close();
	}
}

class GenerateTest {
	@Test
	public void nullTest(){
		Module m = Util.generateGen(new NullSource(), x -> new NInts(5, "newName"));
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void fakeGenerate() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m = Util.generateGen(mL, x -> new NInts(5, "newName"));
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2
		assertTrue(results.add(m.pull())); //12
		assertTrue(results.add(m.pull())); //22
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord r1 = new NInts(0, 0, 2);
		NamedCombinedRecord r2 = new NInts(0, 1, 2);
		NamedCombinedRecord r3 = new NInts(0, 2, 2);
		NamedCombinedRecord add = new NInts(5, "newName");
		assertEquals(Set.of(r1.combine(add), r2.combine(add), r3.combine(add)), results);
		results.iterator().next().getInt("newName");
		m.close();
	}
	@Test
	public void trueGenerate() {
		Module mL= Util.generateSel(new NIntSource(30), x -> x.getInt("O") == 2);
		Module m = Util.generateGen(mL, x -> new NInts(x.getInt("O")+x.getInt("T"), "newName"));
		HashSet<NamedCombinedRecord> results = new HashSet<>();
		assertTrue(results.add(m.pull())); //2
		assertTrue(results.add(m.pull())); //12
		assertTrue(results.add(m.pull())); //22
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		assertNull(m.pull());
		NamedCombinedRecord r1 = new NInts(0, 0, 2);
		NamedCombinedRecord r2 = new NInts(0, 1, 2);
		NamedCombinedRecord r3 = new NInts(0, 2, 2);
		assertEquals(Set.of(r1.combine(new NInts(2, "newName")), r2.combine(new NInts(3, "newName")), r3.combine(new NInts(4, "newName"))), results);
		results.iterator().next().getInt("newName");
		m.close();
	}
}

class StoreTest {
	private DBBuffer buf;
	private Function<Integer, ? extends DBBuffer> bufferGenerator;
	@BeforeEach
	public void init() throws IOException{
		bufferGenerator = Util2.clockBufferGenerator(20);
		int pageSize = 4096;
		buf = bufferGenerator.apply(pageSize);
	}
	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null) buf.close();
	}
	@Test
	public void nullTest(){
		Module m = Util.generateStore(new NullSource(), buf);
		assertNull(m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void keepOrder(){
		Module m = Util.generateStore(Util.generateRename(new NIntSource(5), x -> x.equals("T")?"J":x), buf);
		// Rename && store have to keep the order
		NamedCombinedRecord ncr = m.pull();
		assertEquals(0, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord n = ncr;
		assertThrows(NoSuchElementException.class, () -> n.getInt("T"));
		ncr = m.pull();
		assertEquals(1, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord na = ncr;
		assertThrows(NoSuchElementException.class, () -> na.getInt("T"));
		ncr = m.pull();
		assertEquals(2, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord nam = ncr;
		assertThrows(NoSuchElementException.class, () -> nam.getInt("T"));
		ncr = m.pull();
		assertEquals(3, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord name = ncr;
		assertThrows(NoSuchElementException.class, () -> name.getInt("T"));
		ncr = m.pull();
		assertEquals(4, ncr.getInt("O"));
		assertEquals(0, ncr.getInt("H"));
		assertEquals(0, ncr.getInt("J"));
		final NamedCombinedRecord named = ncr;
		assertThrows(NoSuchElementException.class, () -> named.getInt("T"));
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void closeTest(){
		NIntSource nis = new NIntSource(5);
		Module m = Util.generateStore(nis, buf);
		assertEquals(new NInts(0, 0, 0),m.pull());
		assertEquals(-1, nis.getCnt()); // store has to eagerly call close
		assertEquals(new NInts(0, 0, 1),m.pull()); // if this would call nis.pull(), it would throw
		assertEquals(new NInts(0, 0, 2),m.pull());
		assertEquals(new NInts(0, 0, 3),m.pull());
		assertEquals(new NInts(0, 0, 4),m.pull());
		assertNull(m.pull());
		m.close();
	}
	@Test
	public void reuseTest(){
		NIntSource nis = new NIntSource(5);
		Module m = Util.generateStore(nis, buf);
		NamedCombinedRecord ncr0 = m.pull();
		assertEquals(-1, nis.getCnt()); // store has to eagerly call close
		NamedCombinedRecord ncr1 = m.pull();
		NamedCombinedRecord ncr2 = m.pull();
		NamedCombinedRecord ncr3 = m.pull();
		NamedCombinedRecord ncr4 = m.pull();
		assertNull(m.pull());
		assertEquals(new NInts(0, 0, 0), ncr0);
		assertEquals(new NInts(0, 0, 1), ncr1);
		assertEquals(new NInts(0, 0, 2), ncr2);
		assertEquals(new NInts(0, 0, 3), ncr3);
		assertEquals(new NInts(0, 0, 4), ncr4);
		m.close();
	}
	@Test
	public void oversized() throws Exception{
		buf.close();
		buf = bufferGenerator.apply(8192);
		NIntSource nis = new NIntSource(5);
		Module m = Util.generateStore(nis, buf);
		assertEquals(0, nis.getSum()); // store may not modify anything on construction
		NamedCombinedRecord ncr0 = m.pull();
		assertEquals(-1, nis.getCnt()); // store has to eagerly call close
		NamedCombinedRecord ncr1 = m.pull();
		NamedCombinedRecord ncr2 = m.pull();
		NamedCombinedRecord ncr3 = m.pull();
		NamedCombinedRecord ncr4 = m.pull();
		assertNull(m.pull());
		assertEquals(new NInts(0, 0, 0), ncr0);
		assertEquals(new NInts(0, 0, 1), ncr1);
		assertEquals(new NInts(0, 0, 2), ncr2);
		assertEquals(new NInts(0, 0, 3), ncr3);
		assertEquals(new NInts(0, 0, 4), ncr4);
		m.close();
	}
}

class CombinedModuleTest{
	private Function<Integer, ? extends BlockFile> bfgenerator;
	private Function<Integer, ? extends DBBuffer> bufferGenerator;
	private List<File> testFiles;
	private List<BlockFile> bfs;
	private DBBuffer buf;
	private List<DirectRecordFile<TID,NamedCombinedRecord>> tidFs;
	// This test performs tests on following TID-Files:
	// (1): Name: String, First Name: String, MatrNR: Int, Subject: String, PostalCode: Int, City: String, Email: String, Male: Bool
	// (2): MatrNr: Int, PrfNr: Int, Date: String, Result: Int
	@BeforeEach
	public void init() throws IOException{
		bfgenerator = x -> Util.generateBlockFile(x);
		bufferGenerator = Util2.clockBufferGenerator(200);
		testFiles = new ArrayList<>(2);
		bfs = new ArrayList<>(2);
		tidFs = new ArrayList<>(2);
		int pageSize = 4096;
		for(int i=0; i < 2; i++) {
			File cur = File.createTempFile("tid", null);
			testFiles.add(cur);
			BlockFile bf = bfgenerator.apply(pageSize);
			bf.open(cur.getCanonicalPath(), "rw");
			bfs.add(bf);
		}
		buf = bufferGenerator.apply(pageSize);
	}
	private void gen(NamedCombinedRecord type1, NamedCombinedRecord type2){
		tidFs.add(Util.<NamedCombinedRecord>generateTID(buf, bfs.get(0)));
		tidFs.add(Util.<NamedCombinedRecord>generateTID(buf, bfs.get(1)));
	}
	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null) buf.close();
		buf = null;
		for(BlockFile bf: bfs){
			bf.close();
		}
		bfs = List.of();
		for(File f: testFiles){
			f.delete();
		}
	}

	private void insertUeIDB09Data() throws Exception{
		NamedCombinedRecord left = Util.namedCombinedRecordFrom("Pumpernickel", "Name").combine(Util.namedCombinedRecordFrom("Heinrich", "First Name"))
			.combine(Util.namedCombinedRecordFrom(12345, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Sport", "Subject"))
			.combine(Util.namedCombinedRecordFrom(96050, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Bamberg", "City"))
			.combine(Util.namedCombinedRecordFrom("hini@pumper.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		NamedCombinedRecord right = Util.namedCombinedRecordFrom(12345, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(1, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("21.09.1994", "Date"))
			.combine(Util.namedCombinedRecordFrom(13, "Result"));
		gen(left.copy(), right.copy());
		tidFs.get(0).insert(left);
		tidFs.get(1).insert(right);
		left = Util.namedCombinedRecordFrom("Einstein", "Name")
			.combine(Util.namedCombinedRecordFrom("Albert", "First Name"))
			.combine(Util.namedCombinedRecordFrom(23451, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Physik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90443, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Nürnberg", "City"))
			.combine(Util.namedCombinedRecordFrom("ali1stein@gmail.com", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("de Brudzewo", "Name")
			.combine(Util.namedCombinedRecordFrom("Albert", "First Name"))
			.combine(Util.namedCombinedRecordFrom(34512, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Mathematik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91055, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("albert.brudzewo@fau.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Michael", "Name")
			.combine(Util.namedCombinedRecordFrom("Elisa", "First Name"))
			.combine(Util.namedCombinedRecordFrom(88150, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Archäologie", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("elisa.michael@robotics-erlangen.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Curie", "Name")
			.combine(Util.namedCombinedRecordFrom("Marie", "First Name"))
			.combine(Util.namedCombinedRecordFrom(45123, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Physik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("mary.c@yahoo.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Stadelmann", "Name")
			.combine(Util.namedCombinedRecordFrom("Michael", "First Name"))
			.combine(Util.namedCombinedRecordFrom(22239, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Maschinenbau", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("m.stadelmann@fau.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("von Brabant", "Name")
			.combine(Util.namedCombinedRecordFrom("Prinzessin Eleonora", "First Name"))
			.combine(Util.namedCombinedRecordFrom(22489, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Informations- & Kommunikationstechnik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90513, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Zirndorf", "City"))
			.combine(Util.namedCombinedRecordFrom("eli@von-brabant.com", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Tieger", "Name")
			.combine(Util.namedCombinedRecordFrom("Tilly", "First Name"))
			.combine(Util.namedCombinedRecordFrom(32168, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("MoS - Mäusefangen ohne Stuhlverlassen", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91054, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Buckenhof", "City"))
			.combine(Util.namedCombinedRecordFrom("tilly.stubentieger@informatik.studium.fau.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Hamilton", "Name")
			.combine(Util.namedCombinedRecordFrom("Margaret", "First Name"))
			.combine(Util.namedCombinedRecordFrom(51234, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Informatik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90443, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Nürnberg", "City"))
			.combine(Util.namedCombinedRecordFrom("ham.ma@hotmail.com", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Lavoisier", "Name")
			.combine(Util.namedCombinedRecordFrom("Marie", "First Name"))
			.combine(Util.namedCombinedRecordFrom(54321, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Chemie", "Subject"))
			.combine(Util.namedCombinedRecordFrom(96052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Bamberg", "City"))
			.combine(Util.namedCombinedRecordFrom("marie.lavoisier@studium.fau.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Cavendish", "Name")
			.combine(Util.namedCombinedRecordFrom("Margaret", "First Name"))
			.combine(Util.namedCombinedRecordFrom(43215, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Chemie", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90763, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Nürnberg", "City"))
			.combine(Util.namedCombinedRecordFrom("cavendish.ma@icloud.com", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Dürer", "Name")
			.combine(Util.namedCombinedRecordFrom("Albrecht", "First Name"))
			.combine(Util.namedCombinedRecordFrom(32154, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Mathematik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90408, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Nürnberg", "City"))
			.combine(Util.namedCombinedRecordFrom("me@duerer.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Raymond", "Name")
			.combine(Util.namedCombinedRecordFrom("Jade", "First Name"))
			.combine(Util.namedCombinedRecordFrom(21543, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Informatik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(96052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Bamberg", "City"))
			.combine(Util.namedCombinedRecordFrom("jade@myspace.com", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Hopper", "Name")
			.combine(Util.namedCombinedRecordFrom("Grace", "First Name"))
			.combine(Util.namedCombinedRecordFrom(15432, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Informatik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91058, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("g.h@navy.gov", "Email"))
			.combine(Util.namedCombinedRecordFrom(false, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Legmann", "Name")
			.combine(Util.namedCombinedRecordFrom("Wukas", "First Name"))
			.combine(Util.namedCombinedRecordFrom(21161, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91058, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("wukas.legmann@navy.gov", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Turing", "Name")
			.combine(Util.namedCombinedRecordFrom("Alan", "First Name"))
			.combine(Util.namedCombinedRecordFrom(13524, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Informatik", "Subject"))
			.combine(Util.namedCombinedRecordFrom(90449, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Nürnberg", "City"))
			.combine(Util.namedCombinedRecordFrom("616c616e@547572696e670d0a.uk", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		left = Util.namedCombinedRecordFrom("Köch", "Name")
			.combine(Util.namedCombinedRecordFrom("Tobias", "First Name"))
			.combine(Util.namedCombinedRecordFrom(32486, "MatrNR"))
			.combine(Util.namedCombinedRecordFrom("Maschinenbau", "Subject"))
			.combine(Util.namedCombinedRecordFrom(91052, "PostalCode"))
			.combine(Util.namedCombinedRecordFrom("Erlangen", "City"))
			.combine(Util.namedCombinedRecordFrom("koch.tobias@fau.de", "Email"))
			.combine(Util.namedCombinedRecordFrom(true, "Male"));
		tidFs.get(0).insert(left);
		right = Util.namedCombinedRecordFrom(12345, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(2, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("29.09.1994", "Date"))
			.combine(Util.namedCombinedRecordFrom(23, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(23451, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(3, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("29.04.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(23451, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(4, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("29.09.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(43, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(23451, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(4, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("29.01.1905", "Date"))
			.combine(Util.namedCombinedRecordFrom(27, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(23451, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(5, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.03.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(23451, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(10, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("17.03.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(17, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(34512, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(6, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("17.03.1445", "Date"))
			.combine(Util.namedCombinedRecordFrom(27, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(34512, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(7, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("18.03.1445", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(34512, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(8, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("20.03.1445", "Date"))
			.combine(Util.namedCombinedRecordFrom(17, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(34512, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(9, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("20.07.1445", "Date"))
			.combine(Util.namedCombinedRecordFrom(10, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(45123, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(4, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("29.09.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(13, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(45123, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(5, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.03.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(45123, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(10, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("17.03.1904", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(51234, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(13, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.01.1924", "Date"))
			.combine(Util.namedCombinedRecordFrom(27, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(51234, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(12, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("17.05.1924", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(54321, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(14, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("19.05.1774", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(54321, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(15, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.05.1775", "Date"))
			.combine(Util.namedCombinedRecordFrom(37, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(54321, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(16, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("12.05.1775", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(54321, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(17, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("10.04.1775", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(32154, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(18, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("10.02.1500", "Date"))
			.combine(Util.namedCombinedRecordFrom(13, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(32154, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(19, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.02.1500", "Date"))
			.combine(Util.namedCombinedRecordFrom(17, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(32154, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(20, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.02.1500", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(32154, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(21, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.03.1500", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(32154, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(22, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.02.1501", "Date"))
			.combine(Util.namedCombinedRecordFrom(40, "Result"));
		tidFs.get(1).insert(right);

		right = Util.namedCombinedRecordFrom(21543, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(23, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.02.1980", "Date"))
			.combine(Util.namedCombinedRecordFrom(40, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(21543, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(24, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("14.07.1980", "Date"))
			.combine(Util.namedCombinedRecordFrom(43, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(21543, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(24, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("24.02.1981", "Date"))
			.combine(Util.namedCombinedRecordFrom(23, "Result"));
		tidFs.get(1).insert(right);

		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(25, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("24.02.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(23, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(26, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("12.03.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(33, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(27, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("10.03.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(30, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(28, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("13.09.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(27, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(29, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("10.09.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
		right = Util.namedCombinedRecordFrom(13524, "MatrNr")
			.combine(Util.namedCombinedRecordFrom(30, "PrfNr"))
			.combine(Util.namedCombinedRecordFrom("19.09.1933", "Date"))
			.combine(Util.namedCombinedRecordFrom(20, "Result"));
		tidFs.get(1).insert(right);
	}
	@Test
	public void query1() throws Exception{
		// This query is:
		// A := Scan(1)
		// B := Scan(2)
		// C := Cross(A, B, "Stud", "Results")
		// D := Store(C)
		// E := Sel(D, Stud.MatrNR == Results.MatrNr)
		// F := Group(E, List.of(min("Results.Result"), count()), "Stud.Name", "Stud.First Name", "Stud.Subject", "Stud.City", "Stud.Email")
		// G := Sel(F, count() >= 3)
		// H := GEN(G, x -> namedCombinedRecordFrom(x.getString("Stud.First Name") + x.getString("Stud.Name"), "Full Name"))
		// K := REN(H, x -> x.startsWith("Stud.") ? x.split(".")[1] : x)
		// I := PROJ(K, "min(Results.Result)", "Full Name", "Subject", "City", "Email");
		// L := Order(I, "min(Results.Result)", "Full Name");
		//
		// Results:
		// 12345 has to few results
		// 23451 : 1.7
		// 34512 : 1.0
		// 45123: 1.3
		// 51234 has to few results
		// 54321 : 3.3
		// 43215 has no results
		// 32154 : 1.3
		// 21543 : 2.3
		// 15432 has no results
		// 13524 : 2.0
		insertUeIDB09Data();
	// (1): Name: String, First Name: String, MatrNR: Int, Subject: String, PostalCode: Int, City: String, Email: String, Male: Bool
	// (2): MatrNr: Int, PrfNr: Int, Date: String, Result: Int
		Module a = Util.generateTableScan(tidFs.get(0).view(), Util.getNCR(List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), List.of("Name", "First Name", "MatrNR", "Subject", "PostalCode", "City", "Email", "Male")));
		Module b = Util.generateTableScan(tidFs.get(1).view(), Util.getNCR(List.of(NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT), List.of("MatrNr", "PrfNr", "Date", "Result")));
		Module c = Util.generateCross(a, b, "Stud", "Results");
		Module d = Util.generateStore(c, buf);
		Module e = Util.generateSel(d, x -> x.getInt("Stud.MatrNR") == x.getInt("Results.MatrNr"));
		Module f = Util.generateGroup(e, List.of(Util.count(), Util.min("Results.Result")), "Stud.Name", "Stud.First Name", "Stud.Subject", "Stud.City", "Stud.Email");
		Module g = Util.generateSel(f, x -> x.getInt("count()") >= 3);
		Module h = Util.generateGen(g, x -> Util.namedCombinedRecordFrom(x.getString("Stud.First Name")+" "+x.getString("Stud.Name"), "Full Name"));
		Module km = Util.generateRename(h, x -> x.startsWith("Stud.") ? x.split("\\.")[1] : x);
		Module k = Util.generateStore(km, buf);
		Module i = Util.generateProj(k, "min(Results.Result)", "Full Name", "Subject", "City", "Email");
		Module l = Util.generateOrder(i, true, "min(Results.Result)", "Full Name");
		NamedCombinedRecord ncr = l.pull();
		assertNotNull(ncr); // de Brudzewo
		assertEquals(10, ncr.getInt("min(Results.Result)"));
		assertEquals("Albert de Brudzewo", ncr.getString("Full Name"));
		assertEquals("Mathematik", ncr.getString("Subject"));
		assertEquals("Erlangen", ncr.getString("City"));
		assertEquals("albert.brudzewo@fau.de", ncr.getString("Email"));
		ncr = l.pull(); //Albrecht Dürer
		assertNotNull(ncr);
		assertEquals(13, ncr.getInt("min(Results.Result)"));
		assertEquals("Albrecht Dürer", ncr.getString("Full Name"));
		assertEquals("Mathematik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("me@duerer.de", ncr.getString("Email"));
		ncr = l.pull(); //Marie Curie
		assertNotNull(ncr);
		assertEquals(13, ncr.getInt("min(Results.Result)"));
		assertEquals("Marie Curie", ncr.getString("Full Name"));
		assertEquals("Physik", ncr.getString("Subject"));
		assertEquals("Erlangen", ncr.getString("City"));
		assertEquals("mary.c@yahoo.de", ncr.getString("Email"));
		ncr = l.pull(); //Albert Einstein
		assertNotNull(ncr);
		assertEquals(17, ncr.getInt("min(Results.Result)"));
		assertEquals("Albert Einstein", ncr.getString("Full Name"));
		assertEquals("Physik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("ali1stein@gmail.com", ncr.getString("Email"));
		ncr = l.pull(); //Alan Turing
		assertNotNull(ncr);
		assertEquals(20, ncr.getInt("min(Results.Result)"));
		assertEquals("Alan Turing", ncr.getString("Full Name"));
		assertEquals("Informatik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("616c616e@547572696e670d0a.uk", ncr.getString("Email"));
		ncr = l.pull(); //Jade Raymond
		assertNotNull(ncr);
		assertEquals(23, ncr.getInt("min(Results.Result)"));
		assertEquals("Jade Raymond", ncr.getString("Full Name"));
		assertEquals("Informatik", ncr.getString("Subject"));
		assertEquals("Bamberg", ncr.getString("City"));
		assertEquals("jade@myspace.com", ncr.getString("Email"));
		ncr = l.pull(); //Marie Lavoisier
		assertNotNull(ncr);
		assertEquals(33, ncr.getInt("min(Results.Result)"));
		assertEquals("Marie Lavoisier", ncr.getString("Full Name"));
		assertEquals("Chemie", ncr.getString("Subject"));
		assertEquals("Bamberg", ncr.getString("City"));
		assertEquals("marie.lavoisier@studium.fau.de", ncr.getString("Email"));
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
		l.close();
	}
}

class DeleteTest {

	private List<BlockFile> blockFiles;
	private List<File> testFiles;
	private DBBuffer buf;
	private DirectRecordFile<TID, NamedCombinedRecord> tidFile;
	private KeyRecordFile<TID, DBString> hashFile;
	@BeforeEach
	public void init() throws Exception{
		testFiles = new ArrayList<>(3);
		blockFiles = new ArrayList<>(3);
		//tidFs = new ArrayList<>(2);
		int pageSize = 4096;
		for(int i=0; i < 3; i++) {
			File cur = File.createTempFile("delete-test", null);
			testFiles.add(cur);
			BlockFile bf = Util.generateBlockFile(pageSize);
			bf.open(cur.getCanonicalPath(), "rw");
			blockFiles.add(bf);
		}
		buf = Util.generateClockBuffer(pageSize, 200);
		tidFile = new ConcurrentModificationTIDFile(Util.generateTID(buf, blockFiles.get(0)));
		hashFile = Util.generateHash(buf, blockFiles.get(1), blockFiles.get(2), 0.75, 7);
		generateSampleData();
	}
	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null) buf.close();
		buf = null;
		for(BlockFile bf: blockFiles){
			bf.close();
		}
		blockFiles = List.of();
		for(File f: testFiles){
			f.delete();
		}
	}

	private void insert(NamedCombinedRecord ncr, String key) throws Exception{
		TID cur = tidFile.insert(ncr);
		hashFile.insert(new DBString(key), cur);
	}

	private void generateSampleData() throws Exception {
		NamedCombinedRecord ncr;
		ncr = Util.namedCombinedRecordFrom("SongsFromHome", "Titel")
			.combine(Util.namedCombinedRecordFrom("LastMinuteProductions", "Producer"))
			.combine(Util.namedCombinedRecordFrom(1996, "ReleaseYear"))
			.combine(Util.namedCombinedRecordFrom("Metal", "Genre"));
		insert(ncr, ncr.getString("Titel"));
		ncr = Util.namedCombinedRecordFrom("CelticSongs", "Titel")
			.combine(Util.namedCombinedRecordFrom("LastMinuteProductions", "Producer"))
			.combine(Util.namedCombinedRecordFrom(1998, "ReleaseYear"))
			.combine(Util.namedCombinedRecordFrom("Acoustic", "Genre"));
		insert(ncr, ncr.getString("Titel"));
		ncr = Util.namedCombinedRecordFrom("Celtic Cats", "Titel")
			.combine(Util.namedCombinedRecordFrom("Scotish Music", "Producer"))
			.combine(Util.namedCombinedRecordFrom(1999, "ReleaseYear"))
			.combine(Util.namedCombinedRecordFrom("Pop", "Genre"));
		insert(ncr, ncr.getString("Titel"));
	}

	private Module getScan() {
		Module m = Util.generateTableScan(tidFile.view(), Util.getNCR(List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING), List.of("Titel", "Producer", "ReleaseYear", "Genre")));
		return m;
	}

	@Test
	public void noDelete() throws Exception{
		Module m = getScan();
		m = Util.generateSel(m, x -> x.getString("Producer").equals("Invalid"));
		Util.deleteStringIndex(m, "Titel", tidFile, hashFile, buf);
		m = getScan();
		Module l = Util.generateOrder(m, true, "Genre");
		NamedCombinedRecord ncr = l.pull(); //CelticSongs
		assertNotNull(ncr);
		assertEquals(1998, ncr.getInt("ReleaseYear"));
		assertEquals("CelticSongs", ncr.getString("Titel"));
		assertEquals("Acoustic", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //SongsFromHome
		assertNotNull(ncr);
		assertEquals(1996, ncr.getInt("ReleaseYear"));
		assertEquals("SongsFromHome", ncr.getString("Titel"));
		assertEquals("Metal", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //CelticCats
		assertNotNull(ncr);
		assertEquals(1999, ncr.getInt("ReleaseYear"));
		assertEquals("Celtic Cats", ncr.getString("Titel"));
		assertEquals("Pop", ncr.getString("Genre"));
		assertEquals("Scotish Music", ncr.getString("Producer"));
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
	}

	@Test
	public void partDelete() throws Exception{
		Module m = getScan();
		m = Util.generateSel(m, x -> x.getString("Producer").equals("Scotish Music"));
		Util.deleteStringIndex(m, "Titel", tidFile, hashFile, buf);
		m = getScan();
		Module l = Util.generateOrder(m, true, "Genre");
		NamedCombinedRecord ncr = l.pull(); //CelticSongs
		assertNotNull(ncr);
		assertEquals(1998, ncr.getInt("ReleaseYear"));
		assertEquals("CelticSongs", ncr.getString("Titel"));
		assertEquals("Acoustic", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //SongsFromHome
		assertNotNull(ncr);
		assertEquals(1996, ncr.getInt("ReleaseYear"));
		assertEquals("SongsFromHome", ncr.getString("Titel"));
		assertEquals("Metal", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //CelticCats is deleted
		assertNull(ncr);
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
	}

	@Test
	public void fullDelete() throws Exception{
		Module m = getScan();
		m = Util.generateSel(m, x -> true);
		Util.deleteStringIndex(m, "Titel", tidFile, hashFile, buf);
		m = getScan();
		Module l = Util.generateOrder(m, true, "Genre");
		NamedCombinedRecord ncr = l.pull(); //CelticSongs is deleted
		assertNull(ncr);
		ncr = l.pull(); //SongsFromHome is deleted
		assertNull(ncr);
		ncr = l.pull(); //CelticCats is deleted
		assertNull(ncr);
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
	}

	@Test
	public void projDelete() throws Exception{
		Module m = getScan();
		m = Util.generateProj(m, "Titel", "Producer");
		m = Util.generateSel(m, x -> x.getString("Producer").equals("Scotish Music"));
		m = Util.generateProj(m, "Titel");
		Util.deleteStringIndex(m, "Titel", tidFile, hashFile, buf);
		m = getScan();
		Module l = Util.generateOrder(m, true, "Genre");
		NamedCombinedRecord ncr = l.pull(); //CelticSongs
		assertNotNull(ncr);
		assertEquals(1998, ncr.getInt("ReleaseYear"));
		assertEquals("CelticSongs", ncr.getString("Titel"));
		assertEquals("Acoustic", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //SongsFromHome
		assertNotNull(ncr);
		assertEquals(1996, ncr.getInt("ReleaseYear"));
		assertEquals("SongsFromHome", ncr.getString("Titel"));
		assertEquals("Metal", ncr.getString("Genre"));
		assertEquals("LastMinuteProductions", ncr.getString("Producer"));
		ncr = l.pull(); //CelticCats is deleted
		assertNull(ncr);
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
	}
}

class ConcurrentModificationTIDFile implements DirectRecordFile<TID, NamedCombinedRecord> {

	private DirectRecordFile<TID, NamedCombinedRecord> drf;
	private boolean modified;

	public ConcurrentModificationTIDFile(DirectRecordFile<TID, NamedCombinedRecord> pdrf){
		drf = pdrf;
		modified = false;
	}
	public TID insert(NamedCombinedRecord object) throws BufferFullException, IOException {
		modified = true;
		return drf.insert(object);
	}
	public void read(TID position, NamedCombinedRecord output) throws BufferFullException, IOException, DeletedRecordException {
		drf.read(position, output);
	}
	public void modify(TID position, NamedCombinedRecord object) throws BufferFullException, IOException, DeletedRecordException {
		modified = true;
		drf.modify(position, object);
	}
	public void delete(TID position) throws BufferFullException, IOException {
		modified = true;
		drf.delete(position);
	}
	public View<NamedCombinedRecord> view() {
		View<NamedCombinedRecord> internal = drf.view();
		modified = false;
		return new View<NamedCombinedRecord>(){
			public void restart() {
				internal.restart();
			}

			public boolean next(NamedCombinedRecord out) throws IOException, BufferFullException{
				if (modified) throw new RuntimeException("ConcurrentModification");
				return internal.next(out);
			}
		};
	}
}
