// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to store route plotting requests.
 * @author Luke
 * @version 3.0
 * @since 3.0
 */

public class RoutePlotMessage extends DispatchMessage {
	
	private Airport _airportD;
	private Airport _airportA;
	
	private String _sid;
	private String _star;
	
	private String _route;
	
	private PopulatedRoute _rt;

	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 */
	public RoutePlotMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_ROUTEPLOT, msgFrom);
	}
	
	/**
	 * Returns the departure Airport.
	 * @return the departure Airport
	 */
	public Airport getAirportD() {
		return _airportD;
	}

	/**
	 * Returns the arrival Airport.
	 * @return the arrival Airport
	 */
	public Airport getAirportA() {
		return _airportA;
	}

	/**
	 * Returns the SID code.
	 * @return the SID code
	 */
	public String getSID() {
		return _sid;
	}
	
	/**
	 * Returns the STAR code.
	 * @return the STAR code
	 */
	public String getSTAR() {
		return _star;
	}
	
	/**
	 * Returns the route to plot.
	 * @return the route
	 */
	public String getRoute() {
		return _route;
	}
	
	/**
	 * Returns the plotted Route.
	 * @return a PopulatedRoute bean, or null if not plotted
	 */
	public PopulatedRoute getResults() {
		return _rt;
	}
	
	/**
	 * Updates the departure Airport.
	 * @param a the departure Airport
	 */
	public void setAirportD(Airport a) {
		_airportD = a;
	}

	/**
	 * Updates the arrival Airport.
	 * @param a the arrival Airport
	 */
	public void setAirportA(Airport a) {
		_airportA = a;
	}
	
	/**
	 * Updates the Standard Instrument Departure code.
	 * @param sid the SID code
	 */
	public void setSID(String sid) {
		_sid = sid;
	}
	
	/**
	 * Updates the Standard Terminal Arrival Route code.
	 * @param sid the STAR code
	 */
	public void setSTAR(String star) {
		_star = star;
	}
	
	/**
	 * Updates the route to plot.
	 * @param route the route
	 */
	public void setRoute(String route) {
		_route = route;
	}
	
	/**
	 * Updates the route plot results.
	 * @param rt a PopulatedRoute bean
	 */
	public void setResults(PopulatedRoute rt) {
		_rt = rt;
	}
}