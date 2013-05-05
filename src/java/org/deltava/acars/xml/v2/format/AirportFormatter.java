// Copyright 2005, 2006, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirportMessage;

import org.deltava.beans.schedule.Airport;

/**
 * An XML Formatter for Airport data messages. This is a seperate class only to use formatAirport
 * in the v2 ElementFormatter.
 * @author Luke
 * @version 5.1
 * @since 5.1
 */

class AirportFormatter extends ElementFormatter {

	/**
	 * Formats an AirportMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AirportMessage amsg = (AirportMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "airports");
		for (Airport a : amsg.getResponse())
			e.addContent(formatAirport(a, "airport"));
		
		return pe;
	}
}