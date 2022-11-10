/**
 * @file
 * @author Tobias Heineken <tobias.heineken@fau.de>
 */
package idb.module;

import idb.datatypes.NamedCombinedRecord;

public interface Module{
	/**
	 * Retrieves one record for this module.
	 * @return null if no futher record can be found
	 */
	public NamedCombinedRecord pull();
	/**
	 * Resets this module to the start.
	 * This methode is needed to be able to implement a cross-product without storing every entry in Memory.
	 */
	public void reset();
	/**
	 * Closes this module. This is beeing used to free ressources (like buffer.unfix) before destruction.
	 * After closing, it is undefined behaviour to call reset or pull.
	 */
	public void close();
}
