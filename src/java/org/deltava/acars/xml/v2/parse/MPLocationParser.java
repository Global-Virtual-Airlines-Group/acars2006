// Copyright 2008, 2009, 2011, 2012, 2016, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.time.Instant;

import org.jdom2.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.acars.message.mp.MPMessage;
import org.deltava.acars.xml.*;

import org.deltava.util.StringUtils;

/**
 * A parser for multi-player location elements.
 * @author Luke
 * @version 8.6
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
		long now = System.currentTimeMillis();
		try {
			msg.setDate(Instant.ofEpochMilli(StringUtils.parse(getChildText(e, "dt", String.valueOf(now)), now, false)));
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
			msg.setFlag(ACARSFlags.AFTERBURNER, (attr.indexOf('A') > 0));
			msg.setFlag(ACARSFlags.GEARDOWN, (attr.indexOf('G') > 0));
			msg.setFlag(ACARSFlags.SP_ARMED, (attr.indexOf('S') > 0));
			msg.setFlag(ACARSFlags.ONGROUND, (attr.indexOf('O') > 0));
		} catch (Exception ex) {
			throw new XMLException("Error parsing MP Position data - " + ex.getMessage(), ex);
		}
		
		return msg;
	}
}