// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.RoutePlan;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to store route search results.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class RouteInfoMessage extends DispatchMessage {

	private final Collection<RoutePlan> _plans = new ArrayList<RoutePlan>();
	private boolean _routeValid;
	
	/**
	 * Creates the Message.
	 * @param msgFrom the originating Pilot
	 */
	public RouteInfoMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_ROUTEDATA, msgFrom);
	}

	public Collection<RoutePlan> getPlans() {
		return _plans;
	}
	
	public boolean isRouteValid() {
		return _routeValid;
	}
	
	public void addPlan(RoutePlan rp) {
		_plans.add(rp);
	}
	
	public void setRouteValid(boolean isValid) {
		_routeValid = isValid;
	}
}