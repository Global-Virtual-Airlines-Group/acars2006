/*
 * Created on Feb 9, 2004
 *
 * Links a messsage with its owner
 */
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.Message;

/**
 * @author Luke J. Kolin
 */
public class Envelope {

	private Pilot _owner;
	private Object _msg;
	private long _cid;
	private long _timeStamp;

	// The bean should alraedy have what we need
	public Envelope(Message msgData, long conID) {
		super();
		_owner = msgData.getSender();
		_msg = msgData;
		_timeStamp = msgData.getTime();
		_cid = conID;
	}

	// Since were getting text, we need to supply a userID and do the timestamp ourselves
	public Envelope(Pilot usrInfo, String msgText, long conID) {
		super();
		_owner = usrInfo;
		_msg = msgText;
		_timeStamp = System.currentTimeMillis();
		_cid = conID;
	}
	
	public long getConnectionID() {
		return _cid;
	}
	
	public Pilot getOwner() {
		return _owner;
	}

	public String getOwnerID() {
		return (_owner == null) ? Message.SYSTEM_NAME : _owner.getPilotCode();
	}
	
	public Object getMessage() {
		return _msg;
	}
	
	public long getTime() {
		return _timeStamp;
	}
}