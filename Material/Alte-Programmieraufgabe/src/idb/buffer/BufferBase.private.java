package idb.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class BufferBase implements Buffer{
	protected Map<Integer, Segment> segments;
	protected Map<Integer, Map<Integer, Page>> pagetable;
	protected int pagesize;
	protected BufferBase(int pagesize){
		segments = new HashMap<Integer, Segment>();
		pagetable = new HashMap<Integer, Map<Integer, Page>>();
		this.pagesize = pagesize;
	}

	public abstract ByteBuffer fix(int segno, int pageno) throws IOException, BufferFullException;
	public abstract void unfix(int segno, int pageno) throws IOException;
	public void setDirty(int segno, int pageno){
		pagetable.get(segno).get(pageno).setDirty();
	}
	public int getPagesize(){
		return pagesize;
	}
}
