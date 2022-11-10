package idb.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import idb.block.BlockFile;
/**
 * The common interface for all database buffer implementations. It supports
 * the basic operations of fixing a page, marking it as dirty to write it
 * back, and unfix it. By the construction, the pagesize for this buffer should be defined.
 * This interface assumes "direkte Seiteneinbringung und direkte Seitenzuordnung"
 * @author Christoph Merdes
 *
 */
public interface DBBuffer {
	/**
	 * Fix a page from a segment in this buffer. It will not be removed until\
	 * it is unfixed.
	 * @param blockfile The Blockfile
	 * @param pageno The page number
	 * @return The ByteBuffer representing the bytes of the page
	 * @throws IOException Indicates a problem with the underlying file system
	 * @throws BufferFullException Indicates that the buffer capacity is exceeded
	 */
	public ByteBuffer fix(BlockFile blockfile, int pageno) throws IOException, BufferFullException;
	/**
	 * Notify the buffer, that you are done with this page. This must not result in
	 * instant writing the page back.
	 * @param blockfile The Blockfile
	 * @param pageno The page number
	 * @throws IOException Indicates a problem with the underlying filesystem
	 */
	public void unfix(BlockFile blockfile, int pageno) throws IOException;
	/**
	 * Mark this page as written. If a page is unfixed and setDirty was not called,
	 * the buffer will NOT write this page persistently
	 * @param blockfile The Blockfile
	 * @param pageno The page number
	 */
	public void setDirty(BlockFile blockfile, int pageno);
	/**
	 * Retrieve the page size of this buffer
	 * @return The pagesize of this buffer
	 */
	public int getPagesize();
	/**
	 * write the unfixed, buffered pages back to disk
	 * @throws IOException signals an error in the underlying file system
	 */
	public void flush() throws IOException;
	/**
	 * Flushes and closes the buffer.
	 * Using the buffer after calling close() is undefined.
	 * Does not close any BlockFiles.
	 * @throws BufferNotEmptyException if there are fixed pages remaining
	 * @throws IOException signals an error in the underlying file system
	 * in the buffer on closing
	 */
	public void close() throws BufferNotEmptyException, IOException;
}
