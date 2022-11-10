/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import idb.buffer.Buffer;
import idb.buffer.BufferNotEmptyException;
import idb.datatypes.DBInteger;
import idb.datatypes.TID;


public class Main {

	public static void query(String command){
		System.out.println("This is a query: " + command);
	}

	public static void shell() {
		BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("IDB DB-Shell. End by typing \"quit;\" and hitting return");
outer: while(true) {
			try{
				// Read next line
				String command = "";
				try {
					do{
						String cCommand = commandLine.readLine();
						if (cCommand == null) break outer;
						command += " "+cCommand;
					} while(!command.endsWith(";"));
				} catch(IOException ioe) {
					break;
				}
				command = command.substring(0, command.length()-1).trim();
				if(command.isEmpty()) continue;

				if (command.equals("quit")) return;
				String lowercaseCommand = command.toLowerCase();
				if (lowercaseCommand.startsWith("select") || lowercaseCommand.startsWith("from") || lowercaseCommand.startsWith("where") || lowercaseCommand.startsWith("group by") || lowercaseCommand.startsWith("order by")) {
					query(command);
				}
				else{
					System.out.println("This is the res: " + command);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		/*Buffer buf = new LRUBuffer(24, 20);

		//Buffer buf = new SimpleBuffer(24);
		HashIdx<DBInteger, TID> index =
			new HashImpl<DBIntege, TID>(buf, 4711, 4712, new IntegerTIDFactory(), 4);

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
		}*/ /*
		new idb.tests.BlockFileTest(idb.block.BlockImpl::new);
		try ( idb.block.BlockFile block = new idb.block.BlockImpl(4096) ) {
			System.out.println(block.bytes());
			block.open("segment1", "rw");
			block.append(2);
			System.out.println(block.size());
			block.drop(2);
			java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(4096);
			block.read(3, buf);
			System.out.println(buf);

			java.util.Formatter formatter = new java.util.Formatter();
			for (byte b : buf.array()) {
				formatter.format("%02x", b);
			}
			System.out.println(formatter.toString());
			for (int i=0; i < 16; i++){
				buf.putInt(i);
			}
			idb.datatypes.TIDIndex tid = new idb.datatypes.TIDIndex(2);
			tid.read(buf);
			System.out.println(tid.isInvalid());
			System.out.println(tid.index());
			block.write(3, buf);
			idb.buffer.DBBuffer dbbuf = null;
			try (idb.block.BlockFile b2 = new idb.block.BlockImpl(4096)){
				b2.open("segmentTID", "rw");
				dbbuf = new idb.buffer.SimpleDBBuffer(4096);
				idb.record.DirectRecordFile<?, idb.datatypes.IntegerData> tid_file = new idb.record.TIDFile<idb.datatypes.IntegerData>(dbbuf, b2);
				for(int i=0; i < 4096; ++i){
					Object o = tid_file.insert(new idb.datatypes.IntegerData(0));
				}
				//System.out.println(tid_file.read(o).toString());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				dbbuf.close();
			}
		}
		catch (IOException e) {
				System.err.println("[ERROR] An error occurred. Stack Trace:");
				e.printStackTrace();
		}
		catch (BufferNotEmptyException bnee) {
				System.err.println("[ERROR] An error occurred. Stack Trace:");
				bnee.printStackTrace();
		}*/

		shell();
	}

}
