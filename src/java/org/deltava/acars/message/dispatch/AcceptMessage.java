// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.acars.message.DispatchMessage;
import org.deltava.beans.Pilot;

/**
 * An ACARS message to accept dispatch requests.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class AcceptMessage extends DispatchMessage {
	
	private long _parentID;
	
	/**
	 * Creates the message.
	 * @param msgFrom the Pilot sending the message
	 * @param parentID the message ID of the DispatchRequest message
	 */
	public AcceptMessage(Pilot msgFrom, long parentID) {
		super(DSP_ACCEPT, msgFrom);
		_parentID = parentID;
	}
	
	/**
	 * Returns the ID of the service request message.
	 * @return the message ID
	 */
	public long getParentID() {
		return _parentID;
	}
}