// Copyright 2007, 2008, 2009, 2010, 2012, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.PopulatedRoute;

import org.deltava.acars.message.DispatchMessage;
import org.deltava.acars.message.DispatchRequest;

/**
 * A message to store route search results.
 * @author Luke
 * @version 8.4
 * @since 2.0
 */

public class RouteInfoMessage extends DispatchMessage {

	private final Collection<PopulatedRoute> _plans = new ArrayList<PopulatedRoute>();
	private final Collection<Gate> _arrivalGates = new LinkedHashSet<Gate>();
	
	private final long _parent;
	private Flight _schedInfo;
	private Gate _gateD;
	
	private String _msg;
	
	/**
	 * Creates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public RouteInfoMessage(Pilot msgFrom, long parentID) {
		super(DispatchRequest.ROUTEDATA, msgFrom);
		_parent = Math.max(0, parentID);
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
	 * Returns the most likely arrival gates.
	 * @return a Collection of Gates
	 */
	public Collection<Gate> getArrivalGates() {
		return _arrivalGates;
	}
	
	/**
	 * Returns the closest gate to the Aircraft's position.
	 * @return the closest Gate, or null
	 */
	public Gate getClosestGate() {
		return _gateD;
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
	 * Adds an arrival gate to the message.
	 * @param g a Gate
	 */
	public void addGate(Gate g) {
		_arrivalGates.add(g);
	}
	
	/**
	 * Updates the closest Gate to the aircraft's position.
	 * @param g a Gate
	 */
	public void setClosestGate(Gate g) {
		_gateD = g;
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