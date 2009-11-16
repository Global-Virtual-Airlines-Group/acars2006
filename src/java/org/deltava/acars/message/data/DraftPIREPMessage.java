// Copyright 2006, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.flight.FlightReport;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store draft PIREPs.
 * @author Luke
 * @version 2.7
 * @since 1.0
 */

public class DraftPIREPMessage extends DataResponseMessage<FlightReport> {

	/**
	 * Instantiates the message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public DraftPIREPMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_DRAFTPIREP, parentID);
	}
}