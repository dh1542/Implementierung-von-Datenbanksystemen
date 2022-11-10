/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.datatypes;

import java.util.function.Predicate;
import java.util.function.Function;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.nio.ByteBuffer;

public class NamedCombinedRecordImpl implements NamedCombinedRecord {

	private List<Type> types;
	private List<DataObject> values;
	private List<String> names;

	private NamedCombinedRecordImpl(List<DataObject> dO, List<String> s, List<Type> t) {
		names = s;
		values = dO;
		types = t;
	}

	public NamedCombinedRecordImpl(List<String> nam, List<Type> t) {
		names = nam;
		types = t;
		values = new ArrayList<>(types.size());
		if (types.size() != names.size()) {
			throw new RuntimeException("Unbalanced types / names");
		}
		assert types.size() == names.size();
		for(Type ty : types) {
			switch(ty) {
				case INT: values.add(new DBInteger(0)); break;
				case STRING: values.add(new DBString("")); break;
				case BOOL: values.add(new Bool(true)); break;
				default: throw new RuntimeException("This is prohibited");
			}
		}
	}

	public NamedCombinedRecordImpl(int i, String name) {
		names = List.of(name);
		types = List.of(Type.INT);
		values = List.of(new DBInteger(i));
	}

	public NamedCombinedRecordImpl(boolean b, String name) {
		names = List.of(name);
		types = List.of(Type.BOOL);
		values = List.of(new Bool(b));
	}

	public NamedCombinedRecordImpl(String value, String name) {
		names = List.of(name);
		types = List.of(Type.STRING);
		values = List.of(new DBString(value));
	}

	@Override
	public NamedCombinedRecord combine(NamedCombinedRecord other) {
		assert other instanceof NamedCombinedRecordImpl;
		NamedCombinedRecordImpl nother = (NamedCombinedRecordImpl) other;
		if (nother.names.stream().anyMatch(names::contains)) {
			throw new IllegalArgumentException("Duplicated names");
		}
		return new NamedCombinedRecordImpl(Stream.concat(values.stream(), nother.values.stream()).map(DataObject::copy).collect(Collectors.toList()), Stream.concat(names.stream(), nother.names.stream()).collect(Collectors.toList()), Stream.concat(types.stream(), nother.types.stream()).collect(Collectors.toList()));
	}

	@Override
	public String toString() {
		String out = "";
		for(int i=0; i < names.size(); ++i){
			out += "(" + names.get(i)+":"+values.get(i)+")";
		}
		return out;
	}

	@Override
	public NamedCombinedRecord get(Predicate<String> p) {
		List<Type> nT = new ArrayList<>();
		List<DataObject> nV = new ArrayList<>();
		List<String> nN = new ArrayList<>();

		for(int i=0; i < names.size(); ++i){
			if (p.test(names.get(i))) {
				nT.add(types.get(i));
				nV.add(values.get(i).copy());
				nN.add(names.get(i));
			}
		}
		if (nT.isEmpty()) {
			throw new NoSuchElementException("Predicate did not match");
		}
		return new NamedCombinedRecordImpl(nV, nN, nT);
	}

	@Override
	public Type getType(String name) {
		int idx = names.indexOf(name);
		if (idx == -1) throw new NoSuchElementException("Name ("+name+") is not in this NamedCombinedRecord");
		return types.get(idx);
	}

	@Override
	public String getString(String name) {
		int idx = names.indexOf(name);
		if (idx == -1) throw new NoSuchElementException("Name ("+name+") is not in this NamedCombinedRecord");
		if (types.get(idx) != Type.STRING) throw new IllegalStateException("this type is "+types.get(idx).name());
		return ((DBString) values.get(idx)).content();/* convert from DataObject.copy to DBString / Bool / IntegerKey is allowed*/
	}

	@Override
	public int getInt(String name) {
		int idx = names.indexOf(name);
		if (idx == -1) throw new NoSuchElementException("Name ("+name+") is not in this NamedCombinedRecord");
		if (types.get(idx) != Type.INT) throw new IllegalStateException("this type is "+types.get(idx).name());
		return ((DBInteger) values.get(idx)).getValue();/* convert from DataObject.copy to DBString / Bool / IntegerKey is allowed*/
	}

	@Override
	public boolean getBool(String name) {
		int idx = names.indexOf(name);
		if (idx == -1) throw new NoSuchElementException("Name ("+name+") is not in this NamedCombinedRecord");
		if (types.get(idx) != Type.BOOL) throw new IllegalStateException("this type is "+types.get(idx).name());
		return ((Bool) values.get(idx)).getValue();/* convert from DataObject.copy to DBString / Bool / IntegerKey is allowed*/
	}

	@Override
	public NamedCombinedRecordImpl rename(Function<String, String> f) {
		return new NamedCombinedRecordImpl(values.stream().map(DataObject::copy).collect(Collectors.toList()), names.stream().map(f).collect(Collectors.toList()), types);
	}

	@Override
	public int size() {
		int out = 0;
		for(DataObject d : values) {
			out += d.size();
		}
		return out;
	}

	/*@Override
	public NamedCombinedRecord copy() {
		return rename(x->x);
	}*/

	@Override
	public void write(int idx, ByteBuffer bb) {
		for(DataObject d : values) {
			int sz = d.size();
			d.write(idx, bb);
			idx += sz;
		}
	}

	@Override
	public void read(int idx, ByteBuffer bb) {
		for(DataObject d : values) {
			d.read(idx, bb);
			idx += d.size();
		}
	}

	@Override
	public NamedCombinedRecord modifyValues(NamedCombinedRecord other) {
		NamedCombinedRecordImpl nother = (NamedCombinedRecordImpl) other;
		for(String n : nother.names) {
			if (nother.getType(n) != getType(n)) {
				throw new IllegalStateException("Types differ");
			}
		}
		NamedCombinedRecordImpl cpy = rename(x -> x);
		for (int i=0; i < cpy.names.size(); ++i){
			int idx = nother.names.indexOf(cpy.names.get(i));
			if (idx == -1) continue;
			System.out.println("Valid: " + cpy.names.get(i));
			nother.values.get(idx).transfer(cpy.values.get(i));
		}
		return cpy;
	}

}
