// Copyright 2010, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.util.List;

import org.deltava.acars.xml.*;

import org.deltava.acars.message.dispatch.RoutePlotMessage;

import org.deltava.beans.Pilot;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An XML parser for RoutePlot messages. 
 * @author Luke
 * @version 4.2
 * @since 3.0
 */

public class RoutePlotParser extends XMLElementParser<RoutePlotMessage> {

	/**
	 * Convert an XML route plot element into a RoutePlotMessage.
	 * @param e the XML element
	 * @return a RoutePlotMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public RoutePlotMessage parse(org.jdom2.Element e, Pilot usr) throws XMLException {

		// Create the message
		RoutePlotMessage msg = new RoutePlotMessage(usr);
		msg.setAirportD(SystemData.getAirport(getChildText(e, "airportD", null)));
		if (msg.getAirportD() == null)
			throw new XMLException("Unknown Departure Airport - " + getChildText(e, "airportD", "??"));
		
		// Get the SID/STAR
		msg.setSID(getChildText(e, "sid", null));
		msg.setSTAR(getChildText(e, "star", null));
		
		// Check if the SID/STAR are part of the route
		List<String> waypoints = StringUtils.split(getChildText(e, "route", ""), " "); 
    	try {
        	String[] wps = waypoints.toArray(new String[0]);
        	int wpMax = wps.length - 1;
        	boolean hasSID = (wpMax > 1) && (wps[0].length() > 3) && Character.isDigit(wps[0].charAt(wps[0].length() - 1));
        	if (hasSID && (msg.getSID() == null)) {
        		waypoints.remove(wps[0]);
        		msg.setSID(wps[0] + "." + wps[1]);
        	}

        	boolean hasSTAR = (wpMax > 1) && (wps[wpMax].length() > 3) && Character.isDigit(wps[wpMax].charAt(wps[wpMax].length() - 1));
        	if (hasSTAR && (msg.getSTAR() == null)) {
        		msg.setSTAR(wps[wpMax] + "." + wps[wpMax - 1]);
        		waypoints.remove(wps[wpMax]);
        	}
    	} finally {
    		msg.setRoute(StringUtils.listConcat(waypoints, " "));
    	}
		
		// Check the arrival airport
		msg.setAirportA(SystemData.getAirport(getChildText(e, "airportA", null)));
		if (msg.getAirportA() == null)
			throw new XMLException("Unknown Arrival Airport - " + getChildText(e, "airportA", "??"));
		
		return msg;
	}
}