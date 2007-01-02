// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirlineMessage;

import org.deltava.beans.schedule.Airline;

/**
 * An XML Formatter for Airline data messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class AirlineFormatter extends ElementFormatter {

	/**
	 * Formats an AirlineMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		AirlineMessage amsg = (AirlineMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "airlines");
		for (Iterator<Airline> i = amsg.getResponse().iterator(); i.hasNext(); ) {
			Airline al = i.next();
			
			// Build the airline element
			Element ae = new Element("airline");
			ae.setAttribute("code", al.getCode());
			ae.setAttribute("name", al.getName());
			e.addContent(ae);
		}
		
		return pe;
	}
}