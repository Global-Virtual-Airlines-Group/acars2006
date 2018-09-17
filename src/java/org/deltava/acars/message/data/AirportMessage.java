// Copyright 2006, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Airport data.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class AirportMessage extends DataResponseMessage<Airport> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent messge ID
	 */
	public AirportMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.APLIST, parentID);
	}
}