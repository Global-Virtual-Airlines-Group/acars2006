// Copyright 2004, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An abstract class to store common Message data.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public abstract class AbstractMessage implements Message {
	
	private int _msgType;
	private long _timeStamp = System.nanoTime();
	private Pilot _sender;
	private long _id;
	private int _version = 1;

	protected AbstractMessage(int type, Pilot msgFrom) {
		super();
		_msgType = type;
		_sender = msgFrom;
	}
	
	/**
	 * Allows a message to define the minimum protocol version it is supported by.
	 * @param pVersion the protocol version
	 */
	protected void setProtocolVersion(int pVersion) {
		_version = Math.max(_version, pVersion);
	}
	
	/**
	 * Returns the minimum protocol version that supports this message.
	 */
	public final int getProtocolVersion() {
		return _version;
	}
	
	/**
	 * Returns whether the message can be sent by an unauthenticated user.
	 */
	public boolean isAnonymous() {
		return false;
	}
	
	public final void setID(long newID) {
		if (_id == 0)
			_id = newID;
	}
	
	public final void setSender(Pilot msgFrom) {
		if (msgFrom != null)
			_sender = msgFrom;
	}
	
	public final long getID() {
		return _id;
	}

	public final Pilot getSender() {
		return _sender;
	}
	
	public final String getSenderID() {
		return (_sender == null) ? SYSTEM_NAME : _sender.getPilotCode(); 
	}

	public final int getType() {
		return _msgType;
	}
	
	public final void setTime(long ts) {
		if (ts > 0)
			_timeStamp = Math.min(ts, _timeStamp);
	}

	public final long getTime() {
		return _timeStamp;
	}
}