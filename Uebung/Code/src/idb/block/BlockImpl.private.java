/**
 * @file
 *
 * Implentation for a BlockFile.
 * It allows appending, reading, writing of blocks.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */

package idb.block;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class BlockImpl implements BlockFile
{
	private int blocksize;
	private RandomAccessFile file = null;
	private String filename = null;

	public BlockImpl(int blocksize) {
		this.blocksize = blocksize;
	}

	@Override
	public int bytes() {
		return blocksize;
	}

	@Override
	public int size() throws IOException {
		return (int)(file.length() / blocksize);
	}

	@Override
	public void append(int numBlocks) throws IOException {
		file.setLength(file.length() + blocksize * numBlocks);
	}

	@Override
	public void drop(int numBlocks) throws IOException {
		file.setLength(file.length() - blocksize * numBlocks);
	}

	@Override
	public void close() throws IOException {
		if (file == null) return;
		file.close();
	}

	@Override
	public void write(int blockNo, ByteBuffer buffer) throws IOException {
		if (buffer.capacity() != blocksize) throw new IllegalArgumentException("This buffer has a wrong size for this file");
		file.seek(blocksize*blockNo);
		file.write(buffer.array());
	}

	@Override
	public void read(int blockNo, ByteBuffer buffer) throws IOException {
		if (buffer.capacity() != blocksize) throw new IllegalArgumentException("This buffer has a wrong size for this file");
		file.seek(blocksize*blockNo);
		file.readFully(buffer.array());
	}

	@Override
	public void open(String filename, String mode) throws FileNotFoundException, IOException {
		this.filename = filename;
		file = new RandomAccessFile(filename, mode);
		if (file.length() % blocksize != 0) throw new IllegalArgumentException("This file does not use the correct blocksize");
	}

	@Override
	public String filename() {
		return filename;
	}
}
