package idb.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

public class SimpleBuffer extends BufferBase implements Buffer{
	private int fill;
	public SimpleBuffer(int pagesize){
		super(pagesize);
		this.fill = 0;
	}
	public ByteBuffer fix(int segno, int pageno) throws IOException{
		Segment s = segments.get(segno);
		if(s == null){
			s = new Segment(segno, pagesize);
			segments.put(segno, s);
			pagetable.put(segno, new HashMap<Integer, Page>());
		}
		Page p = pagetable.get(segno).get(pageno);
		if(p == null){
			p = s.loadPage(pageno);
			pagetable.get(segno).put(pageno, p);
			fill++;
		}
		p.fix();
		return p.getData();
	}
	public void unfix(int segno, int pageno) throws IOException{
		Page p = pagetable.get(segno).get(pageno);
		if(p != null){
			p.unfix();
			if(p.getNumFix() == 0){
				segments.get(segno).writePage(pageno, p);
				pagetable.get(segno).remove(pageno);
				fill--;
			}
		}

	}
	public void flush() throws IOException{
		//this buffer does not really buffer pages
		return;
	}
	public void close() throws BufferNotEmptyException, IOException{
		if(fill != 0){
			throw new BufferNotEmptyException(fill+" pages remaining");	
		}
		for(Entry<Integer, Segment> e: segments.entrySet()){
			e.getValue().close();	
		}	
		segments.clear();
	}
}
