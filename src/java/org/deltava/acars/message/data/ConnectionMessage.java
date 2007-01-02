// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.acars.message.DataResponseMessage;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message to store ACARS Connection information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ConnectionMessage extends DataResponseMessage<ACARSConnection> {

	public static final int ADD = REQ_ADDUSER;
	public static final int LIST = REQ_USRLIST;
	
	/**
	 * Instantiates the message.
	 * @param msgFrom the originating Pilot
	 * @param rType the message type
	 * @param parentID the request message ID
	 */
	public ConnectionMessage(Pilot msgFrom, int rType, long parentID) {
		super(msgFrom, rType, parentID);
	}
}