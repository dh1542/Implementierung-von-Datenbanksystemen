/**
 * @file
 *
 * Interface for a BlockFile.
 * It allows appending, reading, writing of blocks.
 *
 * @author Tobias Heineken <tobias.heineken@fau.de>
 *
 */
package idb.block;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.AutoCloseable;
import java.nio.ByteBuffer;

public interface BlockFile extends Closeable, AutoCloseable {
	/**
	 * Opens a file.
	 * @param filename A path to the file
	 * @param mode A String describing the mode for the file, usually "rw". see @RandomAccessFile::open
	 */
	public void open(String filename, String mode) throws FileNotFoundException, IOException;

	/**
	 * Closes the current File and releases all ressources.
	 * Using a BlockFile after calling close() is illegal and undefined behaviour.
	 */
	public void close() throws IOException;

	/**
	 * Appends a file.
	 * This operation is only valid after a call to open.
	 * The content of the appended blocks in udefined
	 *
	 * @param numBlocks The number of blocks to be appended at the end of the file
	 */
	public void append(int numBlocks) throws IOException;

	/**
	 * Writes a block to disk.
	 * This operation is only valid after a call to open.
	 * If blockNo is greater or equals to size(), the file is extended (see append) to hold exactly blockNo+1 blocks, otherwise the block is overridden
	 *
	 * @param blockNo The blocknumber to write to
	 * @param buffer a ByteBuffer with size bytes() to be witten to the file
	 */
	public void write(int blockNo, ByteBuffer buffer) throws IOException;

	/**
	 * Reads a block from disk.
	 * This operation is only valid after a call to open.
	 * BlockNo may not be equals or greater to size().
	 *
	 * @param blockNo The blocknumber to read from
	 * @param buffer a ByteBuffer with size bytes() to be read into
	 * @throws EOFException if the referenced BlockNo is not valid, i.e. larger than size()
	 */
	public void read(int blockNo, ByteBuffer buffer) throws IOException;

	/**
	 * Returns the number of blocks in this blockfile.
	 * This operation is only valid after a call to open.
	 *
	 * @return The number of blocks
	 */
	public int size() throws IOException;

	/**
	 * Returns the number of bytes in a block.
	 *
	 * @return The number of bytes in a block
	 */
	public int bytes();

	/**
	 * Drops the last numBlock block of this file.
	 * This is the inverse operation to append.
	 * This operation is only valid after a call to open.
	 *
	 * @param numBlocks The number of blocks that should be removed.
	 */
	public void drop(int numBlocks) throws IOException;

	/**
	 * Returns the current filename.
	 *
	 * @return The current filename or null, if no file has been opened yet.
	 */
	public String filename();
}
