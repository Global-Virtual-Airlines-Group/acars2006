// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.text.*;
import java.util.Date;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.xml.XMLException;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for Flight Information elements.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

class FlightInfoParser extends XMLElementParser<InfoMessage> {

	/**
	 * Convert an XML flight information element into an InfoMessage.
	 * @param e the XML element
	 * @return an InfoMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public InfoMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean
		InfoMessage msg = new InfoMessage(user);

		// Parse the start date/time
		try {
			final DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			Date dt = dtf.parse(getChildText(e, "startTime", ""));
			if (dt.getTime() > (System.currentTimeMillis() + 86400000))
				throw new Exception("Start date/time too far in future - " + dt);
				
			msg.setStartTime(dt);
		} catch (Exception ex) {
			msg.setStartTime(new Date());
		}

		// Load the flight code
		String fCode = getChildText(e, "flight_num", SystemData.get("airline.code") + "001");
		if (!Character.isLetter(fCode.charAt(0)))
			fCode = SystemData.get("airline.code") + fCode;

		// Load the bean
		msg.setFlightID(StringUtils.parse(getChildText(e, "flight_id", "0"), 0));
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setLivery(getChildText(e, "livery", null));
		msg.setFlightCode(fCode);
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setFSVersion(StringUtils.parse(getChildText(e, "fs_ver", "2004"), 2004));
		msg.setAirportD(getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(getAirport(getChildText(e, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(e, "airportL", null)));
		msg.setCheckRide(Boolean.valueOf(getChildText(e, "checkRide", null)).booleanValue());
		msg.setOffline(Boolean.valueOf(getChildText(e, "offline", "false")).booleanValue());
		msg.setComplete(Boolean.valueOf(getChildText(e, "complete", null)).booleanValue());
		msg.setDispatchPlan(Boolean.valueOf(getChildText(e, "dispatchPlan", "false")).booleanValue());
		msg.setScheduleValidated(Boolean.valueOf(getChildText(e, "scheduleValidated", "false")).booleanValue());
		msg.setDispatcherID(StringUtils.parse(getChildText(e, "dispatcherID", "0"), 0));
		msg.setRouteID(StringUtils.parse(getChildText(e, "routeID", "0"), 0));
		
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
	
	/**
	 * Helper method to load an airport.
	 */
	private Airport getAirport(String code) throws XMLException {
		Airport a = SystemData.getAirport(code);
		if (a == null)
			throw new XMLException("Invalid Airport Code - " + code);

		return a;
	}
}