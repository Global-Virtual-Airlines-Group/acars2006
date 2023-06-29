// Copyright 2006, 2008, 2011, 2012, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ChartsMessage;

import org.deltava.beans.schedule.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Chart data messages.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

public class ChartsFormatter extends ElementFormatter {

	/**
	 * Formats a ChartsMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		ChartsMessage cmsg = (ChartsMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "charts");
		Airport a = cmsg.getAirport();
		
		// Save the airport data
		e.setAttribute("name", a.getName());
		e.setAttribute("iata", a.getIATA());
		e.setAttribute("icao", a.getICAO());
		
		// Write charts
		for (Chart c : cmsg.getResponse()) {
			Element ce = new Element("chart");
			ce.setAttribute("name", c.getName());
			ce.setAttribute("id", String.valueOf(c.getID()));
			if (c instanceof ExternalChart ec) {
				ce.setAttribute("src", ec.getSource());
				ce.addContent(XMLUtils.createElement("url", ec.getURL()));
			}
			
			e.addContent(ce);
		}
		
		return pe;
	}
}