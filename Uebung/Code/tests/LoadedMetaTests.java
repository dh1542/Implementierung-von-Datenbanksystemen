import idb.construct.Util;

import idb.meta.Metadata;
import idb.meta.FileCache;

public class LoadedMetaTests extends MetaTests {
	@Override
	protected void createIDB09Relation() {
		super.createIDB09Relation();
		fc.close();
		fc = new FileCache(dbbuf);
		dbbuf = Util.generateClockBuffer(4096, 250);
		m = Util.reloadMetadata(dbbuf, fc, p);
	}
}
