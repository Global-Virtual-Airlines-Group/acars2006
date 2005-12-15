package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.*;
import java.nio.channels.SocketChannel;

import java.nio.charset.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.beans.system.UserData;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.ProtocolInfo;

import org.deltava.util.system.SystemData;

/**
 * @author Luke J. Kolin
 */
public class ACARSConnection implements Serializable, Comparable {

	protected static final Logger log = Logger.getLogger(ACARSConnection.class);
	private static final int MAX_WRITE_ATTEMPTS = 12;

	// Byte byffer decoder and character set
	private final CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();

	private SocketChannel _channel;
	private InetAddress _remoteAddr;
	private String _remoteHost;
	private int protocolVersion = 1;
	private int clientVersion;

	// Input/output network buffers
	private ByteBuffer _iBuffer;
	private ByteBuffer _oBuffer;

	// The the actual buffer for messages
	private StringBuilder _msgBuffer;

	// Connection information
	private long id;
	private Pilot _userInfo;
	private UserData _userData;
	private InfoMessage _fInfo;
	private PositionMessage _pInfo;

	// Activity monitors
	private long startTime;
	private long lastActivityTime;

	// Statistics
	private long bytesIn;
	private long bytesOut;
	private long msgsIn;
	private long msgsOut;
	private long bufferWrites;

	public ACARSConnection(long cid, SocketChannel sc) {

		// Init the superclass and start time
		super();
		this.startTime = System.currentTimeMillis();
		this.id = cid;

		// Get IP Address information
		_remoteAddr = sc.socket().getInetAddress();

		// Turn off blocking
		try {
			sc.configureBlocking(false);
		} catch (IOException ie) {
			// Log our error and shut the connection
			log.error("Cannot set non-blocking I/O from " + _remoteAddr.getHostAddress(), ie);
			try {
				sc.close();
			} catch (Exception e) {
			}
		} finally {
			_channel = sc;
		}

		// Allocate the buffers and output stack for this channel
		_msgBuffer = new StringBuilder();
		_iBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.nio"));
		_oBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.nio"));
	}

	public void close() {
		// Clear the buffers
		_iBuffer = null;
		_oBuffer = null;

		// Close the socket
		try {
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
		return (this.id == cid);
	}

	public boolean equals(String pid) {
		return _userInfo.getPilotCode().equals(pid);
	}

	public long getBytesIn() {
		return this.bytesIn;
	}

	public long getBytesOut() {
		return this.bytesOut;
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
		return this.id;
	}

	public InfoMessage getFlightInfo() {
		return _fInfo;
	}

	public PositionMessage getPosition() {
		return _pInfo;
	}

	public long getLastActivity() {
		return this.lastActivityTime;
	}

	public long getMsgsIn() {
		return this.msgsIn;
	}

	public long getMsgsOut() {
		return this.msgsOut;
	}
	
	public long getBufferWrites() {
		return this.bufferWrites;
	}

	public int getProtocolVersion() {
		return this.protocolVersion;
	}

	public int getClientVersion() {
		return this.clientVersion;
	}

	public long getStartTime() {
		return this.startTime;
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
			this.protocolVersion = pv;
	}

	public void setClientVersion(int ver) {
		if (ver > 50)
			clientVersion = ver;
	}

	public void setUser(Pilot p) {
		_userInfo = p;
	}

	public void setUserLocation(UserData ud) {
		_userData = ud;
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
		bytesIn += _iBuffer.limit();
		msgsIn++;
		lastActivityTime = System.currentTimeMillis();

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

	public void write(String msg) {
		if (msg == null)
			return;

		int ofs = 0;
		int writeCount = 0;
		byte[] msgBytes = msg.getBytes();
		try {
			// Keep writing until the message is done
			while (ofs < msgBytes.length) {
				_oBuffer.clear();
				writeCount++;

				// Keep writing to the buffer
				while ((ofs < msgBytes.length) && (_oBuffer.remaining() > 0)) {
					_oBuffer.put(msgBytes[ofs]);
					ofs++;
				}

				// Flip the buffer
				_oBuffer.flip();
				while (_oBuffer.remaining() > 0)
					_channel.write(_oBuffer);
			}

			// Update statistics
			lastActivityTime = System.currentTimeMillis();
			msgsOut++;
			bytesOut += ofs;
			bufferWrites += writeCount;
		} catch (Exception e) {
			log.error("Error writing to socket " + _remoteAddr.getHostAddress() + " - " + e.getMessage(), e);
		}

		// Warn if too many attempts
		if (writeCount > MAX_WRITE_ATTEMPTS)
			log.warn("Excessive number of buffer writes - " + writeCount);
	}
}