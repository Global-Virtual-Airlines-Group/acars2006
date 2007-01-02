// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ChartsMessage;

import org.deltava.beans.schedule.*;

/**
 * An XML Formatter for Chart data messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ChartsFormatter extends ElementFormatter {

	/**
	 * Formats a ChartsMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		ChartsMessage cmsg = (ChartsMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "charts");
		for (Iterator<Chart> i = cmsg.getResponse().iterator(); i.hasNext(); ) {
			Chart c = i.next();
			Airport a = c.getAirport();
			
			// Save the airport data
			e.setAttribute("name", a.getName());
			e.setAttribute("iata", a.getIATA());
			e.setAttribute("icao", a.getICAO());
			
			// Add the cheart element
			Element ce = new Element("chart");
			ce.setAttribute("name", c.getName());
			ce.setAttribute("id", String.valueOf(c.getID()));
			e.addContent(ce);
		}
		
		return pe;
	}
}