/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.buffer;

public class BufferNotEmptyException extends Exception{

	public BufferNotEmptyException(String msg){
		super(msg);
	}
}
