import java.util.function.Function;
import idb.buffer.DBBuffer;

import idb.construct.Util;

public class SimpleBufferTest extends AbstractBufferTest {
	@Override
	protected Function<Integer, ? extends DBBuffer> makeBufferGenerator(int numberOfPages) {
		return Util::generateSimpleBuffer;
	}
}
