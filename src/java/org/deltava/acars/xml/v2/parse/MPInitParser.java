// Copyright 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.util.StringUtils;

import org.deltava.acars.message.mp.InitMessage;

import org.deltava.acars.xml.*;

/**
 * A message parser for multi-player initialization messages. 
 * @author Luke
 * @version 4.2
 * @since 3.0
 */

class MPInitParser extends XMLElementParser<InitMessage> {

	/**
	 * Convert an XML MP init element into an MPInitMessage.
	 * @param e the XML element
	 * @return an MPInitMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public InitMessage parse(org.jdom2.Element e, Pilot usr) {
		
		// Get the center
		double lat = Double.parseDouble(e.getAttributeValue("lat", "0"));
		double lng = Double.parseDouble(e.getAttributeValue("lon", "0"));

		// Create the message
		InitMessage msg = new InitMessage(usr);
		msg.setLocation(new GeoPosition(lat, lng, StringUtils.parse(e.getAttributeValue("alt"), 0)));
		msg.setRange(StringUtils.parse(getChildText(e, "range", "40"), 40));
		msg.setLivery(getChildText(e, "livery", "DEFAULT"));
		return msg;
	}
}