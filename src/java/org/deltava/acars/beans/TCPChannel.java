// Copyright 2011, 2012, 2014, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.*;
import java.io.IOException;

import java.nio.*;
import java.nio.channels.*;

import org.deltava.acars.util.DataCompressor;
import org.deltava.acars.xml.ProtocolInfo;
import org.deltava.beans.acars.ConnectionStats;

import org.deltava.util.system.SystemData;

import org.apache.log4j.Logger;

/**
 * An object to handle TCP control connections.
 * @author Luke
 * @version 6.4
 * @since 4.0
 */

public class TCPChannel extends ACARSChannel<String> {
	
	private static transient final Logger log = Logger.getLogger(TCPChannel.class);
	
	private transient static final String MAGIC_RESET_CODE = "</!ACARSReset>";
	
	// Input buffers
	private transient final StringBuilder _msgBuffer = new StringBuilder();
	private transient final ByteBuffer _iBuffer = ByteBuffer.allocateDirect(SystemData.getInt("acars.buffer.nio"));
	
	private transient final SocketChannel _sc;
	
	private transient final DataCompressor _compress = new DataCompressor();
	
	/**
	 * Creates a new TCP Channel.
	 * @param id the channel ID
	 * @param sc the SocketChannel.
	 * @throws IOException if an I/O error occurs
	 */
	TCPChannel(long id, SocketChannel sc) throws IOException {
		super((InetSocketAddress) sc.getRemoteAddress());
		_stats = new InternalConnectionStats("TCP-" + Long.toHexString(id));
		_sc = sc;
		_wSelector = Selector.open();
		_sc.configureBlocking(false);
		_sc.register(_wSelector, SelectionKey.OP_WRITE);
		updateLastActivity();
	}
	
	/**
	 * Returns the daata compression type.
	 * @return a Compression type
	 */
	public Compression getCompression() {
		return _compress.getCompression();
	}
	
	/**
	 * Returns the Channel.
	 * @return a SocketChannel
	 */
	@Override
	public SocketChannel getChannel() {
		return _sc;
	}

	@Override
	public ConnectionStats getStatistics() {
		return _stats;
	}
	
	@Override
	public void close() {
		super.close();
		try {
			_sc.close();
		} catch (Exception e) {
			// empty
		}
	}
	
	/**
	 * Enables/disables data compression.
	 * @param c the Compression to use
	 */
	public void setCompression(Compression c) {
		_compress.setCompression(c);
	}
	
	/**
	 * Reads a message from the TCP channel.
	 * @return the message
	 * @throws IOException if an I/O error occurs
	 */
	String read() throws IOException {
		
		// Clear the buffer
		_iBuffer.clear();

		// Try and read from the channel until end of stream
		try {
			_sc.read(_iBuffer);
			_stats.addBufferRead();
		} catch (IOException ie) {
			throw new SocketException("Error reading channel - " + ie.getMessage());
		}
		
		// if we got nothing, return null
		if (_iBuffer.position() == 0)
			throw new ProtocolException("Connection Closed");

		// Set the limit on the buffer and return to the start, update counters
		_stats.addBytesIn(_iBuffer.flip().limit());
		updateLastActivity();
		
		// Decompress the data
		byte[] rawData = new byte[_iBuffer.remaining()];
		_iBuffer.get(rawData);
		
		// If the data is compressed and the buffer is empty, add it to the buffer.
		// If the data is compressed and the buffer is not empty, reset the buffer and add it.
		// If the buffer has data, add this to it and check whether the packet is complete.
		// If the buffer does not have data, treat as uncompressed
		boolean isCompressed = DataCompressor.isCompressed(rawData); boolean hasBufferedData = _compress.hasBuffer();
		if (isCompressed) {
			if (hasBufferedData && !_compress.hasCompletePacket()) _compress.reset();
			_compress.buffer(rawData);
		} else if (hasBufferedData)
			_compress.buffer(rawData);
		
		// If we have a complete packet in the buffer, decompress it
		if (_compress.hasCompletePacket()) {
			byte[] pkt = _compress.getPacket();
			while (pkt != null) {
				byte[] data = DataCompressor.decompress(pkt, Compression.GZIP);
				_stats.addBytesSaved(data.length - pkt.length);
				_msgBuffer.append(new String(data, UTF_8));
				pkt = _compress.getPacket();
			}
		} else if (!_compress.hasBuffer())
			_msgBuffer.append(new String(rawData, UTF_8));
		
		// Now, search the start of an XML message in the buffer; if there's no open discard the whole thing
		int sPos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_OPEN);
		if (sPos == -1) {
			if ((_msgBuffer.length() > 54) && (_msgBuffer.indexOf(ProtocolInfo.XML_HEADER) == -1)) {
				log.warn("Malformed message from " + getRemoteHost() + " - (" + _msgBuffer.length() + " bytes) " + _msgBuffer.toString());
				_msgBuffer.setLength(0);
			}

			return null;
		}
		
		// Get the end of the message - if there's an end element build a message and return it
		int ePos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_CLOSE, sPos);
		if (ePos == -1) return null;

		// Get the XML messages out of the buffer
		StringBuilder msgOut = new StringBuilder();
		while ((ePos > sPos) && (sPos > -1)) {
			msgOut.append(ProtocolInfo.XML_HEADER);
			ePos += ProtocolInfo.REQ_ELEMENT_CLOSE.length();
			_stats.addMessageIn();
			msgOut.append(_msgBuffer.substring(sPos, ePos));
			_msgBuffer.delete(0, ePos);
			sPos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_OPEN);
			ePos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_CLOSE, sPos);
		}
		
		if ((_msgBuffer.capacity() - _msgBuffer.length()) > 2048) {
			_msgBuffer.trimToSize();
			_msgBuffer.ensureCapacity(256);
		}

		return msgOut.toString();
	}

	/**
	 * Writes a message to the TCP channel.
	 * @param msg the message text
	 */
	@Override
	public void write(String msg) {
		if (msg == null)
			return;

		int writeCount = 1;
		try {
			byte[] msgData = msg.getBytes(UTF_8);
			byte[] msgBytes = _compress.compress(msgData);
			_stats.addBytesSaved(msgData.length - msgBytes.length);

			// Keep writing until the message is done
			int ofs = 0;
			while (ofs < msgBytes.length) {
				_oBuffer.clear();
				
				// Keep writing to the buffer
				while ((ofs < msgBytes.length) && (_oBuffer.remaining() > 0)) {
					_oBuffer.put(msgBytes[ofs]);
					ofs++;
				}

				// Flip the buffer and write if we can
				_oBuffer.flip();
				while (_oBuffer.hasRemaining()) {
					if (_wSelector.select(225) > 0) {
						_stats.addBytesOut(_sc.write(_oBuffer));
						_wSelector.selectedKeys().clear();
						_stats.addBufferWrite();
						if (writeCount > 4)
							writeCount--;
					} else if (!_sc.isConnected()) {
						close();
						return;
					} else {
						_stats.addBufferWrite();
						if (writeCount >= MAX_WRITE_ATTEMPTS) {
							_stats.addWriteError();
							_oBuffer.clear();
							_oBuffer.put(_compress.compress(MAGIC_RESET_CODE.getBytes(UTF_8)));
							_oBuffer.flip();
							if (_wSelector.select(300) > 0) {
								_stats.addBytesOut(_sc.write(_oBuffer));		
								_wSelector.selectedKeys().clear();
							}
							
							throw new IOException("Write timeout for " + getRemoteAddress());
						}
					}
				}
			}

			_stats.addMessageOut();
		} catch (ClosedSelectorException cse) {
			log.info("Cannot write to " + getRemoteAddress() + " - selector closed");
		} catch (AsynchronousCloseException ace) {
			log.warn("Connection for " + getRemoteAddress() + " closed during write");
		} catch (IOException ie) {
			log.warn("Error writing to channel for " + getRemoteAddress() + " - " + ie.getMessage());
		} catch (Exception e) {
			log.error("Error writing to socket " + getRemoteAddress() + " - " + e.getMessage(), e);
		}

		// Update statistics
		updateLastActivity();
	}
}