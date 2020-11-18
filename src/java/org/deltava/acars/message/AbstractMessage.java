// Copyright 2004, 2009, 2011, 2016, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An abstract class to store common Message data.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public abstract class AbstractMessage implements Message {
	
	private final MessageType _msgType;
	private long _timeStamp = System.nanoTime();
	private final Pilot _sender;
	private long _id;
	private int _version = 1;

	protected AbstractMessage(MessageType type, Pilot msgFrom) {
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
	
	@Override
	public final int getProtocolVersion() {
		return _version;
	}
	
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
	public final MessageType getType() {
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