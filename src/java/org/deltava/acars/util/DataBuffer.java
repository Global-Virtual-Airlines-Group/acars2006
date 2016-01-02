// Copyright 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.io.*;
import java.util.Arrays;

/**
 * A seekable, unsynchronized data buffer.
 * @author Luke
 * @version 6.4
 * @since 6.4
 */

class DataBuffer extends ByteArrayOutputStream {

	/**
	 * Initializes the buffer.
	 * @param size the initial size in bytes
	 */
	public DataBuffer(int size) {
		super(size);
	}
	
	/**
	 * Adds data to the buffer. This swallows the IOExceptions declared by the superclass.
	 * @param data the data to add
	 */
	public void add(byte[] data) {
		try {
			super.write(data);
		} catch (IOException ie) { /* empty */ }
	}
	
	/**
	 * Returns the integer at a particular buffer offset.
	 * @param ofs the offset
	 * @return the integer
	 */
	public int getAt(int ofs) {
		return (buf[ofs] & 0xFF) + ((buf[ofs+1] & 0xFF) << 8) + ((buf[ofs+2] & 0xFF) << 16) + ((buf[ofs+3] & 0xFF) << 24);
	}
	
	/**
	 * Searches for a particular 32-bit integer in the buffer.
	 * @param i the integer
	 * @return the offset in the array, or -1 if not found
	 */
	public int indexOf(int i) {
		return indexOf(i, 0);
	}
	
	/**
	 * Searches for a particular 32-bit integer in the buffer.
	 * @param i the integer
	 * @param start the starting offset in the buffer
	 * @return the offset in the array, or -1 if not found
	 */
	public int indexOf(int i, int start) {
		if (buf.length < (start+4)) return -1;
		int v = getAt(start); int ofs = 4;
		while ((v != i) && (ofs < buf.length)) {
			v = (v << 8) + buf[ofs];
			ofs++;
		}
			
		return (v == i) ? (ofs - 4) : -1;
	}
	
	/**
	 * Returns data from the buffer.
	 * @param ofs the starting offset
	 * @param length the amount of data to retrieve
	 * @return the data to fetch
	 */
	public byte[] getData(int ofs, int length) {
		int endOfs = ofs+length;
		if (endOfs > count) throw new IndexOutOfBoundsException("Requested " + ofs + " to " + (endOfs-1) + ",size=" + count);
		byte[] data = Arrays.copyOfRange(buf, ofs, endOfs);
		if (endOfs < count)
			System.arraycopy(buf, endOfs, buf, 0, (count - endOfs));
		
		count -= length;
		return data;
	}
}