// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.navdata.NavigationDataBean;

/**
 * A bean to store flight route waypoint data, to mark a waypoint as
 * being part of a SID/STAR.
 * @author Luke
 * @version 2.1
 * @since 2.1
 */

public class RouteWaypoint {

	private NavigationDataBean _nd;
	private String _airway;
	private boolean _inTerminalRoute;
	
	/**
	 * Initializes the bean.
	 * @param nd the NavigartionDataBean to wrap
	 */
	public RouteWaypoint(NavigationDataBean nd) {
		super();
		_nd = nd;
	}
	
	/**
	 * Returns the waypoint.
	 * @return the waypoint NavigationDataBean
	 */
	public NavigationDataBean getWaypoint() {
		return _nd;
	}
	
	/**
	 * Returns if this waypoint is on an Airway. 
	 * @return the airway code
	 */
	public String getAirway() {
		return _airway;
	}
	
	/**
	 * Returns whether this waypoint is part of a terminal route.
	 * @return TRUE if part of a Terminal Route, otherwise FALSE
	 */
	public boolean isInTerminalRoute() {
		return _inTerminalRoute;
	}

	/**
	 * Sets this waypoint as part of a Terminal Route.
	 * @param inTR TRUE if part of a 
	 */
	public void setInTerminalRoute(boolean inTR) {
		_inTerminalRoute = inTR;
	}
	
	/**
	 * Updates whether this waypoint is on an Airway.
	 * @param aCode the Airway code
	 */
	public void setAirway(String aCode) {
		_airway = (aCode == null) ? null : aCode.toUpperCase();
	}
	
	public String toString() {
		return _nd.getCode();
	}
	
	public int hashCode() {
		return _nd.getCode().hashCode();
	}
}