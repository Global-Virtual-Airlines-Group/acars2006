// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airline;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store Airline data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AirlineMessage extends DataResponseMessage<Airline> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public AirlineMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_ALLIST, parentID);
	}
}