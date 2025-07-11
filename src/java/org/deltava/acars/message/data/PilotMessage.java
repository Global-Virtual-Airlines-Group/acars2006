// Copyright 2006, 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message to store Pilot information updates. In most cases it is preferrable to send a {@link
 * ConnectionMessage} but for some message types where insufficient connection data is available, a PilotMessage may be sent.
 * @author Luke
 * @version 8.4
 * @since 1.0
 * @see ConnectionMessage
 */

public class PilotMessage extends DataResponseMessage<Pilot> {

	private boolean _isDispatch;
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot.
	 * @param msgType the message type (Add/Remote/List)
	 * @param parentID the request message ID
	 */
	public PilotMessage(Pilot msgFrom, DataRequest msgType, long parentID) {
		super(msgFrom, msgType, parentID);
	}
	
	/**
	 * Returns whether this Pilot is a Dispatcher.
	 * @return TRUE if a Dispatcher, otherwise FALSE
	 */
	public boolean isDispatch() {
		return _isDispatch;
	}
	
	/**
	 * Updates whether this Pilot was a Dispatcher.
	 * @param isDispatch TRUE if a Dispatcher, otherwise FALSE 
	 */
	public void setDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}
}