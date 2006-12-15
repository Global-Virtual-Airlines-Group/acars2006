// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.system.UserData;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.ProtocolInfo;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server connection.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnection implements Serializable, Comparable, ViewEntry {

	protected static final Logger log = Logger.getLogger(ACARSConnection.class);
	private static final int MAX_WRITE_ATTEMPTS = 12;

	// Byte byffer decoder and character set
	private final CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();

	private SocketChannel _channel;
	private Selector _wSelector;
	
	private InetAddress _remoteAddr;
	private String _remoteHost;
	private int _protocolVersion = 1;
	private int _clientVersion;
	private boolean _isDispatch;

	// Input/output network buffers
	private final ByteBuffer _iBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.nio"));
	private final ByteBuffer _oBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.nio"));

	// The the actual buffers for messages
	private final StringBuilder _msgBuffer = new StringBuilder();
	protected final List<String> _msgOutBuffer = Collections.synchronizedList(new ArrayList<String>());

	// Connection information
	private long _id;
	private Pilot _userInfo;
	private UserData _userData;
	private InfoMessage _fInfo;
	private PositionMessage _pInfo;
	private boolean _isUserBusy;
	private boolean _isUserHidden;

	// Activity monitors
	private long _startTime;
	private long _lastActivityTime;
	
	// The write lock
	private final ReadWriteLock _rwLock = new ReentrantReadWriteLock(true);
	private final Lock _wLock = _rwLock.writeLock();

	// Statistics
	private long _bytesIn;
	private long _bytesOut;
	private long _msgsIn;
	private long _msgsOut;
	private long _bufferWrites;

	public ACARSConnection(long cid, SocketChannel sc) {

		// Init the superclass and start time
		super();
		_startTime = System.currentTimeMillis();
		_id = cid;

		// Get IP Address information
		_remoteAddr = sc.socket().getInetAddress();

		// Turn off blocking and get a write selector
		try {
			sc.configureBlocking(false);
			_wSelector = Selector.open();
			sc.register(_wSelector, SelectionKey.OP_WRITE);
		} catch (IOException ie) {
			// Log our error and shut the connection
			log.error("Cannot set non-blocking I/O from " + _remoteAddr.getHostAddress());
			try {
				sc.close();
			} catch (Exception e) {
			}
		} finally {
			_channel = sc;
		}
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		try {
			_wSelector.close();
			_channel.close();
		} catch (Exception e) {
		}
	}

	public boolean equals(SocketChannel ch) {
		return _channel.equals(ch);
	}

	public boolean equals(Object o2) {

		// Check to make sure we are the same type
		if (!(o2 instanceof ACARSConnection))
			return false;

		// Do the cast and compare the connections
		ACARSConnection c2 = (ACARSConnection) o2;
		return equals(c2.getID());
	}

	public boolean equals(long cid) {
		return (_id == cid);
	}

	public boolean equals(String pid) {
		return _userInfo.getPilotCode().equals(pid);
	}

	public long getBytesIn() {
		return _bytesIn;
	}

	public long getBytesOut() {
		return _bytesOut;
	}

	SocketChannel getChannel() {
		return _channel;
	}

	public Socket getSocket() {
		return _channel.socket();
	}

	public int getFlightID() {
		return (_fInfo == null) ? 0 : _fInfo.getFlightID();
	}

	public long getID() {
		return _id;
	}

	public InfoMessage getFlightInfo() {
		return _fInfo;
	}

	public PositionMessage getPosition() {
		return _pInfo;
	}
	
	public boolean getUserBusy() {
		return _isUserBusy;
	}
	
	public boolean getUserHidden() {
		return _isUserHidden;
	}

	public long getLastActivity() {
		return _lastActivityTime;
	}

	public long getMsgsIn() {
		return _msgsIn;
	}

	public long getMsgsOut() {
		return _msgsOut;
	}
	
	public long getBufferWrites() {
		return _bufferWrites;
	}

	public int getProtocolVersion() {
		return _protocolVersion;
	}
	
	public boolean getIsDispatch() {
		return _isDispatch;
	}

	public int getClientVersion() {
		return _clientVersion;
	}

	public long getStartTime() {
		return _startTime;
	}

	public String getRemoteAddr() {
		return _remoteAddr.getHostAddress();
	}

	public String getRemoteHost() {
		return (_remoteHost == null) ? _remoteAddr.getHostName() : _remoteHost;
	}

	public Pilot getUser() {
		return _userInfo;
	}

	public UserData getUserData() {
		return _userData;
	}

	public String getUserID() {
		return isAuthenticated() ? _userInfo.getPilotCode() : getRemoteAddr();
	}

	public boolean isAuthenticated() {
		return (_userInfo != null);
	}

	public boolean isConnected() {
		return _channel.isConnected();
	}

	public void setFlightInfo(InfoMessage msg) {
		_fInfo = msg;
	}

	public void setPosition(PositionMessage msg) {
		_pInfo = msg;
	}

	public void setProtocolVersion(int pv) {
		if ((pv > 0) && (pv <= Message.PROTOCOL_VERSION))
			_protocolVersion = pv;
	}

	public void setClientVersion(int ver) {
		if (ver > 0)
			_clientVersion = ver;
	}
	
	public void setIsDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}

	public void setUser(Pilot p) {
		_userInfo = p;
	}
	
	public void setUserBusy(boolean isBusy) {
		_isUserBusy = isBusy;
	}
	
	public void setUserHidden(boolean isHidden) {
		_isUserHidden = isHidden;
	}

	public void setUserLocation(UserData ud) {
		_userData = ud;
	}
	
	public String getRowClassName() {
		return _isDispatch ? "opt2" : null;
	}

	public int compareTo(Object o2) {
		ACARSConnection c2 = (ACARSConnection) o2;
		if (!isAuthenticated())
			return -1;
		else if (!c2.isAuthenticated())
			return 1;

		Pilot usr = c2.getUser();
		return new Integer(_userInfo.getPilotNumber()).compareTo(new Integer(usr.getPilotNumber()));
	}

	/* Here are the basic I/O methods, read and write */
	String read() throws SocketException, ProtocolException {

		// Clear the buffer
		_iBuffer.clear();

		// Try and read from the channel until end of stream
		try {
			_channel.read(_iBuffer);
		} catch (IOException ie) {
			throw new SocketException("Error reading channel - " + ie.getMessage());
		}

		// if we got nothing, return null
		if (_iBuffer.position() == 0)
			throw new ProtocolException("Connection Closed");

		// Set the limit on the buffer and return to the start
		_iBuffer.flip();

		// Update the counters
		_bytesIn += _iBuffer.limit();
		_msgsIn++;
		_lastActivityTime = System.currentTimeMillis();

		// Reset the decoder and decode into a char buffer
		try {
			_msgBuffer.append(decoder.decode(_iBuffer).toString());
		} catch (CharacterCodingException cce) {
		}

		// Now, search the start of an XML message in the buffer; if there's no open discard the whole thing
		int sPos = _msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_OPEN);
		if (sPos == -1) {
			if (_msgBuffer.indexOf(ProtocolInfo.XML_HEADER) == -1) {
				log.warn("Malformed message - " + _msgBuffer.toString());
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
		return msgOut.toString();
	}
	
	public void queue(String msg) {
		_msgOutBuffer.add(msg);
		if (_wLock.tryLock()) {
			while (!_msgOutBuffer.isEmpty()) {
				write(_msgOutBuffer.get(0));
				_msgOutBuffer.remove(0);
			}

			_wLock.unlock();
		}
	}
	
	protected synchronized void write(String msg) {
		if ((_oBuffer == null) || (msg == null))
			return;

		int writeCount = 1;
		try {
			int ofs = 0;
			byte[] msgBytes = msg.getBytes();

			// Keep writing until the message is done
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
					if (_wSelector.select(200) > 0) {
						_channel.write(_oBuffer);
						_wSelector.selectedKeys().clear();
						if (writeCount > 4)
							writeCount--;
					} else {
						writeCount++;
						if (writeCount >= MAX_WRITE_ATTEMPTS)
							throw new IOException("Write timeout for " + getUserID() + " - " + _channel.socket());
					}
				}
			}

			_bytesOut += ofs;
			_msgsOut++;
		} catch (IOException ie) {
			log.warn("Error writing to channel - " + ie.getMessage());
		} catch (Exception e) {
			log.error("Error writing to socket " + _remoteAddr.getHostAddress() + " - " + e.getMessage(), e);
		}
		
		// Update statistics
		_lastActivityTime = System.currentTimeMillis();
		_bufferWrites += writeCount;
	}
}