import java.util.function.Function;
import idb.record.DirectRecordFile;
import idb.record.DeletedRecordException;
import idb.record.View;
import idb.datatypes.TID;
import idb.datatypes.DataObject;
import idb.datatypes.DBInteger;
import idb.datatypes.DBString;
import idb.block.BlockFile;
import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;
import idb.buffer.BufferNotEmptyException;

import idb.construct.Util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;

class CountingDBBuffer implements DBBuffer {
	private DBBuffer instance;
	private int fix, unfix, setDirty;

	public CountingDBBuffer(DBBuffer other) {
		instance = other;
	}

	public ByteBuffer fix(BlockFile blockfile, int pageno) throws IOException, BufferFullException {
		fix++;
		return instance.fix(blockfile, pageno);
	}
	public void unfix(BlockFile blockfile, int pageno) throws IOException {
		unfix++;
		instance.unfix(blockfile, pageno);
	}
	public void setDirty(BlockFile blockfile, int pageno) {
		setDirty++;
		instance.setDirty(blockfile, pageno);
	}
	public int getPagesize() {
		return instance.getPagesize();
	}
	/**
	 * write the unfixed, buffered pages back to disk
	 * @throws IOException signals an error in the underlying file system
	 */
	public void flush() throws IOException {
		instance.flush();
	}
	public void close() throws BufferNotEmptyException, IOException {
		instance.close();
	}
	public void reset() {
		fix = 0;
		unfix = 0;
		setDirty = 0;
	}
	public int getFix() {
		return fix;
	}
	public int getUnfix() {
		return unfix;
	}
	public int getDirty() {
		return setDirty;
	}
}

class AdjustableRecord implements DataObject {
	int size;
	public AdjustableRecord(int val){
		size = val;
	}


	@Override
	public void read(int index, ByteBuffer bb) {
		for(int i=0; i < size; ++i){
			assertEquals(0, bb.get(i+index));
		}
	}

	@Override
	public void write(int index, ByteBuffer bb) {
		for(int i=0; i < size; ++i){
			bb.put(i+index, (byte)0);
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public DataObject copy() {
		return new AdjustableRecord(size);
	}
}

class VariableRecord extends UnguardedVariableRecord {
	public VariableRecord(int numInts){
		super(numInts);
		assert size <= 1000;
	}
	@Override
	public DataObject copy() {
		return new VariableRecord(size);
	}
}

class UnguardedVariableRecord implements DataObject {
	int size;
	public UnguardedVariableRecord(int numInts){
		size = numInts;
	}


	@Override
	public void read(int index, ByteBuffer bb) {
		size = bb.getInt(index);
		for (int i=1; i < size; ++i){
			final int finalI = i;
			assertEquals(size, bb.getInt(index + i*(Integer.SIZE / Byte.SIZE)), () ->"At Pos "+ finalI);
		}
	}

	@Override
	public void write(int index, ByteBuffer bb) {
		assert size > 0;
		for (int i = 0; i < size; ++i)
			bb.putInt(index+i*(Integer.SIZE / Byte.SIZE), size);
	}

	@Override
	public int size() {
		return size * (Integer.SIZE / Byte.SIZE);
	}

	@Override
	public DataObject copy() {
		return new UnguardedVariableRecord(size);
	}

	@Override
	public String toString() {
		return "{size: " + size+"}";
	}
}

class ForeignVSR extends VariableShortRecord {
	public ForeignVSR(int numInts, short v){
		super(numInts, v);
	}
	// copy will return a VariableShortRecord, not a ForeignVSR
}

class VariableShortRecord implements DataObject {
	int size;
	short value;
	public VariableShortRecord(int numInts, short v){
		size = numInts;
		assert size <= 127;
		value = v;
	}

	public void setV(short v){
		value = v;
	}


	@Override
	public void read(int index, ByteBuffer bb) {
		size = bb.get(index);
		value = bb.getShort(index + 1);
		for (int i=1; i < size; ++i){
			assert bb.getShort(index + i*(Integer.SIZE / Byte.SIZE)+1) == value;
		}
	}

	@Override
	public void write(int index, ByteBuffer bb) {
		for (int i = 0; i < size; ++i) {
			bb.put(index+i*(Integer.SIZE / Byte.SIZE), (byte)size);
			bb.putShort(index+i*(Integer.SIZE / Byte.SIZE)+1, value);
		}
	}

	@Override
	public int size() {
		return size * (Integer.SIZE / Byte.SIZE);
	}

	@Override
	public DataObject copy() {
		return new VariableShortRecord(size, value);
	}

	public short getV() {
		return value;
	}

	public String toString() {
		return "{"+size+", "+value+"}";
	}
}

class GarbageBlockFile implements BlockFile {
	private BlockFile inner;
	private ByteBuffer garbage;

	public GarbageBlockFile(BlockFile o, int blockSize) {
		inner = o;
		garbage = ByteBuffer.allocate(blockSize);
	}

	@Override
	public void open(String filename, String mode) throws FileNotFoundException, IOException {
		inner.open(filename, mode);
	}

	@Override
	public void close() throws IOException {
		inner.close();
	}

	@Override
	public void append(int numBlocks) throws IOException {
		for (int i = 0; i < numBlocks; ++i){
			inner.write(inner.size(), garbage);
		}
	}

	@Override
	public void drop(int numBlocks) throws IOException {
		inner.drop(numBlocks);
	}

	@Override
	public int bytes() {
		return inner.bytes();
	}

	@Override
	public String filename()  {
		return inner.filename();
	}

	@Override
	public int size() throws IOException {
		return inner.size();
	}

	@Override
	public void write(int blockNo, ByteBuffer buffer) throws IOException {
		inner.write(blockNo, buffer);
	}

	@Override
	public void read(int blockNo, ByteBuffer buffer) throws IOException {
		inner.read(blockNo, buffer);
	}

	public void setGarbage(ByteBuffer bb) {
		garbage = bb;
	}
}


public class TIDTest {
	private  Function<Integer, ? extends BlockFile> bfgenerator;
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

	private <E extends DataObject> DirectRecordFile<TID, E> generate(int pageSize, E elem) throws IOException {
		bf = new GarbageBlockFile(bfgenerator.apply(pageSize), pageSize);
		bf.open(testFile.getCanonicalPath(), "rw");
		buf = new CountingDBBuffer(bufferGenerator.apply(pageSize));
		return Util.<E>generateTID(buf, bf);
	}

	private <E extends DataObject> ArrayList<TID> fill(DirectRecordFile<TID, E> file, ArrayList<E> data) throws Exception {
		ArrayList<TID> ret = new ArrayList<TID>();
		for (int i = 0; i < data.size(); ++i){
			ret.add(file.insert(data.get(i)));
		}
		return ret;
	}

	private ArrayList<DBInteger> random(int size) {
		ArrayList<DBInteger> ret = new ArrayList<>();
		Random r = new Random(42);
		for(int i=0; i < size; ++i){
			ret.add(new DBInteger(r.nextInt(Integer.MAX_VALUE)));
		}
		return ret;
	}

	// @param max: exclusive upper bound
	// @min: 1
	private ArrayList<VariableRecord> randomVR(int size, int max) {
		ArrayList<VariableRecord> ret = new ArrayList<>();
		Random r = new Random(1337);
		for (int i=0; i < size; ++i){
			ret.add(new VariableRecord(r.nextInt(max-1)+1));
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

	@Test
	public void freshTID0() throws Exception {
		// The first TID shall be TID(0, 0) on an empty file
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		idb.datatypes.TID first = drf.insert(new DBInteger(0));
		assertEquals(0,first.getIndex());
		assertEquals(0,first.getBlock());
	}

	@Test
	public void loadTest() throws Exception {
		ArrayList<DBInteger> data = random(10_000);
		DirectRecordFile<TID, DBInteger> drf = generate(4096, new DBInteger(0));
		ArrayList<TID> tids = fill(drf, data);
		for (int i = 0 ; i < tids.size(); ++i){
			DBInteger ret = new DBInteger(0);
			drf.read(tids.get(i), ret);
			assertEquals(data.get(i).getValue(), ret.getValue());
		}
	}

	private <D extends DataObject> boolean checkElement(ArrayList<D> data, D elem, BiFunction<D, D, Boolean> eq){
		Iterator<D> i = data.iterator();
		while(i.hasNext()) {
			D listE = i.next();
			if (eq.apply(listE, elem)) {
				i.remove();
				return true;
			}
		}
		return false;
	}

	private <D extends DataObject> void checkView(List<D> data, View<D> view, BiFunction<D, D, Boolean> eq, D item) throws Exception{
		ArrayList<D> copy = new ArrayList<D>();
		copy.addAll(data);
		view.restart();
		while(view.next(item)) {
			assertTrue(checkElement(copy, item, eq), "Element "+item+" was returned by the view without beeing in data");
		}
		assertEquals(new ArrayList<D>(), copy);
	}

	@Test
	public void emptyView() throws Exception {
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		checkView(List.<DBInteger>of(), drf.view(), (x, y) -> x.getValue() == y.getValue(), new DBInteger(0));
	}

	@Test
	public void unmovedTIDs() throws Exception {
		// unmoved TIDs have to be found in one fix, 0 unfix and 0 dirties
		ArrayList<DBInteger> data = random(100);
		DirectRecordFile<TID, DBInteger> drf = generate(4096, new DBInteger(0));
		// if no modify is called, they have to be at one place (as long as they're not splitted)
		// fragmented may only happen if new DBInteger(0).size() + 5 (max IndexSize) > 4096, which is not true
		ArrayList<TID> tids = fill(drf, data);
		buf.reset();
		for (int i = 0; i < tids.size(); ++i){
			drf.read(tids.get(i), new DBInteger(0));
			assertEquals(1, buf.getFix());
			assertEquals(1, buf.getUnfix());
			assertEquals(0, buf.getDirty());
			buf.reset();
		}
		checkView(data, drf.view(), (a, b) -> a.getValue() == b.getValue(), new DBInteger(0));
	}

	private void needsDirty(){
		assertTrue(1 <= buf.getDirty(), "This call needs to call setDirty at least once but this one didn't");
	}

	@Test
	public void maxSizeRecord() throws Exception {
		// an Index Entry is between 2 Byte (12 Bits 4096, 3 Bits Flags) and 5 Byte (4 Byte Integer, 3 Bits Flags).
		// a Record of size blockSize - 2*5 has to be accepted
		DirectRecordFile<TID, AdjustableRecord> drf = generate(4096, new AdjustableRecord(4086));
		buf.reset();
		idb.datatypes.TID tid = drf.insert(new AdjustableRecord(4086));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		drf.read(tid, new AdjustableRecord(4086));
	}
	@Test
	public void recordSizeChange() throws Exception {
		DirectRecordFile<TID, AdjustableRecord> drf = generate(4096, new AdjustableRecord(4086));
		idb.datatypes.TID tid = drf.insert(new AdjustableRecord(40));
		idb.datatypes.TID tid2 = drf.insert(new AdjustableRecord(40));
		idb.datatypes.TID tid3 = drf.insert(new AdjustableRecord(40));
		drf.modify(tid2, new AdjustableRecord(4080));
		drf.read(tid2, new AdjustableRecord(4080));
		drf.read(tid, new AdjustableRecord(40));
		drf.read(tid3, new AdjustableRecord(40));
	}
	@Test
	public void maybeOversizedRecord() throws Exception {
		// an Index Entry is between 2 Byte (12 Bits 4096, 3 Bits Flags) and 5 Byte (4 Byte Integer, 3 Bits Flags).
		// a Record between blocksize - 2*indexSize and blocksize - indexSize might break some logic, as it cannot be displaid in a regular
		// block, but does not need to be split, either.
		DirectRecordFile<TID, AdjustableRecord> drf = generate(4096, new AdjustableRecord(4091));
		idb.datatypes.TID tid = drf.insert(new AdjustableRecord(4091));
		drf.read(tid, new AdjustableRecord(4091));
	}
	@Test
	public void maybeOversizedRecord2() throws Exception {
		// an Index Entry is between 2 Byte (12 Bits 4096, 3 Bits Flags) and 5 Byte (4 Byte Integer, 3 Bits Flags).
		// a Record between blocksize - 2*indexSize and blocksize - indexSize might break some logic, as it cannot be displaid in a regular
		// block, but does not need to be split, either.
		DirectRecordFile<TID, AdjustableRecord> drf = generate(4096, new AdjustableRecord(4093));
		idb.datatypes.TID tid = drf.insert(new AdjustableRecord(4093));
		drf.read(tid, new AdjustableRecord(4093));
	}

	@Test
	public void fixUnfixBalance() throws Exception {
		ArrayList<VariableRecord> data = randomVR(1000, 1000); // 1000 ints is less than 4000 (bytes)
		DirectRecordFile<TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		ArrayList<TID> tids = fill(drf, data);
		assertEquals(buf.getFix(), buf.getUnfix());
	}

	@Test
	public void varSizeRecord() throws Exception {
		ArrayList<VariableRecord> data = randomVR(1000, 1000); // 1000 ints is less than 4000 (bytes)
		DirectRecordFile<TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		ArrayList<TID> tids = fill(drf, data);
		for (int i = 0 ; i < tids.size(); ++i){
			VariableRecord ret = new VariableRecord(0);
			drf.read(tids.get(i), ret);
			assertEquals(data.get(i).size(), ret.size());
		}
		checkView(data, drf.view(), (a, b) -> a.size() == b.size(), new VariableRecord(0));
	}

	@RepeatedTest(30)
	public void garbage(RepetitionInfo repetitionInfo) throws Exception {
		ArrayList<VariableRecord> data = randomVR(1000, 1000); // 1000 ints is less than 4000 (bytes)
		DirectRecordFile<TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		bf.setGarbage(randomBB(4096, repetitionInfo.getCurrentRepetition()));
		ArrayList<TID> tids = fill(drf, data);
		assertEquals(buf.getFix(), buf.getUnfix());
		for (int i = 0 ; i < tids.size(); ++i){
			VariableRecord ret = new VariableRecord(0);
			drf.read(tids.get(i), ret);
			assertEquals(data.get(i).size(), ret.size());
		}
		checkView(data, drf.view(), (a, b) -> a.size() == b.size(), new VariableRecord(0));
	}

	@Test
	public void afterDeleteRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		idb.datatypes.TID first = drf.insert(new DBInteger(0));
		idb.datatypes.TID snd = drf.insert(new DBInteger(200));
		drf.delete(first);
		DBInteger id = new DBInteger(0);
		drf.read(snd, id);
		assertEquals(200, id.getValue());
	}

	@Test
	public void deletedRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		idb.datatypes.TID first = drf.insert(new DBInteger(0));
		idb.datatypes.TID snd = drf.insert(new DBInteger(200));
		drf.delete(first);
		DBInteger id = new DBInteger(0);
		assertThrows(idb.record.DeletedRecordException.class, () ->drf.read(first, id));
	}

	@Test
	public void deletedMod() throws Exception {
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		idb.datatypes.TID first = drf.insert(new DBInteger(0));
		idb.datatypes.TID snd = drf.insert(new DBInteger(200));
		drf.delete(first);
		DBInteger id = new DBInteger(5);
		assertThrows(idb.record.DeletedRecordException.class, () ->drf.modify(first, id));
	}

	@Test
	public void deletedReadLater() throws Exception {
		DirectRecordFile<idb.datatypes.TID, idb.datatypes.DBInteger> drf = generate(4096, new DBInteger(0));
		idb.datatypes.TID first = drf.insert(new DBInteger(0));
		idb.datatypes.TID snd = drf.insert(new DBInteger(200));
		drf.delete(first);
		ArrayList<DBInteger> data = random(100);
		ArrayList<TID> tids = fill(drf, data);
		DBInteger id = new DBInteger(0);
		assertThrows(idb.record.DeletedRecordException.class, () ->drf.read(first, id));
	}

	@Test
	public void loadTestDeleted() throws Exception {
		Random r = new Random(11);
		ArrayList<DBInteger> data = random(10_000);
		DirectRecordFile<TID, DBInteger> drf = generate(4096, new DBInteger(0));
		ArrayList<TID> tids = fill(drf, data);
		ArrayList<DBInteger> rData = new ArrayList<>();
		for (int i = 0 ; i < tids.size(); ++i){
			if (r.nextBoolean()){
				DBInteger ret = new DBInteger(0);
				drf.read(tids.get(i), ret);
				assertEquals(data.get(i).getValue(), ret.getValue());
				rData.add(new DBInteger(data.get(i).getValue()));
			} else {
				drf.delete(tids.get(i));
			}
		}
		r.setSeed(11);
		for (int i = 0 ; i < tids.size(); ++i){
			if (r.nextBoolean()){
				DBInteger ret = new DBInteger(0);
				drf.read(tids.get(i), ret);
				assertEquals(data.get(i).getValue(), ret.getValue());
			} else {
				TID tid = tids.get(i);
				assertThrows(idb.record.DeletedRecordException.class, () -> drf.read(tid, new DBInteger(0)));
			}
		}
		checkView(rData, drf.view(), (a, b) -> a.getValue() == b.getValue(), new DBInteger(0));
	}

	@Test
	public void useBlocksEfficicy() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		assertEquals(first.getBlock(), snd.getBlock());
	}

	@Test
	public void afterMoveForwardRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		buf.reset();
		drf.modify(first, new VariableRecord(10));
		//modify with smaller may only touch one block
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(200 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(first, vr);
		assertEquals(10 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
	}

	@Test
	public void afterMoveBackwardRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		buf.reset();
		drf.modify(first, new VariableRecord(100)); // (100 + 200)*4 + 10 is still eanough (compare to 4096)
		//modify may only touch one block
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(200 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(first, vr);
		assertEquals(100 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
	}

	@Test
	public void afterMoveNewBlockRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(800));
		buf.reset();
		drf.modify(first, new VariableRecord(300)); // (300 + 800)*4 is too much (compare to 4096)
		//modify may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(800 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(first, vr);
		assertEquals(300 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		assertEquals(0, buf.getDirty());
	}

	@Test
	public void useBlocksEfficicy2() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(800));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		assertEquals(first.getBlock(), snd.getBlock());
	}

	@Test
	public void reclaimSpace1() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(800));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		drf.modify(first, new VariableRecord(10));
		TID trd = drf.insert(new VariableRecord(600));
		assertEquals(first.getBlock(), trd.getBlock());
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(200 * Integer.SIZE / Byte.SIZE, vr.size());
		drf.read(first, vr);
		assertEquals(10 * Integer.SIZE / Byte.SIZE, vr.size());
		drf.read(trd, vr);
		assertEquals(600 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void reclaimSpace2() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(800));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		drf.delete(first);
		TID trd = drf.insert(new VariableRecord(600));
		assertEquals(first.getBlock(), trd.getBlock());
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(200 * Integer.SIZE / Byte.SIZE, vr.size());
		drf.read(trd, vr);
		assertEquals(600 * Integer.SIZE / Byte.SIZE, vr.size());
	}

	@Test
	public void doubleMove() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(800));
		buf.reset();
		drf.modify(first, new VariableRecord(300)); // (300 + 800)*4 is too much (compare to 4096)
		//modify may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		TID trd = drf.insert(new VariableRecord(700)); // (700 + 300)*4 is eanough
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();
		drf.modify(first, new VariableRecord(400)); // (400 + 700)*4 is too much (compare to 4096)
		//modify may only touch three blocks
		assertEquals(3, buf.getFix());
		assertEquals(3, buf.getUnfix());
		needsDirty();
		buf.reset();
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(800 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(first, vr);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(trd, vr);
		assertEquals(700 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		ArrayList<VariableRecord> data = new ArrayList<>();
		data.add(new VariableRecord(800));
		data.add(new VariableRecord(400));
		data.add(new VariableRecord(700));
		checkView(data, drf.view(), (a, b) -> a.size() == b.size(), new VariableRecord(0));
	}
	@Test
	public void moveTiny() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableShortRecord> drf = generate(4096, new VariableShortRecord(1, (short)0));
		ArrayList<TID> tids = new ArrayList<>();
		ArrayList<VariableShortRecord> entries = new ArrayList<>();
		Random r = new Random(5);
		TID cur;
		do {
			VariableShortRecord vrs = new VariableShortRecord(1, (short) r.nextInt(Short.MAX_VALUE+1));
			cur = drf.insert(vrs);
			entries.add(vrs);
			tids.add(cur);
		} while (cur.getBlock() == 0);
		for (int i = 0; i < tids.size(); ++i){
			VariableShortRecord vrs = new VariableShortRecord(5, entries.get(i).getV());
			drf.modify(tids.get(i), vrs);
		}
		for (int i = 0; i < tids.size(); ++i){
			VariableShortRecord vrs = new VariableShortRecord(5, (short)0);
			drf.read(tids.get(i), vrs);
			assertEquals(entries.get(i).getV(), vrs.getV());
		}
		checkView(entries, drf.view(), (a, b) -> a.getV() == b.getV(), new VariableShortRecord(0, (short)0));
	}
	@Test
	// Note: This test is identical to moveTiny, but checks if instanceof(d) != instanceof(d.copy()) can be handled.
	// Always debug moveTiny first, and if foreignCopy still failes, start looking into this. This whole class (apart from foreignCopy)
	// does not fail if instanceof(d) == instanceof(d.copy()) is assumed. So debug foreignCopy only if it is the only failing testcase in TIDTest.
	public void foreinCopy() throws Exception {
		DirectRecordFile<idb.datatypes.TID, ForeignVSR> drf = generate(4096, new ForeignVSR(1, (short)0));
		ArrayList<TID> tids = new ArrayList<>();
		ArrayList<ForeignVSR> entries = new ArrayList<>();
		Random r = new Random(5);
		TID cur;
		do {
			ForeignVSR vrs = new ForeignVSR(1, (short) r.nextInt(Short.MAX_VALUE+1));
			cur = drf.insert(vrs);
			entries.add(vrs);
			tids.add(cur);
		} while (cur.getBlock() == 0);
		for (int i = 0; i < tids.size(); ++i){
			ForeignVSR vrs = new ForeignVSR(5, entries.get(i).getV());
			drf.modify(tids.get(i), vrs);
		}
		for (int i = 0; i < tids.size(); ++i){
			ForeignVSR vrs = new ForeignVSR(5, (short)0);
			drf.read(tids.get(i), vrs);
			assertEquals(entries.get(i).getV(), vrs.getV());
		}
		checkView(entries, drf.view(), (a, b) -> a.getV() == b.getV(), new ForeignVSR(0, (short)0));
	}
	@Test
	public void restart() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(0));
		Random r = new Random(915);
		TID cur;
		ArrayList<TID> tids = new ArrayList<>();
		ArrayList<VariableRecord> entries = new ArrayList<>();
		do {
			VariableRecord vrs = new VariableRecord(r.nextInt(50)+1);
			cur = drf.insert(vrs);
			entries.add(vrs);
			tids.add(cur);
		} while (cur.getBlock() < 20); // this stops as soon as one entry is written into block 21 (as no modifications take place, this should be correct)
		buf.close();
		bf.close();
		bf = null;
		CountingBlockFile cbf = new CountingBlockFile(bfgenerator.apply(4096));
		cbf.open(testFile.getCanonicalPath(), "rw");
		buf = new CountingDBBuffer(bufferGenerator.apply(4096));
		drf = Util.<VariableRecord>generateTID(buf, cbf);
		assertEquals(0, cbf.getWrites());
		assertEquals(21, cbf.getReads()); // these numbers should be needed to read the free memory eagerly.
		for (int i = 0; i < tids.size(); ++i){
			VariableRecord vrs = new VariableRecord(1);
			drf.read(tids.get(i), vrs); // reading has to work even if the obejct relevant for reading changes.
			assertEquals(entries.get(i).size(), vrs.size());
		}
		int oldTids = tids.size();
		for (int i = 0; i < oldTids; ++i){
			VariableRecord vr = new VariableRecord(r.nextInt(75)+1);
			drf.modify(tids.get(i), vr);
			entries.set(i, vr);
			vr = new VariableRecord(r.nextInt(30)+1);
			tids.add(drf.insert(vr));
			entries.add(vr);
		}
		for (int i = 0; i < tids.size(); ++i){
			VariableRecord vrs = new VariableRecord(1);
			drf.read(tids.get(i), vrs);
			assertEquals(entries.get(i).size(), vrs.size());
		}
		checkView(entries, drf.view(), (a, b) -> a.size() == b.size(), new VariableRecord(0));
	}
	@Test
	public void tidMove() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(10));
		idb.datatypes.TID fur = drf.insert(new VariableRecord(800));
		buf.reset();
		drf.modify(snd, new VariableRecord(300)); // (300 + 800)*4 is too much (compare to 4096)
		//modify may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		drf.modify(first, new VariableRecord(300)); // (300 + 800)*4 is too much (compare to 4096)
		//modify may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		TID trd = drf.insert(new VariableRecord(400)); // (700 + 300)*4 is eanough
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();
		drf.modify(first, new VariableRecord(400)); // (400 + 700)*4 is too much (compare to 4096)
		//modify may only touch three blocks
		assertEquals(3, buf.getFix());
		assertEquals(3, buf.getUnfix());
		needsDirty();
		buf.reset();
		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(300 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(first, vr);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();
		drf.read(trd, vr);
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
	}

	@Test
	public void movedDelete() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(10));
		idb.datatypes.TID fur = drf.insert(new VariableRecord(800));
		buf.reset();
		drf.modify(snd, new VariableRecord(300)); // (300 + 800)*4 is too much (compare to 4096)
		//modify may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		drf.delete(snd);
		//delete may only touch two blocks
		assertEquals(2, buf.getFix());
		assertEquals(2, buf.getUnfix());
		needsDirty();
		buf.reset();
		assertThrows(idb.record.DeletedRecordException.class, () -> drf.read(snd, new VariableRecord(2)));
		//deleteCheck may only touch one blocks
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
	}

	@Test
	public void extremeLargeWrite() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		buf.reset();
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(5500));
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
		needsDirty();
	}

	@Test
	public void extremeLargePosition() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new UnguardedVariableRecord(5500));
		assertNotEquals(first.getBlock(), snd.getBlock());
	}

	@Test
	public void extremeLargeUpdate() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new UnguardedVariableRecord(5500));
		drf.modify(first, new UnguardedVariableRecord(9900));
		drf.modify(snd, new UnguardedVariableRecord(50));
		UnguardedVariableRecord uvr = new UnguardedVariableRecord(1);
		drf.read(first, uvr);
		assertEquals(9900* Integer.SIZE / Byte.SIZE, uvr.size());
		drf.read(snd, uvr);
		assertEquals(50* Integer.SIZE / Byte.SIZE, uvr.size());
		// there are 1024 integer per Block. Subtracting 2 for TID and 3 for Index-Entries each, there are still 1019 integers per Block.
		// 9900 / 1019 is 9, remaining 729. If they happen to end up in the same block as the 50 (which is impossible, but ...), there is still some space
		// (729 + 50 < 1024). We want to check if that is registered correctly by inserting entries until we get one that matches with snd.getBlock.
		// We will stop if the drf allocates a new Block
		int curSize = bf.size();
		while (bf.size() == curSize) {
			if (drf.insert(new UnguardedVariableRecord(1)).getBlock() == snd.getBlock()) {
				drf.read(first, uvr);
				assertEquals(9900* Integer.SIZE / Byte.SIZE, uvr.size());
				drf.read(snd, uvr);
				assertEquals(50* Integer.SIZE / Byte.SIZE, uvr.size());
				return;
			}
		}
		fail("TID did extend its blockfile before adding to the now partially unused Block " + snd.getBlock());
	}

	@Test
	public void extremeLargeRead() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(5500));
		buf.reset();
		UnguardedVariableRecord uvr = new UnguardedVariableRecord(1);
		drf.read(first, uvr);
		assertEquals(5500* Integer.SIZE / Byte.SIZE, uvr.size());
		assertEquals(0, buf.getDirty());
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
	}

	@Test
	public void extremeLargeReadTwice() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		// We have to use at least 4096 - 10 - 8 bytes per Block.
		// So 5500 * 4 = 22000 uses at most 1610 bytes in the "last block"
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(5500));
		buf.reset();
		UnguardedVariableRecord uvr = new UnguardedVariableRecord(1);
		drf.read(first, uvr);
		assertEquals(5500* Integer.SIZE / Byte.SIZE, uvr.size());
		assertEquals(0, buf.getDirty());
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
		buf.reset();
		// So 5501 * 4 = 22004 uses at most 1614 bytes in the "last block"
		// 1614 + 1610 < 4096 - 10 - 8, so they have to share a common block
		idb.datatypes.TID snd = drf.insert(new UnguardedVariableRecord(5501));
		buf.reset();
		buf.flush();
		assertEquals(11, bf.size());
		drf.read(first, uvr);
		assertEquals(5500* Integer.SIZE / Byte.SIZE, uvr.size());
		assertEquals(0, buf.getDirty());
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
		buf.reset();
		drf.read(snd, uvr);
		assertEquals(5501* Integer.SIZE / Byte.SIZE, uvr.size());
		assertEquals(0, buf.getDirty());
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
		buf.reset();
	}

	@Test
	public void extremeLargeDelete() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(5500));
		buf.reset();
		drf.delete(first);
		needsDirty();
		assertEquals(6, buf.getFix());
		assertEquals(6, buf.getUnfix());
		buf.reset();
		UnguardedVariableRecord uvr = new UnguardedVariableRecord(1);
		assertThrows(idb.record.DeletedRecordException.class, () -> drf.read(first, uvr));
		assertEquals(0, buf.getDirty());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		ArrayList<UnguardedVariableRecord> list = new ArrayList<>(2);
		checkView(list, drf.view(), (a, b) -> a.size() == b.size(), new UnguardedVariableRecord(0));
	}
	@Test
	public void reclaimSpaceExtreme() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(5500));
		drf.delete(first);
		// 5500 Int result in approximatly 6 blocks (1024 per block - spare) * 6 = 61.. > 5500 > (1024 - x) * 5 < 5120
		// Check that the large TID is somewhere there
		assertTrue(first.getBlock() <= 6, "First should be at latest in Block 6");
		assertTrue(first.getBlock() >= 0,"First should be positive");
		// Now check that every block between 0 and 6 will be reused. Use a 600 Int record for that as two of them will not be able to squeeze in one block.
		boolean[] hit = new boolean[7]; // will be initialized with false;
		TID[] tids = new TID[7];

		for(int i=0; i < 7; i++){
			TID cur = drf.insert(new UnguardedVariableRecord(600));
			assertFalse(hit[cur.getBlock()], "using Block "+cur.getBlock()+" twice");
			hit[cur.getBlock()] =  true;
			tids[cur.getBlock()] = cur;
		}
		for (int i=0; i < 7; ++i){
			assertTrue(hit[i], "Did not reuse Block "+i);
		}
		UnguardedVariableRecord vr = new UnguardedVariableRecord(2);
		for (int i=0; i < 7; ++i){
			drf.read(tids[i], vr);
			assertEquals(600 * Integer.SIZE / Byte.SIZE, vr.size(), "in Block " + i);
		}
		buf.reset();
		assertThrows(idb.record.DeletedRecordException.class, () -> drf.read(first, vr));
		assertEquals(0, buf.getDirty());
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		ArrayList<UnguardedVariableRecord> list = new ArrayList<>(7);
		for (int i=0; i < 7; ++i)
			list.add(new UnguardedVariableRecord(600));
		checkView(list, drf.view(), (a, b) -> a.size() == b.size(), new UnguardedVariableRecord(0));
	}

	@Test
	public void extremeLargeUpdate2() throws Exception {
		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		idb.datatypes.TID first = drf.insert(new UnguardedVariableRecord(20));
		idb.datatypes.TID snd = drf.insert(new UnguardedVariableRecord(5500));
		drf.modify(first, new UnguardedVariableRecord(9900));
		drf.modify(snd, new UnguardedVariableRecord(4400));
		UnguardedVariableRecord uvr = new UnguardedVariableRecord(1);
		drf.read(first, uvr);
		assertEquals(9900* Integer.SIZE / Byte.SIZE, uvr.size());
		drf.read(snd, uvr);
		assertEquals(4400* Integer.SIZE / Byte.SIZE, uvr.size());
		ArrayList<UnguardedVariableRecord> list = new ArrayList<>(2);
		list.add(new UnguardedVariableRecord(9900));
		list.add(new UnguardedVariableRecord(4400));
		checkView(list, drf.view(), (a, b) -> a.size() == b.size(), new UnguardedVariableRecord(0));
	}

	@Test
	public void extremeString() throws Exception {
		DirectRecordFile<idb.datatypes.TID, DBString> drf = generate(4096, new DBString(""));
		ArrayList<DBInteger> data = random(7_201);
		StringBuilder sb = new StringBuilder();
		for(DBInteger i : data) {
			sb.append(i.getValue());
		}
		String res = sb.toString();
		DBString s = new DBString(res);
		TID first = drf.insert(s);
		DBString read = new DBString("");
		drf.read(first, read);
		assertEquals(read.toString(), res);
	}

	@Test
	public void deletedMove() throws Exception {
		DirectRecordFile<idb.datatypes.TID, VariableRecord> drf = generate(4096, new VariableRecord(3));
		idb.datatypes.TID first = drf.insert(new VariableRecord(300));
		idb.datatypes.TID snd = drf.insert(new VariableRecord(200));
		TID trd = drf.insert(new VariableRecord(250));
		TID four = drf.insert(new VariableRecord(25));
		TID five = drf.insert(new VariableRecord(50));
		TID six = drf.insert(new VariableRecord(75));
		assertEquals(0, first.getBlock());
		assertEquals(0, snd.getBlock());
		assertEquals(0, trd.getBlock());
		assertEquals(0, four.getBlock());
		assertEquals(0, five.getBlock());
		assertEquals(0, six.getBlock());
		drf.delete(first);
		drf.delete(four);
		buf.reset();
		drf.modify(snd, new VariableRecord(400));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();

		drf.delete(five);
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();

		drf.modify(trd, new VariableRecord(190));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		needsDirty();
		buf.reset();

		VariableRecord vr = new VariableRecord(2);
		drf.read(snd, vr);
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		assertEquals(400 * Integer.SIZE / Byte.SIZE, vr.size());
		buf.reset();

		drf.read(trd, vr);
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		assertEquals(190 * Integer.SIZE / Byte.SIZE, vr.size());
		buf.reset();

		assertThrows(DeletedRecordException.class, () -> drf.read(first, vr));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();

		assertThrows(DeletedRecordException.class, () -> drf.read(four, vr));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();

		assertThrows(DeletedRecordException.class, () -> drf.read(five, vr));
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		buf.reset();

		drf.read(six, vr);
		assertEquals(1, buf.getFix());
		assertEquals(1, buf.getUnfix());
		assertEquals(0, buf.getDirty());
		assertEquals(75 * Integer.SIZE / Byte.SIZE, vr.size());


		checkView(List.of(new VariableRecord(75), new VariableRecord(190), new VariableRecord(400)), drf.view(), (a, b) -> a.size() == b.size(), new VariableRecord(0));
	}

	// @author Daniel Schuell, Tobias Heineken
	@RepeatedTest(20)
	public void randomModifyOversized(RepetitionInfo repInfo) throws Exception {
		Random rng = new Random(repInfo.getCurrentRepetition() + 4091);
		int recordCount = rng.nextInt(3) + 3;

		// start with size=4 where all records initially fit in first block so all tids will be on one block
		// and then increase so tids are spread across blocks until size=8000
		int initialRecordSize = Integer.BYTES + (repInfo.getCurrentRepetition() - 1) * 8000 / repInfo.getTotalRepetitions();

		DirectRecordFile<idb.datatypes.TID, UnguardedVariableRecord> drf = generate(4096, new UnguardedVariableRecord(3));
		TID[] tids = new TID[recordCount];
		UnguardedVariableRecord[] records = new UnguardedVariableRecord[recordCount];
		for (int i=0; i < recordCount; ++i) {
			records[i] = new UnguardedVariableRecord(initialRecordSize / Integer.BYTES);
			tids[i] = drf.insert(records[i]);
			needsDirty();
			assertEquals(buf.getFix(), buf.getUnfix());
			buf.reset();
		}
		final int iter_count = 300;
		for (int i=0; i < iter_count; ++i) {
			//TODO: reopen at r.nextInt(20) == 0
			// check contents
			checkView(List.of(records), drf.view(), (a, b) -> a.size() == b.size(), new UnguardedVariableRecord(0));
			assertEquals(0, buf.getDirty());
			assertEquals(buf.getFix(), buf.getUnfix());
			buf.reset();
			// change size of random record
			int record = rng.nextInt(recordCount);
			int newSize = Integer.BYTES + rng.nextInt(8000);
			records[record] = new UnguardedVariableRecord(newSize / Integer.BYTES);
			drf.modify(tids[record], records[record]);
			needsDirty();
			assertEquals(buf.getFix(), buf.getUnfix());
			buf.reset();
		}
		checkView(List.of(records), drf.view(), (a, b) -> a.size() == b.size(), new UnguardedVariableRecord(0));
	}
}
