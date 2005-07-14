/*
 * Created on Feb 7, 2004
 *
 */
package org.deltava.acars.message;

import java.util.HashMap;
import java.util.Iterator;

import org.deltava.beans.Pilot;

/**
 * @author Luke J. Kolin
 */
public class AcknowledgeMessage extends AbstractMessage {

	private long parent;
	private HashMap msgs = new HashMap();

	public AcknowledgeMessage(Pilot msgFrom, long parentID) {
		super(Message.MSG_ACK, msgFrom);
		this.parent = parentID;
	}
	
	public long getParentID() {
		return this.parent;
	}
	
	public String getEntry(String eName) {
		return (String) this.msgs.get(eName);
	}
	
	public Iterator getEntryNames() {
		return this.msgs.keySet().iterator();
	}
	
	public void setEntry(String eName, String eValue) {
		this.msgs.put(eName, eValue);
	}
}
