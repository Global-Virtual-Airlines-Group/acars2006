// Copyright 2004, 2009, 2011, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An abstract class to store common Message data.
 * @author Luke
 * @version 7.0
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
	@Override
	public final int getProtocolVersion() {
		return _version;
	}
	
	/**
	 * Returns whether the message can be sent by an unauthenticated user.
	 */
	@Override
	public boolean isAnonymous() {
		return false;
	}
	
	@Override
	public final void setID(long newID) {
		if (_id == 0)
			_id = newID;
	}
	
	@Override
	public final void setSender(Pilot msgFrom) {
		if (msgFrom != null)
			_sender = msgFrom;
	}
	
	@Override
	public final long getID() {
		return _id;
	}

	@Override
	public final Pilot getSender() {
		return _sender;
	}
	
	@Override
	public final String getSenderID() {
		return (_sender == null) ? SYSTEM_NAME : _sender.getPilotCode(); 
	}

	@Override
	public final int getType() {
		return _msgType;
	}
	
	@Override
	public final void setTime(long ts) {
		if (ts > 0)
			_timeStamp = Math.min(ts, _timeStamp);
	}

	@Override
	public final long getTime() {
		return _timeStamp;
	}
}