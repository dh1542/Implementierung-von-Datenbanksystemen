/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.record;

public class DeletedRecordException extends Exception {
	public DeletedRecordException(String mess) {
		super(mess);
	}
}
