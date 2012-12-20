// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.text.*;
import java.util.Date;

import org.jdom2.Element;

import org.deltava.beans.*;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for Flight Information elements.
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

class FlightInfoParser extends XMLElementParser<InfoMessage> {
	
	// FSUIPC Flight Simulator version constants - 2006=FSX, 2008=Prepar3D/ESP
	private static final int[] FSUIPC_FS_VERSIONS = {95, 98, 2000, 0, 0, 0, 2002, 2004, 2006, 2008, 2008};

	/**
	 * Convert an XML flight information element into an InfoMessage.
	 * @param e the XML element
	 * @return an InfoMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public InfoMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean
		InfoMessage msg = new InfoMessage(user);

		// Parse the start date/time
		try {
			final DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			Date dt = dtf.parse(getChildText(e, "startTime", ""));
			if (dt.getTime() > (System.currentTimeMillis() + 86400000))
				throw new IllegalArgumentException("Start date/time too far in future - " + dt);
				
			msg.setStartTime(dt);
		} catch (Exception ex) {
			msg.setStartTime(new Date());
		}

		// Load the flight code
		String fCode = getChildText(e, "flight_num", SystemData.get("airline.code") + "001");
		if (!Character.isLetter(fCode.charAt(0)))
			fCode = SystemData.get("airline.code") + fCode;
		int ofs = fCode.indexOf(" LEG");
		if (ofs > 0)
			fCode = fCode.substring(0, ofs).trim(); 

		// Load the bean
		msg.setFlightID(StringUtils.parse(getChildText(e, "flight_id", "0"), 0));
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setFlightCode(fCode);
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setAirportD(getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(getAirport(getChildText(e, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(e, "airportL", null)));
		msg.setCheckRide(Boolean.valueOf(getChildText(e, "checkRide", null)).booleanValue());
		msg.setComplete(Boolean.valueOf(getChildText(e, "complete", null)).booleanValue());
		msg.setDispatchPlan(Boolean.valueOf(getChildText(e, "dispatchPlan", "false")).booleanValue());
		msg.setScheduleValidated(Boolean.valueOf(getChildText(e, "scheduleValidated", "false")).booleanValue());
		msg.setDispatcherID(StringUtils.parse(getChildText(e, "dispatcherID", "0"), 0));
		msg.setRouteID(StringUtils.parse(getChildText(e, "routeID", "0"), 0));
		try {
			msg.setNetwork(OnlineNetwork.valueOf(getChildText(e, "network", null)));
		} catch (Exception ex) {
			msg.setNetwork(null);
		}
		
		// Parse the simulator
		int ver = StringUtils.parse(getChildText(e, "fs_ver", "2004"), 2004);
		if ((ver > 0) && (ver < FSUIPC_FS_VERSIONS.length))
			msg.setSimulator(Simulator.fromVersion(FSUIPC_FS_VERSIONS[ver]));
		else
			msg.setSimulator(Simulator.fromVersion(ver));
		if (msg.getSimulator() == Simulator.UNKNOWN)
			msg.setSimulator(Simulator.FS9);
		
		// Load SID data
		Element sid = e.getChild("sid");
		if (sid != null)
			msg.setSID(getChildText(sid, "name", "") + "." + getChildText(sid, "transition", "") + "." + getChildText(sid, "rwy", ""));
		
		// Load STAR data
		Element star = e.getChild("star");
		if (star != null)
			msg.setSTAR(getChildText(star, "name", "") + "." + getChildText(star, "transition", "") + "." + getChildText(star, "rwy", ""));

		// Load waypoints
		msg.setWaypoints(getChildText(e, "route", "DIRECT"));
		return msg;
	}
}