// Copyright 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.util.zip.*;

/**
 * A class to compress and decompress stream data.
 * @author Luke
 * @version 6.4
 * @since 6.4
 */

public class DataCompressor {
	
	private final ByteArrayOutputStream _inByteS = new ByteArrayOutputStream(512);
	private final ByteArrayOutputStream _outByteS = new ByteArrayOutputStream(8192);
	
	private Compression _c = Compression.NONE;

	/**
	 * Utility method to check whether data is GZIP encoded.
	 * @param data the data to check
	 * @return TRUE if the GZIP magic header is present, otherwise FALSE
	 */
	public static boolean isGZIP(byte[] data) {
		return ((data.length > 1) && (data[0] == 0x1F) && (data[1] == (byte)0x8B));
	}
	
	/**
	 * Returns the Compression in use.
	 * @return the Compression type
	 */
	public Compression getCompression() {
		return _c;
	}
	
	/**
	 * Sets the compression to use.
	 * @param c the Compression type
	 */
	public void setCompression(Compression c) {
		_c = c;
	}
	
	/**
	 * Decompresses data using the current algorithm.
	 * @param data the data to decompress
	 * @return the decompressed data, or a zero length array in case of error 
	 */
	public byte[] decompress(byte[] data) {
		return decompress(data, _c);
	}

	/**
	 * Decompresses data.
	 * @param data the data to decompress
	 * @param c the Compression to use
	 * @return the decompressed data, or a zero length array in case of error 
	 */
	public byte[] decompress(byte[] data, Compression c) {
		if (c == Compression.NONE) return data;
		_inByteS.reset();
		try (InputStream is = new ByteArrayInputStream(data)) {
			try (InputStream in = new GZIPInputStream(is, 1024)) {
				byte[] buffer = new byte[1024]; 
				int bytesRead = in.read(buffer);
				while (bytesRead > 0) {
					_inByteS.write(buffer, 0, bytesRead);
					bytesRead = in.read(buffer);
				}
			}
			
			return _inByteS.toByteArray();
		} catch (IOException ie) {
			return new byte[0];
		}
	}
	
	/**
	 * Compresses data.
	 * @param data the data to compress
	 * @return the compressed dataa
	 */
	public byte[] compress(byte[] data) {
		if (_c == Compression.NONE) return data;
		_outByteS.reset();
		try (InputStream is = new ByteArrayInputStream(data)) {
			try (OutputStream os = new GZIPOutputStream(_outByteS, 1024, true)) {
				byte[] buffer = new byte[1024]; int bytesRead = is.read(buffer);	
				while (bytesRead > 0) {
					os.write(buffer, 0, bytesRead);
					bytesRead = is.read(buffer);
				}
			}
			
			return _outByteS.toByteArray();
		} catch (IOException ie) {
			return null;
		}
	}
}