// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.jdom.*;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLException;

/**
 * A Parser for Position elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class PositionParser extends ElementParser {
	
	/**
	 * Convert an XML position element into a PositionMessage.
	 * @param e the XML element
	 * @return a PositionMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(Element e, Pilot user) throws XMLException {
		
		// Create the bean
		PositionMessage msg = new PositionMessage(user);

		// Parse the date
		final DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		try {
			msg.setDate(dtf.parse(getChildText(e, "date", "")));
		} catch (Exception ex) {
			// empty
		}

		// Get the basic information
		try {
			msg.setHeading(Integer.parseInt(getChildText(e, "hdg", "0")));
			msg.setPitch(Double.parseDouble(getChildText(e, "pitch", "0")));
			msg.setBank(Double.parseDouble(getChildText(e, "bank", "0")));
			msg.setLatitude(Double.parseDouble(getChildText(e, "lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText(e, "lon", "0")));
			msg.setAltitude(Integer.parseInt(getChildText(e, "msl", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText(e, "agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "aSpeed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "gSpeed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText(e, "vSpeed", "0")));
			msg.setMach(Double.parseDouble(getChildText(e, "mach", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText(e, "fuel", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText(e, "flags", "0")));
			msg.setAngleOfAttack(Double.parseDouble(getChildText(e, "aoa", "0")));
			msg.setG(Double.parseDouble(getChildText(e, "g", "1")));
			msg.setN1(Double.parseDouble(getChildText(e, "n1", "0")));
			msg.setN2(Double.parseDouble(getChildText(e, "n2", "0")));
			msg.setWindHeading(Integer.parseInt(getChildText(e, "wHdg", "0")));
			msg.setWindSpeed(Integer.parseInt(getChildText(e, "wSpeed", "0")));
			msg.setFuelFlow(Integer.parseInt(getChildText(e, "fuelFlow", "0")));
			msg.setPhase(getChildText(e, "phase", PositionMessage.FLIGHT_PHASES[PositionMessage.PHASE_UNKNOWN]));
			msg.setSimRate(Integer.parseInt(getChildText(e, "simrate", "256")));
			msg.setLogged(Boolean.valueOf(getChildText(e, "isLogged", "true")).booleanValue());
			msg.setNoFlood(Boolean.valueOf(getChildText(e, "noFlood", "false")).booleanValue());
			msg.setFrameRate(Integer.parseInt(getChildText(e, "frameRate", "0")));
		} catch (Exception ex) {
			throw new XMLException("Error parsing Position data - " + ex.getMessage(), ex);
		}

		// Return the bean
		return msg;
	}
}