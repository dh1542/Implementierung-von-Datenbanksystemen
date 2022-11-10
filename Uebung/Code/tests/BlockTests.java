import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.function.Function;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.nio.ByteBuffer;

import idb.block.BlockFile;

import idb.construct.Util;

public class BlockTests{
	private static Function<Integer, ? extends BlockFile> generator;
	private File testFile;

	@BeforeEach
	public void init() throws IOException{
		generator = a -> Util.generateBlockFile(a);
		testFile = File.createTempFile("foobar", null);
	}

	@AfterEach
	public void cleanup() {
		testFile.delete();
	}

	@Test
	public void closeWithoutOpen() throws IOException {
		// check for correct handling of close without open
		try (BlockFile bf = generator.apply(4096)) {
		}
	}

	@Test
	public void doubleClose() throws IOException{
		// check for correct handling of double close
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.close();
		}
	}

	@Test
	public void blockSize() throws IOException{
		for(int i = 1 << 10; i < (1 << 15); i <<= 1){
			try (BlockFile bf = generator.apply(i)) {
				assertEquals(i, bf.bytes());
			}
		}
	}

	@Test
	public void readPastTheEnd() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			ByteBuffer bb = ByteBuffer.allocate(4096);
			assertThrows(EOFException.class, ()->bf.read(1, bb));
		}
	}

	@Test
	public void readWrongSize() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(1);
			ByteBuffer bb = ByteBuffer.allocate(4095);
			assertThrows(IllegalArgumentException.class, ()->bf.read(1, bb));
		}
	}

	@Test
	public void writeWrongSize() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(1);
			ByteBuffer bb = ByteBuffer.allocate(4095);
			assertThrows(IllegalArgumentException.class, ()->bf.write(1, bb));
		}
	}

	@Test
	public void readWritten() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(1);
			ByteBuffer read = ByteBuffer.allocate(4096);
			ByteBuffer write = ByteBuffer.allocate(4096);
			for (int i=0; i < 20; i++) {
				write.putInt(i);
			}
			bf.write(0, write);
			bf.read(0, read);
			for (int i=0; i < 20; i++) {
				int found = read.getInt();
				assertEquals(i, found);
			}
		}
	}

	@Test
	public void readAbsWritten() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(1);
			ByteBuffer read = ByteBuffer.allocate(4096);
			ByteBuffer write = ByteBuffer.allocate(4096);
			for (int i=0; i < 20; i++) {
				write.putInt(i * 10, i);
			}
			bf.write(0, write);
			bf.read(0, read);
			for (int i=0; i < 20; i++) {
				int found = read.getInt(i * 10);
				assertEquals(i, found);
			}
		}
	}

	@Test
	public void readWrittenPost0() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(3);
			ByteBuffer read = ByteBuffer.allocate(4096);
			ByteBuffer write = ByteBuffer.allocate(4096);
			for (int i=0; i < 20; i++) {
				write.putInt(i);
			}
			bf.write(1, write);
			bf.write(2, write);
			bf.read(1, read);
			for (int i=0; i < 20; i++) {
				int found = read.getInt();
				assertEquals(i, found);
			}
			bf.read(2, read);
			read.position(0);
			for (int i=0; i < 20; i++) {
				int found = read.getInt();
				assertEquals(i, found);
			}
		}
	}

	@Test
	public void readUninitPost0() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(2);
			ByteBuffer read = ByteBuffer.allocate(4096);
			bf.read(1, read);
		}
	}

	@Test
	public void extendedSize() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			bf.append(1);
			assertEquals(1, bf.size());
		}
	}

	@Test
	public void implicitExtendedSize() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			ByteBuffer write = ByteBuffer.allocate(4096);
			bf.write(10, write);
			assertEquals(11, bf.size());
			bf.write(12, write);
			assertEquals(13, bf.size());
		}
	}

	@Test
	public void readWrittenTwoFiles() throws IOException{
		try (BlockFile bfread = generator.apply(4096); BlockFile bfwrite = generator.apply(4096)) {
			bfwrite.open(testFile.getCanonicalPath(), "rw");
			bfread.open(testFile.getCanonicalPath(), "rw");
			bfwrite.append(1);
			ByteBuffer read = ByteBuffer.allocate(4096);
			ByteBuffer write = ByteBuffer.allocate(4096);
			for (int i=0; i < 20; i++) {
				write.putInt(i);
			}
			bfwrite.write(0, write);
			bfread.read(0, read);
			for (int i=0; i < 20; i++) {
				int found = read.getInt();
				assertEquals(i, found);
			}
		}
	}

	@Test
	public void sizeTwoFiles() throws IOException{
		try (BlockFile bfread = generator.apply(4096); BlockFile bfwrite = generator.apply(4096)) {
			bfwrite.open(testFile.getCanonicalPath(), "rw");
			bfwrite.append(2);
			assertEquals(2, bfwrite.size());
			bfwrite.close();
			bfread.open(testFile.getCanonicalPath(), "rw");
			assertEquals(2, bfread.size());
			bfread.append(1);
			assertEquals(3, bfread.size());
		}
	}

	@Test
	public void dropSize() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			ByteBuffer write = ByteBuffer.allocate(4096);
			bf.write(10, write);
			bf.drop(8);
			assertEquals(3, bf.size());
		}
	}

	@Test
	public void freshFilename() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			assertNull(bf.filename());
		}
	}

	@Test
	public void correctFilename() throws IOException{
		try (BlockFile bf = generator.apply(4096)) {
			bf.open(testFile.getCanonicalPath(), "rw");
			assertEquals(testFile.getCanonicalPath(), bf.filename());
		}
	}
}
