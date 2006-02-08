// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS Acknowledgement message.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AcknowledgeMessage extends AbstractMessage {

	private long _parent;
	private Map<String, String> _msgs = new LinkedHashMap<String, String>();

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
	
	public Collection<String> getEntryNames() {
		return _msgs.keySet();
	}
	
	public void setEntry(String eName, String eValue) {
		_msgs.put(eName, eValue);
	}
}