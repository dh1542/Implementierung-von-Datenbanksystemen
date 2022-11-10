import java.io.IOException;
import java.io.File;
import java.util.List;

import idb.buffer.Buffer;
import idb.buffer.BufferNotEmptyException;
import idb.buffer.LRUBuffer;
import idb.buffer.SimpleBuffer;
import idb.datatypes.IntegerKey;
import idb.datatypes.IntegerTIDFactory;
import idb.datatypes.TID;
import idb.hashing.HashIdx;
import idb.hashing.HashImpl;


public class Main {

	public static void main(String[] args) {
		Buffer buf = new LRUBuffer(24, 20);

		//Buffer buf = new SimpleBuffer(24);
		HashIdx<IntegerKey, TID> index =
			new HashImpl<IntegerKey, TID>(buf, 4711, 4712, new IntegerTIDFactory(), 4);

		// Remove these lines if you want to keep the data stored on the disk.
		// For this testcase it's easier to see whats going on if the index is not modified by previous runs.
		File base = new File("segment4711");
		base.deleteOnExit();
		File overflow = new File("segment4712");
		overflow.deleteOnExit();

		System.out.println("Writing Key 1 with TID 5,15");
		index.put(new IntegerKey(1), new TID(5,15));
		System.out.println("Writing Key 1 with TID 0,2");
		index.put(new IntegerKey(1), new TID(0,2));
		System.out.println("Writing Key 2 with TID 2,2");
		index.put(new IntegerKey(2), new TID(2,2));
		for(int i = 0; i < 5; ++i){
			System.out.println("Writing Key 0 to TID "+i+","+(i+i));
			index.put(new IntegerKey(0), new TID(i, i+i));
		}

		// Now check retreival
		List<TID> vals = index.get(new IntegerKey(0));
		System.out.println("-- Get all TIDs with key: "+0+" --");
		for(TID t: vals) System.out.println("Key 0, entry: "+t);
		vals = index.get(new IntegerKey(1));
		System.out.println("-- Get all TIDs with key: "+1+" --");
		for(TID t: vals) System.out.println("Key 1, entry: "+t);

		vals = index.get(new IntegerKey(2));
		System.out.println("-- Get all TIDs with key: "+2+" --");
		for(TID t: vals) System.out.println("Key 2, entry: "+t);

		try {
			((HashImpl)index).close();
			((LRUBuffer) buf).close();
		} catch (IOException e) {
			System.err.println("[ERROR] An error occurred. Stack Trace:");
			e.printStackTrace();
		} catch (BufferNotEmptyException e) {
			System.err.println("[ERROR] An error occurred. Stack Trace:");
			e.printStackTrace();
		}
	}

}
