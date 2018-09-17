// Copyright 2012, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

/**
 * A message to store alternate airport data.
 * @author Luke
 * @version 8.4
 * @since 4.2
 */

public class AlternateAirportMessage extends DataResponseMessage<Airport> {
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent messge ID
	 */
	public AlternateAirportMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.ALT, parentID);
	}
}