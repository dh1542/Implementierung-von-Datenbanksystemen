import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.function.Function;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.FileNotFoundException;
import java.io.File;

import idb.buffer.DBBuffer;
import idb.buffer.BufferNotEmptyException;
import idb.buffer.BufferFullException;
import idb.block.BlockFile;

import idb.construct.Util;

class CountingBlockFile implements BlockFile {
		private BlockFile inner;
		private int numWrites = 0;
		private int numReads = 0;
		private int expRead = -1;
		private int expWrite = -1;

		public CountingBlockFile(BlockFile o) {
			inner = o;
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
			inner.append(numBlocks);
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
			if (expWrite != -1) {
				assertEquals(expWrite, blockNo);
			}
			expWrite = -1;
			numWrites++;
			inner.write(blockNo, buffer);
		}

		@Override
		public void read(int blockNo, ByteBuffer buffer) throws IOException {
			if (expRead != -1) {
				assertEquals(expRead, blockNo);
			}
			expRead = -1;
			numReads++;
			inner.read(blockNo, buffer);
		}

		public int getReads() {
			return numReads;
		}

		public int getWrites() {
			return numWrites;
		}

		public void setExpRead(int eR) {
			expRead = eR;
		}

		public void setExpWrite(int eW) {
			expWrite = eW;
		}

		public void reset() {
			numReads = 0;
			numWrites = 0;
		}
	}
public abstract class AbstractBufferTest{

	protected Function<Integer, CountingBlockFile> bfGenerator;
	protected File testFile;
	protected Function<Integer, ? extends DBBuffer> bufferGenerator;

	@BeforeEach
	public void init() throws IOException{
		bfGenerator = (Integer a) -> new CountingBlockFile(Util.generateBlockFile(a));
		testFile = File.createTempFile("foobar", null);
		bufferGenerator = makeBufferGenerator(20);
	}

	protected abstract Function<Integer, ? extends DBBuffer> makeBufferGenerator(int numberOfPages);

	@AfterEach
	public void cleanup() {
		testFile.delete();
	}

	@Test
	public void notFixedUnfix() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		BlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		assertThrows(Exception.class, () -> buf.unfix(bf, 0));
	}

	@Test
	public void notFixedDirty() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		BlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		assertThrows(Exception.class, () -> buf.setDirty(bf, 0));
	}

	@Test
	public void emptyFix() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		BlockFile bf = bfGenerator.apply(4096);
		// bf does not point to an file
		assertThrows(Exception.class, () -> buf.fix(bf, 0));
	}

	@Test
	public void doubleFix() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1);
		assertEquals(4096, buf.fix(bf, 1).capacity());
		assertEquals(1, bf.getReads());
	}

	@Test
	public void fixClose() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1);
		assertThrows(BufferNotEmptyException.class, () -> buf.close());
	}

	@Test
	public void noDirty() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1);
		buf.unfix(bf, 1);
		buf.flush();
		assertEquals(0, bf.getWrites());
	}

	@Test
	public void setDirty() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		buf.flush();
		assertEquals(1, bf.getWrites());
	}


	@Test
	public void doubleFixUnfixClose() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1);
		buf.fix(bf, 1);
		buf.unfix(bf, 1);
		assertThrows(BufferNotEmptyException.class, () -> buf.close());
	}

	@Test
	public void refixContent() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		assertEquals(15, buf.fix(bf, 1).getInt());
	}

	@Test
	public void flushContent() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		buf.flush();
		DBBuffer buf2 = buf;
		buf = bufferGenerator.apply(4096);
		assertEquals(15, buf.fix(bf, 1).getInt());
		buf2.close();
	}

	@Test
	public void closeContent() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		buf.close();
		buf = bufferGenerator.apply(4096);
		assertEquals(15, buf.fix(bf, 1).getInt());
	}

	@Test
	public void refixTwoContent() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		buf.fix(bf, 0).putInt(5);
		buf.setDirty(bf, 0);
		buf.unfix(bf, 0);
		assertEquals(15, buf.fix(bf, 1).getInt());
		assertEquals(5, buf.fix(bf, 0).getInt());
	}

	@Test
	public void closeTwoContent() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		buf.fix(bf, 0).putInt(5);
		buf.setDirty(bf, 0);
		buf.unfix(bf, 0);
		buf.close();
		buf = bufferGenerator.apply(4096);
		assertEquals(15, buf.fix(bf, 1).getInt());
		assertEquals(5, buf.fix(bf, 0).getInt());
	}

	@Test
	public void pagesizes() throws Exception {
		for(int i=0; i < 15; ++i){
			DBBuffer buf = bufferGenerator.apply(2 << i);
			assertEquals(2 << i, buf.getPagesize());
		}
	}

	@Test
	public void tenPageLoad() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(400);
		for(int i = 0; i < 400; ++i){
			buf.fix(bf, i).putInt(i);
			buf.setDirty(bf, i);
			if (i > 10) {
				buf.unfix(bf, i - 10);
			}
		}
		for (int i = 390; i < 400; ++i) {
			buf.unfix(bf, i);
		}
		for (int i=400-1; i >= 0; --i) {
			assertEquals(i, buf.fix(bf, i).getInt());
			buf.unfix(bf, i);
		}
		for(int i = 0; i < 400; ++i){
			assertEquals(i, buf.fix(bf, i).getInt());
			buf.unfix(bf, i);
		}
	}

	@Test
	public void doubleFixTwoBlockfile() throws Exception {
		CountingBlockFile bf0 = bfGenerator.apply(4096);
		bf0.open(testFile.getCanonicalPath(), "rw");
		bf0.append(2);
		bf0.close();

		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf1 = bfGenerator.apply(4096);
		bf1.open(testFile.getCanonicalPath(), "rw");
		CountingBlockFile bf2 = bfGenerator.apply(4096);
		bf2.open(testFile.getCanonicalPath(), "rw");
		buf.fix(bf1, 1);
		buf.fix(bf2, 1);
		assertEquals(1, bf1.getReads()+bf2.getReads());
	}

	@Test
	public void checkOverride() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(2);
		buf.fix(bf, 1).putInt(15);
		buf.setDirty(bf, 1);
		buf.fix(bf, 1).putInt(5);
		buf.unfix(bf, 1);
		buf.unfix(bf, 1);
		assertEquals(5, buf.fix(bf, 1).getInt());
	}
}
