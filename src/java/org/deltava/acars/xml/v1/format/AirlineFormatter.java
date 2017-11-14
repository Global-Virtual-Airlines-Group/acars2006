// Copyright 2005, 2006, 2011, 2012, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirlineMessage;

import org.deltava.beans.schedule.Airline;
import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Airline data messages.
 * @author Luke
 * @version 8.0
 * @since 1.0
 */

class AirlineFormatter extends ElementFormatter {

	/**
	 * Formats an AirlineMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AirlineMessage amsg = (AirlineMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "airlines");
		for (Airline al : amsg.getResponse()) {
			Element ae = new Element("airline");
			ae.setAttribute("code", al.getCode());
			ae.setAttribute("name", al.getName());
			ae.setAttribute("historic", String.valueOf(al.getHistoric()));
			al.getApplications().forEach(c -> ae.addContent(XMLUtils.createElement("app", c)));
			al.getCodes().forEach(c -> ae.addContent(XMLUtils.createElement("altCode", c)));
			e.addContent(ae);
		}
		
		return pe;
	}
}