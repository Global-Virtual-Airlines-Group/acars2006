// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.Message;

/**
 * An Envelope is a bean used to link data with sender/addressee information.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public abstract class Envelope<T> implements Comparable<Envelope<T>> {

	/**
	 * The envelope payload.
	 */
	private T _payload;
	
	/**
	 * The envelope timestamp.
	 */
	protected long _timeStamp;
	
	private Pilot _owner;
	private long _cid;
	private boolean _critical;

	/**
	 * Initializes the envelope.
	 * @param msgData the payload
	 * @param owner the envelope owner
	 * @param ts the original timestamp
	 * @param conID the connection ID
	 */
	protected Envelope(T msgData, Pilot owner, long ts, long conID) {
		super();
		_owner = owner;
		_payload = msgData;
		_timeStamp = ts;
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
	 * Returns the Envelope payload.
	 * @return the payload
	 */
	public T getMessage() {
		return _payload;
	}
	
	/**
	 * Compares two Envelopes by comparing their timestamps and connection IDs.
	 */
	public int compareTo(Envelope<T> e2) {
		int tmpResult = Boolean.valueOf(_critical).compareTo(Boolean.valueOf(e2._critical));
		if (tmpResult == 0)
			tmpResult = new Long(_timeStamp).compareTo(new Long(e2._timeStamp));
		
		return (tmpResult == 0) ? new Long(_cid).compareTo(new Long(_cid)) : tmpResult;
	}
}