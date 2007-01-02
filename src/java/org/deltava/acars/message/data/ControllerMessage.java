// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.servinfo.Controller;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store ATC data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ControllerMessage extends DataResponseMessage<Controller> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ControllerMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_ATCINFO, parentID);
	}
}