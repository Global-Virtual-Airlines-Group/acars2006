// Copyright 2010, 2011, 2012, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.mvs;

import java.io.*;

/**
 * A class to handle encoding for MVS packets.
 * @author Luke
 * @version 5.2
 * @since 4.0
 */

class PacketOutputStream extends DataOutputStream {

	/**
	 * Creates the stream.
	 * @param os the destination OutputStream
	 */
	PacketOutputStream(OutputStream os) {
		super(os);
	}

	/**
	 * Writes a null-terminated UTF-8 string to the stream.
	 * @param s the String
	 * @throws IOException if an I/O error occurs
	 */
	public void write(String s) throws IOException {
		write(s.getBytes("UTF-8"));
		write(0);
	}

	/**
	 * Writes a 32-bit signed integer to the stream.
	 * @param i the int
	 * @throws IOException if an I/O error occurs
	 */
	public void writeInt32(int i) throws IOException {
		write(i & 0xFF);
		write((i >> 8) & 0xFF);
		write((i >> 16) & 0xFF);
		write((i >> 24) & 0xFF);
	}

	/**
	 * Writes a 64-bit signed integer to the stream.
	 * @param l the long
	 * @throws IOException if an I/O error occurs
	 */
	public void writeInt64(long l) throws IOException {
		byte[] buffer = new byte[8];
		for (int x = 0; x < 8; x++)
			buffer[x] = (byte) ((l >> (x * 8)) & 0xFF);

		write(buffer, 0, 8);
	}

	/**
	 * Writes a 64-bit floating point number to the stream. 
	 * @param d the double
	 * @throws IOException if an I/O error occurs
	 */
	public void writeDouble64(double d) throws IOException {
		writeInt64(Double.doubleToLongBits(d));
	}
}