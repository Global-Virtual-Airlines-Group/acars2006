// Copyright 2006, 2009, 2018, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.DraftFlightPackage;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store draft Flight Reports and briefing packages.
 * @author Luke
 * @version 10.3
 * @since 1.0
 */

public class DraftPIREPMessage extends DataResponseMessage<DraftFlightPackage> {

	/**
	 * Instantiates the message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public DraftPIREPMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.DRAFTPIREP, parentID);
	}
}