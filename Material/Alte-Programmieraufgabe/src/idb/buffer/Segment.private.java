package idb.buffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Segment{
	private RandomAccessFile datafile;
	private int pagesize;
	
	public Segment(int segno, int pagesize) throws FileNotFoundException{
		this.datafile = new RandomAccessFile("segment"+segno, "rw");
		this.pagesize = pagesize;
	}

	public Page loadPage(int pageno) throws IOException{
		//create page
		Page p = new Page(pagesize);
		//read page from file
		datafile.seek(pagesize*pageno);
		datafile.read(p.getData().array());
		return p;
	}

	public void writePage(int pageno, Page p) throws IOException{
		if(p.isDirty()){
			datafile.seek(pagesize*pageno);
			datafile.write(p.getData().array());
		}
	}
	public void close() throws IOException{
		datafile.close();	
	}
}
