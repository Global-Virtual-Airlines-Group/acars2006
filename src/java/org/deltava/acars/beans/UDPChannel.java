// Copyright 2011, 2013, 2014, 2016, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

import org.apache.logging.log4j.*;

import org.deltava.util.NetworkUtils;

/**
 * An object to handle UDP voice connections.
 * @author Luke
 * @version 11.0
 * @since 4.0
 */

public class UDPChannel extends ACARSChannel<byte[]> {

	private static transient final Logger log = LogManager.getLogger(UDPChannel.class);
	private transient DatagramChannel _dc;

	/**
	 * Creates a new UDP channel.
	 * @param id the connection ID
	 * @param dc the DatagramChannel to use
	 * @param addr the remote SocketAddress
	 * @throws IOException if an I/O error occurs
	 */
	UDPChannel(long id, DatagramChannel dc, InetSocketAddress addr) throws IOException {
		super(addr);
		_stats = new InternalConnectionStats("UDP-" + Long.toHexString(id));
		_dc = dc;
		_wSelector = Selector.open();

		// Register the selector
		_dc.register(_wSelector, SelectionKey.OP_WRITE);
		log.info("Enabled voice access from " + NetworkUtils.getSourceAddress(addr));
		updateLastActivity();
	}

	/**
	 * Updates the remote address.
	 * @param addr the remote SocketAddress
	 * @param dc the DatagramChannel
	 */
	public void setRemoteAddress(InetSocketAddress addr, DatagramChannel dc) {
		_remoteAddr = addr;
		if (dc != _dc)
			_dc = dc;
	}

	@Override
	public SelectableChannel getChannel() {
		return _dc;
	}

	/**
	 * Read method to increment counters.
	 * @param bytes the number of bytes read
	 */
	public void read(int bytes) {
		_stats.addBufferRead();
		_stats.addMessageIn();
		_stats.addBytesIn(bytes);
	}

	@Override
	public void write(byte[] data) {
		if (data == null) return;
		int writeCount = 1;
		try {
			// Keep writing until the packet is done
			int ofs = 0;
			while (ofs < data.length) {
				_oBuffer.clear();

				// Keep writing to the buffer
				while ((ofs < data.length) && (_oBuffer.remaining() > 0)) {
					_oBuffer.put(data[ofs]);
					ofs++;
				}

				// Flip the buffer and write if we can
				_oBuffer.flip();
				while (_oBuffer.hasRemaining()) {
					if (_wSelector.select(200) > 0) {
						_stats.addBytesOut(_dc.send(_oBuffer, _remoteAddr));
						_stats.addBufferWrite();
						_wSelector.selectedKeys().clear();
						if (writeCount > 4) writeCount--;
					} else {
						if (writeCount >= MAX_WRITE_ATTEMPTS) {
							_stats.addWriteError();
							_oBuffer.clear();
							if (_wSelector.select(200) > 0) {
								_stats.addBytesOut(_dc.send(_oBuffer, _remoteAddr));
								_stats.addBufferWrite();
								_wSelector.selectedKeys().clear();
							}

							throw new IOException("Write timeout for " + getRemoteAddr());
						}
					}
				}
			}

			_stats.addMessageOut();
		} catch (IOException ie) {
			log.warn("Error writing to voice channel for " + getRemoteAddr() + " - " + ie.getMessage());
		}
	}
}