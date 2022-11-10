/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.buffer;

import java.util.HashMap;
import java.nio.ByteBuffer;
import java.io.IOException;

import idb.block.BlockFile;


public class SimpleDBBuffer implements DBBuffer
{
	private HashMap<PageDescriptor, Page> pages;
	private int pagesize;

	public SimpleDBBuffer(int page) {
		pages = new HashMap<>();
		pagesize = page;
	}

	@Override
	public ByteBuffer fix(BlockFile blockfile, int pageno) throws IOException, BufferFullException{
		PageDescriptor pd = new PageDescriptor(blockfile.filename(), pageno);
		Page page = pages.get(pd);
		if (page == null) {
			// Not referenced yet
			page = new Page(pagesize);
			pages.put(pd, page);
			page.load(blockfile, pageno);
		}
		page.fix();
		return page.getData();
	}

	@Override
	public void setDirty(BlockFile blockfile, int pageno) {
		PageDescriptor pd = new PageDescriptor(blockfile.filename(), pageno);
		Page page = pages.get(pd);
		page.setDirty();
	}

	@Override
	public void unfix(BlockFile blockfile, int pageno) throws IOException {
		PageDescriptor pd = new PageDescriptor(blockfile.filename(), pageno);
		Page page = pages.get(pd);
		page.unfix();
		if (page.getNumFix() == 0) {
			try{
				page.clear(blockfile, pageno);
				pages.remove(pd);
			}
			catch (BufferNotEmptyException bnee) {
				// this is a programming Error
				throw new RuntimeException("This is invalid", bnee);
			}
		}
	}

	@Override
	public int getPagesize() {
		return pagesize;
	}

	@Override
	public void flush() {
		return; // this buffer doesn't buffer at all.
	}

	@Override
	public void close() throws BufferNotEmptyException {
		if (pages.size() != 0) {
			throw new BufferNotEmptyException("Buffer contains " + pages.size() + " elements on closing");
		}
	}
}
