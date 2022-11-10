import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.datatypes.DataObject;
import idb.record.SeqRecordFile;
import idb.record.View;

import idb.construct.Util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.function.Function;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
public class SeqTest{
	private Function<Integer, ? extends BlockFile> bfgenerator;
	private Function<Integer, ? extends DBBuffer> bufferGenerator;
	private File testFile;
	private GarbageBlockFile bf;
	private CountingDBBuffer buf;

	@BeforeEach
	public void init() throws IOException{
		bfgenerator = x -> Util.generateBlockFile(x);
		bufferGenerator = Util::generateSimpleBuffer;
		testFile = File.createTempFile("foobar", null);
	}

	private <E extends DataObject> SeqRecordFile<E> generate(int pageSize) throws IOException {
		bf = new GarbageBlockFile(bfgenerator.apply(pageSize), pageSize);
		bf.open(testFile.getCanonicalPath(), "rw");
		buf = new CountingDBBuffer(bufferGenerator.apply(pageSize));
		return Util.<E>generateSeqRecordFile(buf, bf);
	}

	// @param max: exclusive upper bound
	// @min: 1
	private ArrayList<UnguardedVariableRecord> randomVR(int size, int max, int seed) {
		ArrayList<UnguardedVariableRecord> ret = new ArrayList<>();
		Random r = new Random(seed);
		for (int i=0; i < size; ++i){
			ret.add(new UnguardedVariableRecord(r.nextInt(max-1)+1));
		}
		return ret;
	}

	private ByteBuffer randomBB(int size, long seed){
		byte[] arr = new byte[size];
		new Random(seed).nextBytes(arr);
		return ByteBuffer.wrap(arr);
	}

	@AfterEach
	public void cleanup() throws Exception {
		if (buf != null) buf.close();
		buf = null;
		if (bf != null) bf.close();
		bf = null;
		testFile.delete();
	}

	@RepeatedTest(30)
	public void garbage(RepetitionInfo repetitionInfo) throws Exception {
		ArrayList<UnguardedVariableRecord> data = randomVR(1000, 2000, 1490);
		SeqRecordFile<UnguardedVariableRecord> srf = generate(4096);
		bf.setGarbage(randomBB(4096, repetitionInfo.getCurrentRepetition()));
		for (UnguardedVariableRecord v : data) {
			srf.insert(v);
		}
		assertEquals(buf.getFix(), buf.getUnfix());
		View<UnguardedVariableRecord> view = srf.view();
		for (int i = 0 ; i < data.size(); ++i){
			UnguardedVariableRecord ret = new UnguardedVariableRecord(0);
			assertTrue(view.next(ret));
			assertEquals(data.get(i).size(), ret.size());
		}
		view.restart();
		for (int i = 0 ; i < data.size(); ++i){
			UnguardedVariableRecord ret = new UnguardedVariableRecord(0);
			assertTrue(view.next(ret));
			assertEquals(data.get(i).size(), ret.size());
		}
		srf.close();
		srf = Util.rebuildSeqRecordFile(buf, bf);
		view = srf.view();
		for (int i = 0 ; i < data.size(); ++i){
			UnguardedVariableRecord ret = new UnguardedVariableRecord(0);
			assertTrue(view.next(ret));
			assertEquals(data.get(i).size(), ret.size());
		}
	}

	private void needsDirty(){
		assertTrue(1 <= buf.getDirty(), "This call needs to call setDirty at least once but this one didn't");
	}

	@Test
	public void simple() throws Exception{
		SeqRecordFile<VariableRecord> srf = generate(4096);
		buf.reset();
		srf.insert(new VariableRecord(20));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		buf.reset();
		srf.insert(new VariableRecord(25));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		buf.reset();
		srf.insert(new VariableRecord(30));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		View<VariableRecord> view = srf.view();
		VariableRecord vr = new VariableRecord(0);
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
		view.restart();
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		view.restart();
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
	}
	@Test
	public void reconstruct() throws Exception{
		SeqRecordFile<VariableRecord> srf = generate(4096);
		buf.reset();
		srf.insert(new VariableRecord(20));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		buf.reset();
		srf.insert(new VariableRecord(25));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		buf.reset();
		srf.insert(new VariableRecord(30));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		View<VariableRecord> view = srf.view();
		VariableRecord vr = new VariableRecord(0);
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
		srf.close();
		srf = Util.rebuildSeqRecordFile(buf, bf);
		view = srf.view();
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
		buf.reset();
		srf.insert(new VariableRecord(10));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		view = srf.view();
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(20).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(25).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(10).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
	}
	@Test
	public void oversized() throws Exception{
		SeqRecordFile<VariableRecord> srf = generate(4096);
		srf.insert(new VariableRecord(900));
		srf.insert(new VariableRecord(400));
		buf.reset();
		srf.insert(new VariableRecord(30));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		View<VariableRecord> view = srf.view();
		VariableRecord vr = new VariableRecord(0);
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(900).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(400).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new VariableRecord(30).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
	}
	@Test
	public void fragmented() throws Exception{
		SeqRecordFile<UnguardedVariableRecord> srf = generate(4096);
		srf.insert(new UnguardedVariableRecord(1200));
		buf.reset();
		srf.insert(new UnguardedVariableRecord(40));
		assertEquals(1, buf.getUnfix());
		assertEquals(1, buf.getFix());
		needsDirty();
		srf.insert(new UnguardedVariableRecord(3000));
		View<UnguardedVariableRecord> view = srf.view();
		UnguardedVariableRecord vr = new UnguardedVariableRecord(0);
		assertTrue(view.next(vr));
		assertEquals(new UnguardedVariableRecord(1200).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new UnguardedVariableRecord(40).size(), vr.size());
		assertTrue(view.next(vr));
		assertEquals(new UnguardedVariableRecord(3000).size(), vr.size());
		assertFalse(view.next(vr));
		assertFalse(view.next(vr));
	}
}
