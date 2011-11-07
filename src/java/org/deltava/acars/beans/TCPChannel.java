// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.IOException;
import java.net.*;

import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

import org.deltava.acars.xml.ProtocolInfo;
import org.deltava.beans.acars.ConnectionStats;

import org.deltava.util.system.SystemData;

import org.apache.log4j.Logger;

/**
 * An object to handle TCP control connections.
 * @author Luke
 * @version 4.1
 * @since 4.0
 */

public class TCPChannel extends ACARSChannel<String> {
	
	private static transient final Logger log = Logger.getLogger(TCPChannel.class);
	
	private transient static final String MAGIC_RESET_CODE = "</!ACARSReset>";
	
	// Byte byffer decoder and character set
	private transient final Charset cs = Charset.forName("UTF-8"); 
	private transient final CharsetDecoder decoder = cs.newDecoder();
	
	// Input buffers
	private transient final StringBuilder _msgBuffer = new StringBuilder();
	private transient final ByteBuffer _iBuffer = ByteBuffer.allocateDirect(SystemData.getInt("acars.buffer.nio"));
	
	private transient final SocketChannel _sc;
	
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
	 * Returns the Channel.
	 * @return a SocketChannel
	 */
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
		
		// Reset the decoder and decode into a char buffer - strip out ping nulls
		try {
			CharBuffer cBuffer = decoder.decode(_iBuffer);
			for (int x = cBuffer.position(); x < cBuffer.limit(); x++) {
				char c = cBuffer.charAt(x);
				if (c > 1)
					_msgBuffer.append(c);
			}
		} catch (CharacterCodingException cce) {
			// empty
		}
		
		// Now, search the start of an XML message in the buffer; if there's no open discard the whole thing
		int sPos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_OPEN);
		if (sPos == -1) {
			if ((_msgBuffer.length() > 54) && (_msgBuffer.indexOf(ProtocolInfo.XML_HEADER) == -1)) {
				log.warn("Malformed message from " + getRemoteHost() + " - (" + _msgBuffer.length() + " bytes) " + _msgBuffer.toString());
				_msgBuffer.setLength(0);
			}

			// Return nothing
			return null;
		}
		
		// Get the end of the message - if there's an end element build a message and return it
		int ePos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_CLOSE, sPos);
		if (ePos == -1)
			return null;

		ePos += ProtocolInfo.REQ_ELEMENT_CLOSE.length();

		// Get the XML message out of the buffer
		StringBuilder msgOut = new StringBuilder(ProtocolInfo.XML_HEADER);
		msgOut.append(_msgBuffer.substring(sPos, ePos));

		// Clear the message out of the buffer
		_msgBuffer.delete(0, ePos);

		// Return the buffer
		_stats.addMessageIn();
		return msgOut.toString();
	}

	/**
	 * Writes a message to the TCP channel.
	 */
	@Override
	public void write(String msg) {
		if (msg == null)
			return;

		int writeCount = 1;
		try {
			byte[] msgBytes = msg.getBytes(cs);

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
					if (_wSelector.select(250) > 0) {
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
							_oBuffer.put(MAGIC_RESET_CODE.getBytes(cs));
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