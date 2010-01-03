// Copyright 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom.Element;

import org.deltava.beans.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.util.StringUtils;

import org.deltava.acars.message.TakeoffMessage;
import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.xml.XMLException;

/**
 * A parser for takeoff/touchdown messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

class TakeoffParser extends XMLElementParser<TakeoffMessage> {
	
	/**
	 * Convert an XML takeoff element into a TakeoffMessage.
	 * @param e the XML element
	 * @return a TakeoffMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public TakeoffMessage parse(Element e, Pilot usr) throws XMLException {
		
		// Create the location
		GeospaceLocation loc = null;
		try {
			double lat = Double.parseDouble(e.getAttributeValue("lat"));
			double lng = Double.parseDouble(e.getAttributeValue("lng"));
			loc = new GeoPosition(lat, lng, StringUtils.parse(e.getAttributeValue("alt"), 0));
		} catch (Exception ex) {
			throw new XMLException("Cannot parse takeoff location - " + ex.getMessage());
		}
		
		// Create the message
		TakeoffMessage msg = new TakeoffMessage(usr);
		msg.setLocation(loc);
		msg.setHeading(StringUtils.parse(getChildText(e, "hdg", "0"), 0));
		msg.setTakeoff(Boolean.valueOf(e.getAttributeValue("takeoff")).booleanValue());
		return msg;
	}
}