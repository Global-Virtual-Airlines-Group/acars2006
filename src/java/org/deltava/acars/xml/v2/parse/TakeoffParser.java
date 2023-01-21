// Copyright 2009, 2010, 2011, 2012, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.beans.*;
import org.deltava.beans.flight.ILSCategory;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.util.StringUtils;

import org.deltava.acars.message.TakeoffMessage;
import org.deltava.acars.xml.*;

/**
 * A parser for takeoff/touchdown messages. 
 * @author Luke
 * @version 10.4
 * @since 2.8
 */

class TakeoffParser extends XMLElementParser<TakeoffMessage> {
	
	/**
	 * Convert an XML takeoff element into a TakeoffMessage.
	 * @param e the XML element
	 * @return a TakeoffMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
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
		msg.setVSpeed(StringUtils.parse(getChildText(e, "vSpeed", "0"), 0));
		msg.setHeading(StringUtils.parse(getChildText(e, "hdg", "0"), 0));
		msg.setTakeoff(Boolean.parseBoolean(e.getAttributeValue("takeoff")));
		if (!msg.isTakeoff())
			msg.setILS(ILSCategory.get(getChildText(e, "ils", "CATI")));
			
		return msg;
	}
}