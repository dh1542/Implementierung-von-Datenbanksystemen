import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.function.Function;
import java.io.IOException;
import java.nio.ByteBuffer;

import idb.buffer.DBBuffer;
import idb.buffer.BufferFullException;
import idb.buffer.BufferNotEmptyException;

public class ClockBufferTest extends AbstractBufferTest {
	private int num;
	@Override
	protected Function<Integer, ? extends DBBuffer> makeBufferGenerator(int numberOfPages) {
		num = numberOfPages;
		return Util2.clockBufferGenerator(numberOfPages);
	}

	@Test
	public void exhaustCapacity() throws IOException, BufferFullException {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(num + 5);
		for(int i=0; i < num; ++i){
			buf.fix(bf, i);
		}
		assertThrows(BufferFullException.class, () -> buf.fix(bf, num + 2));
	}

	@Test
	public void postFlushFix() throws Exception {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(num + 5);
		ByteBuffer bb = buf.fix(bf, 1);
		bb.putInt(45);
		bb.putInt(50);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		bf.reset();
		buf.flush();
		assertEquals(0, bf.getReads());
		assertEquals(1, bf.getWrites());
		bf.reset();
		bb = buf.fix(bf, 1);
		assertEquals(0, bf.getReads());
		assertEquals(0, bf.getWrites());
		assertEquals(45, bb.getInt());
		assertEquals(50, bb.getInt());
	}

	@Test
	public void clockBehavior() throws IOException, BufferFullException, BufferNotEmptyException {
		DBBuffer buf = bufferGenerator.apply(4096);
		CountingBlockFile bf = bfGenerator.apply(4096);
		bf.open(testFile.getCanonicalPath(), "rw");
		bf.append(num + 5);
		for(int i=0; i < num-3; ++i){
			buf.fix(bf, i+8);
		}
		assertEquals(num-3, bf.getReads());
		bf.setExpRead(0);
		buf.fix(bf, 0);
		buf.setDirty(bf, 0);
		buf.unfix(bf, 0);
		bf.setExpRead(1);
		buf.fix(bf, 1);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		bf.setExpRead(2);
		buf.fix(bf, 2);
		buf.setDirty(bf, 2);
		buf.unfix(bf, 2);
		// State: 0TC, 1T, 2T,
		bf.setExpWrite(0);
		bf.setExpRead(3);
		buf.fix(bf, 3);
		buf.setDirty(bf, 3);
		buf.unfix(bf, 3);
		// State: 3T, 1C, 2
		bf.setExpRead(4);
		bf.setExpWrite(2);
		buf.fix(bf, 1);
		buf.setDirty(bf, 1);
		buf.unfix(bf, 1);
		// State: 3T, 1TC, 2
		buf.fix(bf, 4);
		buf.setDirty(bf, 4);
		buf.unfix(bf, 4);
		// State: 3TC, 1, 4T
		bf.setExpRead(2);
		bf.setExpWrite(1);
		buf.fix(bf, 4);
		buf.setDirty(bf, 4);
		buf.unfix(bf, 4);
		buf.fix(bf, 2);
		buf.setDirty(bf, 2);
		buf.unfix(bf, 2);
		for(int i=0; i < num-3; ++i){
			buf.unfix(bf, i+8);
		}
		buf.close();
		assertEquals(6, bf.getWrites());
	}
}
