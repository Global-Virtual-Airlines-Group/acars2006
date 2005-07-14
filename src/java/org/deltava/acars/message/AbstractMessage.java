/*
 * Created on Feb 6, 2004
 */
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class AbstractMessage implements Message {
	
	protected int msgType;
	protected long timeStamp;
	protected Pilot sender;
	protected long id;
	protected int protocolVersion = 0;

	protected AbstractMessage(int type, Pilot msgFrom) {
		
		// Init super class
		super();
		
		// Init the bean
		this.msgType = type;
		this.timeStamp = System.currentTimeMillis();
		this.sender = msgFrom;
	}
	
	public void setProtocolVersion(int pVersion) {
		if ((this.protocolVersion == 0) && (pVersion > this.protocolVersion))
			this.protocolVersion = pVersion; 
	}
	
	public int getProtocolVersion() {
		return this.protocolVersion;
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
			} catch (Exception e) { }
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