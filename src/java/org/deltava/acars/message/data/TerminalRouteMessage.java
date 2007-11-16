// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.DataResponseMessage;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.TerminalRoute;

/**
 * An ACARS data response message to store Terminal Route data.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class TerminalRouteMessage extends DataResponseMessage<TerminalRoute> {

	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public TerminalRouteMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_TRINFO, parentID);
	}
}