package idb.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
/**
 * A database buffer implementation using the LRU strategy for choosing
 * the page to be replaced.
 * @author Christoph Merdes
 */
public class LRUBuffer extends BufferBase implements Buffer{
	private LinkedList<PageDesc> unfixed;
	private int capacity;
	private int fill;

	public LRUBuffer(int pagesize, int capacity){
		super(pagesize);
		this.capacity = capacity;
		this.fill = 0;
		this.unfixed = new LinkedList<PageDesc>();
	}
	
	public ByteBuffer fix(int segno, int pageno) throws IOException, BufferFullException{
		//System.out.println("fix page "+pageno +" in segment "+segno);
		//check if there is a free buffer frame
		if(capacity == fill){
			throw new BufferFullException("No space left to load the page.");	
		}
		//load the segment if necessary
		Segment s = segments.get(segno);
		if(s == null){
			s = new Segment(segno, pagesize);
			segments.put(segno, s);
			pagetable.put(segno, new HashMap<Integer, Page>());
		}
		//write page back if buffer is full, yet unfixed pages exist
		if((fill+unfixed.size()) == capacity){
				PageDesc tmp = unfixed.poll();
				if(tmp.page.isDirty()){
					Segment seg = segments.get(tmp.segno);
					seg.writePage(tmp.pageno, tmp.page);
					//System.err.println("wrote back page "+tmp.pageno+" segno "+tmp.segno);
				}
		}
		//load the page if necessary
		Page p = pagetable.get(segno).get(pageno);
		if(p == null){
			PageDesc pd = removeFromUnfixed(segno, pageno);
			if(pd == null){
				p = s.loadPage(pageno);
				//System.err.println("loaded page "+pageno+" from segment "+segno);
			} else {
				p = pd.page;
			}
			//System.out.println("argument: "+pageno+" "+segno);
			//if (pd != null) System.out.println("pd: "+pd.pageno+" "+pd.segno);
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
				pagetable.get(segno).remove(pageno);
				unfixed.add(new PageDesc(segno, pageno,p));
				fill--;
			}
		} else {
			System.err.println("This should not happen here!");
		}

	}
	//write buffered, yet unfixed pages back to the harddrive
	public void flush() throws IOException{
		while(!unfixed.isEmpty()){
			PageDesc tmp = unfixed.poll();
			Segment s = segments.get(tmp.segno);
			s.writePage(tmp.pageno, tmp.page);
		}
	}

	public void close() throws IOException, BufferNotEmptyException{
		if(fill != 0){
			throw new BufferNotEmptyException(fill+" pages remaining");
		}
		this.flush();
		for(Entry<Integer, Segment> e: segments.entrySet()){
			e.getValue().close();	
		}
		segments.clear();
	}
	private PageDesc removeFromUnfixed(int segno, int pageno){
		Iterator<PageDesc> iter = unfixed.iterator();
		while(iter.hasNext()){
			PageDesc pd = iter.next();
			if(pd.pageno == pageno && pd.segno == segno){
				iter.remove();
				return pd;
			}
		}
		return null;
	}
	class PageDesc{
		PageDesc(int segno, int pageno, Page p){
			this.pageno = pageno;
			this.page = p;
			this.segno = segno;
		}
		Page page;
		int pageno;
		int segno;
	}
}
