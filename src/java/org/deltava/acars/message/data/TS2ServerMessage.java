// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.ts2.Server;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store TeamSpeak 2 server data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TS2ServerMessage extends DataResponseMessage<Server> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public TS2ServerMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_TS2SERVERS, parentID);
	}
}