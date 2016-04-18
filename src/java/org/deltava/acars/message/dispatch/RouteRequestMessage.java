// Copyright 2007, 2008, 2011, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to transmit route information requests.
 * @author Luke
 * @version 7.0
 * @since 2.0
 */

public class RouteRequestMessage extends DispatchMessage implements RoutePair {
	
	private Airport _airportD;
	private Airport _airportA;
	private boolean _extRoutes;

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
	@Override
	public Airport getAirportD() {
		return _airportD;
	}

	/**
	 * Returns the arrival Airport.
	 * @return the Airport
	 */
	@Override
	public Airport getAirportA() {
		return _airportA;
	}
	
	@Override
	public int getDistance() {
		return new GeoPosition(_airportD).distanceTo(_airportA);
	}
	
	/**
	 * Returns whether external route sources should be searched.
	 * @return TRUE if external sources should be queried, otherwise FALSE
	 */
	public boolean getExternalRoutes() {
		return _extRoutes;
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

	/**
	 * Update whether external route sources should be searched.
	 * @param doExternal TRUE if external sources should be queried, otherwise FALSE
	 */
	public void setExternalRoutes(boolean doExternal) {
		_extRoutes = doExternal;
	}
}