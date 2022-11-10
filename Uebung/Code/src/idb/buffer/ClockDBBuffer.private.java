/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.buffer;

import java.util.HashMap;
import java.nio.ByteBuffer;
import java.io.IOException;

import idb.block.BlockFile;


public class ClockDBBuffer implements DBBuffer
{
	private HashMap<PageDescriptor, Integer> pageAssignment;
	private Page pages[];
	private BlockFile bfiles[];
	private int pagesize;
	private int cnt = 0;
	private boolean tokens[];

	public ClockDBBuffer(int pageSize, int numberOfPages) {
		pageAssignment = new HashMap<>();
		pagesize = pageSize;
		pages = new Page[numberOfPages];
		for (int i = 0; i < pages.length; ++i){
			pages[i] = new Page(pagesize);
		}
		bfiles = new BlockFile[numberOfPages];
		tokens = new boolean[numberOfPages];
	}

	@Override
	public ByteBuffer fix(BlockFile file, int pageno) throws IOException, BufferFullException{
		PageDescriptor pd = new PageDescriptor(file.filename(), pageno);
		Integer pageNr = pageAssignment.get(pd);
		if (pageNr == null) {
			// Not referenced yet
			pageNr = findEmptyIndex();
			if (bfiles[pageNr] != null) {
				int blockNo = getBlocknoForPageIndex(pageNr);
				try{
					pages[pageNr].clear(bfiles[pageNr], blockNo);
				} catch(BufferNotEmptyException bnee) {
					//This is a programming error
					throw new RuntimeException("This is impossible", bnee);
				}
				PageDescriptor delete = new PageDescriptor(bfiles[pageNr].filename(), blockNo);
				pageAssignment.remove(delete);
			}
			pageAssignment.put(pd, pageNr);
			pages[pageNr].load(file, pageno);
		}
		pages[pageNr].fix();
		tokens[pageNr] = true;
		return pages[pageNr].getData();
	}

	private int findEmptyIndex() throws BufferFullException {
		do {
			boolean modified = false;
			for(int i = 0; i < pages.length; ++i){
				if (pages[cnt].getNumFix() == 0) {
					modified = true;
					if (!tokens[cnt]) {
						int res = cnt;
						cnt++;
						if (cnt >= pages.length) {
							cnt -= pages.length;
						}
						return res;
					}
					tokens[cnt] = false;
				}
				cnt++;
				if (cnt >= pages.length) {
					cnt -= pages.length;
				}
			}
			if (!modified) {
				throw new BufferFullException();
			}
		} while(true);
	}

	@Override
	public void setDirty(BlockFile file, int pageno) {
		PageDescriptor pd = new PageDescriptor(file.filename(), pageno);
		Page page = pages[pageAssignment.get(pd)];
		page.setDirty();
	}

	@Override
	public void unfix(BlockFile blockfile, int pageno) throws IOException {
		PageDescriptor pd = new PageDescriptor(blockfile.filename(), pageno);
		Page page = pages[pageAssignment.get(pd)];
		page.unfix();
		bfiles[pageAssignment.get(pd)] = blockfile;
	}

	@Override
	public int getPagesize() {
		return pagesize;
	}

	@Override
	public void flush() throws IOException {
		for(int i = 0; i < pages.length; ++i){
			if (pages[i].getNumFix() == 0) {
				try {
					pages[i].clear(bfiles[i], getBlocknoForPageIndex(i), false);
				} catch (BufferNotEmptyException bnee) {
					// This is a programming error
					throw new RuntimeException("This is impossible", bnee);
				}
			}
		}
	}

	// calculates the blockno for a given index into pages, returns -1 if no is available
	private int getBlocknoForPageIndex(int pageNo){
		for(java.util.Map.Entry<PageDescriptor, Integer> e : pageAssignment.entrySet()) {
			if (e.getValue() == pageNo) return e.getKey().getPageNr();
		}
		return -1;
	}

	@Override
	public void close() throws BufferNotEmptyException, IOException {
		for(int i = 0; i < pages.length; ++i){
			pages[i].clear(bfiles[i], getBlocknoForPageIndex(i));
		}
	}
}
