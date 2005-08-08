package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.BufferOverflowException;

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
public class ACARSConnection implements Serializable {

	private static final Logger log = Logger.getLogger(ACARSConnection.class);

	// Info type constants
	public static final int POSITION_INFO = 0;
	public static final int FLIGHT_INFO = 1;
	public static final int USER_INFO = 2;
	public static final int USER_LOCATION_DATA = 3;

	// Byte byffer decoder and character set
	private final CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();

	private SocketChannel channel;
	private InetAddress remoteAddr;
	private String remoteHost;
	private int protocolVersion = 1;

	// Input/output network buffers
	private ByteBuffer _iBuffer;
	private ByteBuffer _oBuffer;

	// The the actual buffer for messages
	private StringBuffer msgBuffer;

	// Connection information
	private long id;
	private Pilot userInfo;
	private UserData userData;

	private InfoMessage fInfo;
	private PositionMessage pInfo;

	// Activity monitors
	private long startTime;
	private long lastActivityTime;

	// Statistics
	private long bytesIn;
	private long bytesOut;
	private long msgsIn;
	private long msgsOut;

	public ACARSConnection(long cid, SocketChannel sc) {

		// Init the superclass and start time
		super();
		this.startTime = System.currentTimeMillis();
		this.id = cid;

		// Get IP Address information
		this.remoteAddr = sc.socket().getInetAddress();

		// Turn off blocking
		try {
			sc.configureBlocking(false);
		} catch (IOException ie) {
			// Log our error and shut the connection
			log.error("Cannot set non-blocking I/O from " + remoteAddr.getHostAddress(), ie);
			try {
				sc.close();
			} catch (Exception e) {
			}
		} finally {
			this.channel = sc;
		}

		// Allocate the buffers and output stack for this channel
		this.msgBuffer = new StringBuffer();
		_iBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.recv"));
		_oBuffer = ByteBuffer.allocate(SystemData.getInt("acars.buffer.send"));
	}

	public void close() {
		// Clear the user info bean
		this.userInfo = null;

		// Close the socket
		try {
			this.channel.close();
		} catch (Exception e) {
		}
	}

	public boolean equals(SocketChannel ch) {
		return this.channel.equals(ch);
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
		return this.userInfo.getPilotCode().equals(pid);
	}

	public long getBytesIn() {
		return this.bytesIn;
	}

	public long getBytesOut() {
		return this.bytesOut;
	}

	SocketChannel getChannel() {
		return this.channel;
	}

	public long getElapsedTime() {
		return (System.currentTimeMillis() - this.startTime);
	}

	public String getFormatID() {
		return Long.toHexString(id).toUpperCase();
	}

	public String getFormatLastActivityTime() {
		return Long.toHexString(this.lastActivityTime).toUpperCase();
	}

	public int getFlightID() {
		return (this.fInfo == null) ? 0 : this.fInfo.getFlightID();
	}

	public long getID() {
		return this.id;
	}

	public Object getInfo(int infoType) {

		switch (infoType) {
			case POSITION_INFO:
				return this.pInfo;
			case FLIGHT_INFO:
				return this.fInfo;
			case USER_INFO:
				return getUser();
			case USER_LOCATION_DATA:
				return this.userData;
			default:
				return null;
		}
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

	public int getProtocolVersion() {
		return this.protocolVersion;
	}

	public long getStartTime() {
		return this.startTime;
	}

	public String getRemoteAddr() {
		return this.remoteAddr.getHostAddress();
	}

	public String getRemoteHost() {
		return (this.remoteHost == null) ? this.remoteAddr.getHostName() : this.remoteHost;
	}

	public Pilot getUser() {
		return (isAuthenticated()) ? userInfo : null;
	}

	public String getUserID() {
		return isAuthenticated() ? userInfo.getPilotCode() : getRemoteAddr();
	}

	public boolean isAuthenticated() {
		return (this.userInfo != null);
	}

	public boolean isConnected() {
		return this.channel.isConnected();
	}

	public void setInfo(Message msg) {
		if (msg == null) {
			fInfo = null;
			pInfo = null;
		} else if (msg instanceof InfoMessage) {
			fInfo = (InfoMessage) msg;
		} else if (msg instanceof PositionMessage) {
			pInfo = (PositionMessage) msg;
		}
	}

	public void setProtocolVersion(int pv) {

		// Validate the protocol version
		if ((pv > 0) && (pv <= Message.PROTOCOL_VERSION))
			this.protocolVersion = pv;
	}

	public void setUser(Pilot p) {
		this.userInfo = p;
	}

	public void setUserLocation(UserData ud) {
		this.userData = ud;
	}

	/* Here are the basic I/O methods, read and write */
	String read() throws SocketException, ProtocolException {

		// Clear the buffer
		_iBuffer.clear();

		// Try and read from the channel until end of stream
		try {
			channel.read(_iBuffer);
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
		CharBuffer cBuf = null;
		synchronized (decoder) {
			try {
				cBuf = decoder.decode(_iBuffer);
			} catch (CharacterCodingException cce) {
			}
		}

		// Append the message into the existing buffer
		this.msgBuffer.append(cBuf.toString());

		// Now, search the start of an XML message in the buffer; if there's no open discard the whole thing
		int sPos = msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_OPEN);
		if (sPos == -1) {
			if (msgBuffer.indexOf(ProtocolInfo.XML_HEADER) == -1) {
				log.info("Malformed message - " + msgBuffer.toString());
				msgBuffer.setLength(0);
			}

			// Return nothing
			return null;
		}

		// Get the end of the message - if there's an end element build a message and return it
		int ePos = msgBuffer.indexOf(ProtocolInfo.REQ_ELEMENT_CLOSE, sPos);
		if (ePos == -1)
			return null;

		ePos += ProtocolInfo.REQ_ELEMENT_CLOSE.length();

		// Get the XML message out of the buffer
		StringBuffer msgOut = new StringBuffer(ProtocolInfo.XML_HEADER);
		msgOut.append(msgBuffer.substring(sPos, ePos));

		// Clear the message out of the buffer
		msgBuffer.delete(0, ePos);

		// Return the buffer
		return msgOut.toString();
	}

	void write(String msg) {

		// Don't write nulls
		if ((msg == null) || (msg.length() < 1))
			return;

		// Clear the buffer
		_oBuffer.clear();

		// If the message would be larger than the buffer, create a new buffer
		if (msg.length() > _oBuffer.limit())
			_oBuffer = ByteBuffer.allocate(msg.length() + 16);

		// Dump the message into the buffer and write to the socket channel
		try {
			_oBuffer.put(msg.getBytes());
			_oBuffer.flip();
			channel.write(_oBuffer);
			bytesOut += msg.length();
			lastActivityTime = System.currentTimeMillis();
		} catch (IOException ie) {
			log.error("Error writing to socket " + remoteAddr.getHostAddress() + " - " + ie.getMessage(), ie);
		}

		// Search for
	}

	public void write(RawMessage rawMsg) {

		// Dont' write nulls
		if (rawMsg == null)
			return;

		// Clear the output buffer
		_oBuffer.clear();

		// Get the message text and size
		String msg = rawMsg.getText();

		// Dump the message into the buffer and write to the socket channel
		try {
			_oBuffer.put(msg.getBytes());
			_oBuffer.flip();
			channel.write(_oBuffer);
			bytesOut += msg.length();
			msgsOut++;
			lastActivityTime = System.currentTimeMillis();
		} catch (BufferOverflowException bfe) {
			log.error("Error writing message to buffer, size=" + msg.length());
		} catch (IOException ie) {
			log.error("Error writing to socket channel " + remoteAddr.getHostAddress());
		}
	}
}