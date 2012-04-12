// Copyright 2008, 2009, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.util.Date;

import org.jdom2.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.mp.MPMessage;
import org.deltava.acars.xml.*;

import static org.gvagroup.acars.ACARSFlags.*;

/**
 * A parser for multi-player location elements.
 * @author Luke
 * @version 4.2
 * @since 2.2
 */

class MPLocationParser extends XMLElementParser<MPMessage> {
	
	/**
	 * Convert an XML MP position element into an MPMessage.
	 * @param e the XML element
	 * @return an MPMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public MPMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean
		MPMessage msg = new MPMessage(user);
		
		// Get the basic information
		try {
			msg.setDate(new Date(Long.parseLong(getChildText(e, "dt", String.valueOf(System.currentTimeMillis())))));
			msg.setLatitude(Double.parseDouble(e.getAttributeValue("lat", "0")));
			msg.setLongitude(Double.parseDouble(e.getAttributeValue("lon", "0")));
			msg.setHeading(Integer.parseInt(getChildText(e, "h", "0")));
			msg.setPitch(Double.parseDouble(getChildText(e, "p", "0")));
			msg.setBank(Double.parseDouble(getChildText(e, "b", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "g", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "fl", "0")));
			msg.setLights(Integer.parseInt(getChildText(e, "l", "0")));
			double alt = Double.parseDouble(getChildText(e, "a", "0"));
			msg.setAltitude((int)Math.floor(alt));
			msg.setFractionalAltitude(Math.abs((int)(Math.floor(alt) - alt)));
			
			// Load attributes
			String attr = e.getAttributeValue("attr", "");
			msg.setFlag(FLAG_AFTERBURNER, (attr.indexOf('A') > 0));
			msg.setFlag(FLAG_GEARDOWN, (attr.indexOf('G') > 0));
			msg.setFlag(FLAG_SPARMED, (attr.indexOf('S') > 0));
			msg.setFlag(FLAG_ONGROUND, (attr.indexOf('O') > 0));
		} catch (Exception ex) {
			throw new XMLException("Error parsing MP Position data - " + ex.getMessage(), ex);
		}
		
		return msg;
	}
}