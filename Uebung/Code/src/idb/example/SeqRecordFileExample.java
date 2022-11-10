/**
 * @file This file contains examples useful for SeqRecordFile and other RecordFiles.
 *       It shows interaction between DBBuffer and idb.record,
 *       as well as our interaction between DBBuffer and DataObject
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */

package idb.example;

import idb.buffer.DBBuffer;
import idb.block.BlockFile;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.File;

import java.util.List;
import java.util.ArrayList;

import idb.datatypes.*;

public class SeqRecordFileExample{
	public void run(DBBuffer buffer, BlockFile blockFile) throws IOException, idb.buffer.BufferFullException {
		// We start by checking if blockFile is already open
		if (blockFile.filename() == null) {
			// If the blockFile is not already opened, we cannot continue
			// Normally, this check is not necessary as it's the callers responsibility to open
			// the blockfile before passing it to any class in idb.record (see exercise sheets)
			System.err.println("Running on an unopened blockfile is not legal");
			return;
		}

		// Now we want to check if this is the first time to open this file (maybe interesting for TIDFile)
		// The caller has to make sure to use one blockfile for one File in idb.record only (using it as a TIDFile and a SeqentialFile will break)
		// As the caller is also not allowed to do something with the blockfile himself, we can check the first call to run by checking the size of blockfile:
		boolean first_run = (blockFile.size() == 0);

		if (first_run) {
			// We can do some initialization here.
			// For this example, we don't do anything interesting. We just want to make sure that the first_run boolean will be false next time
			// To do so, we use BlockFile::append, which is the only modifiing function on BlockFile that we're allowed to call
			System.out.println("This is the first run");
			blockFile.append(1);

			// BlockFile::append leaves us with a block of unknown contents. To modify this content, we call the following functions.
			// DBBuffer::fix gets us a ByteBuffer. We treat the ByteBuffer as a "area of bare memory", a de facto replacement for a C char*
			ByteBuffer bb = buffer.fix(blockFile, 0);
			// Nesting calls to DBBuffer::fix are fine, we just have to make sure to call DBBuffer::unfix accordingly
			ByteBuffer b2 = buffer.fix(blockFile, 0);
			// ByteBuffers are objects like everything else in java. So we can pass it by reference to write, where we write some data to the ByteBuffer:
			// Do not follow this function call immediatly, instead try to understand the run methode first and visit write and read afterwards.
			write(bb);
			// as we fixed that page twice, we need to call DBBuffer::unfix twice aswell, ...
			buffer.unfix(blockFile, 0);
			// but before we do that we have to inform the DBBuffer that we modfied the ByteBuffer.
			// The DBBuffer would otherwise skip writing the data to disc because "no modifcation has taken place".
			// To do so, we call DBBuffer::setDirty, informing the DBBuffer that we might have modfied the ByteBuffer and forcing DBBuffer to write the contents back to disc.
			buffer.setDirty(blockFile, 0);
			buffer.unfix(blockFile, 0);

		}
		else {
			// This can be used to recover metadata on startup. We don't do that in this example.
			System.out.println("We finished rebuilding");
		}

		// Now we are able to use the same procedure we already used for writing for reading data
		ByteBuffer bb = buffer.fix(blockFile, 0);
		read(bb);
		buffer.unfix(blockFile, 0);
		// We don't need DBBuffer::setDirty here and it would a mistake to use it, as it slows down the execution for no reason at all.
	}
	private void write(ByteBuffer bb) {
		// Remember: bb is passed in as a reference. Any modification we do to it will be seen outside,
		// and due to setDirty also persistant on the disc.
		//
		// So let's start and modify out ByteBuffer.

		// First, we need some data. That's where idb.datatypes.DataObject comes into play:
		// Every class derived from DataObject can be written (and read) the same way. Let's start by writing an Integer (42) at the beginning of the ByteBuffer.
		// A DBInteger is exactly what we want for that task. It implements Key, and Key extends DataObject, so we can serialize it.
		DataObject fourtyTwo = new DBInteger(42);

		// forty_two offers a methode to write itself into a given byteBuffer: write(int, ByteBuffer). We need to specify a position somewhere in the ByteBuffer.
		// Let's start with 0, at the beginning of the buffer.

		fourtyTwo.write(0, bb);

		// Now bb contains 42 at the beginning of the buffer. But we want to write more data.
		// We cannot use positon 0 again, because that would override fourtyTwo.
		// Remember, a ByteBuffer acts like a C char* and you can easily override (or partially override) data by accident.
		// We need to know where fourtyTwo ended. Luckely, there's a method for that:
		int nextOffset = fourtyTwo.size();

		// Let's do something else: We might be interested in writing something to the end of a block (TIDFile). So how would we do that?
		// First we create our DataObject:
		DataObject endData = new DBInteger(-1);
		// we can detect the size of any ByteBuffer by calling ByteBuffer::capacity
		int size = bb.capacity();
		// another option would be to ask the DBBuffer, he knows the pagesize aswell, but we don't have access to the DBBuffer right now.
		// The rest is relativly simple, we just need to use these sizes to caluclate the correct position:
		endData.write(size - endData.size(), bb);

		// Now we want to do something more complex: Integers are boring, as their size is always a constant. Let's do something with variable size: a String.
		DataObject varSize = new DBString("Hallo IDB");
		// While I could be calculating the size of this String by hand, let's assume that I'm not able in the general case.
		// Luckely, I don't have to care as the same procedure we used for fourtyTwo still works beatifully:
		varSize.write(nextOffset, bb);
		nextOffset += varSize.size();
		// Note: When implementing a new DataObject of variable size (Exercise 6 or 7), make sure to be able to read the size when asked to "read".
		// We don't know the size of these varSize DataObject a priori all the time, so it has to be part of the persistant data pattern.
		// There are two main ways of doing so: A) prefix the data with its size or B) use a special character to define the end of the data.
		//
		// For fun, we add another Integer right behind the varSize-String:
		DataObject forFun = new DBInteger(15);
		forFun.write(nextOffset, bb);

		// There are two special case that we did not cover (yet).
		// Let's start with Fragmentation. While a database should not fragment records in one block, we do this here for educational purposes.
		// Normally, fragmentation would only occur with different ByteBuffers - i.e. different Blocks.

		// First: Setup: The initial data is about 20 bytes in size. We want some safty distance, so we'll start at byte 50.
		// Sadly, someone told us that we my not use bytes 60 - 90, so we have to work around that. We do want to start at byte 50 though.
		// Note that this is an unrealistic scenario for databases. It's made up to demostrate fragmentation. We do not have invalid bytes in our blockFiles!

		// Testdata: fragmented. Again, assume we cannot calculate it's size when writing the code
		// Comment the other line in to see the difference.
		DataObject fragmented = new DBString("This is a very long string. Noone knows if it will be fine or not");
		// DataObject fragmented = new DBString("sh");

		// First: We need some help here. If fragmented is small eanugh, we don't want to fragment it.
		// To solve that, we prefix the data with its size. When reading, we still know that bytes 60 - 90 are invalid and can calculate if the record is fragmented
		DataObject fSize = new DBInteger(fragmented.size());
		fSize.write(50, bb);
		nextOffset = 50 + fSize.size();

		// Now we can check if the remaing space is ok
		if (fragmented.size() <= 60 - nextOffset){
			System.out.println("No fragmentation needed");
			fragmented.write(nextOffset, bb);
		}
		else {
			System.out.println("We need fragmentation");
			// In order to fragment a dataObject, we need to use the writePart method. It'll write only a part of the dataObject to the ByteBuffer:
			fragmented.writePart(nextOffset, bb, 0, 60 - nextOffset);
			// This call has two additional parameters for fragmented: (60 - nextOffset), which is the number ob bytes that should be written into bb,
			// and 0, which tells fragmented to start at the beginning ("This is") of its data.

			// After we started, we want to continue after byte 90, inserting all remaining bytes.
			// As the exapmle is hand-crafted, we can be sure not to override endData, which would have to be checked for normally.
			fragmented.writePart(90, bb, 60-nextOffset, fragmented.size() - (60 - nextOffset));
		}

		// To prove that bytes 60 - 90 are unsed, we write another Integer to byte 61:
		DataObject prove = new DBInteger(1337);
		prove.write(61, bb);

		// The other special case is where you don't know the structure of the data you're working with.
		// This happens espially during debugging, where you want to see why your data doesn't seem to be there anymore,
		// and in TIDFile, where moving unknown data is a necessary thing.
		//
		// To do that, ByteBuffer offers two methods that we use too: put(int, byte) and get(int).
		// Note that we don't use the relative variants put(byte) or get() and you're on your own if you want to use them in your database.
		//
		// Let's assume we want to know the content of position 62:
		byte content = bb.get(62);
		System.out.println("It is: " + content);

		// Writing it back to position 75 works similar:
		bb.put(75, content);

		// While these functions are capabile of doing almost anything, most of the time it's easier to use a DataObject.
	}

	private void read(ByteBuffer bb) {
		// We want to read all the data we wrote down during write:
		// Please make sure to read the comments in write before continueing here
		//
		// First let's start with all these numbers at fixed positions:
		// prove, fourtyTwo and endData

		DBInteger prove = new DBInteger(1);
		DBInteger fourtyTwo = new DBInteger(2);
		DBInteger endData = new DBInteger(1);
		// Two things to note here: A) we use the concrete class (DBInteger) instad of DataObject. This is just to display or understand the data.
		// Reading itself does not need the concrete class, however you should not try to read with a wrong class (for example try to read a DBString at offset 61,
		// where we wrote a DBInteger.) Most of the time it's the callers resonsibility to make sure the types are correct, but keep this note in mind as you might run into it
		// if you have a bug in your program.
		// Also, the inital paramters (1, 2, and 1) are not important at all for this program, they will be overriden as soon as "read" is called.
		

		// Reading is exactly the other way round compared to write. Instead of using a DataObject to write data to the ByteBuffer,
		// read modifies the DataObject's own storage to contain the information in ByteBuffer.
		prove.read(61, bb);
		fourtyTwo.read(0, bb);
		// endData is a bit tricky. But luckely integers always have a fixed size and so we're able to calculate its starting position a priori.
		// Note that you cannot (easily) write a variable record at the end of a block because you won't be able to detect where it started when reading.
		endData.read(bb.capacity() - endData.size(), bb);

		System.out.println("We found prove: " + prove + ", fourtyTwo: " + fourtyTwo + ", and endData: " + endData);
		// While it is very convinient to use these DBIntegers in a String, we can also get their values directly by calling DBInteger::getValue:

		if (fourtyTwo.getValue() != 42) {
			System.err.println("We've got a bug");
		}

		// there are three object still buried in the bb, and we want to read then: varSize, forFun and fragmented.
		// Technically also fSize but we'll use fSize to read fragmented.
		//
		// varSize is the easiest, as we know it's exact position: fourtyTwo.size();
		DBString varSize = new DBString("");
		// Again, inital data is not important
		varSize.read(fourtyTwo.size(), bb);

		// DBString now detected it's size automatically, and read all necesarry information:
		System.out.println("VarSize is: " + varSize);

		// On variable records, their size is always depended on their current contents, therefore it can change on DataObject::read.
		// So now, after reading, we can get the position for forFun. Doing this in the wrong order will result in a wrong readout for forFun.
		DBInteger forFun = new DBInteger(0);
		forFun.read(fourtyTwo.size() + varSize.size(), bb);
		System.out.println("forFun is: "+ forFun);

		// Now let's end this example with reading the fragmented (or maybe not fragmented) record.
		// We start by extracting the additional size information we wrote at the front in write:
		DBString fragmented = new DBString("");
		DBInteger sizeInfo = new DBInteger(0);
		sizeInfo.read(50, bb);
		if (sizeInfo.getValue() <= 10 - sizeInfo.size()) {
			// fragmented is not fragmented :D
			// This is the simple case, just like with varSize:
			fragmented.read(50 + sizeInfo.size(), bb);
			System.out.println("Unfragmented: " +fragmented);
		}
		else {
			// Now we need to collect all parts of fragmented in a list.
			// We have to insert the parts in correct order in a list so it can be reassembled.
			List<Triplet<ByteBuffer, Integer, Integer>> parts = new ArrayList<>();

			// A Triplet is a generic Pair, just with three entries. In this case, as documented in DataObect::readPart,
			// they represent 1. the Buffer to read this segment from, 2. the offset in the buffer where to start reading and 3. the size that should be read

			// We know that the first segment starts at 50+sizeInfo.size() and ends at 60, resulting in a size of 10 - sizeInfo.size():
			parts.add(new Triplet<>(bb, 50+sizeInfo.size(), 10 - sizeInfo.size()));

			// And the second part contains all remaining bits, starting at 90.
			// The size is tricky: We cannot ask fragmented how large it's going to be,
			// but that's why we prefixed the string with its length and we can use that now.
			parts.add(new Triplet<>(bb, 90, sizeInfo.getValue() - (10 - sizeInfo.size())));

			// After collecting all parts (make sure that the ByteBuffer is still valid and not already unfixed!)
			// we simply call readPart and wait for the magic to happen:

			fragmented.readPart(parts);

			System.out.println("Fragmented: " + fragmented);
		}
	}
	public static void main(String[] args) {
		try{
			// Setup: create a new file in data: (Interesting for your own MetaImpl)
			// As the example is run in build/main, we need to go two directories up. Not necessary for MetaImpl
			String path = "../../data/";
			File dir = new File(path);
			File testFile = File.createTempFile("foobar", ".exmpl", dir);
			// We don't want this file to stick around. Do not do if you want to keep the file :)
			testFile.deleteOnExit();
			BlockFile bf = idb.construct.Util.generateBlockFile(4096);
			DBBuffer buf = idb.construct.Util.generateSimpleBuffer(4096);

			// open the blockfile:
			bf.open(testFile.getCanonicalPath(), "rw");

			// run once to see initialization:
			SeqRecordFileExample srfe = new SeqRecordFileExample();
			srfe.run(buf, bf);

			//run again to see that writing only happens once:
			srfe.run(buf, bf);
		}
		catch (IOException | idb.buffer.BufferFullException ex) {
			ex.printStackTrace();
			return;
		}
	}
}
