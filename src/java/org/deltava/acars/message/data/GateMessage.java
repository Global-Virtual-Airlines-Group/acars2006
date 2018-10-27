// Copyright 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.Airport;

/**
 * An ACARS data response message to store airport gate information.
 * @author Luke
 * @version 8.4
 * @since 8.4
 */

public class GateMessage extends DataResponseMessage<Gate> {
	
	private Airport _a;

	/**
	 * Creates the mesage.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent request message ID
	 */
	public GateMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.GATES, parentID);
	}

	/**
	 * Returns the Airport associated with these gates.
	 * @return an Airport
	 */
	public Airport getAirport() {
		return _a;
	}
	
	/**
	 * Updates the Airport associated with these gates. 
	 * @param a an Airport
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
}