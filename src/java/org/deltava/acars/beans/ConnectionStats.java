// Copyright 2008, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * An abstract class to for objects that can return ACARS connection statistics.
 * @author Luke
 * @version 4.0
 * @since 2.1
 */

public abstract class ConnectionStats implements java.io.Serializable {
	
	private long _id;
	
	protected int _msgsIn;
	protected int _msgsOut;
	
	protected int _pktsIn;
	protected int _pktsOut;
	
	protected long _bytesIn;
	protected long _bytesOut;
	
	protected long _voiceBytesIn;
	protected long _voiceBytesOut;
	
	protected int _writeErrors;
	protected int _voiceWriteErrors;
	
	protected ConnectionStats(long id) {
		super();
		_id = id;
	}
	
	protected ConnectionStats(ConnectionStats cs) {
		this(cs.getID());
		_msgsIn = cs.getMsgsIn();
		_msgsOut = cs.getMsgsOut();
		_pktsIn = cs.getPacketsIn();
		_pktsOut = cs.getPacketsOut();
		_bytesIn = cs.getBytesIn();
		_bytesOut = cs.getBytesOut();
		_voiceBytesIn = cs.getVoiceBytesIn();
		_voiceBytesOut = cs.getVoiceBytesOut();
		_writeErrors = cs.getWriteErrors();
		_voiceWriteErrors = cs.getVoiceWriteErrors();
	}
	
	/**
	 * Returns the ACARS connection ID.
	 * @return the connection ID
	 */
	public long getID() {
		return _id;
	}

	/**
	 * Returns the number of voice packets received on this connection.
	 * @return the number of packets
	 */
	public int getPacketsIn() {
		return _pktsIn;
	}
	
	/**
	 * Returns the number of voice packets sent on this connection.
	 * @return the number of packets
	 */
	public int getPacketsOut() {
		return _pktsOut;
	}

	/**
	 * Returns the number of messages received on this connection.
	 * @return the number of messages
	 */
	public int getMsgsIn() {
		return _msgsIn;
	}
	
	/**
	 * Returns the number of messages sent on this connection.
	 * @return the number of messages
	 */
	public int getMsgsOut() {
		return _msgsOut;
	}
	
	/**
	 * Returns the number of bytes received on this connection.
	 * @return the number of bytes
	 */
	public long getBytesIn() {
		return _bytesIn;
	}
	
	/**
	 * Returns the number of bytes sent on this connection.
	 * @return the number of bytes
	 */
	public long getBytesOut() {
		return _bytesOut;
	}

	/**
	 * Returns the number of voice bytes received on this connection.
	 * @return the number of bytes
	 */
	public long getVoiceBytesIn() {
		return _voiceBytesIn;
	}

	/**
	 * Returns the number of voice bytes sent on this connection.
	 * @return the number of bytes
	 */
	public long getVoiceBytesOut() {
		return _voiceBytesOut;
	}

	/**
	 * Returns the number of write errors on this connection.
	 * @return the number of timed out writes
	 */
	public int getWriteErrors() {
		return _writeErrors;
	}

	/**
	 * Returns the number of voice write errors on this connection.
	 * @return the number of timed out writes
	 */
	public int getVoiceWriteErrors() {
		return _voiceWriteErrors;
	}
	
	public int hashCode() {
		return Long.valueOf(_id).hashCode();
	}
}