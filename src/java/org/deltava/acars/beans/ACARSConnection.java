// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.system.IPAddressInfo;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.ScopeInfoMessage;
import org.deltava.acars.xml.ProtocolInfo;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server connection.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public class ACARSConnection implements Comparable<ACARSConnection>, ViewEntry, ConnectionStats {

	protected transient static final Logger log = Logger.getLogger(ACARSConnection.class);
	
	private transient static final int MAX_WRITE_ATTEMPTS = 32;
	private transient static final String MAGIC_RESET_CODE = "</!ACARSReset>";

	// Byte byffer decoder and character set
	private transient final Charset cs = Charset.forName("UTF-8"); 
	private transient final CharsetDecoder decoder = cs.newDecoder();
	
	private transient SocketChannel _channel;
	private transient Selector _wSelector;

	private InetAddress _remoteAddr;
	private String _remoteHost;
	private int _protocolVersion = 1;
	private int _clientVersion;
	private int _beta;
	
	private long _dispatcherID;
	private long _viewerID;
	
	private boolean _isDispatch;
	private boolean _isViewer;
	
	// Dispatch service location/range
	private GeoLocation _loc;
	private int _range;
	
	// Radar scope range
	private ScopeInfoMessage _scope;

	// Input/output network buffers
	private transient final ByteBuffer _iBuffer = ByteBuffer.allocateDirect(SystemData.getInt("acars.buffer.nio"));
	private transient final ByteBuffer _oBuffer = ByteBuffer.allocateDirect(SystemData.getInt("acars.buffer.nio"));

	// The the actual buffers for messages
	private transient final StringBuilder _msgBuffer = new StringBuilder();
	protected transient final Queue<String> _msgOutBuffer = new ConcurrentLinkedQueue<String>();

	// Connection information
	private long _id;
	private Pilot _userInfo;
	private UserData _userData;
	private IPAddressInfo _addrInfo;
	private InfoMessage _fInfo;
	private PositionMessage _pInfo;
	private boolean _isUserBusy;
	private boolean _isUserHidden;

	// Activity monitors
	private final long _startTime = System.currentTimeMillis();
	private long _lastActivityTime;
	private long _timeOffset;

	// The write lock
	private transient final ReadWriteLock _rwLock = new ReentrantReadWriteLock(true);
	private transient final Lock _wLock = _rwLock.writeLock();

	// Statistics
	private long _bytesIn;
	private long _bytesOut;
	private int _msgsIn;
	private int _msgsOut;
	private int _bufferReads;
	private int _bufferWrites;
	private int _writeErrors;
	
	// MP field
	private final int _maxDistance = SystemData.getInt("mp.max_range", 40);

	/**
	 * Creates a new ACARS connection.
	 * @param cid the connection ID
	 * @param sc the TCP/IP SocketChannel
	 */
	public ACARSConnection(long cid, SocketChannel sc) {
		super();
		_id = cid;

		// Get IP Address information
		_remoteAddr = sc.socket().getInetAddress();

		// Get a write selector
		try {
			_wSelector = Selector.open();
			long startTime = System.currentTimeMillis();
			sc.configureBlocking(false);
			sc.register(_wSelector, SelectionKey.OP_WRITE);
			_lastActivityTime = System.currentTimeMillis();
			
			// Check execution time
			long execTime = _lastActivityTime - startTime;
			if (execTime > 1250)
				log.warn("Excessive connect time - " + execTime + "ms");
		} catch (IOException ie) {
			// Log our error and shut the connection
			log.error("Cannot set non-blocking I/O from " + _remoteAddr.getHostAddress());
			try {
				sc.close();
			} catch (Exception e) {
				// empty
			}
		} finally {
			_channel = sc;
		}
	}

	/**
	 * Closes the connection.
	 */
	public void close() {

		// Clean out the buffer
		if (_wLock.tryLock()) {
			_msgOutBuffer.clear();
			_wLock.unlock();
		}

		try {
			_wSelector.close();
			_channel.close();
		} catch (Exception e) {
			// empty
		}
	}

	public boolean equals(Object o2) {
		return (o2 instanceof ACARSConnection) ? (_id == ((ACARSConnection) o2)._id) : false;
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

	public int getFlightID() {
		return (_fInfo == null) ? 0 : _fInfo.getFlightID();
	}

	public long getID() {
		return _id;
	}
	
	public IPAddressInfo getAddressInfo() {
		return _addrInfo;
	}

	public InfoMessage getFlightInfo() {
		return _fInfo;
	}

	public PositionMessage getPosition() {
		return _pInfo;
	}
	
	public ScopeInfoMessage getScope() {
		return _scope;
	}
	
	public GeoLocation getMPLocation() {
		return (_scope != null) ? _scope : _pInfo;
	}
	
	public int getMPRange() {
		if (_scope != null) 
			return _scope.getRange();
		else if (getIsMP())
			return _maxDistance;
		else
			return -1;
	}
	
	public boolean getUserBusy() {
		return _isUserBusy;
	}

	public boolean getUserHidden() {
		return _isUserHidden;
	}
	
	public boolean getIsMP() {
		return (_fInfo != null) && (_fInfo.getLivery() != null);
	}

	public long getLastActivity() {
		return _lastActivityTime;
	}

	public int getMsgsIn() {
		return _msgsIn;
	}

	public int getMsgsOut() {
		return _msgsOut;
	}

	public int getBufferReads() {
		return _bufferReads;
	}
	
	public int getBufferWrites() {
		return _bufferWrites;
	}
	
	public int getWriteErrors() {
		return _writeErrors;
	}

	public int getProtocolVersion() {
		return _protocolVersion;
	}

	public boolean getIsViewer() {
		return _isViewer;
	}
	
	public boolean getIsDispatch() {
		return _isDispatch;
	}
	
	public long getDispatcherID() {
		return _dispatcherID;
	}
	
	public long getViewerID() {
		return _viewerID;
	}
	
	public GeoLocation getLocation() {
		return _isDispatch ? _loc : _pInfo;
	}
	
	public int getDispatchRange() {
		return _range;
	}

	public int getClientVersion() {
		return _clientVersion;
	}
	
	public int getBeta() {
		return _beta;
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
	
	public long getTimeOffset() {
		return _timeOffset;
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

	public void setFlightInfo(InfoMessage msg) {
		_fInfo = msg;
	}

	public void setPosition(PositionMessage msg) {
		_pInfo = msg;
	}
	
	public void setScope(ScopeInfoMessage msg) {
		_scope = msg;
	}
	
	public void setProtocolVersion(int pv) {
		_protocolVersion = Math.max(_protocolVersion, pv);
	}

	public void setClientVersion(int ver) {
		_clientVersion = Math.max(1, ver);
	}
	
	public void setBeta(int beta) {
		_beta = Math.max(0, beta);
	}
	
	public void setIsViewer(boolean isViewer) {
		_isViewer = isViewer;
	}

	public void setIsDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}
	
	public void setDispatcherID(long conID) {
		_dispatcherID = conID;
	}
	
	public void setViewerID(long conID) {
		_viewerID = conID;
	}	
	
	public void setTimeOffset(long ofs) {
		_timeOffset = ofs;
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
	
	public void setAddressInfo(IPAddressInfo addrInfo) {
		_addrInfo = addrInfo;
	}
	
	public void setDispatchRange(GeoLocation loc, int range) {
		_loc = loc;
		_range = Math.max(0, range);
	}

	public String getRowClassName() {
		if (_isViewer)
			return "opt3";
		else if (_isDispatch)
			return "opt2";
		
		return null;
	}
	
	public int hashCode() {
		return Long.valueOf(_id).hashCode();
	}
	
	public int compareTo(ACARSConnection c2) {
		if (!isAuthenticated())
			return -1;
		else if (!c2.isAuthenticated())
			return 1;

		Pilot usr = c2.getUser();
		return Integer.valueOf(_userInfo.getPilotNumber()).compareTo(Integer.valueOf(usr.getPilotNumber()));
	}

	/* Here are the basic I/O methods, read and write */
	String read() throws SocketException, ProtocolException {

		// Clear the buffer
		_iBuffer.clear();

		// Try and read from the channel until end of stream
		try {
			_channel.read(_iBuffer);
			_bufferReads++;
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
		_lastActivityTime = System.currentTimeMillis();

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
		_msgsIn++;
		return msgOut.toString();
	}

	/**
	 * Queues a message to be written.
	 * @param msg the message text
	 */
	public void queue(String msg) {
		_msgOutBuffer.add(msg);
		
		// Only allow one thread to write to the channel
		if (_wLock.tryLock()) {
			while (!_msgOutBuffer.isEmpty())
				write(_msgOutBuffer.poll());

			_wLock.unlock();
		}
	}

	/**
	 * Writes a message to the socket.
	 * @param msg the message text
	 */
	protected void write(String msg) {
		if ((_oBuffer == null) || (msg == null))
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
						_bytesOut += _channel.write(_oBuffer);
						_wSelector.selectedKeys().clear();
						_bufferWrites++;
						if (writeCount > 4)
							writeCount--;
					} else if (!_channel.isConnected()) {
						close();
						return;
					} else {
						_bufferWrites++;
						if (writeCount >= MAX_WRITE_ATTEMPTS) {
							_writeErrors++;
							_oBuffer.clear();
							_oBuffer.put(MAGIC_RESET_CODE.getBytes(cs));
							if (_wSelector.select(300) > 0) {
								_bytesOut += _channel.write(_oBuffer);		
								_wSelector.selectedKeys().clear();
							}
							
							throw new IOException("Write timeout for " + getUserID() + " at " + _remoteAddr);
						}
					}
				}
			}

			_msgsOut++;
		} catch (ClosedSelectorException cse) {
			log.info("Cannot write to " + _remoteAddr.getHostAddress() + " - selector closed");
		} catch (AsynchronousCloseException ace) {
			log.warn("Connection for " + getUserID() + " closed during write");
		} catch (IOException ie) {
			log.warn("Error writing to channel for " + getUserID() + " - " + ie.getMessage());
		} catch (Exception e) {
			log.error("Error writing to socket " + _remoteAddr.getHostAddress() + " - " + e.getMessage(), e);
		}

		// Update statistics
		_lastActivityTime = System.currentTimeMillis();
	}
}