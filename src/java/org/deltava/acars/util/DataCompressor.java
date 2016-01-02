// Copyright 2015, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.io.*;
import java.util.zip.*;

import org.deltava.acars.beans.Compression;

/**
 * A class to compress and decompress stream data.
 * @author Luke
 * @version 6.4
 * @since 6.4
 */

public class DataCompressor {
	
	private static final int MAGIC = 0x8B1FAC00;
	
	private final DataBuffer _buffer = new DataBuffer(256);
	
	private Compression _c = Compression.NONE;

	/**
	 * Utility method to check whether data is GZIP encoded.
	 * @param data the data to check
	 * @return TRUE if the GZIP magic header is present, otherwise FALSE
	 */
	public static boolean isCompressed(byte[] data) {
		if (data.length < 8) return false;
		int magic = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 16) + ((data[3] & 0xFF) << 24);
		return (MAGIC == magic);
	}
	
	/**
	 * Returns the Compression in use.
	 * @return the Compression type
	 */
	public Compression getCompression() {
		return _c;
	}
	
	/**
	 * Returns whether the buffer has a complete packet to decompress.
	 * @return TRUE if the buffer has at least one complete packet, otherwise FALSE
	 */
	public boolean hasCompletePacket() {
		if (_buffer.size() < 8) return false;
		int pktSize = _buffer.getAt(4);
		return (_buffer.size() >= (pktSize + 8));
	}
	
	/**
	 * Returns a packet from the buffer.
	 * @return the packet data, or null if not complete
	 */
	public byte[] getPacket() {
		if (_buffer.size() < 8) return null;
		int pktSize = _buffer.getAt(4);
		if (_buffer.size() < (pktSize + 8)) return null;
		return _buffer.getData(0, pktSize+8);
	}

	/**
	 * Returns whether the buffer is in use.
	 * @return TRUE if this has buffered data, otherwise FALSE
	 */
	public boolean hasBuffer() {
		return (_buffer.size() > 0);
	}
	
	/**
	 * Adds data to the buffer.
	 * @param data the data
	 */
	public void buffer(byte[] data) {
		_buffer.add(data);
	}
	
	/**
	 * Sets the compression to use.
	 * @param c the Compression type
	 */
	public void setCompression(Compression c) {
		_c = c;
	}
	
	/**
	 * Clears the buffer.
	 */
	public void reset() {
		_buffer.reset();
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
	 * Ccompresses data using the current algorithm.
	 * @param data the data to compress
	 * @return the compressed data 
	 */
	public byte[] compress(byte[] data) {
		return compress(data, _c);
	}

	/**
	 * Decompresses data.
	 * @param data the data to decompress
	 * @param c the Compression to use
	 * @return the decompressed data, or a zero length array in case of error 
	 */
	public static byte[] decompress(byte[] data, Compression c) {
		if (c == Compression.NONE) return data;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(512)) {
			try (PacketInputStream is = new PacketInputStream(new ByteArrayInputStream(data))) {
				int magic = is.readInt32(); // check header
				if (magic != MAGIC) return null;
				int compressedSize = is.readInt32(); // check size
				if (data.length != (compressedSize + 8)) return null;
				try (InputStream in = new GZIPInputStream(is, 1024)) {
					byte[] buffer = new byte[1024]; 
					int bytesRead = in.read(buffer);
					while (bytesRead > 0) {
						out.write(buffer, 0, bytesRead);
						bytesRead = in.read(buffer);
					}
				}
			}	
			
			return out.toByteArray();
		} catch (IOException ie) {
			return null;
		}
	}
	
	/**
	 * Compresses data.
	 * @param data the data to compress
	 * @param c the Compression to use
	 * @return the compressed data
	 */
	public static byte[] compress(byte[] data, Compression c) {
		if (c == Compression.NONE) return data;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(data.length)) {
			try (PacketOutputStream dos = new PacketOutputStream(out)) {
				dos.writeInt32(MAGIC);
				
				// Because we store the compressed length we need to compress the entire packet
				// and get its size
				try (ByteArrayOutputStream bufs = new ByteArrayOutputStream(data.length)) {
					try (InputStream is = new ByteArrayInputStream(data)) {
						try (OutputStream os = new GZIPOutputStream(bufs, 1024, true)) {
							byte[] buffer = new byte[1024]; int bytesRead = is.read(buffer);	
							while (bytesRead > 0) {
								os.write(buffer, 0, bytesRead);
								bytesRead = is.read(buffer);
							}
						}
					}

					// Write the compressed data
					byte[] pkt = bufs.toByteArray();
					dos.writeInt32(pkt.length);
					dos.write(pkt);
				}
			}
			
			return out.toByteArray();
		} catch (IOException ie) {
			return null;
		}
	}
}