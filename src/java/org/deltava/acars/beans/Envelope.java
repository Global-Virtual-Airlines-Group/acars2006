// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.Message;

/**
 * An Envelope is a bean used to link Messages or XML text with sender/addressee information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class Envelope implements Comparable<Envelope> {

	/**
	 * The envelope payload.
	 */
	protected Object _payload;
	
	/**
	 * The envelope timestamp.
	 */
	protected long _timeStamp;
	
	private Pilot _owner;
	private long _cid;
	private boolean _critical;

	// The bean should alraedy have what we need
	protected Envelope(Message msgData, long conID) {
		super();
		_owner = msgData.getSender();
		_payload = msgData;
		_timeStamp = msgData.getTime();
		_cid = conID;
	}

	// Since were getting text, we need to supply a userID and do the timestamp ourselves
	protected Envelope(Pilot usrInfo, String msgText, long conID) {
		super();
		_owner = usrInfo;
		_payload = msgText;
		_timeStamp = System.currentTimeMillis();
		_cid = conID;
	}
	
	/**
	 * Returns the connection ID for this Envelope.
	 * @return the Connection ID
	 */
	public long getConnectionID() {
		return _cid;
	}
	
	/**
	 * Returns whether this payload is critical.
	 * @return TRUE if the payload is critical, otherwise FALSE
	 */
	public boolean isCritical() {
		return _critical;
	}
	
	/**
	 * Returns the owner of the payload.
	 * @return the Pilot who owns the payload
	 */
	public Pilot getOwner() {
		return _owner;
	}

	/**
	 * Returns the payload owner's ID.
	 * @return the Owner's Pilot Code
	 */
	public String getOwnerID() {
		return (_owner == null) ? Message.SYSTEM_NAME : _owner.getPilotCode();
	}
	
	/**
	 * Returns the envelope timestamp.
	 * @return the timestamp
	 */
	public long getTime() {
		return _timeStamp;
	}
	
	/**
	 * Marks the payload as critical.
	 * @param isCritical TRUE if the payload is critical, otherwise FALSE
	 */
	public void setCritical(boolean isCritical) {
		_critical = isCritical;
	}
	
	/**
	 * Compares two Envelopes by comparing their timestamps and connection IDs.
	 */
	public int compareTo(Envelope e2) {
		int tmpResult = Boolean.valueOf(_critical).compareTo(Boolean.valueOf(e2._critical));
		if (tmpResult == 0)
			tmpResult = new Long(_timeStamp).compareTo(new Long(e2._timeStamp));
		
		return (tmpResult == 0) ? new Long(_cid).compareTo(new Long(_cid)) : tmpResult;
	}
}