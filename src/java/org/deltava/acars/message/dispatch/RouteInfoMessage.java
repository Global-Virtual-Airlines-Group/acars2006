// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.schedule.PopulatedRoute;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to store route search results.
 * @author Luke
 * @version 3.3
 * @since 2.0
 */

public class RouteInfoMessage extends DispatchMessage {

	private final Collection<PopulatedRoute> _plans = new ArrayList<PopulatedRoute>();
	private long _parent;
	private Flight _schedInfo;
	
	private String _msg;
	
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
	public Collection<PopulatedRoute> getPlans() {
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
	 * Returns the message sent to the dispatcher or pilot requesting routes.
	 * @return the message
	 */
	public String getMessage() {
		return _msg;
	}
	
	/**
	 * Returns information about this flight from the schedule.
	 * @return a Flight bean
	 */
	public Flight getScheduleInfo() {
		return _schedInfo;
	}

	/**
	 * Returns if the route as valid.
	 * @return isValid TRUE if the route is valid, otherwise FALSE
	 */
	public boolean isRouteValid() {
		return (_schedInfo != null);
	}
	
	/**
	 * Adds a flight route plan to the message.
	 * @param rp a PopulatedRoute bean
	 */
	public void addPlan(PopulatedRoute rp) {
		_plans.add(rp);
	}
	
	/**
	 * Sets an additional message to return to the user requesting routes.
	 * @param msg the message
	 */
	public void setMessage(String msg) {
		_msg = msg;
	}
	
	/**
	 * Updates flight scheudle info, marking the route as valid.
	 * @param f the schedule info
	 */
	public void setScheduleInfo(Flight f) {
		_schedInfo = f;
	}
}