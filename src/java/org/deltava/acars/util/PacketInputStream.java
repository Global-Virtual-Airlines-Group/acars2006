// Copyright 2010, 2011, 2012, 2014, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.io.*;

/**
 * A class to handle decoding for data packets.
 * @author Luke
 * @version 6.4
 * @since 4.0
 */

public class PacketInputStream extends DataInputStream {

	/**
	 * Creates the stream.
	 * @param is the source InputStream
	 */
	public PacketInputStream(InputStream is) {
		super(is);
	}
	
	/**
	 * Reads a null-terminated UTF-8 string from the stream.
	 * @return the String
	 * @throws IOException if an I/O error occurs
	 */
	public String readUTF8() throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream(64)) {
			int b = read();
			while (b > 0) {
				os.write(b);
				b = in.read();
			}

			return new String(os.toByteArray(), "UTF-8");
		}
	}

	/**
	 * Reads a 32-bit signed integer from the stream.
	 * @return an int
	 * @throws IOException if an I/O error occurs
	 */
	public int readInt32() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
		return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
	}

	/**
	 * Reads a 64-bit signed integer from the stream.
	 * @return a long
	 * @throws IOException if an I/O error occurs
	 */
	public long readInt64() throws IOException {
		byte readBuffer[] = new byte[8];
		readFully(readBuffer, 0, 8);
		return (((long) readBuffer[7] << 56) + ((long) (readBuffer[6] & 255) << 48) + ((long) (readBuffer[5] & 255) << 40)
				+ ((long) (readBuffer[4] & 255) << 32) + ((long) (readBuffer[3] & 255) << 24) + ((readBuffer[2] & 255) << 16)
				+ ((readBuffer[1] & 255) << 8) + (readBuffer[0] & 255));
	}

	/**
	 * Reads a 64-bit floating point number from the stream.
	 * @return a double
	 * @throws IOException if an I/O error occurs
	 */
	public double readDouble64() throws IOException {
		return Double.longBitsToDouble(readInt64());
	}
}