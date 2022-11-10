import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;
import idb.buffer.BufferNotEmptyException;
import idb.datatypes.DataObject;
import idb.datatypes.Key;
import idb.datatypes.DBInteger;
import idb.datatypes.DBString;
import idb.record.KeyRecordFile;

import idb.construct.Util;

import java.util.function.Function;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.HashMap;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.*;

@FunctionalInterface
interface TSupplier<T> {
	T get() throws Exception;
}

public class HashTest {
	private Function<Integer, ? extends BlockFile> bfgenerator;
	private Function<Integer, ? extends DBBuffer> bufferGenerator;
	private File testFile, testFileO;
	private GarbageBlockFile bf, bfO;
	private CountingBlockFile cbf, cbfO;
	private CountingDBBuffer buf;

	@BeforeEach
	public void init() throws IOException {
		bfgenerator = x -> Util.generateBlockFile(x);
		bufferGenerator = Util::generateSimpleBuffer;
		testFile = File.createTempFile("foobar", null);
		testFileO = File.createTempFile("foobar-overflow", null);
	}

	private <D extends DataObject, K extends Key> KeyRecordFile<D, K> generate(int pageSize, double thresh,
			int initCapacity) throws IOException {
		bf = new GarbageBlockFile(cbf = new CountingBlockFile(bfgenerator.apply(pageSize)), pageSize);
		bf.open(testFile.getCanonicalPath(), "rw");
		bfO = new GarbageBlockFile(cbfO = new CountingBlockFile(bfgenerator.apply(pageSize)), pageSize);
		bfO.open(testFileO.getCanonicalPath(), "rw");
		buf = new CountingDBBuffer(bufferGenerator.apply(pageSize));
		return Util.<D,K>generateHash(buf, bf, bfO, thresh, initCapacity);
	}

	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null)
			buf.close();
		buf = null;
		if (bf != null)
			bf.close();
		bf = null;
		if (bfO != null)
			bfO.close();
		bfO = null;
		testFile.delete();
		testFileO.delete();
	}

	private <T> T assertBlockRead(CountingBlockFile bfCur, int pageNr, TSupplier<T> delayed) throws Exception {
		bfCur.setExpRead(pageNr);
		ByteBuffer bb = buf.fix(bfCur, pageNr); // fixing the page manually does allow us to make sure that multiple fixes in delayed do not count.
		bfCur.reset();
		T res = delayed.get();
		assertEquals(0, bfCur.getReads()); // Make sure that no other pages are needed
		buf.unfix(bfCur, pageNr);
		buf.flush();
		assertEquals(0, bfCur.getWrites()); // Make sure no setDirty was called
		return res;
	}

	@Test
	public void readWriteNoOverflow() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.5, 7);
		cbfO.reset();
		for (int i = 0; i < 10; ++i) {
			krf.insert(new DBInteger(i), new VariableRecord(10 * i + 10)); // this is less than 200 bytes, so it should
																			// not trigger an overflow or split
		}
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		for (int i = 0; i < 10; ++i) {
			assertEquals(1, krf.size(new DBInteger(i), new VariableRecord(0)));
		}
		for (int i = 0; i < 10; ++i) {
			VariableRecord vr = new VariableRecord(1);
			krf.read(new DBInteger(i), vr, 0);
			final int j = i;
			assertThrows(ArrayIndexOutOfBoundsException.class,
					() -> krf.read(new DBInteger(j), new VariableRecord(1), 1));
			assertEquals((i + 1) * 10 * Integer.SIZE / Byte.SIZE, vr.size(), "In run" + i);
		}
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
	}

	@Test
	public void readWriteForeign() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krfI = generate(4096, 0.5, 7);
		for (int i = 0; i < 10; ++i) {
			krfI.insert(new DBInteger(i), new VariableRecord(10 * i + 10)); // this is less than 200 bytes, so it
																				// should not trigger an overflow or
																				// split
		}
		krfI.close();
		buf.flush();
		cbfO.reset();
		cbf.reset();
		KeyRecordFile<VariableRecord, DBInteger> krf = Util.<VariableRecord, DBInteger>rebuildHash(buf, bf, bfO);
		assertEquals(0, cbf.getReads());
		assertEquals(0, cbf.getWrites());
		assertEquals(1, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		cbfO.reset();
		for (int i = 0; i < 10; ++i) {
			assertEquals(1, krf.size(new DBInteger(i), new VariableRecord(0)));
		}
		for (int i = 0; i < 10; ++i) {
			VariableRecord vr = new VariableRecord(1);
			krf.read(new DBInteger(i), vr, 0);
			final int j = i;
			assertThrows(ArrayIndexOutOfBoundsException.class,
					() -> krf.read(new DBInteger(j), new VariableRecord(1), 1));
			assertEquals((i + 1) * 10 * Integer.SIZE / Byte.SIZE, vr.size());
		}
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		for (int i = 0; i < 5; ++i) {
			krfI.insert(new DBInteger(10 + i), new VariableRecord(10 * i + 10)); // this is less than additional 100
																					// bytes, so it should not trigger
																					// an overflow or split
		}
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		for (int i = 0; i < 15; ++i) {
			VariableRecord vr = new VariableRecord(1);
			krf.read(new DBInteger(i), vr, 0);
			final int j = i;
			assertThrows(ArrayIndexOutOfBoundsException.class,
					() -> krf.read(new DBInteger(j), new VariableRecord(1), 1));
			int k = i;
			if (i >= 10)
				k = i - 10;
			assertEquals((k + 1) * 10 * Integer.SIZE / Byte.SIZE, vr.size());
		}
	}

	private ByteBuffer randomBB(int size, long seed) {
		byte[] arr = new byte[size];
		new Random(seed).nextBytes(arr);
		return ByteBuffer.wrap(arr);
	}

	// @param max: exclusive upper bound
	// @min: 1
	private ArrayList<VariableRecord> randomVR(int size, int max) {
		ArrayList<VariableRecord> ret = new ArrayList<>();
		Random r = new Random(1337);
		for (int i = 0; i < size; ++i) {
			ret.add(new VariableRecord(r.nextInt(max - 1) + 1));
		}
		return ret;
	}

	@RepeatedTest(10)
	public void garbage(RepetitionInfo repetitionInfo) throws Exception {
		ArrayList<VariableRecord> data = randomVR(1000, 100); // 100 ints is less than 4000 (bytes)
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.5, 7);
		bf.setGarbage(randomBB(4096, repetitionInfo.getCurrentRepetition()));
		bfO.setGarbage(randomBB(4096, 50 * repetitionInfo.getCurrentRepetition()));
		for (int i = 0; i < data.size(); ++i) {
			krf.insert(new DBInteger(i), data.get(i));
		}
		assertEquals(buf.getFix(), buf.getUnfix());
		for (int i = 0; i < data.size(); ++i) {
			VariableRecord ret = new VariableRecord(0);
			assertEquals(1, krf.size(new DBInteger(i), ret));
			krf.read(new DBInteger(i), ret, 0);
			assertEquals(data.get(i).size(), ret.size());
		}
		data = randomVR(2000, 100); // 100 ints is less than 4000 (bytes)
		for (int i = 1000; i < data.size(); ++i) {
			krf.insert(new DBInteger(i - 1000), data.get(i));
		}
		for (int i = 0; i < 1000; ++i) {
			VariableRecord ret = new VariableRecord(0);
			assertEquals(2, krf.size(new DBInteger(i), ret), "Int run: " + i);
			krf.read(new DBInteger(i), ret, 0);
			assertEquals(data.get(i).size(), ret.size());
			krf.read(new DBInteger(i), ret, 1);
			assertEquals(data.get(i + 1000).size(), ret.size());
			krf.delete(new DBInteger(i), 1, ret); // This is allowed as the order must be preserved when removing the
													// last element, as stated in @insert.
			assertEquals(1, krf.size(new DBInteger(i), ret), "Int run: " + i);
			krf.read(new DBInteger(i), ret, 0);
			assertEquals(data.get(i).size(), ret.size());
			final int j = i;
			assertThrows(ArrayIndexOutOfBoundsException.class,
					() -> krf.read(new DBInteger(j), new VariableRecord(1), 1));
		}
	}

	@Test
	public void readWriteMixedOverflow() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.5, 7);
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(100));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(7), new VariableRecord(1000)); // this is too large the for bucket, so it has to go in
																	// overflow
		buf.flush();
		assertThat(cbfO.getReads(), either(equalTo(1)).or(equalTo(2))); // We need to read the meta-block at the
																		// beginning of the overflow segment to know
																		// where to get a free block.
		// TODO: this is if free list is optional
		assertEquals(2, cbfO.getWrites()); // As appending is mapped to writes in GarbageBlockFile, we want 2 (one for
											// appending and one for inserting the data)
		cbfO.reset();
		krf.insert(new DBInteger(14), new VariableRecord(10)); // while this does fit into bucket1, one has to make
																// sure that no 14 is stored in the overflow.
		buf.flush();
		// Therefore we don't check Writes, but only reads
		assertNotEquals(0, cbfO.getReads());
		cbf.reset();
		// All should be in the same bucket, bucket 0
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(0), new VariableRecord(0))));
		// Rerun without assertBlockRead to make sure use-after-free could be detectable
		assertEquals(1, krf.size(new DBInteger(0), new VariableRecord(0)));

		VariableRecord vr = new VariableRecord(1);
		krf.read(new DBInteger(0), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		cbf.reset();
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(0), vr, 0); return null;});
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());

		// Repeat for 7 and 14
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(7), new VariableRecord(0))));
		assertEquals(1, krf.size(new DBInteger(7), new VariableRecord(0)));

		krf.read(new DBInteger(7), vr, 0);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		cbf.reset();
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(7), vr, 0); return null;});
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());

		// Repeat for 14
		assertEquals(1, krf.size(new DBInteger(14), new VariableRecord(0)));
		cbf.reset();
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(14), new VariableRecord(0))));

		krf.read(new DBInteger(14), vr, 0);
		assertEquals(10 * Integer.SIZE / Byte.SIZE, vr.size());
		cbf.reset();
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(14), vr, 0); return null;});
		assertEquals(10 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void doubleSplit() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.2, 7); // setting the threshhold to 0.2 should
																				// trigger a split after 1024*7/5 = 1433
																				// Ints, after that at 1024 * 8/5 = 1638
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(1000));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(7), new VariableRecord(400)); // this is too large the for bucket, so it has to go in
																// overflow
		buf.flush();
		assertThat(cbfO.getReads(), either(equalTo(1)).or(equalTo(2))); // We need to read the meta-block at the
																		// beginning of the overflow segment to know
																		// where to get a free block.
		// TODO: this is if free list is optional
		assertEquals(2, cbfO.getWrites()); // As appending is mapped to writes in GarbageBlockFile, we want 2 (one for
											// appending and one for inserting the data)
		krf.insert(new DBInteger(21), new VariableRecord(100));
		buf.flush();
		cbfO.reset();
		cbf.reset();
		krf.insert(new DBInteger(8), new VariableRecord(100));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		cbfO.reset();
		cbf.reset();

		// 0 should be in the bucket 0
		assertEquals(1, krf.size(new DBInteger(0), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(0), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		VariableRecord vr = new VariableRecord(1);
		krf.read(new DBInteger(0), vr, 0);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(0), vr, 0); return null;});
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 7 and 21
		assertEquals(1, krf.size(new DBInteger(7), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(7), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(7), vr, 0); return null;});
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.read(new DBInteger(7), vr, 0);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 21
		assertEquals(1, krf.size(new DBInteger(21), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(21), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(21), vr, 0);
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(21), vr, 0); return null;});
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 8
		assertEquals(1, krf.size(new DBInteger(8), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 1, () -> krf.size(new DBInteger(8), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(8), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertBlockRead(cbf, 1, () -> {krf.read(new DBInteger(8), vr, 0); return null;});
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());

		krf.insert(new DBInteger(9), new VariableRecord(100));
		buf.flush();
		cbfO.reset();
		cbf.reset();

		// 0 should be in the bucket 0
		assertEquals(1, krf.size(new DBInteger(0), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(0), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(0), vr, 0);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(0), vr, 0); return null;});
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 7 and 21
		assertEquals(1, krf.size(new DBInteger(7), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(7), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(7), vr, 0); return null;});
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.read(new DBInteger(7), vr, 0);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 21
		assertEquals(1, krf.size(new DBInteger(21), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(21), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(21), vr, 0);
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(21), vr, 0); return null;});
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 8
		assertEquals(1, krf.size(new DBInteger(8), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 8, () -> krf.size(new DBInteger(8), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(8), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertBlockRead(cbf, 8, () -> {krf.read(new DBInteger(8), vr, 0); return null;});
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());

		// Repeat for 9
		assertEquals(1, krf.size(new DBInteger(9), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 2, () -> krf.size(new DBInteger(9), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(9), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertBlockRead(cbf, 2, () -> {krf.read(new DBInteger(9), vr, 0); return null;});
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void readWriteMixedSplit() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.2, 7); // setting the threshhold to 0.2 should
																				// trigger a split after 1024*7/5 = 1433
																				// Ints
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(1000));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(7), new VariableRecord(400)); // this is too large the for bucket, so it has to go in
																// overflow
		buf.flush();
		assertThat(cbfO.getReads(), either(equalTo(1)).or(equalTo(2))); // We need to read the meta-block at the
																		// beginning of the overflow segment to know
																		// where to get a free block.
		// TODO: this is if free list is optional
		assertEquals(2, cbfO.getWrites()); // As appending is mapped to writes in GarbageBlockFile, we want 2 (one for
											// appending and one for inserting the data)
		krf.insert(new DBInteger(21), new VariableRecord(100));
		buf.flush();
		cbfO.reset();
		cbf.reset();

		VariableRecord vr = new VariableRecord(1);
		// 0 should be in bucket 0
		assertEquals(1, krf.size(new DBInteger(0), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(0), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(0), vr, 0);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(0), vr, 0); return null;});
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 7
		assertEquals(1, krf.size(new DBInteger(7), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(7), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(7), vr, 0);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(7), vr, 0); return null;});
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		// Repeat for 21
		assertEquals(1, krf.size(new DBInteger(21), new VariableRecord(0)));
		assertEquals(1, assertBlockRead(cbf, 7, () -> krf.size(new DBInteger(21), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(21), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 7, () -> {krf.read(new DBInteger(21), vr, 0); return null;});
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
	}

	@Test
	public void reclaimSpace() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.2, 7); // setting the threshhold to 0.2 should
																				// trigger a split after 1024*7/5 = 1433
																				// Ints
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(1000));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(2), new VariableRecord(400));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.delete(new DBInteger(0), 0, new VariableRecord(0)); // now there is a lot free space again
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(21), new VariableRecord(100));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		cbfO.reset();
		cbf.reset();
		VariableRecord vr = new VariableRecord(0);
		// Read 2
		assertEquals(1, krf.size(new DBInteger(2), new VariableRecord(0)));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(1, assertBlockRead(cbf, 2, () -> krf.size(new DBInteger(2), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(2), vr, 0);
		assertEquals(0, cbf.getWrites());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 2, () -> {krf.read(new DBInteger(2), vr, 0); return null;});
		assertEquals(0, cbf.getWrites());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());

		// Read 21
		assertEquals(1, krf.size(new DBInteger(21), new VariableRecord(0)));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(1, assertBlockRead(cbf, 0, () -> krf.size(new DBInteger(21), new VariableRecord(0))));
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());

		krf.read(new DBInteger(21), vr, 0);
		assertEquals(0, cbf.getWrites());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertBlockRead(cbf, 0, () -> {krf.read(new DBInteger(21), vr, 0); return null;});
		assertEquals(0, cbf.getWrites());
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void readWriteSameOverflow() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.5, 7);
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(100));
		buf.flush();
		assertEquals(0, cbfO.getReads());
		assertEquals(0, cbfO.getWrites());
		krf.insert(new DBInteger(0), new VariableRecord(1000)); // this is too large the for bucket, so it has to go in
																	// overflow
		buf.flush();
		assertThat(cbfO.getReads(), either(equalTo(1)).or(equalTo(2))); // We need to read the meta-block at the
																		// beginning of the overflow segment to know
																		// where to get a free block.
		// TODO: this is if free list is optional
		assertEquals(2, cbfO.getWrites()); // As appending is mapped to writes in GarbageBlockFile, we want 2 (one for
											// appending and one for inserting the data)
		cbfO.reset();
		krf.insert(new DBInteger(0), new VariableRecord(10)); // while this does fit into bucket1, one has to make sure
																// that the index is correct
		assertEquals(3, krf.size(new DBInteger(0), new VariableRecord(0)));
		VariableRecord vr = new VariableRecord(1);
		krf.read(new DBInteger(0), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		krf.read(new DBInteger(0), vr, 1);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
		krf.read(new DBInteger(0), vr, 2);
		assertEquals(10 * Integer.SIZE / Byte.SIZE, vr.size());
		krf.delete(new DBInteger(0), 2, vr);
		krf.read(new DBInteger(0), vr, 0);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		krf.read(new DBInteger(0), vr, 1);
		assertEquals(1000 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void dbStrings() throws Exception {
		KeyRecordFile<DBString, DBString> krf = generate(4096, 0.5, 7);
		krf.insert(new DBString("Hallo"), new DBString("Welt"));
		krf.insert(new DBString("Tobias"), new DBString("Heineken"));
		krf.insert(new DBString("USA"), new DBString("UdSSR"));
		krf.insert(new DBString("Umlauts"), new DBString("äöüß"));
		DBString out = new DBString("foobar");
		assertEquals(1, krf.size(new DBString("Hallo"), out));
		krf.read(new DBString("Hallo"), out, 0);
		assertEquals("Welt", out.content());
		assertEquals(1, krf.size(new DBString("Tobias"), out));
		krf.read(new DBString("Tobias"), out, 0);
		assertEquals("Heineken", out.content());
		assertEquals(1, krf.size(new DBString("USA"), out));
		krf.read(new DBString("USA"), out, 0);
		assertEquals("UdSSR", out.content());
		assertEquals(0, krf.size(new DBString("UdSSR"), out));
		assertEquals(1, krf.size(new DBString("Umlauts"), out));
		krf.read(new DBString("Umlauts"), out, 0);
		assertEquals("äöüß", out.content());
	}

	@Test
	public void loadTest() throws Exception {
		KeyRecordFile<VariableRecord, DBInteger> krf = generate(4096, 0.5, 7);
		HashMap<Integer, ArrayList<Integer>> inMemory = new HashMap<>(); // key->(index->Value)
		Random r = new Random(13763);
		for (int i = 0; i < 100; ++i) {
			insert(krf, inMemory, r);
		}
		for (int i = 0; i < 2_000; ++i) {
			if (r.nextInt(2) == 1) {
				insert(krf, inMemory, r);
			}
			if (r.nextInt(5) == 1) {
				delete(krf, inMemory, r);
			}
			if (r.nextInt(3) == 1) {
				modify(krf, inMemory, r);
			}
			if (r.nextInt(4) == 1) {
				modifyKey(krf, inMemory, r);
			}
			if (r.nextInt(40) == 1) {
				deleteAll(krf, inMemory, r);
			}
		}
	}

	private void refresh(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory)
			throws IOException, BufferFullException {
		HashMap<Integer, ArrayList<Integer>> out = new HashMap<>();
		for (Integer k : inMemory.keySet()) {
			int sz = krf.size(new DBInteger(k), new VariableRecord(1));
			ArrayList<Integer> curList = inMemory.get(k);
			assertEquals(curList.size(), sz, "For Key: " + k);
			ArrayList<Integer> actual = new ArrayList<>(sz);
			for (int i = 0; i < sz; ++i) {
				VariableRecord vr = new VariableRecord(1);
				krf.read(new DBInteger(k), vr, i);
				actual.add(vr.size());
			}
			assertTrue(actual.stream().allMatch(v -> curList.contains(v)),
					"Actual and previous do not contain the same content");
			out.put(k, actual);
		}
		inMemory.clear();
		inMemory.putAll(out);
	}

	private void insert(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory,
			Random r) throws IOException, BufferFullException {
		int key = r.nextInt(500);
		int amm = r.nextInt(500) + 1;
		VariableRecord vr = new VariableRecord(amm);
		inMemory.computeIfAbsent(key, ArrayList::new).add(vr.size());
		krf.insert(new DBInteger(key), vr);
		check(krf, inMemory);
	}

	private void check(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory)
			throws IOException, BufferFullException {
		for (Integer k : inMemory.keySet()) {
			int sz = krf.size(new DBInteger(k), new VariableRecord(1));
			ArrayList<Integer> curList = inMemory.get(k);
			assertEquals(curList.size(), sz);
			for (int i = 0; i < sz; ++i) {
				VariableRecord vr = new VariableRecord(1);
				krf.read(new DBInteger(k), vr, i);
				assertEquals(curList.get(i), vr.size());
			}
			final int j = k;
			assertThrows(ArrayIndexOutOfBoundsException.class,
					() -> krf.read(new DBInteger(j), new VariableRecord(1), sz));
		}
	}

	private void delete(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory,
			Random r) throws IOException, BufferFullException {
		int key = choice(inMemory.keySet(), r);
		ArrayList<Integer> cur = inMemory.get(key);
		if (cur.size() == 0)
			return;
		if (r.nextInt(5) < 2) {
			// remove last element
			cur.remove(cur.size() - 1);
			krf.delete(new DBInteger(key), cur.size(), new VariableRecord(1));
			check(krf, inMemory);
			return;
		}
		int idx = r.nextInt(cur.size());
		cur.remove(idx);
		krf.delete(new DBInteger(key), idx, new VariableRecord(1));
		refresh(krf, inMemory);
	}

	private void modify(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory,
			Random r) throws IOException, BufferFullException {
		int key = choice(inMemory.keySet(), r);
		ArrayList<Integer> cur = inMemory.get(key);
		if (cur.size() == 0) {
			insert(krf, inMemory, r);
			return;
		}
		int idx = r.nextInt(cur.size());
		int amm = r.nextInt(500) + 1;
		VariableRecord vr = new VariableRecord(amm);
		cur.set(idx, vr.size());
		krf.modify(new DBInteger(key), idx, vr);
		refresh(krf, inMemory);
	}

	private void modifyKey(KeyRecordFile<VariableRecord, DBInteger> krf,
			HashMap<Integer, ArrayList<Integer>> inMemory, Random r) throws IOException, BufferFullException {
		int key = choice(inMemory.keySet(), r);
		ArrayList<Integer> cur = inMemory.get(key);
		int key2 = choice(inMemory.keySet(), r);
		if (key == key2)
			return;
		ArrayList<Integer> cur2 = inMemory.get(key2);
		cur2.addAll(cur);
		cur.clear();
		krf.modifyKey(new DBInteger(key), new DBInteger(key2), new VariableRecord(1));
		refresh(krf, inMemory);
	}

	private void deleteAll(KeyRecordFile<VariableRecord, DBInteger> krf, HashMap<Integer, ArrayList<Integer>> inMemory,
			Random r) throws IOException, BufferFullException {
		int key = choice(inMemory.keySet(), r);
		ArrayList<Integer> cur = inMemory.get(key);
		cur.clear();
		krf.delete(new DBInteger(key), new VariableRecord(1));
		refresh(krf, inMemory);
	}

	// from: https://stackoverflow.com/a/25410520
	public static <E> E choice(Collection<? extends E> coll, Random rand) {
		if (coll.size() == 0) {
			return null; // or throw IAE, if you prefer
		}
		int index = rand.nextInt(coll.size());
		if (coll instanceof List) { // optimization
			return ((List<? extends E>) coll).get(index);
		} else {
			Iterator<? extends E> iter = coll.iterator();
			for (int i = 0; i < index; i++) {
				iter.next();
			}
			return iter.next();
		}
	}
}
