// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.text.*;
import java.util.Calendar;

import org.jdom.Element;

import org.apache.log4j.Logger;
import org.deltava.beans.Pilot;

import org.deltava.acars.message.mp.MPMessage;
import org.deltava.acars.xml.XMLException;

import static org.gvagroup.acars.ACARSFlags.*;

/**
 * A parser for multi-player location elements.
 * @author Luke
 * @version 2.5
 * @since 2.2
 */

class MPLocationParser extends ElementParser<MPMessage> {
	
	private static final Logger log = Logger.getLogger(MPLocationParser.class);
	private final DateFormat _mdtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");

	/**
	 * Convert an XML MP position element into an MPMessage.
	 * @param e the XML element
	 * @return an MPMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public MPMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean
		MPMessage msg = new MPMessage(user);
		
		// Parse the date
		String de = getChildText(e, "date", null);
		try {
			if ((de != null) && (!de.contains("."))) {
				int ms  = Calendar.getInstance().get(Calendar.MILLISECOND);
				de = de + "." + String.valueOf(ms);
			}
			
			if (de != null) {
				de = de.replace('-', '/');
				msg.setDate(_mdtf.parse(de));
			}
		} catch (Exception ex) {
			log.warn("Unparseable date - " + de);
		}
		
		// Get the basic information
		try {
			msg.setLatitude(Double.parseDouble(e.getAttributeValue("lat", "0")));
			msg.setLongitude(Double.parseDouble(e.getAttributeValue("lon", "0")));
			msg.setHeading(Integer.parseInt(getChildText(e, "h", "0")));
			msg.setAltitude(Integer.parseInt(getChildText(e, "a", "0")));
			msg.setPitch(Double.parseDouble(getChildText(e, "p", "0")));
			msg.setBank(Double.parseDouble(getChildText(e, "b", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "s", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "fl", "0")));
			msg.setLights(Integer.parseInt(getChildText(e, "l", "0")));
			
			// Load attributes
			String attr = e.getAttributeValue("attr", "");
			msg.setFlag(FLAG_AFTERBURNER, (attr.indexOf('A') > 0));
			msg.setFlag(FLAG_GEARDOWN, (attr.indexOf('G') > 0));
			msg.setFlag(FLAG_SPARMED, (attr.indexOf('S') > 0));
			msg.setFlag(FLAG_ONGROUND, (attr.indexOf('O') > 0));
		} catch (Exception ex) {
			throw new XMLException("Error parsing MP Position data - " + ex.getMessage(), ex);
		}
		
		// Return the bean
		return msg;
	}
}