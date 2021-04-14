// Copyright 2005, 2006, 2009, 2012, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirportMessage;

/**
 * An XML Formatter for Airport data messages.
 * @author Luke
 * @version 10.0
 * @since 1.0
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
		amsg.getResponse().forEach(a -> e.addContent(formatAirport(a, "airport")));
		return pe;
	}
}