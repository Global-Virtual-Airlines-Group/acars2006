// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2016, 2017, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;
import org.deltava.acars.message.Message;

/**
 * An Envelope is a bean used to link data with sender/addressee information.
 * @author Luke
 * @version 8.3
 * @since 1.0
 * @param <T>  the envelope type
 */

public abstract class Envelope<T> implements Comparable<Envelope<T>> {

	private final T _payload;
	
	/**
	 * The envelope timestamp.
	 */
	protected long _timeStamp;
	
	private final Pilot _owner;
	private final long _cid;
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
		if (_owner == null)
			return Message.SYSTEM_NAME;
		
		String pCode = _owner.getPilotCode();
		return StringUtils.isEmpty(pCode) ? String.valueOf(_owner.getID()) : pCode;
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
	@Override
	public int compareTo(Envelope<T> e2) {
		int tmpResult = Boolean.compare(_critical, e2._critical);
		if (tmpResult == 0)
			tmpResult = Long.compare(_timeStamp, e2._timeStamp);
		
		return (tmpResult == 0) ? Long.compare(_cid, e2._cid) : tmpResult;
	}
}