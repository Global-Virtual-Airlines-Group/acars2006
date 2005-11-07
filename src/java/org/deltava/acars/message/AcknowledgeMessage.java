/*
 * Created on Feb 7, 2004
 *
 */
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * @author Luke J. Kolin
 */
public class AcknowledgeMessage extends AbstractMessage {

	private long _parent;
	private Map<String, String> _msgs = new HashMap<String, String>();

	public AcknowledgeMessage(Pilot msgFrom, long parentID) {
		super(Message.MSG_ACK, msgFrom);
		_parent = parentID;
	}
	
	public long getParentID() {
		return _parent;
	}
	
	public String getEntry(String eName) {
		return _msgs.get(eName);
	}
	
	public Iterator getEntryNames() {
		return _msgs.keySet().iterator();
	}
	
	public void setEntry(String eName, String eValue) {
		_msgs.put(eName, eValue);
	}
}