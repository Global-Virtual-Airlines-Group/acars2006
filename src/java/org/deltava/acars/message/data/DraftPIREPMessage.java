// Copyright 2006, 2009, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.flight.FlightReport;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store draft PIREPs.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class DraftPIREPMessage extends DataResponseMessage<FlightReport> {

	/**
	 * Instantiates the message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public DraftPIREPMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.DRAFTPIREP, parentID);
	}
}