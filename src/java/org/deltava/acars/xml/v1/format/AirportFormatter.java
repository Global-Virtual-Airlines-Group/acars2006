// Copyright 2005, 2006, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirportMessage;

import org.deltava.beans.schedule.Airport;

/**
 * An XML Formatter for Airport data messages.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

class AirportFormatter extends ElementFormatter {

	/**
	 * Formats an AirportMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		AirportMessage amsg = (AirportMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "airports");
		for (Iterator<Airport> i = amsg.getResponse().iterator(); i.hasNext(); ) {
			Airport a = i.next();
			e.addContent(formatAirport(a, "airport"));
		}
		
		return pe;
	}
}