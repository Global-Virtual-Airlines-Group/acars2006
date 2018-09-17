// Copyright 2006, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.beans.ACARSConnection;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message to store ACARS Connection information.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class ConnectionMessage extends DataResponseMessage<ACARSConnection> {

	/**
	 * Instantiates the message.
	 * @param msgFrom the originating Pilot
	 * @param rType the message type
	 * @param parentID the request message ID
	 */
	public ConnectionMessage(Pilot msgFrom, DataRequest rType, long parentID) {
		super(msgFrom, rType, parentID);
	}
}