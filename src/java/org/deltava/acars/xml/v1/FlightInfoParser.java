// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.text.*;
import java.util.Date;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLException;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for Flight Information elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class FlightInfoParser extends ElementParser {

	/**
	 * Convert an XML flight information element into an InfoMessage.
	 * @param e the XML element
	 * @return an InfoMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(org.jdom.Element e, Pilot user) throws XMLException {

		// Create the bean
		InfoMessage msg = new InfoMessage(user);

		// Check if we're loading an existing flight ID
		msg.setFlightID(StringUtils.parse(getChildText(e, "flight_id", "0"), 0));

		// Parse the start date/time
		final DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		try {
			msg.setStartTime(dtf.parse(getChildText(e, "startTime", "")));
		} catch (Exception ex) {
			msg.setStartTime(new Date());
		}

		// Load the flight code
		String fCode = getChildText(e, "flight_num", SystemData.get("airline.code") + "001");
		if (!Character.isLetter(fCode.charAt(0)))
			fCode = SystemData.get("airline.code") + fCode;

		// Load the bean
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setFlightCode(fCode);
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setFSVersion(Integer.parseInt(getChildText(e, "fs_ver", "2004")));
		msg.setAirportD(getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(getAirport(getChildText(e, "airportA", null)));
		msg.setCheckRide(Boolean.valueOf(getChildText(e, "checkRide", null)).booleanValue());
		msg.setOffline(Boolean.valueOf(getChildText(e, "offline", "false")).booleanValue());
		msg.setComplete(Boolean.valueOf(getChildText(e, "complete", null)).booleanValue());
		msg.setScheduleValidated(Boolean.valueOf(getChildText(e, "scheduleValidated", "false")).booleanValue());

		// Load waypoints
		String waypoints = getChildText(e, "route", "DIRECT");
		msg.setWaypoints(StringUtils.strip(waypoints, ",\'\""));
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