/**
 * @file
 *
 * Index implementaion using hashing.
 * It allows for multiple usage of the same key.
 *
 * @author Christoph Merdes
 * @author Michael Zapf <michael.zapf@fau.de>
 *
 */

package idb.hashing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import idb.buffer.Buffer;
import idb.buffer.BufferFullException;
import idb.datatypes.DataObject;
import idb.datatypes.Key;

public class HashImpl<K extends Key, V extends DataObject> implements HashIdx<K, V> {
	// buffer interface used to access raw data (layer below)
	private Buffer buffer;

	// use one segment for first buckets,
	// and another distinct segment for overflow buckets
	private int primary_segno, overflow_segno;

	// factory used to create Key and Data (TID) instances
	private KeyValueFactory<K, V> factory;

	private int position, fill, size, capacity, oldCapacity, blockfactor, ofpointerCounter;

	// threshold when file is extended by one bucket (triggering split)
	private final double threshold;

	private final static int HEADER_NUMBER_ENTRIES = 0;
	private final static int HEADER_OFPOINTER = Integer.SIZE/Byte.SIZE;
	private final static int HEADER_SIZE = 2*Integer.SIZE/Byte.SIZE;

	// magic numbers for "This is last overflow bucket" and "Undefined overflow bucket."
	private final static int OVERFLOW_END = -1;
	private final static int OVERFLOW_UNDEF = 0;


	private K keyContainer;		// temporary buffer for key storage
	private V valueContainer;	// temporary buffer for value storage

	/**
	 * Constructor.
	 * @author Christoph Merdes
	 *
	 * @param buffer        Buffer object where the data is stored.
	 * @param primary_segno ID of buffer segment, in which the "normal" buckets will be stored.
	 * @param overflow_segno ID of buffer segment, in which the overflow buckets will be stored.
	 * @param factory       Object of type KeyValueFactory that is used to allocate memory space for keys and data items.
	 * @param initial_capacity Initial number of used buckets. (Important for correct hash function.)
	 */
	public HashImpl(Buffer buffer, int primary_segno, int overflow_segno, KeyValueFactory<K, V> factory, int initialCapacity) {
		this.buffer = buffer;
		this.primary_segno = primary_segno;
		this.overflow_segno = overflow_segno;
		this.factory  = factory;
		readOverflowHeader(initialCapacity);
		this.threshold = 0.8;

		// KeyValueFactory allocates memory for temporary key and value storage
		this.keyContainer = factory.createKey();
		this.valueContainer = factory.createDataObject();

		this.blockfactor =
			(buffer.getPagesize() - HEADER_SIZE) /
			(valueContainer.size() + keyContainer.size());
	}

	/**
	 * Implements the hash function (see lecture for mathematical definition)
	 * @author Christoph Merdes
	 * @author You!
	 *
	 * @param K     Object of type Key to compute hash function on
	 */
	private int computeHash(K key) {
		/* LÖSUNG! */
		int hashVal = Math.abs(key.hashCode() % oldCapacity);
		if (hashVal < position) hashVal = Math.abs(key.hashCode() % capacity);

		return hashVal;

		/* VORGABE!

		   // TODO Hashfunktion implementieren,
		   //		siehe auch: key.hashCode()

		   return 0;

		 */
	}

	/**
	 * Returns a list of all data items with the key K.
	 *
	 * @author	Christoph Merdes
	 * @author	You!
	 *
	 * @param	key The key that should be looked for.
	 */
	public List<V> get(K key) {
		int hashVal = computeHash(key);
		List<V> values = new LinkedList<V>();
		try {
			getBucketContent(key, primary_segno, hashVal, values);
		} catch (IOException e) {
			// Print out debugging information
			e.printStackTrace();
		} catch (BufferFullException e) {
			// Print out debugging information
			e.printStackTrace();
		}
		return values;

		/* VORGABE
		//TODO Key hashen, alle Values mit dem Key zurückgeben.
		return null;
		*/
	}

	private void getBucketContent(K key, int segno, int pageno, List<V> values) throws IOException, BufferFullException {
		// Load needed page into buffer.
		ByteBuffer bucket = buffer.fix(segno, pageno);

		// Read header
		int numOfEntries = bucket.getInt(HEADER_NUMBER_ENTRIES);
		int ofpointer = bucket.getInt(HEADER_OFPOINTER);
		int index = HEADER_SIZE;

		// Read bucket's entries and compare keys
		for (int i = 0; i < numOfEntries; ++i) {
			keyContainer.read(index, bucket);
			valueContainer.read(index + key.size(), bucket);
			if (((K)keyContainer).equals(key)) {
				values.add(valueContainer);
				valueContainer = factory.createDataObject();
			}
			index += (keyContainer.size() + valueContainer.size());
		}
		buffer.unfix(segno, pageno);

		// recurse to next overflow bucket
		if (ofpointer != OVERFLOW_END && ofpointer != OVERFLOW_UNDEF) {
			getBucketContent(key, overflow_segno, ofpointer, values);
		}
	}

	/**
	 * Put a data item value with key key to the index.
	 *
	 * @author	Christoph Merdes
	 * @author	You!
	 *
	 * @param	key		The key that should be inserted.
	 * @param	value	The data item that should be inserted.
	 */
	public void put(K key, V value) {
		int hashVal = computeHash(key);
		try {
			putToBucket(key, value, primary_segno, hashVal);
		} catch (IOException e) {
			// Print out debugging information
			e.printStackTrace();
		} catch (BufferFullException e) {
			// Print out debugging information
			e.printStackTrace();
		}

		/* VORGABE
		//TODO Key hashen und Key-Value-Paar in den Index einfügen
		*/

	}

	private void putToBucket(K key, V value, int segno, int pageno) throws IOException, BufferFullException {
		// Load needed page into buffer.
		ByteBuffer bucket = buffer.fix(segno, pageno);

		// Get current
		int numOfEntries = bucket.getInt(HEADER_NUMBER_ENTRIES);
		int ofpointer = bucket.getInt(HEADER_OFPOINTER);

		if (numOfEntries == blockfactor) {
			// Bucket is full, we have to use overflow bucket

			if (ofpointer == OVERFLOW_UNDEF || ofpointer == OVERFLOW_END) {
				// No overflow bucket created yet, so create one

				ofpointer = createNextOverflowBucket();
				bucket.putInt(HEADER_OFPOINTER, ofpointer);
				buffer.setDirty(segno, pageno);
			}
			buffer.unfix(segno, pageno);
			putToBucket(key, value, overflow_segno, ofpointer);
		} else if (numOfEntries < blockfactor) {
			int index = (key.size() + value.size())*numOfEntries + HEADER_SIZE;
			key.write(index, bucket);
			value.write(index + key.size(), bucket);
			bucket.putInt(HEADER_NUMBER_ENTRIES, numOfEntries + 1);
			buffer.setDirty(segno,pageno);
			buffer.unfix(segno, pageno);
			fill++;
			//System.out.println("fill: "+fill+"; size: "+size+"; fill/size: "+(fill/((double)size)));
			if (fill/((double)size) > threshold) {
				split();
			}
		} else {
			System.err.println("HashImpl: Internal data structures corrupted!");
		}
	}

	/**
	 * Creates a new overflow bucket and adjusts the pointers/IDs accordingly
	 *
	 * @author	Christoph Merdes
	 */
	private int createNextOverflowBucket() throws IOException, BufferFullException {
		int ofpointer = ++ofpointerCounter;
		ByteBuffer bucket = buffer.fix(overflow_segno, ofpointer);
		bucket.putInt(HEADER_NUMBER_ENTRIES, 0);
		bucket.putInt(HEADER_OFPOINTER, OVERFLOW_END);
		buffer.setDirty(overflow_segno, ofpointer);
		buffer.unfix(overflow_segno, ofpointer);
		return ofpointer;
	}

	/**
	 * Split wherever necessary (buckets) and double capacity / update hash function.
	 *
	 * @author	Christoph Merdes
	 * @author	You!
	 */
	private void split() throws IOException, BufferFullException {
		System.out.println("Splitting.");

		List<V> values = new LinkedList<V>();
		List<K> keys = new LinkedList<K>();

		// get all entries from the bucket to split
		gatherAndClear(primary_segno, position++, keys, values);
		size += blockfactor;

		// re-hash and put all entries back
		for (int i = 0; i < keys.size(); ++i) {
			put(keys.get(i), values.get(i));
		}

		// update hashfunction if necessary
		if (position == capacity) {
			// all splits are done for the actual functions, expand
			position = 0;
			oldCapacity = capacity;
			capacity *= 2;
		}

		/* VORGABE:
		 // TODO Bucket hinzufügen
		 // TODO Buckets splitten
		 */
	}

	private void gatherAndClear(int segno, int pageno, List<K> keys, List<V> values) throws IOException, BufferFullException {
		// Load needed page into buffer.
		ByteBuffer bucket = buffer.fix(segno, pageno);

		// Read header
		int numOfEntries = bucket.getInt(HEADER_NUMBER_ENTRIES);
		int ofpointer = bucket.getInt(HEADER_OFPOINTER);

		// Read entries
		int index = HEADER_SIZE;
		for (int i = 0; i < numOfEntries; ++i) {
			keyContainer.read(index, bucket);
			valueContainer.read(index+keyContainer.size(), bucket);
			values.add(valueContainer);
			valueContainer = factory.createDataObject();
			keys.add(keyContainer);
			keyContainer = factory.createKey();

			index += (keyContainer.size()+valueContainer.size());
		}
		fill -= numOfEntries;

		// clear bucket (set size = 0, no overflow buckets)
		bucket.putInt(HEADER_NUMBER_ENTRIES, 0);
		bucket.putInt(HEADER_OFPOINTER, OVERFLOW_END);

		// free bucket
		buffer.setDirty(segno, pageno);
		buffer.unfix(segno, pageno);

		// handle overflow if necessary
		if (ofpointer != OVERFLOW_END && ofpointer != OVERFLOW_UNDEF) {
			gatherAndClear(overflow_segno, ofpointer, keys, values);
		}
	}

	/**
	 * Reads the current state of the overflow segment
	 * and initializes the hash function accordingly.
	 * (To be used only on initialization / in constructor.)
	 * @author Christoph Merdes
	 *
	 * @param initialCapacity   Capacity the hash function is initialized with.
	 */
	private void readOverflowHeader(int initialCapacity) {
		try {
			ByteBuffer header = buffer.fix(overflow_segno, 0);
			int stepsize = Integer.SIZE/Byte.SIZE;
			int index = 0;
			this.position = header.getInt(index);
			this.fill	  = header.getInt(index+stepsize);
			this.ofpointerCounter = header.getInt(index+stepsize*2);
			if (fill > 0) {
				this.size = header.getInt(index+stepsize*3);
				this.capacity = header.getInt(index+stepsize*4);
				this.oldCapacity = header.getInt(index+stepsize*5);
			} else {
				this.size = initialCapacity;
				this.capacity = initialCapacity;
				this.oldCapacity = initialCapacity;
			}
			buffer.setDirty(overflow_segno, 0);
			buffer.unfix(overflow_segno, 0);
			//printStats("Read Header");
			return;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BufferFullException e) {
			e.printStackTrace();
		}

		// loading the header failed, start from scratch. This could not work totally like expected
		System.err.println("Loading overflow header failed. Unexpected behaviour may occur.");
		this.position		= 0;
		this.fill			= 0;
		this.size			= initialCapacity;
		this.capacity		= initialCapacity;
		this.oldCapacity	= initialCapacity;
		this.ofpointerCounter = 0;

	}

	/**
	 * Writes the current state of the overflow segment to metadata.
	 * (To be used only on destruction / disposal / close().)
	 * @author Christoph Merdes
	 */
	private void writeOverflowHeader() throws IOException {
		try {
			ByteBuffer header = buffer.fix(overflow_segno, 0);
			int stepsize = Integer.SIZE/Byte.SIZE;
			int index = 0;
			header.putInt(index, position);
			header.putInt(index + stepsize, fill);
			header.putInt(index + stepsize*2, ofpointerCounter);
			header.putInt(index + stepsize*3, size);
			header.putInt(index + stepsize*4, capacity);
			header.putInt(index + stepsize*5, oldCapacity);

			buffer.setDirty(overflow_segno, 0);
			buffer.unfix(overflow_segno, 0);
		} catch (BufferFullException e) {
			e.printStackTrace();
			System.err.println("Writing the header failed.");
			System.err.println("Writing after reload will corrupt the file!");
		}
	}



	/**
	 * Write back overflow header to be able to restore information about
	 * overflow buckets on next startup.
	 *
	 * @author	Christoph Merdes
	 */
	public void close() throws IOException {
		writeOverflowHeader();
	}

	/**
	 * Can be used for debugging.
	 *
	 * @author	Christoph Merdes
	 */
	private void printStats(String arg) {
		System.err.println(arg);
		System.err.println("Old Capacity "+oldCapacity);
		System.err.println("New Capacity "+capacity);
		System.err.println("Last OFPointer "+ofpointerCounter);
		System.err.println("Size "+size);
		System.err.println("Fill "+fill);
		System.err.println("Position "+position);
		System.err.println();
	}
}
