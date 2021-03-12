// Copyright 2018, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.Airport;

/**
 * An ACARS data response message to store airport gate information.
 * @author Luke
 * @version 10.0
 * @since 8.4
 */

public class GateMessage extends DataResponseMessage<Gate> {
	
	private Airport _a;
	private boolean _isRoute;

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
	 * Returns whether Gate usage counts are route-based, or overall.
	 * @return TRUE if route-based, otherwise FALSE
	 */
	public boolean isRouteUsage() {
		return _isRoute;
	}
	
	/**
	 * Updates the Airport associated with these gates. 
	 * @param a an Airport
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
	
	/**
	 * Updates whether gate usasge counts are route-based.
	 * @param isRoute TRUE if usage is route-based, otherwise based on total usage
	 */
	public void setRouteUsage(boolean isRoute) {
		_isRoute = isRoute;
	}
}