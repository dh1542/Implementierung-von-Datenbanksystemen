package idb.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
/**
 * The common interface for all database buffer implementations. It supports
 * the basic operations of fixing a page, marking it as dirty to write it
 * back, and unfix it. By the construction, the pagesize for this buffer should be defined.
 * @see BufferBase
 * @author Christoph Merdes
 *
 */
public interface Buffer {
	/**
	 * Fix a page from a segment in this buffer. It will not be removed until\
	 * it is unfixed.
	 * @param segno The segment descriptor
	 * @param pageno The page number
	 * @return The ByteBuffer representing the bytes of the page
	 * @throws IOException Indicates a problem with the underlying file system
	 * @throws BufferFullException Indicates that the buffer capacity is exceeded
	 */
	public ByteBuffer fix(int segno, int pageno) throws IOException, BufferFullException;
	/**
	 * Notify the buffer, that you are done with this page. This must not result in
	 * instant writing the page back.
	 * @param segno The segment descriptor
	 * @param pageno The page number
	 * @throws IOException Indicates a problem with the underlying filesystem
	 */
	public void unfix(int segno, int pageno) throws IOException;
	/**
	 * Mark this page as written. If a page is unfixed and setDirty was not called,
	 * the buffer will NOT write this page persistently
	 * @param segno The segment descriptor
	 * @param pageno The page number
	 */
	public void setDirty(int segno, int pageno);
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
	 * flush buffer and close all segments
	 * @throws IOException signals an error in the underlying file system
	 * @throws BufferNotEmptyException thrown, when fixed pages remain
	 * in the buffer on closing
	 */
	public void close() throws BufferNotEmptyException, IOException;
}
