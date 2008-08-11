// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.RoutePlan;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to store route search results.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

public class RouteInfoMessage extends DispatchMessage {

	private final Collection<RoutePlan> _plans = new ArrayList<RoutePlan>();
	private long _parent;
	private boolean _routeValid;
	
	/**
	 * Creates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public RouteInfoMessage(Pilot msgFrom, long parentID) {
		super(DispatchMessage.DSP_ROUTEDATA, msgFrom);
		if (parentID > 0)
			_parent = parentID;
	}

	/**
	 * Returns the associated RoutePlan beans.
	 * @return a List of RoutePlan beans
	 */
	public Collection<RoutePlan> getPlans() {
		return _plans;
	}
	
	/**
	 * The ID of the route request message.
	 * @return the message ID
	 */
	public long getParentID() {
		return _parent;
	}

	/**
	 * Returns if the route as valid.
	 * @return isValid TRUE if the route is valid, otherwise FALSE
	 */
	public boolean isRouteValid() {
		return _routeValid;
	}
	
	/**
	 * Adds a flight route plan to the message.
	 * @param rp a RoutePlan bean
	 */
	public void addPlan(RoutePlan rp) {
		_plans.add(rp);
	}
	
	/**
	 * Marks the route as valid.
	 * @param isValid TRUE if the route is valid, otherwise FALSE
	 */
	public void setRouteValid(boolean isValid) {
		_routeValid = isValid;
	}
}