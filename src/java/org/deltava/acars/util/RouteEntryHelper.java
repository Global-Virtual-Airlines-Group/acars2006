// Copyright 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;

import org.deltava.acars.beans.ACARSConnection;

import org.deltava.acars.message.InfoMessage;
import org.deltava.acars.message.PositionMessage;

/**
 * A utility class to turn PositionMessages into RouteEntry beans.
 * @author Luke
 * @version 2.0
 * @since 1.0
 */

public class RouteEntryHelper {

	/**
	 * Builds a route Entry from the current connection data.
	 * @param con the ACARS connection
	 * @return a RouteEntry bean
	 */
	public static RouteEntry build(ACARSConnection con) {

		// Extract data from the connection
		Pilot usr = con.getUser();
		PositionMessage msg = con.getPosition();
		InfoMessage imsg = con.getFlightInfo();
		if ((usr == null) || (msg == null) || (imsg == null))
			return null;

		// Build the NamedRouteEntry
		NamedRouteEntry result = new NamedRouteEntry(new Date(), msg, usr, imsg.getEquipmentType());
		result.setClientBuild(con.getClientVersion());
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
		return result;
	}
}