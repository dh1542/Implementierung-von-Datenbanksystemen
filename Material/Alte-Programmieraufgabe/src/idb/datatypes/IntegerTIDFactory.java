package idb.datatypes;


import idb.hashing.KeyValueFactory;

public class IntegerTIDFactory implements KeyValueFactory<IntegerKey, TID>{
	public IntegerKey createKey(){
		return new IntegerKey(0);
	}
	public TID createDataObject(){
		return new TID(-1, -1);
	}
	
}
