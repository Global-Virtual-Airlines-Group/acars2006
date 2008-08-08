// Copyright 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.acars.message.*;

/**
 * A utility class to turn PositionMessages into {@link ACARSMapEntry} beans.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public class RouteEntryHelper {

	/**
	 * Builds a route Entry from the current connection data.
	 * @param con the ACARS connection
	 * @return a MapRouteEntry bean
	 */
	public static final ACARSMapEntry build(ACARSConnection con) {
		if (con.getIsDispatch())
			return buildDispatch(con);

		// Extract data from the connection
		Pilot usr = con.getUser();
		PositionMessage msg = con.getPosition();
		InfoMessage imsg = con.getFlightInfo();
		if ((usr == null) || (msg == null) || (imsg == null))
			return null;

		// Build the MapRouteEntry bean
		MapRouteEntry result = new MapRouteEntry(new Date(), msg, usr, imsg.getEquipmentType());
		result.setClientBuild(con.getClientVersion(), con.getBeta());
		result.setBusy(con.getUserBusy());
		result.setDispatchPlan(imsg.isDispatchPlan());
		result.setCheckRide(imsg.isCheckRide());
		result.setID(imsg.getFlightID());
		result.setFlightNumber(imsg.getFlightCode());
		result.setAirSpeed(msg.getAspeed());
		result.setGroundSpeed(msg.getGspeed());
		result.setVerticalSpeed(msg.getVspeed());
		result.setAltitude(msg.getAltitude());
		result.setRadarAltitude(msg.getRadarAltitude());
		result.setFlags(msg.getFlags());
		result.setFlaps(msg.getFlaps());
		result.setHeading(msg.getHeading());
		result.setN1(msg.getN1());
		result.setN2(msg.getN2());
		result.setMach(msg.getMach());
		result.setFuelFlow(msg.getFuelFlow());
		result.setAirportD(imsg.getAirportD());
		result.setAirportA(imsg.getAirportA());
		result.setAOA(msg.getAngleOfAttack());
		result.setG(msg.getG());
		result.setFuelRemaining(msg.getFuelRemaining());
		return result;
	}
	
	/**
	 * Builds an ACARS Map Entry for a dispatch connection.
	 * @param ac the ACARS connection
	 * @return a DispatchMapEntry bean
	 */
	public static final ACARSMapEntry buildDispatch(ACARSConnection ac) {
		
		// Get data from the entry
		Pilot usr = ac.getUser();
		GeoLocation loc = ac.getLocation();
		if ((loc == null) || (usr == null))
			return null;
		
		// Build the DispatchMapEntry bean
		DispatchMapEntry result = new DispatchMapEntry(usr, loc);
		result.setClientBuild(ac.getClientVersion(), ac.getBeta());
		result.setBusy(ac.getUserBusy());
		result.setRange(ac.getDispatchRange());
		return result;
	}
}