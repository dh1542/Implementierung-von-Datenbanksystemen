/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.buffer;

import idb.block.BlockFile;

class PageDescriptor{
	private String name;
	private int pageNr;
	public PageDescriptor(String n, int page) {
		name = n;
		pageNr = page;
	}
	public int getPageNr() {
		return pageNr;
	}
	public String getFilename() {
		return name;
	}
	@Override
	public int hashCode() {
		return name.hashCode()*13 + pageNr;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PageDescriptor)) return false;
		PageDescriptor other = (PageDescriptor) o;
		return other.pageNr == pageNr && other.name.equals(name);
	}


}
