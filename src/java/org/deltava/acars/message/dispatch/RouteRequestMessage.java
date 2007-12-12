// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;

import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to transmit route information requests.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class RouteRequestMessage extends DispatchMessage {
	
	private Airport _airportD;
	private Airport _airportA;

	/**
	 * Creates the Message.
	 * @param msgFrom the originating Pilot
	 */
	public RouteRequestMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_ROUTEREQ, msgFrom);
	}

	/**
	 * Returns the departure Airport.
	 * @return the Airport
	 */
	public Airport getAirportD() {
		return _airportD;
	}

	/**
	 * Returns the arrival Airport.
	 * @return the Airport
	 */
	public Airport getAirportA() {
		return _airportA;
	}
	
	/**
	 * Updates the departure Airport.
	 * @param a the Airport
	 */
	public void setAirportD(Airport a) {
		_airportD = a;
	}
	
	/**
	 * Updates the arrival Airport.
	 * @param a the Airport
	 */
	public void setAirportA(Airport a) {
		_airportA = a;
	}
}