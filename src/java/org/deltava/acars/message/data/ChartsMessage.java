// Copyright 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.*;

/**
 * An ACARS data response message to store chart data. 
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public class ChartsMessage extends DataResponseMessage<Chart> {
	
	private Airport _a;

	/**
	 * Instantiates the response.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ChartsMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_CHARTS, parentID);
	}
	
	/**
	 * Returns the airport associated with these charts.
	 * @return the Airport bean
	 */
	public Airport getAirport() {
		return _a;
	}
	
	/**
	 * Updates the Airport associated with these charts. 
	 * @param a the Airport bean
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
}