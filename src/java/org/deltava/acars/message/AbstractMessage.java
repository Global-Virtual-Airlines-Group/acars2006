// Copyright 2004, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An abstract class to store common Message data.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

public abstract class AbstractMessage implements Message {
	
	private int msgType;
	private long timeStamp = System.currentTimeMillis();
	private Pilot sender;
	private long id;
	private int _version = 1;

	protected AbstractMessage(int type, Pilot msgFrom) {
		super();
		this.msgType = type;
		this.sender = msgFrom;
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
	
	public boolean isPublic() {
		return false;
	}
	
	public boolean isAnonymous() {
		return false;
	}
	
	public final void setID(long newID) {
		if (id == 0)
			id = newID;
	}
	
	public final void setID(String newID) {
		if (id == 0) {
			try {
				id = Long.parseLong(newID, 16);
			} catch (Exception e) {
				// empty
			}
		}
	}

	public final void setSender(Pilot msgFrom) {
		if (msgFrom != null)
			this.sender = msgFrom;
	}
	
	public final long getID() {
		return this.id;
	}

	public final Pilot getSender() {
		return this.sender;
	}
	
	public final String getSenderID() {
		return (this.sender == null) ? SYSTEM_NAME : this.sender.getPilotCode(); 
	}

	public final int getType() {
		return this.msgType;
	}
	
	public final void setTime(long ts) {
		if (ts <= this.timeStamp)
			this.timeStamp = ts;
	}

	public final long getTime() {
		return this.timeStamp;
	}
}