// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Chart;

/**
 * An ACARS data response message to store chart data. 
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ChartsMessage extends DataResponseMessage<Chart> {

	/**
	 * Instantiates the response.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ChartsMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_CHARTS, parentID);
	}
}